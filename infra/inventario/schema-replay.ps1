# Replay local, sem SQL. O executor continua Flyway com a fonte V14 original.
# Somente a migration exata revisada pode expandir as duas expressoes anteriores.
function Convert-WmsInventorySchemaReplay($Fontes) {
    $inventory=@($Fontes|Where-Object {[IO.Path]::GetFileName($_.arquivo) -ceq 'V14__inventarios_por_escopo.sql'})
    if($inventory.Count -ne 1){throw 'D21_INVENTORY_SOURCE_INVALID'}
    $f=$inventory[0];$hasher=[Security.Cryptography.SHA256]::Create()
    try{$sha=([BitConverter]::ToString($hasher.ComputeHash([Text.Encoding]::UTF8.GetBytes($f.texto)))).Replace('-','')}finally{$hasher.Dispose()}
    if($sha -cne '308294D469D075C7360544135CE21F390E22F776A6A6B5A85DC9F8233D74B352'){throw 'D21_INVENTORY_SOURCE_INVALID'}
    $prior=@($Fontes|Where-Object {$_ -ne $f})
    $schema=D20SchemaEfetivo $prior
    $operation=$schema.tabelas['operacao_administrativa'].constraints['ck_operacao_administrativa_tipo'].definicao -replace '^CHECK\s*',''
    $audit=$schema.tabelas['auditoria_cadastro'].constraints['ck_auditoria_tipo'].definicao -replace '^CHECK\s*',''
    if(!$operation -or !$audit){throw 'D21_INVENTORY_BASELINE_INVALID'}
    $static=($f.texto -split '-- Preservar integralmente',2)[0].Replace('BEGIN TRANSACTION;','')
    $static+="`nALTER TABLE wms.operacao_administrativa DROP CONSTRAINT ck_operacao_administrativa_tipo;"
    $static+="`nALTER TABLE wms.operacao_administrativa WITH CHECK ADD CONSTRAINT ck_operacao_administrativa_tipo CHECK (($operation) OR tipo IN ('INICIO_INVENTARIO','LEITURA_INVENTARIO','FIM_INVENTARIO','CANCELAMENTO_INVENTARIO'));"
    $static+="`nALTER TABLE wms.auditoria_cadastro DROP CONSTRAINT ck_auditoria_tipo;"
    $static+="`nALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_tipo CHECK (($audit) OR tipo='INVENTARIO');"
    $static+="`nALTER TABLE wms.auditoria_cadastro WITH CHECK ADD CONSTRAINT ck_auditoria_inventario CHECK (tipo<>'INVENTARIO' OR acao IN ('CRIACAO','ALTERACAO'));"
    @($prior)+@([pscustomobject]@{arquivo=$f.arquivo;texto=$static})
}
