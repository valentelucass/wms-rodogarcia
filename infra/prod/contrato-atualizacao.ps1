# PROD-ATUALIZA01: contrato revisado da fonte. Nao abre SQL nem altera direitos.
function Get-WmsProductionReleaseContract($Historical, $Migrations) {
    if ($Historical.schema -ne 1 -or $Historical.database -cne 'WMS_PROD' -or $Historical.login -cne 'WMSPROD') {
        throw 'PROD_RELEASE_BASELINE_INVALID'
    }
    $versions=@($Migrations | Sort-Object {[int]$_.version} | ForEach-Object {[int]$_.version})
    if ($versions.Count -notin @(11,12,13,14,15,16) -or ($versions -join ',') -cne ((1..$versions.Count) -join ',')) {
        throw 'PROD_RELEASE_MIGRATIONS_INVALID'
    }
    $repository=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
    $expected=$Historical | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    foreach ($extension in @(
        @{version=12;file='tipos-unidade';function='Add-WmsUnitTypesExpectedContract'},
        @{version=13;file='tabelas';function='Add-WmsBillingTableExpectedContract'},
        @{version=14;file='inventario';function='Add-WmsInventoryExpectedContract'},
        @{version=15;file='notazz';function='Add-WmsNotazzExpectedContract'},
        @{version=16;file='coletor';function='Add-WmsCollectorExpectedContract'}
    )) {
        if ($versions.Count -ge $extension.version) {
            . (Join-Path $repository ('infra/'+$extension.file+'/contrato-banco.ps1'))
            $expected=& $extension.function $expected ($Migrations | Where-Object {[int]$_.version -eq $extension.version})
        }
    }
    . (Join-Path $repository 'infra/cadastros/contrato-edicao.ps1')
    if ($versions.Count -ge 13) {Add-WmsCadastroEditExpectedContract $expected}
    else {Add-WmsCadastroEditExpectedContract $expected -SomenteBase}
}

function Get-WmsProductionRightKey($Right) {
    [string]::Join([char]31,@($Right.class_desc,$Right.esquema,$Right.objeto,[string]$Right.minor_id,$Right.coluna,$Right.permission_name,$Right.state_desc))
}

function Assert-WmsProductionAdditiveRights($Current, $Expected, $Minimum) {
    $allowed=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    $observed=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    foreach ($right in $Expected) {
        if (-not $allowed.Add((Get-WmsProductionRightKey $right))) {throw 'PROD_RELEASE_DUPLICATE_EXPECTED_RIGHT'}
    }
    foreach ($right in $Current) {
        $key=Get-WmsProductionRightKey $right
        if (-not $allowed.Contains($key) -or -not $observed.Add($key)) {throw 'PROD_RELEASE_UNEXPECTED_RIGHT'}
    }
    foreach ($right in $Minimum) {
        if (-not $observed.Contains((Get-WmsProductionRightKey $right))) {throw 'PROD_RELEASE_EXISTING_RIGHT_MISSING'}
    }
}

function Get-WmsProductionMissingGrants($Current, $Expected, $Minimum) {
    Assert-WmsProductionAdditiveRights $Current $Expected $Minimum
    $observed=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    foreach ($right in $Current) {$null=$observed.Add((Get-WmsProductionRightKey $right))}
    foreach ($right in $Expected) {
        if ($observed.Contains((Get-WmsProductionRightKey $right))) {continue}
        if ($right.class_desc -cne 'OBJECT_OR_COLUMN' -or $right.esquema -cne 'wms' -or
            $right.state_desc -cne 'GRANT' -or $right.objeto -cnotmatch '^[a-z_]+$') {throw 'PROD_RELEASE_GRANT_INVALID'}
        if ($right.minor_id -eq 0 -and $right.coluna -ceq '' -and $right.permission_name -cin @('SELECT','INSERT')) {
            'GRANT '+$right.permission_name+' ON OBJECT::[wms].['+$right.objeto+'] TO [WMSPROD];'
        } elseif ($right.minor_id -gt 0 -and $right.coluna -cmatch '^[a-z_]+$' -and $right.permission_name -ceq 'UPDATE') {
            'GRANT UPDATE (['+$right.coluna+']) ON OBJECT::[wms].['+$right.objeto+'] TO [WMSPROD];'
        } else {throw 'PROD_RELEASE_GRANT_INVALID'}
    }
}

function Get-WmsProductionPreGrantContract($Expected, $Minimum, $Rights) {
    Assert-WmsProductionAdditiveRights $Rights.explicit $Expected.rights.explicit $Minimum.rights.explicit
    $keys=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    $visible=New-Object 'Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    foreach ($right in $Rights.explicit) {
        $null=$keys.Add((Get-WmsProductionRightKey $right))
        if ($right.class_desc -ceq 'OBJECT_OR_COLUMN' -and $right.esquema -ceq 'wms' -and $right.state_desc -ceq 'GRANT') {
            $null=$visible.Add($right.objeto)
        }
    }
    $pre=$Expected | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $pre.rights.explicit=@($Expected.rights.explicit | Where-Object {$keys.Contains((Get-WmsProductionRightKey $_))})
    # A conta ve metadados apenas de objetos com direitos. O catalogo completo e
    # conferido pelo administrador na transacao; esta guarda nunca libera backend.
    $pre.rights.objects=@($pre.rights.objects | Where-Object {$visible.Contains($_.tabela)})
    foreach ($row in $pre.rights.objects) {
        $grant=[pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$row.tabela;minor_id=0;coluna='';permission_name=$row.permissao;state_desc='GRANT'}
        $row.permitido=[int]$keys.Contains((Get-WmsProductionRightKey $grant))
    }
    $pre.rights.columns=@($pre.rights.columns | Where-Object {$visible.Contains($_.tabela)})
    foreach ($table in @($pre.rights.columns | Group-Object tabela)) {
        $index=0
        foreach ($row in $table.Group) {
            $index++
            $grant=[pscustomobject]@{class_desc='OBJECT_OR_COLUMN';esquema='wms';objeto=$row.tabela;minor_id=$index;coluna=$row.coluna;permission_name='UPDATE';state_desc='GRANT'}
            $row.permitido=[int]$keys.Contains((Get-WmsProductionRightKey $grant))
        }
    }
    $pre.catalogo[0].tabelas=@($pre.rights.objects | Where-Object {$_.tabela -cne 'flyway_schema_history'} | Select-Object -ExpandProperty tabela -Unique).Count
    $pre.catalogo[0].colunas=@($pre.rights.columns | Where-Object {$_.tabela -cne 'flyway_schema_history'}).Count
    $pre
}
