from pathlib import Path
import json,hashlib,datetime,re,copy
root=Path(__file__).resolve().parents[3];ev=root/'backend/evidencias'
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest().upper()
lumePath=root/'orchestracao/.runtime/d29-lume-matriz-clausulas.json';lume=read(lumePath)
plan=read(ev/'d29-plano-matriz-cedro.json');progress=read(ev/'d29-cedro-progresso.json');local=read(root/'orchestracao/.runtime/d29-lume-revisao-testes-locais.json');financial=read(ev/'d29-financeiro39-cobertura-cedro.json')
rows=copy.deepcopy(lume['linhas'])
for row in rows:
 base=re.split(r'[.\-]',row['id'])[0]
 groups=[{'id':c['id'],'etapa':c['etapa'],'esperado':c['esperadoIndependente']} for c in plan['casos'] if base in c.get('requisitos',[]) or row['id'] in c.get('requisitos',[])]
 exact=[c for c in local['casos'] if row['id'] in c.get('fontesContrato',[])]
 related=[c['id'] for c in local['casos'] if base in c.get('fontesContrato',[]) and c not in exact]
 row['controleCedro']={'grupos46Relacionados':groups,'testesLocaisRevistosClausulaExata':exact,'testesRelacionadosAoPaiNaoEquivalentes':related,'statusOriginalLume':row.get('status'),'estado':'CONFRONTO_INDIVIDUAL_EM_CURSO','criterio':'Nenhum pai/ID/rota/HTTP200 vira aceite. Vinculo amplo e indice de teste nao fecham atomos; preservar expected e precondicao e revisar efeitos/limites.','rodadasRelacionadas':[r['rodada'] for r in progress['rodadas'] if any(g['etapa']==r['etapa'] for g in groups)],'fonteRegistroLumeSHA256':sha(lumePath)}
doc={'demanda':'D29','observadoUtc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'dono':'Cedro','estado':'MATRIZ_PROPRIA_COMPLETA_DO_CORPUS_CURADO_CONFRONTO_EM_CURSO','fontes':{'corpusLume':{'arquivo':str(lumePath.relative_to(root)),'SHA256':sha(lumePath)},'plano46':sha(ev/'d29-plano-matriz-cedro.json'),'prioridades33':sha(root/'orchestracao/.runtime/d29-vigia-prioridades.json'),'revisaoLocalLume':sha(root/'orchestracao/.runtime/d29-lume-revisao-testes-locais.json'),'financeiros39':sha(ev/'d29-financeiro39-cobertura-cedro.json')},'contagensCorpus':lume['contagens'],'linhas':rows,'prioridades33':read(ev/'d29-prioridades-vinculos-cedro.json')['prioridades'],'financeiros39':financial['casos'],'rodadas':progress['rodadas'],'coberturaTotalDeclarada':False,'limitesIrreversiveis':['Versoes iniciais: hashes/artefato preservados antes do HTTP, mas algumas fontes/classes nao foram integralmente copiadas no arquivo imutavel antes de executar. Nao reconstruir nem backdate; manifestos completos das versoes posteriores sao distintos.','Datas antigas de quatro notas da primeira familia desqualificam prova temporal associada. Dados e red preservados.'],'impedimentosMateriais':['Aprovacao nativa/reabertura de aprovado/entrega/deltas em53/25: corte08/10/2026 03:00Z, recusas reais409; nao alterar Clock/fuso/fatos.','Observacao simultanea de duas sessoes/locks nativos nao autorizada/disponivel na identidade WMSDEV; prova alternativa HTTP/JVM/SELECT delimitada.','Fiscal externo/comercial/dispositivo/piloto fisico/backup-restauracao/frontend fora do escopo D29.'],'trabalhoTecnicoAindaNaoEhBloqueio':['Confrontar cada atomo com precondicoes/oraculos/provas efetivamente equivalentes','Completar fases tecnicas faltantes sem duplicar negocio concluido','SELECTs vigentes e confronto final1895linhas/direitos/historicos','Fecho OS de todos os proprios apos sinais e revisao independente']}
(ev/'d29-matriz-confronto-cedro.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
md=['# D29 — matriz própria Cedro em confronto','',f'{len(rows)} registros curados preservados com fontes, esperado independente, aliases, casos propostos, resultados/limites e controles de Cedro. Os3155/3593 candidatos literais iniciais não são cobertura. Nenhum percentual de aceitação foi calculado.','', 'A matriz JSON conserva o corpus completo do Lume,46 grupos próprios,33 controles Vigia,39 exemplos financeiros e rodadas novas. Vínculos amplos/candidatos não recebem aceite automático; cada prova tem escopo local/HTTP/SQL e requisito atômico.','', 'Impedimentos materiais: corte financeiro nativo08/10às03:00Z; observabilidade SQL nativa indisponível na identidade restrita; ações externas fora do escopo. Curadoria, casos ainda preparáveis e SELECT pendente permanecem trabalho, não bloqueio.','', 'Lacunas históricas de captura e emissões antigas são preservadas; arquivos atuais não retroatestam versões anteriores. Manifestos completos novos distinguem cada execução.']
(ev/'d29-matriz-confronto-cedro.md').write_text('\n'.join(md)+'\n',encoding='utf-8')
extra=read(ev/'d29-capturas-confronto-cedro.json')
for row in doc['linhas']:
 control=row['controleCedro'];exact=[c for c in extra['cases'] if row['id'] in c['atomos'] and c['aprovado']]
 control['estado']='SEM_EQUIVALENCIA_INTEGRAL_REVISADA'
 if row.get('status')=='DEPENDE_DE_VALIDACAO_EXTERNA':control['estado']='EXTERNO_FORA_ESCOPO_AUTORIZADO'
 elif row.get('status')=='ATENDIDO':control['estado']='PROVA_DELIMITADA_REVISTA_POR_LUME_PRESERVADA'
 if exact:
  control['provasCamposIndependentes']=exact;control['estado']='PROVA_ANALITICA_CAMPOS_D29_ASSOCIADA_HTTP_SQL_REVISAR';control['limite']='GETs reais19:01 e SELECT associado; confrontoanalitico novo20:09 sem nova chamada. Nao fecha pai, imutabilidade geral ou outros casos negativos.'
 if control['estado']=='SEM_EQUIVALENCIA_INTEGRAL_REVISADA':control['limiteAcessoAtual']='Casos/acoes individuais permanecem no planoCaso/restante original; complemento nativo impedido pelo prelogin atual semDB_NAME. Prova local existente ligada somente onde equivalente. Curadoria/prova parcial nao vira aceite integral.'
doc['estado']='CONFRONTO_PUBLICADO_PARCIAL_INTEGRACAO_IMPEDIDA_PRELOGIN'
doc['impedimentoTransporte']={'arquivo':'backend/evidencias/d29-impedimento-transporte-atual.md','prova':'D2976713EE4','HTTP':0,'DB_NAMEconfirmadoNestaTentativa':False}
doc['capturasConfrontadas']={'arquivo':'backend/evidencias/d29-capturas-confronto-cedro.json','SHA256':sha(ev/'d29-capturas-confronto-cedro.json'),'checks':extra['checks'],'falhas':extra['falhas'],'novoHTTP':False}
doc['buildAtual']={}
for kind in ['fronteiras','limites']:
 p=ev/('d29-build-'+kind+'-recibo.json')
 if p.exists():
  b=read(p);doc['buildAtual']={'arquivo':str(p.relative_to(root)),'SHA256':sha(p),'total':b['total'],'JAR':b['jar']['SHA256'],'classesAntesDepoisIguais':b['todasClassesIguais'],'SQLServerIT':False}
doc['trabalhoTecnicoAindaNaoEhBloqueio']=['Revisao/consolidacao Farol e Vigia dos vinculos e reds novos; nao autoriza outra jornada ou aceite por contagem']
doc['preservacaoFinal']={'estado':'PENDENTE_IMPEDIDA_ACESSO_SQL_-2','fonte':'orchestracao/.runtime/d29-prumo-preservacao-confronto-checkpoint.json','SHA256':sha(root/'orchestracao/.runtime/d29-prumo-preservacao-confronto-checkpoint.json'),'limite':'Checkpoint19:22 teve0queries, nao prova confronto final1895; cinco separacoes complementadas19:16 sembaseline inicial, nao retroativo.'}
(ev/'d29-matriz-confronto-cedro.json').write_text(json.dumps(doc,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
with (ev/'d29-matriz-confronto-cedro.md').open('a',encoding='utf-8') as f:f.write('\nConfronto atualizado: sete assertivas analiticas7/0 ligam somente seis atomosRN02 aos GETs reais/SELECT da964. Build atual e status de transporte/preservacao final estão no JSON; não declarar cobertura integral. Cada restante conserva seu caso/precondição/oráculo específico.\n')
print('D29 matriz propria',len(rows),'registros, sem porcentagem/aceiteautomatico')
