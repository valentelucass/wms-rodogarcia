const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs/promises');
const path = require('node:path');
const { fork } = require('node:child_process');
const { once } = require('node:events');
const root = path.resolve(__dirname, '../..');

async function fixture(body) {
  const base = path.join(root, 'orchestracao/.runtime/pm2-worker-tests');
  await fs.mkdir(base, { recursive: true });
  const dir = await fs.mkdtemp(path.join(base, 'case-'));
  const scripts = path.join(dir, 'infra/prod');
  await fs.mkdir(scripts, { recursive: true });
  await fs.copyFile(path.join(__dirname, 'pm2-worker.cjs'), path.join(scripts, 'pm2-worker.cjs'));
  await fs.writeFile(path.join(scripts, 'pm2-component.ps1'),
    'param([string]$Component,[string]$ControlDirectory,[int]$WrapperPid)\n'+body);
  const child = fork(path.join(scripts, 'pm2-worker.cjs'), ['api'], { cwd: dir, windowsHide: true, silent: true });
  child.stdout.resume(); child.stderr.resume();
  return { dir, child };
}

test('supervisor sinaliza ready e aguarda parada pelo protocolo Windows do PM2', { timeout: 15000 }, async () => {
  const f = await fixture(`Write-Output 'WMS_PM2_READY'
while(-not (Test-Path -LiteralPath (Join-Path $ControlDirectory 'stop.request'))){Start-Sleep -Milliseconds 50}
[IO.File]::WriteAllText((Join-Path $ControlDirectory 'stopped.txt'),'CLEANUP_FINISHED')
exit 0`);
  try {
    const [message] = await once(f.child, 'message');
    assert.equal(message, 'ready');
    const exited = once(f.child, 'exit');
    f.child.send('shutdown');
    const [code] = await exited;
    assert.equal(code, 0);
    const runs = path.join(f.dir, 'orchestracao/.runtime/launcher-producao/pm2');
    const [run] = await fs.readdir(runs);
    assert.equal(await fs.readFile(path.join(runs, run, 'stopped.txt'), 'utf8'), 'CLEANUP_FINISHED');
  } finally { if(f.child.exitCode === null) f.child.send('shutdown'); }
});

test('falha de preflight sai com codigo que PM2 nao reinicia; sem ready enganoso', { timeout: 15000 }, async () => {
  const f = await fixture("Write-Output 'WMS_PM2_BLOCKED PROD_CURRENT_GUARD_BLOCKED'\nexit 20");
  const messages = []; f.child.on('message', m => messages.push(m));
  const [code] = await once(f.child, 'exit');
  assert.equal(code, 20);
  assert.deepEqual(messages, []);
  const ecosystem = require(path.join(root, 'ecosystem.config.js'));
  assert.deepEqual(ecosystem.apps.map(a => a.name), ['wms-api-25590', 'wms-ui-25591']);
  for (const app of ecosystem.apps) {
    assert.ok(app.stop_exit_codes.includes(code));
    assert.equal(app.shutdown_with_message, true);
    assert.equal(app.treekill, false);
    assert.equal(app.instances, 1);
  }
});
