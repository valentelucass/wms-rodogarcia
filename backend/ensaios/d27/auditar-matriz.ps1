[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$checks=[Collections.Generic.List[object]]::new()
function Check([string]$caso,[bool]$passou){$checks.Add([pscustomobject]@{caso=$caso;passou=$passou})}
function Read([string]$name){[IO.File]::ReadAllText((Join-Path $backend ('evidencias/'+$name)))|ConvertFrom-Json}
$m=Read 'd27-matriz.json';$rules=Read 'd27-regras.json';$sources=@{}
Check 'inventario161 IDs unicos' ($m.matriz.Count -eq 161 -and @($m.matriz.id|Sort-Object -Unique).Count -eq 161)
Check 'historicoD26 distinto do atual e sem aceite total' ($m.historiaD26Preservada -and -not $m.backendTotalAprovado -and $m.ACPropostasNaoHomologadas)
Check 'HTTP soma individuos pares divergencias' ($m.contagens.http -eq ($m.contagens.httpAprovadasIndividualmente+$m.contagens.httpRequisicoesParesConcorrentes+$m.contagens.httpFalhasHistoricas))
Check 'divergencias causais3backend4roteiro preservadas' ($m.contagens.httpDefeitosBackendHistoricosCorrigidos -eq 3 -and $m.contagens.httpDivergenciasRoteiroHistoricas -eq 4)
Check 'dois404 fora API inventariada classificados' ($m.httpNaoMapeado.Count -eq 2 -and @($m.httpNaoMapeado|Where-Object {$_.actual -ne 404 -or $_.classificacao -cne 'erroRoteiroforaAPIinventario'}).Count -eq 0)
$references=0;$differences=0;$duplicates=0
foreach($r in $rules.matriz){
 $keys=[Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
 foreach($c in $r.casosD27){
  $references++
  if(-not $keys.Add($c.arquivo+'|'+$c.indice)){$duplicates++}
  if(-not $sources.ContainsKey($c.arquivo)){$sources[$c.arquivo]=Read $c.arquivo}
  $source=$sources[$c.arquivo];$original=$source.cases[[int]$c.indice-1]
  if($source.rodada -cne $c.rodada -or $original.caso -cne $c.caso -or $original.estado -cne $c.estado -or [string]$original.metodo -cne [string]$c.metodo){$differences++;continue}
  if($c.metodo -and ($original.rota -cne $c.rota -or $original.perfil -cne $c.perfil -or $original.expected -ne $c.expected -or $original.actual -ne $c.actual)){$differences++}
 }
}
Check 'todas referencias regra arquivo indice correspondem a fonte' ($references -gt 364 -and $differences -eq 0)
Check 'sem deduplicacao incorreta nem duplicatas por regra' ($duplicates -eq 0)
foreach($id in @('RN08','RN15','RN17','RN22','RN23','AC04','AC06','AC08','AC13')){$r=@($rules.matriz|Where-Object {$_.regra -ceq $id})[0];Check ('regra '+$id+' tem multiplos casos causais') ($r.casosD27.Count -gt 3)}
$fifo=@($rules.matriz|Where-Object {$_.regra -ceq 'RN23'})[0];$avaria=@($rules.matriz|Where-Object {$_.regra -ceq 'AC08'})[0]
Check 'FIFO sem replayXML como prova' (@($fifo.casosD27|Where-Object {$_.caso -notmatch '^RN23 FIFO'}).Count -eq 0)
Check 'AC08 avaria sem previsao generica como prova' (@($avaria.casosD27|Where-Object {$_.caso -notmatch '^AC08'}).Count -eq 0)
$xml=Read 'd27-D2755FEBD14-http.json'
Check 'JAR atual cinco limites400 e positivo replay' ($xml.ids.jarSha256 -ceq '4304D4283BB21A0F80F3D6AD034F9891D560DFED754B6511E6BD89CDE8900CE1' -and @($xml.cases|Where-Object {$_.metodo -eq 'POST' -and $_.caso -match '^RN22 limite numerico' -and $_.actual -eq 400 -and $_.estado -eq 'aprovado'}).Count -eq 5 -and $xml.fase -ceq 'concluido')
foreach($name in @('d27-matriz.json','d27-matriz.md','d27-regras.json','d27-regras.md')){$path=Join-Path $backend ('evidencias/'+$name);$bytes=[IO.File]::ReadAllBytes($path);$text=[IO.File]::ReadAllText($path,[Text.UTF8Encoding]::new($false,$true));Check ($name+' UTF8 valido semBOM/mojibake') (-not($bytes[0] -eq 239 -and $bytes[1] -eq 187 -and $bytes[2] -eq 191) -and $text -notmatch '\u00C3[\u0080-\u00BF]|\uFFFD')}
$output=[ordered]@{utc=[DateTime]::UtcNow.ToString('o');tipo='AUDITORIA_DOCUMENTAL_OFFLINE_SEM_SQL_HTTP_BUILD';checks=$checks.Count;falhas=@($checks|Where-Object {-not $_.passou}).Count;referenciasRegraConfrontadas=$references;divergenciasReferencias=$differences;duplicatas=$duplicates;SQLExecutado=$false;HTTPExecutado=$false;resultados=$checks}
[IO.File]::WriteAllText((Join-Path $backend 'evidencias/d27-matriz-auditoria.json'),($output|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
$output|ConvertTo-Json -Compress -Depth 5
if($output.falhas){exit 1}
