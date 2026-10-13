# Launcher de produção Windows

## PM2-PROD01 — execução operacional atual, 13/10/2026

Produção passou para PM2 por pedido de Lucas. O launcher continua preparando candidatos e fornecendo guardas; o controlador `pm2-wms.bat` gerencia `wms-api-25590` e `wms-ui-25591`. [Guia atual](45-producao-pm2.md). Run `4573ed7613a6431c84839dd473e3d213`, ativação `2026-10-13T22:26:34.5950008Z`, API/UI online e HTTPS conferido. Evidência: `orchestracao/.runtime/pm2-prod-20261013/resultado.json`.

Pode fechar o terminal após a confirmação. As instruções de manter BAT aberto nos registros abaixo descrevem o modo standalone anterior. Para publicar fonte nova, `pm2-wms.bat atualizar`; para iniciar a versão selecionada após boot, `pm2-wms.bat iniciar`. Serviço de inicialização automática do Windows não instalado.

## FOOTER-PROD01 — horário da atualização publicado, 13/10/2026

Lucas pediu data/hora da última atualização bem-sucedida ao lado dos direitos reservados e autorizou subir produção. Rodapé compartilhado consulta recibo validado da versão ativa, em Brasília; compilar sozinho não altera o horário. Uma aba de versão anterior recebe aviso para atualizar, sem recarga automática. DEV não mostra informação de produção.

Validação: 588 testes frontend, 16 do servidor, 24 do launcher, tipagem/lint/build e Chrome desktop/mobile claro/escuro. Run ativo `e2165b2e2e3a4f8ca7c84aab687d3bef`, backend 64800/25590, frontend 66528/25591. Ativação `2026-10-13T22:10:08.4190579Z`, exibida como `13/10/2026, 19:10:08 (Brasília)`. HTTPS frontend, metadados e relay HTTP 200; data/versão iguais ao recibo. Guarda PROD 26/26. Reiniciados somente os processos PROD anteriores 57608/54916, após atestação; DEV 5904/35416 preservado. Sem DDL/migrations, testes de negócio ou fixtures PROD. Login autenticado não repetido. [Comportamento e limites](design-system/etapa-13.md), recibo `orchestracao/.runtime/footer-prod-20261013/resultado.json`. Sem pendência de publicação; manter sessão do BAT aberta.

## SKU-ZEROS01 — atualização autorizada e publicada, 13/10/2026

Lucas autorizou a breve interrupção para publicar a correspondência de SKU numérico sem zeros à esquerda, parametrizada para Bracell. Após conferir fonte/candidato, guarda atual e propriedade dos processos, encerrados somente os PIDs PROD 16776/5016. O BAT normal gerou e ativou o run `8e2ad08966344568a19be1a2158ebdd5`, backend 57608/25590 e frontend 54916/25591; este substitui a instância do registro PROD-LAUNCH03 abaixo.

Guarda 26/26, 311 fingerprints de fonte e JAR conferidos, frontend/API/relay públicos HTTP 200 e formulário de login sem erros JavaScript no Chrome. Login autenticado não repetido nesta atualização; a prova de login abaixo permanece datada. DEV atual preservado (39564/25580, 43424/25581). Sem migrations/grants, testes de negócio PROD, alteração de contas ou configuração de tunnel. Recibo: `orchestracao/.runtime/sku-zeros/publicacao.json`. Manter a sessão do BAT aberta.

## PROD-LAUNCH03 — produção ativa, 13/10/2026

Lucas autorizou conferir exclusivamente WMS_PROD, criar WMSPROD com permissões mínimas, aplicar migrations pendentes e preparar o administrador inicial. Confirmou que este é o banco desta subida. O tunnel já estava configurado pelo operador; DEV, servidor SQL, demais acessos e configuração externa foram preservados.

**Produção ativa:** run `4364ffe9d1714d65b577775dfd2d4cab`, backend PID 16776/25590 e frontend PID 5016/25591. Frontend e API públicos HTTP 200; login HTTPS no Chrome HTTP 200, identidade Gestor/administrador principal confirmada. Cookies Secure/HttpOnly/SameSite Strict. Primeiro acesso exige que o titular escolha nova senha. Logout da sessão de conferência HTTP 204, sem trocar senha nem executar operações de negócio.

- Frontend: https://wms.rodogarcia.com.br → 127.0.0.1:25591.
- API: https://wms-api.rodogarcia.com.br → 127.0.0.1:25590.
- Banco/aplicação: WMS_PROD/WMSPROD, TLS Mandatory, TrustServerCertificate=false.
- Provisionamento: `orchestracao/.runtime/prod-provisionamento-65d47e09dd944328a1fb7e8231d1dfec.json`.
- Execução: `orchestracao/.runtime/launcher-producao/runs/4364ffe9d1714d65b577775dfd2d4cab/launcher.json`.
- Conferência HTTPS posterior: `orchestracao/.runtime/prod-launch03-20261013/https-login.json`.

O launcher registra readiness local e mantém seu estado datado de login pendente; o recibo HTTPS comprova a etapa posterior. Recibos históricos não foram reescritos.

### Banco e histórico

Inspeção administrativa protegida confirmou V1–V9 e ausência de WMSPROD. Aplicadas somente V10 e V11 via Flyway, com PreValidate/Migrate/PostValidate/Info aprovados. As 64 tabelas preexistentes estavam sem registros e permaneceram assim durante o provisionamento; V11 acrescentou quatro tabelas de autenticação. A aplicação criou depois o administrador pelo serviço de login.

A V10 original aceita somente WMS_DEV e permanece imutável. PROD usa `database/migrations-prod/V10__permitir_ajuste_estoque_producao.sql`, com nome/checksum próprios e corpo equivalente, alterando somente a guarda de alvo. V1–V9 e V11 são compartilhadas sem mudanças. `database/scripts/prod-fontes.ps1` monta fontes PROD conferidas por hashes para Flyway; DEV conserva suas fontes. Não usar repair, checksum artificial ou V10 DEV em PROD.

WMSPROD recebeu 333 direitos explícitos por objeto/coluna, sem roles administrativas, DDL ou DELETE. Credencial aleatória exclusiva sob DPAPI/ACL em `%LOCALAPPDATA%/Rodogarcia/WMS/api-prod/wmsprod-app.clixml`. Guarda real aprovada em 26/26 verificações: identidade, TLS atual, permissões, 68 tabelas/716 colunas, integridade e histórico. Administração restrita ao provisionamento autorizado.

### Login e BAT

Chave RSA 3072 exclusiva PROD sob DPAPI/ACL em `auth-prod`, fora do Git. Configuração parcial é recusada; material existente é preservado. Material inicial separado `bootstrap.clixml`, preparado por `infra/prod/configurar-administrador.ps1`, fornece o hash apenas na memória do processo filho. O serviço cria o principal somente se ausente, nunca redefine conta existente. Sem esse material, a subida normal exige principal já provisionado.

`iniciar-prod.bat --preparar` compila a fonte atual sem banco/testes. `iniciar-prod.bat` exige portas livres, canais próprios e guarda atual. Uma segunda execução com portas ocupadas é recusada sem encerrar a instância. A sessão do BAT precisa permanecer aberta; Ctrl+C encerra somente seus processos. Não há serviço/autostart instalado. O BAT não aplica migrations nem grants automaticamente; o provisionamento autorizado foi separado.

**Validação:** 54 testes locais aprovados (24 launcher, 14 canais fictícios/DPAPI, 16 recibos sintéticos), build backend/frontend atual, guarda SQL 26/26 e login HTTPS real. Primeiro acesso em 390 px sem overflow ou erros JavaScript. DEV manteve PIDs 47028/6332. Não foram executados testes de negócio ou fixtures em PROD.

### Histórico PROD-LAUNCH02

Antes da autorização, 49 testes locais e preparo `3b85e81c0ac644a88b949b6ec87bed12` aprovados. Run `9a24fc3f80be4b8ebf420e1c4c15c296` parou por ausência do canal WMSPROD, sem SQL/processos; HTTPS respondia 502. Na ativação, run `64fadc541f8d4249b1540d256dd720d1` parou antes de SQL porque o launcher retirava COMPUTERNAME do processo filho. Correção coberta por teste antes da nova subida.

## Histórico anterior — não representa o estado atual

## Bloqueio confirmado em 13/10/2026

A execução do operador no run `b2b8f99e74034b1aaf6d228addda6436` encerrou com `PROD_AUTH_PUBLIC_METADATA_MISSING`, antes de build, guarda SQL ou processos. A inspeção local confirmou ausência do diretório `auth-prod`; nenhum conteúdo protegido foi lido. O launcher agora explica essa condição no console e no recibo.

A ativação operacional continua pendente: material AUTH PROD próprio, canal SQL WMSPROD restrito, implementação da guarda e passagem da credencial protegida ao helper backend, além da entrada HTTPS para login. A guarda atual sempre recusa; adicionar `identity.json` ou declarar um booleano não completa essa implementação. Não há evidência atual de provisionamento do login SQL: ausência do canal local não prova ausência do login no servidor.

O fluxo Excel/XML/bobinas foi concluído e testado em **DEV**, conforme a etapa 11 do design system; esse aceite não representa ativação de produção. Esta correção melhora o diagnóstico e não configura credenciais, banco, HTTPS ou servidores. A preparação isolada abaixo não remove esses pré-requisitos.

PROD-LAUNCH01 atende ao pedido de Lucas com o nome convencional **iniciar-prod.bat**. A grafia `.bar` recebida permanece no [pedido original](../orchestracao/.runtime/launcher-producao-demanda-lucas.txt); não é outro comando.

Execute na raiz WMS:

```bat
iniciar-prod.bat --preparar
iniciar-prod.bat
```

`--preparar` compila a versão atual de backend e frontend REAL em um candidato isolado. Não abre banco, inicia backend nem usa Vite dev. Fontes capturadas, JAR, configuração, assets e respectivos hashes ficam no recibo da versão. Falha de build não substitui artefato anterior. Não há instalação automática de dependências.

O comando sem argumentos confere portas e configuração protegida própria PROD antes do build e da guarda. Só inicia processos próprios com pré-requisitos completos. Banco permitido: **WMS_PROD**, identidade esperada **WMSPROD**, jamais WMSDEV, sa ou fallback DEV. Estes nomes são contrato de produção; não comprovam que identidade/canal já estejam provisionados.

Não há migration, init, bootstrap de usuário ou alteração automática do schema: perfil único sqlserver-prod, Hibernate validate, init never e Flyway/Liquibase desativados. A chave/metadata de login e credencial SQL precisam pertencer ao canal protegido PROD; material auth-dev/api-dev não é reaproveitado. Segredos não são parâmetros, frontend, relatório ou logs. Guarda operacional e readiness público não são teste/homologação de negócio em produção.

Cada execução prepara uma versão nova sob `orchestracao/.runtime/launcher-producao/runs/`. O manifesto fixa os bytes usados; não aponta para dist mutável de desenvolvimento. Se houver produção ou outro processo ocupando a porta, o launcher recusa a subida, mesmo que o processo pareça WMS. Pode-se preparar o próximo candidato sem interromper a versão ativa. Parada/atualização de uma execução existente depende de decisão específica do responsável; não há kill por porta, PM2 compartilhado, tarefa, serviço ou autostart. A janela operacional acompanha apenas os processos que criou.

## Portas e domínios para suporte

| Serviço | Origem local planejada | Domínio futuro exato |
|---|---|---|
| Frontend estático REAL e relay /api | http://127.0.0.1:25591 | https://wms.rodogarcia.com.br |
| Backend Java/Spring | http://127.0.0.1:25590 | https://wms-api.rodogarcia.com.br |

Portas inventariadas livres nesta entrega e revalidadas antes de iniciar; isso não é reserva nem prova de produção ativa. Os servidores DEV/visual existentes permanecem intactos. Bind apenas loopback. O [recibo](../orchestracao/.runtime/launcher-producao-resultado.md) distingue alvo preparado, execução local isolada e eventual subida operacional real.

O navegador do frontend usa **/api na mesma origem**; o servidor estático retransmite ao backend loopback sem rewrite/retry, preservando contrato/correlação e Origin. Não transforma cabeçalhos recebidos em prova de HTTPS: Forwarded, X-Forwarded-* e CF-* enviados pelo cliente são removidos. Origin de login produção é fixa https://wms.rodogarcia.com.br, proxy-origin vazio, Secure/HttpOnly/SameSiteStrict e CSRF preservados; API operacional continua Bearer/escopos verificados no backend. Não transportar a regra dinâmica DEV de devtunnels nem liberar CORS wildcard. Expor o domínio backend posteriormente não exige alterar o browser para acesso entre origens.

HTTP loopback permite conferir assets e readiness técnico, **não comprova login HTTPS operacional** com cookie Secure. O domínio frontend precisará de entrada HTTPS válida na etapa futura. A regra TLS SQL permanece independente: confiança privada corresponde ao processo atual, Encrypt Mandatory e TrustServerCertificate=false, sem bypass/trust global/recaptura automática.

## Próxima etapa de suporte, ainda não executada

O suporte receberá os dois domínios exatos e as origens loopback acima, além do manifesto/estado operacional e configuração pública aprovada. Caberá à etapa autorizada futura definir a entrada HTTPS e encaminhamento restrito para cada serviço, validar Host/Origin, autenticação, CSRF, cookies, CSP e status antes de uso. DNS e túnel são responsabilidades distintas. Nenhum tunnelID, CNAME, credencial, certificado público ou regra Cloudflare foi inventado/configurado neste pedido.

Pré-requisitos ausentes são bloqueios concretos: canal SQL PROD restrito/atestado de permissões e catálogo/histórico atuais, material AUTH PROD próprio e usuário já legitimamente provisionado, além da entrada HTTPS operacional. Não criar estes recursos por fallback nem executar testes/fixtures/escritas de negócio no banco PROD. A correção e validação local permanecem separadas das jornadas DEV anteriores já encerradas.

**Estado desta entrega:** `iniciar-prod.bat --preparar` foi executado de fato e passou, com candidato datado em `runs/5bcb34fc4fc24f2ba967bcdca014ac45`. O BAT normal foi executado e retornou exit40 `PROD_AUTH_PUBLIC_METADATA_MISSING`, antes de guarda SQL ou servidores. O guard PROD atual é somente um gate de recusa: não implementa Open nem ramo PASS. O binding entre credencial SQL protegida PROD, guarda e helper backend ainda precisa ser concluído/revisado quando existir uma fonte própria autorizada; criar metadata ou alterar booleano não libera a produção. Não há URL PROD ativa nesta entrega.

O candidato preparado usa a fonte visual capturada naquele instante. Alterações visuais posteriores foram preservadas; novas capturas recusadas por drift não substituem os assets anteriores. O complemento backend inicial também teve uma incompatibilidade encontrada na revisão PROD-VIG-003: a chave de schema generation, mesmo com valor none, era recusada pela guarda conservadora. Os builds iniciais e seus hashes permanecem históricos; o complemento corrigido DFB55 é identificado separadamente no recibo. Os três achados locais foram corrigidos e reconfirmados na [revisão final](../orchestracao/.runtime/launcher-producao/vigia/parecer-final02.json), sem material local aberto. Build PASS não comprova startup. A prova HTTP dos assets usa upstream sintético, sem backend/banco PROD. Builds e harnesses locais não comprovam subida conjunta operacional ou login HTTPS. Entrega parcial encerrada; aguardar Lucas para os pré-requisitos operacionais, sem tentativa automática.
