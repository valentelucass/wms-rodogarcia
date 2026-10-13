const { test } = require('node:test');
const assert = require('node:assert/strict');
const path = require('node:path');
const { ownApps } = require('./pm2-ownership.cjs');
const root = path.resolve(__dirname, '../..');
const names = ['wms-api-25590', 'wms-ui-25591'];
const app = { name:names[0], pm_id:21, pm2_env:{pm_cwd:root, pm_exec_path:path.join(root,'infra/prod/pm2-worker.cjs')} };
test('PM2 principal filtra outros projetos sem selecionar seus IDs', () => {
  const foreign = {name:'outro-projeto',pm_id:1};
  assert.deepEqual(ownApps([foreign,app],root,names),[app]);
  assert.deepEqual(foreign,{name:'outro-projeto',pm_id:1});
});
test('mesmo nome com origem diferente bloqueia operação', () => {
  assert.throws(()=>ownApps([{...app,pm2_env:{...app.pm2_env,pm_cwd:path.dirname(root)}}],root,names),/FOREIGN/);
});
test('duplicidade ou ID ausente não permite parada ambígua', () => {
  assert.throws(()=>ownApps([app,{...app,pm_id:22}],root,names),/FOREIGN/);
  assert.throws(()=>ownApps([{...app,pm_id:undefined}],root,names),/FOREIGN/);
});
test('limpeza do PM2 legado recusa qualquer projeto alheio', () => {
  assert.throws(()=>ownApps([app,{name:'outro'}],root,names,true),/FOREIGN/);
});
