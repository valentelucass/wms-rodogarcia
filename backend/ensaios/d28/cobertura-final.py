from pathlib import Path
import json, hashlib, datetime, re

root=Path(__file__).resolve().parents[3]
ev=root/'backend/evidencias'
runtime=root/'orchestracao/.runtime'
def read(p):
    b=p.read_bytes()
    return json.loads(b.decode('utf-16') if b.startswith((b'\xff\xfe',b'\xfe\xff')) else b.decode('utf-8-sig'))
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest().upper()
def rel(p): return p.relative_to(root).as_posix()
def dump(p,d): p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
build=read(ev/'d28-build.json')
docs={d['rodada']:(p,d) for p in sorted(ev.glob('d28-D28*-http.json')) if not (d:=read(p))['fase'].startswith('offline')}
inventory=read(runtime/'d28-vigia-inventario.json')
first='D287D721FEA'; cargo='D289E87D622'; xml='D288163F01B'; variants='D28925E295C'; concurrent='D28EFFF454F'; saldo='D284B2EA65C'; final='D282DDA1365'

# Escopos por trigger; somente casos nomeados/IDs desta D28. Uma prova compartilhada nao soma novo caso.
rules={
 4:([first],r'AC13.*(aplica|replay35|saldo final|calculo|cálculo)'),
 6:([first],r'AC13.*(45|35|reserva|revisão|revisao)'),
 7:([first],r'RN22.*(XXE|DTD|extern|SKU|unidade|alcance|insuf|rollback|origem|replay|UUID|duplicidade)'),
 8:([xml],r'RN22 limite (numerico|rollback GET|GET sem efeito)'),
 9:([xml],r'RN22 cientifico|100E2147483647'),
 12:([concurrent,final,variants],r'GET FINAL saida|consulta /api/v1/pedidos-saida/[0-9]+$|cancelamento|reversao|reversão'),
 13:([first,saldo],r'consulta /api/v1/estoque/saldo|saldo confirmado'),
 15:([first,cargo,concurrent],r'replay|único|unico'),
 16:([concurrent,variants,saldo,'D289A0CF1ED'],r'cancelamento|responsabilidade|saldo confirmado|entrada28|precondicao|efetiva4|XML sozinho|falta XML nao libera|saldo fisico inclui4'),
 18:([cargo],r'memória v1|memoria v1|versão anterior|reabertura|v2 aprovada|GET snapshot|fonte/estado'),
 21:([cargo,first,saldo,final],r'GET FINAL|replay|memória|memoria|saldo confirmado'),
 23:([concurrent],r'pedido saida CONC|saldo fisico 100'),
 25:([first,variants],r'movimento replay|FIFO|origens|físico/origens|fisico/origens'),
 26:([concurrent,final],r'reserva concorrente [AB]|concorrência|concorrencia|GET FINAL saida'),
 27:([concurrent],r'reserva concorrente [AB]'),
 28:([cargo],r'RN27|BE13|financeiro|AC06 memória'),
 29:([variants,'D28079AE038','D28E89EBAE4','D28BBB4F694'],r'RN15|RN17|RN23|RN08|AC04|AC06|AC08|filtro|página|pagina|metadados|alcance|inexistente|estoque|saldo final|GET FINAL'),
}
offline={
 10:[ev/'d28-guardas-offline.json',runtime/'d28-prumo-preflight.json'],
 11:[ev/'d28-https-falha-offline.json'],
 13:[ev/'d28-sql-quoting-offline.json'],
 14:[ev/'d28-processo-limitado-offline.json'],
 15:[runtime/'d28-prumo-comparadores.json'],
 16:[ev/'d28-saldo-xml-guarda-offline.json'],
 17:[runtime/'d28-prumo-capacidade-witness.json',runtime/'d28-prumo-preflight.json'],
 21:[runtime/'d28-prumo-comparadores.json'],
 22:[ev/'d28-readiness-offline.json'],
 23:[ev/'d28-aguardador-offline.json'],
 24:[runtime/'d28-prumo-comparadores.json',runtime/'d28-prumo-comparadores-complemento.json'],
 27:[ev/'d28-respostas-offline.json'],
}
extra_sql={4:[first],10:[first],11:[],13:[first,saldo],14:[concurrent],15:[first,cargo],17:[concurrent],19:[first,cargo,variants,concurrent],20:[first,cargo,xml,variants,concurrent,saldo,final],21:[first,cargo,xml,variants,saldo,final],23:[concurrent],24:[concurrent]}
sqlpatterns={1:r'contagem',2:r'carga',3:r'temporal|PENDENTE|histórico|historico',4:r'historico|histórico|fato|AJUSTE',5:r'zero|ocupacao|ocupação|ajuste|contagem',6:r'marco|endere|reserva|revis|ajuste|unidade',7:r'XML|RN22|uuid|pedido.*3[456]',8:r'XML|RN22|limite|uuid',9:r'XML|RN22|cientifico|uuid',12:r'concorr|cancel|saida|saída|reserva',13:r'saldo',14:r'concorr',15:r'replay|snapshot|auditoria|filh',16:r'saldoXML|saldo|entrada|cancel|responsabilidade',18:r'memoria|memória|demonstrativo|ciclo|calculo|cálculo',21:r'GET|memoria|memória|saldo|versao|versão',23:r'concorr',25:r'FIFO|marco|data|instante|versao|versão',26:r'concorr',27:r'concorr',28:r'financeiro|memoria|memória|ciclo|calculo|cálculo',29:r'FIFO|avaria|GRIS|tarifa|reserva|lista|origem|saldo|quarentena'}

def case_ref(round,i):
    p,d=docs[round];c=d['cases'][i];body=c.get('payload') or {}
    return {'rodada':round,'arquivo':rel(p),'arquivoSHA256Atual':sha(p),'indiceZero':i,'numeroCaso':i+1,'caso':c['caso'],
      'metodo':c.get('metodo'),'rota':c.get('rota'),'perfilReal':c.get('perfil'),'expected':c.get('expected'),'actual':c.get('actual'),
      'estado':c.get('estado'),'UUID':body.get('operacaoId') if isinstance(body,dict) else None,'RequestId':c.get('requestId'),
      'RequestIdPresente':c.get('requestIdPresente'),'familia':{'clienteId':d['ids'].get('clienteId'),'armazemId':d['ids'].get('armazemId')},'jarSHA256':d['ids'].get('jarSha256')}

sqlcache={}
def sql_ref(round,n):
    signal=runtime/f'd28-prumo-select-liberar-{round}.json'
    summary=runtime/f'd28-prumo-select-{round}-resumo.json'
    focal=runtime/f'd28-prumo-focal-{round}.json'
    s=read(signal) if signal.exists() else (read(summary) if summary.exists() else {})
    photo=root/s.get('arquivoEvidencia',s.get('arquivo','NAO_PUBLICADO'))
    ps=read(photo) if photo.is_file() else {}
    f=read(focal) if focal.exists() else {}
    patt=re.compile(sqlpatterns.get(n,'.'),re.I)
    checks=[]
    for owner,data,path in [('SELECT',ps,photo),('focal',f,focal)]:
        for i,c in enumerate(data.get('casos',[])):
            if patt.search(c.get('caso','')):
                checks.append({'origem':owner,'arquivo':rel(path),'indiceZero':i,**c})
    return {'rodada':round,'contratoCedro':f'backend/evidencias/d28-select-{round}.json','contratoSHA256Atual':sha(ev/f'd28-select-{round}.json'),
      'resumoPrumo':rel(summary) if summary.exists() else None,'sinal':rel(signal) if signal.exists() else None,
      'foto':rel(photo) if photo.is_file() else None,'fotoSHA256':sha(photo) if photo.is_file() else None,
      'fotoHashPublicado':s.get('evidenciaHash'),'httpHashNoMomentoDaFoto':ps.get('httpHash'),
      'DB_NAME':ps.get('alvo',{}).get('banco'),'login':ps.get('alvo',{}).get('login'),'inicioUtc':ps.get('inicioUtc'),'fimUtc':ps.get('fimUtc'),
      'checks':s.get('checks',ps.get('checks')),'falhas':s.get('falhas',ps.get('falhas')),
      'focal':rel(focal) if focal.exists() else None,'focalSHA256':sha(focal) if focal.exists() else None,
      'focalChecks':f.get('checks'),'focalFalhas':f.get('falhas'),'assertivasCausais':checks}

lines=[]
for item in inventory['linhas']:
    n=int(item['id'][-2:]);selected=set()
    for c in item.get('casosD28',[]):
        if c.get('rodada') in docs and isinstance(c.get('indice'),int) and 0<=c['indice']<len(docs[c['rodada']][1]['cases']):selected.add((c['rodada'],c['indice']))
    if n in rules:
        rs,patt=rules[n]
        for r in rs:
            for i,c in enumerate(docs[r][1]['cases']):
                if re.search(patt,c['caso'],re.I):selected.add((r,i))
    rs=sorted({r for r,_ in selected}|set(extra_sql.get(n,[])))
    artifacts=[]
    for p in offline.get(n,[]):
        if p.exists():
            d=read(p);artifacts.append({'arquivo':rel(p),'SHA256':sha(p),'checks':d.get('checks'),'falhas':d.get('falhas'),'SQLExecutado':d.get('SQLExecutado',d.get('sqlExecutado')),'utc':d.get('utc',d.get('observadoEm'))})
    source=item.get('fonteAtual')
    if source and (root/source['arquivo']).exists():source={**source,'SHA256ConferidoAgora':sha(root/source['arquivo'])}
    status='EVIDENCIA_ATUAL_PUBLICADA_PARA_REVISAO'
    limit='Escopo focal D28; nao homologa backend inteiro. Historico apenas fundamenta trigger.'
    if n==17:status='WMSDEV_CONFIRMADO_WITNESS_NATIVE_NAO_COBERTO';limit='DMV online recusada SQL300; sem grant, sa ou novo observador. HTTP/JVM+SELECT posterior sao provas distintas.'
    if n in (22,23,24):limit='Offline novo sem SQL/credencial/API; nao comprova witness nativo online nem execucao do aguardador completo em banco.'
    if n==25:limit='DTO e SQL comparados na precisao disponibilizada; nao inferir micros ausentes nem alterar fatos.'
    if n==28:limit='Minimo50 aprovado usa corte real06->07 e contrato ficticio; previsoes54/53.75 abertas sao distintas. Nao fabrica historia nem emite NFS-e.'
    lines.append({'id':item['id'],'historico':item['historico'],'classificacao':item['categoria'],'correcao':item['correcao'],
      'fonteAtual':source,'fontesNegocio':item.get('fontesNegocio',[]),'trigger':item['criterioMinimo'],'estadoCedro':status,
      'casosD28':[case_ref(r,i) for r,i in sorted(selected)],'offlineOuPreflight':artifacts,
      'SQLNovo':[sql_ref(r,n) for r in rs],'limite':limit,'aceiteIndependente':'Farol e Vigia; este arquivo reporta prova, nao aprova matriz central.'})

profiles={}
for r,(p,d) in docs.items():
    for i,c in enumerate(d['cases']):
        if 'metodo' not in c:continue
        key=(c.get('perfil'),c['metodo'],c.get('rota'),c['expected'],c['actual'],c.get('estado'))
        profiles.setdefault(key,[]).append({'rodada':r,'indiceZero':i,'RequestId':c.get('requestId'),'UUID':(c.get('payload') or {}).get('operacaoId') if isinstance(c.get('payload'),dict) else None})
dump(ev/'d28-perfis.json',{'derivadoDeCasosReais':True,'grupos':[{'perfil':k[0],'metodo':k[1],'rota':k[2],'expected':k[3],'actual':k[4],'estado':k[5],'casos':v} for k,v in profiles.items()]})
links={'incremento':'D28','utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'jarSHA256':build['jarSHA'],
 'inventarioOrigem':rel(runtime/'d28-vigia-inventario.json'),'inventarioSHA256':sha(runtime/'d28-vigia-inventario.json'),
 'contagem':'29 criterios; provas compartilhadas permanecem um caso HTTP no manifest. Nao somar casos por linha.',
 'httpHashLifecycle':'Foto SQL captura HTTP em hold; JSON HTTP final acrescenta cleanup/sinal. Hash atual pode diferir sem repetir negocio; fotos e hashes de captura preservados.',
 'itens':lines,'perfis':rel(ev/'d28-perfis.json'),'backendTotalHomologado':False,'matrizCentralDono':'Farol'}
dump(ev/'d28-cobertura.json',links)
md=['# D28 — cobertura causal Cedro','',f'Artefato unico `{build["jarSHA"]}`. Casos/UUID/IDs/RequestId e assertivas SQL completas em `d28-cobertura.json`; indices com base zero e numero de caso com base um.','',
 '| Criterio | Classificacao | Casos D28 vinculados | SQL novo | Offline/preflight | Limite |','| --- | --- | ---: | --- | --- | --- |']
for l in lines:
    md.append('| '+l['id']+' | '+l['classificacao']+' | '+str(len(l['casosD28']))+' | '+', '.join(s['rodada']+':'+str(s['checks'])+'/'+str(s['falhas']) for s in l['SQLNovo'])+' | '+', '.join(Path(o['arquivo']).name for o in l['offlineOuPreflight'])+' | '+l['limite']+' |')
md+=['','Nenhuma linha usa historico como prova D28. Offline nao soma HTTP/SQL. R17 permanece com limite material SQL300. Reds e greens separados no recibo `d28-cedro.md`. Aceite e registros centrais pertencem a Farol/Vigia.']
(ev/'d28-cobertura.md').write_text('\n'.join(md)+'\n',encoding='utf-8')
print(json.dumps({'criterios':len(lines),'vinculosHTTP_semSomar':sum(len(l['casosD28']) for l in lines),'SQL_fotos':len({s['foto'] for l in lines for s in l['SQLNovo']}),'offlineArquivos':len({o['arquivo'] for l in lines for o in l['offlineOuPreflight']})}))
