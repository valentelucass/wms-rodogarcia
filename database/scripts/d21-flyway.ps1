[CmdletBinding()]
param([ValidateSet('Plan')][string]$Action='Plan')
$WmsD21Database=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $PSScriptRoot 'd21-guardas.ps1')
. (Join-Path $PSScriptRoot 'd21-catalogo.ps1')
. (Join-Path $PSScriptRoot 'd24-runtime.ps1')

function Assert-WmsD21Ferramentas([string]$DatabaseRoot){
    $backend=[IO.Path]::GetFullPath((Join-Path $DatabaseRoot '../backend'))
    Assert-WmsD21Pom (Join-Path $backend 'pom.xml')
    Assert-WmsD21Ambiente ([Environment]::GetEnvironmentVariables())
    foreach($p in @('flyway.conf','.mvn/maven.config','.mvn/jvm.config','.mvn/extensions.xml')){if(Test-Path -LiteralPath (Join-Path $backend $p)){throw 'D21_ARQUIVO_CONFIG_ALTERNATIVO_RECUSADO'}}
    if(Test-Path -LiteralPath (Join-Path $DatabaseRoot '../flyway.conf')){throw 'D21_ARQUIVO_CONFIG_ALTERNATIVO_RECUSADO'}
    if([string]::IsNullOrWhiteSpace($env:JAVA_HOME) -or -not (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin/java.exe'))){throw 'D21_JAVA_EXISTENTE_NECESSARIO'}
    # Impede o wrapper de baixar/instalar distribuicao. Maven usa -o e settings vazios.
    $props=Get-Content -LiteralPath (Join-Path $backend '.mvn/wrapper/maven-wrapper.properties') -Raw -Encoding UTF8|ConvertFrom-StringData
    $url=$props.distributionUrl
    if($url -notmatch '^https://repo\.maven\.apache\.org/maven2/org/apache/maven/apache-maven/[0-9.]+/apache-maven-[0-9.]+-bin\.zip$'){throw 'D21_DISTRIBUICAO_MAVEN_NAO_RECONHECIDA'}
    $nome=($url -replace '^.*/','') -replace '-bin\.zip$',''
    $hash=([Security.Cryptography.SHA256]::Create().ComputeHash([byte[]][char[]]$url)|ForEach-Object {$_.ToString('x2')}) -join ''
    $cache=Join-Path $env:USERPROFILE ('.m2/wrapper/dists/'+$nome+'/'+$hash+'/bin/mvn.cmd')
    if(-not (Test-Path -LiteralPath $cache)){throw 'D21_CACHE_MAVEN_EXISTENTE_NECESSARIO_SEM_INSTALAR'}
}
function New-WmsD21StartInfo([string]$DatabaseRoot,[string]$Banco,[string]$Servidor,[ValidateSet('PreValidate','Migrate','PostValidate','Info')][string]$Etapa){
    Assert-WmsD21Banco $Banco
    $backend=[IO.Path]::GetFullPath((Join-Path $DatabaseRoot '../backend'))
    $settings=[IO.Path]::GetFullPath((Join-Path $DatabaseRoot 'config/flyway-settings-vazias.xml'))
    $flywayConfig=[IO.Path]::GetFullPath((Join-Path $DatabaseRoot 'config/flyway-vazio.conf'))
    if(-not (Test-Path -LiteralPath $flywayConfig -PathType Leaf) -or (Get-Item -LiteralPath $flywayConfig).Length -ne 0){throw 'D24_FLYWAY_CONFIG_VAZIA_NECESSARIA'}
    $goal=if($Etapa -eq 'Migrate'){'migrate'}elseif($Etapa -eq 'Info'){'info'}else{'validate'}
    $ignorar=if($Etapa -eq 'PreValidate'){'*:pending'}else{''}
    $psi=New-Object Diagnostics.ProcessStartInfo
    $psi.FileName=Join-Path $env:SystemRoot 'System32/cmd.exe'
    $psi.Arguments='/d /s /c ""'+(Join-Path $backend 'mvnw.cmd')+'" -B -ntp -o -s "'+$settings+'" -gs "'+$settings+'" -Pbootstrap-local -Dwms.bootstrap.skip=false "-Dflyway.configFiles='+$flywayConfig+'" -Dflyway.ignoreMigrationPatterns='+$ignorar+' validate flyway:'+$goal+'"'
    $psi.WorkingDirectory=$backend;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true
    $psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
    $psi.EnvironmentVariables.Clear()
    foreach($n in @('SystemRoot','WINDIR','ComSpec','PATH','PATHEXT','TEMP','TMP','USERPROFILE','HOMEDRIVE','HOMEPATH','APPDATA','LOCALAPPDATA','PROGRAMDATA','JAVA_HOME')){
        $v=[Environment]::GetEnvironmentVariable($n,'Process');if($null -ne $v){$psi.EnvironmentVariables[$n]=$v}
    }
    $psi.EnvironmentVariables['MAVEN_SKIP_RC']='true'
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_GUARD']='D21_LOCAL_MANUAL_CONFIRMADO'
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_HOST']='127.0.0.1'
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_PORT']='1433'
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_NAME']=$Banco
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_USER']='sa'
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_SERVER']=$Servidor
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_INIT_SQL']=Get-WmsD21InitSql $Banco $Servidor
    $psi
}
function Invoke-WmsD21Processo([Diagnostics.ProcessStartInfo]$Info,[Security.SecureString]$Senha,[int]$TimeoutMs=300000){
    $bstr=[IntPtr]::Zero;$p=$null;$out=$null;$err=$null;$texto=$null;$iniciado=$false
    $script:WmsD21UltimoPid=$null
    try{
        if($null -eq $Senha -or $Senha.Length -eq 0){throw 'D21_SENHA_OCULTA_AUSENTE'}
        $bstr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($Senha)
        $Info.EnvironmentVariables['WMS_DB_BOOTSTRAP_PASSWORD']=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
        $p=New-Object Diagnostics.Process;$p.StartInfo=$Info
        $iniciado=$p.Start();if(-not $iniciado){throw 'D21_FILHO_NAO_INICIADO'}
        $script:WmsD21UltimoPid=$p.Id
        $out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync()
        if(-not $p.WaitForExit($TimeoutMs)){throw 'D21_FLYWAY_TIMEOUT'}
        $p.WaitForExit();$exit=$p.ExitCode
        $texto=$out.Result+"`n"+$err.Result
        $motivo=if($exit -eq 0){'GOAL_OK'}elseif($texto -match '(?i)checksum|validation failed|validate failed|migration.*(missing|failed)'){'HISTORICO_CHECKSUM_OU_MIGRATION'}elseif($texto -match '(?i)SSL|TLS|PKIX|certificate'){'TLS_NAO_VALIDADO'}elseif($texto -match '(?i)connection|login failed|socket|timeout'){'CONEXAO_INTERROMPIDA_OU_RECUSADA'}else{'FERRAMENTA_OU_MIGRATION_FALHOU'}
        # Nunca propagar stdout/stderr bruto, URL, SQL ou valores do ambiente.
        [pscustomobject]@{exitCode=$exit;resultado=$motivo;saidaBrutaDescartada=$true;pid=$p.Id;processoEncerrado=$p.HasExited}
    }finally{
        if($null -ne $p){
            if($iniciado -and -not $p.HasExited){$null=& (Join-Path $env:SystemRoot 'System32/taskkill.exe') /PID $p.Id /T /F 2>&1}
            $p.Dispose()
        }
        $Info.EnvironmentVariables.Remove('WMS_DB_BOOTSTRAP_PASSWORD');$Info.EnvironmentVariables.Clear()
        if($bstr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)}
        $texto=$null;$out=$null;$err=$null
    }
}
function Invoke-WmsD21Flyway([string]$Banco,[string]$Servidor,[Security.SecureString]$Senha,[string]$Etapa){
    $psi=New-WmsD21StartInfo $WmsD21Database $Banco $Servidor $Etapa
    $profile=Get-ProjetosSqlProfile
    if($profile.server -cne $Servidor){throw 'D24_RUNTIME_SERVIDOR_DIVERGENTE'}
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_TRUSTSTORE']=$profile.truststore
    $psi.EnvironmentVariables['WMS_DB_BOOTSTRAP_CERTIFICATE_HOST']=$profile.certificateHost
    Invoke-WmsD21Processo $psi $Senha
}
function Read-WmsD21Schema([string]$Banco,[string]$Servidor,[Security.SecureString]$Senha){
    Assert-WmsD21Banco $Banco
    $cred=Get-WmsD20CredencialCriacao -SenhaLocal $Senha
    $c=New-WmsRuntimeConnection $Banco $cred 'WMS-D24-SCHEMA-LEITURA'
    try{
        $c.Open();$cmd=$c.CreateCommand();$cmd.CommandTimeout=30
        $cmd.CommandText=(Get-WmsD21InitSql $Banco $Servidor)+[IO.File]::ReadAllText((Join-Path $WmsD21Database 'validacao/d21-schema-real.sql'))
        $r=$cmd.ExecuteReader();try{
            if(-not $r.Read()){throw 'D21_SCHEMA_METADADOS_INSUFICIENTES'}
            $val=@{};for($i=0;$i -lt $r.FieldCount;$i++){$val[$r.GetName($i)]=$r.GetValue($i)}
            $estado=[pscustomobject]$val;Assert-WmsD21Schema $estado $Banco
            if(-not $r.NextResult()){throw 'D21_CATALOGO_METADADOS_AUSENTES'}
            $itens=New-Object 'Collections.Generic.List[object]'
            while($r.Read()){
                for($i=0;$i -lt 4;$i++){if($r.IsDBNull($i)){throw 'D21_CATALOGO_METADADOS_AUSENTES'}}
                $itens.Add([pscustomobject]@{categoria=$r.GetString(0);tabela=$r.GetString(1);nome=$r.GetString(2);valor=$r.GetString(3)})
            }
            $estado|Add-Member catalogo @(Convert-WmsD21CatalogoReal $itens.ToArray())
            $estado
        }finally{$r.Close()}
    }catch{throw 'D21_SCHEMA_REAL_LEITURA_FALHOU'}finally{$c.Dispose();$cred.Password.Dispose()}
}
if($MyInvocation.InvocationName -ne '.'){
    [pscustomobject]@{natureza='PLANO_D21_OFFLINE_SEM_AMBIENTE_SEM_SQL';bancos=@('WMS_DEV','WMS_PROD');perfil='bootstrap-local';fonte='filesystem:database/migrations';ordem=@('PreValidate','Migrate','PostValidate','Info','SchemaReal');segredo='SOMENTE_ENV_FILHO_EFEMERO';sqlExecutado=$false}|ConvertTo-Json
}
