# D20 — plano e revisão do pacote database local

**Fecho MANUAL Lucas/Farol:** agente não executa SQL. [iniciar-bancos.bat](../iniciar-bancos.bat) e um [orquestrador PS](../scripts/iniciar-bancos.ps1) reutilizam primitivos: senha oculta em memória, inspeção master/TLS e confirmação local. Existentes preservam schema/dados com identidade/nome/ONLINE/visibilidade completos, nunca classificados como novos/vazios. Create reobserva nome e vira Check se existente, antes de DDL/model. Novos exclusivamente vazios; DEV primeiro, PROD sem migration/carga. Esta atualização prevalece sobre condições iniciais de ausência de endereço/recusa simples de existência/DEV vazio. [21 fixtures](../evidencias/d20-launcher-final.json), cmd Offline/erros e [seis referências públicas](d20-referencias-launcher.md) compõem o novo freeze. Nenhuma conexão nesta sessão.

Autorização Lucas/Farol em 06/10/2026, BE03/BE15. Prumo escreve exclusivamente `database/**` e `infra/**`. Cedro integra backend; Vigia revisa em leitura; Farol mantém os registros centrais. Entrega por arquivos/check e resposta do ask, sem callback. V1–V9 e evidências D19 serão preservadas por SHA-256. Nenhum build/backend executado por Prumo.

**Estado corrente:** pacote local em fecho. TCP127.0.0.1:1433, SQL auth sa/master confirmados por Lucas. Farol verificou ausência de credencial WMS protegida e de conector/cofre disponível: CREATE real **BLOQUEADO POR CREDENCIAL**, não por host. ServerName/TLS/existência/conteúdo ainda não observados por conexão. Nenhum SQL executado. CREATE vazio de ambos autorizado condicionalmente; migrations/cargas PROD recusadas. Os registros iniciais abaixo preservam a sequência de revisão; esta atualização prevalece sobre a antiga ausência de endereço.

## Etapas e evidências

1. Congelar o ponto de partida em `evidencias/d20-baseline.json`; reler o conjunto efetivo, incluindo ALTER/DROP/ADD cumulativos, e comparar todos os models herdados/embutidos. Relatório não confundirá contagens de CREATE com schema final.
2. Conferir FKs/CHECKs/chaves/nulos/precisão/datas/histórico e associar índices a consultas existentes. Sem índice novo por conjectura. Exportar inventário e matriz de UPDATE por coluna das anotações, distinguindo SQL esperado de SQL observado por Cedro.
3. Proteger wrapper: validação pura anterior à conexão, identidades distintas, TLS, sessão SET e mesma guarda no Flyway. Testes fictícios sem driver/conexão. Coordenar POM por arquivo com Cedro.
4. Preparar diagnóstico de metadados, sessão e permissões e roteiro de aplicação incremental/rollback/recuperação com critérios de prova SQL Server. Testes e disponibilidade local separados de homologação.
5. Executar apenas checks locais pertinentes; guardar comandos, códigos, falhas, resultados e hashes em `evidencias/d20-*`. Revisão Farol/Vigia antes de qualquer DDL.

## Achados concretos iniciais

| ID | Evidência / impacto | Tratamento proposto |
| --- | --- | --- |
| D20-01 | SETs em V5/V6/V9 não configuram conexões novas. V2/V8 também criam índices filtrados; V4 contém UPDATE técnico de revisao_conteudo. Sessão inadequada pode recusar DDL/DML. | Cedro inicializa cada conexão da aplicação e Flyway; Prumo confere a própria sessão de identidade do wrapper e as guardas de entrada. Nenhuma alteração V1–V9. |
| D20-02 | Wrapper confirma identidade numa conexão e fecha antes de iniciar Flyway. POM permite chamada direta; conferir o alvo na primeira conexão não protege outra conexão redirecionada/configurada de modo diferente. | Guarda de identidade/SET por conexão Flyway, além de TLS; validação de configuração/overrides antes de conectar. Não declarar a consulta prévia como garantia da conexão subsequente. |
| D20-03 | Matriz antiga de UPDATE descreve comandos; Hibernate sem DynamicUpdate pode enviar todas as colunas updatable, ainda que inalteradas. | Gerar matriz completa com herança/embedded/@Version e confrontar SQL capturado por Cedro. Corrigir marcador indevidamente mutável ou proposta de colunas justificada, sem UPDATE geral nem GRANT real. |
| D20-04 | Serviço MSSQLSERVER Running e sqlcmd presentes; não há prova de banco/servidor local exclusivo WMS, versão/collation/TLS/identidades definidas para o ensaio. | Somente detecção local, sem conexão/provisionamento. Insumos ausentes limitam o ensaio real, não o pacote local. |

## Proposta DDL antes de escrita

**Nenhum DDL corretivo de schema/V10 proposto.** V1–V9 permanecem congeladas. CREATE guardado foi proposto e aprovado por Farol no complemento abaixo. A auditoria efetiva não demonstrou defeito material que justifique V10. Eventual correção futura exigirá objeto/constraint exatos, contraexemplo, fonte, impacto, preflight, locks/transação, compatibilidade, recuperação e revisão antes de arquivo DDL. Correções de wrapper, testes, inventário e procedimentos avançam independentemente.

## Insumos indispensáveis e proposta

Lucas/TI/DBA: host/porta/instância e identidade real confirmados, versão/compatibilidade/collation existentes, cadeia TLS válida e nome certificado, identidade aplicação e migration separadas com permissões efetivas, mecanismo externo de segredo, dono/espaço/cópia/restauração. Configuração final de implantação e RPO/RTO/retenção continuam externos; não inferir valores da tolerância de parada de36h.

## Complemento Lucas — criação guardada WMS_DEV/WMS_PROD

Mesma D20, recebido em06/10. Nomes exatos autorizados: `WMS_DEV` e `WMS_PROD`. Alvo/endereço/acesso ainda não confirmados: **nenhuma conexão permitida agora**. A detecção de MSSQLSERVER local não define o alvo. Farol já perguntou o endereço; Prumo não duplica a pergunta nem solicita senha.

Proposta para revisão Farol/Vigia **antes de escrever DDL**:

- Validação pura separa Plan/CREATE/Migrate. Default é plano offline. Banco deve ser exatamente WMS_DEV ou WMS_PROD; migrations aceitam apenas WMS_DEV. Sem SQL concatenado com identificadores fornecidos livremente.
- Criação futura conecta somente ao alvo confirmado, por TLS validado e acesso administrativo externo explicitamente autorizado para CREATE. Consulta identidade real antes da ação e repete conferência na mesma sessão. Referência a system admin não escolhe servidor nem dá sysadmin à aplicação.
- DEV primeiro: somente CREATE DATABASE WMS_DEV, quando inexistente. Se já existir, recusar; não interpretar existência como sucesso, não apagar/restaurar/renomear/alterar. Corrida entre conferência e CREATE falha pela exclusividade do nome; nunca há fallback de substituição.
- PROD: CREATE de banco vazio já autorizado condicionalmente por Lucas; revisão Farol esclareceu que não exige nova autorização. Preparar agora, executar somente com alvo/acesso inequívocos e existência DEV comprovada. Migrations e cargas PROD continuam recusadas.
- CREATE usa padrões existentes de model/instância, sem COLLATE, ALTER DATABASE, opção de compatibilidade, caminhos inferidos de outro banco, mudança de serviço/versão/edição/configuração compartilhada ou GRANT. Registrar defaults efetivos posteriormente; incompatibilidade gera achado, não mudança global silenciosa.
- Após DEV criado, conferir resultado e encerrar executor de criação. Migration DEV é ação posterior e distinta, com identidade própria e autorização/alvo conferidos. PROD é sempre recusado pelo wrapper.
- Impacto esperado: novo banco DEV vazio, arquivos/espaço conforme defaults do servidor escolhido, sem tocar bancos existentes. Custo/espaço/permissão de CREATE dependem do alvo. Recuperação de falha: inspecionar existência/estado no alvo confirmado, registrar falha, não DROP/repair/repetir CREATE automaticamente. Nenhum rollback destrutivo.

**Revisão Farol recebida em06/10:** aprovada preparação do DDL de CREATE com as guardas acima, sem tocar V1–V9. Correção de escopo preservada: criar AMBOS vazios está condicionalmente autorizado; PROD não recebe migrations/cargas. Vigia revisará focalmente. Alvo/endereço/acesso ainda ausentes: nenhuma conexão atual. Não instalar/atualizar SQL Server nem alterar serviço/instância/collation/compatibilidade/config de bancos existentes. Alvo empresarial/local futuro será distinguido do ensaio fictício isolado; H2/estática não são prova SQL Server.

## Registro inicial para check

Em auditoria. Maestri não está no PATH e MAESTRI_CLI ausente neste processo; nenhuma identidade/variável de terceiros foi usada para recuperar comunicação. Nenhuma mensagem enviada. Os arquivos desta pasta são o canal pedido. Leitura de memória/Graphify restrita ao WMS; não escrever mapa nem registros centrais.

## Contrato para Cedro — necessário no POM antes do fecho

Wrapper agora default Plan offline, exclusivamente WMS_DEV em Info/Validate/Migrate; identidades WMS_DB_MIGRATION_USER/PASSWORD distintas de WMS_DB_USER. Recusa FLYWAY_* e overrides em opções Maven/JVM e .mvn/maven.config/jvm.config antes do conector; settings pessoais não são lidos (usa settings vazias próprias) e configFiles é explicitamente vazio. Após preflight, fornece temporariamente WMS_DB_MIGRATION_GUARD=D20_WMS_DEV_VERIFICADO e **WMS_DB_MIGRATION_INIT_SQL** gerado/escapado pela função pura Get-WmsD20FlywayInitSql. Remove ambos em finally. Invoca `validate flyway:<ação>`.

**Integração necessária, POM exclusivo Cedro:** initSql deve começar pela recusa do marcador ausente/divergente na chamada direta e, após isso, usar `${env.WMS_DB_MIGRATION_INIT_SQL}`. Esse SQL contém SETs e comparação BIN2 de ServerName, WMS_DEV e ORIGINAL_LOGIN da identidade migration na conexão efetiva, antes de schema/histórico. Enforcer validate sozinho não cobre goal direto. Marcador não é segredo e não substitui verificação; SQL externo transitório pré-existente é recusado pelo wrapper. Não usar env de terceiros. Cedro deve registrar teste/inspeção dessa integração. Prumo não altera Java/POM nem executa build.

## Revisão autocommit/model e VIG06

Farol12:06/Vigia VIG06: acrescentados @@TRANCOUNT=0 e IMPLICIT_TRANSACTIONS OFF somente na sessão; preflight genérico model e pós-condição no banco novo para objetos/tipos/schemas/usuários/membros/permissões, com visibilidade completa comprovada. Inspeção em WMS_DEV repetida antes de PROD: criar ambos antes de migrar DEV. Defaults de segurança permitidos são somente principais/funções de sistema, permissões públicas de catálogo/criptografia e CONNECT dbo; qualquer acesso adicional ou metadados insuficientes recusa. Nenhuma lista de nomes/contas/dados é retornada.

Falha após CREATE preserva o novo banco e evidencia a fase; não DROP/fallback/repetição. Metadata CONTROL exigido apenas ao DBA/criador para comprovar segurança completa, nunca à aplicação, sem GRANT automático. Fundamentação: [Microsoft CREATE DATABASE](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-database-transact-sql?view=sql-server-ver17#remarks) e [visibilidade de permissões](https://learn.microsoft.com/en-us/sql/relational-databases/system-catalog-views/sys-database-permissions-transact-sql?view=sql-server-ver17). Nenhuma conexão/CREATE foi executada.

## Alvo confirmado Lucas — atualização da mesma D20

Lucas confirmou TCP127.0.0.1:1433, SQL auth sa, master. Autoriza CREATE de ambos quando identidade/existência conferidas, DEV primeiro, sem sobrescrita/alteração de defaults/servidor/outros bancos/logins. **Credencial ainda ausente no contexto protegido**, portanto nenhuma conexão/CREATE atual. Não pedir senha no chat nem ler ambiente/segredos de outro projeto/terminal. Farol cuida do guia compartilhado ../.runtime; Prumo não o acessa/altera.

`verificar-alvo-criacao.ps1` default Plan; Inspect futuro usa esse endpoint exato, TLS validado e SecureString/PSCredential do console local. Não lê senha de ambiente nem material de outro projeto. Consulta somente ServerName/master/login confirmado/versão e existência dos dois nomes; não adivinha ServerName nem copia resultado automaticamente para autorização. Farol confere o retorno antes de fornecer `-ServidorConfirmado`/`-AlvoConfirmado` ao CREATE, que repete identidade na mesma sessão efetiva. Credencial CREATE separada de app/migration; sa nunca é conta da aplicação. Falha TLS não autoriza confiar no certificado sem validação.

Farol confirmou sem ler valores: WMS_DB_PASSWORD e WMS_DB_MIGRATION_PASSWORD ausentes em Process/User/Machine; guia compartilhado não oferece mecanismo administrativo WMS autorizado. Porta alcançável/MSSQLSERVER Running não são autenticação/TLS nem SQL válido. Prumo não leu o guia nem credenciais. Entrada futura usa `SqlCredential` com cópia SecureString somente em memória e descarte, sem senha literal, ambiente persistente ou log. [Procedimento local](d20-procedimentos.md) contém comandos para operador; não foram executados.

Fecho: Farol confirmou também ausência de WMS_DB_CREATION_PASSWORD. Esta versão dos executores CREATE/Inspect não depende de variável de senha: entrada exclusivamente por objeto protegido. Wrapper migration recusa sa no preflight e roles amplas/CONTROL SERVER/resposta desconhecida no initSql de cada conexão; nenhuma concessão. Matriz atual corresponde ao SQL final Cedro, sem DynamicUpdate. [Consultas/índices](d20-consultas.md), [prontidão](../../infra/d20-prontidao.md) e [relatório/manifesto](../evidencias/d20-relatorio-reorganizacao.md) são o canal para revisão final.
