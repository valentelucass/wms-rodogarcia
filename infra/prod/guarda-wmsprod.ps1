# PROD-LAUNCH01. Guarda operacional somente leitura, nao teste de negocio. Uma conexao propria WMSPROD/WMS_PROD.
# Dot-source define Invoke-WmsProdGuard. Execucao direta retorna JSON e exit 0/20.
[CmdletBinding()]
param([string]$EvidencePath)
$script:WmsProdRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$script:WmsProdSource = $PSCommandPath

function Get-WmsProdMigrations {
    if (-not ('WmsProdChecksum' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text;
public static class WmsProdChecksum {
    public static int Read(string path) {
        uint crc = 0xffffffff;
        bool first = true;
        foreach (string original in File.ReadLines(path, Encoding.UTF8)) {
            string line = original;
            if (first && line.Length > 0 && line[0] == '\ufeff') line = line.Substring(1);
            first = false;
            foreach (byte b in Encoding.UTF8.GetBytes(line)) {
                crc ^= b;
                for (int i = 0; i < 8; i++) crc = (crc & 1) != 0 ? (crc >> 1) ^ 0xedb88320 : crc >> 1;
            }
        }
        return unchecked((int)(crc ^ 0xffffffff));
    }
}
'@
    }
    $common=@(Get-ChildItem -LiteralPath (Join-Path $script:WmsProdRoot 'database/migrations') -File -Filter 'V*.sql' | Where-Object {$_.Name -cnotmatch '^V10__'})
    $prod=Get-Item -LiteralPath (Join-Path $script:WmsProdRoot 'database/migrations-prod/V10__permitir_ajuste_estoque_producao.sql')
    @($common)+@($prod) |
        ForEach-Object {
            if ($_.Name -cnotmatch '^V([1-9][0-9]*)__.+\.sql$') { throw 'PROD_MIGRATION_NOME_INVALIDO' }
            [pscustomobject]@{version=$Matches[1];script=$_.Name;checksum=[WmsProdChecksum]::Read($_.FullName);sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
        } | Sort-Object {[int]$_.version}
}

function Invoke-WmsProdGuard {
    [CmdletBinding()]
    param([switch]$CadastroPreGrant)
    $ErrorActionPreference = 'Stop'
    $waitPolicy = [pscustomobject]@{ConnectTimeoutSeconds=120;CommandTimeoutSeconds=30;GuardTimeoutSeconds=240;ProgressIntervalSeconds=10}
    $connectTimeoutSeconds = $waitPolicy.ConnectTimeoutSeconds
    $commandTimeoutSeconds = $waitPolicy.CommandTimeoutSeconds
    $credential=$null; $connection=$null; $certificate=$null; $builder=$null; $openWatch=$null
    $phase='PREPARO_LOCAL'; $identityConfirmed=$false
    $checks=New-Object 'Collections.Generic.List[object]'
    $queries=New-Object 'Collections.Generic.List[object]'
    $report=[ordered]@{
        demanda='PROD-LAUNCH01';agente='WMS - Prumo'
        inicioUtc=[DateTime]::UtcNow.ToString('o');pidGuarda=$PID;estado='GUARDA_EM_PREPARO';guardaRealAprovada=$false;motivo=$null
        aberturaTentativas=0;aberturaConcluida=$false;aberturaElapsedMs=$null
        identidadeConfirmada=$false;alvo=$null;tls=$null;direitos=$null;catalogo=$null;historico=$null
        queriesGuarda=0;queriesMetadados=0;queriesNegocio=0;erro=$null
        politica=[ordered]@{alvo='tcp:127.0.0.1,1433';banco='WMS_PROD';login='WMSPROD';connectTimeout=$connectTimeoutSeconds;commandTimeout=$commandTimeoutSeconds;guardTimeout=$waitPolicy.GuardTimeoutSeconds;pooling=$false;connectRetryCount=0;fallback=$false}
        operacaoSQLImplementada=$true
        limites=[ordered]@{API=0;DML=0;DDL=0;DEV=0;TESTE_BANCO=0;admin=0;grants=0;sharedRuntimeAlterado=$false;servidorAlterado=$false;recapturaCertificado=$false;segredoPublicado=$false}
        limpeza=[ordered]@{conexaoDescartada=$true;secureStringDescartada=$true;certificadoDescartado=$true}
    }
    function Assert-Prod([string]$Name,[bool]$Ok) {
        $checks.Add([pscustomobject]@{criterio=$Name;passou=$Ok})
        if (-not $Ok) { throw 'PROD_GUARDA_RECUSADA' }
    }
    function Compare-ProdRows($A,$B,[string[]]$Fields) {
        $aa=@($A|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
        $bb=@($B|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
        return ($aa.Count -eq $bb.Count -and @(Compare-Object $aa $bb).Count -eq 0)
    }
    function Select-Prod([string]$Name,[string]$Sql,[switch]$Identity) {
        if (-not $Identity -and -not $identityConfirmed) { throw 'PROD_IDENTIDADE_NAO_CONFIRMADA' }
        $syntax=[regex]::Replace($Sql,"N?'(?:''|[^'])*'","''")
        if ($syntax.TrimStart() -cnotmatch '^SELECT\b' -or $syntax -match ';\s*\S|\b(INTO|INSERT|UPDATE|DELETE|MERGE|EXEC|ALTER|CREATE|DROP|GRANT|REVOKE|DENY|USE)\b|--|/\*') { throw 'PROD_SELECT_RECUSADO' }
        $cmd=$connection.CreateCommand();$reader=$null;$watch=[Diagnostics.Stopwatch]::StartNew()
        $cmd.CommandText=$Sql;$cmd.CommandTimeout=$commandTimeoutSeconds
        $entry=[ordered]@{nome=$Name;commandTimeoutSeconds=$commandTimeoutSeconds;concluida=$false;linhas=$null;elapsedMs=$null}
        $rows=New-Object 'Collections.Generic.List[object]'
        if ($Identity) {$report.queriesGuarda++} else {$report.queriesMetadados++}
        try {
            $reader=$cmd.ExecuteReader()
            while ($reader.Read()) {
                $row=[ordered]@{}
                for ($i=0;$i -lt $reader.FieldCount;$i++) {$row[$reader.GetName($i)]=if($reader.IsDBNull($i)){$null}else{$reader.GetValue($i)}}
                $rows.Add([pscustomobject]$row)
            }
            $entry.concluida=$true;$entry.linhas=$rows.Count
            $rows.ToArray()
        } finally {
            $watch.Stop();$entry.elapsedMs=$watch.Elapsed.TotalMilliseconds;$queries.Add([pscustomobject]$entry)
            if ($reader) {$reader.Dispose()};$cmd.Dispose()
        }
    }
    try {
        . (Join-Path $script:WmsProdRoot 'backend/scripts/prod/credential-prod.ps1')
        $baselinePath=Join-Path $script:WmsProdRoot 'infra/prod/contrato-aplicacao.json'
        $baseline=Get-Content -LiteralPath $baselinePath -Encoding UTF8 -Raw | ConvertFrom-Json
        Assert-Prod 'contrato esperado exclusivo PROD; nao e prova de ambiente' ($baseline.schema -eq 1 -and $baseline.database -ceq 'WMS_PROD' -and $baseline.login -ceq 'WMSPROD')
        $report.oraculo=[ordered]@{arquivo='infra/prod/contrato-aplicacao.json';sha256=(Get-FileHash -LiteralPath $baselinePath -Algorithm SHA256).Hash;uso='contrato esperado comparado com metadados atuais; nao transporta aceite DEV'}
        $migrations=@(Get-WmsProdMigrations)
        if (@($migrations | Where-Object {$_.version -ceq '12'}).Count -eq 1) {
            . (Join-Path $script:WmsProdRoot 'infra/tipos-unidade/contrato-banco.ps1')
            $baseline=Add-WmsUnitTypesExpectedContract $baseline ($migrations | Where-Object {$_.version -ceq '12'})
            $report.oraculo['tiposUnidadeSha256']=(Get-FileHash -LiteralPath (Join-Path $script:WmsProdRoot 'infra/tipos-unidade/contrato-banco.ps1') -Algorithm SHA256).Hash
        }
        if (@($migrations | Where-Object {$_.version -ceq '13'}).Count -eq 1) {
            . (Join-Path $script:WmsProdRoot 'infra/tabelas/contrato-banco.ps1')
            $baseline=Add-WmsBillingTableExpectedContract $baseline ($migrations | Where-Object {$_.version -ceq '13'})
        }
        Assert-Prod 'inventario local completo conforme contrato versionado' (Compare-ProdRows $migrations @($baseline.historico|Where-Object {$_.type -ceq 'SQL'}) @('version','script'))
        . (Join-Path $script:WmsProdRoot 'infra/cadastros/contrato-edicao.ps1')
        $cadastroBefore=$baseline
        $baseline=Add-WmsCadastroEditExpectedContract $baseline
        $report.oraculo['edicaoCadastrosSha256']=(Get-FileHash -LiteralPath (Join-Path $script:WmsProdRoot 'infra/cadastros/contrato-edicao.ps1') -Algorithm SHA256).Hash
        $report.migrationsLocais=$migrations
        $metadata=Get-WmsProductionCredentialMetadata
        Assert-Prod 'canal exclusivo WMSPROD existente, ACL e caminho protegidos' ($metadata.aclValida)
        $report.canal=[ordered]@{arquivo='%LOCALAPPDATA%/Rodogarcia/WMS/api-prod/wmsprod-app.clixml';sha256=$metadata.sha256;aclValida=$true;conteudoPublicado=$false;DPAPIImportada=$false}
        $modulePath=[IO.Path]::GetFullPath((Join-Path $script:WmsProdRoot '../.runtime/sql-server/SqlServerProjetos.psm1'))
        $module=Import-Module $modulePath -PassThru -Force
        # Somente loader com hashes. Profile/factory/update/admin NUNCA chamados.
        & $module {Import-SqlClient}
        $tlsRoot=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer'
        $tlsPath=Join-Path $tlsRoot 'tls.json'
        & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tlsPath
        $tls=Get-Content -LiteralPath $tlsPath -Encoding UTF8 -Raw|ConvertFrom-Json
        # O fallback TLS muda quando o processo SQL reinicia. A baseline D29
        # permanece imutavel para direitos/catalogo/historico, nao para rotacao TLS.
        $tlsIdentityBefore=& $module {Get-LocalIdentity}
        Assert-Prod 'recibo TLS privado pertence ao processo SQL local atual verificado' ($tls.pid -eq $tlsIdentityBefore.pid -and $tls.started -ceq $tlsIdentityBefore.started)
        Assert-Prod 'certificado publico existente no caminho protegido esperado' ($tls.id -cmatch '^[a-f0-9]{32}$' -and $tls.certificate -ceq (Join-Path $tlsRoot ('sql-public-'+$tls.id+'.pem')))
        & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tls.certificate
        $certHash=(Get-FileHash -LiteralPath $tls.certificate -Algorithm SHA256).Hash
        Assert-Prod 'certificado fixado corresponde ao recibo protegido do processo atual' ($certHash -ceq $tls.fileSHA256)
        $certificate=New-Object Security.Cryptography.X509Certificates.X509Certificate2($tls.certificate)
        $certHasher=[Security.Cryptography.SHA256]::Create()
        try {$certDerHash=([BitConverter]::ToString($certHasher.ComputeHash($certificate.RawData))).Replace('-','').ToLowerInvariant()} finally {$certHasher.Dispose()}
        Assert-Prod 'certificado dentro da validade e DER corresponde ao recibo atual' ([DateTime]::UtcNow -ge $certificate.NotBefore.ToUniversalTime() -and [DateTime]::UtcNow -lt $certificate.NotAfter.ToUniversalTime() -and $certDerHash -ceq $tls.certificateSHA256)
        Assert-Prod 'truststore existente sem regeneracao' ($tls.truststore -ceq ($tls.certificate -replace '\.pem$','.p12'))
        & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tls.truststore
        Assert-Prod 'hash truststore publico protegido' ((Get-FileHash -LiteralPath $tls.truststore -Algorithm SHA256).Hash -ceq $tls.truststoreSHA256)
        $credential=Get-WmsProductionApplicationCredential -ExpectedSha256 $metadata.sha256
        $report.canal.DPAPIImportada=$true
        Assert-Prod 'identidade da fonte propria WMSPROD' ($credential.UserName -ceq 'WMSPROD')
        $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
        $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_PROD')
        $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory)
        $builder.set_TrustServerCertificate($false);$builder.set_ServerCertificate($tls.certificate)
        $builder.set_ApplicationName('WMS-PROD-LAUNCH-GUARDA')
        $builder.set_Pooling($false);$builder.set_ConnectTimeout($connectTimeoutSeconds);$builder.set_ConnectRetryCount(0)
        $builder.set_TransparentNetworkIPResolution($false);$builder.set_MultiSubnetFailover($false)
        Assert-Prod 'sem senha/login em connectionstring; sem identidade Windows/fallback' ([string]::IsNullOrEmpty($builder.Password) -and [string]::IsNullOrEmpty($builder.UserID) -and -not $builder.IntegratedSecurity)
        $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSPROD',$credential.Password)
        $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
        $report.tls=[ordered]@{encrypt='Mandatory';trustServerCertificate=$false;certificateSha256=$certHash;thumbprint=$certificate.Thumbprint;notAfter=$certificate.NotAfter.ToUniversalTime().ToString('o');handshakeConcluido=$false;confirmacaoAtual=$false;truststoreSha256=$tls.truststoreSHA256}
        $report.tls.proveniencia=[ordered]@{reciboPID=$tls.pid;reciboInicioUtc=$tls.started;reciboAtualizadoUtc=$tls.updatedAt;certificateDerSha256=$certDerHash;processoLocalVerificadoAntes=$true;processoLocalVerificadoDepois=$false;baselineHistoricaTLSUsadaComoPinAtual=$false}
        $report.cliente=[ordered]@{assembly=[Microsoft.Data.SqlClient.SqlConnection].Assembly.GetName().Version.ToString();sha256=(Get-FileHash -LiteralPath ([Microsoft.Data.SqlClient.SqlConnection].Assembly.Location) -Algorithm SHA256).Hash;moduloSha256=(Get-FileHash -LiteralPath $modulePath -Algorithm SHA256).Hash}
        $phase='ABERTURA';$report.aberturaTentativas=1;$report.aberturaInicioUtc=[DateTime]::UtcNow.ToString('o')
        $openWatch=[Diagnostics.Stopwatch]::StartNew();$connection.Open();$openWatch.Stop()
        $report.aberturaConcluida=$true;$report.aberturaElapsedMs=$openWatch.Elapsed.TotalMilliseconds;$report.tls.handshakeConcluido=$true
        $tlsIdentityAfter=& $module {Get-LocalIdentity}
        Assert-Prod 'processo SQL local preservado durante abertura TLS antes de qualquer SELECT' ($tlsIdentityBefore.pid -eq $tlsIdentityAfter.pid -and $tlsIdentityBefore.started -ceq $tlsIdentityAfter.started)
        $report.tls.proveniencia.processoLocalVerificadoDepois=$true
        $phase='IDENTIDADE'
        $identity=@(Select-Prod 'DB_NAME_ORIGINAL_LOGIN_USER_NAME' "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,USER_NAME() usuario,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,CONVERT(nvarchar(128),SERVERPROPERTY('ProductVersion')) versao,@@SPID sessao,(SELECT state_desc FROM sys.databases WHERE database_id=DB_ID()) estado" -Identity)
        $report.alvo=$identity[0]
        Assert-Prod 'alvo REAL WMS_PROD, login/usuario WMSPROD, ONLINE e servidor autorizado' ($identity.Count -eq 1 -and $identity[0].banco -ceq 'WMS_PROD' -and $identity[0].login -ceq 'WMSPROD' -and $identity[0].usuario -ceq 'WMSPROD' -and $identity[0].estado -ceq 'ONLINE' -and -not [string]::IsNullOrWhiteSpace($identity[0].servidor))
        $identityConfirmed=$true;$report.identidadeConfirmada=$true;$report.tls.confirmacaoAtual=$true
        $phase='PERMISSOES'
        $rights=[ordered]@{}
        $rights.server=@(Select-Prod 'server_efetivo' "SELECT permission_name FROM sys.fn_my_permissions(NULL,N'SERVER') ORDER BY permission_name")
        $rights.database=@(Select-Prod 'database_efetivo' "SELECT permission_name FROM sys.fn_my_permissions(NULL,N'DATABASE') ORDER BY permission_name")
        $rights.roles=@(Select-Prod 'roles_database' 'SELECT r.name role FROM sys.database_role_members m JOIN sys.database_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=USER_ID()')
        $rights.serverRoles=@(Select-Prod 'roles_server' 'SELECT r.name role FROM sys.server_role_members m JOIN sys.server_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=SUSER_ID()')
        $rights.ownership=@(Select-Prod 'schemas_proprios' 'SELECT name FROM sys.schemas WHERE principal_id=USER_ID()')
        $rights.schema=@(Select-Prod 'schema_wms_efetivo' "SELECT permission_name FROM sys.fn_my_permissions(N'wms',N'SCHEMA')")
        $rights.explicit=@(Select-Prod 'direitos_explicitos' "SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID() ORDER BY p.class_desc,esquema,objeto,p.minor_id,p.permission_name")
        $rights.objects=@(Select-Prod 'objetos_wms_efetivo' "SELECT t.name tabela,p.permissao,HAS_PERMS_BY_NAME(N'wms.'+t.name,N'OBJECT',p.permissao) permitido FROM sys.tables t CROSS JOIN (VALUES(N'SELECT'),(N'INSERT'),(N'DELETE'),(N'ALTER'),(N'CONTROL'),(N'TAKE OWNERSHIP'),(N'VIEW CHANGE TRACKING'))p(permissao) WHERE t.schema_id=SCHEMA_ID(N'wms') ORDER BY t.name,p.permissao")
        $rights.columns=@(Select-Prod 'colunas_wms_update_efetivo' "SELECT t.name tabela,c.name coluna,HAS_PERMS_BY_NAME(N'wms.'+t.name,N'OBJECT',N'UPDATE',c.name,N'COLUMN') permitido FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') ORDER BY t.name,c.column_id")
        $report.direitos=$rights
        if ($CadastroPreGrant) {
            $alreadyCurrent=Compare-ProdRows $rights.explicit $baseline.rights.explicit @('class_desc','esquema','objeto','minor_id','coluna','permission_name','state_desc')
            $report.cadastroEdicao=[ordered]@{modo='PRE_GRANT';direitosAtuais=if($alreadyCurrent){'NOVOS'}else{'ANTERIORES'};liberaBackend=$false}
            if (-not $alreadyCurrent) {
                $legacy=Add-WmsCadastroEditExpectedContract $cadastroBefore -SomenteBase
                $isLegacy=Compare-ProdRows $rights.explicit $legacy.rights.explicit @('class_desc','esquema','objeto','minor_id','coluna','permission_name','state_desc')
                $baseline=if($isLegacy){$legacy}else{$cadastroBefore}
            }
        }
        Assert-Prod 'nenhuma role/ownership/schema amplo; server somente CONNECT SQL/VIEW ANY DATABASE; database CONNECT' ($rights.roles.Count -eq 0 -and $rights.serverRoles.Count -eq 0 -and $rights.ownership.Count -eq 0 -and $rights.schema.Count -eq 0 -and (Compare-ProdRows $rights.server @([pscustomobject]@{permission_name='CONNECT SQL'},[pscustomobject]@{permission_name='VIEW ANY DATABASE'}) @('permission_name')) -and (Compare-ProdRows $rights.database @([pscustomobject]@{permission_name='CONNECT'}) @('permission_name')))
        foreach ($kind in @('server','database','roles','serverRoles','ownership','schema','explicit','objects','columns')) {
            $fields=@($baseline.rights.$kind|Select-Object -First 1|ForEach-Object {$_.PSObject.Properties.Name})
            $equal=if($fields.Count){Compare-ProdRows $rights[$kind] $baseline.rights.$kind $fields}else{@($rights[$kind]).Count -eq 0}
            Assert-Prod ('permissoes atuais iguais restricao por objeto/coluna: '+$kind) $equal
        }
        $phase='CATALOGO_HISTORICO'
        $catalog=@(Select-Prod 'catalogo_integridade' "SELECT (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') tabelas,(SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') colunas,(SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) checksInvalidos,(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) fksInvalidas,(SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) indicesDesabilitados")
        $report.catalogo=$catalog[0]
        Assert-Prod 'catalogo e integridade conforme contrato versionado' (Compare-ProdRows $catalog @($baseline.catalogo) @('tabelas','colunas','checksInvalidos','fksInvalidas','indicesDesabilitados'))
        $history=@(Select-Prod 'historico_flyway' 'SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank')
        $report.historico=$history
        Assert-Prod 'historico atual preserva V1-V10 e extensoes versionadas sem divergencia' (Compare-ProdRows $history $baseline.historico @('installed_rank','version','type','script','checksum','success'))
        Assert-Prod 'historico SQL atual corresponde a todas fontes locais' (Compare-ProdRows @($history|Where-Object {$_.type -ceq 'SQL'}) $migrations @('version','script','checksum'))
        $report.perfilProtegido=[ordered]@{host='127.0.0.1';port=1433;database='WMS_PROD';login='WMSPROD';certificate=$tls.certificate;truststore=$tls.truststore;certificateHost=$tls.certificateHost;encrypt=$true;trustServerCertificate=$false}
        if ($CadastroPreGrant) {
            $report.estado='GUARDA_PRE_GRANT_CADASTRO_APROVADA';$report.guardaPermissoesCadastroAprovada=$true
        } else {
            $report.estado='GUARDA_ATUAL_APROVADA';$report.guardaRealAprovada=$true
        }
    } catch {
        $ex=$_.Exception;$code='PROD_FALHA';$types=New-Object 'Collections.Generic.List[string]'
        $number=$null;$native=$null;$markers=[ordered]@{prelogin=$false;handshake=$false;timeout=$false;autenticacao=$false;certificado=$false}
        while ($ex) {
            $types.Add($ex.GetType().FullName);$message=[string]$ex.Message
            if ($message -cmatch '^(PROD|SQL)_[A-Z0-9_]+$') {$code=$message}
            if ($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException') {$number=$ex.Number;$code='PROD_SQL_'+$number}
            if ($ex -is [ComponentModel.Win32Exception]) {$native=$ex.NativeErrorCode}
            if ($message -match '(?i)pre[- ]?login|pré[- ]?login') {$markers.prelogin=$true}
            if ($message -match '(?i)handshake|negocia') {$markers.handshake=$true}
            if ($message -match '(?i)timed?\s*out|timeout|tempo limite') {$markers.timeout=$true}
            if ($message -match '(?i)login failed|falha de logon') {$markers.autenticacao=$true}
            if ($message -match '(?i)certificate|certificado') {$markers.certificado=$true}
            $ex=$ex.InnerException
        }
        $report.guardaRealAprovada=$false;$report.estado='GUARDA_ATUAL_BLOQUEADA'
        $report.erro=[ordered]@{codigo=$code;fase=$phase;sqlNumber=$number;nativeErrorCode=$native;tipos=$types.ToArray();marcadores=$markers;mensagemBrutaPublicada=$false}
    } finally {
        if ($openWatch) {if($openWatch.IsRunning){$openWatch.Stop()};$report.aberturaElapsedMs=$openWatch.Elapsed.TotalMilliseconds}
        if ($connection) {try{$connection.Dispose()}catch{$report.limpeza.conexaoDescartada=$false}}
        if ($credential) {try{$credential.Password.Dispose()}catch{$report.limpeza.secureStringDescartada=$false}}
        if ($certificate) {try{$certificate.Dispose()}catch{$report.limpeza.certificadoDescartado=$false}}
        if ($builder) {$builder.Clear()}
        $credential=$null;$connection=$null;$sqlCredential=$null;$builder=$null
    }
    if (-not $report.limpeza.conexaoDescartada -or -not $report.limpeza.secureStringDescartada -or -not $report.limpeza.certificadoDescartado) {$report.guardaRealAprovada=$false;$report.estado='GUARDA_ATUAL_BLOQUEADA'}
    if ($CadastroPreGrant -and $report.estado -ne 'GUARDA_PRE_GRANT_CADASTRO_APROVADA') {$report.guardaPermissoesCadastroAprovada=$false}
    $report.observadoEm=[DateTime]::UtcNow.ToString('o');$report.faseUltima=$phase
    $report.motivo=if($report.guardaRealAprovada){'Identidade/TLS/permissoes/catalogo/historico atuais e fontes V1-V10 conferidos.'}elseif($report.erro){$report.erro.codigo+' na fase '+$report.erro.fase+'; BE/FE real nao podem iniciar. Sem nova tentativa automatica.'}else{'Descarte seguro nao confirmado; BE/FE real nao podem iniciar.'}
    $report.checks=$checks.ToArray();$report.queries=$queries.ToArray()
    if ($CadastroPreGrant -and $report.estado -eq 'GUARDA_PRE_GRANT_CADASTRO_APROVADA') {$report.motivo='Pre-condicoes para ajuste cadastral conferidas. Nao libera backend; exige guarda normal apos ajuste.'}
    $report.auxiliar=[ordered]@{arquivo='infra/prod/guarda-wmsprod.ps1';sha256=(Get-FileHash -LiteralPath $script:WmsProdSource -Algorithm SHA256).Hash}
    [pscustomobject]$report
}

if ($MyInvocation.InvocationName -ne '.') {
    $stream=$null
    try {
        # Recusa destino invalido/existente ANTES da unica abertura SQL.
        if ($EvidencePath) {$stream=[IO.File]::Open([IO.Path]::GetFullPath($EvidencePath),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)}
        $result=Invoke-WmsProdGuard
        $json=$result|ConvertTo-Json -Depth 18
        if ($stream) {$bytes=[Text.UTF8Encoding]::new($false).GetBytes($json);$stream.Write($bytes,0,$bytes.Length)}
    } catch {
        $result=[pscustomobject]@{demanda='PROD-LAUNCH01';guardaRealAprovada=$false;estado='GUARDA_ATUAL_BLOQUEADA';pidGuarda=$PID;observadoEm=[DateTime]::UtcNow.ToString('o');motivo='PROD_AUXILIAR_OU_SAIDA_RECUSADA; sem liberacao BE/FE.';erro=[pscustomobject]@{codigo='PROD_AUXILIAR_OU_SAIDA_RECUSADA';mensagemBrutaPublicada=$false}}
        $json=$result|ConvertTo-Json -Depth 5
    } finally {if($stream){$stream.Dispose()}}
    Write-Output $json
    if ($result.guardaRealAprovada) {exit 0} else {exit 20}
}
