param()
$ErrorActionPreference = 'Stop'
$backendDirectory = Split-Path $PSScriptRoot -Parent
$projectDirectory = Split-Path $backendDirectory -Parent
$rootPrefix = [IO.Path]::GetFullPath($projectDirectory).TrimEnd('\') + '\'
$manifest = Join-Path $PSScriptRoot 'd20-freeze.sha256'
$checked = 0
foreach ($line in [IO.File]::ReadAllLines($manifest)) {
    if ([string]::IsNullOrWhiteSpace($line)) { continue }
    if ($line -notmatch '^([0-9A-Fa-f]{64})  (backend/.+)$') {
        throw 'Linha de manifesto invalida.'
    }
    $expected = $Matches[1]
    $relative = $Matches[2]
    $absolute = [IO.Path]::GetFullPath((Join-Path $projectDirectory $relative))
    if (-not $absolute.StartsWith($rootPrefix, [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Alvo fora do projeto.'
    }
    if (-not (Test-Path -LiteralPath $absolute -PathType Leaf)) {
        throw "Arquivo ausente: $relative"
    }
    if ((Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash -cne $expected) {
        throw "Hash divergente: $relative"
    }
    $checked++
}
if ($checked -eq 0) { throw 'Manifesto vazio.' }
[pscustomobject]@{ result = 'PASS'; checkedFiles = $checked; manifest = 'backend/evidencias/d20-freeze.sha256' }
