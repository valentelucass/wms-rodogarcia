# Contrato compartilhado autorizado em 06/10/2026. Nenhuma conexao ao importar.
$WmsRuntimeModule=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../.runtime/sql-server/SqlServerProjetos.psm1'))
if(-not(Test-Path -LiteralPath $WmsRuntimeModule)){throw 'D24_RUNTIME_COMPARTILHADO_AUSENTE'}
Import-Module $WmsRuntimeModule
. (Join-Path $PSScriptRoot 'confianca-atual.ps1')
function Read-WmsRuntimeSenha {
    $cred=Get-ProjetosSqlAdminCredential
    try{$copy=$cred.Password.Copy();$copy.MakeReadOnly();$copy}finally{$cred.Password.Dispose()}
}
function New-WmsRuntimeConnection([string]$Banco,$Credencial,[string]$ApplicationName){
    if($Banco -cnotin @('master','WMS_DEV','WMS_PROD')){throw 'D24_BANCO_NAO_AUTORIZADO'}
    if($Credencial.UserId -cne 'sa'){throw 'D24_LOGIN_BOOTSTRAP_DIVERGENTE'}
    $tls=Get-WmsRuntimeTrust
    $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
    $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog($Banco)
    $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory)
    $builder.set_TrustServerCertificate($false);$builder.set_ServerCertificate($tls.certificate)
    $builder.set_ApplicationName($ApplicationName);$builder.set_Pooling($false)
    $builder.set_ConnectTimeout(120);$builder.set_ConnectRetryCount(0)
    $builder.set_TransparentNetworkIPResolution($false);$builder.set_MultiSubnetFailover($false)
    $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential($Credencial.UserId,$Credencial.Password)
    New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
}
