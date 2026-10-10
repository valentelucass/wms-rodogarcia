# FE02-CTX01 / BE14-DASH02 → FE02-DASH02 — seleção por nome e visão geral

Pedido expresso de Lucas em 09/10/2026: implementar os seletores de cliente e armazém e ajustar o Início conforme o texto anexado. A composição inicial desta entrega substituiu o Início de DASH01. Em 10/10, Lucas pediu reincluir os gráficos e exibi-los também com Todos; a composição vigente está no [incremento A03](#gráficos-no-início-e-opção-todos--be14-dash01-a01--fe02-dash02-a03). As regras de estoque e cobrança continuam nos serviços existentes.

## Popups informativos — FE02-INFO01

Pedido expresso de Lucas em 10/10/2026: apresentar explicações ao passar o mouse sobre indicadores/cartões e centralizar seu conteúdo. O catálogo único é [information.ts](../../frontend/src/content/information.ts); reúne títulos, explicações, rótulos e composição da prévia de posição. Valores/situações vêm dos dados já apresentados, inclusive informação ausente e acesso restrito. Ao mudar a função de um consumidor, revisar sua entrada no catálogo, conforme AGENTS.md. O texto não recalcula estoque, disponibilidade ou cobrança.

O componente compartilhado [InformationPopup.tsx](../../frontend/src/components/information/InformationPopup.tsx) atende os dez indicadores do Início, quatro indicadores da visão operacional quando exibidos, quatro atalhos, ajuda dos três gráficos, orientação do mapa e prévia das posições. Os atalhos mantêm seus destinos; clique/Enter na posição continuam abrindo detalhes. Informações essenciais permanecem visíveis fora do popup.

A abertura por mouse espera 260 ms; sair permite 180 ms para alcançar o popup. Foco de teclado abre imediatamente, Escape fecha sem deslocar foco e somente uma ajuda fica aberta. Clique externo, deslocamento do elemento por rolagem, redimensionamento, ocultação e desmontagem encerram a prévia. Rolagem dentro dela é permitida; eventos atrasados da rolagem automática anterior à abertura não a cancelam. Em toque, indicadores abrem ao tocar e gráficos/mapa têm botão de informação; atalhos e posições continuam com sua ação direta.

Popup com título, descrição e dados alinhados, largura de até 320 px, margem mínima de 8 px da janela, inversão para cima quando necessário, limite de altura e rolagem interna. Entrada suave de 140 ms, desativada com movimento reduzido. Superfícies e contraste acompanham o tema. Após ajuste solicitado por Lucas, o “i” visual mede 18 px, tem contorno/cor de destaque e fica junto ao título; área de interação de 24 px no desktop e 44 px em toque.

Evidências e limites da conferência: [recibo FE02-INFO01](../../orchestracao/.runtime/information-popups/resultado.json). Preview isolado com respostas HTTP fictícias interceptadas; não comprova execução em backend/SQL Server real.

## Seleção de contexto

Cliente e Armazém usam seleção pesquisável por nome ou código, no topo e nos campos correspondentes dos formulários operacionais. O catálogo vem dos endpoints existentes de cadastros, percorre todas as páginas e respeita o alcance retornado pelo backend. Os cadastros são feitos em Cadastros; não se cria registro ao digitar um nome. O ID permanece no contrato e no banco, sem exigir memorização pelo operador. Registros inativos são identificados para consultas históricas; a validação de cada operação permanece no backend.

Todos os clientes e Todos os armazéns significam todos os registros permitidos ao usuário. A escolha deve ser aplicada no topo. O seletor do mapa aplica o armazém diretamente ao mesmo contexto, preservando apenas o cliente e descartando referências anteriores. Campos obrigatórios de operações continuam exigindo seleção concreta. Há busca sem diferenciar acentos, teclado, identificação de opções por código, carregamento, vazio, erro e nova tentativa. Respostas antigas são descartadas ao alterar contexto/perfil/alcance; catálogos são atualizados após registros e pela atualização da visão.

O ajuste visual CTX01-A01 conserva seus critérios em [etapa-03.md](etapa-03.md#espaço-dos-seletores--fe02-ctx01-a01).

## Indicadores do Início

Título Início, descrição Visão geral da operação e os quatro acessos rápidos existentes. O painel exibe dez cards mesmo sem movimentações. Zero confirmado é diferente de informação indisponível, exibida como —. Cores, Inter, espaçamentos, raios, superfícies, estados e diálogos usam a fundação de [design.md](design.md); nenhuma faixa decorativa foi acrescentada.

| Indicador | Definição da leitura |
| --- | --- |
| Ocupação | Posições de armazenagem ocupadas / capacidade física considerada × 100; arredondamento HALF_UP para inteiro. 2/80 = 3%. Capacidade zero resulta em 0%, sem divisão |
| Posições ocupadas | Endereços ativos de ARMAZENAGEM, em armazéns ativos, com ocupação física vinculada. Uma unidade pode ocupar mais de uma posição |
| Posições livres | Os mesmos endereços de capacidade, sem unidade vinculada. Endereços inativos e áreas especiais não são livres |
| Unidades armazenadas | Unidades logísticas ativas do alcance cliente/armazém; não é quantidade de produto nem contagem de endereços |
| Valor armazenado | Soma do valor exato do indicador de estoque existente, incluindo seu tratamento de origem, avaria e entradas conferidas sem unitização. Informação incompleta torna o agregado indisponível. Cargas em preparação não viram estoque confirmado |
| Em quarentena | Contagem de unidades logísticas ativas em QUARENTENA, identificada como unidades; não soma quantidades de SKUs com medidas diferentes |
| Reservas ativas | Registros de reserva com situação ATIVA no alcance selecionado |
| Entradas abertas | Pedidos diferentes de EFETIVADO e CANCELADO |
| Saídas abertas | Pedidos RASCUNHO, RESERVADO, EM_SEPARACAO ou SEPARADO |
| Faturamento do mês | Saldos positivos de versões com estado externo EMITIDO e NFS-e registrada com emissão no mês civil atual do fuso informado. Cada versão entra uma vez, mesmo com várias referências. Canceladas não entram; saldo desconhecido torna o valor indisponível. É parcial do mês, sem antecipar emissão ou somar cálculos em elaboração |

O percentual e as posições consideram todos os clientes dos armazéns selecionados, para representar espaço físico real. Unidades, pedidos, reservas, quarentena e valores seguem também o filtro de cliente. A distinção está escrita no painel. Supervisor/Gestor podem consultar os valores financeiros; Operação recebe esses campos nulos. O backend valida o perfil e o alcance em cada leitura.

## Mapa e detalhes

O mapa vem de todos os endereços cadastrados nos armazéns permitidos, agrupados por armazém, rua, nível decrescente e posição. Quantidades de ruas, níveis e posições não são fixadas. As colunas mantêm a relação entre as posições dos níveis, inclusive quando há lacunas. Endereços disponíveis usam verde; ocupados, vermelho; áreas especiais/inativos têm estado próprio. Legenda, texto e nomes acessíveis acompanham as cores.

Busca por código e filtros Todas/Disponíveis/Ocupadas afetam somente o mapa. A paginação de 100 endereços mantém todos os cadastros alcançáveis; as setas percorrem as colunas da rua nesta página, conforme a apresentação vigente de [A06](#mapa-do-início-conforme-referência--fe02-dash02-a06). Os totais continuam do contexto inteiro. O mapa identifica o total de endereços; quando há mais de uma página, mostra também o intervalo e os controles de paginação. Informações de capacidade ficam nos indicadores e na explicação expansível. Vazio, busca sem resultado, carregamento e falha são explícitos.

Cada posição abre um diálogo com código completo, armazém, rua, nível, posição, estado, tipo, capacidade de peso/dimensões/empilhamento/unidade permitida e ocupação. Para conteúdo autorizado, mostra produto, SKU, lote, quantidade, etiqueta, pedido de origem, bloqueio, reserva, quarentena e último movimento de estoque registrado. Ausência de movimento não é substituída pela data de edição. Conteúdo de outro cliente fora do alcance fica oculto; somente sua ocupação física permanece visível.

Endereçar está disponível para endereço livre com cliente selecionado. Abre o Coletor existente com armazém e destino preenchidos, visíveis desde a leitura. Caso o endereço esteja fora da página de posições consultada pelo coletor, é buscado pelo ID e o armazém é conferido. Ler, escolher ou abrir detalhes não grava movimentação. O comando exige leitura da unidade, conferência do destino e confirmação existente; permissões, disponibilidade e capacidade são revalidadas pelos serviços.

## Mapa compacto e navegação por setas — FE02-DASH02-A04

Pedido expresso de Lucas em 10/10/2026: reduzir a altura dos cartões, organizar as ruas e usar setas para mostrar as posições anteriores/próximas. Ruas passam a ter divisões finas, sem grandes painéis internos. Cartões comuns medem 44 px de altura; bloqueio ou reserva acrescentam uma linha e totalizam 54 px. Verde/vermelho, estado textual, código e acesso aos detalhes permanecem.

Cada rua calcula quantas colunas cabem na largura disponível. Quando necessário, setas laterais mostram o grupo anterior/próximo, com indicação de colunas visíveis e controles desabilitados no início/fim. O mesmo grupo vale para todos os níveis da rua, mantendo lacunas e alinhamento vertical; posições têm ordem numérica/textual crescente e níveis, decrescente. Em telas pequenas, o nome da rua fica acima dos níveis. Não há barra horizontal. As setas trabalham sobre a página recebida; a paginação de endereços continua separada.

Conferência local: tipagem, lint focal, formatação, builds real/fictício e oito testes existentes aprovados. Chrome com API interceptada: dois casos, incluindo 16 medições em oito larguras de 320 a 1920 px, nos dois temas; três casos do exercício aprovados. Verificados lacunas, estados, ausência de transbordamento, todas as 20 colunas do cenário, ida/volta, limites, redimensionamento, Tab/Shift+Tab/Enter/Escape e retorno do foco após fechar detalhes. Cartão anterior de 64 px passou a 44 px; a rua do cenário que quebrava posições do mesmo nível em duas linhas passou de 208 para 69 px em 1500 px. Capturas claro/escuro em desktop/mobile inspecionadas. [Medidas](../../frontend/evidencias/map-a04-medidas.json), [recibo](../../orchestracao/.runtime/map-a04/resultado.json). Sem integração SQL ou reinício do DEV nesta alteração visual; previews próprios encerrados.

`graphify update .` executado somente AST: substituição integral recusada por extração de 13.286 contra 15.220 nós, sem `--force`. Houve poda de quatro nós de arquivos ignorados; não se declara atualização integral do grafo. [Log](../../orchestracao/.runtime/map-a04/graphify.log).

## Armazéns com fundos alternados — FE02-DASH02-A05

Pedido expresso de Lucas em 10/10/2026: distinguir cada armazém no empilhamento com fundo alternado. Os blocos completos, do título à última rua, alternam superfície normal e superfície elevada neutra dos tokens existentes. Espaçamento e cantos discretos delimitam os grupos; os rótulos de nível acompanham o fundo. A margem compensa o preenchimento lateral para preservar a largura disponível às posições. Cores semânticas, altura dos cartões e navegação por setas de A04 permanecem.

Conferência proporcional ao ajuste CSS: formatação/build real aprovados; Chrome com API interceptada, quatro armazéns fictícios em seis composições de 320/390/1500 px, claro/escuro. Verificados alternância, separação vertical, rótulos, cartões de 44/54 px, ausência de transbordamento e ida/volta pelas setas. Capturas desktop claro e mobile escuro inspecionadas. [Medidas](../../frontend/evidencias/map-a05-medidas.json), [recibo](../../orchestracao/.runtime/map-a05/resultado.json). Preview próprio encerrado; sem backend, SQL ou reinício do DEV. Graphify AST recusou substituição integral por extração menor, sem forçar; limite registrado no recibo.

## Mapa do Início conforme referência — FE02-DASH02-A06

Pedido expresso de Lucas em 10/10/2026, com imagem de um mapa compacto: aproximar a apresentação da referência, com organização simples e alta densidade. Dependência BE14-DASH02 → FE02-DASH02-A04/A05. Esta composição substitui a apresentação de cartões de A04, preservando fundos alternados entre armazéns de A05.

- Cabeçalho discreto, seletor de armazém compacto e legenda juntos. Busca, estado e explicações ficam em Filtros, recolhido inicialmente e acessível por teclado; quando aplicado, o filtro continua indicado mesmo recolhido.
- Rua identificada acima da grade. Níveis descendentes com rótulo numérico discreto e nome completo acessível; posições em ordem natural e lacunas preservadas. As setas e o intervalo ficam no cabeçalho da rua, sem recuar as grades que precisam de navegação.
- Células de 64 × 28 px no desktop; 64 × 44 px em telas até 700 px ou dispositivos de ponteiro de toque. Código cadastrado visível, verde/vermelho preenchidos com contraste nos dois temas e estado indisponível neutro. Código extenso tem elipse, nome acessível completo, descrição no hover e detalhe ao abrir; nenhum código é reescrito.
- Bloqueio e reserva aparecem como marcadores independentes, inclusive simultaneamente; os caracteres iniciais `!` e `R` foram substituídos pelos ícones de [A07](#ícones-do-mapa--fe02-dash02-a07). Estado e avisos completos continuam no nome acessível, descrição e diálogo. Cor de livre usa somente disponibilidade retornada pelo backend.
- Nome visual do armazém omitido quando há um único grupo, preservando identificação acessível; múltiplos grupos conservam títulos e fundos alternados. Uma página mostra apenas o total de endereços; mais páginas conservam a paginação existente.

Conferência local: tipagem, lint focal, formatação e builds real/fictício aprovados; oito testes de contexto e cinco casos Chrome. Doze medições em 320/390/768/1024/1500/1920 px e claro/escuro: altura, largura, alinhamento entre ruas/níveis, lacunas e ausência de transbordamento conferidos. Todas as 20 colunas do cenário percorridas por ida/volta; bloqueio/reserva simultâneos, filtro ativo/recolhido, escolha de armazém, paginação, erro/vazio, detalhes, destino e Enter/Escape/retorno do foco verificados. Capturas desktop claro e mobile escuro inspecionadas. [Medidas](../../frontend/evidencias/map-a06-medidas.json), [recibo](../../orchestracao/.runtime/map-a06/resultado.json).

API interceptada e exercício explícito são provas de interface, sem backend ou SQL Server reais. Previews próprios encerrados; processos existentes preservados. Graphify AST tentou atualizar o mapa e recusou substituição integral por extração menor, sem `--force`; [log](../../orchestracao/.runtime/map-a06/graphify-final.log). Não se declara atualização integral do grafo.

## Contratos e atualização

`GET /api/v1/visao-operacao`: cliente/armazém opcionais, fuso obrigatório, código/estado/página/tamanho. `GET /api/v1/visao-operacao/posicoes/{id}`: detalhe autorizado do endereço. Controller → serviço → repository, leitura transacional no mesmo isolamento dos indicadores existentes, sem migrations. Contratos frontend regenerados da fonte Java.

A consulta atualiza ao entrar/voltar ao Início, aplicar contexto, mudar armazém, paginar, filtrar ou clicar Atualizar visão. Após confirmação de operação, a revisão do workspace/catálogo é invalidada. Leituras canceladas ou atrasadas de outro contexto não substituem a tela. O transporte real usa autenticação existente; o exercício fictício é separado e não serve de fallback.

## Conferência e limites

Dez testes locais backend (cinco de arquitetura, três de integração JPA/H2 isolada e dois de serviço), `verify` e artefato próprio. Cobertura de permissões, conteúdo restrito, agregação de armazéns, ordenação, filtro independente, capacidade zero, duas posições/uma unidade, 2/80 → 3%, valores incompletos/não zero e mês civil. A suíte frontend passou com 441 testes; o último incremento do coletor recebeu conferência focal adicional. Tipagem, lint e builds real/fictício separados. Dezoito cenários Chrome com HTTP interceptado e três do exercício fictício cobrem temas, telas pequenas, nomes, Long exato, Bearer, erros/zeros, paginação, detalhes/foco, encaminhamento sem escrita e regressão de login/contas.

Conferência focal final: 23 testes, incluindo destino fora da página, resposta atrasada e catálogo com mais de uma página. Dois casos nativos finais repetiram nomes/Long/valores restritos e o destino visível no coletor. Provas locais em `frontend/evidencias/visao-operacao*-browser-resultados.json`; [recibo da entrega](../../orchestracao/.runtime/visao-operacao-resultado.json). Capturas mantidas fora do Git. Não foram executados SQL Server, migrações, reinícios, publicação ou operação real. Dialeto/plano/desempenho em SQL Server, dados operacionais, NFS-e real, equipamento e versão já em execução permanecem verificações de ambiente, sem converter testes isolados em homologação.

`graphify update .` foi executado na raiz, somente AST, e recusou reduzir o mapa de 15.220 para 12.961 nós; o mapa principal foi preservado, sem `--force`. A manutenção do grafo permanece separada da entrega.

## Gráficos no Início e opção Todos — BE14-DASH01-A01 → FE02-DASH02-A03

Pedido expresso de Lucas em 10/10/2026, após identificar os gráficos ausentes: reutilizar a implementação existente e mostrar informação também com Todos os clientes e Todos os armazéns. A composição passa a ser Acesso rápido → dez indicadores → gráficos de ocupação física, fila de saída e disponibilidade por produto → mapa. `WarehouseOverview` recebe o painel `OperationDashboard` entre os indicadores e o mapa, tanto no aplicativo real quanto no exercício. Os quatro cartões antigos não são repetidos; nomes autorizados identificam a seleção.

`GET /api/v1/dashboard` passa a aceitar cliente/armazém opcionais, independentemente. O serviço resolve Todos como cadastros do Gestor ou listas permitidas da identidade dos demais perfis. Os agregados usam os mesmos conjuntos autorizados, sem carregar todas as unidades, consultar cada par ou somar páginas no navegador. Sem alcance, o resultado é vazio; pedido explícito fora do alcance continua recusado. Capacidade e posições livres nos gráficos mantêm a permissão de Gestor do contrato DASH01.

Produtos continuam agrupados pelo ID do produto e paginados no servidor, com `clienteId` em cada item para identificar o proprietário. O mesmo SKU de dois clientes permanece em linhas separadas, assim como KG e UN; somente o saldo do mesmo produto é agregado entre os armazéns selecionados. Posições e fila representam o contexto completo, independentemente da página de produtos. Na visão agregada, o contrato antigo de avisos de validade permanece sem valor: não extrapola uma antecedência de um cliente/armazém para os demais. Esse cartão não é exibido no Início atual.

Atualizar gráficos é uma leitura explícita, sem polling. Trocar contexto ou alcance descarta respostas antigas e reinicia a paginação na composição do Início. Falha/403 conserva o aviso separado de zero e remove os gráficos da consulta recusada. A gravação operacional, a reserva, o cálculo financeiro e o mapa mantêm seus serviços e regras existentes.

A conferência e a versão preparada estão no [recibo A03](../../orchestracao/.runtime/graficos-inicio/resultado.json). Testes HTTP/JPA usam somente H2 isolado; Chrome usa respostas interceptadas no modo real e transporte fictício no exercício, sem API ou SQL Server real. Pacote preparado não significa backend em execução atualizado: o operador deve encerrar o console DEV anterior e executar `iniciar-dev.bat` para carregar a nova consulta. Nenhuma migration ou intervenção no SQL Server é necessária.

## Ícones do mapa — FE02-DASH02-A07

Pedido expresso de Lucas em 10/10/2026. Bloqueada usa cadeado SVG e Reservada usa marcador SVG, substituindo os caracteres !/R de A06. Cada estado forma um item próprio da legenda, mantendo ícone/rótulo juntos na quebra de linha. Ícones da legenda têm 18 px e alinhamento central; os marcadores nas células mantêm 12 px para não encobrir o código. São decorativos para leitores de tela: nomes dos estados continuam nos rótulos visíveis e nos nomes acessíveis das posições. Cores de ocupação, consultas e regras não mudam.

Tipagem, lint focal, formatação/build real, oito testes de contexto/mapa e um caso Chrome aprovados, com 12 combinações de seis larguras (320/390/768/1024/1500/1920) e dois temas. Capturas da legenda e das posições inspecionadas; alinhamento, dimensões, ausência de transbordamento, filtros, detalhes e teclado conferidos com API interceptada. [Recibo](../../orchestracao/.runtime/map-icons/resultado.json). Graphify recusou a atualização por extração menor; sem substituição forçada.

## Encaixe das ruas, divisões e prévia — FE02-DASH02-A08

### Complementos A09/A10 — largura e continuidade, 10/10/2026

Pedidos expressos de Lucas: células passam de 64 para 144 px, com 8 px de preenchimento lateral para tornar códigos comuns legíveis. Códigos excepcionalmente longos conservam reticências e a prévia com o conteúdo completo. O dimensionamento das ruas e a quantidade de colunas visíveis usam a largura nova.

Quando a rua tem colunas fora da janela, cada nível mostra um recorte da posição vizinha à direita e, após avançar, à esquerda, com desfoque leve e degradê. O recorte usa código e cor do dado já consultado; não inventa posições em lacunas nem aparece além do início/fim. Seu espaço diminui em telas estreitas para priorizar uma célula completa. É decorativo, sem foco/clique ou leitura duplicada; setas, contador e detalhes continuam como controles. Não há requisição adicional.

Validação local conjunta: nove testes de contexto/mapa, dois casos Chrome/HTTP interceptado, seis larguras de 320 a 1920 px nos dois temas; códigos de prova completos, recorte/desfoque, navegação até a última coluna, ausência de sobreposição/transbordamento, popup/detalhes e teclado. Build, lint focal, formatação e tipagem da aplicação aprovados. A tipagem integral esbarrou em opções `exact` inválidas no teste paralelo `qual-conf01-init01.test.tsx`, preservado. [Recibo e limites](../../orchestracao/.runtime/map-width/resultado.json).

### Complementos A11–A13 — expansão limitada e hierarquia, 10/10/2026

Os pedidos seguintes refinam A09/A10: medir os códigos na fonte efetivamente usada, priorizar células cortadas na distribuição de sobra e permitir que cartões/células preencham a faixa. **Correção expressa após o exemplo de barras muito largas:** a expansão tem limite útil. A largura-base continua 144 px; o teto por rua é o maior código mais 32 px de respiro, limitado ao intervalo 192–240 px. Ruas com uma/duas posições permanecem compactas; espaço livre não é eliminado à custa de células gigantes. Reticências continuam possíveis quando não há espaço ou o código excede o teto; a prévia conserva o conteúdo completo.

A distribuição mantém o encaixe denso e a ordem de teclado. O espaço é compartilhado por coluna entre ruas da mesma faixa; faixas que cruzam cartões de alturas diferentes não expandem sobre lacunas já ocupadas. Ruas paginadas usam a sobra interna sem reduzir arbitrariamente o número de colunas completas, preservando recortes e setas. Mudanças de fonte, dados e tamanho recalculam as medidas.

A hierarquia de superfícies é uniforme: armazéns usam uma base neutra suave; ruas têm destaque discreto em relação a essa base; títulos e contornos separam os níveis. Remove-se a alternância de armazéns preto/cinza de A08. Livre/ocupada conservam verde/vermelho; indisponíveis têm texto e contorno legíveis. Hover e foco de teclado misturam 22% da cor de texto ao fundo sem perder a família de cor, com contorno e transição de 140 ms. Movimento reduzido desativa a transição; dispositivos sem hover não recebem efeito persistente de mouse.

Tipagem integral, lint focal, formatação, build, nove testes de contexto/mapa e dois casos Chrome com respostas interceptadas aprovados. Conferidos os exemplos com três/cinco ruas e uma/duas posições, seis larguras de 320 a 1920 px nos dois temas, retorno ao desktop, filtros, todas as colunas, limites, popup/teclado e hover nos três estados. Capturas inspecionadas. [Recibo](../../orchestracao/.runtime/map-adaptive/resultado.json); sem SQL/backend/reinício real ou publicação.

### Registro original A08

Pedidos expressos de Lucas em 10/10/2026: ocupar a largura lateral com ruas de tamanhos próprios, distinguir os grupos, mostrar informações ao passar o mouse e preencher as lacunas deixadas por ruas grandes. Dependência BE14-DASH02 → FE02-DASH02-A06/A07.

Cada rua recebe largura conforme a quantidade de colunas, limitada à tela, sem distribuir todas em colunas de tamanho igual. O encaixe denso usa a largura e a altura reais dos blocos: ruas posteriores menores podem subir para um espaço livre. Assim, no exemplo QUARENTENA → R01 extensa → SEPARAÇÃO → TRIAGEM, as ruas pequenas aproveitam a faixa superior quando cabem. Armazéns têm contorno e fundos alternados; ruas têm contorno discreto, preenchimento de 8 px e separador abaixo do título. Nomes extensos quebram sem alargar o mapa.

O tamanho é recalculado ao redimensionar, filtrar ou alterar os dados. A ordem das posições/níveis e as lacunas dentro de cada rua permanecem; a ordem do DOM/teclado permanece estável, enquanto o encaixe visual pode antecipar ruas pequenas. Ruas maiores conservam as setas e todas as colunas alcançáveis. Não se altera a paginação de 100 endereços do servidor. Sem ResizeObserver, a apresentação conserva quebra simples lado a lado.

Cada célula mostra uma prévia ao receber mouse ou foco: código completo, armazém, rua, nível, posição, área, situação e capacidade de peso, além de bloqueio/reserva/quarentena quando informados. Usa somente os dados já autorizados da consulta; não faz uma requisição por hover nem antecipa conteúdo de estoque. Popup é limitado à janela, pode ser percorrido com o ponteiro, fecha com Escape/saída/rolagem e não toma o foco. Clique/Enter continuam abrindo os detalhes completos, com toque direto no celular. O tooltip nativo anterior foi substituído para evitar duas prévias simultâneas.

Conferência local: tipagem, lint focal, formatação/build real, nove testes de contexto/mapa e dois casos Chrome com API interceptada. Dezesseis passagens em 320/390/600/768/1024/1500/1920 px, nos dois temas, incluindo retorno ao desktop. Conferidos encaixe da lacuna do exemplo, contornos, alturas variáveis, alinhamento/lacunas entre níveis, ausência de sobreposição/transbordamento, todas as colunas, filtros, Enter/Escape/Tab e retorno de foco. Popup conferido com mouse, foco, permanência sobre o conteúdo e limites de 320×560 px; capturas inspecionadas. [Recibo](../../orchestracao/.runtime/map-wrap/resultado.json), [medidas claro](../../frontend/evidencias/map-wrap-light-medidas.json), [medidas escuro](../../frontend/evidencias/map-wrap-dark-medidas.json). Sem backend/SQL real, reinício ou publicação. Resultado de graphify AST registrado no recibo, sem `--force`.

## Alinhamento da busca — FE02-DASH02-A01

Lucas apontou o botão Buscar deslocado em 09/10/2026. A margem inferior global de 8px dos rótulos fazia o botão ficar abaixo do campo e do seletor. A correção inicial em `overview.css` zerou essa margem somente em `.map-tools label`, preservando o alinhamento ao final da linha e a quebra no mobile. A mesma regra passou para `forms.css` no [ajuste compartilhado FE02-REG01-A02](etapa-06.md#alinhamento-compartilhado-dos-filtros--fe02-reg01-a02). Não altera busca, filtros, contratos ou regras do mapa.

Formatação, tipagem/build real próprio e [seis casos Chrome](../../frontend/evidencias/map-search-a01-browser.json) passaram: claro/escuro em 1440, 768 e 360px; alinhamento com tolerância de 1px em linhas compartilhadas, controles de toque, página sem vazamento horizontal, busca por Enter e Limpar. Capturas inspecionadas. HTTP interceptado e dados fictícios; sem backend/SQL/reinício de processos existentes. [Recibo e Graphify](../../orchestracao/.runtime/map-search-a01/resultado.json). Próximo: carregar o CSS atual na página.
