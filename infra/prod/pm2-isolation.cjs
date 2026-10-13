// PM2 7.0.4 no Windows fixa rpc.sock/pub.sock mesmo com PM2_HOME distinto.
// Ajuste somente em memoria nos processos WMS; nenhum arquivo do PM2 instalado muda.
const path = require('node:path');
const { createHash } = require('node:crypto');
const home = path.join(process.env.LOCALAPPDATA, 'Rodogarcia/WMS/pm2-prod');
if (path.resolve(process.env.PM2_HOME || '').toLowerCase() !== home.toLowerCase())
  throw new Error('PROD_PM2_HOME_INVALID');
const moduleRoot = path.join(process.env.APPDATA, 'npm/node_modules/pm2');
if (require(path.join(moduleRoot, 'package.json')).version !== '7.0.4')
  throw new Error('PROD_PM2_VERSION_REQUIRES_REVIEW');
const pathsFile = require.resolve(path.join(moduleRoot, 'paths.js'));
const original = require(pathsFile);
const id = createHash('sha256').update(home.toLowerCase()).digest('hex').slice(0, 16);
require.cache[pathsFile].exports = (...args) => ({
  ...original(...args),
  DAEMON_RPC_PORT: `\\\\.\\pipe\\wms-prod-${id}-rpc`,
  DAEMON_PUB_PORT: `\\\\.\\pipe\\wms-prod-${id}-pub`,
  INTERACTOR_RPC_PORT: `\\\\.\\pipe\\wms-prod-${id}-interactor`,
});
