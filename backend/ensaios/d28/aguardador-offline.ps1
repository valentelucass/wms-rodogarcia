param([string]$ChildMutex,[string]$ChildSignal)
$ErrorActionPreference='Stop'
. "$PSScriptRoot/aguardador-controles.ps1"
if($ChildMutex){
 $m=[Threading.Mutex]::new($false,$ChildMutex);$own=$m.WaitOne(0)
 try{[IO.File]::WriteAllText($ChildSignal,([string]$own));Start-Sleep -Milliseconds 3000}finally{if($own){$m.ReleaseMutex()};$m.Dispose()};exit
}
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
$cases=New-Object 'Collections.Generic.List[object]'
function Check([string]$name,[bool]$pass){$cases.Add([ordered]@{caso=$name;passou=$pass})}
$f=[pscustomobject]@{rodada='D2800ABCDEF';login='WMSDEV';alvo='127.0.0.1:1433/WMS_DEV';pedidos=@(900001,900002);estados=@('RASCUNHO','RASCUNHO');fisico=100;reservado=0}
$historical=Join-Path $root 'backend/ensaios/d27/aguardar-concorrencia.ps1'
$hash=(Get-FileHash -LiteralPath $historical).Hash
$ticket=[pscustomobject]@{rodada=$f.rodada;estado='OBSERVADOR_AGUARDANDO_TICKET_NOVO';prontoParaTicket=$true;expiraUtc=[DateTime]::UtcNow.AddMinutes(3).ToString('o');pid=$PID;checks=24;falhas=0;SQLExecutado=$false;hashObservador=$hash;banco='WMS_DEV';login='WMSDEV'}
$name='Local\WMS_Cedro_D28_Offline_'+[guid]::NewGuid().ToString('N')
$script:launches=0;$script:reads=0
function Run($fixture=$f,$opt=$true,$round=$f.rodada){Invoke-D28AguardadorOffline -OptIn $opt -Rodada $round -Fixture $fixture -LerTicket {$script:reads++;$ticket} -Destino {$script:launches++} -MutexName $name -PrazoMs 100 -HashObservador $hash}
try{Run -opt $false;Check 'opt-in ausente recusado' $false}catch{Check 'opt-in ausente recusado antes de ler ticket' ($_.Exception.Message -ceq 'D28_OPTIN_EXPLICITO_AUSENTE' -and $reads -eq 0 -and $launches -eq 0)}
$r=Run
Check 'ticket atual valido chama destino offline uma vez' ($r.destinoChamado -and $launches -eq 1 -and $reads -eq 1)
foreach($variant in @('rodada','estado','prontoParaTicket','prazo','pid','checks','falhas','SQLExecutado','hashObservador','banco','login')){
 $old=$ticket|ConvertTo-Json -Depth 5|ConvertFrom-Json
 switch($variant){'rodada'{$ticket.rodada='D28FFFFFFFF'}'estado'{$ticket.estado='FINALIZADO'}'prontoParaTicket'{$ticket.prontoParaTicket=$false}'prazo'{$ticket.expiraUtc=[DateTime]::UtcNow.AddSeconds(20).ToString('o')}'pid'{$ticket.pid=2147483647}'checks'{$ticket.checks=23}'falhas'{$ticket.falhas=1}'SQLExecutado'{$ticket.SQLExecutado=$true}'hashObservador'{$ticket.hashObservador='outro'}'banco'{$ticket.banco='WMS_PROD'}'login'{$ticket.login='sa'}}
 $before=$launches;$sw=[Diagnostics.Stopwatch]::StartNew();$r=Run;$sw.Stop()
 Check ('ticket negativo '+$variant+' expira sem destino') ($r.estado -ceq 'D28_ESPERA_EXPIRADA_SEM_JVM_SQL' -and $launches -eq $before -and $sw.ElapsedMilliseconds -ge 90 -and $sw.ElapsedMilliseconds -lt 1500)
 $ticket=$old
}
foreach($variant in @('final','rodada','repetido','fisico','reservado')){
 $bad=$f|ConvertTo-Json|ConvertFrom-Json
 if($variant -eq 'final'){$bad.estados=@('RESERVADO','RASCUNHO')}
 if($variant -eq 'rodada'){$bad.rodada='D28FFFFFFFF'}
 if($variant -eq 'repetido'){$bad.pedidos=@(900001,900001)}
 if($variant -eq 'fisico'){$bad.fisico=99}
 if($variant -eq 'reservado'){$bad.reservado=80}
 $beforeR=$reads;$beforeL=$launches;$refused=$false
 try{Run -fixture $bad}catch{$refused=$_.Exception.Message -in @('D28_ESTADO_FINAL_RECUSADO_ANTES_GATE','D28_FIXTURE_FONTE_DIVERGENTE','D28_SALDO_PRE_GATE_RECUSADO')}
 Check ('fixture '+$variant+' recusada antes ticket/gate') ($refused -and $reads -eq $beforeR -and $launches -eq $beforeL)
}
$signal=Join-Path $root ('backend/target-d28-helper/aguardador-'+[guid]::NewGuid().ToString('N')+'.txt')
$psi=[Diagnostics.ProcessStartInfo]::new();$psi.FileName=(Get-Process -Id $PID).Path;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true
$psi.Arguments='-NoProfile -File "'+$PSCommandPath+'" -ChildMutex "'+$name+'" -ChildSignal "'+$signal+'"'
$child=[Diagnostics.Process]::Start($psi)
try{
 $deadline=[DateTime]::UtcNow.AddSeconds(5)
 while(-not(Test-Path -LiteralPath $signal) -and [DateTime]::UtcNow -lt $deadline){Start-Sleep -Milliseconds 25}
 $before=$launches;$r=Run
 Check 'mutex real entre processos recusa segundo aguardador' ((Test-Path -LiteralPath $signal) -and [IO.File]::ReadAllText($signal) -ceq 'True' -and $r.estado -ceq 'D28_AGUARDADOR_JA_ATIVO' -and $launches -eq $before)
}finally{if(-not $child.HasExited){$child.Kill();$child.WaitForExit()};$child.Dispose()}
$before=$launches;$r=Run
Check 'finally libera mutex e permite nova execucao offline' ($r.destinoChamado -and $launches -eq $before+1)
$result=[ordered]@{incremento='D28';historico='D28-VIG-R23';utc=[DateTime]::UtcNow.ToString('o');fonteHistorica='backend/ensaios/d27/aguardar-concorrencia.ps1';fonteSHA256=$hash;fonteExercitada='backend/ensaios/d28/aguardador-controles.ps1';diferencas='Adaptacao offline com entradas publicas, destino contado e guardas rodada/alvo/estado final antes gate. Condicoes ticket/mutex/prazo derivadas D27.';checks=$cases.Count;falhas=@($cases|Where-Object {-not $_.passou}).Count;destinoOfflineChamadas=$launches;SQLExecutado=$false;credencialCarregada=$false;JVMIniciada=$false;witnessNativeOnline=$false;processoFilhoEncerrado=$true;casos=$cases.ToArray()}
[IO.File]::WriteAllText((Join-Path $root 'backend/evidencias/d28-aguardador-offline.json'),($result|ConvertTo-Json -Depth 6),[Text.UTF8Encoding]::new($false))
[pscustomobject]$result|Select-Object checks,falhas,SQLExecutado,credencialCarregada,JVMIniciada|ConvertTo-Json
if($result.falhas){exit 1}
