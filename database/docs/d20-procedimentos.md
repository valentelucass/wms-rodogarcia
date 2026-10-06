# D20 — criação, migrations e ensaios separados

**Entrega corrente MANUAL:** duplo clique em `iniciar-bancos.bat` ou `database\iniciar-bancos.bat` na raiz; `--offline` não lê segredo/SQL. Um orquestrador reutiliza inspeção/guardas/criação, prompt oculto/cópias por etapa, confirma o alvo na interação e executa DEV antes PROD. Existentes com schema/dados são apenas Check: identidade/nome exato/ONLINE/metadados completos. Novos exigem CREATE vazio/model preflight. Corrida na segunda consulta converte Create para Check na mesma sessão, antes de DDL/model. Nenhum SQL pelo agente. [Guia curto](../README.md), [fontes/diferenças](d20-referencias-launcher.md) e [21 fixtures](../evidencias/d20-launcher-final.json).

Pacote local em arquivos. Lucas confirmou **TCP127.0.0.1:1433, SQL auth sa, master**. Credencial no contexto protegido ainda ausente: nenhum executor conectado nesta entrega. CREATE vazio de **WMS_DEV e WMS_PROD** autorizado condicionalmente por Lucas; migrations/cargas em PROD recusadas. Padrões do servidor existente, sem instalação/atualização ou alteração de serviços/versão/edição/collation/compatibilidade/configurações existentes, GRANT, outros bancos ou rotinas.

## Conferência offline reproduzível

Na raiz WMS, PowerShell normal, sem alterar política/confiança:

```powershell
./database/scripts/testar-d20-guardas.ps1
./database/scripts/testar-d20-parser.ps1
./database/scripts/auditar-d20.ps1
./database/scripts/testar-d20-pacote.ps1
./database/scripts/criar-bancos.ps1 -Action Plan -Database WMS_DEV
./database/scripts/criar-bancos.ps1 -Action Plan -Database WMS_PROD
./database/scripts/migrate.ps1 -Action Plan
./database/scripts/ensaio-local-d20.ps1 -Action Plan
./database/scripts/verificar-alvo-criacao.ps1 -Action Plan
```

Defaults Plan não leem ambiente e não conectam. Os checks leem exclusivamente arquivos WMS e fixtures; parser reconstrói CREATE/ALTER/DROP/ADD e recusa sintaxe desconhecida. Inventário não é parser geral de T-SQL nem simulador do motor. Não usar H2 para aceitar CHECKs/índices/locks/permissões/recuperação SQL Server.

## CREATE condicional — antes de migrations

TI/Farol disponibiliza acesso **no próprio contexto protegido**. Falta a credencial administrativa sa: criação real bloqueada, endereço confirmado. Bootstrap em leitura: `verificar-alvo-criacao.ps1 -Action Inspect` exige objeto SecureString ou PSCredential com identidade exata sa, somente do console local. Não lê senha de ambiente, não converte para texto simples, não grava credencial. Usa SqlCredential e descarta sua cópia protegida ao terminar. Não procurar credencial de outro projeto nem pedir senha no chat. sa é apenas criação autorizada, nunca app/migration.

Comando preparado para **operador local com credencial autorizada**, não executado nesta sessão. O prompt não coloca senha na linha de comando/histórico:

```powershell
$senhaLocal = Read-Host 'Credencial sa WMS autorizada (entrada local protegida)' -AsSecureString
try {
    ./database/scripts/verificar-alvo-criacao.ps1 -Action Inspect -SenhaLocal $senhaLocal
} finally {
    $senhaLocal.Dispose()
    Remove-Variable senhaLocal
}
```

O resultado contém somente metadados de master/ServerName/versão/existência dos dois nomes. Falta validar TLS/ServerName/existência no motor; não adivinhar nem transformar o resultado automaticamente em autorização. Após Farol conferir ServerName, entrada CREATE opcional com PSCredential e parâmetros **somente de metadados**:

```powershell
$servidorConfirmado = Read-Host 'ServerName conferido com Farol (metadado)'
$senhaLocal = Read-Host 'Credencial sa WMS autorizada (entrada local protegida)' -AsSecureString
$credencialLocal = [System.Management.Automation.PSCredential]::new('sa', $senhaLocal)
try {
    ./database/scripts/criar-bancos.ps1 -Action Create -Database WMS_DEV -ServidorConfirmado $servidorConfirmado -AlvoConfirmado '127.0.0.1:1433/WMS_DEV' -CredencialLocal $credencialLocal
    ./database/scripts/criar-bancos.ps1 -Action Create -Database WMS_PROD -ServidorConfirmado $servidorConfirmado -AlvoConfirmado '127.0.0.1:1433/WMS_PROD' -CredencialLocal $credencialLocal
} finally {
    $senhaLocal.Dispose()
    Remove-Variable senhaLocal,credencialLocal
}
```

DEV deve concluir todas as pós-condições para continuar. Falha interrompe a sequência e conserva o banco; não repetir automaticamente. O launcher não executa migrations; DEV já existente/migrado pode permitir criar somente PROD faltante. Pode usar `-SenhaLocal` diretamente em vez de PSCredential; passar ambos é recusado. Não usar Start-Transcript, exportação de credenciais, senha literal, variável de ambiente persistente ou log de conexão. Entrada protegida não altera as restrições do alvo nem prova acesso; TLS e identidade continuam obrigatórios.

AlvoConfirmado termina no nome exato do banco solicitado. Executor CREATE desta D20 fixa TCP127.0.0.1:1433, master e SQL auth sa, sem escolha de host/login por ambiente. Servidor e ORIGINAL_LOGIN são comparados na própria conexão TLS a master. Não deduzir alvo pelo serviço encontrado nem pela referência a system admin. ServerName ainda não foi observado; falha de cadeia/nome IP do certificado exige encaminhamento TI, sem desligar TLS ou trustServerCertificate.

1. Conferir identidade administrativa específica de CREATE e metadados completos. Criador não é identidade da aplicação. CONTROL para inspeção de model/DEV/novo banco não é concedido pelo script; ausência recusa a prova, sem auto-GRANT.
2. `criar-bancos.ps1 -Action Create -Database WMS_DEV` reobserva existência na sessão identificada: existente vira Check, preserva schema/dados com metadados completos e não carrega DDL/model; inexistente confirma model, autocommit e CREATE do literal. Para banco novo, confirma ONLINE/nome exato/zero objetos de usuário/tipos/schemas extras/principais/membros/permissões inesperadas. Permissões padrão admitidas estão no SQL de inspeção, sem supor metadados completos a partir de zero linhas.
3. Após DEV criado ou existente conferido (inclusive com schema/dados WMS), confirmar WMS_PROD e executar a mesma ação. O executor comprova DEV ONLINE e metadados completos, sem exigir vazio do existente. PROD só recebe CREATE vazio. Não executar migration/carga/DDL adicional em PROD.
4. Qualquer falha gera nova evidência com fase/código; nunca senha, conta real, string JDBC ou mensagem do driver. Erro de CREATE por corrida de nome não leva a substituição. Falha de pós-condição conserva o banco criado e requer inspeção identificada: não DROP/RESTORE/ALTER/renomear/limpar/repetir automaticamente.

CREATE usa autocommit, fora de transação explícita/implícita, conforme [Microsoft](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-database-transact-sql?view=sql-server-ver17#remarks). model é lido, nunca modificado. Nenhuma cópia de objetos/dados/acessos de outros projetos. Padrões de arquivos/tamanho/defaults herdados são observados depois; incompatibilidade é achado, não licença para ajustar configuração compartilhada.

## Migration somente DEV

Contexto diferente do CREATE: WMS_DB_USER identifica aplicação; WMS_DB_MIGRATION_USER/PASSWORD identifica migration e deve ser distinto. sa recusado para ambos. HOST/PORT/NAME=**WMS_DEV**/CONFIRMED_TARGET/CONFIRMED_SERVER explicitamente definidos. Aplicação sem sysadmin/db_owner/DDL; migration com direitos dos objetos WMS/histórico Flyway apenas. InitSql recusa sysadmin/CONTROL SERVER/db_owner/db_securityadmin/db_accessadmin/db_ddladmin e resposta desconhecida em cada conexão; essa guarda não substitui levantamento completo de privilégios/heranças no alvo. Nenhum GRANT executado. Matriz [por coluna](permissoes-d20.md), [consultas](d20-consultas.md) e [inventário](../evidencias/d20-auditoria-final.json).

Wrapper recusa overrides FLYWAY_*, opções Maven/JVM que alterem configuração, arquivos alternativos e marcadores pré-existentes **antes de conectar**. Usa settings vazias próprias, sem abrir configurações pessoais; isso pode exigir dependências oficiais já em cache ou acesso de ferramenta permitido na execução futura. Não contornar com settings de outro projeto. Defaults do wrapper são Plan; Info/Validate também abrem conexão e só podem ser executados no alvo confirmado.

Após preflight, wrapper gera INIT_SQL transitório escapado com sete SETs/ServerName/WMS_DEV/ORIGINAL_LOGIN e marcador, passa skip=false e invoca `validate flyway:<ação>` no POM versionado. Remove marcador/SQL em finally. Perfil POM tem skip=true/default initSql THROW sem o contexto guardado; BUILD SUCCESS com skip não significa migration aplicada. Marcador não é senha nem autorização genérica. Fonte externa gerada precisa ser conferida em cada conexão, antes de schema/histórico; o preflight de outra conexão sozinho não basta.

Sequência futura: Info → Validate → Migrate → Validate/Info, guardando comandos/códigos e histórico real. Confirmar versões1–9/checksums Flyway e SHA dos arquivos. Não usar clean/baseline/repair/outOfOrder nem modificar checksum para resolver erro. GO delimita lotes, não commit; V4 contém backfill revisao_conteudo=versao. Ensaiar V1–V3 → dados fictícios autorizados → V4 → V5–V9 em DEV isolado para verificar evolução e custo/locks de WITH CHECK/backfill; não apenas criar vazio e inferir evolução validada. Script novo ou drift material exige proposta/V10/revisão; nenhuma V10 identificada agora.

Flyway normalmente envolve cada migration em sua própria transação; confirmar comportamento da versão usada/SQL Server e ausência de configuração contrária. [Transações Flyway](https://documentation.red-gate.com/fd/migration-transaction-handling-273973399.html), [initSql por conexão](https://documentation.red-gate.com/fd/environment-init-sql-setting-277578927.html). SETs de uma migration não são configuração do pool. [Microsoft índices filtrados](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver17#required-set-options-for-filtered-indexes).

## Ensaio SQL Server local fictício

Preparado em `ensaio-local-d20.ps1`, default Plan. Execução optativa exige loopback (localhost/127.0.0.1), WMS_DEV confirmado, TLS validado e WMS_D20_ISOLADO=WMS_DEV_LOCAL_SOMENTE_FICTICIO; nunca escolher o servidor encontrado automaticamente. Metadados registra versão/collation/compatibilidade/isolation/recovery/SETs/constraints confiáveis/índices/histórico/permissões sem alterar nada. Sua identidade de diagnóstico deve possuir leitura explicitamente definida; não ampliar acesso da aplicação para consultar histórico Flyway.

Constraints exige as64 tabelas de domínio já migradas, todas vazias e identidade aplicação sem sysadmin/db_owner. O roteiro testa FK ausente, precisão CONTAGEM inválida, múltiplos NULL de chave, exclusividade ATIVA e reserva posterior à reversão. Tudo em rollback e ausência de linhas reconferida; lacunas identity podem ocorrer e são próprias do ensaio. Não testa services/contexto/alcance/pedido integral. Os resultados só serão reais se o runner executar; os arquivos e fixtures offline não contam como esse ensaio.

Concorrência/transações reais: duas sessões aplicação disputam mesma reserva/posição/fechamento/contagem; demonstrar locks compartilhados cliente→armazém→pedido e ordenação de recursos, revalidação após espera, exclusividade sem dupla baixa e rollback se auditoria falhar. Registrar plano/SQL parametrizado sem bindings, duração, bloqueio/deadlock e resultado por chave fictícia. Deadlock1205 exige reavaliar transação inteira com mesma intenção/chave, sem retry cego com UUID novo. Não habilitar snapshot/RCSI/NOLOCK ou mudar configuração para esconder disputa. [Modelo35](../../docs/35-modelo-integrado-e-jornadas-backend.md) e services são fontes da ordem vigente; H2 não atesta locks SQL Server.

## Recuperação e falhas — ensaio ainda não executado

Lucas/TI/DBA define RPO/RTO/retenção/espaço e destino isolado de restauração; parada36h não é RPO. Não criar rotinas nem comandos de RESTORE/backup empresarial neste pacote. Procedimento reproduzível futuro:

1. Identificar ponto do histórico Flyway, SHA das migrations/JAR e versão candidata; pausar writers por autorização do ensaio e produzir backup com checksum no alvo autorizado. Guardar chaves de criptografia/cadeia externamente e testar sua disponibilidade, sem exportá-las ao relatório.
2. Verificar legibilidade/checksums e restaurar realmente em destino isolado aprovado. Não sobrescrever WMS_DEV/WMS_PROD existentes; destino adicional requer definição/autorização específica, sem inventar terceiro nome nesta rodada. VERIFYONLY sozinho não prova conteúdo nem recuperação. [Microsoft RESTORE](https://learn.microsoft.com/en-us/sql/t-sql/statements/restore-statements-for-restoring-recovering-and-managing-backups-transact-sql?view=sql-server-ver17).
3. Comparar histórico/estrutura/constraints confiáveis e fatos por identidade, contexto, quantidade/origem/FIFO/reserva/ponte/ocupação/permanência/versões/JSON/hash/idempotência. Executar replay das intenções fictícias: mesma chave devolve resposta original, intenção diferente recusa, nenhuma dupla baixa/cobrança. Medir perda e tempo reais contra os objetivos, inclusive permissões/TLS do destino restaurado.
4. Na migration falha, inspecionar transação/histórico/efeitos antes de qualquer ação. Registrar se houve rollback total ou resíduos. Correção incremental identificada ou restauração aprovada; nunca apagar histórico, marcar sucesso manualmente, repair/clean automático ou suprimir constraint.

Provas externas pendentes: SQL Server/TLS/opções efetivas, planos/concorrência/DDL/DML/permissões positivas e negativas, aplicação incremental e restauração real. Não bloqueiam o pacote local; não são homologação, piloto ou publicação.
