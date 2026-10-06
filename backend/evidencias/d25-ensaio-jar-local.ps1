param(
    [Parameter(Mandatory=$true)][string]$JavaHome,
    [string]$BuildDirectory = 'target-d25-final'
)
$ErrorActionPreference = 'Stop'
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ($BuildDirectory -notmatch '^target-d25(?:-final)?$') { throw 'Build D25 exclusivo exigido.' }
$jar = Join-Path $backend "$BuildDirectory/wms-backend-0.0.1-SNAPSHOT.jar"
if (!(Test-Path -LiteralPath $jar -PathType Leaf)) { throw 'JAR D25 ausente.' }
$java = Join-Path $JavaHome 'bin/java.exe'
Add-Type -AssemblyName System.Net.Http
$start = New-Object Diagnostics.ProcessStartInfo
$start.FileName = $java
$start.WorkingDirectory = $backend
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
$start.Arguments = '-Xmx512m -jar "' + $jar + '" --spring.profiles.active=local --server.address=127.0.0.1 --server.port=0 --spring.config.location=classpath:/application.properties,classpath:/application-local.properties'
$start.EnvironmentVariables.Clear()
foreach ($key in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE','LOCALAPPDATA')) {
    $value = [Environment]::GetEnvironmentVariable($key,'Process')
    if ($null -ne $value) { $start.EnvironmentVariables[$key] = $value }
}
$process = New-Object Diagnostics.Process
$process.StartInfo = $start
$handler = New-Object Net.Http.HttpClientHandler
$handler.UseProxy = $false
$handler.AllowAutoRedirect = $false
$http = [Net.Http.HttpClient]::new($handler)
$http.Timeout = [TimeSpan]::FromSeconds(5)
$result = [ordered]@{natureza='D25_JAR_LOCAL_REAL';inicioUtc=[DateTime]::UtcNow.ToString('o');perfil='local';sql=$false;h2=$false;jar=$jar;jarSha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash;java=$java;heapMiB=512;portaDinamica=$true;ambienteFilho='SOMENTE_OS_WHITELIST_SEM_CONFIG_CREDENCIAL';config='SOMENTE_CLASSPATH_LOCAL';checks=@();concluido=$false}
try {
    if (!$process.Start()) { throw 'JVM não iniciou.' }
    $result.pid = $process.Id
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $limite = [DateTime]::UtcNow.AddSeconds(60)
    $listener = $null
    do {
        if ($process.HasExited) { throw 'JVM encerrou antes do HTTP.' }
        $listener = @(Get-NetTCPConnection -OwningProcess $process.Id -State Listen -ErrorAction SilentlyContinue)
        if ($listener.Count -gt 0) { break }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $limite)
    if ($listener.Count -ne 1 -or $listener[0].LocalAddress -ne '127.0.0.1') { throw 'Listener exclusivo loopback não confirmado.' }
    $port = $listener[0].LocalPort
    $result.endereco = "http://127.0.0.1:$port"
    $base = $result.endereco
    $pronto = $false
    $tentativas = 0
    do {
        $tentativas++
        try {
            $readiness = $http.GetAsync($base + '/api/v1/status').GetAwaiter().GetResult()
            try { $pronto = [int]$readiness.StatusCode -eq 200 } finally { $readiness.Dispose() }
        } catch [Net.Http.HttpRequestException] {
            # Aceita apenas indisponibilidade temporária durante o startup desta JVM.
        } catch [Threading.Tasks.TaskCanceledException] {
            # HTTP tem timeout finito e o prazo global permanece obrigatório.
        }
        if ($process.HasExited) { throw 'JVM encerrou durante a espera HTTP.' }
        if (!$pronto) { Start-Sleep -Milliseconds 250 }
    } while (!$pronto -and [DateTime]::UtcNow -lt $limite)
    if (!$pronto) { throw 'Status não ficou pronto no prazo.' }
    $result.readinessTentativas = $tentativas
    $cases = @(
        @{method='GET';path='/api/v1/status';expected=200},
        @{method='GET';path='/api/v1/clientes';expected=403},
        @{method='POST';path='/api/v1/status';expected=403},
        @{method='GET';path='/rota-ficticia-d25';expected=403},
        @{method='GET';path='/error';expected=403},
        @{method='GET';path='/api/v1/status/';expected=403}
    )
    $ids = @()
    foreach ($case in $cases) {
        $request = [Net.Http.HttpRequestMessage]::new([Net.Http.HttpMethod]::new($case.method), ($base + $case.path))
        [void]$request.Headers.TryAddWithoutValidation('X-Request-Id','id-ficticio-cliente-d25')
        $response = $http.SendAsync($request).GetAwaiter().GetResult()
        try {
            $body = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            $json = $body | ConvertFrom-Json
            $status = [int]$response.StatusCode
            $cookie = $response.Headers.Contains('Set-Cookie')
            $interno = $body -match 'stackTrace|java\.|password|senha|jdbc:|SQLException'
            $result.ultimoCheck = [ordered]@{method=$case.method;path=$case.path;status=$status;setCookiePresente=$cookie;detalheInternoPresente=$interno}
            $id = @($response.Headers.GetValues('X-Request-Id'))[0]
            $guid = [Guid]::Empty
            if ($status -ne $case.expected -or ![Guid]::TryParse($id,[ref]$guid) -or $ids -contains $id) { throw 'Status/request-id incorreto.' }
            $ids += $id
            $type = $response.Content.Headers.ContentType.MediaType
            if ($status -eq 200) {
                if ($json.aplicacao -ne 'wms-rodogarcia' -or $json.status -ne 'DISPONIVEL' -or !$json.instante -or $type -ne 'application/json') { throw 'Contrato de status divergente.' }
            } else {
                if ($type -ne 'application/problem+json' -or $json.codigo -ne 'ACESSO_NEGADO' -or $json.status -ne 403 -or $json.idOperacao -ne $id) { throw 'Contrato de recusa divergente.' }
            }
            if ($interno) { $result.condicaoFalha='DETALHE_INTERNO_PRESENTE'; throw 'D25_JAR_LOCAL_DETALHE_INTERNO_PRESENTE' }
            if ($cookie) { $result.condicaoFalha='SET_COOKIE_PRESENTE'; throw 'D25_JAR_LOCAL_SET_COOKIE_PRESENTE' }
            $result.checks += [ordered]@{method=$case.method;path=$case.path;status=$status;contentType=$type;requestId=$id;body=$json;setCookiePresente=$cookie;detalheInternoPresente=$interno;aprovado=$true}
        } finally { $response.Dispose(); $request.Dispose() }
    }
    $result.concluido = $true
} finally {
    if ($result.pid) {
        if (!$process.HasExited) { $process.Kill(); $process.WaitForExit() }
        $result.pidEncerrado = $process.HasExited
        $result.exitCode = $process.ExitCode
        $out = $stdout.GetAwaiter().GetResult()
        $err = $stderr.GetAwaiter().GetResult()
        [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'd25-jar-local-stdout.log'),$out,(New-Object Text.UTF8Encoding($false)))
        [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'd25-jar-local-stderr.log'),$err,(New-Object Text.UTF8Encoding($false)))
        $result.persistenciaInicializada = [bool]($out -match 'Initialized JPA|HikariPool|HikariDataSource|jdbc:h2|jdbc:sqlserver')
    }
    $result.fimUtc = [DateTime]::UtcNow.ToString('o')
    $result | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'd25-jar-local-http.json') -Encoding UTF8
    $http.Dispose(); $handler.Dispose(); $process.Dispose()
}
if (!$result.concluido -or !$result.pidEncerrado -or $result.persistenciaInicializada) { throw 'Ensaio local incompleto ou persistência inesperada.' }
Write-Output "D25_JAR_LOCAL_OK: $($result.checks.Count) checks; PID $($result.pid) encerrado; porta $port."
