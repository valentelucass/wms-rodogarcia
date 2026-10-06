# D21 — contrato público Prumo/Cedro

Lucas supersede D20: launcher manual administrativo `sa` em TCP127.0.0.1:1433 cria/confere e migra **WMS_DEV, depois WMS_PROD**. Backend continua com identidade distinta/restrita. Nenhum SQL pelo agente; senha oculta local somente memória. D20 copiado/hash conferido antes de alterar fontes.

Cedro: perfil Maven **`bootstrap-local`**, isolado de `migrations`/runtime. Wrapper invoca `-Pbootstrap-local -Dwms.bootstrap.skip=false validate flyway:acao` por etapa em filho separado (validate pré usa `ignoreMigrationPatterns=*:pending`; migrate/pós/info usam valor vazio, sem ignorar future/pending). Alvos exatos apenas WMS_DEV/WMS_PROD, endpoint fixo. Não executar build/test/runtime. Perfil deve conferir marcador também no `initSql`, pois goal direto não roda enforcer.

Variáveis públicas, exclusivamente no ambiente do **processo-filho efêmero**, não no pai/User/Machine:

- `WMS_DB_BOOTSTRAP_GUARD=D21_LOCAL_MANUAL_CONFIRMADO`
- `WMS_DB_BOOTSTRAP_HOST=127.0.0.1`, `WMS_DB_BOOTSTRAP_PORT=1433`, `WMS_DB_BOOTSTRAP_NAME=WMS_DEV|WMS_PROD`, `WMS_DB_BOOTSTRAP_USER=sa`
- `WMS_DB_BOOTSTRAP_PASSWORD`: senha oculta em memória; não imprimir, escrever, passar em argumento ou incluir em relatório.
- `WMS_DB_BOOTSTRAP_SERVER`: ServerName observado pelo launcher em leitura TLS de master e confirmado pelo operador.
- `WMS_DB_BOOTSTRAP_INIT_SQL`: wrapper fornece 7 SETs D20 + guarda BIN2 de servidor/banco/ORIGINAL_LOGIN=sa em cada conexão. Não executar se marcador ausente/errado: default initSql `THROW`, não substituição vazia.

Flyway: filesystem `${project.basedir}/../database/migrations`, schema/defaultSchema `wms`, tabela `flyway_schema_history`; versões descobertas pela ferramenta, sem `target` nem enum V1–V9. `cleanDisabled=true`, `baselineOnMigrate=false`, `validateOnMigrate=true`, `outOfOrder=false`, `validateMigrationNaming=true`; placeholders `wmsDatabase` = banco exato. Sem clean/repair/baseline automático. JDBC `encrypt=true;trustServerCertificate=false`, host/porta fixos no URL. Prumo recusa overrides/config implícita, usa settings vazios e captura saída sem divulgar texto bruto do Maven.

Prumo executará cada goal em filho separado e interromperá no primeiro exit não zero; pré-validate permite somente pending (normal upgrade), pós-validate não permite pending/missing/future/failed. Qualquer erro de DEV impede qualquer ação PROD. Pós-schema lê catálogo real e confronta expectativa derivada das mesmas fontes SQL: tabelas, colunas/tipos/nulos/identity/seed/incremento, PK/UNIQUE/FK e ações, CHECK/default (AST normaliza IN/NOT IN/BETWEEN/AND/OR), índices/chaves/ordem/include/filtro; exige restrições trusted/enabled. Não lê dados de negócio. Sintaxe não suportada no replay estrutural falha antes de conectar, sem hardcode latest9. Não é detector universal de drift: permissões, triggers, propriedades físicas/collation e outros objetos não modelados continuam fora desta comparação. Flyway seleciona/executa pendentes e controla histórico/checksum; parser não calcula checksum Flyway nem seleciona pendentes.

Referências oficiais: [validate](https://documentation.red-gate.com/flyway/reference/commands/validate) compara histórico/checksums; [ignoreMigrationPatterns](https://documentation.red-gate.com/fd/flyway-ignore-migration-patterns-setting-277579002.html) default *:future explicitamente removido no pós. Nenhuma execução Flyway/SQL pelo agente.

Contrato reconciliado com [Cedro](../../backend/evidencias/d21-contrato-flyway.md): wms.bootstrap.skip/initSql separados, default THROW sem marcador; skip=false fixo no filho. Maven -o/cache existente/settings vazios, sem instalar/baixar distribuicao. Prumo lê/conferirá o POM antes de qualquer prompt/conexão; nenhum callback/build. V1–V9 usam placeholder wmsDatabase e não hardcode DEV/PROD, conforme [auditoria](../evidencias/d21-migrations-alvos.json); bytes e checksums originais preservados.
