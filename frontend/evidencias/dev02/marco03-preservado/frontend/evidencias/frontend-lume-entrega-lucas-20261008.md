# MARCO03 — correção focal FE-VIG-008

Fonte congelada SHA256 `c58b64105a7b3232f314d56f8150ccbd6ce1530b6fe4d33837ba46ad3dd4f922`, 179 arquivos idênticos ao snapshot R37 executado. [Manifesto](frontend-lume-manifesto-estavel-marco03-20261008.json), SHA256 `c5cb8a3a874dc5c7405a47cb340ea231f0b5fe8e8c8a7874bd5d9ed5166a38ac`. [Recibo JSON](frontend-lume-entrega-lucas-20261008.json). Sem aceite independente antecipado.

O respondedor fictício de rejeição agora informa REJEITADO/REJEITADA no recibo e na consulta. A única mudança de aplicação é ExampleBilling; backend/contratos/CSS/cliente/FE12 íntegros. IDs, revisão, número/hash e documentos/históricos permanecem separados por versão. Teste Vigia literal28fd, sem adaptações; reprodução própria RED antes da correção,10 focais verdes depois. [Delta e comandos](fe-vig-008/delta-r36.json).

Tipagem/lint/build exit0; **299 testes locais +16 Chromium**, zero falhas/ignorados/flaky. Mais uma prova DEV atual de rejeição/consulta em5189, fora da contagem16. 161 smokes não161jornadas;22 critérios compartilhados e seus hashes continuam no mapa existente, sem duplicação de suíte/documentação.

| Comando real no R37 | Resultado | Log |
| --- | --- | --- |
| npm run typecheck | exit0 | [log](snapshot-marco03-r37/frontend/evidencias/marco03-r37-typecheck-final.log) |
| npm run lint | exit0 | [log](snapshot-marco03-r37/frontend/evidencias/marco03-r37-lint-final.log) |
| npm test -- --reporter=default --reporter=json --outputFile=evidencias/marco03-r37-unit-resultados.json | exit0 | [log](snapshot-marco03-r37/frontend/evidencias/marco03-r37-testes-final.log) |
| npm run build | exit0 | [log](snapshot-marco03-r37/frontend/evidencias/marco03-r37-build-final.log) |
| npm run test:browser | exit0 | [log](snapshot-marco03-r37/frontend/evidencias/marco03-r37-browser-final.log) |

**Lucas: http://127.0.0.1:5189**, fictício, PID19856, fonte corrigida R37/c58b. [Vínculo fonte/URL/PID/argv](frontend-lume-exercicio-marco03.json); [prova browser atual](snapshot-marco03-r37/frontend/evidencias/fe-vig-008-browser-atual-v03.json), duas capturas. Gestor/contexto→cálculo801→fechamento901→versão1001→rejeição com motivo→GET atual. Payload usa id901/número1/revisão0; resposta e UI exibem REJEITADO/REJEITADA. Sem copiar IDs de DTO bruto; seleção por tabela e ID exibido. ZeroAPI/externos/CSP/console. Captura de consulta inspecionada.

5178 e5188 foram preservados, sem kill/restart. **5188/R32 é MARCO02 histórico DD440A, com FE-VIG-008 conhecido; não é a fonte corrigida.** A equivalência antiga177 arquivos não é reutilizada como prova desta aplicação alterada. O lançador novo usa a mesma vite.dev.config.ts/CSP, exige fictício+hash corrigido e proxy ausente antes de escutar. Lançamento CLI direto recusado pela revisão automática (bloqueada por política, sem motivo adicional), sem processo criado; alternativa restrita executada e observada. [Detalhes](fe-vig-008/fecho.md).

MARCO02:180 cópias preservadas (177fontes+manifesto+recibos),7782 referências verificadas antes da escrita,zero divergência. Checks/recibos/logs antigos permanecem próprios.167 entradas gerador apontam bytes reais/verificáveis e3 outputs iguais; sem nova geração/execução backend. CSS Prumo/FE12 Cedro iguais, com provas reaproveitadas.

G01 continua bloqueada. Sem API/backend/SQL/provider real/dispositivos/fiscal/piloto. Bundle>500kB e demais limites registrados permanecem. Aplicação/testes/config/docs congelados para revisão do delta integrado via Farol; somente os processos fictícios próprios permanecem abertos.
