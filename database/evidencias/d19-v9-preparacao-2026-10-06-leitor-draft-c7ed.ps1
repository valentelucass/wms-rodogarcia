# Comparador estatico BE14 anterior ao Java. Somente le arquivos; nenhum SQL/JPA/JVM.
[CmdletBinding()]
param([string]$FonteDoc31)
$ErrorActionPreference = 'Stop'
$raiz = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$contratoPath = Join-Path $raiz 'database/contratos/v9-schema-doc31.json'
$c = Get-Content -LiteralPath $contratoPath -Raw -Encoding UTF8 | ConvertFrom-Json
$sqlPath = Join-Path $raiz $c.migration
$sql = Get-Content -LiteralPath $sqlPath -Raw -Encoding UTF8
$limpo = $sql -replace '(?m)--.*$', ''
$checks = [ordered]@{}
$divergencias = @()
function Registrar([string]$nome,[bool]$ok) {
    $script:checks[$nome] = $ok
    if (-not $ok) {$script:divergencias += $nome}
}
function Normalizar([string]$s) {(($s -replace '(?m)--.*$','') -replace '\s','').ToUpperInvariant()}
function MesmoConjunto([object[]]$a,[object[]]$b) {
    if ($a.Count -ne $b.Count) {return $false}
    if ($a.Count -eq 0) {return $true}
    return @(Compare-Object $a $b).Count -eq 0
}
function Literais([string]$s) {@([regex]::Matches($s,"'([^']+)'")|ForEach-Object {$_.Groups[1].Value})}
function CorpoCheck([string]$s,[string]$nome) {
    $m=[regex]::Match($s,'CONSTRAINT\s+'+[regex]::Escape($nome)+'\s+CHECK\s*\(')
    if (-not $m.Success) {return ''}
    $inicio=$m.Index+$m.Length;$cursor=$inicio;$nivel=1;$literal=$false
    while ($cursor -lt $s.Length -and $nivel -gt 0) {
        $ch=$s[$cursor]
        if ($ch -eq "'") {$literal=-not $literal}
        elseif (-not $literal) {
            if ($ch -eq '(') {$nivel++}
            elseif ($ch -eq ')') {$nivel--}
        }
        $cursor++
    }
    if ($nivel -ne 0) {return ''}
    $s.Substring($inicio,$cursor-$inicio-1)
}
function Colunas([string]$s) {
    @([regex]::Matches($s,'(?m)^\s*(?<nome>[a-z]\w*)\s+(?<tipo>bigint|int|bit|datetime2\(\d+\)|decimal\(\d+,\d+\)|n?varchar\((?:\d+|max)\))(?<identity>\s+IDENTITY\(1,1\))?\s+(?<nulo>NOT NULL|NULL)\b'))
}
# Parser limitado ao subconjunto textual dos CHECKs de pares: AND/OR, =/<>, IN/NOT IN.
# A arvore e avaliada sobre literais de tipo/acao; nao executa SQL ou codigo extraido.
function AnalisarPar([string]$s) {
    $padrao="'[^']*'|<>|=|\(|\)|,|[A-Za-z_][A-Za-z_0-9]*"
    $script:tokens=@([regex]::Matches($s,$padrao)|ForEach-Object {$_.Value})
    if ((Normalizar ($script:tokens -join '')) -cne (Normalizar $s)) {throw 'Token nao suportado no CHECK de pares.'}
    $script:pos=0
    $arvore=LerOr
    if ($script:pos -ne $script:tokens.Count) {throw 'CHECK de pares com tokens restantes.'}
    return $arvore
}
function Exigir([string]$valor) {
    if ($script:pos -ge $script:tokens.Count -or $script:tokens[$script:pos] -ine $valor) {throw ('Esperado token '+$valor)}
    $script:pos++
}
function LerOr {
    $a=LerAnd
    while ($script:pos -lt $script:tokens.Count -and $script:tokens[$script:pos] -ieq 'OR') {
        $script:pos++;$b=LerAnd;$a=[pscustomobject]@{kind='OR';a=$a;b=$b}
    }
    return $a
}
function LerAnd {
    $a=LerAtom
    while ($script:pos -lt $script:tokens.Count -and $script:tokens[$script:pos] -ieq 'AND') {
        $script:pos++;$b=LerAtom;$a=[pscustomobject]@{kind='AND';a=$a;b=$b}
    }
    return $a
}
function LerAtom {
    if ($script:tokens[$script:pos] -eq '(') {$script:pos++;$a=LerOr;Exigir ')';return $a}
    $campo=$script:tokens[$script:pos].ToLowerInvariant();$script:pos++
    if ($campo -notin @('tipo','acao')) {throw 'Campo nao suportado em pares.'}
    $op=$script:tokens[$script:pos].ToUpperInvariant();$script:pos++
    if ($op -eq 'NOT') {Exigir 'IN';$op='NOT IN'}
    $valores=@()
    if ($op -in @('IN','NOT IN')) {
        Exigir '('
        do {
            $valor=$script:tokens[$script:pos];$script:pos++
            if ($valor -notmatch "^'[^']*'$") {throw 'Literal ausente no CHECK.'}
            $valores+=$valor.Trim("'")
            if ($script:tokens[$script:pos] -eq ',') {$script:pos++} else {break}
        } while ($true)
        Exigir ')'
    } elseif ($op -in @('=','<>')) {
        $valor=$script:tokens[$script:pos];$script:pos++
        if ($valor -notmatch "^'[^']*'$") {throw 'Literal ausente no CHECK.'}
        $valores+=$valor.Trim("'")
    } else {throw 'Operador nao suportado em pares.'}
    [pscustomobject]@{kind='ATOM';campo=$campo;op=$op;valores=$valores}
}
function AvaliarPar($a,[string]$tipo,[string]$acao) {
    if ($a.kind -eq 'AND') {return (AvaliarPar $a.a $tipo $acao) -and (AvaliarPar $a.b $tipo $acao)}
    if ($a.kind -eq 'OR') {return (AvaliarPar $a.a $tipo $acao) -or (AvaliarPar $a.b $tipo $acao)}
    $valor=if ($a.campo -eq 'tipo') {$tipo} else {$acao}
    if ($a.op -in @('IN','=')) {return $valor -cin $a.valores}
    return $valor -cnotin $a.valores
}
$fonteCanonica=Join-Path $raiz $c.fonte
$fonte=$fonteCanonica
if ($FonteDoc31) {
    $fonte=[IO.Path]::GetFullPath((Join-Path $raiz $FonteDoc31))
    $area=[IO.Path]::GetFullPath((Join-Path $raiz 'database/evidencias'))+[IO.Path]::DirectorySeparatorChar
    if (-not $fonte.StartsWith($area,[StringComparison]::OrdinalIgnoreCase) -or [IO.Path]::GetExtension($fonte) -cne '.md') {throw 'FonteDoc31 deve ser copia MD na area database/evidencias.'}
}
$hashFonte=(Get-FileHash -LiteralPath $fonte -Algorithm SHA256).Hash
$doc=Get-Content -LiteralPath $fonte -Raw -Encoding UTF8
Registrar 'fonte.SHA_exato_da_transcricao' ($hashFonte -ceq $c.fonteSha256)
Registrar 'fonte.estavel_ao_ler' ((Get-FileHash -LiteralPath $fonte -Algorithm SHA256).Hash -ceq $hashFonte)
$quadro=[regex]::Match($doc,'(?s)## Schema/API BE14 V9.*?(?=### Contagem:)').Value
$linhas=@([regex]::Matches($quadro,'(?m)^\| (?<nome>[a-z]\w*) \| (?<texto>[^\r\n]+) \|'))
Registrar 'fonte.oito_tabelas_exatas' (MesmoConjunto @($c.tabelas.nome) @($linhas|ForEach-Object {$_.Groups['nome'].Value}))
Registrar 'fonte.tipos_comuns_e_NN' ($quadro -match 'BIGINT IDENTITY' -and $quadro -match 'DATETIME2\(6\)' -and $quadro -match 'DECIMAL\(19,6\)' -and $quadro -match 'NVARCHAR\(MAX\)' -and $quadro -match 'ISJSON' -and $quadro -match 'Campos n[^.]+NULL[^.]+obrigat')
$fontes=@{};$hashes=@()
foreach ($b in $c.baselineV1V8) {
    $p=Join-Path $raiz $b.arquivo;$h=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash
    Registrar ('preservacao.'+$b.arquivo) ($h -ceq $b.sha256)
    $fontes[$b.arquivo]=Get-Content -LiteralPath $p -Raw -Encoding UTF8
    $hashes+=[pscustomobject]@{arquivo=$b.arquivo;sha256=$h}
}
Registrar 'baseline.oito_migrations' ($hashes.Count -eq 8)
$tabelas=@([regex]::Matches($limpo,'(?s)CREATE TABLE wms\.(?<nome>\w+)\s*\((?<corpo>.*?)\r?\n\);'))
Registrar 'DDL.oito_tabelas_exatas' (MesmoConjunto @($c.tabelas.nome) @($tabelas|ForEach-Object {$_.Groups['nome'].Value}))
$existentes=@([regex]::Matches((@($fontes.Values)-join "`n")+$limpo,'CREATE TABLE wms\.(\w+)')|ForEach-Object {$_.Groups[1].Value})
$resumo=@();$colsTotal=0;$fkTotal=0;$ukTotal=0;$checkTotal=0
foreach ($t in $c.tabelas) {
    $nome=$t.nome;$bloco=($tabelas|Where-Object {$_.Groups['nome'].Value -ceq $nome}).Groups['corpo'].Value
    $formal=($linhas|Where-Object {$_.Groups['nome'].Value -ceq $nome}).Groups['texto'].Value
    $cols=@(Colunas $bloco)
    Registrar "$nome.colunas_exatas" (MesmoConjunto @($t.colunas.nome) @($cols|ForEach-Object {$_.Groups['nome'].Value}))
    foreach ($col in $t.colunas) {
        $observada=@($cols|Where-Object {$_.Groups['nome'].Value -ceq $col.nome})
        $nulo=if ($col.nulo) {'NULL'} else {'NOT NULL'}
        Registrar "$nome.$($col.nome).tipo_nulo_identity" ($observada.Count -eq 1 -and $observada[0].Groups['tipo'].Value -ceq $col.tipo -and $observada[0].Groups['nulo'].Value -ceq $nulo -and $observada[0].Groups['identity'].Success -eq [bool]$col.identity)
        if ($col.nome -ne 'id') {Registrar "$nome.$($col.nome).nome_formal" ($formal -match ('\b'+[regex]::Escape($col.nome)+'\b'))}
        if ($col.referencia) {
            Registrar "$nome.$($col.nome).FK_formal" ($formal -match ([regex]::Escape($col.nome)+' FK '+[regex]::Escape($col.referencia)+'\b'))
            Registrar "$nome.$($col.nome).FK_exata" ($bloco -match ('(?m)^\s*'+[regex]::Escape($col.nome)+'\s+bigint\s+(NOT NULL|NULL)\s+CONSTRAINT \w+ REFERENCES wms\.'+[regex]::Escape($col.referencia)+'\(id\)'))
            Registrar "$nome.$($col.nome).alvo_existente" ($col.referencia -cin $existentes)
            $prefixo=[regex]::Escape($col.nome)
            $indexado=$bloco -match ('UNIQUE\s*\(\s*'+$prefixo+'\s*[,)]') -or $limpo -match ('CREATE (?:UNIQUE )?INDEX \w+ ON wms\.'+$nome+'\(\s*'+$prefixo+'\s*[,)]')
            Registrar "$nome.$($col.nome).FK_indexada" $indexado
        }
        if ($col.nulo) {Registrar "$nome.$($col.nome).NULL_formal" ($formal -match ([regex]::Escape($col.nome)+'[^;]*\bNULL\b'))}
        if ($col.tipo -match '^n?varchar\((\d+)\)$') {
            $limite=[int]$Matches[1];$dominio=@()
            foreach ($m in [regex]::Matches($bloco,'\b'+[regex]::Escape($col.nome)+'\s+IN\s*\(([^)]*)\)')) {$dominio+=@(Literais $m.Groups[1].Value)}
            if ($dominio.Count -gt 0) {
                Registrar "$nome.$($col.nome).enum_cabe" (@($dominio|Where-Object {$_.Length -gt $limite}).Count -eq 0)
                Registrar "$nome.$($col.nome).enum_formal" (@($dominio|Where-Object {$formal -cnotmatch ('\b'+[regex]::Escape($_)+'\b')}).Count -eq 0)
            }
        }
    }
    foreach ($u in $t.unicas) {
        $real=[regex]::Match($bloco,'CONSTRAINT '+[regex]::Escape($u.nome)+' UNIQUE\s*\(([^)]+)\)').Groups[1].Value
        Registrar "$nome.$($u.nome)" ((Normalizar $real) -ceq (Normalizar ($u.colunas -join ',')))
    }
    Registrar "$nome.UNIQUE_quantidade" ([regex]::Matches($bloco,'CONSTRAINT \w+ UNIQUE\s*\(').Count -eq @($t.unicas).Count)
    foreach ($k in $t.checks.PSObject.Properties) {Registrar "$nome.$($k.Name)" ((Normalizar (CorpoCheck $bloco $k.Name)) -ceq (Normalizar $k.Value))}
    Registrar "$nome.CHECK_quantidade" ([regex]::Matches($bloco,'CONSTRAINT \w+ CHECK\s*\(').Count -eq @($t.checks.PSObject.Properties).Count)
    Registrar "$nome.PK_identity" ($bloco -match ('id bigint IDENTITY\(1,1\) NOT NULL CONSTRAINT pk_'+$nome+' PRIMARY KEY'))
    $fks=@($t.colunas|Where-Object {$_.referencia}).Count
    $resumo+=[pscustomobject]@{tabela=$nome;colunas=$t.colunas.Count;fks=$fks;unicas=@($t.unicas).Count;checks=@($t.checks.PSObject.Properties).Count}
    $colsTotal+=$t.colunas.Count;$fkTotal+=$fks;$ukTotal+=@($t.unicas).Count;$checkTotal+=@($t.checks.PSObject.Properties).Count
}
Registrar 'DDL.17_FKs_exatas' ([regex]::Matches($limpo,'REFERENCES wms\.').Count -eq $fkTotal -and $fkTotal -eq 17)
foreach ($i in $c.indices) {Registrar ('indice.'+$i.nome) ($limpo -match ('CREATE INDEX '+[regex]::Escape($i.nome)+' ON wms\.'+$i.tabela+'\('+[regex]::Escape(($i.colunas -join ','))+'\);'))}
foreach ($i in $c.indicesFiltrados) {
    Registrar ('indice_filtrado.'+$i.nome) ($limpo -match ('CREATE UNIQUE INDEX '+[regex]::Escape($i.nome)+' ON wms\.'+$i.tabela+'\('+[regex]::Escape(($i.colunas -join ','))+'\) WHERE '+[regex]::Escape($i.predicado)+';'))
}
Registrar 'indices.nove_comuns_um_filtrado' ([regex]::Matches($limpo,'CREATE INDEX ').Count -eq 9 -and [regex]::Matches($limpo,'CREATE UNIQUE INDEX ').Count -eq 1)
Registrar 'carga.entrada_nullable_sem_UNIQUE_comum' ($limpo -notmatch 'UNIQUE\s*\(entrada_id\)' -and $quadro -match 'entrada_id FK entrada_conferida NULL UNIQUE filtrada')
foreach ($on in @('ANSI_NULLS','QUOTED_IDENTIFIER','ANSI_PADDING','ANSI_WARNINGS','CONCAT_NULL_YIELDS_NULL','ARITHABORT')) {Registrar ('sessao.'+$on) ($limpo -match ('SET '+$on+' ON;'))}
Registrar 'sessao.NUMERIC_ROUNDABORT_OFF' ($limpo -match 'SET NUMERIC_ROUNDABORT OFF;')
$audit=$c.auditoria
$dominios=@(
    [pscustomobject]@{tabela='operacao_administrativa';nome='ck_operacao_administrativa_tipo';campo='tipo';antigos=@($audit.tiposOperacaoAnteriores);novos=@($audit.tiposOperacaoNovos);origem='database/migrations/V8__fechamento_e_versoes.sql';limite=32},
    [pscustomobject]@{tabela='auditoria_cadastro';nome='ck_auditoria_tipo';campo='tipo';antigos=@($audit.tiposAnteriores);novos=@($audit.tiposNovos);origem='database/migrations/V8__fechamento_e_versoes.sql';limite=24},
    [pscustomobject]@{tabela='auditoria_cadastro';nome='ck_auditoria_acao';campo='acao';antigos=@($audit.acoesAnteriores);novos=@($audit.acoesNovas);origem='database/migrations/V8__fechamento_e_versoes.sql';limite=32},
    [pscustomobject]@{tabela='movimento_estoque';nome='ck_movimento_acao';campo='acao';antigos=@($audit.movimentosAnteriores);novos=@($audit.movimentosNovos);origem='database/migrations/V6__separacao_retirada_retornos_e_avaria.sql';limite=32}
)
foreach ($d in $dominios) {
    $esperado=@($d.antigos)+@($d.novos);$corpo=CorpoCheck $limpo $d.nome
    Registrar ($d.nome+'.anteriores_reais') (MesmoConjunto $d.antigos @(Literais (CorpoCheck $fontes[$d.origem] $d.nome)))
    Registrar ($d.nome+'.cumulativo_exato') (MesmoConjunto $esperado @(Literais $corpo))
    $exp=$d.campo+' IN ('+(($esperado|ForEach-Object {"'"+$_+"'"}) -join ',')+')'
    Registrar ($d.nome+'.expressao_sem_bypass') ((Normalizar $corpo) -ceq (Normalizar $exp))
    Registrar ($d.nome+'.DROP_ADD_WITH_CHECK') ([regex]::Matches($limpo,'ALTER TABLE wms\.'+$d.tabela+' DROP CONSTRAINT '+$d.nome+';').Count -eq 1 -and [regex]::Matches($limpo,'ALTER TABLE wms\.'+$d.tabela+' WITH CHECK ADD CONSTRAINT '+$d.nome+' CHECK').Count -eq 1)
    Registrar ($d.nome+'.tamanho_cumulativo') (@($esperado|Where-Object {$_.Length -gt $d.limite}).Count -eq 0)
}
$financeiraAntes=CorpoCheck $fontes['database/migrations/V7__cadastros_servicos_e_calculo.sql'] 'ck_auditoria_financeira_tipo_acao'
$financeira=CorpoCheck $limpo 'ck_auditoria_financeira_tipo_acao'
Registrar 'financeiro.anterior_exato' ((Normalizar $financeiraAntes) -ceq (Normalizar $audit.financeiroAnterior))
Registrar 'financeiro.somente_SERVICO_INATIVACAO' ((Normalizar $financeira) -ceq (Normalizar ($financeiraAntes.Replace("tipo = 'SERVICO_COBRANCA' AND acao IN ('CRIACAO')","tipo = 'SERVICO_COBRANCA' AND acao IN ('CRIACAO','INATIVACAO_DEFINITIVA')"))))
Registrar 'financeiro.DROP_ADD_WITH_CHECK' ($limpo -match 'ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_financeira_tipo_acao;' -and $limpo -match 'WITH CHECK ADD CONSTRAINT ck_auditoria_financeira_tipo_acao')
Registrar 'pares.BE14_expressao_exata' ((Normalizar (CorpoCheck $limpo 'ck_auditoria_be14_tipo_acao')) -ceq (Normalizar $audit.checkPares))
foreach ($nome in $audit.restricoesMantidas) {Registrar ('pares.preservado.'+$nome) ($limpo -notmatch ('ALTER TABLE[^;]*\b'+[regex]::Escape($nome)+'\b'))}
$saida=AnalisarPar (CorpoCheck $fontes['database/migrations/V6__separacao_retirada_retornos_e_avaria.sql'] 'ck_auditoria_saida_tipo_acao')
$finAnt=AnalisarPar $financeiraAntes;$finNovo=AnalisarPar $financeira
$fechamento=AnalisarPar (CorpoCheck $fontes['database/migrations/V8__fechamento_e_versoes.sql'] 'ck_auditoria_fechamento_tipo_acao')
$be14=AnalisarPar (CorpoCheck $limpo 'ck_auditoria_be14_tipo_acao')
$perdas=@();$indevidos=@();$faltantes=@();$paresAntigos=0;$combinacoes=0
foreach ($tipo in @($audit.tiposAnteriores)+@($audit.tiposNovos)) {
    foreach ($acao in @($audit.acoesAnteriores)+@($audit.acoesNovas)) {
        $combinacoes++
        $antes=(AvaliarPar $saida $tipo $acao) -and (AvaliarPar $finAnt $tipo $acao) -and (AvaliarPar $fechamento $tipo $acao)
        $depois=(AvaliarPar $saida $tipo $acao) -and (AvaliarPar $finNovo $tipo $acao) -and (AvaliarPar $fechamento $tipo $acao) -and (AvaliarPar $be14 $tipo $acao)
        if ($acao -cin $audit.acoesNovas) {
            $permitido=$tipo -cin @($audit.paresNovos.$acao)
            if ($permitido -and -not $depois) {$faltantes+=($tipo+'/'+$acao)}
            if (-not $permitido -and $depois) {$indevidos+=($tipo+'/'+$acao)}
        } elseif ($tipo -cin $audit.tiposNovos) {
            if ($depois) {$indevidos+=($tipo+'/'+$acao)}
        } else {
            $paresAntigos++
            if ($antes -ne $depois) {$perdas+=($tipo+'/'+$acao)}
        }
    }
}
Registrar 'pares.todos_novos_validos_com_CHECKs_anteriores' ($faltantes.Count -eq 0)
Registrar 'pares.nenhum_par_novo_indevido' ($indevidos.Count -eq 0)
Registrar 'pares.todas_combinacoes_antigas_preservadas' ($perdas.Count -eq 0)
$pareamento=[regex]::Match($doc,'(?s)### Pareamento V9 de.*').Value
foreach ($acao in $audit.acoesNovas) {Registrar ('fonte.acao.'+$acao) ($pareamento -cmatch ('\b'+[regex]::Escape($acao)+'\b'))}
foreach ($tipo in $audit.tiposNovos) {Registrar ('fonte.tipo.'+$tipo) ($pareamento -cmatch ('\b'+[regex]::Escape($tipo)+'\b'))}
Registrar 'admin.AJUSTE_ESTOQUE_nao_presumido' ('AJUSTE_ESTOQUE' -cnotin @($audit.tiposOperacaoNovos) -and (CorpoCheck $limpo 'ck_operacao_administrativa_tipo') -cnotmatch "'AJUSTE_ESTOQUE'")
Registrar 'identidade.global_sem_contexto_UUID_acao' ($limpo -match 'UNIQUE \(identidade_fato\)' -and $quadro -match 'UNIQUE global' -and $quadro -match 'UUID, rota ou')
Registrar 'JSON.seis_campos_ISJSON_exatos' ([regex]::Matches($limpo,'ISJSON\(').Count -eq 6)
Registrar 'DDL.sem_DML_default_trigger_cascade_grant' ($limpo -notmatch '\b(INSERT|UPDATE|DELETE|MERGE|DEFAULT|TRIGGER|CASCADE|GRANT|DENY|REVOKE)\b')
Registrar 'DDL.sem_coluna_nova_em_tabela_anterior' ($limpo -notmatch 'ALTER TABLE\s+\S+\s+(?:ADD\s+(?!CONSTRAINT)|ALTER COLUMN|DROP COLUMN)')
Registrar 'DDL.alvo_placeholder_sem_nome_real' ($limpo -match 'IF DB_NAME\(\) <> N''\$\{wmsDatabase\}''')
Registrar 'DDL.sem_NO_CHECK_ou_DISABLE' ($limpo -notmatch '\b(NOCHECK|DISABLE)\b')
$nomes=@([regex]::Matches($limpo,'CONSTRAINT\s+(\w+)\s+(?:PRIMARY KEY|UNIQUE|CHECK|REFERENCES)')|ForEach-Object {$_.Groups[1].Value})
Registrar 'nomes.constraints_unicos_limite_128' (@($nomes|Group-Object|Where-Object {$_.Count -gt 1}).Count -eq 0 -and @($nomes|Where-Object {$_.Length -gt 128}).Count -eq 0)
$saidaFinal=[pscustomobject]@{
    capturadoEm=(Get-Date).ToString('o');natureza='Leitura estaticamente lexical/estrutural com tabela simbolica de pares; nao executa SQL ou JPA.'
    fonteCanonica=$c.fonte;fonteLida=$fonte.Substring($raiz.Length+1).Replace('\','/');fonteSha256=$hashFonte
    fonteCanonicaSha256=(Get-FileHash -LiteralPath $fonteCanonica -Algorithm SHA256).Hash
    migration=$c.migration;migrationSha256=(Get-FileHash -LiteralPath $sqlPath -Algorithm SHA256).Hash
    transcricaoSha256=(Get-FileHash -LiteralPath $contratoPath -Algorithm SHA256).Hash
    leitorSha256=(Get-FileHash -LiteralPath $PSCommandPath -Algorithm SHA256).Hash
    totalChecks=$checks.Count;aprovados=$checks.Count-$divergencias.Count;divergencias=$divergencias;checks=$checks
    tabelas=$resumo;totais=[pscustomobject]@{tabelas=8;colunas=$colsTotal;fks=$fkTotal;unicas=$ukTotal;checksTabelas=$checkTotal;indicesComuns=9;indicesUnicosFiltrados=1;checksCumulativosSubstituidos=5;checksParesNovos=1}
    paresSimbolicos=[pscustomobject]@{combinacoes=$combinacoes;combinacoesAnteriores=$paresAntigos;perdas=$perdas;indevidos=$indevidos;faltantes=$faltantes}
    baselineV1V8=$hashes;comparacaoJpa='Nao realizada: aguarda freeze BE14 e extensao do leitor aos models/literais/guardas reais.'
    limites=@($c.regrasNoServico);semSql=$true;semConexao=$true;semJvm=$true;semBuild=$true;semH2=$true
}
$saidaFinal|ConvertTo-Json -Depth 15
if ($divergencias.Count -gt 0) {exit 1}
