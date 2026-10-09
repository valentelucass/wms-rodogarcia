from pathlib import Path
from datetime import datetime, timezone
from functools import lru_cache
import hashlib,json,zipfile,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[2];E=R/'backend/evidencias';T=R/'orchestracao/.runtime'
@lru_cache(maxsize=None)
def meta(p):return dict(arquivo=p.relative_to(R).as_posix(),sha256=hashlib.sha256(p.read_bytes()).hexdigest().upper(),bytes=p.stat().st_size)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def write(p,x):
 with p.open('x',encoding='utf-8',newline='\n') as f:json.dump(x,f,ensure_ascii=True,indent=2);f.write('\n')
recP=E/'d30-cedro-final14-execucao.json';rec=read(recP)
assert rec['exitMaven']==0 and rec['totais']==dict(tests=708,failures=0,errors=0,skipped=0) and len(rec['JAR'])==1 and rec['fontesEstaveis'] and rec['historicosEstaveis']
semP=E/'d30-cedro-disposicoes-causais-v93e-final14-indice.json';sem=read(semP)
assert len(sem['classes'])==57 and sem['variaveisCorrentes']==3891 and sem['grupos']==323
assert not sem['faltantesVariaveis'] and not sem['faltantesGrupos']
for k in ['classes','batchesVariaveis','batches323']:
 for m in sem[k]:assert meta(R/m['arquivo'])==m
gates=[E/n for n in ['d30-cedro-get65-final14-gate-v110.json','d30-cedro-provider26-final14-gate-v111.json','d30-cedro-b664-final14-gate-v112.json','d30-cedro-final14-contextos-gate-v113.json','d30-cedro-final14-inventario-atual-v113.json','d30-cedro-tratativa-replay-equivalente-final14-v114.json','d30-cedro-final14-a08-conservacao-exata-v115.json']]
for p in gates:assert p.is_file()
history=E/'d20-hibernate-update-sql.json';assert meta(history)['sha256']=='38AD8D204C8D947E2DC76DCE2D26473D05EAF76A423A2EE9FFF9EF1E94038801'
old=read(E/'d30-cedro-final10-execucao.json');oldMeta={x['arquivo']:x for x in old['fontesDepois']}
changes=[x for x in rec['fontesDepois'] if x!=oldMeta.get(x['arquivo'])]
assert [x['arquivo'] for x in changes]==['backend/src/test/java/br/com/rodogarcia/wms/ContingenciaIntegrationTest.java']
sources=rec['fontesDepois'];assert all(meta(R/x['arquivo'])==x for x in sources)
z=zipfile.ZipFile(E/'d30-cedro-final14-fontes.zip')
target=R/changes[0]['arquivo'];assert z.read(changes[0]['arquivo'])==target.read_bytes()
xml=E/'d30-cedro-final14/surefire-reports/TEST-br.com.rodogarcia.wms.ContingenciaIntegrationTest.xml'
root=ET.parse(xml).getroot();case=next(x for x in root.findall('testcase') if x.get('name')=='d30RevisaoPublicaCargaPreservaOrigemHistoricoReplayERollback')
assert not any(case.find(tag) is not None for tag in ['failure','error','skipped'])
photos=list((E/'d30-cedro-final14/d30-cedro-saidas').glob('d30-cedro-revisao-carga-*.json'));assert len(photos)==1
pic=read(photos[0]);assert pic['replay64Igual'] and pic['rollback64Igual'] and len(pic['antes64'])==len(pic['depois64'])==64
beforeIdx=read(E/'d30-cedro-disposicoes-causais-v82c-final10-indice.json')
pendingIDs={x['id'] for x in beforeIdx['faltantesVariaveis']};rows=[]
for b in sem['batchesVariaveis']:
 for i,x in enumerate(read(R/b['arquivo'])['linhas']):
  if x['id'] in pendingIDs:rows.append(dict(id=x['id'],artefato=b,pointer='/linhas/'+str(i),disposicao=x['conclusao'],fonte=x['fonteAtual'],suporteTecnico=x['classificacaoTecnica'],limite=x['limite']))
assert len(rows)==55 and {x['id'] for x in rows}==pendingIDs
deltaP=E/'d30-cedro-fecho55-final14-v117.json';write(deltaP,dict(demanda='D30',fonteAnterior=meta(E/'d30-cedro-disposicoes-causais-v82c-final10-indice.json'),indiceCorrente=meta(semP),itens=rows,aceitePorQuedaDeContagem=False))
execs=[]
for p in sorted(E.glob('d30-cedro-*-execucao.json')):
 x=read(p);execs.append(dict(artefato=meta(p),exitMaven=x.get('exitMaven'),exit=x.get('exit'),totaisDestaExecucao=x.get('totais'),JARdestaExecucao=len(x['JAR']) if isinstance(x.get('JAR'),list) else None,fonteMomento=x.get('sourceMomento'),limite='Registro individual; nao somar execucoes/focais/historicos. Formato/javac nao sao JUnit.'))
execP=E/'d30-cedro-inventario-execucoes-v117.json';write(execP,dict(demanda='D30',itens=execs,somaDeTestes=False,atual=meta(recP),ASTatual=meta(E/'d30-cedro-graph-ast-final-v11.json')))
limits=[
 dict(id='D30-NATIVO-SQLSERVER',motivo='SQLServer/HTTPDEV/sondas e execucao SQLIT expressamente suspensos; nenhum acesso novo.',encaminhamento='Farol registra facetas nativas porID; somente nova autorizacao propria e guarda real poderiam permitir execucao. TestesH2 nao aceitam dialeto/TLS/permissoes/relogio/locks/atomicidade SQLServer.'),
 dict(id='D30-NATIVO-F01-A10',motivo='Expressividade UNIQUE filtrado V6/V8 versus metadadoJPA/H2; fonte estatica e oraculos locais nao aceitam filtro/DDL/catalogoSQLServer real.',encaminhamento='Preservar indices/checks/migrations, disposicoes Prumo e limites individuais; nenhuma migration ou main alterada por candidata.'),
 dict(id='D30-NATIVO-PROVIDER26',motivo='Identidade/versao/queries demonstradas somente providerH2 e fixtures/capturas escolhidos porID. Nao geracao/driver/materializacao/locking SQLServer.',encaminhamento='RevisaoVigia por26IDs e facetas SQL especificas; nao transferir regra local/filtro/arithmetic a banco real.'),
 dict(id='D30-EXTERNO-IDENTIDADE',motivo='Bean Nimbus produtivo/condicionais/claims/assinatura exercitados com fixtures e transporte mock. Provedor/credenciais/JWKS reais nao acessados.',encaminhamento='Provedor real fica externo especifico, sem aceite por simulacao.'),
 dict(id='D30-EXTERNO-FISCAL-PESO-COMERCIAL',motivo='NOTAZZ/ESL reais, peso medido pelo cliente, parametros/tarifas comerciais e dispositivos nao aceitos por fixtures. qCom nao peso; convencao rateio0/0/1 nao aceite comercial.',encaminhamento='Manter fontes normativas/procedimento manual/casos locais e responsabilidades externas individuais sem inventar tags/fatores/tetos.'),
 dict(id='D30-FE',motivo='Frontend fora desta demanda.',encaminhamento='Aguardar demanda propria; nenhuma escrita/execucao frontend.'),
 dict(id='D30-REVISAO-GERAL-C09',motivo='Produto autoral delimitado entregue; Vigia/Farol verificam sufficiencia geral e mapa MultiGraph da ultima fonte. Nao impedimento de aplicacao LOCAL/SQL.',encaminhamento='Revisar por identidade/assertiva/oraculo/versao; materialidade real volta ao mesmoID. C01-C10/pais/D30 nao encerrados por autor ou contagem.')]
md=E/'d30-cedro-fecho-autoral-final14-v117.md'
text='''# D30 — produto autoral Cedro na fonte FINAL14

FINAL14: **708 testes, 0 falhas, 0 erros, 0 ignorados; exit0 e 1 JAR**. FonteZIP, classes, JAR, log e 50 XML pertencem à mesma execução. Classificação22: 66 fontes de teste compiladas, 52 classes locais selecionadas, 14 contextos Boot guardados (12 H2 efêmeros e 2 sem datasource); SQLIT, sete métodos, somente compilado. Não há soma com focais ou execuções anteriores.

O índice semântico93e contém disposições individuais de 57 classes, 3.891 variáveis atuais e 323 grupos por caller, em 31 lotes de variáveis e seis de grupos. Cada linha conserva definição, tipo/null/default, usos, transformação, invariantes/oráculo, assertivas, fonte/SHA/XML/snapshot e limite. O delta55 preserva todos os IDs que estavam sem ligação; construtores/DI, campos de records e serialUID têm suporte e limite próprios, sem teste autônomo por alias. Zero ligação pendente neste produto não equivale a aceite de todos os predicados, pais ou C01–C10.

O único complemento de código após FINAL10 foi o teste da revisão pública de carga: nota/contexto/datas/quantidade20 preservados, revisão2/versão+1, hash do envelope fixado antes do retorno, auditoria/operação com ID/alvo/conteúdo, fotos64, recusa de versão/perfil, replay original, UUID divergente e rollback tardio. Reds11/12/13 e snapshots permanecem. Os oráculos foram corrigidos por confronto de escala19,6, literal wire versus nós em memória e código **OPERACAO_DIVERGENTE** do caller administrativo; nenhuma guarda foi removida nem main alterado nesse complemento.

GET65: testemunha por rota/caso/contexto/handler real, fonte e XML desta execução, fotos64 e igualdade SHA/bytes. A08 conserva UTC/nanos/micros/null/tipo/coluna com fixture independente; fotos03/04 históricas não foram reconstruídas. Provider26 e B664 têm disposição por ID e resultados exatos das fixtures locais; não são prova SQLServer nem de todos os filtros/roles/ramificações.

AST11 foi extraído das 325 fontes Java atuais e entregue isoladamente a Farol com posse liberada. Preservação do mapa produtivo e backup, caminhos/SHA/snapshot conferidos; integração MultiGraph e revisão C09 pertencem a Farol/Vigia. Quantidades de nós/relações não são aceite. Históricos D20/D29, migrations e preexistentes preservados.

**SQLServer/HTTPDEV/sondas/credenciais reais/PROD/servidor/runtime/acessos/DDL/migrations/publicação/commit/push/ETL/frontend: zero ações.** HTTP e JDBC executados apenas nos contextos locais fictícios autorizados. As parcelas nativas, provedor/fiscal/dispositivos/comercial e FE têm limites e encaminhamentos individuais no JSON. Nenhum impedimento novo de aplicação local foi identificado; revisão geral não é bloqueio SQL.

O leitor de artefatos v82 antigo segue seu encerramento natural, sem interrupção; o produto93e o substitui para disposição atual. Não há trabalho de código/teste/AST em execução pelo Cedro após FINAL14. A revisão independente do produto geral permanece necessária.
'''
with md.open('x',encoding='utf-8',newline='\n') as f:f.write(text)
oracles=[E/n for n in ['d30-cedro-revisao-carga-oraculos-v92-antes.json','d30-cedro-revisao-carga-red11-oraculo-v94-antes.json','d30-cedro-revisao-carga-red12-oraculo-wire-v102-antes.json','d30-cedro-revisao-carga-red13-oraculo-caller-v109-antes.json']]
result=dict(demanda='D30',dono='Cedro',utc=datetime.now(timezone.utc).isoformat(),tipo='PRODUTO_AUTORAL_ATUAL_POR_ID_CALLER_NAO_ACEITE_CENTRAL',FINAL=meta(recP),fonteSnapshot=meta(E/'d30-cedro-final14-fontes.zip'),classificacao=meta(E/'d30-cedro-auditoria-suites-v22.json'),inventarioJavacAtual=meta(E/'d30-cedro-inventario-javac-v22.json'),
 semantica=meta(semP),delta55=meta(deltaP),execucoesIndividuais=meta(execP),gates=[meta(p) for p in gates],ASTentregue=meta(E/'d30-cedro-graph-ast-final-v11.json'),MD=meta(md),
 historicosD20D29MigrationsPreservados=rec['historicosEstaveis'],D20=meta(history),main259DesdeFINAL10ByteEstavel=True,unicaFonteMudadaDesdeFINAL10=changes[0],
 revisaoCarga=dict(caso=case.attrib,XML=meta(xml),captura=meta(photos[0]),planosEOraculosAntes=[meta(p) for p in oracles],limite='Nao mainbug inferido; sem tolerancia/rounding no19,6/MAX, diferente codigo de reparar. Todos predecessores imutaveis.'),
 tarefasConcluidas=['Confronto finito55 com mesmosIDs/provas ou classificacao tecnica pertinente','Complemento publico revisarCarga depois de confirmar falta de equivalencia','Red-preservado/oraculos antes/correcoes test-only','Compilacao de todos sources/perfis classificados e guarda fail-closed','Regressao local completa autorizada na ultima fonte14 e pacote mesmaexecucao','Gates GET65/provider26/B664/contextos/A08/replay por identidade','Inventario simbolos atual e AST nativo atual entregues','Publicacao propria porID/caller/limite, UTF8 e hashes'],
 pendenciasELimitesIndividuais=limits,faltanteCodigoLocalConfirmadoNesteRecorte=None,proximaAcao='Revisao independente geral Vigia/Farol; tratar somente achado material confirmado no mesmoID, sem nova execucao por relatorio.',
 processos=dict(codigoOuJUnitOuASTCedroEmExecucao=False,leitorV82Legado='Em encerramento natural, sem kill/interrupcao; produto atual93e independente dele.'),
 SQLServerAcoes=0,HTTPDEV=0,novasSondas=0,credenciaisReais=0,PROD=0,frontend=False,DDLServidor=False,migrationsExecutadas=False,processosInterrompidos=0,aceitePorContagem=False,aceiteC01_C10=False,D30Fechada=False)
out=T/'d30-cedro-fecho-autoral-final14-v117.json';write(out,result)
print(json.dumps(dict(JSON=meta(out),MD=meta(md),delta55=meta(deltaP))))
