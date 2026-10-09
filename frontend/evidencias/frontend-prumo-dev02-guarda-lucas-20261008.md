# D31-DEV02 â€” guarda atual Prumo

**Bloqueada por falha TLS atual.** Uma unica Open em 2026-10-09T00:51:02.8522544Z, PID 32144, 454.74490000000003 ms. SQL/native -2146762480, 0x800B0110: Certificado inválido para o uso solicitado. Nenhum SELECT, API, backend, frontend ou fixture real. Identidade/alvo real, permissoes, catalogo e historico permanecem nao confirmados. Canal proprio WMSDEV DPAPI/ACL carregado somente em memoria e descartado.

TLS configurado Mandatory, TrustServerCertificate=false, certificado publico fixado SHA 93F2657D84E49F2FC3573AADA7348C41F3287BB951A1736882BD0540F34F415B, thumbprint FD780A09454872C2DBF677600455753573F13205, validade ate 2056-10-05T12:36:39.0000000Z. Leitura publica local: CN=SSL_Self_Signed_Fallback, nenhuma extensao; isso sozinho nao prova a origem da recusa. Nao houve recaptura, fallback, bypass ou alteracao de servidor/runtime.

O incidente D29 falhou em prelogin/timeout; a tentativa atual falhou na validacao TLS. Nao atribuir a causa historica a esta observacao. Responsavel TLS deve analisar e fornecer encaminhamento seguro; alteracao de certificado/servidor/acessos/runtime compartilhado exige decisao material e nao foi feita. Nao repetir sonda nem iniciar backend.

## Provas e hashes

- JSON da unica tentativa: frontend-prumo-dev02-guarda-lucas-20261008.json, SHA 8CDD5ACF4BE9A7CD06259B7B640E9F3C3A242B8179BCA8D0B4533EFEFB4AC2D8.
- Fonte efetivamente executada preservada: prumo-dev02-guarda-executada-20261008.ps1, SHA A3DE06DD32CF233ECBFD0B12714A4E3D0AF43A357209EBA0D85334DD5E69EACC.
- Auxiliar final para revisao: infra/dev02/guarda-wmsdev.ps1, SHA 1376BE13C220631057B47227213556A01D456CE3D9F2B59CC857BED58F702E5B.
- Complemento sanitizado: frontend-prumo-dev02-diagnostico-lucas-20261008.json, SHA F2774A8F7E37C2BA08A1491142F29F5FF55EB81299FD020660F78ED196F685F3.
- Parse local sem erro; V1-V10 CRCs locais iguais ao oraculo historico, sem consulta ao banco; comparacao offline com Flyway-core 12.4.0 instalado. Correcao BOM/CRC posterior a tentativa foi conferida sem SQL; valores CRC da r01 nao constituem prova de divergencia real do historico.
- CLI bloqueado em prova local de destino ja existente: stdout JSON, stderr vazio, exit20, zero chamada da guarda/Open. Arquivo prumo-dev02-cli-failclosed-local.json. A tentativa real inicial foi chamada por shell pai que reportou exit1; exitcode filho nao foi capturado naquele comando, por isso nao declarado como exit20 comprovado real.
- CSS, DEV01 e vite.config.ts preservados pelos hashes do inventario. Nenhuma suite/rebuild geral ou fonte de colega alterada.

## Contrato para Farol/Cedro

PowerShell 5.1, CLI sem alvo/credencial: powershell.exe -NoLogo -NoProfile -NonInteractive -File infra/dev02/guarda-wmsdev.ps1. EvidencePath opcional nao sensivel, create-only; destino invalido/existente recusa antes da guarda. Por dot-source, Invoke-WmsDev02Guard sem argumentos retorna objeto sanitizado. CLI exit0 apenas apos identidade/TLS/permissoes/catalogo/historico atuais completos e descarte seguro; exit20 bloqueia BE/FE. Nao armazenar/reaproveitar PASS, nem liberar por bool/variavel/listener/recibo antigo.

Schema: guardaRealAprovada, estado, motivo, inicioUtc/observadoEm UTC, pidGuarda; alvo banco/login/usuario/servidor/versao/sessao SQL/estado; tls encrypt/trustServerCertificate/certificateSha256/thumbprint/notAfter/handshakeConcluido/confirmacaoAtual/truststoreSha256; direitos server/database/roles/serverRoles/ownership/schema/explicit/objects/columns; catalogo tabelas/colunas/checksInvalidos/fksInvalidas/indicesDesabilitados; historico installed_rank/version/type/script/checksum/success; erro codigo/fase/sqlNumber/nativeErrorCode/tipos/marcadores. Campos nao conferidos null.

**Agora nao executar novamente a guarda real.** Farol/Cedro/Lume podem verificar propagacao/failclosed isolados; unica tentativa autorizada encerrada. Nenhum sa/PROD/DML/DDL/grant/restart/kill/alteracao global/ETL/Hermes/publicacao.

## Complemento publico solicitado por Farol

O codigo atual 0x800B0110 foi interpretado pela mensagem nativa do Windows: certificado invalido para o uso solicitado. A origem exata da recusa nao foi provada. Nao inferir que ausencia de EKU/SAN seja a causa so porque o arquivo existente nao tem extensoes.

**Cliente e peer sao evidencias diferentes.** O campo tls.certificateSha256 no JSON identifica SHA256 dos bytes do PEM cliente passado a ServerCertificate: 93F2657D84E49F2FC3573AADA7348C41F3287BB951A1736882BD0540F34F415B. SHA256 do certificado DER local: e3a5a7061fb56c225f93cc330cd733f5911f7d3f420d608ebe9afa7371ace276, correspondente ao recibo protegido. Nenhum deles foi observado como certificado do peer nesta tentativa; handshakeConcluido=false. EncryptMandatory e TrustServerCertificatefalse sao configuracao comprovada, nao prova de handshake aprovado.

O recibo publico TLS tem updatedAt=2026-10-06T20:40:45.8889012Z, PID historico=65088, started historico=2026-10-05T12:36:37.4760010Z. Nao foi verificado PID atual, nem recapturado certificado. Subject e issuer do arquivo local CN=SSL_Self_Signed_Fallback; validade vigente e nenhuma extensao identificada. Isso descreve o arquivo cliente, sem certificar origem atual do servidor.

Nao foi comprovado ajuste concreto do cliente que elimine a falha. Encaminhamento: responsavel TLS analisar recusa/proveniencia. Se a solucao exigir certificado/trust/servidor/acessos/sharedruntime, falta decisao material especifica; nenhum bypass ou globaltrust foi aplicado. STOP preservado, sem nova sonda. Analise publica: prumo-dev02-tls-publico-lucas-20261008.json, SHA 0793D7943EDCF67534E46BF4AB794D06BCAF3FCC2805FACA374FE0EB3E9EE172. MD anterior preservado em prumo-dev02-guarda-md-antes-analise-publica.md.