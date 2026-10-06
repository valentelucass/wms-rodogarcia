# Parser do subconjunto SQL usado por V1-V9. Nao executa SQL. Falha em sintaxe desconhecida.
function D20LimparSql([string]$Texto) {
    $b=New-Object Text.StringBuilder;$modo='codigo'
    for($i=0;$i -lt $Texto.Length;$i++) {
        $ch=$Texto[$i];$prox=if($i+1 -lt $Texto.Length){$Texto[$i+1]}else{[char]0}
        if($modo -eq 'linha'){if($ch -eq "`n"){$modo='codigo';[void]$b.Append($ch)};continue}
        if($modo -eq 'bloco'){
            if($ch -eq '/' -and $prox -eq '*'){throw 'D20_COMENTARIO_ANINHADO_NAO_SUPORTADO'}
            if($ch -eq '*' -and $prox -eq '/'){$modo='codigo';$i++};continue
        }
        if($modo -eq 'literal'){
            [void]$b.Append($ch)
            if($ch -eq "'"){if($prox -eq "'"){[void]$b.Append($prox);$i++}else{$modo='codigo'}};continue
        }
        if($ch -eq '-' -and $prox -eq '-'){$modo='linha';[void]$b.Append(' ');$i++;continue}
        if($ch -eq '/' -and $prox -eq '*'){$modo='bloco';[void]$b.Append(' ');$i++;continue}
        if($ch -eq "'"){$modo='literal'}
        [void]$b.Append($ch)
    }
    if($modo -in @('literal','bloco')){throw 'D20_SQL_INCOMPLETO'}
    $b.ToString() -replace '(?m)^\s*GO\s*$',''
}
function D20Partes([string]$Texto,[char]$Separador) {
    $inicio=0;$nivel=0;$literal=$false;$ret=@()
    for($i=0;$i -lt $Texto.Length;$i++){
        $ch=$Texto[$i]
        if($ch -eq "'"){
            if($literal -and $i+1 -lt $Texto.Length -and $Texto[$i+1] -eq "'"){$i++;continue}
            $literal=-not $literal;continue
        }
        if($literal){continue}
        if($ch -eq '('){$nivel++}elseif($ch -eq ')'){$nivel--;if($nivel -lt 0){throw 'D20_PARENTESE_INVALIDO'}}
        elseif($ch -eq $Separador -and $nivel -eq 0){$ret+=$Texto.Substring($inicio,$i-$inicio).Trim();$inicio=$i+1}
    }
    if($literal -or $nivel -ne 0){throw 'D20_SQL_INCOMPLETO'}
    $ret+=$Texto.Substring($inicio).Trim();@($ret|Where-Object {$_})
}
function D20ColunasLista([string]$Texto) {
    @($Texto -split ','|ForEach-Object {
        $nome=$_.Trim() -replace '\s+(ASC|DESC)$',''
        if($nome -notmatch '^[a-z]\w*$'){throw 'D20_COLUNA_NAO_SUPORTADA'};$nome
    })
}
function D20Constraint($Tabela,[string]$Trecho,[string]$Coluna,[string]$Origem) {
    $m=[regex]::Match($Trecho,'(?s)^CONSTRAINT\s+(?<nome>\w+)\s+(?<def>.+)$')
    if(-not $m.Success){throw 'D20_CONSTRAINT_NAO_SUPORTADA'}
    $n=$m.Groups['nome'].Value;$d=$m.Groups['def'].Value
    if($Tabela.constraints.Contains($n)){throw "D20_CONSTRAINT_DUPLICADA_$n"}
    $tipo='';$cols=@();$referencia=$null
    if($d -match '(?s)^(?:FOREIGN KEY\s*\((?<cols>[^)]+)\)\s+)?REFERENCES wms\.(?<t>\w+)\s*\((?<ref>[^)]+)\)$'){
        $tipo='FK';$cols=if($Matches.cols){D20ColunasLista $Matches.cols}else{@($Coluna)}
        $referencia=[pscustomobject]@{tabela=$Matches.t;colunas=@(D20ColunasLista $Matches.ref)}
    } elseif($d -match '^(PRIMARY KEY|UNIQUE)(?:\s*\((?<cols>[^)]+)\))?$'){
        $tipo=if($Matches[1] -eq 'PRIMARY KEY'){'PK'}else{'UNIQUE'}
        $cols=if($Matches.cols){D20ColunasLista $Matches.cols}else{@($Coluna)}
    } elseif($d -match '(?s)^CHECK\s*\(.+\)$'){$tipo='CHECK'}
    elseif($d -match '(?s)^DEFAULT\s+.+$'){$tipo='DEFAULT';$cols=@($Coluna)}
    else{throw "D20_CONSTRAINT_DEFINICAO_NAO_SUPORTADA_$n"}
    $Tabela.constraints[$n]=[pscustomobject]@{nome=$n;tipo=$tipo;colunas=@($cols);referencia=$referencia;definicao=$d;origem=$Origem}
}
function D20Adicionar($Tabela,[string]$Trecho,[string]$Origem) {
    if($Trecho -match '^CONSTRAINT\s'){D20Constraint $Tabela $Trecho '' $Origem;return}
    $m=[regex]::Match($Trecho,'(?s)^(?<nome>[a-z]\w*)\s+(?<tipo>bigint|int|bit|date|datetime2\(\d+\)|decimal\(\d+,\d+\)|n?varchar\((?:\d+|max)\))(?<identity>\s+IDENTITY\(1,1\))?\s+(?<nulo>NOT NULL|NULL)(?<rest>.*)$')
    if(-not $m.Success){throw "D20_COLUNA_DEFINICAO_NAO_SUPORTADA: $Trecho"}
    $n=$m.Groups['nome'].Value
    if($Tabela.colunas.Contains($n)){throw "D20_COLUNA_DUPLICADA_$n"}
    $Tabela.colunas[$n]=[pscustomobject]@{nome=$n;tipo=$m.Groups['tipo'].Value;nulo=($m.Groups['nulo'].Value -eq 'NULL');identity=$m.Groups['identity'].Success;origem=$Origem}
    $rest=$m.Groups['rest'].Value.Trim()
    if($rest){D20Constraint $Tabela $rest $n $Origem}
}
function D20SchemaEfetivo([object[]]$Fontes) {
    $tabelas=[ordered]@{};$alteracoes=@();$dml=@();$sessao=@()
    foreach($f in $Fontes){
        foreach($s in @(D20Partes (D20LimparSql $f.texto) ';')){
            if($s -match '(?s)^IF DB_NAME\(\).*THROW 51000,'){continue}
            if($s -match '^SET \w+ (ON|OFF)$'){$sessao+=[pscustomobject]@{origem=$f.arquivo;sql=$s};continue}
            if($s -match '(?s)^CREATE TABLE wms\.(?<nome>\w+)\s*\((?<corpo>.*)\)$'){
                $n=$Matches.nome;$corpo=$Matches.corpo
                if($tabelas.Contains($n)){throw "D20_TABELA_DUPLICADA_$n"}
                $t=[pscustomobject]@{nome=$n;origem=$f.arquivo;colunas=[ordered]@{};constraints=[ordered]@{};indices=[ordered]@{}}
                $tabelas[$n]=$t
                foreach($p in @(D20Partes $corpo ',')){D20Adicionar $t $p $f.arquivo};continue
            }
            if($s -match '(?s)^ALTER TABLE wms\.(?<nome>\w+)\s+(?:WITH CHECK\s+)?(?<acao>ADD|DROP CONSTRAINT)\s+(?<corpo>.*)$'){
                $n=$Matches.nome;$acao=$Matches.acao;$corpo=$Matches.corpo
                if(-not $tabelas.Contains($n)){throw "D20_TABELA_AUSENTE_$n"}
                $t=$tabelas[$n]
                if($acao -eq 'ADD'){foreach($p in @(D20Partes $corpo ',')){D20Adicionar $t $p $f.arquivo}}
                else{if(-not $t.constraints.Contains($corpo)){throw "D20_DROP_INEXISTENTE_$corpo"};$t.constraints.Remove($corpo)}
                $alteracoes+=[pscustomobject]@{tabela=$n;acao=$acao;origem=$f.arquivo;definicao=$corpo};continue
            }
            if($s -match '(?s)^CREATE(?<unique> UNIQUE)? INDEX (?<nome>\w+) ON wms\.(?<t>\w+)\s*\((?<cols>[^)]+)\)(?:\s+INCLUDE\s*\((?<incl>[^)]+)\))?(?:\s+WHERE (?<filtro>.+))?$'){
                $n=$Matches.nome;$tn=$Matches.t
                if(-not $tabelas.Contains($tn) -or $tabelas[$tn].indices.Contains($n)){throw "D20_INDICE_INVALIDO_$n"}
                $tabelas[$tn].indices[$n]=[pscustomobject]@{nome=$n;unique=([bool]$Matches.unique);colunas=@(D20ColunasLista $Matches.cols);incluidas=if($Matches.incl){@(D20ColunasLista $Matches.incl)}else{@()};filtro=$Matches.filtro;origem=$f.arquivo};continue
            }
            if($s -match '^UPDATE wms\.unidade_logistica SET revisao_conteudo = versao$'){$dml+=[pscustomobject]@{sql=$s;origem=$f.arquivo};continue}
            throw "D20_STATEMENT_NAO_SUPORTADO: $s"
        }
    }
    foreach($t in $tabelas.Values){
        $pks=@($t.constraints.Values|Where-Object {$_.tipo -eq 'PK'})
        if($pks.Count -ne 1){throw "D20_PK_INCORRETA_$($t.nome)"}
        foreach($c in $t.constraints.Values){
            foreach($col in $c.colunas){if(-not $t.colunas.Contains($col)){throw "D20_CONSTRAINT_COLUNA_AUSENTE_$($c.nome)"}}
            if($c.tipo -eq 'FK'){
                $r=$c.referencia
                if(-not $tabelas.Contains($r.tabela) -or $c.colunas.Count -ne $r.colunas.Count){throw "D20_FK_INVALIDA_$($c.nome)"}
                for($i=0;$i -lt $c.colunas.Count;$i++){
                    if(-not $tabelas[$r.tabela].colunas.Contains($r.colunas[$i]) -or $t.colunas[$c.colunas[$i]].tipo -cne $tabelas[$r.tabela].colunas[$r.colunas[$i]].tipo){throw "D20_FK_TIPO_INVALIDO_$($c.nome)"}
                }
                $chaves=@($tabelas[$r.tabela].constraints.Values|Where-Object {$_.tipo -in @('PK','UNIQUE') -and ($_.colunas -join ',') -ceq ($r.colunas -join ',')})
                if(-not $chaves.Count){throw "D20_FK_ALVO_SEM_CHAVE_$($c.nome)"}
            }
        }
        foreach($idx in $t.indices.Values){foreach($col in @($idx.colunas)+@($idx.incluidas)){if(-not $t.colunas.Contains($col)){throw "D20_INDICE_COLUNA_AUSENTE_$($idx.nome)"}}}
    }
    [pscustomobject]@{tabelas=$tabelas;alteracoes=$alteracoes;dml=$dml;sets=$sessao}
}
