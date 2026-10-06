# Funcoes puras: nao le ambiente, nao conecta, nao executa SQL/Flyway.
function Assert-WmsD20Alvo {
    param([string]$HostSql, [string]$Porta, [string]$Banco,
          [string]$AlvoConfirmado, [string]$ServidorConfirmado,
          [ValidateSet('PlanejarCriacao','CriarDev','CriarProd','MigrarDev','DiagnosticarLocal')][string]$Acao)
    if ($HostSql -notmatch '^[A-Za-z0-9.-]+$' -or $HostSql.StartsWith('-') -or
        $Porta -notmatch '^\d{1,5}$' -or [int]$Porta -lt 1 -or [int]$Porta -gt 65535) {
        throw 'D20_FORMATO_ALVO_INVALIDO'
    }
    if ($Banco -cnotin @('WMS_DEV','WMS_PROD')) { throw 'D20_BANCO_NAO_AUTORIZADO' }
    if ($Acao -cin @('MigrarDev','DiagnosticarLocal','CriarDev') -and $Banco -cne 'WMS_DEV') { throw 'D20_PROD_MIGRATION_CARGA_RECUSADA' }
    if ($Acao -ceq 'CriarProd' -and $Banco -cne 'WMS_PROD') { throw 'D20_CRIACAO_ACAO_DIVERGENTE' }
    if ($Acao -eq 'DiagnosticarLocal' -and $HostSql -cnotin @('127.0.0.1','localhost')) {
        throw 'D20_ENSAIO_EXIGE_LOOPBACK'
    }
    if ($AlvoConfirmado -cne ('{0}:{1}/{2}' -f $HostSql,$Porta,$Banco) -or
        [string]::IsNullOrWhiteSpace($ServidorConfirmado)) { throw 'D20_ALVO_NAO_CONFIRMADO' }
}

function Assert-WmsD20Identidade {
    param([string]$ServidorReal,[string]$BancoReal,[string]$ServidorEsperado,[string]$BancoEsperado)
    if ($ServidorReal -cne $ServidorEsperado -or $BancoReal -cne $BancoEsperado) {
        throw 'D20_IDENTIDADE_DIVERGENTE'
    }
}

function Assert-WmsD20Overrides {
    param([System.Collections.IDictionary]$Variaveis)
    foreach ($nome in $Variaveis.Keys) {
        if ([string]$nome -imatch '^FLYWAY_') { throw 'D20_OVERRIDE_FLYWAY_RECUSADO' }
        if([string]$nome -ieq 'MAVEN_ARGS' -and -not [string]::IsNullOrWhiteSpace([string]$Variaveis[$nome])){throw 'D20_OVERRIDE_MAVEN_RECUSADO'}
        if ([string]$nome -iin @('MAVEN_ARGS','MAVEN_OPTS','JAVA_TOOL_OPTIONS','JDK_JAVA_OPTIONS','_JAVA_OPTIONS')) {
            if ([string]$Variaveis[$nome] -imatch '(?:-D\s*(?:flyway\.|wms\.migrations\.|env\.WMS_DB_|maven\.ext\.)|--(?:file|settings)|(?:^|\s)-[fs](?:\s|$))') {
                throw 'D20_OVERRIDE_MAVEN_RECUSADO'
            }
        }
    }
}

function Assert-WmsD20Freeze([string]$DatabaseRoot){
    $b=Get-Content -LiteralPath (Join-Path $DatabaseRoot 'evidencias/d20-baseline.json') -Raw -Encoding UTF8|ConvertFrom-Json
    $arquivos=@(Get-ChildItem -LiteralPath (Join-Path $DatabaseRoot 'migrations') -Filter '*.sql' -File -Recurse)
    if($arquivos.Count -ne 9 -or @($b.migrations).Count -ne 9){throw 'D20_FREEZE_CONJUNTO_MIGRATIONS_DIVERGENTE'}
    foreach($f in $b.migrations){
        $p=Join-Path $DatabaseRoot ('migrations/'+[IO.Path]::GetFileName($f.arquivo))
        if(-not (Test-Path -LiteralPath $p) -or (Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash -cne $f.sha256){throw 'D20_FREEZE_MIGRATION_DIVERGENTE'}
    }
}

function Get-WmsD20PlanoCriacao {
    param([string]$Banco,[bool]$ExisteBanco,[bool]$DevComprovado)
    if ($Banco -cnotin @('WMS_DEV','WMS_PROD')) { throw 'D20_BANCO_NAO_AUTORIZADO' }
    [pscustomobject]@{
        banco=$Banco
        ordem=if ($Banco -ceq 'WMS_DEV') {1} else {2}
        acao=if($ExisteBanco){'PRESERVAR_E_CONFERIR_EXISTENTE'}elseif ($Banco -ceq 'WMS_DEV') {'CREATE_DEV_APOS_ALVO'} else {'CREATE_PROD_VAZIO_APOS_DEV_E_ALVO'}
        devComprovado=$DevComprovado
        permiteExecutar=$false
        depende=if ($Banco -ceq 'WMS_PROD' -and -not $DevComprovado) {'DEV_PRIMEIRO'} else {'ALVO_ACESSO'}
        sobrescreve=$false
        alteraConfiguracaoExistente=$false
    }
}

function Assert-WmsD20Criacao {
    param([string]$Banco,[bool]$ExisteBanco,[bool]$DevComprovado)
    $null=Get-WmsD20PlanoCriacao $Banco $ExisteBanco $DevComprovado
    if($ExisteBanco){throw 'D20_BANCO_EXISTENTE_NAO_SOBRESCREVER'}
    if ($Banco -ceq 'WMS_PROD' -and -not $DevComprovado) { throw 'D20_DEV_PRIMEIRO' }
}

function Assert-WmsD20BancoExistente {
    param([string]$NomeReal,[string]$BancoEsperado,[bool]$Online)
    if($BancoEsperado -cnotin @('WMS_DEV','WMS_PROD') -or $NomeReal -cne $BancoEsperado -or -not $Online){throw 'D20_EXISTENTE_NOME_OU_ESTADO_DIVERGENTE'}
}

function Get-WmsD20AcaoObservada {
    param([ValidateSet('Create','Check')][string]$Solicitada,[bool]$ExisteAgora)
    if($ExisteAgora){return 'Check'}
    if($Solicitada -eq 'Check'){throw 'D20_EXISTENTE_NAO_ENCONTRADO'}
    return 'Create'
}

function Get-WmsD20SessaoSql {
    'SET ANSI_NULLS ON; SET ANSI_PADDING ON; SET ANSI_WARNINGS ON; SET ARITHABORT ON; SET CONCAT_NULL_YIELDS_NULL ON; SET QUOTED_IDENTIFIER ON; SET NUMERIC_ROUNDABORT OFF;'
}

function Get-WmsD20FlywayInitSql {
    param([string]$ServidorConfirmado,[string]$UsuarioMigration)
    if([string]::IsNullOrWhiteSpace($ServidorConfirmado) -or [string]::IsNullOrWhiteSpace($UsuarioMigration) -or
        $ServidorConfirmado.Length -gt 128 -or $UsuarioMigration.Length -gt 128 -or
        $ServidorConfirmado -match '[\x00-\x1f]' -or $UsuarioMigration -match '[\x00-\x1f]'){throw 'D20_IDENTIDADE_INITSQL_INVALIDA'}
    $srv=$ServidorConfirmado.Replace("'","''");$usr=$UsuarioMigration.Replace("'","''")
    (Get-WmsD20SessaoSql)+" IF DB_NAME() COLLATE Latin1_General_100_BIN2 <> N'WMS_DEV' OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) IS NULL OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) COLLATE Latin1_General_100_BIN2 <> N'$srv' OR ORIGINAL_LOGIN() COLLATE Latin1_General_100_BIN2 <> N'$usr' THROW 51025,'D20 identidade Flyway divergente.',1; IF COALESCE(IS_SRVROLEMEMBER(N'sysadmin'),1)<>0 OR COALESCE(IS_ROLEMEMBER(N'db_owner'),1)<>0 OR COALESCE(IS_ROLEMEMBER(N'db_securityadmin'),1)<>0 OR COALESCE(IS_ROLEMEMBER(N'db_accessadmin'),1)<>0 OR COALESCE(IS_ROLEMEMBER(N'db_ddladmin'),1)<>0 OR COALESCE(HAS_PERMS_BY_NAME(NULL,N'SERVER',N'CONTROL SERVER'),1)<>0 THROW 51026,'D20 migration exige identidade restrita.',1;"
}

function Assert-WmsD20Credenciais {
    param([string]$UsuarioApp,[string]$UsuarioMigration)
    if($UsuarioApp -ieq 'sa' -or $UsuarioMigration -ieq 'sa'){throw 'D20_SA_RESERVADO_CRIACAO'}
    if([string]::IsNullOrWhiteSpace($UsuarioApp) -or [string]::IsNullOrWhiteSpace($UsuarioMigration) -or
        $UsuarioApp -ieq $UsuarioMigration){throw 'D20_IDENTIDADES_TECNICAS_NAO_SEPARADAS'}
}

function Assert-WmsD20MetadadosExistente {
    param($Estado,[string]$BancoEsperado)
    foreach($nome in @('banco','metadataCompleta','objetos','tipos','schemas','principais','membros','permissoes')){
        if($null -eq $Estado -or $null -eq $Estado.PSObject.Properties[$nome] -or $null -eq $Estado.$nome){throw 'D20_METADADOS_INSUFICIENTES'}
    }
    if($Estado.banco -cne $BancoEsperado){throw 'D20_VAZIO_BANCO_DIVERGENTE'}
    if($Estado.metadataCompleta -ne 1){throw 'D20_METADADOS_INSUFICIENTES'}
}

function Assert-WmsD20Vazio {
    param($Estado,[string]$BancoEsperado)
    Assert-WmsD20MetadadosExistente $Estado $BancoEsperado
    foreach($nome in @('objetos','tipos','schemas','principais','membros','permissoes')){
        if($Estado.$nome -ne 0){throw 'D20_CONTEUDO_OU_ACESSO_INESPERADO'}
    }
}

# Entrada local em memoria, sem texto simples/ambiente/cofre de outro projeto.
function Get-WmsD20CredencialCriacao {
    param([Management.Automation.PSCredential]$CredencialLocal,[Security.SecureString]$SenhaLocal)
    if($null -ne $CredencialLocal -and $null -ne $SenhaLocal){throw 'D20_CREDENCIAL_ENTRADAS_CONFLITANTES'}
    if($null -ne $CredencialLocal){
        if($CredencialLocal.UserName -cne 'sa'){throw 'D20_CREDENCIAL_CRIACAO_IDENTIDADE_DIVERGENTE'}
        $SenhaLocal=$CredencialLocal.Password
    }
    if($null -eq $SenhaLocal -or $SenhaLocal.Length -eq 0){throw 'D20_CREDENCIAL_CRIACAO_PROTEGIDA_AUSENTE_SEM_CONEXAO'}
    $copia=$SenhaLocal.Copy();$copia.MakeReadOnly()
    try { New-Object System.Data.SqlClient.SqlCredential('sa',$copia) }
    catch { $copia.Dispose();throw 'D20_CREDENCIAL_LOCAL_INVALIDA_SEM_CONEXAO' }
}
