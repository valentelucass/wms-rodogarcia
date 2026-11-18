import {createRequire} from 'node:module';
import {mkdir} from 'node:fs/promises';
import path from 'node:path';
import assert from 'node:assert/strict';
const require=createRequire(import.meta.url);
const {chromium}=require('C:/Users/suporte/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const outputs='C:/Users/suporte/Documents/Codex/2026-10-28/wms-farol-supervisor/outputs';
export async function runBrowserStage(stage,ctx){
 const {report,secret,save,check,f,operator,supervisor,manager,taskUrl,totals,reason,op}=ctx;
 const browser=await chromium.launch({channel:'msedge',headless:true});let page;
 report.ui??={};report.browserErrors??=[];
 async function login(actor){
  page=await browser.newPage({viewport:{width:1640,height:950}});page.setDefaultTimeout(30000);
  page.on('pageerror',e=>report.browserErrors.push({stage,name:e.name}));
  await page.goto(secret.base,{waitUntil:'domcontentloaded'});
  await page.getByLabel('E-mail',{exact:true}).fill(actor.credential.email);
  await page.getByLabel('Senha',{exact:true}).fill(report.authInitialized?.[actor.profile.id]?actor.credential.novaSenha:actor.credential.senha);
  await page.getByRole('button',{name:'Entrar',exact:true}).click();
  const context=page.getByRole('form',{name:'Contexto operacional'});
  await context.getByRole('combobox',{name:'Cliente',exact:true}).fill(f.client.codigo);
  await page.getByRole('option',{name:new RegExp(f.client.nome)}).click();
  await context.getByRole('combobox',{name:'Armazém',exact:true}).fill(f.warehouse.codigo);
  await page.getByRole('option',{name:new RegExp(f.warehouse.nome)}).click();
  await context.getByRole('button',{name:'Aplicar contexto',exact:true}).click();
 }
 async function navigation(name){await page.setViewportSize({width:1640,height:950});await page.getByRole('button',{name,exact:true}).click();}
 async function collector(){await navigation('Coletor');await page.getByRole('heading',{name:'Tarefas disponíveis',exact:true}).waitFor();await page.locator('.collector-queue-count').filter({hasText:/^\d+$/}).first().waitFor();}
 async function mutate(name,url,click,expected=200){
  if(report.ui[name]?.done)return report.ui[name].response;
  if(report.ui[name]?.uncertain)throw new Error('UI_COMANDO_INCERTO_'+name);
  report.ui[name]={url,uncertain:true};await save();
  const reqPromise=page.waitForRequest(r=>new URL(r.url()).pathname===url&&r.method()==='POST');
  const resPromise=page.waitForResponse(r=>new URL(r.url()).pathname===url&&r.request().method()==='POST');
  await click();const req=await reqPromise;report.ui[name].body=req.postDataJSON();await save();
  const res=await resPromise;let data;try{data=await res.json();}catch{data=null;}
  report.ui[name]={url,body:req.postDataJSON(),status:res.status(),response:data,uncertain:false,done:res.status()===expected};await save();
  assert.equal(res.status(),expected,'UI_'+name+'_'+(data?.codigo??''));return data;
 }
 async function openTask(kind,reference){
  await page.getByRole('region',{name:'Tarefas do coletor'}).getByRole('button',{name:new RegExp('^'+kind)}).click();
  const list=page.getByRole('region',{name:'Lista de tarefas'});
  let article=list.locator('article').filter({hasText:reference}).first();
  for(let i=0;i<100;i++){
   if(await article.count())break;
   const next=list.getByRole('button',{name:'Próxima',exact:true});
   if(await next.count()&&await next.isEnabled()){await next.click();await page.getByText('Consultando tarefas…',{exact:true}).waitFor({state:'hidden'});}else break;
  }
  const response=page.waitForResponse(r=>/^\/api\/v1\/tarefas-coletor\/\d+$/.test(new URL(r.url()).pathname)&&r.request().method()==='GET');
  await article.getByRole('button',{name:'Ver tarefa',exact:true}).click();const detail=await(await response).json();
  return detail;
 }
 async function startTask(detail,name){
  if(detail.tarefa.minha)return detail;
  return mutate(name,taskUrl(detail.tarefa.id)+'/iniciar',()=>page.getByRole('dialog').getByRole('button',{name:'Iniciar tarefa',exact:true}).click());
 }
 async function closeTask(){await page.getByRole('button',{name:'Voltar às tarefas',exact:true}).click();await page.getByRole('dialog').waitFor({state:'hidden'});}
 try{
  await mkdir(outputs,{recursive:true});
  if(stage==='enderecar'){
   const prior=report.attempts.find(a=>a.stage==='preparar');
   check(prior.runId!==secret.runId&&prior.sourceHash===secret.sourceHash,'REINICIO_NORMAL_MESMA_FONTE_COM_NOVO_PROCESSO');
   const resumed=await operator.get(taskUrl(f.claimBeforeRestart.id));
   check(resumed.tarefa.minha&&resumed.tarefa.versao===f.claimBeforeRestart.versao&&resumed.tarefa.responsavel===operator.profile.id,'RESPONSAVEL_E_REVISAO_PERSISTEM_APOS_REINICIO');
   await login(operator);await collector();
   const manual=page.getByLabel('1. Leia o UUID da unidade',{exact:true});await manual.fill('LEITURA-MANUAL-PROFUNDA-PRESERVADA');
   for(let index=0;index<2;index++){
    const remaining=(await operator.get(`/api/v1/tarefas-coletor?tipo=ENDERECAMENTO&clienteId=64&armazemId=31&tamanho=100`)).itens.filter(t=>f.addressTasks.some(v=>v.id===t.id));
    if(!remaining.length)break;
    let detail=await openTask('Endereçar',f.product.sku);detail=await startTask(detail,'address-start-'+index);
    const u=f.units.find(v=>v.codigo===detail.tarefa.codigoUnidade);assert.ok(u);
    const destination=u.id===f.units[1].id?f.ARM2:f.ARM3;
    await page.setViewportSize({width:390,height:950});const dialog=page.getByRole('dialog');
    await dialog.getByLabel('1. Leia a etiqueta da mercadoria',{exact:true}).fill(op());
    for(const label of ['Peso (kg)','Altura (m)','Largura (m)','Profundidade (m)'])await dialog.getByLabel(label,{exact:true}).fill('1');
    await dialog.getByLabel('2. Leia a posição de destino',{exact:true}).fill(destination.codigo);
    await dialog.getByRole('button',{name:'Conferir destino',exact:true}).click();
    await dialog.getByText('Leia a etiqueta da mercadoria indicada nesta tarefa.',{exact:true}).waitFor();check(true,'UUID_ERRADO_BLOQUEADO_NA_TELA_'+index);
    await dialog.getByLabel('1. Leia a etiqueta da mercadoria',{exact:true}).fill(u.codigo);
    if(index===0&&!report.ui['address-occupied']){
     await dialog.getByLabel('2. Leia a posição de destino',{exact:true}).fill(f.ARM1.codigo);
     await dialog.getByRole('button',{name:'Conferir destino',exact:true}).click();
     await mutate('address-occupied',taskUrl(detail.tarefa.id)+'/enderecar',()=>dialog.getByRole('button',{name:'Confirmar endereçamento',exact:true}).click(),409);
     check(!(await operator.get(`/api/v1/unidades-logisticas/${u.codigo}/estoque`)).primeiroEnderecamentoEm,'DESTINO_OCUPADO_NAO_MOVE_MERCADORIA');
    }
    await dialog.getByLabel('2. Leia a posição de destino',{exact:true}).fill(destination.codigo);
    await dialog.getByRole('button',{name:'Conferir destino',exact:true}).click();
    if(index===0&&!report.ui['address-lost']?.done){
     const url=taskUrl(detail.tarefa.id)+'/enderecar';let intercepted=false,committed;
     await page.route('**'+url,async route=>{
      if(intercepted){await route.continue();return;}intercepted=true;
      const body=route.request().postDataJSON();const response=await route.fetch();assert.equal(response.status(),200);committed=await response.json();
      report.ui['address-lost']={url,body,response:committed,status:200,networkResponseLost:true};await save();await route.abort('failed');
     });
     await dialog.getByRole('button',{name:'Confirmar endereçamento',exact:true}).click();
     await dialog.getByRole('button',{name:'Conferir operação pendente',exact:true}).waitFor();
     check(await dialog.getByRole('button',{name:'Voltar às tarefas',exact:true}).isDisabled(),'RESPOSTA_PERDIDA_BLOQUEIA_SAIDA_ATE_CONFERIR');
     const receipt=page.waitForResponse(r=>new URL(r.url()).pathname===url&&r.request().method()==='POST');
     await dialog.getByRole('button',{name:'Conferir operação pendente',exact:true}).click();
     const response=await receipt;const replay=await response.json();check(response.status()===200&&JSON.stringify(response.request().postDataJSON())===JSON.stringify(report.ui['address-lost'].body)&&replay.unidade.unidade.versao===committed.unidade.unidade.versao,'RETRY_DA_TELA_REUSA_CHAVE_E_NAO_DUPLICA_MOVIMENTO');
     report.ui['address-lost'].done=true;await save();await page.unroute('**'+url);
    }else await mutate('address-finish-'+index,taskUrl(detail.tarefa.id)+'/enderecar',()=>dialog.getByRole('button',{name:'Confirmar endereçamento',exact:true}).click());
    await dialog.getByText('A tarefa foi encerrada. Os registros da operação foram preservados.',{exact:true}).waitFor();
    check(true,'ENDERECAMENTO_REAL_CONCLUIDO_PELA_TELA_'+index);
    await closeTask();check(await manual.inputValue()==='LEITURA-MANUAL-PROFUNDA-PRESERVADA','FLUXO_MANUAL_PRESERVADO_APOS_TAREFA_'+index);
   }
   const count=await totals();check(count.enderecamento===report.baseline.enderecamento,'CONTADOR_ENDERECAMENTO_RETORNA_A_BASE');
   f.stockAfterAddress=await Promise.all(f.units.map(u=>operator.get(`/api/v1/unidades-logisticas/${u.codigo}/estoque`)));
   check(f.stockAfterAddress.every(s=>s.primeiroEnderecamentoEm)&&f.stockAfterAddress.reduce((n,s)=>n+Number(s.unidade.quantidade),0)===20,'QUATRO_UNIDADES_ENDERECADAS_COM_VINTE_PRODUTOS');
   await page.screenshot({path:path.join(outputs,'profundo-enderecamento-mobile.png'),fullPage:true});report.state='ENDERECAMENTO_APROVADO';await save();
  }else if(stage==='inventario'){
   f.inventory=await manager.post('inventory-sku','/api/v1/inventarios',{operacaoId:op(),clienteId:64,armazemId:31,tipo:'SKU',produtoId:f.product.id,motivo:reason});
   f.inventoryTask??=(await manager.get('/api/v1/tarefas-coletor?tipo=INVENTARIO&clienteId=64&armazemId=31&tamanho=100')).itens.find(t=>t.origemId===f.inventory.id);assert.ok(f.inventoryTask);await save();
   const inventoryDetail=await operator.get('/api/v1/inventarios/'+f.inventory.id);
   if(!f.inventoryReport){
    check(!JSON.stringify(inventoryDetail).includes('esperado'),'INVENTARIO_ABERTO_NAO_REVELA_BASE_POR_API');
    await login(operator);await collector();
    let detail=await openTask('Inventário','Inventário '+f.inventory.id);await startTask(detail,'inventory-start');
    await page.getByRole('button',{name:'Contar mercadorias',exact:true}).click();await page.setViewportSize({width:390,height:950});
    let dialog=page.getByRole('dialog');
    check(await dialog.getByText('Quantidade esperada',{exact:true}).count()===0,'TELA_OPERACAO_MANTEM_CONTAGEM_AS_CEGAS');
    async function fillReading(u,quantity){
     await dialog.getByLabel('Etiqueta da mercadoria',{exact:true}).fill(u.codigo);
     await dialog.getByLabel('Quantidade contada',{exact:true}).fill(String(quantity));
     const stock=await operator.get(`/api/v1/unidades-logisticas/${u.codigo}/estoque`);
     const select=dialog.getByRole('combobox',{name:/Endereço/});
     const addressId=String(stock.posicoes[0].enderecoId);
     // Não injeta opções: a posição tem que estar disponível no seletor real.
     if(await select.locator(`option[value="${addressId}"]`).count()===0){
      const next=dialog.getByRole('button',{name:'Mais opções',exact:true});
      for(let attempt=0;attempt<60&&await next.count()&&await next.isEnabled();attempt++){await next.click();if(await select.locator(`option[value="${addressId}"]`).count())break;}
     }
     if(await select.locator(`option[value="${addressId}"]`).count()===0)throw new Error('POSICAO_DO_ARMAZEM_NAO_DISPONIVEL_NO_INVENTARIO_DO_COLETOR');
     await select.selectOption(addressId);
    }
    for(const [index,u]of f.units.entries()){
     if(report.ui['inventory-read-'+index]?.done)continue;
     await fillReading(u,index===0?3:5);
     const result=await mutate('inventory-read-'+index,taskUrl(f.inventoryTask.id)+'/inventario/leituras',()=>dialog.getByRole('button',{name:'Registrar leitura',exact:true}).click());
     check(!JSON.stringify(result).includes('esperado'),'LEITURA_REAL_CEGA_'+index);
     await dialog.getByText(/Leitura registrada:/).waitFor();
     if(index===0){
      await fillReading(u,4);
      await mutate('inventory-duplicate',taskUrl(f.inventoryTask.id)+'/inventario/leituras',()=>dialog.getByRole('button',{name:'Registrar leitura',exact:true}).click(),409);
      check(true,'LEITURA_DUPLICADA_EXIGE_RECONTAGEM_EXPLICITA');
      await dialog.getByRole('checkbox',{name:/Recontar: substituir/}).check();
      const replacement=await mutate('inventory-recount',taskUrl(f.inventoryTask.id)+'/inventario/leituras',()=>dialog.getByRole('button',{name:'Registrar leitura',exact:true}).click());
      check(Number(replacement.leitura.contado)===4&&Number(replacement.inventario.unidadesLidas)===1,'RECONTAGEM_SUBSTITUI_SEM_DUPLICAR_MERCADORIA');
     }
    }
    await page.screenshot({path:path.join(outputs,'profundo-inventario-cego-mobile.png'),fullPage:true});
    const current=(await operator.get('/api/v1/inventarios/'+f.inventory.id)).inventario;
    await operator.request('POST',taskUrl(f.inventoryTask.id)+'/inventario/finalizar',{operacaoId:op(),versao:current.versao,confirmarNaoLidas:false,motivo:reason},400);check(true,'FINALIZACAO_EXIGE_CONFIRMACAO_EXPLICITA');
    await dialog.getByLabel('Observação de encerramento',{exact:true}).fill(reason);
    await dialog.getByRole('checkbox',{name:/Concluí a conferência/}).check();
    f.inventoryReport=await mutate('inventory-finish',taskUrl(f.inventoryTask.id)+'/inventario/finalizar',()=>dialog.getByRole('button',{name:'Encerrar e gerar relatório',exact:true}).click());
    check(f.inventoryReport.divergenciasQuantidade===1&&f.inventoryReport.divergenciasEndereco===0,'DIVERGENCIA_REGISTRADA_SEM_AJUSTAR_ESTOQUE');
    await save();await page.close();page=null;
   }
   const saldo=await operator.get(`/api/v1/estoque/saldo?clienteId=64&armazemId=31&produtoId=${f.product.id}`);
   check(Number(saldo.fisicoTotal)===20,'INVENTARIO_DIVERGENTE_NAO_ALTERA_ESTOQUE_FISICO');
   f.cancelInventory=await manager.post('inventory-cancel','/api/v1/inventarios',{operacaoId:op(),clienteId:64,armazemId:31,tipo:'CEGAS',motivo:reason});
   f.cancelInventoryTask=(await manager.get('/api/v1/tarefas-coletor?tipo=INVENTARIO&clienteId=64&armazemId=31&tamanho=100')).itens.find(t=>t.origemId===f.cancelInventory.id);assert.ok(f.cancelInventoryTask);
   const cancelTask=(await operator.get(taskUrl(f.cancelInventoryTask.id))).tarefa;
   await operator.post('cancel-inventory-operator-claim',taskUrl(cancelTask.id)+'/iniciar',{operacaoId:op(),versao:cancelTask.versao,motivo:reason});
   await operator.request('POST',taskUrl(f.cancelInventoryTask.id)+'/inventario/cancelar',{operacaoId:op(),versao:f.cancelInventory.versao,confirmarNaoLidas:false,motivo:reason},403);check(true,'OPERACAO_NAO_CANCELA_INVENTARIO');
   const releaseTask=(await operator.get(taskUrl(cancelTask.id))).tarefa;
   await operator.post('cancel-inventory-operator-release',taskUrl(cancelTask.id)+'/liberar',{operacaoId:op(),versao:releaseTask.versao,motivo:reason});
   await login(supervisor);await collector();
   const detail=await openTask('Inventário','Inventário '+f.cancelInventory.id);await startTask(detail,'cancel-inventory-start');
   await page.getByRole('button',{name:'Contar mercadorias',exact:true}).click();
   const dialog=page.getByRole('dialog');await dialog.getByText('Cancelar este inventário',{exact:true}).click();
   await dialog.getByLabel('Motivo do cancelamento',{exact:true}).fill(reason);
   const canceled=await mutate('inventory-cancel-ui',taskUrl(f.cancelInventoryTask.id)+'/inventario/cancelar',()=>dialog.getByRole('button',{name:'Confirmar cancelamento',exact:true}).click());
   check(canceled.situacao==='CANCELADO','SUPERVISOR_CANCELA_INVENTARIO_PELA_TAREFA');
   check((await totals()).inventario===report.baseline.inventario,'CONTADOR_INVENTARIO_RETORNA_A_BASE');report.state='INVENTARIO_APROVADO';await save();
  }else if(stage==='separar'){
   await login(manager);await navigation('Saída, FIFO e reserva');
   async function openOrder(order,step='1. Pedido integral'){
    await navigation('Saída, FIFO e reserva');await page.getByRole('button',{name:step,exact:true}).click();
    const search=page.getByRole('textbox',{name:'Buscar nos registros desta página',exact:true});
    if(await search.count())await search.fill('');
    let row=page.locator('.record-table tbody tr').filter({hasText:order.referencia}).first();
    for(let n=0;n<100&&!(await row.count());n++){
     const next=page.getByRole('button',{name:'Próxima',exact:true});
     if(!(await next.count())||!(await next.isEnabled()))break;
     const response=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/pedidos-saida'&&r.request().method()==='GET');await next.click();await response;
    }
    await row.getByRole('button',{name:'Ver detalhes',exact:true}).last().click();
    await page.getByRole('dialog').getByRole('region',{name:'Separação no Coletor'}).waitFor();
   }
   async function closeRecord(){await page.getByRole('dialog').getByRole('button',{name:/^(Fechar detalhes|Voltar à lista)$/}).last().click();await page.getByRole('dialog').waitFor({state:'hidden'});}
   async function choose(label){const dialog=page.getByRole('dialog');const button=dialog.getByRole('button',{name:label,exact:true});if(!(await button.isVisible()))await dialog.getByText('Outras operações',{exact:true}).click();await button.click();}
   async function submit(name,url){const dialog=page.getByRole('dialog');await dialog.getByLabel('Motivo / justificativa *',{exact:true}).fill(reason);await dialog.getByRole('button',{name:'Conferir e confirmar',exact:true}).click();return mutate(name,url,()=>dialog.getByRole('button',{name:'Confirmar agora',exact:true}).click());}
   if(!f.order&&report.ui['output-create']?.done)f.order=report.ui['output-create'].response.pedido;
   if(!f.order){
    await page.getByRole('button',{name:'Nova saída',exact:true}).click();const dialog=page.getByRole('dialog');
    await dialog.getByRole('button',{name:'Adicionar item',exact:true}).click();
    const select=dialog.getByRole('combobox',{name:'Produto / SKU *',exact:true});
    await select.locator('option').nth(1).waitFor({state:'attached'});
    for(let n=0;n<100&&!(await select.locator(`option[value="${f.product.id}"]`).count());n++){
     const next=dialog.getByRole('button',{name:'Mais opções',exact:true});if(!(await next.count())||!(await next.isEnabled()))break;
     const response=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/produtos'&&r.request().method()==='GET');await next.click();await response;await select.locator('option').nth(1).waitFor({state:'attached'});
    }
    await select.selectOption(String(f.product.id));await dialog.getByLabel('Quantidade *',{exact:true}).fill('8');
    await dialog.getByLabel('Justificativa do pedido *',{exact:true}).fill(reason);
    await dialog.getByRole('button',{name:'Conferir e confirmar',exact:true}).click();
    f.order=(await mutate('output-create','/api/v1/pedidos-saida',()=>dialog.getByRole('button',{name:'Confirmar agora',exact:true}).click(),201)).pedido;await save();await closeRecord();
   }
   f.outputTask??=await manager.get('/api/v1/tarefas-coletor/pedido-saida/'+f.order.id);await save();
   if(!report.ui['output-reserve']?.done){
    check((await totals()).separacao===report.baseline.separacao+1,'RASCUNHO_CRIADO_PELA_TELA_GERA_UMA_TAREFA');
    await collector();await openTask('Separação',f.outputTask.referencia);
    check(await page.getByRole('dialog').getByRole('button',{name:'Iniciar tarefa',exact:true}).count()===0,'RASCUNHO_NAO_LIBERA_INICIO_NA_TELA');
    await operator.request('POST',taskUrl(f.outputTask.id)+'/iniciar',{operacaoId:op(),versao:f.outputTask.versao,motivo:reason},409);check(true,'BACKEND_TAMBEM_RECUSA_INICIO_SEM_RESERVA');await closeTask();
    await openOrder(f.order,'2. Sugestão FIFO e reserva');await choose('Consultar sugestão FIFO');
    const fifoResponse=page.waitForResponse(r=>new URL(r.url()).pathname===`/api/v1/pedidos-saida/${f.order.id}/fifo`);await page.getByRole('dialog').getByRole('button',{name:'Consultar',exact:true}).click();
    f.fifo=await(await fifoResponse).json();check(f.fifo.selecoes.length===2&&f.fifo.selecoes.reduce((n,s)=>n+Number(s.quantidade),0)===8,'FIFO_REAL_DISTRIBUI_OITO_EM_DUAS_MERCADORIAS');await save();
    await page.getByRole('dialog').getByRole('button',{name:'Confirmar reserva da sugestão do servidor',exact:true}).click();
    f.reservedOrder=(await submit('output-reserve',`/api/v1/pedidos-saida/${f.order.id}/reserva`)).pedido;await save();
    const exact=page.waitForResponse(r=>new URL(r.url()).pathname===taskUrl(f.outputTask.id)&&r.request().method()==='GET');
    await page.getByRole('dialog').getByRole('button',{name:'Abrir tarefa no Coletor',exact:true}).click();
    const linked=await(await exact).json();check(linked.tarefa.origemId===f.order.id,'RESERVA_NA_SAIDA_ABRE_TAREFA_EXATA');await closeTask();
   }
   await page.close();page=null;await login(operator);await collector();
   for(let index=0;index<2;index++){
    if(report.ui['output-separate-'+index]?.done)continue;
    let detail=await openTask('Separação',f.outputTask.referencia);detail=await startTask(detail,'output-start');
    const done=new Set(detail.separacoes.filter(s=>s.situacao==='SEPARADA').map(s=>s.reservaId));
    const reservation=detail.pedido.reservas.find(r=>r.situacao==='ATIVA'&&!done.has(r.id));assert.ok(reservation);
    await page.setViewportSize({width:390,height:950});const dialog=page.getByRole('dialog');
    await dialog.getByRole('combobox',{name:'Mercadoria a separar',exact:true}).selectOption(String(reservation.id));
    await dialog.getByLabel('1. Leia a etiqueta da mercadoria',{exact:true}).fill(reservation.codigoUnidade);
    const unit=(await operator.get(taskUrl(f.outputTask.id)+'/unidade?codigo='+reservation.codigoUnidade)).estoque;
    check(Number(unit.medidas.posicoesNecessarias)===1,'FIFO_MERCADORIA_DE_UMA_POSICAO_'+index);
    await dialog.getByLabel('2. Leia a posição de destino',{exact:true}).fill(index===0?f.SEP1.codigo:f.SEP2.codigo);
    await dialog.getByRole('button',{name:'Conferir destino',exact:true}).click();
    const result=await mutate('output-separate-'+index,taskUrl(f.outputTask.id)+'/separar',()=>dialog.getByRole('button',{name:'Confirmar separação',exact:true}).click());
    if(index===0){
     check(result.pedido.situacao==='EM_SEPARACAO'&&result.tarefa.situacao==='EM_ANDAMENTO','PRIMEIRA_MERCADORIA_NAO_ENCERRA_PEDIDO_NEM_TAREFA');
     check((await totals()).separacao===report.baseline.separacao+1,'CONTADOR_MANTEM_PEDIDO_PARCIALMENTE_SEPARADO');
     await dialog.getByRole('combobox',{name:'Mercadoria a separar',exact:true}).locator('option').waitFor({state:'attached'});
     check(await dialog.getByRole('combobox',{name:'Mercadoria a separar',exact:true}).locator('option').count()===1,'TELA_AVANCA_PARA_UNICA_RESERVA_RESTANTE');
     await closeTask();await openOrder(f.order);const panel=page.getByRole('region',{name:'Separação no Coletor'});await panel.getByText('Em separação',{exact:true}).waitFor();
     const response=page.waitForResponse(r=>new URL(r.url()).pathname===taskUrl(f.outputTask.id)&&r.request().method()==='GET');await panel.getByRole('button',{name:'Acompanhar no Coletor',exact:true}).click();
     const resumed=await(await response).json();check(resumed.tarefa.minha&&resumed.separacoes.filter(s=>s.situacao==='SEPARADA').length===1,'ATALHO_SAIDA_RETOMA_SEPARACAO_PARCIAL_DO_OPERADOR');await closeTask();
    }else{
     check(result.pedido.situacao==='SEPARADO'&&result.tarefa.situacao==='CONCLUIDA','SEGUNDA_MERCADORIA_CONCLUI_PEDIDO_E_TAREFA');
     const replay=(await operator.request('POST',taskUrl(f.outputTask.id)+'/separar',report.ui['output-separate-1'].body)).body;
     check(replay.pedido.versao===result.pedido.versao,'REPLAY_SEPARACAO_NAO_DUPLICA_EFEITO');
     await page.screenshot({path:path.join(outputs,'profundo-separacao-concluida-mobile.png'),fullPage:true});await closeTask();
    }
   }
   await page.close();page=null;await login(supervisor);
   f.reverseOrder??=(await manager.post('reverse-output-create','/api/v1/pedidos-saida',{operacaoId:op(),clienteId:64,armazemId:31,referencia:report.prefix+'-REV',itens:[{produtoId:f.product.id,quantidade:1}],motivo:reason},201)).pedido;
   f.reverseTask??=await manager.get('/api/v1/tarefas-coletor/pedido-saida/'+f.reverseOrder.id);await save();
   f.reverseReserved??=(await manager.post('reverse-output-reserve',`/api/v1/pedidos-saida/${f.reverseOrder.id}/reserva`,{operacaoId:op(),versao:f.reverseOrder.versao,motivo:reason})).pedido;await save();
   if(!report.ui['output-reverse']?.done){await openOrder(f.reverseOrder,'3. Reversão e cancelamento');await choose('Reverter reserva');await submit('output-reverse',`/api/v1/pedidos-saida/${f.reverseOrder.id}/reversao-reserva`);await closeRecord();}
   const reversed=(await manager.get(taskUrl(f.reverseTask.id))).tarefa;check(reversed.situacao==='AGUARDANDO_RESERVA','REVERSAO_RETORNA_TAREFA_PARA_AGUARDAR_RESERVA');
   if(!report.ui['output-cancel']?.done){await openOrder(f.reverseOrder,'3. Reversão e cancelamento');await choose('Registrar cancelamento');await submit('output-cancel',`/api/v1/pedidos-saida/${f.reverseOrder.id}/cancelamento`);await closeRecord();}
   check((await manager.get(taskUrl(f.reverseTask.id))).tarefa.situacao==='CANCELADA','CANCELAMENTO_NA_ETAPA_TRES_ENCERRA_TAREFA');
   check((await totals()).separacao===report.baseline.separacao,'CONTADOR_SEPARACAO_RETORNA_A_BASE');
   const saldo=await operator.get(`/api/v1/estoque/saldo?clienteId=64&armazemId=31&produtoId=${f.product.id}`);check(Number(saldo.fisicoTotal)===20,'SEPARACAO_E_REVERSAO_NAO_REALIZAM_BAIXA_FISICA');
   check(JSON.stringify(await operator.get(`/api/v1/inventarios/${f.inventory.id}/relatorio`))===JSON.stringify(f.inventoryReport),'RELATORIO_FECHADO_IMUTAVEL_APOS_SEPARACAO');
   report.state='SEPARACAO_APROVADA';await save();
  }else if(stage==='posicao'){
   f.positionInventory=await manager.post('inventory-position','/api/v1/inventarios',{operacaoId:op(),clienteId:64,armazemId:31,tipo:'POSICAO',enderecoId:f.PAR1.id,motivo:reason});
   f.positionTask??=(await manager.get('/api/v1/tarefas-coletor?tipo=INVENTARIO&clienteId=64&armazemId=31&tamanho=100')).itens.find(t=>t.origemId===f.positionInventory.id);assert.ok(f.positionTask);await save();
   await login(operator);await collector();let detail=await openTask('Inventário','Inventário '+f.positionInventory.id);await startTask(detail,'position-start');
   await page.getByRole('button',{name:'Contar mercadorias',exact:true}).click();await page.setViewportSize({width:390,height:950});const dialog=page.getByRole('dialog');
   if(!report.ui['position-read']?.done){await dialog.getByLabel('Etiqueta da mercadoria',{exact:true}).fill(f.units[3].codigo);await dialog.getByLabel('Quantidade contada',{exact:true}).fill('5');await mutate('position-read',taskUrl(f.positionTask.id)+'/inventario/leituras',()=>dialog.getByRole('button',{name:'Registrar leitura',exact:true}).click());}
   await dialog.getByRole('region',{name:'Últimas leituras'}).locator('tbody tr').waitFor();
   report.inventoryDialogGeometry=await dialog.evaluate(d=>({client:d.clientWidth,scroll:d.scrollWidth,body:d.querySelector('.dialog-body').clientWidth,content:d.querySelector('.inventory-dialog-content').getBoundingClientRect().width}));await save();
   check(report.inventoryDialogGeometry.scroll<=report.inventoryDialogGeometry.client+1,'DIALOGO_INVENTARIO_COM_LEITURAS_CABE_NO_COLETOR');
   await dialog.evaluate(d=>{d.scrollTop=0;d.scrollLeft=0;});await page.screenshot({path:path.join(outputs,'profundo-inventario-posicao-mobile.png'),fullPage:true});
   await dialog.getByLabel('Observação de encerramento',{exact:true}).fill(reason);await dialog.getByRole('checkbox',{name:/Concluí a conferência/}).check();
   f.positionInventoryReport=await mutate('position-finish',taskUrl(f.positionTask.id)+'/inventario/finalizar',()=>dialog.getByRole('button',{name:'Encerrar e gerar relatório',exact:true}).click());
   check(f.positionInventoryReport.mercadoriasConferidas===1&&f.positionInventoryReport.divergenciasQuantidade===0&&f.positionInventoryReport.divergenciasEndereco===0,'INVENTARIO_POR_POSICAO_CONFERE_UNIDADE_QUE_OCUPA_DUAS');
   check((await totals()).inventario===0,'FILA_INVENTARIO_ZERADA_APOS_MODO_POSICAO');report.state='POSICAO_APROVADA';await save();
  }else if(stage==='mobile'){
   f.mobileInventory=await manager.post('inventory-mobile','/api/v1/inventarios',{operacaoId:op(),clienteId:64,armazemId:31,tipo:'SKU',produtoId:f.product.id,motivo:reason+' revisão visual móvel'});
   f.mobileTask??=(await manager.get('/api/v1/tarefas-coletor?tipo=INVENTARIO&clienteId=64&armazemId=31&tamanho=100')).itens.find(t=>t.origemId===f.mobileInventory.id);assert.ok(f.mobileTask);await save();
   await login(operator);await collector();const detail=await openTask('Inventário','Inventário '+f.mobileInventory.id);await startTask(detail,'mobile-start');await page.getByRole('button',{name:'Contar mercadorias',exact:true}).click();const dialog=page.getByRole('dialog');
   for(const [index,u]of f.units.entries()){
    if(report.ui['mobile-read-'+index]?.done)continue;
    await dialog.getByLabel('Etiqueta da mercadoria',{exact:true}).fill(u.codigo);await dialog.getByLabel('Quantidade contada',{exact:true}).fill('5');
    const stock=await operator.get(`/api/v1/unidades-logisticas/${u.codigo}/estoque`),addressId=String(stock.posicoes[0].enderecoId),select=dialog.getByRole('combobox',{name:/Endereço/});
    await select.locator('option').nth(1).waitFor({state:'attached'});
    for(let n=0;n<60&&!(await select.locator(`option[value="${addressId}"]`).count());n++){const next=dialog.getByRole('button',{name:'Mais opções',exact:true});if(!(await next.count())||!(await next.isEnabled()))break;const response=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/enderecos');await next.click();await response;await select.locator('option').nth(1).waitFor({state:'attached'});}
    await select.selectOption(addressId);await mutate('mobile-read-'+index,taskUrl(f.mobileTask.id)+'/inventario/leituras',()=>dialog.getByRole('button',{name:'Registrar leitura',exact:true}).click());
   }
   await dialog.getByRole('region',{name:'Últimas leituras'}).locator('tbody tr').first().waitFor();report.mobileFormLayouts=[];
   for(const theme of ['light','dark'])for(const width of [320,360,390,768]){
    await page.setViewportSize({width,height:950});await page.evaluate(t=>document.documentElement.setAttribute('data-og-theme',t),theme);await dialog.evaluate(d=>{d.scrollTop=0;d.scrollLeft=0;});
    const geometry=await dialog.evaluate(d=>{const rect=d.getBoundingClientRect(),controls=[...d.querySelectorAll('input,select,button,label')].filter(e=>e.getClientRects().length&&!e.closest('.inventory-table-scroll'));return{client:d.clientWidth,scroll:d.scrollWidth,titleWidth:d.querySelector('h2').getBoundingClientRect().width,titleHeight:d.querySelector('h2').getBoundingClientRect().height,close:d.querySelector('.dialog-close').getBoundingClientRect().width,outside:controls.filter(e=>e.getBoundingClientRect().left<rect.left-1||e.getBoundingClientRect().right>rect.right+1).map(e=>e.tagName)}});
    check(geometry.scroll<=geometry.client+1&&geometry.titleWidth>=80&&geometry.titleHeight<150&&geometry.close<=60&&geometry.outside.length===0,'FORMULARIO_INVENTARIO_SEM_CORTE_'+width+'_'+theme);report.mobileFormLayouts.push({width,theme,...geometry});await save();
    if(theme==='light'&&width===390){await page.screenshot({path:path.join(outputs,'profundo-inventario-formulario-mobile.png')});await dialog.getByLabel('Etiqueta da mercadoria',{exact:true}).scrollIntoViewIfNeeded();await page.screenshot({path:path.join(outputs,'profundo-inventario-leituras-mobile.png')});}
   }
   await page.evaluate(()=>document.documentElement.setAttribute('data-og-theme','light'));await page.setViewportSize({width:390,height:950});await dialog.getByLabel('Observação de encerramento',{exact:true}).fill(reason);await dialog.getByRole('checkbox',{name:/Concluí a conferência/}).check();
   f.mobileInventoryReport=await mutate('mobile-finish',taskUrl(f.mobileTask.id)+'/inventario/finalizar',()=>dialog.getByRole('button',{name:'Encerrar e gerar relatório',exact:true}).click());
   check(f.mobileInventoryReport.mercadoriasConferidas===4&&f.mobileInventoryReport.divergenciasQuantidade===0&&f.mobileInventoryReport.divergenciasEndereco===0,'CONTAGEM_MOVEL_FINAL_VINTE_PRODUTOS_SEM_DIVERGENCIA');
   check((await totals()).inventario===0,'CONTADOR_ZERO_APOS_REGRESSAO_VISUAL_MOVEL');report.state='MOBILE_APROVADO';await save();
  }else if(stage==='final'){
   await login(operator);report.layouts=[];
   for(const screen of ['Saída, FIFO e reserva','Coletor']){
    await navigation(screen);
    if(screen==='Saída, FIFO e reserva'){
     await page.locator('.record-table tbody tr').filter({hasText:f.order.referencia}).first().waitFor();
     const nav=page.getByRole('navigation',{name:'Etapas de Saída, FIFO e reserva'});
     check(await nav.getByRole('button').count()===3&&await page.getByRole('button',{name:'3. Leitura e separação',exact:true}).count()===0,'SAIDA_TRES_ETAPAS_SEM_SEPARACAO_OPERACIONAL');
    }else{await page.getByRole('heading',{name:'Tarefas disponíveis',exact:true}).waitFor();await page.locator('.collector-queue-count').filter({hasText:/^0$/}).first().waitFor();check(await page.locator('.collector-queue-count').filter({hasText:/^0$/}).count()===3,'TRES_CARDS_ZERADOS_NA_TELA_FINAL');}
    for(const theme of ['light','dark'])for(const width of [390,768,1024,1640]){
     await page.setViewportSize({width,height:950});await page.evaluate(t=>document.documentElement.setAttribute('data-og-theme',t),theme);
     await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));await Promise.all(document.getAnimations().filter(a=>Number.isFinite(a.effect?.getComputedTiming().endTime)).map(a=>a.finished.catch(()=>{})));});
     const size=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));
     const key=(screen==='Coletor'?'coletor':'saida')+'-'+width+'-'+theme;
     check(size.scroll<=size.width,'SEM_TRANSBORDAMENTO_'+key);report.layouts.push({screen,theme,...size});
     if(theme==='light'&&[390,1640].includes(width))await page.screenshot({path:path.join(outputs,'profundo-'+key+'.png'),fullPage:true});
    }
   await page.evaluate(()=>document.documentElement.setAttribute('data-og-theme','light'));
   }
   await navigation('Saída, FIFO e reserva');await page.locator('.record-table tbody tr').filter({hasText:f.order.referencia}).first().getByRole('button',{name:'Ver detalhes',exact:true}).last().click();
   const panel=page.getByRole('region',{name:'Separação no Coletor'});await panel.getByRole('button',{name:'Acompanhar no Coletor',exact:true}).click();
   const taskDialog=page.getByRole('dialog');await taskDialog.getByText('A tarefa foi encerrada. Os registros da operação foram preservados.',{exact:true}).waitFor();await page.setViewportSize({width:390,height:950});
   report.taskDialogGeometry=await taskDialog.evaluate(d=>({client:d.clientWidth,scroll:d.scrollWidth,close:d.querySelector('.dialog-close').getBoundingClientRect().width,titleHeight:d.querySelector('h2').getBoundingClientRect().height}));await save();
   check(report.taskDialogGeometry.scroll<=report.taskDialogGeometry.client+1&&report.taskDialogGeometry.close<=60&&report.taskDialogGeometry.titleHeight<120,'CABECALHO_DA_TAREFA_LEGIVEL_NO_COLETOR');
   await page.screenshot({path:path.join(outputs,'profundo-separacao-concluida-mobile.png'),fullPage:true});await closeTask();
   await navigation('Inventário');await page.getByRole('button',{name:'Inventários realizados',exact:true}).click();
   const history=page.getByRole('table',{name:'Inventários realizados',exact:true});const row=history.getByRole('row').filter({has:page.getByText('#'+f.inventory.id,{exact:true})});
   await row.getByRole('button').click();await page.getByRole('dialog').getByRole('heading',{name:'Resultado da conferência',exact:true}).waitFor();
   await page.setViewportSize({width:390,height:950});const inventoryDialog=page.getByRole('dialog');
   report.inventoryReportGeometry=await inventoryDialog.evaluate(d=>({client:d.clientWidth,scroll:d.scrollWidth}));await save();
   check(report.inventoryReportGeometry.scroll<=report.inventoryReportGeometry.client+1,'RELATORIO_DO_INVENTARIO_CABE_NO_COLETOR_COM_TABELAS_ROLAVEIS');
   await inventoryDialog.evaluate(d=>{d.scrollTop=0;d.scrollLeft=0;});await page.screenshot({path:path.join(outputs,'profundo-inventario-relatorio-mobile.png'),fullPage:true});
   report.state='LAYOUTS_APROVADOS';await save();
  }else throw new Error('ETAPA_BROWSER_AINDA_NAO_IMPLEMENTADA');
  check(report.browserErrors.length===0,'SEM_ERRO_JAVASCRIPT_'+stage);await save();
 }catch(e){
  if(page&&new URL(page.url()).pathname!=='/login'){
   report.uiFailure={stage,labels:await page.locator('label').allTextContents().catch(()=>[])};
   await page.screenshot({path:path.join(outputs,'profundo-falha-'+stage+'.png'),fullPage:true}).catch(()=>{});
  }
  throw e;
 }finally{await browser.close();}
}
