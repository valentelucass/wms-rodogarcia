$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$psi=New-Object Diagnostics.ProcessStartInfo
$psi.FileName='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin/java.exe';$psi.WorkingDirectory=$backend;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true;$psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
$psi.Arguments='-Xmx192m -XX:+UseSerialGC -XX:ErrorFile=NUL -XX:-CreateCoredumpOnCrash -XX:-DumpReplayDataOnError -cp "target-d29-helper;target-d29-helper/lib/*" TlsCreateNewOfflineD29'
$p=New-Object Diagnostics.Process;$p.StartInfo=$psi
try {
 if(-not $p.Start()){throw 'D29_TLS_FIXTURE_NAO_INICIADA'}
 $first=$p.StandardOutput.ReadLine()
 if($first -cne ('D29 CREATE_NEW recusado PID='+$p.Id)){throw 'D29_TLS_FASE_NEGATIVA_AUSENTE'}
 $actual=Get-CimInstance Win32_Process -Filter ('ProcessId='+$p.Id)
 $listeners=@(Get-NetTCPConnection -State Listen -ErrorAction SilentlyContinue|Where-Object OwningProcess -eq $p.Id|Select-Object LocalAddress,LocalPort,OwningProcess)
 $manifest=Get-Content (Join-Path $backend 'target-d29-helper/versao-atual.json') -Raw -Encoding UTF8|ConvertFrom-Json
 $receipt=[ordered]@{demanda='D29';observadoUtc=[DateTime]::UtcNow.ToString('o');helperHash=$manifest.hashConjunto;caso='CREATE_NEW antes controle positivo';processo=$actual|Select-Object ProcessId,Name,@{Name='CriadoUtc';Expression={$_.CreationDate.ToUniversalTime().ToString('o')}};listeners=$listeners;listenerAusente=($actual.Name -eq 'java.exe' -and $listeners.Count -eq 0);semCommandLine=$true}
 [IO.File]::WriteAllText((Join-Path $backend ('evidencias/d29-tls-create-new-os-'+$manifest.hashConjunto+'.json')),($receipt|ConvertTo-Json -Depth 5),[Text.UTF8Encoding]::new($false))
 $out=$p.StandardOutput.ReadToEndAsync();$err=$p.StandardError.ReadToEndAsync();$p.WaitForExit();[IO.File]::WriteAllText((Join-Path $backend ('evidencias/d29-tls-offline-'+$manifest.hashConjunto+'.log')),($first+"`n"+$out.Result+"`n"+$err.Result),[Text.UTF8Encoding]::new($false))
 Write-Output ('D29 TLS CREATE_NEW listenerAusente='+$receipt.listenerAusente+' exit='+$p.ExitCode)
 if($p.ExitCode -ne 0 -or -not $receipt.listenerAusente){throw 'D29_TLS_OFFLINE_RED'}
}finally{if(-not $p.HasExited){$p.Kill();$p.WaitForExit()};$p.Dispose()}
