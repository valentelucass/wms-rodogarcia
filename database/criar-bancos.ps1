[CmdletBinding()]
param([ValidateSet('Plan','Create')][string]$Action='Plan',
      [ValidateSet('WMS_DEV','WMS_PROD')][string]$Database='WMS_DEV')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd20-guardas.ps1')
. (Join-Path $PSScriptRoot 'criacao/ler-vazio.ps1')
if ($Database -cnotin @('WMS_DEV','WMS_PROD')) {throw 'D20_BANCO_NAO_AUTORIZADO'}
if ($Action -eq 'Plan') {
    [pscustomobject]@{natureza='PLANO_OFFLINE_SEM_AMBIENTE_SEM_CONEXAO';banco=$Database;
        ddl=('criacao/CREATE_'+$Database+'.sql');existencia='NAO_CONSULTADA';
        ordem=if($Database -ceq 'WMS_DEV'){1}else{2};
        requisito='ALVO_ACESSO_INEQUIVOCOS_TLS_IDENTIDADE_MESMA_SESSAO';
        devAntesProd=$true;semSobrescrita=$true;migrationProd=$false;cargaProd=$false} | ConvertTo-Json
    return
}
# Nao ha conexao no default. Criacao e migration usam contextos separados.
$nomes=@('WMS_DB_HOST','WMS_DB_PORT','WMS_DB_CONFIRMED_TARGET','WMS_DB_CONFIRMED_SERVER',
         'WMS_DB_AUTH','WMS_DB_CONFIRMED_LOGIN')
$v=@{}
foreach($nome in $nomes) {
    $v[$nome]=[Environment]::GetEnvironmentVariable($nome)
    if([string]::IsNullOrWhiteSpace($v[$nome])){throw "D20_VARIAVEL_AUSENTE_$nome"}
}
$acao=if($Database -ceq 'WMS_DEV'){'CriarDev'}else{'CriarProd'}
Assert-WmsD20Alvo $v.WMS_DB_HOST $v.WMS_DB_PORT $Database $v.WMS_DB_CONFIRMED_TARGET $v.WMS_DB_CONFIRMED_SERVER $acao
$b=New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$b.DataSource="$($v.WMS_DB_HOST),$($v.WMS_DB_PORT)";$b.InitialCatalog='master'
$b.Encrypt=$true;$b.TrustServerCertificate=$false;$b.ConnectTimeout=10;$b.ApplicationName='WMS-D20-CREATE'
switch -CaseSensitive ($v.WMS_DB_AUTH) {
    'Windows' {$b.IntegratedSecurity=$true}
    'SqlLogin' {
        $b.UserID=[Environment]::GetEnvironmentVariable('WMS_DB_USER')
        $b.Password=[Environment]::GetEnvironmentVariable('WMS_DB_PASSWORD')
        if([string]::IsNullOrWhiteSpace($b.UserID) -or [string]::IsNullOrWhiteSpace($b.Password)){throw 'D20_CREDENCIAL_EXTERNA_AUSENTE'}
        if($b.UserID -cne $v.WMS_DB_CONFIRMED_LOGIN){throw 'D20_LOGIN_CONFIGURADO_DIVERGENTE'}
    }
    default {throw 'D20_AUTH_NAO_DEFINIDA'}
}
$c=New-Object System.Data.SqlClient.SqlConnection($b.ConnectionString)
$fase='CONEXAO';$codigo=$null;$resultado='FALHA';$dev=$false;$ddlSolicitado=$false
try {
    $c.Open();$cmd=$c.CreateCommand();$cmd.CommandTimeout=15
    $cmd.CommandText="IF @@TRANCOUNT<>0 THROW 51023,'CREATE exige autocommit.',1; SET IMPLICIT_TRANSACTIONS OFF; SELECT CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')),DB_NAME(),ORIGINAL_LOGIN()"
    $r=$cmd.ExecuteReader()
    if(-not $r.Read()){throw 'D20_IDENTIDADE_AUSENTE'}
    $servidor=$r.GetString(0);$banco=$r.GetString(1);$login=$r.GetString(2);$r.Close()
    Assert-WmsD20Identidade $servidor $banco $v.WMS_DB_CONFIRMED_SERVER 'master'
    if($login -cne $v.WMS_DB_CONFIRMED_LOGIN){throw 'D20_LOGIN_REAL_DIVERGENTE'}
    $fase='EXISTENCIA';$cmd.CommandText="SELECT name,state FROM sys.databases WHERE name IN (N'WMS_DEV',N'WMS_PROD')"
    $r=$cmd.ExecuteReader();$existe=$false
    while($r.Read()) {
        $n=$r.GetString(0)
        if($n -ieq $Database){$existe=$true}
        if($n -ceq 'WMS_DEV' -and $r.GetByte(1) -eq 0){$dev=$true}
    }
    $r.Close();Assert-WmsD20Criacao $Database $existe $dev
    if($Database -ceq 'WMS_PROD'){
        $fase='DEV_PRIMEIRO_COMPROVAR_VAZIO';Assert-WmsD20Vazio (Read-WmsD20Vazio $c 'WMS_DEV') 'WMS_DEV'
    }
    $fase='PREFLIGHT_MODEL';Assert-WmsD20Vazio (Read-WmsD20Vazio $c 'model') 'model'
    $fase='CREATE';$cmd.CommandTimeout=120
    $cmd.CommandText=[IO.File]::ReadAllText((Join-Path $PSScriptRoot ('criacao/CREATE_'+$Database+'.sql')))
    $null=$cmd.Parameters.Add('@ServidorEsperado',[System.Data.SqlDbType]::NVarChar,128)
    $cmd.Parameters['@ServidorEsperado'].Value=$v.WMS_DB_CONFIRMED_SERVER
    $null=$cmd.Parameters.Add('@LoginEsperado',[System.Data.SqlDbType]::NVarChar,128)
    $cmd.Parameters['@LoginEsperado'].Value=$v.WMS_DB_CONFIRMED_LOGIN
    $ddlSolicitado=$true;$null=$cmd.ExecuteNonQuery()
    $fase='CONFIRMACAO';$cmd.Parameters.Clear()
    $cmd.CommandText="SELECT name,state FROM sys.databases WHERE name=@banco"
    $null=$cmd.Parameters.Add('@banco',[System.Data.SqlDbType]::NVarChar,128);$cmd.Parameters['@banco'].Value=$Database
    $r=$cmd.ExecuteReader()
    if(-not $r.Read() -or $r.GetString(0) -cne $Database -or $r.GetByte(1) -ne 0){throw 'D20_CREATE_RESULTADO_NAO_CONFIRMADO'}
    $r.Close()
    $fase='VERIFICACAO_SEM_OBJETOS_USUARIO'
    # Database foi validado contra dois literais. Nao ha identificador livre.
    Assert-WmsD20Vazio (Read-WmsD20Vazio $c $Database) $Database
    $resultado='CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'
} catch {
    # Somente codigo, nunca ConnectionString, senha/login real ou mensagem de driver.
    $e=$_.Exception
    while($e.InnerException){$e=$e.InnerException}
    $codigo=if($e -is [System.Data.SqlClient.SqlException]){$e.Number}else{'GUARDA_OU_CONFIRMACAO'}
} finally { $c.Dispose();$b.Password='' }
$evidencia=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');banco=$Database;acao='CREATE_VAZIO';
    fase=$fase;resultado=$resultado;codigo=$codigo;devOnlineComprovado=$dev;ddlSolicitado=$ddlSolicitado;
    migrationsExecutadas=$false;cargasExecutadas=$false;grantsExecutados=$false}
$arquivo=Join-Path $PSScriptRoot ('evidencias/d20-create-'+$Database+'-'+[Guid]::NewGuid().ToString('N')+'.json')
$evidencia|ConvertTo-Json|Set-Content -LiteralPath $arquivo -Encoding UTF8
$evidencia|ConvertTo-Json
if($resultado -ne 'CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'){throw 'D20_CREATE_FALHOU_CONFERIR_EVIDENCIA_SEM_REPETICAO_AUTOMATICA'}
