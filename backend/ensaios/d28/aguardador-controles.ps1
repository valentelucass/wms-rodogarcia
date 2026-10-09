# Controles do aguardador D27 exercitados offline em D28. Nenhum launcher SQL e chamado.
function Invoke-D28AguardadorOffline {
 param([bool]$OptIn,[string]$Rodada,$Fixture,[scriptblock]$LerTicket,[scriptblock]$Destino,
       [string]$MutexName,[int]$PrazoMs=500,[string]$HashObservador)
 if(-not $OptIn){throw 'D28_OPTIN_EXPLICITO_AUSENTE'}
 if($Rodada -cnotmatch '^D28[A-F0-9]{8}$' -or $Fixture.rodada -cne $Rodada -or $Fixture.login -cne 'WMSDEV' -or $Fixture.alvo -cne '127.0.0.1:1433/WMS_DEV' -or
    @($Fixture.pedidos).Count -ne 2 -or @($Fixture.pedidos|Select-Object -Unique).Count -ne 2){throw 'D28_FIXTURE_FONTE_DIVERGENTE'}
 # Antes de mutex/ticket/gate: uma reserva final jamais e reexecutada.
 if(@($Fixture.estados|Where-Object {$_ -cne 'RASCUNHO'}).Count -gt 0 -or @($Fixture.estados).Count -ne 2){throw 'D28_ESTADO_FINAL_RECUSADO_ANTES_GATE'}
 if($Fixture.fisico -ne 100 -or $Fixture.reservado -ne 0){throw 'D28_SALDO_PRE_GATE_RECUSADO'}
 $mutex=[Threading.Mutex]::new($false,$MutexName);$owned=$false
 try {
  try{$owned=$mutex.WaitOne(0)}catch [Threading.AbandonedMutexException]{$owned=$true}
  if(-not $owned){return [ordered]@{estado='D28_AGUARDADOR_JA_ATIVO';destinoChamado=$false;SQLExecutado=$false}}
  $fim=[DateTime]::UtcNow.AddMilliseconds($PrazoMs)
  do {
   $r=& $LerTicket
   $valid=$false
   if($r){
    try{$restante=([DateTimeOffset]::Parse($r.expiraUtc).UtcDateTime-[DateTime]::UtcNow).TotalSeconds}catch{$restante=-1}
    $p=Get-Process -Id $r.pid -ErrorAction SilentlyContinue
    $valid=$r.rodada -ceq $Rodada -and $r.estado -ceq 'OBSERVADOR_AGUARDANDO_TICKET_NOVO' -and $r.prontoParaTicket -eq $true -and $restante -ge 90 -and
      $p -and $p.ProcessName -cin @('powershell','pwsh') -and $r.checks -ge 24 -and $r.falhas -eq 0 -and $r.SQLExecutado -eq $false -and $r.hashObservador -ceq $HashObservador -and
      $r.banco -ceq 'WMS_DEV' -and $r.login -ceq 'WMSDEV'
   }
   if($valid){& $Destino;return [ordered]@{estado='D28_TICKET_VALIDO_DESTINO_OFFLINE_UNICO';destinoChamado=$true;SQLExecutado=$false}}
   Start-Sleep -Milliseconds 25
  }while([DateTime]::UtcNow -lt $fim)
  return [ordered]@{estado='D28_ESPERA_EXPIRADA_SEM_JVM_SQL';destinoChamado=$false;SQLExecutado=$false}
 }finally{if($owned){$mutex.ReleaseMutex()};$mutex.Dispose()}
}
