[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidenceDirectory)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$repository=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$evidence=[IO.Path]::GetFullPath($EvidenceDirectory)
$allowed=Join-Path $repository 'orchestracao/.runtime'
if (-not $evidence.StartsWith($allowed+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)) { throw 'HARNESS_PATH_OUTSIDE_OWN_EVIDENCE' }
if (Test-Path -LiteralPath $evidence) { throw 'HARNESS_CREATE_ONLY' }
$null=New-Item -ItemType Directory -Path $evidence
. (Join-Path $PSScriptRoot 'launcher-common.ps1')
$results=[Collections.Generic.List[object]]::new()
function Check {
    param([string]$Name,[scriptblock]$Body)
    try { & $Body; $results.Add([pscustomobject]@{name=$Name;estado='PASS'}) }
    catch { $results.Add([pscustomobject]@{name=$Name;estado='FAIL';reason=$_.Exception.Message}) }
}
function Need { param([bool]$Condition,[string]$Code) if(-not $Condition){throw $Code} }
function Refuses {
    param([scriptblock]$Body,[string]$Code)
    $seen=$null
    try { & $Body } catch {
        $exception=$_.Exception
        while($null -ne $exception.InnerException){$exception=$exception.InnerException}
        $seen=$exception.Message
    }
    Need ($seen -ceq $Code) ('EXPECTED_'+$Code)
}
function Config { [IO.File]::ReadAllText((Join-Path $PSScriptRoot 'producao.json')) | ConvertFrom-Json }
function FakeChild {
    param([bool]$HasExited=$false,[bool]$ThrowDispose=$false)
    $value=[pscustomobject]@{checks=0;exited=$HasExited;kills=0;waits=0;disposes=0;throwDispose=$ThrowDispose}
    $value | Add-Member ScriptProperty HasExited { $this.checks++; return $this.exited }
    $value | Add-Member ScriptMethod Kill { $this.kills++; $this.exited=$true }
    $value | Add-Member ScriptMethod WaitForExit { param($timeout) $this.waits++; return $true }
    $value | Add-Member ScriptMethod Dispose { $this.disposes++; if($this.throwDispose){throw 'SYNTHETIC_DISPOSE_FAILURE'} }
    return $value
}
function Stop-WmsProductionBackend {
    param([object]$Handle)
    Need ($null -ne $Handle.Process -and $Handle.RunId -ceq 'synthetic-run') 'BACKEND_WRAPPER_REQUIRED'
    $Handle.stops++
    if($Handle.fail){throw 'SYNTHETIC_BACKEND_STOP_FAILURE'}
    $Handle.Process.Dispose()
}
Check 'config-prod-exclusivo-controle' { Assert-WmsProductionPublicConfig (Config) }
foreach($pair in @(@('database','WMS_DEV'),@('database','master'),@('expectedLogin','WMSDEV'),@('expectedLogin','sa'),@('profile','sqlserver-dev'))){
    $field=$pair[0];$invalid=$pair[1]
    Check ('config-recusa-'+$field+'-'+$invalid) { $c=Config;$c.$field=$invalid;Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_TARGET_OR_IDENTITY_INVALID' }
}
Check 'config-nao-aceita-cookie-inseguro' { $c=Config;$c.secureCookie=$false;Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_PUBLIC_TOPOLOGY_INVALID' }
Check 'config-cookie-string-nao-e-booleano' { $c=Config;$c.secureCookie='true';Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_PUBLIC_TOPOLOGY_INVALID' }
Check 'config-namespace-array-recusado' { $c=Config;$c.database=@('WMS_PROD');Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_PUBLIC_CONFIG_TYPE_INVALID' }
Check 'config-versao-string-recusada' { $c=Config;$c.schemaVersion='1';Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_TARGET_OR_IDENTITY_INVALID' }
Check 'config-nao-aceita-origem-dev' { $c=Config;$c.frontendOrigin='https://example.invalid';Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_PUBLIC_TOPOLOGY_INVALID' }
Check 'porta-string-nao-vira-numero-silenciosamente' { $c=Config;$c.backendPort='25590';Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_PORT_INVALID' }
Check 'portas-distintas' { $c=Config;$c.frontendPort=$c.backendPort;Refuses {Assert-WmsProductionPublicConfig $c} 'PROD_PORTS_EQUAL' }
Check 'listener-proprio-conflito-preservado' {
    $listener=[Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback,0)
    try { $listener.Start();$port=$listener.LocalEndpoint.Port;Refuses {Assert-WmsProductionFreePort $port} ('PROD_PORT_OCCUPIED_'+$port);Need ($listener.Server.IsBound) 'LISTENER_EXISTENTE_INTERROMPIDO' }
    finally{$listener.Stop()}
}
Check 'ambiente-filho-nao-herda-overrides' {
    $previous=[Environment]::GetEnvironmentVariable('JAVA_TOOL_OPTIONS','Process')
    try { [Environment]::SetEnvironmentVariable('JAVA_TOOL_OPTIONS','SYNTHETIC_OVERRIDE','Process');$info=New-WmsProductionChildInfo 'node.exe' '' $repository;Need (-not $info.EnvironmentVariables.ContainsKey('JAVA_TOOL_OPTIONS')) 'OVERRIDE_HERDADO';Need (-not $info.EnvironmentVariables.ContainsKey('SPRING_PROFILES_ACTIVE')) 'SPRING_HERDADO';Need (-not $info.EnvironmentVariables.ContainsKey('VITE_DATA_MODE')) 'VITE_HERDADO' }
    finally{[Environment]::SetEnvironmentVariable('JAVA_TOOL_OPTIONS',$previous,'Process')}
}
Check 'argumento-com-espacos-preservado-em-processo-proprio' {
    $node=(Get-Command node.exe).Source;$value='C:/pasta de teste/artefato.json'
    $args='-e '+(ConvertTo-WmsProductionArgument 'process.stdout.write(process.argv[1])')+' '+(ConvertTo-WmsProductionArgument $value)
    $child=[Diagnostics.Process]::new();$child.StartInfo=New-WmsProductionChildInfo $node $args $repository;$started=$false
    try{$started=$child.Start();$output=$child.StandardOutput.ReadToEnd();$child.WaitForExit();Need ($child.ExitCode -eq 0 -and $output -ceq $value) 'ARGUMENTO_ALTERADO'}finally{Stop-WmsProductionChild $child $started}
}
Check 'argumento-injetado-recusado' {Refuses {ConvertTo-WmsProductionArgument 'arquivo" --outra-opcao'} 'PROD_ARGUMENT_INVALID'}
Check 'cleanup-frontend-unstarted-nao-acessa-HasExited' {$child=FakeChild;Stop-WmsProductionChild $child $false;Need ($child.checks -eq 0 -and $child.kills -eq 0 -and $child.disposes -eq 1) 'UNSTARTED_ASSOCIATION_ERROR'}
Check 'cleanup-fe-falha-nao-pula-backend-guarda' {
    $fe=FakeChild -ThrowDispose $true;$be=[pscustomobject]@{RunId='synthetic-run';Process=(FakeChild);stops=0;fail=$false};$guard=FakeChild
    Refuses {Close-WmsProductionHandles $fe $false $be $guard $false} 'SYNTHETIC_DISPOSE_FAILURE'
    Need ($be.stops -eq 1 -and $be.Process.disposes -eq 1 -and $guard.disposes -eq 1 -and $guard.checks -eq 0) 'CLEANUP_INDEPENDENTE_FALHOU'
}
Check 'cleanup-be-falha-nao-pula-guarda' {
    $be=[pscustomobject]@{RunId='synthetic-run';Process=(FakeChild);stops=0;fail=$true};$guard=FakeChild
    Refuses {Close-WmsProductionHandles $null $false $be $guard $false} 'SYNTHETIC_BACKEND_STOP_FAILURE'
    Need ($be.stops -eq 1 -and $guard.disposes -eq 1) 'GUARDA_NAO_DISPOSTA'
}
Check 'cleanup-apenas-child-proprio-started' {$child=FakeChild;Stop-WmsProductionChild $child $true;Need ($child.kills -eq 1 -and $child.waits -eq 1 -and $child.disposes -eq 1) 'OWN_CHILD_CLEANUP_FAILED'}
Check 'codigos-falhas-distinguem-auth-porta-guarda-build' {Need ((Get-WmsProductionFailureExitCode 'PROD_AUTH_PUBLIC_METADATA_MISSING') -eq 40 -and (Get-WmsProductionFailureExitCode 'PROD_PORT_OCCUPIED_25590') -eq 50 -and (Get-WmsProductionFailureExitCode 'PROD_CURRENT_GUARD_BLOCKED') -eq 20 -and (Get-WmsProductionFailureExitCode 'PROD_BACKEND_BUILD_FAILED') -eq 30) 'EXIT_CODE_CLASSIFICATION_FAILED'}

# Executa o launcher literal numa estrutura nova com compilador sintetico que falha.
# Nenhum datasource, identidade real, API ou aplicacao WMS e iniciado neste harness.
Check 'falha-build-literal-preserva-versao-anterior-e-nao-chega-guarda' {
    $isolated=Join-Path $evidence 'build-failure-repository'
    foreach($dir in @('infra/prod','backend/scripts/prod','frontend/tools')){$null=New-Item -ItemType Directory -Path (Join-Path $isolated $dir) -Force}
    foreach($name in @('iniciar-prod.ps1','launcher-common.ps1','producao.json')){Copy-Item -LiteralPath (Join-Path $PSScriptRoot $name) -Destination (Join-Path $isolated ('infra/prod/'+$name))}
    [IO.File]::WriteAllText((Join-Path $isolated 'backend/scripts/prod/identity-prod.ps1'),'function Get-WmsProductionIdentity { [pscustomobject]@{origin="https://wms.rodogarcia.com.br";issuer="https://example.invalid";audience="synthetic-prod"} }; function Assert-WmsProductionIdentity { param($Identity) }',[Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $isolated 'backend/scripts/prod/build-backend-prod.ps1'),'function Build-WmsProductionBackend { param($CandidateDirectory) throw "PROD_BACKEND_BUILD_FAILED" }',[Text.UTF8Encoding]::new($false))
    [IO.File]::WriteAllText((Join-Path $isolated 'frontend/tools/build-prod.mjs'),'throw new Error("HARNESS_FRONTEND_SHOULD_NOT_RUN");',[Text.UTF8Encoding]::new($false))
    $old=Join-Path $isolated 'previous-production.marker';[IO.File]::WriteAllText($old,'SYNTHETIC_PREVIOUS_VERSION');$oldHash=(Get-FileHash $old).Hash
    $child=[Diagnostics.Process]::new();$arguments='-NoProfile -File '+(ConvertTo-WmsProductionArgument (Join-Path $isolated 'infra/prod/iniciar-prod.ps1'))
    $child.StartInfo=New-WmsProductionChildInfo (Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe') $arguments $isolated;$started=$false
    try{$started=$child.Start();$out=$child.StandardOutput.ReadToEndAsync();$err=$child.StandardError.ReadToEndAsync();$child.WaitForExit();[IO.File]::WriteAllText((Join-Path $evidence 'literal-build-failure.log'),$out.Result+$err.Result);Need ($child.ExitCode -eq 30) 'LITERAL_BUILD_FAILURE_EXIT'}finally{Stop-WmsProductionChild $child $started}
    $runs=@(Get-ChildItem -LiteralPath (Join-Path $isolated 'orchestracao/.runtime/launcher-producao/runs') -Directory)
    Need ($runs.Count -eq 1) 'LITERAL_RUN_COUNT'
    $receipt=[IO.File]::ReadAllText((Join-Path $runs[0].FullName 'launcher.json'))|ConvertFrom-Json
    Need ($receipt.codigo -ceq 'PROD_BACKEND_BUILD_FAILED' -and -not $receipt.sqlGuardInvoked -and -not $receipt.backendStarted -and -not $receipt.frontendStarted) 'BUILD_FAILURE_ADVANCED_TO_RUNTIME'
    Need ((Get-FileHash $old).Hash -ceq $oldHash) 'PREVIOUS_VERSION_CHANGED'
}
$failures=@($results | Where-Object estado -eq 'FAIL')
$report=[ordered]@{natureza='PROD_LAUNCH01_ROOT_FOCAL_LOCAL';observadoUtc=[DateTime]::UtcNow.ToString('o');source=@('iniciar-prod.ps1','launcher-common.ps1','producao.json')|ForEach-Object {@{path=('infra/prod/'+$_);sha256=(Get-FileHash (Join-Path $PSScriptRoot $_)).Hash}};total=$results.Count;passed=$results.Count-$failures.Count;failed=$failures.Count;results=$results;sqlExecuted=$false;applicationBackendStarted=$false;productionBusinessApiCalled=$false;syntheticHarness=$true}
[IO.File]::WriteAllText((Join-Path $evidence 'resultados.json'),($report|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$results | Format-Table -AutoSize | Out-String | Write-Output
if($failures.Count){exit 1}
exit 0
