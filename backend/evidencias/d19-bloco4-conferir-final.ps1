$ErrorActionPreference = 'Stop'
$taskBackend = Split-Path $PSScriptRoot -Parent
$taskProject = Split-Path $taskBackend -Parent
$taskEncoding = New-Object System.Text.UTF8Encoding($false)
function SalvarJson([string] $nome, $conteudo) {
    $destino = Join-Path $PSScriptRoot $nome
    if (Test-Path -LiteralPath $destino) { throw "Evidencia ja existe: $nome" }
    [IO.File]::WriteAllText($destino, ($conteudo | ConvertTo-Json -Depth 15) + "`n", $taskEncoding)
}
$taskLog = Join-Path $PSScriptRoot 'd19-bloco4-clean-verify-final-camadas.log'
$taskOutput = Get-Content -LiteralPath $taskLog -Raw -Encoding UTF8
if ($taskOutput -notmatch '\[INFO\] BUILD SUCCESS') { throw 'Build final sem SUCCESS.' }
$taskXmls = @(Get-ChildItem -LiteralPath (Join-Path $taskBackend 'target-be14/surefire-reports') -Filter 'TEST-*.xml' | Sort-Object Name)
$taskSuites = @($taskXmls | ForEach-Object {
    [xml] $taskDocument = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
    $taskSuite = $taskDocument.testsuite
    [pscustomobject][ordered]@{ suite = $taskSuite.name; testes = [int]$taskSuite.tests; falhas = [int]$taskSuite.failures; erros = [int]$taskSuite.errors; ignorados = [int]$taskSuite.skipped; segundos = $taskSuite.time; xml = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
})
$taskFalhas = ($taskSuites | Measure-Object falhas -Sum).Sum
$taskErros = ($taskSuites | Measure-Object erros -Sum).Sum
$taskIgnorados = ($taskSuites | Measure-Object ignorados -Sum).Sum
if ($taskFalhas -ne 0 -or $taskErros -ne 0 -or $taskIgnorados -ne 0) { throw 'XML final com falha, erro ou ignorado.' }
$taskResumoAnterior = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'd19-bloco3-resumo-final-p2-origem.json') -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($suite in $taskResumoAnterior.suites) {
    $atual = @($taskSuites | Where-Object { $_.suite -eq $suite.suite })
    if ($atual.Count -ne 1 -or $atual[0].testes -lt $suite.testes) { throw "Suite anterior ausente/reduzida: $($suite.suite)" }
}
$taskJar = Get-Item -LiteralPath (Join-Path $taskBackend 'target-be14/wms-backend-0.0.1-SNAPSHOT.jar')
$taskFontes = @(& rg --files (Join-Path $taskBackend 'src/main/java') | Where-Object { $_.EndsWith('.java') })
$taskTestes = @(& rg --files (Join-Path $taskBackend 'src/test/java') | Where-Object { $_.EndsWith('.java') })
$taskXmlDestino = Join-Path $PSScriptRoot 'd19-bloco4-xml-final'
if (Test-Path -LiteralPath $taskXmlDestino) { throw 'Pasta de XMLs ja existe.' }
New-Item -ItemType Directory -Path $taskXmlDestino | Out-Null
foreach ($taskXmlFile in $taskXmls) { Copy-Item -LiteralPath $taskXmlFile.FullName -Destination (Join-Path $taskXmlDestino $taskXmlFile.Name) }
SalvarJson 'd19-bloco4-resumo-final.json' ([ordered]@{
    comando = 'mvnw.cmd -B -ntp -Dwms.build.directory=target-be14 -DargLine=-Xmx768m clean verify'; log = 'd19-bloco4-clean-verify-final-camadas.log'; jdk = 'Temurin 21.0.12.1+1'; maven = '3.9.16'; ferramentas = 'd19-bloco4-ferramentas-final.log'; JAVA_HOME = 'C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21'; MAVEN_OPTS = '-Xmx384m';
    finalizado = [regex]::Match($taskOutput, 'Finished at: ([^\r\n]+)').Groups[1].Value;
    duracao = [regex]::Match($taskOutput, 'Total time: +([^\r\n]+)').Groups[1].Value;
    testes = ($taskSuites | Measure-Object testes -Sum).Sum; falhas = $taskFalhas; erros = $taskErros; ignorados = $taskIgnorados; quantidadeXml = $taskXmls.Count; fontesPrincipais = $taskFontes.Count; fontesTeste = $taskTestes.Count;
    testesAnteriores = 333; testesNovos = ($taskSuites | Measure-Object testes -Sum).Sum - 333; xmlsPreservados = 'backend/evidencias/d19-bloco4-xml-final'; suites = $taskSuites;
    jar = [ordered]@{ arquivo = 'backend/target-be14/wms-backend-0.0.1-SNAPSHOT.jar'; bytes = $taskJar.Length; sha256 = (Get-FileHash -LiteralPath $taskJar.FullName -Algorithm SHA256).Hash };
    limite = 'HTTP/servicos/H2 isolados, dados ficticios. Sem SQL Server real, emissao/envio ESL/NFS-e, cobranca real, publicacao, frontend ou aceite externo.'
})
$taskPaths = @(& rg --files --hidden (Join-Path $taskProject 'backend') | ForEach-Object { $_.Substring($taskProject.Length + 1).Replace('\', '/') } | Where-Object { $_ -notmatch '^backend/(target(?:-[^/]+)?|evidencias)/' })
$taskPaths += @('docs/27-separacao-retirada-retornos-e-avaria.md', 'docs/29-cadastros-servicos-e-calculo.md', 'docs/31-fechamento-contagem-e-contingencia.md')
$taskPaths = @($taskPaths | Sort-Object -Unique)
$taskHashes = [ordered]@{}
foreach ($taskPath in $taskPaths) { $taskHashes[$taskPath] = (Get-FileHash -LiteralPath (Join-Path $taskProject $taskPath) -Algorithm SHA256).Hash }
if ($taskHashes['docs/29-cadastros-servicos-e-calculo.md'] -ne 'D4365689A0A0A3A2A75FC84B01609EE14EF3563164A1E24CD3A42A265DD242EB') { throw 'Contrato29 congelado mudou.' }
if ($taskHashes['docs/27-separacao-retirada-retornos-e-avaria.md'] -ne '06029503B073817962288EDFB6C794FCB6981772C423BEAD65B7B197851DD774') { throw 'Contrato27 historico mudou.' }
$taskManifestoNome = 'd19-bloco4-freeze-final.sha256'
$taskManifestoDestino = Join-Path $PSScriptRoot $taskManifestoNome
if (Test-Path -LiteralPath $taskManifestoDestino) { throw 'Manifesto final ja existe.' }
$taskManifesto = @('D19 bloco4 - freeze BE01/BE14/BE05 final - 2026-10-06 - SHA-256') + @($taskPaths | ForEach-Object { $taskHashes[$_] + '  ' + $_ })
[IO.File]::WriteAllText($taskManifestoDestino, ($taskManifesto -join "`n") + "`n", $taskEncoding)
$taskAnterior = Join-Path $PSScriptRoot 'd19-bloco3-freeze-final-p2-origem.sha256'
if ((Get-FileHash -LiteralPath $taskAnterior -Algorithm SHA256).Hash -ne '628A2B453D71C5DD47CDD8D4F1C4FC5C6AC7667041F4CC174519C51AB5E76DD4') { throw 'Manifesto333/251 mudou.' }
$taskPreservacao = @(Get-Content -LiteralPath $taskAnterior -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([A-F0-9]{64})  (.+)$') {
        $taskHashAnterior = $matches[1]; $taskCaminhoAnterior = $matches[2]
        [pscustomobject][ordered]@{ arquivo = $taskCaminhoAnterior; anterior = $taskHashAnterior; atual = $taskHashes[$taskCaminhoAnterior]; situacao = $(if (-not $taskHashes.Contains($taskCaminhoAnterior)) { 'AUSENTE' } elseif ($taskHashes[$taskCaminhoAnterior] -eq $taskHashAnterior) { 'PRESERVADO' } else { 'ALTERADO_BE14' }) }
    }
})
if (@($taskPreservacao | Where-Object { $_.situacao -eq 'AUSENTE' }).Count -ne 0) { throw 'Arquivo anterior ausente.' }
$taskAntigos = @($taskPreservacao | ForEach-Object { $_.arquivo })
SalvarJson 'd19-bloco4-preservacao-final.json' ([ordered]@{
    manifestoAnterior = 'd19-bloco3-freeze-final-p2-origem.sha256'; sha256Anterior = (Get-FileHash -LiteralPath $taskAnterior -Algorithm SHA256).Hash;
    anteriores = $taskPreservacao.Count; preservados = @($taskPreservacao | Where-Object { $_.situacao -eq 'PRESERVADO' }).Count; alteradosBE14 = @($taskPreservacao | Where-Object { $_.situacao -eq 'ALTERADO_BE14' }).Count; ausentes = 0;
    novoManifesto = $taskManifestoNome; totalArquivosNovo = $taskPaths.Count; sha256Novo = (Get-FileHash -LiteralPath $taskManifestoDestino -Algorithm SHA256).Hash;
    novos = @($taskPaths | Where-Object { $_ -notin $taskAntigos }); comparacao = $taskPreservacao;
    limite = 'Comparacao de fontes/contratos do freeze333/251; outputs/manifestos anteriores nao sobrescritos. Arquivos centrais/database/frontend fora da escrita de Cedro.'
})
Write-Output ('XMLs=' + $taskXmls.Count + '; testes=' + ($taskSuites | Measure-Object testes -Sum).Sum + '; falhas=' + $taskFalhas + '; erros=' + $taskErros + '; ignorados=' + $taskIgnorados + '; manifesto=' + $taskPaths.Count)
