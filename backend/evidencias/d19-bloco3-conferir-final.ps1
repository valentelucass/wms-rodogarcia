$ErrorActionPreference = 'Stop'
$taskBackend = Split-Path $PSScriptRoot -Parent
$taskProject = Split-Path $taskBackend -Parent
$taskEncoding = New-Object System.Text.UTF8Encoding($false)
function SalvarJson([string] $nome, $conteudo) {
    $json = $conteudo | ConvertTo-Json -Depth 12
    [IO.File]::WriteAllText((Join-Path $PSScriptRoot $nome), $json + "`n", $taskEncoding)
}
$taskLog = Join-Path $PSScriptRoot 'd19-bloco3-clean-verify-final.log'
$taskOutput = Get-Content -LiteralPath $taskLog -Raw -Encoding UTF8
if ($taskOutput -notmatch '\[INFO\] BUILD SUCCESS') { throw 'Build final sem SUCCESS.' }
$taskXmls = @(Get-ChildItem -LiteralPath (Join-Path $taskBackend 'target/surefire-reports') -Filter 'TEST-*.xml' | Sort-Object Name)
$taskSuites = @($taskXmls | ForEach-Object {
    [xml] $taskXml = Get-Content -LiteralPath $_.FullName -Raw -Encoding UTF8
    $taskSuite = $taskXml.testsuite
    [pscustomobject][ordered]@{ suite = $taskSuite.name; testes = [int]$taskSuite.tests; falhas = [int]$taskSuite.failures; erros = [int]$taskSuite.errors; ignorados = [int]$taskSuite.skipped; segundos = $taskSuite.time; xml = $_.Name; sha256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash }
})
$taskFalhas = ($taskSuites | Measure-Object falhas -Sum).Sum
$taskErros = ($taskSuites | Measure-Object erros -Sum).Sum
$taskIgnorados = ($taskSuites | Measure-Object ignorados -Sum).Sum
if ($taskFalhas -ne 0 -or $taskErros -ne 0 -or $taskIgnorados -ne 0) { throw 'XML final com falha, erro ou ignorado.' }
$taskJar = Get-Item -LiteralPath (Join-Path $taskBackend 'target/wms-backend-0.0.1-SNAPSHOT.jar')
$taskFontes = @(& rg --files (Join-Path $taskBackend 'src/main/java') | Where-Object { $_.EndsWith('.java') })
$taskTestes = @(& rg --files (Join-Path $taskBackend 'src/test/java') | Where-Object { $_.EndsWith('.java') })
SalvarJson 'd19-bloco3-resumo-final.json' ([ordered]@{
    comando = 'mvnw.cmd -B -ntp clean verify'; log = 'd19-bloco3-clean-verify-final.log'; jdk = 'Temurin 21.0.12.1+1'; maven = '3.9.16'; ferramentas = 'd19-bloco3-ferramentas-cmd-final.log';
    finalizado = [regex]::Match($taskOutput, 'Finished at: ([^\r\n]+)').Groups[1].Value;
    duracao = [regex]::Match($taskOutput, 'Total time: +([^\r\n]+)').Groups[1].Value;
    testes = ($taskSuites | Measure-Object testes -Sum).Sum; falhas = $taskFalhas; erros = $taskErros; ignorados = $taskIgnorados; quantidadeXml = $taskXmls.Count; fontesPrincipais = $taskFontes.Count; fontesTeste = $taskTestes.Count;
    suites = $taskSuites;
    jar = [ordered]@{ arquivo = 'backend/target/wms-backend-0.0.1-SNAPSHOT.jar'; bytes = $taskJar.Length; sha256 = (Get-FileHash -LiteralPath $taskJar.FullName -Algorithm SHA256).Hash };
    limite = 'HTTP/servicos/H2 isolados, dados ficticios. Sem SQL Server real, emissao/envio ESL/NFS-e, cobranca real, publicacao, frontend ou aceite externo.'
})
$taskPaths = @(& rg --files --hidden (Join-Path $taskProject 'backend') | ForEach-Object { $_.Substring($taskProject.Length + 1).Replace('\', '/') } | Where-Object { $_ -notmatch '^backend/(target|evidencias)/' })
$taskPaths += @('docs/27-separacao-retirada-retornos-e-avaria.md', 'docs/29-cadastros-servicos-e-calculo.md', 'docs/31-fechamento-contagem-e-contingencia.md')
$taskPaths = @($taskPaths | Sort-Object -Unique)
$taskHashes = [ordered]@{}
foreach ($taskPath in $taskPaths) { $taskHashes[$taskPath] = (Get-FileHash -LiteralPath (Join-Path $taskProject $taskPath) -Algorithm SHA256).Hash }
$taskManifesto = @('D19 bloco3 - freeze BE13 final - 2026-10-06 - SHA-256') + @($taskPaths | ForEach-Object { $taskHashes[$_] + '  ' + $_ })
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'd19-bloco3-freeze-final.sha256'), ($taskManifesto -join "`n") + "`n", $taskEncoding)
$taskAnterior = Join-Path $PSScriptRoot 'd19-bloco2-freeze-p2-avaria-final.sha256'
$taskPreservacao = @(Get-Content -LiteralPath $taskAnterior -Encoding UTF8 | ForEach-Object {
    if ($_ -match '^([A-F0-9]{64})  (.+)$') {
        $taskHashAnterior = $matches[1]; $taskCaminhoAnterior = $matches[2]
        [pscustomobject][ordered]@{ arquivo = $taskCaminhoAnterior; anterior = $taskHashAnterior; atual = $taskHashes[$taskCaminhoAnterior]; situacao = $(if (-not $taskHashes.Contains($taskCaminhoAnterior)) { 'AUSENTE' } elseif ($taskHashes[$taskCaminhoAnterior] -eq $taskHashAnterior) { 'PRESERVADO' } else { 'ALTERADO_BE13' }) }
    }
})
if (@($taskPreservacao | Where-Object { $_.situacao -eq 'AUSENTE' }).Count -ne 0) { throw 'Arquivo anterior ausente.' }
$taskAntigos = @($taskPreservacao | ForEach-Object { $_.arquivo })
SalvarJson 'd19-bloco3-preservacao-final.json' ([ordered]@{
    manifestoAnterior = 'd19-bloco2-freeze-p2-avaria-final.sha256'; sha256Anterior = (Get-FileHash -LiteralPath $taskAnterior -Algorithm SHA256).Hash;
    anteriores = $taskPreservacao.Count; preservados = @($taskPreservacao | Where-Object { $_.situacao -eq 'PRESERVADO' }).Count; alteradosBE13 = @($taskPreservacao | Where-Object { $_.situacao -eq 'ALTERADO_BE13' }).Count; ausentes = 0;
    novoManifesto = 'd19-bloco3-freeze-final.sha256'; totalArquivosNovo = $taskPaths.Count; sha256Novo = (Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'd19-bloco3-freeze-final.sha256') -Algorithm SHA256).Hash;
    novos = @($taskPaths | Where-Object { $_ -notin $taskAntigos }); comparacao = $taskPreservacao;
    limite = 'Comparacao de fontes/contratos do freeze292; outputs/manifestos anteriores nao sobrescritos. Arquivos centrais/database/frontend fora da escrita de Cedro.'
})
Write-Output ('XMLs=' + $taskXmls.Count + '; testes=' + ($taskSuites | Measure-Object testes -Sum).Sum + '; falhas=' + $taskFalhas + '; erros=' + $taskErros + '; ignorados=' + $taskIgnorados + '; manifesto=' + $taskPaths.Count)
