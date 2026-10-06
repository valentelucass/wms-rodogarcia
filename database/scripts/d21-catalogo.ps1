# Expectativa derivada da fonte SQL canonica; Flyway continua unico executor/versionador.
. (Join-Path $PSScriptRoot 'leitores/d20-schema-efetivo.ps1')
function D21No([string]$Op,[object[]]$Filhos){
    # SQL Server reescreve NOT IN como NOT (a=x OR a=y). De Morgan
    # preserva tambem UNKNOWN/NULL; normalizar sem ignorar nenhum predicado.
    if($Op -ceq 'not' -and $Filhos.Count -eq 1){
        $f=$Filhos[0]
        if($f.op -ceq 'not'){return $f.filhos[0]}
        if($f.op -in @('=','<>')){$inverso=if($f.op -ceq '='){'<>'}else{'='};return D21No $inverso $f.filhos}
        if($f.op -in @('and','or')){
            $inverso=if($f.op -ceq 'and'){'or'}else{'and'}
            $negados=@($f.filhos|ForEach-Object {D21No 'not' @($_)})
            return D21No $inverso $negados
        }
    }
    $val=@();foreach($f in $Filhos){if($Op -in @('and','or') -and $f.op -ceq $Op){$val+=@($f.filhos)}else{$val+=$f}}
    if($Op -in @('and','or') -and $val.Count -eq 1){return $val[0]}
    if($Op -in @('and','or','=','<>')){$val=@($val|Sort-Object texto)}
    [pscustomobject]@{op=$Op;filhos=$val;texto=$Op+'('+(@($val|ForEach-Object {$_.texto}) -join ',')+')'}
}
function D21Atomo([string]$Texto){[pscustomobject]@{op='atom';filhos=@();texto=$Texto}}
function D21Consumir($C,[string]$Token){if($C.i -ge $C.ts.Count -or $C.ts[$C.i] -cne $Token){throw 'D21_EXPRESSAO_NAO_SUPORTADA'};$C.i++}
function D21Expr($C,[int]$Min=0){
    if($C.i -ge $C.ts.Count){throw 'D21_EXPRESSAO_INCOMPLETA'}
    $tok=$C.ts[$C.i];$C.i++
    if($tok -ceq '('){$a=D21Expr $C 0;D21Consumir $C ')'}
    elseif($tok -ceq 'not'){$a=D21No 'not' @((D21Expr $C 25))}
    elseif($tok -in @('-','+')){$a=D21No ('unary'+$tok) @((D21Expr $C 60))}
    else{
        $a=D21Atomo $tok
        if($C.i -lt $C.ts.Count -and $C.ts[$C.i] -ceq '('){
            $C.i++;$funcArgs=@();if($C.ts[$C.i] -cne ')'){$funcArgs+=D21Expr $C 0;while($C.ts[$C.i] -ceq ','){$C.i++;$funcArgs+=D21Expr $C 0}}
            D21Consumir $C ')';$a=D21No ('func:'+ $tok) $funcArgs
        }
    }
    while($C.i -lt $C.ts.Count){
        $op=$C.ts[$C.i];$p=switch($op){'or'{10};'and'{20};'not'{30};'between'{30};'in'{30};'is'{30};'='{30};'<>'{30};'!='{30};'>'{30};'<'{30};'>='{30};'<='{30};'+'{40};'-'{40};'*'{50};'/'{50};default{-1}}
        if($p -lt $Min){break};$C.i++
        $negacao=$false
        if($op -ceq 'not'){$negacao=$true;$op=$C.ts[$C.i];$C.i++;if($op -cnotin @('in','between')){throw 'D21_EXPRESSAO_NAO_SUPORTADA'}}
        if($op -ceq 'between'){
            $lo=D21Expr $C 31;D21Consumir $C 'and';$hi=D21Expr $C 31
            if($negacao){$a=D21No 'or' @((D21No '<' @($a,$lo)),(D21No '>' @($a,$hi)))}else{$a=D21No 'and' @((D21No '>=' @($a,$lo)),(D21No '<=' @($a,$hi)))};continue
        }
        if($op -ceq 'in'){
            $rel=if($negacao){'<>'}else{'='};$logico=if($negacao){'and'}else{'or'}
            D21Consumir $C '(';$list=@((D21No $rel @($a,(D21Expr $C 0))))
            while($C.ts[$C.i] -ceq ','){$C.i++;$list+=D21No $rel @($a,(D21Expr $C 0))};D21Consumir $C ')';$a=D21No $logico $list;continue
        }
        if($op -ceq 'is'){$tipo='isnull';if($C.ts[$C.i] -ceq 'not'){$C.i++;$tipo='isnotnull'};D21Consumir $C 'null';$a=D21No $tipo @($a);continue}
        $b=D21Expr $C ($p+1);if($op -ceq '!='){$op='<>'};$a=D21No $op @($a,$b)
    }
    $a
}
function Get-WmsD21Expressao([string]$Sql){
    if([string]::IsNullOrWhiteSpace($Sql)){return ''}
    $s=$Sql -replace '^\s*(CHECK|DEFAULT)\s*',''
    $ts=@();$fim=0
    foreach($m in [regex]::Matches($s,"(?i)N?'(?:''|[^'])*'|\[[a-z_]\w*\]|[a-z_]\w*|\d+(?:\.\d+)?|<>|!=|<=|>=|[()+,=<>*/-]")){
        if($s.Substring($fim,$m.Index-$fim) -match '\S'){throw 'D21_EXPRESSAO_NAO_SUPORTADA'}
        $t=$m.Value;if($t -match "^(?i)N?'"){$t=$t -replace "^(?i)N'","'"}else{$t=$t.Trim('[',']').ToLowerInvariant()}
        $ts+=$t;$fim=$m.Index+$m.Length
    }
    if($s.Substring($fim) -match '\S' -or -not $ts.Count){throw 'D21_EXPRESSAO_NAO_SUPORTADA'}
    $ctx=@{ts=$ts;i=0};$r=D21Expr $ctx 0
    if($ctx.i -ne $ts.Count){throw 'D21_EXPRESSAO_NAO_SUPORTADA'};$r.texto
}
function Get-WmsD21CatalogoEsperado([object[]]$Fontes){
    # Ordenacao so para replay estrutural do parser; selecao/pendentes/checksums sao Flyway.
    $fs=@($Fontes|Sort-Object @{Expression={
        $v=[IO.Path]::GetFileName($_.arquivo) -replace '^V','' -replace '__.*$',''
        (@($v -split '[._]'|ForEach-Object {if($_ -notmatch '^\d{1,24}$'){throw 'D21_FONTE_PARSER_NAO_SUPORTADA'};$_.TrimStart('0').PadLeft(24,'0')}) -join '.')
    }})
    try{$schema=D20SchemaEfetivo $fs}catch{throw 'D21_FONTE_PARSER_NAO_SUPORTADA'}
    $ret=New-Object 'Collections.Generic.List[object]'
    foreach($t in $schema.tabelas.Values){
        $ret.Add([pscustomobject]@{categoria='T';tabela=$t.nome;nome='';valor=''})
        foreach($c in $t.colunas.Values){$ret.Add([pscustomobject]@{categoria='COL';tabela=$t.nome;nome=$c.nome;valor=($c.tipo+'|'+[int]$c.nulo+'|'+[int]$c.identity+'|'+[int]$c.identity+'|'+[int]$c.identity)})}
        foreach($c in $t.constraints.Values){
            $cat=$c.tipo;$v=$c.colunas -join ','
            switch($cat){
                'FK'{$v+='|wms.'+$c.referencia.tabela+'|'+($c.referencia.colunas -join ',')+'|0|0'}
                'CHECK'{$v=Get-WmsD21Expressao $c.definicao}
                'DEFAULT'{$v+='|'+(Get-WmsD21Expressao $c.definicao)}
            }
            $ret.Add([pscustomobject]@{categoria=$cat;tabela=$t.nome;nome=$c.nome;valor=$v})
        }
        foreach($i in $t.indices.Values){
            $ordens=@();$sql=(@($fs|Where-Object {$_.arquivo -ceq $i.origem})[0]).texto
            $m=[regex]::Match((D20LimparSql $sql),'(?s)CREATE(?: UNIQUE)? INDEX '+[regex]::Escape($i.nome)+' ON wms\.'+[regex]::Escape($t.nome)+'\s*\((?<cols>[^)]+)\)')
            foreach($p in $m.Groups['cols'].Value -split ','){$ordens+=if($p.Trim() -match '\sDESC$'){'1'}else{'0'}}
            $v=([string][int]$i.unique)+'|'+($i.colunas -join ',')+'|'+($ordens -join ',')+'|'+(@($i.incluidas|Sort-Object) -join ',')+'|'+(Get-WmsD21Expressao $i.filtro)
            $ret.Add([pscustomobject]@{categoria='IDX';tabela=$t.nome;nome=$i.nome;valor=$v})
        }
    }
    $ret.ToArray()
}
function Convert-WmsD21CatalogoReal([object[]]$Itens){
    foreach($i in $Itens){
        $v=[string]$i.valor
        if($i.categoria -ceq 'CHECK'){$v=Get-WmsD21Expressao $v}
        if($i.categoria -ceq 'DEFAULT'){$p=$v -split '\|',2;$v=$p[0]+'|'+(Get-WmsD21Expressao $p[1])}
        if($i.categoria -ceq 'IDX'){$p=$v -split '\|',5;$v=($p[0..2] -join '|')+'|'+(@($p[3] -split ','|Where-Object {$_}|Sort-Object) -join ',')+'|'+(Get-WmsD21Expressao $p[4])}
        [pscustomobject]@{categoria=$i.categoria;tabela=$i.tabela;nome=$i.nome;valor=$v}
    }
}
function Assert-WmsD21Catalogo([object[]]$Esperado,[object[]]$Real){
    $a=@($Esperado|ForEach-Object {$_.categoria+'|'+$_.tabela+'|'+$_.nome+'|'+$_.valor}|Sort-Object)
    $b=@($Real|ForEach-Object {$_.categoria+'|'+$_.tabela+'|'+$_.nome+'|'+$_.valor}|Sort-Object)
    if($a.Count -eq 0 -or $a.Count -ne $b.Count -or ($a -join "`n") -cne ($b -join "`n")){throw 'D21_CATALOGO_DIVERGENTE_DA_FONTE'}
}
