# Parecer V9 BE14 — preparação somente em arquivos, 06/10/2026

V9 é compatível como transcrição técnica do schema/API e complementos do31 lidos nesta rodada, fonte final SHA52D6071C4050A178E484B3EF70339B2BB829E843D2AC2CBF7CEBAAE3C98A77A5. [Output final410/410](d19-v9-preparacao-2026-10-06-leitor-vigencia.json), sem divergências estruturais. Não é comparação JPA, aceite de regras/BE14 ou execução/homologação SQL Server. BE13/333 já aceito localmente por Farol; sua revisão não foi repetida.

## Entrega e cobertura

| Arquivo | Conteúdo |
| --- | --- |
| [V9](../migrations/V9__contagem_carga_contingencia_e_encerramento.sql) | Oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs, dez índices comuns e um único filtrado; cinco CHECKs cumulativos substituídos e um novo de pares. |
| [Transcrição](../contratos/v9-schema-doc31.json) / [matriz](../contratos/v9-matriz-cobertura.md) | Fonte/baseline/tipos/nulos/precisões/chaves/pares, mutabilidade prevista e fronteiras entre SQL e serviço. |
| [Leitor estático](../verificar-schema-v9.ps1) | Leitura fonte/DDL/transcrição/V1–V8, enum exato quadro+complemento, FKs indexadas, JSON, filtrada, cumulativos e tabela simbólica dos pares. Não inicia backend ou engine SQL. |
| [Procedimento](../procedimento-v9.md) / [migrations README](../migrations/README.md) | Reprodução somente arquivos, coleta de metadados e ensaio futuro dependente de alvo/autorização externos. |
| [Permissões](../permissoes-minimas.md) / [recuperação](../../infra/recuperacao-e-ensaio.md) / [doc33](../../docs/33-preparacao-tecnica-local-backend.md) | Proposta de DML mínimo, snapshots/histórico, diagnósticos seguros e conciliação/restauração com donos externos; nenhuma concessão/rotina/execução. |

Todas as PKs BIGINT IDENTITY, FKs BIGINT sem cascata; datas DATETIME2(6) UTC, quantidades DECIMAL(19,6), JSON NVARCHAR(MAX)/ISJSON e textos humanos NVARCHAR. Opcional somente onde formalizado. Versões/datas/antecedência não recebem defaults; nenhum DML histórico, preço/parâmetro fiscal/configuração real, seed, trigger ou grant. 59 campos classificados como preservados e20 mutáveis são intenção técnica a reconferir contra JPA no freeze, não anotação Java já observada.

Carga: PENDENTE/PREPARADA/REGULARIZADA/CANCELADA. Preparação entrada/unitização uma vez e leitura posterior de TODAS etiquetas são do serviço. UNIQUE filtrado de entrada permite múltiplos NULL e proíbe repetir entrada preenchida; não substituir por UNIQUE comum nullable. CHECK exige entrada em PREPARADA/REGULARIZADA, sem entrada em PENDENTE, admite conservar entrada histórica em CANCELADA. resolver-cancelamento exige Gestor/escopo antes replay, zero físico/ocupação/reserva/unitização e referência exaustiva de pedido RETIRADO ou contagens APLICADAS a zero. SQL não comprova esses fatos nem libera unidade pelo estado isolado.

resolucao_remanescente.carga_id é BIGINT NULL/FK carga_inicial/indexada, imutável por criação, sem UNIQUE presumido. Pedido identificado/etiquetas/reserva integral autorizam apenas aquela resolução; disponibilidade comum continua recusando estágio não REGULARIZADO com conteúdo positivo. Guarda/elegibilidade e rollback deverão ser lidos/testados no freeze.

Contingência possui onze tipos formais e identidade VARCHAR200 UNIQUE global sem UUID/rota/ação/contexto na chave. Dependências ausentes ficam no conteudo_json.dependencias canônico/hash; FK materializada depois sem JSON/hash alterados ou origem fictícia. Contexto/DAG/ciclos/tempo/imutabilidade e reconstrução temporal insuficiente como pendência são serviços. Nome final efeitoRegistradoNoWms true só VINCULAR efeito WMS comprovado, false reconstrução de fato físico ainda sem registro; nenhum campo relacional adicional.

Revisão de contagem preserva leitura e origens; CHECKs limitam números/quantidades/diferença e sintaxe JSON. Data/efeito aparecem juntos e APLICADA exige ambos; SQL permite conservar efeito histórico em SUBSTITUIDA. Soma por origem, recomposição comprovada, reserva integral/separação, marco físico e permanência no zero não são provas do CHECK. Conferir mutabilidade/transição real no freeze, sem limpar histórico para satisfazer constraint.

## Pareamento cumulativo observado em arquivos

19 tipos/70 ações de auditoria nos VARCHAR20/VARCHAR30 existentes,36 tipos administrativos VARCHAR32 e nove ações de movimento VARCHAR24. Treze ações de auditoria novas expandem para dezenove pares tipo/ação; doze comandos são tipos administrativos. PREPARACAO_CARGA/CARGA_INICIAL cumulativa. AJUSTE_ESTOQUE é movimento e auditoria PEDIDO_ENTRADA, não tipo administrativo presumido. Sem novo movimento em estágio, aviso, inativação ou vínculo de efeito já existente.

Financeiro V7 ampliado para SERVICO_COBRANCA/INATIVACAO_DEFINITIVA e CONTRATO_COBRANCA/ENCERRAMENTO_VIGENCIA. Este último veio do complemento52D607 no31; literal já existia em V7, nenhum enum/coluna extra. TABELA/vínculo e todos os pares anteriormente permitidos permanecem. Constraints de saída V6 e fechamento V8 foram mantidas sem ALTER.

Leitor avaliou simbolicamente1.330 combinações, incluindo855 combinações anteriores: nenhuma perda, par indevido ou par formal faltante. A única ampliação entre tipos/ações anteriores é CONTRATO_COBRANCA/ENCERRAMENTO_VIGENCIA, exigida no output. É interpretação de predicados no subconjunto textual AND/OR/igualdade/IN, não execução do CHECK no SQL Server.

## Fontes, versões e metadados preservados

| Fonte literal | Evidência da leitura |
| --- | --- |
| [C7EDB892](d19-v9-preparacao-2026-10-06-fonte-doc31.md) | Draft SQL/JSON/leitor byte a byte e [383/383](d19-v9-preparacao-2026-10-06-leitor-draft.json) anteriores aos quatro complementos Lume. |
| [23CB5DDA](d19-v9-preparacao-2026-10-06-fonte-doc31-complemento.md) | PREPARADA/PREPARACAO_CARGA/onze tipos, [395/395](d19-v9-preparacao-2026-10-06-leitor-complemento.json), cópias e residuais documentados; não tratados como fronteira definitiva. |
| [996750EA](d19-v9-preparacao-2026-10-06-fonte-doc31-resolucao-carga.md) | carga_id/cancelamento/JSON final, [407/407](d19-v9-preparacao-2026-10-06-leitor-resolucao-carga.json); output informa fonte canônica A039… durante execução, leitura restrita à cópia996750. |
| [52D6071C](d19-v9-preparacao-2026-10-06-fonte-doc31-vigencia.md) | [Delta integral](d19-v9-preparacao-2026-10-06-fonte-diferencas-vigencia.json): correção de redação e par contrato/encerramento, sem outra estrutura. Output410 confirma fonte copiada/canônica iguais nessa captura. |

[Metadados da invocação final](d19-v9-preparacao-2026-10-06-leitor-vigencia-execucao.json): PowerShell terminou normalmente, saída JSON integral, powershellSuccess true e LASTEXITCODE null. Não declarada saída0 nativa. Metadados null anteriores ficaram intactos. [Antes](d19-v9-preparacao-2026-10-06-antes.json), [depois](d19-v9-preparacao-2026-10-06-depois.json), [checks de arquivos](d19-v9-preparacao-2026-10-06-checks-locais.json), [metadados consolidados](d19-v9-preparacao-2026-10-06-metadados.json) e [manifesto da área](d19-v9-preparacao-2026-10-06.sha256) permitem check próprio de Farol.

| Artefato final | SHA-256 |
| --- | --- |
| V9 SQL | A612F0E17D28E8370EAA1B28289E1AA724E50A5894A2A433DEA88B52EF91D17C |
| Transcrição | D00156BEDAE20B226C3169C8EF07D3C6F0C4A713D45889AB5292A77F07E3ABE9 |
| Leitor | AF568EEC546AB9FB29EA692E7A78F8191BEFAA393ED61B6AE6073AF5276508DC |

V1–V8 e evidências anteriores são conferidas pelo baseline antes/depois, com hashes explícitos; não existe histórico de aplicação Flyway nesta entrega. Escritas somente database/infra/doc33. CLI/MAESTRI_CLI próprios ausentes; retorno neste ask/arquivos, sem mensagem por identidade/env alheios, raw ou Hermes/ETL.

## Limites e próximo passo

Nenhuma conexão/SQL Server/Flyway Info/Validate/aplicação de migrations/Hibernate/JVM/H2/build executados. Sem backend/contrato31/centrais/Graphify/frontend/Git/publicação/fiscal/cobrança externa ou rotina alterados. H2/testes de Cedro são outra evidência, não dialeto/locks/permissões/NULL/índice filtrado/collation/restauração SQL Server.

Agora aguardar freeze BE14, manifesto/schema estáveis fornecidos por Cedro/Farol, para leitura JPA/enums/literais/imutabilidade/guardas/contexto e novo output separado. Preparo estrutural não homologa regras de negócio. Ensaio real de alvo/TLS/versão/collation/identidades/permissões/locks/backup/restauração continua com Lucas/TI/DBA e autorização específica; conciliação e equipamentos com responsáveis externos. Ausência desses dados não autorizou conexão ou segredos e não bloqueou esta preparação independente.
