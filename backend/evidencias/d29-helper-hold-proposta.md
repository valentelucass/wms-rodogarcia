# D29 — prova isolada do aguardador atual

Regra: conservar o JAR próprio até SELECT Prumo, reconhecer somente `d29-prumo-select-liberar-RODADA.json` com rodada/WMS_DEV/WMSDEV/leiturasConcluidas coerentes; prazo monotônico120min. Nenhum sinal legado/PROD/identidade ausente deve encerrar a espera.

Hipótese concreta do helper atual: `readTree(Files.readAllBytes(signal))` sem tratamento pode interpretar arquivo momentaneamente incompleto durante publicação como falha fatal e sair antes de um sinal completo. Antes de corrigir, parametrizar somente o diretório e prazo do método auxiliar para fixture isolada. O caminho/prazo da execução real permanece fixo. Exercitar prefixo legado, rodada/alvo/identidade/ack inválidos, timeout e publicação em duas partes; sem app, SQL, segredo ou listener. Preservar red, fonte/classes antes de cada execução e depois repetir os mesmos casos com correção mínima, se a hipótese se confirmar.

Isto testa execução/captura, não defeito de Java de negócio nem witnessSQL. A fixture tem diretório próprio em target-d29-helper e não escreve sinais centrais.
