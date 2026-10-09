[CmdletBinding()]
param(
    [ValidateRange(1024,65535)][int]$BackendPort = 25580,
    [ValidateRange(1024,65535)][int]$FrontendPort = 25581
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$runsRoot = Join-Path $repository 'orchestracao/.runtime/frontend-integracao-dev-runs'
$runId = [guid]::NewGuid().ToString('N')
$runDirectory = Join-Path $runsRoot $runId
$null = New-Item -ItemType Directory -Path $runDirectory -Force
$receiptPath = Join-Path $runDirectory 'launcher.json'
$receipt = [ordered]@{
    natureza='D31_DEV02_LAUNCHER_REAL'; runId=$runId; inicioUtc=[DateTime]::UtcNow.ToString('o')
    projectPath=$repository; modo='real'; database='WMS_DEV'; login='WMSDEV'
    estado='PREPARING'; codigo=''; exitCode=$null; frontendUrl=$null
    backendPort=$BackendPort; frontendPort=$FrontendPort; guard=$null; backend=$null; frontend=$null
    sqlGuardInvoked=$false; backendStarted=$false; frontendStarted=$false
    realRoundtripVerified=$false; mockFallback=$false; existingProcessesAltered=$false
}
$backendHandle = $null
$backendResult = $null
$frontendHandle = $null
$guardProcess = $null
$guardStarted = $false
$frontendStarted = $false

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
    Write-Host ('[WMS DEV] BLOQUEADO: ' + $Reason)
    Write-Host ('[WMS DEV] Recibo: ' + $receiptPath)
    Write-Host '[WMS DEV] Nenhuma integracao real e declarada; nao ha fallback ficticio.'
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
    Write-Host '[WMS DEV] Desenvolvimento REAL: backend + frontend, somente WMS_DEV/WMSDEV.'
    Write-Host '[WMS DEV] Processos existentes serao preservados. Credenciais nunca sao solicitadas aqui.'
    if ($BackendPort -eq $FrontendPort) { Fail-Startup 50 'BACKEND_FRONTEND_PORTS_EQUAL' }
    Assert-FreePort $BackendPort
    Assert-FreePort $FrontendPort
    # Configuracao publica primeiro: nao abrir SQL quando a autenticacao nem esta configurada.
    $identity = Require-PublicIdentity
    $guardHelper = Join-Path $repository 'infra/dev02/guarda-wmsdev.ps1'
    $backendHelper = Join-Path $repository 'backend/scripts/dev/start-backend.ps1'
    $artifactReceiptPath = Join-Path $repository 'backend/evidencias/frontend-dev02-preparo.json'
    foreach ($required in @($guardHelper,$backendHelper,$artifactReceiptPath)) {
        if (-not (Test-Path -LiteralPath $required -PathType Leaf)) { Fail-Startup 30 'PREPARATION_ARTIFACT_MISSING' }
    }
    $nodeCommand = Get-Command node.exe -ErrorAction Stop
    $javaCommand = Get-Command java.exe -ErrorAction SilentlyContinue
    $frontendRoot = Join-Path $repository 'frontend'
    $vitePath = Join-Path $frontendRoot 'node_modules/vite/bin/vite.js'
    if (-not (Test-Path -LiteralPath $vitePath -PathType Leaf)) { Fail-Startup 60 'FRONTEND_DEPENDENCIES_MISSING' }

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
    if (-not $guardProcess.WaitForExit(90000)) { Fail-Startup 20 'GUARD_PROCESS_TIMEOUT' }
    $guardOutput=$guardStdout.Result
    $null=$guardStderr.Result # Erros nao sao copiados para evitar dados nao sanitizados.
    $guardReceiptPath = Join-Path $runDirectory 'guarda.json'
    try { $guard = $guardOutput | ConvertFrom-Json -ErrorAction Stop } catch { Fail-Startup 20 'GUARD_OUTPUT_INVALID' }
    [IO.File]::WriteAllText($guardReceiptPath,$guardOutput,[Text.UTF8Encoding]::new($false))
    $guardReceiptHash=(Get-FileHash -LiteralPath $guardReceiptPath -Algorithm SHA256).Hash
    $receipt['guard']=@{ path=$guardReceiptPath; sha256=$guardReceiptHash; helperSha256=$guardHash; processId=$guardProcess.Id; exitCode=$guardProcess.ExitCode }
    if ($guardProcess.ExitCode -ne 0) { Fail-Startup 20 'GUARD_CURRENT_BLOCKED' }
    if ((Get-FileHash -LiteralPath $guardHelper -Algorithm SHA256).Hash -ne $guardHash) { Fail-Startup 20 'GUARD_HELPER_CHANGED' }

    # O helper backend valida o esquema e a procedencia da guarda antes de DPAPI/Java.
    . $backendHelper
    $artifact = Get-Content -LiteralPath $artifactReceiptPath -Raw -Encoding UTF8 | ConvertFrom-Json
    $javaPath = $artifact.java.path
    if ([string]::IsNullOrWhiteSpace($javaPath) -and $javaCommand) { $javaPath=$javaCommand.Source }
    $backend = Start-WmsFrontendDevBackend -Port $BackendPort -RunDirectory $runDirectory -GuardReceipt $guardReceiptPath -GuardSha256 $guardReceiptHash -GuardProcessId $guardProcess.Id -GuardHelperSha256 $guardHash -JarPath $artifact.jar.path -JarSha256 $artifact.jar.sha256 -JavaPath $javaPath -Identity $identity
    $backendResult=$backend
    $backendHandle=$backend.Process
    $receipt['backend']=$backend.Sanitized
    $receipt['backendStarted']=($null -ne $backendHandle)
    if ($backend.Sanitized.estado -ne 'READY') { Fail-Startup 30 'BACKEND_NOT_READY' }

    # Contrato nao sensivel FE, vinculado a guarda e ao processo BE real deste run.
    $chain = [ordered]@{
        natureza='D31_DEV02_INTEGRACAO_PRONTA'; runId=$runId; projectPath=$repository; observadoUtc=[DateTime]::UtcNow.ToString('o')
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
            try {
                $html=Invoke-WebRequest -Uri ('http://127.0.0.1:'+$FrontendPort+'/') -UseBasicParsing -TimeoutSec 3
                if ($html.StatusCode -eq 200 -and $html.Content -match 'WMS') { $frontendReady=$true; break }
            } catch { }
        }
        Start-Sleep -Milliseconds 300
    }
    if (-not $frontendReady) { Fail-Startup 60 'FRONTEND_NOT_READY' }
    $receipt['frontend']=@{ pid=$frontendHandle.Id; startUtc=$frontendHandle.StartTime.ToUniversalTime().ToString('o'); listenerOwned=$true; mode='real'; chainPath=$chainPath; chainSha256=$chainHash; cssVerificationPending=$true }
    $receipt['frontendUrl']='http://127.0.0.1:'+$FrontendPort
    $receipt['estado']='PROCESSES_READY_ROUNDTRIP_PENDING'
    $receipt['codigo']='API_AUTHENTICATED_BROWSER_CHECK_REQUIRED'
    $receipt['exitCode']=0
    Save-LauncherReceipt
    Write-Host ('[WMS DEV] Frontend REAL: '+$receipt.frontendUrl)
    Write-Host '[WMS DEV] Backend e frontend proprios prontos; consulta autenticada nao e presumida.'
    Write-Host ('[WMS DEV] Recibo: '+$receiptPath)
    Write-Host '[WMS DEV] Mantenha este console aberto. Ctrl+C encerra somente processos criados aqui.'
    while (-not $frontendHandle.HasExited -and -not $backendHandle.HasExited) { Start-Sleep -Milliseconds 500 }
    $receipt['estado']='OWN_PROCESS_EXITED'; $receipt['observadoUtc']=[DateTime]::UtcNow.ToString('o')
    Save-LauncherReceipt
    exit 1
} catch {
    # Nao imprimir excecao arbitraria: pode conter configuracao ou argumento sensivel.
    Fail-Startup 30 'LOCAL_STARTUP_FAILED'
} finally {
    # Um objeto Process sem Start bem-sucedido nao possui HasExited valido.
    # Uma falha ao limpar FE nao pode impedir a limpeza do BE proprio.
    try {
        if ($frontendStarted -and $null -ne $frontendHandle -and -not $frontendHandle.HasExited) {
            $frontendHandle.Kill()
            $frontendHandle.WaitForExit()
        }
    } finally {
        try {
            if ($null -ne $backendHandle) { Stop-WmsFrontendDevBackend -Handle $backendResult }
        } finally {
            if ($null -ne $guardProcess) {
                if ($guardStarted -and -not $guardProcess.HasExited) { $guardProcess.Kill(); $guardProcess.WaitForExit() }
                $guardProcess.Dispose()
            }
        }
    }
}
