import {readFile,writeFile,rename,access} from 'node:fs/promises';
import {randomUUID} from 'node:crypto';
import path from 'node:path';
import assert from 'node:assert/strict';
const area=process.argv[2],base='http://127.0.0.1:25582',origin='http://localhost:25581';
let privateText='';for await(const c of process.stdin)privateText+=c;
const secret=JSON.parse(privateText);privateText='';
const resultFile=path.join(area,'ciclo.json');
let report;try{report=JSON.parse(await readFile(resultFile,'utf8'));}catch{report={id:'BE10-NOTAZZ-SQL01',prefix:'NFH23-'+randomUUID().slice(0,8),state:'RUNNING',checks:[],fixtures:{},commands:{},attempts:[],http:[],externalCalls:0,sql:'WMS_DEV/WMSDEV'};}
report.attempts.push({started:new Date().toISOString(),previous:report.state});report.state='RUNNING';
let token='',csrf='',csrfHeader='';const cookies=new Map();
const save=()=>writeFile(resultFile,JSON.stringify(report,null,2));
const reason='Teste interno ficticio de persistencia fiscal '+report.prefix;
const check=(ok,code)=>{assert.ok(ok,code);if(!report.checks.includes(code))report.checks.push(code);};
const exists=async f=>{try{await access(path.join(area,f));return true;}catch{return false;}};
async function req(method,url,data,expected=200,auth=true){
 const r=await fetch(base+url,{method,headers:{Origin:origin,...(cookies.size?{Cookie:[...cookies].map(([k,v])=>k+'='+v).join('; ')}:{}),...(token&&auth?{Authorization:'Bearer '+token}:{}),...(csrf&&method!=='GET'?{[csrfHeader]:csrf}:{}),...(data?{'Content-Type':'application/json'}:{})},...(data?{body:JSON.stringify(data)}:{}),signal:AbortSignal.timeout(45000)});
 for(const c of r.headers.getSetCookie()){const first=c.split(';',1)[0],i=first.indexOf('=');if(i>0)cookies.set(first.slice(0,i),first.slice(i+1));}
 let body;try{body=await r.json();}catch{body=null;}
 if(!url.startsWith('/api/auth/')){report.http.push({method,url,status:r.status,expected});await save();}
 if(!(Array.isArray(expected)?expected.includes(r.status):r.status===expected)){
  report.lastError={method,url,status:r.status,code:body?.codigo??body?.code??null,details:body?.mensagem??body?.message??null};
  throw new Error('HTTP_'+r.status+'_'+method+'_'+url.split('?')[0]);
 }
 return body;
}
async function command(name,method,url,data,expected=200){
 const prior=report.commands[name];
 if(prior?.done)return prior.response;
 if(prior?.uncertain)throw new Error('COMANDO_INCERTO_'+name);
 const body=prior?.body??data;
 report.commands[name]={method,url,body,uncertain:true};await save();
 let response;try{response=await req(method,url,body,expected);}catch(e){
  if(report.lastError?.url===url)report.commands[name].uncertain=false;
  await save();throw e;
 }
 report.commands[name]={method,url,body,response,done:true,uncertain:false};await save();return response;
}
const post=(name,url,data,expected=200)=>command(name,'POST',url,data,expected);
const get=url=>req('GET',url);
const uuid=()=>randomUUID();
const dest=a=>({enderecoId:a.id,codigoLido:a.codigo});
const cpfIssuer='00000000090701',cpfOwner='00000000090702';
function key(cnpj,num){const b='352610'+cnpj+'55'+'999'+String(num).padStart(9,'0')+'1'+'00000001';let sum=0;for(let i=0;i<43;i++)sum+=Number(b[42-i])*(2+i%8);const d=11-sum%11;return b+(d>=10?0:d);}
const fiscalData=name=>({razaoSocial:name,inscricaoEstadual:'ISENTO',logradouro:'Rua Ficticia',numeroEndereco:'1',bairro:'Ficticio',cidade:'Osasco',uf:'SP',cep:'06000000',pais:'BR',codigoMunicipio:'3534401',indicadorIe:9,agrupamentoNfe:'CONSOLIDAR'});
async function address(name,type,index,warehouse){return post(name,'/api/v1/enderecos',{armazemId:warehouse.id,codigo:report.prefix+'-'+name,rua:'NFH',nivel:0,posicao:String(index),descricao:'Posicao ficticia '+name,tipo:type,capacidadePesoKg:10000,alturaMetros:10,larguraMetros:10,profundidadeMetros:10,empilhamentoMaximo:10,sequenciaColeta:index,tipoUnidadePermitido:'TODAS'},201);}
async function createCase(name,index,client,warehouse){
 const product=await post(name+'Produto','/api/v1/produtos',{clienteId:client.id,sku:report.prefix+'-'+name,descricao:'Produto ficticio '+name,unidadeMedida:'UN',tipoQuantidade:'CONTAGEM',precisaoQuantidade:0,controlaLote:false,controlaValidade:false},201);
 const pack=await post(name+'Pack','/api/v1/embalagens',{produtoId:product.id,codigoDun:report.prefix+'D'+index,descricao:'Embalagem ficticia',quantidadeProduto:1},201);
 const storage=await address(name+'Arm','ARMAZENAGEM',index*3,warehouse),separation=await address(name+'Sep','SEPARACAO',index*3+1,warehouse),returnAddress=await address(name+'Ret','ARMAZENAGEM',index*3+2,warehouse);
 for(const [n,cfop] of [['RETORNO_MERCADORIA','5906'],['RETORNO_SIMBOLICO','5907'],['REGULARIZACAO_ENTRADA','1905']])await post(name+'Ref'+n,`/api/v1/produtos/${product.id}/referencias-fiscais`,{operacaoId:uuid(),armazemId:warehouse.id,operacao:n,versao:0,ncm:'39269090',cfop,aliquotaIcms:0,aliquotaIpi:0,fonte:'FICTICIO TRANSPORTE CONTROLADO, SEM VALIDACAO TRIBUTARIA',conferidaPor:'Runner interno ficticio',conferidaEm:new Date(Date.now()-1000).toISOString(),motivo:reason,tributacao:{origem:'0',icmsCst:'41',ipiCst:'99',pisCst:'08',cofinsCst:'08',aliquotaPis:0,aliquotaCofins:0}});
 const entry=await post(name+'Entrada','/api/v1/pedidos-entrada',{clienteId:client.id,armazemId:warehouse.id,referencia:report.prefix+'-E'+index},201);
 let e=await post(name+'Nota',`/api/v1/pedidos-entrada/${entry.id}/notas`,{versao:entry.versao,serie:999,numero:907000+index,emissao:'2026-10-23',chaveAcesso:key(cpfOwner,907000+index),itens:[{numeroItem:1,produtoId:product.id,quantidadePrevista:10,valorMercadoria:100}]});
 e=await post(name+'Conferencia',`/api/v1/pedidos-entrada/${entry.id}/iniciar-conferencia`,{versao:e.versao,motivo:reason});
 const ed=await get(`/api/v1/pedidos-entrada/${entry.id}`);
 e=await post(name+'Chegada',`/api/v1/pedidos-entrada/${entry.id}/chegadas`,{operacaoId:uuid(),versao:e.versao,chegouEm:new Date(Date.now()-1000).toISOString(),observacao:reason,itens:[{itemNotaId:ed.notas[0].itens[0].id,quantidadeBoa:10,quantidadeAvariada:0}]});
 e=await post(name+'Efetivar',`/api/v1/pedidos-entrada/${entry.id}/efetivacao`,{versao:e.versao,aceitarDivergencias:false,motivo:reason});
 const entries=await get(`/api/v1/pedidos-entrada/${entry.id}/entradas`);
 await post(name+'Unitizar',`/api/v1/pedidos-entrada/${entry.id}/entradas/${entries.itens[0].id}/unitizacao`,{operacaoId:uuid(),versaoPedido:e.versao,motivo:reason,unidades:[{embalagemId:pack.id,tipo:'VOLUME',condicao:'BOA',quantidade:10}]});
 const units=await get(`/api/v1/pedidos-entrada/${entry.id}/unidades`),unit=units.itens[0];
 await post(name+'Posicionar',`/api/v1/unidades-logisticas/${unit.codigo}/movimentos`,{operacaoId:uuid(),versaoUnidade:unit.versao,medidas:{pesoKg:1,alturaMetros:1,larguraMetros:1,profundidadeMetros:1,empilhamento:1,posicoesNecessarias:1},destinos:[dest(storage)],motivo:reason});
 let order=(await post(name+'Saida','/api/v1/pedidos-saida',{operacaoId:uuid(),clienteId:client.id,armazemId:warehouse.id,referencia:report.prefix+'-'+name,itens:[{produtoId:product.id,quantidade:4}],motivo:reason},201)).pedido;
 order=(await post(name+'Reservar',`/api/v1/pedidos-saida/${order.id}/reserva`,{operacaoId:uuid(),versao:order.versao,motivo:reason})).pedido;
 const u=await get(`/api/v1/unidades-logisticas/${unit.codigo}/estoque`),reservation=order.reservas[0];
 let detail=(await post(name+'Ler',`/api/v1/pedidos-saida/${order.id}/leituras`,{operacaoId:uuid(),versao:order.versao,reservaId:reservation.id,codigoLido:unit.codigo,revisaoConteudo:u.unidade.revisaoConteudo,motivo:reason})).expedicao;
 detail=(await post(name+'Separar',`/api/v1/pedidos-saida/${order.id}/separacoes`,{operacaoId:uuid(),versao:detail.pedido.versao,destinacao:{reservaId:reservation.id,destinos:[dest(separation)]},motivo:reason})).expedicao;
 check(detail.pedido.situacao==='SEPARADO','PEDIDO_'+name+'_SEPARADO_SQL');
 const f={product,pack,storage,separation,returnAddress,entry,unit,order:detail.pedido,reservation};report.fixtures[name]=f;await save();return f;
}
async function status(f){return get(`/api/v1/pedidos-saida/${f.order.id}/fiscal`);}
async function issue(f,nature,source,name){
 const url=`/api/v1/pedidos-saida/${f.order.id}/fiscal`,d=await get(`/api/v1/pedidos-saida/${f.order.id}/expedicao`);
 if(!report.commands[name+'Confirmar']?.done){
  const p=await req('POST',url+'/previa',{versao:d.pedido.versao,natureza:nature,documentoOrigemId:source??null});
  report.previews??={};report.previews[name]=p;await save();
  check(p.podeEmitir,'PREVIA_'+name+'_SEM_PENDENCIAS');
  check(Number(p.documentos[0].valorTotal)===40,'VALOR_'+name+'_PROPORCIONAL_40');
  const stock=await get(`/api/v1/unidades-logisticas/${f.unit.codigo}/estoque`);
  const cmd={operacaoId:uuid(),versao:p.versao,hash:p.hash,natureza:nature,documentoOrigemId:source??null,motivo:reason};
  await post(name+'Confirmar',url+'/emissoes',cmd);
  const after=await status(f),s=after.solicitacoes.find(x=>x.natureza===nature);
  check(!!s,'SOLICITACAO_'+name+'_DURAVEL');
  check(JSON.stringify(await get(`/api/v1/unidades-logisticas/${f.unit.codigo}/estoque`))===JSON.stringify(stock),'FISCAL_'+name+'_NAO_MOVIMENTA_ESTOQUE');
  const repeat=await req('POST',url+'/emissoes',cmd);
  check(repeat.solicitacoes.filter(x=>x.natureza===nature).length===1,'CONFIRMACAO_'+name+'_IDEMPOTENTE');
  await req('POST',url+'/emissoes',{...cmd,motivo:reason+' alterado'},409);
  if(!report.restartRequested){
   report.restartRequested=true;report.pendingAfterRestart={pedidoId:f.order.id,solicitacaoId:s.id,operation:cmd.operacaoId};
   report.state='WAITING_OWN_RUNNER_RESTART';await save();
   process.exitCode=75;throw new Error('RESTART_CONTROLADO');
  }
 }
 const result=await status(f),s=result.solicitacoes.find(x=>x.natureza===nature);
 check(!!s,'SOLICITACAO_'+name+'_RECUPERADA');
 await writeFile(path.join(area,'liberar-'+s.id),'');
 let actual=s;
 for(let i=0;i<12&&actual.situacao!=='AUTORIZADA';i++){
  await new Promise(r=>setTimeout(r,850));
  const next=await req('POST',url+'/'+s.id+'/processamento',undefined,[200,409]);
  actual=next.solicitacoes?.find(x=>x.id===s.id)??(await status(f)).solicitacoes.find(x=>x.id===s.id);
 }
 check(actual.situacao==='AUTORIZADA','SOLICITACAO_'+name+'_AUTORIZADA_CONCILIADA');
 const ledger=JSON.parse(await readFile(path.join(area,'provider-ledger.json'),'utf8'));
 check(ledger.criacoes[s.id]===1,'RESPOSTA_PERDIDA_'+name+'_SEM_REPETIR_CRIACAO');
 const file=await get(url+'/'+s.id+'/xml');check(file.conteudo.includes(actual.chaveAcesso),'XML_'+name+'_GRAVADO_E_RECUPERADO');
 report.issued??={};report.issued[name]={...actual,xml:file.conteudo};await save();return report.issued[name];
}
try{
 const csrfData=await req('GET','/api/auth/csrf',undefined,200,false);csrf=csrfData.token;csrfHeader=csrfData.header;
 const login=await req('POST','/api/auth/entrar',{email:secret.email,senha:secret.senha},200,false);secret.senha='';token=login.accessToken;
 check(login.usuario.perfil==='GESTOR'&&!login.usuario.trocarSenha,'GESTOR_FICTICIO_AUTENTICADO');
 const client=await post('Cliente','/api/v1/clientes',{codigo:report.prefix,nome:'Cliente ficticio '+report.prefix,documentoFiscal:cpfOwner},201);
 const warehouse=await post('Armazem','/api/v1/armazens',{codigo:report.prefix,nome:'Armazem ficticio '+report.prefix,documentoFiscal:cpfIssuer,cidade:'Osasco',uf:'SP'},201);
 for(const [name,type,obj] of [['CFCliente','clientes',client],['CFArmazem','armazens',warehouse]]){
  const c=await get(`/api/v1/${type}/${obj.id}/complemento-fiscal`);
  await post(name,`/api/v1/${type}/${obj.id}/complemento-fiscal`,{operacaoId:uuid(),versao:c.versao,dados:fiscalData(obj.nome),motivo:reason});
 }
 report.fixtures.cliente=client;report.fixtures.armazem=warehouse;await save();
 await writeFile(path.join(area,'fixture-plan.json'),JSON.stringify({armazemId:warehouse.id,prefix:report.prefix}));
 for(let i=0;i<100&&!await exists('provider-ready.json');i++)await new Promise(r=>setTimeout(r,100));
 check(await exists('provider-ready.json'),'CONFIGURACAO_FICTICIA_ISOLADA');
 const cases=[];for(const [i,name] of ['CANCEL','AGED','RETURN','SYMBOLIC'].entries())cases.push(await createCase(name,i+1,client,warehouse));
 const [cancel,aged,returned,symbolic]=cases;
 if(report.pendingAfterRestart){
  const p=report.pendingAfterRestart;
  check((await get(`/api/v1/pedidos-saida/${p.pedidoId}/fiscal`)).solicitacoes.some(s=>s.id===p.solicitacaoId),'SOLICITACAO_SOBREVIVE_REINICIO_DO_PROCESSO');
 }
 if(!report.cancelled){
  const s=await issue(cancel,'RETORNO_MERCADORIA',null,'CANCEL');
  await req('POST',`/api/v1/pedidos-saida/${cancel.order.id}/documentos/${s.documentoId}/cancelamento`,{operacaoId:uuid(),versao:(await get(`/api/v1/pedidos-saida/${cancel.order.id}/expedicao`)).pedido.versao,motivo:reason},409);
  const commandData={operacaoId:uuid(),motivo:reason+' desistência antes da retirada'};
  await post('CANCELCancelamento',`/api/v1/pedidos-saida/${cancel.order.id}/fiscal/${s.id}/cancelamento`,commandData);
  check((await status(cancel)).solicitacoes.find(x=>x.id===s.id).situacao==='CANCELADA','CANCELAMENTO_CONFIRMADO_POR_EVENTO_XML');
  const detail=await get(`/api/v1/pedidos-saida/${cancel.order.id}/expedicao`);
  check(detail.pedido.situacao==='SEPARADO','CANCELAMENTO_FISCAL_PRESERVA_FATO_FISICO');
  const body={operacaoId:uuid(),versao:detail.pedido.versao,unidades:[{reservaId:cancel.reservation.id,destinos:[dest(cancel.returnAddress)]}],motivo:reason};
  const restored=await post('CANCELRetorno',`/api/v1/pedidos-saida/${cancel.order.id}/retorno-interno`,body);
  check(restored.expedicao.pedido.situacao==='CANCELADO','CANCELAMENTO_E_REENDERECAMENTO_CONFIRMADOS');
  check(Number((await get(`/api/v1/unidades-logisticas/${cancel.unit.codigo}/estoque`)).unidade.quantidade)===10,'CANCELAMENTO_ESTOQUE_ORIGINAL_10');
  await req('POST',`/api/v1/pedidos-saida/${cancel.order.id}/retorno-interno`,body);
  report.cancelled=true;await save();
 }
 if(!report.aged){
  const s=await issue(aged,'RETORNO_MERCADORIA',null,'AGED');
  await req('POST',`/api/v1/pedidos-saida/${aged.order.id}/fiscal/${s.id}/cancelamento`,{operacaoId:uuid(),motivo:reason+' após prazo de cancelamento'},409);
  const incoming=await issue(aged,'REGULARIZACAO_ENTRADA',s.documentoId,'AGEDENTRY');
  const detail=await get(`/api/v1/pedidos-saida/${aged.order.id}/expedicao`);
  const body={operacaoId:uuid(),versao:detail.pedido.versao,unidades:[{reservaId:aged.reservation.id,destinos:[dest(aged.returnAddress)]}],regularizacoesFiscais:[incoming.id],motivo:reason};
  await post('AGEDRetorno',`/api/v1/pedidos-saida/${aged.order.id}/retorno-interno`,body);
  await req('POST',`/api/v1/pedidos-saida/${aged.order.id}/retorno-interno`,body);
  const after=await get(`/api/v1/pedidos-saida/${aged.order.id}/expedicao`);
  check(after.documentos.find(d=>String(d.id)===String(s.documentoId)).situacao==='AUTORIZADO','APOS_24H_SAIDA_FISCAL_ORIGINAL_PRESERVADA');
  check(Number((await get(`/api/v1/unidades-logisticas/${aged.unit.codigo}/estoque`)).unidade.quantidade)===10,'REGULARIZACAO_SEM_TRANSITO_NAO_DUPLICA_ESTOQUE');
  report.aged=true;await save();
 }
 if(!report.returned){
  const s=await issue(returned,'RETORNO_MERCADORIA',null,'RETURN');
  let detail=await get(`/api/v1/pedidos-saida/${returned.order.id}/expedicao`);
  await post('RETURNRetirada',`/api/v1/pedidos-saida/${returned.order.id}/retirada`,{operacaoId:uuid(),versao:detail.pedido.versao,xmls:[s.xml],remanescentes:[{reservaId:returned.reservation.id,destinos:[dest(returned.returnAddress)]}],motivo:reason});
  check(Number((await get(`/api/v1/unidades-logisticas/${returned.unit.codigo}/estoque`)).unidade.quantidade)===6,'RETIRADA_PARCIAL_DEZ_MENOS_QUATRO_IGUAL_SEIS');
  await req('POST',`/api/v1/pedidos-saida/${returned.order.id}/fiscal/${s.id}/cancelamento`,{operacaoId:uuid(),motivo:reason+' circulação impede cancelamento'},409);
  const incoming=await issue(returned,'REGULARIZACAO_ENTRADA',s.documentoId,'RETURNENTRY');
  const op=await get(`/api/v1/pedidos-saida/${returned.order.id}/fiscal/${incoming.id}/retorno`);
  check(op.houveRetirada&&op.itens.length===1,'NOVA_ENTRADA_VINCULADA_AO_RETORNO_REAL');
  const body={operacaoId:uuid(),versao:op.versao,referencia:report.prefix+'-DEV',nota:op.nota,chegadaReal:new Date(Date.now()-1000).toISOString(),itens:op.itens,solicitacaoEntradaId:incoming.id,motivo:reason};
  const response=await post('RETURNDevolver',`/api/v1/pedidos-saida/${returned.order.id}/devolucoes`,body);
  const replay=await req('POST',`/api/v1/pedidos-saida/${returned.order.id}/devolucoes`,body);
  check(String(replay.pedidoEntradaId)===String(response.pedidoEntradaId),'DEVOLUCAO_IDEMPOTENTE_MESMA_ENTRADA');
  const e=await get(`/api/v1/pedidos-entrada/${response.pedidoEntradaId}`);
  check(Number(e.notas[0].itens[0].valorMercadoria)===40,'RETORNO_CONSERVA_VALOR_DA_PARCELA_ORIGINAL');
  const entries=await get(`/api/v1/pedidos-entrada/${response.pedidoEntradaId}/entradas`);
  await post('RETURNUnitizar',`/api/v1/pedidos-entrada/${response.pedidoEntradaId}/entradas/${entries.itens[0].id}/unitizacao`,{operacaoId:uuid(),versaoPedido:e.pedido.versao,motivo:reason,unidades:[{embalagemId:returned.pack.id,tipo:'VOLUME',condicao:'BOA',quantidade:4}]});
  const units=await get(`/api/v1/pedidos-entrada/${response.pedidoEntradaId}/unidades`),unit=units.itens[0];
  await post('RETURNPosicionar',`/api/v1/unidades-logisticas/${unit.codigo}/movimentos`,{operacaoId:uuid(),versaoUnidade:unit.versao,medidas:{pesoKg:1,alturaMetros:1,larguraMetros:1,profundidadeMetros:1,empilhamento:1,posicoesNecessarias:1},destinos:[dest(returned.storage)],motivo:reason});
  detail=await get(`/api/v1/pedidos-saida/${returned.order.id}/expedicao`);
  check(detail.pedido.situacao==='RETIRADO'&&detail.documentos.find(d=>String(d.id)===String(s.documentoId)).situacao==='AUTORIZADO','RETORNO_PRESERVA_SAIDA_E_NOTA_ORIGINAIS');
  report.returned={pedidoEntradaId:response.pedidoEntradaId,unit};await save();
 }
 if(!report.symbolic){
  const before=await get(`/api/v1/unidades-logisticas/${symbolic.unit.codigo}/estoque`);
  await issue(symbolic,'RETORNO_SIMBOLICO',null,'SYMBOLIC');
  check(JSON.stringify(await get(`/api/v1/unidades-logisticas/${symbolic.unit.codigo}/estoque`))===JSON.stringify(before),'RETORNO_SIMBOLICO_PODE_SER_SOMENTE_FISCAL');
  report.symbolic=true;await save();
 }
 const rows=await get(`/api/v1/pedidos-saida/fiscal?clienteId=${client.id}&armazemId=${warehouse.id}&situacao=FINALIZADO&pagina=0&tamanho=25`);
 check(rows.itens.some(x=>String(x.id)===String(returned.order.id)&&x.numerosNfe.length>0),'LISTAGEM_FINALIZADA_COM_NUMERO_NFE');
 const ledger=JSON.parse(await readFile(path.join(area,'provider-ledger.json'),'utf8'));
 check(Object.values(ledger.criacoes).every(n=>n===1),'TODAS_SOLICITACOES_CRIADAS_UMA_UNICA_VEZ');
 report.state='PASS_SQL_REAL_TRANSPORTE_CONTROLADO';report.finished=new Date().toISOString();await save();
 console.log(JSON.stringify({state:report.state,checks:report.checks.length,fixtures:{cliente:client.id,armazem:warehouse.id},externalCalls:0}));
}catch(e){
 if(e.message==='RESTART_CONTROLADO'){console.log(JSON.stringify({state:report.state,checks:report.checks.length,restart:report.pendingAfterRestart}));}
 else{report.state='FAIL';report.failure=e instanceof assert.AssertionError?e.message.split('\n')[0]:String(e.message).slice(0,180);report.finished=new Date().toISOString();await save();process.exitCode=20;console.log(JSON.stringify({state:report.state,failure:report.failure,lastError:report.lastError,checks:report.checks.length}));}
}finally{token='';csrf='';secret.senha='';cookies.clear();}
