# D27 — migration mínima e executor exclusivamente DEV

**Atual: V10 aplicada e conferida somente WMS_DEV**, quatro etapas Flyway exit0/checksum−562012523. [Checkpoint aplicado e atestado](../../orchestracao/.runtime/d27-prumo-aplicado.md). V10/sidecar ativos; não reaplicar nem editar. A preparação abaixo foi preservada como histórico anterior à aplicação.

## Registro da preparação anterior

Estado anterior: **preflight administrativo real concluído; aplicação aguardava revisão Vigia e sinal Farol**. [Checkpoint histórico](../../orchestracao/.runtime/d27-prumo-preflight.md), [metadados](../../orchestracao/.runtime/d27-prumo-preflight.json) e [SELECT completo sanitizado](../../orchestracao/.runtime/d27-prumo-preflight-real.json). Nesse checkpoint nenhuma V10 em migrations ativas; nenhum DDL/DML/grant nessa preparação. A proposta D26 e suas evidências permanecem históricas e intactas.

O SELECT administrativo pela API Get-ProjetosSqlAdminCredential/New-ProjetosSqlConnection em WMS_DEV autenticou sa/dbo, servidorROD-SRVW-001,127.0.0.1:1433, Standard16.0.1000.6/ONLINE, conexão criptografadaTRUE e certificado privado exato. Uma tentativa, sem18456/retry; a recusa do observer D26 não foi reproduzida. CHECK real, habilitado/confiável: `([tipo]='REPARO' OR [tipo]='AVARIA' OR [tipo]='RETIRADA' OR [tipo]='RETORNO_INTERNO' OR [tipo]='SEPARACAO')`. É a forma OR exata protegida contra tautologia na revisão VIG12; definição não inferida da fonte congelada.

Catálogo64 tabelas/687 colunas, FK/CHECK inválidos0/índices desabilitados0. Nove registros SQL comparados com D24 por installed_rank/versão/script/checksum/success; SCHEMA técnico separado. Nove arquivos originais conferidos SHA256 contra baselineD20; V1–V9 não editadas. Capturadas311 permissões explícitas WMSDEV, nenhuma role de banco e sysadmin0, login/usuário/defaultWMS_DEV preservados para comparação posterior. Preflight guardou unit6=40/contagem2 revisão2/fatoAJUSTE0/fixtureIT1. Atestado efetivo próprio e recortes HTTP/SQL serão pós-aplicação.

O [SQL preparado](propostas/d27/V10__permitir_ajuste_estoque_em_fato_permanencia.sql) guarda DEV exato/transação/definição anterior exata e troca somente ck_fato_permanencia_tipo, WITH CHECK, acrescentando AJUSTE_ESTOQUE aos cinco tipos antigos. Pós-condição dentro da transação exige domínio completo conhecido de seis tipos, habilitado/confiável. [Sidecar](propostas/d27/V10__permitir_ajuste_estoque_em_fato_permanencia.sql.conf) fixa executeInTransaction=true: [Flyway script configuration oficial](https://documentation.red-gate.com/fd/script-configuration-277578847.html). Não usa CREATE INDEX/GRANT/UPDATE/DELETE de negócio. Risco material: lock de schema e varredura de validação dos fatos na transação; erro deve reverter a troca, nunca limpar dados/repair. Após fatosAJUSTE, reversão do domínio antigo pode violar histórico; não prevista reversão destrutiva automática.

## Execução preparada

Plan padrão, offline, sem API de credencial/conexão/Flyway:

```powershell
powershell.exe -NoProfile -NonInteractive -File database/scripts/d27-migrate-dev.ps1
```

Após revisão independente/sinal Farol, o mesmo executor permite `-Modo Apply`. O sinal público fixo é orchestracao/.runtime/d27-farol-continuar.json conforme o [EXAMPLE false](../config/d27-revisao.example.json), com os hashes revisados, booleanos reais Vigia/Farol e bancoDEV. O EXAMPLE não é aprovação. O executor recusa ausência/divergência e reconfere os componentes e fontes em cada etapa.

Somente após esse gate/preflight o executor promove o SQL/sidecar idênticos para database/migrations, sem sobrescrever arquivo divergente. Reutiliza perfilPOM bootstrap-local existente, fonte filesystem única e processo-filho existente; nenhum POM/backend alterado. JDK21 privado e JDBC13.4 existente somente no filho, target10 fixo; nomes livres/PROD/pendente alémV10/sidecars inesperados são recusados. PreValidate ignora exclusivamentepending; Migrate/PostValidate/Info têm ignoreMigrationPatterns explicitamente vazio, recusando missing/future/failed/checksum: [Flyway oficial](https://documentation.red-gate.com/fd/flyway-ignore-migration-patterns-setting-277579002.html). InitSql confirma servidor/banco/sa por conexão, SETs e histórico inesperado antes de Flyway; nunca iniciar-bancos.bat DEV→PROD.

Senha obtida somente pela API protegida, SecureString própria por etapa e env somente do filho efêmero; jamais CLI/log/arquivo/env persistente. stdout/stderr brutos descartados, apenas códigos sanitizados. O auxiliarD21 recebeu somente rastreio PID/estado de término; [cópia exata anterior](../evidencias/d27-d21-flyway-antes.ps1.txt) preservada. Timeout mantém cleanup da árvore própria existente. Póscheck exige domínio novo/catálogo/histórico/checksums e permissões/fixtures anteriores intactos. Falha interrompe/preserva, sem clean/repair/baseline/reset.

[Fixtures offline](../evidencias/d27-testes-offline.json):41/41, dois filhoscmd fictícios exit0/2, IDs registrados/encerrados, env limpo. [Execução nativa](../evidencias/d27-executor-native-offline.json):3/3, Plan em outro cwd, PROD recusado, Apply sem review recusado antes da API. Parsers PS/SQL160:0 erros. [Primeira tentativa39/1](../evidencias/d27-testes-offline-tentativa1.json) preserva erro de brace no executor, corrigido antes de qualquer aplicação; nenhum SQL nesses testes. Esses resultados não ensaiam DDL/rollback real nem antecipam sucesso Flyway. Maven/cache/JDBC existentes conferidos sem instalação; goal Flyway real ainda não executado.

Próximo passo: Vigia revisar SQL/executor/guards/definição real e Farol publicar sinal com hashes; Prumo aplica apenas DEV quando receber a continuação. Nenhum novo pedido de autorização a Lucas, callback, observador/rotina permanente, IT que exige vazio ou alteração sharedruntime/global/PROD.
