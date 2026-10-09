from pathlib import Path
import json,datetime,hashlib
root=Path(__file__).resolve().parents[3];ev=root/'backend/evidencias'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def put(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
rounds=[]
for p in sorted(ev.glob('d29-D29*-http.json')):
 d=read(p);ids=d['ids'];cases=d['cases'];rounds.append({'rodada':d['rodada'],'etapa':ids.get('etapa'),'fase':d['fase'],'red':d.get('bloqueio'),'clienteId':ids.get('clienteId'),'armazemId':ids.get('armazemId'),'JAR':ids.get('jarSha256'),'helperHash':ids.get('helperFontesCompilados',{}).get('hashConjunto'),'registros':len(cases),'HTTP':sum('metodo'in c for c in cases),'assertivas':sum(c.get('tipo')=='GET/estado' for c in cases),'falhas':[{k:c.get(k) for k in ['caso','expected','actual','requestId']} for c in cases if c.get('estado')=='falhou'],'contrato':str((ev/('d29-select-'+d['rodada']+'.json')).relative_to(root)),'arquivo':str(p.relative_to(root)),'pid':ids.get('jarPid'),'helperPid':ids.get('helperPid'),'porta':ids.get('porta'),'issuerPorta':ids.get('issuerPorta'),'jarEncerrado':ids.get('jarEncerrado'),'portasLivresBind':ids.get('portasLivres'),'sinalPrumo':ids.get('prumoSelectSinal')})
mapping={1:'precheck',2:'todas',3:'preservacao',4:'concorrencia',5:'financeiro',6:'entrada',7:'estoque',8:'estoque',9:'capacidade-integral',10:'estoque',11:'saida',12:'fifo',13:'estoque',14:'estoque',15:'estoque',16:'retornos',17:'todas',18:'auth',19:'auth',20:'fiscal-servicos',21:'excel',22:'fiscal-servicos',23:'financeiro',24:'financeiro',25:'financeiro',26:'carga-contagem',27:'carga-contagem',28:'contingencia',29:'encerramento',30:'consultas',31:'build',32:'externos',33:'processos'}
pri=read(root/'orchestracao/.runtime/d29-vigia-prioridades.json')['prioridades'];lume=read(root/'orchestracao/.runtime/d29-lume-inventario.json')
for p in pri:
 group=mapping[int(p['id'][-2:])];p['grupoCedro']=group;p['status']='controle de lacuna; atendimento composto ainda requer revisão';p['evidenciasD29']=[r['arquivo'] for r in rounds if group in ('todas',r['etapa']) and r['HTTP']];p['limite']='Vinculo nao fecha automaticamente cada clausula, nem o grupo inteiro.'
put(ev/'d29-prioridades-vinculos-cedro.json',{'utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'fontePrioridadesSHA':sha(root/'orchestracao/.runtime/d29-vigia-prioridades.json'),'lumeSHA':sha(root/'orchestracao/.runtime/d29-lume-inventario.json'),'gruposPlano':'backend/evidencias/d29-plano-matriz-cedro.json','prioridades':pri})
original=read(ev/'d29-fontes-baseline.json');changes=[]
if isinstance(original,list):files=original
else:files=original.get('arquivos',original.get('fontes',[]))
for f in files:
 if not isinstance(f,dict):continue
 name=f.get('arquivo',f.get('path',''));expected=f.get('SHA256',f.get('sha256'))
 if name and expected and (root/name).is_file() and sha(root/name)!=expected:changes.append(name)
put(ev/'d29-cedro-progresso.json',{'demanda':'D29','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'estado':'EXECUCAO_ATIVA_JORNADAS_RESTANTES_EM_PREPARO','rodadas':rounds,'plano':'backend/evidencias/d29-plano-matriz-cedro.json','prioridades':'backend/evidencias/d29-prioridades-vinculos-cedro.json','desvios':['d29-desvio-helper-datas.md','redC633D347medidas/GETconjunto corrigidos na retomadaF46','redfinanceiroLocal01 fixture deepmockInstant; nunca classificar como defeito backend'],'fontesPreexistentesDiferentesBaseline':changes,'financeiroLocal':'datas sinteticas em memoria, não SQL/HTTP real/aprovacao','proximos':['carga-contagem/financeiro/fiscal-servicos/contingencia/auth/limites','decompor clausulas Lume e prioridades33, evidencias atuais','ampliar oraculos financeiros locais sem substituir prova real','fecho processos e preservacao Prumo ao fim'],'coberturaTotalDeclarada':False})
current=read(ev/'d29-cedro-progresso.json')
if (ev/'d29-impedimento-transporte-atual.md').exists():
 current['estado']='GUARDA_API_RECUSADA_PRELOGIN_TRABALHO_ISOLADO_ATIVO'
 current['impedimentoAtual']={'arquivo':'backend/evidencias/d29-impedimento-transporte-atual.md','prova':'D2976713EE4','HTTP':0,'alvoRealConfirmadoNestaTentativa':False,'naoInferirServidorForaDoAr':True}
 current['proximos']=['terminar provas XML locais/build novo e confrontar atomos','manifestos/causal/fecho OS/preservacao final em coordenacao Farol','novo precheck restrito somente apos transporte viavel; nenhum fallback ou SQL de fixtures']
put(ev/'d29-cedro-progresso.json',current)
print('D29 progresso',len(rounds),'rodadas;33prioridades ligadas, semaceiteautomatico')
