$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$rounds=New-Object 'Collections.Generic.List[object]'
foreach($f in Get-ChildItem -LiteralPath (Join-Path $backend 'evidencias') -Filter 'd28-D28*-http.json') {
 $h=Get-Content -LiteralPath $f.FullName -Encoding UTF8 -Raw|ConvertFrom-Json
 if(-not $h.ids.jarPid){continue}
 $pidApi=[long]$h.ids.jarPid
 $process=Get-CimInstance Win32_Process -Filter "ProcessId=$pidApi" -ErrorAction Stop
 $own=$false
 if($process){$own=($process.Name -ceq 'java.exe' -and $process.CommandLine.Replace('\','/').Contains('/backend/target-d28-atual/wms-backend-0.0.1-SNAPSHOT.jar'))}
 $ports=New-Object 'Collections.Generic.List[object]'
 foreach($kind in @('porta','issuerPorta')) {
  if(-not $h.ids.$kind){continue};$port=[int]$h.ids.$kind;$listeners=@();$confirmed=$false
  try {$listeners=@(Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction Stop);$confirmed=$true}
  catch {if($_.FullyQualifiedErrorId -clike 'CmdletizationQuery_NotFound*'){$confirmed=$true}else{throw}}
  $ports.Add([ordered]@{tipo=$kind;porta=$port;consultaConfirmada=$confirmed;listeners=$listeners.Count;donos=@($listeners.OwningProcess)})
 }
 $helperPid=$h.ids.helperPid
 if($own -and $process.ParentProcessId){$helperPid=[long]$process.ParentProcessId}
 $helper=$null
 if($helperPid){$helper=Get-CimInstance Win32_Process -Filter "ProcessId=$helperPid" -ErrorAction Stop}
 $version=$h.ids.helperVersao
 if(-not $version){$version=switch($h.rodada){'D287D721FEA' {'H1 prefixo antigo 15min'} 'D289E87D622' {'H1 prefixo Prumo 15min'} 'D2830B61B28' {'H3 GET complementar 15min'} 'D2895805120' {'H3 GET complementar 15min'} 'D284DC609CC' {'H2 limites antes fixture saldo 15min'} 'D288163F01B' {'H4 limites com saldo 15min'} 'D28925E295C' {'H4 variantes 15min'} 'D28079AE038' {'H5 coletor GET negativo; red preservado 120min'} 'D28EFFF454F' {'H5 par HTTP/JVM concluido; coletor red 120min'} 'D28BBB4F694' {'H5 GET estoque 120min'} default {'H6 GET200 120min'}}}
 $rounds.Add([ordered]@{rodada=$h.rodada;fase=$h.fase;helperVersao=$version;jarSHA=$h.ids.jarSha256;pidApi=$pidApi;processoExiste=($null -ne $process);processoProprioConfirmado=$own;helperPid=$helperPid;helperExiste=($null -ne $helper);flagsHistoricas=[ordered]@{jarEncerrado=$h.ids.jarEncerrado;portasLivres=$h.ids.portasLivres};portas=$ports.ToArray();nenhumProcessoEncerradoPorEsteScript=$true})
}
$result=[ordered]@{utc=[DateTime]::UtcNow.ToString('o');incremento='D28';metodo='Win32_Process PID e Get-NetTCPConnection por portas propias';sqlExecutado=$false;processos=$rounds.ToArray();jarsAtivos=@($rounds|Where-Object processoProprioConfirmado).Count;leiturasAguardando=@($rounds|Where-Object {$_.fase -ceq 'aguardando-select-prumo'}|ForEach-Object rodada);listenersProprios=($rounds|ForEach-Object {$_.portas}|ForEach-Object listeners|Measure-Object -Sum).Sum}
[IO.File]::WriteAllText((Join-Path $backend 'evidencias/d28-processos-atual.json'),($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
[pscustomobject]$result|Select-Object utc,jarsAtivos,listenersProprios,leiturasAguardando|ConvertTo-Json
