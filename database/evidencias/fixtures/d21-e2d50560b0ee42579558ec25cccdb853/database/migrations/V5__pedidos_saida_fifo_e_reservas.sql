-- SQL Server: arquivo preparado, aplicar somente pelo procedimento explicito do WMS.
-- Schema e mapeamentos BE01/BE09: docs/24-pedido-saida-fifo-e-reserva.md.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

-- Opcoes exigidas pelo indice unico filtrado de reservas ativas.
SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

CREATE TABLE wms.pedido_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_pedido_saida PRIMARY KEY,
    versao bigint NOT NULL,
    cliente_id bigint NOT NULL CONSTRAINT fk_pedido_saida_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_pedido_saida_armazem REFERENCES wms.armazem(id),
    referencia varchar(40) NOT NULL,
    situacao varchar(24) NOT NULL,
    criado_em datetime2(6) NOT NULL,
    alterado_em datetime2(6) NOT NULL,
    CONSTRAINT uk_pedido_saida_referencia UNIQUE (cliente_id, armazem_id, referencia),
    CONSTRAINT ck_pedido_saida_versao CHECK (versao >= 0),
    CONSTRAINT ck_pedido_saida_situacao CHECK (situacao IN ('RASCUNHO','RESERVADO','CANCELADO'))
);

CREATE TABLE wms.item_pedido_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_item_pedido_saida PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_item_saida_pedido REFERENCES wms.pedido_saida(id),
    produto_id bigint NOT NULL CONSTRAINT fk_item_saida_produto REFERENCES wms.produto(id),
    quantidade decimal(19,6) NOT NULL,
    CONSTRAINT uk_item_saida_produto UNIQUE (pedido_id, produto_id),
    CONSTRAINT ck_item_saida_quantidade CHECK (quantidade > 0)
);

CREATE TABLE wms.reserva_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_reserva_saida PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_reserva_saida_pedido REFERENCES wms.pedido_saida(id),
    item_id bigint NOT NULL CONSTRAINT fk_reserva_saida_item REFERENCES wms.item_pedido_saida(id),
    unidade_id bigint NOT NULL CONSTRAINT fk_reserva_saida_unidade REFERENCES wms.unidade_logistica(id),
    operacao_reserva_id varchar(36) NOT NULL,
    quantidade decimal(19,6) NOT NULL,
    situacao varchar(16) NOT NULL,
    criada_em datetime2(6) NOT NULL,
    encerrada_em datetime2(6) NULL,
    CONSTRAINT uk_reserva_saida_operacao_unidade UNIQUE (pedido_id, operacao_reserva_id, unidade_id),
    CONSTRAINT ck_reserva_saida_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_reserva_saida_situacao CHECK (situacao IN ('ATIVA','CANCELADA','REVERTIDA')),
    CONSTRAINT ck_reserva_saida_encerramento CHECK (
        (situacao = 'ATIVA' AND encerrada_em IS NULL)
        OR (situacao IN ('CANCELADA','REVERTIDA') AND encerrada_em IS NOT NULL)
    )
);

CREATE TABLE wms.operacao_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_operacao_saida PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_operacao_saida_pedido REFERENCES wms.pedido_saida(id),
    operacao_id varchar(36) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    tipo varchar(24) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    resultado nvarchar(max) NOT NULL,
    CONSTRAINT uk_operacao_saida UNIQUE (pedido_id, operacao_id),
    CONSTRAINT ck_operacao_saida_json CHECK (ISJSON(resultado) = 1)
);

-- A ponte aponta para o PEDIDO de saida, nao para uma linha historica de reserva.
-- Unidades existentes recebem ponte nula e sinalizador falso, sem alterar conteudo/datas.
ALTER TABLE wms.unidade_logistica ADD
    reserva_saida_id bigint NULL,
    avaria_posterior bit NOT NULL CONSTRAINT df_unidade_avaria_posterior DEFAULT (0);
GO
-- Separar lotes permite usar as colunas recem-adicionadas no SQL Server.
ALTER TABLE wms.unidade_logistica WITH CHECK ADD
    CONSTRAINT fk_unidade_reserva_saida FOREIGN KEY (reserva_saida_id) REFERENCES wms.pedido_saida(id),
    CONSTRAINT ck_unidade_avaria_posterior CHECK (avaria_posterior = 0 OR bloqueada = 1);

CREATE INDEX ix_pedido_saida_escopo ON wms.pedido_saida(cliente_id, armazem_id, id);
CREATE INDEX ix_item_saida_produto ON wms.item_pedido_saida(produto_id);
CREATE INDEX ix_reserva_saida_pedido ON wms.reserva_saida(pedido_id, situacao, id);
CREATE INDEX ix_reserva_saida_item ON wms.reserva_saida(item_id);
CREATE INDEX ix_unidade_reserva_saida ON wms.unidade_logistica(reserva_saida_id);

-- AC10: exclusividade da unidade fisica enquanto qualquer parte estiver reservada.
-- Linhas CANCELADA/REVERTIDA preservam historia sem impedir outra operacao de reserva.
CREATE UNIQUE INDEX uk_reserva_saida_unidade_ativa ON wms.reserva_saida(unidade_id)
    INCLUDE (pedido_id, item_id, quantidade) WHERE situacao = 'ATIVA';

-- Preservar todos os tipos/acoes de V1-V4 e acrescentar somente os de BE09.
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_tipo CHECK (
    tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','PEDIDO_ENTRADA','CONJUNTO_POSICOES','PEDIDO_SAIDA')
);
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_acao CHECK (
    acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO',
        'NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA',
        'ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS',
        'CONFIGURAR_CAPACIDADE','ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE',
        'CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE')
);
-- Vincular as novas acoes ao tipo correto, sem restringir pares anteriores.
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_saida_tipo_acao CHECK (
    acao NOT IN ('CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE')
    OR (tipo = 'PEDIDO_SAIDA' AND acao IN ('CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA'))
    OR (tipo = 'PEDIDO_ENTRADA' AND acao = 'AVARIA_ESTOQUE')
);

-- Avaria posterior usa o historico/repeticao fisico de BE08 e o tipo PEDIDO_ENTRADA.
ALTER TABLE wms.movimento_estoque DROP CONSTRAINT ck_movimento_acao;
ALTER TABLE wms.movimento_estoque WITH CHECK ADD CONSTRAINT ck_movimento_acao CHECK (
    acao IN ('ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE','AVARIA_ESTOQUE')
);
-- Sem DELETE CASCADE, sementes, concessoes de acesso ou baixa fisica/fiscal/cobranca.
