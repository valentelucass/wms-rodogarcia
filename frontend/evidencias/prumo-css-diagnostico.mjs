import { createHash } from 'node:crypto';
import { cpSync, existsSync, mkdirSync, readFileSync, writeFileSync, readdirSync } from 'node:fs';
import path from 'node:path';
import { chromium } from 'playwright';
import { build, createServer, preview } from 'vite';

const frontend = path.resolve(import.meta.dirname, '..');
const evidence = path.join(frontend, 'evidencias');
const area = path.join(evidence, 'prumo-css-20261008');
mkdirSync(area, { recursive: true });
const sha = data => createHash('sha256').update(data).digest('hex');
const manifestPath = path.join(evidence, 'handoff-cedro-prumo-20261008/manifesto.json');
const manifest = JSON.parse(readFileSync(manifestPath));
const manifestSha = sha(readFileSync(manifestPath));
if (manifestSha !== 'f49f86454f8852ddf334c71a04e4ebd624968b28892d62faaa70883711e2220b') throw Error('Manifesto diferente');
const inventory = [...manifest.transferred, ...manifest.boundaries].map(item => {
    const file = path.resolve(frontend, '..', item.path);
    const actual = sha(readFileSync(file));
    return { ...item, actual, same: actual === item.sha256 };
});
writeFileSync(path.join(area, 'inventario-v03.json'), JSON.stringify({ at: new Date().toISOString(), manifestSha, inventory }, null, 2));
const source = path.join(area, 'diagnostic-source-v03');
if (existsSync(source)) throw Error('Preservar snapshot diagnóstico v03');
const sourceFiles = readdirSync(path.join(frontend, 'src'), { recursive: true, withFileTypes: true }).filter(d => d.isFile()).map(d => path.relative(frontend, path.join(d.parentPath, d.name)));
sourceFiles.push('index.html', 'package.json', 'package-lock.json', 'tsconfig.json', 'vite.config.ts');
const snapshotHashes = sourceFiles.map(file => ({ file, sha256: sha(readFileSync(path.join(frontend, file))) }));
for (const { file } of snapshotHashes) { mkdirSync(path.dirname(path.join(source, file)), { recursive: true }); cpSync(path.join(frontend, file), path.join(source, file)); }
for (const { file, sha256 } of snapshotHashes) if (sha(readFileSync(path.join(source, file))) !== sha256 || sha(readFileSync(path.join(frontend, file))) !== sha256) throw Error('Fonte mudou durante cópia: ' + file);
writeFileSync(path.join(area, 'snapshot-diagnostic-v03.json'), JSON.stringify(snapshotHashes, null, 2));
const config = { root: source, configFile: path.join(frontend, 'vite.config.ts'), cacheDir: path.join(area, 'cache-diagnostic'), server: { host: '127.0.0.1', port: 5191, strictPort: true, open: false } };
const browser = await chromium.launch({ headless: true });
const results = [];
async function capture(mode) {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 }, serviceWorkers: 'block' });
    const page = await context.newPage();
    const blocked = [], consoleErrors = [], responses = [], violations = [];
    await context.route('**/*', route => {
        const url = new URL(route.request().url());
        if (url.origin === 'http://127.0.0.1:5191' && !url.pathname.startsWith('/api')) return route.continue();
        blocked.push(route.request().url()); return route.abort();
    });
    await page.addInitScript(() => {
        window.cssViolations = [];
        document.addEventListener('securitypolicyviolation', e => window.cssViolations.push({ directive: e.effectiveDirective, blockedURI: e.blockedURI, sourceFile: e.sourceFile, lineNumber: e.lineNumber }));
    });
    page.on('console', msg => { if (msg.type() === 'error') consoleErrors.push(msg.text()); });
    page.on('pageerror', err => consoleErrors.push(err.message));
    page.on('response', async r => {
        try { const body = await r.body(); responses.push({ url: r.url(), status: r.status(), contentType: r.headers()['content-type'], sha256: sha(body), bytes: body.length }); } catch {}
    });
    await page.goto('http://127.0.0.1:5191/#coletor');
    await page.waitForTimeout(1500);
    const state = await page.evaluate(() => {
        const css = el => el ? Object.fromEntries(['fontFamily', 'fontSize', 'margin', 'padding', 'display', 'backgroundColor', 'color', 'border', 'outline'].map(k => [k, getComputedStyle(el)[k]])) : null;
        return { url: location.href, text: document.body.innerText, body: css(document.body), header: css(document.querySelector('header')), sheets: [...document.styleSheets].map(s => ({ href: s.href, rules: s.cssRules.length })), inlineScripts: [...document.scripts].filter(s => !s.src).map(s => s.textContent), violations: window.cssViolations };
    });
    const inlineScriptHashes = state.inlineScripts.map(script => ({ script, hash: 'sha256-' + createHash('sha256').update(script).digest('base64') }));
    await page.screenshot({ path: path.join(evidence, `prumo-css-antes-${mode}.png`), fullPage: true });
    results.push({ mode, ...state, inlineScriptHashes, consoleErrors, responses, blocked });
    await context.close();
}
let server;
try {
    server = await createServer(config); await server.listen();
    console.log('DIAGNOSTICO DEV próprio PID', process.pid, 'porta5191');
    await capture('dev'); await server.close(); server = null;
    await build({ ...config, build: { outDir: path.join(area, 'dist-diagnostic'), emptyOutDir: false } });
    server = await preview({ ...config, build: { outDir: path.join(area, 'dist-diagnostic') }, preview: { host: '127.0.0.1', port: 5191, strictPort: true, open: false } });
    console.log('DIAGNOSTICO BUILD próprio PID', process.pid, 'porta5191');
    await capture('build');
} finally { if (server) { if (server.close) await server.close(); else await new Promise(resolve => server.httpServer.close(resolve)); } await browser.close(); writeFileSync(path.join(area, 'diagnostico-v03.json'), JSON.stringify({ at: new Date().toISOString(), pid: process.pid, browserPath: chromium.executablePath(), results }, null, 2)); }
console.log(JSON.stringify(results.map(({ mode, body, header, sheets, violations, blocked, consoleErrors, inlineScriptHashes }) => ({ mode, body, header, sheets, violations, blocked, consoleErrors, hashes: inlineScriptHashes.map(x => x.hash) })), null, 2));
