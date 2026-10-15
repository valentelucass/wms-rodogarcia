# Listas, detalhes e ações por registro — FE02-REG01

Pedido expresso de Lucas em 09/10/2026. Esta entrega reorganiza as 12 áreas e suas 50 páginas existentes. Substitui as abas que separavam consulta, criação e comandos dentro de cada página. Mantém módulos e etapas reais da jornada, os contratos backend e a fundação visual de [design.md](design.md) e [etapa-03.md](etapa-03.md).

**Complemento FE05-PED01:** Entrada e conferência passou a reunir seus quatro grupos de capacidades na única visão Pedidos de entrada, com seções internas. A contagem de 50 páginas/171 ocorrências abaixo registra o inventário original de REG01, preservado para rastrear comandos; não representa quatro etapas globais atuais de Entrada. [Comportamento atual](etapa-07.md).

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
