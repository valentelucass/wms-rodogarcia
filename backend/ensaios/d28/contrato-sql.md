# D28 — contrato Cedro para leituras novas de Prumo

Cada `backend/evidencias/d28-select-D28XXXXXXXX.json` publica rodada, fase, IDs próprios e arquivo HTTP. O arquivo HTTP registra esperado/obtido, RequestId, perfil, payload fictício, resposta, assertivas e SHA/PID/portas do único artefato D28. Não consultar fixtures D26/D27 para somar casos D28.

O runner confirma DB_NAME=WMS_DEV, ORIGINAL_LOGIN=USER_NAME=WMSDEV, alvo loopback/servidor, privilégios enumerados e história V1–V10 antes da API e de cada etapa. Negócio usa somente HTTP. O preflight de Prumo complementa a atestação. Nenhum IT vazio ou migration é executado.

Quando a fase é `aguardando-select-prumo`, HTTP da rodada terminou e a API permanece viva para SELECT novo. As primeiras rodadas usaram prazo de 15 minutos; a partir do helper H5 a espera supervisionada usa até 120 minutos, no processo em background, com atualizações ao responsável. Não reexecuta HTTP nem aumenta prazo de request. A versão H7 registra `helperVersao` e `holdPrazoMinutos` nos IDs; versões anteriores são identificadas por rodada na evidência de processos. Prumo publica o sinal `orchestracao/.runtime/d28-prumo-select-liberar-D28XXXXXXXX.json` somente após capturar leituras da rodada. A primeira rodada D287D721FEA iniciou com o nome anterior sem `prumo`; Farol espelhou seu sinal somente após SELECT181/0 e focal43/0, preservando o original de Prumo.

```json
{"rodada":"D28XXXXXXXX","DB_NAME":"WMS_DEV","login":"WMSDEV","leiturasConcluidas":true,"arquivoEvidencia":"orchestracao/.runtime/d28-prumo-...json"}
```

O sinal libera o processo; aprovação das assertivas SQL é separada desse sinal. Divergência SQL não vira sucesso por encerrar a leitura. O encerramento e portas livres ficam nos IDs do JSON HTTP final.

| Etapa | Conferência SQL nova necessária |
| --- | --- |
| xml-ajuste / xml-limites | RN22 com hash/origem/soma5 e snapshot/auditoria únicos; recusas sem pedido/movimento/reserva. Contagens/revisões iniciais1, 50→45/50→0/45→35; zero inativo, posição liberada, linha/localização/marcos preservados; fatos delta−65 únicos e sem efeitos das recusas/reserva/replay. |
| carga-temporal | Carga inicial revisão1, preparação/revisão/regularização20 única, cancelamento3; fatos e origens preservados. Cálculo temporal PENDENTE/totalNULL com HISTORICO_QUANTIDADE_INSUFICIENTE, replay/GET/auditoria/snapshot únicos; divisão/reagrupamento reais identificados. Financeiro mínimo50 aprovado, reabertura/v2, memória v1 intacta; corte real anterior, nenhuma história fabricada. |
| listas / listas-estoque | Contexto/IDs/páginas/total e recusas de perfil/alcance. |
| variantes | FIFO/origens/lot​es/quarentena/avaria, mistura SKU recusada, disponibilidade/reserva preservadas; tabela específica/vigência, GRIS/mínimo/memórias distintas, previsões abertas separadas do fechamento50. |
| concorrencia | Fixture nova, duas solicitações integral80 para físico100; par200/409, vencedor/reservas80 únicos, perdedor sem efeito, replay sem duplicata e saldo físico100/reservado80/bloqueado20/disponível0. Witness online exige capacidade administrativa autorizada e readiness D28 antes do gate; falta de capacidade deve ser documentada, sem grant/sa na API. |

Cada SELECT precisa publicar rodada, nome real, login, instante, IDs, expected/actual/assertivas e limites. Farol coordena os especialistas; Cedro não altera leitores nem registros centrais. Preços fictícios e propostas AC não representam homologação comercial.

Complementos finais: `D282DDA1365` contém somente GETs, inclusive pedidos de saída 46/47 depois dos replays e entrada 27, com DTO na raiz para saída e `pedido` para entrada. O par da origem `D28EFFF454F` não foi repetido.

R16: `D289A0CF1ED` conserva o red do oráculo que esperou saldo físico após a recusa 409 `DIVERGENCIA_PENDENTE`. A entrada 28 estava em QUARENTENA, prevista5/recebida4; XML documental não criou saldo. `D284B2EA65C` retomou apenas essa fase pendente: GET confirmou ID/referência/estado/quantidades, Supervisor registrou solução fictícia AC03 no motivo e aceitou explicitamente a falta1 por HTTP, efetivando somente4. GETs mostram EFETIVADO, saldo104/pendenteUnitizacao4/fisicoUnitizado100/disponivel100. SELECT novo deve confirmar entrada_conferida4, sem unidades dessa entrada, motivo/origem preservados, nenhuma quantidade faltante fabricada. Ambas as JVMs H8 aguardam o prefixo próprio de Prumo.

Hash de fontes e classes de cada H8 está em `ids.helperFontesCompilados`; cópias são guardadas por hash em `target-d28-helper/versionados/`. H7 foi capturado antes de editar H8. H1–H6 têm classificação por rodada e fontes iniciais de derivação, mas não tiveram todos os compilados históricos arquivados: esse limite permanece explícito. A compilação final não inicia jornadas.
