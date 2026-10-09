# Material exclusivo WMS DEV. Biblioteca sem abertura SQL ou inicio de processos.
function Get-WmsLoginMaterialPath {
    Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/WMS/auth-dev/login.clixml'
}

function Assert-WmsLoginPrivatePath([string]$Path) {
    $resolved = [IO.Path]::GetFullPath($Path)
    $cursor = $resolved
    while ($cursor) {
        if (Test-Path -LiteralPath $cursor) {
            if ((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'AUTH_CAMINHO_LINK_RECUSADO' }
        }
        $parent = [IO.Path]::GetDirectoryName($cursor)
        if ($parent -eq $cursor) { break }
        $cursor = $parent
    }
    $sid = [Security.Principal.WindowsIdentity]::GetCurrent().User.Value
    $acl = Get-Acl -LiteralPath $resolved
    if (-not $acl.AreAccessRulesProtected) { throw 'AUTH_ACL_NAO_PRIVADA' }
    foreach ($rule in $acl.Access) {
        if ($rule.AccessControlType -eq 'Allow' -and $rule.IdentityReference.Translate([Security.Principal.SecurityIdentifier]).Value -notin @($sid, 'S-1-5-18', 'S-1-5-32-544')) { throw 'AUTH_ACL_NAO_PRIVADA' }
    }
}

function Get-WmsLoginIdentity([int]$FrontendPort) {
    . (Join-Path $PSScriptRoot '../dev/acesso-dev.ps1')
    $access=Get-WmsDevAccess -FrontendPort $FrontendPort
    $path = Get-WmsLoginMaterialPath
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw 'AUTH_CONFIGURAR_LOGIN_UMA_VEZ' }
    Assert-WmsLoginPrivatePath ([IO.Path]::GetDirectoryName($path))
    Assert-WmsLoginPrivatePath $path
    $material = Import-Clixml -LiteralPath $path
    if ($material -isnot [Security.SecureString]) { throw 'AUTH_MATERIAL_INVALIDO' }
    # Issuer e JWK sao identificadores do emissor proprio, nunca descoberta externa.
    @{ native=$true; issuer='https://wms.localhost'; jwkSetUri='https://wms.localhost/api/auth/jwks';
       audience='wms-dev'; origin=$access.origin; proxyOrigin=$access.proxyOrigin;
       secureCookie=$access.secureCookie; accessMode=$access.mode; protectedMaterial=$material }
}
