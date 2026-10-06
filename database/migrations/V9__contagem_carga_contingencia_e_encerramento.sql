-- V9 BE14 preparada SOMENTE EM ARQUIVOS conforme schema/API formal doc31.
-- Fonte31 0A4D40: pareamento ENTRADA_CONTINGENCIA e CHECK de servico INATIVO cumulativos.
-- Guarda fisica/ref de resolucao e efeitoRegistradoNoWms sao dos servicos; JPA aguarda freeze.
-- V1-V8 intocadas; sem dados/defaults/grants/triggers/JPA/SQL/Flyway executados.
-- FK/ISJSON/CHECK locais nao provam contexto, locks, hash ou imutabilidade no servico.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

CREATE TABLE wms.contagem_estoque (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_contagem_estoque PRIMARY KEY,
    unidade_id bigint NOT NULL CONSTRAINT fk_contagem_estoque_unidade REFERENCES wms.unidade_logistica(id),
    versao bigint NOT NULL,
    revisao_atual int NOT NULL,
    impedimento bit NOT NULL,
    criada_em datetime2(6) NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_contagem_estoque_unidade UNIQUE (unidade_id),
    CONSTRAINT ck_contagem_estoque_revisao CHECK (revisao_atual >= 1)
);

CREATE TABLE wms.revisao_contagem (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_revisao_contagem PRIMARY KEY,
    contagem_id bigint NOT NULL CONSTRAINT fk_revisao_contagem_contagem REFERENCES wms.contagem_estoque(id),
    numero int NOT NULL,
    versao_unidade bigint NOT NULL,
    revisao_conteudo bigint NOT NULL,
    esperado decimal(19,6) NOT NULL,
    contado decimal(19,6) NOT NULL,
    diferenca decimal(19,6) NOT NULL,
    observado_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    situacao varchar(24) NOT NULL,
    origens_json nvarchar(max) NOT NULL,
    reserva_id bigint NULL CONSTRAINT fk_revisao_contagem_reserva REFERENCES wms.pedido_saida(id),
    aplicada_em datetime2(6) NULL,
    efeito_json nvarchar(max) NULL,
    CONSTRAINT uk_revisao_contagem_contagem_numero UNIQUE (contagem_id,numero),
    CONSTRAINT ck_revisao_contagem_numero CHECK (numero >= 1),
    CONSTRAINT ck_revisao_contagem_versao_unidade CHECK (versao_unidade >= 0),
    CONSTRAINT ck_revisao_contagem_revisao_conteudo CHECK (revisao_conteudo >= 0),
    CONSTRAINT ck_revisao_contagem_quantidades CHECK (esperado >= 0 AND contado >= 0),
    CONSTRAINT ck_revisao_contagem_diferenca CHECK (diferenca = contado - esperado),
    CONSTRAINT ck_revisao_contagem_situacao CHECK (situacao IN ('PENDENTE','PENDENTE_RESERVA','RECONCILIADA','SUBSTITUIDA','APLICADA')),
    CONSTRAINT ck_revisao_contagem_origens_json CHECK (ISJSON(origens_json) = 1),
    CONSTRAINT ck_revisao_contagem_efeito_json CHECK (efeito_json IS NULL OR ISJSON(efeito_json) = 1),
    CONSTRAINT ck_revisao_contagem_aplicacao CHECK (((aplicada_em IS NULL AND efeito_json IS NULL) OR (aplicada_em IS NOT NULL AND efeito_json IS NOT NULL)) AND (situacao <> 'APLICADA' OR (aplicada_em IS NOT NULL AND efeito_json IS NOT NULL)))
);

CREATE TABLE wms.carga_inicial (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_carga_inicial PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_carga_inicial_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_carga_inicial_armazem REFERENCES wms.armazem(id),
    produto_id bigint NOT NULL CONSTRAINT fk_carga_inicial_produto REFERENCES wms.produto(id),
    referencia varchar(100) NOT NULL,
    etiqueta_fornecida nvarchar(200) NULL,
    quantidade decimal(19,6) NOT NULL,
    versao bigint NOT NULL,
    revisao_atual int NOT NULL,
    situacao varchar(24) NOT NULL,
    criada_em datetime2(6) NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    entrada_id bigint NULL CONSTRAINT fk_carga_inicial_entrada REFERENCES wms.entrada_conferida(id),
    CONSTRAINT uk_carga_inicial_cliente_armazem_referencia UNIQUE (cliente_id,armazem_id,referencia),
    CONSTRAINT ck_carga_inicial_quantidade CHECK (quantidade > 0),
    CONSTRAINT ck_carga_inicial_revisao CHECK (revisao_atual >= 1),
    CONSTRAINT ck_carga_inicial_situacao CHECK (situacao IN ('PENDENTE','PREPARADA','REGULARIZADA','CANCELADA')),
    CONSTRAINT ck_carga_inicial_regularizacao CHECK ((situacao IN ('PREPARADA','REGULARIZADA') AND entrada_id IS NOT NULL) OR (situacao = 'PENDENTE' AND entrada_id IS NULL) OR situacao = 'CANCELADA')
);

CREATE TABLE wms.revisao_carga_inicial (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_revisao_carga_inicial PRIMARY KEY,
    carga_id bigint NOT NULL CONSTRAINT fk_revisao_carga_inicial_carga REFERENCES wms.carga_inicial(id),
    numero int NOT NULL,
    dados_json nvarchar(max) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    CONSTRAINT uk_revisao_carga_inicial_carga_numero UNIQUE (carga_id,numero),
    CONSTRAINT ck_revisao_carga_inicial_numero CHECK (numero >= 1),
    CONSTRAINT ck_revisao_carga_inicial_dados_json CHECK (ISJSON(dados_json) = 1)
);

CREATE TABLE wms.linha_contingencia (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_linha_contingencia PRIMARY KEY,
    identidade_fato varchar(200) NOT NULL,
    cliente_id bigint NOT NULL CONSTRAINT fk_linha_contingencia_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_linha_contingencia_armazem REFERENCES wms.armazem(id),
    tipo varchar(24) NOT NULL,
    conteudo_json nvarchar(max) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    ocorrida_em datetime2(6) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    operador nvarchar(200) NOT NULL,
    fonte nvarchar(500) NOT NULL,
    versao bigint NOT NULL,
    situacao varchar(24) NOT NULL,
    pendencia nvarchar(500) NULL,
    resultado_json nvarchar(max) NULL,
    conciliada_em datetime2(6) NULL,
    CONSTRAINT uk_linha_contingencia_identidade_fato UNIQUE (identidade_fato),
    CONSTRAINT ck_linha_contingencia_tipo CHECK (tipo IN ('CHEGADA','RESERVA','SEPARACAO','RETIRADA','RETORNO','ENTRADA','AVARIA','FATO_SERVICO','CONTAGEM','AJUSTE','REMANEJAMENTO')),
    CONSTRAINT ck_linha_contingencia_situacao CHECK (situacao IN ('PENDENTE','CONCILIADA')),
    CONSTRAINT ck_linha_contingencia_conteudo_json CHECK (ISJSON(conteudo_json) = 1),
    CONSTRAINT ck_linha_contingencia_resultado_json CHECK (resultado_json IS NULL OR ISJSON(resultado_json) = 1),
    CONSTRAINT ck_linha_contingencia_conciliacao CHECK ((situacao = 'PENDENTE' AND conciliada_em IS NULL AND resultado_json IS NULL) OR (situacao = 'CONCILIADA' AND conciliada_em IS NOT NULL AND resultado_json IS NOT NULL))
);

CREATE TABLE wms.dependencia_contingencia (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_dependencia_contingencia PRIMARY KEY,
    linha_id bigint NOT NULL CONSTRAINT fk_dependencia_contingencia_linha REFERENCES wms.linha_contingencia(id),
    depende_id bigint NOT NULL CONSTRAINT fk_dependencia_contingencia_depende REFERENCES wms.linha_contingencia(id),
    CONSTRAINT uk_dependencia_contingencia_linha_depende UNIQUE (linha_id,depende_id),
    CONSTRAINT ck_dependencia_contingencia_distinta CHECK (linha_id <> depende_id)
);

CREATE TABLE wms.configuracao_aviso_validade (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_configuracao_aviso_validade PRIMARY KEY,
    cliente_id bigint NOT NULL CONSTRAINT fk_configuracao_aviso_validade_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_configuracao_aviso_validade_armazem REFERENCES wms.armazem(id),
    versao bigint NOT NULL,
    dias_antecedencia int NOT NULL,
    alterada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_configuracao_aviso_validade_cliente_armazem UNIQUE (cliente_id,armazem_id),
    CONSTRAINT ck_configuracao_aviso_validade_dias CHECK (dias_antecedencia >= 0)
);

CREATE TABLE wms.resolucao_remanescente (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_resolucao_remanescente PRIMARY KEY,
    pedido_saida_id bigint NOT NULL CONSTRAINT fk_resolucao_remanescente_pedido_saida REFERENCES wms.pedido_saida(id),
    cliente_id bigint NOT NULL CONSTRAINT fk_resolucao_remanescente_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_resolucao_remanescente_armazem REFERENCES wms.armazem(id),
    carga_id bigint NULL CONSTRAINT fk_resolucao_remanescente_carga REFERENCES wms.carga_inicial(id),
    unidades_json nvarchar(max) NOT NULL,
    motivo nvarchar(500) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    criada_em datetime2(6) NOT NULL,
    CONSTRAINT uk_resolucao_remanescente_pedido_saida UNIQUE (pedido_saida_id),
    CONSTRAINT ck_resolucao_remanescente_unidades_json CHECK (ISJSON(unidades_json) = 1)
);

-- Opcoes de sessao SQL Server para o indice filtrado; sem execucao local.
SET ANSI_NULLS ON;
SET QUOTED_IDENTIFIER ON;
SET ANSI_PADDING ON;
SET ANSI_WARNINGS ON;
SET CONCAT_NULL_YIELDS_NULL ON;
SET ARITHABORT ON;
SET NUMERIC_ROUNDABORT OFF;
CREATE INDEX ix_revisao_contagem_reserva ON wms.revisao_contagem(reserva_id);
CREATE INDEX ix_carga_inicial_armazem ON wms.carga_inicial(armazem_id);
CREATE INDEX ix_carga_inicial_produto ON wms.carga_inicial(produto_id);
CREATE INDEX ix_linha_contingencia_cliente ON wms.linha_contingencia(cliente_id);
CREATE INDEX ix_linha_contingencia_armazem ON wms.linha_contingencia(armazem_id);
CREATE INDEX ix_dependencia_contingencia_depende ON wms.dependencia_contingencia(depende_id);
CREATE INDEX ix_configuracao_aviso_validade_armazem ON wms.configuracao_aviso_validade(armazem_id);
CREATE INDEX ix_resolucao_remanescente_cliente ON wms.resolucao_remanescente(cliente_id);
CREATE INDEX ix_resolucao_remanescente_armazem ON wms.resolucao_remanescente(armazem_id);
CREATE INDEX ix_resolucao_remanescente_carga ON wms.resolucao_remanescente(carga_id);
CREATE UNIQUE INDEX ux_carga_inicial_entrada ON wms.carga_inicial(entrada_id) WHERE entrada_id IS NOT NULL;

-- Dominios cumulativos; AJUSTE_ESTOQUE e movimento/auditoria, nao tipo administrativo presumido.

-- VARCHAR(24) ja definido por V7; revalidar dominio formal no V9 sem coluna nova.
ALTER TABLE wms.servico_cobranca DROP CONSTRAINT ck_servico_cobranca_situacao;
ALTER TABLE wms.servico_cobranca WITH CHECK ADD CONSTRAINT ck_servico_cobranca_situacao CHECK (
    situacao IN ('ATIVO','ENCERRAMENTO_PENDENTE','INATIVO')
);

ALTER TABLE wms.operacao_administrativa DROP CONSTRAINT ck_operacao_administrativa_tipo;
ALTER TABLE wms.operacao_administrativa WITH CHECK ADD CONSTRAINT ck_operacao_administrativa_tipo CHECK (
    tipo IN ('CRIACAO_SERVICO','CRIACAO_TABELA','CRIACAO','COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA','PREPARACAO_FECHAMENTO','APROVACAO_FECHAMENTO','REJEICAO_FECHAMENTO','REABERTURA_FECHAMENTO','ENTREGA_ESL','CONFIRMACAO_EXTERNA','REFERENCIA_NFSE','RESOLUCAO_FINANCEIRA','AJUSTE_FECHAMENTO','TRATATIVA_EXTERNA','LEITURA_CONTAGEM','APLICACAO_CONTAGEM','REGISTRO_CARGA','REVISAO_CARGA','CONFIRMACAO_CARGA','CANCELAMENTO_CARGA','REGISTRO_CONTINGENCIA','CONCILIACAO_CONTINGENCIA','CONFIGURACAO_VALIDADE','INATIVACAO_DEFINITIVA','RESOLUCAO_REMANESCENTE','PREPARACAO_CARGA','SOLICITACAO_ENCERRAMENTO','ENTRADA_CONTINGENCIA')
);

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_tipo CHECK (
    tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','PEDIDO_ENTRADA','CONJUNTO_POSICOES','PEDIDO_SAIDA','SERVICO_COBRANCA','TABELA_COBRANCA','CONTRATO_COBRANCA','FATO_SERVICO','AVARIA_FINANCEIRA','CALCULO_COBRANCA','FECHAMENTO_COBRANCA','CONTAGEM_ESTOQUE','CARGA_INICIAL','CONTINGENCIA','AVISO_VALIDADE')
);

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_acao CHECK (
    acao IN ('CRIACAO','ALTERACAO','SOLICITAR_ENCERRAMENTO','REATIVACAO','NOTA_INCLUIDA','XML_VINCULADO','CONFERENCIA_INICIADA','CHEGADA_REGISTRADA','CHEGADA_ESTORNADA','ENTRADA_EFETIVADA','PEDIDO_CANCELADO','UNIDADES_CRIADAS','UNIDADE_DIVIDIDA','UNIDADES_REAGRUPADAS','CONFIGURAR_CAPACIDADE','ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE','CRIACAO_SAIDA','JUSTIFICATIVA_FIFO','RESERVA_SAIDA','CANCELAMENTO_SAIDA','REVERSAO_RESERVA','AVARIA_ESTOQUE','LEITURA_SAIDA','SEPARACAO_SAIDA','DOCUMENTO_SAIDA','CANCELAMENTO_DOCUMENTO','RETIRADA_FISICA','RETORNO_INTERNO','DEVOLUCAO_SAIDA','DEVOLUCAO_ENTRADA','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA','COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA','PREPARACAO_FECHAMENTO','APROVACAO_FECHAMENTO','REJEICAO_FECHAMENTO','REABERTURA_FECHAMENTO','ENTREGA_ESL','CONFIRMACAO_EXTERNA','REFERENCIA_NFSE','RESOLUCAO_FINANCEIRA','AJUSTE_FECHAMENTO','TRATATIVA_EXTERNA','LEITURA_CONTAGEM','APLICACAO_CONTAGEM','REGISTRO_CARGA','REVISAO_CARGA','CONFIRMACAO_CARGA','CANCELAMENTO_CARGA','REGISTRO_CONTINGENCIA','CONCILIACAO_CONTINGENCIA','CONFIGURACAO_VALIDADE','INATIVACAO_DEFINITIVA','RESOLUCAO_REMANESCENTE','AJUSTE_ESTOQUE','PREPARACAO_CARGA','SOLICITACAO_ENCERRAMENTO')
);

ALTER TABLE wms.movimento_estoque DROP CONSTRAINT ck_movimento_acao;
ALTER TABLE wms.movimento_estoque WITH CHECK ADD CONSTRAINT ck_movimento_acao CHECK (
    acao IN ('ENDERECAMENTO','MOVIMENTACAO','BLOQUEIO_ESTOQUE','LIBERACAO_ESTOQUE','AVARIA_ESTOQUE','AVARIA_DETALHADA','RESPONSABILIDADE_AVARIA','REPARO_AVARIA','AJUSTE_ESTOQUE')
);

-- Ampliar pares V7: SERVICO/INATIVACAO e CONTRATO/ENCERRAMENTO_VIGENCIA conforme doc31.

ALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_financeira_tipo_acao;
ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_financeira_tipo_acao CHECK (
    (acao NOT IN ('COMPLEMENTO_FISCAL','REFERENCIA_FISCAL','PREVIA_ENDERECOS','IMPORTACAO_ENDERECOS','ENCERRAMENTO_VIGENCIA','CONFIGURACAO','REGISTRO_SERVICO','ANULACAO_SERVICO','MARCO_AVARIA','CALCULO','VINCULO_TABELA')
        OR (acao = 'COMPLEMENTO_FISCAL' AND tipo IN ('CLIENTE','ARMAZEM'))
        OR (acao = 'REFERENCIA_FISCAL' AND tipo IN ('PRODUTO'))
        OR (acao = 'PREVIA_ENDERECOS' AND tipo IN ('ARMAZEM'))
        OR (acao = 'IMPORTACAO_ENDERECOS' AND tipo IN ('ARMAZEM'))
        OR (acao = 'ENCERRAMENTO_VIGENCIA' AND tipo IN ('TABELA_COBRANCA','CONTRATO_COBRANCA'))
        OR (acao = 'CONFIGURACAO' AND tipo IN ('CONTRATO_COBRANCA'))
        OR (acao = 'REGISTRO_SERVICO' AND tipo IN ('FATO_SERVICO'))
        OR (acao = 'ANULACAO_SERVICO' AND tipo IN ('FATO_SERVICO'))
        OR (acao = 'MARCO_AVARIA' AND tipo IN ('AVARIA_FINANCEIRA'))
        OR (acao = 'CALCULO' AND tipo IN ('CALCULO_COBRANCA'))
        OR (acao = 'VINCULO_TABELA' AND tipo IN ('CONTRATO_COBRANCA')))
    AND (tipo NOT IN ('SERVICO_COBRANCA','TABELA_COBRANCA','CONTRATO_COBRANCA','FATO_SERVICO','AVARIA_FINANCEIRA','CALCULO_COBRANCA')
        OR (tipo = 'SERVICO_COBRANCA' AND acao IN ('CRIACAO','INATIVACAO_DEFINITIVA','SOLICITACAO_ENCERRAMENTO'))
        OR (tipo = 'TABELA_COBRANCA' AND acao IN ('CRIACAO','ENCERRAMENTO_VIGENCIA'))
        OR (tipo = 'CONTRATO_COBRANCA' AND acao IN ('CONFIGURACAO','VINCULO_TABELA','ENCERRAMENTO_VIGENCIA'))
        OR (tipo = 'FATO_SERVICO' AND acao IN ('REGISTRO_SERVICO','ANULACAO_SERVICO'))
        OR (tipo = 'AVARIA_FINANCEIRA' AND acao IN ('MARCO_AVARIA'))
        OR (tipo = 'CALCULO_COBRANCA' AND acao IN ('CALCULO')))
);

ALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_be14_tipo_acao CHECK (
    (acao NOT IN ('LEITURA_CONTAGEM','APLICACAO_CONTAGEM','REGISTRO_CARGA','REVISAO_CARGA','CONFIRMACAO_CARGA','CANCELAMENTO_CARGA','REGISTRO_CONTINGENCIA','CONCILIACAO_CONTINGENCIA','CONFIGURACAO_VALIDADE','INATIVACAO_DEFINITIVA','RESOLUCAO_REMANESCENTE','AJUSTE_ESTOQUE','PREPARACAO_CARGA','SOLICITACAO_ENCERRAMENTO') OR (acao = 'LEITURA_CONTAGEM' AND tipo IN ('CONTAGEM_ESTOQUE')) OR (acao = 'APLICACAO_CONTAGEM' AND tipo IN ('CONTAGEM_ESTOQUE')) OR (acao = 'REGISTRO_CARGA' AND tipo IN ('CARGA_INICIAL')) OR (acao = 'REVISAO_CARGA' AND tipo IN ('CARGA_INICIAL')) OR (acao = 'CONFIRMACAO_CARGA' AND tipo IN ('CARGA_INICIAL')) OR (acao = 'CANCELAMENTO_CARGA' AND tipo IN ('CARGA_INICIAL')) OR (acao = 'REGISTRO_CONTINGENCIA' AND tipo IN ('CONTINGENCIA')) OR (acao = 'CONCILIACAO_CONTINGENCIA' AND tipo IN ('CONTINGENCIA')) OR (acao = 'CONFIGURACAO_VALIDADE' AND tipo IN ('AVISO_VALIDADE')) OR (acao = 'INATIVACAO_DEFINITIVA' AND tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','CONJUNTO_POSICOES','SERVICO_COBRANCA')) OR (acao = 'RESOLUCAO_REMANESCENTE' AND tipo IN ('PEDIDO_SAIDA')) OR (acao = 'AJUSTE_ESTOQUE' AND tipo IN ('PEDIDO_ENTRADA')) OR (acao = 'PREPARACAO_CARGA' AND tipo IN ('CARGA_INICIAL')) OR (acao = 'SOLICITACAO_ENCERRAMENTO' AND tipo IN ('CLIENTE','ARMAZEM','PRODUTO','EMBALAGEM','ENDERECO','CONJUNTO_POSICOES','SERVICO_COBRANCA'))) AND (tipo NOT IN ('CONTAGEM_ESTOQUE','CARGA_INICIAL','CONTINGENCIA','AVISO_VALIDADE') OR (tipo = 'CONTAGEM_ESTOQUE' AND acao IN ('LEITURA_CONTAGEM','APLICACAO_CONTAGEM')) OR (tipo = 'CARGA_INICIAL' AND acao IN ('REGISTRO_CARGA','REVISAO_CARGA','PREPARACAO_CARGA','CONFIRMACAO_CARGA','CANCELAMENTO_CARGA')) OR (tipo = 'CONTINGENCIA' AND acao IN ('REGISTRO_CONTINGENCIA','CONCILIACAO_CONTINGENCIA')) OR (tipo = 'AVISO_VALIDADE' AND acao IN ('CONFIGURACAO_VALIDADE')))
);
-- Pares de saida V6 e fechamento V8 permanecem sem ALTER.
-- Nenhum dado historico modificado; imutabilidade e dependencia ausente nao recebem trigger/tabela extra.
