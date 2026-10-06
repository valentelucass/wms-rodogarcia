[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'leitores/d20-schema-efetivo.ps1')
. (Join-Path $PSScriptRoot 'leitores/v9-java-arquivos.ps1')
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$baseline=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'evidencias/d20-baseline.json') -Raw -Encoding UTF8|ConvertFrom-Json
$fontes=@();$hashes=@()
foreach($f in $baseline.migrations){
    $p=Join-Path $raiz $f.arquivo;$h=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash
    if($h -cne $f.sha256){throw "D20_FREEZE_ALTERADO_$($f.arquivo)"}
    $fontes+=[pscustomobject]@{arquivo=$f.arquivo;texto=[IO.File]::ReadAllText($p)};$hashes+=$f
}
if($fontes.Count -ne 9){throw 'D20_BASELINE_NOVE_MIGRATIONS'}
$sql=D20SchemaEfetivo $fontes
$models=@{};$modelHashes=@()
foreach($f in Get-ChildItem -LiteralPath (Join-Path $raiz 'backend/src/main/java/br/com/rodogarcia/wms/models') -Filter '*.java'){
    $txt=[IO.File]::ReadAllText($f.FullName)
    if((V9SemComentarios $txt) -notmatch '@(?:Entity|Embeddable|MappedSuperclass)\b'){continue}
    $antes=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash
    # Adapter local: leitor D19 nao conhece Table.check (Jakarta 3.2).
    # Preserve leitor historico; extraia e guarde os CHECKs originais separadamente.
    $parse=V9SemComentarios $txt;$jpaChecks=@()
    $anns=@(V9Anotacoes $parse|Where-Object {$_.nome -eq 'Table'})
    foreach($an in $anns){
        foreach($chk in @(V9Anotacoes $an.argumentos|Where-Object {$_.nome -eq 'CheckConstraint'})){
            $jpaChecks+=V9TextoJava (V9Atributo $chk.argumentos 'constraint')
        }
    }
    foreach($an in @(V9Anotacoes $parse|Where-Object {$_.nome -eq 'CheckConstraint'}|Sort-Object inicio -Descending)){
        $parse=$parse.Remove($an.inicio,$an.fim-$an.inicio).Insert($an.inicio,'""')
    }
    # V9Anotacoes pula as annotations internas de Table; substituir nesse argumento tambem.
    foreach($an in @($anns|Sort-Object inicio -Descending)){
        $arg=$an.argumentos
        foreach($chk in @(V9Anotacoes $arg|Where-Object {$_.nome -eq 'CheckConstraint'}|Sort-Object inicio -Descending)){
            $arg=$arg.Remove($chk.inicio,$chk.fim-$chk.inicio).Insert($chk.inicio,'""')
        }
        $parse=$parse.Remove($an.inicio,$an.fim-$an.inicio).Insert($an.inicio,('@Table('+$arg+')'))
    }
    $m=V9ModelTexto $parse
    $m|Add-Member NoteProperty jpaChecks $jpaChecks
    if(@($m.problemas).Count){throw "D20_MODEL_PARSER_$($m.classe): $($m.problemas -join ',')"}
    $m|Add-Member NoteProperty texto $txt;$models[$m.classe]=$m
    $modelHashes+=[pscustomobject]@{arquivo=$f.Name;sha256=$antes}
    if((Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash -cne $antes){throw 'D20_MODEL_MUDOU_DURANTE_LEITURA'}
}
function Resolver($M,[string[]]$Visitados=@()){
    if($M.classe -cin $Visitados){throw 'D20_MODEL_CICLO'}
    $visitados2=$Visitados+@($M.classe);$campos=@()
    if($M.base){if(-not $models.ContainsKey($M.base)){throw 'D20_MODEL_BASE_AUSENTE'};$campos+=@(Resolver $models[$M.base] $visitados2)}
    foreach($c in $M.campos){
        if($c.embedded){if(-not $models.ContainsKey($c.java) -or -not $models[$c.java].embeddable){throw 'D20_MODEL_EMBEDDED_AUSENTE'};$campos+=@(Resolver $models[$c.java] $visitados2)}
        else{$campos+=$c}
    };$campos
}
$hibernatePath=Join-Path $raiz 'backend/evidencias/d20-hibernate-update-sql.json'
$hibernateHash=(Get-FileHash -LiteralPath $hibernatePath -Algorithm SHA256).Hash
$hibernate=Get-Content -LiteralPath $hibernatePath -Raw -Encoding UTF8|ConvertFrom-Json
$div=@();$matriz=@();$colunas=0;$fks=0;$checks=0;$unicos=0;$indices=0;$filtros=0;$updates=0
foreach($t in $sql.tabelas.Values){
    $mm=@($models.Values|Where-Object {$_.tabela -ceq $t.nome})
    if($mm.Count -ne 1){throw "D20_MODEL_TABELA_AUSENTE_OU_DUPLICADO_$($t.nome)"}
    $m=$mm[0];$cc=@(Resolver $m)
    if(@($cc.nome|Group-Object|Where-Object {$_.Count -gt 1}).Count){throw 'D20_MODEL_COLUNA_DUPLICADA'}
    foreach($c in $cc){
        if(-not $t.colunas.Contains($c.nome)){$div+="$($t.nome).$($c.nome).ausente_SQL";continue}
        $s=$t.colunas[$c.nome]
        if($c.tipo -cne $s.tipo){$div+="$($t.nome).$($c.nome).tipo: $($c.tipo)/$($s.tipo)"}
        if($c.nulo -ne $s.nulo){$div+="$($t.nome).$($c.nome).nulo"}
        if($c.identity -ne $s.identity){$div+="$($t.nome).$($c.nome).identity"}
        if($c.relacao){
            $fk=@($t.constraints.Values|Where-Object {$_.tipo -eq 'FK' -and ($_.colunas -join ',') -ceq $c.nome})
            if($fk.Count -ne 1 -or -not $models.ContainsKey($c.java) -or $fk[0].referencia.tabela -cne $models[$c.java].tabela){$div+="$($t.nome).$($c.nome).FK_JPA_divergente"}
        }
    }
    foreach($c in $t.colunas.Keys){if($c -cnotin @($cc.nome)){$div+="$($t.nome).${c}.ausente_JPA"}}
    foreach($u in $m.unicas){
        $chave=(@($u.colunas|Sort-Object) -join ',')
        $eq=@($t.constraints.Values|Where-Object {$_.tipo -in @('PK','UNIQUE') -and (@($_.colunas|Sort-Object) -join ',') -ceq $chave})
        $eqIndice=@($t.indices.Values|Where-Object {$_.unique -and (@($_.colunas|Sort-Object) -join ',') -ceq $chave})
        if(-not $eq.Count -and -not $eqIndice.Count){$div+="$($t.nome).UNIQUE_JPA_sem_equivalente:$chave"}
    }
    $colsUpdate=@($cc|Where-Object {$_.mutavel}|ForEach-Object {$_.nome}|Sort-Object)
    $prop=$hibernate.PSObject.Properties[$m.classe];$stmt=if($prop){[string]$prop.Value}else{''};$sqlCols=@()
    if($stmt){
        $match=[regex]::Match($stmt,'^update wms\.'+[regex]::Escape($t.nome)+' set (?<cols>.+?) where id=\?(?: and versao=\?)?$')
        if(-not $match.Success){$div+="$($t.nome).UPDATE_SQL_nao_suportado"}
        else{$sqlCols=@($match.Groups['cols'].Value -split ','|ForEach-Object {$_ -replace '=\?$',''}|Sort-Object);$updates++}
    }
    if(($sqlCols -join ',') -cne ($colsUpdate -join ',')){$div+="$($t.nome).UPDATE_SQL_divergente"}
    if($m.schema -cne 'wms'){$div+="$($t.nome).schema_JPA_divergente"}
    $matriz+=[pscustomobject]@{tabela=$t.nome;classe=$m.classe;schema=$m.schema;
        updateEsperado=@($cc|Where-Object {$_.mutavel}|ForEach-Object {$_.nome}|Sort-Object);
        imutaveis=@($cc|Where-Object {-not $_.mutavel}|ForEach-Object {$_.nome}|Sort-Object);
        versao=@($cc|Where-Object {$_.version}|ForEach-Object {$_.nome});
        dynamicUpdate=($m.texto -match '@DynamicUpdate\b');checksJpa=$m.jpaChecks;
        colunasSqlPreparado=$sqlCols;sqlPreparado=$stmt;sqlObservadoNoServidor=$false}
    $colunas+=$t.colunas.Count;$fks+=@($t.constraints.Values|Where-Object {$_.tipo -eq 'FK'}).Count
    $checks+=@($t.constraints.Values|Where-Object {$_.tipo -eq 'CHECK'}).Count
    $unicos+=@($t.constraints.Values|Where-Object {$_.tipo -eq 'UNIQUE'}).Count
    $indices+=$t.indices.Count;$filtros+=@($t.indices.Values|Where-Object {$_.filtro}).Count
}
[pscustomobject]@{natureza='AUDITORIA_ARQUIVOS_NAO_PROVA_SQL_SERVER';utc=[DateTime]::UtcNow.ToString('o');
    resumo=[pscustomobject]@{migrations=9;tabelas=$sql.tabelas.Count;colunas=$colunas;fks=$fks;checksEfetivos=$checks;
        uniques=$unicos;indicesExplicitos=$indices;filtrados=$filtros;alteracoes=$sql.alteracoes.Count;dmlTecnico=$sql.dml.Count;updatesHibernatePreparados=$updates};
    divergenciasJpa=$div;matrizUpdate=$matriz;schema=$sql;hashesMigrations=$hashes;hashesModels=$modelHashes;
    sqlHibernateSha256=$hibernateHash}|ConvertTo-Json -Depth 30
if((Get-FileHash -LiteralPath $hibernatePath -Algorithm SHA256).Hash -cne $hibernateHash){throw 'D20_SQL_PREPARADO_MUDOU_DURANTE_LEITURA'}
if($div.Count){exit 1}
