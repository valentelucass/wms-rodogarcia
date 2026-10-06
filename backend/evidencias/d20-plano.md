# D20 — plano de integração backend/database local

Autorização recebida em 06/10/2026: BE03/BE15, escrita exclusiva `backend/**`. Prumo mantém `database/**` e `infra/**`; Vigia revisa em leitura. Entrega neste ask/arquivos, sem callback. D19/367 é baseline histórica aceita, não resultado D20.

## Plano inicial

1. Preservar JAR, XMLs, hashes D19 e inventário do backend antes de `clean`; usar `target-d20` separado.
2. Auditar models/repositories, SQL V1–V9, permissões propostas, transações/locks e idempotência sem mudar negócio nem migrations congeladas.
3. Corrigir somente integração demonstrável: opções SET de cada conexão para índices filtrados; bloqueio das alternativas JPA de criação de esquema; validação de parâmetros antes de conectar. TLS continua obrigatório com certificado validado.
4. Conferir perfil Maven `migrations`: execução externa ao ciclo de build, proteção de alvo, clean/baseline e variáveis externas. Reportar a Farol, por este arquivo, divergências que dependam de Prumo; não editar áreas alheias.
5. Preparar ensaio SQL Server reproduzível optativo com guardas de host loopback, nome de banco isolado, identidade real confirmada e esquema previamente migrado. Sem Docker presumido, sem execução automática de DDL/grants/migrations e sem H2 como prova SQL Server.
6. Executar testes das guardas, formatação e `clean verify` completo se Java/POM mudar. Guardar logs reais, XMLs, resumo e hashes `d20-*`; distinguir testes sem conexão de ensaio SQL Server.

## Achados iniciais

- `SqlServerConfig` fixa TLS, mas não inicializa os SETs requeridos pelos índices filtrados. Os SETs das migrations não alcançam as conexões da aplicação (migrations README, linhas 76/197).
- A proteção confere `ddl-auto`/generate-ddl/scripts/Flyway, mas não as propriedades `jakarta.persistence.schema-generation.*`/`hibernate.hbm2ddl.auto` que podem alterar a ação efetiva.
- `WMS_DB_CONFIRMED_SERVER` é conferido pelo wrapper de migrations; a aplicação confere somente a string host:porta/banco. Conferência da identidade efetiva será avaliada no recorte.
- Docker não foi encontrado no PATH. SQLCMD no PATH não comprova servidor local disponível. Nenhuma conexão foi feita.

## Fontes e limites

Lidos AGENTS, resumo/BE03/BE15 de states, continuidade09, preparo33, modelo35, engenharia16, README backend/database/migrations e configuração atual. Graphify e ai-memory usados somente em leitura deste WMS; estado e autorização atuais prevalecem sobre histórico. Referências oficiais Microsoft/Flyway serão registradas na entrega. Alvo SQL local/TLS/identidades ainda não fornecidos; ensaio real só ocorrerá se todas as guardas forem satisfeitas, sem usar SQL Server da empresa/rede externa.

Status: auditoria e integração em andamento. Farol pode incorporar os achados em `database/d20-plano` sem escrita deste worker fora do backend.

## Complementos recebidos e contrato para Prumo — em andamento

Lucas definiu nomes exatos WMS_DEV/WMS_PROD. Runtime `sqlserver-dev` e migrations desta rodada permitem somente WMS_DEV; criação pertence a Prumo e exige endereço/acesso inequívocos. Nenhuma conexão atual. O ensaio optativo usa WMS_DEV vazio **somente em 127.0.0.1**, nunca PROD, sem provisionar/apagar banco.

Auditoria inicial: 32 testes sem conexão, 31 aprovados e uma falha esperada da verificação de mutabilidade; log/XMLs preservados. SQL preparado pelo Hibernate 7.4.5 com SQLServerDialect, sem execução JDBC, em `d20-hibernate-update-sql-antes.json`. Constataram-se UPDATEs indevidos de ItemChegada e OperacaoAdministrativa; UPDATEs de estorno/pedido/XML incluem campos originais sem mutação contratada. Correção proporcional: updatable=false em sequencia/lote/validade/quantidades do item físico; UUID/hash/tempo/autor/observação originais da chegada; referência/criação do pedido; emitente/série/número/emissão da nota; número/quantidade prevista do item da nota; recurso_id do snapshot administrativo. Estorno, XML/chave e complemento de valor desconhecido permanecem mutáveis conforme contrato18. Não muda schema, negócio, grants ou snapshots existentes; nenhum DynamicUpdate.

Contrato proposto de execução Maven para Prumo:

- POM fixa databaseName e placeholder em WMS_DEV e mantém wms/locations/cleanDisabled/baselineOnMigrate.
- Credenciais Flyway passam a ser `WMS_DB_MIGRATION_USER`/`WMS_DB_MIGRATION_PASSWORD`, distintas de `WMS_DB_USER`/`WMS_DB_PASSWORD` da aplicação. Nenhuma credencial é fornecida/inventada.
- Wrapper deve recusar overrides FLYWAY_*, -Dflyway.*, arquivos de configuração/argumentos JVM/injetados que mudem URL/user/locations/schema/baseline/initSql/skip e env comuns Maven que introduzam essas opções; invocar `validate flyway:<ação>` com guardas antes de qualquer conector. Não ler nem expor segredos/arquivos de configuração globais.
- POM exigirá `WMS_DB_MIGRATION_GUARD=D20_WMS_DEV_VERIFICADO` no perfil migrations; wrapper fornece o marcador após suas conferências e remove/restaura ao encerrar. Não é segredo nem substitui a validação. Invocação direta do goal não executa a fase validate; procedimento permitido continua o wrapper.
- Flyway permanece `skip=true` por padrão no POM. Wrapper precisa passar `-Dwms.migrations.skip=false` somente após suas guardas, junto de `validate flyway:<ação>`. Chamada direta padrão deve ignorar o goal sem conectar; não interpretar BUILD SUCCESS/skip como migration aplicada. O wrapper deve rejeitar override desse parâmetro recebido do ambiente/usuário.
- Flyway initSql fixa os sete SETs e recusa DB_NAME diferente de WMS_DEV na própria conexão. POM não pode anular por si só a precedência nativa dos overrides Flyway: recusa do wrapper + validate são essenciais (VIG01).
- Refinamento Farol: initSql padrão **sempre THROW 51002**, inclusive se alguém habilitar skip=false no goal direto sem marcador. Perfil `migrations-wrapper-confirmado` só ativa com `WMS_DB_MIGRATION_GUARD=D20_WMS_DEV_VERIFICADO`, substituindo a fonte por `WMS_DB_MIGRATION_INIT_SQL` transitório, exigido não vazio em validate. Não há SQL com env servidor/marker interpolado cru no POM.
- **Contrato final da fonte para Prumo:** wrapper recusa variáveis de guarda preexistentes e overrides; depois da validação, constrói SQL com marcador conferido, SETs e comparação binária da identidade real WMS_DEV/ServerName em cada conexão antes do schema/histórico. Escapar cada literal (`'` → `''`), inclusive servidor; ausência/divergência de marcador no construtor devolve THROW seguro, nunca string vazia. Define temporariamente os dois WMS_DB_MIGRATION_* de guarda, chama validate + goal com skip=false, e restaura/remove no finally. Não basta conferir servidor na conexão prévia. POM não promete impedir adulteração maliciosa do próprio executor/configuração.
- Runtime inicializa SETs e confere ServerName/banco em **cada** conexão do pool, exigindo WMS_DB_CONFIRMED_SERVER. Aplicação com sysadmin/db_owner/DDL será recusada antes de expor o pool. Identidades de migração/aplicação separadas, sem GRANT.

Prumo/Farol devem reconciliar esse contrato com o wrapper e atualizar a matriz: chegada UPDATE somente três colunas de estorno; pedido entrada somente estado/versão/alteração/efetivação/motivo; nota somente chave/XML/hash; item nota somente valor_mercadoria. SQL final completo será entregue; conferir suas demais colunas mutáveis, incluindo @Version. Parecer inicial Vigia VIG01/03/04/05 lido como evidência; aceite somente após freeze/testes finais.
