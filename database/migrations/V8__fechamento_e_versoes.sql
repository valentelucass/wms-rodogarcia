-- V8 preparada somente em arquivos, conforme doc31 BE13 refinado antes do Java.
-- Sem execucao/conexao/JPA/Hibernate/H2/build, DML, dados/precos/fiscal reais ou BE14 presumido.
-- V1-V7, historico e snapshots preservados. FK isolada nao comprova contexto/ciclo.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

CREATE TABLE wms.fechamento_cobranca (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_fechamento_cobranca PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_fechamento_cobranca_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_fechamento_cobranca_armazem REFERENCES wms.armazem(id),
    contrato_id bigint NOT NULL CONSTRAINT fk_fechamento_cobranca_contrato REFERENCES wms.contrato_cobranca(id),
    periodo_inicio date NOT NULL,
    periodo_fim date NOT NULL,
    situacao varchar(24) NOT NULL,
    versao_atual int NOT NULL,
    versao bigint NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    CONSTRAINT uk_fechamento_contexto_inicio UNIQUE (cliente_id,armazem_id,periodo_inicio),
    CONSTRAINT ck_fechamento_periodo CHECK (
        periodo_fim > periodo_inicio
    ),
    CONSTRAINT ck_fechamento_situacao CHECK (
        situacao IN ('EM_REVISAO','APROVADO','REJEITADO','EMITIDO','FINALIZADO_SEM_EMISSAO','CONFLITO_EXTERNO')
    ),
    CONSTRAINT ck_fechamento_versoes CHECK (
        versao_atual >= 1 AND versao >= 0
    )
);

CREATE TABLE wms.versao_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_versao_fechamento PRIMARY KEY,
    fechamento_id bigint NOT NULL CONSTRAINT fk_versao_fechamento_fechamento REFERENCES wms.fechamento_cobranca(id),
    calculo_id bigint NOT NULL CONSTRAINT fk_versao_fechamento_calculo REFERENCES wms.calculo_cobranca(id),
    numero int NOT NULL,
    situacao varchar(24) NOT NULL,
    estado_externo varchar(24) NOT NULL,
    saldo decimal(19,2) NULL,
    natureza varchar(8) NULL,
    memoria_json nvarchar(max) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    criada_em datetime2(6) NOT NULL,
    decidida_em datetime2(6) NULL,
    decisor nvarchar(200) NULL,
    motivo_decisao nvarchar(500) NULL,
    versao bigint NOT NULL,
    CONSTRAINT uk_versao_fechamento_numero UNIQUE (fechamento_id,numero),
    CONSTRAINT ck_versao_fechamento_numeros CHECK (
        numero >= 1 AND versao >= 0
    ),
    CONSTRAINT ck_versao_fechamento_situacao CHECK (
        situacao IN ('PENDENTE_REVISAO','APROVADA','REJEITADA','SUPERADA','EMITIDA','FINALIZADA_SEM_EMISSAO')
    ),
    CONSTRAINT ck_versao_fechamento_externo CHECK (
        estado_externo IN ('NAO_ENVIADO','DESCONHECIDO','NAO_EMITIDO_CONFIRMADO','EMITIDO')
    ),
    CONSTRAINT ck_versao_fechamento_saldo CHECK (
        (saldo IS NULL AND natureza IS NULL) OR (saldo IS NOT NULL AND natureza IS NOT NULL AND ((saldo > 0 AND natureza = 'DEBITO') OR (saldo = 0 AND natureza = 'ZERO') OR (saldo < 0 AND natureza = 'CREDITO')))
    ),
    CONSTRAINT ck_versao_fechamento_saldo_final CHECK (
        situacao NOT IN ('APROVADA','EMITIDA','FINALIZADA_SEM_EMISSAO') OR (saldo IS NOT NULL AND natureza IS NOT NULL)
    ),
    CONSTRAINT ck_versao_fechamento_decisao CHECK (
        ((decidida_em IS NULL AND decisor IS NULL AND motivo_decisao IS NULL) OR (decidida_em IS NOT NULL AND decisor IS NOT NULL AND motivo_decisao IS NOT NULL)) AND (situacao NOT IN ('APROVADA','REJEITADA','EMITIDA','FINALIZADA_SEM_EMISSAO') OR (decidida_em IS NOT NULL AND decisor IS NOT NULL AND motivo_decisao IS NOT NULL)) AND (situacao <> 'PENDENTE_REVISAO' OR (decidida_em IS NULL AND decisor IS NULL AND motivo_decisao IS NULL))
    ),
    CONSTRAINT ck_versao_fechamento_json CHECK (
        ISJSON(memoria_json) = 1
    ),
    CONSTRAINT ck_versao_fechamento_hash CHECK (
        LEN(conteudo_hash) = 64
    )
);

CREATE TABLE wms.dia_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_dia_fechamento PRIMARY KEY,
    fechamento_id bigint NOT NULL CONSTRAINT fk_dia_fechamento_fechamento REFERENCES wms.fechamento_cobranca(id),
    cliente_id bigint NOT NULL CONSTRAINT fk_dia_fechamento_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_dia_fechamento_armazem REFERENCES wms.armazem(id),
    data date NOT NULL,
    CONSTRAINT uk_dia_fechamento_contexto_data UNIQUE (cliente_id,armazem_id,data),
    CONSTRAINT uk_dia_fechamento_data UNIQUE (fechamento_id,data)
);

CREATE TABLE wms.fato_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_fato_fechamento PRIMARY KEY,
    fechamento_id bigint NOT NULL CONSTRAINT fk_fato_fechamento_fechamento REFERENCES wms.fechamento_cobranca(id),
    fato_id bigint NOT NULL CONSTRAINT fk_fato_fechamento_fato REFERENCES wms.fato_servico(id),
    registrado_em datetime2(6) NOT NULL,
    CONSTRAINT uk_fato_fechamento_fato UNIQUE (fato_id)
);

CREATE TABLE wms.ajuste_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_ajuste_fechamento PRIMARY KEY,
    origem_versao_id bigint NOT NULL CONSTRAINT fk_ajuste_fechamento_origem_versao REFERENCES wms.versao_fechamento(id),
    destino_fechamento_id bigint NOT NULL CONSTRAINT fk_ajuste_fechamento_destino_fechamento REFERENCES wms.fechamento_cobranca(id),
    calculo_base_id bigint NOT NULL CONSTRAINT fk_ajuste_fechamento_calculo_base REFERENCES wms.calculo_cobranca(id),
    calculo_corrigido_id bigint NOT NULL CONSTRAINT fk_ajuste_fechamento_calculo_corrigido REFERENCES wms.calculo_cobranca(id),
    aplicado_versao_id bigint NULL CONSTRAINT fk_ajuste_fechamento_aplicado_versao REFERENCES wms.versao_fechamento(id),
    tipo varchar(24) NOT NULL CONSTRAINT df_ajuste_fechamento_tipo DEFAULT 'CORRECAO_CALCULO',
    tratativa_origem_id bigint NULL,
    hash_correcao varchar(64) NOT NULL,
    valor_base decimal(19,2) NOT NULL,
    valor_corrigido decimal(19,2) NOT NULL,
    diferenca decimal(19,2) NOT NULL,
    situacao varchar(16) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    evidencia nvarchar(500) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    registrado_em datetime2(6) NOT NULL,
    aplicado_em datetime2(6) NULL,
    versao bigint NOT NULL,
    CONSTRAINT uk_ajuste_fechamento_origem_hash UNIQUE (origem_versao_id,hash_correcao),
    CONSTRAINT ck_ajuste_fechamento_valores CHECK (
        valor_base >= 0 AND valor_corrigido >= 0 AND diferenca = valor_corrigido - valor_base
    ),
    CONSTRAINT ck_ajuste_fechamento_versao CHECK (
        versao >= 0
    ),
    CONSTRAINT ck_ajuste_fechamento_situacao CHECK (
        situacao IN ('VALIDADO','APLICADO')
    ),
    CONSTRAINT ck_ajuste_fechamento_aplicacao CHECK (
        (situacao = 'VALIDADO' AND aplicado_em IS NULL AND aplicado_versao_id IS NULL) OR (situacao = 'APLICADO' AND aplicado_em IS NOT NULL AND aplicado_versao_id IS NOT NULL)
    ),
    CONSTRAINT ck_ajuste_fechamento_hash CHECK (
        LEN(hash_correcao) = 64
    ),
    CONSTRAINT ck_ajuste_fechamento_tipo CHECK (
        tipo IN ('CORRECAO_CALCULO','REGULARIZACAO_ORIGEM')
    ),
    CONSTRAINT ck_ajuste_fechamento_tratativa_origem CHECK (
        (tipo = 'CORRECAO_CALCULO' AND tratativa_origem_id IS NULL) OR (tipo = 'REGULARIZACAO_ORIGEM' AND tratativa_origem_id IS NOT NULL)
    )
);

CREATE TABLE wms.ajuste_versao_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_ajuste_versao_fechamento PRIMARY KEY,
    versao_id bigint NOT NULL CONSTRAINT fk_ajuste_versao_fechamento_versao REFERENCES wms.versao_fechamento(id),
    ajuste_id bigint NOT NULL CONSTRAINT fk_ajuste_versao_fechamento_ajuste REFERENCES wms.ajuste_fechamento(id),
    CONSTRAINT uk_ajuste_versao_fechamento UNIQUE (versao_id,ajuste_id)
);

CREATE TABLE wms.entrega_esl (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_entrega_esl PRIMARY KEY,
    versao_id bigint NOT NULL CONSTRAINT fk_entrega_esl_versao REFERENCES wms.versao_fechamento(id),
    sequencia int NOT NULL,
    layout_versao int NOT NULL,
    arquivo_hash varchar(64) NOT NULL,
    destino_referencia nvarchar(200) NOT NULL,
    entregue_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT uk_entrega_esl_versao_sequencia UNIQUE (versao_id,sequencia),
    CONSTRAINT ck_entrega_esl_sequencia_layout CHECK (
        sequencia >= 1 AND layout_versao >= 1
    ),
    CONSTRAINT ck_entrega_esl_hash CHECK (
        LEN(arquivo_hash) = 64
    )
);

CREATE TABLE wms.confirmacao_externa_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_confirmacao_externa_fechamento PRIMARY KEY,
    versao_id bigint NOT NULL CONSTRAINT fk_confirmacao_externa_fechamento_versao REFERENCES wms.versao_fechamento(id),
    entrega_id bigint NULL CONSTRAINT fk_confirmacao_externa_fechamento_entrega REFERENCES wms.entrega_esl(id),
    situacao varchar(24) NOT NULL,
    fonte nvarchar(500) NOT NULL,
    confirmada_por nvarchar(200) NOT NULL,
    confirmada_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT ck_confirmacao_externa_fechamento_situacao CHECK (
        situacao IN ('NAO_EMITIDO_CONFIRMADO')
    )
);

CREATE TABLE wms.referencia_nfse (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_referencia_nfse PRIMARY KEY,
    versao_id bigint NOT NULL CONSTRAINT fk_referencia_nfse_versao REFERENCES wms.versao_fechamento(id),
    emissor_documento varchar(20) NOT NULL,
    referencia_externa varchar(200) NOT NULL,
    numero varchar(80) NULL,
    serie varchar(20) NULL,
    emitida_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    fonte nvarchar(500) NOT NULL,
    conferida_por nvarchar(200) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT uk_referencia_nfse_identidade UNIQUE (emissor_documento,referencia_externa)
);

CREATE TABLE wms.resolucao_financeira_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_resolucao_financeira_fechamento PRIMARY KEY,
    versao_id bigint NOT NULL CONSTRAINT fk_resolucao_financeira_fechamento_versao REFERENCES wms.versao_fechamento(id),
    tipo varchar(24) NOT NULL,
    referencia_externa nvarchar(200) NOT NULL,
    confirmada_por nvarchar(200) NOT NULL,
    confirmada_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    fonte nvarchar(500) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT uk_resolucao_financeira_fechamento_versao UNIQUE (versao_id),
    CONSTRAINT ck_resolucao_financeira_fechamento_tipo CHECK (
        tipo IN ('SALDO_CREDOR','SALDO_ZERO')
    )
);

CREATE TABLE wms.tratativa_externa_fechamento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_tratativa_externa_fechamento PRIMARY KEY,
    fechamento_id bigint NOT NULL CONSTRAINT fk_tratativa_externa_fechamento_fechamento REFERENCES wms.fechamento_cobranca(id),
    versao_resultado_id bigint NOT NULL CONSTRAINT fk_tratativa_externa_fechamento_versao_resultado REFERENCES wms.versao_fechamento(id),
    versao_base_anterior_id bigint NULL CONSTRAINT fk_tratativa_externa_fechamento_versao_base_anterior REFERENCES wms.versao_fechamento(id),
    dependencias_origem_json nvarchar(max) NULL,
    resultado varchar(24) NOT NULL,
    referencias_json nvarchar(max) NOT NULL,
    fonte nvarchar(500) NOT NULL,
    conferida_por nvarchar(200) NOT NULL,
    conferida_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT ck_tratativa_externa_fechamento_resultado CHECK (
        resultado IN ('EMITIDO','NAO_EMITIDO_CONFIRMADO')
    ),
    CONSTRAINT ck_tratativa_externa_fechamento_json CHECK (
        ISJSON(referencias_json) = 1
    ),
    CONSTRAINT ck_tratativa_externa_fechamento_base_origem CHECK (
        (versao_base_anterior_id IS NULL AND dependencias_origem_json IS NULL) OR (versao_base_anterior_id IS NOT NULL AND dependencias_origem_json IS NOT NULL)
    ),
    CONSTRAINT ck_tratativa_externa_fechamento_dependencias_json CHECK (
        dependencias_origem_json IS NULL OR ISJSON(dependencias_origem_json) = 1
    )
);

-- Complemento formal p2-origem: resolve o ciclo de FK somente depois das tabelas.
ALTER TABLE wms.ajuste_fechamento WITH CHECK ADD CONSTRAINT fk_ajuste_fechamento_tratativa_origem
    FOREIGN KEY (tratativa_origem_id) REFERENCES wms.tratativa_externa_fechamento(id);

-- Indices de FK/consulta; UNIQUE cobre os demais vinculos como primeiro campo.
CREATE INDEX ix_fechamento_armazem_contexto ON wms.fechamento_cobranca(armazem_id,cliente_id,periodo_inicio);
CREATE INDEX ix_fechamento_contrato ON wms.fechamento_cobranca(contrato_id,id);
CREATE INDEX ix_versao_fechamento_calculo ON wms.versao_fechamento(calculo_id,id);
CREATE INDEX ix_dia_fechamento_armazem ON wms.dia_fechamento(armazem_id,data,cliente_id);
CREATE INDEX ix_fato_fechamento_fechamento ON wms.fato_fechamento(fechamento_id,fato_id);
CREATE INDEX ix_ajuste_fechamento_destino ON wms.ajuste_fechamento(destino_fechamento_id,situacao,id);
CREATE INDEX ix_ajuste_fechamento_base ON wms.ajuste_fechamento(calculo_base_id,id);
CREATE INDEX ix_ajuste_fechamento_corrigido ON wms.ajuste_fechamento(calculo_corrigido_id,id);
CREATE INDEX ix_ajuste_fechamento_aplicado ON wms.ajuste_fechamento(aplicado_versao_id,id);
CREATE INDEX ix_ajuste_versao_fechamento_ajuste ON wms.ajuste_versao_fechamento(ajuste_id,versao_id);
CREATE INDEX ix_confirmacao_externa_fechamento_versao ON wms.confirmacao_externa_fechamento(versao_id,confirmada_em,id);
CREATE INDEX ix_confirmacao_externa_fechamento_entrega ON wms.confirmacao_externa_fechamento(entrega_id,id);
CREATE INDEX ix_referencia_nfse_versao ON wms.referencia_nfse(versao_id,id);
CREATE INDEX ix_tratativa_externa_fechamento_fechamento ON wms.tratativa_externa_fechamento(fechamento_id,id);
CREATE INDEX ix_tratativa_externa_fechamento_versao ON wms.tratativa_externa_fechamento(versao_resultado_id,id);
CREATE INDEX ix_tratativa_externa_fechamento_base_anterior ON wms.tratativa_externa_fechamento(versao_base_anterior_id,id);
CREATE UNIQUE INDEX ux_ajuste_fechamento_tratativa_origem ON wms.ajuste_fechamento(tratativa_origem_id)
    WHERE tratativa_origem_id IS NOT NULL;

-- Complemento formal doc31: INATIVO historico, sem inativacao definitiva BE05.
-- Mantem VARCHAR24/NOT NULL e os estados anteriores; nao altera dados.
ALTER TABLE wms.cliente DROP CONSTRAINT ck_cliente_situacao;
ALTER TABLE wms.cliente WITH CHECK ADD CONSTRAINT ck_cliente_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);
ALTER TABLE wms.armazem DROP CONSTRAINT ck_armazem_situacao;
ALTER TABLE wms.armazem WITH CHECK ADD CONSTRAINT ck_armazem_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);
ALTER TABLE wms.produto DROP CONSTRAINT ck_produto_situacao;
ALTER TABLE wms.produto WITH CHECK ADD CONSTRAINT ck_produto_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);
ALTER TABLE wms.embalagem DROP CONSTRAINT ck_embalagem_situacao;
ALTER TABLE wms.embalagem WITH CHECK ADD CONSTRAINT ck_embalagem_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);
ALTER TABLE wms.endereco DROP CONSTRAINT ck_endereco_situacao;
ALTER TABLE wms.endereco WITH CHECK ADD CONSTRAINT ck_endereco_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);
ALTER TABLE wms.conjunto_posicoes DROP CONSTRAINT ck_conjunto_situacao;
ALTER TABLE wms.conjunto_posicoes WITH CHECK ADD CONSTRAINT ck_conjunto_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);

ALTER TABLE wms.operacao_administrativa DROP CONSTRAINT ck_operacao_administrativa_tipo;
ALTER TABLE wms.operacao_administrativa WITH CHECK ADD CONSTRAINT ck_operacao_administrativa_tipo CHECK (
    tipo IN ('CRIACAO_SERVICO','CRIACAO_TABELA','CRIACAO','COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA','PREPARACAO_FECHAMENTO','APROVACAO_FECHAMENTO','REJEICAO_FECHAMENTO','REABERTURA_FECHAMENTO','ENTREGA_ESL','CONFIRMACAO_EXTERNA','REFERENCIA_NFSE','RESOLUCAO_FINANCEIRA','AJUSTE_FECHAMENTO','TRATATIVA_EXTERNA')
);

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_tipo CHECK (
    tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','PEDIDO_ENTRADA','CONJUNTO_POSICOES','PEDIDO_SAIDA','SERVICO_COBRANCA','TABELA_COBRANCA','CONTRATO_COBRANCA','FATO_SERVICO','AVARIA_FINANCEIRA','CALCULO_COBRANCA','FECHAMENTO_COBRANCA')
);

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_acao CHECK (
    acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO','NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA','ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS','CONFIGURAR_CAPACIDADE','ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE','CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE','LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA','CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA','RETORNO_INTERNO','DEVOLUCAO_SAIDA','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA','COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA','PREPARACAO_FECHAMENTO','APROVACAO_FECHAMENTO','REJEICAO_FECHAMENTO','REABERTURA_FECHAMENTO','ENTREGA_ESL','CONFIRMACAO_EXTERNA','REFERENCIA_NFSE','RESOLUCAO_FINANCEIRA','AJUSTE_FECHAMENTO','TRATATIVA_EXTERNA')
);
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_fechamento_tipo_acao CHECK (
    (acao NOT IN ('PREPARACAO_FECHAMENTO','APROVACAO_FECHAMENTO','REJEICAO_FECHAMENTO','REABERTURA_FECHAMENTO','ENTREGA_ESL','CONFIRMACAO_EXTERNA','REFERENCIA_NFSE','RESOLUCAO_FINANCEIRA','AJUSTE_FECHAMENTO','TRATATIVA_EXTERNA') OR tipo = 'FECHAMENTO_COBRANCA') AND (tipo <> 'FECHAMENTO_COBRANCA' OR acao IN ('PREPARACAO_FECHAMENTO','APROVACAO_FECHAMENTO','REJEICAO_FECHAMENTO','REABERTURA_FECHAMENTO','ENTREGA_ESL','CONFIRMACAO_EXTERNA','REFERENCIA_NFSE','RESOLUCAO_FINANCEIRA','AJUSTE_FECHAMENTO','TRATATIVA_EXTERNA'))
);

-- ck_auditoria_saida_tipo_acao da V6 e ck_auditoria_financeira_tipo_acao da V7 permanecem.
-- Nenhuma acao financeira grava movimento_estoque. Snapshot/hash nao sao atualizados aqui.
