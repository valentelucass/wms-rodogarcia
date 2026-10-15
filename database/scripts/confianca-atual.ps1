# Somente metadados locais. Nunca chama profile/factory que possa recapturar TLS.
function Get-WmsRuntimeTrust {
    $modulePath=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../../.runtime/sql-server/SqlServerProjetos.psm1'))
    $module=Import-Module $modulePath -PassThru -Force
    $tlsRoot=Join-Path ([Environment]::GetFolderPath('LocalApplicationData')) 'Rodogarcia/SqlServer'
    $tlsPath=Join-Path $tlsRoot 'tls.json'
    & $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $tlsPath
    $tls=Get-Content -LiteralPath $tlsPath -Raw -Encoding UTF8|ConvertFrom-Json
    $identity=& $module {Get-LocalIdentity}
    if($tls.pid -ne $identity.pid -or $tls.started -cne $identity.started){throw 'D21_TLS_PROCESSO_ATUAL_DIVERGENTE'}
    $pin=([string]$tls.pid)+'|'+$tls.started+'|'+$tls.fileSHA256+'|'+$tls.truststoreSHA256
    $previous=Get-Variable -Name WmsRuntimeTrustPin -Scope Script -ErrorAction SilentlyContinue
    if($previous -and $previous.Value -cne $pin){throw 'D21_TLS_MUDOU_DURANTE_PREPARO'}
    if($tls.id -cnotmatch '^[a-f0-9]{32}$' -or $tls.certificate -cne (Join-Path $tlsRoot ('sql-public-'+$tls.id+'.pem')) -or
        $tls.truststore -cne ($tls.certificate -replace '\.pem$','.p12') -or
        [string]::IsNullOrWhiteSpace($tls.certificateHost) -or $tls.certificateHost -match '[\x00-\x20;]') {throw 'D21_TLS_CAMINHO_DIVERGENTE'}
    foreach($p in @($tls.certificate,$tls.truststore)){& $module {param($p) Assert-NoLink $p;Assert-PrivateAcl $p} $p}
    if((Get-FileHash -LiteralPath $tls.certificate).Hash -cne $tls.fileSHA256 -or
        (Get-FileHash -LiteralPath $tls.truststore).Hash -cne $tls.truststoreSHA256){throw 'D21_TLS_HASH_DIVERGENTE'}
    $certificate=New-Object Security.Cryptography.X509Certificates.X509Certificate2($tls.certificate)
    $hasher=[Security.Cryptography.SHA256]::Create()
    try {
        $der=([BitConverter]::ToString($hasher.ComputeHash($certificate.RawData))).Replace('-','').ToLowerInvariant()
        if($der -cne $tls.certificateSHA256 -or [DateTime]::UtcNow -lt $certificate.NotBefore.ToUniversalTime() -or
            [DateTime]::UtcNow -ge $certificate.NotAfter.ToUniversalTime()){throw 'D21_TLS_CERTIFICADO_INVALIDO'}
    } finally {$hasher.Dispose();$certificate.Dispose()}
    & $module {Import-SqlClient}
    $script:WmsRuntimeTrustPin=$pin
    $tls
}
