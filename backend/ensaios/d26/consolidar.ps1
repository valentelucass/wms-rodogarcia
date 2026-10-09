$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$utf8=[Text.UTF8Encoding]::new($false)
function WriteJson($name,$value){[IO.File]::WriteAllText((Join-Path $backend "evidencias/$name"),($value|ConvertTo-Json -Depth 18),$utf8)}
function XmlSummary($dir){
 $classes=@(Get-ChildItem (Join-Path $backend "evidencias/d26-xml/$dir") -Filter 'TEST-*.xml'|Sort-Object Name|ForEach-Object {
  [xml]$x=Get-Content $_.FullName -Raw -Encoding UTF8
  [pscustomobject]@{arquivo=$_.Name;tests=[int]$x.testsuite.tests;failures=[int]$x.testsuite.failures;errors=[int]$x.testsuite.errors;skipped=[int]$x.testsuite.skipped;sha256=(Get-FileHash $_.FullName -Algorithm SHA256).Hash}
 })
 [pscustomobject]@{pasta="d26-xml/$dir";xmls=$classes.Count;tests=($classes|Measure-Object tests -Sum).Sum;failures=($classes|Measure-Object failures -Sum).Sum;errors=($classes|Measure-Object errors -Sum).Sum;skipped=($classes|Measure-Object skipped -Sum).Sum;classes=$classes}
}
$historico=Join-Path $backend 'evidencias/d26-testes-resumo-inicial.historico.json'
if(-not(Test-Path $historico)){Copy-Item -LiteralPath (Join-Path $backend 'evidencias/d26-testes-resumo.json') -Destination $historico}
$builds=@(foreach($b in @(@('target-d26','surefire'),@('target-d26-final','final'),@('target-d26-financeiro','financeiro-final'))){
 $jar=Join-Path $backend "$($b[0])/wms-backend-0.0.1-SNAPSHOT.jar"
 [pscustomobject]@{build=$b[0];jarSha256=(Get-FileHash $jar -Algorithm SHA256).Hash;jarBytes=(Get-Item $jar).Length;resultado=(XmlSummary $b[1]);natureza='clean verify independente; H2 quando utilizado nao comprova SQL Server'}
})
$resumo=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');java='21';maven='3.9.16';buildAtual=$builds[-1];buildsPreservados=$builds;sqlServerIT=[pscustomobject]@{rodada='D2622F35458';resultado=(XmlSummary 'sqlserver-it');fixtureLockClienteId=1;fixturePreservada=$true;repetidoDepoisPopulacao=$false};focais=@(foreach($d in @('repro-revisao-zero','green-revisao-minima','repro-financeiro','green-financeiro')){XmlSummary $d});tentativasSemTeste=@('d26-repro-revisao-zero.log','d26-repro-financeiro-tentativa-formatacao.log');contagensNaoSomadas='409,411,412 sao execucoes distintas com sobreposicao; focais tambem nao somar como suite unica.'}
WriteJson 'd26-testes-resumo.json' $resumo
$rounds=@();$http=@();$asserts=@();$offline=@()
foreach($file in Get-ChildItem (Join-Path $backend 'evidencias') -Filter 'd26-*-http.json'|Sort-Object Name){
 $d=Get-Content $file.FullName -Raw -Encoding UTF8|ConvertFrom-Json
 if($d.fase -like 'offline*' -or -not $d.ids.jarPid){$offline+=[pscustomobject]@{rodada=$d.rodada;fase=$d.fase;tipo=$(if($d.rodada -ceq 'D2622F35458'){'IT SQL real, separado'}else{'sem JAR, offline/guarda isolada'});casos=$d.cases.Count};continue}
 $rounds+=[pscustomobject]@{rodada=$d.rodada;arquivo=$file.Name;sha256=(Get-FileHash $file.FullName -Algorithm SHA256).Hash;fase=$d.fase;bloqueio=$d.bloqueio;jarSha256=$d.ids.jarSha256;jarPid=$d.ids.jarPid;jarEncerrado=$d.ids.jarEncerrado;porta=$d.ids.porta;issuerPorta=$d.ids.issuerPorta;http=@($d.cases|Where-Object metodo).Count;assertivas=@($d.cases|Where-Object {-not $_.metodo}).Count}
 $i=0;foreach($c in $d.cases){$i++;$ref=[pscustomobject]@{rodada=$d.rodada;numero=$i;caso=$c.caso;metodo=$c.metodo;rota=$c.rota;perfil=$c.perfil;expected=$c.expected;actual=$c.actual;estado=$c.estado;cookie=$c.cookiePresente;segredo=$c.segredoPresente;requestId=$c.requestIdPresente}
  if($c.metodo){$http+=$ref}else{$asserts+=$ref}
 }
}
$matrix=Get-Content (Join-Path $backend 'evidencias/d26-matriz.json') -Raw -Encoding UTF8|ConvertFrom-Json
$sqlFile=Get-ChildItem "$backend/../orchestracao/.runtime" -Filter 'd26-prumo-complementos-*.json'|Where-Object {$_.Name -match '^d26-prumo-complementos-\d{8}T\d+\.json$'}|Sort-Object LastWriteTime -Descending|Select-Object -First 1
$sqlData=Get-Content $sqlFile.FullName -Raw -Encoding UTF8|ConvertFrom-Json
$sqlProof=[pscustomobject]@{arquivo="../../orchestracao/.runtime/$($sqlFile.Name)";sha256=(Get-FileHash $sqlFile.FullName -Algorithm SHA256).Hash;estado=$sqlData.estado;checks=$sqlData.checks;divergencias=$sqlData.divergencias;somenteSELECT=$sqlData.somenteSELECT;prodConectado=$sqlData.prodConectado;rodadas=$sqlData.rodadas;contextos=$sqlData.contextosSeparados;certificaTodasAPIs=$false;witnessConcorrenciaSQL=$false;preservaTentativa222div4=$true}
$report=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');escopo='D26 SQL real HTTP WMSDEV/WMS_DEV; sem frontend/publicacao/DDL/PROD';backendTotalAprovado=$false;buildAtual=$resumo.buildAtual;sqlServerIT=$resumo.sqlServerIT;endpoints=160;positivosHTTP=$matrix.comPositivoHTTP;semPositivoHTTP=$matrix.semPositivoHTTP;regras='d26-regras.md/json; 2xx nao comprova todas regras';contagens=[pscustomobject]@{rodadasAPI=$rounds.Count;requestsHTTP=$http.Count;httpExpectedActualAprovados=@($http|Where-Object estado -ceq 'aprovado').Count;httpParesConcorrentesConfrontadosEmConjunto=@($http|Where-Object {$_.expected -is [string] -and $_.expected -like 'um 200 outro 409'}).Count;httpTentativasHistoricasComDivergencia=@($http|Where-Object {$_.estado -cne 'aprovado' -and -not ($_.expected -is [string] -and $_.expected -like 'um 200 outro 409')}).Count;anonimos=@($http|Where-Object perfil -ceq 'anonimo').Count;autenticados2xx=@($http|Where-Object {$_.perfil -cne 'anonimo' -and $_.actual -ge 200 -and $_.actual -lt 300}).Count;autenticadosRecusas=@($http|Where-Object {$_.perfil -cne 'anonimo' -and $_.actual -ge 400}).Count;assertivas=$asserts.Count;assertivasAprovadas=@($asserts|Where-Object estado -ceq 'aprovado').Count;evidenciasSemJARSeparadas=$offline};protecoes=[pscustomobject]@{cookiesDetectados=@($http|Where-Object cookie -eq $true).Count;segredosDetectados=@($http|Where-Object segredo -eq $true).Count;requestIdAusenciaObservada=@($http|Where-Object requestId -eq $false).Count;requestIdNaoCapturado=@($http|Where-Object {$null -eq $_.requestId}).Count;flagsCookieSegredoNaoCapturadas=@($http|Where-Object {$null -eq $_.cookie -or $null -eq $_.segredo}).Count;JarsNaoEncerrados=@($rounds|Where-Object jarEncerrado -ne $true).Count};impedimentos=@('E052: CHECK fato_permanencia nao aceita AJUSTE_ESTOQUE; 409 rollback comprovado, proposta V10 fora migrations nao aplicada.','Witness SQL simultaneo da concorrencia nao comprovado; par200/409 e duas threads JVM no reservar/JDBC comprovados separadamente.','Integracoes externas, valores comerciais reais, frontend/equipamentos/recuperacao e todas variantes RN/AC nao homologados.');rodadas=$rounds;tentativasHTTPHistoricasComDivergencia=@($http|Where-Object {$_.estado -cne 'aprovado' -and -not ($_.expected -is [string] -and $_.expected -like 'um 200 outro 409')});paresConcorrentes=@($http|Where-Object {$_.expected -is [string] -and $_.expected -like 'um 200 outro 409'});assertivas=@($asserts|Select-Object rodada,numero,caso,estado);sqlFinal=$sqlProof}
$extraFile=Join-Path $backend 'evidencias/d26-extra-naozero-prumo-pedido.json'
if(Test-Path $extraFile){
 $e=Get-Content $extraFile -Raw -Encoding UTF8|ConvertFrom-Json
 $report|Add-Member -NotePropertyName extraFinanceiro -NotePropertyValue ([pscustomobject]@{rodada=$e.rodada;fonte=$e.fonte;sha256=$e.sha256;clockUTC=$e.clockUTC;positivo20Aprovado=$e.positivo20Aprovado;guarda=$e.guarda;calculoAtual=$e.calculoAtual;ciclosIntermediarios=@($e.ciclos).Count;fechamentoId=$e.fechamento.id;fechamentoSituacao=$e.fechamento.situacao;pedidoSQL='d26-extra-naozero-prumo-pedido.json';preservacao='d26-preextra-naozero';SQLHistorico236NaoReexecutado=$true})
 $report.impedimentos+='Extra APROVADO20 bloqueado: fato3 ANULADO, calculo atual zero, snapshot5=20 desatualizado; periodo real encerrado e recusa409 CALCULO_DESATUALIZADO comprovada.'
 $focal=Get-ChildItem "$backend/../orchestracao/.runtime" -Filter 'd26-prumo-fechamento20-*.json'|Where-Object {$_.Name -match '^d26-prumo-fechamento20-\d{8}T\d+\.json$'}|Sort-Object LastWriteTime -Descending|Select-Object -First 1
 if($focal){
  $f=Get-Content $focal.FullName -Raw -Encoding UTF8|ConvertFrom-Json
  $report.extraFinanceiro|Add-Member -NotePropertyName sqlFocal -NotePropertyValue ([pscustomobject]@{arquivo="../../orchestracao/.runtime/$($focal.Name)";sha256=(Get-FileHash $focal.FullName -Algorithm SHA256).Hash;estado=$f.estado;checks=$f.checks;divergencias=$f.divergencias;somenteSELECT=$f.somenteSELECT;aprovacao20Concluida=$f.aprovacao20Concluida;fotografia236Refazida=$f.fotografia236Refazida;prodConectado=$f.prodConectado})
 }
}
WriteJson 'd26-cedro.json' $report
[pscustomobject]@{buildTests=$resumo.buildAtual.resultado.tests;http=$http.Count;assertivas=$asserts.Count;positivos=$matrix.comPositivoHTTP;rodadas=$rounds.Count;naoEncerrados=$report.protecoes.JarsNaoEncerrados}|ConvertTo-Json -Compress
