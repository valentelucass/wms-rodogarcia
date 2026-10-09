[CmdletBinding()]
param([ValidateSet('xml-ajuste','ajuste','financeiro','financeiro-validar','financeiro-limites','xml-validar','xml-limites','variantes','variantes-financeiro','variantes-validar','listas','listas-estoque','concorrencia')][string]$Etapa='listas',[switch]$ExecutarSqlD27,[string]$Jar='target-d27-numerico-final/wms-backend-0.0.1-SNAPSHOT.jar',[ValidatePattern('^D27[A-F0-9]{8}$')][string]$BaseRodada)
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
  if(-not $ExecutarSqlD27){throw 'D27_OPT_IN_SQL_AUSENTE'}
  $readyFile=Join-Path $raiz 'orchestracao/.runtime/d26-prumo-pronto.json'
  if(-not(Test-Path -LiteralPath $readyFile)){throw 'D27_ATESTADO_PRUMO_AUSENTE'}
  $r=Get-Content -LiteralPath $readyFile -Encoding UTF8 -Raw|ConvertFrom-Json
  if($r.estado -cne 'PRONTO_WMSDEV_ATESTADO' -or $r.banco -cne 'WMS_DEV' -or $r.login -cne 'WMSDEV' -or $r.servidor -cne 'ROD-SRVW-001' -or $r.host -cne '127.0.0.1' -or $r.porta -ne 1433 -or $r.direitosAtestados -ne $true -or $r.historicoIgualD24 -ne $true -or $r.aplicacaoSaPermitida -ne $false -or $r.prodConectado -ne $false){throw 'D27_ATESTADO_DIVERGENTE'}
  if($Etapa -eq 'it' -and $r.vazioPreIT -ne $true){throw 'D27_IT_VAZIO_NAO_ATESTADO'}
  $signal=Join-Path $raiz 'orchestracao/.runtime/d27-prumo-aplicado.json'
  if(-not(Test-Path -LiteralPath $signal)){throw 'D27_V10_ATESTADO_AUSENTE'}
  $v=Get-Content -LiteralPath $signal -Encoding UTF8 -Raw|ConvertFrom-Json
  if($v.estado -cne 'V10_DEV_APLICADA_CONFERIDA_CEDRO_PODE_INICIAR_HTTP' -or $v.banco -cne 'WMS_DEV' -or $v.v10Success -ne $true -or $v.checkHabilitadoConfiavel -ne $true -or $v.seisTiposExatos -ne $true -or $v.historicoSQLV1V9IgualD24 -ne $true -or $v.permissoesWmsdevAntesDepoisIguais -ne $true -or $v.prodConectado -ne $false){throw 'D27_V10_ATESTADO_DIVERGENTE'}
  $psi.EnvironmentVariables['WMS_D27_V10_CHECKSUM']='-562012523'
  . (Join-Path $raiz 'database/scripts/d26-credencial-aplicacao.ps1')
  $p=Get-WmsDevApplicationProfile
  if($p.host -cne '127.0.0.1' -or $p.port -ne 1433 -or $p.database -cne 'WMS_DEV' -or $p.login -cne 'WMSDEV' -or $p.server -cne $r.servidor -or $p.encrypt -ne $true -or $p.trustServerCertificate -ne $false -or [string]::IsNullOrWhiteSpace($p.certificateHost) -or -not(Test-Path -LiteralPath $p.truststore)){throw 'D27_PERFIL_PUBLICO_DIVERGENTE'}
  $cred=Get-WmsDevApplicationCredential
  if($cred.UserName -cne 'WMSDEV' -or $cred.Password.Length -lt 32){throw 'D27_CREDENCIAL_PROPRIA_RECUSADA'}
  $psi.EnvironmentVariables['WMS_DB_HOST']='127.0.0.1';$psi.EnvironmentVariables['WMS_DB_PORT']='1433';$psi.EnvironmentVariables['WMS_DB_NAME']='WMS_DEV';$psi.EnvironmentVariables['WMS_DB_USER']='WMSDEV';$psi.EnvironmentVariables['WMS_DB_CONFIRMED_TARGET']='127.0.0.1:1433/WMS_DEV';$psi.EnvironmentVariables['WMS_DB_CONFIRMED_SERVER']=$p.server
  $psi.EnvironmentVariables['WMS_DB_CERTIFICATE_HOST']=$p.certificateHost;$psi.EnvironmentVariables['WMS_DB_TRUST_STORE']=$p.truststore;$psi.EnvironmentVariables['WMS_DB_TRUST_STORE_PASSWORD']=$p.trustStorePassword
  $bstr=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($cred.Password)
  $senha=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
  $psi.EnvironmentVariables['WMS_DB_PASSWORD']=$senha
 }
 $jarFull=[IO.Path]::GetFullPath((Join-Path $backend $Jar))
 if(-not $jarFull.StartsWith($backend+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'D27_JAR_FORA_BACKEND'}
 $psi.Arguments='-Xmx384m -cp "target-d27-helper;target-d27-helper/lib/*" EnsaioD27 '+$Etapa+' "'+$jarFull+'"'
 if($BaseRodada){$psi.Arguments+=' '+$BaseRodada}
 $processo=New-Object Diagnostics.Process;$processo.StartInfo=$psi
 if(-not $processo.Start()){throw 'D27_PROCESSO_NAO_INICIADO'}
 $psi.EnvironmentVariables.Remove('WMS_DB_PASSWORD')
 if($bstr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr);$bstr=[IntPtr]::Zero}
 if($cred){$cred.Password.Dispose();$cred=$null}
 $out=$processo.StandardOutput.ReadToEndAsync();$err=$processo.StandardError.ReadToEndAsync()
 $processo.WaitForExit()
 $log=$out.Result+"`n"+$err.Result
 if($senha){$log=$log.Replace($senha,'[SEGREDO_OMITIDO]')}
 $senha=$null
 $stamp=[DateTime]::UtcNow.ToString('yyyyMMddTHHmmssfff')
 [IO.File]::WriteAllText((Join-Path $backend "evidencias/d27-launch-$Etapa-$stamp.log"),$log,[Text.UTF8Encoding]::new($false))
 Write-Output $log
 if($processo.ExitCode -ne 0){throw 'D27_ENSAIO_FALHOU_EVIDENCIA_PRESERVADA'}
} catch {
 if($_.Exception.Message -cmatch '^D27_[A-Z0-9_]+$'){Write-Output $_.Exception.Message}else{Write-Output 'D27_LAUNCHER_FALHOU_SEM_DETALHE_PRIVADO'}
 exit 1
} finally {
 if($bstr -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)}
 if($cred){$cred.Password.Dispose()}
 $senha=$null
 if($processo){if(-not $processo.HasExited){$processo.Kill();$processo.WaitForExit()};$processo.Dispose()}
}
