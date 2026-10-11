[CmdletBinding()]
param([switch]$SomenteBuild)
Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
# SQL01: build sem banco e aceite SQL sao etapas distintas. Sem prova SQL
# vinculada ao candidato, este script nao publica recibos nem recomenda o BAT.
if (-not $SomenteBuild) { throw 'SQL01_INTEGRACAO_OBRIGATORIA' }
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
$evidence = Join-Path $run 'sql01-prumo-build-evidencias'
# Saida exclusiva do build. Nao seleciona os contextos H2/D30 historicos.
if (Test-Path -LiteralPath $evidence) { throw 'DEV02_BUILD_EVIDENCE_ALREADY_EXISTS' }
$null = New-Item -ItemType Directory -Path $evidence
$sourceHash = Get-WmsBackendSourceHash $repository
$wrapper = Join-Path $repository 'backend/mvnw.cmd'
foreach ($path in @($wrapper, $build, $evidence)) { if ($path -match '["\r\n%!]') { throw 'DEV02_BUILD_PATH_INVALID' } }
$info = [Diagnostics.ProcessStartInfo]::new()
$info.FileName = Join-Path $env:SystemRoot 'System32/cmd.exe'
$info.Arguments = '/d /c ""' + $wrapper + '" -B "-Dwms.build.directory=' + $build +
    '" "-Dwms.test.evidencias.dir=' + $evidence + '" "-Dmaven.test.skip=true" ' +
    '"-Ppure-no-db,!sqlserver-it,!migrations,!migrations-wrapper-confirmado,!bootstrap-local,!bootstrap-local-wrapper-confirmado" package"'
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
Write-Host 'Compilando candidato sem banco e sem testes. Integracao SQL obrigatoria pendente; recibos ativos preservados.'
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
$candidates = New-Object 'Collections.Generic.List[object]'
foreach ($relative in $receiptPaths) {
    $path = Join-Path $repository $relative
    $current = [ordered]@{
        natureza='SQL01_CANDIDATO_BUILD_SEM_BANCO'; observadoUtc=[DateTime]::UtcNow.ToString('o')
        estado='BUILD_SEM_BANCO_SQL_PENDENTE'; ativosAtualizados=$false
        sourceHash=$sourceHash
        java=@{path=$javaPath;sha256=(Get-FileHash -LiteralPath $javaPath -Algorithm SHA256).Hash.ToLowerInvariant()}
        jar=@{path=$jar;sha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant()}
        config=@{path=$config;sha256=(Get-FileHash -LiteralPath $config -Algorithm SHA256).Hash.ToLowerInvariant()}
        verificacao=@{testes=0;ambiente='Build sem banco';sqlServer=$false;integracaoSqlObrigatoria=$true;log=(Join-Path $run 'build.log')}
        linhagem=@{reciboAnterior=$path;sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant();preservado=$true}
    }
    $prepared = Join-Path $run ('novo-' + [IO.Path]::GetFileName($path))
    $stream = [IO.File]::Open($prepared, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
    try {
        $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($current | ConvertTo-Json -Depth 5) + [Environment]::NewLine)
        $stream.Write($bytes, 0, $bytes.Length)
    } finally { $stream.Dispose() }
    $null = Assert-WmsDevApplicationArtifact $repository $prepared
    $candidates.Add([pscustomobject]@{path=$prepared;sha256=(Get-FileHash -LiteralPath $prepared -Algorithm SHA256).Hash.ToLowerInvariant()})
}
$manifest = [ordered]@{
    natureza='SQL01_CANDIDATO_BUILD_SEM_BANCO'; observadoUtc=[DateTime]::UtcNow.ToString('o')
    estado='BUILD_SEM_BANCO_SQL_PENDENTE'; sourceHash=$sourceHash;jar=$current.jar
    verificacao=$current.verificacao;ativosAtualizados=$false;candidatos=$candidates.ToArray()
}
$stream = [IO.File]::Open((Join-Path $run 'preparo-candidato.json'), [IO.FileMode]::CreateNew, [IO.FileAccess]::Write, [IO.FileShare]::None)
try {
    $bytes = [Text.UTF8Encoding]::new($false).GetBytes(($manifest | ConvertTo-Json -Depth 6) + [Environment]::NewLine)
    $stream.Write($bytes, 0, $bytes.Length)
} finally { $stream.Dispose() }
Write-Host ('Candidato compilado: ' + (Join-Path $run 'preparo-candidato.json') + '. Integracao SQL obrigatoria pendente; nenhum recibo ativo atualizado.')
