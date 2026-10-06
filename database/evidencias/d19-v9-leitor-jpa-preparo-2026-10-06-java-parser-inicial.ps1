# Funcoes textuais apenas. Dot-source nao le backend, nao executa comparacao ou Java.
function V9SemComentarios([string]$texto) {
    $b=New-Object Text.StringBuilder;$modo='codigo';$escape=$false
    for($i=0;$i -lt $texto.Length;$i++) {
        $ch=$texto[$i];$prox=if($i+1 -lt $texto.Length){$texto[$i+1]}else{[char]0}
        if($modo -eq 'linha') {if($ch -eq "`n"){$modo='codigo';[void]$b.Append($ch)}else{[void]$b.Append(' ')};continue}
        if($modo -eq 'bloco') {if($ch -eq '*' -and $prox -eq '/'){[void]$b.Append('  ');$i++;$modo='codigo'}else{[void]$b.Append($(if($ch -in @("`r","`n")){$ch}else{' '}))};continue}
        if($modo -in @('string','char')) {
            [void]$b.Append($ch)
            if($escape){$escape=$false;continue};if($ch -eq '\'){$escape=$true;continue}
            if(($modo -eq 'string' -and $ch -eq '"') -or ($modo -eq 'char' -and $ch -eq "'")){$modo='codigo'};continue
        }
        if($ch -eq '/' -and $prox -eq '/'){$modo='linha';[void]$b.Append('  ');$i++;continue}
        if($ch -eq '/' -and $prox -eq '*'){$modo='bloco';[void]$b.Append('  ');$i++;continue}
        if($ch -eq '"') {if($i+2 -lt $texto.Length -and $texto.Substring($i,3) -eq '"""'){throw 'PARSER_TEXT_BLOCK_NAO_SUPORTADO'};$modo='string'}
        elseif($ch -eq "'"){$modo='char'}
        [void]$b.Append($ch)
    }
    if($modo -in @('bloco','string','char')){throw 'PARSER_LITERAL_OU_COMENTARIO_INCOMPLETO'}
    $b.ToString()
}
function V9MascaraLiterais([string]$texto) {
    $b=New-Object Text.StringBuilder;$modo='codigo';$escape=$false
    foreach($ch in $texto.ToCharArray()) {
        if($modo -eq 'codigo') {if($ch -eq '"'){$modo='string';[void]$b.Append(' ')}elseif($ch -eq "'"){$modo='char';[void]$b.Append(' ')}else{[void]$b.Append($ch)};continue}
        [void]$b.Append($(if($ch -in @("`r","`n")){$ch}else{' '}))
        if($escape){$escape=$false;continue};if($ch -eq '\'){$escape=$true;continue}
        if(($modo -eq 'string' -and $ch -eq '"') -or ($modo -eq 'char' -and $ch -eq "'")){$modo='codigo'}
    }
    $b.ToString()
}
function V9Delimitado([string]$texto,[int]$aberto) {
    if($aberto -lt 0 -or $aberto -ge $texto.Length -or $texto[$aberto] -notin @('(','{','[')){throw 'PARSER_DELIMITADOR'}
    $pilha=New-Object 'Collections.Generic.List[char]';$inicio=$aberto+1;$partes=@();$modo='codigo';$escape=$false
    for($i=$aberto;$i -lt $texto.Length;$i++) {
        $ch=$texto[$i]
        if($modo -ne 'codigo') {if($escape){$escape=$false;continue};if($ch -eq '\'){$escape=$true;continue};if(($modo -eq 'string' -and $ch -eq '"') -or ($modo -eq 'char' -and $ch -eq "'")){$modo='codigo'};continue}
        if($ch -eq '"'){$modo='string';continue};if($ch -eq "'"){$modo='char';continue}
        if($ch -in @('(','{','[')){$pilha.Add($ch);continue}
        if($ch -in @(')','}',']')) {
            $esperado=switch($ch){')'{'('};'}'{'{'};']'{'['}}
            if($pilha.Count -eq 0 -or $pilha[$pilha.Count-1] -ne $esperado){throw 'PARSER_DELIMITADORES_INCOMPATIVEIS'}
            $pilha.RemoveAt($pilha.Count-1)
            if($pilha.Count -eq 0){$partes+=$texto.Substring($inicio,$i-$inicio).Trim();return [pscustomobject]@{partes=$partes;fim=$i;corpo=$texto.Substring($aberto+1,$i-$aberto-1)}}
            continue
        }
        if($ch -eq ',' -and $pilha.Count -eq 1){$partes+=$texto.Substring($inicio,$i-$inicio).Trim();$inicio=$i+1}
    }
    throw 'PARSER_DELIMITADOR_INCOMPLETO'
}
function V9Anotacoes([string]$texto) {
    $lista=@();$cursor=0
    while($cursor -lt $texto.Length) {
        $m=[regex]::Match($texto.Substring($cursor),'@(?<nome>[A-Za-z_]\w*(?:\.\w+)*)')
        if(-not $m.Success){break};$pos=$cursor+$m.Index;$fim=$pos+$m.Length;$arg=''
        while($fim -lt $texto.Length -and [char]::IsWhiteSpace($texto[$fim])){$fim++}
        if($fim -lt $texto.Length -and $texto[$fim] -eq '('){$d=V9Delimitado $texto $fim;$arg=$d.corpo;$fim=$d.fim+1}
        $lista+=[pscustomobject]@{nome=($m.Groups['nome'].Value -split '\.')[-1];argumentos=$arg;inicio=$pos;fim=$fim}
        $cursor=$fim
    }
    $lista
}
function V9Atributo([string]$args,[string]$nome,[string]$padrao='') {
    $d=V9Delimitado ('('+$args+')') 0
    foreach($p in $d.partes){$m=[regex]::Match($p,'^'+[regex]::Escape($nome)+'\s*=\s*(?<v>.*)$',[Text.RegularExpressions.RegexOptions]::Singleline);if($m.Success){return $m.Groups['v'].Value.Trim()}}
    $padrao
}
function V9TextoJava([string]$texto) {
    $m=[regex]::Match($texto,'^"(?<v>[^"\\]*)"$');if(-not $m.Success){throw 'PARSER_STRING_NAO_LITERAL'};$m.Groups['v'].Value
}
function V9ValoresEnum([string]$texto,[string]$nome) {
    $s=V9SemComentarios $texto;$m=[regex]::Match($s,'\benum\s+'+[regex]::Escape($nome)+'\s*\{')
    if(-not $m.Success){throw 'PARSER_ENUM_AUSENTE'};$d=V9Delimitado $s ($m.Index+$m.Length-1);$c=($d.corpo -split ';',2)[0].Trim()
    if($c -notmatch '^[A-Z][A-Z0-9_]*(?:\s*,\s*[A-Z][A-Z0-9_]*)*\s*,?\s*$'){throw 'PARSER_ENUM_COMPLEXO_NAO_SUPORTADO'}
    @([regex]::Matches($c,'\b[A-Z][A-Z0-9_]*\b')|ForEach-Object {$_.Value})
}
function V9ModelTexto([string]$texto) {
    $s=V9SemComentarios $texto
    if($s -match '@(?:Access|Inheritance|AttributeOverrides?|JoinColumns|EmbeddedId)\b'){throw 'PARSER_MAPPING_NAO_SUPORTADO'}
    $classe=[regex]::Match($s,'\bclass\s+(?<nome>\w+)(?:\s+extends\s+(?<base>\w+))?[^\{]*\{')
    if(-not $classe.Success){throw 'PARSER_CLASSE_AUSENTE'}
    $anotacoes=@(V9Anotacoes ($s.Substring(0,$classe.Index)))
    $table=@($anotacoes|Where-Object {$_.nome -eq 'Table'});$tabela='';$schema='';$unicas=@()
    if($table.Count -gt 1){throw 'PARSER_TABLE_DUPLICADA'}
    if($table.Count -eq 1) {
        $tabela=V9TextoJava (V9Atributo $table[0].argumentos 'name');$schema=V9TextoJava (V9Atributo $table[0].argumentos 'schema' '""')
        foreach($a in @(V9Anotacoes $table[0].argumentos)) {
            if($a.nome -notin @('UniqueConstraint','Index')){throw 'PARSER_TABLE_ANOTACAO_DESCONHECIDA'}
            if($a.nome -eq 'Index' -and (V9Atributo $a.argumentos 'unique' 'false') -ne 'true'){continue}
            $key=if($a.nome -eq 'Index'){'columnList'}else{'columnNames'};$v=V9Atributo $a.argumentos $key
            $cols=if($v.StartsWith('{')){@((V9Delimitado $v 0).partes|Where-Object {$_}|ForEach-Object {V9TextoJava $_})}else{@((V9TextoJava $v) -split '\s*,\s*')}
            $unicas+=[pscustomobject]@{nome=(V9TextoJava (V9Atributo $a.argumentos 'name' '""'));colunas=$cols;origem=$a.nome;filtrada=$false}
        }
    }
    $corpo=(V9Delimitado $s ($classe.Index+$classe.Length-1)).corpo
    $corpoMascara=V9MascaraLiterais $corpo
    $campos=@();$ignorado=@();$problemas=@()
    $padrao='(?<attrs>(?:@\w+(?:\((?:[^()"]|"(?:\\.|[^"\\])*")*\))?\s*)*)\b(?:private|protected)\s+(?<mods>(?:(?:static|final|transient|volatile)\s+)*)?(?<java>\w+)\s+(?<campo>\w+)\s*(?:=\s*(?<inicial>[^;]+))?;'
    foreach($m in [regex]::Matches($corpo,$padrao)) {
        if(-not $corpoMascara.Substring($m.Groups['campo'].Index,$m.Groups['campo'].Length).Trim()){continue}
        $attrs=$m.Groups['attrs'].Value;$nome=$m.Groups['campo'].Value;$java=$m.Groups['java'].Value
        if($m.Groups['mods'].Value -match '\b(?:static|transient)\b' -or $attrs -match '@Transient\b'){$ignorado+=$nome;continue}
        $aa=@(V9Anotacoes $attrs);$a=@($aa|Where-Object {$_.nome -in @('Column','JoinColumn')})
        if($a.Count -gt 1){throw 'PARSER_COLUNA_DUPLICADA'};$arg=if($a.Count){$a[0].argumentos}else{''}
        $sqlnome=V9Atributo $arg 'name';if($sqlnome){$sqlnome=V9TextoJava $sqlnome}else{$sqlnome=[regex]::Replace($nome,'[A-Z]',{param($x)'_'+$x.Value.ToLowerInvariant()})}
        $id=$attrs -match '@Id\b';$join=$attrs -match '@JoinColumn\b';$tipo='';$embutido=$attrs -match '@Embedded\b'
        if($join){$tipo='bigint'} elseif($java -in @('long','Long')){$tipo='bigint'} elseif($java -in @('int','Integer')){$tipo='int'} elseif($java -in @('boolean','Boolean')){$tipo='bit'} elseif($java -eq 'Instant'){$tipo='datetime2(6)'} elseif($java -eq 'LocalDate'){$tipo='date'}
        elseif($java -eq 'BigDecimal') {$p=V9Atributo $arg 'precision';$sc=V9Atributo $arg 'scale';if(-not $p -or -not $sc){throw 'PARSER_DECIMAL_SEM_PRECISAO'};$tipo='decimal('+$p+','+$sc+')'}
        elseif($java -eq 'String' -or $attrs -match '@Enumerated\b') {$def=V9Atributo $arg 'columnDefinition';if($def){$tipo=(V9TextoJava $def).ToLowerInvariant()}else{$tam=V9Atributo $arg 'length' '255';$pre=if($attrs -match '@Nationalized\b'){'nvarchar'}else{'varchar'};$tipo=$pre+'('+$tam+')'}}
        elseif(-not $embutido){$problemas+=($nome+'.tipo_nao_suportado')}
        $nulo=-not ($id -or $java -cin @('long','int','boolean') -or (V9Atributo $arg 'nullable' 'true') -eq 'false')
        $mutavel=-not ($id -or (V9Atributo $arg 'updatable' 'true') -eq 'false')
        $campos+=[pscustomobject]@{nome=$sqlnome;campoJava=$nome;java=$java;tipo=$tipo;nulo=$nulo;mutavel=$mutavel;id=$id;identity=($attrs -match 'GenerationType\.IDENTITY');version=($attrs -match '@Version\b');relacao=$join;enumString=($attrs -match '@Enumerated\s*\(\s*EnumType\.STRING\s*\)');enumAnotado=($attrs -match '@Enumerated\b');embedded=$embutido;unique=((V9Atributo $arg 'unique' 'false') -eq 'true');jdbcTimestamp=($attrs -match 'SqlTypes\.TIMESTAMP');inicializador=$m.Groups['inicial'].Value;attrs=$attrs;optionalFalse=($attrs -match 'optional\s*=\s*false')}
        if((V9Atributo $arg 'unique' 'false') -eq 'true'){$unicas+=[pscustomobject]@{nome='';colunas=@($sqlnome);origem='JoinColumn/Column.unique';filtrada=$false}}
    }
    $declarados=@([regex]::Matches($corpoMascara,'\b(?:private|protected)\s+(?!(?:static|transient)\b)(?:final\s+)?\w+\s+(\w+)\s*(?:=[^;]*)?;')|ForEach-Object {$_.Groups[1].Value})
    foreach($declarado in $declarados){if($declarado -cnotin @($campos.campoJava) -and $declarado -cnotin $ignorado){$problemas+=($declarado+'.campo_nao_parseado')}}
    if(@($campos.nome|Group-Object|Where-Object {$_.Count -gt 1}).Count){$problemas+='coluna_duplicada'}
    [pscustomobject]@{classe=$classe.Groups['nome'].Value;base=$classe.Groups['base'].Value;tabela=$tabela;schema=$schema;mappedSuperclass=($s -match '@MappedSuperclass\b');embeddable=($s -match '@Embeddable\b');campos=$campos;unicas=$unicas;problemas=$problemas}
}
function V9Metodos([string]$texto) {
    $s=V9SemComentarios $texto;$mascara=V9MascaraLiterais $s;$ret=@()
    foreach($m in [regex]::Matches($mascara,'(?m)^\s*(?:public|private|protected)\s+(?:static\s+)?[\w.<>,?\[\] ]+\s+(?<nome>\w+)\s*\(')) {
        $args=V9Delimitado $s ($m.Index+$m.Length-1);$r=[regex]::Match($s.Substring($args.fim+1),'^\s*(?:throws [\w., ]+)?\s*\{');if(-not $r.Success){continue}
        $aberto=$args.fim+1+$r.Length-1;$corpo=V9Delimitado $s $aberto;$params=@()
        foreach($a in $args.partes|Where-Object {$_}){$pm=[regex]::Match($a,'(?<tipo>\w+(?:\.\w+)*)\s+(?<nome>\w+)\s*$');if(-not $pm.Success){throw 'PARSER_PARAMETRO_COMPLEXO'};$params+=[pscustomobject]@{tipo=$pm.Groups['tipo'].Value;nome=$pm.Groups['nome'].Value}}
        $ret+=[pscustomobject]@{nome=$m.Groups['nome'].Value;inicio=$m.Index;aberto=$aberto;fim=$corpo.fim;corpo=$corpo.corpo;parametros=$params}
    }
    $ret
}
function V9Chamadas([string]$texto,[string]$padrao) {
    $s=V9SemComentarios $texto;$mascara=V9MascaraLiterais $s;$ret=@()
    foreach($m in [regex]::Matches($mascara,$padrao+'\s*\(')) {$d=V9Delimitado $s ($m.Index+$m.Length-1);$ret+=[pscustomobject]@{indice=$m.Index;linha=([regex]::Matches($s.Substring(0,$m.Index),'\n').Count+1);argumentos=$d.partes;fim=$d.fim}}
    $ret
}
function V9ResolverLiteral([string]$fonte,[object[]]$metodos,[string]$expr,[int]$indice,$enums,[string[]]$pilha=@()) {
    $e=$expr.Trim();$direto=[regex]::Match($e,'^"([A-Z][A-Z0-9_]*)"$');if($direto.Success){return @($direto.Groups[1].Value)}
    $ternario=[regex]::Match($e,'^[^?;]+\?\s*"([A-Z][A-Z0-9_]*)"\s*:\s*"([A-Z][A-Z0-9_]*)"$');if($ternario.Success){return @($ternario.Groups[1].Value,$ternario.Groups[2].Value)}
    $metodo=@($metodos|Where-Object {$indice -gt $_.aberto -and $indice -lt $_.fim}|Sort-Object aberto -Descending|Select-Object -First 1)
    if($metodo.Count -ne 1){throw 'PARSER_LITERAL_FORA_METODO'};$f=$metodo[0]
    if($pilha.Count -ge 8){throw 'PARSER_REPASSE_LIMITE'}
    $enum=[regex]::Match($e,'^(\w+)\.name\(\)$')
    if($enum.Success){$param=@($f.parametros|Where-Object {$_.nome -ceq $enum.Groups[1].Value});if($param.Count -eq 1 -and $enums.ContainsKey($param[0].tipo)){return @($enums[$param[0].tipo])};throw 'PARSER_ENUM_REPASSE_NAO_RESOLVIDO'}
    if($e -notmatch '^\w+$'){throw 'PARSER_LITERAL_NAO_RESOLVIDO'}
    $chave=$f.nome+'.'+$e;if($chave -cin $pilha){throw 'PARSER_REPASSE_CICLICO'};$novaPilha=@($pilha)+@($chave)
    $locais=@([regex]::Matches($f.corpo,'\bString\s+'+[regex]::Escape($e)+'\s*=\s*(?<valor>[^;]+);'))
    if($locais.Count -eq 1){if([regex]::Matches($f.corpo,'\b'+[regex]::Escape($e)+'\s*(?:=|\+=|\+\+)').Count -ne 1){throw 'PARSER_REATRIBUICAO_LITERAL'};return @(V9ResolverLiteral $fonte $metodos $locais[0].Groups['valor'].Value $indice $enums $novaPilha)}
    $pos=-1;for($i=0;$i -lt $f.parametros.Count;$i++){if($f.parametros[$i].nome -ceq $e){$pos=$i}}
    if($pos -lt 0){throw 'PARSER_LITERAL_NAO_RESOLVIDO'}
    if(@($metodos|Where-Object {$_.nome -ceq $f.nome}).Count -ne 1){throw 'PARSER_REPASSE_OVERLOAD'}
    $ret=@();$chamadas=@(V9Chamadas $fonte ('(?<![\w.])'+[regex]::Escape($f.nome)))
    foreach($ch in $chamadas){if(@($metodos|Where-Object {$ch.indice -gt $_.aberto -and $ch.indice -lt $_.fim}).Count -eq 0){continue};if($ch.argumentos.Count -ne $f.parametros.Count){throw 'PARSER_REPASSE_ARIDADE'};$ret+=@(V9ResolverLiteral $fonte $metodos $ch.argumentos[$pos] $ch.indice $enums $novaPilha)}
    if(-not $ret.Count){throw 'PARSER_REPASSE_SEM_ORIGEM'};@($ret|Sort-Object -Unique)
}
function V9ManifestoTexto([string]$texto) {
    $mapa=@{};$cabecalho=''
    foreach($linha in ($texto -split '\r?\n')) {
        if(-not $linha.Trim() -or $linha.StartsWith('#')){continue}
        $m=[regex]::Match($linha,'^(?<hash>[A-Fa-f0-9]{64})\s+\*?(?<arquivo>.+?)\s*$')
        if(-not $m.Success){if(-not $cabecalho -and $mapa.Count -eq 0 -and $linha -match '^D19 .+SHA-256$'){$cabecalho=$linha;continue};throw 'BLOQUEADO_MANIFESTO_FORMATO'}
        $nome=$m.Groups['arquivo'].Value.Replace('\','/');if($nome -match '^src/|^pom.xml$|^README.md$'){$nome='backend/'+$nome}
        if($nome -notmatch '^(?:backend/|docs/)' -or $nome -match '(?:^|/)\.\.(?:/|$)|:|^/'){throw 'BLOQUEADO_MANIFESTO_ESCOPO'}
        if($mapa.ContainsKey($nome)){throw 'BLOQUEADO_MANIFESTO_DUPLICADO'};$mapa[$nome]=$m.Groups['hash'].Value.ToUpperInvariant()
    }
    if(-not $mapa.Count){throw 'BLOQUEADO_MANIFESTO_VAZIO'}
    [pscustomobject]@{mapa=$mapa;cabecalho=$cabecalho}
}
function V9Freeze([string]$raiz,[string]$relativo) {
    if(-not $relativo -or $relativo -notmatch '^backend[/\\]evidencias[/\\][^/\\]+\.sha256$'){throw 'BLOQUEADO_FREEZE_EXPLICITO_NECESSARIO'}
    $p=[IO.Path]::GetFullPath((Join-Path $raiz $relativo));if(-not $p.StartsWith($raiz+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'BLOQUEADO_FREEZE_FORA_RAIZ'}
    if(-not (Test-Path -LiteralPath $p -PathType Leaf)){throw 'BLOQUEADO_FREEZE_AUSENTE'}
    $hash=(Get-FileHash -LiteralPath $p -Algorithm SHA256 -ErrorAction Stop).Hash
    $manifesto=V9ManifestoTexto (Get-Content -LiteralPath $p -Raw -Encoding UTF8 -ErrorAction Stop)
    if((Get-FileHash -LiteralPath $p -Algorithm SHA256 -ErrorAction Stop).Hash -cne $hash){throw 'BLOQUEADO_MANIFESTO_MUDOU'}
    [pscustomobject]@{arquivo=$relativo;sha256=$hash;mapa=$manifesto.mapa;cabecalho=$manifesto.cabecalho;fontes=(New-Object System.Collections.Generic.List[object])}
}
function V9LerCongelado([string]$raiz,$freeze,[string]$relativo) {
    if($relativo -notmatch '^backend/src/(?:main|test)/java/[\w/-]+\.java$|^docs/\d+[^/]*\.md$'){throw 'BLOQUEADO_LEITURA_FORA_FONTES'}
    if(-not $freeze.mapa.ContainsKey($relativo)){throw 'BLOQUEADO_FONTE_FORA_MANIFESTO'}
    $p=Join-Path $raiz $relativo;if(-not (Test-Path -LiteralPath $p -PathType Leaf)){throw 'BLOQUEADO_FONTE_AUSENTE'}
    $antes=(Get-FileHash -LiteralPath $p -Algorithm SHA256 -ErrorAction Stop).Hash;if($antes -cne $freeze.mapa[$relativo]){throw 'BLOQUEADO_HASH_FREEZE_DIVERGENTE'}
    $s=Get-Content -LiteralPath $p -Raw -Encoding UTF8 -ErrorAction Stop;$depois=(Get-FileHash -LiteralPath $p -Algorithm SHA256 -ErrorAction Stop).Hash
    if($antes -cne $depois){throw 'BLOQUEADO_FONTE_MUDOU_AO_LER'}
    if(-not @($freeze.fontes|Where-Object {$_.arquivo -ceq $relativo}).Count){$freeze.fontes.Add([pscustomobject]@{arquivo=$relativo;antes=$antes;depois=$depois})}
    $s
}
