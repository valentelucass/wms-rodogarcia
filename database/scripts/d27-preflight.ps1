[CmdletBinding()]
param()
$WmsD27Root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
function Invoke-WmsD27Preflight {
    $ErrorActionPreference='Stop';$inicio=[DateTime]::UtcNow;$c=$null;$cred=$null;$etapa='API_PROTEGIDA';$abriu=$false
    function Select-D27([string]$Sql){
        if($Sql.TrimStart() -notmatch '^SELECT\b' -or $Sql -match ';\s*\S'){throw 'D27_SOMENTE_SELECT'}
        $cmd=$c.CreateCommand();$cmd.CommandText=$Sql;$cmd.CommandTimeout=20;$rows=New-Object 'Collections.Generic.List[object]'
        try{$reader=$cmd.ExecuteReader();try{while($reader.Read()){$o=[ordered]@{};for($i=0;$i -lt $reader.FieldCount;$i++){$o[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}};$rows.Add([pscustomobject]$o)}}finally{$reader.Dispose()}}finally{$cmd.Dispose()};$rows.ToArray()
    }
    try{
        Import-Module ([IO.Path]::GetFullPath((Join-Path $WmsD27Root '../.runtime/sql-server/SqlServerProjetos.psm1')))
        $cred=Get-ProjetosSqlAdminCredential
        if($cred -isnot [Management.Automation.PSCredential] -or $cred.UserName -cne 'sa' -or $cred.Password -isnot [Security.SecureString]){throw 'D27_CREDENCIAL_ADMIN_INVALIDA'}
        $perfil=Get-ProjetosSqlProfile
        $c=New-ProjetosSqlConnection -Database WMS_DEV -Credential $cred -ApplicationName 'WMS-D27-PREFLIGHT-SELECT'
        $b=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder($c.ConnectionString)
        if($b.DataSource -cne 'tcp:127.0.0.1,1433' -or $b.InitialCatalog -cne 'WMS_DEV' -or $b.TrustServerCertificate -or $b.Encrypt -ne [Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory -or -not $b.ServerCertificate -or $b.Pooling){throw 'D27_CONEXAO_TLS_ALVO_INVALIDO'}
        $etapa='OPEN_ADMIN';$c.Open();$abriu=$true
        $etapa='IDENTIDADE';$a=@(Select-D27 "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,USER_NAME() usuario,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,CONVERT(nvarchar(128),SERVERPROPERTY('ProductVersion')) versao,CONVERT(nvarchar(128),SERVERPROPERTY('Edition')) edicao,(SELECT state_desc FROM sys.databases WHERE database_id=DB_ID()) estado,(SELECT encrypt_option FROM sys.dm_exec_connections WHERE session_id=@@SPID) criptografado,HAS_PERMS_BY_NAME(DB_NAME(),N'DATABASE',N'VIEW DEFINITION') definicaoVisivel")
        if($a.Count -ne 1 -or $a[0].banco -cne 'WMS_DEV' -or $a[0].login -cne 'sa' -or $a[0].servidor -cne $perfil.server -or $a[0].versao -cne '16.0.1000.6' -or $a[0].edicao -notlike 'Standard*' -or $a[0].estado -cne 'ONLINE' -or $a[0].criptografado -cne 'TRUE' -or $a[0].definicaoVisivel -ne 1){throw 'D27_IDENTIDADE_REAL_DIVERGENTE'}
        $etapa='CHECK_CATALOGO_HISTORICO'
        $check=@(Select-D27 "SELECT name,definition,is_disabled,is_not_trusted FROM sys.check_constraints WHERE parent_object_id=OBJECT_ID(N'wms.fato_permanencia') AND name=N'ck_fato_permanencia_tipo'")
        if($check.Count -ne 1 -or $null -eq $check[0].definition -or $check[0].is_disabled -or $check[0].is_not_trusted){throw 'D27_CHECK_NAO_VISIVEL_OU_INVALIDO'}
        $cat=@(Select-D27 "SELECT (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') tabelas,(SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') colunas,(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) fkInvalidas,(SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) checkInvalidas,(SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) indicesDesabilitados")
        if($cat[0].tabelas -ne 64 -or $cat[0].colunas -ne 687 -or $cat[0].fkInvalidas -ne 0 -or $cat[0].checkInvalidas -ne 0 -or $cat[0].indicesDesabilitados -ne 0){throw 'D27_CATALOGO_DIVERGENTE'}
        $hist=@(Select-D27 'SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank')
        $d24=Get-Content -LiteralPath (Join-Path $WmsD27Root 'database/evidencias/d24-resultado-real.json') -Raw -Encoding UTF8|ConvertFrom-Json
        $dev=@($d24.wms|Where-Object {$_.metadata.banco -ceq 'WMS_DEV'});$campos=@('installed_rank','version','script','checksum','success');$sql9=@($hist|Where-Object {$_.type -ceq 'SQL' -and $_.version -in @('1','2','3','4','5','6','7','8','9')})
        if(($sql9|Select-Object $campos|ConvertTo-Json -Compress) -cne ($dev[0].history|Select-Object $campos|ConvertTo-Json -Compress)){throw 'D27_HISTORICO_V1_V9_DIVERGENTE'}
        $extra=@($hist|Where-Object {$_.type -ceq 'SQL' -and $_.version -notin @('1','2','3','4','5','6','7','8','9','10')})
        if($extra.Count -or @($hist|Where-Object {-not $_.success -or $_.type -notin @('SQL','SCHEMA')}).Count){throw 'D27_HISTORICO_INESPERADO'}
        $etapa='WMSDEV_PERMISSOES_E_FIXTURES'
        $identidade=@(Select-D27 "SELECT sp.name login,sp.type_desc,sp.is_disabled,sp.default_database_name,IS_SRVROLEMEMBER(N'sysadmin',N'WMSDEV') sysadmin,dp.name usuario,dp.type_desc tipoUsuario FROM sys.server_principals sp LEFT JOIN sys.database_principals dp ON dp.sid=sp.sid WHERE sp.name=N'WMSDEV'")
        $permissoes=@(Select-D27 "SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID(N'WMSDEV') ORDER BY p.class,p.major_id,p.minor_id,p.permission_name")
        $roles=@(Select-D27 "SELECT r.name role FROM sys.database_role_members m JOIN sys.database_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=USER_ID(N'WMSDEV') ORDER BY r.name")
        $fixtures=@(Select-D27 "SELECT (SELECT quantidade FROM wms.unidade_logistica WHERE id=6) unidade6Quantidade,(SELECT revisao_atual FROM wms.contagem_estoque WHERE id=2) contagem2RevisaoAtual,(SELECT COUNT(*) FROM wms.fato_permanencia WHERE tipo=N'AJUSTE_ESTOQUE') fatosAjuste,(SELECT COUNT(*) FROM wms.cliente WHERE id=1 AND codigo LIKE N'D2622F35458%') fixtureITPresente")
        $resultado=[pscustomobject]@{estado='PREFLIGHT_ADMIN_REAL_CONFERIDO';inicioUtc=$inicio.ToString('o');fimUtc=[DateTime]::UtcNow.ToString('o');alvo=$a[0];tlsMandatoryCertificadoExato=$true;credencialOrigem='Get-ProjetosSqlAdminCredential/API_DPAPI_ACL';check=$check[0];catalogo=$cat[0];historico=$hist;historicoSQLV1V9IgualD24=$true;wmsdevIdentidade=$identidade;wmsdevPermissoes=$permissoes;wmsdevRoles=$roles;fixtures=$fixtures[0];javaHome=$perfil.javaHome;somenteSELECT=$true;abriuConexao=$true;tentativas=1;ddl=0;dml=0;grants=0;prodConectado=$false;segredoExibido=$false}
    }catch{
        $ex=$_.Exception;$numero=$null;$classe=$null;$estadoSql=$null;$codigo=$null;$tipo=$ex.GetType().FullName
        while($ex){if($ex.Message -cmatch '^(D27_|SQL_)[A-Z0-9_]+$'){$codigo=$ex.Message};if($ex.GetType().FullName -eq 'Microsoft.Data.SqlClient.SqlException'){$numero=$ex.Number;$classe=$ex.Class;$estadoSql=$ex.State};$ex=$ex.InnerException}
        if(-not $codigo){$codigo=if($null -ne $numero){'D27_SQL_'+$numero}else{'D27_PREFLIGHT_FALHOU'}}
        $resultado=[pscustomobject]@{estado='PREFLIGHT_BLOQUEADO';inicioUtc=$inicio.ToString('o');fimUtc=[DateTime]::UtcNow.ToString('o');etapa=$etapa;codigo=$codigo;sqlNumber=$numero;sqlClass=$classe;sqlState=$estadoSql;tipoErro=$tipo;linha=$_.InvocationInfo.ScriptLineNumber;bancoSolicitado='WMS_DEV';loginSolicitado='sa';credencialOrigem='Get-ProjetosSqlAdminCredential/API_DPAPI_ACL';abriuConexao=$abriu;tentativas=1;somenteSELECT=$true;ddl=0;dml=0;grants=0;prodConectado=$false;segredoExibido=$false;limite='Falha atual deste consumidor administrativo; nao inferir estado/permissao da aplicacao nem indisponibilidade SQL/TLS. Sem retry cego.'}
    }finally{if($c){$c.Dispose()};if($cred){$cred.Password.Dispose()}}
    $resultado|Add-Member NoteProperty descarteFinallyExecutado $true
    $resultado
}
if($MyInvocation.InvocationName -ne '.'){
    $r=Invoke-WmsD27Preflight;$nome='d27-preflight-real-'+[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfff')+'.json'
    $r|ConvertTo-Json -Depth 12|Set-Content -LiteralPath (Join-Path $WmsD27Root ('database/evidencias/'+$nome)) -Encoding UTF8
    $r|ConvertTo-Json -Depth 12|Set-Content -LiteralPath (Join-Path $WmsD27Root 'orchestracao/.runtime/d27-prumo-preflight-real.json') -Encoding UTF8
    $r|Select-Object estado,codigo,etapa,sqlNumber,sqlClass,sqlState,abriuConexao,ddl|ConvertTo-Json -Compress
    if($r.estado -cne 'PREFLIGHT_ADMIN_REAL_CONFERIDO'){exit 2}
}
