$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
$pre=Get-Content (Join-Path $root 'orchestracao/.runtime/inventory-activation-20261023/guard-current.json') -Raw|ConvertFrom-Json
$before=[pscustomobject]@{historico=$pre.historico;catalogo=@($pre.catalogo);rights=$pre.direitos}
$original=$before|ConvertTo-Json -Depth 40 -Compress
$migration=Get-WmsDev02Migrations|Where-Object version -ceq '14'
$expected=Add-WmsInventoryExpectedContract $before $migration
function Check([bool]$Ok,[string]$Name){if(-not $Ok){throw $Name};Write-Output ('PASS '+$Name)}
Check (($before|ConvertTo-Json -Depth 40 -Compress) -ceq $original) 'baseline_preservada'
Check ($expected.catalogo[0].tabelas -eq 70 -and $expected.catalogo[0].colunas -eq 740) 'duas_tabelas_23_colunas'
Check (@($expected.historico|Where-Object version -ceq '14').Count -eq 1) 'historico_V14_unico'
Check (@($expected.rights.explicit).Count-@($before.rights.explicit).Count -eq 14) 'somente_select_insert_e_dez_updates'
Check (@($expected.rights.objects|Where-Object {$_.tabela -in @('inventario','inventario_item') -and $_.permissao -in @('DELETE','ALTER','CONTROL','TAKE OWNERSHIP') -and $_.permitido -ne 0}).Count -eq 0) 'sem_admin_delete_ou_ddl'
$changed=$migration|ConvertTo-Json|ConvertFrom-Json;$changed.checksum++
$refused=$false;try{$null=Add-WmsInventoryExpectedContract $before $changed}catch{$refused=$_.Exception.Message -ceq 'INVENTORY_MIGRATION_CONTRACT_INVALID'}
Check $refused 'checksum_alterado_recusado'
$refused=$false;try{$null=Add-WmsInventoryExpectedContract $expected $migration}catch{$refused=$_.Exception.Message -ceq 'INVENTORY_BASELINE_CONTRACT_INVALID'}
Check $refused 'baseline_futura_recusada'
