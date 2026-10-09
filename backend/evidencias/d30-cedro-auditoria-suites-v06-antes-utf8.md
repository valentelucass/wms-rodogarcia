# D30 Cedro: classifica??o corrente ap?s formata??o

61 fontes de testes/helpers, 50 classes locais e 14 contextos Boot HTTP (12 H2 em mem?ria e 2 sem datasource). SQLIT tem 7 m?todos somente compilados. Cada contexto exige guarda efetiva individual, antes dos singletons e dos casos, na mesma execu??o FINAL. Parsers, mappers e mocks t?m canais pr?prios sem SQL Server.

FINAL01 encerrou no Spotless, antes do JUnit: apenas Jornada precisava de formata??o. Log, snapshot e recibo foram preservados; a formata??o34 corrigiu somente esse arquivo. A FINAL seguinte usa verify, todos os sources e todas as classes locais, sem filtro de m?todo, ambiente por whitelist, settings pr?prios, Maven offline e os cinco perfis perigosos desativados. Build e sa?das s?o novos.

XML verde n?o aceita automaticamente requisitos nem GET: o gate exige captura por rota/caso/contexto/handler, f?sico igual e fonte pertinente. SQL Server permanece suspenso.
