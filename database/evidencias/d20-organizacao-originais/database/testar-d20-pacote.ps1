[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$falhas=@();$checks=@()
function Conferir([string]$Nome,[bool]$Ok){$script:checks+=[pscustomobject]@{caso=$Nome;aprovado=$Ok};if(-not $Ok){$script:falhas+=$Nome}}
$baseline=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'evidencias/d20-baseline.json') -Raw -Encoding UTF8|ConvertFrom-Json
foreach($f in $baseline.migrations){Conferir ('freeze '+$f.arquivo) ((Get-FileHash -LiteralPath (Join-Path $raiz $f.arquivo) -Algorithm SHA256).Hash -ceq $f.sha256)}
$arquivos=@('d20-guardas.ps1','auditar-d20.ps1','migrate.ps1','criar-bancos.ps1','ensaio-local-d20.ps1',
    'testar-d20-guardas.ps1','testar-d20-parser.ps1','gerar-d20-matrizes.ps1','leitores/d20-schema-efetivo.ps1','criacao/ler-vazio.ps1','verificar-alvo-criacao.ps1','congelar-d20.ps1','iniciar-bancos.ps1','testar-d20-launcher.ps1')
foreach($f in $arquivos){
    $tokens=$null;$erros=$null;$null=[Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $f),[ref]$tokens,[ref]$erros)
    Conferir ('sintaxe '+$f) (@($erros).Count -eq 0)
}
foreach($script in @('criar-bancos.ps1','migrate.ps1','ensaio-local-d20.ps1','verificar-alvo-criacao.ps1')){
    $json=& (Join-Path $PSScriptRoot $script)|ConvertFrom-Json
    Conferir ('default offline '+$script) ($json.natureza -match 'OFFLINE')
}
$prod=& (Join-Path $PSScriptRoot 'criar-bancos.ps1') -Database WMS_PROD|ConvertFrom-Json
$offline=& (Join-Path $PSScriptRoot 'iniciar-bancos.ps1') -Offline|ConvertFrom-Json
Conferir 'launcher offline sem senha SQL DDL' ($offline.senhaLida -eq $false -and $offline.conexoes -eq 0 -and $offline.ddl -eq 0)
Conferir 'PROD somente CREATE separado' ($prod.migrationProd -eq $false -and $prod.cargaProd -eq $false -and $prod.existencia -ceq 'NAO_CONSULTADA')
$a=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'evidencias/d20-auditoria-final.json') -Raw -Encoding UTF8|ConvertFrom-Json
Conferir 'auditoria sem divergencias' (@($a.divergenciasJpa).Count -eq 0)
Conferir 'inventario efetivo completo' ($a.resumo.tabelas -eq 64 -and $a.resumo.colunas -eq 687 -and $a.resumo.fks -eq 130 -and $a.resumo.alteracoes -eq 80)
Conferir 'SQL preparado Hibernate pareado' ($a.resumo.updatesHibernatePreparados -eq 34 -and $a.sqlHibernateSha256 -ceq (Get-FileHash -LiteralPath (Join-Path $raiz 'backend/evidencias/d20-hibernate-update-sql.json') -Algorithm SHA256).Hash)
[xml]$pom=Get-Content -LiteralPath (Join-Path $raiz 'backend/pom.xml') -Raw -Encoding UTF8
$skip=$pom.SelectSingleNode('//*[local-name()="properties"]/*[local-name()="wms.migrations.skip"]')
$init=$pom.SelectSingleNode('//*[local-name()="properties"]/*[local-name()="wms.migrations.initSql"]')
Conferir 'POM default skip sem conector' ($skip.InnerText -ceq 'true')
Conferir 'POM default SQL recusa sem guarda' ($init.InnerText -match '^THROW 51002')
$txt=$pom.OuterXml
Conferir 'POM initSQL externo gerado wrapper' ($txt.Contains('${env.WMS_DB_MIGRATION_INIT_SQL}'))
Conferir 'POM identidades migrations' ($txt.Contains('${env.WMS_DB_MIGRATION_USER}') -and $txt.Contains('${env.WMS_DB_MIGRATION_PASSWORD}'))
$links=@()
foreach($doc in @('d20-plano.md','d20-procedimentos.md','indices-d20.md','permissoes-d20.md','d20-consultas.md','d20-referencias-launcher.md','evidencias/d20-relatorio-final.md','../infra/d20-prontidao.md')){
    $path=Join-Path $PSScriptRoot $doc;$s=[IO.File]::ReadAllText($path)
    Conferir ('acentuacao '+$doc) ($s -notmatch '\u00C3[\u0080-\u00BF]|\u00E2\u20AC|\uFFFD')
    foreach($m in [regex]::Matches($s,'\[[^\]]+\]\((?<ref>[^)]+)\)')){
        $r=$m.Groups['ref'].Value;if($r -match '^https?://'){continue};$r=($r -split '#')[0]
        if($r){$links+=$doc+':'+$r;Conferir ('link '+$doc+':'+$r) (Test-Path -LiteralPath (Join-Path ([IO.Path]::GetDirectoryName($path)) $r))}
    }
}
[pscustomobject]@{natureza='CHECKS_ARQUIVOS_DEFAULTS_OFFLINE_NAO_SQL_SERVER';utc=[DateTime]::UtcNow.ToString('o');
    total=$checks.Count;aprovados=$checks.Count-$falhas.Count;falhas=$falhas;links=$links.Count;checks=$checks}|ConvertTo-Json -Depth 7
if($falhas.Count){exit 1}
