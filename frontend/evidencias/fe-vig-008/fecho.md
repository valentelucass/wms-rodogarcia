# FE-VIG-008 — delta e novo congelamento integrado

Em D31, a única alteração de aplicação é `src/modules/financeiro/ExampleBilling.ts`: o ramo fictício `rejeitar` passa a devolver fechamento REJEITADO e versão REJEITADA. O backend permanece responsável pela decisão real. Nenhuma mudança de contratos, autorização, cliente, regras ou cálculo; nenhum backend/SQL/HTTPDEV executado.

Antes de editar: MARCO02 DD440A e manifesto96e50 preservados em `marco02-preservado`, com 180 cópias (177 fontes, manifesto, JSON e MD). As 7.782 referências foram reconferidas sem divergência. Artefatos/checks/input copies/snapshots históricos permanecem próprios. Seis originais do achado Vigia foram copiados em `original/`, sem editar a origem.

O teste Vigia foi reutilizado literalmente, SHA256 `28fd05a8c0c945a3fb6db467131bf3e11aaae96baf4038ad158bbb3497799114`, zero adaptações. Reprodução própria no snapshot R35: um teste RED com três divergências, preservado. R36: dez casos verdes — os oito financeiros existentes mais o literal e a prova própria de rejeição1→consulta→reabertura→rejeição2→consultas históricas. IDs901/1001/1002, número1/2, hashes a/e e revisão recebida permaneceram corretos. [Delta focal completo](delta-r36.json).

A fonte integrada final R37 é idêntica à focal R36: 179 arquivos, SHA256 `c58b64105a7b3232f314d56f8150ccbd6ce1530b6fe4d33837ba46ad3dd4f922`. Tipagem/lint/build exit0, 299 testes locais e16 Chromium verdes, sem falhas/ignorados/flaky. Ledger/comandos/cwd/horários/logs em `../snapshot-marco03-r37/frontend/evidencias/marco03-r37-checks-final.json`. Não são novas provas backend nem161E2E.

O processo corrigido é **http://127.0.0.1:5189**, PID19856, WMS - Lume, snapshot R37. O lançador `servir-snapshot-corrigido.mjs` exige modo fictício, hash corrigido cd217 e proxy ausente antes de abrir porta própria estrita, usando a mesma `vite.dev.config.ts`. O comando CLI direto foi recusado pela revisão automática, “bloqueada por política”, sem motivo adicional e sem criar processo. A alternativa restrita foi executada; argv/PID/cwd real foi observado no processo próprio. 5178 e5188 não sofreram kill/restart ou intervenção.

**5188/R32 continua sendo MARCO02 histórico DD440A, com a rejeição antiga.** Não é apresentado como corrigido. Seu vínculo antigo de176/177 arquivos servia à fonte anterior; a mudança de aplicação FE008 exige o vínculo novo179/179 do R37/5189.

A prova atual de navegador v03 percorreu Gestor/contexto→consulta de cálculos→seleção801→fechamento901→versão1001→Rejeitar ciclo integral com motivo→confirmação→resultado→consulta atual. Assertivas de payload id901/número1/revisão0, estados REJEITADO/REJEITADA, ID/hash preservados e consulta com mesma revisão. Duas capturas e trace integral de requests/receipts, sem descoberta ou cópia manual de ID de DTO bruto. Captura da consulta inspecionada. ZeroAPI/externos/console/CSP, CSS externo existente aplicado. Esta prova é adicional às16 integradas, sem repetir a suíte CSS de Prumo.

Ensaios do observador v01/v02 são preservados: v01 entrou na etapa Fatos em vez de Memória; v02 usou caption exata sem o sufixo de contagem. São erros de condução do script de prova, sem mudança de aplicação. v03 seleciona a etapa e a tabela semântica correta, exige uma única linha por ID visível e confirma cada recibo novo. JSON/logs e versões do script permanecem distintos, sem reescrita dos resultados vermelhos.

Execução real da prova: `node C:/Users/suporte/Documents/projetos/wms-rodogarcia/frontend/evidencias/fe-vig-008/provar-rejeicao-browser.mjs`, cwd `frontend/evidencias/snapshot-marco03-r37/frontend`, com Chromium local. Prova `fe-vig-008-browser-atual-v03.json` e log correspondentes no snapshot. O processo5189 permanece aberto para Lucas; o contexto/browser da prova foi encerrado normalmente.

Aplicação/testes/config/docs congelados após a emissão do MARCO03. As22 provas essenciais existentes e as fatias CSS/FE12/contratos/cliente permanecem por bytes, sem nova missão ou documentação duplicada. Vigia reconfirma o delta via Farol; sem aceite global antecipado. G01 permanece bloqueada, provider/dispositivos/fiscal/piloto e demais limites anteriores continuam explícitos.
