const path = require('node:path');

function ownApps(apps, root, names, isolated = false) {
  const own = [];
  const same = (a, b) => typeof a === 'string' && path.resolve(a).toLowerCase() === path.resolve(b).toLowerCase();
  for (const app of apps) {
    if (!names.includes(app.name)) {
      if (isolated) throw new Error('PROD_PM2_FOREIGN_APP_REFUSED');
      continue;
    }
    if (own.some(a => a.name === app.name) || !same(app.pm2_env?.pm_cwd, root) ||
        !same(app.pm2_env?.pm_exec_path, path.join(root, 'infra/prod/pm2-worker.cjs')) ||
        !Number.isInteger(app.pm_id)) throw new Error('PROD_PM2_FOREIGN_APP_REFUSED');
    own.push(app);
  }
  return own;
}
module.exports = { ownApps };
