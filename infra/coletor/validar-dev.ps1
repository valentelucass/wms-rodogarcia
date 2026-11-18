[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RuntimeReceipt,[switch]$Navegador,[switch]$Saida)
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -ceq 'Desktop'){$env:PSModulePath=[Environment]::GetEnvironmentVariable('PSModulePath','Machine')}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $root $(if($Saida){'orchestracao/.runtime/saida-coletor-20261029'}else{'orchestracao/.runtime/coletor-tarefas-20261028'})
. (Join-Path $root 'infra/dev/artefato-backend.ps1')
. (Join-Path $root 'infra/notazz/runtime-validacao.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'infra/auth/login-protegido.ps1')
$process=$null;$login=$null
try{
 $runtime=Assert-NotazzCurrentRuntime $RuntimeReceipt
 $guard=Invoke-WmsDev02Guard
 if(!$guard.guardaRealAprovada -or @($guard.checks|Where-Object {!$_.passou}).Count){throw 'COLETOR_GUARDA_RECUSADA'}
 if($guard.tls.proveniencia.reciboPID -ne $runtime.runtime.sql.pid -or $guard.tls.proveniencia.reciboInicioUtc -cne $runtime.runtime.sql.startUtc){throw 'COLETOR_PROCESSO_SQL_MUDOU'}
 $guardPath=Join-Path $area ('guarda-http-'+[guid]::NewGuid().ToString('N')+'.json')
 Write-WmsDevCreateOnlyJson $guardPath $guard
 $authProof=Join-Path $root 'orchestracao/.runtime/qual-conf01/sql01/cedro/ddc2555b-5518-4fa7-b486-694e6c983e2f/http/auth-result.json'
 if((Get-FileHash -LiteralPath $authProof).Hash -ine 'c0170836bdf6f25df25b8e94e2c7c465455b1c5a2e1336920c138dad27ed1a97'){throw 'COLETOR_LOGIN_PROVENIENCIA_INVALIDA'}
 $auth=Get-Content -LiteralPath $authProof -Raw|ConvertFrom-Json
 $loginPath=Join-Path $env:LOCALAPPDATA 'Rodogarcia/WMS/dev-test-users/boot02-ddc2555b-5518-4fa7-b486-694e6c983e2f.clixml'
 Assert-WmsLoginPrivatePath (Split-Path $loginPath -Parent);Assert-WmsLoginPrivatePath $loginPath
 $login=Import-Clixml -LiteralPath $loginPath
 if($login.banco -cne 'WMS_DEV' -or $login.email -cne 'gestor.teste@wms.invalid' -or $login.id -cne $auth.accountId -or $login.senha -isnot [Security.SecureString]){throw 'COLETOR_LOGIN_INVALIDO'}
 $info=[Diagnostics.ProcessStartInfo]::new();$info.FileName='C:\Users\suporte\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe';$info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.WorkingDirectory=$root
 $testScript=if($Saida){'testes/saida-dev.mjs'}elseif($Navegador){'testes/navegador-dev.mjs'}else{'testes/fluxos-dev.mjs'}
 $info.Arguments='"'+(Join-Path $PSScriptRoot $testScript)+'" "'+$area+'"'
 $info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 $process=[Diagnostics.Process]::new();$process.StartInfo=$info
 if(!$process.Start()){throw 'COLETOR_TESTE_NAO_INICIOU'}
 $stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
 $plain=[Management.Automation.PSCredential]::new($login.email,$login.senha)
 $payload=@{base=$runtime.runtime.localOrigin;email=$plain.UserName;senha=$plain.GetNetworkCredential().Password;sourceHash=$runtime.runtime.sourceHash;jarSha256=$runtime.candidate.jar.sha256}|ConvertTo-Json -Compress
 $process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$plain=$null;$login.senha.Dispose();$login=$null
 $process.WaitForExit();Write-Output $stdout.Result
 if($process.ExitCode -notin @(0,75)){throw 'COLETOR_FLUXOS_NAO_APROVADOS'}
 $exit=$process.ExitCode
}catch{
 [pscustomobject]@{estado='INTERROMPIDO';tipo=$_.Exception.GetType().FullName;id=$_.FullyQualifiedErrorId;linha=$_.InvocationInfo.ScriptLineNumber;mensagemBrutaPublicada=$false}|ConvertTo-Json -Depth 4
 $exit=20
}finally{if($login){$login.senha.Dispose()};if($process){if(!$process.HasExited){$process.Kill();$process.WaitForExit()};$process.Dispose()}}
exit $exit
