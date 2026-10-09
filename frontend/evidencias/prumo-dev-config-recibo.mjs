import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
const evidence=import.meta.dirname;
const frontend=path.resolve(evidence,'..');
const sha=data=>createHash('sha256').update(data).digest('hex');
const jsonPath=path.join(evidence,'frontend-prumo-dev-config-lucas-20261008.json');
const report=JSON.parse(readFileSync(jsonPath));
if(report.result!=='PASS'||!report.ownedSame||!report.originalConfigPreserved||!report.cssReceiptPreserved||!report.guardReceiptPreserved)throw Error('Delta sem prova preservada');
report.effectiveSources=[...new Map(report.sources.map(s=>[s.path,s])).values()];
for(const s of report.effectiveSources)if(sha(readFileSync(path.join(frontend,report.source,s.path)))!==s.sha256)throw Error('Snapshot final diverge: '+s.path);
for(const s of report.owned)if(sha(readFileSync(path.join(frontend,s.path)))!==s.sha256)throw Error('Posse mudou apos prova: '+s.path);
report.sourceComposition='closure r08 conferido contra manifesto47214BF3, sobreposto com oito arquivos CSS/entrypoint congelados; somente dois novos arquivos Prumo e copia byte a byte da guarda bloqueada9135BC92. Fontes efetivas discriminadas; nenhum recibo de guarda ficticio criado.';
report.typecheck={scope:'somente vite.dev.config.ts',command:'node node_modules/typescript/bin/tsc --ignoreConfig --noEmit --target ES2022 --module ESNext --moduleResolution Bundler --types node --skipLibCheck vite.dev.config.ts',exitCode:0,log:'evidencias/prumo-dev-config-typecheck-r02.log',previousCliRefusal:{code:'TS5112',exitCode:1,log:'evidencias/prumo-dev-config-typecheck.log',reason:'TypeScript6 exige --ignoreConfig quando arquivo e fornecido; primeira chamada nao carregou projeto nem diagnosticou fonte'}};
report.preparedContract={scriptForLume:'vite --config vite.dev.config.ts',frontendDefault:'http://127.0.0.1:5178',actualProofUrl:'http://127.0.0.1:5191/#coletor (processo proprio encerrado)',dataMode:'ficticio',backendPreparedOnly:'http://127.0.0.1:8080 / WMS_PORT contratado por Cedro; nenhum processo observado ou sonda',backendOverride:'WMS_DEV_BACKEND_URL somente HTTP127.0.0.1:porta sem path/userinfo/query/hash',plannedProxyPrefix:'/api',rewrite:false,changeOrigin:false,activeProxy:false,sameOriginPaths:'/api/v1/... preservados',guardBeforeServer:'real recusa G01; true isolado tambem nao implementa liberacao',futureGuardImplemented:false};
report.limits=[
    'Somente dois arquivos novos em posse Prumo: vite.dev.config.ts e docs/desenvolvimento-dev.md. Package/App/sessao/cliente/CSS/original vite.config.ts permanecem fora dessa alteracao.',
    'G01 permanece bloqueado; nenhuma leitura DPAPI/credencial/token, sonda SQL, chamada API/backend ou fixture real. Middleware foi exercitado como funcao local sem HTTP; proxy nunca foi associado ao servidor.',
    'Checks focais da configuracao com Vite loader, import do contrato preparado e tipagem somente desse arquivo; um bootstrap/consulta ficticia em copia r08+CSS freeze. Sem build/rebuild/suite geral ou repeticao de backend/infra.',
    'Endereco5191 serviu apenas a prova ficticia e esta fechado. Processo/configuracao existentes5178 nao foram reiniciados/tocados. Backend8080 e contrato preparado Cedro, nao processo observado.',
    'Nova guarda futura nao inventada nem implementada: evidencia atual de resolucao segura e nova guarda inteira precisam de avaliacao e liberacao Farol antes de revisar o caminho real. Booleano/listener/preflight antigo nao ativam proxy.',
    'Farol encaminhou contrato a Lume; Lume integra o script/mode real quando liberado e inicia frontend atual proprio ficticio para Lucas apos consolidacao. Vigia recebeu a fatia para leitura; esta prova nao declara aceite global nem integracao real.'
];
report.finalizedAt=new Date().toISOString();
writeFileSync(jsonPath,JSON.stringify(report,null,2));
const screenshot=report.checks.find(x=>x.screenshot);
const md=[
    '# Prumo — delta de configuração DEV01, 08/10/2026',
    '',
    '**Preparo local conferido; uso real continua bloqueado em G01.** Dois arquivos novos entregues: vite.dev.config.ts e docs/desenvolvimento-dev.md. Proxy preparado `/api` sem rewrite, mas ausente no servidor fictício. Modo real falha antes de criar servidor; flag booleana não libera integração.',
    '',
    'O contrato enviado cedo ao Farol foi acolhido para Lume integrar `dev = vite --config vite.dev.config.ts`. Padrão frontend: 127.0.0.1:5178/strictPort/fictício. Preserve-se o processo e a configuração original; nenhum restart executado. Backend HTTP127.0.0.1:8080/WMS_PORT é contrato preparado do Cedro, sem processo observado. O destino opcional WMS_DEV_BACKEND_URL aceita só HTTP127.0.0.1:porta válida, sem caminho, credenciais, query ou fragmento. Same-origin conserva os caminhos existentes /api/v1; sem CORS wildcard ou mudança de CSP.',
    '',
    '| Fonte nova | SHA-256 |',
    '| --- | --- |',
    ...report.owned.map(s=>'| '+s.path+' | '+s.sha256+' |'),
    '',
    '## Provas próprias',
    '',
    '10 casos de configuração passaram com o loader Vite instalado: padrão fictício, WMS_PORT preparado, destino loopback explícito, real/G01 bloqueado, real com flag booleana sem liberação, host externo recusado, prefixo API no destino recusado, porta fora do intervalo recusada, WMS_PORT inválida e modo desconhecido. Nos casos fictícios, proxy ausente; contrato preparado conserva /api sem rewrite/changeOrigin. Middleware503 foi invocado apenas como função local, sem request HTTP/API/backend.',
    '',
    'Bootstrap fictício passou no Chromium local em **http://127.0.0.1:5191/#coletor**, 390px, usando cópia consistente closure r08 + oito arquivos CSS/entrypoint congelados + os dois arquivos novos e o recibo bloqueado original. Consulta UUID respondeu em memória; zero API/externos/console/CSP/overflow, Arial/margin0 e seis folhas CSS carregadas. Processo próprio fechado após a prova; esta URL não está sendo entregue como servidor ativo. [Captura](prumo-dev-config-bootstrap-ficticio-390.png).',
    '',
    'Composição/hashes efetivos estão no JSON e em `prumo-dev-config-20261008/snapshot.json`; manifesto r08 conferido SHA47214BF3. Fonte final da configuração permaneceu idêntica à executada. Nenhuma guarda fictícia foi criada: a cópia da guarda recebida conserva SHA9135BC92 e G01 bloqueado.',
    '',
    'Comandos executados:',
    '',
    '- Formatter instalado, somente vite.dev.config.ts e docs/desenvolvimento-dev.md (logs prumo-dev-config-formato.log/r02).',
    '- PLAYWRIGHT_BROWSERS_PATH=frontend/.tools/ms-playwright node evidencias/prumo-dev-config-prova.mjs; exit0, 11 verificações (10 de config + bootstrap). Log prumo-dev-config-prova.log.',
    '- '+report.typecheck.command+'; exit0. A primeira chamada sem --ignoreConfig recebeu TS5112 antes de avaliar a fonte; log preservado. Nenhuma suíte/tipagem global executada.',
    '',
    'Config original vite.config.ts preservada SHA327C6FCD19B616DCE39B3B118A9C03B92DB51611E94F57120A44FB384BCE2489. Recibo CSS final SHA2453DC77 e guarda SHA9135BC92 preservados. Sem package/App/cliente/CSS/backend/canônicos/mapa alterados por este recorte; sem rebuild, SQL/DPAPI/HTTPAPI/proxy/backend real, publicação, ETL ou callback Hermes.',
    '',
    '## Limite e encaminhamento',
    '',
    'Não há autorização técnica inferida de um booleano. Mesmo guardaRealAprovada=true isolado não abre esse caminho; nova prova atual de resolução D29, guarda real inteira e liberação Farol precisam de revisão explícita antes de habilitar real/proxy. Guia Cedro foi usado como preparação, nunca como backend8080 em execução. Nenhum novo lançador, guarda futura, credencial ou token foi inventado.',
    '',
    'Fonte estabilizada encaminhada ao Vigia via Farol. Lume mantém integração/package/App/sessão/cliente e único check final; ele inicia frontend atual próprio fictício e fornece endereço após consolidação. G01 bloqueado permanece independente desta entrega local. [Guia](../docs/desenvolvimento-dev.md) · [JSON completo](frontend-prumo-dev-config-lucas-20261008.json).',
];
const mdPath=path.join(evidence,'frontend-prumo-dev-config-lucas-20261008.md');writeFileSync(mdPath,md.join('\n')+'\n');
console.log(JSON.stringify({result:report.result,checks:report.checks.length,owned:report.owned,originalPreserved:report.originalConfigPreserved,guardPreserved:report.guardReceiptPreserved,cssPreserved:report.cssReceiptPreserved,processClosed:report.ownProcessClosed,jsonSha256:sha(readFileSync(jsonPath)),mdSha256:sha(readFileSync(mdPath)),screenshot:screenshot.screenshot},null,2));
