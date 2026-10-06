# V7 — alinhamento Cedro/Farol em 06/10/2026

Prumo. **Compatibilidade estrutural mantida, sem alteração SQL. JPA final aguarda freeze.** Doc29 lido integralmente conserva o hash da preparação anterior; os detalhes adicionais deste ask foram registrados como complemento confirmado de Cedro por Farol.

## Evidência observada

[Output datado](d19-v7-alinhamento-2026-10-06.json): comparador estático saída 0, 494/494 verificações atendidas, sem divergências. Permanecem 15 tabelas/172 colunas, 26 extensões, 36 FKs, 13 chaves únicas e 25 índices. Nenhum JPA/SQL/H2/build executado.

`servico_cobranca.situacao VARCHAR(24)` está formalizada no doc29 e na V7. `memoria_diaria` mantém UNIQUE(calculo_id,data); tabela_id/item_tabela_id/tarifa são NULL permitidos e segmentos_json é NVARCHAR(MAX) NOT NULL com ISJSON. A representação dos segmentos por regra/categoria e a escolha de NULL quando um campo não é único pertencem ao serviço. Nenhum CHECK/coluna/tabela adicional foi presumido.

[Manifesto SHA-256](d19-v7-alinhamento-2026-10-06.sha256): 13 arquivos com hashes iguais entre 2026-10-06T04:06:42.3444627Z e 2026-10-06T04:07:27.1541377Z. Inclui V1–V7, doc29, transcrição/leitor e os três outputs anteriores da preparação V7; todos preservados sem sobrescrita. V1–V6 também foram cotejadas com a baseline pelo leitor.

- V7: `6078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328`
- Doc29: `BC913E6C42D42A1605734287BE71A71C70BB053C842328C946434BA42F3EA573`

## Complementos recebidos neste ask

- Memória diária única por cálculo/data, segmentos/picos/valores por regra/categoria; tabela/item/tarifa singulares NULL quando não únicos.
- Item exato PALLET/BOBINA prevalece sobre geral; múltiplos serviços de armazenagem aplicáveis geram pendência.
- Serviço recebe a tarifa vigente no instante executado.
- GRIS DIARIA considera somente dias incluídos, sem fator adicional de duração; POR_CICLO usa a proporção configurada.
- Avarias históricas agregadas por intervalo, sem clamp; incompatibilidade vira pendência.

O doc29 lido ainda não explicita integralmente esses complementos na redação atual. Essa é a diferença documental observada; os campos/constraints existentes comportam os esclarecimentos sem DDL novo. README de migrations e doc33 registram a fonte como ask de Farol/Cedro, sem alterar contrato central nem aceitar regra comercial.

## Próximo passo e reporte

Ao freeze informado por Cedro/Farol, reconferir doc29 final, JPA e hashes com novo output datado; não sobrescrever esta leitura. O documento atual ainda declara implementação em andamento. Revisão das fórmulas, tarifa/contexto, agregados, pendências e memória pertence a Cedro/Vigia. Compatibilidade de arquivo não testa esse comportamento.

Reporte neste ask próprio/arquivo para WMS - Farol realizar check com seu ambiente. CLI maestri no PATH e MAESTRI_CLI próprio ausentes; list/mensagem não executados, sem identidade/env de terceiros ou raw.

Somente leitores/checks de arquivos e documentação/evidência da área foram usados. SQL Server (inclusive Info/Validate), migrations, H2, build, fiscal/cobrança real, publicação/git, rotinas/Hermes/ETL/frontend permanecem sem execução. Backend/BE08/contratos/centrais/Graphify e SQL V1–V7 não foram editados. Schema31/V8 seguem aguardando contrato exato.
