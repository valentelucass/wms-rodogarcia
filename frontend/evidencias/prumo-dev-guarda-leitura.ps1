# D31-DEV01: somente arquivos e metadados de ACL WMSDEV; nenhuma abertura SQL/HTTP.
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$taskOutput = Join-Path $PSScriptRoot 'frontend-prumo-dev-guarda-lucas-20261008.json'
$taskAt = [DateTime]::UtcNow.ToString('o')
$taskSources = @(
    'states.md',
    'docs/09-continuidade.md',
    'docs/06-decisoes-e-pendencias.md',
    'orchestracao/.runtime/d29-incidente-suspensao-farol.md',
    'orchestracao/.runtime/d29-incidente-diagnostico-farol.md',
    'orchestracao/.runtime/d29-incidente-prumo-diagnostico-20261007T203909936.json',
    'orchestracao/.runtime/d30-retomada-apos-reinicio-chats.json',
    'orchestracao/.runtime/d30-resultado-parcial-final14.json',
    'backend/src/main/resources/application.properties',
    'backend/src/main/resources/application-sqlserver-dev.properties',
    'database/scripts/d26-credencial-aplicacao.ps1',
    'database/scripts/d22-credencial.ps1'
)
$taskHashRecords = @($taskSources | ForEach-Object {
    $p = Join-Path $taskRoot $_
    $item = Get-Item -LiteralPath $p
    [pscustomobject]@{path=$_;sha256=(Get-FileHash -LiteralPath $p -Algorithm SHA256).Hash;bytes=$item.Length;lastWriteUtc=$item.LastWriteTimeUtc.ToString('o')}
})
# Dot-source revisado: bibliotecas apenas definem funcoes; nunca chamar profile/factory/import de segredo.
. (Join-Path $taskRoot 'database/scripts/d26-credencial-aplicacao.ps1')
$taskMetadata = Get-WmsDevCredentialMetadata
$taskHistorical = Get-Content -LiteralPath (Join-Path $taskRoot 'orchestracao/.runtime/d29-incidente-prumo-diagnostico-20261007T203909936.json') -Encoding UTF8 -Raw | ConvertFrom-Json
$taskStates = Get-Content -LiteralPath (Join-Path $taskRoot 'states.md') -Encoding UTF8 -Raw
$taskContinuity = Get-Content -LiteralPath (Join-Path $taskRoot 'docs/09-continuidade.md') -Encoding UTF8 -Raw
$taskFinal14 = Get-Content -LiteralPath (Join-Path $taskRoot 'orchestracao/.runtime/d30-resultado-parcial-final14.json') -Encoding UTF8 -Raw | ConvertFrom-Json
$taskReferences = @($taskStates -split '\r?\n' | Where-Object { $_ -match 'Limites preservados por ID: incidente D29' }) + @($taskContinuity -split '\r?\n' | Where-Object { $_ -match 'Limites preservados por ID: incidente D29' })
$taskRuntimeCandidates = @(Get-ChildItem -File -LiteralPath (Join-Path $taskRoot 'orchestracao/.runtime') | Where-Object { $_.Name -match '^frontend-' -and $_.Name -match 'integrado|dev01|guarda|resolu' } | ForEach-Object {[pscustomobject]@{path=('orchestracao/.runtime/'+$_.Name);lastWriteUtc=$_.LastWriteTimeUtc.ToString('o')}})
$taskReport = [ordered]@{
    id='D31-DEV01-PRUMO-GUARDA-MINIMA-20261008'
    demanda='FIM_FRONTEND_LUCAS_20261008 / incremento D31-DEV01'
    agente='WMS - Prumo'
    observedAt=$taskAt
    estado='BLOQUEADO_NO_PREREQUISITO_RESOLUCAO_SEGURA_D29'
    guardaRealAprovada=$false
    backendPodeIniciarComSQL=$false
    frontendPodeConectarAPIComSQL=$false
    resolucaoSeguraComprovada=$false
    provaAtualDeResolucao=$null
    motivo='Fontes atuais consultadas mantem D29 sem resolucao segura comprovada. A nova autorizacao permite verificacao minima quando seguro; nao comprova recuperacao. Sem prova de resolucao, nao iniciar abertura real ou reusar preflight antigo.'
    limiteDeBusca='Leitura direcionada de canônicos/recibos atuais e nomes de incrementos frontend no runtime WMS. Ausencia nessas fontes, nao alegacao universal de inexistencia de prova em todo ambiente. Referencia atual de resolucao solicitada ao Farol; nenhuma fornecida ate esta captura.'
    fontes=$taskHashRecords
    afirmacoesCanonicasAtuais=$taskReferences
    reciboAtualFINAL14=$taskFinal14.SQLIncidenteD29
    candidatosIncrementoLocal=$taskRuntimeCandidates
    historicoSeparado=[ordered]@{
        path='orchestracao/.runtime/d29-incidente-prumo-diagnostico-20261007T203909936.json'
        observedAt=$taskHistorical.observadoEm
        estado=$taskHistorical.estado
        aberturaTentativas=$taskHistorical.aberturaTentativas
        aberturaConcluida=$taskHistorical.aberturaConcluida
        fase=$taskHistorical.erro.fase
        sqlNumber=$taskHistorical.erro.sqlNumber
        nativeErrorCode=$taskHistorical.erro.nativeErrorCode
        elapsedMs=$taskHistorical.aberturaElapsedMs
        queriesGuarda=$taskHistorical.queriesGuarda
        queriesMetadados=$taskHistorical.queriesMetadados
        causa='INCONCLUSIVA; nao atribuir timeout atual a eventos701 sem prova'
        baseline19h14='Historica anterior a falha20h39; nao aceita guarda atual'
    }
    canalProprioWMSDEV=[ordered]@{
        fonte='database/scripts/d26-credencial-aplicacao.ps1 / Get-WmsDevCredentialMetadata'
        path=$taskMetadata.arquivo
        metadata=$taskMetadata
        conteudoCLIXMLLido=$false
        importClixmlExecutado=$false
        dpapiDescriptografada=$false
        senhaOuTokenLidoOuPublicado=$false
        loginRealConfirmado=$false
        limite='Existencia/owner/ACL somente. Nao certifica PSCredential, senha, login ou permissao SQL atual.'
    }
    configuracaoEstatica=[ordered]@{
        perfil='sqlserver-dev opt-in; application.properties default local'
        destino='wms.database.name/user/host/confirmed-target/confirmed-server via entradas externas; valores nao lidos'
        perfilProprio='Get-WmsDevApplicationProfile declara WMS_DEV/WMSDEV; funcao nao chamada'
        ddlAuto='validate'
        generateDDL=$false
        sqlInit='never'
        flywayEnabled=$false
        TLS='configuracao externa de certificado/truststore; nao validar handshake por configuracao estatica'
        segredoExternoInspecionado=$false
        limite='Fontes mostram mutacao automatica desligada; catalogo/historico/checksum/migrations atuais nao consultados.'
    }
    criterios=@(
        [ordered]@{id='D31-DEV01-G01';criterio='Resolucao segura atual D29';estado='BLOQUEADO_PROVA_NAO_LOCALIZADA_NAS_FONTES_CONSULTADAS'},
        [ordered]@{id='D31-DEV01-G02';criterio='Canal proprio WMSDEV existencia e ACL';estado=if($taskMetadata.existe -and $taskMetadata.aclValida){'CONFIRMADO_SOMENTE_METADADO_LOCAL_SEM_SEGREDO'}else{'AUSENTE_OU_ACL_NAO_APROVADA'}},
        [ordered]@{id='D31-DEV01-G03';criterio='Nome real WMS_DEV e identidade real WMSDEV restrita';estado='NAO_EXECUTADO_DEPENDE_G01'},
        [ordered]@{id='D31-DEV01-G04';criterio='TLS atual valido sem bypass';estado='NAO_EXECUTADO_DEPENDE_G01'},
        [ordered]@{id='D31-DEV01-G05';criterio='Permissoes atuais restritas por objeto/coluna sem DDL/admin';estado='NAO_EXECUTADO_DEPENDE_G01'},
        [ordered]@{id='D31-DEV01-G06';criterio='Schema/history/migrations sem pendentes e sem mutacao automatica';estado='ESTATICO_SEM_MUTACAO_CONFIRMADO_REAL_NAO_EXECUTADO_DEPENDE_G01'}
    )
    executadoAgora=[ordered]@{somenteLeituraArquivos=$true;metadadosCanalWMSDEV=$true;aberturasSQL=0;SELECT=0;HTTP=0;fixture=0;testeBackend=0;buildBackend=0;processosBackendIniciados=0;prod=0;sa=0;DDL=0;DML=0;grants=0;alteracaoRuntime=0;restart=0;kill=0;publicacao=0;callbackHermes=0}
    css=[ordered]@{recibo='frontend/evidencias/frontend-prumo-css-lucas-20261008.json';estado='PASS_LOCAL_30CAPTURAS_COMPUTED_DEV_BUILD_1440_768_390';freeze='8arquivosHashEstaveis_7alterados_aggregadorPreservado';processosProprios5191Fechados=$true;revisao='Fatia encaminhada a Vigia via Farol; Lume integra/checkfinal'}
    encaminhamento='Farol manter Cedro sem BEcomSQL e Lume sem APIcomSQL. Responsavel pelo SQL fornecer referencia atual de resolucao segura. Somente depois revisar evidencia e avaliar nova guarda propria minima WMS_DEV/WMSDEV/TLS/permissoes/catalogo/history; sem sonda repetida, fallback ou alteracao de servidor/acessos/sharedruntime. Decisao material necessaria se recuperacao exige essas alteracoes.'
}
$taskReport | ConvertTo-Json -Depth 14 | Set-Content -LiteralPath $taskOutput -Encoding UTF8
[pscustomobject]@{estado=$taskReport.estado;guardaRealAprovada=$false;credencialArquivoExiste=$taskMetadata.existe;aclValida=$taskMetadata.aclValida;conteudoLido=$false;aberturaSQL=0;recibo=$taskOutput} | ConvertTo-Json
