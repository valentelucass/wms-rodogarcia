[CmdletBinding()]
param([ValidateSet('Inventariar','Mover','Ajustar','Conferir')][string]$Etapa)
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$db=Join-Path $raiz 'database'
$ev=Join-Path $db 'evidencias'
$inventario=Join-Path $ev 'd20-organizacao-inventario-antes.json'
$mapPath=Join-Path $ev 'd20-organizacao-de-para.json'
function Rel([string]$P){$P.Substring($raiz.Length+1).Replace('\','/')}
function Sha([string]$P){(Get-FileHash -LiteralPath $P -Algorithm SHA256).Hash}
function Gravar([string]$P,[string]$Texto){[IO.File]::WriteAllText($P,$Texto,(New-Object Text.UTF8Encoding($true)))}
function Backup([string]$P){
    $dest=Join-Path $ev ('d20-organizacao-originais/'+(Rel $P))
    if(-not(Test-Path -LiteralPath $dest)){New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($dest)) -Force|Out-Null;Copy-Item -LiteralPath $P -Destination $dest}
    return $dest
}
if($Etapa -eq 'Inventariar'){
    if(Test-Path -LiteralPath $inventario){throw 'Inventario anterior preservado; nao sobrescrever'}
    $files=@();foreach($f in Get-ChildItem -LiteralPath $db -Recurse -File){$files+=[pscustomobject]@{arquivo=(Rel $f.FullName);bytes=$f.Length;sha256=(Sha $f.FullName)}}
    $map=@()
    foreach($f in Get-ChildItem -LiteralPath $db -File){
        $dir=switch($f.Extension.ToLowerInvariant()){'.ps1'{'scripts'};'.xml'{'config'};'.md'{if($f.Name -cne 'README.md'){'docs'}}}
        if($dir){$map+=[pscustomobject]@{antes=(Rel $f.FullName);depois=('database/'+$dir+'/'+$f.Name);shaAntes=(Sha $f.FullName)}}
    }
    foreach($dir in @('leitores','criacao')){foreach($f in Get-ChildItem -LiteralPath (Join-Path $db $dir) -File -Filter '*.ps1'){
        $map+=[pscustomobject]@{antes=(Rel $f.FullName);depois=('database/scripts/'+$dir+'/'+$f.Name);shaAntes=(Sha $f.FullName)}
    }}
    [pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');natureza='INVENTARIO_PRE_MOVIMENTO_SEM_SQL';total=$files.Count;arquivos=$files}|ConvertTo-Json -Depth 5|Set-Content -LiteralPath $inventario -Encoding UTF8
    [pscustomobject]@{natureza='DE_PARA_PRE_MOVIMENTO';movimentos=$map}|ConvertTo-Json -Depth 5|Set-Content -LiteralPath $mapPath -Encoding UTF8
    [pscustomobject]@{arquivos=$files.Count;movimentos=$map.Count}|ConvertTo-Json;return
}
$mapDoc=Get-Content -LiteralPath $mapPath -Raw -Encoding UTF8|ConvertFrom-Json
$map=@($mapDoc.movimentos)
if($Etapa -eq 'Mover'){
    foreach($m in $map){
        $src=[IO.Path]::GetFullPath((Join-Path $raiz $m.antes));$dst=[IO.Path]::GetFullPath((Join-Path $raiz $m.depois))
        foreach($p in @($src,$dst)){if(-not $p.StartsWith($db+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Movimento fora de database recusado'}}
        if(Test-Path -LiteralPath $dst){throw 'Destino existente preservado; movimento recusado'}
        $backup=Backup $src
        New-Item -ItemType Directory -Path ([IO.Path]::GetDirectoryName($dst)) -Force|Out-Null
        Move-Item -LiteralPath $src -Destination $dst
        $h=Sha $dst;if($h -cne $m.shaAntes){throw 'Hash divergiu durante movimento'}
        $m|Add-Member shaAposMovimento $h;$m|Add-Member originalPreservado (Rel $backup)
    }
    $readme=Join-Path $db 'README.md';$null=Backup $readme
    Copy-Item -LiteralPath $readme -Destination (Join-Path $db 'docs/README-historico-d20.md')
    $mapDoc.natureza='MOVIMENTOS_BYTE_A_BYTE_CONFIRMADOS'
    $mapDoc|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $mapPath -Encoding UTF8
    [pscustomobject]@{movidos=$map.Count;hashesIdenticos=$map.Count;readmeOriginalPreservado=$true}|ConvertTo-Json;return
}
$trocas=@{};foreach($m in $map){$trocas[$m.antes]=$m.depois}
function CaminhosTexto([string]$S){
    foreach($k in @($trocas.Keys|Sort-Object Length -Descending)){$S=$S.Replace($k,$trocas[$k]).Replace($k.Replace('/','\'),$trocas[$k].Replace('/','\'))}
    return $S
}
function Links([string]$S,[string]$Origem,[string]$Destino){
    $od=[IO.Path]::GetDirectoryName($Origem);$nd=[IO.Path]::GetDirectoryName($Destino)
    [regex]::Replace($S,'\[[^\]]+\]\((?<ref>[^)]+)\)',[Text.RegularExpressions.MatchEvaluator]{param($m)
        $r=$m.Groups['ref'].Value
        if($r -match '^(https?://|#|app://)' -or $r -match '^[A-Za-z]:'){return $m.Value}
        $parts=$r -split '#',2;$path=$parts[0]
        if(-not $path){return $m.Value}
        $target=[IO.Path]::GetFullPath((Join-Path $od $path));$oldRel=Rel $target
        if($trocas.ContainsKey($oldRel)){$target=Join-Path $raiz $trocas[$oldRel]}
        $base=[Uri]('file:///'+$nd.Replace('\','/')+'/');$uri=[Uri]('file:///'+$target.Replace('\','/'))
        $novo=[Uri]::UnescapeDataString($base.MakeRelativeUri($uri).ToString());if($parts.Count -gt 1){$novo+='#'+$parts[1]}
        $m.Value.Replace(']('+$r+')',']('+$novo+')')
    })
}
if($Etapa -eq 'Ajustar'){
    foreach($m in $map|Where-Object {$_.depois.EndsWith('.ps1')}){
        $p=Join-Path $raiz $m.depois;$s=[IO.File]::ReadAllText($p)
        $s=CaminhosTexto $s
        # Literais relativos dos scripts antigos sao relativos a sua antiga base de dados.
        foreach($k in @($trocas.Keys|Sort-Object Length -Descending)){
            $o=$k.Substring('database/'.Length);$n=$trocas[$k].Substring('database/'.Length)
            foreach($q in @("'",'"')){$s=$s.Replace($q+$o+$q,$q+$n+$q).Replace($q+$o.Replace('/','\')+$q,$q+$n.Replace('/','\')+$q)}
        }
        if($s.Contains('$PSScriptRoot')){
            $alias='$WmsD20Path'+([IO.Path]::GetFileNameWithoutExtension($p) -replace '[^A-Za-z0-9]','')
            $s=$s.Replace('$PSScriptRoot',$alias)
            $oldFolder=[IO.Path]::GetDirectoryName($m.antes).Replace('\','/');$base='..'
            $sub=$oldFolder.Substring('database'.Length).TrimStart('/')
            if($sub){$base='../..'}
            $def=$alias+"=[IO.Path]::GetFullPath((Join-Path `$PSScriptRoot '$base'))"+"`r`n"
            if($sub){$def+=$alias+"=Join-Path $alias '$sub'"+"`r`n"}
            $t=$null;$e=$null;$ast=[Management.Automation.Language.Parser]::ParseInput($s,[ref]$t,[ref]$e)
            $pos=if($ast.ParamBlock){$ast.ParamBlock.Extent.EndOffset}else{0}
            $s=$s.Insert($pos,"`r`n"+$def)
        }
        Gravar $p $s
    }
    $docs=@(Get-ChildItem -LiteralPath $db -Recurse -File -Filter '*.md'|Where-Object {$_.FullName -notmatch '\\evidencias\\'})
    $docs+=@(Get-ChildItem -LiteralPath (Join-Path $raiz 'infra') -File -Filter '*.md')
    foreach($f in $docs){
        $rel=Rel $f.FullName;$origem=$f.FullName
        $m=@($map|Where-Object {$_.depois -ceq $rel});if($m.Count){$origem=Join-Path $raiz $m[0].antes}
        if($rel -ceq 'database/docs/README-historico-d20.md'){$origem=Join-Path $db 'README.md'}
        $null=Backup $f.FullName;$s=[IO.File]::ReadAllText($f.FullName)
        $s=Links $s $origem $f.FullName;$s=CaminhosTexto $s
        Gravar $f.FullName $s
    }
    foreach($f in Get-ChildItem -LiteralPath (Join-Path $raiz 'infra') -File -Filter '*.ps1'){
        if($f.FullName -ceq $PSCommandPath){continue}
        $s=[IO.File]::ReadAllText($f.FullName);$novo=CaminhosTexto $s
        if($novo -cne $s){$null=Backup $f.FullName;Gravar $f.FullName $novo}
    }
    $bat=Join-Path $db 'iniciar-bancos.bat';$null=Backup $bat
    $s=[IO.File]::ReadAllText($bat).Replace('%~dp0iniciar-bancos.ps1','%~dp0scripts\iniciar-bancos.ps1')
    [IO.File]::WriteAllText($bat,($s -replace '\r?\n',"`r`n"),[Text.Encoding]::ASCII)
    [pscustomobject]@{caminhosAjustados=$map.Count;sqlEditado=$false;historicosEvidenciasEditados=$false}|ConvertTo-Json;return
}
if($Etapa -eq 'Conferir'){
    $inv=Get-Content -LiteralPath $inventario -Raw -Encoding UTF8|ConvertFrom-Json;$perdidos=@();$preservados=0
    foreach($f in $inv.arquivos){
        $dest=if($trocas.ContainsKey($f.arquivo)){$trocas[$f.arquivo]}else{$f.arquivo}
        $p=Join-Path $raiz $dest;$copia=Join-Path $ev ('d20-organizacao-originais/'+$f.arquivo)
        if((Test-Path -LiteralPath $p) -and (Sha $p) -ceq $f.sha256){$preservados++}
        elseif((Test-Path -LiteralPath $copia) -and (Sha $copia) -ceq $f.sha256){$preservados++}
        else{$perdidos+=$f.arquivo}
    }
    foreach($m in $map){$m|Add-Member -Force shaAposAjustes (Sha (Join-Path $raiz $m.depois))}
    $mapDoc.natureza='DE_PARA_HASH_ANTES_MOVIMENTO_AJUSTES_ORIGINAIS_PRESERVADOS'
    $mapDoc|ConvertTo-Json -Depth 6|Set-Content -LiteralPath $mapPath -Encoding UTF8
    $r=[pscustomobject]@{natureza='PRESERVACAO_TODOS_ARQUIVOS_PREEXISTENTES';totalAntes=$inv.total;originaisDisponiveis=$preservados;perdidos=$perdidos;
        migrations=@($inv.arquivos|Where-Object {$_.arquivo -match '^database/migrations/.*\.sql$'}).Count}
    $r|ConvertTo-Json -Depth 5|Set-Content -LiteralPath (Join-Path $ev 'd20-organizacao-preservacao.json') -Encoding UTF8
    $r|ConvertTo-Json -Depth 5;if($perdidos.Count){exit 1}
}
