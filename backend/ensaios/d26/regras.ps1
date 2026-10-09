$ErrorActionPreference='Stop'
$backend=(Resolve-Path "$PSScriptRoot/../..").Path
$root=(Resolve-Path "$backend/..").Path
$matrix=Get-Content "$backend/evidencias/d26-matriz.json" -Raw -Encoding UTF8|ConvertFrom-Json
# Relacao deliberadamente parcial: uma resposta2xx da rota nao homologa todos os requisitos.
# Colunas: regra;endpoints;achado do ensaio;limite concreto;recorte SQL.
$definitions=@'
RN01|E032,E005,E148,E059|Famílias fictícias distintas, produtos/embalagens e proprietários cadastrados por HTTP.|Não homologados produtos/clientes reais ou toda variedade operacional.|cadastro
RN02|E074,E075,E076,E159|GETs identificam cliente/SKU/unidade/situação/armazém/endereço; saldo e conteúdo confrontados por família.|Fotografias não são certificação de todo estoque nem inventário real.|fisico
RN03||Separação WMS/TMS preservada; suíte de arquitetura incluída no build412.|Integração/transporte TMS fora D26; nenhum endpoint/provedor TMS acionado.|nenhum
RN04|E005,E070,E114,E116|Armazém/endereço/fiscal fictícios;400 cidade/UF/campos exclusivos de cliente preservado e payload corrigido.|Validação fiscal cadastral não prova emissor real nem dados de implantação.|cadastro
RN05|E070,E071,E119,E121|Endereços rastreáveis, XLSX fictício prévia/confirmação/replay e uma posição nova.|Códigos de ensaio; convenção de identificação e uso no coletor não homologados.|fisico
RN06|E032,E033,E148,E149,E115,E118|Cliente/produto/dados fiscais criados/revisados; propriedade e versões mantidas, recusa de versão antiga preservada.|Cadastros reais e todas combinações tributárias não homologados.|cadastro
RN07|E115,E116,E118|Complementos fiscais e referências por produto/cliente testados com códigos fictícios.|Não comprova todas regras CFOP por operação nem integração fiscal; valores reais ausentes.|cadastro
RN08|E040,E043,E046,E041,E044,E047,E067|Tabelas/vínculos/contratos vigentes; cortes reais e encerramentos/replay;409 HISTORICO_CALCULADO preservado.|Ensaio financeiro com tabela padrão; combinações de tabela específica/conflitos completos só suíte independente, não novas provas SQL.|financeiro
RN09|E037,E094,E016|Serviço manual4 × tarifa5 =20 e anulação; memória identificada.|Não homologado catálogo/preços reais ou toda unidade de cobrança.|financeiro
RN10|E153,E159,E160|Unitização/GET/reimpressão sem criar estoque, identidade e conteúdo preservados.|Impressora/leitor físico e conteúdo visual impresso não testados.|fisico
RN11|E078,E160|Remanejamento mantendo código, nota/FIFO/conteúdo e duas posições; GETs/histórico.|Etiqueta física/uso em armazém não testados.|fisico
RN12|E125,E128,E129,E126,E135|Entrada manual e XML fictício; chegadas parciais, filtros/GETs;409 nota já vinculada preservado.|Não é NOTAZZ/provedor real; XMLs reais e todas variantes não homologados.|entrada
RN13|E130,E131,E133|Conferência/chegadas/liberação por HTTP com Operação/Supervisor; replay e versões.|Não prova conferência física de operador/equipamento.|entrada
RN14|E131,E133,E079,E011|XML previsto5/recebido4, divergência aceita por Supervisor; bloqueios/quarentena e avaria.|Ajuste de contagem permanece bloqueado E052; divergência não foi apagada.|entrada
RN15|E153,E154,E155|Unidades de um produto; divisão40+10 e reagrupamento50 preservam origens.|Não alegar ensaio SQL de todas tentativas de mistura de SKU; cenários da suíte independente separados.|fisico
RN16|E153,E154,E155,E156|Conteúdo50 conservado em divisão/reagrupamento; cargas20/3 e entrada10 rastreáveis.|A igualdade global de todas unidades não foi atestada por resposta2xx; recortes específicos publicados.|fisico
RN17|E079,E080,E081,E011,E140,E142|Avaria/quarentena bloqueiam disponibilidade; liberação/reparo e GETs; estoque bloqueado não atende reserva.|Variantes simultâneas de avaria/reserva e todas causas de bloqueio não integralmente exercitadas por HTTP.|fisico
RN18|E017,E018,E078,E084|Capacidade/duas posições, leitura UUID e destino; medida imutável recusada400 e remanejamento válido.|Não prova coletor/leitura física; toda geometria/capacidade não homologada.|fisico
RN19|E077,E078,E083,E009|Movimentos/fatos/auditoria por GET; replays e snapshots físicos únicos nos recortes SQL.|Não certifica histórico universal, somente IDs/famílias publicados.|fisico
RN20|E074,E075,E076,E122|Listas/saldo/unidade/indicadores com filtros/paginação e negativas de alcance.|400 parâmetro produtoId ausente no helper preservado; GET corrigido não apaga tentativa.|fisico
RN21|E078,E016|Permanências/endereço alimentam memórias; previsão sem transformação ambígua e pendência explícita quando há ambiguidade.|Não inventada quantidade anterior à transformação; extra corte real encerrado bloqueia aprovação20 pois fato3 ANULADO, recálculo0 e snapshot antigo desatualizado.|financeiro
RN22|E137,E138,E139|Pedido manual integral, consulta/filtros e estoque/documentos separados.|Criação de pedido de saída por XML não tem endpoint específico nesta versão; XML documental E086 não substitui esse requisito do PDF.|nenhum
RN23|E140,E141,E142|FIFO sugerido e exceção justificada Supervisor; pedido integral/reserva parcial da unidade.|Todas datas/lotes/combinações FIFO não homologadas em SQL; chegada original preservada nos recortes.|saida
RN24|E142,E144,E145|Reserva/replay/insuficiência/reversão; par200/409 com duas threads JDBC no reservar e saldo final.|Witness SQL simultâneo não comprovado: amostras antigas0 e nova janela18456; não aprovar concorrência SQL só pelo par HTTP.|concorrencia
RN25|E084,E085,E088,E089,E090|Retirada60 deixa40, retorno interno após documento cancelado sem baixa, devolução externa5; carga3 retirada integral.|Transporte/entrega TMS e fiscal real fora escopo; ajuste de contagem não integra jornada física concluída.|saida
RN26|E086,E087,E111,E112|XML documental fictício, cancelamento, cobertura/retirada; confirmação externa manual e resolução zero.|Sem emissão NOTAZZ/ESL/NFS-e reais; referência fictícia não prova integração real.|saida
RN27|E016,E094,E105,E106,E107,E108,E110,E001|Memórias20, mínimo50+GRIS1=51 previsão; fechamento histórico zero/reabertura/ajuste/replay. Extra real preparou oito cortes zero; aprovação snapshot20 recusada409 CALCULO_DESATUALIZADO.|Não homologados preços, mês completo, fuso/DST/prorratas/todas bases GRIS. APROVADO20 não obtido por anulação prévia do fato, não por corte futuro.|financeiro
RN28|E074,E122,E098,E102|Consultas/indicadores/demonstrativos HTTP reais e paginação.|Relatórios gerenciais/exports/visualizações frontend completos não homologados.|fisico
RN29|E009,E077,E083,E024,E051,E100|GETs de auditoria/movimentos/revisões/versões; operação/snapshot/auditoria únicos nos SELECTs específicos.|Não certificação de toda auditoria por todas rotas; comparar recorte/IDs/instante.|historico
RN30||Backend fornece APIs; nenhuma alteração ou ensaio frontend/coletor nesta D26.|Telas/teclado/coletores/leitores ficam fora do escopo backend autorizado.|nenhum
AC01|E086,E088,E087,E089|Nota documental separada da retirada física; XML/cancelamento/retorno e retirada parcial da unidade com pedido integral.|Proposta AC não vira homologação; emissão externa real não testada.|saida
AC02|E086,E109,E110,E111,E112|Procedimento fiscal manual fictício e confirmação/tratativa/resolução sem provider.|Integrações NOTAZZ/ESL reais e reconciliação externa não executadas por proibição expressa.|financeiro
AC03|E131,E133,E129,E079|Recebimento parcial50+50, XML5/4 e aceitação de divergência; quarentena preservada.|Não leitura física, nem todas variantes de triagem.|entrada
AC04|E016,E017,E078|Memória de diárias/picos, previsão fictícia e pendência temporal deliberada.|Fórmula é proposta; meia-noite/DST/prorrata/ocupação real completos não homologados por HTTP.|financeiro
AC05|E018,E020,E078|Duas posições únicas em conjunto1, remanejadas para conjunto2 e conjunto1 desocupado/encerrado.|Não homologada toda geometria nem cobrança de duas posições sob tarifa real.|fisico
AC06|E040,E046,E016|Tarifas fictícias5/zero; mínimo50 e GRIS1% base100 total51 com memória e GET.|Não aprova valores comerciais reais; outras modalidades de mínimo/GRIS e tabelas específicas não totalmente ensaiadas no SQL.|financeiro
AC07|E105,E107,E108,E001,E067|Corte26/09–27/09; rejeição/reabertura versão2, ajuste zero. Extra D26A70E7FE0 avançou oito ciclos até06/10; preparo06→07/replay e negativas de aprovação20 após corte real.|Fato3 previamente anulado; recálculo0 e snapshot20 antigo recusado CALCULO_DESATUALIZADO. Sem aprovação não zero nem recriação retroativa do fato para fabricar resultado.|financeiro
AC08|E011,E012,E013,E097|Responsabilidades distintas, marco afetado2, replay e reparo mantendo40; Supervisor403/Gestor200.|Regra proposta; todas repartições financeiras/picos não homologadas com preços reais.|avaria
AC09|E094,E095,E016|Serviço manual4×5, cota nota1, replay/idempotência e anulação antes fechamento.|Outras unidades/rateios/arredondamentos só onde comprovados; não generalizar20 a família com pendências.|financeiro
AC10|E059,E153,E154,E155,E160,E088|DUN/unidades, divisão/reagrupamento, etiqueta permanente e retirada60/remanescente40.|Impressão/equipamentos e todas embalagens reais não ensaiados.|fisico
AC11|E141,E142,E084,E085,E016|JWT RSA/HTTPS reais; perfis/alcance/claims inválidos, FIFO excepcional e acesso Gestor/Supervisor/Operação.|Perfis exercitados incluem recusas;2xx não significa permissão irrestrita. Todas combinações de acesso não homologadas.|nenhum
AC12|E064,E065,E066,E029,E067|Encerramento/reativação cadastros, remanescente carga3 exclusivamente Gestor, resolução integral preserva unidade/histórico.|Não apagado estoque; efeitos de implantação/inativação real não autorizados.|carga
AC13|E048,E054,E052,E051|Contagem primeira revisão1 corrigida, contingência conciliada/replay; revisão2 PENDENTE e aplicar409 rollback42/0.|FALHOU/BLOQUEADO: CHECK V6 fato_permanencia recusa AJUSTE_ESTOQUE; V10 só proposta inativa, não aplicada.|ajuste
AC14|E053,E054,E055,E056|Registro e conciliação de linha de contingência com replay e acesso Supervisor.|Não testado coletor offline/rede indisponível/retomada geral; somente contrato API, ajuste E052 bloqueado.|ajuste
AC15|E009,E024,E051,E077,E100|Dados anteriores, lockIT, revisões, snapshots, memória financeira e rollback preservados; sem reset.|Recuperação backup/desastre/restauração não testada/autorizada.|historico
AC16||Dados fictícios e controles de alvo/TLS/identidade própria/artefatos com hashes.|Piloto/deploy/usuários reais/preços/treinamento/equipamentos/aceite operacional fora D26; AC é proposta.|nenhum
'@
$sql=@{
 cadastro=@('d26-prumo-D26BD79EA56-persistencia.md','d26-prumo-complementos-resumo.md');
 fisico=@('d26-prumo-complementos-resumo.md','d26-prumo-D2694ED1B4D-persistencia.md');
 entrada=@('d26-prumo-D26BD79EA56-persistencia.md','d26-prumo-complementos-resumo.md');
 saida=@('d26-prumo-D2603638663-financeiro.md','d26-prumo-complementos-resumo.md');
 financeiro=@('d26-prumo-D2603638663-financeiro.md','d26-prumo-calculo4-resumo.md');
 concorrencia=@('d26-prumo-D269D06DD04-persistencia.md');
 ajuste=@('d26-prumo-D2694ED1B4D-persistencia.md');
 avaria=@('d26-prumo-complementos-resumo.md');
 historico=@('d26-prumo-complementos-resumo.md','d26-prumo-calculo4-resumo.md');carga=@('d26-prumo-complementos-resumo.md');nenhum=@()
}
$rows=@();$md=New-Object 'Collections.Generic.List[string]'
$md.Add('# D26 — confronto por requisito e proposta');$md.Add('')
$md.Add('RN vêm de docs/03 (PDF); respostas recebidas de docs/10 e consolidação docs/11 têm precedência. AC01–AC16 continuam propostas, não homologação. Docs14–35 delimitam os contratos implementados. Cada linha limita a conclusão aos casos preservados; cobertura positiva de159 rotas não aprova todas regras. JSON relaciona método/endpoint/perfis/expected/actual e arquivos; matriz de160 rotas tem todos os casos, inclusive tentativas e negativas.');$md.Add('')
$final=Get-ChildItem "$root/orchestracao/.runtime" -Filter 'd26-prumo-complementos-*.json'|Where-Object {$_.Name -match '^d26-prumo-complementos-\d{8}T\d+\.json$'}|Sort-Object LastWriteTime -Descending|Select-Object -First 1
$finalData=if($final){Get-Content $final.FullName -Raw -Encoding UTF8|ConvertFrom-Json}else{$null}
$finalPassed=$finalData -and $finalData.checks -ge 222 -and $finalData.divergencias -eq 0 -and $finalData.somenteSELECT -eq $true -and $finalData.prodConectado -eq $false
$finalRef=if($final){"../../orchestracao/.runtime/$($final.Name)"}else{$null}
$extra=Get-ChildItem "$root/orchestracao/.runtime" -Filter 'd26-prumo-fechamento20-*.json'|Where-Object {$_.Name -match '^d26-prumo-fechamento20-\d{8}T\d+\.json$'}|Sort-Object LastWriteTime -Descending|Select-Object -First 1
$extraData=if($extra){Get-Content $extra.FullName -Raw -Encoding UTF8|ConvertFrom-Json}else{$null}
$extraPassed=$extraData -and $extraData.checks -ge 76 -and $extraData.divergencias -eq 0 -and $extraData.somenteSELECT -eq $true -and $extraData.prodConectado -eq $false
$note=if($finalPassed){"Fotografia final SELECT $($finalData.checks)/0, arquivo $($final.Name), cobre IDs/contextos publicados e estados finais; sem certificar cada rota ou witness SQL simultâneo."}else{'Fotografia final222/4 preservada; quatro expected de estados anteriores à reabertura/ajuste confrontados em d26-confronto-estados-financeiros.md. Comparador Prumo ainda precisa confirmar; não tratá-la como222/0.'}
$md.Add("SQL é recorte por família/IDs/instante, nunca certificado automático de cada API. Recorte70/0 antecede novas escritas; cálculo4 corrigido tem15/0 próprio. $note O marco financeiro foi provado por HTTP/GET/replay; referenciar apenas fotografia que efetivamente inclui esse marco.");$md.Add('')
if($extraPassed){$md.Add("Extra corte real D26A70E7FE0: SELECT focal $($extraData.checks)/0 em $($extra.Name), preservando236/0. APROVADO20 bloqueado por fato anulado/calculo desatualizado; negativas e oito ciclos zero preservados, não aprovação20.");$md.Add('')}
$md.Add('| Regra / fonte | Métodos/rotas/perfis e prova HTTP | Achado delimitado | Lacuna / SQL exato |');$md.Add('| --- | --- | --- | --- |')
foreach($line in $definitions -split "`r?`n"){
 $p=$line.Split('|');$id=$p[0];$ids=@($p[1].Split(',')|Where-Object {$_});$endpoints=@($matrix.matriz|Where-Object {$ids -contains $_.id})
 $cases=@($endpoints.casos|Where-Object {$_.perfil -cne 'anonimo'})
 $refs=@($cases|Where-Object {$_.metodo -cne 'GET'}|Select-Object -Last 5)+@($cases|Where-Object {$_.metodo -ceq 'GET'}|Select-Object -Last 2)
 $proof=(@($refs|ForEach-Object {"[$($_.rodada):$($_.numero)]($($_.arquivo)) $($_.metodo) $($_.perfil) $($_.expected)/$($_.actual)"})-join '; ')
 if(-not $proof){$proof='Sem jornada HTTP atribuída; limite explícito.'}
 $sources=@(foreach($s in $sql[$p[4]]){if(-not(Test-Path "$root/orchestracao/.runtime/$s")){throw "SQL_REF_AUSENTE $s"};"../../orchestracao/.runtime/$s"})
 if($finalPassed -and $p[4] -cne 'nenhum'){$sources+=$finalRef}
 if($extraPassed -and $id -in @('RN08','RN09','RN21','RN27','RN29','AC06','AC07','AC09','AC15')){$sources+="../../orchestracao/.runtime/$($extra.Name)"}
 $links=(@($sources|ForEach-Object {"[SELECT/recorte]($_)"})-join ', ')
 $status=if($id -eq 'AC13'){'falhou/bloqueado por schema'}elseif(-not $ids.Count){'fora do ensaio HTTP autorizado'}else{'testado parcialmente; limites explícitos'}
 $source=if($id.StartsWith('RN')){'../../docs/03-regras-de-negocio.md'}else{'../../docs/11-alinhamentos-apos-respostas.md'}
 $title=if($id.StartsWith('RN')){(Get-Content "$root/docs/03-regras-de-negocio.md" -Encoding UTF8|Where-Object {$_ -match "^\| $id \|"}) -replace '^\| [^|]+\|\s*','' -replace '\|.*$',''}else{(Get-Content "$root/docs/11-alinhamentos-apos-respostas.md" -Encoding UTF8|Where-Object {$_ -match "^## $id "}) -replace '^## ',''}
 $rows+=[pscustomobject]@{regra=$id;requisito=$title;fonte=$source;estado=$status;achado=$p[2];lacuna=$p[3];endpoints=@($endpoints|Select-Object id,documentos,metodo,rota,perfisExercitados,perfisCom2xx,perfisCom403);casosSelecionados=$refs;GETs=@($cases|Where-Object metodo -ceq 'GET');SQLRecortes=$sources;SQLCertificaTodasRotas=$false;novosComplementosSQLFinalPendente=-not $finalPassed}
 $md.Add("| [$id]($source) $($ids -join ',') | $proof | $status. $($p[2]) | $($p[3]) $links |")
}
[IO.File]::WriteAllText("$backend/evidencias/d26-regras.json",([pscustomobject]@{utc=[DateTime]::UtcNow.ToString('o');regras=$rows.Count;backendTotalAprovado=$false;ACPropostasNaoHomologadas=$true;matriz=$rows}|ConvertTo-Json -Depth 14),[Text.UTF8Encoding]::new($false))
[IO.File]::WriteAllText("$backend/evidencias/d26-regras.md",($md -join "`n"),[Text.UTF8Encoding]::new($false))
Write-Output "D26 regras $($rows.Count); aprovação total falsa; lacunas preservadas"
