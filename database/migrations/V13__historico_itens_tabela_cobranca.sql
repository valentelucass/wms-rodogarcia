-- TAB-EDIT01: itens substituídos permanecem ligados às memórias financeiras.
-- Executar exclusivamente pelo fluxo de migrations WMS com alvo/histórico conferidos.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

ALTER TABLE wms.item_tabela_cobranca ADD ativo bit NOT NULL
    CONSTRAINT df_item_tabela_cobranca_ativo DEFAULT (1) WITH VALUES;
ALTER TABLE wms.item_tabela_cobranca DROP CONSTRAINT uk_item_tabela_cobranca_1;
CREATE UNIQUE INDEX uk_item_tabela_cobranca_ativo
    ON wms.item_tabela_cobranca(tabela_id, servico_id, categoria) WHERE ativo = 1;

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_financeira_tipo_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_financeira_tipo_acao CHECK (
    (acao NOT IN ('COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA')
        OR (acao = 'COMPLEMENTO_FISCAL' AND tipo IN ('CLIENTE','ARMAZEM'))
        OR (acao = 'REFERENCIA_FISCAL' AND tipo IN ('PRODUTO'))
        OR (acao = 'PREVIA_ENDERECOS' AND tipo IN ('ARMAZEM'))
        OR (acao = 'IMPORTACAO_ENDERECOS' AND tipo IN ('ARMAZEM'))
        OR (acao = 'ENCERRAMENTO_VIGENCIA' AND tipo IN ('TABELA_COBRANCA','CONTRATO_COBRANCA'))
        OR (acao = 'CONFIGURACAO' AND tipo IN ('CONTRATO_COBRANCA'))
        OR (acao = 'REGISTRO_SERVICO' AND tipo IN ('FATO_SERVICO'))
        OR (acao = 'ANULACAO_SERVICO' AND tipo IN ('FATO_SERVICO'))
        OR (acao = 'MARCO_AVARIA' AND tipo IN ('AVARIA_FINANCEIRA'))
        OR (acao = 'CALCULO' AND tipo IN ('CALCULO_COBRANCA'))
        OR (acao = 'VINCULO_TABELA' AND tipo IN ('CONTRATO_COBRANCA')))
    AND (tipo NOT IN ('SERVICO_COBRANCA','TABELA_COBRANCA','CONTRATO_COBRANCA','FATO_SERVICO','AVARIA_FINANCEIRA','CALCULO_COBRANCA')
        OR (tipo = 'SERVICO_COBRANCA' AND acao IN ('CRIACAO','INATIVACAO_DEFINITIVA','SOLICITACAO_ENCERRAMENTO'))
        OR (tipo = 'TABELA_COBRANCA' AND acao IN ('CRIACAO','ALTERACAO','ENCERRAMENTO_VIGENCIA'))
        OR (tipo = 'CONTRATO_COBRANCA' AND acao IN ('CONFIGURACAO','VINCULO_TABELA','ENCERRAMENTO_VIGENCIA'))
        OR (tipo = 'FATO_SERVICO' AND acao IN ('REGISTRO_SERVICO','ANULACAO_SERVICO'))
        OR (tipo = 'AVARIA_FINANCEIRA' AND acao IN ('MARCO_AVARIA'))
        OR (tipo = 'CALCULO_COBRANCA' AND acao IN ('CALCULO')))
);
