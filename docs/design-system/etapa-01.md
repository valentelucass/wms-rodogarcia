# FE02-DS01 — Fundação visual e navegação WMS

Primeira etapa solicitada por Lucas em 09/10/2026: organização, tipografia, cores, temas claro/escuro, topo e lateral. Especificação: [design.md](design.md), especialmente seções 5–10, 18, 36–39 e apêndice A. Reconstrução independente da referência OnlyGenius creditada à ZeeFrames, adaptada ao nome e aos fluxos do WMS Rodogarcia.

## Fundação e organização

| Arquivo | Responsabilidade |
| --- | --- |
| `frontend/src/design-system/tokens.css` | Fonte, escala, geometria e cores semânticas dos dois temas |
| `frontend/src/design-system/ThemeSelector.tsx` e `theme.ts` | Seletor compartilhado e preferência de apresentação |
| `frontend/public/theme-init.js` | Detecção do tema do dispositivo antes do CSS e acompanhamento do sistema até a troca manual |
| `frontend/src/design-system/Icon.tsx` | Família pequena de ícones SVG de traço, sem dependência adicional |
| `frontend/src/components/shell/AppShell.tsx` | Estrutura compartilhada de topo, lateral, contexto e conteúdo |
| `frontend/src/components/shell/Navigation.tsx` | Destinos existentes, nomes acessíveis e página atual |
| `frontend/src/styles/{base,shell,forms,results,collector}.css` e `src/auth/auth.css` | Consumidores dos tokens, preservando a divisão existente |

O aplicativo real e o exercício fictício usam a mesma estrutura visual. Login e troca obrigatória de senha usam o mesmo seletor e os mesmos tokens. Destinos, handlers e contratos operacionais foram preservados. Administração continua condicionada à permissão existente; a lateral não concede autorização.

## Tipografia, cores e origem

| Decisão | Antes | Aplicação nesta etapa | Origem |
| --- | --- | --- | --- |
| Fonte | Arial/Helvetica | Inter local, com fallback Arial/sans-serif; pesos 400/500/600/700 | C na referência; hospedagem local P |
| Corpo e navegação | 16 px na raiz | 14/20 px; apoio 12/16 px; campos mobile 16/24 px | E/P do guia |
| Títulos | Escala local em rem | Página 28/36 px; seção 18/24 px; subtítulo 16/24 px | E/P do guia |
| Ação principal | `#124CAB` | `#2563EB`, com hover/active e texto branco | C para marca; estados P |
| Superfícies | Fundos claros e cores literais | Fundo, lateral, cards, campos, tabelas, foco, seleção e mensagens por tokens semânticos | A/P e P do guia |
| Lateral | 220 px | 256 px expandida; 64 px reduzida no desktop | E/P do guia |
| Geometria | Botões 3 px; campos 2 px | Controles 6 px; cards 12 px; espaço em múltiplos de 4 px | E/P do guia |

A paleta clara/escura segue o apêndice A sem mudança de marca. Sucesso, erro e atenção usam tokens próprios e texto explícito. Não foram criados indicadores ou dados de negócio. A prévia da etiqueta mantém papel branco e texto escuro nos dois temas, por tokens próprios de impressão; impressão física permanece pendente.

Inter é distribuída pelo [projeto oficial rsms/inter](https://github.com/rsms/inter). Arquivo original `docs/font-files/InterVariable.woff2`, servido em `frontend/public/fonts/InterVariable.woff2`; [licença OFL](../../frontend/public/fonts/Inter-LICENSE.txt) preservada. SHA-256 do WOFF2: `693B77D4F32EE9B8BFC995589B5FAD5E99ADF2832738661F5402F9978429A8E3`. O navegador carrega a fonte do próprio WMS, sem CDN. Não há ampliação da CSP nem scripts inline.

## Temas

**Comportamento vigente FE02-DS01-A03, correção expressa de Lucas:** o tema do dispositivo é o ponto de partida somente quando não existe uma escolha válida salva. Ao usar o botão sol/lua, Light ou Dark é guardado em `localStorage["wms.theme"]` e passa a prevalecer sobre o sistema operacional na recarga e nos próximos acessos ao mesmo navegador/origem. A leitura acontece antes do CSS, preservando a aplicação inicial do tema.

Alterações do sistema acompanham a página somente enquanto a preferência for automática. A escolha manual é compartilhada entre abas da mesma origem. Valores diferentes de `light`/`dark` são ignorados e outros dados são preservados. Se o navegador bloquear armazenamento, a tela continua funcionando e mantém a escolha durante aquela página. Permanece somente um botão circular sol/lua; o ícone de Sistema não retorna.

**Conferência A03:** builds real/fictício, tipagem, lint focal e formatação aprovados; 15 casos de navegador fictício e 7 de login/API interceptada passaram. Light salvo com sistema Dark e Dark salvo com sistema Light, recarga, abas, preferência inválida, armazenamento indisponível e edição preservada conferidos. Provas `frontend/evidencias/design-system-a03-{preferencia,login}-browser-resultados.json`. Não houve conexão SQL ou autenticação real.

**Histórico FE02-DS01-A02, substituído por A03 na persistência:** Lucas esclareceu que o ícone de Sistema confunde e deve ser removido. Cada abertura ou recarga detectava automaticamente `prefers-color-scheme` e iniciava com o tema do dispositivo, antes do CSS. Existe somente um botão circular de 44 px: lua ativa escuro; sol ativa claro. O botão tem nome acessível, foco e operação por teclado. Na versão A02, a troca preservava o conteúdo e valia durante o uso da página; na próxima abertura, o tema voltava a seguir o dispositivo.

Na versão A02, mudanças do sistema acompanhavam a tela até o usuário alternar manualmente. A escolha manual não era salva nem sincronizada entre abas; valores antigos de `localStorage["wms.theme"]` eram ignorados, sem limpar outros dados. Superfícies, controles nativos e drawer recebem o mesmo tema. Movimento reduzido desativa transições.

**Histórico FE02-DS01-A01:** substituiu o seletor textual pelos botões circulares sol/lua e monitor, ainda com preferência salva. O monitor e a persistência foram substituídos pelo comportamento automático de A02; o recolhimento da lateral permanece.

## Topo e lateral

Anatomia: identificação WMS → destinos → identidade/perfil. O topo mostra o módulo atual, tema e, na sessão autenticada, Minha senha/Sair. No mobile, também abre a lateral. Nome longo quebra linha na identidade. O ícone de armazém é uma identificação visual de interface, sem alegar um novo logotipo oficial.

Desktop a partir de 1024 px: lateral fixa com navegação própria por setas quando faltar altura, conforme A04 abaixo, expandida por padrão e redução opcional. Botões reduzidos mantêm nome acessível e título. Os ícones são decorativos; o destino atual tem `aria-current="page"`, fundo de seleção e texto. A ordem dos módulos existentes é preservada.

**Incremento FE02-DS03:** a escolha expandida/minimizada passa a persistir entre recargas; ausência de preferência inicia expandida. O header recebe o contexto operacional compacto e a identidade fica somente na lateral, conforme [composição atual](etapa-03.md#header-e-preferências).

**FE02-DS01-A01:** “Minimizar menu” fica na própria lateral, acima da identidade. A seta aponta à esquerda; ao clicar, a largura passa de 256 para 64 px, ancorada à esquerda, e os textos desaparecem visualmente. O conteúdo ocupa o espaço liberado. A seta passa a apontar à direita para expandir. Redução/expansão preservam o formulário e a seleção; a transição respeita movimento reduzido. No mobile permanece o drawer com destinos nomeados.

Abaixo de 1024 px: drawer lateral com texto e alvos de pelo menos 44 px. Esta é uma adaptação P ao menu operacional extenso do WMS; a barra inferior de cinco destinos do produto de referência não foi introduzida. O painel fecha pelo botão, pelo fundo, por Escape ou pela navegação. O diálogo nativo torna o restante da página inerte, e o ciclo de Tab/Shift+Tab permanece nos controles do painel. Escape devolve foco ao acionador; a navegação leva foco ao conteúdo. O atalho de conteúdo evita alterar o hash e preserva a edição.

## Validação local

### Setas e desfoque na lateral — FE02-DS01-A04

Pedido expresso de Lucas em 10/10/2026. A lista de módulos ocupa o espaço entre cabeçalho e rodapé. Quando seu conteúdo excede essa altura, apresenta Mostrar módulos anteriores/próximos, acima e abaixo da lista; cada passo avança quase uma área visível, com sobreposição de um item. No início/fim, a seta correspondente fica desabilitada. Quando tudo cabe, ambas desaparecem. A medição observa a lista e o espaço disponível, inclusive ao redimensionar, minimizar/expandir ou abrir o drawer.

A barra nativa não aparece; roda e toque permanecem funcionais. Desfoque de 3 px com máscara gradual nas bordas indica somente a direção com conteúdo oculto, sem interceptar cliques. Foco por teclado e módulo selecionado são trazidos à área visível. Setas têm nomes acessíveis e vínculo com a navegação; no celular recebem área de toque de 44 px. Movimento reduzido elimina a animação dos passos. Destinos, permissões, seleção, Escape/retorno do foco e identidade do rodapé permanecem.

Tipagem/lint/formatação/builds real e fictício, 25 testes de páginas e cinco casos Chrome aprovados. Conferidos todos os destinos por setas, limites, desfoque, ausência de barra, foco, seleção, redimensionamento, menu de 64/256 px, claro/escuro e drawer de 390×560 px. Capturas inspecionadas. API interceptada, sem backend/SQL ou reinício/publicação. [Recibo](../../orchestracao/.runtime/sidebar-arrows/resultado.json).

### Validações anteriores

- Tipagem, lint, formatação e builds real/fictício aprovados. A primeira regressão frontend passou com 374 testes em 37 arquivos. Uma execução posterior, durante alterações simultâneas de CORR-LOGIN-CAD01, teve 401 aprovados e 1 falha em `auth.test.tsx`: a expectativa de `create(body)` ainda não aceita o novo argumento `AbortSignal` do painel. Essa frente e seu teste foram preservados; não se declara `npm run check` integral aprovado na fonte mais recente.
- Navegador fictício: 12 testes aprovados (11 desta etapa e regressão do atalho). Temas, recarga, sistema, abas, armazenamento indisponível, seleção/redução, edição preservada, drawer, foco e Escape.
- Larguras 320, 360, 390, 768, 1024, 1280 e 1440 px: claro/escuro, fonte local carregada, nenhuma rolagem horizontal da página e nenhum erro de console nos cenários visuais.
- Aplicação real com API interceptada: 2 testes aprovados, em 360/1440 px, cobrindo login, tema, nome longo, atalho sem perda de contexto, Minha senha, Usuários e Sair. Todas as respostas de autenticação foram fictícias; outras requisições API foram bloqueadas.
- Capturas de desktop, login e menu móvel foram inspecionadas. Relatórios e imagens ficam em `frontend/evidencias/design-system-etapa01*-browser-resultados.json` e `frontend/test-results-design-system-etapa01*/`.

Na validação, a volta do Tab para fora dos controles do drawer foi reproduzida e corrigida com ciclo explícito de foco. Não há declaração de conformidade WCAG completa. A integração com SQL Server, dispositivos, contas reais e impressão não foi executada neste recorte. Processos existentes foram preservados; previews de teste usam portas próprias 5196/5197 e terminam com os testes.

`graphify update .` foi executado na raiz e recusou reduzir o mapa de 60.677 nós (última tentativa: 12.579 nós). Mapa oficial preservado, sem `--force`; sua manutenção continua pendente. Uma execução anterior no diretório frontend gerou um mapa auxiliar em `frontend/graphify-out`, fora do Git e sem uso na aplicação. A limpeza foi recusada pela política automática e a movimentação reversível recebeu acesso negado pelo filesystem; esse artefato foi preservado, sem alterar ACL ou elevar privilégios.

[Recibo local com hashes e limites](../../orchestracao/.runtime/design-system-etapa01/resultado.json).

## Continuidade

**Manutenção do mapa em A02:** Graphify tentou atualizar na raiz e recusou a redução 15.220→12.617 nós, sem `--force`. [Recibo A02](../../orchestracao/.runtime/design-system-etapa01/resultado-a02.json).

**Validação da correção FE02-DS01-A02:** tipagem, lint, formatação e builds real/fictício aprovados. Doze testes no modo fictício e dois da aplicação nativa com API interceptada passaram. Conferidos: somente um botão circular, detecção inicial/recarga pelo sistema mesmo com preferência antiga, mudanças do dispositivo antes da troca manual, Enter, isolamento entre abas, armazenamento indisponível, formulário preservado e menus em sete larguras. Capturas claro/escuro do painel e login inspecionadas. Relatórios em `frontend/evidencias/design-system-etapa01-a02*-browser-resultados.json`, imagens em `frontend/test-results-design-system-etapa01-a02*/` e lateral em `frontend/evidencias/design-system-etapa01-a02-sidebar-minimizado.png`. Previews próprios nas portas 5198/5199 encerraram com os testes. Não foram executados SQL, autenticação real ou suíte integral de backend; a regressão frontend integral anterior conserva seus limites históricos.

**Validação do ajuste FE02-DS01-A01:** tipagem, lint, formatação e builds real/fictício aprovados. Doze casos de navegador no modo fictício passaram, cobrindo Sistema inicial, botão circular e Enter, retorno ao sistema, persistência/abas, redução256→64px com posição esquerda0, textos ocultos, edição preservada e sete larguras. Dois casos da aplicação nativa com API interceptada passaram em360/1440px. Capturas de login, painel e lateral minimizada inspecionadas. Relatórios em `frontend/evidencias/design-system-etapa01-a01*-browser-resultados.json`; captura da lateral em `frontend/evidencias/design-system-etapa01-a01-sidebar-minimizado.png`. Testes e processos locais isolados; não executados banco, autenticação ou alterações de negócio reais. A atualização AST do graphify foi tentada na raiz e recusou reduzir o mapa atual (15.220→12.616 nós); mapa preservado, sem forçar.

FE02-DS01 entrega a fundação e a navegação; não conclui todo o design system. Próxima etapa sugerida: componentes compartilhados de botão, campo, abas, mensagens e tabela, seguida da composição visual de páginas. Dashboard, gráficos, novas regras, contratos e mudanças de autenticação não pertencem a esta etapa. As correções paralelas CORR-LOGIN-CAD01 e a validação operacional mantêm seus próprios registros e critérios.
