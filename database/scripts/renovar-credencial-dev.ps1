[CmdletBinding(DefaultParameterSetName='Plano')]
param(
    [Parameter(Mandatory,ParameterSetName='Renovar')][switch]$Renovar,
    [Parameter(Mandatory,ParameterSetName='Concluir')][switch]$ConcluirPendente
)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd26-credencial-aplicacao.ps1')
. (Join-Path $PSScriptRoot 'd24-runtime.ps1')

$ctx=Get-WmsDevCredentialContext
$pending=Join-Path $ctx.pasta 'wmsdev-renovacao-pendente.clixml'
if(!$Renovar -and !$ConcluirPendente){
    'Plano: renovar somente a senha do login existente WMSDEV em WMS_DEV e atualizar a credencial DPAPI local.'
    return
}
$lock=$null;$admin=$null;$account=$null;$conn=$null;$cmd=$null
$phase='PREPARO_LOCAL'
try {
    $lock=[Threading.Mutex]::new($false,'Global\WMS-WMSDEV-Renovacao')
    try{$owned=$lock.WaitOne(0)}catch [Threading.AbandonedMutexException]{$owned=$true}
    if(!$owned){throw 'RENOVACAO_EM_ANDAMENTO'}
    if(!(Get-WmsDevCredentialMetadata).aclValida){throw 'ACL_DEV_INVALIDA'}
    $null=Get-WmsRuntimeTrust
    if($Renovar){
        if(Test-Path -LiteralPath $pending){throw 'PENDENCIA_EXISTENTE_PRESERVADA'}
        $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
        $bytes=New-Object byte[] 32
        try {
            $rng.GetBytes($bytes)
            $secret=ConvertTo-SecureString ('Wm2!'+[Convert]::ToBase64String($bytes)) -AsPlainText -Force
            try {
                $stream=[IO.File]::Open($pending,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
                $stream.Dispose()
                [IO.File]::SetAccessControl($pending,(New-WmsD22Security $ctx.sid $false))
                [Management.Automation.PSCredential]::new('WMSDEV',$secret) | Export-Clixml -LiteralPath $pending
            }finally{$secret.Dispose()}
        }finally{[Array]::Clear($bytes,0,$bytes.Length);$rng.Dispose()}
    }
    $item=Get-Item -LiteralPath $pending -Force
    if($item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint) -or
        !(Get-WmsD22AclMetadata $pending $ctx.sid $false).valida){throw 'PENDENCIA_INVALIDA'}
    $account=Import-Clixml -LiteralPath $pending
    if($account -isnot [Management.Automation.PSCredential] -or $account.UserName -cne 'WMSDEV' -or
        $account.Password.Length -lt 32){throw 'PENDENCIA_INVALIDA'}
    $account.Password.MakeReadOnly()
    if($Renovar){
        $phase='RENOVACAO_WMSDEV'
        $admin=Get-ProjetosSqlAdminCredential
        $conn=New-WmsRuntimeConnection 'WMS_DEV' ([pscustomobject]@{UserId=$admin.UserName;Password=$admin.Password}) 'WMS-DEV-RENOVAR'
        $conn.Open()
        $null=Get-WmsRuntimeTrust
        $cmd=$conn.CreateCommand()
        $cmd.CommandText=@'
IF DB_NAME() <> N'WMS_DEV' OR ORIGINAL_LOGIN() <> N'sa' OR CONVERT(nvarchar(128), SERVERPROPERTY('ServerName')) <> N'ROD-SRVW-001'
    THROW 51000, 'Alvo recusado', 1;
IF NOT EXISTS (SELECT 1 FROM sys.sql_logins l JOIN sys.database_principals u ON u.sid=l.sid
    WHERE l.name=N'WMSDEV' AND u.name=N'WMSDEV' AND l.is_disabled=0 AND u.type='S')
    OR EXISTS (SELECT 1 FROM sys.server_role_members WHERE member_principal_id=SUSER_ID(N'WMSDEV'))
    THROW 51000, 'Identidade recusada', 1;
DECLARE @ddl nvarchar(max)=N'ALTER LOGIN [WMSDEV] WITH PASSWORD='+QUOTENAME(@senha, CHAR(39));
EXEC sys.sp_executesql @ddl;
'@
        $parameter=$cmd.Parameters.Add('@senha',[Data.SqlDbType]::NVarChar,128)
        try {
            $parameter.Value=$account.GetNetworkCredential().Password
            $null=$cmd.ExecuteNonQuery()
        }finally{$cmd.Parameters.Clear();$parameter=$null}
        $cmd.Dispose();$cmd=$null;$conn.Dispose();$conn=$null;$admin.Password.Dispose();$admin=$null
    }
    $phase='VALIDACAO_LOGIN_DEV'
    $tls=Get-WmsRuntimeTrust
    $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
    $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_DEV')
    $builder.set_ServerCertificate($tls.certificate);$builder.set_TrustServerCertificate($false)
    $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory)
    $builder.set_Pooling($false);$builder.set_ConnectRetryCount(0);$builder.set_ApplicationName('WMS-DEV-VALIDAR-RENOVACAO')
    $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSDEV',$account.Password)
    $conn=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
    $conn.Open()
    $null=Get-WmsRuntimeTrust
    $cmd=$conn.CreateCommand()
    $cmd.CommandText="SELECT CASE WHEN DB_NAME()=N'WMS_DEV' AND ORIGINAL_LOGIN()=N'WMSDEV' AND USER_NAME()=N'WMSDEV' AND CONVERT(nvarchar(128),SERVERPROPERTY('ServerName'))=N'ROD-SRVW-001' THEN 1 ELSE 0 END"
    if($cmd.ExecuteScalar() -ne 1){throw 'IDENTIDADE_DEV_DIVERGENTE'}
    $phase='INSTALACAO_LOCAL'
    if(!(Get-WmsDevCredentialMetadata).aclValida){throw 'ACL_DEV_INVALIDA'}
    $backup=Join-Path $ctx.pasta ('wmsdev-anterior-'+[Guid]::NewGuid().ToString('N')+'.clixml')
    [IO.File]::Replace($pending,$ctx.arquivo,$backup)
    $saved=Get-WmsDevApplicationCredential
    $saved.Password.Dispose()
    'WMSDEV_RENOVACAO_VALIDADA'
} catch {
    $reason=if($_.Exception.Message -cmatch '^[A-Z][A-Z0-9_]+$'){$_.Exception.Message}else{$_.Exception.GetBaseException().GetType().Name}
    throw ('WMSDEV_'+$phase+'_'+$reason)
} finally {
    if($cmd){$cmd.Dispose()};if($conn){$conn.Dispose()}
    if($admin){$admin.Password.Dispose()};if($account -is [Management.Automation.PSCredential]){$account.Password.Dispose()}
    if($owned){$lock.ReleaseMutex()};if($lock){$lock.Dispose()}
}
