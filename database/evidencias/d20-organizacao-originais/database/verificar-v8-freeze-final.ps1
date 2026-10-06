# Suplemento BE13 somente em arquivos. Nao inicia Java/JPA/Hibernate/SQL/H2/build.
[CmdletBinding()]
param()
$ErrorActionPreference='Stop'
$raiz=[IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$contrato=Get-Content -LiteralPath (Join-Path $raiz 'database/contratos/v8-schema-doc31.json') -Raw -Encoding UTF8|ConvertFrom-Json
$fontes=@{};$hashes=@();$checks=[ordered]@{};$evidencias=@()
function Ler([string]$arquivo){
    if(-not $script:fontes.ContainsKey($arquivo)){
        $abs=Join-Path $raiz $arquivo
        $script:fontes[$arquivo]=Get-Content -LiteralPath $abs -Raw -Encoding UTF8
        $script:hashes+=[pscustomobject]@{arquivo=$arquivo;sha256=(Get-FileHash -LiteralPath $abs -Algorithm SHA256).Hash}
    }
    $script:fontes[$arquivo]
}
function Registrar([string]$nome,[bool]$ok,[string]$arquivo,[string]$trecho=''){
    $script:checks[$nome]=$ok
    $fonte=Ler $arquivo
    $indice=if($trecho){$fonte.IndexOf($trecho,[StringComparison]::Ordinal)}else{-1}
    $linha=if($indice -ge 0){[regex]::Matches($fonte.Substring(0,$indice),'\n').Count+1}else{$null}
    $script:evidencias+=[pscustomobject]@{check=$nome;atendido=$ok;arquivo=$arquivo;linha=$linha;trecho=$trecho;limite='Presenca/ordem lexical ou anotacao; nao prova execucao, transacao ou regra de negocio'}
}
function Metodo([string]$fonte,[string]$nome){
    $cabecalho=[regex]::Match($fonte,('\b(?:public|private|protected)\s+(?:static\s+)?[\w<>]+\s+'+[regex]::Escape($nome)+'\s*\([^;{}]*?\)\s*\{'),[Text.RegularExpressions.RegexOptions]::Singleline)
    if(-not $cabecalho.Success){return ''}
    $nivel=1;$literal=$false;$escape=$false;$inicio=$cabecalho.Index+$cabecalho.Length
    for($i=$inicio;$i -lt $fonte.Length;$i++){
        $ch=$fonte[$i]
        if($literal){
            if($escape){$escape=$false;continue}
            if($ch -eq '\'){$escape=$true;continue}
            if($ch -eq '"'){$literal=$false}
            continue
        }
        if($ch -eq '"'){$literal=$true;continue}
        if($ch -eq '/' -and $i+1 -lt $fonte.Length -and $fonte[$i+1] -eq '/'){
            $nl=$fonte.IndexOf([char]10,$i+2);if($nl -lt 0){return ''};$i=$nl;continue
        }
        if($ch -eq '{'){$nivel++}
        if($ch -eq '}'){$nivel--;if($nivel -eq 0){return $fonte.Substring($cabecalho.Index,$i-$cabecalho.Index+1)}}
    }
    ''
}
$declaracoesCheckJpa=0;$imutaveis=0;$relacoes=0
foreach($t in $contrato.tabelas){
    $arquivo='backend/src/main/java/br/com/rodogarcia/wms/models/'+$t.model+'.java'
    $fonte=Ler $arquivo
    $declaracoesCheckJpa+=[regex]::Matches($fonte,'@(?:org\.hibernate\.annotations\.)?Check\b').Count
    $campos=@{}
    foreach($m in [regex]::Matches($fonte,'(?<attrs>(?:@\w+(?:\((?:[^()"]|"[^"]*")*\))?\s*)+)\s*private\s+(?<java>\w+)\s+(?<field>\w+)\s*(?:=\s*[^;]+)?;')){
        $attrs=$m.Groups['attrs'].Value
        $col=[regex]::Match($attrs,'@(?:Column|JoinColumn)(?:\((?:[^()"]|"[^"]*")*\))?').Value
        $nome=[regex]::Match($col,'name\s*=\s*"([^"]+)"').Groups[1].Value
        if(-not $nome){$nome=[regex]::Replace($m.Groups['field'].Value,'[A-Z]',{param($x)'_'+$x.Value.ToLowerInvariant()})}
        $campos[$nome]=$m
    }
    foreach($c in $t.colunas){
        $campo=$campos[$c.nome];$attrs=if($campo){$campo.Groups['attrs'].Value}else{''}
        if($c.nome -notin @('id','versao') -and $c.nome -cnotin $t.mutaveis){
            $imutaveis++
            Registrar ($t.nome+'.'+$c.nome+'.updatable_false') ($null -ne $campo -and $attrs -match 'updatable\s*=\s*false') $arquivo $(if($campo){$campo.Value}else{''})
        }
        if($c.referencia){
            $relacoes++
            $nn=$attrs -match '@ManyToOne\(.*?optional\s*=\s*false'
            Registrar ($t.nome+'.'+$c.nome+'.optional_coerente_com_NULL') ($attrs -match '@ManyToOne\b' -and $nn -eq (-not [bool]$c.nulo)) $arquivo $(if($campo){$campo.Value}else{''})
        }
    }
}
$fs='backend/src/main/java/br/com/rodogarcia/wms/services/FechamentoCobrancaService.java'
$fm='backend/src/main/java/br/com/rodogarcia/wms/models/AjusteFechamento.java'
$servico=Ler $fs;$model=Ler $fm
$redirecionar=Metodo $model 'redirecionar';$aplicarModel=Metodo $model 'aplicar'
Registrar 'ajuste.redirecionar_guardado_por_VALIDADO_antes_da_atribuicao' ($redirecionar -match '(?s)if\s*\(!situacao\.equals\("VALIDADO"\)\).*?throw.*?destinoFechamento\s*=\s*destino') $fm $redirecionar
Registrar 'ajuste.aplicar_exige_destino_atual_e_VALIDADO' ($aplicarModel -match '(?s)!destinoFechamento\.getId\(\)\.equals\(versao\.getFechamento\(\)\.getId\(\)\).*?throw.*?!situacao\.equals\("VALIDADO"\).*?throw.*?situacao\s*=\s*"APLICADO"') $fm $aplicarModel
Registrar 'ajuste.aplicacao_guarda_versao_e_instante' ($aplicarModel -match '(?s)aplicadoVersao\s*=\s*versao;.*?aplicadoEm\s*=\s*agora;') $fm $aplicarModel
$aplicar=Metodo $servico 'aplicar';$composicao=Metodo $servico 'conferirComposicao';$tratar=Metodo $servico 'tratarExterno'
Registrar 'servico.aplicar_confere_composicao_e_destino_antes_model' ($aplicar -match '(?s)conferirComposicao\(v\).*?!a\.getDestinoFechamento\(\)\.getId\(\)\.equals\(v\.getFechamento\(\)\.getId\(\)\).*?throw.*?a\.aplicar\(v,') $fs $aplicar
Registrar 'servico.composicao_compara_lista_do_destino_atual' ($composicao -match '(?s)findByDestinoFechamentoIdOrderByIdAsc.*?findByVersaoIdOrderByIdAsc.*?!atual\.equals\(gravada\).*?throw') $fs $composicao
Registrar 'tratativa.historia_redirecionada_conserva_conflito' ($tratar -match '(?s)!a\.getDestinoFechamento\(\)\.getId\(\)\.equals\(f\.getId\(\)\).*?throw conflito\(\s*"COMPOSICAO_HISTORICA_REDIRECIONADA"') $fs $tratar
Registrar 'tratativa.aplicado_outra_versao_recusado' ($tratar -match '(?s)a\.getSituacao\(\)\.equals\("APLICADO"\).*?!a\.getAplicadoVersao\(\)\.getId\(\)\.equals\(alvo\.getId\(\)\).*?throw conflito\(\s*"APLICACAO_JA_FINALIZADA"') $fs $tratar
Registrar 'tratativa.destino_opcionais_obrigatorios_juntos' ($tratar -match '\(d\.destinoAjustesId\(\)\s*==\s*null\)\s*!=\s*\(d\.versaoDestinoAjustes\(\)\s*==\s*null\)') $fs $tratar
Registrar 'tratativa.posterior_contexto_e_primeiro_ciclo_aptos_presentes' ($tratar -match '(?s)findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc.*?!x\.getPeriodoInicio\(\)\.isBefore\(f\.getPeriodoFim\(\)\).*?destinos\.getFirst\(\)\.getId\(\)\.equals\(d\.destinoAjustesId\(\)\).*?buscarParaAtualizar\(d\.destinoAjustesId\(\)\).*?conferirVersaoLock\(destino') $fs $tratar
Registrar 'tratativa.redirecionamento_nova_versao_e_atribuicoes_auditadas' ($tratar -match '(?s)atribuicaoAnterior.*?a\.redirecionar\(destino\).*?novaVersao\(destino.*?"TRATATIVA_EXTERNA".*?new Object\[\]\s*\{antes,\s*atribuicaoAnterior,\s*dependenciasOrigem\}.*?new Object\[\]\s*\{\s*resultado,\s*destinoAlterado,\s*regularizacaoCriada,\s*origemRegularizada\s*\}') $fs $tratar
Registrar 'tratativa.aplica_so_VALIDADO_na_composicao' ($tratar -match '(?s)findByVersaoIdOrderByIdAsc\(alvo\.getId\(\)\).*?if\s*\(x\.getAjuste\(\)\.getSituacao\(\)\.equals\("VALIDADO"\)\)\s*x\.getAjuste\(\)\.aplicar\(alvo,') $fs $tratar
$bloquear=Metodo $servico 'bloquear';$ctxFile='backend/src/main/java/br/com/rodogarcia/wms/services/ContextoCobrancaService.java';$ctx=Ler $ctxFile;$bloqContexto=Metodo $ctx 'bloquear'
Registrar 'contexto.gestor_antes_bloqueio_e_conferencia_periodo' ($bloquear -match '(?s)acesso\.exigirGestor\(\).*?contextos\.bloquear.*?cadastros\.conferir.*?buscarParaAtualizar.*?cadastros\.conferirPeriodo') $fs $bloquear
Registrar 'contexto.CLIENTE_antes_ARMAZEM_declarado' ($bloqContexto -match '(?s)autorizar.*?clientes\.buscarParaAtualizar.*?armazens\.buscarParaAtualizar') $ctxFile $bloqContexto
Registrar 'tratativa.transacao_declarada' ($servico -match '(?s)@Transactional\s+public\s+ConfirmacaoComando\s+tratarExterno\s*\(') $fs
$calc=Metodo $servico 'conferirCalculo'
Registrar 'contexto.calculo_cliente_armazem_contrato_e_periodo_presentes' ($calc -match '(?s)getCliente.*?getArmazem.*?getContrato.*?getPeriodoInicio.*?getPeriodoFim.*?CONTEXTO_DIVERGENTE') $fs $calc
$versaoFile='backend/src/main/java/br/com/rodogarcia/wms/models/VersaoFechamento.java';$versaoModel=Ler $versaoFile
Registrar 'snapshot.json_hash_sem_setter_publico' ($versaoModel -notmatch '\bpublic\s+void\s+(?:setMemoriaJson|setConteudoHash|setSaldo|setNatureza)\s*\(') $versaoFile
$novaVersao=Metodo $servico 'novaVersao'
Registrar 'snapshot.hash_UTF8_memoria_mesmo_json' ($novaVersao -match '(?s)String json\s*=\s*mapper\.writeValueAsString.*?new VersaoFechamento\(.*?json,.*?digest\(\s*json\.getBytes\(StandardCharsets\.UTF_8\)') $fs $novaVersao
$dtoFile='backend/src/main/java/br/com/rodogarcia/wms/dto/FechamentoCobrancaDto.java';$dto=Ler $dtoFile
Registrar 'tratativa.dto_resultado_dominio_finito' ($dto -match 'Pattern\(regexp\s*=\s*"EMITIDO\|NAO_EMITIDO_CONFIRMADO"\)\s*String resultado') $dtoFile
Registrar 'tratativa.dto_referencias_dominio_JSON' ($dto -match 'Pattern\(regexp\s*=\s*"MANTIDO\|CANCELAMENTO_COMPROVADO"\)\s*String situacao') $dtoFile
$csFile='backend/src/main/java/br/com/rodogarcia/wms/services/CadastroSupport.java';$cs=Ler $csFile;$transicao=Metodo $cs 'transicao'
Registrar 'cadastro.INATIVO_recusa_origem_e_destino_comuns' ($transicao -match '(?s)cadastro\.getSituacao\(\)\s*==\s*SituacaoCadastro\.INATIVO\s*\|\|\s*destino\s*==\s*SituacaoCadastro\.INATIVO.*?throw') $csFile $transicao
$servicosCadastros=@('ClienteService','ArmazemService','ProdutoService','EmbalagemService','EnderecoService','CapacidadeService')
foreach($s in $servicosCadastros){
    $arquivo='backend/src/main/java/br/com/rodogarcia/wms/services/'+$s+'.java'
    $fonte=Ler $arquivo
    Registrar ($s+'.guarda_transicao_comum_presente') ($fonte -match 'CadastroSupport\.transicao\(') $arquivo
}
# Complemento restrito p2-origem: anotacoes/trechos do freeze333, sem aceitar negocio.
$sqlFile='database/migrations/V8__fechamento_e_versoes.sql';$sql=Ler $sqlFile
$traitFile='backend/src/main/java/br/com/rodogarcia/wms/models/TratativaExternaFechamento.java';$trait=Ler $traitFile
function Normalizar-Expressao([string]$texto){($texto -replace '\s','').ToUpperInvariant()}
foreach($par in @(@('ajuste_fechamento',$fm,'ck_ajuste_fechamento_tratativa_origem'),@('tratativa_externa_fechamento',$traitFile,'ck_tratativa_externa_fechamento_base_origem'))){
    $fonte=Ler $par[1]
    $checkJpa=[regex]::Match($fonte,'@(?:org\.hibernate\.annotations\.)?Check\s*\(\s*constraints\s*=\s*"([^"]+)"\s*\)').Groups[1].Value
    $regra=($contrato.tabelas|Where-Object{$_.nome -ceq $par[0]}).checks.($par[2])
    Registrar ($par[0]+'.Check_JPA_mesma_expressao_pareamento') ((Normalizar-Expressao $checkJpa) -ceq (Normalizar-Expressao $regra)) $par[1] $checkJpa
}
Registrar 'p2.tipo_default_SQL_explicito' ($sql -match "tipo varchar\(24\) NOT NULL CONSTRAINT df_ajuste_fechamento_tipo DEFAULT 'CORRECAO_CALCULO'") $sqlFile
Registrar 'p2.tipo_inicializador_Java_comum' ($model -match 'private String tipo\s*=\s*"CORRECAO_CALCULO"\s*;') $fm
$tiposAtribuidos=@([regex]::Matches($model,'(?m)^\s*(?:private String )?tipo\s*=\s*"([A-Z_]+)"')|ForEach-Object{$_.Groups[1].Value}|Sort-Object -Unique)
Registrar 'p2.tipo_dois_literais_efetivamente_atribuidos' ($tiposAtribuidos.Count -eq 2 -and 'CORRECAO_CALCULO' -cin $tiposAtribuidos -and 'REGULARIZACAO_ORIGEM' -cin $tiposAtribuidos) $fm
$identificar=Metodo $model 'identificarRegularizacao';$baseTrait=Metodo $trait 'identificarBaseAnterior'
Registrar 'p2.model_regularizacao_identificada_antes_de_persistir' ($identificar -match '(?s)id != null.*?tratativaOrigem != null.*?!situacao.equals\("VALIDADO"\).*?throw.*?tipo = "REGULARIZACAO_ORIGEM";.*?tratativaOrigem = tratativa;') $fm $identificar
Registrar 'p2.model_base_contexto_delta_cruzados' ($identificar -match '(?s)getVersaoBaseAnterior.*?getVersaoResultado.*?calculoBase.*?calculoCorrigido.*?valorBase.compareTo.*?valorCorrigido.compareTo.*?diferenca.compareTo.*?getCliente.*?getArmazem.*?getPeriodoInicio.*?throw') $fm $identificar
Registrar 'p2.tratativa_base_escrita_uma_vez_mesmo_fechamento' ($baseTrait -match '(?s)id != null.*?versaoBaseAnterior != null.*?dependencias == null.*?getFechamento.*?getId.*?throw.*?versaoBaseAnterior = anterior;.*?dependenciasOrigemJson = dependencias;') $traitFile $baseTrait
$repoFile='backend/src/main/java/br/com/rodogarcia/wms/repositories/AjusteFechamentoRepository.java';$repo=Ler $repoFile
Registrar 'p2.repositorio_por_todo_fechamento' ($repo -match 'findByOrigemVersaoFechamentoIdOrderByIdAsc\(Long id\)') $repoFile
Registrar 'p2.tratativa_todos_originados_sem_filtro_situacao_tipo' ($tratar -match 'var originados\s*=\s*ajustes.findByOrigemVersaoFechamentoIdOrderByIdAsc\(f.getId\(\)\)\s*;') $fs $tratar
Registrar 'p2.tratativa_lista_exata_sem_ID_repetido' ($tratar -match '(?s)new HashSet<>\(reg.ajustesDependentesIds\(\)\).size.*?reg.ajustesDependentesIds\(\).size.*?originados.stream.*?map\(AjusteFechamento::getId\).*?DEPENDENCIAS_ORIGEM_DIVERGENTES') $fs $tratar
Registrar 'p2.delta_anterior_menos_selecionado_e_esperado' ($tratar -match '(?s)deltaOrigem\s*=\s*baseAnterior.getCalculo\(\).getTotal\(\).subtract\(alvo.getCalculo\(\).getTotal\(\)\);.*?deltaOrigem.compareTo\(reg.diferencaEsperada\(\)\)') $fs $tratar
Registrar 'p2.tratativa_snapshot_antes_persistir_regularizacao' ($tratar -match '(?s)identificarBaseAnterior\(\s*baseAnterior, mapper.writeValueAsString\(dependenciasOrigem\)\).*?tratativas.saveAndFlush\(tratativa\).*?new AjusteFechamento\(\s*alvo,\s*destinoOrigem,\s*alvo.getCalculo\(\),\s*baseAnterior.getCalculo\(\).*?reg.identificarRegularizacao\(tratativa\).*?ajustes.saveAndFlush\(reg\)') $fs $tratar
Registrar 'p2.regularizacao_hash_tecnico_sem_admin_novo' ($tratar -match '"REGULARIZACAO_ORIGEM", tratativa.getId\(\), dependenciasOrigem' -and $tratar -notmatch 'operacoes.salvar\(\s*[^,]+,\s*"REGULARIZACAO_ORIGEM"') $fs $tratar
Registrar 'p2.regularizacao_cria_composicao_destino_em_revisao' ($tratar -match '(?s)reg.identificarRegularizacao.*?novaVersao\(destinoOrigem.*?destinoOrigem.atualizar\("EM_REVISAO"') $fs $tratar
$destinoReg=Metodo $servico 'destinoRegularizacao'
Registrar 'p2.destino_regularizacao_primeiro_posterior_lock_versao' ($destinoReg -match '(?s)findByClienteIdAndArmazemIdOrderByPeriodoInicioAsc.*?getPeriodoInicio.*?getPeriodoFim.*?getFirst.*?buscarParaAtualizar.*?conferirVersaoLock.*?EM_REVISAO.*?REJEITADO') $fs $destinoReg
$ajustar=Metodo $servico 'ajustar'
Registrar 'p2.cadeia_comum_exclui_regularizacoes_todas_versoes' ($ajustar -match '(?s)findByOrigemVersaoFechamentoIdOrderByIdAsc\(original.getId\(\)\).*?filter\(a -> a.getTipo\(\).equals\("CORRECAO_CALCULO"\)\)') $fs $ajustar
Registrar 'p2.cadeia_hash_em_todo_fechamento_e_base_ultima_comum' ($ajustar -match '(?s)historico.stream.*?getHashCorrecao.*?AJUSTE_DUPLICADO.*?historico.isEmpty.*?origem.getCalculo.*?historico.getLast\(\).getCalculoCorrigido') $fs $ajustar
Registrar 'p2.cadeia_exige_origem_atualmente_reconciliada' ($ajustar -match 'original.getVersaoAtual\(\) != origem.getNumero\(\)') $fs $ajustar
$preparar=Metodo $servico 'preparar'
Registrar 'p2.preparar_periodo_e_contexto_antes_replay' ($preparar -match '(?s)acesso.exigirGestor.*?contextos.bloquear.*?cadastros.conferir\(.*?cadastros.conferirPeriodo.*?calc.getPeriodoInicio.*?calc.getPeriodoFim.*?operacoes.repetida') $fs $preparar
$resFile='backend/src/main/java/br/com/rodogarcia/wms/services/ResolucaoFinanceiraCadastroService.java';$res=Ler $resFile
Registrar 'p2.auditoria_JSON_resolucao_contexto_periodos_identificados' ($res -match '(?s)new AuditoriaResolucao.*?new CompromissoAuditado\(\s*r.tipo\(\),\s*r.compromissoId\(\),\s*cliente,\s*armazem,\s*compromissoInicio,\s*compromissoFim,\s*inicio,\s*fim') $resFile
Registrar 'p2.concluir_auditoria_resolucao_e_periodo' ($servico -match 'cadastros.paraAuditoria\(resolucao, r, f.getPeriodoInicio\(\), f.getPeriodoFim\(\)\)') $fs
$calcFile='backend/src/main/java/br/com/rodogarcia/wms/services/CalculoCobrancaService.java';$calcFonte=Ler $calcFile
Registrar 'p2.calculo_auditoria_JSON_identificado' ($calcFonte -match '(?s)auditoria.registrar.*?resolucaoCadastro.paraAuditoria\(\s*d.resolucao\(\), r, d.periodoInicio\(\), d.periodoFim\(\)\)') $calcFile
$configFile='backend/src/main/java/br/com/rodogarcia/wms/services/ConfiguracaoCobrancaService.java';$configFonte=Ler $configFile
Registrar 'p2.configuracao_auditoria_JSON_identificado' ($configFonte -match 'resolucaoCadastro.paraAuditoria\(d.resolucao\(\), r\)') $configFile
$doc29File='docs/29-cadastros-servicos-e-calculo.md';$doc29=Ler $doc29File
Registrar 'p2.doc29_JSON_existente_sem_DDL_adicional_formal' ($doc29 -match 'JSON existente de auditoria guarda explicitamente' -and $doc29 -match 'Sem coluna/tabela V7 adicional') $doc29File
$divergencias=@($checks.Keys|Where-Object{-not $checks[$_]})
$instaveis=@($hashes|Where-Object{(Get-FileHash -LiteralPath (Join-Path $raiz $_.arquivo) -Algorithm SHA256).Hash -cne $_.sha256})
[pscustomobject]@{
    natureza='Suplemento estatico de anotacoes e trechos BE13; nenhum SQL/JPA/Java/build'
    utc=[DateTime]::UtcNow.ToString('o');verificacoes=$checks.Count;atendidas=$checks.Count-$divergencias.Count
    imutaveisAnotados=$imutaveis;relacoesConferidas=$relacoes;checksJpaDeclarados=$declaracoesCheckJpa
    checks=$checks;divergencias=$divergencias;hashes=$hashes;arquivosAlteradosDuranteLeitura=$instaveis;evidencias=$evidencias
    limite='Presenca lexical nao prova semantica/transacao/locks/replay/concorrencia nem valida negocio. Dois @Check JPA conferidos nao equivalem aos26 CHECKs SQL Server; indice nullable nao comprova filtro/dialeto real. Freeze333 somente em leitura.'
}|ConvertTo-Json -Depth 8
if($divergencias.Count -gt 0 -or $instaveis.Count -gt 0){exit 1}
