# Etapa 13 — data da atualização de produção no rodapé

FOOTER-PROD01, pedido expresso de Lucas em 13/10/2026, incluindo publicação em produção.

O rodapé compartilhado das telas e do login mostra, ao lado dos direitos reservados, `Atualizado em DD/MM/AAAA, HH:mm:ss (Brasília)`. Usa os tokens existentes, texto de apoio, elemento `time` e quebra de linha em telas menores. DEV e builds sem identificação de produção não mostram a informação.

O horário vem de `activatedUtc` no recibo do launcher, gravado somente após readiness do backend, frontend e relay. Preparar/compilar não comprova ativação e não produz essa data. O servidor fornece apenas `runId` e `activatedUtc` em `/__wms/release`, sem cache, após conferir sucesso, versão, PID e hash do manifesto servido. Recibo ausente, inválido ou de outra versão retorna indisponibilidade, sem inventar horário.

O pacote inclui seu próprio `runId`. Ao abrir, voltar à aba ou a cada minuto, consulta a ativação; se outra versão estiver ativa, informa `Nova versão disponível. Atualize a página.`. Não atribui a uma aba antiga o horário da versão nova nem recarrega silenciosamente um formulário em edição.

Validação local: 588 testes frontend, 16 testes do servidor de produção, 24 testes do launcher, tipagem, lint dos arquivos alterados e build de produção aprovados. Ensaio visual do pacote preparado em desktop/mobile 390 px, com API interceptada, separado da conferência pública pós-publicação. Corrigido o harness de falha de build para usar `-PrepareOnly`, pois uma produção ativa ocupava as portas e impedia alcançar a falha sintética pretendida. Sem testes de banco, migrations ou fixtures em produção.

Evidências e publicação: `orchestracao/.runtime/footer-prod-20261013/resultado.json`. O horário exibido comprova a ativação local daquela versão; disponibilidade HTTPS é conferida separadamente. Manter a sessão do launcher aberta.

Publicado no run `e2165b2e2e3a4f8ca7c84aab687d3bef`, backend 64800/25590 e frontend 66528/25591. Ativação às `13/10/2026, 19:10:08 (Brasília)`, igual ao recibo UTC `2026-10-13T22:10:08.4190579Z`. Conferência pública HTTPS sem interceptação: frontend/metadados/relay HTTP 200, versão e data confirmadas, Chrome desktop/mobile claro/escuro sem overflow ou erros JavaScript. Login autenticado não repetido. DEV observado no início/fim preservado (5904/35416). Guarda PROD 26/26; nenhuma operação de negócio em produção.
