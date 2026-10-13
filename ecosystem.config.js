const path = require("node:path");

// PM2 principal: operar com pm2-wms.bat no mesmo contexto administrativo do daemon.
const root = __dirname;
const logs = path.join(root, "orchestracao", ".runtime", "pm2", "logs");
module.exports = {
  apps: [
    { name: "wms-api-25590", component: "api" },
    { name: "wms-ui-25591", component: "ui" },
  ].map(({ name, component }) => ({
    name,
    namespace: "wms-prod",
    cwd: root,
    script: path.join(root, "infra", "prod", "pm2-worker.cjs"),
    args: [component],
    interpreter: process.execPath,
    exec_mode: "fork",
    instances: 1,
    autorestart: true,
    restart_delay: 5000,
    min_uptime: 120000,
    max_restarts: 3,
    // Falha de guarda/configuração não pode provocar novas tentativas SQL.
    stop_exit_codes: [0, 20],
    wait_ready: true,
    listen_timeout: 300000,
    shutdown_with_message: true,
    kill_timeout: 300000,
    treekill: false,
    windowsHide: true,
    watch: false,
    out_file: path.join(logs, `${name}-out.log`),
    error_file: path.join(logs, `${name}-error.log`),
    log_date_format: "YYYY-MM-DD HH:mm:ss.SSS",
    env: { NODE_ENV: "production", LANG: "pt_BR.UTF-8", LC_ALL: "pt_BR.UTF-8" },
  })),
};
