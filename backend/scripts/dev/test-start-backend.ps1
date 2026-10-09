# Focais isolados de validacao. Nenhum Start, SQL, DPAPI, HTTP ou Java.
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'start-backend.ps1')
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../../..'))
$directory = Join-Path $root ('backend/evidencias/dev02-cedro-focais/' + [Guid]::NewGuid().ToString('N'))
$null = New-Item -ItemType Directory -Path $directory
$helperSha = (Get-FileHash -LiteralPath (Join-Path $root 'infra/dev02/guarda-wmsdev.ps1')).Hash
$cases = New-Object 'Collections.Generic.List[object]'
function Case([string]$Name, [string]$Expected, [scriptblock]$Action) {
    $actual = 'ACCEPT_FIXTURE_STRUCTURE_ONLY'
    try { $null = & $Action } catch { $actual = $_.Exception.Message }
    $cases.Add([pscustomobject]@{name=$Name;expected=$Expected;actual=$actual;passed=($actual -ceq $Expected)})
}
function Fixture {
    $now = [DateTime]::UtcNow.ToString('o')
    # PASS abaixo e APENAS estrutura ficticia para Assert; nunca passada a Start.
    @{guardaRealAprovada=$true;estado='GUARDA_ATUAL_APROVADA';inicioUtc=$now;observadoEm=$now;pidGuarda=12345
      auxiliar=@{arquivo='infra/dev02/guarda-wmsdev.ps1';sha256=$helperSha}
      aberturaTentativas=1;aberturaConcluida=$true;identidadeConfirmada=$true
      alvo=@{banco='WMS_DEV';login='WMSDEV';usuario='WMSDEV';estado='ONLINE';servidor='FICT-STRUCTURE-ONLY'}
      tls=@{encrypt='Mandatory';trustServerCertificate=$false;handshakeConcluido=$true;confirmacaoAtual=$true;certificateSha256=('a'*64);truststoreSha256=('b'*64)}
      direitos=@{server=@();database=@();roles=@();serverRoles=@();ownership=@();schema=@();explicit=@();objects=@();columns=@()}
      catalogo=@{tabelas=@();colunas=@();checksInvalidos=@();fksInvalidas=@();indicesDesabilitados=@()}
      historico=@(@{success=$true;version='FICT'});checks=@(@{criterio='fixture structure only';passou=$true})
      perfilProtegido=@{host='127.0.0.1';port=1433;database='WMS_DEV';login='WMSDEV';encrypt=$true;trustServerCertificate=$false}}
}
function Verify($Fixture, [int]$ExpectedPid=12345, [string]$ExpectedHelper=$helperSha) {
    $path = Join-Path $directory ([Guid]::NewGuid().ToString('N')+'.json')
    [IO.File]::WriteAllText($path, ($Fixture | ConvertTo-Json -Depth 8), [Text.UTF8Encoding]::new($false))
    Assert-WmsFrontendDevGuard $path (Get-FileHash -LiteralPath $path).Hash $ExpectedPid $ExpectedHelper
}
Case 'AUTH absent' 'DEV02_AUTH_SOURCE_MISSING' { Assert-WmsFrontendDevIdentity $null }
Case 'AUTH requires HTTPS' 'DEV02_AUTH_SOURCE_MISSING_OR_INVALID' { Assert-WmsFrontendDevIdentity @{issuer='http://identity.invalid';jwkSetUri='https://identity.invalid/jwks';audience='FICT'} }
Case 'AUTH audience absent' 'DEV02_AUTH_AUDIENCE_MISSING_OR_INVALID' { Assert-WmsFrontendDevIdentity @{issuer='https://identity.invalid';jwkSetUri='https://identity.invalid/jwks';audience=''} }
Case 'Original actual guard BLOCKED' 'DEV02_GUARD_BLOCKED' {
    $path = Join-Path $root 'frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json'
    Assert-WmsFrontendDevGuard $path (Get-FileHash -LiteralPath $path).Hash 32144 'A3DE06DD32CF233ECBFD0B12714A4E3D0AF43A357209EBA0D85334DD5E69EACC'
}
Case 'Receipt hash mismatch' 'DEV02_GUARD_RECEIPT_OR_HASH_INVALID' {
    $path = Join-Path $root 'frontend/evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json'
    Assert-WmsFrontendDevGuard $path ('0'*64) 32144 $helperSha
}
Case 'Boolean string is not proof' 'DEV02_GUARD_SCHEMA_INVALID' { $f=Fixture;$f.guardaRealAprovada='true';Verify $f }
Case 'Old boolean only is not proof' 'DEV02_GUARD_SCHEMA_INVALID' { Verify @{backendPodeIniciarComSQL=$true} }
Case 'Wrong process' 'DEV02_GUARD_PROCESS_MISMATCH' { Verify (Fixture) 54321 }
Case 'Wrong helper bytes' 'DEV02_GUARD_HELPER_HASH_MISMATCH' { Verify (Fixture) 12345 ('0'*64) }
Case 'Stale time' 'DEV02_GUARD_TIME_INVALID_OR_STALE' { $f=Fixture;$f.inicioUtc=[DateTime]::UtcNow.AddMinutes(-10).ToString('o');Verify $f }
Case 'Future time' 'DEV02_GUARD_TIME_INVALID_OR_STALE' { $f=Fixture;$f.observadoEm=[DateTime]::UtcNow.AddMinutes(10).ToString('o');Verify $f }
Case 'Other database' 'DEV02_GUARD_IDENTITY_INVALID' { $f=Fixture;$f.alvo.banco='FICT_OTHER';Verify $f }
Case 'TLS bypass' 'DEV02_GUARD_TLS_INVALID' { $f=Fixture;$f.tls.trustServerCertificate=$true;Verify $f }
Case 'Rights unknown' 'DEV02_GUARD_RIGHTS_INCOMPLETE' { $f=Fixture;$f.direitos=$null;Verify $f }
Case 'Catalog unknown' 'DEV02_GUARD_CATALOG_INCOMPLETE' { $f=Fixture;$f.catalogo=$null;Verify $f }
Case 'Failed history' 'DEV02_GUARD_HISTORY_OR_PROFILE_INVALID' { $f=Fixture;$f.historico[0].success=$false;Verify $f }
Case 'Failed check' 'DEV02_GUARD_CHECKS_INCOMPLETE' { $f=Fixture;$f.checks[0].passou=$false;Verify $f }
Case 'Consistent fixture structure only' 'ACCEPT_FIXTURE_STRUCTURE_ONLY' {
    $result=Verify (Fixture)
    if ($result.guard.processId -ne 12345 -or $result.guard.sha256.Length -ne 64 -or
        $result.guard.finishedUtc -eq $null -or $result.guard.ContainsKey('proof')) { throw 'WRONG_OUTPUT_CONTRACT' }
}
Case 'Foreign process handle refused' 'DEV02_FOREIGN_PROCESS_HANDLE_REFUSED' { Stop-WmsFrontendDevBackend @{RunId='foreign';Process=$null} }
Case 'UTC Z exact original literal survives parsing' 'ACCEPT_FIXTURE_STRUCTURE_ONLY' {
    $f=Fixture
    $original=[DateTime]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffffff'Z'",[Globalization.CultureInfo]::InvariantCulture)
    $f.inicioUtc=$original;$f.observadoEm=$original
    $result=Verify $f
    if ($result.guard.finishedUtc -cne $original -or $result.guard.finishedUtc -cnotmatch '\.\d{7}Z$') {
        throw 'UTC_Z_LITERAL_CHANGED'
    }
    # A forma normalizada representa o mesmo instante, mas nao e o contrato literal.
    if ([DateTimeOffset]::Parse($original).ToString('o') -ceq $original) { throw 'UTC_OFFSET_REGRESSION_NOT_DISTINGUISHED' }
}
$result = [pscustomobject]@{natureza='D31_DEV02_ISOLATED_GUARD_VALIDATION_NO_START';observadoUtc=[DateTime]::UtcNow.ToString('o')
    total=$cases.Count;passed=@($cases | Where-Object passed).Count;failed=@($cases | Where-Object {-not $_.passed}).Count
    cases=$cases.ToArray();directory=$directory;backendStarted=$false;dpapiImported=$false;sqlOpened=$false;httpCalled=$false;javaExecuted=$false}
$json = $result | ConvertTo-Json -Depth 6
[IO.File]::WriteAllText((Join-Path $directory 'checks.json'), $json, [Text.UTF8Encoding]::new($false))
Write-Output $json
if ($result.failed) { exit 1 }
