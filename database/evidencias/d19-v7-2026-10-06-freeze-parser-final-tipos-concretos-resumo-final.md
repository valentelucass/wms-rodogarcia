# V7 — resumo final para check de Farol, 06/10/2026

Prumo. **CHECK corrigido somente em arquivos; evidência intermediária, sem aceite BE05/BE12.**

- [Parecer extenso e hashes](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.md):518/518 verificações (494 anteriores +24 de cobertura),198 colunas JPA em leitura e67/67 suplementares, contra o complemento formal dos tipos em doc29 SHA E5144BAE18D0211B44D721868FA06F0F4B27F6D94AC0BBE6E66E75822B25A029.
- [Falha65/67 preservada](./d19-v7-2026-10-06-freeze-parser-final-suplementar.json) e [cópia V7 anterior](./d19-v7-2026-10-06-freeze-parser-final-V7-antes-tipos-concretos.sql). V1–V6 e20 outputs anteriores preservados. V7 mudou somente pela inclusão de CRIACAO_SERVICO/CRIACAO_TABELA no CHECK; ações/pares de auditoria intactos.
- Hash V7 atual:3193D8436D360BA0D1D36E290112F39F76AB3A300AC2C2AD931208B6745B8940. Inventário real:16 chamadas/13tipos;14 permitidos preservando CRIACAO anterior. Colunas/enums/FKs/chaves/JSON não receberam schema adicional.
- [Checks locais](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-checks-locais.json):23/23 e74 links na captura; AST sem erro. [Manifesto estrutural/documental](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-documentos.sha256) capturou82 fontes.
- **Doc29 mudou novamente após essa captura para tratar o P2.** [Releitura e diferença](./d19-v7-2026-10-06-freeze-parser-final-tipos-concretos-fonte-em-correcao.json): novo hash955718EFEB0A6AD7CD9A7BC8FCE423EF0D5BE565D487062FD31EB47016665A45, mesmo quadro SQL/tipos. O manifesto anterior é histórico:81/82 ainda iguais naquela última leitura, doc29 mudou externamente. O hash da transcrição continua na fonte E514 comprovada; não foi trocado automaticamente. O leitor deve sinalizar esse descompasso até reconciliação após o próximo freeze.
- Três arquivos de Cedro e states também evoluíram externamente durante o P2; models comparados permaneceram estáveis. Não inferir compatibilidade/aceite do freeze286 depois dessas alterações. Aguardar P2, revisão e novo freeze para doc29/JPA/hashes finais.
- [V8 em leitura/planejamento](./d19-v8-leitura-preliminar-2026-10-06.md):dez tabelas BE13 recebidas, sem migration V8, sem estrutura BE14 presumida e sem liberar Java. Retomada somente após Farol/P2/revisão contratual. Contagem textual corrigida na evidência: FECHAMENTO_COBRANCA tem19 caracteres (doc31 informa18); maiores estados têm22, cabendo24.
- README/procedimento/permissões/doc33 atualizados na área autorizada. Nenhum SQL Server inclusive Info/Validate, JVM/H2/build/git/fiscal/cobrança real/ETL/Graphify/frontend, segredos ou env/identidade emprestados.

CLI Maestri próprio/PATH ausentes; reporte neste arquivo/ask para WMS - Farol fazer check. Nenhuma list/mensagem via identidade alheia ou raw.
