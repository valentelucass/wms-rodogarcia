[CmdletBinding()]
param([switch]$PrepareOnly)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'launcher-common.ps1')
$runId = [guid]::NewGuid().ToString('N')
$runDirectory = Join-Path $repository ('orchestracao/.runtime/launcher-producao/runs/' + $runId)
$null = New-Item -ItemType Directory -Path $runDirectory
$candidate = Join-Path $runDirectory 'candidate'
$null = New-Item -ItemType Directory -Path $candidate
$receiptPath = Join-Path $runDirectory 'launcher.json'
$receipt = [ordered]@{
    natureza='WMS_PRODUCTION_LAUNCHER'; runId=$runId; projectPath=$repository
    inicioUtc=[DateTime]::UtcNow.ToString('o'); estado='PREPARING'; codigo=''; exitCode=$null
    prepareOnly=[bool]$PrepareOnly; database='WMS_PROD'; login='WMSPROD'
    frontendUrl=$null; backendUrl=$null; candidate=$candidate
    build=$null; guard=$null; backend=$null; frontend=$null
    sqlGuardInvoked=$false; backendStarted=$false; frontendStarted=$false
    operationalReadiness=$false; authenticatedBusinessRead=$false; businessWrites=0
    mockFallback=$false; existingProcessesAltered=$false; cloudflareConfigured=$false
}
$frontendProcess = $null
$frontendStarted = $false
$guardProcess = $null
$guardStarted = $false
$backend = $null

function Save-WmsProductionReceipt {
    $receipt['observadoUtc'] = [DateTime]::UtcNow.ToString('o')
    [IO.File]::WriteAllText($receiptPath, ($receipt | ConvertTo-Json -Depth 20),
        [Text.UTF8Encoding]::new($false))
}

function Fail-WmsProductionLauncher {
    param([int]$Code, [string]$Reason)
    $receipt['estado']='BLOCKED'; $receipt['codigo']=$Reason; $receipt['exitCode']=$Code
    Save-WmsProductionReceipt
    Write-Host ('[WMS PROD] Bloqueado: ' + $Reason) -ForegroundColor Yellow
    Write-Host ('[WMS PROD] Recibo: ' + $receiptPath)
    Write-Host '[WMS PROD] Nenhum fallback DEV, migration ou encerramento de processo existente.'
    exit $Code
}

try {
    Write-Host '[WMS PROD] Preparacao de versao atual. Publicacao externa nao configurada.'
    $configPath = Join-Path $PSScriptRoot 'producao.json'
    $config = [IO.File]::ReadAllText($configPath) | ConvertFrom-Json
    Assert-WmsProductionPublicConfig $config
    $receipt['config'] = @{path=$configPath; sha256=(Get-FileHash $configPath -Algorithm SHA256).Hash}
    if (-not $PrepareOnly) {
        Assert-WmsProductionFreePort $config.backendPort
        Assert-WmsProductionFreePort $config.frontendPort
        # Nao recompilar nem abrir SQL quando o canal AUTH PROD nem existe.
        . (Join-Path $repository 'backend/scripts/prod/identity-prod.ps1')
        $identity = Get-WmsProductionIdentity
        Assert-WmsProductionIdentity $identity
        $receipt['identity'] = @{mode='native'; origin=$identity.origin; issuer=$identity.issuer; audience=$identity.audience}
    }
    $buildBackend = Join-Path $repository 'backend/scripts/prod/build-backend-prod.ps1'
    $buildFrontend = Join-Path $repository 'frontend/tools/build-prod.mjs'
    foreach ($file in @($buildBackend,$buildFrontend)) {
        if (-not (Test-Path -LiteralPath $file -PathType Leaf)) { throw 'PROD_BUILD_HELPER_MISSING' }
    }
    . $buildBackend
    Write-Host '[WMS PROD] Gerando backend em candidato isolado...'
    $artifact = Build-WmsProductionBackend -CandidateDirectory (Join-Path $candidate 'backend')
    Write-Host '[WMS PROD] Gerando frontend REAL estatico em candidato isolado...'
    $node = (Get-Command node.exe -ErrorAction Stop).Source
    $frontArgs = (ConvertTo-WmsProductionArgument $buildFrontend) + ' --release ' +
        (ConvertTo-WmsProductionArgument (Join-Path $candidate 'frontend')) + ' --run-id ' + $runId
    $buildInfo = New-WmsProductionChildInfo $node $frontArgs $repository
    $buildInfo.EnvironmentVariables['NODE_ENV']='production'
    $buildProcess = [Diagnostics.Process]::new()
    $buildProcess.StartInfo = $buildInfo
    $buildStarted = $false
    try {
        if (-not $buildProcess.Start()) { throw 'PROD_FRONTEND_BUILD_NOT_STARTED' }
        $buildStarted = $true
        $out = $buildProcess.StandardOutput.ReadToEndAsync()
        $err = $buildProcess.StandardError.ReadToEndAsync()
        $buildProcess.WaitForExit()
        $null = $out.Result; $null = $err.Result
        if ($buildProcess.ExitCode -ne 0) { throw 'PROD_FRONTEND_BUILD_FAILED' }
    } finally { Stop-WmsProductionChild $buildProcess $buildStarted }
    $frontendManifest = Join-Path $candidate 'frontend/dist/build-manifest.json'
    if (-not (Test-Path -LiteralPath $frontendManifest -PathType Leaf)) { throw 'PROD_FRONTEND_MANIFEST_MISSING' }
    $release = [ordered]@{
        natureza='WMS_PRODUCTION_CANDIDATE'; runId=$runId; observadoUtc=[DateTime]::UtcNow.ToString('o')
        database='WMS_PROD'; productionMode='real'; backend=$artifact
        frontend=@{manifestPath=$frontendManifest; manifestSha256=(Get-FileHash $frontendManifest -Algorithm SHA256).Hash}
        config=$receipt.config; promoted=$false; existingVersionReplaced=$false
    }
    $releasePath = Join-Path $runDirectory 'release.json'
    [IO.File]::WriteAllText($releasePath, ($release | ConvertTo-Json -Depth 16), [Text.UTF8Encoding]::new($false))
    $receipt['build'] = @{releasePath=$releasePath; releaseSha256=(Get-FileHash $releasePath -Algorithm SHA256).Hash; backend=$artifact; frontend=$release.frontend}
    if ($PrepareOnly) {
        $receipt['estado']='PREPARED_ONLY'; $receipt['codigo']='BUILDS_REAL_NO_DATABASE_OR_SERVERS'; $receipt['exitCode']=0
        Save-WmsProductionReceipt
        Write-Host '[WMS PROD] Candidato preparado; nenhum banco ou servidor iniciado.' -ForegroundColor Green
        Write-Host ('[WMS PROD] Versao: ' + $releasePath)
        exit 0
    }
    # Auth PROD foi conferida em metadados antes do build. Nunca usar material DEV.
    . (Join-Path $repository 'backend/scripts/prod/start-backend-prod.ps1')
    Assert-WmsProductionFreePort $config.backendPort
    Assert-WmsProductionFreePort $config.frontendPort
    $guardHelper = Join-Path $PSScriptRoot 'guarda-wmsprod.ps1'
    $guardHash = (Get-FileHash $guardHelper -Algorithm SHA256).Hash
    $guardLaunchArgs = '-NoProfile -ExecutionPolicy Bypass -File ' + (ConvertTo-WmsProductionArgument $guardHelper)
    $guardInfo = New-WmsProductionChildInfo -Executable (Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe') -Arguments $guardLaunchArgs -WorkingDirectory $repository
    $guardProcess = [Diagnostics.Process]::new()
    $guardProcess.StartInfo = $guardInfo
    if (-not $guardProcess.Start()) { throw 'PROD_GUARD_NOT_STARTED' }
    $guardStarted=$true; $receipt['sqlGuardInvoked']=$true
    $guardOut=$guardProcess.StandardOutput.ReadToEndAsync(); $guardErr=$guardProcess.StandardError.ReadToEndAsync()
    $deadline = [DateTime]::UtcNow.AddSeconds(240)
    while (-not $guardProcess.WaitForExit(10000)) {
        Write-Host '[WMS PROD] Guarda operacional em andamento; nao e ensaio de negocio.'
        if ([DateTime]::UtcNow -ge $deadline) { throw 'PROD_GUARD_TIMEOUT' }
    }
    $guardText=$guardOut.Result; $null=$guardErr.Result
    $guard = $guardText | ConvertFrom-Json
    $guardPath = Join-Path $runDirectory 'guarda.json'
    [IO.File]::WriteAllText($guardPath,$guardText,[Text.UTF8Encoding]::new($false))
    $guardReceiptHash=(Get-FileHash $guardPath -Algorithm SHA256).Hash
    $receipt['guard']=@{path=$guardPath; sha256=$guardReceiptHash; helperSha256=$guardHash; processId=$guardProcess.Id; exitCode=$guardProcess.ExitCode}
    if ($guardProcess.ExitCode -ne 0 -or -not $guard.guardaRealAprovada) { Fail-WmsProductionLauncher 20 'PROD_CURRENT_GUARD_BLOCKED' }
    if ((Get-FileHash $guardHelper -Algorithm SHA256).Hash -ine $guardHash) { throw 'PROD_GUARD_HELPER_CHANGED' }
    $backendArguments = @{Port=$config.backendPort; RunDirectory=$runDirectory; GuardReceipt=$guardPath;
        GuardSha256=$guardReceiptHash; GuardProcessId=$guardProcess.Id; GuardHelperSha256=$guardHash;
        Artifact=$artifact; Identity=$identity}
    $backend = Start-WmsProductionBackend @backendArguments
    $receipt['backendStarted']=($null -ne $backend.Process)
    $receipt['backend']=@{receiptPath=$backend.ReceiptPath; receiptSha256=$backend.ReceiptSha256; pid=$backend.Process.Id}
    $server = Join-Path $repository 'frontend/tools/prod-server.mjs'
    $frontArgs=(ConvertTo-WmsProductionArgument $server) + ' --dist ' +
        (ConvertTo-WmsProductionArgument (Join-Path $candidate 'frontend/dist')) + ' --manifest ' +
        (ConvertTo-WmsProductionArgument $frontendManifest) + ' --port ' + $config.frontendPort +
        ' --backend http://127.0.0.1:' + $config.backendPort + ' --public-origin ' + $config.frontendOrigin
    $frontendInfo=New-WmsProductionChildInfo $node $frontArgs $repository
    $frontendInfo.EnvironmentVariables['NODE_ENV']='production'
    $frontendProcess=[Diagnostics.Process]::new(); $frontendProcess.StartInfo=$frontendInfo
    if (-not $frontendProcess.Start()) { throw 'PROD_FRONTEND_NOT_STARTED' }
    $frontendStarted=$true; $receipt['frontendStarted']=$true
    $frontendOut=$frontendProcess.StandardOutput.ReadToEndAsync(); $frontendErr=$frontendProcess.StandardError.ReadToEndAsync()
    $ready=$false; $deadline=[DateTime]::UtcNow.AddSeconds(30)
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($frontendProcess.HasExited -or $backend.Process.HasExited) { break }
        $listener=@(Get-NetTCPConnection -State Listen -LocalAddress 127.0.0.1 -LocalPort $config.frontendPort -ErrorAction SilentlyContinue)
        if ($listener.Count -eq 1 -and $listener[0].OwningProcess -eq $frontendProcess.Id) {
            try {
                $html=Invoke-WebRequest -UseBasicParsing -TimeoutSec 3 -Uri ('http://127.0.0.1:'+$config.frontendPort+'/')
                $status=Invoke-RestMethod -TimeoutSec 3 -Uri ('http://127.0.0.1:'+$config.frontendPort+'/api/v1/status')
                if ($html.StatusCode -eq 200 -and $html.Content -match 'WMS' -and
                    $status.aplicacao -ceq 'wms-rodogarcia' -and $status.status -ceq 'DISPONIVEL') { $ready=$true; break }
            } catch { }
        }
        Start-Sleep -Milliseconds 300
    }
    if (-not $ready) { Fail-WmsProductionLauncher 60 'PROD_FRONTEND_READINESS_FAILED' }
    $receipt['frontend']=@{pid=$frontendProcess.Id; startUtc=$frontendProcess.StartTime.ToUniversalTime().ToString('o'); listenerOwned=$true; mode='production-real-static'; manifest=$frontendManifest}
    $receipt['backendUrl']='http://127.0.0.1:'+$config.backendPort
    $receipt['frontendUrl']='http://127.0.0.1:'+$config.frontendPort
    $receipt['operationalReadiness']=$true
    $receipt['estado']='OPERATIONAL_PUBLIC_READINESS_HTTPS_LOGIN_PENDING'; $receipt['codigo']='NO_BUSINESS_HOMOLOGATION'; $receipt['exitCode']=0
    Save-WmsProductionReceipt
    Write-Host ('[WMS PROD] Frontend estatico: ' + $receipt.frontendUrl) -ForegroundColor Green
    Write-Host ('[WMS PROD] Backend: ' + $receipt.backendUrl)
    Write-Host '[WMS PROD] Readiness publico aprovado. Login exige a entrada HTTPS futura; localhost nao comprova login seguro.'
    Write-Host '[WMS PROD] Cloudflare/DNS nao configurados. Mantenha esta janela; Ctrl+C encerra somente processos criados neste run.'
    Write-Host ('[WMS PROD] Recibo: ' + $receiptPath)
    while (-not $frontendProcess.HasExited -and -not $backend.Process.HasExited) { Start-Sleep -Milliseconds 500 }
    Fail-WmsProductionLauncher 30 'PROD_OWN_PROCESS_EXITED'
} catch {
    $reason=$_.Exception.Message
    if ($reason -notmatch '^PROD_[A-Z0-9_]+$') { $reason='PROD_LOCAL_STARTUP_FAILED' }
    Fail-WmsProductionLauncher (Get-WmsProductionFailureExitCode $reason) $reason
} finally {
    $cleanupArguments = @{Frontend=$frontendProcess; FrontendStarted=$frontendStarted;
        Backend=$backend; Guard=$guardProcess; GuardStarted=$guardStarted}
    Close-WmsProductionHandles @cleanupArguments
}
