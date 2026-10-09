# Biblioteca do launcher de producao. Dot-source nao inicia SQL/processos.
Set-StrictMode -Version Latest

function Assert-WmsProductionPublicConfig {
    param([Parameter(Mandatory=$true)][object]$Config)
    foreach($name in @('database','expectedLogin','profile','bindAddress','frontendOrigin','backendPublicOrigin','apiBrowserBase')) {
        if($Config.$name -isnot [string]) { throw 'PROD_PUBLIC_CONFIG_TYPE_INVALID' }
    }
    if ($Config.schemaVersion -isnot [int] -or $Config.schemaVersion -ne 1 -or $Config.database -cne 'WMS_PROD' -or
        $Config.expectedLogin -cne 'WMSPROD' -or $Config.profile -cne 'sqlserver-prod') {
        throw 'PROD_TARGET_OR_IDENTITY_INVALID'
    }
    if ($Config.bindAddress -cne '127.0.0.1' -or
        $Config.frontendOrigin -cne 'https://wms.rodogarcia.com.br' -or
        $Config.backendPublicOrigin -cne 'https://wms-api.rodogarcia.com.br' -or
        $Config.apiBrowserBase -cne '/api' -or $Config.secureCookie -isnot [bool] -or $Config.secureCookie -ne $true -or
        $Config.cloudflareConfigured -isnot [bool] -or $Config.cloudflareConfigured -ne $false) { throw 'PROD_PUBLIC_TOPOLOGY_INVALID' }
    foreach ($name in @('backendPort','frontendPort')) {
        $port = $Config.$name
        if ($port -isnot [int] -and $port -isnot [long]) { throw 'PROD_PORT_INVALID' }
        if ($port -lt 1024 -or $port -gt 65535) { throw 'PROD_PORT_INVALID' }
    }
    if ($Config.backendPort -eq $Config.frontendPort) { throw 'PROD_PORTS_EQUAL' }
}

function Assert-WmsProductionFreePort {
    param([int]$Port)
    $listeners = @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue)
    if ($listeners.Count -gt 0) { throw ('PROD_PORT_OCCUPIED_' + $Port) }
}

function Get-WmsProductionFailureExitCode {
    param([string]$Reason)
    if ($Reason -match '^PROD_PORT') { return 50 }
    if ($Reason -match '^PROD_AUTH|^PROD_IDENTITY') { return 40 }
    if ($Reason -match '^PROD_GUARD|^PROD_CURRENT_GUARD') { return 20 }
    if ($Reason -match '^PROD_FRONTEND_(NOT_STARTED|READINESS)') { return 60 }
    return 30
}

function ConvertTo-WmsProductionArgument {
    param([string]$Value)
    if ([string]::IsNullOrWhiteSpace($Value) -or $Value.Contains('"') -or
        $Value.EndsWith('\') -or $Value -match '[\r\n]') { throw 'PROD_ARGUMENT_INVALID' }
    return '"' + $Value + '"'
}

function Stop-WmsProductionChild {
    param([object]$Process, [bool]$Started)
    if ($null -eq $Process) { return }
    try {
        if ($Started -and -not $Process.HasExited) {
            $Process.Kill()
            $null = $Process.WaitForExit(10000)
        }
    } finally { $Process.Dispose() }
}

function Close-WmsProductionHandles {
    param([object]$Frontend, [bool]$FrontendStarted, [object]$Backend,
        [object]$Guard, [bool]$GuardStarted)
    # Cada handle criado neste run recebe limpeza mesmo se outra limpeza falhar.
    try { Stop-WmsProductionChild -Process $Frontend -Started $FrontendStarted }
    finally {
        try {
            if ($null -ne $Backend) { Stop-WmsProductionBackend -Handle $Backend }
        } finally { Stop-WmsProductionChild -Process $Guard -Started $GuardStarted }
    }
}

function New-WmsProductionChildInfo {
    param([string]$Executable, [string]$Arguments, [string]$WorkingDirectory)
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $Executable
    $info.Arguments = $Arguments
    $info.WorkingDirectory = $WorkingDirectory
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.EnvironmentVariables.Clear()
    foreach ($name in @('PATH','SystemRoot','WINDIR','TEMP','TMP','COMSPEC','PATHEXT',
        'LOCALAPPDATA','APPDATA','USERPROFILE','NUMBER_OF_PROCESSORS','PROCESSOR_ARCHITECTURE')) {
        $value = [Environment]::GetEnvironmentVariable($name,'Process')
        if ($null -ne $value) { $info.EnvironmentVariables[$name] = $value }
    }
    return $info
}
