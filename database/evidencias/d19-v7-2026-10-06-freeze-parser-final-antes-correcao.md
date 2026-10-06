# V7 — fronteira de tipos administrativos antes da correção

Prumo, 06/10/2026. Comparação somente em arquivos do freeze parser-final. Este registro preserva a falha e não é aceite do bloco nem ensaio SQL Server.

O [leitor inicial](./d19-v7-2026-10-06-freeze-parser-final-leitor-inicial.json) encontrou 23 divergências aparentes: 22 campos de ComplementoFiscal embutido e um Long anulável. O leitor não expandia @Embedded e sua comparação PowerShell de Long/long ignorava caixa. Corrigida somente a leitura em database, a [conferência de anotações](./d19-v7-2026-10-06-freeze-parser-final.json) teve 494/494 checks contrato/SQL e 198 colunas JPA compatíveis. Isso **não conferia os tipos administrativos efetivamente gravados**.

A [conferência suplementar](./d19-v7-2026-10-06-freeze-parser-final-suplementar.json) teve saída 1, 65/67 verificações: CRIACAO_SERVICO e CRIACAO_TABELA não cabiam no domínio CHECK da V7. O leitor principal foi fortalecido e [confirmou a falha](./d19-v7-2026-10-06-freeze-parser-final-cobertura-antes-correcao.json): saída 1, 516/518, sendo 494 anteriores e 24 de cobertura da operação; duas falhas. Inventário: 16 gravações literais em seis services, 13 tipos distintos, encaminhados sem conversão por OperacaoAdministrativaService.

| Evidência | Constatação |
| --- | --- |
| ConfiguracaoCobrancaService.java:133/231 | Salva CRIACAO_SERVICO/CRIACAO_TABELA em operacao_administrativa.tipo |
| OperacaoAdministrativaService.java:76–83 | Encaminha tipo ao construtor sem normalizar |
| V7 SQL:118–119 | CHECK aceitava CRIACAO e 11 ações, sem os dois tipos concretos |
| Auditoria nos mesmos comandos | SERVICO_COBRANCA/CRIACAO e TABELA_COBRANCA/CRIACAO compatíveis; não devem mudar |

Inferência estática: criar catálogo/tabela alcançaria violação desse CHECK no SQL Server; efeito não foi executado ou observado no banco. H2/build do freeze não aplicam V7 e não comprovam esta fronteira.

Farol confirmou o achado e autorizou ajustar apenas o CHECK V7 após formalização no doc29, admitindo os tipos concretos e preservando demais tipos/auditoria. Nenhuma alteração Java por Prumo. [Cópia V7 anterior](./d19-v7-2026-10-06-freeze-parser-final-V7-antes-tipos-concretos.sql) SHA-256 6078D2803A6E52DE5425D4465E7FB065903AA1EA9F3B3498C8F82A5C58A2E328. Outputs e V1–V6 preservados. Nova comparação corrigida deve ter output e manifesto separados.

[Hashes antes](./d19-v7-2026-10-06-freeze-parser-final-antes.json): 272 arquivos; 222 declarados no manifesto Cedro conferidos, sem diferença na captura inicial. CLI Maestri próprio ausente; reporte por este arquivo/ask, sem identidade/env de terceiros.
