# D20 — auditoria da integração, sem conexão SQL Server

Recorte BE03/BE15, somente backend. Não houve alteração de services, repositories, regras de negócio, DTOs, migrations ou grants. O SQL abaixo foi preparado pelo Hibernate 7.4.5.Final com SQLServerDialect e metadados JDBC desabilitados; não foi enviado a um servidor. H2 continua sendo apenas suporte à regressão comum.

## UPDATE e permissões

A captura anterior à correção está em [d20-hibernate-update-sql-antes.json](d20-hibernate-update-sql-antes.json). Ela demonstrou dois snapshots com UPDATE preparado (`ItemChegada` e `OperacaoAdministrativa`) e colunas originais incluídas nos comandos de chegada, pedido, nota e item da nota. A ausência de setter não exclui uma coluna do UPDATE estático do Hibernate.

A correção aprovada usa `updatable=false` somente nesses campos imutáveis. A captura final está em [d20-hibernate-update-sql.json](d20-hibernate-update-sql.json):

| Entidade | UPDATE preparado após correção |
| --- | --- |
| ItemChegada / OperacaoAdministrativa | Nenhum |
| ChegadaRecebimento | estornada_em, estornada_por, motivo_estorno |
| PedidoEntrada | alterado_em, efetivado_em, motivo_conclusao, situacao, versao |
| NotaEntrada | chave_acesso, xml_hash, xml_original |
| ItemNotaEntrada | valor_mercadoria |

O [confronto](d20-confronto-update.json) compara o SQL final com o arquivo de auditoria de Prumo: 64 entidades, 34 UPDATEs, correspondência completa das colunas, inclusive embedded e `@Version`. A [matriz D20 de Prumo](../../database/permissoes-d20.md) torna explícitas as colunas antes resumidas em [permissoes-minimas](../../database/permissoes-minimas.md). Nenhum UPDATE geral ou DynamicUpdate foi introduzido. Permissão efetiva/herdada e DML no SQL Server permanecem sem ensaio.

## Queries, locks, transações e repetição

Foi conferida a integração dos repositories com os serviços atuais, preservando o modelo MVC e as dependências por construtor. `PedidoEntradaRepository`, `PedidoSaidaRepository`, cadastros e `LinhaContingenciaRepository` usam `PESSIMISTIC_WRITE` nos métodos de alteração. `PedidoEntradaService.bloquear/validarVinculos` exigem transação existente; autorização de cliente/armazém, revisão e situação são revalidadas pelos serviços. Registro/estorno/efetivação do recebimento têm transações explícitas, e a confirmação grava fatos/auditoria no mesmo fluxo.

`UnidadeLogisticaRepository` restringe candidatas por cliente/armazém/produto e elegibilidade; impedimentos de contagem/carga, avaria, reserva e ocupação integram as consultas. Os serviços continuam responsáveis pela revalidação e pelos locks, e não foi proposta alteração de índice sem plano/custo observado. UUID/hash/replay e unicidades dos fatos permanecem nos serviços/models existentes. A regressão comum verifica seus cenários; não comprova hints, isolamento, contenção ou deadlocks do SQL Server. O ensaio optativo preparado confronta duas transações reais pelo lock de um repository e testa unicidade e rollback, sem substituir uma homologação operacional de todos os fluxos.

## Guardas e procedimento

`SqlServerConfig/Properties` recusam alvo diverso de WMS_DEV, parâmetros de conexão malformados e alternativas JPA de DDL/alvo antes de criar o pool. Cada conexão física recebe os sete SETs, a comparação exata de ServerName/banco (incluindo recusa de NULL) e a recusa de privilégios administrativos/DDL. TLS permanece com encrypt=true e trustServerCertificate=false. A função da aplicação não ganha grants.

Flyway continua externo ao startup e ao build comum. O initSql padrão do POM é THROW 51002; somente o marcador exato ativa a fonte transitória escapada de Prumo. Portanto, goal direto sem marcador, mesmo habilitado, conserva a recusa dentro de initSql. Enforcer exige WMS_DEV, marcador e fonte não vazia na fase validate. O wrapper deve continuar recusando overrides, isolando settings e confirmando servidor/banco/ORIGINAL_LOGIN em cada conexão Flyway antes de schema/histórico. Marcador é controle de procedimento, não segredo nem defesa contra alteração maliciosa do executor.

Os testes negativos usam parâmetros fictícios e conexões Mockito; as fixtures Maven só executam validate/help:evaluate. Nenhum goal Flyway ou conector SQL real foi invocado. O [ensaio local](d20-ensaio-sqlserver.md) fica optativo e exige alvo/acesso confirmados, WMS_DEV vazio em 127.0.0.1, V1–V9 pré-aplicadas, TLS e conta restrita. PROD é recusado nesta rodada. Não há V10 ou falha de schema nova demonstrada que justifique DDL.

## Fontes oficiais consultadas

- [Microsoft CREATE INDEX — SETs para índices filtrados](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver17#required-set-options-for-filtered-indexes): os SETs são necessários também às sessões de DML; SET na migration não configura o pool.
- [Microsoft JDBC e TLS](https://learn.microsoft.com/en-us/sql/connect/jdbc/connecting-with-ssl-encryption): criptografia e validação do certificado da conexão.
- [Hibernate — schema generation](https://docs.hibernate.org/orm/7.2/userguide/html_single/): a ação Jakarta pode prevalecer sobre hbm2ddl.auto; database.action=none pode suprimir validate. A versão efetiva do build é 7.4.5.Final; as alternativas foram também verificadas no código/dependências locais.
- [Flyway Maven](https://documentation.red-gate.com/flyway/reference/usage/maven-goal): precedência de propriedades, ambiente e arquivos exige as recusas do wrapper além do POM.
- [Flyway initSql](https://documentation.red-gate.com/fd/environment-init-sql-setting-277578927.html): inicializador por conexão; permanece suportado na versão utilizada, embora marcado deprecated. Não é prova de execução atual.
- [Flyway SQL Server](https://documentation.red-gate.com/fd/sql-server-database-277579330.html): GO é suportado; V1–V9 foram preservadas.
- [Microsoft HAS_PERMS_BY_NAME](https://learn.microsoft.com/en-us/sql/t-sql/functions/has-perms-by-name-transact-sql?view=sql-server-ver17) e [IS_SRVROLEMEMBER](https://learn.microsoft.com/en-us/sql/t-sql/functions/is-srvrolemember-transact-sql?view=sql-server-ver17): associação de role não substitui a conferência de permissões; resposta desconhecida é recusada.
