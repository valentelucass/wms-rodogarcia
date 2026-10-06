-- Diagnostico preparado: somente leitura; executar no WMS_DEV autorizado.
-- Parametro @ServidorEsperado vinculado pelo cliente, nao substituicao textual.
IF DB_NAME() <> N'WMS_DEV' OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) <> @ServidorEsperado
    OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) IS NULL
    THROW 51030,'D20 alvo do diagnostico divergente.',1;
SELECT CONVERT(varchar(30),SERVERPROPERTY('ProductVersion')) AS versao,
    CONVERT(nvarchar(128),DATABASEPROPERTYEX(DB_NAME(),'Collation')) AS collation,
    compatibility_level,is_read_committed_snapshot_on,snapshot_isolation_state,
    recovery_model_desc FROM sys.databases WHERE database_id=DB_ID();
SELECT SESSIONPROPERTY('ANSI_NULLS') AS ANSI_NULLS,SESSIONPROPERTY('ANSI_PADDING') AS ANSI_PADDING,
    SESSIONPROPERTY('ANSI_WARNINGS') AS ANSI_WARNINGS,SESSIONPROPERTY('ARITHABORT') AS ARITHABORT,
    SESSIONPROPERTY('CONCAT_NULL_YIELDS_NULL') AS CONCAT_NULL_YIELDS_NULL,
    SESSIONPROPERTY('QUOTED_IDENTIFIER') AS QUOTED_IDENTIFIER,
    SESSIONPROPERTY('NUMERIC_ROUNDABORT') AS NUMERIC_ROUNDABORT,@@TRANCOUNT AS transacoes;
SELECT name,is_disabled,is_not_trusted FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms');
SELECT name,is_disabled,is_not_trusted,delete_referential_action,update_referential_action
    FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms');
SELECT OBJECT_NAME(object_id) AS tabela,name,is_unique,filter_definition,is_disabled
    FROM sys.indexes WHERE object_id IN (SELECT object_id FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms')) AND index_id>0;
SELECT installed_rank,version,script,checksum,success FROM wms.flyway_schema_history ORDER BY installed_rank;
-- Principios tecnicos testados separadamente por sessao app/migration, sem nomes/logins/segredos.
SELECT IS_SRVROLEMEMBER(N'sysadmin') AS sysadmin,IS_MEMBER(N'db_owner') AS db_owner,
    HAS_PERMS_BY_NAME(DB_NAME(),N'DATABASE',N'CONTROL') AS controla_banco,
    HAS_PERMS_BY_NAME(N'wms',N'SCHEMA',N'ALTER') AS altera_schema,
    HAS_PERMS_BY_NAME(N'wms.auditoria_cadastro',N'OBJECT',N'DELETE') AS delete_auditoria,
    HAS_PERMS_BY_NAME(N'wms.auditoria_cadastro',N'OBJECT',N'UPDATE') AS update_auditoria;
