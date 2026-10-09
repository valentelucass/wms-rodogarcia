# D27 — pedir nova espera bounded antes de executar

A espera Prumo PID56580 expirou01:30:53UTC; nenhuma nova JVM/gate iniciou depois dela. Cedro não inicia uma janela sem observador já aguardando ticket com prazo suficiente.

Helper atual corrigido: bytes UTF8 (com/sem BOM), 10/0 fixtures, flags/request-id/cookie/segredo concorrentes, registro conjunto sincronizado, status200/409 + estado integral, SET/identidade do gate, leitura sanitizada de cada readiness. Retomada implementada por `-Etapa concorrencia -BaseRodada D27C2F6B9BB`, sem estoque/pedidos novos: cliente25/armazém12/produto13/pedidos25/26 ainda RASCUNHO/físico100/reservado0, GETs prévios comprovam intactos.

Antes de armar próxima espera, o observador precisa aceitar o contrato de duas fontes já proposto em d27-readiness-repro.md: `fixtureRodada`/`fonteCriacao` apontam a criação original, `rodada`/GETs/`appPid` atuais comprovam a retomada. Não copiar POSTs antigos para cases atuais nem inventar SPIDs. Código cliente SQL mantém prefixo da fixture original, e timestamp de criação original não tem que ser novo; pedido/readiness/expiração/PID são novos e estritos. Fonte observador lida ainda busca criação201 na rodada atual, portanto a retomada ainda não satisfaz essa guarda.

Solicita-se somente ajuste de parser/contrato/fixtures desse caso, sem relaxar identidade/SQL/locks. Depois publicar `d27-prumo-observador-aguardando-ticket.json` com PID real, prontoParaTicket=true e prazo120s novo. Cedro inicia imediatamente a JVM final quando fonte/fixture estiverem conferidas, com no mínimo90s disponíveis para startup/GET/publicação; gate só após readiness SQL real da rodada. Um Observe único após ticket novo válido; sem retry automático ou espera de senha.

Cedro inicia agora aguardar-concorrencia.ps1: espera unica60s, nenhuma credencial/SQL/JVM durante espera, detecta nova WaitTicket viva com >=90s e hash do observador igual fixture24/0. Contrato retomada já conferido. Prumo pode iniciar NOVA espera120s imediatamente; ao sinal o helper dispara sozinho nesta unica tentativa, sem novo ask. Se nenhum sinal em60s, encerra semJVM e informa por terminal.
Checkpoint 01:49 UTC: helper/retomada e fixtures24/0 estáveis, aguardador bounded60s será iniciado agora. Prumo pode armar nova WaitTicket120s; somente disparo com PID vivo e>=90s. Nenhuma JVM/SQL enquanto não há sinal atual.

02:00 UTC: recheckPrumo24/0 hashE489... agora igual fonte; compat-armado7/0 lido. Cedro pronto para espera bounded60s, sem JVM/segredo antes flag vivo com>=90s. Nova WaitTicket120s solicitada por arquivo; nenhuma reativação de janela59156. Helper retomará somentefixture25/pedidos25/26. BuildVIG04 em paralelo, guardas/concorrência código inalterados.

02:03 UTC: aguardador atualizado explicitamente paraJAR420 target-d27-vig04-final/C05B7517...377A conformeFarol; observadorhashE489/fixtures24_0 atuais. Inicia agora esperaunica60s, nenhumaJVM/segredoSQL atéWaitTicket120s ativa. Prumo pode armar imediato; janela velha continua recusada. Não reexecutaIT nem recriafixture25/pedidos25/26.

02:08 UTC: teste/readersourceE489_24/0 coerentes, três variantes concluídas; protocolo still semflagativo. Aguarda próximaWaitTicket após sinald27-concorrencia-sinal-cedro.json atual. Aguardador aponta420, conformeFarol; próximos60s ativos agora. Nenhum retrySQL/JVM semreadinessnova.

02:12 UTC: coordenacaoPrumo02:04 comprovou que60s terminavam antes guarda final doPID. Corrigida somente janela do aguardador previo (agora bounded180s, processo em background sem credencial/JVM/SQL). PublicaPID/inicio/expiracao em d27-cedro-aguardador-ativo.json; Prumo pode verificar e armarWaitTicket120s uma unica vez. Mantem>=90s/PID/hash24_0/alvo/fase antesgate; nao ampliou15s de gate nempermissoes. Aguardador180 ativoagora, JAR420C05B.

02:15UTC FINAL: buildnumerico420SHA4304D428...E8900CE1 pronto; HTTPD2755FEBD14 completo16HTTP/6assertivas, todos limites400+positivo/replay. Agoraaguardadorativo60s publicaráPID/prazo e finalJAR. FarolacionaPrumo pela proxima ferramenta conforme protocolo; nenhuma janela antecipada, semcallbacks Cedro. NovaWaitTicket120s exige>=90s quandohelperdetecta; retomaC2/pedidos25/26, semnovosdados/IT/build.

02:18 UTC: primeira espera posHTTPfinal50960 expirou60s semnovaWaitTicket, nenhumaJVM/SQL. Preservado sinal comfim. Continua mesma entrega; novo aguardador60s sera publicado agora comPID novo antes de qualquergate. Farol pode usarproxima ferramenta conformeprotocolo. FINAL4304...0CE1, semnovosdados/rebuild. Frentesindependentes: atualiza geradorlimiteshistoricos e recortesSQL106/0 eSQLXML.
