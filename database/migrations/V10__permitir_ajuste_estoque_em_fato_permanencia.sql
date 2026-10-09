-- D27: preparado apos SELECT administrativo real. Aguarda revisao/sinal Farol.
-- Somente WMS_DEV; transacao gerenciada pelo Flyway (executeInTransaction=true).
-- Preservar V1-V9/checksums/historico/dados. Executor DEV-only, nunca launcher.
IF DB_NAME() <> N'WMS_DEV'
    THROW 51000, 'Proposta V10 restrita a WMS_DEV.', 1;
IF @@TRANCOUNT = 0
    THROW 51000, 'Proposta V10 exige transacao da ferramenta aprovada.', 1;

DECLARE @def nvarchar(max), @normalizada nvarchar(max);
DECLARE @formaIN nvarchar(max) = N'tipoIN''SEPARACAO'',''RETORNO_INTERNO'',''RETIRADA'',''AVARIA'',''REPARO''';
DECLARE @formaOR nvarchar(max) = N'tipo=''REPARO''ORtipo=''AVARIA''ORtipo=''RETIRADA''ORtipo=''RETORNO_INTERNO''ORtipo=''SEPARACAO''';
SELECT @def = definition
FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'wms.fato_permanencia')
  AND name = N'ck_fato_permanencia_tipo'
  AND is_disabled = 0 AND is_not_trusted = 0;
IF @def IS NULL
    THROW 51000, 'CHECK ausente, divergente ou definicao sem visibilidade.', 1;

-- VIG12: duas formas exatas, IN da V6 e OR na ordem explicita acima.
-- Nao aceitar toda ordem nem remover operadores, nomes ou valores.
-- D27 preflight confirmou a forma OR exata acima, CHECK habilitado/confiavel.
-- Exigir os literais originais evita aceitar espacos removidos dentro de valores.
IF CHARINDEX(N'''SEPARACAO''', @def) = 0
 OR CHARINDEX(N'''RETORNO_INTERNO''', @def) = 0
 OR CHARINDEX(N'''RETIRADA''', @def) = 0
 OR CHARINDEX(N'''AVARIA''', @def) = 0
 OR CHARINDEX(N'''REPARO''', @def) = 0
    THROW 51000, 'Tipos preexistentes divergem da fonte V6 congelada.', 1;
SET @normalizada = @def;
-- Somente brackets do identificador conhecido, antes de remover brancos.
SET @normalizada = REPLACE(@normalizada,N'[tipo]',N'tipo');
SET @normalizada = REPLACE(@normalizada,N'(',N'');
SET @normalizada = REPLACE(@normalizada,N')',N'');
SET @normalizada = REPLACE(@normalizada,N' ',N'');
SET @normalizada = REPLACE(@normalizada,NCHAR(9),N'');
SET @normalizada = REPLACE(@normalizada,NCHAR(10),N'');
SET @normalizada = REPLACE(@normalizada,NCHAR(13),N'');
-- Comparacao binaria local da expressao, sem mudar collation de banco/servidor.
IF CONVERT(varbinary(max), @normalizada) <> CONVERT(varbinary(max), @formaIN)
 AND CONVERT(varbinary(max), @normalizada) <> CONVERT(varbinary(max), @formaOR)
    THROW 51000, 'Expressao CHECK diferente do predicado canonico; preservar.', 1;

ALTER TABLE wms.fato_permanencia DROP CONSTRAINT ck_fato_permanencia_tipo;
ALTER TABLE wms.fato_permanencia WITH CHECK
ADD CONSTRAINT ck_fato_permanencia_tipo CHECK (
    tipo IN ('SEPARACAO','RETORNO_INTERNO','RETIRADA','AVARIA','REPARO','AJUSTE_ESTOQUE')
);
IF NOT EXISTS (
    SELECT 1 FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'wms.fato_permanencia')
      AND name = N'ck_fato_permanencia_tipo'
      AND is_disabled = 0 AND is_not_trusted = 0
      AND definition LIKE N'%''AJUSTE_ESTOQUE''%'
)
    THROW 51000, 'Pos-condicao CHECK habilitado/confiavel nao confirmada.', 1;

-- Conferir o dominio completo antes do commit, nao apenas a presenca do sexto tipo.
SELECT @def = definition FROM sys.check_constraints
WHERE parent_object_id = OBJECT_ID(N'wms.fato_permanencia')
  AND name=N'ck_fato_permanencia_tipo' AND is_disabled=0 AND is_not_trusted=0;
SET @normalizada=REPLACE(@def,N'[tipo]',N'tipo');
SET @normalizada=REPLACE(@normalizada,N'(',N'');
SET @normalizada=REPLACE(@normalizada,N')',N'');
SET @normalizada=REPLACE(@normalizada,N' ',N'');
SET @normalizada=REPLACE(@normalizada,NCHAR(9),N'');
SET @normalizada=REPLACE(@normalizada,NCHAR(10),N'');
SET @normalizada=REPLACE(@normalizada,NCHAR(13),N'');
IF @normalizada IS NULL OR (
 CONVERT(varbinary(max),@normalizada)<>CONVERT(varbinary(max),N'tipoIN''SEPARACAO'',''RETORNO_INTERNO'',''RETIRADA'',''AVARIA'',''REPARO'',''AJUSTE_ESTOQUE''')
 AND CONVERT(varbinary(max),@normalizada)<>CONVERT(varbinary(max),N'tipo=''AJUSTE_ESTOQUE''ORtipo=''REPARO''ORtipo=''AVARIA''ORtipo=''RETIRADA''ORtipo=''RETORNO_INTERNO''ORtipo=''SEPARACAO''')
)
 THROW 51000, 'Pos-condicao dominio CHECK completo divergente; reverter transacao.', 1;

