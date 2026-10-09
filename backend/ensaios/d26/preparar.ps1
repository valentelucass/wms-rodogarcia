param([string]$Jar='target-d26-financeiro/wms-backend-0.0.1-SNAPSHOT.jar')
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$java='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin'
$work=Join-Path $backend 'target-d26-helper'
if(-not(Test-Path $work)){New-Item -ItemType Directory -Path $work|Out-Null}
$lib=Join-Path $work 'lib';New-Item -ItemType Directory -Path $lib -Force|Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar=Join-Path $backend $Jar
$z=[IO.Compression.ZipFile]::OpenRead($jar)
try{foreach($e in $z.Entries|Where-Object {$_.FullName.StartsWith('BOOT-INF/lib/') -and $_.Name.EndsWith('.jar')}){[IO.Compression.ZipFileExtensions]::ExtractToFile($e,(Join-Path $lib $e.Name),$true)}}finally{$z.Dispose()}
& "$java/javac.exe" -encoding UTF-8 -cp "$lib/*" -d $work "$PSScriptRoot/FixtureHttps.java" "$PSScriptRoot/EnsaioD26.java" "$PSScriptRoot/JornadasD26.java" "$PSScriptRoot/ComplementosD26.java" "$PSScriptRoot/ProcessoLimitadoD26Test.java" "$PSScriptRoot/RespostaConcorrenteD26Test.java"
if($LASTEXITCODE -ne 0){throw 'D26_HELPER_COMPILACAO_FALHOU'}
Write-Output 'D26 helper compilado com bibliotecas do JAR atual/JDK21'
