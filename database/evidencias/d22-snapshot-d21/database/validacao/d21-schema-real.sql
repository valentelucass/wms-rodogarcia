-- Somente leitura, apos Flyway validate estrito. Dados de negocio nao sao lidos.
IF OBJECT_ID(N'wms.flyway_schema_history',N'U') IS NULL
    THROW 51032,'D21 historico Flyway ausente.',1;
SELECT DB_NAME() AS banco,
       CONVERT(int,COALESCE(HAS_PERMS_BY_NAME(DB_NAME(),N'DATABASE',N'CONTROL'),0)) AS metadataCompleta,
       CONVERT(int,CASE WHEN SCHEMA_ID(N'wms') IS NOT NULL THEN 1 ELSE 0 END) AS schemaWms,
       (SELECT COUNT(*) FROM sys.tables WHERE schema_id=SCHEMA_ID(N'wms') AND name<>N'flyway_schema_history') AS tabelas,
       (SELECT COUNT(*) FROM sys.columns c JOIN sys.tables t ON c.object_id=t.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history') AS colunas,
       (SELECT COUNT(*) FROM wms.flyway_schema_history WHERE success=1 AND type=N'SQL') AS historico,
       (SELECT COUNT(*) FROM wms.flyway_schema_history WHERE success=0) AS falhasHistorico,
       ((SELECT COUNT(*) FROM sys.foreign_keys WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1))+
        (SELECT COUNT(*) FROM sys.check_constraints WHERE schema_id=SCHEMA_ID(N'wms') AND (is_disabled=1 OR is_not_trusted=1))) AS restricoesInvalidas,
       (SELECT COUNT(*) FROM sys.indexes i JOIN sys.tables t ON i.object_id=t.object_id WHERE t.schema_id=SCHEMA_ID(N'wms') AND i.is_disabled=1) AS indicesDesabilitados;

-- Catalogo estrutural: nenhuma linha de negocio. History e excecao tecnica explicita.
SELECT N'T' categoria,t.name tabela,N'' nome,N'' valor
FROM sys.tables t WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history'
UNION ALL
SELECT N'COL',t.name,c.name,
       CONVERT(nvarchar(max),ty.name)+CASE
         WHEN ty.name IN (N'varchar',N'nvarchar') THEN N'('+CASE WHEN c.max_length=-1 THEN N'max' ELSE CONVERT(nvarchar(10),c.max_length/CASE WHEN ty.name=N'nvarchar' THEN 2 ELSE 1 END) END+N')'
         WHEN ty.name=N'decimal' THEN N'('+CONVERT(nvarchar(10),c.precision)+N','+CONVERT(nvarchar(10),c.scale)+N')'
         WHEN ty.name=N'datetime2' THEN N'('+CONVERT(nvarchar(10),c.scale)+N')' ELSE N'' END
       +N'|'+CONVERT(nvarchar(1),c.is_nullable)+N'|'+CONVERT(nvarchar(1),c.is_identity)
       +N'|'+COALESCE(CONVERT(nvarchar(30),CONVERT(bigint,idc.seed_value)),N'0')
       +N'|'+COALESCE(CONVERT(nvarchar(30),CONVERT(bigint,idc.increment_value)),N'0')
FROM sys.tables t JOIN sys.columns c ON c.object_id=t.object_id JOIN sys.types ty ON c.user_type_id=ty.user_type_id
LEFT JOIN sys.identity_columns idc ON idc.object_id=c.object_id AND idc.column_id=c.column_id
WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history'
UNION ALL
SELECT CASE WHEN k.type=N'PK' THEN N'PK' ELSE N'UNIQUE' END,t.name,k.name,
       STUFF((SELECT N','+c.name FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
              WHERE ic.object_id=t.object_id AND ic.index_id=k.unique_index_id AND ic.key_ordinal>0
              ORDER BY ic.key_ordinal FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,N'')
FROM sys.key_constraints k JOIN sys.tables t ON k.parent_object_id=t.object_id
WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history'
UNION ALL
SELECT N'FK',t.name,f.name,
       STUFF((SELECT N','+c.name FROM sys.foreign_key_columns fc JOIN sys.columns c ON c.object_id=fc.parent_object_id AND c.column_id=fc.parent_column_id
              WHERE fc.constraint_object_id=f.object_id ORDER BY fc.constraint_column_id FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,N'')
       +N'|'+OBJECT_SCHEMA_NAME(f.referenced_object_id)+N'.'+OBJECT_NAME(f.referenced_object_id)+N'|'
       +STUFF((SELECT N','+c.name FROM sys.foreign_key_columns fc JOIN sys.columns c ON c.object_id=fc.referenced_object_id AND c.column_id=fc.referenced_column_id
              WHERE fc.constraint_object_id=f.object_id ORDER BY fc.constraint_column_id FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,N'')
       +N'|'+CONVERT(nvarchar(1),f.delete_referential_action)+N'|'+CONVERT(nvarchar(1),f.update_referential_action)
FROM sys.foreign_keys f JOIN sys.tables t ON f.parent_object_id=t.object_id
WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history'
UNION ALL
SELECT N'CHECK',t.name,c.name,c.definition FROM sys.check_constraints c JOIN sys.tables t ON c.parent_object_id=t.object_id
WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history'
UNION ALL
SELECT N'DEFAULT',t.name,d.name,c.name+N'|'+d.definition
FROM sys.default_constraints d JOIN sys.tables t ON d.parent_object_id=t.object_id JOIN sys.columns c ON c.object_id=d.parent_object_id AND c.column_id=d.parent_column_id
WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history'
UNION ALL
SELECT N'IDX',t.name,i.name,CONVERT(nvarchar(max),i.is_unique)+N'|'
       +STUFF((SELECT N','+c.name FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
              WHERE ic.object_id=t.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0 ORDER BY ic.key_ordinal FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,N'')+N'|'
       +STUFF((SELECT N','+CONVERT(nvarchar(1),ic.is_descending_key) FROM sys.index_columns ic
              WHERE ic.object_id=t.object_id AND ic.index_id=i.index_id AND ic.key_ordinal>0 ORDER BY ic.key_ordinal FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,N'')+N'|'
       +COALESCE(STUFF((SELECT N','+c.name FROM sys.index_columns ic JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
              WHERE ic.object_id=t.object_id AND ic.index_id=i.index_id AND ic.is_included_column=1 ORDER BY c.name FOR XML PATH(''),TYPE).value('.','nvarchar(max)'),1,1,N''),N'')
       +N'|'+COALESCE(i.filter_definition,N'')
FROM sys.indexes i JOIN sys.tables t ON i.object_id=t.object_id
WHERE t.schema_id=SCHEMA_ID(N'wms') AND t.name<>N'flyway_schema_history' AND i.index_id>0 AND i.is_primary_key=0 AND i.is_unique_constraint=0;
