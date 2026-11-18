[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$RuntimeReceipt,[ValidateSet('preflight','preparar','enderecar','inventario','separar','posicao','mobile','final')][string]$Etapa='preflight')
$ErrorActionPreference='Stop'
if($PSVersionTable.PSEdition -ceq 'Desktop'){$env:PSModulePath=[Environment]::GetEnvironmentVariable('PSModulePath','Machine')}
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $root 'orchestracao/.runtime/coletor-profundo-20261029'
[void][IO.Directory]::CreateDirectory($area)
. (Join-Path $root 'infra/dev/artefato-backend.ps1')
. (Join-Path $root 'infra/notazz/runtime-validacao.ps1')
. (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')
. (Join-Path $root 'infra/auth/login-protegido.ps1')
$process=$null;$primary=$null;$actors=$null;$permanent=$null
try{
 $runtime=Assert-NotazzCurrentRuntime $RuntimeReceipt
 $guard=Invoke-WmsDev02Guard
 if(!$guard.guardaRealAprovada -or @($guard.checks|Where-Object {!$_.passou}).Count -or $guard.tls.proveniencia.reciboPID -ne $runtime.runtime.sql.pid -or $guard.tls.proveniencia.reciboInicioUtc -cne $runtime.runtime.sql.startUtc){throw 'PROFUNDO_GUARDA_RECUSADA'}
 Write-WmsDevCreateOnlyJson (Join-Path $area ('guarda-'+[guid]::NewGuid().ToString('N')+'.json')) $guard
 $directory=Join-Path $env:LOCALAPPDATA 'Rodogarcia/WMS/dev-test-users'
 $primaryPath=Join-Path $directory 'boot02-ddc2555b-5518-4fa7-b486-694e6c983e2f.clixml'
 $actorsPath=Join-Path $directory '20261010-1bbe9c5b.clixml'
 Assert-WmsLoginPrivatePath $directory;Assert-WmsLoginPrivatePath $primaryPath;Assert-WmsLoginPrivatePath $actorsPath
 $primary=Import-Clixml -LiteralPath $primaryPath
 $actors=Import-Clixml -LiteralPath $actorsPath
 if($primary.banco -cne 'WMS_DEV' -or $primary.email -cne 'gestor.teste@wms.invalid' -or $primary.senha -isnot [Security.SecureString] -or $actors.banco -cne 'WMS_DEV'){throw 'PROFUNDO_IDENTIDADE_RECUSADA'}
 $permanentPath=Join-Path $directory 'coletor-profundo-20261029.clixml'
 if(!(Test-Path -LiteralPath $permanentPath)){
   $generated=@()
   foreach($entry in @($actors.contas|Where-Object {$_.perfil -in @('SUPERVISOR','OPERACAO')})){
     $value='CP29!'+[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
     $generated+=@{id=$entry.id;email=$entry.email;senha=(ConvertTo-SecureString -String $value -AsPlainText -Force)};$value=$null
   }
   @{banco='WMS_DEV';contas=$generated;tipo='PRIMEIRO_ACESSO_CONTAS_FICTICIAS'}|Export-Clixml -LiteralPath $permanentPath -Depth 5
   Set-Acl -LiteralPath $permanentPath -AclObject (Get-Acl -LiteralPath $actorsPath)
   foreach($entry in $generated){$entry.senha.Dispose()};$generated=$null
 }
 Assert-WmsLoginPrivatePath $permanentPath
 $permanent=Import-Clixml -LiteralPath $permanentPath
 if($permanent.banco -cne 'WMS_DEV' -or $permanent.tipo -cne 'PRIMEIRO_ACESSO_CONTAS_FICTICIAS'){throw 'PROFUNDO_MATERIAL_NOVO_INVALIDO'}
 $credentials=@()
 foreach($entry in @($primary)+@($actors.contas|Where-Object {$_.perfil -in @('SUPERVISOR','OPERACAO')})){
   if($entry.senha -isnot [Security.SecureString] -or $entry.email -notmatch '^(gestor|supervisor|operacao)\.teste@wms\.invalid$'){throw 'PROFUNDO_CONTA_NAO_FICTICIA'}
   $plain=[Management.Automation.PSCredential]::new($entry.email,$entry.senha)
   $item=@{id=$entry.id;email=$plain.UserName;senha=$plain.GetNetworkCredential().Password}
   $saved=@($permanent.contas|Where-Object {$_.id -ceq $entry.id})
   if($saved.Count -eq 1){$next=[Management.Automation.PSCredential]::new($saved[0].email,$saved[0].senha);$item.novaSenha=$next.GetNetworkCredential().Password;$next=$null}
   $credentials+=$item
 }
 if($credentials.Count -ne 3){throw 'PROFUNDO_TRES_PERFIS_OBRIGATORIOS'}
 $info=[Diagnostics.ProcessStartInfo]::new();$info.FileName='C:\Users\suporte\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin\node.exe';$info.UseShellExecute=$false;$info.CreateNoWindow=$true;$info.WorkingDirectory=$root
 $info.Arguments='"'+(Join-Path $PSScriptRoot 'testes/profundo-dev.mjs')+'" "'+$area+'" "'+$Etapa+'"'
 $info.RedirectStandardInput=$true;$info.RedirectStandardOutput=$true;$info.RedirectStandardError=$true
 $process=[Diagnostics.Process]::new();$process.StartInfo=$info
 if(!$process.Start()){throw 'PROFUNDO_PROCESSO_NAO_INICIOU'}
 $stdout=$process.StandardOutput.ReadToEndAsync();$stderr=$process.StandardError.ReadToEndAsync()
 $payload=@{base=$runtime.runtime.localOrigin;actors=$credentials;sourceHash=$runtime.runtime.sourceHash;jarSha256=$runtime.candidate.jar.sha256;runId=$runtime.runtime.runId}|ConvertTo-Json -Depth 5 -Compress
 $process.StandardInput.Write($payload);$process.StandardInput.Close();$payload=$null;$credentials=$null;$plain=$null
 $process.WaitForExit();Write-Output $stdout.Result
 if($process.ExitCode -ne 0){throw 'PROFUNDO_ETAPA_NAO_APROVADA'}
 $exit=0
}catch{
 [pscustomobject]@{estado='INTERROMPIDO';tipo=$_.Exception.GetType().FullName;id=$_.FullyQualifiedErrorId;linha=$_.InvocationInfo.ScriptLineNumber;mensagemBrutaPublicada=$false}|ConvertTo-Json
 $exit=20
}finally{
 if($primary -and $primary.senha){$primary.senha.Dispose()};if($actors){foreach($entry in $actors.contas){if($entry.senha -is [Security.SecureString]){$entry.senha.Dispose()}}}
 if($permanent){foreach($entry in $permanent.contas){if($entry.senha -is [Security.SecureString]){$entry.senha.Dispose()}}}
 if($process){if(!$process.HasExited){$process.Kill();$process.WaitForExit()};$process.Dispose()}
}
exit $exit
