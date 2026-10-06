# Somente validate/help. Nao invoca Flyway, SQL, Java compile/test nem build integral.
param([string[]]$Only=@())
$ErrorActionPreference = 'Stop'
$backend = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$wrapper = Join-Path $backend 'mvnw.cmd'
. (Join-Path $backend '../database/scripts/d21-guardas.ps1')
Assert-WmsD21Pom (Join-Path $backend 'pom.xml')
$cases = @(
    @{id='dev-sa'; bank='WMS_DEV'; expected=0},
    @{id='prod-sa'; bank='WMS_PROD'; expected=0},
    @{id='sem-marker'; bank='WMS_DEV'; marker=''; expected=1},
    @{id='marker-d20'; bank='WMS_DEV'; marker='D20_WMS_DEV_VERIFICADO'; expected=1},
    @{id='fonte-ausente'; bank='WMS_DEV'; noSql=$true; expected=1},
    @{id='fonte-vazia'; bank='WMS_DEV'; sql=' '; expected=1},
    @{id='servidor-ausente'; bank='WMS_DEV'; noServer=$true; expected=1},
    @{id='senha-ausente'; bank='WMS_DEV'; noPassword=$true; expected=1},
    @{id='outro-banco'; bank='master'; expected=1},
    @{id='banco-minusculo'; bank='wms_dev'; expected=1},
    @{id='host-remoto'; bank='WMS_DEV'; host='sql.test.invalid'; expected=1},
    @{id='porta-outra'; bank='WMS_DEV'; port='1434'; expected=1},
    @{id='usuario-outro'; bank='WMS_DEV'; user='app-ficticio'; expected=1},
    @{id='init-sem-marker'; bank='WMS_DEV'; marker=''; evaluate='wms.bootstrap.initSql'; expected=0; throw=$true},
    @{id='init-marker-errado'; bank='WMS_DEV'; marker='OUTRO'; evaluate='wms.bootstrap.initSql'; expected=0; throw=$true},
    @{id='init-dev-escapado'; bank='WMS_DEV'; evaluate='wms.bootstrap.initSql'; expected=0; source=$true},
    @{id='init-prod-escapado'; bank='WMS_PROD'; evaluate='wms.bootstrap.initSql'; expected=0; source=$true},
    @{id='d20-fonte-isolada'; bank='WMS_PROD'; evaluate='wms.migrations.initSql'; expected=0; d20=$true},
    @{id='skip-default'; bank='WMS_DEV'; evaluate='wms.bootstrap.skip'; expected=0; skip=$true},
    @{id='config-dev'; bank='WMS_DEV'; effective=$true; noPassword=$true; expected=0},
    @{id='config-prod'; bank='WMS_PROD'; effective=$true; noPassword=$true; expected=0},
    @{id='config-pre-pending'; bank='WMS_DEV'; effective=$true; noPassword=$true; ignore='*:pending'; expected=0},
    @{id='config-runner-prod'; bank='WMS_PROD'; effective=$true; noPassword=$true; enable=$true; expected=0},
    @{id='config-fora-runner'; bank='WMS_PROD'; effective=$true; noPassword=$true; enable=$true; marker=''; expected=0},
    @{id='sem-profile-manual'; bank='WMS_DEV'; noProfile=$true; expected=0}
)
$results = @()
if ($Only.Count -gt 0) {
    if (@($Only | Where-Object {$_ -notin $cases.id}).Count -gt 0) { throw 'Fixture desconhecida.' }
    $cases=@($cases | Where-Object {$_.id -in $Only})
    $priorPath=Join-Path $PSScriptRoot 'd21-maven-resultados.json'
    if (Test-Path -LiteralPath $priorPath) {
        $prior=Get-Content $priorPath -Raw -Encoding UTF8 | ConvertFrom-Json
        $results=@($prior | Where-Object {$_.id -notin $Only})
    }
}
foreach ($case in $cases) {
    $passwordFixture = [Guid]::NewGuid().ToString('N')
    $bankForSql = if ($case.bank -cin @('WMS_DEV','WMS_PROD')) { $case.bank } else { 'WMS_DEV' }
    $sql = Get-WmsD21InitSql $bankForSql "SQL'FICTICIO"
    if ($case.ContainsKey('sql')) { $sql=$case.sql }
    $goal='validate'
    if ($case.evaluate) { $goal='help:evaluate -Dexpression='+$case.evaluate+' -q -DforceStdout' }
    $effectivePath=Join-Path $PSScriptRoot ('d21-maven-'+$case.id+'.xml')
    if ($case.effective) { $goal='help:effective-pom -Doutput="'+$effectivePath+'"' }
    $profile=if ($case.noProfile) { '' } else { '-Pbootstrap-local ' }
    $settings=[IO.Path]::GetFullPath((Join-Path $backend '../database/config/flyway-settings-vazias.xml'))
    $psi=New-Object Diagnostics.ProcessStartInfo
    $psi.FileName='cmd.exe'
    $ignoreArgument=if ($case.ignore) { '-Dflyway.ignoreMigrationPatterns='+$case.ignore+' ' } else { '-Dflyway.ignoreMigrationPatterns= ' }
    $enableArgument=if ($case.enable) { '-Dwms.bootstrap.skip=false ' } else { '' }
    $psi.Arguments='/d /s /c ""'+$wrapper+'" -B -ntp -o -s "'+$settings+'" -gs "'+$settings+'" '+$profile+$ignoreArgument+$enableArgument+$goal+'"'
    $psi.WorkingDirectory=$backend
    $psi.UseShellExecute=$false
    $psi.CreateNoWindow=$true
    $psi.RedirectStandardOutput=$true
    $psi.RedirectStandardError=$true
    foreach ($key in @($psi.EnvironmentVariables.Keys)) {
        if ($key -match '^(WMS_|FLYWAY_|MVNW_|JAVA_TOOL_OPTIONS$|_JAVA_OPTIONS$|JDK_JAVA_OPTIONS$|MAVEN_ARGS$|MAVEN_OPTS$|MAVEN_DEBUG_OPTS$)') { $psi.EnvironmentVariables.Remove($key) }
    }
    $psi.EnvironmentVariables['JAVA_HOME']=$env:JAVA_HOME
    $psi.EnvironmentVariables['MAVEN_OPTS']='-Xmx384m'
    $psi.EnvironmentVariables['MAVEN_SKIP_RC']='true'
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_HOST']=if ($case.host) { $case.host } else { '127.0.0.1' }
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_PORT']=if ($case.port) { $case.port } else { '1433' }
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_NAME']=$case.bank
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_USER']=if ($case.user) { $case.user } else { 'sa' }
    if (-not $case.noPassword) { $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_PASSWORD']=$passwordFixture }
    $marker=if ($case.ContainsKey('marker')) { $case.marker } else { 'D21_LOCAL_MANUAL_CONFIRMADO' }
    if ($marker) { $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_GUARD']=$marker }
    if (-not $case.noServer) { $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_SERVER']="SQL'FICTICIO" }
    if (-not $case.noSql) { $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL']=$sql }
    $process=New-Object Diagnostics.Process
    $process.StartInfo=$psi
    [void]$process.Start()
    $stdout=$process.StandardOutput.ReadToEndAsync()
    $stderr=$process.StandardError.ReadToEndAsync()
    $process.WaitForExit()
    $output=$stdout.Result+$stderr.Result
    $ok=$process.ExitCode -eq $case.expected
    if ($output.Contains($passwordFixture)) { $ok=$false; $output='FIXTURE_PASSWORD_OUTPUT_RECUSADO' }
    if ($case.throw) { $ok=$ok -and $output.Contains('THROW 51021') -and -not $output.Contains('SERVERPROPERTY') }
    if ($case.d20) { $ok=$ok -and $output.Contains('THROW 51002') -and -not $output.Contains('SERVERPROPERTY') }
    if ($case.skip) { $ok=$ok -and $output.Trim().EndsWith('true') }
    if ($case.source) {
        foreach ($piece in @("SQL''FICTICIO",$case.bank,'SERVERPROPERTY','DB_NAME() IS NULL','ORIGINAL_LOGIN() IS NULL',"N'sa'",'ARITHABORT ON','NUMERIC_ROUNDABORT OFF')) { $ok=$ok -and $output.Contains($piece) }
    }
    if ($case.effective -and $process.ExitCode -eq 0) {
        [xml]$effective=[IO.File]::ReadAllText($effectivePath)
        $config=$effective.SelectSingleNode('/*[local-name()="project"]/*[local-name()="build"]/*[local-name()="plugins"]/*[local-name()="plugin"][*[local-name()="artifactId"]="flyway-maven-plugin"]/*[local-name()="configuration"]')
        $ok=$ok -and $null -ne $config
        if ($config) {
            $ok=$ok -and $config.url -ceq ('jdbc:sqlserver://127.0.0.1:1433;databaseName='+$case.bank+';encrypt=true;trustServerCertificate=false')
            $expectedSkip=if ($case.enable) { 'false' } else { 'true' }
            $ok=$ok -and $config.user -ceq 'sa' -and $config.skip -ceq $expectedSkip
            $ok=$ok -and $config.placeholders.wmsDatabase -ceq $case.bank
            if ($case.ContainsKey('marker') -and -not $case.marker) {
                $ok=$ok -and ([string]$config.initSql).Contains('THROW 51021') -and -not ([string]$config.initSql).Contains('SERVERPROPERTY')
            } else { $ok=$ok -and $config.initSql -ceq $sql }
            $ok=$ok -and $config.cleanDisabled -ceq 'true' -and $config.baselineOnMigrate -ceq 'false'
            $ok=$ok -and $config.validateOnMigrate -ceq 'true' -and $config.outOfOrder -ceq 'false'
            $ok=$ok -and $config.defaultSchema -ceq 'wms' -and $config.table -ceq 'flyway_schema_history'
            $ok=$ok -and ([string]$config.locations.location).Replace([char]92,[char]47) -ceq ('filesystem:'+$backend.Replace([char]92,[char]47)+'/../database/migrations')
            $ok=$ok -and $null -eq $config.target
            $pattern=$config.SelectSingleNode('*[local-name()="ignoreMigrationPatterns"]')
            $expectedPattern=if ($case.ignore) { $case.ignore } else { '' }
            $ok=$ok -and $null -ne $pattern -and $pattern.InnerText -ceq $expectedPattern
        }
    }
    $log=Join-Path $PSScriptRoot ('d21-maven-'+$case.id+'.log')
    [IO.File]::WriteAllText($log,$output,[Text.UTF8Encoding]::new($false))
    $results+=[pscustomobject]@{id=$case.id;exitCode=$process.ExitCode;expected=$case.expected;passed=$ok;goal=$goal;log=[IO.Path]::GetFileName($log)}
    Write-Output "$($case.id): exit=$($process.ExitCode), passou=$ok"
    $process.Dispose()
    $psi.EnvironmentVariables.Remove('WMS_DB_BOOTSTRAP_PASSWORD')
    $passwordFixture=$null
}
$results | ConvertTo-Json -Depth 4 | Set-Content (Join-Path $PSScriptRoot 'd21-maven-resultados.json') -Encoding UTF8
if (@($results | Where-Object {-not $_.passed}).Count -ne 0) { throw 'Fixture Maven D21 falhou; conferir evidencias offline.' }
