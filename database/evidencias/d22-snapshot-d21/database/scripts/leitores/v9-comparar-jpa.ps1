# Definicoes somente; nenhuma leitura/backend/comparacao ao carregar.
function V9ModelCongelado([string]$raiz,$freeze,[string]$classe,[string[]]$pilha=@()) {
    if($classe -notmatch '^\w+$' -or $classe -cin $pilha -or $pilha.Count -ge 8){throw 'PARSER_HERANCA_CICLICA_OU_LIMITE'}
    $rel='backend/src/main/java/br/com/rodogarcia/wms/models/'+$classe+'.java'
    $m=V9ModelTexto (V9LerCongelado $raiz $freeze $rel)
    $m|Add-Member -NotePropertyName arquivo -NotePropertyValue $rel
    $m|Add-Member -NotePropertyName herdadas -NotePropertyValue @()
    if($m.base) {
        $base=V9ModelCongelado $raiz $freeze $m.base (@($pilha)+@($classe))
        if(-not $base.mappedSuperclass){throw 'PARSER_HERANCA_SEM_MAPPEDSUPERCLASS'}
        $m.campos=@($m.campos)+@($base.campos);$m.herdadas=@($base.arquivo)+@($base.herdadas);$m.problemas=@($m.problemas)+@($base.problemas)
    }
    foreach($emb in @($m.campos|Where-Object {$_.embedded})) {
        $b=V9ModelCongelado $raiz $freeze $emb.java (@($pilha)+@($classe))
        if(-not $b.embeddable -or @($b.campos|Where-Object {$_.embedded}).Count){throw 'PARSER_EMBEDDED_NAO_SUPORTADO'}
        $m.campos=@($m.campos|Where-Object {$_.campoJava -cne $emb.campoJava})+@($b.campos);$m.herdadas+=@($b.arquivo);$m.problemas+=@($b.problemas)
    }
    if(@($m.campos.nome|Group-Object|Where-Object {$_.Count -gt 1}).Count){$m.problemas+=@('coluna_duplicada_heranca')}
    $m
}
function V9InventariarLiterais([string]$raiz,$freeze,$c) {
    $admin=@();$auditoria=@();$movimentos=@();$pendencias=@();$enums=@{}
    foreach($e in @($c.jpa.enumsRepasse)){$texto=V9LerCongelado $raiz $freeze $e.arquivo;$enums[$e.tipo]=@(V9ValoresEnum $texto $e.enum)}
    $services=@($freeze.mapa.Keys|Where-Object {$_ -match '^backend/src/main/java/br/com/rodogarcia/wms/services/\w+\.java$'}|Sort-Object)
    foreach($rel in $services) {
        $s=V9SemComentarios (V9LerCongelado $raiz $freeze $rel);$metodos=@(V9Metodos $s)
        $enumsEscopo=@{}
        foreach($e in @($c.jpa.enumsRepasse)) {if($rel -cin @($e.services)){$enumsEscopo[$e.tipo]=$enums[$e.tipo]}}
        foreach($inj in [regex]::Matches($s,'private\s+final\s+OperacaoAdministrativaService\s+(\w+)\s*;')) {
            foreach($ch in @(V9Chamadas $s ('\b'+[regex]::Escape($inj.Groups[1].Value)+'\.salvar'))) {
                try {if($ch.argumentos.Count -ne 7){throw 'PARSER_SALVAR_ARIDADE'};$valores=@(V9ResolverLiteral $s $metodos $ch.argumentos[1] $ch.indice $enumsEscopo);foreach($valor in $valores){$admin+=[pscustomobject]@{arquivo=$rel;linha=$ch.linha;tipo=$valor}}}
                catch {$pendencias+=($rel+':'+$ch.linha+':administrativa.literal_nao_resolvido')}
            }
        }
        if($rel -cnotin @($c.jpa.servicesAuditoria)){continue}
        foreach($inj in [regex]::Matches($s,'private\s+final\s+AuditoriaService\s+(\w+)\s*;')) {
            foreach($ch in @(V9Chamadas $s ('\b'+[regex]::Escape($inj.Groups[1].Value)+'\.registrar'))) {
                try {if($ch.argumentos.Count -ne 6){throw 'PARSER_AUDITORIA_ARIDADE'};$tipos=@(V9ResolverLiteral $s $metodos $ch.argumentos[0] $ch.indice $enumsEscopo);$acoes=@(V9ResolverLiteral $s $metodos $ch.argumentos[2] $ch.indice $enumsEscopo);foreach($tipo in $tipos){foreach($acao in $acoes){$auditoria+=[pscustomobject]@{arquivo=$rel;linha=$ch.linha;tipo=$tipo;acao=$acao}}}}
                catch {$pendencias+=($rel+':'+$ch.linha+':auditoria.par_nao_resolvido')}
            }
        }
        foreach($ch in @(V9Chamadas $s '\bnew\s+MovimentoEstoque')) {
            try {if($ch.argumentos.Count -lt 5){throw 'PARSER_MOVIMENTO_ARIDADE'};$acoes=@(V9ResolverLiteral $s $metodos $ch.argumentos[4] $ch.indice $enumsEscopo);foreach($acao in $acoes){$movimentos+=[pscustomobject]@{arquivo=$rel;linha=$ch.linha;acao=$acao}}}
            catch {$pendencias+=($rel+':'+$ch.linha+':movimento.acao_nao_resolvida')}
        }
    }
    [pscustomobject]@{administrativas=$admin;auditoria=$auditoria;movimentos=$movimentos;pendencias=$pendencias;limite='Resolucao lexical de literais, ternarios e repasses privados sem reatribuicao. Dominios de enum sao sobreaproximacao declarada, sem provar ramo/fluxo/efeito.'}
}
function V9CompararJpaArquivos([string]$raiz,$freeze,$c,[string]$sql) {
    $checks=[ordered]@{};$models=@();$unicas=@();$filtrados=@();$enums=@();$cols=0
    $alvos=@($c.tabelas|ForEach-Object {[pscustomobject]@{tabela=$_.nome;model=$_.model;colunas=$_.colunas;unicas=$_.unicas;parcial=$false}})+@($c.jpa.cadastros|ForEach-Object {[pscustomobject]@{tabela=$_.tabela;model=$_.model;colunas=@([pscustomobject]@{nome='situacao';tipo='varchar(24)';nulo=$false;mutavel=$true});unicas=@();parcial=$true}})
    foreach($a in $alvos) {
        $m=V9ModelCongelado $raiz $freeze $a.model
        $checks[($a.tabela+'.table_schema')]=($m.tabela -ceq $a.tabela -and $m.schema -ceq 'wms')
        $checks[($a.tabela+'.parser_sem_lacuna')]=(@($m.problemas).Count -eq 0)
        if(-not $a.parcial){$checks[($a.tabela+'.campos_exatos')]=(MesmoConjunto @($a.colunas.nome) @($m.campos.nome))}
        foreach($col in $a.colunas) {
            $f=@($m.campos|Where-Object {$_.nome -ceq $col.nome});$ok=$f.Count -eq 1
            $checks[($a.tabela+'.'+$col.nome+'.tipo_nulo')]=($ok -and $f[0].tipo -ceq $col.tipo -and $f[0].nulo -eq $col.nulo)
            $checks[($a.tabela+'.'+$col.nome+'.mutabilidade_declarada')]=($ok -and $f[0].mutavel -eq $col.mutavel)
            if($col.identity){$checks[($a.tabela+'.'+$col.nome+'.identity')]=($ok -and $f[0].identity -and $f[0].id)}
            if($col.nome -ceq 'versao'){$checks[($a.tabela+'.Version')]=($ok -and $f[0].version)}
            if($col.tipo -eq 'datetime2(6)'){$checks[($a.tabela+'.'+$col.nome+'.TIMESTAMP_declarado')]=($ok -and $f[0].jdbcTimestamp)}
            if($col.referencia) {
                $ref='';if($ok -and $f[0].relacao){$r=V9LerCongelado $raiz $freeze ('backend/src/main/java/br/com/rodogarcia/wms/models/'+$f[0].java+'.java');$an=@(V9Anotacoes (V9SemComentarios $r)|Where-Object {$_.nome -eq 'Table'});if($an.Count -eq 1){$ref=V9TextoJava (V9Atributo $an[0].argumentos 'name')}}
                $checks[($a.tabela+'.'+$col.nome+'.FK_model_referido')]=($ref -ceq $col.referencia)
                if(-not $col.nulo){$checks[($a.tabela+'.'+$col.nome+'.relacao_obrigatoria')]=($ok -and $f[0].optionalFalse)}
            }
            if($ok -and $f[0].enumAnotado) {
                $checks[($a.tabela+'.'+$col.nome+'.enum_STRING')]=[bool]$f[0].enumString
                $textoEnum=V9LerCongelado $raiz $freeze ('backend/src/main/java/br/com/rodogarcia/wms/models/'+$f[0].java+'.java');$valores=@(V9ValoresEnum $textoEnum $f[0].java)
                $enumCheck=@($c.jpa.enums|Where-Object {$_.tabela -ceq $a.tabela -and $_.coluna -ceq $col.nome})
                $dominio=if($a.parcial){@('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')}elseif($enumCheck.Count -eq 1){@(Literais (CorpoCheck $sql $enumCheck[0].check))}else{@()}
                $checks[($a.tabela+'.'+$col.nome+'.enum_dominio_exato')]=(MesmoConjunto $dominio $valores)
                $enums+=[pscustomobject]@{tabela=$a.tabela;coluna=$col.nome;java=$f[0].java;valores=$valores;dominioSql=$dominio}
            } elseif($a.parcial -and $ok -and $f[0].java -ceq 'String' -and @($c.jpa.cadastros|Where-Object {$_.tabela -ceq $a.tabela -and $_.representacao -ceq 'StringComCheck'}).Count -eq 1) {
                $textoCheck=V9LerCongelado $raiz $freeze $m.arquivo
                $valores=@(V9DominioStringCheck $textoCheck $col.nome);$dominio=@('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
                $checks[($a.tabela+'.'+$col.nome+'.String_Check_dominio_exato')]=(MesmoConjunto $dominio $valores)
                $enums+=[pscustomobject]@{tabela=$a.tabela;coluna=$col.nome;java='StringComCheck';valores=$valores;dominioSql=$dominio;limite='Check anotado, nao enum ou DDL executado.'}
            } elseif($a.parcial -or @($c.jpa.enums|Where-Object {$_.tabela -ceq $a.tabela -and $_.coluna -ceq $col.nome}).Count) {$checks[($a.tabela+'.'+$col.nome+'.enum_esperado_presente')]=$false}
            $cols++
        }
        if(-not $a.parcial) {
            foreach($u in $a.unicas) {$achadas=@($m.unicas|Where-Object {($_.colunas -join ',') -ceq ($u.colunas -join ',')});$checks[($a.tabela+'.'+$u.nome+'.chave_unica')]=($achadas.Count -eq 1);$checks[($a.tabela+'.'+$u.nome+'.nome_Jpa_registrado')]=($achadas.Count -eq 1 -and $achadas[0].nome -ceq $u.nomeJpa);$unicas+=[pscustomobject]@{tabela=$a.tabela;nomeSql=$u.nome;nomeJpa=@($achadas.nome);chave=$u.colunas}}
            $totalExtra=@($c.indicesFiltrados|Where-Object {$_.tabela -ceq $a.tabela}).Count
            $checks[($a.tabela+'.unicas_quantidade')]=($m.unicas.Count -eq @($a.unicas).Count+$totalExtra)
            foreach($idx in @($c.indicesFiltrados|Where-Object {$_.tabela -ceq $a.tabela})) {$achadas=@($m.unicas|Where-Object {($_.colunas -join ',') -ceq ($idx.colunas -join ',')});$checks[($a.tabela+'.nullable_unica_mesma_chave')]=($achadas.Count -eq 1);$filtrados+=[pscustomobject]@{tabela=$a.tabela;nomeSql=$idx.nome;chave=$idx.colunas;predicadoSql=$idx.predicado;origemJpa=@($achadas.origem);filtroExpressoJpa=$false;semanticaRealNaoConferida=$true}}
        }
        $models+=[pscustomobject]@{tabela=$a.tabela;classe=$a.model;arquivo=$m.arquivo;heranca=$m.herdadas;parcial=$a.parcial;campos=$m.campos|Select-Object nome,java,tipo,nulo,mutavel,identity,version}
    }
    $literais=V9InventariarLiterais $raiz $freeze $c
    $checks['literais.cobertura_sem_nao_resolvidos']=(@($literais.pendencias).Count -eq 0)
    $domAdmin=@(Literais (CorpoCheck $sql 'ck_operacao_administrativa_tipo'));$domMov=@(Literais (CorpoCheck $sql 'ck_movimento_acao'))
    foreach($lit in $literais.administrativas){$checks[('admin.'+$lit.arquivo+'.'+$lit.linha+'.'+$lit.tipo)]=($lit.tipo -cin $domAdmin)}
    foreach($lit in $literais.movimentos){$checks[('movimento.'+$lit.arquivo+'.'+$lit.linha+'.'+$lit.acao)]=($lit.acao -cin $domMov)}
    $sql6=Get-Content -LiteralPath (Join-Path $raiz 'database/migrations/V6__separacao_retirada_retornos_e_avaria.sql') -Raw -Encoding UTF8 -ErrorAction Stop
    $sql8=Get-Content -LiteralPath (Join-Path $raiz 'database/migrations/V8__fechamento_e_versoes.sql') -Raw -Encoding UTF8 -ErrorAction Stop
    $pares=@(AnalisarPar (CorpoCheck $sql 'ck_auditoria_be14_tipo_acao'))+@(AnalisarPar (CorpoCheck $sql 'ck_auditoria_financeira_tipo_acao'))+@(AnalisarPar (CorpoCheck $sql6 'ck_auditoria_saida_tipo_acao'))+@(AnalisarPar (CorpoCheck $sql8 'ck_auditoria_fechamento_tipo_acao'))
    foreach($lit in $literais.auditoria){$aceito=$lit.tipo -cin @($c.auditoria.tiposAnteriores+$c.auditoria.tiposNovos) -and $lit.acao -cin @($c.auditoria.acoesAnteriores+$c.auditoria.acoesNovas);foreach($par in $pares){$aceito=$aceito -and (AvaliarPar $par $lit.tipo $lit.acao)};$checks[('auditoria.'+$lit.arquivo+'.'+$lit.linha+'.'+$lit.tipo+'/'+$lit.acao)]=$aceito}
    foreach($esperado in $c.jpa.literaisEsperados){$checks[('literal_BE14.'+$esperado)]=($esperado -cin @($literais.administrativas.tipo))}
    foreach($rel in $freeze.fontes){$checks[('hash_depois.'+$rel.arquivo)]=((Get-FileHash -LiteralPath (Join-Path $raiz $rel.arquivo) -Algorithm SHA256 -ErrorAction Stop).Hash -ceq $rel.antes)}
    $checks['freeze.manifesto_estavel']=((Get-FileHash -LiteralPath (Join-Path $raiz $freeze.arquivo) -Algorithm SHA256 -ErrorAction Stop).Hash -ceq $freeze.sha256)
    [pscustomobject]@{solicitado=$true;natureza='Somente leitura lexical/estrutural sob manifesto fornecido apos freeze; nao e execucao de JPA nem aceite BE14';checks=$checks;models=$models;colunasConferidas=$cols;unicas=$unicas;enums=$enums;indicesFiltrados=$filtrados;literais=$literais;freeze=[pscustomobject]@{arquivo=$freeze.arquivo;sha256=$freeze.sha256;fontes=$freeze.fontes.ToArray()};limites=@('Nomes JPA/SQL locais separados; filtros, ISJSON e CHECKs SQL nao executados.','Instant/TIMESTAMP representa contrato DATETIME2(6), sem observar DDL Hibernate/precisao efetiva.','Sobreaproximacao de literais nao prova caminho, imutabilidade por servico, rollback, locks, collation ou permissao real.')}
}
