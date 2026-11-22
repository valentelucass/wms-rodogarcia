#requires -Version 5.1
[CmdletBinding()]
param([switch]$Aplicar)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'login-protegido.ps1')
$path=Get-WmsLoginMaterialPath
$directory=Split-Path $path -Parent
Assert-WmsLoginPrivatePath $directory
Assert-WmsLoginPrivatePath $path
if(!$Aplicar){Write-Output 'Plano: recuperar somente o arquivo de assinatura ilegivel do site DEV.';return}

$mutex=[Threading.Mutex]::new($false,'Global\WMS-Assinatura-Site-DEV')
$owned=$false;$process=$null;$secret=$null;$check=$null;$temp=$null
try {
    try{$owned=$mutex.WaitOne(0)}catch [Threading.AbandonedMutexException]{$owned=$true}
    if(!$owned){throw 'AUTH_RECUPERACAO_EM_ANDAMENTO'}
    $originalHash=(Get-FileHash -LiteralPath $path).Hash
    try {
        $current=Import-Clixml -LiteralPath $path
        if($current -isnot [Security.SecureString]){throw 'AUTH_MATERIAL_INVALIDO'}
        $current.Dispose()
        Write-Output 'AUTH_DEV_MATERIAL_LEGIVEL_PRESERVADO'
        return
    }catch {
        if($_.Exception.GetBaseException() -isnot [Security.Cryptography.CryptographicException]){throw}
    }
    $repo=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
    $jdk=(Get-Content -LiteralPath (Join-Path $repo 'database/config/java21.json') -Raw | ConvertFrom-Json).java
    $java=[Environment]::ExpandEnvironmentVariables($jdk.path)
    if((Get-FileHash -LiteralPath $java).Hash -ine $jdk.sha256){throw 'AUTH_JDK_DIVERGENTE'}
    $info=[Diagnostics.ProcessStartInfo]::new()
    $info.FileName=$java
    $info.Arguments='"'+(Join-Path $PSScriptRoot 'PrepararLogin.java')+'"'
    $info.UseShellExecute=$false;$info.CreateNoWindow=$true
    $info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
    foreach($name in @('JAVA_TOOL_OPTIONS','_JAVA_OPTIONS','JDK_JAVA_OPTIONS')){$info.EnvironmentVariables.Remove($name)}
    $process=[Diagnostics.Process]::Start($info)
    $output=$process.StandardOutput.ReadToEndAsync()
    $errors=$process.StandardError.ReadToEndAsync()
    # O envelope historico exige um hash de bootstrap. O launcher integrado mantem o bootstrap desativado.
    $entropy=New-Object byte[] 32
    $rng=[Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($entropy)
        $process.StandardInput.WriteLine([Convert]::ToBase64String($entropy))
        $process.StandardInput.Close()
    }finally{$rng.Dispose();[Array]::Clear($entropy,0,$entropy.Length)}
    if(!$process.WaitForExit(60000)){$process.Kill();throw 'AUTH_GERACAO_TIMEOUT'}
    $null=$errors.Result
    if($process.ExitCode -ne 0){throw 'AUTH_GERACAO_FALHOU'}
    $material=ConvertFrom-Json -InputObject $output.Result
    if($material.schema -ne 1 -or $material.privateKey -cnotmatch '^[A-Za-z0-9+/=]{1000,}$' -or
        $material.bootstrapHash -cnotmatch '^\{pbkdf2-600k\}[a-f0-9]{96}$'){throw 'AUTH_GERACAO_INVALIDA'}
    $secret=ConvertTo-SecureString -String $output.Result -AsPlainText -Force
    $temp=Join-Path $directory ('renovacao-'+[guid]::NewGuid().ToString('N')+'.clixml')
    $secret | Export-Clixml -LiteralPath $temp -NoClobber
    Set-Acl -LiteralPath $temp -AclObject (Get-Acl -LiteralPath $path)
    Assert-WmsLoginPrivatePath $temp
    $check=Import-Clixml -LiteralPath $temp
    if($check -isnot [Security.SecureString] -or $check.Length -ne $secret.Length){throw 'AUTH_DPAPI_INVALIDA'}
    Assert-WmsLoginPrivatePath $path
    if((Get-FileHash -LiteralPath $path).Hash -cne $originalHash){throw 'AUTH_ARQUIVO_MUDOU'}
    $backup=Join-Path $directory ('login-anterior-'+[guid]::NewGuid().ToString('N')+'.clixml')
    [IO.File]::Replace($temp,$path,$backup)
    Assert-WmsLoginPrivatePath $backup
    $identity=Get-WmsLoginIdentity 25581
    $identity.protectedMaterial.Dispose()
    Write-Output 'AUTH_DEV_ASSINATURA_RECUPERADA'
}finally{
    if($temp -and (Test-Path -LiteralPath $temp)){Remove-Item -LiteralPath $temp -Force}
    if($check -is [Security.SecureString]){$check.Dispose()}
    if($secret){$secret.Dispose()};if($process){$process.Dispose()}
    if($owned){$mutex.ReleaseMutex()};$mutex.Dispose()
}
