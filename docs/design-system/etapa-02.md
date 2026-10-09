# FE02-DS02 — Login, topo e rodapé

Pedido de Lucas em 09/10/2026: melhorar a apresentação e as informações do login, alinhar e completar o cabeçalho e incluir suas informações no rodapé do projeto. Dependências: [FE02-DS01-A02](etapa-01.md), FE03-AUTH01 e BE04-AUTH01 → FE03-AUTH01. Esta etapa usa a fundação existente e altera a apresentação e os controles de senha; o backend continua responsável pela autenticação e pelas permissões.

## Login e telas de acesso

No desktop, a entrada apresenta duas colunas: propósito do WMS e etapas de recebimento, armazenagem e expedição à esquerda; formulário à direita. No celular, o formulário tem prioridade e a introdução é omitida para caber na altura disponível. Espaçamentos e apresentação desktop também se ajustam à altura. A descrição não contém métricas, disponibilidade do servidor ou dados operacionais fictícios apresentados como reais. Essa compactação foi solicitada por Lucas durante a conferência da etapa.

O card mantém rótulos permanentes, autocomplete e validação nativa de e-mail. O botão nomeado Mostrar/Ocultar senha funciona com teclado, mantém o valor e fica indisponível durante o envio. A recusa preserva o e-mail e limpa/oculta a senha, seguindo o comportamento anterior de limpeza. O envio continua protegido contra repetição pelo handler existente.

As ações “Esqueceu sua senha?” e “É seu primeiro acesso?” abrem um diálogo nativo sobre a página, explicando o fluxo existente: administrador fornece senha temporária e o titular cria sua nova senha antes das operações. Substituem as sanfonas iniciais, a pedido de Lucas, para não deslocar o card. Escape ou Entendi fecham o diálogo e devolvem o foco ao acionador. Não há nova recuperação automática nem cadastro público. Ajuda no cabeçalho abre o contato de suporte confirmado abaixo, com ícone de headset, também ajustado por pedido expresso.

A descrição e os avisos usam a mesma área reservada de 64 px. Erro substitui o texto de apoio e tem prioridade sobre mensagens anteriores. Falhas de conexão, indisponibilidade e resposta incompatível recebem orientação própria de login; recusa de credenciais mantém a mensagem recebida. Não há repetição automática de envio. Mensagens excepcionais muito longas podem ampliar a área para manter a leitura; alturas extremas, teclado virtual e zoom preservam rolagem acessível em vez de esconder conteúdo.

`AuthLayout` compartilha marca, suporte, botão de tema e rodapé entre login, verificação de sessão e troca obrigatória de senha. A troca continua com as mesmas regras e contratos de D32. O tema segue o [comportamento vigente A03](etapa-01.md#temas), que substitui a ausência de persistência de A02.

**FE02-DS02-A01, ajuste expresso de Lucas:** as duas ações de ajuda mantêm fundo transparente no mouse/foco. O destaque usa apenas o sublinhado do texto, com traço mais forte no teclado, substituindo o fundo e o contorno retangular herdados dos botões. A área continua com altura mínima de 44 px e os diálogos conservam abertura/fechamento e foco.

**Conferência A01:** sete casos Chrome com API interceptada aprovaram fundo transparente, sublinhado, foco por teclado e diálogos em claro/escuro e cinco dimensões. Capturas de foco mobile/desktop inspecionadas; `frontend/evidencias/design-system-a03-login-browser-resultados.json`. Os resultados de A03 e de navegação estão no registro da etapa 01.

## Rodapé e origem das informações

`AppFooter` aparece no login, nas demais telas de acesso, no painel autenticado e no exercício fictício. Exibe identificação do WMS, ano corrente e direitos da Rodogarcia, crédito e contato. Os valores estão centralizados em `frontend/src/components/shell/AppFooter.tsx`.

| Informação | Valor usado | Fontes consultadas somente em leitura |
| --- | --- | --- |
| Desenvolvedor | Lucas Andrade | `avaliacao-desempenho-competencias/frontend/src/App.tsx`, `dashboards-etl/frontend/src/components/layout/LayoutPainel.tsx`, `site-cms-landingbuilder/cms/frontend/src/app/auth/entrar/page.tsx` |
| LinkedIn | `https://www.linkedin.com/in/dev-lucasandrade/` | Rodapés dos três projetos e `dashboards-etl/frontend/src/pages/LoginPage.tsx` |
| Suporte | `lucasmac.dev@gmail.com` | Rodapés de login/painel do Dashboards e login do CMS |

O LinkedIn abre em nova aba, indicada no nome acessível, com `noopener noreferrer`. O e-mail usa `mailto:`; nenhum envio de mensagem foi executado. O login do Dashboards também usa o identificador `@valentelucass`; esta composição usa o nome Lucas Andrade, confirmado nos rodapés dos três projetos.

Os controles do exercício fictício passam a ser uma seção nomeada, conservando seus handlers e evitando dois rodapés globais. O rodapé acompanha o fim do conteúdo e permanece no fundo em páginas curtas; não cobre formulários ou foco.

## Cabeçalho e organização

**Incremento vigente FE02-DS03:** por pedido posterior de Lucas, contexto operacional ocupa o centro do topo com altura desktop de 64 px; nome/perfil ficam somente na sidebar. Placeholders e preferência de expansão estão descritos na [etapa 03](etapa-03.md#header-e-preferências). A composição abaixo registra o fecho histórico DS02.

O topo do painel alinha o módulo atual à esquerda e identidade, suporte, tema e ações da conta à direita. No desktop, mostra nome, perfil e iniciais; nomes longos têm limite visual no topo e continuam completos na lateral e no título do controle. No celular, as ações Minha senha/Sair usam ícones com nomes acessíveis; a identidade continua na lateral. Menu, ações e tema permanecem em uma linha, com quebra de título quando necessária.

| Arquivo | Responsabilidade |
| --- | --- |
| `frontend/src/auth/AuthLayout.tsx` | Estrutura comum das telas de acesso |
| `frontend/src/auth/LoginPage.tsx` e `auth.css` | Composição, formulário e orientações |
| `frontend/src/components/shell/Brand.tsx` | Identificação visual usada no topo e na lateral |
| `frontend/src/components/shell/AppFooter.tsx` | Rodapé e informações confirmadas do desenvolvedor |
| `frontend/src/components/shell/AppShell.tsx` e `src/styles/shell.css` | Topo, rodapé e disposição do painel |
| `frontend/src/design-system/Icon.tsx` | Ampliação da mesma família de ícones de traço |

As superfícies, bordas, espaçamentos, cores e estados usam os tokens dos dois temas. A largura de leitura de 1120 px e o título introdutório de 44 px, reduzido em tablet/mobile, são adaptações **P** para a entrada WMS; não são medidas confirmadas da referência. Não há dependência, imagem externa, script inline ou alteração de CSP.

## Conferência e limites

- Tipagem, lint completo, formatação dos arquivos alterados e builds real/fictício aprovados. O build mantém o aviso de tamanho do bundle JavaScript acima de 500 kB; divisão de código não integra este pedido.
- 66 testes focais passaram em cinco arquivos: autenticação, correções de sessão/interface, fluxos e apresentação.
- Sete casos de Chrome passaram com API interceptada: cinco jornadas completas em 320×640, 360×740, 768×720, 1024×720 e 1440×720, mais verificação de sessão/troca obrigatória em 360/1440 px. O login ficou sem rolagem horizontal/vertical nessas dimensões. Ajuda, recusa 401, serviço 503, conexão interrompida e resposta inválida mantiveram a posição do formulário e a página no topo. Mostrar/ocultar senha, estados de envio, preservação de e-mail, limpeza de senha, foco, Escape, saída e links do rodapé foram conferidos.
- Doze casos de regressão da navegação passaram no exercício fictício: tema automático, botão sol/lua, lateral reduzida, drawer, ciclo de foco, atalho de conteúdo e edição preservada. Claro/escuro e fonte local em sete larguras, 320–1440 px.
- Capturas finais de login, painel e senha temporária foram inspecionadas. Evidências: `frontend/evidencias/design-system-etapa02-ajustes-browser-resultados.json`, `frontend/evidencias/design-system-etapa02-navegacao-browser-resultados.json` e respectivas pastas `frontend/test-results-design-system-etapa02-*`. [Recibo da entrega](../../orchestracao/.runtime/design-system-etapa02-resultado.json).

A tentativa inicial com o Chromium padrão foi impedida pela ausência do executável. As conferências aprovadas usam o Chrome já instalado, via configuração local `.tools/design-system-etapa02.config.ts`; nenhum navegador foi instalado. Os previews próprios, na porta 5198, encerraram ao fim dos testes. Artefatos das tentativas anteriores foram preservados, sem apresentar seus resultados como prova da fonte final.

`graphify update .` foi executado. A manutenção do mapa continua com a recusa de redução e avisos de schema/filtro já observados no projeto; não foi usado `--force` nem alterada a política de exclusão para fabricar um mapa atualizado. Essa pendência não é apresentada como aceite do mapa e permanece separada da entrega visual local.

Esta entrega visual não comprova login com conta real, disponibilidade SQL, impressão ou homologação operacional. A investigação BE02-SQL-DIAG01 e a validação real VALID-LOGIN-CAD01 conservam seus próprios estados. Backend, banco, processos existentes e alterações preexistentes no launcher/documentação foram preservados.

**Próximo passo visual:** atualizar a página em execução para carregar o login/topo/rodapé; continuar a composição de outras páginas somente por demanda. Não há correção visual pendente identificada nesta etapa. Integração real e manutenção do mapa mantêm os limites acima.
