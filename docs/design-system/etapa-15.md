# FE05-ACEITE01 — aceite somente quando houver divergência

Pedido de Lucas em 13/10/2026: Efetivar carga integral solicitava resposta para Aceitar divergências mesmo quando a conferência não apresentava diferença ou avaria.

## Comportamento

O formulário usa o resultado `divergente` da consulta atual, vinculado ao mesmo pedido e à mesma revisão. Com `divergente=false`, oculta o campo, apresenta a orientação de conferência sem divergência e envia `aceitarDivergencias=false` automaticamente. A confirmação também não apresenta uma escolha desnecessária. Motivo, revisão e confirmação da carga continuam obrigatórios.

Com divergência, avaria ou conferência não comprovada/atual, mantém a escolha explícita. A opção vazia se chama Selecione; nunca preenche Sim automaticamente. Resultado de outro pedido ou de revisão antiga não dispensa o campo. Avaria exige tratativa mesmo se o total recebido coincidir com o previsto. O cenário fictício correspondente foi alinhado a esse comportamento.

A API continua recebendo Boolean obrigatório, sem alteração de DTO, service ou schema. O backend já exige Supervisor/Gestor, bloqueia o pedido, confere revisão e recalcula a divergência ao efetivar. Se a conferência mudar após a consulta, um envio com false permanece sujeito à recusa DIVERGENCIA_PENDENTE; não há repetição ou aceite automático. O frontend apresenta o resultado autorizado e não decide a liberação.

Orientações centralizadas em `frontend/src/content/information.ts`. O vínculo de conferência é metadado de apresentação; não é enviado no corpo da operação.

## Validação

Seis testes novos cobrem ausência de escolha e envio false, exigência de escolha com divergência, pedido diferente, revisão antiga, falta de consulta e recusa do servidor após mudança. Chrome em exercício fictício explícito confirmou ambos os fluxos, inclusive avaria com diferença zero; celular 390 px e tema escuro sem overflow/erros JavaScript. Nenhum SQL/backend real ou produção foi usado nesses testes.

Regressão completa: **625 testes em 62 arquivos, sem falhas**. Tipagem, lint, build final e diff check aprovados. Recibo `orchestracao/.runtime/receiving-finalization-20261013/resultado.json`, com logs/capturas na mesma pasta; não somar fases. Fonte nova exige atualização pelo PM2 principal em terminal administrador; esta mudança não reinicia nem escreve em DEV/PROD. Procedimento em [produção no PM2](../45-producao-pm2.md).
