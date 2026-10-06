# Manifesto tecnico separado; nao sobrescreve baseline456 ou qualquer output.
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$prefixo='infra/evidencias/d19-correcao-identidade-ciclos-2026-10-06'
$utf8=New-Object Text.UTF8Encoding($false,$true)
function HF([string]$p){(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash}
function JF([string]$p){[IO.File]::ReadAllText((Join-Path $script:raiz $p),$script:utf8)|ConvertFrom-Json}
try {
    $a=JF ($prefixo+'-antes.json');$fecho=JF ($prefixo+'-fecho-final-output.json');$meta=JF ($prefixo+'-fecho-final-execucao.json')
    if($fecho.aprovados -ne 77 -or $fecho.total -ne 77 -or $fecho.divergencias.Count -or -not $meta.powershellSuccess -or $null -ne $meta.lastExitCode -or (HF $meta.output) -cne $meta.sha256){throw 'BLOQUEADO_FECHO_NAO_INTEGRAL'}
    if((HF $a.manifesto) -cne $a.manifestoSha256){throw 'BLOQUEADO_BASELINE456_ALTERADA'}
    foreach($f in $fecho.baseline456.fontes){if((HF $f.arquivo) -cne $f.depois){throw 'BLOQUEADO_ARQUIVO_MUDOU_APOS_FECHO'}}
    foreach($f in $fecho.freezeBackend289){if((HF $f.arquivo) -cne $f.atual){throw 'BLOQUEADO_BACKEND_MUDOU_APOS_FECHO'}}
    $arquivos=@($a.fontes.arquivo)+@($a.manifesto)+@('infra/leitores/identidade-ciclos-preparacao.ps1','infra/verificar-identidade-ciclos-fixtures.ps1')
    $arquivos+=@(Get-ChildItem -LiteralPath (Join-Path $raiz 'infra/evidencias') -Filter 'd19-correcao-identidade-ciclos-2026-10-06*' -File|Where-Object {$_.Extension -ne '.sha256'}|ForEach-Object {'infra/evidencias/'+$_.Name})
    $arquivos=@($arquivos|Sort-Object -Unique)
    $manifesto=$prefixo+'-fecho.sha256';$abs=Join-Path $raiz $manifesto
    if(Test-Path -LiteralPath $abs){throw 'BLOQUEADO_DESTINO_MANIFESTO_EXISTENTE'}
    $linhas=@($arquivos|ForEach-Object {(HF $_)+'  '+$_})
    [IO.File]::WriteAllText($abs,($linhas -join "`n"),(New-Object Text.UTF8Encoding($false)))
    $div=@();foreach($l in $linhas){$m=[regex]::Match($l,'^([A-F0-9]{64})\s+(.+)$');if(-not $m.Success -or (HF $m.Groups[2].Value) -cne $m.Groups[1].Value){$div+='HASH_DIVERGENTE'}}
    $links=0;$falhasLinks=0
    foreach($p in @($fecho.links.arquivo|Sort-Object -Unique)){
        $texto=[IO.File]::ReadAllText((Join-Path $raiz $p),$utf8)
        foreach($m in [regex]::Matches($texto,'\[[^\]]*\]\((?<alvo>[^)]+)\)')){
            $alvo=$m.Groups['alvo'].Value.Trim('<','>') -replace '#.*$','';if(-not $alvo -or $alvo -match '^[a-z]+://'){continue}
            $pAbs=[IO.Path]::GetFullPath((Join-Path (Split-Path (Join-Path $raiz $p)) $alvo));$links++
            if(-not $pAbs.StartsWith($raiz+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase) -or -not (Test-Path -LiteralPath $pAbs)){$falhasLinks++}
        }
    }
    if($div.Count -or $falhasLinks){throw 'BLOQUEADO_MANIFESTO_OU_LINK_FINAL'}
    [pscustomobject]@{formato='wms-manifesto-correcao-identidade-v1';capturadoEm=(Get-Date).ToString('o');manifesto=$manifesto;sha256=HF $manifesto;arquivos=$arquivos.Count;divergencias=$div;links=$links;linksAusentes=$falhasLinks;backendFontesEstaveis=@($fecho.freezeBackend289).Count;baseline456=[pscustomobject]@{manifesto=$a.manifesto;sha256=HF $a.manifesto;preservados=$fecho.baseline456.preservados;alteradosComCopia=$fecho.baseline456.alteradosComCopia};diagnostico='306/306';fixtures='18/18';fecho='77/77';limite='Manifesto tecnico dos arquivos listados, nao novo freeze/aceite de backend. Esta verificacao posterior e seus metadados ficam fora do proprio manifesto; nao inclui manifesto em si. Sem SQL/JVM/JPA/build/rede/ambiente.'}|ConvertTo-Json -Depth 6
} catch {
    $codigo='BLOQUEADO_MANIFESTO_IDENTIDADE';if($_.Exception.Message -match '^BLOQUEADO_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{status=$codigo;sem_raw=$true}|ConvertTo-Json
    exit 1
}
