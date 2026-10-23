# BE14-INVENTARIO01: contrato esperado em memoria. Sem conexao ou DDL.
function Get-WmsInventoryTables {
    @(
        @{name='inventario';columns=@('id','versao','cliente_id','armazem_id','tipo','produto_id','endereco_id','situacao','iniciado_em','encerrado_em','usuario','motivo','divergencias','relatorio_json');update=@('versao','situacao','encerrado_em','divergencias','relatorio_json')},
        @{name='inventario_item';columns=@('inventario_id','codigo_unidade','base_json','prevista','contado','endereco_lido','posicao_lida','lido_em','usuario');update=@('contado','endereco_lido','posicao_lida','lido_em','usuario')}
    )
}
function Add-WmsInventoryExpectedContract($Historical,$Migration) {
    if (!$Migration -or $Migration.version -cne '14' -or $Migration.script -cne 'V14__inventarios_por_escopo.sql' -or
        $Migration.sha256 -cne '308294D469D075C7360544135CE21F390E22F776A6A6B5A85DC9F8233D74B352' -or $Migration.checksum -ne 119378608) {
        throw 'INVENTORY_MIGRATION_CONTRACT_INVALID'
    }
    $history=@($Historical.historico|Where-Object {$_.type -ceq 'SQL'})
    if ((@($history|Sort-Object {[int]$_.version}|ForEach-Object {$_.version}) -join ',') -cne '1,2,3,4,5,6,7,8,9,10,11,12,13' -or
        @($history|Where-Object {$_.success -ne $true}).Count -ne 0) {throw 'INVENTORY_BASELINE_CONTRACT_INVALID'}
    $expected=$Historical|ConvertTo-Json -Depth 40|ConvertFrom-Json
    foreach($table in (Get-WmsInventoryTables)) {
        foreach($permission in @('SELECT','INSERT','DELETE','ALTER','CONTROL','TAKE OWNERSHIP','VIEW CHANGE TRACKING')) {
            $expected.rights.objects += [pscustomobject]@{tabela=$table.name;permissao=$permission;permitido=$(if($permission -in @('SELECT','INSERT')){1}else{0})}
        }
        foreach($permission in @('SELECT','INSERT')) {
            $expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=0;coluna='';permission_name=$permission;state_desc='GRANT'}
        }
        for($index=0;$index -lt $table.columns.Count;$index++) {
            $column=$table.columns[$index];$allowed=if($column -in $table.update){1}else{0}
            $expected.rights.columns += [pscustomobject]@{tabela=$table.name;coluna=$column;permitido=$allowed}
            if($allowed){$expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=($index+1);coluna=$column;permission_name='UPDATE';state_desc='GRANT'}}
        }
        $expected.catalogo[0].tabelas=[int]$expected.catalogo[0].tabelas+1
        $expected.catalogo[0].colunas=[int]$expected.catalogo[0].colunas+$table.columns.Count
    }
    $expected.historico += [pscustomobject]@{installed_rank=14;version='14';type='SQL';script=$Migration.script;checksum=$Migration.checksum;success=$true}
    $expected
}
