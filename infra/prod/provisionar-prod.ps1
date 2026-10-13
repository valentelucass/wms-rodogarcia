[CmdletBinding()]
param([ValidateSet('Inspect','Apply')][string]$Mode='Inspect')
# Autorizacao expressa de Lucas em 13/10/2026: somente WMS_PROD/WMSPROD.
$ErrorActionPreference='Stop'
$taskRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $taskRoot 'backend/scripts/prod/credential-prod.ps1')
. (Join-Path $PSScriptRoot 'guarda-wmsprod.ps1')
$report=[ordered]@{natureza='PROD_PROVISIONAMENTO_AUTORIZADO';modo=$Mode;inicioUtc=[DateTime]::UtcNow.ToString('o');estado='PREPARO';fase='TLS_LOCAL';aberturas=0;alteracoes=@();DEVAlterado=$false;outrosAcessosAlterados=$false}
$output=Join-Path $taskRoot ('orchestracao/.runtime/prod-provisionamento-'+[Guid]::NewGuid().ToString('N')+'.json')
$connection=$null;$credential=$null;$certificate=$null;$transaction=$null
function Assert-Provision([bool]$Ok,[string]$Code){if(-not $Ok){throw ('PROD_PROVISION_'+$Code)}}
function Read-Provision([string]$Sql){
    $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
    if($transaction){$cmd.Transaction=$transaction}
    $reader=$null;$rows=New-Object 'Collections.Generic.List[object]'
    try{$reader=$cmd.ExecuteReader();while($reader.Read()){$row=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}};$rows.Add([pscustomobject]$row)};$rows.ToArray()}
    finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
function Compare-Provision($A,$B,[string[]]$Fields){
    $a1=@($A|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    $b1=@($B|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    ($a1.Count -eq $b1.Count) -and (($a1 -join "`n") -ceq ($b1 -join "`n"))
}
function Write-Provision([string]$Sql){
    $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
    if($transaction){$cmd.Transaction=$transaction}
    try{$null=$cmd.ExecuteNonQuery()}finally{$cmd.Dispose()}
}
try{
    $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $taskRoot '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
    & $module {Import-SqlClient}
    $tlsRoot=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer'
    $tlsPath=Join-Path $tlsRoot 'tls.json'
    & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tlsPath
    $tls=Get-Content -LiteralPath $tlsPath -Raw -Encoding UTF8|ConvertFrom-Json
    $before=& $module {Get-LocalIdentity}
    Assert-Provision ($tls.pid -eq $before.pid -and $tls.started -ceq $before.started) 'TLS_OBSOLETO'
    Assert-Provision ($tls.id -cmatch '^[a-f0-9]{32}$' -and $tls.certificate -ceq (Join-Path $tlsRoot ('sql-public-'+$tls.id+'.pem')) -and $tls.truststore -ceq ($tls.certificate -replace '\.pem$','.p12')) 'TLS_CAMINHO'
    foreach($path in @($tls.certificate,$tls.truststore)){& $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $path}
    Assert-Provision ((Get-FileHash $tls.certificate).Hash -ceq $tls.fileSHA256 -and (Get-FileHash $tls.truststore).Hash -ceq $tls.truststoreSHA256) 'TLS_HASH'
    $certificate=New-Object Security.Cryptography.X509Certificates.X509Certificate2($tls.certificate)
    $hasher=[Security.Cryptography.SHA256]::Create()
    try{$der=([BitConverter]::ToString($hasher.ComputeHash($certificate.RawData))).Replace('-','').ToLowerInvariant()}finally{$hasher.Dispose()}
    Assert-Provision ($der -ceq $tls.certificateSHA256 -and [DateTime]::UtcNow -ge $certificate.NotBefore.ToUniversalTime() -and [DateTime]::UtcNow -lt $certificate.NotAfter.ToUniversalTime()) 'TLS_VALIDADE'
    $report.tls=[ordered]@{pid=$before.pid;inicio=$before.started;certificadoSha256=$tls.fileSHA256;encrypt='Mandatory';trustServerCertificate=$false;recaptura=$false}
    # Loader administrativo existente: nao chama factory/profile que recaptura TLS.
    & $module {param($p) Assert-PrivateAcl $p} $tlsRoot
    $credential=Get-ProjetosSqlAdminCredential
    Assert-Provision ($credential.UserName -ceq 'sa') 'ADMIN_CANAL'
    $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
    $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_PROD')
    $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$builder.set_TrustServerCertificate($false);$builder.set_ServerCertificate($tls.certificate)
    $builder.set_Pooling($false);$builder.set_ConnectTimeout(30);$builder.set_ConnectRetryCount(0);$builder.set_TransparentNetworkIPResolution($false);$builder.set_ApplicationName('WMS-PROD-PROVISIONAMENTO-AUTORIZADO')
    $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('sa',$credential.Password)
    $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
    $report.fase='ABERTURA_ADMIN';$report.aberturas=1;$connection.Open()
    $after=& $module {Get-LocalIdentity}
    Assert-Provision ($before.pid -eq $after.pid -and $before.started -ceq $after.started) 'PROCESSO_MUDOU'
    $report.fase='IDENTIDADE'
    $identity=@(Read-Provision "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,(SELECT state_desc FROM sys.databases WHERE database_id=DB_ID()) estado,(SELECT encrypt_option FROM sys.dm_exec_connections WHERE session_id=@@SPID) criptografado")
    Assert-Provision ($identity.Count -eq 1 -and $identity[0].banco -ceq 'WMS_PROD' -and $identity[0].login -ceq 'sa' -and $identity[0].servidor -ceq 'ROD-SRVW-001' -and $identity[0].estado -ceq 'ONLINE' -and $identity[0].criptografado -ceq 'TRUE') 'IDENTIDADE'
    $report.identidade=$identity[0];$report.fase='METADADOS'
    $report.historico=@(Read-Provision 'SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank')
    $report.catalogo=@(Read-Provision "SELECT t.name tabela,COUNT(c.column_id) colunas FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') GROUP BY t.name ORDER BY t.name")
    $report.login=@(Read-Provision "SELECT name,type_desc,is_disabled,default_database_name FROM sys.server_principals WHERE name=N'WMSPROD'")
    $report.usuario=@(Read-Provision "SELECT name,type_desc,authentication_type_desc FROM sys.database_principals WHERE name=N'WMSPROD'")
    $report.direitos=@(Read-Provision "SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID(N'WMSPROD')")
    $report.roles=@(Read-Provision "SELECT r.name role FROM sys.database_role_members m JOIN sys.database_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=USER_ID(N'WMSPROD')")
    $report.serverRoles=@(Read-Provision "SELECT r.name role FROM sys.server_role_members m JOIN sys.server_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=SUSER_ID(N'WMSPROD')")
    $report.canalLocalExiste=Test-Path -LiteralPath (Get-WmsProductionCredentialPath)
    if($Mode -ceq 'Apply'){
        $report.fase='VALIDACAO_PREVIA'
        $expected=Get-Content (Join-Path $PSScriptRoot 'contrato-aplicacao.json') -Raw -Encoding UTF8|ConvertFrom-Json
        $migrations=@(Get-WmsProdMigrations)
        Assert-Provision (Compare-Provision $migrations @($expected.historico|Where-Object type -ceq 'SQL') @('version','script','checksum')) 'FONTES_CONTRATO'
        $last=($report.historico|Where-Object type -ceq 'SQL'|Measure-Object -Property installed_rank -Maximum).Maximum
        Assert-Provision ($last -ge 9 -and $last -le 11) 'VERSAO_NAO_SUPORTADA'
        Assert-Provision (Compare-Provision $report.historico @($expected.historico|Where-Object installed_rank -le $last) @('installed_rank','version','type','script','checksum','success')) 'HISTORICO_DIVERGENTE'
        Assert-Provision ($report.roles.Count -eq 0 -and $report.serverRoles.Count -eq 0) 'ACESSO_EXISTENTE_PRIVILEGIADO'
        Assert-Provision (-not $report.login.Count -or $report.canalLocalExiste) 'LOGIN_EXISTENTE_SEM_CANAL_PRESERVADO'
        $sourceDev=[IO.File]::ReadAllText((Join-Path $taskRoot 'database/migrations/V10__permitir_ajuste_estoque_em_fato_permanencia.sql'))
        $sourceProd=[IO.File]::ReadAllText((Join-Path $taskRoot 'database/migrations-prod/V10__permitir_ajuste_estoque_producao.sql'))
        Assert-Provision ($sourceDev.Substring($sourceDev.IndexOf('IF DB_NAME()')).Replace("N'WMS_DEV'","N'WMS_PROD'").Replace('Proposta V10 restrita a WMS_DEV.','V10 exclusiva de WMS_PROD.') -ceq $sourceProd.Substring($sourceProd.IndexOf('IF DB_NAME()'))) 'V10_EQUIVALENCIA'
        $report.fontes=$migrations
        $report.linhasAntes=@(Read-Provision "SELECT t.name tabela,SUM(p.rows) linhas FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1) WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history' GROUP BY t.name ORDER BY t.name")
        if($last -lt 11){
            . (Join-Path $taskRoot 'database/scripts/d21-flyway.ps1')
            $env:JAVA_HOME='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21'
            Assert-WmsD21Ferramentas (Join-Path $taskRoot 'database')
            $staging=Join-Path $taskRoot ('orchestracao/.runtime/prod-migrations-'+[Guid]::NewGuid().ToString('N'))
            $null=New-Item -ItemType Directory -Path $staging
            foreach($m in $migrations){
                $folder=if($m.version -ceq '10'){'database/migrations-prod'}else{'database/migrations'}
                $source=Join-Path (Join-Path $taskRoot $folder) $m.script
                Copy-Item -LiteralPath $source -Destination $staging
                if(Test-Path -LiteralPath ($source+'.conf')){Copy-Item -LiteralPath ($source+'.conf') -Destination $staging}
                Assert-Provision ((Get-FileHash (Join-Path $staging $m.script)).Hash -ceq $m.sha256) 'STAGING_HASH'
            }
            $report.etapasFlyway=@()
            foreach($phase in @('PreValidate','Migrate','PostValidate','Info')){
                $report.fase='FLYWAY_'+$phase
                Assert-Provision (Compare-Provision @(Get-WmsProdMigrations) $migrations @('version','script','sha256')) 'FONTES_MUDARAM'
                $info=New-WmsD21StartInfo (Join-Path $taskRoot 'database') 'WMS_PROD' 'ROD-SRVW-001' $phase
                $info.Arguments=$info.Arguments.Replace('-Pbootstrap-local ','-Pbootstrap-local,pure-no-db ').Replace(' validate flyway:',(' "-Dflyway.locations=filesystem:'+($staging.Replace('\','/'))+'" -Dflyway.target=11 -Dmssql-jdbc.version=13.4.0.jre11 validate flyway:'))
                $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_TRUSTSTORE']=$tls.truststore
                $info.EnvironmentVariables['WMS_DB_BOOTSTRAP_CERTIFICATE_HOST']=$tls.certificateHost
                $result=Invoke-WmsD21Processo $info $credential.Password
                $report.etapasFlyway+=@([ordered]@{etapa=$phase;resultado=$result})
                Assert-Provision ($result.exitCode -eq 0) 'FLYWAY_INTERROMPIDO'
                if($phase -ceq 'Migrate'){$report.alteracoes+=@('MIGRATIONS_PENDENTES_PROD')}
            }
        }
        $report.fase='VALIDACAO_POS_MIGRATION'
        $report.historicoDepois=@(Read-Provision 'SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank')
        Assert-Provision (Compare-Provision $report.historicoDepois $expected.historico @('installed_rank','version','type','script','checksum','success')) 'HISTORICO_POS_DIVERGENTE'
        $report.linhasDepois=@(Read-Provision "SELECT t.name tabela,SUM(p.rows) linhas FROM sys.tables t JOIN sys.partitions p ON p.object_id=t.object_id AND p.index_id IN (0,1) WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history' GROUP BY t.name ORDER BY t.name")
        Assert-Provision (Compare-Provision $report.linhasAntes @($report.linhasDepois|Where-Object {$_.tabela -in $report.linhasAntes.tabela}) @('tabela','linhas')) 'LINHAS_PREEXISTENTES_MUDARAM'
        $report.fase='ACESSO_PROPRIO'
        # Guardar a senha gerada em DPAPI antes do DDL evita login sem canal se o processo parar.
        if(-not $report.canalLocalExiste){
            $rng=[Security.Cryptography.RandomNumberGenerator]::Create();$bytes=New-Object byte[] 48
            try{$rng.GetBytes($bytes);$newPassword=ConvertTo-SecureString ([Convert]::ToBase64String($bytes)+'aA1!') -AsPlainText -Force;[Array]::Clear($bytes,0,$bytes.Length)}finally{$rng.Dispose()}
            try{$null=Save-WmsProductionApplicationCredential $newPassword}finally{$newPassword.Dispose()}
            $report.alteracoes+=@('CANAL_DPAPI_WMSPROD_CRIADO')
        }
        $appCredential=Get-WmsProductionApplicationCredential
        try{
            if(-not $report.login.Count){
                $ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($appCredential.Password)
                $cmd=$connection.CreateCommand()
                try{
                    $plain=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
                    $cmd.CommandText="IF DB_NAME()<>N'WMS_PROD' THROW 51000,'Alvo recusado',1; IF SUSER_ID(N'WMSPROD') IS NOT NULL THROW 51000,'Login concorrente preservado',1; DECLARE @ddl nvarchar(max)=N'CREATE LOGIN [WMSPROD] WITH PASSWORD='+QUOTENAME(@password,CHAR(39))+N', DEFAULT_DATABASE=[WMS_PROD], CHECK_POLICY=ON, CHECK_EXPIRATION=OFF'; EXEC sys.sp_executesql @ddl;"
                    $null=$cmd.Parameters.Add('@password',[Data.SqlDbType]::NVarChar,128);$cmd.Parameters['@password'].Value=$plain
                    $null=$cmd.ExecuteNonQuery();$report.alteracoes+=@('LOGIN_WMSPROD_CRIADO')
                }finally{$cmd.Dispose();$plain=$null;[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)}
            }
        }finally{$appCredential.Password.Dispose()}
        $transaction=$connection.BeginTransaction()
        if(-not $report.usuario.Count){Write-Provision "CREATE USER [WMSPROD] FOR LOGIN [WMSPROD] WITH DEFAULT_SCHEMA=[wms]";$report.alteracoes+=@('USUARIO_WMSPROD_CRIADO')}
        foreach($p in $report.direitos){Assert-Provision (@($expected.rights.explicit|Where-Object {Compare-Provision @($_) @($p) @('class_desc','esquema','objeto','coluna','permission_name','state_desc')}).Count -eq 1) 'DIREITO_PREEXISTENTE_FORA_CONTRATO'}
        foreach($p in $expected.rights.explicit){
            Assert-Provision ($p.state_desc -cin @('GRANT','DENY')) 'ESTADO_PERMISSAO'
            if($p.class_desc -ceq 'DATABASE'){
                Assert-Provision ($p.permission_name -cin @('CONNECT','VIEW ANY COLUMN ENCRYPTION KEY DEFINITION','VIEW ANY COLUMN MASTER KEY DEFINITION')) 'PERMISSAO_BANCO'
                Write-Provision ($p.state_desc+' '+$p.permission_name+' TO [WMSPROD]')
            }else{
                Assert-Provision ($p.class_desc -ceq 'OBJECT_OR_COLUMN' -and $p.esquema -ceq 'wms' -and $p.objeto -cmatch '^[a-z_]+$' -and $p.permission_name -cin @('SELECT','INSERT','UPDATE') -and $p.state_desc -ceq 'GRANT') 'PERMISSAO_OBJETO'
                $column=''
                if($p.minor_id -gt 0){Assert-Provision ($p.coluna -cmatch '^[a-z_0-9]+$' -and $p.permission_name -ceq 'UPDATE') 'PERMISSAO_COLUNA';$column=' (['+$p.coluna+'])'}
                Write-Provision ('GRANT '+$p.permission_name+' ON OBJECT::[wms].['+$p.objeto+']'+$column+' TO [WMSPROD]')
            }
        }
        $report.direitosDepois=@(Read-Provision "SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID(N'WMSPROD')")
        Assert-Provision (Compare-Provision $report.direitosDepois $expected.rights.explicit @('class_desc','esquema','objeto','minor_id','coluna','permission_name','state_desc')) 'DIREITOS_POS_DIVERGENTES'
        $transaction.Commit();$transaction.Dispose();$transaction=$null
        $report.alteracoes+=@('PERMISSOES_OBJETO_COLUNA_CONFERIDAS')
        $report.estado='PROVISIONAMENTO_SQL_CONCLUIDO';return
    }
    $report.estado='INSPECAO_CONCLUIDA_SEM_ALTERACOES'
}catch{
    $report.estado='INTERROMPIDO';$ex=$_.Exception;$code='PROD_PROVISION_FALHA';$number=$null
    while($ex){if($ex.Message -cmatch '^(PROD|SQL|D21)_[A-Z0-9_]+$'){$code=$ex.Message};if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException}
    $report.erro=[ordered]@{codigo=$code;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber}
}finally{
    if($transaction){try{$transaction.Rollback()}finally{$transaction.Dispose()}}
    if($connection){$connection.Dispose()};if($credential){$credential.Password.Dispose()};if($certificate){$certificate.Dispose()}
    $report.fimUtc=[DateTime]::UtcNow.ToString('o')
    [IO.File]::WriteAllText($output,($report|ConvertTo-Json -Depth 16),[Text.UTF8Encoding]::new($false))
    [pscustomobject]@{estado=$report.estado;fase=$report.fase;recibo=$output;erro=$report.erro}|ConvertTo-Json -Depth 4
}
if($report.estado -ceq 'INTERROMPIDO'){exit 20}
