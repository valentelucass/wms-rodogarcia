# Respostas recebidas para o WMS Rodogarcia

Recebidas em 05/10/2026 no arquivo [Questionamentos WMS Rodogarcia](referencias/Questionamentos_WMS_Rodogarcia_2026-10-05.docx). Os 110 subitens possuem texto de resposta. As 12 imagens distintas e os complementos ilustram cadastros, preços, endereços e etiquetas.

Este registro resume o que foi informado. As interpretações para completar o funcionamento estão em [regras consolidadas](11-alinhamentos-apos-respostas.md). Os pontos ambíguos têm solução proposta; não formam uma nova rodada de perguntas. Campos e valores das imagens são referências e não se tornam automaticamente parâmetros aprovados para Osasco.

## Operação e responsáveis

- Primeiro piloto em Osasco, com o cliente escrito como Bracel; o PDF usa Brasel. A identificação empresarial deve vir do cadastro.
- Depois de validado, replicação em Castro com a Tigre.
- Data desejada: 13/10/2026. Não é uma estimativa de entrega validada.
- Dez usuários e quatro simultâneos, como estimativa; volume de mercadorias ainda desconhecido.
- Caio acompanha testes e valida a operação. Natalina, da Controladoria, orienta o fiscal. Mickael fornece equipamentos/amostras. Lucas responde pelo ambiente e pela recuperação em TI.

Origem: Q13, Q25, Q26 e Q27.

## Produto, quantidade e identidade

O piloto usa bobinas. Peso, quando necessário, vem da nota do cliente. O SKU deve corresponder ao cadastro do produto do cliente. O cadastro DUN informa embalagem e quantidade; não substitui a identidade única de cada pallet ou bobina prevista no PDF.

É permitida a retirada de parte de um pallet. Reagrupamento exige mesmo produto e lote, e Q21.d não permite reunir datas de recebimento diferentes. Pedidos por peso não admitem diferença entre a quantidade solicitada e a saída correspondente à nota de origem.

Se houver validade, haverá aviso ao usuário na entrada do sistema. A antecedência é um parâmetro a cadastrar; não foi solicitado bloqueio automático adicional.

A etiqueta deve acrescentar nota de entrada e quantidade de produtos por DUN à data de entrada. SKU, ID permanente e código de leitura do PDF continuam aplicáveis; endereço permanece no sistema.

Origem: Q01, Q02, Q21 e apêndices de produtos e etiquetas. Tratamento: AC10 e AC16.

## Recebimento e endereços

Uma nota pode chegar em veículos ou dias diferentes, mas será efetivada uma única vez. Um pedido pode reunir várias notas; uma nota não pode pertencer a mais de um pedido de entrada. Recebimento incompleto permanece aberto e em quarentena até solução com o cliente.

Falta, sobra ou avaria coloca toda a carga em quarentena. O responsável interno trata com o cliente e o supervisor libera operacionalmente. Fotos foram consideradas úteis, sem obrigação expressa. Existe área física de quarentena.

Mercadoria na triagem não atende saída. A interpretação compatível com o PDF é exigir liberação e endereçamento para disponibilidade. No pedido manual, informar nota, SKU e quantidades; o XML pode vir depois.

Uma posição recebe uma unidade, sem compartilhar produtos ou clientes. Q06.a admite duas posições para unidade grande; Q14.c nega o cenário numa pergunta composta. A conciliação proposta está em AC05. Triagem, quarentena e separação aparecem separadamente na ocupação. Limites de peso, tamanho, tipo e empilhamento serão cadastrados. Endereços pertencem ao armazém e devem ser importáveis por Excel.

Origem: Q03 a Q06, Q14, Q19 e apêndice de localizações. Tratamento: AC03 e AC05.

## Pedidos, FIFO e reservas

O pedido de saída não excede o saldo disponível e deve ser atendido inteiro. Não há várias retiradas em datas diferentes nem cancelamento parcial: cancelar o pedido e criar outro na quantidade correta. Isso é compatível com retirar 100 caixas de um pallet de 500 para atender integralmente um pedido de 100.

Cada pedido possui um proprietário e uma filial. O emitente da nota de entrada é o cliente proprietário. Mudança de proprietário dentro do armazém foi descartada.

FIFO usa chegada física; quando uma nota chega em partes, vale a primeira chegada. Devolução preserva a referência original. Para empate, foram citados proximidade e níveis inferiores. É admitida escolha por nota específica, com justificativa. Qualquer usuário pode justificar; supervisor autoriza exceções segundo Q20.

A reserva não vence; termina na retirada ou no cancelamento. Problema fiscal mantém a reserva aguardando solução interna. Urgência não transfere diretamente uma reserva: cancela-se o pedido original antes de atender o novo.

Origem: Q07 a Q09, Q12, Q20 e Q21. Tratamento: AC10 e AC11.

## Separação, fiscal, cancelamento e retorno

Cada bobina ou pallet é lido antes da saída; não há segunda conferência sistêmica. A Rodogarcia emite a nota no NOTAZZ após separação. Um pedido pode exigir várias notas. Supervisor ou gestor confirma, com XML como comprovante.

Q22 associa a confirmação à emissão, mas Q16 encerra armazenagem na retirada física e o PDF separa esses marcos. AC01 propõe a emissão como condição e a confirmação do responsável como registro físico, sem baixa automática apenas pelo XML.

Em desistência, cancelar nota e pedido, devolver mercadoria separada a uma posição e cobrar serviços executados, inclusive movimentação adicional aplicável. Não se exige nova conferência no retorno interno. Devolução depois de saída física usa novo pedido de entrada, ligado à origem para preservar FIFO.

Transferência entre armazéns está fora da primeira versão. Transporte é do TMS; taxas correspondentes podem ser lançadas manualmente. Os valores de serviços do WMS vão para o ESL emitir a NFS-e. O apêndice admite integração NOTAZZ em segunda fase e mostra retornos físico e simbólico; AC02 propõe o procedimento manual intermediário.

Origem: Q10, Q11, Q13, Q17, Q21 e Q22. Tratamento: AC01 e AC02.

## Cobrança e fechamento

- Entrada se torna cobrável após endereçamento; saída, após concluir o pedido.
- Armazenagem por posição e dia de calendário; segunda até terça gera uma diária e entrada/saída no mesmo dia gera zero.
- Não há horário de corte; fins de semana e feriados seguem a regra normal.
- Pico diário de posições; mudança de endereço não altera a cobrança.
- Nova tabela vale a partir de sua vigência, inclusive para mercadoria que já estava armazenada.
- Reserva, separação e quarentena posterior mantêm armazenagem; saída física encerra.
- Avaria atribuída à Rodogarcia interrompe cobrança do item afetado.
- Existe mínimo. Demais condições perguntadas em Q15.d não foram indicadas como existentes.
- Ciclo por cliente, inclusive dia 24 ao dia 24; respeitar o último corte para não duplicar dias.
- Gestor aprova ou rejeita o fechamento inteiro. Depois de emissão da nota, correções vão ao ciclo seguinte. Demonstrativos ficam no histórico.

O exemplo da Tigre mostra preços, GRIS, mínimo, veículos, contêineres e tributos. O texto cita planilha de cálculos, mas não há planilha editável anexada. Essas imagens ajudam a prever campos; não comprovam preços ou fórmulas do cliente piloto. As convenções que combinam diárias, pico e períodos estão em AC04 a AC08.

Origem: Q14 a Q17 e apêndice de preços.

## Serviços, avaria e valor do estoque

Entrada, saída e paletização foram citadas como lançamentos manuais em pedidos. Supervisor registra e informa ao cliente; gestor libera cobrança. Rearrumação não exige autorização prévia do cliente. A referência de Q23.c a um pedido ainda inexistente é conciliada em AC09, sem criar uma saída fictícia.

Valor da mercadoria vem da nota de entrada, fica fixo e é reduzido proporcionalmente quando parte sai. Reservada e quarentena entram no indicador; avariada fica fora dele. Isso não elimina a mercadoria física: avaria é condição registrada, com destino a tratar com o cliente.

Origem: Q04.e, Q16, Q18 e Q23. Tratamento: AC08 e AC09.

## Contagens, acesso e continuidade

Estoque inicial pode não existir. Se existir, levantar por planilha, receber no sistema, imprimir/aplicar etiquetas e ler todas para conferir quantidades. Contagem física manual com apoio de leitura das etiquetas/DUN. Dados incompletos são tratados com o cliente; o histórico não deve ser inventado.

Gestor tem acesso geral e controla cadastros, fiscal e valores. Supervisor possui autoridade operacional, inclusive liberação, correção e cancelamento. Operação recebe, confere, movimenta, separa e trabalha com estoque, etiquetas e contagens. Não há exigência de segunda pessoa para aprovação interna.

Q24 bloqueia novas entradas e saídas de inativos e dá resposta afirmativa genérica à continuidade com estoque existente. AC12 propõe concluir compromissos antes da inativação definitiva.

Coletor web e impressoras térmicas Tanca; equipamentos concretos precisam de teste. Reimprimir etiqueta danificada e aguardar recuperação de coletor com falha. Em queda de rede/sistema, registrar em planilha para posterior lançamento. Parada tolerada informada: 36 horas. Backup foi solicitado; o tempo de parada não define perda aceitável de dados.

Origem: Q19, Q20, Q24, Q26 e Q27. Tratamento: AC12 a AC16.

## Efeito na arquitetura

Permanecem React/TypeScript, Java/Spring com pastas MVC convencionais e SQL Server. A modelagem precisa distinguir quantidade de produto, embalagem, unidade física, posição, reserva, condição, nota fiscal e cobrança; também data FIFO, chegada real e início da armazenagem.

As respostas permitem continuar o estudo e preparar exemplos completos de operação. Preços, cadastros e verificações de ambiente são insumos de implantação. As escolhas de conciliação estão identificadas como interpretações/propostas, sem atribuir aprovação tácita ao gestor. Não houve implementação nesta atualização.
