# D31-DEV02 — startup backend protegido para desenvolvimento integrado

**Estado observado: integração real bloqueada.** A autorização atual permitiu a tentativa segura nova de Prumo. A única abertura, PID32144 em 09/10/2026 00:51Z, falhou em TLS antes de SELECT, código `-2146762480`, marcador de certificado. O [recibo original](../../frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json) não confirma alvo/identidade/permissões. Não repetir a sonda, iniciar backend/API ou usar fallback. O impedimento é a tentativa atual, não a ausência de prova histórica.

Separadamente, `WMS_OIDC_ISSUER`, `WMS_OIDC_JWK_SET_URI` e `WMS_OIDC_AUDIENCE` estavam ausentes no processo de Cedro; Farol confirmou a mesma ausência. A fonte própria DEV de provider/contas/fluxo aprovado de obtenção do token não foi encontrada nas fontes dirigidas. `CadastrosSegurancaConfig`, `IdentidadeProperties`, `JwtWmsValidator` e [documento14](../../docs/14-cadastros-acesso-e-persistencia.md) definem Resource Server RS256, Bearer e alcance WMS; não oferecem login/emissão/sessão local. Cookies não autenticam. A credencial SQL WMSDEV não autentica o navegador. Tokens/issuer de ensaio não substituem o provider DEV.

O launcher único `iniciar-dev.bat`/`infra/dev/iniciar-dev.ps1` e o [guia raiz](../../docs/19-desenvolvimento-integrado-dev.md) pertencem a Farol. Ele verifica AUTH antes da guarda SQL. Lume mantém `npm run dev` real por padrão e `dev:ficticio` explícito, proxy `/api` sem rewrite e `changeOrigin=false`. Portas propostas: BE25580/FE25581, sempre conferidas livres; não reutilizar processos existentes.

## Contrato do auxiliar backend

[start-backend.ps1](../scripts/dev/start-backend.ps1) é biblioteca para dot-source, sem startup, SQL, DPAPI, mudança de ambiente ou escrita ao importar. Farol chama `Start-WmsFrontendDevBackend` com:

| Parâmetro | Conteúdo |
| --- | --- |
| `Port` | Porta loopback livre, por exemplo25580 |
| `RunDirectory` | Diretório UUID novo em `orchestracao/.runtime/frontend-integracao-dev-runs/`, criado pelo launcher |
| `GuardReceipt`, `GuardSha256` | JSON **original** do filho Prumo atual no mesmo diretório e seu SHA256 |
| `GuardProcessId`, `GuardHelperSha256` | PID observado no handle do filho Prumo que terminou exit0 e SHA da fonte estável da guarda |
| `JarPath`, `JarSha256`, `JavaPath` | Artefato conferido e JDK21 existente; não executa build automaticamente |
| `Identity` | Objeto público com `issuer`, `jwkSetUri` HTTPS e `audience`; nenhum token/senha/segredo de cliente |

Os parâmetros de origem da guarda são necessários: omissão recusa a inicialização. O auxiliar confere hash original, estado aprovado, PID, fonte fixa `infra/dev02/guarda-wmsdev.ps1` na raiz WMS, horários `inicioUtc`/`observadoEm` com idade máxima de cinco minutos, identidade WMS_DEV/WMSDEV, TLS/permissões/catálogo/histórico/checks completos e perfil protegido. Não adapta booleanos antigos nem executa a guarda. O recibo atual bloqueado é recusado antes de DPAPI/Java.

Somente numa execução futura autorizada, após PASS atual e todas essas conferências, o auxiliar carrega `Get-WmsDevApplicationCredential` da biblioteca própria `database/scripts/d26-credencial-aplicacao.ps1`. A credencial DPAPI/ACL passa apenas ao ambiente efêmero do filho Java, que não herda `SPRING_*`, opções JVM ou variáveis livres do pai. Não chama `Get-ProjetosSqlProfile`/`Get-WmsDevApplicationProfile`, pois o primeiro pode atualizar/recapturar certificado do runtime compartilhado; consome somente `perfilProtegido` e hashes TLS do recibo atual. O texto de integridade `projetos-public-cert` já pertence ao contrato do truststore de certificado público; não é credencial SQL.

Usa apenas `sqlserver-dev`, o complemento externo `application-frontend-dev.properties` e configurações efetivas fixas: loopback, WMS_DEV/WMSDEV, `ddl-auto=validate`, `generate-ddl=false`, init `never`, Flyway/Liquibase desligados. As localizações base são os recursos do JAR, com complemento por `additional-location`; sem configuração livre do diretório de trabalho. JWT continua sob autoridade do backend e CORS fechado. Não há segredo em argumento, log bruto ou arquivo versionado.

O retorno é `{RunId, Process, Sanitized, ReceiptPath, ReceiptSha256}`. `Process` é handle somente em memória, nunca serializado. O arquivo próprio `backend-readiness.json` é create-only. Em `Sanitized.guard`, os campos planos são `sha256` do recibo original, `processId` igual `pidGuarda`, `helperSha256` e `finishedUtc` igual `observadoEm`; não existe `guard.proof`. Inclui `java`, `artifact` e `config`, cada um com `path`/`sha256`.

`READY` exige Java próprio vivo, listener exclusivamente127.0.0.1 pertencente ao mesmo PID e GET real `/api/v1/status` com HTTP200, `aplicacao=wms-rodogarcia`, `status=DISPONIVEL`. Não confirma consulta autenticada ou roundtrip SQL; `authenticatedBusinessRead=false`. Falha retorna `BLOCKED`/código sanitizado, sem publicar saída bruta de boot, e encerra somente o handle criado nessa chamada. `Stop-WmsFrontendDevBackend -Handle $resultado` aceita apenas handles registrados pelo auxiliar, recusando processos alheios.

## Evidência de preparo e limites

[frontend-dev02-preparo.json](../evidencias/frontend-dev02-preparo.json) identifica JDK/artefato/configuração/fontes/checks e os impedimentos atuais. O JAR FINAL14 existente foi conferido por nome/bytes/SHA contra seu recibo; não foi executado, recompilado nem recebeu novo aceite. O JDK foi localizado pelo campo público `java` do perfil existente e pelo arquivo `release`, sem executar a API do runtime ou Java.

[test-start-backend.ps1](../scripts/dev/test-start-backend.ps1) verifica recusas com o JSON atual bloqueado e fixtures de estrutura **explicitamente fictícias**. Não chama Start, DPAPI, SQL, HTTP ou Java; aceitação estrutural de fixture não é PASS real. Parser e focais são provas locais do auxiliar. Boot/readiness real, autenticação e integração permanecem não executados sob o STOP atual.

---

# Histórico preservado D31-DEV01 — preparação DEV para o frontend

**Situação atual: execução bloqueada em G01.** O [recibo Prumo](../../frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json), SHA-256 `9135BC92A2C1BF3DF012D47B8EA19E196B835D0A153F840F10D3AA34F15C9005`, informa ausência de prova atual de resolução segura de D29. `guardaRealAprovada`, `backendPodeIniciarComSQL` e `frontendPodeConectarAPIComSQL` são `false`. G03–G06 reais não foram executados. Metadados de existência/ACL do canal WMSDEV não substituem essa guarda. Nenhum backend, SQL, HTTP, build ou teste backend foi executado nesta preparação.

## Configuração existente e complemento

O padrão é `local`, sem DataSource/JPA/repositories e sem os módulos persistentes. Para API com SQL, o perfil existente é `sqlserver-dev`, que habilita cadastros/JPA e recebe configuração externa. `AmbienteConfig` aceita **um único perfil**, entre `local`, `test` e `sqlserver-dev`; `SqlServerConfig` depende de `sqlserver-dev`. Portanto, `frontend-dev` não é um perfil ativo novo.

[application-frontend-dev.properties](../src/main/resources/application-frontend-dev.properties) é um **complemento externo** a carregar com `spring.config.additional-location`, mantendo somente `sqlserver-dev` ativo. Fixa `WMS_DEV` e `WMSDEV`, endereço loopback, `ddl-auto=validate`, `generate-ddl=false`, SQL init `never` e Flyway/Liquibase desligados. Não altera fontes/migrations/históricos. Não contém senha, token, import de segredo, criação de esquema nem autorização para conectar. O arquivo, isoladamente, não executa a guarda de D29; esta precisa estar comprovada antes da inicialização.

O carregamento adicional mantém as localizações padrão de configuração, conforme a [documentação oficial Spring Boot](https://docs.spring.io/spring-boot/4.1/reference/features/external-config.html). Ainda não houve validação de carregamento na aplicação nesta entrega.

Não foi criado um script iniciador: a guarda recebida está bloqueada e não há contrato aprovado de um lançador que possa obter a credencial depois dos pré-requisitos. Não adaptar nem executar runners históricos de ensaio como atalho.

## Contrato para Farol e Lume

| Item | Contrato existente/preparado |
| --- | --- |
| Backend | `http://127.0.0.1:8080`; porta por `WMS_PORT`, somente após confirmar que está livre para processo próprio |
| API | Prefixo `/api/v1`; rotas existentes, sem criar `/login`, emissão de token ou relatório genérico |
| Perfil ativo | Somente `sqlserver-dev`; complemento externo `application-frontend-dev.properties` |
| Banco/identidade | Exclusivamente `WMS_DEV` / `WMSDEV`, próprios e restritos, comprovados na guarda atual |
| Frontend atual | `npm run dev`, `http://127.0.0.1:5178`, modo `ficticio`; preparação do modo real sob Lume, sem execução com SQL enquanto G01 bloqueado |
| Autenticação | Bearer JWT do provedor existente, sem cookie/senha/token em arquivo ou argumento; `wms_perfil`, `wms_clientes`, `wms_armazens` e sujeito/alcance conferidos pelo backend |

Entradas não sensíveis já consumidas por `application-sqlserver-dev.properties`: `WMS_DB_HOST`, `WMS_DB_PORT` (padrão 1433), `WMS_DB_CONFIRMED_TARGET` (host:porta/WMS_DEV), `WMS_DB_CONFIRMED_SERVER`, `WMS_DB_CERTIFICATE_HOST`, `WMS_DB_TRUST_STORE`, `WMS_OIDC_ISSUER`, `WMS_OIDC_JWK_SET_URI` e `WMS_OIDC_AUDIENCE`. O complemento declara nome/usuário fixos. A guarda/lançador futuro deve conferir os valores efetivos e recusar sobreposições por ambiente/argumento que mudem esses alvos; um arquivo de configuração não impede sozinho todas as sobreposições Spring. Não presumir host SQL nem valor desses parâmetros a partir da URL HTTP. TLS SQL permanece `encrypt=true;trustServerCertificate=false`.

`WMS_DB_PASSWORD` e eventual `WMS_DB_TRUST_STORE_PASSWORD` são entradas protegidas do processo, nunca exemplos literais, `.env`, logs, relatório ou linha de comando. O canal próprio WMSDEV precisa fornecer esses valores somente em memória de processo após os pré-requisitos comprovados. Nesta entrega não foi lido nem importado DPAPI. Não utilizar fonte administrativa compartilhada/`sa` na API.

JWT existente usa JWK remoto e valida issuer/audience, prazo e claims WMS; issuer/JWK exigem HTTPS. Não inventar provedor, chave, token ou liberação de rede para demonstrar integração. O frontend tem adaptador `realTransport`, ainda não instanciado na sessão fictícia; a integração e o ciclo do token pertencem a Lume.

**Fronteira CORS:** a leitura de `backend/src/main/java` e `resources` não encontrou `CorsConfigurationSource`, `cors()` ou `@CrossOrigin`. A confirmação do responsável nesta mesma DEV01 escolhe manter CORS fechado e usar proxy Vite de mesma origem para **`/api`**, alvo backend loopback configurado, sem reescrever os caminhos `/api/v1`. Lume cuida do proxy/mode real condicionado à guarda. CSP permanece `self`, sem wildcard ou novo provedor; JWT e alcance continuam sob autoridade do backend. Nenhuma configuração de CORS/Security compartilhada foi alterada por Cedro. Proxy, CSP e integração não foram exercitados por esta preparação.

## Forma futura de iniciar — não executar no estado atual

Primeiro, o responsável deve fornecer prova atual de resolução de D29. Prumo confere e emite nova guarda mínima própria de WMS_DEV/WMSDEV/TLS/permissões/schema/histórico, mantendo G01–G06 e referências verificáveis. Guarda ausente, antiga, inconclusiva ou qualquer requisito recusado mantém a inicialização fechada, sem DPAPI, sonda/retry/fallback ou alteração de servidor/acessos. Farol liga a guarda e o escopo atual à execução; configuração estática não aceita os critérios reais.

Somente depois: confirmar JDK21, artefato backend existente e seu SHA correspondente à fonte que será usada; confirmar porta livre e todas as entradas externas/identidade sem imprimir segredos; disponibilizar credencial WMSDEV no canal protegido em memória. Não gerar outro build, iniciar Maven, migrar ou reaproveitar fixtures antigas por causa deste guia.

Forma do comando, **a partir de `backend/`**, para o lançador protegido futuro depois de sua guarda aprovada:

```powershell
# $wmsDevJar deve ser o caminho do JAR existente conferido, fornecido pelo lancador.
# As entradas protegidas devem estar apenas na memoria do processo autorizado.
& "$env:JAVA_HOME/bin/java.exe" -jar $wmsDevJar `
    '--spring.profiles.active=sqlserver-dev' `
    '--spring.config.additional-location=file:./src/main/resources/application-frontend-dev.properties'
```

Não usar `--spring.profiles.active=sqlserver-dev,frontend-dev` nem ativar `frontend-dev` sozinho. O comando acima descreve carregamento de configuração; não é lançador com guarda implementada e não está liberado agora. HTTP de readiness, frontend com API, escritas fictícias de integração e encerramento de processo próprio precisam seguir a guarda/rodada autorizada, sem tocar processos existentes. Configuração/guia conferidos por leitura e comparação estática, sem execução ou aceite de integração.
