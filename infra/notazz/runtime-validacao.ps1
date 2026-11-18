# Somente atesta a instancia DEV indicada. Nao prepara, seleciona ou inicia artefatos.
function Assert-NotazzCurrentRuntime {
 param([Parameter(Mandatory=$true)][string]$RuntimeReceipt)
 $root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
 . (Join-Path $root 'infra/dev/artefato-backend.ps1')
 . (Join-Path $root 'infra/sql01/launcher-backend.ps1')
 . (Join-Path $root 'infra/dev/reiniciar-instancia.ps1')
 . (Join-Path $root 'infra/dev/acesso-dev.ps1')
 $runtimePath=Resolve-WmsSql01Path $RuntimeReceipt (Join-Path $root 'orchestracao/.runtime/be02-dev-fonte01/prumo/runs')
 if([IO.Path]::GetFileName($runtimePath) -cne 'runtime-prumo.json'){throw 'NFH_RUNTIME_PATH_INVALIDO'}
 $run=Split-Path $runtimePath -Parent;$runId=[IO.Path]::GetFileName($run);$uuid=[guid]::Empty
 if(![guid]::TryParseExact($runId,'D',[ref]$uuid)){throw 'NFH_RUN_INVALIDO'}
 $r=Get-Content -LiteralPath $runtimePath -Raw -Encoding UTF8|ConvertFrom-Json
 if($r.runId -cne $runId -or $r.id -cne 'BE02-DEV-FONTE01' -or $r.owner -cne 'WMS - Prumo' -or $r.estado -cne 'READY' -or $r.database -cne 'WMS_DEV' -or $r.login -cne 'WMSDEV' -or $r.profile -cne 'sqlserver-dev' -or !$r.bootstrapDisabled -or !$r.jdbcProviderProved){throw 'NFH_RUNTIME_INVALIDO'}
 $candidatePath=Read-WmsSql01HashedFile $r.candidate $run
 $a=Assert-WmsDevCurrentArtifact $root $candidatePath $r.candidate.sha256
 if($a.runId -cne $runId -or $a.jar.path -ine (Join-Path $run 'target/wms-backend-0.0.1-SNAPSHOT.jar') -or $a.config.path -ine (Join-Path $run 'application-frontend-dev.properties') -or $a.jar.sha256 -ine $r.artifact.sha256 -or $a.sourceHash -cne $r.sourceHash -or $a.fingerprint.sha256 -cne $r.fingerprint.sha256){throw 'NFH_PACOTE_DIVERGENTE'}
 $startupPath=Read-WmsSql01HashedFile $r.guard $run
 $g=Get-Content -LiteralPath $startupPath -Raw -Encoding UTF8|ConvertFrom-Json
 if(!$g.guardaRealAprovada -or $g.estado -cne 'GUARDA_ATUAL_APROVADA' -or @($g.checks|Where-Object {!$_.passou}).Count -or $g.auxiliar.sha256 -ine (Get-FileHash -LiteralPath (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')).Hash){throw 'NFH_GUARDA_DE_PARTIDA_INVALIDA'}
 $access=Get-WmsDevAccess 25581
 $origin=if($access.mode -ceq 'tunnel'){$access.proxyOrigin}else{$access.origin}
 if($r.localOrigin -cne $origin -or $r.authOrigin -cne $access.origin -or $r.proxyOrigin -cne $access.proxyOrigin -or $r.secureCookie -ne $access.secureCookie){throw 'NFH_ORIGEM_DIVERGENTE'}
 $plan=@{root=$root;run=$run;runId=$runId;jar=$a.jar.path;config=$a.config.path;sourceHash=$a.sourceHash;jarSha256=$a.jar.sha256;guard=$g;truststore=$g.perfilProtegido.truststore;access=$access;localOrigin=$origin}
 $jdbc=Read-WmsDevCurrentJdbcProof $plan $r.backend.pid
 if($jdbc.sha256 -ine $r.jdbc.sha256 -or $jdbc.path -ine $r.jdbc.path){throw 'NFH_JDBC_DIVERGENTE'}
 $p=Get-WmsSql01BackendProcess $r.backend.pid
 $arguments=@(Split-WmsDevCommandLine $p.CommandLine);$expected=@($a.java.path)+@(Get-WmsDevCurrentBackendArguments $plan)
 if($p.StartTime.ToUniversalTime().ToString('o') -cne $r.backend.startUtc -or $p.ExecutablePath -ine $a.java.path -or $arguments.Count -ne $expected.Count){throw 'NFH_BACKEND_DIVERGENTE'}
 for($i=0;$i -lt $arguments.Count;$i++){if($arguments[$i] -cne $expected[$i]){throw 'NFH_COMANDO_DIVERGENTE'}}
 $listeners=@(Get-WmsDevListeners|Where-Object OwningProcess -eq $p.Id)
 if(!$listeners.Count -or @($listeners|Where-Object {$_.LocalPort -ne 25580 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'NFH_LISTENER_DIVERGENTE'}
 $l=Get-Content -LiteralPath (Join-Path $run 'launcher.json') -Raw -Encoding UTF8|ConvertFrom-Json
 if($l.estado -cne 'PROCESSES_READY_ROUNDTRIP_PENDING' -or $l.backend.backend.pid -ne $p.Id -or !$l.frontend.listenerOwned){throw 'NFH_LAUNCHER_DIVERGENTE'}
 $fe=Get-CimInstance Win32_Process -Filter ('ProcessId='+$l.frontend.pid)
 $feArgs=@(Split-WmsDevCommandLine $fe.CommandLine)
 if($fe.Name -ine 'node.exe' -or [math]::Abs(([DateTimeOffset]::Parse($l.frontend.startUtc).UtcDateTime-$fe.CreationDate.ToUniversalTime()).TotalMilliseconds) -gt 1 -or $feArgs.Count -ne 4 -or $feArgs[1] -ine (Join-Path $root 'frontend/node_modules/vite/bin/vite.js') -or $feArgs[2] -cne '--config' -or $feArgs[3] -cne 'vite.dev.config.ts'){throw 'NFH_FRONTEND_DIVERGENTE'}
 $feListeners=@(Get-WmsDevListeners|Where-Object OwningProcess -eq $fe.ProcessId)
 if(!$feListeners.Count -or @($feListeners|Where-Object {$_.LocalPort -ne 25581 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'NFH_FRONTEND_LISTENER_DIVERGENTE'}
 $sql=Get-WmsSql01SqlProcess $r.sql.pid
 if($sql.StartTime.ToUniversalTime().ToString('o') -cne $r.sql.startUtc){throw 'NFH_SQL_DIVERGENTE'}
 [pscustomobject]@{runtime=$r;candidate=$a;run=$run;startupGuard=$g;fullCurrentSourceEquivalent=$true;allRecordedSourceHashesEqual=$true;runtimePath=$runtimePath;runtimeSha256=(Get-FileHash -LiteralPath $runtimePath).Hash}
}
