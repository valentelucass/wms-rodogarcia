# V7 — revisão documental financeira de 06/10/2026

Prumo. **Doc29 atualizado reconciliado; nenhuma mudança DDL. JPA final aguarda freeze.** Colunas, tipos, nulabilidade, chaves e pares de auditoria permanecem iguais à leitura anterior; a redação de memória foi esclarecida. Testes de Cedro ainda em correção, conforme Farol; nenhum aceite BE05/BE12 declarado.

## Resultado em arquivos

[Output datado](d19-v7-revisao-financeira-2026-10-06.json): leitor estático saída 0, 494/494 verificações atendidas, sem divergências estruturais. V7 permanece com 15 tabelas/172 colunas, 26 extensões, 36 FKs, 13 chaves únicas e 25 índices explícitos. Nenhum SQL/JPA/H2/build executado. Conferência documental local: 18/18 verificações, 42 links existentes e 24 hashes do manifesto conferidos (UTC 2026-10-06T04:38:30.1617094Z).

[Transcrição atual](../contratos/v7-schema-doc29.json) teve somente referência/hash e regras de serviço/documentais atualizados após releitura integral. [Cópia anterior](d19-v7-schema-doc29-2026-10-06-antes-financeiros.json) preservada byte a byte, SHA-256 `6B3629DD8E779B7B8D88B03735C4CCF815C93AD58C1BD5DEE946ADF92105EB92`. Cinco partes comparadas com a cópia: extensões/tabelas/índices/auditoria/baseline idênticas. SQL e leitor não mudaram.

[Manifesto final](d19-v7-revisao-financeira-2026-10-06.sha256) e output registram 23 fontes capturadas antes/depois (2026-10-06T04:30:42.1966695Z → 2026-10-06T04:37:31.7087987Z): 21 iguais, inclusive V1–V7 e os 13 outputs antigos; duas diferenças: metadados locais da transcrição e doc29 atualizado externamente durante a conferência. Prumo releu a fonte e registrou seu hash atual, sem editar doc29. Manifesto inclui também a nova cópia anterior. Não sobrescreve os outputs de preparação/alinhamento V7, 72/74 colunas V6 ou p2-final.

## Complementos agora explícitos

- Intervalos físicos de valor são separados da diária; intervalosValor e detalhes de origens/quantidades/valores ficam no JSON existente. Mercadoria presente antes da retirada participa do valor físico no mesmo dia, ainda que a diária seja zero.
- Dano sem origem identificada e preços unitários diferentes produz pendência, sem desconto uniforme. Preços comprovadamente iguais permitem agregado exato com memória das origens.
- Marcos seguem ordem física inclusive inserção fora de ordem, sem aumento indevido da quantidade afetada ou redução além da retirada possível. Avarias reparadas/retroativas são reconciliadas por unidade/intervalo, com pendência quando incompatíveis/acima da base, sem clamp.
- Chave do adicional deriva serviço/contexto/pedido de entrada/referência estável; unidade/saída opcionais são vínculos e não nova execução.
- Sugestões SAIDA por quantidade são individuais por retirada/SKU ainda não confirmado.

Memória continua uma por cálculo/data, com segmentos/picos/valores por regra/categoria. Tabela/item/tarifa singulares NULL quando não únicos; PALLET/BOBINA exato prevalece sobre geral explícito; múltiplos serviços aplicáveis geram pendência. GRIS DIARIA usa somente dias incluídos, sem segunda proporção de duração; POR_CICLO usa proporção configurada. Tarifa do serviço é da data civil do executado, independentemente do registro.

A atualização recebida durante a conferência detalha `diarias[].regras[]`, `segmentos[]`, `intervalosValor[]`, `servicos[]` e `ajustes` no JSON existente, com campos singulares ausentes/NULL quando não únicos. Limites de importação e rotas também foram explicitados, sem nova coluna/tabela/CHECK presumido. O leitor não testa o cálculo, os marcos, as sugestões ou o formato efetivamente gravado. Cedro/Vigia devem conferir comportamento e JSON real no freeze.

## Hashes e diferenças

- V7 SQL preservada: `6078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328`
- Doc29 atual: `753ACAC640FF18F5A1DA4DF47CB4BD47A71061F081F48E5FAC96365D6A302511`; anterior `BC913E6C42D42A1605734287BE71A71C70BB053C842328C946434BA42F3EA573`.
- Transcrição atual: `A37CB92B59BAA331064722376D65A7315FCAF81AEB76305EE31523397624BEEA`.
- Leitor preservado: `7F3BE06A9605BDF46DAD15B9B2EB4F15AE88815042EEB2BDBBBC1817C3EC9891`.

A diferença documental apontada no parecer anterior foi resolvida no contrato atualizado. Mudaram doc29 entre rodadas e metadados locais nesta rodada; estrutura SQL/baseline não mudou.

## Próximo passo e reporte

Aguardar freeze final por Farol/Cedro para doc29/JPA/hash e nova evidência datada, sem sobrescrever esta entrega. Testes ainda em correção; não antecipar aceite de regras de negócio ou do bloco. Schema31/V8 continuam dependentes do contrato exato.

README de migrations e doc33 atualizados somente na área de Prumo. Reporte neste ask/arquivo para WMS - Farol realizar check com seu ambiente. CLI maestri no PATH e MAESTRI_CLI próprio ausentes; list/mensagem não executados, sem identidade emprestada ou raw.

Nenhuma conexão SQL Server (inclusive Info/Validate), migration, JPA/H2/build, fiscal/cobrança real, publicação/git/rotina/Hermes/ETL/frontend. V1–V7 SQL, backend/BE08/contratos/centrais/Graphify e outputs anteriores preservados.
