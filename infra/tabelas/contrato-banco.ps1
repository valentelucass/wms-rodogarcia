# TAB-EDIT01: somente contrato esperado em memoria; nao conecta nem aplica DDL/grants.
function Add-WmsBillingTableExpectedContract($Historical, $Migration) {
    if (!$Migration -or $Migration.version -cne '13' -or
        $Migration.script -cne 'V13__historico_itens_tabela_cobranca.sql' -or
        $Migration.checksum -ne 1550763371 -or
        $Migration.sha256 -cne '3794C8638C4A3F2B823B76288967EE0E03C81E804879F15F4A4986BECFB7DD28') {
        throw 'BILLING_TABLE_MIGRATION_CONTRACT_INVALID'
    }
    $history=@($Historical.historico | Where-Object {$_.type -ceq 'SQL'})
    if ((@($history | Sort-Object {[int]$_.version} | ForEach-Object {$_.version}) -join ',') -cne '1,2,3,4,5,6,7,8,9,10,11,12' -or
        @($history | Where-Object {$_.success -ne $true}).Count -ne 0 -or
        @($Historical.rights.columns | Where-Object {$_.tabela -ceq 'item_tabela_cobranca' -and $_.coluna -ceq 'ativo'}).Count -ne 0) {
        throw 'BILLING_TABLE_BASELINE_CONTRACT_INVALID'
    }
    $expected=$Historical | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $expected.historico += [pscustomobject]@{
        installed_rank=13;version='13';type='SQL';script=$Migration.script;
        checksum=$Migration.checksum;success=$true
    }
    $expected.rights.columns += [pscustomobject]@{tabela='item_tabela_cobranca';coluna='ativo';permitido=0}
    $expected.catalogo[0].colunas=[int]$expected.catalogo[0].colunas+1
    return $expected
}
