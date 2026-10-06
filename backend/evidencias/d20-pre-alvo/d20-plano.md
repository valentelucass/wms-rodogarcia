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

Status inicial (histórico): auditoria e integração em andamento. Farol pode incorporar os achados em `database/d20-plano` sem escrita deste worker fora do backend. O resultado final consta ao fim deste arquivo.

## Complementos recebidos e contrato para Prumo

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
- **Contrato final da fonte para Prumo:** wrapper recusa variáveis de guarda preexistentes e overrides; depois da validação, constrói SQL com SETs e comparação binária da identidade real WMS_DEV/ServerName/ORIGINAL_LOGIN em cada conexão antes do schema/histórico. Escapar cada literal (`'` → `''`), inclusive servidor/usuário. A seleção da fonte ocorre pela ativação exata do marcador no POM: ausente/divergente conserva THROW seguro, mesmo com fonte externa; fonte vazia é recusada em validate. Define temporariamente os dois WMS_DB_MIGRATION_* de guarda, chama validate + goal com skip=false, e remove no finally. Não basta conferir servidor na conexão prévia. POM não promete impedir adulteração maliciosa do próprio executor/configuração.
- Runtime inicializa SETs e confere ServerName/banco em **cada** conexão do pool, exigindo WMS_DB_CONFIRMED_SERVER. Aplicação com sysadmin/db_owner/DDL será recusada antes de expor o pool. Identidades de migração/aplicação separadas, sem GRANT.

Prumo/Farol devem reconciliar esse contrato com o wrapper e atualizar a matriz: chegada UPDATE somente três colunas de estorno; pedido entrada somente estado/versão/alteração/efetivação/motivo; nota somente chave/XML/hash; item nota somente valor_mercadoria. SQL final completo será entregue; conferir suas demais colunas mutáveis, incluindo @Version. Parecer inicial Vigia VIG01/03/04/05 lido como evidência; aceite somente após freeze/testes finais.

Leitura do wrapper Prumo durante o build: credenciais separadas, marcador/fonte transitórios, skip=false + validate, settings vazias e initSql com identidade real/ORIGINAL_LOGIN escapados já presentes. A recusa no initSql do POM é definida pela ativação exata do marcador: ausente/divergente conserva THROW 51002, mesmo havendo fonte externa; somente marcador exato seleciona a fonte construída pelo wrapper. Isso evita concatenar env cru em SQL. Fixtures Maven ampliadas usam o construtor puro real de Prumo com servidor/usuário fictícios contendo aspas; sem invocar o wrapper/conector nem goal Flyway. A própria fonte fornecida deve continuar não vazia e guardada, e preexistência/overrides recusados pelo wrapper.

Revisão final acrescentou recusa explícita de ServerName/DB_NAME NULL no pool (comparação SQL de NULL não resulta TRUE). O primeiro clean verify já em execução será preservado como pré-NULL; depois de formatar esta guarda haverá clean verify final completo no mesmo diretório isolado, com XMLs/logs anteriores copiados. Nenhuma execução SQL inferida desse ajuste.

## Fecho local — 06/10/2026, 12:32:28 -03:00

Integração backend necessária concluída e entregue para check. Escrita somente backend; sem callback, conexão SQL real, migrations/grants, alteração de negócio ou V10. Revisão/aceite BE03/BE15 e ensaio real continuam decisões distintas.

- `spotless:apply`: BUILD SUCCESS, 280 fontes Java conferidos; [log final](d20-spotless-final-null.log).
- `clean verify` real com JDK21/Maven Wrapper3.9.16, `target-d20`, Maven384MiB/testes768MiB: **BUILD SUCCESS, 396/0/0/0**, 19 XMLs, 4:09min; [log](d20-clean-verify.log), [resumo](d20-resumo.json), [XMLs](d20-xml-clean-verify/). D19/367 permanece histórico e não foi reapresentado como D20. Os 35 testes focais estão incluídos nos 396, não somados a eles.
- Oito fixtures Maven offline aprovadas; [script reproduzível](d20-testar-maven-guardas.ps1) e [resultados](d20-maven-guardas.json). Usam validate/help:evaluate e o construtor puro de Prumo com aspas fictícias; nenhum goal Flyway/conector.
- [Confronto SQL/matriz](d20-confronto-update.json): 64/64 entidades coincidem, 34 UPDATEs preparados pelo SQLServerDialect, zero divergências de colunas com Prumo. SQL anterior/final e falha inicial preservados. Isso não comprova permissões SQL reais.
- [Preservação](d20-preservacao.json): V1–V9, JAR/XMLs e evidências/hashes D19 inalterados. O único arquivo de evidência preexistente alterado em paralelo foi `manutencao-java-2026-10-06.md` (complemento BE16/editor); foi preservado, sem restauração/escrita D20. Os 12 arquivos de POM/fontes anteriores alterados pela integração estão enumerados no resumo de preservação.
- Primeira tentativa pré-NULL teve log interrompido pela captura PowerShell/stderr; JAR, 19 XMLs e log foram copiados em [d20-pre-final](d20-pre-final/). Seu código de saída não foi comprovado e ela não é o verify aceito. Recusa inicial do wrapper por JAVA_HOME inexistente também ficou em log separado; formatação/build finais usaram a instalação JDK21 correta. Nenhum processo alheio foi encerrado.
- JAR final 78.529.706 bytes, SHA-256 `03BFE49B70E7F119324ED21AAA065F590473BF7275232C83EB1B57D26B1AEBB4`. [Inspeção](d20-jar-inspecao.json) confirma driver SQL Server e ausência de H2/fixtures/dependências de teste no JAR. [Freeze](d20-freeze.sha256) delimita o conteúdo entregue.

Fontes oficiais, limites de queries/locks/transações e decisões VIG01/03/04/05 estão na [auditoria](d20-auditoria-integracao.md). Os sete ITs SQL Server estão compilados e preparados, **não executados**; falta alvo/acesso local inequívoco, TLS/conta restrita e WMS_DEV vazio previamente migrado. Nenhuma conexão deve ocorrer enquanto isso não for confirmado. Próximo encaminhamento para Farol/Prumo/Vigia: conferir este freeze, contrato wrapper/POM e matriz; só depois, sob autorização/alvo próprios, executar o ensaio local. Trabalho deste recorte encerrado.
