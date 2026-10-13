[CmdletBinding()]
param([Parameter(Mandatory=$true)][ValidateSet('api','ui')][string]$Component,
      [Parameter(Mandatory=$true)][string]$ControlDirectory,
      [Parameter(Mandatory=$true)][int]$WrapperPid)
Set-StrictMode -Version Latest
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=Join-Path $root 'orchestracao/.runtime/launcher-producao/pm2'
. (Join-Path $PSScriptRoot 'launcher-common.ps1')
. (Join-Path $PSScriptRoot 'pm2-common.ps1')
. (Join-Path $root 'backend/scripts/prod/start-backend-prod.ps1')
$backend=$null;$frontend=$null;$frontendStarted=$false;$credential=$null;$bootstrap=$null;$parent=$null
$ready=$false;$exitCode=20
function Stopping { (Test-Path -LiteralPath (Join-Path $ControlDirectory 'stop.request')) -or $parent.HasExited }
try {
    $ControlDirectory=[IO.Path]::GetFullPath($ControlDirectory)
    if(-not $ControlDirectory.StartsWith($area+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'PROD_PM2_CONTROL_PATH_INVALID'}
    Assert-WmsProductionOwnedDirectory $ControlDirectory
    $parent=Get-Process -Id $WrapperPid; $null=$parent.Handle
    $self=Get-CimInstance Win32_Process -Filter ('ProcessId='+$PID)
    if($self.ParentProcessId -ne $WrapperPid){throw 'PROD_PM2_PARENT_INVALID'}
    $release=Get-WmsPm2Release $root
    $config=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'producao.json'))|ConvertFrom-Json
    Assert-WmsProductionPublicConfig $config
    Assert-WmsProductionArtifact $release.backend
    if(Stopping){$exitCode=0;return}
    if($Component -eq 'api') {
        Assert-WmsProductionFreePort $config.backendPort
        . (Join-Path $root 'backend/scripts/prod/setup-prod.ps1')
        . (Join-Path $root 'backend/scripts/prod/credential-prod.ps1')
        . (Join-Path $root 'backend/scripts/prod/bootstrap-prod.ps1')
        $identity=Initialize-WmsProductionIdentity
        Assert-WmsProductionIdentity $identity
        . (Join-Path $PSScriptRoot 'guarda-wmsprod.ps1')
        $guard=Invoke-WmsProdGuard
        $guardPath=Join-Path $ControlDirectory 'guarda.json'
        Save-WmsPm2Json $guardPath $guard
        if(-not $guard.guardaRealAprovada){throw 'PROD_CURRENT_GUARD_BLOCKED'}
        if(Stopping){$exitCode=0;return}
        $credential=Get-WmsProductionApplicationCredential -ExpectedSha256 $guard.canal.sha256
        $bootstrap=Get-WmsProductionBootstrapHash
        $arguments=@{Port=$config.backendPort;RunDirectory=$ControlDirectory;GuardReceipt=$guardPath;
            GuardSha256=(Get-FileHash $guardPath).Hash;GuardProcessId=$PID;
            GuardHelperSha256=(Get-FileHash (Join-Path $PSScriptRoot 'guarda-wmsprod.ps1')).Hash;
            Artifact=$release.backend;Identity=$identity;DatabaseCredential=$credential}
        if($bootstrap){$arguments.BootstrapHash=$bootstrap}
        $backend=Start-WmsProductionBackend @arguments
        $credential=$null
        if(Stopping){$exitCode=0;return}
        $current=@{estado='READY';releaseRunId=$release.runId;receiptPath=$backend.ReceiptPath;receiptSha256=$backend.ReceiptSha256;
            workerPid=$PID;wrapperPid=$WrapperPid;controlDirectory=$ControlDirectory;observadoUtc=[DateTime]::UtcNow.ToString('o')}
        Save-WmsPm2Json (Join-Path $area 'api-current.json') $current
        Save-WmsPm2Json (Join-Path $ControlDirectory 'ready.json') $current
    } else {
        Assert-WmsProductionFreePort $config.frontendPort
        # Espera apenas o supervisor API desta versao; nao abre SQL nem cria outro backend.
        $deadline=[DateTime]::UtcNow.AddSeconds(180);$api=$null
        while([DateTime]::UtcNow -lt $deadline -and -not (Stopping)) {
            try {$api=Assert-WmsPm2Api $root $release;break}catch{Start-Sleep -Milliseconds 500}
        }
        if(Stopping){$exitCode=0;return}
        if(-not $api){throw 'PROD_PM2_API_WAIT_TIMEOUT'}
        $receiptPath=Join-Path $ControlDirectory 'launcher.json'
        $server=Join-Path $root 'frontend/tools/prod-server.mjs'
        $node=(Get-Command node.exe).Source
        $arguments=(ConvertTo-WmsProductionArgument $server)+' --dist '+(ConvertTo-WmsProductionArgument (Split-Path $release.frontend.manifestPath))+
            ' --manifest '+(ConvertTo-WmsProductionArgument $release.frontend.manifestPath)+' --port '+$config.frontendPort+
            ' --backend http://127.0.0.1:'+$config.backendPort+' --public-origin '+$config.frontendOrigin+
            ' --activation-receipt '+(ConvertTo-WmsProductionArgument $receiptPath)
        $frontend=[Diagnostics.Process]::new();$frontend.StartInfo=New-WmsProductionChildInfo $node $arguments $root
        $frontend.StartInfo.EnvironmentVariables['NODE_ENV']='production'
        if(-not $frontend.Start()){throw 'PROD_FRONTEND_NOT_STARTED'}
        $frontendStarted=$true
        $out=$frontend.StandardOutput.ReadToEndAsync();$err=$frontend.StandardError.ReadToEndAsync()
        $deadline=[DateTime]::UtcNow.AddSeconds(30);$available=$false
        while([DateTime]::UtcNow -lt $deadline -and -not (Stopping)) {
            if($frontend.HasExited){throw 'PROD_FRONTEND_EXITED_BEFORE_READY'}
            $listener=@(Get-NetTCPConnection -State Listen -LocalPort $config.frontendPort -ErrorAction SilentlyContinue)
            if($listener.Count -eq 1 -and $listener[0].OwningProcess -eq $frontend.Id -and $listener[0].LocalAddress -eq '127.0.0.1') {
                try {
                    $status=Invoke-RestMethod -TimeoutSec 3 -Uri ('http://127.0.0.1:'+$config.frontendPort+'/api/v1/status')
                    $health=Invoke-RestMethod -TimeoutSec 3 -Uri ('http://127.0.0.1:'+$config.frontendPort+'/__wms/health')
                    if($status.status -ceq 'DISPONIVEL' -and $status.aplicacao -ceq 'wms-rodogarcia' -and
                       $health.runId -ceq $release.runId -and $health.pid -eq $frontend.Id -and
                       $health.manifestSha256 -ieq $release.frontend.manifestSha256){$available=$true;break}
                }catch{}
            }
            Start-Sleep -Milliseconds 300
        }
        if(Stopping){$exitCode=0;return}
        if(-not $available){throw 'PROD_FRONTEND_READINESS_FAILED'}
        $null=Assert-WmsPm2Api $root $release
        $activationPath=Join-Path $area ('activation-'+$release.runId+'.json')
        $activatedUtc=[DateTime]::UtcNow.ToString('o')
        if(Test-Path -LiteralPath $activationPath){
            $activation=[IO.File]::ReadAllText($activationPath)|ConvertFrom-Json
            if($activation.runId -cne $release.runId -or $activation.manifestSha256 -ine $release.frontend.manifestSha256){throw 'PROD_PM2_ACTIVATION_INVALID'}
            $activatedUtc=$activation.activatedUtc
        }
        $receipt=@{natureza='WMS_PRODUCTION_LAUNCHER';runId=$release.runId;projectPath=$root;supervisor='PM2';
            estado='OPERATIONAL_PUBLIC_READINESS_HTTPS_LOGIN_PENDING';operationalReadiness=$true;activatedUtc=$activatedUtc;
            frontend=@{pid=$frontend.Id;startUtc=$frontend.StartTime.ToUniversalTime().ToString('o');port=$config.frontendPort};
            build=@{frontend=$release.frontend};backend=$api.backend;workerPid=$PID;wrapperPid=$WrapperPid}
        Save-WmsPm2Json $receiptPath $receipt
        Save-WmsPm2Json $activationPath @{runId=$release.runId;manifestSha256=$release.frontend.manifestSha256;activatedUtc=$activatedUtc}
        $current=@{estado='READY';releaseRunId=$release.runId;receiptPath=$receiptPath;receiptSha256=(Get-FileHash $receiptPath).Hash;
            workerPid=$PID;wrapperPid=$WrapperPid;controlDirectory=$ControlDirectory;observadoUtc=[DateTime]::UtcNow.ToString('o')}
        Save-WmsPm2Json (Join-Path $area 'ui-current.json') $current
        Save-WmsPm2Json (Join-Path $ControlDirectory 'ready.json') $current
    }
    $ready=$true
    Write-Output 'WMS_PM2_READY'
    while(-not (Stopping)) {
        if(($backend -and $backend.Process.HasExited) -or ($frontend -and $frontend.HasExited)){throw 'PROD_PM2_OWN_PROCESS_EXITED'}
        Start-Sleep -Milliseconds 500
    }
    $exitCode=0
} catch {
    $code=[string]$_.Exception.Message
    if($code -cnotmatch '^PROD_[A-Z0-9_]+$'){$code='PROD_PM2_START_FAILED'}
    Write-Output ('WMS_PM2_BLOCKED '+$code)
    $exitCode=if($ready){1}else{20}
} finally {
    if($bootstrap){$bootstrap.Dispose()}
    if($credential){$credential.Password.Dispose()}
    try {Stop-WmsProductionChild $frontend $frontendStarted}
    finally {if($backend){Stop-WmsProductionBackend $backend};if($parent){$parent.Dispose()}}
}
exit $exitCode
