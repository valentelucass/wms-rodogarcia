# V6 — reconferência estrutural p2-final de 06/10/2026

Prumo. **V6 compatível com doc27 e models finais na leitura registrada, sem divergências estruturais.** Reconferência solicitada por Farol após correção P2 e freeze local. Doc27/backend README registram clean verify de Cedro com 240 testes, zero falhas/erros/ignorados, término às 00:35:36 -03:00. Prumo não executou essa suíte. Este parecer encerra a comparação estrutural desta rodada; aceite do bloco e revisão de comportamento continuam com Farol/Cedro/Vigia.

## Saídas observadas

- [Output p2-final](d19-v6-freeze-2026-10-06-p2-final.json): leitor estático com sintaxe PowerShell conferida e saída 0; oito tabelas/74 colunas, uma extensão da unidade, 14 FKs e oito chaves únicas. Nomes/tipos/tamanhos/precisão/escala/nulabilidade, IDENTITY, FKs/índices, enums e ações cumulativas compatíveis.
- Checks locais de texto/contrato: saída 0, 58/58 atendidos; suplemento P2: saída 0, 6/6 atendidos. A leitura manual do schema do doc27 complementa o comparador, que automatiza metadados JPA/SQL sem executar JPA ou constraints.
- [Manifesto SHA-256 p2-final](d19-v6-freeze-2026-10-06-p2-final.sha256): 32 arquivos com hashes antes/depois, iguais durante a rodada. Capturas UTC: 2026-10-06T03:41:22.8753187Z e 2026-10-06T03:42:13.5765941Z. Inclui V1–V6, doc27, models/enums/reader, backend README e fontes relacionadas ao P2, além das evidências protegidas.
- [Output inicial de 72 colunas](d19-v6-jpa-sql.json) e [parecer anterior de 74 colunas](d19-v6-freeze-2026-10-06.md), seu JSON e manifesto preservados sem sobrescrita. V1–V5 coincidem com a baseline. V6 SQL não precisou de alteração.

## Contrato e P2

`avaria_estoque.equivalencia_base decimal(19,6) NOT NULL >=0`, `ciclo_id varchar(36) NOT NULL` e `bloqueio_previo bit NOT NULL` coincidem com os models. Equivalência/ciclo são imutáveis no JPA; ciclo admite várias ocorrências, sem DEFAULT/FK/UNIQUE. O índice não único por unidade/id permanece. `unidade_logistica.avaria_inicial_reparada bit NOT NULL DEFAULT 0` mantém CHECK da condição original AVARIADA e lote GO.

Pares cumulativos conferidos: sete ações de expedição em PEDIDO_SAIDA/auditoria/operacao_saida; DEVOLUCAO_ENTRADA em PEDIDO_ENTRADA/auditoria; AVARIA_DETALHADA, RESPONSABILIDADE_AVARIA e REPARO_AVARIA em PEDIDO_ENTRADA/auditoria/movimento_estoque. Conservam-se 25 ações anteriores e oito tipos de auditoria; 11 ações novas; movimento conserva cinco e recebe somente três. Os 17 pares restritos preservam V5 e deixam os demais pares antigos com a aceitação anterior.

A ponte de reserva continua para pedido_saida; exclusividade ATIVA e chave histórica V5 permanecem. CHECKs de encerramento/JSON/reconhecimento/reparo mantêm nulos explícitos. Localização V4 recebe apenas SEPARACAO, com condições anteriores preservadas. Versão lida `-1` somente em LEITURA aceita snapshot anterior completo; o histórico por ciclo permanece nos resultados/auditoria, sem tabela extra. Equivalência da baixa é referência não somável por linha.

O doc27 final registra que P2 usa resultados imutáveis de operacao_unidade para divisão/reagrupamento BE07 e recusa passado/cadeia insuficientes. Os oito campos persistidos de OperacaoUnidade correspondem à V3, incluindo tipos, FK, chave histórica e resultado JSON; consulta/getters não exigem nova estrutura V6. A leitura estrutural não comprova o algoritmo temporal nem seu resultado financeiro. Essas regras foram cotejadas como contrato, sem novo aceite de negócio.

## Hashes finais

| Arquivo | SHA-256 |
| --- | --- |
| V6__separacao_retirada_retornos_e_avaria.sql | `EE0EEBAF5D7EA5B7329C182039A274EBE46DAFCB561EAEF5DF8846381A897D39` |
| V1__cadastros_e_auditoria.sql | `41346740B1D9FC60291908F82190722F9C004F5B04DBA6B68CE57E898B4E3D58` |
| V2__recebimento_e_entradas_conferidas.sql | `E17AD30B1D8BACF1409FE3083661DE09A2602563776CA747449E0F0764470052` |
| V3__unidades_logisticas_e_origens.sql | `B16CD6507AC256D4757385A9C98C984CFDF86918CDBBCBE2BE11DF40FCE6B6DD` |
| V4__enderecamento_capacidade_e_movimentos.sql | `CE6C77238777EC87DDDF05A72D925CCAFA1C784CA16883242238E45935221A05` |
| V5__pedidos_saida_fifo_e_reservas.sql | `57EA2766C4170159CB4CCD2C210481E63FA13D976BFD932D6E711A734DCFFF88` |
| doc27 | `D09F3CBDB8D2FF47D82A1994019804799A1017A1F742EDCF530BB88F5192E230` |

Doc27 mudou em relação ao parecer anterior para registrar P2; seu hash permaneceu igual entre as capturas desta rodada. V6 e models/enum relacionados já registrados no manifesto anterior mantêm os mesmos hashes. Hashes dos outputs antigos preservados constam no novo manifesto.

## Reporte e pendências

Reporte a WMS - Farol neste ask próprio e nestes arquivos para check com seu ambiente. CLI maestri não disponível no PATH e MAESTRI_CLI próprio ausente: list/mensagem não executados; nenhuma identidade/env de terceiros ou raw usado.

V7 aguarda schema29 exato de Cedro por Farol; nenhuma estrutura antecipada. Dialeto, locks, índices filtrados, constraints efetivamente aplicadas, pool, recuperação e permissões reais aguardam ensaio SQL Server externo autorizado. H2/build de Cedro não executam estes scripts nem substituem esse ensaio.

Nesta rodada Prumo executou somente leitores/checks locais e editou documentação/evidências de sua área. Nenhuma conexão SQL Server, Info/Validate, migration, H2, build, fiscal/cobrança real, publicação, commit/push ou rotina. V1–V6 SQL, backend/BE08/frontend, registros centrais e Graphify preservados.
