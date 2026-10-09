param([ValidatePattern('^target-d29-[a-z0-9-]+$')][string]$BuildTarget='target-d29-atual')
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$work=Join-Path $backend 'target-d29-helper'
$files=@(Get-ChildItem -LiteralPath $PSScriptRoot -File | Where-Object Extension -in @('.java','.ps1','.py'))
$files+=@(Get-ChildItem -LiteralPath (Join-Path $backend 'src/test') -Recurse -File)
$files+=@(Get-ChildItem -LiteralPath (Join-Path $backend 'src/main') -Recurse -File)
$classes=@(Get-ChildItem -LiteralPath $work -File -Filter '*.class')
$testClasses=Join-Path $backend "$BuildTarget/test-classes"
if(Test-Path -LiteralPath $testClasses){$classes+=@(Get-ChildItem -LiteralPath $testClasses -Recurse -File)}
$mainClasses=Join-Path $backend "$BuildTarget/classes"
if(Test-Path -LiteralPath $mainClasses){$classes+=@(Get-ChildItem -LiteralPath $mainClasses -Recurse -File)}
$jar=Join-Path $backend "$BuildTarget/wms-backend-0.0.1-SNAPSHOT.jar"
if(Test-Path -LiteralPath $jar){$classes+=@(Get-Item -LiteralPath $jar)}
$rows=@(foreach($f in $files+$classes){$fileHash=(Get-FileHash -LiteralPath $f.FullName -Algorithm SHA256).Hash;$kind=if($files.FullName -contains $f.FullName){'fontes'}else{'classes'};[ordered]@{arquivo=$f.FullName.Substring($backend.Length+1).Replace('\','/');sha256=$fileHash;destinoArchive=$kind+'/'+$fileHash+$f.Extension}})
$ordered=@($rows | Sort-Object arquivo)
$bytes=[Text.Encoding]::UTF8.GetBytes(($ordered|ConvertTo-Json -Compress))
$sha=[Security.Cryptography.SHA256]::Create()
try{$hash=([BitConverter]::ToString($sha.ComputeHash($bytes))).Replace('-','')}finally{$sha.Dispose()}
$archive=Join-Path $work "versionados/$hash"
if(-not(Test-Path -LiteralPath $archive)){
 New-Item -ItemType Directory -Path (Join-Path $archive 'fontes') -Force|Out-Null
 New-Item -ItemType Directory -Path (Join-Path $archive 'classes') -Force|Out-Null
 foreach($row in $ordered){$dest=Join-Path $archive $row.destinoArchive;if(-not(Test-Path -LiteralPath $dest)){Copy-Item -LiteralPath (Join-Path $backend $row.arquivo) -Destination $dest}}
}
foreach($row in $ordered){$dest=Join-Path $archive $row.destinoArchive;if(-not(Test-Path -LiteralPath $dest) -or (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash -cne $row.sha256){throw 'D29_ARCHIVE_PARCIAL_OU_DIVERGENTE_NAO_PUBLICADO'}}
$manifest=[ordered]@{incremento='D29';capturadoUtc=[DateTime]::UtcNow.ToString('o');hashConjunto=$hash;arquivoArchive=$archive.Substring($backend.Length+1).Replace('\','/');fontes=$files.Count;compilados=$classes.Count;arquivos=$ordered;segredos=$false}
$path=Join-Path $backend "evidencias/d29-helper-versao-$hash.json"
if(-not(Test-Path -LiteralPath $path)){[IO.File]::WriteAllText($path,($manifest|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))}
[IO.File]::WriteAllText((Join-Path $work 'versao-atual.json'),($manifest|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
Write-Output "D29 fontes/classes preservados hash=$hash"
