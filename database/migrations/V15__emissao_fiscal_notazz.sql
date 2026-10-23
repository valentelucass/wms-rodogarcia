SET XACT_ABORT ON;
BEGIN TRANSACTION;

ALTER TABLE wms.cliente ADD codigo_municipio varchar(7) NULL, indicador_ie int NULL, agrupamento_nfe varchar(16) NULL;
ALTER TABLE wms.armazem ADD codigo_municipio varchar(7) NULL, indicador_ie int NULL, agrupamento_nfe varchar(16) NULL;
ALTER TABLE wms.referencia_fiscal_produto ADD tributacao_nfe nvarchar(max) NULL;
GO

ALTER TABLE wms.cliente ADD CONSTRAINT ck_cliente_indicador_ie CHECK(indicador_ie IS NULL OR indicador_ie IN(1,2,9)), CONSTRAINT ck_cliente_agrupamento_nfe CHECK(agrupamento_nfe IS NULL OR agrupamento_nfe IN('CONSOLIDAR','POR_NOTA'));
ALTER TABLE wms.armazem ADD CONSTRAINT ck_armazem_indicador_ie CHECK(indicador_ie IS NULL OR indicador_ie IN(1,2,9));
ALTER TABLE wms.referencia_fiscal_produto ADD CONSTRAINT ck_referencia_tributacao_nfe CHECK(tributacao_nfe IS NULL OR ISJSON(tributacao_nfe)=1);

CREATE TABLE wms.solicitacao_nfe (
    id varchar(36) NOT NULL CONSTRAINT pk_solicitacao_nfe PRIMARY KEY,
    plano_id varchar(36) NOT NULL, operacao_id varchar(36) NOT NULL,
    pedido_id bigint NOT NULL CONSTRAINT fk_solicitacao_nfe_pedido REFERENCES wms.pedido_saida(id),
    grupo varchar(64) NOT NULL, natureza varchar(24) NOT NULL, versao_pedido bigint NOT NULL,
    hash_confirmacao varchar(64) NOT NULL, corpo_json nvarchar(max) NOT NULL, documento_json nvarchar(max) NOT NULL,
    emitente_cnpj varchar(14) NOT NULL, ambiente int NOT NULL, situacao varchar(24) NOT NULL,
    notazz_id varchar(100) NULL,
    documento_id bigint NULL CONSTRAINT fk_solicitacao_nfe_documento REFERENCES wms.documento_saida(id),
    documento_origem_id bigint NULL CONSTRAINT fk_solicitacao_nfe_origem REFERENCES wms.documento_saida(id),
    numero varchar(9) NULL, serie varchar(3) NULL, chave_acesso varchar(44) NULL, protocolo varchar(60) NULL,
    xml nvarchar(max) NULL, mensagem nvarchar(500) NOT NULL, usuario nvarchar(160) NOT NULL,
    criada_em datetime2(6) NOT NULL, atualizada_em datetime2(6) NOT NULL, ativa bit NOT NULL,
    regularizacao_operacao_id varchar(36) NULL, regularizada_em datetime2(6) NULL, pedido_entrada_id bigint NULL,
    CONSTRAINT ck_solicitacao_nfe_json CHECK(ISJSON(corpo_json)=1 AND ISJSON(documento_json)=1),
    CONSTRAINT ck_solicitacao_nfe_ambiente CHECK(ambiente IN(1,2)),
    CONSTRAINT ck_solicitacao_nfe_natureza CHECK(natureza IN('RETORNO_MERCADORIA','RETORNO_SIMBOLICO','REGULARIZACAO_ENTRADA')),
    CONSTRAINT ck_solicitacao_nfe_situacao CHECK(situacao IN('PREPARADA','ENVIANDO','PROCESSANDO','DESCONHECIDO','REJEITADA','AUTORIZADA','CANCELANDO','CANCELADA','CONFLITO')),
    CONSTRAINT uk_solicitacao_nfe_operacao UNIQUE(operacao_id,grupo),
    CONSTRAINT fk_solicitacao_nfe_entrada FOREIGN KEY(pedido_entrada_id) REFERENCES wms.pedido_entrada(id)
);
CREATE UNIQUE INDEX uk_solicitacao_nfe_ativa ON wms.solicitacao_nfe(pedido_id,natureza,grupo) WHERE ativa=1;
CREATE UNIQUE INDEX uk_solicitacao_nfe_remoto ON wms.solicitacao_nfe(emitente_cnpj,ambiente,notazz_id) WHERE notazz_id IS NOT NULL;
CREATE INDEX ix_solicitacao_nfe_pedido ON wms.solicitacao_nfe(pedido_id,criada_em);

CREATE TABLE wms.parcela_nfe (
    solicitacao_id varchar(36) NOT NULL CONSTRAINT fk_parcela_nfe_solicitacao REFERENCES wms.solicitacao_nfe(id),
    reserva_id bigint NOT NULL CONSTRAINT fk_parcela_nfe_reserva REFERENCES wms.reserva_saida(id),
    entrada_id bigint NOT NULL CONSTRAINT fk_parcela_nfe_entrada REFERENCES wms.entrada_conferida(id),
    item_nota_id bigint NOT NULL CONSTRAINT fk_parcela_nfe_item REFERENCES wms.item_nota_entrada(id),
    quantidade decimal(19,6) NOT NULL, valor_total decimal(19,2) NOT NULL,
    CONSTRAINT pk_parcela_nfe PRIMARY KEY(solicitacao_id,reserva_id,entrada_id),
    CONSTRAINT ck_parcela_nfe_valores CHECK(quantidade>0 AND valor_total>=0)
);
CREATE INDEX ix_parcela_nfe_item ON wms.parcela_nfe(item_nota_id,reserva_id) INCLUDE(quantidade,valor_total);
CREATE TABLE wms.evento_nfe (
    id bigint IDENTITY NOT NULL CONSTRAINT pk_evento_nfe PRIMARY KEY,
    solicitacao_id varchar(36) NOT NULL CONSTRAINT fk_evento_nfe_solicitacao REFERENCES wms.solicitacao_nfe(id),
    tipo varchar(72) NOT NULL, usuario nvarchar(160) NOT NULL, motivo nvarchar(500) NOT NULL, registrado_em datetime2(6) NOT NULL
);
CREATE INDEX ix_evento_nfe_solicitacao ON wms.evento_nfe(solicitacao_id,id);
CREATE TABLE wms.notazz_canal (
    emitente_cnpj varchar(14) NOT NULL, ambiente int NOT NULL, ocupado_ate datetime2(6) NOT NULL, dono varchar(36) NOT NULL,
    CONSTRAINT pk_notazz_canal PRIMARY KEY(emitente_cnpj,ambiente), CONSTRAINT ck_notazz_canal_ambiente CHECK(ambiente IN(1,2))
);
COMMIT TRANSACTION;
