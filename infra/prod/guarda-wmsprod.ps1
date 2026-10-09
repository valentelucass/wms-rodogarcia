# PROD-LAUNCH01: gate failclosed atual. Nenhuma Open/DPAPI/SQL nesta versao.
# Fonte PROD propria nao descoberta: nao inventar identidade/loader/atestacao.
# Dot-source define Invoke-WmsProdGuard; execucao direta JSONstdout/exit20.
[CmdletBinding()]
param([string]$EvidencePath)
$script:WmsProdRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$script:WmsProdSource=$PSCommandPath

function Invoke-WmsProdGuard {
    [CmdletBinding()]
    param()
    $ErrorActionPreference='Stop'
    $report=[ordered]@{
        demanda='PROD-LAUNCH01';agente='WMS - Prumo';inicioUtc=[DateTime]::UtcNow.ToString('o');pidGuarda=$PID
        estado='GUARDA_ATUAL_BLOQUEADA';guardaRealAprovada=$false;motivo=$null
        aberturaTentativas=0;aberturaInicioUtc=$null;aberturaConcluida=$false;aberturaElapsedMs=$null
        identidadeConfirmada=$false;alvo=$null;tls=$null;direitos=$null;catalogo=$null;historico=$null
        queriesGuarda=0;queriesMetadados=0;queriesNegocio=0;erro=$null
        politica=[ordered]@{alvo='tcp:127.0.0.1,1433';banco='WMS_PROD';login='WMSPROD';encrypt='Mandatory';trustServerCertificate=$false;connectTimeout=120;guardTimeout=240;progressInterval=10;commandTimeout=3;pooling=$false;connectRetryCount=0;fallback=$false}
        limites=[ordered]@{API=0;DML=0;DDL=0;PROD=0;TESTE_BANCO=0;DEV=0;admin=0;grants=0;DPAPIImportada=$false;sharedRuntimeAlterado=$false;servidorAlterado=$false;recapturaCertificado=$false;segredoPublicado=$false}
        limpeza=[ordered]@{conexaoDescartada=$true;secureStringDescartada=$true;certificadoDescartado=$true;recursosSensiveisCriados=$false}
        checks=@();queries=@();faseUltima='PREPARO_LOCAL';operacaoSQLImplementada=$false
    }
    $checks=New-Object 'Collections.Generic.List[object]'
    $code='PROD_CHANNEL_CONFIGURATION_MISSING'
    try {
        $path=Join-Path $script:WmsProdRoot 'infra/prod/contrato-prod-publico.json'
        $public=Get-Content -LiteralPath $path -Encoding UTF8 -Raw | ConvertFrom-Json
        $report.contratoPublico=[ordered]@{arquivo='infra/prod/contrato-prod-publico.json';sha256=(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash;somenteMetadados=$true}
        $targetOk=($public.versaoContrato -eq 1 -and $public.bancoEsperado -ceq 'WMS_PROD')
        $checks.Add([pscustomobject]@{criterio='contrato exclusivo WMS_PROD, sem fallbackDEV';passou=$targetOk})
        if(-not $targetOk){$code='PROD_TARGET_CONTRACT_REFUSED';throw $code}
        if($public.identidadeEsperada -cne 'WMSPROD') {$code='PROD_IDENTITY_REFUSED';throw $code}
        $hasBinding=($null -ne $public.canalAplicacaoProtegido -and -not [string]::IsNullOrWhiteSpace([string]$public.identidadeEsperada) -and $null -ne $public.fonteIdentidadeRestrita -and $null -ne $public.contratoPermissoesCatalogoHistorico)
        $checks.Add([pscustomobject]@{criterio='canal e identidade PROD proprios descobertos e atestados';passou=$hasBinding})
        if(-not $hasBinding){$code='PROD_PROTECTED_APPLICATION_CHANNEL_MISSING';throw $code}
        # Declaracao em JSON/variavel nao implementa nem autoriza binding protegido.
        # Nenhum loader/DPAPI/factory/runtimeProfile/update/SQL e chamado aqui.
        $code='PROD_PROTECTED_BINDING_NOT_IMPLEMENTED'
        throw $code
    } catch {
        $allowed=@('PROD_CHANNEL_CONFIGURATION_MISSING','PROD_TARGET_CONTRACT_REFUSED','PROD_IDENTITY_REFUSED','PROD_PROTECTED_APPLICATION_CHANNEL_MISSING','PROD_PROTECTED_BINDING_NOT_IMPLEMENTED')
        if($code -cnotin $allowed){$code='PROD_CHANNEL_CONFIGURATION_MISSING'}
        $report.erro=[ordered]@{codigo=$code;fase='PREPARO_LOCAL';sqlNumber=$null;nativeErrorCode=$null;mensagemBrutaPublicada=$false}
        $report.motivo=$code+'; nenhuma Open/DPAPI/SQL/BE/FE autorizada por este gate. Fonte PROD propria e binding operacional ainda ausentes.'
    }
    $report.checks=$checks.ToArray();$report.observadoEm=[DateTime]::UtcNow.ToString('o')
    $report.auxiliar=[ordered]@{arquivo='infra/prod/guarda-wmsprod.ps1';sha256=(Get-FileHash -LiteralPath $script:WmsProdSource -Algorithm SHA256).Hash}
    [pscustomobject]$report
}

if($MyInvocation.InvocationName -ne '.') {
    $stream=$null
    try {
        # Saida create-only; erro de destino bloqueia antes do preflight.
        if($EvidencePath){$stream=[IO.File]::Open([IO.Path]::GetFullPath($EvidencePath),[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)}
        $result=Invoke-WmsProdGuard
        $json=$result | ConvertTo-Json -Depth 16
        if($stream){$bytes=[Text.UTF8Encoding]::new($false).GetBytes($json);$stream.Write($bytes,0,$bytes.Length)}
    } catch {
        $result=[pscustomobject]@{demanda='PROD-LAUNCH01';guardaRealAprovada=$false;estado='GUARDA_ATUAL_BLOQUEADA';pidGuarda=$PID;observadoEm=[DateTime]::UtcNow.ToString('o');aberturaTentativas=0;queriesGuarda=0;queriesMetadados=0;queriesNegocio=0;motivo='PROD_AUXILIAR_OU_SAIDA_RECUSADA; sem liberacao BE/FE.';erro=[pscustomobject]@{codigo='PROD_AUXILIAR_OU_SAIDA_RECUSADA';mensagemBrutaPublicada=$false};auxiliar=[pscustomobject]@{arquivo='infra/prod/guarda-wmsprod.ps1';sha256=(Get-FileHash -LiteralPath $script:WmsProdSource -Algorithm SHA256).Hash}}
        $json=$result | ConvertTo-Json -Depth 5
    } finally {if($stream){$stream.Dispose()}}
    Write-Output $json
    # Nao ha ramo PASS nesta versao: nunca aprovar por bool/recibo antigo/convencao.
    exit 20
}
