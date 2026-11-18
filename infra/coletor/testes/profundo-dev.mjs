import {readFile,writeFile} from 'node:fs/promises';
import {randomUUID,createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import path from 'node:path';
const area=process.argv[2],stage=process.argv[3];let input='';for await(const c of process.stdin)input+=c;const secret=JSON.parse(input);input='';
assert.ok(['localhost','127.0.0.1'].includes(new URL(secret.base).hostname)&&new URL(secret.base).port==='25581');
const file=path.join(area,'profundo.json');let report;
try{report=JSON.parse(await readFile(file,'utf8'));}catch(e){if(e.code!=='ENOENT')throw e;report={id:'COLETOR-PROFUNDO01',prefix:'CP29-'+randomUUID().slice(0,8),checks:[],commands:{},fixtures:{},http:[],attempts:[],state:'PREPARADO'};}
report.attempts.push({stage,at:new Date().toISOString(),sourceHash:secret.sourceHash,jarSha256:secret.jarSha256,runId:secret.runId,previousState:report.state,previousFailure:report.failure});
delete report.failure;delete report.lastFailure;
const save=()=>writeFile(file,JSON.stringify(report,null,2));
const check=(ok,name)=>{assert.ok(ok,name);if(!report.checks.includes(name))report.checks.push(name);};
class Actor{
 constructor(credential){this.credential=credential;this.cookies=new Map();this.token='';this.csrf='';this.header='';}
 async request(method,url,data,expected=200,authorized=true){
  const response=await fetch(secret.base+url,{method,headers:{Origin:secret.base,...(this.cookies.size?{Cookie:[...this.cookies].map(([k,v])=>k+'='+v).join('; ')}:{}),...(authorized&&this.token?{Authorization:'Bearer '+this.token}:{}),...(this.csrf&&method!=='GET'?{[this.header]:this.csrf}:{}),...(data?{'Content-Type':'application/json'}:{})},...(data?{body:JSON.stringify(data)}:{}),signal:AbortSignal.timeout(45000)});
  for(const c of response.headers.getSetCookie()){const first=c.split(';',1)[0],i=first.indexOf('=');if(i>0)this.cookies.set(first.slice(0,i),first.slice(i+1));}
  let body;try{body=await response.json();}catch{body=null;}
  if(!url.startsWith('/api/auth/')){report.http.push({actor:this.profile?.perfil??'SEM_LOGIN',method,url,status:response.status});await save();}
  if(!(Array.isArray(expected)?expected.includes(response.status):response.status===expected)){report.lastFailure={method,url,status:response.status,code:body?.codigo,message:body?.mensagem};await save();throw new Error('HTTP_'+response.status+'_'+url);}
  return {body,status:response.status};
 }
 async login(){
  const csrf=(await this.request('GET','/api/auth/csrf',undefined,200,false)).body;this.csrf=csrf.token;this.header=csrf.header;
  const password=report.authInitialized?.[this.credential.id]?this.credential.novaSenha:this.credential.senha;
  const auth=(await this.request('POST','/api/auth/entrar',{email:this.credential.email,senha:password},200,false)).body;
  assert.equal(auth.usuario.id,this.credential.id);this.token=auth.accessToken;this.profile=auth.usuario;
  if(this.profile.trocarSenha&&stage!=='preflight'){
    assert.ok(this.credential.novaSenha);report.authInitializationPending??={};report.authInitializationPending[this.profile.id]=true;await save();
    await this.request('POST','/api/auth/senha',{senhaAtual:password,novaSenha:this.credential.novaSenha},204);
    report.authInitialized??={};report.authInitialized[this.profile.id]=true;delete report.authInitializationPending[this.profile.id];await save();
    await this.login();assert.equal(this.profile.trocarSenha,false);
  }
 }
 async get(url){return(await this.request('GET',url)).body;}
 async post(name,url,body,status=200){
  const prior=report.commands[name];if(prior?.done)return prior.response;if(prior?.uncertain)throw new Error('COMANDO_INCERTO_'+name);
  const data=prior?.body??body;report.commands[name]={url,body:data,uncertain:true};await save();
  let result;try{result=(await this.request('POST',url,data,status)).body;}catch(e){if(report.lastFailure?.url===url)report.commands[name].uncertain=false;await save();throw e;}
  report.commands[name]={url,body:data,response:result,done:true,uncertain:false};await save();return result;
 }
}
const actors=secret.actors.map(c=>new Actor(c));let manager,supervisor,operator;
const reason='Validação profunda fictícia '+report.prefix;
const taskUrl=id=>'/api/v1/tarefas-coletor/'+id;
const op=()=>randomUUID();
const measures={pesoKg:1,alturaMetros:1,larguraMetros:1,profundidadeMetros:1,empilhamento:1,posicoesNecessarias:1};
const dest=a=>({enderecoId:a.id,codigoLido:a.codigo});
const f=report.fixtures;
async function totals(){return operator.get(`/api/v1/tarefas-coletor/resumo?clienteId=${f.client.id}&armazemId=${f.warehouse.id}`);}
async function ownTasks(type){return(await manager.get(`/api/v1/tarefas-coletor?tipo=${type}&clienteId=${f.client.id}&armazemId=${f.warehouse.id}&tamanho=100`)).itens;}
async function address(name,type,index){return manager.post('address-'+name,'/api/v1/enderecos',{armazemId:f.warehouse.id,codigo:report.prefix+'-'+name,rua:report.prefix,nivel:0,posicao:String(index),descricao:'Teste profundo fictício '+name,tipo:type,capacidadePesoKg:10000,alturaMetros:10,larguraMetros:10,profundidadeMetros:10,empilhamentoMaximo:10,sequenciaColeta:index,tipoUnidadePermitido:'TODAS'},201);}
async function claim(a,name,t){return a.post(name,taskUrl(t.id)+'/iniciar',{operacaoId:op(),versao:t.versao,motivo:reason});}
async function release(a,name,t){return a.post(name,taskUrl(t.id)+'/liberar',{operacaoId:op(),versao:t.versao,motivo:reason});}
function nfeKey(cnpj,num){const b='352610'+cnpj+'55999'+String(num).padStart(9,'0')+'100000001';let sum=0;for(let i=0;i<43;i++)sum+=Number(b[42-i])*(2+i%8);const d=11-sum%11;return b+(d>=10?0:d);}
try{
 for(const a of actors)await a.login();
 manager=actors.find(a=>a.profile.perfil==='GESTOR');supervisor=actors.find(a=>a.profile.perfil==='SUPERVISOR');operator=actors.find(a=>a.profile.perfil==='OPERACAO');
 check(Boolean(manager&&supervisor&&operator),'TRES_PERFIS_AUTENTICADOS_SEM_TROCAR_IDENTIDADES');
 if(stage==='preflight'){
  report.profiles=actors.map(a=>({id:a.profile.id,email:a.profile.email,perfil:a.profile.perfil,administrador:a.profile.administrador,trocarSenha:a.profile.trocarSenha,clientes:a.profile.clientes,armazens:a.profile.armazens}));
  const commonClients=operator.profile.clientes.filter(c=>supervisor.profile.clientes.includes(c));
  const commonWarehouses=operator.profile.armazens.filter(c=>supervisor.profile.armazens.includes(c));
  report.access={commonClients,commonWarehouses};
  report.preflightCadastros={clients:await Promise.all(commonClients.map(id=>manager.get('/api/v1/clientes/'+id))),warehouses:await Promise.all(commonWarehouses.map(id=>manager.get('/api/v1/armazens/'+id)))};
  report.state='PREFLIGHT_APROVADO';await save();
 }else if(stage==='preparar'){
  check(actors.every(a=>!a.profile.trocarSenha),'PRIMEIRO_ACESSO_FICTICIO_CONCLUIDO_SEM_MUDAR_PERFIL_OU_ESCOPO');
  f.client??=await manager.get('/api/v1/clientes/64');f.warehouse??=await manager.get('/api/v1/armazens/31');
  check(f.client.nome.startsWith('DEV fictício')&&f.warehouse.nome.startsWith('DEV fictício'),'CLIENTE_ARMAZEM_FICTICIOS_PREEXISTENTES_CONFERIDOS');
  check(operator.profile.clientes.includes('64')&&operator.profile.armazens.includes('31')&&supervisor.profile.clientes.includes('64')&&supervisor.profile.armazens.includes('31'),'PERMISSOES_ORIGINAIS_PRESERVADAS');
  report.baseline??=await totals();await save();
  f.product=await manager.post('product','/api/v1/produtos',{clienteId:64,sku:report.prefix,descricao:'Produto fictício da validação profunda',unidadeMedida:'UN',tipoQuantidade:'CONTAGEM',precisaoQuantidade:0,controlaLote:false,controlaValidade:false},201);
  f.pack=await manager.post('pack','/api/v1/embalagens',{produtoId:f.product.id,codigoDun:report.prefix+'D',descricao:'Embalagem fictícia',quantidadeProduto:1},201);
  for(const [i,name]of ['ARM1','ARM2','ARM3','PAR1','PAR2'].entries())f[name]=await address(name,'ARMAZENAGEM',i+1);
  f.SEP1=await address('SEP1','SEPARACAO',6);f.SEP2=await address('SEP2','SEPARACAO',7);
  f.pair=await manager.post('pair','/api/v1/conjuntos-posicoes',{armazemId:31,codigo:report.prefix+'-PAR',enderecoAId:f.PAR1.id,enderecoBId:f.PAR2.id,limites:{pesoKg:10000,alturaMetros:10,larguraMetros:10,profundidadeMetros:10,empilhamentoMaximo:10},motivo:reason},201);
  f.entry=await manager.post('entry','/api/v1/pedidos-entrada',{clienteId:64,armazemId:31,referencia:report.prefix},201);
  report.noteCnpj??='000000'+String(Number.parseInt(op().slice(0,8),16)%100000000).padStart(8,'0');report.noteNumber??=100000000+Number.parseInt(op().slice(0,8),16)%700000000;await save();
  let e=await manager.post('note',`/api/v1/pedidos-entrada/${f.entry.id}/notas`,{versao:f.entry.versao,serie:999,numero:report.noteNumber,emissao:'2026-10-29',chaveAcesso:nfeKey(report.noteCnpj,report.noteNumber),itens:[{numeroItem:1,produtoId:f.product.id,quantidadePrevista:20,valorMercadoria:200}]});
  e=await manager.post('conference',`/api/v1/pedidos-entrada/${f.entry.id}/iniciar-conferencia`,{versao:e.versao,motivo:reason});
  const d=await manager.get(`/api/v1/pedidos-entrada/${f.entry.id}`);
  e=await manager.post('arrival',`/api/v1/pedidos-entrada/${f.entry.id}/chegadas`,{operacaoId:op(),versao:e.versao,chegouEm:new Date(Date.now()-5000).toISOString(),observacao:reason,itens:[{itemNotaId:d.notas[0].itens[0].id,quantidadeBoa:20,quantidadeAvariada:0}]});
  e=await manager.post('effective',`/api/v1/pedidos-entrada/${f.entry.id}/efetivacao`,{versao:e.versao,aceitarDivergencias:false,motivo:reason});
  check((await totals()).enderecamento===report.baseline.enderecamento,'ENTRADA_SEM_UNITIZAR_NAO_GERA_TAREFA');
  const entries=await manager.get(`/api/v1/pedidos-entrada/${f.entry.id}/entradas`);
  const unitBody={operacaoId:op(),versaoPedido:e.versao,motivo:reason,unidades:[0,1,2,3].map(()=>({embalagemId:f.pack.id,tipo:'VOLUME',condicao:'BOA',quantidade:5}))};
  f.unitization=await manager.post('unitize',`/api/v1/pedidos-entrada/${f.entry.id}/entradas/${entries.itens[0].id}/unitizacao`,unitBody);
  f.units=(await manager.get(`/api/v1/pedidos-entrada/${f.entry.id}/unidades`)).itens;
  check(f.units.length===4&&(await totals()).enderecamento===report.baseline.enderecamento+4,'QUATRO_UNIDADES_GERAM_QUATRO_TAREFAS');
  await manager.request('POST',report.commands.unitize.url,report.commands.unitize.body);
  await manager.get(`/api/v1/unidades-logisticas/${f.units[0].codigo}/etiqueta`);await manager.get(`/api/v1/unidades-logisticas/${f.units[0].codigo}/etiqueta`);
  check((await totals()).enderecamento===report.baseline.enderecamento+4,'REPLAY_UNITIZACAO_E_REIMPRESSAO_NAO_DUPLICAM_TAREFAS');
  f.addressTasks=(await ownTasks('ENDERECAMENTO')).filter(t=>f.units.some(u=>u.id===t.origemId));check(f.addressTasks.length===4,'ORIGENS_DAS_TAREFAS_EXATAS');await save();
  await operator.post('manual-address',`/api/v1/unidades-logisticas/${f.units[0].codigo}/movimentos`,{operacaoId:op(),versaoUnidade:f.units[0].versao,medidas:measures,destinos:[dest(f.ARM1)],motivo:reason});
  check((await totals()).enderecamento===report.baseline.enderecamento+3,'ENDERECAMENTO_MANUAL_CONCLUI_PENDENCIA_DA_FILA');
  const pairTask=f.addressTasks.find(t=>t.origemId===f.units[3].id),pairClaim=await claim(operator,'pair-claim',pairTask);
  const pairDest=await operator.get(taskUrl(pairTask.id)+'/destinos?codigo='+encodeURIComponent(f.PAR1.codigo)+'&segundoCodigo='+encodeURIComponent(f.PAR2.codigo));
  check(pairDest.conjuntoId===f.pair.id&&pairDest.destinos.length===2,'PAR_DE_POSICOES_RESOLVE_CONJUNTO_ATIVO');
  const pairOp=op();const pairDone=await operator.post('pair-address',taskUrl(pairTask.id)+'/enderecar',{operacaoId:pairOp,codigoLido:f.units[3].codigo,posicionar:{operacaoId:pairOp,versaoUnidade:pairClaim.unidade.unidade.versao,medidas:{...measures,posicoesNecessarias:2},conjuntoId:pairDest.conjuntoId,destinos:pairDest.destinos,motivo:reason}});
  check(pairDone.tarefa.situacao==='CONCLUIDA'&&pairDone.unidade.posicoes.length===2,'ENDERECAMENTO_EM_DUAS_POSICOES_PRESERVA_UNIDADE');
  const raceTask=f.addressTasks.find(t=>t.origemId===f.units[2].id);
  if(!report.commands.race){
   const outcomes=await Promise.all([operator.request('POST',taskUrl(raceTask.id)+'/iniciar',{operacaoId:op(),versao:raceTask.versao,motivo:reason},[200,409]),supervisor.request('POST',taskUrl(raceTask.id)+'/iniciar',{operacaoId:op(),versao:raceTask.versao,motivo:reason},[200,409])]);
   check(outcomes.map(r=>r.status).sort().join(',')==='200,409','DOIS_OPERADORES_REAIS_UM_UNICO_RESPONSAVEL');report.commands.race={done:true};await save();
  }
  let current=(await supervisor.get(taskUrl(raceTask.id))).tarefa;
  if(current.responsavel!==supervisor.profile.id){await release(supervisor,'supervisor-redistribute',current);current=(await supervisor.get(taskUrl(raceTask.id))).tarefa;current=(await claim(supervisor,'supervisor-claim',current)).tarefa;}
  await operator.request('POST',taskUrl(raceTask.id)+'/liberar',{operacaoId:op(),versao:current.versao,motivo:reason},403);check(true,'OPERACAO_NAO_DEVOLVE_TAREFA_DE_OUTRO_USUARIO');
  const deniedOp=op();await operator.request('POST',taskUrl(raceTask.id)+'/enderecar',{operacaoId:deniedOp,codigoLido:f.units[2].codigo,posicionar:{operacaoId:deniedOp,versaoUnidade:f.units[2].versao,medidas:measures,destinos:[dest(f.ARM3)],motivo:reason}},409);check(true,'OPERACAO_NAO_EXECUTA_TAREFA_DE_OUTRO_USUARIO');
  current=(await release(supervisor,'release-to-operator',current)).tarefa;
  f.claimBeforeRestart=(await claim(operator,'operator-reclaim',current)).tarefa;
  const repeated=await operator.request('POST',taskUrl(raceTask.id)+'/iniciar',report.commands['operator-reclaim'].body);check(repeated.body.tarefa.versao===f.claimBeforeRestart.versao,'MESMO_ATOR_REPLAY_NAO_DUPLICA_INICIO');
  await supervisor.request('POST',taskUrl(raceTask.id)+'/iniciar',report.commands['operator-reclaim'].body,409);check(true,'OUTRO_ATOR_NAO_REUTILIZA_CHAVE_ALHEIA');
  await operator.request('GET','/api/v1/tarefas-coletor/resumo?clienteId=88&armazemId=31',undefined,403);check(true,'CLIENTE_FORA_DO_ESCOPO_RECUSADO');
  await operator.request('GET','/api/v1/tarefas-coletor/resumo?clienteId=64&armazemId=51',undefined,403);check(true,'ARMAZEM_FORA_DO_ESCOPO_RECUSADO');
  await operator.request('GET','/api/v1/tarefas-coletor/pedido-saida/117',undefined,403);check(true,'ATALHO_DE_PEDIDO_ALHEIO_NAO_REVELA_TAREFA');
  check((await totals()).enderecamento===report.baseline.enderecamento+2,'CONTADOR_INCLUI_PENDENTE_E_EM_ANDAMENTO');
  report.state='AGUARDANDO_REINICIO_DEV';await save();
 }else if(['enderecar','inventario','separar','posicao','mobile'].includes(stage)){
  const {runBrowserStage}=await import('./profundo-browser.mjs');
  await runBrowserStage(stage,{report,secret,save,check,f,operator,supervisor,manager,taskUrl,totals,reason,op});
 }else if(stage==='final'){
  const count=await totals();check(['enderecamento','separacao','inventario'].every(k=>count[k]===report.baseline[k]),'TRES_CONTADORES_RETORNAM_A_BASE_SEM_AFETAR_OUTRAS_FILAS');
  await operator.request('GET',taskUrl(f.outputTask.id),undefined,401,false);check(true,'TAREFA_EXIGE_AUTENTICACAO_ATUAL');
  await operator.request('GET',taskUrl(f.inventoryTask.id)+'/unidade?codigo='+f.units[0].codigo,undefined,400);check(true,'TAREFA_INVENTARIO_NAO_EXPOE_SALDO_POR_ATALHO_DE_UNIDADE');
  const lost=report.ui['address-lost'];await operator.request('POST',lost.url,{...lost.body,codigoLido:op()},409);check(true,'MESMA_CHAVE_COM_OUTRO_CONTEUDO_RECUSADA');
  const sourcePaths=['frontend/src/modules/saida/definition.ts','frontend/src/modules/saida/continuation.ts','frontend/src/modules/saida/DispatchTaskStatus.tsx','frontend/src/components/records/RecordDialog.tsx','frontend/src/components/Collector.tsx','frontend/src/modules/coletor/CollectorTasks.tsx','frontend/src/modules/coletor/CollectorTaskDialog.tsx','frontend/src/modules/coletor/CollectorTaskExecution.tsx','frontend/src/modules/regularizacao/inventory.css','frontend/src/styles/collector.css','frontend/src/domain/recordPages.ts','frontend/tests/collector-tasks.test.tsx'];
  const fingerprints=async()=>Object.fromEntries(await Promise.all(sourcePaths.map(async p=>[p,createHash('sha256').update(await readFile(p)).digest('hex')])));
  report.frontendFiles=await fingerprints();await save();
  const {runBrowserStage}=await import('./profundo-browser.mjs');await runBrowserStage('final',{report,secret,save,check,f,operator,supervisor,manager,taskUrl,totals,reason,op});
  check(JSON.stringify(report.frontendFiles)===JSON.stringify(await fingerprints()),'FONTES_DAS_DUAS_TELAS_ESTAVEIS_DURANTE_PROVA_FINAL');
  report.state='APLICACAO_APROVADA';report.finished=new Date().toISOString();await save();
 }else throw new Error('ETAPA_AINDA_NAO_IMPLEMENTADA');
 console.log(JSON.stringify({state:report.state,checks:report.checks.length,stage,prefix:report.prefix}));
}catch(e){report.state='INTERROMPIDO';report.failure=String(e.message).slice(0,240);await save();console.log(JSON.stringify({state:report.state,stage,checks:report.checks.length,failure:report.failure,lastFailure:report.lastFailure}));process.exitCode=1;}
