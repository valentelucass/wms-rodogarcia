# D31-DEV02. Uma conexao propria; somente SELECTs de guarda/metadados WMS_DEV.
# Dot-source define Invoke-WmsDev02Guard. Execucao direta retorna JSON e exit 0/20.
[CmdletBinding()]
param([string]$EvidencePath)
$script:WmsDev02Root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$script:WmsDev02Source = $PSCommandPath

function Get-WmsDev02Migrations {
    if (-not ('WmsDev02Checksum' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.IO;
using System.Text;
public static class WmsDev02Checksum {
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
    Get-ChildItem -LiteralPath (Join-Path $script:WmsDev02Root 'database/migrations') -File -Filter 'V*.sql' |
        ForEach-Object {
            if ($_.Name -cnotmatch '^V([1-9][0-9]*)__.+\.sql$') { throw 'DEV02_MIGRATION_NOME_INVALIDO' }
            [pscustomobject]@{version=$Matches[1];script=$_.Name;checksum=[WmsDev02Checksum]::Read($_.FullName);sha256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash}
        } | Sort-Object {[int]$_.version}
}

function Invoke-WmsDev02Guard {
    [CmdletBinding()]
    param()
    $ErrorActionPreference = 'Stop'
    $credential=$null; $connection=$null; $certificate=$null; $builder=$null; $openWatch=$null
    $phase='PREPARO_LOCAL'; $identityConfirmed=$false
    $checks=New-Object 'Collections.Generic.List[object]'
    $queries=New-Object 'Collections.Generic.List[object]'
    $report=[ordered]@{
        demanda='D31-DEV02 / FIM_INTEGRACAO_DEV_LUCAS';agente='WMS - Prumo'
        inicioUtc=[DateTime]::UtcNow.ToString('o');pidGuarda=$PID;estado='GUARDA_EM_PREPARO';guardaRealAprovada=$false;motivo=$null
        aberturaTentativas=0;aberturaConcluida=$false;aberturaElapsedMs=$null
        identidadeConfirmada=$false;alvo=$null;tls=$null;direitos=$null;catalogo=$null;historico=$null
        queriesGuarda=0;queriesMetadados=0;queriesNegocio=0;erro=$null
        politica=[ordered]@{alvo='tcp:127.0.0.1,1433';banco='WMS_DEV';login='WMSDEV';connectTimeout=5;commandTimeout=3;pooling=$false;connectRetryCount=0;fallback=$false}
        limites=[ordered]@{API=0;DML=0;DDL=0;PROD=0;admin=0;grants=0;sharedRuntimeAlterado=$false;servidorAlterado=$false;recapturaCertificado=$false;segredoPublicado=$false}
        limpeza=[ordered]@{conexaoDescartada=$true;secureStringDescartada=$true;certificadoDescartado=$true}
    }
    function Assert-Dev02([string]$Name,[bool]$Ok) {
        $checks.Add([pscustomobject]@{criterio=$Name;passou=$Ok})
        if (-not $Ok) { throw 'DEV02_GUARDA_RECUSADA' }
    }
    function Compare-Dev02Rows($A,$B,[string[]]$Fields) {
        $aa=@($A|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
        $bb=@($B|Select-Object $Fields|ForEach-Object {$_|ConvertTo-Json -Compress}|Sort-Object)
        return ($aa.Count -eq $bb.Count -and @(Compare-Object $aa $bb).Count -eq 0)
    }
    function Select-Dev02([string]$Name,[string]$Sql,[switch]$Identity) {
        if (-not $Identity -and -not $identityConfirmed) { throw 'DEV02_IDENTIDADE_NAO_CONFIRMADA' }
        $syntax=[regex]::Replace($Sql,"N?'(?:''|[^'])*'","''")
        if ($syntax.TrimStart() -cnotmatch '^SELECT\b' -or $syntax -match ';\s*\S|\b(INTO|INSERT|UPDATE|DELETE|MERGE|EXEC|ALTER|CREATE|DROP|GRANT|REVOKE|DENY|USE)\b|--|/\*') { throw 'DEV02_SELECT_RECUSADO' }
        $cmd=$connection.CreateCommand();$reader=$null;$watch=[Diagnostics.Stopwatch]::StartNew()
        $cmd.CommandText=$Sql;$cmd.CommandTimeout=3
        $entry=[ordered]@{nome=$Name;concluida=$false;linhas=$null;elapsedMs=$null}
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
        . (Join-Path $script:WmsDev02Root 'database/scripts/d26-credencial-aplicacao.ps1')
        $baselinePath=Join-Path $script:WmsDev02Root 'orchestracao/.runtime/d29-farol-retomada-preflight.json'
        Assert-Dev02 'oraculo historico imutavel; nunca substitui leitura atual' ((Get-FileHash -LiteralPath $baselinePath -Algorithm SHA256).Hash -ceq 'C22AABF54F839D4B6E139A598A8478530F92F8911FEEE46BB2F92BC85313EA90')
        $baseline=Get-Content -LiteralPath $baselinePath -Encoding UTF8 -Raw|ConvertFrom-Json
        $report.oraculo=[ordered]@{arquivo='orchestracao/.runtime/d29-farol-retomada-preflight.json';sha256=(Get-FileHash -LiteralPath $baselinePath -Algorithm SHA256).Hash;observadoEm=$baseline.observadoEm;uso='comparacao de direitos/catalogo/historico apos SELECTs atuais'}
        $migrations=@(Get-WmsDev02Migrations)
        Assert-Dev02 'inventario local V1-V10 sem arquivo ausente ou adicional' (Compare-Dev02Rows $migrations @($baseline.historico|Where-Object {$_.type -ceq 'SQL'}) @('version','script'))
        $report.migrationsLocais=$migrations
        $metadata=Get-WmsDevCredentialMetadata
        Assert-Dev02 'canal exclusivo WMSDEV existente, ACL e caminho protegidos' ($metadata.existe -and $metadata.aclValida)
        $report.canal=[ordered]@{arquivo='%LOCALAPPDATA%/Rodogarcia/WMS/api-dev/wmsdev-app.clixml';aclValida=$true;conteudoPublicado=$false;DPAPIImportada=$false}
        $modulePath=[IO.Path]::GetFullPath((Join-Path $script:WmsDev02Root '../.runtime/sql-server/SqlServerProjetos.psm1'))
        $module=Import-Module $modulePath -PassThru -Force
        # Somente loader com hashes. Profile/factory/update/admin NUNCA chamados.
        & $module {Import-SqlClient}
        $tlsRoot=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer'
        $tlsPath=Join-Path $tlsRoot 'tls.json'
        & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tlsPath
        $tls=Get-Content -LiteralPath $tlsPath -Encoding UTF8 -Raw|ConvertFrom-Json
        Assert-Dev02 'certificado publico existente no caminho protegido esperado' ($tls.id -cmatch '^[a-f0-9]{32}$' -and $tls.certificate -ceq (Join-Path $tlsRoot ('sql-public-'+$tls.id+'.pem')))
        & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tls.certificate
        $certHash=(Get-FileHash -LiteralPath $tls.certificate -Algorithm SHA256).Hash
        Assert-Dev02 'certificado fixado corresponde ao recibo protegido e origem WMS validada' ($certHash -ceq $tls.fileSHA256 -and $certHash -ceq $baseline.tls.certificateSha256)
        $certificate=New-Object Security.Cryptography.X509Certificates.X509Certificate2($tls.certificate)
        Assert-Dev02 'certificado dentro da validade e thumbprint esperado' ([DateTime]::UtcNow -ge $certificate.NotBefore.ToUniversalTime() -and [DateTime]::UtcNow -lt $certificate.NotAfter.ToUniversalTime() -and $certificate.Thumbprint -ceq $baseline.tls.thumbprint)
        Assert-Dev02 'truststore existente sem regeneracao' ($tls.truststore -ceq ($tls.certificate -replace '\.pem$','.p12'))
        & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tls.truststore
        Assert-Dev02 'hash truststore publico protegido' ((Get-FileHash -LiteralPath $tls.truststore -Algorithm SHA256).Hash -ceq $tls.truststoreSHA256)
        $credential=Get-WmsDevApplicationCredential
        $report.canal.DPAPIImportada=$true
        Assert-Dev02 'identidade da fonte propria WMSDEV' ($credential.UserName -ceq 'WMSDEV')
        $builder=New-Object Microsoft.Data.SqlClient.SqlConnectionStringBuilder
        $builder.set_DataSource('tcp:127.0.0.1,1433');$builder.set_InitialCatalog('WMS_DEV')
        $builder.set_Encrypt([Microsoft.Data.SqlClient.SqlConnectionEncryptOption]::Mandatory)
        $builder.set_TrustServerCertificate($false);$builder.set_ServerCertificate($tls.certificate)
        $builder.set_ApplicationName('WMS-D31-DEV02-PRUMO-GUARDA')
        $builder.set_Pooling($false);$builder.set_ConnectTimeout(5);$builder.set_ConnectRetryCount(0)
        $builder.set_TransparentNetworkIPResolution($false);$builder.set_MultiSubnetFailover($false)
        Assert-Dev02 'sem senha/login em connectionstring; sem identidade Windows/fallback' ([string]::IsNullOrEmpty($builder.Password) -and [string]::IsNullOrEmpty($builder.UserID) -and -not $builder.IntegratedSecurity)
        $sqlCredential=New-Object Microsoft.Data.SqlClient.SqlCredential('WMSDEV',$credential.Password)
        $connection=New-Object Microsoft.Data.SqlClient.SqlConnection($builder.ConnectionString,$sqlCredential)
        $report.tls=[ordered]@{encrypt='Mandatory';trustServerCertificate=$false;certificateSha256=$certHash;thumbprint=$certificate.Thumbprint;notAfter=$certificate.NotAfter.ToUniversalTime().ToString('o');handshakeConcluido=$false;confirmacaoAtual=$false;truststoreSha256=$tls.truststoreSHA256}
        $report.cliente=[ordered]@{assembly=[Microsoft.Data.SqlClient.SqlConnection].Assembly.GetName().Version.ToString();sha256=(Get-FileHash -LiteralPath ([Microsoft.Data.SqlClient.SqlConnection].Assembly.Location) -Algorithm SHA256).Hash;moduloSha256=(Get-FileHash -LiteralPath $modulePath -Algorithm SHA256).Hash}
        $phase='ABERTURA';$report.aberturaTentativas=1;$report.aberturaInicioUtc=[DateTime]::UtcNow.ToString('o')
        $openWatch=[Diagnostics.Stopwatch]::StartNew();$connection.Open();$openWatch.Stop()
        $report.aberturaConcluida=$true;$report.aberturaElapsedMs=$openWatch.Elapsed.TotalMilliseconds;$report.tls.handshakeConcluido=$true
        $phase='IDENTIDADE'
        $identity=@(Select-Dev02 'DB_NAME_ORIGINAL_LOGIN_USER_NAME' "SELECT DB_NAME() banco,ORIGINAL_LOGIN() login,USER_NAME() usuario,CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) servidor,CONVERT(nvarchar(128),SERVERPROPERTY('ProductVersion')) versao,@@SPID sessao,(SELECT state_desc FROM sys.databases WHERE database_id=DB_ID()) estado" -Identity)
        $report.alvo=$identity[0]
        Assert-Dev02 'alvo REAL WMS_DEV, login/usuario WMSDEV, ONLINE e servidor autorizado' ($identity.Count -eq 1 -and $identity[0].banco -ceq 'WMS_DEV' -and $identity[0].login -ceq 'WMSDEV' -and $identity[0].usuario -ceq 'WMSDEV' -and $identity[0].estado -ceq 'ONLINE' -and $identity[0].servidor -ceq 'ROD-SRVW-001')
        $identityConfirmed=$true;$report.identidadeConfirmada=$true;$report.tls.confirmacaoAtual=$true
        $phase='PERMISSOES'
        $rights=[ordered]@{}
        $rights.server=@(Select-Dev02 'server_efetivo' "SELECT permission_name FROM sys.fn_my_permissions(NULL,N'SERVER') ORDER BY permission_name")
        $rights.database=@(Select-Dev02 'database_efetivo' "SELECT permission_name FROM sys.fn_my_permissions(NULL,N'DATABASE') ORDER BY permission_name")
        $rights.roles=@(Select-Dev02 'roles_database' 'SELECT r.name role FROM sys.database_role_members m JOIN sys.database_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=USER_ID()')
        $rights.serverRoles=@(Select-Dev02 'roles_server' 'SELECT r.name role FROM sys.server_role_members m JOIN sys.server_principals r ON r.principal_id=m.role_principal_id WHERE m.member_principal_id=SUSER_ID()')
        $rights.ownership=@(Select-Dev02 'schemas_proprios' 'SELECT name FROM sys.schemas WHERE principal_id=USER_ID()')
        $rights.schema=@(Select-Dev02 'schema_wms_efetivo' "SELECT permission_name FROM sys.fn_my_permissions(N'wms',N'SCHEMA')")
        $rights.explicit=@(Select-Dev02 'direitos_explicitos' "SELECT p.class_desc,COALESCE(OBJECT_SCHEMA_NAME(p.major_id),N'') esquema,COALESCE(OBJECT_NAME(p.major_id),N'') objeto,p.minor_id,COALESCE(COL_NAME(p.major_id,p.minor_id),N'') coluna,p.permission_name,p.state_desc FROM sys.database_permissions p WHERE p.grantee_principal_id=USER_ID() ORDER BY p.class_desc,esquema,objeto,p.minor_id,p.permission_name")
        $rights.objects=@(Select-Dev02 'objetos_wms_efetivo' "SELECT t.name tabela,p.permissao,HAS_PERMS_BY_NAME(N'wms.'+t.name,N'OBJECT',p.permissao) permitido FROM sys.tables t CROSS JOIN (VALUES(N'SELECT'),(N'INSERT'),(N'DELETE'),(N'ALTER'),(N'CONTROL'),(N'TAKE OWNERSHIP'),(N'VIEW CHANGE TRACKING'))p(permissao) WHERE t.schema_id=SCHEMA_ID(N'wms') ORDER BY t.name,p.permissao")
        $rights.columns=@(Select-Dev02 'colunas_wms_update_efetivo' "SELECT t.name tabela,c.name coluna,HAS_PERMS_BY_NAME(N'wms.'+t.name,N'OBJECT',N'UPDATE',c.name,N'COLUMN') permitido FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') ORDER BY t.name,c.column_id")
        $report.direitos=$rights
        Assert-Dev02 'nenhuma role/ownership/schema amplo; server somente CONNECT SQL/VIEW ANY DATABASE; database CONNECT' ($rights.roles.Count -eq 0 -and $rights.serverRoles.Count -eq 0 -and $rights.ownership.Count -eq 0 -and $rights.schema.Count -eq 0 -and (Compare-Dev02Rows $rights.server @([pscustomobject]@{permission_name='CONNECT SQL'},[pscustomobject]@{permission_name='VIEW ANY DATABASE'}) @('permission_name')) -and (Compare-Dev02Rows $rights.database @([pscustomobject]@{permission_name='CONNECT'}) @('permission_name')))
        foreach ($kind in @('server','database','roles','serverRoles','ownership','schema','explicit','objects','columns')) {
            $fields=@($baseline.rights.$kind|Select-Object -First 1|ForEach-Object {$_.PSObject.Properties.Name})
            $equal=if($fields.Count){Compare-Dev02Rows $rights[$kind] $baseline.rights.$kind $fields}else{@($rights[$kind]).Count -eq 0}
            Assert-Dev02 ('permissoes atuais iguais restricao por objeto/coluna: '+$kind) $equal
        }
        $phase='CATALOGO_HISTORICO'
        $catalog=@(Select-Dev02 'catalogo_integridade' "SELECT (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') tabelas,(SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON t.object_id=c.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') colunas,(SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) checksInvalidos,(SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1)) fksInvalidas,(SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON t.object_id=i.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) indicesDesabilitados")
        $report.catalogo=$catalog[0]
        Assert-Dev02 'catalogo 64/687 e integridade conforme fonte' (Compare-Dev02Rows $catalog @($baseline.catalogo) @('tabelas','colunas','checksInvalidos','fksInvalidas','indicesDesabilitados'))
        $history=@(Select-Dev02 'historico_flyway' 'SELECT installed_rank,version,type,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank')
        $report.historico=$history
        Assert-Dev02 'historico atual imutavel V1-V10 sem falhas/pendentes/divergencia' (Compare-Dev02Rows $history $baseline.historico @('installed_rank','version','type','script','checksum','success'))
        Assert-Dev02 'historico SQL atual corresponde a todas fontes locais' (Compare-Dev02Rows @($history|Where-Object {$_.type -ceq 'SQL'}) $migrations @('version','script','checksum'))
        $report.perfilProtegido=[ordered]@{host='127.0.0.1';port=1433;database='WMS_DEV';login='WMSDEV';certificate=$tls.certificate;truststore=$tls.truststore;certificateHost=$tls.certificateHost;encrypt=$true;trustServerCertificate=$false}
        $report.estado='GUARDA_ATUAL_APROVADA';$report.guardaRealAprovada=$true
    } catch {
        $ex=$_.Exception;$code='DEV02_FALHA';$types=New-Object 'Collections.Generic.List[string]'
        $number=$null;$native=$null;$markers=[ordered]@{prelogin=$false;handshake=$false;timeout=$false;autenticacao=$false;certificado=$false}
        while ($ex) {
            $types.Add($ex.GetType().FullName);$message=[string]$ex.Message
            if ($message -cmatch '^(DEV02|D26|SQL)_[A-Z0-9_]+$') {$code=$message}
            if ($ex.GetType().FullName -ceq 'Microsoft.Data.SqlClient.SqlException') {$number=$ex.Number;$code='DEV02_SQL_'+$number}
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
    $report.observadoEm=[DateTime]::UtcNow.ToString('o');$report.faseUltima=$phase
    $report.motivo=if($report.guardaRealAprovada){'Identidade/TLS/permissoes/catalogo/historico atuais e fontes V1-V10 conferidos.'}elseif($report.erro){$report.erro.codigo+' na fase '+$report.erro.fase+'; BE/FE real nao podem iniciar. Sem nova tentativa automatica.'}else{'Descarte seguro nao confirmado; BE/FE real nao podem iniciar.'}
    $report.checks=$checks.ToArray();$report.queries=$queries.ToArray()
    $report.auxiliar=[ordered]@{arquivo='infra/dev02/guarda-wmsdev.ps1';sha256=(Get-FileHash -LiteralPath $script:WmsDev02Source -Algorithm SHA256).Hash}
    [pscustomobject]$report
}

if ($MyInvocation.InvocationName -ne '.') {
    $stream=$null
    try {
        # Recusa destino invalido/existente ANTES da unica abertura SQL.
        if ($EvidencePath) {$stream=[IO.File]::Open([IO.Path]::GetFullPath($EvidencePath),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)}
        $result=Invoke-WmsDev02Guard
        $json=$result|ConvertTo-Json -Depth 18
        if ($stream) {$bytes=[Text.UTF8Encoding]::new($false).GetBytes($json);$stream.Write($bytes,0,$bytes.Length)}
    } catch {
        $result=[pscustomobject]@{demanda='D31-DEV02';guardaRealAprovada=$false;estado='GUARDA_ATUAL_BLOQUEADA';pidGuarda=$PID;observadoEm=[DateTime]::UtcNow.ToString('o');motivo='DEV02_AUXILIAR_OU_SAIDA_RECUSADA; sem liberacao BE/FE.';erro=[pscustomobject]@{codigo='DEV02_AUXILIAR_OU_SAIDA_RECUSADA';mensagemBrutaPublicada=$false}}
        $json=$result|ConvertTo-Json -Depth 5
    } finally {if($stream){$stream.Dispose()}}
    Write-Output $json
    if ($result.guardaRealAprovada) {exit 0} else {exit 20}
}
