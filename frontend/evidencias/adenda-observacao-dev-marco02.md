# Observação DEV própria — MARCO02

O processo fictício de Lume foi iniciado em `http://127.0.0.1:5188`, PID 52792, no snapshot R32. Permanece aberto. Não houve reinício ou encerramento do processo preexistente 5178 nem acesso à API/backend/SQL.

O observador R32 terminou com exit 1 porque a navegação entre âncoras retorna `null` em `page.goto`, sem uma nova resposta HTTP. O [log original](snapshot-marco02-r32/frontend/evidencias/exercicio-observacao.log) permanece preservado.

R33 corrigiu a recarga, mas sua captura desktop revelou uma referência de página incorreta (`recebimento` em vez da rota real `entrada`). Seu registro de CSS não comprova a página de recebimento; JSON e capturas históricos foram preservados. R34 usa a rota real e exige o título da página antes de registrar o estado. A inspeção visual confirmou Entrada e conferência em 1440px e Coletor em 390px. A observação executada cobre ambas as páginas em 1440/768/390px, HTTP 200, CSS externo aplicado e nenhum erro CSP/console, overflow de documento ou solicitação API/externa.

Os checks integrados R32/R33 permanecem históricos. A fonte final é R34, com seu próprio ledger, logs e resultados; a execução corrigida do observador consta em [observação R34](snapshot-marco02-r34/frontend/evidencias/marco02-exercicio-observado.json).

O [recibo do processo](frontend-lume-exercicio-marco02.json) confronta todos os 177 arquivos de fonte R34 com os bytes servidos pelo processo R32: 176 idênticos, uma diferença exclusivamente no observador de evidência. Aplicação, contratos, estilos e configuração em execução não divergem da fonte final. Não foi necessário reiniciar o servidor para corrigir o observador.

Comandos reais: `node tools/start-exercise.mjs` no R32 e `node tools/observe-exercise.mjs` no R34; cwd, horários, PID, URL, hashes e requisições estão nos recibos correspondentes. Os checks finais são executados no snapshot imutável R34. Esse exercício é fictício e não comprova integração/piloto.
