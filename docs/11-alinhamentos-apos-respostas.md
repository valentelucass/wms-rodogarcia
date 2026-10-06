# Regras consolidadas para o WMS Rodogarcia

Atualização de 05/10/2026. O PDF e o questionário respondido permitem avançar na definição do projeto. Não é necessário reenviar as 110 perguntas nem transformar cada detalhe de funcionamento em uma nova pergunta ao gestor.

As regras explícitas estão resumidas em [respostas recebidas](10-respostas-recebidas-2026-10-05.md). Aqui estão as interpretações e propostas que completam o desenho. O responsável pediu que esgotássemos o raciocínio com o material disponível; isso autoriza formular estas soluções, mas não transforma uma interpretação em resposta literal do gestor ou em condição comercial homologada.

Os identificadores AC01 a AC16 organizam o tratamento dos pontos antes levantados. Eles não representam dezesseis perguntas a serem enviadas. O projeto pode avançar com esta base; valores de contrato e condições reais de equipamentos entram como dados de implantação.

## AC01 Baixa do estoque e nota de saída

**Base:** Q13, Q16 e Q22; seção 15 do PDF. A nota é emitida após a separação, supervisor ou gestor confirma a saída, e a armazenagem termina na retirada física.

**Interpretação proposta:** a emissão da nota habilita a conclusão da saída. O supervisor ou gestor confirma que as unidades foram efetivamente entregues para retirada, vinculando o XML. Essa confirmação registra a baixa e encerra a armazenagem. Se emissão e retirada ocorrerem juntas, o operador pode concluir tudo na mesma sequência; continuam sendo fatos distintos no histórico.

Exemplo: nota emitida na segunda, mercadoria retirada na terça. Na segunda ela permanece fisicamente no armazém e reservada; a data de saída é terça. Isso concilia o comprovante fiscal exigido em Q22 com o término físico expresso em Q16. A frase de Q22 que associa confirmação à emissão é tratada como simplificação operacional, não como prova automática de retirada.

Retorno simbólico, citado no apêndice, é um registro fiscal e não comprova movimentação física. Seu registro isolado não deve baixar o saldo nem encerrar a armazenagem. O enquadramento fiscal concreto continua com Natalina; este documento define apenas o efeito físico no WMS.

## AC02 Fiscal no piloto e integração posterior

**Base:** Q13 e Q17.d; apêndice que admite NOTAZZ em segunda fase.

**Proposta para a primeira fase:** emitir no NOTAZZ pelo procedimento existente e registrar/importar o documento autorizado no WMS. O pedido de saída pode ser aberto manualmente antes de existir a nota; um XML já emitido é vinculado como documento existente, sem provocar outra emissão. Preparar a integração automática para etapa posterior.

Um pedido pode ter várias notas. Cada documento aponta os itens e quantidades que cobre; a soma deve corresponder ao pedido inteiro. Enquanto faltar documento exigido ou houver rejeição, o pedido permanece reservado e não é concluído. Cancelar uma nota não comprova retorno físico.

O WMS consolida os serviços e fornece demonstrativo para lançamento no ESL, responsável pela NFS-e. A entrega manual é a proposta inicial; o formato da futura integração não impede definir o negócio. Não supor que o ESL aceite um arquivo ainda não testado. As condições fiscais e autorizações dos documentos permanecem verificadas no sistema emissor.

## AC03 Recebimento em partes e alcance da quarentena

**Base:** Q03, Q04, Q05, Q19 e Q20. Uma nota pertence a um único pedido de entrada; esse pedido pode reunir várias notas. A entrada é efetivada de uma vez, após a resolução das divergências.

**Proposta:** usar o pedido de entrada como limite da carga sob conferência. Registrar cada chegada, nota e quantidade física, mas manter o pedido inteiro indisponível enquanto houver falta, sobra ou avaria. Não bloquear outros pedidos sem relação com essa carga. Essa escolha conservadora respeita a resposta de colocar toda a carga em quarentena e evita inventar liberação parcial.

O supervisor registra a solução acordada com o cliente. Se foram previstas 100 unidades e aceitas 98, preservar os dois números e o motivo da diferença; efetivar somente as 98 existentes. Não criar as duas faltantes para coincidir com a nota. A liberação operacional não substitui a regularização fiscal necessária.

Para recebimento manual normal, exigir cliente, armazém, identificação da nota, SKU e quantidade. Para estoque inicial excepcional, registrar o que existe e as informações faltantes, com autorização do supervisor e tratativa com o cliente. Não inventar número de nota, data FIFO ou valor; unidades sem dados indispensáveis à saída ficam identificadas e bloqueadas até regularização.

## AC04 Contagem de diárias e ocupação cobrável

**Base:** Q14 a Q17 e RN21. A cobrança começa no endereçamento, usa dias de calendário, não cobra entrada e saída no mesmo dia, considera pico diário e não é interrompida por reserva, separação ou quarentena posterior.

**Convenção proposta para tornar essas regras calculáveis:**

1. Iniciar o período de armazenagem no primeiro endereçamento, não na data usada pelo FIFO.
2. Incluir a data inicial e excluir a data da saída física. Assim, segunda até terça gera uma diária; segunda até segunda gera zero.
3. Para cada dia, apurar o maior número simultâneo de posições equivalentes cobráveis, por cliente, armazém e regra de preço. Considerar apenas permanências que geram diária naquele dia. Não somar todos os endereços visitados.
4. Manter a equivalência de ocupação ao levar a unidade para separação ou quarentena, pois Q16 manda continuar cobrando. A posição física liberada volta à disponibilidade; a equivalência usada no faturamento continua ligada à mercadoria.
5. Aplicar a tarifa vigente em cada dia. Não aplicar uma tarifa nova a dias anteriores.

Posição equivalente significa a medida de espaço atribuída à unidade na armazenagem. Não é uma segunda posição física. Uma unidade que ocupava uma posição continua representando uma posição cobrável enquanto aguarda retirada na separação. Se outra unidade ocupar o endereço liberado, há duas permanências cobráveis, embora apenas uma esteja naquele endereço. O demonstrativo precisa explicar essa diferença.

Um simples remanejamento preserva a equivalência. Uma alteração real de volume ou unitização que aumente o espaço necessário deve registrar a nova equivalência e seu momento, sem reiniciar o período. Retirar parte de um pallet não reduz a posição cobrável se o restante continua ocupando o mesmo espaço; manter unidades adicionais armazenadas em outro espaço aumenta a ocupação correspondente. A avaria pode suspender parte dessa equivalência conforme AC08.

| Exemplo documental | Resultado pela convenção proposta |
| --- | --- |
| Uma posição, endereçamento segunda e retirada terça | Uma posição-dia, referente à segunda |
| Endereçamento e retirada na segunda | Zero armazenagem; entrada e saída continuam possíveis |
| Uma posição, segunda até quarta, com três mudanças de endereço | Duas posições-dia, uma por dia cobrável |
| Unidade já armazenada vai à separação na segunda e sai quarta | Continua cobrável até terça; quarta não gera diária |
| A fica numa posição de segunda a terça; B usa essa posição de terça a quarta | Uma posição-dia na segunda e outra na terça |
| Unidade nova entra e sai no mesmo dia em que existe outra permanência cobrável | A passagem no mesmo dia não aumenta o pico faturável |

A combinação de pico com isenção no mesmo dia não está detalhada no Word. O tratamento acima é uma escolha de desenho explícita, não a alegação de que o contrato já definiu essa fórmula. Os exemplos servem para validar o primeiro demonstrativo com o gestor, sem reabrir um questionário abstrato. Pico físico do armazém e pico cobrável devem ter nomes diferentes nos relatórios.

## AC05 Uma unidade em duas posições

**Base:** Q06.a admite expressamente uma unidade grande em duas posições; Q14.c nega um cenário perguntado de forma composta, envolvendo também compartilhamento.

**Interpretação proposta:** prevalece a resposta específica sobre a capacidade física. Uma posição não recebe duas unidades, mas uma unidade grande pode ocupar duas posições. Reservar e liberar ambas juntas. As posições devem formar um conjunto compatível com o tamanho da unidade, indicado no cadastro operacional, sem presumir que quaisquer duas posições servem.

**Cobrança proposta:** essa unidade representa duas posições de armazenagem. Entrada e saída continuam seguindo a unidade de cobrança cadastrada para cada serviço, sem dobrar automaticamente suas taxas. Essa escolha resolve o desenho; o valor resultante aparece explicitamente na validação do demonstrativo.

**Implementação D17/BE08:** capacidade configurável, conjunto explícito e ocupação conjunta foram preparados no [documento 22](22-enderecamento-movimentacao-e-estoque.md), com [testes locais](23-validacao-estoque-backend.md). Datas e equivalência de AC04/AC05 ficam registradas, sem cálculo/cobrança. Isso não transforma as propostas em aprovação do gestor nem confirma capacidade real dos endereços.

## AC06 Tabelas, mínimo e valores comerciais

**Base:** Q14, Q15 e apêndice de preços. A Tigre é um exemplo de tabela, não comprovação dos preços contratados para Osasco.

**Proposta:** cadastrar por cliente, armazém, serviço e vigência a unidade de cobrança, o preço e os parâmetros aplicáveis. Permitir valor por unidade, posição-dia, tipo de veículo ou contêiner, e percentual quando contratado. A tela do cliente permite consultar/editar sua tabela sem eliminar o histórico independente.

O faturamento mínimo é um ajuste do ciclo, não um valor a somar integralmente aos serviços. Quando o subtotal dos serviços abrangidos for menor que o mínimo, acrescentar apenas a diferença. Quais serviços entram nessa comparação e eventual proporcionalidade de ciclo incompleto são parâmetros do contrato. Ausência de configuração não equivale a zero, nem permite copiar automaticamente o exemplo da Tigre.

Para GRIS, manter percentual, base de valor e periodicidade explícitos. O Word mostra o percentual de um exemplo, mas não fornece sua fórmula completa. Enquanto esses parâmetros não existirem para o cliente, não calcular esse item automaticamente. Isso delimita o cadastro sem inventar uma condição comercial.

Na primeira fase, o WMS entrega valores de serviços e memória de cálculo; o ESL segue responsável pela emissão da NFS-e e pelo tratamento tributário validado pela Controladoria. Não reproduzir alíquotas das imagens como padrão nem calcular os mesmos tributos duas vezes.

Esses são dados para configurar o cliente antes de faturar, não perguntas necessárias para continuar desenhando o projeto.

## AC07 Fechamento sem repetir dias

**Base:** Q17. Cada cliente possui seu período e o próximo cálculo respeita o último corte.

**Interpretação proposta:** quando o contrato usar dia fixo, como 24, o ciclo vai do dia 24, inclusive, até o dia 24 do mês seguinte, exclusive. O próximo ciclo começa nesse segundo dia 24. A expressão geralmente 30 dias é tratada como referência a ciclo mensal, não como obrigação de deslocar a data de corte a cada mês.

Um contrato que use efetivamente intervalos de 30 dias pode ter essa modalidade cadastrada. A mesma data nunca pertence a dois fechamentos. O fim do ciclo divide a cobrança, mas não encerra a permanência da mercadoria.

Antes da emissão da NFS-e, permitir ao gestor reabrir, corrigir e recalcular, preservando a versão anterior e exigindo nova aprovação. Depois da emissão, fazer ajuste identificado no ciclo seguinte. O gestor aprova ou rejeita o fechamento inteiro. Preservar histórico; não criar rotina de exclusão automática baseada em prazo presumido.

## AC08 Avaria e responsabilidade pela cobrança

**Base:** Q04.e, Q16.d, Q18.d e Q20. Avaria bloqueia expedição, sai do indicador de valor e pode interromper cobrança quando atribuída à Rodogarcia.

**Proposta:** registrar condição física, quantidade afetada, data da ocorrência e responsabilidade separadamente. O supervisor registra a ocorrência e a tratativa; o gestor valida o efeito financeiro. Essa autorização por perfil não exige uma pessoa diferente quando o próprio gestor executa a ação.

Interromper a armazenagem da parte atribuída à Rodogarcia a partir da data de ocorrência reconhecida. Se a responsabilidade só for reconhecida depois, recalcular período ainda não faturado ou lançar ajuste no próximo ciclo. Não cancelar automaticamente serviços de entrada e movimentação já prestados.

Se apenas 20 de 100 unidades de produto de um pallet forem afetadas, a proposta é suspender 20% da equivalência cobrável desse pallet, preservando o motivo do rateio. Esse rateio financeiro não libera 20% de um endereço para ocupação por outra unidade. A quantidade avariada sai do indicador financeiro informado em Q18, mas continua no controle físico até ter destino registrado.

## AC09 Serviços manuais sem duplicidade

**Base:** Q14, Q23 e RN09. Os serviços podem ser lançados manualmente; supervisor registra e gestor libera cobrança.

**Interpretação proposta:** registrar um único lançamento por serviço efetivamente prestado. O sistema pode sugerir entrada ou saída com base no fato operacional, e o supervisor informa/confere a quantidade; essa sugestão e o lançamento manual não viram duas cobranças. Entrada fica elegível após endereçamento; saída, após conclusão; serviços adicionais, após execução e liberação do gestor.

Para um serviço executado durante a permanência, registrar imediatamente cliente, unidade, nota de origem, data, quantidade e responsável. Usar o pedido de entrada existente como referência inicial; associar também ao pedido de saída quando ele existir. Não criar saída fictícia e não deixar de registrar serviço aguardando retirada futura.

Q23.c parece inverter entrada e saída ao afirmar que a entrada ainda não existe para mercadoria já armazenada. A proposta acima resolve a inconsistência preservando RN09 e a rastreabilidade exigida. Se houver várias notas de origem, discriminar as parcelas do serviço por nota, sem repetir seu valor total em todas elas.

## AC10 DUN, retiradas parciais e etiquetas

**Base:** Q01, Q02, Q07, Q21 e apêndice de etiquetas.

**Interpretação proposta:** o cadastro DUN informa embalagem e quantidade de produto; o ID da unidade logística identifica o pallet ou bobina físico. Não usar um código de configuração de embalagem como identidade de todas as unidades iguais.

Controlar a quantidade na unidade de medida do produto. A embalagem informa sua conversão. Para itens contados, não permitir fração de uma unidade indivisível; para produtos medidos por peso, usar a precisão cadastrada e a quantidade da nota. O exemplo de caixas permite retirar 100 de um pallet de 500, mantendo 400 e a mesma origem.

**Proposta de movimentação:** reservar a quantidade solicitada e bloquear a unidade física para outra separação simultânea até finalizar a retirada parcial. Depois, liberar o restante elegível. Se formar uma nova unidade física para a parte separada, gerar um novo ID ligado à origem. Preservar o ID do pallet remanescente e reimprimir sua etiqueta com a quantidade atualizada; a etiqueta anterior deve ser substituída para evitar leitura ambígua.

Reagrupar somente mesmo cliente, SKU, lote e data FIFO. Como proteção adicional da primeira versão, exigir também a mesma nota de origem: a etiqueta mostra uma nota e a saída é vinculada a ela. A restrição à mesma nota é proposta, não proibição literal recebida. Quando o produto não tiver lote, o dado deve estar explicitamente indicado como não controlado, sem inventar um lote comum.

O pedido de saída é atendido integralmente. Para mudar sua quantidade, cancelar e criar outro, observando os efeitos fiscais e físicos. Dividir o conteúdo do pallet não autoriza atender apenas parte do pedido.

## AC11 Permissões e ordem de seleção

**Base:** Q08 e Q20. Qualquer usuário pode justificar; supervisor autoriza exceções; gestor possui acesso geral.

**Interpretação:** o operador registra a necessidade de escolher uma nota/unidade fora do FIFO; supervisor ou gestor confirma a exceção. O supervisor pode justificar e confirmar sua própria ação, porque não foi exigida aprovação por uma segunda pessoa. Isso concilia as duas respostas sem nova pergunta.

**Proposta de desempate:** data FIFO mais antiga, nível mais baixo, sequência de coleta cadastrada para o armazém e, por fim, ID da unidade para estabilidade. A sequência operacional representa proximidade; não alegar cálculo de distância física com base apenas em códigos como A101. Na ausência de sequência, usar ordenação de endereços como alternativa simples, identificada como tal.

Gestor acessa o conjunto da operação. Supervisor e Operação atuam nos armazéns/clientes atribuídos aos seus usuários. Gestor configura cadastros, fiscal e valores; Supervisor executa liberações e correções operacionais; Operação recebe, confere, endereça, separa e imprime etiquetas. Atribuições de acesso são configuração inicial, não um novo questionário.

## AC12 Inativação com estoque remanescente

**Base:** Q24 impede novas entradas e saídas, mas admite continuidade necessária para o estoque existente.

**Proposta de conciliação:** bloquear novos pedidos e não permitir inativação definitiva enquanto houver estoque, pedidos abertos ou cobrança pendente. Nesse caso, sinalizar encerramento pendente e manter o cadastro ativo até concluir os compromissos; a baixa definitiva do cadastro ocorre depois.

Durante o encerramento, o gestor pode autorizar uma operação identificada para resolver o saldo remanescente, sem apagar histórico. Consultas, correções justificadas e fechamento continuam acessíveis. Essa transição é uma proteção proposta para não aprisionar mercadoria nem liberar saídas silenciosamente em cadastro já inativo. Transferência entre armazéns continua fora da primeira versão.

## AC13 Contagem e ajustes

**Base:** Q19 e Q20. Contagem física manual, leitura das etiquetas para comparar quantidades, supervisor com poder de correção e tratativa com cliente.

**Proposta:** oferecer uma contagem simples com quantidade esperada, contada e diferença; não assumir um módulo completo de inventário rotativo. Depois de uma entrada concluída, corrigir por ajuste identificado, com motivo, evidência disponível, supervisor responsável e referência à tratativa. Nunca editar silenciosamente a nota original para esconder uma diferença.

Se o ajuste afetar quantidade reservada, sinalizar e bloquear a saída até recompor ou cancelar o pedido. O ajuste precisa conciliar disponibilidade e reserva na mesma operação, sem deixar saldo negativo. Descarte ou devolução física exige também registro de destino, não apenas redução de um número.

## AC14 Falha de coletor e indisponibilidade geral

**Base:** Q26 manda reimprimir etiqueta ou aguardar coletor; Q27 permite planilha quando rede ou sistema parar.

**Interpretação:** são situações diferentes. Na falha isolada do coletor, a atividade afetada aguarda um equipamento funcional. Na indisponibilidade geral, aplicar a contingência por planilha, sob coordenação do supervisor.

**Procedimento proposto:** registrar identificador único do movimento, data/hora, operador, cliente, armazém, pedido/nota, unidade, quantidade, origem e destino. Manter uma única relação coordenada das unidades já comprometidas, evitando que duas equipes prometam o mesmo saldo. Ao retornar, conferir fisicamente, lançar em ordem, marcar os registros conciliados e investigar conflitos. A mesma linha não pode gerar dois movimentos.

O aplicativo inicial depende de conexão. A planilha é uma contingência operacional; não implica um aplicativo capaz de sincronizar movimentações automaticamente sem rede. Cobertura, impressão e leitura devem ser verificadas no local com Mickael.

## AC15 Histórico e recuperação

**Base:** Q17.e e Q27. Manter histórico, backups e suportar até 36 horas de parada; Lucas responde por TI e Caio pela operação.

**Proposta:** tratar 36 horas como limite operacional informado, buscando recuperar antes disso. Não interpretar o número como permissão para perder 36 horas de movimentos. A meta de desenho é recuperar os movimentos confirmados ou reconstruir de forma verificável o que faltar, usando registros de contingência quando aplicável.

A frequência dos backups, a possibilidade de recuperação até determinado instante e o prazo efetivamente alcançável dependem do ambiente SQL Server e de um teste de restauração com Lucas. São verificações técnicas, não regras que precisam ser adivinhadas pelo gestor. Registrar a capacidade comprovada antes do piloto; backup existente não equivale a restauração já testada.

Não excluir automaticamente documentos ou histórico por prazo inventado. A política de retenção pode ser configurada depois de estabelecida pela empresa, preservando a auditabilidade.

## AC16 Piloto e dados de implantação

**Base:** Q25 a Q27 e apêndices. Primeiro Osasco, depois Castro/Tigre; Caio valida a operação. Dez usuários, quatro simultâneos, são estimativas. A data desejada é 13/10/2026.

**Proposta de recorte:** cadastros essenciais; importação de endereços; entrada manual/XML; conferência e quarentena; etiquetas; endereçamento; estoque; FIFO/reserva; saída integral com eventual retirada parcial de pallet; registro fiscal; serviços e demonstrativo de cobrança; perfis e histórico. Transferência entre armazéns fica fora, como respondido. Integrações automáticas, mapa elaborado e inventário completo podem vir depois.

Conferir razão social/CNPJ no cadastro para resolver a grafia Brasel/Bracel. Levantar estoque inicial somente se existir; não criar mercadoria inicial por suposição. Limites de ocupação, embalagens, preços, dia de corte e eventual antecedência de aviso de validade são parâmetros do cliente. A validade gera aviso conforme a resposta, não bloqueio automático adicional.

A data de 13/10 é uma necessidade informada, não compromisso de entrega demonstrado. O plano deve considerar equipe, disponibilidade e validação do percurso completo. A ausência de volumetria pode ser suprida por medição no piloto e ensaio de capacidade; não inventar volume para afirmar desempenho.

## O que efetivamente precisa vir de fora

| Dado ou verificação | Responsável de referência | Momento em que se torna necessário |
| --- | --- | --- |
| Preços do cliente, unidade de cada serviço, mínimo e composição, eventual GRIS e corte | Gestor/comercial | Configurar e conferir cobrança real |
| Documentos fiscais de exemplo e tratamento da operação | Natalina/Controladoria | Validar o percurso fiscal do piloto |
| Impressora, etiqueta, coletor e cobertura real | Mickael e TI | Testar leitura e impressão no armazém |
| SQL Server, ambientes, acessos e recuperação comprovada | Lucas | Preparar ambiente e liberar operação |
| Cadastro do cliente, endereços, eventual estoque inicial e validação do percurso | Caio com a operação | Preparar e aprovar o piloto |

Esses dados não bloqueiam o estudo da arquitetura. O que não se pode obter por raciocínio é um preço contratado, uma condição tributária específica ou a capacidade real de um equipamento. O desenho já prevê onde essas informações entram, sem demandar outra rodada extensa de perguntas.

## Como validar sem reabrir o levantamento

Apresentar um percurso completo com exemplos: recebimento divergente, liberação, duas posições quando necessário, retirada parcial de pallet, nota emitida antes da coleta, cancelamento e fechamento. Usar os resultados propostos acima como referência concreta para Caio e gestor apontarem somente exceções à rotina real.

Os três pontos de maior impacto para essa conferência são o momento da baixa, a medida de posições cobráveis e o cálculo de diárias. Eles já têm solução proposta, mas seus resultados financeiros e operacionais devem ser conferidos antes do uso real. Até lá, permanecem identificados como interpretações/propostas, sem serem apresentados como respostas literais do gestor.

A arquitetura segue React com TypeScript, Java com Spring em camadas MVC convencionais e SQL Server. Este trabalho define negócio e responsabilidades; não implementa aplicação nem altera o banco.
