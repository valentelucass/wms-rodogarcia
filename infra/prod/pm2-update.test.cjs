const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { EventEmitter } = require('node:events');
const vm = require('node:vm');
const path = require('node:path');

// Executa o controlador com fronteiras de processo/PM2/FS substituidas, sem SQL ou daemon.
async function scenario(command, grantResult) {
  const calls = [];
  let finish;
  const finished = new Promise(resolve => { finish = resolve; });
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
    async readFile() { throw Error('UNEXPECTED_ARTIFACT_READ'); },
  };
  const mockRequire = id => {
    if (id === 'node:fs/promises') return fakeFs;
    if (id === 'node:child_process') return { spawn(_exe, args) {
      const name = path.basename(args[args.indexOf('-File') + 1]); calls.push(name);
      const child = new EventEmitter(); child.stdout = new EventEmitter(); child.stderr = { resume() {} };
      setImmediate(() => {
        if (name === 'aplicar-permissoes-prod.ps1') {
          child.stdout.emit('data', Buffer.from(JSON.stringify({ estado: grantResult ? 'DIREITOS_PROD_APLICADOS_E_CONFERIDOS' : 'INTERROMPIDO' })));
          child.emit('exit', grantResult ? 0 : 20);
        } else if (name === 'iniciar-prod.ps1') child.emit('exit', 20);
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
    console: { log() {}, error() {}, table() {} }, setTimeout, clearTimeout,
  });
  await finished;
  return { calls, exitCode: process.exitCode };
}

test('permissoes recusadas impedem preparo e parada da producao', async () => {
  const result = await scenario('atualizar', false);
  assert.deepEqual(result.calls, ['aplicar-permissoes-prod.ps1']);
  assert.equal(result.exitCode, 20);
});
test('preparo falho apos permissoes nao interrompe a instancia existente', async () => {
  const result = await scenario('atualizar', true);
  assert.deepEqual(result.calls, ['aplicar-permissoes-prod.ps1', 'iniciar-prod.ps1']);
  assert.equal(result.exitCode, 20);
});
test('consulta de status nao aplica permissoes nem altera processos', async () => {
  const result = await scenario('status', true);
  assert.deepEqual(result.calls, []);
  assert.equal(result.exitCode, 0);
});
