# Parecer V7 — freeze p2-avaria-final — 06/10/2026

**Compatível em leitura de arquivos.** Doc29 final, transcrição técnica, anotações JPA e V7 preparada não apresentam divergência estrutural neste recorte. Comparador: **518/518** (494 contrato/SQL + 24 de cobertura administrativa), **198 colunas JPA**; suplementar: **67/67**. Farol informou **aceite LOCAL BE05/BE12 p2-avaria292** após Vigia favorável e esta evidência. O parecer de Prumo cobre estrutura/leitura; não aceita regras financeiras/comerciais nem homologa SQL Server.

Foram executadas somente as duas invocações PowerShell documentadas:

```powershell
./database/verificar-schema-v7.ps1 -CompararJpa
./database/verificar-v7-freeze-parser-final.ps1
```

Ambas concluíram sem erro e produziram JSON integral/analisável. `LASTEXITCODE` foi **nulo**, como preservado nos [metadados do leitor](d19-v7-2026-10-06-p2-avaria-final-leitor-execucao.json) e [do suplementar](d19-v7-2026-10-06-p2-avaria-final-suplementar-execucao.json). Isso não demonstra código nativo 0 dos leitores; nenhum subprocesso ou repetição foi feito para obtê-lo. [Contexto da execução e limites](d19-v7-2026-10-06-p2-avaria-final-metadados.json).

Escopo confirmado: 15 tabelas/172 colunas e 26 extensões opcionais, 36 FKs sem cascata, 13 unicidades, 75 CHECKs e 25 índices explícitos. Anotações de 17 models (15 novos mais cliente/armazém), com ComplementoFiscal embutido em ambos. Comparação de nomes, tipos, tamanhos, precisão/escala, nulos, identity, FKs, chaves, enums, CHECKs e índices conforme transcrição. `servico_cobranca.situacao` permanece VARCHAR(24), suficiente para ENCERRAMENTO_PENDENTE.

`memoria_diaria` mantém UNIQUE(calculo_id,data), com tabela/item/tarifa NULL quando não únicos. Regras/categorias, segmentos e intervalosValor/origens/parcelas/ajustes usam o JSON existente; o leitor confere formas e serialização, não executa cálculo. O P2 de avaria BE08 reconcilia marcador/movimento com histórico no serviço; não introduziu model, tabela, campo, enum ou CHECK. A revisão do comportamento cabe a Cedro/Vigia/Farol.

Inventário administrativo: 16 gravações literais, 13 tipos efetivos e 14 valores permitidos no CHECK. Os 13 tipos são ANULACAO_SERVICO, CALCULO, COMPLEMENTO_FISCAL, CONFIGURACAO, CRIACAO_SERVICO, CRIACAO_TABELA, ENCERRAMENTO_VIGENCIA, IMPORTACAO_ENDERECOS, MARCO_AVARIA, PREVIA_ENDERECOS, REFERENCIA_FISCAL, REGISTRO_SERVICO e VINCULO_TABELA. CRIACAO anterior continua permitido, embora não salvo como tipo administrativo nesse recorte. Auditoria conserva SERVICO_COBRANCA/CRIACAO e TABELA_COBRANCA/CRIACAO; seus domínios cumulativos continuam 14 tipos/47 ações e preservam o pareamento V6. Suplementar confere 17 chamadas auditadas, 11 domínios DTO e ausência de gravação física direta nos seis serviços analisados; não presume movimento a partir de ação auditada. [Output principal](d19-v7-2026-10-06-p2-avaria-final.json) e [suplementar](d19-v7-2026-10-06-p2-avaria-final-suplementar.json).

O [freeze final de Cedro](../../backend/evidencias/d19-bloco2-freeze-p2-avaria-final.sha256) contém 222 arquivos: todos os hashes conferem antes e depois. Contra o freeze parser-final, são exatamente cinco diferenças: CalculoCobrancaService, MovimentoEstoqueRepository, CobrancaIntegrationTest, backend/README e doc29; os outros 217 continuam iguais. Não houve alteração em models/schema, recursos properties, doc27 ou Java administrativo. A [entrega](../../backend/evidencias/d19-bloco2-entrega-p2-avaria-final.txt), o [resumo](../../backend/evidencias/d19-bloco2-resumo-p2-avaria-final.json) e a [preservação](../../backend/evidencias/d19-bloco2-preservacao-p2-avaria-final.json) foram lidos, sem executar seus comandos.

O resumo existente registra clean verify de Cedro **292/0/0/0**, finalizado às 02:17:02 -03:00 de 06/10/2026; JAR SHA83235E12A8CE2A0F2756B8702E1D3AF7E5ECDDCD584E64A25ED1C356AE910D5A. Seu hash foi conferido somente em leitura. Farol informou Vigia favorável às 02:33, sem novos achados; esse relato não constitui teste realizado por Prumo.

A seção formal de schema do doc29 final é idêntica à fonte E5144BAE da correção de tipos, após normalizar somente CRLF. A [comparação das seções](d19-v7-2026-10-06-p2-avaria-final-contrato.json) preserva os dois textos. A transcrição técnica recebeu somente `fonteSha256`, para o hash final DF9C92A4…; nenhuma estrutura/ação foi alterada. [Cópia anterior exata](d19-v7-2026-10-06-p2-avaria-final-transcricao-antes.json) SHA80C8F62176E86C61860C5F4CB7F2D4935E44E0AE1C77DA1C675EAEDC9EF60FD1.

Captura antes/depois: **361 arquivos, 356 iguais, cinco alterações locais autorizadas**, em transcrição, database/README, migrations/README, permissões mínimas e doc33. **112 arquivos de evidência históricos** de database/infra/backend permanecem iguais, incluindo falhas 65/67 e 516/518, outputs 518/67, cópia V7 anterior e comparadores históricos. A [falha/correção intermediária](d19-v7-2026-10-06-freeze-parser-final-tipos-concretos.md) permanece histórica; seus 494 checks antigos não são convertidos em prova de cobertura administrativa. Leitores atuais e V1–V7 SQL ficaram intactos. [Antes](d19-v7-2026-10-06-p2-avaria-final-antes.json), [depois](d19-v7-2026-10-06-p2-avaria-final-depois.json) e [manifesto final](d19-v7-2026-10-06-p2-avaria-final.sha256).

Após registrar o aceite recebido, a recaptura de encerramento encontrou 355 hashes iguais: as mesmas cinco alterações locais e uma alteração externa de states.md. Prumo não editou esse registro central; ele continua sob manutenção de Farol. O manifesto congelado de 222 fontes e os 112 arquivos históricos continuaram iguais, sem alteração de migrations ou leitores.

| Arquivo/fonte | SHA-256 final |
| --- | --- |
| doc29 | DF9C92A474A0617CC6AD2EF2D7BB6E149DB78F23B887F64070F33802A772581C |
| Transcrição V7 | 9CB714B41EB110A74297E40C499629AA6426814819019B47F69B87A4DE990550 |
| Leitor V7 | 752A2876DBDA2E9F64A28CF47137522DC1513D053CE507FEB966F4EEE2DBA535 |
| Leitor suplementar | EDB0889AF5A537F5B57FE076707FCD3FBA7485A08E954942B4960119D2F145F3 |
| V1 | 41346740B1D9FC60291908F82190722F9C004F5B04DBA6B68CE57E898B4E3D58 |
| V2 | E17AD30B1D8BACF1409FE3083661DE09A2602563776CA747449E0F0764470052 |
| V3 | B16CD6507AC256D4757385A9C98C984CFDF86918CDBBCBE2BE11DF40FCE6B6DD |
| V4 | CE6C77238777EC87DDDF05A72D925CCAFA1C784CA16883242238E45935221A05 |
| V5 | 57EA2766C4170159CB4CCD2C210481E63FA13D976BFD932D6E711A734DCFFF88 |
| V6 | EE0EEBAF5D7EA5B7329C182039A274EBE46DAFCB561EAEF5DF8846381A897D39 |
| V7 | 3193D8436D360BA0D1D36E290112F39F76AB3A300AC2C2AD931208B6745B8940 |

[Procedimento](../migrations/README.md#v7--cadastros-serviços-e-snapshots-de-cálculo), [permissões mínimas](../permissoes-minimas.md), [recuperação/ensaio](../../infra/recuperacao-e-ensaio.md) e [doc33](../../docs/33-preparacao-tecnica-local-backend.md) mantêm alvo/TLS/identidades/backup/restauração com donos externos e autorização específica. Matriz de permissões permanece proposta, sem GRANT; observação anterior sobre anotação atualizável de operacao_administrativa.recurso_id permanece, sem setter/comando atual. Não houve concessão adicional.

Este parecer não valida dialeto SQL Server, ISJSON em execução, constraints confiáveis/índices filtrados/SETs JDBC, ordem de flush, planos/locks/concorrência, permissões efetivas ou backup/restauração. H2/HTTP são evidências locais de Cedro e não executam V7. Nenhuma conexão/SQL/Info/Validate/migration/Hibernate/JVM/H2/build, alteração backend/centrais/Graphify, publicação/git ou fiscal/cobrança real ocorreu por Prumo.

BE13 segue autorizado por Farol; V8 aguarda o schema31 refinado comunicado por Cedro/Farol, sem escrita do draft antigo. Preparação posterior começa por matriz/schema/cobertura/procedimento/permissões; nenhuma tarefa V8 foi repetida nesta entrega. CLI Maestri próprio ausente no PATH e sem MAESTRI_CLI: bloqueio de ask back direto, sem usar identidade/env alheios ou raw. Parecer e artefatos entregues neste ask/terminal para check. [Encerramento e preservação final](d19-v7-2026-10-06-p2-avaria-final-encerramento.json) registra o aceite recebido e a recaptura após atualizar apenas esta documentação.
