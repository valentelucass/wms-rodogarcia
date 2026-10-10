[CmdletBinding()]
param()
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$repository = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'artefato-backend.ps1')
$receiptPaths = @('backend/evidencias/login-d32-preparo.json', 'backend/evidencias/frontend-dev02-preparo.json')
$previous = Get-Content -LiteralPath (Join-Path $repository $receiptPaths[0]) -Raw -Encoding UTF8 | ConvertFrom-Json
$javaPath = [IO.Path]::GetFullPath($previous.java.path)
$javaHome = Split-Path (Split-Path $javaPath -Parent) -Parent
if ([IO.Path]::GetFileName($javaPath) -ine 'java.exe' -or
    -not (Test-Path -LiteralPath $javaPath -PathType Leaf) -or
    -not ([IO.File]::ReadAllText((Join-Path $javaHome 'release')) -match '(?m)^JAVA_VERSION="21[.\-]')) {
    throw 'DEV02_JDK21_REQUIRED'
}
$run = Join-Path $repository ('orchestracao/.runtime/backend-dev-builds/' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $run
$build = Join-Path $run 'target'
$sourceHash = Get-WmsBackendSourceHash $repository
$wrapper = Join-Path $repository 'backend/mvnw.cmd'
foreach ($path in @($wrapper, $build)) { if ($path -match '["\r\n%!]') { throw 'DEV02_BUILD_PATH_INVALID' } }
$info = [Diagnostics.ProcessStartInfo]::new()
$info.FileName = Join-Path $env:SystemRoot 'System32/cmd.exe'
$info.Arguments = '/d /c ""' + $wrapper + '" -B "-Dwms.build.directory=' + $build +
    '" "-P!sqlserver-it,!migrations,!migrations-wrapper-confirmado" ' +
    '"-Dtest=LoginIntegrationTest,VisaoOperacaoIntegrationTest,VisaoOperacaoServiceTest,ArquiteturaTest,NfeDocumentoServiceTest,NfeXmlServiceTest,PedidoEntradaXmlIntegrationTest,RecebimentoIntegrationTest" verify"'
$info.WorkingDirectory = Join-Path $repository 'backend'
$info.UseShellExecute = $false
$info.CreateNoWindow = $true
$info.RedirectStandardOutput = $true
$info.RedirectStandardError = $true
# O build recebe somente ambiente de ferramentas, nunca o material do login ou do SQL.
$info.EnvironmentVariables.Clear()
foreach ($name in @('SystemRoot','WINDIR','ComSpec','PATH','PATHEXT','TEMP','TMP','USERPROFILE',
    'HOMEDRIVE','HOMEPATH','APPDATA','LOCALAPPDATA','PROGRAMDATA')) {
    $value = [Environment]::GetEnvironmentVariable($name)
    if ($null -ne $value) { $info.EnvironmentVariables[$name] = $value }
}
$info.EnvironmentVariables['JAVA_HOME'] = $javaHome
$info.EnvironmentVariables['PATH'] = (Join-Path $javaHome 'bin') + ';' + $info.EnvironmentVariables['PATH']
$process = [Diagnostics.Process]::new()
$process.StartInfo = $info
Write-Host 'Preparando backend atual e conferindo login, visao geral e recebimento/XML em H2 isolado. Sem SQL Server.'
try {
    $null = $process.Start()
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    [IO.File]::WriteAllText((Join-Path $run 'build.log'), $stdout.Result + $stderr.Result, [Text.UTF8Encoding]::new($false))
    if ($process.ExitCode -ne 0) { throw ('DEV02_LOCAL_BUILD_FAILED: ' + (Join-Path $run 'build.log')) }
} finally { $process.Dispose() }
if ($sourceHash -cne (Get-WmsBackendSourceHash $repository)) { throw 'DEV02_SOURCE_CHANGED_DURING_BUILD' }
$jar = Join-Path $build 'wms-backend-0.0.1-SNAPSHOT.jar'
$config = Join-Path $repository 'backend/src/main/resources/application-frontend-dev.properties'
$reports = @(Get-ChildItem -LiteralPath (Join-Path $build 'surefire-reports') -Filter 'TEST-*.xml' -File)
$tests = 0
foreach ($report in $reports) {
    $xml = [xml][IO.File]::ReadAllText($report.FullName)
    if ([int]$xml.testsuite.failures -ne 0 -or [int]$xml.testsuite.errors -ne 0) { throw 'DEV02_TEST_REPORT_FAILED' }
    $tests += [int]$xml.testsuite.tests
}
if ($tests -eq 0) { throw 'DEV02_TEST_REPORT_MISSING' }
foreach ($relative in $receiptPaths) {
    $path = Join-Path $repository $relative
    $backup = Join-Path $run ([IO.Path]::GetFileName($path))
    Copy-Item -LiteralPath $path -Destination $backup
    $current = [ordered]@{
        natureza='BE02_DEV_ARTEF01_FONTE_ATUAL_LOGIN_E_VISAO'; observadoUtc=[DateTime]::UtcNow.ToString('o')
        sourceHash=$sourceHash
        java=@{path=$javaPath;sha256=(Get-FileHash -LiteralPath $javaPath -Algorithm SHA256).Hash.ToLowerInvariant()}
        jar=@{path=$jar;sha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()}
        config=@{path=$config;sha256=(Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash.ToLowerInvariant()}
        verificacao=@{testes=$tests;ambiente='H2 isolado';sqlServer=$false;log=(Join-Path $run 'build.log')}
        linhagem=@{reciboAnterior=$backup;sha256=(Get-FileHash -LiteralPath $backup -Algorithm SHA256).Hash.ToLowerInvariant()}
    }
    $prepared = Join-Path $run ('novo-' + [IO.Path]::GetFileName($path))
    [IO.File]::WriteAllText($prepared, ($current | ConvertTo-Json -Depth 5) + [Environment]::NewLine, [Text.UTF8Encoding]::new($false))
    $null = Assert-WmsDevApplicationArtifact $repository $prepared
    Copy-Item -LiteralPath $prepared -Destination $path
}
Write-Host ('Pacote atual preparado: ' + $tests + ' testes aprovados. Execute iniciar-dev.bat para carrega-lo.')
