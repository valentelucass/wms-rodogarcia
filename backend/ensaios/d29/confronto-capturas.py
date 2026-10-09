from pathlib import Path
import json,hashlib,datetime,uuid
root=Path(__file__).resolve().parents[3];ev=root/'backend/evidencias'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
path=ev/'d29-D29964B4883-http.json';doc=read(path);round_id=doc['rodada'];ids=doc['ids'];rows=[]
def get(route):
 matches=[(n,c) for n,c in enumerate(doc['cases']) if c.get('metodo')=='GET' and c.get('actual')==200 and c.get('caso','').startswith('D29 GET FINAL estado atual ') and c.get('rota')==route]
 if not matches:raise RuntimeError('D29_GET_FINAL_DOCUMENTADO_AUSENTE')
 return matches[-1]
def check(name,atomos,expected,actual,passed,refs):
 rows.append({'caso':name,'atomos':atomos,'esperadoIndependente':expected,'obtido':actual,'aprovado':bool(passed),'fontesHTTP':[{'arquivo':str(path.relative_to(root)),'SHA256':sha(path),'caseIndex':n,'caso':c['caso'],'rota':c['rota'],'requestId':c['requestId'],'perfil':c['perfil'],'instanteRespostaUtc':c['instanteRespostaUtc'],'corpoSHA256':c['corpoSHA256']} for n,c in refs]})
units=[];products=[];stocks=[]
for n,c in enumerate(doc['cases']):
 if c.get('metodo')=='GET' and c.get('actual')==200 and c.get('caso','').startswith('D29 GET FINAL estado atual /api/v1/unidades-logisticas/') and c['rota'].endswith('/estoque'):units.append((n,c))
for n,c in units:
 u=c['resposta']['unidade'];pr=get('/api/v1/produtos/'+str(u['produtoId']));products.append(pr);stocks.append(get('/api/v1/estoque/saldo?clienteId=61&armazemId=29&produtoId='+str(u['produtoId'])))
check('D29 DTO proprietario explicito',['RN02.01'],{'clienteId':61},[c['resposta']['unidade']['clienteId'] for n,c in units],len(units)==2 and all(c['resposta']['unidade']['clienteId']==61 and pr['resposta']['clienteId']==61 for (n,c),(pn,pr) in zip(units,products)),units+products)
check('D29 SKU corresponde ao produto',['RN02.02'],{'SKU1':round_id+'SKU','SKU2':round_id+'SKU2'},[c['resposta']['unidade']['sku'] for n,c in units],len(units)==2 and all(c['resposta']['unidade']['sku']==pr['resposta']['sku'] and c['resposta']['unidade']['produtoId']==pr['resposta']['id'] for (n,c),(pn,pr) in zip(units,products)),units+products)
check('D29 quantidade e unidade explicitas',['RN02.03'],{'quantidadePorUL':10,'unidade':'UN'},[{'quantidade':c['resposta']['unidade']['quantidade'],'unidade':c['resposta']['unidade']['unidadeMedida']} for n,c in units],len(units)==2 and all(c['resposta']['unidade']['quantidade']==10 and c['resposta']['unidade']['unidadeMedida']=='UN' and pr['resposta']['unidadeMedida']=='UN' for (n,c),(pn,pr) in zip(units,products)),units+products)
codes=[c['resposta']['unidade']['codigo'] for n,c in units];valid=all(str(uuid.UUID(v))==v for v in codes)
check('D29 identidade UL distinta de DUN e SKU',['RN02.04'],{'codigosUUIDDistintos':2,'naoDUN':True},codes,valid and len(set(codes))==2 and all(c['resposta']['unidade']['codigo'] not in (c['resposta']['unidade']['codigoDun'],c['resposta']['unidade']['sku']) for n,c in units),units)
check('D29 armazem explicito',['RN02.06'],{'armazemId':29},[c['resposta']['unidade']['armazemId'] for n,c in units],len(units)==2 and all(c['resposta']['unidade']['armazemId']==29 for n,c in units),units)
order=get('/api/v1/pedidos-saida/71');o=order[1]['resposta']
check('D29 situacao pedido e condicao mercadoria separadas',['RN02.05'],{'pedido':'RESERVADO','mercadoria':['BOA','BOA']},{'pedido':o['situacao'],'mercadoria':[c['resposta']['unidade']['condicao'] for n,c in units]},o['situacao']=='RESERVADO' and all(c['resposta']['unidade']['condicao']=='BOA' for n,c in units),[order]+units)
check('D29 saldo conserva total e categorias distintas',[],{'porSKU':{'fisico':10,'pendente':0,'unitizado':10,'reservado':8,'bloqueado':2,'disponivel':0}},[c['resposta'] for n,c in stocks],all(c['resposta']['fisicoTotal']==10 and c['resposta']['pendenteUnitizacao']==0 and c['resposta']['fisicoUnitizado']==10 and c['resposta']['reservado']==8 and c['resposta']['bloqueado']==2 and c['resposta']['disponivel']==0 and c['resposta']['fisicoTotal']==c['resposta']['pendenteUnitizacao']+c['resposta']['fisicoUnitizado'] and c['resposta']['fisicoUnitizado']==c['resposta']['disponivel']+c['resposta']['reservado']+c['resposta']['bloqueado'] for n,c in stocks),stocks)
signal=root/'orchestracao/.runtime/d29-prumo-select-liberar-D29964B4883.json';s=read(signal)
result={'demanda':'D29','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'classeProva':'CONFRONTO_ANALITICO_NOVO_DE_CAPTURAS_HTTP_D29_JA_EXECUTADAS','novoHTTP':False,'novoSQL':False,'novaMutacao':False,'checks':len(rows),'falhas':sum(not r['aprovado'] for r in rows),'cases':rows,'SQLassociado':{'sinal':str(signal.relative_to(root)),'SHA256':sha(signal),'checks':s['checks'],'falhas':s['falhas'],'focalChecks':s['focalChecks'],'limite':s['limite']},'JAR':ids['jarSha256'],'helperExecutado':ids['helperFontesCompilados']['hashConjunto'],'limite':'Reavalia campos e aritmetica dos GETs capturados19:01; nao declara HTTP novo20h, imutabilidade geral/cobertura de pais ou ausencia de falhaSQLtardia.'}
(ev/'d29-capturas-confronto-cedro.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('D29 confronto analitico',result['checks'],result['falhas'],'semHTTP/SQLnovo')
if result['falhas']:raise SystemExit(1)
