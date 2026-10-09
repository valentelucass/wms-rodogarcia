import { createHash } from 'node:crypto';
import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { chromium } from 'playwright';
import { createServer, loadConfigFromFile } from 'vite';

const frontend=path.resolve(import.meta.dirname,'..');
const evidence=path.join(frontend,'evidencias');
const area=path.join(evidence,'prumo-dev-config-20261008');
const source=path.join(area,'source');
const sha=data=>createHash('sha256').update(data).digest('hex');
if(existsSync(source))throw Error('Preservar snapshot proprio existente');
mkdirSync(source,{recursive:true});
const closurePath=path.join(evidence,'snapshot-marco02-r08/closure-fronteiras.json');
const closure=JSON.parse(readFileSync(closurePath));
const r08Manifest=path.join(evidence,'snapshot-marco02-r08/manifesto.json');
if(sha(readFileSync(r08Manifest))!==closure.snapshotManifestSha256)throw Error('Manifesto r08 diferente');
const r08=path.join(evidence,'snapshot-marco02-r08/frontend');
const copied=[];
for(const item of closure.closure.filter(x=>x.path.startsWith('src/')||['index.html','package.json','package-lock.json','tsconfig.json','vite.config.ts'].includes(x.path))) {
    const origin=path.join(r08,item.path);
    if(sha(readFileSync(origin))!==item.sha256)throw Error('r08 diverge: '+item.path);
    const destination=path.join(source,item.path);mkdirSync(path.dirname(destination),{recursive:true});cpSync(origin,destination);
    if(sha(readFileSync(destination))!==item.sha256)throw Error('Copia r08 diverge: '+item.path);
    copied.push({...item,origin:'closure r08 verificado'});
}
const cssReceiptPath=path.join(evidence,'frontend-prumo-css-lucas-20261008.json');
const cssReceipt=JSON.parse(readFileSync(cssReceiptPath));
if(sha(readFileSync(cssReceiptPath))!=='2453dc770565379d01bdc21635f134080a578767418739c9200757010fa5f608')throw Error('Recibo CSS mudou');
for(const item of cssReceipt.owned) {
    const origin=path.join(evidence,'prumo-css-20261008/verified-source-r02',item.file);
    if(sha(readFileSync(origin))!==item.sha256)throw Error('Freeze CSS diverge: '+item.file);
    const destination=path.join(source,item.file);mkdirSync(path.dirname(destination),{recursive:true});cpSync(origin,destination);
    copied.push({path:item.file,sha256:item.sha256,origin:'8 CSS/entrypoint freeze Prumo'});
}
const own=['vite.dev.config.ts','docs/desenvolvimento-dev.md'];
const ownHashes=own.map(file=>({path:file,sha256:sha(readFileSync(path.join(frontend,file)))}));
for(const item of ownHashes) {const dest=path.join(source,item.path);mkdirSync(path.dirname(dest),{recursive:true});cpSync(path.join(frontend,item.path),dest);if(sha(readFileSync(dest))!==item.sha256)throw Error('Nova fonte mudou');copied.push({...item,origin:'posse DEV01 Prumo'});}
const guardRelative='evidencias/frontend-prumo-dev-guarda-lucas-20261008.json';
const guard=path.join(frontend,guardRelative);
const guardSha=sha(readFileSync(guard));
if(guardSha!=='9135bc92a2c1bf3df012d47b8ea19e196b835d0a153f840f10d3aa34f15c9005')throw Error('Guarda recebida mudou');
mkdirSync(path.dirname(path.join(source,guardRelative)),{recursive:true});cpSync(guard,path.join(source,guardRelative));
copied.push({path:guardRelative,sha256:guardSha,origin:'recibo bloqueado real recebido, sem fixture de guarda'});
writeFileSync(path.join(area,'snapshot.json'),JSON.stringify({at:new Date().toISOString(),closureSha256:sha(readFileSync(closurePath)),r08ManifestSha256:closure.snapshotManifestSha256,copied},null,2));
const report={id:'D31-DEV01-PRUMO-CONFIG-20261008',at:new Date().toISOString(),pid:process.pid,owned:ownHashes,guardSha256:guardSha,guardApproved:false,realProxyApproved:false,source:path.relative(frontend,source),sources:copied,checks:[],requests:[],responses:[],errors:[],blocked:[],commands:['node node_modules/prettier/bin/prettier.cjs --write vite.dev.config.ts docs/desenvolvimento-dev.md','PLAYWRIGHT_BROWSERS_PATH=frontend/.tools/ms-playwright node evidencias/prumo-dev-config-prova.mjs'],originalConfigBeforeSha256:sha(readFileSync(path.join(frontend,'vite.config.ts')))};
const configFile=path.join(source,'vite.dev.config.ts');
const names=['VITE_DATA_MODE','WMS_PORT','WMS_DEV_BACKEND_URL','WMS_DEV_GUARD_APPROVED'];
const originals=new Map(names.map(n=>[n,process.env[n]]));
function environment(values){for(const n of names)delete process.env[n];Object.assign(process.env,values);}
async function configCase(name,values,expectedError,target) {
    environment(values);let observed;
    try {
        const loaded=await loadConfigFromFile({command:'serve',mode:'development'},configFile,source,'silent');
        if(!loaded)throw Error('Configuracao nao carregada');
        const module=await import(pathToFileURL(configFile).href+'?case='+encodeURIComponent(name));
        const server=loaded.config.server;
        if(expectedError)throw Error('Modo recusado aceito: '+name);
        if(server.proxy!==undefined||server.host!=='127.0.0.1'||server.port!==5178||server.strictPort!==true)throw Error('Servidor ficticio/proxy alterado');
        const planned=module.proxyApiDev['/api'];
        if(planned.target!==target||planned.rewrite!==undefined||planned.changeOrigin!==false)throw Error('Contrato proxy destino/prefixo');
        const plugin=loaded.config.plugins.flat(Infinity).find(p=>p?.name==='wms-dev-api-bloqueada');
        let middleware;
        plugin.configureServer({middlewares:{use(prefix,handler){if(prefix!=='/api')throw Error('Prefixo middleware');middleware=handler;}}});
        const response={statusCode:0,headers:{},setHeader(k,v){this.headers[k]=v;},end(body){this.body=body;}};
        middleware({},response);
        if(response.statusCode!==503||!response.body.includes('API real desabilitada'))throw Error('API local nao recusada');
        observed={host:server.host,port:server.port,strictPort:server.strictPort,proxyAbsent:true,plannedProxy:{prefix:'/api',...planned,rewriteAbsent:true},localMiddlewareWithoutHttp:{status:response.statusCode,contentType:response.headers['Content-Type'],body:response.body},modeDefine:loaded.config.define['import.meta.env.VITE_DATA_MODE']};
    } catch(error) {
        if(!expectedError||!error.message.startsWith(expectedError))throw error;
        observed={error:error.message,serverNotCreated:true,expectedPrefix:expectedError};
    }
    report.checks.push({name,inputs:values,result:'PASS',...observed});console.log('CONFIG PASS',name);
}
let server,browser;
try {
    await configCase('padrao-ficticio',{},null,'http://127.0.0.1:8080');
    await configCase('porta-WMS_PORT-contrato-preparado',{WMS_PORT:'8089'},null,'http://127.0.0.1:8089');
    await configCase('destino-explicito-loopback',{WMS_DEV_BACKEND_URL:'http://127.0.0.1:8090/'},null,'http://127.0.0.1:8090');
    await configCase('real-guarda-G01-bloqueada',{VITE_DATA_MODE:'real'},'WMS_DEV_G01_BLOQUEADO');
    await configCase('real-flag-boolean-nao-libera',{VITE_DATA_MODE:'real',WMS_DEV_GUARD_APPROVED:'true'},'WMS_DEV_G01_BLOQUEADO');
    await configCase('destino-host-nao-loopback',{WMS_DEV_BACKEND_URL:'http://example.invalid:8080'},'WMS_DEV_DESTINO_INVALIDO');
    await configCase('destino-nao-aceita-prefixo-api',{WMS_DEV_BACKEND_URL:'http://127.0.0.1:8080/api/v1'},'WMS_DEV_DESTINO_INVALIDO');
    await configCase('destino-porta-fora-intervalo',{WMS_DEV_BACKEND_URL:'http://127.0.0.1:65536'},'WMS_DEV_DESTINO_INVALIDO');
    await configCase('WMS_PORT-invalida',{WMS_PORT:'0'},'WMS_DEV_PORTA_INVALIDA');
    await configCase('modo-desconhecido',{VITE_DATA_MODE:'dev'},'WMS_DEV_MODO_INVALIDO');
    environment({VITE_DATA_MODE:'ficticio'});
    server=await createServer({root:source,configFile,cacheDir:path.join(area,'cache'),server:{host:'127.0.0.1',port:5191,strictPort:true,open:false}});
    if(server.config.server.proxy!==undefined)throw Error('Proxy ativo no bootstrap');
    await server.listen();
    report.bootstrap={url:'http://127.0.0.1:5191/#coletor',host:server.config.server.host,port:server.config.server.port,proxyAbsent:true,ownProcess:true,backendStarted:false};
    browser=await chromium.launch({headless:true});
    const context=await browser.newContext({viewport:{width:390,height:844},serviceWorkers:'block'});
    const page=await context.newPage();page.setDefaultTimeout(12000);
    const pending=[];
    await context.route('**/*',r=>{const u=new URL(r.request().url());const safe=u.origin+u.pathname;report.requests.push({url:safe,method:r.request().method(),type:r.request().resourceType()});if(u.origin==='http://127.0.0.1:5191'&&!u.pathname.startsWith('/api'))return r.continue();report.blocked.push(safe);return r.abort();});
    await context.routeWebSocket('**/*',socket=>{const u=new URL(socket.url());if(u.hostname==='127.0.0.1'&&u.port==='5191')socket.connectToServer();else{report.blocked.push(u.origin+u.pathname);socket.close();}});
    await page.addInitScript(()=>{window.cssViolations=[];document.addEventListener('securitypolicyviolation',e=>window.cssViolations.push({directive:e.effectiveDirective,blockedURI:e.blockedURI}));});
    page.on('console',m=>{if(m.type()==='error')report.errors.push(m.text());});page.on('pageerror',e=>report.errors.push(e.message));
    page.on('response',r=>pending.push((async()=>{try{const body=await r.body();const u=new URL(r.url());report.responses.push({url:u.origin+u.pathname,status:r.status(),contentType:r.headers()['content-type'],sha256:sha(body)});}catch{}})()));
    await page.goto(report.bootstrap.url);
    await page.getByRole('heading',{level:1,name:'Coletor',exact:true}).waitFor();
    await page.getByLabel('1. Leia o UUID da unidade').fill('00000000-0000-4000-8000-000000000001');
    await page.getByLabel('1. Leia o UUID da unidade').press('Enter');
    await page.getByLabel('2. Leia o codigo da posicao').count();
    await page.getByLabel('2. Leia o código da posição').waitFor();
    const state=await page.evaluate(()=>({url:location.href,title:document.querySelector('h1').textContent,header:document.querySelector('header').innerText,bodyFont:getComputedStyle(document.body).fontFamily,bodyMargin:getComputedStyle(document.body).margin,proxyNetworkRequests:performance.getEntriesByType('resource').filter(e=>new URL(e.name).pathname.startsWith('/api')).length,sheets:[...document.styleSheets].map(s=>({href:s.href,rules:s.cssRules.length})),documentWidth:document.documentElement.scrollWidth,width:innerWidth,violations:window.cssViolations,focusLabel:document.activeElement.closest('label')?.textContent.trim()}));
    if(!state.header.includes('FICTÍCIO')||state.proxyNetworkRequests!==0||state.violations.length||!state.bodyFont.includes('Arial')||state.documentWidth>state.width||report.errors.length||report.blocked.length)throw Error('Bootstrap ficticio/rede/CSS falhou');
    const screenshot='prumo-dev-config-bootstrap-ficticio-390.png';
    await page.screenshot({path:path.join(evidence,screenshot),fullPage:true});
    await Promise.allSettled(pending);
    report.checks.push({name:'bootstrap-ficticio-r08-mais-CSS-freeze',result:'PASS',...state,screenshot,sha256:sha(readFileSync(path.join(evidence,screenshot)))});
    await context.close();report.result='PASS';
} catch(error) {report.result='FAIL';report.failure=error.stack;console.error(error);process.exitCode=1;}
finally {
    if(browser)await browser.close();if(server)await server.close();
    for(const [name,value]of originals){if(value===undefined)delete process.env[name];else process.env[name]=value;}
    report.ownProcessClosed=true;
    report.ownedSame=ownHashes.every(x=>sha(readFileSync(path.join(frontend,x.path)))===x.sha256);
    report.originalConfigAfterSha256=sha(readFileSync(path.join(frontend,'vite.config.ts')));
    report.originalConfigPreserved=report.originalConfigBeforeSha256===report.originalConfigAfterSha256;
    report.cssReceiptPreserved=sha(readFileSync(cssReceiptPath))==='2453dc770565379d01bdc21635f134080a578767418739c9200757010fa5f608';
    report.guardReceiptPreserved=sha(readFileSync(guard))===guardSha;
    report.endedAt=new Date().toISOString();
    writeFileSync(path.join(evidence,'frontend-prumo-dev-config-lucas-20261008.json'),JSON.stringify(report,null,2));
}
console.log('RESULTADO',report.result,'checks',report.checks.length,'backend/API/external',report.blocked.length,'console',report.errors.length,'processo fechado',report.ownProcessClosed);
