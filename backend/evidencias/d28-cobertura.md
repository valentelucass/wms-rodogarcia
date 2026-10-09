# D28 — cobertura causal Cedro

Artefato unico `4304D4283BB21A0F80F3D6AD034F9891D560DFED754B6511E6BD89CDE8900CE1`. Casos/UUID/IDs/RequestId e assertivas SQL completas em `d28-cobertura.json`; indices com base zero e numero de caso com base um.

| Criterio | Classificacao | Casos D28 vinculados | SQL novo | Offline/preflight | Limite |
| --- | --- | ---: | --- | --- | --- |
| D28-VIG-R01 | backend | 6 | D287D721FEA:181/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R02 | backend | 5 | D289E87D622:193/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R03 | backend | 12 | D289E87D622:193/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R04 | schema aplicado | 19 | D287D721FEA:181/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R05 | backend | 26 | D287D721FEA:181/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R06 | backend | 46 | D287D721FEA:181/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R07 | backend | 39 | D287D721FEA:181/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R08 | backend | 16 | D288163F01B:88/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R09 | backend | 19 | D288163F01B:88/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R10 | helper | 0 | D287D721FEA:181/0 | d28-guardas-offline.json, d28-prumo-preflight.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R11 | helper | 0 |  | d28-https-falha-offline.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R12 | helper/oráculo | 18 | D282DDA1365:68/0, D28925E295C:211/0, D28EFFF454F:104/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R13 | oráculo SQL | 19 | D284B2EA65C:34/0, D287D721FEA:181/0 | d28-sql-quoting-offline.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R14 | helper | 0 | D28EFFF454F:104/0 | d28-processo-limitado-offline.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R15 | oráculo SQL | 90 | D287D721FEA:181/0, D289E87D622:193/0, D28EFFF454F:104/0 | d28-prumo-comparadores.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R16 | oráculo/permissão | 35 | D284B2EA65C:34/0, D28925E295C:211/0, D289A0CF1ED:43/0, D28EFFF454F:104/0 | d28-saldo-xml-guarda-offline.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R17 | segurança | 0 | D28EFFF454F:104/0 | d28-prumo-capacidade-witness.json, d28-prumo-preflight.json | DMV online recusada SQL300; sem grant, sa ou novo observador. HTTP/JVM+SELECT posterior sao provas distintas. |
| D28-VIG-R18 | oráculo SQL | 16 | D289E87D622:193/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R19 | registro | 35 | D287D721FEA:181/0, D28925E295C:211/0, D289E87D622:193/0, D28EFFF454F:104/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R20 | registro | 14 | D282DDA1365:68/0, D284B2EA65C:34/0, D287D721FEA:181/0, D288163F01B:88/0, D28925E295C:211/0, D289E87D622:193/0, D28EFFF454F:104/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R21 | oráculo SQL | 102 | D282DDA1365:68/0, D284B2EA65C:34/0, D287D721FEA:181/0, D288163F01B:88/0, D28925E295C:211/0, D289E87D622:193/0 | d28-prumo-comparadores.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R22 | helper | 0 |  | d28-readiness-offline.json | Offline novo sem SQL/credencial/API; nao comprova witness nativo online nem execucao do aguardador completo em banco. |
| D28-VIG-R23 | helper | 3 | D28EFFF454F:104/0 | d28-aguardador-offline.json | Offline novo sem SQL/credencial/API; nao comprova witness nativo online nem execucao do aguardador completo em banco. |
| D28-VIG-R24 | oráculo concorrente | 0 | D28EFFF454F:104/0 | d28-prumo-comparadores.json, d28-prumo-comparadores-complemento.json | Offline novo sem SQL/credencial/API; nao comprova witness nativo online nem execucao do aguardador completo em banco. |
| D28-VIG-R25 | oráculo temporal | 28 | D287D721FEA:181/0, D28925E295C:211/0 |  | DTO e SQL comparados na precisao disponibilizada; nao inferir micros ausentes nem alterar fatos. |
| D28-VIG-R26 | registro | 13 | D282DDA1365:68/0, D28EFFF454F:104/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R27 | helper/segurança | 3 | D28EFFF454F:104/0 | d28-respostas-offline.json | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |
| D28-VIG-R28 | jornada | 66 | D289E87D622:193/0 |  | Minimo50 aprovado usa corte real06->07 e contrato ficticio; previsoes54/53.75 abertas sao distintas. Nao fabrica historia nem emite NFS-e. |
| D28-VIG-R29 | jornada | 218 | D28079AE038:42/0, D28925E295C:211/0, D28BBB4F694:24/0, D28E89EBAE4:42/0 |  | Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger. |

Nenhuma linha usa historico como prova D28. Offline nao soma HTTP/SQL. R17 permanece com limite material SQL300. Reds e greens separados no recibo `d28-cedro.md`. Aceite e registros centrais pertencem a Farol/Vigia.
