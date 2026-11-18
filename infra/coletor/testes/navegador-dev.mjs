import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import path from 'node:path';
const require=createRequire(import.meta.url);
const {chromium}=require('C:/Users/suporte/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
const area=process.argv[2],outputs='C:/Users/suporte/Documents/Codex/2026-10-28/wms-farol-supervisor/outputs';
let input='';for await(const c of process.stdin)input+=c;const secret=JSON.parse(input);input='';
const flow=JSON.parse(await readFile(path.join(area,'fluxos.json'),'utf8'));
const proof={id:'COLETOR-TAREFAS01',started:new Date().toISOString(),sourceHash:secret.sourceHash,jarSha256:secret.jarSha256,checks:[],layouts:[],errors:[],state:'RUNNING'};
const root=path.resolve(import.meta.dirname,'../../..');
const sourcePaths=['frontend/src/components/Collector.tsx','frontend/src/auth/AuthApp.tsx','frontend/src/modules/coletor/CollectorTasks.tsx','frontend/src/modules/coletor/CollectorTaskDialog.tsx','frontend/src/modules/coletor/CollectorTaskExecution.tsx','frontend/src/modules/regularizacao/InventoryDialog.tsx','frontend/src/modules/regularizacao/inventoryApi.ts','frontend/src/styles/collector.css','frontend/src/contracts/endpoints.json','frontend/src/contracts/schemas.json'];
async function fingerprints(){return Object.fromEntries(await Promise.all(sourcePaths.map(async p=>[p,createHash('sha256').update(await readFile(path.join(root,p))).digest('hex')])));}
proof.frontendFiles=await fingerprints();
const check=(ok,code)=>{assert.ok(ok,code);proof.checks.push(code);};
const browser=await chromium.launch({channel:'msedge',headless:true});
try{
 const page=await browser.newPage({viewport:{width:1640,height:950}});page.setDefaultTimeout(30000);
 page.on('pageerror',e=>proof.errors.push(e.name));
 await page.goto(secret.base,{waitUntil:'domcontentloaded'});
 await page.getByLabel('E-mail',{exact:true}).fill(secret.email);await page.getByLabel('Senha',{exact:true}).fill(secret.senha);secret.senha='';
 await page.getByRole('button',{name:'Entrar',exact:true}).click();
 await page.getByRole('button',{name:'Coletor',exact:true}).click();
 await page.getByRole('heading',{name:'Tarefas disponíveis',exact:true}).waitFor();
 await page.locator('.collector-queue-count').filter({hasText:/^[0-9]+$/}).first().waitFor();
 await mkdir(outputs,{recursive:true});
 const zero=flow.state==='APROVADO';
 for(const theme of ['light','dark'])for(const width of [390,768,1024,1640]){
  await page.setViewportSize({width,height:950});await page.evaluate(t=>document.documentElement.setAttribute('data-og-theme',t),theme);
  // Aguarda as transições naturais sem alterar CSS ou política de segurança da aplicação.
  await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));await Promise.all(document.getAnimations().filter(a=>Number.isFinite(a.effect?.getComputedTiming().endTime)).map(a=>a.finished.catch(()=>{})));await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));});
  const size=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth,cards:[...document.querySelectorAll('.collector-queue-card')].map(e=>{const r=e.getBoundingClientRect();return {height:r.height,width:r.width,x:r.x,y:r.y};})}));
  check(size.scroll<=size.width,'SEM_TRANSBORDAMENTO_'+width+'_'+theme);check(size.cards.length===3&&size.cards.every(c=>c.height>=44&&c.width>=44),'ALVOS_TOQUE_'+width+'_'+theme);
  check(size.cards.every(c=>c.height<200)&&(width<=800?size.cards.every(c=>Math.abs(c.x-size.cards[0].x)<1&&c.width>width*.8):size.cards.every(c=>Math.abs(c.y-size.cards[0].y)<1)),'DISPOSICAO_RESPONSIVA_'+width+'_'+theme);
  proof.layouts.push({theme,...size});
  await page.screenshot({path:path.join(area,`tela-${zero?'zero':'pendente'}-${width}-${theme}.png`),fullPage:true});
  if(theme==='light'&&(width===390||width===1640))await page.screenshot({path:path.join(outputs,`coletor-implementado-${zero?'zero-':''}${width===390?'coletor':'desktop'}.png`),fullPage:true});
 }
 await page.evaluate(()=>document.documentElement.setAttribute('data-og-theme','light'));
 await page.setViewportSize({width:390,height:950});
 if(!zero){
  const manual=page.getByLabel('1. Leia o UUID da unidade',{exact:true});await manual.fill('LEITURA-MANUAL-PRESERVADA');
  await page.getByRole('button',{name:/^Inventário/}).click();await page.getByRole('button',{name:'Ver tarefa',exact:true}).click();
  await page.getByRole('button',{name:'Contar mercadorias',exact:true}).click();
  await page.getByLabel('Etiqueta da mercadoria',{exact:true}).fill(flow.claimBeforeRestart.codigoUnidade);
  await page.getByLabel('Quantidade contada',{exact:true}).fill('5');await page.getByRole('checkbox',{name:'Mercadoria sem endereço',exact:true}).check();
  const response=page.waitForResponse(r=>r.url().includes('/tarefas-coletor/'+flow.fixtures.inventoryTask.id+'/inventario/leituras')&&r.request().method()==='POST');
  await page.getByRole('button',{name:'Registrar leitura',exact:true}).click();check((await response).status()===200,'LEITURA_INVENTARIO_BROWSER_SQL_REAL');
  await page.getByText(/Leitura registrada:/).waitFor();
  check(await page.getByText('Quantidade esperada',{exact:true}).count()===0,'TELA_CONTAGEM_CEGA_SEM_ESPERADO');
  await page.screenshot({path:path.join(outputs,'coletor-inventario-leitura-validada.png'),fullPage:true});
  await page.keyboard.press('Escape');await page.getByRole('button',{name:'Voltar às tarefas',exact:true}).click();
  check(await manual.inputValue()==='LEITURA-MANUAL-PRESERVADA','OPERACAO_MANUAL_PRESERVADA_APOS_TAREFA');
  await page.getByRole('button',{name:/^Endereçar/}).click();await page.getByRole('button',{name:'Ver tarefa',exact:true}).click();
  let dialog=page.getByRole('dialog');await dialog.getByLabel('1. Leia a etiqueta da mercadoria',{exact:true}).fill(flow.claimBeforeRestart.codigoUnidade);
  for(const label of ['Peso (kg)','Altura (m)','Largura (m)','Profundidade (m)'])await dialog.getByLabel(label,{exact:true}).fill('1');
  await dialog.getByLabel('2. Leia a posição de destino',{exact:true}).fill(flow.fixtures.storage2.codigo);
  await dialog.getByRole('button',{name:'Conferir destino',exact:true}).click();await dialog.getByRole('button',{name:'Confirmar endereçamento',exact:true}).waitFor();
  check(true,'DESTINO_ENDERECAMENTO_RESOLVIDO_EM_BROWSER_REAL');await page.screenshot({path:path.join(outputs,'coletor-enderecamento-conferencia.png'),fullPage:true});
  await page.keyboard.press('Escape');
  await page.getByRole('button',{name:/^Separação para saída/}).click();await page.getByRole('button',{name:'Ver tarefa',exact:true}).click();
  dialog=page.getByRole('dialog');await dialog.getByText('Aguardando reserva',{exact:true}).waitFor();check(await dialog.getByRole('button',{name:'Iniciar tarefa',exact:true}).count()===0,'BROWSER_RASCUNHO_SEM_INICIAR');
  await page.keyboard.press('Escape');
 }else{check((await page.locator('.collector-queue-count').allTextContents()).every(n=>n==='0'),'BROWSER_CARDS_ZERADOS');}
 check(proof.errors.length===0,'SEM_ERROS_JAVASCRIPT');check(JSON.stringify(proof.frontendFiles)===JSON.stringify(await fingerprints()),'FONTES_FRONTEND_ESTAVEIS_DURANTE_PROVA');proof.state='APROVADO';
}catch(e){proof.state='INTERROMPIDO';proof.failure=String(e.message).slice(0,280);process.exitCode=1;}
finally{await browser.close();proof.finished=new Date().toISOString();const file=path.join(area,'browser-'+Date.now()+'.json');await writeFile(file,JSON.stringify(proof,null,2));console.log(JSON.stringify({state:proof.state,checks:proof.checks.length,failure:proof.failure,file}));}
