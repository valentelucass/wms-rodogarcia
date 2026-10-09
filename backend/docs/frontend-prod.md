# Backend do launcher de produção

PROD-LAUNCH01 prepara um candidato isolado da fonte corrente. O launcher único de Farol é `iniciar-prod.bat`; `--preparar` compila candidatos sem identidade protegida, guarda SQL ou subida. A operação normal permanece condicionada aos canais exclusivos PROD e à guarda atual de Prumo. As portas acordadas são backend **25590** e frontend **25591**, sempre verificadas livres antes da subida. Nenhum processo existente é encerrado por porta.

## Contrato dos auxiliares

Dot-source dos três arquivos em `backend/scripts/prod` apenas declara funções.

| Arquivo/função | Entrada e resultado |
| --- | --- |
| `build-backend-prod.ps1` / `Build-WmsProductionBackend` | `-CandidateDirectory` vazio dentro de `orchestracao/.runtime/launcher-producao/`, opcional `-JavaPath` JDK 21. Captura fonte principal, wrapper e POM; package offline sem testes/Boot/banco. Retorna `jar`, `config`, `java`, `sourceManifest`, `sourceSha256`, `CandidateDirectory`, `ReceiptPath`, `ReceiptSha256`. Cada entrada tem caminho absoluto e SHA256. Drift da fonte ou falha recusa promoção; candidatos anteriores são preservados. |
| `identity-prod.ps1` / `Get-WmsProductionIdentity`, `Assert-WmsProductionIdentity` | Somente metadados públicos do canal próprio `auth-prod`; não abre DPAPI, cria chave, usuário ou senha. Ausência emite erro codificado antes de SQL. |
| `start-backend-prod.ps1` / `Start-WmsProductionBackend` | `-Port -RunDirectory -GuardReceipt -GuardSha256 -GuardProcessId -GuardHelperSha256 -Artifact -Identity`, e `-DatabaseCredential` protegido WMSPROD. Valida bytes/tempo/PID/fonte da guarda atual, finalidade PROD, TLS/permissões/catálogo/histórico, candidato e canais antes de Java. Não executa uma sonda SQL própria. |
| `Stop-WmsProductionBackend` | Somente `-Handle` com o resultado completo devolvido por Start. Confere RunId e referência do processo registrado. Não aceita PID/porta externos. |

O build registra seu processo próprio antes de Start e encerra/aguarda o handle em falhas de leitura. Filhos só são encerrados por referências cuja filiação e data de criação foram conferidas. Falha de prova de filiação é reportada; processos sem prova são preservados. Falha ao encerrar filho comprovado mantém seu handle e erro, sem alegar cleanup completo. A gravação do recibo não pode pular cleanup/dispose. Start registra seu handle antes de BeginOutputReadLine; falhas de leitura/publicação também encerram e aguardam somente o processo criado por essa chamada. Processo que não iniciou é descartado sem consultar HasExited.

## Perfil e precedência efetiva

Único perfil ativo: `sqlserver-prod`. A base carregada explicitamente é `application.properties` mais `application-sqlserver-prod.properties`. `application-frontend-prod.properties` é **complemento externo** por `spring.config.additional-location`, não segundo perfil.

O processo-filho recebe ambiente limpo. Os argumentos fixos têm precedência sobre variáveis: bind loopback, `ddl-auto=validate`, `generate-ddl=false`, `sql.init.mode=never`, Flyway/Liquibase desabilitados, banco `WMS_PROD`, login `WMSPROD`. Os validadores não permitem substituição do alvo/ação por propriedades JPA alternativas. O complemento não declara `schema-generation.database.action`; a chave, mesmo `none`, é recusada pelo validador existente. Nenhum relaxamento DEV foi introduzido. O método DEV `validarAlvo()` continua separado de `validarAlvoProducao()`.

Auth nativa D32 permanece intacta: origin fixo `https://wms.rodogarcia.com.br`, issuer público esperado `https://wms-api.rodogarcia.com.br`, JWKS `/api/auth/jwks`, Secure=true e proxy-origin vazio. O launcher força `--wms.auth.bootstrap-hash=`. `LoginService.iniciarPrincipal` recusa principal ausente com hash vazio antes de criar conta; principal existente também precisa passar suas verificações. Existência/estado do principal PROD não foi observado nesta tarefa.

## Canais e impedimentos atuais

O canal AUTH próprio esperado fica em `%LOCALAPPDATA%/Rodogarcia/WMS/auth-prod`: `identity.json` público e `login.clixml` protegido. Os metadados públicos exigem schema=1, native=true, environment=PROD, database=WMS_PROD, origin/issuer/JWKS acima, audiência própria não DEV, secureCookie=true e proxyOrigin vazio. A audiência não foi provisionada nesta entrega. O material protegido só pode ser aberto no Start, após os pré-requisitos e ACL/owner conferidos, e precisa comprovar finalidade/issuer/audience PROD. Não há criador/provisionador de chave/bootstrap/conta nestes auxiliares. `auth-dev/login.clixml` e emissor/audiência DEV não são reutilizáveis.

Na observação de preparo não foram encontrados metadados/material AUTH PROD. Prumo também não descobriu canal restrito WMSPROD: guarda gate atual sem PASS operacional, zero Open/DPAPI. O binding futuro de `DatabaseCredential` pertence a Prumo/Farol e permanece pendente; root não deve fabricar credencial nem importar canal DEV. Senha e chave só passam por memória do processo-filho quando os pré-requisitos estiverem satisfeitos; não entram nos argumentos ou recibos. Este guia não autoriza provisionamento ou uma tentativa SQL.

## Readiness e linhagem

Readiness verifica listener do PID próprio e respostas públicas `/api/v1/status` e `/api/auth/jwks`, quando a subida for autorizada. `READY_PUBLIC` e HTTP200 não comprovam sessão, principal, negócio, HTTPS público ou integração operacional. Domínios são configuração esperada; Cloudflare/DNS/túnel/TLS público não foram configurados. Frontend produtivo e proxy pertencem a Lume/Farol; autenticação exige preservar Origin/CSRF e o transporte HTTPS da sessão.

Os primeiros candidatos usaram helper de build D07F e complemento 03DE; build PASS é prova de compilação, não de startup. JAR SHA256 `0CA6B667A46CADC9D88CB48EEFA39FACCBEF60E06A440E23610CDB7C750F66EF` e manifestos históricos continuam intactos. O delta externo DFB55F remove duas chaves redundantes, mantendo as três classes compiladas idênticas. Prova em `orchestracao/.runtime/launcher-producao/cedro/delta-cleanup01/external-config01/report.json`: configuração final aceita, 03DE recusada e negativos DDL/init/Flyway recusados, sem DataSource/Spring/JDBC/SQL. Não houve novo package por este delta. Candidatos antigos não recebem atualização silenciosa; Build futuro captura o complemento corrente e produz um novo manifesto. Reuso dos bytes do JAR requer vínculo explícito ao complemento externo efetivo, preservando seus hashes distintos.

Os 26 focais de configuração e 39 focais iniciais de helper ficam datados. Cleanup 001/002 tem 15 focais finais isolados e REDs históricos preservados; os 14 do primeiro delta ficam datados, sem soma. Configuração externa 003 tem 8 focais restritos. Nenhum deles é teste de banco PROD ou aceite operacional. Fonte/hash/contratos finais e limites estão nos artefatos próprios de Cedro; canônicos e recibo central são de Farol.
