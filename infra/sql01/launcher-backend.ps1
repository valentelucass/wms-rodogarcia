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
    $cor01Run=Join-Path $script:WmsSql01Root 'orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/prumo/runs/2c86b256-97b9-4fa8-a443-9d438241ee98'
    $cor01=$RunId -ceq '2c86b256-97b9-4fa8-a443-9d438241ee98' -and [IO.Path]::GetFullPath((Split-Path $Path -Parent)) -ieq $cor01Run
    if($cor01){$run=$cor01Run}
    $devRun=Join-Path $script:WmsSql01Root ('orchestracao/.runtime/be02-dev-fonte01/prumo/runs/'+$RunId)
    $dev=[IO.Path]::GetFullPath((Split-Path $Path -Parent)) -ieq $devRun
    if($dev){$run=$devRun}
    $resolved=Resolve-WmsSql01Path $Path $run
    if($resolved -ine (Join-Path $run 'bootstrap-diagnostic.json')) { throw 'SQL01_DIAGNOSTIC_PATH_INVALID' }
    $file=Get-Item -LiteralPath $resolved -ErrorAction Stop
    if($file.PSIsContainer -or $file.Length -gt 8192) { throw 'SQL01_DIAGNOSTIC_INVALID' }
    $value=Get-Content -LiteralPath $resolved -Raw -Encoding UTF8|ConvertFrom-Json
    $phases=@('ARGUMENTS','PUBLIC_PATHS','ARG_VALIDATION','JAR_HASH','PRIVATE_FRAMES','SECRET_INSTALL','APPLICATION_ARGUMENTS','JAR_CLASS_LOADING','JAR_MAIN_LOOKUP','APPLICATION_LAUNCH')
    if($cor01){$phases+=,'COR01_CONTEXT_AND_JDBC_PROOF'}
    if($dev){$phases+=,'DEV_CURRENT_CONTEXT_AND_JDBC_PROOF'}
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

function Assert-WmsCor01PublicPlan {
    param([Parameter(Mandatory)][string]$Cor01Plan,[Parameter(Mandatory)][string]$Cor01PlanSha256)
    $root=$script:WmsSql01Root
    $area=Join-Path $root 'orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01'
    $id='QUAL-CONF01-PERF02-INICIO01-COR01';$uuid='2c86b256-97b9-4fa8-a443-9d438241ee98'
    $path=Read-WmsSql01HashedFile @{path=$Cor01Plan;sha256=$Cor01PlanSha256} $area
    $p=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    $query='clienteId=67&armazemId=33&fuso=America%2FSao_Paulo&estado=TODAS&pagina=0&tamanho=100'
    if($p.id -cne $id -or $p.owner -cne 'WMS - Farol' -or $p.runId -cne $uuid -or
        $p.observationEnabled -isnot [bool] -or $p.observationEnabled -or $p.businessGETCount -ne 4 -or
        $p.frozenQuery -cne $query -or @($p.excludedDTOFields).Count -ne 1 -or $p.excludedDTOFields[0] -cne 'consultadoEm' -or
        $p.localOrigin -cne 'http://localhost:25581' -or $p.authSurfaceOrigin -cne 'https://1z8126n0-25581.brs.devtunnels.ms' -or
        $p.sourceHash -cne '6549ffb6d5005163ca464f00d177ecad37a90eb482b5a36806a278ed1fed359c' -or
        $p.jarSha256 -cne '423eeb81f301bd0db67898ca1bebb9d402b12761b94839419b27cbd0dafb45ca') {
        throw 'COR01_PLAN_BINDING_REQUIRED'
    }
    $run=Resolve-WmsSql01Path (Join-Path $area ('prumo/runs/'+$uuid)) (Join-Path $area 'prumo')
    foreach($binding in @(@('guardReceiptPath','guarda-atual.json'),@('runtimeReceiptPath','runtime-prumo.json'),@('jdbcReceiptPath','jdbc-provider.json'))) {
        if((Resolve-WmsSql01Path $p.($binding[0]) (Join-Path $area 'prumo')) -ine (Join-Path $run $binding[1])) {throw 'COR01_OUTPUT_BINDING_REQUIRED'}
    }
    $legacy=Join-Path $root 'orchestracao/.runtime/qual-conf01/sql01/cedro'
    $candidatePath=Read-WmsSql01HashedFile $p.candidate $legacy
    $expectedCandidate=Join-Path $legacy 'reg01/indicator-residual-v1/4f61b450-045b-49ad-96cf-c514667bc30e/candidate.json'
    if($candidatePath -ine $expectedCandidate -or $p.candidate.sha256 -ine 'bdd627b8655a7bfaa204eb77a0ae0e5ac18e258edae7bd2239b800fb138e959b'){throw 'COR01_CANDIDATE_REQUIRED'}
    $candidate=Get-Content -LiteralPath $candidatePath -Raw -Encoding UTF8|ConvertFrom-Json
    Assert-WmsSql01IndicatorCandidate $candidate $root
    . (Join-Path $root 'infra/dev/artefato-backend.ps1')
    $null=Assert-WmsDevApplicationArtifact $root $candidatePath
    $jar=Read-WmsSql01HashedFile $candidate.jar $legacy
    $java=Read-WmsSql01HashedFile $candidate.java ([Environment]::GetFolderPath('LocalApplicationData'))
    $config=Read-WmsSql01HashedFile $candidate.config $root
    $sources=@{launcher=$script:WmsSql01Launcher;bootstrap=(Join-Path $PSScriptRoot 'Sql01Bootstrap.java');transport=(Join-Path $PSScriptRoot 'transporte-privado.ps1');guardHelper=(Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')}
    foreach($name in $sources.Keys) {
        if((Read-WmsSql01HashedFile $p.$name $root) -ine $sources[$name]){throw 'COR01_SOURCE_BINDING_REQUIRED'}
    }
    if((Read-WmsSql01HashedFile $p.credential (Join-Path $area 'lume')) -ine (Join-Path $area 'lume/credencial-http-cor01.ps1')){throw 'COR01_CREDENTIAL_REQUIRED'}
    if((Read-WmsSql01HashedFile $p.apiPlan (Join-Path $area 'cedro')) -ine (Join-Path $area 'cedro/plano-before-after-final.json')){throw 'COR01_API_PLAN_REQUIRED'}
    if((Read-WmsSql01HashedFile $p.necessidadeV2 (Join-Path $area 'prumo')) -ine (Join-Path $area 'prumo/necessidade-minima-v2.json')){throw 'COR01_NECESSITY_REQUIRED'}
    # Public access metadata only; no protected material imported here.
    . (Join-Path $root 'infra/dev/acesso-dev.ps1')
    $access=Get-WmsDevAccess 25581
    if($access.mode -cne 'tunnel' -or $access.origin -cne '' -or $access.proxyOrigin -cne $p.localOrigin -or -not $access.secureCookie){throw 'COR01_AUTH_RUNTIME_REQUIRED'}
    $signingPath=Read-WmsSql01HashedFile $p.authSigningMetadata (Join-Path $area 'prumo')
    $signing=Get-Content -LiteralPath $signingPath -Raw -Encoding UTF8|ConvertFrom-Json
    . (Join-Path $root 'infra/auth/login-protegido.ps1')
    $privatePath=Get-WmsLoginMaterialPath
    Assert-WmsLoginPrivatePath (Split-Path $privatePath -Parent);Assert-WmsLoginPrivatePath $privatePath
    if($signing.signingMaterialMetadata.path -ine $privatePath -or $signing.signingMaterialMetadata.decrypted -ne $false -or
        $signing.signingMaterialMetadata.aclValidated -ne $true -or $signing.signingMaterialMetadata.precedesOperatorStart -ne $true -or
        (Get-FileHash -LiteralPath $privatePath).Hash -ine $signing.signingMaterialMetadata.encryptedFileSHA256 -or
        $signing.identifiers.issuer -cne 'https://wms.localhost' -or $signing.identifiers.audience -cne 'wms-dev'){throw 'COR01_SIGNING_METADATA_REQUIRED'}
    if($p.operator.pid -ne 61588 -or $p.operator.port -ne 25580 -or $p.operator.jarSha256 -ine '2c1afe85c7a0a34aeebb63f37083656cad34c9db1ca21cd43d5946ab46aca1df'){throw 'COR01_OPERATOR_BINDING_REQUIRED'}
    $operatorReceipt=Read-WmsSql01HashedFile $p.operator.receipt (Join-Path $root 'orchestracao/.runtime/frontend-integracao-dev-runs')
    $operator=Get-Content -LiteralPath $operatorReceipt -Raw -Encoding UTF8|ConvertFrom-Json
    $process=Get-WmsSql01BackendProcess 61588
    if($process.StartTime.ToUniversalTime().ToString('o') -cne $p.operator.startUtc -or
        $operator.backend.pid -ne 61588 -or $operator.backend.startUtc -cne $p.operator.startUtc -or
        $operator.profile -cne 'sqlserver-dev' -or $operator.database -cne 'WMS_DEV' -or $operator.login -cne 'WMSDEV' -or
        (Get-FileHash -LiteralPath $operator.artifact.path).Hash -ine $p.operator.jarSha256 -or
        $process.CommandLine.IndexOf($operator.artifact.path,[StringComparison]::OrdinalIgnoreCase) -lt 0 -or
        @(Get-WmsSql01Listeners 25580|Where-Object {$_.OwningProcess -eq 61588 -and $_.LocalAddress -ceq '127.0.0.1'}).Count -ne 1) {throw 'COR01_OPERATOR_CURRENT_REQUIRED'}
    [pscustomobject]@{area=$area;run=$run;runId=$uuid;plan=$p;planPath=$path;planSha256=$Cor01PlanSha256;candidate=$candidate;candidatePath=$candidatePath;jar=$jar;java=$java;config=$config;signing=$signing}
}

function Assert-WmsCor01Review($Bound,[string]$Review,[string]$ReviewSha256) {
    $path=Read-WmsSql01HashedFile @{path=$Review;sha256=$ReviewSha256} (Join-Path $Bound.area 'vigia')
    $r=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.id -cne 'QUAL-CONF01-PERF02-INICIO01-COR01' -or $r.owner -cne 'WMS - Vigia' -or
        $r.runId -cne $Bound.runId -or $r.SQLLaunchApproved -isnot [bool] -or -not $r.SQLLaunchApproved -or
        $r.OperationReviewApproved -isnot [bool] -or -not $r.OperationReviewApproved -or @($r.failed).Count -ne 0 -or
        $r.plan.path -ine $Bound.planPath -or $r.plan.sha256 -ine $Bound.planSha256 -or
        $r.sourceHash -cne $Bound.plan.sourceHash -or $r.jarSha256 -ine $Bound.plan.jarSha256 -or $r.localOrigin -cne $Bound.plan.localOrigin) {throw 'COR01_CURRENT_REVIEW_REQUIRED'}
    foreach($name in @('candidate','launcher','bootstrap','transport','credential','guardHelper','authSigningMetadata')) {
        if($r.$name.path -ine $Bound.plan.$name.path -or $r.$name.sha256 -ine $Bound.plan.$name.sha256){throw 'COR01_REVIEW_REFERENCE_MISMATCH'}
    }
    $readyPath=Read-WmsSql01HashedFile $r.ready (Join-Path $Bound.area 'lume')
    $ready=Get-Content -LiteralPath $readyPath -Raw -Encoding UTF8|ConvertFrom-Json
    if($ready.id -cne $r.id -or $ready.owner -cne 'WMS - Lume' -or $ready.runId -cne $Bound.runId -or
        $ready.commandReady -isnot [bool] -or -not $ready.commandReady -or $ready.executed -isnot [bool] -or $ready.executed -or
        $ready.plan.path -ine $Bound.planPath -or $ready.plan.sha256 -ine $Bound.planSha256){throw 'COR01_COMMAND_READY_REQUIRED'}
    [pscustomobject]@{path=$path;sha256=$ReviewSha256;review=$r}
}

function Read-WmsCor01CurrentGuard($Bound,[string]$GuardReceipt,[string]$GuardSha256) {
    $path=Read-WmsSql01HashedFile @{path=$GuardReceipt;sha256=$GuardSha256} $Bound.run
    if($path -ine $Bound.plan.guardReceiptPath){throw 'COR01_GUARD_PATH_REQUIRED'}
    $g=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    $begin=[DateTimeOffset]::Parse($g.inicioUtc);$finish=[DateTimeOffset]::Parse($g.observadoEm)
    if($begin -lt [DateTimeOffset]::UtcNow.AddMinutes(-5) -or $finish -lt $begin -or $finish -gt [DateTimeOffset]::UtcNow.AddSeconds(15) -or
        $g.guardaRealAprovada -ne $true -or $g.estado -cne 'GUARDA_ATUAL_APROVADA' -or $g.aberturaTentativas -ne 1 -or
        $g.aberturaConcluida -ne $true -or $g.alvo.banco -cne 'WMS_DEV' -or $g.alvo.login -cne 'WMSDEV' -or $g.alvo.usuario -cne 'WMSDEV' -or
        $g.tls.encrypt -cne 'Mandatory' -or $g.tls.trustServerCertificate -ne $false -or $g.tls.handshakeConcluido -ne $true -or
        $g.tls.confirmacaoAtual -ne $true -or @($g.checks).Count -ne 26 -or @($g.checks|Where-Object {$_.passou -ne $true}).Count -ne 0 -or
        $g.auxiliar.sha256 -ine $Bound.plan.guardHelper.sha256){throw 'COR01_CURRENT_GUARD_REQUIRED'}
    $sql=Get-WmsSql01SqlProcess ([int]$g.tls.proveniencia.reciboPID)
    if($sql.StartTime.ToUniversalTime().Ticks -ne [DateTimeOffset]::Parse($g.tls.proveniencia.reciboInicioUtc).UtcDateTime.Ticks){throw 'COR01_SQL_PROCESS_CHANGED'}
    $profile=$g.perfilProtegido
    if($profile.host -cne '127.0.0.1' -or $profile.port -ne 1433 -or $profile.database -cne 'WMS_DEV' -or
        $profile.login -cne 'WMSDEV' -or $profile.encrypt -ne $true -or $profile.trustServerCertificate -ne $false){throw 'COR01_SQL_PROFILE_REQUIRED'}
    $trust=Read-WmsSql01HashedFile @{path=$profile.truststore;sha256=$g.tls.truststoreSha256} ([Environment]::GetFolderPath('LocalApplicationData'))
    [pscustomobject]@{guard=$g;path=$path;sha256=$GuardSha256;truststore=$trust}
}

function Read-WmsCor01JdbcProof($Bound,[int]$ProcessId) {
    $path=Resolve-WmsSql01Path $Bound.plan.jdbcReceiptPath $Bound.run
    $j=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    if($j.id -cne 'QUAL-CONF01-SQL01' -or $j.runId -cne $Bound.runId -or $j.pid -ne $ProcessId -or
        $j.approved -isnot [bool] -or -not $j.approved -or $j.provider -cne 'SQLServer' -or $j.database -cne 'WMS_DEV' -or
        $j.login -cne 'WMSDEV' -or $j.user -cne 'WMSDEV' -or $j.databaseProductName -cne 'Microsoft SQL Server' -or
        $j.metadataStatementsExecuted -ne 1 -or $j.connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse -ne $true -or
        $j.privateTrustStoreAndCertificateHostConfigured -ne $true -or $j.declaredSourceHash -cne $Bound.plan.sourceHash -or
        $j.declaredJarSha256 -ine $Bound.plan.jarSha256){throw 'COR01_CURRENT_JDBC_REQUIRED'}
    [pscustomobject]@{path=$path;sha256=(Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant();proof=$j}
}

function Assert-WmsCor01LaunchPlan {
    param([string]$Candidato,[string]$CandidateSha256,[string]$GuardReceipt,[string]$GuardSha256,
        [string]$Review,[string]$ReviewSha256,[string]$RunDirectory,[string]$ObservationDirectory,[int]$Port,[string]$LocalOrigin,
        [string]$Cor01Plan,[string]$Cor01PlanSha256)
    $b=Assert-WmsCor01PublicPlan $Cor01Plan $Cor01PlanSha256
    $reviewed=Assert-WmsCor01Review $b $Review $ReviewSha256
    if($Port -ne 25586 -or [IO.Path]::GetFullPath($RunDirectory) -ine $b.run -or
        $Candidato -ine $b.candidatePath -or $CandidateSha256 -ine $b.plan.candidate.sha256 -or
        -not [string]::IsNullOrEmpty($ObservationDirectory) -or $LocalOrigin -cne $b.plan.localOrigin){throw 'COR01_LAUNCH_ARGUMENTS_REQUIRED'}
    if(-not (Test-Path -LiteralPath $b.run -PathType Container)){throw 'COR01_RUN_REQUIRED'}
    foreach($name in @('backend-readiness.json','runtime-prumo.json','jdbc-provider.json','http-gate.json','candidate-end.json')) {
        if(Test-Path -LiteralPath (Join-Path $b.run $name)){throw 'COR01_OUTPUT_ALREADY_EXISTS'}
    }
    if(@(Get-WmsSql01Listeners 25586).Count){throw 'COR01_PORT_OCCUPIED'}
    $current=Read-WmsCor01CurrentGuard $b $GuardReceipt $GuardSha256
    [pscustomobject]@{run=$b.run;runId=$b.runId;observation=$b.run;candidatePath=$b.candidatePath;candidateSha256=$CandidateSha256;
        sourceHash=$b.plan.sourceHash;jar=$b.jar;jarSha256=$b.plan.jarSha256;java=$b.java;config=$b.config;
        guard=$current.guard;guardPath=$current.path;guardSha256=$current.sha256;reviewPath=$reviewed.path;reviewSha256=$reviewed.sha256;
        truststore=$current.truststore;port=25586;localOrigin=$LocalOrigin;runtimePath=$b.plan.runtimeReceiptPath;cor01=$b}
}

function Assert-WmsCor01Runtime {
    param([Parameter(Mandatory)][string]$Cor01Plan,[Parameter(Mandatory)][string]$Cor01PlanSha256,
        [Parameter(Mandatory)][string]$RuntimeReceipt,[Parameter(Mandatory)][string]$RuntimeSha256)
    $b=Assert-WmsCor01PublicPlan $Cor01Plan $Cor01PlanSha256
    $path=Read-WmsSql01HashedFile @{path=$RuntimeReceipt;sha256=$RuntimeSha256} $b.run
    if($path -ine $b.plan.runtimeReceiptPath){throw 'COR01_RUNTIME_PATH_REQUIRED'}
    $r=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    if($r.id -cne 'QUAL-CONF01-PERF02-INICIO01-COR01' -or $r.owner -cne 'WMS - Prumo' -or $r.runId -cne $b.runId -or
        $r.code -cne 'COR01_OWN_RUNTIME_READY' -or $r.state -cne 'COR01_OWN_RUNTIME_JDBC_PROVED_AUTH_PENDING' -or
        $r.profile -cne 'sqlserver-dev' -or $r.database -cne 'WMS_DEV' -or $r.login -cne 'WMSDEV' -or
        $r.observationEnabled -ne $false -or $r.jdbcProviderProved -ne $true -or $r.secretEnv -ne $false -or
        $r.bootstrapDisabled -ne $true -or $r.existingProcessesAltered -ne $false -or
        $r.plan.path -ine $b.planPath -or $r.plan.sha256 -ine $b.planSha256 -or $r.sourceHash -cne $b.plan.sourceHash -or
        $r.artifact.path -ine $b.jar -or $r.artifact.sha256 -ine $b.plan.jarSha256 -or $r.localOrigin -cne $b.plan.localOrigin -or
        $r.config.path -ine $b.config -or $r.config.sha256 -ine $b.candidate.config.sha256 -or
        $r.authSigningMetadata.path -ine $b.plan.authSigningMetadata.path -or $r.authSigningMetadata.sha256 -ine $b.plan.authSigningMetadata.sha256 -or
        $r.backend.port -ne 25586 -or $r.backend.address -cne '127.0.0.1' -or $r.backend.listenerOwned -ne $true){throw 'COR01_RUNTIME_BINDING_REQUIRED'}
    $null=Assert-WmsCor01Review $b $r.review.path $r.review.sha256
    $currentGuard=Read-WmsCor01CurrentGuard $b $r.guard.path $r.guard.sha256
    if($r.sql.pid -ne $currentGuard.guard.tls.proveniencia.reciboPID -or
        $r.sql.startUtc -cne $currentGuard.guard.tls.proveniencia.reciboInicioUtc -or
        $r.credential.path -ine $b.plan.credential.path -or $r.credential.sha256 -ine $b.plan.credential.sha256 -or
        $r.authOrigin -cne '' -or $r.proxyOrigin -cne 'http://localhost:25581' -or $r.secureCookie -ne $true -or
        $r.authSurfaceOrigin -cne $b.plan.authSurfaceOrigin){throw 'COR01_RUNTIME_AUTH_SQL_BINDING_REQUIRED'}
    $process=Get-WmsSql01BackendProcess ([int]$r.backend.pid)
    $listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop|Where-Object {$_.OwningProcess -eq $process.Id})
    if($process.StartTime.ToUniversalTime().ToString('o') -cne $r.backend.startUtc -or
        $process.ExecutablePath -ine $b.java -or $process.CommandLine.IndexOf($b.jar,[StringComparison]::OrdinalIgnoreCase) -lt 0 -or
        $process.CommandLine.IndexOf('-Dwms.sql01.scope=QUAL-CONF01-PERF02-INICIO01-COR01',[StringComparison]::Ordinal) -lt 0 -or
        $listeners.Count -ne 1 -or $listeners[0].LocalPort -ne 25586 -or $listeners[0].LocalAddress -cne '127.0.0.1'){throw 'COR01_CURRENT_PROCESS_REQUIRED'}
    $jdbc=Read-WmsCor01JdbcProof $b $process.Id
    if([DateTimeOffset]::Parse($jdbc.proof.startedUtc) -lt [DateTimeOffset]::Parse($r.startedUtc) -or
        [DateTimeOffset]::Parse($jdbc.proof.finishedUtc) -gt [DateTimeOffset]::Parse($r.finishedUtc) -or
        @(Get-NetTCPConnection -State Established -ErrorAction Stop|Where-Object {$_.OwningProcess -eq $process.Id -and $_.RemoteAddress -eq '127.0.0.1' -and $_.RemotePort -eq 1433}).Count -eq 0) {throw 'COR01_CURRENT_POOL_LINK_REQUIRED'}
    if($jdbc.path -ine $r.jdbc.path -or $jdbc.sha256 -ine $r.jdbc.sha256 -or
        (Get-FileHash -LiteralPath (Join-Path $b.run 'backend-readiness.json')).Hash -ine $RuntimeSha256){throw 'COR01_CURRENT_PROOF_HASH_REQUIRED'}
    [pscustomobject]@{Bound=$b;Runtime=$r;RuntimePath=$path;RuntimeSha256=$RuntimeSha256;Jdbc=$jdbc}
}

function New-WmsCor01HttpGate {
    param([Parameter(Mandatory)][string]$Cor01Plan,[Parameter(Mandatory)][string]$Cor01PlanSha256,
        [Parameter(Mandatory)][string]$RuntimeReceipt,[Parameter(Mandatory)][string]$RuntimeSha256)
    $checked=Assert-WmsCor01Runtime @PSBoundParameters
    $b=$checked.Bound;$r=$checked.Runtime
    $path=Join-Path $b.run 'http-gate.json'
    $stream=[IO.File]::Open($path,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
    try{
        $gate=@{id='QUAL-CONF01-PERF02-INICIO01-COR01';owner='WMS - Prumo';runId=$b.runId;issuedUtc=[DateTime]::UtcNow.ToString('o');
            protectedHttpAllowed=$true;businessGETCount=4;plan=@{path=$b.planPath;sha256=$b.planSha256};
            runtime=@{path=$RuntimeReceipt;sha256=$RuntimeSha256};guard=$r.guard;jdbc=$r.jdbc;review=$r.review;
            targets=@{operator='http://127.0.0.1:25580';candidate='http://127.0.0.1:25586'};
            localOrigin=$b.plan.localOrigin;authSurfaceOrigin=$b.plan.authSurfaceOrigin;observationEnabled=$false}
        $bytes=[Text.UTF8Encoding]::new($false).GetBytes(($gate|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length)
    }finally{$stream.Dispose()}
    [pscustomobject]@{path=$path;sha256=(Get-FileHash -LiteralPath $path).Hash;gate=[pscustomobject]$gate}
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
        [string]$Review,[string]$ReviewSha256,
        [Parameter(Mandatory)][string]$RunDirectory,[string]$ObservationDirectory,[int]$Port=25584,[switch]$Diagnostico,[string]$LocalOrigin,
        [switch]$Cor01Readonly,[string]$Cor01Plan,[string]$Cor01PlanSha256,[switch]$DevCurrent)
    $ErrorActionPreference='Stop';$credential=$null;$identity=$null;$process=$null;$started=$false;$info=$null
    # PSBoundParameters nao inclui valores default. Encaminhar a porta tambem
    # quando o chamador usa o default evita validacao involuntaria de porta zero.
    $planParameters=@{Port=$Port};foreach($key in $PSBoundParameters.Keys){if($key -cnotin @('Diagnostico','Cor01Readonly','DevCurrent')){$planParameters[$key]=$PSBoundParameters[$key]}}
    if($DevCurrent){
        if($Cor01Readonly -or $Review -or $ReviewSha256 -or $Cor01Plan -or $Cor01PlanSha256 -or $ObservationDirectory){throw 'DEV_CURRENT_MODE_BINDING_REFUSED'}
        . (Join-Path $script:WmsSql01Root 'infra/dev/artefato-backend.ps1')
        $plan=Assert-WmsDevCurrentLaunchPlan @planParameters
    }
    elseif($Cor01Readonly){$plan=Assert-WmsCor01LaunchPlan @planParameters}
    else{
        if($PSBoundParameters.ContainsKey('Cor01Plan') -or $PSBoundParameters.ContainsKey('Cor01PlanSha256')){throw 'COR01_MODE_REQUIRED'}
        $plan=Assert-WmsSql01LaunchPlan @planParameters
    }
    $diagnosticPath=$null;$phase='PROTECTED_MATERIAL'
    if($Diagnostico) {
        $diagnosticPath=Resolve-WmsSql01Path (Join-Path $plan.run 'bootstrap-diagnostic.json') $plan.run
        if($Cor01Readonly){$diagnosticPath=Join-Path $plan.run 'bootstrap-diagnostic.json'}
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
    if($Cor01Readonly){
        $receipt.id='QUAL-CONF01-PERF02-INICIO01-COR01'
        $receipt.plan=@{path=$plan.cor01.planPath;sha256=$plan.cor01.planSha256}
        $receipt.credential=$plan.cor01.plan.credential
        $receipt.observationDirectory=$null;$receipt.observationEnabled=$false
        $receipt.auth=@{origin='';proxyOrigin=$plan.localOrigin;secureCookie=$true;issuer='https://wms.localhost';audience='wms-dev'}
        $receipt.authOrigin='';$receipt.proxyOrigin=$plan.localOrigin;$receipt.secureCookie=$true
        $receipt.authSurfaceOrigin=$plan.cor01.plan.authSurfaceOrigin
        $receipt.authSigningMetadata=$plan.cor01.plan.authSigningMetadata
        $receipt.jdbc=$null
    }
    if($DevCurrent){
        $receipt.id='BE02-DEV-FONTE01';$receipt.natureza='D31_DEV02_BACKEND_READINESS';$receipt.projectPath=$plan.root
        $receipt.estado='BLOCKED';$receipt.observationDirectory=$null;$receipt.observationEnabled=$false;$receipt.jdbc=$null
        $receipt.fingerprint=$plan.candidate.fingerprint;$receipt.java=$plan.candidate.java
        $receipt.guard=$plan.guardReference;$receipt.guard.path=$plan.guardPath
        $receipt.authOrigin=$plan.access.origin;$receipt.proxyOrigin=$plan.access.proxyOrigin;$receipt.secureCookie=$plan.access.secureCookie
        $receipt.accessMode=$plan.access.mode;$receipt.backend.url='http://127.0.0.1:25580'
        $receipt.Remove('review')
    }
    if($Diagnostico){$receipt.backend.exitCode=$null;$receipt.diagnostic=@{enabled=$true;parentPhase=$null;bootstrap=$null;bootstrapStatus='NOT_OBSERVED';messagesIncluded=$false}}
    try {
        . (Join-Path $script:WmsSql01Root 'database/scripts/d26-credencial-aplicacao.ps1')
        . (Join-Path $script:WmsSql01Root 'infra/auth/login-protegido.ps1')
        $credential=Get-WmsDevApplicationCredential
        if($credential.UserName -cne 'WMSDEV') { throw 'SQL01_CREDENTIAL_IDENTITY_INVALID' }
        $nativePath=Get-WmsLoginMaterialPath
        Assert-WmsLoginPrivatePath (Split-Path $nativePath -Parent);Assert-WmsLoginPrivatePath $nativePath
        if($DevCurrent){
            $signing=[ordered]@{id='BE02-DEV-FONTE01';runId=$plan.runId;path=$nativePath;encryptedFileSHA256=(Get-FileHash -LiteralPath $nativePath).Hash;ACLValidated=$true;issuer='https://wms.localhost';audience='wms-dev';protectedSourceOnly=$true;keyPublished=$false}
            $signingPath=Join-Path $plan.run 'auth-signing-metadata.json'
            Write-WmsDevCreateOnlyJson $signingPath $signing
            $receipt.authSigningMetadata=@{path=$signingPath;sha256=(Get-FileHash -LiteralPath $signingPath).Hash}
        }
        if($Cor01Readonly -and (Get-FileHash -LiteralPath $nativePath).Hash -ine $plan.cor01.signing.signingMaterialMetadata.encryptedFileSHA256){throw 'COR01_SIGNING_SOURCE_CHANGED'}
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
        if($Cor01Readonly){$arguments=@('-Dwms.sql01.scope=QUAL-CONF01-PERF02-INICIO01-COR01','-Dwms.sql01.observation.disabled=true')+$arguments}
        if($Diagnostico){$arguments+=,$diagnosticPath}
        if($DevCurrent){$arguments=@(Get-WmsDevCurrentBackendArguments $plan)}
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
                if($DevCurrent){
                    if(-not(Test-Path -LiteralPath (Join-Path $plan.run 'jdbc-provider.json'))){Start-Sleep -Milliseconds 250;continue}
                    $proof=Read-WmsDevCurrentJdbcProof $plan $process.Id
                    if([DateTimeOffset]::Parse($proof.value.startedUtc) -lt [DateTimeOffset]::Parse($receipt.backend.startUtc)){throw 'DEV_CURRENT_JDBC_TIME_INVALID'}
                    $receipt.jdbc=@{path=$proof.path;sha256=$proof.sha256};$receipt.jdbcProviderProved=$true
                    $receipt.state='DEV_CURRENT_JDBC_READY';$receipt.estado='READY'
                }
                elseif($Cor01Readonly){
                    if(-not(Test-Path -LiteralPath $plan.cor01.plan.jdbcReceiptPath)){Start-Sleep -Milliseconds 250;continue}
                    $proof=Read-WmsCor01JdbcProof $plan.cor01 $process.Id
                    $receipt.jdbc=@{path=$proof.path;sha256=$proof.sha256}
                    $receipt.jdbcProviderProved=$true
                    $receipt.state='COR01_OWN_RUNTIME_JDBC_PROVED_AUTH_PENDING'
                }else{$receipt.state='OWN_LISTENER_READY_JDBC_AUTH_PENDING'}
                $receipt.backend.listenerOwned=$true;break
            }
            Start-Sleep -Milliseconds 250
        }while([DateTime]::UtcNow -lt $until)
        if(-not $receipt.backend.listenerOwned){throw 'SQL01_LISTENER_TIMEOUT'}
        $receipt.code=if($DevCurrent){'DEV_CURRENT_READY'}elseif($Cor01Readonly){'COR01_OWN_RUNTIME_READY'}else{'SQL01_OWN_LISTENER_READY'}
        $script:WmsSql01Handles[$plan.runId]=$process
    } catch {
        $receipt.code='SQL01_START_FAILED'
        if($_.Exception.Message -cmatch '^(SQL01|DEV_CURRENT)_[A-Z0-9_]+$'){$receipt.code=$_.Exception.Message}
        $receipt.error=@{type=$_.Exception.GetType().FullName;hresult=$_.Exception.HResult;rawMessagePublished=$false}
        if($Diagnostico){$receipt.diagnostic.parentPhase=$phase;$receipt.error.chain=Get-WmsSql01ExceptionTypes $_.Exception}
        if($started -and $process -and -not $process.HasExited){
            if($Cor01Readonly -or $DevCurrent){
                # A failure is not authority to kill an inconclusively owned process.
                $script:WmsSql01Handles[$plan.runId]=$process
                $receipt.cleanupPendingOwnershipGate=$true
            }else{$process.Kill();$null=$process.WaitForExit(5000)}
        }
        if($Diagnostico -and $started -and $process -and $process.HasExited) {
            try{$receipt.backend.exitCode=$process.ExitCode}catch{$receipt.backend.exitCode=$null}
            if(Test-Path -LiteralPath $diagnosticPath) {
                try{$receipt.diagnostic.bootstrap=Read-WmsSql01BootstrapDiagnostic $diagnosticPath $plan.runId $process.Id;$receipt.diagnostic.bootstrapStatus='SANITIZED_SOURCE_BOUND'}
                catch{$receipt.diagnostic.bootstrapStatus='INVALID_OR_UNREADABLE'}
            }else{$receipt.diagnostic.bootstrapStatus='MISSING'}
        }
        if($process -and ((-not $Cor01Readonly -and -not $DevCurrent) -or $process.HasExited)){$process.Dispose();$process=$null}
    } finally {
        if($credential){$credential.Password.Dispose()};if($identity){$identity.Dispose()};if($info){$info.EnvironmentVariables.Clear()}
        $receipt.finishedUtc=[DateTime]::UtcNow.ToString('o')
        if($DevCurrent){$receipt.observadoUtc=$receipt.finishedUtc}
        try{$bytes=[Text.UTF8Encoding]::new($false).GetBytes(($receipt|ConvertTo-Json -Depth 8));$stream.Write($bytes,0,$bytes.Length);$runtimeStream.Write($bytes,0,$bytes.Length)}
        finally{$stream.Dispose();$runtimeStream.Dispose()}
    }
    [pscustomobject]@{RunId=$plan.runId;Process=$process;Sanitized=[pscustomobject]$receipt;ReceiptPath=$path;ReceiptSha256=(Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant();RuntimeReceiptPath=$runtimePath;RuntimeReceiptSha256=(Get-FileHash -LiteralPath $runtimePath).Hash.ToLowerInvariant()}
}

function Stop-WmsSql01Backend([Parameter(Mandatory)]$Handle) {
    if(-not $script:WmsSql01Handles.ContainsKey($Handle.RunId) -or -not [object]::ReferenceEquals($script:WmsSql01Handles[$Handle.RunId],$Handle.Process)) {throw 'SQL01_FOREIGN_HANDLE_REFUSED'}
    if($Handle.Sanitized.id -ceq 'BE02-DEV-FONTE01'){
        . (Join-Path $script:WmsSql01Root 'infra/dev/artefato-backend.ps1')
        . (Join-Path $script:WmsSql01Root 'infra/dev/reiniciar-instancia.ps1')
        $run=Join-Path $script:WmsSql01Root ('orchestracao/.runtime/be02-dev-fonte01/prumo/runs/'+$Handle.RunId)
        $path=Read-WmsSql01HashedFile @{path=$Handle.RuntimeReceiptPath;sha256=$Handle.RuntimeReceiptSha256} $run
        $r=Get-Content -LiteralPath $path -Raw|ConvertFrom-Json
        if($r.id -cne 'BE02-DEV-FONTE01' -or $r.runId -cne $Handle.RunId -or $r.backend.pid -ne $Handle.Process.Id -or
            $r.backend.port -ne 25580 -or $r.profile -cne 'sqlserver-dev'){throw 'DEV_CURRENT_END_OWNER_NOT_CONFIRMED'}
        if(!$Handle.Process.HasExited){
            $p=Get-WmsSql01BackendProcess $r.backend.pid
            $g=Get-Content -LiteralPath (Read-WmsSql01HashedFile $r.guard $run) -Raw|ConvertFrom-Json
            $plan=@{root=$script:WmsSql01Root;run=$run;runId=$r.runId;jar=$r.artifact.path;config=$r.config.path;guard=$g;truststore=$g.perfilProtegido.truststore;access=@{mode=$r.accessMode};localOrigin=$r.localOrigin;sourceHash=$r.sourceHash;jarSha256=$r.artifact.sha256}
            $expected=@($r.java.path)+@(Get-WmsDevCurrentBackendArguments $plan)
            $actual=@(Split-WmsDevCommandLine $p.CommandLine)
            if($p.StartTime.ToUniversalTime().ToString('o') -cne $r.backend.startUtc -or $p.ExecutablePath -ine $r.java.path -or
                $actual.Count -ne $expected.Count){throw 'DEV_CURRENT_END_OWNER_NOT_CONFIRMED'}
            for($i=0;$i -lt $actual.Count;$i++){if($actual[$i] -cne $expected[$i]){throw 'DEV_CURRENT_END_OWNER_NOT_CONFIRMED'}}
            $ports=@(Get-WmsDevListeners|Where-Object OwningProcess -eq $p.Id)
            if(@($ports|Where-Object {$_.LocalPort -ne 25580 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'DEV_CURRENT_END_ADDITIONAL_PORT_REFUSED'}
            foreach($ref in @($r.artifact,$r.config,$r.java)){if((Get-FileHash -LiteralPath $ref.path).Hash -ine $ref.sha256){throw 'DEV_CURRENT_END_HASH_CHANGED'}}
            $Handle.Process.Kill();if(!$Handle.Process.WaitForExit(5000)){throw 'DEV_CURRENT_END_TIMEOUT'}
        }
        Write-WmsDevCreateOnlyJson (Join-Path $run 'backend-end.json') @{id=$r.id;runId=$r.runId;pid=$r.backend.pid;endedUtc=[DateTime]::UtcNow.ToString('o');onlyOwnBackend=$true}
        $script:WmsSql01Handles.Remove($Handle.RunId);$Handle.Process.Dispose()
        return
    }
    if($Handle.RunId -ceq '2c86b256-97b9-4fa8-a443-9d438241ee98'){
        $path=Resolve-WmsSql01Path $Handle.RuntimeReceiptPath (Join-Path $script:WmsSql01Root 'orchestracao/.runtime/qual-conf01/perf02-inicio01/cor01/prumo/runs/2c86b256-97b9-4fa8-a443-9d438241ee98')
        if((Get-FileHash -LiteralPath $path).Hash -ine $Handle.RuntimeReceiptSha256){throw 'COR01_END_RECEIPT_CHANGED'}
        $r=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
        $alreadyExited=$Handle.Process.HasExited
        if($r.id -cne 'QUAL-CONF01-PERF02-INICIO01-COR01' -or $r.runId -cne $Handle.RunId -or
            $r.backend.pid -ne $Handle.Process.Id -or $r.backend.pid -in @(61588,59336,48480) -or
            $r.backend.port -ne 25586 -or $r.profile -cne 'sqlserver-dev' -or
            $r.artifact.sha256 -ine '423eeb81f301bd0db67898ca1bebb9d402b12761b94839419b27cbd0dafb45ca'){throw 'COR01_END_OWNERSHIP_REQUIRED'}
        if(-not $alreadyExited){
            $p=Get-WmsSql01BackendProcess $Handle.Process.Id
            $listeners=@(Get-NetTCPConnection -State Listen -ErrorAction Stop|Where-Object {$_.OwningProcess -eq $p.Id})
            if($p.StartTime.ToUniversalTime().ToString('o') -cne $r.backend.startUtc -or
                $p.CommandLine.IndexOf($r.artifact.path,[StringComparison]::OrdinalIgnoreCase) -lt 0 -or
                @($listeners|Where-Object {$_.LocalPort -ne 25586 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'COR01_END_CURRENT_OWNERSHIP_REQUIRED'}
        }
        $end=Join-Path (Split-Path $path -Parent) 'candidate-end.json'
        $stream=[IO.File]::Open($end,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
        try{
            if(-not $alreadyExited){$Handle.Process.Kill();if(-not $Handle.Process.WaitForExit(5000)){throw 'COR01_END_TIMEOUT'}}
            $bytes=[Text.UTF8Encoding]::new($false).GetBytes((@{id=$r.id;runId=$r.runId;pid=$r.backend.pid;startUtc=$r.backend.startUtc;endedUtc=[DateTime]::UtcNow.ToString('o');alreadyExited=$alreadyExited;onlyOwnCandidate=$true;existingProcessesAltered=$false}|ConvertTo-Json))
            $stream.Write($bytes,0,$bytes.Length)
        }finally{$stream.Dispose()}
    }
    if(-not $Handle.Process.HasExited){$Handle.Process.Kill();$null=$Handle.Process.WaitForExit(5000)}
    $script:WmsSql01Handles.Remove($Handle.RunId);$Handle.Process.Dispose()
}
