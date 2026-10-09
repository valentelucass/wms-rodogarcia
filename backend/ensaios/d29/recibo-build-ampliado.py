from pathlib import Path
import hashlib,json,datetime,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[3];be=root/'backend';ev=be/'evidencias';target=be/'target-d29-financeiro-ampliado'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
totals={k:0 for k in ('tests','failures','errors','skipped')};reports=[]
for p in sorted((target/'surefire-reports').glob('TEST-*.xml')):
 e=ET.parse(p).getroot();r={'arquivo':str(p.relative_to(root)),'SHA256':sha(p),'classe':e.attrib['name']}
 for k in totals:r[k]=int(e.attrib.get(k,0));totals[k]+=r[k]
 archive=ev/('d29-build-financeiro-ampliado-'+p.name)
 if not archive.exists():archive.write_bytes(p.read_bytes())
 if sha(archive)!=sha(p):raise SystemExit('D29_REPORT_DIVERGENTE')
 r['archive']=str(archive.relative_to(root));reports.append(r)
pre=read(ev/'d29-helper-versao-7F797D08D009570C8AB5D720F9AA29D114365CEA28BCD14A675A027401FF9F87.json');comparison=[]
for r in pre['arquivos']:
 if r['arquivo'].startswith('target-d29-financeiro-ampliado/') and r['arquivo'].endswith('.class'):
  p=be/r['arquivo'];comparison.append({'arquivo':r['arquivo'],'antes':r['sha256'],'depois':sha(p),'igual':sha(p)==r['sha256']})
doc={'demanda':'D29','observadoUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'comando':'JAVA_HOME=JDK21; MAVEN_OPTS=-Xmx384m; mvnw.cmd -B -ntp -Dwms.build.directory=target-d29-financeiro-ampliado -DargLine=-Xmx768m clean verify','SQLServerIT':False,'exit':int((ev/'d29-build-financeiro-ampliado-clean-verify.exit').read_text()),'total':totals,'relatorios':reports,'jar':{'arquivo':str((target/'wms-backend-0.0.1-SNAPSHOT.jar').relative_to(root)),'SHA256':sha(target/'wms-backend-0.0.1-SNAPSHOT.jar'),'mesmosBytesJARXinclude':sha(target/'wms-backend-0.0.1-SNAPSHOT.jar')==sha(be/'target-d29-xinclude/wms-backend-0.0.1-SNAPSHOT.jar')},'log':{'arquivo':'backend/evidencias/d29-build-financeiro-ampliado-clean-verify.log','SHA256':sha(ev/'d29-build-financeiro-ampliado-clean-verify.log')},'classesCapturadasAntes':comparison,'todasClassesIguais':all(r['igual'] for r in comparison),'manifestAntesBuild':pre['hashConjunto'],'localDatas':'Somente fontes isoladas/H2 do build; WMS_DEV/Clock real intactos','limite':'Não equivale homologacao operacional/comercial, dispositivo, SQLServer ou aprovacao nativa.'}
(ev/'d29-build-financeiro-ampliado-recibo.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('D29 build',totals,'classes',len(comparison),'iguais',doc['todasClassesIguais'],'JAR',doc['jar']['SHA256'])
