-- SQL Server: preparar somente; aplicar pelo procedimento explicito de database/migrate.ps1.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

CREATE TABLE wms.pedido_entrada (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_pedido_entrada PRIMARY KEY,
    versao bigint NOT NULL,
    cliente_id bigint NOT NULL CONSTRAINT fk_pedido_entrada_cliente_id REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_pedido_entrada_armazem_id REFERENCES wms.armazem(id),
    referencia varchar(40) NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    efetivado_em datetime2(6) NULL,
    motivo_conclusao nvarchar(500) NULL,
    CONSTRAINT uk_pedido_entrada_1 UNIQUE (cliente_id, armazem_id, referencia),
    CONSTRAINT ck_pedido_entrada_situacao CHECK (situacao IN ('RASCUNHO','EM_CONFERENCIA','QUARENTENA','EFETIVADO','CANCELADO'))
);

CREATE TABLE wms.nota_entrada (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_nota_entrada PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_nota_entrada_pedido_id REFERENCES wms.pedido_entrada(id),
    emitente varchar(30) NOT NULL,
    serie int NOT NULL,
    numero bigint NOT NULL,
    emissao date NOT NULL,
    chave_acesso varchar(44) NULL,
    xml_hash varchar(64) NULL,
    xml_original nvarchar(max) NULL,
    CONSTRAINT uk_nota_entrada_1 UNIQUE (emitente, serie, numero),
    CONSTRAINT ck_nota_entrada_numero CHECK (numero BETWEEN 1 AND 999999999 AND serie BETWEEN 0 AND 999)
);

CREATE TABLE wms.item_nota_entrada (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_item_nota_entrada PRIMARY KEY,
    nota_id bigint NOT NULL CONSTRAINT fk_item_nota_entrada_nota_id REFERENCES wms.nota_entrada(id),
    numero_item int NOT NULL,
    produto_id bigint NOT NULL CONSTRAINT fk_item_nota_entrada_produto_id REFERENCES wms.produto(id),
    quantidade_prevista decimal(19,6) NOT NULL,
    valor_mercadoria decimal(19,2) NULL,
    CONSTRAINT uk_item_nota_entrada_1 UNIQUE (nota_id, numero_item),
    CONSTRAINT ck_item_nota_entrada_quantidade CHECK (numero_item BETWEEN 1 AND 990 AND quantidade_prevista > 0 AND (valor_mercadoria IS NULL OR valor_mercadoria >= 0))
);

CREATE TABLE wms.chegada_recebimento (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_chegada_recebimento PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_chegada_recebimento_pedido_id REFERENCES wms.pedido_entrada(id),
    operacao_id varchar(36) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    chegou_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    observacao nvarchar(500) NOT NULL,
    estornada_em datetime2(6) NULL,
    estornada_por nvarchar(200) NULL,
    motivo_estorno nvarchar(500) NULL,
    CONSTRAINT uk_chegada_recebimento_1 UNIQUE (pedido_id, operacao_id),
    CONSTRAINT ck_chegada_estorno CHECK ((estornada_em IS NULL AND estornada_por IS NULL AND motivo_estorno IS NULL) OR (estornada_em IS NOT NULL AND estornada_por IS NOT NULL AND motivo_estorno IS NOT NULL))
);

CREATE TABLE wms.item_chegada (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_item_chegada PRIMARY KEY,
    chegada_id bigint NOT NULL CONSTRAINT fk_item_chegada_chegada_id REFERENCES wms.chegada_recebimento(id),
    sequencia int NOT NULL,
    item_nota_id bigint NOT NULL CONSTRAINT fk_item_chegada_item_nota_id REFERENCES wms.item_nota_entrada(id),
    lote nvarchar(60) NULL,
    validade date NULL,
    quantidade_boa decimal(19,6) NOT NULL,
    quantidade_avariada decimal(19,6) NOT NULL,
    CONSTRAINT uk_item_chegada_1 UNIQUE (chegada_id, sequencia),
    CONSTRAINT ck_item_chegada_quantidade CHECK (quantidade_boa >= 0 AND quantidade_avariada >= 0 AND quantidade_boa + quantidade_avariada > 0)
);

CREATE TABLE wms.entrada_conferida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_entrada_conferida PRIMARY KEY,
    item_chegada_id bigint NOT NULL CONSTRAINT fk_entrada_conferida_item_chegada_id REFERENCES wms.item_chegada(id),
    data_fifo datetime2(6) NOT NULL,
    efetivada_em datetime2(6) NOT NULL,
    quantidade_triagem decimal(19,6) NOT NULL,
    quantidade_quarentena decimal(19,6) NOT NULL,
    CONSTRAINT uk_entrada_conferida_1 UNIQUE (item_chegada_id),
    CONSTRAINT ck_entrada_conferida_quantidade CHECK (quantidade_triagem >= 0 AND quantidade_quarentena >= 0 AND quantidade_triagem + quantidade_quarentena > 0)
);

CREATE UNIQUE INDEX uk_nota_entrada_chave ON wms.nota_entrada(chave_acesso) WHERE chave_acesso IS NOT NULL;
CREATE INDEX ix_nota_entrada_pedido ON wms.nota_entrada(pedido_id);
CREATE INDEX ix_pedido_entrada_escopo ON wms.pedido_entrada(cliente_id, armazem_id, id);
CREATE INDEX ix_item_nota_produto ON wms.item_nota_entrada(produto_id);
CREATE INDEX ix_item_chegada_nota ON wms.item_chegada(item_nota_id);

-- A auditoria existente passa a registrar tambem comandos do recebimento.
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
ALTER TABLE wms.auditoria_cadastro ADD CONSTRAINT ck_auditoria_tipo CHECK (tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','PEDIDO_ENTRADA'));
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro ADD CONSTRAINT ck_auditoria_acao CHECK (acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO','NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA','ENTRADA_EFETIVADA','PEDIDO_CANCELADO'));
