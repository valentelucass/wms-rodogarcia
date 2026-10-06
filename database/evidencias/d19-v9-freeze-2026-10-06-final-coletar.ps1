# Coleta local autorizada D19 final BE14. Somente arquivos; nenhum ambiente/rede/runtime.
[CmdletBinding()]
param([ValidateSet('Antes','Leitor','Suplemento','Fixtures','Tecnico')][string]$Etapa='Antes',[string]$Sufixo='inicial')
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$prefixo='database/evidencias/d19-v9-freeze-2026-10-06-final'
$utf8=New-Object Text.UTF8Encoding($false)
function Hash([string]$p){(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash}
function Novo([string]$p,[string]$texto){$alvo=Join-Path $script:raiz $p;if(Test-Path -LiteralPath $alvo){throw 'DESTINO_EXISTENTE_PRESERVADO'};[IO.File]::WriteAllText($alvo,$texto,$script:utf8)}
function Json([string]$p,$v){Novo $p ($v|ConvertTo-Json -Depth 22)}
function Copiar([string]$p,[string]$dest){if(Test-Path -LiteralPath (Join-Path $script:raiz $dest)){throw 'COPIA_EXISTENTE_PRESERVADA'};Copy-Item -LiteralPath (Join-Path $script:raiz $p) -Destination (Join-Path $script:raiz $dest);[pscustomobject]@{arquivo=$p;sha256=Hash $p;copia=$dest;hashCopia=Hash $dest}}
if($Sufixo -notmatch '^[a-z0-9-]+$'){throw 'SUFIXO_INVALIDO'}
if($Etapa -eq 'Antes'){
    . (Join-Path $raiz 'database/leitores/v9-java-arquivos.ps1')
    $f=V9Freeze $raiz 'backend/evidencias/d19-bloco4-freeze-final.sha256'
    if($f.sha256 -cne '42284559254B36C42806E9DC461E9A3A6C49A654984D9D1B332B745226ED3486' -or $f.mapa.Count -ne 289){throw 'FREEZE_DIVERGENTE'}
    $fontes=@();foreach($p in @($f.mapa.Keys|Sort-Object)){$h=Hash $p;$fontes+=[pscustomobject]@{arquivo=$p;esperado=$f.mapa[$p];antes=$h;confere=($h -ceq $f.mapa[$p])}}
    if(@($fontes|Where-Object {-not $_.confere}).Count){throw 'FONTE_FREEZE_DIVERGENTE'}
    $anterior='database/evidencias/d19-v9-leitor-jpa-preparo-2026-10-06.sha256';$baseline=@()
    foreach($l in Get-Content -LiteralPath (Join-Path $raiz $anterior) -Encoding UTF8){if($l -match '^([A-F0-9]{64})\s+(.+)$'){$p=$Matches[2];$h=Hash $p;$baseline+=[pscustomobject]@{arquivo=$p;esperado=$Matches[1];antes=$h;confere=($h -ceq $Matches[1])}}}
    if($baseline.Count -ne 333 -or @($baseline|Where-Object {-not $_.confere}).Count){throw 'BASELINE_333_DIVERGENTE'}
    $copias=@();$arquivos=@('database/verificar-schema-v9.ps1','database/leitores/v9-java-arquivos.ps1','database/leitores/v9-comparar-jpa.ps1','database/verificar-v9-freeze-suplementar.ps1','database/verificar-parser-v9-fixtures.ps1','database/contratos/v9-schema-doc31.json','database/contratos/v9-guardas-leitura.json','database/contratos/v9-leitor-jpa.md','database/procedimento-v9.md','database/contratos/v9-matriz-cobertura.md','infra/verificar-preparacao-final-local.ps1','infra/contratos/preparacao-final-local.json','infra/preparacao-tecnica-final.md','infra/recuperacao-e-ensaio.md','database/permissoes-minimas.md','database/README.md','database/migrations/README.md','infra/README.md','infra/configuracao-externa.md','docs/33-preparacao-tecnica-local-backend.md')
    foreach($p in $arquivos){$dest=$prefixo+'-antes-'+($p.Replace('/','-'));$copias+=Copiar $p $dest}
    $copias+=Copiar 'docs/31-fechamento-contagem-e-contingencia.md' ($prefixo+'-fonte-doc31.md')
    $sql=@(Get-ChildItem -LiteralPath (Join-Path $raiz 'database/migrations') -Filter 'V*.sql' -File|Sort-Object Name|ForEach-Object {[pscustomobject]@{arquivo=('database/migrations/'+$_.Name);sha256=Hash ('database/migrations/'+$_.Name)}})
    $jar='backend/target-be14/wms-backend-0.0.1-SNAPSHOT.jar'
    $artefato=[pscustomobject]@{arquivo=$jar;bytes=(Get-Item -LiteralPath (Join-Path $raiz $jar)).Length;sha256=Hash $jar;executado=$false}
    if($artefato.bytes -ne 78524597 -or $artefato.sha256 -cne '829A59B6CA60AF314089D1A0D303603FF3CACEEDAB4076F47608D7B513B3BA13'){throw 'ARTEFATO_DIVERGENTE'}
    Json ($prefixo+'-antes.json') ([pscustomobject]@{capturadoEm=(Get-Date).ToString('o');freeze=[pscustomobject]@{arquivo=$f.arquivo;sha256=$f.sha256;total=$fontes.Count;fontes=$fontes};manifesto333=[pscustomobject]@{arquivo=$anterior;sha256=Hash $anterior;fontes=$baseline};manifesto307=[pscustomobject]@{arquivo='infra/evidencias/d19-preparacao-tecnica-final-2026-10-06-final.sha256';sha256=Hash 'infra/evidencias/d19-preparacao-tecnica-final-2026-10-06-final.sha256'};copias=$copias;migrations=$sql;artefato=$artefato;semSqlJvmBuildRedeAmbiente=$true})
    [pscustomobject]@{etapa=$Etapa;freeze=$fontes.Count;baseline=$baseline.Count;copias=$copias.Count;jarBytes=$artefato.bytes}|ConvertTo-Json -Compress
    return
}
$saidas=@{Leitor='leitor';Suplemento='suplemento';Fixtures='fixtures';Tecnico='tecnico'}
$out=$prefixo+'-'+$saidas[$Etapa]+'-'+$Sufixo+'.json';$meta=$prefixo+'-'+$saidas[$Etapa]+'-'+$Sufixo+'-execucao.json'
if((Test-Path -LiteralPath (Join-Path $raiz $out)) -or (Test-Path -LiteralPath (Join-Path $raiz $meta))){throw 'EVIDENCIA_EXISTENTE_PRESERVADA'}
$inicio=(Get-Date).ToString('o');$LASTEXITCODE=$null
switch($Etapa){
    Leitor {$comando='./database/verificar-schema-v9.ps1 -CompararJpa -ManifestoFreeze backend/evidencias/d19-bloco4-freeze-final.sha256';$saida=& (Join-Path $raiz 'database/verificar-schema-v9.ps1') -CompararJpa -ManifestoFreeze 'backend/evidencias/d19-bloco4-freeze-final.sha256';$ok=$?;$codigo=$LASTEXITCODE}
    Suplemento {$comando='./database/verificar-v9-freeze-suplementar.ps1 -ManifestoFreeze backend/evidencias/d19-bloco4-freeze-final.sha256';$saida=& (Join-Path $raiz 'database/verificar-v9-freeze-suplementar.ps1') -ManifestoFreeze 'backend/evidencias/d19-bloco4-freeze-final.sha256';$ok=$?;$codigo=$LASTEXITCODE}
    Fixtures {$comando='./database/verificar-parser-v9-fixtures.ps1';$saida=& (Join-Path $raiz 'database/verificar-parser-v9-fixtures.ps1');$ok=$?;$codigo=$LASTEXITCODE}
    Tecnico {$comando='./infra/verificar-preparacao-final-local.ps1 -DiretorioArtefato target-be14';$saida=& (Join-Path $raiz 'infra/verificar-preparacao-final-local.ps1') -DiretorioArtefato 'target-be14';$ok=$?;$codigo=$LASTEXITCODE}
}
$fim=(Get-Date).ToString('o');Novo $out ($saida -join "`n")
Json $meta ([pscustomobject]@{inicio=$inicio;fim=$fim;comando=$comando;powershellSuccess=$ok;lastExitCode=$codigo;conclusao=if($codigo -eq $null){'PowerShell concluido; LASTEXITCODE nulo nao comprova saida nativa 0. Conferir output integral.'}else{'Codigo retornado pelo script preservado; conferir output integral.'};output=$out;outputSha256=Hash $out;semSqlJvmBuildRedeAmbiente=$true})
$v=($saida -join "`n")|ConvertFrom-Json
[pscustomobject]@{etapa=$Etapa;output=$out;total=$v.totalChecks;totalSuplemento=$v.total;aprovados=$v.aprovados;falhas=$v.divergencias;bloqueio=$v.bloqueio;powershellSuccess=$ok;lastExitCode=$codigo}|ConvertTo-Json -Depth 4
