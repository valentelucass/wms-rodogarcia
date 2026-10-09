# D29 — ligação dos cálculos locais antes da execução

`D29FinanceiroOraculosTest` usa o serviço público CalculoCobrancaService do artefato atual, preços/fatos/modelos isolados em memória e repositórios simulados. Não inicia SQL/HTTP/listener, não altera Clock e não grava qualquer história DEV. A prova financeira real continuará separada.

Casos ligados: Vigia F18, F19, F21–24, F28–29, F31–32 e conservação de F37; Lume F19 adicional (avaria20/100 às12h, tarifa10, pico do dia10, dia seguinte8). Esperados publicados no plano antes de execução. Datas locais 2026-09 são sintéticas e exclusivas desses cálculos.

F37 contém uma exigência adicional ainda não estabelecida no documento29, linha43: o oráculo atribui resíduo ao menorID (11:0,01;12:0), enquanto o contrato exige resíduo determinístico pela ordem deID e soma exata, sem definir qual ponta recebe o centavo. A implementação atual trunca parcelas anteriores e atribui o saldo à última. Este primeiro caso prova somente total único e soma das parcelas; a distribuição exata porID permanece não atendida frente ao oráculo, com conflito explícito para Vigia/Farol. Nenhum preço ou regra comercial foi alterado.

F31/F32 tratam cálculo de períodos reais da biblioteca de domínio, sem aprovar ciclos DEV. Métodos/HTTP200 e aritmética externa isolada não substituem execução do motor.
