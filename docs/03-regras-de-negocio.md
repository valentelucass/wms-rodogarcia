# Regras de negócio do WMS Rodogarcia

Os requisitos abaixo foram extraídos da especificação funcional versão 1.0. As referências usam seção e página do [PDF original](referencias/Especificacao_Funcional_WMS_Rodogarcia.pdf). Interpretações e propostas aparecem separadamente para não serem confundidas com regras aprovadas.

Atualização de 05/10/2026: a tabela RN preserva os requisitos originais do PDF. As respostas posteriores estão no [documento 10](10-respostas-recebidas-2026-10-05.md), e as interpretações/propostas que conciliam o conjunto no [documento 11](11-alinhamentos-apos-respostas.md). Não tratar o questionário histórico como pendente.

## Requisitos especificados

| ID | Regra | Referência |
| --- | --- | --- |
| RN01 | Atender outros produtos, clientes e unidades logísticas além das bobinas da Brasel. | Seção 1, p. 2; seção 13, p. 8 |
| RN02 | Conhecer proprietário, SKU, quantidade, unidade logística, status, armazém e endereço da mercadoria. | Seção 1, p. 2 |
| RN03 | Manter WMS responsável pela armazenagem e TMS responsável pelo transporte. | Seção 1.1, p. 2; seção 8.6, p. 6 |
| RN04 | Cadastrar cada armazém com dados operacionais e fiscais. Todo endereço pertence a um armazém. | Seções 3.1 e 3.2, pp. 2 e 3; seção 13, p. 8 |
| RN05 | Formar o endereço por rua, nível e posição; A101 é o exemplo de rua A, nível 1 e posição 01. | Seção 3.2, p. 3 |
| RN06 | Cadastrar cliente, produto e dados fiscais. O produto tem proprietário e unidade de medida. | Seções 3.3 e 3.4, p. 3 |
| RN07 | Permitir regras fiscais parametrizáveis por produto e operação; CFOP não deve ser obrigatoriamente fixo no SKU. | Seção 3.4, p. 3; seção 9, p. 7 |
| RN08 | Manter tabelas independentes, padrão ou específicas, vinculadas ao cliente, com vigência e histórico. | Seção 3.5, pp. 3 e 4; seção 13, p. 8 |
| RN09 | Permitir serviços adicionais vinculados a pedidos de entrada ou saída para cobrança posterior. | Seção 3.6, p. 4; seção 13, p. 8 |
| RN10 | Identificar cada unidade com ID único permanente. A etiqueta contém SKU, ID, data de entrada e código de leitura. | Seção 4, p. 4 |
| RN11 | Manter endereço no sistema, fora da etiqueta da mercadoria. Mudanças de endereço preservam a etiqueta. | Seção 3.2, p. 3; seção 6.2, p. 6 |
| RN12 | Criar pedidos de entrada manualmente ou por importação de XML, com consulta e filtros. | Seções 5.1 e 5.2, p. 4 |
| RN13 | Registrar a conferência física e a decisão do operador; não há automação física de conferência na primeira versão. | Seção 5.3, pp. 4 e 5 |
| RN14 | Registrar divergências e permitir encaminhamento para quarentena quando necessário. | Seção 5.3, p. 5 |
| RN15 | Não misturar SKUs diferentes na mesma unidade logística na primeira versão. | Seção 5.4, p. 5 |
| RN16 | A soma das quantidades unitizadas deve corresponder à quantidade conferida. | Seção 5.4, p. 5 |
| RN17 | Excluir mercadoria em quarentena do estoque disponível, do FIFO e da reserva. | Seção 5.5, p. 5; seção 7, p. 6 |
| RN18 | Endereçar após leitura da unidade e do endereço, validando armazém, posição e localização; privilegiar leitura física. | Seção 6, p. 5 |
| RN19 | Registrar histórico das mudanças de endereço. | Seção 6.2, p. 6 |
| RN20 | Permitir consulta de estoque por cliente, SKU, unidade, armazém, endereço e status. | Seção 7, p. 6 |
| RN21 | Iniciar os eventos de armazenagem a partir do endereçamento, conforme a tabela contratada. | Seção 7, p. 6; seção 15, p. 9 |
| RN22 | Criar pedidos de saída manualmente ou por XML. | Seções 8.1 e 8.2, p. 6 |
| RN23 | Sugerir pelo FIFO as unidades disponíveis mais antigas pela data de entrada, excluindo bloqueadas, reservadas, indisponíveis e expedidas. | Seção 8.3, p. 6 |
| RN24 | Reservar quando o operador confirmar a seleção e impedir que outro pedido obtenha as mesmas unidades. | Seção 8.4, p. 6 |
| RN25 | Registrar a retirada física e a baixa no WMS; transporte e entrega permanecem no TMS. | Seções 8.5 e 8.6, p. 6 |
| RN26 | Preparar integração com o emissor fiscal usando os dados já registrados. Uma emissão falha ou rejeitada não pode concluir a baixa definitiva. | Seção 9, p. 7 |
| RN27 | Registrar fatos faturáveis por cliente e tabela vigente, consolidando o mês com origem identificável de cada valor. | Seção 10, p. 7 |
| RN28 | Disponibilizar relatórios operacionais, financeiros e indicadores de ocupação, estoque e pedidos. | Seções 11 e 12, pp. 7 e 8 |
| RN29 | Registrar usuário, data e hora e origem/destino nas operações críticas; manter cobranças auditáveis. | Seção 13, p. 8 |
| RN30 | Priorizar simplicidade nas telas de coletor; o mapa visual pode ser entregue depois do núcleo. | Seção 2, p. 2; seção 6.1, p. 6; seção 12, p. 8 |

## Interpretações usadas na análise

Estas conclusões são deduções do fluxo descrito, não novos requisitos textuais.

| ID | Interpretação | Fundamento |
| --- | --- | --- |
| I01 | Importar XML representa o documento e a previsão; não comprova recebimento físico. | RN12 e RN13 separam criação do pedido e conferência. |
| I02 | Unidade reservada continua no saldo fisicamente presente até a retirada. | RN24 e RN25 separam reserva e baixa. |
| I03 | Reimprimir a etiqueta ou mudar de endereço não cria unidade nova nem reinicia sua data de entrada. | RN10 e RN11 tratam identidade permanente e dados estáveis. |
| I04 | Quantidade do produto, número de unidades logísticas e ocupação são medidas distintas. | Seções 3.5 e 5.4 dão exemplos de várias caixas em um pallet. |
| I05 | O código A101 só identifica uma posição quando considerado junto com seu armazém. | RN04 exige vínculo do endereço com o armazém. |
| I06 | Uma aprovação fiscal não comprova retirada física. | Seção 15, p. 9, separa emissão, confirmação da saída e baixa. |
| I07 | O fechamento precisa do histórico do período, inclusive de mercadorias já expedidas. | RN27 exige origem dos valores e consolidação mensal. |

O PDF tratava a mistura de SKUs por unidade logística. Q06 acrescentou uma unidade por posição, sem compartilhamento, e a possibilidade de duas posições para unidade grande. A conciliação com a negativa genérica de Q14 está proposta em AC05.

Q21 definiu FIFO pela chegada física, primeira chegada quando uma nota é entregue em partes e manutenção da data original na devolução. Reagrupamento não reúne datas diferentes. FIFO não usa a data da nota nem o início da cobrança. Transferência foi excluída da primeira versão pela Q11.

## Proteções propostas

| ID | Proposta | Finalidade |
| --- | --- | --- |
| PR01 | Centralizar alterações de estoque e verificar novamente a disponibilidade ao confirmar uma reserva. | Evitar reservas simultâneas incompatíveis. |
| PR02 | Reconhecer repetição da mesma solicitação e manter seu resultado único. | Evitar baixas, reservas ou cobranças duplicadas. |
| PR03 | Preservar diferenças entre previsto e recebido, inclusive mercadoria bloqueada. | Evitar ocultar faltas, sobras e avarias. |
| PR04 | Registrar correções, cancelamentos e reversões com vínculo ao fato original. | Preservar explicação do saldo e do histórico. |
| PR05 | Manter situações de pedido, mercadoria, localização, fiscal e cobrança separadas. | Representar pendências simultâneas com clareza. |
| PR06 | Conferir identidade, proprietário, produto, armazém e permissão na operação. | Evitar movimentação ou consulta fora do contexto permitido. |
| PR07 | Revalidar capacidade e destino na confirmação da movimentação. | Evitar ocupação incompatível com os limites cadastrados e o conjunto de posições da unidade. |
| PR08 | Não concluir a retirada enquanto houver impedimento operacional ou fiscal no fluxo aplicável. | Proteger a baixa e expor a pendência ao operador. |
| PR09 | Preservar a tabela e a memória de cada cálculo, inclusive em processamento posterior. | Explicar o valor e impedir recálculo histórico silencioso. |
| PR10 | Consultar resultado fiscal desconhecido antes de repetir a emissão. | Evitar tratar ausência de resposta como rejeição confirmada. |
| PR11 | Ao detectar avaria em unidade reservada, bloquear sua expedição e sinalizar o pedido afetado. | Evitar liberar ou substituir mercadoria silenciosamente. |

## Regras complementadas pelo raciocínio

As respostas de 05/10 permitem avançar sem nova rodada extensa de perguntas. Pedido de saída integral, reserva sem vencimento, parcial de pallet, quarentena da carga e perfis estão esclarecidos. AC01 a AC16 completam o desenho com interpretações e propostas rastreáveis.

I06 é mantida como base da conciliação de Q22: emissão habilita a saída, mas supervisor/gestor confirma a retirada física. A fórmula que combina pico com diárias, a cobrança de duas posições e os efeitos da avaria possuem convenções explícitas em AC04, AC05 e AC08; são propostas a conferir por exemplos antes da cobrança real.

Preços, parâmetros fiscais, cadastros e condições de ambiente são dados a obter na implantação. Não inventá-los e não transformá-los em bloqueio para o estudo de negócio e arquitetura.
