# Definicoes locais; nao le arquivos ao carregar, nao executa backend/SQL/env/rede.
function CopiarIdentidadeWmsProfunda($objeto) {
    # Atribuir antes de retornar enumera uma lista JSON uma vez, sem array aninhado.
    $copia=$objeto|ConvertTo-Json -Depth 28|ConvertFrom-Json
    return $copia
}
function LerIdentidadeWms([string]$raiz,[string]$p) {
    if($p -notmatch '^(?:infra|database|backend)/[\w./-]+$' -or $p -match '(?:^|/)\.\.(?:/|$)'){throw 'BLOQUEADO_IDENTIDADE_CAMINHO'}
    [IO.File]::ReadAllText((Join-Path $raiz $p),(New-Object Text.UTF8Encoding($false,$true)))
}
function HashIdentidadeWms([string]$raiz,[string]$p) {
    if($p -notmatch '^(?:infra|database|backend)/[\w./-]+$' -or $p -match '(?:^|/)\.\.(?:/|$)'){throw 'BLOQUEADO_IDENTIDADE_CAMINHO'}
    (Get-FileHash -LiteralPath (Join-Path $raiz $p) -Algorithm SHA256 -ErrorAction Stop).Hash
}
function ConferirIdentidadesWmsCiclos([string]$raiz,$c) {
    $checks=[ordered]@{};$resumo=@()
    if($null -eq $c.identidadeCiclos -or $c.identidadeCiclos.versao -ne 1){throw 'BLOQUEADO_IDENTIDADE_ANCORAS_AUSENTES'}
    $hist=@($c.historicoArtefatos|Where-Object {$_.id -ceq 'BE14-360'})
    $checks['historico360.registro_unico']=($hist.Count -eq 1 -and @($c.historicoArtefatos).Count -eq 1)
    if(-not $checks['historico360.registro_unico']){return [pscustomobject]@{checks=$checks;divergencias=@('historico360.registro_unico');resumo=@()}}
    $alvos=@([pscustomobject]@{id='atual367';meta=$c.identidadeCiclos.atual;freeze=$c.freezeFinal;artefato=$c.artefatoFinal;evidencias=@($c.evidenciasJpaFinal)},[pscustomobject]@{id='historico360';meta=$c.identidadeCiclos.historico;freeze=$hist[0].freeze;artefato=$hist[0].artefato;evidencias=@($hist[0].evidenciasJpa)})
    foreach($alvo in $alvos) {
        $id=$alvo.id;$meta=$alvo.meta
        if($meta.origem -notmatch '^(?:infra|database)/evidencias/[\w.-]+\.json$'){throw 'BLOQUEADO_IDENTIDADE_ORIGEM'}
        $hashOrigem=HashIdentidadeWms $raiz $meta.origem
        $checks[($id+'.origem_hash_exato')]=($hashOrigem -ceq $meta.origemSha256)
        if(-not $checks[($id+'.origem_hash_exato')]){throw 'BLOQUEADO_IDENTIDADE_ORIGEM_MUDOU'}
        $origem=(LerIdentidadeWms $raiz $meta.origem)|ConvertFrom-Json
        $f=$origem.freezeFinal;$a=$origem.artefatoFinal;$e=@($origem.evidenciasJpaFinal)
        foreach($campo in @('arquivo','sha256','doc31Sha256','estado','fonteSnapshot')) {$checks[($id+'.freeze.'+$campo)]=($alvo.freeze.$campo -ceq $f.$campo)}
        foreach($campo in @('diretorio','nome','bytes','sha256','resumo','log','xmls')) {$checks[($id+'.artefato.'+$campo)]=($alvo.artefato.$campo -ceq $a.$campo)}
        $checks[($id+'.manifesto_arquivo_hash')]=((HashIdentidadeWms $raiz $f.arquivo) -ceq $f.sha256)
        $checks[($id+'.fonte_snapshot_hash')]=((HashIdentidadeWms $raiz $f.fonteSnapshot) -ceq $f.doc31Sha256)
        $manifesto=LerIdentidadeWms $raiz $f.arquivo
        $linha=[regex]::Match($manifesto,'(?m)^([A-Fa-f0-9]{64})\s+docs/31-fechamento-contagem-e-contingencia\.md\s*$')
        $checks[($id+'.manifesto_fonte31_pareada')]=($linha.Success -and $linha.Groups[1].Value.ToUpperInvariant() -ceq $f.doc31Sha256)
        $checks[($id+'.evidencias_quantidade')]=($alvo.evidencias.Count -eq $e.Count -and @($meta.evidencias).Count -eq $e.Count)
        foreach($esperada in $e) {
            $achada=@($alvo.evidencias|Where-Object {$_.tipo -ceq $esperada.tipo});$pin=@($meta.evidencias|Where-Object {$_.arquivo -ceq $esperada.arquivo})
            $nome=$id+'.resultado.'+$esperada.tipo
            $checks[($nome+'.identidade_exata')]=($achada.Count -eq 1 -and $achada[0].arquivo -ceq $esperada.arquivo -and $achada[0].total -eq $esperada.total -and $achada[0].estado -ceq $esperada.estado)
            $h=HashIdentidadeWms $raiz $esperada.arquivo
            $checks[($nome+'.output_hash')]=($pin.Count -eq 1 -and $h -ceq $pin[0].sha256)
            $v=(LerIdentidadeWms $raiz $esperada.arquivo)|ConvertFrom-Json
            $freezeResultado=if($esperada.tipo -ceq 'principal'){$v.comparacaoJpa.freeze}else{$v.freeze}
            $total=if($esperada.tipo -ceq 'principal'){$v.totalChecks}else{$v.total}
            $checks[($nome+'.manifesto_pareado')]=($freezeResultado.arquivo -ceq $f.arquivo -and $freezeResultado.sha256 -ceq $f.sha256)
            $checks[($nome+'.contagem_integral')]=($total -eq $esperada.total -and $v.aprovados -eq $esperada.total -and @($v.divergencias).Count -eq 0)
            if($esperada.tipo -ceq 'principal'){$checks[($nome+'.fonte31_pareada')]=($v.fonteSha256 -ceq $f.doc31Sha256 -and $v.fonteCanonicaSha256 -ceq $f.doc31Sha256)}
        }
        $checks[($id+'.jar_snapshot_identificado')]=($meta.jarSnapshot -ceq $alvo.artefato.snapshot)
        $checks[($id+'.jar_snapshot_hash')]=((HashIdentidadeWms $raiz $meta.jarSnapshot) -ceq $a.sha256)
        $checks[($id+'.jar_snapshot_bytes')]=((Get-Item -LiteralPath (Join-Path $raiz $meta.jarSnapshot) -ErrorAction Stop).Length -eq $a.bytes)
        $resumoBuild=(LerIdentidadeWms $raiz $a.resumo)|ConvertFrom-Json
        $checks[($id+'.resumo_ciclo_testes')]=($resumoBuild.testes -eq $meta.testes -and $alvo.artefato.testes -eq $meta.testes -and $resumoBuild.quantidadeXml -eq 17 -and $resumoBuild.falhas -eq 0 -and $resumoBuild.erros -eq 0 -and $resumoBuild.ignorados -eq 0)
        $checks[($id+'.origem_estavel_apos_leituras')]=((HashIdentidadeWms $raiz $meta.origem) -ceq $hashOrigem)
        $resumo+=[pscustomobject]@{ciclo=$id;manifesto=$f.arquivo;sha256=$f.sha256;fonte31Sha256=$f.doc31Sha256;jarSnapshot=$meta.jarSnapshot;jarSha256=$a.sha256;bytes=$a.bytes;testes=$meta.testes;evidencias=@($e|Select-Object tipo,arquivo,total);limite='Identidade/proveniencia de arquivos; nao executa JPA/JAR nem aceita regra/locks.'}
    }
    $checks['ciclos.manifestos_distintos']=($c.freezeFinal.sha256 -cne $hist[0].freeze.sha256)
    $checks['ciclos.fontes31_distintas']=($c.freezeFinal.doc31Sha256 -cne $hist[0].freeze.doc31Sha256)
    $checks['ciclos.jar_hash_bytes_distintos']=($c.artefatoFinal.sha256 -cne $hist[0].artefato.sha256 -and $c.artefatoFinal.bytes -ne $hist[0].artefato.bytes)
    $checks['ciclos.objetos_freeze_independentes']=(-not [object]::ReferenceEquals($c.freezeFinal,$hist[0].freeze))
    $checks['ciclos.objetos_artefato_independentes']=(-not [object]::ReferenceEquals($c.artefatoFinal,$hist[0].artefato))
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{checks=$checks;divergencias=$div;resumo=$resumo}
}
