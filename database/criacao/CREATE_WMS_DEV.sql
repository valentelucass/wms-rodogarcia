-- D20: executar somente pelo executor guardado, no alvo/acesso confirmados.
-- Parametros vinculados pelo SqlCommand; nao substituir texto livre.
IF DB_NAME() <> N'master' OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) <> @ServidorEsperado
    OR CONVERT(nvarchar(128),SERVERPROPERTY('ServerName')) IS NULL
    OR ORIGINAL_LOGIN() <> @LoginEsperado
    THROW 51020, 'D20 identidade de criacao divergente.', 1;
IF DB_ID(N'WMS_DEV') IS NOT NULL
    THROW 51021, 'WMS_DEV existente: nao sobrescrever.', 1;
IF @@TRANCOUNT <> 0
    THROW 51023, 'CREATE exige autocommit sem transacao ativa.', 1;
SET IMPLICIT_TRANSACTIONS OFF;
IF EXISTS (SELECT 1 FROM [model].sys.objects WHERE is_ms_shipped = 0)
    THROW 51024, 'Model tem objetos de usuario: nao copiar para WMS.', 1;
CREATE DATABASE [WMS_DEV];
