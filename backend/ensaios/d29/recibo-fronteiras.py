from pathlib import Path
import json,hashlib,datetime,xml.etree.ElementTree as ET,zipfile,sys
root=Path(__file__).resolve().parents[3];be=root/'backend';ev=be/'evidencias';kind=sys.argv[1] if len(sys.argv)>1 else 'fronteiras'
if kind not in ('fronteiras','limites'):raise SystemExit('D29_RECIBO_ALVO_INVALIDO')
target=be/('target-d29-'+kind)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
prehash=(ev/('d29-'+kind+'-manifest-preverify.txt')).read_text().strip();pre=read(ev/('d29-helper-versao-'+prehash+'.json'));reports=[];total={k:0 for k in ['tests','failures','errors','skipped']}
for p in sorted((target/'surefire-reports').glob('TEST-*.xml')):
 e=ET.parse(p).getroot();dest=ev/('d29-build-'+kind+'01-'+p.name)
 if not dest.exists():dest.write_bytes(p.read_bytes())
 if sha(dest)!=sha(p):raise RuntimeError('D29_XML_ARCHIVE_DIVERGENTE')
 row={'arquivo':str(p.relative_to(root)),'archive':str(dest.relative_to(root)),'SHA256':sha(p),'classe':e.attrib['name'],'testcases':[]}
 for k in total:row[k]=int(e.attrib.get(k,0));total[k]+=row[k]
 for c in e.findall('testcase'):row['testcases'].append({'nome':c.attrib['name'],'falhou':c.find('failure') is not None or c.find('error') is not None,'segundos':c.attrib.get('time')})
 reports.append(row)
classes=[];sources=[]
for r in pre['arquivos']:
 p=be/r['arquivo']
 if r['arquivo'].startswith('target-d29-'+kind+'/') and r['arquivo'].endswith('.class'):classes.append({'arquivo':r['arquivo'],'pre':r['sha256'],'pos':sha(p),'igual':sha(p)==r['sha256']})
 if r['arquivo'].startswith('src/'):sources.append({'arquivo':r['arquivo'],'pre':r['sha256'],'pos':sha(p),'igual':sha(p)==r['sha256']})
libs=[]
with zipfile.ZipFile(target/'wms-backend-0.0.1-SNAPSHOT.jar') as z:
 for r in z.infolist():
  if r.filename.startswith('BOOT-INF/lib/') and r.filename.endswith('.jar'):
   p=be/'target-d29-helper/lib'/Path(r.filename).name;expected=hashlib.sha256(z.read(r.filename)).hexdigest().upper();libs.append({'arquivo':str(p.relative_to(root)),'JARentry':r.filename,'esperadoSHA256':expected,'atualSHA256':sha(p),'igual':expected==sha(p),'mtimeUtc':datetime.datetime.fromtimestamp(p.stat().st_mtime,datetime.timezone.utc).isoformat()})
doc={'demanda':'D29','observadoUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'comando':'JAVA_HOME=JDK21; MAVEN_OPTS=-Xmx384m; mvnw.cmd -B -ntp -Dwms.build.directory=target-d29-fronteiras -DargLine=-Xmx768m clean verify','exit':int((ev/'d29-build-fronteiras-clean-verify01.exit').read_text()),'SQLServerIT':False,'total':total,'relatorios':reports,'manifestAntesExecucao':prehash,'manifestCapturadoUtc':pre['capturadoUtc'],'classesAntesDepois':classes,'todasClassesIguais':all(r['igual'] for r in classes),'fontesAntesDepois':sources,'todasFontesIguais':all(r['igual'] for r in sources),'jar':{'arquivo':str((target/'wms-backend-0.0.1-SNAPSHOT.jar').relative_to(root)),'SHA256':sha(target/'wms-backend-0.0.1-SNAPSHOT.jar'),'mesmosBytes472':sha(target/'wms-backend-0.0.1-SNAPSHOT.jar')==sha(be/'target-d29-financeiro-ampliado/wms-backend-0.0.1-SNAPSHOT.jar')},'logSHA256':sha(ev/'d29-build-fronteiras-clean-verify01.log'),'bibliotecasHelperAtuais':libs,'bibliotecasIguaisAoJAR':all(r['igual'] for r in libs),'limiteBibliotecas':'Comparacao atual dos bytes extraidos; nao retroatesta momento de extracao nem reconstrói arquivos iniciais ausentes.','limite':'486 locais/H2, sem SQLServer real, sem aceite comercial/corteDEV/fiscalexterno/dispositivo. 9 casos parser originais e3 da outra retomada se sobrepoem; nao somar como12 novas facetas nativas. Dois casosfinanceiros retomada pertencem ao escritor daquela entrada, preservados/reexecutados no build atual.'}
doc['comando']=doc['comando'].replace('target-d29-fronteiras','target-d29-'+kind)
doc['exit']=int((ev/('d29-build-'+kind+'-clean-verify01.exit')).read_text())
doc['logSHA256']=sha(ev/('d29-build-'+kind+'-clean-verify01.log'))
doc['limite']=doc['limite'].replace('486 locais/H2',str(total['tests'])+' locais/H2')
retomada=next((r['tests'] for r in reports if r['classe'].endswith('.D29FinanceiroRetomadaTest')),0)
doc['limite']=doc['limite'].replace('Dois casosfinanceiros retomada',str(retomada)+' casosfinanceiros retomada')
(ev/('d29-build-'+kind+'-recibo.json')).write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('D29 build atual',total,'classes',len(classes),'iguais',doc['todasClassesIguais'],'fontesIguais',doc['todasFontesIguais'],'JAR',doc['jar']['SHA256'],'libsIguais',doc['bibliotecasIguaisAoJAR'])
