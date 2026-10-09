# D26 — confronto temporal da fotografia financeira final

Fotografia Prumo [222/4](../../orchestracao/.runtime/d26-prumo-complementos-20261006T235833433.json) preservada. As quatro divergências usam expected anterior à transição HTTP, não evidenciam sozinhas defeito do backend. Nenhum novo teste/SQL/DML/patch principal executado para este confronto.

| Comparação | Expected antigo / SQL final | Fonte atual e transição |
| --- | --- | --- |
| Fechamento3 versao | 0 / 1 | D26724AF37C: GET `/api/v1/fechamentos-cobranca/3` após ajuste traz id3/versao1/versaoAtual2/EM_REVISAO; ids.financeiroComplemento.destino é DTO Resumo direto, não wrapper fechamento. |
| Fechamento3 versaoAtual | 1 / 2 | Mesmo GET final; POST ajuste1 e replay não duplicam ajuste. O serviço cria nova versão do destino, atualiza versaoAtual e incrementa revisão. |
| Versão financeira2 situação | REJEITADA / SUPERADA | Versão1 do fechamento2 foi rejeitada; a reabertura posterior cria versão2/id3 e marca a anterior SUPERADA. Não comparar o DTO capturado na rejeição com o estado depois da reabertura. |
| Versão financeira4 situação | PENDENTE_REVISAO / SUPERADA | Versão1/id4 do fechamento3 foi superada pelo ajuste, que criou versão2 do destino. O DTO da preparação precede o ajuste. |

Fontes: [HTTP completo D26724AF37C](d26-D26724AF37C-http.json), caso `complemento GET /api/v1/fechamentos-cobranca/3`; [FechamentoCobrancaService](../src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java), reabertura e ajuste (antiga.situacao SUPERADA; novaVersao; destino.atualizar). Situação da versão é estado de fluxo; memória/hash do demonstrativo devem permanecer preservados. A comparação de memória não deve ignorar divergência de conteúdo para resolver um expected temporal errado.

Prumo possui o leitor: ajustar somente a escolha dos estados finais segundo GET/transições, preservar222/4 e publicar nova fotografia separada se confirmado. Cedro não altera script de Prumo nem dados SQL. Escritas já encerradas; GET cálculo4/alcance/replay completos em D261D771A8E/D26777E3FA5, auditoria única com tipo canônico em D2696D1ADDC. Todas31 JVM e portas de ensaio encerradas/conferidas no d26-processos-final.json.

Confirmado depois em [fotografia236/0 separada](../../orchestracao/.runtime/d26-prumo-complementos-20261007T000051578.json),16 rodadas/cinco contextos, somente SELECT DEV/WMSDEV. Nenhum caso passou alterando dados; o comparador foi reconciliado com os estados finais e acrescentou comparações.222/4 permanece histórico. Catálogo/histórico preservados no atestado final000142928. Não é witness SQL simultâneo nem aprovação do ajuste E052.
