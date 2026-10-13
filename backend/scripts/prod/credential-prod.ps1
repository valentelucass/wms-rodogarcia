# Canal exclusivo PROD. Importar esta biblioteca nao le segredo nem conecta.
. (Join-Path $PSScriptRoot 'identity-prod.ps1')

function Get-WmsProductionCredentialPath {
    Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/WMS/api-prod/wmsprod-app.clixml'
}

function Get-WmsProductionCredentialMetadata {
    $path = Get-WmsProductionCredentialPath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'PROD_PROTECTED_APPLICATION_CHANNEL_MISSING' }
    foreach ($item in @([IO.Path]::GetDirectoryName($path), $path)) { Assert-WmsProductionPrivatePath $item }
    [pscustomobject]@{path=$path;sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant();database='WMS_PROD';login='WMSPROD';aclValida=$true}
}

function Get-WmsProductionApplicationCredential {
    param([string]$ExpectedSha256)
    $original = $null; $copy = $null
    try {
        $before = Get-WmsProductionCredentialMetadata
        if ($ExpectedSha256 -and $ExpectedSha256 -ine $before.sha256) { throw 'PROD_CREDENTIAL_CHANGED_SINCE_GUARD' }
        $original = Import-Clixml -LiteralPath $before.path -ErrorAction Stop
        if ($original -isnot [Management.Automation.PSCredential] -or
            $original.UserName -cne 'WMSPROD' -or $original.Password.Length -lt 32) { throw 'PROD_CREDENTIAL_IDENTITY_OR_TYPE_INVALID' }
        $after = Get-WmsProductionCredentialMetadata
        if ($before.sha256 -cne $after.sha256) { throw 'PROD_CREDENTIAL_CHANGED_DURING_READ' }
        $copy = $original.Password.Copy(); $copy.MakeReadOnly()
        New-Object Management.Automation.PSCredential('WMSPROD', $copy)
    } catch {
        if ($copy) { $copy.Dispose() }
        if ($_.Exception.Message -cmatch '^PROD_[A-Z0-9_]+$') { throw $_.Exception.Message }
        throw 'PROD_CREDENTIAL_DPAPI_REFUSED'
    } finally {
        if ($original -is [Management.Automation.PSCredential]) { $original.Password.Dispose() }
    }
}

function Save-WmsProductionApplicationCredential {
    param([Parameter(Mandatory=$true)][Security.SecureString]$Password)
    if ($Password.Length -lt 32 -or $Password.Length -gt 128) { throw 'PROD_CREDENTIAL_PASSWORD_LENGTH_INVALID' }
    . (Join-Path $PSScriptRoot 'setup-prod.ps1')
    $path = Get-WmsProductionCredentialPath
    $directory = [IO.Path]::GetDirectoryName($path)
    New-WmsProductionPrivateDirectory $directory
    if (Test-Path -LiteralPath $path) { throw 'PROD_CREDENTIAL_EXISTING_PRESERVED' }
    $copy = $Password.Copy()
    try {
        $credential = New-Object Management.Automation.PSCredential('WMSPROD', $copy)
        $credential | Export-Clixml -LiteralPath $path -NoClobber -Encoding UTF8
        Protect-WmsProductionNewFile $path
        Get-WmsProductionCredentialMetadata
    } finally { $copy.Dispose() }
}
