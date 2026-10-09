function Assert-D29Preflight {
 param([Parameter(Mandatory=$true)]$Preflight)
 if($Preflight.database -cne 'WMS_DEV' -or $Preflight.identity -cne 'WMSDEV' -or
    $Preflight.alvo.banco -cne 'WMS_DEV' -or $Preflight.alvo.login -cne 'WMSDEV' -or
    $Preflight.alvo.usuario -cne 'WMSDEV' -or $Preflight.alvo.servidor -cne 'ROD-SRVW-001' -or
    $Preflight.guardasValidas -ne $true -or $Preflight.tls.mandatory -ne $true -or
    $Preflight.tls.trustServerCertificate -ne $false -or $Preflight.prodConectado -ne $false) {
  throw 'D29_PREFLIGHT_PRUMO_RECUSADO'
 }
 $authorization=(Get-Item -LiteralPath "$PSScriptRoot/../../../orchestracao/.runtime/d29-autorizacao-e-distribuicao.md").LastWriteTimeUtc
 if([DateTimeOffset]::Parse($Preflight.observadoEm).UtcDateTime -lt $authorization){throw 'D29_PREFLIGHT_ANTERIOR_AUTORIZACAO_RECUSADO'}
}
