# Decisões e pendências do WMS Rodogarcia

## FE02-INFO01 — catálogo único de ajuda, 10/10/2026

**Pedido expresso de Lucas:** popups em indicadores/cartões com explicações centralizadas em um único `.ts`, revisado quando a função correspondente mudar. Catálogo em `frontend/src/content/information.ts`, manutenção registrada em AGENTS.md. O complemento de Lucas pede o “i” menor, mais visível e melhor posicionado; definição e consumidores em [etapa 05](design-system/etapa-05.md#popups-informativos--fe02-info01). Explicações usam dados já autorizados, sem novas consultas ou cálculos de negócio por hover.

## BE02-DEV-REINICIO01 — reinício exclusivo do DEV, 10/10/2026

**Decisão expressa de Lucas:** cada execução de `iniciar-dev.bat` deve substituir a instância DEV deste WMS, preservando outros projetos, portas e produção. Implementado para 25580/25581 com prova de origem por recibo do próprio projeto, PID/início, executável/comando completo e perfil DEV. Um processo também escutando outra porta não é encerrado. Identidade incerta interrompe a subida; não se usa encerramento indiscriminado por porta ou nome de executável.

O launcher confere pré-requisitos e guarda atual antes de encerrar a instância anterior, revalida os dois donos antes do primeiro encerramento e serializa a inicialização concorrente. Registra PID/porta/execução anterior no recibo novo. A autorização altera somente a restrição histórica de reinício deste DEV; banco, TLS, produção e serviços compartilhados mantêm seus limites. [Procedimento](19-desenvolvimento-integrado-dev.md#reinicio-dev-exclusivo). Validação isolada e pendência real registradas no [estado](../STATES.md#be02-dev-reinicio01--reinício-exclusivo-do-dev-10102026).

## QUAL-CONF01 — qualidade e confiabilidade, 10/10/2026

Nova demanda expressa de Lucas após reset: ler a documentação vigente, executar primeiro as suítes atuais em saídas isoladas, inventariar áreas/riscos de forma compacta e corrigir erros demonstrados por testes ou prova concreta. Cada correção exige reprodução, causa, menor ajuste, verde/regressão/build da fonte final e revisão independente. Propostas AC permanecem identificadas; não inventar parâmetros ou transformar baseline verde em ausência universal de erros.

Posses exclusivas: Cedro backend/regras/transações/segurança/testes; Lume frontend/contratos/fluxos/erros/acessibilidade/browser; Prumo scripts/database/infra/pareamento JPA-DDL em arquivo e testes isolados; Vigia revisão em leitura; Farol canônicos/consolidação. Preservar toda alteração preexistente e paralela; sem reset/revert/stash/commit/push. Locais, H2 efêmero isolado e browser interceptado autorizados. Integração real pertinente somente WMS_DEV/WMSDEV após prova atual do runtime/alvo/TLS/permissões/catálogo/histórico e sessão legítima; não iniciar servidores reais por inferência. Sem SQLfixture/DELETE/reset/DDL/migrations/grants/PROD/sa na API/servidor/runtime compartilhado/kill ou restart existente/fiscal ou cobrança externa/dispositivos reais/ETL/publicação/rotinas. POP e a autorização histórica de população SQL não são retomados.

**Steering de Lucas:** priorizar diferença entre site aberto e fonte. Inspeção atual somente de leitura, sem SQL/novo login/senha/processo real novo. Uma recarga do portal WMS é autorizada se não houver edição/operação pendente, preservando a sessão sem inspecionar tokens/cookies; se não for seguro, orientar Ctrl+Shift+R sem alegar execução. Reinício de processos do usuário exige devolver o limite e o comando documentado antes de agir. [Andamento oficial](../STATES.md#qual-conf01--qualidade-e-confiabilidade-10102026); provas em `orchestracao/.runtime/qual-conf01/`.

**Conferência expressa durante a rodada:** preservar os testes históricos não substitui provar os comportamentos vigentes do runner D22 atual. Exercitar em cópias isoladas, com stubs/dados fictícios e sem alterar executores/migrations/históricos: retomada/reexecução/corrida Create→Check, falha PROD preservando DEV, gates PreValidate/Migrate/PostValidate/Info/catálogo e sanitização de checksum/transporte. Caso não executável exige barreira concreta; não declarar validação integral. Intermitência em File.Replace permanece pendência material até causa/teste determinístico ou justificativa explícita; uma rodada verde não apaga o vermelho.

### QUAL-CONF01-PERF01 — Inicio desempenho e seguranca

Lucas ampliou expressamente QUAL-CONF01 para medir e corrigir a demora do Início e os riscos de segurança, com dependência dashboard/visão/contexto: BE14-DASH01/BE14-DASH02 → FE02-DASH01/FE02-DASH02 e FE02-CTX01. O bloco anterior permanece baseline local parcial datada; não encerrar a rodada sem tratar o complemento. Lume pode fazer navegação/Atualizar normal de leitura pertinente na sessão/origem legítima existente, após confronto atual de runtime/alvo/TLS/permissões/catálogo/histórico e ausência de edição/operação pendente. Capturar apenas status, tempos, tamanhos, contagens e sequência sanitizados; nunca tokens/cookies/Authorization/senhas/payload sensível. Sem bypass CSP, nova conexão inferida ou benchmark contra o real.

Cedro deve medir cardinalidade/statements/consultas com massa H2 isolada e oráculo independente, separando banco/API/rede/CPU/render e preservando contrato, BigDecimal, origem, avaria, null e alcance. Revalidação de sessão/permissão permanece obrigatória; repetição de método não demonstra repetição SQL. Corrigir a menor origem factual, sem DDL/índices/migrations/cache global/mascarar espera/reduzir segurança. Pacote preparado não comprova runtime atualizado; se depender de reinício/perfil/índice, devolver o passo seguro documentado antes de atuar. Antes/depois devem identificar fonte/ambiente e deltas paralelos, com regressão pertinente da fonte final e revisão Vigia. Ausência de instrumento real exige medida não obtida e próximo instrumento/responsável explícitos.

P01 recebeu diagnóstico mínimo somente em arquivos fictícios isolados: helper real e .NET direto, metadados/ACL/estado/HResult sanitizados quando disponíveis, sem retries ou intervenção no canal real. Não reprodução não é correção. Se persistir aberto, QUAL-CONF01 fecha apenas parcialmente, com próximo caso exato e responsável. [Andamento](../STATES.md#qual-conf01-perf01--início-desempenho-e-segurança); [provas do complemento](../orchestracao/.runtime/qual-conf01/perf01/).

**Preparo utilizável, steering expresso:** após verify final e revisão Vigia, executar o preparo local documentado `infra/dev/preparar-backend-dev.ps1`, com H2/guarda de fonte/Assertartifact e backups antes de atualizar os dois recibos WMS próprios. Se houver seleção menor já documentada de candidato compatível, comprová-la; não inventar seleção manual de JAR. Conferir saídas isoladas e preservar evidências preexistentes. Fonte corrigida, pacote pronto e backend atual `43BD…` são estados distintos; não orientar reinício do BAT com recibos antigos esperando fonte nova. Autoriza esse preparo local, sem abrir SQL, iniciar/reiniciar site, alterar sharedruntime/segredos ou publicar.

### QUAL-CONF01-PERF01-DEC01 — processamento adequado no banco, 10/10/2026

**Decisão expressa de Lucas, vigente; não é proposta.** A arquitetura por camadas não impede deslocar processamento de dados Java → SQL quando a adequação e o benefício forem demonstrados. Preferir filtragem, joins, agregação, projeção, paginação e consultas em lote executados no banco, com DTOs enxutos, quando devolverem o resultado exato sem hidratação/processamento integral desnecessários em Java. Não presumir ganho infinito: comparar resultado funcional por oráculo independente e custo por carga, consultas e memória quando mensurável, identificando fonte e ambiente.

Serviços continuam garantindo permissões, alcance e coordenação das regras/transações; predicados SQL podem executar processamento das regras aprovadas. Preservar precisão BigDecimal, null/indisponível, origem, avaria e histórico. Não generalizar a conversão de tudo, criar triggers/procedures ou reescrever a arquitetura sem necessidade. A autorização cobre engenharia, código e testes de deslocamento adequado no escopo atual; não libera DDL, índices, migrations, grants, servidor, PROD, sa na API, reset/dados reais ou reinício de processos existentes. Se schema/procedure/índice for indispensável, apresentar proposta concreta com evidência e limite antes de aplicação real.

**Impacto em PERF01:** Cedro e Vigia reavaliam o custo residual de valoração por unidade/históricos já medido. Consulta em lote/SQL equivalente, com ganho demonstrado e oráculo independente, permite correção mínima na mesma rodada. Sem ganho adicional demonstrado, declarar o residual e a prova necessária, sem alterar por inferência nem bloquear o preparo oficial por otimização especulativa. Fonte otimizada, pacote pronto e runtime antigo permanecem estados distintos. [Trilha e evidências](../STATES.md#qual-conf01-perf01--início-desempenho-e-segurança).

### QUAL-CONF01-PERF01-CARGA01 — implementação do carregamento, 10/10/2026

**Nova autorização expressa de Lucas: implementar a otimização forte do carregamento ponta a ponta**, dentro de QUAL-CONF01/PERF01; não apenas estudar ou repetir a reavaliação DEC01. Preservar correções INIT01, visual/labels/Information e todo trabalho paralelo. Baseline focal por endpoint, consultas, cardinalidade, payload e custo frontend; separar banco, API, rede/transferência e render quando mensuráveis, sem atribuir ao SQL Server os números H2.

Cedro deve reduzir o residual demonstrado de valoração por unidades/históricos, usando consultas em lote/projeções/SQL adequado sob alcance autorizado e limites por lote. Se a fórmula exigir coordenação Java, reduzir fetching em conjunto e entregar ganho concreto, preservando BigDecimal, null/incompleto, origem, avaria, história, transações e oráculos temporais independentes na mesma massa 1/8/40 e uma massa maior controlada. Lume deve corrigir waterfall/fetch repetido demonstrados na primeira carga/contexto/listas/mapa/gráficos, paralelizar blocos independentes quando apropriado e apresentar resultados progressivos. Dedupe, se necessário e comprovado, fica limitado à sessão/alcance com invalidação após escrita; sem cache global, TTL inventado, dados antigos, fallback mock REAL ou espera mascarada.

Exigir reprodução e menor correção, antes/depois calculado, tempos locais representativos repetidos, sucesso/vazio/erro/troca de contexto e alcance/escrita/invalidação/navegação rápida/concorrência INIT01 preservados, regressão pertinente, build datado e Vigia. Não perseguir indefinidamente deltas fora do recorte. Fonte, artefato e runtime são resultados distintos. Runtime de referência atual: prova Prumo 20:46, BE 61588/JAR a8 `2C1A…`, FE 59336, SQL 48480; 43BD e PIDs antigos são históricos.

Posse exclusiva: Cedro BE; Lume FE funcional de carregamento; Prumo metadados/proposta; Vigia revisão; Farol canônicos/consolidação. Provas em `orchestracao/.runtime/qual-conf01/perf01/carga01/`. Sem POP, escrita de negócio real, SQL direto/DDL/índices/migrations/grants/sa na API/PROD/alteração de servidor, preparador adicional, restart/kill/processo real novo, aba/portal/login/credenciais, instalações ou ETL. Schema/índice indispensável exige proposta concreta antes de aplicação real. Leituras normais apenas na mesma sessão/superfície legítima utilizável e escopo atestado; Network indisponível permanece limite, sem bypass ou sondas repetidas. [Estado e dependências](../STATES.md#qual-conf01-perf01-carga01--implementação-do-carregamento-10102026).

### QUAL-CONF01-PERF01-INIT01 — independência de Início, 10/10/2026

**Nova verificação expressa de Lucas dentro da mesma rodada:** investigar o relato de que as outras telas só carregam depois de abrir Início. Dependências: autenticação/identidade e FE02-CTX01 → workspace, catálogos e telas operacionais; dashboard/visão não deve ser confundido com o contexto cliente/armazém obrigatório. Inspeção do provider externo sozinha não demonstra independência.

Lume deve executar testes locais/interceptados de entrada operacional sem visitar Início, rota direta/hash/reload conforme capacidade e contrato vigentes, navegação rápida, dashboard pendente/500/sucesso, catálogos pendentes/erro/refresh, auth.fresh concorrente, cancelamentos e memoização. Não inventar suporte de rota nem liberar fallback mock no modo REAL. Cedro deve exercitar endpoints de identidade/catálogo/operacional autenticados antes de qualquer dashboard, com negativos de sessão/alcance preservados. Se houver defeito: causa reproduzida, menor patch, verde/regressão/build pertinentes e revisão Vigia, preservando visual/regras/contratos e alterações paralelas.

Provas em `orchestracao/.runtime/qual-conf01/perf01/inicio-independencia/`, posses exclusivas Lume/Cedro/Vigia e canônicos Farol. Não iniciar preparador adicional, restart/kill, SQL direto, PROD/POP, login/portal ou processo real novo. Preservar o preparo já em curso. Fonte/pacote/runtime distintos; fechar a verificação por execuções ou limite concreto, não apenas encaminhamento. [Andamento](../STATES.md#qual-conf01-perf01-init01--independência-de-início-10102026).

## FE02-REG01-A08 — ações e altura das tabelas, 10/10/2026

Lucas apontou tabelas fiscais/de encerramento com rótulos partidos e linhas muito altas, pedindo corrigir os equivalentes. O ramo de operação contextual em `RecordTable` passa a usar botão compacto como detalhes/edição, com nome completo no hover e para leitores de tela. Ações ficam lado a lado, preservando destino, permissão e confirmação. [Definição e evidências](design-system/etapa-06.md#operações-compactas-nas-tabelas--fe02-reg01-a08). Sem mudança em regras ou backend.

## FE02-REG01-A07 — botões junto aos campos de filtro, 10/10/2026

Lucas pediu Aplicar/Restaurar alinhados às caixas de seleção e a mesma organização nas páginas/etapas equivalentes. Alterado o componente compartilhado: comandos ao lado dos campos quando cabem, com quebra conjunta conforme a largura disponível. [Definição e validação](design-system/etapa-06.md#comandos-ao-lado-dos-filtros--fe02-reg01-a07). Sem alteração de consulta, restauração, contratos ou regras backend.

## FE02-DASH02-A08 — encaixe e informações do mapa, 10/10/2026

Lucas pediu ruas lado a lado com dimensões próprias, divisões visíveis, popup por célula e melhor aproveitamento das lacunas em torno de ruas maiores. Implementado encaixe denso adaptável, contornos entre armazéns/ruas e prévia por mouse/teclado com os dados já autorizados da consulta. Detalhes completos permanecem no clique. [Definição e evidências](design-system/etapa-05.md#encaixe-das-ruas-divisões-e-prévia--fe02-dash02-a08). Sem alteração de regras/backend ou autorização adicional de ambiente.

## FE02-DS01-A04 — setas e blur no menu lateral, 10/10/2026

Lucas pediu usar setas e blur quando o menu lateral não couber em monitor pequeno. Autoriza ocultar a barra de rolagem e introduzir controles de anterior/próximo com desfoque nas bordas, mantendo destinos, roda/toque e teclado. [Definição](design-system/etapa-01.md#setas-e-desfoque-na-lateral--fe02-ds01-a04).

## FE02-DASH02-A07 — ícones de bloqueio e reserva, 10/10/2026

Lucas pediu corrigir os ícones da legenda do mapa. A apresentação usa cadeado para bloqueio e marcador para reserva, em SVG, com alinhamento central e o mesmo símbolo nas posições. Estados e rótulos mantidos. [Definição](design-system/etapa-05.md#ícones-do-mapa--fe02-dash02-a07).

## FE02-REG01-A06 — disposição dos botões de auditoria, 10/10/2026

Lucas pediu organizar os botões Auditar Cliente/Serviço que estavam empilhados sem intervalo. O componente compartilhado passa a alinhar as ações com espaçamento e quebra conforme a largura. Aviso de vínculo/legado e seleção de cada namespace permanecem. [Definição](design-system/etapa-06.md#botões-da-auditoria--fe02-reg01-a06).

## FE05-PED01-A01 — botões da consulta de pedidos, 10/10/2026

Lucas pediu reorganizar a posição dos botões de Pedidos de entrada. Aplicar/Restaurar passam a compartilhar a faixa com Atualizar lista, mantendo Novo pedido no cabeçalho e disposição adaptada ao celular. Mudança de apresentação sem alterar consultas ou regras. [Definição](design-system/etapa-07.md#botões-da-consulta--fe05-ped01-a01).

## FE02-REG01-A05 — compactação dos diálogos e rastreamento de padrões, 10/10/2026

Lucas enviou detalhes de serviço e edição de cliente, pediu reduzir espaços/divisões vazias e acordeões dispensáveis e rastrear o padrão no projeto. Autoriza corrigir os componentes compartilhados de apresentação e registrar tudo o que mudou. A edição mostra identificação fixa compacta e campos editáveis; o detalhe completo continua acessível. Referências úteis, confirmações, dados, permissões e proteção de alterações pendentes permanecem. [Inventário e definição](design-system/etapa-06.md#diálogos-e-formulários-compactos--fe02-reg01-a05).

## FE02-REG01-A04 — tabela sem rolagem lateral no desktop, 10/10/2026

Lucas pediu melhorar a tabela de Cadastros que rolava lateralmente em monitor de 1500 px. Autoriza distribuir a largura entre colunas, quebrar códigos/documentos extensos e compactar detalhes/edição em ícones identificados, na tabela compartilhada. Dados completos, comandos, permissões e acesso por teclado permanecem. Em telas estreitas, a rolagem fica restrita à tabela. [Definição e conferência](design-system/etapa-06.md#tabelas-ajustadas-à-largura--fe02-reg01-a04).

## FE02-DASH02-A06 — mapa compacto conforme imagem, 10/10/2026

Lucas forneceu uma nova referência e pediu melhor organização do mapa do Início. A apresentação passa a usar códigos em células pequenas preenchidas de verde/vermelho, ruas com título acima da grade, legenda junto da seleção e filtros/informações secundárias recolhidos. Setas no cabeçalho de cada rua preservam o mesmo alinhamento entre grades. A06 substitui a apresentação dos cartões de A04; fundos alternados de A05, estados, consultas e detalhes continuam. [Definição e conferência](design-system/etapa-05.md#mapa-do-início-conforme-referência--fe02-dash02-a06).

## FE02-DASH02-A05 — fundos alternados no empilhamento, 10/10/2026

Lucas pediu separar melhor cada grupo empilhado com fundo alternado. Aplicado ao bloco completo de cada armazém, com superfície normal e cinza suave alternados, conforme o tema. [Definição e conferência](design-system/etapa-05.md#armazéns-com-fundos-alternados--fe02-dash02-a05).

## FE02-DASH02-A04 — mapa compacto com setas, 10/10/2026

Lucas pediu simplificar ruas/cartões, reduzir a altura e corrigir o alinhamento. Ao conferir muitas posições na rua, orientou usar setas para mostrar mais/menos, substituindo a barra horizontal. Autoriza a apresentação compartilhada com grupos de colunas ajustados à largura, preservando níveis, lacunas, estados e acesso aos detalhes. [Definição e conferência](design-system/etapa-05.md#mapa-compacto-e-navegação-por-setas--fe02-dash02-a04).

## FE02-DS03-A06 — atalhos compactos com borda, 10/10/2026

Lucas pediu alinhar atalhos, Início e administração, reduzir a altura, padronizar o botão administrativo e aumentar a separação dos indicadores. Após conferir a composição, pediu simplificar os botões e manter uma borda. A apresentação neutra substitui a orientação de fundos marcantes de A05; destinos e permissões permanecem iguais. [Definição atual](design-system/etapa-03.md#atalhos-compactos-e-neutros--fe02-ds03-a06).

## FE02-CTX01-A02 — contexto no cabeçalho a partir de 1200 px, 10/10/2026

Lucas pediu manter os seletores de cliente/armazém e Aplicar contexto na linha principal até 1200 px. Autoriza ajustar o limite responsivo e o espaçamento do cabeçalho compartilhado. [Composição e conferência](design-system/etapa-03.md#contexto-na-linha-principal--fe02-ctx01-a02). Seleção, catálogo e regras operacionais preservados.

## POP-DEV01-SQL01 — canal SQL direto autorizado, 10/10/2026

Lucas pediu popular o banco DEV e esclareceu expressamente que a população deve ser por SQL, sem entrar no site. Autoriza inserir a massa fictícia diretamente em WMS_DEV pela identidade própria WMSDEV, substituindo somente a exigência anterior de escrita HTTP para este recorte. Permanecem as guardas atuais de alvo/identidade/TLS/permissões/histórico, integridade transacional e preservação de dados, contas, migrations e processos. Não autoriza PROD, DDL, grants, exclusão ou emissão fiscal/financeira externa. Prefixo `LUCASDEV20261010`; valores comerciais da massa são apenas demonstração fictícia. [Andamento](../STATES.md#pop-dev01-sql01--população-direta-de-wms_dev-10102026).

## D33 — propriedade da mercadoria na entrada, 10/10/2026

**Resposta expressa do gestor encaminhada por Lucas:** no exemplo TIGRE → DALGA, os produtos pertencem à TIGRE e a DALGA apenas guarda. O gestor afirmou que essa regra se aplica a todos os pedidos. Na entrada, o cliente proprietário é associado ao emitente da nota; destinatário/armazém não adquire a propriedade pelo recebimento. A confirmação encerra a dúvida anterior e autoriza seguir a implementação solicitada BE06-XML01 → FE05-XML01. Importação documental, chegada, conferência e efetivação continuam comandos distintos.

## BE02-DEV-ARTEF01 → FE02-DASH02-A02 — recusa da visão geral, 09/10/2026

Lucas relatou 403 na visão geral usando a conta principal e perguntou sobre reinício. Diagnóstico concreto: o JAR indicado pelo launcher DEV não contém a API/rota da tela, embora a fonte já permita ao Gestor todos os contextos. Corrigida a seleção por novos recibos de build atual e guarda de correspondência fonte/JAR/APIs antes de abrir SQL; não ampliar privilégios da conta para resolver ausência de código. [Procedimento](43-login-e-administracao-de-usuarios.md#acesso-à-visão-geral-e-pacote-dev--be02-dev-artef01), [resultado local](../orchestracao/.runtime/acesso-visao-a01/resultado.json). Pacote preparado com testes H2; próximo reinício manual carrega a nova versão. Preparação não comprova integração real nem autoriza operação PROD; processos existentes preservados nesta entrega.

## POP-DEV01 — autorização de massa real DEV, 09/10/2026

Pedido expresso de Lucas: popular completamente as áreas suportadas do modo DEV real com cenários fictícios coerentes para teste, mantendo situações editáveis. Alvo exclusivo WMS_DEV, aplicação WMSDEV restrita/TLS e guarda atual antes de operação. Escritas somente por HTTP/API normal, prefixo próprio e prevenção de duplicação; verificar depois por leituras exatas API/frontend. Fiscal/financeiro simbólicos, sem emissão/cobrança externa. Preservar dados/históricos/migrations/principal/senhas/sessões/processos; sem SQLfixture/reset/DELETE/DDL/grants/PROD/saAPI/servidor/runtime/killrestart/ETL/rotinas. Acesso autenticado apenas por mecanismo protegido legítimo existente, sem segredo de conversa. Farol registra alcance e eventuais limites atuais; não declarar população por simulação. [Escopo](../orchestracao/.runtime/populacao-dev/inicio.json).

## FE02-REG01-A03 — filtros diretos na página, 09/10/2026

Lucas rejeitou a sanfona isolada de Filtros e contexto da consulta e o efeito de cartão dentro de cartão. Autoriza o formulário sempre visível nas páginas que compartilham essa estrutura, com um único título e sem borda interna, preservando as consultas e ações existentes. [Composição e conferência](design-system/etapa-06.md#filtros-visíveis-sem-sanfona--fe02-reg01-a03). Sem mudança de negócio ou contrato.

## FE02-REG01-A02 — alinhamento em todas as barras semelhantes, 09/10/2026

Lucas pediu alinhar Atualizar lista e esclareceu “todas que forem assim”. Autoriza unificar o alinhamento visual dos filtros de registros e mapa, preservando a quebra responsiva e os comportamentos existentes. [Regra compartilhada e conferência](design-system/etapa-06.md#alinhamento-compartilhado-dos-filtros--fe02-reg01-a02). Sem mudança de negócio, contrato ou autorização de operação PROD.

## FE02-DASH02-A01 — alinhamento da busca do mapa, 09/10/2026

Lucas pediu alinhar o botão Buscar na imagem do Mapa do armazém. Autoriza a correção visual da margem dos rótulos dos filtros, preservando busca, filtros e comportamento operacional. [Ajuste e conferência](design-system/etapa-05.md#alinhamento-da-busca--fe02-dash02-a01).

## FE02-REG01-A01 — cores e organização dos registros, 09/10/2026

Lucas pediu expressamente melhorar a organização da tabela mostrada em Clientes, dar cores aos badges de situação e aos botões. Autoriza o ajuste visual compartilhado, com cores semânticas, rótulos legíveis, ícones e hierarquia das ações nos dois temas. [Composição, conferência e limites](design-system/etapa-06.md#tabelas-badges-e-ações--fe02-reg01-a01). Valores de contrato/filtros, permissões e regras operacionais preservados; cores não comprovam disponibilidade nem confirmação de movimentação. Não há mudança de backend, schema ou autorização de operação PROD.

## FE05-PED01 — entrada centrada no pedido, 09/10/2026

Lucas pediu expressamente unificar as quatro etapas globais de Entrada e conferência em Pedidos de entrada, com criação no cabeçalho, notas/itens, conferência, divergências, histórico e efetivação no pedido selecionado. Essa orientação complementa FE02-REG01 e autoriza a refatoração frontend, preservando comandos, origens, revisões, permissões e efeitos de estoque. [Mapeamento, interpretação dos rótulos e validação](design-system/etapa-07.md). Conferido permanece distinto de Efetivado; não há alteração manual de situação. Filtros adicionais usam somente a página recebida conforme os contratos atuais, com alcance indicado. Dados gerais não possuem endpoint de edição; administração usa as operações existentes. A auditoria do pedido reutiliza a API disponível para Gestor. Não há nova regra de negócio, schema ou autorização de operação PROD.

## FE02-REG01 — listas e ações no registro, 09/10/2026

Lucas pediu expressamente refatorar as 12 áreas: abrir lista/fila/overview ao entrar, criar no cabeçalho, acessar detalhe/edição/ações pelo registro persistente e preservar etapas reais. Modal é o padrão de edição, adaptado ao mobile; operações extensas mantêm seções e comandos próprios. A apresentação segue o design system existente. Esta orientação autoriza a implementação frontend e substitui as abas artificiais dentro da página, sem mudar as regras backend. [Padrão e limites](design-system/etapa-06.md), [inventário](design-system/inventario-paginas.md). Cidade e vínculos de armazém do cliente dependem de campos/relação efetivamente expostos; o exemplo visual não autoriza inventá-los ou modificar o schema.

## FE02-CTX01 / BE14-DASH02 → FE02-DASH02 — contexto por nome e mapa, 09/10/2026

Lucas solicitou expressamente as três implementações: cliente por nome, armazém por nome e Início conforme o texto anexado. IDs continuam internos; nomes/códigos vêm dos cadastros autorizados, sem criação implícita. Todos significa alcance permitido. A composição inicial substituiu os quatro indicadores/gráficos de DASH01 por dez indicadores e mapa cadastral; a reinclusão dos gráficos em 10/10 está no incremento A03 abaixo. Capacidade física considera armazenagem ativa, enquanto o mapa mantém áreas especiais e inativos com estado próprio. Ocupação do armazém inclui seus clientes; demais indicadores seguem também o cliente escolhido. Valor reutiliza o indicador de estoque; faturamento é parcial do mês pelas NFS-e registradas, sem emitir documento ou recalcular cobrança. Operação não recebe valores financeiros; conteúdo fora do alcance é ocultado. Endereçar preenche o coletor, mantendo confirmação e regras backend. [Definições completas e conferência](design-system/etapa-05.md).

## BE14-DASH01-A01 → FE02-DASH02-A03 — gráficos e opção Todos, 10/10/2026

Lucas pediu ajustar a exibição dos gráficos existentes e esclareceu que devem mostrar informação também com Todos os clientes e Todos os armazéns. Essa orientação amplia o contrato de leitura DASH01 e complementa os dez indicadores e o mapa, sem retirar nenhuma dessas partes. Todos segue o alcance autorizado; produtos de clientes distintos e unidades de medida distintas não são fundidos. [Composição, contrato e limites](design-system/etapa-05.md#gráficos-no-início-e-opção-todos--be14-dash01-a01--fe02-dash02-a03). Conferência local e pacote DEV separados da versão em execução, sem migration, banco real ou reinício pelo agente.

## BE14-DASH01 → FE02-DASH01 — gráficos do Início, 09/10/2026

Lucas autorizou aplicar a proposta proporcional de gráficos abaixo do Acesso rápido: ocupação física, disponibilidade por SKU e fila de saída, com quatro indicadores e aderência à fundação visual. O backend em `backend/` recebe contrato agregado de leitura com escopo validado no serviço; não há migration ou nova regra operacional. Totais do contexto não são inferidos de uma página de produtos. Capacidade/livres do armazém ficam somente com Gestor; quantidades mantêm SKU/unidade e reserva parcial continua bloqueando o restante da unidade. [Composição, contrato e limites](design-system/etapa-04.md). Histórico de movimentações e financeiro são evoluções propostas, sem tratá-las como autorização para mudar cobranças ou iniciar nova fase.

## PROD-LAUNCH01 — launcher Windows e publicação futura, 09/10/2026

Pedido expresso de Lucas: preparar e testar `iniciar-prod.bat`, com inventário de portas e domínios futuros **wms.rodogarcia.com.br** e **wms-api.rodogarcia.com.br**. BE02-PROD01 → FE02-PROD01; preparo local em candidatos isolados, preservando a frente visual e os processos existentes. Operação exclusiva WMS_PROD/identidade própria/TLS, sem testes ou escritas de negócio em PROD, migrations/grants/servidor, fallback DEV ou configuração Cloudflare/DNS/túnel. A grafia recebida `.bar` permanece no pedido original.

Portas planejadas 127.0.0.1:25590 backend e :25591 frontend, conferidas novamente a cada subida. `--preparar` real passou em versão datada; execução normal real recusou AUTH PROD ausente antes de SQL/servidores. Metadados não localizaram canais próprios SQL/AUTH PROD no recorte autorizado. O gate SQL atual recusa e não possui binding operacional/ramo PASS: o canal protegido e sua integração precisam ser concluídos/revisados antes de uso, sem criar identidade ou permissões nesta entrega. HTTPS de entrada e publicação ficam separados do readiness HTTP loopback. [Uso, limites e suporte](44-launcher-producao-e-publicacao-futura.md), [recibo](../orchestracao/.runtime/launcher-producao-resultado.json).

## FE02-DS03-A06 — Minha senha em modal e composição de usuários, 09/10/2026

Lucas determinou que Minha senha abra em pop-up sobre a página atual e pediu melhor organização, uso de espaço e separação do fundo nos diálogos de usuários. No refinamento, rejeitou a faixa azul decorativa no topo e exigiu aderência ao design system. A versão atual remove a faixa e usa superfícies, bordas, espaçamento, ícones e ações da fundação documentada; [composição e conferência](design-system/etapa-03.md#diálogos-de-conta--fe02-ds03-a06). Regras de acesso e senha preservadas; a troca obrigatória de senha temporária mantém seu fluxo anterior à área operacional.

## FE02-DS03-A05 — fundos distintos no Acesso rápido, 09/10/2026

Lucas pediu cada card de Acesso rápido com fundo marcante e diferente, preservando o conteúdo interno e seguindo a documentação visual. A adaptação P do WMS usa azul cobalto, verde petróleo, índigo e grafite azulado, com detalhes geométricos em CSS e tokens próprios. Definição, origem e conferência em [etapa-03.md](design-system/etapa-03.md#fundos-dos-acessos-rápidos--fe02-ds03-a05). Cores identificam os atalhos visualmente; não indicam situação de operação nem alteram permissões ou destinos.

## FE02-DS03-A04 — referências com apresentação própria, 09/10/2026

Lucas pediu estilizar todo o painel Referências da jornada no contexto atual, inclusive o botão branco, nas páginas que o compartilham. Implementação visual com superfície de apoio, hierarquia interna e botão azul em [etapa-03.md](design-system/etapa-03.md#referências-da-jornada--fe02-ds03-a04). Mantidos aviso de descarte da edição, seleção explícita, dados e bloqueios existentes; não há nova regra operacional nem retomada automática da textura A01.

## FE02-DS03-A02 — Coletor integrado às páginas, 09/10/2026

Lucas pediu ajustar a apresentação do Coletor e corrigiu expressamente a proposta centralizada: o painel acompanha o alinhamento do título e a largura útil da página. Escolha da tarefa e leitura são organizadas lado a lado no desktop e empilhadas no mobile. Definição e conferência em [etapa-03.md](design-system/etapa-03.md#composição-do-coletor--fe02-ds03-a02). Recorte visual, sem alterar operações ou contratos; refinamento da textura A01 interrompido a seu pedido.

## FE02-DS03-A03 — limite de altura global, 09/10/2026

Lucas determinou que caixas de entrada redimensionáveis tenham limite de altura em todo o projeto, citando Motivo / justificativa de Cadastros → Clientes. Aplicação visual comum aos campos multilinha, com redimensionamento limitado e rolagem interna. Teto e cobertura definidos em [etapa-03.md](design-system/etapa-03.md#limite-de-altura-dos-campos--fe02-ds03-a03), sem alterar limites de caracteres ou conteúdo dos contratos.

## FE02-DS03 — organização interna e contexto no topo, 09/10/2026

Lucas pediu organizar todas as páginas internas, com hierarquia de ações, melhor uso de espaço, paginação junto dos resultados e diálogos centrais. Nos refinamentos expressos, definiu Cliente (ID), Armazém (ID) e Aplicar contexto no header, separado das demais ações, mantendo a altura desktop anterior de 64 px; nome e perfil permanecem na sidebar. Os textos dos campos são placeholders sem espaço reservado e desaparecem ao focar. Aplicar contexto recebe cor própria distinta do azul da marca, e a sidebar mantém a escolha expandida/minimizada após refresh. Implementação e limites estão em [etapa-03.md](design-system/etapa-03.md). Escopo visual autorizado; regras, contratos, permissões e fontes de contexto existentes preservados.

## FE02-DS01-A03 / FE02-DS02-A01 — preferência salva e destaque textual, 09/10/2026

Lucas corrigiu expressamente a política de tema: detectar o sistema no primeiro acesso sem escolha salva e respeitar Light/Dark escolhido nos próximos acessos. Essa decisão substitui a reinicialização por dispositivo de A02, mantendo o botão sol/lua. Definição em [etapa-01.md](design-system/etapa-01.md#temas). Pediu também remover o destaque em torno das ações de ajuda; o ajuste A01 mantém somente sublinhado do texto e foco por teclado, descrito em [etapa-02.md](design-system/etapa-02.md). Mesmo escopo visual, sem mudança de autenticação.

## FE02-DS02 — login, topo e rodapé, 09/10/2026

Lucas pediu melhorar o login e seu card, alinhar e completar o cabeçalho e acrescentar rodapé com suas informações encontradas nos outros projetos da pasta `projetos`. Na mesma entrega, pediu login sem rolagem, correção da apresentação dos erros, ajuda sem movimento da tela e troca do ícone de suporte. Autorizada a alteração visual local com a fundação FE02-DS01-A02, mantendo contratos e autenticação D32. Fontes visuais consultadas em leitura confirmam Lucas Andrade, LinkedIn `dev-lucasandrade` e suporte `lucasmac.dev@gmail.com`. Composição, informações, conferência e origem das adaptações estão em [etapa-02.md](design-system/etapa-02.md). Consulta a rodapés não concede acesso a bancos, processos ou integrações desses projetos.

## FE02-DEV-CON02 — correção do prazo solicitada em 09/10/2026

Lucas pediu expressamente corrigir a expiração. Cliente WMS DEV passa de30 para120s de abertura SQL; launcher passa de90 para240s para a guarda completa, com andamento a cada10s. Política central em `infra/dev/espera-conexao.ps1`, consumida pelos dois auxiliares. Inclui verificação local e uma guarda atual pelo canal próprio WMS_DEV/WMSDEV; não altera servidor, TLS, direitos ou regra de conexão única. O prazo maior é um ajuste do cliente e não uma conclusão sobre a causa da indisponibilidade.

**Resultado:**27 casos locais aprovados; a única abertura real nova expirou após120,19s, com consultas0. Metadados de acesso ao ERRORLOG recusados por SecurityException; causa não comprovada. [Evidências e encaminhamento](../orchestracao/.runtime/dev-conexao-20261009-con02-resultado.json). Não declarar disponibilidade resolvida nem iniciar outra tentativa automaticamente.

## VALID-LOGIN-CAD01 — fecho parcial autorizado por Lucas, 09/10/2026

**BE04-COR01-DEV01 → FE03-COR01-DEV01 parcial real revisado:** reaproveitado run existente8fa3c9e6722b4170898dc66aabfa044f, frontend http://127.0.0.1:25581, BE72644/25580 e FE70328/25581 com JAR corrigido99128/configD415/preparo947D. Guarda3293 confirmou WMS_DEV/WMSDEV/TLSMandatory/trustServerCertificatefalse,26/26critérios/permissões/catálogo/V1–V11, uma Open215,5245ms e12SELECTmetadados, zero negócio. SQLPID48480/início18:31:41Z e confiança existente atual conferidos; nenhuma outraBAT/Open/renovação ou intervenção em processo nesta retomada. Relato Lucas preservado; CON02/E7B204 permanecem históricos, sem bloqueioSQL corrente diante destePASS datado.

Lume observou realmente18:46:53–57UTC: statusAPI200, usuários anônimo401, CSS/Inter/foco/clique/Tab/Enter1440+390,24asserções públicas aprovadas sem mock/interceptação. CSRF200/renovação automática403,0pageerror/CSP e1consolemetadata sem texto; causa do403 não inferida. Chrome próprio encerrado, servidores preservados. Deltas visuais externos durante/depois da prova qualificados; DOM/capturas datados, sem freeze geral ou equivalência integral posterior.

**7 locais aprovados preservados/0 critérios integrais operacionais reais:** não disponível à equipe sessão WMS própria autenticada; hash bootstrap não recupera senha. Workspace006, Users007 e demais jornadas protegidas não executados. Login nativo D32/DEV07 existe; não retomar proposta histórica Keycloak/BFF. Link loopback é superfície técnica, modo tunnel/Secure preservado; entrada HTTPS operacional e sessão não comprovadas por status/listener. Long gigante/201null/corrida específica continuam complementos locais, sem injeção no banco/API.

[Recibo atual e limites por001–007](../orchestracao/.runtime/login-cadastros-validacao-dev-resultado.md), [JSON/hashes](../orchestracao/.runtime/login-cadastros-validacao-dev-resultado.json), [parecer independente](../orchestracao/.runtime/login-cadastros-validacao-dev/vigia/parecer-retomada-02.md), [prova pública real](../orchestracao/.runtime/login-cadastros-validacao-dev/lume/browser-publico-real02/resultado.json) e [parcial anterior literal](../orchestracao/.runtime/login-cadastros-validacao-dev/retomada-20261009T183735Z/historico/login-cadastros-validacao-dev-resultado.md). Prumo ambiente/guarda; Cedro fonte/JAR/API; Lume browser; Vigia confronto independente; Farol consolidação. Nenhum material novo demonstrado, sem homologação operacional001–007/roundtrip de negócio.

**Atualização após disponibilização da conta:** Lucas confirmou login real funcionando; CUA do Farol retornou apps[]/browsers[]/zero abas acessíveis. Não há contradição com sessão existente no computador. [Observação deste acesso](../orchestracao/.runtime/login-cadastros-validacao-dev/sessao-disponibilizada-20261009T192151Z/resultado.md); nenhuma senha usada/lida/registrada, nenhuma API/SQL/guarda/app nova. Perfil/alcance da conta ainda não observado, sem autorização de escrita por inferência.

**Decisão expressa de Lucas — encerrar parcialmente:** origem https://1z8126n0-25581.brs.devtunnels.ms/ fornecida, mas CUA sem navegador/aba WMS disponível (`No browser is available`). Não converter login funcional relatado em homologação. SQLPASS datado e sete locais aprovados preservados;0/7 critérios operacionais integrais. Responsável segue na frente visual; nenhuma nova tentativa/instalação/configuração/login/ação dele exigida. [Fecho autorizado](../orchestracao/.runtime/login-cadastros-validacao-dev/origem-https-cua-20261009T194922Z/fecho-autorizado.json). Frente visual, fonte/aplicação/estilos/launcher/mapa e processos preservados. Equipe para este escopo e aguarda nova demanda; sem revisão/ACK/rotina/Hermes/ETL.

## FE02-DEV-CON01 — relato de falha na abertura DEV, 09/10/2026

**Observação, sem nova decisão de ambiente:** Lucas apresentou timeout do launcher no run `874b0c4b965c4e9f831487a48ec94acd`. Revisão de recibos/código e metadados Windows confirma abertura expirada após30,2s, sem consultas ou aplicação iniciada; causa ainda não demonstrada. [Evidências e próximo encaminhamento](../orchestracao/.runtime/dev-conexao-20261009-874b0c4b.md). Preservar limite existente de não repetir sondas após falha de transporte e não mudar SQL Server/runtime; o relato não autoriza reinício ou alteração global.

## CORR-LOGIN-CAD01 — sete correções concluídas e revisadas localmente, 09/10/2026

**BE04-COR01 → FE03-COR01 concluídos no recorte autorizado:** AUD-CONS-001 a007 corrigidos e revisados independentemente; nenhum achado material local aberto nesta entrega. Login próprio D32/DEV07 e incrementos DS01 preservados. Cedro backend004/005; Lume AUTH/UI e leitores005; Prumo contratos/persistência em arquivo; Vigia revisão; Farol consolidação/canônicos. Atalho006 já corrigido pela frente DS01, com autoria externa preservada e handler atual idêntico por diff.

Provas: frontend253 verdes únicos por composição252+1, tipagem/lint/build e3 testes Chrome com API sintética. Backend66 verdes na fonte MARCO02 e14 novos verdes no delta final MARCO03, separados por versão; builds offline aprovados e JAR não executado. Parecer final confirma os sete critérios. Não é certificação geral de segurança, teste integral da fonte global ou integração real com banco.

[Recibo final e limites](../orchestracao/.runtime/login-cadastros-correcoes-resultado.md), [JSON/hashes](../orchestracao/.runtime/login-cadastros-correcoes-resultado.json), [matriz individual](../orchestracao/.runtime/login-cadastros-correcoes/matriz-problemas-casos.md) e [parecer independente](../orchestracao/.runtime/login-cadastros-correcoes/vigia/parecer-final.md).

Namespace do vínculo: `VINCULO_COBRANCA` é lógico na API/consulta/UI; carrier físico e ações já permitidos foram preservados, com `tipoFisico` aditivo. Qualquer marcador exige envelope completo/coerente e cada documento presente precisa de prova própria. Ambíguos permanecem sem reescrita, acessíveis como `LEGADO_NAO_ATRIBUIDO`; nenhum tipo SQL novo/migration. Contrato e casos completos na matriz. Esta correção não altera a arquitetura de autenticação.

Fonte/provas datadas: deltas posteriores da frente visual qualificados por comparação/adenda, sem alterar os caminhos revisados dos7critérios; não declarar200fontes atuais globalmente iguais. Geração03 preserva174inputs históricos, incluindo AuditoriaService5801 anterior ao delta0F12; DTO/API/permissão/outputs permanecem iguais por adenda. Mapa AST das correções atualizado semforce/LLM, com backups; comunidades/semântica integral não recalculadas e últimos ajustes visuais externos posteriores qualificados.

Nenhum SQL/banco/HTTPDEV/DDL/migration/PROD/sa/provider novo/alteração servidor/runtime/kill ou restart existente/ETL/Hermes. Auditoria original, FINAL14, TLS01/DEV04, migrations/dados e históricos preservados. Próximo passo: aguardar nova demanda de Lucas; não iniciar aplicação, banco, teste ou macrobloco automaticamente. Homologação real/custo JPA e histórico ambíguo permanecem limites próprios, sem reabrir tarefas antigas.

## FE02-DS01 — primeira etapa do design system, 09/10/2026

**Correção expressa FE02-DS01-A02:** Lucas pediu remover o ícone de Sistema e detectar automaticamente o tema do dispositivo ao iniciar. Esse comportamento substitui o monitor e a preferência salva de A01; permanece somente o botão circular sol/lua. [Definição atual](design-system/etapa-01.md#temas).

**Histórico FE02-DS01-A01, tema substituído por A02:** Lucas pediu Sistema como padrão inicial, troca por ícone circular sol/lua e minimizar a lateral à esquerda até restarem os ícones. Implementação e comportamento registrados na [entrega visual](design-system/etapa-01.md); preferências manuais salvas continuam respeitadas, sem alterar login ou regras operacionais.

Pedido expresso de Lucas: aplicar por etapas `docs/design-system/design.md`, começando pela organização, tipografia, cores, temas claro/escuro e menus superior/lateral. Fundação e navegação compartilhadas implementadas; defaults visuais e adaptações WMS, evidências e limites no [registro da etapa](design-system/etapa-01.md). A referência visual não altera regras de negócio, permissões ou contratos. CORR-LOGIN-CAD01 mantém sua autorização e posse próprias; não atribuir suas correções à entrega visual.

### Histórico preservado — auditoria e incrementos anteriores

## AUD-LOGIN-CAD01 — auditoria concluída com achados; correções aguardam Lucas

BE04-AUD01 → FE03-AUD01, pedido expresso de 09/10/2026. Cedro, Lume, Prumo e Vigia concluíram seus recortes; Farol consolidou. Login próprio D32 e administração de usuários existem; Keycloak/BFF histórico não adotado. Sete achados materiais abertos: um P1 (401 antigo encerra sessão nova) e seis P2 (Long nativo, resposta inválida/sucesso indevido, categoria após normalização, namespace de vínculo, atalho de conteúdo e carregamento após erro). Sem vulnerabilidade crítica/escalada demonstrada; sem aceite geral de segurança ou integração real. [Recibo e propostas](../orchestracao/.runtime/login-cadastros-auditoria-resultado.md), [JSON/provas](../orchestracao/.runtime/login-cadastros-auditoria-resultado.json).

Matriz individual: 74 contratos HTTP; 1395 instâncias HTTP/frontend, 240 complementos backend e 252 atributos persistentes em 22 entidades, com grãos distintos. Pareamentos JPA-DDL/direitos em arquivo252/252; divergências e limites por fronteira preservados. [Matriz](../orchestracao/.runtime/login-cadastros-auditoria/matriz-atributos.md), [atributos completos](../orchestracao/.runtime/login-cadastros-auditoria/matriz-atributos.json) e [parecer independente](../orchestracao/.runtime/login-cadastros-auditoria/vigia/parecer.md). Não somar reutilizações/contagens como cobertura ou aceite.

Aplicação somente leitura. Provas novas isoladas e sintéticas; nenhuma correção app, SQL/guarda/JDBC/H2/HTTPDEV/provider/launcher/build backend integral/processo real/segredo/ETL/Hermes nesta auditoria. Fonte inicial411:404iguais/7deltas DEV06/DEV07 datados; nenhum freeze global presumido. Os15focais Origin são DEV06; DEV07 posterior lido separadamente e confiança dos headers/topologia real é limite. TLS01/DEV04, FINAL14, migrations, dados e processos existentes preservados.

Próximo passo: Lucas definir o início das correções propostas; nenhum job, refatoração, auth nova ou processo iniciado automaticamente no fecho. Estado/regras D32-DEV07 abaixo continuam preservados como incremento distinto. Registro do fechoUTC: 2026-10-09T15:53:57.298249+00:00.

### Estado anterior preservado — D32 e demais entregas

## D32 — autenticação e usuários próprios WMS, 09/10/2026

Correção expressa D32-DEV07: usuário esclareceu que o endereço do Dev Tunnel é variável. Retirada a URL fixa da configuração; modo tunnel usa origem/destino da requisição atual no proxy loopback, preservando CSRF e cookies Secure. Não exige edição manual quando o túnel muda. Relato400 ao trocar senha tratado com regras explícitas no formulário e teste de troca/novo login; mensagem real solicitada para confirmar a causa. [Validação e limites](../orchestracao/.runtime/login-d32/dev07-resultado.json). Mantidos dados, configuração protegida e processos existentes.

Incremento D32-DEV06: o relato de falha403 usando o Dev Tunnel motivou adequação do mesmo login à origem HTTPS exata informada. Configuração pública separada em `infra/dev/acesso-dev.json`; cookies Secure e CSRF obrigatórios, sem liberação genérica de domínios. Alias localhost do túnel restrito ao perfil DEV e conexão loopback. Não altera senha, exposição/permissão do túnel, dados ou processos existentes. [Implementação e validação local](../orchestracao/.runtime/login-d32/dev06-resultado.json); reinício e primeiro login real nesse modo pelo usuário permanecem pendentes.

Incremento D32-DEV04: após apresentação do escopo (nova verificação, tabelas e permissões do login somente WMS_DEV e inicialização do sistema), Lucas pediu “preciso que ajeite que quero rodar iniciar-dev.bat e ja testar o sistema em modo dev”. Autoriza concluir essa ativação, usando os canais protegidos existentes; administração somente para V11 e direitos mínimos, nunca na API. Sem PROD, alteração de servidor, perda de dados ou intervenção em processos existentes. Substitui a pendência de autorização SQL descrita abaixo somente neste recorte.

Resultado DEV04: V11 via Flyway e 22 direitos mínimos aplicados/conferidos exclusivamente WMS_DEV. BAT validado com guarda atual, backend WMSDEV e frontend reais; tela de login desktop/mobile, CSP e respostas públicas/protegidas conferidas. Instância de teste encerrada, portas liberadas para o usuário. Login com senha real e operações autenticadas permanecem como próximo teste do usuário, sem presumir homologação. [Recibo](../orchestracao/.runtime/login-d32/dev04-resultado.json).

Lucas autorizou implementar login e gestão de usuários dentro do WMS, incluindo administradores delegados. Conta principal: `desenvolvedor@rodogarcia.com.br`, protegida contra exclusão, desativação, mudança de identidade e redução de privilégios; outros administradores não podem redefinir sua senha. Todos os cadastros e redefinições administrativas exigem troca da senha temporária antes de liberar operações. Administrador de usuários é uma permissão separada do perfil operacional (Gestor/Supervisor/Operação). Senhas nunca são recuperadas ou mostradas: somente redefinidas. Alteração de senha/permissões/desativação revoga sessões existentes; saída revoga a sessão corrente.

Escolha técnica deste pedido: Spring Security, hash adaptativo PBKDF2, JWT RS256 curto em memória do navegador, renovação por token opaco rotativo em cookie HttpOnly/SameSite/Secure, proteção CSRF na fronteira de autenticação e consulta de revogação no backend. A proposta anterior Keycloak/BFF fica histórica. Conta inicial provisionada uma única vez a partir de material protegido externo; reiniciar não restaura senha ou privilégios de contas comuns. Migration nova preparada e validação isolada autorizadas; DDL/grants/PROD/alterações no servidor não são executados por este pedido de implementação.

## Resultado atual D31-DEV02-TLS01 — SQL/TLS aprovado; AUTH pendente

Handoff expresso de Lucas absorvido por Farol em 09/10/2026. Provas do apoio Codex: guarda WMS_DEV/WMSDEV aprovada em 2026-10-09T11:56:41.5843363Z, 26/26 critérios; SqlClient e JDBC PASS, TLS validado (`Encrypt=true/Mandatory`, `TrustServerCertificate=false`), zero alterações SQL. O helper atual corresponde ao SHA D2FF82F7241F448FA5FA836A732A1F9CB2ECE2137B6735DAFDBD1AD4F06B35A9. A baseline D29 de permissões/catálogo/histórico foi preservada. [Provas e proveniência](../orchestracao/.runtime/frontend-integracao-dev-tls-handoff-20261009.md).

SQL/TLS resolvido conforme estas provas datadas; pin/recibo antigos e recusas anteriores ficam históricos, sem inferir alteração servidor/sa. Regra permanente em [AGENTS.md](../AGENTS.md): confiança corresponde ao processo atual; renovação segura autorizada, sem recaptura automática. Guarda normal futura exige atualidade e pertence à execução integrada após resolução AUTH, não ao recebimento deste handoff.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, Keycloak/BFF não aprovado. Backend/API não iniciados; URL integrada=null, roundtrip=false, entrega integrada aberta. Mapa anterior preservado, atualização estrutural pendente após rejeição; nenhum mapa/SQL/guarda/JDBC/build/teste/processo executado por Farol nesta absorção. FINAL14 local aprovada; D30 geral aberta, aceiteLocalIntegral=false; C06/C07/C10 requeridos impedidos históricos preservados. [Recibo vigente](../orchestracao/.runtime/frontend-integracao-dev-resultado.json).

## Histórico preservado — conteúdo anterior à absorção Farol TLS01

## Resultado atual D31-DEV02 — nova guarda SQL bloqueada TLS; AUTH separada

Após relato Lucas “sql server voltou”, Prumo verificou canal próprio independenteAUTH: iníciohelper2026-10-09T11:30:14.2409217Z,Open2026-10-09T11:30:16.2451044Z,fim2026-10-09T11:30:16.5307190Z,PID29060,UMAOpen258.9705ms/exit20 filho capturado. TLS0x800B0110/native-2146762480 faseABERTURA antesSELECT; queries0/handshakefalse, alvo/identidade/permissões/catálogo/histórico não confirmados. Relato não éPASS nem comprova estado sa/disponibilidade geralSQL. [Resultado](../orchestracao/.runtime/frontend-integracao-dev-retomada-sql-20261009.md), [guarda](../frontend/evidencias/frontend-prumo-dev02-guarda-retomada-20261009.json).

Interrompido sem segundaOpen/BE/API/proxy/sa/PROD/bypass/TLSglobal/DDL/migrations/grants/servidor/restartkill/ETL/Hermes. Helper1376 antes/depois igual e credencialWMSDEV protegida em memória descartada; segredo não publicado. Vigia conferiu recibos em leitura sem novaSQL. Responsável TLS/SQL apresenta encaminhamento seguro para a recusa atual; causa/peer não comprovados, nenhum ajuste compartilhado ou nova sonda automática.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, independenteSQL; Keycloak/BFF NÃO aprovado nem criado. Nenhum backend/URL integrada sem autenticação real adequada. Mesma demandaDEV02, sem missão duplicada; auditoriaSA/FINAL14/MARCO03/históricos preservados. FINAL14 local aprovada; D30geralaberta, aceiteLocalIntegral=false,C06/C07/C10 requeridos impedidos históricos. RegistroUTC2026-10-09T11:38:25.736665Z.

## Histórico preservado — estado anterior à nova verificação SQL

## Resultado atual D31-DEV02 após AUD-SQL-SA01 — BAT real40; decisão AUTH e TLS pendentes

BAT WMS real executado após auditoria em 2026-10-09T02:22:04.2682459Z: exit40 AUTH_CONFIGURATION_MISSING das três referências públicas; SQLGuard/BE/FE=false, URL=null, roundtrip=false, sem mock. Portas/proprietários observados iguais antes/depois; nenhuma nova Open ou alteração do SQL Server. [Run atual](../orchestracao/.runtime/frontend-integracao-dev-runs/e169e141b28d4537b02a2a33cd9a8463/launcher.json), [consolidação](../orchestracao/.runtime/frontend-integracao-dev-apos-auditoria-adenda.md).

Referência EXATA dashboards-etl examinada por Lume:14 fontes técnicas/10WMS e24 hashes conferidos. Organização BE→readiness→FE é aproveitável; login próprio HMAC daquele projeto não corresponde a ResourceServerRS256/JWK WMS. Nenhum launcher alheio executado ou env/conexão/conta/credencial/segredo/controle copiado; não portar killporporta/limpalogs/Securefalse/CSRFdisable/replay401. Keycloak/BFF NÃO aprovado nem criado. Nenhuma fonte própria OIDC localizada nos canais examinados; ausência delimitada, não universal. Não há loaderfix demonstrado.

Decisão material ÚNICA continua D31-DEV02-AUTH-DECISAO01: proposta Keycloak DEV exclusivoWMS+BFF, dono de identidade designado, API RS256 preservada. Pedido exige decisão antes provider/servidor novo ou mudança de autenticação. Sessão operacional FE/catálogos/adapter ainda necessários após contrato aprovado. TLS SQL é impedimento independente0x800B0110 antesSELECT na única Open; sem causa/peer comprovado e sem segunda sonda. Responsável TLS/SQL apresenta encaminhamento seguro; nenhuma mudança servidor/trust global autorizada. [Proposta](../orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md).

AUD-SQL-SA01 encerrada em leitura: D26identidadeWMSDEV/direitos eD27V10 históricos comprovados; nenhuma mudança sa/senha/authmode demonstrada no recorte. SQLPID65088→21812/início07out20:57:17.273Z sem autor/causa. ERRORLOGnegado/18456state/reason desconhecidos; operador autorizado obter extrato e flags pela sessão Windows existente, semsenha/novaSQLagente/reparo. [Auditoria](../orchestracao/.runtime/sql-auditoria-login-sa-resultado.md).

Mesma DEV02 aberta, não novo macrobloco. Sem repetição FINAL14/suítes antigas, Keycloak/provider, SQL/sa/PROD/DDL/grants/restartkill/ETL/Hermes. Lume referência; Prumo OS/registro; Cedro execuções/AUTH; Vigia revisão; Farol BAT/canônicos/consolidação. FINAL14 local aprovada preservada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos. Registro UTC 2026-10-09T02:26:22.791284Z.

## Histórico preservado — auditoria SA e contexto DEV02 anteriores

## Resultado atual AUD-SQL-SA01 — auditoria encerrada em leitura; causa sa não determinada

Efeitos históricos WMS comprovados: D24 validação administrativa sa; D26 criação de login/usuário/direitos WMSDEV; D27 V10 WMS_DEV. Prumo por atribuição documental, operador Windows não individualizado. Nenhuma execução/efeito de mudança de sa/senha/authmode demonstrado no recorte; não é garantia universal. Processo SQL mudou: PID65088 baseline07out20:38:51Z → PID21812 criado07out20:57:17.273Z, sem autor/causa identificados. Registro gravado LoginMode2/ForceEncryption0/certificado vazio sem baseline comparável, não flags sa/peerTLS.

18456 sa/state/reason atuais indisponíveis: Parameters/ERRORLOG negados e Windows sem evento utilizável; não significa ausência de falhas. Próximo passo do operador já autorizado: extrato nativo sanitizado e consulta única de flags pela sessão Windows existente, sem senha no chat/nova conexão do agente/reparo. ZERO SQL/Open/sa/DPAPI/BE/API/grants/servidor/restart/kill na auditoria. [Resultado](../orchestracao/.runtime/sql-auditoria-login-sa-resultado.md), [fontes/hashes](../orchestracao/.runtime/sql-auditoria-login-sa-resultado.json).

Lucas determinou retomar a MESMA DEV02 após os achados, confrontando somente referência técnica dashboards-etl. Keycloak/BFF não aprovado; integração ainda exige BAT real, guardas WMS_DEV/WMSDEV/TLS e roundtrip. FINAL14 e demais históricos preservados. Fecho UTC 2026-10-09T02:20:38.625201Z.

## Contexto anterior preservado — D31-DEV02 ainda pendente

## Resultado atual D31-DEV02 — integração real pendente; decisão AUTH e impedimento TLS atuais

**Run real de Lucas:** iniciar-dev.bat em 2026-10-09T01:14:35.6191414Z → exit40 AUTH_CONFIGURATION_MISSING das três referências OIDC. Guarda SQL não invocada; backend/frontend não iniciados; URL integrada e roundtrip inexistentes. [Recibo observado](../orchestracao/.runtime/frontend-integracao-dev-runs/7483449c3bcf41608496bc8eb35e3982/launcher.json).

**Origem AUTH investigada:** Prumo/Cedro verificaram presença apenas: 9/9 ausentes em Process/User/Machine, zero valores publicados. Fontes públicas WMS definem ResourceServer RS256/Bearer e placeholders; canais locais consultados são SQL. Nenhuma fonte OIDC própria previamente aprovada/configurada foi localizada nesses escopos/fontes; isso não prova ausência universal corporativa. Não há correção de carregamento demonstrada. O frontend real ainda precisa da sessão operacional, além das três referências.

**Uma proposta, não aprovação:** Farol propõe Keycloak DEV exclusivo WMS, gerido por responsável de identidade designado, com sessão BFF same-origin no backend; tokens/secret fora do navegador, API RS256/Bearer stateless preservada. Provider/contas/BFF não criados. Lucas precisa decidir este arranjo antes de provisionar ou mudar autenticação, como exigiu no pedido. [Descoberta e proposta](../orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md), [fontes/hashes](../orchestracao/.runtime/frontend-integracao-dev-auth-proposta.json). Não pedir três valores técnicos soltos nem usar identidades de outros projetos.

**SQL/TLS independente:** única Open atual em 2026-10-09T00:51:02.8522544Z, PID32144, 454,7449ms, recusou TLS0x800B0110 antes SELECT. Alvo real/identidade/permissões/catálogo/histórico não confirmados; zero SELECT/API/backend. PEM/DER configurados no cliente não são peer atual nem comprovam causa. Sem segunda abertura/bypass/recaptura. Responsável TLS precisa apresentar correção segura; aprovar AUTH não resolve nem libera automaticamente SQL. [Guarda atual](../frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json).

**Preparo local revisado, entrega integrada aberta:** launcher único REAL/default, frontend sem fallback, helper/readiness BE e guardas foram implementados e revisados. Vigia encerrou achados locais001–003; FE CORE01 tem188 fontes/63 focais, tipo/lint e dois builds locais; browser estático4, sem API. Cedro20 focais isolados; launcher AUTH4 green e cleanup10 green. Estas provas não comprovam autenticação ou WMS_DEV real. FINAL14 não foi repetida. Só arquivos/recibos próprios dos cinco WMS; sem agentes/conexões novos, Hermes ou ETL. Nenhuma alteração de fonte frontend após CORE01 por este registro.

Farol: launcher/canônicos/recibo/mapa/nota; Cedro: backend/config/readiness/AUTH origem; Lume: frontend/sessão/cliente; Prumo: guarda/TLS/canais; Vigia: revisão independente. Processos históricos5178/5188/5189 preservados; não são URL integrada desta entrega. Após decisões e pré-requisitos seguros no escopo autorizado, será necessário executar BAT real e comprovar login/consulta browser→API→WMS_DEV/WMSDEV. Sem PROD/sa/DDL/migrations/grants/servidor/sharedruntime/killrestart existentes/publicação/commit/push/ETL/rotinas.

Recibo vigente: [JSON](../orchestracao/.runtime/frontend-integracao-dev-resultado.json), [MD](../orchestracao/.runtime/frontend-integracao-dev-resultado.md). Leitura mínima: AGENTS → states → docs/09 → docs/19 → recibo → proposta AUTH/guarda e parecer conforme pertinência. FINAL14 local aprovada preservada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos históricos. Base frontend MARCO03 local preservada; não equivale ao sistema inteiro ou integração real. Registro UTC 2026-10-09T01:30:19.545206+00:00.

## Histórico preservado — fecho local anterior e limite DEV01

## Resultado atual D31 — base local concluída e revisada; integração real impedida

FIM_FRONTEND_LUCAS_20261008: base React/TypeScript funcional concluída e aprovada na revisão local integrada de Vigia. Fonte SHA256 C58B64105A7B3232F314D56F8150CCBD6CE1530B6FE4D33837BA46AD3DD4F922 (179 arquivos); tipagem/lint/build, 299 testes locais e 16 Chromium, todos exit0. As 22 parcelas essenciais usam provas compartilhadas; smokes contratuais não equivalem a jornadas integrais. Organização, legibilidade, acoplamento e testabilidade revisados; CSS preto/branco/azul efetivamente aplicado no navegador desktop/coletor. [Resultado JSON](../orchestracao/.runtime/frontend-resultado-lucas-20261008.json), [MD](../orchestracao/.runtime/frontend-resultado-lucas-20261008.md), [parecer](../orchestracao/.runtime/frontend-vigia-parecer-lucas-20261008.json). Registro UTC 2026-10-09T00:08:45.382787+00:00.

**Abrir frontend:** http://127.0.0.1:5189, processo próprio Lume PID19856, exercício fictício. Vínculo de fonte e observações em [recibo de execução](../frontend/evidencias/frontend-lume-exercicio-marco03.json). npm run dev usa vite.dev.config.ts: [guia](../frontend/docs/desenvolvimento-dev.md). 5178/5188 preservados; 5188 é histórico com FE-VIG-008 e não é a versão corrigida. API/SQL, provider/contas/tokens/CORS reais, usuários sem rota, equipamentos/fiscal/piloto permanecem pendentes; aprovação local não homologa o sistema inteiro ou FE13 real.

**DEV01 real BLOQUEADO G01:** falta prova atual de resolução segura D29. [Guarda Prumo](../frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json): guardaRealAprovada=false. Canal WMSDEV conferido apenas por metadados; zero leitura de segredo/DPAPI, SQL/SELECT/API/backend/fixtures. Configuração DEV preparada, modo real fechado. Responsável SQL deve fornecer evidência atual; depois avaliar nova guarda WMS_DEV/WMSDEV/TLS/permissões/catálogo/histórico. Nenhuma sonda/fallback ou intervenção em servidor/acessos/runtime compartilhado autorizada por este fecho.

Lume: integração frontend/checks; Cedro: FE12 e preparo mínimo backend DEV; Prumo: CSS/DEVconfig/guarda; Vigia: revisão independente; Farol: canônicos/recibo/mapa/nota. Fonte congelada, bloco local encerrado, somente exercício fictício próprio aberto. Aguardar demanda/decisão ou prova segura SQL; sem retomada automática. FINAL14 local preservada/aprovada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos. Históricos/IDs/checksums preservados, sem repetir backend/FINAL14 ou tarefas reconciliadas.


## D31 — frontend funcional autorizado por Lucas em 08/10/2026 (registro intermediário preservado; resultado atual acima)

**D31-DEV01 — incremento expresso de Lucas; integração real BLOQUEADA em G01:** “ja deveria juntar o frotend modo run dev com backend e sql server _DEV sim para ser possivel testar enquanto esta sendo feito as atualizacoes”. Autorizado preparar desenvolvimento integrado frontend run dev + backend + SQL Server exclusivamente WMS_DEV, identidade própria WMSDEV restrita. Prumo precisa comprovar resolução segura atual do incidente D29 antes da nova guarda mínima real de alvo/identidade/TLS/permissões/catálogo/histórico. A autorização, listener ativo e provas antigas não comprovam recuperação. [Guarda atual](../frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json), SHA256 9135BC92A2C1BF3DF012D47B8EA19E196B835D0A153F840F10D3AA34F15C9005: guardaRealAprovada=false; nenhuma prova atual segura localizada nas fontes direcionadas; canal WMSDEV existente/ACL compatível apenas por metadados. Zero abertura SQL/SELECT/HTTP/backend/fixtures neste recorte. Cedro prepara exclusivamente perfil/guia backend DEV; Lume prepara frontend real/proxy/baseURL/run dev sem segredos; nenhum backend com SQL ou FE→API com SQL será iniciado enquanto bloqueado. Após pré-requisitos comprovados: somente processos próprios, consultas primeiro; escritas fictícias pertinentes somente HTTP/API rastreável. Sem DDL/Flyway pendente/mutação automática do schema, SQL de fixtures/reset/DELETE, PROD/sa na aplicação, alteração de servidor/acessos/runtime compartilhado, kill/restart existentes ou reabertura FINAL14. Responsável SQL deve fornecer prova atual de recuperação segura; partes frontend independentes continuam. Registro de incorporação: 2026-10-08T21:35:56.890749+00:00; horário do pedido não presumido. [Registro do incremento](../orchestracao/.runtime/frontend-farol-dev01-lucas-20261008.json).

**CSS aplicado — prova Prumo encerrada:** link CSS externo no index, import JS duplicado removido, CSP preservada; 30 capturas/estados DEV e BUILD em 1440/768/390 px, HTTP200 text/css, computed Arial/margin0/header azul, zero violações CSP/erros de console/overflow do documento/API/externos. [Recibo](../frontend/evidencias/frontend-prumo-css-lucas-20261008.json), SHA256 2453DC770565379D01BDC21635F134080A578767418739C9200757010FA5F608. Diagnóstico reproduziu HTML cru no DEV devido à injeção CSS inline bloqueada; build anterior já estilizado. Não identifica a URL vista por Lucas. O defeito de navegação do skip link é separado, corrigido por Lume e ainda aguarda reconferência; CSS aprovado na fatia não aceita teclado global nem a entrega inteira.

**Fase atual: implementação da base frontend FE01–FE13 em andamento**, por demanda expressa FIM_FRONTEND_LUCAS_20261008. React com TypeScript em frontend/, visual básico preto/branco/azul, sem rodada estética. [Demanda integral](../orchestracao/.runtime/frontend-demanda-lucas-20261008.txt) e [distribuição/limites](../orchestracao/.runtime/frontend-farol-escopo-lucas-20261008.json). Posse atual distribuída por autorização expressa de Lucas: Cedro assume somente frontend/src/modules/regularizacao/ e testes próprios FE12; Prumo, estilos/entrypoint (src/styles/, styles.css, main.tsx, index.html) e prova do CSS; Lume mantém os demais arquivos frontend, contratos/cliente/workflow/agregadores e integração/checks finais. Vigia revisa em leitura fatias estabilizadas e artefatos próprios, em paralelo. Farol mantém canônicos, decisões, continuidade, mapa e recibos centrais. A abertura inicial não havia enviado trabalho a Cedro/Prumo; a distribuição paralela e o incremento DEV01 acima são os estados atuais.

**Prioridade expressa de Lucas — CSS aplicado e execução paralela:** “nenhum css de fato foi aplicado sendo que tem css, eu falei pra aplicar so uma base fina, e nao deixar html puro, e precisamos adiantar rapido, coloque outros agents para adiantar outras coisas”. Prumo confronta imports/entrypoint/asset e DOM/computed styles em navegação real desktop/coletor; corrige a base fina preto/branco/azul, legibilidade/espaçamento/alinhamento/controles/tabelas/feedback/foco/responsividade, sem refinamento estético. Cedro completa FE12; Lume os demais recortes; Vigia reconfirma fatias estáveis sem esperar fecho global. Handoff imutável F49F86454F8852DDF334C71A04E4EBD624968B28892D62FAAA70883711E2220B; formatter anterior encerrado, donos liberados sem concorrência. [Distribuição e fronteiras](../orchestracao/.runtime/frontend-farol-distribuicao-paralela-lucas-20261008.json).

**Escopo concreto:** telas administrativas e fluxos de coletor segundo contratos atuais; navegação, formulários, consultas/ações, carga/vazio/erro/sucesso, prevenção de envio repetido, acessibilidade/teclado e responsividade. Cliente HTTP central e DTOs precisos; backend continua responsável por regras, disponibilidade, permissões e cálculo. Configuração externa sem segredos; nenhuma rota ou autenticação inventada. Provedor JWT/CORS real e equipamentos permanecem dependências próprias. Referências reais inventariadas: um PDF de especificação, dois DOCX e README; originais preservados.

**Validação autorizada:** instalação/build/tipagem/lint/testes e exercício do frontend local próprio. Respostas fictícias explicitamente identificadas para ensaio isolado não comprovam integração real. Fonte, comandos/logs/hashes, cobertura concreta BE→FE e revisão independente serão consolidados; FE13 real com API/dispositivos permanece pendente enquanto impedida. Não concluir pela existência de scaffold, formulário genérico ou contagem de testes.

**Preservação e limites:** FINAL14 backend LOCAL concluída/revisada/aprovada, sem faltante local executável conhecido; não repetir tarefas/testes/build backend. D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos conservados. D29/317nativas/6equipamento/45externas/47históricas/corte temporal e históricos/IDs/checksums preservados. O limite inicial sem SQL/HTTPDEV foi incrementado exclusivamente por D31-DEV01 acima, condicionado à resolução segura e nova guarda; a integração real permanece bloqueada. Nenhuma migration/DDL/PROD/sa/runtime compartilhado/restart/kill existentes/fiscalreal/dispositivos/publicação/commit/push/ETL/rotina/callback ou ask Hermes autorizado. Processos próprios FE e, após guardas, BE DEV permitidos.

**Critério de fecho D31:** base funcional entregue e exercitada; contratos/documentação coerentes; parecer independente sobre fonte atual; limitações reais delimitadas. Recibo final somente [JSON](../orchestracao/.runtime/frontend-resultado-lucas-20261008.json) e [MD](../orchestracao/.runtime/frontend-resultado-lucas-20261008.md). Registro iniciado em 2026-10-08T17:55:54.3120267Z. Próximo passo: Lume implementa/valida, Vigia revisa fonte estável; Farol consolida evidências e encerra aguardando decisão de Lucas.

**Revisão do MARCO01 em 08/10/2026:** tipagem/lint/211 testes/build/oito jornadas de navegador passaram também em cópia independente, mas três focais novos de Vigia reproduziram defeitos: FE-VIG-002 (vínculo de pedido e reserva/derivados), FE-VIG-003 (constraints de elementos primitivos de listas) e FE-VIG-004 (null em resposta primitiva obrigatória). O MARCO01 não recebeu aceite local. Lume preserva o marco e corrige exclusivamente frontend/ para novo freeze/provas/MARCO02; Vigia reconfirma as correções antes do fecho. Organização estrutural e recortes anteriores foram tratados, sem compensar os três defeitos. O mapa em execução é intermediário, sem aceite de comportamento; será reconciliado com a fonte final. FINAL14 permanece intocada.

**Suficiência das jornadas locais (revisão 8):** além dos três defeitos, o parecer preserva parcelas operacionais locais ainda não demonstradas em FE04/05/06/08/09/10/11/12, especialmente retirada/retorno/devolução em FE10. Lume recebeu complemento focal da mesma demanda para resolver percursos essenciais e provar comandos, referências, revisões e feedback; Vigia delimita suficiência em artefatos próprios. Formulários, mapa de 161 rotas e mocks isolados não aceitam passos ausentes. Integração real permanece requisito separado. [Confronto e encaminhamentos](../orchestracao/.runtime/frontend-farol-suficiencia-jornadas-20261008.json).

**Correção expressa de Lucas nesta mesma demanda (registro 2026-10-08T18:44:36.079090+00:00):** “estou achando o codigo muito mal organizado, cuidado”. Organização, legibilidade, acoplamento e testabilidade passam a critério de entrega: separar telas/módulos, componentes, contratos/HTTP, hooks e estilos por responsabilidade concreta; corrigir concentração, duplicação e abstrações sem uso, preservando comportamento. Lume corrige; Vigia revisa explicitamente a organização na fonte estável; verificar frontend após as correções. Sem rodada estética, missão duplicada ou ampliação dos limites. Base não será declarada pronta com desorganização material conhecida. Inspeção e snapshots em [D31-ORG01](../orchestracao/.runtime/frontend-farol-organizacao-20261008.json).

## Histórico anterior preservado — FINAL14 e demais fases

A demanda D31 inicia frontend expressamente; textos antigos “frontend não iniciado/aguarda demanda” conservam sua data e não descrevem mais esta nova fase. A aprovação e os limites backend históricos permanecem inalterados.

**D30 — verificação local integral backend/documentação/contratos, demanda Lucas07/10/2026:** em andamento BE01–BE16. Frontend não iniciado; FE01–FE13 recebem somente ligações documentais. [Escopo e dez critérios](../orchestracao/.runtime/d30-farol-escopo-e-distribuicao.json), [matriz atual](../orchestracao/.runtime/d30-matriz-aceite-local.json) e [andamento](../orchestracao/.runtime/d30-farol-andamento.json). Aceite integral somente com zero critério local pendente, sem percentual por número de testes.

**Produto integral de disposição recebido:** [Lumev10](../orchestracao/.runtime/d30-lume-revisao-integral-v10.json) preserva1131IDs,727/706,1373/192,161rotas,48/39 e45/47. Cada linha possui definição/usos/oráculo/prova relacionada ou limite/faltante; não aceite de comportamento por estrutura, URI, chamada, annotation ou nome. [Fila de encaminhamentos](../orchestracao/.runtime/d30-farol-fila-integral-lume-v09.json) conserva51qualificações individuais, sem limitar o conjunto. Cedro confronta equivalentes e completa faltantes locais; Vigia revisa assertivas/recibos. Reserva no cálculo, DUN contextual, marcos encerradaEm, hash decimal e compromissos/rollback do encerramento continuam com disposição específica.

**Provas revisadas e versões:** [Vigia01/02](../orchestracao/.runtime/d30-vigia-parecer-focal01-02.json), [03–05](../orchestracao/.runtime/d30-vigia-parecer-predicados-focais03-05.json), [Prumo/SQM/06–08/header](../orchestracao/.runtime/d30-vigia-parecer-prumo-focais06-08-sqm-header.json) e [Long12/13](../orchestracao/.runtime/d30-vigia-coercao-long-focais12-13-conferencias.json) favoráveis somente aos predicados delimitados, sem somar execuções ou fechar pais. [Integer14/15 autor](../backend/evidencias/d30-cedro-coercao-integer-recibo.json) conserva red4/4/0/0 e green4/0/0/0 dos quatro negativos; [Parecer INTEGER independente](../orchestracao/.runtime/d30-vigia-coercao-integer-focais14-15-conferencias.json) favorável somente aos quatro negativos; positivos/nulos/exatos/overflow/Long/Decimal e regressão completa pendentes na última fonte. Módulo correnteDF7D Long/Integer distingue versão histórica5C630 do green13;259main atuais,258anteriores preservadas nas capturas. Header64KB é correção local de transporte, domínio500/Long/sub200/900s intacto. Prumo317 estático e SQM7/34zeroJDBC revisados com limites; A11 valor lexical suficiente na captura5F230, textoUTF8autorD01D/F183 qualificado separadamente em [Vigia](../orchestracao/.runtime/d30-vigia-a11-utf8-versao-parecer.json), sem retroagir hashes ou repetir confronto. Não materialização/ORDERBY/locks reais. A associação futura RN09.01/V40.02/AC09.06 recebeu [parecer Vigia17–20](../orchestracao/.runtime/d30-vigia-associacao-focais17-20-parecer.json) favorável somente aos vetores do green20, com fixture/red/parcial separados; rollback tardio, audit/op por assertiva dedicada, SKU/dados/nulos e FINAL permanecem pendentes. FatoServico e seu service mudaram por caminho/SHA; preservação das258 fontes anteriores é histórica, sem certificar todasasatuais iguais.

**Ainda necessário para concluir LOCAL:** resolver todos faltantes pertinentes nas706/727/1373/161/classes/variáveis, incluindo fonte nova no inventário de símbolos após última mudança. A08 temporal lossless, A10 schemaH2 e três ramosv09 têm primeira execução somente na regressãoFINAL;65GETs precisam observação guardada por rota/cenário, reaproveitando jornadas, sem duplicar fixtures/focais. Depois das últimasfontes: finalCOMPLETO seguro com fonte/snapshot/classes/JAR/XML/log da mesma execução, mapa produtivo por caminho/SHA, freeze e revisão independente de TODO pacote. [Disposição documental Farol](../orchestracao/.runtime/d30-farol-disposicoes-documentais-v09.json) confirma fontes do vínculo futuro adicional/peso e explicita lacunas locais; não cria conversão ou saída fictícia. [Contrato derivado atualizado](../orchestracao/.runtime/d30-farol-adenda-contratos-associacao-inteiros-v01.json) separa associação/replays/consulta atual e negativos Long/Integer, preservando prefixo do documento35. [Adenda Lumev05](../orchestracao/.runtime/d30-lume-adenda-v10-vigia-assertivas-v05.json) preserva as disposições anteriores e individualiza equivalentes/faltantes de recebimento, unitização e saída; [gate de cobertura](../orchestracao/.runtime/d30-farol-gate-final-disposicoes-v10-v01.json) e [faltantes por atributo](../orchestracao/.runtime/d30-farol-gate-final-atributos-faltantes-v10.json) mantêm IDs/pointers em batches sem aceite estrutural. limites de recebimento, unitização e saída continuam na mesma fila, com comandos válidos e conservação na recusa, sem repetir positivos já provados. Ausência de prova realSQL não impede predicados locais. Nenhum impedimento concreto de execuçãoLOCAL identificado; trabalho restante não é aceite nem bloqueio externo.

**Autoridade e preservação:** Cedro único escritor/executor backend; Farol docs/canônicos; Lume/Prumo/Vigia leitura e próprios artefatos. HTTPLOCAL+H2 efêmero fictício e mocks autorizados com configuração/URL/perfis e guardas fail-closed antes dos casos. SQLIT somente compilação. SQLServer/HTTPDEV/sondas/banco real,PROD,sa real,servidor/runtime/acessos,DDL/migrations reais,restart/kill/reset/limpeza,publicação/commit/push,ETL e rotinas continuam suspensos. D29 e históricos preservados; byte-identidade das caudas apenas na linhagem temporal conferida em [Vigia](../orchestracao/.runtime/d30-vigia-canonicos-vinculos-temporais.json), sem afirmar todosbytes imutáveis desde baseline inicial. Dependências nativas/equipamento/externos conservam requisitos e encaminhamento próprio; nenhuma simulação os aceita. FE aguarda demanda posterior.

**Delimitação atual de defaults e provas:** a [adenda dos defaults de Avaria](../orchestracao/.runtime/d30-farol-adenda-contratos-defaults-avaria-v01.json) qualifica somente os dois construtores compactos Java NULL→false; binding HTTP, permissões, replay e efeitos dos callers continuam com prova própria pendente. O [parecer de oráculos da primeira FINAL](../orchestracao/.runtime/d30-vigia-oraculos-final-inteiros-get-v01.json) exige capturas H2 pertinentes por rota/caso/contexto, handler real e fisicoIgual=true, ligadas à fonte/XML da mesma execução; XML verde sozinho não atesta as fotografias. Captura ausente, false/null ou concorrente sem correspondente permanece pendente. Contextos sem banco conservam prova própria. A [revisão das assertivas de componentes](../orchestracao/.runtime/d30-vigia-componentes-v13-v15-limites-assertivas-v01.json) mantém validação aninhada, causalidade do400 e transformações por caller nos mesmos IDs, sem aceite por roundtrip sintético.

**Consolidação de disposições,08/10/2026:** [Vigia:706locais/727IDs](../orchestracao/.runtime/d30-vigia-cobertura-facetas727-local706-v01.json), [1373atributos](../orchestracao/.runtime/d30-vigia-cobertura-atributosDTO1373-v01.json) e [161contratos](../orchestracao/.runtime/d30-vigia-cobertura-contratosHTTP161-v01.json) têm disposições individuais conferidas; fontes/corpos relacionados e faltantes preservados, sem equivalência comportamental por contagem. [Lumev07](../orchestracao/.runtime/d30-lume-adenda-v10-delta-produtivo-v07.json) e [Vigia32](../orchestracao/.runtime/d30-vigia-delta-produtivo-v07-simbolos32-v01.json) delimitam32símbolos selecionados, não total corrente8716; onze do módulo faltavam no índice histórico. [Posição v08](../orchestracao/.runtime/d30-lume-adenda-v10-posicao-por-id-v08.json) qualifica a restrição na mesma posição, sem exclusividade global deSKU/cliente; [sobrecargas v09](../orchestracao/.runtime/d30-lume-adenda-v10-sobrecargas-v09.json) corrige cinco vínculos derivados: Indicador146 conserva fuso e fixa produtoId=null, controller chama157 com ambos. [Consolidação v32](../orchestracao/.runtime/d30-farol-consolidacao-contratos-guardas-v32.json) importa planos Cedro34–37 e a leitura estática C07 Prumo: receita declarativa não é executor/FINAL. format31/compile31 exit0 informado pelo autor; nenhum JUnit novo após focal20. Ainda pendem disposições correntes completas de classes/variáveis, complementos finitos, último código/símbolos, freeze/executor seguro/primeiraFINALcompleta e revisão geral. Fragmentos graph v2 são históricos após esta atualização dos cabeçalhos; integração e fonte corrente mantêm gate próprio C09. C01–C10 abertos; SQLServer/FE suspensos.

**Regressão e partições,08/10/2026:** a [consolidação FINAL02](../orchestracao/.runtime/d30-farol-consolidacao-final02-particoes-v34.json) confere os50XML/artefatos preservados e87reds porcaso:695/67/20/0,608green,exit1 e0JAR somente nessa execução. FINAL01 anterior falhou em formato semJUnit/JAR; não somar tentativas nem inferir87defeitos produtivos. Cedro trata causas compartilhadas, fixtures/oráculos/instrumentação e candidatos materiais antes da próxima regressão completa após últimas fontes. [Lume autoria exclusiva v02](../orchestracao/.runtime/d30-lume-particao-semantica-v02.json) e [Prumo autoria exclusiva v1](../orchestracao/.runtime/d30-prumo-particao-semantica-v1.json) foram recebidas comIDs/pointers/SHA preservados; suficiência semântica/provas e FINAL continuam em revisão independente. [Parecer de suficiência Cedro](../orchestracao/.runtime/d30-vigia-cedro-indice-semantico-linhagens-v01.json) exige substituir disposições genéricas por transformação/invariante/caller/assertiva/oráculo/versão ou faltante concreto. [Ligações por dono/caller](../orchestracao/.runtime/d30-farol-ligacoes-prumo-v36-distribuicao.json) preservam356grupos sem duplicação, sem testes novos automáticos. Histórico8716 permanece; inventárioV06 corrente8744 e delta28 são declarações, não requisitos/testes/aceite. O [mapa documental v34](../orchestracao/.runtime/d30-farol-graph-documental-integrado-v34.json) foi integrado combackup/guardas e posse liberadaCedro; seusSHAs são temporais anteriores a este registro. C09 exige fonte/mapa final após últimas mudanças. Freeze/regressão/revisão geral não aguardam mera redação; achados reais permanecem na fila. C01–C10/D30 abertos; SQLServer/FE/históricos e limites mantidos.

**FINAL04 e faltantes locais comprovados,08/10/2026:** a [conferência FINAL04](../orchestracao/.runtime/d30-farol-final04-pacote-conferencia.json) verifica696/0/0/0,50XML,1JAR e7095artefatosSHA da mesma execução,exit0; FINAL01/02/03 e seusreds continuam históricos separados, sem soma. Isso comprova regressão/proveniência deste snapshot, não aceite integral por número de testes. A [conferência GET65](../orchestracao/.runtime/d30-farol-get-final04-conferencia-v42.json) confronta rota/handler/caso/XML/hash das fotos64 e conserva18faltantes positivos:47rotas têm captura2xx pertinente, sem aceitar403/handlerNULL/concorrentes sem correspondente. Cedro complementa somente faltantes na fila vigente; fonte nova exige nova correspondência e regressão final após últimas mudanças. As [ligações Lume/Prumo](../orchestracao/.runtime/d30-farol-ligacoes-lume-prumo-v40.json) têm33grupos originais sem omissão/duplicação, com provas delimitadas ou falta concreta; instantes de cadastro, strings fiscais/cnull e limites individuais provider/declarações sem caller permanecem em confronto de equivalentes. [Plano de cadastros](../backend/evidencias/d30-cedro-transformacoes-cadastros-confronto-plano-v52.json) identifica candidato fiscal positivo real, sem generalizarcnull/tempo. Os [exemplos semânticos Cedro](../backend/evidencias/d30-cedro-semantica-autoral-v49.json) não concluem57classes/323grupos por contagem; autoria específica e parecer independente integral ainda necessários. O [mapa documental v41](../orchestracao/.runtime/d30-farol-graph-documental-integrado-v41.json) preserva ASTv04/backup e oito fontes temporais anteriores a este registro; posse liberadaCedro para AST após últimas fontes. Não repetir verdes por mera redação. C01–C10/D30 continuam abertos; faltantes locais são trabalho autorizado, não impedimentoSQL. IncidenteD29/guardaSQLServer não resolvidos nem reexecutados; FE não iniciado.

**FINAL05 e deltas por item,08/10/2026:** a [conferencia FINAL05](../orchestracao/.runtime/d30-farol-final05-pacote-conferencia.json) registra700/2/0/0,51XML,exit1 e0JAR na execucao encerrada naturalmente; FINAL04696/0/0/0/JAR permanece prova de seu snapshot, sem soma ou heranca para fontes posteriores. Os doisreds foram preservados por caso/XML e [importados com a prova fiscal Lume](../orchestracao/.runtime/d30-farol-lume-final04-final05-deltas-v44.json): consulta da importacao retorna enderecos vazios versus expectativa da confirmacao; alteracao fisica no mesmo instante aplica ramo monotono de um microssegundo. Cedro confronta contrato/oraculo antes de corrigir, preservando main se a falha for expectativa; isso nao infere bug geral nem autoriza tolerancia temporal. A [consolidacao Prumo/Vigia](../orchestracao/.runtime/d30-farol-prumo-vigia-final04-registros-v45.json) conserva29grupos e33pareceres porID; MapDecimalMAX/associacao/rollback simulado/captura temporal tipada sao suficientes somente nos vetores revisados FINAL04. NULL/default/omissao/limites nao herdam o positivo fiscal11; identidades, versoes otimistas e repositories conservam faltante concreto de ligacao/caller quando aplicavel. Autoria Cedro57classes/323grupos, provas GET65/complementos atuais, ultima regressao verde comJAR/freeze/mapa e revisao independente geral continuam pendentes. Nao repetir focais verdes por redacao ou criar teste por getter/alias/variavel. C01–C10/pais/D30 abertos; SQLServer/FE/incidenteD29/historicos e limites permanecem.

**Próximo passo D30:** [decisão local revisada e impedimentos](../orchestracao/.runtime/d30-farol-consolidacao-decisao-local-v90.json): bloco LOCAL da fonte14 conferido e aprovado somente nos predicados/canais/oráculos/limites revisados. [Parecer geral Vigia](../orchestracao/.runtime/d30-vigia-final-geral14-parecer-v01.json) conclui C01–C10 na parcela local, sem faltante local concreto conhecido ou novo teste/código necessário. FINAL14:708/0/0/0,exit0,1JAR,fonte/ZIP/classes/log/XML/capturas mesma execução; número de testes não é cobertura. Disposições93e/v117 e GET65/contextos/provider/A08 atuais revisados, AST11/mapa84 na fonte14 e preservação de relações confirmados. D30 geral/pais permanecem abertos:317facetas nativas,6equipamento,45externos/47históricos e corte03:00Z não cumprido; parcelas requeridas impedidas conservadas no denominadorC06/C07/C10, sem aceite porH2/static/mocks nem relabel. SQLD29 sem resolução segura/nova guardaWMS_DEV/WMSDEV/TLS. Próximos encaminhamentos são os donos/IDs da matriz e futura retomada nativa só no escopo seguro autorizado; nenhuma execução atual. Recibo parcial previsto em orchestracao/.runtime/d30-resultado-parcial-final14.json; frontend aguarda demanda posterior. D29/reds01–13/preexistentes preservados, sem soma/backdate/publicação/commit/push/ETL.

Registro iniciado em 03/10/2026. As decisões do responsável, os requisitos da especificação e as propostas de análise têm origens diferentes. Em 05/10/2026 foi recebido o questionário com respostas aos 110 subitens. As interpretações decorrentes estão identificadas e permitem avançar no desenho sem nova rodada extensa de perguntas.



## D29 — cobertura documental/técnica ampla e novas famílias DEV,07/10/2026



**Retomada local expressa de Lucas,07/10/2026:** bloco local concluído e revisado pelo [parecer final Vigia](../orchestracao/.runtime/d29-fecho-local-vigia-final.json), favorável com limites; resultado no [recibo parcial](../orchestracao/.runtime/d29-resultado-parcial-fecho-local.json), sem fecho geral ou sistema completo. [Matriz1131/48/39](../orchestracao/.runtime/d29-fecho-local-lume.json), [síntesePrumo](../orchestracao/.runtime/d29-fecho-local-prumo.json) e [parecer498](../orchestracao/.runtime/d29-fecho-local-vigia-build498.json) consolidados. Os nove novos locais9/0/0/0 foram revisados: seis permutações comR$1,00 (0,33/0,33/0,34) e cortes1/0/32 somente em serviços reais/repos simulados. R$0,01 de RN09.03/CT29-L043-01 e ORDER BY SQL nativo permanecem sem prova específica deste recorte; predicados locais não homologam autorização real,400HTTP,persistência ou rollback. UTF8 Cedro corrigido pelo autor e hashes importados; versões históricas diferentes recebem limite explícito, sem reconstrução. Nenhuma nova sonda/SQL/HTTP/escrita até resolução segura comprovada e nova guarda dentro do escopo autorizado. Preservar fatos/reds, seis aceites anteriores e pais abertos; incidente/preservação final/corte/witness/727equivalências/45externos têm impedimentos e encaminhamentos distintos. Nenhuma repetição do pacote concluído. Guarda futura permanece naD29 já autorizada após comprovação segura; ações fora do escopo exigem autorização própria.



**Incidente SQL prioritário D29,07/10/2026:** Lucas relatou connection timeout. Novos ensaios SQL, chamadas HTTP e escritas de negócio estão suspensos até resolução segura e nova guarda real WMS_DEV/WMSDEV/TLS. A tentativa única de diagnóstico de Prumo já terminou; a retomada atual não autoriza nova sonda, SQL ou HTTP. Cedro completa somente lacunas locais comprovadas; Lume consolida a matriz existente, Prumo os impedimentos em arquivos e Vigia revisa o pacote local. Farol mantém os registros e o recibo delimitado. Sem restart, mudança de servidor/runtime/acessos, kill de transações ou limpeza. Timeout de abertura/prelogin antes de identidade já registrado; serviço/listener não comprovam login, saúde SQL ou autoria. [Controle de suspensão](../orchestracao/.runtime/d29-incidente-suspensao-farol.md). D29 permanece aberta; nenhuma atribuição causal sem prova.



**Diagnóstico do incidente D29,07/10/2026:** sonda única Prumo20:39:17Z falhou em5,12s no prelogin (SQL_-2/native258),pooling=false,zeroSELECT/HTTP e identidade/TLSatuais não confirmados. MSSQLSERVERRunning/PID65088 desde05/10,mesmoPIDD24/listener1433; não prova saúde. Quatro eventos70117:02Z de memória no poolinternal precedem preflightválido19:14:15Z; causa atual/aplicação causadora inconclusivas. Histórico administrativoD24/D26/D27 separado deD29HTTPDEV; nenhum comandoD29 de alteração global encontrado nos registros revisados, sem afirmar inexistência universal. [Recibo do incidente](../orchestracao/.runtime/d29-incidente-diagnostico-farol.md). Escritas/ensaios suspensos até resolução segura e nova guarda. Vigia1131/39 ePRIO06local14/0 revisados porcanal, semaceitepais/DEVporH2; preservação final1895+5tardios e demais limites individuais mantidos. D29 não encerrada.



**Correlação701 D29,07/10/2026:** nos21arquivos HTTP/1491respostas,19fotos/1274intervalos de queries de collectors e85logs WMS examinados, não há resposta, intervalo de consulta publicado ou linha JVM timestampada entre14:02:08 e14:02:20BRT. Isso não exclui atividade/API/SQL/JVM ou consumo de memória e não identifica aplicação causadora. Fonte/checkpoint documental17:02:35Z não prova execução SQL naquele segundo. O processo65088 sem reinício e a configuração atual são critérios distintos: configuração atual permanece não comprovada enquanto a guarda falha. Aplicativo/tela, horário e erro exato do timeout relatado porLucas foram solicitados neste canal e seguem pendentes; sondaPrumo e erroEnsaioD29 não os substituem. [Correlação offline](../orchestracao/.runtime/d29-incidente-correlacao701-farol.json). AdendaVigia57/0 resolveu fonte04versus14/UTF16 sem mudar bytes; nove novosPRIO06 e consolidado14 já revisados. Escritas/ensaios continuam suspensos, sem reexecução ou atribuição causal.



**Aceite delimitado dos seis vínculos D29,07/10/2026:** RN24.02/MULTI01, V33.02/RET01, AC05.01/CAP01, Q-D10-L047-S02/RET02, Q-D10-L059-S02/RET05 e Q-D10-L059-S03/RET03 atendidos somente nos casos DEV enumerados e revisados. Base LumeFINAL19:49:39.577Z/DEFCDF57, parecerVigia23/0/9C42A9F2; nenhuma nova execução. Pais abertos. Saldo porSKU10=8reservado+2bloqueado+0disponível; texto disponível2 original preservado, sem usar como esperado. Revisão local14 posterior preservada. CheckpointCedro029 está na cópia histórica; corrente456 é atualização20:45, não prova de negócio ou posse. [Aceite e fontes](../orchestracao/.runtime/d29-farol-aceite-seis-vinculos.json). Incidente e escritas suspensas permanecem; configuraçãoSQLatual não comprovada, causa/aplicação e relatoLucas pendentes. Sem fecho geral.



**Origem e autorização:** nova demanda expressa de Lucas após fecho D28. Suprir lacunas da validação backend/documentação, popular WMS_DEV com todos os dados fictícios rastreáveis necessários por HTTP/API, coordenar mesmos chats até comprovar o tecnicamente possível ou demonstrar bloqueio concreto. Não limitar à repetição D28 ou ao número de métodos/HTTP200. Matriz completa requisito/regra/cenário/correção -> caso/oráculo -> evidência -> atendido/não atendido/depende de validação externa; preservar IDs RN/I/PR/V/AC e distinguir propostas de decisões.



**Escopo:** recebimento/XML/divergências/quarentena; unidades/transformações/etiqueta documental; capacidade/endereço/movimento/FIFO/avaria; estoque/reserva/pedido integral/retirada parcial/reversões/repetição/perfis/alcance; cargas/contagens/ajustes e financeiro. AC04–08 positivos/zero/limites/recalculo por exemplos de teste e esperado independente, sem aprovação comercial presumida. Concorrência HTTP real com repetições/controles, sem gate artificial apresentado como lock nativo. Investigar observabilidade da identidade atual para SQL300 sem sa/DMV administrativa/grants/acessos novos; se inviável, prova alternativa e permissão faltante precisas. Corrigir defeitos concretos autorizados com red/green/build novo/reexecução/revisão.



**Limites:** confirmar DB_NAME WMS_DEV e WMSDEV restrita antes de teste/população; preservar banco populado/fixtures/históricos, negócio apenas HTTP fictício. Nenhum PROD/DDL/migration/launcherDEV-PROD/sharedruntime/servidor/terceiros/frontend/publicação/commit/push/fiscal externo/cobrança real/reset/DELETE/SQL direto de fixtures/IT vazio/sa/grants. Não alterar relógio/fatos para corte nem inventar estoque real/preços/provedor/aceite humano. Impressora/coletor/operação/fiscal externo/aceite AC/recuperação em destino isolado ficam separados; avançar contratos/testes locais, sem dispositivos/serviços/restauração não autorizados. Simulação não comprova físico/fiscal.



**Coordenação:** Farol central/matriz/fecho; Cedro backend/jornadas/build; Prumo preflight/SELECT/observabilidade permitida; Vigia revisão independente; Lume apenas leitura contratual/documental. Arquivos e guardas no [contrato D29](../orchestracao/.runtime/d29-autorizacao-e-distribuicao.md). Recibo final WMS somente orchestracao/.runtime e resposta neste ask, sem callback Hermes/ETL. D29 em andamento, nova demanda prevalece sobre aguardar do fecho D28; D28 abaixo preservada.



**Achado D29 em tratamento,07/10/2026:** XML XInclude foi aceito201 apesar da recusa400 expressa no contrato24:101, com pedido persistido. Preservar fixture/snapshots/auditorias e corrigir somente parser Java, sem DDL/acessos/limpeza; red/green público, build novo, nova identidade HTTP, SELECT e revisão independente já autorizados. [Proposta anterior à alteração](../backend/evidencias/d29-defeito-xinclude-proposta.md). Não há prova de leitura de arquivo/rede. O genérico SELECT comprova persistência e não anula o oráculo focal vermelho. Previsões financeiras66/76 e recusa por corte08/10 são provas diferentes de aprovação/reabertura; relógio/fatos continuam imutáveis. [Andamento e matriz](../states.md).



**Fecho da correção XInclude D29,07/10/2026:** greenD29E89F4D21 recusou400 com nova identidade no JAR7E22471E…939FD; SELECT134/0/focal101/0 comprova ausência de novos efeitos e preservação do pedido66 vermelho. Vigia revisou943/0 verificações; Farol conferiu somente os próprios dois PIDs/duas portas sem processo/listener em18:40:36UTC. [Achado e provas](../orchestracao/.runtime/d29-achados-farol.json), [parecer independente](../orchestracao/.runtime/d29-vigia-xinclude-green.json). Correção aceita nesse caso, sem aceitar XML inteiro/fiscal/segurança ou backend completo. Reds, demais limites e critérios D29 continuam preservados.



**Retomada da D29 existente autorizada por Lucas,07/10/2026:** queda Farol e recuperação de Prumo/Lume/Vigia com --no-daemon não abrem incremento nem autorizam duplicar operações ainda produzidas pelas sessões originais. Reconciliar por arquivos/instantes/metadados; novos leitores usam arquivos distintos. Nenhum reinício de daemon/Maestri ou mudança de acesso pelo Farol. [Registro](../orchestracao/.runtime/d29-retomada-infra-reconciliada-farol.json).



**Limite material de preservação D29:** collector antigo buscava separações por conjunto_origem_id opcional; as1.895linhas/hashes enumeradas não incluíam separações sem esse conjunto. CincoIDs foram capturados em baseline complementar tardia19:16:17Z, sem retroatividade. Preserve baseline/confrontos antigos; comparação futura desses5IDs comprova somente desde a captura tardia. Esse limite não é apagado por build/SELECT genérico ou contagem maior. [Resumo Prumo](../orchestracao/.runtime/d29-prumo-preservacao-separacoes-baseline-resumo.json).



**Impedimento atual de acesso D29:** tentativas19:22/19:24 retornaram SQL_-2 antes de DB_NAME/login/usuario, zeroqueriesdeDados, sem fallback e com conexões próprias descartadas. Não tratar ausência de captura como zero registros. Serviço/listener ativos19:27 não comprovam identidade, disponibilidade ou causa; preflightFarol19:14 é prova anterior. Só retomar etapa SQL/API após nome real/identidade/restrições/TLS novamente comprovados. Nenhuma mudança de servidor/acesso autorizada por este diagnóstico. [Evidência](../orchestracao/.runtime/d29-prumo-acesso-checkpoint.json).



## D28 — regressão real das correções D26/D27, 07/10/2026



**Autorização expressa de Lucas recebida às 09:03 BRT:** repetir o teste real do backend com banco para todas as correções identificadas, sem frontend, nos mesmos chats WMS Cedro/Prumo/Vigia/Farol. Inventariar correção → caso → evidência nova no artefato atual e executar build/regressão e jornadas HTTP/API/JPA com confronto SQL novo. Histórico D26/D27 é fonte do inventário, nunca substituto desta execução.



**Limites:** SQL exclusivamente WMS_DEV, nome real comprovado antes de testes/fixtures; API somente WMSDEV restrita, nunca sa. Banco populado: preservar fixtures/históricos, sem reset/DELETE/IT de banco vazio/reaplicar V1–V10/launcher DEV→PROD. Sem PROD/DDL/grants/sharedruntime/servidor/terceiros/frontend/publicação/commit/push. Escritas de negócio somente HTTP fictício rastreável. Revisão independente e red/green de defeitos concretos dentro desse escopo; impedimento material fora dele deve ser registrado, sem contornar. Não fabricar corte financeiro/relógio/história para obter aprovação.



**Coordenação e aceite:** Cedro possui backend/ensaios/d28, evidências/backend e correções Java necessárias; Prumo apenas scripts/evidências novos d28-prumo em orchestracao/.runtime; Vigia apenas pareceres d28-vigia em leitura. Complemento de Lucas na mesma demanda: Lume apoia somente leitura dos contratos/cobertura de respostas API, em d28-lume, sem FE/código/execução. Farol mantém registros centrais, matriz correção/caso/evidência/limites e recibo final somente em orchestracao/.runtime. Concluir com casos efetivamente reexecutados, falhas/não cobertos e estado dos terminais; não declarar backend inteiro homologado por subconjunto. Sem callback Hermes/ETL ou reset dos chats.



**Estado inicial:** baseline300 arquivos de backend/src+migrations preservado; [permissões observadas dos cinco WMS](../orchestracao/.runtime/wms-permissoes-verificacao.json), Full Access atual, persistência em novo chat não ensaiada. D28 encerrada no escopo comprovado em BE03–BE15, com witness nativo impedido por SQL300; D27 encerrada abaixo conserva seu aceite histórico.



**Fecho D28,07/10/2026:** build420/0/0/0 novo,17 rodadas/761 HTTP/211 assertivas,29 correções vinculadas e revisão independente favorável com limites. SELECTs atuais1315/0 e focais211/0 sobre as fotos, sem duplicar consultas. Preflight final WMS_DEV/WMSDEV,64/687,V1–V10/311 direitos preservados;300 fontes iguais ao baseline. Quatro reds de roteiro/coletor/precondição preservados e tratados por green/complementos próprios; nenhum defeito novo de negócio demonstrado ou alteração em backend/src/migrations. Concorrência200/409/replay confirmada por HTTP/JVM+gate SELECT/rollback e SQL posterior; witness nativo não observado por recusaSQL300, sem sa/grants. Modo fresco do auxiliar saldo XML removido/bloqueado antes de credenciais/JVM/SQL/HTTP; guarda isolada3/0, sem nova jornada. Processos/listeners próprios zero, mapa AST atualizado, dados/históricos preservados. [Recibo final](../orchestracao/.runtime/d28-resultado-final.json), [matriz29](../orchestracao/.runtime/d28-matriz-correcoes-farol.json), [Vigia](../orchestracao/.runtime/d28-vigia-final.md). Backend total/piloto/comercial/fiscal/provedor/frontend/recuperação/volumetria não homologados. Aguardar nova demanda, sem callbacks Hermes/ETL.



## D27 — fechamento técnico das pendências API/SQL DEV, 06/10/2026



**Origem:** autorização expressa de Lucas após o recibo D26: continuar autonomamente nos mesmos chats WMS até concluir o escopo técnico ou demonstrar impedimento real inevitável. **Estado:** concluído no escopo técnico comprovado em06/10/2026, retomada às21:33 BRT e fecho registrado no recibo. D26/históricos preservados; IDs anteriores não renumerados.



**Escopo autorizado:** BE03–BE15, correção mínima versionada V10 somente WMS_DEV para AJUSTE_ESTOQUE após inspeção administrativa protegida de identidade/TLS/CHECK/Flyway e revisão Vigia; preservar V1–V9/checksums e constraint habilitada/confiável. Reproduzir ajuste/recusas/replay/rollback/efeitos por HTTP e SELECT próprio. Completar jornada financeira não zero com fixture nova válida no corte real; RN22/XML conforme requisitos vigentes, filtros/paginação/perfis/alcance/estados/idempotência/concorrência e demais lacunas técnicas das matrizes D26. Escritas de negócio somente HTTP fictício com WMSDEV; API nunca administrativa. Testes SQL adaptados ao banco populado, sem repetir IT que exige vazio. Build/regressão/JAR final e provas GET/SQL/auditoria por caso; revisão final independente e mapa AST WMS proporcional.



**Limites:** exclusivamente WMS_DEV. Fonte administrativa compartilhada existente somente pela API protegida para inspeção/migration DEV; nunca pelo launcher normal DEV→PROD, nem pela WMSDEV para DDL. Não alterar sa/credenciais/sharedruntime/SQL Server/serviços/instalação/compatibilidade/collation/trust global/outros bancos, nem grants globais ou privilégios da aplicação para observação. Preservar todas fixtures e históricos; nenhum reset/limpeza/repair/baseline/exclusão de dados. A alteração transacional mínima do CHECK aprovada nesta D27 não autoriza remover proteção de integridade. Sem frontend/projeto alheio/fiscal real/NFS-e/publicação/commit/push/rotinas permanentes/ETL/callback Hermes. Propostas AC e homologação operacional/provedor/equipamentos/recuperação/desempenho permanecem identificadas e fora do aceite técnico.



**Coordenação:** Farol mantém AGENTS/states/decisões/continuidade/índice e recibo WMS; Prumo possui migrations/executor DEV/SQL/evidências de database; Cedro possui Java/HTTP/roteiros/matrizes/evidências backend; Vigia somente leitura e pareceres próprios. Não editar arquivos do colega em trabalho. Especialistas devolvem no mesmo ask; Farol aciona os próximos passos internos sem reconfirmação do usuário.



**Progresso21:38 BRT:** [preflight administrativo real preservado](../database/evidencias/d27-preflight-real-20261007T003856112.json) passou em uma tentativa, somente SELECT WMS_DEV/sa, TLS obrigatório/certificado validado, servidor/versão preservados. CHECK habilitado/confiável e expressão OR dos cinco tipos efetivamente visível, histórico SQL V1–V9 igual à D24. Nenhum DDL/DML/grant/PROD; executor/migration ainda preparados para [revisão Vigia](../orchestracao/.runtime/d27-vigia-checkpoint-v10.md). Não confundir esse checkpoint com V10 aplicada ou ajuste HTTP aprovado.



**Aplicação real21:52 BRT:** [parecer independente](../orchestracao/.runtime/d27-vigia-checkpoint-v10.md) favorável ao pacote revisto, 41/41 fixtures e3/3 execuções nativas offline, sete hashes pontuais conferidos. Farol gravou sinal interno conforme a autorização já existente. [Flyway DEV real](../database/evidencias/d27-aplicacao-20261007T005201425.json): quatro etapas exit0, V10 success/checksum−562012523, domínio completo com AJUSTE_ESTOQUE habilitado/confiável,64/687 e V1–V9/histórico/direitos/fixtures preservados. [Conferência Farol](../orchestracao/.runtime/d27-migration-conferida-farol.json). Nenhuma conexão PROD/DML de negócio/grant/sharedruntime/global. Próxima fase D27: JAR/HTTP WMSDEV e provas próprias; migration aplicada não encerra E052/AC13 por si só.



**Conclusão D27:** [resultado42](42-validacao-tecnica-api-sql-dev.md), [recibo WMS](../orchestracao/.runtime/d27-resultado-final.md) e [revisão Vigia favorável](../orchestracao/.runtime/d27-vigia-final.md). Build atual420/0/0/0;726 HTTP novos e248 assertivas, históricos/classificações preservados, sem achado material atual aberto nos incrementos revistos. RN22/XML implementada, zero e dois extremos numéricos corrigidos com red/green e HTTP/SQL posteriores. Financeiro50 aprovado no corte real e previsões54/53,75 separadas; V10 aplicada somente DEV, história V1–V9 imutável,311 direitos preservados. Witness SQL online verdadeiro na rodada final, par200/409/efeito único;21 JARs encerrados/42 portas sem listener e mapa AST WMS atualizado.161 métodos positivos combinados D26/D27 não são161 no JAR atual nem backend total/AC/operacional homologados. Regra permanente Hermes AGENTS:55 preservada. Fixtures/dados/históricos permanecem; nenhum PROD, reset, grant adicional, SQL Server/sharedruntime alterado, frontend/publicação/ETL/callback. Aguardar nova demanda; prompts internos já atendidos não autorizam reexecução.



## D26 — ensaio real da API com persistência DEV, 06/10/2026



Lucas: “então vc vai ter que testar populando o _DEV, e esse teste de banco de dados que quero, usando por api para ver se tudo tá funcionando corretamente”. Autoriza executar integralmente o ensaio HTTP da API atual, com Hibernate validate e dados fictícios rastreáveis exclusivamente WMS_DEV, usando identidade própria WMSDEV. Autoriza provisionamento mínimo proposto na D25, como pré-requisito: conferir existência/identidade antes; não sobrescrever login/usuário de terceiros; CONNECT e SELECT/INSERT/UPDATE apenas objetos/colunas necessários da matriz, SELECT imutável do histórico técnico, sem DELETE/DDL/roles administrativas/PROD/outros bancos. Gerar segredo forte em memória e proteger por usuário/máquina/ACL fora Git; administração compartilhada somente pela API protegida, nunca sa no backend nem senha em chat/arquivo literal/SQL literal/log/argumento/env persistente. Atestar direitos efetivos e recusas antes da API.



Cedro lidera roteiro HTTP, JWT issuer/JWKS RSA efêmeros locais, JAR sqlserver-dev em loopback/porta livre e correções proporcionais com reprodução/regressão. Prumo identidade/credencial/atestado/conexão e leitura escopada da persistência. Vigia revisão independente; Farol registros centrais. Cobrir jornadas existentes do cadastro ao recebimento/unidades/estoque/pedido/reserva/expedição/retorno/cobrança fictícia/contagem/contingência, recusas de segurança/disponibilidade/bloqueios, replay, integralidade/rollback e reserva concorrente determinística. Expected/actual/HTTP e GET/SQL de persistência por caso, sem inventar rotas/campos. Toda escrita de negócio via API; sqlserver-it antes da população se precondições concretas permitirem, sem reset para resolver conflito de vazio/fixtures.



Preservar regra permanente de Hermes AGENTS:55, todos dados/fixtures/históricos/migrations aplicadas/checksums. Conferir alvo real antes de toda fase; sem BAT DEV→PROD, SQL PROD, DROP/TRUNCATE/DELETE/clean/repair/baseline/recriação. Sem alteração de sa/servidor/instalação/versão/global/compatibilidade/collation/firewall/trust global/sharedruntime/terceiros. Proibidos publicação/frontend/emissão/ESL/cobrança real/commit/push/rotinas/ETL/callback Hermes. Segredos/token/chaves nunca evidências. Encerrar só processos de teste criados e conservar fixtures rastreáveis. Mudança de schema aplicada exige proposta de migration separada, não correção silenciosa.



**Refinamento de aceite da mesma D26:** testar como Gestor/Supervisor/Operação por HTTP com autenticação de ensaio e persistência SQL. Inventariar endpoints atuais e cruzar matriz requisito/regra/documento → método/endpoint/perfil → caso positivo/recusa/transição/replay/concorrência → HTTP esperado/obtido → GET/SQL/auditoria. Incluir filtros/paginação/validações/erros/alcance/integridade/rollback e valores fictícios explicáveis. Cada API/regra recebe aprovado, falhou ou não executado com motivo. Sem aprovação total se faltar cobertura real; AC propostas e fiscal/provedor/equipamentos reais não homologados. Não substitui por services/repositories/H2/SELECT/happy path; reproduzir/corrigir/regredir e repetir caso HTTP real.



**Ligação do diagnóstico administrativo de leitura:** a autorização expressa da D25 para fonte compartilhada sa somente diagnóstico de LEITURA continua vigente; D26 acrescenta provisionamento restrito sem revogá-la. A observação temporária de locks usa exclusivamente SELECT administrativo filtrado por WMS_DEV/WMSDEV/rodada/porteira, com origem e encerramento separados. Não representa acesso sa da API/IT, concessão de DMV à aplicação ou escrita de negócio. Readiness e par HTTP 200/409 não comprovam sozinhos sobreposição SQL.



**Status:** em validação; ensaios D26 encerrados com falha aberta. WMSDEV/DPAPI/direitos restritos atestados, build412/0/0/0, SQL IT7/0/0/0 antes de popular.159/160 métodos positivos por HTTP; matrizes RN/AC e1.404 solicitações preservam recusas/tentativas/lacunas. Primeira revisão/INSERT e rollback-only de cálculo pendente corrigidos/reexecutados. SELECT236/0, GET/cálculo4 17/0 e adicional financeiro76/0. Ajuste E052 falhou409 por domínio CHECK da V6 não ampliado na V9; rollback42/0, V10 proposta fora fontes ativas, não aplicada, V1–V9/checksums intactos. Corte UTC real permitiu preparar snapshot20, mas fato anulado/recálculo0 bloquearam aprovação com CALCULO_DESATUALIZADO; nenhuma aprovação não zero inventada. Próxima decisão: migration mínima somente DEV em escopo separado. RN22/entrada XML de saída sem endpoint e demais lacunas da matriz explícitas; backend total não aprovado. [Resultado41](41-ensaio-http-e-persistencia-dev.md). D25 histórica, IDs estáveis.



## D25 — testes atuais do backend, 06/10/2026



Lucas: “corrigi já, vamos testar o backend”. Autoriza build clean verify novo e testes HTTP/serviços/arquitetura/regressão, artefato atual em loopback livre e ensaio JDBC/JPA Hibernate validate/sqlserver-it somente WMS_DEV com dados fictícios identificados, após inspeção de integridade/alvo/catálogo/vazio/identidade própria. D24 comprovou migrations e conexão administrativa; os bloqueios D22/D23 são históricos.



Complemento direto: testes SQL exclusivamente WMS_DEV, recusar PROD/outros/fallback; a edição permanente atribuída a Hermes em [AGENTS.md](../AGENTS.md:55) foi preservada sem refazer patch. Bootstrap DEV → PROD não autoriza testes PROD. Credencial compartilhada sa somente diagnóstico de LEITURA, nunca API/IT. Criação de login/usuário/grants não autorizada; ausência de identidade restrita limita SQL/runtime sem parar build/testes independentes, com proposta administrativa mínima somente WMS_DEV para decisão final. Preservar migrations aplicadas/checksums, fixture persistida do lock, servidor/sharedruntime/terceiros e históricos. JWT efêmero de teste não homologa provedor real. Sem publicação/frontend/fiscal/cobrança/commit/push/rotinas/ETL/callback.



**Status D25:** entrega local e leituras DEV concluídas, revisão independente favorável; ensaio SQL da aplicação bloqueado pela identidade própria restrita ausente. Build final novo 408/0/0/0 em 20 XMLs e JAR HTTP 6/6, JVM própria encerrada. SqlClient/JDBC administrativo somente SELECT em WMS_DEV confirmaram catálogo 1.295/1.295, 64 tabelas vazias/687 colunas e nove migrations iguais à D24. Correções locais TLS/lock/perfil do IT/cookie de sessão; nenhum schema/DDL/DML/grant/SQL PROD. Cedro backend, Prumo leitura/conexão, Vigia revisão, Farol consolidação. [Resultado40](40-testes-backend-e-leitura-sql-dev.md), [parecer](../orchestracao/.runtime/d25-vigia-final.md), [recibo](../orchestracao/.runtime/d25-resultado-final.md). BE03/BE15 permanecem em validação externa; BE16 local verificada.



**Encaminhamento único D25, ainda proposta:** autorizar provisionamento de identidade própria e credencial protegida somente WMS_DEV, com CONNECT e direitos por objetos/colunas da matriz existente; SELECT técnico Flyway sem alteração, sem DELETE/DDL/roles administrativas/PROD/outros bancos. Atestar direitos efetivos antes de JPA/API/IT. A demanda de testes não autoriza executar esse provisionamento. Nenhuma fixture de lock SQL criada; o ensaio futuro deixará registro confirmado sem limpeza automática.



## D24 — runtime compartilhado e execução real, 06/10/2026



Pedido direto do responsável: jamais mudar a versão SQL Server 2022 Standard; alinhar a conexão em projetos/.runtime para novos projetos e, ao final, executar o BAT WMS. Autoriza uso administrativo da credencial SQL existente em fonte compartilhada DPAPI protegida, substituindo a exclusividade WMS D22/D23, sem expor senha. Mantém ordem DEV → PROD, preservação das migrations/checksums, TLS validado, servidor e outros bancos imutáveis; sa não é conta da aplicação. Executado e comprovado no [resultado39](39-conexao-compartilhada-e-sql-real.md). Identidades/permissões da aplicação e homologação continuam separadas.



## Decisões do responsável pelo projeto



| ID | Data | Decisão | Consequência |

| --- | --- | --- | --- |

| D01 | 03/10/2026 | Discutir regras de negócio e arquitetura em linguagem simples, antes de código. | Orientou a fase documental inicial; D11 posteriormente autorizou começar o backend. |

| D02 | 03/10/2026 | Frontend React com TypeScript. | Registrar essa base e preparar a implementação futura. |

| D03 | 03/10/2026 | Backend Java com Spring. | Manter o ecossistema escolhido. |

| D04 | 03/10/2026 | Banco SQL Server já disponível na empresa. | Levantar ambiente e acesso; não substituir o banco escolhido. |

| D05 | 03/10/2026 | MVC convencional com repository, models, DTOs e demais camadas usuais. | Organizar o backend por camadas e separar responsabilidades dos serviços. |

| D06 | 03/10/2026 | Analisar o PDF antes de perguntar; o responsável não saberá responder às regras operacionais. | Consolidar lacunas reais em linguagem simples para o gestor. |

| D07 | 03/10/2026 | Criar o projeto em `C:\Users\suporte\Documents\projetos` e documentar em `docs`. | Criada a pasta `wms-rodogarcia`, nome derivado da especificação. |

| D08 | 03/10/2026 | Trazer todas as lacunas identificadas em perguntas fáceis de responder pelo gestor. | Ampliado o roteiro para 27 temas com 110 subitens, preservando Q01 a Q20 e acrescentando Q21 a Q27. Nenhuma regra de negócio nova foi aprovada por essa revisão. |

| D09 | 05/10/2026 | Usar as informações recebidas e o raciocínio para resolver as pontas restantes. | Formular uma base de trabalho para as ambiguidades. Distinguir resposta literal, interpretação e proposta; não exigir nova resposta para cada detalhe nem inventar dados comerciais. |

| D10 | 05/10/2026 | Consultar AGENTS.md e states.md dos outros projetos e montar os arquivos do WMS, com trilha separada em backend e frontend. | AGENTS.md atualizado e states.md criado na raiz; 15 etapas BE e 13 FE com dependências e critérios. Pedido documental, sem início de implementação. |

| D11 | 05/10/2026 | Ler AGENTS.md e states.md para iniciar o backend, mantendo MVC padrão com repository, models e demais camadas, e escolher uma frente. | Escolhida BE02: base executável local, com contrato técnico inicial de BE01. Autoriza código nesse escopo; não autoriza conexão ao ambiente real, migração ou publicação. |

| D12 | 05/10/2026 | Fazer um bloco maior do backend. | Ampliado para cliente, armazém, produto, embalagem e endereço, com JPA, API, permissões JWT, auditoria e V1 SQL Server preparada. Recortes de BE01/BE03/BE04/BE05; testes locais com H2 e identidade efêmera. Nenhuma conexão/migration no SQL Server ou publicação. |

| D13 | 05/10/2026 | Usar `.properties` em resources e seguir boas práticas de engenharia de software. | BE16 acrescenta padronização de configuração, verificação de ferramentas/formato/camadas e validação nas entradas dos serviços. Convenções e escolhas técnicas no documento 16; permanece MVC convencional. |

| D14 | 05/10/2026 | Preparar no Maestri aberto uma orquestração WMS no mesmo canvas, ao lado do ETL v2, com Hermes próprio; preservar os terminais existentes e não conectar projetos. | Hermes WMS com perfil, memória e sessões próprios, mais cinco agentes WMS com papéis exclusivos. Conexões e nota somente da equipe WMS. Preparação/ativação em OR01 e documento 17; não inicia novo bloco da aplicação nem reconfigura o gateway, a ponte ou o perfil antigo. |

| D15 | 05/10/2026 | Aplicar outro macrobloco do backend. | Escolhido recebimento (BE01/BE06): pedido, notas manual/XML, chegadas, quarentena, estorno e efetivação única da quantidade física conferida. V2 preparada; contrato/limites no documento 18. Unitização e disponibilidade pertencem a BE07/BE08. Não autoriza conexão SQL Server, emissão fiscal ou publicação. |

| D16 | 05/10/2026 | Avançar mais no backend. | Escolhido BE01/BE07: unidades logísticas, identidade permanente, divisão/reagrupamento e dados das etiquetas, mantendo MVC e properties. AC10 continua identificado como proposta; endereçamento/disponibilidade em BE08 e validação de equipamentos em FE06. Autoriza código e testes locais, sem conexão SQL Server ou publicação. |

| D17 | 05/10/2026 | Aplicar o próximo macrobloco proposto, BE08. | Entregues endereçamento, capacidade, movimentos, posições conjuntas, bloqueio/liberação e disponibilidade; consultas iniciais BE14 para FE07/FE08. V4 preparada e 153 testes aprovados; contratos/limites nos documentos 22/23. Mantém MVC/properties e propostas AC04/AC05 identificadas; não autoriza conexão SQL Server, migração, cobrança real ou publicação. |

| D18 | 05/10/2026 | Avançar BE01/BE09 pela orquestração WMS: pedido de saída integral, FIFO, reserva atômica sem vencimento, parcial de pallet, exceção por perfil, saldo reservado e bloqueio por avaria posterior. | Autoriza código, contratos, migration SQL Server somente em arquivos e testes locais isolados. Cedro escreve backend/contrato, Prumo migration/procedimento, Vigia revisa em leitura e Farol consolida os registros. Preservar BE08 e alterações existentes. Exige formatação, clean verify com saída real e atualização Graphify. Proíbe conexão/migration SQL Server real, emissão fiscal, cobrança real, publicação, commit/push, rotinas, alteração dos perfis Hermes e uso do ETL. AC10/AC11 continuam interpretações/propostas identificadas; frontend não iniciado. |

| D19 | 05/10/2026 | Continuar autonomamente até concluir todo o escopo local restante BE01–BE16, sem autorização por macrobloco e sem parar após BE10. | Autoriza backend, contratos, SQL/migrations somente em arquivos, procedimentos e testes isolados fictícios: BE10/BE11, BE05/BE12, BE13, BE14 e integração/preparação BE01/BE03/BE04/BE15. Ordem e arquivos no [documento 26](26-execucao-continua-backend.md). Cedro backend/contratos, Prumo migrations/infra/procedimentos, Vigia leitura e Farol registros centrais; Lume somente apoio de contratos em leitura. Preserva MVC/properties, histórico e propostas AC01–AC16. Exige testes reais por bloco, correção dos achados, clean verify final, Graphify e matriz BE01–BE16. Proíbe SQL Server real, emissão fiscal/NFS-e, cobrança real, publicação, commit/push, rotinas, perfis Hermes e ETL. Dados externos não bloqueiam trabalho independente; registrar item e dono, sem declarar homologação. |



O nome da pasta foi escolhido na execução do pedido com base no nome do projeto. O pacote Java e as ferramentas foram escolhidos tecnicamente em BE02 e registrados no [documento 12](12-base-e-contratos-backend.md); não são escolhas expressas de versão/pacote pelo responsável. D12 acrescentou decisões técnicas descritas no [documento 14](14-cadastros-acesso-e-persistencia.md): JPA/Hibernate, Flyway, esquema `wms`, API Bearer/JWT, normalização/unicidade e campos inicialmente imutáveis. Nome de banco, provedor real de identidade e publicação continuam indefinidos. Essas escolhas não convertem AC01 a AC16 em aprovação do gestor.



### D20 — macrobloco local de database, 06/10/2026



Lucas autorizou, após o fecho local D19, executar BE03/BE15 com auditoria integrada de V1–V9, modelos, consultas e permissões; implementar melhorias locais justificadas, automações, testes isolados fictícios e procedimentos de engenharia de database. Não recriar o modelo nem reabrir Q01–Q27. Migrations congeladas devem ser preservadas; eventual correção exige justificativa, impacto e revisão registrados. Farol mantém states, decisões, continuidade, índice e mapa; Prumo escreve database/infra, Cedro somente a integração backend necessária e Vigia revisa em leitura independente. Resultado pelo ask recebido e registro exclusivo em `orchestracao/.runtime/`, sem callback a Hermes WMS.



O pedido inicial limitava a execução ao pacote local, sem conexão/aplicação SQL Server. Os complementos abaixo ampliaram somente a criação dos bancos exclusivos no alvo informado e a preparação de guia compartilhado; demais limites continuam: sem provisionamento externo, grants reais, migrations/carga PROD, fiscal/cobrança, frontend, publicação, commit/push, rotinas, perfis Hermes ou ETL. H2 e leitura estática não comprovam SQL Server. IDs D01–D19 e BE/FE permanecem estáveis. Execução e evidências no [documento 36](36-database-local-engenharia-e-validacao.md).



**Complemento da mesma D20, 06/10/2026:** Lucas definiu nomes exatos `WMS_DEV` e `WMS_PROD` e autorizou iniciar sua criação somente quando alvo real e acesso autorizado forem inequivocamente confirmados, sem sobrescrever existentes. DEV primeiro; nenhuma migration/carga em PROD nesta rodada. Jamais alterar instalação/configuração global, versão/edição, serviços, instância, collation do servidor, compatibilidade/configurações de bancos existentes, outros bancos/acessos/rotinas; não instalar/atualizar SQL Server. Usar ambiente/padrões já existentes. “System admin, o mesmo que todos os outros estão usando” não identifica host/instância e não autoriza sysadmin à aplicação. Segredos e conexões de outros agentes/projetos não podem ser lidos/usados. Regra durável registrada no AGENTS canônico WMS. Alvo exato ausente na documentação WMS; solicitado somente servidor/instância ou host:porta, sem senha. Demanda local original continua, sem duplicar tarefa ou renumerar IDs.



**Atualização expressa da mesma D20:** Lucas confirmou TCP `127.0.0.1:1433`, autenticação SQL, usuário administrativo `sa`, conexão inicial `master`, e autorizou criar **ambos** os bancos exclusivos após verificar identidade/existência, sem apagar/sobrescrever. DEV primeiro e PROD sem migrations/cargas. Usar defaults existentes, sem inventar collation/compatibilidade ou mudar o servidor/model. A credencial informada no chat não deve ser retransmitida, registrada ou solicitada no chat; somente mecanismo protegido existente/autorizado. Consulta de guias/metadados não sensíveis de outros projetos/runtime e guia compartilhado sem senha em `../.runtime` foram autorizados; `.env`, credenciais, históricos/dados e controles/ponte ETL continuam excluídos. “Features Trigger” não autoriza triggers. Esta atualização resolve o endereço anteriormente ausente; não autoriza acesso administrativo da aplicação.



**Acesso observado por Farol:** em06/10, TCP alcançável e serviço local MSSQLSERVER Running, sem autenticação SQL. Não há credencial WMS disponível nos canais protegidos desta sessão; verificou-se somente presença das variáveis, sem ler/imprimir valores. Não houve conexão SQL, leitura de sys.databases nem CREATE. Bloqueio preciso da execução real: entrada protegida local da credencial administrativa `sa`. Depois dessa entrada, identidade/defaults/TLS/existência devem ser conferidos pelo procedimento, sem pedir novamente o alvo já informado. Evidência sanitizada em `orchestracao/.runtime/d20-acesso-sql-sanitizado.json`.



**Entrega manual expressa, mesma D20:** Lucas mudou a entrega para “cria um .bat no database e eu subo manualmente”. A execução SQL passa ao operador; **o agente não executa SQL nem solicita credencial no chat**. Prumo entrega `database/iniciar-bancos.bat` e auxiliar PowerShell reaproveitando guardas existentes. Duplo clique abre console visível, mostra alvo fixo e os dois nomes, pede senha oculta só em memória, inspeciona identidade, confirma alvo na interação e cria ambos vazios DEV primeiro, sem sobrescrita. Bancos existentes devem ser preservados/conferidos, sem abortar cegamente ou recriar; retomada DEV existente/PROD ausente permitida. Garantir cópia segura por etapa e descarte de SecureString; sem segredo em bat/ps1/env/arquivo/log/argumento, bypass TLS/política, novos installs ou mudanças no ambiente/terceiros. Migrations DEV em entrada manual distinta; nenhuma migration/carga PROD automática. README curto no topo, pontos de entrada claros, histórico/evidências preservados. Testes reais via cmd.exe em modo offline e fixtures/mocks de orquestração, sem DDL real; Vigia revisa, Farol consolida. Suite backend completa não será repetida para mudança somente de launcher/procedimento. Essa atualização substitui a execução real pelo agente; ausência de credencial não impede a entrega do launcher.



**Organização autorizada, mesma D20:** Lucas pediu limpar também a raiz de `database`, mantendo somente README curto e BATs necessários ao operador. Prumo inventaria antes e coordena movimentos após terminar gravações; PS1 em `scripts/`, documentação técnica em `docs/`, SQL/migrations/contratos/evidências em suas pastas próprias. Nenhum arquivo/histórico/evidência será excluído; não ler/mover secrets ou configs privadas. Atualizar caminhos, wrappers, referências Maven/Flyway, links, diagnósticos, testes e guia compartilhado afetados. Registrar de/para e justificativa no manifesto corrente, preservando manifestos históricos e nomes/conteúdo/checksums SQL. Revalidar BATs com cmd.exe offline, fixtures e caminhos após os movimentos; sem SQL ou repetição do build backend integral. Vigia revisa a organização antes do fecho local.



**Delimitação técnica após revisão VIG07:** a política de mínimo privilégio da aplicação permanece sem DDL/administração, mas a guarda implementada só recusa os privilégios enumerados (`sysadmin`, `db_owner`, `db_ddladmin`, CONTROL SERVER/DATABASE, CREATE TABLE e ALTER no schema wms); ela não consulta CREATE SCHEMA nem todas as roles administrativas. Não certifica mínimo privilégio. Para o pacote manual, adotada a correção documental proposta pelo revisor: exigir atestação completa das identidades/permissões efetivas antes de uso operacional/ensaio DEV, incluindo CREATE SCHEMA, db_securityadmin/db_accessadmin e roles administrativas de servidor. Migration precisa dos direitos próprios limitados necessários, separada da aplicação/sa. Nenhuma atestação real executada; limitação do predicado permanece explícita, sem alegar correção de código ou novo build. Aprovação do launcher administrativo não aprova runtime ou grants. [Parecer independente](../orchestracao/.runtime/d20-vigia-revisao-final-2026-10-06.md).



**Fecho local da mesma D20:** entregue [iniciar-bancos.bat](../database/iniciar-bancos.bat) e [README curto](../database/README.md), raiz com somente esses arquivos. Após34 movimentos,489 originais preservados, testes80/21/13/336 aprovados e722 hashes conferidos; V1–V9 iguais ao baseline. Backend396/0/0/0 em19 suítes e404 hashes, sem novo build para launcher. Revisão Vigia favorável ao launcher/organização, VIG07 delimitado conforme parágrafo anterior, sem certificação de privilégios reais. [Resultado36](36-database-local-engenharia-e-validacao.md). Nenhuma conexão SQL/CREATE/migration/carga/GRANT executada pelo agente. Próximo passo pertence ao operador manual; BE03/BE15 permanecem em validação externa nos critérios SQL. Macrobloco local encerrado, sem novo ID ou callback Hermes.



### D21 — Bootstrap e upgrade completos pelo BAT manual



Correção expressa de Lucas em06/10/2026, na mesma entrega: “se eu quiser derrubar toda a database e subir o .bat ele deve subir a versão mais recente de alterações”. O launcher que somente cria bancos vazios não atende ao comportamento final. D20 permanece histórico; D21 livre acrescentada sem renumerar outros IDs.



`database/iniciar-bancos.bat` deve criar bancos WMS ausentes e aplicar toda a sequência versionada vigente; em bancos existentes, preservar dados e aplicar somente migrations pendentes, validando histórico/checksums e schema real. Alvos exatos WMS_DEV/WMS_PROD em127.0.0.1:1433; DEV completo primeiro, qualquer erro DEV impede PROD. Fonte única `database/migrations` descoberta/ordenada pelo Flyway atual; hoje V1–V9, futura V10 sem lista fixa no launcher. Apagar um banco é ação manual de Lucas fora do BAT; este não executa DROP/clean/repair/baseline automático nem presume schema atualizado por arquivos presentes.



**Autorização manual atual:** preparar bootstrap/migrations também PROD no launcher executado pelo próprio usuário, supersedendo o limite D20 somente nesse fluxo. Não autoriza SQL/PROD pelo agente, aplicação publicada, fiscal/cobrança ou mudanças no SQL Server/global/versão/instalação, logins/acessos compartilhados/terceiros. `sa` administrativo local pode criar/migrar ambos; isso não exige criar/grantar identidade de aplicação nem autoriza usar sa no backend. VIG07/atestação completa permanecem requisitos do runtime/ensaio da aplicação, não impedimento do DDL administrativo D21. Senha digitada oculta local, sem senha do chat ou hardcode/arquivo/argumento/log/env persistente; transporte efêmero no processo-filho, descarte e saída sanitizada. TLS compatível com confiança existente, sem bypass silencioso/alteração global.



**Execução local em andamento:** Prumo database/infra/launcher, Cedro somente integração Maven/config necessária, Vigia revisão focal, Farol registros centrais/mapa/recibo. Raiz README/BAT, scripts/docs nas pastas próprias; preserve D20 antes de modificar fontes. Testes cmd.exe offline e fixtures ausentes/existentes/pendentes/checksum/queda parcial/reexecução/V10; SQL real somente operador. Build backend proporcional se config/Java alterar, sem repetir suíte integral por launcher. Sem callback Hermes ou ETL. Entrega final deve ser bootstrap/upgrade completo, não criação vazia.





**Entrega local D21 em06/10/2026:** BAT completo DEV/PROD pronto,41/41 fixtures,672/672 checks,25/25 Maven,675 hashes database e84 backend, cmd.exe0/0/2/2 e parecer favorável. V1–V9 e D20 preservados; nenhum SQL pelo agente. Evidências/limites no [37](37-bootstrap-e-upgrade-manual-database.md); próximo passo do operador, sem novo macrobloco.



### D22 — Launcher automático com credencial WMS protegida



06/10/2026, correção expressa de Lucas ao mesmo launcher D21 após parar em Read-Host. Incremento somente da obtenção de credencial e interação: execução normal de iniciar-bancos.bat faz bootstrap/upgrade completo WMS_DEV depois WMS_PROD sem senha/confirmacão repetida. Mantém inspeção, allowlist, identidade por conexão, TLS, checksums/histórico/catálogo e falha DEV bloqueando PROD. Operador continua responsável pela execução SQL; agente não conecta/executa SQL ou altera servidor/sa/terceiros. D21 e seus testes/manifestos permanecem históricos, sem duplicação ou renumeração.



Primeiro conferir código/guias públicos e metadados de organização já usados, nunca config.local/.env/credenciais privadas. Padrão verificado da Avaliação: PSCredential Export/Import-Clixml DPAPI para mesma conta Windows/máquina, fora Git e ACL restrita. WMS usa armazenamento próprio em LOCALAPPDATA/Rodogarcia/WMS/database-runner, sem herdar credenciais/configuração/trust/controles alheios. Auxiliar configurar-credencial.bat solicita senha oculta UMA VEZ ao operador local; arquivo contém somente representação protegida, owner/ACL por SID corrente e SYSTEM, não senha literal. Segredo nunca chat/argumento/log/texto/env persistente; leitura automática retorna SecureString, mantendo transporte efêmero do filho D21 e descarte por etapa.



Ausência/erro de canal protegido deve interromper rapidamente o BAT normal e explicar configurar-credencial.bat, sem pergunta de senha nem promessa de funcionamento sem provisionamento. Metadados iniciais mostraram diretório WMS ausente; nenhum segredo lido/provisionado pelo agente. Raiz README/BATs apenas. Prumo implementa database/infra, Vigia revisão focal, Farol centrais/mapa/recibo; backend inalterado, sem suíte integral. Testes cmd.exe offline e fixtures DPAPI fictícias isoladas, ausência/usuário errado/ACL/erro/retorno sem prompts. [Entrega38](38-launcher-automatico-e-credencial-protegida.md). Sem callback Hermes/ETL ou novo macrobloco.





**Fecho local D22:** 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão independente Vigia favorável. Launcher automático pronto; credencial real AUSENTE por metadados, configuração única e SQL real pelo operador pendentes. Nenhum segredo real/SQL pelo agente, alteração backend/SQL congelado ou servidor global. [Resultado38](38-launcher-automatico-e-credencial-protegida.md).





### D23 — Execução real autorizada; acesso automático bloqueado



06/10/2026, atualização urgente de Lucas ao mesmo BE03/BE15 e launcher D21/D22. Autoriza agente a executar bootstrap/upgrade real WMS_DEV → WMS_PROD no alvo fixo existente, sem pedir senha/configurador, por identidade Windows corrente e direitos existentes ou fonte protegida exclusiva WMS já autorizada. Sem senha do chat, novos grants/logins, trust global/bypass, instalação ou alteração do servidor/terceiros; mesmos IDs e migrations preservados. Supersede somente o limite anterior de execução SQL pelo operador.



Resultado real: SqlClient Windows encrypt=true/trustServerCertificate=false falhou NativeErrorCode/SQL -2146893019, cadeia TLS não confiável, antes de comprovar login. Farol verificou serviço SQL Microsoft assinado, PID65088/listener1433/criação iguais antes/depois; coletou somente certificado público recusando TLS antes LOGIN7, sem credencial. Pin exclusivo WMS fora Git, ACL usuário/SYSTEM, sem confiança global. sqlcmd Go existente -E/-Ntrue/-J então recusou logon Windows ROD-SRVW-001\suporte. Não inferir permissões/sysadmin ou causa adicional do logon; código SQL numérico não exposto nesse retorno. Fonte protegida WMS AUSENTE por metadados, sem ler conteúdo.



**Bloqueado na execução real:** zero DDL, nenhuma migration/carga ou leitura autenticada de sys.databases/histórico/schema; WMS_DEV/WMS_PROD não verificados. BAT normal não executado pelo agente nesta fase porque não há acesso válido, conforme condição do pedido. Única ação indispensável: disponibilizar autenticação SQL válida já autorizada em canal protegido exclusivo WMS, fora do chat; não criar grants ou alterar login para contornar. [Evidência inicial TLS](../orchestracao/.runtime/d23-probe-windows-tls.json), [pin verificado](../orchestracao/.runtime/d23-certificado-publico-verificado.json), [recusa após pin](../orchestracao/.runtime/d23-probe-pin-windows.json) e [resultado WMS](../orchestracao/.runtime/d23-resultado-final.md). D22 é histórico local, não aceite da execução real. Nenhuma nova rodada de grafo/manifestos ou callback Hermes/ETL.



**Continuação focal D23, 06/10/2026:** padrão de autenticação dos runners públicos comparado; fonte administrativa SQL compartilhada não localizada/documentada nos guias/metadados autorizados de ../.runtime. 33/33 booleanos de presença dos canais consultados ausentes; configs de referência existem sem leitura do conteúdo, DPAPI WMS ausente. O caminho fixo D22 não recebe a configuração como os demais. Menor integração: somente aquisição de credencial no launcher existente, após fonte administrativa protegida explicitamente disponibilizada ao WMS; sem novo configurador ou credencial alheia. Nenhuma conexão nova/DDL; servidor indisponível não comprovado. [Comparação e resultado](../orchestracao/.runtime/d23-comparacao-fontes-autenticacao.md).



## Base funcional



A especificação versão 1.0 é a fonte funcional inicial. Os requisitos RN01 a RN30 estão em [regras de negócio](03-regras-de-negocio.md), com seção e página. As interpretações I01 a I07 estão identificadas no mesmo arquivo. O Word recebido em 05/10 complementa essa base; o resumo está em [respostas recebidas](10-respostas-recebidas-2026-10-05.md) e as conciliações em [regras consolidadas](11-alinhamentos-apos-respostas.md).



## Propostas de análise



| ID | Proposta | Situação |

| --- | --- | --- |

| P01 | Manter uma aplicação backend e responsabilidades de negócio separadas dentro das camadas convencionais. | Proposta compatível com a estrutura solicitada; implantação ainda a definir. |

| P02 | Centralizar alterações do estoque, reserva e localização nos serviços responsáveis pelo estoque. | Proposta documentada. |

| P03 | Separar situação do pedido, condição da mercadoria, reserva, localização, fiscal e cobrança. | Proposta documentada; transições ainda serão detalhadas. |

| P04 | Aplicar proteções de concorrência, repetição e registro conjunto de alterações relacionadas. | Proposta de proteção; mecanismos técnicos ainda não escolhidos. |

| P05 | Preservar origem de quantidades, histórico e correções rastreáveis. | Proposta documentada. |

| P06 | Validar integração fiscal cedo e registrar fatos cobrados desde o primeiro piloto. | Proposta que antecipa dependências da sequência do PDF. |

| P07 | Planejar acesso, recuperação, confirmação nos coletores e piloto controlado desde o início. | Proposta documentada; responsáveis e parâmetros pendentes. |



As proteções PR01 a PR11 detalham essas propostas. Não há aprovação tácita por ausência de comentários.



## Situação dos temas após as respostas



Todos os temas receberam respostas. A coluna de tratamento aponta a regra explícita ou a interpretação/proposta que completa o funcionamento. Receber uma resposta não equivale a comprovar preços, ambiente ou desempenho real.



| ID | Tema | Resultado e tratamento | Detalhamento |

| --- | --- | --- | --- |

| Q01 | Dados da mercadoria | Bobinas; peso da nota; SKU do cliente; aviso de validade. Parâmetros por cadastro. | AC10, AC16 |

| Q02 | Divisão e reagrupamento | Parcial do pallet permitido; mesmo SKU/lote/data para reunir. Tratamento de ID e origem proposto. | AC10 |

| Q03 | Entregas em partes | Várias chegadas e notas por pedido; uma nota em um pedido; efetivação única. | AC03 |

| Q04 | Divergências e avaria | Carga em quarentena; tratativa com cliente; supervisor libera. Limite por pedido proposto. | AC03, AC08 |

| Q05 | Triagem | Não atende saída; disponibilidade após liberação e endereçamento. | AC03 |

| Q06 | Posições | Uma unidade por posição; duas para unidade grande. Conciliação com Q14 proposta. | AC05 |

| Q07 | Saída incompleta | Pedido atendido inteiro; mudança exige cancelar e recriar. | AC10 |

| Q08 | FIFO e exceções | Nota específica permite exceção; justificativa e autorização são ações distintas. | AC11 |

| Q09 | Reserva | Sem vencimento; erro fiscal mantém; urgência exige cancelar pedido anterior. | AC01, AC11 |

| Q10 | Cancelamento e devolução | Cancelar documentos/pedido; retornar separado à posição; devolução física por nova entrada. | AC01, AC02 |

| Q11 | Transferência | Fora da primeira versão; transporte no TMS. | AC16 |

| Q12 | Propriedade | Um proprietário por pedido; emitente da entrada é o cliente; sem troca interna de dono. | AC03 |

| Q13 | Fiscal | Rodogarcia emite no NOTAZZ; várias notas possíveis; Natalina valida. Piloto manual proposto. | AC01, AC02 |

| Q14 | Critérios de cobrança | Entrada, posição-dia e saída; parâmetros contratuais por cliente. | AC04, AC05, AC06 |

| Q15 | Diárias | Calendário, mesmo dia zero, pico e vigência nova. Combinação matemática proposta com exemplos. | AC04, AC06 |

| Q16 | Término da cobrança | Saída física encerra; reserva/separação/quarentena continuam; avaria imputável suspende. | AC01, AC04, AC08 |

| Q17 | Fechamento | Ciclo por cliente, corte sem repetir dias, aprovação integral, ESL e ajustes após nota. | AC02, AC07 |

| Q18 | Valor do estoque | Valor da nota proporcional ao saldo; exclui avaria do indicador, não do controle físico. | AC08 |

| Q19 | Estoque inicial e contagem | Planilha se existir saldo; etiquetas conferidas; correção operacional rastreável proposta. | AC03, AC13 |

| Q20 | Perfis | Gestor, Supervisor e Operação; sem obrigatoriedade de segunda pessoa. | AC11 |

| Q21 | Data FIFO | Chegada física; primeira entrega da nota; devolução preserva original; sem reunir datas diferentes. | AC10, AC11 |

| Q22 | Confirmação da saída | Leitura de cada unidade; XML e supervisor/gestor. Conciliação fiscal/físico proposta. | AC01 |

| Q23 | Serviços | Registro manual, supervisor e liberação do gestor; vínculo e duplicidade resolvidos por proposta. | AC09 |

| Q24 | Inativos | Encerramento de compromissos antes da inativação definitiva proposto. | AC12 |

| Q25 | Piloto | Osasco; Caio; dez usuários/quatro simultâneos estimados; necessidade em 13/10. | AC16 |

| Q26 | Equipamentos | Web, Tanca, Mickael; impressão/leitura/rede requerem teste local. | AC14, AC16 |

| Q27 | Continuidade | Planilha, até 36h de parada, backups; Lucas e Caio. Recuperação a comprovar tecnicamente. | AC14, AC15 |



Os identificadores Q01 a Q27 e seus 110 subitens permanecem no questionário histórico. AC01 a AC16 não são outra lista de perguntas: registram soluções derivadas da leitura e do raciocínio. A emissão versus retirada, as duas posições e a combinação pico/diária merecem conferência por exemplos antes do uso real, sem impedir o avanço do estudo.



Os dados que não podem ser deduzidos são preços contratados, parâmetros fiscais concretos, cadastros reais e capacidades do ambiente. Eles entram como tarefas de implantação com responsáveis conhecidos. As propostas não substituem autorização fiscal ou homologação de valores.



## Levantamento técnico



O acompanhamento de execução está no [states.md](../states.md). Esta seção registra os assuntos a definir; os status de implementação ficam nas etapas BE e FE correspondentes, sem repetir uma segunda trilha aqui.



| ID | Ponto ainda aberto | Encaminhamento previsto |

| --- | --- | --- |

| T01 | Versões e ferramentas do frontend/SQL Server; ambiente operacional | Base Java/Spring/Maven escolhida e validada localmente em BE02; ver documento 12. React, SQL Server e ambiente operacional continuam a definir. |

| T02 | Organização interna e dependências do frontend | Pacote e dependências backend definidos tecnicamente em BE02, conforme documento 12; frontend permanece futuro. |

| T03 | Banco, acessos, versão e validação SQL Server | Esquema técnico `wms`, JPA e Flyway escolhidos; V1/procedimento preparados. Nome de banco/host/credenciais e aplicação real ainda dependem do alvo de desenvolvimento. H2 usado somente em testes. |

| T04 | Interface do emissor, retorno, autenticação e ambiente de validação | NOTAZZ identificado; Natalina é referência fiscal. Para o piloto, registro manual/importação proposto; futura API e entrega ao ESL serão levantadas. |

| T05 | Coletor, impressora, etiqueta e código físico de leitura | D16 escolheu UUID permanente e contrato de dados da etiqueta no documento 20. Coletor web e impressora Tanca informados; formato/simbologia, modelos, amostras e leitura continuam para validação com Mickael. |

| T06 | Rede, usuários simultâneos, volume e quantidade de armazéns | Osasco primeiro, dez usuários/quatro simultâneos estimados. Volumes e cobertura serão medidos; não presumir desempenho validado. |

| T07 | Provedor de identidade, implantação, monitoramento e gestão de segredos | Resource Server JWT implementado para cadastros, com perfis/alcances e auditoria. Falta definir/configurar emissor real, contas, ciclo de tokens e integração de login. Hospedagem e condições operacionais continuam a definir. |

| T08 | Recuperação, cópias de segurança e tolerância à indisponibilidade | Lucas em TI e Caio na operação; 36h de parada informadas. Definir e testar capacidade real de recuperação, sem equiparar parada a perda aceitável de dados. |



Os dados funcionais recebidos orientam esses levantamentos; verificações técnicas ainda não executadas permanecem futuras. Nenhuma solicitação foi enviada a terceiros e não há credenciais registradas.



## Como registrar novas decisões



Para cada resposta ou mudança, acrescentar data, quem decidiu ou qual fonte respondeu, IDs afetados, decisão, motivo e impacto. Atualizar o estado da pendência e os documentos envolvidos. Se a resposta contradizer o PDF, registrar o conflito e a solução proposta com fundamento; avançar no estudo sem apresentar a proposta como aprovação do gestor. Validar seus efeitos por exemplos antes do uso real.



## Histórico



| Data | Registro |

| --- | --- |

| 03/10/2026 | Criada a estrutura local e preservada a especificação original. Registradas as decisões D01 a D07, propostas P01 a P07 e questões Q01 a Q20. Aplicação ainda sem implementação. |

| 03/10/2026 | Registrado o pedido D08. Questionário ampliado para Q01 a Q27, com 110 subitens curtos e condicionais. Atualizados índice, continuidade e vínculos com os fluxos. Todas as respostas seguem pendentes. |

| 05/10/2026 | Recebido e preservado o Word com respostas aos 110 subitens e 12 imagens distintas. Registrada D09; consolidadas regras explícitas e 16 tratamentos por raciocínio, sem reenviar questionário. Atualizados arquitetura, fluxos, cenários e continuidade. Sem implementação, acesso ao banco ou mensagem a terceiros. |

| 05/10/2026 | Registrada D10. Consultados os padrões de satelite-tms-api, dashboards-etl e avaliacao-desempenho-competencias. Adaptado AGENTS.md e criado states.md com trilhas backend/frontend e marcos conjuntos. Índices e orientações de retomada sincronizados; sem copiar configurações de outros sistemas ou implementar a aplicação. |

| 05/10/2026 | Registrada D11 e iniciada implementação. Escolhas técnicas de BE02 no documento 12; build, 12 testes e execução local no documento 13. BE01 parcial; SQL Server, autenticação e frontend permanecem etapas futuras. Nenhuma proposta de negócio foi convertida em aprovação do gestor. |

| 05/10/2026 | Registrada D12. Entregues cinco cadastros com API/JPA, controle de acesso, auditoria atômica, revisão e encerramento pendente. V1 SQL Server preparada; 40 testes aprovados em ambiente isolado. Escopo/limites no documento 14 e validação no 15. BE03/BE04/BE05 não concluídos no ambiente real. |

| 05/10/2026 | Registrada D13 e concluída BE16: resources em properties, validação nos serviços, formatação e verificações de ferramentas/arquitetura. Build limpo com 52 testes; JAR local conferido. Padrões no documento 16 e evidências no 15. Sem alteração no SQL Server ou publicação. |

| 05/10/2026 | D15 entrega recebimento manual/XML, chegadas, quarentena, estorno e efetivação única. V2 preparada e 86 testes aprovados; documentos 18/19. Preservados D14/OR01 e os arquivos da preparação de orquestração paralela. Sem migração, emissão fiscal ou publicação nesta implementação. |

| 05/10/2026 | D16 entrega unidades logísticas, composição de origem, divisão/reagrupamento e dados de etiquetas. V3 preparada; 122 testes aprovados, sendo 36 novos. Contrato e validação nos documentos 20/21; próximo código BE08. SQL Server, frontend e impressão física permanecem separados. |

| 05/10/2026 | D17 entrega endereçamento, capacidade, conjuntos de posições, movimentos/bloqueios e consultas de estoque. V4 preparada; 153 testes aprovados, sendo 31 novos. Contratos/evidências nos documentos 22/23. BE08 em validação externa e BE14 parcial; próximo código BE09. Sem SQL Server, frontend, cobrança ou publicação. |

| 05/10/2026 | D18 entregue localmente pela equipe WMS: pedido integral por cliente/armazém, FIFO, reserva sem vencimento, parcial de pallet, exceção por perfil, saldo reservado e avaria posterior. Contratos no [documento 24](24-pedido-saida-fifo-e-reserva.md), V5 preparada e [validação](25-validacao-pedido-saida-e-reserva.md): spotless/clean verify com 197 testes, 44 novos, revisão favorável de Vigia e Graphify atualizado. BE09 em validação externa, BE01/BE14 parciais; BE10/BE11 completos fora do recorte. AC10/AC11 continuam propostas identificadas. Git zero commits, alterações preexistentes preservadas. Sem ações externas proibidas; aguardar nova demanda. |

| 05/10/2026 | Registrada autorização expressa D19 para concluir continuamente o backend local restante BE01–BE16, sem autorização por macrobloco. Ordem/divisão no documento 26; não autoriza homologação ou as ações externas proibidas. |

| 06/10/2026 | Primeiro bloco D19 aceito localmente: BE10/BE11 e complemento BE01, [contrato 27](27-separacao-retirada-retornos-e-avaria.md) e [evidência 28](28-validacao-separacao-retirada-retornos.md). Após achados/P2 temporal, spotless/clean verify com 240 testes, Vigia favorável e V6 compatível em arquivos. Base197, V1–V5, origem e histórico preservados. Segue BE05/BE12 autonomamente. SQL Server, fiscal, equipamentos e conferência operacional/comercial reais permanecem externos; AC01–AC16 não convertidas em aprovação. |

| 06/10/2026 | Recorte local BE01/BE05/BE12 D19 aceito após P2 financeiro BE08: clean verify292/0/0/0,14 XMLs/JAR/222 hashes conferidos, Vigia favorável02:33 e V7/JPA518/518 mais67/67 suplementares compatíveis. [Contrato29](29-cadastros-servicos-e-calculo.md)/[evidência30](30-validacao-servicos-e-calculo.md). Inativação definitiva BE05 permanece após BE13/BE14; BE13 liberado para refinar31 antes de Java/V8. Continuar autonomamente, sem emissão, SQL Server real, cobrança a clientes ou homologação comercial/fiscal presumida. |

| 06/10/2026 | BE01/BE13 D19 aceito localmente após três P2: clean verify333/0/0/0,15 XMLs/JAR/251 hashes conferidos, Vigia favorável04:29, V8/JPA523/523 e163/163 em arquivos. [Contrato31](31-fechamento-contagem-e-contingencia.md)/[evidência32](32-validacao-fechamento-e-contingencia.md). Regularização de origem conserva deltas/versões, período é conferido antes do replay e auditoria recupera a resolução; externo fiscal/SQL/comercial não homologado. Segue BE14/inativação definitiva BE05 e integração final, sem nova autorização, preservando todas as evidências anteriores. |





**Fecho local D19 em06/10/2026:** escopo BE01–BE16 entregue/testado/revisado após P2-locks:367/0/0/0,17 XMLs/JAR/289 hashes conferidos, Vigia favorável e V9/JPA904/904+129/129 em arquivos. Matriz [34](34-matriz-e-validacao-final-backend.md), evidência [32](32-validacao-fechamento-e-contingencia.md) e modelo [35](35-modelo-integrado-e-jornadas-backend.md) sincronizados; Graphify atualizado. Não é aprovação comercial/fiscal/homologação/piloto/publicação. AC01–AC16 e todos os limites/donos externos preservados. Sem nova autorização criada; conclui o trabalho local autorizado por D19.
