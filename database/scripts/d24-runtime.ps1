# Contrato compartilhado autorizado em 06/10/2026. Nenhuma conexao ao importar.
$WmsRuntimeModule=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../.runtime/sql-server/SqlServerProjetos.psm1'))
if(-not(Test-Path -LiteralPath $WmsRuntimeModule)){throw 'D24_RUNTIME_COMPARTILHADO_AUSENTE'}
Import-Module $WmsRuntimeModule
function Read-WmsRuntimeSenha {
    $cred=Get-ProjetosSqlAdminCredential
    try{$copy=$cred.Password.Copy();$copy.MakeReadOnly();$copy}finally{$cred.Password.Dispose()}
}
function New-WmsRuntimeConnection([string]$Banco,$Credencial,[string]$ApplicationName){
    if($Banco -cnotin @('master','WMS_DEV','WMS_PROD')){throw 'D24_BANCO_NAO_AUTORIZADO'}
    if($Credencial.UserId -cne 'sa'){throw 'D24_LOGIN_BOOTSTRAP_DIVERGENTE'}
    New-ProjetosSqlConnection -Database $Banco -Credential $Credencial -ApplicationName $ApplicationName
}
