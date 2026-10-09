# Extensao D32 do oraculo D29; somente calculo em memoria, sem SQL ou arquivos alterados.
function Get-WmsAuthTables {
    @(
        @{name='usuario_acesso';columns=@('id','nome','email','senha_hash','perfil','administrador','principal','ativo','trocar_senha','versao_tokens','falhas','bloqueado_ate','clientes','armazens','versao');update=@('nome','senha_hash','perfil','administrador','ativo','trocar_senha','versao_tokens','falhas','bloqueado_ate','clientes','armazens','versao')},
        @{name='sessao_acesso';columns=@('id','usuario_id','versao_tokens','expira','revogada');update=@('revogada')},
        @{name='renovacao_acesso';columns=@('hash','sessao_id','expira','usado');update=@('usado')},
        @{name='evento_acesso';columns=@('id','ator','alvo','acao','instante');update=@()}
    )
}

function Add-WmsAuthExpectedContract($Historical, $Migration) {
    if ($Migration.version -cne '11' -or $Migration.script -cne 'V11__login_usuarios_e_sessoes.sql') { throw 'AUTH_MIGRATION_CONTRACT_INVALID' }
    $expected = $Historical | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    foreach ($table in (Get-WmsAuthTables)) {
        foreach ($permission in @('SELECT','INSERT','DELETE','ALTER','CONTROL','TAKE OWNERSHIP','VIEW CHANGE TRACKING')) {
            $allowed = if ($permission -in @('SELECT','INSERT')) { 1 } else { 0 }
            $expected.rights.objects += [pscustomobject]@{tabela=$table.name;permissao=$permission;permitido=$allowed}
        }
        foreach ($permission in @('SELECT','INSERT')) {
            $expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=0;coluna='';permission_name=$permission;state_desc='GRANT'}
        }
        for ($index=0; $index -lt $table.columns.Count; $index++) {
            $column = $table.columns[$index]
            $allowed = if ($column -in $table.update) { 1 } else { 0 }
            $expected.rights.columns += [pscustomobject]@{tabela=$table.name;coluna=$column;permitido=$allowed}
            if ($allowed) {
                $expected.rights.explicit += [pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$table.name;minor_id=($index+1);coluna=$column;permission_name='UPDATE';state_desc='GRANT'}
            }
        }
        $expected.catalogo.tabelas++
        $expected.catalogo.colunas += $table.columns.Count
    }
    $expected.historico += [pscustomobject]@{installed_rank=11;version='11';type='SQL';script=$Migration.script;checksum=$Migration.checksum;success=$true}
    $expected
}
