# D30 Cedro — inventário e plano inicial

258 arquivos Java produtivos analisados pelo javac, sem diagnóstico de compilação e sem executar aplicação, testes ou conexões. O inventário identifica tipos aninhados, enums, records, campos, parâmetros e variáveis; registra tipos resolvidos, anotações/defaults, chamadas, usos entre arquivos, casts e atribuições por caminho produtivo e SHA. Nenhuma declaração está aceita apenas por estrutura.

Os casos locais já identificados são: R$0,01 entre três notas em seis ordens; proteção da saída histórica do teste de mapeamento; prova fail-closed dos 12 contextos HTTP; contratos JSON Long e decimal. Os 727 confrontos normativos e as 258 revisões de fontes são individualizados no plano. A decomposição revisada de Lume e o extrator revisado do Prumo ainda serão importados; isso permanece explícito como trabalho local em curso.

HTTP local com H2 efêmero, isolado e fictício está autorizado pela correção expressa das 23:18:25Z. A triagem inicial contrária é histórica e supersedida. SqlServerLocalIT e acesso SQL Server continuam impedidos. SqlServerUpdateMappingTest gera SQL estático sem JDBC, mas tem achado concreto de sobrescrita D20; o destino será corrigido antes de executá-lo. Zero ações SQL Server.

Artefatos próprios: `d30-cedro-inventario-v01.json`, `d30-cedro-inventario-javac.json`, `d30-cedro-plano-local-v01.json`, `d30-cedro-baseline-antes.json` e `d30-cedro-baseline-fontes.zip`. Primeira tentativa do helper preservada por falha de expansão de argumento; análise v02 encerrada com exit0. Isso é análise estática, sem build final ou aceite integral.
