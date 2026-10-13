# Metadados publicos do supervisor; segredos continuam nos loaders PROD existentes.
function Save-WmsPm2Json {
    param([string]$Path, [object]$Value)
    $temporary=$Path+'.'+[guid]::NewGuid().ToString('N')+'.tmp'
    [IO.File]::WriteAllText($temporary,($Value|ConvertTo-Json -Depth 20),[Text.UTF8Encoding]::new($false))
    Move-Item -LiteralPath $temporary -Destination $Path -Force
}

function Get-WmsPm2Release {
    param([string]$Root)
    $selectionPath=Join-Path $Root 'orchestracao/.runtime/launcher-producao/pm2/selected.json'
    $selection=[IO.File]::ReadAllText($selectionPath)|ConvertFrom-Json
    if($selection.runId -cnotmatch '^[a-f0-9]{32}$'){throw 'PROD_PM2_SELECTION_INVALID'}
    $expected=Join-Path $Root ('orchestracao/.runtime/launcher-producao/runs/'+$selection.runId+'/release.json')
    if([IO.Path]::GetFullPath($selection.releasePath) -ine $expected -or
       (Get-FileHash -LiteralPath $expected).Hash -ine $selection.releaseSha256){throw 'PROD_PM2_RELEASE_HASH_INVALID'}
    $release=[IO.File]::ReadAllText($expected)|ConvertFrom-Json
    if($release.natureza -cne 'WMS_PRODUCTION_CANDIDATE' -or $release.runId -cne $selection.runId -or
       $release.database -cne 'WMS_PROD' -or $release.productionMode -cne 'real'){throw 'PROD_PM2_RELEASE_INVALID'}
    $candidate=Join-Path (Split-Path $expected) 'candidate'
    if([IO.Path]::GetFullPath($release.frontend.manifestPath) -ine (Join-Path $candidate 'frontend/dist/build-manifest.json') -or
       [IO.Path]::GetFullPath($release.backend.CandidateDirectory) -ine (Join-Path $candidate 'backend')){throw 'PROD_PM2_CANDIDATE_INVALID'}
    if((Get-FileHash -LiteralPath $release.frontend.manifestPath).Hash -ine $release.frontend.manifestSha256){throw 'PROD_PM2_FRONTEND_HASH_INVALID'}
    if($release.config.path -ine (Join-Path $Root 'infra/prod/producao.json') -or
       (Get-FileHash -LiteralPath $release.config.path).Hash -ine $release.config.sha256){throw 'PROD_PM2_CONFIG_CHANGED'}
    $release
}

function Assert-WmsPm2Api {
    param([string]$Root, [object]$Release)
    $current=[IO.File]::ReadAllText((Join-Path $Root 'orchestracao/.runtime/launcher-producao/pm2/api-current.json'))|ConvertFrom-Json
    if($current.releaseRunId -cne $Release.runId -or $current.estado -cne 'READY'){throw 'PROD_PM2_API_NOT_READY'}
    $receiptPath=[IO.Path]::GetFullPath($current.receiptPath)
    $scope=Join-Path $Root 'orchestracao/.runtime/launcher-producao/pm2/'
    if(-not $receiptPath.StartsWith($scope,[StringComparison]::OrdinalIgnoreCase) -or
       (Get-FileHash -LiteralPath $receiptPath).Hash -ine $current.receiptSha256){throw 'PROD_PM2_API_RECEIPT_INVALID'}
    $b=[IO.File]::ReadAllText($receiptPath)|ConvertFrom-Json
    if($b.estado -cne 'READY_PUBLIC' -or $b.profile -cne 'sqlserver-prod' -or $b.database -cne 'WMS_PROD' -or
       $b.artifact.sha256 -ine $Release.backend.jar.sha256 -or $b.artifact.path -ine $Release.backend.jar.path){throw 'PROD_PM2_API_VERSION_MISMATCH'}
    $p=Get-Process -Id $b.backend.pid -ErrorAction Stop
    $ci=Get-CimInstance Win32_Process -Filter ('ProcessId='+$b.backend.pid)
    if([Math]::Abs(($p.StartTime.ToUniversalTime()-[DateTime]::Parse($b.backend.startUtc).ToUniversalTime()).TotalMilliseconds) -gt 2 -or
       -not $ci.CommandLine.Contains($Release.backend.jar.path) -or -not $ci.CommandLine.Contains('--spring.profiles.active=sqlserver-prod')){throw 'PROD_PM2_API_OWNER_INVALID'}
    $listeners=@(Get-NetTCPConnection -State Listen -LocalPort 25590 -ErrorAction SilentlyContinue)
    if($listeners.Count -ne 1 -or $listeners[0].OwningProcess -ne $b.backend.pid -or $listeners[0].LocalAddress -cne '127.0.0.1'){throw 'PROD_PM2_API_LISTENER_INVALID'}
    $b
}
