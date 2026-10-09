param([string]$Jar='target-d29-atual/wms-backend-0.0.1-SNAPSHOT.jar',[string]$BuildTarget='target-d29-atual')
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$java='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin'
$work=Join-Path $backend 'target-d29-helper';$lib=Join-Path $work 'lib'
New-Item -ItemType Directory -Path $lib -Force|Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem
$z=[IO.Compression.ZipFile]::OpenRead((Join-Path $backend $Jar))
try{foreach($e in $z.Entries|Where-Object {$_.FullName.StartsWith('BOOT-INF/lib/') -and $_.Name.EndsWith('.jar')}){
 $target=Join-Path $lib $e.Name
 if(-not(Test-Path -LiteralPath $target)){[IO.Compression.ZipFileExtensions]::ExtractToFile($e,$target,$false)}
 elseif((Get-Item -LiteralPath $target).Length -ne $e.Length){throw 'D29_BIBLIOTECA_PREEXISTENTE_DIVERGENTE'}
}}finally{$z.Dispose()}
$sources=@(Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.java' -File|ForEach-Object FullName)
& "$java/javac.exe" -J-Xmx256m -J-XX:+UseSerialGC -encoding UTF-8 -cp "$lib/*" -d $work $sources
if($LASTEXITCODE -ne 0){throw 'D29_COMPILACAO_HELPER_FALHOU'}
& "$PSScriptRoot/versionar.ps1" -BuildTarget $BuildTarget
Write-Output 'D29 todas fontes/classes arquivadas antes de executar'
