[CmdletBinding()]
param([ValidateSet('Plan','Create','Check')][string]$Action='Plan',
      [ValidateSet('WMS_DEV','WMS_PROD')][string]$Database='WMS_DEV',
      [string]$ServidorConfirmado,[string]$AlvoConfirmado,
      [Management.Automation.PSCredential]$CredencialLocal,[Security.SecureString]$SenhaLocal)
$WmsD20Pathcriarbancos=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
. (Join-Path $WmsD20Pathcriarbancos 'scripts/d20-guardas.ps1')
. (Join-Path $WmsD20Pathcriarbancos 'scripts/criacao/ler-vazio.ps1')
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
# Metadata explicita; alvo fixado pela autorizacao Lucas desta D20.
$v=@{WMS_DB_HOST='127.0.0.1';WMS_DB_PORT='1433';WMS_DB_CONFIRMED_TARGET=$AlvoConfirmado;
     WMS_DB_CONFIRMED_SERVER=$ServidorConfirmado;WMS_DB_CONFIRMED_LOGIN='sa'}
$acao=if($Database -ceq 'WMS_DEV'){'CriarDev'}else{'CriarProd'}
Assert-WmsD20Alvo $v.WMS_DB_HOST $v.WMS_DB_PORT $Database $v.WMS_DB_CONFIRMED_TARGET $v.WMS_DB_CONFIRMED_SERVER $acao
$b=New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$b.DataSource="$($v.WMS_DB_HOST),$($v.WMS_DB_PORT)";$b.InitialCatalog='master'
$b.Encrypt=$true;$b.TrustServerCertificate=$false;$b.ConnectTimeout=10;$b.ApplicationName='WMS-D20-CREATE'
$b.Pooling=$false
$credencial=Get-WmsD20CredencialCriacao $CredencialLocal $SenhaLocal
$c=New-Object System.Data.SqlClient.SqlConnection($b.ConnectionString,$credencial)
$acaoSolicitada=$Action
$fase='CONEXAO';$codigo=$null;$resultado='FALHA';$dev=$false;$ddlSolicitado=$false;$metadados=$null
try {
    $c.Open();$cmd=$c.CreateCommand();$cmd.CommandTimeout=15
    $cmd.CommandText="IF @@TRANCOUNT<>0 THROW 51023,'CREATE exige autocommit.',1; SET IMPLICIT_TRANSACTIONS OFF; SELECT CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')),DB_NAME(),ORIGINAL_LOGIN()"
    $r=$cmd.ExecuteReader()
    if(-not $r.Read()){throw 'D20_IDENTIDADE_AUSENTE'}
    $servidor=$r.GetString(0);$banco=$r.GetString(1);$login=$r.GetString(2);$r.Close()
    Assert-WmsD20Identidade $servidor $banco $v.WMS_DB_CONFIRMED_SERVER 'master'
    if($login -cne $v.WMS_DB_CONFIRMED_LOGIN){throw 'D20_LOGIN_REAL_DIVERGENTE'}
    $fase='EXISTENCIA';$cmd.CommandText="SELECT name,state FROM sys.databases WHERE name IN (N'WMS_DEV',N'WMS_PROD')"
    $r=$cmd.ExecuteReader();$existe=$false;$nomeExistente='';$onlineExistente=$false
    while($r.Read()) {
        $n=$r.GetString(0)
        if($n -ieq $Database){$existe=$true;$nomeExistente=$n;$onlineExistente=($r.GetByte(1) -eq 0)}
        if($n -ceq 'WMS_DEV' -and $r.GetByte(1) -eq 0){$dev=$true}
    }
    $r.Close()
    # Observacao ATUAL na mesma sessao: corrida desde o launcher vira Check, nunca DDL.
    $Action=Get-WmsD20AcaoObservada $Action $existe
    if($Action -eq 'Check'){
        $fase='EXISTENTE_CONFERIR'
        if(-not $existe){throw 'D20_EXISTENTE_NAO_ENCONTRADO'}
        Assert-WmsD20BancoExistente $nomeExistente $Database $onlineExistente
        if($Database -ceq 'WMS_PROD' -and -not $dev){throw 'D20_DEV_PRIMEIRO'}
    }else{Assert-WmsD20Criacao $Database $existe $dev}
    if($Database -ceq 'WMS_PROD'){
        $fase='DEV_PRIMEIRO_CONFERIR_METADADOS';Assert-WmsD20MetadadosExistente (Read-WmsD20Vazio $c 'WMS_DEV') 'WMS_DEV'
    }
    if($Action -eq 'Create'){
    $fase='PREFLIGHT_MODEL';Assert-WmsD20Vazio (Read-WmsD20Vazio $c 'model') 'model'
    $fase='CREATE';$cmd.CommandTimeout=120
    $cmd.CommandText=[IO.File]::ReadAllText((Join-Path $WmsD20Pathcriarbancos ('criacao/CREATE_'+$Database+'.sql')))
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
    }
    $fase=if($Action -eq 'Check'){'EXISTENTE_METADADOS_COMPLETOS'}else{'NOVO_VAZIO_CONFIRMAR'}
    # Database foi validado contra dois literais. Nao ha identificador livre.
    $metadados=Read-WmsD20Vazio $c $Database
    if($Action -eq 'Check'){Assert-WmsD20MetadadosExistente $metadados $Database}else{Assert-WmsD20Vazio $metadados $Database}
    $resultado=if($Action -eq 'Check'){'EXISTENTE_PRESERVADO_CONFERIDO'}else{'CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO'}
} catch {
    # Somente codigo, nunca ConnectionString, senha/login real ou mensagem de driver.
    $e=$_.Exception
    while($e.InnerException){$e=$e.InnerException}
    $codigo=if($e -is [System.Data.SqlClient.SqlException]){$e.Number}elseif($e.Message -cmatch '^D20_[A-Z0-9_]+$'){$e.Message}else{'GUARDA_OU_CONFIRMACAO'}
} finally { $c.Dispose();$credencial.Password.Dispose() }
$evidencia=[pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');banco=$Database;acaoSolicitada=$acaoSolicitada;acao=$Action;
    fase=$fase;resultado=$resultado;codigo=$codigo;devOnlineComprovado=$dev;ddlSolicitado=$ddlSolicitado;
    novo=($Action -eq 'Create');metadados=$metadados;conteudoExistenteAlterado=$false;
    migrationsExecutadas=$false;cargasExecutadas=$false;grantsExecutados=$false}
$arquivo=Join-Path $WmsD20Pathcriarbancos ('evidencias/d20-create-'+$Database+'-'+[Guid]::NewGuid().ToString('N')+'.json')
$evidencia|ConvertTo-Json|Set-Content -LiteralPath $arquivo -Encoding UTF8
$evidencia|ConvertTo-Json
if($resultado -notin @('CREATE_ONLINE_VAZIO_ACESSOS_PADRAO_CONFIRMADO','EXISTENTE_PRESERVADO_CONFERIDO')){throw "D20_BANCO_FALHOU_${fase}_${codigo}"}
