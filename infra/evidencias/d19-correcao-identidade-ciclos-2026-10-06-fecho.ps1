# Fecho somente em arquivos. Nao executa comparacao JPA ou codigo de aplicacao.
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$prefixo='infra/evidencias/d19-correcao-identidade-ciclos-2026-10-06'
$utf8=New-Object Text.UTF8Encoding($false,$true)
$checks=[ordered]@{}
function HF([string]$p){(Get-FileHash -LiteralPath (Join-Path $script:raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash}
function LF([string]$p){[IO.File]::ReadAllText((Join-Path $script:raiz $p),$script:utf8)}
function JF([string]$p){(LF $p)|ConvertFrom-Json}
try {
    $a=JF ($prefixo+'-antes.json');$c=JF 'infra/contratos/preparacao-final-local.json'
    $checks['baseline.456_exatos_antes']=(@($a.fontes).Count -eq 456 -and @($a.fontes|Where-Object {-not $_.confere}).Count -eq 0)
    $checks['manifesto456_bytes_preservados']=((HF $a.manifesto) -ceq $a.manifestoSha256 -and $a.manifestoSha256 -ceq '3A0CEB5620050D8FAF9176780D5BEE2CEA96002B8593654583614F9062332C6F')
    $preservacao=@();$mudados=@();$iguais=0
    foreach($f in $a.fontes) {
        $h=HF $f.arquivo;$igual=($h -ceq $f.antes);$copia=$null;$copiaExata=$false
        if($igual){$iguais++}else{
            $cp=@($a.copias|Where-Object {$_.arquivo -ceq $f.arquivo -and $_.sha256 -ceq $f.antes})
            if($cp.Count -eq 1){$copia=$cp[0].copia;$copiaExata=((HF $copia) -ceq $f.antes)}
            $checks[('alteracao_com_copia.'+$f.arquivo)]=$copiaExata
            $mudados+=$f.arquivo
        }
        $preservacao+=[pscustomobject]@{arquivo=$f.arquivo;antes=$f.antes;depois=$h;preservado=$igual;copia=$copia;copiaExata=$copiaExata}
    }
    $checks['baseline.449_preservados_7_documentais_copiados']=($iguais -eq 449 -and $mudados.Count -eq 7 -and @($a.copias).Count -eq 7)
    $checks['baseline.outputs_antigos_intactos']=(@($preservacao|Where-Object {-not $_.preservado -and $_.arquivo -match '(?:output|execucao|leitor-final|suplemento-final|tecnico-final|checks-locais|\.sha256$)'}).Count -eq 0)
    $checks['escopo.somente_area_autorizada']=(@($mudados|Where-Object {$_ -notmatch '^(?:database/|infra/|docs/33-preparacao-tecnica-local-backend\.md$)'}).Count -eq 0)
    foreach($cp in $a.copias){$checks[('copia.exata.'+$cp.arquivo)]=((HF $cp.copia) -ceq $cp.sha256)}
    $migrations=@(foreach($m in $c.baselineV1V9){$h=HF $m.arquivo;[pscustomobject]@{arquivo=$m.arquivo;antes=$m.sha256;depois=$h;preservada=($h -ceq $m.sha256)}})
    $checks['migrations.V1_V9_byte_identicas']=($migrations.Count -eq 9 -and @($migrations|Where-Object {-not $_.preservada}).Count -eq 0)
    $freezeAntes=HF $c.freezeFinal.arquivo;$fontes=@()
    foreach($l in ((LF $c.freezeFinal.arquivo) -split '\r?\n')) {
        if(-not $l.Trim()){continue}
        if($l -ceq 'D19 bloco4 - freeze P2-locks - 2026-10-06 - SHA-256'){continue}
        $m=[regex]::Match($l,'^([A-Fa-f0-9]{64})\s+((?:backend|docs)/[\w./-]+)$')
        if(-not $m.Success -or $m.Groups[2].Value -match '(?:^|/)\.\.(?:/|$)'){throw 'BLOQUEADO_MANIFESTO_FORMATO'}
        $p=$m.Groups[2].Value;$h=HF $p
        $fontes+=[pscustomobject]@{arquivo=$p;esperado=$m.Groups[1].Value.ToUpperInvariant();atual=$h;confere=($h -ceq $m.Groups[1].Value.ToUpperInvariant())}
    }
    $checks['backend.freeze289_intacto_sem_leitura_JPA']=($fontes.Count -eq 289 -and @($fontes|Where-Object {-not $_.confere}).Count -eq 0 -and $freezeAntes -ceq $c.freezeFinal.sha256)
    $checks['backend.manifesto_estavel_apos_hashes']=((HF $c.freezeFinal.arquivo) -ceq $freezeAntes)
    $checks['backend.fonte31_final_intacta']=((HF 'docs/31-fechamento-contagem-e-contingencia.md') -ceq $c.freezeFinal.doc31Sha256)
    . (Join-Path $raiz 'infra/leitores/identidade-ciclos-preparacao.ps1')
    $atual=JF $c.identidadeCiclos.atual.origem;$historico=JF $c.identidadeCiclos.historico.origem
    $checks['atual.freezeFinal_identico_antes456']=(($c.freezeFinal|ConvertTo-Json -Depth 12) -ceq ($atual.freezeFinal|ConvertTo-Json -Depth 12))
    $checks['atual.artefato_so_acrescenta_snapshot']=(@($c.artefatoFinal.PSObject.Properties.Name|Where-Object {$_ -notin @($atual.artefatoFinal.PSObject.Properties.Name) -and $_ -cne 'snapshot'}).Count -eq 0)
    foreach($prop in $atual.artefatoFinal.PSObject.Properties){$checks[('atual.artefato_preservado.'+$prop.Name)]=($c.artefatoFinal.($prop.Name) -ceq $prop.Value)}
    $checks['atual.resultados_identicos_antes456']=(($c.evidenciasJpaFinal|ConvertTo-Json -Depth 12) -ceq ($atual.evidenciasJpaFinal|ConvertTo-Json -Depth 12))
    $checks['historico.freeze_exato_origem360']=(($c.historicoArtefatos[0].freeze|ConvertTo-Json -Depth 12) -ceq ($historico.freezeFinal|ConvertTo-Json -Depth 12))
    foreach($prop in $historico.artefatoFinal.PSObject.Properties){$checks[('historico.artefato_exato.'+$prop.Name)]=($c.historicoArtefatos[0].artefato.($prop.Name) -ceq $prop.Value)}
    $checks['historico.resultados_exatos_origem360']=(($c.historicoArtefatos[0].evidenciasJpa|ConvertTo-Json -Depth 12) -ceq ($historico.evidenciasJpaFinal|ConvertTo-Json -Depth 12))
    $checks['guarda68_integral']=((ConferirIdentidadesWmsCiclos -raiz $raiz -c $c).divergencias.Count -eq 0)
    $fixture=JF ($prefixo+'-fixtures-final-output.json');$diag=JF ($prefixo+'-diagnostico-final-output.json')
    $checks['fixtures18_integral_12_recusas']=($fixture.total -eq 18 -and $fixture.aprovados -eq 18 -and @($fixture.casos).Count -eq 12 -and @($fixture.divergencias).Count -eq 0)
    $checks['diagnostico306_integral_original56']=($diag.total -eq 306 -and $diag.atendidos -eq 306 -and $diag.falhas -eq 0 -and $diag.original56.total -eq 56 -and $diag.original56.atendidos -eq 56)
    $checks['diagnostico68_identidades_ciclos']=(@($diag.conferencias.PSObject.Properties|Where-Object {$_.Name.StartsWith('identidade.')}).Count -eq 68 -and @($diag.identidadeCiclos).Count -eq 2)
    foreach($etapa in @('fixtures-final','diagnostico-final')){
        $e=JF ($prefixo+'-'+$etapa+'-execucao.json')
        $checks[('metadados.'+$etapa+'.normal_null_sem_saida0')]=($e.powershellSuccess -and $null -eq $e.lastExitCode)
        $checks[('metadados.'+$etapa+'.output_hash')]=((HF $e.output) -ceq $e.sha256)
    }
    $ini=JF ($prefixo+'-contrato-inicial-output.json');$mi=JF ($prefixo+'-contrato-inicial-execucao.json')
    $checks['tentativa1_codigo1_preservado_sem_escrita']=($ini.status -ceq 'BLOQUEADO_IDENTIDADE_CAMINHO' -and $ini.etapa -ceq 'pins' -and $mi.lastExitCode -eq 1)
    $jsons=@('infra/contratos/preparacao-final-local.json')+@(Get-ChildItem -LiteralPath (Join-Path $raiz 'infra/evidencias') -Filter 'd19-correcao-identidade-ciclos-2026-10-06*.json' -File|ForEach-Object {'infra/evidencias/'+$_.Name})
    foreach($p in $jsons){[void](JF $p);$checks[('json.valido.'+$p)]=$true}
    $scripts=@('infra/leitores/identidade-ciclos-preparacao.ps1','infra/verificar-identidade-ciclos-fixtures.ps1','infra/verificar-preparacao-final-local.ps1',($prefixo+'-corrigir-contrato.ps1'),($prefixo+'-fecho.ps1'))
    foreach($p in $scripts){$tk=$null;$er=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $raiz $p),[ref]$tk,[ref]$er);$checks[('ast.valido.'+$p)]=(@($er).Count -eq 0)}
    $leitor=LF 'infra/verificar-preparacao-final-local.ps1'
    $checks['guarda.antes_codigo_corrente_original56']=($leitor.IndexOf('ConferirIdentidadesWmsCiclos') -lt $leitor.IndexOf('$gateFreeze=') -and $leitor.IndexOf('BLOQUEADO_IDENTIDADES_CICLOS_DIVERGENTES') -lt $leitor.IndexOf('$originalSaida='))
    $docs=@($mudados|Where-Object {$_ -match '\.md$'})+@($prefixo+'.md');$links=@()
    $produzidos=@(($prefixo+'-fecho-final-output.json'),($prefixo+'-fecho-final-execucao.json'),($prefixo+'-fecho.sha256'))
    foreach($p in $docs){foreach($m in [regex]::Matches((LF $p),'\[[^\]]*\]\((?<alvo>[^)]+)\)')){
        $alvo=$m.Groups['alvo'].Value.Trim('<','>') -replace '#.*$','';if(-not $alvo -or $alvo -match '^[a-z]+://'){continue}
        $abs=[IO.Path]::GetFullPath((Join-Path (Split-Path (Join-Path $raiz $p)) $alvo))
        if(-not $abs.StartsWith($raiz+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'BLOQUEADO_LINK_FORA_PROJETO'}
        $rel=$abs.Substring($raiz.Length+1).Replace('\','/')
        $links+=[pscustomobject]@{arquivo=$p;alvo=$alvo;existe=(Test-Path -LiteralPath $abs);geradoNestaColeta=($rel -in $produzidos)}
    }}
    $checks['links.existentes_ou_tres_produzidos_nesta_coleta']=(@($links|Where-Object {-not $_.existe -and -not $_.geradoNestaColeta}).Count -eq 0)
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{formato='wms-correcao-identidade-fecho-v1';capturadoEm=(Get-Date).ToString('o');total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div;checks=$checks;baseline456=[pscustomobject]@{manifesto=$a.manifesto;sha256=$a.manifestoSha256;preservados=$iguais;alteradosComCopia=$mudados.Count;fontes=$preservacao};migrations=$migrations;freezeBackend289=$fontes;links=$links;arquivosNovosOuAtualizados=@($mudados)+$scripts;limite='Hashes/JSON/AST/links e resultados existentes em leitura. Nao repete JPA/suplemento nem executa JVM/SQL/build/rede/ambiente; aceite Farol/Vigia.'}|ConvertTo-Json -Depth 14
    if($div.Count){exit 1}
} catch {
    $codigo='BLOQUEADO_FECHO_IDENTIDADE';if($_.Exception.Message -match '^BLOQUEADO_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{status=$codigo;sem_raw=$true}|ConvertTo-Json
    exit 1
}
