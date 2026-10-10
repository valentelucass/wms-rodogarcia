# Login e administração de usuários — D32

Pedido direto de Lucas em 09/10/2026; ligação BE04-AUTH01 → FE03-AUTH01 e launcher D31-DEV02. Implementação própria no Spring Security, sem provisionar Keycloak. A mudança substitui a pendência anterior de escolha do provedor; não transforma validação local em implantação SQL Server.

## Correção D32-DEV05 em 09/10/2026

O primeiro login real retornava401 porque o construtor legado do encoder Spring usava SHA1, incompatível com o hash SHA256 produzido pelo configurador. O backend agora declara `PBKDF2WithHmacSHA256` explicitamente. A configuração protegida existente é compatível e não precisa ser refeita. Encerrar o console antigo com Ctrl+C, executar `iniciar-dev.bat` novamente e atualizar a página para usar o novo JAR `target-auth-d32-loginfix`.

Regressão executa `PrepararLogin.java` de verdade com senha fictícia UTF-8; falhou antes e passou depois. A fixture HTTP/navegador deriva seu hash por JCA independentemente do encoder Spring. Vinte testes backend passaram, incluindo troca inicial, administrador delegado e redefinição com nova troca; build frontend aprovado e favicon SVG respondeu200. [Resultado local](../orchestracao/.runtime/login-d32/dev05-resultado.json). O agente preservou a instância do usuário, sem nova operação SQL ou consumo da senha real. Primeiro login autenticado no SQL Server permanece para a validação pelo usuário.

O aviso React DevTools é informativo. A renovação pode retornar401 ao abrir a página sem sessão; isso não é falha na conferência da senha. O ícone da aba agora é servido em `/favicon.svg`.

## Uso do sistema

### Acesso pelo Dev Tunnel — D32-DEV07

A configuração `infra/dev/acesso-dev.json` contém somente `modo: tunnel`, sem endereço público. Use o link HTTPS atual da porta25581 no painel Portas do VS Code. Recriar o túnel e mudar seu endereço não exige editar configuração ou reiniciar o WMS; será necessário entrar novamente porque os cookies pertencem ao endereço acessado. Para usar exclusivamente o acesso local, definir `modo: local` e reiniciar o BAT. Mudanças de modo não alteram senha/chave nem exigem repetir o configurador.

Nesse modo, cookies de sessão e CSRF usam Secure/HttpOnly/SameSite=Strict. Se a origem pública é preservada, ela deve usar HTTPS, pertencer ao domínio Dev Tunnels e coincidir exatamente com Host ou com X-Forwarded-Host acompanhado de X-Forwarded-Proto=https. Essa interpretação é limitada ao proxy loopback e perfil sqlserver-dev/test, com backend ligado em127.0.0.1. Origem de outro destino, HTTP público, domínio parecido, caminho ou credencial na URL são recusados. Não há liberação apenas por sufixo de domínio, e o CSRF continua obrigatório. Quando o Dev Tunnel reescreve Origin para `http://localhost:<porta>`, permanece o alias exato dessa porta. [Comportamento documentado pelo projeto Microsoft](https://github.com/microsoft/dev-tunnels/issues/284).

O403 anterior foi confirmado como `ORIGEM_INVALIDA` por POST sem senha/token à rota CSRF, sem operação de negócio. A primeira correção DEV06 fixou a URL; DEV07 substitui essa configuração conforme pedido expresso. Para carregar o novo código, reiniciar uma vez o console e atualizar a página. Mudanças posteriores apenas no endereço do túnel não precisam desse reinício. A instância do usuário é preservada pelo agente.

Na troca de senha, a tela informa antes do envio que a nova senha deve ter12–128 caracteres e diferir da atual. A API também valida essas regras. O400 isolado relatado não permite concluir qual regra foi recusada sem a mensagem mostrada na tela; não solicitar nem registrar a senha para diagnosticar.

### Contas e permissões

- A conta principal é `desenvolvedor@rodogarcia.com.br`. Ela nasce como Gestor e administrador de usuários. Não pode ser excluída, desativada, renomeada ou ter seus privilégios reduzidos; outros administradores não podem redefinir sua senha. O próprio titular altera a senha em **Minha senha**.
- Em **Usuários e acessos**, um administrador cria uma conta com nome, e-mail, perfil operacional, vínculos de cliente/armazém e senha temporária. Marcar **Administrador de usuários** permite que essa pessoa também gerencie contas. Essa permissão é separada de Gestor/Supervisor/Operação.
- Todas as contas, inclusive a principal, precisam trocar a senha temporária antes de usar o WMS. **Redefinir senha** encerra os acessos antigos e repete essa exigência. Não há recuperação ou exibição de senhas existentes.
- Desmarcar **Acesso ativo** bloqueia a pessoa, preservando seu histórico. Não existe endpoint de exclusão física. Um administrador não pode retirar o próprio acesso administrativo; outro administrador deve fazê-lo.
- Alterações de permissões e senha invalidam as sessões anteriores. O e-mail é a identidade de entrada e não é editável nesta etapa. O painel recusa sobrescrever uma revisão de cadastro desatualizada.

## Proteções implementadas

Spring `PasswordEncoder` com PBKDF2-HMAC-SHA256, sal aleatório de 16 bytes e 600.000 iterações. Senhas de 12 a 128 caracteres; sem corte silencioso, sem senha em log/DTO de resposta e sem recuperação reversível. A configuração inicial armazena somente hash e chave privada em um envelope DPAPI do usuário Windows, fora do repositório. Reiniciar o sistema não restaura a senha inicial.

JWT assinado RS256 com chave RSA de 3.072 bits, duração de cinco minutos, emissor/audience próprios, `iat`/`exp`, ID de sessão e versão do usuário. A chave pública está em `GET /api/auth/jwks`; a chave privada nunca é retornada. O emissor `https://wms.localhost` é o identificador do emissor próprio DEV: a API verifica a chave local, sem descoberta ou chamada a um IdP externo.

O navegador mantém o access token somente em memória. A renovação usa 256 bits aleatórios em cookie `WMS_REFRESH` HttpOnly/SameSite=Strict, limitado a `/api/auth`; só o SHA-256 do token é persistido. Cada renovação consome o token anterior. Reuso detectado revoga a família de sessão; concorrência recebe tratamento transacional com locks reais JPA. Janela de renovação de 30 minutos e duração absoluta de oito horas; sessões com senha temporária duram no máximo 15 minutos. Logout revoga a sessão. Senha/permissões/desativação alteram a versão validada no servidor em cada acesso; os serviços operacionais também revalidam a identidade local.

`/api/v1` continua aceitando somente Bearer no cabeçalho. `/api/auth` aplica CSRF e origem exata às escritas, sem CORS aberto, autenticação por parâmetros ou tokens em localStorage/sessionStorage. Cookies Secure são obrigatórios por padrão; a exceção HTTP exige explicitamente perfil test/sqlserver-dev, bind `127.0.0.1` e origem `http://127.0.0.1`. A exposição fora do computador local exige HTTPS e configuração própria.

Cinco senhas incorretas bloqueiam a conta por 15 minutos, com contagem persistida mesmo quando o login retorna erro. Mensagem genérica não revela se a conta existe. Há limite adicional de 20 tentativas por IP/minuto, memória limitada e sem confiar em cabeçalhos de proxy recebidos do cliente. A implantação atual é de uma instância; múltiplas instâncias exigirão limite compartilhado e política de proxy revisada.

Eventos de criação, acesso, alteração de senha/permissão, logout e replay conservam IDs e data, sem credenciais ou tokens. As quatro tabelas novas não recebem DELETE; email/principal/IDs e eventos não recebem UPDATE pela aplicação. MFA, recuperação por e-mail, expurgo de sessões expiradas e rotação coordenada de chaves não foram implementados nesta etapa.

## Ativação DEV realizada em 09/10/2026

Por autorização D32-DEV04, a V11 foi aplicada via Flyway e os 22 direitos mínimos foram concedidos somente no WMS_DEV. Validação antes/depois conferiu alvo real, TLS, catálogo, direitos e histórico. Executor específico: `database/scripts/d32-ativar-login-dev.ps1`, cujo modo padrão `Plan` não conecta; o modo `Apply` foi usado somente nesta ativação autorizada. [Recibo administrativo](../orchestracao/.runtime/login-d32/ativacao-1330858b07e343dca09f887ba77240bf.json).

O usuário já configurou o material protegido. Para usar este ambiente, basta executar **`iniciar-dev.bat`**, manter o console aberto e abrir **http://127.0.0.1:25581**. Entrar com a conta principal e a senha escolhida no configurador; no primeiro acesso, trocar a senha. Não repetir a configuração nem aplicar novamente a atualização para cada inicialização.

BAT, backend e frontend reais foram conferidos no [run ce0bf9dd](../orchestracao/.runtime/frontend-integracao-dev-runs/ce0bf9dd46b3423ba148782a52cf212f/launcher.json). Navegador conferiu login desktop/mobile, CSS/CSP e respostas públicas200/rota protegida401. O CSS do login agora é uma folha externa também no DEV, compatível com a CSP. Instância de teste encerrada e portas liberadas; primeiro acesso e operações autenticadas com a senha real serão conferidos pelo usuário. [Resultado e limites](../orchestracao/.runtime/login-d32/dev04-resultado.json).

### Procedimento técnico de preparação de um ambiente novo

1. Aplicar, mediante autorização específica e procedimento protegido exclusivamente WMS_DEV, a [V11](../database/migrations/V11__login_usuarios_e_sessoes.sql). V1–V10 permanecem intocadas. Não usar o launcher administrativo DEV → PROD para este recorte.
2. Aplicar somente os direitos mínimos do [contrato D32](../database/contratos/D32-permissoes-login-dev.sql) à identidade WMSDEV. São 22 direitos adicionais por objeto/coluna; nenhuma role, DELETE ou administração. A guarda compara o estado esperado 68 tabelas/716 colunas e histórico V11 com a leitura atual. O oráculo D29 permanece imutável e é estendido somente em memória por `infra/auth/contrato-banco.ps1`.
3. Na pasta raiz do projeto, executar uma única vez `infra\auth\configurar-login-dev.bat`, informando a senha inicial diretamente no console com entrada oculta. O auxiliar fica junto aos arquivos de autenticação, fora da raiz, e resolve seus caminhos pelo próprio diretório. O configurador cria chave e hash e cifra o conjunto em `%LOCALAPPDATA%/Rodogarcia/WMS/auth-dev/login.clixml`, com ACL privada; não sobrescreve configuração existente e não acessa SQL. A senha solicitada pelo responsável não é transcrita em nenhum arquivo. Falta desse material interrompe a inicialização antes do SQL com `AUTH_NATIVE_CONFIGURATION_REQUIRED`; a mensagem mostra o comando acima. Essa configuração resolve somente esse pré-requisito, não aplica a V11 nem os direitos pendentes.
4. Depois do build validado e recibo `backend/evidencias/login-d32-preparo.json`, executar `iniciar-dev.bat`. O modo normal usa o login próprio, mantém sqlserver-dev/validate/init-never/Flyway desligado, faz a guarda atual e inicia somente processos próprios. `WMS_AUTH_MODE=externo` conserva a opção explícita do Resource Server anterior.
5. Entrar, trocar a senha temporária e cadastrar a equipe. Conferir o percurso navegador → API → WMS_DEV com nova evidência antes de declarar integração real concluída.

O procedimento acima foi inicialmente apenas preparado e testado com H2. A autorização D32-DEV04 liberou a ativação DEV registrada nesta seção; nenhum PROD ou serviço SQL Server foi alterado. Inicializações normais mantêm migrations desligadas e não repetem concessão de direitos.

## Acesso à visão geral e pacote DEV — BE02-DEV-ARTEF01

Em 09/10/2026, Lucas relatou “Seu acesso não permite consultar este contexto” usando a conta principal. O JAR indicado pelo recibo do DEV em execução não incluía a API da visão geral nem a liberação da rota. A fonte atual já permite ao Gestor consultar todos os clientes/armazéns; não é necessário mudar a conta, a senha ou seus vínculos.

`iniciar-dev.bat` agora compara o pacote preparado com o hash das fontes principais e do `pom.xml`, confere o hash do JAR e a presença das APIs antes da guarda SQL. Pacote antigo/alterado é recusado. A preparação local é feita por `powershell -NoProfile -File infra\dev\preparar-backend-dev.ps1`: build com JDK 21 e testes de login/visão geral/arquitetura em H2 isolado, sem SQL Server, migrations ou segredo de autenticação no build. Os recibos anteriores são preservados em `orchestracao/.runtime/backend-dev-builds/`.

Preparar um pacote não troca o processo já aberto. Depois da preparação aprovada, fechar o console DEV e executar `iniciar-dev.bat` carrega a nova versão e repete as guardas normais. Recarregar apenas o navegador não troca o backend. A conferência local inclui login legítimo da conta principal com contextos geral, por armazém e por cliente/armazém; os testes existentes preservam as recusas por perfil/alcance e a proteção da conta principal. Integração SQL Server e versão efetivamente reiniciada continuam evidências separadas.

## Contratos HTTP

| Rota | Efeito |
| --- | --- |
| `GET /api/auth/csrf` | Token CSRF da origem atual, sem cache |
| `GET /api/auth/jwks` | Somente chave pública |
| `POST /api/auth/entrar` | E-mail/senha → JWT curto, usuário e cookie de renovação |
| `POST /api/auth/renovar` | Rotação do refresh, sem repetição automática após erro |
| `POST /api/auth/sair` | Revoga sessão e apaga cookie |
| `GET /api/auth/eu` | Identidade atual do usuário autenticado |
| `POST /api/auth/senha` | Senha atual + nova → revoga todos os acessos; exige novo login |
| `GET /api/auth/usuarios?pagina=0` | Lista de 20 usuários, somente administradores |
| `POST /api/auth/usuarios` | Cria usuário com senha temporária e permissão administrativa opcional |
| `PUT /api/auth/usuarios/{id}` | Atualiza nome/perfil/vínculos/admin/ativo com revisão |
| `POST /api/auth/usuarios/{id}/senha` | Redefine senha temporária com revisão |

## Verificação

Entrega local aprovada em 09/10/2026: 19 testes backend (18 de API/segurança/arquitetura e um de navegador com quatro jornadas), 359 testes frontend, tipagem/lint/build, 9 verificações de contratos e configuração fictícia e 20 do helper. A inspeção visual confirmou desktop e troca de senha em 360px, com teclado e sem tokens no storage. [Recibo da entrega](../orchestracao/.runtime/login-d32/resultado.json). A chamada nativa `fetch` foi corrigida após falha observada no navegador e a jornada completa passou depois da correção. A atualização graphify foi recusada pelo limite de redução do grafo; mapa anterior preservado.

As evidências da entrega ficam em `orchestracao/.runtime/login-d32/` e no diretório de build próprio `backend/target-auth-d32`. `LoginIntegrationTest` usa HTTP real e H2 efêmero; `LoginBrowserTest` usa Chromium, frontend e backend reais com H2 efêmero. Os scripts locais usam somente material fictício e comparação de contratos em memória. Nenhuma dessas provas comprova dialeto/permissões do SQL Server.

Para reproduzir a jornada visual, primeiro executar `npm run check` em `frontend`; depois, com JDK 21, executar em `backend`: `mvnw.cmd -B -ntp -Dwms.build.directory=target-auth-d32 -Dwms.browser.tests=true -Dtest=LoginBrowserTest test`. Requer Node, dependências frontend e Chrome instalado; usa perfil temporário próprio e porta local livre 59999. A propriedade explícita mantém essa dependência de navegador fora dos testes backend comuns. Os demais testes desta entrega são `LoginIntegrationTest,ArquiteturaTest,D30BearerCookieTest,D30JwtLimitesTest`.

Referências técnicas consultadas: [armazenamento de senha no Spring Security](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html), [CSRF no Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html), [sessões OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Session_Management_Cheat_Sheet.html) e [autenticação OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html).
