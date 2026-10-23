[CmdletBinding()]
param([ValidateSet('Plan','Apply')][string]$Modo='Plan')

# CAD-EDIT01: autorizacao expressa de Lucas, 14/10/2026. Base historica de 17 colunas; TAB-EDIT01 acrescenta 6. Apply depende da liberacao do alvo.
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'infra/auth/contrato-banco.ps1')
. (Join-Path $PSScriptRoot 'contrato-edicao.ps1')
$out=Join-Path $root ('orchestracao/.runtime/cadastros-etiqueta-20261014/ativacao-'+[Guid]::NewGuid().ToString('N'))
$null=New-Item -ItemType Directory -Path $out
$report=[ordered]@{demanda='CAD-EDIT01';modo=$Modo;inicioUtc=[DateTime]::UtcNow.ToString('o');estado='PREPARO';fase='FONTES';banco='WMS_DEV';loginAplicacao='WMSDEV';grantsAplicados=$false;colunas=23;adminAberturas=0;prod=$false;ddl=$false;dml=$false;recapturaTLS=$false}
$connection=$null;$credential=$null;$transaction=$null;$builder=$null
function Assert-Cad([bool]$Ok,[string]$Code){if(-not $Ok){throw ('CAD_'+$Code)}}
function Same-CadRows($A,$B,[string[]]$Fields){
    $left=@($A|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    $right=@($B|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
    ($left.Count -eq $right.Count) -and (($left -join "`n") -ceq ($right -join "`n"))
}
function Select-Cad([string]$Sql){
    Assert-Cad ($Sql.TrimStart() -cmatch '^SELECT\b' -and $Sql -notmatch ';\s*\S') 'SELECT_INVALIDO'
    $cmd=$connection.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=30
    if($transaction){$cmd.Transaction=$transaction}
    $reader=$null;$rows=[Collections.Generic.List[object]]::new()
    try{
        $reader=$cmd.ExecuteReader()
        while($reader.Read()){
            $row=[ordered]@{}
            for($i=0;$i -lt $reader.FieldCount;$i++){$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}}
            $rows.Add([pscustomobject]$row)
        }
        $rows.ToArray()
    }finally{if($reader){$reader.Dispose()};$cmd.Dispose()}
}
$historySql='SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank'
$rightsSql="SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID(N'WMSDEV')"
$catalogSql="SELECT (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') tabelas,(SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') colunas,(SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) checksInvalidos,(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) fksInvalidas,(SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) indicesDesabilitados"
$historyFields=@('installed_rank','version','type','script','checksum','success')
$rightsFields=@('class_desc','esquema','objeto','minor_id','coluna','permission_name','state_desc')
$catalogFields=@('tabelas','colunas','checksInvalidos','fksInvalidas','indicesDesabilitados')
try {
    $baselinePath=Join-Path $root 'orchestracao/.runtime/d29-farol-retomada-preflight.json'
    Assert-Cad ((Get-FileHash -LiteralPath $baselinePath).Hash -ceq 'C22AABF54F839D4B6E139A598A8478530F92F8911FEEE46BB2F92BC85313EA90') 'BASELINE_DIVERGENTE'
    $baseline=Get-Content -LiteralPath $baselinePath -Raw -Encoding UTF8|ConvertFrom-Json
    $migrations=@(Get-WmsDev02Migrations)
    Assert-Cad ($migrations.Count -in @(11,12,13,14)) 'MIGRATIONS_INESPERADAS'
    $v11=@($migrations|Where-Object {$_.version -ceq '11'})[0]
    $before=Add-WmsAuthExpectedContract $baseline $v11
    if ($migrations.Count -ge 12) {
        . (Join-Path $root 'infra/tipos-unidade/contrato-banco.ps1')
        $before=Add-WmsUnitTypesExpectedContract $before ($migrations | Where-Object {$_.version -ceq '12'})
    }
    if ($migrations.Count -ge 13) {
        . (Join-Path $root 'infra/tabelas/contrato-banco.ps1')
        $before=Add-WmsBillingTableExpectedContract $before ($migrations | Where-Object {$_.version -ceq '13'})
    }
    if ($migrations.Count -eq 14) {
        . (Join-Path $root 'infra/inventario/contrato-banco.ps1')
        $before=Add-WmsInventoryExpectedContract $before ($migrations | Where-Object {$_.version -ceq '14'})
    }
    $legacy=Add-WmsCadastroEditExpectedContract $before -SomenteBase
    $expected=Add-WmsCadastroEditExpectedContract $before
    Assert-Cad (Same-CadRows @($expected.historico|Where-Object {$_.type -ceq 'SQL'}) $migrations @('version','script','checksum')) 'MIGRATIONS_DIVERGENTES'
    $delta=Get-WmsCadastroEditableColumns
    $sql=@($delta.Keys|Sort-Object|ForEach-Object {
        $table=$_;Assert-Cad ($table -cmatch '^[a-z_]+$') 'TABELA_INVALIDA'
        $columns=@($delta[$table]|ForEach-Object {Assert-Cad ($_ -cmatch '^[a-z_]+$') 'COLUNA_INVALIDA';'['+$_+']'})
        'GRANT UPDATE ('+($columns -join ',')+') ON OBJECT::[wms].['+$table+'] TO [WMSDEV];'
    }) -join "`n"
    Assert-Cad (@($expected.rights.explicit).Count-@($before.rights.explicit).Count -eq 23) 'DELTA_DIVERGENTE'
    $sourceHash=(Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'contrato-edicao.ps1')).Hash
    $report.contratoSha256=$sourceHash
    [IO.File]::WriteAllText((Join-Path $out 'permissoes.sql'),$sql,[Text.UTF8Encoding]::new($false))
    if($Modo -eq 'Plan'){$report.estado='PLANO_LOCAL_VALIDADO';return}

    $report.fase='GUARDA_PRE_GRANT_WMSDEV'
    $pre=Invoke-WmsDev02Guard -CadastroPreGrant
    [IO.File]::WriteAllText((Join-Path $out 'guarda-antes.json'),($pre|ConvertTo-Json -Depth 22),[Text.UTF8Encoding]::new($false))
    Assert-Cad ($pre.guardaPermissoesCadastroAprovada -and -not $pre.guardaRealAprovada -and $pre.estado -ceq 'GUARDA_PRE_GRANT_CADASTRO_APROVADA') 'GUARDA_ANTES_RECUSADA'
    $report.fase='ADMIN_PROTEGIDO'
    $module=Import-Module ([IO.Path]::GetFullPath((Join-Path $root '../.runtime/sql-server/SqlServerProjetos.psm1'))) -PassThru -Force
    & $module {Import-SqlClient}
    $tlsRoot=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer'
    & $module {param($p) Assert-PrivateAcl $p;Assert-PrivateAcl (Join-Path $p 'admin.clixml')} $tlsRoot
    $local=& $module {Get-LocalIdentity}
    Assert-Cad ($local.pid -eq $pre.tls.proveniencia.reciboPID -and $local.started -ceq $pre.tls.proveniencia.reciboInicioUtc) 'PROCESSO_SQL_MUDOU'
    Assert-Cad ((Get-FileHash -LiteralPath $pre.perfilProtegido.certificate).Hash -ceq $pre.tls.certificateSha256) 'CERTIFICADO_MUDOU'
    Assert-Cad (([DateTime]::UtcNow-[DateTime]::Parse($pre.observadoEm).ToUniversalTime()).TotalSeconds -lt 120) 'GUARDA_EXPIRADA'
    $credential=Get-ProjetosSqlAdminCredential
    Assert-Cad ($credential.UserName -ceq 'sa') 'ADMIN_INVALIDO'
    $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
    $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_DEV')
    $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory);$builder.set_TrustServerCertificate($false)
    $builder.set_ServerCertificate($pre.perfilProtegido.certificate);$builder.set_Pooling($false)
    $builder.set_ConnectTimeout(30);$builder.set_ConnectRetryCount(0);$builder.set_TransparentNetworkIPResolution($false)
    $builder.set_ApplicationName('WMS-TAB-EDIT01-23-COLUNAS')
    $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('sa',$credential.Password)
    $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
    $report.adminAberturas=1;$connection.Open()
    $afterOpen=& $module {Get-LocalIdentity}
    Assert-Cad ($local.pid -eq $afterOpen.pid -and $local.started -ceq $afterOpen.started) 'PROCESSO_SQL_MUDOU'
    $identity=@(Select-Cad "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,(SELECT state_desc FROM sys.databases WHERE database_id=DB_ID()) estado,(SELECT encrypt_option FROM sys.dm_exec_connections WHERE session_id=@@SPID) criptografado")
    Assert-Cad ($identity.Count -eq 1 -and $identity[0].banco -ceq 'WMS_DEV' -and $identity[0].login -ceq 'sa' -and $identity[0].servidor -ceq 'ROD-SRVW-001' -and $identity[0].estado -ceq 'ONLINE' -and $identity[0].criptografado -ceq 'TRUE') 'IDENTIDADE_INVALIDA'
    $report.identidadeAdmin=$identity[0]
    $report.fase='TRANSACAO_DIREITOS'
    $transaction=$connection.BeginTransaction()
    Assert-Cad (Same-CadRows @(Select-Cad $historySql) $expected.historico $historyFields) 'HISTORICO_DIVERGENTE'
    Assert-Cad (Same-CadRows @(Select-Cad $catalogSql) @($expected.catalogo) $catalogFields) 'CATALOGO_DIVERGENTE'
    $rights=@(Select-Cad $rightsSql)
    Assert-Cad (Same-CadRows $rights $pre.direitos.explicit $rightsFields) 'DIREITOS_MUDARAM'
    $alreadyGranted=Same-CadRows $rights $expected.rights.explicit $rightsFields
    Assert-Cad ($alreadyGranted -or (Same-CadRows $rights $before.rights.explicit $rightsFields) -or (Same-CadRows $rights $legacy.rights.explicit $rightsFields)) 'DIREITOS_INESPERADOS'
    $report.antes=$rights
    if(-not $alreadyGranted){
        Assert-Cad ((Get-FileHash -LiteralPath (Join-Path $PSScriptRoot 'contrato-edicao.ps1')).Hash -ceq $sourceHash) 'CONTRATO_MUDOU'
        $cmd=$connection.CreateCommand();$cmd.Transaction=$transaction;$cmd.CommandTimeout=30
        try{$cmd.CommandText=$sql;$null=$cmd.ExecuteNonQuery()}finally{$cmd.Dispose()}
    }
    $report.depois=@(Select-Cad $rightsSql)
    Assert-Cad (Same-CadRows $report.depois $expected.rights.explicit $rightsFields) 'DIREITOS_POS_DIVERGENTES'
    Assert-Cad (Same-CadRows @(Select-Cad $historySql) $expected.historico $historyFields) 'HISTORICO_POS_DIVERGENTE'
    Assert-Cad (Same-CadRows @(Select-Cad $catalogSql) @($expected.catalogo) $catalogFields) 'CATALOGO_POS_DIVERGENTE'
    $transaction.Commit();$transaction.Dispose();$transaction=$null
    $report.grantsAplicados=-not $alreadyGranted;$report.jaAplicados=$alreadyGranted
    $connection.Dispose();$connection=$null;$credential.Password.Dispose();$credential=$null
    $report.fase='GUARDA_NORMAL_APOS_GRANT'
    $post=Invoke-WmsDev02Guard
    [IO.File]::WriteAllText((Join-Path $out 'guarda-depois.json'),($post|ConvertTo-Json -Depth 22),[Text.UTF8Encoding]::new($false))
    Assert-Cad ($post.guardaRealAprovada -and $post.estado -ceq 'GUARDA_ATUAL_APROVADA') 'GUARDA_DEPOIS_RECUSADA'
    $report.estado='DIREITOS_DEV_APLICADOS_E_CONFERIDOS'
} catch {
    $report.estado='INTERROMPIDO';$ex=$_.Exception;$code='CAD_FALHA';$number=$null
    while($ex){if($ex.Message -cmatch '^(CAD|SQL|DEV02)_[A-Z0-9_]+$'){$code=$ex.Message};if($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException'){$number=$ex.Number};$ex=$ex.InnerException}
    $report.erro=[ordered]@{codigo=$code;sqlNumber=$number;linha=$_.InvocationInfo.ScriptLineNumber;mensagemBrutaPublicada=$false}
} finally {
    if($transaction){try{$transaction.Rollback()}finally{$transaction.Dispose()}}
    if($connection){$connection.Dispose()};if($credential){$credential.Password.Dispose()};if($builder){$builder.Clear()}
    $report.fimUtc=[DateTime]::UtcNow.ToString('o')
    [IO.File]::WriteAllText((Join-Path $out 'resultado.json'),($report|ConvertTo-Json -Depth 22),[Text.UTF8Encoding]::new($false))
    [pscustomobject]@{estado=$report.estado;fase=$report.fase;recibo=$out;grantsAplicados=$report.grantsAplicados;erro=$report.erro}|ConvertTo-Json -Depth 4
}
if($report.estado -eq 'INTERROMPIDO'){exit 20}
