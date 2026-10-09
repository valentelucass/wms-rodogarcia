-- Preparado para revisao. Nao executado; exige autorizacao especifica DEV.
-- Somente depois da V11, pelo executor administrativo protegido WMS.
IF DB_NAME() <> N'WMS_DEV' THROW 51032, 'Este contrato exige WMS_DEV.', 1;
IF USER_ID(N'WMSDEV') IS NULL THROW 51032, 'Identidade WMSDEV ausente.', 1;
IF OBJECT_ID(N'wms.usuario_acesso', N'U') IS NULL THROW 51032, 'V11 ausente.', 1;

GRANT SELECT, INSERT ON OBJECT::wms.usuario_acesso TO WMSDEV;
GRANT UPDATE (nome, senha_hash, perfil, administrador, ativo, trocar_senha,
    versao_tokens, falhas, bloqueado_ate, clientes, armazens, versao)
    ON OBJECT::wms.usuario_acesso TO WMSDEV;
GRANT SELECT, INSERT ON OBJECT::wms.sessao_acesso TO WMSDEV;
GRANT UPDATE (revogada) ON OBJECT::wms.sessao_acesso TO WMSDEV;
GRANT SELECT, INSERT ON OBJECT::wms.renovacao_acesso TO WMSDEV;
GRANT UPDATE (usado) ON OBJECT::wms.renovacao_acesso TO WMSDEV;
GRANT SELECT, INSERT ON OBJECT::wms.evento_acesso TO WMSDEV;
-- Sem DELETE, DDL, roles, grants amplos ou UPDATE de principal/email/identidades/auditoria.
