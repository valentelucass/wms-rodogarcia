const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { EventEmitter } = require('node:events');
const vm = require('node:vm');
const path = require('node:path');
const { createHash } = require('node:crypto');

// Executa o controlador com fronteiras de processo/PM2/FS substituidas, sem SQL ou daemon.
async function scenario(command, fail = '', failure = {}) {
  const calls = [];
  const errors = [];
  let finish;
  const finished = new Promise(resolve => { finish = resolve; });
  const root = path.resolve(__dirname, '../..');
  const runId = '0123456789abcdef0123456789abcdef';
  const releasePath = path.join(root, 'orchestracao/.runtime/launcher-producao/runs', runId, 'release.json');
  const hash = value => createHash('sha256').update(value).digest('hex');
  const source = Buffer.from(JSON.stringify({ files: [] }));
  const backend = { sourceManifest: { path: 'source', sha256: hash(source) } };
  for (const part of ['jar', 'config', 'java']) backend[part] = { path: part, sha256: hash(part) };
  const release = Buffer.from(JSON.stringify({ backend, frontend: { manifestPath: 'manifest' } }));
  const selected = { runId, releasePath, releaseSha256: hash(release) };
  let guarded = false;
  const process = { argv: ['node', 'pm2-control.cjs', command], env: {
    SystemRoot: 'C:/Windows', APPDATA: 'C:/ficticio/app', USERPROFILE: 'C:/ficticio', LOCALAPPDATA: 'C:/ficticio/local',
  }, execPath: 'node', exitCode: 0 };
  const pm2 = {
    connect(cb) { cb(null); },
    list(cb) { cb(null, [{ pm_id: 19, pid: 123, name: 'wms-api-25590', pm2_env: { status: 'online' } }]); },
    disconnect() { setImmediate(finish); },
    stop(...args) { calls.push('stop'); args.at(-1)(null); },
    dump(...args) { calls.push('dump'); args.at(-1)(null); },
  };
  const fakeFs = {
    async mkdir() {}, async unlink() {},
    async open() { return { async writeFile() {}, async close() {} }; },
    async readFile(file) {
      if (file === releasePath) return release.toString();
      if (path.basename(file) === 'prepared.json') return JSON.stringify(selected);
      if (/pre-update-guard-/.test(file)) return JSON.stringify({ guardaRealAprovada: fail !== 'guard' });
      if (file === 'source') return source.toString();
      if (['jar','config','java'].includes(file)) return Buffer.from(file + (guarded && fail === 'source' ? 'CHANGED' : ''));
      throw Error('UNEXPECTED_ARTIFACT_READ');
    },
  };
  const mockRequire = id => {
    if (id === 'node:fs/promises') return fakeFs;
    if (id === 'node:child_process') return { spawn(_exe, args) {
      const name = path.basename(args[args.indexOf('-File') + 1]); calls.push(name);
      const child = new EventEmitter(); child.stdout = new EventEmitter(); child.stderr = { resume() {} };
      setImmediate(() => {
        if (name === 'preparar-ambiente.ps1') {
          assert.deepEqual([...args.slice(-4)], ['-Ambiente','PROD','-Modo','Apply']);
          child.stdout.emit('data', Buffer.from(JSON.stringify({ estado: fail === 'preparation' ? 'INTERROMPIDO' : 'AMBIENTE_ATUALIZADO', ...failure })));
          child.emit('exit', fail === 'preparation' ? 20 : 0);
        } else if (name === 'iniciar-prod.ps1') child.emit('exit', fail === 'build' ? 20 : 0);
        else if (name === 'guarda-wmsprod.ps1') { guarded = true; child.emit('exit', 0); }
        else throw Error('UNEXPECTED_HELPER');
      });
      return child;
    } };
    if (id === 'node:net') return { connect() { const socket = new EventEmitter(); socket.setTimeout = socket.destroy = () => {}; setImmediate(() => socket.emit('connect')); return socket; } };
    if (id === './pm2-ownership.cjs') return { ownApps: apps => apps };
    if (id.replace(/\\/g, '/').endsWith('node_modules/pm2')) return pm2;
    if (id.endsWith('ecosystem.config.js')) return { apps: [{ name: 'wms-api-25590' }, { name: 'wms-ui-25591' }] };
    return require(id);
  };
  vm.runInNewContext(readFileSync(path.join(__dirname, 'pm2-control.cjs'), 'utf8'), {
    require: mockRequire, __dirname, __filename: path.join(__dirname, 'pm2-control.cjs'), process,
    console: { log() {}, error(value) { errors.push(String(value)); }, table() {} }, setTimeout, clearTimeout,
  }, { importModuleDynamically: () => import('data:text/javascript,export async function verifyBuild(){return {manifest:{inputs:[]}}}') });
  await finished;
  return { calls, errors, exitCode: process.exitCode };
}

test('build falho preserva producao e nao abre preparo do banco', async () => {
  const result = await scenario('atualizar', 'build');
  assert.deepEqual(result.calls, ['iniciar-prod.ps1']);
  assert.equal(result.exitCode, 20);
});
test('migration ou grant recusado preserva instancia e nao invoca guarda final', async () => {
  const result = await scenario('atualizar', 'preparation');
  assert.deepEqual(result.calls, ['iniciar-prod.ps1', 'preparar-ambiente.ps1']);
  assert.equal(result.exitCode, 20);
});
test('consulta de status nao aplica permissoes nem altera processos', async () => {
  const result = await scenario('status');
  assert.deepEqual(result.calls, []);
  assert.equal(result.exitCode, 0);
});

test('recusa de migrations informa causa controlada e recibo do projeto sem parar producao', async () => {
  const receipt = path.resolve(__dirname, '../../orchestracao/.runtime/preparo-ambiente/ficticio/resultado.json');
  const result = await scenario('atualizar', 'preparation', { recibo: receipt, codigo: 'D21_FONTE_PARSER_NAO_SUPORTADA', mensagem: 'NAO_PUBLICAR_DADO_BRUTO' });
  assert.deepEqual(result.calls, ['iniciar-prod.ps1','preparar-ambiente.ps1']);
  assert.equal(result.exitCode, 20);
  assert.ok(result.errors.some(line => line.includes('D21_FONTE_PARSER_NAO_SUPORTADA')));
  assert.ok(result.errors.some(line => line.includes(receipt)));
  assert.ok(!result.errors.join('\n').includes('NAO_PUBLICAR_DADO_BRUTO'));
});

test('recibo fora da area propria e erro bruto nao sao publicados', async () => {
  const result = await scenario('atualizar', 'preparation', { recibo: path.resolve(__dirname, '../../outro-projeto'), codigo: 'D21_FALHA' });
  assert.equal(result.errors.length, 1);
  assert.equal(result.errors[0], 'PROD_PM2_PREPARAR_AMBIENTE_REFUSED_20');
});

test('guarda final recusada nao interrompe instancia', async () => {
  const r = await scenario('atualizar','guard');
  assert.deepEqual(r.calls,['iniciar-prod.ps1','preparar-ambiente.ps1','guarda-wmsprod.ps1']);
  assert.equal(r.exitCode,20);
});
test('fonte alterada durante preparo impede parada', async () => {
  const r = await scenario('atualizar','source');
  assert.ok(!r.calls.includes('stop')); assert.equal(r.errors[0],'PROD_PM2_ARTIFACT_CHANGED');
});
test('iniciar tambem prepara fonte atual em vez de selecionar pacote anterior', async () => {
  const r = await scenario('iniciar','preparation');
  assert.deepEqual(r.calls,['iniciar-prod.ps1','preparar-ambiente.ps1']);
  assert.equal(r.exitCode,20);
});
