# D30 Cedro — contextos e execuções focais02

Compilação completa:258 main/37 test com exit0; fontes/classe/XML/log são preservados na mesma rodada focal02. Execuções distintas: F01 1/0/0/0; ApiHttp 4/0/0/0; Cadastros método focal de guarda 1/0/0/0. Não somar estes resultados nem chamá-los de build final/regressão integral.

ApiHttp: perfil test único, HTTP127.0.0.1/porta0, sem URL de datasource e zero DataSource/EntityManagerFactory, comprovados antes de singletons e no contexto antes dos casos. Cadastros: URL efetiva jdbc:h2:mem:wms-cadastros, org.h2.Driver, perfiltest, um DataSource/JPA, JWT com chave local e Flyway desabilitado. A guarda recusará perfil/URL/tipo/configuração diferente antes de criar singletons. Os outros dez H2 permanecem autorizados e classificados em fonte; prova efetiva ocorrerá na execução final guardada.

F01: metadado DocumentoSaida nullable/UNIQUE sem filtro; schema local gerado aceita duas chaves ausentes e recusa chave informada repetida com23505, mantendo três documentos/uma chave. Não inferir defeito operacional ou equivalência SQL Server. Java main e migrations intactos. Histórico D20 SHA38AD8D204C8D947E2DC76DCE2D26473D05EAF76A423A2EE9FFF9EF1E94038801 preservado. Zero ações SQL Server/PROD.

A06 importado para confronto individual dos marcos FIFO/chegada/início da cobrança e do positivo de embalagem válida sem estoque. As706 facetas locais e1373 componentes permanecem em revisão semântica, sem aceite por estrutura. Recibo detalhado: d30-cedro-focal02-contextos-recibo.json. Plano/importações: d30-cedro-plano-provas-v02.json.
