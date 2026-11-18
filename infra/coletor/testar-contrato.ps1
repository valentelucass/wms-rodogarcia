$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
. (Join-Path $root 'database/scripts/d21-catalogo.ps1')
$results=New-Object 'Collections.Generic.List[object]'
function Check([bool]$pass,[string]$name){$results.Add([pscustomobject]@{caso=$name;passou=$pass});if(!$pass){throw ('COLETOR_TEST_'+$name)}}
$baseline=Get-Content (Join-Path $root 'orchestracao/.runtime/coletor-tarefas-20261028/guarda-inicial.json') -Raw|ConvertFrom-Json
$before=[pscustomobject]@{historico=$baseline.historico;catalogo=@($baseline.catalogo);rights=$baseline.direitos}
$migration=Get-WmsDev02Migrations|Where-Object version -ceq '16'
$expected=Add-WmsCollectorExpectedContract $before $migration
Check ($expected.historico.Count -eq ($before.historico.Count+1)) 'somente_V16_adicionada'
Check ($expected.catalogo[0].tabelas -eq ($before.catalogo[0].tabelas+2)) 'duas_tabelas'
Check ($expected.catalogo[0].colunas -eq ($before.catalogo[0].colunas+17)) 'dezessete_colunas'
Check (@($expected.rights.columns|Where-Object {$_.tabela -ceq 'coletor_operacao' -and $_.permitido -ne 0}).Count -eq 0) 'ledger_imutavel'
Check (@($expected.rights.columns|Where-Object {$_.tabela -ceq 'coletor_tarefa' -and $_.permitido -eq 1}).Count -eq 3) 'somente_dono_inicio_revisao_mutaveis'
$changed=$migration|ConvertTo-Json|ConvertFrom-Json;$changed.checksum++
$refused=$false;try{$null=Add-WmsCollectorExpectedContract $before $changed}catch{$refused=$_.Exception.Message -ceq 'COLETOR_MIGRATION_CONTRACT_INVALID'}
Check $refused 'checksum_alterado_recusado'
$refused=$false;try{$null=Add-WmsCollectorExpectedContract $expected $migration}catch{$refused=$_.Exception.Message -ceq 'COLETOR_BASELINE_CONTRACT_INVALID'}
Check $refused 'reaplicacao_recusada'
$sources=@(Get-ChildItem -LiteralPath (Join-Path $root 'database/migrations') -Filter 'V*.sql' -File|ForEach-Object {[pscustomobject]@{arquivo=('migrations/'+$_.Name);texto=[IO.File]::ReadAllText($_.FullName)}})
$catalog=@(Get-WmsD21CatalogoEsperado $sources)
Check (@($catalog|Where-Object categoria -ceq 'T').Count -eq 76 -and @($catalog|Where-Object categoria -ceq 'COL').Count -eq 809) 'catalogo_completo_V16'
Check (@($catalog|Where-Object {$_.categoria -ceq 'CHECK' -and $_.nome -ceq 'ck_operacao_administrativa_tipo' -and $_.valor.Contains("'LEITURA_INVENTARIO'")}).Count -eq 1) 'inventario_anterior_preservado'
Check (@($catalog|Where-Object {$_.categoria -ceq 'UNIQUE' -and $_.nome -ceq 'uk_coletor_origem' -and $_.valor -ceq 'tipo,origem_id'}).Count -eq 1) 'origem_unica'
$out=Join-Path $root ('orchestracao/.runtime/coletor-tarefas-20261028/contrato-'+[guid]::NewGuid().ToString('N')+'.json')
[IO.File]::WriteAllText($out,($results.ToArray()|ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
Write-Output ('Contrato: '+$results.Count+' verificacoes aprovadas. '+$out)
