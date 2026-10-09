# D30 Cedro — focal05

Execução separada: 3/0/0/0, exit Maven 0, duas classes. Compilação de todos os fontes antes do focal; nenhum caso green anterior repetido.

`D30-L-LOCAL-CT24-L029-02` / RN23.03: pedido de quantidade1, duas unidades de quantidade10/FIFO iguais. A ocupa conjunto A9(ID1/nível9)+A0(ID99/nível0); B ocupa B1(nível1). O serviço real escolhe somente A/ID11/quantidade1 em duas ordens reversas. Quantidades, marcos, conjunto, ocupações e pedido conservados; nenhum efeito em reserva/auditoria. Endereços reais, mesmo armazém/área/PALLET, limites físicos coerentes. Repositórios e EstoqueService mockados: não comprova materialização/filtros SQL, comando de capacidade, transação real ou empate final por ID de unidade. Não criar compartilhamento de endereço ocupado contrariando unicidade para fabricar essa última prova.

`D30-CEDRO-PAGINA-001`: ClienteController/ClienteService/PaginaResponse e mapper Boot reais, MockMvc sem DataSource/rede. GET sem parâmetros: página0/tamanho20/IDs1..20/total21/2páginas. Página2 vazia: total21/2páginas preservados. Somente duas leituras no repositório, zero auditoria e fotografia dos21 cadastros idêntica. Completa esses predicados de CT14-L032-02 e os cinco componentes do conversor; CT31-L280-01 e outros GETs permanecem sujeitos ao confronto próprio. Não comprova consultas SQL ou autenticação dessa rota.

Oráculos/snapshot fixados antes do código em d30-cedro-focal05-plano-antes.json. Freeze contém os bytes fonte da execução, classes, XML, log e recibo de exit. Focais03/04, D29 e histórico D20 preservados. Zero ações SQL Server; main/pom/migrations sem alteração. Sem build final ou aceite integral.
