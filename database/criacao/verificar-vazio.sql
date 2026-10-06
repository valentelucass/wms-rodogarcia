-- Somente metadados agregados. Nao retorna nomes de objetos/contas ou dados.
-- CONTROL no contexto do DBA/criador garante visibilidade completa de seguranca,
-- inclusive roles fixas e SQL Server2022+. Nao e privilegio para a aplicacao.
SELECT DB_NAME() AS banco,
    HAS_PERMS_BY_NAME(DB_NAME(),N'DATABASE',N'CONTROL') AS metadataCompleta,
    (SELECT COUNT_BIG(*) FROM sys.objects WHERE is_ms_shipped=0) AS objetos,
    (SELECT COUNT_BIG(*) FROM sys.types WHERE is_user_defined=1) AS tipos,
    (SELECT COUNT_BIG(*) FROM sys.schemas WHERE name NOT IN
        (N'dbo',N'guest',N'INFORMATION_SCHEMA',N'sys',N'db_owner',N'db_accessadmin',
         N'db_securityadmin',N'db_ddladmin',N'db_backupoperator',N'db_datareader',
         N'db_datawriter',N'db_denydatareader',N'db_denydatawriter')) AS schemas,
    (SELECT COUNT_BIG(*) FROM sys.database_principals
        WHERE principal_id>4 AND is_fixed_role=0) AS principais,
    -- dbo em db_owner e o vinculo padrao observado em model; outros membros continuam bloqueados.
    (SELECT COUNT_BIG(*) FROM sys.database_role_members
        WHERE NOT (role_principal_id=16384 AND member_principal_id=1)) AS membros,
    (SELECT COUNT_BIG(*) FROM sys.database_permissions WHERE NOT (
        (class=1 AND major_id<0 AND grantee_principal_id=0 AND state=N'G')
        OR (class=0 AND grantee_principal_id=1 AND permission_name=N'CONNECT' AND state=N'G')
        OR (class=0 AND grantee_principal_id=0 AND state=N'G' AND permission_name IN
            (N'VIEW ANY COLUMN MASTER KEY DEFINITION',N'VIEW ANY COLUMN ENCRYPTION KEY DEFINITION'))
    )) AS permissoes;
