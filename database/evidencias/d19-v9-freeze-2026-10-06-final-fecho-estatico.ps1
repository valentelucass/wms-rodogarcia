# Fecho de arquivos historicos. Nao compara Java em escrita nem executa os leitores de backend.
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '../..'))
$p='database/evidencias/d19-v9-freeze-2026-10-06-final'
$utf8=New-Object Text.UTF8Encoding($false,$true)
$checks=[ordered]@{};$inicio=(Get-Date).ToString('o')
function HashFecho([string]$f){(Get-FileHash -LiteralPath (Join-Path $script:raiz $f) -Algorithm SHA256 -ErrorAction Stop).Hash}
function LerFecho([string]$f){[IO.File]::ReadAllText((Join-Path $script:raiz $f),$script:utf8)}
function NovoFecho([string]$f,$v,[switch]$Texto){$path=Join-Path $script:raiz $f;if(Test-Path -LiteralPath $path){throw 'DESTINO_EXISTENTE_PRESERVADO'};$s=if($Texto){[string]$v}else{$v|ConvertTo-Json -Depth 25};[IO.File]::WriteAllText($path,$s,(New-Object Text.UTF8Encoding($false)))}
try{
    $antes=(LerFecho ($p+'-antes.json'))|ConvertFrom-Json
    $checks['antes.freeze289_exatos']=($antes.freeze.total -eq 289 -and @($antes.freeze.fontes|Where-Object {-not $_.confere}).Count -eq 0)
    $checks['antes.baseline333_exatos']=(@($antes.manifesto333.fontes).Count -eq 333 -and @($antes.manifesto333.fontes|Where-Object {-not $_.confere}).Count -eq 0)
    $checks['manifesto333_original_preservado']=((HashFecho $antes.manifesto333.arquivo) -ceq $antes.manifesto333.sha256)
    $checks['manifesto307_original_preservado']=((HashFecho $antes.manifesto307.arquivo) -ceq $antes.manifesto307.sha256)
    $migrations=@();foreach($m in $antes.migrations){$h=HashFecho $m.arquivo;$igual=$h -ceq $m.sha256;$checks[('migration.preservada.'+$m.arquivo)]=$igual;$migrations+=[pscustomobject]@{arquivo=$m.arquivo;antes=$m.sha256;depois=$h;preservada=$igual}}
    $checks['nove_migrations_preservadas']=($migrations.Count -eq 9 -and @($migrations|Where-Object {-not $_.preservada}).Count -eq 0)
    $c=(LerFecho 'database/contratos/v9-schema-doc31.json')|ConvertFrom-Json
    $cAntes=(LerFecho ($p+'-antes-database-contratos-v9-schema-doc31.json'))|ConvertFrom-Json
    foreach($n in @('tabelas','indices','indicesFiltrados','auditoria','paresEstadoDados','servicoSituacao')){$checks[('estrutura_transcricao_intacta.'+$n)]=(($c.$n|ConvertTo-Json -Depth 24) -ceq ($cAntes.$n|ConvertTo-Json -Depth 24))}
    $checks['fonte_snapshot_final_exata']=((HashFecho $c.fonteSnapshot) -ceq 'B45954EF4E79346657F9406BF788FF5CB3432B951F37BF817381E30C36180E7B')
    $checks['fase_p2_nao_aceita']=($c.jpa.fase -cmatch 'HISTORICO.*AGUARDA_NOVA_TAREFA')
    $leitor=(LerFecho ($p+'-leitor-reconciliado-final.json'))|ConvertFrom-Json;$sup=(LerFecho ($p+'-suplemento-guardas-final.json'))|ConvertFrom-Json;$fixture=(LerFecho ($p+'-fixtures-reconciliado-final.json'))|ConvertFrom-Json
    $checks['output904_historico_integral']=($leitor.totalChecks -eq 904 -and $leitor.aprovados -eq 904 -and @($leitor.divergencias).Count -eq 0)
    $checks['output122_historico_integral']=($sup.total -eq 122 -and $sup.aprovados -eq 122 -and @($sup.divergencias).Count -eq 0)
    $checks['fixture37_integral']=($fixture.total -eq 37 -and $fixture.aprovados -eq 37)
    $checks['transcricao904_reconstituida_hash_exato']=((HashFecho ($p+'-transcricao-usada904-reconstituida.json')) -ceq $leitor.transcricaoSha256)
    $checks['guardas122_reconstituidas_hash_exato']=((HashFecho ($p+'-guardas-usadas122-reconstituidas.json')) -ceq $sup.regrasSha256)
    foreach($s in @('leitor-reconciliado-final','suplemento-guardas-final','fixtures-reconciliado-final')){$e=(LerFecho ($p+'-'+$s+'-execucao.json'))|ConvertFrom-Json;$checks[('metadata.'+$s+'.null_nao_saida0')]=($e.powershellSuccess -and $null -eq $e.lastExitCode -and $e.conclusao -match 'nulo nao comprova');$checks[('metadata.'+$s+'.output_hash')]=((HashFecho $e.output) -ceq $e.outputSha256)}
    $artefato=(LerFecho 'infra/evidencias/d19-be14-360-historico-2026-10-06-artefato-output.json')|ConvertFrom-Json
    $checks['artefato52_historico_semJavaAtual']=($artefato.total -eq 52 -and $artefato.aprovados -eq 52 -and $artefato.semLeituraJavaAtual -and -not $artefato.artefato.executado)
    $jar='infra/evidencias/d19-be14-360-historico-2026-10-06-jar-snapshot.jar';$checks['jar_snapshot_829A_78524597']=((HashFecho $jar) -ceq '829A59B6CA60AF314089D1A0D303603FF3CACEEDAB4076F47608D7B513B3BA13' -and (Get-Item -LiteralPath (Join-Path $raiz $jar)).Length -eq 78524597)
    $tecnico=(LerFecho 'infra/contratos/preparacao-final-local.json')|ConvertFrom-Json
    $checks['original56_leitor_byte_preservado']=((HashFecho $tecnico.leitor56.arquivo) -ceq $tecnico.leitor56.sha256)
    $checks['original56_output_byte_preservado']=((HashFecho $tecnico.leitor56.output) -ceq $tecnico.leitor56.outputSha256)
    $ps=@('database/verificar-schema-v9.ps1','database/verificar-v9-freeze-suplementar.ps1','database/leitores/v9-java-arquivos.ps1','database/leitores/v9-comparar-jpa.ps1','database/verificar-parser-v9-fixtures.ps1','infra/verificar-preparacao-final-local.ps1','infra/verificar-artefato-historico-local.ps1',($p+'-coletar.ps1'),($p+'-fecho-estatico.ps1'))
    foreach($f in $ps){$tokens=$null;$erros=$null;[void][Management.Automation.Language.Parser]::ParseFile((Join-Path $raiz $f),[ref]$tokens,[ref]$erros);$checks[('ast.sem_erro.'+$f)]=(@($erros).Count -eq 0)}
    foreach($f in @('database/contratos/v9-schema-doc31.json','database/contratos/v9-guardas-leitura.json','infra/contratos/preparacao-final-local.json')){[void]((LerFecho $f)|ConvertFrom-Json);$checks[('json.valido.'+$f)]=$true}
    $main=LerFecho 'database/verificar-schema-v9.ps1';$suplemento=LerFecho 'database/verificar-v9-freeze-suplementar.ps1';$diag=LerFecho 'infra/verificar-preparacao-final-local.ps1'
    $checks['gate_main_antes_models']=($main.IndexOf('BLOQUEADO_FREEZE_HISTORICO_P2_LOCKS') -ge 0 -and $main.IndexOf('BLOQUEADO_FREEZE_HISTORICO_P2_LOCKS') -lt $main.IndexOf('$sqlPath'))
    $checks['gate_suplemento_antes_fonteJava']=($suplemento.IndexOf('BLOQUEADO_FREEZE_HISTORICO_P2_LOCKS') -lt $suplemento.IndexOf('$fonte=V9LerCongelado'))
    $checks['gate_tecnico_antes_diagnostico56']=($diag.IndexOf('FREEZE_HISTORICO_RECONFERENCIA_PENDENTE') -lt $diag.IndexOf('$original='))
    $checks['manifesto_exato_gates_presentes']=($main.Contains('BLOQUEADO_MANIFESTO_DIFERENTE_DO_CONTRATO') -and $suplemento.Contains('BLOQUEADO_MANIFESTO_DIFERENTE_DO_CONTRATO'))
    $docs=@('database/procedimento-v9.md','database/contratos/v9-leitor-jpa.md','database/contratos/v9-matriz-cobertura.md','database/README.md','database/migrations/README.md','database/permissoes-minimas.md','infra/README.md','infra/preparacao-tecnica-final.md','docs/33-preparacao-tecnica-local-backend.md',($p+'-historico-p2-locks.md'),'infra/evidencias/d19-be14-360-historico-2026-10-06.md')
    $links=@();foreach($d in $docs){$texto=LerFecho $d;foreach($m in [regex]::Matches($texto,'\[[^\]]*\]\((?<alvo>[^)]+)\)')){$alvo=$m.Groups['alvo'].Value.Trim('<','>') -replace '#.*$','';if(-not $alvo -or $alvo -match '^[a-z]+://'){continue};$abs=[IO.Path]::GetFullPath((Join-Path (Split-Path (Join-Path $raiz $d)) $alvo));$links+=[pscustomobject]@{arquivo=$d;alvo=$alvo;existe=(Test-Path -LiteralPath $abs)}}}
    $checks['links_locais_existentes']=(@($links|Where-Object {-not $_.existe}).Count -eq 0)
    $protegidos=@();$mudados=@();foreach($f in $antes.manifesto333.fontes){$h=HashFecho $f.arquivo;if($h -ceq $f.antes){$protegidos+=[pscustomobject]@{arquivo=$f.arquivo;sha256=$h}}else{$cop=@($antes.copias|Where-Object {$_.arquivo -ceq $f.arquivo -and $_.sha256 -ceq $f.antes});$ok=$cop.Count -eq 1 -and (HashFecho $cop[0].copia) -ceq $f.antes;$checks[('alteracao_autorizada_com_copia.'+$f.arquivo)]=$ok;$mudados+=[pscustomobject]@{arquivo=$f.arquivo;antes=$f.antes;depois=$h;copia=if($cop.Count){$cop[0].copia}else{$null};copiaExata=$ok}}}
    # Apenas HASH de arquivos correntes, sem ler/comparar Java/doc31 em escrita.
    $fontes=@();foreach($f in $antes.freeze.fontes){$h=HashFecho $f.arquivo;$fontes+=[pscustomobject]@{arquivo=$f.arquivo;freezeHistorico=$f.antes;hashAtualCapturado=$h;igualHistorico=($h -ceq $f.antes);pareamentoAtual='NAO_EXECUTADO_AGUARDA_NOVA_TAREFA_P2_LOCKS'}}
    $checks['manifesto_backend_historico_bytes_preservados']=((HashFecho $antes.freeze.arquivo) -ceq $antes.freeze.sha256)
    $div=@($checks.Keys|Where-Object {-not $checks[$_]})
    $depois=[pscustomobject]@{capturadoEm=(Get-Date).ToString('o');natureza='Preservacao/localQA, nao reconferencia backend final';migrations=$migrations;baseline333=[pscustomobject]@{iguais=$protegidos.Count;alteradosAutorizados=$mudados.Count;alteracoes=$mudados;protegidos=$protegidos};fontes289Historicas=[pscustomobject]@{iguaisAgora=@($fontes|Where-Object igualHistorico).Count;deltasExternosEmEscrita=@($fontes|Where-Object {-not $_.igualHistorico}).Count;fontes=$fontes;aceiteRetidoP2=$true};limites='Hashes atuais nao aceitam/reconciliam codigo em escrita; nova tarefa/manifeso p2-locks necessarios.'}
    NovoFecho ($p+'-depois-historico.json') $depois
    NovoFecho ($p+'-checks-locais-historico.json') ([pscustomobject]@{capturadoEm=(Get-Date).ToString('o');total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div;checks=$checks;linksQuantidade=$links.Count;links=$links;nenhumLeitorBackendExecutado=$true;semSqlJvmH2BuildRedeAmbiente=$true})
    NovoFecho ($p+'-fecho-metadados.json') ([pscustomobject]@{inicio=$inicio;fim=(Get-Date).ToString('o');comando='./database/evidencias/d19-v9-freeze-2026-10-06-final-fecho-estatico.ps1';natureza='Somente AST/JSON/links/hashes; nenhum comparador backend executado';conclusao=if($div.Count){'Checks locais com divergencia; output preservado'}else{'Conclusao PowerShell normal; nao infere codigo nativo0 do LASTEXITCODE dos leitores'};semSqlJvmBuildRedeAmbiente=$true})
    if($div.Count){[pscustomobject]@{total=$checks.Count;aprovados=$checks.Count-$div.Count;divergencias=$div}|ConvertTo-Json;exit 1}
    $arquivos=@($antes.manifesto333.fontes.arquivo)+@($antes.manifesto333.arquivo,$antes.manifesto307.arquivo)
    foreach($pasta in @('database/evidencias','infra/evidencias')){$arquivos+=@(Get-ChildItem -LiteralPath (Join-Path $raiz $pasta) -File|Where-Object {$_.Name -like 'd19-v9-freeze-2026-10-06-final*' -or $_.Name -like 'd19-be14-360-historico-2026-10-06*'}|ForEach-Object {$pasta+'/'+$_.Name})}
    $arquivos+=@('infra/verificar-artefato-historico-local.ps1')
    $linhas=@($arquivos|Sort-Object -Unique|ForEach-Object {(HashFecho $_)+'  '+$_})
    $manifesto='database/evidencias/d19-v9-2026-10-06-historico360-p2-pendente.sha256';NovoFecho $manifesto ($linhas -join "`n") -Texto
    [pscustomobject]@{total=$checks.Count;aprovados=$checks.Count;links=$links.Count;migrationsPreservadas=$migrations.Count;baseline333Iguais=$protegidos.Count;alteradosComCopia=$mudados.Count;deltasBackendEmEscrita=@($fontes|Where-Object {-not $_.igualHistorico}).Count;manifestoArquivos=$linhas.Count;manifestoSha256=HashFecho $manifesto;aceite='RETIDO_P2_LOCKS';comparacaoFinal='AGUARDA_NOVA_TAREFA'}|ConvertTo-Json
}catch{
    '{"bloqueio":"BLOQUEADO_FECHO_ARQUIVOS","rawExposto":false,"semSqlJvmBuildRedeAmbiente":true}'
    exit 1
}
