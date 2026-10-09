-- D32. Preparada; aplicar somente pelo procedimento Flyway autorizado.
-- Nenhuma senha, conta inicial ou chave privada integra a migration.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

CREATE TABLE wms.usuario_acesso (
    id varchar(36) NOT NULL CONSTRAINT pk_usuario_acesso PRIMARY KEY,
    nome nvarchar(200) NOT NULL,
    email nvarchar(254) NOT NULL CONSTRAINT uk_usuario_acesso_email UNIQUE,
    senha_hash varchar(255) NOT NULL,
    perfil varchar(20) NOT NULL,
    administrador bit NOT NULL,
    principal bit NOT NULL,
    ativo bit NOT NULL,
    trocar_senha bit NOT NULL,
    versao_tokens bigint NOT NULL,
    falhas int NOT NULL,
    bloqueado_ate datetime2(6) NULL,
    clientes nvarchar(max) NOT NULL,
    armazens nvarchar(max) NOT NULL,
    versao bigint NOT NULL,
    CONSTRAINT ck_usuario_acesso_perfil CHECK (perfil IN ('GESTOR','SUPERVISOR','OPERACAO')),
    CONSTRAINT ck_usuario_acesso_contadores CHECK (versao_tokens >= 0 AND falhas >= 0 AND versao >= 0),
    CONSTRAINT ck_usuario_acesso_principal CHECK (
        (principal = 1 AND id = '00000000-0000-4000-8000-000000000001'
            AND email = N'desenvolvedor@rodogarcia.com.br' AND ativo = 1
            AND administrador = 1 AND perfil = 'GESTOR')
        OR (principal = 0 AND id <> '00000000-0000-4000-8000-000000000001'
            AND email <> N'desenvolvedor@rodogarcia.com.br'))
);
CREATE UNIQUE INDEX uk_usuario_acesso_principal ON wms.usuario_acesso(principal) WHERE principal = 1;

CREATE TABLE wms.sessao_acesso (
    id varchar(36) NOT NULL CONSTRAINT pk_sessao_acesso PRIMARY KEY,
    usuario_id varchar(36) NOT NULL,
    versao_tokens bigint NOT NULL,
    expira datetime2(6) NOT NULL,
    revogada bit NOT NULL,
    CONSTRAINT fk_sessao_acesso_usuario FOREIGN KEY (usuario_id) REFERENCES wms.usuario_acesso(id)
);
CREATE INDEX ix_sessao_acesso_usuario ON wms.sessao_acesso(usuario_id, expira);

CREATE TABLE wms.renovacao_acesso (
    hash varchar(64) NOT NULL CONSTRAINT pk_renovacao_acesso PRIMARY KEY,
    sessao_id varchar(36) NOT NULL,
    expira datetime2(6) NOT NULL,
    usado bit NOT NULL,
    CONSTRAINT fk_renovacao_acesso_sessao FOREIGN KEY (sessao_id) REFERENCES wms.sessao_acesso(id)
);
CREATE INDEX ix_renovacao_acesso_sessao ON wms.renovacao_acesso(sessao_id, expira);

CREATE TABLE wms.evento_acesso (
    id varchar(36) NOT NULL CONSTRAINT pk_evento_acesso PRIMARY KEY,
    ator varchar(36) NOT NULL,
    alvo varchar(36) NOT NULL,
    acao varchar(40) NOT NULL,
    instante datetime2(6) NOT NULL,
    CONSTRAINT fk_evento_acesso_ator FOREIGN KEY (ator) REFERENCES wms.usuario_acesso(id),
    CONSTRAINT fk_evento_acesso_alvo FOREIGN KEY (alvo) REFERENCES wms.usuario_acesso(id)
);
CREATE INDEX ix_evento_acesso_alvo ON wms.evento_acesso(alvo, instante);
