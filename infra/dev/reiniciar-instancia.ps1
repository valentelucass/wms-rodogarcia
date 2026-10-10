# Biblioteca local: nao inicia processos, SQL ou encerramento ao ser carregada.
Set-StrictMode -Version Latest

function Assert-WmsDevRestartPorts([int]$BackendPort, [int]$FrontendPort) {
    if ($BackendPort -ne 25580 -or $FrontendPort -ne 25581) { throw 'DEV_RESTART_PORTS_NOT_ALLOWED' }
}

function Get-WmsDevListeners {
    @(Get-NetTCPConnection -State Listen -ErrorAction Stop)
}

function Split-WmsDevCommandLine([string]$CommandLine) {
    if (-not ('WmsDevRestart.CommandLine' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
namespace WmsDevRestart {
    public static class CommandLine {
        [DllImport("shell32.dll", SetLastError=true, CharSet=CharSet.Unicode)]
        public static extern IntPtr CommandLineToArgvW(string command, out int count);
        [DllImport("kernel32.dll")] public static extern IntPtr LocalFree(IntPtr memory);
    }
}
'@
    }
    $count = 0
    $pointer = [WmsDevRestart.CommandLine]::CommandLineToArgvW($CommandLine, [ref]$count)
    if ($pointer -eq [IntPtr]::Zero) { throw 'DEV_RESTART_COMMAND_UNREADABLE' }
    try {
        for ($i=0; $i -lt $count; $i++) {
            [Runtime.InteropServices.Marshal]::PtrToStringUni([Runtime.InteropServices.Marshal]::ReadIntPtr($pointer, $i * [IntPtr]::Size))
        }
    } finally { $null = [WmsDevRestart.CommandLine]::LocalFree($pointer) }
}

function Test-WmsDevProcessProof($Receipt, $Process, [string[]]$Arguments, [string]$Executable, [string]$Repository, [int]$Port) {
    try {
        $root = [IO.Path]::GetFullPath($Repository).TrimEnd('\','/')
        if ([IO.Path]::GetFullPath($Receipt.projectPath).TrimEnd('\','/') -ine $root -or
            $Receipt.natureza -cne 'D31_DEV02_LAUNCHER_REAL' -or $Receipt.modo -cne 'real' -or
            $Receipt.database -cne 'WMS_DEV' -or $Receipt.login -cne 'WMSDEV' -or
            $Receipt.backendPort -ne 25580 -or $Receipt.frontendPort -ne 25581 -or
            $Port -notin @(25580,25581)) { return $false }
        if ($Port -eq 25580) {
            $be = $Receipt.backend
            $proof = $be.backend
            if ($be.natureza -cne 'D31_DEV02_BACKEND_READINESS' -or $be.profile -cne 'sqlserver-dev' -or
                $be.database -cne 'WMS_DEV' -or $be.login -cne 'WMSDEV' -or $proof.port -ne $Port -or
                $Executable -ine $be.java.path -or [IO.Path]::GetFileName($Executable) -ine 'java.exe') { return $false }
            $jar = [IO.Path]::GetFullPath($be.artifact.path)
            if (-not $jar.StartsWith($root + '\', [StringComparison]::OrdinalIgnoreCase)) { return $false }
            $config = Join-Path $root 'backend/src/main/resources/application-frontend-dev.properties'
            $expected = @($Executable, '-jar', $jar, '--spring.profiles.active=sqlserver-dev',
                '--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-dev.properties',
                ('--spring.config.additional-location=' + ([Uri]$config).AbsoluteUri),
                '--server.address=127.0.0.1', '--server.port=25580', '--wms.cadastros.enabled=true',
                '--spring.autoconfigure.exclude=', '--spring.jpa.hibernate.ddl-auto=validate', '--spring.jpa.generate-ddl=false',
                '--spring.sql.init.mode=never', '--spring.flyway.enabled=false', '--spring.liquibase.enabled=false',
                '--wms.database.name=WMS_DEV', '--wms.database.user=WMSDEV')
        } else {
            $proof = $Receipt.frontend
            if ($proof.mode -cne 'real' -or [IO.Path]::GetFileName($Executable) -ine 'node.exe') { return $false }
            $expected = @($Executable, (Join-Path $root 'frontend/node_modules/vite/bin/vite.js'), '--config', 'vite.dev.config.ts')
        }
        if (-not $proof.listenerOwned -or $proof.pid -ne $Process.Id -or
            [DateTimeOffset]::Parse($proof.startUtc).UtcDateTime.Ticks -ne $Process.StartTime.ToUniversalTime().Ticks -or
            $Arguments.Count -ne $expected.Count) { return $false }
        for ($i=0; $i -lt $expected.Count; $i++) {
            if ($Arguments[$i] -cne $expected[$i]) { return $false }
        }
        return $true
    } catch { return $false }
}

function Assert-WmsDevRestartTarget($Target, [string]$Repository, $Listeners) {
    if ($Target.Process.HasExited) { return }
    $owned = @($Listeners | Where-Object { $_.OwningProcess -eq $Target.Process.Id })
    if (@($owned | Where-Object { $_.LocalPort -ne $Target.Port -or $_.LocalAddress -cne '127.0.0.1' }).Count) {
        throw 'DEV_RESTART_OTHER_LISTENER_REFUSED'
    }
    $metadata = Get-CimInstance Win32_Process -Filter ('ProcessId=' + $Target.Process.Id) -ErrorAction Stop
    if ($Target.Process.HasExited) { return }
    if ($null -eq $metadata -or [string]::IsNullOrWhiteSpace($metadata.CommandLine)) { throw 'DEV_RESTART_PROCESS_UNREADABLE' }
    $arguments = @(Split-WmsDevCommandLine $metadata.CommandLine)
    if (-not (Test-WmsDevProcessProof $Target.Receipt $Target.Process $arguments $metadata.ExecutablePath $Repository $Target.Port)) {
        throw 'DEV_RESTART_OWNER_NOT_CONFIRMED'
    }
}

function Get-WmsDevRestartPlan([string]$Repository, [int]$BackendPort, [int]$FrontendPort) {
    Assert-WmsDevRestartPorts $BackendPort $FrontendPort
    $listeners = @(Get-WmsDevListeners)
    $plan = [Collections.Generic.List[object]]::new()
    try {
        foreach ($port in @($FrontendPort,$BackendPort)) {
            $owners = @($listeners | Where-Object { $_.LocalPort -eq $port } | Select-Object -ExpandProperty OwningProcess -Unique)
            if (-not $owners.Count) { continue }
            if ($owners.Count -ne 1) { throw 'DEV_RESTART_OWNER_NOT_CONFIRMED' }
            $process = Get-Process -Id $owners[0] -ErrorAction Stop
            $matched = $null
            try {
                # Fixar o handle nativo antes da validacao impede encerrar um PID reutilizado.
                $null = $process.Handle
                $runs = Join-Path $Repository 'orchestracao/.runtime/frontend-integracao-dev-runs'
                if ((Get-Item -LiteralPath $runs).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'DEV_RESTART_OWNER_NOT_CONFIRMED' }
                foreach ($directory in @(Get-ChildItem -LiteralPath $runs -Directory -ErrorAction Stop | Sort-Object LastWriteTime -Descending)) {
                    if ($directory.Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
                    $path = Join-Path $directory.FullName 'launcher.json'
                    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { continue }
                    if ((Get-Item -LiteralPath $path).Attributes -band [IO.FileAttributes]::ReparsePoint) { continue }
                    try {
                        $receipt = Get-Content -LiteralPath $path -Raw -Encoding UTF8 | ConvertFrom-Json
                        $candidate = [pscustomobject]@{Port=$port;Process=$process;Receipt=$receipt;PreviousRun=$directory.Name}
                        Assert-WmsDevRestartTarget $candidate $Repository $listeners
                        $matched = $candidate
                        break
                    } catch { continue }
                }
                if ($null -eq $matched) { throw 'DEV_RESTART_OWNER_NOT_CONFIRMED' }
                $plan.Add($matched)
            } finally { if ($null -eq $matched) { $process.Dispose() } }
        }
        return $plan.ToArray()
    } catch {
        foreach ($target in $plan) { $target.Process.Dispose() }
        throw
    }
}

function Stop-WmsDevRestartPlan($Plan, [string]$Repository, [scriptblock]$OnStopped) {
    # Conferir TODOS antes do primeiro encerramento, inclusive trocas de dono durante a guarda SQL.
    $listeners = @(Get-WmsDevListeners)
    foreach ($port in @(25580,25581)) {
        foreach ($listener in @($listeners | Where-Object { $_.LocalPort -eq $port })) {
            if (-not @($Plan | Where-Object { $_.Port -eq $port -and -not $_.Process.HasExited -and $_.Process.Id -eq $listener.OwningProcess }).Count) {
                throw 'DEV_RESTART_OWNER_CHANGED'
            }
        }
    }
    foreach ($target in $Plan) { Assert-WmsDevRestartTarget $target $Repository $listeners }
    foreach ($target in $Plan) {
        if ($target.Process.HasExited) { continue }
        Assert-WmsDevRestartTarget $target $Repository @(Get-WmsDevListeners)
        if ($target.Process.HasExited) { continue }
        try { $target.Process.Kill() }
        catch {
            # O launcher anterior pode limpar seu backend ao observar o frontend sair.
            if ($target.Process.HasExited) { continue }
            throw 'DEV_RESTART_STOP_FAILED'
        }
        & $OnStopped ([ordered]@{pid=$target.Process.Id;port=$target.Port;previousRun=$target.PreviousRun;stoppedUtc=[DateTime]::UtcNow.ToString('o')})
        if (-not $target.Process.WaitForExit(5000)) { throw 'DEV_RESTART_STOP_TIMEOUT' }
    }
    if (@(Get-WmsDevListeners | Where-Object { $_.LocalPort -in @(25580,25581) }).Count) { throw 'DEV_RESTART_PORT_NOT_RELEASED' }
}
