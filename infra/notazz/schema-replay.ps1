# Replay estrutural local. Flyway continua executando a migration original, sem conversão.
function Convert-WmsNotazzSchemaReplay($Fontes) {
    $items=@($Fontes|Where-Object {[IO.Path]::GetFileName($_.arquivo) -ceq 'V15__emissao_fiscal_notazz.sql'})
    if($items.Count -ne 1){throw 'D21_NOTAZZ_SOURCE_INVALID'}
    $source=$items[0];$hasher=[Security.Cryptography.SHA256]::Create()
    try{$sha=([BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($source.texto)))).Replace('-','')}finally{$hasher.Dispose()}
    if($sha -cne 'CB4B75D57B07A9CC306CD26FD5B006D48A5C1F846F49E1952B90F4DEA7A62D6F'){throw 'D21_NOTAZZ_SOURCE_INVALID'}
    foreach($f in $Fontes){
        if($f -eq $source){[pscustomobject]@{arquivo=$f.arquivo;texto=$f.texto.Replace('BEGIN TRANSACTION;','').Replace('COMMIT TRANSACTION;','').Replace('id bigint IDENTITY NOT NULL','id bigint IDENTITY(1,1) NOT NULL')}}else{$f}
    }
}
