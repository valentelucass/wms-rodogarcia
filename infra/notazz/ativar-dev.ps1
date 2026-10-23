[CmdletBinding()]
param([ValidateSet('Plan','Apply')][string]$Modo='Plan',[string]$GuardaAntes,[string]$GuardaSha256)
# Implementação fiscal autorizada em 23/10/2026. Somente WMS_DEV, V15 e direitos mínimos; sem dados de negócio/PROD.
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $root 'database/scripts/d21-flyway.ps1')
. (Join-Path $root 'database/scripts/trava-preparo.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
$out=Join-Path $root ('orchestracao/.runtime/notazz-implementation-20261023/apply-'+[Guid]::NewGuid().ToString('N')+'.json')
$report=[ordered]@{demanda='BE10-NOTAZZ-IMP01';modo=$Modo;inicioUtc=[DateTime]::UtcNow.ToString('o');banco='WMS_DEV';estado='PREPARO';fase='FONTES';migrationAplicada=$false;grantsAplicados=$false;adminAberturas=0;prod=$false;dadosNegocioAlterados=$false;etapas=@()}
$connection=$null;$credential=$null;$transaction=$null;$databaseLease=$null
function Assert-Nfe([bool]$Ok,[string]$Code){if(-not $Ok){throw ('NOTAZZ_'+$Code)}}
function Same-NfeRows($A,$B,[string[]]$Fields){
    $left=@($A|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    $right=@($B|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    ($left.Count -eq $right.Count) -and (($left -join "`n") -ceq ($right -join "`n"))
}
function Select-Nfe([string]$Sql){
    Assert-Nfe ($Sql.TrimStart() -cmatch '^SELECT\b' -and $Sql -notmatch ';\s*\S') 'SELECT_INVALIDO'
    $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
    if($transaction){$cmd.Transaction=$transaction}
    $reader=$null;$rows=New-Object 'Collections.Generic.List[object]'
    try{$reader=$cmd.ExecuteReader();while($reader.Read()){
        $row=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}}
        $rows.Add([pscustomobject]$row)
    };$rows.ToArray()}finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
$historySql='SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank'
$rightsSql="SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID(N'WMSDEV')"
$catalogSql="SELECT (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') tabelas,(SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') colunas,(SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) checksInvalidos,(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) fksInvalidas,(SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) indicesDesabilitados"
$hf=@('installed_rank','version','type','script','checksum','success');$rf=@('class_desc','esquema','objeto','minor_id','coluna','permission_name','state_desc');$cf=@('tabelas','colunas','checksInvalidos','fksInvalidas','indicesDesabilitados')
try{
    Assert-Nfe ($GuardaAntes -and (Get-FileHash -LiteralPath $GuardaAntes).Hash -ceq $GuardaSha256) 'GUARDA_HASH_INVALIDO'
    $pre=Get-Content -LiteralPath $GuardaAntes -Raw -Encoding UTF8|ConvertFrom-Json
    Assert-Nfe ($pre.guardaRealAprovada -and $pre.alvo.banco -ceq 'WMS_DEV' -and $pre.alvo.login -ceq 'WMSDEV' -and $pre.aberturaTentativas -eq 1) 'GUARDA_INVALIDA'
    Assert-Nfe (([DateTime]::UtcNow-[DateTime]::Parse($pre.inicioUtc).ToUniversalTime()).TotalMinutes -lt 45) 'GUARDA_EXPIRADA'
    $before=[pscustomobject]@{historico=$pre.historico;catalogo=@($pre.catalogo);rights=$pre.direitos}
    $migrations=@(Get-WmsDev02Migrations);$sources=@(Get-WmsD21Fontes (Join-Path $root 'database'))
    Assert-Nfe ($migrations.Count -eq 15 -and (Same-NfeRows @($migrations|Where-Object version -ne '15') @($before.historico|Where-Object type -ceq 'SQL') @('version','script','checksum'))) 'FONTES_ANTERIORES_DIVERGENTES'
    $expected=Add-WmsNotazzExpectedContract $before ($migrations|Where-Object version -ceq '15')
    $grants=@(
        foreach($table in (Get-WmsNotazzTables)){
            'GRANT SELECT, INSERT ON OBJECT::wms.['+$table.name+'] TO WMSDEV;'
            if($table.update.Count){'GRANT UPDATE ('+(($table.update|ForEach-Object {'['+$_+']'}) -join ',')+') ON OBJECT::wms.['+$table.name+'] TO WMSDEV;'}
        }
        foreach($table in (Get-WmsNotazzAddedColumns)){'GRANT UPDATE ('+(($table.columns|ForEach-Object {'['+$_+']'}) -join ',')+') ON OBJECT::wms.['+$table.name+'] TO WMSDEV;'}
    ) -join "`n"
    $report.fontes=$migrations;$report.guardaSha256=$GuardaSha256;$report.antes=$before
    $tool=Get-Content (Join-Path $root 'backend/evidencias/login-d32-preparo.json') -Raw|ConvertFrom-Json
    $env:JAVA_HOME=Split-Path (Split-Path $tool.java.path -Parent) -Parent
    Assert-WmsD21Ferramentas (Join-Path $root 'database')
    if($Modo -ceq 'Plan'){$report.estado='PLANO_LOCAL_VALIDADO';return}
    $databaseLease=Enter-WmsDatabasePreparationLock
    $report.trava=Get-WmsDatabasePreparationLockReport $databaseLease
    Assert-Nfe ([bool]$databaseLease.acquired) 'PREPARO_CONCORRENTE'
    $report.fase='ADMIN_PROTEGIDO'
    $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $root '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
    & $module {Import-SqlClient}
    & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $pre.perfilProtegido.certificate
    Assert-Nfe ((Get-FileHash -LiteralPath $pre.perfilProtegido.certificate).Hash -ceq $pre.tls.certificateSha256) 'CERTIFICADO_MUDOU'
    $local=& $module {Get-LocalIdentity}
    Assert-Nfe ($local.pid -eq $pre.tls.proveniencia.reciboPID -and $local.started -ceq $pre.tls.proveniencia.reciboInicioUtc) 'PROCESSO_SQL_MUDOU'
    $credential=Get-ProjetosSqlAdminCredential
    Assert-Nfe ($credential.UserName -ceq 'sa') 'IDENTIDADE_ADMIN_INVALIDA'
    $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
    $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_DEV')
    $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$builder.set_TrustServerCertificate($false)
    $builder.set_ServerCertificate($pre.perfilProtegido.certificate);$builder.set_Pooling($false);$builder.set_ConnectTimeout(30);$builder.set_ConnectRetryCount(0)
    $builder.set_ApplicationName('WMS-BE10-NOTAZZ-IMP01-V15')
    $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('sa',$credential.Password)
    $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
    $report.adminAberturas=1;$connection.Open()
    $identity=@(Select-Nfe "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,(SELECT encrypt_option FROM sys.dm_exec_connections WHERE session_id=@@SPID) criptografado")
    Assert-Nfe ($identity.Count -eq 1 -and $identity[0].banco -ceq 'WMS_DEV' -and $identity[0].login -ceq 'sa' -and $identity[0].servidor -ceq 'ROD-SRVW-001' -and $identity[0].criptografado -ceq 'TRUE') 'IDENTIDADE_REAL_INVALIDA'
    $report.identidadeAdmin=$identity[0]
    Assert-Nfe (Same-NfeRows @(Select-Nfe $historySql) $before.historico $hf) 'HISTORICO_ANTES_DIVERGENTE'
    Assert-Nfe (Same-NfeRows @(Select-Nfe $catalogSql) @($before.catalogo) $cf) 'CATALOGO_ANTES_DIVERGENTE'
    Assert-Nfe (Same-NfeRows @(Select-Nfe $rightsSql) $before.rights.explicit $rf) 'DIREITOS_ANTES_DIVERGENTES'
    $report.constraintsAntes=@(Select-Nfe "SELECT name,definition FROM sys.check_constraints WHERE parent_object_id IN(OBJECT_ID(N'wms.operacao_administrativa'),OBJECT_ID(N'wms.auditoria_cadastro')) ORDER BY name")
    foreach($phase in @('PreValidate','Migrate','PostValidate','Info')){
        $report.fase='FLYWAY_'+$phase
        Assert-WmsD21FontesIguais $sources @(Get-WmsD21Fontes (Join-Path $root 'database'))
        $info=New-WmsD21StartInfo (Join-Path $root 'database') 'WMS_DEV' 'ROD-SRVW-001' $phase
        $info.Arguments=$info.Arguments.Replace(' validate flyway:',' -Dflyway.target=15 -Dmssql-jdbc.version=13.4.0.jre11 validate flyway:')
        $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_TRUSTSTORE']=$pre.perfilProtegido.truststore
        $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_CERTIFICATE_HOST']=$pre.perfilProtegido.certificateHost
        $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL']=(Get-WmsD21InitSql 'WMS_DEV' 'ROD-SRVW-001')+" IF EXISTS(SELECT 1 FROM wms.flyway_schema_history WHERE success=0 OR (type=N'SQL' AND TRY_CONVERT(int,version) NOT BETWEEN 1 AND 15)) THROW 51000,'Histórico fiscal divergente.',1;"
        $result=Invoke-WmsD21Processo $info $credential.Password
        $report.etapas+=@([ordered]@{etapa=$phase;resultado=$result})
        Assert-Nfe ($result.exitCode -eq 0) 'FLYWAY_INTERROMPIDO'
        if($phase -ceq 'Migrate'){$report.migrationAplicada=$true}
    }
    $report.fase='CATALOGO_ESTRUTURAL_COMPLETO'
    $report.adminAberturas++
    $realSchema=Read-WmsD21Schema 'WMS_DEV' 'ROD-SRVW-001' $credential.Password
    $schemaSources=@($sources|ForEach-Object {[pscustomobject]@{arquivo=$_.arquivo;texto=[IO.File]::ReadAllText((Join-Path $root ('database/'+$_.arquivo)))}})
    $expectedSchema=@(Get-WmsD21CatalogoEsperado $schemaSources)
    Assert-WmsD21Catalogo $expectedSchema $realSchema.catalogo
    Assert-WmsD21FontesIguais $sources @(Get-WmsD21Fontes (Join-Path $root 'database'))
    $report.catalogoEstrutural=[ordered]@{conferido=$true;itens=$expectedSchema.Count;tabelas=$realSchema.tabelas;colunas=$realSchema.colunas;dadosNegocioLidos=$false}
    $report.fase='DIREITOS_MINIMOS'
    Assert-Nfe (Same-NfeRows @(Select-Nfe $historySql) $expected.historico $hf) 'HISTORICO_POS_DIVERGENTE'
    Assert-Nfe (Same-NfeRows @(Select-Nfe $catalogSql) @($expected.catalogo) $cf) 'CATALOGO_POS_DIVERGENTE'
    $transaction=$connection.BeginTransaction()
    Assert-Nfe (Same-NfeRows @(Select-Nfe $rightsSql) $before.rights.explicit $rf) 'DIREITOS_MUDARAM'
    $cmd=$connection.CreateCommand();$cmd.Transaction=$transaction;$cmd.CommandTimeout=30
    try{$cmd.CommandText=$grants;$null=$cmd.ExecuteNonQuery()}finally{$cmd.Dispose()}
    Assert-Nfe (Same-NfeRows @(Select-Nfe $rightsSql) $expected.rights.explicit $rf) 'DIREITOS_POS_DIVERGENTES'
    $transaction.Commit();$transaction.Dispose();$transaction=$null;$report.grantsAplicados=$true
    $report.depois=[ordered]@{historico=@(Select-Nfe $historySql);catalogo=@(Select-Nfe $catalogSql);direitos=@(Select-Nfe $rightsSql)}
    $report.estado='V15_E_DIREITOS_DEV_CONFERIDOS'
}catch{
    $report.estado='INTERROMPIDO';$ex=$_.Exception;$code='NOTAZZ_FALHA';$number=$null
    while($ex){if($ex.Message -cmatch '^(NOTAZZ|D21|SQL)_[A-Z0-9_]+$'){$code=$ex.Message};if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException}
    $report.erro=[ordered]@{codigo=$code;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber}
}finally{
    if($transaction){try{$transaction.Rollback()}finally{$transaction.Dispose()}}
    if($connection){$connection.Dispose()};if($credential){$credential.Password.Dispose()}
    Exit-WmsDatabasePreparationLock $databaseLease
    $report.fimUtc=[DateTime]::UtcNow.ToString('o')
    [IO.File]::WriteAllText($out,($report|ConvertTo-Json -Depth 24),[Text.UTF8Encoding]::new($false))
    [pscustomobject]@{estado=$report.estado;fase=$report.fase;recibo=$out;erro=$report.erro}|ConvertTo-Json -Depth 4
}
if($report.estado -ceq 'INTERROMPIDO'){exit 20}
