[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$OutputPath)
$ErrorActionPreference='Stop'
Set-StrictMode -Version Latest
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'contrato-banco.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'infra/prod/guarda-wmsprod.ps1')
. (Join-Path $root 'infra/auth/contrato-banco.ps1')
$cases=[Collections.Generic.List[object]]::new()
function Check([string]$Name,[bool]$Passed) {
    $cases.Add([pscustomobject]@{name=$Name;passed=$Passed})
    if(!$Passed){throw ('UNIT_TYPES_PURE_FAILED_'+$Name)}
}
$devMigrations=@(Get-WmsDev02Migrations)
$prodMigrations=@(Get-WmsProdMigrations)
$v12=@($devMigrations | Where-Object version -ceq '12')[0]
foreach($source in @(@{file='infra/dev02/guarda-wmsdev.ps1';name='Compare-Dev02Rows'},@{file='infra/prod/guarda-wmsprod.ps1';name='Compare-ProdRows'})) {
    $tokens=$null;$errors=$null
    $ast=[Management.Automation.Language.Parser]::ParseFile((Join-Path $root $source.file),[ref]$tokens,[ref]$errors)
    if(@($errors).Count){throw 'GUARD_PARSER_INVALID'}
    $compare=$ast.Find({param($node) $node -is [Management.Automation.Language.FunctionDefinitionAst] -and $node.Name -ceq $source.name},$true)
    if(!$compare){throw 'GUARD_COMPARE_MISSING'}
    . ([scriptblock]::Create($compare.Extent.Text))
}
foreach($source in @('DEV','PROD')) {
    if($source -ceq 'DEV') {
        $baseline=Get-Content -LiteralPath (Join-Path $root 'orchestracao/.runtime/d29-farol-retomada-preflight.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        $baseline=Add-WmsAuthExpectedContract $baseline ($devMigrations | Where-Object version -ceq '11')
        $migrations=@($devMigrations | Where-Object {[int]$_.version -le 12})
    } else {
        $baseline=Get-Content -LiteralPath (Join-Path $root 'infra/prod/contrato-aplicacao.json') -Raw -Encoding UTF8 | ConvertFrom-Json
        $migrations=@($prodMigrations | Where-Object {[int]$_.version -le 12})
    }
    $original=$baseline | ConvertTo-Json -Depth 40 -Compress
    $expected=Add-WmsUnitTypesExpectedContract $baseline $v12
    Check ($source+'_baseline_preserved') ($original -ceq ($baseline | ConvertTo-Json -Depth 40 -Compress))
    Check ($source+'_rights_catalog_preserved') (($baseline.rights | ConvertTo-Json -Depth 40 -Compress) -ceq ($expected.rights | ConvertTo-Json -Depth 40 -Compress) -and ($baseline.catalogo | ConvertTo-Json -Compress) -ceq ($expected.catalogo | ConvertTo-Json -Compress))
    Check ($source+'_only_V12_added') (($baseline.historico | ConvertTo-Json -Depth 8 -Compress) -ceq (@($expected.historico | Where-Object version -cne '12') | ConvertTo-Json -Depth 8 -Compress) -and @($expected.historico | Where-Object version -ceq '12').Count -eq 1)
    $fields=@('version','script','checksum')
    $local=@($migrations | Select-Object $fields | ForEach-Object {$_ | ConvertTo-Json -Compress} | Sort-Object)
    $contract=@($expected.historico | Where-Object type -ceq 'SQL' | Select-Object $fields | ForEach-Object {$_ | ConvertTo-Json -Compress} | Sort-Object)
    Check ($source+'_local_inventory_matches') (($local -join "`n") -ceq ($contract -join "`n"))
    $compareName=if($source -ceq 'DEV'){'Compare-Dev02Rows'}else{'Compare-ProdRows'}
    $historyFields=@('installed_rank','version','type','script','checksum','success')
    Check ($source+'_database_before_V12_rejected') (-not (& $compareName $baseline.historico $expected.historico $historyFields))
    foreach($field in @('version','script','checksum','sha256')) {
        $changed=$v12 | ConvertTo-Json | ConvertFrom-Json
        $changed.$field=if($field -ceq 'checksum'){0}else{'ALTERADO'}
        $refused=$false
        try {$null=Add-WmsUnitTypesExpectedContract $baseline $changed} catch {$refused=$_.Exception.Message -ceq 'UNIT_TYPES_MIGRATION_CONTRACT_INVALID'}
        Check ($source+'_changed_'+$field+'_refused') $refused
    }
}
$report=[pscustomobject]@{scope='PURE_CONTRACT_NO_SQL_NO_DPAPI_NO_APP';selected=$cases.Count;passed=@($cases | Where-Object passed).Count;failed=@($cases | Where-Object {!$_.passed}).Count;sqlConnections=0;applicationProcesses=0;cases=$cases.ToArray()}
if(Test-Path -LiteralPath $OutputPath){throw 'PURE_OUTPUT_EXISTS'}
[IO.File]::WriteAllText([IO.Path]::GetFullPath($OutputPath),($report | ConvertTo-Json -Depth 7),[Text.UTF8Encoding]::new($false))
$report | Select-Object scope,selected,passed,failed,sqlConnections,applicationProcesses | ConvertTo-Json
