# Correcao local de metadados. Copias de origem fixadas antes de qualquer escrita.
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
. (Join-Path $raiz 'infra/leitores/identidade-ciclos-preparacao.ps1')
$etapa='origens'
try {
    $origem360='database/evidencias/d19-v9-2026-10-06-p2-locks-final-antes-infra-contratos-preparacao-final-local.json'
    $origem367='infra/evidencias/d19-correcao-identidade-ciclos-2026-10-06-antes-infra-contratos-preparacao-final-local.json'
    $anterior=(LerIdentidadeWms -raiz $raiz -p $origem360)|ConvertFrom-Json
    $atual=(LerIdentidadeWms -raiz $raiz -p $origem367)|ConvertFrom-Json
    $c=CopiarIdentidadeWmsProfunda $atual
    $etapa='clone360'
    $hist=[pscustomobject]@{
        id='BE14-360'
        freeze=(CopiarIdentidadeWmsProfunda $anterior.freezeFinal)
        artefato=(CopiarIdentidadeWmsProfunda $anterior.artefatoFinal)
        evidenciasJpa=@(CopiarIdentidadeWmsProfunda $anterior.evidenciasJpaFinal)
        origem=$origem360
        origemSha256=(HashIdentidadeWms -raiz $raiz -p $origem360)
        natureza='Historico360/904/122/52/411 restaurado por copia profunda da origem preservada; aceite360 retido. Registro incorreto do456 preservado no snapshot anterior desta correcao.'
    }
    $hist.artefato|Add-Member -NotePropertyName snapshot -NotePropertyValue 'infra/evidencias/d19-be14-360-historico-2026-10-06-jar-snapshot.jar'
    $hist.artefato|Add-Member -NotePropertyName testes -NotePropertyValue 360
    $c.historicoArtefatos=@($hist)
    $c.artefatoFinal|Add-Member -NotePropertyName snapshot -NotePropertyValue 'infra/evidencias/d19-preparacao-p2-locks367-2026-10-06-jar-snapshot.jar' -Force
    $etapa='pins'
    $pins367=@(foreach($e in $c.evidenciasJpaFinal){[pscustomobject]@{arquivo=$e.arquivo;sha256=(HashIdentidadeWms -raiz $raiz -p $e.arquivo)}})
    $pins360=@(foreach($e in $hist.evidenciasJpa){[pscustomobject]@{arquivo=$e.arquivo;sha256=(HashIdentidadeWms -raiz $raiz -p $e.arquivo)}})
    $c|Add-Member -NotePropertyName identidadeCiclos -NotePropertyValue ([pscustomobject]@{
        versao=1
        atual=[pscustomobject]@{origem=$origem367;origemSha256=(HashIdentidadeWms -raiz $raiz -p $origem367);jarSnapshot=$c.artefatoFinal.snapshot;testes=367;evidencias=$pins367}
        historico=[pscustomobject]@{origem=$origem360;origemSha256=(HashIdentidadeWms -raiz $raiz -p $origem360);jarSnapshot=$hist.artefato.snapshot;testes=360;evidencias=$pins360}
    }) -Force
    $etapa='verificacao_pre_escrita'
    $resultado=ConferirIdentidadesWmsCiclos -raiz $raiz -c $c
    if($resultado.divergencias.Count){throw 'BLOQUEADO_IDENTIDADES_CICLOS_DIVERGENTES'}
    $etapa='escrita'
    [IO.File]::WriteAllText((Join-Path $raiz 'infra/contratos/preparacao-final-local.json'),($c|ConvertTo-Json -Depth 28),(New-Object Text.UTF8Encoding($false)))
    [pscustomobject]@{status='CONTRATO_CORRIGIDO';etapa=$etapa;total=$resultado.checks.Count;aprovados=$resultado.checks.Count;divergencias=@();historico360=$resultado.resumo[1];atual367=$resultado.resumo[0];sem_sql_jvm_jpa_env_rede=$true}|ConvertTo-Json -Depth 8
} catch {
    $codigo='BLOQUEADO_CORRECAO_IDENTIDADE';if($_.Exception.Message -match '^BLOQUEADO_[A-Z0-9_]+$'){$codigo=$_.Exception.Message}
    [pscustomobject]@{status=$codigo;etapa=$etapa;divergencias=if($resultado){$resultado.divergencias}else{@()};sem_raw=$true}|ConvertTo-Json -Depth 4
    exit 1
}
