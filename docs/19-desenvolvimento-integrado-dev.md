# Desenvolvimento integrado WMS_DEV

## Política vigente de testes — QUAL-CONF01-SQL01

**Decisão permanente de Lucas, 10/10/2026:** testes de persistência, integração/backend com banco, performance de dados, HTTP e browser integrado usam SQL Server real somente WMS_DEV/WMSDEV. H2/mockSQL não substituem aceite; puros sem banco, TS/lint/build ficam separados. [Regra e autorização](06-decisoes-e-pendencias.md#qual-conf01-sql01--testes-reais-sql-server-10102026). Os procedimentos e resultados H2 abaixo são históricos e não autorizam nova execução, mesmo por opt-in/perfil/override.

Antes do teste: guarda atual DB_NAME/identidade/TLS/SQLprocesso/ACL/permissões/catálogo/histórico e canal protegido próprio; EncryptMandatory e TSCfalse, sem segredo em env/log/chat. STOP em falha de transporte/inconclusivo sem retry/fallback. Não executar SQL-IT antigo com exigência de banco vazio, create-drop/INIT/reset/cleanup/DELETE no DEV populado. Caminho SQL seguro exige validate/initnever/migrationsOFF e autenticação legítima; fixtures fictícias necessárias por HTTP/API identificado, idempotente e preservado.

O preparador DEV foi alterado neste incremento: a execução padrão recusa `SQL01_INTEGRACAO_OBRIGATORIA` antes de processo; `-SomenteBuild` compila/package sem testes de banco, gera somente candidatos com aceite SQL pendente e preserva os dois recibos ativos. [Revisão desse recorte](../orchestracao/.runtime/qual-conf01/sql01/vigia/preparador-build-only-review.json). O modo build usa perfil `pure-no-db` e skip explícito/tests0; a whitelist literal de seis classes é a única seleção pura permitida. Perfil H2 e SQLIT antigo são recusados em validate, inclusive combinado com modo puro. [Negativas executadas](../orchestracao/.runtime/qual-conf01/sql01/cedro/policy-1312ea14-9416-426d-a73f-4542320263ca/negative-gates-final.json). Build não comprova integração nem deixa BAT pronto para fonte nova. A primeira leitura funcional pelo candidato próprio revisado não depende de promover recibos do operador; [caminho crítico](../orchestracao/.runtime/qual-conf01/sql01/caminho-critico.md). Comandos/outputs efetivamente executados ficam no [andamento SQL01](../orchestracao/.runtime/qual-conf01/sql01/andamento.json). Revalidar origem/dono/recibo antes de qualquer BATDEV; preservar listeners existentes no teste loopback. Nenhum DDL/PROD/grant/sharedruntime ou senha/JWT forjado.


## Reinicio DEV exclusivo

**BE02-DEV-REINICIO01, 10/10/2026:** execute `iniciar-dev.bat` para iniciar ou reiniciar este WMS. Se já estiver online, o launcher substitui apenas seu backend em 25580 e frontend em 25581, depois de conferir os pré-requisitos e a guarda atual. Não é necessário fechar previamente o console antigo. Produção e outras portas ficam preservadas.

A origem é comprovada por recibos locais, projeto, PID/horário de início, executável/comando e perfil DEV. Porta ocupada por outro projeto, origem desconhecida ou processo que também escute outra porta interrompe a inicialização sem encerrá-lo. Duas inicializações simultâneas do mesmo projeto não prosseguem juntas. O recibo `launcher.json` registra em `restart.stopped` os processos anteriores encerrados. O console antigo pode mostrar que seus processos terminaram; o novo console acompanha a nova instância.

O pacote precisa estar aceito e vinculado aos recibos atuais antes do BAT. Em SQL01 o preparador padrão recusa aceite sem integração SQL, e `-SomenteBuild` não atualiza esses recibos; não usar build candidato como indicação de reiniciar para carregar código novo. A confirmação segura de pacote fica separada da primeira leitura funcional SQL. Uma falha de preparo/guarda preserva a instância anterior. Esta seção substitui as instruções históricas abaixo sobre fechar manualmente o console ou recusar toda porta ocupada. Nenhuma alteração de produção, SQL Server ou serviço compartilhado faz parte do reinício.

## D32 — login próprio

**D32-DEV07 — túnel variável:** `infra/dev/acesso-dev.json` contém somente `modo: tunnel`; não guarda URL. Use o link HTTPS atual da porta25581 no painel Portas do VS Code. Depois de carregar essa versão pelo BAT, recriar o túnel não exige editar configuração nem reiniciar o WMS. Será necessário entrar novamente no novo endereço. Para voltar ao acesso local, usar `modo: local` e reiniciar. Não refazer o configurador de senha. [Detalhes](43-login-e-administracao-de-usuarios.md#acesso-pelo-dev-tunnel--d32-dev07).

`iniciar-dev.bat` usa o login WMS, com conta principal protegida e administradores delegados. **Ativação D32-DEV04 concluída:** V11/direitos mínimos aplicados somente WMS_DEV, material protegido configurado pelo usuário e BAT validado com backend/frontend reais. Para testar, executar o BAT e abrir `http://127.0.0.1:25581`; primeiro login exige trocar a senha temporária. `infra\auth\configurar-login-dev.bat` é auxiliar de preparação única, já atendida neste computador. Consulte o [procedimento e evidências](43-login-e-administracao-de-usuarios.md). O launcher não solicita senha nem aplica migrations; a conexão inicial aguarda até 30 segundos, sem repetição automática.

O console D32-DEV05 apresenta quatro etapas com horários e cores, URL de acesso e situação de frontend/backend/banco. Falhas indicam a etapa e o registro técnico. Para carregar a correção de login D32-DEV05, encerrar o console antigo com Ctrl+C e executar o BAT novamente; a senha configurada permanece válida. O painel final só aparece após as verificações de disponibilidade existentes. [Evidência local](../orchestracao/.runtime/login-d32/dev05-resultado.json).

## Histórico anterior à autorização D32

## Resultado atual D31-DEV02-TLS01 — SQL/TLS aprovado; AUTH pendente

Handoff expresso de Lucas absorvido por Farol em 09/10/2026. Provas do apoio Codex: guarda WMS_DEV/WMSDEV aprovada em 2026-10-09T11:56:41.5843363Z, 26/26 critérios; SqlClient e JDBC PASS, TLS validado (`Encrypt=true/Mandatory`, `TrustServerCertificate=false`), zero alterações SQL. O helper atual corresponde ao SHA D2FF82F7241F448FA5FA836A732A1F9CB2ECE2137B6735DAFDBD1AD4F06B35A9. A baseline D29 de permissões/catálogo/histórico foi preservada. [Provas e proveniência](../orchestracao/.runtime/frontend-integracao-dev-tls-handoff-20261009.md).

SQL/TLS resolvido conforme estas provas datadas; pin/recibo antigos e recusas anteriores ficam históricos, sem inferir alteração servidor/sa. Regra permanente em [AGENTS.md](../AGENTS.md): confiança corresponde ao processo atual; renovação segura autorizada, sem recaptura automática. Guarda normal futura exige atualidade e pertence à execução integrada após resolução AUTH, não ao recebimento deste handoff.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, Keycloak/BFF não aprovado. Backend/API não iniciados; URL integrada=null, roundtrip=false, entrega integrada aberta. Mapa anterior preservado, atualização estrutural pendente após rejeição; nenhum mapa/SQL/guarda/JDBC/build/teste/processo executado por Farol nesta absorção. FINAL14 local aprovada; D30 geral aberta, aceiteLocalIntegral=false; C06/C07/C10 requeridos impedidos históricos preservados. [Recibo vigente](../orchestracao/.runtime/frontend-integracao-dev-resultado.json).

## Histórico preservado — conteúdo anterior à absorção Farol TLS01

## Guarda SQL atual após relato de recuperação

RelatoLucas “sql server voltou” motivou guarda própria independenteOIDC: Open2026-10-09T11:30:16.2451044Z,TLS0x800B0110 antesSELECT,UMAOpen258.9705ms/exit20capturado,semretry. WMS_DEV/WMSDEV/direitos não confirmados; nenhum backend/API iniciado. [Resultado atual](../orchestracao/.runtime/frontend-integracao-dev-retomada-sql-20261009.md). Não repetir BAT como sondaSQL: semAUTHadequada ele para antesguarda; recorteindependente já realizado e interrompido peloerroatual.

ResponsávelTLS/SQL analisa recusa do clienteWMS com segurança, sem bypass/servidor/trustglobal. AUTH é decisão separada e Keycloak/BFF não aprovado. BATreal anterior/URLnull/roundtripfalse e propostas permanecem históricos; entrega integrada aberta.

## Histórico preservado — verificações DEV02 anteriores

## Estado observado após auditoria e referência exata

Execute `iniciar-dev.bat` na raiz WMS; o console deve ficar aberto quando os pré-requisitos permitirem subida. **Último teste real:** 2026-10-09T02:22:04.2682459Z → exit40 AUTH_CONFIGURATION_MISSING, sem guardaSQL/BE/FE, URL ou roundtrip. [Run](../orchestracao/.runtime/frontend-integracao-dev-runs/e169e141b28d4537b02a2a33cd9a8463/launcher.json), [confronto da referência](../frontend/evidencias/dev02/referencia-dashboards-dev-lume.md), [adenda atual](../orchestracao/.runtime/frontend-integracao-dev-apos-auditoria-adenda.md).

dashboards-etl já possui login próprio HMAC; o launcher não fornece autenticação ausente ao WMS RS256. Sua técnica BE background→readiness→FE strictPort foi confrontada; nenhum BAT alheio executado, conexão/segredo/identidade copiada ou kill/limpeza/Securefalse portado. Não há correção local de carregamento comprovada sem fonte própria. Keycloak/BFF permanece proposta NÃO aprovada; decisão necessária antes de provider/servidor novo/arquitetura AUTH. Não substitua autenticação por token de ensaio, Securitydisable ou mock.

TLS SQL permanece STOP independente da falta AUTH. Auditoria sa não liberou reparo: processo mudou sem autoria/causa;18456 atual não acessível. Depois decisão/contrato e solução segura TLS, este mesmo launcher ainda precisa provar processos próprios/readiness e consulta autenticada browser→API→WMS_DEV/WMSDEV. Listener/status/HTML não completam o teste. Portas propostas25580/25581 não são URL ativa.

## Histórico e contrato preparado preservados

Resultado atual: **integração real pendente**, sem URL ou roundtrip. Lucas executou o BAT, run `7483449c3bcf41608496bc8eb35e3982`, e recebeu exit40 AUTH_CONFIGURATION_MISSING em 2026-10-09T01:14:37.5586920Z. A guarda SQL não foi invocada e nenhum backend/frontend novo iniciou. [Recibo real](../orchestracao/.runtime/frontend-integracao-dev-runs/7483449c3bcf41608496bc8eb35e3982/launcher.json).

Cedro/Prumo rastrearam configuração própria pública: três refs OIDC ausentes em Process/User/Machine (9/9), placeholders Spring e canais locais somente SQL. Nenhuma origem aprovada foi localizada nas fontes consultadas; ausência qualificada, não universal. Sem fonte concreta, alterar carregamento ou preencher valores supostos não resolve autenticação. A sessão operacional real ainda exige adaptação; não basta fornecer issuer/JWK/audience.

**Proposta única de Farol para decisão:** Keycloak DEV dedicado WMS gerido pelo responsável de identidade, com BFF same-origin no backend. Tokens/secret permanecem no servidor; API ResourceServer RS256/Bearer e permissões atuais são preservadas. Provider/BFF/contas não foram criados: o pedido exige decisão material antes disso. Escopo, fluxo, claims, TLS, CSRF, donos e pergunta concreta em [proposta AUTH](../orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md). O fluxo descrito abaixo é o contrato preparado do launcher, não uma sessão já conectada.

**TLS SQL permanece impedimento separado:** única Open 2026-10-09T00:51:02.8522544Z recusou TLS0x800B0110 antes SELECT; alvo/identidade/permissões não confirmados. Arquivo cliente não identifica peer/causa. Responsável TLS precisa apresentar encaminhamento seguro; sem recaptura, bypass, trust global, alterações compartilhadas ou nova sonda automática. Autorizar AUTH não libera SQL. [Guarda](../frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json). 5178/5188/5189 permanecem históricos, não substituem integração real.

Execute `iniciar-dev.bat` na raiz do WMS. O console verifica portas próprias, configuração de identidade pública e uma guarda atual WMS_DEV/WMSDEV/TLS/permissões; só depois inicia backend e frontend reais. Mantenha o console aberto. Não há encerramento ou reinício de processos preexistentes, fallback fictício, migration, bootstrap SQL ou execução PROD.

As portas propostas são backend `127.0.0.1:25580` e frontend `127.0.0.1:25581`. Uma porta ocupada interrompe a execução; o launcher não reutiliza nem encerra seu dono. Os servidores históricos 5178/5188/5189 são preservados. A URL só é anunciada após readiness dos processos novos; isso não substitui a prova de consulta autenticada no navegador/API.

O backend usa somente `sqlserver-dev` e o complemento `application-frontend-dev.properties`: banco WMS_DEV, login WMSDEV, Hibernate validate, init SQL e migrations automáticas desativados. A credencial SQL própria fica no canal WMS DPAPI/ACL existente, carregada em memória após guarda aprovada, sem senha em argumento/log/frontend/repositório. A guarda não reutiliza o G01 histórico como prova atual nem um booleano de ambiente como aprovação.

A autenticação existente é ResourceServer JWT RS256. `WMS_OIDC_ISSUER`, `WMS_OIDC_JWK_SET_URI` e `WMS_OIDC_AUDIENCE` são referências públicas de um provedor WMS DEV já configurado; não são criadas pelo launcher. O fluxo de obtenção de token/sessão e as contas precisam ser aprovados e fornecidos pelo responsável pela identidade. O launcher não cria conta, endpoint de login, chave ou token de ensaio para substituir esse provedor. A ausência da configuração pública interrompe antes de abrir SQL.

`npm run dev` é o caminho frontend integrado real, sem fallback mock. O exercício isolado permanece somente em modo explícito separado `npm run dev:ficticio`; não comprova WMS_DEV. O proxy same-origin encaminha `/api` sem rewrite para o backend próprio confirmado; não permite SQL direto no navegador nem exige liberar CORS/CSP global.

Cada execução grava evidências sanitizadas em `orchestracao/.runtime/frontend-integracao-dev-runs/<runId>/`: launcher, guarda, readiness backend e vínculo frontend. O recibo final da demanda fica em `orchestracao/.runtime/frontend-integracao-dev-resultado.json/.md`. Consulta autenticada, alvo SQL confirmado e observação do navegador são provas distintas; uma porta aberta ou HTTP200 do HTML/status não comprova roundtrip de negócio.

As referências dos launchers de outros projetos foram consultadas apenas para organização: BAT ancorado no repositório, preparação separada, propagação de exit code e readiness antes da URL. Não foram executados nem copiados env, conexões, credenciais, controles, históricos, PM2, rotinas de kill/limpeza ou comportamento PROD.

Limites: sem sa/PROD/DDL/migrations/grants/roles/alterar SQLServer/serviços/sharedruntime, sem reset/DELETE/fixtures SQL, equipamentos/fiscal reais, publicação/commit/push/ETL/rotinas. Escrita de negócio pertinente, quando autorizada e viável, somente HTTP com dados fictícios rastreáveis. Falha atual de transporte ou guarda inconclusiva interrompe sem repetição/fallback. O impedimento observado deve ser lido no recibo atual; o histórico não substitui nova verificação autorizada.
