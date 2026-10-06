-- SQL Server: arquivo preparado, aplicar somente pelo procedimento explicito do WMS.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

ALTER TABLE wms.endereco ADD tipo_unidade_permitido varchar(16) NULL;
GO
ALTER TABLE wms.endereco ADD CONSTRAINT ck_endereco_perfil CHECK (
    tipo_unidade_permitido IS NULL OR (
        tipo_unidade_permitido IN ('PALLET','BOBINA','VOLUME')
        AND capacidade_peso_kg IS NOT NULL AND altura_metros IS NOT NULL
        AND largura_metros IS NOT NULL AND profundidade_metros IS NOT NULL
        AND empilhamento_maximo IS NOT NULL
    )
);

CREATE TABLE wms.conjunto_posicoes (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_conjunto_posicoes PRIMARY KEY,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    armazem_id bigint NOT NULL CONSTRAINT fk_conjunto_armazem REFERENCES wms.armazem(id),
    codigo varchar(40) NOT NULL,
    endereco_a_id bigint NOT NULL CONSTRAINT fk_conjunto_endereco_a REFERENCES wms.endereco(id),
    endereco_b_id bigint NOT NULL CONSTRAINT fk_conjunto_endereco_b REFERENCES wms.endereco(id),
    capacidade_peso_kg decimal(19,3) NOT NULL,
    altura_metros decimal(10,3) NOT NULL,
    largura_metros decimal(10,3) NOT NULL,
    profundidade_metros decimal(10,3) NOT NULL,
    empilhamento_maximo int NOT NULL,
    CONSTRAINT uk_conjunto_codigo UNIQUE (armazem_id, codigo),
    CONSTRAINT ck_conjunto_situacao CHECK (situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE')),
    CONSTRAINT ck_conjunto_versao CHECK (versao >= 0),
    CONSTRAINT ck_conjunto_ordem CHECK (endereco_a_id < endereco_b_id),
    CONSTRAINT ck_conjunto_limites CHECK (capacidade_peso_kg > 0 AND altura_metros > 0
        AND largura_metros > 0 AND profundidade_metros > 0 AND empilhamento_maximo BETWEEN 1 AND 999)
);

ALTER TABLE wms.unidade_logistica ADD
    revisao_conteudo bigint NOT NULL CONSTRAINT df_unidade_revisao_conteudo DEFAULT (0),
    peso_kg decimal(19,3) NULL,
    altura_metros decimal(10,3) NULL,
    largura_metros decimal(10,3) NULL,
    profundidade_metros decimal(10,3) NULL,
    empilhamento int NULL,
    posicoes_necessarias int NULL,
    tipo_localizacao varchar(16) NULL,
    conjunto_atual_id bigint NULL CONSTRAINT fk_unidade_conjunto REFERENCES wms.conjunto_posicoes(id),
    bloqueada bit NOT NULL CONSTRAINT df_unidade_bloqueada DEFAULT (0),
    primeiro_enderecamento_em datetime2(6) NULL,
    inicio_armazenagem_em datetime2(6) NULL,
    posicoes_equivalentes int NOT NULL CONSTRAINT df_unidade_posicoes DEFAULT (0);
GO
-- Preservar a revisao impressa antes de separar conteudo e localizacao (BE07).
-- GO separa os lotes para o SQL Server reconhecer as colunas recem-adicionadas.
UPDATE wms.unidade_logistica SET revisao_conteudo = versao;

ALTER TABLE wms.unidade_logistica ADD
    CONSTRAINT ck_unidade_revisao_conteudo CHECK (revisao_conteudo >= 0),
    CONSTRAINT ck_unidade_medidas CHECK (
        (peso_kg IS NULL AND altura_metros IS NULL AND largura_metros IS NULL
         AND profundidade_metros IS NULL AND empilhamento IS NULL AND posicoes_necessarias IS NULL)
        OR (peso_kg IS NOT NULL AND altura_metros IS NOT NULL AND largura_metros IS NOT NULL
            AND profundidade_metros IS NOT NULL AND empilhamento IS NOT NULL AND posicoes_necessarias IS NOT NULL
            AND peso_kg > 0 AND altura_metros > 0 AND largura_metros > 0 AND profundidade_metros > 0
            AND empilhamento BETWEEN 1 AND 999 AND posicoes_necessarias IN (1,2))
    ),
    CONSTRAINT ck_unidade_localizacao CHECK (
        (tipo_localizacao IS NULL AND primeiro_enderecamento_em IS NULL AND peso_kg IS NULL AND conjunto_atual_id IS NULL)
        OR (tipo_localizacao IS NOT NULL AND tipo_localizacao IN ('ARMAZENAGEM','TRIAGEM','QUARENTENA')
            AND primeiro_enderecamento_em IS NOT NULL AND peso_kg IS NOT NULL
            AND ((posicoes_necessarias = 1 AND conjunto_atual_id IS NULL)
                 OR (posicoes_necessarias = 2 AND conjunto_atual_id IS NOT NULL)))
    ),
    CONSTRAINT ck_unidade_inicio_armazenagem CHECK (
        (inicio_armazenagem_em IS NULL AND posicoes_equivalentes = 0)
        OR (inicio_armazenagem_em IS NOT NULL AND primeiro_enderecamento_em IS NOT NULL
            AND inicio_armazenagem_em >= primeiro_enderecamento_em
            AND posicoes_equivalentes IN (1,2) AND posicoes_equivalentes = posicoes_necessarias)
    ),
    CONSTRAINT ck_unidade_quarentena CHECK (tipo_localizacao <> 'QUARENTENA' OR bloqueada = 1);

CREATE TABLE wms.ocupacao_endereco (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_ocupacao_endereco PRIMARY KEY,
    endereco_id bigint NOT NULL CONSTRAINT fk_ocupacao_endereco REFERENCES wms.endereco(id),
    unidade_id bigint NULL CONSTRAINT fk_ocupacao_unidade REFERENCES wms.unidade_logistica(id),
    CONSTRAINT uk_ocupacao_endereco UNIQUE (endereco_id)
);

CREATE TABLE wms.movimento_estoque (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_movimento_estoque PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_movimento_pedido REFERENCES wms.pedido_entrada(id),
    unidade_id bigint NOT NULL CONSTRAINT fk_movimento_unidade REFERENCES wms.unidade_logistica(id),
    operacao_id varchar(36) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    acao varchar(24) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    instante datetime2(6) NOT NULL,
    dados_antes nvarchar(max) NOT NULL,
    resultado nvarchar(max) NOT NULL,
    CONSTRAINT uk_movimento_operacao UNIQUE (pedido_id, operacao_id),
    CONSTRAINT ck_movimento_acao CHECK (acao IN ('ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE')),
    CONSTRAINT ck_movimento_json CHECK (ISJSON(dados_antes) = 1 AND ISJSON(resultado) = 1)
);

CREATE INDEX ix_ocupacao_unidade ON wms.ocupacao_endereco(unidade_id, endereco_id);
CREATE INDEX ix_movimento_unidade ON wms.movimento_estoque(unidade_id, id);
CREATE INDEX ix_conjunto_endereco_a ON wms.conjunto_posicoes(endereco_a_id, situacao);
CREATE INDEX ix_conjunto_endereco_b ON wms.conjunto_posicoes(endereco_b_id, situacao);
CREATE INDEX ix_unidade_conjunto ON wms.unidade_logistica(conjunto_atual_id);

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
ALTER TABLE wms.auditoria_cadastro ADD CONSTRAINT ck_auditoria_tipo CHECK (tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','PEDIDO_ENTRADA','CONJUNTO_POSICOES'));
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro ADD CONSTRAINT ck_auditoria_acao CHECK (acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO','NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA','ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS','CONFIGURAR_CAPACIDADE','ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE'));
