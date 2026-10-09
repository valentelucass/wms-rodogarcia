[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'login-protegido.ps1')
$path = Get-WmsLoginMaterialPath
if (Test-Path -LiteralPath $path) { throw 'Configuracao ja existe. Nao sobrescrever chaves ou senha inicial.' }
$java = Join-Path $env:LOCALAPPDATA 'Programs/Eclipse Adoptium/jdk-21/bin/java.exe'
if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'JDK 21 nao encontrado no caminho WMS. Configure o caminho local antes de continuar.' }
$password = Read-Host 'Senha inicial do administrador principal (minimo 12 caracteres)' -AsSecureString
$confirmation = Read-Host 'Repita a senha inicial' -AsSecureString
$first = [IntPtr]::Zero; $second = [IntPtr]::Zero; $process = $null
try {
    $first = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)
    $second = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($confirmation)
    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($first)
    if ($plain -cne [Runtime.InteropServices.Marshal]::PtrToStringBSTR($second) -or $plain.Length -lt 12 -or $plain.Length -gt 128 -or $plain -match '[\r\n]') { throw 'Senhas diferentes ou fora do limite de 12 a 128 caracteres.' }
    $directory = [IO.Path]::GetDirectoryName($path)
    if (Test-Path -LiteralPath $directory) { Assert-WmsLoginPrivatePath $directory }
    else {
        $null = New-Item -ItemType Directory -Path $directory
        $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl = New-Object Security.AccessControl.DirectorySecurity
        $acl.SetAccessRuleProtection($true, $false)
        $acl.SetOwner($sid)
        $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')))
        Set-Acl -LiteralPath $directory -AclObject $acl
    }
    Assert-WmsLoginPrivatePath $directory
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $java
    $info.Arguments = '"' + (Join-Path $PSScriptRoot 'PrepararLogin.java') + '"'
    $info.UseShellExecute = $false; $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true; $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
    $info.EnvironmentVariables.Remove('JAVA_TOOL_OPTIONS'); $info.EnvironmentVariables.Remove('_JAVA_OPTIONS'); $info.EnvironmentVariables.Remove('JDK_JAVA_OPTIONS')
    $process = [Diagnostics.Process]::Start($info)
    $output = $process.StandardOutput.ReadToEndAsync(); $errors = $process.StandardError.ReadToEndAsync()
    $writer = [IO.StreamWriter]::new($process.StandardInput.BaseStream, [Text.UTF8Encoding]::new($false))
    try { $writer.WriteLine($plain); $writer.Flush() } finally { $plain = $null; $writer.Dispose() }
    if (-not $process.WaitForExit(60000)) { $process.Kill(); throw 'Configuracao excedeu o prazo.' }
    $null = $errors.Result
    if ($process.ExitCode -ne 0) { throw 'Falha na geracao protegida. Nenhuma configuracao salva.' }
    $secured = ConvertTo-SecureString -String $output.Result -AsPlainText -Force
    try {
        $secured | Export-Clixml -LiteralPath $path -Encoding UTF8 -NoClobber
        $fileAcl = Get-Acl -LiteralPath $path
        $fileAcl.SetAccessRuleProtection($true, $true)
        Set-Acl -LiteralPath $path -AclObject $fileAcl
        Assert-WmsLoginPrivatePath $path
    } finally { $secured.Dispose() }
    Write-Host 'Configuracao WMS protegida salva fora do repositorio. A senha sera usada somente na primeira criacao da conta.'
} finally {
    $plain = $null
    if ($first -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($first) }
    if ($second -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($second) }
    $password.Dispose(); $confirmation.Dispose()
    if ($process) { $process.Dispose() }
}
