# Casos estruturais fictícios; não inicia Java, HTTP, DPAPI ou SQL.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'artefato-backend.ps1')
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$run = Join-Path $root ('orchestracao/.runtime/acesso-visao-a01/fixtures/' + [guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $run
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
$cases = @()
$classes = @('controllers/VisaoOperacaoController.class', 'services/VisaoOperacaoService.class',
    'controllers/DashboardController.class', 'config/LoginConfig.class', 'config/CadastrosSegurancaConfig.class')
function New-FixtureJar([string]$Name, [bool]$Complete) {
    $path = Join-Path $run $Name
    $zip = [IO.Compression.ZipFile]::Open($path, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($class in $classes) {
            if (-not $Complete -and $class -eq $classes[0]) { continue }
            $entry = $zip.CreateEntry('BOOT-INF/classes/br/com/rodogarcia/wms/' + $class)
            $stream = $entry.Open()
            try { $stream.WriteByte(0) } finally { $stream.Dispose() }
        }
    } finally { $zip.Dispose() }
    return $path
}
function Check([string]$Name, [string]$Expected, $Receipt) {
    $path = Join-Path $run ($Name + '.json')
    [IO.File]::WriteAllText($path, ($Receipt | ConvertTo-Json -Depth 4), [Text.UTF8Encoding]::new($false))
    $actual = 'ESTRUTURA_FICTICIA_ACEITA'
    try { $null = Assert-WmsDevApplicationArtifact $root $path } catch { $actual = $_.Exception.Message }
    $script:cases += @{caso=$Name;esperado=$Expected;resultado=$actual;aprovado=($actual -ceq $Expected)}
}
$jar = New-FixtureJar 'completo-ficticio.jar' $true
$source = Get-WmsBackendSourceHash $root
$hash = (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash
Check 'estrutura-completa' 'ESTRUTURA_FICTICIA_ACEITA' @{sourceHash=$source;jar=@{path=$jar;sha256=$hash}}
Check 'recibo-antigo' 'DEV02_ARTIFACT_SOURCE_STALE' @{jar=@{path=$jar;sha256=$hash}}
Check 'fonte-alterada' 'DEV02_ARTIFACT_SOURCE_STALE' @{sourceHash=('0'*64);jar=@{path=$jar;sha256=$hash}}
Check 'jar-alterado' 'DEV02_ARTIFACT_JAR_INVALID' @{sourceHash=$source;jar=@{path=$jar;sha256=('0'*64)}}
Check 'jar-fora-projeto' 'DEV02_ARTIFACT_JAR_INVALID' @{sourceHash=$source;jar=@{path=(Join-Path ([IO.Path]::GetTempPath()) 'alheio.jar');sha256=$hash}}
$incomplete = New-FixtureJar 'incompleto-ficticio.jar' $false
Check 'api-ausente' 'DEV02_ARTIFACT_API_MISSING' @{sourceHash=$source;jar=@{path=$incomplete;sha256=(Get-FileHash -LiteralPath $incomplete).Hash}}
$cases | ConvertTo-Json -Depth 3
if (@($cases | Where-Object { -not $_.aprovado }).Count) { throw 'ARTEFATO_FIXTURE_FAILED' }
