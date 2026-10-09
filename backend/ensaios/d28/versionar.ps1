$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$work=Join-Path $backend 'target-d28-helper'
$files=@(Get-ChildItem -LiteralPath $PSScriptRoot -File | Where-Object Extension -in @('.java','.ps1','.py'))
$classes=@(Get-ChildItem -LiteralPath $work -File -Filter '*.class')
$rows=@(foreach($f in $files+$classes){[ordered]@{arquivo=$f.FullName.Substring($backend.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash}})
$ordered=@($rows | Sort-Object arquivo)
$bytes=[Text.Encoding]::UTF8.GetBytes(($ordered|ConvertTo-Json -Compress))
$sha=[Security.Cryptography.SHA256]::Create()
try{$hash=([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-','')}finally{$sha.Dispose()}
$archive=Join-Path $work "versionados/$hash"
if(-not(Test-Path -LiteralPath $archive)){
 New-Item -ItemType Directory -Path (Join-Path $archive 'fontes') -Force|Out-Null
 New-Item -ItemType Directory -Path (Join-Path $archive 'classes') -Force|Out-Null
 foreach($f in $files){Copy-Item -LiteralPath $f.FullName -Destination (Join-Path $archive ('fontes/'+$f.Name))}
 foreach($f in $classes){Copy-Item -LiteralPath $f.FullName -Destination (Join-Path $archive ('classes/'+$f.Name))}
}
$manifest=[ordered]@{incremento='D28';capturadoUtc=[DateTime]::UtcNow.ToString('o');hashConjunto=$hash;arquivoArchive=$archive.Substring($backend.Length+1).Replace('\','/');fontes=$files.Count;compilados=$classes.Count;arquivos=$ordered;segredos=$false}
$path=Join-Path $backend "evidencias/d28-helper-versao-$hash.json"
if(-not(Test-Path -LiteralPath $path)){[IO.File]::WriteAllText($path,($manifest|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))}
[IO.File]::WriteAllText((Join-Path $work 'versao-atual.json'),($manifest|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "D28 fontes/classes preservados hash=$hash"
