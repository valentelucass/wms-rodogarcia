# Leitor local de arquivos V7; nao conecta banco, nao executa JPA/SQL/build.
[CmdletBinding()]
param([switch]$CompararJpa)
$ErrorActionPreference = 'Stop'
$raizProjeto = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$arquivoContrato = Join-Path $raizProjeto 'database/contratos/v7-schema-doc29.json'
$arquivoSql = Join-Path $raizProjeto 'database/migrations/V7__cadastros_servicos_e_calculo.sql'
$contrato = Get-Content -LiteralPath $arquivoContrato -Raw -Encoding UTF8 | ConvertFrom-Json
$sql = Get-Content -LiteralPath $arquivoSql -Raw -Encoding UTF8
$sqlSemComentarios = $sql -replace '(?m)--.*$', ''
$divergencias = @()
$checks = [ordered]@{}
function Registrar([string]$nome, [bool]$ok) {
    $script:checks[$nome] = $ok
    if (-not $ok) { $script:divergencias += $nome }
}
function Normalizar([string]$texto) { (($texto -replace '(?m)--.*$', '') -replace '\s','').ToUpperInvariant() }
function Mesmo-Conjunto([object[]]$esperado, [object[]]$observado) {
    if ($esperado.Count -ne $observado.Count) {return $false}
    if ($esperado.Count -eq 0) {return $true}
    @(Compare-Object -ReferenceObject $esperado -DifferenceObject $observado).Count -eq 0
}
function Corpo-Check([string]$fonte,[string]$nome) {
    $inicio = [regex]::Match($fonte,('CONSTRAINT ' + [regex]::Escape($nome) + '\s+CHECK\s*\('))
    if (-not $inicio.Success) {return ''}
    $cursor = $inicio.Index + $inicio.Length; $comeco = $cursor; $nivel = 1; $literal = $false
    while ($cursor -lt $fonte.Length -and $nivel -gt 0) {
        $charAtual = $fonte[$cursor]
        if ($charAtual -eq "'") {$literal = -not $literal}
        elseif (-not $literal) {
            if ($charAtual -eq '(') {$nivel++}
            elseif ($charAtual -eq ')') {$nivel--}
        }
        $cursor++
    }
    if ($nivel -ne 0) {return ''}
    $fonte.Substring($comeco,$cursor-$comeco-1)
}
function Literais([string]$fonte) { @([regex]::Matches($fonte,"'([^']+)'") | ForEach-Object {$_.Groups[1].Value}) }
function Colunas-Sql([string]$fonte) {
    @([regex]::Matches($fonte,'(?m)^\s*(?<nome>[a-z]\w*)\s+(?<tipo>bigint|int|bit|date(?!time)|datetime2\(\d+\)|decimal\(\d+,\d+\)|n?varchar\((?:\d+|max)\))(?<identity>\s+IDENTITY\(1,1\))?\s+(?<nulo>NOT NULL|NULL)\b'))
}
function Conferir-Colunas([string]$tabela,[object[]]$esperadas,[string]$fonte) {
    $observadas = @(Colunas-Sql $fonte)
    Registrar "$tabela.colunas" (Mesmo-Conjunto @($esperadas | ForEach-Object {$_.nome}) @($observadas | ForEach-Object {$_.Groups['nome'].Value}))
    foreach ($coluna in $esperadas) {
        $def = $observadas | Where-Object {$_.Groups['nome'].Value -ceq $coluna.nome}
        $nulo = if ($coluna.nulo) {'NULL'} else {'NOT NULL'}
        Registrar "$tabela.$($coluna.nome).tipo_nulo_identity" (
            $null -ne $def -and $def.Groups['tipo'].Value -ceq $coluna.tipo -and
            $def.Groups['nulo'].Value -ceq $nulo -and $def.Groups['identity'].Success -eq [bool]$coluna.identity)
        if ($coluna.referencia) {
            Registrar "$tabela.$($coluna.nome).FK" ($fonte -match ('(?m)^\s*' + [regex]::Escape($coluna.nome) + '\s+bigint\s+(?:NOT NULL|NULL)[^\r\n]*REFERENCES wms\.' + [regex]::Escape($coluna.referencia) + '\(id\)'))
        }
    }
}
$fonteAtual = Join-Path $raizProjeto $contrato.fonte
Registrar 'doc29.hash_da_transcricao' ((Get-FileHash -LiteralPath $fonteAtual -Algorithm SHA256).Hash -ceq $contrato.fonteSha256)
$fonteDoc29 = Get-Content -LiteralPath $fonteAtual -Raw -Encoding UTF8
Registrar 'doc29.situacao_servico_formal_24' ($fonteDoc29 -match '(?m)^\| servico_cobranca \|[^\r\n]*situacao VARCHAR\(24\)')
$hashesPreservados = @()
$fontesAnteriores = @{}
foreach ($anterior in $contrato.baselineV1V6) {
    $arquivoAnterior = Join-Path $raizProjeto $anterior.arquivo
    $atual = (Get-FileHash -LiteralPath $arquivoAnterior -Algorithm SHA256).Hash
    Registrar ('preservacao.' + $anterior.arquivo) ($atual -ceq $anterior.sha256)
    $hashesPreservados += [pscustomobject]@{arquivo=$anterior.arquivo;sha256=$atual}
    $fontesAnteriores[$anterior.arquivo] = Get-Content -LiteralPath $arquivoAnterior -Raw -Encoding UTF8
}
Registrar 'baseline.seis_migrations' ($hashesPreservados.Count -eq 6)
$tabelasSql = @([regex]::Matches($sql,'(?s)CREATE TABLE wms\.(?<nome>\w+)\s*\((?<corpo>.*?)\r?\n\);'))
Registrar 'schema.tabelas_exatas' (Mesmo-Conjunto @($contrato.tabelas | ForEach-Object {$_.nome}) @($tabelasSql | ForEach-Object {$_.Groups['nome'].Value}))
$todasFontesSql = @($fontesAnteriores.Values) -join "`n"
$tabelasExistentes = @([regex]::Matches(($todasFontesSql + $sql),'CREATE TABLE wms\.(\w+)') | ForEach-Object {$_.Groups[1].Value})
Registrar 'FK.alvos_existem_nas_migrations' (@($contrato.tabelas | ForEach-Object {$_.colunas} | Where-Object {$_.referencia -and $_.referencia -notin $tabelasExistentes}).Count -eq 0)
$nomesConstraints = @([regex]::Matches($sqlSemComentarios,'CONSTRAINT\s+(\w+)\s+(?:PRIMARY KEY|UNIQUE|CHECK|REFERENCES)') | ForEach-Object {$_.Groups[1].Value})
Registrar 'constraints.nomes_unicos_novos' (@($nomesConstraints | Group-Object | Where-Object {$_.Count -gt 1}).Count -eq 0)
Registrar 'identificadores.limite_SQL_Server_128' (@($nomesConstraints | Where-Object {$_.Length -gt 128}).Count -eq 0)
$resumo = @(); $totalColunas = 0; $totalFk = 0; $totalUnicas = 0; $totalChecks = 0
foreach ($tabela in $contrato.tabelas) {
    $nome = $tabela.nome
    $bloco = ($tabelasSql | Where-Object {$_.Groups['nome'].Value -ceq $nome}).Groups['corpo'].Value
    Conferir-Colunas $nome @($tabela.colunas) $bloco
    foreach ($unica in $tabela.unicas) {
        $observada = [regex]::Match($bloco,('CONSTRAINT ' + [regex]::Escape($unica.nome) + '\s+UNIQUE\s*\(([^)]+)\)')).Groups[1].Value
        Registrar "$nome.$($unica.nome)" ((Normalizar $observada) -ceq (Normalizar ($unica.colunas -join ',')))
    }
    Registrar "$nome.quantidade_unicas" ([regex]::Matches($bloco,'CONSTRAINT \w+ UNIQUE\s*\(').Count -eq @($tabela.unicas).Count)
    foreach ($regra in $tabela.checks.PSObject.Properties) {
        Registrar "$nome.$($regra.Name)" ((Normalizar (Corpo-Check $bloco $regra.Name)) -ceq (Normalizar $regra.Value))
    }
    Registrar "$nome.quantidade_checks" ([regex]::Matches($bloco,'CONSTRAINT \w+ CHECK\s*\(').Count -eq @($tabela.checks.PSObject.Properties).Count)
    foreach ($coluna in @($tabela.colunas | Where-Object {$_.referencia})) {
        $prefixo = [regex]::Escape($coluna.nome)
        $indexada = $bloco -match ('UNIQUE\s*\(\s*' + $prefixo + '\s*[,)]') -or $sql -match ('CREATE (?:UNIQUE )?INDEX \w+ ON wms\.' + $nome + '\s*\(\s*' + $prefixo + '\s*[,)]')
        Registrar "$nome.$($coluna.nome).indice_FK" $indexada
    }
    Registrar "$nome.PK_identity" ($bloco -match ('id bigint IDENTITY\(1,1\) NOT NULL CONSTRAINT pk_' + [regex]::Escape($nome) + ' PRIMARY KEY'))
    foreach ($coluna in @($tabela.colunas | Where-Object {$_.tipo -match '^n?varchar\(\d+\)$'})) {
        $limite = [int][regex]::Match($coluna.tipo,'\d+').Value
        $dominio = @()
        foreach ($lista in [regex]::Matches($bloco,('\b' + [regex]::Escape($coluna.nome) + '\s+(?:NOT\s+)?IN\s*\(([^)]*)\)'))) { $dominio += @(Literais $lista.Groups[1].Value) }
        foreach ($valor in [regex]::Matches($bloco,('\b' + [regex]::Escape($coluna.nome) + '\s*(?:=|<>)\s*''([^'']*)'''))) { $dominio += $valor.Groups[1].Value }
        if ($dominio.Count -gt 0) {Registrar "$nome.$($coluna.nome).dominio_cabe_no_tamanho" (@($dominio | Where-Object {$_.Length -gt $limite}).Count -eq 0)}
    }
    $qtdFk = @($tabela.colunas | Where-Object {$_.referencia}).Count
    $resumo += [pscustomobject]@{tabela=$nome;colunas=$tabela.colunas.Count;fks=$qtdFk;unicas=@($tabela.unicas).Count;checks=@($tabela.checks.PSObject.Properties).Count}
    $totalColunas += $tabela.colunas.Count; $totalFk += $qtdFk; $totalUnicas += @($tabela.unicas).Count; $totalChecks += @($tabela.checks.PSObject.Properties).Count
}
$totalExtensoes = 0
foreach ($extensao in $contrato.extensoes.PSObject.Properties) {
    $bloco = [regex]::Match($sql,('(?s)ALTER TABLE wms\.' + $extensao.Name + ' ADD\s*(.*?);')).Groups[1].Value
    Conferir-Colunas $extensao.Name @($extensao.Value) $bloco
    Registrar ($extensao.Name + '.opcionais_sem_default') (@($extensao.Value | Where-Object {-not $_.nulo}).Count -eq 0 -and $bloco -notmatch '\bDEFAULT\b')
    $baseV1 = $fontesAnteriores['database/migrations/V1__cadastros_e_auditoria.sql']
    $originais = [regex]::Match($baseV1,('(?s)CREATE TABLE wms\.' + $extensao.Name + '\s*\((.*?)\r?\n\);')).Groups[1].Value
    $nomesOriginais = @(Colunas-Sql $originais | ForEach-Object {$_.Groups['nome'].Value})
    Registrar ($extensao.Name + '.sem_duplicar_existentes') (@($extensao.Value | Where-Object {$_.nome -in $nomesOriginais}).Count -eq 0)
    $totalExtensoes += @($extensao.Value).Count
}
Registrar 'indices.quantidade_declarada' ([regex]::Matches($sqlSemComentarios,'CREATE INDEX \w+ ON wms\.').Count -eq @($contrato.indices).Count)
foreach ($indice in $contrato.indices) {
    $observado = [regex]::Match($sql,('CREATE INDEX ' + [regex]::Escape($indice.nome) + ' ON wms\.' + $indice.tabela + '\s*\(([^)]+)\)')).Groups[1].Value
    Registrar ('indice.' + $indice.nome) ((Normalizar $observado) -ceq (Normalizar ($indice.colunas -join ',')))
}
$fonteV5 = $fontesAnteriores['database/migrations/V5__pedidos_saida_fifo_e_reservas.sql']
$fonteV6 = $fontesAnteriores['database/migrations/V6__separacao_retirada_retornos_e_avaria.sql']
Registrar 'auditoria.tipos_anteriores_reais' (Mesmo-Conjunto @($contrato.auditoria.tiposAnteriores) @(Literais (Corpo-Check $fonteV5 'ck_auditoria_tipo')))
Registrar 'auditoria.acoes_anteriores_reais' (Mesmo-Conjunto @($contrato.auditoria.acoesAnteriores) @(Literais (Corpo-Check $fonteV6 'ck_auditoria_acao')))
Registrar 'auditoria.tipos_cumulativos' (Mesmo-Conjunto @($contrato.auditoria.tiposAnteriores + $contrato.auditoria.tiposNovos) @(Literais (Corpo-Check $sql 'ck_auditoria_tipo')))
Registrar 'auditoria.acoes_cumulativas' (Mesmo-Conjunto @($contrato.auditoria.acoesAnteriores + $contrato.auditoria.acoesNovas) @(Literais (Corpo-Check $sql 'ck_auditoria_acao')))
Registrar 'auditoria.pares_financeiros' ((Normalizar (Corpo-Check $sql 'ck_auditoria_financeira_tipo_acao')) -ceq (Normalizar $contrato.auditoria.checkFinanceiro))
Registrar 'auditoria.pares_V6_nao_removidos' ($sqlSemComentarios -notmatch '\b(?:DROP|ADD) CONSTRAINT ck_auditoria_saida_tipo_acao\b')
Registrar 'movimento_estoque.sem_alteracao' ($sqlSemComentarios -notmatch '\bwms\.movimento_estoque\b')
Registrar 'auditoria.tipo_acao_cabem_24_30' (
    @($contrato.auditoria.tiposAnteriores + $contrato.auditoria.tiposNovos | Where-Object {$_.Length -gt 24}).Count -eq 0 -and
    @($contrato.auditoria.acoesAnteriores + $contrato.auditoria.acoesNovas | Where-Object {$_.Length -gt 30}).Count -eq 0)
Registrar 'operacao.recurso_sem_FK_generica' ($sqlSemComentarios -notmatch 'recurso_id bigint[^\r\n]*REFERENCES|FOREIGN KEY\s*\(\s*recurso_id')
Registrar 'alvo.guarda_placeholder' ($sql.Contains("IF DB_NAME() <> N'" + '$' + "{wmsDatabase}'"))
Registrar 'historico.sem_DML_default_cascata_grants_exec_trigger' ($sqlSemComentarios -notmatch '\b(?:INSERT|UPDATE|DELETE|MERGE|TRUNCATE|DEFAULT|GRANT|DENY|EXEC(?:UTE)?|TRIGGER)\b|\bON\s+(?:DELETE|UPDATE)\s+CASCADE\b')
Registrar 'historico.sem_alterar_colunas_anteriores' ($sqlSemComentarios -notmatch '\bALTER COLUMN\b|\bDROP TABLE\b')
$verificacoesBaseContratoSql = $checks.Count
$operacoesAdministrativas = [ordered]@{solicitado=[bool]$CompararJpa;literaisSalvos=@();fontes=@();dominioSql=@();limite='Literais diretos dos services que injetam OperacaoAdministrativaService; nao executa Java/SQL'}
$jpa = [ordered]@{solicitado=[bool]$CompararJpa;situacao='NAO_CONFERIDO_AGUARDA_FREEZE';models=@();fontesEmbeddable=@();colunasConferidas=0;divergencias=@()}
if ($CompararJpa) {
    $raizModels = Join-Path $raizProjeto 'backend/src/main/java/br/com/rodogarcia/wms/models'
    $mapaModels = @{}
    foreach ($arquivo in Get-ChildItem -LiteralPath $raizModels -Filter '*.java' -File) {
        $fonte = Get-Content -LiteralPath $arquivo.FullName -Raw -Encoding UTF8
        $nomeTabela = [regex]::Match($fonte,'@Table\s*\(\s*name\s*=\s*"([^"]+)"').Groups[1].Value
        if ($nomeTabela) {$mapaModels[$nomeTabela] = [pscustomobject]@{arquivo=$arquivo.FullName;fonte=$fonte}}
    }
    $alvosJpa = @($contrato.tabelas | ForEach-Object {[pscustomobject]@{nome=$_.nome;colunas=$_.colunas;unicas=$_.unicas;extensao=$false}})
    $alvosJpa += @($contrato.extensoes.PSObject.Properties | ForEach-Object {[pscustomobject]@{nome=$_.Name;colunas=$_.Value;unicas=@();extensao=$true}})
    foreach ($alvo in $alvosJpa) {
        $model = $mapaModels[$alvo.nome]
        if (-not $model) {$jpa.divergencias += $alvo.nome + '.model_ausente';continue}
        $campos = @{}
        $fonteCampos = $model.fonte
        if ($model.fonte -match '@AttributeOverrides?\b') {$jpa.divergencias += $alvo.nome + '.override_nao_suportado_pelo_leitor'}
        foreach ($embutido in [regex]::Matches($model.fonte,'@Embedded\s+private\s+(?<java>\w+)\s+\w+\s*;')) {
            $arquivoEmbutido = Join-Path $raizModels ($embutido.Groups['java'].Value + '.java')
            $fonteEmbutida = Get-Content -LiteralPath $arquivoEmbutido -Raw -Encoding UTF8
            if ($fonteEmbutida -notmatch '@Embeddable\b' -or $fonteEmbutida -match '@Embedded\b|@AttributeOverrides?\b') {$jpa.divergencias += $alvo.nome + '.embeddable_nao_suportado_pelo_leitor';continue}
            $fonteCampos += [Environment]::NewLine + $fonteEmbutida
            $jpa.fontesEmbeddable += [pscustomobject]@{tabela=$alvo.nome;arquivo=$arquivoEmbutido.Substring($raizProjeto.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $arquivoEmbutido -Algorithm SHA256).Hash}
        }
        foreach ($campo in [regex]::Matches($fonteCampos,'(?<attrs>(?:@\w+(?:\((?:[^()"]|"[^"]*")*\))?\s*)+)\s*private\s+(?<java>\w+)\s+(?<field>\w+)\s*;')) {
            $attrs = $campo.Groups['attrs'].Value
            if ($attrs -match '@Transient\b|@Embedded\b') {continue}
            $col = [regex]::Match($attrs,'@(?:Column|JoinColumn)(?:\((?:[^()"]|"[^"]*")*\))?').Value
            $nome = [regex]::Match($col,'name\s*=\s*"([^"]+)"').Groups[1].Value
            if (-not $nome) {$nome = [regex]::Replace($campo.Groups['field'].Value,'[A-Z]',{param($m) '_' + $m.Value.ToLowerInvariant()})}
            $java = $campo.Groups['java'].Value
            $tipo = ''
            $referencia = $null
            if ($attrs -match '@JoinColumn') {
                $relacionado = Get-Content -LiteralPath (Join-Path $raizModels ($java + '.java')) -Raw -Encoding UTF8
                $referencia = [regex]::Match($relacionado,'@Table\s*\(\s*name\s*=\s*"([^"]+)"').Groups[1].Value
                $tipo = 'bigint'
            }
            elseif ($java -in @('long','Long')) {$tipo='bigint'}
            elseif ($java -in @('int','Integer')) {$tipo='int'}
            elseif ($java -in @('boolean','Boolean')) {$tipo='bit'}
            elseif ($java -eq 'Instant') {$tipo='datetime2(6)'}
            elseif ($java -eq 'LocalDate') {$tipo='date'}
            elseif ($java -eq 'BigDecimal') {
                $p=[regex]::Match($col,'precision\s*=\s*(\d+)').Groups[1].Value
                $s=[regex]::Match($col,'scale\s*=\s*(\d+)').Groups[1].Value
                $tipo="decimal($p,$s)"
            }
            elseif ($java -eq 'String' -or $attrs -match '@Enumerated') {
                $def=[regex]::Match($col,'columnDefinition\s*=\s*"([^"]+)"').Groups[1].Value
                $tam=[regex]::Match($col,'length\s*=\s*(\d+)').Groups[1].Value
                if ($def) {$tipo=$def.ToLowerInvariant()} else {
                    if (-not $tam) {$tam='255'}
                    $prefixo=if($attrs -match '@Nationalized') {'nvarchar'} else {'varchar'}
                    $tipo="$prefixo($tam)"
                }
            }
            $nulo = -not ($attrs -match '@Id\b' -or $java -cin @('long','int','boolean') -or $col -match 'nullable\s*=\s*false')
            $campos[$nome] = [pscustomobject]@{tipo=$tipo;nulo=$nulo;referencia=$referencia;attrs=$attrs}
        }
        if (-not $alvo.extensao -and -not (Mesmo-Conjunto @($alvo.colunas | ForEach-Object {$_.nome}) @($campos.Keys))) {$jpa.divergencias += $alvo.nome + '.colunas_diferentes'}
        foreach ($coluna in $alvo.colunas) {
            $campo=$campos[$coluna.nome]
            if (-not $campo -or $campo.tipo -cne $coluna.tipo -or $campo.nulo -ne $coluna.nulo -or $campo.referencia -cne $coluna.referencia) {$jpa.divergencias += $alvo.nome + '.' + $coluna.nome + '.tipo_nulo_FK'}
            if ($coluna.identity -and $campo.attrs -notmatch 'GenerationType\.IDENTITY') {$jpa.divergencias += $alvo.nome + '.' + $coluna.nome + '.identity'}
            $jpa.colunasConferidas++
        }
        if (-not $alvo.extensao) {
            $unicasJpa = @([regex]::Matches($model.fonte,'@UniqueConstraint\(\s*name\s*=\s*"(?<nome>[^"]+)"\s*,\s*columnNames\s*=\s*(?:"(?<simples>[^"]+)"|\{(?<composta>[^}]+)\})\s*\)'))
            if ($unicasJpa.Count -ne @($alvo.unicas).Count) {$jpa.divergencias += $alvo.nome + '.quantidade_unicas'}
            foreach ($unica in $alvo.unicas) {
                $observada = $unicasJpa | Where-Object {$_.Groups['nome'].Value -ceq $unica.nome}
                if (-not $observada) {$jpa.divergencias += $alvo.nome + '.' + $unica.nome + '.ausente';continue}
                $nomes = if ($observada.Groups['simples'].Success) {$observada.Groups['simples'].Value} else {([regex]::Matches($observada.Groups['composta'].Value,'"([^"]+)"') | ForEach-Object {$_.Groups[1].Value}) -join ','}
                if ($nomes -cne ($unica.colunas -join ',')) {$jpa.divergencias += $alvo.nome + '.' + $unica.nome + '.chave_unica'}
            }
        }
        $jpa.models += [pscustomobject]@{tabela=$alvo.nome;arquivo=$model.arquivo.Substring($raizProjeto.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $model.arquivo -Algorithm SHA256).Hash}
    }
    $jpa.situacao = if ($jpa.divergencias.Count -eq 0) {'COMPATIVEL_EM_LEITURA'} else {'DIVERGENCIAS_EM_LEITURA'}
    $divergencias += @($jpa.divergencias)
    # Inventario efetivo de operacao_administrativa, separado das acoes da auditoria.
    $operacoesAdministrativas.dominioSql = @(Literais (Corpo-Check $sql 'ck_operacao_administrativa_tipo'))
    $raizServices = Join-Path $raizProjeto 'backend/src/main/java/br/com/rodogarcia/wms/services'
    $construtoresOperacao = 0
    foreach ($arquivoService in Get-ChildItem -LiteralPath $raizServices -Filter '*.java' -File) {
        $fonteService = Get-Content -LiteralPath $arquivoService.FullName -Raw -Encoding UTF8
        $construtoresOperacao += [regex]::Matches($fonteService,'new\s+OperacaoAdministrativa\s*\(').Count
        $injecoes = @([regex]::Matches($fonteService,'private\s+final\s+OperacaoAdministrativaService\s+(?<variavel>\w+)\s*;'))
        if ($injecoes.Count -eq 0) {continue}
        $relService = $arquivoService.FullName.Substring($raizProjeto.Length+1).Replace('\','/')
        $operacoesAdministrativas.fontes += [pscustomobject]@{arquivo=$relService;sha256=(Get-FileHash -LiteralPath $arquivoService.FullName -Algorithm SHA256).Hash}
        foreach ($injecao in $injecoes) {
            $metodo = [regex]::Escape($injecao.Groups['variavel'].Value) + '\.salvar\s*\('
            $salvas = @([regex]::Matches($fonteService,($metodo + '\s*[^,]+,\s*"(?<tipo>[A-Z_]+)"')))
            Registrar ('operacao_administrativa.'+$arquivoService.BaseName+'.cobertura_literais') ($salvas.Count -eq [regex]::Matches($fonteService,$metodo).Count)
            foreach ($salva in $salvas) {
                $tipoSalvo = $salva.Groups['tipo'].Value
                $linhaSalva = [regex]::Matches($fonteService.Substring(0,$salva.Index),'\n').Count+1
                $aceito = $operacoesAdministrativas.dominioSql -ccontains $tipoSalvo
                $operacoesAdministrativas.literaisSalvos += [pscustomobject]@{arquivo=$relService;linha=$linhaSalva;tipo=$tipoSalvo;aceitoNoCheck=$aceito}
                Registrar ('operacao_administrativa.'+$arquivoService.BaseName+'.'+$linhaSalva+'.'+$tipoSalvo) $aceito
            }
        }
    }
    $arquivoGateway = Join-Path $raizServices 'OperacaoAdministrativaService.java'
    $fonteGateway = Get-Content -LiteralPath $arquivoGateway -Raw -Encoding UTF8
    $operacoesAdministrativas.fontes += [pscustomobject]@{arquivo='backend/src/main/java/br/com/rodogarcia/wms/services/OperacaoAdministrativaService.java';sha256=(Get-FileHash -LiteralPath $arquivoGateway -Algorithm SHA256).Hash}
    Registrar 'operacao_administrativa.tipo_encaminhado_sem_transformacao' ($fonteGateway -match 'new OperacaoAdministrativa\(\s*id.toString\(\),\s*tipo,')
    Registrar 'operacao_administrativa.construtor_central_unico' ($construtoresOperacao -eq 1 -and [regex]::Matches($fonteGateway,'new\s+OperacaoAdministrativa\s*\(').Count -eq 1)
}
$falhas = @($checks.Keys | Where-Object {-not $checks[$_]})
[pscustomobject][ordered]@{
    natureza='Comparacao estatica V7/schema29 transcrito e SQL; nenhuma conexao, JPA, SQL, H2 ou build'
    capturadoEmUtc=[DateTime]::UtcNow.ToString('o')
    tabelas=$contrato.tabelas.Count;colunas=$totalColunas;extensoes=$totalExtensoes;fks=$totalFk;unicas=$totalUnicas;checksSql=$totalChecks;indicesExplicitos=$contrato.indices.Count
    verificacoes=$checks.Count;atendidas=$checks.Count-$falhas.Count;verificacoesBaseContratoSql=$verificacoesBaseContratoSql;verificacoesAdicionaisOperacao=$checks.Count-$verificacoesBaseContratoSql
    auditoria=[pscustomobject]@{tiposAnteriores=$contrato.auditoria.tiposAnteriores.Count;tiposNovos=$contrato.auditoria.tiposNovos.Count;acoesAnteriores=$contrato.auditoria.acoesAnteriores.Count;acoesNovas=$contrato.auditoria.acoesNovas.Count;movimentoIntocado=$checks['movimento_estoque.sem_alteracao']}
    resumo=$resumo;checks=$checks;divergencias=@($divergencias);jpa=[pscustomobject]$jpa;operacoesAdministrativas=[pscustomobject]$operacoesAdministrativas
    hashes=[pscustomobject]@{sql=(Get-FileHash -LiteralPath $arquivoSql -Algorithm SHA256).Hash;transcricao=(Get-FileHash -LiteralPath $arquivoContrato -Algorithm SHA256).Hash;doc29=(Get-FileHash -LiteralPath $fonteAtual -Algorithm SHA256).Hash;leitor=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash;preservadosV1V6=$hashesPreservados}
} | ConvertTo-Json -Depth 8
if ($divergencias.Count -gt 0) {exit 1}
