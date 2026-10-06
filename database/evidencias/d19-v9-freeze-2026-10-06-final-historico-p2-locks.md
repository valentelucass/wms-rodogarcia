# D19 BE14 — leitura do freeze360 histórico e retenção P2 locks

Prumo, 06/10/2026. Escrita somente database/infra/doc33. **Não há parecer global final, aceite BE14 ou homologação.** Farol informou retenção por Vigia07:27: ContingenciaService/CONTAGEM obtinha prelock pelo código raiz enquanto executava DTO aninhado distinto. Cedro corrige contrato/código/testes; comparar novamente somente após nova tarefa com manifesto p2-locks exato. Não presumido DDL novo, nem alterado backend.

## Fontes e preservação

Freeze autorizado na abertura: [manifesto289](../../backend/evidencias/d19-bloco4-freeze-final.sha256), SHA42284559254B36C42806E9DC461E9A3A6C49A654984D9D1B332B745226ED3486. [Fonte31 copiada](d19-v9-freeze-2026-10-06-final-fonte-doc31.md), SHAB45954EF4E79346657F9406BF788FF5CB3432B951F37BF817381E30C36180E7B. [Coleta antes](d19-v9-freeze-2026-10-06-final-antes.json):289 hashes exatos,333/333 arquivos do preparo anterior exatos,21 cópias anteriores à escrita, nove migrations/JAR conferidos.

Quadro31 final tem oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. Desde o snapshot0A4D, três linhas do quadro consolidaram PREPARADA, REMANEJAMENTO e carga_id, que já estavam nos complementos, SQL e transcrição. Nenhum novo DDL neste freeze. identidadeExecucao, operação/prova e escala histórica do ajuste usam JSON existente. SQL V9 segue17C6C2F9FB5B362242CF54AB2CD615B5646011EB7AB81D052C8FCE6833DF88B8; V1–V8 intactas. A transcrição corrente aponta o snapshot final histórico, sem atualizar silenciosamente para fonte em escrita.

## Outputs históricos da rodada autorizada do freeze360

| Evidência | Resultado e alcance |
| --- | --- |
| [Leitor histórico](d19-v9-freeze-2026-10-06-final-leitor-reconciliado-final.json) / [metadados](d19-v9-freeze-2026-10-06-final-leitor-reconciliado-final-execucao.json) |904/904,15 models,86 campos (79 novos + sete situações), hashes de70 fontes consultadas estáveis durante essa leitura. Não é execução JPA/Hibernate nem aceite de regra. |
| [Suplemento histórico](d19-v9-freeze-2026-10-06-final-suplemento-guardas-final.json) / [metadados](d19-v9-freeze-2026-10-06-final-suplemento-guardas-final-execucao.json) |122/122: mutabilidade declarada e18 regras lexicais, inclusive ocupação corrente encerrada no zero. Não detecta equivalência do código pretravado com o DTO executado. |
| [Fixtures](d19-v9-freeze-2026-10-06-final-fixtures-reconciliado-final.json) / [metadados](d19-v9-freeze-2026-10-06-final-fixtures-reconciliado-final-execucao.json) |37/37 em textos fictícios; parser, balanceamento, varargs, String/Check, conjuntos e serialização. Não aceita backend. |

Inventário lexical:43 gravações administrativas/37 tipos distintos entre38 admitidos;34 ocorrências de auditoria nas sete fontes afetadas, incluindo sobreaproximação declarada dos sete tipos cadastrais; uma construção de movimento AJUSTE_ESTOQUE, sem pendência lexical. ENTRADA_CONTINGENCIA só administrativo, auditoria PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE continua APLICACAO_CONTAGEM/AJUSTE_ESTOQUE. Preservação cumulativa de pares avaliou símbolos do CHECK em arquivos, sem SQL engine ou inferência de movimento físico a partir de auditoria.

## Limitações corrigidas no leitor, com tentativas preservadas

1. [Inicial principal](d19-v9-freeze-2026-10-06-final-leitor-inicial.json) bloqueou PARSER_PARAMETRO_COMPLEXO: varargs Java. [Inicial suplemento](d19-v9-freeze-2026-10-06-final-suplemento-inicial.json) bloqueou serialização da lista de fontes. Usar tipo varargs distinto do singular e ToArray resolve os dois limites. Cópias anteriores estão no baseline; não são falhas Java.
2. [Primeira comparação integral](d19-v9-freeze-2026-10-06-final-leitor-parser1.json):901/904. Quadro consolidado mais complemento duplicava PREPARADA/REMANEJAMENTO no multiconjunto; leitor agora compara união dos literais. A terceira falha exigia enum Java de situação em todo cadastro, premissa incorreta para o serviço.
3. [Resposta específica segura](d19-v9-freeze-2026-10-06-final-servico-situacao-resumo-seguro.json): ServicoCobranca.java **linha18** CHECK ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; **linha49** @Column(nullable=false,length=24); **linha50** String situacao. Java capturado e SQL coincidem em domínio/tamanho/nulo. Não houve divergência estrutural de enum no serviço: seu mapeamento é String com Check, explicitado na transcrição. Leitor exige esse Check literal exato somente para representação formal StringComCheck; ausência/expressão adicional bloqueiam.
4. [Suplemento120/121](d19-v9-freeze-2026-10-06-final-suplemento-parser1.json) tinha ausência genérica `.delete`, atingindo ocupacoes.deleteAll no zero (ContagemEstoqueService.java:290). Fonte31 exige encerrar ocupação, preservando fatos/histórico. Negativa agora mira repositórios históricos; regra adicional confere sequência textual da alteração para zero/ocupação/fato, sem comprovar efeito/rollback. [Regras anteriores](d19-v9-freeze-2026-10-06-final-guardas-antes-ajuste-zero.json) preservadas.

[Transcrição usada904](d19-v9-freeze-2026-10-06-final-transcricao-usada904-reconstituida.json) e [regras usadas122](d19-v9-freeze-2026-10-06-final-guardas-usadas122-reconstituidas.json) foram reconstituídas somente removendo os metadados posteriores de retenção: seus hashes coincidem exatamente com os registrados nos respectivos outputs. Não são cópias feitas antes do evento; essa origem está explícita. Cópias antes das correções e todos os outputs anteriores permanecem separados.

Comandos e início/fim reais constam nos metadados. `$?` true nos três outputs concluídos; LASTEXITCODE **null**, registrado sem inventar saída nativa0. Tentativas bloqueadas/falhas têm `$?` false e null, também preservados. A saída0 do processo que coletou evidência não é a saída nativa do leitor invocado.

## Situação corrente e limites

Leitor/suplemento correntes bloqueiam a fase HISTORICO/AGUARDA_NOVA_TAREFA antes de ler fontes Java; também exigem o hash do manifesto formal indicado no contrato. Gates foram conferidos por AST/texto, **sem executar nova comparação após o aviso recebido neste ask**. Fonte fora do manifesto, diferença ou mudança ao ler continuam bloqueios seguros sem raw. O estado atual do backend em escrita não está coberto pelo904/122 histórico.

Nullable UNIQUE JPA não expressa índice SQL filtrado `entrada_id IS NOT NULL`; oito chaves comuns e filtro são separados. Instant/TIMESTAMP e quantidades mapeiam ao contrato DATETIME2(6)/DECIMAL(19,6), sem precisão efetiva observada no Hibernate/SQL Server. ISJSON/CHECK/FK garantem somente o que seus textos dizem; não provam bytes/hash/estrutura/contexto/imutabilidade transacional. H2 e build360 de Cedro não aplicam V9 nem comprovam dialeto, locks, collation, índices/permissões reais. Nenhum SQL/JVM/H2/build/rede/env/segredo executado ou coletado por Prumo.

[Referência histórica JAR360 e preparo técnico](../../infra/evidencias/d19-be14-360-historico-2026-10-06.md), [procedimento V9](../procedimento-v9.md), [protocolo do leitor](../contratos/v9-leitor-jpa.md) e [doc33](../../docs/33-preparacao-tecnica-local-backend.md) registram dependência e próximo passo. Vigia revisa semântica/locks; Farol mantém estados e aceite. Aguardar tarefa p2-locks, sem reabrir revisão333 ou alterar migrations.
