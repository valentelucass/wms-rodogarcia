import {readFile,writeFile} from 'node:fs/promises';
import {createRequire} from 'node:module';
import {createHash} from 'node:crypto';
import assert from 'node:assert/strict';
import path from 'node:path';
const require=createRequire(import.meta.url);
const {chromium}=require('C:/Users/suporte/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright');
let input='';for await(const c of process.stdin)input+=c;const secret=JSON.parse(input);input='';
const area=process.argv[2],root=path.resolve(import.meta.dirname,'../../..');
const outputs='C:/Users/suporte/Documents/Codex/2026-10-28/wms-farol-supervisor/outputs';
const flow=JSON.parse(await readFile(path.join(root,'orchestracao/.runtime/coletor-tarefas-20261028/fluxos.json'),'utf8'));
assert.equal(flow.state,'APROVADO');assert.equal(flow.fixtures.order.id,117);
const proof={id:'SAIDA-COLETOR01',state:'RUNNING',started:new Date().toISOString(),sourceHash:secret.sourceHash,jarSha256:secret.jarSha256,checks:[],http:[],layouts:[],errors:[],businessWrites:[]};
const sourcePaths=['frontend/src/modules/saida/definition.ts','frontend/src/modules/saida/continuation.ts','frontend/src/modules/saida/DispatchTaskStatus.tsx','frontend/src/components/records/RecordDialog.tsx','frontend/src/components/Collector.tsx','frontend/src/modules/coletor/CollectorTasks.tsx','frontend/src/auth/AuthApp.tsx','frontend/src/pages/CurrentPage.tsx','frontend/src/domain/recordPages.ts','frontend/src/contracts/endpoints.json'];
async function fingerprints(){return Object.fromEntries(await Promise.all(sourcePaths.map(async p=>[p,createHash('sha256').update(await readFile(path.join(root,p))).digest('hex')])));}
proof.frontendFiles=await fingerprints();
const check=(ok,code)=>{assert.ok(ok,code);proof.checks.push(code);};
const browser=await chromium.launch({channel:'msedge',headless:true});
try{
 const page=await browser.newPage({viewport:{width:1640,height:950}});page.setDefaultTimeout(30000);
 page.on('pageerror',e=>proof.errors.push(e.name));
 page.on('request',r=>{if(new URL(r.url()).pathname.startsWith('/api/v1/')&&!['GET','OPTIONS'].includes(r.method()))proof.businessWrites.push({method:r.method(),path:new URL(r.url()).pathname});});
 await page.goto(secret.base,{waitUntil:'domcontentloaded'});
 await page.getByLabel('E-mail',{exact:true}).fill(secret.email);await page.getByLabel('Senha',{exact:true}).fill(secret.senha);secret.senha='';
 const authResponse=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/auth/entrar'&&r.request().method()==='POST');
 await page.getByRole('button',{name:'Entrar',exact:true}).click();
 let auth=await (await authResponse).json();let token=auth.accessToken;auth=null;
 async function get(url,expected=200,authorized=true){
   const r=await page.request.get(secret.base+url,{headers:authorized?{Authorization:'Bearer '+token}:{}});
   proof.http.push({method:'GET',url,status:r.status()});assert.equal(r.status(),expected,url);return r;
 }
 const task=await (await get('/api/v1/tarefas-coletor/pedido-saida/117')).json();
 check(task.id===flow.fixtures.outputTask.id&&task.origemId===117&&task.tipo==='SEPARACAO'&&task.situacao==='CONCLUIDA','CONSULTA_ORIGEM_EXATA_INCLUSIVE_CONCLUIDA_SQL_REAL');
 await get('/api/v1/tarefas-coletor/pedido-saida/112',404);check(true,'HISTORICO_SEM_TAREFA_SEM_BACKFILL');
 await get('/api/v1/tarefas-coletor/pedido-saida/117',401,false);check(true,'CONSULTA_EXIGE_AUTENTICACAO');token='';
 await page.getByRole('button',{name:'Saída, FIFO e reserva',exact:true}).click();
 const context=page.getByRole('form',{name:'Contexto operacional'});
 await context.getByRole('combobox',{name:'Cliente',exact:true}).fill('CT28-fd4190ec');
 await page.getByRole('option',{name:/Cliente fictício CT28-fd4190ec/}).click();
 await context.getByRole('combobox',{name:'Armazém',exact:true}).fill('CT28-fd4190ec');
 await page.getByRole('option',{name:/Armazém fictício CT28-fd4190ec/}).click();
 await context.getByRole('button',{name:'Aplicar contexto',exact:true}).click();
 await page.locator('.record-table tbody tr').filter({hasText:flow.fixtures.order.referencia}).first().waitFor();
 check(await page.getByRole('button',{name:'3. Leitura e separação',exact:true}).count()===0,'ETAPA_OPERACIONAL_REMOVIDA');
 check(await page.getByRole('button',{name:'3. Reversão e cancelamento',exact:true}).count()===1,'REVERSAO_RENUMERADA');
 const nav=page.getByRole('navigation',{name:'Etapas de Saída, FIFO e reserva'});
 check(await nav.getByRole('button').count()===3,'TRES_ETAPAS_ADMINISTRATIVAS');
 async function settle(){await page.evaluate(async()=>{await document.fonts.ready;await new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r)));await Promise.all(document.getAnimations().filter(a=>Number.isFinite(a.effect?.getComputedTiming().endTime)).map(a=>a.finished.catch(()=>{})));});}
 for(const theme of ['light','dark'])for(const width of [390,768,1024,1640]){
   await page.setViewportSize({width,height:950});await page.evaluate(t=>document.documentElement.setAttribute('data-og-theme',t),theme);await settle();
   const size=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));check(size.scroll<=size.width,'SEM_TRANSBORDAMENTO_'+width+'_'+theme);proof.layouts.push({theme,...size});
   await page.screenshot({path:path.join(area,`saida-${width}-${theme}.png`),fullPage:true});
   if(theme==='light'&&[390,1640].includes(width))await page.screenshot({path:path.join(outputs,`saida-depois-${width===390?'celular':'desktop'}.png`),fullPage:true});
 }
 await page.evaluate(()=>document.documentElement.setAttribute('data-og-theme','light'));
 await page.setViewportSize({width:1640,height:950});await settle();
 const row=page.locator('.record-table tbody tr').filter({hasText:flow.fixtures.order.referencia}).first();
 await row.getByRole('button',{name:'Ver detalhes',exact:true}).last().click();
 const panel=page.getByRole('region',{name:'Separação no Coletor'});
 await panel.getByText('Separado',{exact:true}).waitFor();check(true,'ACOMPANHAMENTO_SEPARACAO_REAL_NO_DETALHE');
 await page.screenshot({path:path.join(outputs,'saida-acompanhamento.png'),fullPage:true});
 const exactTask=page.waitForResponse(r=>new URL(r.url()).pathname==='/api/v1/tarefas-coletor/'+task.id&&r.request().method()==='GET');
 await panel.getByRole('button',{name:'Acompanhar no Coletor',exact:true}).click();
 check((await exactTask).status()===200,'ATALHO_ABRE_TAREFA_EXATA_DO_PEDIDO');
 const dialog=page.getByRole('dialog');await dialog.getByText('A tarefa foi encerrada. Os registros da operação foram preservados.',{exact:true}).waitFor();
 check(await dialog.getByRole('button',{name:'Iniciar tarefa',exact:true}).count()===0,'TAREFA_CONCLUIDA_NAO_REINICIA');
 await page.screenshot({path:path.join(outputs,'saida-tarefa-no-coletor.png'),fullPage:true});
 check(proof.businessWrites.length===0,'NENHUMA_ESCRITA_DE_NEGOCIO');check(proof.errors.length===0,'SEM_ERROS_JAVASCRIPT');
 check(JSON.stringify(proof.frontendFiles)===JSON.stringify(await fingerprints()),'FONTES_ESTAVEIS_DURANTE_PROVA');proof.state='APROVADO';
}catch(e){proof.state='INTERROMPIDO';proof.failure=String(e.message).slice(0,360);process.exitCode=1;}
finally{await browser.close();proof.finished=new Date().toISOString();const file=path.join(area,'browser-'+Date.now()+'.json');await writeFile(file,JSON.stringify(proof,null,2));console.log(JSON.stringify({state:proof.state,checks:proof.checks.length,failure:proof.failure,file}));}
