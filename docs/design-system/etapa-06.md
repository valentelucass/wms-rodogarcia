# Listas, detalhes e ações por registro — FE02-REG01

Pedido expresso de Lucas em 09/10/2026. Esta entrega reorganiza as 12 áreas e suas 50 páginas existentes. Substitui as abas que separavam consulta, criação e comandos dentro de cada página. Mantém módulos e etapas reais da jornada, os contratos backend e a fundação visual de [design.md](design.md) e [etapa-03.md](etapa-03.md).

**Complemento FE05-PED01:** Entrada e conferência passou a reunir seus quatro grupos de capacidades na única visão Pedidos de entrada, com seções internas. A contagem de 50 páginas/171 ocorrências abaixo registra o inventário original de REG01, preservado para rastrear comandos; não representa quatro etapas globais atuais de Entrada. [Comportamento atual](etapa-07.md).

## Lote na etiqueta — FE06-LOTE01

Pedido de Lucas em 20/10/2026. A etiqueta exibe o lote da unidade com prefixo `L:`, após a descrição do produto, na tela e na mídia de impressão. Fonte visual destacada e quebra de linha para identificadores extensos. Lote não informado e produto sem controle recebem textos distintos; o frontend não cria um lote nem usa SKU ou UUID como substituto. O DTO já fornecia os dados, sem nova API ou alteração de estoque.

Casos de lote real, ausência e valor textual zero conferidos nos testes; consulta/reimpressão conserva o UUID dos códigos de barras/QR. Comparação visual usa a fonte anterior `645153b6033775925f906dbd81aee1a619008bc4` e os mesmos dados fictícios. [Antes](../../orchestracao/.runtime/faturamento-etiqueta-20261020/label-visual/before-1440.png), [depois](../../orchestracao/.runtime/faturamento-etiqueta-20261020/label-visual/after-1440.png) e [conferência](../../orchestracao/.runtime/faturamento-etiqueta-20261020/label-visual/visual.json). Mídia de impressão de 150 mm; sem envio a impressora física.

## Contingência: dicas e identificação — FE12-CONTING-AUTO01

Pedido expresso de Lucas em 20/10/2026, pela imagem de Registrar ocorrência. Dicas abertas dentro de um diálogo nativo passam a usar esse diálogo como destino do portal. Permanecem na mesma camada visual e acessível do formulário; fora de modal continuam no corpo da página. Escape encerra somente a dica, mantendo cadastro e foco. A posição, fechamento por fora, rolagem e interação por mouse/teclado/toque conservam o componente compartilhado.

Identidade do fato e Fonte deixam de ser campos de preenchimento. Cada novo rascunho recebe `CONT-UUID`, independente da chave HTTP e de identidade anterior no contexto. A identidade se mantém ao editar, trocar tipo, voltar da conferência, receber erro e repetir a solicitação salva. Nova ocorrência recebe outra identidade. A identidade permanece legível na confirmação/resultado para uso nas dependências; registros anteriores mantêm sua identificação original.

Fonte ausente é preenchida com a descrição factual Registro manual na tela de contingência do WMS.; fonte válida de contexto já preparado é conservada. O contrato backend continua validando e registrando ambos os valores, inclusive no hash; nenhuma alteração de API, schema, permissões, dependências ou execução da conciliação. Ajuda de cadastro e dependências em `information.ts` acompanha o comportamento atual.

Conferência local: 25 testes em quatro arquivos, incluindo estabilidade de identidade/reenvio/novo registro e portal/Escape, tipagem/lint/formatação e build próprio aprovados. Revisão independente favorável. Chrome fictício em oito layouts (CHEGADA/CONTAGEM, claro/escuro, 390/1440 px), 48 interações de dicas e quatro envios em memória; sem overflow, erro de console ou acesso API/externo. Dicas verificadas acima do modal com `elementFromPoint`, foco/hover/clique, Escape preservando cadastro/foco e alvos de toque. Conferidos voltar/conflito/reenvio/novo rascunho e precisão de IDs/decimais/instantes. [Antes](../../orchestracao/.runtime/contingency-fixes-20261020/antes.png), [depois](../../orchestracao/.runtime/contingency-fixes-20261020/depois.png), [dica visível](../../orchestracao/.runtime/contingency-fixes-20261020/depois-dica.png), [recibo](../../orchestracao/.runtime/contingency-fixes-20261020/validacao.json) e [visual](../../orchestracao/.runtime/contingency-fixes-20261020/visual.json). Fonte pronta; pacote de conferência separado da versão ativa, não medida. Sem SQL/backend, reinício operacional ou publicação.

Graphify consultado antes do código. A atualização AST foi executada sem LLM/force e recusada pela guarda ao propor redução de 15.220 para 14.591 nós; o grafo histórico manteve hash/data. [Limite da ferramenta](../../orchestracao/.runtime/contingency-fixes-20261020/graphify-update.json); não representa atualização aceita do mapa.

## Alertas das operações — FE02-ALERTAS-UX01

Pedido de Lucas em 16/10/2026, pela imagem do alerta de saldo insuficiente. Erros de `OperationFeedback` passam a ter fundo suavemente tonalizado, faixa lateral de 3 px e ícone; a mensagem original usa texto neutro. Código e correlação ficam completos em Detalhes técnicos, inicialmente recolhidos, com orientação de suporte central em `information.operationErrorDetails`. Identificadores extensos quebram dentro do bloco e continuam disponíveis para cópia. No celular, dados técnicos empilham e o controle tem área de toque de 44 px.

Resultado desconhecido usa âmbar e conserva sua orientação sempre visível, fora dos detalhes. Validação mantém a indicação do campo e sessão expirada orienta entrar novamente, sem referência obsoleta à integração pendente. Foco automático e região acessível são mantidos; somente a borda azul após interação de mouse deixa de ser forçada. Teclado conserva o foco visível. Não há fechamento automático do alerta, reenvio ao ler detalhes ou alteração de permissões/comandos/bloqueios. Estilo limitado aos erros do componente compartilhado; recibos de sucesso e outros avisos não são remodelados neste recorte.

25 testes em seis arquivos, três novos, tipagem/lint/formatação/build aprovados. Seis layouts Chrome fictícios claro/escuro em 390/768/1440 px, além de incerteza, sessão, validação e texto extenso no celular. Conferidos Enter/Space, diagnóstico exato, foco, área de toque, ausência de overflow/reenvio e formulário preservado. [Galeria](../../orchestracao/.runtime/operation-alerts-20261016/galeria.html), [recibo](../../orchestracao/.runtime/operation-alerts-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, operação real, reinício ou publicação; versão ativa não aferida.

## Formulário do pedido de saída — FE09-PEDIDO-UX01

Pedido de Lucas em 16/10/2026, a partir do formulário com molduras aninhadas e rótulos repetidos de Itens. `DispatchOrderFields`, usado somente na criação da saída, separa cliente/armazém, lista de produtos e justificativa. Cada item tem um cartão simples, número, remoção vermelha no cabeçalho e campos Produto / SKU e Quantidade. Adicionar item fica no cabeçalho da lista. A orientação de quantidade aparece uma vez; justificativa ocupa a largura toda e tem altura inicial de 112 px. No celular, campos empilham e inclusão ocupa a linha. Cabeçalho e confirmação permanecem acessíveis durante a rolagem.

Metadados, seletores oficiais, alcance por cliente, valores iniciais, contexto fixo e estado do formulário são preservados. Inclusão/remoção alteram a mesma lista original; não convertem IDs nem decimais. Código segue automático, conferência precede envio, lista vazia é recusada e backend continua responsável por disponibilidade e atendimento integral. Orientações em `information.dispatchOrder`/`dispatchOrderQuantity`. Sem alterar o renderer das outras listas.

12 testes em quatro arquivos, três novos, tipagem/lint/formatação/build aprovados. Seis layouts Chrome fictícios claro/escuro em 390/768/1440 px conferiram alinhamento, empilhamento, ausência de overflow, toque, justificativa e rolagem. Inclusão por teclado, remoção do primeiro item, retorno da conferência, precisão/IDs, envio único e conflito sem perda de dados também conferidos. [Galeria](../../orchestracao/.runtime/dispatch-order-form-20261016/galeria.html), [recibo](../../orchestracao/.runtime/dispatch-order-form-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, pedido real, reinício ou publicação; versão ativa não aferida. Jornada Playwright antiga ajustada somente nos seletores deste formulário, sem alegar execução integral.

## Texto longo nas células — FE02-TABELA-TEXTO01

Pedido de Lucas em 16/10/2026 pela imagem do Relato de avaria ocupando muitas linhas. Tabelas compartilhadas de `Result` apresentam os valores em uma linha e limitam o texto com reticências, sem cortar o conteúdo armazenado. Só valores que excedem a largura disponível oferecem leitura por clique/Enter/Space, com indicação visual discreta e nome acessível do campo. Textos curtos continuam sem controle adicional. O popup mostra a string apresentada integralmente, com quebras de linha, seleção para cópia, rolagem própria e fechamento acessível. Referências, datas e quantidades mantêm a formatação existente; a mudança não modifica o DTO.

`TableCellText` recebe o valor final de `ReferenceValue` pelo modo `compact`, usado somente nas células de `ResultTable`. Medição acompanha a largura disponível. Texto longo permanece no DOM da célula; reticências são de apresentação. Troca do texto remontará a célula e encerrará seu popup anterior. Conteúdo é renderizado como texto, inclusive quando contém marcação HTML. Ajuda e rótulo em `information.ts`.

Popup usa o diálogo existente em portal próprio, com cabeçalho acessível na rolagem e área de toque no celular. Tab/Shift+Tab permanecem na leitura. Cancelamento do diálogo interrompe propagação para que Escape encerre apenas a camada atual; fechamento devolve foco ao texto de origem. Ler não seleciona nem altera o registro. Ver registro e Selecionar registro mantêm os callbacks, objetos e bloqueios anteriores.

27 testes em cinco arquivos, três novos, tipagem, lint/formatação/build e seis layouts Chrome fictícios claro/escuro em 390/768/1440 px aprovados. Conferidos linhas compactas, reticências, texto completo exato/HTML literal, teclado/foco, troca de conteúdo, bloqueio da seleção durante atualização e ausência de overflow fora da tabela/popup. [Galeria](../../orchestracao/.runtime/table-text-popup-20261016/galeria.html), [recibo](../../orchestracao/.runtime/table-text-popup-20261016/validacao.json). Preview próprio encerrado; sem API/backend/SQL reais, reinício ou publicação; versão ativa não aferida.

## Espaço antes da continuação — FE02-CONTINUAR-ESP01

Pedido de Lucas em 16/10/2026 pela imagem de Confirmar reserva da sugestão do servidor encostado na seção Seleções. O botão compartilhado recebe `operation-continue`; somente quando filho direto de `.results`, margem superior de `--og-space-4` (16 px) o separa do conteúdo anterior. Ações embutidas da etiqueta e do bloco Próximo passo da aprovação mantêm seu layout. Cor, título, disponibilidade e continuação permanecem iguais.

11 testes existentes em três arquivos, tipagem, lint e formatação aprovados. Quatro layouts Chrome fictícios claro/escuro em 390/1440 px conferiram gap de 16 px, ausência de overflow, toque e callback de reserva por Enter. [Prévia](../../orchestracao/.runtime/operation-continue-gap-20261016/reserva-dark-1440.png), [medidas](../../orchestracao/.runtime/operation-continue-gap-20261016/visual.json). Preview próprio encerrado; sem API/backend/SQL reais, reinício ou publicação. Fonte local; versão ativa não aferida.

## Resultado da aprovação — FE11-APROVACAO-UX01

Pedido de Lucas em 16/10/2026. O resultado de Aprovar ciclo integral passa a usar `BillingApprovalResult`: período/contexto, versão/saldo/motivo e ajustes separados; entregas, confirmações, NFS-e/resolução financeira e identificadores completos disponíveis em áreas recolhidas. Cabeçalho acessível, sem duplicação do registro selecionado após o resultado. Selecionar ciclo/versão identifica o alvo; entrega manual aparece em bloco Próximo passo, com a ação existente. Resultado anterior em atualização bloqueia seleção/continuação; repetição histórica continua sem próximo passo.

`Result` apresenta o DTO `FechamentoCobrancaDto.Ajuste` e sua lista válida por `BillingAdjustments`, substituindo a tabela extensa por cartões. Origem/destino e cálculos em duas colunas, três valores em faixa própria, motivo/evidência com largura inteira e aplicação identificada. Hash completo, IDs e instantes originais ficam sob demanda; celular empilha tudo. Precisão, zero, crédito, ausência e objetos/tipos originais de seleção preservados. Nenhuma aritmética ou leitura adicional. Referências já recebidas são indexadas no escopo de apresentação; histórico mantém os DTOs e controles existentes. Ajuda em `information.billingApprovalResult`/`billingAdjustments`.

27 testes direcionados em oito arquivos, três novos para conteúdo/seleção/continuação, atualização/repetição e zero/ausência/crédito/histórico. Suíte completa: 770 testes em 94 arquivos aprovados. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, sem rolagem horizontal nos ajustes, hash por teclado, referência da versão recebida, seleção original, envio único, histórico/série zero e ações adaptadas conferidos. [Galeria](../../orchestracao/.runtime/billing-approval-20261016/galeria.html), [recibo](../../orchestracao/.runtime/billing-approval-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, aprovação real, reinício operacional ou publicação; versão ativa não medida.

## Formulário da reabertura — FE11-REABRIR-UX01

Pedido de Lucas em 16/10/2026. `ReopenCycleFields` organiza `FechamentoCobrancaController.reabrir` em versão/cálculo, justificativa e resolução financeira. Campos Versão atual do ciclo/Cálculo de referência têm largura útil; motivo ocupa a linha e tem altura inicial de 104 px. Cabeçalho identifica o ciclo pelo título de negócio/período e traduz localmente EM_REVISAO para Em revisão. Ícone de fechamento, cabeçalho e confirmação acessíveis na rolagem; celular empilha a grade.

`HistoricalResolutionField`, extraído do cálculo para uso nos dois formulários, preserva `ResolutionReferences`, metadados e `ScalarField`, com rótulos curtos, campos alinhados e remoção vermelha em linha própria de altura normal. Não muda políticas, IDs, revisão, número atual, cálculo, motivo, confirmação ou repetição. O backend continua responsável por criar a próxima versão. Ajuda em `information.reopenCycle`; estilos próprios em financeiro.

46 testes direcionados em oito arquivos, dois novos, tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, sem overflow, remoção compacta, motivo legível, retorno, IDs/revisão/número exatos, repetição original após resposta desconhecida, conflito, restrição ao Gestor, teclado/toque e regressão visual do cálculo conferidos. [Galeria](../../orchestracao/.runtime/reopen-cycle-20261016/galeria.html), [recibo](../../orchestracao/.runtime/reopen-cycle-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reabertura real, reinício operacional ou publicação; versão ativa não medida.

## Formulário do cálculo — FE11-CALC-UX01

Pedido de Lucas em 16/10/2026. `CalculationFields`, específico de `CalculoCobrancaController.calcular`, agrupa cliente/armazém, período e justificativa. Orientação curta do fim exclusivo no catálogo `information.serviceCalculation`; justificativa em largura inteira e altura inicial de 104 px. Resolução financeira identifica a ação do Gestor, mostra referências consultadas e alinha Tipo de compromisso/Compromisso sem o caminho repetido. Remover resolução financeira fica em linha própria, vermelho e com altura normal; Informar resolução financeira mantém roxo. Referências do diálogo compactas, ícone de cobrança, cabeçalho/confirmação acessíveis na rolagem e grade de uma coluna no celular.

Metadados/seletores oficiais, campos fixos/bloqueados, política do Gestor, compromisso, período, motivo e confirmação explícita preservados. Sem cálculo no navegador. 44 testes em seis arquivos, dois novos para comando exato/retorno/remoção/reinclusão e perfil/conflito; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, altura do botão e justificativa, ausência de overflow, teclado/toque, bloqueio durante envio, envio único e conteúdo preservado conferidos. [Galeria](../../orchestracao/.runtime/calculation-form-20261016/galeria.html), [recibo](../../orchestracao/.runtime/calculation-form-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício operacional ou publicação; versão ativa não medida.

## Recuperação da anulação do fato — FE11-FATO-REC01

Pedido de Lucas em 16/10/2026. A recuperação de `FatoServicoController.anular` apresenta aviso amarelo de resultado ainda desconhecido, consulta turquesa e saída vermelha com rótulo curto. A situação consultada pertence ao fato e não confirma a anulação. Consulta anterior permanece identificada durante atualização e após falha; erro preserva a mensagem recebida. Saída mantém confirmação nativa e callback de resultado desconhecido. Rodapé duplicado é ocultado apenas enquanto essa recuperação está bloqueada. Cabeçalho permanece acessível na rolagem; celular empilha ações e campos. Orientação em `information.serviceFactRecovery`.

O detalhe de `FatoServicoDto.Fato` em `Result` usa `ServiceFactDetail`: serviço/situação, contexto, execução, vínculos e rateio em grupos próprios. Critério ocupa largura inteira; cotas mostram nota e valor diretamente, sem tabela/ação genérica para objeto sem identidade independente. Identificadores, revisão e instante original ficam recolhidos. Precisão textual, zero/ausências e identificação das referências mantidos; seleção do fato conserva objeto/tipo e bloqueio. Não há cálculo, novo GET automático ou alteração de comando/permissão.

Conferidos 52 testes direcionados em sete arquivos, incluindo quatro novos de consulta sem repetição, persistência da incerteza com ANULADO, erro/consulta anterior, repetição idêntica, saída explícita, cotas/zero e seleção. Suíte completa: 763 testes em 91 arquivos aprovados. Tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, toque de 44 px no celular, teclado, referência de nota/série zero e conteúdo exato conferidos. [Recibo](../../orchestracao/.runtime/service-fact-recovery-20261016/validacao.json), [galeria](../../orchestracao/.runtime/service-fact-recovery-20261016/galeria.html). Preview próprio encerrado; sem SQL/backend, reinício operacional ou publicação; versão ativa não medida.

## Confirmação do fato de serviço — FE11-FATO-CONF01

Pedido de Lucas em 16/10/2026. `OperationConfirmation` utiliza `ServiceFactConfirmation` somente em `FatoServicoController.registrar`, tanto no diálogo de registro quanto em confirmação própria. Contexto aparece uma vez; serviço/execução, vínculos, quantidade/valor, critério/cotas, justificativa e resolução ficam em grupos próprios. Critério e motivo recebem largura inteira e preservam quebras de linha. Campo vazio é apresentado como Não informado; zero mantém seu valor. Identificador da operação, ID do serviço e instante original ficam disponíveis sob demanda. Ajuda em `information.serviceFactConfirmation`; CSS próprio em financeiro. Foco inicial no retorno, cabeçalho e ações acessíveis e celular em uma coluna.

**Nome consultado pelo seletor:** a resposta de `EntityReferenceField` ficava restrita à instância editável, levando a Referência indisponível na confirmação. O seletor agora entrega ao leitor de apresentação somente a linha escolhida da resposta atual, dentro do mesmo escopo de acesso. O catálogo de opções continua vindo da consulta autorizada. Troca de escopo invalida a resposta do seletor ainda montado e provoca nova leitura; linha antiga não é republicada no novo escopo. Nenhuma cache global ou alteração do comando/ID foi criada.

33 testes direcionados em sete arquivos, incluindo quatro novos de regressão para nome persistido/retorno/envio único, escopo desmontado/aberto, ausência/zero e textos/instante exatos. Suíte completa: 759 testes em 90 arquivos aprovados. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, serviço escolhido após consulta, justificativa completa fora das ações, foco/retorno, quantidade/valor/IDs exatos, cotas com nota/série zero, resolução do Gestor, conflito e confirmação própria/embutida conferidos. Detalhes no [recibo](../../orchestracao/.runtime/service-fact-confirmation-20261016/validacao.json); [galeria](../../orchestracao/.runtime/service-fact-confirmation-20261016/galeria.html). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Registro do fato de serviço — FE11-FATO-UX01

Pedido de Lucas em 16/10/2026. `OperationForm` usa `ServiceFactFields` somente em `FatoServicoController.registrar`. Os campos existentes são distribuídos por cliente/armazém, serviço/execução, vínculos, quantidade/valor, rateio e justificativa/resolução. Serviço e referência ocupam largura inteira, origem fica junto à data, vínculos ficam em grupo próprio e critério/motivo recebem espaço para leitura. Cotas e resolução mantêm `Fields`; os demais campos mantêm `ScalarField`, metadados, seletores, precisão e bloqueios por perfil. Campos fixos/contexto continuam sob responsabilidade do formulário existente.

Rótulos de origem, data, referência e critério foram clarificados. Valores MANUAL/SUGESTAO permanecem nos comandos, apresentados como Manual/Sugestão. Orientação decimal única e ajuda em `information.serviceFact`/`serviceFactValues`. Referências compactas, ícone de serviços, cabeçalho e ação de conferência acessíveis na rolagem; celular empilha os campos. CSS limitado a `modules/financeiro/serviceFact.css`.

**Correção de preenchimento encontrada na conferência:** `billingReferences` copiava a chave calculada de uma sugestão para `referenciaExecucao`. O DTO recusa os dois-pontos dessa chave e `FatoServicoService.execucao` exige referência nula para ENTRADA/SAIDA. Sugestões agora inicializam `referenciaExecucao` como nula; chave e demais dados permanecem na sugestão, e execuções manuais conservam a referência informada. Fontes: `FatoServicoDto.Registrar` e `FatoServicoService.execucao/sugestoes`; backend e contrato preservados. Esta correção não comprova execução real no banco.

34 testes em dez arquivos aprovados, incluindo três regressões novas de entrada/saída com chave calculada e registro manual. Dois casos reproduziram a falha antes da correção. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, precisão decimal/IDs Long/instante, cotas por nota, confirmação explícita com um envio, retorno, carregamento, conflito com dados conservados e perfis Supervisor/Gestor/Operação conferidos. [Galeria](../../orchestracao/.runtime/service-fact-20261016/galeria.html), [recibo](../../orchestracao/.runtime/service-fact-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Consulta de sugestões de serviço — FE11-SUGESTOES-UX01

Pedido de Lucas em 16/10/2026. `OperationForm` aplica composição específica somente a `FatoServicoController.sugestoes`: orientação curta, cliente/armazém identificados, seletor de serviço e Consultar alinhados e paginação em seção recolhida acessível por teclado. Campos oficiais, filtros de serviços, valores atuais e metadados são preservados. Celular empilha campos e ação; estilos próprios em `modules/financeiro/serviceSuggestions.css`, orientação em `information.serviceSuggestions`.

`RecordDialog` compacta referências e usa ícone de serviços nesta ação. O banner de um fato selecionado anteriormente é omitido nesta consulta por serviço, evitando apresentar sua situação como resultado da consulta. A navegação ao detalhe anterior permanece disponível. Outros comandos e banners não mudam; cabeçalho permanece acessível durante rolagem.

Preservados GET, cliente/armazém/serviço, página/tamanho, permissões e bloqueio durante carregamento. Consultar não registra execução automaticamente. 19 testes existentes em seis arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Conferidos IDs Long exatos, paginação por Enter, alinhamento, toque, erro com serviço conservado, nova consulta, vazio, perfil Operação e ausência de escrita. [Galeria](../../orchestracao/.runtime/service-suggestions-20261016/galeria.html), [recibo](../../orchestracao/.runtime/service-suggestions-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Nota de origem agrupada — FE09-RESERVAS-UX03

Complemento de Lucas em 16/10/2026: o cartão melhorou, mas a nota de origem parecia perdida. A coluna da quantidade deixa de crescer proporcionalmente ao cartão; acompanha seu conteúdo até 240 px. Nota começa 24 px depois dessa coluna, mantendo os dados próximos da borda esquerda. Identificador continua em linha inteira abaixo; celular conserva empilhamento. Somente CSS, com ajuda `information.dispatchReservations` revisada e preservada.

Sete testes existentes em dois arquivos, formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Conferidos intervalo/alinhamento, quantidade extensa no desktop, reserva cancelada selecionada em 1280 px, teclado/toque, IDs/seleção/bloqueio e ausência de overflow. Sem novos testes para o ajuste visual. [Prévia](../../orchestracao/.runtime/dispatch-reservation-origin-20261016/reserva-cancelada-agrupada-dark-1280.png), [recibo](../../orchestracao/.runtime/dispatch-reservation-origin-20261016/validacao.json). Preview próprio encerrado; fonte local, sem SQL/backend, reinício/publicação ou aferição da versão ativa.

## Cartão da reserva — FE09-RESERVAS-UX02

Pedido de Lucas em 16/10/2026, complementando UX01 pela imagem do cartão selecionado. O cartão usa uma coluna externa; quantidade e nota ficam na primeira linha dos campos, enquanto o identificador completo da unidade ocupa a largura inteira abaixo, em fonte monoespaçada. Indicação Reserva selecionada fica no cabeçalho, junto da identificação e da situação recebida. Selecionar reserva permanece verde em rodapé com divisor e espaço próprio; celular empilha os campos e usa ação larga. A composição funciona também no diálogo estreito em desktop, sem depender somente da largura da janela.

Ajuda em `information.dispatchReservations` orienta a conferência dos três dados. Seleção mantém objeto/tipo, UUID no nome acessível, `aria-pressed`, foco e bloqueio anteriores; não registra expedição. Referências ausentes e unidade não informada continuam explícitas, sem inferir número de nota ou unidade de medida. Quantidades preservam precisão.

21 testes existentes em cinco arquivos, tipagem, lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Medidas conferem unidade em largura inteira e ação abaixo dos campos; teclado, filtro do pedido, seleção exata, valores grandes/fracionários/zero, ausências e bloqueios preservados. Diálogo desktop/celular também conferido. [Prévia](../../orchestracao/.runtime/dispatch-reservation-card-20261016/reservas-dark-1440.png), [recibo](../../orchestracao/.runtime/dispatch-reservation-card-20261016/validacao.json). Preview próprio encerrado; sem API/backend/SQL reais, reinício ou publicação; versão ativa não aferida.

## Reservas do pedido — FE09-RESERVAS-UX01

Pedido de Lucas em 16/10/2026. `DispatchReservations`, usado na jornada e no diálogo de saída/fiscal, apresenta cada reserva em cartão: situação recebida em badge, quantidade reservada em destaque, nota de origem e identificador completo da unidade em campos separados. Referência do pedido fica junto ao título. Botão verde Selecionar reserva mantém o UUID no nome acessível; reserva escolhida tem texto explícito, borda e `aria-pressed`. No celular, os campos e a ação ocupam a largura disponível. Estilos limitados ao componente em `modules/saida/dispatchReservations.css`; orientação em `information.dispatchReservations`.

Preservados `referenceCatalog`, associação da reserva ao pedido atual, callback com objeto/tipo originais, bloqueio existente, consulta da nota e formatação exata da quantidade. Selecionar apenas atualiza a referência do fluxo existente; não registra leitura, destino, retirada ou liberação automaticamente. Não presume unidade de medida ausente nem substitui referência indisponível por um nome inventado.

21 testes existentes em cinco arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px aprovados. Conferidos teclado/foco, objeto original, escolha atual, bloqueio, propriedade do pedido, nota/série zero, quantidade grande/fracionária/zero, UUID longo, ausências e composição no diálogo. [Galeria](../../orchestracao/.runtime/dispatch-reservations-20261016/galeria.html), [recibo](../../orchestracao/.runtime/dispatch-reservations-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Detalhe abaixo da linha — FE02-DETALHE-LINHA01

Pedido de Lucas em 16/10/2026, pela imagem em que o detalhe de endereço/limites abria na célula de ações, comprimindo os dados e aumentando a linha inteira. O padrão compartilhado `ResultTable`, interno a `Result`, passa a abrir o registro abaixo da linha, em célula que abrange todas as colunas. A linha de origem fica compacta. A expansão substitui o `<details>` dentro da coluna, preservando rótulos e callbacks das ações.

Botão roxo mantém `aria-expanded`/associação ao painel. Um detalhe permanece aberto por tabela; fechar devolve foco ao botão de origem. Mudança da coleção consultada não reaproveita uma expansão anterior. A região usa a largura visível do contêiner e acompanha a rolagem horizontal, permitindo ler os limites no celular sem ampliar o modal/página. Cabeçalho do diálogo permanece acessível enquanto houver detalhe expandido. Seleção verde mantém o objeto/tipo recebido; atualização desabilita seleção na linha e nos detalhes. A abertura/fechamento continua disponível para leitura.

Conferência direcionada: 23 testes em oito arquivos, cinco novos para expansão fora da coluna, colspan, limites, foco/troca, atualização dos registros, recarga e notas/série zero. Dois testes de indicadores passaram a abrir o painel antes de conferir unidades e saldos. Suíte completa: 752 testes em 88 arquivos aprovados. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios em 390/768/1024/1440 px claro/escuro, rolagem para ambos os lados, linha compacta, limites legíveis, toque de 44 px, teclado/fechamento/foco, seleção exata e consulta sem callback. Detalhes no [recibo](../../orchestracao/.runtime/result-row-detail-20261016/validacao.json); [galeria](../../orchestracao/.runtime/result-row-detail-20261016/galeria.html). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Encerramento da vigência — FE04-VIGENCIA-UX01

Pedido de Lucas em 16/10/2026, pela imagem de Encerrar vigência identificada com rótulos extensos e compromissos em botões consecutivos. `EndValidityFields` separa data do encerramento, resolução financeira histórica, identificação do compromisso e justificativa; rótulos curtos mantêm obrigatoriedade, tipo e metadados dos campos. Motivo ocupa a largura inteira. Cabeçalho e confirmação permanecem acessíveis na rolagem; celular empilha os campos.

`ResolutionReferences` passa a apresentar tipo e período/identificação em cartões, com Usar compromisso roxo e indicação da escolha atual. Rótulos acessíveis mantêm o contexto completo, sem alongar o texto visual do botão. Lista numerosa tem rolagem própria; ausência de referências tem orientação. Os campos de tipo/compromisso continuam disponíveis para seleção explícita. Descrição ausente usa o ID recebido, sem inventar um nome. Ajuda em `information.endValidity`/`resolutionReferences`.

O componente utiliza os campos existentes, a política do Gestor e os controles oficiais. Escolher cartão preenche somente `tipo`/`compromissoId`; não envia nem confirma automaticamente. Tipo da vigência, ID/revisão, corte, compromisso e motivo seguem no comando existente; vínculo histórico e autorização continuam sob validação do servidor.

Conferidos 21 testes em cinco arquivos, três novos para comando exato/retorno, conflito/bloqueio e perfil/ausências; tipagem/lint/formatação/build. Chrome com componentes oficiais e transporte fictício: oito layouts claro/escuro em 390/768/1024/1440 px sem overflow, campos/IDs HTML, seleção por teclado, toque de 44 px, confirmação explícita, 12 referências com busca, texto longo e ausência de referências. [Galeria](../../orchestracao/.runtime/end-validity-20261016/galeria.html), [recibo](../../orchestracao/.runtime/end-validity-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, encerramento real, reinício ou publicação; versão ativa não medida.

## Ações das tabelas gerais — FE02-TABELA-ACOES01

Verificação pedida por Lucas em 16/10/2026: a imagem com Disponível para saída ainda correspondia ao caminho genérico de `Result`, enquanto FE05-NOTAS-ACOES01 organizava somente as notas. O estilo compartilhado passa a usar `result-row-actions` em todas as tabelas de resultados: detalhes roxos, seleção verde, controles de mesma largura, textos sem quebra e coluna Ações. Notas mantêm seus rótulos acessíveis e largura específica. Demais rótulos, expansão nativa e seleção dos objetos originais preservados.

Conferidos 16 testes existentes em seis arquivos, tipagem/lint/formatação/build e oito layouts Chrome fictícios em 390/768/1024/1440 px claro/escuro. Sem overflow fora da tabela, toque de 44 px, expansão/seleção por teclado, objeto/tipo exatos, ausência de seleção sem callback e regressão das notas/série zero. [Galeria](../../orchestracao/.runtime/result-table-actions-20261016/galeria.html), [recibo](../../orchestracao/.runtime/result-table-actions-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Importação do XML — FE05-XML-UX01

Pedido de Lucas em 16/10/2026, pela imagem com nome do arquivo duplicado, botões neutros e erro solto no diálogo de importação. `ReceivingCreateDialog` passa a reunir seleção e identificação em um cartão. O nome aparece uma vez e quebra linha quando necessário. Botão Selecionar XML/Trocar arquivo roxo, Remover arquivo vermelho, Ler XML azul e Atualizar prévia turquesa. Download/consulta do pedido existente, cadastro de produto e recuperação da confirmação seguem as cores por função já existentes.

Mensagem de erro ganha superfície e título próprios, mantendo o texto recebido; carregamento permanece identificado e os controles continuam bloqueados durante leitura/confirmação ou resultado desconhecido. O seletor nativo é acionado pelo botão acessível, inclusive por teclado. Cabeçalho permanece acessível na rolagem; ações empilham no celular. Orientação curta e limite/formato do arquivo centralizados em `information.receivingXml`/`receivingXmlFile`. Prévia, associações, confirmação explícita, repetição/recuperação e conteúdo do XML não foram alterados.

Conferência: 22 testes em três arquivos, dois novos para seleção/remoção/reseleção e erro/recuperação; tipagem/lint/formatação/build aprovados. Chrome com transporte e documentos fictícios: oito layouts em 390/768/1024/1440 px claro/escuro sem overflow; seletor real do navegador por clique/Enter, FileList limpa ao remover, nome extenso, bloqueios, mensagem original e XML com BOM/acentos/quebras preservados. Sem gravação automática ou validação fiscal real. [Galeria](../../orchestracao/.runtime/receiving-xml-ux-20261016/galeria.html), [recibo](../../orchestracao/.runtime/receiving-xml-ux-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida.

## Consulta e reimpressão da etiqueta — FE06-ETIQUETA-UX01

Pedido de Lucas em 16/10/2026, pela imagem do diálogo com a etiqueta isolada à esquerda e ações abaixo. `OperationResults` passa a apresentar diretamente a prévia; o resultado completo permanece recolhido em Dados da unidade consultada. A consulta alinha código/Consultar, referências e correlação têm expansão própria. Cabeçalho permanece acessível na rolagem.

Prévia centralizada sobre uma superfície própria, ao lado do painel Impressão: botão roxo com ícone, orientação curta, Como ler a etiqueta recolhido e Próximo passo com a continuação azul existente. Até 900 px, os blocos ficam em uma coluna; controles de toque preservados. Textos de ajuda centralizados em `information.unitLabelPrint`/`unitLabel`. Atualização da consulta bloqueia impressão/continuação do resultado anterior; erro na geração do QR mantém impressão bloqueada e avisa o operador.

Estilos novos restritos à tela. Conteúdo/marcação da etiqueta, CODE128/QR do mesmo UUID, quantidade das notas, chegada física, conferente, DUN/numeração e CSS de impressão de 150 mm preservados. Impressão continua por ação explícita, sem criar estoque ou iniciar endereçamento. Nenhuma regra de negócio adicionada ao navegador.

Conferência: 38 testes em sete arquivos, quatro novos para impressão explícita, continuação, recarga/histórico, falha do QR e recuperação por GET/UUID exato; tipagem/lint/formatação/build aprovados. Chrome com componentes oficiais e transporte local fictício: oito layouts claro/escuro em 390/768/1024/1440 px sem overflow, teclado, toque, SKU/descrições/notas extensos e UUID inválido. Mídia de impressão: somente etiqueta, papel branco, 150 mm e conteúdo idêntico. Sem teste de impressora física ou leitura em dispositivo. [Galeria](../../orchestracao/.runtime/label-preview-20261016/galeria.html), [recibo](../../orchestracao/.runtime/label-preview-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício operacional ou publicação; versão ativa não medida.

## Cores por função e alinhamento dos filtros — FE02-ACOES01

Pedido de Lucas em 15/10/2026, com exemplos dos diálogos de detalhe/edição de Clientes e do filtro de Embalagens/DUN. A correção expressa pede cores diferentes, evitando concentrar todas as ações em azul.

| Função | Apresentação compartilhada |
| --- | --- |
| Ver detalhes e reabrir referências | Roxo, inclusive o ícone de detalhes na tabela. |
| Selecionar registro | Verde nos resultados individuais e nas listas de referências. |
| Atualizar lista, recarregar e consultar estado atual | Turquesa. |
| Voltar, fechar detalhes/referências e X do diálogo | Vermelho suave, inclusive nos diálogos de conta. |
| Ação principal e edição | Azul existente. |

Classes reutilizáveis em `styles/base.css`, com cores dos tokens dos temas claro/escuro. Rótulos e ícones preservam a identificação da ação; cor não declara sucesso, disponibilidade ou efeito de negócio. O fechamento mantém a mesma proteção de alterações pendentes, foco e bloqueio durante operações.

Nos filtros compartilhados de `RecordWorkspace`, campos e grupo de ações se alinham pelo início da linha do controle: o grupo reserva a altura do rótulo mais seu intervalo. Textos de orientação continuam abaixo do campo, sem puxar os botões para baixo. Em até 600 px, as ações ocupam sua própria linha, sem a reserva superior. Filtros, contexto e consultas permanecem iguais.

Validação local: 49 testes frontend, tipagem, lint, formatação e build isolado aprovados. Chrome com componentes oficiais e transporte fictício explícito: 46 medições de contraste, mínimo 4,66:1; normal/hover, dois temas, teclado, toque de 44 px, seleção/retorno/recarga e fechamento bloqueado conferidos. Dez medições do filtro de Produto/SKU em 1920/1440/1024/768/390 px; diferença de 0 px entre os topos dos controles e botões quando lado a lado, sem overflow. [Capturas](../../orchestracao/.runtime/button-colors-20261015/galeria.html) e [recibo](../../orchestracao/.runtime/button-colors-20261015/visual.json). Sem SQL, backend, reinício operacional ou publicação.

## Organização dos diálogos — FE02-DIALOG-ORG01

Pedido de Lucas em 15/10/2026, a partir do diálogo de solicitar encerramento definitivo. Ajuste no padrão compartilhado de `RecordDialog` e `OperationForm`:

- O cabeçalho informa a ação atual. Nome do registro e situação consultada aparecem em uma faixa própria; quando a resposta não traz nome/código, a identificação da linha original é usada somente com identidade correspondente.
- Campos fixos exclusivamente técnicos não geram uma superfície vazia. Inputs ocultos de identidade/revisão continuam protegidos. Encerramento de um registro selecionado mantém também seu tipo fixo, sem oferecer troca para outro cadastro.
- A ação principal fica junto ao formulário. Outras operações ficam em um grupo separado, aberto nos detalhes e recolhido durante uma ação. A escolha preserva o aviso de edição pendente; após troca autorizada, recolhe o menu e leva o foco ao formulário.
- O rodapé contém somente voltar/fechar, detalhes e recarga. Botões ficam alinhados no desktop e em coluna no celular. Solicitação/confirmação simples de encerramento usa largura de até 720 px; formulários extensos conservam sua largura.

Permissões, revisão, IDs, confirmação, resposta desconhecida e validações backend permanecem nas fronteiras existentes. Menu recolhido não concede nem retira permissão. Dados e ações disponíveis continuam vindo das consultas e políticas atuais.

Chrome local com componentes oficiais e transporte fictício explícito: oito layouts nos temas claro/escuro, 390/768/1024/1440 px, sem overflow. Conferidos menu por teclado, foco, proteção da justificativa, tipo e ID Long preservados, revisão atual na confirmação e separação da navegação. [Capturas](../../orchestracao/.runtime/dialog-organization-20261015/galeria.html) e [recibo](../../orchestracao/.runtime/dialog-organization-20261015/visual.json). Fonte e build locais não representam atualização operacional.

## Consulta de encerramento — FE04-ENC-UX01

Pedido de Lucas em 15/10/2026, após imagem da consulta com tabela e comandos desorganizados. `ClosureDetail` substitui a renderização genérica de `EncerramentoDto.Resultado` tanto no diálogo quanto nos demais resultados desse contrato.

- Cabeçalho “Encerramento do cadastro”; resumo com tipo, nome/referência e situação legível. Nome da linha original só é aproveitado com a mesma identidade.
- Impedimentos em lista numerada, com título legível, recurso, descrição integral e referência do registro relacionado, usando consulta autorizada existente quando disponível. Nenhuma operação de negócio ou seleção é criada para a linha do impedimento.
- Lista vazia e lista não informada têm mensagens distintas; item incompleto permanece contado. Nenhuma dessas mensagens decide liberação ou substitui a revalidação backend.
- Seleção única do cadastro após os impedimentos. Operações recolhidas por padrão, em duas colunas ao expandir e uma no celular. Rodapé exclusivo para fechar/recarregar.
- Textos de ajuda em `frontend/src/content/information.ts`; regras, permissões e contratos de confirmação preservados.

Conferência: 50 testes em cinco arquivos, cinco novos casos de apresentação/identidade/seleção/estados; tipagem/lint/formatação/build aprovados. Chrome com componentes oficiais e transporte fictício: oito layouts em 390/768/1024/1440 px, claro/escuro, sem overflow; lista com um/dois/zero impedimentos, lista ausente, seleção sem escrita e confirmação com ID Long/tipo/revisão. [Capturas](../../orchestracao/.runtime/closure-detail-20261015/galeria.html), [recibo](../../orchestracao/.runtime/closure-detail-20261015/visual.json). Sem integração SQL/backend, reinício operacional ou publicação neste recorte.

## Dados e consultas alinhados — FE02-RESULT-ALIGN01

Pedido de Lucas em 15/10/2026, a partir da consulta de Pedido/Entradas conferidas/Progresso. O estilo posterior de `workspace.css` retirava a divisão entre as seções genéricas; consultas relacionadas exibiam atualização isolada abaixo dos resultados. A apresentação compartilhada de `Result` e `RelatedRecords` passa a manter uma hierarquia uniforme.

- Campos com espaçamento constante; em diálogos, duas colunas no desktop e uma no celular. Resumo e objetos aninhados possuem limites visuais e títulos alinhados. Seleção fica ao final dos dados do próprio registro, sem trocar seu alvo.
- Consultas relacionadas em blocos da mesma largura, com título e Atualizar no cabeçalho. Atualização e erro pertencem ao bloco correspondente. Nomes acessíveis distinguem a consulta da lista interna de entradas conferidas.
- Listas vazias informam ausência de registros sem controles de paginação inativos. Listas preenchidas preservam contagem/páginas; as relacionadas usam `onPage` com o `useRecordQuery` existente, mantendo pedido e filtros aplicados.
- Zero, falso, campos não informados e coleções vazias continuam distintos. IDs Long, permissões, revisão e seleção de entrada preservados. Ajuda de `information.confirmedEntries` e seu consumidor revisados, permanecendo adequados ao fluxo.

Conferência: 686 testes frontend em 70 arquivos, incluindo três novos casos de consulta relacionada e adaptação da expectativa do estado vazio. Tipagem/lint/formatação/build aprovados; preservado aviso preexistente de bundle acima de 500 kB. Chrome fictício: oito layouts claro/escuro em 390/768/1024/1440 px sem overflow, larguras/margens/cabeçalhos alinhados, teclado, recarga isolada após erro, paginação e seleção exatas. [Capturas](../../orchestracao/.runtime/result-alignment-20261015/galeria.html), [recibo](../../orchestracao/.runtime/result-alignment-20261015/visual.json). Sem API/SQL real, alteração de processo operacional ou publicação neste recorte.

## Filtros de unidades — FE08-FILTROS01

Pedido de Lucas em 15/10/2026, após imagem dos sete filtros comprimidos de Corrigir organização física. `StockQueryFilters` organiza a consulta `EstoqueController.listar`, compartilhada pelas páginas de unidades/estoque, reutilizando os campos existentes.

- Cliente e armazém: escopo obrigatório, lado a lado quando há largura.
- Identificação da unidade: Produto/SKU e UUID, com espaço para ler os seletores e manter as orientações existentes.
- Localização e situação: endereço, disponibilidade para saída e situação, com endereço mais largo. Tablet redistribui os controles; celular mantém uma coluna.
- Aplicar/Restaurar em faixa própria alinhada à direita, antes da busca local e atualização da lista. Campos adicionais futuros não são omitidos.

Defaults, filtros enviados, obrigatoriedade, permissões, tipagem e seletores existentes permanecem. Orientações de referência continuam junto aos campos, incluindo carregamento/erro/ausência; não foi duplicada regra operacional em ajuda ou UI.

Conferência: 45 testes em quatro arquivos, incluindo dois novos casos de envio/restauração dos sete critérios e bloqueio/erro com preservação dos filtros. Tipagem/lint/formatação/build aprovados; aviso preexistente do bundle preservado. Chrome fictício em dez layouts, dois temas e 390/768/1024/1280/1600 px: sem overflow, controles com pelo menos 250 px, alinhamento, teclado, alvos de toque de 44 px e carregamento das referências no contexto. IDs Long, UUID e critérios exatos conferidos. [Capturas](../../orchestracao/.runtime/stock-filters-20261015/galeria.html), [recibo](../../orchestracao/.runtime/stock-filters-20261015/visual.json). Sem API/SQL real, alteração de processo operacional ou publicação.

## Cores das consultas de referências — FE02-REF-COR01

Pedido de Lucas em 15/10/2026 a partir dos três botões de “Consultar referências para a tarefa” em Estoque. Botões compartilhados de `ReferenceLookup` recebem `action-reference`, com família estável pela consulta:

| Consulta | Cor |
| --- | --- |
| Geral/catálogos, incluindo produtos | Roxo |
| Unidade/estoque, incluindo origem da unidade | Âmbar |
| Endereço/capacidade, incluindo posições | Turquesa |

Reutiliza tokens dos temas claro/escuro; hover e foco seguem o padrão compartilhado. Rótulos continuam identificando as ações. Carregamento, bloqueio de duplo envio, erro, destinos e seleção permanecem. Ver referências consultadas continua com a variante existente de detalhes.

Conferência: 24 testes em quatro arquivos, tipagem/lint/formatação/build aprovados. Chrome fictício em seis layouts claro/escuro, 390/768/1440 px: sem overflow, três cores distintas, 18 medições de contraste normal/hover/foco acima de 4,5:1, teclado, toque de 44 px, bloqueio durante consulta e erro preservados. [Capturas](../../orchestracao/.runtime/reference-colors-20261015/galeria.html), [recibo](../../orchestracao/.runtime/reference-colors-20261015/visual.json). Sem API/SQL real, reinício operacional ou publicação; preservado aviso preexistente do bundle.

## Registro do fato de avaria — FE08-AVARIA-UX01

Pedido de Lucas em 15/10/2026, após imagem de Registrar fato. `DamageRegistrationFields`, selecionado por `OperationForm` somente em `AvariaController.registrar`, apresenta quatro seções: Unidade afetada, Dados do fato, Destino da mercadoria e Justificativa/pendências. Campos continuam usando metadados, componentes e políticas existentes; campos adicionais futuros não são omitidos.

Posições de destino usam linhas numeradas com nomes acessíveis próprios e rótulos curtos Endereço/Código lido. Adicionar é verde; remover é vermelho. Incluir/remover preserva quantidade, horário, justificativa e os demais destinos. Conjunto de posições permanece opcional, após a lista de destinos. Cores de inclusão/remoção também cobrem listas genéricas de `Fields`; referências do diálogo herdam FE02-REF-COR01. Confirmação visível durante a rolagem, com duas colunas no desktop e uma no celular.

Ajuda em `information.damageUnit`, `damageQuantity`, `damageDestinations` e `damageDestinationsEmpty`. Decimal, fuso, revisão, campos fixos, limites e validações do contrato permanecem. Resolução de pendências conserva a política exclusiva do Gestor. Nenhum cálculo ou autorização foi transferido ao navegador.

Conferência: 61 testes em sete arquivos, três novos casos de contrato/destinos/bloqueio/conflito/perfil, além da jornada completa de avaria e fronteiras existentes. Tipagem/lint/formatação/build aprovados. Chrome fictício: oito layouts iniciais claro/escuro em 390/768/1024/1440 px e destinos preenchidos nos dois temas em desktop/celular; sem overflow, margens/campos alinhados, confirmação visível, teclado, remoção sem perda e POST fictício somente após revisão, com ID Long/revisão/decimal exatos. [Capturas](../../orchestracao/.runtime/damage-form-20261015/galeria.html), [recibo](../../orchestracao/.runtime/damage-form-20261015/visual.json). Sem API/SQL real, alteração de processo operacional ou publicação; aviso preexistente do bundle preservado.

## Filtros de indicadores — FE08-IND-FILTROS01

Pedido de Lucas em 15/10/2026 para organizar Indicadores e validade. `StockQueryFilters` estende o agrupamento de estoque às consultas `IndicadorEstoqueController.listar`: Contexto da consulta contém cliente/armazém lado a lado e produto em uma linha inteira; Opções do relatório contém incluir valor e fuso. Os grupos usam proporção 3:2 no desktop, se empilham abaixo de 1280 px e usam uma coluna abaixo de 768 px. Aplicar/Restaurar ficam alinhados à direita em faixa própria; busca local/Atualizar permanecem separados.

Reutilizados `Fields`, seletores e políticas existentes. Sem alteração em obrigatoriedade, IDs, fuso padrão `America/Sao_Paulo`, inclusão de valor restrita a Supervisor/Gestor, validação, consulta ou restauração. Ajuda de valor armazenado/validade em `information.ts` e orientação da página revisadas e preservadas; layout não muda esses significados nem exige nova explicação.

**Conferência:** 44 testes frontend em cinco arquivos, incluindo três novos casos de aplicação/restauração e acesso ao valor nos três perfis; tipagem, lint focal, formatação e build aprovados. Dez layouts Chrome fictícios nos dois temas, 390/768/1024/1280/1600 px: campos com pelo menos 250 px, alinhamento, ausência de overflow, teclado, IDs Long/fuso exatos e Operação sem valor. Capturas desktop/tablet/celular inspecionadas. [Galeria](../../orchestracao/.runtime/indicator-filters-20261015/galeria.html), [recibo visual](../../orchestracao/.runtime/indicator-filters-20261015/visual.json).

Fonte pronta; sem backend/SQL, reinício operacional ou publicação. Prova com transporte fictício não comprova integração real ou atualização da instância ativa. Preview próprio encerrado; DEV pelo operador no fluxo habitual.

## Detalhes do serviço — FE11-SERV-UX01

Pedido de Lucas em 15/10/2026 a partir da imagem do serviço de armazenagem. Apresentação específica de `ConfiguracaoCobrancaDto.Servico` em `RecordDialog`: largura máxima de 760 px, título Detalhes do serviço, ícone de serviços, descrição uma única vez e situação em badge. Código, tipo de serviço e unidade de cobrança formam uma faixa de três colunas no desktop e uma no celular. Fechar e Selecionar compartilham o rodapé, eliminando a faixa adicional de seleção.

`ServiceDetail` traduz apenas termos conhecidos de tipo/unidade do contrato para leitura (por exemplo, `POSICAO_DIA` → Posição por dia). Valores desconhecidos e dados ausentes continuam explícitos; textos longos quebram sem truncamento. Não calcula preços nem altera dados, seleção, permissões ou regras. Outros resultados e formulários mantêm sua apresentação. Nenhuma ajuda do catálogo é adicionada ou alterada neste ajuste de organização.

**Conferência:** 37 testes frontend em cinco arquivos, incluindo três novos casos de seleção exata, desconhecidos e ausência/inatividade; tipagem, lint focal, formatação e build aprovados. Oito layouts Chrome fictícios em 390/768/1024/1440 px, claro/escuro: diálogo compacto, controles alinhados e sem overflow. Textos longos adicionais conferidos nos dois temas em desktop/celular. Tab contido, Enter/Escape, retorno ao acionador e seleção com ID/revisão/tipo/unidade exatos, sem requisição de negócio. Capturas inspecionadas. [Galeria](../../orchestracao/.runtime/service-detail-20261015/galeria.html), [recibo](../../orchestracao/.runtime/service-detail-20261015/visual.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Tabelas e auditoria — FE11-TAB-UX01

Pedido de Lucas em 15/10/2026 a partir da imagem com auditoria, Visão e Tabelas e vigências. O seletor de visão deixa de ocupar uma faixa solta fora da consulta e passa ao cabeçalho de `RecordWorkspace`, junto dos comandos de criação. Grade mantém as bases alinhadas no desktop e empilha no celular. Aplica-se também aos outros seletores de visão do mesmo componente, com rótulos existentes preservados.

`AuditSelection` usa cabeçalho e comandos lado a lado quando há espaço, sem margens externas adicionais na jornada. Botões roxos identificam ação/tipo e nome do registro em linhas próprias; textos extensos quebram sem corte. Alvos múltiplos e o bloco de legado com sua explicação permanecem separados. Não altera DTO/ID enviado, seleção ou permissões. O filtro de `ConfiguracaoCobrancaController.tabelas` limita a largura do armazém e alinha Aplicar/Restaurar à base do controle; busca local/Atualizar continuam em faixa própria.

A conferência de teclado nativa identificou que mudar a visão reaplicava o `startAction` inicial e abria um diálogo do contexto anterior. A ação inicial é agora consumida apenas até o usuário trocar a visão; essa troca monta a consulta escolhida, sem reabrir o comando, e mantém foco no seletor. Abertura inicial de operações e proteção de descarte existentes preservadas. Textos de ajuda e orientação sobre histórico, vigências e parâmetros ausentes revisados e preservados; sem regra financeira no navegador.

**Conferência:** 38 testes frontend em cinco arquivos, incluindo dois novos casos de foco, ausência de diálogo indevido, critérios exatos e perfis; tipagem, lint focal, formatação e build aprovados. Oito layouts Chrome fictícios em 390/768/1024/1600 px, claro/escuro: sem overflow, controles alinhados, alvos móveis de 44 px, troca por teclado, aplicação com ID Long exato e busca mantida na recarga. Auditoria/legado com DTO/ID próprios; Supervisor sem auditoria/criação. Alvos múltiplos e explicação do legado conferidos em desktop/celular. Capturas inspecionadas. [Galeria](../../orchestracao/.runtime/pricing-workspace-20261015/galeria.html), [recibo](../../orchestracao/.runtime/pricing-workspace-20261015/visual.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Filtros do histórico — FE12-AUD-FILTROS01

Pedido de Lucas em 15/10/2026 a partir da imagem de Histórico do vínculo. Layout específico das consultas `AuditoriaController.listar` no `RecordWorkspace`: tipo até 300 px e registro no restante da grade. Busca nas opções e seleção compartilham a linha quando há espaço. Abaixo de 1280 px, tipo e registro usam linhas próprias; abaixo de 768 px, busca/seleção também se empilham. Aplicar/Restaurar têm faixa exclusiva alinhada à direita e alvos de 44 px no celular. Busca local da resposta/Atualizar continuam separados dos critérios da consulta.

Apenas classe/estilos de apresentação; campos, valores, tipos, seletores, paginação de opções, envio e permissões existentes preservados. Nenhuma atribuição automática de legado ou alteração de namespace. Orientação da página sobre histórico confirmado/legado revisada e preservada; nenhuma ajuda do catálogo precisou mudar.

**Conferência:** 45 testes frontend existentes em quatro arquivos, tipagem, lint focal, formatação e build aprovados. Oito layouts Chrome fictícios, claro/escuro em 390/768/1024/1600 px, sem overflow e com controles/comandos alinhados. Verificados teclado, busca nas opções, registro único, pendência, erro/recuperação, restauração, busca local da resposta e consulta com tipo/ID Long exatos. Legado mantém namespace próprio; Supervisor permanece sem acesso à consulta. Capturas desktop/tablet/celular inspecionadas. [Galeria](../../orchestracao/.runtime/audit-filters-20261015/galeria.html), [recibo](../../orchestracao/.runtime/audit-filters-20261015/visual.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Configuração do contrato — FE11-CONTRATO-UX01

Pedido de Lucas em 15/10/2026 a partir da imagem de mínimo, GRIS, justificativa e resolução histórica. Apresentação específica de `ConfiguracaoCobrancaController.configurar` por `ContractConfigurationFields`: Cliente e vigência, Ciclo de cobrança, Mínimo de cobrança, GRIS e Justificativa/resolução histórica. Campos em duas colunas no desktop/tablet e uma no celular; modalidade/aplicação em linha própria, sem dispersar os parâmetros entre grupos. Confirmação permanece visível durante rolagem.

Rótulos mais curtos e opções legíveis preservam os valores canônicos do contrato. `ScalarField.choiceLabels` é opcional e mantém a apresentação anterior para os demais consumidores. Serviços abrangidos pelo mínimo têm seleção por nome/código, linhas identificadas, Adicionar verde e Remover vermelho; envio continua como lista de IDs Long. Inclusão/remoção preservam os demais serviços e parâmetros. Resolução histórica reutiliza os campos/permissões existentes; orientação em linha própria e tipo/compromisso alinhados. Informar resolução fica roxo e remover vermelho nos objetos `resolucao` de `Fields`.

Ajuda de configuração/mínimo/GRIS/resolução em `information.ts`, revisada com o documento 29 e os alinhamentos AC. Sem cálculo no frontend ou defaults financeiros novos. Ausência, não aplicação e zero continuam distintos; nenhum parâmetro é apagado automaticamente ao trocar opções. Backend permanece responsável por combinação válida, permissão, histórico e cálculo.

**Conferência:** 199 testes frontend em cinco arquivos, incluindo três novos casos de lista/ID/precisão, ausência/resolução opcional e bloqueio/conflito; tipagem, lint focal, formatação e build aprovados. Oito layouts Chrome fictícios em 390/768/1024/1440 px, claro/escuro, sem overflow; seções alinhadas, confirmação visível ao rolar e controles de 44 px no celular. Também conferidos serviços/parâmetros preenchidos, resolução aberta com tipo/compromisso na mesma linha, confirmação por teclado, bloqueio durante envio e POST fictício com IDs/decimais/códigos exatos. Supervisor sem configuração. Capturas inspecionadas. [Galeria](../../orchestracao/.runtime/contract-form-20261015/galeria.html), [recibo](../../orchestracao/.runtime/contract-form-20261015/visual.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Seleção do serviço — FE11-SERV-SEL01

Pedido de Lucas em 15/10/2026: Selecionar este registro parecia não responder nos detalhes de um serviço. A abertura da consulta já selecionava o registro, e o botão repetia a seleção sem fechar a janela ou confirmar visualmente. `RecordWorkspace` deixa de selecionar serviços pela abertura; o botão do leitor especializado em `RecordDialog` seleciona explicitamente e fecha após a guarda existente.

Ao retornar à lista, mensagem com `role=status` identifica o serviço escolhido. `RecordTable.selectedId` marca a linha com fundo verde discreto e texto Selecionado, usando os tokens dos dois temas. O foco retorna ao botão que abriu os detalhes. Consultar/fechar outro serviço preserva a seleção; selecionar outro substitui o anterior e mantém apenas um marcador. A seleção chega ao contexto da jornada e ao serviço do novo item de tabela, com ID/revisão originais. Não executa escrita de negócio nem altera permissões. Outros tipos de registro e seleções internas permanecem como antes; nenhuma ajuda do catálogo foi acrescentada.

**Conferência:** 46 testes frontend em cinco arquivos, incluindo três casos integrados de seleção/fechamento/confirmação/contexto/perfis; tipagem, lint focal, formatação e build aprovados. Chrome com transporte fictício em oito layouts claro/escuro, 390/768/1024/1440 px, sem overflow da página. Conferidos Enter, retorno do foco, consulta passiva, substituição explícita, ID Long/revisão exatos e Nova tabela recebendo o serviço escolhido. Supervisor seleciona sem ganhar criação/auditoria. Capturas desktop/celular e próxima etapa inspecionadas. [Galeria](../../orchestracao/.runtime/service-selection-20261015/galeria.html), [recibo](../../orchestracao/.runtime/service-selection-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Filtros da contagem física — FE12-CONT-FILTROS01

Pedido de Lucas em 15/10/2026 a partir da imagem de Contagem física. `ContagemController.listar` reutiliza `StockQueryFilters` em `RecordWorkspace`: Cliente e armazém, Identificação da unidade (produto/UUID) e Localização e situação (endereço/situação/impedimento). Os dois primeiros grupos dividem a linha no desktop; endereço ocupa metade do último grupo e os dois seletores de resultado dividem o restante. Tablet empilha grupos, dando linha própria ao endereço; celular usa uma coluna. Aplicar/Restaurar ficam alinhados em faixa abaixo dos filtros, separados da busca local/Atualizar lista.

Reutilizados campos, estilos e estados existentes; apenas a escolha do apresentador e o agrupamento de impedimento foram ampliados. Preservados critérios, padrões, ID Long/UUID, distinção entre contar/aplicar ajuste e permissões. Orientação atual da jornada revisada e mantida; nenhuma ajuda do catálogo foi acrescentada. O navegador não calcula diferença nem confirma ajuste automaticamente.

**Conferência:** 42 testes frontend existentes em cinco arquivos, tipagem, lint focal, formatação e build aprovados. Chrome com transporte fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: sete filtros sem ausência/duplicação, seletores com pelo menos 250 px, comandos alinhados e sem overflow. Enter, toque de 44 px no celular, IDs/UUID/situação/impedimento exatos, restauração, bloqueio durante consulta, erro/recuperação e perfis Gestor/Supervisor/Operação conferidos. Capturas desktop/celular inspecionadas. [Galeria](../../orchestracao/.runtime/count-filters-20261015/galeria.html), [recibo](../../orchestracao/.runtime/count-filters-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Filtros da carga inicial — FE12-CARGA-FILTROS01

Pedido de Lucas em 15/10/2026 a partir da imagem de Carga inicial excepcional. `CargaInicialController.listar` reutiliza `StockQueryFilters`, com dois grupos empilhados: Cliente e armazém e Dados da carga. Desktop distribui produto, referência e situação em 2:1:1, com rótulos/controles alinhados. Abaixo de 1280 px, produto ganha linha inteira e referência/situação ficam abaixo; celular usa uma coluna. Aplicar/Restaurar ficam alinhados à direita em faixa própria, separados da busca local/Atualizar lista. O empilhamento evita sobra excessiva dentro do grupo de cliente/armazém.

Somente apresentador/estilos: `Fields`, seletores, consulta, padrões, IDs, referência e situação canônica preservados. Ajuda atual da jornada revisada e mantida; nenhuma entrada do catálogo precisou mudar. Preparação não vira liberação de saída; regularização, versão/revisão/hash, etiquetas e permissões continuam regidos pelo backend.

**Conferência:** 42 testes frontend existentes em cinco arquivos, tipagem, lint focal, formatação e build aprovados. Chrome com transporte fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: cinco filtros sem ausência/duplicação, campos com pelo menos 250 px, produto mais largo e sem overflow. Enter, toque de 44 px no celular, IDs Long/referência/situação exatos, restauração, pendência, erro/recuperação e acesso de Gestor/Supervisor conferidos; Operação sem acesso. Ajuste final de proporção conferido novamente no navegador e build. Capturas desktop/tablet/celular inspecionadas. [Galeria](../../orchestracao/.runtime/initial-load-filters-20261015/galeria.html), [recibo](../../orchestracao/.runtime/initial-load-filters-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Filtros da contingência — FE12-CONTING-FILTROS01

Pedido de Lucas em 15/10/2026 a partir da imagem de Registrar fatos da planilha. `ContingenciaController.listar` reutiliza `StockQueryFilters` com dois grupos empilhados: Cliente e armazém e Registro e situação. O segundo apresenta identidade/tipo na primeira linha e situação/pendência na seguinte. Desktop/tablet usam duas colunas; celular usa uma. Campos maiores acomodam o rótulo de identidade sem deslocar o controle em relação a Tipo. Aplicar/Restaurar ficam em faixa própria, separados da busca local/Atualizar lista.

`groupsBySchema` reúne os agrupamentos específicos de indicadores, carga inicial e contingência; estoque/contagem mantêm o agrupamento padrão. Reutilizados `Fields`, seletores, valores, critérios e estados. Orientação da jornada revisada e preservada; nenhuma entrada do catálogo precisou mudar. Conciliação continua explícita e dependente de versão/hash, dependências e prova da operação original; nenhuma fila automática ou operação de negócio foi criada.

**Conferência:** 42 testes frontend existentes em cinco arquivos, tipagem, lint focal, formatação e build aprovados. Chrome com transporte fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: seis filtros sem ausência/duplicação, campos com pelo menos 250 px, controles/comandos alinhados e sem overflow. Enter, toque de 44 px no celular, IDs Long/identidade/tipo/situação/pendência exatos, restauração, bloqueio durante consulta e erro/recuperação conferidos. Gestor/Supervisor mantêm consulta/registro; Operação sem acesso. Capturas desktop/tablet/celular inspecionadas. [Galeria](../../orchestracao/.runtime/contingency-filters-20261015/galeria.html), [recibo](../../orchestracao/.runtime/contingency-filters-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Identificação da consulta de histórico — FE02-AUD-CLAREZA01

Pedido de Lucas em 15/10/2026: marcação roxa Auditar Serviço considerada confusa. `AuditSelection` separa os dados da ação. Cartão neutro identifica Registro selecionado e tipo; serviço apresenta nome e código separados. Botão roxo Ver histórico recebe ícone próprio e nome acessível com o alvo. Cabeçalho Histórico de alterações e orientação visível explicam a finalidade. Textos principal/legado centralizados em `information.ts`. Sem identificação completa, são apresentados código ou ID disponíveis; nenhum nome é inventado. A área dos dados não executa a consulta.

`JourneyPage` passa a selecionar o alvo e abrir a etapa de auditoria no mesmo clique, respeitando `canLeavePage`. Antes, o comando apenas preparava o alvo. A consulta existente recebe o tipo e o ID originais. `RecordWorkspace` usa Histórico de alterações como título geral e Legado não atribuído para esse namespace, com explicações do mesmo catálogo; serviço não é apresentado como vínculo. Histórico confirmado e legado não atribuído permanecem separados em conteúdo e namespace. Permissões/contratos não foram ampliados. O leitor interno de auditoria conserva o contexto do seu diálogo.

**Conferência:** 34 testes frontend em sete arquivos, incluindo dois novos casos de navegação com serviço e legado de mesmo ID Long; tipagem, lint focal, formatação e build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: sem overflow, nome/código separados, ação por Enter e etapa aberta com tipo/ID exatos. Conferidos múltiplos alvos, os 17 tipos, código/ID sem nome, vazio, textos longos, bloqueio e toque de 44 px no celular. Supervisor/Operação continuam sem auditoria; transporte somente GET fictício. Capturas inspecionadas. [Galeria](../../orchestracao/.runtime/audit-target-clarity-20261015/galeria.html), [recibo](../../orchestracao/.runtime/audit-target-clarity-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Apresentação da contingência — FE12-CONTING-TEXTO01

Pedido de Lucas em 15/10/2026: texto grande e estranho no cabeçalho de Registrar fatos da planilha. A etapa apresenta apenas “Registre os fatos da planilha após a recuperação da conexão.” Como funciona fica junto de Registrar ocorrência, com aparência secundária; o comando principal permanece azul. Detalhes são consultados no popup existente, em quatro assuntos: Dados do fato, Conciliação, Pendências e Reenvio.

Texto centralizado em `information.contingencyRegistration`, preservando registro manual, dados específicos de CHEGADA, versão/hash, execução/vínculo explícitos, prova da operação original, dependências, conflitos e confirmação original no reenvio. A observação específica sobre exercício não ocupa o cabeçalho compartilhado. Nenhuma fila automática, operação de negócio ou permissão foi criada. `InformationHint.label` é opcional; consumidores anteriores continuam com ícone e comportamento existentes.

**Conferência:** 44 testes frontend existentes em quatro arquivos, tipagem, lint focal, formatação e build aprovados. Chrome fictício em dez layouts claro/escuro, 390/768/1024/1280/1600 px: resumo em até duas linhas e ajuda dentro da tela em desktop/celular. Conferidos conteúdo dos quatro assuntos, Enter/Escape, foco preservado, toque de 44 px, ajuda sem abrir registro/escrever, seis critérios exatos, restauração, pendência/erro/recuperação e perfis. Capturas inspecionadas após a animação do popup. [Galeria](../../orchestracao/.runtime/contingency-intro-20261015/galeria.html), [recibo](../../orchestracao/.runtime/contingency-intro-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real nem atualização da instância ativa; DEV pelo operador no fluxo habitual.

## Carga inicial: consulta e revisão — FE12-CARGA-UX01

Pedido de Lucas em 15/10/2026 com imagens de Revisar dados levantados e da consulta de uma carga inicial. Causas visuais: rótulo genérico de contingência aplicado ao DTO da carga, repetição do caminho inteiro em cada campo, várias bordas aninhadas e uma tabela com seleção de `NovaUnidade`, que descreve a composição da carga e não é um registro independente selecionável nesse leitor.

`InitialLoadFields` organiza criar/revisar em Identificação da carga (quando aplicável), Origem da mercadoria, Datas e lote, Quantidades levantadas, Unidades informadas e Fonte e justificativa. Unidade possui um cartão, numeração, quatro rótulos curtos, inclusão verde/remoção vermelha e opções legíveis com valores canônicos preservados. Fonte/motivo usam largura completa. Seções têm separadores simples; informações de origem/datas/unidades ficam no catálogo `information.ts`. Campos e validações reutilizam o contrato existente; nenhuma soma, origem ou data é inventada pelo navegador.

`InitialLoadDetail` separa identificação/situação, pendências, origem/datas, quantidades, unidades, fonte e etiquetas. Unidade usa cartão de dados sem coluna Detalhe ou Selecionar configuração. Identificação/versão/revisão/hash permanecem disponíveis em seção recolhida; nota de origem também pode ser expandida. Etiquetas são exibidas completas e numeradas como etiquetas, sem presumir novo vínculo. Ausência não vira zero nem ausência de pendências confirmada. Selecionar esta carga fica no rodapé, envia o objeto original e fecha os detalhes; operações existentes permanecem em menu recolhido com largura proporcional. Cabeçalho fica visível na rolagem e confirmação do formulário acompanha o rodapé.

**Conferência:** 23 testes frontend em cinco arquivos, incluindo cinco novos casos de criação/ausência, edição/lista/payload, bloqueio/conflito, consulta/seleção e zero/dados parciais/enum desconhecido. Tipagem, lint focal, formatação e build aprovados. Chrome fictício em 16 layouts de consulta/revisão, claro/escuro em 390/768/1024/1440 px, sem overflow; etiquetas/hash completos, quatro rótulos curtos por unidade, inclusão/remoção, confirmação prévia por teclado, bloqueio durante envio, fonte preservada após 409 e seleção exata da carga. IDs Long, revisão, decimais e instante com frações mantidos no payload. Perfil Operação continua sem revisão. Capturas desktop/celular inspecionadas. [Galeria](../../orchestracao/.runtime/initial-load-ux-20261015/galeria.html), [recibo](../../orchestracao/.runtime/initial-load-ux-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Transporte fictício não comprova integração real, versão ativa ou conclusão da inspeção manual FE13-MANUAL01.

## Confirmação da carga inicial — FE12-CARGA-CONF01

Pedido de Lucas em 15/10/2026 a partir do diálogo Confirmar. `InitialLoadConfirmation` apresenta orientação curta do catálogo, carga/situação/revisão e cliente/armazém, leitura da etiqueta fornecida, todas as etiquetas de unidade e justificativa. Dados técnicos da revisão ficam recolhidos, com identificador, versão e SHA-256 completos ao expandir; nenhuma etiqueta é associada por suposição à posição de uma unidade. Removidas as caixas recursivas da conferência genérica e a repetição do registro selecionado. Título específico, abertura pelo início, foco na volta à conferência e botões acessíveis na rolagem; celular usa uma coluna e toque de 44 px.

A conferência no navegador revelou `rowContext` sobrescrevendo o número da revisão pelo objeto completo do resultado da carga. Corrigido somente para preparar/confirmar abertos por `CargaInicialDto.Resultado`, preservando o número existente. Regressão reproduzida antes da correção e aprovada depois. O apresentador não modifica o comando nem decide regularização, capacidade, permissão ou liberação de saída; confirmação e guardas continuam no fluxo/backend existentes.

**Conferência:** 33 testes em seis arquivos, cinco casos novos cobrindo contexto dos dois comandos, retorno sem envio, confirmação/envio único/payload/conflito e ausências. Tipagem/lint/formatação/build aprovados. Chrome fictício em oito layouts claro/escuro, 390/768/1024/1440 px, sem overflow; início/foco, expansão técnica por teclado, toque, doze etiquetas longas, dados exatos e perfil Operação conferidos. Capturas desktop/celular inspecionadas. [Galeria](../../orchestracao/.runtime/initial-load-confirmation-20261015/galeria.html), [recibo](../../orchestracao/.runtime/initial-load-confirmation-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida. Esta prova fictícia permanece separada da inspeção manual FE13-MANUAL01.

## Consulta da carga inicial — FE12-CARGA-CONS01

Pedido de Lucas em 15/10/2026 pela imagem de Consultar detalhe. A consulta passa a se identificar como Consultar carga inicial, com rótulo Carga inicial e orientação curta do catálogo. Seletor e Consultar compartilham a linha; quando há busca entre mais opções, busca/seletor ficam lado a lado. Celular empilha os controles e mantém toque de 44 px. A consulta preserva GET, referências e ID exato; o fieldset desabilita os campos durante carregamento mantendo a seleção visível.

Correlação disponível ao expandir Dados da consulta; mensagem fictícia continua explícita e erros permanecem destacados com seu código. O resultado tipado `CargaInicialDto.Resultado` reutiliza `InitialLoadDetail` também em `Result`, com as referências indexadas do objeto retornado. Mantidos separação por assunto, unidades sem seleção interna, etiquetas completas, pendências, ausências/zero e identificação da revisão. Há uma seleção do objeto completo, desabilitada enquanto a consulta é atualizada; resultado anterior é identificado e falha retira o resultado, sem apresentar sucesso.

**Conferência:** 53 testes em sete arquivos, incluindo três novos casos de GET/ID/seleção exata, carregamento/erro/recuperação e perfil Operação. Tipagem/lint/formatação/build aprovados. Chrome fictício em doze layouts: claro/escuro, 390/768/1024/1440 px, com duas ou doze opções. Alinhamento, Enter, expansão da correlação por teclado, toque, unidades, estado anterior e ausência de overflow conferidos. Capturas desktop/celular inspecionadas. [Galeria](../../orchestracao/.runtime/initial-load-query-20261015/galeria.html), [recibo](../../orchestracao/.runtime/initial-load-query-20261015/validacao.json).

Fonte pronta; preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida; prova fictícia separada da inspeção FE13-MANUAL01.

## Cores das ações após operação — FE02-REPETIR-CORES01

Pedido de Lucas em 15/10/2026 pela imagem de Nova carga inicial concluída. Repetir exatamente a mesma operação recebe `action-replay`, com fundo/texto âmbar pelos tokens de atenção existentes. Iniciar nova operação recebe `action-add`, verde. Confirmação mantém azul e estado desabilitado quando concluída. O padrão se aplica aos controles compartilhados de `OperationForm`; nenhuma alteração de texto, ajuda, payload, chave ou permissão. Hover e foco seguem as classes semânticas de `base.css`.

Conferência local: 14 testes existentes de reenvio, escopo e jornadas, tipagem/lint/formatação/build aprovados. Seis layouts Chrome fictícios claro/escuro em 390/768/1440 px: cores distintas, hover/foco, toque e ausência de overflow. Reenvio mantém os mesmos dados/chave; nova operação por teclado não envia automaticamente. [Prévia](../../orchestracao/.runtime/operation-button-colors-20261015/galeria.html), [recibo](../../orchestracao/.runtime/operation-button-colors-20261015/validacao.json). Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida.

## Formulário de ocorrência — FE12-CONTING-FORM01

Pedido de Lucas em 15/10/2026 pela imagem de Registrar ocorrência com tipo CONTAGEM. `ContingencyRegistrationFields` organiza identificação, origem na planilha, dependências, dados do fato e justificativa. Identidade do fato e fonte têm espaço próprio; tipo/efeito informado e cliente/armazém ficam alinhados. Dependências usam identidade numerada, inclusão verde e remoção vermelha, sem caixas recursivas. Cabeçalho/fechamento e confirmação permanecem acessíveis na rolagem.

`ContingencyData` apresenta Dados da contagem com UUID da unidade, Versão da unidade, Quantidade contada, Instante da observação e Motivo da contagem. O caminho interno repetido foi removido dos rótulos; a estrutura `dados.dados` permanece no comando. Justificativa do registro é outro campo, no nível original do registro de ocorrência. Os demais tipos continuam usando metadados e `Fields`, com rótulos sem o prefixo duplicado. Ajuda no catálogo `information.ts`. Nenhuma alteração de cálculo, conciliação, ajuste, identidade, permissão ou validação de negócio.

**Conferência:** 29 testes em cinco arquivos, cinco casos novos para contagem/dependências/payload, bloqueio/conflito, troca de tipo, chegada pelo contrato existente e perfil Operação. Tipagem/lint/formatação/build aprovados. Chrome fictício em 16 layouts de contagem/chegada, claro/escuro em 390/768/1024/1440 px, sem overflow; IDs HTML únicos, teclado/toque, comandos na rolagem, IDs Long, decimais/instantes e motivos exatos conferidos. Capturas desktop/celular inspecionadas. [Galeria](../../orchestracao/.runtime/contingency-form-20261015/galeria.html), [recibo](../../orchestracao/.runtime/contingency-form-20261015/validacao.json).

Seletores pertinentes da contingência em `tests/browser/fe12.spec.ts` atualizados; a suíte integrada completa não foi executada neste pedido. Preview próprio encerrado, sem SQL/backend, reinício operacional ou publicação. Versão ativa não medida; prova fictícia separada da inspeção FE13-MANUAL01.

## Botões das notas de mercadoria — FE05-NOTAS-ACOES01

Pedido de Lucas pela tabela de notas no detalhe do pedido de entrada. `Result` aplica a apresentação somente a `List<PedidoEntradaDto.Nota>`: Ver detalhes usa roxo, Selecionar nota usa verde e a coluna se chama Ações. Controles empilhados com a mesma largura, espaçamento consistente e texto sem quebra; altura mínima de 44 px no celular. Tokens existentes preservam temas, hover e foco. Nomes acessíveis identificam número e série, inclusive série zero. Expansão nativa e callback com a nota/tipo original permanecem; apresentação sem seletor quando não existe `onSelect`. Catálogo de ajuda não alterado, pois o fluxo permanece igual.

Concluído em 16/10/2026: sete testes existentes de apresentação, recebimento e registros relacionados; tipagem/lint/formatação/build aprovados. Chrome fictício nos temas claro/escuro em 390/768/1024/1440 px: oito layouts, alinhamento, texto, teclado, toque e rolagem contida na tabela. Abrir detalhes não seleciona automaticamente; seleção preserva objeto, tipo, ID Long, número com zeros e série zero. [Galeria](../../orchestracao/.runtime/note-table-buttons-20261015/galeria.html), [recibo](../../orchestracao/.runtime/note-table-buttons-20261015/validacao.json). Sem SQL/backend, reinício operacional ou publicação; versão ativa não medida. Prova local separada da inspeção FE13-MANUAL01.

## Cartões do histórico — FE13-AUDIT-GRADE01

Pedido de Lucas em 16/10/2026 pela imagem do Histórico de alterações com cinco registros. `AuditSelection` coloca a introdução acima da grade, em vez de disputar largura com os cartões. Grade com colunas de mesma largura, botão abaixo da identificação e alinhado aos demais da linha; nomes e códigos longos permanecem completos. O rótulo mostra somente o tipo, e o cabeçalho explica que são registros selecionados. No celular os cartões empilham e o botão ocupa a largura disponível, com toque de 44 px. Cor roxa e nomes acessíveis específicos permanecem. Catálogo `information.auditHistory` e consumidor revisados.

Destinos, seleção, bloqueio, vazio e consulta separada do legado preservados. Conferidos 16 testes existentes em cinco arquivos: navegação de auditoria, namespaces/legado, seleção de serviço, workspace de preços e popups. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, mais os 17 alvos em desktop/celular: sem sobreposição ou overflow; cinco ações por teclado mantêm DTO e ID Long. [Galeria](../../orchestracao/.runtime/audit-cards-layout-20261016/galeria.html), [recibo](../../orchestracao/.runtime/audit-cards-layout-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida. Inspeção FE13-MANUAL01 preservada.

## Detalhe da alteração — FE13-AUDIT-DET01

Pedido de Lucas em 16/10/2026 pelo detalhe de auditoria com JSON extenso. `AuditDetail` apresenta a ação, os metadados do registro, a justificativa e uma comparação Antes/Depois. O JSON recebe indentação em fonte monoespaçada e altura limitada com rolagem por teclado; celular empilha os painéis. Ver JSON original abre a string recebida; Identificadores da auditoria conserva códigos, IDs, operação e instante original. Título Detalhes da alteração, ícone de histórico e cabeçalho/fechamento fixos no `RecordDialog`. Explicações em `information.auditSnapshots`, `auditSnapshotText` e no catálogo de legado existente.

A formatação usa a dependência `lossless-json` existente, sem arredondar Longs/decimais, substituir strings do contrato ou indexar snapshots como registros selecionáveis. Texto inválido é preservado como texto React, sem interpretação de HTML. Dado ausente é diferente dos valores JSON zero/falso/nulo. Códigos desconhecidos permanecem legíveis no valor recebido; origem física e aviso do legado seguem distintos do histórico confirmado. Seleção mantém objeto e tipo originais, bloqueada durante atualização; consulta sem callback não oferece seleção.

Conferência: 22 testes em seis arquivos, seis casos novos; teste do legado atualizado para abrir o detalhe e conferir o aviso específico. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px sem overflow, JSON original exato, IDs/decimais/instantes precisos, teclado, foco após Escape, criação/ausência, texto inválido, legado e somente leitura. [Galeria](../../orchestracao/.runtime/audit-detail-20261016/galeria.html), [recibo](../../orchestracao/.runtime/audit-detail-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação. Versão ativa não medida; FE13-MANUAL01 preservada.

## Motivo na conferência — FE02-CONF-MOTIVO01

Pedido de Lucas em 16/10/2026 pela confirmação de inativação com o motivo apertado em meia coluna. `Result` identifica os campos escalares por `data-field`; somente dentro de `.confirmation`, o campo `motivo` ocupa todas as colunas do seu resumo. Espaçamento interno, fundo discreto, entrelinha de 1,6 e peso regular tornam o texto mais confortável. `white-space: pre-wrap` preserva parágrafos e espaços digitados; palavras longas quebram sem overflow. Conteúdo/ajuda, confirmações, valores e demais apresentações preservados.

Conferência local: 16 testes existentes em três arquivos, tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, texto original/parágrafos, largura total, conteúdo sem espaços e retorno/confirmação por teclado conferidos. [Galeria](../../orchestracao/.runtime/confirmation-reason-20261016/galeria.html), [recibo](../../orchestracao/.runtime/confirmation-reason-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação. Versão ativa não medida; FE13-MANUAL01 preservada.

## Edição de endereço — FE04-ENDERECO-EDIT01

Pedido de Lucas em 16/10/2026 pela edição com peso, dimensões, coleta, tipos de unidade e motivo sem agrupamento. `AddressEditFields` organiza Identificação e localização, Capacidade e dimensões, Coleta e unidades permitidas e Justificativa da alteração. Descrição e motivo têm largura disponível; código/tipo ficam juntos; rua/nível/posição e altura/largura/profundidade formam linhas próprias. Peso em kg e dimensões em metros, rótulos curtos e orientação decimal única no catálogo. Ajuda completa de `information.addressCapacity` recolhida na edição; novo texto em `addressCapacityValues`. Celular empilha campos, tipos de unidade têm alvo de toque de 44 px e os comandos permanecem acessíveis na rolagem.

`OperationForm` passa os grupos/metadados existentes somente na alteração do endereço. `ScalarField`, `UnitTypesField`, políticas de bloqueio e callback original por grupo preservam valores, perfis e validações; campos não agrupados permanecem em `Fields`. Nenhuma conversão numérica, regra de capacidade ou autorização foi transferida ao formulário.

Conferência: 30 testes em quatro arquivos, quatro casos novos de comando exato, conflito, bloqueio e perfil Operação; tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px, sem overflow, dimensões alinhadas e IDs HTML únicos. Ajuda/seleção/confirmar por teclado, toque, cabeçalho/confirmação na rolagem, ID/revisão Long, decimais exatos e combinação PALLET_VOLUME conferidos. Ausência de capacidade/tipos não inventa configuração. [Galeria](../../orchestracao/.runtime/address-edit-20261016/galeria.html), [recibo](../../orchestracao/.runtime/address-edit-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação. Versão ativa não medida; FE13-MANUAL01 preservada.

## Cor de remover medidas — FE02-REMOVER-MEDIDAS01

Pedido de Lucas em 16/10/2026: Remover Medidas físicas recebe `action-remove` no ramo de remoção de objeto opcional de `Fields`. Esse controle compartilhado usa os tokens vermelhos existentes para claro/escuro, hover/foco e estado desabilitado. Callback `onChange(null)`, texto, ajuda e demais regras preservados. Quatro testes existentes do coletor, tipagem/lint/formatação/build aprovados; sem nova execução de navegador/integração. [Recibo](../../orchestracao/.runtime/remove-measures-color-20261016/validacao.json). Sem SQL/backend, reinício ou publicação; versão ativa não medida. FE13-MANUAL01 preservada.

## Confirmação da contagem — FE12-CONTAGEM-CONF01

Pedido de Lucas em 16/10/2026 pela confirmação de leitura física no coletor. `CountConfirmation` substitui o resumo genérico somente em `ContagemController.contar`. Cliente/armazém têm referências próprias; quantidade contada recebe destaque, instante é identificado como horário de Brasília e UUID permanece completo. Justificativa usa largura inteira, peso regular e parágrafos preservados. Dados técnicos da contagem recolhem versão da unidade, ID da operação e instante original. Título Confirmar contagem física, ícone de contagem e modal de 720 px; celular empilha dados. Cabeçalho e ações permanecem acessíveis na rolagem, com foco inicial no retorno.

Textos centralizados em `information.countConfirmation`, distinguindo registro da aplicação de ajuste. Referências, valores exatos e callbacks permanecem nos componentes/fluxos existentes; não há cálculo ou comando de ajuste no resumo. A implementação de `ContagemEstoqueService.contar` foi consultada em leitura, sem execução, para conferir essa distinção. Outras confirmações mantêm sua apresentação.

Conferência: 27 testes em cinco arquivos, três casos novos de referências/retorno, confirmação explícita/envio único/comando exato/conflito e zero/ausências. Tipagem/lint/formatação/build aprovados. Oito layouts Chrome fictícios claro/escuro em 390/768/1024/1440 px sem overflow; justificativa longa, teclado, toque, dados técnicos, Escape e preservação do formulário conferidos. UUID, versão Long, decimal, instante com nanossegundos e motivo permanecem exatos no comando. [Galeria](../../orchestracao/.runtime/count-confirmation-20261016/galeria.html), [recibo](../../orchestracao/.runtime/count-confirmation-20261016/validacao.json). Preview próprio encerrado; sem SQL/backend, reinício ou publicação; versão ativa não medida. FE13-MANUAL01 preservada.

## Cobertura e fontes

O [inventário por página](inventario-paginas.md) registra a visão anterior, a fonte principal, o detalhe, cada ação migrada, DTOs, campos, permissões, referências obrigatórias e fontes de validação. São 171 ocorrências de ações, incluindo comandos compartilhados entre etapas. O [inventário estruturado](../../frontend/evidencias/record-pages-inventario.json) é gerado pela verificação das definições reais, sem excluir ações para reduzir o denominador.

| Área | Páginas | Adaptação efetiva |
| --- | ---: | --- |
| Cadastros | 9 | Clientes, armazéns, produtos, embalagens, endereços, capacidade, importação de endereços, dados fiscais, encerramentos. Listas por tipo; revisão e situação no registro. |
| Entrada e conferência | 4 | Fila de pedidos; nota/XML, itens, chegadas, divergência e efetivação dentro do pedido. XML mantém seu significado documental. |
| Unidades e etiquetas | 3 | Distribuição parte de pedidos de entrada e entradas efetivadas relacionadas; organização física e etiquetas partem das unidades consultadas. |
| Estoque e rastreabilidade | 4 | Unidades e histórico por código persistente; localização/bloqueio nos comandos próprios; avarias relacionadas à unidade e indicadores por produto. |
| Saída FIFO e reserva | 4 | Pedidos e reservas relacionados; sugestão, exceção FIFO, leitura, separação, reversão e cancelamento mantêm comandos distintos. |
| Fiscal e retirada | 3 | Pedido abre o detalhe da expedição; documentos, conciliação, retirada e devolução mantêm seus estados e confirmações. |
| Serviços e tabelas | 5 | Serviços, tabelas, vínculos, contratos e auditoria; cabeçalhos específicos e versões/vigências nos contratos existentes. |
| Serviços e cálculo | 3 | Fatos, marcos por avaria e cálculos; simulação, resolução histórica e aplicação continuam diferenciadas. |
| Fechamento e ESL | 5 | Ciclos, decisão integral, NFS-e ESL, versões/reabertura e ajustes com origem consultada. ESL continua tratando serviços. |
| Contagem e carga inicial | 3 | Estoque inicial de consulta, contagens e cargas; preparação, revisão, divergências e confirmação preservadas. |
| Contingência | 1 | Ocorrências com contexto, motivo, evidências e conciliação pelo comando existente. |
| Consultas e relatórios | 6 | Estoque, indicadores, entradas, saídas, cálculos e auditoria; filtros e resultados sem criação artificial. |

As 50 páginas foram adaptadas, com política explícita por página em `recordPages.ts`. O escopo não acrescenta entidades ou APIs novas. Futuras páginas dessas áreas devem usar a mesma estrutura com uma fonte e comandos de domínio definidos; uma página prevista sem contrato não deve apresentar uma integração inventada.

## Comportamento entregue

A página consulta sua fonte automaticamente ao entrar. Quando a API exige uma referência ainda ausente, apresenta filtros e orientação para preenchê-la: por exemplo, importação específica, avaria dos marcos financeiros ou registro da auditoria. Não há endpoint de lista global dessas operações; sua visão principal é a consulta contextual disponível.

O cabeçalho apresenta criação ou início específico quando existe no domínio. Nome/identificador e Ver detalhes abrem o registro pelo ID persistente. Editar carrega o detalhe atual antes de preencher o formulário e recusa resposta de outro ID. Operações de situação e processamento ficam no mesmo diálogo do registro. Formulários e confirmação continuam usando os DTOs e comandos anteriores, incluindo código, revisão, motivo, responsável e demais campos exigidos.

Clientes é a referência: lista imediata, Novo cliente, Editar, consulta atual e atualização após confirmação. A transição real é **Solicitar desativação**, de ATIVO para ENCERRAMENTO_PENDENTE. Reativar cancela o encerramento pendente quando permitido. A revisão do encerramento conserva os impedimentos e a conclusão controlada. Um cadastro definitivamente INATIVO não recebe reativação genérica: o serviço existente a recusa. Não há alteração direta de booleano.

CPF/CNPJ apresenta o documento recebido. A resposta básica de Cliente contém ID, revisão, código, nome, documento e situação. Cidade está no complemento fiscal consultado separadamente; a relação cliente–armazéns não está disponível nesse DTO nem é um vínculo cadastral demonstrado pelo modelo existente. Essas duas colunas não foram preenchidas com valores inventados ou com o armazém selecionado no topo. Para acrescentá-las à lista, é necessária uma definição da relação e de sua leitura backend; múltiplos vínculos devem ser preservados.

## Estrutura compartilhada e diferenças de domínio

`RecordWorkspace` organiza cabeçalho, ações, filtros, lista/overview, loading, vazio, erro, retorno e paginação. `RecordTable` apresenta colunas adequadas aos principais DTOs, situação e ações por registro. `RecordDialog` carrega o detalhe, protege edição e organiza os comandos; `useRecordQuery` cancela leituras antigas e preserva a consulta aplicada. `rowContext` separa ID do pedido, unidade, avaria, marco e versão financeira. Os formulários, resultados operacionais e confirmações permanecem específicos dos módulos.

Os diálogos usam o componente nativo do design system, tokens `--og-*`, Inter local, ambos os temas e foco inicial/retorno. Operações extensas usam a superfície larga com rolagem, seções de dados relacionados e etapas específicas. A confirmação é interna ao diálogo; não cria um segundo modal. Referências consultadas também aparecem dentro dele. No mobile, a superfície aproveita a tela e mantém botões acessíveis; tabelas rolam horizontalmente dentro da página.

Durante envio, confirmação pendente ou resposta desconhecida, a edição fica protegida. Erro preserva os campos. Cancelar/Escape, mudar etapa, módulo, tipo de cadastro ou contexto e sair da aplicação verificam alterações pendentes. Leituras não alteram a situação. A API continua responsável por autorização, concorrência, regras e efeitos transacionais.

Pedidos de entrada apresentam entradas efetivadas e progresso relacionado antes da unitização. Selecionar uma entrada ou reserva é explícito. Listar uma única linha não seleciona automaticamente sua identidade. A seleção de outra reserva elimina referências de etiqueta antigas; a revisão vem da etiqueta correspondente. Avarias usam o ID da ocorrência, mantendo separado o ID da unidade. Marcos financeiros usam a avaria, sem substituir seu ID pelo ID do marco.

## Busca, contexto e atualização

Filtros remotos são somente os oferecidos pelo contrato. A busca e a situação identificadas como **nesta página** filtram a resposta atual; não prometem uma busca global inexistente no backend. Paginação remota usa total/tamanho recebidos; listas não paginadas recebem paginação local de dez registros. A página é corrigida quando desaparece após uma atualização. Pesquisa e filtros aplicados permanecem durante a atualização.

Sucesso exige resposta de confirmação. O registro confirmado pode ser aberto mesmo quando os filtros ativos o ocultam. Uma repetição idempotente mantém a confirmação original separada dos dados atuais. Resposta desconhecida mantém aviso próprio, bloqueia novos payloads e permite consultar o estado por GET. Encerrar esse acompanhamento requer reconhecimento explícito e não declara sucesso. O modo fictício identifica seu resultado e permanece separado do transporte HTTP real.

O cliente/armazém do registro e os campos de identidade/revisão preenchidos ficam protegidos na edição. Mudança do contexto aplicado remonta e cancela a consulta anterior após a guarda de edição. Seletores auxiliares tratam referências de domínio; a visibilidade frontend não substitui a permissão aplicada pelo serviço.

## Navegação

As URLs `#cadastros`, `#entrada`, `#unidades`, `#estoque`, `#saida`, `#fiscal`, `#precos`, `#cobranca`, `#fechamento`, `#contagem`, `#contingencia` e `#relatorios` continuam válidas. Abrir a URL principal mostra a visão principal. Os encaminhamentos internos com ação preservam a referência consultada e abrem o comando em seu contexto.

O frontend anterior não tinha rotas públicas de detalhe por ID ou URL própria para cada aba de ação. Nesta entrega, o modal também não cria URL: atualizar a página retorna à visão principal do módulo e mantém as preferências/contexto já persistidos pelo produto. Voltar/avançar entre hashes usa a proteção de edição. Não foi criado um segundo fluxo concorrente para executar o mesmo comando.

## Tabelas, badges e ações — FE02-REG01-A01

Pedido expresso de Lucas em 09/10/2026 após a imagem de Clientes: melhorar a organização visual e acrescentar cores às situações e aos botões. O ajuste usa os tokens e ícones existentes de [design.md](design.md), com as mesmas cores semânticas nos temas claro e escuro. Aplica-se à tabela compartilhada das páginas por registro e aos badges da lista/detalhe de Entrada e conferência (FE05-PED01).

Filtros ficam em uma superfície discreta; busca ocupa o espaço flexível e situação tem largura menor. O título da tabela identifica a lista, a contagem fica separada, nomes têm espaço mínimo, códigos/documentos usam algarismos alinhados e as ações ficam compactas. Linhas têm divisores leves e realce ao passar o ponteiro ou receber foco. Documentos não são cortados para caber. No mobile, a tabela rola dentro do seu contêiner e os controles mantêm alvos de pelo menos 44px.

`StatusBadge` apresenta rótulos legíveis, texto, ponto de cor, fundo e borda discretos. A classificação usa uma lista explícita de valores; não procura palavras parcialmente. Os valores originais do contrato e dos filtros permanecem intactos. Situações desconhecidas conservam seu texto e recebem cor neutra.

| Cor | Exemplos de apresentação |
| --- | --- |
| Verde | Ativo, Boa, Conferido, Efetivado e outras conclusões identificadas |
| Âmbar | Encerramento pendente, Quarentena, Triagem, Conferência pendente ou com divergência |
| Vermelho | Avariada, Divergente, Bloqueado e recusas identificadas |
| Azul | Rascunho, Em conferência, Reservado e operações em andamento identificadas |
| Neutro | Inativo, Cancelado, Estornado, substituições e valores não classificados |

Cor indica apresentação da situação recebida, sem liberar estoque, autorizar comandos ou inferir confirmação. Conferido e Efetivado continuam distintos. O mesmo badge é reutilizado nos resultados de conferência e na situação operacional do pedido.

Editar e a ação principal da conferência usam azul sólido. Ver detalhes e Atualizar lista usam azul discreto com ícone e texto; ações destrutivas identificadas na tabela usam a cor de erro. Contraste, foco, desabilitação, permissões e confirmações mantêm suas funções anteriores.

**Conferência local:** tipagem, lint, formatação e builds real/fictício próprios aprovados; [43 testes focais](../../frontend/evidencias/record-colors-a01-focal.json), [sete casos Chrome de apresentação](../../frontend/evidencias/record-colors-a01-browser.json) e [duas jornadas existentes de Clientes](../../frontend/evidencias/record-colors-a01-regression-browser.json) passaram. Cobertura: 20 linhas, filtros, permissão de Operação, detalhes por teclado/Escape/retorno do foco, criação/edição/reativação, ambos os temas, 1440px/360px, alvos de toque, documentos inteiros, tabela de Clientes sem rolagem horizontal no desktop do recorte e página sem vazamento horizontal no mobile. Contraste de texto foi medido no navegador, com mínimo 4,5:1 para os cinco tons e os botões da linha. Capturas foram inspecionadas.

O Chrome usa o build real com HTTP interceptado e dados fictícios; o build fictício permanece separado. Não houve escrita real, backend, SQL Server, migration, publicação ou reinício de processos existentes. `graphify update .` executou a extração AST local, mas recusou substituir o mapa por uma extração menor (13.131 contra 15.220 nós registrados pela ferramenta); sem `--force`, sem atualização bem-sucedida declarada. O [recibo do ajuste](../../orchestracao/.runtime/record-colors-a01/resultado.json) registra fontes, artefatos e o log dessa recusa. Carregar a fonte atual na aplicação é a conferência seguinte; a integração real continua na etapa de ambiente correspondente.

## Filtros visíveis sem sanfona — FE02-REG01-A03

Lucas rejeitou a sanfona isolada de Filtros e contexto da consulta e o efeito de cartão dentro de cartão em 09/10/2026. O formulário de filtros de `RecordWorkspace` agora fica sempre visível, direto na superfície da página, com o único título Filtros da consulta. A borda e o espaçamento interno do fieldset foram removidos; o agrupamento semântico continua identificando os campos e desabilitando-os durante a consulta. Não há controle de abrir/fechar esse bloco. A regra vale para todas as páginas que usam esses filtros; páginas sem parâmetros além da paginação continuam sem um bloco artificial de filtros.

Aplicar filtros, Restaurar filtros, referências, consultas, permissões, validação e valores recebidos mantêm os contratos anteriores. O auxiliar dos testes passou a reconhecer a orientação de referências obrigatórias, preservando quando deve executar a consulta, sem depender do atributo de abertura removido.

Tipagem/build real próprio, lint/formatação e [74 testes existentes em 17 arquivos](../../frontend/evidencias/record-filters-a03-regression.json) passaram, incluindo páginas por registro e jornadas que utilizam o auxiliar alterado. [Seis casos Chrome](../../frontend/evidencias/record-filters-a03-browser.json) conferiram filtros de Entrada (dois campos) e Estoque (sete campos), claro/escuro em 1440/768/360px: visibilidade antes/depois da consulta, ausência de sanfona e borda interna, campos dentro da tela, teclado nos seletores e na aplicação, bloqueio durante espera, preservação/restauração de valores e alvos de toque. Clientes não ganhou um formulário vazio. Capturas de desktop, tablet e celular foram inspecionadas.

O navegador usou os assets servidos pelo frontend DEV existente em `http://127.0.0.1:25581`, com todas as APIs interceptadas e dados fictícios. Sem operação backend/SQL, reinício ou publicação; essa prova não comprova integração real. `graphify update .` executou a extração AST, mas recusou substituir 15.220 nós por 13.134, sem `--force`; atualização AST não concluída. [Recibo e log do ajuste](../../orchestracao/.runtime/record-filters-a03/resultado.json). Próximo: recarregar a página para carregar a fonte atual.

## Alinhamento compartilhado dos filtros — FE02-REG01-A02

Lucas apontou Atualizar lista deslocado e esclareceu “todas que forem assim” em 09/10/2026. Os estilos locais de registros e mapa já removiam a margem inferior de 8px dos rótulos; essa correção agora fica em `forms.css`, compartilhada por `.record-toolbar label` (incluindo os filtros de recebimento) e `.map-tools label`. Campos e botões se alinham nas linhas compartilhadas; tablet e celular preservam a quebra organizada, sem altura fixa ou alteração de filtros e contratos.

Formatação e tipagem/build real próprio passaram. [Seis casos Chrome](../../frontend/evidencias/record-refresh-a02-browser.json) conferiram os assets servidos pelo frontend DEV existente em `http://127.0.0.1:25581`, nos temas claro/escuro e em 1590/768/360px: barras de Clientes, Armazéns e Estoque, margem zerada, alinhamento com tolerância de 1px quando os controles compartilham a linha, alvos de 44px no celular e página sem vazamento horizontal. Atualizar lista por Enter preservou a busca preenchida. Capturas de desktop, tablet e celular foram inspecionadas.

Todas as APIs foram interceptadas com dados fictícios; essa prova confere a interface servida, sem comprovar integração backend/SQL. O processo DEV existente foi preservado, sem reinício. `graphify update .` executou a extração AST, mas recusou substituir 15.220 nós por 13.133, sem `--force`; a atualização AST não foi concluída. [Recibo e log do ajuste](../../orchestracao/.runtime/record-refresh-a02/resultado.json). Recarregar a página carrega a fonte atual; a URL da imagem enviada não foi identificada.

## Tabelas ajustadas à largura — FE02-REG01-A04

Pedido expresso de Lucas em 10/10/2026: melhorar a tabela de Cadastros que exigia rolagem lateral em monitor de 1500 px. Dependências FE02-REG01/A01/A03; contratos e vínculos BE05 → FE04 e dos demais leitores mantidos.

`RecordTable` usa colunas distribuídas na largura disponível, com coluna de ações definida e largura mínima proporcional ao número de campos. Códigos e documentos extensos podem quebrar linha; os valores completos continuam na tabela, sem elipse ou alteração do conteúdo. Situações longas também podem quebrar, mantendo cor, rótulo e marcador. Cabeçalho e quantidade acompanham a largura da tabela.

Ver detalhes e Editar usam ícones com nomes acessíveis originais, título no hover e indicação de foco. O nome do registro continua abrindo detalhes. As operações específicas mantêm ícone e rótulo textual, com quebra quando necessária. Botões de consulta/edição têm 36 px no desktop e 44 px em mobile/ponteiro de toque. Seus rótulos ocultos ficam contidos no botão, evitando transbordamento da página. Nenhum comando, permissão, revisão, filtro ou contrato foi alterado.

Clientes com códigos/documentos longos da forma mostrada na imagem ocupa toda a área sem rolagem lateral nas larguras verificadas de 1200/1280/1400/1500/1920 px, nos dois temas e com menu aberto/recolhido. Em 1500 px, com menu aberto, a tabela cabe na área disponível e as ações permanecem visíveis. Uma tela estreita ou tabela com muitos campos conserva rolagem interna para manter a leitura; a página não se alarga.

Conferência local: tipagem, lint focal, formatação e builds real/fictício aprovados; 25 testes existentes de páginas por registro e oito casos Chrome. Vinte medições de Clientes; Armazéns em 1200/1500/1920 px, estoque e saída nos dois temas. Conferidos valores longos completos, ações visíveis, contraste, filtro de situação, ausência de edição para Operação, detalhes/edição via Enter, Escape e retorno de foco. Em 360 px, alvos de 44 px e rolagem restrita à tabela. Capturas desktop claro/escuro e mobile inspecionadas. [Medidas](../../frontend/evidencias/table-a04-medidas.json), [recibo](../../orchestracao/.runtime/table-a04/resultado.json).

Navegador com API interceptada e dados fictícios; sem backend/SQL, reinício de processos existentes ou publicação. Previews próprios encerrados. `graphify update .` executou extração AST e recusou substituição integral por extração menor, sem `--force`; [log](../../orchestracao/.runtime/table-a04/graphify.log). A manutenção do grafo permanece separada da entrega visual.

## Verificação e limites

Tipagem, lint e builds real/fictício aprovados. A [suíte completa](../../frontend/evidencias/record-pages-regressao-final.json) passou com **467 testes**; os [25 testes focais](../../frontend/evidencias/record-pages-focal-final.json) incluem todas as páginas, Clientes, resposta desconhecida, IDs de domínios distintos, 41 registros/paginação e recuperação de última página. As jornadas adaptadas também verificam entrada/divergência, unitização, divisão/reagrupamento, etiquetas, avaria, reserva/FIFO, fiscal/retirada, configuração/cálculo/fechamento, carga/contagem, contingência e auditoria.

Os [seis casos Chrome](../../frontend/evidencias/record-pages-browser.json) passaram: todas as 50 páginas em 1440px claro e 360px escuro; criação/edição/cancelamento por Escape, foco, solicitação de desativação, reativação e filtros; erro de carregamento, ausência de resultados, conflito 409 e Operação sem edição. Não houve erro de JavaScript ou requisição a endpoint não previsto. Capturas de desktop/mobile e diálogos foram inspecionadas. O navegador usa o build real com HTTP interceptado e dados fictícios; isso exercita autenticação e transporte do frontend, mas não comprova execução Java/SQL Server. O exercício fictício usa seu transporte separado.

Os builds preservam o aviso preexistente de bundle acima de 500 kB, sem falha. `graphify update .` foi executado; a ferramenta recusou substituir o grafo de 15.220 nós por uma extração menor. O mapa existente foi preservado, sem `--force`; esse impedimento não é apresentado como atualização bem-sucedida.

Nenhum backend, SQL Server, migration, emissão fiscal, impressão em dispositivo, publicação ou processo existente foi iniciado/alterado por este recorte. Integração real no WMS_DEV, equipamentos e versão carregada na aplicação em execução permanecem conferências de ambiente. Não representam páginas omitidas na implementação local.

## Diálogos e formulários compactos — FE02-REG01-A05

Pedido expresso de Lucas em 10/10/2026, a partir das imagens de detalhe de serviço e edição de cliente. Correção nos componentes compartilhados das jornadas, incluindo Cadastros, Entrada, Estoque, Saída, Fiscal, Financeiro, Regularização e Relatórios; não altera contratos ou regras backend.

| Ponto rastreado | Alteração |
| --- | --- |
| `RecordDialog`: faixa de operações sem botões gerava duas linhas e espaço vazio | Só renderiza o grupo se houver comandos; ações secundárias e navegação reunidas no rodapé, com uma separação. |
| `RecordDialog`: acordeão de referências podia envolver componentes que retornavam `null` | Removido o envoltório; consultas e seleções úteis aparecem diretamente. Auditoria auxiliar aparece apenas no comando de auditoria. |
| `RecordDialog`: identificação recolhida repetia dados durante a edição | Removido o acordeão. Formulário mostra as referências fixas; botão Ver detalhes do registro retorna aos dados completos e respeita o descarte de alterações pendentes. |
| `OperationForm`: moldura e legenda genérica acrescentavam uma hierarquia sem utilidade | Campos editáveis em grade sem moldura externa; identificação/revisão fixas em faixa compacta. Grupos sem campos não são renderizados. Sem alterar valores, validação ou conteúdo enviado. |
| `RecordDialog`: botão repetia o comando já aberto | Oculto durante essa ação; reaparece após conclusão quando o domínio permite uma nova operação, incluindo nova chegada física. |
| `Result`: cada coleção vazia gerava um bloco grande | Coleções/objetos vazios aninhados aparecem como item curto com seu rótulo; conteúdo preenchido conserva seção própria. Resposta `{}` informa ausência de dados. Zero, falso e campos não informados permanecem distintos. |
| `Result` e estilos de diálogo | Grade de dados responsiva, sem linha embaixo de cada campo; espaçamentos menores, rótulos discretos, quebra de texto e rodapé adaptado a celular/tablet. |
| `ReferenceLookup`: botão de reabertura sem resultado disponível | Só oferece Ver referências consultadas quando há consulta já obtida e recolhida. |

**Inventário completo de acordeões em `frontend/src`:** oito usos encontrados. Dois removidos em `RecordDialog`; um corrigido em `OperationForm` para aparecer somente se o contrato do coletor contiver medidas/conjunto. Cinco preservados por terem conteúdo útil: identidade consultada do coletor (`Collector`), detalhe por linha de resultados (`Result`), dados para conferência abertos por padrão (`OperationConfirmation`), recibo detalhado do coletor (`CollectorReceipt`) e menu de comandos do pedido (`ReceivingList`). Nenhum desses cinco serve como seção genérica vazia.

As proteções de carregamento/erro, identidade exata, revisão, permissões, alteração pendente, envio, resposta desconhecida, repetição idempotente, Escape e retorno do foco continuam. Interface local com API interceptada não comprova integração real. Conferência final e evidências registradas em `STATES.md` e `orchestracao/.runtime/dialog-a05/resultado.json`.

## Botões da auditoria — FE02-REG01-A06

Pedido expresso de Lucas em 10/10/2026. `AuditSelection`, utilizado em Serviços e tabelas, Relatórios e no diálogo de auditoria, apresenta os botões lado a lado, com intervalo de 8 px e quebra natural quando faltar largura. Rótulos/identificadores extensos quebram dentro do botão sem transbordar. O vínculo de cobrança mantém seu botão, explicação e consulta de legado em um grupo próprio, sem confundir histórico confirmado com legado não atribuído. Sem seleção disponível, permanece a orientação existente e não há faixa vazia. Eventos, bloqueio e mapeamento do alvo da auditoria não mudam.

Tipagem, lint focal, formatação, build real, oito testes de auditoria/consultas e quatro casos Chrome aprovados. Capturas de 1500/360 px nos dois temas inspecionadas, incluindo IDs longos, espaçamento, ordem Tab e alvos de toque. API interceptada; sem validação de backend/SQL ou publicação. [Recibo](../../orchestracao/.runtime/audit-buttons/resultado.json).

## Comandos ao lado dos filtros — FE02-REG01-A07

Pedido expresso de Lucas em 10/10/2026: Aplicar filtros e Restaurar filtros alinhados às caixas, em todas as páginas/etapas com o mesmo padrão. `RecordWorkspace` passa a colocar os comandos no mesmo fieldset flexível dos filtros. Campos crescem conforme o espaço, botões mantêm tamanho próprio e alinham à base dos controles; quando não cabem, o grupo quebra junto para a próxima linha. Até 600 px, Aplicar/Restaurar dividem a faixa seguinte; Atualizar lista de Entrada ocupa a linha posterior. Nas outras páginas, Atualizar lista permanece com a busca local. Estado de carregamento, submissão por Enter, restauração e consultas foram preservados.

Rastreamento estático: uma única implementação dos dois comandos; 49 definições de consulta com campos em 12 módulos no inventário de contratos/páginas. Esse número inclui os quatro mapeamentos históricos de Entrada reunidos atualmente numa tela, conforme FE05-PED01; não representa 49 telas distintas atuais. [Inventário](../../frontend/evidencias/filter-inline-inventario.json).

Conferência local e capturas em [recibo A07](../../orchestracao/.runtime/filter-inline/resultado.json). A apresentação de Entrada complementa [FE05-PED01-A01](etapa-07.md#botões-da-consulta--fe05-ped01-a01), alinhando agora a faixa de comandos aos seletores quando há espaço. Sem mudança em backend, contratos ou regras de consulta.

## Operações compactas nas tabelas — FE02-REG01-A08

Pedido expresso de Lucas em 10/10/2026, com exemplos em Dados fiscais cadastrais e Encerramento e impedimentos. Causa: A04 compactou detalhes/edição e fixou a coluna de ações, mas a operação contextual ainda exibiu o rótulo inteiro nessa mesma coluna. O texto quebrava em muitas linhas, determinando a altura excessiva de todos os campos do registro.

`RecordTable` usa agora o mesmo botão compacto para todas as três variantes: detalhes, edição e operação contextual. Operações mantêm o ícone de seta, nome completo no título/hover e texto para leitores de tela; destinos, permissões, bloqueio durante carregamento e confirmação posterior permanecem. A faixa não quebra entre os dois botões. Alvos de toque continuam com 44 px; a altura da linha passa a depender do conteúdo dos dados, não do comprimento do comando. Nomes/códigos continuam completos, com quebra quando necessária.

Rastreio: o problema estava no único ramo `canOperate` do componente compartilhado, alcançando todas as tabelas que o utilizam. Também foram inspecionadas a tabela genérica `Result` e a lista especializada `ReceivingList`: não usam essa coluna fixa de ícones com o ramo textual defeituoso. Regras dessas outras apresentações foram preservadas.

Conferência: tipagem, lint focal, formatação/build real, 25 testes existentes e dois casos Chrome aprovados. Cinco visões (fiscal e encerramento de clientes/armazéns e lista comum de clientes), quatro larguras (1500/1280/1024/390) e dois temas: 40 combinações, botões lado a lado, dimensões, títulos/nomes acessíveis, toque, Tab/Enter/Escape, abertura do formulário e retorno do foco. Desktop sem rolagem lateral nos cenários conferidos; mobile mantém rolagem interna da tabela quando necessária e não transborda a página. Capturas inspecionadas. [Recibo](../../orchestracao/.runtime/table-actions/resultado.json). API interceptada; sem backend/SQL/reinício/publicação. Graphify recusou substituir o índice por extração menor, sem forçar.
