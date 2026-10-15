$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'contrato-edicao.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'infra/auth/contrato-banco.ps1')
. (Join-Path $root 'infra/tipos-unidade/contrato-banco.ps1')
. (Join-Path $root 'infra/tabelas/contrato-banco.ps1')
$migrations=@(Get-WmsDev02Migrations)
foreach($source in @('orchestracao/.runtime/d29-farol-retomada-preflight.json','infra/prod/contrato-aplicacao.json')) {
    $baseline=Get-Content -LiteralPath (Join-Path $root $source) -Raw -Encoding UTF8 | ConvertFrom-Json
    if (@($baseline.historico | Where-Object version -ceq '11').Count -eq 0) {
        $baseline=Add-WmsAuthExpectedContract $baseline ($migrations | Where-Object version -ceq '11')
    }
    $baseline=Add-WmsUnitTypesExpectedContract $baseline ($migrations | Where-Object version -ceq '12')
    $baseline=Add-WmsBillingTableExpectedContract $baseline ($migrations | Where-Object version -ceq '13')
    $original=$baseline | ConvertTo-Json -Depth 40 -Compress
    $expected=Add-WmsCadastroEditExpectedContract $baseline
    $legacy=Add-WmsCadastroEditExpectedContract $baseline -SomenteBase
    if(@($legacy.rights.explicit).Count-@($baseline.rights.explicit).Count -ne 17){throw 'LEGACY_DELTA_NOT_17'}
    if(@($expected.rights.explicit).Count-@($legacy.rights.explicit).Count -ne 6){throw 'BILLING_DELTA_NOT_6'}
    if($original -cne ($baseline | ConvertTo-Json -Depth 40 -Compress)){throw 'BASELINE_MUTATED'}
    if(@($expected.rights.explicit).Count-@($baseline.rights.explicit).Count -ne 23){throw 'DELTA_NOT_23'}
    foreach($kind in @('server','database','roles','serverRoles','ownership','schema','objects')) {
        if(($baseline.rights.$kind | ConvertTo-Json -Depth 20 -Compress) -cne ($expected.rights.$kind | ConvertTo-Json -Depth 20 -Compress)){throw "UNRELATED_RIGHTS_CHANGED_$kind"}
    }
    if(($baseline.historico | ConvertTo-Json -Depth 20 -Compress) -cne ($expected.historico | ConvertTo-Json -Depth 20 -Compress)){throw 'HISTORY_MUTATED'}
    $again=Add-WmsCadastroEditExpectedContract $expected
    if(@($again.rights.explicit).Count -ne @($expected.rights.explicit).Count){throw 'NOT_IDEMPOTENT'}
    $delta=Get-WmsCadastroEditableColumns
    foreach($table in $delta.Keys) {
        foreach($name in $delta[$table]) {
            $right=@($expected.rights.explicit | Where-Object {$_.objeto -ceq $table -and $_.coluna -ceq $name -and $_.permission_name -ceq 'UPDATE'})
            if($right.Count -ne 1 -or $right[0].state_desc -cne 'GRANT' -or $right[0].minor_id -lt 1){throw 'COLUMN_GRANT_INVALID'}
        }
    }
    $baseline.rights.columns=@($baseline.rights.columns | Where-Object { $_.tabela -cne 'cliente' -or $_.coluna -cne 'documento_fiscal' })
    $refused=$false
    try {$null=Add-WmsCadastroEditExpectedContract $baseline} catch { $refused=$_.Exception.Message -ceq 'CAD_EDIT_CONTRACT_COLUMN_MISSING' }
    if(!$refused){throw 'MISSING_COLUMN_ACCEPTED'}
}
'PASS: contratos DEV/PROD em memoria; delta exato, baseline/historico/direitos alheios preservados, idempotencia e recusa de coluna ausente. Sem conexao ou grant.'
