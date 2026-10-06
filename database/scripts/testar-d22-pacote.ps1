[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$db=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'));$raiz=Split-Path $db -Parent
$casos=New-Object 'Collections.Generic.List[object]'
function Check([string]$Nome,[bool]$Ok){$casos.Add([pscustomobject]@{caso=$Nome;aprovado=$Ok})}
foreach($p in @('iniciar-bancos.ps1','configurar-credencial.ps1','d22-credencial.ps1','d22-fixture.ps1','d22-console.ps1','testar-d22.ps1','testar-d22-console.ps1','testar-d22-pacote.ps1','congelar-d22.ps1')){
    $tokens=$null;$erros=$null;$null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $p),[ref]$tokens,[ref]$erros)
    Check ('Parser '+$p) (@($erros).Count -eq 0)
}
$root=@(Get-ChildItem -LiteralPath $db -File|ForEach-Object {$_.Name}|Sort-Object)
Check 'Raiz somente README e BATs operador' (($root -join ',') -ceq 'configurar-credencial.bat,iniciar-bancos.bat,README.md')
$bat=[IO.File]::ReadAllText((Join-Path $db 'iniciar-bancos.bat'))
$setup=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'configurar-credencial.ps1'))
$runner=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'iniciar-bancos.ps1'))
$cred=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'd22-credencial.ps1'))
$console=[IO.File]::ReadAllText((Join-Path $PSScriptRoot 'd22-console.ps1'))
Check 'Normal NonInteractive sem pause bypass politica' ($bat.Contains('-NonInteractive') -and $bat -notmatch 'pause|Bypass|ExecutionPolicy')
Check 'Normal nao pergunta senha ou confirma alvo' ($runner -notmatch 'Read-Host|Adaptadores\.Confirmar')
Check 'Setup oculto separado unico prompt' (([regex]::Matches($setup,'Read-Host')).Count -eq 1 -and $setup.Contains('-AsSecureString'))
Check 'Leitor sem plaintext ou ambiente pai' ($cred -notmatch 'GetNetworkCredential|SetEnvironmentVariable|ConvertFrom-SecureString|SecureStringToBSTR')
Check 'Caminho de producao fixo via Windows known folder' ($cred.Contains("GetFolderPath('LocalApplicationData')") -and $cred.Contains('Rodogarcia/WMS/database-runner/credencial-sa.clixml'))
Check 'Console le so metadados e nao commandline' ($console -notmatch 'CommandLine' -and $console.Contains('-Property Name,ProcessId,ParentProcessId'))
Check 'Console /k somente Explorer normal antes runner' ($bat.IndexOf('goto argumento') -lt $bat.IndexOf('d22-console.ps1') -and $bat.Contains('"EXPLORER"') -and $bat.Contains('cmd.exe /d /k call'))
foreach($p in @('README.md','evidencias/d22-relatorio-final.md')){
    $path=Join-Path $db $p;$texto=[IO.File]::ReadAllText($path)
    foreach($m in [regex]::Matches($texto,'\]\(([^)]+)\)')){
        $alvo=$m.Groups[1].Value
        if($alvo -match '^https?://'){continue}
        Check ('Link '+$p+' -> '+$alvo) (Test-Path -LiteralPath (Join-Path (Split-Path $path -Parent) $alvo))
    }
}
$snap=Get-Content -LiteralPath (Join-Path $db 'evidencias/d22-snapshot-d21.json') -Raw -Encoding UTF8|ConvertFrom-Json
Check 'Snapshot D21 73 entradas' ($snap.total -eq 73 -and $snap.copias.Count -eq 73)
foreach($f in $snap.copias){Check ('Copia D21 '+$f.arquivo) ((Get-FileHash -LiteralPath (Join-Path $raiz $f.copia) -Algorithm SHA256).Hash -ceq $f.sha256)}
foreach($f in $snap.copias|Where-Object {$_.arquivo -match '(d21-(flyway|guardas|catalogo)\.ps1$|^database/migrations/.*\.sql$|^database/evidencias/.*manifesto|^database/docs/d21-contrato-flyway\.md$)'}){
    Check ('Origem preservada '+$f.arquivo) ((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -ceq $f.sha256)
}
foreach($p in @('testes','console-fixtures')){
    $r=Get-Content -LiteralPath (Join-Path $db ('evidencias/d22-'+$p+'-final.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    $e=Get-Content -LiteralPath (Join-Path $db ('evidencias/d22-'+$p+'-final-execucao.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    Check ('Fixtures aprovadas '+$p) ($r.aprovados -eq $r.total -and $r.falhas -eq 0 -and $e.exitCode -eq 0 -and $r.sqlExecutado -eq $false)
}
foreach($p in @('offline','outro-cwd','fixture','invalido','extra','setup-offline','setup-invalido')){
    $r=Get-Content -LiteralPath (Join-Path $db ('evidencias/d22-bat-final-'+$p+'.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    Check ('CMD nativo '+$p) ($r.exitCode -eq $r.esperado -and $r.sqlExecutado -eq $false -and $r.credencialRealLida -eq $false -and $r.normalCaminhoRealExecutado -eq $false)
}
$m=Get-Content -LiteralPath (Join-Path $db 'evidencias/d22-provisionamento-metadados-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
Check 'Provisionamento real inspecao so metadados' ($m.conteudoLido -eq $false -and $m.credencialProvisionadaPorAgente -eq $false)
$c=Get-Content -LiteralPath (Join-Path $db 'evidencias/d22-console-forf-caller-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
Check 'FORF real retorna caller sem bloqueio' ($c.exitCode -eq 0 -and $c.resultado -ceq 'CALLER' -and $c.credencialRealLida -eq $false)
$falhas=@($casos|Where-Object {-not $_.aprovado}).Count
[pscustomobject]@{natureza='D22_CHECK_PACOTE_AFETADO_SEM_SQL';total=$casos.Count;aprovados=$casos.Count-$falhas;falhas=$falhas;casos=$casos.ToArray();sqlExecutado=$false;mavenExecutado=$false;credencialRealLida=$false}|ConvertTo-Json -Depth 5
if($falhas){exit 1}
