const { spawn } = require("node:child_process");
const { mkdirSync, writeFileSync } = require("node:fs");
const path = require("node:path");
const { randomUUID } = require("node:crypto");
const root = path.resolve(__dirname, "../..");
const component = process.argv[2];
if (!["api", "ui"].includes(component)) process.exit(20);
const control = path.join(root, "orchestracao/.runtime/launcher-producao/pm2", `${component}-${randomUUID()}`);
mkdirSync(control, { recursive: true });
const env = {};
for (const key of ["PATH", "SystemRoot", "WINDIR", "TEMP", "TMP", "COMSPEC", "PATHEXT", "LOCALAPPDATA", "APPDATA", "USERPROFILE", "COMPUTERNAME", "NUMBER_OF_PROCESSORS", "PROCESSOR_ARCHITECTURE"])
  if (process.env[key]) env[key] = process.env[key];
let stopping = false;
let ready = false;
function stop() {
  if (stopping) return;
  stopping = true;
  writeFileSync(path.join(control, "stop.request"), "STOP");
}
process.on("message", (message) => { if (message === "shutdown") stop(); });
process.on("SIGINT", stop);
process.on("SIGTERM", stop);
process.on("disconnect", stop);
const child = spawn(path.join(env.SystemRoot, "System32/WindowsPowerShell/v1.0/powershell.exe"), [
  "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", path.join(__dirname, "pm2-component.ps1"),
  "-Component", component, "-ControlDirectory", control, "-WrapperPid", String(process.pid),
], { cwd: root, windowsHide: true, env, stdio: ["ignore", "pipe", "pipe"] });
let buffer = "";
child.stdout.on("data", (chunk) => {
  buffer = (buffer + chunk.toString()).slice(-8192);
  const lines = buffer.split(/\r?\n/); buffer = lines.pop();
  for (const line of lines) {
    if (line === "WMS_PM2_READY") {
      ready = true;
      if (!stopping && process.send) process.send("ready");
      console.log(`[WMS PM2] ${component} disponivel.`);
    } else if (/^WMS_PM2_BLOCKED PROD_[A-Z0-9_]+$/.test(line)) console.error(line);
  }
});
// Saída bruta do PowerShell/backend nunca vai para logs do PM2.
child.stderr.resume();
child.on("error", () => { console.error("WMS_PM2_BLOCKED PROD_WORKER_NOT_STARTED"); process.exitCode = 20; });
child.on("exit", (code) => {
  console.log(`[WMS PM2] ${component} encerrado (${stopping ? "solicitado" : code ?? "inesperado"}).`);
  process.exit(stopping ? 0 : code === 20 || !ready ? 20 : 1);
});
