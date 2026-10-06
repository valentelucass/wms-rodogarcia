[CmdletBinding()]
param()
$WmsD20Pathcongelard20=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
. (Join-Path $WmsD20Pathcongelard20 'scripts/d20-guardas.ps1')
$raiz=[IO.Path]::GetFullPath((Join-Path $WmsD20Pathcongelard20 '..'))
Assert-WmsD20Freeze $WmsD20Pathcongelard20
$preservacao=Get-Content -LiteralPath (Join-Path $WmsD20Pathcongelard20 'evidencias/d20-organizacao-preservacao.json') -Raw -Encoding UTF8|ConvertFrom-Json
$depara=Get-Content -LiteralPath (Join-Path $WmsD20Pathcongelard20 'evidencias/d20-organizacao-de-para.json') -Raw -Encoding UTF8|ConvertFrom-Json
if($preservacao.totalAntes -ne 489 -or $preservacao.originaisDisponiveis -ne 489 -or @($preservacao.perdidos).Count -ne 0 -or @($depara.movimentos).Count -ne 34){throw 'D20_ORGANIZACAO_PRESERVACAO_INCOMPLETA'}
foreach($m in $depara.movimentos){
    if($m.shaAntes -cne $m.shaAposMovimento -or (Get-FileHash -LiteralPath (Join-Path $raiz $m.depois) -Algorithm SHA256).Hash -cne $m.shaAposAjustes){throw 'D20_MOVIMENTO_OU_AJUSTE_DIVERGENTE'}
}
foreach($nome in @('guardas','parser','pacote','launcher')){
    $r=Get-Content -LiteralPath (Join-Path $WmsD20Pathcongelard20 ('evidencias/d20-'+$nome+'-reorg.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.aprovados -ne $r.total -or @($r.falhas).Count -gt 0 -and $r.falhas -ne 0){throw 'D20_CHECK_LOCAL_NAO_APROVADO'}
}
$a=Get-Content -LiteralPath (Join-Path $WmsD20Pathcongelard20 'evidencias/d20-auditoria-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($b in @(@('offline-final',0),@('outro-cwd',0),@('invalido',2),@('argumento-extra',2))){
    $r=Get-Content -LiteralPath (Join-Path $WmsD20Pathcongelard20 ('evidencias/d20-bat-'+$b[0]+'-reorg.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.exitCode -ne $b[1] -or $r.sqlExecutado -ne $false){throw 'D20_BAT_REAL_NAO_APROVADO'}
}
if(@($a.divergenciasJpa).Count){throw 'D20_AUDITORIA_DIVERGENTE'}
$paths=@('database/README.md','database/migrations/README.md','database/docs/permissoes-minimas.md',
    'database/scripts/migrate.ps1','database/scripts/criar-bancos.ps1','database/scripts/verificar-alvo-criacao.ps1',
    'database/iniciar-bancos.bat','database/scripts/iniciar-bancos.ps1','database/scripts/testar-d20-launcher.ps1',
    'database/scripts/congelar-d20.ps1','database/config/flyway-settings-vazias.xml','database/scripts/leitores/d20-schema-efetivo.ps1',
    'database/criacao/CREATE_WMS_DEV.sql','database/criacao/CREATE_WMS_PROD.sql',
    'database/scripts/criacao/ler-vazio.ps1','database/criacao/verificar-vazio.sql','database/scripts/ensaio-local-d20.ps1',
    'database/ensaios/d20-constraints.sql','database/ensaios/d20-metadados.sql',
    'database/scripts/auditar-d20.ps1','database/scripts/gerar-d20-matrizes.ps1','database/scripts/testar-d20-guardas.ps1',
    'database/scripts/testar-d20-parser.ps1','database/scripts/testar-d20-pacote.ps1','database/docs/permissoes-d20.md','database/docs/indices-d20.md',
    'infra/README.md','infra/configuracao-externa.md','infra/d20-prontidao.md',
    'backend/pom.xml','backend/evidencias/d20-hibernate-update-sql.json','backend/evidencias/d20-auditoria-integracao.md',
    'backend/evidencias/d20-resumo.json','backend/evidencias/d20-maven-guardas.json')
$paths+='infra/organizar-d20-database.ps1'
foreach($dir in @('config','repositories')){
    foreach($f in Get-ChildItem -LiteralPath (Join-Path $raiz ('backend/src/main/java/br/com/rodogarcia/wms/'+$dir)) -File -Filter '*.java'){
        $paths+=$f.FullName.Substring($raiz.Length+1).Replace('\','/')
    }
}
$paths+='backend/src/main/resources/application-sqlserver-dev.properties'
foreach($f in Get-ChildItem -LiteralPath (Join-Path $WmsD20Pathcongelard20 'evidencias') -File -Recurse){
    $rel=$f.FullName.Substring($raiz.Length+1).Replace('\','/')
    if($rel -match '/d20-' -and $rel -cne 'database/evidencias/d20-manifesto-corrente-reorganizacao.json'){$paths+=$rel}
}
foreach($f in Get-ChildItem -LiteralPath $WmsD20Pathcongelard20 -File -Recurse){
    $rel=$f.FullName.Substring($raiz.Length+1).Replace('\','/')
    if($rel -cne 'database/evidencias/d20-manifesto-corrente-reorganizacao.json'){$paths+=$rel}
}
foreach($f in $a.hashesMigrations){$paths+=$f.arquivo}
foreach($f in $a.hashesModels){
    $rel='backend/src/main/java/br/com/rodogarcia/wms/models/'+$f.arquivo
    if((Get-FileHash -LiteralPath (Join-Path $raiz $rel) -Algorithm SHA256).Hash -cne $f.sha256){throw 'D20_MODEL_MUDOU_APOS_AUDITORIA'}
    $paths+=$rel
}
if((Get-FileHash -LiteralPath (Join-Path $raiz 'backend/evidencias/d20-hibernate-update-sql.json') -Algorithm SHA256).Hash -cne $a.sqlHibernateSha256){throw 'D20_SQL_HIBERNATE_MUDOU_APOS_AUDITORIA'}
$arquivos=@()
foreach($rel in @($paths|Sort-Object -Unique)){
    $p=Join-Path $raiz $rel;$f=Get-Item -LiteralPath $p
    $arquivos+=[pscustomobject]@{arquivo=$rel;bytes=$f.Length;sha256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash;
        acesso=if($rel.StartsWith('backend/')){'FONTE_LEITURA_CEDRO'}else{'PACOTE_DATABASE_INFRA'}}
}
$manifesto=[pscustomobject]@{natureza='FREEZE_LOCAL_PARA_REVISAO_FINAL_NAO_SQL_SERVER';utc=[DateTime]::UtcNow.ToString('o');
    algoritmo='SHA256';estado='PACOTE_LOCAL_CONCLUIDO_REVISAO_FINAL_FAROL_VIGIA';
    criacaoReal='EXECUCAO_MANUAL_PELO_OPERADOR_NAO_EXECUTADA_PELO_AGENTE';endpointConfirmado='127.0.0.1:1433';
    conexoesSqlExecutadas=0;migrationsExecutadas=0;grantsExecutados=0;buildExecutadoPrumo=$false;
    organizacao=[pscustomobject]@{justificativa='Pedido expresso Lucas: raiz com README curto e BAT; PS1 em scripts, documentos tecnicos em docs, XML em config, caminhos relativos atualizados';movimentos=34;originaisDisponiveis=489;perdidos=0;migrationsPreservadas=9;historicosPreservados=$true;dePara='database/evidencias/d20-organizacao-de-para.json';checks='database/evidencias/d20-reorganizacao-execucoes.json'};
    excluidos=@('o proprio manifesto para evitar hash circular');total=$arquivos.Count;arquivos=$arquivos}
$manifesto|ConvertTo-Json -Depth 6|Set-Content -LiteralPath (Join-Path $WmsD20Pathcongelard20 'evidencias/d20-manifesto-corrente-reorganizacao.json') -Encoding UTF8
[pscustomobject]@{resultado=$manifesto.natureza;arquivos=$arquivos.Count;migrations=9;divergencias=0}|ConvertTo-Json
