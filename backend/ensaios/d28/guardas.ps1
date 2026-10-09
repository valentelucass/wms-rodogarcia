function Assert-D28Preflight {
 param([Parameter(Mandatory=$true)]$Preflight)
 if($Preflight.database -cne 'WMS_DEV' -or $Preflight.identity -cne 'WMSDEV' -or
    $Preflight.alvo.banco -cne 'WMS_DEV' -or $Preflight.alvo.login -cne 'WMSDEV' -or
    $Preflight.alvo.usuario -cne 'WMSDEV' -or $Preflight.alvo.servidor -cne 'ROD-SRVW-001' -or
    $Preflight.guardasValidas -ne $true -or $Preflight.tls.mandatory -ne $true -or
    $Preflight.tls.trustServerCertificate -ne $false -or $Preflight.prodConectado -ne $false) {
  throw 'D28_PREFLIGHT_PRUMO_RECUSADO'
 }
}
