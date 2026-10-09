# Listas, detalhes e ações por registro — FE02-REG01

Pedido expresso de Lucas em 09/10/2026. Esta entrega reorganiza as 12 áreas e suas 50 páginas existentes. Substitui as abas que separavam consulta, criação e comandos dentro de cada página. Mantém módulos e etapas reais da jornada, os contratos backend e a fundação visual de [design.md](design.md) e [etapa-03.md](etapa-03.md).

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

## Verificação e limites

Tipagem, lint e builds real/fictício aprovados. A [suíte completa](../../frontend/evidencias/record-pages-regressao-final.json) passou com **467 testes**; os [25 testes focais](../../frontend/evidencias/record-pages-focal-final.json) incluem todas as páginas, Clientes, resposta desconhecida, IDs de domínios distintos, 41 registros/paginação e recuperação de última página. As jornadas adaptadas também verificam entrada/divergência, unitização, divisão/reagrupamento, etiquetas, avaria, reserva/FIFO, fiscal/retirada, configuração/cálculo/fechamento, carga/contagem, contingência e auditoria.

Os [seis casos Chrome](../../frontend/evidencias/record-pages-browser.json) passaram: todas as 50 páginas em 1440px claro e 360px escuro; criação/edição/cancelamento por Escape, foco, solicitação de desativação, reativação e filtros; erro de carregamento, ausência de resultados, conflito 409 e Operação sem edição. Não houve erro de JavaScript ou requisição a endpoint não previsto. Capturas de desktop/mobile e diálogos foram inspecionadas. O navegador usa o build real com HTTP interceptado e dados fictícios; isso exercita autenticação e transporte do frontend, mas não comprova execução Java/SQL Server. O exercício fictício usa seu transporte separado.

Os builds preservam o aviso preexistente de bundle acima de 500 kB, sem falha. `graphify update .` foi executado; a ferramenta recusou substituir o grafo de 15.220 nós por uma extração menor. O mapa existente foi preservado, sem `--force`; esse impedimento não é apresentado como atualização bem-sucedida.

Nenhum backend, SQL Server, migration, emissão fiscal, impressão em dispositivo, publicação ou processo existente foi iniciado/alterado por este recorte. Integração real no WMS_DEV, equipamentos e versão carregada na aplicação em execução permanecem conferências de ambiente. Não representam páginas omitidas na implementação local.
