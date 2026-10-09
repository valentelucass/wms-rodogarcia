# D31-DEV02. Biblioteca: dot-source declara funcoes; nao inicia processo/SQL/DPAPI.
function Assert-WmsFrontendDevIdentity {
    param([object]$Identity)
    if ($null -eq $Identity) { throw 'DEV02_AUTH_SOURCE_MISSING' }
    foreach ($name in @('issuer', 'jwkSetUri')) {
        $value = [string]$Identity.$name
        $uri = $null
        if (-not [Uri]::TryCreate($value, [UriKind]::Absolute, [ref]$uri) -or
            $uri.Scheme -cne 'https' -or $uri.UserInfo -or $uri.Query -or $uri.Fragment) {
            throw 'DEV02_AUTH_SOURCE_MISSING_OR_INVALID'
        }
    }
    if ([string]::IsNullOrWhiteSpace([string]$Identity.audience) -or
        ([string]$Identity.audience).Length -gt 200 -or
        ([string]$Identity.audience) -match '[\r\n]') { throw 'DEV02_AUTH_AUDIENCE_MISSING_OR_INVALID' }
}

function Assert-WmsFrontendDevGuard {
    param([string]$GuardReceipt, [string]$GuardSha256, [int]$GuardProcessId,
        [string]$GuardHelperSha256, [string]$RunDirectory)
    if ($GuardSha256 -notmatch '^[A-Fa-f0-9]{64}$' -or
        -not (Test-Path -LiteralPath $GuardReceipt -PathType Leaf) -or
        (Get-FileHash -LiteralPath $GuardReceipt -Algorithm SHA256).Hash -ine $GuardSha256) {
        throw 'DEV02_GUARD_RECEIPT_OR_HASH_INVALID'
    }
    $report = [IO.File]::ReadAllText([IO.Path]::GetFullPath($GuardReceipt)) | ConvertFrom-Json
    if ($report.PSObject.Properties.Name -notcontains 'guardaRealAprovada' -or
        $report.guardaRealAprovada -isnot [bool]) { throw 'DEV02_GUARD_SCHEMA_INVALID' }
    if (-not $report.guardaRealAprovada -or $report.estado -cne 'GUARDA_ATUAL_APROVADA') {
        throw 'DEV02_GUARD_BLOCKED'
    }
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $helper = Join-Path $root 'infra/dev02/guarda-wmsdev.ps1'
    if ($RunDirectory -and [IO.Path]::GetDirectoryName([IO.Path]::GetFullPath($GuardReceipt)) -ine [IO.Path]::GetFullPath($RunDirectory)) {
        throw 'DEV02_GUARD_WRONG_RUN_DIRECTORY'
    }
    if ($GuardProcessId -le 0 -or $report.pidGuarda -ne $GuardProcessId) { throw 'DEV02_GUARD_PROCESS_MISMATCH' }
    if ($report.auxiliar.arquivo -cne 'infra/dev02/guarda-wmsdev.ps1' -or
        $GuardHelperSha256 -notmatch '^[A-Fa-f0-9]{64}$' -or
        $report.auxiliar.sha256 -ine $GuardHelperSha256 -or
        (Get-FileHash -LiteralPath $helper -Algorithm SHA256).Hash -ine $GuardHelperSha256) {
        throw 'DEV02_GUARD_HELPER_HASH_MISMATCH'
    }
    # O JSON original usa observadoEm; nao renomear/reescrever o recibo Prumo.
    $begin = [DateTimeOffset]::MinValue; $finish = [DateTimeOffset]::MinValue
    if (-not [DateTimeOffset]::TryParse([string]$report.inicioUtc, [ref]$begin) -or
        -not [DateTimeOffset]::TryParse([string]$report.observadoEm, [ref]$finish) -or
        $finish -lt $begin -or $finish -gt [DateTimeOffset]::UtcNow.AddSeconds(15) -or
        $begin -lt [DateTimeOffset]::UtcNow.AddMinutes(-5)) { throw 'DEV02_GUARD_TIME_INVALID_OR_STALE' }
    if ($report.aberturaTentativas -ne 1 -or $report.aberturaConcluida -ne $true -or
        $report.identidadeConfirmada -ne $true -or $report.alvo.banco -cne 'WMS_DEV' -or
        $report.alvo.login -cne 'WMSDEV' -or $report.alvo.usuario -cne 'WMSDEV' -or
        $report.alvo.estado -cne 'ONLINE' -or [string]::IsNullOrWhiteSpace([string]$report.alvo.servidor)) {
        throw 'DEV02_GUARD_IDENTITY_INVALID'
    }
    if ($report.tls.encrypt -cne 'Mandatory' -or $report.tls.trustServerCertificate -ne $false -or
        $report.tls.handshakeConcluido -ne $true -or $report.tls.confirmacaoAtual -ne $true -or
        $report.tls.certificateSha256 -notmatch '^[A-Fa-f0-9]{64}$' -or
        $report.tls.truststoreSha256 -notmatch '^[A-Fa-f0-9]{64}$') { throw 'DEV02_GUARD_TLS_INVALID' }
    foreach ($name in @('server', 'database', 'roles', 'serverRoles', 'ownership', 'schema', 'explicit', 'objects', 'columns')) {
        if ($null -eq $report.direitos -or $null -eq $report.direitos.$name) { throw 'DEV02_GUARD_RIGHTS_INCOMPLETE' }
    }
    foreach ($name in @('tabelas', 'colunas', 'checksInvalidos', 'fksInvalidas', 'indicesDesabilitados')) {
        if ($null -eq $report.catalogo -or $null -eq $report.catalogo.$name) { throw 'DEV02_GUARD_CATALOG_INCOMPLETE' }
    }
    if (@($report.historico).Count -eq 0 -or @($report.historico | Where-Object { $_.success -ne $true }).Count -gt 0 -or
        $report.perfilProtegido.host -cne '127.0.0.1' -or $report.perfilProtegido.port -ne 1433 -or
        $report.perfilProtegido.database -cne 'WMS_DEV' -or $report.perfilProtegido.login -cne 'WMSDEV' -or
        $report.perfilProtegido.encrypt -ne $true -or $report.perfilProtegido.trustServerCertificate -ne $false) {
        throw 'DEV02_GUARD_HISTORY_OR_PROFILE_INVALID'
    }
    if ($null -eq $report.checks -or @($report.checks).Count -eq 0 -or
        @($report.checks | Where-Object { $_.passou -ne $true }).Count) { throw 'DEV02_GUARD_CHECKS_INCOMPLETE' }
    [pscustomobject]@{ profile = $report.perfilProtegido; server = $report.alvo.servidor; tls = $report.tls
        guard = @{ receiptPath = [IO.Path]::GetFullPath($GuardReceipt); sha256 = $GuardSha256.ToLowerInvariant()
            processId = $GuardProcessId
            # Identidade literal da cadeia: nao reserializar $finish (Z viraria +00:00).
            finishedUtc = [string]$report.observadoEm
            helperPath = $helper
            helperSha256 = $GuardHelperSha256.ToLowerInvariant(); projectPath = $root } }
}

function Stop-WmsFrontendDevBackend {
    param([Parameter(Mandatory = $true)][object]$Handle)
    if (-not (Get-Variable WmsFrontendDevHandles -Scope Script -ErrorAction SilentlyContinue) -or
        -not $script:WmsFrontendDevHandles.ContainsKey([string]$Handle.RunId) -or
        -not [object]::ReferenceEquals($script:WmsFrontendDevHandles[[string]$Handle.RunId], $Handle.Process)) {
        throw 'DEV02_FOREIGN_PROCESS_HANDLE_REFUSED'
    }
    $process = $Handle.Process
    if (-not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000) }
    $null = $script:WmsFrontendDevHandles.Remove([string]$Handle.RunId)
    $process.Dispose()
}

function Start-WmsFrontendDevBackend {
    [CmdletBinding()]
    param(
        [Parameter(Mandatory = $true)][ValidateRange(1024, 65535)][int]$Port,
        [Parameter(Mandatory = $true)][string]$RunDirectory,
        [Parameter(Mandatory = $true)][string]$GuardReceipt,
        [Parameter(Mandatory = $true)][string]$GuardSha256,
        [Parameter(Mandatory = $true)][string]$JarPath,
        [Parameter(Mandatory = $true)][string]$JarSha256,
        [Parameter(Mandatory = $true)][string]$JavaPath,
        [Parameter(Mandatory = $true)][object]$Identity,
        [int]$GuardProcessId,
        [string]$GuardHelperSha256
    )
    $ErrorActionPreference = 'Stop'
    $root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
    $runs = [IO.Path]::GetFullPath((Join-Path $root 'orchestracao/.runtime/frontend-integracao-dev-runs'))
    $directory = [IO.Path]::GetFullPath($RunDirectory)
    $uuid = [Guid]::Empty
    if (-not $directory.StartsWith($runs + [IO.Path]::DirectorySeparatorChar, [StringComparison]::OrdinalIgnoreCase) -or
        -not [Guid]::TryParse([IO.Path]::GetFileName($directory), [ref]$uuid) -or
        -not (Test-Path -LiteralPath $directory -PathType Container)) { throw 'DEV02_RUN_DIRECTORY_INVALID' }
    foreach ($path in @($runs, $directory)) {
        if ((Get-Item -LiteralPath $path -Force).Attributes -band [IO.FileAttributes]::ReparsePoint) {
            throw 'DEV02_RUN_DIRECTORY_REPARSE_REFUSED'
        }
    }
    $receiptPath = Join-Path $directory 'backend-readiness.json'
    $stream = [IO.File]::Open($receiptPath, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    $runId = [IO.Path]::GetFileName($directory)
    $process = $null; $started = $false; $credential = $null; $info = $null; $client = $null; $handler = $null
    $stage = 'PREFLIGHT_AUTH'
    $receipt = [ordered]@{
        natureza = 'D31_DEV02_BACKEND_READINESS'; runId = $runId; observadoUtc = $null
        estado = 'BLOCKED'; codigo = $null; etapa = $stage
        profile = 'sqlserver-dev'; database = 'WMS_DEV'; login = 'WMSDEV'
        backend = $null; status = $null; guard = $null; artifact = $null; java = $null; config = $null
        authenticatedBusinessRead = $false; sqlProbeByHelper = $false; rawBootLogsSaved = $false
    }
    try {
        Assert-WmsFrontendDevIdentity $Identity
        $stage = 'GUARD_RECEIPT'
        $guard = Assert-WmsFrontendDevGuard $GuardReceipt $GuardSha256 $GuardProcessId $GuardHelperSha256 $directory
        $receipt.guard = $guard.guard
        $stage = 'ARTIFACT_AND_PORT'
        $jar = [IO.Path]::GetFullPath($JarPath); $java = [IO.Path]::GetFullPath($JavaPath)
        $config = Join-Path $root 'backend/src/main/resources/application-frontend-dev.properties'
        if (-not (Test-Path -LiteralPath $jar -PathType Leaf) -or
            $JarSha256 -notmatch '^[A-Fa-f0-9]{64}$' -or
            (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash -ine $JarSha256) { throw 'DEV02_JAR_HASH_INVALID' }
        if (-not (Test-Path -LiteralPath $java -PathType Leaf) -or [IO.Path]::GetFileName($java) -ine 'java.exe') {
            throw 'DEV02_JAVA_PATH_INVALID'
        }
        $release = Join-Path ([IO.Path]::GetDirectoryName([IO.Path]::GetDirectoryName($java))) 'release'
        if (-not (Test-Path -LiteralPath $release -PathType Leaf) -or
            -not ([IO.File]::ReadAllText($release) -match '(?m)^JAVA_VERSION="21[.\-"]')) { throw 'DEV02_JDK21_REQUIRED' }
        if (@(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue).Count) {
            throw 'DEV02_PORT_ALREADY_IN_USE'
        }
        $receipt.artifact = @{ path = $jar; sha256 = $JarSha256.ToLowerInvariant() }
        $receipt.java = @{ path = $java; sha256 = (Get-FileHash -LiteralPath $java -Algorithm SHA256).Hash.ToLowerInvariant() }
        $receipt.config = @{ path = $config; sha256 = (Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash.ToLowerInvariant() }
        $stage = 'PROTECTED_WMSDEV_CHANNEL'
        . (Join-Path $root 'database/scripts/d26-credencial-aplicacao.ps1')
        # Nao chamar Get-WmsDevApplicationProfile/Get-ProjetosSqlProfile:
        # este ultimo pode recapturar certificado e alterar runtime compartilhado.
        $profile = $guard.profile
        if ($profile.database -cne 'WMS_DEV' -or $profile.login -cne 'WMSDEV' -or
            [string]::IsNullOrWhiteSpace([string]$profile.certificateHost) -or
            -not (Test-Path -LiteralPath $profile.truststore -PathType Leaf) -or
            (Get-FileHash -LiteralPath $profile.truststore -Algorithm SHA256).Hash -ine $guard.tls.truststoreSha256) {
            throw 'DEV02_PROFILE_CHANGED_SINCE_GUARD'
        }
        $credential = Get-WmsDevApplicationCredential
        $info = New-Object Diagnostics.ProcessStartInfo
        $info.FileName = $java; $info.WorkingDirectory = Join-Path $root 'backend'
        $info.UseShellExecute = $false; $info.CreateNoWindow = $true
        $info.RedirectStandardOutput = $true; $info.RedirectStandardError = $true
        $info.EnvironmentVariables.Clear()
        foreach ($name in @('SystemRoot', 'WINDIR', 'TEMP', 'TMP', 'USERPROFILE', 'LOCALAPPDATA', 'APPDATA')) {
            $value = [Environment]::GetEnvironmentVariable($name, 'Process')
            if ($value) { $info.EnvironmentVariables[$name] = $value }
        }
        $variables = @{
            WMS_PORT = [string]$Port; WMS_DB_HOST = $profile.host; WMS_DB_PORT = [string]$profile.port
            WMS_DB_NAME = 'WMS_DEV'; WMS_DB_USER = 'WMSDEV'
            WMS_DB_CONFIRMED_TARGET = ('{0}:{1}/WMS_DEV' -f $profile.host, $profile.port)
            WMS_DB_CONFIRMED_SERVER = $guard.server; WMS_DB_CERTIFICATE_HOST = $profile.certificateHost
            WMS_DB_TRUST_STORE = $profile.truststore; WMS_DB_TRUST_STORE_PASSWORD = 'projetos-public-cert'
            WMS_OIDC_ISSUER = $Identity.issuer; WMS_OIDC_JWK_SET_URI = $Identity.jwkSetUri; WMS_OIDC_AUDIENCE = $Identity.audience
        }
        foreach ($entry in $variables.GetEnumerator()) { $info.EnvironmentVariables[$entry.Key] = [string]$entry.Value }
        if ($Identity -is [Collections.IDictionary] -and $Identity.Contains('native') -and $Identity.native) {
            if ($Identity.protectedMaterial -isnot [Security.SecureString]) { throw 'DEV02_AUTH_PROTECTED_MATERIAL_MISSING' }
            $authPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Identity.protectedMaterial)
            try {
                $authMaterial = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($authPointer) | ConvertFrom-Json
                if ($authMaterial.schema -ne 1 -or [string]::IsNullOrWhiteSpace($authMaterial.privateKey) -or $authMaterial.bootstrapHash -notmatch '^\{pbkdf2-600k\}[0-9a-f]{96}$') { throw 'DEV02_AUTH_PROTECTED_MATERIAL_INVALID' }
                $info.EnvironmentVariables['WMS_AUTH_ENABLED']='true'
                $info.EnvironmentVariables['WMS_AUTH_PRIVATE_KEY']=[string]$authMaterial.privateKey
                $info.EnvironmentVariables['WMS_AUTH_BOOTSTRAP_HASH']=[string]$authMaterial.bootstrapHash
                $info.EnvironmentVariables['WMS_AUTH_ORIGIN']=[string]$Identity.origin
                $info.EnvironmentVariables['WMS_AUTH_SECURE_COOKIE']=([bool]$Identity.secureCookie).ToString().ToLowerInvariant()
                $info.EnvironmentVariables['WMS_AUTH_PROXY_ORIGIN']=[string]$Identity.proxyOrigin
            } finally {
                $authMaterial=$null
                [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($authPointer)
                $Identity.protectedMaterial.Dispose()
            }
        }
        $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($credential.Password)
        try { $info.EnvironmentVariables['WMS_DB_PASSWORD'] = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
        # Argumentos fixos nao sensiveis vencem sobreposicoes Spring. Nenhum shell intermediario.
        foreach ($path in @($jar, $config)) { if ($path -match '["\r\n]') { throw 'DEV02_ARGUMENT_PATH_INVALID' } }
        $configUri = ([Uri]$config).AbsoluteUri
        $info.Arguments = '-jar "' + $jar + '" --spring.profiles.active=sqlserver-dev ' +
            '--spring.config.location=classpath:/application.properties,classpath:/application-sqlserver-dev.properties ' +
            '--spring.config.additional-location="' + $configUri + '" ' +
            '--server.address=127.0.0.1 --server.port=' + $Port + ' --wms.cadastros.enabled=true ' +
            '--spring.autoconfigure.exclude= --spring.jpa.hibernate.ddl-auto=validate --spring.jpa.generate-ddl=false ' +
            '--spring.sql.init.mode=never --spring.flyway.enabled=false --spring.liquibase.enabled=false ' +
            '--wms.database.name=WMS_DEV --wms.database.user=WMSDEV'
        $stage = 'BOOT'
        $process = New-Object Diagnostics.Process; $process.StartInfo = $info
        if (-not $process.Start()) { throw 'DEV02_PROCESS_START_REFUSED' }
        $started = $true
        # Drenar e descartar saida bruta: nunca arquivar possiveis valores de bind/config/driver.
        $process.BeginOutputReadLine(); $process.BeginErrorReadLine()
        $info.EnvironmentVariables.Clear(); $credential.Password.Dispose(); $credential = $null
        $receipt.backend = @{ pid = $process.Id; startUtc = $process.StartTime.ToUniversalTime().ToString('o'); url = "http://127.0.0.1:$Port"; port = $Port; listenerOwned = $false }
        Add-Type -AssemblyName System.Net.Http
        $handler = New-Object Net.Http.HttpClientHandler; $handler.UseProxy = $false; $handler.AllowAutoRedirect = $false
        $client = New-Object Net.Http.HttpClient($handler); $client.Timeout = [TimeSpan]::FromSeconds(2)
        $until = [DateTime]::UtcNow.AddSeconds(90)
        do {
            if ($process.HasExited) { throw 'DEV02_BACKEND_EXITED_BEFORE_READY' }
            $listeners = @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
            if ($listeners.Count) {
                if (@($listeners | Where-Object { $_.OwningProcess -ne $process.Id -or $_.LocalAddress -cne '127.0.0.1' }).Count) {
                    throw 'DEV02_LISTENER_NOT_OWNED'
                }
                $receipt.backend.listenerOwned = $true
                $response = $null
                try {
                    $response = $client.GetAsync("http://127.0.0.1:$Port/api/v1/status").GetAwaiter().GetResult()
                    if ([int]$response.StatusCode -eq 200) {
                        $data = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult() | ConvertFrom-Json
                        if ($data.aplicacao -cne 'wms-rodogarcia' -or $data.status -cne 'DISPONIVEL') { throw 'DEV02_STATUS_BODY_MISMATCH' }
                        $receipt.status = @{ http = 200; aplicacao = 'wms-rodogarcia'; status = 'DISPONIVEL' }
                        $receipt.estado = 'READY'; $receipt.codigo = 'DEV02_OWN_BACKEND_STATUS_READY'
                        break
                    }
                } catch { if ($_.Exception.Message -cmatch '^DEV02_') { throw } }
                finally { if ($response) { $response.Dispose() } }
            }
            Start-Sleep -Milliseconds 250
        } while ([DateTime]::UtcNow -lt $until)
        if ($receipt.estado -cne 'READY') { throw 'DEV02_READINESS_TIMEOUT' }
    } catch {
        $receipt.codigo = 'DEV02_START_FAILED'
        if ($_.Exception.Message -cmatch '^DEV02_[A-Z0-9_]+$') { $receipt.codigo = $_.Exception.Message }
        if ($started -and -not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000) }
        if ($process) { $process.Dispose(); $process = $null }
    } finally {
        if ($credential) { $credential.Password.Dispose() }
        if ($info) { $info.EnvironmentVariables.Clear() }
        if ($client) { $client.Dispose() }; if ($handler) { $handler.Dispose() }
        $receipt.etapa = $stage; $receipt.observadoUtc = [DateTime]::UtcNow.ToString('o')
        try {
            $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($receipt | ConvertTo-Json -Depth 8))
            $stream.Write($bytes, 0, $bytes.Length)
        } catch {
            if ($started -and $process -and -not $process.HasExited) { $process.Kill(); $null = $process.WaitForExit(5000) }
            throw 'DEV02_RECEIPT_WRITE_FAILED'
        } finally { $stream.Dispose() }
    }
    if ($process) {
        if (-not (Get-Variable WmsFrontendDevHandles -Scope Script -ErrorAction SilentlyContinue)) { $script:WmsFrontendDevHandles = @{} }
        $script:WmsFrontendDevHandles[$runId] = $process
    }
    [pscustomobject]@{ RunId = $runId; Process = $process; Sanitized = [pscustomobject]$receipt
        ReceiptPath = $receiptPath; ReceiptSha256 = (Get-FileHash -LiteralPath $receiptPath -Algorithm SHA256).Hash.ToLowerInvariant() }
}
