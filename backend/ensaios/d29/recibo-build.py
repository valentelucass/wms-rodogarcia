from pathlib import Path
import hashlib,json,datetime,xml.etree.ElementTree as ET
root=Path(__file__).resolve().parents[3];be=root/'backend';ev=be/'evidencias'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
target=be/'target-d29-xinclude';reports=[];total={k:0 for k in ('tests','failures','errors','skipped')}
for p in sorted((target/'surefire-reports').glob('TEST-*.xml')):
 e=ET.parse(p).getroot(); row={'arquivo':str(p.relative_to(root)),'SHA256':sha(p),'suite':e.attrib.get('name')}
 for k in total:row[k]=int(e.attrib.get(k,0));total[k]+=row[k]
 archive=ev/('d29-build-xinclude-'+p.name)
 if not archive.exists():archive.write_bytes(p.read_bytes())
 if sha(archive)!=sha(p):raise SystemExit('D29_REPORT_ARCHIVE_DIVERGENTE')
 row['archive']=str(archive.relative_to(root));reports.append(row)
pre=read(ev/'d29-helper-versao-9D6857F466042D0F5BF812EE9FB9D69FA2A75D593D4C62CBEEE088D6614B2A1F.json')
comparison=[]
for row in pre['arquivos']:
 if row['arquivo'].startswith('target-d29-xinclude/') and row['arquivo'].endswith('.class'):
  p=be/row['arquivo'];comparison.append({'arquivo':row['arquivo'],'antes':row['sha256'],'depois':sha(p) if p.is_file() else None,'igual':p.is_file() and sha(p)==row['sha256']})
doc={'demanda':'D29','observadoUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'target':'backend/target-d29-xinclude','comando':'JAVA_HOME=JDK21; mvnw.cmd -B -ntp -Dwms.build.directory=target-d29-xinclude -DargLine=-Xmx768m clean verify','profileSqlServerIT':False,'SQLExecutadoPeloBuild':False,'exit':read(ev/'d29-build-xinclude-clean-verify.exit') if (ev/'d29-build-xinclude-clean-verify.exit').read_text().strip().startswith('{') else (ev/'d29-build-xinclude-clean-verify.exit').read_text().strip(),'total':total,'relatorios':reports,'jar':{'arquivo':str((target/'wms-backend-0.0.1-SNAPSHOT.jar').relative_to(root)),'SHA256':sha(target/'wms-backend-0.0.1-SNAPSHOT.jar')},'log':{'arquivo':'backend/evidencias/d29-build-xinclude-clean-verify.log','SHA256':sha(ev/'d29-build-xinclude-clean-verify.log')},'classesPreCompiladas':{'manifest':'9D6857F466042D0F5BF812EE9FB9D69FA2A75D593D4C62CBEEE088D6614B2A1F','comparacao':comparison,'todasIguais':all(r['igual'] for r in comparison)},'limite':'Build/local isolado nao prova SQL/HTTP nem aprovacao nativa/comercial.'}
(ev/'d29-build-xinclude-recibo.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('D29 build',total,'JAR',doc['jar']['SHA256'],'classes',len(comparison),'iguais',doc['classesPreCompiladas']['todasIguais'])
