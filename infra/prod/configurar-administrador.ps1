[CmdletBinding()]
param([switch]$FromStandardInput)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot '../../backend/scripts/prod/bootstrap-prod.ps1')
$path=Get-WmsProductionBootstrapPath
if(Test-Path -LiteralPath $path){throw 'PROD_BOOTSTRAP_EXISTING_PRESERVED'}
$plain=$null;$password=$null;$ptr=[IntPtr]::Zero;$hash=$null;$secured=$null;$kdf=$null
try{
    if($FromStandardInput){$plain=[Console]::In.ReadLine()}
    else{$password=Read-Host 'Senha inicial do administrador PROD' -AsSecureString;$ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($password);$plain=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)}
    if($null -eq $plain -or $plain.Length -lt 12 -or $plain.Length -gt 128 -or $plain -match '[\r\n]'){throw 'PROD_BOOTSTRAP_PASSWORD_LENGTH_INVALID'}
    $salt=New-Object byte[] 16;$rng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try{$rng.GetBytes($salt)}finally{$rng.Dispose()}
    $kdf=[Security.Cryptography.Rfc2898DeriveBytes]::new($plain,$salt,600000,[Security.Cryptography.HashAlgorithmName]::SHA256)
    $hash='{pbkdf2-600k}'+([BitConverter]::ToString($salt)).Replace('-','').ToLowerInvariant()+([BitConverter]::ToString($kdf.GetBytes(32))).Replace('-','').ToLowerInvariant()
    $data=[ordered]@{schema=1;environment='PROD';database='WMS_PROD';purpose='INITIAL_PRINCIPAL_ONLY';hash=$hash}
    $secured=ConvertTo-SecureString ($data|ConvertTo-Json -Compress) -AsPlainText -Force
    New-WmsProductionPrivateDirectory ([IO.Path]::GetDirectoryName($path))
    $secured|Export-Clixml -LiteralPath $path -NoClobber -Encoding UTF8
    Protect-WmsProductionNewFile $path
    Write-Output 'PROD_INITIAL_PRINCIPAL_MATERIAL_PROTECTED'
}finally{
    if($ptr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)}
    if($password){$password.Dispose()};if($secured){$secured.Dispose()};if($kdf){$kdf.Dispose()}
    $plain=$null;$hash=$null;$data=$null
}
