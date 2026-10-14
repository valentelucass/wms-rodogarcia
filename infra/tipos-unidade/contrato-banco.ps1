# END-TIPOS01: contrato esperado em memoria. Nao conecta nem aplica migration.
function Add-WmsUnitTypesExpectedContract($Historical, $Migration) {
    if (!$Migration -or $Migration.version -cne '12' -or
        $Migration.script -cne 'V12__tipos_unidade_permitidos_endereco.sql' -or
        $Migration.checksum -ne 1229599714 -or
        $Migration.sha256 -cne '7EE5D71244898DDDF27BA81334F4DB0C9E4D86521BF7BF78370B62D16D6FFE93') {
        throw 'UNIT_TYPES_MIGRATION_CONTRACT_INVALID'
    }
    $history=@($Historical.historico | Where-Object {$_.type -ceq 'SQL'})
    $versions=@($history | Sort-Object {[int]$_.version} | ForEach-Object {$_.version}) -join ','
    if ($versions -cne '1,2,3,4,5,6,7,8,9,10,11' -or
        @($history | Where-Object {$_.success -ne $true}).Count -ne 0) {
        throw 'UNIT_TYPES_BASELINE_CONTRACT_INVALID'
    }
    $expected=$Historical | ConvertTo-Json -Depth 40 | ConvertFrom-Json
    $expected.historico += [pscustomobject]@{
        installed_rank=12;version='12';type='SQL';script=$Migration.script;
        checksum=$Migration.checksum;success=$true
    }
    return $expected
}
