[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidenceDirectory,[switch]$Executar)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=[IO.Path]::GetFullPath($EvidenceDirectory)
$allowed=Join-Path $root 'orchestracao\.runtime\notazz-lifecycle-20261023'
if($area -ine $allowed){throw 'NFH_EVIDENCIA_FORA_DO_ESCOPO'}
[void][IO.Directory]::CreateDirectory($area)
if(-not $Executar){
 & (Join-Path $root '../.runtime/memory-control/Executar-Batch.ps1') -Executable (Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe') -ArgumentList @('-NoProfile','-File',$PSCommandPath,'-EvidenceDirectory',$area,'-Executar') -WorkingDirectory $root -ReceiptPath (Join-Path $area ('memoria-'+[guid]::NewGuid().ToString('N')+'.json')) -MaxMinutes 35
 exit $LASTEXITCODE
}
Set-Location -LiteralPath $root
. (Join-Path $root 'orchestracao/.runtime/notazz-implementation-20261023/active-read-runtime.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'database/scripts/d26-credencial-aplicacao.ps1')
. (Join-Path $root 'infra/auth/login-protegido.ps1')
. (Join-Path $root 'infra/sql01/transporte-privado.ps1')
$javaProcess=$null;$nodeProcess=$null;$credential=$null;$signing=$null;$login=$null
$phase='GUARDAS';$report=[ordered]@{id='BE10-NOTAZZ-SQL01';started=[DateTime]::UtcNow.ToString('o');state='PREPARO';database='WMS_DEV';login='WMSDEV';port=25582;production=$false;externalCalls=0;businessFixtures='Somente HTTP autenticado';rawLogsSaved=$false}
try{
 $runtime=Assert-NotazzActiveReadRuntime
 if(!$runtime.fullCurrentSourceEquivalent -or !$runtime.allRecordedSourceHashesEqual){throw 'NFH_FONTE_ATIVA_DIVERGENTE'}
 $guard=Invoke-WmsDev02Guard
 if(!$guard.guardaRealAprovada -or @($guard.checks).Count -ne 26 -or @($guard.checks|Where-Object {!$_.passou}).Count){throw 'NFH_GUARDA_RECUSADA'}
 if($guard.tls.certificateSha256 -cne $runtime.startupGuard.tls.certificateSha256 -or $guard.tls.proveniencia.reciboPID -ne $runtime.runtime.sql.pid -or $guard.tls.proveniencia.reciboInicioUtc -cne $runtime.runtime.sql.startUtc){throw 'NFH_SQL_ATUAL_DIVERGENTE'}
 $guardPath=Join-Path $area ('guarda-'+[guid]::NewGuid().ToString('N')+'.json')
 Write-WmsDevCreateOnlyJson $guardPath $guard
 if(@(Get-NetTCPConnection -State Listen -LocalPort 25582 -ErrorAction SilentlyContinue).Count){throw 'NFH_PORTA_OCUPADA'}
 foreach($name in @('server-ready.json','provider-ready.json','stop')){
  $old=Join-Path $area $name
  if(Test-Path -LiteralPath $old){Move-Item -LiteralPath $old -Destination ($old+'.anterior-'+[guid]::NewGuid().ToString('N'))}
 }
 $gate=[ordered]@{allowed=$true;database='WMS_DEV';login='WMSDEV';externalCalls=0;server=$guard.alvo.servidor;certificateHost=$guard.perfilProtegido.certificateHost;truststore=$guard.perfilProtegido.truststore;guardPath=$guardPath;guardSha256=(Get-FileHash -LiteralPath $guardPath).Hash;sourceHash=$runtime.runtime.sourceHash;jar=$runtime.candidate.jar.path;jarSha256=$runtime.candidate.jar.sha256}
 [IO.File]::WriteAllText((Join-Path $area 'gate.json'),($gate|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
 $phase='ARTEFATO_ATUAL'
 Add-Type -AssemblyName System.IO.Compression.FileSystem
 $expanded=Join-Path $area 'pacote';[void][IO.Directory]::CreateDirectory($expanded)
 $zip=[IO.Compression.ZipFile]::OpenRead($runtime.candidate.jar.path)
 try{
  foreach($entry in $zip.Entries){
   if($entry.FullName -notmatch '^BOOT-INF/(classes/.+|lib/[^/]+\.jar)$' -or $entry.FullName.EndsWith('/')){continue}
   if($entry.FullName -match '(?i)(^|/)h2-[^/]*\.jar$'){throw 'NFH_H2_RECUSADO'}
   $target=[IO.Path]::GetFullPath((Join-Path $expanded $entry.FullName))
   if(!$target.StartsWith($expanded+'\',[StringComparison]::OrdinalIgnoreCase)){throw 'NFH_ZIP_PATH_INVALIDO'}
   [void][IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($target))
   [IO.Compression.ZipFileExtensions]::ExtractToFile($entry,$target,$true)
  }
 }finally{$zip.Dispose()}
 $java=$runtime.candidate.java.path;$javac=Join-Path ([IO.Path]::GetDirectoryName($java)) 'javac.exe'
 $classes=Join-Path $expanded 'BOOT-INF/classes';$libs=Join-Path $expanded 'BOOT-INF/lib/*';$runner=Join-Path $area 'runner-classes';[void][IO.Directory]::CreateDirectory($runner)
 $cp=$classes+';'+$libs;$source=Join-Path $PSScriptRoot 'testes/FiscalSqlDev.java'
 $phase='COMPILACAO_RUNNER'
 & $javac '-J-Xmx256m' '-encoding' 'UTF-8' '-cp' $cp '-d' $runner $source 2> (Join-Path $area 'runner-compile.txt')
 if($LASTEXITCODE -ne 0){throw 'NFH_RUNNER_NAO_COMPILOU'}
 $phase='CANAL_PROTEGIDO'
 $credential=Get-WmsDevApplicationCredential
 if($credential.UserName -cne 'WMSDEV'){throw 'NFH_IDENTIDADE_INVALIDA'}
 $privatePath=Get-WmsLoginMaterialPath;Assert-WmsLoginPrivatePath (Split-Path $privatePath -Parent);Assert-WmsLoginPrivatePath $privatePath
 $signing=Import-Clixml -LiteralPath $privatePath
 if($signing -isnot [Security.SecureString]){throw 'NFH_ASSINATURA_INVALIDA'}
 $info=[Diagnostics.ProcessStartInfo]::new();$info.FileName=$java;$info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.WorkingDirectory=$root
 $info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 $info.EnvironmentVariables.Clear()
 foreach($name in @('SystemRoot','WINDIR','TEMP','TMP','USERPROFILE','LOCALAPPDATA','APPDATA')){ $value=[Environment]::GetEnvironmentVariable($name,'Process');if($value){$info.EnvironmentVariables[$name]=$value} }
 $arguments=@('-Xmx640m','-XX:MaxMetaspaceSize=256m','-XX:ActiveProcessorCount=2','-cp',($cp+';'+$runner),'FiscalSqlDev',$area,$runtime.candidate.config.path)
 foreach($arg in $arguments){if($arg -match '["\r\n]'){throw 'NFH_ARGUMENTO_INVALIDO'}}
 $info.Arguments=(@($arguments|ForEach-Object {'"'+$_+'"'}) -join ' ')
 $javaProcess=[Diagnostics.Process]::new();$javaProcess.StartInfo=$info
 if(!$javaProcess.Start()){throw 'NFH_RUNNER_NAO_INICIOU'}
 $jout=$javaProcess.StandardOutput.ReadToEndAsync();$jerr=$javaProcess.StandardError.ReadToEndAsync()
 Send-WmsSql01PrivateMaterial $javaProcess.StandardInput.BaseStream $credential.Password $signing
 $javaProcess.StandardInput.Close();$credential.Password.Dispose();$credential=$null;$signing.Dispose();$signing=$null
 $phase='SERVIDOR_FICTICIO_COM_SQL_REAL'
 $until=[DateTime]::UtcNow.AddSeconds(100)
 while(!(Test-Path -LiteralPath (Join-Path $area 'server-ready.json'))){
  if($javaProcess.HasExited){throw 'NFH_RUNNER_ENCERROU'}
  if([DateTime]::UtcNow -gt $until){throw 'NFH_SERVIDOR_NAO_FICOU_PRONTO'}
  Start-Sleep -Milliseconds 250
 }
 $listeners=@(Get-NetTCPConnection -State Listen -OwningProcess $javaProcess.Id -ErrorAction SilentlyContinue)
 if(!$listeners.Count -or @($listeners|Where-Object {$_.LocalPort -ne 25582 -or $_.LocalAddress -cne '127.0.0.1'}).Count){throw 'NFH_LISTENER_INVALIDO'}
 $report.runner=@{pid=$javaProcess.Id;started=$javaProcess.StartTime.ToUniversalTime().ToString('o');sourceSha256=(Get-FileHash -LiteralPath $source).Hash;jarSha256=$gate.jarSha256;sourceHash=$gate.sourceHash;guardPath=$guardPath;guardSha256=$gate.guardSha256}
 $phase='HTTP_FICTICIO_PERSISTENCIA_REAL'
 $authProof=Join-Path $root 'orchestracao/.runtime/qual-conf01/sql01/cedro/ddc2555b-5518-4fa7-b486-694e6c983e2f/http/auth-result.json'
 if((Get-FileHash -LiteralPath $authProof).Hash -ine 'c0170836bdf6f25df25b8e94e2c7c465455b1c5a2e1336920c138dad27ed1a97'){throw 'NFH_LOGIN_PROVENIENCIA_INVALIDA'}
 $auth=Get-Content -LiteralPath $authProof -Raw|ConvertFrom-Json
 $loginPath=Join-Path $env:LOCALAPPDATA 'Rodogarcia/WMS/dev-test-users/boot02-ddc2555b-5518-4fa7-b486-694e6c983e2f.clixml'
 Assert-WmsLoginPrivatePath (Split-Path $loginPath -Parent);Assert-WmsLoginPrivatePath $loginPath
 $login=Import-Clixml -LiteralPath $loginPath
 if($login.banco -cne 'WMS_DEV' -or $login.email -cne 'gestor.teste@wms.invalid' -or $login.id -cne $auth.accountId -or $login.senha -isnot [Security.SecureString]){throw 'NFH_LOGIN_INVALIDO'}
 $nodeInfo=[Diagnostics.ProcessStartInfo]::new();$nodeInfo.FileName='C:\Program Files\nodejs\node.exe';$nodeInfo.UseShellExecute=$false;$nodeInfo.CreateNoWindow=$true;$nodeInfo.WorkingDirectory=$root
 $nodeInfo.Arguments='"'+(Join-Path $PSScriptRoot 'testes/ciclo-sql-dev.mjs')+'" "'+$area+'"'
 $nodeInfo.RedirectStandardInput=$true;$nodeInfo.RedirectStandardOutput=$true;$nodeInfo.RedirectStandardError=$true
 $nodeProcess=[Diagnostics.Process]::new();$nodeProcess.StartInfo=$nodeInfo
 if(!$nodeProcess.Start()){throw 'NFH_HTTP_NAO_INICIOU'}
 $nout=$nodeProcess.StandardOutput.ReadToEndAsync();$nerr=$nodeProcess.StandardError.ReadToEndAsync()
 $plain=[Management.Automation.PSCredential]::new($login.email,$login.senha)
 $payload=@{email=$plain.UserName;senha=$plain.GetNetworkCredential().Password}|ConvertTo-Json -Compress
 $nodeProcess.StandardInput.Write($payload);$nodeProcess.StandardInput.Close();$payload=$null;$plain=$null;$login.senha.Dispose();$login=$null
 while(!$nodeProcess.WaitForExit(500)) {if($javaProcess.HasExited){throw 'NFH_SERVIDOR_ENCERROU_DURANTE_TESTE'}}
 Write-Output $nout.Result
 if($nodeProcess.ExitCode -eq 75){$report.state='RESTART_REQUIRED'}
 elseif($nodeProcess.ExitCode -ne 0){throw 'NFH_CICLO_HTTP_FALHOU'}
 else{$report.state='PASS_SQL_REAL_TRANSPORTE_CONTROLADO'}
}catch{
 $report.state='FAIL';$report.phase=$phase;$ex=$_.Exception;$code='NFH_VALIDACAO_FALHOU'
 while($ex){if($ex.Message -cmatch '^NFH_[A-Z0-9_]+$'){$code=$ex.Message};$ex=$ex.InnerException}
 $report.failure=@{code=$code;line=$_.InvocationInfo.ScriptLineNumber;type=$_.Exception.GetType().FullName}
}finally{
 if($credential){$credential.Password.Dispose()};if($signing){$signing.Dispose()};if($login){$login.senha.Dispose()}
 if($javaProcess -and !$javaProcess.HasExited){[IO.File]::WriteAllText((Join-Path $area 'stop'),'');if(!$javaProcess.WaitForExit(15000)){$javaProcess.Kill();$javaProcess.WaitForExit()}}
 if($javaProcess){$report.runnerExit=$javaProcess.ExitCode;$javaProcess.Dispose()};if($nodeProcess){$nodeProcess.Dispose()}
 $report.finished=[DateTime]::UtcNow.ToString('o')
 $out=Join-Path $area ('execucao-'+[guid]::NewGuid().ToString('N')+'.json')
 [IO.File]::WriteAllText($out,($report|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
 Write-Output ([pscustomobject]@{state=$report.state;phase=$report.phase;failure=$report.failure;evidence=$out}|ConvertTo-Json -Depth 4 -Compress)
}
if($report.state -ceq 'FAIL'){exit 20}
