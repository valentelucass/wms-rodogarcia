[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RunDirectory,[switch]$Executar)
$ErrorActionPreference='Stop'
$repository=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot '../sql01/launcher-backend.ps1')
$area=Join-Path $repository 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs'
$run=Resolve-WmsSql01Path $RunDirectory $area
$uuid=[guid]::Empty
if(-not [guid]::TryParseExact([IO.Path]::GetFileName($run),'D',[ref]$uuid) -or !(Test-Path -LiteralPath $run -PathType Container)){throw 'DEV_CURRENT_RUN_INVALID'}
if(-not $Executar){
    # Somente a compilacao entra no Job Object; os servidores permanentes ficam no launcher.
    $controller=[IO.Path]::GetFullPath((Join-Path $repository '../.runtime/memory-control/Executar-Batch.ps1'))
    if(!(Test-Path -LiteralPath $controller -PathType Leaf)){throw 'DEV_CURRENT_MEMORY_CONTROLLER_MISSING'}
    & $controller -Executable (Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe') -ArgumentList @('-NoProfile','-File',$PSCommandPath,'-RunDirectory',$run,'-Executar') -WorkingDirectory (Join-Path $repository 'backend') -ReceiptPath (Join-Path $run 'build-memory.json')
    exit $LASTEXITCODE
}
$target=Join-Path $run 'target'
$evidence=Join-Path $run 'build-pure-evidence'
foreach($path in @($target,$evidence)){if($path -match '["\r\n%!]'){throw 'DEV_CURRENT_BUILD_PATH_INVALID'}}
$env:MAVEN_OPTS='-Xmx768m -XX:ActiveProcessorCount=2'
Set-Location -LiteralPath (Join-Path $repository 'backend')
& .\mvnw.cmd -B ('-Dwms.build.directory='+$target) ('-Dwms.test.evidencias.dir='+$evidence) '-Dmaven.test.skip=true' '-Ppure-no-db,!h2-historical,!sqlserver-it,!migrations,!migrations-wrapper-confirmado,!bootstrap-local,!bootstrap-local-wrapper-confirmado' package
exit $LASTEXITCODE
