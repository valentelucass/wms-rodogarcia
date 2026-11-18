# COLETOR-TAREFAS01: contrato exato, somente metadados em memória.
function Get-WmsCollectorTables {
    @(
        @{name='coletor_tarefa';columns=@('id','versao','tipo','origem_id','cliente_id','armazem_id','criada_em','responsavel','iniciada_em');update=@('versao','responsavel','iniciada_em')},
        @{name='coletor_operacao';columns=@('id','tarefa_id','operacao_id','tipo','usuario','registrada_em','conteudo_hash','resultado');update=@()}
    )
}
function Add-WmsCollectorExpectedContract($Historical,$Migration) {
    if(!$Migration -or $Migration.version -cne '16' -or $Migration.script -cne 'V16__tarefas_coletor.sql' -or $Migration.sha256 -cne '2A304B6E90736021FB417E0D4CDE8E78D816A56CAC8D071EC6C3E951854E53DF' -or $Migration.checksum -ne -1071438601){throw 'COLETOR_MIGRATION_CONTRACT_INVALID'}
    $history=@($Historical.historico|Where-Object type -ceq 'SQL')
    if((@($history|Sort-Object {[int]$_.version}|ForEach-Object version) -join ',') -cne '1,2,3,4,5,6,7,8,9,10,11,12,13,14,15' -or @($history|Where-Object success -ne $true).Count){throw 'COLETOR_BASELINE_CONTRACT_INVALID'}
    $expected=$Historical|ConvertTo-Json -Depth 40|ConvertFrom-Json
    foreach($table in (Get-WmsCollectorTables)) {
        foreach($permission in @('SELECT','INSERT','DELETE','ALTER','CONTROL','TAKE OWNERSHIP','VIEW CHANGE TRACKING')) {
            $expected.rights.objects += [pscustomobject]@{tabela=$table.name;permissao=$permission;permitido=$(if($permission -in @('SELECT','INSERT')){1}else{0})}
        }
        foreach($permission in @('SELECT','INSERT')){$expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=0;coluna='';permission_name=$permission;state_desc='GRANT'}}
        for($i=0;$i -lt $table.columns.Count;$i++) {
            $column=$table.columns[$i];$allowed=if($column -in $table.update){1}else{0}
            $expected.rights.columns += [pscustomobject]@{tabela=$table.name;coluna=$column;permitido=$allowed}
            if($allowed){$expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=($i+1);coluna=$column;permission_name='UPDATE';state_desc='GRANT'}}
        }
        $expected.catalogo[0].tabelas=[int]$expected.catalogo[0].tabelas+1
        $expected.catalogo[0].colunas=[int]$expected.catalogo[0].colunas+$table.columns.Count
    }
    $expected.historico += [pscustomobject]@{installed_rank=16;version='16';type='SQL';script=$Migration.script;checksum=$Migration.checksum;success=$true}
    $expected
}
