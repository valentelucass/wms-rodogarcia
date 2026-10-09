[CmdletBinding()]
param([ValidateSet('Plan','Apply')][string]$Modo='Plan')

# D32-DEV04: somente WMS_DEV, V11 e direitos minimos. Sem bootstrap DEV -> PROD.
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'd21-flyway.ps1')
. (Join-Path $taskRoot 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $taskRoot 'infra/auth/contrato-banco.ps1')
$taskOutput=Join-Path $taskRoot ('orchestracao/.runtime/login-d32/ativacao-'+[Guid]::NewGuid().ToString('N')+'.json')
$report=[ordered]@{natureza='D32_DEV04_ATIVACAO_LOGIN';modo=$Modo;inicioUtc=[DateTime]::UtcNow.ToString('o');banco='WMS_DEV';estado='PREPARO';fase='FONTES';prod=$false;grantsAplicados=$false;migrationAplicada=$false;etapas=@()}
$connection=$null;$credential=$null;$transaction=$null

function Assert-D32([bool]$Ok,[string]$Code) { if(-not $Ok){throw ('D32_'+$Code)} }
function Test-D32Rows($A,$B,[string[]]$Fields) {
    $left=@($A|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    $right=@($B|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    ($left.Count -eq $right.Count) -and (($left -join "`n") -ceq ($right -join "`n"))
}
function Select-D32([string]$Sql) {
    Assert-D32 ($Sql.TrimStart() -cmatch '^SELECT\b' -and $Sql -notmatch ';\s*\S') 'SELECT_INVALIDO'
    $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
    if($transaction){$cmd.Transaction=$transaction}
    $reader=$null;$rows=New-Object 'Collections.Generic.List[object]'
    try {
        $reader=$cmd.ExecuteReader()
        while($reader.Read()){
            $row=[ordered]@{}
            for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}}
            $rows.Add([pscustomobject]$row)
        }
        $rows.ToArray()
    } finally {if($reader){$reader.Dispose()};$cmd.Dispose()}
}
$historySql='SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank'
$rightsSql="SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID(N'WMSDEV')"
$catalogSql="SELECT (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') tabelas,(SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') colunas,(SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) checksInvalidos,(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) fksInvalidas,(SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) indicesDesabilitados"
$historyFields=@('installed_rank','version','type','script','checksum','success')
$rightsFields=@('class_desc','esquema','objeto','minor_id','coluna','permission_name','state_desc')
$catalogFields=@('tabelas','colunas','checksInvalidos','fksInvalidas','indicesDesabilitados')
try {
    $baselinePath=Join-Path $taskRoot 'orchestracao/.runtime/d29-farol-retomada-preflight.json'
    Assert-D32 ((Get-FileHash $baselinePath).Hash -ceq 'C22AABF54F839D4B6E139A598A8478530F92F8911FEEE46BB2F92BC85313EA90') 'BASELINE_DIVERGENTE'
    $baseline=Get-Content $baselinePath -Raw -Encoding UTF8|ConvertFrom-Json
    $sources=@(Get-WmsD21Fontes (Join-Path $taskRoot 'database'))
    $migrations=@(Get-WmsDev02Migrations)
    Assert-D32 ($migrations.Count -eq 11) 'MIGRATIONS_INESPERADAS'
    Assert-D32 (Test-D32Rows @($migrations|Where-Object {$_.version -ne '11'}) @($baseline.historico|Where-Object {$_.type -ceq 'SQL'}) @('version','script','checksum')) 'HISTORICO_FONTES_DIVERGENTE'
    $v11=@($migrations|Where-Object {$_.version -ceq '11'})[0]
    Assert-D32 ($v11.sha256 -ceq 'A75B295811BD4CE8C80344AFA8BEA48509B63671D6A4A532DB829EAEF6E91289') 'V11_NAO_REVISADA'
    $expected=Add-WmsAuthExpectedContract $baseline $v11
    $grantsPath=Join-Path $taskRoot 'database/contratos/D32-permissoes-login-dev.sql'
    $grantsHash=(Get-FileHash $grantsPath).Hash
    $report.fontes=$migrations;$report.permissoesSha256=$grantsHash
    $env:JAVA_HOME='C:\Users\suporte\AppData\Local\Programs\Eclipse Adoptium\jdk-21'
    Assert-WmsD21Ferramentas (Join-Path $taskRoot 'database')
    if($Modo -eq 'Plan'){$report.estado='PLANO_LOCAL_VALIDADO';return}

    $report.fase='ABERTURA_ADMIN_PROTEGIDA'
    $profile=Get-ProjetosSqlProfile
    Assert-D32 ($profile.server -ceq 'ROD-SRVW-001' -and $profile.host -ceq '127.0.0.1' -and $profile.port -eq 1433) 'PERFIL_INVALIDO'
    $credential=Get-ProjetosSqlAdminCredential
    Assert-D32 ($credential -is [Management.Automation.PSCredential] -and $credential.UserName -ceq 'sa') 'CREDENCIAL_INVALIDA'
    $connection=New-ProjetosSqlConnection -Database WMS_DEV -Credential $credential -ApplicationName 'WMS-D32-DEV04-ATIVACAO'
    $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder($connection.ConnectionString)
    Assert-D32 ($builder.DataSource -ceq 'tcp:127.0.0.1,1433' -and $builder.InitialCatalog -ceq 'WMS_DEV' -and -not $builder.TrustServerCertificate -and $builder.Encrypt -eq [Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory -and $builder.ServerCertificate -and -not $builder.Pooling) 'CONEXAO_INVALIDA'
    $builder.set_ConnectTimeout(30);$builder.set_ConnectRetryCount(0);$connection.ConnectionString=$builder.ConnectionString
    $connection.Open()
    $report.fase='IDENTIDADE_CATALOGO_HISTORICO'
    $identity=@(Select-D32 "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,(SELECT state_desc FROM sys.databases WHERE database_id=DB_ID()) estado,(SELECT encrypt_option FROM sys.dm_exec_connections WHERE session_id=@@SPID) criptografado")
    Assert-D32 ($identity.Count -eq 1 -and $identity[0].banco -ceq 'WMS_DEV' -and $identity[0].login -ceq 'sa' -and $identity[0].servidor -ceq 'ROD-SRVW-001' -and $identity[0].estado -ceq 'ONLINE' -and $identity[0].criptografado -ceq 'TRUE') 'IDENTIDADE_REAL_INVALIDA'
    $report.identidade=$identity[0]
    $history=@(Select-D32 $historySql);$catalog=@(Select-D32 $catalogSql);$rights=@(Select-D32 $rightsSql)
    $alreadyMigrated=Test-D32Rows $history $expected.historico $historyFields
    Assert-D32 ($alreadyMigrated -or (Test-D32Rows $history $baseline.historico $historyFields)) 'HISTORICO_REAL_DIVERGENTE'
    $expectedCatalog=if($alreadyMigrated){$expected.catalogo}else{$baseline.catalogo}
    Assert-D32 (Test-D32Rows $catalog @($expectedCatalog) $catalogFields) 'CATALOGO_DIVERGENTE'
    $alreadyGranted=Test-D32Rows $rights $expected.rights.explicit $rightsFields
    Assert-D32 ($alreadyGranted -or (Test-D32Rows $rights $baseline.rights.explicit $rightsFields)) 'PERMISSOES_PREVIAS_DIVERGENTES'
    $report.antes=[ordered]@{historico=$history;catalogo=$catalog;direitos=$rights}

    if(-not $alreadyMigrated){
        foreach($phase in @('PreValidate','Migrate','PostValidate','Info')){
            $report.fase='FLYWAY_'+$phase
            Assert-WmsD21FontesIguais $sources @(Get-WmsD21Fontes (Join-Path $taskRoot 'database'))
            $info=New-WmsD21StartInfo (Join-Path $taskRoot 'database') 'WMS_DEV' $profile.server $phase
            $info.Arguments=$info.Arguments.Replace(' validate flyway:', ' -Dflyway.target=11 -Dmssql-jdbc.version=13.4.0.jre11 validate flyway:')
            $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_TRUSTSTORE']=$profile.truststore
            $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_CERTIFICATE_HOST']=$profile.certificateHost
            $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL']=(Get-WmsD21InitSql 'WMS_DEV' $profile.server)+" IF EXISTS(SELECT 1 FROM wms.flyway_schema_history WHERE success=0 OR (type=N'SQL' AND version NOT IN(N'1',N'2',N'3',N'4',N'5',N'6',N'7',N'8',N'9',N'10',N'11'))) THROW 51032,'D32 historico divergente.',1;"
            $result=Invoke-WmsD21Processo $info $credential.Password
            $report.etapas+=@([ordered]@{etapa=$phase;resultado=$result})
            Assert-D32 ($result.exitCode -eq 0) 'FLYWAY_INTERROMPIDO'
            if($phase -eq 'Migrate'){$report.migrationAplicada=$true}
        }
    }
    $report.fase='CONFERENCIA_V11'
    $history=@(Select-D32 $historySql);$catalog=@(Select-D32 $catalogSql)
    Assert-D32 (Test-D32Rows $history $expected.historico $historyFields) 'HISTORICO_POS_DIVERGENTE'
    Assert-D32 (Test-D32Rows $catalog @($expected.catalogo) $catalogFields) 'CATALOGO_POS_DIVERGENTE'
    $report.fase='DIREITOS_MINIMOS'
    if(-not $alreadyGranted){
        Assert-D32 ((Get-FileHash $grantsPath).Hash -ceq $grantsHash) 'CONTRATO_DIREITOS_MUDOU'
        $transaction=$connection.BeginTransaction()
        Assert-D32 (Test-D32Rows @(Select-D32 $rightsSql) $baseline.rights.explicit $rightsFields) 'PERMISSOES_MUDARAM'
        $cmd=$connection.CreateCommand();$cmd.Transaction=$transaction;$cmd.CommandTimeout=30
        try{$cmd.CommandText=[IO.File]::ReadAllText($grantsPath);$null=$cmd.ExecuteNonQuery()}finally{$cmd.Dispose()}
        Assert-D32 (Test-D32Rows @(Select-D32 $rightsSql) $expected.rights.explicit $rightsFields) 'DIREITOS_POS_DIVERGENTES'
        $transaction.Commit();$transaction.Dispose();$transaction=$null;$report.grantsAplicados=$true
    }
    $report.depois=[ordered]@{historico=$history;catalogo=$catalog;direitos=@(Select-D32 $rightsSql)}
    Assert-WmsD21FontesIguais $sources @(Get-WmsD21Fontes (Join-Path $taskRoot 'database'))
    $report.estado='V11_E_DIREITOS_DEV_CONFERIDOS'
} catch {
    $report.estado='INTERROMPIDO';$ex=$_.Exception;$code='D32_FALHA';$number=$null
    while($ex){if($ex.Message -cmatch '^(D32|D21|SQL)_[A-Z0-9_]+$'){$code=$ex.Message};if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException}
    $report.erro=[ordered]@{codigo=$code;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber}
} finally {
    if($transaction){try{$transaction.Rollback()}finally{$transaction.Dispose()}}
    if($connection){$connection.Dispose()};if($credential){$credential.Password.Dispose()}
    $report.fimUtc=[DateTime]::UtcNow.ToString('o')
    [IO.File]::WriteAllText($taskOutput,($report|ConvertTo-Json -Depth 15),[Text.UTF8Encoding]::new($false))
    [pscustomobject]@{estado=$report.estado;fase=$report.fase;recibo=$taskOutput;erro=$report.erro}|ConvertTo-Json -Depth 4
}
if($report.estado -eq 'INTERROMPIDO'){exit 20}
