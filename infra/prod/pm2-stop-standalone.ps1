# Transicao autorizada BAT -> PM2; nunca encerra por numero de porta isoladamente.
[CmdletBinding()]
param([switch]$InspectOnly)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$handles=@()
try {
    $listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop|Where-Object {$_.LocalPort -in @(25590,25591)})
    if($listeners.Count -eq 0){Write-Output 'PROD_PM2_PORTS_FREE';exit 0}
    if($listeners.Count -ne 2 -or @($listeners|Where-Object LocalAddress -ne '127.0.0.1').Count){throw 'PROD_PM2_FOREIGN_LISTENER'}
    $feListener=@($listeners|Where-Object LocalPort -eq 25591)
    $beListener=@($listeners|Where-Object LocalPort -eq 25590)
    if($feListener.Count -ne 1 -or $beListener.Count -ne 1){throw 'PROD_PM2_FOREIGN_LISTENER'}
    $fe=Get-CimInstance Win32_Process -Filter ('ProcessId='+$feListener[0].OwningProcess)
    $be=Get-CimInstance Win32_Process -Filter ('ProcessId='+$beListener[0].OwningProcess)
    if($fe.CommandLine -notmatch '--manifest "([^"]+)"'){throw 'PROD_PM2_UNATTESTED_FRONTEND'}
    $manifest=[IO.Path]::GetFullPath($Matches[1])
    $runs=Join-Path $root 'orchestracao/.runtime/launcher-producao/runs'
    $relative=$manifest.Substring($runs.Length)
    if(-not $manifest.StartsWith($runs+'\',[StringComparison]::OrdinalIgnoreCase) -or $relative -notmatch '^\\([a-f0-9]{32})\\candidate\\frontend\\dist\\build-manifest.json$'){throw 'PROD_PM2_FOREIGN_RUN'}
    $runId=$Matches[1];$run=Join-Path $runs $runId
    $l=[IO.File]::ReadAllText((Join-Path $run 'launcher.json'))|ConvertFrom-Json
    if($l.projectPath -ine $root -or $l.runId -cne $runId -or -not $l.operationalReadiness -or
       $l.frontend.pid -ne $fe.ProcessId -or $l.backend.pid -ne $be.ProcessId -or
       $l.frontend.manifest -ine $manifest -or
       (Get-FileHash -LiteralPath $l.backend.receiptPath).Hash -ine $l.backend.receiptSha256){throw 'PROD_PM2_RECEIPT_MISMATCH'}
    $b=[IO.File]::ReadAllText($l.backend.receiptPath)|ConvertFrom-Json
    if($b.profile -cne 'sqlserver-prod' -or $b.database -cne 'WMS_PROD' -or $b.guard.projectPath -ine $root -or
       -not $be.CommandLine.Contains($b.artifact.path) -or -not $be.CommandLine.Contains('--spring.profiles.active=sqlserver-prod') -or
       -not $fe.CommandLine.Contains((Join-Path $root 'frontend/tools/prod-server.mjs')) -or
       (Get-FileHash -LiteralPath $b.artifact.path).Hash -ine $b.artifact.sha256){throw 'PROD_PM2_PROCESS_MISMATCH'}
    if($be.ParentProcessId -ne $fe.ParentProcessId){throw 'PROD_PM2_PARENT_MISMATCH'}
    $parent=Get-CimInstance Win32_Process -Filter ('ProcessId='+$be.ParentProcessId)
    if(-not $parent.CommandLine.Contains((Join-Path $root 'infra/prod/iniciar-prod.ps1'))){throw 'PROD_PM2_PARENT_MISMATCH'}
    $verified=@()
    foreach($entry in @(@{id=$be.ProcessId;start=$b.backend.startUtc;port=25590},@{id=$fe.ProcessId;start=$l.frontend.startUtc;port=25591})){
        $p=Get-Process -Id $entry.id;$null=$p.Handle
        if([Math]::Abs(($p.StartTime.ToUniversalTime()-[DateTime]::Parse($entry.start).ToUniversalTime()).TotalMilliseconds) -gt 2){throw 'PROD_PM2_START_MISMATCH'}
        $own=@(Get-NetTCPConnection -State Listen|Where-Object OwningProcess -eq $p.Id)
        if($own.Count -ne 1 -or $own[0].LocalPort -ne $entry.port){throw 'PROD_PM2_EXTRA_LISTENER'}
        $handles+=$p;$verified+=@{pid=$p.Id;startUtc=$entry.start;port=$entry.port}
    }
    if($InspectOnly){Write-Output 'PROD_PM2_STANDALONE_ATTESTED';exit 0}
    $evidence=Join-Path $root ('orchestracao/.runtime/launcher-producao/pm2/transition-'+[guid]::NewGuid().ToString('N')+'.json')
    [IO.File]::WriteAllText($evidence,(@{previousRun=$runId;verified=$verified;authorizedScope='WMS PROD para PM2';observadoUtc=[DateTime]::UtcNow.ToString('o')}|ConvertTo-Json -Depth 8))
    foreach($p in $handles){if(-not $p.HasExited){$p.Kill();if(-not $p.WaitForExit(15000)){throw 'PROD_PM2_STOP_TIMEOUT'}}}
    Write-Output 'PROD_PM2_STANDALONE_STOPPED'
} catch {
    $code=$_.Exception.Message
    if($code -cnotmatch '^PROD_[A-Z0-9_]+$'){$code='PROD_PM2_STANDALONE_UNCONFIRMED'}
    Write-Output $code
    exit 20
} finally {foreach($p in $handles){$p.Dispose()}}
