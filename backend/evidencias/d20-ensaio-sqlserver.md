# D20 — ensaio SQL Server preparado, ainda não executado

O código de `SqlServerLocalIT` é compilado pela verificação comum, mas executado somente com `-Psqlserver-it` (Failsafe). H2 não substitui nenhum resultado deste ensaio. Não há Testcontainers/Docker automático, criação de banco, migration, grant, limpeza, restauração ou fallback de TLS.

Complemento recebido: endereço confirmado **TCP 127.0.0.1:1433**. `sa/master` é exclusivo da criação administrativa WMS_DEV/WMS_PROD pelo procedimento Prumo/Farol; não usar no runtime, migrations ou neste ensaio. Identidades limitadas próprias, identidade real/TLS e estado do WMS_DEV ainda precisam ser comprovados. Opt-in permanece desligado, sem conexão SQL. Não executar com credencial ausente ou de terceiros. Não migrar/carregar PROD nem alterar configuração global SQL.

## Pré-condições

- Alvo e acesso inequivocamente confirmados para este ensaio fictício local. Host **127.0.0.1**, porta explícita e banco exatamente **WMS_DEV**; nenhuma conexão de empresa/servidor remoto/PROD. Não inferir autorização do serviço instalado ou de SQLCMD disponível.
- WMS_DEV exclusivo, vazio, previamente migrado V1–V9 pelo procedimento guardado de Prumo e DBA autorizado. Os 64 objetos operacionais precisam estar vazios; histórico Flyway tem as nove versões. Nenhum dado real ou objeto compartilhado.
- Certificado com cadeia confiável e identidade válida para a conexão loopback. Não usar trustServerCertificate=true nem desligar encrypt. Truststore/segredos ficam no mecanismo externo do ambiente; não entram em arquivos/CLI/logs.
- Conta de aplicação distinta da conta de migration, com SELECT/INSERT e UPDATE somente nas colunas da matriz final. Sem sysadmin, db_owner, db_ddladmin, CONTROL SERVER/CONTROL DATABASE/CREATE TABLE/ALTER SCHEMA. Sem UPDATE/DELETE de auditoria/movimentos/snapshots. Eventual ausência de metadados/permissões recusa o ensaio, sem grant automático.
- Variáveis externas `WMS_DB_HOST`, `WMS_DB_PORT`, `WMS_DB_NAME`, `WMS_DB_USER`, `WMS_DB_PASSWORD`, `WMS_DB_CONFIRMED_TARGET`, `WMS_DB_CONFIRMED_SERVER`; somente nomes, sem valores nesta evidência. Opt-in `WMS_SQLSERVER_IT=D20_LOCAL_ISOLADO` e `WMS_SQLSERVER_IT_CONFIRMED_DATABASE=WMS_DEV`.

## Execução futura no backend

Com JDK21 e configuração externa já confirmada, executar e guardar saída sem debug/trace:

```powershell
./mvnw.cmd -B -ntp '-Dwms.build.directory=target-d20-sqlserver' '-DargLine=-Xmx768m' -Psqlserver-it verify
```

Não ativar o perfil Maven migrations junto desse comando. A inicialização Spring recusa overrides de DDL/conexão antes do pool e aplica a identidade/permissão real em cada conexão. O inicializador do teste recusa host remoto, DNS, PROD, opt-in ausente e confirmação divergente antes do DataSource. Nenhuma propriedade H2 é carregada no contexto deste IT; JWT usa chave efêmera somente em testes, sem buscar provedor externo.

## O que os sete testes preparados verificam

1. SETs reais via SESSIONPROPERTY; contexto inicial confirma SQLServerDialect/validate, identidade restrita, V1–V9 e banco vazio.
2. CHECKs e FKs ativos/confiáveis no catálogo.
3. Índice filtrado real da chave de nota: duas chaves NULL aceitas; mesma chave preenchida recusada. Transação revertida, XML fictício sem homologação fiscal.
4. INSERT/UPDATE JPA cadastral com Unicode, precisão temporal em microssegundos lida do SQL Server e lock do repository. Transação revertida.
5. CHECK de auditoria inválida reverte o cadastro na mesma transação.
6. Chave idempotente duplicada recusada pela unicidade real; transação revertida.
7. Duas transações/conexões, lock PESSIMISTIC_WRITE do repository e LOCK_TIMEOUT na segunda; reset do timeout em finally. Uma fixture de cliente precisa ser confirmada antes do confronto e **permanece** no alvo isolado. Não apagar histórico. Para repetir a bateria inteira, preparar outro WMS_DEV vazio em instância isolada autorizada; o ensaio não provisiona nem remove nada.

O confronto de duas transações usa latch/pré-lock e timeout do SQL Server, não espera cega do teste. Falha de qualquer guarda/DDL/schema/permissão/constraint não autoriza alterar instância, compatibilidade/collation, migrations ou grants. Registrar logs e XMLs Failsafe reais, versões/identidade lógica e falhas sem segredos. O [clean verify comum D20](d20-resumo.json) terminou com 396/0/0/0 em 06/10/2026 às 12:32:28 -03:00; esses testes e o SQL preparado sem JDBC não comprovam os sete ITs. Nenhuma execução SQL Server ocorreu nesta entrega.
