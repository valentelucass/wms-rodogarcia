-- V7 preparada somente em arquivos, conforme doc29 antes do Java; nao aplicada.
-- Sem preco/fiscal/corte real, seeds, defaults historicos, DML, grants ou movimento fisico.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

ALTER TABLE wms.cliente ADD
    razao_social nvarchar(160) NULL,
    inscricao_estadual varchar(30) NULL,
    logradouro nvarchar(160) NULL,
    numero_endereco varchar(20) NULL,
    complemento nvarchar(80) NULL,
    bairro nvarchar(80) NULL,
    cidade nvarchar(100) NULL,
    uf varchar(2) NULL,
    cep varchar(16) NULL,
    pais varchar(2) NULL,
    contato_nome nvarchar(120) NULL,
    contato_email varchar(160) NULL,
    contato_telefone varchar(30) NULL,
    faturamento_email varchar(160) NULL,
    faturamento_referencia nvarchar(500) NULL;

ALTER TABLE wms.armazem ADD
    razao_social nvarchar(160) NULL,
    inscricao_estadual varchar(30) NULL,
    logradouro nvarchar(160) NULL,
    numero_endereco varchar(20) NULL,
    complemento nvarchar(80) NULL,
    bairro nvarchar(80) NULL,
    cep varchar(16) NULL,
    pais varchar(2) NULL,
    contato_nome nvarchar(120) NULL,
    contato_email varchar(160) NULL,
    contato_telefone varchar(30) NULL;

GO

CREATE TABLE wms.referencia_fiscal_produto (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_referencia_fiscal_produto PRIMARY KEY,
    produto_id bigint NOT NULL CONSTRAINT fk_referencia_fiscal_produto_produto REFERENCES wms.produto(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_referencia_fiscal_produto_armazem REFERENCES wms.armazem(id),
    operacao varchar(40) NOT NULL,
    ncm varchar(8) NULL,
    cfop varchar(4) NULL,
    cest varchar(7) NULL,
    enquadramento nvarchar(500) NULL,
    aliquota_icms decimal(9,6) NULL,
    aliquota_ipi decimal(9,6) NULL,
    fonte nvarchar(200) NOT NULL,
    conferida_por nvarchar(200) NULL,
    conferida_em datetime2(6) NULL,
    versao bigint NOT NULL,
    criada_em datetime2(6) NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_referencia_fiscal_produto_1 UNIQUE (produto_id, armazem_id, operacao),
    CONSTRAINT ck_referencia_fiscal_produto_icms CHECK (
        aliquota_icms IS NULL OR aliquota_icms BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_referencia_fiscal_produto_ipi CHECK (
        aliquota_ipi IS NULL OR aliquota_ipi BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_referencia_fiscal_produto_conferencia CHECK (
        (conferida_por IS NULL AND conferida_em IS NULL) OR (conferida_por IS NOT NULL AND conferida_em IS NOT NULL)
    ),
    CONSTRAINT ck_referencia_fiscal_produto_versao CHECK (
        versao >= 0
    )
);

CREATE TABLE wms.importacao_endereco (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_importacao_endereco PRIMARY KEY,
    armazem_id bigint NOT NULL CONSTRAINT fk_importacao_endereco_armazem REFERENCES wms.armazem(id),
    arquivo_hash varchar(64) NOT NULL,
    layout_versao int NOT NULL,
    quantidade_linhas int NOT NULL,
    situacao varchar(16) NOT NULL,
    linhas_json nvarchar(max) NOT NULL,
    erros_json nvarchar(max) NOT NULL,
    versao bigint NOT NULL,
    criada_em datetime2(6) NOT NULL,
    confirmada_em datetime2(6) NULL,
    usuario nvarchar(200) NOT NULL,
    CONSTRAINT ck_importacao_endereco_layout CHECK (
        layout_versao >= 1
    ),
    CONSTRAINT ck_importacao_endereco_linhas CHECK (
        quantidade_linhas BETWEEN 1 AND 5000
    ),
    CONSTRAINT ck_importacao_endereco_situacao CHECK (
        situacao IN ('PREVIA','COM_ERROS','CONFIRMADA')
    ),
    CONSTRAINT ck_importacao_endereco_confirmacao CHECK (
        (situacao IN ('PREVIA','COM_ERROS') AND confirmada_em IS NULL) OR (situacao = 'CONFIRMADA' AND confirmada_em IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(erros_json, N' ', N''), NCHAR(9), N''), NCHAR(10), N''), NCHAR(13), N'') = N'[]')
    ),
    CONSTRAINT ck_importacao_endereco_versao CHECK (
        versao >= 0
    ),
    CONSTRAINT ck_importacao_endereco_linhas_json CHECK (
        ISJSON(linhas_json) = 1
    ),
    CONSTRAINT ck_importacao_endereco_erros_json CHECK (
        ISJSON(erros_json) = 1
    )
);

CREATE TABLE wms.operacao_administrativa (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_operacao_administrativa PRIMARY KEY,
    operacao_id varchar(36) NOT NULL,
    tipo varchar(32) NOT NULL,
    cliente_id bigint NULL CONSTRAINT fk_operacao_administrativa_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NULL CONSTRAINT fk_operacao_administrativa_armazem REFERENCES wms.armazem(id),
    recurso_id bigint NULL,
    conteudo_hash varchar(64) NOT NULL,
    resultado nvarchar(max) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_operacao_administrativa_1 UNIQUE (operacao_id),
    CONSTRAINT ck_operacao_administrativa_tipo CHECK (
        tipo IN ('CRIACAO_SERVICO','CRIACAO_TABELA','CRIACAO','COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA')
    ),
    CONSTRAINT ck_operacao_administrativa_resultado CHECK (
        ISJSON(resultado) = 1
    )
);

CREATE TABLE wms.servico_cobranca (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_servico_cobranca PRIMARY KEY,
    codigo varchar(40) NOT NULL,
    descricao nvarchar(160) NOT NULL,
    tipo varchar(16) NOT NULL,
    unidade varchar(24) NOT NULL,
    situacao varchar(24) NOT NULL,
    versao bigint NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    CONSTRAINT uk_servico_cobranca_1 UNIQUE (codigo),
    CONSTRAINT ck_servico_cobranca_tipo CHECK (
        tipo IN ('ENTRADA','SAIDA','ARMAZENAGEM','ADICIONAL')
    ),
    CONSTRAINT ck_servico_cobranca_unidade CHECK (
        unidade IN ('UNIDADE_LOGISTICA','QUANTIDADE_PRODUTO','POSICAO_DIA','VEICULO','CONTEINER','PERCENTUAL')
    ),
    CONSTRAINT ck_servico_cobranca_situacao CHECK (
        situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
    ),
    CONSTRAINT ck_servico_cobranca_armazenagem CHECK (
        tipo <> 'ARMAZENAGEM' OR unidade = 'POSICAO_DIA'
    ),
    CONSTRAINT ck_servico_cobranca_percentual CHECK (
        unidade <> 'PERCENTUAL' OR tipo = 'ADICIONAL'
    ),
    CONSTRAINT ck_servico_cobranca_versao CHECK (
        versao >= 0
    )
);

CREATE TABLE wms.tabela_cobranca (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_tabela_cobranca PRIMARY KEY,
    armazem_id bigint NOT NULL CONSTRAINT fk_tabela_cobranca_armazem REFERENCES wms.armazem(id),
    cliente_id bigint NULL CONSTRAINT fk_tabela_cobranca_cliente REFERENCES wms.cliente(id),
    codigo varchar(40) NOT NULL,
    descricao nvarchar(160) NOT NULL,
    tipo varchar(16) NOT NULL,
    vigencia_inicio date NOT NULL,
    vigencia_fim date NULL,
    situacao varchar(16) NOT NULL,
    versao bigint NOT NULL,
    criada_em datetime2(6) NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_tabela_cobranca_1 UNIQUE (armazem_id, codigo),
    CONSTRAINT ck_tabela_cobranca_tipo CHECK (
        tipo IN ('PADRAO','ESPECIFICA')
    ),
    CONSTRAINT ck_tabela_cobranca_situacao CHECK (
        situacao IN ('ATIVA','ENCERRADA')
    ),
    CONSTRAINT ck_tabela_cobranca_vigencia CHECK (
        vigencia_fim IS NULL OR vigencia_fim > vigencia_inicio
    ),
    CONSTRAINT ck_tabela_cobranca_contexto CHECK (
        (tipo = 'PADRAO' AND cliente_id IS NULL) OR (tipo = 'ESPECIFICA' AND cliente_id IS NOT NULL)
    ),
    CONSTRAINT ck_tabela_cobranca_versao CHECK (
        versao >= 0
    )
);

CREATE TABLE wms.item_tabela_cobranca (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_item_tabela_cobranca PRIMARY KEY,
    tabela_id bigint NOT NULL CONSTRAINT fk_item_tabela_cobranca_tabela REFERENCES wms.tabela_cobranca(id),
    servico_id bigint NOT NULL CONSTRAINT fk_item_tabela_cobranca_servico REFERENCES wms.servico_cobranca(id),
    categoria varchar(40) NOT NULL,
    preco decimal(19,6) NULL,
    percentual decimal(9,6) NULL,
    CONSTRAINT uk_item_tabela_cobranca_1 UNIQUE (tabela_id, servico_id, categoria),
    CONSTRAINT ck_item_tabela_cobranca_preco CHECK (
        preco IS NULL OR preco >= 0
    ),
    CONSTRAINT ck_item_tabela_cobranca_percentual CHECK (
        percentual IS NULL OR percentual BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_item_tabela_cobranca_parametro CHECK (
        (preco IS NOT NULL AND percentual IS NULL) OR (preco IS NULL AND percentual IS NOT NULL)
    )
);

CREATE TABLE wms.vinculo_tabela_cliente (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_vinculo_tabela_cliente PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_vinculo_tabela_cliente_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_vinculo_tabela_cliente_armazem REFERENCES wms.armazem(id),
    tabela_id bigint NOT NULL CONSTRAINT fk_vinculo_tabela_cliente_tabela REFERENCES wms.tabela_cobranca(id),
    vigencia_inicio date NOT NULL,
    vigencia_fim date NULL,
    versao bigint NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    CONSTRAINT uk_vinculo_tabela_cliente_1 UNIQUE (cliente_id, armazem_id, vigencia_inicio),
    CONSTRAINT ck_vinculo_tabela_cliente_vigencia CHECK (
        vigencia_fim IS NULL OR vigencia_fim > vigencia_inicio
    ),
    CONSTRAINT ck_vinculo_tabela_cliente_versao CHECK (
        versao >= 0
    )
);

CREATE TABLE wms.contrato_cobranca (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_contrato_cobranca PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_contrato_cobranca_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_contrato_cobranca_armazem REFERENCES wms.armazem(id),
    vigencia_inicio date NOT NULL,
    vigencia_fim date NULL,
    fuso varchar(60) NOT NULL,
    moeda varchar(3) NOT NULL,
    modalidade_ciclo varchar(20) NOT NULL,
    dia_corte int NULL,
    duracao_dias int NULL,
    minimo_modo varchar(16) NOT NULL,
    gris_modo varchar(16) NOT NULL,
    minimo_valor decimal(19,2) NULL,
    minimo_proporcao varchar(20) NULL,
    gris_percentual decimal(9,6) NULL,
    gris_base varchar(24) NULL,
    gris_periodicidade varchar(16) NULL,
    gris_proporcao varchar(20) NULL,
    versao bigint NOT NULL,
    criada_em datetime2(6) NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_contrato_cobranca_1 UNIQUE (cliente_id, armazem_id, vigencia_inicio),
    CONSTRAINT ck_contrato_cobranca_vigencia CHECK (
        vigencia_fim IS NULL OR vigencia_fim > vigencia_inicio
    ),
    CONSTRAINT ck_contrato_cobranca_moeda CHECK (
        moeda = 'BRL'
    ),
    CONSTRAINT ck_contrato_cobranca_modalidade CHECK (
        modalidade_ciclo IN ('MES_DIA_FIXO','DIAS_CORRIDOS')
    ),
    CONSTRAINT ck_contrato_cobranca_ciclo CHECK (
        (modalidade_ciclo = 'MES_DIA_FIXO' AND dia_corte IS NOT NULL AND dia_corte BETWEEN 1 AND 31 AND duracao_dias IS NULL) OR (modalidade_ciclo = 'DIAS_CORRIDOS' AND duracao_dias IS NOT NULL AND duracao_dias BETWEEN 1 AND 366 AND dia_corte IS NULL)
    ),
    CONSTRAINT ck_contrato_cobranca_minimo_modo CHECK (
        minimo_modo IN ('NAO_INFORMADO','NAO_APLICAVEL','APLICAVEL')
    ),
    CONSTRAINT ck_contrato_cobranca_gris_modo CHECK (
        gris_modo IN ('NAO_INFORMADO','NAO_APLICAVEL','APLICAVEL')
    ),
    CONSTRAINT ck_contrato_cobranca_minimo_valor CHECK (
        minimo_valor IS NULL OR minimo_valor >= 0
    ),
    CONSTRAINT ck_contrato_cobranca_minimo_proporcao CHECK (
        minimo_proporcao IS NULL OR minimo_proporcao IN ('INTEGRAL','PROPORCIONAL_DIAS')
    ),
    CONSTRAINT ck_contrato_cobranca_minimo_config CHECK (
        (minimo_modo = 'APLICAVEL' AND minimo_valor IS NOT NULL AND minimo_proporcao IS NOT NULL) OR (minimo_modo IN ('NAO_INFORMADO','NAO_APLICAVEL') AND minimo_valor IS NULL AND minimo_proporcao IS NULL)
    ),
    CONSTRAINT ck_contrato_cobranca_gris_percentual CHECK (
        gris_percentual IS NULL OR gris_percentual BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_contrato_cobranca_gris_base CHECK (
        gris_base IS NULL OR gris_base IN ('VALOR_ESTOQUE_PICO','VALOR_ESTOQUE_MEDIO')
    ),
    CONSTRAINT ck_contrato_cobranca_gris_periodicidade CHECK (
        gris_periodicidade IS NULL OR gris_periodicidade IN ('DIARIA','POR_CICLO')
    ),
    CONSTRAINT ck_contrato_cobranca_gris_proporcao CHECK (
        gris_proporcao IS NULL OR gris_proporcao IN ('INTEGRAL','PROPORCIONAL_DIAS')
    ),
    CONSTRAINT ck_contrato_cobranca_gris_config CHECK (
        (gris_modo = 'APLICAVEL' AND gris_percentual IS NOT NULL AND gris_base IS NOT NULL AND gris_periodicidade IS NOT NULL AND gris_proporcao IS NOT NULL) OR (gris_modo IN ('NAO_INFORMADO','NAO_APLICAVEL') AND gris_percentual IS NULL AND gris_base IS NULL AND gris_periodicidade IS NULL AND gris_proporcao IS NULL)
    ),
    CONSTRAINT ck_contrato_cobranca_versao CHECK (
        versao >= 0
    )
);

CREATE TABLE wms.servico_minimo_contrato (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_servico_minimo_contrato PRIMARY KEY,
    contrato_id bigint NOT NULL CONSTRAINT fk_servico_minimo_contrato_contrato REFERENCES wms.contrato_cobranca(id),
    servico_id bigint NOT NULL CONSTRAINT fk_servico_minimo_contrato_servico REFERENCES wms.servico_cobranca(id),
    CONSTRAINT uk_servico_minimo_contrato_1 UNIQUE (contrato_id, servico_id)
);

CREATE TABLE wms.fato_servico (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_fato_servico PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_fato_servico_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_fato_servico_armazem REFERENCES wms.armazem(id),
    servico_id bigint NOT NULL CONSTRAINT fk_fato_servico_servico REFERENCES wms.servico_cobranca(id),
    pedido_entrada_id bigint NULL CONSTRAINT fk_fato_servico_pedido_entrada REFERENCES wms.pedido_entrada(id),
    pedido_saida_id bigint NULL CONSTRAINT fk_fato_servico_pedido_saida REFERENCES wms.pedido_saida(id),
    unidade_id bigint NULL CONSTRAINT fk_fato_servico_unidade REFERENCES wms.unidade_logistica(id),
    produto_id bigint NULL CONSTRAINT fk_fato_servico_produto REFERENCES wms.produto(id),
    chave_fato varchar(160) NOT NULL,
    origem varchar(16) NOT NULL,
    referencia_execucao varchar(80) NULL,
    executado_em datetime2(6) NOT NULL,
    registrado_em datetime2(6) NOT NULL,
    quantidade decimal(19,6) NOT NULL,
    categoria varchar(40) NOT NULL,
    valor_base decimal(19,2) NULL,
    criterio_rateio nvarchar(200) NOT NULL,
    situacao varchar(16) NOT NULL,
    anulado_em datetime2(6) NULL,
    versao bigint NOT NULL,
    usuario nvarchar(200) NOT NULL,
    CONSTRAINT uk_fato_servico_1 UNIQUE (cliente_id, armazem_id, servico_id, chave_fato),
    CONSTRAINT ck_fato_servico_origem CHECK (
        origem IN ('MANUAL','SUGESTAO')
    ),
    CONSTRAINT ck_fato_servico_situacao CHECK (
        situacao IN ('CONFIRMADO','ANULADO')
    ),
    CONSTRAINT ck_fato_servico_quantidade CHECK (
        quantidade > 0
    ),
    CONSTRAINT ck_fato_servico_valor_base CHECK (
        valor_base IS NULL OR valor_base >= 0
    ),
    CONSTRAINT ck_fato_servico_anulacao CHECK (
        (situacao = 'CONFIRMADO' AND anulado_em IS NULL) OR (situacao = 'ANULADO' AND anulado_em IS NOT NULL)
    ),
    CONSTRAINT ck_fato_servico_versao CHECK (
        versao >= 0
    )
);

CREATE TABLE wms.rateio_fato_servico (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_rateio_fato_servico PRIMARY KEY,
    fato_id bigint NOT NULL CONSTRAINT fk_rateio_fato_servico_fato REFERENCES wms.fato_servico(id),
    nota_id bigint NOT NULL CONSTRAINT fk_rateio_fato_servico_nota REFERENCES wms.nota_entrada(id),
    cota decimal(19,6) NOT NULL,
    CONSTRAINT uk_rateio_fato_servico_1 UNIQUE (fato_id, nota_id),
    CONSTRAINT ck_rateio_fato_servico_cota CHECK (
        cota > 0
    )
);

CREATE TABLE wms.marco_financeiro_avaria (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_marco_financeiro_avaria PRIMARY KEY,
    avaria_id bigint NOT NULL CONSTRAINT fk_marco_financeiro_avaria_avaria REFERENCES wms.avaria_estoque(id),
    fato_permanencia_id bigint NOT NULL CONSTRAINT fk_marco_financeiro_avaria_fato_permanencia REFERENCES wms.fato_permanencia(id),
    quantidade_afetada decimal(19,6) NOT NULL,
    quantidade_base decimal(19,6) NOT NULL,
    equivalencia_base decimal(19,6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    validado_em datetime2(6) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT uk_marco_financeiro_avaria_1 UNIQUE (avaria_id, fato_permanencia_id),
    CONSTRAINT ck_marco_financeiro_avaria_quantidade CHECK (
        quantidade_afetada >= 0 AND quantidade_base >= 0 AND quantidade_afetada <= quantidade_base
    ),
    CONSTRAINT ck_marco_financeiro_avaria_equivalencia CHECK (
        equivalencia_base >= 0
    )
);

CREATE TABLE wms.calculo_cobranca (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_calculo_cobranca PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_calculo_cobranca_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_calculo_cobranca_armazem REFERENCES wms.armazem(id),
    contrato_id bigint NULL CONSTRAINT fk_calculo_cobranca_contrato REFERENCES wms.contrato_cobranca(id),
    periodo_inicio date NOT NULL,
    periodo_fim date NOT NULL,
    fuso varchar(60) NULL,
    moeda varchar(3) NULL,
    situacao varchar(16) NOT NULL,
    regra_versao int NOT NULL,
    entradas_hash varchar(64) NOT NULL,
    memoria_json nvarchar(max) NOT NULL,
    pendencias_json nvarchar(max) NOT NULL,
    subtotal_conhecido decimal(19,2) NOT NULL,
    minimo_calculado decimal(19,2) NULL,
    gris_calculado decimal(19,2) NULL,
    total decimal(19,2) NULL,
    calculado_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    CONSTRAINT ck_calculo_cobranca_periodo CHECK (
        periodo_fim > periodo_inicio
    ),
    CONSTRAINT ck_calculo_cobranca_moeda CHECK (
        moeda IS NULL OR moeda = 'BRL'
    ),
    CONSTRAINT ck_calculo_cobranca_situacao CHECK (
        situacao IN ('COMPLETO','PENDENTE')
    ),
    CONSTRAINT ck_calculo_cobranca_regra CHECK (
        regra_versao >= 1
    ),
    CONSTRAINT ck_calculo_cobranca_subtotal CHECK (
        subtotal_conhecido >= 0
    ),
    CONSTRAINT ck_calculo_cobranca_minimo CHECK (
        minimo_calculado IS NULL OR minimo_calculado >= 0
    ),
    CONSTRAINT ck_calculo_cobranca_gris CHECK (
        gris_calculado IS NULL OR gris_calculado >= 0
    ),
    CONSTRAINT ck_calculo_cobranca_total CHECK (
        total IS NULL OR total >= 0
    ),
    CONSTRAINT ck_calculo_cobranca_completo CHECK (
        situacao <> 'COMPLETO' OR (contrato_id IS NOT NULL AND fuso IS NOT NULL AND moeda IS NOT NULL AND minimo_calculado IS NOT NULL AND gris_calculado IS NOT NULL AND total IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(pendencias_json, N' ', N''), NCHAR(9), N''), NCHAR(10), N''), NCHAR(13), N'') = N'[]')
    ),
    CONSTRAINT ck_calculo_cobranca_memoria_json CHECK (
        ISJSON(memoria_json) = 1
    ),
    CONSTRAINT ck_calculo_cobranca_pendencias_json CHECK (
        ISJSON(pendencias_json) = 1
    )
);

CREATE TABLE wms.memoria_diaria (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_memoria_diaria PRIMARY KEY,
    calculo_id bigint NOT NULL CONSTRAINT fk_memoria_diaria_calculo REFERENCES wms.calculo_cobranca(id),
    data date NOT NULL,
    tabela_id bigint NULL CONSTRAINT fk_memoria_diaria_tabela REFERENCES wms.tabela_cobranca(id),
    item_tabela_id bigint NULL CONSTRAINT fk_memoria_diaria_item_tabela REFERENCES wms.item_tabela_cobranca(id),
    pico_cobravel decimal(19,6) NOT NULL,
    equivalencia_suspensa decimal(19,6) NOT NULL,
    valor_estoque decimal(19,2) NULL,
    tarifa decimal(19,6) NULL,
    valor decimal(19,2) NULL,
    segmentos_json nvarchar(max) NOT NULL,
    CONSTRAINT uk_memoria_diaria_1 UNIQUE (calculo_id, data),
    CONSTRAINT ck_memoria_diaria_equivalencias CHECK (
        pico_cobravel >= 0 AND equivalencia_suspensa >= 0
    ),
    CONSTRAINT ck_memoria_diaria_valor_estoque CHECK (
        valor_estoque IS NULL OR valor_estoque >= 0
    ),
    CONSTRAINT ck_memoria_diaria_tarifa CHECK (
        tarifa IS NULL OR tarifa >= 0
    ),
    CONSTRAINT ck_memoria_diaria_valor CHECK (
        valor IS NULL OR valor >= 0
    ),
    CONSTRAINT ck_memoria_diaria_segmentos_json CHECK (
        ISJSON(segmentos_json) = 1
    )
);

CREATE TABLE wms.memoria_servico (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_memoria_servico PRIMARY KEY,
    calculo_id bigint NOT NULL CONSTRAINT fk_memoria_servico_calculo REFERENCES wms.calculo_cobranca(id),
    fato_id bigint NOT NULL CONSTRAINT fk_memoria_servico_fato REFERENCES wms.fato_servico(id),
    item_tabela_id bigint NULL CONSTRAINT fk_memoria_servico_item_tabela REFERENCES wms.item_tabela_cobranca(id),
    quantidade decimal(19,6) NOT NULL,
    preco decimal(19,6) NULL,
    percentual decimal(9,6) NULL,
    valor_base decimal(19,2) NULL,
    valor decimal(19,2) NULL,
    parcelas_json nvarchar(max) NOT NULL,
    CONSTRAINT uk_memoria_servico_1 UNIQUE (calculo_id, fato_id),
    CONSTRAINT ck_memoria_servico_quantidade CHECK (
        quantidade > 0
    ),
    CONSTRAINT ck_memoria_servico_preco CHECK (
        preco IS NULL OR preco >= 0
    ),
    CONSTRAINT ck_memoria_servico_percentual CHECK (
        percentual IS NULL OR percentual BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_memoria_servico_valor_base CHECK (
        valor_base IS NULL OR valor_base >= 0
    ),
    CONSTRAINT ck_memoria_servico_valor CHECK (
        valor IS NULL OR valor >= 0
    ),
    CONSTRAINT ck_memoria_servico_parcelas_json CHECK (
        ISJSON(parcelas_json) = 1
    )
);

-- UNIQUE ja indexa a primeira FK; demais vinculos recebem indice de consulta.
CREATE INDEX ix_referencia_fiscal_produto_armazem ON wms.referencia_fiscal_produto(armazem_id, id);
CREATE INDEX ix_importacao_endereco_armazem ON wms.importacao_endereco(armazem_id, criada_em, id);
CREATE INDEX ix_operacao_administrativa_cliente ON wms.operacao_administrativa(cliente_id, id);
CREATE INDEX ix_operacao_administrativa_armazem ON wms.operacao_administrativa(armazem_id, id);
CREATE INDEX ix_tabela_cobranca_cliente ON wms.tabela_cobranca(cliente_id, id);
CREATE INDEX ix_item_tabela_cobranca_servico ON wms.item_tabela_cobranca(servico_id, id);
CREATE INDEX ix_vinculo_tabela_cliente_armazem ON wms.vinculo_tabela_cliente(armazem_id, id);
CREATE INDEX ix_vinculo_tabela_cliente_tabela ON wms.vinculo_tabela_cliente(tabela_id, id);
CREATE INDEX ix_contrato_cobranca_armazem ON wms.contrato_cobranca(armazem_id, id);
CREATE INDEX ix_servico_minimo_contrato_servico ON wms.servico_minimo_contrato(servico_id, id);
CREATE INDEX ix_fato_servico_armazem ON wms.fato_servico(armazem_id, executado_em, id);
CREATE INDEX ix_fato_servico_servico ON wms.fato_servico(servico_id, id);
CREATE INDEX ix_fato_servico_pedido_entrada ON wms.fato_servico(pedido_entrada_id, id);
CREATE INDEX ix_fato_servico_pedido_saida ON wms.fato_servico(pedido_saida_id, id);
CREATE INDEX ix_fato_servico_unidade ON wms.fato_servico(unidade_id, id);
CREATE INDEX ix_fato_servico_produto ON wms.fato_servico(produto_id, id);
CREATE INDEX ix_rateio_fato_servico_nota ON wms.rateio_fato_servico(nota_id, id);
CREATE INDEX ix_marco_financeiro_avaria_fato_permanencia ON wms.marco_financeiro_avaria(fato_permanencia_id, id);
CREATE INDEX ix_calculo_cobranca_cliente ON wms.calculo_cobranca(cliente_id, id);
CREATE INDEX ix_calculo_cobranca_armazem ON wms.calculo_cobranca(armazem_id, periodo_inicio, id);
CREATE INDEX ix_calculo_cobranca_contrato ON wms.calculo_cobranca(contrato_id, id);
CREATE INDEX ix_memoria_diaria_tabela ON wms.memoria_diaria(tabela_id, id);
CREATE INDEX ix_memoria_diaria_item_tabela ON wms.memoria_diaria(item_tabela_id, id);
CREATE INDEX ix_memoria_servico_fato ON wms.memoria_servico(fato_id, id);
CREATE INDEX ix_memoria_servico_item_tabela ON wms.memoria_servico(item_tabela_id, id);

-- Preservar pares V5/V6 e ampliar apenas auditoria administrativa/financeira.
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_tipo CHECK (
    tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','PEDIDO_ENTRADA','CONJUNTO_POSICOES','PEDIDO_SAIDA','SERVICO_COBRANCA','TABELA_COBRANCA','CONTRATO_COBRANCA','FATO_SERVICO','AVARIA_FINANCEIRA','CALCULO_COBRANCA')
);
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_acao CHECK (
    acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO','NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA','ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS','CONFIGURAR_CAPACIDADE','ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE','CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE','LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA','CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA','RETORNO_INTERNO','DEVOLUCAO_SAIDA','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA','COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA')
);
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_financeira_tipo_acao CHECK (
    (acao NOT IN ('COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA')
        OR (acao = 'COMPLEMENTO_FISCAL' AND tipo IN ('CLIENTE','ARMAZEM'))
        OR (acao = 'REFERENCIA_FISCAL' AND tipo IN ('PRODUTO'))
        OR (acao = 'PREVIA_ENDERECOS' AND tipo IN ('ARMAZEM'))
        OR (acao = 'IMPORTACAO_ENDERECOS' AND tipo IN ('ARMAZEM'))
        OR (acao = 'ENCERRAMENTO_VIGENCIA' AND tipo IN ('TABELA_COBRANCA'))
        OR (acao = 'CONFIGURACAO' AND tipo IN ('CONTRATO_COBRANCA'))
        OR (acao = 'REGISTRO_SERVICO' AND tipo IN ('FATO_SERVICO'))
        OR (acao = 'ANULACAO_SERVICO' AND tipo IN ('FATO_SERVICO'))
        OR (acao = 'MARCO_AVARIA' AND tipo IN ('AVARIA_FINANCEIRA'))
        OR (acao = 'CALCULO' AND tipo IN ('CALCULO_COBRANCA'))
        OR (acao = 'VINCULO_TABELA' AND tipo IN ('CONTRATO_COBRANCA')))
    AND (tipo NOT IN ('SERVICO_COBRANCA','TABELA_COBRANCA','CONTRATO_COBRANCA','FATO_SERVICO','AVARIA_FINANCEIRA','CALCULO_COBRANCA')
        OR (tipo = 'SERVICO_COBRANCA' AND acao IN ('CRIACAO'))
        OR (tipo = 'TABELA_COBRANCA' AND acao IN ('CRIACAO','ENCERRAMENTO_VIGENCIA'))
        OR (tipo = 'CONTRATO_COBRANCA' AND acao IN ('CONFIGURACAO','VINCULO_TABELA'))
        OR (tipo = 'FATO_SERVICO' AND acao IN ('REGISTRO_SERVICO','ANULACAO_SERVICO'))
        OR (tipo = 'AVARIA_FINANCEIRA' AND acao IN ('MARCO_AVARIA'))
        OR (tipo = 'CALCULO_COBRANCA' AND acao IN ('CALCULO')))
);
-- ck_auditoria_saida_tipo_acao permanece da V6; nenhum ALTER em movimento_estoque.
