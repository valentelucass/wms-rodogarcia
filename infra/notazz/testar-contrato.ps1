$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
# Teste estrutural em memória sobre recibo sanitizado; não conecta nem executa SQL.
$pre=Get-Content -LiteralPath (Join-Path $root 'orchestracao/.runtime/notazz-implementation-20261023/guarda-antes.json') -Raw|ConvertFrom-Json
$before=[pscustomobject]@{historico=$pre.historico;catalogo=@($pre.catalogo);rights=$pre.direitos}
$original=$before|ConvertTo-Json -Depth 40 -Compress
$migration=Get-WmsDev02Migrations|Where-Object version -ceq '15'
$expected=Add-WmsNotazzExpectedContract $before $migration
function Check([bool]$Ok,[string]$Name){if(-not $Ok){throw $Name};Write-Output ('PASS '+$Name)}
Check (($before|ConvertTo-Json -Depth 40 -Compress) -ceq $original) 'baseline_preservada'
Check ($expected.catalogo[0].tabelas-$before.catalogo[0].tabelas -eq 4 -and $expected.catalogo[0].colunas-$before.catalogo[0].colunas -eq 52) 'quatro_tabelas_e_52_colunas'
Check (@($expected.historico|Where-Object version -ceq '15').Count -eq 1) 'historico_V15_unico'
Check (@($expected.rights.explicit).Count-@($before.rights.explicit).Count -eq 31) 'somente_oito_direitos_de_tabela_e_23_updates_de_coluna'
$names=@(Get-WmsNotazzTables|ForEach-Object name)
Check (@($expected.rights.objects|Where-Object {$_.tabela -in $names -and $_.permissao -in @('DELETE','ALTER','CONTROL','TAKE OWNERSHIP') -and $_.permitido -ne 0}).Count -eq 0) 'sem_exclusao_ou_administracao'
Check (@($expected.rights.columns|Where-Object {$_.tabela -in @('parcela_nfe','evento_nfe') -and $_.permitido -ne 0}).Count -eq 0) 'parcelas_e_eventos_imutaveis'
Check (@($expected.rights.columns|Where-Object {$_.tabela -ceq 'solicitacao_nfe' -and $_.coluna -in @('corpo_json','documento_json','hash_confirmacao','emitente_cnpj','ambiente','operacao_id','pedido_id','grupo') -and $_.permitido -ne 0}).Count -eq 0) 'identidade_e_snapshot_imutaveis'
$changed=$migration|ConvertTo-Json|ConvertFrom-Json;$changed.checksum++
$refused=$false;try{$null=Add-WmsNotazzExpectedContract $before $changed}catch{$refused=$_.Exception.Message -ceq 'NOTAZZ_MIGRATION_CONTRACT_INVALID'}
Check $refused 'checksum_alterado_recusado'
$refused=$false;try{$null=Add-WmsNotazzExpectedContract $expected $migration}catch{$refused=$_.Exception.Message -ceq 'NOTAZZ_BASELINE_CONTRACT_INVALID'}
Check $refused 'reaplicacao_e_baseline_futura_recusadas'
. (Join-Path $root 'database/scripts/d21-catalogo.ps1')
$sources=@(Get-ChildItem -LiteralPath (Join-Path $root 'database/migrations') -Filter 'V*.sql' -File|ForEach-Object {[pscustomobject]@{arquivo=('migrations/'+$_.Name);texto=[IO.File]::ReadAllText($_.FullName)}})
$catalog=@(Get-WmsD21CatalogoEsperado $sources)
Check (@($catalog|Where-Object categoria -ceq 'T').Count -eq 74 -and @($catalog|Where-Object categoria -ceq 'COL').Count -eq 792) 'catalogo_completo_V15_consumivel_pelo_preparador'
Check (@($catalog|Where-Object {$_.categoria -ceq 'IDX' -and $_.nome -ceq 'uk_solicitacao_nfe_ativa' -and $_.valor -match 'ativa'}).Count -eq 1) 'unicidade_filtrada_de_solicitacao_ativa'
Check (@($catalog|Where-Object {$_.categoria -ceq 'CHECK' -and $_.nome -ceq 'ck_operacao_administrativa_tipo' -and $_.valor.Contains("'LEITURA_INVENTARIO'")}).Count -eq 1) 'operacoes_preexistentes_de_inventario_preservadas'
. (Join-Path $PSScriptRoot 'schema-replay.ps1')
$changedSources=$sources|ConvertTo-Json -Depth 8|ConvertFrom-Json
($changedSources|Where-Object arquivo -like '*V15__*').texto+='-- alteracao'
$refused=$false;try{$null=Convert-WmsNotazzSchemaReplay $changedSources}catch{$refused=$_.Exception.Message -ceq 'D21_NOTAZZ_SOURCE_INVALID'}
Check $refused 'replay_estrutural_recusa_fonte_alterada'
