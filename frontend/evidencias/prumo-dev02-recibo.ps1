# Consolidacao somente local: le recibo, fontes e certificado PUBLICO; nunca abre SQL.
$ErrorActionPreference='Stop'
$proofRoot=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$actualPath=Join-Path $PSScriptRoot 'frontend-prumo-dev02-guarda-lucas-20261008.json'
$actual=Get-Content -LiteralPath $actualPath -Encoding UTF8 -Raw|ConvertFrom-Json
$checksums=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'prumo-dev02-checksums-locais.json') -Encoding UTF8 -Raw|ConvertFrom-Json
$cli=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'prumo-dev02-cli-failclosed-local.json') -Encoding UTF8 -Raw|ConvertFrom-Json
$helperPath=Join-Path $proofRoot 'infra/dev02/guarda-wmsdev.ps1'
$tokens=$null;$errors=$null
$null=[Management.Automation.Language.Parser]::ParseFile($helperPath,[ref]$tokens,[ref]$errors)
$publicTlsPath=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer/tls.json'
$publicTls=Get-Content -LiteralPath $publicTlsPath -Encoding UTF8 -Raw|ConvertFrom-Json
$publicCert=New-Object Security.Cryptography.X509Certificates.X509Certificate2($publicTls.certificate)
try {
    $certMetadata=[ordered]@{subject=$publicCert.Subject;issuer=$publicCert.Issuer;thumbprint=$publicCert.Thumbprint;extensions=@($publicCert.Extensions|ForEach-Object {$_.Oid.Value});notBefore=$publicCert.NotBefore.ToUniversalTime().ToString('o');notAfter=$publicCert.NotAfter.ToUniversalTime().ToString('o');sha256=(Get-FileHash -LiteralPath $publicTls.certificate -Algorithm SHA256).Hash}
} finally {$publicCert.Dispose()}
$nativeMeaning=(New-Object ComponentModel.Win32Exception([int]$actual.erro.nativeErrorCode)).Message
$inventory=@(
    'infra/dev02/guarda-wmsdev.ps1',
    'frontend/evidencias/prumo-dev02-guarda-executada-20261008.ps1',
    'frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json',
    'frontend/evidencias/prumo-dev02-checksums-locais.json',
    'frontend/evidencias/prumo-dev02-cli-failclosed-local.json',
    'frontend/evidencias/prumo-dev02-checksum-local.java',
    'orchestracao/.runtime/frontend-integracao-dev-correcao-lucas.txt',
    'database/scripts/d26-credencial-aplicacao.ps1',
    'frontend/evidencias/frontend-prumo-css-lucas-20261008.json',
    'frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json',
    'frontend/vite.config.ts'
)|ForEach-Object {[pscustomobject]@{arquivo=$_;sha256=(Get-FileHash -LiteralPath (Join-Path $proofRoot $_) -Algorithm SHA256).Hash}}
$receipt=[ordered]@{
    demanda='D31-DEV02';agente='WMS - Prumo';estado='BLOQUEIO_ATUAL_TLS';guardaRealAprovada=$false
    observadoEm=$actual.observadoEm;compiladoEm=[DateTime]::UtcNow.ToString('o');tentativasReais=1
    tentativa=[ordered]@{pid=$actual.pidGuarda;inicioUtc=$actual.aberturaInicioUtc;elapsedMs=$actual.aberturaElapsedMs;fase=$actual.erro.fase;sqlNumber=$actual.erro.sqlNumber;nativeErrorCode=$actual.erro.nativeErrorCode;nativeHex='0x800B0110';significadoSistema=$nativeMeaning;TLSConcluido=$false;identidadeRealConfirmada=$false;alvoRealConfirmado=$false;SELECTs=0;API=0;BE=0;FE=0;descarte=$actual.limpeza}
    canal=[ordered]@{exclusivo='WMSDEV/api-dev';ACLConferida=$true;DPAPIImportadaEmMemoria=$true;senhaEmArgumentoOuRelatorio=$false;fonteAdminUsada=$false}
    certificadoPublico=$certMetadata
    causaComprovada='Validacao TLS atual recusou certificado para o uso solicitado. Nao e timeout D29 reproduzido; nao identifica sozinho qual componente causa a recusa.'
    decisaoMaterial='Responsavel pelo TLS deve analisar a recusa atual e fornecer correcao/evidencia segura. Se exigir certificado, servidor, acessos ou runtime compartilhado, Prumo nao altera. Sem nova sonda ate encaminhamento expresso.'
    verificacoesLocais=[ordered]@{parseErrors=$errors.Count;checksumsLocaisConferidos=@($checksums).Count;checksumsLocaisTodosIguaisOraculo=(@($checksums|Where-Object {-not $_.checksumMatch}).Count -eq 0);referenciaAlgoritmo='Flyway-core 12.4.0 instalado; javap ChecksumCalculator e leitura offline FileSystemResource V10 produziram -562012523';CLIRecusaAntesGuarda=$cli}
    deltaAuxiliar='Tentativa real preservada com fonte A3DE06DD. Depois corrigida apenas leitura local BOM/CRC e recusa create-only antes da guarda. Fonte final conferida localmente; sem segunda Open. Os checksums locais da tentativa r01 eram calculados incorretamente e nao foram usados para liberar SQL; prova corrigida separada, historico real continua desconhecido.'
    contrato=[ordered]@{arquivo='infra/dev02/guarda-wmsdev.ps1';assinatura='Invoke-WmsDev02Guard sem argumentos por dot-source';CLI='powershell.exe -NoLogo -NoProfile -NonInteractive -File infra/dev02/guarda-wmsdev.ps1';parametroOpcional='EvidencePath nao sensivel, create-only antes da guarda';stdout='JSON sanitizado';exitPASS=0;exitBLOCK=20;credencial='Get-WmsDevApplicationCredential apenas memoria; PSCredential nunca no JSON';frescor='PASS somente resultado desta invocacao completa, mesmo helper hash. Nunca variavel bool/listener/recibo antigo.';estadoAgora='Bloqueado: launcher/equipe nao devem chamar novamente guard real nem BE/FE. Propagacao isolada somente, sem nova Open.'}
    limites=[ordered]@{permissoesCatalogoHistoricoReaisNaoVerificados=$true;nenhumaCorrecaoTLSExecutada=$true;DDL=0;DML=0;grants=0;PROD=0;sa=0;alteracoesSharedRuntime=0;restartKillExistentes=0;SQLFixture=0;HermesETL=0;commitPushPublicacao=0;nenhumaSuiteGeral=$true}
    inventario=$inventory
}
$deltaPath=Join-Path $PSScriptRoot 'frontend-prumo-dev02-diagnostico-lucas-20261008.json'
if(Test-Path -LiteralPath $deltaPath){throw 'DEV02_RECIBO_EXISTENTE_PRESERVADO'}
[IO.File]::WriteAllText($deltaPath,($receipt|ConvertTo-Json -Depth 12),[Text.UTF8Encoding]::new($false))
$executedSha=$actual.auxiliar.sha256
$finalSha=(Get-FileHash -LiteralPath $helperPath -Algorithm SHA256).Hash
$actualSha=(Get-FileHash -LiteralPath $actualPath -Algorithm SHA256).Hash
$deltaSha=(Get-FileHash -LiteralPath $deltaPath -Algorithm SHA256).Hash
$markdown=@"
# D31-DEV02 — guarda atual Prumo

**Bloqueada por falha TLS atual.** Uma unica Open em $($actual.aberturaInicioUtc), PID $($actual.pidGuarda), $($actual.aberturaElapsedMs) ms. SQL/native $($actual.erro.sqlNumber), 0x800B0110: $nativeMeaning. Nenhum SELECT, API, backend, frontend ou fixture real. Identidade/alvo real, permissoes, catalogo e historico permanecem nao confirmados. Canal proprio WMSDEV DPAPI/ACL carregado somente em memoria e descartado.

TLS configurado Mandatory, TrustServerCertificate=false, certificado publico fixado SHA $($actual.tls.certificateSha256), thumbprint $($actual.tls.thumbprint), validade ate $($actual.tls.notAfter). Leitura publica local: CN=SSL_Self_Signed_Fallback, nenhuma extensao; isso sozinho nao prova a origem da recusa. Nao houve recaptura, fallback, bypass ou alteracao de servidor/runtime.

O incidente D29 falhou em prelogin/timeout; a tentativa atual falhou na validacao TLS. Nao atribuir a causa historica a esta observacao. Responsavel TLS deve analisar e fornecer encaminhamento seguro; alteracao de certificado/servidor/acessos/runtime compartilhado exige decisao material e nao foi feita. Nao repetir sonda nem iniciar backend.

## Provas e hashes

- JSON da unica tentativa: frontend-prumo-dev02-guarda-lucas-20261008.json, SHA $actualSha.
- Fonte efetivamente executada preservada: prumo-dev02-guarda-executada-20261008.ps1, SHA $executedSha.
- Auxiliar final para revisao: infra/dev02/guarda-wmsdev.ps1, SHA $finalSha.
- Complemento sanitizado: frontend-prumo-dev02-diagnostico-lucas-20261008.json, SHA $deltaSha.
- Parse local sem erro; V1-V10 CRCs locais iguais ao oraculo historico, sem consulta ao banco; comparacao offline com Flyway-core 12.4.0 instalado. Correcao BOM/CRC posterior a tentativa foi conferida sem SQL; valores CRC da r01 nao constituem prova de divergencia real do historico.
- CLI bloqueado em prova local de destino ja existente: stdout JSON, stderr vazio, exit20, zero chamada da guarda/Open. Arquivo prumo-dev02-cli-failclosed-local.json. A tentativa real inicial foi chamada por shell pai que reportou exit1; exitcode filho nao foi capturado naquele comando, por isso nao declarado como exit20 comprovado real.
- CSS, DEV01 e vite.config.ts preservados pelos hashes do inventario. Nenhuma suite/rebuild geral ou fonte de colega alterada.

## Contrato para Farol/Cedro

PowerShell 5.1, CLI sem alvo/credencial: powershell.exe -NoLogo -NoProfile -NonInteractive -File infra/dev02/guarda-wmsdev.ps1. EvidencePath opcional nao sensivel, create-only; destino invalido/existente recusa antes da guarda. Por dot-source, Invoke-WmsDev02Guard sem argumentos retorna objeto sanitizado. CLI exit0 apenas apos identidade/TLS/permissoes/catalogo/historico atuais completos e descarte seguro; exit20 bloqueia BE/FE. Nao armazenar/reaproveitar PASS, nem liberar por bool/variavel/listener/recibo antigo.

Schema: guardaRealAprovada, estado, motivo, inicioUtc/observadoEm UTC, pidGuarda; alvo banco/login/usuario/servidor/versao/sessao SQL/estado; tls encrypt/trustServerCertificate/certificateSha256/thumbprint/notAfter/handshakeConcluido/confirmacaoAtual/truststoreSha256; direitos server/database/roles/serverRoles/ownership/schema/explicit/objects/columns; catalogo tabelas/colunas/checksInvalidos/fksInvalidas/indicesDesabilitados; historico installed_rank/version/type/script/checksum/success; erro codigo/fase/sqlNumber/nativeErrorCode/tipos/marcadores. Campos nao conferidos null.

**Agora nao executar novamente a guarda real.** Farol/Cedro/Lume podem verificar propagacao/failclosed isolados; unica tentativa autorizada encerrada. Nenhum sa/PROD/DML/DDL/grant/restart/kill/alteracao global/ETL/Hermes/publicacao.
"@
$mdPath=Join-Path $PSScriptRoot 'frontend-prumo-dev02-guarda-lucas-20261008.md'
if(Test-Path -LiteralPath $mdPath){throw 'DEV02_MD_EXISTENTE_PRESERVADO'}
[IO.File]::WriteAllText($mdPath,$markdown,[Text.UTF8Encoding]::new($false))
$log=@"
REAL (unica): powershell.exe -NoLogo -NoProfile -NonInteractive -File infra/dev02/guarda-wmsdev.ps1 -EvidencePath frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json
Resultado preservado $actualSha; tentativa $($actual.aberturaInicioUtc); PID$($actual.pidGuarda); $($actual.aberturaElapsedMs)ms; native0x800B0110; zero SELECT/API. Shell pai exit1; codigo filho nao capturado. Nenhuma repeticao.
OFFLINE: Parser.ParseFile helper, errors=$($errors.Count); Get-WmsDev02Migrations V1-V10 todos CRCs iguais ao oraculo. javap Flyway ChecksumCalculator e java prumo-dev02-checksum-local.java V10 -562012523; somente fontes locais.
OFFLINE CLI: rejeicao create-only antes Invoke-WmsDev02Guard; filho PS5.1 capturado exit$($cli.exitCode), stderr vazio, JSONfalse; sem Open.
Consolidacao: powershell.exe -NoProfile -File frontend/evidencias/prumo-dev02-recibo.ps1; nenhum SQL/API/segredo em stdout.
"@
[IO.File]::WriteAllText((Join-Path $PSScriptRoot 'prumo-dev02-provas.log'),$log,[Text.UTF8Encoding]::new($false))
[pscustomobject]@{guardaRealAprovada=$false;helperSha256=$finalSha;realReceiptSha256=$actualSha;deltaReceiptSha256=$deltaSha;mdSha256=(Get-FileHash -LiteralPath $mdPath -Algorithm SHA256).Hash;parseErrors=$errors.Count}|ConvertTo-Json -Compress
