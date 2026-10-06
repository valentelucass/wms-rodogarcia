# Plano de permissões SQL Server — preparação local

**D20 corrente:** a [matriz por coluna](permissoes-d20.md) reconcilia o SQL UPDATE estático preparado pelo Hibernate final de Cedro (34 statements/64 entidades); inclui herança, embedded e versão. Prevalece sobre resumos antigos abaixo. UPDATE em coluna inalterada ainda exige permissão se presente no statement; ausência de setter não garante proteção. Nenhum GRANT real ou prova SQL Server. App/migration distintas e restritas; sa somente CREATE separado. [Procedimento](d20-procedimentos.md) e [relatório](evidencias/d20-relatorio-final.md).

Atualização FINAL local p2-locks367: [V9/JPA904/129](evidencias/d19-v9-2026-10-06-p2-locks-final.md) e [diagnóstico238](../infra/evidencias/d19-preparacao-p2-locks367-2026-10-06.md) leem arquivos congelados; não consultam/aplicam privilégios ou locks reais.64 tabelas e planos de menor privilégio abaixo mantidos, sem novo schema. DBA a nomear por Lucas/TI comprovará permissões/heranças/custos/locks no ensaio autorizado. Vigia/Farol decidem revisão/aceite local, não concessões externas. Histórico360 preservado.

Revisão histórica BE14 em06/10: [outputs904/122 e retenção P2 locks](evidencias/d19-v9-freeze-2026-10-06-final-historico-p2-locks.md) são leitura lexical/estrutural, sem comprovar permissões/locks efetivos. V1–V9 intactas; novo freeze p2-locks e ensaio externo autorizados continuam necessários. Nenhum GRANT/conta/perfil foi aplicado ou consultado no SQL Server. O plano abaixo permanece preparado.

## Atualização local do pareamento complementar V9 — 06/10

Fonte31 copiada SHA0A4D40A89EDBA6E3CCD124379F818C2813DF55ED992C478A798E76C093F6217C, anterior ao Java. [Parecer/outputs novos](evidencias/d19-v9-preparacao-2026-10-06-pareamento-complementar.md): leitor424/424, somente arquivos, oito tabelas/79 colunas/18 FKs/oito UNIQUE/24 CHECKs de tabela/dez índices comuns/um único filtrado; seis CHECKs cumulativos substituídos e um novo de pares. ENTRADA_CONTINGENCIA acrescentado só em operacao_administrativa.tipo VARCHAR32, total38; auditoria permanece PEDIDO_ENTRADA/ENTRADA_EFETIVADA. AJUSTE mantém APLICACAO_CONTAGEM/AJUSTE_ESTOQUE, sem ação adicional. Serviço mantém VARCHAR24 e domínio ATIVO/ENCERRAMENTO_PENDENTE/INATIVO; V7 já tinha esses literais, V9 reafirma por DROP/ADD WITH CHECK formal autorizado, sem coluna/tabela/enum novo.

V1–V8 byte a byte preservadas. V9 preparada foi atualizada com cópia/hash anteriores; não afirmar imutabilidade integral V1–V9. Fonte1D/413, manifesto250, drafts e diagnósticos191/194 e194/194 anteriores são históricos preservados. Os registros abaixo de fonte corrente/contagens descrevem suas etapas anteriores. Nenhum grant/dado/estado/JSON alterado no banco; nenhuma execução SQL, JVM/JPA/H2/build/rede/ambiente. Mutabilidade, efeitos temporais, guardas, SQL emitido e JPA BE14 só após freeze/tarefa separados; não é aceite/homologação.


Preparado em D19, somente em arquivos, a partir de V1–V5, models atuais e [procedimento de migrations](migrations/README.md). Nenhum usuário, role, GRANT ou conexão foi criado. Novos módulos acrescentarão somente os acessos comprovados pelos seus contratos/mapeamentos.

## Extensão V9 BE14 — proposta, sem concessão executada

Fonte [schema/API31](../docs/31-fechamento-contagem-e-contingencia.md) SHA996750EA… e [matriz V9](contratos/v9-matriz-cobertura.md). Resolver-cancelamento/vínculo histórico e efeitoRegistradoNoWms formalizados antes Java; JPA final ainda pendente. Conferir atualizações efetivas no freeze antes de converter proposta em permissões reais. Sem DELETE de histórico, UPDATE amplo, grant ou identidade nova nesta preparação.

Delta posterior52D607 formaliza encerramento futuro em vigencia_fim já existente de contrato/tabela/vínculo por Gestor/referência/versão/corte. Usar somente UPDATE previsto nesse campo, sem preço/default novo, apagar compromisso ou alterar memória finalizada. Auditoria CONTRATO_COBRANCA/TABELA_COBRANCA; nenhum objeto/ação administrativa novo ou concessão executada.

Complemento1D6413 formaliza SOLICITACAO_ENCERRAMENTO Gestor nos sete cadastros, incluindo serviço: situação/versão/alteração existentes, sem novo campo ou grant. Perfil/alcance antes replay, ATIVO→ENCERRAMENTO_PENDENTE e auditoria/UUID atômicos; não reativar INATIVO. Permissões por coluna já previstas não garantem essa transição/guardas, a conferir no freeze BE14.

| Objeto | SELECT/INSERT | UPDATE previsto para conferir no freeze |
| --- | --- | --- |
| contagem_estoque | Leitura/cadastro por unidade | versao, revisao_atual, impedimento, alterada_em |
| revisao_contagem | Snapshot original | situacao, aplicada_em, efeito_json; não limpar efeito histórico ou editar leitura original |
| carga_inicial | Estágio/contexto/referência | versao, revisao_atual, situacao, alterada_em, entrada_id; preparar uma vez/congelar origem/revisão; resolver-cancelamento conserva entrada ligada, sem editar resolução física anterior |
| revisao_carga_inicial | Revisão imutável | Nenhum |
| linha_contingencia | Identidade/conteúdo/hash/tempo originais | versao, situacao, pendencia, resultado_json, conciliada_em; identidade e JSON/hash preservados |
| dependencia_contingencia | Materialização quando existe referência/contexto | Nenhum; ausência no envelope original, sem linha fictícia |
| configuracao_aviso_validade | Contexto/configuração inicial | versao, dias_antecedencia, alterada_em, Gestor |
| resolucao_remanescente | Pedido/contexto/carga opcional/unidades/autor imutáveis | Nenhum; carga_id só na criação do pedido identificado |

Efeitos delegados conservam acessos já descritos de recebimento/unidade/conteúdo/permanência/ocupação/saída. V9 não concede apagar entrada ao cancelar, editar movimento/operação/auditoria ou reativar INATIVO. Origem/quantidade/revisões não recebem UPDATE para resolver ausência de prova. INSERT não substitui guarda de perfil e UPDATE por coluna não garante imutabilidade temporal: serviços conferem alcance, locks, prova, contexto/replay/auditoria atômicos.

DBA confere permissões efetivas/herdadas e SETs exigidos pelo índice filtrado na sessão real, sem db_owner/db_datawriter/DELETE/DDL/backup da aplicação. Diagnóstico seguro usa IDs técnicos/códigos e métricas autorizadas; não despejar JSON/provas/nomes/notas/valores. Falha de acesso/privacidade é informada, nunca contornada por raw ou outro perfil/env; nenhum segredo coletado.

## Identidades separadas

Consolidação V1–V9 em06/10:64 tabelas citadas nas matrizes; [diagnóstico final](../infra/verificar-preparacao-final-local.ps1) verifica cobertura textual/inventário/hashes, sem validar concessões. [Pacote integrado](../infra/preparacao-tecnica-final.md) exige DBA nomeado por Lucas/TI, confronto com DML realmente emitido/colunas @Version/flush, privilégios herdados e SETs dos índices filtrados. Guardas por estado/contexto são serviços, não GRANT condicional presumido. Sem grant/role/conta criados, UPDATE geral ou DELETE histórico; JPA BE14 só após freeze. Não ampliar privilégio para contornar falha.

| Identidade proposta | Alcance e uso | Limites |
| --- | --- | --- |
| Aplicação WMS | Conectar ao banco WMS definido; DML necessário nas tabelas listadas abaixo | Sem DDL, DELETE de histórico, backup/restauração, `sysadmin` ou `db_owner` |
| Migration WMS | Criar/alterar objetos do esquema `wms` e histórico Flyway, no alvo autorizado | Credencial fora do processo da aplicação; uso apenas em execução aprovada |
| DBA/recuperação | Preparar alvo, certificados, permissões, backup/restauração e ensaio isolado | Responsabilidade Lucas/TI/DBA; não é função do backend ou operador do armazém |
| Consulta técnica, se necessária | Leitura dos objetos/metadados aprovados para diagnóstico | Definir separadamente com DBA; não criar acesso geral por conveniência |

Não conceder `db_datawriter` como substituto da matriz: esse papel também permitiria alterações não necessárias. O DBA deve transformar a matriz em permissões explícitas e verificar as permissões efetivas, inclusive herdadas. Identidades e ambientes distintos não reutilizam nomes, bancos ou credenciais de outros projetos. Não há SQL de concessão pronto para executar sem alvo.

## Matriz preparada V1–V5

| Objetos | Leitura/entrada | Atualização necessária | Preservação |
| --- | --- | --- | --- |
| cliente, armazem, produto, embalagem, endereco | SELECT/INSERT | Campos mutáveis cadastrais, situação, capacidade quando aplicável, revisão e alteração | Identidades/vínculos imutáveis segundo JPA/contrato; nenhum DELETE |
| auditoria_cadastro | SELECT/INSERT | Nenhuma | Antes/depois, autor, correlação e histórico sem UPDATE/DELETE |
| pedido_entrada, nota_entrada, item_nota_entrada, chegada_recebimento | SELECT/INSERT | Estado/revisão, XML complementar compatível e estorno identificável, conforme model de cada tabela | Origem e fatos não são substituídos silenciosamente |
| item_chegada | SELECT/INSERT | Nenhuma | Fato físico original sem UPDATE/DELETE |
| entrada_conferida | SELECT/INSERT | Somente `unitizada_em` | Quantidade, FIFO e origem imutáveis |
| unidade_logistica, conteudo_unidade | SELECT/INSERT | Quantidade/composição nas operações responsáveis, localização, medidas, bloqueios, reserva, avaria, revisão e instantes mutáveis | UUID, origem e datas originais protegidos pelo mapeamento/serviço; sem DELETE |
| operacao_unidade, movimento_estoque | SELECT/INSERT | Nenhuma | Confirmação/histórico idempotente sem UPDATE/DELETE |
| conjunto_posicoes, ocupacao_endereco | SELECT/INSERT | Situação/revisão e ocupação conforme operação | Sem apagar conjunto ou linha de ocupação |
| pedido_saida | SELECT/INSERT | Somente `versao`, `situacao`, `alterado_em` | Cliente/armazém/referência e criação imutáveis |
| item_pedido_saida, operacao_saida | SELECT/INSERT | Nenhuma | Quantidades/identidade do pedido e confirmação idempotente preservadas |
| reserva_saida | SELECT/INSERT | Somente `situacao`, `encerrada_em` | UUID/quantidade/origem imutáveis; reversão e cancelamento preservam linhas |

As permissões por coluna são uma proposta de mínimo privilégio a ensaiar contra o SQL efetivamente emitido pelo JPA. Não presumir que o ORM atualiza apenas os campos alterados: considerar todas as colunas mutáveis e `@Version` do UPDATE gerado. Caso haja incompatibilidade, Cedro e Farol ajustam contrato/mapeamento ou a matriz de forma justificada; não conceder UPDATE geral para esconder o problema.

## Complemento V6 conforme schema 27

| Objetos | Leitura/entrada | Atualização necessária | Preservação |
| --- | --- | --- | --- |
| separacao_saida | SELECT/INSERT | `versao_unidade_lida`, `revisao_conteudo_lida`, `lida_em`, `separada_em`, `encerrada_em`, `situacao`, `origens_json`, `conjunto_origem_id` | Reserva imutável; ciclo anterior preservado no resultado/auditoria antes de atualizar o estado corrente |
| documento_saida | SELECT/INSERT | Somente `situacao`, `cancelado_em` | Identidade, metadados/hash e autorização registrada preservados |
| cobertura_documento_saida, retirada_saida, baixa_saida, devolucao_saida, fato_permanencia | SELECT/INSERT | Nenhuma | Fatos, composição/FIFO, vínculo de retorno e resultado integral sem UPDATE/DELETE |
| avaria_estoque | SELECT/INSERT | Somente `versao`, `responsabilidade`, `reconhecida_em`, `validada_por`, `resolvida_em`, `tratativa` | Unidade, ocorrência, quantidade/base, equivalência-base, ciclo, destino e relato imutáveis |

Reutiliza as atualizações físicas da unidade/composição/ocupação, estado/versão do pedido e encerramento da reserva já preparados; acrescenta UPDATE de `avaria_inicial_reparada` da unidade. `bloqueio_previo`, `equivalencia_base` e `ciclo_id` da avaria são somente INSERT/leitura, conforme JPA informado. A V6 não aplica concessões. Conferência final depende dos models e comandos de Cedro estáveis. Acesso a XML original, dados fiscais externos ou emissão não é permissão introduzida por este bloco.

FK não substitui alcance por cliente/armazém ou integridade entre item/pedido/SKU. Essas regras e as somas de estoque/reserva permanecem nos serviços, com locks/transações e auditoria. Acesso direto ao banco por navegador/coletor não faz parte do desenho.

## Conferência real futura

DBA confirma versão/collation e identidades de servidor/banco, remove permissões herdadas excessivas e ensaia acessos positivos e negativos. A aplicação deve conseguir suas operações e ser recusada em DDL, exclusão de histórico e alteração de confirmações. A identity de migration deve criar/evoluir o WMS sem autorização implícita para outro esquema.

Conferir as opções de conexão necessárias aos índices filtrados de V2/V5/V6, especialmente ANSI/ARITHABORT e NUMERIC_ROUNDABORT, conforme [Microsoft CREATE INDEX](https://learn.microsoft.com/en-us/sql/t-sql/statements/create-index-transact-sql?view=sql-server-ver17#required-set-options-for-filtered-indexes). O SET da migration não configura o pool JDBC da aplicação.

Registrar somente identidade lógica da função técnica, objetos/colunas aprovados, teste e resultado. Senhas, tokens e strings de conexão não entram na evidência. SQL real, inclusive consultas de permissões, permanece não executado nesta preparação.

## Complemento V7 conforme schema29 — conferência de anotações no freeze

| Objetos | Leitura/entrada | Atualização proposta | Preservação |
| --- | --- | --- | --- |
| cliente, armazem | SELECT/INSERT existentes | Novos complementos opcionais, versão e alteração; cidade/UF existentes do armazém no comando fiscal próprio | Código/documento fiscal e demais identidades protegidos; nada preenchido pela migration |
| referencia_fiscal_produto | SELECT/INSERT | Referências opcionais, fonte/conferência, versão e alteração | Produto/armazém/operação e criação preservados; sem motor tributário |
| importacao_endereco | SELECT/INSERT | Somente situação, confirmação e versão | Hash/layout/linhas/erros/usuário/criação preservados; confirma endereço sem alterar ocupação |
| operacao_administrativa | SELECT/INSERT | Nenhuma | UUID/hash/alvos/resultado/autor/instante imutáveis; sem DELETE |
| servico_cobranca | SELECT/INSERT | Descrição/situação/versão/alteração conforme comandos finais | Código, tipo/unidade e criação protegidos; enum INATIVO não autoriza inativação antecipada |
| tabela_cobranca | SELECT/INSERT | Encerramento de vigência/situação/versão/alteração | Contexto/identidade/preços usados históricos; detalhes de mutabilidade a conferir no JPA final |
| item_tabela_cobranca, servico_minimo_contrato | SELECT/INSERT | Nenhuma na proposta inicial | Não editar preço/item/serviço de período usado; inclusão substituta conforme contrato |
| vinculo_tabela_cliente, contrato_cobranca | SELECT/INSERT | Somente fim de vigência/versão/alteração na proposta inicial | Novo vínculo/contrato para parâmetros novos, sem reprecificar passado; validar comandos finais |
| fato_servico | SELECT/INSERT | Somente situação, anulado_em e versão | Chave, execução, origem/quantidade/categoria/base/autor preservados; anulação auditada |
| rateio_fato_servico, marco_financeiro_avaria | SELECT/INSERT | Nenhuma | Notas/cotas/marcos identificados e imutáveis; não alteram estoque |
| calculo_cobranca, memoria_diaria, memoria_servico | SELECT/INSERT | Nenhuma | Snapshot/memória/pendências e parâmetros originais; novo cálculo gera nova linha |

Nenhum GRANT aplicado. Em 06/10, os 15 models V7 e o ComplementoFiscal embutido foram lidos no freeze parser-final: `@Version` e colunas revisáveis correspondem à matriz; itens, serviços mínimos, rateios, marcos e memórias têm `updatable=false` nas colunas de conteúdo. `operacao_administrativa.recurso_id` é a exceção documental: sua anotação permite UPDATE por padrão, embora não haja setter ou comando atual que o atualize. Conservar SELECT/INSERT apenas nesse snapshot; não acrescentar GRANT de UPDATE. Cedro/Vigia podem conferir esse marcador de imutabilidade sem tratar a observação como UPDATE executado ou como novo schema.

A [reconferência p2-avaria-final](evidencias/d19-v7-2026-10-06-p2-avaria-final.md) confirmou models e schema sem alteração; a matriz e a observação sobre recurso_id permanecem iguais, sem nova concessão proposta. A comparação de anotações não observa o SQL emitido pelo ORM nem comprova permissões reais; esse ensaio continua externo. Integridade cruzada, perfil/Gestor, contexto, períodos, imutabilidade após uso e auditoria permanecem nos serviços. Não ampliar UPDATE geral para acomodar campo indevidamente mutável. Leituras financeiras não são concedidas a Operação apenas por alcance físico; essa restrição pertence à autorização do backend. Ver [procedimento V7](migrations/README.md#v7--cadastros-serviços-e-snapshots-de-cálculo).

## Complemento V8 BE13 — proposta antes do freeze JPA

Base atual: doc31 SHA3284E984538781CEB01C332DF64BDDF59325D1C809CF6114EF1774E367F90933, [transcrição](contratos/v8-schema-doc31.json), [matriz](contratos/v8-matriz-cobertura.md) e [procedimento](migrations/README.md#v8--fechamento-versões-e-registros-externos-be13). Onze tabelas112 colunas, sem GRANT/conexão/SQL observado. Final333523/523 e163/163 em arquivos confere86 atributos imutáveis, incluindo os quatro novos, e26 relações. Preparação469/470 e histórico490/132 preservados. [Parecer final](evidencias/d19-v8-2026-10-06-p2-origem-final.md) não aplica permissão nem aceita BE13.

Complemento cadastral31/INATIVO: a identidade de migration precisa futuramente alterar os seis CHECKs herdados de cliente/armazem/produto/embalagem/endereco/conjunto_posicoes, sob autorização/alvo próprios. A identidade da aplicação continua sem DDL. Não há novo GRANT/UPDATE por ampliar enum; alcance/Gestor/serviço recusam ativação/desativação comum e permitem somente consulta/resolução financeira histórica identificada. Inativação definitiva BE05 será conferida após BE14.

| Objetos | Leitura/entrada | Atualização proposta | Preservação |
| --- | --- | --- | --- |
| fechamento_cobranca | SELECT/INSERT | situacao, versao_atual, versao, alterado_em | Contexto/contrato/período/criação imutáveis; datas/dias não se mudam para evitar unicidade |
| versao_fechamento | SELECT/INSERT | situacao, estado_externo, decidida_em, decisor, motivo_decisao, versao | Identidade/cálculo/numero/saldo/natureza/memoria_json/conteudo_hash/criada_em sem UPDATE; metadados externos separados do snapshot |
| ajuste_fechamento | SELECT/INSERT | destino_fechamento_id somente VALIDADO por tratativa identificada; situacao, aplicado_versao_id, aplicado_em, versao | Tipo/tratativa_origem_id, origem/base/correção/valores/delta/hash/autor/evidência/criação imutáveis. APLICADO nunca muda destino/versão/data; regularização é novo efeito identificado, sem UPDATE em deltas antigos |
| dia_fechamento, fato_fechamento, ajuste_versao_fechamento | SELECT/INSERT | Nenhuma | Identidades/vínculos únicos históricos; sem DELETE para reabertura/rejeição |
| entrega_esl, confirmacao_externa_fechamento | SELECT/INSERT | Nenhuma | Tentativas/declarações/autor/fontes/datas imutáveis; entrega não comprova emissão |
| referencia_nfse | SELECT/INSERT | Nenhuma | Várias por versão, identidade emissor/referência única; não mudar/apagar referência para declarar cancelamento |
| resolucao_financeira_fechamento, tratativa_externa_fechamento | SELECT/INSERT | Nenhuma | Base anterior e dependencias_origem_json imutáveis junto do resultado/comprovação/lista de todas referências; múltiplas tratativas, nenhuma ação fiscal real |

Sem DELETE de histórico/versão/dia/fato, DDL ou privilégios globais para a aplicação. Identidade de migration pode criar onze tabelas/índices/CHECKs no alvo autorizado e ampliar domínios cumulativos; não usar seu poder no processo do backend. DML de auditoria/operacao_administrativa permanece somente SELECT/INSERT; movimento_estoque não recebe nova permissão financeira. As guardas de Gestor/cliente/armazém e replay pertencem ao serviço, não a GRANT por função da aplicação.

P2 formal do doc31 SHA F7ABE244AE6843321FF25E8296D8035E680F8378D1FEBEF268E21C1E7DCB1307: a permissão proposta de UPDATE em destino_fechamento_id não restringe por situação da linha. Serviço/Gestor impõem VALIDADO, destino posterior apto/contexto/versões otimistas e nova composição integral, com anterior/nova atribuição na auditoria/snapshots e todos os efeitos atômicos. APLICADOS são imutáveis; não introduzir trigger ou GRANT condicional não contratado. Vínculo antigo de ajuste_versao_fechamento permanece histórico identificado e não autoriza aplicar no destino antigo.

Guarda histórica E88FA/031 confronta destino/aplicação atuais dos vínculos recebidos. Complemento atual p2-origem exige também TODOS os deltas originados/todas versões e regularização identificada quando muda base comprometida. Identidade de migration precisará criar FK posterior/índice único filtrado e novos CHECKs; identidade da aplicação permanece sem DDL. Nenhum DELETE/UPDATE novo para snapshots/deltas aplicados, nem GRANT executado. SQL efetivamente emitido/permissões/opções de sessão do índice filtrado continuam sem ensaio. Serviço/Gestor impõem contexto/conjunto exato/valor/pendência; privilégio por coluna não prova guardas de linha.

Ensaio futuro do DBA deve conferir todas as colunas mutáveis e @Version no UPDATE realmente emitido, inclusive efeitos de flush, recusas de UPDATE nos snapshots/declarações e DELETE/DDL. Não ampliar UPDATE geral para esconder marcador errado. Tratativa não permite alterar hash/saldo nem mover ajuste já aplicado para outra versão; privilégio técnico não substitui revalidação. H2/anotações não comprovam permissões SQL Server.
