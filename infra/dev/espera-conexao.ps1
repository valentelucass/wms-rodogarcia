# Prazos do cliente WMS DEV. Nao conecta, nao inicia processos e nao le segredos.
function Get-WmsDevConnectionWaitPolicy {
    [pscustomobject]@{
        ConnectTimeoutSeconds = 120
        GuardTimeoutSeconds = 240
        ProgressIntervalSeconds = 10
    }
}

function Wait-WmsDevGuardExit {
    param(
        [Parameter(Mandatory = $true)][Diagnostics.Process]$Process,
        [Parameter(Mandatory = $true)][TimeSpan]$Timeout,
        [Parameter(Mandatory = $true)][TimeSpan]$ProgressInterval,
        [scriptblock]$OnProgress
    )
    if ($Timeout.TotalMilliseconds -le 0 -or $Timeout.TotalMilliseconds -gt [int]::MaxValue -or
        $ProgressInterval.TotalMilliseconds -le 0) { throw 'DEV_WAIT_INTERVAL_INVALID' }

    $watch = [Diagnostics.Stopwatch]::StartNew()
    try {
        while ($true) {
            $remaining = $Timeout.TotalMilliseconds - $watch.Elapsed.TotalMilliseconds
            if ($remaining -le 0) { return $Process.HasExited }
            $slice = [int][Math]::Ceiling([Math]::Min($remaining, $ProgressInterval.TotalMilliseconds))
            if ($Process.WaitForExit($slice)) { return $true }
            if ($watch.Elapsed -ge $Timeout) { return $false }
            if ($OnProgress) { $null = & $OnProgress $watch.Elapsed.TotalSeconds $Timeout.TotalSeconds }
        }
    } finally {
        $watch.Stop()
    }
}
