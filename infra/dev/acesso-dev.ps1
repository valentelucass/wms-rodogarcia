# Configuracao publica de acesso. Nao le senhas nem inicia conexoes.
function Get-WmsDevAccess([int]$FrontendPort, [string]$ConfigPath = (Join-Path $PSScriptRoot 'acesso-dev.json')) {
    $origin='http://127.0.0.1:'+$FrontendPort
    $proxyOrigin=''
    $secure=$false
    $mode='local'
    if(Test-Path -LiteralPath $ConfigPath -PathType Leaf) {
        $config=Get-Content -LiteralPath $ConfigPath -Raw -Encoding UTF8 | ConvertFrom-Json
        if($config.PSObject.Properties.Name -notcontains 'modo'){throw 'AUTH_DEV_ACCESS_MODE_INVALIDO'}
        $mode=[string]$config.modo
    }
    if($mode -cnotin @('local','tunnel')){throw 'AUTH_DEV_ACCESS_MODE_INVALIDO'}
    if($mode -ceq 'tunnel') {
        # O endereco publico pertence ao tunel atual; nao e persistido nem descoberto por segredo.
        $origin=''
        $proxyOrigin='http://localhost:'+$FrontendPort
        $secure=$true
    }
    @{ origin=$origin; proxyOrigin=$proxyOrigin; secureCookie=$secure; mode=$mode }
}
