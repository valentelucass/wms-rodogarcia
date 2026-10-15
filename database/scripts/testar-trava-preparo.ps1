# Processos Windows ficticios; zero SQL, credencial real ou processo operacional.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'trava-preparo.ps1')
$repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $repo ('orchestracao/.runtime/trava-preparo/'+[guid]::NewGuid().ToString('N'))
$null=New-Item -ItemType Directory -Path $area
$helper=Join-Path $area 'cliente-ficticio.ps1'
$source=Join-Path $PSScriptRoot 'trava-preparo.ps1'
$body=@'
param([string]$Source,[string]$Name,[int]$Wait=0,[int]$Hold=0,[string]$Ready,[string]$Output,[switch]$Abandon)
$ErrorActionPreference='Stop'
. $Source
$lease=$null
try {
    $lease=Enter-WmsDatabasePreparationLock -Name $Name -WaitSeconds $Wait
    $report=Get-WmsDatabasePreparationLockReport $lease
    $report|Add-Member pid $PID
    [IO.File]::WriteAllText($Output,($report|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
    if($Ready -and $lease.acquired){[IO.File]::WriteAllText($Ready,'PRONTO')}
    if($Hold){Start-Sleep -Milliseconds $Hold}
    if($Abandon){exit 0}
}finally {if(-not $Abandon){Exit-WmsDatabasePreparationLock $lease}}
'@
[IO.File]::WriteAllText($helper,$body,[Text.UTF8Encoding]::new($false))
$cases=[Collections.Generic.List[object]]::new()
function Need([bool]$Ok){if(-not $Ok){throw ('TRAVA_TESTE_FALHOU_LINHA_'+(Get-PSCallStack)[1].ScriptLineNumber)}}
function Check([string]$Name,[scriptblock]$Test){try{& $Test;$cases.Add(@{caso=$Name;passou=$true})}catch{$cases.Add(@{caso=$Name;passou=$false;erro=$_.Exception.Message})}}
function Start-Client([string]$Name,[int]$Wait=0,[int]$Hold=0,[switch]$Abandon){
    $id=[guid]::NewGuid().ToString('N');$output=Join-Path $area ($id+'.json');$ready=Join-Path $area ($id+'.ready')
    $info=[Diagnostics.ProcessStartInfo]::new()
    $info.FileName=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $info.Arguments='-NoProfile -File "'+$helper+'" -Source "'+$source+'" -Name "'+$Name+'" -Wait '+$Wait+' -Hold '+$Hold+' -Ready "'+$ready+'" -Output "'+$output+'"'
    if($Abandon){$info.Arguments+=' -Abandon'}
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    $p=[Diagnostics.Process]::new();$p.StartInfo=$info
    if(-not $p.Start()){throw 'TRAVA_FIXTURE_PROCESSO_FALHOU'}
    @{process=$p;stdout=$p.StandardOutput.ReadToEndAsync();stderr=$p.StandardError.ReadToEndAsync();output=$output;ready=$ready}
}
function Finish-Client($Client){
    try{
        if(-not $Client.process.WaitForExit(10000)){throw 'TRAVA_FIXTURE_TIMEOUT'}
        Need ($Client.process.ExitCode -eq 0)
        Get-Content -LiteralPath $Client.output -Raw -Encoding UTF8|ConvertFrom-Json
    }finally{
        if(-not $Client.process.HasExited){$Client.process.Kill();$Client.process.WaitForExit()}
        $Client.process.Dispose()
    }
}
function Wait-Ready($Client){
    $until=[DateTime]::UtcNow.AddSeconds(10)
    while(-not (Test-Path -LiteralPath $Client.ready)){
        if($Client.process.HasExited -or [DateTime]::UtcNow -ge $until){throw 'TRAVA_FIXTURE_NAO_PRONTA'}
        Start-Sleep -Milliseconds 20
    }
}
Check 'trava livre adquirida e liberada para outro processo' {
    $name='Local\WMS_TRAVA_FIXTURE_'+[guid]::NewGuid().ToString('N')
    $lease=Enter-WmsDatabasePreparationLock -Name $name -WaitSeconds 0
    Need $lease.acquired;Exit-WmsDatabasePreparationLock $lease
    $r=Finish-Client (Start-Client $name);Need $r.adquirida
}
Check 'concorrente aguarda liberacao e entra sem repetir trabalho' {
    $name='Local\WMS_TRAVA_FIXTURE_'+[guid]::NewGuid().ToString('N')
    $holder=Start-Client $name -Hold 2400;Wait-Ready $holder
    $waiter=Start-Client $name -Wait 4
    $r=Finish-Client $waiter;$h=Finish-Client $holder
    Need ($r.adquirida -and $r.esperaMs -gt 100 -and $r.esperaMs -lt 4500 -and $r.pidObservado -eq $h.pid)
}
Check 'timeout limitado identifica dono e nao libera sua trava' {
    $name='Local\WMS_TRAVA_FIXTURE_'+[guid]::NewGuid().ToString('N')
    $lease=Enter-WmsDatabasePreparationLock -Name $name -WaitSeconds 0
    try{
        $r=Finish-Client (Start-Client $name -Wait 1)
        Need (-not $r.adquirida -and $r.esperaMs -ge 950 -and $r.esperaMs -lt 2000 -and $r.pidObservado -eq $PID)
        $stillBusy=Finish-Client (Start-Client $name)
        Need (-not $stillBusy.adquirida -and $stillBusy.pidObservado -eq $PID)
    }finally{Exit-WmsDatabasePreparationLock $lease}
}
Check 'bootstrap aninhado nao libera trava externa antes das permissoes' {
    $name='Local\WMS_TRAVA_FIXTURE_'+[guid]::NewGuid().ToString('N')
    $outer=Enter-WmsDatabasePreparationLock -Name $name -WaitSeconds 0
    try{
        $inner=Enter-WmsDatabasePreparationLock -Name $name -WaitSeconds 0
        Need $inner.acquired;Exit-WmsDatabasePreparationLock $inner
        $r=Finish-Client (Start-Client $name);Need (-not $r.adquirida)
    }finally{Exit-WmsDatabasePreparationLock $outer}
    $r=Finish-Client (Start-Client $name);Need $r.adquirida
}
Check 'excecao libera somente trava propria em finally' {
    $name='Local\WMS_TRAVA_FIXTURE_'+[guid]::NewGuid().ToString('N')
    try{$lease=Enter-WmsDatabasePreparationLock -Name $name -WaitSeconds 0;throw 'FICTICIO'}catch{}finally{Exit-WmsDatabasePreparationLock $lease}
    $r=Finish-Client (Start-Client $name);Need $r.adquirida
}
Check 'processo encerrado sem release permite recuperacao sem matar terceiros' {
    $name='Local\WMS_TRAVA_FIXTURE_'+[guid]::NewGuid().ToString('N')
    $holder=Start-Client $name -Hold 1200 -Abandon;Wait-Ready $holder
    $keep=[Threading.Mutex]::OpenExisting($name)
    try{
        $lease=Enter-WmsDatabasePreparationLock -Name $name -WaitSeconds 3
        try{Need ($lease.acquired -and $lease.abandoned)}finally{Exit-WmsDatabasePreparationLock $lease}
        $null=Finish-Client $holder
    }finally{$keep.Dispose()}
}
Check 'Plan literal nao adquire trava de banco nem importa senha real' {
    & {
        . (Join-Path $PSScriptRoot 'preparar-ambiente.ps1')
        function Enter-WmsDatabasePreparationLock {throw 'PLAN_TRAVOU_BANCO_INDEVIDAMENTE'}
        function Read-WmsRuntimeSenha {throw 'PLAN_LEU_SENHA_INDEVIDAMENTE'}
        $r=Invoke-WmsEnvironmentPreparation -Ambiente DEV -Modo Plan
        Need ($r.estado -ceq 'PLANO_SEM_SQL' -and $null -eq $r.trava -and $null -eq $r.bootstrap)
    }
}
$failed=@($cases|Where-Object {-not $_.passou})
$report=[pscustomobject]@{total=$cases.Count;aprovados=$cases.Count-$failed.Count;falhas=$failed.Count;casos=$cases;sql=$false;processosOperacionaisAlterados=$false}
$json=$report|ConvertTo-Json -Depth 6
[IO.File]::WriteAllText((Join-Path $area 'resultado.json'),$json,[Text.UTF8Encoding]::new($false))
$json
if($failed.Count){exit 1}
