const {test} = require('node:test');
const assert = require('node:assert/strict');
const net = require('node:net');
const path = require('node:path');
const {spawn} = require('node:child_process');
const {once} = require('node:events');
const {randomUUID} = require('node:crypto');

test('identifica dono real do pipe pelo filho sem exigir Daemon.js no comando', {timeout:30000}, async () => {
  const pipeName='wms-pm2-test-'+randomUUID();
  const server=net.createServer(socket=>socket.on('error',()=>{}));
  server.listen('\\\\.\\pipe\\'+pipeName);
  await once(server,'listening');
  const child=spawn(process.execPath,['-e','setInterval(()=>{},1000)'],{windowsHide:true,stdio:'ignore'});
  await once(child,'spawn');
  async function check(pid) {
    const helper=spawn(path.join(process.env.SystemRoot,'System32/WindowsPowerShell/v1.0/powershell.exe'),
      ['-NoProfile','-ExecutionPolicy','Bypass','-File',path.join(__dirname,'pm2-main-check.ps1'),
       '-KnownChildPids',String(pid),'-PipeName',pipeName,'-ReadOnly'],{windowsHide:true});
    let output='';
    helper.stdout.on('data',c=>output+=c);
    helper.stderr.on('data',c=>output+=c);
    const [code]=await once(helper,'exit');
    return {code,output};
  }
  try {
    const accepted=await check(child.pid);
    assert.equal(accepted.code,0,accepted.output);
    assert.match(accepted.output,/PROD_PM2_MAIN_OWNER_CONFIRMED/);
    const refused=await check(process.pid);
    assert.equal(refused.code,20,refused.output);
    assert.match(refused.output,/PROD_PM2_MAIN_OWNER_UNCONFIRMED/);
  } finally {
    child.kill();
    server.close();
  }
});
