[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidencePath)
$ErrorActionPreference='Stop'
$collectorEvidencePath=[IO.Path]::GetFullPath($EvidencePath)
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
$stream=[IO.File]::Open($collectorEvidencePath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
try{
    $r=Invoke-WmsDev02Guard -ColetorPreMigration
    $bytes=[Text.UTF8Encoding]::new($false).GetBytes(($r|ConvertTo-Json -Depth 20))
    $stream.Write($bytes,0,$bytes.Length)
    [pscustomobject]@{estado=$r.estado;aprovada=$r.guardaRealAprovada;checks=@($r.checks).Count;erro=$r.erro}|ConvertTo-Json -Depth 4
}finally{$stream.Dispose()}
if(!$r.guardaRealAprovada){exit 20}
