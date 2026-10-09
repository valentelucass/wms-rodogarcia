param([string]$Jar='target-d28-atual/wms-backend-0.0.1-SNAPSHOT.jar')
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$java='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin'
$work=Join-Path $backend 'target-d28-helper'
if(-not(Test-Path $work)){New-Item -ItemType Directory -Path $work|Out-Null}
$lib=Join-Path $work 'lib';New-Item -ItemType Directory -Path $lib -Force|Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$jar=Join-Path $backend $Jar
$z=[IO.Compression.ZipFile]::OpenRead($jar)
try{foreach($e in $z.Entries|Where-Object {$_.FullName.StartsWith('BOOT-INF/lib/') -and $_.Name.EndsWith('.jar')}){$target=Join-Path $lib $e.Name;if(-not(Test-Path -LiteralPath $target)){[IO.Compression.ZipFileExtensions]::ExtractToFile($e,$target,$false)}elseif((Get-Item -LiteralPath $target).Length -ne $e.Length){throw 'D28_BIBLIOTECA_PREEXISTENTE_DIVERGENTE'}}}finally{$z.Dispose()}
& "$java/javac.exe" -encoding UTF-8 -cp "$lib/*" -d $work "$PSScriptRoot/FixtureHttps.java" "$PSScriptRoot/EnsaioD28.java" "$PSScriptRoot/JornadasD28.java" "$PSScriptRoot/PendenciasD28.java" "$PSScriptRoot/VariantesD28.java" "$PSScriptRoot/RespostaConcorrenteD28Test.java" "$PSScriptRoot/ReadinessD28Test.java" "$PSScriptRoot/ProcessoLimitadoD28Test.java" "$PSScriptRoot/RegressaoD28.java" "$PSScriptRoot/ConcorrenciaD28.java" "$PSScriptRoot/FixtureHttpsD28Test.java" "$PSScriptRoot/SaldoEntradaXmlD28.java"
if($LASTEXITCODE -ne 0){throw 'D28_HELPER_COMPILACAO_FALHOU'}
& "$java/javac.exe" -encoding UTF-8 -cp "$work;$lib/*" -d $work "$PSScriptRoot/SaldoEntradaXmlD28Test.java"
if($LASTEXITCODE -ne 0){throw 'D28_HELPER_FIXTURE_R16_COMPILACAO_FALHOU'}
& "$PSScriptRoot/versionar.ps1"
Write-Output 'D28 helper compilado com bibliotecas do JAR atual/JDK21'
