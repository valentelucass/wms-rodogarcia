-- SQL Server: perfil de tipos da posição; preserva dados, limites e tipos físicos.
-- Aplicar somente pelo executor WMS autorizado, após conferir alvo e histórico.
IF DB_NAME() <> N'${wmsDatabase}'
    THROW 51000, 'Banco diferente do alvo WMS definido para a migracao.', 1;

ALTER TABLE wms.endereco DROP CONSTRAINT ck_endereco_perfil;
ALTER TABLE wms.endereco WITH CHECK ADD CONSTRAINT ck_endereco_perfil CHECK (
    tipo_unidade_permitido IS NULL OR (
        tipo_unidade_permitido IN (
            'PALLET', 'BOBINA', 'VOLUME',
            'PALLET_BOBINA', 'PALLET_VOLUME', 'BOBINA_VOLUME', 'TODAS'
        )
        AND capacidade_peso_kg IS NOT NULL AND altura_metros IS NOT NULL
        AND largura_metros IS NOT NULL AND profundidade_metros IS NOT NULL
        AND empilhamento_maximo IS NOT NULL
    )
);
