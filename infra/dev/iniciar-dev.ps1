[CmdletBinding()]
param(
    [ValidateRange(1024,65535)][int]$BackendPort = 25580,
    [ValidateRange(1024,65535)][int]$FrontendPort = 25581,
    [string]$AuthSurfaceOrigin = [Environment]::GetEnvironmentVariable('WMS_DEV_AUTH_SURFACE_ORIGIN','Process')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'console-dev.ps1')
. (Join-Path $PSScriptRoot 'espera-conexao.ps1')
. (Join-Path $PSScriptRoot 'artefato-backend.ps1')
. (Join-Path $PSScriptRoot 'reiniciar-instancia.ps1')
. (Join-Path $PSScriptRoot 'preparo-ambiente.ps1')
$connectionWaitPolicy = Get-WmsDevConnectionWaitPolicy
$runsRoot = Join-Path $repository 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs'
$runId = [guid]::NewGuid().ToString('D')
$runDirectory = Join-Path $runsRoot $runId
$null = New-Item -ItemType Directory -Path $runDirectory
$receiptPath = Join-Path $runDirectory 'launcher.json'
$receipt = [ordered]@{
    id='BE02-DEV-FONTE01'; natureza='D31_DEV02_LAUNCHER_REAL'; runId=$runId; inicioUtc=[DateTime]::UtcNow.ToString('o')
    projectPath=$repository; modo='real'; database='WMS_DEV'; login='WMSDEV'
    estado='PREPARING'; codigo=''; exitCode=$null; frontendUrl=$null
    backendPort=$BackendPort; frontendPort=$FrontendPort; guard=$null; backend=$null; frontend=$null
    sqlGuardInvoked=$false; backendStarted=$false; frontendStarted=$false
    realRoundtripVerified=$false; mockFallback=$false; existingProcessesAltered=$false
    restart=@{ stopped=@(); scope='WMS_DEV_ONLY_25580_25581' }
}
$backendHandle = $null
$backendResult = $null
$frontendHandle = $null
$guardProcess = $null
$guardStarted = $false
$frontendStarted = $false
$restartPlan = @()
$startupMutex = $null
$startupLock = $false

function Save-LauncherReceipt {
    $receipt['observadoUtc'] = [DateTime]::UtcNow.ToString('o')
    $json = $receipt | ConvertTo-Json -Depth 16
    [IO.File]::WriteAllText($receiptPath, $json + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
}

function Fail-Startup([int]$Code, [string]$Reason) {
    $receipt['estado']='BLOCKED'
    $receipt['codigo']=$Reason
    $receipt['exitCode']=$Code
    Save-LauncherReceipt
    Write-WmsDevConsoleFailure -Reason $Reason -ReceiptPath $receiptPath
    exit $Code
}

function Assert-FreePort([int]$Port) {
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 0) { Fail-Startup 50 ('PORT_OCCUPIED_' + $Port) }
}

function Quote-Argument([string]$Value) {
    if ($Value.Contains('"') -or $Value.EndsWith('\')) { throw 'INVALID_ARGUMENT_PATH' }
    return '"' + $Value + '"'
}

function Require-PublicIdentity {
    # D32: login proprio passa a ser o padrao. Externo continua explicito.
    if ($env:WMS_AUTH_MODE -ne 'externo') {
        . (Join-Path $repository 'infra/auth/login-protegido.ps1')
        try {
            # Somente metadados/ACL antes da guarda. DPAPI apenas no canal SQL01 apos gates.
            $nativePath=Get-WmsLoginMaterialPath
            Assert-WmsLoginPrivatePath (Split-Path $nativePath -Parent);Assert-WmsLoginPrivatePath $nativePath
            . (Join-Path $PSScriptRoot 'acesso-dev.ps1')
            $access=Get-WmsDevAccess $FrontendPort
            $native=@{native=$true;issuer='https://wms.localhost';jwkSetUri='https://wms.localhost/api/auth/jwks';audience='wms-dev';origin=$access.origin;proxyOrigin=$access.proxyOrigin;secureCookie=$access.secureCookie;accessMode=$access.mode}
            $receipt['identityConfiguration'] = @{ mode='native'; protectedMaterialPresent=$true }
            return $native
        } catch {
            if($_.Exception.Message -eq 'AUTH_DEV_ACCESS_MODE_INVALIDO') {
                Write-Host '[WMS DEV] Confira modo em infra\dev\acesso-dev.json: use local ou tunnel.'
                Fail-Startup 40 'AUTH_DEV_ACCESS_MODE_INVALIDO'
            }
            Write-Host '[WMS DEV] Falta configurar o login neste computador. Na pasta do projeto, execute uma unica vez:'
            Write-Host '[WMS DEV] infra\auth\configurar-login-dev.bat'
            Write-Host '[WMS DEV] Informe a senha inicial no console protegido. Depois execute iniciar-dev.bat novamente.'
            Fail-Startup 40 'AUTH_NATIVE_CONFIGURATION_REQUIRED'
        }
    }
    Fail-Startup 40 'DEV_CURRENT_NATIVE_AUTH_REQUIRED'
    $map = [ordered]@{ issuer='WMS_OIDC_ISSUER'; jwkSetUri='WMS_OIDC_JWK_SET_URI'; audience='WMS_OIDC_AUDIENCE' }
    $identity = @{}
    $missing = @()
    foreach ($key in $map.Keys) {
        $value = [Environment]::GetEnvironmentVariable($map[$key], 'Process')
        if ([string]::IsNullOrWhiteSpace($value)) { $missing += $map[$key] }
        else { $identity[$key]=$value }
    }
    $receipt['identityConfiguration'] = @{ publicFieldsPresent=@($map.Keys | ForEach-Object { @{name=$map[$_]; present=$identity.ContainsKey($_)} }) }
    if ($missing.Count -gt 0) { Fail-Startup 40 ('AUTH_CONFIGURATION_MISSING_' + ($missing -join '_')) }
    foreach ($key in @('issuer','jwkSetUri')) {
        $uri = $null
        if (-not [Uri]::TryCreate($identity[$key], [UriKind]::Absolute, [ref]$uri)) { Fail-Startup 40 'AUTH_CONFIGURATION_INVALID_URI' }
        if ($uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.Scheme -ne 'https') { Fail-Startup 40 'AUTH_CONFIGURATION_UNSAFE_URI' }
    }
    if ($identity.audience.Length -gt 200 -or $identity.audience -match '[\r\n]') { Fail-Startup 40 'AUTH_CONFIGURATION_INVALID_AUDIENCE' }
    return $identity
}

try {
    Initialize-WmsDevConsole
    Write-WmsDevConsoleStep 1 'Conferindo portas, login e arquivos...'
    Assert-WmsDevRestartPorts $BackendPort $FrontendPort
    $mutexHash = [Security.Cryptography.SHA256]::Create()
    try { $mutexKey = [BitConverter]::ToString($mutexHash.ComputeHash([Text.Encoding]::UTF8.GetBytes($repository.ToLowerInvariant()))).Replace('-','') }
    finally { $mutexHash.Dispose() }
    $startupMutex = [Threading.Mutex]::new($false, ('Local\WmsDevStartup_' + $mutexKey))
    try { $startupLock = $startupMutex.WaitOne(0) } catch [Threading.AbandonedMutexException] { $startupLock = $true }
    if (-not $startupLock) { Fail-Startup 50 'DEV_RESTART_IN_PROGRESS' }
    $restartPlan = @(Get-WmsDevRestartPlan $repository $BackendPort $FrontendPort $runDirectory)
    # Configuracao publica primeiro: nao abrir SQL quando a autenticacao nem esta configurada.
    $identity = Require-PublicIdentity
    $guardHelper = Join-Path $repository 'infra/dev02/guarda-wmsdev.ps1'
    $backendHelper = Join-Path $repository 'infra/sql01/launcher-backend.ps1'
    $preparer=Join-Path $PSScriptRoot 'preparar-backend-dev.ps1'
    foreach ($required in @($guardHelper,$backendHelper,$preparer)) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { Fail-Startup 30 'PREPARATION_ARTIFACT_MISSING' }
    }
    $prepared=& $preparer -RunDirectory $runDirectory
    $artifact=Assert-WmsDevCurrentArtifact $repository $prepared.path $prepared.sha256
    $receipt['candidate']=@{path=$prepared.path;sha256=$prepared.sha256}
    $receipt['fingerprint']=$artifact.fingerprint
    $receipt['artifactSourceHash'] = $artifact.sourceHash
    $nodeCommand = Get-Command node.exe -ErrorAction Stop
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    $frontendRoot = Join-Path $repository 'frontend'
    $vitePath = Join-Path $frontendRoot 'node_modules/vite/bin/vite.js'
    if (-not (Test-Path -LiteralPath $vitePath -PathType Leaf)) { Fail-Startup 60 'FRONTEND_DEPENDENCIES_MISSING' }
    Write-WmsDevConsoleStep 1 'Configuracao inicial conferida' -Done
    Write-WmsDevConsoleStep 2 'Atualizando e verificando o WMS_DEV...'
    $receipt['databasePreparation']=Start-WmsEnvironmentPreparation $repository 'DEV'
    Write-Host ('  Abertura SQL: ate '+$connectionWaitPolicy.ConnectTimeoutSeconds+'s; consultas: ate '+$connectionWaitPolicy.CommandTimeoutSeconds+'s cada; verificacao total: ate '+$connectionWaitPolicy.GuardTimeoutSeconds+'s.') -ForegroundColor DarkGray
    $receipt['connectionWaitPolicy'] = $connectionWaitPolicy

    # Uma guarda ATUAL neste run; nunca reutilizar bool, recibo antigo ou alvo presumido.
    $guardHash = (Get-FileHash -LiteralPath $guardHelper -Algorithm SHA256).Hash
    $guardInfo = [Diagnostics.ProcessStartInfo]::new()
    $guardInfo.FileName = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $guardInfo.Arguments = '-NoProfile -ExecutionPolicy Bypass -File ' + (Quote-Argument $guardHelper)
    $guardInfo.WorkingDirectory = $repository
    $guardInfo.UseShellExecute = $false
    $guardInfo.CreateNoWindow = $true
    $guardInfo.RedirectStandardOutput = $true
    $guardInfo.RedirectStandardError = $true
    $guardProcess = [Diagnostics.Process]::new()
    $guardProcess.StartInfo=$guardInfo
    $receipt['sqlGuardInvoked']=$true
    if (-not $guardProcess.Start()) { Fail-Startup 20 'GUARD_PROCESS_NOT_STARTED' }
    $guardStarted=$true
    $guardStdout = $guardProcess.StandardOutput.ReadToEndAsync()
    $guardStderr = $guardProcess.StandardError.ReadToEndAsync()
    $guardFinished = Wait-WmsDevGuardExit -Process $guardProcess -Timeout ([TimeSpan]::FromSeconds($connectionWaitPolicy.GuardTimeoutSeconds)) -ProgressInterval ([TimeSpan]::FromSeconds($connectionWaitPolicy.ProgressIntervalSeconds)) -OnProgress {
        param($elapsed, $maximum)
        Write-Host ('  {0}  Verificacao do banco em andamento... {1:N0}s / {2:N0}s' -f (Get-Date -Format 'HH:mm:ss'), $elapsed, $maximum) -ForegroundColor DarkGray
    }
    if (-not $guardFinished) { Fail-Startup 20 'GUARD_PROCESS_TIMEOUT' }
    $guardOutput=$guardStdout.Result
    $null=$guardStderr.Result # Erros nao sao copiados para evitar dados nao sanitizados.
    $guardReceiptPath = Join-Path $runDirectory 'guarda.json'
    try { $guard = $guardOutput | ConvertFrom-Json -ErrorAction Stop } catch { Fail-Startup 20 'GUARD_OUTPUT_INVALID' }
    [IO.File]::WriteAllText($guardReceiptPath,$guardOutput,[Text.UTF8Encoding]::new($false))
    $guardReceiptHash=(Get-FileHash -LiteralPath $guardReceiptPath -Algorithm SHA256).Hash
    $receipt['guard']=@{ path=$guardReceiptPath; sha256=$guardReceiptHash; helperSha256=$guardHash; processId=$guardProcess.Id; exitCode=$guardProcess.ExitCode }
    if ($guardProcess.ExitCode -ne 0) {
        Write-Host ('[WMS DEV] '+(Get-WmsDevGuardFailureMessage -Guard $guard -Policy $connectionWaitPolicy))
        Write-Host ('[WMS DEV] Detalhes da verificacao: ' + $guardReceiptPath)
        Fail-Startup 20 'GUARD_CURRENT_BLOCKED'
    }
    if ((Get-FileHash -LiteralPath $guardHelper -Algorithm SHA256).Hash -ne $guardHash) { Fail-Startup 20 'GUARD_HELPER_CHANGED' }
    . (Join-Path $repository 'backend/scripts/dev/start-backend.ps1')
    $null=Assert-WmsFrontendDevGuard -GuardReceipt $guardReceiptPath -GuardSha256 $guardReceiptHash -GuardProcessId $guardProcess.Id -GuardHelperSha256 $guardHash -RunDirectory $runDirectory
    if(@($guard.checks).Count -ne 26 -or $guard.aberturaTentativas -ne 1 -or !$guard.aberturaConcluida){Fail-Startup 20 'GUARD_CURRENT_INCOMPLETE'}
    # Build/selecionar/guardas falhos e fonte alterada param ANTES do primeiro encerramento.
    $null=Assert-WmsDevCurrentArtifact $repository $prepared.path $prepared.sha256
    $receipt['preStopCurrentArtifactVerifiedUtc']=[DateTime]::UtcNow.ToString('o')
    $ownership=@{id='BE02-DEV-FONTE01';runId=$runId;observadoUtc=[DateTime]::UtcNow.ToString('o');scope='ONLY_WMS_DEV_25580_25581';targets=@($restartPlan|ForEach-Object{@{pid=$_.Process.Id;startUtc=[DateTimeOffset]::Parse((Read-WmsDevRestartReceipt $_.Snapshot.path).body.startUtc).UtcDateTime.ToString('o');port=$_.Port;receipt=@{path=$_.ReceiptPath;sha256=$_.ReceiptSha256};snapshot=$_.Snapshot}})}
    $ownershipPath=Join-Path $runDirectory 'posse-anterior.json'
    Write-WmsDevCreateOnlyJson $ownershipPath $ownership
    $receipt['ownership']=@{path=$ownershipPath;sha256=(Get-FileHash -LiteralPath $ownershipPath).Hash}
    Write-WmsDevConsoleStep 2 'Banco WMS_DEV e conexao segura conferidos' -Done
    if ($restartPlan.Count) { Write-Host '  Reiniciando somente a instancia DEV deste WMS (25580/25581)...' -ForegroundColor Yellow }
    Stop-WmsDevRestartPlan $restartPlan $repository {
        param($stopped)
        $receipt['restart']['stopped'] += $stopped
        $receipt['existingProcessesAltered'] = $true
        Save-LauncherReceipt
    }
    Assert-FreePort $BackendPort
    Assert-FreePort $FrontendPort
    Write-WmsDevConsoleStep 3 'Iniciando backend e aguardando resposta...'

    # O helper backend valida o esquema e a procedencia da guarda antes de DPAPI/Java.
    . $backendHelper
    $null=Assert-WmsDevCurrentArtifact $repository $prepared.path $prepared.sha256
    $origin=if($identity.accessMode -ceq 'tunnel'){$identity.proxyOrigin}else{$identity.origin}
    $backend = Start-WmsSql01Backend -DevCurrent -Diagnostico -Candidato $prepared.path -CandidateSha256 $prepared.sha256 -Port $BackendPort -RunDirectory $runDirectory -GuardReceipt $guardReceiptPath -GuardSha256 $guardReceiptHash -LocalOrigin $origin
    $backendResult=$backend
    $backendHandle=$backend.Process
    $receipt['backend']=$backend.Sanitized
    $receipt['backendStarted']=($null -ne $backendHandle)
    if ($backend.Sanitized.estado -ne 'READY') { Fail-Startup 30 'BACKEND_NOT_READY' }
    Write-WmsDevConsoleStep 3 ('Backend disponivel na porta '+$BackendPort) -Done
    Write-WmsDevConsoleStep 4 'Iniciando frontend e conferindo a pagina...'

    # Contrato nao sensivel FE, vinculado a guarda e ao processo BE real deste run.
    $chain = [ordered]@{
        id='BE02-DEV-FONTE01'; natureza='D31_DEV02_INTEGRACAO_PRONTA'; runId=$runId; projectPath=$repository; observadoUtc=[DateTime]::UtcNow.ToString('o')
        candidate=$receipt.candidate;fingerprint=$artifact.fingerprint;jdbc=$backend.Sanitized.jdbc
        guard=@{ path=$guardReceiptPath; sha256=$guardReceiptHash; helperSha256=$guardHash; processId=$guardProcess.Id }
        backend=@{ receiptPath=$backend.ReceiptPath; receiptSha256=$backend.ReceiptSha256; pid=$backendHandle.Id; url=('http://127.0.0.1:'+$BackendPort) }
        frontend=@{ port=$FrontendPort; modo='real'; proxyPrefix='/api'; mockFallback=$false }
        publicIdentity=@{ issuer=$identity.issuer; jwkSetUri=$identity.jwkSetUri; audience=$identity.audience }
        authenticatedBusinessRead=$false
    }
    $chainPath=Join-Path $runDirectory 'integracao-pronta.json'
    [IO.File]::WriteAllText($chainPath,($chain|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
    $chainHash=(Get-FileHash -LiteralPath $chainPath -Algorithm SHA256).Hash
    $frontendInfo=[Diagnostics.ProcessStartInfo]::new()
    $frontendInfo.FileName=$nodeCommand.Source
    $frontendInfo.Arguments=(Quote-Argument $vitePath)+' --config vite.dev.config.ts'
    $frontendInfo.WorkingDirectory=$frontendRoot
    $frontendInfo.UseShellExecute=$false
    $frontendInfo.CreateNoWindow=$true
    $frontendInfo.RedirectStandardOutput=$true
    $frontendInfo.RedirectStandardError=$true
    $frontendInfo.EnvironmentVariables.Clear()
    foreach ($name in @('PATH','SystemRoot','WINDIR','TEMP','TMP','COMSPEC','PATHEXT','LOCALAPPDATA','APPDATA','USERPROFILE','NUMBER_OF_PROCESSORS','PROCESSOR_ARCHITECTURE')) {
        $value=[Environment]::GetEnvironmentVariable($name,'Process')
        if ($null -ne $value) { $frontendInfo.EnvironmentVariables[$name]=$value }
    }
    $frontendInfo.EnvironmentVariables['NODE_ENV']='development'
    $frontendInfo.EnvironmentVariables['VITE_DATA_MODE']='real'
    $frontendInfo.EnvironmentVariables['WMS_FE_PORT']=[string]$FrontendPort
    $frontendInfo.EnvironmentVariables['WMS_DEV_BACKEND_URL']='http://127.0.0.1:'+$BackendPort
    $frontendInfo.EnvironmentVariables['WMS_DEV_READY_PATH']=$chainPath
    $frontendInfo.EnvironmentVariables['WMS_DEV_READY_SHA256']=$chainHash
    $frontendHandle=[Diagnostics.Process]::new()
    $frontendHandle.StartInfo=$frontendInfo
    if (-not $frontendHandle.Start()) { Fail-Startup 60 'FRONTEND_PROCESS_NOT_STARTED' }
    $frontendStarted=$true
    $receipt['frontendStarted']=$true
    $frontendOut=$frontendHandle.StandardOutput.ReadToEndAsync()
    $frontendErr=$frontendHandle.StandardError.ReadToEndAsync()
    $deadline=[DateTime]::UtcNow.AddSeconds(30)
    $frontendReady=$false
    while ([DateTime]::UtcNow -lt $deadline) {
        if ($frontendHandle.HasExited -or $backendHandle.HasExited) { break }
        $listener=@(Get-NetTCPConnection -State Listen -LocalAddress 127.0.0.1 -LocalPort $FrontendPort -ErrorAction SilentlyContinue)
        if ($listener.Count -eq 1 -and $listener[0].OwningProcess -eq $frontendHandle.Id) {
            $frontendReady=$true;break
        }
        Start-Sleep -Milliseconds 300
    }
    if (-not $frontendReady) { Fail-Startup 60 'FRONTEND_NOT_READY' }
    $receipt['frontend']=@{ pid=$frontendHandle.Id; startUtc=$frontendHandle.StartTime.ToUniversalTime().ToString('o'); listenerOwned=$true; mode='real'; chainPath=$chainPath; chainSha256=$chainHash; cssVerificationPending=$true }
    $tunnelMode=$identity.Contains('native') -and $identity.native -and $identity.accessMode -eq 'tunnel'
    $receipt['frontendUrl']='http://127.0.0.1:'+$FrontendPort
    $receipt['accessMode']=if($tunnelMode){'tunnel'}else{'local'}
    $receipt['estado']='PROCESSES_READY_ROUNDTRIP_PENDING'
    $receipt['codigo']='API_AUTHENTICATED_BROWSER_CHECK_REQUIRED'
    $receipt['exitCode']=0
    Save-LauncherReceipt
    $gate=New-WmsDevCurrentHttpGate -RuntimePath $backend.RuntimeReceiptPath -RuntimeSha256 $backend.RuntimeReceiptSha256 -LauncherPath $receiptPath -AuthSurfaceOrigin $AuthSurfaceOrigin
    Write-Host ('[WMS DEV] Gate API/browser atual: '+$gate.path+' SHA256 '+$gate.sha256)
    Write-WmsDevConsoleStep 4 'Frontend disponivel' -Done
    $startupMutex.ReleaseMutex()
    $startupLock = $false
    Write-WmsDevConsoleReady -FrontendUrl $receipt.frontendUrl -BackendPort $BackendPort -ReceiptPath $receiptPath -TunnelMode:$tunnelMode -FrontendPort $FrontendPort
    while (-not $frontendHandle.HasExited -and -not $backendHandle.HasExited) { Start-Sleep -Milliseconds 500 }
    $receipt['estado']='OWN_PROCESS_EXITED'; $receipt['observadoUtc']=[DateTime]::UtcNow.ToString('o')
    Save-LauncherReceipt
    Write-Host ''
    Write-Host '  [ENCERRADO] Um dos processos do WMS foi encerrado.' -ForegroundColor Yellow
    Write-Host '  Execute iniciar-dev.bat quando quiser iniciar novamente.' -ForegroundColor Yellow
    exit 1
} catch {
    if ($_.Exception.Message -match '^WMS_PREP_[A-Z_]+$') { Fail-Startup 20 $_.Exception.Message }
    # Nao imprimir excecao arbitraria: pode conter configuracao ou argumento sensivel.
    if ($_.Exception.Message -match '^DEV_RESTART_[A-Z_]+$') { Fail-Startup 50 $_.Exception.Message }
    if ($_.Exception.Message -match '^DEV_CURRENT_[A-Z_]+$') { Fail-Startup 30 $_.Exception.Message }
    Fail-Startup 30 'LOCAL_STARTUP_FAILED'
} finally {
    foreach ($target in $restartPlan) { $target.Process.Dispose() }
    # Um objeto Process sem Start bem-sucedido nao possui HasExited valido.
    # Uma falha ao limpar FE nao pode impedir a limpeza do BE proprio.
    try {
        if ($frontendStarted -and $null -ne $frontendHandle -and -not $frontendHandle.HasExited) {
            $frontendHandle.Kill()
            $frontendHandle.WaitForExit()
        }
    } finally {
        try {
            if ($null -ne $backendHandle) { Stop-WmsSql01Backend -Handle $backendResult }
        } finally {
            try {
                if ($null -ne $guardProcess) {
                    if ($guardStarted -and -not $guardProcess.HasExited) { $guardProcess.Kill(); $guardProcess.WaitForExit() }
                    $guardProcess.Dispose()
                }
            } finally {
                if ($null -ne $startupMutex) {
                    if ($startupLock) { $startupMutex.ReleaseMutex() }
                    $startupMutex.Dispose()
                }
            }
        }
    }
}
