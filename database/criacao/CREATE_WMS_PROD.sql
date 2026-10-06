-- Somente banco vazio. Nenhuma migration/carga/GRANT/configuracao.
IF DB_NAME() <> N'master' OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) <> @ServidorEsperado
    OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) IS NULL
    OR ORIGINAL_LOGIN() <> @LoginEsperado
    THROW 51020, 'D20 identidade de criacao divergente.', 1;
IF NOT EXISTS (SELECT 1 FROM sys.databases WHERE name = N'WMS_DEV' AND state = 0)
    THROW 51022, 'WMS_DEV online deve ser comprovado antes de PROD.', 1;
IF DB_ID(N'WMS_PROD') IS NOT NULL
    THROW 51021, 'WMS_PROD existente: nao sobrescrever.', 1;
IF @@TRANCOUNT <> 0
    THROW 51023, 'CREATE exige autocommit sem transacao ativa.', 1;
SET IMPLICIT_TRANSACTIONS OFF;
IF EXISTS (SELECT 1 FROM [model].sys.objects WHERE is_ms_shipped = 0)
    THROW 51024, 'Model tem objetos de usuario: nao copiar para WMS.', 1;
CREATE DATABASE [WMS_PROD];
