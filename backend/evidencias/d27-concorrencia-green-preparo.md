# D27 — preparo da única automação green

Estado: preparado; aguarda coordenação Farol e parecer Vigia do predicado748. **Nenhum aguardador/JVM/SQL novo iniciado pelo preparo.** Metadados e hashes em [JSON](d27-concorrencia-green-preparo.json).

Fonte imutável [D27CAC92281](d27-D27CAC92281-http.json): cliente27/armazém13/SKU14, pedidos **28/29** (IDs reais da fonte; a menção27/28 não será usada para substituir IDs). GETs atuais obrigatórios antes do ticket/gate: ambos RASCUNHO, físico100/reservado0. Recusar sem mutação se divergente. Nenhuma nova fixture ou reversão de25/26.

JAR420 atual4304 e observador748 com fixtures9/0/24/0. Somente WMSDEV; JWT RSA/issuer/JWKS HTTPS efêmeros com alcance da família, mesmas guardas TLS/alvo/identidade. Antes da JVM, WaitTicket atual com PID vivo/prazo>=90s; depois, readiness da própria rodada e testemunha de duas requests reais. Guarda de exclusão mantém mutex durante espera **e filho inteiro**, evitando sobreposição constatada nas tentativas anteriores; [fixture3/0](d27-aguardador-exclusao-fixtures.json), sem credencial/SQL/JVM.

Comando a partir de backend, **somente após coordenação**:

```powershell
./ensaios/d27/aguardar-concorrencia.ps1 -ExecutarSqlD27 -BaseRodada D27CAC92281 -EsperaSegundos 60
```

BaseRodada é obrigatória; não há default de fixture concluída. O sinal público conterá PID real/início/prazo/base/hash. Executar uma vez; nenhum retry automático ou outra janela em paralelo. O preparo não altera o antigo flag e não é autorização para reabrir espera sem coordenação.
