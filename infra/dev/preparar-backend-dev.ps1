[CmdletBinding()]
param([switch]$SomenteBuild,[string]$RunDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$repository=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'artefato-backend.ps1')
. (Join-Path $repository 'infra/sql01/launcher-backend.ps1')
$area=Join-Path $repository 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs'
if(!$RunDirectory){$RunDirectory=Join-Path $area ([guid]::NewGuid().ToString('D'));$null=New-Item -ItemType Directory -Path $RunDirectory}
$run=Resolve-WmsSql01Path $RunDirectory $area
$uuid=[guid]::Empty
if(![guid]::TryParseExact([IO.Path]::GetFileName($run),'D',[ref]$uuid) -or !(Test-Path -LiteralPath $run -PathType Container)){throw 'DEV_CURRENT_RUN_INVALID'}
$output=Join-Path $run 'preparo-candidato.json'
if(Test-Path -LiteralPath $output){throw 'DEV_CURRENT_CANDIDATE_EXISTS'}
$tool=Get-Content -LiteralPath (Join-Path $repository 'backend/evidencias/login-d32-preparo.json') -Raw -Encoding UTF8|ConvertFrom-Json
$java=[IO.Path]::GetFullPath($tool.java.path)
$javaHome=Split-Path (Split-Path $java -Parent) -Parent
$release=Join-Path $javaHome 'release'
if([IO.Path]::GetFileName($java) -ine 'java.exe' -or !(Test-Path -LiteralPath $java -PathType Leaf) -or
    (Get-FileHash -LiteralPath $java).Hash -ine $tool.java.sha256 -or
    !([IO.File]::ReadAllText($release) -match '(?m)^JAVA_VERSION="21[.\-]')){throw 'DEV_CURRENT_JDK21_REQUIRED'}
$javaHash=(Get-FileHash -LiteralPath $java).Hash.ToLowerInvariant()
$releaseHash=(Get-FileHash -LiteralPath $release).Hash.ToLowerInvariant()
$before=Get-WmsDevInputFingerprint $repository
$target=Join-Path $run 'target';$evidence=Join-Path $run 'build-pure-evidence'
if(Test-Path -LiteralPath $target){throw 'DEV_CURRENT_BUILD_TARGET_EXISTS'}
$null=New-Item -ItemType Directory -Path $evidence
$wrapper=Join-Path $repository 'backend/mvnw.cmd'
foreach($path in @($wrapper,$target,$evidence)){if($path -match '["\r\n%!]'){throw 'DEV_CURRENT_BUILD_PATH_INVALID'}}
$info=[Diagnostics.ProcessStartInfo]::new();$info.FileName=Join-Path $env:SystemRoot 'System32/cmd.exe'
$info.Arguments='/d /c ""'+$wrapper+'" -B "-Dwms.build.directory='+$target+'" "-Dwms.test.evidencias.dir='+$evidence+'" '+
    '"-Dmaven.test.skip=true" "-Ppure-no-db,!h2-historical,!sqlserver-it,!migrations,!migrations-wrapper-confirmado,!bootstrap-local,!bootstrap-local-wrapper-confirmado" package"'
$info.WorkingDirectory=Join-Path $repository 'backend';$info.UseShellExecute=$false;$info.CreateNoWindow=$true
$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true;$info.EnvironmentVariables.Clear()
foreach($name in @('SystemRoot','WINDIR','ComSpec','PATH','PATHEXT','TEMP','TMP','USERPROFILE','HOMEDRIVE','HOMEPATH','APPDATA','LOCALAPPDATA','PROGRAMDATA')){
    $value=[Environment]::GetEnvironmentVariable($name);if($null -ne $value){$info.EnvironmentVariables[$name]=$value}
}
$info.EnvironmentVariables['JAVA_HOME']=$javaHome
$info.EnvironmentVariables['PATH']=(Join-Path $javaHome 'bin')+';'+$info.EnvironmentVariables['PATH']
$process=[Diagnostics.Process]::new();$process.StartInfo=$info;$startedUtc=[DateTime]::UtcNow.ToString('o')
Write-Host '[WMS DEV] Compilando a fonte atual, sem testes/banco. Instancia existente preservada.'
try{
    if(!$process.Start()){throw 'DEV_CURRENT_BUILD_NOT_STARTED'}
    $stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
    $process.WaitForExit();$exitCode=$process.ExitCode
    $build=[ordered]@{scope='PURE_NO_DATABASE';exit=$exitCode;startedUtc=$startedUtc;finishedUtc=[DateTime]::UtcNow.ToString('o');testsExecuted=0;H2=0;SQL=0;rawLogsSaved=$false;stdoutLines=@($stdout.Result -split "`n").Count;stderrLines=@($stderr.Result -split "`n").Count}
    $digest=[Security.Cryptography.SHA256]::Create()
    try{$build.logSha256=([BitConverter]::ToString($digest.ComputeHash([Text.Encoding]::UTF8.GetBytes($stdout.Result+$stderr.Result)))).Replace('-','').ToLowerInvariant()}finally{$digest.Dispose()}
    foreach($kind in @('stdout','stderr')){
        $capture=if($kind -ceq 'stdout'){$stdout.Result}else{$stderr.Result}
        $digest=[Security.Cryptography.SHA256]::Create()
        try{$build[$kind+'Sha256']=([BitConverter]::ToString($digest.ComputeHash([Text.Encoding]::UTF8.GetBytes($capture)))).Replace('-','').ToLowerInvariant()}finally{$digest.Dispose()}
    }
    $build.profiles='pure-no-db,!h2-historical,!sqlserver-it,!migrations,!migrations-wrapper-confirmado,!bootstrap-local,!bootstrap-local-wrapper-confirmado'
    $build.goal='package';$build.mavenTestSkip='true';$build.publicCommand=$info.Arguments
    $build.observed=@{testSourcesNotCompiled=$stdout.Result.Contains('Not compiling test sources');testsSkipped=$stdout.Result.Contains('Tests are skipped');buildSuccess=$stdout.Result.Contains('BUILD SUCCESS')}
    Write-WmsDevCreateOnlyJson (Join-Path $run 'build-result.json') $build
    if($exitCode -ne 0){throw 'DEV_CURRENT_BUILD_FAILED'}
    if(!$build.observed.testSourcesNotCompiled -or !$build.observed.testsSkipped -or !$build.observed.buildSuccess){throw 'DEV_CURRENT_BUILD_MARKERS_INCOMPLETE'}
}finally{$info.EnvironmentVariables.Clear();$process.Dispose()}
$after=Get-WmsDevInputFingerprint $repository
if($before.sha256 -cne $after.sha256 -or $before.sourceHash -cne $after.sourceHash -or
    (Get-FileHash -LiteralPath $java).Hash -ine $javaHash -or (Get-FileHash -LiteralPath $release).Hash -ine $releaseHash){throw 'DEV_CURRENT_SOURCE_CHANGED_DURING_BUILD'}
$jar=Join-Path $target 'wms-backend-0.0.1-SNAPSHOT.jar'
$config=Join-Path $run 'application-frontend-dev.properties'
[IO.File]::Copy((Join-Path $repository 'backend/src/main/resources/application-frontend-dev.properties'),$config,$false)
$package=Get-WmsDevPackageMetadata $jar
$value=[ordered]@{id='BE02-DEV-FONTE01';schema='WMS_CURRENT_DEV_ARTIFACT_V1';owner='WMS - Prumo';runId=$uuid.ToString('D');observadoUtc=[DateTime]::UtcNow.ToString('o');reused=$false;sourceHash=$after.sourceHash;sourceHashBefore=$before.sourceHash;sourceHashAfter=$after.sourceHash;fingerprint=$after;build=$build;package=$package;
    recipe=@{preparer=@{path=$PSCommandPath;sha256=(Get-FileHash -LiteralPath $PSCommandPath).Hash};goal='package';profiles='pure-no-db,!h2-historical,!sqlserver-it,!migrations,!migrations-wrapper-confirmado,!bootstrap-local,!bootstrap-local-wrapper-confirmado';testCompilationSkipped=$true};
    fingerprintBefore=@{sha256=$before.sha256;sourceHash=$before.sourceHash};fingerprintAfter=@{sha256=$after.sha256;sourceHash=$after.sourceHash};
    verificacao=@{testes=0;ambiente='Build sem banco';sqlServer=$false;integracaoSqlObrigatoria=$true};
    jar=@{path=$jar;sha256=(Get-FileHash -LiteralPath $jar).Hash.ToLowerInvariant()};config=@{path=$config;sha256=(Get-FileHash -LiteralPath $config).Hash.ToLowerInvariant()};
    java=@{path=$java;sha256=$javaHash};javaRelease=@{path=$release;sha256=$releaseHash};integracaoSql=$false;activeReceiptsChanged=$false}
Write-WmsDevCreateOnlyJson $output $value
$hash=(Get-FileHash -LiteralPath $output).Hash.ToLowerInvariant();$null=Assert-WmsDevCurrentArtifact $repository $output $hash
[pscustomobject]@{path=$output;sha256=$hash;artifact=[pscustomobject]$value}
