[CmdletBinding()]
param([ValidateSet('Plan','Info','Validate','Migrate')][string]$Action='Plan')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'd20-guardas.ps1')
if($Action -eq 'Plan'){
    [pscustomobject]@{natureza='PLANO_OFFLINE_SEM_AMBIENTE_SEM_CONEXAO';banco='WMS_DEV';prod='MIGRATIONS_RECUSADAS';
        criacao='SEPARADA_EM_criar-bancos.ps1';identidade='MIGRATION_DISTINTA_APP';
        acoes='Info/Validate/Migrate exigem alvo/acesso confirmados'}|ConvertTo-Json;return
}
# Nenhum valor ou segredo e impresso. Nao usar variaveis de outro terminal.
Assert-WmsD20Overrides ([Environment]::GetEnvironmentVariables())
Assert-WmsD20Freeze $PSScriptRoot
foreach($n in @('WMS_DB_MIGRATION_GUARD','WMS_DB_MIGRATION_INIT_SQL')){
    if($null -ne [Environment]::GetEnvironmentVariable($n)){throw 'D20_MARCADOR_OU_INITSQL_PREEXISTENTE'}
}
$required=@('WMS_DB_HOST','WMS_DB_PORT','WMS_DB_NAME','WMS_DB_USER','WMS_DB_MIGRATION_USER',
    'WMS_DB_MIGRATION_PASSWORD','WMS_DB_CONFIRMED_TARGET','WMS_DB_CONFIRMED_SERVER')
foreach($name in $required){if([string]::IsNullOrWhiteSpace([Environment]::GetEnvironmentVariable($name))){throw "D20_VARIAVEL_AUSENTE_$name"}}
Assert-WmsD20Alvo $env:WMS_DB_HOST $env:WMS_DB_PORT $env:WMS_DB_NAME $env:WMS_DB_CONFIRMED_TARGET $env:WMS_DB_CONFIRMED_SERVER 'MigrarDev'
Assert-WmsD20Credenciais $env:WMS_DB_USER $env:WMS_DB_MIGRATION_USER
$initSql=Get-WmsD20FlywayInitSql $env:WMS_DB_CONFIRMED_SERVER $env:WMS_DB_MIGRATION_USER
$backendPath=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../backend'))
# Recusar fontes implicitas em arquivo sem abrir/levar seu conteudo a logs.
foreach($p in @((Join-Path $backendPath 'flyway.conf'),(Join-Path $PSScriptRoot '../flyway.conf'),
    (Join-Path $backendPath '.mvn/maven.config'),(Join-Path $backendPath '.mvn/jvm.config'))){
    if(Test-Path -LiteralPath $p){throw 'D20_ARQUIVO_CONFIG_ALTERNATIVO_RECUSADO'}
}
$builder=New-Object System.Data.SqlClient.SqlConnectionStringBuilder
$builder.DataSource="$($env:WMS_DB_HOST),$($env:WMS_DB_PORT)";$builder.InitialCatalog='WMS_DEV'
$builder.UserID=$env:WMS_DB_MIGRATION_USER;$builder.Password=$env:WMS_DB_MIGRATION_PASSWORD
$builder.Encrypt=$true;$builder.TrustServerCertificate=$false;$builder.ConnectTimeout=10
$builder.ApplicationName='WMS-D20-MIGRATION-PREFLIGHT'
$connection=New-Object System.Data.SqlClient.SqlConnection($builder.ConnectionString)
try{
    $connection.Open();$command=$connection.CreateCommand();$command.CommandTimeout=15
    $command.CommandText=(Get-WmsD20SessaoSql)+" SELECT CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')),DB_NAME(),ORIGINAL_LOGIN()"
    $r=$command.ExecuteReader();if(-not $r.Read()){throw 'D20_IDENTIDADE_AUSENTE'}
    $srv=$r.GetString(0);$db=$r.GetString(1);$login=$r.GetString(2);$r.Close()
    Assert-WmsD20Identidade $srv $db $env:WMS_DB_CONFIRMED_SERVER 'WMS_DEV'
    if($login -cne $env:WMS_DB_MIGRATION_USER){throw 'D20_IDENTIDADE_MIGRATION_DIVERGENTE'}
}catch{throw 'D20_PREFLIGHT_FALHOU_NENHUM_FLYWAY_SOLICITADO'}
finally{$connection.Dispose();$builder.Password=''}
# Marcadores transitorios do processo proprio; a guarda SQL vale em cada conexao.
try{
    [Environment]::SetEnvironmentVariable('WMS_DB_MIGRATION_GUARD','D20_WMS_DEV_VERIFICADO','Process')
    [Environment]::SetEnvironmentVariable('WMS_DB_MIGRATION_INIT_SQL',$initSql,'Process')
    Push-Location -LiteralPath $backendPath
    try{
        & ./mvnw.cmd -B -ntp -s ../database/flyway-settings-vazias.xml -gs ../database/flyway-settings-vazias.xml -Pmigrations '-Dflyway.configFiles=' '-Dwms.migrations.skip=false' validate "flyway:$($Action.ToLowerInvariant())"
        if($LASTEXITCODE -ne 0){throw 'D20_FLYWAY_FALHOU_CONFERIR_HISTORICO_SEM_REPETICAO'}
    }finally{Pop-Location}
}finally{
    [Environment]::SetEnvironmentVariable('WMS_DB_MIGRATION_GUARD',$null,'Process')
    [Environment]::SetEnvironmentVariable('WMS_DB_MIGRATION_INIT_SQL',$null,'Process')
}
