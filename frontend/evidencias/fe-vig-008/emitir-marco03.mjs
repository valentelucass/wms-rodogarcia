import { readFileSync, writeFileSync, readdirSync } from "node:fs";
import { createHash } from "node:crypto";
import { join, relative, resolve } from "node:path";
import assert from "node:assert/strict";

// Recibo do delta autorizado. Nenhuma escrita de aplicação ou fonte histórica.
const root = process.cwd();
const hash = bytes => createHash("sha256").update(bytes).digest("hex");
const read = path => JSON.parse(readFileSync(path, "utf8").replace(/^\uFEFF/, ""));
const execution = "evidencias/snapshot-marco03-r37/frontend";
const manifestPath = "evidencias/frontend-lume-manifesto-estavel-marco03-20261008.json";
const receiptPath = "evidencias/frontend-lume-entrega-lucas-20261008.json";
const markdownPath = "evidencias/frontend-lume-entrega-lucas-20261008.md";
const preserved = "evidencias/marco02-preservado";
const preservedManifest = preserved + "/frontend/evidencias/frontend-lume-manifesto-estavel-marco02-20261008.json";
const previous = read(preservedManifest);
const base = read(preserved + "/frontend/" + receiptPath);
const preservation = read(preserved + "/preservacao.json");
for (const file of preservation.files)
    assert.equal(hash(readFileSync(preserved + "/" + file.path)), file.sha256, file.path);
assert.equal(hash(readFileSync(preservedManifest)), preservation.manifestSha256);
assert.equal(hash(readFileSync("evidencias/frontend-lume-manifesto-estavel-marco02-20261008.json")), preservation.manifestSha256);

const source = read("evidencias/snapshot-marco03-r37/manifesto.json").files.map(file => {
    const bytes = readFileSync(file.path);
    assert.equal(hash(bytes), file.sha256, "Fonte atual divergiu: " + file.path);
    assert.equal(hash(readFileSync(execution + "/" + file.path)), file.sha256);
    return { path: "frontend/" + file.path, bytes: bytes.length, sha256: file.sha256 };
}).sort((a, b) => a.path.localeCompare(b.path));
const sourceHash = hash(source.map(file => `${file.path}\t${file.sha256}\t${file.bytes}\n`).join(""));
const original = new Map(previous.source.map(file => [file.path, file]));
const changes = source.filter(file => original.get(file.path)?.sha256 !== file.sha256)
    .map(file => ({ ...file, beforeSha256: original.get(file.path)?.sha256 ?? null }));
assert.deepEqual(changes.map(file => file.path), [
    "frontend/src/modules/financeiro/ExampleBilling.ts",
    "frontend/tests/billing-rejection-stage.test.ts",
    "frontend/tests/vigia-marco02-rejection.test.tsx",
]);
assert.equal(sourceHash, read("evidencias/fe-vig-008/delta-r36.json").sourceHash);
for (const input of previous.contractInputs) {
    assert.equal(hash(readFileSync(resolve("..", input.path))), input.sha256);
    assert.equal(hash(readFileSync(resolve("..", input.sourcePath))), input.sha256);
}
for (const [path, sha256] of Object.entries(previous.generatedOutputs))
    assert.equal(hash(readFileSync(resolve("..", path))), sha256);

const checks = read(execution + "/evidencias/marco03-r37-checks-final.json");
assert.equal(checks.length, 5);
assert.ok(checks.every(check => check.exitCode === 0));
const unit = read(execution + "/evidencias/marco03-r37-unit-resultados.json");
const browser = read(execution + "/evidencias/marco03-r37-browser-resultados.json");
assert.equal(unit.success, true);
assert.equal(unit.numFailedTests + unit.numPendingTests, 0);
assert.equal(browser.stats.unexpected + browser.stats.skipped + browser.stats.flaky, 0);
const observedPath = execution + "/evidencias/fe-vig-008-browser-atual-v03.json";
const observed = read(observedPath);
const runtime = read(execution + "/evidencias/runtime-5189.json");
const runtimeObservation = read(execution + "/evidencias/runtime-5189-observacao-processo.json");
assert.equal(observed.status, "passed");
assert.equal(observed.sourceSha256, changes[0].sha256);
assert.equal(observed.origin, runtime.url);
assert.equal(runtimeObservation.pid, runtime.pid);
assert.equal(runtimeObservation.observedRunning, true);
assert.equal(runtime.observedRunning && runtime.proxyAbsent, true);
assert.equal(observed.backendCalled, false);
assert.equal(observed.externalRequests.length + observed.errors.length + observed.css.csp.length, 0);
const redPath = "evidencias/snapshot-marco03-r35-red/frontend/evidencias/fe-vig-008-literal-red-report.json";
const focalPath = "evidencias/snapshot-marco03-r36-focal/frontend/evidencias/fe-vig-008-focal-green-report.json";
const focal = read(focalPath);
assert.equal(read(redPath).numFailedTests, 1);
assert.equal(focal.success, true);
assert.equal(focal.numPassedTests, 10);
assert.equal(hash(readFileSync("tests/vigia-marco02-rejection.test.tsx")), "28fd05a8c0c945a3fb6db467131bf3e11aaae96baf4038ad158bbb3497799114");
const coverage = read("docs/provas-jornadas-marco02.json");
for (const criterion of coverage.rows) for (const witness of criterion.witnesses)
    assert.equal(hash(readFileSync(resolve("..", witness.path))), witness.sha256);
const processProof = {
    ...runtime, actualProcess: runtimeObservation, sourceHash,
    sourceSnapshot: "frontend/" + execution, sourceFiles: source.length, byteMatches: source.length,
    observation: "frontend/" + observedPath, servedCorrection: "FE-VIG-008",
    historical5188: {
        url: "http://127.0.0.1:5188", pid: 52792,
        sourceHash: previous.sourceHash,
        snapshot: "frontend/evidencias/snapshot-marco02-r32/frontend",
        corrected: false, untouched: true,
        qualification: "MARCO02 histórico: conserva FE-VIG-008. A equivalência antiga 176/177 não vincula este processo à aplicação MARCO03 corrigida.",
    },
};
writeFileSync("evidencias/frontend-lume-exercicio-marco03.json", JSON.stringify(processProof, null, 2) + "\n");

const excluded = new Set([manifestPath, receiptPath, markdownPath]);
function inventory(directory) {
    const rows = [];
    for (const item of readdirSync(directory, { withFileTypes: true })) {
        if (["node_modules", ".tools", ".git"].includes(item.name)) continue;
        if (item.name.startsWith(".env") && item.name !== ".env.example") continue;
        const file = join(directory, item.name);
        if (item.isDirectory()) rows.push(...inventory(file));
        else if (item.isFile()) {
            const path = relative(root, file).replaceAll("\\", "/");
            if (excluded.has(path)) continue;
            const bytes = readFileSync(file);
            rows.push({ path: "frontend/" + path, bytes: bytes.length, sha256: hash(bytes) });
        }
    }
    return rows;
}
const sourcePaths = new Set(source.map(file => file.path));
const artifacts = inventory(root).filter(file => !sourcePaths.has(file.path))
    .sort((a, b) => a.path.localeCompare(b.path));
const at = new Date().toISOString();
const previousStage = {
    stage: "D31-LUME-MARCO02", sourceHash: previous.sourceHash,
    manifestSha256: preservation.manifestSha256,
    preservedManifest: "frontend/" + preservedManifest,
    preservedFiles: preservation.copied, directory: "frontend/" + preserved,
    baselineReferencesVerifiedBeforeEdit: preservation.referencesVerified, divergences: 0,
    artifactPolicy: "177 fontes+3 recibos/manifesto copiados; artefatos/checks/input copies antigos preservados nos caminhos originais",
};
const manifest = {
    demand: "FIM_FRONTEND_LUCAS_20261008", stage: "D31-LUME-MARCO03", finding: "FE-VIG-008",
    emittedUtc: at, algorithm: "SHA256", sourceHash,
    sourceHashDefinition: previous.sourceHashDefinition, previousStage,
    exclusions: ["node_modules", ".tools", ".git", ".env exceto .env.example", ...excluded],
    source, artifacts, changes, contractInputs: previous.contractInputs,
    generatedOutputs: previous.generatedOutputs,
    contractInterpretation: "Contratos/gerador/overrides iguais ao MARCO02;167 cópias reais e fontes atuais+3 outputs reconferidos, sem regeneração necessária. Hashes certificam bytes, não interpretação/permissão. Nenhuma alteração backend/database.",
};
writeFileSync(manifestPath, JSON.stringify(manifest, null, 2) + "\n");
const traceFiles = readdirSync(execution + "/evidencias").filter(name =>
    name.endsWith(".json") && (name.startsWith("marco03-r37-ui-") || name.startsWith("fe12-cedro-ui-") || name === "marco03-r37-browser-rede.json"));
const receipt = {
    ...base, marco: manifest.stage, emitidoUtc: at, fonteSha256: sourceHash,
    estado: "FONTE_LOCAL_ESTAVEL_CONGELADA_APOS_FE_VIG_008_PENDENTE_REVISAO_INTEGRADA",
    marcoAnterior: previousStage, historicoMarco01: base.marcoAnterior,
    fonte: { ...base.fonte, manifesto: "frontend/" + manifestPath,
        sha256: hash(readFileSync(manifestPath)), arquivos: source.length,
        artefatos: artifacts.length, entradasGerador: manifest.contractInputs.length,
        snapshot: "frontend/" + execution, byteMatches: source.length, divergencias: 0 },
    execucoes: checks.map(check => ({ ...check, log: "frontend/" + execution + "/" + check.log })),
    testes: { ...base.testes, unitarios: unit.numPassedTests, browser: browser.stats.expected,
        falhasUnitarias: unit.numFailedTests, falhasBrowser: browser.stats.unexpected,
        ignorados: unit.numPendingTests + browser.stats.skipped, focal008: 10, browserDev008: 1 },
    criterios: base.criterios,
    criterio008: { id: "FE-VIG-008", status: "RED_LITERAL_PRESERVADO_GREEN_LOCAL_SEM_ACEITE_INDEPENDENTE",
        applicationChange: changes[0], literalHash: changes[2].sha256, localHistoryHash: changes[1].sha256,
        delta: "frontend/evidencias/fe-vig-008/delta-r36.json", deltaSha256: hash(readFileSync("evidencias/fe-vig-008/delta-r36.json")),
        red: "frontend/" + redPath, focal: "frontend/" + focalPath, browser: "frontend/" + observedPath,
        scope: "Somente dois estados preparados do respondedor fictício: rejeitar→REJEITADO/REJEITADA. IDs/revisões/hash/históricos mantidos; sem decisão/cálculo financeiro real." },
    browser: { ...base.browser, processoChecks: read(execution + "/evidencias/marco03-r37-browser-processo.json"),
        traceFiles: traceFiles.map(name => "frontend/" + execution + "/evidencias/" + name),
        runtimeLucas: processProof, observacao: "frontend/" + observedPath,
        css: "CSS Prumo íntegro, provas anteriores reaproveitadas; observação atual FE008 confirma Arial/header azul/zeroCSP/console. Sem nova rodada estética/CSS." },
    correcoes: [...base.correcoes, "FE-VIG-008: rejeição integral fictícia e GET consultado respondem REJEITADO/REJEITADA; teste Vigia literal e histórico1/2 preservados"],
    revisao: { ...base.revisao, estado: "Delta imutável R36 comunicado; fonte integrada R37 para reconferência via Farol/Vigia. Sem aceite global antecipado.",
        escritaSuspensa: true, deltaAposMarco02: changes },
    preservacao: { ...base.preservacao, marco02: previousStage,
        novosReds: "Literal Vigia e reprodução própriaR35 preservados. Observador browserv01 entrou em Fatos, v02 usou caption exata incompleta; ambos preservados e qualificados. v03 prova jornada correta; aplicação não mudou entre esses ensaios." },
    limites: base.limites,
    proximoPasso: "Farol encaminha MARCO03/R37 e delta FE008 a Vigia, qualifica 5188 histórico e 5189 corrigido, atualiza canônicos/mapa isoladamente. Somente achado material justifica nova escrita/re-freeze; G01 bloqueada.",
};
writeFileSync(receiptPath, JSON.stringify(receipt, null, 2) + "\n");
writeFileSync(markdownPath, [
    "# MARCO03 — correção focal FE-VIG-008", "",
    `Fonte congelada SHA256 \`${sourceHash}\`, ${source.length} arquivos idênticos ao snapshot R37 executado. [Manifesto](frontend-lume-manifesto-estavel-marco03-20261008.json), SHA256 \`${receipt.fonte.sha256}\`. [Recibo JSON](frontend-lume-entrega-lucas-20261008.json). Sem aceite independente antecipado.`, "",
    "O respondedor fictício de rejeição agora informa REJEITADO/REJEITADA no recibo e na consulta. A única mudança de aplicação é ExampleBilling; backend/contratos/CSS/cliente/FE12 íntegros. IDs, revisão, número/hash e documentos/históricos permanecem separados por versão. Teste Vigia literal28fd, sem adaptações; reprodução própria RED antes da correção,10 focais verdes depois. [Delta e comandos](fe-vig-008/delta-r36.json).", "",
    `Tipagem/lint/build exit0; **${unit.numPassedTests} testes locais +${browser.stats.expected} Chromium**, zero falhas/ignorados/flaky. Mais uma prova DEV atual de rejeição/consulta em5189, fora da contagem16. 161 smokes não161jornadas;22 critérios compartilhados e seus hashes continuam no mapa existente, sem duplicação de suíte/documentação.`, "",
    "| Comando real no R37 | Resultado | Log |", "| --- | --- | --- |",
    ...receipt.execucoes.map(check => `| ${check.command} | exit${check.exitCode} | [log](${check.log.replace("frontend/evidencias/", "")}) |`), "",
    `**Lucas: ${runtime.url}**, fictício, PID${runtime.pid}, fonte corrigida R37/c58b. [Vínculo fonte/URL/PID/argv](frontend-lume-exercicio-marco03.json); [prova browser atual](snapshot-marco03-r37/frontend/evidencias/fe-vig-008-browser-atual-v03.json), duas capturas. Gestor/contexto→cálculo801→fechamento901→versão1001→rejeição com motivo→GET atual. Payload usa id901/número1/revisão0; resposta e UI exibem REJEITADO/REJEITADA. Sem copiar IDs de DTO bruto; seleção por tabela e ID exibido. ZeroAPI/externos/CSP/console. Captura de consulta inspecionada.`, "",
    "5178 e5188 foram preservados, sem kill/restart. **5188/R32 é MARCO02 histórico DD440A, com FE-VIG-008 conhecido; não é a fonte corrigida.** A equivalência antiga177 arquivos não é reutilizada como prova desta aplicação alterada. O lançador novo usa a mesma vite.dev.config.ts/CSP, exige fictício+hash corrigido e proxy ausente antes de escutar. Lançamento CLI direto recusado pela revisão automática (bloqueada por política, sem motivo adicional), sem processo criado; alternativa restrita executada e observada. [Detalhes](fe-vig-008/fecho.md).", "",
    "MARCO02:180 cópias preservadas (177fontes+manifesto+recibos),7782 referências verificadas antes da escrita,zero divergência. Checks/recibos/logs antigos permanecem próprios.167 entradas gerador apontam bytes reais/verificáveis e3 outputs iguais; sem nova geração/execução backend. CSS Prumo/FE12 Cedro iguais, com provas reaproveitadas.", "",
    "G01 continua bloqueada. Sem API/backend/SQL/provider real/dispositivos/fiscal/piloto. Bundle>500kB e demais limites registrados permanecem. Aplicação/testes/config/docs congelados para revisão do delta integrado via Farol; somente os processos fictícios próprios permanecem abertos.", "",
].join("\n"));
console.log(JSON.stringify({stage:manifest.stage,sourceHash,manifestHash:receipt.fonte.sha256,
    sourceFiles:source.length,artifacts:artifacts.length,contractInputs:manifest.contractInputs.length,
    unitTests:unit.numPassedTests,browserTests:browser.stats.expected,browserDev008:1,
    runtime:runtime.url,pid:runtime.pid,frozen:true},null,2));
