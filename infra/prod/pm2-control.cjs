const path = require("node:path");
const fs = require("node:fs/promises");
const { spawn } = require("node:child_process");
const { createHash, randomUUID } = require("node:crypto");
const { pathToFileURL } = require("node:url");
const net = require('node:net');
const { ownApps } = require('./pm2-ownership.cjs');
const root = path.resolve(__dirname, "../..");
const area = path.join(root, "orchestracao/.runtime/launcher-producao/pm2");
const command = process.argv[2] || "atualizar";
const isolated = process.argv[3] === '--isolado';
if (!["atualizar", "iniciar", "parar", "status", "logs", "migrar", "retirar-legado"].includes(command) ||
    process.argv.length > (isolated ? 4 : 3) || (command === 'retirar-legado' && !isolated) || (command === 'migrar' && isolated)) {
  console.log("Uso: pm2-wms.bat [atualizar|iniciar|parar|status|logs|migrar]"); process.exit(2);
}
// O daemon/dump do WMS não recebem variáveis de credenciais de outro ambiente.
const allowed = new Set(["PATH", "SYSTEMROOT", "WINDIR", "TEMP", "TMP", "COMSPEC", "PATHEXT", "LOCALAPPDATA", "APPDATA", "USERPROFILE", "COMPUTERNAME", "NUMBER_OF_PROCESSORS", "PROCESSOR_ARCHITECTURE", "HOMEDRIVE", "HOMEPATH"]);
for (const key of Object.keys(process.env)) if (!allowed.has(key.toUpperCase())) delete process.env[key];
process.env.PM2_HOME = isolated ? path.join(process.env.LOCALAPPDATA, "Rodogarcia/WMS/pm2-prod") : path.join(process.env.USERPROFILE, '.pm2');
if (isolated) process.env.NODE_OPTIONS = '--require "' + path.join(__dirname, 'pm2-isolation.cjs').replace(/\\/g, '/') + '"';
process.env.PM2_NO_INTERACTION = 'true';
if (isolated) require('./pm2-isolation.cjs');
const pm2 = require(path.join(process.env.APPDATA, "npm/node_modules/pm2"));
const config = require(path.join(root, "ecosystem.config.js"));
const names = config.apps.map(a => a.name);
const invoke = (method, ...args) => new Promise((resolve, reject) => {
  const timer = setTimeout(() => reject(new Error("PROD_PM2_" + method.toUpperCase() + "_TIMEOUT")), ['start', 'stop', 'delete'].includes(method) ? 310000 : 20000);
  pm2[method](...args, (err, value) => {
    clearTimeout(timer);
    err ? reject(new Error("PROD_PM2_" + method.toUpperCase() + "_FAILED")) : resolve(value);
  });
});
const hash = bytes => createHash("sha256").update(bytes).digest("hex");
const json = async file => JSON.parse((await fs.readFile(file, "utf8")).replace(/^\uFEFF/, ""));
let operationLock;
const lockPath = path.join(area, isolated ? 'isolated-operation.lock' : 'operation.lock');
async function ps(file, args = [], quiet = false) {
  return new Promise((resolve, reject) => {
    const child = spawn(path.join(process.env.SystemRoot, "System32/WindowsPowerShell/v1.0/powershell.exe"),
      ["-NoProfile", "-ExecutionPolicy", "Bypass", "-File", path.join(__dirname, file), ...args],
      { cwd: root, windowsHide: true, stdio: ["ignore", "pipe", "pipe"] });
    let output = "";
    child.stdout.on("data", chunk => {
      if (!quiet) for (const line of chunk.toString().split(/\r?\n/))
        if (line.startsWith("[WMS PROD]") || /^PROD_[A-Z0-9_]+$/.test(line)) console.log(line);
      output = (output + chunk.toString()).slice(-4096);
    });
    child.stderr.resume();
    child.on("error", () => reject(new Error("PROD_PM2_HELPER_NOT_STARTED")));
    child.on("exit", code => {
      const reason = output.split(/\r?\n/).findLast(line => /^PROD_[A-Z0-9_]+$/.test(line));
      if (code === 0) resolve(output);
      else {
        const error = new Error(reason || 'PROD_PM2_'+path.basename(file,'.ps1').replace(/-/g,'_').toUpperCase()+'_REFUSED_'+code);
        if (path.basename(file) === 'aplicar-permissoes-prod.ps1') {
          try {
            const receipt = JSON.parse(output.replace(/^\uFEFF/, ''));
            const receiptPath = typeof receipt.recibo === 'string' ? path.resolve(receipt.recibo) : '';
            const relative = path.relative(path.join(root, 'orchestracao/.runtime/cadastros-etiqueta-20261014'), receiptPath);
            if (receipt.estado === 'INTERROMPIDO' && /^(CAD|SQL|PROD)_[A-Z0-9_]+$/.test(receipt.erro?.codigo) &&
                receiptPath && !/[\r\n\0]/.test(receiptPath) && relative && !relative.startsWith('..') && !path.isAbsolute(relative)) {
              error.controlledCode = receipt.erro.codigo;
              error.receiptPath = path.join(receiptPath, 'resultado.json');
            }
          } catch { /* Mensagem bruta do helper continua privada. */ }
        }
        reject(error);
      }
    });
  });
}
async function list() {
  const apps = await invoke("list");
  return ownApps(apps, root, names, isolated);
}
async function show() {
  const apps = await list();
  console.table(apps.map(a => ({ nome: a.name, estado: a.pm2_env.status, supervisorPid: a.pid, reinicios: a.pm2_env.restart_time })));
}
async function stopApps(apps) {
  for (const name of [...names].reverse()) {
    const app = apps.find(a => a.name === name);
    if (app) await invoke("stop", app.pm_id);
  }
}
async function startSelected() {
  const apps = await list();
  for (const app of config.apps) {
    const existing = apps.find(a => a.name === app.name);
    if (existing?.pm2_env.status === "online") continue;
    // Recria somente a entrada própria parada para consumir alterações do ecosystem.
    if (existing) await invoke("delete", existing.pm_id);
    console.log(`[WMS PM2] Iniciando ${app.name}...`);
    await invoke("start", app);
    const component = app.args[0];
    const current = await json(path.join(area, `${component}-current.json`));
    const selected = await json(path.join(area, "selected.json"));
    const active = (await list()).find(a => a.name === app.name);
    if (active?.pm2_env.status !== "online" || current.wrapperPid !== active.pid || current.releaseRunId !== selected.runId)
      throw new Error("PROD_PM2_READINESS_UNCONFIRMED");
  }
  await invoke("dump");
  console.log('[WMS PM2] Lista salva (pm2 save).');
  await show();
}
async function main() {
  await fs.mkdir(area, { recursive: true });
  await fs.mkdir(path.join(root, "orchestracao/.runtime/pm2/logs"), { recursive: true });
  if (!["status", "logs"].includes(command)) {
    try { operationLock = await fs.open(lockPath, "wx"); }
    catch { throw new Error("PROD_PM2_OPERATION_BUSY"); }
    await operationLock.writeFile(JSON.stringify({ pid: process.pid, startedUtc: new Date().toISOString(), command }));
  }
  // Recusa acesso negado antes do cliente PM2 tentar iniciar outro daemon.
  if (!isolated) await new Promise((resolve, reject) => {
    const socket = net.connect('\\\\.\\pipe\\rpc.sock');
    socket.setTimeout(5000);
    socket.once('connect', () => { socket.destroy(); resolve(); });
    socket.once('error', () => { socket.destroy(); reject(new Error('PROD_PM2_MAIN_ACCESS_REQUIRED')); });
    socket.once('timeout', () => { socket.destroy(); reject(new Error('PROD_PM2_MAIN_ACCESS_REQUIRED')); });
  });
  await invoke("connect");
  const apps = await list();
  if (command === "status") return show();
  if (command === "logs") {
    for (const app of config.apps) for (const file of [app.out_file, app.error_file]) {
      const content = await fs.readFile(file, "utf8").catch(() => "");
      console.log(path.basename(file)+"\n"+content.split(/\r?\n/).slice(-20).join("\n"));
    }
    return;
  }
  if (command === "parar") { await stopApps(apps); await invoke("dump"); return show(); }
  if (command === 'retirar-legado') {
    await stopApps(apps);
    for (const app of await list()) await invoke('delete', app.pm_id);
    await invoke('dump', true);
    return;
  }
  if (command === 'migrar') {
    if (apps.length) throw new Error('PROD_PM2_MAIN_ALREADY_REGISTERED');
    const before = (await invoke('list')).map(a => ({id:a.pm_id,name:a.name,pid:a.pid,status:a.pm2_env.status,restarts:a.pm2_env.restart_time}));
    const childPids = before.filter(a => a.pid > 0 && a.status === 'online').map(a=>a.pid);
    if (!childPids.length) throw new Error('PROD_PM2_MAIN_NO_LIVE_CHILD');
    await ps('pm2-main-check.ps1',['-KnownChildPids',childPids.join(',')]);
    const migrationPath = path.join(area, 'main-migration.json');
    await fs.writeFile(migrationPath, JSON.stringify({startedUtc:new Date().toISOString(),before},null,2));
    const guardPath = path.join(area,'pre-migration-guard-'+randomUUID()+'.json');
    await ps('guarda-wmsprod.ps1',['-EvidencePath',guardPath],true);
    if (!(await json(guardPath)).guardaRealAprovada) throw new Error('PROD_CURRENT_GUARD_BLOCKED');
    await new Promise((resolve,reject) => {
      const child = spawn(process.execPath,[__filename,'retirar-legado','--isolado'],{cwd:root,windowsHide:true,stdio:'inherit'});
      child.once('error',()=>reject(new Error('PROD_PM2_LEGACY_STOP_FAILED')));
      child.once('exit',code=>code===0?resolve():reject(new Error('PROD_PM2_LEGACY_STOP_FAILED')));
    });
    await startSelected();
    const after = (await invoke('list')).map(a => ({id:a.pm_id,name:a.name,pid:a.pid,status:a.pm2_env.status,restarts:a.pm2_env.restart_time}));
    await fs.writeFile(migrationPath, JSON.stringify({finishedUtc:new Date().toISOString(),guardPath,before,after,saved:true},null,2));
    console.log('[WMS PM2] Transferencia para PM2 principal concluida; pm2 save executado.');
    return;
  }
  if (command === "iniciar") return startSelected();
  console.log("[WMS PM2] Preparando a fonte atual antes de substituir a versao ativa.");
  // CAD-EDIT01: somente a atualizacao explicita do operador aplica o delta restrito.
  // A guarda preliminar nao libera a aplicacao; a guarda normal continua obrigatoria.
  if (command === "atualizar") {
    console.log("[WMS PM2] Conferindo as permissoes das correcoes cadastrais em WMS_PROD.");
    const result = JSON.parse((await ps("../cadastros/aplicar-permissoes-prod.ps1", ["-Modo", "Apply"], true)).replace(/^\uFEFF/, ""));
    if (result.estado !== "DIREITOS_PROD_APLICADOS_E_CONFERIDOS") throw new Error("PROD_CAD_EDIT_NOT_CONFIRMED");
  }
  await ps("iniciar-prod.ps1", ["-PrepareOnly", "-PrepareForPm2"]);
  const selected = await json(path.join(area, "prepared.json"));
  const expected = path.join(root, "orchestracao/.runtime/launcher-producao/runs", selected.runId, "release.json");
  if (!/^[a-f0-9]{32}$/.test(selected.runId) || path.resolve(selected.releasePath) !== expected ||
      hash(await fs.readFile(expected)).toLowerCase() !== selected.releaseSha256.toLowerCase()) throw new Error("PROD_PM2_PREPARED_INVALID");
  const release = await json(expected);
  const { verifyBuild } = await import(pathToFileURL(path.join(root, "frontend/tools/prod-server.mjs")).href);
  const build = await verifyBuild(path.dirname(release.frontend.manifestPath), release.frontend.manifestPath);
  for (const item of build.manifest.inputs)
    if (hash(await fs.readFile(path.join(root, item.sourcePath))) !== item.sha256) throw new Error("PROD_PM2_SOURCE_CHANGED");
  for (const item of [release.backend.jar, release.backend.config, release.backend.java, release.backend.sourceManifest])
    if (hash(await fs.readFile(item.path)) !== item.sha256.toLowerCase()) throw new Error("PROD_PM2_ARTIFACT_CHANGED");
  const source = await json(release.backend.sourceManifest.path);
  for (const item of source.files)
    if (hash(await fs.readFile(path.join(root, "backend", item.path))) !== item.sha256) throw new Error("PROD_PM2_SOURCE_CHANGED");
  const guardPath = path.join(area, "pre-update-guard-"+randomUUID()+".json");
  console.log("[WMS PM2] Conferindo ambiente antes da interrupcao.");
  await ps("guarda-wmsprod.ps1", ["-EvidencePath", guardPath], true);
  const guard = await json(guardPath);
  if (!guard.guardaRealAprovada) throw new Error("PROD_CURRENT_GUARD_BLOCKED");
  if (!apps.some(a => a.pm2_env.status === "online")) await ps("pm2-stop-standalone.ps1", ["-InspectOnly"]);
  await stopApps(apps);
  await ps("pm2-stop-standalone.ps1");
  const temp = path.join(area, "selected-"+randomUUID()+".tmp");
  await fs.writeFile(temp, JSON.stringify(selected, null, 2));
  await fs.rename(temp, path.join(area, "selected.json"));
  await startSelected();
  console.log("[WMS PM2] API e UI disponiveis. Pode fechar este terminal.");
  console.log("[WMS PM2] Acesso seguro: https://wms.rodogarcia.com.br");
}
main().catch(err => {
  console.error(/^PROD_[A-Z0-9_]+$/.test(err.message) ? err.message : "PROD_PM2_CONTROL_FAILED");
  if (err.controlledCode && err.receiptPath) {
    console.error('[WMS PM2] Motivo: '+err.controlledCode);
    if (err.controlledCode === 'CAD_MIGRATIONS_INESPERADAS' || err.controlledCode === 'CAD_MIGRATIONS_DIVERGENTES')
      console.error('[WMS PM2] O contrato de migrations nao corresponde a fonte atual. Conclua a ativacao da migration pendente (V12) e a atualizacao das guardas antes de publicar. Nenhum processo WMS foi parado.');
    console.error('[WMS PM2] Recibo: '+err.receiptPath);
  }
  if (err.message === 'PROD_PM2_MAIN_ACCESS_REQUIRED')
    console.error('[WMS PM2] Execute este BAT no terminal administrador onde pm2 ls funciona. Nenhum processo foi parado.');
  process.exitCode = 20;
}).finally(async () => {
  pm2.disconnect();
  if (operationLock) { await operationLock.close(); await fs.unlink(lockPath); }
});
