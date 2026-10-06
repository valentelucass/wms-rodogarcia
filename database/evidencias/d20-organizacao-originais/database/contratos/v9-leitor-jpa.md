# V9 — suporte preparado para o freeze BE14

## Leitura FINAL autorizada p2-locks367 — 06/10

[904/904 e129/129 atuais](../evidencias/d19-v9-2026-10-06-p2-locks-final.md), fonte31SHA0C57C902…/manifestoSHAA257476… exatos. Nenhuma mudança de parser Java nesta tarefa, models/DDL/literais conservados. Suplemento25 regras verifica sete fronteiras adicionais: derivação CONTAGEM do DTO, projeção escalar do pedido, referências redundantes, mesma resolução no lock/efeito/pendência e FATO_SERVICO/contexto. Presença/ordem textual não prova equivalência semântica/transação/lock SQL Server. Vigia revisa código, Farol decide aceite.

Tentativa128/129 conservada: premissa lexical contarContingencia corrigida para chamada real contagens.contar(dto), sem backend/SQL alterados. Históricos904/122/411/360 preservados; nova fase autorizada/hash exato não é bypass de confiança ou seleção automática de freeze. Fonte mudando/divergente bloqueia com código seguro. Metadados reais PowerShell normal/null não alegam saída nativa0. O gate e a pendência360 abaixo descrevem a etapa anterior, resolvida para leitura somente pela nova tarefa explícita.

## Resultado histórico e gate P2 locks — 06/10

[Outputs904/122, tentativas/cópias e limites](../evidencias/d19-v9-freeze-2026-10-06-final-historico-p2-locks.md) concluídos na rodada autorizada do freeze360 do freeze360 são históricos, sem aceite. Backend/31 agora em correção P2 CONTAGEM prelock raiz versus DTO aninhado; nova tarefa/manifesto p2-locks necessários. Gates de fase histórica e hash exato do manifesto impedem nova comparação corrente. Não executar comandos finais abaixo até nova tarefa; essas instruções de preparo anteriores estão preservadas.

Parser admite varargs mantendo tipo distinto do enum singular; lista de fontes serializada com ToArray; quadro mais complemento usa união sem duplicidade. Serviço situação é String/Check formal, não enum presumido: Check literal exato ATIVO/ENCERRAMENTO_PENDENTE/INATIVO e VARCHAR24. Fixtures37/37. Regra de exclusão distingue histórico de ocupação corrente encerrada no zero; suplemento18 regras. Presença lexical não detecta equivalência do objeto/código travado com DTO delegado, nem prova locks/ramo/transação. Nenhuma adaptação de backend ou SQL. LASTEXITCODE null preservado, sem saída nativa0 inventada.

Revisão local posterior ao preparo194/424/22/307 aceito por Farol/Vigia. Escrita somente database; V1–V9 e evidências anteriores preservadas. Backend em leitura para construir parser, ainda em escrita por Cedro. **Não executado -CompararJpa ou suplemento contra backend; nenhum novo parecer de compatibilidade.**

| Componente | Cobertura preparada | Limite |
| --- | --- | --- |
| [Leitor principal](../verificar-schema-v9.ps1) | Mantém424 checks anteriores; -CompararJpa exige -ManifestoFreeze e hashes das fontes lidas |424 é resultado histórico preservado, sem executar leitor novo nesta preparação |
| [Parser](../leitores/v9-java-arquivos.ps1) / [coletor JPA](../leitores/v9-comparar-jpa.ps1) | Oito models/79 campos e sete situações legadas; nomes/tipos/tamanhos/precisões/nulos/Unicode, IDENTITY/@Version, FKs, enums STRING, herança @MappedSuperclass/Embedded simples, chaves e carga_id | Subconjunto lexical declarado, sem observar Hibernate; Instant/TIMESTAMP corresponde ao contrato DATETIME2(6), sem precisão efetiva comprovada |
| Unicidades | Chaves e nomes JPA/SQL explícitos separados; entrada_id nullable unique JPA versus filtro SQL | @JoinColumn(unique=true) não expressa WHERE IS NOT NULL; não prova NULL/collation/índice físico |
| Literais | Chamadas salvar nos services presentes no manifesto; auditoria em fontes BE14 afetadas e MovimentoEstoque; literais diretos, ternários e helpers privados sem reatribuição | Dinâmica não resolvida gera pendência, não aprovação. Enum é sobreaproximação; não prova ramos/efeitos/execução |
| [Suplemento](../verificar-v9-freeze-suplementar.ps1) / [matriz](v9-guardas-leitura.json) | Mutabilidade declarada dos79 campos e17 regras de ocorrência/ausência/ordem no método | Não prova guarda completa, autorização, transação/rollback, lock, capacidade ou efeito físico |
| [Fixtures](../verificar-parser-v9-fixtures.ps1) | Textos fictícios de comentários/strings, balanceamento/anotações, herança, enum/decimal/chaves, repasse/reatribuição/generics e manifesto | Não lê/pareia backend real, contrato31, V9 ou freeze real; não aceita BE14 |

## Procedimento após mensagem explícita de Farol

1. Receber tarefa e manifesto BE14/backend/contrato31 finais. Não inferir freeze de arquivo disponível/testes nem selecionar manifesto BE13 antigo. O parâmetro não substitui autorização/contexto de Farol.
2. Copiar/hash arquivos antes de ajuste; registrar V1–V9/manifesto/fontes antes/depois. Fonte31 diferente exige leitura/cópia/revisão da transcrição antes do parecer, sem atualizar hash automaticamente. SQL V9 permanece17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8 até divergência formal comunicada.
3. Comandos **futuros** na raiz WMS, com caminho real fornecido no freeze:

```powershell
./database/verificar-schema-v9.ps1 -CompararJpa -ManifestoFreeze backend/evidencias/ARQUIVO_FREEZE_BE14.sha256
./database/verificar-v9-freeze-suplementar.ps1 -ManifestoFreeze backend/evidencias/ARQUIVO_FREEZE_BE14.sha256
```

Fonte fora do manifesto, hash diferente ou mudança durante a leitura gera bloqueio seguro sem raw. Cabeçalho D19 e linhas hash/caminho locais são aceitos; wrappers/artefatos podem constar, mas somente fontes Java/docs numerados são lidos neste modo. Hashes consultados não comprovam todos os arquivos do manifesto/artefato final. Overrides, enums complexos/text blocks/sintaxe dinâmica/repasses cíclicos não são silenciosamente aprovados.

4. Output integral/metadados em sufixos novos, sem sobrescrever tentativas. Registrar início/fim/comando, `$?` e LASTEXITCODE imediatamente após invocação. Null com output completo/sem falhas registra conclusão PowerShell, não saída0 nativa. Bloqueio de parser/regex fica distinto de falha Java/SQL; corrigir só parser com cópia/output preservados, sem JVM/backend.
5. Conferir pares cumulativos: ENTRADA_CONTINGENCIA só administrativo; entrada audita PEDIDO_ENTRADA/ENTRADA_EFETIVADA; AJUSTE usa APLICACAO_CONTAGEM/AJUSTE_ESTOQUE. CANCELADA conserva entrada histórica e resolução.carga_id; não presumir guardas por FK/estado. Reportar linhas/hashes/chaves/nomes/lacunas reais e limites SQL nullable/filtro/ISJSON/CHECK.

Vigia/Cedro revisam fluxos/locks/rollback/replay/temporal/contagem/reserva/zero físico/contexto/encerramento/testes; Farol mantém aceite e estados. ISJSON/CHECK/FK simples não garantem estrutura/hash/alcance. H2 cria esquema próprio e não aplica V9 ou comprova dialeto/NULL/collation/locks/permissões SQL Server. Sem SQL/Flyway Info/Validate/JVM/H2/build/rede/env/segredos nesta tarefa. [Relatório](../evidencias/d19-v9-leitor-jpa-preparo-2026-10-06.md) registra fixtures/sintaxe/preservação; comparação final aguarda freeze separado.
