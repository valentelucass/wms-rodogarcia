[CmdletBinding()]
param([ValidateSet('Plan','Inspect')][string]$Action='Plan',
      [Management.Automation.PSCredential]$CredencialLocal,[Security.SecureString]$SenhaLocal)
$WmsD20Pathverificaralvocriacao=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))

$ErrorActionPreference='Stop'
. (Join-Path $WmsD20Pathverificaralvocriacao 'scripts/d20-guardas.ps1')
. (Join-Path $PSScriptRoot 'd24-runtime.ps1')
if($Action -eq 'Plan'){
    [pscustomobject]@{natureza='PLANO_OFFLINE_SEM_CONEXAO';hostConfirmado='127.0.0.1';porta=1433;
        autenticacao='SQL';identidadeCriacao='sa';bancoInicial='master';
        credencial='AUSENTE_NO_CONTEXTO_PROTEGIDO';servidorReal='A_OBTER_EM_LEITURA_TLS';
        cria=$false;migra=$false;altera=$false}|ConvertTo-Json;return
}
# Bootstrap autorizado: endpoint exato Lucas. Nao inventa ServerName nem muda SQL Server.
# SecureString/PSCredential somente no console local. Sem senha em env/argumento literal.
$credencial=Get-WmsD20CredencialCriacao $CredencialLocal $SenhaLocal
try{
    $c=New-WmsRuntimeConnection 'master' $credencial 'WMS-D24-IDENTIDADE-LEITURA'
    try{
        $c.Open();$cmd=$c.CreateCommand();$cmd.CommandTimeout=15
        $cmd.CommandText="SELECT CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')),DB_NAME(),ORIGINAL_LOGIN(),CONVERT(varchar(30),SERVERPROPERTY('ProductVersion')); SELECT name,state FROM sys.databases WHERE name IN(N'WMS_DEV',N'WMS_PROD');"
        $r=$cmd.ExecuteReader();if(-not $r.Read()){throw 'D20_IDENTIDADE_AUSENTE'}
        $srv=$r.GetString(0);$db=$r.GetString(1);$login=$r.GetString(2);$versao=$r.GetString(3)
        if($db -cne 'master' -or $login -cne 'sa' -or [string]::IsNullOrWhiteSpace($srv)){throw 'D20_BOOTSTRAP_IDENTIDADE_DIVERGENTE'}
        $existentes=@();$null=$r.NextResult()
        while($r.Read()){$estado=$r.GetByte(1);$existentes+=[pscustomobject]@{banco=$r.GetString(0);estado=$estado;online=($estado -eq 0)}};$r.Close()
        [pscustomobject]@{natureza='INSPECAO_REAL_SOMENTE_LEITURA';endpoint='127.0.0.1:1433';
            servidorReal=$srv;bancoInicial=$db;loginCriacao=$login;versao=$versao;existentes=$existentes;
            proximo='FAROL_CONFERIR_SERVERNAME_ANTES_CREATE';cria=$false;migra=$false;altera=$false}|ConvertTo-Json -Depth 5
    }catch{throw 'D20_INSPECAO_FALHOU_TLS_ACESSO_IDENTIDADE_CONFERIR_SEM_BYPASS'}
    finally{if($null -ne $c){$c.Dispose()}}
}finally{
    $credencial.Password.Dispose()
}
