import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import path from 'node:path';
const evidence = import.meta.dirname;
const frontend = path.resolve(evidence, '..');
const area = path.join(evidence, 'prumo-css-20261008');
const sha = data => createHash('sha256').update(data).digest('hex');
const reportPath = path.join(evidence,'frontend-prumo-css-lucas-20261008.json');
const report = JSON.parse(readFileSync(reportPath));
const diagnosis = JSON.parse(readFileSync(path.join(area,'diagnostico-v03.json')));
const inventory = JSON.parse(readFileSync(path.join(area,'inventario-v03.json')));
const allChecks = [...report.checks,...report.complement.checks];
const allResponses = [...report.responses,...report.complement.responses];
const cssResponses = [...new Map(allResponses.filter(r=>r.contentType?.startsWith('text/css')).map(r=>[r.mode+' '+r.url,r])).values()];
if (report.result !== 'PASS' || cssResponses.some(r=>r.status!==200)) throw Error('Prova CSS sem sucesso');
const ownedUnchanged = report.owned.every(({file,sha256})=>sha(readFileSync(path.join(frontend,file)))===sha256);
if (!ownedUnchanged) throw Error('Fonte em posse mudou após prova');
const initial = new Map(inventory.inventory.map(x=>[x.path.replace('frontend/',''),x]));
report.scope = 'D31 / FIM_FRONTEND_LUCAS_20261008 / somente estilos, entrypoint e prova própria Prumo';
report.manifestSha256 = inventory.manifestSha;
report.owned = report.owned.map(item=>({...item,beforeSha256:initial.get(item.file)?.actual,changed:item.sha256!==initial.get(item.file)?.actual}));
report.diagnostic = diagnosis.results.map(({mode,url,body,header,sheets,violations,responses})=>({mode,url,body,header,sheets,violations,cssAndEntryResponses:responses.filter(r=>/main\.tsx|styles\.css|\/assets\//.test(r.url))}));
report.cssResponses = cssResponses;
report.summary = {
    result:'PASS somente CSS/entrypoint nas fontes próprias verificadas',
    screenshots:report.screenshots.length+report.complement.screenshots.length,
    computedStates:allChecks.filter(c=>c.body).length,
    sourceOwnedUnchanged:ownedUnchanged,
    consoleErrors:report.errors.length+report.complement.errors.length,
    externalOrApiAttempts:report.blocked.length+report.complement.blocked.length,
    cssCspViolations:allChecks.filter(c=>c.violations?.length).length,
    documentOverflow:allChecks.filter(c=>c.documentWidth>c.viewport?.width || c.documentWidth>c.width).length,
    typecheckOrGeneralSuiteExecuted:false,
    bundlerBuildExecuted:true,
    nativeIntegrationOrGlobalAcceptance:false,
    ownProcessesClosed:report.processClosed&&report.complement.processClosed,
};
report.chain = 'index.html link /src/styles.css → cinco @import styles/{base,shell,forms,results,collector}.css; main.tsx só monta React. Vite DEV entrega text/css externo; BUILD concatena em asset CSS com hash. CSP original mantida, sem bypass/unsafe-inline e sem injeção CSS de prova.';
report.limitations = [
    'Causa comprovada para DEV próprio 127.0.0.1:5191: Vite injetava style inline bloqueado por CSP. BUILD inicial já aplicava CSS. URL/tela original vista por Lucas não fornecida; não atribuir retrospectivamente a ela esta causa.',
    'Defeito do skip em fonte copiada: href=#conteudo dispara hashchange de useNavigation e renderiza Módulo não encontrado. Foco/outline do link funcionam; navegação preservada exige correção Lume encaminhada via Farol. Após registrar defeito, provas focais CSS recarregam a jornada; teclado global não declarado aprovado.',
    'Snapshot próprio completo conferido byte a byte durante cópia; usa fonte de 21:16Z + oito arquivos em posse estabilizados. Fields.tsx já divergia do handoff e foi preservado na cópia atual. Lume/Cedro seguem alterando outras áreas; esta prova não certifica a futura fonte global. Integração e checks finais ficam com Lume.',
    'Frontend somente fictício sem API/backend/SQL/equipamento/publicação; 5191 foi usada sequencialmente por servidores próprios fechados. Nenhum processo existente foi tocado.',
    'Vite avisou bundle JS >500kB, já presente no diagnóstico; build focal gerou assets. Não alterar config/package/splitting fora da posse CSS.',
];
report.proofArtifacts = ['prumo-css-diagnostico.mjs','prumo-css-prova.mjs','prumo-css-complemento.mjs','prumo-css-recibo.mjs','prumo-css-formato.log','prumo-css-diagnostico.log','prumo-css-diagnostico-r02.log','prumo-css-diagnostico-r03.log','prumo-css-prova.log','prumo-css-prova-r02.log','prumo-css-complemento.log','prumo-css-20261008/inventario-v03.json','prumo-css-20261008/snapshot-diagnostic-v03.json','prumo-css-20261008/snapshot-verificado.json','prumo-css-20261008/diagnostico-v03.json','prumo-css-20261008/prova-r01-falha-navegacao.json','prumo-css-20261008/prova-r02-css-verde.json','prumo-css-20261008/complemento.json'];
report.finalizedAt = new Date().toISOString();
writeFileSync(reportPath,JSON.stringify(report,null,2));
const cssAsset = report.buildAssets.find(x=>x.file.endsWith('.css'));
const representative=allChecks.filter(x=>['cadastros-formulario','coletor-foco'].includes(x.label)&&x.mode==='dev');
const lines=[
    '# Prumo — base CSS local de Lucas, 08/10/2026',
    '',
    '**Resultado da fatia: PASS.** CSS e entrypoint aplicados em Chromium local, DEV e BUILD próprios, desktop 1440/768 e coletor 390. 30 capturas atuais, zero erro de console, zero violação CSP, zero overflow do documento, zero tentativa de API/rede externa. Não é aceite global do frontend nem integração real.',
    '',
    'Manifesto de posse confirmado: `f49f86454f8852ddf334c71a04e4ebd624968b28892d62faaa70883711e2220b`. Organização de cinco estilos e classes existentes preservada. Sete arquivos alterados; `src/styles.css` ficou intacto.',
    '',
    '## Causa e correção',
    '',
    'Antes, em `http://127.0.0.1:5191/#coletor`, o DEV carregava styles.css como módulo JS e o Vite injetava `<style>` inline: a CSP `style-src self` recusava sua aplicação. Prova: Times New Roman, body margin 8px, cabeçalho sem fundo/padding, zero stylesheets e evento style-src-elem/inline. O BUILD inicial da mesma cópia já aplicava Arial, margin 0 e azul por asset externo. Não se conhece a URL original observada por Lucas.',
    '',
    '`index.html` passa a carregar `<link rel="stylesheet" href="/src/styles.css">`; `main.tsx` deixa de importar o CSS como JS. Os cinco @import continuam no agregador. DEV e BUILD usam folhas externas, sem relaxar CSP e sem bypass. Base mínima: Arial/16px/preto/branco/azul124cab, espaçamento/alinhamento, botões/campos44px, coletor52px, foco azul3px, feedback/detalhes legíveis, navegação horizontal contida em telas<=800 e tabelas com rolagem contida.',
    '',
    '## Provas e fonte',
    '',
    '- URL sequencial própria: `http://127.0.0.1:5191/#cadastros`, `/#entrada`, `/#coletor`; processos próprios fechados após captura. Não é URL atualmente servida ao usuário.',
    '- Cadastros: consulta com tabela/sucesso, erro e formulário em 1440/768. Entrada: navegação real por botão e feedback em 1440/768. Coletor390: input/foco, erro, consulta/avanço de foco, detalhes medidas/conjunto abertos por teclado, revisão, resumo confirmado com foco e detalhes da resposta.',
    '- DOM/getComputedStyle registra fonte/tamanho/margem/padding/layout/gap/background/color/border/outline/min-height/overflow; documentWidth e mediaqueries800/480; URL/rules de cada folha. HTTPstatus/Content-Type/bytes/SHA de assets estão no JSON. Todas respostas CSS text/css e HTTP200.',
    '- Asset CSS BUILD: `' + cssAsset.file + '`, SHA-256 `' + cssAsset.sha256 + '`, em `prumo-css-20261008/dist-verified/assets`. Nenhuma escrita em dist/evidências Lume.',
    '- Fonte executada: cópia completa consistente `diagnostic-source-v03` + oito arquivos em posse para `verified-source-r02`; manifests de todos os arquivos e hashes em snapshot-diagnostic-v03.json/snapshot-verificado.json. O snapshot do handoff continha só recortes; a tentativa inicial incompleta foi preservada e substituída por cópia completa, com comparação antes/cópia/depois.',
    '- Comandos: formatter local apenas nos oito arquivos; `PLAYWRIGHT_BROWSERS_PATH=frontend/.tools/ms-playwright node evidencias/prumo-css-diagnostico.mjs`; `node evidencias/prumo-css-prova.mjs`; `node evidencias/prumo-css-complemento.mjs`. Scripts chamam Vite DEV/build/preview em5191 com cache/saída próprios. Prova e complemento retornaram exit0; sem typecheck/lint/suíte geral repetida. Logs r01 de browser ausente, snapshot incompleto e falha skip preservados.',
    '',
    '| Arquivo | SHA-256 atual |',
    '| --- | --- |',
    ...report.owned.map(x=>`| ${x.file} | ${x.sha256} |`),
    '',
    '| Estado DEV | Largura | Fonte | Main padding | Outline foco | Documento |',
    '| --- | --- | --- | --- | --- | --- |',
    ...representative.map(x=>`| ${x.label} | ${x.viewport.width} | ${x.body.fontFamily} / ${x.body.fontSize} | ${x.main.padding} | ${x.focus.outline} | ${x.documentWidth}px |`),
    '',
    '## Limites e encaminhamento',
    '',
    'O link “Ir para o conteúdo” tem defeito na navegação da fonte copiada: href=#conteudo dispara useNavigation e troca a jornada por “Módulo não encontrado”. Foi reproduzido, registrado no JSON e comunicado ao Farol para Lume corrigir markup/hook, fora da posse Prumo. A prova de CSS prosseguiu recarregando a jornada após registrar o defeito. Não declarar teclado global aprovado com esse resultado.',
    '',
    'Lume/Cedro trabalham nas demais fontes; esta cópia não certifica a futura integração global. Revisão da fatia foi encaminhada ao Vigia via Farol após estabilizar hashes. Lume integra e executa os checks finais. Nenhum componente/contrato/package/config/tools/backend/canônico/mapa foi editado por Prumo. Sem SQL, HTTPDEV, integração real, equipamento, publicação ou callback Hermes.',
    '',
    'Recibo completo: [JSON](frontend-prumo-css-lucas-20261008.json). Capturas representativas: [desktop formulário](prumo-css-dev-cadastros-formulario-1440.png), [tablet tabela](prumo-css-build-cadastros-tabela-sucesso-768.png), [coletor](prumo-css-dev-coletor-consulta-390.png), [resumo confirmado](prumo-css-build-coletor-resumo-390.png).',
];
writeFileSync(path.join(evidence,'frontend-prumo-css-lucas-20261008.md'),lines.join('\n')+'\n');
console.log(JSON.stringify({summary:report.summary,cssAsset,reportSha256:sha(readFileSync(reportPath)),mdSha256:sha(readFileSync(path.join(evidence,'frontend-prumo-css-lucas-20261008.md'))),cssResponses:cssResponses.map(x=>({mode:x.mode,url:x.url,status:x.status,type:x.contentType,sha:x.sha256})),owned:report.owned},null,2));
