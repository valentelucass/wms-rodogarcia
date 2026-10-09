from pathlib import Path
import re,json,hashlib,datetime,subprocess
root=Path(__file__).resolve().parents[3];ev=root/'backend/evidencias';ev.mkdir(exist_ok=True)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def write(p,d):p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def source(p,line,text):return {'arquivo':p.relative_to(root).as_posix(),'linha':line,'SHA256':sha(p),'texto':text}
rows=[]
for name in ['03-regras-de-negocio.md','08-cenarios-de-validacao.md']:
 p=root/'docs'/name
 for line,text in enumerate(p.read_text(encoding='utf-8').splitlines(),1):
  if re.match(r'\| (RN\d+|I\d+|PR\d+|V\d+) \|',text):
   parts=[v.strip() for v in text.strip('|').split('|')]
   rows.append({'id':parts[0],'origem':'requisito PDF' if parts[0].startswith('RN') else 'cenario/protecao/interpretacao identificada',
    'fonte':source(p,line,text),'requisito':parts[1],'esperadoIndependente':parts[2] if parts[0].startswith('V') else parts[1],
    'casosD29':[],'evidencias':[],'status':'inventariado; decompor/ensaiar','limite':None})
for name,pattern,category in [('11-alinhamentos-apos-respostas.md',r'^## (AC\d+) (.+)$','proposta/interpretacao AC, sem aprovacao comercial'),('10-respostas-recebidas-2026-10-05.md',r'^## (.+)$','resposta recebida consolidada')]:
 p=root/'docs'/name;lines=p.read_text(encoding='utf-8').splitlines();headers=[(i,re.match(pattern,t)) for i,t in enumerate(lines) if re.match(pattern,t)]
 for n,(i,m) in enumerate(headers):
  end=headers[n+1][0] if n+1<len(headers) else len(lines);sid=m.group(1) if name.startswith('11') else 'RESP-'+str(n+1).zfill(2)
  body='\n'.join(lines[i+1:end]).strip()
  if name.startswith('11') and not sid.startswith('AC'):continue
  rows.append({'id':sid,'origem':category,'fonte':source(p,i+1,body),'requisito':m.group(0).lstrip('# '),'esperadoIndependente':body,'casosD29':[],'evidencias':[],'status':'inventariado; decompor/ensaiar','limite':None})
contracts=[]
for num in ['12','14','18','20','22','24','27','29','31','33','34','35']:
 p=next((root/'docs').glob(num+'-*.md'));heading='';fenced=False;items=[]
 for line,t in enumerate(p.read_text(encoding='utf-8').splitlines(),1):
  if t.startswith('```'):fenced=not fenced;continue
  if fenced or not t.strip():continue
  if t.startswith('#'):heading=t.lstrip('# ');continue
  if t.startswith('| ---') or re.match(r'^\| (ID|Item|Situação|Rota|Operação|Método|Campo)',t):continue
  # Texto completo das clausulas candidatas, sem usar assinatura HTTP como criterio.
  if re.search(r'(?i)não|nao|somente|apenas|deve|exige|preserv|recus|confir|mant[eé]|replay|repeti|obrigat|permite|retorn|at[oô]mic|quarentena|FIFO|limite|cobr|fiscal|status|vers[aã]o',t):
   sid=f'CT{num}-L{line:04d}'
   atomics=[a.strip() for a in re.split(r';\s+|(?<=[.!])\s+(?=[A-ZÁÉÍÓÚ])',t) if a.strip()]
   for k,a in enumerate(atomics,1):
    items.append({'id':sid+f'-A{k:02d}','secao':heading,'fonte':source(p,line,a),'origem':'clausula candidata do contrato; conferir vigencia/qualificador',
      'esperadoIndependente':a,'casosD29':[],'evidencias':[],'status':'inventariado; decompor/ensaiar','limite':'Assinatura/HTTP200 isolado nao atende clausula.'})
 contracts.extend(items)
external={'RN30':'Frontend/coletor fora escopo; contrato local e autenticacao permanecem ensaiaveis.','V18':'Emissor fiscal externo nao acionado; estados/referencias locais ensaiaveis.','V22':'Restauracao fora escopo; nao simular como recuperacao comprovada.','V26':'Duas chegadas atuais ensaiaveis; dias distintos nao fabricar retroativamente.','V42':'Piloto e desempenho no armazem dependem de operacao/equipamento/metas.','V43':'Documento etiqueta/contingencia locais; falha real de coletor/rede/impressora fora escopo.','V44':'Conciliacao local ensaiavel; restauracao/disponibilidade real externa.','AC15':'Backup/restauracao e metas reais fora escopo.','AC16':'Homologacao comercial/operacional e dispositivos externos.'}
for r in rows:
 if r['id'] in external:r['limite']=external[r['id']]
result={'demanda':'D29','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'estado':'INVENTARIO_INICIAL_SEM_ACEITE','regrasCenariosRespostasAC':rows,'clausulasContratos':contracts,
 'criterio':'Decompor caso/oraculo de estado, quantidades, memoria, historico, rollback e permissao. HTTP/métodos nao equivalem cobertura.',
 'contagens':{'RN':sum(r['id'].startswith('RN') for r in rows),'I':sum(re.fullmatch(r'I\d+',r['id']) is not None for r in rows),'PR':sum(r['id'].startswith('PR') for r in rows),'V':sum(r['id'].startswith('V') for r in rows),'AC':sum(r['id'].startswith('AC') for r in rows),'respostasSecoes':sum(r['id'].startswith('RESP') for r in rows),'clausulasCandidatas':len(contracts)}}
write(ev/'d29-inventario-cedro.json',result)
md=['# D29 — inventário inicial Cedro','',json.dumps(result['contagens'],ensure_ascii=False),'','Todas as cláusulas candidatas e textos de origem estão no JSON. Inventário não é cobertura comprovada. Lume/Vigia revisarão vigência e decomposição; Farol mantém matriz central.','', '| ID | Origem | Fonte | Situação | Limite |','| --- | --- | --- | --- | --- |']
for r in rows:md.append(f'| {r["id"]} | {r["origem"]} | {r["fonte"]["arquivo"]}:{r["fonte"]["linha"]} | {r["status"]} | {r["limite"] or ""} |')
(ev/'d29-inventario-cedro.md').write_text('\n'.join(md)+'\n',encoding='utf-8')
baseline=ev/'d29-git-baseline.txt'
if not baseline.exists():baseline.write_text(subprocess.run(['git','status','--short'],cwd=root,capture_output=True,text=True).stdout,encoding='utf-8')
old=ev/'d29-fontes-baseline.json'
if not old.exists():write(old,[{'arquivo':p.relative_to(root).as_posix(),'SHA256':sha(p)} for folder in ['backend/src','backend/ensaios/d26','backend/ensaios/d27','backend/ensaios/d28','database/migrations'] for p in sorted((root/folder).rglob('*')) if p.is_file()])
write(ev/'d29-cedro-progresso.json',{'demanda':'D29','utc':result['utc'],'fase':'INVENTARIO_E_PREPARO','inventario':'backend/evidencias/d29-inventario-cedro.json','contagens':result['contagens'],'APIIniciada':False,'SQLNegocioExecutado':False,'pendencias':['revalidar preflight D29 no runner','build novo','oraculos e jornadas novas','SELECT por contratos','revisao/cleanup/recibo']})
print(json.dumps(result['contagens']))
