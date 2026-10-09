# Desenvolvimento frontend — D31-DEV02

**Atualização D32-DEV04, 09/10/2026:** frontend e backend reais iniciados pelo BAT após V11/direitos WMS_DEV e guarda aprovados. Login desktop/mobile e API pública conferidos; acesso anônimo à rota protegida retorna401. CSS de autenticação servido por folha externa, sem violar CSP no DEV. Primeiro acesso com a senha real fica para o usuário. Rotas de autenticação em `/api/auth`, token em memória e renovação HttpOnly. Consulte o [guia vigente](../../docs/43-login-e-administracao-de-usuarios.md). Diagnósticos de AUTH/TLS abaixo são históricos.

## Histórico e contrato D31 preservados

O padrão npm run dev é REAL. npm run dev:ficticio é um exercício separado. Nenhuma falha de configuração, guarda, processo ou autenticação substitui o modo real por respostas fictícias.

Em 09/10/2026 UTC, a integração continua impedida por evidências atuais: as três referências públicas WMS_OIDC_ISSUER/JWK_SET_URI/AUDIENCE estão ausentes e não há provider/fluxo de entrada real aprovado. A única abertura Prumo falhou no TLS, código -2146762480, antes de qualquer SELECT; alvo e identidade não foram confirmados. [Guarda atual](../evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json). Nenhuma nova abertura, BE/API ou servidor com proxy ativo foi executado por Lume. O launcher Farol também recusou AUTH antes de SQL. Os impedimentos não são inferidos de D29.

## Comando e contrato do launcher

**FE02-DEV-CON02, 09/10/2026:** a abertura SQL aguarda até120s; o prazo total da guarda é240s, com indicação de andamento a cada10s. Os prazos ficam em `infra/dev/espera-conexao.ps1`, compartilhado pelo launcher e pela guarda. Continua uma única abertura, sem retry ou fallback, antes de qualquer processo de aplicação. A extensão do prazo não comprova disponibilidade do SQL nem altera configuração do servidor.

O operador usa iniciar-dev.bat na raiz. Farol possui o BAT e infra/dev/iniciar-dev.ps1; Cedro possui o helper backend e Prumo a guarda. Lume possui esta configuração/frontend. O launcher precisa concluir seu preflight público de autenticação, uma guarda atual PASS e o backend próprio READY, antes de iniciar frontend. Um listener ou HTML200 não comprova integração autenticada.

Em frontend/:

- npm run dev = vite --config vite.dev.config.ts --mode real.
- npm run dev:ficticio = vite --config vite.dev.config.ts --mode ficticio.
- WMS_FE_PORT informa uma porta própria livre. Não há fallback de porta nem uso dos processos5178/5188/5189.
- WMS_DEV_BACKEND_URL é a origem EXATA http://127.0.0.1:porta confirmada no processo BE. Sem default8080, path, credencial, query ou fragmento.
- WMS_DEV_READY_PATH aponta integracao-pronta.json no diretório UUID do run do launcher; WMS_DEV_READY_SHA256 vincula seus bytes atuais. A chain informa publicIdentity (issuer/jwkSetUri/audience), sem token, conta ou segredo. O ambiente FE não precisa de WMS_OIDC_*.
- Um VITE_DATA_MODE conflitante com o comando é recusado; não muda silenciosamente o padrão real.

O desenvolvimento real usa /api como proxy same-origin, changeOrigin=false e sem rewrite. As rotas existentes já incluem /api/v1. Nenhum caminho de login/token foi criado. CSP e CORS não foram relaxados. Em ficticio, server.proxy é ausente e /api responde503 localmente sem upstream.

## Validação da cadeia

A configuração lê somente os recibos locais e hashes informados pelo launcher. Exige projeto/run/porta/destino coerentes, recibos no mesmo diretório sem redirecionamento de arquivo e observações com até10min. Compara SHA de chain/guarda/backend/helper/JAR/config/Java. Não lê credenciais nem conecta SQL.

Guarda: guardaRealAprovada=true e estado GUARDA_ATUAL_APROVADA, PID igual à chain, início/fim atuais, alvo WMS_DEV/WMSDEV/ONLINE, identidade confirmada, TLS Mandatory sem TrustServerCertificate, handshake/confirmação atuais, evidências de direitos/catálogo/histórico e checks aprovados. Não observado/null não é PASS. O proprietário da guarda valida a restrição SQL completa; frontend verifica procedência e consistência, não certifica direitos por conta própria.

Backend: natureza D31_DEV02_BACKEND_READINESS, estado READY, profile sqlserver-dev, WMS_DEV/WMSDEV, hashes dos bytes atuais e guard plano sha256/processId/helperSha256/finishedUtc iguais ao original. Exige status200 wms-rodogarcia/DISPONIVEL e PID/início/porta/listenerOwned vinculados. Só depois consulta metadados locais do processo informado: PID, início, caminho Java e listener loopback proprietário. Não faz sonda HTTP. Nenhum processo é encerrado/reiniciado.

A configuração de AUTH pública exige issuer/JWK HTTPS sem credenciais/query/fragmento e audience válida. Isso não emite token nem comprova usuário/sessão. Não há exceção incondicional histórica após uma cadeia válida: fixtures locais coerentes podem preparar o proxy em testes com inspetor fictício. Estes testes nunca geram recibo PASS no runtime real.

## Sessão e cliente

App escolhe o modo antes de executar hooks do exercício. Modo real/ausente/inválido não cria FictitiousTransport, perfilGestor nem contexto cliente1/armazém1. A página real informa sessão indisponível e não envia consulta operacional. Perfil/alcance devem vir da identidade real, e autorização continua backend.

O backend atual é ResourceServer RS256 stateless: recebe Bearer, não emite token e não autentica cookie. Issuer/JWK/audience não resolvem sozinhos o fluxo de entrada. O adaptador HTTP existente exige Bearer em memória e recusa antes de fetch quando ausente. Não há formulário para copiar token, senha, armazenamento de token, provider fictício ou conta criada por esta preparação. A obtenção/renovação real permanece uma fronteira concreta a resolver com o responsável; não foi disfarçada por mocks.

O cliente preserva JSON numérico exato, status/requestId, abort de contexto, 401 vigente, resultado incerto em escrita5xx ou2xx incompatível e comando congelado/replay. Provas novas usam fetcher local, nunca API real. Não é necessário repetir a suíte integral MARCO03 para estas fronteiras.

## Preservação e provas

MARCO03 foi copiado antes das edições para evidencias/dev02/marco03-preservado:183arquivos, fonte C58B6410…, manifesto C5CB8A3A…,8625referências sem divergência. Recibos, checks, snapshots, screenshots e os processos históricos permanecem preservados. CSS e módulos de jornadas não foram editados.

Os novos focais verificam configuração real/default, fictício explícito sem proxy, recusa da guarda ATUAL bloqueada, cadeia/hash/contexto/TLS/auth/processo incompatíveis e ausência de sessão/HTTP/fallback. Tipagem/lint/build e observação local de navegador são registrados em evidencias/dev02. Qualquer navegador local é prova de apresentação/isolamento, não roundtrip FE→BE→WMS_DEV.

Após correção autorizada dos impedimentos, uma guarda nova/currente PASS, backend próprio confirmado e mecanismo real de sessão precisam preceder a prova operacional por navegador. Não reaproveitar recibo histórico como aprovação, reabrir SQL automaticamente ou usar sa/PROD/bypassTLS. O recibo central de integração pertence a Farol.

A preparação DEV01 e suas provas permanecem históricas no snapshot MARCO03 e nos recibos Prumo originais. Esta correção substitui somente o padrão fictício e o bloqueio histórico incondicional da configuração; não afirma integração executada.
