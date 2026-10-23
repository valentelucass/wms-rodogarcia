# BE10-NOTAZZ-IMP01: contrato exato esperado em memória; não executa SQL.
function Get-WmsNotazzTables {
    @(
        @{name='solicitacao_nfe';columns=@('id','plano_id','operacao_id','pedido_id','grupo','natureza','versao_pedido','hash_confirmacao','corpo_json','documento_json','emitente_cnpj','ambiente','situacao','notazz_id','documento_id','documento_origem_id','numero','serie','chave_acesso','protocolo','xml','mensagem','usuario','criada_em','atualizada_em','ativa','regularizacao_operacao_id','regularizada_em','pedido_entrada_id');update=@('situacao','notazz_id','documento_id','numero','serie','chave_acesso','protocolo','xml','mensagem','atualizada_em','ativa','regularizacao_operacao_id','regularizada_em','pedido_entrada_id')},
        @{name='parcela_nfe';columns=@('solicitacao_id','reserva_id','entrada_id','item_nota_id','quantidade','valor_total');update=@()},
        @{name='evento_nfe';columns=@('id','solicitacao_id','tipo','usuario','motivo','registrado_em');update=@()},
        @{name='notazz_canal';columns=@('emitente_cnpj','ambiente','ocupado_ate','dono');update=@('ocupado_ate','dono')}
    )
}
function Get-WmsNotazzAddedColumns {
    @(
        @{name='cliente';columns=@('codigo_municipio','indicador_ie','agrupamento_nfe')},
        @{name='armazem';columns=@('codigo_municipio','indicador_ie','agrupamento_nfe')},
        @{name='referencia_fiscal_produto';columns=@('tributacao_nfe')}
    )
}
function Add-WmsNotazzExpectedContract($Historical,$Migration) {
    if(!$Migration -or $Migration.version -cne '15' -or $Migration.script -cne 'V15__emissao_fiscal_notazz.sql' -or $Migration.sha256 -cne 'CB4B75D57B07A9CC306CD26FD5B006D48A5C1F846F49E1952B90F4DEA7A62D6F' -or $Migration.checksum -ne 650621757){throw 'NOTAZZ_MIGRATION_CONTRACT_INVALID'}
    $history=@($Historical.historico|Where-Object type -ceq 'SQL')
    if((@($history|Sort-Object {[int]$_.version}|ForEach-Object version) -join ',') -cne '1,2,3,4,5,6,7,8,9,10,11,12,13,14' -or @($history|Where-Object success -ne $true).Count){throw 'NOTAZZ_BASELINE_CONTRACT_INVALID'}
    $expected=$Historical|ConvertTo-Json -Depth 40|ConvertFrom-Json
    foreach($table in (Get-WmsNotazzTables)){
        foreach($permission in @('SELECT','INSERT','DELETE','ALTER','CONTROL','TAKE OWNERSHIP','VIEW CHANGE TRACKING')){
            $expected.rights.objects += [pscustomobject]@{tabela=$table.name;permissao=$permission;permitido=$(if($permission -in @('SELECT','INSERT')){1}else{0})}
        }
        foreach($permission in @('SELECT','INSERT')){$expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=0;coluna='';permission_name=$permission;state_desc='GRANT'}}
        for($i=0;$i -lt $table.columns.Count;$i++){
            $column=$table.columns[$i];$allowed=if($column -in $table.update){1}else{0}
            $expected.rights.columns += [pscustomobject]@{tabela=$table.name;coluna=$column;permitido=$allowed}
            if($allowed){$expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=($i+1);coluna=$column;permission_name='UPDATE';state_desc='GRANT'}}
        }
        $expected.catalogo[0].tabelas=[int]$expected.catalogo[0].tabelas+1
        $expected.catalogo[0].colunas=[int]$expected.catalogo[0].colunas+$table.columns.Count
    }
    foreach($table in (Get-WmsNotazzAddedColumns)){
        $count=@($expected.rights.columns|Where-Object tabela -ceq $table.name).Count
        if(!$count){throw 'NOTAZZ_EXISTING_TABLE_MISSING'}
        for($i=0;$i -lt $table.columns.Count;$i++){
            $column=$table.columns[$i]
            $expected.rights.columns += [pscustomobject]@{tabela=$table.name;coluna=$column;permitido=1}
            $expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=($count+$i+1);coluna=$column;permission_name='UPDATE';state_desc='GRANT'}
        }
        $expected.catalogo[0].colunas=[int]$expected.catalogo[0].colunas+$table.columns.Count
    }
    $expected.historico += [pscustomobject]@{installed_rank=15;version='15';type='SQL';script=$Migration.script;checksum=$Migration.checksum;success=$true}
    $expected
}
