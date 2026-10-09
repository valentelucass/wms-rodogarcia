from pathlib import Path
import json, hashlib, datetime

root=Path(__file__).resolve().parents[3];backend=root/'backend';ev=backend/'evidencias';rt=root/'orchestracao/.runtime'
def rd(p):
    b=p.read_bytes();return json.loads(b.decode('utf-16') if b.startswith((b'\xff\xfe',b'\xfe\xff')) else b.decode('utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def rel(p):return p.relative_to(root).as_posix()
def wr(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
manifest=rd(ev/'d28-manifest.json');coverage=rd(ev/'d28-cobertura.json');process=rd(ev/'d28-processos-atual.json')
version=rd(backend/'target-d28-helper/versao-atual.json')
checks=[]
def check(name,passed,details=None):checks.append({'caso':name,'passou':bool(passed),'detalhe':details})
jar=backend/'target-d28-atual/wms-backend-0.0.1-SNAPSHOT.jar'
check('JAR atual igual ao artefato de todas jornadas',sha(jar)==manifest['build']['jarSHA'] and all(r['jarSha256']==sha(jar) for r in manifest['rodadas']))
check('build novo sem profile sqlserver-it e420 testes sem falha',manifest['build']['tests']==420 and manifest['build']['failures']==0 and manifest['build']['errors']==0 and not manifest['build']['sqlserverItExecutado'])
check('29 criterios causais sem somar compartilhamento',len(coverage['itens'])==29)
check('inventario causal corresponde a leitura atual Vigia',coverage['inventarioSHA256']==sha(rt/'d28-vigia-inventario.json'))
check('todos JARs/helpers proprios ausentes e portas sem listeners',process['jarsAtivos']==0 and process['listenersProprios']==0 and all(not r['helperExiste'] for r in process['processos']),{'utc':process['utc']})
check('HTTP real nao inclui sintese offline',manifest['HTTPReal']==sum(r['HTTP'] for r in manifest['rodadas']) and manifest['offlineNuncaSomadoAoHTTP'])
preserved=[]
for f in rd(ev/'d28-fontes-derivacao.json'):
    p=backend/Path(f['origem'].replace('\\','/'));ok=sha(p)==f['shaOrigem'].upper()
    preserved.append({'arquivo':rel(p),'SHA256':sha(p),'igualAoInicio':ok})
check('fontes D27 historicas derivadas preservadas',all(f['igualAoInicio'] for f in preserved),len(preserved))
versions=[]
for p in sorted(ev.glob('d28-helper-versao-*.json')):
    v=rd(p);archive=backend/v['arquivoArchive'];ok=True
    for f in v['arquivos']:
        target=archive/('classes' if f['arquivo'].endswith('.class') else 'fontes')/Path(f['arquivo']).name
        ok=ok and target.is_file() and sha(target)==f['sha256']
    used=[r['rodada'] for r in manifest['rodadas'] if rd(ev/f'd28-{r["rodada"]}-http.json')['ids'].get('helperFontesCompilados',{}).get('hashConjunto')==v['hashConjunto']]
    versions.append({'manifest':rel(p),'SHA256':sha(p),'hashConjunto':v['hashConjunto'],'capturadoUtc':v['capturadoUtc'],'fontes':v['fontes'],'compilados':v['compilados'],'copiaConferida':ok,'archive':v['arquivoArchive'],'rodadasComManifestEmRuntime':used,'compilacaoFinalIniciaJornadas':False})
check('fontes/classes arquivadas por hash conferem bytes',all(v['copiaConferida'] for v in versions),len(versions))
sql=[]
for r in manifest['rodadas']:
    p=rt/f'd28-prumo-select-{r["rodada"]}-resumo.json'
    signal=rt/f'd28-prumo-select-liberar-{r["rodada"]}.json'
    if not p.exists():sql.append({'rodada':r['rodada'],'estado':'SEM_RESUMO'});continue
    d=rd(p);s=rd(signal) if signal.exists() else {};photo=root/d['arquivo'];focal=rt/f'd28-prumo-focal-{r["rodada"]}.json';f=rd(focal) if focal.exists() else {}
    sql.append({'rodada':r['rodada'],'estado':d['estado'],'checks':d['checks'],'falhas':d['falhas'],'resumo':rel(p),'resumoSHA256':sha(p),
                'foto':rel(photo),'fotoSHA256':sha(photo),'focal':rel(focal) if focal.exists() else None,'focalChecks':f.get('checks'),'focalFalhas':f.get('falhas'),
                'sinal':rel(signal) if signal.exists() else None,'sinalSHA256':sha(signal) if signal.exists() else None})
check('SQL novo publicado para todas rodadas reais inclusive reds',all(s.get('falhas')==0 for s in sql))
check('rodadas em hold encerraram apenas apos sinal Prumo',all(r['jarEncerrado'] and (rt/f'd28-prumo-select-liberar-{r["rodada"]}.json').exists() for r in manifest['rodadas'] if r['fase']=='concluido'))
offline=[]
for name in ['d28-readiness-offline.json','d28-respostas-offline.json','d28-processo-limitado-offline.json','d28-https-falha-offline.json','d28-guardas-offline.json','d28-sql-quoting-offline.json','d28-aguardador-offline.json','d28-saldo-xml-guarda-offline.json']:
    p=ev/name;d=rd(p);passed=d.get('falhas',0)==0 and d.get('aprovado',True)
    offline.append({'arquivo':rel(p),'SHA256':sha(p),'checks':d.get('checks',d.get('respostasSinteticas')),'aprovado':passed})
check('auxiliares offline com green separado de HTTP/SQL',all(d['aprovado'] for d in offline))
app_sources=[]
for p in sorted((backend/'src').rglob('*')):
    if p.is_file():app_sources.append({'arquivo':rel(p),'SHA256':sha(p)})
errors=[
 {'red':'D284DC609CC','classificacao':'roteiro/fixture','trigger':'positivo XML2E0+3E0 sem saldo preparado','obtido':'409 SALDO_INSUFICIENTE correto','green':'D288163F01B criou recebimento100 por HTTP na mesma familia36 e positivo201/replay soma5','backendDefeito':False},
 {'red':['D28079AE038','D28EFFF454F'],'classificacao':'helper/oraculo GET final','trigger':'GET negativo404 aprovado no roteiro entrou no coletor que esperava200','green':'Filtro actual200+DTO/ID valido; D28E89EBAE4 listas, D282DDA1365 saidas46/47 apos replay+entrada27','backendDefeito':False},
 {'red':'D289A0CF1ED','classificacao':'oraculo/precondicao R16','trigger':'esperou fisico+4 apos409 divergencia pendente; entrada28 QUARENTENA','obtido':'HTTP409 manteve100/pendente0 corretamente','green':'D284B2EA65C retomou somente entrada28 com aceite explicito motivo ficticioAC03, efetivou4;104/pendente4/100unitizado. Fonte final bloqueia modo fresco; fixture offline3/0 e launcher negativo sem SQL/HTTP','backendDefeito':False},
 {'classificacao':'helper gerador/contrato','trigger':'prefixo inicial d28-select-liberar e guardaDB_NAME/login antigos','green':'gerador+runner usam prefixo d28-prumo-select-liberar e envelopealvo.banco/login/usuario; guardas12/0 recusam envelope antigo','preservacao':'Farol espelhou apenas sinal primeiro aprovado; original intacto'},
 {'classificacao':'helper compilacao','trigger':'reextracao JAR lib em uso falhou por lock','green':'extrair somente ausente, conferir comprimento de lib preexistente; compilacao final sem substituir lib aberta'},
 {'classificacao':'captura de encerramento','trigger':'bind imediato portFree false apos fechamento, inclusive failureXML','green':'sem corrigir flags historicas; Win32_Process/Get-NetTCPConnectionconfirmam0 listeners e0 APIs/helpers nas17 rodadas'},
 {'classificacao':'teste/auxiliar offline aguardador','red':'d28-aguardador-red.md','trigger':'operador PowerShell no inicio linha gerou parse error','green':'posicionar operador no fim linha;20/0 com mutex real entre processos, sem SQL/credencial/JVM'},
 {'classificacao':'recibo/consolidador','red':'d28-cobertura-red.md','trigger':'UTF16LE BOM em relatorio offline PowerShell','green':'decodificacao explicita por BOM mantendo bytes;29 linhas causais'},
 {'classificacao':'oraculos SQL Prumo, autoria independente','trigger':'GET historicopreunitizacao vsfoto atual; array vazioNULL; zero movimentos global depois fixture; avaria5 vs20/reservaREVERTIDA vsATIVA','green':'SELECT/focais corrigidos novos; reds de Prumo preservados nos arquivos -antes/tentativas','limite':'Cedro nao alterou leitor SQL nem declarou defeito do app por esses reds'}]
result={'incremento':'D28','papel':'Cedro/backend','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'estado':'ESCOPO_CEDRO_EXECUTADO_E_RECIBO_ENTREGUE_REVISAO_FAROL_VIGIA',
 'novasJornadasProibidas':True,'fontesBackendAlteradasPeloCedro':False,'build':manifest['build'],'HTTPReal':manifest['HTTPReal'],'assertivasReais':manifest['assertivasReais'],
 'rodadasReais':len(manifest['rodadas']),'familiasD28':sorted({(r['clienteId'],r['armazemId']) for r in manifest['rodadas']}),
 'casosHTTPFalhosPreservados':[{'rodada':r['rodada'],'divergencias':r['divergencias']} for r in manifest['rodadas'] if r['divergencias']],
 'criteriosCausais':29,'matrizCentralDono':'Farol','SQLNovoPrumo':sql,'offline':offline,'helpersVersionados':versions,'helperVersaoFinal':version['hashConjunto'],
 'fontesBackendAtuais':app_sources,'fontesHistoricasPreservadas':preserved,'errosEClassificacao':errors,
 'processos':{'arquivo':'backend/evidencias/d28-processos-atual.json','SHA256':sha(ev/'d28-processos-atual.json'),'utc':process['utc'],'jarsAtivos':process['jarsAtivos'],'listenersProprios':process['listenersProprios'],'helpersAtivos':sum(r['helperExiste'] for r in process['processos'])},
 'verificacaoRecibo':{'checks':len(checks),'falhas':sum(not c['passou'] for c in checks),'casos':checks},
 'limites':['Witness SQL nativo online nao coberto: WMSDEV recusou DMV SQL300. Sem sa/grant/fallback; concorrencia realHTTP/JVM+SELECT posterior sao provas separadas.',
 'H1-H6: fontes iniciais e versoes logicas por rodada preservadas; todos os bytes compilados intermediarios nao foram arquivados na epoca. H7/preH8, H8 ativo e fonte/compilacao final estao arquivados porhash. Nao reconstruir hashes antigos como se capturados.',
 'Primeiras duas rodadas registram presenca RequestId, sem todos valores. Demais/complementos capturam valores; nao fabricar RequestId antigo.',
 'Financeiro minimo50 aprovado com corte real06->07; previsoes54/53.75 e PENDENTE temporal separados. Sem mudar clock/historia, precos homologados ou emissaofiscal.',
 'Sem homologacao integral debackend/frontend/producao. Aceite independente e recibo central sao deFarol/Vigia.'],
 'proximo':'Farol consolidar matriz/recibo central e Vigia finalizar parecer. Atualizacao AST do mapa conformeAGENTS coordenada porFarol; Cedro preserva dono dos arquivos centrais. Consumir notificacoes antigas como ja atendidas, sem callbackHermes.'}
wr(ev/'d28-cedro.json',result)
progress={k:v for k,v in result.items() if k not in ['fontesBackendAtuais','helpersVersionados','fontesHistoricasPreservadas','SQLNovoPrumo','verificacaoRecibo']}
progress['recibo']='backend/evidencias/d28-cedro.json';progress['matrizCausal']='backend/evidencias/d28-cobertura.json';wr(ev/'d28-cedro-progresso.json',progress)
md=['# D28 — entrega Cedro','',f'Executado no JAR atual `{manifest["build"]["jarSHA"]}`: build novo clean verify sem sqlserver-it, 420 testes sem falhas, erros ou ignorados. Foram {manifest["HTTPReal"]} chamadas HTTP reais e {manifest["assertivasReais"]} assertivas nas 17 rodadas, incluindo reds preservados. Fixtures offline não somam HTTP. Seis famílias D28 fictícias, negócio somente por HTTP e WMSDEV/WMS_DEV confirmado antes de operar.','',
 'As 29 linhas estão vinculadas em `d28-cobertura.json/md`: caso, índice, UUID, perfil, IDs, RequestId, hash e assertivas/fotos SQL novas. R17 mantém o limite material SQL300. `d28-perfis.json` deriva perfis e respostas dos casos reais. Não é homologação do backend inteiro.','',
 'R13: AST e mock Query-Sql, 6/0. R23: 20/0 offline de opt-in, ticket, mutex entre processos, prazo, estado final e saldo antes do gate, sem SQL/credencial/JVM. R24 Prumo: 16/0 + 6/0 KEY/PID. R16: entrada 28 efetivada com aceite explícito fictício da falta de uma unidade; físico104, pendente4, unitizado100, disponível100. Modo fresco bloqueado no launcher e helper; fixture isolada3/0.','',
 'R16 green D284B2EA65C tem SELECT34/0 e focal8/0. GETs após replay dos pedidos 46/47 e entrada 27 estão em D282DDA1365, somente leitura, SELECT68/0 e focal5/0. As reservas originais não foram repetidas. O par real200/409 tem duas threads HTTP/JDBC na JVM e SELECT posterior; vencedor47/perdedor46 foram derivados da prova.','',
 'Financeiro aprovado não zero: mínimo50 em ciclo fechado real06→07, reabertura/v2/v1SUPERADA e memória anterior intacta, sem história fabricada ou NFS-e. Variantes54/PENDENTE/53.75 são previsões em corte aberto. Ajustes, RN22/extremos/científico, filtros/FIFO/quarentena/avaria/GRIS/vigência constam nos casos e SQL novos.','',
 '| Red / achado | Classificacao | Green / conclusao |','| --- | --- | --- |']
for e in errors:md.append('| '+str(e.get('red',e.get('trigger'))).replace('|','/')+' | '+e['classificacao']+' | '+e['green']+' |')
md+=['',f'Fontes/classes finais arquivadas por hash `{version["hashConjunto"]}`. H8 executado:7B0FA59855F40B7C8A7B6AB49F02ACE977EDAFEE966426108DB4B92EDA7FECBB preservado; JAR de aplicação inalterado. Manifestos `d28-helper-versao-*.json` e cópias `target-d28-helper/versionados/`. A compilação final não inicia jornadas. H1–H6 não tiveram todos os compilados intermediários arquivados: limite explícito, sem hashes retrospectivos inventados.','',
 f'Encerramento nativo em {process["utc"]}: zero JARs próprios, helpers e listeners nas portas das 17 rodadas. Inclui failureXML com flag histórica portFree=false preservada. Cada hold encerrou após sinal Prumo. Nenhum processo alheio ou terminal foi encerrado ou resetado.','',
 f'Conferência dos recibos: {len(checks)} checks, {sum(not c["passou"] for c in checks)} falhas. Fontes D27 derivadas preservadas por hash. Preexistentes/dirty preservados, sem alterar backend/src, database, frontend ou registros centrais. Sem DDL/PROD/launcherDEV-PROD/commit/push/Hermes/ETL.','',
 'Entrega de Cedro concluída. Farol consolida matriz/recibo central e Vigia encerra parecer. Nenhuma jornada/API de negócio nova será iniciada. Mensagens de preflight antigas são notificações já atendidas.']
(ev/'d28-cedro.md').write_text('\n'.join(md)+'\n',encoding='utf-8')
print(json.dumps({'estado':result['estado'],'reciboChecks':len(checks),'reciboFalhas':result['verificacaoRecibo']['falhas'],'HTTP':result['HTTPReal'],'assertivas':result['assertivasReais'],'jarsAtivos':process['jarsAtivos'],'listeners':process['listenersProprios'],'versaoFinal':version['hashConjunto']}))
if(result['verificacaoRecibo']['falhas']):raise SystemExit(1)
