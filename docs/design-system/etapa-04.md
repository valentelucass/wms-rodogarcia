# BE14-DASH01 → FE02-DASH01 — gráficos do Início

**Composição histórica DASH01:** CTX01/DASH02 substituiu inicialmente a apresentação por dez indicadores e mapa. Em 10/10, os gráficos foram reincluídos com apoio à opção Todos no incremento BE14-DASH01-A01 → FE02-DASH02-A03. Este documento preserva a prova original; ver [composição e contrato atuais](etapa-05.md#gráficos-no-início-e-opção-todos--be14-dash01-a01--fe02-dash02-a03).

Lucas autorizou a aplicação em 09/10/2026, após pedir gráficos abaixo do Acesso rápido, aderência ao design system e uma preparação proporcional do backend em `backend/`. Este incremento entrega ocupação física, disponibilidade por SKU e fila de saída. Séries históricas de entrada/saída e indicadores financeiros permanecem propostas para outro incremento.

## Composição visual

`OperationDashboard` aparece depois de `HomeOverview` no modo real e no exercício fictício. Quatro indicadores resumem posições ocupadas do cliente, unidades logísticas disponíveis, pedidos abertos e avisos de validade. Dois cards apresentam barras por área e por situação do pedido; um card ocupa a largura seguinte para a composição do estoque por SKU, com seis produtos por página.

São usados Inter, tokens de tipografia/espaçamento, bordas finas, raio de card de 12 px e os temas claro/escuro já existentes. As séries de gráficos seguem a seção 6.6 de [design.md](design.md): azul para barras gerais, verde para disponível, laranja para reservado, cinza para demais indisponíveis e roxo para entrada sem unitização. Os tokens de série são a adaptação P deste incremento. Valores e legendas permanecem visíveis; a informação não depende somente da cor ou de passar o mouse.

Os gráficos são SVG nativo com atributos numéricos e CSS externo; nenhuma biblioteca foi acrescentada nem a CSP foi ampliada. No desktop, quatro métricas e dois gráficos por linha; no mobile, os gráficos empilham e as métricas se ajustam à largura. A paginação reutiliza `Pagination`; após uma troca de página concluída, o foco segue para o título dos produtos. Os atalhos para estoque e saída usam a navegação existente.

## Contrato e leitura

`GET /api/v1/dashboard` recebe `clienteId`, `armazemId`, `fuso`, `pagina` e `tamanho` (padrão 6, máximo 12). O controller delega ao serviço, que valida parâmetros e alcance do usuário antes das consultas. O repository usa agregações e consultas por IDs da página, sem carregar todas as unidades. A leitura transacional mantém o isolamento usado pelos indicadores de estoque existentes; não altera regras, schema ou dados de negócio.

| Medida | Significado apresentado |
| --- | --- |
| Posições ocupadas | Ocupações físicas do cliente no armazém selecionado, por tipo de endereço; uma unidade pode ocupar mais de uma posição |
| Capacidade e posições livres | Endereços ativos do armazém inteiro, apresentados somente ao Gestor e identificados como contexto do armazém |
| Unidades disponíveis | Contagem com o mesmo predicado de elegibilidade do estoque existente |
| Fila de saída | Pedidos em elaboração, reservados, em separação e aguardando retirada; situações finais ficam fora do total aberto |
| Disponibilidade por SKU | Quantidade física separada em disponível, reservado, demais indisponíveis e entrada conferida ainda sem unitização |
| Avisos de validade | Unidades ativas vencidas ou até a antecedência configurada, incluindo a data limite no fuso informado; configuração ausente produz `null`, apresentado como “—” |

Posições físicas não representam ocupação cobrável. Quantidades de produtos diferentes não são somadas; cada SKU conserva sua unidade de medida. Uma reserva parcial bloqueia a disponibilidade da unidade inteira: a quantidade reservada aparece no segmento próprio, e o remanescente permanece nos demais indisponíveis. Estes segmentos são disjuntos; quarentena, avaria e reserva não são somadas como categorias sobrepostas. As barras de área usam escala explícita de posições do cliente, sem representar percentual global do armazém. O gráfico de produtos representa a composição de cada SKU, sem comparar KG e UN.

Totais de posições, unidades e pedidos são agregados do contexto completo, independentes da página de produtos. O frontend preserva os lexemas de Long/BigDecimal nos rótulos; somente a geometria das barras usa proporções numéricas.

## Estados e atualização

Sem cliente/armazém válido, o painel solicita seleção e não consulta a API. A carga inicial tem estado de espera; resultados vazios são explícitos; erro de transporte ou permissão não vira estoque zero. Ao atualizar o mesmo contexto, o resultado anterior permanece identificado enquanto a consulta termina. Ao mudar contexto/perfil, os dados anteriores desaparecem, a requisição é cancelada e respostas atrasadas são descartadas. Um erro remove o resultado anterior.

A atualização ocorre ao entrar, trocar contexto, paginar ou acionar Atualizar indicadores; não há polling. O modo real consulta o backend pelo transporte autenticado existente. O exercício tem uma fixture explicitamente fictícia e não fornece fallback para a API real.

## Conferência e limites

Backend: cinco cenários HTTP com H2 efêmero isolado para conciliação/leitura sem escrita, reserva parcial, parâmetros/permissões/página vazia, isolamento de clientes/armazéns e limite civil da validade. Cinco verificações de arquitetura também passaram no `verify`, com artefato separado em `backend/target-dashboard`. A guarda do teste exige a URL H2 própria antes de limpar suas tabelas.

Frontend: tipagem, lint, 432 testes da suíte, testes focais do dashboard e builds real/fictício em diretórios próprios. Três casos Chrome no exercício fictício, com tráfego API bloqueado, cobrem temas, larguras de 320/375/768/1024/1440 px, teclado, atualização lenta, troca de contexto, vazio, erro e CSP. Um caso na aplicação real com respostas interceptadas confere Bearer, seleção prévia de contexto, Long exato e descarte após 403; também não acessa API/banco reais. Capturas e resultado estão na [prova visual](../../frontend/evidencias/dashboard-dash01-browser-resultados.json) e na [prova do transporte nativo](../../frontend/evidencias/dashboard-dash01-real-browser-resultados.json); o [recibo](../../orchestracao/.runtime/dashboard-dash01-resultado.json) delimita a versão conferida.

Esta validação não comprova dialeto/plano de execução/desempenho em SQL Server nem a versão do backend já em execução. Nenhuma migration, conexão SQL Server, mudança de permissões no banco, publicação ou reinício de processos existentes foi executado nesta entrega. A consulta real requer carregar o backend atualizado pelo fluxo DEV autorizado. `graphify update .` recusou substituir `graph.json` por uma extração menor (12.834 contra 15.220 nós); não foi usado `--force`. O log está no recibo, e a manutenção do mapa conserva sua frente própria.
