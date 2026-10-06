$ErrorActionPreference = 'Stop'
$taskBackend = Split-Path $PSScriptRoot -Parent
$taskProject = Split-Path $taskBackend -Parent
$taskEncoding = New-Object System.Text.UTF8Encoding($false)
function SalvarJsonP2([string] $nome, $conteudo) {
    $destino = Join-Path $PSScriptRoot $nome
    if (Test-Path -LiteralPath $destino) { throw "Evidencia ja existe: $nome" }
    [IO.File]::WriteAllText($destino, ($conteudo | ConvertTo-Json -Depth 15) + [Environment]::NewLine, $taskEncoding)
}
$taskLogNome = 'd19-bloco4-clean-verify-p2-locks.log'
$taskOutput = Get-Content -LiteralPath (Join-Path $PSScriptRoot $taskLogNome) -Raw -Encoding UTF8
if ($taskOutput -notmatch '\[INFO\] BUILD SUCCESS') { throw 'Build P2 sem SUCCESS.' }
$taskXmls = @(Get-ChildItem -LiteralPath (Join-Path $taskBackend 'target-be14/surefire-reports') -Filter 'TEST-*.xml' | Sort-Object Name)
$taskSuites = @($taskXmls | ForEach-Object {
    [xml] $taskDocument = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
    $taskSuite = $taskDocument.testsuite
    [pscustomobject][ordered]@{ suite = $taskSuite.name; testes = [int]$taskSuite.tests; falhas = [int]$taskSuite.failures; erros = [int]$taskSuite.errors; ignorados = [int]$taskSuite.skipped; segundos = $taskSuite.time; xml = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
})
$taskTestes = ($taskSuites | Measure-Object testes -Sum).Sum
$taskFalhas = ($taskSuites | Measure-Object falhas -Sum).Sum
$taskErros = ($taskSuites | Measure-Object erros -Sum).Sum
$taskIgnorados = ($taskSuites | Measure-Object ignorados -Sum).Sum
if ($taskTestes -ne 367 -or $taskXmls.Count -ne 17 -or $taskFalhas -ne 0 -or $taskErros -ne 0 -or $taskIgnorados -ne 0) { throw 'XMLs P2 nao conferem 367/0/0/0 em17 suites.' }
$taskCasosAntigos = 0
foreach ($taskXmlAnterior in Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'd19-bloco4-xml-final') -Filter 'TEST-*.xml') {
    [xml] $taskAnteriorDoc = Get-Content -LiteralPath $taskXmlAnterior.FullName -Raw -Encoding UTF8
    [xml] $taskAtualDoc = Get-Content -LiteralPath (Join-Path $taskBackend ('target-be14/surefire-reports/' + $taskXmlAnterior.Name)) -Raw -Encoding UTF8
    $taskNomesAtuais = @($taskAtualDoc.testsuite.testcase | ForEach-Object { $_.classname + '/' + $_.name })
    foreach ($taskCaso in $taskAnteriorDoc.testsuite.testcase) {
        $taskCasosAntigos++
        if (($taskCaso.classname + '/' + $taskCaso.name) -notin $taskNomesAtuais) { throw 'Cenario360 ausente no P2.' }
    }
}
if ($taskCasosAntigos -ne 360) { throw 'Baseline XML360 divergente.' }
$taskJarAnterior = Join-Path $PSScriptRoot 'd19-bloco4-artefato-freeze360.jar'
if ((Get-FileHash -LiteralPath $taskJarAnterior -Algorithm SHA256).Hash -ne '829A59B6CA60AF314089D1A0D303603FF3CACEEDAB4076F47608D7B513B3BA13') { throw 'JAR360 historico divergente.' }
$taskManifestoAnterior = Join-Path $PSScriptRoot 'd19-bloco4-freeze-final.sha256'
if ((Get-FileHash -LiteralPath $taskManifestoAnterior -Algorithm SHA256).Hash -ne '42284559254B36C42806E9DC461E9A3A6C49A654984D9D1B332B745226ED3486') { throw 'Manifesto360/289 divergente.' }
$taskPermitidos = @(
    'backend/src/main/java/br/com/rodogarcia/wms/services/ContingenciaService.java',
    'backend/src/test/java/br/com/rodogarcia/wms/ContingenciaIntegrationTest.java',
    'docs/31-fechamento-contagem-e-contingencia.md'
)
$taskComparacao = @(Get-Content -LiteralPath $taskManifestoAnterior -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([A-F0-9]{64})  (.+)$') {
        $taskHashAnterior = $matches[1]; $taskPath = $matches[2]
        $taskHashAtual = (Get-FileHash -LiteralPath (Join-Path $taskProject $taskPath) -Algorithm SHA256).Hash
        if ($taskHashAtual -ne $taskHashAnterior -and $taskPath -notin $taskPermitidos) { throw "Alteracao fora P2: $taskPath" }
        [pscustomobject][ordered]@{ arquivo = $taskPath; anterior = $taskHashAnterior; atual = $taskHashAtual; situacao = $(if ($taskHashAtual -eq $taskHashAnterior) { 'PRESERVADO' } else { 'ALTERADO_P2_LOCKS' }) }
    }
})
if ($taskComparacao.Count -ne 289) { throw 'Baseline289 divergente.' }
$taskManifestoNome = 'd19-bloco4-freeze-p2-locks.sha256'
$taskManifestoDestino = Join-Path $PSScriptRoot $taskManifestoNome
if (Test-Path -LiteralPath $taskManifestoDestino) { throw 'Manifesto P2 ja existe.' }
$taskManifesto = @('D19 bloco4 - freeze P2-locks - 2026-10-06 - SHA-256') + @($taskComparacao | ForEach-Object { $_.atual + '  ' + $_.arquivo })
[IO.File]::WriteAllText($taskManifestoDestino, ($taskManifesto -join [Environment]::NewLine) + [Environment]::NewLine, $taskEncoding)
$taskXmlDestino = Join-Path $PSScriptRoot 'd19-bloco4-xml-p2-locks'
if (Test-Path -LiteralPath $taskXmlDestino) { throw 'Pasta de XMLs P2 ja existe.' }
New-Item -ItemType Directory -Path $taskXmlDestino | Out-Null
foreach ($taskXmlFile in $taskXmls) { Copy-Item -LiteralPath $taskXmlFile.FullName -Destination (Join-Path $taskXmlDestino $taskXmlFile.Name) }
$taskJar = Get-Item -LiteralPath (Join-Path $taskBackend 'target-be14/wms-backend-0.0.1-SNAPSHOT.jar')
$taskFontes = @(& rg --files (Join-Path $taskBackend 'src/main/java') | Where-Object { $_.EndsWith('.java') })
$taskTestesFontes = @(& rg --files (Join-Path $taskBackend 'src/test/java') | Where-Object { $_.EndsWith('.java') })
SalvarJsonP2 'd19-bloco4-resumo-p2-locks.json' ([ordered]@{
    comando = 'mvnw.cmd -B -ntp -Dwms.build.directory=target-be14 -DargLine=-Xmx768m clean verify'; log = $taskLogNome;
    JAVA_HOME = 'C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21'; MAVEN_OPTS = '-Xmx384m'; ferramentas = 'd19-bloco4-ferramentas-p2-locks.log';
    finalizado = [regex]::Match($taskOutput, 'Finished at: ([^\r\n]+)').Groups[1].Value;
    duracao = [regex]::Match($taskOutput, 'Total time: +([^\r\n]+)').Groups[1].Value;
    testes = $taskTestes; falhas = $taskFalhas; erros = $taskErros; ignorados = $taskIgnorados; quantidadeXml = $taskXmls.Count;
    fontesPrincipais = $taskFontes.Count; fontesTeste = $taskTestesFontes.Count; cenariosAnterioresConferidosPorNome = $taskCasosAntigos; testesNovos = $taskTestes - 360;
    suites = $taskSuites; xmlsPreservados = 'backend/evidencias/d19-bloco4-xml-p2-locks';
    jar = [ordered]@{ arquivo = 'backend/target-be14/wms-backend-0.0.1-SNAPSHOT.jar'; bytes = $taskJar.Length; sha256 = (Get-FileHash -LiteralPath $taskJar.FullName -Algorithm SHA256).Hash };
    jar360Preservado = 'backend/evidencias/d19-bloco4-artefato-freeze360.jar';
    limite = 'HTTP/transacoes H2/dados ficticios. Sem SQL Server/provedor/equipamento/fiscal/comercial reais, publicacao ou aceite BE14 antecipado.'
})
SalvarJsonP2 'd19-bloco4-preservacao-p2-locks.json' ([ordered]@{
    manifestoAnterior = 'd19-bloco4-freeze-final.sha256'; sha256Anterior = (Get-FileHash -LiteralPath $taskManifestoAnterior -Algorithm SHA256).Hash;
    anteriores = 289; preservados = @($taskComparacao | Where-Object { $_.situacao -eq 'PRESERVADO' }).Count; alteradosP2 = @($taskComparacao | Where-Object { $_.situacao -eq 'ALTERADO_P2_LOCKS' }).Count; ausentes = 0;
    novoManifesto = $taskManifestoNome; sha256Novo = (Get-FileHash -LiteralPath $taskManifestoDestino -Algorithm SHA256).Hash; comparacao = $taskComparacao;
    preservacaoHistorica = '360/289/XMLs/logs/JAR e333/251 mantidos; nenhum contrato27/29, model, config, DDL ou arquivo central alterado.'
})
Write-Output ('XMLs=' + $taskXmls.Count + '; testes=' + $taskTestes + '; falhas=' + $taskFalhas + '; erros=' + $taskErros + '; ignorados=' + $taskIgnorados + '; anterioresPorNome=' + $taskCasosAntigos + '; manifesto=' + $taskComparacao.Count)
Write-Output ('Alterados=' + @($taskComparacao | Where-Object { $_.situacao -eq 'ALTERADO_P2_LOCKS' }).Count)
Write-Output ('JAR=' + (Get-FileHash -LiteralPath $taskJar.FullName -Algorithm SHA256).Hash)
