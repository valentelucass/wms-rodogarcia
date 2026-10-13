# Recibos sinteticos em memoria/arquivo proprio; nenhum banco, segredo ou backend.
[CmdletBinding()]
param([Parameter(Mandatory=$true)][string]$EvidenceDirectory)
$ErrorActionPreference='Stop'
$root=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$area=[IO.Path]::GetFullPath($EvidenceDirectory)
if(-not $area.StartsWith((Join-Path $root 'orchestracao/.runtime/'),[StringComparison]::OrdinalIgnoreCase) -or (Test-Path -LiteralPath $area)){throw 'TEST_NEW_OWN_DIRECTORY_REQUIRED'}
$null=New-Item -ItemType Directory -Path $area
. (Join-Path $root 'backend/scripts/prod/start-backend-prod.ps1')
$contract=Get-Content (Join-Path $PSScriptRoot 'contrato-aplicacao.json') -Encoding UTF8 -Raw | ConvertFrom-Json
$helperHash=(Get-FileHash (Join-Path $PSScriptRoot 'guarda-wmsprod.ps1')).Hash
$now=[DateTimeOffset]::UtcNow.ToString('o')
$baseline=[ordered]@{synthetic=$true;demanda='PROD-LAUNCH01';operacaoSQLImplementada=$true;guardaRealAprovada=$true;estado='GUARDA_ATUAL_APROVADA';pidGuarda=$PID;
 inicioUtc=$now;observadoEm=$now;aberturaTentativas=1;aberturaConcluida=$true;identidadeConfirmada=$true;
 alvo=@{banco='WMS_PROD';login='WMSPROD';usuario='WMSPROD';estado='ONLINE';servidor='SYNTHETIC-NO-SQL'};
 auxiliar=@{arquivo='infra/prod/guarda-wmsprod.ps1';sha256=$helperHash};
 tls=@{encrypt='Mandatory';trustServerCertificate=$false;handshakeConcluido=$true;confirmacaoAtual=$true;certificateSha256=('a'*64);truststoreSha256=('b'*64);
 proveniencia=@{reciboPID=1;reciboInicioUtc=$now;processoLocalVerificadoAntes=$true;processoLocalVerificadoDepois=$true;baselineHistoricaTLSUsadaComoPinAtual=$false}};
 direitos=$contract.rights;catalogo=$contract.catalogo;historico=$contract.historico;
 perfilProtegido=@{host='127.0.0.1';port=1433;database='WMS_PROD';login='WMSPROD';encrypt=$true;trustServerCertificate=$false};checks=@(@{criterio='SYNTHETIC';passou=$true})}
$baselineText=$baseline|ConvertTo-Json -Depth 40
$results=[Collections.Generic.List[object]]::new()
function Case($Name,[scriptblock]$Change,[bool]$Pass) {
    $report=$baselineText|ConvertFrom-Json
    & $Change $report
    $path=Join-Path $area ($Name+'.json')
    [IO.File]::WriteAllText($path,($report|ConvertTo-Json -Depth 40),[Text.UTF8Encoding]::new($false))
    $accepted=$false
    try {$null=Assert-WmsProductionGuard $path (Get-FileHash $path).Hash $PID $helperHash $area;$accepted=$true}catch{}
    $results.Add([pscustomobject]@{name=$Name;passed=($accepted -eq $Pass)})
}
Case 'catalogo-com-zero-falhas-aceito' {} $true
Case 'recibo-dev-recusado' {param($r) $r.alvo.banco='WMS_DEV'} $false
Case 'login-admin-recusado' {param($r) $r.alvo.login='sa'} $false
Case 'sem-abertura-real' {param($r) $r.aberturaTentativas=0} $false
Case 'segunda-abertura-recusada' {param($r) $r.aberturaTentativas=2} $false
Case 'recibo-antigo' {param($r) $r.inicioUtc=[DateTimeOffset]::UtcNow.AddHours(-1).ToString('o')} $false
Case 'tls-bypass-recusado' {param($r) $r.tls.trustServerCertificate=$true} $false
Case 'tls-sem-processo-atual' {param($r) $r.tls.proveniencia.processoLocalVerificadoDepois=$false} $false
Case 'direitos-incompletos' {param($r) $r.direitos.columns=$null} $false
Case 'ddl-role-recusada' {param($r) $r.direitos.roles=@(@{name='db_owner';role='db_owner'})} $false
Case 'integridade-check-recusada' {param($r) $r.catalogo.checksInvalidos=1} $false
Case 'integridade-fk-recusada' {param($r) $r.catalogo.fksInvalidas=1} $false
Case 'indice-desabilitado-recusado' {param($r) $r.catalogo.indicesDesabilitados=1} $false
Case 'migration-falhou' {param($r) $r.historico[0].success=$false} $false
Case 'check-falhou' {param($r) $r.checks[0].passou=$false} $false
Case 'pid-divergente' {param($r) $r.pidGuarda=0} $false
$result=@{sqlExecuted=$false;synthetic=$true;total=$results.Count;failed=@($results|Where-Object {-not $_.passed}).Count;checks=$results}
[IO.File]::WriteAllText((Join-Path $area 'resultado.json'),($result|ConvertTo-Json -Depth 8),[Text.UTF8Encoding]::new($false))
$results|Format-Table -AutoSize|Out-String|Write-Output
if($result.failed){exit 1}
