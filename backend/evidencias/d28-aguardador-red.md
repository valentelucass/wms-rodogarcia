# D28 — red do auxiliar offline de aguardador

Primeira execução offline recusada no parse PowerShell: operadores `-or`/`-and` ficaram no início da linha seguinte sem continuação. Nenhum SQL, credencial, JVM ou destino foi iniciado. Classificação: erro de sintaxe do novo teste/auxiliar D28, não defeito do backend. Corrigido posicionando os operadores no fim da linha; green registrado em `d28-aguardador-offline.json`.
