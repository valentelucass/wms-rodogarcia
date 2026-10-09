"""Derivação estática, sem JVM, rede ou execução backend. Revise o diff ao atualizar."""
from pathlib import Path
import re, json, hashlib

base = Path(__file__).resolve().parents[2]
java = base/'backend/src/main/java/br/com/rodogarcia/wms'
out = base/'frontend/src/contracts'
out.mkdir(parents=True, exist_ok=True)
hashes, reads, enum_sources = {}, {}, {}
def source_text(file, role):
    key=str(file.relative_to(base)).replace('\\','/')
    content=file.read_bytes()
    hashes[key]=hashlib.sha256(content).hexdigest()
    reads.setdefault(key, set()).add(role)
    return content.decode('utf-8')
def balanced(text, start, opening='(', closing=')'):
    depth, quote, escape = 0, False, False
    for i in range(start, len(text)):
        c=text[i]
        if quote:
            if escape: escape=False
            elif c=='\\': escape=True
            elif c=='"': quote=False
            continue
        if c=='"': quote=True
        elif c==opening: depth+=1
        elif c==closing:
            depth-=1
            if depth==0: return text[start+1:i], i+1
    raise ValueError(text[start:start+80])
def split(text):
    result, start, depth, quote, escape = [], 0, 0, False, False
    for i,c in enumerate(text):
        if quote:
            if escape: escape=False
            elif c=='\\': escape=True
            elif c=='"': quote=False
        elif c=='"': quote=True
        elif c in '(<': depth+=1
        elif c in ')>': depth-=1
        elif c==',' and depth==0: result.append(text[start:i]); start=i+1
    if text[start:].strip(): result.append(text[start:])
    return result
def strip_annotations(s):
    while '@' in s:
        m=re.search(r'@[\w.]+',s)
        end=m.end()
        if s[end:end+1]=='(': _,end=balanced(s,end)
        s=s[:m.start()]+s[end:]
    return s.strip()
def constraints(raw, typ, name, source):
    generic=re.search(r'\b(?:List|Map)\s*<',raw)
    outer=raw[:generic.start()] if generic else raw
    f={'name':name,'type':typ,'required':bool(re.search(r'@(?:NotNull|NotBlank|NotEmpty)\b',outer)) or typ in ['int','long','boolean'],'source':source}
    if 'JsonInclude' in outer and 'NON_NULL' in outer: f['omitNull']=True
    if '@NotBlank' in outer: f['notBlank']=True
    for ann in ['Size','Digits','Min','Max','DecimalMin','DecimalMax','Pattern']:
        match=re.search('@'+ann+r'\(([^)]*)\)',outer)
        if match: f[ann]=match[1]
    if '@Positive' in outer: f['positive']=True
    if '@PositiveOrZero' in outer: f['positive']=False;f['Min']='0'
    if typ.startswith('List<'):
        child,_=balanced(raw,generic.end()-1,'<','>')
        f['element']=constraints(child,typ[5:-1],'element',source)
    return f
schemas, enums = {}, {}
sources=list((java/'dto').glob('*.java')) + list((java/'models').glob('*.java'))
for file in sources:
    text=source_text(file, 'enum-scan')
    for m in re.finditer(r'public enum (\w+)\s*\{',text):
        body,_=balanced(text,m.end()-1,'{','}')
        values=re.findall(r'\b[A-Z][A-Z_0-9]*\b', body.split(';')[0])
        key=file.stem+'.'+m[1] if file.stem!=m[1] else m[1]
        enums[key]=values
        enum_sources[key]=str(file.relative_to(base)).replace('\\','/')
for file in (java/'dto').glob('*.java'):
    text=source_text(file, 'dto-records')
    for m in re.finditer(r'public record (\w+)(?:<[^>]+>)?\s*\(',text):
        raw,_=balanced(text,m.end()-1)
        key=file.stem+'.'+m[1] if file.stem!=m[1] else m[1]
        fields=[]
        for part in split(raw):
            clean=strip_annotations(part)
            typ,name=clean.rsplit(None,1)
            typ=re.sub(r'\s+','',typ).replace('java.util.','')
            def qualify(t):
                if '.' in t: return t
                imp = {m[1].split('.')[-1]:m[1] for m in re.finditer(r'import br.com.rodogarcia.wms.dto.([\w.]+);',text)}
                if t in imp and '.' in imp[t]: return imp[t]
                if t in enums: return t
                if file.stem+'.'+t in enums: return file.stem+'.'+t
                if re.search(r'public record '+re.escape(t)+r'\b',text): return file.stem+'.'+t if file.stem!=t else t
                return t
            typ=re.sub(r'\b[A-Z][\w.]*\b',lambda mm:qualify(mm[0]),typ)
            f=constraints(part,typ,name,str(file.relative_to(base)).replace('\\','/'))
            fields.append(f)
        schemas[key]=fields
def imports(text):
    return {m[1].split('.')[-1]:m[1] for m in re.finditer(r'import br.com.rodogarcia.wms.dto.([\w.]+);',text)}
def resolve(t, imp):
    return re.sub(r'\b[A-Z][\w.]*\b',lambda m:imp.get(m[0],m[0]),t.replace(' ','').replace('java.util.',''))
endpoints=[]
for file in (java/'controllers').glob('*.java'):
    text=source_text(file, 'controller-routes'); imp=imports(text)
    prefix=re.search(r'@RequestMapping\("([^"]+)"\)',text)
    prefix=prefix[1] if prefix else ''
    for m in re.finditer(r'@(Get|Post|Put|Delete)Mapping\b',text):
        pos=m.end(); args=''
        if text[pos:pos+1]=='(': args,pos=balanced(text,pos)
        pathMatch=re.search(r'"([^"]+)"',args)
        path=prefix+(pathMatch[1] if pathMatch else '')
        head=re.search(r'public\s+([\w.<>,\[\]]+)\s+(\w+)\s*\(',text[pos:])
        assert head, file
        params,pend=balanced(text,pos+head.end()-1)
        bodyStart=text.index('{',pend); body,bend=balanced(text,bodyStart,'{','}')
        query=[]; paths=[]; request=None; multipart=False
        for part in split(params):
            clean=strip_annotations(part); typ,name=clean.rsplit(None,1)
            if '@RequestBody' in part: request=resolve(typ,imp)
            if '@RequestPart' in part:
                multipart=True
                if typ!='MultipartFile': request=resolve(typ,imp)
            if '@PathVariable' in part or '@RequestParam' in part:
                default=re.search(r'defaultValue\s*=\s*"([^"]+)"',part)
                f={'name':name,'type':resolve(typ,imp),'required':not bool(re.search(r'required\s*=\s*false|defaultValue',part))}
                if default: f['default']=default[1]
                if '@PathVariable' in part: paths.append(f)
                else: query.append(f)
        response=resolve(head[1],imp)
        if response.startswith('ResponseEntity<'): response=response[15:-1]
        # The controller returns the stored JSON bytes assembled as Demonstrativo
        # by FechamentoCobrancaService; retain raw bytes alongside this schema.
        if response=='byte[]': response='FechamentoCobrancaDto.Demonstrativo' if file.stem=='FechamentoCobrancaController' and head[2]=='demonstrativo' else 'unknown'
        endpoint={'id':file.stem+'.'+head[2], 'method':m[1].upper(),'path':path,'request':request,'response':response,'query':query,'params':paths,'multipart':multipart,'source':str(file.relative_to(base)).replace('\\','/'),'handler':head[2]}
        # Permission presentation derived from current service entry point and its local helpers.
        svcvars=dict(re.findall(r'private final (\w+) (\w+);',text))
        var_to_svc={v:k for k,v in svcvars.items()}
        calls=re.findall(r'(\w+)\.(\w+)\(',body)
        permission='OPERACAO'; evidence=[]
        for var,method in calls:
            sf=java/'services'/f'{var_to_svc.get(var, "")}.java'
            if not sf.is_file(): continue
            st=source_text(sf, 'permission:'+file.stem+'.'+head[2])
            sm=re.search(r'public [\w.<>,\[\] ?]+ '+re.escape(method)+r'\s*\(',st)
            if not sm: continue
            _,se=balanced(st,sm.end()-1); sb=st.index('{',se); sbody,_=balanced(st,sb,'{','}')
            # Direct role gates plus local helper called by this method.
            chunks=[sbody]
            for helper in re.findall(r'(?<![.\w])([a-z]\w*)\(',sbody):
                hm=re.search(r'private [\w.<>,\[\] ?]+ '+re.escape(helper)+r'\s*\(',st)
                if hm:
                    _,he=balanced(st,hm.end()-1); hb=st.index('{',he); hbody,_=balanced(st,hb,'{','}');chunks.append(hbody)
            merged='\n'.join(chunks)
            if 'exigirGestor()' in merged: permission='GESTOR'
            elif 'exigirSupervisor()' in merged and permission!='GESTOR': permission='SUPERVISOR'
            evidence.append(str(sf.relative_to(base)).replace('\\','/')+'#'+method)
        endpoint['permission']=permission;endpoint['permissionSource']=evidence
        endpoints.append(endpoint)
# Explicit review of conditional gates/overloads: the role below is the base
# presentation role, not authorization. Conditional historical resolutions remain Gestor.
for e in endpoints:
    derived=e['permission']
    c,h=e['id'].split('.')
    if c in ['CalculoCobrancaController','FechamentoCobrancaController','FatoServicoController','AjusteFechamentoController']:
        e['permission']='SUPERVISOR' if e['method']=='GET' or h in ['calcular','registrar','sugestoes'] else 'GESTOR'
    if c=='ConfiguracaoCobrancaController' and e['method']=='GET': e['permission']='SUPERVISOR'
    if c=='CargaInicialController': e['permission']='GESTOR' if h in ['cancelar','resolverCancelamento'] else 'SUPERVISOR'
    if c=='ContingenciaController': e['permission']='SUPERVISOR'
    if c=='ContagemController': e['permission']='SUPERVISOR' if h=='aplicar' else 'OPERACAO'
    if c=='AvariaController': e['permission']='GESTOR' if h=='reconhecer' else ('OPERACAO' if e['method']=='GET' else 'SUPERVISOR')
    if c=='PedidoSaidaController' and h=='reservar': e['permission']='OPERACAO'
    if c=='ExpedicaoController': e['permission']='OPERACAO' if e['method']=='GET' or h in ['ler','separar'] else 'SUPERVISOR'
    if e['permission']!=derived:
        e['permissionOverride']={'derived':derived,'reviewed':e['permission'],'source':'frontend/tools/contratos.py#base-role-overrides'}
def ts(t):
    if t.startswith('List<'): return f'Array<{ts(t[5:-1])}>'
    if t.startswith('PaginaResponse<'): return f'PaginaResponse<{ts(t[15:-1])}>'
    if t.startswith('Map<'): return 'Record<string, unknown>'
    if t in ['Long','long','BigDecimal']: return 'string'
    if t in ['int','Integer']: return 'number'
    if t in ['Boolean','boolean']: return 'boolean'
    if t in ['String','UUID','Instant','LocalDate','LocalDateTime','OffsetDateTime','ZoneId']: return 'string'
    if t in ['Object','unknown']: return 'unknown'
    if t=='T': return 'T'
    return t.replace('.','_')
types=['// Gerado de fontes Java atuais. Long/BigDecimal são lexemas internos; no fio são números JSON.']
for k,values in enums.items(): types.append('export type '+k.replace('.','_')+' = '+' | '.join(json.dumps(x) for x in values)+';')
for k,fields in schemas.items():
    types.append('export interface '+k.replace('.','_')+('<T>' if k=='PaginaResponse' else '')+' {')
    for f in fields:
        nullable=not f['required'] and f['type'] not in ['boolean','long','int']
        types.append('  '+f['name']+('?' if f.get('omitNull') else '')+': '+ts(f['type'])+(' | null' if nullable else '')+';')
    types.append('}')
types.append('export interface ApiContracts {')
for e in endpoints:
    types.append('  '+json.dumps(e['id'])+': { request: '+(ts(e['request']) if e['request'] else 'undefined')+'; response: '+ts(e['response'])+' };')
types.append('}')
(out/'types.ts').write_text('\n'.join(types)+'\n',encoding='utf-8')
(out/'schemas.json').write_text(json.dumps({'records':schemas,'enums':enums},ensure_ascii=False,indent=2),encoding='utf-8')
(out/'endpoints.json').write_text(json.dumps(endpoints,ensure_ascii=False,indent=2),encoding='utf-8')
ev=base/'frontend/evidencias';ev.mkdir(exist_ok=True)
generator=Path(__file__).resolve()
source_text(generator,'generator-and-embedded-overrides')
generated={str(f.relative_to(base)).replace('\\','/'):hashlib.sha256(f.read_bytes()).hexdigest() for f in [out/'types.ts',out/'schemas.json',out/'endpoints.json']}
(ev/'fontes-contratos.json').write_text(json.dumps({'fontes':dict(sorted(hashes.items())),'leituras':{k:sorted(v) for k,v in sorted(reads.items())},'enumSources':enum_sources,'gerados':generated,'overrides':[{'endpoint':e['id'],**e['permissionOverride']} for e in endpoints if 'permissionOverride' in e],'rotas':len(endpoints),'records':len(schemas),'enums':len(enums),'modo':'leitura estática; não executa backend'},ensure_ascii=False,indent=2),encoding='utf-8')
print(len(schemas),'records;',len(endpoints),'endpoints')
print('\n'.join(f'{e["id"]}: {e["method"]} {e["path"]} | {e["request"]} -> {e["response"]} | {e["permission"]}' for e in endpoints))
