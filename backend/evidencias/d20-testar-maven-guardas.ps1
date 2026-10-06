# Fixtures offline: somente validate/help, nunca um goal Flyway ou conexão SQL.
$ErrorActionPreference = 'Stop'
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$wrapper = Join-Path $backend 'mvnw.cmd'
. (Join-Path $backend '../database/d20-guardas.ps1')
$sqlFicticio = Get-WmsD20FlywayInitSql "SQL'FICTICIO" "mig'FICTICIO"
$cases = @(
    @{id='sem-marker'; bank='WMS_DEV'; marker=$null; sql=$null; goal='validate'; expected=1},
    @{id='prod-recusa'; bank='WMS_PROD'; marker='D20_WMS_DEV_VERIFICADO'; sql="THROW 51002, 'Fixture offline.', 1;"; goal='validate'; expected=1},
    @{id='fonte-ausente'; bank='WMS_DEV'; marker='D20_WMS_DEV_VERIFICADO'; sql=$null; goal='validate'; expected=1},
    @{id='validacao-ficticia'; bank='WMS_DEV'; marker='D20_WMS_DEV_VERIFICADO'; sql=$sqlFicticio; goal='validate'; expected=0},
    @{id='default-init'; bank='WMS_DEV'; marker=$null; sql=$null; goal='help:evaluate -Dexpression=wms.migrations.initSql -q -DforceStdout'; expected=0},
    @{id='marker-divergente'; bank='WMS_DEV'; marker='OUTRO'; sql=$sqlFicticio; goal='help:evaluate -Dexpression=wms.migrations.initSql -q -DforceStdout'; expected=0},
    @{id='sem-marker-com-fonte'; bank='WMS_DEV'; marker=$null; sql=$sqlFicticio; goal='help:evaluate -Dexpression=wms.migrations.initSql -q -DforceStdout'; expected=0},
    @{id='fonte-wrapper-escapada'; bank='WMS_DEV'; marker='D20_WMS_DEV_VERIFICADO'; sql=$sqlFicticio; goal='help:evaluate -Dexpression=wms.migrations.initSql -q -DforceStdout'; expected=0}
)
$results = @()
foreach($case in $cases) {
    $psi = New-Object Diagnostics.ProcessStartInfo
    $psi.FileName = 'cmd.exe'
    $psi.Arguments = '/d /s /c ""' + $wrapper + '" -B -ntp -Pmigrations ' + $case.goal + '"'
    $psi.WorkingDirectory = $backend
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    # Sem ler/registrar valores do ambiente real: remover pelas chaves e usar fixtures.
    foreach($key in @($psi.EnvironmentVariables.Keys)) {
        if($key -match '^(WMS_|FLYWAY_|JAVA_TOOL_OPTIONS$|_JAVA_OPTIONS$|JDK_JAVA_OPTIONS$|MAVEN_ARGS$|MAVEN_OPTS$)') {
            $psi.EnvironmentVariables.Remove($key)
        }
    }
    $psi.EnvironmentVariables['JAVA_HOME'] = $env:JAVA_HOME
    $psi.EnvironmentVariables['MAVEN_OPTS'] = '-Xmx384m'
    $psi.EnvironmentVariables['WMS_DB_NAME'] = $case.bank
    if($case.marker) { $psi.EnvironmentVariables['WMS_DB_MIGRATION_GUARD'] = $case.marker }
    if($case.sql) { $psi.EnvironmentVariables['WMS_DB_MIGRATION_INIT_SQL'] = $case.sql }
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $psi
    [void]$process.Start()
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $output = $stdout.Result + $stderr.Result
    $log = Join-Path $PSScriptRoot ('d20-maven-' + $case.id + '.log')
    [IO.File]::WriteAllText($log, $output, [Text.UTF8Encoding]::new($false))
    $ok = ($process.ExitCode -eq $case.expected)
    if($case.id -in @('default-init','marker-divergente','sem-marker-com-fonte')) { $ok = $ok -and $output.Contains('THROW 51002') -and !$output.Contains('SERVERPROPERTY') }
    if($case.id -eq 'fonte-wrapper-escapada') {
        foreach($part in @("SQL''FICTICIO", "mig''FICTICIO", 'SERVERPROPERTY', 'ORIGINAL_LOGIN', 'WMS_DEV', 'ARITHABORT ON', 'NUMERIC_ROUNDABORT OFF')) {
            $ok = $ok -and $output.Contains($part)
        }
    }
    $results += [pscustomobject]@{id=$case.id;exitCode=$process.ExitCode;expected=$case.expected;passed=$ok;goal=$case.goal;log=[IO.Path]::GetFileName($log)}
    Write-Output "$($case.id): exit=$($process.ExitCode), passou=$ok"
    $process.Dispose()
}
$results | ConvertTo-Json -Depth 3 | Set-Content (Join-Path $PSScriptRoot 'd20-maven-guardas.json') -Encoding utf8
if(@($results | Where-Object {!$_.passed}).Count -gt 0) { throw 'Fixture Maven falhou; conferir logs offline.' }
