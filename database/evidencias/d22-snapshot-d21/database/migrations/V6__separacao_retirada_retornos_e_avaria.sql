-- SQL Server: preparacao D19 em arquivos; nao autoriza conexao ou aplicacao.
-- Schema inicial: docs/27-separacao-retirada-retornos-e-avaria.md.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

SET ANSI_NULLS ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET ARITHABORT ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET QUOTED_IDENTIFIER ON;
SET NUMERIC_ROUNDABORT OFF;

-- Expansoes preservam os estados e dados aceitos por V1-V5.
ALTER TABLE wms.pedido_saida DROP CONSTRAINT ck_pedido_saida_situacao;
ALTER TABLE wms.pedido_saida WITH CHECK ADD CONSTRAINT ck_pedido_saida_situacao CHECK (
    situacao IN ('RASCUNHO','RESERVADO','EM_SEPARACAO','SEPARADO','RETIRADO','CANCELADO')
);
ALTER TABLE wms.reserva_saida DROP CONSTRAINT ck_reserva_saida_situacao;
ALTER TABLE wms.reserva_saida WITH CHECK ADD CONSTRAINT ck_reserva_saida_situacao CHECK (
    situacao IN ('ATIVA','CANCELADA','REVERTIDA','RETIRADA')
);
ALTER TABLE wms.reserva_saida DROP CONSTRAINT ck_reserva_saida_encerramento;
ALTER TABLE wms.reserva_saida WITH CHECK ADD CONSTRAINT ck_reserva_saida_encerramento CHECK (
    (situacao = 'ATIVA' AND encerrada_em IS NULL)
    OR (situacao IN ('CANCELADA','REVERTIDA','RETIRADA') AND encerrada_em IS NOT NULL)
);

-- Separar move a unidade inteira: mesmas medidas, conjunto e inicio de permanencia.
-- A quantidade ativa/zerada continua regida por ck_unidade_quantidade de V3.
ALTER TABLE wms.unidade_logistica DROP CONSTRAINT ck_unidade_localizacao;
ALTER TABLE wms.unidade_logistica WITH CHECK ADD CONSTRAINT ck_unidade_localizacao CHECK (
    (tipo_localizacao IS NULL AND primeiro_enderecamento_em IS NULL AND peso_kg IS NULL AND conjunto_atual_id IS NULL)
    OR (tipo_localizacao IS NOT NULL AND tipo_localizacao IN ('ARMAZENAGEM','TRIAGEM','QUARENTENA','SEPARACAO')
        AND primeiro_enderecamento_em IS NOT NULL AND peso_kg IS NOT NULL
        AND ((posicoes_necessarias = 1 AND conjunto_atual_id IS NULL)
             OR (posicoes_necessarias = 2 AND conjunto_atual_id IS NOT NULL)))
);

-- Reparo inicial e explicito; nao reescrever a condicao original nem inferir reparos antigos.
ALTER TABLE wms.unidade_logistica ADD
    avaria_inicial_reparada bit NOT NULL CONSTRAINT df_unidade_avaria_inicial_reparada DEFAULT (0);
GO
ALTER TABLE wms.unidade_logistica WITH CHECK ADD CONSTRAINT ck_unidade_avaria_inicial_reparada CHECK (
    avaria_inicial_reparada = 0 OR condicao = 'AVARIADA'
);

CREATE TABLE wms.separacao_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_separacao_saida PRIMARY KEY,
    reserva_id bigint NOT NULL CONSTRAINT fk_separacao_reserva REFERENCES wms.reserva_saida(id),
    versao_unidade_lida bigint NOT NULL,
    revisao_conteudo_lida bigint NOT NULL,
    lida_em datetime2(6) NOT NULL,
    separada_em datetime2(6) NULL,
    encerrada_em datetime2(6) NULL,
    situacao varchar(16) NOT NULL,
    origens_json nvarchar(max) NULL,
    conjunto_origem_id bigint NULL CONSTRAINT fk_separacao_conjunto REFERENCES wms.conjunto_posicoes(id),
    CONSTRAINT uk_separacao_reserva UNIQUE (reserva_id),
    -- O model usa -1 apenas em LEITURA para invalidar leitura apos reparo.
    CONSTRAINT ck_separacao_revisoes CHECK (
        (versao_unidade_lida >= 0 OR (versao_unidade_lida = -1 AND situacao = 'LEITURA'))
        AND revisao_conteudo_lida >= 0
    ),
    CONSTRAINT ck_separacao_situacao CHECK (situacao IN ('LEITURA','SEPARADA','RETIRADA','RETORNADA')),
    CONSTRAINT ck_separacao_origens_json CHECK (origens_json IS NULL OR ISJSON(origens_json) = 1),
    CONSTRAINT ck_separacao_snapshot CHECK (
        (separada_em IS NULL AND origens_json IS NULL AND conjunto_origem_id IS NULL
            AND situacao IN ('LEITURA','RETORNADA'))
        OR (separada_em IS NOT NULL AND origens_json IS NOT NULL AND ISJSON(origens_json) = 1)
    ),
    CONSTRAINT ck_separacao_encerramento CHECK (
        (situacao IN ('LEITURA','SEPARADA') AND encerrada_em IS NULL)
        OR (situacao IN ('RETIRADA','RETORNADA') AND encerrada_em IS NOT NULL)
    )
);

CREATE TABLE wms.documento_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_documento_saida PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_documento_saida_pedido REFERENCES wms.pedido_saida(id),
    origem varchar(16) NOT NULL,
    natureza varchar(24) NOT NULL,
    emitente_cnpj varchar(14) NOT NULL,
    serie varchar(3) NOT NULL,
    numero varchar(9) NOT NULL,
    emissao date NOT NULL,
    chave_acesso varchar(44) NULL,
    protocolo nvarchar(60) NOT NULL,
    xml_hash varchar(64) NULL,
    situacao varchar(16) NOT NULL,
    registrado_em datetime2(6) NOT NULL,
    cancelado_em datetime2(6) NULL,
    CONSTRAINT uk_documento_saida_identidade UNIQUE (emitente_cnpj, serie, numero),
    CONSTRAINT ck_documento_saida_origem CHECK (origem IN ('NOTAZZ','XML')),
    CONSTRAINT ck_documento_saida_natureza CHECK (natureza IN ('RETORNO_MERCADORIA','RETORNO_SIMBOLICO')),
    CONSTRAINT ck_documento_saida_situacao CHECK (situacao IN ('AUTORIZADO','CANCELADO')),
    CONSTRAINT ck_documento_saida_cancelamento CHECK (
        (situacao = 'AUTORIZADO' AND cancelado_em IS NULL)
        OR (situacao = 'CANCELADO' AND cancelado_em IS NOT NULL)
    )
);
-- Varias autorizacoes sem chave sao permitidas; chave informada e historicamente unica.
CREATE UNIQUE INDEX uk_documento_saida_chave ON wms.documento_saida(chave_acesso)
    WHERE chave_acesso IS NOT NULL;

CREATE TABLE wms.cobertura_documento_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_cobertura_documento_saida PRIMARY KEY,
    documento_id bigint NOT NULL CONSTRAINT fk_cobertura_documento REFERENCES wms.documento_saida(id),
    reserva_id bigint NOT NULL CONSTRAINT fk_cobertura_reserva REFERENCES wms.reserva_saida(id),
    quantidade decimal(19,6) NOT NULL,
    CONSTRAINT uk_cobertura_documento_reserva UNIQUE (documento_id, reserva_id),
    CONSTRAINT ck_cobertura_quantidade CHECK (quantidade > 0)
);

CREATE TABLE wms.retirada_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_retirada_saida PRIMARY KEY,
    pedido_id bigint NOT NULL CONSTRAINT fk_retirada_pedido REFERENCES wms.pedido_saida(id),
    retirada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    comprovante_hash varchar(64) NOT NULL,
    CONSTRAINT uk_retirada_pedido UNIQUE (pedido_id)
);

CREATE TABLE wms.baixa_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_baixa_saida PRIMARY KEY,
    retirada_id bigint NOT NULL CONSTRAINT fk_baixa_retirada REFERENCES wms.retirada_saida(id),
    reserva_id bigint NOT NULL CONSTRAINT fk_baixa_reserva REFERENCES wms.reserva_saida(id),
    entrada_origem_id bigint NOT NULL CONSTRAINT fk_baixa_entrada REFERENCES wms.entrada_conferida(id),
    quantidade decimal(19,6) NOT NULL,
    data_fifo datetime2(6) NOT NULL,
    inicio_armazenagem_em datetime2(6) NULL,
    posicoes_equivalentes decimal(19,6) NOT NULL,
    CONSTRAINT uk_baixa_origem UNIQUE (retirada_id, reserva_id, entrada_origem_id),
    CONSTRAINT ck_baixa_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_baixa_equivalencia CHECK (posicoes_equivalentes >= 0)
);

CREATE TABLE wms.devolucao_saida (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_devolucao_saida PRIMARY KEY,
    baixa_id bigint NOT NULL CONSTRAINT fk_devolucao_baixa REFERENCES wms.baixa_saida(id),
    pedido_entrada_id bigint NOT NULL CONSTRAINT fk_devolucao_pedido REFERENCES wms.pedido_entrada(id),
    entrada_nova_id bigint NOT NULL CONSTRAINT fk_devolucao_entrada REFERENCES wms.entrada_conferida(id),
    quantidade decimal(19,6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_devolucao_entrada UNIQUE (entrada_nova_id),
    CONSTRAINT ck_devolucao_quantidade CHECK (quantidade > 0)
);

CREATE TABLE wms.avaria_estoque (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_avaria_estoque PRIMARY KEY,
    unidade_id bigint NOT NULL CONSTRAINT fk_avaria_unidade REFERENCES wms.unidade_logistica(id),
    quantidade decimal(19,6) NOT NULL,
    quantidade_base decimal(19,6) NOT NULL,
    -- Bases comprovadas do instante da ocorrencia, nao do saldo atual apos retirada.
    equivalencia_base decimal(19,6) NOT NULL,
    -- Um ciclo pode conter varias ocorrencias; nao e chave unica nem FK para outra tabela.
    ciclo_id varchar(36) NOT NULL,
    ocorrida_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    responsabilidade varchar(16) NOT NULL,
    destino_json nvarchar(max) NOT NULL,
    relato nvarchar(500) NOT NULL,
    reconhecida_em datetime2(6) NULL,
    validada_por nvarchar(200) NULL,
    resolvida_em datetime2(6) NULL,
    tratativa varchar(16) NOT NULL,
    versao bigint NOT NULL,
    bloqueio_previo bit NOT NULL,
    CONSTRAINT ck_avaria_quantidades CHECK (quantidade > 0 AND quantidade_base > 0 AND quantidade <= quantidade_base),
    CONSTRAINT ck_avaria_equivalencia_base CHECK (equivalencia_base >= 0),
    CONSTRAINT ck_avaria_versao CHECK (versao >= 0),
    CONSTRAINT ck_avaria_responsabilidade CHECK (responsabilidade IN ('PENDENTE','RODOGARCIA','PROPRIETARIO','TERCEIRO')),
    CONSTRAINT ck_avaria_destino_json CHECK (ISJSON(destino_json) = 1),
    CONSTRAINT ck_avaria_reconhecimento CHECK (
        (responsabilidade = 'PENDENTE' AND reconhecida_em IS NULL AND validada_por IS NULL)
        OR (responsabilidade IN ('RODOGARCIA','PROPRIETARIO','TERCEIRO')
            AND reconhecida_em IS NOT NULL AND validada_por IS NOT NULL)
    ),
    CONSTRAINT ck_avaria_tratativa CHECK (tratativa IN ('EM_TRATAMENTO','REPARADA')),
    CONSTRAINT ck_avaria_resolucao CHECK (
        (tratativa = 'EM_TRATAMENTO' AND resolvida_em IS NULL)
        OR (tratativa = 'REPARADA' AND resolvida_em IS NOT NULL)
    )
);

CREATE TABLE wms.fato_permanencia (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_fato_permanencia PRIMARY KEY,
    unidade_id bigint NOT NULL CONSTRAINT fk_fato_permanencia_unidade REFERENCES wms.unidade_logistica(id),
    operacao_id varchar(36) NOT NULL,
    tipo varchar(32) NOT NULL,
    ocorrida_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    quantidade_antes decimal(19,6) NOT NULL,
    quantidade_depois decimal(19,6) NOT NULL,
    equivalencia_antes decimal(19,6) NOT NULL,
    equivalencia_depois decimal(19,6) NOT NULL,
    CONSTRAINT uk_fato_permanencia UNIQUE (unidade_id, operacao_id, tipo),
    CONSTRAINT ck_fato_permanencia_tipo CHECK (tipo IN ('SEPARACAO','RETORNO_INTERNO','RETIRADA','AVARIA','REPARO')),
    CONSTRAINT ck_fato_permanencia_quantidades CHECK (quantidade_antes >= 0 AND quantidade_depois >= 0),
    CONSTRAINT ck_fato_permanencia_equivalencias CHECK (equivalencia_antes >= 0 AND equivalencia_depois >= 0)
);

-- Uniques ja indexam as primeiras FKs; estes indices atendem as outras direcoes.
CREATE INDEX ix_separacao_conjunto ON wms.separacao_saida(conjunto_origem_id);
CREATE INDEX ix_documento_saida_pedido ON wms.documento_saida(pedido_id, id) INCLUDE (situacao, natureza);
CREATE INDEX ix_cobertura_reserva ON wms.cobertura_documento_saida(reserva_id, documento_id) INCLUDE (quantidade);
CREATE INDEX ix_baixa_reserva ON wms.baixa_saida(reserva_id, id);
CREATE INDEX ix_baixa_entrada ON wms.baixa_saida(entrada_origem_id, id);
CREATE INDEX ix_devolucao_baixa ON wms.devolucao_saida(baixa_id, id) INCLUDE (quantidade);
CREATE INDEX ix_devolucao_pedido ON wms.devolucao_saida(pedido_entrada_id, id);
-- Repositorio atual consulta historico/abertas por unidade; ciclo e filtrado no servico.
-- Preservar esse indice nao unico, sem limitar a uma ocorrencia por ciclo/unidade.
CREATE INDEX ix_avaria_unidade ON wms.avaria_estoque(unidade_id, id) INCLUDE (tratativa, quantidade);
CREATE INDEX ix_fato_permanencia_tempo ON wms.fato_permanencia(unidade_id, ocorrida_em, id);

-- Pares confirmados por Farol no ask D19; conservar todas as acoes de V1-V5.
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_acao CHECK (
    acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO',
        'NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA',
        'ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS',
        'CONFIGURAR_CAPACIDADE','ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE',
        'CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE',
        'LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA','CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA',
        'RETORNO_INTERNO','DEVOLUCAO_SAIDA','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA')
);
ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_saida_tipo_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_saida_tipo_acao CHECK (
    acao NOT IN ('CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE',
        'LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA','CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA',
        'RETORNO_INTERNO','DEVOLUCAO_SAIDA','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA')
    OR (tipo = 'PEDIDO_SAIDA' AND acao IN ('CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA',
        'CANCELAMENTO_SAIDA','REVERSAO_RESERVA','LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA',
        'CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA','RETORNO_INTERNO','DEVOLUCAO_SAIDA'))
    OR (tipo = 'PEDIDO_ENTRADA' AND acao IN ('AVARIA_ESTOQUE','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA'))
);
-- Doc27: somente avaria/reconhecimento/reparo reutilizam movimento_estoque.
-- Expedicao usa operacao_saida e fato_permanencia; devolucao de entrada usa auditoria.
ALTER TABLE wms.movimento_estoque DROP CONSTRAINT ck_movimento_acao;
ALTER TABLE wms.movimento_estoque WITH CHECK ADD CONSTRAINT ck_movimento_acao CHECK (
    acao IN ('ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE','AVARIA_ESTOQUE',
        'AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA')
);
-- Sem sementes, grants, triggers, exclusao em cascata ou movimentacao de estoque/fiscal.
