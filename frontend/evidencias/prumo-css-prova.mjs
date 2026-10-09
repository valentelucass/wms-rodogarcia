import { createHash } from 'node:crypto';
import { cpSync, mkdirSync, readdirSync, readFileSync, writeFileSync, existsSync } from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';
import { build, createServer, preview } from 'vite';

const frontend = path.resolve(import.meta.dirname, '..');
const evidence = path.join(frontend, 'evidencias');
const area = path.join(evidence, 'prumo-css-20261008');
const source = path.join(area, 'verified-source-r02');
const sha = data => createHash('sha256').update(data).digest('hex');
const own = ['index.html', 'src/main.tsx', 'src/styles.css', ...readdirSync(path.join(frontend, 'src/styles')).map(f => 'src/styles/' + f)];
if (existsSync(source)) throw Error('Snapshot próprio já existe: preservar');
cpSync(path.join(area, 'diagnostic-source-v03'), source, { recursive: true });
const owned = own.map(file => ({ file, sha256: sha(readFileSync(path.join(frontend, file))) }));
for (const { file, sha256 } of owned) {
    cpSync(path.join(frontend, file), path.join(source, file));
    if (sha(readFileSync(path.join(source, file))) !== sha256 || sha(readFileSync(path.join(frontend, file))) !== sha256) throw Error('Fonte mudou na cópia: ' + file);
}
const files = readdirSync(source, { recursive: true, withFileTypes: true }).filter(x => x.isFile()).map(x => path.relative(source, path.join(x.parentPath, x.name)));
const sourceManifest = files.map(file => ({ file, sha256: sha(readFileSync(path.join(source, file))) }));
writeFileSync(path.join(area, 'snapshot-verificado.json'), JSON.stringify({ basis: 'snapshot-diagnostic-v03.json + CSS/entrypoint próprios', owned, files: sourceManifest }, null, 2));
const config = { root: source, configFile: path.join(frontend, 'vite.config.ts'), cacheDir: path.join(area, 'cache-verified'), server: { host: '127.0.0.1', port: 5191, strictPort: true, open: false } };
const report = { at: new Date().toISOString(), pid: process.pid, port: 5191, ownsProcess: true, browserPath: chromium.executablePath(), owned, sourceManifest, screenshots: [], checks: [], responses: [], blocked: [], errors: [], violations: [], commands: ['node node_modules/prettier/bin/prettier.cjs --write (somente oito arquivos em posse)', 'PLAYWRIGHT_BROWSERS_PATH=frontend/.tools/ms-playwright node evidencias/prumo-css-prova.mjs'], diagnosis: 'DEV antes: style inline injetado pelo Vite bloqueado por CSP style-src self, corpo Times New Roman/margin8/zero sheets. BUILD antes aplicava CSS externo. URL original vista por Lucas não conhecida.' };
const browser = await chromium.launch({ headless: true });
async function inspect(page, mode, label) {
    const result = await page.evaluate(() => {
        const props = ['fontFamily', 'fontSize', 'lineHeight', 'margin', 'padding', 'display', 'gap', 'gridTemplateColumns', 'backgroundColor', 'color', 'border', 'outline', 'outlineOffset', 'minHeight', 'overflowX', 'flexWrap'];
        const css = el => el ? { tag: el.tagName, text: el.textContent.trim().slice(0, 150), box: { x: el.getBoundingClientRect().x, y: el.getBoundingClientRect().y, width: el.getBoundingClientRect().width, height: el.getBoundingClientRect().height }, ...Object.fromEntries(props.map(k => [k, getComputedStyle(el)[k]])) } : null;
        const stylesheets = [];
        const visit = sheet => { stylesheets.push({ href: sheet.href, rules: sheet.cssRules.length }); for (const rule of sheet.cssRules) if (rule.styleSheet) visit(rule.styleSheet); };
        [...document.styleSheets].forEach(visit);
        const scrolls = [...document.querySelectorAll('.table-scroll, .main-nav')].map(el => ({ class: el.className, clientWidth: el.clientWidth, scrollWidth: el.scrollWidth, overflowX: getComputedStyle(el).overflowX }));
        return { url: location.href, viewport: { width: innerWidth, height: innerHeight }, documentWidth: document.documentElement.scrollWidth, media800: matchMedia('(max-width: 800px)').matches, media480: matchMedia('(max-width: 480px)').matches, stylesheets, scrolls, body: css(document.body), header: css(document.querySelector('header')), layout: css(document.querySelector('.layout')), main: css(document.querySelector('main')), nav: css(document.querySelector('.main-nav')), button: css(document.querySelector('main button.primary')), input: css(document.querySelector('main input')), label: css(document.querySelector('main label')), fieldset: css(document.querySelector('main fieldset')), table: css(document.querySelector('table')), cell: css(document.querySelector('td')), feedback: css(document.querySelector('.feedback:has(p), .collector [role="alert"]')), focus: css(document.activeElement), summary: css(document.querySelector('.collector summary')), violations: window.cssViolations };
    });
    report.checks.push({ mode, label, ...result });
    if (result.documentWidth > result.viewport.width) throw Error('Overflow documento: ' + mode + '/' + label);
    if (!result.body.fontFamily.includes('Arial') || result.body.margin !== '0px' || result.header.backgroundColor !== 'rgb(18, 76, 171)') throw Error('CSS não aplicado: ' + mode + '/' + label);
    if (!result.stylesheets.length || result.stylesheets.some(s => !s.href)) throw Error('Stylesheet sem URL externa');
    if (result.violations.length) throw Error('Violação CSP: ' + JSON.stringify(result.violations));
    const screenshot = `prumo-css-${mode}-${label}-${result.viewport.width}.png`;
    await page.screenshot({ path: path.join(evidence, screenshot), fullPage: true });
    report.screenshots.push({ file: screenshot, sha256: sha(readFileSync(path.join(evidence, screenshot))), mode, label });
    console.log(mode, label, result.viewport.width, 'CSS/overflow/CSP ok', result.url);
}
async function exercise(mode, width, collector = false) {
    const context = await browser.newContext({ viewport: { width, height: width === 390 ? 844 : 1000 }, serviceWorkers: 'block' });
    const page = await context.newPage();
    page.setDefaultTimeout(12000);
    const pendingResponses = [];
    await context.route('**/*', route => {
        const url = new URL(route.request().url());
        if (url.origin === 'http://127.0.0.1:5191' && !url.pathname.startsWith('/api')) return route.continue();
        report.blocked.push({ mode, url: url.href }); return route.abort('blockedbyclient');
    });
    await context.routeWebSocket('**/*', socket => { if (new URL(socket.url()).hostname === '127.0.0.1' && new URL(socket.url()).port === '5191') socket.connectToServer(); else { report.blocked.push({ mode, url: socket.url() }); socket.close(); } });
    await page.addInitScript(() => { window.cssViolations = []; document.addEventListener('securitypolicyviolation', e => window.cssViolations.push({ directive: e.effectiveDirective, blockedURI: e.blockedURI })); });
    page.on('console', msg => { if (msg.type() === 'error') report.errors.push({ mode, text: msg.text() }); });
    page.on('pageerror', err => report.errors.push({ mode, text: err.message }));
    page.on('response', response => { pendingResponses.push((async () => { try { const body = await response.body(); report.responses.push({ mode, width, url: response.url(), status: response.status(), contentType: response.headers()['content-type'], sha256: sha(body), bytes: body.length }); } catch {} })()); });
    try {
        await page.goto('http://127.0.0.1:5191/#' + (collector ? 'coletor' : 'cadastros'));
        await page.getByRole('heading', { level: 1, name: collector ? 'Coletor' : 'Cadastros', exact: true }).waitFor();
        await page.keyboard.press('Tab');
        const skip = await page.evaluate(() => ({ text: document.activeElement.textContent, outline: getComputedStyle(document.activeElement).outline, x: document.activeElement.getBoundingClientRect().x }));
        if (skip.text.trim() !== 'Ir para o conteúdo' || skip.x < 0 || !skip.outline.includes('3px')) throw Error('Foco skip não visível');
        await page.keyboard.press('Enter');
        if (await page.evaluate(() => document.activeElement.id) !== 'conteudo') throw Error('Skip não alcançou main');
        await page.waitForTimeout(50);
        const skipContent = await page.locator('main').innerText();
        report.checks.push({ mode, label: 'skip-keyboard', width, skip, contentAfterEnter: skipContent, knownNavigationDefect: skipContent.includes('Módulo não encontrado'), delegatedTo: 'Lume via Farol, sem edição fora de posse' });
        await page.goto('http://127.0.0.1:5191/#' + (collector ? 'coletor' : 'cadastros'));
        await page.getByRole('heading', { level: 1, name: collector ? 'Coletor' : 'Cadastros', exact: true }).waitFor();
        if (collector) {
            await page.getByLabel('1. Leia o UUID da unidade').focus();
            const focused = await page.getByLabel('1. Leia o UUID da unidade').evaluate(el => ({ outline: getComputedStyle(el).outline, height: el.getBoundingClientRect().height }));
            if (!focused.outline.includes('3px') || focused.height < 52) throw Error('Controle coletor/foco');
            await inspect(page, mode, 'coletor-foco');
            await page.getByLabel('1. Leia o UUID da unidade').press('Enter');
            await page.locator('.collector [role="alert"]').waitFor();
            await inspect(page, mode, 'coletor-erro');
            await page.getByLabel('1. Leia o UUID da unidade').fill('00000000-0000-4000-8000-000000000001');
            await page.getByLabel('1. Leia o UUID da unidade').press('Enter');
            await page.getByLabel('2. Leia o código da posição').waitFor();
            await inspect(page, mode, 'coletor-consulta');
            const nextFocus = await page.evaluate(() => ({ label: document.activeElement.closest('label')?.textContent.trim(), outline: getComputedStyle(document.activeElement).outline }));
            if (!nextFocus.label?.startsWith('2. Leia')) throw Error('Foco próximo passo coletor');
            report.checks.push({ mode, label: 'coletor-next-focus', width, ...nextFocus });
        } else {
            await page.getByRole('button', { name: 'Consultar', exact: true }).click();
            await page.locator('table').waitFor();
            await page.locator('.feedback').scrollIntoViewIfNeeded();
            await inspect(page, mode, 'cadastros-tabela-sucesso');
            await page.getByLabel('Resposta do exercício fictício').selectOption('falha');
            await page.getByRole('button', { name: 'Consultar', exact: true }).click();
            await page.locator('.feedback[role="alert"] p').first().waitFor();
            await page.locator('.feedback').scrollIntoViewIfNeeded();
            await inspect(page, mode, 'cadastros-erro');
            await page.getByLabel('Resposta do exercício fictício').selectOption('sucesso');
            await page.getByRole('button', { name: 'Novo registro', exact: true }).click();
            await page.locator('main fieldset input').first().focus();
            await inspect(page, mode, 'cadastros-formulario');
        }
        await Promise.allSettled(pendingResponses);
    } finally { await context.close(); }
}
let server;
try {
    server = await createServer(config); await server.listen();
    for (const width of [1440, 768]) await exercise('dev', width);
    await exercise('dev', 390, true);
    await server.close(); server = null;
    await build({ ...config, build: { outDir: path.join(area, 'dist-verified'), emptyOutDir: false } });
    server = await preview({ ...config, build: { outDir: path.join(area, 'dist-verified') }, preview: { host: '127.0.0.1', port: 5191, strictPort: true, open: false } });
    for (const width of [1440, 768]) await exercise('build', width);
    await exercise('build', 390, true);
    report.buildAssets = readdirSync(path.join(area, 'dist-verified/assets')).map(file => ({ file, sha256: sha(readFileSync(path.join(area, 'dist-verified/assets', file))) }));
    if (report.errors.length || report.blocked.length) throw Error('Erros ou acesso fora da fronteira: ' + JSON.stringify({ errors: report.errors, blocked: report.blocked }));
    report.result = 'PASS';
} catch (error) { report.result = 'FAIL'; report.failure = error.stack; process.exitCode = 1; console.error(error); }
finally {
    if (server) { if (server.close) await server.close(); else await new Promise(resolve => server.httpServer.close(resolve)); }
    await browser.close();
    report.processClosed = true;
    report.endedAt = new Date().toISOString();
    report.ownedStillSame = owned.every(({ file, sha256 }) => sha(readFileSync(path.join(frontend, file))) === sha256);
    writeFileSync(path.join(evidence, 'frontend-prumo-css-lucas-20261008.json'), JSON.stringify(report, null, 2));
}
console.log('RESULTADO', report.result, 'capturas', report.screenshots.length, 'erros', report.errors.length, 'bloqueados', report.blocked.length);
