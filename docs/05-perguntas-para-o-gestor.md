# Perguntas para o gestor do WMS Rodogarcia

Roteiro histórico ampliado em 03/10/2026, com 27 temas e 110 subitens. Em 05/10/2026 foi recebido o Word com respostas para todos os subitens. A numeração e o texto original foram preservados. Não reenviar este roteiro como se estivesse sem resposta. Consultar [respostas recebidas](10-respostas-recebidas-2026-10-05.md) e [regras consolidadas](11-alinhamentos-apos-respostas.md). Nenhuma mensagem foi enviada automaticamente.

Os temas 1 a 20 preservam os identificadores Q01 a Q20 da primeira versão. Seus detalhes foram desdobrados e complementados. Os temas 21 a 27 acrescentam lacunas e levantamentos antes dispersos nos fluxos e na arquitetura. As perguntas condicionais identificam uma necessidade possível; não a aprovam como escopo.

Registrar as respostas com número e letra, por exemplo Q02.a, mantendo data, responsável e consequência em [decisões e pendências](06-decisoes-e-pendencias.md). Manter a numeração estável. Escolhas de programação e ferramentas continuam com a equipe técnica; os temas 25 a 27 solicitam informações da operação e contatos responsáveis.

## Mensagem para encaminhar

Olá! Estamos estruturando o WMS Rodogarcia e precisamos completar as regras que a documentação ainda não esclarece.

Pode responder por número, com frases simples ou exemplos. Se a situação não acontecer, escreva “não se aplica”. Se ainda não houver uma regra, escreva “precisamos definir”. Se a resposta variar por cliente ou armazém, indique a diferença. Os pontos fiscais, comerciais e de TI podem ser encaminhados aos responsáveis dessas áreas.

As perguntas condicionais só precisam ser respondidas quando a situação existir. Um procedimento, contrato ou exemplo real que já esclareça um tema pode substituir a resposta item por item.

1. **Como identificar e medir a mercadoria**

    - **a)** Quais dados precisamos guardar em cada bobina, pallet ou caixa: número do fabricante, lote, peso, validade ou outros?
    - **b)** A quantidade é controlada e solicitada em bobinas, caixas, peças, quilos ou mais de uma dessas medidas? Se houver peso, vale o informado na nota, na etiqueta ou o conferido no armazém?
    - **c)** O mesmo produto pode ter um código na nota do fornecedor e outro no cadastro do cliente? Como a equipe reconhece que são o mesmo produto?
    - **d)** Se houver controle de validade, o que deve acontecer quando a mercadoria vencer ou estiver próxima do vencimento?

2. **Retirada de parte de um pallet e reorganização**

    - **a)** Um pallet com 500 caixas pode ter somente 100 retiradas? Se sim, como ficam identificadas as caixas retiradas e as 400 restantes?
    - **b)** A operação desmonta ou junta pallets do mesmo produto? Pode juntar mercadorias de recebimentos ou lotes diferentes?
    - **c)** Se o pedido for por peso e as bobinas saírem inteiras, pode sair um pouco mais ou menos que o solicitado? Qual diferença é aceita e quem autoriza?

3. **Recebimento em partes e documentos**

    - **a)** Uma mesma nota pode chegar em mais de um veículo ou em dias diferentes?
    - **b)** Um pedido de entrada pode reunir várias notas? Uma nota pode ser atendida em mais de um pedido de entrada?
    - **c)** Se chegar apenas parte da mercadoria, o pedido fica aberto aguardando o restante? Quando podemos encerrá-lo sem receber tudo?
    - **d)** No recebimento cadastrado manualmente, quais dados da nota precisam estar disponíveis para liberar a mercadoria? O XML pode ser anexado depois?

4. **Faltas, sobras, avarias e quarentena**

    - **a)** Se a nota indicar 100 caixas e chegarem 98, recebemos as 98 e deixamos duas pendentes ou encerramos registrando a falta? E se chegarem 102, o que acontece com as duas extras?
    - **b)** Se cinco das 100 caixas estiverem avariadas, podemos liberar as 95 boas ou devemos bloquear todo o recebimento? Quando a quarentena é obrigatória?
    - **c)** Para liberar uma mercadoria bloqueada, basta a decisão do responsável interno ou é necessária autorização do cliente? É preciso guardar foto, laudo ou outro comprovante?
    - **d)** A mercadoria bloqueada vai para uma área específica ou pode permanecer na posição onde está?
    - **e)** Se a avaria aparecer depois da reserva, quem decide substituir a unidade? Se ela não puder ser aproveitada, qual é o destino: devolução, descarte ou outro procedimento?

5. **Retirada antes de guardar na posição**

    - **a)** Uma mercadoria já conferida e liberada, mas ainda na triagem, pode ser reservada e retirada diretamente?
    - **b)** Se isso acontecer, existe alguma conferência ou autorização adicional antes da retirada?

6. **Capacidade e uso dos endereços**

    - **a)** Cada posição comporta uma única bobina ou pallet, ou várias unidades? Uma unidade grande pode ocupar mais de uma posição?
    - **b)** Existem limites por peso, tamanho, tipo de produto ou empilhamento?
    - **c)** Unidades de produtos ou clientes diferentes podem compartilhar a mesma posição?
    - **d)** Há posições interditadas ou reservadas para uso específico? Elas devem ficar fora da capacidade disponível mostrada nos relatórios?
    - **e)** Triagem, quarentena e separação entram na contagem de posições do armazém ou serão mostradas separadamente?

7. **Pedido maior que o estoque disponível**

    - **a)** Se o cliente pedir dez unidades e houver oito disponíveis, podemos reservar e entregar as oito, deixando duas pendentes?
    - **b)** Um mesmo pedido pode ter retiradas em dias diferentes? Como sabemos que o cliente desistiu do restante e o pedido pode ser encerrado?
    - **c)** Se o produto estiver em mais de um armazém, um pedido pode ser atendido por vários armazéns ou deve ficar vinculado a apenas um?

8. **Escolha de uma unidade fora da ordem das mais antigas**

    - **a)** Em quais situações o cliente ou a operação pode escolher uma unidade específica ou uma mais nova?
    - **b)** Essa escolha precisa de autorização e justificativa? De qual função?
    - **c)** Quando duas unidades tiverem a mesma data de entrada, existe algum critério operacional de preferência ou podemos definir um desempate fixo no sistema?

9. **Prazo e liberação das reservas**

    - **a)** Se o cliente não retirar, a reserva fica até alguém cancelar ou vence após um prazo? Qual prazo?
    - **b)** O prazo muda quando a mercadoria já foi separada ou a nota já foi emitida?
    - **c)** Se houver problema na emissão da nota, a reserva continua aguardando correção? Quem decide liberá-la?
    - **d)** Um pedido urgente pode usar uma unidade reservada para outro pedido? Se isso for permitido, quem autoriza a troca?

10. **Desistências, cancelamentos e devoluções**

    - **a)** Como a equipe procede quando o cliente desiste depois da reserva, da separação ou da emissão da nota?
    - **b)** Se a mercadoria já estiver separada, ela precisa voltar a uma posição e ser conferida antes de ficar disponível novamente?
    - **c)** Mercadorias já retiradas podem voltar? Se sim, voltam para conferência ou quarentena e como identificamos a saída que está sendo devolvida?
    - **d)** Quais serviços já realizados continuam sendo cobrados se o pedido for cancelado ou a mercadoria voltar?

11. **Transferências entre armazéns**

    - **a)** Precisamos atender transferências entre armazéns da Rodogarcia desde a primeira versão?
    - **b)** Se sim, quem solicita e autoriza, quem confirma a saída da origem e quem confirma a chegada ao destino?
    - **c)** Durante o transporte, como a equipe acompanha a mercadoria? O que acontece se chegar menos ou houver avaria?
    - **d)** Na cobrança, quando termina a armazenagem da origem e começa a do destino? Existem taxas de entrada, saída ou transferência nesse caso?

12. **Identificação do dono da mercadoria**

    - **a)** Como a equipe identifica o cliente proprietário quando quem emite a nota é um fornecedor ou outra empresa?
    - **b)** Um mesmo pedido de entrada ou de saída pode envolver mercadorias de mais de um proprietário?
    - **c)** A mercadoria pode mudar de dono enquanto continua fisicamente no armazém? Se isso acontecer, qual é o procedimento e precisamos atendê-lo na primeira versão?

13. **Notas fiscais e sistema emissor**

    - **a)** O XML usado no pedido de saída é a nota que já acompanhará a retirada ou é um documento usado como base para emitir outra nota?
    - **b)** Quem emite cada documento: o cliente, a Rodogarcia ou ambos, conforme a situação? Um pedido pode exigir mais de uma nota?
    - **c)** Qual sistema emite as notas hoje? Quem pode explicar o processo e colocar a equipe em contato com o suporte desse sistema?
    - **d)** Quem da área fiscal fornecerá ou validará as regras de cada operação e armazém, inclusive CFOPs e documentos necessários?
    - **e)** Podemos usar exemplos de documentos de uma entrada e de uma saída para entender o fluxo completo?

14. **Como cada serviço é cobrado**

    - **a)** Entrada, armazenagem e saída são cobradas por bobina, pallet, caixa, quilo, posição ou outro critério? Isso varia por cliente ou armazém?
    - **b)** Em qual momento o serviço de entrada e o de saída passam a ser cobrados? Por exemplo: após a conferência, após a liberação ou após a retirada física?
    - **c)** Quando várias unidades compartilham uma posição, ou uma unidade ocupa várias posições, como a cobrança é dividida?
    - **d)** Há valores negociados para uma operação específica, além da tabela do cliente? Como essa negociação é autorizada?
    - **e)** Podemos receber uma tabela ou contrato real e um exemplo de cobrança já calculada para conferir as regras?

15. **Como contar e calcular as diárias**

    - **a)** A contagem é por dia do calendário ou por períodos de 24 horas? Uma mercadoria endereçada na segunda e retirada na terça paga quantas diárias?
    - **b)** Se for endereçada e retirada no mesmo dia, paga uma diária, uma fração ou nada?
    - **c)** Existe horário de corte? Sábados, domingos e feriados seguem a mesma regra?
    - **d)** Há dias gratuitos, cobrança mínima, faixas de quantidade, descontos ou arredondamentos previstos no contrato?
    - **e)** Se a ocupação mudar durante o dia, cobramos pela maior ocupação, pela ocupação no fim do dia ou por outro critério? Uma mudança de endereço no mesmo dia altera o cálculo?
    - **f)** Se uma nova tabela começar a valer durante a permanência da mercadoria, qual regra o contrato prevê para os dias seguintes?

16. **Quando a armazenagem deixa de ser cobrada**

    - **a)** A cobrança termina quando a mercadoria sai da posição, vai para a separação ou deixa fisicamente o armazém?
    - **b)** Uma unidade já armazenada continua sendo cobrada enquanto está reservada, separada ou em quarentena?
    - **c)** Se a retirada for cancelada e a unidade voltar para a posição, a cobrança continua normalmente ou existe outra regra?
    - **d)** Se a avaria for atribuída à Rodogarcia, isso muda a cobrança durante o período de bloqueio?

17. **Fechamento e correção das cobranças**

    - **a)** O fechamento considera o mês do primeiro ao último dia ou cada cliente tem um período diferente?
    - **b)** Quem confere e aprova o fechamento? É possível aprovar alguns itens e deixar outros pendentes de discussão?
    - **c)** Se um serviço for esquecido ou cobrado errado depois do fechamento, corrigimos o mês fechado ou fazemos um ajuste no próximo?
    - **d)** O WMS entregará uma relação de valores para outro sistema faturar? Qual sistema e em qual formato a equipe utiliza essa informação hoje?
    - **e)** Há prazo definido para guardar e consultar os documentos e demonstrativos de cobrança? Quem confirma esse prazo?

18. **Valor da mercadoria mostrado nos relatórios**

    - **a)** Devemos usar o valor da nota de entrada, um valor informado pelo cliente, um valor de seguro ou outro?
    - **b)** Se parte de um pallet sair, como calculamos o valor que continua armazenado?
    - **c)** O valor é mantido desde a entrada ou pode ser atualizado? Quem fornece e autoriza essa atualização?
    - **d)** O valor total armazenado inclui unidades reservadas e em quarentena? Existe alguma exclusão usada nos relatórios atuais?

19. **Estoque inicial, contagens e correções**

    - **a)** Já haverá mercadoria armazenada quando começarmos? Onde estão registrados cliente, produto, quantidade, localização, data de entrada e valor?
    - **b)** Como será feita a conferência e identificação desse estoque inicial? Quem confirma que ele está correto?
    - **c)** Como tratar mercadoria existente sem nota de origem, data de entrada ou informação suficiente para o cadastro? Quem resolve essas pendências?
    - **d)** Se uma contagem mostrar falta, sobra, perda ou descarte, qual é o procedimento para ajustar o saldo e quais comprovantes são necessários?
    - **e)** Desde a primeira versão será necessário registrar contagens dentro do WMS, ou a contagem será feita fora e somente os ajustes aprovados entrarão no sistema?

20. **Quem pode ver, executar e aprovar**

    - **a)** Quais funções usarão o sistema e quais clientes e armazéns cada uma pode acessar?
    - **b)** Quem pode cadastrar ou alterar clientes, produtos, endereços e dados fiscais?
    - **c)** Quem pode liberar quarentena, corrigir quantidades, autorizar exceções de saída, transferir reservas e cancelar operações?
    - **d)** Quem pode consultar valores, alterar preços, conceder descontos e aprovar ou corrigir cobranças?
    - **e)** Alguma ação exige uma segunda pessoa para aprovar, diferente de quem a solicitou? Quais ações?

21. **Qual data define a mercadoria mais antiga**

    - **a)** Para ordenar as saídas, vale a data em que a mercadoria chegou fisicamente ou a data em que sua conferência foi concluída? Se ocorrerem em dias diferentes, qual usar?
    - **b)** Quando uma nota chega em várias entregas, cada entrega usa a sua data real de chegada?
    - **c)** Se houver devolução ou transferência entre armazéns, a unidade mantém a data de entrada original ou recebe uma nova referência para a ordem de saída?
    - **d)** Se for permitido juntar mercadorias recebidas em datas diferentes, como elas devem participar dessa ordem? As quantidades precisam continuar distinguíveis por recebimento?

22. **Conferência e comprovação da retirada**

    - **a)** Antes da saída, cada bobina ou pallet precisa ser lido no coletor para confirmar que é o reservado? Existe uma segunda conferência?
    - **b)** Em qual momento a retirada física deve ser confirmada: no carregamento, na entrega ao responsável pela retirada ou na saída do veículo do armazém?
    - **c)** Quem faz essa confirmação e qual comprovante é necessário?
    - **d)** Se o cliente retirar somente parte do que já estava separado e documentado, como a equipe resolve a diferença com a operação e o fiscal?

23. **Serviços adicionais durante a armazenagem**

    - **a)** Quais serviços adicionais serão cobrados desde o início e como cada um é medido?
    - **b)** Um serviço como rearrumação precisa de autorização prévia do cliente? Como essa autorização é comprovada?
    - **c)** Se o serviço ocorrer enquanto a mercadoria está armazenada, sem uma nova entrada ou saída, a qual pedido ele deve ser vinculado?
    - **d)** Quem registra que o serviço foi executado, qual quantidade informa e quem libera a cobrança?

24. **Cadastros inativos e bloqueios operacionais**

    - **a)** O que significa deixar um cliente ou armazém inativo na operação: impedir novas entradas, impedir novas solicitações ou outro comportamento?
    - **b)** Se ainda houver mercadoria armazenada, quais atividades precisam continuar permitidas, como retirada, transferência e correção?
    - **c)** Existem outros bloqueios usados hoje, além de quarentena e reserva? Em cada caso, quem aplica, quem libera e quais operações ficam impedidas?

25. **Primeira operação, volume e aprovação do piloto**

    - **a)** Qual armazém começará com a Brasel? Outros clientes ou tipos de mercadoria precisam entrar já na primeira versão?
    - **b)** Aproximadamente quantas unidades ficam armazenadas, quantas entram e saem por dia e quantas pessoas usam o sistema ao mesmo tempo? Existem dias ou horários de pico?
    - **c)** Existe uma data necessária para começar a operar e alguma dependência externa que possa afetá-la?
    - **d)** Quem acompanhará os testes e dará a confirmação de que a operação pode começar a usar o sistema?

26. **Coletores, impressoras e condições do armazém**

    - **a)** Quais modelos de coletor, impressora e etiqueta já existem? Há um responsável que possa disponibilizar equipamentos e amostras para validação?
    - **b)** Há código de barras já usado pelo cliente ou pelo armazém com o qual precisamos manter compatibilidade? Se houver, podem fornecer uma amostra?
    - **c)** A rede funciona em todas as áreas onde haverá leitura? Existem locais ou horários com falhas frequentes?
    - **d)** Se a etiqueta estiver danificada ou o coletor falhar, qual procedimento alternativo a operação permite e quem o autoriza?

27. **Procedimento quando o sistema fica indisponível**

    - **a)** Se a rede ou o sistema parar, a movimentação pode aguardar ou precisa continuar? Se continuar, como as operações são registradas hoje para depois conferir o estoque?
    - **b)** Qual é o maior tempo de parada que a operação consegue suportar?
    - **c)** Em uma falha grave, é necessário recuperar até a última movimentação confirmada? Se houver registros externos para reconstruir movimentos, qual período máximo a empresa aceita reconciliar dessa forma?
    - **d)** Quem da TI será responsável pelo SQL Server, acessos, infraestrutura e recuperação? Quem da operação deve ser acionado quando houver uma falha?

Se possível, encaminhe exemplos de uma entrada, uma saída, uma etiqueta, uma tabela de preços e uma cobrança já calculada. Eles ajudam a verificar se entendemos corretamente a rotina. As respostas podem ser enviadas em partes.
