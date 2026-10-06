[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$db=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$raiz=Split-Path $db -Parent
. (Join-Path $PSScriptRoot 'd21-flyway.ps1')
$checks=New-Object 'Collections.Generic.List[object]'
function Check([string]$Nome,[bool]$Ok){$checks.Add([pscustomobject]@{caso=$Nome;aprovado=$Ok})}
foreach($f in Get-ChildItem -LiteralPath (Join-Path $db 'scripts') -File -Filter '*.ps1' -Recurse){
    $t=$null;$e=$null;$null=[Management.Automation.Language.Parser]::ParseFile($f.FullName,[ref]$t,[ref]$e)
    Check ('sintaxe '+$f.Name) (@($e).Count -eq 0)
}
Check 'raiz README/BAT somente' ((@(Get-ChildItem -LiteralPath $db -File).Name|Sort-Object) -join ',' -ceq 'iniciar-bancos.bat,README.md')
$bat=[IO.File]::ReadAllText((Join-Path $db 'iniciar-bancos.bat'))
Check 'BAT relativo allowlist TLS politica pausa sem bypass' ($bat.Contains('%~dp0scripts\iniciar-bancos.ps1') -and $bat.Contains('TLS com validacao obrigatoria') -and $bat.Contains('Flyway migrations DEV e PROD') -and $bat -match 'pause' -and $bat -notmatch 'ExecutionPolicy|Bypass|install|TLS validado')
$off=& (Join-Path $db 'scripts/iniciar-bancos.ps1') -Offline|ConvertFrom-Json
Check 'D21 offline sem SQL senha' ($off.natureza -ceq 'D21_LAUNCHER_OFFLINE_SEM_SEGREDO_SEM_SQL' -and $off.senhaLida -eq $false -and $off.conexoes -eq 0 -and $off.ddl -eq 0)
try{Assert-WmsD21Pom (Join-Path $raiz 'backend/pom.xml');$ok=$true}catch{$ok=$false}
Check 'POM contrato Cedro sem Maven' $ok
$stable=Get-Content -LiteralPath (Join-Path $db 'evidencias/d21-contrato-estavel.json') -Raw -Encoding UTF8|ConvertFrom-Json
Check 'guardas estaveis Cedro' ((Get-FileHash -LiteralPath (Join-Path $raiz $stable.guardas) -Algorithm SHA256).Hash -ceq $stable.guardasSha256)
Check 'contrato estavel Cedro' ((Get-FileHash -LiteralPath (Join-Path $raiz $stable.contrato) -Algorithm SHA256).Hash -ceq $stable.contratoSha256)
$src=@(Get-WmsD21Fontes $db)
Check 'fontes frozen presentes mais futuras permitidas' ($src.Count -ge 9)
$snap=Get-Content -LiteralPath (Join-Path $db 'evidencias/d21-snapshot-d20.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($f in $snap.copias){
    Check ('snapshot '+$f.arquivo) ((Get-FileHash -LiteralPath (Join-Path $raiz $f.copia) -Algorithm SHA256).Hash -ceq $f.sha256)
    if($f.arquivo -match '^database/evidencias/.*manifesto'){Check ('manifesto D20 intacto '+$f.arquivo) ((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -ceq $f.sha256)}
}
$d20=Get-Content -LiteralPath (Join-Path $db 'evidencias/d20-manifesto-corrente-reorganizacao.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($f in $d20.arquivos|Where-Object {$_.arquivo -match '^database/evidencias/'}){
    Check ('evidencia D20 intacta '+$f.arquivo) ((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -ceq $f.sha256)
}
$sql=[IO.File]::ReadAllText((Join-Path $db 'validacao/d21-schema-real.sql')) -replace '(?m)--.*$',''
Check 'catalogo somente SELECT metadata sem DDL/DML' ($sql -notmatch '\b(CREATE|ALTER|DROP|INSERT|UPDATE|DELETE|GRANT|REPAIR|BASELINE|CLEAN)\b')
$runner=[IO.File]::ReadAllText((Join-Path $db 'scripts/d21-flyway.ps1'))
Check 'segredo somente childenv sem pai/setenv arquivo' ($runner -notmatch 'SetEnvironmentVariable|Set-Content|WriteAllText' -and $runner.Contains("EnvironmentVariables['WMS_DB_BOOTSTRAP_PASSWORD']") -and $runner.Contains('ZeroFreeBSTR'))
Check 'saida bruta nao retornada' ($runner.Contains('saidaBrutaDescartada=$true') -and $runner.Contains('RedirectStandardError=$true') -and $runner.Contains('ReadToEndAsync'))
foreach($doc in @('README.md','migrations/README.md','docs/d21-contrato-flyway.md','evidencias/d21-relatorio-final.md')){
    $p=Join-Path $db $doc;$s=[IO.File]::ReadAllText($p)
    Check ('UTF8 '+$doc) ($s -notmatch '\uFFFD|\u00C3[\u0080-\u00BF]|\u00E2\u20AC')
    foreach($m in [regex]::Matches($s,'\[[^\]]+\]\((?<ref>[^)]+)\)')){
        $r=$m.Groups['ref'].Value;if($r -match '^https?://'){continue};$r=($r -split '#')[0]
        if($r){Check ('link '+$doc+':'+$r) (Test-Path -LiteralPath (Join-Path (Split-Path $p -Parent) $r))}
    }
}
$falhas=@($checks|Where-Object {-not $_.aprovado})
[pscustomobject]@{natureza='D21_PACOTE_SINTAXE_LINKS_HASHES_OFFLINE_NAO_SQL';total=$checks.Count;aprovados=$checks.Count-$falhas.Count;falhas=$falhas.Count;checks=$checks.ToArray();sqlExecutado=$false;mavenExecutado=$false}|ConvertTo-Json -Depth 6
if($falhas.Count){exit 1}
