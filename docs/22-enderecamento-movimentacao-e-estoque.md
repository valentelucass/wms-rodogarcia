# Endereçamento, capacidade, movimentação e estoque

Entrega D17 de 05/10/2026: BE08, ampliação de BE01/BE03 e consultas iniciais de BE14 para FE07/FE08. Mantém Spring MVC e as camadas convencionais. O [states.md](../states.md) registra o andamento e o [documento 23](23-validacao-estoque-backend.md) registra as verificações. Não há frontend entregue nem aplicação no SQL Server real.

## Fontes e limites

RN18 a RN21 e as respostas Q06/Q14 orientam endereço, capacidade e rastreabilidade. A possibilidade de uma unidade ocupar duas posições é a **interpretação proposta de AC05**; o início e a equivalência de armazenagem seguem a **proposta AC04**, sem cálculo ou lançamento de cobrança nesta entrega. As regras de negócio permanecem no [documento 11](11-alinhamentos-apos-respostas.md). Valores físicos reais devem ser configurados pela operação; os exemplos abaixo são fictícios.

O bloco permite confirmar a localização física, remanejar uma unidade inteira, bloquear/liberar e consultar saldo. Reserva, FIFO de seleção, saída e retirada parcial de pallet pertencem a BE09/BE10. Novas avarias, reclassificação de condição, correções de medidas já confirmadas e ajustes de estoque precisam de fluxo rastreável próprio em BE11. Uma operação física já confirmada não é apagada: o remanejamento corretivo registra novo destino, responsável e motivo.

## Camadas e persistência

| Responsabilidade | Implementação |
| --- | --- |
| Contratos HTTP | `CapacidadeController`, `EstoqueController`, DTOs específicos com Bean Validation |
| Capacidade e conjuntos | `CapacidadeService`, `CapacidadeSupport` |
| Confirmação física e bloqueios | `MovimentacaoEstoqueService` |
| Saldo, disponibilidade e histórico | `EstoqueService`, consultas em repositories |
| Dados | `MedidasUnidade`, `ConjuntoPosicoes`, `OcupacaoEndereco`, `MovimentoEstoque`; extensão de `Endereco` e `UnidadeLogistica` |

`ocupacao_endereco` mantém uma linha por endereço; `unidade_id` nulo significa posição livre. Remanejamento libera o vínculo anterior e ocupa os destinos na mesma transação, sem DELETE. `movimento_estoque` guarda a confirmação original, o antes/depois e a chave de repetição. A composição da unidade, nota, SKU, lote, validade, chegada e FIFO permanecem preservados.

V4 acrescenta os campos e tabelas, índices, FKs e restrições; amplia auditoria. V1/V2/V3 ficam preservadas. Unidades antigas continuam sem localização até confirmação física. A migração inicializa `revisao_conteudo` pela versão antiga para preservar a revisão da etiqueta existente. A aplicação nunca executa migrations automaticamente; procedimento e permissões em [database/migrations](../database/migrations/README.md).

## Capacidade e duas posições

Gestor configura peso em kg, altura/largura/profundidade em metros, empilhamento máximo e tipo de unidade permitido (PALLET, BOBINA ou VOLUME). Endereço antigo sem perfil completo não pode receber unidade. Capacidade só muda em endereço ativo, livre e sem conjunto ativo que use sua configuração. Revisão e motivo são obrigatórios; antes/depois ficam auditados.

Conjunto identifica **exatamente duas posições**, do mesmo armazém, área e tipo permitido. A compatibilidade física deve ser conferida no cadastro operacional. Limites efetivos são explícitos: peso e largura não ultrapassam a soma das posições; altura, profundidade e empilhamento não ultrapassam o menor limite. Essa validação é um teto técnico; não calcula apoio, resistência ou distribuição de carga. Os limites reais precisam ser aferidos, sem assumir que duas posições vizinhas sempre comportam uma carga.

O conjunto não pode ser encerrado enquanto uma unidade o ocupa. Encerramento preserva cadastro/histórico em `ENCERRAMENTO_PENDENTE` e impede novo uso. Conjuntos podem compartilhar uma posição; a unicidade da ocupação impede usá-los simultaneamente em conflito. Alterar composição/limites de um conjunto exige encerrar o anterior e cadastrar outro código.

No primeiro endereçamento, informar medidas físicas da unidade e uma ou duas posições necessárias. O servidor confere todas as dimensões, peso, empilhamento e tipo. Não converte quantidade de produto em peso nem presume rotação da carga. As medidas ficam preservadas nos remanejamentos; o operador não pode diminuí-las no pedido seguinte para contornar limites.

Exemplo fictício: posições A/B têm cada uma limite de 1.000 kg e dimensões de 2 × 2 × 2 m. O conjunto pode ter limite operacional aferido de 1.500 kg e largura útil de 4 m. Unidade de 500 kg, 1 × 3 × 1 m, com duas posições necessárias, exige A+B e o conjunto cadastrado. Se B estiver ocupada, A continua livre: não existe confirmação parcial. Mover A+B para B+C ocupa B+C e libera somente A, em uma transação.

## Condição, localização e disponibilidade

| Situação | Comportamento |
| --- | --- |
| Sem endereço | Mantém estoque físico; indisponível |
| TRIAGEM | Mantém estoque físico; indisponível; não inicia armazenagem |
| QUARENTENA | Mantém estoque físico; indisponível; estabelece bloqueio operacional persistente |
| BOA em ARMAZENAGEM | Pode ficar disponível se atender a todos os critérios abaixo |
| AVARIADA | Só pode ser endereçada/remanejada para QUARENTENA; liberação não muda a condição |
| SEPARACAO | Rejeitada neste comando; depende de pedido/reserva pelo fluxo de saída |

Sair da quarentena não libera automaticamente a unidade. Supervisor/Gestor precisam confirmar liberação motivada, com revisão atual, mercadoria BOA, armazenagem e localização válida. Operação pode registrar bloqueio preventivo. Bloqueio não muda a condição BOA para AVARIADA nem altera saldo, origem, etiqueta ou futura reserva.

A disponibilidade é definida no backend por um único predicado compartilhado: unidade ativa, BOA, sem bloqueio, em ARMAZENAGEM, pedido EFETIVADO, cliente/armazém/produto ativos, todas as posições necessárias ocupadas por ela em armazenagem ativa no mesmo armazém e conjunto ativo quando duplo. Endereço/cadastro em encerramento retira disponibilidade e preserva a ocupação e o histórico. O remanejamento revalida os vínculos e permite sair de endereço encerrado para destino ativo; se cliente/armazém/produto estiver em encerramento, novos movimentos ficam recusados, mas o bloqueio preventivo continua permitido.

Divisão/reagrupamento de BE07 ficam restritos a unidades ainda não endereçadas e sem bloqueio. Isso evita duplicar ocupação ou reiniciar datas por uma transformação que ainda não coordena posições. Repetição válida de um comando anterior devolve a confirmação original; não executa nova transformação. O futuro fluxo de retirada parcial será coordenado em BE09/BE10.

## Datas e equivalência

- `primeiroEnderecamentoEm`: primeiro endereço físico confirmado, inclusive triagem/quarentena.
- `inicioArmazenagemEm`: primeira entrada em ARMAZENAGEM. É o marco preparado para AC04; não é data FIFO nem chegada real.
- `posicoesEquivalentes`: zero antes da armazenagem; uma ou duas ao iniciá-la, conforme a unidade. Preservada no remanejamento e no bloqueio.
- `revisaoConteudo`: muda quando o conteúdo é dividido/reagrupado. A versão concorrente da unidade também muda com posição/bloqueio; a revisão da etiqueta não muda nessas operações.

Exemplo: unidade recebida em 01/09, posicionada em triagem em 02/09 e armazenada em 03/09 conserva FIFO de 01/09, primeiro endereço de 02/09 e início de armazenagem de 03/09. Remanejamento em 04/09 não reinicia nenhum marco. Posições físicas, equivalência cobrável e quantidade de produto continuam grandezas diferentes. Não há preços, diárias calculadas, rateio de avaria, fechamento ou cobrança nesta etapa.

## API, autorização e repetição

Todas as rotas exigem Bearer/JWT. Consultas de estoque, histórico e comandos físicos verificam cliente **e** armazém; Gestor mantém acesso geral. Configuração de capacidade/conjunto exige Gestor; liberação exige Supervisor/Gestor. Página começa em zero, tamanho de 1 a 100, ordenação estável por ID.

| Método e rota | Finalidade |
| --- | --- |
| PUT `/api/v1/enderecos/{id}/capacidade` | Configurar perfil físico com revisão/motivo |
| POST `/api/v1/conjuntos-posicoes` | Cadastrar conjunto compatível; HTTP 201 |
| GET `/api/v1/conjuntos-posicoes?armazemId=...` | Consultar conjuntos paginados |
| POST `/api/v1/conjuntos-posicoes/{id}/encerramento` | Retirar conjunto de uso, preservando histórico |
| POST `/api/v1/unidades-logisticas/{codigo}/movimentos` | Primeiro endereço ou remanejamento; UUID da etiqueta na rota |
| POST `/api/v1/unidades-logisticas/{codigo}/bloqueio` | Bloqueio operacional motivado |
| POST `/api/v1/unidades-logisticas/{codigo}/liberacao` | Liberação expressa, revalidada no servidor |
| GET `/api/v1/unidades-logisticas/{codigo}/estoque` | Unidade, medidas, posições e disponibilidade atuais |
| GET `/api/v1/unidades-logisticas/{codigo}/movimentos` | Histórico paginado com confirmação original |
| GET `/api/v1/estoque?clienteId=...&armazemId=...` | Unidades ativas; filtros opcionais `produtoId` e `disponivel` |
| GET `/api/v1/estoque/saldo?clienteId=...&armazemId=...&produtoId=...` | Saldo na unidade de medida do produto |

Configuração de uma posição, com valores fictícios:

```json
{
  "versao": 0,
  "tipoUnidadePermitido": "PALLET",
  "limites": {"pesoKg": 1000, "alturaMetros": 2, "larguraMetros": 2, "profundidadeMetros": 2, "empilhamentoMaximo": 2},
  "motivo": "Capacidade física conferida"
}
```

Primeiro endereçamento, com IDs ilustrativos:

```json
{
  "operacaoId": "0efdb66d-245a-45b9-8238-dfcf4bc2d707",
  "versaoUnidade": 0,
  "medidas": {"pesoKg": 500, "alturaMetros": 1, "larguraMetros": 1, "profundidadeMetros": 1, "empilhamento": 1, "posicoesNecessarias": 1},
  "destinos": [{"enderecoId": 12, "codigoLido": "A-01"}],
  "motivo": "Unidade e posição conferidas no local"
}
```

Remanejamento omite `medidas`, conserva a necessidade de posições e envia revisão atual. Duas posições exigem `conjuntoId` e a leitura de ambos os endereços. Bloqueio/liberação usam `operacaoId`, `versaoUnidade` e `motivo`. A confirmação retorna a chave, ID/revisão do pedido e o estoque resultante.

`operacaoId` é UUID único por pedido dentro dos comandos de estoque, em registro separado das operações de unitização. O hash inclui ação, código da unidade na rota e DTO completo, inclusive revisão e ordem dos destinos. Mesma chave/conteúdo recupera a resposta original mesmo depois de outro movimento; chave reutilizada com outro conteúdo retorna 409. A permissão atual é conferida inclusive na repetição. Consultar GET após recuperar confirmação antiga para mostrar o estado atual.

Erros seguem Problem Details: 400 para dados/leitura/conjunto incompatível, 401 sem autenticação, 403 sem alcance/perfil, 404 para recurso ausente e 409 para revisão antiga, posição ocupada, capacidade excedida, bloqueio ou conflito concorrente. Campo `codigo` distingue o motivo; FE07 deve preservar a chave original quando repetir a mesma confirmação e consultar o servidor antes de propor uma nova operação.

## Saldo e consistência

`fisicoTotal = pendenteUnitizacao + fisicoUnitizado`. A primeira parcela soma entradas ainda não unitizadas; a segunda soma apenas unidades ativas. Nunca somar todas as entradas às unidades correspondentes. `disponivel` segue o predicado acima; `reservado` permanece zero até BE09; `bloqueado = fisicoUnitizado - disponivel` expressa a quantidade unitizada indisponível, incluindo falta de endereço. A flag `bloqueada` de uma unidade representa apenas o bloqueio operacional expresso.

`naoEnderecado`, `emTriagem`, `emQuarentena` e `emArmazenagem` particionam a quantidade unitizada. `avariado` é uma classificação sobreposta a esses grupos; não deve ser somada novamente ao físico. O saldo exige SKU/produto para não misturar KG, UN ou outras unidades. Mercadoria pendente de unitização não é posição disponível nem reserva.

A leitura de saldo usa transação `SERIALIZABLE` para manter entradas, unidades e disponibilidade coerentes entre as consultas, inclusive durante unitização. O total disponível é agregado em consulta própria, com as subconsultas de localização no filtro; SQL Server não aceita subconsulta dentro da expressão agregada de [SUM](https://learn.microsoft.com/en-us/sql/t-sql/functions/sum-transact-sql?view=sql-server-ver17). A consulta não cria reserva. Listas e detalhes são informativos; o comando revalida versão/estado no momento de confirmar. Conflitos de lock/serialização exigem repetir a leitura; comportamento, tempo e contenção precisam ser medidos no SQL Server real.

Escritas de movimento bloqueiam pedido, cliente, armazém e produtos em ordem, depois os endereços envolvidos em ordem de ID e o conjunto. O lock do armazém serializa a disputa por posições inclusive entre clientes/pedidos distintos; é uma escolha conservadora para o primeiro fluxo, não evidência de capacidade produtiva. Bloqueio preventivo exige pedido/armazém e liberação revalida vínculos/endereço/disponibilidade. Configuração de capacidade/conjunto também bloqueia armazém antes dos endereços. Origem, destino, medidas, datas, revisão da unidade/pedido, auditoria e histórico/repetição são confirmados juntos ou revertidos juntos.

## Integração e pendências delimitadas

FE07 pode usar leitura do UUID, leitura de endereço, medidas iniciais, confirmação e tratamento de conflito. FE08 pode usar lista, saldo e histórico, distinguindo saldo indisponível de bloqueio expresso. FE06 deve usar `versaoConteudo` para atualizar a etiqueta; mudar endereço não demanda nova etiqueta. Os contratos estão disponíveis, mas telas e coletor ainda não foram implementados/validados.

BE08 permanece em validação externa de V4, locks, isolamento, permissões, capacidade real e equipamentos. BE14 permanece parcial: faltam indicadores financeiros, contagem, carga inicial e contingência. Próximo macrobloco de código: **BE09, pedido de saída integral, seleção FIFO e reserva atômica**; complementar o saldo reservado e coordenar bloqueios sem liberação silenciosa. Nenhuma integração fiscal, emissão, preço real ou publicação foi realizada.
