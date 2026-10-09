# PROD-LAUNCH01. Dot-source sem efeitos; start explicito apenas apos preflights.
. (Join-Path $PSScriptRoot 'identity-prod.ps1')

function Assert-WmsProductionOwnedDirectory {
    param([string]$Directory)
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $base = [IO.Path]::GetFullPath((Join-Path $root 'orchestracao/.runtime/launcher-producao'))
    $directoryPath = [IO.Path]::GetFullPath($Directory)
    if (-not $directoryPath.StartsWith($base + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $directoryPath -PathType Container)) { throw 'PROD_RUN_DIRECTORY_INVALID' }
    $walk = $directoryPath
    while ($walk -and $walk.Length -ge $base.Length) {
        if ((Get-Item -LiteralPath $walk -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) { throw 'PROD_RUN_DIRECTORY_REPARSE_REFUSED' }
        $walk = [IO.Path]::GetDirectoryName($walk)
    }
}

function Assert-WmsProductionGuard {
    param([string]$GuardReceipt, [string]$GuardSha256, [int]$GuardProcessId,
        [string]$GuardHelperSha256, [string]$RunDirectory)
    if ($GuardSha256 -notmatch '^[a-fA-F0-9]{64}$' -or -not (Test-Path -LiteralPath $GuardReceipt -PathType Leaf) -or
        (Get-FileHash -LiteralPath $GuardReceipt -Algorithm SHA256).Hash -ine $GuardSha256) { throw 'PROD_GUARD_RECEIPT_OR_HASH_INVALID' }
    try { $report = [IO.File]::ReadAllText($GuardReceipt) | ConvertFrom-Json } catch { throw 'PROD_GUARD_SCHEMA_INVALID' }
    if ($report.guardaRealAprovada -isnot [bool]) { throw 'PROD_GUARD_SCHEMA_INVALID' }
    if (-not $report.guardaRealAprovada -or $report.estado -cne 'GUARDA_ATUAL_APROVADA') { throw 'PROD_GUARD_BLOCKED' }
    if ($report.demanda -cne 'PROD-LAUNCH01' -or $report.operacaoSQLImplementada -ne $true -or
        $GuardProcessId -le 0 -or $report.pidGuarda -ne $GuardProcessId) { throw 'PROD_GUARD_PROCESS_OR_PURPOSE_INVALID' }
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $helper = Join-Path $root 'infra/prod/guarda-wmsprod.ps1'
    if ($report.auxiliar.arquivo -cne 'infra/prod/guarda-wmsprod.ps1' -or
        $GuardHelperSha256 -notmatch '^[a-fA-F0-9]{64}$' -or $report.auxiliar.sha256 -ine $GuardHelperSha256 -or
        (Get-FileHash -LiteralPath $helper -Algorithm SHA256).Hash -ine $GuardHelperSha256) { throw 'PROD_GUARD_HELPER_INVALID' }
    if ([IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($GuardReceipt)) -ine [IO.Path]::GetFullPath($RunDirectory)) {
        throw 'PROD_GUARD_WRONG_RUN_DIRECTORY'
    }
    $begin = [DateTimeOffset]::MinValue; $finish = [DateTimeOffset]::MinValue
    if (-not [DateTimeOffset]::TryParse([string]$report.inicioUtc, [ref]$begin) -or
        -not [DateTimeOffset]::TryParse([string]$report.observadoEm, [ref]$finish) -or
        $finish -lt $begin -or $finish -gt [DateTimeOffset]::UtcNow.AddSeconds(15) -or
        $begin -lt [DateTimeOffset]::UtcNow.AddMinutes(-5)) { throw 'PROD_GUARD_TIME_INVALID_OR_STALE' }
    if ($report.aberturaTentativas -ne 1 -or $report.aberturaConcluida -ne $true -or
        $report.identidadeConfirmada -ne $true -or $report.alvo.banco -cne 'WMS_PROD' -or
        $report.alvo.login -cne 'WMSPROD' -or $report.alvo.usuario -cne 'WMSPROD' -or
        $report.alvo.estado -cne 'ONLINE' -or [string]::IsNullOrWhiteSpace([string]$report.alvo.servidor)) {
        throw 'PROD_GUARD_IDENTITY_INVALID'
    }
    if ($report.tls.encrypt -cne 'Mandatory' -or $report.tls.trustServerCertificate -isnot [bool] -or
        $report.tls.trustServerCertificate -ne $false -or $report.tls.handshakeConcluido -ne $true -or
        $report.tls.confirmacaoAtual -ne $true -or $report.tls.certificateSha256 -notmatch '^[a-fA-F0-9]{64}$' -or
        $report.tls.truststoreSha256 -notmatch '^[a-fA-F0-9]{64}$') { throw 'PROD_GUARD_TLS_INVALID' }
    if ($report.tls.proveniencia.reciboPID -le 0 -or [string]::IsNullOrWhiteSpace([string]$report.tls.proveniencia.reciboInicioUtc) -or
        $report.tls.proveniencia.processoLocalVerificadoAntes -ne $true -or $report.tls.proveniencia.processoLocalVerificadoDepois -ne $true -or
        $report.tls.proveniencia.baselineHistoricaTLSUsadaComoPinAtual -ne $false) { throw 'PROD_GUARD_TLS_PROCESS_PROVENANCE_MISSING' }
    foreach ($name in @('server', 'database', 'roles', 'serverRoles', 'ownership', 'schema', 'explicit', 'objects', 'columns')) {
        if ($null -eq $report.direitos.$name) { throw 'PROD_GUARD_RIGHTS_INCOMPLETE' }
    }
    foreach ($name in @('tabelas', 'colunas', 'checksInvalidos', 'fksInvalidas', 'indicesDesabilitados')) {
        if ($null -eq $report.catalogo.$name) { throw 'PROD_GUARD_CATALOG_INCOMPLETE' }
    }
    if (@($report.historico).Count -eq 0 -or @($report.historico | Where-Object { $_.success -ne $true }).Count -or
        $report.perfilProtegido.host -cne '127.0.0.1' -or $report.perfilProtegido.port -ne 1433 -or
        $report.perfilProtegido.database -cne 'WMS_PROD' -or $report.perfilProtegido.login -cne 'WMSPROD' -or
        $report.perfilProtegido.encrypt -ne $true -or $report.perfilProtegido.trustServerCertificate -ne $false) {
        throw 'PROD_GUARD_HISTORY_OR_PROFILE_INVALID'
    }
    if ($null -eq $report.checks -or @($report.checks).Count -eq 0 -or
        @($report.checks | Where-Object { $_.passou -isnot [bool] -or $_.passou -ne $true }).Count) {
        throw 'PROD_GUARD_CHECKS_INCOMPLETE'
    }
    if (@($report.direitos.roles | Where-Object { $_.name -in @('db_owner','db_ddladmin','db_securityadmin','db_accessadmin') -or $_.role -in @('db_owner','db_ddladmin','db_securityadmin','db_accessadmin') }).Count -or
        @($report.direitos.serverRoles).Count -or @($report.direitos.ownership).Count -or
        @($report.catalogo.checksInvalidos).Count -or @($report.catalogo.fksInvalidas).Count -or @($report.catalogo.indicesDesabilitados).Count) {
        throw 'PROD_GUARD_PRIVILEGES_OR_CATALOG_REFUSED'
    }
    [pscustomobject]@{ profile = $report.perfilProtegido; server = $report.alvo.servidor; tls = $report.tls
        guard = @{ receiptPath = [IO.Path]::GetFullPath($GuardReceipt); sha256 = $GuardSha256.ToLowerInvariant()
            processId = $GuardProcessId; helperPath = $helper; helperSha256 = $GuardHelperSha256.ToLowerInvariant()
            finishedUtc = [string]$report.observadoEm; projectPath = $root } }
}

function Assert-WmsProductionArtifact {
    param([object]$Artifact)
    if ($null -eq $Artifact) { throw 'PROD_ARTIFACT_MISSING' }
    foreach ($part in @('jar', 'config', 'java', 'sourceManifest')) {
        $entry = $Artifact.$part
        if ($null -eq $entry -or $entry.sha256 -notmatch '^[a-fA-F0-9]{64}$' -or
            -not (Test-Path -LiteralPath $entry.path -PathType Leaf) -or
            ((Get-Item -LiteralPath $entry.path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) -or
            (Get-FileHash -LiteralPath $entry.path -Algorithm SHA256).Hash -ine $entry.sha256) { throw 'PROD_ARTIFACT_OR_HASH_INVALID' }
    }
    if ($Artifact.sourceSha256 -ine $Artifact.sourceManifest.sha256) { throw 'PROD_SOURCE_HASH_INVALID' }
    $candidate = [IO.Path]::GetFullPath($Artifact.CandidateDirectory)
    Assert-WmsProductionOwnedDirectory $candidate
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $base = [IO.Path]::GetFullPath((Join-Path $root 'orchestracao/.runtime/launcher-producao'))
    if (-not $candidate.StartsWith($base + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        [IO.Path]::GetFullPath($Artifact.jar.path) -ine (Join-Path $candidate 'target/wms-backend-0.0.1-SNAPSHOT.jar') -or
        [IO.Path]::GetFullPath($Artifact.config.path) -ine (Join-Path $candidate 'src/main/resources/application-frontend-prod.properties') -or
        [IO.Path]::GetFullPath($Artifact.sourceManifest.path) -ine (Join-Path $candidate 'source-manifest.json')) {
        throw 'PROD_ARTIFACT_NOT_OWN_CANDIDATE'
    }
    $manifest = [IO.File]::ReadAllText($Artifact.sourceManifest.path) | ConvertFrom-Json
    if ($manifest.natureza -cne 'PROD_LAUNCH01_BACKEND_SOURCE_CAPTURE' -or $manifest.root -ine $root -or @($manifest.files).Count -eq 0) {
        throw 'PROD_SOURCE_MANIFEST_INVALID'
    }
    foreach ($file in $manifest.files) {
        if ($file.path -match '(^|/)\.\.(/|$)' -or [IO.Path]::IsPathRooted($file.path)) { throw 'PROD_SOURCE_MANIFEST_INVALID' }
        $copied = Join-Path $candidate $file.path
        if ((Get-FileHash -LiteralPath $copied -Algorithm SHA256).Hash -ine $file.sha256) { throw 'PROD_CANDIDATE_SOURCE_CHANGED' }
    }
    $release = Join-Path ([IO.Path]::GetDirectoryName([IO.Path]::GetDirectoryName($Artifact.java.path))) 'release'
    if ([IO.Path]::GetFileName($Artifact.java.path) -ine 'java.exe' -or
        -not ([IO.File]::ReadAllText($release) -match '(?m)^JAVA_VERSION="21[.\-"]')) { throw 'PROD_JDK21_REQUIRED' }
}

function Stop-WmsProductionBackend {
    param([Parameter(Mandatory = $true)][object]$Handle)
    if (-not (Get-Variable WmsFrontendProdHandles -Scope Script -ErrorAction SilentlyContinue) -or
        -not $script:WmsFrontendProdHandles.ContainsKey([string]$Handle.RunId) -or
        -not [object]::ReferenceEquals($script:WmsFrontendProdHandles[[string]$Handle.RunId], $Handle.Process)) {
        throw 'PROD_FOREIGN_PROCESS_HANDLE_REFUSED'
    }
    $process = $Handle.Process
    if (-not $process.HasExited) {
        $process.Kill(); $null = $process.WaitForExit(5000)
        if (-not $process.HasExited) { throw 'PROD_OWN_PROCESS_STOP_TIMEOUT' }
    }
    $null = $script:WmsFrontendProdHandles.Remove([string]$Handle.RunId)
    $process.Dispose()
}

function New-WmsProductionBackendStartInfo {
    param([int]$Port, [object]$Artifact)
    foreach ($path in @($Artifact.jar.path,$Artifact.config.path)) { if ($path -match '["\r\n]') { throw 'PROD_ARGUMENT_PATH_INVALID' } }
    $info = New-Object Diagnostics.ProcessStartInfo
    $info.FileName = $Artifact.java.path; $info.WorkingDirectory = $Artifact.CandidateDirectory
    $info.UseShellExecute = $false; $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true; $info.EnvironmentVariables.Clear()
    foreach ($name in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE','LOCALAPPDATA','APPDATA')) {
        $value = [Environment]::GetEnvironmentVariable($name,'Process'); if ($value) { $info.EnvironmentVariables[$name] = $value }
    }
    $configUri = ([Uri][IO.Path]::GetFullPath($Artifact.config.path)).AbsoluteUri
    $info.Arguments = '-jar "' + $Artifact.jar.path + '" --spring.profiles.active=sqlserver-prod ' +
        '--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-prod.properties ' +
        '--spring.config.additional-location="' + $configUri + '" --server.address=127.0.0.1 --server.port=' + $Port +
        ' --server.forward-headers-strategy=none --spring.autoconfigure.exclude= --wms.cadastros.enabled=true ' +
        '--spring.jpa.hibernate.ddl-auto=validate --spring.jpa.generate-ddl=false --spring.sql.init.mode=never ' +
        '--spring.flyway.enabled=false --spring.liquibase.enabled=false --wms.database.name=WMS_PROD --wms.database.user=WMSPROD ' +
        '--wms.auth.enabled=true --wms.auth.origin=https://wms.rodogarcia.com.br --wms.auth.secure-cookie=true --wms.auth.proxy-origin= --wms.auth.bootstrap-hash='
    $info
}

function Start-WmsProductionBackend {
    [CmdletBinding()]
    param([Parameter(Mandatory = $true)][ValidateRange(1024,65535)][int]$Port,
        [Parameter(Mandatory = $true)][string]$RunDirectory,
        [Parameter(Mandatory = $true)][string]$GuardReceipt,
        [Parameter(Mandatory = $true)][string]$GuardSha256,
        [Parameter(Mandatory = $true)][int]$GuardProcessId,
        [Parameter(Mandatory = $true)][string]$GuardHelperSha256,
        [Parameter(Mandatory = $true)][object]$Artifact,
        [Parameter(Mandatory = $true)][object]$Identity,
        [Management.Automation.PSCredential]$DatabaseCredential)
    $ErrorActionPreference = 'Stop'
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $base = [IO.Path]::GetFullPath((Join-Path $root 'orchestracao/.runtime/launcher-producao'))
    $directory = [IO.Path]::GetFullPath($RunDirectory)
    Assert-WmsProductionOwnedDirectory $directory
    if (-not $directory.StartsWith($base + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        -not (Test-Path -LiteralPath $directory -PathType Container) -or
        ((Get-Item -LiteralPath $directory -Force).Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'PROD_RUN_DIRECTORY_INVALID' }
    $runId = [IO.Path]::GetFileName($directory)
    $receiptPath = Join-Path $directory 'backend-readiness.json'
    $stream = [IO.File]::Open($receiptPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    $receipt = [ordered]@{ natureza = 'PROD_LAUNCH01_BACKEND_READINESS'; runId = $runId; observadoUtc = $null
        estado = 'BLOCKED'; codigo = $null; etapa = 'PREFLIGHT'; profile = 'sqlserver-prod'; database = 'WMS_PROD'; login = 'WMSPROD'
        backend = $null; guard = $null; artifact = $null; config = $null; java = $null; source = $null; publicAuth = $null
        purpose = 'SUBIDA_OPERACIONAL_NAO_TESTE_BANCO'; authenticatedBusinessRead = $false; sqlProbeByHelper = $false
        bootstrapHashPassed = $false; rawBootLogsSaved = $false; existingProcessesAltered = $false }
    $process = $null; $started = $false; $handle = $null; $info = $null; $client = $null; $handler = $null; $material = $null; $credentialConsumed = $false; $published = $false
    try {
        Assert-WmsProductionIdentity $Identity
        $receipt.etapa = 'GUARD_RECEIPT'
        $guard = Assert-WmsProductionGuard $GuardReceipt $GuardSha256 $GuardProcessId $GuardHelperSha256 $directory
        $receipt.guard = $guard.guard
        $receipt.etapa = 'ARTIFACT_AND_PORT'
        Assert-WmsProductionArtifact $Artifact
        if (@(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue).Count) { throw 'PROD_PORT_ALREADY_IN_USE' }
        $receipt.artifact = $Artifact.jar; $receipt.config = $Artifact.config; $receipt.java = $Artifact.java; $receipt.source = $Artifact.sourceManifest
        $receipt.etapa = 'PROTECTED_CHANNELS'
        # SQL binding pertence a Prumo/root. Nao adivinhar caminho/loader ou importar DEV.
        if ($null -eq $DatabaseCredential -or $DatabaseCredential.UserName -cne 'WMSPROD') { throw 'PROD_PROTECTED_APPLICATION_CHANNEL_MISSING' }
        if ((Get-FileHash -LiteralPath $Identity.metadataPath -Algorithm SHA256).Hash -ine $Identity.metadataSha256) { throw 'PROD_AUTH_METADATA_CHANGED' }
        foreach ($path in @([IO.Path]::GetDirectoryName($Identity.materialPath), $Identity.materialPath, $Identity.metadataPath)) { Assert-WmsProductionPrivatePath $path }
        if ((Get-FileHash -LiteralPath $guard.profile.truststore -Algorithm SHA256).Hash -ine $guard.tls.truststoreSha256) { throw 'PROD_TRUSTSTORE_CHANGED_SINCE_GUARD' }
        $material = Import-Clixml -LiteralPath $Identity.materialPath
        if ($material -isnot [Security.SecureString]) { throw 'PROD_AUTH_PROTECTED_MATERIAL_INVALID' }
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($material)
        try { $auth = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) | ConvertFrom-Json }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
        if ($auth.schema -ne 1 -or $auth.environment -cne 'PROD' -or $auth.database -cne 'WMS_PROD' -or
            $auth.issuer -cne $Identity.issuer -or $auth.audience -cne $Identity.audience -or
            [string]::IsNullOrWhiteSpace([string]$auth.privateKey)) { throw 'PROD_AUTH_PROTECTED_PURPOSE_INVALID' }
        $info = New-WmsProductionBackendStartInfo $Port $Artifact
        $variables = @{
            WMS_PROD_PORT = [string]$Port; WMS_PROD_DB_HOST = $guard.profile.host; WMS_PROD_DB_PORT = [string]$guard.profile.port
            WMS_PROD_DB_CONFIRMED_TARGET = ('{0}:{1}/WMS_PROD' -f $guard.profile.host,$guard.profile.port); WMS_PROD_DB_CONFIRMED_SERVER = $guard.server
            WMS_PROD_DB_CERTIFICATE_HOST = $guard.profile.certificateHost; WMS_PROD_DB_TRUST_STORE = $guard.profile.truststore
            WMS_PROD_DB_TRUST_STORE_PASSWORD = 'projetos-public-cert'; WMS_PROD_AUTH_ISSUER = $Identity.issuer
            WMS_PROD_AUTH_JWK_SET_URI = $Identity.jwkSetUri; WMS_PROD_AUTH_AUDIENCE = $Identity.audience; WMS_PROD_AUTH_PRIVATE_KEY = $auth.privateKey }
        foreach ($entry in $variables.GetEnumerator()) { $info.EnvironmentVariables[$entry.Key] = [string]$entry.Value }
        $auth = $null; $material.Dispose(); $material = $null
        $credentialConsumed = $true
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($DatabaseCredential.Password)
        try { $info.EnvironmentVariables['WMS_PROD_DB_PASSWORD'] = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
        $receipt.etapa = 'BOOT'
        $process = New-Object Diagnostics.Process; $process.StartInfo = $info
        if (-not $process.Start()) { throw 'PROD_PROCESS_START_REFUSED' }
        $started = $true
        if (-not (Get-Variable WmsFrontendProdHandles -Scope Script -ErrorAction SilentlyContinue)) { $script:WmsFrontendProdHandles = @{} }
        $script:WmsFrontendProdHandles[$runId] = $process
        $handle = [pscustomobject]@{ RunId = $runId; Process = $process; ReceiptPath = $receiptPath; ReceiptSha256 = $null }
        $process.BeginOutputReadLine(); $process.BeginErrorReadLine(); $info.EnvironmentVariables.Clear()
        $receipt.backend = @{ pid = $process.Id; startUtc = $process.StartTime.ToUniversalTime().ToString('o'); port = $Port; url = "http://127.0.0.1:$Port"; listenerOwned = $false }
        Add-Type -AssemblyName System.Net.Http
        $handler = New-Object Net.Http.HttpClientHandler; $handler.UseProxy = $false; $handler.AllowAutoRedirect = $false
        $client = New-Object Net.Http.HttpClient($handler); $client.Timeout = [TimeSpan]::FromSeconds(2)
        $until = [DateTime]::UtcNow.AddSeconds(90); $ready = $false
        do {
            if ($process.HasExited) { throw 'PROD_BACKEND_EXITED_BEFORE_READY' }
            $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
            if ($listeners.Count) {
                if (@($listeners | Where-Object { $_.OwningProcess -ne $process.Id -or $_.LocalAddress -cne '127.0.0.1' }).Count) { throw 'PROD_LISTENER_OWNER_MISMATCH' }
                try {
                    $statusResponse = $client.GetAsync("http://127.0.0.1:$Port/api/v1/status").GetAwaiter().GetResult()
                    try { $status = $statusResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json; $statusCode = [int]$statusResponse.StatusCode } finally { $statusResponse.Dispose() }
                    if ($statusCode -eq 200 -and $status.aplicacao -ceq 'wms-rodogarcia' -and $status.status -ceq 'DISPONIVEL') {
                        $jwkResponse = $client.GetAsync("http://127.0.0.1:$Port/api/auth/jwks").GetAwaiter().GetResult()
                        try { $jwks = $jwkResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json; $jwkCode = [int]$jwkResponse.StatusCode } finally { $jwkResponse.Dispose() }
                        if ($jwkCode -eq 200 -and @($jwks.keys | Where-Object { $_.kty -ceq 'RSA' -and $_.kid }).Count) {
                            $receipt.publicAuth = @{ jwksHttp = 200; native = $true; origin = $Identity.origin; issuer = $Identity.issuer; audience = $Identity.audience
                                secureCookie = $true; proxyOrigin = ''; httpsLoginVerified = $false; publicHttpsConfigured = $false }
                            $receipt.status = @{ http = 200; status = 'DISPONIVEL'; aplicacao = 'wms-rodogarcia' }
                            $receipt.backend.listenerOwned = $true; $ready = $true
                        }
                    }
                } catch { if ($_.Exception.Message -match '^PROD_') { throw } }
            }
            if (-not $ready) { Start-Sleep -Milliseconds 250 }
        } while (-not $ready -and [DateTime]::UtcNow -lt $until)
        if (-not $ready) { throw 'PROD_BACKEND_READINESS_TIMEOUT' }
        $receipt.estado = 'READY_PUBLIC'; $receipt.codigo = 'PROD_OWN_BACKEND_PUBLIC_READY_NOT_BUSINESS_ACCEPTANCE'
    } catch {
        $code = [string]$_.Exception.Message
        $receipt.codigo = if ($code -match '^PROD_[A-Z0-9_]+$') { $code } else { 'PROD_START_FAILED_SANITIZED' }
        throw $receipt.codigo
    } finally {
        try {
            if ($info) { $info.EnvironmentVariables.Clear() }
            if ($material) { $material.Dispose() }; $auth = $null
            if ($credentialConsumed) { $DatabaseCredential.Password.Dispose() }
            if ($client) { $client.Dispose() }; if ($handler) { $handler.Dispose() }
            $receipt.observadoUtc = [DateTime]::UtcNow.ToString('o')
            $bytes = (New-Object Text.UTF8Encoding($false)).GetBytes(($receipt | ConvertTo-Json -Depth 8))
            $stream.Write($bytes,0,$bytes.Length)
            $stream.Dispose(); $stream = $null
            if ($receipt.estado -ceq 'READY_PUBLIC') {
                $handle.ReceiptSha256 = (Get-FileHash -LiteralPath $receiptPath -Algorithm SHA256).Hash.ToLowerInvariant()
                $published = $true
            }
        } finally {
            try { if ($stream) { $stream.Dispose() } } finally {
                # Falha de leitura/recibo nunca deixa o processo proprio sem cleanup.
                if (-not $published -and $process) {
                    if ($started) {
                        if ($handle) { Stop-WmsProductionBackend -Handle $handle }
                        else {
                            # Referencia criada neste Start; cobre falha antes de registrar wrapper.
                            if (-not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000) }
                            if ($process.HasExited) { $process.Dispose() }
                            else { throw 'PROD_OWN_PROCESS_STOP_TIMEOUT' }
                        }
                    } else { $process.Dispose() }
                }
            }
        }
    }
    $handle
}
