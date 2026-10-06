# Fluxos operacionais do WMS Rodogarcia

Os passos principais vêm do PDF, sobretudo das seções 5 a 10 e 15. Os cuidados adicionais são propostas de desenho. Os identificadores RN, I e PR estão em [regras de negócio](03-regras-de-negocio.md); Q01 a Q27 estão nas [perguntas ao gestor](05-perguntas-para-o-gestor.md).

## Recebimento

1. Criar pedido manualmente ou importar o XML.
2. Identificar cliente proprietário, produtos e quantidades previstas.
3. Receber fisicamente e registrar a conferência humana.
4. Registrar faltas, sobras e avarias quando houver.
5. Manter toda a carga em quarentena se houver falta, sobra, avaria ou entrega incompleta; AC03 propõe delimitar a carga pelo pedido e liberar pelo supervisor após tratativa.
6. Organizar a quantidade conferida em unidades logísticas de um único SKU cada.
7. Gerar identificadores e etiquetas.
8. Liberar as unidades elegíveis e encaminhar para triagem.

Proposta: quantidade recebida, quantidade bloqueada e quantidade liberada devem permanecer identificáveis mesmo antes da etiqueta definitiva. A soma da unitização deve fechar com a conferência, sem criar ou apagar mercadoria para forçar uma igualdade com a nota.

Q01 a Q05 e Q12 foram respondidas. Registrar várias chegadas sem duplicar a nota e efetivar a entrada uma vez. Aplicar os tratamentos de AC03 e AC10.

## Endereçamento e movimentação

1. Ler a etiqueta da unidade.
2. Ler a etiqueta do endereço de destino.
3. Validar a leitura e o vínculo do endereço com o armazém.
4. Confirmar o endereçamento e atualizar ocupação e localização.
5. Guardar o histórico da movimentação.
6. Iniciar a contabilização da armazenagem conforme o contrato, no endereçamento inicial.

Proposta: validar condição da unidade, autorização e capacidade antes da confirmação. Origem, destino e ocupação devem permanecer coerentes. Uma movimentação posterior não cria uma nova entrada de mercadoria; eventuais serviços de movimentação seguem o contrato.

Triagem, quarentena e separação têm localização conhecida e ocupação mostrada separadamente. Triagem não atende saída. Armazenagem já iniciada continua em separação/quarentena; AC04 distingue posição física de equivalência cobrável. AC05 trata unidade grande em duas posições.

## Consulta e disponibilidade

O PDF prevê consulta por cliente, SKU, unidade, armazém, endereço e status. Quarentena, reserva, movimentação/separação e expedição excluem a unidade de novas sugestões FIFO.

Proposta: mostrar separadamente o saldo fisicamente presente e o saldo elegível para atendimento. Se dez bobinas estão presentes e três estão reservadas, continuam presentes dez; sete poderão estar disponíveis se não houver outros bloqueios.

Impedimentos podem se sobrepor. Se uma das três bobinas reservadas apresentar avaria, ela não deve ficar disponível nem ser contada como duas mercadorias bloqueadas. O pedido afetado precisa ser sinalizado.

## Pedido de saída e reserva

1. Criar pedido manualmente ou por XML.
2. Identificar proprietário, produto, quantidade e contexto do armazém.
3. Sugerir as unidades elegíveis mais antigas pela data de entrada.
4. Aguardar confirmação do operador.
5. Confirmar a reserva e retirar as unidades do saldo disponível para outros pedidos.
6. Separar e ler as unidades reservadas; eventual retirada parcial do pallet deve atender integralmente o pedido e preservar o saldo remanescente.

Proposta: revalidar a disponibilidade na confirmação; outra operação pode ter reservado a unidade desde a sugestão. A aplicação deve informar o conflito sem reservar uma quantidade indevida.

Reserva não vence; erro fiscal mantém o vínculo. FIFO usa a primeira chegada física da nota. Operador pode justificar exceção; supervisor/gestor autoriza conforme AC11. Para mudar quantidade do pedido, cancelar e recriar.

## Fiscal e retirada física

1. Preparar o documento fiscal conforme o fluxo aplicável.
2. Encaminhar e acompanhar a emissão quando necessária.
3. Resolver pendências ou rejeições sem concluir a baixa por uma emissão falha.
4. Confirmar a retirada física das unidades corretas.
5. Registrar a baixa e preservar sua origem.
6. O operador utiliza o TMS para o transporte, conforme o processo descrito.

Q13 situa a emissão após separação. AC01 concilia Q22 com o PDF: supervisor/gestor confirma a retirada com o XML; emissão e retirada podem ocorrer na mesma sequência, mas continuam registradas separadamente. NOTAZZ emite a nota; TMS continua responsável pelo transporte.

Proposta: não tratar ausência de resposta do emissor como rejeição certa. Registrar a referência e consultar o resultado antes de emitir novamente. Uma autorização fiscal, sozinha, não baixa a mercadoria; um cancelamento fiscal, sozinho, não comprova seu retorno.

AC02 propõe emissão externa no piloto com registro/importação no WMS; várias notas devem cobrir o pedido inteiro. Documento simbólico não implica saída física. Aplicar o procedimento fiscal validado pela Controladoria.

## Cobrança e fechamento

1. Registrar fatos de entrada, armazenagem, saída e serviços adicionais que gerem cobrança conforme o contrato.
2. Associar cada fato ao cliente e à tabela aplicável.
3. Calcular armazenagem a partir do endereçamento, segundo a fórmula contratada.
4. Consolidar o ciclo cadastrado por cliente, respeitando o último corte, e submeter o fechamento inteiro ao gestor.
5. Permitir identificar a origem de cada valor.

Proposta: guardar memória de cálculo e ajustes, recuperar períodos cujo processamento falhou e impedir que uma repetição cobre novamente o mesmo fato. O fechamento usa o histórico do período, inclusive unidades que já saíram.

AC04 a AC09 definem as convenções propostas: início no endereçamento, dia final excluído, pico cobrável, vigência por dia, serviços sem duplicidade e ajustes rastreáveis. Preços e parâmetros são cadastrados para o cliente; demonstrativo aprovado segue ao ESL para NFS-e.

## Cancelamentos e correções

O PDF não descreve todas as reversões. Antes de definir a ação, será necessário saber se houve conferência, reserva, deslocamento físico, documento fiscal, saída ou fechamento financeiro.

Proposta: uma reversão deve considerar esses efeitos e manter rastreabilidade. Cancelar um pedido não deve tornar imediatamente disponível uma unidade que ainda está separada em local inadequado ou bloqueada por avaria. Q10 permite retorno interno à posição sem nova conferência, preservando serviços executados. Devolução após saída física exige novo pedido de entrada e referência à data FIFO original.

Transferência entre armazéns está fora da primeira versão. Estoque inicial só será carregado se existir; diferenças posteriores seguem o ajuste rastreável proposto em AC13.

AC12 propõe resolver compromissos antes da inativação definitiva. Falha isolada do coletor aguarda recuperação; indisponibilidade geral permite planilha, com conciliação coordenada pelo supervisor conforme AC14. O aplicativo inicial continua dependendo de conexão.
