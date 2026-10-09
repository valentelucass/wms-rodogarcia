# Processos ficticios proprios. Nenhum SQL, credencial, backend ou HTTP.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'espera-conexao.ps1')
$checks = 0
$fixtures = New-Object 'Collections.Generic.List[Diagnostics.Process]'

function Start-WmsWaitFixture([int]$SleepMilliseconds, [int]$ExitCode) {
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $info.Arguments = '-NoProfile -NonInteractive -Command "Start-Sleep -Milliseconds ' + $SleepMilliseconds + '; exit ' + $ExitCode + '"'
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $info
    if (-not $process.Start()) { throw 'FIXTURE_START_FAILED' }
    $fixtures.Add($process)
    return $process
}

try {
    $policy = Get-WmsDevConnectionWaitPolicy
    # A guarda precisa ter margem para preparo/SELECTs e caber no recibo de 5 min.
    if ($policy.ConnectTimeoutSeconds -le 30 -or $policy.GuardTimeoutSeconds -le $policy.ConnectTimeoutSeconds + 36 -or
        $policy.GuardTimeoutSeconds -ge 300 -or $policy.ProgressIntervalSeconds -ge $policy.ConnectTimeoutSeconds) { throw 'WAIT_BUDGET_INVALID' }
    $checks++

    $fast = Start-WmsWaitFixture 1 0
    if (-not (Wait-WmsDevGuardExit -Process $fast -Timeout ([TimeSpan]::FromSeconds(5)) -ProgressInterval ([TimeSpan]::FromSeconds(10)))) { throw 'FAST_PROCESS_FALSE_TIMEOUT' }
    if ($fast.ExitCode -ne 0) { throw 'FAST_PROCESS_EXIT_INVALID' }
    $checks++

    $pulses = New-Object 'Collections.Generic.List[double]'
    $slow = Start-WmsWaitFixture 700 0
    $output = @(Wait-WmsDevGuardExit -Process $slow -Timeout ([TimeSpan]::FromSeconds(5)) -ProgressInterval ([TimeSpan]::FromMilliseconds(100)) -OnProgress {
        param($elapsed, $maximum)
        $pulses.Add($elapsed)
        'Callback output must not become a success result'
    })
    if ($output.Count -ne 1 -or $output[0] -isnot [bool] -or -not $output[0] -or $pulses.Count -lt 1) { throw 'SLOW_PROCESS_OR_PROGRESS_INVALID' }
    $checks++

    $failed = Start-WmsWaitFixture 1 20
    if (-not (Wait-WmsDevGuardExit -Process $failed -Timeout ([TimeSpan]::FromSeconds(5)) -ProgressInterval ([TimeSpan]::FromMilliseconds(100)))) { throw 'EXIT20_FALSE_TIMEOUT' }
    # A espera terminou; o launcher ainda deve recusar o codigo da guarda.
    if ($failed.ExitCode -ne 20) { throw 'EXIT20_WAS_LOST' }
    $checks++

    $pending = Start-WmsWaitFixture 1500 0
    $timeout = [Diagnostics.Stopwatch]::StartNew()
    if (Wait-WmsDevGuardExit -Process $pending -Timeout ([TimeSpan]::FromMilliseconds(150)) -ProgressInterval ([TimeSpan]::FromSeconds(10))) { throw 'PENDING_PROCESS_FALSE_SUCCESS' }
    $timeout.Stop()
    if ($timeout.Elapsed.TotalMilliseconds -gt 1000 -or $pending.HasExited) { throw 'DEADLINE_IGNORED_OR_PROCESS_TERMINATED' }
    $checks++

    foreach ($invalid in @(
        @{Timeout=[TimeSpan]::Zero; ProgressInterval=[TimeSpan]::FromSeconds(1)},
        @{Timeout=[TimeSpan]::FromSeconds(1); ProgressInterval=[TimeSpan]::Zero}
    )) {
        $rejected = $false
        try { $null = Wait-WmsDevGuardExit -Process $fast @invalid } catch {
            if ($_.Exception.Message -ne 'DEV_WAIT_INTERVAL_INVALID') { throw }
            $rejected = $true
        }
        if (-not $rejected) { throw 'INFINITE_WAIT_ACCEPTED' }
        $checks++
    }
    Write-Output ('PASS: ' + $checks + ' verificacoes de prazo/progresso com processos ficticios, sem banco.')
} finally {
    foreach ($process in $fixtures) {
        if (-not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000) }
        $process.Dispose()
    }
}
