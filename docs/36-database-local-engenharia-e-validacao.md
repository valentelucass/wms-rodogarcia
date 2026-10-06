# Database local — engenharia e validação BE03/BE15

## Autorização e escopo D20

Em06/10/2026 Lucas autorizou o macrobloco local de database após o backend D19 concluído. A [D20](06-decisoes-e-pendencias.md) registra limites; o [states](../states.md) mantém BE03/BE15 e critérios externos. Modelo35, contratos14–31 e respostas10/regras11 continuam as fontes de negócio. Não recriar o modelo nem reabrir o questionário.

Entregar auditoria efetiva V1–V9/JPA/permissões, correções locais justificadas e automações/testes/procedimentos reproduzíveis. Preparação em arquivo e H2/leitura estática não comprovam SQL Server. O pedido não autoriza SQL Server da empresa, servidor externo, grants reais, produção ou ações externas.

## Distribuição e preservação

Complemento da mesma demanda: bancos exclusivos `WMS_DEV` e `WMS_PROD`, DEV primeiro. Preparar criação sem sobrescrita com guardas de alvo/acesso; nenhuma migration/carga PROD nesta rodada. Proibidas alterações globais/instalação/edição/versão/serviços/instância/collation do servidor e compatibilidade/configurações de bancos existentes, outros bancos/acessos/rotinas. A referência “system admin” não determina servidor nem concede sysadmin à aplicação. Endereço exato ausente na documentação WMS; solicitado somente servidor/instância ou host:porta, sem senha. Regra durável no AGENTS canônico; nenhuma conexão/criação efetuada.

`maestri list` conferiu WMS - Farol (Supervisão), WMS - Prumo (Banco e Infra), WMS - Cedro (Backend), WMS - Vigia (Revisão), WMS - Lume (Frontend) e Hermes WMS (Hermes), ligados à nota WMS - Continuidade. Nenhum destino ETL. CLI executado com identidade do próprio Farol do cadastro WMS; ambiente da chamada original estava sem MAESTRI_PIPE/CLI, diagnosticado antes da recuperação local. Sem alteração de terminal, conexão, perfil ou rotina.

| Responsável | Arquivos e responsabilidade |
| --- | --- |
| WMS - Prumo | database/** e infra/**; auditoria SQL/permissões/procedimentos e melhorias locais |
| WMS - Cedro | backend/**; integração database necessária, testes/build proporcionais |
| WMS - Vigia | Leitura independente; parecer novo exclusivo em orchestracao/.runtime/d20-vigia-*.md |
| WMS - Farol | states, decisões, continuidade, índice, documento36, mapa e resultado consolidado |

Baseline SHA-256 de333 arquivos de backend/src, migrations e docs em `orchestracao/.runtime/d20-baseline-fontes.json`; estado Git inicial em `d20-git-inicial.txt`. Repositório iniciou com arquivos preexistentes não rastreados; não limpar/reverter nem inferir autoria pelo status Git. Preservar V1–V9 congeladas, históricos, fontes originais e artefatos D19.

## Resultado

Em andamento. Não há aceite D20 antecipado. Evidências, achados corrigidos, comandos/resultados reais, estados observados dos participantes e próximo ensaio serão consolidados após a entrega e revisão independente.

## Referências técnicas consultadas

Os critérios de sessão usam [Microsoft CREATE INDEX](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver17#required-set-options-for-filtered-indexes). A criação segue [Microsoft CREATE DATABASE](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-database-transact-sql?view=sql-server-ver17#remarks), incluindo autocommit e herança de model; por isso a confirmação de ONLINE precisa de pós-condição própria para conteúdo e acessos. Essas fontes orientam arquivos e testes, sem comprovar o ambiente desconhecido.

[Flyway SQL Server](https://documentation.red-gate.com/fd/sql-server-database-277579330.html), [Maven Goal](https://documentation.red-gate.com/fd/maven-goal-277579365.html) e [Init SQL](https://documentation.red-gate.com/flyway/reference/configuration/environments-namespace/environment-init-sql-setting) fundamentam lotes GO, precedência de configurações e guarda de cada conexão. Transação de migration, transação operacional, criação de banco e recuperação têm critérios separados; não alterar defaults globais para contornar uma incompatibilidade.
