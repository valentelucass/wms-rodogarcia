# D26 — ensaio HTTP real DEV

Autorização D26 de Lucas lida em AGENTS/estados/decisões/continuidade. Cedro escreve somente backend; Prumo provisiona/atesta identidade WMSDEV e revisa persistência por SELECT; Vigia revisa. Não alterar central, migrations/checksums/schema, sharedruntime, grafo ou frontend. D25 e suas tentativas/freezes permanecem históricos.

Ordem: inventário completo das rotas/DTOs/regras e matriz objetiva; aquisição protegida da identidade atestada em d26-prumo-pronto.json; conferência real WMS_DEV/servidor/login/TLS/guardas/vazio; sete sqlserver-it antes da população, preservando fixture de lock; JAR atual JDK21/JDBC13.4 sqlserver-dev/Hibernate validate com issuer/JWKS/RSA local efêmero; jornadas HTTP com Gestor/Supervisor/Operação e alcance, positivos/recusas/transições/replay/confrontos GET/SQL/auditoria; concorrência coordenada via HTTP; evidências e encerramento somente dos processos próprios.

Todas as escritas de negócio do roteiro serão por rotas HTTP existentes. Dados novos rastreáveis por D26+ID de rodada; preservação das anteriores, sem reset/DELETE/DROP/clean Flyway/repair/baseline/PROD/BAT. Token/chave/senha não entram em evidências. Segredo somente API protegida e memória/canal efêmero do filho. Nenhum sa em API/IT.

A matriz terá uma linha por endpoint e casos por regra/RN/AC/documento, com expected/actual/status/perfil e GET/IDs/SQL/auditoria. Cobertura incompleta permanecerá explicitamente não executada com motivo; AC propostas não se tornam homologadas. Falha material será reproduzida, corrigida proporcionalmente com regressão e build atual. DDL necessário será proposta separada, sem tocar schema aplicado.
