import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';
import { createServer, preview } from 'vite';

const frontend = path.resolve(import.meta.dirname, '..');
const evidence = path.join(frontend, 'evidencias');
const area = path.join(evidence, 'prumo-css-20261008');
const sha = data => createHash('sha256').update(data).digest('hex');
const previousPath = path.join(evidence, 'frontend-prumo-css-lucas-20261008.json');
const previous = JSON.parse(readFileSync(previousPath));
writeFileSync(path.join(area, 'prova-r02-css-verde.json'), JSON.stringify(previous, null, 2));
const config = { root: path.join(area, 'verified-source-r02'), configFile: path.join(frontend, 'vite.config.ts'), cacheDir: path.join(area, 'cache-verified'), server: { host: '127.0.0.1', port: 5191, strictPort: true, open: false } };
const report = { at: new Date().toISOString(), pid: process.pid, url: 'http://127.0.0.1:5191', checks: [], responses: [], screenshots: [], errors: [], blocked: [] };
const browser = await chromium.launch({ headless: true });
async function capture(page, mode, label) {
    const state = await page.evaluate(() => {
        const css = e => { const s = getComputedStyle(e); return Object.fromEntries(['fontFamily','fontSize','margin','padding','display','backgroundColor','color','border','outline','outlineOffset','minHeight','overflowX'].map(k => [k,s[k]])); };
        return { url: location.href, width: innerWidth, documentWidth: document.documentElement.scrollWidth, body: css(document.body), main: css(document.querySelector('main')), focus: { text: document.activeElement.textContent.trim().slice(0,100), ...css(document.activeElement) }, details: [...document.querySelectorAll('.collector details')].map(el => ({ open: el.open, summary: el.querySelector('summary').textContent.trim(), ...css(el.querySelector('summary')) })), receipt: document.querySelector('[aria-label="Resumo confirmado no coletor"]') ? css(document.querySelector('[aria-label="Resumo confirmado no coletor"]')) : null, violations: window.cssViolations };
    });
    if (state.documentWidth > state.width || !state.body.fontFamily.includes('Arial') || state.violations.length) throw Error('CSS/overflow/CSP complemento');
    const file = `prumo-css-${mode}-${label}-${state.width}.png`;
    await page.screenshot({ path: path.join(evidence,file), fullPage: true });
    report.checks.push({ mode, label, ...state });
    report.screenshots.push({ mode, label, file, sha256: sha(readFileSync(path.join(evidence,file))) });
    console.log(mode,label,state.width,'ok');
}
async function run(mode,width,collector=false) {
    const context = await browser.newContext({ viewport: { width, height: width===390 ? 844 : 1000 },serviceWorkers:'block' });
    const page = await context.newPage(); page.setDefaultTimeout(12000);
    const responses = [];
    await context.route('**/*', r => { const u=new URL(r.request().url()); if(u.origin==='http://127.0.0.1:5191' && !u.pathname.startsWith('/api')) return r.continue(); report.blocked.push(u.href); return r.abort(); });
    await context.routeWebSocket('**/*', socket => { const u=new URL(socket.url()); if(u.hostname==='127.0.0.1'&&u.port==='5191') socket.connectToServer(); else {report.blocked.push(socket.url());socket.close();} });
    await page.addInitScript(() => { window.cssViolations=[]; document.addEventListener('securitypolicyviolation',e=>window.cssViolations.push({ directive:e.effectiveDirective, blockedURI:e.blockedURI })); });
    page.on('console',m=>{if(m.type()==='error')report.errors.push(m.text());});
    page.on('pageerror',e=>report.errors.push(e.message));
    page.on('response',r=>responses.push((async()=>{try {const body=await r.body();report.responses.push({mode,width,url:r.url(),status:r.status(),contentType:r.headers()['content-type'],sha256:sha(body)});}catch {}})()));
    try {
        await page.goto('http://127.0.0.1:5191/#'+(collector?'coletor':'cadastros'));
        if(collector) {
            await page.getByLabel('1. Leia o UUID da unidade').fill('00000000-0000-4000-8000-000000000001');
            await page.getByLabel('1. Leia o UUID da unidade').press('Enter');
            await page.getByLabel('2. Leia o código da posição').fill('A101');
            await page.getByLabel('2. Leia o código da posição').press('Enter');
            await page.getByLabel('Motivo / justificativa *',{exact:true}).fill('Prova CSS fictícia Prumo');
            const details=page.getByText('Medidas físicas e conjunto de duas posições',{exact:true});
            await details.focus(); await page.keyboard.press('Enter');
            await capture(page,mode,'coletor-detalhes-abertos');
            await details.focus(); await page.keyboard.press('Enter');
            await page.getByRole('button',{name:'Conferir e confirmar',exact:true}).click();
            await capture(page,mode,'coletor-revisao');
            await page.getByRole('button',{name:'Confirmar agora',exact:true}).click();
            await page.getByRole('region',{name:'Resumo confirmado no coletor',exact:true}).waitFor();
            await capture(page,mode,'coletor-resumo');
            await page.getByText('Ver detalhes da confirmação',{exact:true}).focus();
            await page.keyboard.press('Enter');
            await capture(page,mode,'coletor-confirmacao-aberta');
        } else {
            await page.getByRole('navigation',{name:'Módulos',exact:true}).getByRole('button',{name:'Entrada e conferência',exact:true}).click();
            await page.getByRole('heading',{level:1,name:'Entrada e conferência',exact:true}).waitFor();
            await page.getByRole('button',{name:'Consultar',exact:true}).click();
            await page.locator('.feedback p').waitFor();
            await capture(page,mode,'entrada-navegacao-feedback');
            const nav=page.getByRole('navigation',{name:'Módulos',exact:true});
            await nav.getByRole('button').first().focus();
            const n=await nav.getByRole('button').count();
            for(let i=1;i<n;i++)await page.keyboard.press('Tab');
            const state=await nav.evaluate(el=>({ scrollLeft:el.scrollLeft, clientWidth:el.clientWidth,scrollWidth:el.scrollWidth,focused:document.activeElement.textContent.trim(),outline:getComputedStyle(document.activeElement).outline }));
            if(state.focused!=='Acesso e limites'||!state.outline.includes('3px')||(width<=800&&state.scrollLeft===0))throw Error('Navegação horizontal teclado');
            report.checks.push({mode,label:'nav-teclado-ultimo-modulo',width,...state});
        }
        await Promise.allSettled(responses);
    } finally { await context.close(); }
}
let server;
try {
    for(const mode of ['dev','build']) {
        server=mode==='dev'?await createServer(config):await preview({...config,build:{outDir:path.join(area,'dist-verified')},preview:{host:'127.0.0.1',port:5191,strictPort:true,open:false}});
        if(mode==='dev')await server.listen();
        for(const width of [1440,768])await run(mode,width);
        await run(mode,390,true);
        if(server.close)await server.close();else await new Promise(resolve=>server.httpServer.close(resolve));server=null;
    }
    if(report.errors.length||report.blocked.length)throw Error('Erros/rede complemento');
    report.result='PASS';
} catch(e) {report.result='FAIL';report.failure=e.stack;console.error(e);process.exitCode=1;}
finally {
    if(server){if(server.close)await server.close();else await new Promise(resolve=>server.httpServer.close(resolve));}
    await browser.close();report.processClosed=true;report.endedAt=new Date().toISOString();
    report.ownedStillSame=previous.owned.every(({file,sha256})=>sha(readFileSync(path.join(frontend,file)))===sha256);
    writeFileSync(path.join(area,'complemento.json'),JSON.stringify(report,null,2));
    previous.complement=report;
    previous.result=previous.result==='PASS'&&report.result==='PASS'&&report.ownedStillSame?'PASS':'FAIL';
    writeFileSync(previousPath,JSON.stringify(previous,null,2));
}
console.log('COMPLEMENTO',report.result);
