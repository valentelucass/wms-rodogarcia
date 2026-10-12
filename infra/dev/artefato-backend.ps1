Set-StrictMode -Version Latest

# BE02: build sempre, sem cache/pacote historico ou selecao por data.
function Write-WmsDevCreateOnlyJson([string]$Path, $Value) {
    $bytes=[Text.UTF8Encoding]::new($false).GetBytes(($Value|ConvertTo-Json -Depth 20)+[Environment]::NewLine)
    $stream=[IO.File]::Open([IO.Path]::GetFullPath($Path),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::Read)
    try{$stream.Write($bytes,0,$bytes.Length)}finally{$stream.Dispose()}
}

function Get-WmsDevInputFingerprint([string]$Repository) {
    $ErrorActionPreference='Stop'
    $root=[IO.Path]::GetFullPath($Repository).TrimEnd('\','/')
    $files=@(Get-Item -LiteralPath (Join-Path $root 'backend/pom.xml'))+
        @(Get-Item -LiteralPath (Join-Path $root 'backend/mvnw'))+
        @(Get-Item -LiteralPath (Join-Path $root 'backend/mvnw.cmd'))+
        @(Get-ChildItem -LiteralPath (Join-Path $root 'backend/.mvn') -Recurse -File)+
        @(Get-ChildItem -LiteralPath (Join-Path $root 'backend/src/main') -Recurse -File)
    foreach($path in @('infra/dev/preparar-backend-dev.ps1','infra/dev/artefato-backend.ps1','infra/sql01/launcher-backend.ps1','infra/sql01/Sql01Bootstrap.java','infra/dev/acesso-dev.ps1','infra/dev/acesso-dev.json')){$files+=Get-Item -LiteralPath (Join-Path $root $path)}
    $records=@($files|ForEach-Object{
        $cursor=$_.FullName
        while($cursor.Length -gt $root.Length){
            if((Get-Item -LiteralPath $cursor -Force).Attributes -band [IO.FileAttributes]::ReparsePoint){throw 'DEV_CURRENT_INPUT_LINK_REFUSED'}
            $cursor=[IO.Path]::GetDirectoryName($cursor)
        }
        [pscustomobject]@{path=$_.FullName.Substring($root.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()}
    }|Sort-Object path)
    foreach($path in @('backend/.mvn/maven.config','backend/.mvn/jvm.config','backend/.mvn/extensions.xml')){
        if(!(Test-Path -LiteralPath (Join-Path $root $path))){$records+=[pscustomobject]@{path=$path;sha256='ABSENT'}}
    }
    $tool=Get-Content -LiteralPath (Join-Path $root 'backend/evidencias/login-d32-preparo.json') -Raw|ConvertFrom-Json
    $taskJdkHome=Split-Path (Split-Path $tool.java.path -Parent) -Parent
    foreach($path in @('bin/java.exe','bin/javac.exe','release','lib/modules')){
        $records+=[pscustomobject]@{path=('JDK21/'+$path);sha256=(Get-FileHash -LiteralPath (Join-Path $taskJdkHome $path)).Hash.ToLowerInvariant()}
    }
    $records=@($records|Sort-Object path)
    $sha=[Security.Cryptography.SHA256]::Create()
    try{$hash=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes((@($records|ForEach-Object{$_.path+'='+$_.sha256}) -join "`n"))))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
    [pscustomobject]@{schema=1;sha256=$hash;sourceHash=(Get-WmsBackendSourceHash $root);records=$records}
}

function Get-WmsDevPackageMetadata([string]$Jar) {
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip=[IO.Compression.ZipFile]::OpenRead($Jar)
    $dependencies=[Collections.Generic.List[object]]::new();$classes=[Collections.Generic.List[object]]::new();$resources=[Collections.Generic.List[object]]::new()
    try{
        $names=@($zip.Entries|ForEach-Object FullName)
        if(@($names|Where-Object{$_ -match '(^/|\\|(^|/)\.\.(/|$))'}).Count){throw 'DEV_CURRENT_JAR_ENTRY_PATH_INVALID'}
        if(@($names|Group-Object|Where-Object Count -gt 1).Count){throw 'DEV_CURRENT_JAR_DUPLICATE_ENTRY'}
        $manifest=$zip.GetEntry('META-INF/MANIFEST.MF')
        if(!$manifest){throw 'DEV_CURRENT_MANIFEST_MISSING'}
        $reader=[IO.StreamReader]::new($manifest.Open());try{$text=$reader.ReadToEnd()}finally{$reader.Dispose()}
        if($text -notmatch '(?m)^Start-Class: br.com.rodogarcia.wms.WmsApplication\r?$' -or
            $text -notmatch '(?m)^Main-Class: org.springframework.boot.loader.launch.JarLauncher\r?$' -or
            $text -notmatch '(?m)^Build-Jdk-Spec: 21\r?$' -or $text -notmatch '(?m)^Spring-Boot-Classes: BOOT-INF/classes/\r?$' -or
            $text -notmatch '(?m)^Spring-Boot-Lib: BOOT-INF/lib/\r?$'){throw 'DEV_CURRENT_MANIFEST_INVALID'}
        foreach($entry in $zip.Entries){
            if($entry.FullName.EndsWith('/')){continue}
            if($entry.FullName -match '(?i)^BOOT-INF/lib/h2-'){throw 'DEV_CURRENT_H2_RUNTIME_REFUSED'}
            $stream=$entry.Open();$sha=[Security.Cryptography.SHA256]::Create()
            try{$hash=([BitConverter]::ToString($sha.ComputeHash($stream))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose();$stream.Dispose()}
            $record=[pscustomobject]@{path=$entry.FullName;sha256=$hash;bytes=$entry.Length}
            if($entry.FullName -like 'BOOT-INF/lib/*.jar'){
                $dependencies.Add($record)
                $memory=[IO.MemoryStream]::new();$innerStream=$entry.Open()
                try{
                    $innerStream.CopyTo($memory);$memory.Position=0
                    $inner=[IO.Compression.ZipArchive]::new($memory,[IO.Compression.ZipArchiveMode]::Read,$true)
                    try{if($inner.GetEntry('org/h2/Driver.class') -or @($inner.Entries|Where-Object FullName -like 'META-INF/maven/com.h2database/*').Count){throw 'DEV_CURRENT_H2_RUNTIME_REFUSED'}}finally{$inner.Dispose()}
                }finally{$innerStream.Dispose();$memory.Dispose()}
            }elseif($entry.FullName -like 'BOOT-INF/classes/*.class'){$classes.Add($record)}
            elseif($entry.FullName -eq 'META-INF/MANIFEST.MF'){$manifestHash=$hash}
            else{$resources.Add($record)}
        }
        foreach($name in @('WmsApplication','controllers/VisaoOperacaoController','services/VisaoOperacaoService','services/IndicadorEstoqueService','services/CalculoCobrancaService','config/AmbienteConfig','config/SqlServerConfig','config/LoginConfig','config/CadastrosSegurancaConfig','config/Sql01JdbcObservation')){
            if(!$zip.GetEntry('BOOT-INF/classes/br/com/rodogarcia/wms/'+$name+'.class')){throw 'DEV_CURRENT_CLASS_MISSING'}
        }
        if(!$dependencies.Count -or !@($dependencies|Where-Object path -match '/mssql-jdbc-').Count){throw 'DEV_CURRENT_SQL_DRIVER_MISSING'}
        $sha=[Security.Cryptography.SHA256]::Create()
        try{$depsHash=([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes((@($dependencies|Sort-Object path|ForEach-Object{$_.path+'='+$_.sha256}) -join "`n"))))).Replace('-','').ToLowerInvariant()}finally{$sha.Dispose()}
        if($zip.GetEntry('BOOT-INF/classes/org/h2/Driver.class')){throw 'DEV_CURRENT_H2_RUNTIME_REFUSED'}
        [pscustomobject]@{manifestSha256=$manifestHash;classes=$classes.ToArray();resources=$resources.ToArray();dependencies=$dependencies.ToArray();dependenciesSha256=$depsHash;newH2Executed=$false}
    }finally{$zip.Dispose()}
}

function Assert-WmsDevCurrentArtifact([string]$Repository,[string]$ReceiptPath,[string]$ReceiptSha256) {
    $root=[IO.Path]::GetFullPath($Repository)
    . (Join-Path $root 'infra/sql01/launcher-backend.ps1')
    $area=Join-Path $root 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs'
    $path=Read-WmsSql01HashedFile @{path=$ReceiptPath;sha256=$ReceiptSha256} $area
    $a=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    if($a.id -cne 'BE02-DEV-FONTE01' -or $a.build.exit -ne 0 -or $a.build.scope -cne 'PURE_NO_DATABASE' -or
        $a.build.testsExecuted -ne 0 -or $a.build.H2 -ne 0 -or $a.reused -ne $false -or
        $a.sourceHash -cne $a.sourceHashBefore -or $a.sourceHash -cne $a.sourceHashAfter){throw 'DEV_CURRENT_BUILD_NOT_APPROVED'}
    if($a.schema -cne 'WMS_CURRENT_DEV_ARTIFACT_V1' -or $a.owner -cne 'WMS - Prumo' -or
        $a.build.mavenTestSkip -cne 'true' -or $a.build.goal -cne 'package' -or
        !$a.build.observed.testSourcesNotCompiled -or !$a.build.observed.testsSkipped -or !$a.build.observed.buildSuccess -or
        $a.fingerprintBefore.sha256 -cne $a.fingerprintAfter.sha256 -or $a.fingerprintAfter.sha256 -cne $a.fingerprint.sha256){throw 'DEV_CURRENT_BUILD_PROOF_INCOMPLETE'}
    $current=Get-WmsDevInputFingerprint $root
    if($a.sourceHash -cne $current.sourceHash -or $a.fingerprint.sha256 -cne $current.sha256){throw 'DEV_CURRENT_SOURCE_CHANGED'}
    $uuid=[guid]::Empty
    if(![guid]::TryParseExact($a.runId,'D',[ref]$uuid) -or $path -ine (Join-Path $area ($a.runId+'/preparo-candidato.json'))){throw 'DEV_CURRENT_RUN_INVALID'}
    foreach($ref in @($a.jar,$a.config)){ $null=Read-WmsSql01HashedFile $ref (Split-Path $path -Parent) }
    if($a.jar.path -ine (Join-Path (Split-Path $path -Parent) 'target/wms-backend-0.0.1-SNAPSHOT.jar') -or
        $a.config.path -ine (Join-Path (Split-Path $path -Parent) 'application-frontend-dev.properties') -or
        $a.config.sha256 -ine (Get-FileHash -LiteralPath (Join-Path $root 'backend/src/main/resources/application-frontend-dev.properties')).Hash){throw 'DEV_CURRENT_ARTIFACT_PATH_INVALID'}
    foreach($ref in @($a.java,$a.javaRelease)){
        if((Get-FileHash -LiteralPath $ref.path -Algorithm SHA256).Hash -ine $ref.sha256){throw 'DEV_CURRENT_TOOLCHAIN_CHANGED'}
    }
    $package=Get-WmsDevPackageMetadata $a.jar.path
    if($package.manifestSha256 -ine $a.package.manifestSha256 -or $package.dependenciesSha256 -ine $a.package.dependenciesSha256){throw 'DEV_CURRENT_PACKAGE_CHANGED'}
    return $a
}

function Get-WmsDevCurrentBackendArguments($Plan) {
    @('-Dwms.sql01.scope=BE02-DEV-FONTE01','-Dwms.sql01.observation.disabled=true',
        ('-Dwms.dev.access-mode='+$Plan.access.mode),('-Dwms.sql01.local-origin='+$Plan.localOrigin),
        '-cp',$Plan.jar,(Join-Path $Plan.root 'infra/sql01/Sql01Bootstrap.java'),$Plan.jar,$Plan.config,'25580',
        [string]$Plan.guard.alvo.servidor,[string]$Plan.guard.perfilProtegido.certificateHost,$Plan.truststore,
        $Plan.root,$Plan.run,$Plan.sourceHash,$Plan.jarSha256,$Plan.runId,(Join-Path $Plan.run 'bootstrap-diagnostic.json'))
}

function Read-WmsDevCurrentJdbcProof($Plan,[int]$ProcessId) {
    $path=Join-Path $Plan.run 'jdbc-provider.json'
    $j=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    if($j.id -cne 'QUAL-CONF01-SQL01' -or $j.runId -cne $Plan.runId -or $j.pid -ne $ProcessId -or
        $j.declaredSourceHash -cne $Plan.sourceHash -or $j.declaredJarSha256 -ine $Plan.jarSha256 -or
        $j.approved -ne $true -or $j.provider -cne 'SQLServer' -or $j.database -cne 'WMS_DEV' -or
        $j.login -cne 'WMSDEV' -or $j.user -cne 'WMSDEV' -or $j.metadataStatementsExecuted -ne 1 -or
        $j.connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse -ne $true -or
        $j.privateTrustStoreAndCertificateHostConfigured -ne $true -or
        [DateTimeOffset]::Parse($j.startedUtc) -lt [DateTimeOffset]::Parse($Plan.guard.observadoEm) -or
        [DateTimeOffset]::Parse($j.finishedUtc) -lt [DateTimeOffset]::Parse($j.startedUtc)){throw 'DEV_CURRENT_JDBC_NOT_APPROVED'}
    [pscustomobject]@{path=$path;sha256=(Get-FileHash -LiteralPath $path).Hash.ToLowerInvariant();value=$j}
}

function Assert-WmsDevCurrentLaunchPlan {
    param([string]$Candidato,[string]$CandidateSha256,[string]$GuardReceipt,[string]$GuardSha256,
        [string]$RunDirectory,[int]$Port,[string]$LocalOrigin)
    $root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
    $a=Assert-WmsDevCurrentArtifact $root $Candidato $CandidateSha256
    $run=[IO.Path]::GetFullPath($RunDirectory)
    if($Port -ne 25580 -or $run -ine (Split-Path $Candidato -Parent) -or
        @(Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue).Count){throw 'DEV_CURRENT_LAUNCH_BINDING_REFUSED'}
    . (Join-Path $root 'backend/scripts/dev/start-backend.ps1')
    $g=Get-Content -LiteralPath $GuardReceipt -Raw -Encoding UTF8|ConvertFrom-Json
    $guardHelper=Join-Path $root 'infra/dev02/guarda-wmsdev.ps1'
    $proof=Assert-WmsFrontendDevGuard -GuardReceipt $GuardReceipt -GuardSha256 $GuardSha256 -GuardProcessId $g.pidGuarda -GuardHelperSha256 ((Get-FileHash -LiteralPath $guardHelper).Hash) -RunDirectory $run
    if(@($g.checks).Count -ne 26 -or $g.aberturaTentativas -ne 1 -or $g.aberturaConcluida -ne $true){throw 'DEV_CURRENT_GUARD_INCOMPLETE'}
    . (Join-Path $root 'infra/dev/acesso-dev.ps1')
    $access=Get-WmsDevAccess 25581
    $origin=if($access.mode -ceq 'tunnel'){$access.proxyOrigin}else{$access.origin}
    if($origin -cne $LocalOrigin){throw 'DEV_CURRENT_AUTH_CONTEXT_CHANGED'}
    [pscustomobject]@{root=$root;run=$run;runId=$a.runId;observation=$run;candidatePath=$Candidato;candidateSha256=$CandidateSha256;candidate=$a;
        sourceHash=$a.sourceHash;jar=$a.jar.path;jarSha256=$a.jar.sha256;java=$a.java.path;config=$a.config.path;
        guard=$g;guardPath=$GuardReceipt;guardSha256=$GuardSha256;guardReference=$proof.guard;
        reviewPath=$null;reviewSha256=$null;truststore=$proof.profile.truststore;port=25580;localOrigin=$origin;access=$access;runtimePath=(Join-Path $run 'runtime-prumo.json')}
}

function Assert-WmsDevCurrentRuntime {
    param([Parameter(Mandatory)][string]$RuntimePath,[Parameter(Mandatory)][string]$RuntimeSha256)
    $root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
    . (Join-Path $root 'infra/sql01/launcher-backend.ps1')
    . (Join-Path $root 'infra/dev/reiniciar-instancia.ps1')
    . (Join-Path $root 'infra/dev/acesso-dev.ps1')
    $area=Join-Path $root 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs'
    $path=Read-WmsSql01HashedFile @{path=$RuntimePath;sha256=$RuntimeSha256} $area
    $r=Get-Content -LiteralPath $path -Raw -Encoding UTF8|ConvertFrom-Json
    $run=Join-Path $area $r.runId
    if($path -ine (Join-Path $run 'runtime-prumo.json') -or $r.id -cne 'BE02-DEV-FONTE01' -or
        $r.owner -cne 'WMS - Prumo' -or $r.projectPath -ine $root -or $r.natureza -cne 'D31_DEV02_BACKEND_READINESS' -or
        $r.estado -cne 'READY' -or $r.code -cne 'DEV_CURRENT_READY' -or $r.profile -cne 'sqlserver-dev' -or
        $r.database -cne 'WMS_DEV' -or $r.login -cne 'WMSDEV' -or !$r.bootstrapDisabled -or
        $r.observationEnabled -ne $false -or !$r.jdbcProviderProved -or $r.backend.port -ne 25580 -or
        $r.backend.address -cne '127.0.0.1' -or !$r.backend.listenerOwned){throw 'DEV_CURRENT_RUNTIME_BINDING_REFUSED'}
    $a=Assert-WmsDevCurrentArtifact $root $r.candidate.path $r.candidate.sha256
    if($a.runId -cne $r.runId -or $a.sourceHash -cne $r.sourceHash -or $a.fingerprint.sha256 -cne $r.fingerprint.sha256 -or
        $a.jar.sha256 -ine $r.artifact.sha256 -or $a.config.sha256 -ine $r.config.sha256 -or $a.java.sha256 -ine $r.java.sha256){throw 'DEV_CURRENT_RUNTIME_ARTIFACT_MISMATCH'}
    . (Join-Path $root 'backend/scripts/dev/start-backend.ps1')
    $null=Assert-WmsFrontendDevGuard -GuardReceipt $r.guard.path -GuardSha256 $r.guard.sha256 -GuardProcessId $r.guard.processId -GuardHelperSha256 $r.guard.helperSha256 -RunDirectory $run
    $g=Get-Content -LiteralPath $r.guard.path -Raw -Encoding UTF8|ConvertFrom-Json
    if(@($g.checks).Count -ne 26 -or $g.aberturaTentativas -ne 1 -or !$g.aberturaConcluida){throw 'DEV_CURRENT_RUNTIME_GUARD_REFUSED'}
    $access=Get-WmsDevAccess 25581
    $origin=if($access.mode -ceq 'tunnel'){$access.proxyOrigin}else{$access.origin}
    if($r.authOrigin -cne $access.origin -or $r.proxyOrigin -cne $access.proxyOrigin -or $r.secureCookie -ne $access.secureCookie -or $r.localOrigin -cne $origin){throw 'DEV_CURRENT_RUNTIME_AUTH_CHANGED'}
    $plan=@{root=$root;run=$run;runId=$r.runId;jar=$a.jar.path;config=$a.config.path;sourceHash=$a.sourceHash;jarSha256=$a.jar.sha256;guard=$g;truststore=$g.perfilProtegido.truststore;access=$access;localOrigin=$origin}
    $j=Read-WmsDevCurrentJdbcProof $plan $r.backend.pid
    if($j.sha256 -ine $r.jdbc.sha256 -or $j.path -ine $r.jdbc.path -or
        [DateTimeOffset]::Parse($j.value.startedUtc) -lt [DateTimeOffset]::Parse($r.backend.startUtc) -or
        [DateTimeOffset]::Parse($j.value.finishedUtc) -gt [DateTimeOffset]::Parse($r.observadoUtc)){throw 'DEV_CURRENT_RUNTIME_JDBC_MISMATCH'}
    $p=Get-WmsSql01BackendProcess $r.backend.pid
    $processArguments=@(Split-WmsDevCommandLine $p.CommandLine)
    $expected=@($a.java.path)+@(Get-WmsDevCurrentBackendArguments $plan)
    if($p.StartTime.ToUniversalTime().ToString('o') -cne $r.backend.startUtc -or $p.ExecutablePath -ine $a.java.path -or
        $processArguments.Count -ne $expected.Count){throw 'DEV_CURRENT_RUNTIME_PROCESS_CHANGED'}
    for($i=0;$i -lt $processArguments.Count;$i++){if($processArguments[$i] -cne $expected[$i]){throw 'DEV_CURRENT_RUNTIME_COMMAND_CHANGED'}}
    $listeners=@(Get-WmsDevListeners|Where-Object OwningProcess -eq $p.Id)
    if(!$listeners.Count -or @($listeners|Where-Object {$_.LocalPort -ne 25580 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'DEV_CURRENT_RUNTIME_PORT_CHANGED'}
    $sql=Get-WmsSql01SqlProcess $r.sql.pid
    if($sql.StartTime.ToUniversalTime().ToString('o') -cne $r.sql.startUtc){throw 'DEV_CURRENT_RUNTIME_SQL_PROCESS_CHANGED'}
    $pool=@(Get-NetTCPConnection -State Established -OwningProcess $p.Id -ErrorAction Stop|Where-Object {$_.RemoteAddress -ceq '127.0.0.1' -and $_.RemotePort -eq 1433})
    if(!$pool.Count){throw 'DEV_CURRENT_RUNTIME_POOL_SOCKET_MISSING'}
    . (Join-Path $root 'infra/auth/login-protegido.ps1')
    $sPath=Read-WmsSql01HashedFile $r.authSigningMetadata $run
    $s=Get-Content -LiteralPath $sPath -Raw|ConvertFrom-Json
    $native=Get-WmsLoginMaterialPath
    Assert-WmsLoginPrivatePath (Split-Path $native -Parent);Assert-WmsLoginPrivatePath $native
    if($s.path -ine $native -or $s.encryptedFileSHA256 -ine (Get-FileHash -LiteralPath $native).Hash -or
        !$s.ACLValidated -or $s.issuer -cne 'https://wms.localhost' -or $s.audience -cne 'wms-dev'){throw 'DEV_CURRENT_RUNTIME_SIGNING_CHANGED'}
    [pscustomobject]@{root=$root;run=$run;runtime=$r;candidate=$a;guard=$g;jdbc=$j;runtimeReference=@{path=$path;sha256=$RuntimeSha256};poolSocketCount=$pool.Count}
}

function New-WmsDevCurrentHttpGate {
    param([string]$RuntimePath,[string]$RuntimeSha256,[string]$LauncherPath,[string]$AuthSurfaceOrigin)
    $v=Assert-WmsDevCurrentRuntime $RuntimePath $RuntimeSha256
    $launcherPath=Resolve-WmsSql01Path $LauncherPath $v.run
    if($launcherPath -ine (Join-Path $v.run 'launcher.json')){throw 'DEV_CURRENT_GATE_LAUNCHER_PATH_INVALID'}
    $l=Get-Content -LiteralPath $launcherPath -Raw|ConvertFrom-Json
    if($l.id -cne 'BE02-DEV-FONTE01' -or $l.runId -cne $v.runtime.runId -or
        $l.estado -cne 'PROCESSES_READY_ROUNDTRIP_PENDING' -or $l.exitCode -ne 0 -or !$l.frontend.listenerOwned -or
        $l.backend.backend.pid -ne $v.runtime.backend.pid -or $l.candidate.sha256 -ine $v.runtime.candidate.sha256){throw 'DEV_CURRENT_GATE_LAUNCHER_NOT_READY'}
    $fe=Get-CimInstance -ClassName Win32_Process -Filter ('ProcessId='+$l.frontend.pid) -ErrorAction Stop
    $start=[DateTimeOffset]::Parse($l.frontend.startUtc).UtcDateTime.Ticks-$fe.CreationDate.ToUniversalTime().Ticks
    $processArguments=@(Split-WmsDevCommandLine $fe.CommandLine)
    $expected=@($fe.ExecutablePath,(Join-Path $v.root 'frontend/node_modules/vite/bin/vite.js'),'--config','vite.dev.config.ts')
    if($fe.Name -ine 'node.exe' -or $start -lt 0 -or $start -gt 9 -or $processArguments.Count -ne $expected.Count){throw 'DEV_CURRENT_GATE_FRONTEND_CHANGED'}
    for($i=0;$i -lt $processArguments.Count;$i++){if($processArguments[$i] -cne $expected[$i]){throw 'DEV_CURRENT_GATE_FRONTEND_COMMAND_CHANGED'}}
    $listeners=@(Get-WmsDevListeners|Where-Object OwningProcess -eq $fe.ProcessId)
    if(!$listeners.Count -or @($listeners|Where-Object {$_.LocalPort -ne 25581 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'DEV_CURRENT_GATE_FRONTEND_PORT_CHANGED'}
    $authOrigin=$null
    if($AuthSurfaceOrigin){
        $uri=$null
        if(![Uri]::TryCreate($AuthSurfaceOrigin,[UriKind]::Absolute,[ref]$uri) -or $uri.Scheme -cne 'https' -or
            $uri.UserInfo -or $uri.Query -or $uri.Fragment -or $uri.AbsolutePath -cne '/'){throw 'DEV_CURRENT_GATE_AUTH_SURFACE_INVALID'}
        $authOrigin=$uri.GetLeftPart([UriPartial]::Authority)
    }
    $gate=[ordered]@{id='BE02-DEV-FONTE01';runId=$v.runtime.runId;issuedUtc=[DateTime]::UtcNow.ToString('o');allowed=$true;owner='WMS - Prumo';
        targets=@{backend='http://127.0.0.1:25580';frontend='http://127.0.0.1:25581'};localOrigin=$v.runtime.localOrigin;
        runtime=$v.runtimeReference;launcher=@{path=$launcherPath;sha256=(Get-FileHash -LiteralPath $launcherPath).Hash};
        candidate=$v.runtime.candidate;fingerprint=$v.runtime.fingerprint;sourceHash=$v.runtime.sourceHash;jarSha256=$v.runtime.artifact.sha256;
        readiness=@{path=(Join-Path $v.run 'backend-readiness.json');sha256=(Get-FileHash -LiteralPath (Join-Path $v.run 'backend-readiness.json')).Hash};
        guard=$v.runtime.guard;jdbc=$v.runtime.jdbc;ownership=$l.ownership;authSigningMetadata=$v.runtime.authSigningMetadata;
        authSurfaceOrigin=$authOrigin;authSurfaceOriginMustBeCurrentMetadataPinnedByFarol=$true;businessContext=@{clienteId=67;armazemId=33;perfil='GESTOR'};
        furtherSQLGuardAllowed=$false;transportRetryAllowed=$false;normalBATExternalReviewRequired=$false;measurementOnly=$true}
    $path=Join-Path $v.run 'http-gate.json'
    Write-WmsDevCreateOnlyJson $path $gate
    [pscustomobject]@{path=$path;sha256=(Get-FileHash -LiteralPath $path).Hash;value=$gate}
}

function Get-WmsBackendSourceHash([string]$Repository) {
    $root = [IO.Path]::GetFullPath($Repository).TrimEnd('\', '/')
    $files = @(Get-Item -LiteralPath (Join-Path $root 'backend/pom.xml')) +
        @(Get-ChildItem -LiteralPath (Join-Path $root 'backend/src/main') -File -Recurse)
    $lines = @($files | ForEach-Object {
        $relative = $_.FullName.Substring($root.Length + 1).Replace('\', '/')
        $relative + '=' + (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
    } | Sort-Object)
    $sha = [Security.Cryptography.SHA256]::Create()
    try {
        return ([BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes(
            ($lines -join "`n"))))).Replace('-', '').ToLowerInvariant()
    } finally { $sha.Dispose() }
}

function Assert-WmsDevApplicationArtifact([string]$Repository, [string]$ReceiptPath) {
    $artifact = Get-Content -LiteralPath $ReceiptPath -Raw -Encoding UTF8 | ConvertFrom-Json
    if ($artifact.PSObject.Properties.Name -notcontains 'sourceHash' -or
        $artifact.sourceHash -cne (Get-WmsBackendSourceHash $Repository)) {
        throw 'DEV02_ARTIFACT_SOURCE_STALE'
    }
    $root = [IO.Path]::GetFullPath($Repository).TrimEnd('\', '/') + [IO.Path]::DirectorySeparatorChar
    $jar = [IO.Path]::GetFullPath($artifact.jar.path)
    if (-not $jar.StartsWith($root, [StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $jar -PathType Leaf) -or
        (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ine $artifact.jar.sha256) {
        throw 'DEV02_ARTIFACT_JAR_INVALID'
    }
    Add-Type -AssemblyName System.IO.Compression
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        foreach ($entry in @(
            'controllers/VisaoOperacaoController.class', 'services/VisaoOperacaoService.class',
            'controllers/DashboardController.class', 'config/LoginConfig.class',
            'config/CadastrosSegurancaConfig.class')) {
            if ($null -eq $zip.GetEntry('BOOT-INF/classes/br/com/rodogarcia/wms/' + $entry)) {
                throw 'DEV02_ARTIFACT_API_MISSING'
            }
        }
    } finally { $zip.Dispose() }
    return $artifact
}
