# Matriz V9 — preparação BE14 anterior ao Java

## Matriz atual FINAL p2-locks367 — 06/10

[Parecer atual904/129](../evidencias/d19-v9-2026-10-06-p2-locks-final.md), fonte31SHA0C57C902… e manifesto289SHAA257476…: quadro/79 colunas/constraints/índices/pares iguais ao histórico360, sem novo DDL. Models/enum e StringCheck do serviço conservados. CONTAGEM alvo aninhado, projeção de pedido e referências redundantes passam a sete regras lexicais adicionais; FATO_SERVICO usa unidadeId/contexto do DTO.25 regras ao todo, sem aceitar semântica/locks ou testes pela regex. Históricos retidos abaixo preservados; nova tarefa distinta permite esta reconciliação final de arquivos, aceite com Farol/Vigia.

## Reconciliação histórica360, sem aceite corrente

[Parecer histórico](../evidencias/d19-v9-freeze-2026-10-06-final-historico-p2-locks.md) registra904/904 e122/122 em arquivos na rodada autorizada do freeze360, agora históricos pelo P2 locks. Fonte31B459 consolida os complementos já representados (PREPARADA/REMANEJAMENTO/carga_id), sem novo DDL. Serviço situação usa String com Check nas linhas18/49–50, não enum Java; domínio três valores exato. As fronteiras abaixo conservaram seu desenho; guardas foram apenas lidas lexicalmente.

Farol/Vigia07:27 retiveram BE14 por CONTAGEM prelock raiz versus DTO aninhado. Correção/fonte/código em escrita não estão pareados por esta matriz; final só após nova tarefa/manifesto p2-locks. Predicados de estado/carga/zero/replay e compartilhamento de locks são matéria de revisão/testes por Cedro/Vigia, não conclusão de CHECK/regex. Leitores bloqueiam fase histórica antes de Java. V1–V9 SQL intactas; outputs424/904/122 e falhas anteriores preservados.

## Parser preparado após307 — sem parecer JPA

[Suporte JPA/imutabilidade/guardas](v9-leitor-jpa.md) e [matriz lexical17 regras](v9-guardas-leitura.json) são preparação posterior à entrega aceita. Campos existentes agora têm nomes de model/chaves JPA para o leitor, sem alteração de DDL, enums, negócio ou execução da comparação. Leitor -CompararJpa e suplemento aguardam freeze/mensagem de Farol; sintaxe não resolvida gera pendência. SQL V9 SHA17C6C2F9… intocado; cópias/hash anteriores e evidências424/194/22/307 preservadas. [Relatório novo](../evidencias/d19-v9-leitor-jpa-preparo-2026-10-06.md) separa fixtures/sintaxe de compatibilidade final.

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](../evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


Fonte oficial: [schema/API BE14 no doc31](../../docs/31-fechamento-contagem-e-contingencia.md), leitura do rodapé/complementos e delta integral final, SHA-256 `52D6071C4050A178E484B3EF70339B2BB829E843D2AC2CBF7CEBAAE3C98A77A5`. [Cópia literal desta preparação](../evidencias/d19-v9-preparacao-2026-10-06-fonte-doc31-vigencia.md). [Delta para996750](../evidencias/d19-v9-preparacao-2026-10-06-fonte-diferencas-vigencia.json). A [transcrição técnica](v9-schema-doc31.json) não substitui a fonte. Draft C7EDB892/383, complemento23CB/395, residuais e996750/407, SQL/JSON/leitores/fontes/metadados preservados antes dos ajustes; são fronteiras históricas.

Os dois residuais23CB foram formalizados na fonte996750 antes do Java: resolver-cancelamento Gestor com prova integral/zero físico/ocupação/reserva/unitização, preservação entrada_id em CANCELADA e vínculo opcional resolucao_remanescente.carga_id FK/índice. Campo final do DTO/envelope/hash `efeitoRegistradoNoWms`: true somente VINCULAR efeito WMS comprovado, false reconstrução temporal do fato físico ainda não registrado; JSON existente. Nenhum literal/tabela extra. Esta preparação estrutural está alinhada à fonte, mas não aceita implementação/guardas/JPA BE14 ainda sem freeze.

Fonte52D607 formalizou ainda o encerramento futuro de vigências de CONTRATO/TABELA/VINCULO por Gestor/referência no31, sem editar29 ou criar DDL BE12. ENCERRAMENTO_VIGENCIA já era literal de V7; ampliar somente o par CONTRATO_COBRANCA/ENCERRAMENTO_VIGENCIA nos dois sentidos do CHECK financeiro. Vínculo mantém TABELA_COBRANCA como alvo auditado. A tabela simbólica do leitor exige essa única ampliação de par anterior e nenhuma perda/combinação indevida.

**Fonte corrente desta preparação:** [cópia1D6413AF](../evidencias/d19-v9-preparacao-2026-10-06-fonte-doc31-solicitacao.md), SHA1D6413AF9CBD37180DD1400D72F8CBE837424718C618DCA8C7E2F3557AAB8B38. [Delta](../evidencias/d19-v9-preparacao-2026-10-06-fonte-diferencas-solicitacao.json) formaliza SOLICITACAO_ENCERRAMENTO auditoria/admin para os sete cadastros, Gestor ATIVO→ENCERRAMENTO_PENDENTE, sem coluna/tabela/enum novo. Histórico SOLICITAR_ENCERRAMENTO permanece; serviço ganha solicitação identificada. CHECK financeiro também admite esse par SERVICO_COBRANCA. Output413/413;410/407/395/383 e check15/16 por mudança da fonte preservados. JPA/negócio aguardam freeze.

[V9](../migrations/V9__contagem_carga_contingencia_e_encerramento.sql): oito tabelas, 79 colunas, 18 FKs, oito constraints UNIQUE, 24 CHECKs de tabela, dez índices comuns e um único filtrado. Cinco CHECKs cumulativos substituídos e um novo CHECK de pares BE14; somente a coluna adicional formal carga_id, sem tabela extra, DML, default, seed, trigger, grant ou cascata. V1–V8 permanecem byte a byte no baseline.

| Tabela | Colunas / FK / UNIQUE / CHECK | Identidade e mutabilidade previstas; verificar JPA no freeze |
| --- | --- | --- |
| contagem_estoque | 7 / 1 / 1 / 1 | Uma por unidade; identidade/unidade/criação preservadas. Versão/revisão/impedimento/alteração correntes. Impedimento próprio, sem reutilizar avaria/bloqueio. |
| revisao_contagem | 17 / 2 / 1 / 9 | Contagem/número/observação/snapshot originais preservados; situação e aplicação evoluem com auditoria. `reserva_id` referencia pedido_saida, conforme quadro, não reserva_saida. |
| carga_inicial | 13 / 4 / 1 / 4 | Identidade por cliente/armazém/referência. Origem/quantidade/revisão congeladas após PREPARADA. Versão, estado, alteração e entrada ligados pelos serviços. |
| revisao_carga_inicial | 8 / 1 / 1 / 2 | Snapshot imutável por carga/número; JSON apenas fornecido/hash/autor/motivo. Sem editar revisão usada pela preparação. |
| linha_contingencia | 16 / 2 / 1 / 5 | Identidade VARCHAR200 global, sem UUID/ação/contexto na chave. Contexto/instante/conteúdo/hash originais imutáveis; estado/pendência/resultado/data evoluem juntos. |
| dependencia_contingencia | 3 / 2 / 1 / 1 | Par único e sem autorreferência; vínculos materializados quando possíveis, conservando declaração original no JSON. |
| configuracao_aviso_validade | 6 / 2 / 1 / 1 | Um contexto; antecedência/versão/alteração mutáveis por Gestor. Sem antecedência padrão ou bloqueio automático. |
| resolucao_remanescente | 9 / 4 / 1 / 1 | Pedido único/contexto/carga opcional/unidades/autor/motivo/criação imutáveis; pedido integral é conduzido pelas regras normais. carga_id não é UNIQUE, somente FK indexada conforme fonte. |

Datas DATETIME2(6) UTC, quantidades DECIMAL(19,6), IDs/FKs BIGINT, oito PKs IDENTITY, JSON NVARCHAR(MAX)/ISJSON; textos humanos NVARCHAR e opcionais somente onde declarados. Sem defaults de versão/data/valor histórico. FK tem índice próprio ou prefixo de UNIQUE; entrada opcional usa `WHERE entrada_id IS NOT NULL`, permitindo vários estágios sem origem ligada e recusando repetir a mesma entrada preenchida.

| Fronteira | Proteção no arquivo SQL | Verificação de serviço/JPA ainda pendente |
| --- | --- | --- |
| Contagem | Esperado/contado não negativos, diferença exata, número/versões válidos, estados, JSON e par data/efeito; APLICADA exige ambos. | Revisão/unidade/conteúdo atuais, observado não futuro, reserva×retirada, origens/deltas/comprovação/recomposição; fim físico/permanência no zero. |
| Revisão histórica aplicada | SQL permite conservar aplicada_em/efeito_json em SUBSTITUIDA; não exige que só APLICADA possua o efeito. | Conferir transição histórica no freeze. Não limpar efeito antigo para satisfazer CHECK. |
| Carga | PENDENTE sem entrada; PREPARADA/REGULARIZADA exigem entrada. CANCELADA aceita NULL de estágio pendente cancelado ou entrada histórica preparada/resolvida. Quatro estados formais. | Preparar/unitizar uma vez e ler TODAS etiquetas; indisponibilidade comum mantida para estágio não REGULARIZADO com conteúdo positivo. resolver-cancelamento comprova zero/ref/exaustividade; falha de auditoria reverte só a transição, conserva resolução já existente. |
| Contingência | Onze tipos formais, identidade global, par estado/data/resultado, JSON, FKs e não autorreferência. | Envelope dependencias/efeitoRegistradoNoWms/hash imutável, ausência/materialização posterior e ciclos por identidade ausente. True só VINCULAR; false temporal suficiente/contexto/tempo, história insuficiente pendente. |
| Inativação | CHECKs cadastrais INATIVO já estão em V8; V9 não os reescreve. Par administrativo/auditado ampliado. | Gestor/escopo antes replay, revisão e matriz completa física/financeira sob locks comuns; resolução histórica preservada, sem reativação/deleção. |
| JSON/hash/contexto | ISJSON e FK simples verificam somente sintaxe/existência. | Não provam estrutura/bytes/hash, todos os IDs, alcance/contexto, somas, disponibilidade, prova temporal ou imutabilidade. Sem trigger cruzado presumido. |

## Pareamento cumulativo

Auditoria mantém os tipos/ações anteriores; novos tipos CONTAGEM_ESTOQUE/CARGA_INICIAL/CONTINGENCIA/AVISO_VALIDADE cabem no VARCHAR20 existente; ações cabem VARCHAR30. Operação administrativa mantém VARCHAR32 e acrescenta os doze comandos: LEITURA_CONTAGEM, APLICACAO_CONTAGEM, REGISTRO_CARGA, REVISAO_CARGA, PREPARACAO_CARGA, CONFIRMACAO_CARGA, CANCELAMENTO_CARGA, REGISTRO_CONTINGENCIA, CONCILIACAO_CONTINGENCIA, CONFIGURACAO_VALIDADE, INATIVACAO_DEFINITIVA e RESOLUCAO_REMANESCENTE.

Complemento1D6413 acrescenta o décimo terceiro comando administrativo SOLICITACAO_ENCERRAMENTO e a décima quarta ação nova de auditoria, totalizando26 pares novos tipo/ação (sete cadastros em cada solicitação/inativação). Domínios finais:19 tipos/71 ações auditoria,37 tipos administrativos e nove ações de movimento. Preservados todos os pares antigos permitidos; três ampliações financeiras: serviço/inativação, serviço/solicitação e contrato/encerramento de vigência.

AJUSTE_ESTOQUE acrescenta somente movimento_estoque.acao VARCHAR24 e auditoria PEDIDO_ENTRADA; não foi presumido tipo administrativo. Os treze novos pares de ação expandem para dezenove pares tipo/ação porque INATIVACAO_DEFINITIVA admite os sete cadastros definidos. CHECK bidirecional restringe os quatro tipos novos às suas ações. Pareamento saída V6 e dez ações FECHAMENTO_COBRANCA V8 permanecem intocados. Pareamento financeiro V7 permite SERVICO_COBRANCA/INATIVACAO_DEFINITIVA e CONTRATO_COBRANCA/ENCERRAMENTO_VIGENCIA, conservando todos os pares anteriormente válidos.

O [leitor](../verificar-schema-v9.ps1) compara fonte/transcrição/DDL, FKs/índices/nulos/precisões/uniques/CHECKs/ISJSON, ausência de DML e hashes V1–V8. Faz avaliação simbólica do subconjunto textual dos CHECKs de tipo/ação: todas as combinações antigas preservadas e novos pares exatos, sem usar engine SQL. Essa cobertura não prova negócio, collation/NULL real, locks, dialeto/permissões, auditoria transacional ou JPA. No freeze ampliar o leitor aos models/enums/literais gravados/guardas reais; não executar JVM para essa comparação.

Procedimento em [migrations/README](../migrations/README.md), acessos em [permissões mínimas](../permissoes-minimas.md) e [ensaio/recuperação](../../infra/recuperacao-e-ensaio.md). JPA BE14 ainda em implementação; não há aceite BE14 ou migration executada nesta preparação.
