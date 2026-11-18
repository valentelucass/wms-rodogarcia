-- Tarefas novas somente. Preserva operações e históricos anteriores.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

CREATE TABLE wms.coletor_tarefa (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_coletor_tarefa PRIMARY KEY,
    versao bigint NOT NULL CONSTRAINT df_coletor_versao DEFAULT 0,
    tipo varchar(16) NOT NULL,
    origem_id bigint NOT NULL,
    cliente_id bigint NOT NULL CONSTRAINT fk_coletor_cliente REFERENCES wms.cliente(id),
    armazem_id bigint NOT NULL CONSTRAINT fk_coletor_armazem REFERENCES wms.armazem(id),
    criada_em datetime2(6) NOT NULL,
    responsavel nvarchar(200) NULL,
    iniciada_em datetime2(6) NULL,
    CONSTRAINT uk_coletor_origem UNIQUE(tipo,origem_id),
    CONSTRAINT ck_coletor_tipo CHECK(tipo IN ('ENDERECAMENTO','SEPARACAO','INVENTARIO')),
    CONSTRAINT ck_coletor_versao CHECK(versao >= 0),
    CONSTRAINT ck_coletor_responsavel CHECK((responsavel IS NULL AND iniciada_em IS NULL) OR (responsavel IS NOT NULL AND iniciada_em IS NOT NULL))
);
CREATE INDEX ix_coletor_escopo ON wms.coletor_tarefa(cliente_id,armazem_id,tipo,id);

CREATE TABLE wms.coletor_operacao (
    id bigint IDENTITY(1,1) NOT NULL CONSTRAINT pk_coletor_operacao PRIMARY KEY,
    tarefa_id bigint NOT NULL CONSTRAINT fk_coletor_operacao_tarefa REFERENCES wms.coletor_tarefa(id),
    operacao_id varchar(36) NOT NULL,
    tipo varchar(24) NOT NULL,
    usuario nvarchar(200) NOT NULL,
    registrada_em datetime2(6) NOT NULL,
    conteudo_hash varchar(64) NOT NULL,
    resultado nvarchar(max) NOT NULL,
    CONSTRAINT uk_coletor_operacao UNIQUE(tarefa_id,operacao_id),
    CONSTRAINT ck_coletor_operacao_tipo CHECK(tipo IN ('INICIAR','LIBERAR','ENDERECAR','SEPARAR','CONTAR','FINALIZAR','CANCELAR_INVENTARIO')),
    CONSTRAINT ck_coletor_operacao_json CHECK(ISJSON(resultado)=1)
);
