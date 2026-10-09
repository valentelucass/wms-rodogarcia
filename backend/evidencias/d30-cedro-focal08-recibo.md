D30 Cedro — focal08

Quatro casos novos passaram: 4/0/0/0, exit Maven 0, dois XMLs, fontes/classes/log preservados no freeze D66E0DA8F0DD6A4F0A13A7A450ABAB5AB1BF257D40CE17B9BAF22131D1CA8BD8. Não é a regressão final.

VOLUME de C2 foi confirmado pelo serviço real em três unidades de quantidade2, total6, com origem/cliente/SKU/nota/FIFO/chegada recuperados, replay original sem duplicação e PALLET anterior10 conservado. Repositories e serviços colaboradores são doubles; não prova JPA/transação ou etiqueta física.

Auditoria conserva sujeito, instante UTC, motivo, correlação, criação com antes nulo e snapshots JSON-string. Antes/depois recuperam origem/destino, versão e quantidade fornecidos, sem mudar após mutação dos mapas de entrada. Sem correlação externa, UUIDs válidos distintos. Não aceita por isso todos os callers ou rollback nativo.

Plano anterior: backend/evidencias/d30-cedro-focal08-plano-antes.json, SHA CCFAA458FA7C04CA1E4CCFD04AFB67A79F2E6A16EBA464B7CD6D66819605D8A2. Recibo JSON: 1E24136F98EC3B98BD46F2348E72FE6C01B7FBA768F0EA359FD0D7703D9DD342. D20 preservado 38AD8D204C8D947E2DC76DCE2D26473D05EAF76A423A2EE9FFF9EF1E94038801. Zero ações SQL Server; main e migrations sem alterações. A08 somente compilado, primeira execução na FINAL.

A09: 168 métodos declarados de repositories e 21 getters de projeção. São189 entradas preservadas, sem inferir consultas executadas ou quantidade real de consultas. A fila integral706/1373/161 continua individual; nenhuma pendência local foi transferida para SQL.
