# Selecao de release sintetica: nenhum banco, credencial ou processo de aplicacao.
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'pm2-common.ps1')
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$fixture=Join-Path $root ('orchestracao/.runtime/pm2-selection-tests/'+[guid]::NewGuid().ToString('N'))
$runId='0123456789abcdef0123456789abcdef'
$run=Join-Path $fixture ('orchestracao/.runtime/launcher-producao/runs/'+$runId)
$area=Join-Path $fixture 'orchestracao/.runtime/launcher-producao/pm2'
$manifest=Join-Path $run 'candidate/frontend/dist/build-manifest.json'
$config=Join-Path $fixture 'infra/prod/producao.json'
foreach($dir in @($area,(Split-Path $manifest),(Split-Path $config))){$null=New-Item -ItemType Directory -Path $dir -Force}
[IO.File]::WriteAllText($manifest,'SYNTHETIC_FRONTEND_MANIFEST')
[IO.File]::WriteAllText($config,'SYNTHETIC_CONFIG')
$release=@{natureza='WMS_PRODUCTION_CANDIDATE';runId=$runId;database='WMS_PROD';productionMode='real';
frontend=@{manifestPath=$manifest;manifestSha256=(Get-FileHash $manifest).Hash};
backend=@{CandidateDirectory=(Join-Path $run 'candidate/backend')};config=@{path=$config;sha256=(Get-FileHash $config).Hash}}
$releasePath=Join-Path $run 'release.json'
function SelectRelease {
    Save-WmsPm2Json $releasePath $release
    Save-WmsPm2Json (Join-Path $area 'selected.json') @{runId=$runId;releasePath=$releasePath;releaseSha256=(Get-FileHash $releasePath).Hash}
}
function Refuses([string]$Name) {
    $failed=$false
    try {$null=Get-WmsPm2Release $fixture}catch{$failed=$true}
    if(-not $failed){throw ('EXPECTED_REFUSAL_'+$Name)}
    Write-Output ('PASS '+$Name)
}
SelectRelease
$r=Get-WmsPm2Release $fixture
if($r.runId -ne $runId){throw 'RELEASE_NOT_SELECTED'}
Write-Output 'PASS selected-exact-release'
[IO.File]::AppendAllText($releasePath,' ')
Refuses 'changed-release-bytes'
SelectRelease
[IO.File]::AppendAllText($manifest,'CHANGED')
Refuses 'changed-frontend'
[IO.File]::WriteAllText($manifest,'SYNTHETIC_FRONTEND_MANIFEST')
$release.database='WMS_DEV';SelectRelease
Refuses 'wrong-database'
$release.database='WMS_PROD';$release.frontend.manifestPath=$config;SelectRelease
Refuses 'manifest-outside-candidate'
$release.frontend.manifestPath=$manifest;SelectRelease
[IO.File]::AppendAllText($config,'CHANGED')
Refuses 'changed-production-config'
