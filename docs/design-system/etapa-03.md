# FE02-DS03 — Organização das páginas internas

Pedido expresso de Lucas em 09/10/2026: organizar ações, etapas, referências, formulários, resultados, paginação e diálogos em todas as páginas internas. Durante a entrega, pediu também contexto no header compacto, placeholders que desaparecem ao focar, outra cor para Aplicar contexto, identidade somente na lateral e persistência da expansão/minimização da sidebar. Dependências: FE02-DS01-A03 e FE02-DS02-A01; os vínculos backend/frontend das jornadas e BE04-AUTH01 → FE03-AUTH01 são preservados.

## Composição e cobertura

| Páginas | Organização aplicada |
| --- | --- |
| Início | Cabeçalho, quatro acessos rápidos e painel de consulta dos indicadores do contexto, sem inventar métricas |
| Cadastros; Entrada e conferência; Unidades e etiquetas; Estoque e rastreabilidade | Etapas na lateral interna desktop, área de tarefa, ações secundárias, formulário, resultado e referências selecionadas |
| Saída, FIFO e reserva; Fiscal e retirada | Mesmo padrão; reservas e resumo fiscal/físico conservam seleção e informações operacionais |
| Serviços e tabelas; Serviços e cálculo; Fechamentos e ESL | Mesmo padrão; origem do ajuste, auditoria, memória e continuação existentes preservadas |
| Contagem e carga inicial; Contingência; Consultas e relatórios | Mesmo padrão; permissões, prova manual, pendências e efeitos continuam explícitos |
| Coletor | Blocos de tarefa/leitura e destino, sequência existente, foco do scanner e controles de pelo menos 52 px |
| Usuários e acessos | Ação principal no cabeçalho, tabela e paginação; cadastro, edição e redefinição em diálogo central |

`PageHeader`, `HomeOverview`, `Pagination` e `Dialog` são componentes compartilhados usados concretamente nessas telas. `JourneyPage` aplica a composição às 12 jornadas existentes. No desktop há duas colunas: navegação das etapas e tarefa. Em telas menores as etapas passam acima da tarefa. Botões das etapas conservam seus nomes, ordem, permissões e `aria-current`; ações conservam `aria-pressed`. O preenchimento azul permanece nas ações principais de envio; seleção de etapa/ação usa superfície discreta de seleção.

Campos de página/tamanho têm agrupamento próprio ao final da consulta. Fieldsets vazios deixam de ocupar espaço. Tabelas mantêm semântica HTML, separadores horizontais e rolagem local quando suas colunas excedem a largura. Referências selecionadas ficam em painel próprio; listas continuam exigindo seleção explícita e revisões/IDs permanecem distintos.

## Diálogos de conta — FE02-DS03-A06

Pedido expresso de Lucas: Minha senha em pop-up, com a página atual atrás, e melhor organização e destaque do cadastro de usuários. No refinamento, rejeitou a faixa azul decorativa do topo. A implementação final remove a faixa e segue a anatomia de `design.md`, seções 9 e 30: título, conteúdo e ações ao final; raio de 16 px; padding de 24 px no desktop e 16 px no mobile; superfícies adequadas ao tema, borda e sombra de sobreposição. Superfície de conta, overlay e sombra são adaptações P por tokens WMS para distinguir a janela da tela escura; não são cores atribuídas à referência. O fundo recebe escurecimento e desfoque discreto.

Identificação, acesso operacional e segurança da conta têm grupos próprios. Campos relacionados ficam lado a lado no desktop e empilhados até 600 px. A janela de usuários usa até 840 px; troca de senha até 620 px, para acomodar nova senha e confirmação na mesma linha. Ações ficam juntas ao final, com envio azul; no mobile ocupam a largura disponível. Conteúdo alto tem rolagem interna, sem esconder o fechamento. Fundo do formulário é transparente para evitar faixas pretas entre os grupos.

Minha senha abre por estado local, sem navegar ou desmontar a página corrente. Ao fechar, URL e edição da operação são preservadas. O endereço legado `#senha` continua abrindo o diálogo com Início ao fundo. A troca obrigatória da senha temporária continua antes da área operacional. Os handlers de troca/criação/edição/redefinição, limites de senha, vínculos, permissões, mensagens e bloqueio por resultado incerto são preservados. Foco inicial no primeiro campo, Tab dentro da janela, Escape bloqueado durante envio e retorno ao acionador sem deslocar a página usam o `Dialog` nativo compartilhado. Não há modais empilhados nem alteração da regra de encerramento das sessões.

**Conferência local:** tipagem final, lint focal, formatação e builds real/fictício isolados aprovados; 14 testes unitários existentes de autenticação/correções e 16 casos Chrome com API interceptada. Os seis casos novos cobrem 320/390/768/1440/1920 px, claro/escuro, ausência da faixa, foco inicial, Tab, Escape, retorno ao acionador, edição/URL preservadas, validação, espera, erro e conclusão da troca de senha. Dez casos existentes cobrem login, senha obrigatória, edição de usuário e bloqueio por resultado incerto. Capturas desktop e mobile inspecionadas. [Prova final](../../frontend/evidencias/account-a06-corrigido-browser-resultados.json). A rodada inicial detectou foco no fechamento em vez do primeiro campo; corrigido antes da rodada final. A tipagem teve erro transitório na frente DASH01 concorrente, ausente na conferência final. Prova de interface, sem autenticação/SQL reais, equipamento ou homologação operacional. Preview próprio 5213 encerrado; processos existentes preservados.

**Mapa:** atualização AST executada após o código; a tentativa final recusou reduzir 15.220 → 12.825 nós e apontou problemas de esquema preexistentes. Mapa preservado, sem forçar; manutenção mantém sua frente própria. [Log final](../../orchestracao/.runtime/account-a06-final-graphify.txt), [recibo local](../../orchestracao/.runtime/account-a06-resultado.json).

**Compatibilidade com a frente concorrente DASH01:** após sua inclusão no build, a rodada atual teve 13 aprovações e três recusas no inventário de requisições do teste de usuários, pela nova consulta `/api/v1/dashboard`. A prova de usuários passou a interceptar essa consulta de leitura com falha fictícia 503 e foi reexecutada: três aprovações. São 16 casos únicos aprovados por composição, sem tratar a rodada de 13/16 como integralmente verde. [Rodada atual](../../frontend/evidencias/account-a06-entrega-browser-resultados.json), [três reexecuções](../../frontend/evidencias/account-a06-users-final-browser-resultados.json). A resposta fictícia do dashboard não comprova seus gráficos ou integração.

## Composição do Coletor — FE02-DS03-A02

Lucas pediu integrar o Coletor à linguagem visual das páginas internas. Durante a conferência, rejeitou a centralização do painel: a composição atual alinha com o título e ocupa a largura útil da página. No desktop, um painel divide a escolha da operação e a identificação da unidade. Superfície de apoio, ícone, títulos, bordas, espaçamento e cores usam os tokens existentes. O botão de consulta tem largura natural à direita; em telas até 800 px, tarefa e leitura ficam empilhadas e o botão ocupa a largura disponível, mantendo controles de pelo menos 52 px.

Identidade consultada, destino e comando usam o mesmo eixo e painéis. A tarefa continua no seletor existente, a leitura conserva os rótulos e o envio por Enter, e a resposta mantém foco no campo de posição. Troca de tarefa/contexto, validações, confirmação e tratamento de espera continuam nos handlers existentes. Não há mudança de regra, contrato ou permissão. Dependência FE02-DS03; vínculos backend/frontend do Coletor preservados.

**Conferência local:** tipagem, lint focal, formatação e builds real/fictício aprovados; seis testes existentes de Coletor/workspace e sete casos Chrome no exercício fictício. Conferidos alinhamento e largura, quatro tarefas, edição preservada ao alternar tema, ausência de rolagem horizontal em 320/390/1024/1440/1920 px e leitura/destino/confirmação em 390/1440 px. Capturas desktop claro e mobile escuro inspecionadas. [Prova do navegador](../../frontend/evidencias/collector-a02-resultados.json). O preview próprio da porta 5210 encerrou com os testes; processos existentes preservados. Equipamento e integração real permanecem fora desta prova visual. O refinamento da textura das jornadas (A01) foi interrompido por Lucas e não recebeu aceite visual.

**Mapa:** a atualização AST após o ajuste do Coletor recusou reduzir 15.220 → 12.661 nós e conservou 804 avisos de esquema preexistentes. Mapa preservado, sem forçar; manutenção continua pendente. [Log](../../orchestracao/.runtime/collector-a02-graphify.txt), [recibo local com hashes](../../orchestracao/.runtime/collector-a02-resultado.json).

## Referências da jornada — FE02-DS03-A04

Pedido de Lucas: melhorar o frame inteiro de Referências da jornada no contexto atual em todas as páginas que o utilizam, incluindo a cor do botão. Composição adotada no `JourneyPage` compartilhado: fundo com tonalidade azul suave e superfície de apoio, borda discreta, ícone da mesma família visual e título; referências selecionadas em cards internos; faixa inferior com orientação e botão azul. A lista vazia não reserva espaço. Os temas usam tokens existentes e o botão ocupa a largura disponível no mobile. O aviso de descarte da edição permanece no próprio botão.

O conteúdo das referências, IDs, revisões e seleção explícita continuam iguais. O botão mantém o handler e o bloqueio durante confirmação/resultado incerto; apenas apresentação e wrappers foram alterados. Dependência FE02-DS03; vínculos BE04–BE14 → FE03–FE13 preservados. Esta entrega não retoma a textura A01 interrompida.

**Conferência:** tipagem, lint focal, formatação e builds real/fictício isolados aprovados; cinco testes existentes de seleção/recebimento e seis casos Chrome finais. Os cinco casos de layout cobrem as 12 jornadas em 320/390/1024/1440/1920 px e nos dois temas; o sexto cobre referências preenchidas e aplicação por Enter, descartando a edição e recuperando o ID selecionado. Fundo, botão, foco, limites do painel e ausência de rolagem horizontal conferidos. Capturas desktop claro e mobile escuro, vazio e preenchido, inspecionadas. [Prova final](../../frontend/evidencias/references-a04-final-resultados.json). A tentativa inicial de prova preenchida usava Clientes, referência que não pertence à lista dessa jornada; o cenário foi corrigido para Pedido de saída, sem alterar a regra do produto. Preview próprio 5211 encerrado; sem backend/banco ou processos existentes reiniciados. Esta prova visual não comprova integração real ou dispositivos.

Graphify tentou atualizar o AST e recusou reduzir 15.220 → 12.663 nós, mantendo 804 avisos de esquema preexistentes. Mapa preservado, sem forçar; manutenção permanece pendente. [Log](../../orchestracao/.runtime/references-a04-graphify.txt), [recibo local](../../orchestracao/.runtime/references-a04-resultado.json).

## Fundos dos acessos rápidos — FE02-DS03-A05

Lucas pediu fundos marcantes e diferentes para cada card de Acesso rápido, mantendo o conteúdo interno. A composição preserva anatomia, fonte Inter, pesos, raios, espaçamento, ícones e grade existentes, conforme os princípios e tokens de `design.md` (seções 4–9, 22 e 27). Os novos fundos são uma adaptação **P** ao WMS, autorizada pelo pedido visual; não são cores atribuídas à referência nem representam status operacional.

| Card | Fundo adotado | Detalhe visual |
| --- | --- | --- |
| Receber e conferir | Azul cobalto, `#123B8E` → `#1E40AF` | Contornos angulares |
| Ler e endereçar | Verde petróleo, `#083E46` → `#115E59` | Arcos suaves |
| Reservar e separar | Índigo, `#2E1065` → `#5B21B6` | Faixas inclinadas |
| Conferir fechamento | Grafite azulado, `#111827` → `#334155` | Moldura deslocada |

As variantes usam tokens `--wms-shortcut-*`, degradês e geometria CSS, sem imagens externas ou animação. O texto fica branco para leitura sobre os fundos intensos; nomes, descrições, ícones, ordem e destinos permanecem iguais. Os fundos funcionam nos dois temas e permanecem no hover; o foco por teclado conserva contorno visível. A grade existente continua com quatro, duas ou uma coluna conforme a largura.

**Conferência local:** tipagem, lint focal, formatação e builds real/fictício isolados aprovados; seis casos Chrome finais em 320/390/768/1024/1440/1920 px, nos dois temas. Verificados quatro fundos diferentes, contraste conservador de pelo menos 4,5:1 para texto branco nos extremos dos degradês com sobreposição clara de 18%, descrições preservadas, hover, Tab/Shift+Tab/Enter, foco no conteúdo ao navegar e os quatro destinos. Sem rolagem horizontal ou erro de página. Capturas desktop claro e mobile escuro inspecionadas. A primeira prova confundia foco programático após mouse com foco visível de teclado; o cenário foi ajustado para Tab/Shift+Tab, sem mudança operacional. [Prova final](../../frontend/evidencias/shortcuts-a05-final-resultados.json). Preview próprio 5212 encerrado; sem backend/banco, equipamento ou reinício de processos existentes. Esta prova de apresentação não comprova integração real.

**Mapa:** a atualização AST recusou reduzir 15.220 → 12.708 nós; o mapa principal foi preservado, sem forçar. Manutenção permanece pendente. [Log](../../orchestracao/.runtime/shortcuts-a05-graphify.txt), [recibo local com hashes](../../orchestracao/.runtime/shortcuts-a05-resultado.json).

## Header e preferências

Na sessão real, Cliente (ID), Armazém (ID) e Aplicar contexto passam ao centro do header, entre módulo e ações da conta, separados por divisórias. O header desktop mantém 64 px de altura. Nome e perfil são mostrados somente na sidebar. Em largura reduzida, o contexto ocupa uma linha própria dentro do topo, com alvos de toque de 44 px; em celulares estreitos, o botão fica abaixo dos dois campos.

Os campos usam placeholders, sem prefixos permanentes ou espaço reservado para o texto. O placeholder fica transparente ao focar; quando vazio e sem foco reaparece. Nomes acessíveis permanentes preservam a identificação mesmo preenchidos. Aplicar contexto usa tokens próprios em verde petróleo, diferentes do azul da marca, com variantes claro/escuro e foco visível. Validação de IDs e alcance continua a mesma. Sucesso é anunciado; erro aparece junto ao controle, sem ampliar o header desktop. O modo fictício conserva seu seletor de perfil e controles próprios no bloco identificado de exercício.

A sidebar persiste a escolha em `localStorage["wms.sidebar.collapsed"]`: `true` minimizada, `false` expandida. Ausência/valor inválido inicia expandida. Refresh, troca de módulo e passagem temporária pelo layout móvel preservam a escolha desktop. Falta de armazenamento não interrompe o uso, mas limita a preferência à visita atual. A chave de tema e outros dados não são alterados por esse controle.

## Diálogos e paginação

Confirmações de operações aparecem no centro, com resumo dos valores preenchidos, decisão explícita e retorno à conferência. O formulário e seus valores continuam no contexto. A consulta auxiliar abre referências em diálogo largo; selecionar fecha o diálogo e mantém o mecanismo de seleção existente. Fechar/Escape sem selecionar conserva a edição. O botão de rever referências ocupa seu espaço desde o início para evitar deslocar a tarefa após uma consulta.

Diálogos usam `dialog.showModal`, fundo inerte, ciclo explícito de Tab/Shift+Tab e retorno ao acionador após fechamento. Não fecham por clique acidental no fundo. Conteúdo longo rola dentro do diálogo. Usuários não permitem fechar/editar durante envio ou resultado incerto; a consulta explícita continua sendo a recuperação, sem repetir a escrita. O acionador de cadastro permanece no DOM para receber foco ao fechar. O fallback de `open` serve aos testes DOM sem suporte modal; as propriedades modais são conferidas no Chrome.

Resultados paginados apresentam total conhecido, quantidade recebida, tamanho e página ao pé da lista. Anterior/Próxima usam GET remoto quando o contrato aceita `pagina`; preservam filtros e parâmetros da consulta recebida, sem misturar edições ainda não enviadas. Totais Long permanecem texto decimal exato. Controles ficam indisponíveis nos limites e durante espera. Resultados anteriores são identificados enquanto há atualização, sem anunciar uma confirmação antiga como nova. Respostas somente consultivas sem controlador de página mantêm os destinos indisponíveis, sem inventar navegação local. Usuários usa o mesmo componente, mantendo sua API de paginação existente.

## Limite de altura dos campos — FE02-DS03-A03

Pedido expresso de Lucas em 09/10/2026: caixas de entrada que podem aumentar de tamanho precisam de limite de altura em todo o projeto, incluindo Motivo / justificativa de Cadastros → Clientes. A regra global de `textarea` em `styles/base.css` mantém redimensionamento vertical e limita a altura pelo token `--wms-textarea-max-height`, definido em 240 px. Conteúdo excedente usa rolagem interna automática. Altura inicial, largura, valores e limites de caracteres definidos pelos contratos permanecem os mesmos.

A regra atende tanto os campos multilinha de `ScalarField` (motivo, observação, descrição, fonte e critério) quanto `XmlField`, os dois pontos de criação de textarea existentes. Novos campos que usem o elemento nativo recebem o mesmo limite. Desktop e mobile compartilham o teto; tema não altera a geometria.

Conferência A03: tipagem e formatação dos dois arquivos CSS aprovadas; build fictício em `.tools/textarea-height-a03-dist`, sem substituir o `dist` de outra frente. Oito verificações no Chrome instalado aprovaram justificativa de Clientes e XML de Entrada em 390/1440 px e claro/escuro. Arraste real e tentativa de altura de 1.200 px respeitaram o teto; texto longo permaneceu acessível pela rolagem interna, sem rolagem horizontal da página. Captura móvel inspecionada. [Prova e medidas](../../frontend/evidencias/textarea-height-final.json); modo fictício isolado, nenhum backend/API real/SQL chamado e preview próprio encerrado. Sem nova suíte unitária para este ajuste CSS; as provas gerais abaixo pertencem ao fecho anterior DS03.

Atualização AST A03 executada sem `--force`: recusou a redução de 15.220 para 12.663 nós e conservou o limite de manutenção já registrado, sem descartar fontes por força. [Log próprio](../../orchestracao/.runtime/textarea-height-a03-graphify.txt). Resultado da manutenção do mapa é separado da conferência visual. [Recibo A03 e hashes dos dois CSS](../../orchestracao/.runtime/textarea-height-a03-resultado.json).

## Espaço dos seletores — FE02-CTX01-A01

Lucas pediu em 09/10/2026 somente aliviar o aperto dos seletores no topo de Cadastros, conservando todos os clientes/armazéns e Aplicar contexto. `context.css` remove a largura fixa de 170 px, dá largura fluida às colunas e reserva espaço às ações do topo. Em 1024–1399 px o contexto ocupa a linha seguinte; até 600 px os seletores empilham. O exercício também distribui os seletores com largura flexível. São ajustes de apresentação, sem alteração do catálogo, seleção, validação ou backend.

Formatação e build real próprios aprovados; Chrome com API interceptada em 320/390/600/768/1024/1280/1440 px confirmou textos completos, listas dentro da tela e ausência de rolagem horizontal após a adaptação do layout. Capturas claro/escuro inspecionadas, inclusive as ações do topo. [Medidas e prova](../../frontend/evidencias/context-spacing-resultados.json). Preview próprio encerrado; sem nova suíte unitária para este recorte CSS. `graphify update .` recusou substituir o mapa por extração menor, sem `--force`; [log](../../orchestracao/.runtime/context-spacing-graphify.log). A entrega funcional CTX01/DASH02 mantém validação e estado próprios.

## Conferência e limites

Tipagem, lint integral, formatação dos arquivos desta entrega e builds real/fictício aprovados. A verificação ampla de formatação encontrou sete arquivos de outras frentes; foram preservados, sem reformatação alheia ao pedido. Os builds conservam o aviso de chunk acima de 500 kB.

- 424 testes unitários únicos aprovados por composição: 422 no relatório amplo e os dois casos que expiraram em 5 s aprovados na rodada focal, com um worker. A rodada focal aprovou 18 casos; não somar casos repetidos como cobertura adicional nem apresentar o relatório amplo como uma execução integral verde.
- 23 casos Chrome no exercício fictício: composição das 14 páginas operacionais em claro/escuro e 320, 390, 768, 1024 e 1440 px; ausência de rolagem horizontal; sidebar; confirmação e referências por teclado; jornadas de recebimento, saída, financeiro, contagem, carga inicial e coletor. Prova `frontend/evidencias/design-system-ds03-workspace-final-browser-resultados.json`.
- 10 casos Chrome na aplicação nativa com API interceptada: sete de login/sessão/senha temporária e três do header/usuários em 390, 1024 e 1440 px. Conferidos header desktop de 64 px, placeholder transparente ao focar, paginação, retorno de foco, envio incerto sem repetição e diálogos em ambos os temas. Prova `frontend/evidencias/design-system-ds03-auth-users-final-browser-resultados.json`.

Capturas de Cadastros desktop, Coletor móvel, header e usuários foram inspecionadas. As rodadas anteriores e a chamada interrompida que selecionou testes de exercício sobre build nativo permanecem como histórico, sem compor os resultados aprovados. Exercício fictício e API interceptada são provas de interface, não integração com SQL Server, homologação operacional ou dispositivos. Nenhum backend, SQL ou processo existente foi reiniciado nesta entrega. Os previews próprios encerraram com os testes; o último build é o real.

O delta intermediário paralelo da coluna de etapas em `JourneyPage.tsx`/`workspace.css` foi preservado e recebeu conferência visual adicional de oito casos, registrada em `frontend/evidencias/design-system-ds03-layout-delta-browser-resultados.json`; são casos repetidos, sem ampliar a contagem de 33 casos únicos. Incrementos posteriores de FE02-DS03-A01 mantêm sua própria validação; este fecho não os converte em aceite.

`graphify update .` foi executado após as alterações e recusou substituir 15.220 nós por 12.661, apontando também 804 avisos de esquema preexistentes. O mapa principal foi preservado, sem `--force`; [log da tentativa](../../orchestracao/.runtime/design-system-etapa03-graphify.txt). Manutenção do mapa permanece um limite próprio, sem bloquear a composição visual local. [Recibo com composição das provas, fontes e limites](../../orchestracao/.runtime/design-system-etapa03-resultado.json).
