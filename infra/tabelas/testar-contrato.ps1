[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$OutputPath)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'infra/prod/guarda-wmsprod.ps1')
. (Join-Path $root 'infra/auth/contrato-banco.ps1')
. (Join-Path $root 'infra/tipos-unidade/contrato-banco.ps1')
. (Join-Path $root 'infra/cadastros/contrato-edicao.ps1')
$cases=[Collections.Generic.List[object]]::new()
function Check([string]$Name,[bool]$Passed) {
    $cases.Add([pscustomobject]@{name=$Name;passed=$Passed})
    if(!$Passed){throw ('BILLING_TABLE_PURE_FAILED_'+$Name)}
}
$migrations=@(Get-WmsDev02Migrations)
$v13=@($migrations | Where-Object version -ceq '13')[0]
foreach($source in @('DEV','PROD')) {
    $inventory=if($source -ceq 'DEV'){$migrations}else{@(Get-WmsProdMigrations)}
    $file=if($source -ceq 'DEV'){'orchestracao/.runtime/d29-farol-retomada-preflight.json'}else{'infra/prod/contrato-aplicacao.json'}
    $baseline=Get-Content -LiteralPath (Join-Path $root $file) -Raw -Encoding UTF8 | ConvertFrom-Json
    if($source -ceq 'DEV'){$baseline=Add-WmsAuthExpectedContract $baseline ($migrations | Where-Object version -ceq '11')}
    $baseline=Add-WmsUnitTypesExpectedContract $baseline ($migrations | Where-Object version -ceq '12')
    $original=$baseline | ConvertTo-Json -Depth 40 -Compress
    $expected=Add-WmsBillingTableExpectedContract $baseline $v13
    Check ($source+'_baseline_preserved') ($original -ceq ($baseline | ConvertTo-Json -Depth 40 -Compress))
    Check ($source+'_rights_preserved') (($baseline.rights.explicit | ConvertTo-Json -Depth 15 -Compress) -ceq ($expected.rights.explicit | ConvertTo-Json -Depth 15 -Compress))
    Check ($source+'_one_new_column') (@($expected.rights.columns).Count -eq @($baseline.rights.columns).Count+1 -and @($expected.rights.columns | Where-Object {$_.tabela -ceq 'item_tabela_cobranca' -and $_.coluna -ceq 'ativo' -and $_.permitido -eq 0}).Count -eq 1)
    Check ($source+'_catalog_exact_delta') ($expected.catalogo[0].colunas -eq $baseline.catalogo[0].colunas+1 -and $expected.catalogo[0].tabelas -eq $baseline.catalogo[0].tabelas)
    Check ($source+'_history_preserved') (($baseline.historico | ConvertTo-Json -Depth 8 -Compress) -ceq (@($expected.historico | Where-Object version -cne '13') | ConvertTo-Json -Depth 8 -Compress))
    Check ($source+'_all_migrations_match') ((@($expected.historico | Where-Object type -ceq 'SQL' | Sort-Object {[int]$_.version} | ForEach-Object {$_.version+':'+$_.script+':'+$_.checksum}) -join ',') -ceq (@($inventory | ForEach-Object {$_.version+':'+$_.script+':'+$_.checksum}) -join ','))
    foreach($field in @('version','script','checksum','sha256')) {
        $changed=$v13 | ConvertTo-Json | ConvertFrom-Json
        $changed.$field=if($field -ceq 'checksum'){0}else{'ALTERADO'}
        $refused=$false
        try{$null=Add-WmsBillingTableExpectedContract $baseline $changed}catch{$refused=$_.Exception.Message -ceq 'BILLING_TABLE_MIGRATION_CONTRACT_INVALID'}
        Check ($source+'_changed_'+$field+'_refused') $refused
    }
    $refused=$false
    try{$null=Add-WmsBillingTableExpectedContract $expected $v13}catch{$refused=$_.Exception.Message -ceq 'BILLING_TABLE_BASELINE_CONTRACT_INVALID'}
    Check ($source+'_repeated_schema_rejected') $refused
    $legacy=Add-WmsCadastroEditExpectedContract $expected -SomenteBase
    $current=Add-WmsCadastroEditExpectedContract $expected
    Check ($source+'_only_six_billing_grants') (@($current.rights.explicit).Count-@($legacy.rights.explicit).Count -eq 6)
}
foreach($file in @('infra/tabelas/contrato-banco.ps1','infra/tabelas/testar-contrato.ps1','infra/dev02/guarda-wmsdev.ps1','infra/prod/guarda-wmsprod.ps1','infra/cadastros/contrato-edicao.ps1','infra/cadastros/aplicar-permissoes-dev.ps1','infra/cadastros/aplicar-permissoes-prod.ps1')) {
    $tokens=$null;$errors=$null
    $null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $root $file),[ref]$tokens,[ref]$errors)
    Check ('parsing_'+$file) (@($errors).Count -eq 0)
}
$report=[pscustomobject]@{scope='PURE_CONTRACT_NO_SQL_NO_DPAPI_NO_APP';selected=$cases.Count;passed=@($cases|Where-Object passed).Count;failed=@($cases|Where-Object {!$_.passed}).Count;sqlConnections=0;cases=$cases.ToArray()}
if(Test-Path -LiteralPath $OutputPath){throw 'PURE_OUTPUT_EXISTS'}
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath),($report|ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
$report | Select-Object scope,selected,passed,failed,sqlConnections | ConvertTo-Json
