$ErrorActionPreference='Stop'
$repository=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $PSScriptRoot 'guarda-wmsprod.ps1')
. (Join-Path $PSScriptRoot 'contrato-atualizacao.ps1')
$results=New-Object 'Collections.Generic.List[object]'
function Check([string]$Name,[scriptblock]$Test) {
    & $Test
    $results.Add([pscustomobject]@{caso=$Name;passou=$true})
}
function Need([bool]$Value) {if (-not $Value) {throw 'PROD_RELEASE_TEST_ASSERTION'}}
function Copy-ReleaseFixture($Value) {$Value | ConvertTo-Json -Depth 40 | ConvertFrom-Json}
function Refuse([scriptblock]$Test,[string]$Code) {
    $caught=$false
    try {& $Test | Out-Null} catch {$caught=$_.Exception.Message -ceq $Code}
    Need $caught
}
$minimum=Get-Content (Join-Path $PSScriptRoot 'contrato-aplicacao.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$minimumJson=$minimum | ConvertTo-Json -Depth 40 -Compress
$migrations=@(Get-WmsProdMigrations)
$expected=Get-WmsProductionReleaseContract $minimum $migrations
$old=Get-WmsProductionReleaseContract $minimum @($migrations | Where-Object {[int]$_.version -le 13})
$expectedJson=$expected | ConvertTo-Json -Depth 40 -Compress
Check 'V16_catalogo_completo_sem_mutar_baseline' {
    Need ($expected.catalogo[0].tabelas -eq 76 -and $expected.catalogo[0].colunas -eq 809)
    Need (($minimum | ConvertTo-Json -Depth 40 -Compress) -ceq $minimumJson)
}
Check 'V10_propria_PROD_preservada' {
    $actual=@($expected.historico | Where-Object version -ceq '10')[0]
    $original=@($minimum.historico | Where-Object version -ceq '10')[0]
    Need ($actual.script -ceq $original.script -and $actual.checksum -eq $original.checksum)
}
Check 'V1_a_V16_sem_reaplicar_historico' {
    Need ((@($expected.historico | Where-Object type -ceq 'SQL' | ForEach-Object version) -join ',') -ceq ((1..16) -join ','))
}
Check 'V14_hash_alterado_recusado' {
    $changed=Copy-ReleaseFixture $migrations
    ($changed | Where-Object version -ceq '14').sha256='0'*64
    Refuse {Get-WmsProductionReleaseContract $minimum $changed} 'INVENTORY_MIGRATION_CONTRACT_INVALID'
}
Check 'migration_desconhecida_recusada' {
    Refuse {Get-WmsProductionReleaseContract $minimum @($migrations + [pscustomobject]@{version='17'})} 'PROD_RELEASE_MIGRATIONS_INVALID'
}
Check 'baseline_DEV_recusada' {
    $changed=Copy-ReleaseFixture $minimum;$changed.database='WMS_DEV'
    Refuse {Get-WmsProductionReleaseContract $changed $migrations} 'PROD_RELEASE_BASELINE_INVALID'
}
Check 'migration_duplicada_recusada' {
    Refuse {Get-WmsProductionReleaseContract $minimum @($migrations + $migrations[-1])} 'PROD_RELEASE_MIGRATIONS_INVALID'
}
Check 'direitos_V13_aceitos_apenas_como_preparo' {
    Assert-WmsProductionAdditiveRights $old.rights.explicit $expected.rights.explicit $minimum.rights.explicit
    $script:pre=Get-WmsProductionPreGrantContract $expected $minimum $old.rights
    Need ($pre.catalogo[0].tabelas -eq 68 -and $pre.catalogo[0].colunas -eq 724)
    Need (@($pre.rights.objects | Where-Object tabela -ceq 'coletor_tarefa').Count -eq 0)
    Need (@($pre.historico | Where-Object version -ceq '16').Count -eq 1)
}
Check 'colunas_fiscais_novas_sem_UPDATE_antes_do_grant' {
    Need (@($pre.rights.columns | Where-Object {$_.tabela -ceq 'cliente' -and $_.coluna -ceq 'codigo_municipio' -and $_.permitido -eq 0}).Count -eq 1)
    Need (@($expected.rights.columns | Where-Object {$_.tabela -ceq 'cliente' -and $_.coluna -ceq 'codigo_municipio' -and $_.permitido -eq 1}).Count -eq 1)
}
Check 'grants_somente_novos_necessarios' {
    $script:grants=@(Get-WmsProductionMissingGrants $old.rights.explicit $expected.rights.explicit $minimum.rights.explicit)
    Need ($grants.Count -eq ($expected.rights.explicit.Count-$old.rights.explicit.Count))
    Need (@($grants | Where-Object {$_ -cnotmatch '^GRANT (SELECT|INSERT|UPDATE \(\[[a-z_]+\]\)) ON OBJECT::\[wms\]\.\[[a-z_]+\] TO \[WMSPROD\];$'}).Count -eq 0)
}
Check 'ledger_coletor_sem_UPDATE_DELETE' {
    Need (@($grants | Where-Object {$_ -match 'coletor_operacao' -and $_ -notmatch '^GRANT (SELECT|INSERT) '}).Count -eq 0)
}
Check 'idempotencia_sem_grants_quando_atual' {
    Need (@(Get-WmsProductionMissingGrants $expected.rights.explicit $expected.rights.explicit $minimum.rights.explicit).Count -eq 0)
    $all=Get-WmsProductionPreGrantContract $expected $minimum $expected.rights
    Need (($all | ConvertTo-Json -Depth 40 -Compress) -ceq $expectedJson)
}
foreach ($permission in @('ALTER','CONTROL','DELETE','UPDATE')) {
    Check ('direito_amplo_'+$permission+'_recusado') {
        $bad=[pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto='cliente';minor_id=0;coluna='';permission_name=$permission;state_desc='GRANT'}
        Refuse {Get-WmsProductionMissingGrants @($old.rights.explicit+$bad) $expected.rights.explicit $minimum.rights.explicit} 'PROD_RELEASE_UNEXPECTED_RIGHT'
    }
}
Check 'grant_option_recusado' {
    $changed=Copy-ReleaseFixture $old.rights.explicit;$changed[3].state_desc='GRANT_WITH_GRANT_OPTION'
    Refuse {Get-WmsProductionMissingGrants $changed $expected.rights.explicit $minimum.rights.explicit} 'PROD_RELEASE_UNEXPECTED_RIGHT'
}
Check 'remocao_direito_anterior_recusada' {
    Refuse {Get-WmsProductionMissingGrants @($old.rights.explicit | Select-Object -Skip 1) $expected.rights.explicit $minimum.rights.explicit} 'PROD_RELEASE_EXISTING_RIGHT_MISSING'
}
Check 'direito_duplicado_recusado' {
    Refuse {Get-WmsProductionMissingGrants @($old.rights.explicit+$old.rights.explicit[0]) $expected.rights.explicit $minimum.rights.explicit} 'PROD_RELEASE_UNEXPECTED_RIGHT'
}
Check 'contrato_final_nao_modificado_pelo_preparo' {
    Need (($expected | ConvertTo-Json -Depth 40 -Compress) -ceq $expectedJson)
}
$area=Join-Path $repository ('orchestracao/.runtime/prod-atualiza-20261103/contrato-'+[guid]::NewGuid().ToString('N'))
$null=New-Item -ItemType Directory -Path $area
[IO.File]::WriteAllText((Join-Path $area 'resultado.json'),($results.ToArray() | ConvertTo-Json -Depth 4),[Text.UTF8Encoding]::new($false))
Write-Output ('Contrato PROD: '+$results.Count+' verificacoes aprovadas, sem SQL. '+$area)
