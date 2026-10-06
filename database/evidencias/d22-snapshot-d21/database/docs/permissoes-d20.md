# D20 — matriz de UPDATE por coluna

Fonte: [auditoria efetiva](../evidencias/d20-auditoria-final.json) e [SQL preparado pelo Hibernate](../../backend/evidencias/d20-hibernate-update-sql.json). Correspondência exata: 34 statements de UPDATE e64 entidades. Hibernate preparou os statements com SQLServerDialect sem executar JDBC; não é SQL observado no servidor nem GRANT validado. Herança, embedded, updatable=false e @Version incluídos.

SELECT/INSERT para as64 tabelas de domínio conforme operações existentes. UPDATE somente nas colunas da tabela abaixo, inclusive inalteradas presentes no statement estático. Nenhum DELETE/DDL/CONTROL/db_owner/sysadmin para aplicação, nenhum GRANT neste pacote. Acesso de diagnóstico ao histórico Flyway deve ser separado, não ampliado por conveniência.

| Tabela | Colunas no UPDATE preparado | Proteção |
| --- | --- | --- |
| cliente | `alterado_em`, `bairro`, `cep`, `cidade`, `complemento`, `contato_email`, `contato_nome`, `contato_telefone`, `faturamento_email`, `faturamento_referencia`, `inscricao_estadual`, `logradouro`, `nome`, `numero_endereco`, `pais`, `razao_social`, `situacao`, `uf`, `versao` | Demais colunas sem UPDATE |
| armazem | `alterado_em`, `bairro`, `cep`, `cidade`, `complemento`, `contato_email`, `contato_nome`, `contato_telefone`, `inscricao_estadual`, `logradouro`, `nome`, `numero_endereco`, `pais`, `razao_social`, `situacao`, `uf`, `versao` | Demais colunas sem UPDATE |
| produto | `alterado_em`, `descricao`, `situacao`, `versao` | Demais colunas sem UPDATE |
| embalagem | `alterado_em`, `descricao`, `situacao`, `versao` | Demais colunas sem UPDATE |
| endereco | `alterado_em`, `altura_metros`, `capacidade_peso_kg`, `descricao`, `empilhamento_maximo`, `largura_metros`, `profundidade_metros`, `situacao`, `tipo_unidade_permitido`, `versao` | Demais colunas sem UPDATE |
| auditoria_cadastro | Nenhuma | Histórico somente INSERT/leitura |
| pedido_entrada | `alterado_em`, `efetivado_em`, `motivo_conclusao`, `situacao`, `versao` | Demais colunas sem UPDATE |
| nota_entrada | `chave_acesso`, `xml_hash`, `xml_original` | Demais colunas sem UPDATE |
| item_nota_entrada | `valor_mercadoria` | Demais colunas sem UPDATE |
| chegada_recebimento | `estornada_em`, `estornada_por`, `motivo_estorno` | Demais colunas sem UPDATE |
| item_chegada | Nenhuma | Histórico somente INSERT/leitura |
| entrada_conferida | `unitizada_em` | Demais colunas sem UPDATE |
| unidade_logistica | `alterada_em`, `altura_metros`, `ativa`, `avaria_inicial_reparada`, `avaria_posterior`, `bloqueada`, `conjunto_atual_id`, `empilhamento`, `inicio_armazenagem_em`, `largura_metros`, `peso_kg`, `posicoes_equivalentes`, `posicoes_necessarias`, `primeiro_enderecamento_em`, `profundidade_metros`, `quantidade`, `reserva_saida_id`, `revisao_conteudo`, `tipo_localizacao`, `versao` | Demais colunas sem UPDATE |
| conteudo_unidade | `quantidade` | Demais colunas sem UPDATE |
| operacao_unidade | Nenhuma | Histórico somente INSERT/leitura |
| conjunto_posicoes | `alterado_em`, `situacao`, `versao` | Demais colunas sem UPDATE |
| ocupacao_endereco | `unidade_id` | Demais colunas sem UPDATE |
| movimento_estoque | Nenhuma | Histórico somente INSERT/leitura |
| pedido_saida | `alterado_em`, `situacao`, `versao` | Demais colunas sem UPDATE |
| item_pedido_saida | Nenhuma | Histórico somente INSERT/leitura |
| reserva_saida | `encerrada_em`, `situacao` | Demais colunas sem UPDATE |
| operacao_saida | Nenhuma | Histórico somente INSERT/leitura |
| separacao_saida | `conjunto_origem_id`, `encerrada_em`, `lida_em`, `origens_json`, `revisao_conteudo_lida`, `separada_em`, `situacao`, `versao_unidade_lida` | Demais colunas sem UPDATE |
| documento_saida | `cancelado_em`, `situacao` | Demais colunas sem UPDATE |
| cobertura_documento_saida | Nenhuma | Histórico somente INSERT/leitura |
| retirada_saida | Nenhuma | Histórico somente INSERT/leitura |
| baixa_saida | Nenhuma | Histórico somente INSERT/leitura |
| devolucao_saida | Nenhuma | Histórico somente INSERT/leitura |
| avaria_estoque | `reconhecida_em`, `resolvida_em`, `responsabilidade`, `tratativa`, `validada_por`, `versao` | Demais colunas sem UPDATE |
| fato_permanencia | Nenhuma | Histórico somente INSERT/leitura |
| referencia_fiscal_produto | `aliquota_icms`, `aliquota_ipi`, `alterada_em`, `cest`, `cfop`, `conferida_em`, `conferida_por`, `enquadramento`, `fonte`, `ncm`, `versao` | Demais colunas sem UPDATE |
| importacao_endereco | `confirmada_em`, `situacao`, `versao` | Demais colunas sem UPDATE |
| operacao_administrativa | Nenhuma | Histórico somente INSERT/leitura |
| servico_cobranca | `alterado_em`, `descricao`, `situacao`, `versao` | Demais colunas sem UPDATE |
| tabela_cobranca | `alterada_em`, `situacao`, `versao`, `vigencia_fim` | Demais colunas sem UPDATE |
| item_tabela_cobranca | Nenhuma | Histórico somente INSERT/leitura |
| vinculo_tabela_cliente | `alterado_em`, `versao`, `vigencia_fim` | Demais colunas sem UPDATE |
| contrato_cobranca | `alterada_em`, `versao`, `vigencia_fim` | Demais colunas sem UPDATE |
| servico_minimo_contrato | Nenhuma | Histórico somente INSERT/leitura |
| fato_servico | `anulado_em`, `situacao`, `versao` | Demais colunas sem UPDATE |
| rateio_fato_servico | Nenhuma | Histórico somente INSERT/leitura |
| marco_financeiro_avaria | Nenhuma | Histórico somente INSERT/leitura |
| calculo_cobranca | Nenhuma | Histórico somente INSERT/leitura |
| memoria_diaria | Nenhuma | Histórico somente INSERT/leitura |
| memoria_servico | Nenhuma | Histórico somente INSERT/leitura |
| fechamento_cobranca | `alterado_em`, `situacao`, `versao`, `versao_atual` | Demais colunas sem UPDATE |
| versao_fechamento | `decidida_em`, `decisor`, `estado_externo`, `motivo_decisao`, `situacao`, `versao` | Demais colunas sem UPDATE |
| dia_fechamento | Nenhuma | Histórico somente INSERT/leitura |
| fato_fechamento | Nenhuma | Histórico somente INSERT/leitura |
| ajuste_fechamento | `aplicado_em`, `aplicado_versao_id`, `destino_fechamento_id`, `situacao`, `versao` | Demais colunas sem UPDATE |
| ajuste_versao_fechamento | Nenhuma | Histórico somente INSERT/leitura |
| entrega_esl | Nenhuma | Histórico somente INSERT/leitura |
| confirmacao_externa_fechamento | Nenhuma | Histórico somente INSERT/leitura |
| referencia_nfse | Nenhuma | Histórico somente INSERT/leitura |
| resolucao_financeira_fechamento | Nenhuma | Histórico somente INSERT/leitura |
| tratativa_externa_fechamento | Nenhuma | Histórico somente INSERT/leitura |
| contagem_estoque | `alterada_em`, `impedimento`, `revisao_atual`, `versao` | Demais colunas sem UPDATE |
| revisao_contagem | `aplicada_em`, `efeito_json`, `situacao` | Demais colunas sem UPDATE |
| carga_inicial | `alterada_em`, `entrada_id`, `revisao_atual`, `situacao`, `versao` | Demais colunas sem UPDATE |
| revisao_carga_inicial | Nenhuma | Histórico somente INSERT/leitura |
| linha_contingencia | `conciliada_em`, `pendencia`, `resultado_json`, `situacao`, `versao` | Demais colunas sem UPDATE |
| dependencia_contingencia | Nenhuma | Histórico somente INSERT/leitura |
| configuracao_aviso_validade | `alterada_em`, `dias_antecedencia`, `versao` | Demais colunas sem UPDATE |
| resolucao_remanescente | Nenhuma | Histórico somente INSERT/leitura |

As permissões por coluna não impõem a situação da linha nem alcance por cliente/armazém. Services mantêm permissões do operador, locks, contexto, revisão e atomicidade. Na mudança de contrato ou versão Hibernate, regenerar/confrontar o SQL antes de implantar permissões. Não conceder UPDATE geral para resolver incompatibilidade.

Correções D20 de Cedro: item_chegada e operacao_administrativa sem UPDATE; chegada somente estorno; pedido entrada somente situação/revisão/alteração/efetivação/motivo; nota somente XML/hash/chave; item nota somente valor_mercadoria. Origem/quantidade/data/identidades protegidas. @DynamicUpdate não introduzido.

[Microsoft GRANT](https://learn.microsoft.com/en-us/sql/t-sql/statements/grant-transact-sql?view=sql-server-ver17): permissões herdadas e exceções por coluna devem ser auditadas; DENY na tabela não garante sobrepor GRANT em coluna. [Hibernate](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#pc-managed-state): statement estático pode incluir todas as colunas updatable. Ensaio positivo/negativo e comandos efetivos no SQL Server continuam pendentes.
