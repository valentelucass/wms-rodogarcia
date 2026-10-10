# Estado e trilha de implementação do WMS Rodogarcia

## FE02-REG01-A03 — filtros visíveis sem sanfona, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** removida a sanfona isolada de Filtros e contexto da consulta nas páginas que compartilham `RecordWorkspace`. Formulário sempre visível com um único título e sem borda interna; aplicação/restauração, espera, permissões e responsividade preservadas. Dependências FE02-REG01 e FE02-REG01-A02; consultas e vínculos BE04–BE14 → FE03–FE13 mantidos.

- [x] Remover abertura/fechamento e títulos/bordas duplicados dos filtros compartilhados.
- [x] Conferir consultas, restauração, teclado, espera, temas e tamanhos de tela.

**Conferência:** tipagem/build real próprio, lint/formatação, [74 testes existentes](frontend/evidencias/record-filters-a03-regression.json) e [seis casos Chrome](frontend/evidencias/record-filters-a03-browser.json) aprovados; filtros com dois/sete campos em 1440/768/360px, ambos os temas, sem vazamento horizontal, aplicação por teclado, campos bloqueados durante espera e valores mantidos após resposta/restaurados ao estado inicial. Capturas inspecionadas. Assets do frontend DEV existente; APIs interceptadas com dados fictícios, sem backend/SQL ou reinício. [Definição](docs/design-system/etapa-06.md#filtros-visíveis-sem-sanfona--fe02-reg01-a03), [recibo](orchestracao/.runtime/record-filters-a03/resultado.json). **Próximo:** recarregar a página para carregar a fonte atual; integração real mantém sua etapa própria.

## FE02-REG01-A02 — alinhamento compartilhado das barras de filtros, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** aplicar o alinhamento a todas as barras semelhantes à imagem de Atualizar lista. Dependências FE02-REG01-A01 e FE02-DASH02-A01. Regra comum em `forms.css` para filtros de registros (incluindo recebimento) e mapa; margem já corrigida nos estilos locais agora centralizada. Contratos e vínculos backend/frontend mantidos.

- [x] Unificar o alinhamento das barras de filtros, preservando a quebra responsiva.
- [x] Conferir os arquivos servidos pelo frontend DEV, temas, teclado e tamanhos de tela.

**Conferência:** formatação, tipagem/build real próprio e [seis casos Chrome](frontend/evidencias/record-refresh-a02-browser.json) aprovados em 1590/768/360px, claro/escuro, nas barras de Clientes, Armazéns e Estoque; alinhamento medido, Enter preserva a busca, toque e página sem vazamento horizontal, capturas inspecionadas. Assets do frontend DEV existente em 127.0.0.1:25581; todas as APIs interceptadas com dados fictícios. [Definição](docs/design-system/etapa-06.md#alinhamento-compartilhado-dos-filtros--fe02-reg01-a02), [recibo](orchestracao/.runtime/record-refresh-a02/resultado.json). **Próximo:** recarregar a página para carregar a fonte atual. Sem backend/SQL ou reinício; processo DEV existente preservado.

## FE02-DASH02-A01 — alinhamento da busca do mapa, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** removida a margem inferior de 8px dos rótulos dos filtros do mapa, alinhando Buscar com Buscar endereço e Mostrar posições. Dependência FE02-DASH02; BE14-DASH02 → FE02-DASH02 mantém contratos e regras.

- [x] Corrigir o alinhamento apenas nos filtros do mapa e conferir ambos os temas em desktop/tablet/mobile.

**Conferência:** formatação, tipagem/build real próprio e seis casos Chrome com HTTP interceptado em 1440/768/360px aprovados; alinhamento medido, Enter/Limpar e quebra de linha no mobile preservados, capturas inspecionadas. [Definição](docs/design-system/etapa-05.md#alinhamento-da-busca--fe02-dash02-a01), [recibo](orchestracao/.runtime/map-search-a01/resultado.json). **Próximo:** carregar o CSS atual na página. Sem backend/banco ou reinício de processos existentes; preview próprio encerrado.

## FE02-REG01-A01 — tabelas, badges e ações, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** tabelas com filtros proporcionais, cabeçalhos e ações organizados; badges com rótulos legíveis e cores semânticas, botões azuis com ícones. Dependências FE02-REG01, FE02-DS01–DS03 e FE05-PED01; contratos BE05 → FE04 e demais leitores preservados.

- [x] Ajustar composição compartilhada, situações e ações nos temas claro/escuro.
- [x] Conferir tipagem, comportamento existente, teclado, contraste e desktop/mobile.

**Conferência:** tipagem/lint/formatação/builds próprios, 43 testes focais, sete casos Chrome de apresentação e duas jornadas existentes de Clientes aprovados; cinco tons e botões com contraste mínimo 4,5:1, desktop/mobile, teclado e permissões. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-06.md#tabelas-badges-e-ações--fe02-reg01-a01), [recibo](orchestracao/.runtime/record-colors-a01/resultado.json). **Próximo:** carregar a fonte atual na página; integração real conserva sua etapa própria. Sem backend, SQL, alteração operacional, publicação ou reinício de processos existentes; HTTP interceptado e dados fictícios nos previews próprios encerrados.

## FE05-PED01 — entrada centrada no pedido, 09/10/2026

**Concluído no recorte local, pedido expresso de Lucas:** as quatro etapas globais de Entrada e conferência foram reunidas em Pedidos de entrada, com criação no cabeçalho e notas/itens, conferência, divergências, histórico e efetivação no pedido persistente. Dependências FE02-REG01, FE02-DS01–DS03, BE06 → FE05. Comandos, revisões, origens, permissões e efeitos backend preservados; sem nova situação manual, migration ou fusão de entidades.

- [x] Mapear todos os comandos e estados atuais e definir seu destino no pedido.
- [x] Implementar lista, criação, detalhe e operações com atualização após confirmação.
- [x] Conferir fronteiras, temas, teclado, mobile e documentar evidências e limites.

**Conferência:** tipagem, lint, formatação e builds real/fictício próprios aprovados; 478 testes na regressão completa, 15 focais e seis casos Chrome com HTTP interceptado, ambos os temas, desktop/mobile, notas múltiplas, criação seguida de nota/XML, identidade/revisão/UUID, histórico, conflito, filtros preservados após erro, teclado e paginação. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-07.md), [recibo](orchestracao/.runtime/receiving-ped01/resultado.json). Graphify recusou substituir 15.220 nós por 13.119, sem `--force`; mapa preservado. **Próximo:** conferir fonte em execução e integração WMS_DEV/desempenho pelo fluxo autorizado e guarda vigente. Filtros complementares usam a página atual; dados gerais não têm endpoint de edição. Sem backend, SQL, migration, reinício de processo existente ou publicação nesta entrega; alterações preexistentes preservadas.

## FE02-REG01 — listas, detalhes e ações por registro, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** 12 áreas, 50 páginas e 171 ocorrências de ações inventariadas e migradas. Dependências FE02-DS01–DS03, FE04–FE12 e contratos BE04–BE14. Lista/fila/consulta principal ao entrar; criação no cabeçalho; detalhe e comandos no registro persistente; edição em diálogo, mantendo etapas operacionais, permissões, revisão, motivo e confirmação. Cada área tem fontes, colunas e comandos próprios, além da estrutura compartilhada.

- [x] Inventariar fontes e ações das 12 áreas e definir seu destino por página.
- [x] Implementar Clientes e expandir as páginas com adaptações específicas.
- [x] Conferir integrações de transporte, estados, contexto, teclado e mobile; registrar evidências e limites.

**Conferência:** tipagem/lint/builds real e fictício, 467 testes da suíte e 25 focais, seis casos Chrome HTTP interceptado, cobrindo as 50 páginas em desktop/mobile, temas, clientes e falhas. [Padrão e limites](docs/design-system/etapa-06.md), [inventário](docs/design-system/inventario-paginas.md), [recibo](orchestracao/.runtime/record-pages-resultado.json). Graphify recusou extração menor; grafo preservado sem force.

**Próximo:** conferir a fonte carregada na aplicação e integração real no WMS_DEV pelo fluxo autorizado com guarda vigente. Sem SQL Server, backend, migrations, emissão/impressão real, publicação ou reinício nesta entrega. Cidade/vínculos de armazém não constam da resposta básica de Cliente; sua inclusão exige contrato/relação definidos, preservando múltiplos vínculos. Demais frentes preexistentes conservadas.

## FE02-CTX01-A01 — espaço dos seletores de contexto, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** ajuste somente visual do topo em Cadastros e demais páginas, preservando seleção de todos os clientes/armazéns e Aplicar contexto. Dependência FE02-CTX01; a implementação funcional CTX01/DASH02 mantém seu estado próprio.

- [x] Remover largura fixa que sobrepunha as colunas, ampliar o espaço e proteger a largura das ações.
- [x] Distribuir o contexto em outra linha no tablet/notebook e empilhar os seletores no mobile.
- [x] Conferir formatação/build e Chrome com API interceptada em sete larguras (320–1440 px), textos completos, listas dentro da tela e temas; capturas inspecionadas.

**Prova:** [medidas](frontend/evidencias/context-spacing-resultados.json), [composição](docs/design-system/etapa-03.md#espaço-dos-seletores--fe02-ctx01-a01). Alteração de apresentação em `context.css`, sem backend/banco ou regra de seleção. Preview próprio encerrado. Graphify recusou extração menor, sem forçar. **Próximo:** atualizar a página em execução para carregar o CSS; nenhuma correção visual pendente neste recorte.

## FE02-CTX01 / BE14-DASH02 → FE02-DASH02 — seletores e visão geral, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** três correções: seleção de cliente por nome, seleção de armazém por nome e Início conforme o texto anexado. Dependências BE05/FE04, BE08/FE07, BE09/FE08, BE12–BE14 e FE02-DS03. Substitui a composição do Início de DASH01, preservando seu contrato/componente. Catálogos e leituras respeitam alcance; IDs permanecem nos contratos. Dez indicadores, todos os armazéns permitidos, mapa paginado, detalhes e endereçamento pelo coletor existente. CTX01-A01 conserva sua entrega visual própria.

- [x] Cliente pesquisável por nome/código no topo e nos campos correspondentes dos formulários operacionais.
- [x] Armazém pesquisável, seleção de todos para a visão geral e contexto compartilhado com o mapa.
- [x] Dez indicadores e mapa ligados à leitura backend, detalhes autorizados, filtros, atualização e destino preenchido sem gravar automaticamente.

**Conferência:** `verify` backend com dez testes locais (arquitetura/JPA H2 isolada/serviço) e artefato próprio; suíte frontend com 441 aprovados e 23 focais finais, tipagem/lint/builds real/fictício. Dezoito casos Chrome com API interceptada e três do exercício fictício, incluindo temas, 320–1920px, nomes/Long/Bearer, filtros/paginação, foco e encaminhamento. Capturas inspecionadas. [Definições e limites](docs/design-system/etapa-05.md), [recibo](orchestracao/.runtime/visao-operacao-resultado.json).

**Próximo:** carregar a fonte atual pelos fluxos autorizados e conferir dados/consultas no WMS_DEV após a guarda vigente. Sem SQL Server, migration, restart, publicação ou comprovação da versão já em execução neste recorte. Desempenho/dialeto reais e validação de armazém/equipamento/fiscal continuam separados. As demais frentes foram preservadas.

## BE14-DASH01 → FE02-DASH01 — gráficos do Início, 09/10/2026

**Concluído localmente, autorização expressa de Lucas neste chat:** contrato agregado de leitura em `backend/` e gráficos abaixo do Acesso rápido: ocupação física, disponibilidade por SKU e fila de saída, com quatro indicadores. Dependências BE04/BE08/BE09/BE14, FE02/FE03/FE08 e FE02-DS03-A05. Permissões, regras operacionais e frentes preexistentes preservadas; nenhuma migration ou intervenção em SQL Server/processos existentes. Séries históricas e cobrança permanecem evolução separada.

- [x] Implementar contrato agregado com escopo validado e disponibilidade apurada no backend.
- [x] Apresentar gráficos e indicadores nos dois modos, com estados de espera/erro/vazio, teclado e responsividade.
- [x] Conferir backend isolado, contratos, frontend e navegador; registrar evidências e limites.

**Conferência:** `verify` isolado com cinco cenários HTTP/H2 do dashboard e cinco verificações de arquitetura; tipagem/lint, 432 testes frontend e cinco focais finais, builds real/fictício próprios. Três casos Chrome no exercício e um no modo real com API interceptada aprovados, incluindo ambos os temas, cinco larguras, foco após paginação, contexto Long, erro/vazio e CSP; capturas inspecionadas. [Definição](docs/design-system/etapa-04.md), [recibo](orchestracao/.runtime/dashboard-dash01-resultado.json). Graphify recusou substituir o mapa por extração menor, sem `--force`. **Próximo:** carregar o backend atualizado pelo fluxo DEV autorizado e conferir a integração SQL Server; esta entrega local não comprova dialeto, desempenho ou versão já em execução. Previews próprios encerrados; frentes A06/PROD e manutenção do mapa conservam seus estados.

## FE02-DS03-A06 — diálogos de senha e usuários, 09/10/2026

**Concluído no recorte local, pedido expresso de Lucas:** Minha senha abre sobre a página atual; cadastro de usuários tem campos agrupados e diálogo destacado. Lucas rejeitou a faixa azul no topo; ela foi removida. Dependências: FE02-DS03 e BE04-AUTH01 → FE03-AUTH01. Validações, permissões e encerramento das sessões após troca de senha preservados.

- [x] Abrir Minha senha como modal sem substituir a página ou perder sua edição.
- [x] Agrupar campos de usuários/senha, destacar superfície e reorganizar ações.
- [x] Conferir teclado, fechamento/foco, espera/erro, temas e tamanhos de tela.

**Conferência:** tipagem final, lint focal, formatação e builds real/fictício aprovados; 14 testes unitários focais e 16 casos Chrome com API interceptada, incluindo cinco larguras, ambos os temas, foco inicial/retorno, edição preservada, senha obrigatória e envio incerto de usuários. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-03.md#diálogos-de-conta--fe02-ds03-a06), [prova](frontend/evidencias/account-a06-corrigido-browser-resultados.json). Preview próprio encerrado. **Próximo:** conferir a página em execução para carregar o ajuste; sem backend/banco ou reinício de processos existentes. Frentes DASH01 e PROD-LAUNCH01 preservadas.

## PROD-LAUNCH01 — launcher Windows de produção, 09/10/2026

**Preparo local testado e revisado; entrega parcial, operação PROD bloqueada:** BE02-PROD01 → FE02-PROD01, dependências BE02/FE02/D32/SQL-TLS-ATUAL. Entrega `iniciar-prod.bat` (grafia recebida `.bar` preservada no pedido), build candidato isolado/servidor frontend estático real, backend perfil PROD estrito e informações para os domínios futuros wms.rodogarcia.com.br e wms-api.rodogarcia.com.br. Nenhuma configuração Cloudflare/DNS/túnel nesta etapa.

Farol BAT/auxiliares e canônicos; Cedro backend/configs PROD; Lume novos build/servidor frontend, fonte visual somente leitura; Prumo portas/canais protegidos/guarda; Vigia revisão independente. Portas locais25590/25591 inventariadas livres, revalidadas no launcher; bind127.0.0.1, sem kill por porta. Canal SQL/AUTH PROD próprio ainda não descoberto no recorte de metadados; não usar material DEV/admin ou fabricar identidade. [Escopo e donos](orchestracao/.runtime/launcher-producao/inicio.json).

**Conferência:** BAT `--preparar` real exit0 no run5bcb; BAT normal real exit40 AUTH PROD ausente no run9626, antes de guarda SQL ou servidores. Root23 focais verdes; frontend17 focais e13 verificações HTTP de assets com upstream sintético, guard de recusa sem SQL. Builds recentes recusaram promoção quando a frente visual mudou. Três achados nos helpers/config efetiva corrigidos e reconfirmados por Vigia; zero material local aberto. Cedro15 cleanup e8 configuração externa finais, sem novo package; versões/REDS originais preservados. Build PASS não comprova startup; complemento03DE inicial histórico, DFB55 corrigido separado. [Recibo final](orchestracao/.runtime/launcher-producao-resultado.json), [revisão](orchestracao/.runtime/launcher-producao/vigia/parecer-final02.json), [uso e suporte](docs/44-launcher-producao-e-publicacao-futura.md).

**Próximo:** aguardar Lucas. Encaminhamento material: responsável de produção definir/provisionar canais SQL/AUTH e principal WMS próprios; então concluir/revisar binding operacional e executar a guarda/subida autorizada. O gate atual não implementa Open/PASS; identidade/permissões/catálogo/histórico PROD e entrada HTTPS continuam não comprovados. Nenhum recurso provisionado aqui ou URL PROD ativa. Não repetir builds por handoff, testar/escrever negócio em PROD ou reiniciar processos existentes. Frente visual ativa preservada; VALID anterior permanece encerrada.

## FE02-DS03-A05 — fundos dos acessos rápidos, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** fundos marcantes e distintos nos quatro cards de Acesso rápido, com base no design system e preservando conteúdo interno/destinos. Dependência: FE02-DS03; ligações backend/frontend dos quatro destinos preservadas.

- [x] Criar variantes de fundo com tokens próprios e geometria da fundação visual.
- [x] Conferir contraste, temas, responsividade, teclado e destinos dos quatro cards.

**Conferência:** tipagem/lint focal/formatação/builds real e fictício; seis casos Chrome em seis larguras e ambos os temas, incluindo contraste, hover, teclado e quatro destinos. Capturas inspecionadas. [Definição e limites](docs/design-system/etapa-03.md#fundos-dos-acessos-rápidos--fe02-ds03-a05), [prova](frontend/evidencias/shortcuts-a05-final-resultados.json). Preview próprio encerrado, sem backend/banco ou alteração de operações. **Próximo:** conferir a página em execução para carregar as variantes; manutenção do mapa conserva sua frente própria.

## FE02-DS03-A04 — painel de referências da jornada, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** painel Referências da jornada no contexto atual e botão estilizados nas 12 jornadas. Dependência: FE02-DS03; vínculos BE04–BE14 → FE03–FE13 preservados. Escopo visual compartilhado, mantendo seleção explícita, bloqueio durante confirmação e aviso de descarte da edição.

- [x] Aplicar superfície de apoio, hierarquia das referências e botão destacado com tokens dos dois temas.
- [x] Conferir painel vazio/preenchido, teclado, tamanhos de tela e aplicação das referências no formulário.

**Conferência:** tipagem/lint focal/formatação/builds real e fictício; cinco testes existentes e seis casos Chrome finais nas 12 jornadas, claro/escuro e cinco larguras. Referências preenchidas e aplicação por Enter conferidas; capturas inspecionadas. [Entrega e limites](docs/design-system/etapa-03.md#referências-da-jornada--fe02-ds03-a04), [prova](frontend/evidencias/references-a04-final-resultados.json). Preview próprio encerrado, sem backend/banco/reinício de processos existentes. Graphify recusou redução do mapa, sem forçar. **Próximo:** conferir a página em execução para carregar o novo painel; manutenção do mapa e validações reais continuam separadas.

## FE02-DS03-A03 — limite de altura dos campos de texto, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** expansão vertical limitada globalmente, incluindo motivo/justificativa de Cadastros → Clientes. Dependência: FE02-DS03; recorte visual compartilhado, preservando valores, limites de caracteres e contratos existentes.

- [x] Aplicar altura máxima global aos campos multilinha, com rolagem interna e redimensionamento vertical limitado.
- [x] Conferir campos de justificativa e XML em desktop/mobile e registrar a regra e a prova local.

**Conferência:** tipagem, formatação dos dois CSS e build fictício em diretório isolado aprovados; oito verificações Chrome de justificativa/XML, 390/1440 px e claro/escuro, com arraste real de redimensionamento, expansão forçada, rolagem interna e ausência de rolagem horizontal. [Regra e limites](docs/design-system/etapa-03.md#limite-de-altura-dos-campos--fe02-ds03-a03), [prova local](frontend/evidencias/textarea-height-final.json). Sem backend/banco; preview próprio encerrado. Manutenção do mapa registrada no mesmo documento, sem forçar redução de fontes.

**Próximo:** atualizar a página já aberta para carregar o limite. As frentes A01/A02 mantêm seus estados próprios.

## FE02-DS03-A02 — composição do Coletor, 09/10/2026

**Concluído localmente, pedido expresso de Lucas:** Coletor integrado ao design system. Na conferência, Lucas corrigiu a proposta de centralização: o painel alinha com o título e ocupa a largura útil da página. Dependência: FE02-DS03; vínculos backend/frontend do Coletor preservados. Recorte de apresentação, sem mudar contratos ou regras de leitura/confirmação.

- [x] Alinhar a área operacional à página e organizar tarefa, leitura, destino e confirmação com os tokens existentes.
- [x] Conferir desktop/mobile, claro/escuro, teclado/scanner web e regressão focal do Coletor.

**Conferência:** tipagem/lint focal/formatação/builds real e fictício; seis testes existentes e sete casos Chrome no exercício fictício. Temas e alinhamento em 320/390/1024/1440/1920 px; Enter/foco do destino/confirmação em 390/1440 px, capturas inspecionadas. [Entrega e limites](docs/design-system/etapa-03.md#composição-do-coletor--fe02-ds03-a02), [prova](frontend/evidencias/collector-a02-resultados.json). Sem banco/equipamento/reinício de processos existentes; preview próprio encerrado. **Próximo:** conferir a página já aberta para carregar o ajuste. Refinamento da textura A01 interrompido a pedido de Lucas; não retomar automaticamente. A03 mantém seus próprios critérios.

## FE02-DS03-A01 — preenchimento lateral das jornadas, 09/10/2026

**Interrompido por mudança expressa de foco de Lucas:** painel lateral implementado; o refinamento da textura foi deixado de lado para priorizar o Coletor (A02). Dependência: FE02-DS03; componente comum às 12 jornadas, preservando vínculos BE04–BE14 → FE03–FE13. Não há aceite visual da textura.

- [ ] Compor painel decorativo com altura automática no desktop e ocultá-lo no layout empilhado.
- [ ] Conferir mudanças de etapa/ação, temas, teclado, responsividade e deslocamento do topo.

**Próximo:** aguardar nova demanda para esta textura. O fecho geral FE02-DS03 abaixo mantém seus próprios critérios.

## FE02-DS03 — organização interna das páginas, 09/10/2026

**Concluído localmente, por pedido expresso de Lucas:** padronizada a hierarquia de ações, agrupamentos, uso de espaço, paginação e diálogos nas páginas internas. Dependências: FE02-DS01-A03 e FE02-DS02-A01; ligações BE04–BE14 → FE03–FE13 existentes preservadas. Recorte visual frontend; regras, contratos, permissões e conexão com banco preservados.

- [x] Organizar Início e a estrutura compartilhada das jornadas de Cadastros até Consultas/relatórios.
- [x] Padronizar formulários, listas, paginação e confirmações centrais.
- [x] Aplicar organização proporcional ao Coletor e Usuários/acessos.
- [x] Conferir contexto no topo desktop de 64 px, placeholders, ação em verde petróleo e persistência da sidebar, conforme refinamentos expressos de Lucas.
- [x] Conferir tipagem, estados operacionais, teclado, temas e tamanhos de tela; registrar evidências e limites.

**Conferência:** tipagem, lint, formatação dos arquivos alterados e builds real/fictício; 424 testes unitários únicos aprovados por composição de 422 amplos + dois timeouts resolvidos em 18 focais, sem apresentar a rodada ampla como integralmente verde. 33 casos Chrome únicos: 23 de exercício e dez de aplicação nativa com API interceptada; oito casos repetidos qualificam o delta intermediário paralelo da coluna de etapas. Incrementos posteriores A01 mantêm sua própria validação. Capturas conferidas. [Composição e limites](docs/design-system/etapa-03.md), [recibo local](orchestracao/.runtime/design-system-etapa03-resultado.json). Sem integração de negócio real ou equipamentos nesta etapa. Graphify recusou redução 15.220→12.661 e o mapa principal foi preservado, sem forçar.

**Próximo:** atualizar a página já aberta para carregar os ajustes; conferir novas observações de uso somente por demanda. Frentes reais e de ambiente abaixo mantêm seus estados próprios.

## FE02-DS01-A03 / FE02-DS02-A01 — preferência de tema e links de ajuda, 09/10/2026

**Concluído localmente:** A03 substitui a reinicialização automática de A02, conforme [regra atual](docs/design-system/etapa-01.md#temas). A01 restringe o destaque das ações de ajuda ao texto, com sublinhado e foco por teclado, preservando diálogos e alvos de toque. Dependências: FE02-DS01-A02 e FE02-DS02; sem alterar contratos BE04-AUTH01 → FE03-AUTH01. Builds real/fictício, tipagem, lint focal e formatação aprovados; 22 casos Chrome passaram (7 login/API interceptada + 15 preferência/navegação fictícia), com capturas de foco conferidas. Provas `frontend/evidencias/design-system-a03-{login,preferencia}-browser-resultados.json`. Graphify mantém recusa anterior, sem forçar. Próximo: atualizar a página para carregar a correção; composição interna segue na nova FE02-DS03.

## FE02-DS02 — login, topo e rodapé, 09/10/2026

**Concluído no recorte visual local:** login informativo e compacto, ajuda em diálogo sem deslocar o card, área reservada para avisos, mensagens de falha próprias de acesso e ícone de headset. Topo alinhado e rodapé compartilhado com Lucas Andrade, LinkedIn e suporte confirmados nos projetos vizinhos em leitura. Dependências: FE02-DS01-A02 e BE04-AUTH01 → FE03-AUTH01. Contratos e regras de autenticação preservados; sem SQL ou reinício de processos existentes.

- [x] Melhorar composição e orientações do login usando os tokens existentes.
- [x] Compartilhar rodapé com crédito, LinkedIn e contato confirmados nas referências.
- [x] Alinhar identificação e ações do topo nos tamanhos desktop/mobile.
- [x] Conferir tipagem, lint, build, fluxos de autenticação e aparência no navegador.

**Evidência:** [entrega e limites](docs/design-system/etapa-02.md), [recibo](orchestracao/.runtime/design-system-etapa02-resultado.json). Tipagem/lint/formatação e builds real/fictício aprovados; 66 testes focais e 19 casos de Chrome (7 com API interceptada, 12 fictícios). Login sem rolagem entre 320×640 e 1440×720, sem deslocamento por ajuda/erros; capturas claro/escuro conferidas. Mapa Graphify mantém impedimento próprio de atualização, sem força.

**Próximo:** atualizar a página aberta para carregar o visual. Próxima composição de páginas depende de nova demanda; investigação SQL e validações reais mantêm os estados próprios abaixo.

## BE02-SQL-DIAG01 — investigação de indisponibilidade SQL, 09/10/2026

**Em andamento, somente diagnóstico:** Lucas confirmou timeout também ao acessar o SQL Server e pediu investigação aprofundada. Dependências: BE02/BE03 → FE02 e FE02-DEV-CON02. Escopo atual: eventos Windows/SQL, recursos do host, processo/listener e ERRORLOG com acesso permitido; sem nova conexão SQL após a falha, reinício, mudança de servidor/ACL ou acesso a dados de outros projetos.

Há cinco eventos Windows2004 em07–08/10 com memória comprometida acima de99% do limite; PowerShell14–17,55GiB e Python20–21,87GiB aparecem como maiores consumidores. Os eventos de07/10 cercam os quatro erros SQL701 históricos. Serviço/PID atual permanecem ligados desde07/10; essa correlação histórica ainda não explica o timeout de09/10. Evidências em `orchestracao/.runtime/sql-investigacao-20261009-*.json`. Próximo: cruzar os recursos atuais e obter os diagnósticos internos delimitados do SQL; origem dos processos antigos não atribuída.

## FE02-DEV-CON02 — prazo de conexão DEV, 09/10/2026

**Ajuste do cliente validado; inicialização real bloqueada:** por pedido expresso de Lucas, abertura SQL120s, guarda completa240s e andamento a cada10s, por política compartilhada. Sete casos de espera/progresso,20 focais do consumo da guarda e sintaxe de quatro scripts aprovados. A única guarda real nova expirou após120,19s em prelogin/handshake (-2/258), sem consultas ou BE/FE; o prazo maior não resolveu a indisponibilidade. Metadados SQLArg1/ERRORLOG recusados pelo Registro Windows com SecurityException, sem elevacao ou leitura do log.

**Dependências:** FE02-DEV-CON01 e BE02 → FE02. **Evidências:** [resultado e hashes](orchestracao/.runtime/dev-conexao-20261009-con02-resultado.json), [guarda real bloqueada](orchestracao/.runtime/dev-conexao-20261009-con02-guarda.json). Conexão única WMS_DEV/WMSDEV/TLS e servidor preservados; sem nova repetição automática. Graphify recusou redução15220→12622, sem forçar. **Próximo encaminhamento:** obter extrato administrativo do ERRORLOG no intervalo14:40–14:42BRT para identificar a causa; retomar a guarda após encaminhamento concreto. Entrega local do prazo concluída, disponibilidade real permanece em validação.

## VALID-LOGIN-CAD01 — fecho parcial autorizado por Lucas, 09/10/2026

**BE04-COR01-DEV01 → FE03-COR01-DEV01 parcial real revisado:** reaproveitado run existente8fa3c9e6722b4170898dc66aabfa044f, frontend http://127.0.0.1:25581, BE72644/25580 e FE70328/25581 com JAR corrigido99128/configD415/preparo947D. Guarda3293 confirmou WMS_DEV/WMSDEV/TLSMandatory/trustServerCertificatefalse,26/26critérios/permissões/catálogo/V1–V11, uma Open215,5245ms e12SELECTmetadados, zero negócio. SQLPID48480/início18:31:41Z e confiança existente atual conferidos; nenhuma outraBAT/Open/renovação ou intervenção em processo nesta retomada. Relato Lucas preservado; CON02/E7B204 permanecem históricos, sem bloqueioSQL corrente diante destePASS datado.

Lume observou realmente18:46:53–57UTC: statusAPI200, usuários anônimo401, CSS/Inter/foco/clique/Tab/Enter1440+390,24asserções públicas aprovadas sem mock/interceptação. CSRF200/renovação automática403,0pageerror/CSP e1consolemetadata sem texto; causa do403 não inferida. Chrome próprio encerrado, servidores preservados. Deltas visuais externos durante/depois da prova qualificados; DOM/capturas datados, sem freeze geral ou equivalência integral posterior.

**7 locais aprovados preservados/0 critérios integrais operacionais reais:** não disponível à equipe sessão WMS própria autenticada; hash bootstrap não recupera senha. Workspace006, Users007 e demais jornadas protegidas não executados. Login nativo D32/DEV07 existe; não retomar proposta histórica Keycloak/BFF. Link loopback é superfície técnica, modo tunnel/Secure preservado; entrada HTTPS operacional e sessão não comprovadas por status/listener. Long gigante/201null/corrida específica continuam complementos locais, sem injeção no banco/API.

[Recibo atual e limites por001–007](orchestracao/.runtime/login-cadastros-validacao-dev-resultado.md), [JSON/hashes](orchestracao/.runtime/login-cadastros-validacao-dev-resultado.json), [parecer independente](orchestracao/.runtime/login-cadastros-validacao-dev/vigia/parecer-retomada-02.md), [prova pública real](orchestracao/.runtime/login-cadastros-validacao-dev/lume/browser-publico-real02/resultado.json) e [parcial anterior literal](orchestracao/.runtime/login-cadastros-validacao-dev/retomada-20261009T183735Z/historico/login-cadastros-validacao-dev-resultado.md). Prumo ambiente/guarda; Cedro fonte/JAR/API; Lume browser; Vigia confronto independente; Farol consolidação. Nenhum material novo demonstrado, sem homologação operacional001–007/roundtrip de negócio.

**Atualização após disponibilização da conta:** Lucas confirmou login real funcionando; CUA do Farol retornou apps[]/browsers[]/zero abas acessíveis. Não há contradição com sessão existente no computador. [Observação deste acesso](orchestracao/.runtime/login-cadastros-validacao-dev/sessao-disponibilizada-20261009T192151Z/resultado.md); nenhuma senha usada/lida/registrada, nenhuma API/SQL/guarda/app nova. Perfil/alcance da conta ainda não observado, sem autorização de escrita por inferência.

**Fecho por decisão expressa de Lucas:** origem https://1z8126n0-25581.brs.devtunnels.ms/ fornecida pelo responsável; consulta única CUA retornou0 navegadores/abas WMS e resolução `No browser is available`. Sessão/perfil/alcance não observados, sem inferir falha WMS/login/SQL. Lucas relata login funcional e trabalha na frente visual; determinou encerrar sem novas tentativas nem outra ação exigida dele. SQLPASS datado e sete locais aprovados preservados,0/7 critérios operacionais integrais reais. [Fecho autorizado](orchestracao/.runtime/login-cadastros-validacao-dev/origem-https-cua-20261009T194922Z/fecho-autorizado.json). Somente documentação/aviso à equipe; nenhum frontend/backend/estilo/launcher/mapa/processo do usuário alterado, sem sondas/instalação/login/revisões novas. **Rodada encerrada parcialmente; participantes aguardam nova demanda.**

## FE02-DEV-CON01 — timeout do launcher relatado em 09/10/2026

**Diagnóstico local concluído; inicialização real bloqueada:** Lucas apresentou o run `874b0c4b965c4e9f831487a48ec94acd` de 14:27 BRT. Abertura única WMS_DEV/WMSDEV expirou após 30.217 ms em prelogin/handshake (-2/258), sem identidade confirmada, consultas ou backend/frontend iniciados. Serviço SQL Running/PID21812 e listener1433 observados em leitura; isso não comprova resposta SQL. Helper, módulo cliente, SqlClient e hashes TLS coincidem com a guarda aprovada de 12:06 BRT (232 ms); nenhuma causa ou alteração do servidor demonstrada.

**Dependências:** BE02 → FE02, guarda atual de D31-DEV02 e limites D32-DEV04. **Evidência:** [diagnóstico delimitado](orchestracao/.runtime/dev-conexao-20261009-874b0c4b.md). Nenhuma nova conexão, SQL, mudança de aplicação/servidor/runtime ou reinício. **Próximo encaminhamento:** esclarecer se a falha foi observada somente no WMS ou também em outro cliente e obter evidência atual da disponibilidade antes de nova guarda autorizada. Etapas visuais e correções locais concluídas abaixo permanecem concluídas.

## CORR-LOGIN-CAD01 — sete correções concluídas e revisadas localmente, 09/10/2026

**BE04-COR01 → FE03-COR01 concluídos no recorte autorizado:** AUD-CONS-001 a007 corrigidos e revisados independentemente; nenhum achado material local aberto nesta entrega. Login próprio D32/DEV07 e incrementos DS01 preservados. Cedro backend004/005; Lume AUTH/UI e leitores005; Prumo contratos/persistência em arquivo; Vigia revisão; Farol consolidação/canônicos. Atalho006 já corrigido pela frente DS01, com autoria externa preservada e handler atual idêntico por diff.

Provas: frontend253 verdes únicos por composição252+1, tipagem/lint/build e3 testes Chrome com API sintética. Backend66 verdes na fonte MARCO02 e14 novos verdes no delta final MARCO03, separados por versão; builds offline aprovados e JAR não executado. Parecer final confirma os sete critérios. Não é certificação geral de segurança, teste integral da fonte global ou integração real com banco.

[Recibo final e limites](orchestracao/.runtime/login-cadastros-correcoes-resultado.md), [JSON/hashes](orchestracao/.runtime/login-cadastros-correcoes-resultado.json), [matriz individual](orchestracao/.runtime/login-cadastros-correcoes/matriz-problemas-casos.md) e [parecer independente](orchestracao/.runtime/login-cadastros-correcoes/vigia/parecer-final.md).

Fonte/provas datadas: deltas posteriores da frente visual qualificados por comparação/adenda, sem alterar os caminhos revisados dos7critérios; não declarar200fontes atuais globalmente iguais. Geração03 preserva174inputs históricos, incluindo AuditoriaService5801 anterior ao delta0F12; DTO/API/permissão/outputs permanecem iguais por adenda. Mapa AST das correções atualizado semforce/LLM, com backups; comunidades/semântica integral não recalculadas e últimos ajustes visuais externos posteriores qualificados.

Nenhum SQL/banco/HTTPDEV/DDL/migration/PROD/sa/provider novo/alteração servidor/runtime/kill ou restart existente/ETL/Hermes. Auditoria original, FINAL14, TLS01/DEV04, migrations/dados e históricos preservados. Próximo passo: aguardar nova demanda de Lucas; não iniciar aplicação, banco, teste ou macrobloco automaticamente. Homologação real/custo JPA e histórico ambíguo permanecem limites próprios, sem reabrir tarefas antigas.

### Histórico preservado — auditoria e incrementos anteriores

## FE02-DS01 — fundação visual e navegação, 09/10/2026

**FE02-DS01-A02 — detecção automática sem ícone de Sistema:** concluído localmente, por correção expressa de Lucas. Somente o botão circular sol/lua; cada abertura/recarga inicia pelo tema do dispositivo. Escolha manual vale durante o uso e substitui a persistência de A01. Dependência: FE02-DS01-A01; tipagem/lint/builds e 14 casos de navegador aprovados, com inicialização, sistema, teclado, edição e aparência em sete larguras. Login/painel com API interceptada; contratos BE→FE preservados. [Detalhes e limites](docs/design-system/etapa-01.md).

**FE02-DS01-A01 — ajuste solicitado de tema e lateral, histórico anterior a A02:** concluído localmente. Sistema permanece o padrão sem preferência salva; botão circular sol/lua alterna claro/escuro, com retorno ao sistema pelo monitor. “Minimizar menu” na própria lateral recolhe à esquerda de 256 para 64 px, somente ícones; seta direita expande. Dependência: FE02-DS01; contratos BE→FE preservados. Tipagem/lint/formatação/builds e 14 casos de navegador aprovados; recarga, sistema/abas, teclado, formulários preservados, sete larguras e capturas conferidos. API nativa interceptada, sem banco real. [Detalhes e limites](docs/design-system/etapa-01.md). Graphify recusou redução do mapa atual, sem forçar. Próximo passo visual permanece a composição de componentes e páginas.

**Status:** Concluído no recorte visual local. **Dependências:** FE02, FE03 e [design system fornecido](docs/design-system/design.md). Pedido expresso de Lucas: primeira etapa com organização, tipografia, cores, temas claro/escuro, topo e lateral. Implementação visual local autorizada; sem mudança de regras, contratos ou backend.

- [x] Centralizar tokens, fonte Inter local, escala tipográfica e cores semânticas.
- [x] Aplicar tema antes do CSS; a detecção automática e o botão sol/lua seguem o ajuste A02, com escolha manual durante o uso da página.
- [x] Compartilhar topo e lateral entre aplicação real e exercício fictício; conferir menu mobile e teclado.
- [x] Conferir tipagem, lint, regressão pertinente, builds e aparência nos dois temas.

**Evidência:** [entrega, fontes e limites](docs/design-system/etapa-01.md). Tipagem/lint/formatação/builds aprovados; 14 testes de navegador aprovados (12 fictícios + 2 da aplicação real com API interceptada), sete larguras 320–1440 px e revisão de capturas claro/escuro. Regressão inicial374/374; execução posterior401 aprovados/1 falha no teste do novo argumento AbortSignal da frente paralela CORR-LOGIN-CAD01. Não declara check integral atual aprovado nem integração SQL/autenticação real. Graphify recusou redução; mapa oficial preservado.

**Ligação BE → FE:** preserva BE04-AUTH01 → FE03-AUTH01 e os contratos operacionais existentes. **Próximo passo do design system:** componentes compartilhados e composição de páginas em nova etapa. Pendências da frente de autenticação e manutenção do mapa permanecem nos respectivos escopos; detalhes na entrega visual.

## AUD-LOGIN-CAD01 — auditoria concluída com achados; correções aguardam Lucas

BE04-AUD01 → FE03-AUD01, pedido expresso de 09/10/2026. Cedro, Lume, Prumo e Vigia concluíram seus recortes; Farol consolidou. Login próprio D32 e administração de usuários existem; Keycloak/BFF histórico não adotado. Sete achados materiais abertos: um P1 (401 antigo encerra sessão nova) e seis P2 (Long nativo, resposta inválida/sucesso indevido, categoria após normalização, namespace de vínculo, atalho de conteúdo e carregamento após erro). Sem vulnerabilidade crítica/escalada demonstrada; sem aceite geral de segurança ou integração real. [Recibo e propostas](orchestracao/.runtime/login-cadastros-auditoria-resultado.md), [JSON/provas](orchestracao/.runtime/login-cadastros-auditoria-resultado.json).

Matriz individual: 74 contratos HTTP; 1395 instâncias HTTP/frontend, 240 complementos backend e 252 atributos persistentes em 22 entidades, com grãos distintos. Pareamentos JPA-DDL/direitos em arquivo252/252; divergências e limites por fronteira preservados. [Matriz](orchestracao/.runtime/login-cadastros-auditoria/matriz-atributos.md), [atributos completos](orchestracao/.runtime/login-cadastros-auditoria/matriz-atributos.json) e [parecer independente](orchestracao/.runtime/login-cadastros-auditoria/vigia/parecer.md). Não somar reutilizações/contagens como cobertura ou aceite.

Aplicação somente leitura. Provas novas isoladas e sintéticas; nenhuma correção app, SQL/guarda/JDBC/H2/HTTPDEV/provider/launcher/build backend integral/processo real/segredo/ETL/Hermes nesta auditoria. Fonte inicial411:404iguais/7deltas DEV06/DEV07 datados; nenhum freeze global presumido. Os15focais Origin são DEV06; DEV07 posterior lido separadamente e confiança dos headers/topologia real é limite. TLS01/DEV04, FINAL14, migrations, dados e processos existentes preservados.

Próximo passo: Lucas definir o início das correções propostas; nenhum job, refatoração, auth nova ou processo iniciado automaticamente no fecho. Estado/regras D32-DEV07 abaixo continuam preservados como incremento distinto. Registro do fechoUTC: 2026-10-09T15:53:57.298249+00:00.

### Estado anterior preservado — D32 e demais entregas

## D32 — login e administração de usuários (09/10/2026)

**D32-DEV07 — túnel variável e validação de senha conferidos localmente.** Configuração agora contém somente `modo: tunnel`, sem URL pública fixa. Origem HTTPS deve corresponder exatamente ao destino atual no proxy loopback; alias localhost, CSRF e cookies Secure preservados. Console orienta usar o link atual da porta no VS Code. Trinta e dois testes backend passaram, incluindo três hosts sem reinício, troca de senha/novo login, recusa de outra origem e CSRF; sete testes frontend, lint/tipagem/build e nove verificações PowerShell passaram. JAR `target-auth-d32-dynamic` registrado no launcher. Frontend explica e valida senha diferente da atual e comprimento12–128 antes do envio. Próximo passo: usuário reinicia o BAT uma vez para carregar esta versão; novas mudanças apenas na URL do túnel dispensam reinício. Causa do400 real ainda não confirmada: aguardando mensagem da tela, sem senha. [Resultado e limites](orchestracao/.runtime/login-d32/dev07-resultado.json). BE04-AUTH01 → FE03-AUTH01, mesmo escopo D32.

**D32-DEV06 — acesso pelo Dev Tunnel validado localmente; reinício pelo usuário pendente.** Relato403 em `https://1z8126n0-25581.brs.devtunnels.ms`; diagnóstico sem senha na rota CSRF confirmou `ORIGEM_INVALIDA`, antes da autenticação. Configuração pública em `infra/dev/acesso-dev.json`, origem HTTPS exata, cookies Secure e alias de Origin localhost restrito ao proxy loopback/perfil DEV. Não amplia para outros túneis nem desliga CSRF. BE04-AUTH01 → FE03-AUTH01: 20 testes backend, incluindo HTTP interno de proxy/H2, passaram; login/renovação, cookies Secure, outra origem403 e CSRF inválido403 conferidos. Treze verificações de configuração e sintaxe PowerShell passaram. JAR `target-auth-d32-tunnel` preparado no recibo do launcher. Próximo passo: usuário encerra o console antigo, executa o BAT e entra pelo link do túnel; login real novo ainda não conferido. Os 11 usos obsoletos `asText()` nos dois testes relatados foram substituídos por `asString()`; compilação aprovada. [Resultado e limites](orchestracao/.runtime/login-d32/dev06-resultado.json). Auditoria paralela acima preservada.

**D32-DEV05 — correção de senha e console validados localmente; reinício pelo usuário pendente.** O construtor legado do encoder Spring usava SHA1, enquanto `PrepararLogin.java` gera PBKDF2-SHA256. Algoritmo agora explícito, com regressão que executa o configurador real e fixture independente via JCA. Falha reproduzida antes da correção; 20 testes backend, incluindo navegador/H2, e build frontend aprovados depois. Hash/configuração protegida existentes preservados. Console com quatro etapas, horários/cores, painel de acesso e orientação de falha; favicon SVG respondeu200. Novo JAR `target-auth-d32-loginfix` registrado no launcher. Instância atual do usuário preservada. [Evidências e limites](orchestracao/.runtime/login-d32/dev05-resultado.json). Atualização graphify recusada pela guarda de redução; sem forçar.

**D32-DEV04 — ativação concluída; `iniciar-dev.bat` validado em ambiente real.** V11 aplicada via Flyway somente WMS_DEV; 22 direitos mínimos adicionados ao WMSDEV (311 → 333), com V1–V10 e dados preservados. Guarda atual passou: TLS validado, identidade WMSDEV/WMS_DEV, 68 tabelas/716 colunas e histórico V11 íntegros. Backend real `sqlserver-dev`, migrations desligadas e frontend real subiram pelo BAT no [run ce0bf9dd](orchestracao/.runtime/frontend-integracao-dev-runs/ce0bf9dd46b3423ba148782a52cf212f/launcher.json). Chrome confirmou login desktop/360px, CSS sem violações CSP, API/status200, chave pública200 e acesso anônimo a clientes401. Corrigido CSS de login para folha externa no DEV. `npm run check`: 359 testes, tipagem, lint e build aprovados.

Próximo passo vigente: o usuário encerra seu console atual com Ctrl+C, executa `iniciar-dev.bat` novamente, atualiza `http://127.0.0.1:25581` e entra com a conta principal e a senha já configurada. Não repetir o configurador. A senha real não foi usada nos testes do agente: primeiro login, troca de senha e operações autenticadas em SQL Server ficam para a validação pelo usuário. A ativação DEV04 permanece como prova histórica de banco/subida, não de login autenticado. [Ativação e limites](orchestracao/.runtime/login-d32/dev04-resultado.json).

### Histórico da implementação D32 e impedimentos superados

**BE04-AUTH01 / FE03-AUTH01 — implementação e validação local concluídas; ativação WMS_DEV pendente.** Pedido direto de Lucas neste chat: login próprio WMS, JWT, conta principal `desenvolvedor@rodogarcia.com.br` protegida contra exclusão, painel de usuários, administradores delegados, senhas temporárias e troca obrigatória no primeiro acesso e após redefinição. Dependências: BE04, FE03 e integração D31-DEV02. A autorização de implementação substitui a pendência de escolha AUTH; a proposta Keycloak/BFF anterior não foi adotada. Permissões operacionais por perfil/cliente/armazém continuam no backend. Senha inicial não integra fontes, documentação ou logs.

Evidências: 18 testes backend direcionados e uma jornada automatizada de navegador com quatro verificações completas, HTTP/JPA/H2 efêmero; 359 testes frontend, tipagem, lint e build; 9 verificações de contratos/material fictício e 20 do helper. Capturas desktop/360px e teclado conferidos. A jornada visual detectou e validou a correção da chamada nativa `fetch`. [Resultado local](orchestracao/.runtime/login-d32/resultado.json), [guia e contratos](docs/43-login-e-administracao-de-usuarios.md). JAR próprio gerado; nenhuma conta real criada. Atualização graphify recusada pela proteção contra redução do grafo; mapa anterior preservado, sem `--force`.

Próximo passo: obter liberação específica para nova verificação e aplicação da V11 e dos 22 direitos mínimos WMSDEV exclusivamente em WMS_DEV, seguida da jornada integrada pelo `iniciar-dev.bat`. Configuração inicial protegida concluída pelo usuário. Migration, contrato e launcher preparados; V1–V10 e oráculo D29 preservados. Testes locais não comprovam integração WMS_DEV; BE04-AUTH01/FE03-AUTH01 permanecem em validação de ambiente até essa evidência.

**D32-DEV03, diagnóstico em 09/10/2026:** execução manual do usuário [8a778edc](orchestracao/.runtime/frontend-integracao-dev-runs/8a778edc4be045ffb83c4b3fbb24d98a/guarda.json) confirmou presença do material de login, mas a abertura SQL expirou em 5,26 segundos, código -2/258, fase prelogin/handshake; zero SELECT e BE/FE não iniciados. Inspeção do Windows constatou serviço MSSQLSERVER ligado e listener1433/PID21812; isso não comprova conexão SQL funcional nem a causa do timeout. Cliente ajustado de 5 para 30 segundos, mantendo conexão única, TLS validado e zero retries; launcher agora explica a classe do bloqueio. Sintaxe e 20 testes isolados do helper passaram. Nenhuma nova conexão foi executada pelo agente neste diagnóstico; nova verificação e ativação DEV aguardam liberação específica apresentada ao usuário.

Organização D32 em 09/10/2026, a pedido de Lucas: configurador movido da raiz para `infra/auth/configurar-login-dev.bat`; chamada do PS1 e orientação do launcher ajustadas. Caminhos e sintaxe conferidos estaticamente, sem executar configuração de senha ou banco. O bloqueio relatado `AUTH_NATIVE_CONFIGURATION_REQUIRED` indica ausência da configuração protegida local; a ativação SQL continua pendente conforme o próximo passo acima.

## Histórico anterior à autorização de login D32

## Resultado atual D31-DEV02-TLS01 — SQL/TLS aprovado; AUTH pendente

Handoff expresso de Lucas absorvido por Farol em 09/10/2026. Provas do apoio Codex: guarda WMS_DEV/WMSDEV aprovada em 2026-10-09T11:56:41.5843363Z, 26/26 critérios; SqlClient e JDBC PASS, TLS validado (`Encrypt=true/Mandatory`, `TrustServerCertificate=false`), zero alterações SQL. O helper atual corresponde ao SHA D2FF82F7241F448FA5FA836A732A1F9CB2ECE2137B6735DAFDBD1AD4F06B35A9. A baseline D29 de permissões/catálogo/histórico foi preservada. [Provas e proveniência](orchestracao/.runtime/frontend-integracao-dev-tls-handoff-20261009.md).

SQL/TLS resolvido conforme estas provas datadas; pin/recibo antigos e recusas anteriores ficam históricos, sem inferir alteração servidor/sa. Regra permanente em [AGENTS.md](AGENTS.md): confiança corresponde ao processo atual; renovação segura autorizada, sem recaptura automática. Guarda normal futura exige atualidade e pertence à execução integrada após resolução AUTH, não ao recebimento deste handoff.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, Keycloak/BFF não aprovado. Backend/API não iniciados; URL integrada=null, roundtrip=false, entrega integrada aberta. Mapa anterior preservado, atualização estrutural pendente após rejeição; nenhum mapa/SQL/guarda/JDBC/build/teste/processo executado por Farol nesta absorção. FINAL14 local aprovada; D30 geral aberta, aceiteLocalIntegral=false; C06/C07/C10 requeridos impedidos históricos preservados. [Recibo vigente](orchestracao/.runtime/frontend-integracao-dev-resultado.json).

## Histórico preservado — conteúdo anterior à absorção Farol TLS01

## Apoio atual D31-DEV02-TLS01 — certificado renovado e guarda real aprovada

Em 09/10/2026, por pedido direto do responsável para ajudar Hermes WMS, o Codex deste apoio renovou a confiança privada pelo helper existente do runtime. O recibo ainda era do PID65088; o SQL atual PID21812 apresenta outro certificado fallback. SqlClient e JDBC13.4/JDK21 passaram em leituras reais WMS_DEV/WMSDEV, com TLS validado. A guarda cliente foi corrigida para verificar recibo/PID/início/hashes/validade atuais antes da abertura e confirmar o processo depois, preservando a baseline D29 de direitos/catálogo/histórico. Guarda completa em 11:56:41UTC:26 critérios aprovados,64 tabelas/687 colunas,V1–V10 e permissões conferidos; uma abertura e12SELECTs de identidade/metadados, zero negócio/DML/DDL.

TLS resolvido neste recorte; AUTH dos usuários da aplicação continua pendente, sem backend/API/URL integrada. Nenhum sa/PROD/grants/servidor/serviço/trust global/migrations alterado. [Relatório e limites](orchestracao/.runtime/tls-apoio-hermes-wms-20261009/LEIA-HERMES-WMS-CERTIFICADO-SQL.txt), [guarda atual](orchestracao/.runtime/tls-apoio-hermes-wms-20261009/certificado-sql-wms-guarda.json). Encaminhamento direto ao Hermes WMS/Farol indisponível neste chat externo; relatório e provas deixados no runtime WMS para a equipe. Não repetir rodadas encerradas nem considerar a integração concluída por este apoio.

## Histórico preservado — estado anterior ao apoio TLS01

## Resultado atual D31-DEV02 — nova guarda SQL bloqueada TLS; AUTH separada

Após relato Lucas “sql server voltou”, Prumo verificou canal próprio independenteAUTH: iníciohelper2026-10-09T11:30:14.2409217Z,Open2026-10-09T11:30:16.2451044Z,fim2026-10-09T11:30:16.5307190Z,PID29060,UMAOpen258.9705ms/exit20 filho capturado. TLS0x800B0110/native-2146762480 faseABERTURA antesSELECT; queries0/handshakefalse, alvo/identidade/permissões/catálogo/histórico não confirmados. Relato não éPASS nem comprova estado sa/disponibilidade geralSQL. [Resultado](orchestracao/.runtime/frontend-integracao-dev-retomada-sql-20261009.md), [guarda](frontend/evidencias/frontend-prumo-dev02-guarda-retomada-20261009.json).

Interrompido sem segundaOpen/BE/API/proxy/sa/PROD/bypass/TLSglobal/DDL/migrations/grants/servidor/restartkill/ETL/Hermes. Helper1376 antes/depois igual e credencialWMSDEV protegida em memória descartada; segredo não publicado. Vigia conferiu recibos em leitura sem novaSQL. Responsável TLS/SQL apresenta encaminhamento seguro para a recusa atual; causa/peer não comprovados, nenhum ajuste compartilhado ou nova sonda automática.

AUTH continua decisão material D31-DEV02-AUTH-DECISAO01, independenteSQL; Keycloak/BFF NÃO aprovado nem criado. Nenhum backend/URL integrada sem autenticação real adequada. Mesma demandaDEV02, sem missão duplicada; auditoriaSA/FINAL14/MARCO03/históricos preservados. FINAL14 local aprovada; D30geralaberta, aceiteLocalIntegral=false,C06/C07/C10 requeridos impedidos históricos. RegistroUTC2026-10-09T11:38:25.736665Z.

## Histórico preservado — estado anterior à nova verificação SQL

## Resultado atual D31-DEV02 após AUD-SQL-SA01 — BAT real40; decisão AUTH e TLS pendentes

BAT WMS real executado após auditoria em 2026-10-09T02:22:04.2682459Z: exit40 AUTH_CONFIGURATION_MISSING das três referências públicas; SQLGuard/BE/FE=false, URL=null, roundtrip=false, sem mock. Portas/proprietários observados iguais antes/depois; nenhuma nova Open ou alteração do SQL Server. [Run atual](orchestracao/.runtime/frontend-integracao-dev-runs/e169e141b28d4537b02a2a33cd9a8463/launcher.json), [consolidação](orchestracao/.runtime/frontend-integracao-dev-apos-auditoria-adenda.md).

Referência EXATA dashboards-etl examinada por Lume:14 fontes técnicas/10WMS e24 hashes conferidos. Organização BE→readiness→FE é aproveitável; login próprio HMAC daquele projeto não corresponde a ResourceServerRS256/JWK WMS. Nenhum launcher alheio executado ou env/conexão/conta/credencial/segredo/controle copiado; não portar killporporta/limpalogs/Securefalse/CSRFdisable/replay401. Keycloak/BFF NÃO aprovado nem criado. Nenhuma fonte própria OIDC localizada nos canais examinados; ausência delimitada, não universal. Não há loaderfix demonstrado.

Decisão material ÚNICA continua D31-DEV02-AUTH-DECISAO01: proposta Keycloak DEV exclusivoWMS+BFF, dono de identidade designado, API RS256 preservada. Pedido exige decisão antes provider/servidor novo ou mudança de autenticação. Sessão operacional FE/catálogos/adapter ainda necessários após contrato aprovado. TLS SQL é impedimento independente0x800B0110 antesSELECT na única Open; sem causa/peer comprovado e sem segunda sonda. Responsável TLS/SQL apresenta encaminhamento seguro; nenhuma mudança servidor/trust global autorizada. [Proposta](orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md).

AUD-SQL-SA01 encerrada em leitura: D26identidadeWMSDEV/direitos eD27V10 históricos comprovados; nenhuma mudança sa/senha/authmode demonstrada no recorte. SQLPID65088→21812/início07out20:57:17.273Z sem autor/causa. ERRORLOGnegado/18456state/reason desconhecidos; operador autorizado obter extrato e flags pela sessão Windows existente, semsenha/novaSQLagente/reparo. [Auditoria](orchestracao/.runtime/sql-auditoria-login-sa-resultado.md).

Mesma DEV02 aberta, não novo macrobloco. Sem repetição FINAL14/suítes antigas, Keycloak/provider, SQL/sa/PROD/DDL/grants/restartkill/ETL/Hermes. Lume referência; Prumo OS/registro; Cedro execuções/AUTH; Vigia revisão; Farol BAT/canônicos/consolidação. FINAL14 local aprovada preservada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos. Registro UTC 2026-10-09T02:26:22.791284Z.

## Histórico preservado — auditoria SA e contexto DEV02 anteriores

## Resultado atual AUD-SQL-SA01 — auditoria encerrada em leitura; causa sa não determinada

Efeitos históricos WMS comprovados: D24 validação administrativa sa; D26 criação de login/usuário/direitos WMSDEV; D27 V10 WMS_DEV. Prumo por atribuição documental, operador Windows não individualizado. Nenhuma execução/efeito de mudança de sa/senha/authmode demonstrado no recorte; não é garantia universal. Processo SQL mudou: PID65088 baseline07out20:38:51Z → PID21812 criado07out20:57:17.273Z, sem autor/causa identificados. Registro gravado LoginMode2/ForceEncryption0/certificado vazio sem baseline comparável, não flags sa/peerTLS.

18456 sa/state/reason atuais indisponíveis: Parameters/ERRORLOG negados e Windows sem evento utilizável; não significa ausência de falhas. Próximo passo do operador já autorizado: extrato nativo sanitizado e consulta única de flags pela sessão Windows existente, sem senha no chat/nova conexão do agente/reparo. ZERO SQL/Open/sa/DPAPI/BE/API/grants/servidor/restart/kill na auditoria. [Resultado](orchestracao/.runtime/sql-auditoria-login-sa-resultado.md), [fontes/hashes](orchestracao/.runtime/sql-auditoria-login-sa-resultado.json).

Lucas determinou retomar a MESMA DEV02 após os achados, confrontando somente referência técnica dashboards-etl. Keycloak/BFF não aprovado; integração ainda exige BAT real, guardas WMS_DEV/WMSDEV/TLS e roundtrip. FINAL14 e demais históricos preservados. Fecho UTC 2026-10-09T02:20:38.625201Z.

## Contexto anterior preservado — D31-DEV02 ainda pendente

## Resultado atual D31-DEV02 — integração real pendente; decisão AUTH e impedimento TLS atuais

**Run real de Lucas:** iniciar-dev.bat em 2026-10-09T01:14:35.6191414Z → exit40 AUTH_CONFIGURATION_MISSING das três referências OIDC. Guarda SQL não invocada; backend/frontend não iniciados; URL integrada e roundtrip inexistentes. [Recibo observado](orchestracao/.runtime/frontend-integracao-dev-runs/7483449c3bcf41608496bc8eb35e3982/launcher.json).

**Origem AUTH investigada:** Prumo/Cedro verificaram presença apenas: 9/9 ausentes em Process/User/Machine, zero valores publicados. Fontes públicas WMS definem ResourceServer RS256/Bearer e placeholders; canais locais consultados são SQL. Nenhuma fonte OIDC própria previamente aprovada/configurada foi localizada nesses escopos/fontes; isso não prova ausência universal corporativa. Não há correção de carregamento demonstrada. O frontend real ainda precisa da sessão operacional, além das três referências.

**Uma proposta, não aprovação:** Farol propõe Keycloak DEV exclusivo WMS, gerido por responsável de identidade designado, com sessão BFF same-origin no backend; tokens/secret fora do navegador, API RS256/Bearer stateless preservada. Provider/contas/BFF não criados. Lucas precisa decidir este arranjo antes de provisionar ou mudar autenticação, como exigiu no pedido. [Descoberta e proposta](orchestracao/.runtime/frontend-integracao-dev-auth-proposta.md), [fontes/hashes](orchestracao/.runtime/frontend-integracao-dev-auth-proposta.json). Não pedir três valores técnicos soltos nem usar identidades de outros projetos.

**SQL/TLS independente:** única Open atual em 2026-10-09T00:51:02.8522544Z, PID32144, 454,7449ms, recusou TLS0x800B0110 antes SELECT. Alvo real/identidade/permissões/catálogo/histórico não confirmados; zero SELECT/API/backend. PEM/DER configurados no cliente não são peer atual nem comprovam causa. Sem segunda abertura/bypass/recaptura. Responsável TLS precisa apresentar correção segura; aprovar AUTH não resolve nem libera automaticamente SQL. [Guarda atual](frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json).

**Preparo local revisado, entrega integrada aberta:** launcher único REAL/default, frontend sem fallback, helper/readiness BE e guardas foram implementados e revisados. Vigia encerrou achados locais001–003; FE CORE01 tem188 fontes/63 focais, tipo/lint e dois builds locais; browser estático4, sem API. Cedro20 focais isolados; launcher AUTH4 green e cleanup10 green. Estas provas não comprovam autenticação ou WMS_DEV real. FINAL14 não foi repetida. Só arquivos/recibos próprios dos cinco WMS; sem agentes/conexões novos, Hermes ou ETL. Nenhuma alteração de fonte frontend após CORE01 por este registro.

Farol: launcher/canônicos/recibo/mapa/nota; Cedro: backend/config/readiness/AUTH origem; Lume: frontend/sessão/cliente; Prumo: guarda/TLS/canais; Vigia: revisão independente. Processos históricos5178/5188/5189 preservados; não são URL integrada desta entrega. Após decisões e pré-requisitos seguros no escopo autorizado, será necessário executar BAT real e comprovar login/consulta browser→API→WMS_DEV/WMSDEV. Sem PROD/sa/DDL/migrations/grants/servidor/sharedruntime/killrestart existentes/publicação/commit/push/ETL/rotinas.

Recibo vigente: [JSON](orchestracao/.runtime/frontend-integracao-dev-resultado.json), [MD](orchestracao/.runtime/frontend-integracao-dev-resultado.md). Leitura mínima: AGENTS → states → docs/09 → docs/19 → recibo → proposta AUTH/guarda e parecer conforme pertinência. FINAL14 local aprovada preservada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos históricos. Base frontend MARCO03 local preservada; não equivale ao sistema inteiro ou integração real. Registro UTC 2026-10-09T01:30:19.545206+00:00.

## Histórico preservado — fecho local anterior e limite DEV01

## Resultado atual D31 — base local concluída e revisada; integração real impedida

FIM_FRONTEND_LUCAS_20261008: base React/TypeScript funcional concluída e aprovada na revisão local integrada de Vigia. Fonte SHA256 C58B64105A7B3232F314D56F8150CCBD6CE1530B6FE4D33837BA46AD3DD4F922 (179 arquivos); tipagem/lint/build, 299 testes locais e 16 Chromium, todos exit0. As 22 parcelas essenciais usam provas compartilhadas; smokes contratuais não equivalem a jornadas integrais. Organização, legibilidade, acoplamento e testabilidade revisados; CSS preto/branco/azul efetivamente aplicado no navegador desktop/coletor. [Resultado JSON](orchestracao/.runtime/frontend-resultado-lucas-20261008.json), [MD](orchestracao/.runtime/frontend-resultado-lucas-20261008.md), [parecer](orchestracao/.runtime/frontend-vigia-parecer-lucas-20261008.json). Registro UTC 2026-10-09T00:08:45.382787+00:00.

**Abrir frontend:** http://127.0.0.1:5189, processo próprio Lume PID19856, exercício fictício. Vínculo de fonte e observações em [recibo de execução](frontend/evidencias/frontend-lume-exercicio-marco03.json). npm run dev usa vite.dev.config.ts: [guia](frontend/docs/desenvolvimento-dev.md). 5178/5188 preservados; 5188 é histórico com FE-VIG-008 e não é a versão corrigida. API/SQL, provider/contas/tokens/CORS reais, usuários sem rota, equipamentos/fiscal/piloto permanecem pendentes; aprovação local não homologa o sistema inteiro ou FE13 real.

**DEV01 real BLOQUEADO G01:** falta prova atual de resolução segura D29. [Guarda Prumo](frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json): guardaRealAprovada=false. Canal WMSDEV conferido apenas por metadados; zero leitura de segredo/DPAPI, SQL/SELECT/API/backend/fixtures. Configuração DEV preparada, modo real fechado. Responsável SQL deve fornecer evidência atual; depois avaliar nova guarda WMS_DEV/WMSDEV/TLS/permissões/catálogo/histórico. Nenhuma sonda/fallback ou intervenção em servidor/acessos/runtime compartilhado autorizada por este fecho.

Lume: integração frontend/checks; Cedro: FE12 e preparo mínimo backend DEV; Prumo: CSS/DEVconfig/guarda; Vigia: revisão independente; Farol: canônicos/recibo/mapa/nota. Fonte congelada, bloco local encerrado, somente exercício fictício próprio aberto. Aguardar demanda/decisão ou prova segura SQL; sem retomada automática. FINAL14 local preservada/aprovada; D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos. Históricos/IDs/checksums preservados, sem repetir backend/FINAL14 ou tarefas reconciliadas.


## MAN01 — proteção do versionamento, 08/10/2026

Concluído por pedido direto de Lucas: `.gitignore` ampliado para runtime, credenciais locais, dumps, relatórios automatizados e snapshots/pacotes de ensaios. Retirados apenas do índice 274 arquivos gerados: 80 fixtures (incluindo 43 CLIXML) na primeira entrega e 194 artefatos adicionais na revisão aprofundada; todos preservados no disco e no histórico existente. Conferido com `git check-ignore`, `git ls-files -ci --exclude-standard` (zero restante) e `git diff --check` no ignore. Código BE/FE, migrations oficiais, lockfile e `.env.example` permanecem versionáveis; configuração Spring local sem segredo continua fonte do projeto. Sem aplicação, banco, commit ou push. Fase e pendências BE/FE abaixo permanecem próprias.

Revisão aprofundada solicitada em 08/10/2026: inventário e varredura por padrões de segredos no conteúdo elegível, no índice e nos três commits alcançáveis localmente; última varredura completa cobriu 1.306 arquivos de texto e 49 binários inventariados antes da exclusão final das capturas PNG geradas. Evidência sanitizada em `.runtime/git-audit-report.json`, complemento de literais históricos em `.runtime/git-audit-history-literals.json`. Alertas de texto correspondem a variáveis/parâmetros, placeholders Maven, fixtures explícitas e senha de truststore da fixture HTTPS gerada; nenhum segredo real confirmado pelos padrões examinados. Incluídos snapshots antigos, configurações Maven geradas, JSONs de execução, HAR, bases locais, material de chave e JAR com extensão histórica (78 MB). Limites: ferramenta de padrões local, sem gitleaks/trufflehog instalados; binários originais de requisitos e fixture XLSX não tiveram inspeção integral de conteúdo, objetos inalcançáveis/reflogs e histórico remoto não consultados. Exclusão do índice não expurga os 43 CLIXML históricos. Próximo passo deste item: revisar o diff pretendido antes de commit/publicação; não tratar a varredura como garantia absoluta de ausência de segredos.

## D31 — frontend funcional autorizado por Lucas em 08/10/2026 (registro intermediário preservado; resultado atual acima)

**D31-DEV01 — incremento expresso de Lucas; integração real BLOQUEADA em G01:** “ja deveria juntar o frotend modo run dev com backend e sql server _DEV sim para ser possivel testar enquanto esta sendo feito as atualizacoes”. Autorizado preparar desenvolvimento integrado frontend run dev + backend + SQL Server exclusivamente WMS_DEV, identidade própria WMSDEV restrita. Prumo precisa comprovar resolução segura atual do incidente D29 antes da nova guarda mínima real de alvo/identidade/TLS/permissões/catálogo/histórico. A autorização, listener ativo e provas antigas não comprovam recuperação. [Guarda atual](frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json), SHA256 9135BC92A2C1BF3DF012D47B8EA19E196B835D0A153F840F10D3AA34F15C9005: guardaRealAprovada=false; nenhuma prova atual segura localizada nas fontes direcionadas; canal WMSDEV existente/ACL compatível apenas por metadados. Zero abertura SQL/SELECT/HTTP/backend/fixtures neste recorte. Cedro prepara exclusivamente perfil/guia backend DEV; Lume prepara frontend real/proxy/baseURL/run dev sem segredos; nenhum backend com SQL ou FE→API com SQL será iniciado enquanto bloqueado. Após pré-requisitos comprovados: somente processos próprios, consultas primeiro; escritas fictícias pertinentes somente HTTP/API rastreável. Sem DDL/Flyway pendente/mutação automática do schema, SQL de fixtures/reset/DELETE, PROD/sa na aplicação, alteração de servidor/acessos/runtime compartilhado, kill/restart existentes ou reabertura FINAL14. Responsável SQL deve fornecer prova atual de recuperação segura; partes frontend independentes continuam. Registro de incorporação: 2026-10-08T21:35:56.890749+00:00; horário do pedido não presumido. [Registro do incremento](orchestracao/.runtime/frontend-farol-dev01-lucas-20261008.json).

**CSS aplicado — prova Prumo encerrada:** link CSS externo no index, import JS duplicado removido, CSP preservada; 30 capturas/estados DEV e BUILD em 1440/768/390 px, HTTP200 text/css, computed Arial/margin0/header azul, zero violações CSP/erros de console/overflow do documento/API/externos. [Recibo](frontend/evidencias/frontend-prumo-css-lucas-20261008.json), SHA256 2453DC770565379D01BDC21635F134080A578767418739C9200757010FA5F608. Diagnóstico reproduziu HTML cru no DEV devido à injeção CSS inline bloqueada; build anterior já estilizado. Não identifica a URL vista por Lucas. O defeito de navegação do skip link é separado, corrigido por Lume e ainda aguarda reconferência; CSS aprovado na fatia não aceita teclado global nem a entrega inteira.

**Fase atual: implementação da base frontend FE01–FE13 em andamento**, por demanda expressa FIM_FRONTEND_LUCAS_20261008. React com TypeScript em frontend/, visual básico preto/branco/azul, sem rodada estética. [Demanda integral](orchestracao/.runtime/frontend-demanda-lucas-20261008.txt) e [distribuição/limites](orchestracao/.runtime/frontend-farol-escopo-lucas-20261008.json). Posse atual distribuída por autorização expressa de Lucas: Cedro assume somente frontend/src/modules/regularizacao/ e testes próprios FE12; Prumo, estilos/entrypoint (src/styles/, styles.css, main.tsx, index.html) e prova do CSS; Lume mantém os demais arquivos frontend, contratos/cliente/workflow/agregadores e integração/checks finais. Vigia revisa em leitura fatias estabilizadas e artefatos próprios, em paralelo. Farol mantém canônicos, decisões, continuidade, mapa e recibos centrais. A abertura inicial não havia enviado trabalho a Cedro/Prumo; a distribuição paralela e o incremento DEV01 acima são os estados atuais.

**Prioridade expressa de Lucas — CSS aplicado e execução paralela:** “nenhum css de fato foi aplicado sendo que tem css, eu falei pra aplicar so uma base fina, e nao deixar html puro, e precisamos adiantar rapido, coloque outros agents para adiantar outras coisas”. Prumo confronta imports/entrypoint/asset e DOM/computed styles em navegação real desktop/coletor; corrige a base fina preto/branco/azul, legibilidade/espaçamento/alinhamento/controles/tabelas/feedback/foco/responsividade, sem refinamento estético. Cedro completa FE12; Lume os demais recortes; Vigia reconfirma fatias estáveis sem esperar fecho global. Handoff imutável F49F86454F8852DDF334C71A04E4EBD624968B28892D62FAAA70883711E2220B; formatter anterior encerrado, donos liberados sem concorrência. [Distribuição e fronteiras](orchestracao/.runtime/frontend-farol-distribuicao-paralela-lucas-20261008.json).

**Escopo concreto:** telas administrativas e fluxos de coletor segundo contratos atuais; navegação, formulários, consultas/ações, carga/vazio/erro/sucesso, prevenção de envio repetido, acessibilidade/teclado e responsividade. Cliente HTTP central e DTOs precisos; backend continua responsável por regras, disponibilidade, permissões e cálculo. Configuração externa sem segredos; nenhuma rota ou autenticação inventada. Provedor JWT/CORS real e equipamentos permanecem dependências próprias. Referências reais inventariadas: um PDF de especificação, dois DOCX e README; originais preservados.

**Validação autorizada:** instalação/build/tipagem/lint/testes e exercício do frontend local próprio. Respostas fictícias explicitamente identificadas para ensaio isolado não comprovam integração real. Fonte, comandos/logs/hashes, cobertura concreta BE→FE e revisão independente serão consolidados; FE13 real com API/dispositivos permanece pendente enquanto impedida. Não concluir pela existência de scaffold, formulário genérico ou contagem de testes.

**Preservação e limites:** FINAL14 backend LOCAL concluída/revisada/aprovada, sem faltante local executável conhecido; não repetir tarefas/testes/build backend. D30 geral aberta, aceiteLocalIntegral=false, C06/C07/C10 requeridos impedidos conservados. D29/317nativas/6equipamento/45externas/47históricas/corte temporal e históricos/IDs/checksums preservados. O limite inicial sem SQL/HTTPDEV foi incrementado exclusivamente por D31-DEV01 acima, condicionado à resolução segura e nova guarda; a integração real permanece bloqueada. Nenhuma migration/DDL/PROD/sa/runtime compartilhado/restart/kill existentes/fiscalreal/dispositivos/publicação/commit/push/ETL/rotina/callback ou ask Hermes autorizado. Processos próprios FE e, após guardas, BE DEV permitidos.

**Critério de fecho D31:** base funcional entregue e exercitada; contratos/documentação coerentes; parecer independente sobre fonte atual; limitações reais delimitadas. Recibo final somente [JSON](orchestracao/.runtime/frontend-resultado-lucas-20261008.json) e [MD](orchestracao/.runtime/frontend-resultado-lucas-20261008.md). Registro iniciado em 2026-10-08T17:55:54.3120267Z. Próximo passo: Lume implementa/valida, Vigia revisa fonte estável; Farol consolida evidências e encerra aguardando decisão de Lucas.

**Revisão do MARCO01 em 08/10/2026:** tipagem/lint/211 testes/build/oito jornadas de navegador passaram também em cópia independente, mas três focais novos de Vigia reproduziram defeitos: FE-VIG-002 (vínculo de pedido e reserva/derivados), FE-VIG-003 (constraints de elementos primitivos de listas) e FE-VIG-004 (null em resposta primitiva obrigatória). O MARCO01 não recebeu aceite local. Lume preserva o marco e corrige exclusivamente frontend/ para novo freeze/provas/MARCO02; Vigia reconfirma as correções antes do fecho. Organização estrutural e recortes anteriores foram tratados, sem compensar os três defeitos. O mapa em execução é intermediário, sem aceite de comportamento; será reconciliado com a fonte final. FINAL14 permanece intocada.

**Suficiência das jornadas locais (revisão 8):** além dos três defeitos, o parecer preserva parcelas operacionais locais ainda não demonstradas em FE04/05/06/08/09/10/11/12, especialmente retirada/retorno/devolução em FE10. Lume recebeu complemento focal da mesma demanda para resolver percursos essenciais e provar comandos, referências, revisões e feedback; Vigia delimita suficiência em artefatos próprios. Formulários, mapa de 161 rotas e mocks isolados não aceitam passos ausentes. Integração real permanece requisito separado. [Confronto e encaminhamentos](orchestracao/.runtime/frontend-farol-suficiencia-jornadas-20261008.json).

**Correção expressa de Lucas nesta mesma demanda (registro 2026-10-08T18:44:36.079090+00:00):** “estou achando o codigo muito mal organizado, cuidado”. Organização, legibilidade, acoplamento e testabilidade passam a critério de entrega: separar telas/módulos, componentes, contratos/HTTP, hooks e estilos por responsabilidade concreta; corrigir concentração, duplicação e abstrações sem uso, preservando comportamento. Lume corrige; Vigia revisa explicitamente a organização na fonte estável; verificar frontend após as correções. Sem rodada estética, missão duplicada ou ampliação dos limites. Base não será declarada pronta com desorganização material conhecida. Inspeção e snapshots em [D31-ORG01](orchestracao/.runtime/frontend-farol-organizacao-20261008.json).

## Histórico anterior preservado — FINAL14 e demais fases

A demanda D31 inicia frontend expressamente; textos antigos “frontend não iniciado/aguarda demanda” conservam sua data e não descrevem mais esta nova fase. A aprovação e os limites backend históricos permanecem inalterados.

## Resultado atual — FINAL14 local encerrada; registro de retomada dos chats

**Pedido de Lucas registrado em 08/10/2026 14:32:23 -03:00 (UTC: 2026-10-08T17:32:23.3521232Z). Escopo desta entrega: somente documentação e handoff.** Lucas informou que já reiniciou seu chat Farol antes do pedido de ajuste; esta entrega ocorre no chat reiniciado. O usuário reinicia os chats; o agente não reinicia nada. O bloco LOCAL da fonte FINAL14 está **concluído, revisado e aprovado**, nos predicados, canais, oráculos e limites inventariados. **Nenhum faltante local executável concreto conhecido.** A D30 geral permanece aberta: aceiteLocalIntegral=false, D30Fechada=false; C06/C07/C10 mantêm parcelas requeridas impedidas no denominador. Isso não declara o sistema inteiro concluído nem frontend testado.

Fonte atual: [snapshot de retomada](orchestracao/.runtime/d30-retomada-apos-reinicio-chats.md), [JSON com caminhos e SHA-256](orchestracao/.runtime/d30-retomada-apos-reinicio-chats.json), [recibo parcial FINAL14](orchestracao/.runtime/d30-resultado-parcial-final14.json), [matriz final14](orchestracao/.runtime/d30-matriz-requisito-evidencia-limite-final14.json), [parecer geral Vigia14](orchestracao/.runtime/d30-vigia-final-geral14-parecer-v01.json), [delta documental revisado](orchestracao/.runtime/d30-vigia-delta-documental-v91-parecer-v01.json) e [freeze v92](orchestracao/.runtime/d30-farol-freeze-decisao-final-v92.json). O snapshot registra estes bytes e a atualização documental posterior ao freeze; o mapa final existente foi preservado, sem nova execução ou atualização nesta entrega.

FINAL14, somente sua execução: **708/0/0/0, exit0, BUILD SUCCESS, 1 JAR, 52 XML/classes locais**; fonte/snapshot/log/XML/capturas/JAR correspondentes. SQLIT: sete métodos somente compilados. Disposições v93e/v117, inventário javac v22, GET65/provider/A08/contextos e AST11/mapa final permanecem nas referências atuais; contagens não equivalem a cobertura ou aceite de pais.

Limites preservados por ID: incidente D29 sem resolução segura comprovada nem nova guarda WMS_DEV/WMSDEV/TLS; **317 nativas SQLServer, seis facetas de equipamento, 45 externas/47 históricas** e corte **08/10/2026 03:00Z ultrapassado**, sem retrodata ou soma de execuções. Frontend **não iniciado** (FE01–FE13); aguarda nova demanda expressa.

**Leitura no chat reiniciado:** ler AGENTS.md → este resumo de states.md → docs/09-continuidade.md → snapshot de retomada → recibo e matriz FINAL14, conforme pertinência. Reinício não autoriza retomar jobs ou tarefas. FINAL02–13, as 387 variáveis e os 23 grupos já reconciliados são históricos, não fila corrente. Nenhuma nova missão, teste/build/mapa/backend/FE/SQL/sonda/HTTPDEV/DDL/PROD/sa/servidor/kill/publicação/commit/push/ETL/rotina foi autorizada por este registro; nenhum terminal/chat/modelo/processo deve ser reiniciado pelo agente.

Posse: Farol mantém canônicos, decisões, continuidade, snapshots e recibos centrais; Cedro backend e artefatos próprios; Lume contratos para futuro FE e artefatos próprios, sem FE iniciado; Prumo database/infra em arquivos e artefatos próprios; Vigia revisão e pareceres próprios. graphify-out/ permanece sob coordenação de Farol, sem writer nesta entrega. Entrada exclusiva **Hermes WMS**, perfil **wms-rodogarcia**; somente os seis papéis WMS e a nota **WMS - Continuidade**, sem compartilhar contexto com ETL ou outros projetos. Este registro encerra o bloco documental autorizado; próximo passo: aguardar nova demanda expressa.

## Histórico preservado — registros anteriores ao snapshot de reinício

Os textos abaixo conservam integralmente IDs, detalhes, fontes e conclusões de suas datas. Pendências e comandos antigos não são instruções atuais de retomada; o resultado acima e o recibo FINAL14 prevalecem para o estado corrente.

**D30 — verificação local integral backend/documentação/contratos, demanda Lucas07/10/2026:** em andamento BE01–BE16. Frontend não iniciado; FE01–FE13 recebem somente ligações documentais. [Escopo e dez critérios](orchestracao/.runtime/d30-farol-escopo-e-distribuicao.json), [matriz atual](orchestracao/.runtime/d30-matriz-aceite-local.json) e [andamento](orchestracao/.runtime/d30-farol-andamento.json). Aceite integral somente com zero critério local pendente, sem percentual por número de testes.

**Produto integral de disposição recebido:** [Lumev10](orchestracao/.runtime/d30-lume-revisao-integral-v10.json) preserva1131IDs,727/706,1373/192,161rotas,48/39 e45/47. Cada linha possui definição/usos/oráculo/prova relacionada ou limite/faltante; não aceite de comportamento por estrutura, URI, chamada, annotation ou nome. [Fila de encaminhamentos](orchestracao/.runtime/d30-farol-fila-integral-lume-v09.json) conserva51qualificações individuais, sem limitar o conjunto. Cedro confronta equivalentes e completa faltantes locais; Vigia revisa assertivas/recibos. Reserva no cálculo, DUN contextual, marcos encerradaEm, hash decimal e compromissos/rollback do encerramento continuam com disposição específica.

**Provas revisadas e versões:** [Vigia01/02](orchestracao/.runtime/d30-vigia-parecer-focal01-02.json), [03–05](orchestracao/.runtime/d30-vigia-parecer-predicados-focais03-05.json), [Prumo/SQM/06–08/header](orchestracao/.runtime/d30-vigia-parecer-prumo-focais06-08-sqm-header.json) e [Long12/13](orchestracao/.runtime/d30-vigia-coercao-long-focais12-13-conferencias.json) favoráveis somente aos predicados delimitados, sem somar execuções ou fechar pais. [Integer14/15 autor](backend/evidencias/d30-cedro-coercao-integer-recibo.json) conserva red4/4/0/0 e green4/0/0/0 dos quatro negativos; [Parecer INTEGER independente](orchestracao/.runtime/d30-vigia-coercao-integer-focais14-15-conferencias.json) favorável somente aos quatro negativos; positivos/nulos/exatos/overflow/Long/Decimal e regressão completa pendentes na última fonte. Módulo correnteDF7D Long/Integer distingue versão histórica5C630 do green13;259main atuais,258anteriores preservadas nas capturas. Header64KB é correção local de transporte, domínio500/Long/sub200/900s intacto. Prumo317 estático e SQM7/34zeroJDBC revisados com limites; A11 valor lexical suficiente na captura5F230, textoUTF8autorD01D/F183 qualificado separadamente em [Vigia](orchestracao/.runtime/d30-vigia-a11-utf8-versao-parecer.json), sem retroagir hashes ou repetir confronto. Não materialização/ORDERBY/locks reais. A associação futura RN09.01/V40.02/AC09.06 recebeu [parecer Vigia17–20](orchestracao/.runtime/d30-vigia-associacao-focais17-20-parecer.json) favorável somente aos vetores do green20, com fixture/red/parcial separados; rollback tardio, audit/op por assertiva dedicada, SKU/dados/nulos e FINAL permanecem pendentes. FatoServico e seu service mudaram por caminho/SHA; preservação das258 fontes anteriores é histórica, sem certificar todasasatuais iguais.

**Ainda necessário para concluir LOCAL:** resolver todos faltantes pertinentes nas706/727/1373/161/classes/variáveis, incluindo fonte nova no inventário de símbolos após última mudança. A08 temporal lossless, A10 schemaH2 e três ramosv09 têm primeira execução somente na regressãoFINAL;65GETs precisam observação guardada por rota/cenário, reaproveitando jornadas, sem duplicar fixtures/focais. Depois das últimasfontes: finalCOMPLETO seguro com fonte/snapshot/classes/JAR/XML/log da mesma execução, mapa produtivo por caminho/SHA, freeze e revisão independente de TODO pacote. [Disposição documental Farol](orchestracao/.runtime/d30-farol-disposicoes-documentais-v09.json) confirma fontes do vínculo futuro adicional/peso e explicita lacunas locais; não cria conversão ou saída fictícia. [Contrato derivado atualizado](orchestracao/.runtime/d30-farol-adenda-contratos-associacao-inteiros-v01.json) separa associação/replays/consulta atual e negativos Long/Integer, preservando prefixo do documento35. [Adenda Lumev05](orchestracao/.runtime/d30-lume-adenda-v10-vigia-assertivas-v05.json) preserva as disposições anteriores e individualiza equivalentes/faltantes de recebimento, unitização e saída; [gate de cobertura](orchestracao/.runtime/d30-farol-gate-final-disposicoes-v10-v01.json) e [faltantes por atributo](orchestracao/.runtime/d30-farol-gate-final-atributos-faltantes-v10.json) mantêm IDs/pointers em batches sem aceite estrutural. limites de recebimento, unitização e saída continuam na mesma fila, com comandos válidos e conservação na recusa, sem repetir positivos já provados. Ausência de prova realSQL não impede predicados locais. Nenhum impedimento concreto de execuçãoLOCAL identificado; trabalho restante não é aceite nem bloqueio externo.

**Autoridade e preservação:** Cedro único escritor/executor backend; Farol docs/canônicos; Lume/Prumo/Vigia leitura e próprios artefatos. HTTPLOCAL+H2 efêmero fictício e mocks autorizados com configuração/URL/perfis e guardas fail-closed antes dos casos. SQLIT somente compilação. SQLServer/HTTPDEV/sondas/banco real,PROD,sa real,servidor/runtime/acessos,DDL/migrations reais,restart/kill/reset/limpeza,publicação/commit/push,ETL e rotinas continuam suspensos. D29 e históricos preservados; byte-identidade das caudas apenas na linhagem temporal conferida em [Vigia](orchestracao/.runtime/d30-vigia-canonicos-vinculos-temporais.json), sem afirmar todosbytes imutáveis desde baseline inicial. Dependências nativas/equipamento/externos conservam requisitos e encaminhamento próprio; nenhuma simulação os aceita. FE aguarda demanda posterior.

**Delimitação atual de defaults e provas:** a [adenda dos defaults de Avaria](orchestracao/.runtime/d30-farol-adenda-contratos-defaults-avaria-v01.json) qualifica somente os dois construtores compactos Java NULL→false; binding HTTP, permissões, replay e efeitos dos callers continuam com prova própria pendente. O [parecer de oráculos da primeira FINAL](orchestracao/.runtime/d30-vigia-oraculos-final-inteiros-get-v01.json) exige capturas H2 pertinentes por rota/caso/contexto, handler real e fisicoIgual=true, ligadas à fonte/XML da mesma execução; XML verde sozinho não atesta as fotografias. Captura ausente, false/null ou concorrente sem correspondente permanece pendente. Contextos sem banco conservam prova própria. A [revisão das assertivas de componentes](orchestracao/.runtime/d30-vigia-componentes-v13-v15-limites-assertivas-v01.json) mantém validação aninhada, causalidade do400 e transformações por caller nos mesmos IDs, sem aceite por roundtrip sintético.

**Consolidação de disposições,08/10/2026:** [Vigia:706locais/727IDs](orchestracao/.runtime/d30-vigia-cobertura-facetas727-local706-v01.json), [1373atributos](orchestracao/.runtime/d30-vigia-cobertura-atributosDTO1373-v01.json) e [161contratos](orchestracao/.runtime/d30-vigia-cobertura-contratosHTTP161-v01.json) têm disposições individuais conferidas; fontes/corpos relacionados e faltantes preservados, sem equivalência comportamental por contagem. [Lumev07](orchestracao/.runtime/d30-lume-adenda-v10-delta-produtivo-v07.json) e [Vigia32](orchestracao/.runtime/d30-vigia-delta-produtivo-v07-simbolos32-v01.json) delimitam32símbolos selecionados, não total corrente8716; onze do módulo faltavam no índice histórico. [Posição v08](orchestracao/.runtime/d30-lume-adenda-v10-posicao-por-id-v08.json) qualifica a restrição na mesma posição, sem exclusividade global deSKU/cliente; [sobrecargas v09](orchestracao/.runtime/d30-lume-adenda-v10-sobrecargas-v09.json) corrige cinco vínculos derivados: Indicador146 conserva fuso e fixa produtoId=null, controller chama157 com ambos. [Consolidação v32](orchestracao/.runtime/d30-farol-consolidacao-contratos-guardas-v32.json) importa planos Cedro34–37 e a leitura estática C07 Prumo: receita declarativa não é executor/FINAL. format31/compile31 exit0 informado pelo autor; nenhum JUnit novo após focal20. Ainda pendem disposições correntes completas de classes/variáveis, complementos finitos, último código/símbolos, freeze/executor seguro/primeiraFINALcompleta e revisão geral. Fragmentos graph v2 são históricos após esta atualização dos cabeçalhos; integração e fonte corrente mantêm gate próprio C09. C01–C10 abertos; SQLServer/FE suspensos.

**Regressão e partições,08/10/2026:** a [consolidação FINAL02](orchestracao/.runtime/d30-farol-consolidacao-final02-particoes-v34.json) confere os50XML/artefatos preservados e87reds porcaso:695/67/20/0,608green,exit1 e0JAR somente nessa execução. FINAL01 anterior falhou em formato semJUnit/JAR; não somar tentativas nem inferir87defeitos produtivos. Cedro trata causas compartilhadas, fixtures/oráculos/instrumentação e candidatos materiais antes da próxima regressão completa após últimas fontes. [Lume autoria exclusiva v02](orchestracao/.runtime/d30-lume-particao-semantica-v02.json) e [Prumo autoria exclusiva v1](orchestracao/.runtime/d30-prumo-particao-semantica-v1.json) foram recebidas comIDs/pointers/SHA preservados; suficiência semântica/provas e FINAL continuam em revisão independente. [Parecer de suficiência Cedro](orchestracao/.runtime/d30-vigia-cedro-indice-semantico-linhagens-v01.json) exige substituir disposições genéricas por transformação/invariante/caller/assertiva/oráculo/versão ou faltante concreto. [Ligações por dono/caller](orchestracao/.runtime/d30-farol-ligacoes-prumo-v36-distribuicao.json) preservam356grupos sem duplicação, sem testes novos automáticos. Histórico8716 permanece; inventárioV06 corrente8744 e delta28 são declarações, não requisitos/testes/aceite. O [mapa documental v34](orchestracao/.runtime/d30-farol-graph-documental-integrado-v34.json) foi integrado combackup/guardas e posse liberadaCedro; seusSHAs são temporais anteriores a este registro. C09 exige fonte/mapa final após últimas mudanças. Freeze/regressão/revisão geral não aguardam mera redação; achados reais permanecem na fila. C01–C10/D30 abertos; SQLServer/FE/históricos e limites mantidos.

**FINAL04 e faltantes locais comprovados,08/10/2026:** a [conferência FINAL04](orchestracao/.runtime/d30-farol-final04-pacote-conferencia.json) verifica696/0/0/0,50XML,1JAR e7095artefatosSHA da mesma execução,exit0; FINAL01/02/03 e seusreds continuam históricos separados, sem soma. Isso comprova regressão/proveniência deste snapshot, não aceite integral por número de testes. A [conferência GET65](orchestracao/.runtime/d30-farol-get-final04-conferencia-v42.json) confronta rota/handler/caso/XML/hash das fotos64 e conserva18faltantes positivos:47rotas têm captura2xx pertinente, sem aceitar403/handlerNULL/concorrentes sem correspondente. Cedro complementa somente faltantes na fila vigente; fonte nova exige nova correspondência e regressão final após últimas mudanças. As [ligações Lume/Prumo](orchestracao/.runtime/d30-farol-ligacoes-lume-prumo-v40.json) têm33grupos originais sem omissão/duplicação, com provas delimitadas ou falta concreta; instantes de cadastro, strings fiscais/cnull e limites individuais provider/declarações sem caller permanecem em confronto de equivalentes. [Plano de cadastros](backend/evidencias/d30-cedro-transformacoes-cadastros-confronto-plano-v52.json) identifica candidato fiscal positivo real, sem generalizarcnull/tempo. Os [exemplos semânticos Cedro](backend/evidencias/d30-cedro-semantica-autoral-v49.json) não concluem57classes/323grupos por contagem; autoria específica e parecer independente integral ainda necessários. O [mapa documental v41](orchestracao/.runtime/d30-farol-graph-documental-integrado-v41.json) preserva ASTv04/backup e oito fontes temporais anteriores a este registro; posse liberadaCedro para AST após últimas fontes. Não repetir verdes por mera redação. C01–C10/D30 continuam abertos; faltantes locais são trabalho autorizado, não impedimentoSQL. IncidenteD29/guardaSQLServer não resolvidos nem reexecutados; FE não iniciado.

**FINAL05 e deltas por item,08/10/2026:** a [conferencia FINAL05](orchestracao/.runtime/d30-farol-final05-pacote-conferencia.json) registra700/2/0/0,51XML,exit1 e0JAR na execucao encerrada naturalmente; FINAL04696/0/0/0/JAR permanece prova de seu snapshot, sem soma ou heranca para fontes posteriores. Os doisreds foram preservados por caso/XML e [importados com a prova fiscal Lume](orchestracao/.runtime/d30-farol-lume-final04-final05-deltas-v44.json): consulta da importacao retorna enderecos vazios versus expectativa da confirmacao; alteracao fisica no mesmo instante aplica ramo monotono de um microssegundo. Cedro confronta contrato/oraculo antes de corrigir, preservando main se a falha for expectativa; isso nao infere bug geral nem autoriza tolerancia temporal. A [consolidacao Prumo/Vigia](orchestracao/.runtime/d30-farol-prumo-vigia-final04-registros-v45.json) conserva29grupos e33pareceres porID; MapDecimalMAX/associacao/rollback simulado/captura temporal tipada sao suficientes somente nos vetores revisados FINAL04. NULL/default/omissao/limites nao herdam o positivo fiscal11; identidades, versoes otimistas e repositories conservam faltante concreto de ligacao/caller quando aplicavel. Autoria Cedro57classes/323grupos, provas GET65/complementos atuais, ultima regressao verde comJAR/freeze/mapa e revisao independente geral continuam pendentes. Nao repetir focais verdes por redacao ou criar teste por getter/alias/variavel. C01–C10/pais/D30 abertos; SQLServer/FE/incidenteD29/historicos e limites permanecem.

**Próximo passo D30:** [decisão local revisada e impedimentos](orchestracao/.runtime/d30-farol-consolidacao-decisao-local-v90.json): bloco LOCAL da fonte14 conferido e aprovado somente nos predicados/canais/oráculos/limites revisados. [Parecer geral Vigia](orchestracao/.runtime/d30-vigia-final-geral14-parecer-v01.json) conclui C01–C10 na parcela local, sem faltante local concreto conhecido ou novo teste/código necessário. FINAL14:708/0/0/0,exit0,1JAR,fonte/ZIP/classes/log/XML/capturas mesma execução; número de testes não é cobertura. Disposições93e/v117 e GET65/contextos/provider/A08 atuais revisados, AST11/mapa84 na fonte14 e preservação de relações confirmados. D30 geral/pais permanecem abertos:317facetas nativas,6equipamento,45externos/47históricos e corte03:00Z não cumprido; parcelas requeridas impedidas conservadas no denominadorC06/C07/C10, sem aceite porH2/static/mocks nem relabel. SQLD29 sem resolução segura/nova guardaWMS_DEV/WMSDEV/TLS. Próximos encaminhamentos são os donos/IDs da matriz e futura retomada nativa só no escopo seguro autorizado; nenhuma execução atual. Recibo parcial previsto em orchestracao/.runtime/d30-resultado-parcial-final14.json; frontend aguarda demanda posterior. D29/reds01–13/preexistentes preservados, sem soma/backdate/publicação/commit/push/ETL.

### D30 — inventário integral e aceite local

- [x] **D30-C01 Inventário completo de fontes/classes produtivas e atributos relevantes** — Cedro/Farol. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C02 Rastreabilidade normativa e decomposição das727lacunas** — Lume/Farol. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C03 Atributos, tipos e transformação entre camadas** — Cedro/Lume. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C04 JPA/migrations em arquivo e consultas estáticas** — Prumo/Vigia. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C05 Contratos destinados ao futuro frontend** — Lume/Cedro. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [ ] **D30-C06 Todos casos locais pertinentes e achados materiais** — Cedro. **LOCAL concluído; parcela requerida impedida permanece no denominador e critério integral aberto.**
- [ ] **D30-C07 Build final completo e proveniência da execução** — Cedro/Vigia. **LOCAL concluído; parcela requerida impedida permanece no denominador e critério integral aberto.**
- [x] **D30-C08 Revisão independente de todo pacote local** — Vigia. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [x] **D30-C09 Preservação, registros e mapa** — Farol/Cedro. **Conferido no bloco LOCAL delimitado da fonte14; sem fecho automático dos pais nativos/externos.**
- [ ] **D30-C10 Decisão de aceite local integral** — Farol/Vigia. **LOCAL concluído; parcela requerida impedida permanece no denominador e critério integral aberto.**

**Bloco local D29,07/10/2026, concluído e revisado:** [parecer final Vigia](orchestracao/.runtime/d29-fecho-local-vigia-final.json) favorável com limites; [recibo parcial](orchestracao/.runtime/d29-resultado-parcial-fecho-local.json). [Matriz](orchestracao/.runtime/d29-fecho-local-lume.json) preserva1.131IDs/48complementos/39financeiros, seis aceites anteriores delimitados,47externos históricos/45posteriores,727IDs sem equivalência integral selecionada e pais abertos. [Prumo](orchestracao/.runtime/d29-fecho-local-prumo.json) individualiza cinco impedimentos; parecer498 anterior preservado. Nove novos locais9/0/0/0 revisados: seis permutações de três notas somenteR$1,00 (0,33/0,33/0,34) e cortes1/0/32. R$0,01 de RN09.03/CT29-L043-01, ORDER BY nativo e demais facetas não recebem esse aceite. UTF8 Cedro corrigido pelo autor; referências históricas de hashes diferentes qualificadas sem reconstrução. Nenhuma nova sonda/SQL/HTTP/escrita nesta consolidação. D29 geral e backend integral permanecem abertos; nenhum frontend ou sistema completo declarado.



**Incidente SQL prioritário D29,07/10/2026:** Lucas relatou connection timeout. Novos ensaios SQL, chamadas HTTP e escritas de negócio estão suspensos até resolução segura e nova guarda real WMS_DEV/WMSDEV/TLS. A tentativa única de diagnóstico de Prumo já terminou; a retomada atual não autoriza nova sonda, SQL ou HTTP. Cedro completa somente lacunas locais comprovadas; Lume consolida a matriz existente, Prumo os impedimentos em arquivos e Vigia revisa o pacote local. Farol mantém os registros e o recibo delimitado. Sem restart, mudança de servidor/runtime/acessos, kill de transações ou limpeza. Timeout de abertura/prelogin antes de identidade já registrado; serviço/listener não comprovam login, saúde SQL ou autoria. [Controle de suspensão](orchestracao/.runtime/d29-incidente-suspensao-farol.md). D29 permanece aberta; nenhuma atribuição causal sem prova.



**Diagnóstico do incidente D29,07/10/2026:** sonda única Prumo20:39:17Z falhou em5,12s no prelogin (SQL_-2/native258),pooling=false,zeroSELECT/HTTP e identidade/TLSatuais não confirmados. MSSQLSERVERRunning/PID65088 desde05/10,mesmoPIDD24/listener1433; não prova saúde. Quatro eventos70117:02Z de memória no poolinternal precedem preflightválido19:14:15Z; causa atual/aplicação causadora inconclusivas. Histórico administrativoD24/D26/D27 separado deD29HTTPDEV; nenhum comandoD29 de alteração global encontrado nos registros revisados, sem afirmar inexistência universal. [Recibo do incidente](orchestracao/.runtime/d29-incidente-diagnostico-farol.md). Escritas/ensaios suspensos até resolução segura e nova guarda. Vigia1131/39 ePRIO06local14/0 revisados porcanal, semaceitepais/DEVporH2; preservação final1895+5tardios e demais limites individuais mantidos. D29 não encerrada.



**Correlação701 D29,07/10/2026:** nos21arquivos HTTP/1491respostas,19fotos/1274intervalos de queries de collectors e85logs WMS examinados, não há resposta, intervalo de consulta publicado ou linha JVM timestampada entre14:02:08 e14:02:20BRT. Isso não exclui atividade/API/SQL/JVM ou consumo de memória e não identifica aplicação causadora. Fonte/checkpoint documental17:02:35Z não prova execução SQL naquele segundo. O processo65088 sem reinício e a configuração atual são critérios distintos: configuração atual permanece não comprovada enquanto a guarda falha. Aplicativo/tela, horário e erro exato do timeout relatado porLucas foram solicitados neste canal e seguem pendentes; sondaPrumo e erroEnsaioD29 não os substituem. [Correlação offline](orchestracao/.runtime/d29-incidente-correlacao701-farol.json). AdendaVigia57/0 resolveu fonte04versus14/UTF16 sem mudar bytes; nove novosPRIO06 e consolidado14 já revisados. Escritas/ensaios continuam suspensos, sem reexecução ou atribuição causal.



**Aceite delimitado dos seis vínculos D29,07/10/2026:** RN24.02/MULTI01, V33.02/RET01, AC05.01/CAP01, Q-D10-L047-S02/RET02, Q-D10-L059-S02/RET05 e Q-D10-L059-S03/RET03 atendidos somente nos casos DEV enumerados e revisados. Base LumeFINAL19:49:39.577Z/DEFCDF57, parecerVigia23/0/9C42A9F2; nenhuma nova execução. Pais abertos. Saldo porSKU10=8reservado+2bloqueado+0disponível; texto disponível2 original preservado, sem usar como esperado. Revisão local14 posterior preservada. CheckpointCedro029 está na cópia histórica; corrente456 é atualização20:45, não prova de negócio ou posse. [Aceite e fontes](orchestracao/.runtime/d29-farol-aceite-seis-vinculos.json). Incidente e escritas suspensas permanecem; configuraçãoSQLatual não comprovada, causa/aplicação e relatoLucas pendentes. Sem fecho geral.



**D29 em andamento,07/10/2026 — BE03–BE15:** nova autorização expressa de Lucas para suprir lacunas documentais/técnicas do backend no DEV, com novas famílias fictícias por API e matriz completa RN/I/PR/V/AC/respostas/contratos/correções. D28 permanece encerrada, como inventário histórico; não substitui execução D29. Cedro jornadas/build/correções mínimas; Prumo preflight/SELECT/observabilidade permitida; Vigia revisão independente; Lume somente leitura; Farol registros/matriz/recibo. [Escopo e arquivos](orchestracao/.runtime/d29-autorizacao-e-distribuicao.md).



**Diagnostico de posse Cedro D29,07/10/2026 19:29:47 UTC:** prioridade atual de Lucas. `maestri check` mostra chat 01a117bb-d254-7c80-bd98-23f94a2e65df, diferente do original 01a11636-be42-77a3-9d1a-4cf73804deab. Leitura oficial de metadados do original confirma `active`/`inProgress`; duas entregas ao mesmo turno, separadas por reconexao oficial, recusadas com `Server is draining; retry after reconnecting`. Aplicativo/PID dono nao expostos: nao inferir VS Code pela origem nem declarar instancia residual. Sem encerramento/reset/lock apagado/servidor/config/seguranca raw ou negocio novo. Build/arquivos anteriores sao historicos, nao prova de terminal operacional atual. [Diagnostico sanitizado](orchestracao/.runtime/d29-cedro-sessao-diagnostico-farol.json). Retomada so aceita apos aviso ausente no mesmo chat original e resposta nova do modelo; Prumo/Lume seguem leituras independentes ja atribuidas.



### D29 — cobertura ampliada e critérios



- [ ] Inventariar fontes normativas e decompor requisito/regra/cenário/correção com origem, oráculo e status atendido/não atendido/depende de validação externa.

- [x] Novo preflight real WMS_DEV/WMSDEV restrita:36/0 em07/10/2026 16:09:36 UTC, TLS exato,64/687,V1–V10 e311 direitos preservados. [Conferência Farol](orchestracao/.runtime/d29-preflight-conferido-farol.json). Revalidação própria obrigatória antes de cada etapa; catálogo não é prova de negócio.

- [ ] Build/regressão atuais e jornadas ponta a ponta novas; AC04–08 exemplos positivos/zero/limites/recalculo com esperado independente e preços fictícios.

- [ ] SELECT novo de efeitos/replay/rollback/históricos; concorrência HTTP real repetida sem gate artificial como lock nativo, investigação de observabilidade sob direitos atuais.

- [ ] Corrigir defeitos concretos com red/green, build/reexecução e revisão; contratos/documentação honestos sobre dispositivos/fiscal/comercial/operação/recuperação externos.

- [ ] Revisão/cobertura/contagens, processos próprios encerrados, mapa AST após código e recibo final somente orchestracao/.runtime, sem callback Hermes/ETL.



**Próximo passo D29:** o bloco local e seu parecer estão concluídos; não repetir498/14/nove/seis ou jornadas encerradas. O responsável SQL deve comprovar resolução segura do incidente antes de qualquer nova guarda WMS_DEV/WMSDEV/TLS dentro da D29 autorizada. Preservação final1.895+cinco tardios/catálogo64/687/V1–V10/311direitos depende dessa guarda; corte nativo08/10/2026 03:00Z também exige origem admissível. Witness mantém limite de permissões. Confrontar prova específica existente paraR$0,01 entre três notas, sem execução adicional neste bloco; demais equivalências e45externos têm responsável/encaminhamento porID na matriz. Não atribuir todas as727lacunas ao SQL. Resultado somente parcial, sem callbackHermes/ETL.



**Parcial D29,17:00 UTC:** entrada, red de estoque e sua continuação verde, concorrência nova:462 chamadas HTTP/76 assertivas; duas recusas inesperadas do roteiro preservadas no red, sem defeito backend inferido. Quatro fotos SQL novas771 comparações/0 divergências,268 consultas; focais reutilizam fotografias. Concorrência:três pares200/409, inversão no segundo, três replays200/200 e controle sequencial em pedidos novos; intervalos HTTP e JVM próprios não são locks SQL nativos. [Ledger parcial](orchestracao/.runtime/d29-ledger-parcial-farol.json). A emissão manual herdada da primeira entrada desqualifica prova temporal; quantidades/origem/replay têm revisão própria. Guarda nova8/0 isolada, sem remutação. Baseline derivada corrige somente IDs vazios, conserva1.895 hashes e foto16:27; [conferência](orchestracao/.runtime/d29-preservacao-baseline-conferida-farol.json), confronto final ainda pendente. Financeiro local novo14/0 após corrigir fixture do teste, com repositórios simulados; não é jornada SQL/API. Financeiro real, demais jornadas/regras, revisão final, build final e encerramento geral continuam pendentes. [Matriz em andamento](orchestracao/.runtime/d29-matriz-requisitos-farol.json); partes atendidas limitadas às provas vinculadas.





**Parcial D29,17:51 UTC:**11 jornadas/complementos com980 chamadas HTTP/156 assertivas; sete divergências HTTP preservadas, incluindo o defeito concreto XInclude (contrato400, observado201 com pedido persistido), em correção red/green/build novo por Cedro e revisão Vigia. Demais reds identificados são de roteiro/fixture/oráculo. Onze fotos SQL ligadas a HTTP mais uma foto diagnóstica de auxiliar com zeroHTTP:1.699 comparações/0 divergências genéricas,738+67 consultas respectivamente; focal XInclude91/5 permanece vermelho e não é apagado pelo confronto genérico. Financeiro53/25 comprova previsões66/76 e snapshots preservados; complemento recusa aprovação antes do corte real08/10, não comprova aprovação/reabertura. Carga6 retomada sem recontagem; XML/extremos e Excel/contingência em confronto próprio. [Ledger atual](orchestracao/.runtime/d29-ledger-parcial-farol.json), [matriz](orchestracao/.runtime/d29-matriz-requisitos-farol.json):471 cláusulas,487 contratos,172 partes de respostas recebidas e avaliações independentes vinculadas, sem aceitar pais por referência geral. Cedro original operacional confirmado por checkpoints; entrada reaberta tem posse em outra app, sem intervenção/reset/hook/config alterados. [Recuperação observada](orchestracao/.runtime/d29-recuperacao-farol.json). Preservação17:17 dos1.895 IDs/hashes antigos é checkpoint, com uma adição de auditoria rastreada à API D29; confronto final/build final/cobertura completa/fecho geral ainda pendentes. Nenhum100% declarado.



**Parcial D29,18:39 UTC:**15 jornadas/complementos,1217 chamadas HTTP/194 assertivas; reds preservados. 14 fotos SQL ligadas a HTTP (939 consultas) mais uma foto diagnóstica de auxiliar com zeroHTTP (67 consultas), sem somar focais como novas consultas. Build448/0/0/0 novo e JAR7E22471E…939FD. Green XInclude400, SELECT134/0/focal101/0 sem novo pedido e red66 intacto; revisão independente em conclusão. Contingência L1/L2 e financeiroV1 rejeitada/V2 revisão106 em confronto, sem aprovação por corte08/10. FIFO novo D295BD16414 tem104 HTTP/22 assertivas e contrato para Prumo. Cedro confirmou leitura do [complemento financeiro](orchestracao/.runtime/d29-cedro-lacunas-financeiro-farol.md) e executa lacunas sem repetir fatos. Primeira tentativa local ampliada46 testes/1 falha/14 erros preservada; análise de fixtures/oráculo em curso, sem inferir defeito de negócio. [Índice de todos os critérios publicados](orchestracao/.runtime/d29-indice-auditoria-farol.md) e [JSON](orchestracao/.runtime/d29-indice-auditoria-farol.json) mantêm471 cláusulas/488 contratos/172 partes de respostas, candidatos Vigia/externos/39 financeiros separados. Somente aceites delimitados com revisão; nenhum percentual ou homologação integral. Preservação1.895 hashes ainda exige confronto final após últimas mutações; próximos testes/build final/processos/mapa/revisão geral continuam pendentes.



**Retomada D29 após queda,07/10/2026:** respostas novas de Cedro/Prumo/Vigia e confronto Lume nos chats recuperados confirmados. As sessões originais continuaram produzindo provas até19:42 e Cedro original concluiu build completo486/0/0/0 em26XML às20:06:35;307fontes/hash conferidos e JAR7E224…939FD preservado. [Conferência486](orchestracao/.runtime/d29-build-fronteiras-conferido-farol.json). Retomada Cedro executou5casos locais F33/F34/fronteiras XML64/200/1000000bytes UTF8, green5/0, com reds de fixture/oráculo preservados; não somar5+486 nem aceitar DEV por H2. Limites locais de configuração financeira1/366 e campos exclusivos em complemento. Vigia favoreceu17partes8B/13/964 com pais abertos; red1915/2 do leitor preservado e adenda4/0 fundamentada na reserva exclusiva do pallet: físico10=reservado8+bloqueado2+disponível0. [Parecer](orchestracao/.runtime/d29-vigia-retomada-20261007T195308Z-parecer.md). Lume confrontou1131IDs/48complementos sem renumeração; propostas DEV restantes aguardam revisão, não nova execução dos casos concluídos. [Confronto](orchestracao/.runtime/d29-lume-retomada-confronto-final.json).39PIDs históricos próprios e listeners estavam ausentes na conferência Farol posterior, sem encerramento por Farol. SQL_-2 antes de DB_NAME/identidade mantém retidos negócio DEV, preflight e preservação final; último confronto1895 comprovado17:17 está na cópia imutável, não no caminho corrente interrompido. Cinco separações tardias19:16:17 não são retroativas. Corte08/10/03Z, witnessSQL e externos permanecem individualizados. Revisão geral, complemento local e recibo ainda em andamento.



**D28 encerrada no escopo comprovado, 07/10/2026 — BE03–BE15:** regressão nova das correções D26/D27 nos mesmos chats WMS, com revisão independente favorável às 29 linhas e limites explícitos. Build420/0/0/0 em20 XMLs;17 rodadas reais,761 chamadas HTTP (756 individuais aprovadas+2 do par200/409+3 divergências de roteiro preservadas),211 assertivas (210 aprovadas+1 precondição inválida preservada). Fotografias SQL atuais1315/0,1141 consultas nelas; focais211/0 reutilizam as fotos, sem novas consultas. Ajustes/rollback/replay, temporal, financeiro50 aprovado/v2, XML/RN22/extremos, perfis/filtros/FIFO/avaria e concorrência HTTP/JVM+SELECT posterior reexecutados. Witness SQL nativo impedido por SQL300 sob WMSDEV; nenhuma ampliação de privilégio. Preflight final WMS_DEV/WMSDEV,64/687,V1–V10 e311 direitos sem alteração;300 fontes de backend/migrations intactas. Zero APIs/helpers/listeners D28 na conferência OS; fixtures/históricos preservados. [Recibo final](orchestracao/.runtime/d28-resultado-final.json), [matriz29](orchestracao/.runtime/d28-matriz-correcoes-farol.json), [parecer Vigia](orchestracao/.runtime/d28-vigia-final.md). Sem frontend/PROD/reset/DELETE/DDL/grants/migration/publicação/commit/push/callback Hermes/ETL. Não homologa o backend inteiro.



### Regressão D28 — critérios e evidências atuais



- [x] Inventário29 correções/casos/evidências atuais; auxiliares offline e negócio real separados, revisão Vigia e referências/hashes conferidos por Farol.

- [x] Comprovar DB_NAME=WMS_DEV/WMSDEV/TLS no preflight novo de 07/10/2026 12:14:59 UTC, catálogo64/687, V1–V10 e311 direitos preservados; runner revalida antes das etapas. [Preflight](orchestracao/.runtime/d28-prumo-preflight.json).

- [x] Build novo em target-d28-atual:420 testes,0 falhas/erros/ignorados,20 XMLs, sem IT SQL que exige banco vazio. SHA4304D428…E8900CE1 reproduz o conteúdo preservado. [Conferência](orchestracao/.runtime/d28-build-conferido-farol.json).

- [x] Reexecutar ajuste zero/parcial/rollback/replay, contagem/carga e temporal; financeiro50/aprovação/reabertura/v2; XML/RN22/extremos; permissões/filtros/FIFO/avaria e concorrência HTTP/JVM. Limite nativo abaixo.

- [x] Confrontar HTTP/GET/snapshots/auditoria/saldos por SELECT novo:17 fotos atuais1315/0 e focais211/0; IDs/perfis/UUIDs/hash do corte preservados, sem contar focal como nova consulta.

- [x] Revisão independente favorável com limites,29 vínculos/2574 checks de referências sem diferenças; processos próprios encerrados, Graphify AST atualizado e recibo final somente em orchestracao/.runtime/.

- [ ] Witness SQL nativo simultâneo: impedimento concreto SQL300 sob WMSDEV; somente CONNECT SQL/VIEW ANY DATABASE observados, sem autorização de sa/grants. Alternativa HTTP/JVM+gate SELECT/rollback e SELECT posterior comprovada separadamente.



**Próxima ação D28:** aguardar nova demanda. Não iniciar nova jornada, fixture, IT vazio, migration ou reset por checkpoint/prompt antigo. Reds de roteiro/coletor/precondição permanecem classificados, com green/complementos novos; o modo fresco do auxiliar saldo XML foi removido/bloqueado antes de credenciais/JVM/SQL/HTTP após a continuação green; guarda isolada3/0, sem nova jornada. Persistência das permissões em novo chat e flags reais de início não confirmadas. [Limites e recibo](orchestracao/.runtime/d28-resultado-final.json). D27 abaixo permanece histórica e concluída.



**D27 concluída no escopo técnico comprovado, 06/10/2026:** retomada autorizada às21:33 BRT nos mesmos chats WMS. V10 DEV, ajustes/rollback, financeiro50 aprovado, RN22/XML, filtros/perfis/FIFO/avaria e concorrência com witness SQL online executados. Build atual420/0/0/0;726 HTTP (715 individuais aprovados,4 dos dois pares e7 divergências históricas corrigidas/classificadas),248 assertivas (247 aprovadas e1 guarda histórica que recusou retomada já reservada antes de gate/POST). [Resultado e limites](docs/42-validacao-tecnica-api-sql-dev.md), [parecer favorável Vigia](orchestracao/.runtime/d27-vigia-final.md), [recibo final WMS](orchestracao/.runtime/d27-resultado-final.md). API WMS_DEV/WMSDEV, escritas fictícias por HTTP e históricos preservados; nenhum achado material atual aberto no incremento revisto. D26 abaixo permanece histórico. Aguardar nova demanda, sem iniciar outro bloco.



### Retomada D27 — BE03–BE15



- [x] Inspecionar alvo administrativo/TLS/CHECK/histórico e revisar V10; aplicar Flyway exclusivamente WMS_DEV por executor DEV protegido. [Conferência real](orchestracao/.runtime/d27-migration-conferida-farol.json): PreValidate/Migrate/PostValidate/Info exit0, V10 success/checksum−562012523, seis tipos/habilitada/confiável; V1–V9/histórico/permissões/fixtures preservados, nenhum PROD.

- [x] Revalidar E052/AC13 por HTTP próprio: ajustes50→45,50→0 e45→35, replay/recusas/reserva ativa/reversão/revisão superada e GET. [SELECT próprio28/0](orchestracao/.runtime/d27-prumo-ajustes-final-20261007T014149078.json): físico/disponível35, reservado0, ocupação/ID e marcos preservados, fatos/movimentos/auditoria/snapshots únicos, recusas sem efeitos e cálculo25 legitimamente PENDENTE/totalNULL sem contrato.409 anterior/localização e rollback preservados; sem DELETE/grants/DDL adicional.

- [x] Executar jornada financeira completa não zero em fixture nova válida no corte real, sem alterar relógio/fatos históricos: mínimo fictício50, aprovação/replay/recusas/reabertura/versão2 preservando versão1. HTTP inicial51/13 e JAR final19/6; [SELECT WMSDEV/DEV65/0](orchestracao/.runtime/d27-prumo-casos-20261007T012544553.json) inclui memória/versões/auditoria/snapshots e ausência de emissão. Validação delimitada desse contrato, não de todas fórmulas.

- [x] Confrontar lacunas técnicas selecionadas da matriz D26: RN22/XML com cinco limites400 e positivo/replay/SELECT20/0, filtros/paginação/perfis/alcance; variantes FIFO/tarifas/GRIS/avaria com fotografia106/0. [Matriz](backend/evidencias/d27-matriz.md)161 métodos positivos combinados D26/D27,61 D27 em diferentes JARs; não161 no último artefato. [Green concorrente](backend/evidencias/d27-D2748A39681-http.json)16HTTP/2assertivas, witness SQL82→64→88 verdadeiro e [SELECT final24/0](orchestracao/.runtime/d27-prumo-concorrencia-final-select-20261007T024151964.json):100 físico/80 reservado/20 bloqueado/0 disponível, replay e perdedor sem efeitos. Propostas AC e combinações não executadas delimitadas.

- [x] Build/regressão proporcional/JAR final: [420/0/0/0,20 XMLs e SHA4304](orchestracao/.runtime/d27-build-numerico-conferido-farol.json). HTTP real no artefato final, leituras SQL compatíveis com banco populado; nenhum novo IT vazio. [Vigia final favorável](orchestracao/.runtime/d27-vigia-final.md) aos casos/limites, sem backend total ou AC homologada.

- [x] [21 JARs encerrados/42 portas sem listener](orchestracao/.runtime/d27-processos-conferidos-farol.json), helpers/readers próprios encerrados, dados/fixtures/históricos preservados. [Graphify AST WMS](orchestracao/.runtime/d27-graphify-ast-farol.json)5700 nós/21421 arestas, sem LLM/grafos alheios. Registros e [recibo final somente WMS](orchestracao/.runtime/d27-resultado-final.md).



**D26 — ensaios encerrados com falha aberta, 06/10/2026:** API real com Gestor/Supervisor/Operação e persistência exclusivamente WMS_DEV/WMSDEV, JWT HTTPS/JWKS/RSA efêmero e Hibernate validate. Toda escrita de negócio por HTTP fictício; identidade/DPAPI/direitos restritos atestados. Regra permanente Hermes AGENTS.md:55 preservada. Cedro API, Prumo acesso/SELECT, Vigia revisão, Farol registros; nenhum frontend/PROD/reset/DELETE/DDL de schema/sa na aplicação.



**Resultado D26:** build novo atual412/0/0/0 em20 XMLs e SQL IT7/0/0/0 antes da população.1.404 requisições HTTP:1.375 aprovadas individualmente,8 dos quatro pares concorrentes e21 tentativas históricas divergentes;159/160 métodos positivos, não aprovação de todas regras. Correções de revisão inicial e rollback-only financeiro reexecutadas em JAR412. SELECT próprios236/0, GET/cálculo4 17/0 e adicional financeiro76/0; históricos/memórias/fixtures preservados. E052 falhou409 por CHECK sem AJUSTE_ESTOQUE, rollback42/0 e V10 proposta inativa. Snapshot financeiro20 não aprovado: fato anulado, recálculo0 e409 CALCULO_DESATUALIZADO correto. [Resultado41](docs/41-ensaio-http-e-persistencia-dev.md), [build](orchestracao/.runtime/d26-build-financeiro-conferido-farol.json), [recibo WMS](orchestracao/.runtime/d26-resultado-final.md). Backend total não aprovado.



**D25 — entrega local e leituras DEV concluídas, 06/10/2026:** clean verify atual com **408 testes/0 falhas/0 erros/0 skips em 20 XMLs**, JAR atual com **6/6 verificações HTTP** em loopback, processo encerrado e porta liberada. Correções de TLS cliente, critério de lock, perfil do IT e cookie de sessão local revisadas por Vigia. SQL Server real WMS_DEV por SqlClient/JDBC administrativo somente SELECT: 64 tabelas vazias/687 colunas, 1.295 itens de catálogo, nove migrations/histórico iguais à D24. **API/JPA validate e sete ITs SQL não executados: identidade própria restrita ausente.** Nenhum DDL/DML/grant/SQL PROD. Regra AGENTS.md:55 de Hermes preservada. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md) e [recibo WMS](orchestracao/.runtime/d25-resultado-final.md). BE03/BE15 continuam em validação externa; D22/D23 são históricos.



**D24 concluída no escopo SQL administrativo, 06/10/2026:** conexão compartilhada em `../.runtime/sql-server` provisionada e BAT real concluído DEV → PROD. Ambos ONLINE, 64 tabelas, 687 colunas, nove migrations e 1.295 itens de catálogo conferidos por banco. SQL Server 2022 Standard 16.0.1000.6 preservado, mesmo processo, sem reinício. [Resultado e limites](docs/39-conexao-compartilhada-e-sql-real.md); [leitura autenticada](database/evidencias/d24-resultado-real.json). Backend/piloto continuam separados. D20–D23 abaixo são históricos, superados quanto ao bloqueio de autenticação e à exigência de fonte exclusiva WMS.





**Continuação focal D23, 06/10/2026:** padrão de autenticação dos runners públicos comparado; fonte administrativa SQL compartilhada não localizada/documentada nos guias/metadados autorizados de ../.runtime. 33/33 booleanos de presença dos canais consultados ausentes; configs de referência existem sem leitura do conteúdo, DPAPI WMS ausente. O caminho fixo D22 não recebe a configuração como os demais. Menor integração: somente aquisição de credencial no launcher existente, após fonte administrativa protegida explicitamente disponibilizada ao WMS; sem novo configurador ou credencial alheia. Nenhuma conexão nova/DDL; servidor indisponível não comprovado. [Comparação e resultado](orchestracao/.runtime/d23-comparacao-fontes-autenticacao.md).



**Histórico D23 — acesso então bloqueado em 06/10/2026:** Lucas autorizou bootstrap/upgrade real DEV → PROD pelo agente, sem configurador/prompts, com identidade Windows corrente ou fonte protegida WMS existente. Probe SqlClient confirmou TLS -2146893019; pin público privado WMS verificado resolveu esse bloqueio no sqlcmd Go. SQL Server então recusou logon de ROD-SRVW-001\suporte. Credencial protegida WMS AUSENTE. Nenhuma leitura sys.databases/histórico/schema ou DDL concluída; permissões não determinadas. Única ação indispensável: disponibilizar autenticação SQL válida já autorizada em canal protegido exclusivo WMS, fora do chat, sem grants ou alterações globais. D22 abaixo é histórico local, não conclusão da execução real D23.



**Histórico D22 em 06/10/2026:** D22 BE03/BE15 concluído localmente: BAT normal automático sem senha/confirmação, DPAPI WMS exclusiva e configurador UMA VEZ. 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão Vigia favorável. Credencial real AUSENTE por metadados, provisionamento pelo operador pendente; nenhum segredo real/SQL pelo agente. [Resultado38](docs/38-launcher-automatico-e-credencial-protegida.md) e [recibo WMS](database/evidencias/d22-relatorio-final.md). D20/D21 abaixo são históricos preservados.



Atualizado em06/10/2026. **D21 BE03/BE15 concluída localmente:** BAT de bootstrap/upgrade completo WMS_DEV e WMS_PROD, DEV primeiro, pela sequência vigente do Flyway. 41/41 fixtures,672/672 checks de pacote,25/25 fixtures Maven,cmd.exe real offline0/0 e inválidos2/2;675 hashes database e84 backend conferidos, revisão Vigia favorável. V1–V9 e históricos D20 preservados. Nenhum SQL pelo agente; ensaio real será manual pelo operador. [Resultado37](docs/37-bootstrap-e-upgrade-manual-database.md) e [recibo WMS](orchestracao/.runtime/d21-resultado-final.md).



## Estado atual



| Frente | Situação comprovada |

| --- | --- |

| Documentação | PDF e Word preservados; respostas e regras consolidadas; arquitetura e cenários documentados |

| Backend | D27 concluída tecnicamente nas jornadas da matriz: build420/0/0/0,726 HTTP novos e persistência DEV/concorrência SQL real.161 métodos positivos combinados D26/D27,61 na D27 em artefatos distintos; parecer Vigia favorável com limites por caso. Homologação operacional e frontend separados. |

| Frontend | Somente orientação de estrutura; sem aplicação React/TypeScript, dependências, build ou testes executados |

| Banco | D27: V10 aplicada/conferida somente WMS_DEV, dez migrations SQL e CHECK com AJUSTE_ESTOQUE confiável/habilitado;64/687 preservados. PROD não acessado na D27, última fotografia D24 com V1–V9. SQL Server 2022 Standard preservado. [DEV real](orchestracao/.runtime/d27-migration-conferida-farol.json), [histórico D24](docs/39-conexao-compartilhada-e-sql-real.md) |

| Ambiente e piloto | Sem aplicação publicada; equipamentos, infraestrutura e recuperação ainda sem validação no WMS |

| Escopo autorizado agora | D29 em andamento por nova autorização expressa: cobertura documental/técnica ampla, famílias fictícias API somente WMS_DEV/WMSDEV. Limites em d29-autorizacao-e-distribuicao; D28 histórica encerrada. |



Base definida: React/TypeScript, Java/Spring com MVC convencional e SQL Server existente. Primeiro piloto em Osasco, depois Castro/Tigre; Caio valida a operação. A necessidade informada de iniciar em 13/10/2026 não representa estimativa técnica nem promessa de entrega.



## Onde consultar e como retomar



- [AGENTS.md](AGENTS.md): orientações de trabalho e de atualização deste arquivo.

- [Decisões](docs/06-decisoes-e-pendencias.md): escolhas do responsável e estado dos temas Q01 a Q27.

- [Respostas](docs/10-respostas-recebidas-2026-10-05.md) e [regras consolidadas](docs/11-alinhamentos-apos-respostas.md): comportamento recebido e interpretações/propostas AC01 a AC16.

- [Arquitetura](docs/02-arquitetura.md), [fluxos](docs/04-fluxos-operacionais.md) e [cenários](docs/08-cenarios-de-validacao.md): responsabilidades, sequência operacional e verificações V01 a V48.

- [Plano geral](docs/07-plano-de-entregas.md) e [continuidade](docs/09-continuidade.md): visão da entrega e contexto resumido.



Escopo local D19 preservado nos [28](docs/28-validacao-separacao-retirada-retornos.md), [30](docs/30-validacao-servicos-e-calculo.md), [32](docs/32-validacao-fechamento-e-contingencia.md), [34](docs/34-matriz-e-validacao-final-backend.md) e [35](docs/35-modelo-integrado-e-jornadas-backend.md). D24 comprovou migrations; D25 é histórico de build/leituras; D26 executou API/JPA/SQL com identidade própria, conforme [41](docs/41-ensaio-http-e-persistencia-dev.md). [D27](docs/42-validacao-tecnica-api-sql-dev.md) resolveu ajuste/RN22/financeiro e variantes técnicas selecionadas; homologação/publicação/frontend permanecem fora do escopo. Pendências SQL citadas nas evidências antigas descrevem a data original e não substituem esse resultado atual.



**Próximo passo vigente:** executar D29 no DEV com novas provas e revisão independente, sem pedir autorização por etapas internas; fechar comprovável ou apontar bloqueio concreto e dependência externa exata.



**Histórico D20, substituído por D21 no launcher:** alvo confirmado TCP `127.0.0.1:1433`, SQL auth `sa`, conexão inicial `master`. Lucas passou à execução **manual**: `iniciar-bancos.bat` deve criar ambos os nomes ausentes, DEV primeiro, preservar/conferir existentes e permitir retomada; PROD sem migrations/cargas. Senha oculta local em memória e confirmação de identidade/alvo na interação. Nenhum SQL pelo agente; TCP alcançável/MSSQLSERVER Running não comprovam identidade SQL. Regra imutável do servidor em AGENTS/D20; guia compartilhado sem senha autorizado, sem ler `.env`/credenciais/dados/ETL nem alterar terceiros. Falta de credencial no canal do agente é observação histórica, sem impedir o launcher.



**Organização da mesma D20 entregue:**34 movimentos byte a byte,489 originais preservados, raiz database somente README curto e BAT. Scripts/docs/config nas pastas próprias; históricos, fontes e manifestos anteriores intactos; V1–V9 sem renomeação/alteração. Caminhos/BATs/links e testes conferidos após os movimentos, manifesto corrente722 hashes e revisão Vigia favorável ao layout/launcher. Sem SQL pelo agente ou novo build backend integral.



**Ressalva VIG07 do runtime:** guarda enumera recusas, não certifica mínimo privilégio geral. Antes do uso/ensaio do backend, exigir atestação completa das identidades/permissões próprias. Java/JAR396/404 são histórico D20; nenhuma atestação SQL executada. Não condiciona o bootstrap/migrations administrativo manual D21 com sa a novos grants/conta de aplicação.



### Conferência D26 — BE03/BE04–BE15



- [x] Preservar a regra Hermes AGENTS.md:55; SQL de testes somente WMS_DEV, sem PROD/fallback.

- [x] Provisionar identidade própria WMSDEV/DPAPI exclusiva, atestar direitos efetivos e recusas DDL/histórico imutável/PROD por metadados.

- [x] Executar clean verify novo412/0/0/0 e sete ITs nativos SQL7/0/0/0 antes da população.

- [x] Executar jornadas HTTP reais dos três perfis, escrita fictícia pela API, Hibernate validate e JWT efêmero HTTPS/JWKS sem desabilitar segurança.

- [x] Inventariar160 métodos;159 positivos, E052 falhou/bloqueado, matrizes RN/AC com limites explícitos.

- [x] Confrontar persistência/estoque/auditoria/memórias: fotografia236/0 e cálculo corrigido/GET17/0, além dos recortes separados.

- [x] Corrigir primeira revisão/INSERT e rollback-only financeiro, com red/green e repetição HTTP real no JAR412.

- [x] Executar último complemento financeiro após corte UTC real e SELECT76/0: snapshot20 preservado, fato anulado/recálculo0 e aprovação409 corretamente recusada.32 JARs encerrados/64 portas livres; nenhuma aprovação20 alegada.

- [x] Encerrar a [revisão independente](orchestracao/.runtime/d26-vigia-final.md) e consolidar o recibo WMS: VIG14 corrigido, VIG15 delimitado, E052 aberto; Cedro/Prumo/Vigia finalizados, Farol encerra este ask.

- [ ] Aprovar ajuste físico E052: depende de migration nova em escopo separado DEV; V1–V9 imutáveis, rollback42/0 comprovado.

- [ ] Aprovar backend total: variantes/lacunas da matriz, witness SQL simultâneo, homologação operacional/provedor/equipamentos/recuperação permanecem separados.



### Conferência histórica D25 — BE03/BE15/BE16



- [x] Preservar a regra permanente de Hermes em AGENTS.md:55: testes SQL apenas WMS_DEV, recusar PROD/outros/fallback.

- [x] Inspecionar alvo real, integridade, catálogo, vazio, histórico e existência de identidade da aplicação por leitura administrativa DEV.

- [x] Executar clean verify novo e corrigir achados locais; 408/0/0/0, 20 XMLs finais conferidos por Cedro, Vigia e Farol.

- [x] Executar JAR atual com seis checks HTTP locais, encerrar a própria JVM e conferir porta liberada.

- [x] Comprovar TLS/JDBC standalone com dois SELECTs administrativos DEV; não equivale a pool/JPA da aplicação.

- [x] Obter revisão independente favorável, registrar evidências e fechar o pacote sem callback Hermes/ETL.

- [ ] Disponibilizar e atestar identidade própria/credencial protegida restrita exclusivamente WMS_DEV — operação administrativa ainda não autorizada/executada.

- [ ] Executar API/JPA validate e sete sqlserver-it no DEV com essa identidade; nenhum fixture SQL criado na D25.



Escopo local concluído; os dois últimos itens impedem o ensaio SQL da aplicação e não anulam os testes independentes. [Resultado e proposta40](docs/40-testes-backend-e-leitura-sql-dev.md).



## Convenções do acompanhamento



| Status | Significado |

| --- | --- |

| Planejado | Escopo descrito, trabalho ainda não iniciado |

| Em andamento | Há trabalho efetivo, mas faltam partes ou verificações |

| Em validação | Entrega preparada e aguardando verificação identificada |

| Bloqueado | Impedimento concreto registrado, com responsável ou próximo encaminhamento |

| Concluído | Critérios atendidos, evidência e limites registrados |

| Adiado | Fora da entrega atual, com motivo e condição de retomada |



Uma caixa marcada significa que aquela ação foi concluída; não conclui automaticamente a etapa. Ao mudar um status, atualizar também a evidência. **BE01/BE02/BE16 concluídas localmente; BE03–BE15 em validação externa, com escopo local D19 aceito**. Nenhuma caixa externa foi marcada sem evidência. Etapas FE permanecem planejadas. A preparação documental abaixo também está concluída.



As dependências indicadas precisam estar disponíveis para concluir a etapa. Estudo de contratos e protótipos pode avançar antes, com dados fictícios e indicação de simulação. Protótipo não comprova integração. Não usar percentual de avanço calculado apenas pela quantidade de caixas.



## Preparação documental concluída



- [x] **DOC01** Estrutura `backend`, `frontend`, `database`, `infra` e `docs`, com finalidade registrada nos respectivos arquivos de orientação.

- [x] **DOC02** Fontes originais preservadas e 110 respostas analisadas, com regras e propostas distinguíveis nos documentos 10 e 11.

- [x] **DOC03** Arquitetura, fluxo geral e 48 cenários futuros de validação documentados, sem alegar testes executados.

- [x] **DOC04** `AGENTS.md` adaptado e este `states.md` criado com trilhas separadas, dependências e critérios de conclusão.



Evidência: arquivos presentes na raiz e em `docs`, referências verificadas e histórico ao final. Isso comprova documentação, não funcionalidades executáveis.



## Preparação da equipe no Maestri



### OR01 Equipe WMS com Hermes próprio



**Status:** Em validação. **Base:** D14. A preparação não altera os status das etapas BE/FE nem inicia outro bloco de implementação.



- [x] Criar Farol, Cedro, Lume, Prumo e Vigia com papéis WMS exclusivos e diretório do WMS; Farol com Maestro próprio.

- [x] Criar Hermes WMS real com perfil `wms-rodogarcia`, memória e sessões próprias, sem clonar o perfil anterior ou trocar o perfil padrão.

- [x] Ligar os quatro especialistas ao Farol e Hermes WMS ao Farol; compartilhar a nota própria somente entre os seis terminais WMS, sem ligação ao ETL.

- [x] Preservar nomes, pastas, papéis e posições dos seis terminais antigos, as oito conexões de agentes e as duas da nota antiga.

- [x] Registrar papéis, IDs, limites e responsabilidades em `orchestracao/` e no [documento 17](docs/17-orquestracao-maestri-hermes.md).

- [ ] Após escolha de privacidade no Hermes WMS e liberação de confiança da pasta nos agentes Codex pelo usuário, conferir respostas breves e o caminho Hermes WMS → Farol, sem iniciar implementação.



**Evidência da preparação D14 em 05/10/2026:** seis terminais WMS no mesmo canvas Projetos, por orientação do usuário. Listagem do Farol confirmou quatro especialistas, Hermes WMS e nota própria. Auditoria das conexões confirmou cinco ligações internas de agentes, seis da nota WMS e nenhuma entre WMS e ETL; o grupo antigo permaneceu preservado nos campos conferidos. Cadastro em [equipe.json](orchestracao/equipe.json) e [perfil Hermes](orchestracao/hermes/README.md). Naquela conferência, Hermes WMS abriu `Help improve Hermes?` e os agentes Codex aguardavam `Trust and continue`; esse é um registro histórico, não o estado de todos os terminais após D18. Telegram WMS não configurado; gateway, ponte e perfil padrão existentes não foram reconfigurados.



**Atualização D18/D19:** Farol recebeu as tarefas por este canal; `maestri list` confirmou os nomes/papéis e a nota própria. `ask`/`check` comprovaram leitura/entrega de Cedro/Prumo, parecer de Vigia e, em D19, parecer de contratos de Lume somente em leitura. Resultados D18 no [documento 25](docs/25-validacao-pedido-saida-e-reserva.md) e andamento D19 no [26](docs/26-execucao-continua-backend.md). Não houve resposta automática a confiança/privacidade. Prumo/Vigia relataram ausência do CLI/ambiente Maestri próprio; Farol recebeu e encaminhou pelos terminais conectados. **Pendente OR01:** retorno autônomo de todos os workers e conferência completa do caminho Hermes WMS, sem iniciar frontend nem alterar Hermes. Essa conferência permanece separada do backend autorizado.



## Trilha backend



As entregas desta trilha ficam em `backend` e, quando necessário, `database` e `infra`. A organização permanece por camadas convencionais. Nomes de classes, tabelas e rotas serão definidos durante o detalhamento; os IDs abaixo identificam entregas de negócio.



### BE01 Modelo conceitual e contratos



**Verificação ampliada D30 em andamento:** os aceites locais históricos abaixo conservam o recorte original; nova auditoria integral de atributos/usos/contratos/casos locais nos dez critériosD30, sem aceite antecipado ou frontend. [MatrizD30](orchestracao/.runtime/d30-matriz-aceite-local.json).



**Status:** Concluído localmente. **Dependências:** DOC01 a DOC04.



- [x] Relacionar cliente, armazém, produto, embalagem/DUN, nota, pedido, recebimento, unidade, posição, reserva, movimento, serviço, tabela e fechamento.

- [x] Descrever transições, ações permitidas, quantidades, datas e precisão; separar estoque físico, disponível e cobrável.

- [x] Definir contratos de entrada/saída, erros, filtros e paginação para as jornadas FE01, preservando propostas identificadas nos documentos.



**Conclusão:** modelo e contratos explicam uma entrada, uma reserva, uma saída parcial de pallet e um fechamento sem contradição entre quantidades, identidade e datas. **Base:** RN02, RN10, RN16, PR05 e AC01 a AC13.



**Evidência histórica em 05/10/2026:** [contrato técnico inicial](docs/12-base-e-contratos-backend.md) de status, erros e identificação da solicitação, disponível para FE02. Naquela etapa faltavam modelo completo, transições, precisão, contratos operacionais, filtros/paginação e ligação com jornadas FE01. Esses itens foram complementados no fecho local D19 abaixo.



**Ampliação D12:** [modelo e contratos cadastrais](docs/14-cadastros-acesso-e-persistencia.md), incluindo quantidade/embalagem, paginação, revisão e encerramento pendente. O recorte atende aos cadastros de FE04; os contratos completos de entrada/reserva/saída/fechamento e jornadas FE01 ainda faltam. Não confundir precisão de embalagem entregue com cálculo de estoque/cobrança.



**Ampliação D15:** [contratos de recebimento](docs/18-recebimento-e-conferencia.md) para FE05, com notas, itens, fatos físicos, estornos, divergências, FIFO e entradas conferidas. BE01 continua parcial: unidade logística, posições, reserva, saída, serviços e fechamento ainda precisam do modelo integrado.



**Ampliação D16:** [contratos de unidades](docs/20-unidades-logisticas-e-etiquetas.md) para FE06, com identidade permanente, composição por entrada, conservação de quantidades, transformação e etiqueta. Permanecem posições, reserva, saída, serviços e fechamento em BE01.



**Ampliação D17:** [capacidade, posições, movimentos e saldo](docs/22-enderecamento-movimentacao-e-estoque.md) para FE07/FE08. Reserva, saída, serviços e fechamento continuam pendentes em BE01; regras AC04/AC05 não se tornam aprovação comercial.



**Ampliação D18:** [pedido de saída, FIFO e reserva](docs/24-pedido-saida-fifo-e-reserva.md) para FE09 e saldo FE08; [197 testes](docs/25-validacao-pedido-saida-e-reserva.md) entregues. **D19:** BE10/BE11 aceitos localmente no [contrato27](docs/27-separacao-retirada-retornos-e-avaria.md)/[evidência28](docs/28-validacao-separacao-retirada-retornos.md), build final de240 testes. Recorte BE05/BE12 aceito localmente pelo [contrato29](docs/29-cadastros-servicos-e-calculo.md)/[evidência30](docs/30-validacao-servicos-e-calculo.md), build292, revisão favorável e V7/JPA compatíveis em arquivos. Naquela entrega, faltava integrar fechamento/contagem do31 e completar o [modelo35](docs/35-modelo-integrado-e-jornadas-backend.md). AC10/AC11 permanecem propostas identificadas.



**Ampliação D19 após aceite BE13:** fechamento/ciclo/ajustes do31 aceitos localmente com333 testes, Vigia favorável e V8/JPA compatíveis, conforme32. Naquela fase, contagem/carga/contingência e encerramento BE14/BE05 ainda estavam em construção. Contratos/transições e percurso HTTP integrado foram complementados no fecho D19 abaixo; o diagrama sozinho não comprova execução nem frontend.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Modelo35/contratos14–31 e jornada HTTP própria ligam entrada em partes, etiquetas, armazenagem, pedido integral/parcial de pallet, retirada, remanescente, serviço único, cálculo e fechamento. Lume encerrou a divergência de replay de chegada sem alterar o contrato legado18. **Ligação FE:** FE01–FE12 por contrato; telas não entregues.



### BE02 Base Java e configuração dos ambientes



**Status:** Concluído. **Dependências:** contrato técnico inicial de BE01 disponível; modelo operacional integrado BE01 disponível localmente.



- [x] Definir versões compatíveis, ferramenta de construção, pacote e procedimento de execução local.

- [x] Criar aplicação Spring nas camadas `controllers`, `services`, `repositories`, `models`, `dto`, `config` e `exceptions`.

- [x] Preparar configuração por ambiente, mensagens de erro, identificação das operações e estrutura de verificação sem credenciais versionadas.



**Conclusão:** aplicação inicia em ambiente de desenvolvimento definido, possui construção reproduzível e informa falhas sem expor segredos. Inicialização não pode operar por engano no ambiente real. **Base:** D03 a D05, T01 a T03, T07.



**Evidência da entrega inicial D11 em 05/10/2026:** [escolhas/contratos](docs/12-base-e-contratos-backend.md) e [validação](docs/13-validacao-base-backend.md): 12 testes, JAR local/loopback e recusa de `prod`. Naquela entrega, models/repositories eram apenas pacotes. D12 acrescentou cadastros e acesso, descritos em BE03/BE04/BE05 abaixo; [backend/README.md](backend/README.md) reflete o estado atual. **Ligação FE:** contrato de base para FE02; frontend e M01 continuam abertos.



### BE03 Estrutura SQL Server e migrações



**Status:** Em validação externa; D24 bootstrap real, D26 identidade WMSDEV/atestação e D27 V10/evolução com dados/API/concorrência DEV executados. **Dependências:** BE01, BE02; recuperação por backup/restauração e implantação operacional continuam pendentes. sa somente administrativo autorizado, nunca API.



- [x] Definir esquema WMS, ferramenta/procedimento de migrations, proteção de alvo e separação de ambientes em arquivos.

- [x] Lucas informar nomes WMS_DEV/WMS_PROD e alvo TCP127.0.0.1:1433, sa/master administrativo; execução manual definida na D20.

- [x] Conferir identidade/TLS/defaults/versão/collation reais no bootstrap administrativo D24.

- [x] Definir e atestar identidade própria WMSDEV/credencial protegida e direitos efetivos no WMS_DEV (D26; SELECT/INSERT e UPDATE em179 colunas; nenhum DELETE/DDL/role admin/PROD).

- [x] Preparar V1–V9, vínculos, restrições, histórico e procedimentos em arquivos, sem aplicar SQL.

- [x] Entregar launcher manual, organizar database com preservação e conferir fixtures/cmd.exe/caminhos/manifesto/revisão local D20.

- [x] Verificar criação, aplicação V1–V9 e retomada do BAT no SQL Server real, com DEV antes de PROD (D24).

- [x] Ensaiar evolução com dados, falhas/rollback e concorrência em WMS_DEV (D26/D27), preservando migrations/histórico/fixtures.

- [ ] Validar recuperação por backup/restauração em escopo próprio, sem reset do DEV para repetir ensaio.

- [x] Preparar o recorte cadastral: seis tabelas, mapeamentos JPA, V1, ferramenta/procedimento de migrations e proteção contra DDL automático.

- [x] Preparar V2 e seis modelos de recebimento, vínculos, restrições, índices e ampliação da auditoria, preservando V1.

- [x] Preparar V3, três modelos de unidades/composição/repetição e marcador de unitização, preservando V1/V2 e os fatos conferidos.

- [x] Preparar V4, perfil físico, conjuntos, ocupação/histórico e revisão independente de etiqueta, preservando V1/V2/V3.

- [x] Preparar V5, pedidos/itens, reservas históricas, operações idempotentes, ponte exclusiva e avaria posterior, preservando V1–V4.



**Conclusão:** estrutura pode ser criada e atualizada de modo controlado no alvo correto; inconsistências de origem e duplicidade são impedidas. Preparar script não equivale a aplicar migration. **Base:** RN04, RN16, RN29, PR02, PR04 e T03.



**Evidência D12/D15/D16/D17/D18:** [V1 a V5 e procedimento](database/migrations/README.md), configuração `sqlserver-dev` e testes locais dos documentos 15/19/21/23/[25](docs/25-validacao-pedido-saida-e-reserva.md). V5 revisada contra JPA por Cedro/Prumo/Vigia; índice filtrado, CHECKs e opções JDBC não executados no H2. **Pendente:** alvo/credenciais/versão SQL Server, aplicação e validação de V1 a V5 (inclusive evolução com dados), permissões/recuperação e ensaio integrado das V1–V9 preparadas. H2 não valida dialeto/concorrência SQL Server. Nenhuma migration foi aplicada. **Ligação FE:** persistência FE04 a FE09, sem integração real concluída.



**Complemento D19 de BE03:** V6/V7/V8 preparadas e comparadas com os respectivos freezes locais aceitos, sem SQL executado. V8/JPA523/523 e163/163; V9/fonte424/424 em leitura estática, oito tabelas/79 colunas, incluindo solicitação de encerramento e ENTRADA_CONTINGENCIA; preparo técnico194/194, fecho22/22 e307 hashes conferidos. Outputs413/18/250 preservados como histórico. JPA/guardas finais BE14 conferidos no fecho P2-locks: 904/904 e129/129, somente em arquivos. Validação de todas as migrations no alvo real, evolução/locks/permissões/recuperação continuam externas com Lucas/DBA.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Flyway externo ao build, ddl-auto=validate e SQL-init=never. H2 não comprova dialeto, índice filtrado/CHECK, isolamento, deadlock, collation ou recuperação SQL Server. **Externos:** Lucas/TI e DBA.



**Fecho local D20:** auditoria efetiva64 tabelas/687 colunas/130 FKs/200 CHECKs/61 UNIQUE/83 índices e0 divergências JPA em arquivos;34 UPDATEs Hibernate preparados, sem JDBC. Sem novo índice/migration/modelo de negócio. BAT pronto/testado, V1–V9 iguais9/9,34 movimentos/489 originais e722 hashes conferidos; revisão favorável ao launcher/organização. Integração backend396/19/404 preservada. Limite VIG07 e atestação externa obrigatória explícitos no [36](docs/36-database-local-engenharia-e-validacao.md). SQL real não executado.



**Fecho local D21 de BE03:** [BAT](database/iniciar-bancos.bat) pronto para bootstrap/upgrade completo manual dos dois nomes exatos. Migrations canônicas dinâmicas, V1–V9 intactas, V10 em fixture isolada; checksum/schema divergente e falha DEV impedem PROD.675 hashes database,84 backend, revisão favorável e evidências no [37](docs/37-bootstrap-e-upgrade-manual-database.md). Criar/aplicar/validar SQL real permanece externo.



**Fecho local D22 de BE03:** execução normal automática sem senha/confirmação, canal DPAPI WMS exclusivo por usuário/máquina, auxiliar [configurar-credencial.bat](database/configurar-credencial.bat) UMA VEZ. 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão favorável. Diretório/arquivo real ausente: operador configura, depois executa BAT; nenhuma provisão/importação real/SQL pelo agente. [38](docs/38-launcher-automatico-e-credencial-protegida.md). D21 permanece histórico, mesmos IDs/modelo/migrations.



**Evidência atual D24 de BE03:** [resultado real39](docs/39-conexao-compartilhada-e-sql-real.md); ambos os catálogos conferidos com a fonte e histórico Flyway validado. Os parágrafos D12–D22 anteriores descrevem o estado de cada entrega histórica.



**Evidência D26 de BE03:** WMSDEV/DPAPI/ACL própria provisionada e direitos completos atestados; JPA validate e IT SQL7/0/0/0 reais antes da população, fixture preservada. Jornadas HTTP159/160, provas236/0+17/0+76/0 e rollback42/0 do ajuste CHECK. V1–V9/histórico iguais D24; V10 só proposta não aplicada. [Resultado41](docs/41-ensaio-http-e-persistencia-dev.md). BE03/BE15 em validação; backend total não aprovado.



**Evidência D25 de BE03:** catálogo e histórico DEV reobservados por leitura SqlClient/JDBC protegida: 1.295/1.295 itens, 64 tabelas/687 colunas, 64 vazias e nove migrations iguais à D24. Nenhum DDL/DML/migration/reparo; identidade própria ausente, JPA validate não executado. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).



### BE04 Identidade, permissões e auditoria

**Atual D32 / AUD-LOGIN-CAD01:** login e usuários próprios implementados; mecanismo nativo, contratos e ativação DEV04 descritos no [documento 43](docs/43-login-e-administracao-de-usuarios.md). Auditoria BE04-AUD01/FE03-AUD01 concluída com sete achados abertos e propostas no [recibo](orchestracao/.runtime/login-cadastros-auditoria-resultado.md); correções aguardam Lucas. Primeiro login real/negócio autenticado não presumido por esta auditoria.

#### Histórico do item antes de D32 — preservado




**Status:** Em validação externa; acesso técnico HTTP/SQL DEV e auditoria nos casos D26/D27 exercitados; provedor/identidades operacionais não homologados. **Dependências:** BE02, BE03.



- [x] Implementar Resource Server RS256 e validação de claims/configuração conforme contrato local.

- [ ] Validar provedor, contas, login, renovação, revogação/rotação e contenção no ambiente real.

- [x] Aplicar Gestor, Supervisor e Operação, com alcance por cliente/armazém e proteção em cada ação relevante.

- [x] Registrar usuário, momento, motivo e origem/destino das operações críticas; verificar recusas de acesso.

- [x] Implementar o recorte cadastral: JWT assinado com emissor/audiência/prazo, três perfis, alcance por cliente/armazém e auditoria atômica das alterações.



**Conclusão:** acesso indevido é recusado mesmo sem usar a tela; justificativa e autorização ficam distinguíveis; o histórico permite atribuir ações. **Base:** Q20, AC11, PR06, V16 e V21.



**Evidência D12:** contrato de identidade/auditoria no [documento 14](docs/14-cadastros-acesso-e-persistencia.md), testes RSA/escopos/recusas/rollback no documento 15. **Pendente:** provedor real, ciclo de contas/login/renovação/revogação, validação de permissões no SQL Server e integração do provedor real com as ações operacionais implementadas. **Ligação FE:** contrato para FE03; login/telas ainda não implementados. O backend não emite tokens nem possui usuários padrão.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Perfis/alcances/resolução antes do replay e auditoria/rollback verificados nos serviços, incluindo BE14. Ausência de provedor/política externa não foi preenchida com emissor ou blacklist inventados. **Ligação FE:** FE03; login/telas não implementados.



### BE05 Cadastros, contratos e posições



**Status:** Em validação externa; código e jornadas HTTP/SQL DEV D26/D27 conferidos no recorte da matriz, cadastros/preços reais não homologados. **Dependências:** BE03, BE04 disponíveis para o ensaio.



- [x] Implementar clientes, armazéns, endereços, produtos, embalagens/DUN, lotes/validade aplicáveis e dados fiscais de referência.

- [x] Implementar importação de endereços com conferência de erros e duplicidades, posições especiais e limites de ocupação.

- [x] Manter tabelas/serviços, unidades de cobrança, vigências e parâmetros contratuais; aplicar encerramento/inativação conforme AC12.

- [x] Entregar API de cliente/armazém/produto/embalagem/endereço, consultas paginadas, revisão, auditoria e solicitação de encerramento/reativação.



**Conclusão:** cadastros respeitam proprietário e armazém; não há posição ambígua nem alteração silenciosa de preço histórico. **Base:** RN04 a RN09, AC05, AC06, AC10 a AC12 e AC16.



**Evidência D12/D13:** cinco conjuntos de models/repositories/services/controllers/DTOs; [contratos e limites](docs/14-cadastros-acesso-e-persistencia.md),52 testes no15. **D19:** importação Excel, tabelas/serviços/valores/vigências e complemento fiscal aceitos localmente no29/30,292 testes. **Complemento final D19:** inativação definitiva com revalidação de compromissos BE13/BE14 e avisos de validade entregues/testados localmente. Flags de lote/validade/limites cadastrados; ocupação/estoque entregues em BE08. **Ligação FE:** contratos FE04, nenhuma tela/M01 entregue.



**D19 aceito localmente no recorte:** complemento fiscal, prévia/confirmar Excel e configuração comercial do [contrato29](docs/29-cadastros-servicos-e-calculo.md); clean verify292, revisão favorável Vigia e V7/JPA compatíveis, conforme [30](docs/30-validacao-servicos-e-calculo.md). Tabelas/serviços/vigências entregues; inativação complementada no fecho D19, conforme32. Referências fiscais não comprovam autorização externa.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Inativação definitiva dos sete tipos revalida compromissos físicos/financeiros na transação; resolução específica de Gestor não reativa cadastro nem dá disponibilidade geral. Encerramento de vigência conserva o passado. **Externos:** Caio/gestor/Natalina e dados reais.



### BE06 Recebimento, XML e divergências



**Status:** Em validação externa; jornadas técnicas HTTP/SQL DEV D26/D27 exercitadas. **Dependências:** acesso/cadastros BE04/BE05 e SQL DEV BE03 disponíveis; documentos reais/conferência física/homologação operacional pendentes.



**Macrobloco autorizado em 05/10/2026:** implementar pedido/nota manual ou XML, chegadas, conferência, quarentena, correção por estorno e efetivação única da quantidade física. Registro de entrada em triagem/quarentena prepara BE07/BE08; não representa saldo disponível, endereçamento ou cobrança. Aplicar AC03 como proposta identificada.



- [x] Criar pedido manual ou por XML, validar arquivo no formato operacional suportado e associar proprietário, notas, SKUs e quantidades, sem duplicar importação.

- [x] Registrar chegadas em partes, primeira data FIFO e conferência física; preservar previsto, recebido e divergente.

- [x] Bloquear a carga conforme AC03, registrar tratativa/liberação e efetivar uma única vez a quantidade real aceita.



**Conclusão:** nota em dois dias não duplica previsão nem libera saldo incompleto; liberação tem responsável e justificativa. **Base:** RN12 a RN17, AC03, V01 a V05 e V26 a V29.



**Evidência D15:** [modelo/API](docs/18-recebimento-e-conferencia.md), seis tabelas em V2 e [86 testes totais](docs/19-validacao-recebimento-backend.md), incluindo 28 cenários de recebimento e seis de segurança XML. **Pendente de validação:** persistência/concorrência no SQL Server e documentos/cenários operacionais reais com Caio. O extrator não valida fiscalmente assinatura, XSD completo ou autorização SEFAZ. **Entregas distintas:** unitização disponível em BE07 após D16; endereçamento/disponibilidade ainda em BE08 e correções após efetivação em BE11. **Ligação FE:** contratos FE05 disponíveis, tela não implementada. M02 não concluído.



### BE07 Unidades logísticas e etiquetas



**Status:** Em validação externa; unitização/etiquetas via API e persistência DEV D26/D27 exercitadas. **Dependências:** BE05/BE06/SQL DEV disponíveis; impressora/leitor/uso físico e homologação operacional pendentes.



**Macrobloco autorizado D16 em 05/10/2026:** unitização por entrada conferida, identidade permanente, divisão/reagrupamento rastreáveis e dados para etiqueta. Endereçamento e disponibilidade continuam em BE08; impressão/leitura física dependem de FE06 e equipamentos.



- [x] Unitizar quantidades conferidas com ID permanente e vínculo de origem; impedir mistura de SKU/lote/data não permitida.

- [x] Representar quantidade de produto e embalagem separadamente; manter vínculos em divisão ou reagrupamento autorizado.

- [x] Fornecer dados da etiqueta com SKU, ID, código de leitura, data, nota e quantidade; reimprimir preservando identidade.



**Conclusão:** quantidades unitizadas fecham com o recebido e etiquetas não criam saldo; mudanças de conteúdo permanecem rastreáveis. **Base:** RN10, RN11, RN15, RN16, AC10, V02 a V04, V09 e V25.



**Evidência D16:** [modelo/API e limites](docs/20-unidades-logisticas-e-etiquetas.md), V3 e [122 testes totais na entrega original](docs/21-validacao-unidades-logisticas.md), com 36 casos novos de conservação, permissões, repetição, concorrência e rollback. **Ampliação D17:** BE08 calcula disponibilidade e separa revisão da etiqueta; divisão/reagrupamento restringidos a unidades não endereçadas e sem bloqueio. **Pendente de validação:** persistência/locks no SQL Server e cenários físicos com Caio. **Ligação FE:** contratos FE06 disponíveis; layout, simbologia, impressão Tanca e leitura física ainda não implementados/validados. M02 não concluído.



### BE08 Endereçamento, movimentação e saldo



**Status:** Em validação externa; movimentos/saldo/API/SQL DEV D26/D27 confrontados nos casos publicados. **Dependências:** BE05 a BE07 disponíveis; geometria/equipamentos/uso operacional permanecem validações próprias.



**Macrobloco D17 autorizado em 05/10/2026:** capacidade física, conjuntos de duas posições, confirmação de leitura/endereço, movimentos atômicos, bloqueio/liberação e consulta de disponibilidade. Preservar AC04/AC05 como propostas e manter reserva, saída e cobrança nas etapas próprias.



- [x] Confirmar unidade/endereço, validar capacidade e ocupar conjuntamente as posições necessárias.

- [x] Centralizar alterações de estoque e localização, com proteção contra concorrência, repetição e gravação incompleta.

- [x] Expor físico, disponível e bloqueado; registrar primeiro endereço, início de armazenagem, equivalência e histórico. D18 integra reservado e avaria posterior conforme o documento 24.

- [ ] Conferir V4, locks, isolamento do saldo e permissões no SQL Server; validar capacidade real e leituras com a operação.



**Conclusão:** duas operações não ocupam indevidamente a mesma posição; remanejamento não cria entrada nem perde histórico. **Base:** RN18 a RN21, PR01 a PR07, AC04/AC05, V08, V14/V15, V23 e V30.



**Evidência D17:** [contratos e limites](docs/22-enderecamento-movimentacao-e-estoque.md), V4 preparada e [153 testes totais](docs/23-validacao-estoque-backend.md), com 31 novos de saldo, capacidade, concorrência entre clientes, quarentena, permissões, repetição e rollback. JAR local conferido e encerrado naquela entrega. **Integração D18:** saldo reservado, bloqueio por avaria posterior e lock do armazém antes de encerrar endereço; os 31 testes BE08 e os demais anteriores passaram no [build com 197 testes](docs/25-validacao-pedido-saida-e-reserva.md). **Pendente de validação:** itens externos acima. AC04/AC05 continuam propostas. **Ligação FE:** contratos FE07/FE08 disponíveis; telas/coletor não implementados. M02 não concluído.



### BE09 Pedido de saída, FIFO e reserva



**Status:** Em validação externa; pedido/RN22/FIFO/reserva e concorrência HTTP/SQL DEV D26/D27 exercitados. **Dependências:** BE04/BE08/SQL DEV disponíveis; provedor e operação reais não homologados. **Autorização:** D18/D26/D27 nos respectivos limites.



- [x] Criar pedido por proprietário/armazém, validar saldo por SKU e sugerir FIFO com data original e desempate documentado.

- [x] Confirmar reserva sem vencimento, com justificativa/autorização de exceção e proteção contra disputa simultânea.

- [x] Reservar quantidade parcial de pallet sem permitir atendimento parcial do pedido; sinalizar avaria posterior e pedido afetado.

- [ ] Homologar V5, concorrência, saldo e permissões no SQL Server e conferir os cenários com a operação.



**Conclusão:** reserva não duplica nem desaparece por tempo; quantidade e unidade permanecem coerentes; bloqueios impedem expedição. **Base:** RN23/RN24, Q07 a Q09, AC10/AC11, V06/V07, V13/V17, V24 e V31/V32.



**Evidência D18 em 05/10/2026:** [contratos](docs/24-pedido-saida-fifo-e-reserva.md), V5 preparada e [validação](docs/25-validacao-pedido-saida-e-reserva.md). `spotless:apply` e `clean verify` reais aprovados com JDK 21: **197 testes, zero falhas/erros/ignorados**, sendo 44 novos. Concorrência, pedido inteiro/rollback, repetição, FIFO/exceção, permissões/escopos, indisponibilidades, avaria posterior, reconciliação, cancelamento/reversão/auditoria testados em H2 isolado. Vigia favorável, sem novos achados materiais; schema JPA/V5 conferido em leitura. Graphify atualizado. Git `main`, zero commits e conteúdo preexistente preservado. **Pendente:** validação externa acima; H2 não comprova dialeto, índices, CHECKs, isolamento ou deadlocks SQL Server. **Ligação FE:** FE09 e saldo FE08 com contratos disponíveis, sem telas. AC10/AC11 continuam propostas. **Continuação D19:** BE10/BE11 aceitos localmente no [documento28](docs/28-validacao-separacao-retirada-retornos.md); demais blocos seguem pelo [plano26](docs/26-execucao-continua-backend.md), sem nova autorização por etapa.



### BE10 Separação, documentos e retirada física



**Status:** Em validação externa. **Dependências:** BE07, BE09 disponíveis localmente. **Autorização:** D19; código local e revisão concluídos junto com BE11.



- [x] Conferir leituras das unidades reservadas e completar separação com quantidades corretas.

- [x] Vincular documentos existentes do NOTAZZ/XML no procedimento inicial, inclusive várias notas e itens; impedir repetição divergente ou conclusão com pendência. Sem emissão pelo WMS.

- [x] Confirmar retirada por Supervisor/Gestor com XML, baixar estoque e registrar término da permanência conforme AC01; separar retorno simbólico de movimento físico.



**Evidência D19:** [contrato 27](docs/27-separacao-retirada-retornos-e-avaria.md) e [validação 28](docs/28-validacao-separacao-retirada-retornos.md). Spotless/clean verify reais com JDK21, **240 testes, zero falhas/erros/ignorados**, 43 expedição + 197 anteriores. Vigia favorável no freeze P2; V6 preparada e compatível em leitura, 74 colunas/58 checks. **Externos:** SQL Server/locks/dialeto com Lucas; documentos/procedimento fiscal com Natalina; percurso/equipamento com Caio/Mickael. H2 e XML extraído não homologam essas etapas. **Ligação FE:** FE09/FE10 com contrato, sem telas.



**Conclusão:** nota emitida hoje e retirada amanhã mantêm datas e saldo corretos; rejeição ou ausência de documento exigido não conclui a saída. **Base:** RN25/RN26, AC01/AC02, V10/V11, V18, V34 e V39.



### BE11 Cancelamentos, retornos e avaria



**Status:** Em validação externa. **Dependências:** BE06, BE08 a BE10 disponíveis localmente; código desenvolvido no mesmo bloco D19.



- [x] Cancelar pedido inteiro conforme os efeitos já ocorridos, preservando pendências fiscais e serviços prestados.

- [x] Retornar separado à posição sem criar recebimento; tratar retorno após retirada como nova entrada com vínculo e FIFO original.

- [x] Registrar quantidade avariada, destino, responsabilidade e momento reconhecido; fornecer os fatos para ajuste financeiro.



**Evidência D19:** mesmos contrato/build/revisão de BE10. Bases temporais de avaria por fato/snapshot imutável BE07, ou recusa explícita `HISTORICO_AVARIA_INSUFICIENTE`; reparo conserva condição original/reserva e considera somente o bloqueio do ciclo vigente. Retorno de 201 reservas e rollback integral, perfis no replay, duas posições e snapshot por ciclo testados. **Integração seguinte:** efeito financeiro BE12/BE13. **Externos:** tratativa e destino reais com Caio/cliente, fiscal com Natalina e demonstrativo com gestor. **Ligação FE:** FE10/FE11, sem telas.



**Conclusão:** cancelamento não libera mercadoria fisicamente separada ou avariada por engano; retorno externo conserva origem sem apagar saída anterior. **Base:** Q04/Q10/Q21, AC01/AC08/AC10, V17, V21, V33 e V46.



### BE12 Serviços e cálculo de armazenagem



**Status:** Em validação externa; código local aceito. **Dependências:** recorte comercial BE05 e BE08/BE10/BE11 disponíveis localmente. **Autorização:** D19; contrato/schema antes do código e evidência no [documento30](docs/30-validacao-servicos-e-calculo.md).



- [x] Registrar serviços únicos, quantidade, unidade, origem, responsável, execução e aprovação; impedir duplicidade entre sugestão e lançamento manual.

- [x] Implementar cálculo por calendário, posição equivalente, pico e vigência, incluindo mesma data, duas posições, separação, quarentena e avaria conforme propostas AC04 a AC08.

- [x] Parametrizar preços, mínimo e eventual GRIS com memória de cálculo; informação ausente não vira valor zero nem preço copiado de outro cliente.



**Conclusão:** exemplos documentais e testes explicam cada valor e período. Fórmula proposta fica identificada até conferência do demonstrativo real; não confundir valor da mercadoria com valor de serviços. **Base:** RN21/RN27, AC04 a AC09, V19/V20, V35, V40 e V46 a V48.



**D19 aceito localmente:** P2 financeiro BE08 corrigido por movimentos/ciclos imutáveis e pendência sem prova histórica. Clean verify **292/0/0/0**, BUILD SUCCESS às02:17:02; Farol conferiu14 XMLs/JAR/222 hashes,217 iguais ao freeze286/cinco diferenças declaradas. Vigia favorável às02:33 sem achados materiais; Prumo V7/JPA518/518 e suplemento67/67 compatíveis. Evidência no [30](docs/30-validacao-servicos-e-calculo.md), contrato no [29](docs/29-cadastros-servicos-e-calculo.md). Naquela etapa seguiu BE13 autonomamente; fecho integrado D19 abaixo. **Externos:** gestor/comercial valida AC04–AC09/preços/cortes, Natalina fiscal e Lucas SQL Server.



### BE13 Fechamento e entrega de valores ao ESL



**Status:** Em validação externa; código local aceito. **Dependências:** BE12 aceito localmente. **Autorização:** D19; refinamento31 antes do código/schemaV8, sem nova autorização do usuário.



- [x] Consolidar ciclos do cliente, respeitar último corte e exigir aprovação/rejeição integral do gestor.

- [x] Preservar versões, reabertura anterior à emissão e ajustes posteriores no próximo ciclo; impedir faturamento repetido.

- [x] Preparar demonstrativo para o procedimento inicial de NFS-e no ESL, com referência do documento emitido e sem duplicar tributos.



**Conclusão:** ciclos não repetem dias; correções explicam sua origem e documento faturado não é silenciosamente reescrito. **Base:** AC02/AC06/AC07, V12, V19, V36 e V45.



**D19 aceito localmente:** [contrato31](docs/31-fechamento-contagem-e-contingencia.md) e refinamentos registrados antes dos ajustes Java/V8. Três P2 finais corrigidos: ajustes originados, período e auditoria da resolução histórica. Bateria93/0/0/0 e clean verify **333/0/0/0**, BUILD SUCCESS04:17:56; spotless238 Java aprovado. Farol conferiu15 XMLs/JAR/251 hashes; Vigia favorável04:29, sem achado material remanescente. V8/JPA **523/523** e suplemento**163/163**,17 models/118 campos compatíveis em leitura;11 tabelas/112 colunas SQL preparadas. Histórico328/251 e anteriores preservados; [32](docs/32-validacao-fechamento-e-contingencia.md) registra outputs/limites. Índice nullable H2 não comprova o filtro SQL Server. **Externos:** Natalina/gestor validam procedimento ESL/fiscal e condições reais; Lucas/DBA, SQL Server. Naquela etapa seguiu BE14/inativação BE05, agora entregues localmente; nenhum envio/emissão/cobrança real.



### BE14 Consultas, contagem e contingência



**Status:** Em validação externa; código local aceito. **Dependências:** BE06 a BE13, conforme a consulta ou operação; consultas físicas disponíveis após BE08.



**Recorte D17:** lista por cliente/armazém, filtros de produto/disponibilidade, saldo por SKU e histórico paginado de movimentos entregues para FE08, conforme [documentos 22](docs/22-enderecamento-movimentacao-e-estoque.md) e [23](docs/23-validacao-estoque-backend.md). Naquele recorte, indicadores financeiros, contagem, carga inicial, ajustes e contingência não estavam entregues; foram complementados no fecho D19 abaixo.



**Complemento D18:** reservado e bloqueado reconciliados com o físico unitizado, indicação de avaria posterior e reserva vinculada à unidade no [documento 24](docs/24-pedido-saida-fifo-e-reserva.md); [197 testes](docs/25-validacao-pedido-saida-e-reserva.md) aprovados. Não entrega valor financeiro, contagem ou contingência.



**Andamento D19:** após aceite local BE13/333, Cedro formalizou schema/API BE14 e inativação BE05 no rodapé do [31](docs/31-fechamento-contagem-e-contingencia.md), antes do Java: oito tabelas de contagem/revisão, estágio/revisão inicial, linha/dependência de contingência, aviso de validade e resolução de remanescente. Backend/contrato31 atribuídos a Cedro; V9 somente em arquivos a Prumo; Lume conferiu o novo contrato em leitura. Check comprovou trabalho/leitura reais. Registro da implementação anterior ao fecho final D19; aceite/checklist atual abaixo.



**Revisão preliminar D19:** Lume encerrou os residuais de desenho da carga/dependências/efeito registrado; Vigia apontou um P1 e cinco P2 no código em construção, encaminhados a Cedro e formalizados no31 antes do ajuste. Os achados exigiam correção de revisão antiga, tempo do vínculo, obrigação financeira, cortes de vigência/tabela padrão e ordem de locks; foram corrigidos e verificados no fecho abaixo. Após correções, bateria de integração físico-financeira117/0/0/0, BUILD SUCCESS06:46:08:52 financeiro,12 contingência,43 fechamento e10 jornada HTTP própria. Entrada temporal conserva instante físico; replay devolve confirmação original; ajuste a zero comprova fim físico no cálculo. Naquela bateria parcial, validade e concorrência de encerramento/compromisso ainda estavam em teste; passaram posteriormente no fecho367 abaixo. Tentativas iniciais preservadas. Consultas FE08/FE12 e jornada BE01 formalizadas antes do Java no31; nenhum aceite BE14 antecipado. Detalhes e outputs no32; revisão final somente após freeze e build completos.



- [x] Expor filtros, histórico, indicadores essenciais e valor da mercadoria proporcional, incluindo exclusão de avaria somente do indicador aplicável.

- [x] Implementar contagem simples, eventual carga inicial e ajustes aprovados, sem editar a nota para esconder diferenças ou desconsiderar reservas.

- [x] Permitir lançamento/conciliação rastreável da planilha de contingência, com identificador único e prevenção de repetição; não pressupor importação automática irrestrita.



**Conclusão:** consultas reconciliam saldos com movimentos; contagem e contingência mantêm origem, reserva e responsável. **Base:** RN20/RN28/RN29, Q18/Q19, AC13 a AC15, V21, V37, V41 e V44.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. P2 residual resolvido: o alvo do DTO executado define pedido/cliente/armazém/linha; referências redundantes incompatíveis são recusadas antes dos efeitos. Duas ordens concorrentes, replay/perfis/rollback testados. Carga PENDENTE/PREPARADA/REGULARIZADA conserva ausências e entrada única; contagem não corta pedido/reserva nem recompõe baixas retiradas; contingência preserva identidade global/instante real/dependências e GET atual. **Ligação FE:** FE08/FE12; nenhum frontend. **Externos:** Caio/cliente, operação/Mickael/TI e Lucas/DBA.



### BE15 Validação técnica e preparação do piloto



**Verificação ampliada D30 em andamento:** os aceites locais históricos abaixo conservam o recorte original; nova auditoria integral de atributos/usos/contratos/casos locais nos dez critériosD30, sem aceite antecipado ou frontend. [MatrizD30](orchestracao/.runtime/d30-matriz-aceite-local.json).



**Status:** D29 em andamento: cobertura normativa/cenários/propostas e lacunas técnicas no DEV por nova autorização. D28 encerrada com limites preservados. Homologação operacional/comercial/piloto permanece externa. **Dependências:** preflight novo DEV/WMSDEV, oráculos e revisão; dispositivos/fiscal externo/recuperação em destino isolado fora deste escopo. [Escopo D29](orchestracao/.runtime/d29-autorizacao-e-distribuicao.md).



**Preparação D19 observada:** Prumo preparou [configuração externa, diagnóstico e planos de recuperação/ensaio](docs/33-preparacao-tecnica-local-backend.md) em arquivos. O diagnóstico final real conferiu194/194 condições estáticas, incluindo as56 originais; fecho22/22,307 hashes e240 links conferidos por Farol. Vigia favorável às06:09, sem achado material no novo pacote; preparo local em arquivos aceito. Não consulta ambiente, rede ou SQL nem executa JVM/build. Testes integrados finais de367 cenários, artefato e revisão local concluídos no32; ensaios externos continuam pendentes.



- [x] Conferir regras, permissões, concorrência, repetições, falhas fiscais e cálculo nos testes locais adequados.

- [x] Validar conexão administrativa, migrations e catálogo no SQL Server existente (D24).

- [x] Executar operações HTTP integradas e concorrência determinística no SQL Server DEV/WMSDEV (D26: build412/IT7/API159/160, fotografias236/0+17/0+76/0; HTTP/JVM e SQL posterior separados).

- [x] Fechar as lacunas técnicas selecionadas da D26 por D27: ajuste E052, financeiro não zero e witness SQL nos casos do resultado42; demais combinações RN/AC e homologação operacional permanecem delimitadas.

- [x] D28: regressão das29 correções inventariadas com evidências atuais, matriz e revisão favorável no escopo comprovado; auxiliares offline e impedimento witnessSQL300 separados, sem homologação total.

- [ ] D29: matriz integral documental e novos casos DEV, exemplos AC04–08, concorrência real repetida/observabilidade permitida, correções red/green e encaminhamentos externos precisos.

- [x] Preparar execução, configuração, diagnóstico, cópias de segurança e recuperação em arquivos.

- [ ] Ensaiar backup/restauração, recuperação e desempenho com Lucas/DBA, medindo critérios reais.

- [x] Registrar artefato construído, verificações, limites e plano de recuperação para a liberação conjunta; publicação é um registro separado.



**Conclusão:** há evidência da versão candidata e do que foi verificado; nenhuma afirmação de operação real decorre apenas de build ou testes locais. **Base:** PR01 a PR11, AC14 a AC16, V13 a V23 e V42/V44.





**Fecho local D19:** 367/0/0/0 no clean verify final P2-locks,17 XMLs/JAR e289 hashes conferidos; Vigia favorável às07:58 e V9/JPA 904/904 mais129/129 em arquivos. [Evidência integrada32](docs/32-validacao-fechamento-e-contingencia.md) e [matriz34](docs/34-matriz-e-validacao-final-backend.md). Sem validação externa presumida. Candidato local, procedimentos e recuperação separados da homologação/piloto/publicação. Donos e lacunas externos no33/34; não inventados RPO/RTO, volumetria, credenciais, preço, parâmetro fiscal ou data de piloto. Frontend e validação conjunta futura permanecem fora desta entrega.



**Fecho local D20:** runner manual/guardas/procedimentos/ensaio optativo preparados, backend396/0/0/0 em19 suítes e arquivo conferido; testes locais após organização80/21/13/336, cmd.exe0/0/2/2,722 hashes. SQL Server, TLS/identidades, privilégios efetivos completos (VIG07), schema/locks e recuperação continuam externos, sem aceite presumido. [Resultado36](docs/36-database-local-engenharia-e-validacao.md). Frontend/homologação conjunta fora do bloco; trabalho local encerrado.



**Fecho local D21 de BE15:**41/41 fixtures do fluxo,672/672 checks de pacote,25/25 Maven e cmd.exe offline0/0/2/2;675 hashes database e84 backend conferidos. Quedas parciais/reexecução e versão futura verificadas por mocks, não SQL Server. Perfil manual administrativo permite DEV/PROD sem novos grants, runtime continua restrito. Revisão Vigia favorável; [37](docs/37-bootstrap-e-upgrade-manual-database.md) e recibo WMS. Não repetir396/build por launcher; nenhum SQL real pelo agente.



**Fecho local D22 de BE15:** 18/18 fixtures DPAPI fictícias, 5/5 fixtures de console e 9/9 casos cmd.exe independentes; 818/818 hashes database, 84/84 backend, 73 snapshots D21 e V1–V9 intactas; revisão independente favorável. Ausência, SID/owner/ACL, erro DPAPI/tipo/login e descarte de cópias verificados com fixtures isoladas; console Explorer/FOR-F/filho sem recursão por metadados simulados. Sem duplo clique visual ou SQL real. Backend/POM sem alterações, nenhuma suíte integral/Maven repetida. Provisionamento real e ensaio SQL/TLS permanecem do operador, conforme [38](docs/38-launcher-automatico-e-credencial-protegida.md).



**Evidência atual D24 de BE15:** conexão e BAT real DEV/PROD concluídos; 18 testes D22 e nove testes D24 aprovados. [Resultado39](docs/39-conexao-compartilhada-e-sql-real.md). Homologação operacional, permissões da aplicação e recuperação continuam pendentes.



**Evidência D25 de BE15:** build atual 408/0/0/0 e JAR 6/6 HTTP reais; SqlClient/JDBC administrativo DEV com TLS validado. Vigia favorável; sa nunca API/IT. Operações/concorrência SQL e sete ITs não executados por falta de identidade própria; backup/restauração e homologação permanecem externos. Nenhuma fixture SQL criada. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).



### BE16 Padrões e verificações de engenharia

**BE16-JAVA01 — avisos Java informados em 08/10/2026:** correção e validação Java concluídas; atualização do mapa em andamento. Chamada de `NfeXmlService.hash` pela classe, conversões JSON/Map tipadas em Contingencia/Expedicao/Auditoria e remoção dos dois membros sem uso em D29FinanceiroRetomadaTest. Dependência: BE16; contratos BE→FE preservados. [Resultado e fontes](orchestracao/.runtime/avisos-java-20261008/resultado.json), [diff deste recorte](orchestracao/.runtime/avisos-java-20261008/alteracoes.diff) e fontes anteriores em `orchestracao/.runtime/avisos-java-20261008/antes/`. Formatação aprovada; 259 fontes main e 66 fontes test compiladas; dez testes pertinentes em cinco classes, zero falhas/erros/ignorados, quatro contextos H2 em memória comprovados pela guarda local. `javac -Xlint:unchecked,static -Werror` nos cinco arquivos: antes exit1/16 avisos, depois exit0/zero avisos. Compilação geral conserva um aviso de depreciação preexistente em D30BearerCookieTest, fora do pedido. Build próprio `backend/target-java-avisos-20261008`, sem SQL Server, JAR ou regressão integral. Manutenção posterior à FINAL14, com provas históricas preservadas; não reabre a auditoria D30. Próximo passo deste recorte: concluir e registrar `graphify update .`.



**Verificação ampliada D30 em andamento:** os aceites locais históricos abaixo conservam o recorte original; nova auditoria integral de atributos/usos/contratos/casos locais nos dez critériosD30, sem aceite antecipado ou frontend. [MatrizD30](orchestracao/.runtime/d30-matriz-aceite-local.json).



**Evidência atual D25:** JDK 21.0.12.1/Maven 3.9.16, formatação e clean verify atual com 408/0/0/0 em 20 suítes; HTTP/arquitetura/serviços/regressão executados. Correções focais de TLS cliente, critério/perfil do IT e sessão local, sem alteração POM/MVC/models/migrations. JAR final conferido e encerrado após 6/6 checks; revisão Vigia favorável. [Resultado40](docs/40-testes-backend-e-leitura-sql-dev.md).



**Status:** Concluído. **Dependências:** BE02; recorte cadastral de BE03/BE04/BE05 disponível.



- [x] Padronizar configuração Spring em `.properties`, preservando ambientes e proteções.

- [x] Automatizar conferência de ferramentas, formatação e separação das camadas MVC no build.

- [x] Validar contratos também na entrada dos serviços e testar chamadas fora do HTTP.

- [x] Registrar convenções, comandos e evidências da validação local.



**Conclusão:** padrões definidos e verificáveis no backend atual, com build/testes aprovados. Não substitui BE15 nem homologação dos módulos futuros. **Base:** pedido do responsável de usar properties e boas práticas de engenharia. **Ligação FE:** mantém os contratos FE02/FE03/FE04; sem tela nova.



**Evidência D13 em 05/10/2026:** [padrões](docs/16-padroes-de-engenharia-backend.md), Enforcer/Spotless, testes ArchUnit, validação dos serviços e perfis properties. `clean verify` aprovado com 52 testes (12 adicionados), conforme [registro](docs/15-validacao-cadastros-backend.md). **Próximo passo:** aplicar esses padrões às próximas entregas do resumo; não há pendência neste recorte. BE15/validações reais permanecem com escopo próprio.



**Manutenção em 06/10/2026:** `.gitignore` ajustado a pedido do responsável para builds alternativos, binários, temporários e evidências brutas/cópias locais. Fontes, migrations oficiais, referências, relatórios de entrega, manifestos SHA-256 e scripts atuais permanecem versionáveis. Conferência por inventário Git e `git check-ignore`; arquivos preservados no disco, sem alteração do índice ou commit. Mudança restrita ao versionamento, sem novo build/teste de aplicação e sem alterar o aceite D19 ou os contratos frontend.



**Complemento do `.gitignore` em 06/10/2026:** a pedido do responsável, `.agents/`, toda a pasta `orchestracao/`, `.ai-memory.toml` e `.graphifyignore` passam a ser apoio local fora do Git. `.codex/`, `.maestri/`, `.graphify/`, `graphify-out/` e `.tools/` já estavam excluídas. Foram retirados **28 arquivos adicionais** da lista de candidatos ao versionamento; **369 arquivos de fontes, migrations, documentação e infraestrutura** foram conferidos como preservados nas regras. `AGENTS.md`, `STATES.md` e a configuração compartilhada `.vscode/settings.json` continuam versionáveis. Arquivos das ferramentas permanecem no disco e operacionais; nenhum arquivo apagado, índice modificado ou commit realizado. Conferência com `git check-ignore`, sem build de aplicação. Escopo BE16, sem mudança de contratos frontend ou do próximo passo vigente D20.



**Manutenção Java em 06/10/2026:** avisos de APIs obsoletas e membros/variáveis sem uso corrigidos, com atualização automática Java/Maven configurada no VS Code. Restrições SQL, preparação dos cenários e contratos frontend preservados. `spotless:apply clean verify` aprovado com **367/0/0/0 em 17 suítes**; compilação adicional Eclipse JDT sem avisos/erros e Graphify atualizado. [Evidência da manutenção](backend/evidencias/manutencao-java-2026-10-06.md), com artefato separado em `backend/target-avisos/`; o aceite/JAR D19 anterior permanece histórico e preservado. **Complemento do editor:** servidor Java do workspace WMS reiniciado e reimportação/atualização Maven confirmadas nos logs do VS Code; controle visual indisponível. Aviso novo de `SqlServerUpdateMappingTest` corrigido com a API atual do Hibernate: teste direcionado **1/0/0/0**, compilação Eclipse JDT sem avisos/erros e Graphify atualizado; detalhes na mesma evidência. Alterações concorrentes de D20 preservadas. **Próximo passo:** segue a etapa vigente do resumo, sem nova entrega frontend nem alteração das pendências de homologação.



## Trilha frontend



As entregas desta trilha ficam em `frontend`. Telas de gestão e coletor usam React/TypeScript. Usar linguagem simples, feedback de confirmação e estados claros de erro; ações críticas dependem da resposta do backend.



### FE01 Jornadas, telas e contratos



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** DOC01 a DOC04; alinhamento com BE01.



- [x] Desenhar jornadas por perfil: cadastro, entrada, conferência, etiqueta, endereço, consulta, reserva, separação, retirada e fechamento.

- [x] Definir campos, ações, bloqueios, mensagens e navegação; destacar quais tarefas usam coletor.

- [x] Alinhar contratos e exemplos fictícios com BE01, incluindo erro, vazio, carregamento, conflito e indisponibilidade.



**Conclusão:** cada ação de tela tem responsável, condição e resultado de negócio; a jornada não inventa liberações ou cálculos locais. **Base:** RN30, AC01 a AC16 e FE/BE conforme as entregas seguintes.



**Evidência D31 atual FE01:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE02 Base React e componentes comuns



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE01 e contrato inicial de BE02.



- [x] Definir ferramentas e organização interna, criar base React/TypeScript e procedimento de execução local.

- [x] Preparar navegação, formulários, tabelas, diálogos, mensagens e comunicação centralizada com a API.

- [x] Implementar tratamento comum de carregamento, erros, conflito e repetição de envio; preparar verificação de tipagem e construção.



**Conclusão:** estrutura é utilizável em telas administrativas e coletor, com confirmação explícita e sem dependência de dados produtivos. **Base:** D02, RN30, PR02 e T02.



**Evidência D31 atual FE02:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE03 Acesso e navegação por perfil

**Atual D32 / AUD-LOGIN-CAD01:** login e usuários próprios implementados; mecanismo nativo, contratos e ativação DEV04 descritos no [documento 43](docs/43-login-e-administracao-de-usuarios.md). Auditoria BE04-AUD01/FE03-AUD01 concluída com sete achados abertos e propostas no [recibo](orchestracao/.runtime/login-cadastros-auditoria-resultado.md); correções aguardam Lucas. Primeiro login real/negócio autenticado não presumido por esta auditoria.

#### Histórico do item antes de D32 — preservado




**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE02, BE04.



- [ ] Implementar entrada/saída de sessão e apresentação das ações autorizadas para Gestor, Supervisor e Operação.

- [x] Exibir contexto de cliente/armazém e tratar sessão expirada, acesso recusado e troca de contexto sem reutilizar dados indevidos.

- [ ] Disponibilizar gestão de usuários e atribuições conforme o contrato aprovado em BE04.



**Conclusão:** interface e API concordam quanto ao acesso; ocultar botão não é a única proteção. **Base:** Q20, AC11 e V16.



**Evidência D31 atual FE03:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados. Sessão fictícia exercitada; provider real e usuários sem rota pendentes.

### FE04 Cadastros e importação de endereços



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE03, BE05.



- [x] Criar telas de cliente, armazém, posição, produto, embalagem/DUN e informações fiscais necessárias ao fluxo.

- [x] Permitir conferência da importação de endereços, com erros identificáveis e resultado sem duplicidade.

- [x] Apresentar tabelas/serviços, vigências e parâmetros; mostrar impedimentos de inativação/encerramento e histórico pertinente.



**Conclusão:** usuário distingue campos obrigatórios, erros e efeito das alterações; valores reais não são preenchidos com exemplos de outro cliente. **Base:** RN04 a RN09 e AC05/AC06/AC10/AC12.



**Evidência D31 atual FE04:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE05 Entrada, conferência e quarentena



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE04, BE06.



- [x] Criar lista/detalhe e pedido manual/XML, com notas, itens e chegadas em partes.

- [x] Registrar contagem, falta, sobra, avaria e tratativa, mostrando diferença entre previsto e recebido.

- [x] Exibir bloqueio da carga e permitir liberação somente ao perfil autorizado, com confirmação do servidor.



**Conclusão:** operador entende o que está em conferência, bloqueado ou liberado; importar XML não aparece como recebimento físico concluído. **Base:** AC03, V01 e V26 a V29.



**Histórico D15:** rotas, DTOs, permissões, estados e regras de repetição no [documento 18](docs/18-recebimento-e-conferencia.md). Naquele momento, FE05 estava planejada, sem aplicação/tela ou teste de integração frontend.

**Atualização FE05-PED01:** a implementação local FE05 foi entregue em D31 e reorganizada em FE05-PED01, com [Pedidos de entrada e seções do pedido](docs/design-system/etapa-07.md). Validação real de ambiente permanece separada.



**Evidência D31 atual FE05:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE06 Unitização, etiquetas e impressão



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE05, BE07.



- [x] Distribuir quantidades em unidades, mostrar identidade/origem e apresentar divergências da soma.

- [ ] Exibir e imprimir etiqueta no formato definido, com SKU, ID, data, nota, quantidade e código legível, sem endereço fixo.

- [x] Reimprimir sem criar unidade e substituir etiqueta de quantidade desatualizada após retirada parcial.



**Conclusão:** etiqueta e registro correspondem; impressão/leitura são verificadas nos equipamentos disponibilizados por Mickael antes do piloto. **Base:** RN10/RN11/RN16, AC10, V02 a V04, V09 e V43.



**Backend disponível após D16:** contratos de unitização, divisão/reagrupamento, progresso, leitura por UUID e dados atuais da etiqueta no [documento 20](docs/20-unidades-logisticas-e-etiquetas.md). FE06 permanece planejada: não há tela, layout de impressão, PDF/ZPL ou envio à impressora. Reimpressão consulta a identidade existente; frontend deve substituir etiqueta cujo conteúdo mudou.



**Evidência D31 atual FE06:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados. Prévia/QR e dados atuais; impressão/layout físico pendentes.

### FE07 Coletor para endereçamento e movimentação



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE03, FE06, BE08.



**Registro histórico preservado; fase atual D31 acima.** **Contrato disponível D17:** capacidade, conjuntos, leitura/posição, bloqueio/liberação e repetição em [documento 22](docs/22-enderecamento-movimentacao-e-estoque.md). Sem tela implementada ou leitura no coletor real.



- [x] Criar sequência simples de leitura da unidade e posição, com foco adequado e ações acessíveis.

- [x] Mostrar armazém, destino, limites, posições necessárias e recusa de leitura/capacidade incompatível.

- [x] Aguardar confirmação do servidor antes de exibir sucesso; tratar leitura repetida, perda de resposta e tentativa concorrente.



**Conclusão:** operador identifica o movimento confirmado e consegue corrigir a leitura sem duplicar operação; verificar no coletor real. **Base:** RN18/RN30, AC05, V08, V14/V15, V23 e V30.



**Evidência D31 atual FE07:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE08 Estoque, filtros e rastreabilidade



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE03, BE08 e contratos de consulta de BE14.



**Registro histórico preservado; fase atual D31 acima.** **Contrato disponível D17/D18:** lista, saldo por produto e histórico em [documento 22](docs/22-enderecamento-movimentacao-e-estoque.md); reservado e avaria posterior complementados no [documento 24](docs/24-pedido-saida-fifo-e-reserva.md). Indicadores financeiros seguem suas etapas; nenhuma tela entregue.



- [x] Exibir saldo físico, disponível, reservado, quarentena e avaria, com filtros por cliente, SKU, unidade, armazém, endereço e situação.

- [x] Mostrar origem, localização, movimentos e datas FIFO/chegada/armazenagem sem confundi-las.

- [x] Apresentar indicadores essenciais e aviso de validade quando aplicável, sem tratar erro de consulta como estoque vazio.



**Conclusão:** quantidades e condições reconciliam com o backend e respeitam o alcance do usuário. **Base:** RN20/RN28, Q01/Q18/Q21, V07, V17, V37 e V38.



**Evidência D31 atual FE08:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE09 Saída, FIFO, reserva e separação



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE06, FE08, BE09 e etapa de separação de BE10.



**Registro histórico preservado; fase atual D31 acima.** **Contrato disponível D18:** criação, consulta, sugestão FIFO, justificativa/autorização, reserva, reversão/cancelamento e revalidação em [documento24](docs/24-pedido-saida-fifo-e-reserva.md). **Complemento D19:** leitura/separação BE10 no [documento27](docs/27-separacao-retirada-retornos-e-avaria.md), aceito localmente com testes/build e revisão após correção temporal no [28](docs/28-validacao-separacao-retirada-retornos.md). Frontend não iniciado.



- [x] Criar pedido, mostrar sugestão FIFO e confirmar reserva; permitir justificativa/autorização de exceção por perfil.

- [x] Exibir reserva sem vencimento, pendência fiscal, indisponibilidade concorrente e avaria posterior.

- [x] Conferir leitura para separação, incluindo parcial de pallet com pedido integral e etiqueta correta do remanescente.



**Conclusão:** a interface não promete saldo antes da confirmação nem conclui parte do pedido; erros de concorrência são compreensíveis. **Base:** AC10/AC11, V06/V07, V13/V17, V24 e V31/V32.



**Evidência D31 atual FE09:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE10 Fiscal, retirada, cancelamento e devolução



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE09, BE10, BE11.



- [x] Vincular/exibir documentos, sua cobertura do pedido e pendências do NOTAZZ no procedimento inicial.

- [x] Separar visualmente nota emitida de retirada confirmada e limitar a conclusão a Supervisor/Gestor.

- [x] Guiar cancelamento, retorno à posição, devolução por nova entrada e avaria, mostrando consequências e serviços preservados.



**Conclusão:** operador sabe o que falta para retirar e o que um cancelamento desfaz; documento simbólico não é mostrado como expedição física. **Base:** AC01/AC02/AC08, V10/V11, V33/V34 e V39.



**Evidência D31 atual FE10:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE11 Serviços, armazenagem e fechamento



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE04, FE10, BE12, BE13.



- [x] Registrar serviços, quantidades e origem; disponibilizar liberação financeira ao gestor sem duplicar lançamento.

- [x] Exibir memória de diárias, posições equivalentes, vigência, mínimo, avaria e ajustes, distinguindo ocupação física de cobrança.

- [x] Conferir/aprovar o ciclo inteiro, reabrir quando permitido e apresentar demonstrativo para ESL e referência da NFS-e.



**Conclusão:** cada valor é explicável e a interface exibe cálculo do servidor; exemplo documental não aparece como contrato real confirmado. **Base:** AC04 a AC09, V12, V19/V20, V35/V36, V40 e V45 a V48.



**Evidência D31 atual FE11:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE12 Contagem, contingência e relatórios



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE08, FE11, BE14.



- [x] Apoiar contagem simples e eventual carga inicial, com diferença, justificativa e aprovação do ajuste.

- [x] Apoiar lançamento/conferência dos registros de contingência, identificando conciliados e conflitos sem criar sincronização automática sem rede.

- [x] Disponibilizar consultas e relatórios essenciais de estoque, ocupação, movimentos, valor, pedidos e cobrança com o mesmo alcance de acesso.



**Conclusão:** registros podem ser conferidos pelo supervisor/gestor; diferença ou falha não desaparece da tela nem altera saldo sem confirmação. **Base:** AC13 a AC15, V21, V37 e V44.



**Evidência D31 atual FE12:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados.

### FE13 Validação das jornadas e preparação do piloto



**Status:** Base local D31 entregue e revisada; integração real/ambiente e critérios indicados abaixo permanecem pendentes. **Dependências:** FE02 a FE12; BE15 para fechamento conjunto.



- [x] Verificar tipagem, comportamento, contratos e construção; validar teclado, legibilidade, tamanhos de tela e feedback de erro/sucesso.

- [ ] Executar percurso completo com API, impressora/coletor e dados de validação, registrando falhas de conexão e repetição.

- [x] Preparar artefato frontend, instruções para a operação e evidências da versão candidata, distinguindo simulação de teste real.



**Conclusão:** jornadas funcionam com o backend e nos dispositivos do piloto; versão construída e versão publicada possuem registros separados. **Base:** RN30, AC14 a AC16 e V01 a V48 conforme a jornada.



**Evidência D31 atual FE13:** [cobertura/limites](frontend/docs/cobertura-e-limites.md), [provas](frontend/docs/provas-jornadas-marco02.md), [recibo](orchestracao/.runtime/frontend-resultado-lucas-20261008.json). Checkboxes assinalados correspondem ao recorte local frontend; API/SQL/equipamento/fiscal reais continuam separados. Validação local/artefato/guia; integração/piloto pendentes.

## Entregas conjuntas e ordem de avanço



Esses marcos agrupam as duas trilhas, sem duplicar o controle de cada etapa. Todos estão planejados. Não é necessário concluir todo o backend antes de começar o frontend.



| Marco | Backend | Frontend | Resultado para a operação |

| --- | --- | --- | --- |

| M01 Base e cadastros | BE01 a BE05 | FE01 a FE04 | Acessar com perfil correto e cadastrar o contexto da operação |

| M02 Receber e localizar | BE06 a BE08 | FE05 a FE07 | Receber, conferir, identificar e endereçar uma unidade |

| M03 Consultar e expedir | BE09 a BE11; consultas iniciais BE14 | FE08 a FE10 | Consultar saldo, reservar, separar, documentar, retirar e tratar retorno |

| M04 Cobrar e conferir | BE12 a BE14 | FE11 e FE12 | Explicar serviços, armazenagem, ajustes, contagens e fechamento |

| M05 Validar o piloto | BE15 | FE13 | Conferir percurso com Caio, equipamentos, fiscal e recuperação |



BE14 pode disponibilizar consultas de estoque para FE08 depois de BE08, sem aguardar sua parte financeira. Essa entrega parcial deve ser registrada sem marcar BE14 inteiro como concluído. FE01 pode avançar junto com BE01; ambos fecham os contratos antes de concluir telas integradas.



## Dados e verificações de implantação



São dependências das etapas afetadas, não um bloqueio geral do projeto nem outra rodada das 110 perguntas.



| Item | Responsável de referência | Etapa afetada | Situação atual |

| --- | --- | --- | --- |

| Banco/esquema WMS, acesso e ambientes SQL Server | Lucas/TI | BE03, BE15 | D24 schemas; D26 identidade DEV/atestação/API reais. Ajuste demanda V10 DEV em escopo separado; recuperação pendente |

| Forma de autenticação, hospedagem e operação | Equipe técnica com Lucas | BE02, BE04, BE15, FE03/FE13 | A definir; não copiar configuração de outro projeto |

| Cliente, limites, embalagens, endereços e eventual estoque inicial | Caio/operação | BE05/BE06/BE14, FE04/FE12 | Preparar dados; razão social resolve grafia Brasel/Bracel |

| Preços, unidades de cobrança, mínimo, corte e eventual GRIS | Gestor/comercial | BE12/BE13, FE11 | Parâmetros reais a cadastrar; tabela Tigre é referência |

| Exemplos fiscais e procedimento NOTAZZ/ESL | Natalina/Controladoria | BE10/BE13, FE10/FE11 | Responsável identificado; fluxo real a validar |

| Coletor, Tanca, etiqueta e cobertura | Mickael e TI | FE06/FE07/FE13 | Tipos informados; modelos e teste real pendentes |

| Volumes e simultaneidade real | Caio/operação e equipe técnica | BE15/FE13 | Dez usuários/quatro simultâneos estimados; movimentações desconhecidas |

| Backups, restauração e reconciliação | Lucas/TI e Caio | BE15/FE12/FE13 | Até 36h de parada informadas; capacidade real a comprovar |

| Conferência de baixa e cálculo propostos | Caio e gestor | BE10/BE12/BE13, FE10/FE11 | Usar exemplos de AC01/AC04/AC05/AC08 antes do uso real |



Nenhum desses itens está registrado como teste concluído. Dados fictícios podem apoiar o desenvolvimento; não substituem configuração e validação do piloto.



## Fora da primeira entrega



- **Adiado por resposta:** transferência entre armazéns, conforme Q11.

- **Evolução proposta:** integração automática NOTAZZ/ESL, mapa visual elaborado e inventário completo. O piloto prevê procedimento fiscal intermediário, consultas de ocupação e contagem simples.

- Aplicativo que registra e sincroniza movimentos sem conexão não está definido; a contingência recebida é por planilha com conciliação.

- Expansão Castro/Tigre depende da validação inicial. A arquitetura comporta outros clientes/armazéns sem obrigar ativação simultânea.



## Registro das próximas atualizações



Ao iniciar ou concluir uma etapa, atualizar o cabeçalho dela e registrar: data; IDs BE/FE; escopo entregue; arquivos/artefatos; validação executada e resultado; limitação ou impedimento concreto; próximo passo. Registrar versão em execução somente quando observada. Referenciar evidências existentes por links, sem inventar comandos, relatórios ou arquivos futuros.



Manter próximos passos no resumo e status nas próprias etapas; conservar no histórico apenas o que explica o avanço. Decisões de negócio continuam no documento 06, sem redefinir fórmulas neste arquivo.



## Histórico de evolução



| Data | Registro | Evidência e limite |

| --- | --- | --- |

| 03/10/2026 | Estrutura local e documentação inicial preparadas | Pastas e documentos do projeto; sem implementação |

| 05/10/2026 | Questionário recebido e regras consolidadas pelo raciocínio | Documentos 10 e 11; 110 subitens respondidos, com interpretações/propostas identificadas |

| 05/10/2026 | Orientações e trilha organizadas a pedido do responsável | AGENTS.md e states.md; 15 etapas backend e 13 frontend, todas planejadas; referências e dependências verificadas |

| 05/10/2026 | Pedido D11 inicia o backend; escolhida e concluída BE02, com recorte técnico de BE01 | Spring MVC por camadas, Maven Wrapper, status/erros/identificação, bloqueio de acesso e perfis; 12 testes e execução do JAR local aprovados. Documentos 12/13. Sem SQL Server, frontend ou publicação |

| 05/10/2026 | D12 amplia o backend para cinco cadastros, persistência, acesso e auditoria | BE01/BE03/BE04/BE05 em andamento; V1 preparada; JWT/escopos, versões, encerramento e transações testados. Build final `verify`: 40 testes aprovados; documentos 14/15. Sem SQL Server real, provedor real, frontend ou publicação |

| 05/10/2026 | D13 e BE16 padronizam properties e engenharia | Configuração convertida, validação nos serviços, formatação/imports explícitos, ferramentas e arquitetura conferidas no build. `clean verify`: 52 testes aprovados; padrões no documento 16. IDs existentes preservados; nenhum ambiente real alterado |

| 05/10/2026 | D15 entrega o macrobloco de recebimento | Pedido/notas manual/XML, chegadas, quarentena, estorno e efetivação única; seis modelos e V2 preparada. `clean verify`: 86 testes aprovados, sendo 34 novos; documentos 18/19. BE06 em validação externa; próxima frente BE07. Sem SQL Server, emissão fiscal ou publicação |

| 05/10/2026 | D16 entrega unidades logísticas e dados de etiquetas | Unitização por entrada, UUID, origem, divisão/reagrupamento, repetição e auditoria; V3 preparada. `clean verify`: 122 testes aprovados, sendo 36 novos; documentos 20/21. BE07 em validação externa; próxima frente BE08. Sem SQL Server, frontend, impressão real ou publicação |

| 05/10/2026 | D17 entrega endereçamento e estoque | Capacidade, conjuntos, ocupação/movimentos atômicos, bloqueio/liberação e saldo/histórico; V4 preparada. `clean verify`: 153 testes aprovados, sendo 31 novos; documentos 22/23. BE08 em validação externa e BE14 parcial; próximo código BE09. Sem SQL Server, frontend, cobrança ou publicação |

| 05/10/2026 | D18 entrega pedido de saída, FIFO e reserva pela equipe WMS | Cedro backend/contrato, Prumo V5/procedimento, Vigia revisão favorável em leitura e Farol registros/mapa. `spotless:apply` e `clean verify` aprovados: 197 testes, 44 novos; documentos 24/25. BE09 em validação externa e BE01/BE14 parciais; próximo código proposto BE01/BE10 após nova demanda. Git zero commits, conteúdo preexistente preservado. Sem SQL Server real, frontend, fiscal/cobrança, publicação, rotinas, Hermes/ETL |

| 05/10/2026 | D19 autoriza execução contínua de todo o backend restante | Ordem, arquivos e aceite local no documento 26. Iniciadas BE10/BE11, sem antecipar testes; base D18/197 preservada. Autorização abrange os próximos blocos locais, não homologação ou ações externas proibidas |

| 06/10/2026 | D19 aceita localmente BE10/BE11 e segue BE05/BE12 | Após correções de Vigia e P2 temporal BE07, spotless e clean verify reais: 240/0/0/0; contrato27/evidência28, JAR e hashes conferidos. Vigia favorável; V6 compatível em arquivos, sem SQL real. Base197 e V1–V5 preservadas. BE10/BE11 em validação externa; schema BE05/BE12 e V7 seguem autonomamente, sem nova autorização |

| 06/10/2026 | D19 aceita recorte BE05/BE12 e libera BE13 | Após P2 financeiro BE08, spotless/clean verify292/0/0/0,14 XMLs/JAR/222 hashes conferidos. Vigia favorável02:33; V7/JPA518/518 e67/67 suplementares compatíveis, arquivos somente. Contratos29/31, evidências30/32. Cedro refina31 antes do Java/V8 e segue BE13/BE14; inativação BE05 após esses fatos. Não conclui SQL Server/comercial/fiscal/equipamentos externos |



Padrões de organização consultados: `satelite-tms-api`, `dashboards-etl` e `avaliacao-desempenho-competencias`, especialmente separação entre orientação e estado, IDs de tarefas, evidência e próximo passo. Regras específicas de bancos, portas, bibliotecas e operação desses projetos não foram incorporadas ao WMS.



<!-- mcp-setup-evidence:start -->

## Integração de ferramentas MCP — 05/10/2026



- ai-memory acessível pelo MCP no escopo exclusivo workspace=rodogarcia, project=wms-rodogarcia; declaração em .ai-memory.toml. Históricos existentes preservados e espaços novos registrados vazios pelo mecanismo nativo, sem sessões ou páginas artificiais.

- Graphify consultável pelo MCP com project_path deste repositório: 1629 nós e 5861 relações na conferência. JSON, relatório e HTML presentes; verificação do grafo exportado encontrou zero relações com endpoint ausente. Consultas reais de saúde e conteúdo executadas pelos dois MCPs.

- Instruções oficiais e sete skills locais preparados, com escopo explícito em AGENTS.md; hooks Git de commit e troca de branch instalados. A captura de sessões usa os hooks globais já instalados no Codex.

- Preparador comum e guia em ../.runtime/mcp-tools/README.md; integração automática para novos repositórios Git diretamente em projetos, com processo próprio no login do usuário. Recibos e logs ficam na pasta da ferramenta. Não modifica runtime das aplicações, banco ou equipe Maestri.

- Limites: Conteúdo de 42 documentos/PDFs incluído; imagens incorporadas do Word não reavaliadas. Diagnóstico bruto: 778 referências sem nó definido e 285 relações paralelas agrupadas; grafo exportado com zero endpoints ausentes. Auditoria original preservada em graphify-out/extraction-audit.json e integrity-audit.json. Tokens dos agentes não medidos.



Este registro trata somente das ferramentas dos agentes. Critérios e pendências de implementação deste projeto permanecem nas seções próprias acima.

<!-- mcp-setup-evidence:end -->





**Fecho D19 em06/10/2026:** escopo local BE01–BE16 concluído após P2-locks, 367/0/0/0, revisão Vigia favorável e V9/JPA conferidos. Graphify atualizado por Farol, registros centrais/índice/modelo/matriz sincronizados e alterações preexistentes preservadas. Nenhuma ação externa proibida realizada; próxima etapa é definição/autorização dos ensaios externos pelo33/34, sem backend independente faltante identificado na auditoria/revisão.





**Preparo técnico final aceito após P2 de identidade:** Vigia favorável às08:34, histórico360 e atual367 separados; fixtures18/18 com12 recusas, diagnóstico306/306, fecho77/77 e488 hashes conferidos. Backend289 e V1–V9 intactos. Evidência32 e preparo33; sem execução SQL/JPA/JVM ou novo build por essa correção documental.
