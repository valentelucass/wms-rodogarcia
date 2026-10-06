param()
$ErrorActionPreference='Stop'
$rootDirectory=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$rootPrefix=[IO.Path]::GetFullPath($rootDirectory).TrimEnd('\')+'\'
$checked=0
foreach($line in [IO.File]::ReadAllLines((Join-Path $PSScriptRoot 'd21-freeze.sha256'))){
    if($line -notmatch '^([0-9A-F]{64})  (backend/.+)$'){throw 'Manifesto D21 invalido'}
    $expected=$Matches[1];$relative=$Matches[2]
    $absolute=[IO.Path]::GetFullPath((Join-Path $rootDirectory $relative))
    if(-not $absolute.StartsWith($rootPrefix,[StringComparison]::OrdinalIgnoreCase)){throw 'Alvo fora do projeto'}
    if((Get-FileHash -LiteralPath $absolute -Algorithm SHA256).Hash -cne $expected){throw "Hash divergente: $relative"}
    $checked++
}
if($checked -eq 0){throw 'Manifesto vazio'}
[pscustomobject]@{result='PASS';checkedFiles=$checked;manifest='backend/evidencias/d21-freeze.sha256'}
