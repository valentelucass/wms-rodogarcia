# PROD-LAUNCH01. Somente metadados publicos; nunca importa DPAPI ao dot-source/Get.
function Get-WmsProductionAuthDirectory {
    Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/WMS/auth-prod'
}

function Assert-WmsProductionIdentity {
    param([Parameter(Mandatory = $true)][object]$Identity)
    if ($Identity.schema -ne 1 -or $Identity.native -isnot [bool] -or $Identity.native -ne $true -or
        $Identity.environment -cne 'PROD' -or $Identity.database -cne 'WMS_PROD' -or
        $Identity.origin -cne 'https://wms.rodogarcia.com.br' -or
        $Identity.issuer -cne 'https://wms-api.rodogarcia.com.br' -or
        $Identity.jwkSetUri -cne 'https://wms-api.rodogarcia.com.br/api/auth/jwks' -or
        $Identity.secureCookie -isnot [bool] -or $Identity.secureCookie -ne $true -or -not [string]::IsNullOrEmpty([string]$Identity.proxyOrigin) -or
        [string]::IsNullOrWhiteSpace([string]$Identity.audience) -or
        ([string]$Identity.audience).Length -gt 200 -or $Identity.audience -match '[\r\n]' -or
        $Identity.audience -imatch '(^|[^a-z])dev([^a-z]|$)') { throw 'PROD_AUTH_PUBLIC_METADATA_INVALID' }
    $directory = [IO.Path]::GetFullPath((Get-WmsProductionAuthDirectory))
    if ([IO.Path]::GetFullPath([string]$Identity.materialPath) -ine (Join-Path $directory 'login.clixml') -or
        [IO.Path]::GetFullPath([string]$Identity.metadataPath) -ine (Join-Path $directory 'identity.json') -or
        $Identity.metadataSha256 -notmatch '^[a-fA-F0-9]{64}$') { throw 'PROD_AUTH_CHANNEL_NOT_EXCLUSIVE' }
}

function Get-WmsProductionIdentity {
    [CmdletBinding()]
    param()
    $ErrorActionPreference = 'Stop'
    $directory = Get-WmsProductionAuthDirectory
    $metadata = Join-Path $directory 'identity.json'
    $material = Join-Path $directory 'login.clixml'
    if (-not (Test-Path -LiteralPath $metadata -PathType Leaf)) { throw 'PROD_AUTH_PUBLIC_METADATA_MISSING' }
    foreach ($path in @($directory, $metadata, $material)) {
        if (-not (Test-Path -LiteralPath $path)) { throw 'PROD_AUTH_PROTECTED_CHANNEL_MISSING' }
        if ((Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'PROD_AUTH_CHANNEL_REPARSE_REFUSED'
        }
    }
    try { $json = [IO.File]::ReadAllText($metadata) | ConvertFrom-Json } catch { throw 'PROD_AUTH_PUBLIC_METADATA_INVALID' }
    $allowed = @('schema', 'native', 'environment', 'database', 'origin', 'issuer', 'jwkSetUri', 'audience', 'secureCookie', 'proxyOrigin')
    if (@($json.PSObject.Properties.Name | Where-Object { $_ -cnotin $allowed }).Count) { throw 'PROD_AUTH_PUBLIC_METADATA_INVALID' }
    $identity = [pscustomobject]@{ schema = $json.schema; native = $json.native; environment = $json.environment
        database = $json.database; origin = $json.origin; issuer = $json.issuer; jwkSetUri = $json.jwkSetUri
        audience = $json.audience; secureCookie = $json.secureCookie; proxyOrigin = $json.proxyOrigin
        metadataPath = $metadata; metadataSha256 = (Get-FileHash -LiteralPath $metadata -Algorithm SHA256).Hash.ToLowerInvariant()
        materialPath = $material; protectedContentsRead = $false }
    Assert-WmsProductionIdentity $identity
    $identity
}

function Assert-WmsProductionPrivatePath {
    param([Parameter(Mandatory = $true)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { throw 'PROD_PROTECTED_PATH_MISSING' }
    if ((Get-Item -LiteralPath $Path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'PROD_PROTECTED_REPARSE_REFUSED' }
    $acl = Get-Acl -LiteralPath $Path
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    if (-not $acl.AreAccessRulesProtected -or $acl.Owner -notmatch [regex]::Escape($sid)) {
        try { $owner = (New-Object Security.Principal.NTAccount($acl.Owner)).Translate([Security.Principal.SecurityIdentifier]).Value }
        catch { throw 'PROD_PROTECTED_OWNER_INVALID' }
        if (-not $acl.AreAccessRulesProtected -or $owner -cne $sid) { throw 'PROD_PROTECTED_OWNER_INVALID' }
    }
    foreach ($rule in $acl.GetAccessRules($true, $true, [Security.Principal.SecurityIdentifier])) {
        if ($rule.AccessControlType -eq [Security.AccessControl.AccessControlType]::Allow -and
            $rule.IdentityReference.Value -cnotin @($sid, 'S-1-5-18', 'S-1-5-32-544')) { throw 'PROD_PROTECTED_ACL_TOO_BROAD' }
    }
}
