from pathlib import Path
import json, hashlib, datetime

backend=Path(__file__).resolve().parents[2]
ev=backend/'evidencias'
def read(p): return json.loads(p.read_text(encoding='utf-8-sig'))
build=read(ev/'d28-build.json')
docs=[(p,read(p)) for p in sorted(ev.glob('d28-D28*-http.json'))]
real=[(p,d) for p,d in docs if not str(d['fase']).startswith('offline')]
offline=[(p,d) for p,d in docs if str(d['fase']).startswith('offline')]
rounds=[]
for p,d in real:
    http=[c for c in d['cases'] if 'metodo' in c]
    assertions=[c for c in d['cases'] if 'metodo' not in c]
    rounds.append({'rodada':d['rodada'],'fase':d['fase'],'bloqueio':d['bloqueio'],
      'fonteHTTP':str(p.relative_to(backend.parent)).replace('\\','/'),
      'etapa':d['ids'].get('etapa'),'clienteId':d['ids'].get('clienteId'),'armazemId':d['ids'].get('armazemId'),
      'jarSha256':d['ids'].get('jarSha256'),'jarPid':d['ids'].get('jarPid'),
      'porta':d['ids'].get('porta'),'issuerPorta':d['ids'].get('issuerPorta'),
      'jarEncerrado':d['ids'].get('jarEncerrado'),'portasLivres':d['ids'].get('portasLivres'),
      'complementoDe':d['ids'].get('complementoLeituraDe'),'HTTP':len(http),'assertivas':len(assertions),
      'divergencias':[{'caso':c['caso'],'esperado':c['expected'],'obtido':c['actual']} for c in d['cases'] if c.get('estado')=='falhou'],
      'requestIdPresente':sum(c.get('requestIdPresente',False) for c in http),
      'requestIdValorCapturado':sum(bool(c.get('requestId')) for c in http)})
sources=[]
for root in [backend/'src',backend/'ensaios/d28']:
    for p in sorted(root.rglob('*')):
        if p.is_file(): sources.append({'arquivo':str(p.relative_to(backend.parent)).replace('\\','/'),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
result={'incremento':'D28','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'build':build,
  'rodadas':rounds,'HTTPReal':sum(r['HTTP'] for r in rounds),'assertivasReais':sum(r['assertivas'] for r in rounds),
  'offlineRodadas':[d['rodada'] for _,d in offline],'offlineNuncaSomadoAoHTTP':True,
  'escritaNegocio':'somente HTTP','producao':False,'ddl':False,'sqlserverITVazio':False,
  'fontesAtuais':sources,'limites':['SQL novo independente e revisão exigidos; fase aguardando SELECT não é aprovado.',
    'Witness DMV online restrito recusado SQL300; HTTP/JVM e SELECT posterior separados.',
    'Primeiras duas rodadas capturam presença RequestId; valores exatos nos complementos e próximas fases.',
    'D284DC609CC positivo científico recusado corretamente SALDO_INSUFICIENTE; roteiro sem fixture de estoque, não defeito backend.'],
  'proximo':'Fecho de recibos/hashes/processos; Farol consolida e Vigia revisa. Nenhuma nova jornada autorizada.'}
(ev/'d28-manifest.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
(ev/'d28-cedro-progresso.json').write_text(json.dumps({k:v for k,v in result.items() if k!='fontesAtuais'},ensure_ascii=False,indent=2),encoding='utf-8')
# A matriz causal detalhada e produzida por cobertura-final.py; nunca substituir por links genericos.
lines=['# D28 — manifest Cedro', '',f'Build novo: {int(build["tests"])}/0/0/0 em {build["xmlCount"]} XMLs; SHA `{build["jarSHA"]}`.',
  'Mesmo conteúdo do último JAR D27; comando, execução, relatórios e jornadas D28 são novos. Sem profile sqlserver-it.', '',
  '| Rodada | Etapa | HTTP | Assertivas | Fase | Família |', '| --- | --- | ---: | ---: | --- | --- |']
for r in rounds: lines.append(f'| {r["rodada"]} | {r["etapa"]} | {r["HTTP"]} | {r["assertivas"]} | {r["fase"]} | {r["clienteId"]}/{r["armazemId"]} |')
lines += ['', 'Offline é separado de HTTP real. GET complementar não é nova fixture nem repetição de mutação. SQL e aprovação final continuam separados.', '',
  'Divergência D284DC609CC: cinco extremos400 passaram; positivo201 esperado recebeu409 SALDO_INSUFICIENTE por estoque ausente no roteiro. Preservado; não é defeito backend.']
(ev/'d28-manifest.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
print(json.dumps({'rodadas':len(rounds),'HTTP':result['HTTPReal'],'assertivas':result['assertivasReais'],'status':'manifest atualizado, sem inferir aprovação'}))
