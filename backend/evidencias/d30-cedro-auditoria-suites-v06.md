# D30 Cedro: classificação corrente após formatação

61 fontes de testes/helpers, 50 classes locais e 14 contextos Boot HTTP (12 H2 em memória e 2 sem datasource). SQLIT tem 7 métodos somente compilados. Cada contexto exige guarda efetiva individual, antes dos singletons e dos casos, na mesma execução FINAL. Parsers, mappers e mocks têm canais próprios sem SQL Server.

FINAL01 encerrou no Spotless, antes do JUnit: apenas Jornada precisava de formatação. Log, snapshot e recibo foram preservados; a formatação34 corrigiu somente esse arquivo. A FINAL seguinte usa verify, todos os sources e todas as classes locais, sem filtro de método, ambiente por whitelist, settings próprios, Maven offline e os cinco perfis perigosos desativados. Build e saídas são novos.

XML verde não aceita automaticamente requisitos nem GET: o gate exige captura por rota/caso/contexto/handler, físico igual e fonte pertinente. SQL Server permanece suspenso.
