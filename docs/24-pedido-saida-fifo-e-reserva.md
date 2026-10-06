# Pedido de saída, FIFO e reserva — BE01/BE09

Recorte local D18 de 05/10/2026. Implementação local entregue; formatação e clean verify aprovados, com 197 testes (153 anteriores e 44 novos). Fontes: respostas Q07–Q09/Q20 no documento 10, propostas AC10/AC11 no documento 11 e disponibilidade BE08 no documento 22. Não representa aceite comercial das propostas nem validação SQL Server.

## Schema para V5

Schema `wms`, IDs `bigint identity`, instantes `datetime2(6)`, quantidades `decimal(19,6)`. FKs preservam histórico, sem exclusão em cascata. Escrita da migration pertence a Prumo; Cedro mantém os mapeamentos.

| Tabela | Colunas e restrições |
| --- | --- |
| `pedido_saida` | `id` PK, `versao` bigint obrigatório, `cliente_id` e `armazem_id` FKs obrigatórias imutáveis, `referencia` varchar(40), `situacao` varchar(24), `criado_em` e `alterado_em`; UNIQUE(cliente_id, armazem_id, referencia); situação RASCUNHO/RESERVADO/CANCELADO |
| `item_pedido_saida` | `id` PK, `pedido_id`, `produto_id` FKs obrigatórias, `quantidade` positiva obrigatória; UNIQUE(pedido_id, produto_id) |
| `reserva_saida` | `id` PK, `pedido_id`, `item_id`, `unidade_id` FKs obrigatórias, `operacao_reserva_id` varchar(36), `quantidade` positiva, `situacao` varchar(16), `criada_em`, `encerrada_em` opcional; UNIQUE(pedido_id, operacao_reserva_id, unidade_id); situação ATIVA/CANCELADA/REVERTIDA; ATIVA exige encerrada_em nulo e encerrada exige instante preenchido |
| `operacao_saida` | `id` PK, `pedido_id` FK, `operacao_id` varchar(36), `conteudo_hash` varchar(64), `tipo` varchar(24), `usuario` nvarchar(200), `motivo` nvarchar(500), `registrada_em`, `resultado` nvarchar(max), todos obrigatórios; UNIQUE(pedido_id, operacao_id) |
| `unidade_logistica` | Acrescentar `reserva_saida_id` FK nullable para pedido_saida e índice; `avaria_posterior` bit NOT NULL DEFAULT 0. Ponte exclusiva do pallet atualmente reservado e sinalizador de avaria constatada após a entrada; preserva condição original, quantidade física, composição, etiqueta, posição e datas |

Reversão marca linhas históricas REVERTIDA e remove a ponte; nova reserva usa outro UUID e novas linhas. Cancelamento marca linhas CANCELADA. Não apagar linhas anteriores. Auditoria existente precisa admitir tipo `PEDIDO_SAIDA` e ações `CRIACAO_SAIDA`, `JUSTIFICATIVA_FIFO`, `RESERVA_SAIDA`, `CANCELAMENTO_SAIDA`, `REVERSAO_RESERVA`, além de `AVARIA_ESTOQUE` no tipo PEDIDO_ENTRADA (conferir restrições de V1–V4). A justificativa e seleção propostas ficam na confirmação imutável de operacao_saida, sem tabela adicional.

V5 preparada por Prumo também inclui índice UNIQUE filtrado por unidade para reservas ATIVA, CHECK de JSON no resultado, coerência entre avaria posterior/bloqueio e pares tipo/ação da auditoria. São proteções adicionais SQL Server; o H2 não executa nem comprova V5. Mapeamentos JPA de nomes/tipos/tamanhos, vínculos, enums e unicidades simples foram conferidos contra o arquivo.

## Operação e conservação

Pedido por proprietário/cliente e armazém, com um item por SKU. Quantidades imutáveis: cancelar e criar outro pedido para alterá-las. Reserva confirma todos os itens ou não grava nenhum efeito. Não vence com o tempo. Quantidade parcial de PALLET é permitida, sem atendimento parcial do pedido. BOBINA é indivisível neste recorte.

A proposta AC10 mantém o pallet inteiro indisponível para outros pedidos enquanto qualquer parte dele estiver reservada. `fisicoUnitizado = disponivel + reservado + bloqueado`; reservado soma somente quantidades ativas, bloqueado inclui o restante protegido do pallet e demais indisponibilidades. `fisicoTotal = pendenteUnitizacao + fisicoUnitizado`. Avaria/bloqueio posterior preserva reserva e físico, sinaliza impedimentos no pedido e recusa prosseguimento. A revalidação usa o predicado físico de BE08, desconsiderando somente a ponte pertencente ao próprio pedido. Reserva não autoriza separação fiscal ou retirada física BE10.

## FIFO e autorização

Ordenação proposta AC11: data FIFO original, nível mais baixo, sequência de coleta cadastrada, código do endereço e ID da unidade. Para unidade em duas posições, usar a primeira posição nessa ordem. Código é desempate estável, não distância física. Data FIFO, chegada real, início da armazenagem e identidade/etiqueta ficam preservados.

Qualquer perfil com alcance simultâneo por cliente e armazém pode criar pedido e justificar uma seleção fora do FIFO. Supervisor/Gestor autorizam a seleção excepcional ao confirmar a reserva, com motivo; não exige segunda pessoa. Seleção manual é comparada ao FIFO recalculado dentro da transação. Unidades indisponíveis não podem ser liberadas por exceção FIFO.

## Concorrência e repetição

Escrita bloqueia cliente, armazém, pedido de saída e produtos em ordem de ID. Não adquire pedido de entrada depois do armazém; BE08 conserva seus locks anteriores. O lock compartilhado do armazém serializa reserva e movimento/bloqueio físico. Reconsultar unidades e disponibilidade após os locks evita snapshot anterior. Linhas, ponte exclusiva, estado, auditoria e resposta idempotente ficam na mesma transação.

UUID da operação é único por pedido; criação é identificada por referência única e UUID. Hash inclui ação e DTO completo, incluindo versão e seleção; o pedido delimita a unicidade, e o DTO de criação inclui cliente/armazém. Mesma chave/conteúdo retorna a confirmação original; payload divergente retorna 409. Acesso e perfil atuais são conferidos também na repetição. GET informa o estado atual.

## Limites

Sem SQL Server real, migration aplicada, fiscal/cobrança, frontend, impressão, publicação, commit/push, rotinas ou perfis Hermes. H2 verifica transações locais; não comprova dialeto, isolamento ou desempenho SQL Server. A condição original continua preservada; ocorrência posterior usa sinalizador próprio e bloqueio motivado, impedindo prosseguimento e liberação silenciosa, sem inventar o fluxo de reparo/ajuste BE11.

## API para FE09 e complemento FE08

Todas as rotas exigem JWT e alcance por cliente e armazém. Gestor tem alcance geral. Quantidade usa a unidade/precisão do SKU, até decimal(19,6). Pedido admite até 100 SKUs distintos; seleção justificada, até 500 unidades distintas. Página começa em zero, tamanho 1–100 e ordem por ID. Erros seguem Problem Details com codigo e idOperacao.

| Método/rota | Corpo e resultado |
| --- | --- |
| POST `/api/v1/pedidos-saida` | operacaoId, clienteId, armazemId, referencia, itens[{produtoId,quantidade}], motivo; HTTP 201 com confirmação original, inclusive na repetição |
| GET `/api/v1/pedidos-saida?clienteId=...&armazemId=...` | Lista paginada com detalhes; pagina e tamanho opcionais |
| GET `/api/v1/pedidos-saida/{id}` | Estado atual, revisão, itens, histórico de reservas, unidadesImpedidas e podeProsseguir |
| GET `/api/v1/pedidos-saida/{id}/fifo` | Revisão e selecoes[{unidadeId,quantidade}] recalculadas; insuficiência retorna 409 |
| POST `/api/v1/pedidos-saida/{id}/justificativas-fifo` | Qualquer perfil: operacaoId, versao, selecoes integrais, motivo; incrementa revisão e guarda seleção/motivo/autor, sem reservar |
| POST `/api/v1/pedidos-saida/{id}/reserva` | operacaoId, versao, motivo; FIFO automático. Com justificativaId (UUID da proposta), somente Supervisor/Gestor confirmam a seleção justificada |
| POST `/api/v1/pedidos-saida/{id}/cancelamento` | Supervisor/Gestor: operacaoId, versao, motivo; cancelamento integral do rascunho/reserva deste recorte |
| POST `/api/v1/pedidos-saida/{id}/reversao-reserva` | Supervisor/Gestor: mesmo corpo; libera integralmente e retorna a RASCUNHO; reserva futura exige novo UUID |
| POST `/api/v1/pedidos-saida/{id}/revalidacao` | Sem corpo; verifica reserva integral e elegibilidade sob lock, retorna detalhe ou 409 RESERVA_IMPEDIDA; não separa nem baixa estoque |
| POST `/api/v1/unidades-logisticas/{codigo}/avaria` | Qualquer perfil com alcance: corpo BE08 operacaoId, versaoUnidade, motivo; marca avaria posterior e bloqueio juntos, auditados/idempotentes |

A confirmação tem operacaoId, pedido, selecoes, excecaoFifo, justificadaPor e justificativa. A auditoria da autorização identifica quem confirmou; a proposta preserva quem justificou e seu motivo. Supervisor/Gestor pode cumprir ambos os passos. Proposta vale apenas na revisão resultante; outra justificativa ou reversão exige nova proposta. Indisponibilidade surgida entre proposta e confirmação é revalidada e não pode ser ignorada por perfil superior.

Cada reserva expõe item, unidade/código, nota de origem, lote, data FIFO, quantidade, situação e instantes de criação/encerramento. Sem expiração, nota de saída, cobrança ou retirada. PodeProsseguir verifica elegibilidade neste recorte, sem liberação fiscal ou confirmação BE10. Respostas idempotentes antigas não substituem GET/revalidação atual.

AvariaPosterior também aparece no detalhe de estoque. Liberação de bloqueio não repara avaria; reparo/ajuste ficará em BE11. Liberação expressa de bloqueio preventivo revalida condição física e mantém a ponte/reserva ativa. O indicador avariado do saldo abrange condição original AVARIADA ou avaria posterior, como grupo sobreposto ao físico.

## Complementos da revisão Vigia

Q07 exige saldo também na criação: todos os itens precisam caber na soma disponível, sob os mesmos locks de cliente/armazém. Criar não reserva e não garante disponibilidade futura; confirmar revalida todos os itens. Para bobinas, saldo agregado não garante composição indivisível possível. O FIFO automático percorre a ordem e usa somente bobinas inteiras que cabem no restante; se não fechar, retorna 409 sem reservar o prefixo parcial. Uma seleção manual integral pode ser justificada e autorizada mesmo nesse caso. Exemplo: bobinas de 4 e 6, pedido de 6 — o FIFO guloso não fecha; a bobina de 6 pode ser selecionada por exceção. Pedido de 10 fecha automaticamente em 4 + 6. Bobina de 6 para pedido de 4 não pode ser fracionada.

Encerramento/reativação de endereço projeta somente o ID do armazém e o bloqueia antes de carregar/bloquear o endereço. Isso impede encerramento entre a consulta de candidatas e o commit da reserva. Se o endereço for encerrado depois, o pedido sinaliza impedimento e a revalidação recusa prosseguimento, mantendo reserva e ocupação.

## Validação local executada em 05/10/2026

Com JAVA_HOME apontando para `C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21` (Java 21.0.12.1), na pasta backend:

| Comando | Saída preservada fora de target | Resultado |
| --- | --- | --- |
| `./mvnw.cmd -B -ntp spotless:apply` | [d18-spotless-final.log](../backend/evidencias/d18-spotless-final.log) | BUILD SUCCESS, 129 arquivos Java limpos |
| `./mvnw.cmd -B -ntp -Dtest=PedidoSaidaIntegrationTest test` | [d18-testes-saida-ajustes.log](../backend/evidencias/d18-testes-saida-ajustes.log) | 44 testes, 0 falhas/erros/ignorados; 59.917 s |
| `./mvnw.cmd -B -ntp clean verify` | [d18-clean-verify.log](../backend/evidencias/d18-clean-verify.log) | BUILD SUCCESS; 197 testes, 0 falhas/erros/ignorados; 02:01 min, JAR gerado |

A bateria inicial de 44 testes falhou nas comparações de escala decimal e na preparação do spy transacional do teste determinístico. Corrigidos: DTOs apresentam quantidades com escala 6 consistente entre confirmação e consulta; o teste instrumenta o alvo real do serviço, mantendo o proxy/transação em execução. A execução inicial permanece no log d18-testes-saida-inicial.log; os resultados aprovados acima a substituem como evidência de validação atual.

Cobertura nova em PedidoSaidaIntegrationTest:

- Criação respeita Q07 por SKU; quantidades inválidas/duplicadas e item sem saldo revertem o pedido inteiro. Três perfis reservam FIFO com alcance atual.
- Transações HTTP concorrentes disputam saldo limitado e a mesma unidade/pedido, com duas chaves e com repetição da mesma chave; apenas uma reserva é gravada.
- Pedido multiitem não confirma parcialmente; falha real de CHECK na auditoria após gravar duas linhas e pontes reverte estado, versões, quantidades, histórico e auditoria. Falha no registro de idempotência também reverte todos os efeitos.
- FIFO preserva data original e desempata por nível, sequência e código; sugestão repetida é estável. Bobinas 4/6 atendem 10 automaticamente; pedido 6 exige a seleção excepcional integral da bobina 6, justificada por qualquer perfil e autorizada por Supervisor/Gestor. Bobina não é fracionada.
- Proposta antiga, seleção parcial/duplicada, motivo ausente, unidade indisponível e alcance incorreto são recusados. Validação/permissão também são exercitadas em chamadas diretas ao serviço.
- Triagem, quarentena, avaria, bloqueio e cadastros/endereço encerrados não entram na disponibilidade. Avaria/bloqueio posterior sinalizam pedido afetado, mantêm reserva/físico e impedem revalidação; liberar bloqueio preventivo conserva a reserva, liberar avaria é recusado.
- Teste determinístico pausa a reserva depois de ler candidatas e tenta encerrar o endereço em outra transação: encerramento aguarda commit, depois sinaliza o pedido como impedido sem liberar reserva. Reserva/avaria concorrentes também preservam saldo e revisão.
- Cancelamento/reversão são integrais, auditados/idempotentes, conservam físico e liberam apenas o que ainda é elegível. Nova reserva após reversão cria novas linhas e preserva as anteriores. Tempo decorrido não expira reserva. Etiqueta, composição, datas e nota de origem são preservadas.

Os testes usam H2 isolado, HTTP local e JWTs com chave efêmera. Os spies apenas controlam as barreiras do teste; repositories, locks, transações e commits/rollbacks são reais nesse banco. Não executar SQL Server, aplicar V5 ou inferir validação de isolamento/dialeto SQL Server a partir desses resultados. Nenhuma aplicação operacional foi iniciada. Farol mantém estados/decisões/continuidade/índice/evidência central e atualiza Graphify; revisão de Vigia permanece registrada por Farol.
