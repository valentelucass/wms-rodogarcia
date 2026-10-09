# D30 Cedro: classificação corrente

60 fontes de testes e apoio conferidas por caminho e SHA. A execução local contém todas as 49 classes JUnit locais descobertas; SqlServerLocalIT permanece somente compilação. Os cinco perfis Maven de SQLIT/Flyway/bootstrap ficam explicitamente desativados, com child offline, settings vazios e ambiente por whitelist. A FINAL usa verify em build novo, sem clean, e valida a formatação de todos os sources.

Há 14 contextos Boot HTTP (12 H2 em memória e ApiHttp/header sem datasource), além da cadeia de segurança MockServlet sem banco e mappers locais. Cada contexto depende da guarda efetiva da mesma execução, antes dos singletons e dos casos. A referência de header na v03 foi qualificada: o contexto histórico pertinente é focal11, não focal02. Nenhuma nova execução ocorreu nesta classificação.

Escritas de evidência ficam em caminhos próprios novos. Os scripts e CHECKs dos testes existentes atingem exclusivamente o H2 efêmero confirmado. D20, D29, migrations e snapshots anteriores continuam preservados. Provas de dialeto, driver, atomicidade e permissões SQL Server reais ficam individualizadas; não impedem estes casos locais. GET requer captura por rota/caso/contexto/handler e físico igual, além do XML; ausência, null ou diferença não recebe aceite.
