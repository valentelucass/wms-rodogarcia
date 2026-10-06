-- SQL Server: arquivo preparado, aplicar somente pelo procedimento explicito do WMS.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

ALTER TABLE wms.entrada_conferida ADD unitizada_em datetime2(6) NULL;

CREATE TABLE wms.unidade_logistica (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_unidade_logistica PRIMARY KEY,
    versao bigint NOT NULL,
    codigo varchar(36) NOT NULL CONSTRAINT uk_unidade_codigo UNIQUE,
    pedido_id bigint NOT NULL CONSTRAINT fk_unidade_pedido REFERENCES wms.pedido_entrada(id),
    nota_id bigint NOT NULL CONSTRAINT fk_unidade_nota REFERENCES wms.nota_entrada(id),
    produto_id bigint NOT NULL CONSTRAINT fk_unidade_produto REFERENCES wms.produto(id),
    embalagem_id bigint NOT NULL CONSTRAINT fk_unidade_embalagem REFERENCES wms.embalagem(id),
    tipo varchar(16) NOT NULL,
    condicao varchar(16) NOT NULL,
    lote nvarchar(60) NULL,
    validade date NULL,
    data_fifo datetime2(6) NOT NULL,
    chegada_real datetime2(6) NOT NULL,
    quantidade decimal(19,6) NOT NULL,
    ativa bit NOT NULL,
    criada_em datetime2(6) NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    CONSTRAINT ck_unidade_tipo CHECK (tipo IN ('PALLET','BOBINA','VOLUME')),
    CONSTRAINT ck_unidade_condicao CHECK (condicao IN ('BOA','AVARIADA')),
    CONSTRAINT ck_unidade_quantidade CHECK ((ativa = 1 AND quantidade > 0) OR (ativa = 0 AND quantidade = 0)),
    CONSTRAINT ck_unidade_versao CHECK (versao >= 0)
);

CREATE TABLE wms.conteudo_unidade (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_conteudo_unidade PRIMARY KEY,
    unidade_id bigint NOT NULL CONSTRAINT fk_conteudo_unidade REFERENCES wms.unidade_logistica(id),
    entrada_id bigint NOT NULL CONSTRAINT fk_conteudo_entrada REFERENCES wms.entrada_conferida(id),
    quantidade decimal(19,6) NOT NULL,
    CONSTRAINT uk_conteudo_origem UNIQUE (unidade_id, entrada_id),
    CONSTRAINT ck_conteudo_quantidade CHECK (quantidade >= 0)
);

CREATE TABLE wms.operacao_unidade (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_operacao_unidade PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_operacao_unidade_pedido REFERENCES wms.pedido_entrada(id),
    operacao_id varchar(36) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    tipo varchar(24) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    resultado nvarchar(max) NOT NULL,
    CONSTRAINT uk_operacao_unidade UNIQUE (pedido_id, operacao_id),
    CONSTRAINT ck_operacao_unidade_tipo CHECK (tipo IN ('UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS')),
    CONSTRAINT ck_operacao_unidade_json CHECK (ISJSON(resultado) = 1)
);

CREATE INDEX ix_unidade_pedido ON wms.unidade_logistica(pedido_id, id);
CREATE INDEX ix_unidade_produto ON wms.unidade_logistica(produto_id);
CREATE INDEX ix_conteudo_entrada ON wms.conteudo_unidade(entrada_id, unidade_id);

-- Auditoria e efeitos operacionais participam da mesma transacao.
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro ADD CONSTRAINT ck_auditoria_acao CHECK (acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO','NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA','ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS'));
