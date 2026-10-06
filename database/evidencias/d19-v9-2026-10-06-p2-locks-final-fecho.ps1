# Fecho local p2-locks367. Somente AST/JSON/XML/links/hashes; nao inicia runtime.
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$prefixo='database/evidencias/d19-v9-2026-10-06-p2-locks-final'
$utf8=New-Object Text.UTF8Encoding($false,$true)
$checks=[ordered]@{};$inicio=(Get-Date).ToString('o')
function HF([string]$p){(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash}
function LF([string]$p){[IO.File]::ReadAllText((Join-Path $script:raiz $p),$script:utf8)}
function JF([string]$p){(LF $p)|ConvertFrom-Json}
function NF([string]$p,$v,[switch]$Texto){$d=Join-Path $script:raiz $p;if(Test-Path -LiteralPath $d){throw 'DESTINO_EXISTENTE'};$t=if($Texto){[string]$v}else{$v|ConvertTo-Json -Depth 26};[IO.File]::WriteAllText($d,$t,(New-Object Text.UTF8Encoding($false)))}
function CasosXml([string]$pasta){
    $casos=New-Object 'System.Collections.Generic.HashSet[string]' ([StringComparer]::Ordinal)
    $q=0
    foreach($x in @(Get-ChildItem -LiteralPath (Join-Path $script:raiz $pasta) -Filter '*.xml' -File)){
        $settings=New-Object Xml.XmlReaderSettings;$settings.DtdProcessing=[Xml.DtdProcessing]::Prohibit;$settings.XmlResolver=$null
        $xr=[Xml.XmlReader]::Create($x.FullName,$settings)
        try{$doc=New-Object Xml.XmlDocument;$doc.XmlResolver=$null;$doc.Load($xr)}finally{$xr.Dispose()}
        foreach($c in $doc.SelectNodes('/testsuite/testcase')){$q++;[void]$casos.Add($c.GetAttribute('classname')+'|'+$c.GetAttribute('name'))}
    }
    [pscustomobject]@{total=$q;identidades=$casos}
}
try{
    $a=JF ($prefixo+'-antes.json');$copias=@($a.copias)+@(JF ($prefixo+'-copia-adicional.json'))
    $checks['antes.289_exatos']=($a.freeze.total -eq 289 -and @($a.freeze.fontes|Where-Object {-not $_.confere}).Count -eq 0)
    $checks['antes.411_exatos']=(@($a.manifesto411.fontes).Count -eq 411 -and @($a.manifesto411.fontes|Where-Object {-not $_.confere}).Count -eq 0)
    $checks['manifesto411_bytes_preservados']=((HF $a.manifesto411.arquivo) -ceq $a.manifesto411.sha256)
    $checks['manifesto307_bytes_preservados']=((HF $a.manifesto307.arquivo) -ceq $a.manifesto307.sha256)
    $fontes=@();foreach($f in $a.freeze.fontes){$h=HF $f.arquivo;$fontes+=[pscustomobject]@{arquivo=$f.arquivo;antes=$f.antes;depois=$h;preservada=($h -ceq $f.antes)}}
    $checks['freeze289_antes_depois_estaveis']=(@($fontes|Where-Object {-not $_.preservada}).Count -eq 0)
    $checks['manifesto367_hash_estavel']=((HF $a.freeze.arquivo) -ceq $a.freeze.sha256)
    $migrations=@();foreach($m in $a.migrations){$h=HF $m.arquivo;$ok=$h -ceq $m.sha256;$checks[('migration.preservada.'+$m.arquivo)]=$ok;$migrations+=[pscustomobject]@{arquivo=$m.arquivo;antes=$m.sha256;depois=$h;preservada=$ok}}
    $c=JF 'database/contratos/v9-schema-doc31.json';$old=JF ($prefixo+'-antes-database-contratos-v9-schema-doc31.json')
    foreach($n in @('tabelas','indices','indicesFiltrados','auditoria','paresEstadoDados','servicoSituacao')){$checks[('estrutura_intacta.'+$n)]=(($c.$n|ConvertTo-Json -Depth 24) -ceq ($old.$n|ConvertTo-Json -Depth 24))}
    $checks['fonte31_atual_snapshot_exatos']=((HF $c.fonte) -ceq $c.fonteSha256 -and (HF $c.fonteSnapshot) -ceq $c.fonteSha256)
    $v=JF ($prefixo+'-leitor-final.json');$s=JF ($prefixo+'-suplemento-final.json');$t=JF ($prefixo+'-tecnico-final.json')
    $checks['principal904_integral']=($v.totalChecks -eq 904 -and $v.aprovados -eq 904 -and @($v.divergencias).Count -eq 0)
    $checks['suplemento129_integral']=($s.total -eq 129 -and $s.aprovados -eq 129 -and @($s.divergencias).Count -eq 0)
    $checks['tecnico238_integral']=($t.total -eq 238 -and $t.atendidos -eq 238 -and $t.falhas -eq 0)
    $checks['tres_leituras_mesmo_freeze']=($v.comparacaoJpa.freeze.sha256 -ceq $a.freeze.sha256 -and $s.freeze.sha256 -ceq $a.freeze.sha256 -and (JF 'infra/contratos/preparacao-final-local.json').freezeFinal.sha256 -ceq $a.freeze.sha256)
    foreach($etapa in @('leitor-final','suplemento-final','tecnico-final')){$e=JF ($prefixo+'-'+$etapa+'-execucao.json');$checks[('metadata.'+$etapa+'.normal_null_sem_saida0')]=($e.powershellSuccess -and $null -eq $e.lastExitCode -and $e.conclusao -match 'nulo nao comprova');$checks[('metadata.'+$etapa+'.hash_output')]=((HF $e.output) -ceq $e.outputSha256)}
    $checks['copia128_preservada']=((JF ($prefixo+'-suplemento-inicial.json')).aprovados -eq 128 -and (Test-Path -LiteralPath (Join-Path $raiz ($prefixo+'-guardas-antes128.json'))))
    $casos360=CasosXml 'backend/evidencias/d19-bloco4-xml-final';$casos367=CasosXml 'backend/evidencias/d19-bloco4-xml-p2-locks'
    $ausentes=@($casos360.identidades|Where-Object {-not $casos367.identidades.Contains($_)});$novos=@($casos367.identidades|Where-Object {-not $casos360.identidades.Contains($_)})
    $checks['xml360_todos_preservados_por_nome']=($casos360.total -eq 360 -and $casos360.identidades.Count -eq 360 -and $ausentes.Count -eq 0)
    $checks['xml367_sete_novos_sem_duplicidade']=($casos367.total -eq 367 -and $casos367.identidades.Count -eq 367 -and $novos.Count -eq 7)
    $pres=JF 'backend/evidencias/d19-bloco4-preservacao-p2-locks.json'
    $checks['preservacao_cedro286_tres_zeroausentes']=($pres.preservados -eq 286 -and $pres.alteradosP2 -eq 3 -and $pres.ausentes -eq 0 -and $pres.sha256Novo -ceq $a.freeze.sha256)
    $tecnico=JF 'infra/contratos/preparacao-final-local.json'
    $checks['original56_leitor_preservado']=((HF $tecnico.leitor56.arquivo) -ceq $tecnico.leitor56.sha256)
    $checks['original56_output_preservado']=((HF $tecnico.leitor56.output) -ceq $tecnico.leitor56.outputSha256)
    $checks['jar367_antes_depois_exato']=((HF $a.artefato.arquivo) -ceq $a.artefato.sha256 -and (Get-Item -LiteralPath (Join-Path $raiz $a.artefato.arquivo)).Length -eq 78526951)
    $checks['jar367_snapshot_exato']=((HF 'infra/evidencias/d19-preparacao-p2-locks367-2026-10-06-jar-snapshot.jar') -ceq $a.artefato.sha256)
    $checks['jar360_snapshot_antigo_preservado']=((HF 'infra/evidencias/d19-be14-360-historico-2026-10-06-jar-snapshot.jar') -ceq '829A59B6CA60AF314089D1A0D303603FF3CACEEDAB4076F47608D7B513B3BA13')
    $ps=@('database/verificar-schema-v9.ps1','database/verificar-v9-freeze-suplementar.ps1','database/leitores/v9-java-arquivos.ps1','database/leitores/v9-comparar-jpa.ps1','infra/verificar-preparacao-final-local.ps1','infra/verificar-artefato-historico-local.ps1',($prefixo+'-coletar.ps1'),($prefixo+'-fecho.ps1'))
    foreach($p in $ps){$tk=$null;$er=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $raiz $p),[ref]$tk,[ref]$er);$checks[('ast.'+$p)]=(@($er).Count -eq 0)}
    foreach($p in @('database/contratos/v9-schema-doc31.json','database/contratos/v9-guardas-leitura.json','infra/contratos/preparacao-final-local.json')){[void](JF $p);$checks[('json.'+$p)]=$true}
    $docs=@('docs/33-preparacao-tecnica-local-backend.md','database/procedimento-v9.md','database/contratos/v9-leitor-jpa.md','database/contratos/v9-matriz-cobertura.md','database/README.md','database/migrations/README.md','database/permissoes-minimas.md','infra/README.md','infra/preparacao-tecnica-final.md',($prefixo+'.md'),'infra/evidencias/d19-preparacao-p2-locks367-2026-10-06.md')
    $links=@();foreach($p in $docs){foreach($m in [regex]::Matches((LF $p),'\[[^\]]*\]\((?<alvo>[^)]+)\)')){$alvo=$m.Groups['alvo'].Value.Trim('<','>') -replace '#.*$','';if(-not $alvo -or $alvo -match '^[a-z]+://'){continue};$abs=[IO.Path]::GetFullPath((Join-Path (Split-Path (Join-Path $raiz $p)) $alvo));$links+=[pscustomobject]@{arquivo=$p;alvo=$alvo;existe=(Test-Path -LiteralPath $abs)}}}
    $checks['links_locais_validos']=(@($links|Where-Object {-not $_.existe}).Count -eq 0)
    $preservados=@();$mudados=@();foreach($f in $a.manifesto411.fontes){$h=HF $f.arquivo;if($h -ceq $f.antes){$preservados+=$f.arquivo}else{$cp=@($copias|Where-Object {$_.arquivo -ceq $f.arquivo -and $_.sha256 -ceq $f.antes});$ok=$cp.Count -eq 1 -and (HF $cp[0].copia) -ceq $f.antes;$checks[('alterado_com_copia.'+$f.arquivo)]=$ok;$mudados+=[pscustomobject]@{arquivo=$f.arquivo;antes=$f.antes;depois=$h;copia=if($cp.Count){$cp[0].copia}else{$null};copiaExata=$ok}}}
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    NF ($prefixo+'-depois.json') ([pscustomobject]@{capturadoEm=(Get-Date).ToString('o');freeze289=$fontes;migrations=$migrations;baseline411=[pscustomobject]@{iguais=$preservados.Count;alterados=$mudados.Count;alteracoes=$mudados};testesXml=[pscustomobject]@{anteriores=$casos360.total;atuais=$casos367.total;anterioresAusentes=$ausentes.Count;novos=$novos.Count};semSqlJvmH2BuildRedeAmbiente=$true})
    NF ($prefixo+'-checks-locais.json') ([pscustomobject]@{capturadoEm=(Get-Date).ToString('o');total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div;checks=$checks;linksQuantidade=$links.Count;links=$links;semSqlJvmH2BuildRedeAmbiente=$true})
    NF ($prefixo+'-fecho-metadados.json') ([pscustomobject]@{inicio=$inicio;fim=(Get-Date).ToString('o');comando='./database/evidencias/d19-v9-2026-10-06-p2-locks-final-fecho.ps1';natureza='Somente leitores de arquivo AST/JSON/XML/links/hashes';conclusao=if($div.Count){'Output com divergencia preservado'}else{'Conclusao PowerShell normal, sem inferir saida nativa0 dos LASTEXITCODE null'};semSqlJvmBuildRedeAmbiente=$true})
    if($div.Count){[pscustomobject]@{total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div}|ConvertTo-Json;exit 1}
    $arquivos=@($a.manifesto411.fontes.arquivo)+@($a.manifesto411.arquivo)
    foreach($dir in @('database/evidencias','infra/evidencias')){$arquivos+=@(Get-ChildItem -LiteralPath (Join-Path $raiz $dir) -File|Where-Object {$_.Name -like 'd19-v9-2026-10-06-p2-locks-final*' -or $_.Name -like 'd19-preparacao-p2-locks367-2026-10-06*'}|ForEach-Object {$dir+'/'+$_.Name})}
    $linhas=@($arquivos|Sort-Object -Unique|ForEach-Object {(HF $_)+'  '+$_});$manifesto=$prefixo+'.sha256';NF $manifesto ($linhas -join "`n") -Texto
    $q=0;foreach($l in $linhas){if($l -notmatch '^([A-F0-9]{64})\s+(.+)$'){throw 'MANIFESTO_FORMATO'};$h=$Matches[1];$p=$Matches[2];if((HF $p) -cne $h){throw 'MANIFESTO_FINAL_DIVERGENTE'};$q++}
    [pscustomobject]@{total=$checks.Count;aprovados=$checks.Count;links=$links.Count;freezeEstavel=$fontes.Count;migrationsPreservadas=$migrations.Count;baseline411Iguais=$preservados.Count;alteradosComCopia=$mudados.Count;manifestoArquivos=$q;manifestoSha256=HF $manifesto;compatibilidade='ESTRUTURAL_LEXICAL_CONFERIDA';aceite='FAROL_VIGIA';homologacaoExterna='NAO_EXECUTADA'}|ConvertTo-Json
}catch{
    '{"bloqueio":"BLOQUEADO_FECHO_ARQUIVOS_P2_LOCKS","rawExposto":false,"semSqlJvmBuildRedeAmbiente":true}'
    exit 1
}
