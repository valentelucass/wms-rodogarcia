# FE02-CTX01 / BE14-DASH02 → FE02-DASH02 — seleção por nome e visão geral

Pedido expresso de Lucas em 09/10/2026: implementar os seletores de cliente e armazém e ajustar o Início conforme o texto anexado. Esta entrega substitui a composição do Início de DASH01; seu contrato e componente de gráficos permanecem preservados. As regras de estoque e cobrança continuam nos serviços existentes.

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

Busca por código e filtros Todas/Disponíveis/Ocupadas afetam somente o mapa. Paginação de 100 endereços e rolagem dentro da rua mantêm todos os cadastros alcançáveis; os totais continuam do contexto inteiro. São identificados total de registros, quantidade nesta página e capacidade considerada. Vazio, busca sem resultado, carregamento e falha são explícitos.

Cada posição abre um diálogo com código completo, armazém, rua, nível, posição, estado, tipo, capacidade de peso/dimensões/empilhamento/unidade permitida e ocupação. Para conteúdo autorizado, mostra produto, SKU, lote, quantidade, etiqueta, pedido de origem, bloqueio, reserva, quarentena e último movimento de estoque registrado. Ausência de movimento não é substituída pela data de edição. Conteúdo de outro cliente fora do alcance fica oculto; somente sua ocupação física permanece visível.

Endereçar está disponível para endereço livre com cliente selecionado. Abre o Coletor existente com armazém e destino preenchidos, visíveis desde a leitura. Caso o endereço esteja fora da página de posições consultada pelo coletor, é buscado pelo ID e o armazém é conferido. Ler, escolher ou abrir detalhes não grava movimentação. O comando exige leitura da unidade, conferência do destino e confirmação existente; permissões, disponibilidade e capacidade são revalidadas pelos serviços.

## Contratos e atualização

`GET /api/v1/visao-operacao`: cliente/armazém opcionais, fuso obrigatório, código/estado/página/tamanho. `GET /api/v1/visao-operacao/posicoes/{id}`: detalhe autorizado do endereço. Controller → serviço → repository, leitura transacional no mesmo isolamento dos indicadores existentes, sem migrations. Contratos frontend regenerados da fonte Java.

A consulta atualiza ao entrar/voltar ao Início, aplicar contexto, mudar armazém, paginar, filtrar ou clicar Atualizar visão. Após confirmação de operação, a revisão do workspace/catálogo é invalidada. Leituras canceladas ou atrasadas de outro contexto não substituem a tela. O transporte real usa autenticação existente; o exercício fictício é separado e não serve de fallback.

## Conferência e limites

Dez testes locais backend (cinco de arquitetura, três de integração JPA/H2 isolada e dois de serviço), `verify` e artefato próprio. Cobertura de permissões, conteúdo restrito, agregação de armazéns, ordenação, filtro independente, capacidade zero, duas posições/uma unidade, 2/80 → 3%, valores incompletos/não zero e mês civil. A suíte frontend passou com 441 testes; o último incremento do coletor recebeu conferência focal adicional. Tipagem, lint e builds real/fictício separados. Dezoito cenários Chrome com HTTP interceptado e três do exercício fictício cobrem temas, telas pequenas, nomes, Long exato, Bearer, erros/zeros, paginação, detalhes/foco, encaminhamento sem escrita e regressão de login/contas.

Conferência focal final: 23 testes, incluindo destino fora da página, resposta atrasada e catálogo com mais de uma página. Dois casos nativos finais repetiram nomes/Long/valores restritos e o destino visível no coletor. Provas locais em `frontend/evidencias/visao-operacao*-browser-resultados.json`; [recibo da entrega](../../orchestracao/.runtime/visao-operacao-resultado.json). Capturas mantidas fora do Git. Não foram executados SQL Server, migrações, reinícios, publicação ou operação real. Dialeto/plano/desempenho em SQL Server, dados operacionais, NFS-e real, equipamento e versão já em execução permanecem verificações de ambiente, sem converter testes isolados em homologação.

`graphify update .` foi executado na raiz, somente AST, e recusou reduzir o mapa de 15.220 para 12.961 nós; o mapa principal foi preservado, sem `--force`. A manutenção do grafo permanece separada da entrega.
