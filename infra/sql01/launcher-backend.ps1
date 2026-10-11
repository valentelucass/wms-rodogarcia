# SQL01 biblioteca opt-in: carregar define funcoes, nao importa DPAPI/SQL nem inicia processo.
Set-StrictMode -Version Latest
$script:WmsSql01Root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$script:WmsSql01Launcher=$PSCommandPath
. (Join-Path $PSScriptRoot 'transporte-privado.ps1')
if (-not (Get-Variable WmsSql01Handles -Scope Script -ErrorAction SilentlyContinue)) { $script:WmsSql01Handles=@{} }

function Resolve-WmsSql01Path([string]$Path,[string]$AllowedRoot) {
    if ([string]::IsNullOrWhiteSpace($Path) -or $Path -match '["\r\n]') { throw 'SQL01_PATH_INVALID' }
    $value=if([IO.Path]::IsPathRooted($Path)){$Path}else{Join-Path $script:WmsSql01Root $Path}
    $resolved=[IO.Path]::GetFullPath($value)
    $allowed=[IO.Path]::GetFullPath($AllowedRoot).TrimEnd('\','/')
    if (-not $resolved.StartsWith($allowed+'\',[StringComparison]::OrdinalIgnoreCase)) { throw 'SQL01_PATH_OUTSIDE_OWNER' }
    $cursor=$resolved
    while($cursor -and $cursor.Length -ge $allowed.Length) {
        if((Test-Path -LiteralPath $cursor) -and ((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'SQL01_PATH_LINK_REFUSED' }
        $cursor=[IO.Path]::GetDirectoryName($cursor)
    }
    return $resolved
}

function Read-WmsSql01HashedFile($Reference,[string]$AllowedRoot) {
    if($Reference.sha256 -notmatch '^[a-fA-F0-9]{64}$') { throw 'SQL01_HASH_INVALID' }
    $path=Resolve-WmsSql01Path $Reference.path $AllowedRoot
    if(-not (Test-Path -LiteralPath $path -PathType Leaf) -or (Get-FileHash -LiteralPath $path).Hash -ine $Reference.sha256) { throw 'SQL01_FILE_HASH_MISMATCH' }
    return $path
}

function Get-WmsSql01EvidenceArea([string]$RunDirectory) {
    $area=Join-Path $script:WmsSql01Root 'orchestracao/.runtime/qual-conf01/sql01'
    $reg01=Join-Path $area 'reg01'
    if([IO.Path]::GetFullPath($RunDirectory).StartsWith((Join-Path $reg01 'prumo/runs')+'\',[StringComparison]::OrdinalIgnoreCase)){return $reg01}
    return $area
}

function New-WmsSql01Run([guid]$RunId=[guid]::NewGuid()) {
    $area=Join-Path $script:WmsSql01Root 'orchestracao/.runtime/qual-conf01/sql01/prumo'
    $path=Resolve-WmsSql01Path (Join-Path $area ('runs/'+$RunId.ToString('D'))) $area
    if(Test-Path -LiteralPath $path) { throw 'SQL01_RUN_EXISTS' }
    $null=New-Item -ItemType Directory -Path $path
    return $path
}

function Get-WmsSql01SqlProcess([int]$ProcessId) {
    if($ProcessId -le 0) { throw 'SQL01_SQL_PROCESS_METADATA_INVALID' }
    $processes=@(Get-CimInstance -ClassName Win32_Process -Filter ('ProcessId='+$ProcessId) -ErrorAction Stop)
    if($processes.Count -ne 1) { throw 'SQL01_SQL_PROCESS_METADATA_INVALID' }
    $process=$processes[0]
    if($process.ProcessId -ne $ProcessId -or $process.Name -ine 'sqlservr.exe' -or
        $process.CreationDate -isnot [DateTime]) { throw 'SQL01_SQL_PROCESS_METADATA_INVALID' }
    # Same current PID/start metadata channel as the SQL guard, without privileged process handles.
    [pscustomobject]@{Id=$process.ProcessId;Name=$process.Name;StartTime=$process.CreationDate;metadataSource='CIM_CREATION_DATE'}
}
function Get-WmsSql01Listeners([int]$Port) { @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) }
function New-WmsSql01Process { [Diagnostics.Process]::new() }
function Get-WmsSql01BackendProcess([int]$ProcessId) {
    $found=@(Get-CimInstance -ClassName Win32_Process -Filter ('ProcessId='+$ProcessId) -Property ProcessId,Name,CreationDate,ExecutablePath,CommandLine -ErrorAction Stop)
    if($found.Count -ne 1 -or $found[0].ProcessId -ne $ProcessId -or $found[0].Name -ine 'java.exe' -or $found[0].CreationDate -isnot [DateTime]){throw 'SQL01_OWN_PROCESS_METADATA_INVALID'}
    [pscustomobject]@{Id=$found[0].ProcessId;StartTime=$found[0].CreationDate;ExecutablePath=$found[0].ExecutablePath;CommandLine=$found[0].CommandLine;metadataSource='CIM_CREATION_DATE'}
}

function Get-WmsSql01ExceptionTypes([Exception]$Exception) {
    $types=New-Object 'Collections.Generic.List[string]'
    for($current=$Exception;$null -ne $current -and $types.Count -lt 8;$current=$current.InnerException) {
        $type=$current.GetType().FullName
        $types.Add($(if($type -cmatch '^[A-Za-z0-9_.$+]{1,192}$'){$type}else{'UNKNOWN'}))
    }
    return ,$types.ToArray()
}

function Read-WmsSql01BootstrapDiagnostic([string]$Path,[string]$RunId,[int]$ProcessId) {
    $area=Get-WmsSql01EvidenceArea (Split-Path $Path -Parent)
    $run=Join-Path $area ('prumo/runs/'+$RunId)
    $resolved=Resolve-WmsSql01Path $Path $run
    if($resolved -ine (Join-Path $run 'bootstrap-diagnostic.json')) { throw 'SQL01_DIAGNOSTIC_PATH_INVALID' }
    $file=Get-Item -LiteralPath $resolved -ErrorAction Stop
    if($file.PSIsContainer -or $file.Length -gt 8192) { throw 'SQL01_DIAGNOSTIC_INVALID' }
    $value=Get-Content -LiteralPath $resolved -Raw -Encoding UTF8|ConvertFrom-Json
    $phases=@('ARGUMENTS','PUBLIC_PATHS','ARG_VALIDATION','JAR_HASH','PRIVATE_FRAMES','SECRET_INSTALL','APPLICATION_ARGUMENTS','JAR_CLASS_LOADING','JAR_MAIN_LOOKUP','APPLICATION_LAUNCH')
    if($value.id -cne 'QUAL-CONF01-SQL01' -or $value.runId -cne $RunId -or $value.pid -ne $ProcessId -or
        $phases -cnotcontains $value.phase -or $value.type -cnotmatch '^[A-Za-z0-9_.$]{1,192}$' -or
        @($value.chain).Count -lt 1 -or @($value.chain).Count -gt 8 -or $value.chain[0] -cne $value.type -or
        @($value.chain|Where-Object {$_ -isnot [string] -or $_ -cnotmatch '^[A-Za-z0-9_.$]{1,192}$'}).Count -or
        $value.exitCode -ne 20 -or $value.messagesIncluded -ne $false) { throw 'SQL01_DIAGNOSTIC_INVALID' }
    # Only the allowlisted fields enter the receipt, never arbitrary child JSON.
    [pscustomobject]@{path=$resolved;sha256=(Get-FileHash -LiteralPath $resolved).Hash.ToLowerInvariant();phase=$value.phase;type=$value.type;chain=@($value.chain);declaredExitCode=$value.exitCode}
}

function Assert-WmsSql01ReviewedClient($Reference,[string]$Area,[string]$RunId) {
    $cedro=Join-Path $Area 'cedro'
    $clientPath=Read-WmsSql01HashedFile $Reference $cedro
    $directory=Split-Path $clientPath -Parent
    $indicator=$RunId -ceq '4f61b450-045b-49ad-96cf-c514667bc30e'
    $clientDirectory=if($indicator){'indicator-residual-v1'}else{'client-first'}
    if([IO.Path]::GetFileName($Area) -ceq 'reg01' -and $clientPath -ine (Join-Path $cedro ($clientDirectory+'/client-manifest.json'))){throw 'SQL01_REG01_CLIENT_REQUIRED'}
    if([IO.Path]::GetFileName($clientPath) -cne 'client-manifest.json') { throw 'SQL01_CLIENT_MANIFEST_REQUIRED' }
    $manifest=Get-Content -LiteralPath $clientPath -Raw -Encoding UTF8|ConvertFrom-Json
    if($manifest.id -cne 'QUAL-CONF01-SQL01' -or $manifest.owner -cne 'WMS - Cedro' -or
        $manifest.runId -cne $RunId -or $manifest.entryPoint -ine (Join-Path $directory 'run-protected-http.ps1')) { throw 'SQL01_CLIENT_MANIFEST_BINDING_REQUIRED' }
    $apiOrigin=if($indicator){'http://127.0.0.1:25586'}else{'http://127.0.0.1:25584'}
    if([IO.Path]::GetFileName($Area) -ceq 'reg01' -and ($manifest.increment -cne 'QUAL-CONF01-SQL01-REG01' -or $manifest.publicOrigin -cne 'http://127.0.0.1:25585' -or $manifest.apiOrigin -cne $apiOrigin)){throw 'SQL01_REG01_CLIENT_ORIGIN_REQUIRED'}
    if(@($manifest.refs).Count -ne 3) { throw 'SQL01_CLIENT_MANIFEST_REFS_REQUIRED' }
    $sources=@(foreach($reference in $manifest.refs){Read-WmsSql01HashedFile $reference $directory})
    $protocol=if($indicator){'http-indicator-residual-protocol.ps1'}else{'http-read-protocol.ps1'}
    foreach($filename in @($protocol,'http-preconditions.ps1','run-protected-http.ps1')) {
        $expected=Join-Path $directory $filename
        if(@($sources|Where-Object {$_ -ieq $expected}).Count -ne 1) { throw 'SQL01_CLIENT_MANIFEST_SOURCE_REQUIRED' }
    }
}

function Get-WmsSql01IndicatorPaths([string]$Root) {
    $id='4f61b450-045b-49ad-96cf-c514667bc30e'
    $sql=Join-Path $Root 'orchestracao/.runtime/qual-conf01/sql01';$reg=Join-Path $sql 'reg01'
    $client=Join-Path $reg 'cedro/indicator-residual-v1'
    $candidate=Join-Path $sql ('cedro/reg01/indicator-residual-v1/'+$id+'/candidate.json')
    [pscustomobject]@{runId=$id;area=$reg;candidatePath=$candidate;candidateArtifactCopyPath=(Join-Path (Split-Path $candidate -Parent) 'wms-backend-0.0.1-SNAPSHOT.jar')
        run=(Join-Path $reg ('prumo/runs/'+$id));guardPath=(Join-Path $reg ('prumo/runs/'+$id+'/guarda-real.json'))
        runtimePath=(Join-Path $reg ('cedro/'+$id+'/runtime-prumo.json'));observationsPath=(Join-Path $sql ('cedro/reg01/'+$id+'/observations'))
        clientPath=(Join-Path $client 'client-manifest.json');planPath=(Join-Path $client 'run-plan.json');httpOutputPath=(Join-Path $reg ('cedro/'+$id+'/http-indicator-residual-v1'))
        credentialHelperPath=(Join-Path $reg 'lume/credencial-http-indicadores-batch.ps1');finalInterfacePath=(Join-Path $reg 'interface-indicadores-batch-coordenada.json')
        reviewPath=(Join-Path $reg 'vigia/indicadores-batch-prelaunch-review.json');executorPath=(Join-Path $reg 'prumo/abrir-runtime-indicadores-batch-own.ps1')
        consumerReadyPath=(Join-Path $client 'indicator-command-review-bound-ready.json')}
}

function Assert-WmsSql01IndicatorInterface($Binding,[string]$Root,[string]$CandidatePath,[string]$SourceHash,[string]$JarSha256) {
    $paths=Get-WmsSql01IndicatorPaths $Root
    if($Binding.id -cne 'QUAL-CONF01-SQL01-REG01-IND501' -or $Binding.owner -cne 'WMS - Farol' -or $Binding.runId -cne $paths.runId -or
        $Binding.sourceHash -cne $SourceHash -or $Binding.jarSha256 -ine $JarSha256 -or
        $Binding.apiOrigin -cne 'http://127.0.0.1:25586' -or $Binding.localOrigin -cne 'http://127.0.0.1:25585' -or $CandidatePath -ine $paths.candidatePath){throw 'SQL01_INDICATOR_INTERFACE_BINDING_REQUIRED'}
    foreach($field in @('candidatePath','candidateArtifactCopyPath','runtimePath','observationsPath','guardPath','clientPath','planPath','credentialHelperPath')) {
        if((Resolve-WmsSql01Path $Binding.$field $Root) -ine $paths.$field){throw 'SQL01_INDICATOR_INTERFACE_PATH_REQUIRED'}
    }
}

function Assert-WmsSql01IndicatorCandidate($Candidate,[string]$Root) {
    $paths=Get-WmsSql01IndicatorPaths $Root;$clientRoot=Split-Path $paths.clientPath -Parent
    if($Candidate.id -cne 'QUAL-CONF01-SQL01' -or $Candidate.increment -cne 'QUAL-CONF01-SQL01-REG01-IND501' -or
        $Candidate.owner -cne 'WMS - Cedro' -or $Candidate.runId -cne $paths.runId -or $Candidate.buildTests -ne 0){throw 'SQL01_INDICATOR_CANDIDATE_BINDING_REQUIRED'}
    $buildFile=Read-WmsSql01HashedFile $Candidate.build $clientRoot
    if($buildFile -ine (Join-Path $clientRoot 'candidate-build-end.json')){throw 'SQL01_INDICATOR_BUILD_PATH_REQUIRED'}
    $build=Get-Content -LiteralPath $buildFile -Raw -Encoding UTF8|ConvertFrom-Json
    if($build.id -cne 'QUAL-CONF01-SQL01-REG01-IND501' -or $build.owner -cne 'WMS - Cedro' -or $build.nativeExit -ne 0 -or
        $build.tests -ne 0 -or $build.SQL -ne 0 -or $build.H2 -ne 0 -or $build.HTTP -ne 0 -or
        $build.testCompilationSkipped -ne $true -or $build.sourceUnchanged -ne $true -or
        $build.sourceHash -cne $Candidate.sourceHash -or $build.sourceHashAfter -cne $Candidate.sourceHash){throw 'SQL01_INDICATOR_BUILD_NOT_APPROVED'}
    $builtJar=Read-WmsSql01HashedFile $build.jar $clientRoot
    if($builtJar -ine (Join-Path $clientRoot 'candidate-target/wms-backend-0.0.1-SNAPSHOT.jar') -or $build.jar.sha256 -ine $Candidate.jar.sha256){throw 'SQL01_INDICATOR_ARTIFACT_LINEAGE_REQUIRED'}
    $freezeFile=Read-WmsSql01HashedFile $Candidate.source.manifest $clientRoot
    if($freezeFile -ine (Join-Path $clientRoot 'candidate-source-freeze.json') -or
        (Resolve-WmsSql01Path $Candidate.source.directory $clientRoot) -ine (Join-Path $clientRoot 'candidate-source')){throw 'SQL01_INDICATOR_SOURCE_COPY_REQUIRED'}
    $freeze=Get-Content -LiteralPath $freezeFile -Raw -Encoding UTF8|ConvertFrom-Json
    if($freeze.candidateSourceHash -cne $Candidate.sourceHash){throw 'SQL01_INDICATOR_SOURCE_COPY_HASH_REQUIRED'}
    $config=Read-WmsSql01HashedFile $Candidate.config $clientRoot
    if($config -ine (Join-Path $clientRoot 'candidate-source/backend/src/main/resources/application-frontend-dev.properties') -or
        $Candidate.config.sha256 -ine (Get-FileHash -LiteralPath (Join-Path $Root 'backend/src/main/resources/application-frontend-dev.properties')).Hash){throw 'SQL01_INDICATOR_CONFIG_COPY_REQUIRED'}
    $pom=Read-WmsSql01HashedFile $build.pom $clientRoot
    if($pom -ine (Join-Path $clientRoot 'candidate-source/backend/pom.xml') -or
        $build.pom.sha256 -ine (Get-FileHash -LiteralPath (Join-Path $Root 'backend/pom.xml')).Hash){throw 'SQL01_INDICATOR_POM_COPY_REQUIRED'}
}

function Assert-WmsSql01IndicatorPrelaunch {
    param([string]$ReviewPath,[string]$ReviewSha256,[string]$CedroReadySha256,[switch]$ComandosProntos)
    # Public file checks only. No process inventory, protected import or SQL here.
    if(-not $ComandosProntos){throw 'SQL01_INDICATOR_COMMANDS_REQUIRED'}
    $root=$script:WmsSql01Root;$paths=Get-WmsSql01IndicatorPaths $root
    $file=Read-WmsSql01HashedFile @{path=$ReviewPath;sha256=$ReviewSha256} (Join-Path $paths.area 'vigia')
    if($file -ine $paths.reviewPath){throw 'SQL01_INDICATOR_UNIFIED_REVIEW_REQUIRED'}
    $review=Get-Content -LiteralPath $file -Raw -Encoding UTF8|ConvertFrom-Json
    if($review.id -cne 'QUAL-CONF01-SQL01' -or $review.owner -cne 'WMS - Vigia' -or $review.runId -cne $paths.runId -or
        $review.SQLLaunchApproved -isnot [bool] -or -not $review.SQLLaunchApproved -or
        $review.OperationReviewApproved -isnot [bool] -or -not $review.OperationReviewApproved -or @($review.failed).Count -or
        $review.localOrigin -cne 'http://127.0.0.1:25585'){throw 'SQL01_INDICATOR_UNIFIED_REVIEW_REQUIRED'}
    $expected=@{launcher=$script:WmsSql01Launcher;bootstrap=(Join-Path $root 'infra/sql01/Sql01Bootstrap.java');transport=(Join-Path $root 'infra/sql01/transporte-privado.ps1')
        observer=(Join-Path $root 'backend/src/main/java/br/com/rodogarcia/wms/config/Sql01ObservationConfig.java');executor=$paths.executorPath
        candidate=$paths.candidatePath;interface=$paths.finalInterfacePath;client=$paths.clientPath;credential=$paths.credentialHelperPath;plan=$paths.planPath}
    foreach($field in $expected.Keys){if((Read-WmsSql01HashedFile $review.$field $root) -ine $expected[$field]){throw 'SQL01_INDICATOR_REVIEW_SOURCE_REQUIRED'}}
    $candidate=Get-Content -LiteralPath $paths.candidatePath -Raw -Encoding UTF8|ConvertFrom-Json
    if($candidate.id -cne 'QUAL-CONF01-SQL01' -or $candidate.sourceHash -cnotmatch '^[a-f0-9]{64}$' -or
        $review.sourceHash -cne $candidate.sourceHash -or $review.jarSha256 -ine $candidate.jar.sha256){throw 'SQL01_INDICATOR_CANDIDATE_BINDING_REQUIRED'}
    . (Join-Path $root 'infra/dev/artefato-backend.ps1')
    $null=Assert-WmsDevApplicationArtifact $root $paths.candidatePath
    if((Read-WmsSql01HashedFile $candidate.jar $root) -ine $paths.candidateArtifactCopyPath){throw 'SQL01_INDICATOR_ARTIFACT_COPY_REQUIRED'}
    Assert-WmsSql01IndicatorCandidate $candidate $root
    $binding=Get-Content -LiteralPath $paths.finalInterfacePath -Raw -Encoding UTF8|ConvertFrom-Json
    Assert-WmsSql01IndicatorInterface $binding $root $paths.candidatePath $candidate.sourceHash $candidate.jar.sha256
    Assert-WmsSql01ReviewedClient $review.client $paths.area $paths.runId
    $readyFile=Read-WmsSql01HashedFile @{path=$paths.consumerReadyPath;sha256=$CedroReadySha256} (Join-Path $paths.area 'cedro/indicator-residual-v1')
    $ready=Get-Content -LiteralPath $readyFile -Raw -Encoding UTF8|ConvertFrom-Json
    $manifest=Get-Content -LiteralPath $paths.clientPath -Raw -Encoding UTF8|ConvertFrom-Json
    $entry=Read-WmsSql01HashedFile $ready.entryPoint (Split-Path $paths.clientPath -Parent)
    if($ready.id -cne 'QUAL-CONF01-SQL01-REG01' -or $ready.owner -cne 'WMS - Cedro' -or $ready.runId -cne $paths.runId -or
        $ready.commandReady -isnot [bool] -or -not $ready.commandReady -or $ready.executed -isnot [bool] -or $ready.executed -or
        (Read-WmsSql01HashedFile $ready.apiReview $paths.area) -ine $file -or $ready.apiReview.sha256 -ine $ReviewSha256 -or
        (Read-WmsSql01HashedFile $ready.client $paths.area) -ine $paths.clientPath -or $ready.client.sha256 -ine $review.client.sha256 -or
        (Resolve-WmsSql01Path $ready.guardPath $paths.run) -ine $paths.guardPath -or
        (Resolve-WmsSql01Path $ready.output (Join-Path $paths.area 'cedro')) -ine $paths.httpOutputPath -or $entry -ine $manifest.entryPoint){throw 'SQL01_INDICATOR_READY_BINDING_REQUIRED'}
    [pscustomobject]@{paths=$paths;review=$review;reviewFile=$file;candidate=$candidate;readyFile=$readyFile}
}

function Assert-WmsSql01LaunchPlan {
    param([string]$Candidato,[string]$CandidateSha256,[string]$GuardReceipt,[string]$GuardSha256,
        [string]$Review,[string]$ReviewSha256,[string]$RunDirectory,[string]$ObservationDirectory,[int]$Port,[string]$LocalOrigin)
    if($Port -notin @(25584,25586)) { throw 'SQL01_PRIVATE_PORT_REQUIRED' }
    if([string]::IsNullOrEmpty($LocalOrigin)){$LocalOrigin='http://127.0.0.1:'+$Port}
    if($LocalOrigin -cnotin @('http://127.0.0.1:25584','http://127.0.0.1:25585')) { throw 'SQL01_LOCAL_ORIGIN_REQUIRED' }
    $root=$script:WmsSql01Root;$legacyArea=Join-Path $root 'orchestracao/.runtime/qual-conf01/sql01'
    $area=Get-WmsSql01EvidenceArea $RunDirectory;$reg01=[IO.Path]::GetFileName($area) -ceq 'reg01'
    $run=Resolve-WmsSql01Path $RunDirectory (Join-Path $area 'prumo/runs')
    $uuid=[guid]::Empty
    if(-not (Test-Path -LiteralPath $run -PathType Container) -or -not [guid]::TryParse([IO.Path]::GetFileName($run),[ref]$uuid)) { throw 'SQL01_RUN_INVALID' }
    $indicator=$Port -eq 25586
    if($indicator -and (-not $reg01 -or [IO.Path]::GetFileName($run) -cne '4f61b450-045b-49ad-96cf-c514667bc30e')){throw 'SQL01_INDICATOR_EXACT_RUN_REQUIRED'}
    if(-not $indicator -and [IO.Path]::GetFileName($run) -ceq '4f61b450-045b-49ad-96cf-c514667bc30e'){throw 'SQL01_INDICATOR_EXACT_PORT_REQUIRED'}
    $observationRoot=if($reg01){Join-Path $legacyArea 'cedro/reg01'}else{Join-Path $area 'cedro'}
    $observation=Resolve-WmsSql01Path $ObservationDirectory $observationRoot
    if(-not (Test-Path -LiteralPath $observation -PathType Container) -or [IO.Path]::GetFileName($observation) -cne 'observations' -or
        [IO.Path]::GetFileName((Split-Path $observation -Parent)) -ine [IO.Path]::GetFileName($run)) { throw 'SQL01_OBSERVATION_DIRECTORY_INVALID' }
    if($reg01 -and ($LocalOrigin -cne 'http://127.0.0.1:25585' -or $observation -ine (Join-Path $observationRoot ([IO.Path]::GetFileName($run)+'/observations')))){throw 'SQL01_REG01_OUTPUT_OR_ORIGIN_REQUIRED'}
    $candidatePath=Read-WmsSql01HashedFile @{path=$Candidato;sha256=$CandidateSha256} (Join-Path $legacyArea 'cedro')
    if($indicator -and $candidatePath -ine (Join-Path $legacyArea 'cedro/reg01/indicator-residual-v1/4f61b450-045b-49ad-96cf-c514667bc30e/candidate.json')){throw 'SQL01_INDICATOR_CANDIDATE_REQUIRED'}
    if([IO.Path]::GetFileName($candidatePath) -cne 'candidate.json') { throw 'SQL01_OFFICIAL_CANDIDATE_REQUIRED' }
    $candidate=Get-Content -LiteralPath $candidatePath -Raw -Encoding UTF8|ConvertFrom-Json
    if($candidate.id -cne 'QUAL-CONF01-SQL01' -or $candidate.sourceHash -cnotmatch '^[a-f0-9]{64}$') { throw 'SQL01_CANDIDATE_SCHEMA_INVALID' }
    if($indicator){Assert-WmsSql01IndicatorCandidate $candidate $root}
    else{
        $buildPath=Read-WmsSql01HashedFile $candidate.build (Join-Path $legacyArea 'cedro')
        $buildRecord=Get-Content -LiteralPath $buildPath -Raw -Encoding UTF8|ConvertFrom-Json
        if($buildRecord.id -cne 'QUAL-CONF01-SQL01' -or $buildRecord.exit -ne 0 -or $buildRecord.scope -cne 'PURE_NO_DATABASE') { throw 'SQL01_BUILD_NOT_APPROVED' }
    }
    . (Join-Path $root 'infra/dev/artefato-backend.ps1')
    if($candidate.sourceHash -cne (Get-WmsBackendSourceHash $root)) { throw 'SQL01_CANDIDATE_SOURCE_STALE' }
    $null=Assert-WmsDevApplicationArtifact $root $candidatePath
    $jar=Read-WmsSql01HashedFile $candidate.jar (Join-Path $legacyArea 'cedro')
    if($indicator){
        if($jar -ine (Join-Path (Split-Path $candidatePath -Parent) 'wms-backend-0.0.1-SNAPSHOT.jar')){throw 'SQL01_INDICATOR_ARTIFACT_COPY_REQUIRED'}
    }elseif($jar -ine (Join-Path $buildRecord.target 'wms-backend-0.0.1-SNAPSHOT.jar')) { throw 'SQL01_MANUAL_JAR_REFUSED' }
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::OpenRead($jar)
    try {
        if(@($zip.Entries|Where-Object {$_.FullName -match '^BOOT-INF/lib/h2-'}).Count -or
            $null -eq $zip.GetEntry('BOOT-INF/classes/br/com/rodogarcia/wms/config/Sql01ObservationConfig.class') -or
            $null -eq $zip.GetEntry('org/springframework/boot/loader/launch/JarLauncher.class')) { throw 'SQL01_JAR_PROVIDER_OR_OBSERVER_INVALID' }
        $reader=[IO.StreamReader]::new($zip.GetEntry('META-INF/MANIFEST.MF').Open())
        try{$manifest=$reader.ReadToEnd()}finally{$reader.Dispose()}
        if($manifest -notmatch '(?m)^Main-Class: org.springframework.boot.loader.launch.JarLauncher\r?$' -or
            $manifest -notmatch '(?m)^Start-Class: br.com.rodogarcia.wms.WmsApplication\r?$') { throw 'SQL01_JAR_ENTRYPOINT_INVALID' }
    } finally {$zip.Dispose()}
    $reviewPath=Read-WmsSql01HashedFile @{path=$Review;sha256=$ReviewSha256} (Join-Path $area 'vigia')
    $reviewed=Get-Content -LiteralPath $reviewPath -Raw -Encoding UTF8|ConvertFrom-Json
    if($indicator -and ($reviewPath -ine (Join-Path $area 'vigia/indicadores-batch-prelaunch-review.json') -or $reviewed.OperationReviewApproved -isnot [bool] -or -not $reviewed.OperationReviewApproved -or @($reviewed.failed).Count)){throw 'SQL01_INDICATOR_UNIFIED_REVIEW_REQUIRED'}
    if($reviewed.id -cne 'QUAL-CONF01-SQL01' -or $reviewed.owner -cne 'WMS - Vigia' -or
        $reviewed.SQLLaunchApproved -isnot [bool] -or -not $reviewed.SQLLaunchApproved -or
        $reviewed.candidate.sha256 -ine $CandidateSha256 -or $reviewed.sourceHash -cne $candidate.sourceHash -or $reviewed.jarSha256 -ine $candidate.jar.sha256) { throw 'SQL01_PRELAUNCH_REVIEW_REQUIRED' }
    $reviewCandidate=Read-WmsSql01HashedFile $reviewed.candidate (Join-Path $legacyArea 'cedro')
    if($reviewCandidate -ine $candidatePath) { throw 'SQL01_PRELAUNCH_CANDIDATE_MISMATCH' }
    $expected=@{
        launcher=$script:WmsSql01Launcher;bootstrap=(Join-Path $PSScriptRoot 'Sql01Bootstrap.java')
        transport=(Join-Path $PSScriptRoot 'transporte-privado.ps1')
        observer=(Join-Path $root 'backend/src/main/java/br/com/rodogarcia/wms/config/Sql01ObservationConfig.java')
    }
    foreach($name in $expected.Keys) {
        $source=Read-WmsSql01HashedFile $reviewed.$name $root
        if($source -ine $expected[$name]) { throw 'SQL01_PRELAUNCH_SOURCE_REVIEW_MISMATCH' }
    }
    if($reviewed.runId -cne [IO.Path]::GetFileName($run)) { throw 'SQL01_PRELAUNCH_RUN_MISMATCH' }
    if($LocalOrigin -cne ('http://127.0.0.1:'+$Port) -and $reviewed.localOrigin -cne $LocalOrigin) { throw 'SQL01_PRELAUNCH_ORIGIN_MISMATCH' }
    $runtimePath=if($reg01){Join-Path (Join-Path $area ('cedro/'+[IO.Path]::GetFileName($run))) 'runtime-prumo.json'}else{Join-Path (Split-Path $observation -Parent) 'runtime-prumo.json'}
    $runtimePath=Resolve-WmsSql01Path $runtimePath (Join-Path $area 'cedro')
    if(-not(Test-Path -LiteralPath (Split-Path $runtimePath -Parent) -PathType Container)){throw 'SQL01_RUNTIME_DIRECTORY_REQUIRED'}
    if($reg01) {
        $interfacePath=Read-WmsSql01HashedFile $reviewed.interface $area
        $interfaceName=if($indicator){'interface-indicadores-batch-coordenada.json'}else{'interface-coordenada.json'}
        if($interfacePath -ine (Join-Path $area $interfaceName)){throw 'SQL01_REG01_INTERFACE_REQUIRED'}
        $binding=Get-Content -LiteralPath $interfacePath -Raw -Encoding UTF8|ConvertFrom-Json
        if($indicator){Assert-WmsSql01IndicatorInterface $binding $root $candidatePath $candidate.sourceHash $candidate.jar.sha256}
        elseif($binding.id -cne 'QUAL-CONF01-SQL01-REG01' -or $binding.owner -cne 'WMS - Farol' -or
            $binding.runId -cne [IO.Path]::GetFileName($run) -or $binding.sourceHash -cne $candidate.sourceHash -or
            $binding.jarSha256 -ine $candidate.jar.sha256 -or $binding.frontendAndAuthOrigin -cne $LocalOrigin -or
            $binding.apiOrigin -cne 'http://127.0.0.1:25584' -or
            (Resolve-WmsSql01Path $binding.candidate (Join-Path $legacyArea 'cedro')) -ine $candidatePath -or
            (Resolve-WmsSql01Path $binding.observationDirectory $observationRoot) -ine $observation -or
            (Resolve-WmsSql01Path $binding.runtimeReceipt (Join-Path $area 'cedro')) -ine $runtimePath -or
            (Resolve-WmsSql01Path $binding.guardReceipt $run) -ine [IO.Path]::GetFullPath($GuardReceipt)){throw 'SQL01_REG01_INTERFACE_MISMATCH'}
    }
    Assert-WmsSql01ReviewedClient $reviewed.client $area ([IO.Path]::GetFileName($run))
    $credentialPath=Read-WmsSql01HashedFile $reviewed.credential (Join-Path $area 'lume')
    $credentialName=if($indicator){'credencial-http-indicadores-batch.ps1'}else{'credencial-http-reg01.ps1'}
    if($reg01){if($credentialPath -ine (Join-Path $area ('lume/'+$credentialName))){throw 'SQL01_REG01_HTTP_CREDENTIAL_HELPER_REQUIRED'}}
    elseif([IO.Path]::GetFileName($credentialPath) -cnotin @('credencial-http-protegida.ps1','credencial-http-boot02.ps1')) { throw 'SQL01_HTTP_CREDENTIAL_HELPER_REQUIRED' }
    $guardPath=Read-WmsSql01HashedFile @{path=$GuardReceipt;sha256=$GuardSha256} $run
    if($indicator -and $guardPath -ine (Join-Path $run 'guarda-real.json')){throw 'SQL01_INDICATOR_GUARD_REQUIRED'}
    if((Split-Path $guardPath -Parent) -ine $run) { throw 'SQL01_GUARD_WRONG_RUN' }
    $guard=Get-Content -LiteralPath $guardPath -Raw -Encoding UTF8|ConvertFrom-Json
    $begin=[DateTimeOffset]::Parse($guard.inicioUtc);$finish=[DateTimeOffset]::Parse($guard.observadoEm)
    if($begin -lt [DateTimeOffset]::UtcNow.AddMinutes(-5) -or $finish -lt $begin -or $finish -gt [DateTimeOffset]::UtcNow.AddSeconds(15)) { throw 'SQL01_GUARD_STALE' }
    if($guard.guardaRealAprovada -ne $true -or $guard.estado -cne 'GUARDA_ATUAL_APROVADA' -or
        $guard.aberturaTentativas -ne 1 -or $guard.aberturaConcluida -ne $true -or
        $guard.alvo.banco -cne 'WMS_DEV' -or $guard.alvo.login -cne 'WMSDEV' -or $guard.alvo.usuario -cne 'WMSDEV' -or
        $guard.tls.encrypt -cne 'Mandatory' -or $guard.tls.trustServerCertificate -ne $false -or
        $guard.tls.handshakeConcluido -ne $true -or $guard.tls.confirmacaoAtual -ne $true -or
        @($guard.checks).Count -ne 26 -or @($guard.checks|Where-Object {$_.passou -ne $true}).Count -ne 0) { throw 'SQL01_GUARD_BLOCKED' }
    if($guard.auxiliar.sha256 -ine (Get-FileHash -LiteralPath (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')).Hash) { throw 'SQL01_GUARD_HELPER_CHANGED' }
    $sql=Get-WmsSql01SqlProcess ([int]$guard.tls.proveniencia.reciboPID)
    if($sql.StartTime.ToUniversalTime().Ticks -ne [DateTimeOffset]::Parse($guard.tls.proveniencia.reciboInicioUtc).UtcDateTime.Ticks) { throw 'SQL01_SQL_PROCESS_CHANGED' }
    $trust=Read-WmsSql01HashedFile @{path=$guard.perfilProtegido.truststore;sha256=$guard.tls.truststoreSha256} ([Environment]::GetFolderPath('LocalApplicationData'))
    if($guard.perfilProtegido.host -cne '127.0.0.1' -or $guard.perfilProtegido.port -ne 1433 -or $guard.perfilProtegido.database -cne 'WMS_DEV' -or
        $guard.perfilProtegido.login -cne 'WMSDEV' -or $guard.perfilProtegido.encrypt -ne $true -or $guard.perfilProtegido.trustServerCertificate -ne $false) { throw 'SQL01_PROTECTED_PROFILE_INVALID' }
    if(@(Get-WmsSql01Listeners $Port).Count) { throw 'SQL01_PORT_OCCUPIED' }
    $java=Read-WmsSql01HashedFile $candidate.java ([Environment]::GetFolderPath('LocalApplicationData'))
    if([IO.Path]::GetFileName($java) -ine 'java.exe' -or [IO.File]::ReadAllText((Join-Path (Split-Path (Split-Path $java -Parent) -Parent) 'release')) -notmatch '(?m)^JAVA_VERSION="21[.\-"]') { throw 'SQL01_JDK21_REQUIRED' }
    $config=Read-WmsSql01HashedFile $candidate.config $root
    $configExpected=if($indicator){Join-Path $area 'cedro/indicator-residual-v1/candidate-source/backend/src/main/resources/application-frontend-dev.properties'}else{Join-Path $root 'backend/src/main/resources/application-frontend-dev.properties'}
    if($config -ine $configExpected) { throw 'SQL01_CONFIG_REQUIRED' }
    [pscustomobject]@{run=$run;runId=[IO.Path]::GetFileName($run);observation=$observation;candidatePath=$candidatePath;candidateSha256=$CandidateSha256;sourceHash=$candidate.sourceHash
        jar=$jar;jarSha256=$candidate.jar.sha256.ToLowerInvariant();java=$java;config=$config;guard=$guard;guardPath=$guardPath;guardSha256=$GuardSha256;reviewPath=$reviewPath;reviewSha256=$ReviewSha256;truststore=$trust;port=$Port;localOrigin=$LocalOrigin;runtimePath=$runtimePath}
}

function Start-WmsSql01Backend {
    [CmdletBinding()]
    param([Parameter(Mandatory)][string]$Candidato,[Parameter(Mandatory)][string]$CandidateSha256,
        [Parameter(Mandatory)][string]$GuardReceipt,[Parameter(Mandatory)][string]$GuardSha256,
        [Parameter(Mandatory)][string]$Review,[Parameter(Mandatory)][string]$ReviewSha256,
        [Parameter(Mandatory)][string]$RunDirectory,[Parameter(Mandatory)][string]$ObservationDirectory,[int]$Port=25584,[switch]$Diagnostico,[string]$LocalOrigin)
    $ErrorActionPreference='Stop';$credential=$null;$identity=$null;$process=$null;$started=$false;$info=$null
    # PSBoundParameters nao inclui valores default. Encaminhar a porta tambem
    # quando o chamador usa o default evita validacao involuntaria de porta zero.
    $planParameters=@{Port=$Port};foreach($key in $PSBoundParameters.Keys){if($key -cne 'Diagnostico'){$planParameters[$key]=$PSBoundParameters[$key]}}
    $plan=Assert-WmsSql01LaunchPlan @planParameters
    $diagnosticPath=$null;$phase='PROTECTED_MATERIAL'
    if($Diagnostico) {
        $diagnosticPath=Resolve-WmsSql01Path (Join-Path $plan.run 'bootstrap-diagnostic.json') $plan.run
        if(Test-Path -LiteralPath $diagnosticPath) { throw 'SQL01_DIAGNOSTIC_ALREADY_EXISTS' }
    }
    $path=Join-Path $plan.run 'backend-readiness.json'
    $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    $runtimePath=$plan.runtimePath
    try{$runtimeStream=[IO.File]::Open($runtimePath,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)}catch{$stream.Dispose();throw 'SQL01_RUNTIME_RECEIPT_ALREADY_EXISTS_OR_REFUSED'}
    $receipt=[ordered]@{id='QUAL-CONF01-SQL01';owner='WMS - Prumo';startedUtc=[DateTime]::UtcNow.ToString('o');finishedUtc=$null;state='BLOCKED';code=$null
        runId=$plan.runId;profile='sqlserver-dev';database='WMS_DEV';login='WMSDEV';sourceHash=$plan.sourceHash;currentMainPomSourceHash=$plan.sourceHash;candidate=@{path=$plan.candidatePath;sha256=$plan.candidateSha256}
        backend=@{pid=$null;startUtc=$null;address='127.0.0.1';port=$Port;listenerOwned=$false;jarSha256=$plan.jarSha256}
        artifact=@{path=$plan.jar;sha256=$plan.jarSha256};config=@{path=$plan.config;sha256=(Get-FileHash -LiteralPath $plan.config).Hash}
        guard=@{path=$plan.guardPath;sha256=$plan.guardSha256};review=@{path=$plan.reviewPath;sha256=$plan.reviewSha256}
        localOrigin=$plan.localOrigin;observationDirectory=$plan.observation;secretTransport='PRIVATE_CHILD_STDIN_THREE_BOUNDED_FRAMES';bootstrapDisabled=$true;secretEnv=$false;rawLogsSaved=$false;existingProcessesAltered=$false
        sql=@{pid=$plan.guard.tls.proveniencia.reciboPID;startUtc=$plan.guard.tls.proveniencia.reciboInicioUtc};jdbcProviderProved=$false;httpAuthenticated=$false;error=$null}
    if($Diagnostico){$receipt.backend.exitCode=$null;$receipt.diagnostic=@{enabled=$true;parentPhase=$null;bootstrap=$null;bootstrapStatus='NOT_OBSERVED';messagesIncluded=$false}}
    try {
        . (Join-Path $script:WmsSql01Root 'database/scripts/d26-credencial-aplicacao.ps1')
        . (Join-Path $script:WmsSql01Root 'infra/auth/login-protegido.ps1')
        $credential=Get-WmsDevApplicationCredential
        if($credential.UserName -cne 'WMSDEV') { throw 'SQL01_CREDENTIAL_IDENTITY_INVALID' }
        $nativePath=Get-WmsLoginMaterialPath
        Assert-WmsLoginPrivatePath (Split-Path $nativePath -Parent);Assert-WmsLoginPrivatePath $nativePath
        $identity=Import-Clixml -LiteralPath $nativePath
        if($identity -isnot [Security.SecureString]) { throw 'SQL01_NATIVE_MATERIAL_INVALID' }
        $phase='PROCESS_ARGUMENTS'
        $info=[Diagnostics.ProcessStartInfo]::new();$info.FileName=$plan.java;$info.WorkingDirectory=Join-Path $script:WmsSql01Root 'backend'
        $info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
        $info.EnvironmentVariables.Clear()
        foreach($name in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE','LOCALAPPDATA','APPDATA')) {
            $value=[Environment]::GetEnvironmentVariable($name,'Process');if($value){$info.EnvironmentVariables[$name]=$value}
        }
        # Os handlers nested: do Boot precisam estar visiveis ao classloader da JVM.
        # URLClassLoader isolado no bootstrap nao basta para java.net.URL encontra-los.
        $arguments=@('-cp',$plan.jar,(Join-Path $PSScriptRoot 'Sql01Bootstrap.java'),$plan.jar,$plan.config,[string]$Port,[string]$plan.guard.alvo.servidor,
            [string]$plan.guard.perfilProtegido.certificateHost,$plan.truststore,$script:WmsSql01Root,$plan.observation,$plan.sourceHash,$plan.jarSha256,$plan.runId)
        if($plan.localOrigin -cne ('http://127.0.0.1:'+$Port)){$arguments=@('-Dwms.sql01.local-origin='+$plan.localOrigin)+$arguments}
        if($Diagnostico){$arguments+=,$diagnosticPath}
        foreach($value in $arguments) {if($value -match '["\r\n]'){throw 'SQL01_ARGUMENT_INVALID'}}
        $info.Arguments=(@($arguments|ForEach-Object {'"'+$_+'"'}) -join ' ')
        $process=New-WmsSql01Process;$process.StartInfo=$info
        $phase='PROCESS_START'
        if(-not $process.Start()) { throw 'SQL01_START_REFUSED' };$started=$true
        $receipt.backend.pid=$process.Id
        $nativeStart=$process.StartTime.ToUniversalTime()
        $ownMetadata=Get-WmsSql01BackendProcess $process.Id
        # O recibo e a revalidacao usam o mesmo canal/precisao CIM. Vincular o
        # metadado ao handle recem-criado antes de transportar qualquer segredo.
        $startDeltaTicks=$nativeStart.Ticks-$ownMetadata.StartTime.ToUniversalTime().Ticks
        if($process.HasExited -or $startDeltaTicks -lt 0 -or $startDeltaTicks -gt 9 -or
            $ownMetadata.ExecutablePath -ine $plan.java -or $ownMetadata.CommandLine.IndexOf($plan.jar,[StringComparison]::OrdinalIgnoreCase) -lt 0){throw 'SQL01_OWN_PROCESS_START_MISMATCH'}
        $receipt.backend.startUtc=$ownMetadata.StartTime.ToUniversalTime().ToString('o')
        $receipt.backend.nativeStartUtc=$nativeStart.ToString('o');$receipt.backend.metadataSource=$ownMetadata.metadataSource
        $receipt.backend.nativeToCimDeltaTicks=$startDeltaTicks;$receipt.backend.nativeToCimMaxTicks=9
        $process.BeginOutputReadLine();$process.BeginErrorReadLine()
        $phase='PRIVATE_TRANSPORT'
        Send-WmsSql01PrivateMaterial $process.StandardInput.BaseStream $credential.Password $identity
        $process.StandardInput.Close();$info.EnvironmentVariables.Clear();$credential.Password.Dispose();$credential=$null;$identity.Dispose();$identity=$null
        $phase='LISTENER_WAIT'
        $until=[DateTime]::UtcNow.AddSeconds(90)
        do {
            if($process.HasExited){throw 'SQL01_PROCESS_EXITED'}
            $listeners=@(Get-WmsSql01Listeners $Port)
            if($listeners.Count) {
                if(@($listeners|Where-Object {$_.OwningProcess -ne $process.Id -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'SQL01_LISTENER_NOT_OWNED'}
                $receipt.backend.listenerOwned=$true;$receipt.state='OWN_LISTENER_READY_JDBC_AUTH_PENDING';break
            }
            Start-Sleep -Milliseconds 250
        }while([DateTime]::UtcNow -lt $until)
        if(-not $receipt.backend.listenerOwned){throw 'SQL01_LISTENER_TIMEOUT'}
        $receipt.code='SQL01_OWN_LISTENER_READY'
        $script:WmsSql01Handles[$plan.runId]=$process
    } catch {
        $receipt.code='SQL01_START_FAILED'
        if($_.Exception.Message -cmatch '^SQL01_[A-Z0-9_]+$'){$receipt.code=$_.Exception.Message}
        $receipt.error=@{type=$_.Exception.GetType().FullName;hresult=$_.Exception.HResult;rawMessagePublished=$false}
        if($Diagnostico){$receipt.diagnostic.parentPhase=$phase;$receipt.error.chain=Get-WmsSql01ExceptionTypes $_.Exception}
        if($started -and $process -and -not $process.HasExited){$process.Kill();$null=$process.WaitForExit(5000)}
        if($Diagnostico -and $started -and $process -and $process.HasExited) {
            try{$receipt.backend.exitCode=$process.ExitCode}catch{$receipt.backend.exitCode=$null}
            if(Test-Path -LiteralPath $diagnosticPath) {
                try{$receipt.diagnostic.bootstrap=Read-WmsSql01BootstrapDiagnostic $diagnosticPath $plan.runId $process.Id;$receipt.diagnostic.bootstrapStatus='SANITIZED_SOURCE_BOUND'}
                catch{$receipt.diagnostic.bootstrapStatus='INVALID_OR_UNREADABLE'}
            }else{$receipt.diagnostic.bootstrapStatus='MISSING'}
        }
        if($process){$process.Dispose();$process=$null}
    } finally {
        if($credential){$credential.Password.Dispose()};if($identity){$identity.Dispose()};if($info){$info.EnvironmentVariables.Clear()}
        $receipt.finishedUtc=[DateTime]::UtcNow.ToString('o')
        try{$bytes=[Text.UTF8Encoding]::new($false).GetBytes(($receipt|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length);$runtimeStream.Write($bytes,0,$bytes.Length)}
        finally{$stream.Dispose();$runtimeStream.Dispose()}
    }
    [pscustomobject]@{RunId=$plan.runId;Process=$process;Sanitized=[pscustomobject]$receipt;ReceiptPath=$path;ReceiptSha256=(Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant();RuntimeReceiptPath=$runtimePath;RuntimeReceiptSha256=(Get-FileHash -LiteralPath $runtimePath).Hash.ToLowerInvariant()}
}

function Stop-WmsSql01Backend([Parameter(Mandatory)]$Handle) {
    if(-not $script:WmsSql01Handles.ContainsKey($Handle.RunId) -or -not [object]::ReferenceEquals($script:WmsSql01Handles[$Handle.RunId],$Handle.Process)) {throw 'SQL01_FOREIGN_HANDLE_REFUSED'}
    if(-not $Handle.Process.HasExited){$Handle.Process.Kill();$null=$Handle.Process.WaitForExit(5000)}
    $script:WmsSql01Handles.Remove($Handle.RunId);$Handle.Process.Dispose()
}
