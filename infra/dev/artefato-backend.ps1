Set-StrictMode -Version Latest

function Get-WmsBackendSourceHash([string]$Repository) {
    $root = [IO.Path]::GetFullPath($Repository).TrimEnd('\', '/')
    $files = @(Get-Item -LiteralPath (Join-Path $root 'backend/pom.xml')) +
        @(Get-ChildItem -LiteralPath (Join-Path $root 'backend/src/main') -File -Recurse)
    $lines = @($files | ForEach-Object {
        $relative = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
        $relative + '=' + (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    } | Sort-Object)
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(
            ($lines -join "`n"))))).Replace('-', '').ToLowerInvariant()
    } finally { $sha.Dispose() }
}

function Assert-WmsDevApplicationArtifact([string]$Repository, [string]$ReceiptPath) {
    $artifact = Get-Content -LiteralPath $ReceiptPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($artifact.PSObject.Properties.Name -notcontains 'sourceHash' -or
        $artifact.sourceHash -cne (Get-WmsBackendSourceHash $Repository)) {
        throw 'DEV02_ARTIFACT_SOURCE_STALE'
    }
    $root = [IO.Path]::GetFullPath($Repository).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $jar = [IO.Path]::GetFullPath($artifact.jar.path)
    if (-not $jar.StartsWith($root, [StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $jar -PathType Leaf) -or
        (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ine $artifact.jar.sha256) {
        throw 'DEV02_ARTIFACT_JAR_INVALID'
    }
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($entry in @(
            'controllers/VisaoOperacaoController.class', 'services/VisaoOperacaoService.class',
            'controllers/DashboardController.class', 'config/LoginConfig.class',
            'config/CadastrosSegurancaConfig.class')) {
            if ($null -eq $zip.GetEntry('BOOT-INF/classes/br/com/rodogarcia/wms/' + $entry)) {
                throw 'DEV02_ARTIFACT_API_MISSING'
            }
        }
    } finally { $zip.Dispose() }
    return $artifact
}
