# Leitor local de arquivos V8; nao conecta banco, nao executa JPA/SQL/build.
[CmdletBinding()]
param([switch]$CompararJpa,[string]$FonteDoc31)
$WmsD20Pathverificarschemav8=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference = 'Stop'
$raizProjeto = [IO.Path]::GetFullPath((Join-Path $WmsD20Pathverificarschemav8 '..'))
$arquivoContrato = Join-Path $raizProjeto 'database/contratos/v8-schema-doc31.json'
$arquivoSql = Join-Path $raizProjeto 'database/migrations/V8__fechamento_e_versoes.sql'
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
            if ($coluna.fkPosterior) {
                $padrao = 'ALTER TABLE wms\.' + [regex]::Escape($tabela) + '\s+WITH CHECK ADD CONSTRAINT ' + [regex]::Escape($coluna.fkPosterior) + '\s+FOREIGN KEY\s*\(\s*' + [regex]::Escape($coluna.nome) + '\s*\)\s+REFERENCES wms\.' + [regex]::Escape($coluna.referencia) + '\(id\)\s*;'
                $fks = @([regex]::Matches($script:sqlSemComentarios,$padrao))
                Registrar "$tabela.$($coluna.nome).FK" ($fks.Count -eq 1)
                $tabelasSemComentarios = @([regex]::Matches($script:sqlSemComentarios,'(?s)CREATE TABLE wms\.\w+\s*\(.*?\r?\n\);'))
                $fimTabelas = ($tabelasSemComentarios | ForEach-Object {$_.Index + $_.Length} | Measure-Object -Maximum).Maximum
                Registrar "$tabela.$($coluna.nome).FK_posterior_todas_tabelas" ($fks.Count -eq 1 -and $fks[0].Index -ge $fimTabelas)
            }
            else {
                Registrar "$tabela.$($coluna.nome).FK" ($fonte -match ('(?m)^\s*' + [regex]::Escape($coluna.nome) + '\s+bigint\s+(?:NOT NULL|NULL)[^\r\n]*REFERENCES wms\.' + [regex]::Escape($coluna.referencia) + '\(id\)'))
            }
        }
        if ($coluna.default) {
            $padraoDefault = '(?m)^\s*' + [regex]::Escape($coluna.nome) + '\s+' + [regex]::Escape($coluna.tipo) + '\s+NOT NULL\s+CONSTRAINT ' + [regex]::Escape($coluna.constraintDefault) + '\s+DEFAULT\s+''' + [regex]::Escape($coluna.default) + '''\s*,'
            Registrar "$tabela.$($coluna.nome).DEFAULT_formal_exato" ([regex]::Matches($fonte,$padraoDefault).Count -eq 1)
        }
    }
}
function Argumentos-Java([string]$fonte,[int]$aberto) {
    $argumentos=@();$inicio=$aberto+1;$nivel=0;$literal=$false;$escape=$false
    for($i=$inicio;$i -lt $fonte.Length;$i++){
        $ch=$fonte[$i]
        if($literal){
            if($escape){$escape=$false;continue}
            if($ch -eq '\'){$escape=$true;continue}
            if($ch -eq '"'){$literal=$false}
            continue
        }
        if($ch -eq '"'){$literal=$true;continue}
        if($ch -in @('(','[','{')){$nivel++;continue}
        if($ch -eq ')' -and $nivel -eq 0){
            $argumentos+=$fonte.Substring($inicio,$i-$inicio).Trim()
            return [pscustomobject]@{argumentos=$argumentos;fim=$i}
        }
        if($ch -in @(')',']','}')){$nivel--;continue}
        if($ch -eq ',' -and $nivel -eq 0){$argumentos+=$fonte.Substring($inicio,$i-$inicio).Trim();$inicio=$i+1}
    }
    return [pscustomobject]@{argumentos=@();fim=-1}
}
function Repasse-Fechamento([string]$fonte) {
    $assinatura='(?s)private\s+ConfirmacaoComando\s+concluir\s*\(\s*FechamentoCobranca\s+f,\s*VersaoFechamento\s+v,\s*UUID\s+id,\s*String\s+tipo,\s*String\s+motivo,\s*Object\s+antes,\s*String\s+hash,\s*Resolucao\s+resolucao\s*\)\s*\{(?<corpo>.*?)\r?\n\s*\}'
    $helper=[regex]::Match($fonte,$assinatura)
    if(-not $helper.Success){return [pscustomobject]@{encontrado=$false;validado=$false;literais=@();repasseOperacao=0;repasseAuditoria=0}}
    $corpo=$helper.Groups['corpo'].Value
    $forwardOp=[regex]::Matches($corpo,'operacoes\.salvar\(\s*id,\s*tipo,')
    $forwardAu=[regex]::Matches($corpo,'auditoria\.registrar\(\s*"FECHAMENTO_COBRANCA",\s*f\.getId\(\),\s*tipo,')
    $ok=$forwardOp.Count -eq 1 -and $forwardAu.Count -eq 1 -and [regex]::Matches($corpo,'\btipo\b').Count -eq 2 -and $corpo -notmatch '\btipo\s*(?:=|\+=|\+\+)'
    $chamadas=@([regex]::Matches($fonte,'(?<![\w.])concluir\s*\(')|Where-Object{$_.Index -lt $helper.Index -or $_.Index -ge $helper.Index+$helper.Length})
    $literais=@()
    foreach($chamada in $chamadas){
        $argumentos=Argumentos-Java $fonte ($chamada.Index+$chamada.Length-1)
        $tipo=if($argumentos.argumentos.Count -eq 8){[regex]::Match($argumentos.argumentos[3],'^"([A-Z_]+)"$').Groups[1].Value}else{''}
        if(-not $tipo){$ok=$false;continue}
        $literais+=[pscustomobject]@{tipo=$tipo;linha=([regex]::Matches($fonte.Substring(0,$chamada.Index),'\n').Count+1);via='concluir(String tipo, Resolucao resolucao), oito argumentos/repasse comprovado nos dois destinos'}
    }
    $ok=$ok -and $chamadas.Count -gt 0 -and $literais.Count -eq $chamadas.Count
    [pscustomobject]@{encontrado=$true;validado=$ok;literais=$literais;repasseOperacao=$forwardOp.Count;repasseAuditoria=$forwardAu.Count;chamadas=$chamadas.Count}
}
$fonteCanonica = Join-Path $raizProjeto $contrato.fonte
$fonteAtual = $fonteCanonica
if ($FonteDoc31) {
    $fonteAtual = [IO.Path]::GetFullPath((Join-Path $raizProjeto $FonteDoc31))
    $areaSnapshots = [IO.Path]::GetFullPath((Join-Path $raizProjeto 'database/evidencias')) + [IO.Path]::DirectorySeparatorChar
    if (-not $fonteAtual.StartsWith($areaSnapshots,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetExtension($fonteAtual) -cne '.md' -or -not (Test-Path -LiteralPath $fonteAtual -PathType Leaf)) {throw 'FonteDoc31 deve ser copia MD existente em database/evidencias, com SHA esperado na transcricao.'}
}
$hashFonteLida = (Get-FileHash -LiteralPath $fonteAtual -Algorithm SHA256).Hash
Registrar 'doc31.hash_da_transcricao' ($hashFonteLida -ceq $contrato.fonteSha256)
$fonteDoc31 = Get-Content -LiteralPath $fonteAtual -Raw -Encoding UTF8
Registrar 'doc31.fonte_estavel_durante_leitura' ((Get-FileHash -LiteralPath $fonteAtual -Algorithm SHA256).Hash -ceq $hashFonteLida)
$quadroDoc31 = [regex]::Match($fonteDoc31,'(?s)## Schema BE13 para V8.*?(?=## API BE13)').Value
$linhasDoc31 = @([regex]::Matches($quadroDoc31,'(?m)^\| (?<tabela>[a-z]\w*) \| (?<corpo>[^\r\n]+) \|'))
$complementoOrigem = [regex]::Match($quadroDoc31,'(?m)^\*\*Complemento V8, antes do JPA, sem tabela nova:\*\*[^\r\n]+').Value
$complementoOrigem = $complementoOrigem.Replace([string][char]96,'')
Registrar 'doc31.quadro_11_tabelas_exatas' (Mesmo-Conjunto @($contrato.tabelas.nome) @($linhasDoc31|ForEach-Object{$_.Groups['tabela'].Value}))
foreach($t in $contrato.tabelas){
    $linha=($linhasDoc31|Where-Object{$_.Groups['tabela'].Value -ceq $t.nome}).Groups['corpo'].Value
    foreach($col in @($t.colunas|Where-Object{$_.nome -cne 'id'})){
        $textoFormal = if ($col.formalNoComplemento) {$complementoOrigem} else {$linha}
        Registrar ('doc31.'+$t.nome+'.'+$col.nome+'.campo_formal') ($textoFormal -match ('\b'+[regex]::Escape($col.nome)+'\b'))
    }
}
Registrar 'doc31.p2_origem_quatro_campos_complementares_exatos' (Mesmo-Conjunto @('tipo','tratativa_origem_id','versao_base_anterior_id','dependencias_origem_json') @($contrato.tabelas.colunas | Where-Object {$_.formalNoComplemento} | ForEach-Object {$_.nome}))
Registrar 'doc31.p2_origem_tipo_DEFAULT_dominio_formais' ($complementoOrigem -match "ajuste_fechamento.tipo VARCHAR\(24\) NOT NULL DEFAULT 'CORRECAO_CALCULO'" -and $complementoOrigem -match 'CHECK CORRECAO_CALCULO\|REGULARIZACAO_ORIGEM')
Registrar 'doc31.p2_origem_FK_filtrada_formal' ($complementoOrigem -match 'tratativa_origem_id BIGINT NULL.*FK tratativa_externa_fechamento' -and $complementoOrigem -match 'WHERE tratativa_origem_id IS NOT NULL')
Registrar 'doc31.p2_origem_base_json_nulos_formais' ($complementoOrigem -match 'versao_base_anterior_id BIGINT NULL.*FK versao_fechamento' -and $complementoOrigem -match 'dependencias_origem_json NVARCHAR\(MAX\) NULL' -and $complementoOrigem -match 'CHECK ambos NULL ou ambos preenchidos' -and $complementoOrigem -match 'ISJSON')
Registrar 'doc31.p2_origem_sem_literal_administrativo_novo' ($complementoOrigem -match 'dez a[^.]*TRATATIVA_EXTERNA' -and $complementoOrigem -match 'sem literal administrativo adicional')
Registrar 'doc31.CONFLITO_EXTERNO_formal' ($quadroDoc31 -match '\| fechamento_cobranca \|[^\r\n]*CONFLITO_EXTERNO')
Registrar 'doc31.aplicado_versao_id_FK_NULL_formal' ($quadroDoc31 -match 'aplicado_versao_id FK versao_fechamento NULL')
Registrar 'doc31.FECHAMENTO_COBRANCA_19' ($quadroDoc31 -match 'FECHAMENTO_COBRANCA.*19 caracteres' -and 'FECHAMENTO_COBRANCA'.Length -eq 19)
$hashesPreservados = @()
$fontesAnteriores = @{}
foreach ($anterior in $contrato.baselineV1V7) {
    $arquivoAnterior = Join-Path $raizProjeto $anterior.arquivo
    $atual = (Get-FileHash -LiteralPath $arquivoAnterior -Algorithm SHA256).Hash
    Registrar ('preservacao.' + $anterior.arquivo) ($atual -ceq $anterior.sha256)
    $hashesPreservados += [pscustomobject]@{arquivo=$anterior.arquivo;sha256=$atual}
    $fontesAnteriores[$anterior.arquivo] = Get-Content -LiteralPath $arquivoAnterior -Raw -Encoding UTF8
}
Registrar 'baseline.sete_migrations' ($hashesPreservados.Count -eq 7)
$tabelasSql = @([regex]::Matches($sql,'(?s)CREATE TABLE wms\.(?<nome>\w+)\s*\((?<corpo>.*?)\r?\n\);'))
Registrar 'schema.tabelas_exatas' (Mesmo-Conjunto @($contrato.tabelas | ForEach-Object {$_.nome}) @($tabelasSql | ForEach-Object {$_.Groups['nome'].Value}))
$todasFontesSql = @($fontesAnteriores.Values) -join "`n"
$tabelasExistentes = @([regex]::Matches(($todasFontesSql + $sql),'CREATE TABLE wms\.(\w+)') | ForEach-Object {$_.Groups[1].Value})
Registrar 'FK.alvos_existem_nas_migrations' (@($contrato.tabelas | ForEach-Object {$_.colunas} | Where-Object {$_.referencia -and $_.referencia -notin $tabelasExistentes}).Count -eq 0)
$nomesConstraints = @([regex]::Matches($sqlSemComentarios,'CONSTRAINT\s+(\w+)\s+(?:PRIMARY KEY|UNIQUE|CHECK|REFERENCES|FOREIGN KEY|DEFAULT)') | ForEach-Object {$_.Groups[1].Value})
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
Registrar 'FK.quantidade_real_declarada' ([regex]::Matches($sqlSemComentarios,'\bREFERENCES wms\.').Count -eq $totalFk)
Registrar 'FK.posteriores_quantidade_exata' ([regex]::Matches($sqlSemComentarios,'\bFOREIGN KEY\s*\(').Count -eq @($contrato.tabelas.colunas | Where-Object {$_.fkPosterior}).Count)
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
$cadastrosEsperados = @('cliente','armazem','produto','embalagem','endereco','conjunto_posicoes')
Registrar 'cadastros.seis_ampliacoes_exatas' (Mesmo-Conjunto $cadastrosEsperados @($contrato.ampliacoesChecks.tabela))
$complementoCadastro = [regex]::Match($quadroDoc31,'(?m)^Extens[^\r\n]*SituacaoCadastro[^\r\n]+').Value
Registrar 'doc31.cadastro_enum_e_tamanho_formais' ($complementoCadastro -match 'ATIVO/ENCERRAMENTO_PENDENTE/INATIVO' -and $complementoCadastro -match 'VARCHAR24')
foreach ($ampliacao in $contrato.ampliacoesChecks) {
    $prefixo = 'cadastro.' + $ampliacao.tabela + '.'
    $fonteAnterior = $fontesAnteriores[$ampliacao.origem]
    $blocoAnterior = [regex]::Match($fonteAnterior,('(?s)CREATE TABLE wms\.' + [regex]::Escape($ampliacao.tabela) + '\s*\((.*?)\r?\n\);')).Groups[1].Value
    $situacaoAnterior = @(Colunas-Sql $blocoAnterior | Where-Object {$_.Groups['nome'].Value -ceq $ampliacao.coluna})
    Registrar ($prefixo + 'tabela_formal_doc31') ($complementoCadastro -match ('\b'+[regex]::Escape($ampliacao.tabela)+'\b'))
    Registrar ($prefixo + 'varchar24_NN_herdado') ($situacaoAnterior.Count -eq 1 -and $situacaoAnterior[0].Groups['tipo'].Value -ceq $ampliacao.tipo -and $situacaoAnterior[0].Groups['nulo'].Value -ceq 'NOT NULL' -and -not $ampliacao.nulo)
    Registrar ($prefixo + 'dominio_anterior_real') (Mesmo-Conjunto @($ampliacao.valoresAnteriores) @(Literais (Corpo-Check $blocoAnterior $ampliacao.nome)))
    $drop = 'ALTER TABLE wms\.'+[regex]::Escape($ampliacao.tabela)+' DROP CONSTRAINT '+[regex]::Escape($ampliacao.nome)+'\s*;'
    Registrar ($prefixo + 'substitui_CHECK_existente_uma_vez') ([regex]::Matches($sqlSemComentarios,$drop).Count -eq 1)
    $add = 'ALTER TABLE wms\.'+[regex]::Escape($ampliacao.tabela)+' WITH CHECK ADD CONSTRAINT '+[regex]::Escape($ampliacao.nome)+' CHECK\s*\('
    $corpo = Corpo-Check $sqlSemComentarios $ampliacao.nome
    $dominioEsperado = $ampliacao.coluna + ' IN (' + ((@($ampliacao.valores) | ForEach-Object {"'" + $_ + "'"}) -join ',') + ')'
    Registrar ($prefixo + 'WITH_CHECK_cumulativo_exato') ([regex]::Matches($sqlSemComentarios,$add).Count -eq 1 -and (Normalizar $corpo) -ceq (Normalizar $dominioEsperado))
    Registrar ($prefixo + 'dominio_cabe_24_preserva_anteriores') (@($ampliacao.valores|Where-Object{$_.Length -gt 24}).Count -eq 0 -and @($ampliacao.valoresAnteriores|Where-Object{$_ -cnotin $ampliacao.valores}).Count -eq 0)
    $altersTabela = @([regex]::Matches($sqlSemComentarios,('(?s)ALTER TABLE wms\.'+[regex]::Escape($ampliacao.tabela)+'\s+.*?;')))
    Registrar ($prefixo + 'sem_coluna_DML_ou_afrouxar_CHECK') ($altersTabela.Count -eq 2 -and ($altersTabela.Value -join ' ') -notmatch '\bNOCHECK\b|\bALTER COLUMN\b|\bADD\s+(?!CONSTRAINT)\w+|\bDROP COLUMN\b')
}
Registrar 'indices.quantidade_declarada' ([regex]::Matches($sqlSemComentarios,'CREATE INDEX \w+ ON wms\.').Count -eq @($contrato.indices).Count)
foreach ($indice in $contrato.indices) {
    $observado = [regex]::Match($sql,('CREATE INDEX ' + [regex]::Escape($indice.nome) + ' ON wms\.' + $indice.tabela + '\s*\(([^)]+)\)')).Groups[1].Value
    Registrar ('indice.' + $indice.nome) ((Normalizar $observado) -ceq (Normalizar ($indice.colunas -join ',')))
}
Registrar 'indices.filtrados_unicos_quantidade_declarada' ([regex]::Matches($sqlSemComentarios,'CREATE UNIQUE INDEX \w+ ON wms\.').Count -eq @($contrato.indicesFiltrados).Count)
foreach ($indice in $contrato.indicesFiltrados) {
    $declaracao = 'CREATE UNIQUE INDEX ' + $indice.nome + ' ON wms.' + $indice.tabela + '(' + ($indice.colunas -join ',') + ') WHERE ' + $indice.filtro + ';'
    $observado = [regex]::Match($sqlSemComentarios,('(?s)CREATE UNIQUE INDEX ' + [regex]::Escape($indice.nome) + '\b.*?;')).Value
    Registrar ('indice_filtrado.' + $indice.nome + '.chave_filtro_exatos') ($indice.unico -and (Normalizar $observado) -ceq (Normalizar $declaracao))
}
Registrar 'p2_origem.indice_filtrado_exato_nao_UNIQUE_nullable' (@($contrato.indicesFiltrados).Count -eq 1 -and $contrato.indicesFiltrados[0].nome -ceq 'ux_ajuste_fechamento_tratativa_origem' -and $contrato.indicesFiltrados[0].tabela -ceq 'ajuste_fechamento' -and $contrato.indicesFiltrados[0].filtro -ceq 'tratativa_origem_id IS NOT NULL' -and $sqlSemComentarios -notmatch 'CONSTRAINT\s+\w+\s+UNIQUE\s*\(\s*tratativa_origem_id\s*\)')
$fonteV7 = $fontesAnteriores['database/migrations/V7__cadastros_servicos_e_calculo.sql']
Registrar 'auditoria.tipos_anteriores_reais' (Mesmo-Conjunto @($contrato.auditoria.tiposAnteriores) @(Literais (Corpo-Check $fonteV7 'ck_auditoria_tipo')))
Registrar 'auditoria.acoes_anteriores_reais' (Mesmo-Conjunto @($contrato.auditoria.acoesAnteriores) @(Literais (Corpo-Check $fonteV7 'ck_auditoria_acao')))
Registrar 'operacao.tipos_anteriores_reais' (Mesmo-Conjunto @($contrato.auditoria.tiposOperacaoAnteriores) @(Literais (Corpo-Check $fonteV7 'ck_operacao_administrativa_tipo')))
Registrar 'auditoria.tipos_cumulativos' (Mesmo-Conjunto @($contrato.auditoria.tiposAnteriores + $contrato.auditoria.tiposNovos) @(Literais (Corpo-Check $sql 'ck_auditoria_tipo')))
Registrar 'auditoria.acoes_cumulativas' (Mesmo-Conjunto @($contrato.auditoria.acoesAnteriores + $contrato.auditoria.acoesNovas) @(Literais (Corpo-Check $sql 'ck_auditoria_acao')))
Registrar 'operacao.tipos_cumulativos' (Mesmo-Conjunto @($contrato.auditoria.tipoOperacaoAdministrativa) @(Literais (Corpo-Check $sql 'ck_operacao_administrativa_tipo')))
Registrar 'auditoria.pares_fechamento' ((Normalizar (Corpo-Check $sql 'ck_auditoria_fechamento_tipo_acao')) -ceq (Normalizar $contrato.auditoria.checkFechamento))
foreach($restricao in $contrato.auditoria.restricoesMantidas){Registrar ('auditoria.'+$restricao+'.preservada') ($sqlSemComentarios -notmatch ('\b(?:DROP|ADD) CONSTRAINT '+[regex]::Escape($restricao)+'\b'))}
Registrar 'movimento_estoque.sem_alteracao' ($sqlSemComentarios -notmatch '\bwms\.movimento_estoque\b')
Registrar 'auditoria.tipo_acao_cabem_24_30' (
    @($contrato.auditoria.tiposAnteriores + $contrato.auditoria.tiposNovos | Where-Object {$_.Length -gt 24}).Count -eq 0 -and
    @($contrato.auditoria.acoesAnteriores + $contrato.auditoria.acoesNovas | Where-Object {$_.Length -gt 30}).Count -eq 0)
Registrar 'operacao.tipos_cabem_32' (@($contrato.auditoria.tipoOperacaoAdministrativa|Where-Object{$_.Length -gt 32}).Count -eq 0)
Registrar 'externo.SUPERADA_EMITIDO_sem_acoplamento_de_estado' ((Corpo-Check $sql 'ck_versao_fechamento_externo') -match "'EMITIDO'" -and (Corpo-Check $sql 'ck_versao_fechamento_situacao') -match "'SUPERADA'" -and $sqlSemComentarios -notmatch "situacao\s*=\s*'EMITIDA'\s+AND\s+estado_externo|estado_externo\s*=\s*'EMITIDO'\s+AND\s+situacao")
Registrar 'ajuste.FK_aplicado_opcional' ($sqlSemComentarios -match 'aplicado_versao_id bigint NULL[^\r\n]*REFERENCES wms\.versao_fechamento\(id\)')
Registrar 'ajuste.VALIDADO_sem_aplicacao_APLICADO_com_par' ((Normalizar (Corpo-Check $sql 'ck_ajuste_fechamento_aplicacao')) -ceq (Normalizar $contrato.tabelas[4].checks.ck_ajuste_fechamento_aplicacao))
Registrar 'saldo_delta.assinados_sem_clamp' ($sqlSemComentarios -notmatch '\bsaldo\s*>=\s*0|\bdiferenca\s*>=\s*0')
Registrar 'doc31.TRATATIVA_EXTERNA_formal' ($quadroDoc31 -match '\bTRATATIVA_EXTERNA\b')
$blocoNFSe=($tabelasSql|Where-Object{$_.Groups['nome'].Value -ceq 'referencia_nfse'}).Groups['corpo'].Value
Registrar 'nfse.versao_indice_nao_unico_sem_UNIQUE' ($blocoNFSe -notmatch 'UNIQUE\s*\(\s*versao_id\s*\)' -and $sql -match 'CREATE INDEX ix_referencia_nfse_versao ON wms\.referencia_nfse\(versao_id,id\)')
Registrar 'nfse.identidade_externa_global_mantida' ($blocoNFSe -match 'UNIQUE\s*\(emissor_documento,referencia_externa\)')
$blocoTratativa=($tabelasSql|Where-Object{$_.Groups['nome'].Value -ceq 'tratativa_externa_fechamento'}).Groups['corpo'].Value
Registrar 'tratativa.historicos_multiplos_sem_UNIQUE' ($blocoTratativa -and $blocoTratativa -notmatch '\bUNIQUE\b')
Registrar 'tratativa.referencias_JSON_comprovacao_servico' ((Corpo-Check $sql 'ck_tratativa_externa_fechamento_json') -match 'ISJSON\(referencias_json\)\s*=\s*1')
Registrar 'alvo.guarda_placeholder' ($sql.Contains("IF DB_NAME() <> N'" + '$' + "{wmsDatabase}'"))
Registrar 'historico.sem_DML_cascata_grants_exec_trigger' ($sqlSemComentarios -notmatch '\b(?:INSERT|UPDATE|DELETE|MERGE|TRUNCATE|GRANT|DENY|EXEC(?:UTE)?|TRIGGER)\b|\bON\s+(?:DELETE|UPDATE)\s+CASCADE\b')
$defaultFormal = "CONSTRAINT df_ajuste_fechamento_tipo DEFAULT 'CORRECAO_CALCULO'"
$semDefaultFormal = $sqlSemComentarios -replace [regex]::Escape($defaultFormal),''
Registrar 'historico.DEFAULT_unico_formal_sem_outro_default' ([regex]::Matches($sqlSemComentarios,[regex]::Escape($defaultFormal)).Count -eq 1 -and $semDefaultFormal -notmatch '\bDEFAULT\b' -and @($contrato.tabelas.colunas|Where-Object{$_.default}).Count -eq 1)
Registrar 'historico.sem_alterar_colunas_anteriores' ($sqlSemComentarios -notmatch '\bALTER COLUMN\b|\bDROP TABLE\b')
$verificacoesBaseContratoSql = $checks.Count
$operacoesAdministrativas = [ordered]@{solicitado=[bool]$CompararJpa;literaisSalvos=@();fontes=@();dominioSql=@();limite='Literais diretos dos services que injetam OperacaoAdministrativaService; nao executa Java/SQL'}
$auditoriasBE13 = [ordered]@{solicitado=[bool]$CompararJpa;pares=@();fontes=@();limite='Literais diretos dos services que referem models BE13 ou tipo FECHAMENTO_COBRANCA; nao interpreta fluxo Java'}
$jpa = [ordered]@{solicitado=[bool]$CompararJpa;situacao='NAO_CONFERIDO_AGUARDA_FREEZE';models=@();fontesEmbeddable=@();fontesHerdadas=@();nomesUnicas=@();indicesFiltrados=@();colunasConferidas=0;divergencias=@()}
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
    $alvosJpa += @($contrato.ampliacoesChecks | ForEach-Object {[pscustomobject]@{nome=$_.tabela;colunas=@([pscustomobject]@{nome=$_.coluna;tipo=$_.tipo;nulo=$_.nulo});unicas=@();extensao=$true;enumJava=$_.enumJava}})
    $arquivosEnumCadastro = @(Get-ChildItem -LiteralPath (Join-Path $raizModels '..') -Filter 'SituacaoCadastro.java' -File -Recurse)
    if ($arquivosEnumCadastro.Count -ne 1) {$jpa.divergencias += 'cadastro.enum_SituacaoCadastro_fonte_unica'}
    else {
        $fonteEnum = Get-Content -LiteralPath $arquivosEnumCadastro[0].FullName -Raw -Encoding UTF8
        $semComentariosEnum = ($fonteEnum -replace '(?s)/\*.*?\*/','') -replace '(?m)//.*$',''
        $corpoEnum = [regex]::Match($semComentariosEnum,'(?s)enum\s+SituacaoCadastro\s*\{(.*?)(?:;|\})').Groups[1].Value
        $valoresEnum = @([regex]::Matches($corpoEnum,'\b[A-Z][A-Z_]+\b')|ForEach-Object{$_.Value})
        if (-not (Mesmo-Conjunto @('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO') $valoresEnum)) {$jpa.divergencias += 'cadastro.enum_SituacaoCadastro_dominio'}
        $jpa.fontesEnumCadastro = @([pscustomobject]@{arquivo=$arquivosEnumCadastro[0].FullName.Substring($raizProjeto.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $arquivosEnumCadastro[0].FullName -Algorithm SHA256).Hash;valores=$valoresEnum})
    }
    foreach ($alvo in $alvosJpa) {
        $model = $mapaModels[$alvo.nome]
        if (-not $model) {$jpa.divergencias += $alvo.nome + '.model_ausente';continue}
        $campos = @{}
        $fonteCampos = $model.fonte
        $classeBase = [regex]::Match($model.fonte,'class\s+\w+\s+extends\s+(\w+)').Groups[1].Value
        if ($classeBase) {
            $arquivoBase = Join-Path $raizModels ($classeBase + '.java')
            $fonteBase = Get-Content -LiteralPath $arquivoBase -Raw -Encoding UTF8
            if ($fonteBase -notmatch '@MappedSuperclass\b' -or $fonteBase -match 'class\s+\w+\s+extends\s+') {$jpa.divergencias += $alvo.nome + '.heranca_nao_suportada'}
            else {
                $fonteCampos += [Environment]::NewLine + $fonteBase
                $jpa.fontesHerdadas += [pscustomobject]@{tabela=$alvo.nome;arquivo=$arquivoBase.Substring($raizProjeto.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $arquivoBase -Algorithm SHA256).Hash}
            }
        }
        if ($model.fonte -match '@AttributeOverrides?\b') {$jpa.divergencias += $alvo.nome + '.override_nao_suportado_pelo_leitor'}
        foreach ($embutido in [regex]::Matches($model.fonte,'@Embedded\s+private\s+(?<java>\w+)\s+\w+\s*;')) {
            $arquivoEmbutido = Join-Path $raizModels ($embutido.Groups['java'].Value + '.java')
            $fonteEmbutida = Get-Content -LiteralPath $arquivoEmbutido -Raw -Encoding UTF8
            if ($fonteEmbutida -notmatch '@Embeddable\b' -or $fonteEmbutida -match '@Embedded\b|@AttributeOverrides?\b') {$jpa.divergencias += $alvo.nome + '.embeddable_nao_suportado_pelo_leitor';continue}
            $fonteCampos += [Environment]::NewLine + $fonteEmbutida
            $jpa.fontesEmbeddable += [pscustomobject]@{tabela=$alvo.nome;arquivo=$arquivoEmbutido.Substring($raizProjeto.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $arquivoEmbutido -Algorithm SHA256).Hash}
        }
        foreach ($campo in [regex]::Matches($fonteCampos,'(?<attrs>(?:@\w+(?:\((?:[^()"]|"[^"]*")*\))?\s*)+)\s*private\s+(?<java>\w+)\s+(?<field>\w+)\s*(?:=\s*[^;]+)?;')) {
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
            $campos[$nome] = [pscustomobject]@{tipo=$tipo;nulo=$nulo;referencia=$referencia;attrs=$attrs;java=$java}
        }
        if (-not $alvo.extensao -and -not (Mesmo-Conjunto @($alvo.colunas | ForEach-Object {$_.nome}) @($campos.Keys))) {$jpa.divergencias += $alvo.nome + '.colunas_diferentes'}
        foreach ($coluna in $alvo.colunas) {
            $campo=$campos[$coluna.nome]
            if (-not $campo -or $campo.tipo -cne $coluna.tipo -or $campo.nulo -ne $coluna.nulo -or $campo.referencia -cne $coluna.referencia) {$jpa.divergencias += $alvo.nome + '.' + $coluna.nome + '.tipo_nulo_FK'}
            if ($alvo.enumJava -and ($campo.java -cne $alvo.enumJava -or $campo.attrs -notmatch '@Enumerated\s*\(\s*EnumType\.STRING\s*\)')) {$jpa.divergencias += $alvo.nome + '.situacao.enum_STRING'}
            if ($coluna.identity -and $campo.attrs -notmatch 'GenerationType\.IDENTITY') {$jpa.divergencias += $alvo.nome + '.' + $coluna.nome + '.identity'}
            $jpa.colunasConferidas++
            if($coluna.nome -ceq 'versao' -and $campo.attrs -notmatch '@Version\b'){$jpa.divergencias += $alvo.nome + '.versao.anotacao_Version'}
            $esquemaAlvo=$contrato.tabelas|Where-Object{$_.nome -ceq $alvo.nome}
            if($esquemaAlvo.mutaveis -ccontains $coluna.nome -and $campo.attrs -match 'updatable\s*=\s*false'){$jpa.divergencias += $alvo.nome + '.'+$coluna.nome+'.campo_mutavel_mapeado_imutavel'}
            if($esquemaAlvo.imutaveisMarcador -ccontains $coluna.nome -and $campo.attrs -notmatch 'updatable\s*=\s*false'){$jpa.divergencias += $alvo.nome + '.'+$coluna.nome+'.snapshot_updatable_default'}

        }
        if (-not $alvo.extensao) {
            $unicasJpa = @([regex]::Matches($model.fonte,'@UniqueConstraint\(\s*name\s*=\s*"(?<nome>[^"]+)"\s*,\s*columnNames\s*=\s*(?:"(?<simples>[^"]+)"|\{(?<composta>[^}]+)\})\s*\)'))
            if ($unicasJpa.Count -ne @($alvo.unicas).Count) {$jpa.divergencias += $alvo.nome + '.quantidade_unicas'}
            foreach ($unica in $alvo.unicas) {
                $nomeJpa = if ($unica.nomeJpa) {$unica.nomeJpa} else {$unica.nome}
                $observada = $unicasJpa | Where-Object {$_.Groups['nome'].Value -ceq $nomeJpa}
                if (-not $observada) {$jpa.divergencias += $alvo.nome + '.' + $unica.nome + '.ausente';continue}
                $nomes = if ($observada.Groups['simples'].Success) {$observada.Groups['simples'].Value} else {([regex]::Matches($observada.Groups['composta'].Value,'"([^"]+)"') | ForEach-Object {$_.Groups[1].Value}) -join ','}
                if ($nomes -cne ($unica.colunas -join ',')) {$jpa.divergencias += $alvo.nome + '.' + $unica.nome + '.chave_unica'}
                $jpa.nomesUnicas += [pscustomobject]@{tabela=$alvo.nome;nomeSql=$unica.nome;nomeJpa=$nomeJpa;colunas=$unica.colunas;mesmaChave=($nomes -ceq ($unica.colunas -join ','));limite='Nomes locais explicitamente distintos; chave comparada integralmente, SQL preservado'}
            }
        }
        foreach ($indiceFiltrado in @($contrato.indicesFiltrados | Where-Object {$_.tabela -ceq $alvo.nome})) {
            $indicesJpa = @([regex]::Matches($model.fonte,'@(?:jakarta\.persistence\.)?Index\s*\(\s*name\s*=\s*"(?<nome>[^"]+)"\s*,\s*columnList\s*=\s*"(?<colunas>[^"]+)"\s*,\s*unique\s*=\s*true\s*\)'))
            $indiceJpa = @($indicesJpa | Where-Object {$_.Groups['nome'].Value -ceq $indiceFiltrado.nomeJpa})
            $mesmaChave = $indiceJpa.Count -eq 1 -and (Normalizar $indiceJpa[0].Groups['colunas'].Value) -ceq (Normalizar ($indiceFiltrado.colunas -join ','))
            Registrar ('jpa.' + $alvo.nome + '.indice_nullable_unico_mesma_chave') $mesmaChave
            $jpa.indicesFiltrados += [pscustomobject]@{tabela=$alvo.nome;nomeSql=$indiceFiltrado.nome;nomeJpa=$indiceFiltrado.nomeJpa;colunas=$indiceFiltrado.colunas;mesmaChave=$mesmaChave;filtroSql=$indiceFiltrado.filtro;filtroExpressoNaAnotacao=$false;limite='Anotacao JPA unique=true nullable nao expressa predicado. V8 aplica filtro formal SQL Server; SQL emitido por Hibernate/semantica real NULL nao observados.'}
        }
        $jpa.models += [pscustomobject]@{tabela=$alvo.nome;arquivo=$model.arquivo.Substring($raizProjeto.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $model.arquivo -Algorithm SHA256).Hash}
    }
    $jpa.situacao = if ($jpa.divergencias.Count -eq 0) {'COMPATIVEL_EM_LEITURA'} else {'DIVERGENCIAS_EM_LEITURA'}
    Registrar 'jpa.sem_divergencias' ($jpa.divergencias.Count -eq 0)
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
            $repasse = if($arquivoService.BaseName -ceq 'FechamentoCobrancaService'){Repasse-Fechamento $fonteService}else{$null}
            if($repasse){Registrar 'repasse.concluir_fechar_cobertura_e_sem_transformar_tipo' ($repasse.encontrado -and $repasse.validado)}
            $forward = if($repasse -and $repasse.validado){$repasse.repasseOperacao}else{0}
            Registrar ('operacao_administrativa.'+$arquivoService.BaseName+'.cobertura_literais') ($salvas.Count+$forward -eq [regex]::Matches($fonteService,$metodo).Count)
            foreach ($salva in $salvas) {
                $tipoSalvo = $salva.Groups['tipo'].Value
                $linhaSalva = [regex]::Matches($fonteService.Substring(0,$salva.Index),'\n').Count+1
                $aceito = $operacoesAdministrativas.dominioSql -ccontains $tipoSalvo
                $operacoesAdministrativas.literaisSalvos += [pscustomobject]@{arquivo=$relService;linha=$linhaSalva;tipo=$tipoSalvo;aceitoNoCheck=$aceito}
                Registrar ('operacao_administrativa.'+$arquivoService.BaseName+'.'+$linhaSalva+'.'+$tipoSalvo) $aceito
            }
            if($repasse -and $repasse.validado){
                foreach($salva in $repasse.literais){
                    $aceito=$operacoesAdministrativas.dominioSql -ccontains $salva.tipo
                    $operacoesAdministrativas.literaisSalvos += [pscustomobject]@{arquivo=$relService;linha=$salva.linha;tipo=$salva.tipo;aceitoNoCheck=$aceito;via=$salva.via}
                    Registrar ('operacao_administrativa.'+$arquivoService.BaseName+'.'+$salva.linha+'.'+$salva.tipo) $aceito
                }
            }
        }
    }
    # Pares efetivamente gravados nas fontes BE13, somente depois do freeze informado.
    $nomesModelsBE13 = ($contrato.tabelas.model|ForEach-Object{[regex]::Escape($_)}) -join '|'
    foreach($arquivoService in Get-ChildItem -LiteralPath $raizServices -Filter '*.java' -File){
        $fonteService=Get-Content -LiteralPath $arquivoService.FullName -Raw -Encoding UTF8
        if($fonteService -notmatch ('\b(?:'+$nomesModelsBE13+')\b|FECHAMENTO_COBRANCA')){continue}
        $relService=$arquivoService.FullName.Substring($raizProjeto.Length+1).Replace('\','/')
        foreach($injecao in [regex]::Matches($fonteService,'private\s+final\s+\w*Auditoria\w*Service\s+(?<variavel>\w+)\s*;')){
            $metodo=[regex]::Escape($injecao.Groups['variavel'].Value)+'\.registrar\s*\('
            $pares=@([regex]::Matches($fonteService,($metodo+'\s*"(?<tipo>[A-Z_]+)"\s*,\s*[^,]+,\s*"(?<acao>[A-Z_]+)"')))
            $repasse=if($arquivoService.BaseName -ceq 'FechamentoCobrancaService'){Repasse-Fechamento $fonteService}else{$null}
            $forward=if($repasse -and $repasse.validado){$repasse.repasseAuditoria}else{0}
            Registrar ('auditoriaBE13.'+$arquivoService.BaseName+'.cobertura_literais') ($pares.Count+$forward -eq [regex]::Matches($fonteService,$metodo).Count)
            foreach($par in $pares){
                $tipo=$par.Groups['tipo'].Value;$acao=$par.Groups['acao'].Value
                $ok=($contrato.auditoria.tiposAnteriores+$contrato.auditoria.tiposNovos) -ccontains $tipo
                $ok=$ok -and (($contrato.auditoria.acoesAnteriores+$contrato.auditoria.acoesNovas) -ccontains $acao)
                if($contrato.auditoria.tiposNovos -ccontains $tipo){$ok=$ok -and ($contrato.auditoria.paresTiposNovos.$tipo -ccontains $acao)}
                if($contrato.auditoria.acoesNovas -ccontains $acao){$ok=$ok -and ($contrato.auditoria.paresNovos.$acao -ccontains $tipo)}
                $linha=[regex]::Matches($fonteService.Substring(0,$par.Index),'\n').Count+1
                $auditoriasBE13.pares += [pscustomobject]@{arquivo=$relService;linha=$linha;tipo=$tipo;acao=$acao;compativel=$ok}
                Registrar ('auditoriaBE13.'+$arquivoService.BaseName+'.'+$linha+'.'+$tipo+'/'+$acao) $ok
            }
            if($repasse -and $repasse.validado){
                foreach($par in $repasse.literais){
                    $tipo='FECHAMENTO_COBRANCA';$acao=$par.tipo
                    $ok=($contrato.auditoria.paresTiposNovos.$tipo -ccontains $acao) -and ($contrato.auditoria.paresNovos.$acao -ccontains $tipo)
                    $auditoriasBE13.pares += [pscustomobject]@{arquivo=$relService;linha=$par.linha;tipo=$tipo;acao=$acao;compativel=$ok;via=$par.via}
                    Registrar ('auditoriaBE13.'+$arquivoService.BaseName+'.'+$par.linha+'.'+$tipo+'/'+$acao) $ok
                }
            }
        }
        $auditoriasBE13.fontes += [pscustomobject]@{arquivo=$relService;sha256=(Get-FileHash -LiteralPath $arquivoService.FullName -Algorithm SHA256).Hash}
        Registrar ('auditoriaBE13.'+$arquivoService.BaseName+'.sem_novo_movimento_fisico') ($fonteService -notmatch 'new\s+MovimentoEstoque\s*\(|movimentos\.save\s*\(')
    }
    Registrar 'auditoriaBE13.fontes_com_pares_encontradas' ($auditoriasBE13.pares.Count -gt 0)
    Registrar 'auditoriaBE13.dez_pares_formais_cobertos' (Mesmo-Conjunto @($contrato.auditoria.acoesNovas) @($auditoriasBE13.pares.acao|Sort-Object -Unique))
    $arquivoGateway = Join-Path $raizServices 'OperacaoAdministrativaService.java'
    $fonteGateway = Get-Content -LiteralPath $arquivoGateway -Raw -Encoding UTF8
    $operacoesAdministrativas.fontes += [pscustomobject]@{arquivo='backend/src/main/java/br/com/rodogarcia/wms/services/OperacaoAdministrativaService.java';sha256=(Get-FileHash -LiteralPath $arquivoGateway -Algorithm SHA256).Hash}
    Registrar 'operacao_administrativa.tipo_encaminhado_sem_transformacao' ($fonteGateway -match 'new OperacaoAdministrativa\(\s*id.toString\(\),\s*tipo,')
    Registrar 'operacao_administrativa.construtor_central_unico' ($construtoresOperacao -eq 1 -and [regex]::Matches($fonteGateway,'new\s+OperacaoAdministrativa\s*\(').Count -eq 1)
}
$falhas = @($checks.Keys | Where-Object {-not $checks[$_]})
[pscustomobject][ordered]@{
    natureza='Comparacao estatica V8/schema31 transcrito e SQL; nenhuma conexao, JPA, SQL, H2 ou build'
    capturadoEmUtc=[DateTime]::UtcNow.ToString('o')
    fonteDocumento=[pscustomobject]@{arquivoLido=$fonteAtual.Substring($raizProjeto.Length+1).Replace('\','/');copiaPreservada=[bool]$FonteDoc31;origemCanonica=$contrato.fonte;hashOrigemCanonicaNaCaptura=(Get-FileHash -LiteralPath $fonteCanonica -Algorithm SHA256).Hash;limite='Copia deve ser literal/hash esperado; comparacao de snapshot nao garante estabilidade posterior da fonte viva.'}
    tabelas=$contrato.tabelas.Count;colunas=$totalColunas;extensoes=$totalExtensoes;ampliacoesChecksCadastro=@($contrato.ampliacoesChecks).Count;fks=$totalFk;unicas=$totalUnicas;checksSql=$totalChecks;indicesExplicitos=$contrato.indices.Count;indicesUnicosFiltrados=@($contrato.indicesFiltrados).Count;fksPosteriores=@($contrato.tabelas.colunas | Where-Object {$_.fkPosterior}).Count
    verificacoes=$checks.Count;atendidas=$checks.Count-$falhas.Count;verificacoesBaseContratoSql=$verificacoesBaseContratoSql;verificacoesAdicionaisOperacao=$checks.Count-$verificacoesBaseContratoSql
    auditoria=[pscustomobject]@{tiposAnteriores=$contrato.auditoria.tiposAnteriores.Count;tiposNovos=$contrato.auditoria.tiposNovos.Count;acoesAnteriores=$contrato.auditoria.acoesAnteriores.Count;acoesNovas=$contrato.auditoria.acoesNovas.Count;movimentoIntocado=$checks['movimento_estoque.sem_alteracao']}
    resumo=$resumo;checks=$checks;divergencias=@($divergencias);jpa=[pscustomobject]$jpa;operacoesAdministrativas=[pscustomobject]$operacoesAdministrativas;auditoriasBE13=[pscustomobject]$auditoriasBE13
    hashes=[pscustomobject]@{sql=(Get-FileHash -LiteralPath $arquivoSql -Algorithm SHA256).Hash;transcricao=(Get-FileHash -LiteralPath $arquivoContrato -Algorithm SHA256).Hash;doc31=(Get-FileHash -LiteralPath $fonteAtual -Algorithm SHA256).Hash;leitor=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash;preservadosV1V7=$hashesPreservados}
} | ConvertTo-Json -Depth 8
if ($divergencias.Count -gt 0) {exit 1}
