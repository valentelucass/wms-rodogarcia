$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'acesso-dev.ps1')
$fixture=Join-Path ([IO.Path]::GetTempPath()) ('wms-access-'+[guid]::NewGuid().ToString('N')+'.json')
$checks=0
try {
    $local=Get-WmsDevAccess 25581 $fixture
    if($local.origin -cne 'http://127.0.0.1:25581' -or $local.secureCookie -or $local.proxyOrigin){throw 'LOCAL_INVALIDO'}
    $checks++
    foreach($port in @(25581,25583)) {
        [IO.File]::WriteAllText($fixture,'{"modo":"tunnel"}')
        $access=Get-WmsDevAccess $port $fixture
        if(-not $access.secureCookie -or $access.proxyOrigin -cne ('http://localhost:'+$port) -or $access.origin -or $access.mode -cne 'tunnel'){throw 'TUNNEL_INVALIDO'}
        $checks++
    }
    foreach($mode in @('', '*', 'https://fixture.devtunnels.ms', 'automatico', 'TUNNEL')) {
        [IO.File]::WriteAllText($fixture,(@{modo=$mode}|ConvertTo-Json))
        $rejected=$false
        try {$null=Get-WmsDevAccess 25581 $fixture} catch {if($_.Exception.Message -ne 'AUTH_DEV_ACCESS_MODE_INVALIDO'){throw};$rejected=$true}
        if(-not $rejected){throw 'URL_INSEGURA_ACEITA'}
        $checks++
    }
    [IO.File]::WriteAllText($fixture,'{"modo":"local"}')
    $local=Get-WmsDevAccess 25581 $fixture
    if($local.secureCookie -or $local.origin -cne 'http://127.0.0.1:25581'){throw 'VOLTA_LOCAL_INVALIDA'}
    $checks++
    Write-Output ('PASS: '+$checks+' verificacoes de acesso DEV, sem banco ou credenciais.')
} finally {
    if(Test-Path -LiteralPath $fixture){Remove-Item -LiteralPath $fixture}
}
