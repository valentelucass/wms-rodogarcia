# Somente fixtures locais isoladas. Nao abre SQL nem inicia backend/aplicacao.
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidenceDirectory)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$evidence=[IO.Path]::GetFullPath($EvidenceDirectory)
if (-not $evidence.StartsWith((Join-Path $root 'orchestracao/.runtime/') , [StringComparison]::OrdinalIgnoreCase) -or (Test-Path -LiteralPath $evidence)) { throw 'PROD_TEST_NEW_OWN_PATH_REQUIRED' }
$null=New-Item -ItemType Directory -Path $evidence
. (Join-Path $root 'backend/scripts/prod/setup-prod.ps1')
. (Join-Path $root 'backend/scripts/prod/credential-prod.ps1')
. (Join-Path $root 'backend/scripts/prod/start-backend-prod.ps1')
. (Join-Path $root 'backend/scripts/prod/bootstrap-prod.ps1')
$script:testAuth=Join-Path $evidence 'auth-ficticia'
$script:testCredential=Join-Path $evidence 'sql-ficticio/credential.clixml'
function Get-WmsProductionAuthDirectory { $script:testAuth }
function Get-WmsProductionCredentialPath { $script:testCredential }
function Get-WmsProductionBootstrapPath { Join-Path $script:testAuth 'bootstrap.clixml' }
$results=[Collections.Generic.List[object]]::new()
function Check($Name,[scriptblock]$Body) {
    try { & $Body; $results.Add([pscustomobject]@{name=$Name;passed=$true}) }
    catch { $results.Add([pscustomobject]@{name=$Name;passed=$false;code=if($_.Exception.Message -match '^[A-Z0-9_]+$'){$_.Exception.Message}else{'LOCAL_TEST_FAILED'}}) }
}
function Need([bool]$Ok) { if(-not $Ok){throw 'ASSERTION_FAILED'} }
function Reject([scriptblock]$Body,[string]$Code) {
    $caught=$null
    try { $null=& $Body } catch { $caught=$_.Exception.Message }
    Need ($caught -ceq $Code)
}
Check 'chave-propria-DPAPI-ACL-metadata-sem-segredo' {
    $script:testIdentity=Initialize-WmsProductionIdentity
    Need ($testIdentity.environment -ceq 'PROD' -and $testIdentity.database -ceq 'WMS_PROD')
    Need (-not ([IO.File]::ReadAllText($testIdentity.metadataPath) -match 'privateKey|bootstrapHash|password'))
    Assert-WmsProductionPrivatePath $testIdentity.materialPath
    $material=Import-Clixml -LiteralPath $testIdentity.materialPath
    try { Need ($material -is [Security.SecureString]) } finally { $material.Dispose() }
    $script:keyHash=(Get-FileHash -LiteralPath $testIdentity.materialPath).Hash
}
Check 'nova-execucao-preserva-chave' {
    $null=Initialize-WmsProductionIdentity
    Need ((Get-FileHash -LiteralPath $testIdentity.materialPath).Hash -ceq $keyHash)
}
Check 'identidade-dev-recusada' {
    $copy=$testIdentity | ConvertTo-Json | ConvertFrom-Json
    $copy.environment='DEV'
    Reject { Assert-WmsProductionIdentity $copy } 'PROD_AUTH_PUBLIC_METADATA_INVALID'
}
Check 'metadata-incompleta-nao-substitui-chave' {
    $text=[IO.File]::ReadAllText($testIdentity.metadataPath)
    try {
        [IO.File]::WriteAllText($testIdentity.metadataPath,'{}')
        $caught=$false; try {$null=Initialize-WmsProductionIdentity}catch{$caught=$true}
        Need $caught
        Need ((Get-FileHash -LiteralPath $testIdentity.materialPath).Hash -ceq $keyHash)
    } finally { [IO.File]::WriteAllText($testIdentity.metadataPath,$text,[Text.UTF8Encoding]::new($false)) }
}
Check 'ausencia-sql-recusa-antes-de-conexao' {
    Reject {Get-WmsProductionCredentialMetadata} 'PROD_PROTECTED_APPLICATION_CHANNEL_MISSING'
}
Check 'bootstrap-ausente-nao-cria-material' {
    Need ($null -eq (Get-WmsProductionBootstrapHash))
}
Check 'bootstrap-proprio-DPAPI-readonly' {
    $data=@{schema=1;environment='PROD';database='WMS_PROD';purpose='INITIAL_PRINCIPAL_ONLY';hash=('{pbkdf2-600k}'+('a'*96))}
    $secret=ConvertTo-SecureString ($data|ConvertTo-Json) -AsPlainText -Force
    try{$secret|Export-Clixml -LiteralPath (Get-WmsProductionBootstrapPath);Protect-WmsProductionNewFile (Get-WmsProductionBootstrapPath)}finally{$secret.Dispose()}
    $read=Get-WmsProductionBootstrapHash
    try{Need ($read -is [Security.SecureString] -and $read.IsReadOnly())}finally{$read.Dispose()}
}
Check 'bootstrap-DEV-recusado' {
    $path=Get-WmsProductionBootstrapPath;$original=[IO.File]::ReadAllBytes($path)
    $data=@{schema=1;environment='DEV';database='WMS_DEV';purpose='INITIAL_PRINCIPAL_ONLY';hash=('{pbkdf2-600k}'+('a'*96))}
    $secret=ConvertTo-SecureString ($data|ConvertTo-Json) -AsPlainText -Force
    try{$secret|Export-Clixml -LiteralPath $path;Reject {Get-WmsProductionBootstrapHash} 'PROD_BOOTSTRAP_PURPOSE_INVALID'}finally{$secret.Dispose();[IO.File]::WriteAllBytes($path,$original)}
}
Check 'bootstrap-fora-finalidade-recusado' {
    $path=Get-WmsProductionBootstrapPath;$original=[IO.File]::ReadAllBytes($path)
    $data=@{schema=1;environment='PROD';database='WMS_PROD';purpose='RESET_PRINCIPAL';hash=('{pbkdf2-600k}'+('a'*96))}
    $secret=ConvertTo-SecureString ($data|ConvertTo-Json) -AsPlainText -Force
    try{$secret|Export-Clixml -LiteralPath $path;Reject {Get-WmsProductionBootstrapHash} 'PROD_BOOTSTRAP_PURPOSE_INVALID'}finally{$secret.Dispose();[IO.File]::WriteAllBytes($path,$original)}
}
Check 'credencial-ficticia-protegida-roundtrip' {
    $secret=ConvertTo-SecureString ('FICTICIO-SEM-SQL-'+('x'*32)) -AsPlainText -Force
    try { $null=Save-WmsProductionApplicationCredential $secret } finally {$secret.Dispose()}
    $script:sqlMeta=Get-WmsProductionCredentialMetadata
    $read=Get-WmsProductionApplicationCredential -ExpectedSha256 $sqlMeta.sha256
    try {Need ($read.UserName -ceq 'WMSPROD' -and $read.Password.IsReadOnly())}finally{$read.Password.Dispose()}
}
Check 'recusa-sobrescrever-credencial' {
    $secret=ConvertTo-SecureString ('FICTICIO-'+('y'*40)) -AsPlainText -Force
    try {Reject {Save-WmsProductionApplicationCredential $secret} 'PROD_CREDENTIAL_EXISTING_PRESERVED'}finally{$secret.Dispose()}
}
Check 'credencial-mudou-desde-guarda' {
    Reject {Get-WmsProductionApplicationCredential -ExpectedSha256 ('0'*64)} 'PROD_CREDENTIAL_CHANGED_SINCE_GUARD'
}
Check 'credencial-dev-nao-e-aceita-em-prod' {
    $original=[IO.File]::ReadAllBytes($testCredential)
    $secret=ConvertTo-SecureString ('FICTICIO-'+('z'*40)) -AsPlainText -Force
    try {
        (New-Object Management.Automation.PSCredential('WMSDEV',$secret)) | Export-Clixml -LiteralPath $testCredential
        Reject {Get-WmsProductionApplicationCredential} 'PROD_CREDENTIAL_IDENTITY_OR_TYPE_INVALID'
    } finally { $secret.Dispose();[IO.File]::WriteAllBytes($testCredential,$original) }
}
Check 'ACL-ampla-recusa-credencial' {
    $original=[IO.File]::GetAccessControl($testCredential,[Security.AccessControl.AccessControlSections]::Access)
    try {
        $changed=[IO.File]::GetAccessControl($testCredential,[Security.AccessControl.AccessControlSections]::Access)
        $changed.AddAccessRule((New-Object Security.AccessControl.FileSystemAccessRule((New-Object Security.Principal.SecurityIdentifier('S-1-1-0')),'Read','Allow')))
        [IO.File]::SetAccessControl($testCredential,$changed)
        Reject {Get-WmsProductionCredentialMetadata} 'PROD_PROTECTED_ACL_TOO_BROAD'
    } finally {[IO.File]::SetAccessControl($testCredential,$original)}
}
$report=[ordered]@{natureza='TESTE_LOCAL_CANAIS_FICTICIOS';sqlExecuted=$false;applicationStarted=$false;total=$results.Count;failed=@($results|Where-Object {-not $_.passed}).Count;checks=$results}
[IO.File]::WriteAllText((Join-Path $evidence 'resultado.json'),($report|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$results|Format-Table -AutoSize|Out-String|Write-Output
if($report.failed){exit 1}
