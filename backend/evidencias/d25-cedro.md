# D25 — entrega Cedro para check

06/10/2026. **Validação local concluída:** clean verify atual JDK21 com408 testes aprovados,20 XMLs novos; JAR final executado e seis checks HTTP aprovados. **Runtime/JPA/IT SQL Server não executados:** identidade própria restrita WMS_DEV indisponível. Nenhuma conexão SQL por Cedro, migration, DML, grant ou fixture SQL foi feita nesta rodada.

## Alterações proporcionais

- D25-VIG01: o IT de lock exige SQLException nativa1222 da busca bloqueada e a mesma falha na causa da tarefa. Permissão/conexão/deadlock/reset não aprovam o confronto. Sete casos offline do oráculo/preparação passaram; os sete ITs SQL continuam sem execução.
- D25-VIG02: canal `WMS_DB_CERTIFICATE_HOST`/`WMS_DB_TRUST_STORE`/`WMS_DB_TRUST_STORE_PASSWORD` entrega nome e confiança PKCS12 ao driver. URL permanece fixa, encrypt=true/trustServerCertificate=false, WMS_DEV e guardas de identidade/SETs/DDL preservados. Sem nome real hardcoded, URL livre ou confiança global. Teste getPropertyInfo do JDBC e negativas antes do pool passaram, com valores fictícios. Prova real da lacuna TLS veio de Prumo, não da aplicação.
- O IT preparado agora ativa um único perfil Spring sqlserver-dev; suas properties isoladas entram por TestPropertySource. Opt-in Maven/Failsafe permanece exigido.
- D25-VIG03: o JAR anterior criava Set-Cookie ao negar POST status. As tentativas foram preservadas; diagnóstico registrou apenas booleanos, nunca o valor do cookie. O teste focal anterior ao patch falhou1/1 nesse requisito. A cadeia local desabilita o token CSRF de sessão, preservando GET status público, denyAll demais rotas e STATELESS. A regressão HTTP POST e o novo JAR continuam exigindo403, agora sem cookie.
- README atualizado apenas em trechos atuais: D24 V1–V9 aplicadas administrativamente, identidade própria pendente, canal TLS, comportamento local e evidências D25. POM permaneceu com o mesmo hash da cópia anterior à D25. MVC, services, modelos/negócio e migrations não foram alterados.

Detalhes, fundamentos oficiais e revisão do patch: [correções](d25-correcoes.md), [diagnóstico CSRF](d25-csrf-diagnostico.md), [plano](d25-plano.md). Vigia leu os patches focais no parecer preliminar; o aceite independente final pertence ao revisor/Farol.

## Execuções novas, separadas

| Etapa | Resultado testes/failures/errors/skips | Evidência |
| --- | --- | --- |
| Inicial atual, target-d25 | 396/0/0/0,19 XMLs; clean verify04:17 | [log](d25-clean-verify.log), [resumo](d25-build-inicial-resumo.json), d25-inicial-surefire/ |
| Pós-TLS/oráculo, antes de CSRF | 408/0/0/0,20 XMLs; clean verify03:57 | [log preservado](d25-clean-verify-tls.log), [resumo](d25-build-tls-resumo.json), d25-tls-surefire/, d25-wms-backend-tls.jar |
| Regressão focal anterior ao patch local | 1/1/0/0, falha esperada somente booleano; exit1 | [log](d25-csrf-red.log), [XML](d25-csrf-red-ApiHttpIntegrationTest.xml), target-d25-csrf-red |
| Final atual pós-CSRF, target-d25-final | **408/0/0/0,20 XMLs; clean verify03:54, exit0, fim18:49:18** | [log](d25-clean-verify-final.log), [resumo](d25-build-final-resumo.json), d25-final-surefire/ |

O inicial terminou18:29:39; o pós-TLS terminou18:40:24. Os quatro testes ApiHttpIntegrationTest usam HTTP local sem persistência; arquitetura, serviços e demais regressões existentes entraram na suíte completa. Os testes de cadastros/transações/JWT com H2 permanecem evidência isolada. Nenhum resultado H2/getPropertyInfo/mock ou SQLServerDialect offline vale execução no SQL Server. Failsafe não foi selecionado e seus relatórios não existem nesta rodada.

Comando de clean verify, a partir de backend, em filho com variáveis WMS/Flyway/Spring/opções Java/Maven removidas e RC desabilitado, settings públicas vazias:

```powershell
# JAVA_HOME JDK21 e MAVEN_OPTS=-Xmx384m somente no filho.
./mvnw.cmd -B -ntp -s ../database/config/flyway-settings-vazias.xml -gs ../database/config/flyway-settings-vazias.xml -Dwms.build.directory=target-d25-final -DargLine=-Xmx768m clean verify
```

Formatação aplicada/conferida nas fases correspondentes; os logs `d25-formatacao*.log` são próprios. O build inicial permaneceu isolado em target-d25. JAR/XMLs/log/hash pós-TLS foram arquivados [antes do novo clean](d25-pre-clean-final.json). Não houve repetição integral depois do final verde: a rodada adicional foi necessária pelo achado HTTP real e alteração Java.

## Artefato realmente executado

JAR final: [cópia preservada](d25-wms-backend-final.jar),78.531.242 bytes, SHA256 `318FCBD02DF2E8DBA6C6F78EE8D87AD9913119439D38813D9029F3CE66C649B1`. O JAR de target-d25-final, a cópia e o JSON HTTP têm o mesmo hash. Não contém H2 nem classes de teste; inclui JDBC13.4.0. JDK21.0.12.1, heap512MiB, perfil local explícito, configuração somente classpath e ambiente-filho somente OS, sem credencial/configuração SQL ou OIDC externa.

[JSON HTTP final](d25-jar-local-http.json), [stdout](d25-jar-local-stdout.log), [stderr](d25-jar-local-stderr.log): execução21:50:09–21:50:20 UTC, loopback127.0.0.1:64987, PID29168 encerrado pelo helper. Seis checks: GET status200; GET clientes, POST status, GET rota fictícia, GET error e GET status com barra final403. Tipos JSON/problem, request-id próprio/novo, código/idOperacao, ausência de cookie/detalhe interno conferidos em todos. Persistência/SQL/H2 não inicializados. Isso comprova status/recusas locais; autorização JWT/alcance com SQL real continua não demonstrada.

Para reproduzir somente o ensaio do artefato atual, sem SQL, na pasta backend:

```powershell
./evidencias/d25-ensaio-jar-local.ps1 -JavaHome 'C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21' -BuildDirectory target-d25-final
```

O helper escolhe porta0, verifica listener exclusivo loopback, usa HTTP sem proxy/redirecionamento e encerra somente a JVM criada. Antes de reexecutar, preservar o freeze/log/JSON atual; uma nova rodada gera timestamps/request-ids e precisa evidência nova. As tentativas anteriores `d25-jar-local-tentativa1-*` e `d25-jar-local-diagnostico-*` continuam incompletas, com seus próprios PIDs encerrados.

## SQL real: leitura Prumo e bloqueio da aplicação

Snapshots sanitizados: [identidade](d25-prumo-identidade-snapshot.json), [TLS com/sem nome](d25-prumo-jdbc-nome-snapshot.json), [JDBC administrativo](d25-prumo-jdbc-leitura-snapshot.json), [catálogo](d25-prumo-catalogo-snapshot.json), [resumo](d25-prumo-resumo-snapshot.json). Registros originais em orchestracao/.runtime/d25-prumo-*.json. Prumo comprovou DEV ONLINE, catálogo1295/1295,64 tabelas/687 colunas, nove migrations SQL e64 tabelas vazias, preservados frente D24. Seu JDBC standalone administrativo executou somente SELECTs com TLS validado; não iniciou Spring/JPA/API/IT.

O levantamento encontrou zero usuários próprios de aplicação no DEV e nenhum canal WMS_DB_USER/password disponível nos escopos consultados; backendSqlLiberado=false. Próximo encaminhamento externo: Lucas autorizar/provisionar identidade própria com mínimo privilégio somente WMS_DEV e credencial protegida, com atestação completa de direitos antes de liberar runtime. O initSQL enumera recusas e não substitui essa atestação (VIG07). A ausência atual não foi contornada com sa, grants ou PROD.

Após essa decisão, validar JPA Hibernate validate e permissões reais do pool, HTTP/JWT/alcance SQL e os sete ITs preparados, sob opt-in/alvo/conferência de vazio. O teste de lock deixará uma fixture confirmada no DEV; ela não foi criada nesta D25. Não autoriza limpeza/recriação/reparo para repetir o ensaio.

## Preservação e encerramento

[Preservação histórica](d25-preservacao-historicos.json):113 arquivos de evidência D20 conferidos contra o freeze histórico, zero divergências. D19/JAR/XMLs dentro desse acervo preservados; os freezes históricos não foram reescritos. Escritas restritas ao backend. AGENTS (incluindo linha55), estados/centrais, database/migrations, sharedruntime, frontend, grafo e processos alheios preservados por Cedro. Sem callback Hermes/ETL, commit, push, deploy ou rotina.

`d25-freeze.sha256` cobre arquivos/evidências/artefatos D25 e fontes backend pertinentes; `d25-verificar-freeze.ps1` confere os hashes e grava resultado separado em `d25-freeze-validacao.json`. A entrega local está encerrada, disponível para check; resta somente o encaminhamento externo da identidade e a subsequente execução SQL autorizada com essa identidade.
