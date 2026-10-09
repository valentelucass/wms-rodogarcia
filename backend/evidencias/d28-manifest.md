# D28 — manifest Cedro

Build novo: 420/0/0/0 em 20 XMLs; SHA `4304D4283BB21A0F80F3D6AD034F9891D560DFED754B6511E6BD89CDE8900CE1`.
Mesmo conteúdo do último JAR D27; comando, execução, relatórios e jornadas D28 são novos. Sem profile sqlserver-it.

| Rodada | Etapa | HTTP | Assertivas | Fase | Família |
| --- | --- | ---: | ---: | --- | --- |
| D28079AE038 | listas | 42 | 7 | falhou | 41/19 |
| D282DDA1365 | get-final | 12 | 3 | concluido | 43/20 |
| D2830B61B28 | get-final | 5 | 0 | concluido | 32/15 |
| D284B2EA65C | saldo-xml | 11 | 3 | concluido | 36/17 |
| D284DC609CC | xml-limites | 29 | 6 | falhou | 36/17 |
| D285C6DE3A9 | get-final | 10 | 1 | concluido | 43/20 |
| D287D721FEA | xml-ajuste | 178 | 54 | concluido | 32/15 |
| D288163F01B | xml-limites | 57 | 16 | concluido | 36/17 |
| D2888882761 | get-final | 10 | 1 | concluido | 36/17 |
| D28925E295C | variantes | 146 | 61 | concluido | 38/18 |
| D2895805120 | get-final | 5 | 0 | concluido | 34/16 |
| D289A0CF1ED | saldo-xml | 18 | 3 | falhou | 36/17 |
| D289E87D622 | carga-temporal | 109 | 25 | concluido | 34/16 |
| D28B9713205 | get-final | 12 | 2 | concluido | 38/18 |
| D28BBB4F694 | listas-estoque | 31 | 14 | concluido | 32/15 |
| D28E89EBAE4 | get-final | 8 | 0 | concluido | 41/19 |
| D28EFFF454F | concorrencia-http | 78 | 15 | falhou | 43/20 |

Offline é separado de HTTP real. GET complementar não é nova fixture nem repetição de mutação. SQL e aprovação final continuam separados.

Divergência D284DC609CC: cinco extremos400 passaram; positivo201 esperado recebeu409 SALDO_INSUFICIENTE por estoque ausente no roteiro. Preservado; não é defeito backend.
