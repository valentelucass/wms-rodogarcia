-- Preparada para SQL Server; ainda não aplicada no ambiente da empresa.
-- Flyway cria/controla somente o esquema wms no banco WMS previamente definido.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

CREATE TABLE wms.cliente (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_cliente PRIMARY KEY,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    codigo varchar(30) NOT NULL,
    nome nvarchar(120) NOT NULL,
    documento_fiscal varchar(30) NOT NULL,
    CONSTRAINT uk_cliente_1 UNIQUE (codigo),
    CONSTRAINT uk_cliente_2 UNIQUE (documento_fiscal),
    CONSTRAINT ck_cliente_situacao CHECK (situacao IN ('ATIVO', 'ENCERRAMENTO_PENDENTE'))
);

CREATE TABLE wms.armazem (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_armazem PRIMARY KEY,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    codigo varchar(30) NOT NULL,
    nome nvarchar(120) NOT NULL,
    documento_fiscal varchar(30) NOT NULL,
    cidade nvarchar(100) NOT NULL,
    uf varchar(2) NOT NULL,
    CONSTRAINT uk_armazem_1 UNIQUE (codigo),
    CONSTRAINT ck_armazem_situacao CHECK (situacao IN ('ATIVO', 'ENCERRAMENTO_PENDENTE'))
);

CREATE TABLE wms.produto (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_produto PRIMARY KEY,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    cliente_id bigint NOT NULL,
    sku varchar(40) NOT NULL,
    descricao nvarchar(160) NOT NULL,
    unidade_medida varchar(8) NOT NULL,
    tipo_quantidade varchar(10) NOT NULL,
    precisao_quantidade int NOT NULL,
    controla_lote bit NOT NULL,
    controla_validade bit NOT NULL,
    antecedencia_aviso_dias int NULL,
    CONSTRAINT fk_produto_cliente FOREIGN KEY (cliente_id) REFERENCES wms.cliente(id),
    CONSTRAINT uk_produto_1 UNIQUE (cliente_id, sku),
    CONSTRAINT ck_produto_situacao CHECK (situacao IN ('ATIVO', 'ENCERRAMENTO_PENDENTE')),
    CONSTRAINT ck_produto_precisao CHECK (
        tipo_quantidade IN ('CONTAGEM', 'MEDIDA') AND precisao_quantidade BETWEEN 0 AND 6
        AND (tipo_quantidade <> 'CONTAGEM' OR precisao_quantidade = 0)),
    CONSTRAINT ck_produto_validade CHECK (
        (controla_validade = 0 AND antecedencia_aviso_dias IS NULL)
        OR (controla_validade = 1 AND antecedencia_aviso_dias IS NOT NULL AND antecedencia_aviso_dias BETWEEN 0 AND 3650))
);

CREATE TABLE wms.embalagem (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_embalagem PRIMARY KEY,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    produto_id bigint NOT NULL,
    codigo_dun varchar(40) NOT NULL,
    descricao nvarchar(160) NOT NULL,
    quantidade_produto decimal(19,6) NOT NULL,
    CONSTRAINT fk_embalagem_produto FOREIGN KEY (produto_id) REFERENCES wms.produto(id),
    CONSTRAINT uk_embalagem_1 UNIQUE (produto_id, codigo_dun),
    CONSTRAINT ck_embalagem_situacao CHECK (situacao IN ('ATIVO', 'ENCERRAMENTO_PENDENTE')),
    CONSTRAINT ck_embalagem_quantidade CHECK (quantidade_produto > 0)
);

CREATE TABLE wms.endereco (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_endereco PRIMARY KEY,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    armazem_id bigint NOT NULL,
    codigo varchar(40) NOT NULL,
    rua varchar(20) NOT NULL,
    nivel int NOT NULL,
    posicao varchar(20) NOT NULL,
    descricao nvarchar(160) NOT NULL,
    tipo varchar(16) NOT NULL,
    capacidade_peso_kg decimal(19,3) NULL,
    altura_metros decimal(10,3) NULL,
    largura_metros decimal(10,3) NULL,
    profundidade_metros decimal(10,3) NULL,
    empilhamento_maximo int NULL,
    sequencia_coleta int NOT NULL,
    CONSTRAINT fk_endereco_armazem FOREIGN KEY (armazem_id) REFERENCES wms.armazem(id),
    CONSTRAINT uk_endereco_1 UNIQUE (armazem_id, codigo),
    CONSTRAINT uk_endereco_2 UNIQUE (armazem_id, rua, nivel, posicao),
    CONSTRAINT ck_endereco_situacao CHECK (situacao IN ('ATIVO', 'ENCERRAMENTO_PENDENTE')),
    CONSTRAINT ck_endereco_tipo CHECK (tipo IN ('ARMAZENAGEM', 'TRIAGEM', 'QUARENTENA', 'SEPARACAO')),
    CONSTRAINT ck_endereco_limites CHECK (nivel BETWEEN 0 AND 999 AND sequencia_coleta >= 0
        AND (capacidade_peso_kg IS NULL OR capacidade_peso_kg > 0)
        AND (altura_metros IS NULL OR altura_metros > 0)
        AND (largura_metros IS NULL OR largura_metros > 0)
        AND (profundidade_metros IS NULL OR profundidade_metros > 0)
        AND (empilhamento_maximo IS NULL OR empilhamento_maximo BETWEEN 1 AND 999))
);

CREATE TABLE wms.auditoria_cadastro (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_auditoria_cadastro PRIMARY KEY,
    tipo varchar(20) NOT NULL,
    registro_id bigint NOT NULL,
    acao varchar(30) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    instante datetime2(6) NOT NULL,
    id_operacao varchar(36) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    dados_antes nvarchar(max) NULL,
    dados_depois nvarchar(max) NOT NULL,
    CONSTRAINT ck_auditoria_tipo CHECK (tipo IN ('CLIENTE', 'ARMAZEM', 'PRODUTO', 'EMBALAGEM', 'ENDERECO')),
    CONSTRAINT ck_auditoria_acao CHECK (acao IN ('CRIACAO', 'ALTERACAO', 'SOLICITAR_ENCERRAMENTO', 'REATIVACAO')),
    CONSTRAINT ck_auditoria_json CHECK ((dados_antes IS NULL OR ISJSON(dados_antes) = 1) AND ISJSON(dados_depois) = 1)
);

CREATE INDEX ix_auditoria_registro ON wms.auditoria_cadastro(tipo, registro_id, id);
-- Sem DELETE CASCADE. Nenhuma tabela de estoque/fiscal/cobranca e nenhum seed real.
