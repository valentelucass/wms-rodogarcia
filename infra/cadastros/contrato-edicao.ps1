# CAD-EDIT01: contrato esperado da fonte, somente em memoria. Nao concede permissoes.
# Preserva a baseline historica e estende somente UPDATE nas colunas corrigiveis.
function Get-WmsCadastroEditableColumns {
    @{
        tabela_cobranca=@('armazem_id','cliente_id','descricao','tipo','vigencia_inicio')
        item_tabela_cobranca=@('ativo')
        cliente=@('documento_fiscal')
        armazem=@('documento_fiscal')
        produto=@('sku','unidade_medida','tipo_quantidade','precisao_quantidade','controla_lote','controla_validade','antecedencia_aviso_dias')
        embalagem=@('codigo_dun','quantidade_produto')
        endereco=@('codigo','rua','nivel','posicao','tipo','sequencia_coleta')
    }
}

function Add-WmsCadastroEditExpectedContract($Historical, [switch]$SomenteBase) {
    $expected=$Historical | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $delta=Get-WmsCadastroEditableColumns
    if($SomenteBase){$delta.Remove('tabela_cobranca');$delta.Remove('item_tabela_cobranca')}
    foreach($table in $delta.Keys) {
        # A baseline foi capturada por column_id, conservando a ordem do catalogo V1-V11.
        $columns=@($expected.rights.columns | Where-Object { $_.tabela -ceq $table })
        foreach($name in $delta[$table]) {
            $matches=@($columns | Where-Object { $_.coluna -ceq $name })
            if($matches.Count -ne 1) { throw 'CAD_EDIT_CONTRACT_COLUMN_MISSING' }
            $column=$matches[0]
            $existing=@($expected.rights.explicit | Where-Object {
                $_.esquema -ceq 'wms' -and $_.objeto -ceq $table -and $_.coluna -ceq $name -and $_.permission_name -ceq 'UPDATE'
            })
            if($existing.Count -gt 0 -and ($existing.Count -ne 1 -or $existing[0].state_desc -cne 'GRANT')) {
                throw 'CAD_EDIT_CONTRACT_UNEXPECTED_PERMISSION'
            }
            $column.permitido=1
            if($existing.Count -eq 0) {
                $index=[Array]::IndexOf($columns,$column)
                $expected.rights.explicit += [pscustomobject]@{
                    class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table;minor_id=($index+1)
                    coluna=$name;permission_name='UPDATE';state_desc='GRANT'
                }
            }
        }
    }
    $expected
}
