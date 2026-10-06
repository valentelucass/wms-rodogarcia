# Contexto e escopo do WMS Rodogarcia

O WMS acompanhará a mercadoria enquanto estiver sob controle do armazém: recebimento, conferência, identificação, organização em unidades, localização, estoque, reserva, saída e cobranças relacionadas. A base funcional é a especificação versão 1.0, especialmente as páginas 2, 7 a 10.

## Objetivo da primeira operação

A Brasel, com bobinas, é o primeiro caso de uso previsto. O sistema deve nascer capaz de representar outros clientes, armazéns, caixas, pallets e unidades logísticas, sem transformar todas as possibilidades futuras em funcionalidades obrigatórias da primeira entrega.

Para cada mercadoria, o sistema precisa conhecer proprietário, SKU, quantidade, unidade logística, condição, armazém e localização. As respostas de 05/10 acrescentam peso conforme nota, lote quando controlado e configuração de embalagem/quantidade por DUN.

## Limite entre armazenagem e transporte

| WMS | TMS |
| --- | --- |
| Receber, conferir e identificar | Controlar transporte de entrada e saída |
| Localizar e movimentar dentro do armazém | Controlar veículo, motorista, rota e frete |
| Consultar estoque e reservar para pedidos | Tratar CT-e, MDF-e e execução do transporte |
| Confirmar retirada e baixa de estoque | Acompanhar transporte e entrega |

O PDF descreve o operador utilizando o TMS após a emissão da nota de saída. Uma integração automática com o TMS não está estabelecida como requisito inicial. A comunicação com o emissor fiscal, por sua vez, está prevista e precisa ser estudada cedo.

## Áreas funcionais previstas

| Área | Abrangência do PDF |
| --- | --- |
| Cadastros | Clientes, produtos, armazéns, endereços, tabelas de preços e serviços |
| Entrada | Pedido por XML ou manual, conferência humana, divergências, quarentena, unitização e etiquetas |
| Endereçamento | Leitura da unidade e do endereço, ocupação e histórico de movimentação |
| Estoque | Consulta por cliente, produto, unidade, armazém, endereço e situação |
| Saída | Pedido, sugestão FIFO, confirmação de reserva, fluxo fiscal e saída física |
| Faturamento | Eventos de entrada, armazenagem, saída e serviços, com fechamento mensal |
| Relatórios | Estoque, ocupação, movimentações, valores e cobranças |
| Dashboard | Indicadores da operação; mapa visual pode ser posterior |

## Termos usados

| Termo | Significado |
| --- | --- |
| Cliente proprietário | Dono da mercadoria sob armazenagem |
| SKU | Identificação de um produto; o contexto do proprietário precisa ser preservado |
| Unidade de medida | Medida da quantidade, como caixa, peça ou peso, conforme o cadastro e a operação |
| Unidade logística | Volume identificado e movimentado, como uma bobina ou um pallet |
| Unitização | Organização de uma quantidade de produto em unidades logísticas |
| Endereço | Posição física dentro de um armazém, formada por rua, nível e posição |
| Quarentena | Condição de bloqueio da mercadoria até resolução |
| Reserva | Compromisso de uma unidade com um pedido de saída |
| FIFO | Sugestão das unidades disponíveis mais antigas pela data de entrada |
| Evento de cobrança | Registro de um fato ou período que fundamenta um valor cobrado |

Um pallet com 500 caixas representa uma unidade logística e 500 caixas de produto. Ocupação física e quantidade cobrável dependem de capacidade e contrato. Não converter automaticamente essas medidas em uma única contagem.

## Relatórios e indicadores previstos

- Estoque e situação por cliente e SKU.
- Quantidade de unidades e quantidade de SKUs por cliente.
- Quarentena e reservas.
- Localização por identificador e histórico de movimentações.
- Valor da mercadoria pela nota de entrada, proporcional ao saldo e com avariadas excluídas do indicador, conforme Q18.
- Ocupação, posições ocupadas e disponíveis por armazém.
- Entradas, saídas e pedidos em aberto.
- Serviços adicionais e valores para faturamento.

## Escopo após as respostas de 05 de outubro

Piloto em Osasco com bobinas; depois Castro/Tigre. Retirada parcial de pallet permitida, pedido de saída integral, entregas de uma nota em partes com efetivação única e devolução física por nova entrada. Transferência entre armazéns fora da primeira versão. NOTAZZ para notas de mercadorias e ESL para NFS-e dos serviços; procedimento manual inicial proposto.

Consultar [respostas recebidas](10-respostas-recebidas-2026-10-05.md) e [regras consolidadas](11-alinhamentos-apos-respostas.md). Data desejada 13/10/2026; Caio valida o piloto. Isso não constitui prazo técnico de entrega estimado.

Inventário completo aparece como definição futura no PDF. Identificar o estoque inicial e propor um procedimento mínimo de correção antes do piloto são cuidados de implantação, sem assumir que um módulo completo de inventário foi aprovado.
