# ONLYGENIUS — Design system reconstruído e instruções para IA

**Versão:** 1.0  
**Data da análise:** 9 de outubro de 2026  
**Referência:** Algorithmic Trading Dashboard / OnlyGenius  
**Crédito do projeto na referência:** ZeeFrames UI UX Design Agency  
**Natureza deste documento:** reconstrução independente de interface, baseada em imagens públicas, com decisões complementares explicitamente identificadas.  
**Uso:** fornecer este arquivo inteiro a uma IA para documentar, implementar e revisar interfaces consistentes com a referência.

## Índice

- [0. Instrução principal para a IA que receber este arquivo](#0-instrução-principal-para-a-ia-que-receber-este-arquivo)
- [1. Legenda de evidência e força das regras](#1-legenda-de-evidência-e-força-das-regras)
- [2. Fontes e evidências consultadas](#2-fontes-e-evidências-consultadas)
- [3. Escopo do sistema reconstruído](#3-escopo-do-sistema-reconstruído)
- [4. Identidade visual e princípios](#4-identidade-visual-e-princípios)
- [5. Arquitetura de tokens](#5-arquitetura-de-tokens)
- [6. Cores](#6-cores)
- [7. Tipografia](#7-tipografia)
- [8. Espaçamento, dimensões e densidade](#8-espaçamento-dimensões-e-densidade)
- [9. Superfícies, elevação e ícones](#9-superfícies-elevação-e-ícones)
- [10. Layout da aplicação](#10-layout-da-aplicação)
- [11. Anatomia comum dos componentes](#11-anatomia-comum-dos-componentes)
- [12. Botões e ações](#12-botões-e-ações)
- [13. Botão de ícone e menu de ações](#13-botão-de-ícone-e-menu-de-ações)
- [14. Campos de formulário](#14-campos-de-formulário)
- [15. Seletores, pesquisa, data e filtros](#15-seletores-pesquisa-data-e-filtros)
- [16. Checkbox, radio e switch](#16-checkbox-radio-e-switch)
- [17. Abas e controles segmentados](#17-abas-e-controles-segmentados)
- [18. Navegação lateral](#18-navegação-lateral)
- [19. Navegação inferior mobile](#19-navegação-inferior-mobile)
- [20. Breadcrumb e cabeçalho de entidade](#20-breadcrumb-e-cabeçalho-de-entidade)
- [21. Card de métrica](#21-card-de-métrica)
- [22. Card de conteúdo e painel de gráfico](#22-card-de-conteúdo-e-painel-de-gráfico)
- [23. Badges de status e indicadores de variação](#23-badges-de-status-e-indicadores-de-variação)
- [24. Tabela de dados](#24-tabela-de-dados)
- [25. Paginação](#25-paginação)
- [26. Progresso e metas](#26-progresso-e-metas)
- [27. Cards de serviço, algoritmo e certificado](#27-cards-de-serviço-algoritmo-e-certificado)
- [28. Perfil, avatar e notificações](#28-perfil-avatar-e-notificações)
- [29. Tooltip, ajuda e mensagens](#29-tooltip-ajuda-e-mensagens)
- [30. Modal e drawer](#30-modal-e-drawer)
- [31. Carregamento, vazio, erro e atualização](#31-carregamento-vazio-erro-e-atualização)
- [32. Visualização de dados](#32-visualização-de-dados)
- [33. Templates de página](#33-templates-de-página)
- [34. Conteúdo, localização e formatação](#34-conteúdo-localização-e-formatação)
- [35. Contratos de dados e estados](#35-contratos-de-dados-e-estados)
- [36. Temas e preferências](#36-temas-e-preferências)
- [37. Movimento e microinterações](#37-movimento-e-microinterações)
- [38. Acessibilidade](#38-acessibilidade)
- [39. Arquitetura de implementação](#39-arquitetura-de-implementação)
- [40. Documentação que a IA deve produzir](#40-documentação-que-a-ia-deve-produzir)
- [41. Fluxo de trabalho recomendado para a IA](#41-fluxo-de-trabalho-recomendado-para-a-ia)
- [42. Matriz mínima de cobertura](#42-matriz-mínima-de-cobertura)
- [43. Validação visual](#43-validação-visual)
- [44. Critérios de aceitação](#44-critérios-de-aceitação)
- [45. Antipadrões que prejudicam a fidelidade](#45-antipadrões-que-prejudicam-a-fidelidade)
- [46. Apêndice A — Tokens CSS completos](#46-apêndice-a--tokens-css-completos)
- [47. Apêndice B — Exemplo de aplicação visual](#47-apêndice-b--exemplo-de-aplicação-visual)
- [48. Apêndice C — Modelo de documentação de componente](#48-apêndice-c--modelo-de-documentação-de-componente)
- [49. Apêndice D — Registro de evidência em JSON](#49-apêndice-d--registro-de-evidência-em-json)
- [50. Apêndice E — Prompt pronto para a IA receptora](#50-apêndice-e--prompt-pronto-para-a-ia-receptora)
- [51. Modelo de relatório de entrega](#51-modelo-de-relatório-de-entrega)
- [52. Governança e evolução](#52-governança-e-evolução)
- [53. Glossário](#53-glossário)
- [54. Registro desta versão](#54-registro-desta-versão)

---

## 0. Instrução principal para a IA que receber este arquivo

Atue como designer de produto e engenheiro de interfaces. Use este documento como especificação de trabalho para reconstruir e documentar a linguagem visual do projeto OnlyGenius. Leia o documento inteiro antes de implementar componentes.

Seu objetivo é produzir uma interface coerente, funcional, responsiva e documentada, preservando a estrutura visual reconhecível da referência. Reutilize os padrões definidos aqui em todas as páginas. Complete comportamentos ausentes com as decisões propostas neste arquivo, registrando sua origem.

### 0.1. Obrigações

1. Identificar os dados confirmados, os padrões observados, as amostras de pixels, as estimativas e as propostas.
2. Preservar a fonte Inter, os pesos publicados e a cor primária confirmada.
3. Centralizar cores, dimensões e estados em tokens reutilizáveis.
4. Implementar componentes compartilhados antes de multiplicar páginas.
5. Usar a mesma anatomia para componentes com a mesma função.
6. Cobrir temas claro e escuro, teclado, mobile, carregamento, vazio e erro.
7. Substituir textos provisórios por conteúdo significativo.
8. Demonstrar as interações que declarar implementadas.
9. Registrar diferenças em relação à referência e justificar as escolhas complementares.
10. Entregar documentação suficiente para outra pessoa continuar o projeto.

### 0.2. Como interpretar este arquivo

- As instruções explícitas do responsável pelo produto e as restrições reais do projeto prevalecem sobre os defaults propostos aqui.
- Uma evidência visual descreve a referência; uma regra normativa determina a implementação desta reconstrução.
- Uma proposta pode ser obrigatória para esta implementação sem se tornar um fato sobre o projeto original.
- Na falta de instrução específica, adote o default declarado neste documento e registre a decisão.
- Se existir uma base de código, preserve suas convenções úteis e adapte os tokens de forma consciente.
- Se o acesso às imagens falhar, use a especificação textual e informe que a comparação visual não foi executada.
- Os links das imagens são referências externas. Este Markdown não incorpora os arquivos binários.

### 0.3. Limites de atribuição

Este documento especifica o sistema visual reconstruído de **um projeto** creditado à ZeeFrames. As imagens analisadas não estabelecem um design system comum a todos os trabalhos da agência.

Os nomes de componentes, tokens, APIs, diretórios e estados apresentados abaixo são convenções desta reconstrução. A biblioteca original de componentes, a biblioteca original de ícones, o arquivo Figma e os nomes internos de tokens permanecem desconhecidos.

---

## 1. Legenda de evidência e força das regras

### 1.1. Classificação

| Código | Significado | Como usar |
|---|---|---|
| C | Confirmado: declarado explicitamente no guia publicado | Preservar; alteração exige uma decisão de produto registrada |
| O | Observado: visível e recorrente nas telas analisadas | Reproduzir a função e a linguagem visual |
| A | Amostrado: valor de pixel em um PNG publicado | Usar como pista; composição pode ter alterado o token original |
| E | Estimado: deduzido das proporções das imagens | Tratar como default de reconstrução, ajustável após validação |
| P | Proposto: decisão nova para completar o sistema | Implementar quando aplicável, identificando sua origem |
| ND | Não determinado: evidência insuficiente | Não atribuir ao projeto original |

Os códigos não representam uma porcentagem de precisão.

### 1.2. Vocabulário normativo

- **DEVE:** requisito desta implementação.
- **RECOMENDADO:** escolha padrão, com alternativa justificável.
- **PODE:** opção quando existir necessidade real.
- **ND:** ponto que não pode ser recuperado com segurança das imagens.

### 1.3. Ordem para resolver conflitos

1. Requisitos explícitos do produto.
2. Dados confirmados da referência.
3. Padrões observados em múltiplas telas.
4. Segurança de uso, legibilidade e acessibilidade da implementação.
5. Defaults normativos deste documento.
6. Preferências locais de uma tela.

Uma decisão que melhore legibilidade sem reproduzir um detalhe frágil da montagem deve ser registrada como P. Quando houver conflito real entre fidelidade e usabilidade, explique a diferença e mantenha a identidade visual por outros meios.

---

## 2. Fontes e evidências consultadas

### 2.1. Páginas principais

| ID | Fonte | Papel na análise |
|---|---|---|
| S01 | [Página do projeto](https://www.awwwards.com/sites/algorithmic-trading-dashboard) | Identificação e crédito |
| S02 | [Font & Colors Scheme](https://www.awwwards.com/inspiration/font-colors-scheme-algorithmic-trading-dashboard) | Fonte, pesos e duas cores declaradas |
| S03 | [Dashboard Layout](https://www.awwwards.com/inspiration/dashboard-layout-algorithmic-trading-dashboard) | Painéis, tabelas, formulários e padrões administrativos |
| S04 | [Dashboard Screens](https://www.awwwards.com/inspiration/dashboard-screens-algorithmic-trading-dashboard) | Apresentação desktop e mobile |
| S05 | [Dashboard](https://www.awwwards.com/inspiration/dashboard-algorithmic-trading-dashboard) | Faturamento, navegação, métricas e tabelas |

### 2.2. Imagens originais

| ID | Link direto | Conteúdo relevante |
|---|---|---|
| I01 | [Paleta declarada](https://assets.awwwards.com/awards/element/2026/03/69ba45016906a585679047.png) | Azul #2563EB e escuro #18181A |
| I02 | [Tipografia declarada](https://assets.awwwards.com/awards/element/2026/03/69ba45017374d242498567.png) | Inter; Regular, Medium, Semibold e Bold |
| I03 | [Montagem de telas do cliente](https://assets.awwwards.com/awards/element/2026/03/69ba44e94946a211935151.png) | Dashboard, conta, serviços, certificados, login e gráficos |
| I04 | [Montagem de telas administrativas](https://assets.awwwards.com/awards/element/2026/03/69ba44e9586bb232298536.png) | Gestão de clientes, receitas, despesas, algoritmos e mapa |
| I05 | [Desktop em dispositivo](https://assets.awwwards.com/awards/element/2026/03/69ba44e961475217457775.png) | Landing page e apresentação do dashboard escuro |
| I06 | [Mobile em dispositivos](https://assets.awwwards.com/awards/element/2026/03/69ba44e96935b601404441.png) | Tema claro, métricas em duas colunas e navegação inferior |
| I07 | [Gestão de faturamento](https://assets.awwwards.com/awards/element/2026/03/69ba44e973816642625323.png) | Tabela financeira, filtros, ações e navegação administrativa |

### 2.3. Metodologia e limites

Foram examinadas cinco imagens de telas e layouts, além das duas imagens do guia de marca. A amostragem de cores foi feita principalmente sobre I04.

- Valores de pixel descrevem a imagem final, não necessariamente a cor original no Figma.
- As montagens contêm redução, perspectiva, sombras e iluminação.
- As duas montagens incluem painéis em escalas diferentes.
- Medidas em pixels da montagem não equivalem diretamente a medidas CSS da aplicação.
- As sombras de dispositivos e pranchas pertencem à apresentação do projeto.
- Os cantos externos de uma prancha não são automaticamente o raio de um componente.
- Os dados e textos nas imagens incluem exemplos e conteúdo provisório.
- Uma tela estática não prova o comportamento de hover, foco, filtro, autenticação ou atualização.

### 2.4. O que está efetivamente confirmado

| Item | Valor | Fonte |
|---|---|---|
| Família tipográfica | Inter | I02 |
| Peso Regular | 400 | I02 |
| Peso Medium | 500 | I02 |
| Peso Semibold | 600 | I02 |
| Peso Bold | 700 | I02 |
| Azul principal | #2563EB | I01 |
| Escuro declarado | #18181A | I01 |

O nome “Racing Green” aparece associado ao escuro declarado. Preserve o valor hexadecimal; o nome comercial não é uma indicação para substituir esse tom por um verde.

---

## 3. Escopo do sistema reconstruído

### 3.1. Incluído

- Fundação visual: cores, tipografia, dimensões, bordas e hierarquia.
- Temas claro e escuro.
- Estrutura de dashboard de cliente e de administração.
- Navegação lateral, navegação inferior e cabeçalhos.
- Cards, botões, campos, seletores, abas, tabelas e estados.
- Visualização de dados e indicadores de progresso.
- Templates para dashboard, listas, detalhes, serviços, certificados e autenticação.
- Contratos de comportamento e critérios de validação.
- Documentação e governança de componentes.

### 3.2. Dependente do produto

O sistema visual não define regras de negócio, cálculos de retorno, permissões reais, negociação, cobrança, autenticação de produção ou integrações. Para essas partes, a IA DEVE utilizar contratos fornecidos pelo produto.

Elementos vistos na referência, como “Surprise box”, afiliados e guias tributários, pertencem ao contexto daquele produto. Só devem aparecer em outro produto quando houver uma necessidade equivalente.

### 3.3. Aplicação a outra marca

Quando o sistema for usado em outro produto:

- Manter anatomia, densidade, hierarquia e consistência.
- Usar o nome, logotipo e conteúdo do produto de destino.
- Alterar a cor de marca somente quando solicitado.
- Registrar a mudança dos tokens de marca.
- Preservar significado e contraste dos estados semânticos.
- Não apresentar o resultado como um arquivo oficial da ZeeFrames.

---

## 4. Identidade visual e princípios

### 4.1. Descrição curta

Interface de operação e análise com alta densidade controlada, superfícies neutras, bordas discretas, tipografia Inter e azul para ações e seleção. As métricas têm destaque; os elementos de apoio permanecem visualmente silenciosos.

### 4.2. Princípios

1. **Dados primeiro:** valores, status e relações devem ser reconhecidos rapidamente.
2. **Hierarquia por estrutura:** posição, tamanho, peso e alinhamento precedem decoração.
3. **Cor com função:** azul para interação; cores semânticas para estado; cores de gráfico para séries.
4. **Anatomia recorrente:** cards e painéis equivalentes repetem a mesma composição.
5. **Densidade consistente:** a interface é compacta, mas conserva leitura e alvos de interação.
6. **Separação sutil:** cards usam contorno leve, com pouca elevação.
7. **Continuidade entre temas:** mudar o tema preserva dimensões, ordem e função.
8. **Adaptação real:** mobile reorganiza navegação e conteúdo, em vez de reduzir a página inteira.
9. **Estados previsíveis:** loading, vazio, erro e sucesso fazem parte do componente.
10. **Conteúdo concreto:** rótulos explicam o que será visto ou feito.

### 4.3. Características observadas

| Característica | Evidência |
|---|---|
| Fundo escuro quase preto no painel | O/A, I03–I04–I07 |
| Menu lateral ligeiramente mais claro | O/A, I03–I04–I07 |
| Cards com borda fina | O, I03–I04–I06 |
| Ações principais azuis | O/C, I01–I03–I04–I07 |
| Números maiores que rótulos | O, I03–I04–I06 |
| Ícones pequenos de traço fino | O, I03–I04–I06 |
| Tabelas com separadores horizontais | O, I03–I04–I07 |
| Abas e filtros compactos | O, I03–I04–I06 |
| Gráficos com área, barras e cores de série | O, I03–I04 |
| Mobile claro com navegação inferior | O, I06 |

---

## 5. Arquitetura de tokens

### 5.1. Três camadas

1. **Primitivos:** valores reutilizáveis, como azul, escala de espaço e pesos.
2. **Semânticos:** função no produto, como fundo, texto, ação, sucesso e borda.
3. **Componentes:** uso específico, como altura de botão, padding de card e largura do menu.

Uma cor literal pode existir na definição de um primitivo. Um componente DEVE usar o token semântico ou de componente correspondente.

### 5.2. Convenção de nomes

O prefixo proposto é **og**, de OnlyGenius. É uma convenção desta reconstrução.

Exemplos:

- og-brand: identidade principal.
- og-background: fundo da aplicação.
- og-text-muted: texto de apoio.
- og-border-strong: contorno funcional que precisa ser discernível.
- og-control-height: altura normal de controle.
- og-radius-card: raio dos cards.
- og-chart-yellow: série amarela adaptada por tema.

### 5.3. Regras

- Cada valor deve ter uma função documentada.
- Cada novo token deve ter origem C/O/A/E/P registrada.
- Um token semântico não deve ser nomeado pela página que o usa.
- Não criar valores diferentes para corrigir pequenas inconsistências de uma tela.
- Separar cor de fundo de estado e cor do texto de estado.
- Separar cor de série de gráfico e cor de status operacional.
- Registrar o par de contraste de cada uso importante.
- Manter números de geometria em uma escala coerente.

---

## 6. Cores

### 6.1. Marca e amostras

| Papel | Valor | Origem |
|---|---|---|
| Azul da marca | #2563EB | C |
| Escuro da marca | #18181A | C |
| Fundo predominante do painel | #09090B | A |
| Menu predominante na montagem | #18171C | A |
| Azul de gráfico amostrado | #2563EC | A |
| Verde de gráfico amostrado | #17A34A | A |
| Laranja de gráfico amostrado | #EA580B | A |
| Amarelo de gráfico amostrado | #E9B308 | A |
| Rosa de gráfico amostrado | #DC2778 | A |
| Roxo de gráfico amostrado | #C026D4 | A |

As pequenas diferenças entre amostras e valores normalizados abaixo não demonstram tokens distintos. Podem resultar da composição do PNG.

### 6.2. Paleta de implementação proposta

| Token | Escuro | Claro | Origem |
|---|---|---|---|
| brand | #2563EB | #2563EB | C |
| brand-hover | #1D4ED8 | #1D4ED8 | P |
| brand-active | #1E40AF | #1E40AF | P |
| brand-foreground | #FFFFFF | #FFFFFF | P |
| background | #09090B | #FFFFFF | A/P |
| surface | #09090B | #FFFFFF | A/P |
| sidebar | #18181A | #F4F4F5 | C aplicado/P |
| surface-raised | #27272A | #F4F4F5 | P |
| surface-hover | #18181A | #F4F4F5 | P |
| border | #27272A | #E4E4E7 | P |
| border-strong | #71717A | #71717A | P |
| text | #FAFAFA | #18181B | P |
| text-muted | #A1A1AA | #6B6B74 | P |
| link | #60A5FA | #1D4ED8 | P |
| focus-ring | #60A5FA | #2563EB | P |
| selection | #1E3A8A | #DBEAFE | P |
| selection-text | #EFF6FF | #1E40AF | P |
| success | #16A34A | #16A34A | P, normalizado da amostra |
| positive-text | #4ADE80 | #15803D | P |
| success-bg | #052E16 | #DCFCE7 | P |
| danger | #DC2626 | #DC2626 | P |
| danger-hover | #B91C1C | #B91C1C | P |
| negative-text | #F87171 | #B91C1C | P |
| danger-bg | #450A0A | #FEE2E2 | P |
| warning | #CA8A04 | #CA8A04 | P |
| warning-text | #FACC15 | #854D0E | P |
| warning-bg | #422006 | #FEF3C7 | P |
| neutral-badge-bg | #27272A | #F4F4F5 | P |

### 6.3. Uso de azul

Azul identifica:

- Ação principal de um contexto.
- Destino selecionado na navegação.
- Seleção ou progresso quando essa função estiver definida.
- Links, com tonalidade semântica adequada ao tema.
- Série de gráfico quando essa for a cor atribuída aos dados.

O botão principal usa brand com brand-foreground. Texto azul pequeno sobre fundo muito escuro deve usar link, que é mais claro. A tonalidade da marca permanece disponível em preenchimentos e elementos de destaque.

### 6.4. Semântica de estado

| Estado | Tratamento |
|---|---|
| Sucesso / ativo / aprovado | Verde, acompanhado de texto |
| Erro / falha / perda | Vermelho, acompanhado de texto |
| Atenção / acesso limitado | Amarelo ou âmbar, acompanhado de texto |
| Pendente / neutro / rascunho | Superfície neutra e rótulo explícito |
| Informativo | Azul ou neutro, conforme o contexto |

Uma variação negativa não é automaticamente uma falha do sistema. A cor do resultado e a cor do estado operacional devem representar conceitos diferentes.

### 6.5. Contornos decorativos e funcionais

- border serve para separar cards, linhas e regiões de baixa ênfase.
- border-strong serve para elementos cuja identificação visual depende do contorno.
- Uma borda decorativa sutil não deve ser usada como único indicador de um campo interativo.
- Um campo pode combinar contorno funcional, rótulo e fundo distinto.
- O foco deve ser visível nos dois temas.
- Não resolver contraste aumentando indiscriminadamente todas as bordas.

O texto auxiliar do tema claro usa #6B6B74 nesta versão. É uma correção P em relação ao exemplo simplificado inicial: #71717A sobre #F4F4F5 fica próximo de 4,40:1 e não atende ao alvo de 4,5:1 para texto normal nessa superfície. O contorno funcional pode continuar usando #71717A, pois tem uma função diferente.

### 6.6. Paleta de gráficos

| Série | Escuro proposto | Claro proposto |
|---|---|---|
| Azul | #2563EB | #2563EB |
| Verde | #16A34A | #15803D |
| Laranja | #EA580C | #C2410C |
| Amarelo | #EAB308 | #A16207 |
| Rosa | #DB2777 | #BE185D |
| Roxo | #C026D3 | #A21CAF |
| Neutro | #71717A | #52525B |

As variantes do tema claro são P: preservam a identidade de cada série com maior discernibilidade em fundo branco.

Uma série mantém sua identidade entre telas e filtros. Não trocar a cor de uma entidade por causa da posição na lista.

---

## 7. Tipografia

### 7.1. Família e carregamento

- Família: Inter [C].
- Fallback: Arial, sans-serif [P].
- Pesos: 400, 500, 600 e 700 [C].
- Carregar os pesos realmente usados.
- Evitar síntese artificial de negrito quando o arquivo correto estiver disponível.
- Seguir a estratégia de hospedagem de fontes do projeto.

### 7.2. Escala proposta

| Uso | Tamanho | Altura de linha | Peso | Origem |
|---|---:|---:|---:|---|
| Informação auxiliar | 12 px | 16 px | 400–500 | E/P |
| Corpo / navegação | 14 px | 20 px | 400–500 | E/P |
| Campo editável mobile | 16 px | 24 px | 400 | P |
| Título de seção | 18 px | 24 px | 600 | E/P |
| Métrica principal | 24 px | 32 px | 600 | E/P |
| Título de página | 28 px | 36 px | 700 | E/P |
| Título de página ampla, opcional | 32 px | 40 px | 700 | P |

### 7.3. Regras de composição

- Manter a hierarquia por função, mesmo quando o tamanho adaptar ao viewport.
- Títulos de página devem ser curtos e específicos.
- Valores numéricos podem usar números tabulares.
- Colunas monetárias devem ter alinhamento consistente, preferencialmente à direita.
- Texto auxiliar usa text-muted; informação essencial deve continuar fácil de ler.
- Rótulos de campos podem usar 14 px para melhorar leitura.
- Texto de 12 px é reservado a apoio, legendas compactas e metadados.
- Não reduzir texto essencial abaixo de 12 px para acomodar um layout.
- Preservar unidades, sinal, moeda e período ao formatar números.
- Permitir nomes longos sem sobrepor ações ou outras colunas.

### 7.4. Uso de truncamento

Pode truncar um nome secundário quando houver acesso ao valor completo por outro meio. Não truncar silenciosamente:

- Valores monetários relevantes.
- Mensagens de erro.
- Status essenciais.
- Rótulos de ações destrutivas.
- Datas necessárias para decidir uma ação.

Se o valor não couber, priorizar reorganização, abreviação documentada ou mudança de layout.

---

## 8. Espaçamento, dimensões e densidade

### 8.1. Escala

Base estimada: múltiplos de 4 px.

| Token | Valor | Uso frequente |
|---|---:|---|
| space-1 | 4 px | Separações mínimas |
| space-2 | 8 px | Ícone e texto; itens compactos |
| space-3 | 12 px | Grupos pequenos |
| space-4 | 16 px | Cards e gaps padrão |
| space-5 | 20 px | Ajustes de composição |
| space-6 | 24 px | Padding de página e seções |
| space-8 | 32 px | Separação de blocos maiores |
| space-10 | 40 px | Uso excepcional em páginas especiais |
| space-12 | 48 px | Uso excepcional, não densidade padrão |

Até 32 px, a escala é E/P. As extensões de 40 e 48 px são P.

### 8.2. Defaults

| Propriedade | Default | Origem |
|---|---:|---|
| Menu lateral expandido | 256 px | E |
| Menu lateral reduzido | 64 px | P |
| Padding de página desktop | 24 px | E |
| Padding de página mobile | 16 px | P |
| Gap entre cards | 16 px | E |
| Padding de card | 16 px | E |
| Card de análise amplo | 20–24 px quando necessário | P |
| Altura normal de controle | 36 px | E |
| Altura de controle para toque | 44 px | P |
| Linha padrão de tabela | 48 px | E |
| Item de navegação | 40 px | P |
| Ícone pequeno | 16 px | E/P |
| Ícone de navegação | 18–20 px | E/P |
| Ícone de navegação mobile | 20–24 px | P |
| Avatar pequeno | 32 px | P |
| Avatar em cabeçalho | 40 px | P |

### 8.3. Raios e bordas

| Uso | Default |
|---|---|
| Badge | 4 px [P] |
| Botão / campo / filtro | 6 px [E] |
| Card | 12 px [E] |
| Modal / drawer | 12–16 px [P] |
| Avatar | Circular [P] |
| Separadores / contorno padrão | 1 px [E] |

Raios externos de dispositivos e montagens não devem ser copiados para todos os componentes.

### 8.4. Densidade

O padrão de dashboard é compacto. A implementação DEVE distinguir:

- Densidade visual: altura, espaço e proporções.
- Área de toque: alvo efetivo de interação.
- Densidade informacional: número de elementos simultâneos.

Um ícone visual pequeno pode ter um alvo maior. Uma versão mobile não deve perder legibilidade para manter a quantidade de colunas desktop.

---

## 9. Superfícies, elevação e ícones

### 9.1. Superfícies

- A aplicação usa background.
- Cards usam surface e border.
- O menu lateral usa sidebar.
- Menus e popovers podem usar surface-raised.
- Áreas de seleção usam selection e selection-text.
- A estrutura principal deve ser legível sem sombras fortes.

### 9.2. Sombras

Cards de conteúdo não precisam de sombra como separador padrão. Menus e modais podem usar sombra discreta para comunicar sobreposição [P].

Proposta:

- Elevação 0: nenhuma sombra.
- Elevação de popover: sombra moderada, concentrada.
- Modal: sombra mais ampla combinada com overlay.

Sombras fotográficas dos mockups não fazem parte desses defaults.

### 9.3. Ícones

Estilo observado: ícones de traço fino, pequenos, acompanhando texto ou representando ações.

Regras propostas:

- Usar uma única família coerente no produto.
- Preferir traço próximo de 1,5–2 px.
- Usar cor herdada do texto ou do estado.
- Alinhar ícones pela caixa visual, não apenas pela origem do SVG.
- Ícone decorativo fica oculto de tecnologia assistiva.
- Ação apenas com ícone recebe nome acessível.
- Estado essencial deve ter texto.

A família original de ícones é ND. A escolha de uma biblioteca de implementação deve ser registrada como P.

---

## 10. Layout da aplicação

### 10.1. Estrutura desktop observada

Ordem recorrente:

1. Menu lateral.
2. Cabeçalho de página com título, apoio e ações.
3. Abas ou filtros quando necessários.
4. Faixa de métricas.
5. Painéis de análise, listas ou tabelas.
6. Paginação ou informações de atualização.

Existem variações por papel de usuário. O painel administrativo tem destinos e ferramentas diferentes do painel do cliente.

### 10.2. Cabeçalho

- Título à esquerda.
- Descrição curta ou data de atualização abaixo.
- Ações à direita no desktop.
- Breadcrumb quando houver uma hierarquia real.
- Ações quebram linha ou passam para um menu quando faltar espaço.
- Uma ação primária deve ser claramente identificável.

### 10.3. Grade

| Região | Desktop amplo | Tablet | Mobile |
|---|---|---|---|
| Métricas | 4 colunas | 2 colunas | 2 colunas quando couber |
| Painéis de gráfico | 2 colunas | 1 ou 2 conforme legibilidade | 1 coluna |
| Cards de serviço | 2 ou 3 conforme conteúdo | 2 ou 1 | 1 coluna |
| Cards de certificado | 2 ou 3 conforme conteúdo | 2 | 1 coluna |
| Detalhes de entidade | Colunas proporcionais | Reorganização | Pilha vertical |

Quatro métricas desktop e duas mobile são O. As demais quantidades e limites são P.

### 10.4. Breakpoints propostos

| Faixa | Comportamento recomendado |
|---|---|
| Menos de 360 px | Métricas em 1 coluna se valores e rótulos não couberem |
| 360–767 px | Navegação inferior; métricas em 2 colunas; painéis em 1 |
| 768–1023 px | Menu reduzido ou recolhível; métricas em 2 colunas |
| 1024–1279 px | Menu reduzido; métricas em 4; gráficos em 2 quando legíveis |
| 1280 px ou mais | Menu expandido; organização desktop completa |

Esses breakpoints são P. O comportamento deve responder ao conteúdo, não apenas a nomes de dispositivos.

### 10.5. Largura e altura

- O conteúdo usa a largura disponível com padding consistente.
- Não impor uma largura máxima estreita a tabelas operacionais amplas.
- Usar uma largura de leitura limitada em formulários e textos longos.
- Não criar alturas fixas em cards com conteúdo variável.
- Cards de uma mesma linha podem alinhar altura pela grade.
- Cabeçalhos fixos ou sticky não devem cobrir conteúdo ou foco.
- Usar min-width: 0 em filhos de grid ou flex que precisem encolher.

### 10.6. Tabelas no mobile

Escolher explicitamente uma estratégia por tabela:

1. Rolagem horizontal contida, mantendo colunas comparáveis.
2. Redução de colunas com acesso aos detalhes.
3. Transformação em lista de cards quando a tarefa não depender da comparação horizontal.

A estratégia deve preservar nome, status, valor e ação essencial. Não fazer a página inteira rolar horizontalmente. O comportamento original de tabelas mobile é ND.

### 10.7. Safe areas

A navegação inferior DEVE respeitar a área segura do dispositivo. O conteúdo deve ter padding inferior suficiente para não ficar escondido atrás da barra.

---

## 11. Anatomia comum dos componentes

Cada componente documentado DEVE incluir:

1. Nome e finalidade.
2. Origem da evidência.
3. Anatomia.
4. Tokens utilizados.
5. Variantes.
6. Estados.
7. Comportamento de teclado e toque.
8. Conteúdo e limites.
9. Adaptação por viewport.
10. Exemplos e critérios de aceitação.

Componentes exibidos em uma prancha estática ainda precisam de contrato de interação. O nome de um componente neste documento não prova que ele existia com esse nome no Figma.

## 12. Botões e ações

**Base:** O, I03–I04–I07. Estados completos: P.

### 12.1. Anatomia

Ícone opcional → rótulo → indicador de carregamento opcional.

| Variante | Aparência | Uso |
|---|---|---|
| Primário | Azul preenchido, texto branco | Principal ação de um contexto |
| Secundário | Superfície neutra, borda e texto principal | Ações de apoio |
| Discreto | Sem preenchimento permanente | Ações de baixa ênfase |
| Destrutivo | Vermelho preenchido ou variante contextual | Excluir, revogar ou cancelar algo de forma destrutiva |
| Link | Cor link e indicação clara de navegação | Destino ou ação textual secundária |

### 12.2. Dimensões

- Altura: 36 px no desktop; 44 px quando for necessário para toque.
- Padding horizontal: 12–16 px.
- Gap ícone/rótulo: 8 px.
- Raio: 6 px.
- Texto: 14 px, peso 500; versão compacta de 12 px apenas em ferramentas densas.

### 12.3. Estados

- Default: aparência da variante.
- Hover: mudança discreta de fundo ou tonalidade.
- Active: feedback curto de pressionamento.
- Focus-visible: anel de foco perceptível.
- Disabled: comportamento bloqueado, com aparência secundária.
- Loading: indicador e bloqueio de submissão duplicada, preservando a largura.
- Success/error: resultado comunicado no contexto da ação.

### 12.4. Regras

- Usar button para ações e a para navegação.
- Informar type em botões de formulário.
- O rótulo deve nomear a ação: “Salvar alterações”, “Exportar relatório”.
- A mesma ação deve manter o mesmo nome entre telas.
- Uma ação destrutiva deve explicar sua consequência antes da confirmação quando necessário.
- Em loading, preservar o contexto e sinalizar ocupação.
- Ícones não substituem o nome acessível.
- Não preencher todas as ações de uma toolbar com azul.

---

## 13. Botão de ícone e menu de ações

**Base:** O, I03–I04–I07. Comportamento: P.

- Visual compacto de 32–36 px, com alvo ampliado quando necessário.
- Ícone de 16–20 px.
- Nome acessível descreve a ação, como “Abrir ações da conta”.
- Tooltip pode esclarecer o nome para usuários que usam ponteiro.
- Ações por linha podem ficar em um menu quando há muitas opções.
- Itens destrutivos devem ser identificados por texto e tratamento visual.
- Fechar o menu deve devolver foco ao acionador quando apropriado.
- A biblioteca usada deve implementar o padrão de teclado correspondente.

Referência de comportamento: [W3C APG — Menu Button](https://www.w3.org/WAI/ARIA/apg/patterns/menu-button/). A aplicação do padrão é uma decisão P.

---

## 14. Campos de formulário

**Base:** O, I03. Estados e validação: P.

### 14.1. Anatomia

Rótulo → campo → descrição opcional → mensagem de erro contextual.

### 14.2. Regras visuais

- Label persistente; placeholder funciona como exemplo.
- Altura de 36 px desktop e 44 px em contextos de toque.
- Texto editável mobile de 16 px.
- Padding horizontal de 12 px.
- Raio de 6 px.
- Contorno funcional discernível.
- Ícones opcionais com espaço reservado.
- Mensagens de apoio não podem mover o campo horizontalmente.

### 14.3. Estados

| Estado | Comportamento |
|---|---|
| Vazio | Label e exemplo de conteúdo |
| Preenchido | Valor completo, legível |
| Foco | Anel ou tratamento de foco claro |
| Inválido | Mensagem específica e vínculo ao campo |
| Desabilitado | Sem edição, com motivo quando necessário |
| Somente leitura | Valor acessível e selecionável, sem edição |
| Carregamento | Explicar a dependência, preservando conteúdo útil |

### 14.4. Validação

- Não depender apenas de borda vermelha.
- Vincular label, descrição e erro ao campo.
- Preservar os valores após falha de envio.
- Levar o usuário ao primeiro erro relevante após tentativa de submissão.
- Aceitar colar conteúdo em campos em que isso faz sentido.
- Selecionar tipo de input e autocomplete pelo significado real.
- Mostrar ou ocultar senha com um controle nomeado.
- Não anunciar erros a cada tecla sem uma razão funcional.

---

## 15. Seletores, pesquisa, data e filtros

**Base:** O, I03–I04–I07. Interações completas: P.

### 15.1. Select

- Valor selecionado à esquerda e indicador de abertura à direita.
- Mesma altura, borda e raio dos campos.
- Opção atual claramente identificada.
- Usar select nativo quando adequado.
- Para seletores customizados, preferir uma primitiva acessível já adotada no projeto.
- Não renderizar um select apenas como um bloco de texto com seta.

### 15.2. Pesquisa

- Label ou nome acessível que indique o que será pesquisado.
- Ícone pode reforçar a função.
- Limpar a pesquisa deve ser fácil e preservar o foco.
- Estados de pesquisa: inicial, digitando, buscando, resultado, vazio e erro.
- Aplicar atraso de consulta apenas quando necessário à experiência.
- Registrar se a pesquisa é local, remota ou demonstrativa.

### 15.3. Data e período

- Mostrar o período atual, não apenas “Selecionar data” após uma escolha.
- Intervalos têm início e fim claros.
- Indicar fuso horário quando isso alterar a leitura do dado.
- Presets como “Últimos 6 meses” dependem do produto.
- Períodos de comparação devem ser explícitos.
- Validar início/fim e preservar uma escolha anterior válida após erro.

### 15.4. Toolbar de filtros

Ordem proposta: filtros principais → limpar filtros → busca → ações secundárias.

- No desktop, alinhar os grupos em uma ou duas linhas previsíveis.
- No mobile, reorganizar ou abrir filtros adicionais em drawer.
- Mostrar quais filtros estão ativos.
- “Limpar filtros” restaura um estado inicial definido.
- Filtro não deve alterar silenciosamente o significado de uma métrica.
- Mudanças devem atualizar os resultados correspondentes.
- Se houver atraso, mostrar o estado de atualização sem apagar todos os dados.

---

## 16. Checkbox, radio e switch

**Base:** checkbox de login O, I03. Demais regras e switch: P.

- Checkbox representa seleção independente.
- Radio representa uma escolha entre alternativas.
- Switch representa uma configuração binária quando a mudança for imediata ou claramente comunicada.
- Tamanho visual sugerido: 16–20 px; área clicável maior.
- O texto associado também deve ativar o controle quando apropriado.
- Estados: desmarcado, marcado, foco, desabilitado; indeterminado em seleção agregada.
- Não usar um switch como botão de submissão.
- O estado e o label devem continuar compreensíveis sem cor.

---

## 17. Abas e controles segmentados

**Base:** O, I03–I04–I06. Teclado: P.

### 17.1. Abas sublinhadas

- Texto em peso 500.
- Indicador inferior de aproximadamente 2 px.
- Espaço entre itens próximo de 16–24 px.
- Altura útil próxima de 40–44 px.
- Item ativo tem contraste e indicador persistentes.

### 17.2. Controle segmentado

- Base neutra com opções agrupadas.
- Opção selecionada usa superfície ou contraste distinto.
- Exemplos: “Hoje”, “Semanal”, “Mensal”, “Personalizado”.
- Usar semântica de abas quando as opções selecionarem painéis.
- Usar semântica apropriada de seleção quando representarem um valor de filtro.

### 17.3. Regras de comportamento

Documentar ativação automática ou manual. Associar aba ao painel. O foco e a seleção são estados diferentes. Se o conteúdo demorar a carregar, preservar previsibilidade do teclado e mostrar o carregamento no painel.

Referência: [W3C APG — Tabs](https://www.w3.org/WAI/ARIA/apg/patterns/tabs/).

---

## 18. Navegação lateral

**Base:** O, I03–I04–I07. Dimensões: E/P.

### 18.1. Anatomia

Marca → destinos principais → destinos auxiliares → notificações e perfil, quando aplicáveis.

### 18.2. Composição

- Largura expandida de 256 px.
- Versão reduzida proposta de 64 px.
- Ícone acompanhado de texto na versão expandida.
- Item selecionado com fundo azul mais escuro e texto legível.
- Os destinos devem manter ordem estável.
- Área inferior pode agrupar suporte, perfil e ações auxiliares.
- Menu extenso pode ter rolagem própria, sem esconder o destino selecionado.

### 18.3. Regras

- O item atual deve ser reconhecível por forma e contraste.
- Links devem permitir navegação real.
- Usar indicação semântica de página atual.
- A versão reduzida deve preservar nomes acessíveis.
- Tooltip pode revelar o nome, mas não deve ser a única forma de descobrir funções essenciais no toque.
- Permissões podem remover destinos indisponíveis, preservando clareza do produto.
- Painel do cliente e painel administrativo podem ter menus distintos com a mesma linguagem visual.

---

## 19. Navegação inferior mobile

**Base:** O, I06. Implementação: P.

As imagens mostram cinco destinos, equivalentes a Dashboard, Contas, Certificados, Afiliados e Mais.

### 19.1. Default para reprodução do contexto original

| Destino | Label sugerido em português |
|---|---|
| Dashboard | Painel |
| Trading accounts | Contas |
| Certificates | Certificados |
| Affiliates | Afiliados |
| More | Mais |

Esses nomes devem ser adaptados ao produto de destino.

### 19.2. Regras

- Ícone e texto curto em cada destino.
- Item selecionado usa azul e indicação persistente.
- Destinos principais continuam acessíveis por toque.
- Considerar safe area inferior.
- Reservar espaço no conteúdo para a barra.
- A opção “Mais” abre uma lista real de destinos secundários.
- Não repetir simultaneamente menu lateral e barra inferior no mesmo contexto sem justificativa.
- Não transformar cada ação de uma página em destino da navegação global.

---

## 20. Breadcrumb e cabeçalho de entidade

**Base:** O, I03–I04–I06.

### 20.1. Breadcrumb

- Representa hierarquia, não histórico aleatório.
- Níveis anteriores são links.
- O nível atual é identificado.
- Pode reduzir níveis intermediários no mobile.
- Não substituir o título da página.

### 20.2. Cabeçalho de entidade

Anatomia proposta: avatar opcional → nome → identificador → status → metadados → ações.

Exemplos observados incluem detalhes de cliente e de conta.

- Identificador não deve competir com o nome.
- Status deve ser explícito.
- Data de criação ou atualização usa texto de apoio.
- Ações críticas permanecem distinguíveis.
- Conteúdo longo deve quebrar linha sem desalinhamento destrutivo.

---

## 21. Card de métrica

**Base:** O, I03–I04–I06–I07. É um dos padrões centrais.

### 21.1. Anatomia obrigatória

1. Ícone opcional e label.
2. Valor principal.
3. Variação, período ou informação de apoio.

### 21.2. Aparência

- Borda discreta de 1 px.
- Raio estimado de 12 px.
- Padding estimado de 16 px.
- Label de 12–14 px.
- Valor de 24 px, peso 600.
- Variação de 12 px ou tamanho maior quando necessário.
- Sem sombra forte por padrão.

### 21.3. Regras de dados

- Label define o indicador.
- Valor preserva moeda ou unidade.
- Variação informa base de comparação.
- Crescimento não implica automaticamente resultado favorável.
- Se não houver dado, mostrar ausência; não transformar ausência em zero.
- Dados desatualizados recebem indicação de atualização.
- Arredondamentos devem seguir a regra de produto.

### 21.4. Estados

| Estado | Resultado |
|---|---|
| Default | Valor e contexto completos |
| Loading inicial | Skeleton com dimensões estáveis |
| Atualizando | Valor anterior pode permanecer, com indicação |
| Sem dado | Símbolo ou mensagem de indisponibilidade |
| Erro | Mensagem curta e recuperação quando possível |
| Compacto | Proporções ajustadas sem esconder unidade ou sinal |

### 21.5. Aceitação

A mesma métrica em duas páginas usa o mesmo nome e formato, salvo uma mudança de contexto documentada.

---

## 22. Card de conteúdo e painel de gráfico

**Base:** O, I03–I04.

### 22.1. Anatomia

Cabeçalho → conteúdo → rodapé opcional.

O cabeçalho pode conter:

- Título de seção.
- Descrição curta.
- Informação de ajuda.
- Filtro de período.
- Ação secundária.

### 22.2. Regras

- Usar alinhamento consistente entre cards da mesma linha.
- Filtros do painel afetam apenas o conteúdo previsto.
- Evitar um card dentro de outro card sem uma divisão funcional real.
- A altura do gráfico deve comportar eixos e labels.
- O rodapé pode explicar origem, atualização ou ação de navegação.
- Componentes com conteúdo variável podem crescer.

### 22.3. Visualização de informação

Uma descrição abaixo do título explica o indicador. Não usar texto genérico de preenchimento. O usuário deve compreender o gráfico sem depender apenas de tooltip.

---

## 23. Badges de status e indicadores de variação

**Base:** O, I03–I04–I07. Tokens finais: P.

### 23.1. Badge

- Texto curto.
- Padding vertical aproximado de 2–4 px.
- Padding horizontal de 6–8 px.
- Raio de aproximadamente 4 px.
- Fundo semântico discreto.
- Texto de estado legível.

### 23.2. Distinções

- Um badge estático comunica um estado.
- Um chip removível representa um filtro ou seleção.
- Um botão executa uma ação.
- Esses elementos não devem parecer ou funcionar de modo indistinguível.

### 23.3. Variação

- Exibir sinal ou indicação textual.
- Mostrar percentual ou diferença absoluta com a unidade correta.
- “+10% no período” é diferente de “+10 pontos percentuais”.
- Comparação desconhecida não deve receber uma direção inventada.
- Usar cor apenas como reforço do significado.

---

## 24. Tabela de dados

**Base:** O, I03–I04–I07.

### 24.1. Anatomia

Título e contexto → toolbar → cabeçalho de colunas → linhas → paginação/resumo.

### 24.2. Defaults

- Cabeçalho menor e de ênfase secundária.
- Separadores horizontais discretos.
- Linha próxima de 48 px.
- Padding horizontal de 12–16 px.
- Números com alinhamento consistente.
- Coluna de ações compacta.
- Status legível em badge ou texto.
- Ausência de zebra forte como padrão.

### 24.3. Colunas

Documentar para cada coluna:

- Nome.
- Campo de dados.
- Formato.
- Alinhamento.
- Ordenação.
- Possibilidade de ocultar.
- Prioridade no mobile.
- Comportamento com valor ausente.

### 24.4. Ordenação e seleção

- Ordenação deve indicar direção e afetar os dados.
- Seleção de linhas deve informar o escopo: página ou conjunto completo.
- Seleção agregada pode apresentar estado indeterminado.
- Ações em lote devem informar quantos itens serão afetados.
- Uma tabela comum deve manter semântica de table.
- Usar uma grade interativa complexa apenas quando o produto realmente exigir navegação por células.

### 24.5. Estados

- Carregamento inicial.
- Atualização com dados existentes.
- Lista vazia.
- Nenhum resultado para filtros.
- Erro de consulta.
- Resultado parcial.
- Uma linha.
- Muitas linhas.
- Texto longo.
- Permissão insuficiente.

### 24.6. Aceitação

Busca, filtros, ordenação, total e paginação devem concordar sobre o conjunto exibido. Não mostrar um total de resultado antigo após uma mudança de filtro.

---

## 25. Paginação

**Base:** O, I03–I04–I07. Defaults funcionais: P.

- Indicar página atual.
- Permitir anterior e próxima.
- Mostrar total conhecido e quantidade exibida.
- Usar ellipsis somente quando necessário.
- Tornar indisponíveis os destinos inexistentes.
- Definir tamanho de página; default inicial proposto de 10 registros.
- Ao alterar filtros, retornar à primeira página ou a uma página válida.
- Distinguir paginação local e remota.
- Preservar o contexto após mudança de página.
- Não estimar um total exato se a API só informar continuidade.

---

## 26. Progresso e metas

**Base:** O, I03.

### 26.1. Anatomia

Nome da meta → valor ou percentual → trilha → preenchimento → contexto.

### 26.2. Regras

- A largura preenchida representa o mesmo valor anunciado.
- Percentuais devem corresponder à meta definida.
- Se houver progresso superior a 100%, explicar a condição.
- Dados desconhecidos não devem preencher a barra.
- Uma barra de progresso não é uma decoração animada.
- O texto deve permitir compreender o indicador sem cor.
- Carregamento indeterminado e conclusão de meta são componentes diferentes.
- Usar track neutro e preenchimento azul ou semântico conforme a função.

---

## 27. Cards de serviço, algoritmo e certificado

**Base:** O, I03–I04.

### 27.1. Serviço / algoritmo

Anatomia:

1. Símbolo ou ícone da entidade.
2. Nome.
3. Status.
4. Informações relevantes, como validade e acesso.
5. Ações de detalhes ou contratação quando fizerem parte do produto.

As imagens apresentam entidades como OnlySystem, OnlyHedge e OnlyBlackBox. Usar os nomes somente no contexto original ou em demonstração claramente identificada.

### 27.2. Certificado

Anatomia:

- Prévia ou ícone de documento.
- Nome.
- Entidade ou sistema associado.
- Data e fase, quando aplicáveis.
- Ação de visualizar.
- Ação de download quando existir um arquivo.

### 27.3. Regras

- Status “bloqueado” deve explicar o próximo passo disponível.
- Download deve entregar um arquivo ou informar a indisponibilidade.
- Imagem de documento não substitui metadados.
- Ações desabilitadas devem ter motivo compreensível.
- Não criar serviços, credenciais ou certificados fictícios em produção.

---

## 28. Perfil, avatar e notificações

**Base:** O, I04–I07. Comportamento complementar: P.

- Avatar deve ter fallback.
- Nome e papel não devem ser confundidos.
- Menu de perfil agrupa ações relacionadas à conta.
- Contador de notificações deve refletir um estado real ou dado demonstrativo identificado.
- Notificação lida e não lida precisam de diferenciação clara.
- Não depender apenas de um ponto colorido.
- Abrir notificações deve apresentar conteúdo ou um estado vazio significativo.
- Informações privadas devem seguir as permissões do produto.

---

## 29. Tooltip, ajuda e mensagens

**Tooltip: O parcial; comportamento P. Mensagens estruturadas: P.**

### 29.1. Tooltip

- Complementa informação; não contém o único conteúdo essencial.
- Aparece com foco e ponteiro.
- Tem texto curto.
- Não deve bloquear uma ação.
- No toque, oferecer acesso alternativo quando a explicação for importante.

### 29.2. Ajuda contextual

Explique termos específicos com texto claro. Exemplo: o significado de um indicador deve ser definido pelo produto, não deduzido da sigla.

### 29.3. Feedback

| Tipo | Uso |
|---|---|
| Inline | Erro de campo ou situação local |
| Banner | Estado persistente relevante para a página |
| Toast | Resultado breve de uma ação concluída |
| Dialog | Decisão que exige atenção e resposta |

Erro importante deve permanecer acessível até ser resolvido ou dispensado de forma consciente.

---

## 30. Modal e drawer

**Origem:** P. As imagens não comprovam uma biblioteca ou comportamento modal completo.

### 30.1. Anatomia

Título → descrição opcional → conteúdo → ações.

### 30.2. Modal

- Usar para uma decisão ou tarefa breve.
- Definir foco inicial adequado à tarefa.
- Manter foco dentro do contexto modal.
- Devolver foco ao acionador ao fechar.
- Permitir fechamento previsível quando não houver uma operação que impeça isso.
- Descrever claramente consequência e objeto em confirmações destrutivas.
- Evitar modais empilhados.

Referência: [W3C APG — Dialog Modal](https://www.w3.org/WAI/ARIA/apg/patterns/dialog-modal/).

### 30.3. Drawer

- Pode organizar filtros ou navegação secundária no mobile.
- Deve deixar claro se é modal.
- Se modal, aplicar as mesmas regras de foco.
- Garantir acesso ao fechamento.
- Footer de ações não pode esconder conteúdo ou teclado virtual.

### 30.4. Defaults visuais

- Raio: 12–16 px.
- Padding: 24 px desktop e 16 px mobile.
- Fundo de superfície adequado ao tema.
- Ações agrupadas no final da tarefa.
- Largura definida pela necessidade; default de modal simples próximo de 480 px.
- Overlay e sombra são P, sem relação com sombras fotográficas dos mockups.

---

## 31. Carregamento, vazio, erro e atualização

**Origem:** P. Esses contratos completam o sistema.

### 31.1. Carregamento inicial

- Preservar o esqueleto principal.
- Usar skeleton proporcional ao conteúdo.
- Evitar salto de layout.
- Indicar ocupação de maneira acessível.
- Não simular um valor financeiro que ainda não chegou.

### 31.2. Atualização

- Manter dados anteriores quando fizer sentido.
- Indicar que estão sendo atualizados.
- Mostrar data de referência quando relevante.
- Substituir os dados de forma consistente.

### 31.3. Vazio

Distinguir:

- Ainda não existem registros.
- Os filtros não encontraram resultado.
- Não há permissão para visualizar.
- A integração ainda não disponibilizou dados.

Um estado vazio deve oferecer um próximo passo válido, como limpar filtros, adicionar um registro ou verificar uma integração.

### 31.4. Erro

- Explicar o que falhou.
- Preservar dados e contexto utilizáveis.
- Oferecer tentativa novamente quando disponível.
- Evitar detalhes internos que não ajudem o usuário.
- Registrar detalhes técnicos na camada apropriada de observabilidade.

### 31.5. Conteúdo demonstrativo

Uma demonstração sem backend pode usar dados locais, mas deve executar as interações locais que anuncia e identificar limitações. A documentação deve separar uma simulação funcional de uma integração real.

## 32. Visualização de dados

**Base:** O, I03–I04–I07. Contratos de dados e interação: P.

### 32.1. Famílias observadas

- Linha e área para evolução.
- Barras para comparação.
- Barras de progresso.
- Mapa com distribuição geográfica.
- Indicadores numéricos e listas auxiliares.

### 32.2. Regras comuns

1. Definir o indicador e a unidade.
2. Definir o período e a origem dos dados.
3. Manter significado das cores entre visualizações equivalentes.
4. Usar eixos e labels legíveis.
5. Preservar as proporções dos dados.
6. Tratar ausência e erro separadamente de zero.
7. Oferecer resumo textual e acesso aos valores quando necessário.
8. Adaptar quantidade de ticks ao espaço.
9. Manter filtros e dados sincronizados.
10. Validar valores extremos e datasets pequenos.

### 32.3. Linha e área

- Linha azul é um padrão observado.
- Área pode usar transparência ou gradiente discreto abaixo da linha.
- A curva visual deve representar os dados com honestidade.
- Não usar suavização que crie máximos ou mínimos inexistentes.
- Intervalos ausentes não devem ser conectados sem uma regra definida.
- A legenda deve informar a série.
- Tooltip deve apresentar período, valor e unidade.
- Atualização preserva a identidade da série.

### 32.4. Barras

- Cantos superiores podem ser levemente arredondados.
- Não converter barras em cápsulas exageradas.
- Em comparações quantitativas por altura, usar baseline zero ou explicar claramente a exceção.
- Valores negativos devem ter representação coerente.
- Cor por mês aparece nas imagens; uma implementação deve explicar o mapeamento escolhido.
- Para uma série simples, cor única pode ser mais clara.
- Se a reprodução exigir cores por período, manter a associação estável e não atribuir semântica de sucesso a todo mês verde.

### 32.5. Mapeamentos observados no contexto original

| Contexto | Padrão visual |
|---|---|
| Evolução de capital | Linha / área azul |
| Receita e despesa | Séries diferenciadas, incluindo azul e laranja |
| Distribuição de assinaturas | Barras por sistema; aparecem azul, amarelo e neutro |
| Despesas por período | Barras rosas em uma das telas administrativas |
| Metas de certificados | Trilhas com preenchimento azul |

Esses exemplos são O. Os dados e o significado final devem vir do produto. Não inferir uma regra universal de cor a partir de uma única visualização.

### 32.6. Tooltip de gráfico

- Período e valores alinhados.
- Unidade consistente com o eixo.
- Contraste adequado ao tema.
- Posição que não esconda o ponto de interesse.
- Leitura possível por toque.
- Alternativa por teclado ou tabela para informação essencial.
- Tooltip não deve ser a única forma de compreender uma tendência.

### 32.7. Grade e labels

- Grade fina e neutra.
- Labels menores que o conteúdo principal, mas legíveis.
- Títulos e valores em texto normal, sem colorir todo o painel pela série.
- Evitar excesso de casas decimais nos ticks.
- Não sobrepor rótulos; reduzir quantidade ou reorganizar.
- Reservar espaço para labels longos.

### 32.8. Mapa

- Usar geometria geográfica real e dados de localização válidos.
- Explicar a medida usada na cor ou no tamanho.
- Incluir legenda quando houver uma escala quantitativa.
- Distinguir região sem dado de valor zero.
- Oferecer lista ou tabela equivalente quando necessária à tarefa.
- O mapa observado não autoriza inventar países, clientes ou totais.

### 32.9. Biblioteca

A biblioteca original de gráficos é ND. A IA pode usar a biblioteca já adotada no projeto, documentando a escolha. A prioridade é o contrato de dados, a fidelidade visual, a acessibilidade e o comportamento responsivo.

---

## 33. Templates de página

Todos os templates abaixo usam os mesmos tokens e componentes. Eles são modelos de organização, não regras de negócio completas.

### 33.1. Dashboard do cliente

**Base:** O, I03–I06.

1. Título “Dashboard” ou equivalente do produto.
2. Descrição de propósito e ação de suporte.
3. Quatro métricas iniciais, quando o produto fornecer essas medidas.
4. Evolução de capital.
5. Retornos mensais.
6. Desempenho por algoritmo.
7. Progresso de certificados ou metas.

No mobile: métricas em duas colunas quando couberem; conteúdo analítico em pilha; navegação inferior.

### 33.2. Dashboard administrativo

**Base:** O, I04–I07.

1. Cabeçalho.
2. Métricas de operação.
3. Receita e despesa.
4. Distribuição de assinaturas.
5. Certificados, tickets ou listas operacionais.
6. Informações de atualização.

As métricas devem pertencer ao papel administrativo, mantendo o mesmo componente visual.

### 33.3. Gestão de clientes

**Base:** O, I04.

1. Cabeçalho com ação de criar cliente quando disponível.
2. Filtros por país, status e sistema.
3. Busca e seleção de data.
4. Cards de totais.
5. Tabela de clientes.
6. Ações por registro.
7. Paginação.

### 33.4. Detalhes de cliente ou conta

**Base:** O, I03–I04–I06.

1. Breadcrumb.
2. Identidade e status.
3. Ações relevantes.
4. Abas por assunto.
5. Métricas do contexto.
6. Gráficos ou histórico.
7. Informações complementares.

Não incluir uma ação de credencial ou de exclusão apenas para preencher o cabeçalho.

### 33.5. Receitas e despesas

**Base:** O, I04.

1. Título e contexto.
2. Ações de registro, se existirem.
3. Totais de receita, despesa e resultado.
4. Gráficos por período.
5. Lista ou tabela de lançamentos.
6. Filtros, busca e paginação.

### 33.6. Faturamento

**Base:** O, I07.

1. Título e última atualização.
2. Sincronização, emissão ou salvar conforme o produto.
3. Filtros de cliente, status e data.
4. Cards financeiros.
5. Tabela de receita/despesa por entidade.
6. Lista de faturas.
7. Status de pagamento e ações.

### 33.7. Algoritmos

**Base:** O, I04.

1. Cabeçalho e ações autorizadas pelo produto.
2. Métricas de uso ou disponibilidade.
3. Lista com nome, usuários, atualização, arquivos e status.
4. Ações por algoritmo.

Upload ou download só é considerado funcional quando houver uma implementação real ou uma simulação identificada.

### 33.8. Serviços

**Base:** O, I03.

Cards de sistema ou serviço com status, acesso, validade e ações. A aparência mantém borda discreta e tipografia consistente.

### 33.9. Certificados

**Base:** O, I03.

Filtros, cards de documento, metadados, visualização e download. O progresso de metas pode usar o componente de progresso.

### 33.10. Autenticação

**Base:** O, I03.

1. Marca.
2. Título da tarefa.
3. Alternância entre entrar e criar conta, quando disponível.
4. Campos.
5. Recuperação de acesso.
6. Opção de manter sessão quando aplicável.
7. Ação primária.
8. Autenticação externa, se integrada.

Proposta de largura de formulário: aproximadamente 360–420 px, limitada pela viewport. Feedback de autenticação é definido pela integração e deve preservar clareza.

### 33.11. Landing page

**Base:** O parcial, I05.

A referência apresenta navegação horizontal, título de destaque, palavra azul, apoio, chamada de ação e apresentação do dashboard.

Se uma landing page fizer parte do escopo, ela deve compartilhar fonte e marca, mas pode ter espaçamento e escala editorial próprios. Não transferir o hero grande para páginas operacionais.

---

## 34. Conteúdo, localização e formatação

### 34.1. Linguagem

- Usar o idioma definido pelo produto.
- Se não houver definição, usar português do Brasil nesta reconstrução [P].
- Usar rótulos curtos, diretos e consistentes.
- Manter um glossário para termos de domínio.
- Remover lorem ipsum antes da entrega.
- Não expor nomes de bibliotecas ou estados internos em textos de produto.

### 34.2. Números

- Formatar por locale.
- Manter a moeda fornecida pelo dado; idioma não determina moeda.
- As imagens usam exemplos em dólares, sem definir uma moeda obrigatória para outro produto.
- Documentar precisão, arredondamento e abreviação.
- Usar K/M ou equivalentes somente quando houver uma convenção estabelecida.
- Expor o valor completo quando a abreviação dificultar a tarefa.
- Preservar sinal e unidade ao exportar.

### 34.3. Dados financeiros e de desempenho

Saldo, capital, equity, lucro, perda e retorno são conceitos de produto. A IA DEVE usar suas definições reais. A interface não deve trocar esses termos ou inventar equivalência.

Cada métrica deve informar:

1. Definição.
2. Unidade.
3. Período.
4. Origem.
5. Frequência de atualização.
6. Regra para ausência.
7. Regra de comparação.

### 34.4. Datas e períodos

- Definir formato curto e completo.
- Mostrar hora e fuso quando necessário.
- Não misturar períodos locais e UTC sem conversão deliberada.
- Diferenciar “última atualização” da data do evento.
- Em comparação temporal, identificar os dois períodos.

### 34.5. Textos de estado

| Situação | Modelo proposto |
|---|---|
| Nenhum resultado | “Nenhum resultado para estes filtros.” |
| Lista inicial vazia | “Ainda não há registros.” |
| Erro de atualização | “Não foi possível atualizar os dados. Tente novamente.” |
| Salvo | “Alterações salvas.” |
| Download indisponível | “O arquivo ainda não está disponível.” |
| Dados antigos | “Exibindo dados da última atualização.” |

Adaptar ao contexto real. Uma ação de recuperação só deve aparecer quando estiver disponível.

---

## 35. Contratos de dados e estados

**Origem:** P. Exemplo de modelagem, adaptável à stack.

### 35.1. Estado assíncrono

Separar resultado, carregamento inicial e atualização:

~~~ts
type AsyncState<T> =
  | { kind: "idle" }
  | { kind: "loading" }
  | { kind: "empty"; reason: "no-data" | "filtered" | "unavailable" }
  | { kind: "error"; message: string; retryable: boolean }
  | {
      kind: "ready";
      data: T;
      updating: boolean;
      updatedAt?: string;
      stale?: boolean;
    };
~~~

O contrato real pode ter permissão e erro parcial. Não reduzir todos os casos a um booleano de loading.

### 35.2. Métrica

~~~ts
type MetricValue =
  | { kind: "money"; amount: string; currency: string }
  | { kind: "number"; value: number; unit?: string }
  | { kind: "percent"; value: number }
  | { kind: "unavailable" };

type MetricComparison = {
  value: number;
  unit: "percent" | "percentage-point" | "absolute";
  label: string;
  favorability: "positive" | "negative" | "neutral";
};

type MetricModel = {
  id: string;
  label: string;
  value: MetricValue;
  comparison?: MetricComparison;
  help?: string;
};
~~~

Neste exemplo, amount usa string decimal para preservar a representação recebida. A implementação deve seguir o contrato e a estratégia de precisão do projeto.

Definir se percent usa escala 0–100 ou 0–1. Default proposto aqui: 0–100. Não misturar as duas convenções.

### 35.3. Série de gráfico

~~~ts
type ChartPoint = {
  x: string | number;
  y: number | null;
};

type ChartSeries = {
  id: string;
  label: string;
  colorToken: string;
  unit: string;
  points: ChartPoint[];
};
~~~

null representa ausência, não zero. Cada série tem identidade e token de cor estáveis.

### 35.4. Estado de tabela

Documentar:

- Termo de pesquisa.
- Filtros aplicados.
- Campo e direção de ordenação.
- Página ou cursor.
- Tamanho de página.
- Total conhecido, quando houver.
- Seleção.
- Estado de consulta.

Mudança de filtro deve produzir um estado coerente. Persistência em URL ou armazenamento é P e depende da experiência desejada.

---

## 36. Temas e preferências

### 36.1. Cobertura

Tema claro e escuro são O. Comportamento de alternância e persistência é P.

### 36.2. Defaults propostos

- Suportar claro, escuro e preferência do sistema quando fizer sentido.
- Preferência explícita do usuário tem prioridade.
- Persistir a escolha localmente quando autorizado pelo contexto do produto.
- Aplicar o tema a todas as superfícies, overlays e gráficos.
- Evitar flash de tema incorreto no carregamento.
- Usar o mesmo conteúdo e dimensões nos dois temas.
- Ícones e logos precisam de variantes legíveis.

### 36.3. Revisão por tema

Verificar campos, selects, badges, foco, tooltips, modais, skeletons, gráficos e mensagens. Um componente não está completo se apenas o tema de desenvolvimento funcionar.

---

## 37. Movimento e microinterações

**Origem:** P. O comportamento original de animação é ND.

| Situação | Default proposto |
|---|---|
| Hover e cor | 120 ms |
| Feedback comum | 180 ms |
| Drawer ou modal | 180–240 ms |
| Movimento de layout | Apenas quando melhora compreensão |

Regras:

- Priorizar estabilidade.
- Não animar números com valores intermediários enganosos.
- Evitar movimento contínuo em painéis de operação.
- Não introduzir animação de entrada em cada card ao carregar uma página.
- Respeitar preferência por movimento reduzido.
- Preservar funcionalidade sem animação.
- Não usar transição para ocultar atrasos de consulta.

---

## 38. Acessibilidade

### 38.1. Meta de implementação

Adotar WCAG 2.2 nível AA como alvo de validação. Isso é P e não uma declaração de conformidade da referência.

Referência: [W3C — WCAG 2.2 Quick Reference](https://www.w3.org/WAI/WCAG22/quickref/).

Aplicar, conforme o critério e suas condições:

- Contraste mínimo de 4,5:1 para texto normal e 3:1 para texto grande.
- Contraste de 3:1 para informação visual não textual necessária à identificação.
- Operação por teclado e foco visível.
- Nomes, funções e estados acessíveis.
- Informação que não dependa apenas de cor.
- Reorganização com ampliação e viewport estreita.
- Rótulos, identificação de erros e mensagens de status.
- Alvos de interação conforme o critério 2.5.8 e suas exceções.

O produto adota 44 px como alvo recomendado para toque; isso é uma escolha de implementação e não uma simplificação do mínimo exigido por todos os critérios.

### 38.2. Regras práticas do projeto

- Botão de ícone recebe nome.
- Elemento decorativo não produz ruído de leitura.
- O foco não desaparece atrás de cabeçalho ou barra inferior.
- Dialog, menu e tabs usam padrões de teclado documentados.
- Valores de progresso são comunicados em texto.
- Um status tem label além da cor.
- Erros são associados ao campo.
- Mensagens assíncronas são anunciadas sem repetição excessiva.
- Gráficos oferecem resumo e valores acessíveis quando a tarefa exigir.
- Tabelas mantêm relações claras entre colunas e células.

### 38.3. Foco proposto

Anel de 2 px, offset de 2 px e token focus-ring. Essa geometria é P. Validar a percepção do foco na superfície real e em componentes adjacentes.

### 38.4. Como declarar resultados

Documentar testes automáticos e manuais separadamente. Uma ferramenta automática sem erros não comprova conformidade completa. Informar o que foi testado, os contextos e as limitações restantes.

### 38.5. Verificação pontual dos pares propostos

Os pares abaixo foram calculados a partir dos valores sRGB deste documento. Servem como verificação da fundação; a interface completa ainda precisa de revisão contextual.

| Par | Contraste aproximado |
|---|---:|
| Texto branco / botão azul #2563EB | 5,17:1 |
| Texto branco / botão vermelho #DC2626 | 4,83:1 |
| Texto auxiliar #A1A1AA / superfície #27272A | 5,81:1 |
| Texto auxiliar #6B6B74 / superfície #F4F4F5 | 4,80:1 |
| Texto positivo #15803D / fundo #DCFCE7 | 4,57:1 |
| Texto negativo #B91C1C / fundo #FEE2E2 | 5,30:1 |
| Texto de atenção #854D0E / fundo #FEF3C7 | 6,15:1 |

Transparência, imagem de fundo, mistura de cores ou mudança de superfície exigem novo cálculo. Esses valores não validam foco, teclado, gráficos ou todos os estados de uma implementação.

---

## 39. Arquitetura de implementação

**Origem:** P. Adaptar à stack existente.

### 39.1. Responsabilidades

| Camada | Responsabilidade |
|---|---|
| Tokens | Valores e semântica visual |
| Primitivas | Botões, campos, selects, texto e ícones |
| Componentes compostos | Cards, filtros, tabelas e navegação |
| Padrões | Fluxos recorrentes e templates |
| Páginas | Conteúdo e coordenação do contexto |
| Dados | Consulta, transformação e contratos |
| Domínio | Regras fornecidas pelo produto |

### 39.2. Estrutura sugerida

~~~text
design-system/
  tokens/
  foundations/
  primitives/
  components/
  patterns/
  examples/
  documentation/
~~~

Essa estrutura é ilustrativa. Usar os diretórios existentes quando houver um padrão claro.

### 39.3. Reutilização

- Não duplicar o CSS de um card em cada página.
- Não manter duas versões do mesmo botão com cores quase iguais.
- Separar formatação de dados da aparência.
- Fazer componentes receberem conteúdo por contrato.
- Reutilizar primitivas acessíveis adotadas no projeto.
- Uma biblioteca de base não define a identidade final.
- A semelhança com convenções de Tailwind não prova uso de uma biblioteca específica de componentes.

### 39.4. CSS e layout

- Preferir tokens semânticos.
- Usar flex e grid conforme a função.
- Evitar posicionamento absoluto para estrutura principal.
- Evitar coordenadas copiadas de montagens.
- Não transformar uma página inteira em uma imagem.
- Usar unidades relativas onde favoreçam adaptação.
- Preservar as proporções essenciais em vez de fixar toda a interface em pixels.

### 39.5. Dados demonstrativos

Podem existir em exemplos e testes visuais. Devem ficar separados dos dados de produção. Não representar uma ação de backend como concluída apenas porque um toast foi exibido.

---

## 40. Documentação que a IA deve produzir

### 40.1. Fundação

- Escopo e identidade.
- Fontes e evidências.
- Tokens por camada.
- Temas.
- Tipografia.
- Espaçamento e geometria.
- Ícones.
- Acessibilidade.

### 40.2. Componentes

Uma página por componente ou agrupamento coerente, contendo o contrato da seção 11.

### 40.3. Padrões

- Estrutura de dashboard.
- Listas operacionais.
- Detalhes de entidade.
- Formulários.
- Filtros e pesquisa.
- Estados assíncronos.
- Navegação mobile.
- Visualização de dados.

### 40.4. Registro de decisões

| Campo | Conteúdo |
|---|---|
| ID | Identificador estável |
| Decisão | O que foi escolhido |
| Origem | C/O/A/E/P |
| Fonte | ID da imagem ou requisito |
| Motivo | Problema resolvido |
| Impacto | Componentes e temas afetados |
| Validação | Evidência ou teste |
| Pendência | O que falta confirmar |

### 40.5. Inventário

Listar componentes implementados, documentados, parcialmente cobertos e ainda ausentes. Não marcar como pronto apenas porque o componente aparece em uma tela de exemplo.

---

## 41. Fluxo de trabalho recomendado para a IA

### Etapa 1 — Compreender

Ler o documento, acessar as referências quando possível e inspecionar o contexto do produto. Produzir um inventário de evidências e lacunas.

### Etapa 2 — Fixar defaults

Adotar as decisões propostas ou registrar alternativas necessárias. Consolidar os tokens antes de montar páginas.

### Etapa 3 — Construir fundações

Fonte, temas, espaçamento, cores semânticas e estados de foco.

### Etapa 4 — Construir primitivas

Botões, campos, selects, badges e controles.

### Etapa 5 — Construir composições

Métricas, painéis, toolbar, tabela, navegação e estados assíncronos.

### Etapa 6 — Construir páginas de referência

Dashboard, lista e detalhe. Essas páginas devem demonstrar os mesmos componentes em contextos diferentes.

### Etapa 7 — Documentar

Especificar contratos, variantes, exemplos, decisões e origem dos valores.

### Etapa 8 — Validar

Revisar identidade, responsividade, estados, teclado, contraste e coerência de dados.

### Etapa 9 — Entregar

Fornecer resultado navegável quando fizer parte da tarefa, documentação, inventário, evidências de validação e pendências.

### Como lidar com lacunas

Não bloquear decisões rotineiras já cobertas por defaults. Pedir esclarecimento quando a ausência de uma definição alterar uma regra de negócio, uma ação importante ou o objetivo real da implementação. Enquanto isso, avançar na documentação e nos componentes independentes.

---

## 42. Matriz mínima de cobertura

| Família | Claro/escuro | Mobile/desktop | Teclado | Estados críticos |
|---|---|---|---|---|
| Botão | Ambos | Ambos | Foco e ativação | Disabled e loading |
| Campo | Ambos | Ambos | Edição e navegação | Erro e readonly |
| Select | Ambos | Ambos | Seleção | Vazio e disabled |
| Checkbox/radio/switch | Ambos | Ambos | Alternância | Seleção e disabled |
| Tabs | Ambos | Ambos | Navegação definida | Ativo e loading |
| Menu | Ambos | Ambos | Abrir, percorrer e fechar | Indisponível |
| Métrica | Ambos | Ambos | Conforme interatividade | Ausente, erro e stale |
| Tabela | Ambos | Ambos | Controles reais | Vazio, filtro e atualização |
| Gráfico | Ambos | Ambos | Alternativa de leitura | Ausência e extremos |
| Modal/drawer | Ambos | Ambos | Gestão de foco | Submissão e erro |
| Navegação | Ambos | Ambos | Destinos acessíveis | Item atual e permissão |
| Mensagem | Ambos | Ambos | Ação de recuperação | Erro e sucesso |

“Ambos” significa verificar de fato, não assumir que a troca de cores será suficiente.

---

## 43. Validação visual

### 43.1. Prioridade de revisão

1. Estrutura e hierarquia.
2. Relação entre menu e conteúdo.
3. Proporções da tipografia.
4. Anatomia e densidade dos componentes.
5. Espaçamento.
6. Superfícies e bordas.
7. Marca e cores de série.
8. Detalhes de ícone e acabamento.

### 43.2. Comparação com as imagens

- Comparar elementos de interface com elementos de interface.
- Separar apresentação fotográfica e componente real.
- Não usar a perspectiva de um notebook como alvo de layout CSS.
- Não exigir correspondência pixel a pixel com imagens em escalas desconhecidas.
- Registrar quando uma diferença vier de acessibilidade ou conteúdo real.
- Usar múltiplas telas para validar um padrão recorrente.

### 43.3. Conteúdo de estresse

Validar:

- Nome longo.
- Label em duas linhas.
- Valor monetário grande.
- Valor negativo.
- Zero real.
- Valor ausente.
- Texto traduzido mais longo.
- Uma linha de tabela.
- Muitas linhas.
- Nenhum resultado.
- Gráfico com um ponto.
- Gráfico com lacuna.
- Resultado parcialmente atualizado.

### 43.4. Viewports propostos para revisão

320, 360, 390, 768, 1024, 1280 e 1440 px de largura. Adicionar outras apenas quando houver uma razão do produto. A revisão não precisa repetir todas as combinações após uma alteração pequena que não as afete.

### 43.5. Evidência

Registrar capturas representativas de claro/escuro, desktop/mobile e estados relevantes. As capturas devem mostrar o contexto necessário para avaliar o resultado.

---

## 44. Critérios de aceitação

### 44.1. Fundação

- [ ] Inter com os quatro pesos declarados.
- [ ] Azul #2563EB preservado onde a marca original é mantida.
- [ ] Temas cobrem todas as superfícies.
- [ ] Tokens semânticos centralizados.
- [ ] Propostas identificadas como propostas.
- [ ] Geometria consistente.

### 44.2. Componentes

- [ ] Componentes equivalentes compartilham implementação ou contrato.
- [ ] Botões têm variantes e estados completos.
- [ ] Campos têm labels e erros contextualizados.
- [ ] Cards seguem anatomia estável.
- [ ] Tabelas têm filtros, ordenação e paginação coerentes quando anunciados.
- [ ] Gráficos têm unidade e período.
- [ ] Status são identificáveis sem depender apenas de cor.
- [ ] Navegação indica o destino atual.

### 44.3. Adaptação

- [ ] Não há overflow horizontal da página em telas estreitas.
- [ ] Conteúdo importante não fica atrás de barras fixas.
- [ ] Mobile preserva a tarefa principal.
- [ ] Valores e rótulos longos são tratados.
- [ ] Tabelas têm estratégia mobile documentada.
- [ ] Controles podem ser usados com toque.

### 44.4. Comportamento

- [ ] Ações declaradas funcionam ou têm limitação demonstrativa explícita.
- [ ] Loading não duplica submissão.
- [ ] Ausência não é mostrada como zero.
- [ ] Erro permite recuperação quando disponível.
- [ ] Filtros atualizam o conteúdo esperado.
- [ ] Seleção de período preserva o significado do indicador.
- [ ] Troca de tema cobre overlays e gráficos.

### 44.5. Acessibilidade

- [ ] Fluxos principais revisados por teclado.
- [ ] Foco visível e sem obstrução.
- [ ] Contraste verificado no uso real.
- [ ] Controles têm nomes apropriados.
- [ ] Leitura de tabelas e estados é compreensível.
- [ ] Modais e menus têm gestão de foco.
- [ ] Acessibilidade dos gráficos foi verificada.
- [ ] Limitações restantes estão registradas.

### 44.6. Documentação

- [ ] Fontes e evidências incluídas.
- [ ] Tokens e temas documentados.
- [ ] Contratos dos componentes registrados.
- [ ] Padrões de página registrados.
- [ ] Decisões P e E identificadas.
- [ ] Inventário corresponde ao que foi implementado.
- [ ] Pendências não são apresentadas como concluídas.

---

## 45. Antipadrões que prejudicam a fidelidade

- Transformar cada card em uma superfície colorida diferente.
- Acrescentar glassmorphism, blur ou neon sem uma necessidade do produto.
- Usar gradiente em todos os botões e painéis.
- Copiar sombras e molduras de dispositivos para a aplicação.
- Fazer todos os cantos parecerem cápsulas.
- Aumentar demais a escala do dashboard como se fosse um hero de landing page.
- Reduzir texto para acomodar um grid rígido.
- Criar vários azuis quase iguais fora dos tokens.
- Misturar famílias de ícones incompatíveis.
- Duplicar cards ou botões com pequenas diferenças arbitrárias.
- Fazer filtros, downloads e ações parecerem ativos sem implementação.
- Inventar números, totais ou status em produção.
- Dar cor de sucesso a um valor sem conhecer seu significado.
- Esconder informação essencial em tooltip.
- Declarar biblioteca original com base apenas na aparência.
- Afirmar que uma proposta deste arquivo foi publicada pela agência.

## 46. Apêndice A — Tokens CSS completos

Este bloco é a base canônica proposta para esta versão do documento. Ele amplia os exemplos simplificados do guia visual inicial. Uma IA pode copiá-lo para a camada de tokens e adaptar sua forma à stack.

Valores C preservados: Inter, pesos 400/500/600/700, #2563EB e #18181A. O uso funcional, as demais cores, os estados, o tema claro e a geometria completam a reconstrução.

~~~css
:root {
  /* Fonte: C. Fallbacks e escala: P/E. */
  --og-font-family: "Inter", Arial, sans-serif;
  --og-font-regular: 400;
  --og-font-medium: 500;
  --og-font-semibold: 600;
  --og-font-bold: 700;

  --og-type-caption: 12px;
  --og-type-body: 14px;
  --og-type-input-touch: 16px;
  --og-type-section: 18px;
  --og-type-metric: 24px;
  --og-type-title: 28px;
  --og-type-title-large: 32px;
  --og-leading-caption: 16px;
  --og-leading-body: 20px;
  --og-leading-input-touch: 24px;
  --og-leading-section: 24px;
  --og-leading-metric: 32px;
  --og-leading-title: 36px;
  --og-leading-title-large: 40px;

  /* Marca confirmada; estados propostos. */
  --og-brand: #2563eb;
  --og-brand-dark: #18181a;
  --og-brand-hover: #1d4ed8;
  --og-brand-active: #1e40af;
  --og-brand-foreground: #ffffff;

  /* Cores de ação: P. O texto semântico depende do tema. */
  --og-success: #16a34a;
  --og-danger: #dc2626;
  --og-danger-hover: #b91c1c;
  --og-warning: #ca8a04;

  /* Espaçamento: E/P. */
  --og-space-1: 4px;
  --og-space-2: 8px;
  --og-space-3: 12px;
  --og-space-4: 16px;
  --og-space-5: 20px;
  --og-space-6: 24px;
  --og-space-8: 32px;
  --og-space-10: 40px;
  --og-space-12: 48px;

  /* Geometria: E/P. */
  --og-radius-badge: 4px;
  --og-radius-control: 6px;
  --og-radius-card: 12px;
  --og-radius-overlay: 16px;
  --og-radius-avatar: 50%;
  --og-border-width: 1px;
  --og-focus-width: 2px;
  --og-focus-offset: 2px;
  --og-control-height: 36px;
  --og-touch-height: 44px;
  --og-sidebar-width: 256px;
  --og-sidebar-collapsed-width: 64px;
  --og-nav-item-height: 40px;
  --og-table-row-height: 48px;
  --og-icon-small: 16px;
  --og-icon-nav: 20px;
  --og-icon-mobile: 24px;
  --og-avatar-small: 32px;
  --og-avatar-header: 40px;
  --og-page-padding: 24px;
  --og-page-padding-mobile: 16px;
  --og-card-padding: 16px;
  --og-card-gap: 16px;

  /* Movimento e sobreposição: P. */
  --og-motion-fast: 120ms;
  --og-motion-normal: 180ms;
  --og-motion-overlay: 240ms;
  --og-ease: cubic-bezier(0.2, 0, 0, 1);
  --og-shadow-popover: 0 8px 24px rgb(0 0 0 / 18%);
  --og-shadow-modal: 0 20px 60px rgb(0 0 0 / 24%);
  --og-z-base: 0;
  --og-z-sticky: 10;
  --og-z-dropdown: 20;
  --og-z-overlay: 30;
  --og-z-modal: 40;
  --og-z-toast: 50;
  --og-z-tooltip: 60;
}

[data-og-theme="dark"] {
  color-scheme: dark;
  --og-background: #09090b;
  --og-surface: #09090b;
  --og-sidebar: #18181a;
  --og-surface-raised: #27272a;
  --og-surface-hover: #18181a;
  --og-border: #27272a;
  --og-border-strong: #71717a;
  --og-text: #fafafa;
  --og-text-muted: #a1a1aa;
  --og-link: #60a5fa;
  --og-focus-ring: #60a5fa;
  --og-selection: #1e3a8a;
  --og-selection-text: #eff6ff;
  --og-positive-text: #4ade80;
  --og-success-bg: #052e16;
  --og-negative-text: #f87171;
  --og-danger-bg: #450a0a;
  --og-warning-text: #facc15;
  --og-warning-bg: #422006;
  --og-neutral-badge-bg: #27272a;
  --og-overlay: rgb(0 0 0 / 60%);
  --og-chart-blue: #2563eb;
  --og-chart-green: #16a34a;
  --og-chart-orange: #ea580c;
  --og-chart-yellow: #eab308;
  --og-chart-pink: #db2777;
  --og-chart-purple: #c026d3;
  --og-chart-neutral: #71717a;
}

[data-og-theme="light"] {
  color-scheme: light;
  --og-background: #ffffff;
  --og-surface: #ffffff;
  --og-sidebar: #f4f4f5;
  --og-surface-raised: #f4f4f5;
  --og-surface-hover: #f4f4f5;
  --og-border: #e4e4e7;
  --og-border-strong: #71717a;
  --og-text: #18181b;
  --og-text-muted: #6b6b74;
  --og-link: #1d4ed8;
  --og-focus-ring: #2563eb;
  --og-selection: #dbeafe;
  --og-selection-text: #1e40af;
  --og-positive-text: #15803d;
  --og-success-bg: #dcfce7;
  --og-negative-text: #b91c1c;
  --og-danger-bg: #fee2e2;
  --og-warning-text: #854d0e;
  --og-warning-bg: #fef3c7;
  --og-neutral-badge-bg: #f4f4f5;
  --og-overlay: rgb(0 0 0 / 40%);
  --og-chart-blue: #2563eb;
  --og-chart-green: #15803d;
  --og-chart-orange: #c2410c;
  --og-chart-yellow: #a16207;
  --og-chart-pink: #be185d;
  --og-chart-purple: #a21caf;
  --og-chart-neutral: #52525b;
}
~~~

### 46.1. Observações de aplicação

- Aplicar data-og-theme no elemento raiz apropriado.
- Não deixar o tema implícito sem definir os tokens semânticos.
- Resolver a preferência “sistema” para claro ou escuro antes de aplicar os tokens.
- Os valores de breakpoint pertencem ao contrato de layout; custom properties não substituem diretamente limites de media query em CSS comum.
- Em bibliotecas de gráficos que recebem valores concretos, resolver os tokens do tema e atualizar o gráfico após a troca.
- Os níveis de z-index são P e devem respeitar a arquitetura existente.
- O uso de um portal deve preservar os tokens do tema.
- CSS anterior simplificado pode ser mantido para referência; este bloco define os defaults completos de v1.0.

---

## 47. Apêndice B — Exemplo de aplicação visual

Os trechos abaixo demonstram anatomia e tokens. Não incluem consulta de dados, autenticação ou handlers completos. Essas partes devem ser implementadas conforme a tarefa.

### 47.1. Card de métrica

~~~html
<article class="og-metric" aria-labelledby="metric-balance-title">
  <h3 id="metric-balance-title" class="og-metric__label">
    Saldo atual
  </h3>
  <p class="og-metric__value">US$ 12.701,19</p>
  <p class="og-metric__context">
    <span class="og-metric__delta">+10%</span>
    em relação ao mês anterior
  </p>
</article>
~~~

~~~css
.og-metric {
  min-width: 0;
  margin: 0;
  padding: var(--og-card-padding);
  color: var(--og-text);
  background: var(--og-surface);
  border: var(--og-border-width) solid var(--og-border);
  border-radius: var(--og-radius-card);
}

.og-metric__label {
  margin: 0;
  font-family: var(--og-font-family);
  font-size: var(--og-type-caption);
  line-height: var(--og-leading-caption);
  font-weight: var(--og-font-medium);
  color: var(--og-text-muted);
}

.og-metric__value {
  margin: var(--og-space-3) 0 var(--og-space-2);
  font-family: var(--og-font-family);
  font-size: var(--og-type-metric);
  line-height: var(--og-leading-metric);
  font-weight: var(--og-font-semibold);
  font-variant-numeric: tabular-nums;
  overflow-wrap: anywhere;
}

.og-metric__context {
  margin: 0;
  font-size: var(--og-type-caption);
  line-height: var(--og-leading-caption);
  color: var(--og-text-muted);
}

.og-metric__delta {
  color: var(--og-positive-text);
  font-weight: var(--og-font-medium);
}
~~~

Quebra de valor numérico é uma proteção de último recurso. A implementação deve preferir largura, formatação ou rearranjo que mantenha o número como unidade.

### 47.2. Botão primário

~~~css
.og-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--og-space-2);
  min-height: var(--og-control-height);
  padding: var(--og-space-1) var(--og-space-3);
  border: var(--og-border-width) solid var(--og-border-strong);
  border-radius: var(--og-radius-control);
  font-family: var(--og-font-family);
  font-size: var(--og-type-body);
  line-height: var(--og-leading-body);
  font-weight: var(--og-font-medium);
  background: var(--og-surface);
  color: var(--og-text);
  cursor: pointer;
  transition:
    background-color var(--og-motion-fast) var(--og-ease),
    border-color var(--og-motion-fast) var(--og-ease);
}

.og-button--primary {
  background: var(--og-brand);
  border-color: var(--og-brand);
  color: var(--og-brand-foreground);
}

.og-button--primary:hover:not(:disabled) {
  background: var(--og-brand-hover);
  border-color: var(--og-brand-hover);
}

.og-button--primary:active:not(:disabled) {
  background: var(--og-brand-active);
  border-color: var(--og-brand-active);
}

.og-button:focus-visible {
  outline: var(--og-focus-width) solid var(--og-focus-ring);
  outline-offset: var(--og-focus-offset);
}

.og-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

@media (pointer: coarse) {
  .og-button {
    min-height: var(--og-touch-height);
  }
}

@media (prefers-reduced-motion: reduce) {
  .og-button {
    transition: none;
  }
}
~~~

Esse exemplo cobre apenas uma variante e parte dos estados. O componente entregue deve incluir loading, outros tipos de botão e integração funcional.

### 47.3. Grade de métricas

~~~css
.og-metrics-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--og-card-gap);
}

@media (max-width: 1023px) {
  .og-metrics-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 359px) {
  .og-metrics-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
~~~

Os limites são P. Ajustar quando o conteúdo demonstrar a necessidade.

---

## 48. Apêndice C — Modelo de documentação de componente

Use este modelo para cada componente relevante:

~~~markdown
# Nome do componente

## Propósito

Qual tarefa resolve e em que contextos é usado.

## Evidência

Código de origem: C/O/A/E/P.
Referências: IDs da fonte e das imagens.
O que é observado e o que foi completado.

## Anatomia

Partes visuais e ordem.

## Tokens

Lista de tokens semânticos e de componente.

## Variantes

Nome, função e diferenças.

## Estados

Default, hover, foco, disabled, loading, erro, vazio e seleção,
conforme a natureza do componente.

## Contrato

Conteúdo, dados, eventos, limites e dependências.

## Responsividade

Mudanças por espaço disponível e conteúdo.

## Acessibilidade

Semântica, nome, teclado, foco e leitura de estados.

## Exemplos

Uso normal, caso extremo e combinação com outros componentes.

## Evitar

Erros que prejudicam função ou consistência.

## Validação

O que foi revisado e a evidência.

## Pendências

Questões ainda não resolvidas.
~~~

---

## 49. Apêndice D — Registro de evidência em JSON

Este formato é uma proposta para permitir rastreabilidade automática:

~~~json
{
  "id": "geometry.card.radius",
  "value": "12px",
  "origin": "E",
  "normativeForReconstruction": true,
  "sources": ["I03", "I04", "I06"],
  "note": "Estimativa normalizada; não é uma medida extraída do Figma.",
  "validation": {
    "status": "pending",
    "method": "Revisão visual em componentes de referência"
  }
}
~~~

Exemplo de decisão complementar:

~~~json
{
  "id": "color.light.chart.yellow",
  "value": "#A16207",
  "origin": "P",
  "normativeForReconstruction": true,
  "sources": ["I04"],
  "note": "Variante proposta para discernibilidade no tema claro.",
  "validation": {
    "status": "pending",
    "method": "Verificar contraste e leitura no gráfico real"
  }
}
~~~

Pending significa pendente. A IA deve atualizar esse campo com uma evidência real; não converter um exemplo em uma validação concluída.

---

## 50. Apêndice E — Prompt pronto para a IA receptora

Copie a instrução abaixo junto com este documento, ou use-a como abertura da tarefa:

> Leia integralmente o arquivo ONLYGENIUS-DESIGN-SYSTEM-IA.md e use-o como especificação para documentar e seguir o sistema visual reconstruído do projeto OnlyGenius.
>
> Primeiro, identifique o contexto do produto e a stack disponível. Organize as evidências pelos códigos C, O, A, E, P e ND. Preserve os elementos confirmados e adote os defaults propostos quando não existir requisito conflitante.
>
> Produza a documentação das fundações, tokens, temas, componentes e padrões de página. Para cada componente, registre anatomia, variantes, estados, contratos de dados, comportamento, responsividade, acessibilidade, exemplos e critérios de aceitação.
>
> Quando a tarefa incluir implementação, construa tokens e componentes compartilhados antes das páginas. Demonstre dashboard, lista e detalhe com a mesma linguagem visual. Faça as interações declaradas funcionarem e identifique qualquer conteúdo demonstrativo.
>
> Verifique claro e escuro, desktop e mobile, valores longos, dados ausentes, loading, erro, teclado e contraste. Registre os testes e suas limitações. A aparência de uma imagem não comprova uma biblioteca ou um estado de interação.
>
> Use nomes, identidade e regras de negócio do produto de destino. Solicite informação somente quando uma lacuna alterar o objetivo, uma regra de negócio ou uma ação importante; continue o trabalho independente com os defaults documentados.
>
> Ao concluir, entregue os arquivos solicitados, o inventário do que foi produzido, a origem das decisões, as evidências de validação e as pendências. Identifique o trabalho como uma reconstrução independente baseada no projeto creditado à ZeeFrames.

### 50.1. Se a tarefa for apenas documentar

Entregar documentação organizada e exemplos suficientes para implementação posterior. Não declarar que uma interação foi implementada apenas porque seu comportamento foi descrito.

### 50.2. Se a tarefa incluir implementar

Entregar a interface funcional, as fundações reutilizáveis e a documentação sincronizada. Informar integrações efetivas, simulações e dependências restantes.

### 50.3. Se a tarefa for revisar uma interface existente

Comparar o resultado com os critérios deste documento. Separar divergências reais, adaptações justificadas e informações que não puderam ser avaliadas.

---

## 51. Modelo de relatório de entrega

A IA deve adaptar este formato à tarefa:

1. **Resultado:** o que foi documentado ou implementado.
2. **Escopo:** páginas e componentes cobertos.
3. **Fundação:** tokens, fontes, temas e mudanças.
4. **Evidência:** fontes acessadas e limitações de acesso.
5. **Decisões:** propostas adotadas e adaptações.
6. **Comportamento:** interações e integrações efetivas.
7. **Validação:** verificações visuais, funcionais e de acessibilidade executadas.
8. **Arquivos:** localização dos entregáveis.
9. **Pendências:** o que permanece sem implementação ou confirmação.

Evitar relatórios que apenas afirmem “fiel”, “responsivo” ou “acessível” sem explicar como isso foi avaliado.

---

## 52. Governança e evolução

### 52.1. Fonte única de decisão

Manter tokens e contratos em um local central. As páginas consomem essas definições.

### 52.2. Nova variante

Antes de criar uma variante, verificar se:

- O problema é recorrente.
- A variante tem função distinta.
- Um token existente resolve a situação.
- A mudança continua coerente nos dois temas.
- O comportamento está definido.

Uma correção local não deve virar automaticamente uma nova família de componentes.

### 52.3. Mudança de token

Registrar valor anterior, valor novo, motivo, origem e componentes afetados. Revisar os usos críticos após a mudança.

### 52.4. Nova evidência

Se surgirem arquivos originais, screenshots mais claras ou contratos do produto:

1. Atualizar o registro de evidência.
2. Distinguir confirmação de correção.
3. Reavaliar estimativas afetadas.
4. Sincronizar documentação e implementação.
5. Explicar diferenças relevantes.

### 52.5. Versionamento proposto

- Patch: correção documental ou ajuste sem mudar contrato.
- Minor: nova variante ou componente compatível.
- Major: alteração que exige adaptação de consumidores.

Essa convenção é P. Seguir o esquema do projeto quando já existir.

---

## 53. Glossário

| Termo | Significado neste documento |
|---|---|
| Token | Valor centralizado com função identificada |
| Primitivo | Base reutilizável, como cor ou espaço |
| Semântico | Nome orientado ao papel no produto |
| Anatomia | Partes e ordem de um componente |
| Variante | Forma de um componente para uma função distinta |
| Estado | Condição atual de uso ou de dados |
| Densidade | Relação entre conteúdo, espaço e dimensão |
| Superfície | Fundo de uma região visual |
| Elevação | Indicação de sobreposição |
| Shell | Estrutura global da aplicação |
| Toolbar | Grupo de filtros e ações do contexto |
| Badge | Rótulo compacto de estado |
| Chip | Representação compacta de filtro ou seleção |
| Skeleton | Placeholder estrutural de carregamento |
| Stale | Dado disponível, mas potencialmente desatualizado |
| Empty state | Situação sem conteúdo aplicável |
| Breakpoint | Limite proposto para adaptação de layout |
| Safe area | Área reservada pela interface do dispositivo |
| Foco | Destino atual da interação por teclado |
| Contrato | Dados, comportamento e limites esperados |
| Handoff | Documentação para continuidade do trabalho |
| Fonte confirmada | Informação declarada pela referência |
| Reconstrução | Sistema deduzido e completado a partir da evidência |

---

## 54. Registro desta versão

| Área | Situação |
|---|---|
| Fonte e pesos | Confirmados no guia |
| Duas cores de marca | Confirmadas no guia |
| Componentes recorrentes | Observados em cinco imagens de telas e layouts |
| Fundo e cores adicionais | Amostrados no PNG |
| Geometria | Estimada e normalizada |
| Tema claro completo | Proposto a partir da existência observada do tema |
| Hover, foco, erros e loading | Propostos |
| Breakpoints e tabelas mobile | Propostos |
| Bibliotecas originais | Não determinadas |
| Figma e tokens originais | Não disponibilizados nesta análise |
| Acessibilidade original | Não determinada |
| Conformidade da implementação futura | Depende de validação efetiva |

**Instrução final para a IA receptora:** preserve o que é confirmado, reproduza os padrões observados, trate amostras e estimativas com sua origem declarada e complete o sistema com decisões documentadas. Mantenha a implementação e sua documentação consistentes.
