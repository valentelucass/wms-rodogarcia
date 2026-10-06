# Regressao do alias documental: apenas objetos em memoria e arquivos fixados.
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
. (Join-Path $raiz 'infra/leitores/identidade-ciclos-preparacao.ps1')
$checks=[ordered]@{};$casos=@()
function RecusaIdentidade([string]$nome,$candidato) {
    try {
        $v=ConferirIdentidadesWmsCiclos -raiz $script:raiz -c $candidato
        $recusado=($v.divergencias.Count -gt 0)
        $script:checks[$nome]=$recusado
        $script:casos+=[pscustomobject]@{caso=$nome;recusado=$recusado;codigo=if($recusado){'BLOQUEADO_IDENTIDADES_CICLOS_DIVERGENTES'}else{'MISTURA_NAO_RECUSADA'};divergencias=$v.divergencias}
    } catch {
        $codigo='BLOQUEADO_LEITURA_OU_FORMATO';if($_.Exception.Message -match '^BLOQUEADO_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
        $script:checks[$nome]=($codigo -ne 'BLOQUEADO_LEITURA_OU_FORMATO')
        $script:casos+=[pscustomobject]@{caso=$nome;recusado=$true;codigo=$codigo;divergencias=@()}
    }
}
try {
    $c=(LerIdentidadeWms -raiz $raiz -p 'infra/contratos/preparacao-final-local.json')|ConvertFrom-Json
    $controle=ConferirIdentidadesWmsCiclos -raiz $raiz -c $c
    $checks['controle.68_identidades_integras']=($controle.checks.Count -eq 68 -and $controle.divergencias.Count -eq 0)
    # Reproduzir a construcao antiga: ambos apontam o MESMO objeto em memoria.
    $raso=CopiarIdentidadeWmsProfunda $c
    $legado=[pscustomobject]@{freeze=$raso.freezeFinal;artefato=$raso.artefatoFinal}
    $checks['alias_legado.referencia_compartilhada']=([object]::ReferenceEquals($raso.freezeFinal,$legado.freeze) -and [object]::ReferenceEquals($raso.artefatoFinal,$legado.artefato))
    $raso.freezeFinal.sha256='MUTACAO_EM_MEMORIA';$raso.artefatoFinal.bytes=1
    $checks['alias_legado.mutacao_contamina_historico']=($legado.freeze.sha256 -ceq 'MUTACAO_EM_MEMORIA' -and $legado.artefato.bytes -eq 1)
    $profundo=CopiarIdentidadeWmsProfunda $c
    $protegido=[pscustomobject]@{freeze=(CopiarIdentidadeWmsProfunda $profundo.freezeFinal);artefato=(CopiarIdentidadeWmsProfunda $profundo.artefatoFinal);evidencias=@(CopiarIdentidadeWmsProfunda $profundo.evidenciasJpaFinal)}
    $profundo.freezeFinal.sha256='MUTACAO_EM_MEMORIA';$profundo.artefatoFinal.bytes=1;$profundo.evidenciasJpaFinal[0].arquivo='MUTACAO_EM_MEMORIA'
    $checks['clone.freeze_independente']=($protegido.freeze.sha256 -ceq $c.freezeFinal.sha256 -and -not [object]::ReferenceEquals($profundo.freezeFinal,$protegido.freeze))
    $checks['clone.jar_independente']=($protegido.artefato.bytes -eq $c.artefatoFinal.bytes -and -not [object]::ReferenceEquals($profundo.artefatoFinal,$protegido.artefato))
    $checks['clone.resultados_independentes_lista_plana']=($protegido.evidencias.Count -eq 2 -and $protegido.evidencias[0].arquivo -ceq $c.evidenciasJpaFinal[0].arquivo)
    $antes=(LerIdentidadeWms -raiz $raiz -p $c.identidadeCiclos.atual.origem)|ConvertFrom-Json
    $antes|Add-Member -NotePropertyName identidadeCiclos -NotePropertyValue (CopiarIdentidadeWmsProfunda $c.identidadeCiclos) -Force
    RecusaIdentidade 'recusa.registro456_original_misturado' $antes
    # Dar apenas ID/snapshot ao legado permite comprovar os pares, alem da forma.
    $antes.historicoArtefatos[0]|Add-Member -NotePropertyName id -NotePropertyValue 'BE14-360' -Force
    $antes.historicoArtefatos[0].artefato|Add-Member -NotePropertyName snapshot -NotePropertyValue $c.artefatoFinal.snapshot -Force
    RecusaIdentidade 'recusa.mistura456_com_id' $antes
    $x=CopiarIdentidadeWmsProfunda $c;$x.historicoArtefatos[0].freeze.arquivo=$c.freezeFinal.arquivo
    RecusaIdentidade 'recusa.manifesto367_no_historico360' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.historicoArtefatos[0].freeze.doc31Sha256=$c.freezeFinal.doc31Sha256
    RecusaIdentidade 'recusa.fonte367_no_historico360' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.historicoArtefatos[0].artefato.sha256=$c.artefatoFinal.sha256;$x.historicoArtefatos[0].artefato.bytes=$c.artefatoFinal.bytes
    RecusaIdentidade 'recusa.jar367_no_historico360' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.historicoArtefatos[0].evidenciasJpa[0].arquivo=$c.evidenciasJpaFinal[0].arquivo
    RecusaIdentidade 'recusa.output904367_no_historico904360' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.historicoArtefatos[0].evidenciasJpa[1].arquivo=$c.evidenciasJpaFinal[1].arquivo
    RecusaIdentidade 'recusa.suplemento129_no_historico122' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.freezeFinal.sha256=$c.historicoArtefatos[0].freeze.sha256
    RecusaIdentidade 'recusa.manifesto360_no_atual367' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.identidadeCiclos.historico.jarSnapshot=$c.identidadeCiclos.atual.jarSnapshot
    RecusaIdentidade 'recusa.snapshot_jar367_no_historico360' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.historicoArtefatos[0].artefato.resumo=$c.artefatoFinal.resumo
    RecusaIdentidade 'recusa.resumo367_no_historico360' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.identidadeCiclos.historico.origemSha256=('0'*64)
    RecusaIdentidade 'recusa.origem_sem_hash_exato' $x
    $x=CopiarIdentidadeWmsProfunda $c;$x.PSObject.Properties.Remove('identidadeCiclos')
    RecusaIdentidade 'recusa.ancoras_ausentes' $x
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    [pscustomobject]@{formato='wms-identidade-ciclos-fixtures-v1';capturadoEm=(Get-Date).ToString('o');total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div;checks=$checks;casos=$casos;controle=$controle.resumo;limite='Objetos e arquivos somente; nao executa comparacao JPA/JVM/SQL/build/env/rede nem valida regras/locks.'}|ConvertTo-Json -Depth 10
    if($div.Count){exit 1}
} catch {
    $codigo='BLOQUEADO_FIXTURE_IDENTIDADE';if($_.Exception.Message -match '^BLOQUEADO_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{status=$codigo;sem_raw=$true}|ConvertTo-Json
    exit 1
}
