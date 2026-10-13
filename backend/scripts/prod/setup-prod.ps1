# Preparacao local exclusivamente PROD; nenhuma conexao, conta SQL ou usuario WMS criado.
. (Join-Path $PSScriptRoot 'identity-prod.ps1')

function New-WmsProductionPrivateDirectory {
    param([string]$Path)
    $cursor = [IO.Path]::GetFullPath($Path)
    while ($cursor) {
        if ((Test-Path -LiteralPath $cursor) -and
            ((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'PROD_PROTECTED_REPARSE_REFUSED' }
        $cursor = [IO.Path]::GetDirectoryName($cursor)
    }
    if (-not (Test-Path -LiteralPath $Path)) {
        $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User
        $acl = New-Object Security.AccessControl.DirectorySecurity
        $acl.SetAccessRuleProtection($true, $false); $acl.SetOwner($sid)
        $acl.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule($sid, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')))
        $null = [IO.Directory]::CreateDirectory($Path, $acl)
    }
    Assert-WmsProductionPrivatePath $Path
}

function Protect-WmsProductionNewFile {
    param([string]$Path)
    $acl = Get-Acl -LiteralPath $Path
    $acl.SetAccessRuleProtection($true, $true)
    Set-Acl -LiteralPath $Path -AclObject $acl
    Assert-WmsProductionPrivatePath $Path
}

function Initialize-WmsProductionIdentity {
    $ErrorActionPreference = 'Stop'
    $directory = Get-WmsProductionAuthDirectory
    New-WmsProductionPrivateDirectory $directory
    $lock = $null; $process = $null; $secured = $null; $plainKey = $null
    try {
        $lock = [IO.File]::Open((Join-Path $directory 'configuracao.lock'), [IO.FileMode]::OpenOrCreate, [IO.FileAccess]::ReadWrite, [IO.FileShare]::None)
        $metadataPath = Join-Path $directory 'identity.json'
        $materialPath = Join-Path $directory 'login.clixml'
        if ((Test-Path -LiteralPath $metadataPath) -or (Test-Path -LiteralPath $materialPath)) {
            foreach ($p in @($metadataPath, $materialPath)) { Assert-WmsProductionPrivatePath $p }
            return Get-WmsProductionIdentity
        }
        $java = Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Programs/Eclipse Adoptium/jdk-21/bin/java.exe'
        if (-not (Test-Path -LiteralPath $java -PathType Leaf)) { throw 'PROD_JDK21_REQUIRED' }
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $java; $info.Arguments = '"' + (Join-Path $PSScriptRoot 'PrepararChave.java') + '"'
        $info.UseShellExecute = $false; $info.CreateNoWindow = $true
        $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
        $info.EnvironmentVariables.Clear()
        foreach ($name in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE','LOCALAPPDATA')) {
            $value = [Environment]::GetEnvironmentVariable($name,'Process')
            if ($value) { $info.EnvironmentVariables[$name] = $value }
        }
        $process = [Diagnostics.Process]::Start($info)
        $output = $process.StandardOutput.ReadToEndAsync(); $errors = $process.StandardError.ReadToEndAsync()
        if (-not $process.WaitForExit(60000)) { $process.Kill(); $process.WaitForExit(); throw 'PROD_AUTH_KEY_GENERATION_TIMEOUT' }
        $null = $errors.Result
        if ($process.ExitCode -ne 0) { throw 'PROD_AUTH_KEY_GENERATION_FAILED' }
        $plainKey = $output.Result
        if ($plainKey -notmatch '^[A-Za-z0-9+/]{1000,4096}={0,2}$') { throw 'PROD_AUTH_KEY_GENERATION_FAILED' }
        $identity = [ordered]@{schema=1;native=$true;environment='PROD';database='WMS_PROD';
            origin='https://wms.rodogarcia.com.br';issuer='https://wms-api.rodogarcia.com.br';
            jwkSetUri='https://wms-api.rodogarcia.com.br/api/auth/jwks';audience='wms-prod';secureCookie=$true;proxyOrigin=''}
        $material = @{schema=1;environment='PROD';database='WMS_PROD';issuer=$identity.issuer;audience=$identity.audience;privateKey=$plainKey}
        $secured = ConvertTo-SecureString ($material | ConvertTo-Json -Compress) -AsPlainText -Force
        $plainKey = $null; $material = $null; $output = $null
        $secured | Export-Clixml -LiteralPath $materialPath -Encoding UTF8 -NoClobber
        Protect-WmsProductionNewFile $materialPath
        $stream = [IO.File]::Open($metadataPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
        try {
            $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($identity | ConvertTo-Json))
            $stream.Write($bytes,0,$bytes.Length)
        } finally { $stream.Dispose() }
        Protect-WmsProductionNewFile $metadataPath
        Get-WmsProductionIdentity
    } catch {
        if ($_.Exception.Message -cmatch '^PROD_[A-Z0-9_]+$') { throw $_.Exception.Message }
        throw 'PROD_AUTH_SETUP_REFUSED'
    } finally {
        $plainKey = $null; $material = $null; $output = $null
        if ($secured) { $secured.Dispose() }
        if ($process) { $process.Dispose() }
        if ($lock) { $lock.Dispose() }
    }
}
