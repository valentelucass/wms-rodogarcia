# Database local — engenharia e validação BE03/BE15

**D20 histórica; launcher supersedido pela D21 expressa de Lucas.** O comportamento vigente é bootstrap/upgrade completo DEV e PROD pelo operador, conforme [documento37](37-bootstrap-e-upgrade-manual-database.md), [D21](06-decisoes-e-pendencias.md) e states. Resultados/texto D20 abaixo preservados; fonte original deste relatório foi copiada para `orchestracao/.runtime/d21-base-central/` antes desta nota. Não executar a orientação antiga de apenas bancos vazios como entrega atual.

## Autorização e escopo D20

Em06/10/2026 Lucas autorizou o macrobloco de database após o backend D19 concluído. A [D20](06-decisoes-e-pendencias.md) registra autorização e complementos; o [states](../states.md) mantém BE03/BE15 e critérios externos. Modelo35, contratos14–31 e respostas10/regras11 continuam as fontes de negócio. Modelo, questionário e IDs preservados.

Entregar auditoria efetiva V1–V9/JPA/permissões, correções locais justificadas e automações/testes/procedimentos reproduzíveis. O complemento mais recente confirmou TCP `127.0.0.1:1433`, SQL auth administrativa `sa`, conexão inicial `master`, e autorizou criar ambos os bancos exclusivos `WMS_DEV`/`WMS_PROD`, DEV primeiro, após verificar identidade/existência e sem sobrescrita. PROD sem migrations/cargas. Sem grants reais, alteração global SQL/terceiros, publicação, fiscal/cobrança, frontend, commit/push, rotinas ou ETL. Preparação/H2/leitura estática não comprovam SQL Server.

**Entrega vigente manual, mesma D20:** Lucas executará `database/iniciar-bancos.bat` localmente; agente não executa SQL. Launcher pronto/testado é a prioridade: console visível, senha oculta apenas em memória, inspeção/confirmação de identidade/alvo na interação, criação dos nomes ausentes DEV primeiro e preservação/conferência de existentes, inclusive retomada. Migrations DEV em entrada distinta, nenhuma migration/carga PROD automática. Prumo implementa reaproveitando guardas; Vigia revisa. Não repetir suite backend completa para alteração somente de launcher/procedimento.

## Distribuição e preservação

**Organização da mesma entrega:** Lucas autorizou raiz database com somente README curto e BATs necessários ao operador. Prumo inventaria e coordena movimentos em database/infra; PS1 em `scripts/`, planos/matrizes/procedimentos em `docs/`, fontes SQL/migrations/contratos/evidências nas pastas próprias. Sem exclusão; inventário/de-para e hashes registram preservação. Manifestos históricos permanecem históricos; o manifesto corrente registra caminhos novos e a justificativa. Farol atualiza referências centrais/guia compartilhado e mapa; Vigia confere o layout e os testes após a reorganização. Nenhum SQL nem repetição do build backend integral.

Regra durável no AGENTS canônico: servidor existente imutável, bancos exclusivos sem sobrescrita, entrada protegida de segredo e identidades próprias de mínimo privilégio. `sa` serve somente à criação administrativa expressamente autorizada. Consultar guias/metadados não sensíveis de runtime e escrever guia compartilhado sem senha também foi autorizado; `.env`, credenciais privadas de terceiros, dados/históricos e controles/ponte ETL não foram consultados. [Guia compartilhado](../../.runtime/SQL-SERVER-EXISTENTE.md) criado, com link acrescentado ao README preexistente; nenhum arquivo/configuração de execução compartilhado foi substituído.

`maestri list` conferiu WMS - Farol (Supervisão), WMS - Prumo (Banco e Infra), WMS - Cedro (Backend), WMS - Vigia (Revisão), WMS - Lume (Frontend) e Hermes WMS (Hermes), ligados à nota WMS - Continuidade. Nenhum destino ETL. CLI executado com identidade do próprio Farol do cadastro WMS; ambiente da chamada original estava sem MAESTRI_PIPE/CLI, diagnosticado antes da recuperação local. Sem alteração de terminal, conexão, perfil ou rotina.

| Responsável | Arquivos e responsabilidade |
| --- | --- |
| WMS - Prumo | database/** e infra/**; auditoria SQL/permissões/procedimentos e melhorias locais |
| WMS - Cedro | backend/**; integração database necessária, testes/build proporcionais |
| WMS - Vigia | Leitura independente; parecer novo exclusivo em orchestracao/.runtime/d20-vigia-*.md |
| WMS - Farol | states, decisões, continuidade, índice, documento36, mapa e resultado consolidado |

Baseline SHA-256 de333 arquivos de backend/src, migrations e docs em `orchestracao/.runtime/d20-baseline-fontes.json`; estado Git inicial em `d20-git-inicial.txt`. Repositório iniciou com arquivos preexistentes não rastreados; não limpar/reverter nem inferir autoria pelo status Git. Preservar V1–V9 congeladas, históricos, fontes originais e artefatos D19.

## Resultado

**Entrega local concluída:** launcher manual e organização de database prontos/testados. [Revisão Vigia](../orchestracao/.runtime/d20-vigia-revisao-final-2026-10-06.md) favorável ao launcher/layout, com limite VIG07 do runtime e atestação completa externa explícitos abaixo. [Conferência independente Farol](../orchestracao/.runtime/d20-entregas-check.json):722/722 hashes database,404/404 backend,489/489 conteúdos originais e34 movimentos preservados,3 fontes originais de referência intactas; zero divergências. Nenhuma validação SQL ou privilégio real presumido.

Integração backend verificada: `clean verify` final em06/10/2026 às12:32:28 -03, **396/0/0/0 em19 suítes**, BUILD SUCCESS,4min09s. [Log real](../backend/evidencias/d20-clean-verify.log). Código/JAR/XMLs preservados após fechamento de launcher e esclarecimento documental VIG07; nenhum novo build integral. SHA-256 JAR `03BFE49B70E7F119324ED21AAA065F590473BF7275232C83EB1B57D26B1AEBB4`.

Após a reorganização, raiz com somente [iniciar-bancos.bat](../database/iniciar-bancos.bat) e [README curto](../database/README.md). [Inventário/de-para](../database/evidencias/d20-organizacao-de-para.json):34 movimentos com SHA idêntico,489 arquivos anteriores preservados atuais ou em cópias originais. Scripts em scripts/, documentos técnicos em docs/, XML em config/; migrations/SQL/contratos/evidências mantidos. [Relatório corrente](../database/evidencias/d20-relatorio-reorganizacao.md), [organização](../database/docs/d20-organizacao.md) e [manifesto corrente](../database/evidencias/d20-manifesto-corrente-reorganizacao.json); [manifesto anterior](../database/evidencias/d20-manifesto-final.json) permanece histórico.

| Verificação local após movimentos | Evidência |
| --- | --- |
| Orquestração manual21/21, sucesso/existentes/retomada/corrida/falhas/descarte de senha | [Fixtures](../database/evidencias/d20-launcher-reorg.json), [saída nativa0](../database/evidencias/d20-launcher-reorg-execucao.json) |
| Guardas80/80 e parser13/13 | [Guardas](../database/evidencias/d20-guardas-reorg.json), [parser](../database/evidencias/d20-parser-reorg.json) |
| Pacote336/336,271 links locais | [Pacote](../database/evidencias/d20-pacote-reorg.json) |
| cmd.exe real: offline0, outro cwd0, inválido2;3/3 pelo Farol | [Execução independente](../orchestracao/.runtime/d20-cmd-pos-organizacao-farol.json) |

O fluxo com conexão foi testado com adapters/fixtures fictícios, sem executar DDL real. O modo offline do BAT não coleta senha nem abre conexão. Janela/entrada oculta/TLS/identidade/criação efetivos permanecem para execução manual do operador; nenhuma migration/carga PROD automática.

### Auditoria efetiva e melhorias concretas

A [auditoria](../database/evidencias/d20-auditoria-final.json) reconstrói a estrutura após80 ALTERs, incluindo substituição de CHECKs: **64 tabelas,687 colunas,130 FKs,200 CHECKs efetivos,61 UNIQUE e83 índices explícitos (5 filtrados)**. JPA com herança/embedded/nullabilidade/tipos/precisão/imutabilidade foi pareado sem divergência. O inventário é estático e delimitado à sintaxe das migrations WMS, sem alegar validação do motor.

As [consultas/índices](../database/docs/indices-d20.md) relacionam índices existentes e métodos concretos dos repositories; seletividade/uso/custo dependem de planos e volume reais. Nenhum índice, abstração genérica, trigger ou migration adicional foi criado. V1–V9 permanecem9/9 iguais ao baseline. Precisão decimal, datetime2(6), origem/FIFO/histórico, FKs/CHECKs/unicidades/nulos e transações/idempotência continuam ligados aos contratos e services existentes.

Hibernate preparou **34 UPDATEs para64 entidades** com SQLServerDialect, sem JDBC. A comparação de campos históricos identificou UPDATEs indevidos; `updatable=false` agora preserva fatos originais de chegada/item físico, referência/criação do pedido, emitente/série/número/emissão da nota, sequência/quantidade prevista e referência administrativa. Estorno, XML/chave/hash e valor desconhecido enriquecido preservam mutações contratadas. [Matriz de UPDATE por coluna](../database/docs/permissoes-d20.md) e [comparação](../backend/evidencias/d20-confronto-update.json); não há GRANT real nem alteração de schema/modelo de negócio.

Aplicação exige somente WMS_DEV, TLS validado e identidade real ServerName/banco; inicializa os sete SETs em cada conexão. A guarda de privilégios recusa os casos enumerados: sysadmin, db_owner, db_ddladmin, CONTROL SERVER/DATABASE, CREATE TABLE e ALTER no schema wms. Recusa ações alternativas JPA/Hibernate, scripts de inicialização, conexões substitutas e Flyway/Liquibase automáticos. Comparação BIN2 é expressão de verificação, sem mudar collation/defaults do servidor ou banco.

**VIG07 — limite do runtime e condição de uso:** a guarda acima não consulta CREATE SCHEMA nem todas as roles administrativas, portanto não comprova ausência geral de DDL/administração nem mínimo privilégio. A política da [matriz](../database/docs/permissoes-d20.md) é requisito de provisionamento/atestação, não privilégio real observado. Adotada a alternativa documental proposta por Vigia: exigir atestação completa prévia das identidades/permissões efetivas próprias antes do uso operacional/ensaio DEV, incluindo CREATE SCHEMA, db_securityadmin/db_accessadmin e roles administrativas de servidor. Migration possui direitos próprios necessários ao schema WMS, separados da aplicação e de sa. Nenhuma atestação executada. O predicado/código/JAR permanecem os verificados396/404; aprovação do pacote manual não aprova runtime, grants ou SQL. [Parecer](../orchestracao/.runtime/d20-vigia-revisao-final-2026-10-06.md).

O wrapper de migrations passou a default Plan offline, credenciais de migration separadas, settings locais vazias e recusa de overrides antes do conector. POM mantém skip padrão e initSql de recusa; o wrapper fornece SQL transitório escapado com identidade/SETs para cada conexão efetiva antes de schema/histórico. Somente WMS_DEV; nenhum clean/baseline/repair automático. Marcador é proteção contra erro operacional, sem promessa de impedir adulteração maliciosa do executor.

### Criação real e ensaio SQL Server

**Criação real não executada: execução transferida expressamente ao operador manual.** Antes da atualização, Farol verificou somente presença das entradas WMS em Process/User/Machine, sem ler/imprimir valores; a credencial administrativa estava indisponível no canal protegido desta sessão. Essa ausência não impede a entrega manual, nem exige senha no chat. TCP alcançável e MSSQLSERVER Running observados em06/10 às12:28:44 -03; [evidência sanitizada](../orchestracao/.runtime/d20-acesso-sql-sanitizado.json). Não houve autenticação SQL, leitura de sys.databases, CREATE, schema/migrations, carga ou grants. Existência de WMS_DEV/WMS_PROD permanece não consultada pelo agente.

Preparados [CREATE guardado](../database/scripts/criar-bancos.ps1), [inspeção de identidade](../database/scripts/verificar-alvo-criacao.ps1) e [procedimento](../database/docs/d20-procedimentos.md), agora integrados ao launcher manual. Entrada segura local em memória, sem segredo literal em comandos e com cópia independente por etapa para não reutilizar SecureString descartada. Execução manual verifica master/ServerName/login/TLS/existência, model e pós-condição de vazio/acessos em bancos novos, DEV antes de PROD. Nomes existentes são preservados/conferidos, sem DDL sobre eles, e a retomada trabalha somente no nome ausente. Falhas/divergências são explicadas, sem DROP, substituição ou repetição automática.

Preparados [runner de metadados/constraints](../database/scripts/ensaio-local-d20.ps1) com rollback fictício e [perfil optativo SQL Server](../backend/evidencias/d20-ensaio-sqlserver.md) com sete ITs de SETs/constraints/unicidade/unicode/data/rollback/idempotência/lock em duas conexões. Nenhum executado no motor. Recuperação inclui evolução V1–V3→V4–V9 e restauração real em destino isolado aprovado; VERIFYONLY sozinho não comprova recuperação. Não criar terceiro banco, grants ou rotinas sem escopo próprio.

### Próximo passo concreto

Lucas deve abrir **`database/iniciar-bancos.bat`** e fornecer a senha na entrada local oculta. O console inspeciona identidade/TLS/existência e solicita confirmação do alvo na própria interação; cria somente nomes ausentes, DEV primeiro, e verifica nomes exatos em sys.databases/pós-condições. Existentes são preservados/conferidos. Não configurar o servidor para contornar falhas. Agente não executa essa etapa. Pacote local encerrado; resultado e estados observados dos terminais em `orchestracao/.runtime/d20-resultado-final.md` e JSON correspondente.

Para ensaiar schema DEV depois da criação: identidades WMS de aplicação/migration separadas e atestação completa dos direitos efetivos próprios, cadeia/nome TLS válidos e confirmação de isolamento fictício. A passagem da guarda do runtime não substitui essa atestação, conforme VIG07. Para recuperação: destino isolado autorizado e insumos de backup/RPO/RTO/espaço. Versão/collation/compatibilidade serão observadas, sem valores inventados nem alteração do ambiente. BE03/BE15 continuam em validação externa; frontend/homologação/publicação seguem fora desta entrega.

### Referências públicas de organização consultadas

Farol e Prumo consultaram somente os READMEs/BATs expressamente indicados, sem abrir configs, `.env`, credenciais, histórico/dados ou controles ETL. São referências de organização, sem autorização de acesso a seus bancos. Hashes sanitizados em `orchestracao/.runtime/d20-referencias-publicas.json`.

| Fonte pública | Padrão aproveitado / diferença WMS |
| --- | --- |
| [Satelite README](../../satelite-tms-api/database/README.md) e [subir_database.bat](../../satelite-tms-api/database/subir_database.bat) | Entrada BAT, caminhos relativos e allowlist. WMS usa nomes/alvo próprios, não lê `.env`/senha em arquivo, não concede acessos e não copia `-C`/trustServerCertificate. |
| [Dashboards README](../../dashboards-etl/database/README.md) e [executar_database.bat](../../dashboards-etl/database/executar_database.bat) | Separação de migrations versionadas/validação e modos Flyway. WMS mantém criação separada; somente DEV no executor de migrations, sem usar conta compartilhada/administração como runtime. |
| [Avaliação README](../../avaliacao-desempenho-competencias/database/README.md) e [executar-database.bat](../../avaliacao-desempenho-competencias/database/executar-database.bat) | DEV antes de PROD, falha DEV interrompe PROD, códigos de saída e configuração/credencial separadas. WMS mantém BAT pequeno, confirmação interativa e criação somente; nenhuma migration PROD automática e nenhum runner amplo copiado. |

Avaliação documenta pin de certificado público por sqlcmd Go `-J` e perfil TLS privado; isso não prova confiança automática para System.Data.SqlClient do WMS. O launcher exige encrypt/validação de certificado, sem herdar truststore/credencial/perfil privado de outro projeto. Compatibilidade real de cadeia/nome/certificado não foi ensaiada, pois nenhum SQL é executado pelo agente; falha interrompe a operação e é explicada no console, sem bypass, instalação ou alteração de SQL/confiança global. A validação manual precisa usar a confiança já aprovada para esse cliente.

## Referências técnicas consultadas

Os critérios de sessão usam [Microsoft CREATE INDEX](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver17#required-set-options-for-filtered-indexes). A criação segue [Microsoft CREATE DATABASE](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-database-transact-sql?view=sql-server-ver17#remarks), incluindo autocommit e herança de model; por isso a confirmação de ONLINE precisa de pós-condição própria para conteúdo e acessos. Essas fontes orientam arquivos e testes, sem comprovar o ambiente desconhecido.

[Flyway SQL Server](https://documentation.red-gate.com/fd/sql-server-database-277579330.html), [Maven Goal](https://documentation.red-gate.com/fd/maven-goal-277579365.html) e [Init SQL](https://documentation.red-gate.com/flyway/reference/configuration/environments-namespace/environment-init-sql-setting) fundamentam lotes GO, precedência de configurações e guarda de cada conexão. Transação de migration, transação operacional, criação de banco e recuperação têm critérios separados; não alterar defaults globais para contornar uma incompatibilidade.
