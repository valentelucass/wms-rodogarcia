# Armazena credencial JA provisionada no SQL. Nao cria login/grants/schema nem conecta.
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot '../../backend/scripts/prod/credential-prod.ps1')
if (Test-Path -LiteralPath (Get-WmsProductionCredentialPath)) { throw 'PROD_CREDENTIAL_EXISTING_PRESERVED' }
$password = Read-Host 'Senha da conta SQL WMSPROD ja provisionada (entrada oculta)' -AsSecureString
$confirmation = Read-Host 'Repita a senha' -AsSecureString
$first = [IntPtr]::Zero; $second = [IntPtr]::Zero
try {
    $first = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $second = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirmation)
    if ([Runtime.InteropServices.Marshal]::PtrToStringBSTR($first) -cne [Runtime.InteropServices.Marshal]::PtrToStringBSTR($second)) { throw 'PROD_CREDENTIAL_CONFIRMATION_MISMATCH' }
    $null = Save-WmsProductionApplicationCredential $password
    Write-Host '[WMS PROD] Credencial cifrada com DPAPI e ACL privada. Nenhuma conexao ou alteracao SQL executada.'
} finally {
    if ($first -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($first) }
    if ($second -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($second) }
    $password.Dispose(); $confirmation.Dispose()
}
