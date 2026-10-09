[CmdletBinding()]
param(
 [Parameter(Mandatory=$true)][switch]$ExecutarSqlD27,
 [Parameter(Mandatory=$true)][ValidatePattern('^D27[A-F0-9]{8}$')][string]$BaseRodada,
 [ValidateRange(1,60)][int]$EsperaSegundos=60
)
$ErrorActionPreference='Stop'
if(-not $ExecutarSqlD27){throw 'D27_OPTIN_EXPLICITO_AUSENTE'}
$mutex=[Threading.Mutex]::new($false,'Local\WMS_Cedro_D27_Aguardador')
$owned=$false
try {
 try {$owned=$mutex.WaitOne(0)} catch [Threading.AbandonedMutexException] {$owned=$true}
 if(-not $owned){Write-Output 'D27_AGUARDADOR_JA_ATIVO_NENHUMA_JVM_SQL';exit 2}
 $backend=(Resolve-Path "$PSScriptRoot/../..").Path
 $root=(Resolve-Path "$backend/..").Path
 $fonte=Join-Path $backend ('evidencias/d27-'+$BaseRodada+'-http.json')
 $fixture=[IO.File]::ReadAllText($fonte)|ConvertFrom-Json
 if($fixture.rodada -cne $BaseRodada -or $fixture.login -cne 'WMSDEV' -or $fixture.alvo -cne '127.0.0.1:1433/WMS_DEV' -or @($fixture.ids.concorrenciaPedidos).Count -ne 2){throw 'D27_FIXTURE_FONTE_DIVERGENTE'}
 $jar='target-d27-numerico-final/wms-backend-0.0.1-SNAPSHOT.jar'
 $fim=[DateTime]::UtcNow.AddSeconds($EsperaSegundos)
 $sinal=[ordered]@{demanda='D27';estado='CEDRO_AGUARDADOR_ATIVO_ANTES_JVM_SQL';pid=$PID;inicioUtc=[DateTime]::UtcNow.ToString('o');expiraUtc=$fim.ToString('o');baseRodada=$BaseRodada;fonteCriacao=('backend/evidencias/d27-'+$BaseRodada+'-http.json');clienteId=$fixture.ids.clienteId;pedidos=$fixture.ids.concorrenciaPedidos;jar=$jar;sha256=(Get-FileHash (Join-Path $backend $jar)).Hash;credencialCarregada=$false;SQLExecutado=$false;JVMIniciada=$false;exclusaoMutua=$true}
 $sinalPath=Join-Path $backend 'evidencias/d27-cedro-aguardador-ativo.json'
 [IO.File]::WriteAllText($sinalPath,($sinal|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
 $enviado=$false
 do {
  $r=$null;$p=$null;$fixtures=$null
  try {
   $r=Get-Content (Join-Path $root 'orchestracao/.runtime/d27-prumo-observador-aguardando-ticket.json') -Encoding UTF8 -Raw|ConvertFrom-Json
   $fixtures=Get-Content (Join-Path $root 'orchestracao/.runtime/d27-prumo-concorrencia-hostpid-fixtures.json') -Encoding UTF8 -Raw|ConvertFrom-Json
   $p=Get-Process -Id $r.pid -ErrorAction SilentlyContinue
   $prazo=([DateTimeOffset]::Parse($r.expiraUtc).UtcDateTime-[DateTime]::UtcNow).TotalSeconds
   $hash=(Get-FileHash (Join-Path $root 'orchestracao/.runtime/d27-prumo-concorrencia.ps1') -Algorithm SHA256).Hash
   if($r.estado -ceq 'OBSERVADOR_AGUARDANDO_TICKET_NOVO' -and $r.prontoParaTicket -eq $true -and $prazo -ge 90 -and $p -and $p.ProcessName -cin @('powershell','pwsh') -and $fixtures.checks -ge 24 -and $fixtures.falhas -eq 0 -and $fixtures.SQLExecutado -eq $false -and $fixtures.hashObservador -ceq $hash){$enviado=$true;break}
  } catch {}
  Start-Sleep -Milliseconds 250
 } while([DateTime]::UtcNow -lt $fim)
 $sinal.estado=if($enviado){'WAITTICKET_VALIDO_RECEBIDO_HELPER_VAI_INICIAR'}else{'CEDRO_ESPERA_EXPIRADA_SEM_JVM_SQL'}
 $sinal.fimUtc=[DateTime]::UtcNow.ToString('o')
 [IO.File]::WriteAllText($sinalPath,($sinal|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
 if(-not $enviado){Write-Output 'D27_WAITTICKET_ATUAL_NAO_RECEBIDO_NENHUMA_JVM_SQL';exit 1}
 Write-Output ('D27_WAITTICKET_ATUAL_PID_'+$r.pid+'_RETOMADA_UNICA_SEM_NOVA_FIXTURE')
 Push-Location $backend
 try { & "$PSScriptRoot/executar.ps1" -Etapa concorrencia -BaseRodada $BaseRodada -ExecutarSqlD27 -Jar $jar } finally {Pop-Location}
} finally {
 if($owned){$mutex.ReleaseMutex()}
 $mutex.Dispose()
}
