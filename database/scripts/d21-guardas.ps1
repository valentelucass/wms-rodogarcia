# D21: guardas locais. Nao conecta nem escolhe versoes pendentes (responsabilidade Flyway).
. (Join-Path $PSScriptRoot 'd20-guardas.ps1')
function Assert-WmsD21Banco([string]$Banco){if($Banco -cnotin @('WMS_DEV','WMS_PROD')){throw 'D21_BANCO_NAO_AUTORIZADO'}}
function Get-WmsD21InitSql([string]$Banco,[string]$Servidor){
    Assert-WmsD21Banco $Banco
    if([string]::IsNullOrWhiteSpace($Servidor) -or $Servidor.Length -gt 128 -or $Servidor -match '[\x00-\x1f]'){throw 'D21_SERVIDOR_INVALIDO'}
    $s=$Servidor.Replace("'","''")
    (Get-WmsD20SessaoSql)+" IF DB_NAME() IS NULL OR DB_NAME() COLLATE Latin1_General_100_BIN2 <> N'$Banco' OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) IS NULL OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) COLLATE Latin1_General_100_BIN2 <> N'$s' OR ORIGINAL_LOGIN() IS NULL OR ORIGINAL_LOGIN() COLLATE Latin1_General_100_BIN2 <> N'sa' THROW 51031,'D21 identidade administrativa divergente.',1;"
}
function Get-WmsD21Fontes([string]$DatabaseRoot){
    # O freeze das nove fontes antigas permanece, sem rejeitar V10+.
    $raiz=[IO.Path]::GetFullPath((Join-Path $DatabaseRoot '..'))
    $base=Get-Content -LiteralPath (Join-Path $DatabaseRoot 'evidencias/d20-baseline.json') -Raw -Encoding UTF8|ConvertFrom-Json
    foreach($f in $base.migrations){if((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -cne $f.sha256){throw 'D21_FONTE_CONGELADA_DIVERGENTE'}}
    $arquivos=@(Get-ChildItem -LiteralPath (Join-Path $DatabaseRoot 'migrations') -File -Recurse -Filter '*.sql'|Sort-Object FullName)
    if(-not $arquivos.Count){throw 'D21_MIGRATIONS_AUSENTES'}
    @($arquivos|ForEach-Object {[pscustomobject]@{arquivo=$_.FullName.Substring($DatabaseRoot.Length+1).Replace('\','/');sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}})
}
function Assert-WmsD21FontesIguais($Antes,$Depois){
    if((@($Antes|ForEach-Object {$_.arquivo+':'+$_.sha256}) -join '|') -cne (@($Depois|ForEach-Object {$_.arquivo+':'+$_.sha256}) -join '|')){throw 'D21_FONTES_MUDARAM_DURANTE_EXECUCAO'}
}
function Assert-WmsD21Ambiente([Collections.IDictionary]$Vars){
    Assert-WmsD20Overrides $Vars
    foreach($n in $Vars.Keys){
        if([string]$n -imatch '^WMS_DB_BOOTSTRAP_' -or ([string]$n -iin @('MAVEN_ARGS','MAVEN_OPTS','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS','MVNW_REPOURL','MVNW_USERNAME','MVNW_PASSWORD') -and -not [string]::IsNullOrWhiteSpace([string]$Vars[$n]))){throw 'D21_CONFIG_IMPLICITA_RECUSADA'}
    }
}
function Assert-WmsD21Schema($Estado,[string]$Banco){
    Assert-WmsD21Banco $Banco
    foreach($n in @('banco','metadataCompleta','schemaWms','tabelas','colunas','historico','falhasHistorico','restricoesInvalidas','indicesDesabilitados')){
        if($null -eq $Estado -or $null -eq $Estado.PSObject.Properties[$n] -or $null -eq $Estado.$n){throw 'D21_SCHEMA_METADADOS_INSUFICIENTES'}
    }
    if($Estado.banco -cne $Banco -or $Estado.metadataCompleta -ne 1 -or $Estado.schemaWms -ne 1 -or $Estado.tabelas -lt 1 -or $Estado.colunas -lt 1 -or $Estado.historico -lt 1 -or $Estado.falhasHistorico -ne 0 -or $Estado.restricoesInvalidas -ne 0 -or $Estado.indicesDesabilitados -ne 0){throw 'D21_SCHEMA_REAL_NAO_CONFIRMADO'}
}
function Assert-WmsD21Pom([string]$Path){
    [xml]$pom=Get-Content -LiteralPath $Path -Raw -Encoding UTF8
    $p=$pom.SelectSingleNode('//*[local-name()="profile"][*[local-name()="id"]="bootstrap-local"]')
    if($null -eq $p){throw 'D21_PROFILE_CEDRO_AUSENTE'}
    $c=$p.SelectSingleNode('.//*[local-name()="plugin"][*[local-name()="artifactId"]="flyway-maven-plugin"]/*[local-name()="configuration"]')
    if($null -eq $c){throw 'D21_PROFILE_FLYWAY_AUSENTE'}
    $esperado=@{url='jdbc:sqlserver://127.0.0.1:1433;databaseName=${env.WMS_DB_BOOTSTRAP_NAME};encrypt=true;trustServerCertificate=false;trustStore=${env.WMS_DB_BOOTSTRAP_TRUSTSTORE};trustStorePassword=projetos-public-cert;hostNameInCertificate=${env.WMS_DB_BOOTSTRAP_CERTIFICATE_HOST}';user='${env.WMS_DB_BOOTSTRAP_USER}';password='${env.WMS_DB_BOOTSTRAP_PASSWORD}';defaultSchema='wms';cleanDisabled='true';baselineOnMigrate='false';validateOnMigrate='true';outOfOrder='false';validateMigrationNaming='true'}
    foreach($n in $esperado.Keys){$v=$c.SelectSingleNode('*[local-name()="'+$n+'"]');if($null -eq $v -or $v.InnerText -cne $esperado[$n]){throw 'D21_POM_CONTRATO_DIVERGENTE'}}
    if($c.SelectSingleNode('*[local-name()="locations"]/*[local-name()="location"]').InnerText -cne 'filesystem:${project.basedir}/../database/migrations' -or $c.SelectSingleNode('*[local-name()="schemas"]/*[local-name()="schema"]').InnerText -cne 'wms' -or $c.SelectSingleNode('*[local-name()="placeholders"]/*[local-name()="wmsDatabase"]').InnerText -cne '${env.WMS_DB_BOOTSTRAP_NAME}'){throw 'D21_FONTE_OU_PLACEHOLDER_DIVERGENTE'}
    # SQL default recusa goal direto sem marcador; ativacao libera initSql apenas no filho.
    $txt=$pom.OuterXml
    if(-not $txt.Contains('D21_LOCAL_MANUAL_CONFIRMADO') -or -not $txt.Contains('${env.WMS_DB_BOOTSTRAP_INIT_SQL}') -or $c.SelectSingleNode('*[local-name()="initSql"]').InnerText -notmatch '^\$\{wms\..+\}$'){throw 'D21_INITSQL_SEM_GUARDA'}
    $chave=$c.SelectSingleNode('*[local-name()="initSql"]').InnerText.Trim('$','{','}')
    $def=$pom.SelectSingleNode('/*[local-name()="project"]/*[local-name()="properties"]/*[local-name()="'+$chave+'"]')
    if($null -eq $def -or $def.InnerText -notmatch '^THROW '){throw 'D21_GOAL_DIRETO_SEM_RECUSA_DEFAULT'}
}
