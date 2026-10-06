[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$db=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));$raiz=Split-Path $db -Parent
foreach($p in @('testes','console-fixtures','pacote')){
    $r=Get-Content -LiteralPath (Join-Path $db ('evidencias/d22-'+$p+'-final.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    $e=Get-Content -LiteralPath (Join-Path $db ('evidencias/d22-'+$p+'-final-execucao.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.aprovados -ne $r.total -or $r.falhas -ne 0 -or $e.exitCode -ne 0 -or $r.sqlExecutado -ne $false){throw 'D22_CHECK_NAO_APROVADO'}
}
foreach($p in @('offline','outro-cwd','fixture','invalido','extra','setup-offline','setup-invalido')){
    $r=Get-Content -LiteralPath (Join-Path $db ('evidencias/d22-bat-final-'+$p+'.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.exitCode -ne $r.esperado -or $r.sqlExecutado -ne $false -or $r.credencialRealLida -ne $false -or $r.normalCaminhoRealExecutado -ne $false){throw 'D22_CMD_NAO_APROVADO'}
}
$meta=Get-Content -LiteralPath (Join-Path $db 'evidencias/d22-provisionamento-metadados-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
if($meta.conteudoLido -ne $false -or $meta.credencialProvisionadaPorAgente -ne $false){throw 'D22_METADADOS_NAO_COMPATIVEIS'}
$snap=Get-Content -LiteralPath (Join-Path $db 'evidencias/d22-snapshot-d21.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($f in $snap.copias){if((Get-FileHash -LiteralPath (Join-Path $raiz $f.copia) -Algorithm SHA256).Hash -cne $f.sha256){throw 'D22_SNAPSHOT_D21_DIVERGENTE'}}
foreach($f in $snap.copias|Where-Object {$_.arquivo -match '(d21-(flyway|guardas|catalogo)\.ps1$|^database/migrations/.*\.sql$|^database/evidencias/.*manifesto|^database/docs/d21-contrato-flyway\.md$)'}){
    if((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -cne $f.sha256){throw 'D22_FONTE_D21_ALTERADA'}
}
$arquivos=@();$excluidos=0
foreach($f in Get-ChildItem -LiteralPath $db -File -Recurse){
    $rel=$f.FullName.Substring($raiz.Length+1).Replace('\','/')
    if($rel -ceq 'database/evidencias/d22-manifesto-final.json'){continue}
    if($f.Extension -ieq '.clixml'){$excluidos++;continue}
    $arquivos+=[pscustomobject]@{arquivo=$rel;bytes=$f.Length;sha256=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash}
}
$m=[pscustomobject]@{natureza='D22_FREEZE_LOCAL_AUTOMATICO_DPAPI_NAO_SQL';utc=[DateTime]::UtcNow.ToString('o');
    estado='PACOTE_LOCAL_CONCLUIDO_PARA_REVISAO_FAROL_VIGIA';execucaoReal='EXECUCAO_AUTOMATICA_PELO_OPERADOR_NAO_EXECUTADA_PELO_AGENTE';
    provisionamentoReal=$meta.estado;provisionamentoEvidencia='database/evidencias/d22-provisionamento-metadados-final.json';
    credencialRealLida=$false;credencialRealProvisionadaPorAgente=$false;normalBatCaminhoRealExecutado=$false;
    endpoint='127.0.0.1:1433';bancos=@('WMS_DEV','WMS_PROD');ordem='DEV_COMPLETO_ANTES_QUALQUER_PROD';
    normal='SEM_SENHA_SEM_CONFIRMACAO_NONINTERACTIVE';setup='CONFIGURADOR_SEPARADO_DPAPI_USUARIO_MAQUINA_ATUAIS';
    fonteUnica='FLYWAY_FILESYSTEM_DINAMICO';latestFixo=$false;v1v9Congeladas=$true;d21Snapshot='database/evidencias/d22-snapshot-d21.json';snapshotCopias=$snap.total;
    helpersContratoD21='PRESERVADOS';sqlExecutado=0;mavenExecutadoPrumo=$false;buildBackendExecutadoPrumo=$false;
    explorerVisualExecutado=$false;grants=0;clean=$false;repair=$false;baselineAutomatico=$false;
    total=$arquivos.Count;algoritmo='SHA256';excluido='proprio manifesto circular e arquivos .clixml ficticios ignorados pelo Git';clixmlFicticiosExcluidos=$excluidos;
    arquivos=@($arquivos|Sort-Object arquivo)}
$m|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $db 'evidencias/d22-manifesto-final.json') -Encoding UTF8
[pscustomobject]@{natureza=$m.natureza;arquivos=$m.total;snapshotCopias=$snap.total;provisionamentoReal=$meta.estado;sqlExecutado=0}|ConvertTo-Json
