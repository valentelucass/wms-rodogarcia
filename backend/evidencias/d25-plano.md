# D25 — validação atual do backend

06/10/2026. Escopo Cedro: somente backend; sem alteração de migrations, runtime compartilhado, orientações centrais ou grafo. AGENTS, estado D25, continuidade, documento39 e guia público do runtime SQL foram consultados. D24 já aplicou V1–V9 administrativamente; isso não comprova runtime JPA.

1. Concluir o clean verify inicialmente disparado, JDK21, Maven 384 MiB, testes 768 MiB, build isolado `target-d25`, settings vazias versionadas. Preservar log, XMLs, JAR e contagem desta execução antes de alterações/repetição.
2. Corrigir D25-VIG01: exigir SQLException 1222 proveniente da busca bloqueada; erro de permissão, conexão ou reset não deve aprovar o confronto. Testar a distinção offline. Conferir também a compatibilidade do perfil preparado do IT com a guarda de perfil único.
3. Confrontar a configuração JDBC/TLS atual com a leitura sanitizada de Prumo e a documentação Microsoft; corrigir somente canal necessário, sem bypass, administrador na aplicação ou configuração global.
4. Formatar Java e executar clean verify final somente se houver alteração material. Preservar os resultados inicial e final separadamente. HTTP, serviços, arquitetura e regressão existentes compõem a suíte; H2 não comprova SQL Server.
5. Executar o JAR final em `local`, loopback/porta dinâmica, sem persistência/SQL/H2; conferir status e recusas/rotas/erros e encerrar exclusivamente o processo criado para este ensaio.
6. Atualizar apenas os trechos atuais do README sobre D24/configuração e entregar `d25-cedro.md`, logs, XMLs, resumo e hashes próprios. Históricos D19/D20/D21 permanecem históricos.

Bloqueio confirmado por `orchestracao/.runtime/d25-prumo-identidade.json` e `d25-prumo-resumo.json`: usuário próprio/roles/permissões e canal de credencial de aplicação ausentes, `backendSqlLiberado=false`. Não haverá API/sqlserver-it SQL, JPA validate real nem fixture persistida nesta entrega. Nenhum grant/login será criado. A decisão externa restante é provisionar identidade própria restrita somente WMS_DEV. Não existe mais o antigo bloqueio administrativo D23.

Atualização focal: o primeiro JAR pós-TLS passou status/GET recusado, mas o POST403 criou Set-Cookie (VIG03). Tentativas, JAR/build408 e XMLs foram preservados. O teste focal anterior ao patch reprovou 1/1 exatamente no booleano cookie. `d25-csrf-diagnostico.md` registra causalidade e patch local CSRF/STATELESS para Vigia. A mudança Java justifica o clean verify final posterior, seguido de novo ensaio do JAR, mantendo as mesmas expectativas HTTP e sem cookie/SQL/H2.

Encerramento: clean verify final pós-CSRF408/0/0/0 em20 XMLs novos, seis checks do JAR novo aprovados, PID29168 encerrado. Sem SQL/H2/persistência no JAR; IT SQL não executado pelo bloqueio confirmado. README atual/evidências/históricos/artefatos separados, com entrega em `d25-cedro.md` e freeze próprio. Não haverá nova rodada integral sem mudança relevante.
