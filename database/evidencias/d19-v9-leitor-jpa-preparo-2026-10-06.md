# V9 — preparo do leitor JPA e suplemento, posterior ao307 aceito

Farol informou aceite local194/424/22/307 e Vigia favorável06:09; não é execução externa. Esta revisão prepara ferramentas para a futura tarefa/freeze BE14. Escrita somente database; backend consultado em leitura para construir parser. **Não executados -CompararJpa, suplemento ou comparação real de modelos/guardas; não há parecer de compatibilidade BE14.**

[Leitor V9](../verificar-schema-v9.ps1) agora aceita -CompararJpa/-ManifestoFreeze; [parser](../leitores/v9-java-arquivos.ps1), [coletor](../leitores/v9-comparar-jpa.ps1), [suplemento](../verificar-v9-freeze-suplementar.ps1), [matriz17 regras](../contratos/v9-guardas-leitura.json) e [protocolo](../contratos/v9-leitor-jpa.md) preparados. [Transcrição técnica](../contratos/v9-schema-doc31.json) acrescenta referências de model/nomes locais de chaves e plano JPA, sem alterar campos/tipos/enums/negócio/schema. [Matriz V9](../contratos/v9-matriz-cobertura.md) e [procedimento](../procedimento-v9.md) delimitam execução somente após mensagem de Farol.

Cobertura preparada: oito models/79 campos, carga_id/histórico e sete situações cadastrais legadas, herança/Embedded simples, IDs/Version, tipos/precisões/tamanhos/nulos/Unicode/FKs, enums STRING/chaves. Literais cumulativos diretos/ternários/repasses privados sem reatribuição, enum com escopo explícito de serviço e pendência de expressão não resolvida. Pares saída/financeiro/fechamento/BE14 ficam cumulativos. Entrada administrativa ENTRADA_CONTINGENCIA não vira nova ação auditoria; AJUSTE conserva APLICACAO_CONTAGEM/AJUSTE_ESTOQUE. Guardas são ocorrência/ausência/ordem lexical no método, sem promessa de fluxo completo.

## Verificação desta preparação

Único script de fixtures executado, na raiz WMS:

```powershell
./database/verificar-parser-v9-fixtures.ps1
```

[Output31/31](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-fecho.json) e [metadados reais](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-fecho-execucao.json): textos fictícios de anotações/strings/comentários, delimitadores/generics, campos/unicas/enums, repasse/reatribuição, cabeçalho/escopo do manifesto e bloqueio de freeze ausente antes de leitura. Fixture não abre backend, fonte31/V9 ou freeze real. PowerShell concluiu normalmente, LASTEXITCODE null sem declarar saída0 nativa.

[Bloqueio inicial](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures.json) e [metadados LASTEXITCODE1](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-execucao.json) preservados. Causa do parser: parâmetro chamado args conflitou com variável automática PowerShell; corrigido só no parser. [Parser inicial](d19-v9-leitor-jpa-preparo-2026-10-06-java-parser-inicial.ps1) e [fixtures iniciais](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-iniciais.ps1) copiados antes. [28/28 intermediário](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-final.json), [metadados](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-final-execucao.json), [parser28](d19-v9-leitor-jpa-preparo-2026-10-06-java-parser-28.ps1) e [fixtures28](d19-v9-leitor-jpa-preparo-2026-10-06-fixtures-28.ps1) também preservados. Ampliação31 verifica parâmetro reatribuído, generics com vírgula e enum finito; não é falha Java/SQL nem resultado do backend.

[Checks locais](d19-v9-leitor-jpa-preparo-2026-10-06-checks-locais.json) registram AST, JSON/regex, interfaces/safety, links, preservação/baseline e hashes. Não foi repetido o leitor424, diagnóstico194, fecho22 ou revisão333. Sintaxe/fixtures passadas não validam o coletor contra backend em escrita; hipóteses do parser precisam confirmação no próximo freeze.

## Preservação

[Antes](d19-v9-leitor-jpa-preparo-2026-10-06-antes.json):307 hashes da entrega aceita sem divergência antes desta revisão, quatro cópias byte a byte de leitor/contrato/procedimento/matriz. [Depois/metadados](d19-v9-leitor-jpa-preparo-2026-10-06-depois.json), [manifesto posterior](d19-v9-leitor-jpa-preparo-2026-10-06.sha256) e checks permitem nova conferência. Manifesto307 permanece byte a byte e refere a revisão anterior destes quatro documentos, preservada nas cópias; não foi sobrescrito ou convertido em aceite do parser novo. Todos os demais307 alvos, V1–V9, fontes/drafts/outputs e manifestos anteriores intactos.

SQL V9 preservado SHA17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8. Nenhum DDL extra ou edição em migrations/infra/doc33/backend/centrais/Graphify. Hash de fonte31 atual em inventário de construção é observação sem freeze; transcrição guarda a fonte formal anterior da V9 e exigirá reconferência/cópia se contrato final mudar.

## Limites e próximo passo

Fonte fora de manifesto/hash alterado/sintaxe desconhecida gera bloqueio seguro, sem raw. -ManifestoFreeze é requisito de rastreabilidade, não autorização automática; aguardar mensagem/tarefa de Farol. Nomes locais JPA/SQL separados; nullable unique não prova filtro/NULL real. ISJSON/CHECK/FK não provam hash/estrutura/contexto/imutabilidade; Instant/TIMESTAMP não prova DDL/precisão efetiva. H2 cria esquema próprio e não homologa SQL Server/locks/permissões/colação.

Mutabilidade, ocorrência/ordem lexical e repasses finitos não comprovam autorização/rollback/locks/recomposição/zero físico/cancelamento seguro/temporal/encerramento; revisão Vigia/Cedro e testes reais congelados permanecem obrigatórios. Artefato final/compatibilidade/aceite BE14 não antecipados. Não executados SQL/conexão/Flyway Info/Validate/JVM/Hibernate/H2/build/rede/env/segredos/fiscal/cobrança real/ETL/FE/Git/publicação. Próximo passo: receber freeze BE14 e tarefa separada, capturar fontes/manifesto atuais e só então rodar modos preparados. CLI próprio ausente conforme contexto; retorno neste ask/arquivos, sem identidade ou ambiente alheios.
