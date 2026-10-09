$ErrorActionPreference='Stop'
$root=(Resolve-Path "$PSScriptRoot/../../..").Path
$ev=Join-Path $root 'backend/evidencias'
$rounds=@(Get-ChildItem -LiteralPath $ev -Filter 'd29-D29*-http.json' -File|ForEach-Object {Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8|ConvertFrom-Json})
$processes=@(Get-CimInstance Win32_Process|Select-Object ProcessId,Name,CreationDate,ParentProcessId)
$listeners=@(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue|Select-Object LocalAddress,LocalPort,OwningProcess)
$rows=@(foreach($r in $rounds){foreach($kind in @('jar','helper')){
 $id=if($kind -eq 'jar'){$r.ids.jarPid}else{$r.ids.helperPid}
 $birth=if($kind -eq 'jar'){$r.ids.jarCriadoUtc}else{$r.ids.helperCriadoUtc}
 if(-not $id){continue}
 $live=@($processes|Where-Object ProcessId -eq $id)
 $match=$false
 if($live.Count -eq 1 -and $birth){$match=$live[0].Name -ceq 'java.exe' -and [Math]::Abs(($live[0].CreationDate.ToUniversalTime()-[DateTime]::Parse($birth).ToUniversalTime()).TotalSeconds) -lt 1}
 $ports=@(if($kind -eq 'jar'){$r.ids.porta}else{$r.ids.issuerPorta})
 $portRows=@($listeners|Where-Object {$ports -contains $_.LocalPort})
 [ordered]@{rodada=$r.rodada;tipo=$kind;PIDOriginal=$id;criacaoOriginalUtc=$birth;processoAtual=$live;identidadeOriginalViva=$match;limiteIdentidade=if(-not $birth){'Criacao nao registrada nesta versao historica; ausente/reuso nominal observado, sem kill por PID.'}else{$null};portasOriginais=$ports;listenersAtuais=$portRows;processoEncerradoDeclarado=$r.ids.jarEncerrado;sinalSelect=$r.ids.prumoSelectSinal.rodada}
}})
$doc=[ordered]@{demanda='D29';observadoUtc=[DateTime]::UtcNow.ToString('o');somenteMetadados=$true;commandLineLida=$false;processosEncerradosPorEsteScript=0;registros=$rows;originaisVivos=@($rows|Where-Object identidadeOriginalViva).Count;portasComListener=@($rows|Where-Object {$_.listenersAtuais.Count -gt 0}).Count;limite='Fotografia corrente, nao prova inexistencia passada/TIME_WAIT; nao encerra terceiros nem processos com PID reutilizado.'}
$stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfff')
[IO.File]::WriteAllText((Join-Path $ev "d29-processos-os-$stamp.json"),($doc|ConvertTo-Json -Depth 10),[Text.UTF8Encoding]::new($false))
Write-Output "D29 OS originaisVivos=$($doc.originaisVivos) portasComListener=$($doc.portasComListener)"
