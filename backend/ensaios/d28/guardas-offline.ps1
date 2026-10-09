$ErrorActionPreference='Stop'
. "$PSScriptRoot/guardas.ps1"
$good='{"database":"WMS_DEV","identity":"WMSDEV","alvo":{"banco":"WMS_DEV","login":"WMSDEV","usuario":"WMSDEV","servidor":"ROD-SRVW-001"},"guardasValidas":true,"tls":{"mandatory":true,"trustServerCertificate":false},"prodConectado":false}'
$cases=New-Object 'Collections.Generic.List[object]'
Assert-D28Preflight ($good|ConvertFrom-Json)
$cases.Add([ordered]@{caso='contrato publico atual valido';passou=$true})
foreach($variant in @('database','identity','banco','login','usuario','servidor','guardasValidas','mandatory','trustServerCertificate','prodConectado','envelopeAntigo')) {
 $r=$good|ConvertFrom-Json
 switch($variant) {
  'database' {$r.database='WMS_PROD'}
  'identity' {$r.identity='sa'}
  'banco' {$r.alvo.banco='WMS_PROD'}
  'login' {$r.alvo.login='sa'}
  'usuario' {$r.alvo.usuario='dbo'}
  'servidor' {$r.alvo.servidor='outro'}
  'guardasValidas' {$r.guardasValidas=$false}
  'mandatory' {$r.tls.mandatory=$false}
  'trustServerCertificate' {$r.tls.trustServerCertificate=$true}
  'prodConectado' {$r.prodConectado=$true}
  'envelopeAntigo' {$r='{"DB_NAME":"WMS_DEV","login":"WMSDEV","guardasValidas":true}'|ConvertFrom-Json}
 }
 $refused=$false
 try {Assert-D28Preflight $r} catch {$refused=$_.Exception.Message -ceq 'D28_PREFLIGHT_PRUMO_RECUSADO'}
 $cases.Add([ordered]@{caso=('recusa '+$variant);passou=$refused})
}
$result=[ordered]@{checks=$cases.Count;falhas=@($cases|Where-Object {-not $_.passou}).Count;SQLExecutado=$false;credencialCarregada=$false;casos=$cases.ToArray()}
[IO.File]::WriteAllText((Join-Path $PSScriptRoot '../../evidencias/d28-guardas-offline.json'),($result|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
[pscustomobject]$result|Select-Object checks,falhas,SQLExecutado,credencialCarregada|ConvertTo-Json
if($result.falhas){exit 1}
