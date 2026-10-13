# Material inicial exclusivo PROD; nunca redefine usuario ja existente.
. (Join-Path $PSScriptRoot 'setup-prod.ps1')
function Get-WmsProductionBootstrapPath {
    Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/WMS/auth-prod/bootstrap.clixml'
}
function Get-WmsProductionBootstrapHash {
    $path=Get-WmsProductionBootstrapPath
    if(-not (Test-Path -LiteralPath $path)){return $null}
    foreach($p in @([IO.Path]::GetDirectoryName($path),$path)){Assert-WmsProductionPrivatePath $p}
    $secured=$null;$ptr=[IntPtr]::Zero
    try{
        $secured=Import-Clixml -LiteralPath $path
        if($secured -isnot [Security.SecureString]){throw 'PROD_BOOTSTRAP_MATERIAL_INVALID'}
        $ptr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secured)
        $data=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)|ConvertFrom-Json
        if($data.schema -ne 1 -or $data.environment -cne 'PROD' -or $data.database -cne 'WMS_PROD' -or $data.purpose -cne 'INITIAL_PRINCIPAL_ONLY' -or $data.hash -cnotmatch '^\{pbkdf2-600k\}[0-9a-f]{96}$'){throw 'PROD_BOOTSTRAP_PURPOSE_INVALID'}
        $result=ConvertTo-SecureString $data.hash -AsPlainText -Force;$result.MakeReadOnly();$result
    }finally{if($ptr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)};if($secured){$secured.Dispose()};$data=$null}
}
