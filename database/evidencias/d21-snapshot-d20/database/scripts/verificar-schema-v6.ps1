
$WmsD20Pathverificarschemav6=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
# Somente leitura de models, contrato e SQL conhecidos; nenhuma conexao ou build.
$ErrorActionPreference = 'Stop'
$raizProjeto = [System.IO.Path]::GetFullPath((Join-Path $WmsD20Pathverificarschemav6 '..'))
$raizModels = Join-Path $raizProjeto 'backend/src/main/java/br/com/rodogarcia/wms/models'
$arquivoV6 = Join-Path $raizProjeto 'database/migrations/V6__separacao_retirada_retornos_e_avaria.sql'
$sqlV6 = Get-Content -LiteralPath $arquivoV6 -Raw -Encoding UTF8
$nomesModels = 'SeparacaoSaida','DocumentoSaida','CoberturaDocumentoSaida','RetiradaSaida','BaixaSaida','DevolucaoSaida','AvariaEstoque','FatoPermanencia'
$resumoModels = @()
$divergencias = @()
$totalColunas = 0
$totalFks = 0
$totalUnicas = 0
foreach ($nomeModel in $nomesModels) {
    $arquivoModel = Join-Path $raizModels "$nomeModel.java"
    $javaFonte = Get-Content -LiteralPath $arquivoModel -Raw -Encoding UTF8
    $nomeTabela = [regex]::Match($javaFonte,'@Table\s*\(\s*name\s*=\s*"([^"]+)"').Groups[1].Value
    $blocoTabela = [regex]::Match($sqlV6, ('(?s)CREATE TABLE wms\.' + $nomeTabela + '\s*\((.*?)\r?\n\);')).Groups[1].Value
    $colunasSql = @{}
    foreach ($colunaSql in [regex]::Matches($blocoTabela,'(?m)^\s*(?<nome>[a-z]\w*)\s+(?<tipo>bigint|bit|date(?!time)|datetime2\(\d+\)|decimal\(\d+,\d+\)|n?varchar\((?:\d+|max)\))(?<identity>\s+IDENTITY\(1,1\))?\s+(?<nulo>NOT NULL|NULL)\b')) {
        $colunasSql[$colunaSql.Groups['nome'].Value] = $colunaSql
    }
    $camposJava = [regex]::Matches($javaFonte,'(?<attrs>(?:@\w+(?:\((?:[^()"]|"[^"]*")*\))?\s*)+)\s*private\s+(?<java>\w+)\s+(?<field>\w+)\s*;')
    $fksModel = 0
    foreach ($campoJava in $camposJava) {
        $atributos = $campoJava.Groups['attrs'].Value
        $tipoJava = $campoJava.Groups['java'].Value
        $campo = $campoJava.Groups['field'].Value
        $attrColuna = [regex]::Match($atributos,'@(?:Column|JoinColumn)(?:\((?:[^()"]|"[^"]*")*\))?').Value
        $nomeExplicito = [regex]::Match($attrColuna,'name\s*=\s*"([^"]+)"').Groups[1].Value
        if ($nomeExplicito) { $coluna = $nomeExplicito } else { $coluna = [regex]::Replace($campo,'[A-Z]',{param($m) '_' + $m.Value.ToLowerInvariant()}) }
        $nuloEsperado = if ($atributos -match '@Id\b' -or $tipoJava -eq 'long' -or $attrColuna -match 'nullable\s*=\s*false') {'NOT NULL'} else {'NULL'}
        $ehFk = $atributos -match '@JoinColumn'
        if ($ehFk -or $tipoJava -in @('Long','long')) { $tipoEsperado = 'bigint' }
        elseif ($tipoJava -in @('boolean','Boolean')) { $tipoEsperado = 'bit' }
        elseif ($tipoJava -eq 'Instant') { $tipoEsperado = 'datetime2(6)' }
        elseif ($tipoJava -eq 'LocalDate') { $tipoEsperado = 'date' }
        elseif ($tipoJava -eq 'BigDecimal') {
            $precisao = [regex]::Match($attrColuna,'precision\s*=\s*(\d+)').Groups[1].Value
            $escala = [regex]::Match($attrColuna,'scale\s*=\s*(\d+)').Groups[1].Value
            $tipoEsperado = "decimal($precisao,$escala)"
        }
        elseif ($tipoJava -eq 'String') {
            $definicao = [regex]::Match($attrColuna,'columnDefinition\s*=\s*"([^"]+)"').Groups[1].Value
            $tamanho = [regex]::Match($attrColuna,'length\s*=\s*(\d+)').Groups[1].Value
            if ($definicao) { $tipoEsperado = $definicao }
            else { if (-not $tamanho) { $tamanho = '255' }; $prefixo = if ($atributos -match '@Nationalized') {'nvarchar'} else {'varchar'}; $tipoEsperado = "$prefixo($tamanho)" }
        }
        else { $divergencias += "$nomeTabela.$coluna tipo Java sem comparador"; continue }
        if (-not $colunasSql.ContainsKey($coluna)) { $divergencias += "$nomeTabela.$coluna ausente no SQL"; continue }
        $defSql = $colunasSql[$coluna]
        if ($defSql.Groups['tipo'].Value -cne $tipoEsperado) { $divergencias += "$nomeTabela.$coluna tipo diferente" }
        if ($defSql.Groups['nulo'].Value -cne $nuloEsperado) { $divergencias += "$nomeTabela.$coluna nulabilidade diferente" }
        if ($atributos -match '@Id\b' -and -not $defSql.Groups['identity'].Success) { $divergencias += "$nomeTabela.$coluna sem IDENTITY" }
        if ($ehFk) {
            $tipoRelacionado = Get-Content -LiteralPath (Join-Path $raizModels "$tipoJava.java") -Raw -Encoding UTF8
            $tabelaRelacionado = [regex]::Match($tipoRelacionado,'@Table\s*\(\s*name\s*=\s*"([^"]+)"').Groups[1].Value
            if ($blocoTabela -notmatch ('(?m)^\s*' + $coluna + '\s+bigint\s+(?:NOT NULL|NULL).*?REFERENCES wms\.' + $tabelaRelacionado + '\(id\)')) { $divergencias += "$nomeTabela.$coluna FK diferente" }
            $chaveFk = [regex]::Escape($coluna)
            $indiceEmTabela = $blocoTabela -match ('UNIQUE\s*\(\s*' + $chaveFk + '\s*[,)]')
            $indiceExterno = $sqlV6 -match ('CREATE (?:UNIQUE )?INDEX \w+ ON wms\.' + $nomeTabela + '\s*\(\s*' + $chaveFk + '\s*[,)]')
            if (-not ($indiceEmTabela -or $indiceExterno)) { $divergencias += "$nomeTabela.$coluna sem indice na primeira coluna" }
            $fksModel++
        }
    }
    $unicasModel = 0
    foreach ($unica in [regex]::Matches($javaFonte,'@UniqueConstraint\(\s*name\s*=\s*"(?<nome>[^"]+)"\s*,\s*columnNames\s*=\s*(?:"(?<simples>[^"]+)"|\{(?<composta>[^}]+)\})\s*\)')) {
        $nomeUnica = $unica.Groups['nome'].Value
        if ($unica.Groups['simples'].Success) { $colunasUnica = $unica.Groups['simples'].Value }
        else { $colunasUnica = ([regex]::Matches($unica.Groups['composta'].Value,'"([^"]+)"') | ForEach-Object { $_.Groups[1].Value }) -join ',' }
        $sqlUnica = [regex]::Match($blocoTabela,('CONSTRAINT ' + $nomeUnica + '\s+UNIQUE\s*\(([^)]+)\)')).Groups[1].Value -replace '\s',''
        if ($nomeUnica -eq 'uk_documento_saida_chave') {
            # JPA/H2 UNIQUE nao expressa indice filtrado; doc27 exige multiplos NULLs.
            if ($sqlV6 -notmatch 'CREATE UNIQUE INDEX uk_documento_saida_chave ON wms\.documento_saida\(chave_acesso\)\s+WHERE chave_acesso IS NOT NULL;') { $divergencias += "$nomeTabela.$nomeUnica filtro diferente" }
        }
        elseif ($sqlUnica -cne $colunasUnica) { $divergencias += "$nomeTabela.$nomeUnica chave unica diferente" }
        $unicasModel++
    }
    if ($camposJava.Count -ne $colunasSql.Count) { $divergencias += "$nomeTabela contagem de campos diferente" }
    $totalColunas += $camposJava.Count
    $totalFks += $fksModel
    $totalUnicas += $unicasModel
    $resumoModels += [pscustomobject][ordered]@{model=$nomeModel; tabela=$nomeTabela; colunas=$camposJava.Count; fks=$fksModel; unicas=$unicasModel; sha256=(Get-FileHash -LiteralPath $arquivoModel -Algorithm SHA256).Hash}
}
$arquivoUnidade = Join-Path $raizModels 'UnidadeLogistica.java'
$unidadeFonte = Get-Content -LiteralPath $arquivoUnidade -Raw -Encoding UTF8
if ($unidadeFonte -notmatch '@Column\(name\s*=\s*"avaria_inicial_reparada",\s*nullable\s*=\s*false\)\s*private boolean avariaInicialReparada;') { $divergencias += 'unidade_logistica.avaria_inicial_reparada mapeamento diferente' }
if ($sqlV6 -notmatch 'avaria_inicial_reparada bit NOT NULL CONSTRAINT df_unidade_avaria_inicial_reparada DEFAULT \(0\);\s*GO\s*ALTER TABLE wms\.unidade_logistica WITH CHECK ADD CONSTRAINT ck_unidade_avaria_inicial_reparada CHECK \(\s*avaria_inicial_reparada = 0 OR condicao = ''AVARIADA''\s*\)') { $divergencias += 'unidade_logistica.avaria_inicial_reparada definicao/lote/check diferente' }

function Lista-Check([string] $fonte, [string] $nome, [string] $coluna) {
    $lista = [regex]::Match($fonte,('(?s)CONSTRAINT ' + $nome + '\s+CHECK\s*\(\s*' + $coluna + '\s+IN\s*\(([^)]*)\)')).Groups[1].Value
    @([regex]::Matches($lista,"'([^']+)'") | ForEach-Object { $_.Groups[1].Value })
}
function Comparar-Lista([string] $identificador, [object[]] $esperada, [object[]] $observada) {
    if ($esperada.Count -eq 0 -or $observada.Count -eq 0 -or @((Compare-Object -ReferenceObject $esperada -DifferenceObject $observada)).Count -gt 0) { $script:divergencias += "$identificador lista diferente" }
}
foreach ($enum in @(@('SituacaoPedidoSaida','ck_pedido_saida_situacao'),@('SituacaoReservaSaida','ck_reserva_saida_situacao'))) {
    $fonteEnum = Get-Content -LiteralPath (Join-Path $raizModels ($enum[0] + '.java')) -Raw -Encoding UTF8
    $valoresEnum = @([regex]::Matches(([regex]::Match($fonteEnum,'(?s)enum \w+\s*\{(.*?)\}').Groups[1].Value),'\b[A-Z][A-Z_]+\b') | ForEach-Object { $_.Value })
    Comparar-Lista $enum[0] $valoresEnum @(Lista-Check $sqlV6 $enum[1] 'situacao')
}
$sqlV5 = Get-Content -LiteralPath (Join-Path $raizProjeto 'database/migrations/V5__pedidos_saida_fifo_e_reservas.sql') -Raw -Encoding UTF8
$auditoriaAnterior = @(Lista-Check $sqlV5 'ck_auditoria_acao' 'acao')
$auditoriaV6 = @(Lista-Check $sqlV6 'ck_auditoria_acao' 'acao')
$acoesNovas = @('LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA','CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA','RETORNO_INTERNO','DEVOLUCAO_SAIDA','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA')
Comparar-Lista 'auditoria.acoes_cumulativas' @($auditoriaAnterior + $acoesNovas) $auditoriaV6
$movimentosAnteriores = @(Lista-Check $sqlV5 'ck_movimento_acao' 'acao')
$movimentosV6 = @(Lista-Check $sqlV6 'ck_movimento_acao' 'acao')
Comparar-Lista 'movimento.acoes_cumulativas' @($movimentosAnteriores + @('AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA')) $movimentosV6

$sqlSemComentarios = $sqlV6 -replace '(?m)--.*$', ''
if ($sqlSemComentarios -match '\bON\s+(?:DELETE|UPDATE)\s+CASCADE\b|\b(?:UPDATE|INSERT\s+INTO|DELETE\s+FROM|MERGE|TRUNCATE|GRANT|EXEC(?:UTE)?)\b') { $divergencias += 'SQL possui DML/cascata/concessao/execucao nao prevista' }
$arquivoContrato = Join-Path $raizProjeto 'docs/27-separacao-retirada-retornos-e-avaria.md'
[pscustomobject][ordered]@{natureza='Comparacao estatica JPA/contrato/arquivo SQL; nao executa JPA ou SQL e exige reconferencia dos models finais'; tabelas=$resumoModels.Count; colunas=$totalColunas; extensoesUnidade=1; fks=$totalFks; chavesUnicas=$totalUnicas; acoesAuditoriaAnteriores=$auditoriaAnterior.Count; acoesAuditoriaNovas=$acoesNovas.Count; acoesMovimento=$movimentosV6.Count; models=$resumoModels; unidadeSha256=(Get-FileHash -LiteralPath $arquivoUnidade -Algorithm SHA256).Hash; contratoSha256=(Get-FileHash -LiteralPath $arquivoContrato -Algorithm SHA256).Hash; divergencias=@($divergencias); migrationSha256=(Get-FileHash -LiteralPath $arquivoV6 -Algorithm SHA256).Hash} | ConvertTo-Json -Depth 5
if ($divergencias.Count -gt 0) { exit 1 }
