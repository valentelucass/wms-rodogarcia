import {readFile,writeFile} from 'node:fs/promises';
import {randomUUID} from 'node:crypto';
import assert from 'node:assert/strict';
import path from 'node:path';
const area=process.argv[2];let input='';for await(const chunk of process.stdin)input+=chunk;
const secret=JSON.parse(input);input='';const base=secret.base;
assert.ok(['localhost','127.0.0.1'].includes(new URL(base).hostname)&&new URL(base).port==='25581','ORIGEM_DEV_RECUSADA');
const file=path.join(area,'fluxos.json');let report;
try{report=JSON.parse(await readFile(file,'utf8'));}catch{report={id:'COLETOR-TAREFAS01',prefix:'CT28-'+randomUUID().slice(0,8),phase:1,checks:[],commands:{},fixtures:{},http:[],attempts:[],externalCalls:0,sql:'WMS_DEV/WMSDEV'};}
report.attempts.push({at:new Date().toISOString(),sourceHash:secret.sourceHash,jarSha256:secret.jarSha256,phase:report.phase,previousState:report.state,previousFailure:report.failure});
delete report.failure;delete report.lastError;
const save=()=>writeFile(file,JSON.stringify(report,null,2));
const check=(ok,name)=>{if(report.checks.includes(name))return;assert.ok(ok,name);report.checks.push(name);};
const reason='Conferência fictícia das tarefas '+report.prefix;
const cookies=new Map();let token='',csrf='',csrfHeader='';
async function request(method,url,data,expected=200,auth=true){
 const r=await fetch(base+url,{method,headers:{Origin:base,...(cookies.size?{Cookie:[...cookies].map(([k,v])=>k+'='+v).join('; ')}:{}),...(auth&&token?{Authorization:'Bearer '+token}:{}),...(csrf&&method!=='GET'?{[csrfHeader]:csrf}:{}),...(data?{'Content-Type':'application/json'}:{})},...(data?{body:JSON.stringify(data)}:{}),signal:AbortSignal.timeout(45000)});
 for(const cookie of r.headers.getSetCookie()){const first=cookie.split(';',1)[0],i=first.indexOf('=');if(i>0)cookies.set(first.slice(0,i),first.slice(i+1));}
 let body;try{body=await r.json();}catch{body=null;}
 if(!url.startsWith('/api/auth/')){report.http.push({method,url,status:r.status,expected});await save();}
 if(!(Array.isArray(expected)?expected.includes(r.status):r.status===expected)){report.lastError={method,url,status:r.status,code:body?.codigo??null,message:body?.mensagem??null};await save();throw new Error('HTTP_'+r.status+'_'+url);}
 return {body,status:r.status};
}
const get=async url=>(await request('GET',url)).body;
async function post(name,url,body,status=200){
 const prior=report.commands[name];if(prior?.done)return prior.response;if(prior?.uncertain)throw new Error('COMANDO_INCERTO_'+name);
 const data=prior?.body??body;report.commands[name]={url,body:data,uncertain:true};await save();
 let result;try{result=(await request('POST',url,data,status)).body;}catch(e){if(report.lastError?.url===url)report.commands[name].uncertain=false;await save();throw e;}
 report.commands[name]={url,body:data,response:result,done:true,uncertain:false};await save();return result;
}
const taskUrl=id=>'/api/v1/tarefas-coletor/'+id;
const dest=a=>({enderecoId:a.id,codigoLido:a.codigo});
const measures={pesoKg:1,alturaMetros:1,larguraMetros:1,profundidadeMetros:1,empilhamento:1,posicoesNecessarias:1};
function key(cnpj,num){const b='352610'+cnpj+'55999'+String(num).padStart(9,'0')+'100000001';let sum=0;for(let i=0;i<43;i++)sum+=Number(b[42-i])*(2+i%8);const d=11-sum%11;return b+(d>=10?0:d);}
async function tasks(type){return(await get(`/api/v1/tarefas-coletor?tipo=${type}&clienteId=${report.fixtures.client.id}&armazemId=${report.fixtures.warehouse.id}&tamanho=100`)).itens;}
async function totals(){return get(`/api/v1/tarefas-coletor/resumo?clienteId=${report.fixtures.client.id}&armazemId=${report.fixtures.warehouse.id}`);}
async function createAddress(name,type,index){const f=report.fixtures;return post('address-'+name,'/api/v1/enderecos',{armazemId:f.warehouse.id,codigo:report.prefix+'-'+name,rua:'CT28',nivel:0,posicao:String(index),descricao:'Posição de teste '+name,tipo:type,capacidadePesoKg:10000,alturaMetros:10,larguraMetros:10,profundidadeMetros:10,empilhamentoMaximo:10,sequenciaColeta:index,tipoUnidadePermitido:'TODAS'},201);}
async function claim(name,t){return post(name,taskUrl(t.id)+'/iniciar',{operacaoId:randomUUID(),versao:t.versao,motivo:reason});}
try{
 const c=(await request('GET','/api/auth/csrf',undefined,200,false)).body;csrf=c.token;csrfHeader=c.header;
 const auth=(await request('POST','/api/auth/entrar',{email:secret.email,senha:secret.senha},200,false)).body;secret.senha='';token=auth.accessToken;
 check(auth.usuario.perfil==='GESTOR'&&!auth.usuario.trocarSenha,'AUTENTICACAO_LEGITIMA_FICTICIA');
 check((await get('/api/v1/tarefas-coletor/disponibilidade')).disponivel,'RECURSO_ATIVO_SQL_DEV');
 const f=report.fixtures;
 if(report.phase===1){
  report.clientDoc??='000000'+String(Number.parseInt(randomUUID().slice(0,8),16)%100000000).padStart(8,'0');
  report.number??=100000000+Number.parseInt(randomUUID().slice(0,8),16)%700000000;await save();
  f.client=await post('client','/api/v1/clientes',{codigo:report.prefix,nome:'Cliente fictício '+report.prefix,documentoFiscal:report.clientDoc},201);
  f.warehouse=await post('warehouse','/api/v1/armazens',{codigo:report.prefix,nome:'Armazém fictício '+report.prefix,documentoFiscal:'000001'+report.clientDoc.slice(6),cidade:'Osasco',uf:'SP'},201);await save();
  f.product=await post('product','/api/v1/produtos',{clienteId:f.client.id,sku:report.prefix,descricao:'Produto fictício do Coletor',unidadeMedida:'UN',tipoQuantidade:'CONTAGEM',precisaoQuantidade:0,controlaLote:false,controlaValidade:false},201);
  f.pack=await post('pack','/api/v1/embalagens',{produtoId:f.product.id,codigoDun:report.prefix+'D',descricao:'Embalagem fictícia',quantidadeProduto:1},201);
  f.storage1=await createAddress('ARM1','ARMAZENAGEM',1);f.storage2=await createAddress('ARM2','ARMAZENAGEM',2);f.separation=await createAddress('SEP','SEPARACAO',3);
  f.entry=await post('entry','/api/v1/pedidos-entrada',{clienteId:f.client.id,armazemId:f.warehouse.id,referencia:report.prefix},201);
  let e=await post('note',`/api/v1/pedidos-entrada/${f.entry.id}/notas`,{versao:f.entry.versao,serie:999,numero:report.number,emissao:'2026-10-28',chaveAcesso:key(report.clientDoc,report.number),itens:[{numeroItem:1,produtoId:f.product.id,quantidadePrevista:10,valorMercadoria:100}]});
  e=await post('conference',`/api/v1/pedidos-entrada/${f.entry.id}/iniciar-conferencia`,{versao:e.versao,motivo:reason});
  const detail=await get(`/api/v1/pedidos-entrada/${f.entry.id}`);
  e=await post('arrival',`/api/v1/pedidos-entrada/${f.entry.id}/chegadas`,{operacaoId:randomUUID(),versao:e.versao,chegouEm:new Date(Date.now()-5000).toISOString(),observacao:reason,itens:[{itemNotaId:detail.notas[0].itens[0].id,quantidadeBoa:10,quantidadeAvariada:0}]});
  e=await post('effective',`/api/v1/pedidos-entrada/${f.entry.id}/efetivacao`,{versao:e.versao,aceitarDivergencias:false,motivo:reason});
  check((await totals()).enderecamento===0,'SEM_TAREFA_ANTES_DA_UNITIZACAO');
  const entries=await get(`/api/v1/pedidos-entrada/${f.entry.id}/entradas`);
  await post('unitize',`/api/v1/pedidos-entrada/${f.entry.id}/entradas/${entries.itens[0].id}/unitizacao`,{operacaoId:randomUUID(),versaoPedido:e.versao,motivo:reason,unidades:[{embalagemId:f.pack.id,tipo:'VOLUME',condicao:'BOA',quantidade:5},{embalagemId:f.pack.id,tipo:'VOLUME',condicao:'BOA',quantidade:5}]});
  f.units=(await get(`/api/v1/pedidos-entrada/${f.entry.id}/unidades`)).itens;
  check((await totals()).enderecamento===2,'DUAS_UNIDADES_GERAM_DUAS_TAREFAS');
  await get(`/api/v1/unidades-logisticas/${f.units[0].codigo}/etiqueta`);await get(`/api/v1/unidades-logisticas/${f.units[0].codigo}/etiqueta`);
  check((await totals()).enderecamento===2,'REIMPRESSAO_NAO_DUPLICA_TAREFA');
  await post('manual-address',`/api/v1/unidades-logisticas/${f.units[0].codigo}/movimentos`,{operacaoId:randomUUID(),versaoUnidade:f.units[0].versao,medidas:measures,destinos:[dest(f.storage1)],motivo:reason});
  check((await totals()).enderecamento===1,'MANUAL_ENCERRA_PENDENCIA_AUTOMATICAMENTE');
  f.addressTask=(await tasks('ENDERECAMENTO'))[0];
  const command={operacaoId:randomUUID(),versao:f.addressTask.versao,motivo:reason};
  if(!report.commands['race']){
   const attempts=await Promise.all([request('POST',taskUrl(f.addressTask.id)+'/iniciar',command,[200,409]),request('POST',taskUrl(f.addressTask.id)+'/iniciar',{...command,operacaoId:randomUUID()},[200,409])]);
   check(attempts.filter(r=>r.status===200).length===1&&attempts.filter(r=>r.status===409).length===1,'DISPUTA_CONCORRENTE_200_409');
   const winner=attempts.find(r=>r.status===200).body;report.commands.race={done:true,response:winner};f.addressClaim=winner;
  }else f.addressClaim=report.commands.race.response;
  const release=await post('release-address',taskUrl(f.addressTask.id)+'/liberar',{operacaoId:randomUUID(),versao:f.addressClaim.tarefa.versao,motivo:reason});
  check(release.tarefa.situacao==='PENDENTE'&&!release.tarefa.responsavel,'DEVOLUCAO_A_FILA_LIBERA_RESPONSAVEL');
  f.addressClaim=await claim('reclaim-address',release.tarefa);
  const claimReplay=await request('POST',taskUrl(f.addressTask.id)+'/iniciar',report.commands['reclaim-address'].body);
  check(claimReplay.body.tarefa.versao===f.addressClaim.tarefa.versao,'INICIO_IDEMPOTENTE_NAO_DUPLICA_RESPONSAVEL');
  f.order=(await post('order','/api/v1/pedidos-saida',{operacaoId:randomUUID(),clienteId:f.client.id,armazemId:f.warehouse.id,referencia:report.prefix,itens:[{produtoId:f.product.id,quantidade:4}],motivo:reason},201)).pedido;
  f.outputTask=(await tasks('SEPARACAO'))[0];check(f.outputTask.situacao==='AGUARDANDO_RESERVA','PEDIDO_GERA_TAREFA_AGUARDANDO_RESERVA');
  await request('POST',taskUrl(f.outputTask.id)+'/iniciar',{operacaoId:randomUUID(),versao:f.outputTask.versao,motivo:reason},409);check(true,'RASCUNHO_NAO_INICIA_SEPARACAO');
  f.oldInventory=await post('old-inventory','/api/v1/inventarios',{operacaoId:randomUUID(),clienteId:f.client.id,armazemId:f.warehouse.id,tipo:'CEGAS',motivo:reason});
  f.inventoryTask=(await tasks('INVENTARIO'))[0];f.inventoryClaim=await claim('claim-old-inventory',f.inventoryTask);
  check(f.inventoryClaim.inventario.unidadesLidas===0&&!f.inventoryClaim.unidade&&!f.inventoryClaim.pedido,'INVENTARIO_NAO_REVELA_BASE_ESPERADA');
  await request('GET',taskUrl(f.inventoryTask.id)+'/unidade?codigo='+f.units[0].codigo,undefined,400);check(true,'ATALHO_NAO_REVELA_ESTOQUE_DO_INVENTARIO');
  const count=await totals();check(count.enderecamento===1&&count.separacao===1&&count.inventario===1,'TRES_CARDS_CONTADORES_REAIS');
  report.phase=2;report.state='AGUARDANDO_REINICIO_DEV';report.claimBeforeRestart=f.addressClaim.tarefa;await save();
  console.log(JSON.stringify({state:report.state,checks:report.checks.length,prefix:report.prefix}));process.exitCode=75;
 }else{
  const current=await get(taskUrl(f.addressTask.id));check(current.tarefa.minha&&current.tarefa.versao===report.claimBeforeRestart.versao,'TAREFA_E_RESPONSAVEL_PERSISTEM_APOS_REINICIO');
  const bad=randomUUID();await request('POST',taskUrl(f.addressTask.id)+'/enderecar',{operacaoId:bad,codigoLido:randomUUID(),posicionar:{operacaoId:bad,versaoUnidade:f.units[1].versao,medidas:measures,destinos:[dest(f.storage2)],motivo:reason}},409);check(true,'ETIQUETA_INCORRETA_RECUSADA');
  const occupied=randomUUID();await request('POST',taskUrl(f.addressTask.id)+'/enderecar',{operacaoId:occupied,codigoLido:current.tarefa.codigoUnidade,posicionar:{operacaoId:occupied,versaoUnidade:current.unidade.unidade.versao,medidas:measures,destinos:[dest(f.storage1)],motivo:reason}},409);check(!(await get(`/api/v1/unidades-logisticas/${current.tarefa.codigoUnidade}/estoque`)).primeiroEnderecamentoEm,'POSICAO_OCUPADA_PRESERVA_ESTOQUE');
  const pos=await get(taskUrl(f.addressTask.id)+'/destinos?codigo='+encodeURIComponent(f.storage2.codigo));check(pos.destinos[0].enderecoId===f.storage2.id,'DESTINO_RESOLVIDO_NO_ARMAZEM_DA_TAREFA');
  const op=randomUUID();const moved=await post('address-task',taskUrl(f.addressTask.id)+'/enderecar',{operacaoId:op,codigoLido:current.tarefa.codigoUnidade,posicionar:{operacaoId:op,versaoUnidade:current.unidade.unidade.versao,medidas:measures,destinos:pos.destinos,motivo:reason}});
  check(moved.tarefa.situacao==='CONCLUIDA','ENDERECAMENTO_CONFIRMA_E_ENCERRA_TAREFA');
  const replay=await request('POST',taskUrl(f.addressTask.id)+'/enderecar',report.commands['address-task'].body);check(replay.body.tarefa.id===moved.tarefa.id&&replay.body.unidade.unidade.versao===moved.unidade.unidade.versao,'ENDERECAMENTO_IDEMPOTENTE_APOS_RESPOSTA_PERDIDA');
  await request('POST',taskUrl(f.addressTask.id)+'/enderecar',{...report.commands['address-task'].body,codigoLido:randomUUID()},409);check(true,'MESMA_CHAVE_COM_OUTRO_CONTEUDO_RECUSADA');
  const oldInventoryCurrent=await get(`/api/v1/inventarios/${f.oldInventory.id}`);
  await post('cancel-old-inventory',taskUrl(f.inventoryTask.id)+'/inventario/cancelar',{operacaoId:randomUUID(),versao:oldInventoryCurrent.inventario.versao,confirmarNaoLidas:false,motivo:reason});check((await totals()).inventario===0,'CANCELAMENTO_ENCERRA_TAREFA_INVENTARIO');
  f.newInventory=await post('new-inventory','/api/v1/inventarios',{operacaoId:randomUUID(),clienteId:f.client.id,armazemId:f.warehouse.id,tipo:'CEGAS',motivo:reason});
  f.newInventoryTask=(await tasks('INVENTARIO'))[0];await claim('claim-new-inventory',f.newInventoryTask);
  let inv=await get(`/api/v1/inventarios/${f.newInventory.id}`);check(!JSON.stringify(inv).includes('esperado'),'CONSULTA_ABERTA_MANTEM_CONTAGEM_CEGA');
  for(const [i,u]of f.units.entries()){
   const location=(await get(`/api/v1/unidades-logisticas/${u.codigo}/estoque`)).posicoes[0];
   const r=await post('count-'+i,taskUrl(f.newInventoryTask.id)+'/inventario/leituras',{operacaoId:randomUUID(),versao:inv.inventario.versao,codigoUnidade:u.codigo,contado:5,enderecoId:location.enderecoId,substituir:false});
   check(!JSON.stringify(r).includes('esperado'),'LEITURA_'+i+'_NAO_REVELA_ESPERADO');inv={inventario:r.inventario};
  }
  f.inventoryReport=await post('finish-inventory',taskUrl(f.newInventoryTask.id)+'/inventario/finalizar',{operacaoId:randomUUID(),versao:inv.inventario.versao,confirmarNaoLidas:true,motivo:reason});check(f.inventoryReport.divergenciasQuantidade===0&&f.inventoryReport.divergenciasEndereco===0,'INVENTARIO_FINALIZA_SEM_AJUSTAR_ESTOQUE');
  let order=(await post('reserve',`/api/v1/pedidos-saida/${f.order.id}/reserva`,{operacaoId:randomUUID(),versao:f.order.versao,motivo:reason})).pedido;
  f.outputClaim=await claim('claim-output',await get(taskUrl(f.outputTask.id)).then(d=>d.tarefa));check(f.outputClaim.tarefa.minha,'RESERVA_LIBERA_INICIO_SEPARACAO');
  const res=order.reservas[0],u=await get(taskUrl(f.outputTask.id)+'/unidade?codigo='+res.codigoUnidade),sepOp=randomUUID();
  const separated=await post('separate-task',taskUrl(f.outputTask.id)+'/separar',{operacaoId:sepOp,leitura:{operacaoId:sepOp,versao:order.versao,reservaId:res.id,codigoLido:res.codigoUnidade,revisaoConteudo:u.revisaoConteudo,motivo:reason,resolverPendentes:false},destinacao:{reservaId:res.id,destinos:[dest(f.separation)]}});
  check(separated.pedido.situacao==='SEPARADO'&&separated.tarefa.situacao==='CONCLUIDA','SEPARACAO_INTEGRADA_ENCERRA_PEDIDO_E_TAREFA');
  const sReplay=await request('POST',taskUrl(f.outputTask.id)+'/separar',report.commands['separate-task'].body);check(sReplay.body.pedido.versao===separated.pedido.versao,'SEPARACAO_IDEMPOTENTE');
  const final=await totals();check(final.enderecamento===0&&final.separacao===0&&final.inventario===0,'TRES_CARDS_ZERADOS_APOS_CONCLUSAO');
  const saldo=await get(`/api/v1/estoque/saldo?clienteId=${f.client.id}&armazemId=${f.warehouse.id}&produtoId=${f.product.id}`);check(Number(saldo.fisicoTotal)===10,'ESTOQUE_FISICO_PRESERVADO_DEZ_UNIDADES');
  const closed=await get(`/api/v1/inventarios/${f.newInventory.id}/relatorio`);check(JSON.stringify(closed)===JSON.stringify(f.inventoryReport),'RELATORIO_FECHADO_IMUTAVEL_APOS_MOVIMENTACAO');
  await request('GET',taskUrl(f.outputTask.id),undefined,401,false);check(true,'SEM_AUTENTICACAO_NAO_ACESSA_TAREFA');
  report.state='APROVADO';report.finished=new Date().toISOString();await save();console.log(JSON.stringify({state:report.state,checks:report.checks.length,http:report.http.length,prefix:report.prefix}));
 }
}catch(e){report.state='INTERROMPIDO';report.failure=String(e.message).slice(0,240);await save();console.log(JSON.stringify({state:report.state,checks:report.checks.length,lastError:report.lastError,failure:report.failure}));process.exitCode=1;}
