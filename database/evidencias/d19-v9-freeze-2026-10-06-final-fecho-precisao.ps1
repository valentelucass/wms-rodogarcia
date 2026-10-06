# Fecho documental posterior ao73/73, sem invocar comparacao de backend.
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$prefixo='database/evidencias/d19-v9-freeze-2026-10-06-final'
$utf8=New-Object Text.UTF8Encoding($false)
function HashPrecisao([string]$p){(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash}
function NovoPrecisao([string]$p,$v,[switch]$Texto){$dest=Join-Path $script:raiz $p;if(Test-Path -LiteralPath $dest){throw 'DESTINO_EXISTENTE'};$t=if($Texto){[string]$v}else{$v|ConvertTo-Json -Depth 16};[IO.File]::WriteAllText($dest,$t,$script:utf8)}
$inicio=(Get-Date).ToString('o');$LASTEXITCODE=$null
try{
    $base='database/evidencias/d19-v9-2026-10-06-historico360-p2-pendente.sha256'
    $copias=Get-Content -LiteralPath (Join-Path $raiz ($prefixo+'-precisao-cronologia-copias.json')) -Raw -Encoding UTF8|ConvertFrom-Json
    $checks=[ordered]@{};$arquivos=@();$historicos=@();$alterados=@()
    $checks['manifesto400_original_bytes_preservados']=((HashPrecisao $base) -ceq 'BA42F36E77655E8CDE3F80083B657ACB5FA251F57FC04299FB127E7F141C7872')
    foreach($l in Get-Content -LiteralPath (Join-Path $raiz $base) -Encoding UTF8){if($l -notmatch '^([A-F0-9]{64})\s+(.+)$'){throw 'MANIFESTO_FORMATO'};$h=$Matches[1];$p=$Matches[2];$arquivos+=$p;$atual=HashPrecisao $p;if($atual -ceq $h){$historicos+=$p}else{$c=@($copias|Where-Object {$_.arquivo -ceq $p});$ok=$c.Count -eq 1 -and (HashPrecisao $c[0].copia) -ceq $h;$checks[('mudanca_documental_com_copia.'+$p)]=$ok;$alterados+=[pscustomobject]@{arquivo=$p;antes=$h;depois=$atual;copia=if($c.Count){$c[0].copia}else{$null};copiaExata=$ok}}}
    $checks['400_395_preservados_cinco_redacoes_com_copia']=($arquivos.Count -eq 400 -and $historicos.Count -eq 395 -and $alterados.Count -eq 5)
    $links=@();$documentos=@($copias.arquivo)+@($prefixo+'-fecho-historico.md')
    foreach($p in $documentos){$texto=Get-Content -LiteralPath (Join-Path $raiz $p) -Raw -Encoding UTF8;foreach($m in [regex]::Matches($texto,'\[[^\]]*\]\((?<alvo>[^)]+)\)')){$alvo=$m.Groups['alvo'].Value.Trim('<','>') -replace '#.*$','';if(-not $alvo -or $alvo -match '^[a-z]+://'){continue};$path=[IO.Path]::GetFullPath((Join-Path (Split-Path (Join-Path $raiz $p)) $alvo));$links+=[pscustomobject]@{arquivo=$p;alvo=$alvo;existe=(Test-Path -LiteralPath $path)}}}
    $checks['links_apos_precisao_documental_validos']=(@($links|Where-Object {-not $_.existe}).Count -eq 0)
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    NovoPrecisao ($prefixo+'-fecho-precisao-checks.json') ([pscustomobject]@{capturadoEm=(Get-Date).ToString('o');total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div;checks=$checks;links=$links.Count;alteracoes=$alterados;semComparacaoBackend=$true;semSqlJvmBuildRedeAmbiente=$true})
    NovoPrecisao ($prefixo+'-fecho-precisao-metadados.json') ([pscustomobject]@{inicio=$inicio;fim=(Get-Date).ToString('o');comando='./database/evidencias/d19-v9-freeze-2026-10-06-final-fecho-precisao.ps1';lastExitCode=$LASTEXITCODE;conclusao='Conclusao PowerShell sem comando nativo; LASTEXITCODE null nao implica saida0 nativa';somenteHashLinksArquivos=$true})
    if($div.Count){[pscustomobject]@{total=$checks.Count;divergencias=$div}|ConvertTo-Json;exit 1}
    $arquivos+=@($base)
    foreach($pasta in @('database/evidencias','infra/evidencias')){$arquivos+=@(Get-ChildItem -LiteralPath (Join-Path $raiz $pasta) -File|Where-Object {$_.Name -like 'd19-v9-freeze-2026-10-06-final*' -or $_.Name -like 'd19-be14-360-historico-2026-10-06*'}|ForEach-Object {$pasta+'/'+$_.Name})}
    $linhas=@($arquivos|Sort-Object -Unique|ForEach-Object {(HashPrecisao $_)+'  '+$_})
    $manifesto='database/evidencias/d19-v9-2026-10-06-historico360-p2-pendente-fecho.sha256';NovoPrecisao $manifesto ($linhas -join "`n") -Texto
    $conferidos=0;foreach($l in $linhas){if($l -notmatch '^([A-F0-9]{64})\s+(.+)$'){throw 'MANIFESTO_FINAL_FORMATO'};$h=$Matches[1];$p=$Matches[2];if((HashPrecisao $p) -cne $h){throw 'MANIFESTO_FINAL_DIVERGENTE'};$conferidos++}
    [pscustomobject]@{checks=$checks.Count;aprovados=$checks.Count;links=$links.Count;manifestoArquivos=$conferidos;manifestoSha256=HashPrecisao $manifesto;comparacaoBackend='NAO_EXECUTADA';aceite='RETIDO_P2_LOCKS'}|ConvertTo-Json
}catch{
    '{"bloqueio":"BLOQUEADO_FECHO_DOCUMENTAL","rawExposto":false,"semComparacaoBackend":true}'
    exit 1
}
