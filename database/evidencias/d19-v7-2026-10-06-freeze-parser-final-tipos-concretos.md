# V7 — tipos administrativos, evidência intermediária de 06/10/2026

Prumo. **CHECK autorizado corrigido em arquivos; 518/518 verificações estáticas e 67/67 suplementares.** Anotações de 198 colunas conferidas. P2 temporal financeiro BE08 continua com Cedro: reconferência após novo freeze pendente, sem aceite BE05/BE12 ou homologação SQL Server.

## Falha e correção preservadas

O [registro anterior](./d19-v7-2026-10-06-freeze-parser-final-antes-correcao.md) distingue as limitações iniciais do parser de uma divergência real: Java salvava CRIACAO_SERVICO/CRIACAO_TABELA, mas o CHECK V7 aceitava CRIACAO. [Suplementar original65/67](./d19-v7-2026-10-06-freeze-parser-final-suplementar.json), [leitor ampliado516/518](./d19-v7-2026-10-06-freeze-parser-final-cobertura-antes-correcao.json) e todas as evidências anteriores permanecem sem sobrescrita. Os494 checks anteriores não conferiam essa fronteira.

Após Farol autorizar e Cedro formalizar [doc29](../../docs/29-cadastros-servicos-e-calculo.md), a V7 recebeu somente dois literais no CHECK. A [cópia anterior](./d19-v7-2026-10-06-freeze-parser-final-V7-antes-tipos-concretos.sql) mantém SHA2566078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328. V1–V6 e demais linhas SQL foram preservadas; sem coluna, tabela, índice, FK, DML, DEFAULT ou ação de auditoria nova.

## Resultado em leitura

[Output principal](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.json): saída0, 518/518 (494 contrato/SQL +24 cobertura administrativa), zero divergências. Quinze tabelas/172 colunas +26 extensões, 36FKs,13unicidades,75CHECKs e25índices. JPA é leitura de17 models e ComplementoFiscal embutido, sem iniciar Hibernate; Long anulável é distinguido de long. VARCHAR24 de situação do serviço e todos os tamanhos/precisões/nulos/IDENTITY/FKs/chaves conferidos.

[Suplementar corrigido](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-suplementar.json): saída0,67/67. Dezessete pares literais de auditoria compatíveis; 11 domínios DTO e pontos de JSON conferidos. Auditoria cumulativa permanece14 tipos/47ações e mantém CHECK V6. Financeiro/administrativo não ganha movimento físico por essa correção. A leitura de presença de expressões não executa nem prova fluxo/concorrência.

OperacaoAdministrativaService recebe16 chamadas literais de seis services, com13 tipos efetivos: ANULACAO_SERVICO, CALCULO, COMPLEMENTO_FISCAL, CONFIGURACAO, CRIACAO_SERVICO, CRIACAO_TABELA, ENCERRAMENTO_VIGENCIA, IMPORTACAO_ENDERECOS, MARCO_AVARIA, PREVIA_ENDERECOS, REFERENCIA_FISCAL, REGISTRO_SERVICO, VINCULO_TABELA. O domínio permite14 preservando CRIACAO anterior conforme pedido; os dois concretos só pertencem ao campo tipo administrativo. Auditoria continua SERVICO_COBRANCA/CRIACAO e TABELA_COBRANCA/CRIACAO. O leitor principal descobre os chamadores pela injeção do gateway, verifica cobertura de todas as chamadas, encaminhamento sem transformação e o CHECK SQL efetivo.

Memória diária continua UNIQUE(calculo_id,data). Regra/categoria, segmentos cobráveis, intervalosValor físicos, origens/valores/parcelas e ajustes permanecem no JSON existente. Item/tarifa singulares NULL quando não únicos; nenhuma estrutura foi presumida para GRIS/avarias ou P2. Cálculo e temporalidade permanecem responsabilidade Cedro/Vigia, sem aceite comercial por Prumo.

## Procedimento, permissões e preservação

[Procedimento](../migrations/README.md) mantém guarda de alvo, identidades separadas, histórico, autorização de ensaio e recuperação sem repair/baseline/clean automático. [Permissões](../permissoes-minimas.md) cotejadas com @Version/updatable dos15 models: nenhum GRANT ou SQL emitido observado. OperacaoAdministrativa.recurso_id permite UPDATE por padrão na anotação, sem setter/comando atual; manter SELECT/INSERT somente e reportar o marcador para revisão, sem presumir alteração histórica executada.

[Metadados](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-metadados.json), [hashes antes](./d19-v7-2026-10-06-freeze-parser-final-antes.json), [hashes depois](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-depois.json) e [manifesto separado](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.sha256) delimitam as capturas. Na inicial,222/222 arquivos declarados por Cedro coincidiam. O doc29 formal recebe hash separado; qualquer mudança posterior de Cedro/Farol é identificada como fonte externa, sem atribuir escrita a Prumo. O freeze286 é evidência histórica, sem inferir estabilidade após P2.

Conferência local final em 2026-10-06T05:10:05.1020162Z: [23/23 checks documentais](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-checks-locais.json), 74 links existentes, AST dos dois leitores sem erro, hashes de V1–V6 e da cópia V7 antiga conferidos. Comparação textual removeu somente os dois literais da V7 nova e recuperou exatamente a anterior. Os20 outputs antigos não mudaram. A captura posterior reúne288 hashes; dos272 iniciais,260 ficaram iguais e12 mudaram: sete arquivos autorizados de Prumo e cinco fontes externas (doc29, states e três arquivos de Cedro no P2). Os models e ComplementoFiscal comparados continuaram iguais. O manifesto original de222 passou a ter quatro diferenças históricas esperadas (doc29 formal e três arquivos P2); não foi reescrito. [Manifesto dos documentos/fontes estruturais finais](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-documentos.sha256) protege a entrega atual separadamente das fontes de código em correção.

## Hashes da comparação

| Arquivo | SHA256 |
| --- | --- |
| V7 antes, cópia preservada | 6078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328 |
| V7 atual, só CHECK ajustado | 3193D8436D360BA0D1D36E290112F39F76AB3A300AC2C2AD931208B6745B8940 |
| Doc29 formal | E5144BAE18D0211B44D721868FA06F0F4B27F6D94AC0BBE6E66E75822B25A029 |
| Transcrição atual | 80C8F62176E86C61860C5F4CB7F2D4935E44E0AE1C77DA1C675EAEDC9EF60FD1 |
| Leitor fortalecido | 752A2876DBDA2E9F64A28CF47137522DC1513D053CE507FEB966F4EEE2DBA535 |

## Continuidade e limites

Reconferir contrato/JPA/hashes após correção temporalP2 BE08, revisão de Vigia e novo freeze por Farol/Cedro. Schema31/V8 fica somente leitura/planejamento até retomada; BE14 não tem schema para presumir. Nenhuma connection SQL Server inclusive Info/Validate, execução migration/JVM/H2/build, fiscal/NFS-e/cobrança real, publicação/git/rotina/Hermes/ETL/frontend.

Reporte extenso neste artefato/ask próprio para WMS - Farol realizar check. CLI Maestri próprio/PATH ausentes, sem list/mensagem, identidade/env emprestados ou raw. Prumo editou somente database e doc33; registros centrais/contratos/backend/Graphify ficam com seus donos.
