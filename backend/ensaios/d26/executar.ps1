[CmdletBinding()]
param([ValidateSet('offline','it','http','retomar','retomar-avaria','retomar-xml','retomar-contagem','retomar-complementos','retomar-complementos-escrita','retomar-cadastros','retomar-movimento','retomar-cargas','retomar-extras','retomar-extras-mutacoes','retomar-retorno-extra','retomar-financeiro','retomar-fechamento','retomar-resolucao-carga','retomar-resolucao-carga-final','retomar-marco','retomar-vigencias','retomar-vigencia-final','retomar-leituras-finais','retomar-auditoria-final','retomar-corte-real','retomar-contagem-complementos','retomar-concorrencia','concorrencia')][string]$Etapa='offline',[switch]$ExecutarSqlD26,[string]$Jar='target-d26-financeiro/wms-backend-0.0.1-SNAPSHOT.jar',[ValidatePattern('^D26[A-F0-9]{8}$')][string]$BaseRodada)
$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$raiz=(Resolve-Path "$backend/..").Path
$java='C:/Users/suporte/AppData/Local/Programs/Eclipse Adoptium/jdk-21/bin/java.exe'
$cred=$null;$bstr=[IntPtr]::Zero;$senha=$null;$processo=$null
try {
 $psi=New-Object Diagnostics.ProcessStartInfo
 $psi.FileName=$java;$psi.WorkingDirectory=$backend;$psi.UseShellExecute=$false;$psi.CreateNoWindow=$true
 $psi.RedirectStandardOutput=$true;$psi.RedirectStandardError=$true
 $psi.EnvironmentVariables.Clear()
 foreach($key in @('SystemRoot','WINDIR','COMSPEC','TEMP','TMP','LOCALAPPDATA','APPDATA','USERPROFILE','PATH','PATHEXT')){if([Environment]::GetEnvironmentVariable($key,'Process')){$psi.EnvironmentVariables[$key]=[Environment]::GetEnvironmentVariable($key,'Process')}}
 if($Etapa -ne 'offline') {
  if(-not $ExecutarSqlD26){throw 'D26_OPT_IN_SQL_AUSENTE'}
  $readyFile=Join-Path $raiz 'orchestracao/.runtime/d26-prumo-pronto.json'
  if(-not(Test-Path -LiteralPath $readyFile)){throw 'D26_ATESTADO_PRUMO_AUSENTE'}
  $r=Get-Content -LiteralPath $readyFile -Encoding UTF8 -Raw|ConvertFrom-Json
  if($r.estado -cne 'PRONTO_WMSDEV_ATESTADO' -or $r.banco -cne 'WMS_DEV' -or $r.login -cne 'WMSDEV' -or $r.servidor -cne 'ROD-SRVW-001' -or $r.host -cne '127.0.0.1' -or $r.porta -ne 1433 -or $r.direitosAtestados -ne $true -or $r.historicoIgualD24 -ne $true -or $r.aplicacaoSaPermitida -ne $false -or $r.prodConectado -ne $false){throw 'D26_ATESTADO_DIVERGENTE'}
  if($Etapa -eq 'it' -and $r.vazioPreIT -ne $true){throw 'D26_IT_VAZIO_NAO_ATESTADO'}
  . (Join-Path $raiz 'database/scripts/d26-credencial-aplicacao.ps1')
  $p=Get-WmsDevApplicationProfile
  if($p.host -cne '127.0.0.1' -or $p.port -ne 1433 -or $p.database -cne 'WMS_DEV' -or $p.login -cne 'WMSDEV' -or $p.server -cne $r.servidor -or $p.encrypt -ne $true -or $p.trustServerCertificate -ne $false -or [string]::IsNullOrWhiteSpace($p.certificateHost) -or -not(Test-Path -LiteralPath $p.truststore)){throw 'D26_PERFIL_PUBLICO_DIVERGENTE'}
  $cred=Get-WmsDevApplicationCredential
  if($cred.UserName -cne 'WMSDEV' -or $cred.Password.Length -lt 32){throw 'D26_CREDENCIAL_PROPRIA_RECUSADA'}
  $psi.EnvironmentVariables['WMS_DB_HOST']='127.0.0.1';$psi.EnvironmentVariables['WMS_DB_PORT']='1433';$psi.EnvironmentVariables['WMS_DB_NAME']='WMS_DEV';$psi.EnvironmentVariables['WMS_DB_USER']='WMSDEV';$psi.EnvironmentVariables['WMS_DB_CONFIRMED_TARGET']='127.0.0.1:1433/WMS_DEV';$psi.EnvironmentVariables['WMS_DB_CONFIRMED_SERVER']=$p.server
  $psi.EnvironmentVariables['WMS_DB_CERTIFICATE_HOST']=$p.certificateHost;$psi.EnvironmentVariables['WMS_DB_TRUST_STORE']=$p.truststore;$psi.EnvironmentVariables['WMS_DB_TRUST_STORE_PASSWORD']=$p.trustStorePassword
  $bstr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($cred.Password)
  $senha=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
  $psi.EnvironmentVariables['WMS_DB_PASSWORD']=$senha
 }
 $jarFull=[IO.Path]::GetFullPath((Join-Path $backend $Jar))
 if(-not $jarFull.StartsWith($backend+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'D26_JAR_FORA_BACKEND'}
 $psi.Arguments='-Xmx384m -cp "target-d26-helper;target-d26-helper/lib/*" EnsaioD26 '+$Etapa+' "'+$jarFull+'"'
 if($Etapa.StartsWith('retomar')){if(-not $BaseRodada){throw 'D26_BASE_RODADA_AUSENTE'};$psi.Arguments+=' '+$BaseRodada}
 $processo=New-Object Diagnostics.Process;$processo.StartInfo=$psi
 if(-not $processo.Start()){throw 'D26_PROCESSO_NAO_INICIADO'}
 $psi.EnvironmentVariables.Remove('WMS_DB_PASSWORD')
 if($bstr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr);$bstr=[IntPtr]::Zero}
 if($cred){$cred.Password.Dispose();$cred=$null}
 $out=$processo.StandardOutput.ReadToEndAsync();$err=$processo.StandardError.ReadToEndAsync()
 $processo.WaitForExit()
 $log=$out.Result+"`n"+$err.Result
 if($senha){$log=$log.Replace($senha,'[SEGREDO_OMITIDO]')}
 $senha=$null
 $stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfff')
 [IO.File]::WriteAllText((Join-Path $backend "evidencias/d26-launch-$Etapa-$stamp.log"),$log,[Text.UTF8Encoding]::new($false))
 Write-Output $log
 if($processo.ExitCode -ne 0){throw 'D26_ENSAIO_FALHOU_EVIDENCIA_PRESERVADA'}
} catch {
 if($_.Exception.Message -cmatch '^D26_[A-Z0-9_]+$'){Write-Output $_.Exception.Message}else{Write-Output 'D26_LAUNCHER_FALHOU_SEM_DETALHE_PRIVADO'}
 exit 1
} finally {
 if($bstr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)}
 if($cred){$cred.Password.Dispose()}
 $senha=$null
 if($processo){if(-not $processo.HasExited){$processo.Kill();$processo.WaitForExit()};$processo.Dispose()}
}
