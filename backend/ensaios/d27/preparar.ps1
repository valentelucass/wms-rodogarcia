param([string]$Jar='target-d27-numerico-final/wms-backend-0.0.1-SNAPSHOT.jar')
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$java='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin'
$work=Join-Path $backend 'target-d27-helper'
if(-not(Test-Path $work)){New-Item -ItemType Directory -Path $work|Out-Null}
$lib=Join-Path $work 'lib';New-Item -ItemType Directory -Path $lib -Force|Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar=Join-Path $backend $Jar
$z=[IO.Compression.ZipFile]::OpenRead($jar)
try{foreach($e in $z.Entries|Where-Object {$_.FullName.StartsWith('BOOT-INF/lib/') -and $_.Name.EndsWith('.jar')}){[IO.Compression.ZipFileExtensions]::ExtractToFile($e,(Join-Path $lib $e.Name),$true)}}finally{$z.Dispose()}
& "$java/javac.exe" -encoding UTF-8 -cp "$lib/*" -d $work "$PSScriptRoot/FixtureHttps.java" "$PSScriptRoot/EnsaioD27.java" "$PSScriptRoot/JornadasD27.java" "$PSScriptRoot/PendenciasD27.java" "$PSScriptRoot/VariantesD27.java" "$PSScriptRoot/RespostaConcorrenteD27Test.java" "$PSScriptRoot/ReadinessD27Test.java"
if($LASTEXITCODE -ne 0){throw 'D27_HELPER_COMPILACAO_FALHOU'}
Write-Output 'D27 helper compilado com bibliotecas do JAR atual/JDK21'
