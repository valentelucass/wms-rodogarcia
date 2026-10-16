# Entrada e conferência no pedido — FE05-PED01

Pedido expresso de Lucas em 09/10/2026, complementar à [refatoração por registro](etapa-06.md). Entrada e conferência abre **Pedidos de entrada**, consulta o cliente/armazém aplicados no topo e apresenta **Novo pedido** no cabeçalho, com escolhas manual/XML no incremento de 10/10. As quatro etapas globais foram substituídas por seções internas do pedido: Dados gerais, Notas e itens, Conferência e Histórico. A composição usa os componentes, tokens e temas de [design.md](design.md) e [etapa-03.md](etapa-03.md).

## Notas e documento original — FE05-NOTAS-DET01

Pedido de Lucas em 16/10/2026 a partir da imagem de Notas e itens com o XML original aberto. Número da nota e série ficam separados; origem, emissão e primeira chegada têm seus próprios campos. A tabela operacional vem antes do documento, com Selecionar item em verde e identificadores técnicos acessíveis sob demanda. O callback continua selecionando os objetos originais da nota e do item; nome acessível conserva item/nota completos. Tabelas têm rolagem própria por teclado e celular.

Documento original usa um bloco separado com ações roxa de consulta/download e vermelha de ocultação. `aria-expanded`/painel associado identificam abertura; identificação fiscal, participantes e itens originais são apresentados por assunto. Chave completa ocupa largura inteira; avisos recebidos ficam em âmbar, preservando os limites da conferência local. Transporte, protocolo e informações adicionais iniciam recolhidos na consulta salva; permanecem abertos na prévia de importação. Valores, unidades, série zero, fuso e textos originais são conservados, sem agrupar itens ou presumir ambiente/operação fiscal ausentes. Ajuda em `information.receivingNotes`/`receivingOriginalDocument`; estilos limitados a `receivingNotes.css`. Cabeçalho e fechamento do pedido permanecem acessíveis na rolagem.

Corrigido o carregamento que podia continuar ativo após receber o documento, impedindo download: sucesso atual encerra `pending` antes de guardar os dados, sem depender do `finally` após o cleanup do efeito. Consulta continua somente ao abrir, com IDs originais, sem leitura duplicada ao reabrir; erro exige nova ação. Download recebe o XML exato, bloqueado junto às ações durante atualização.

39 testes em seis arquivos, três novos, tipagem, lint, formatação e build próprio aprovados. Seis layouts Chrome fictícios claro/escuro em 390/768/1440 px, expansão por Enter, seleção com nota/item originais e IDs Long, XML baixado idêntico, cache e bloqueios conferidos. [Galeria](../../orchestracao/.runtime/receiving-note-detail-20261016/galeria.html), [recibo](../../orchestracao/.runtime/receiving-note-detail-20261016/validacao.json). Preview próprio encerrado; sem API real/SQL/backend, reinício ou publicação; versão ativa não aferida.

## Ação de conferência na tabela — FE05-CONFERENCIA-BOTAO01

Pedido de Lucas em 16/10/2026 pela imagem da lista. Iniciar/Continuar conferência usam o mesmo fundo azul suave, borda e cor de texto das ações da tabela, com ícone de conferência. Altura de 36 px no desktop e área de toque de 44 px no celular; rótulo sem quebra e largura mínima pelo conteúdo, dentro da coluna de ações. O azul sólido deixa de ser aplicado a este botão de linha. Hover/foco e temas reutilizam os tokens existentes.

Rótulos, ação escolhida, callbacks com ID original, disponibilidade e bloqueio durante atualização preservados. A conferência não é enviada automaticamente. 13 testes existentes em dois arquivos, tipagem, lint do componente, formatação e build próprio aprovados. Seis layouts Chrome claro/escuro em 390/768/1440 px, texto inteiro, coluna sem sobreposição, callbacks por teclado/clique e bloqueio conferidos na [prévia local](../../orchestracao/.runtime/receiving-conference-button-20261016/galeria.html). Dados fictícios e componentes reais; preview próprio encerrado, sem integração SQL/backend, reinício ou publicação. A fonte entregue não comprova atualização da versão em execução.

## Criação por XML — FE05-XML01, 10/10/2026

**Novo pedido** abre uma escolha curta: **Criar manualmente** continua o formulário existente; **Importar XML da NF-e** abre o mesmo diálogo ampliado com arquivo, prévia documental e escolhas de cliente proprietário/armazém/produtos. Campos originais e associações no WMS têm rótulos distintos. Não agrupar linhas da nota. Alterar associações exige atualizar a prévia; confirmação fica desabilitada enquanto houver pendências, carregamento ou resultado desconhecido. Avisos, erros e pendências usam status/alerta legíveis e permanecem no diálogo, sem criar acordeão ou cartão isolado para a escolha.

Quantidades e valores conservam representação decimal sem passar por `Number`; volumes de transporte têm seção própria. A composição usa duas colunas no desktop, uma no mobile e rolagem interna nas tabelas, com os mesmos tokens claro/escuro. Diálogo nativo conserva teclado, foco e bloqueio durante confirmação. O registro confirmado fica acessível fora dos filtros correntes. Lista e dados gerais exibem a origem da criação; nota com XML permite consultar/baixar o documento original dentro do detalhe.

Regras, APIs e limites no [contrato do incremento](../45-pedido-entrada-manual-xml.md). Este incremento acrescenta APIs de prévia/confirmar/recuperar/documento, conservando o fluxo anterior de conferência física e a possibilidade de anexar XML depois da criação manual.

## Botões da consulta — FE05-PED01-A01

Pedido expresso de Lucas em 10/10/2026. Aplicar filtros e Restaurar filtros ficam juntos à esquerda; Atualizar lista passa para a extremidade direita da mesma faixa, dentro da área de filtros. Novo pedido continua no cabeçalho. Até 600 px, Aplicar/Restaurar dividem a primeira linha e Atualizar ocupa a seguinte. A ordem do teclado acompanha a apresentação. Atualizar mantém `type="button"` e a consulta já aplicada; não submete os filtros em edição. Handlers, contratos e bloqueio durante carregamento preservados. Conferência e evidências em `STATES.md` e `orchestracao/.runtime/buttons-a01/resultado.json`.

## Tabela de entrada com rolagem lateral — FE05-PED01-A02

Pedido de Lucas em 12/10/2026, com complementos durante a correção: manter conteúdo em uma linha, reduzir a altura dos registros, usar reticências para textos longos e mostrar o valor completo no mouse. A tabela especializada passa a ter larguras próprias e rolagem horizontal somente dentro da região da tabela, também acessível pelo teclado.

Referência e número do pedido ficam na mesma linha, alinhados na altura. Cliente, armazém, notas, datas e etiquetas longas usam reticências; o texto integral permanece no DOM e aparece por mouse, foco ou toque. Referência continua sendo botão de detalhes com nome acessível completo. Os valores vêm das consultas existentes, sem alterar IDs ou regras.

Ações ficam lado a lado e alinhadas à direita: comando principal com texto, olho para Ver pedido e três pontos verticais para Mais ações. Os três pontos têm fundo e borda transparentes; o foco pelo teclado continua visível. Os ícones revelam seus nomes no mouse/foco, conservam nome acessível e área de toque de 44 px. Conferência e Situação ficam mais próximas. O menu usa popover nativo, fora do recorte da rolagem, sem aumentar a altura do registro; Escape, clique/toque fora, rolagem e redimensionamento o fecham. A disponibilidade dos comandos continua filtrada por situação e permissão.

Tipagem, lint focal, formatação, build e 20 testes pertinentes aprovados; 17 casos Chrome aprovados e sete rechecados após o último alinhamento à direita. Provas em 1440/1152/390 px, dois temas, textos extensos, teclado e toque em contexto com `hasTouch`. As capturas foram inspecionadas. HTTP totalmente interceptado, sem validação de backend/SQL ou dispositivos físicos. [Recibo e capturas](../../orchestracao/.runtime/receiving-table/resultado.md).

## Fontes e alcance

O [inventário anterior](inventario-paginas.md#entrada-e-conferência) conserva os IDs `entrada-1` a `entrada-4` e seus comandos. Esses IDs agora identificam grupos de capacidades da mesma visão, sem quatro páginas globais. O [contrato de recebimento](../18-recebimento-e-conferencia.md), controllers, DTOs e serviços existentes determinam regras, permissões, revisões e efeitos de estoque. Nenhuma tabela, migration ou API foi acrescentada nesta entrega.

O modelo consultado expõe uma coleção de notas, cada uma com seus itens. A interface percorre toda a coleção e não presume nota única. Selecionar um item conserva seu ID e a nota de origem; o produto não substitui o ID do item. IDs Long e decimais mantêm a representação sem perda usada pelo transporte.

## Destino dos comandos

| Capacidade anterior | Destino atual | Comando preservado |
| --- | --- | --- |
| Lista e consulta | Pedidos de entrada e abertura pela linha | `PedidoEntradaController.listar` / `consultar` |
| Criação | Cabeçalho da lista | `PedidoEntradaController.criar` |
| Nota manual / XML | Notas e itens do pedido | `PedidoEntradaController.nota` / `xml` |
| Produtos para a nota | Referências dentro do formulário de nota | `ProdutoController.listar` |
| Iniciar conferência | Ação principal da linha e seção Conferência | `PedidoEntradaController.iniciar` |
| Continuar conferência / registrar progresso físico | Ação principal da linha e seção Conferência | `PedidoEntradaController.chegada` |
| Comparar previsto / físico / avaria / diferença | Tabela por nota e item, consulta atual do detalhe | `PedidoEntradaController.consultar` |
| Tratar divergência e efetivar | Conferência, com aceite e motivo no comando existente | `PedidoEntradaController.efetivar` |
| Chegadas, responsáveis, instantes e estornos | Histórico | `PedidoEntradaController.chegadas` |
| Corrigir chegada | Histórico, estorno identificado por Supervisor/Gestor | `PedidoEntradaController.estornar` |
| Cancelamento permitido | Dados gerais, por Supervisor/Gestor | `PedidoEntradaController.cancelar` |
| Entradas efetivadas / continuação para unitização | Histórico e encaminhamento preservado para Unidades e etiquetas | `PedidoEntradaController.entradas` |
| Auditoria de alterações do pedido | Histórico, somente Gestor; tipo e ID protegidos | `AuditoriaController.listar` existente |

Ações visíveis respeitam perfil e situação conhecidos; o serviço continua revalidando todas as condições ao confirmar. Não há endpoint de edição dos dados gerais do pedido. A administração permitida usa os comandos de nota, XML, chegada, estorno, cancelamento e efetivação, sem criar uma edição genérica da situação.

## Lista, filtros e datas

A lista mostra referência e ID, cliente/armazém por nome quando constam no catálogo autorizado, notas, primeira chegada física, efetivação, resultado da conferência e situação operacional. Ver pedido, Iniciar/Continuar conferência e Mais ações abrem o registro pelo ID persistente, com consulta atual antes de operar.

Cliente e armazém são filtros remotos existentes. Sem contexto obrigatório, os seletores por nome no topo e a orientação da consulta permitem escolhê-lo; não há requisição com alvo presumido. Anterior/Próxima e Página 1, 2… apresentam a paginação, mantendo o índice zero apenas no contrato interno.

Busca por pedido/nota, situação, conferência e período de **criação** estão identificados como **nesta página**. Filtram somente a resposta atual: a API de lista não oferece esses filtros globais. Para notas, chegada e conferência, a interface consulta os detalhes dos pedidos da página recebida, com até quatro GETs concorrentes. Não percorre todas as páginas nem altera a seleção da operação durante essas leituras. Respostas antigas são descartadas ao trocar de página/contexto; erro de detalhe fica visível e não equivale a pedido conferido. Esse enriquecimento acrescenta leituras e seu desempenho real permanece uma validação de ambiente.

Emissão da nota, primeira chegada e efetivação permanecem distintas. A primeira chegada é obtida das datas retornadas por nota, conservadas pelo backend; não é a data do XML nem um horário criado pelo navegador. Datas com instante são apresentadas em America/Sao_Paulo. Dados ausentes não são preenchidos com datas inventadas.

## Leitura da conferência

Os rótulos abaixo são uma **interpretação de apresentação** do detalhe existente, não novas situações de domínio nem um comando de conclusão da conferência. A situação operacional recebida aparece em coluna separada. A diferença e o booleano `divergente` vêm do backend; o frontend não recalcula disponibilidade ou libera estoque.

A diferença retornada é **bom + avariado − previsto**: uma falta de duas unidades aparece como −2, sobra de duas como +2. Avaria também é divergência mesmo com diferença zero. Quantidades não mudam de significado conforme o rótulo apresentado.

| Dados retornados | Resultado apresentado |
| --- | --- |
| Rascunho, sem chegada | Pendente |
| Conferência aberta, sem primeira chegada | Em conferência |
| Chegada registrada e `divergente=true`, antes de efetivar | Divergente |
| Chegada registrada e `divergente=false`, antes de efetivar | Conferido |
| Efetivado, `divergente=false` | Conferido |
| Efetivado, `divergente=true` | Conferido com divergência |
| Cancelado | Cancelado |
| Detalhe ausente ou incompatível | Consultando… ou Não consultado, com aviso pertinente |

**Conferido não efetiva a entrada.** Quarentena pode permanecer mesmo após os totais voltarem a coincidir; a efetivação continua própria e supervisionada. A seção Conferência mostra previsto, físico bom, avariado e diferença por item. O registro de chegada mantém instante real, observação, UUID da operação e revisão. Registrar outra chegada gera outro UUID com a revisão atual; repetir uma operação incerta conserva o payload e UUID originais.

A tratativa usa `aceitarDivergencias` e motivo no comando de efetivação existente, com confirmação explícita. O backend exige quantidade física e autoriza ou recusa o resultado. Correção de lançamento usa estorno e novo registro, conservando autor, data, motivo e evidência original. Não existe um campo manual para marcar Conferido, nem exclusão visual do histórico para esconder uma divergência. XML continua documental; após congelar a previsão, o vínculo só é aceito pelo serviço para nota existente compatível.

## Criação, atualização e navegação

Após criação confirmada, o ID retornado é vinculado ao detalhe e a lista é atualizada. Continuar com a nota deste pedido permanece no mesmo diálogo, com ID, cliente/armazém e revisão protegidos. Notas, XML e chegadas confirmados atualizam o detalhe e a lista. O pedido confirmado pode ser aberto mesmo quando os filtros ativos o ocultam.

Durante a atualização do detalhe, novos comandos ficam indisponíveis. Falha nessa leitura mostra tentativa explícita e mantém a confirmação já recebida separada dos dados que ainda precisam ser consultados. Falha na lista mantém busca e filtros locais durante o erro e após Tentar novamente, sem apresentar uma resposta vazia como sucesso. Conflito preserva campos e contexto. Edição pendente exige descarte explícito ao mudar seção, fechar, navegar ou trocar contexto. Resultado desconhecido bloqueia outro payload; a consulta de estado e o encerramento do acompanhamento preservam as guardas de FE02-REG01.

`#entrada` continua válido. Encaminhamentos internos antigos com ação usam o pedido selecionado e sua seção local; a continuação para unitização mantém a seleção explícita da entrada. Não havia URL pública distinta por etapa ou detalhe. Atualizar o navegador volta à lista conforme a navegação existente.

## Conferência local e limites

Tipagem, lint, formatação dos arquivos alterados e builds real/fictício em diretórios próprios aprovados. A [regressão completa](../../frontend/evidencias/receiving-ped01-regressao-final.json) passou com **478 testes**; os [15 testes focais](../../frontend/evidencias/receiving-ped01-receiving-final.json) incluem notas múltiplas, item da segunda nota, criação seguida de nota, revisão/UUID entre chegadas, ausência de contexto, conflito, permissões, auditoria vinculada, filtros conservados após erro e descarte de resposta atrasada.

Uma rodada intermediária teve timeout de cinco segundos no cenário fiscal de duas notas. A [rechecagem isolada](../../frontend/evidencias/receiving-ped01-fiscal-rechecagem.json) passou nos cinco casos, e a rodada completa final passou sem falhas, sem aumentar o timeout ou alterar esse teste fiscal.

Os [seis casos Chrome](../../frontend/evidencias/receiving-ped01-browser.json) conferem desktop 1440px e mobile 360px nos dois temas, notas e detalhe, Enter/Escape e retorno de foco, criação seguida de XML no novo pedido, filtros, paginação, erro e ausência de resultados. Capturas da lista e dos diálogos são inspecionadas; tabelas mantêm rolagem interna no mobile. O navegador usa o build real com HTTP totalmente interceptado e dados fictícios, sem executar serviços Java ou SQL Server.

O [recibo local](../../orchestracao/.runtime/receiving-ped01/resultado.json) registra contagens, hashes, arquivos, capturas e o resultado de Graphify. O aviso de bundle acima de 500 kB permanece. Integração WMS_DEV, desempenho das leituras, dispositivos/fiscal e fonte carregada na aplicação em execução continuam verificações separadas pelo fluxo autorizado e guarda vigente. Não houve startup backend, banco, migration, reinício de processo existente ou publicação nesta entrega.

`graphify update .` foi executado após as alterações da implementação. A ferramenta recusou substituir o mapa de 15.220 nós pela extração de 13.119, com avisos sobre o schema das fontes históricas. O grafo existente foi preservado sem `--force`; a atualização do mapa permanece impedida, sem ser apresentada como verificação funcional da aplicação.
