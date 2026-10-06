[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$db=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$raiz=Split-Path $db -Parent
. (Join-Path $PSScriptRoot 'd21-guardas.ps1')
foreach($nome in @('fixtures-final','pacote-final')){
    $r=Get-Content -LiteralPath (Join-Path $db ('evidencias/d21-'+$nome+'.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    $exec=Get-Content -LiteralPath (Join-Path $db ('evidencias/d21-'+$nome+'-execucao.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.aprovados -ne $r.total -or $r.falhas -ne 0 -or $exec.exitCode -ne 0 -or $r.sqlExecutado -ne $false){throw 'D21_CHECK_NAO_APROVADO'}
}
foreach($nome in @('offline','outro-cwd','invalido','argumento-extra')){
    $r=Get-Content -LiteralPath (Join-Path $db ('evidencias/d21-bat-'+$nome+'.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.exitCode -ne $r.esperado -or $r.sqlExecutado -ne $false -or $r.segredoLido -ne $false){throw 'D21_CMD_NAO_APROVADO'}
}
Assert-WmsD21Pom (Join-Path $raiz 'backend/pom.xml')
$src=@(Get-WmsD21Fontes $db)
$arquivos=@()
foreach($f in Get-ChildItem -LiteralPath $db -File -Recurse){
    $rel=$f.FullName.Substring($raiz.Length+1).Replace('\','/')
    if($rel -ceq 'database/evidencias/d21-manifesto-final.json'){continue}
    $arquivos+=[pscustomobject]@{arquivo=$rel;bytes=$f.Length;sha256=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash;acesso='DATABASE_PRUMO'}
}
foreach($p in @('backend/pom.xml','backend/evidencias/d21-contrato-flyway.md','infra/README.md','infra/configuracao-externa.md')){
    $f=Get-Item -LiteralPath (Join-Path $raiz $p)
    $arquivos+=[pscustomobject]@{arquivo=$p;bytes=$f.Length;sha256=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash;acesso=if($p.StartsWith('backend/')){'LEITURA_CEDRO'}else{'INFRA_PRUMO'}}
}
$m=[pscustomobject]@{natureza='D21_FREEZE_LOCAL_BOOTSTRAP_UPGRADE_MANUAL_NAO_SQL';utc=[DateTime]::UtcNow.ToString('o');
    estado='PACOTE_LOCAL_CONCLUIDO_REVISAO_FAROL_VIGIA';criacaoMigrationsReais='EXECUCAO_MANUAL_PELO_OPERADOR_NAO_EXECUTADA_PELO_AGENTE';
    endpoint='127.0.0.1:1433';bancos=@('WMS_DEV','WMS_PROD');ordem='DEV_TODAS_ETAPAS_OK_ANTES_QUALQUER_PROD';
    fonteUnica='FLYWAY_FILESYSTEM_DINAMICO';fontesAtuais=$src;latestFixo=$false;v1v9Congeladas=$true;d20Snapshot='database/evidencias/d21-snapshot-d20.json';
    sqlExecutado=0;mavenExecutadoPrumo=$false;buildBackendExecutadoPrumo=$false;grants=0;clean=$false;repair=$false;baselineAutomatico=$false;
    total=$arquivos.Count;algoritmo='SHA256';excluido='proprio manifesto para evitar hash circular';arquivos=@($arquivos|Sort-Object arquivo)}
$m|ConvertTo-Json -Depth 7|Set-Content -LiteralPath (Join-Path $db 'evidencias/d21-manifesto-final.json') -Encoding UTF8
[pscustomobject]@{natureza=$m.natureza;arquivos=$m.total;fontes=$src.Count;sqlExecutado=0}|ConvertTo-Json
