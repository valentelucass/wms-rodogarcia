-- FE12-INVENTARIO-UX02 / BE14-INVENTARIO01. Ativacao WMS_DEV autorizada em 23/10/2026.
-- Executar somente pelo fluxo protegido de migrations com alvo/historico conferidos.
-- wms.inventario.enabled permanece false até validação/ativação do ambiente.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;
SET XACT_ABORT ON;
BEGIN TRANSACTION;

CREATE TABLE wms.inventario (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_inventario PRIMARY KEY,
    versao bigint NOT NULL,
    cliente_id bigint NOT NULL CONSTRAINT fk_inventario_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_inventario_armazem REFERENCES wms.armazem(id),
    tipo varchar(12) NOT NULL,
    produto_id bigint NULL CONSTRAINT fk_inventario_produto REFERENCES wms.produto(id),
    endereco_id bigint NULL CONSTRAINT fk_inventario_endereco REFERENCES wms.endereco(id),
    situacao varchar(20) NOT NULL,
    iniciado_em datetime2(6) NOT NULL,
    encerrado_em datetime2(6) NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    divergencias int NULL,
    relatorio_json nvarchar(max) NULL,
    CONSTRAINT ck_inventario_tipo CHECK (
        (tipo='CEGAS' AND produto_id IS NULL AND endereco_id IS NULL) OR
        (tipo='SKU' AND produto_id IS NOT NULL AND endereco_id IS NULL) OR
        (tipo='POSICAO' AND produto_id IS NULL AND endereco_id IS NOT NULL)),
    CONSTRAINT ck_inventario_situacao CHECK (situacao IN ('EM_ANDAMENTO','CONCLUIDO','CANCELADO')),
    CONSTRAINT ck_inventario_relatorio CHECK (relatorio_json IS NULL OR ISJSON(relatorio_json)=1)
);
CREATE INDEX ix_inventario_contexto ON wms.inventario(cliente_id,armazem_id,id DESC);

CREATE TABLE wms.inventario_item (
    inventario_id bigint NOT NULL CONSTRAINT fk_inventario_item REFERENCES wms.inventario(id),
    codigo_unidade varchar(36) NOT NULL,
    base_json nvarchar(max) NOT NULL,
    prevista bit NOT NULL,
    contado decimal(19,6) NULL,
    endereco_lido bigint NULL CONSTRAINT fk_inventario_leitura_endereco REFERENCES wms.endereco(id),
    posicao_lida nvarchar(40) NULL,
    lido_em datetime2(6) NULL,
    usuario nvarchar(200) NULL,
    CONSTRAINT pk_inventario_item PRIMARY KEY(inventario_id,codigo_unidade),
    CONSTRAINT ck_inventario_item_base CHECK (ISJSON(base_json)=1),
    CONSTRAINT ck_inventario_item_quantidade CHECK (contado IS NULL OR contado>=0),
    CONSTRAINT ck_inventario_item_leitura CHECK ((contado IS NULL AND lido_em IS NULL) OR (contado IS NOT NULL AND lido_em IS NOT NULL))
);
CREATE INDEX ix_inventario_leituras ON wms.inventario_item(inventario_id,lido_em DESC) WHERE contado IS NOT NULL;

-- Preservar integralmente a expressão atual das constraints, ampliando só os tipos novos.
DECLARE @operacao nvarchar(max)=(SELECT definition FROM sys.check_constraints
    WHERE parent_object_id=OBJECT_ID(N'wms.operacao_administrativa') AND name=N'ck_operacao_administrativa_tipo');
DECLARE @auditoria nvarchar(max)=(SELECT definition FROM sys.check_constraints
    WHERE parent_object_id=OBJECT_ID(N'wms.auditoria_cadastro') AND name=N'ck_auditoria_tipo');
IF @operacao IS NULL OR @auditoria IS NULL
    THROW 51000, 'Constraints de operacao/auditoria nao correspondem ao catalogo WMS esperado.', 1;
ALTER TABLE wms.operacao_administrativa DROP CONSTRAINT ck_operacao_administrativa_tipo;
DECLARE @sql nvarchar(max)=N'ALTER TABLE wms.operacao_administrativa WITH CHECK ADD CONSTRAINT ck_operacao_administrativa_tipo CHECK (('
    +@operacao+N') OR tipo IN (''INICIO_INVENTARIO'',''LEITURA_INVENTARIO'',''FIM_INVENTARIO'',''CANCELAMENTO_INVENTARIO''));';
EXEC sys.sp_executesql @sql;
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
SET @sql=N'ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_tipo CHECK (('
    +@auditoria+N') OR tipo=''INVENTARIO'');';
EXEC sys.sp_executesql @sql;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_inventario CHECK (
    tipo<>'INVENTARIO' OR acao IN ('CRIACAO','ALTERACAO'));
COMMIT TRANSACTION;
