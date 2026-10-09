import { createHash } from "node:crypto";
import { readFileSync, writeFileSync, readdirSync, mkdirSync } from "node:fs";
import { join, relative, resolve } from "node:path";

// Somente recibo frontend. Recusa divergências e não altera fonte/canônicos/mapa.
const root = process.cwd(),
    label = process.argv[2];
if (!/^marco02-r\d+$/.test(label ?? ""))
    throw Error("Informe snapshot final novo");
const execution = `evidencias/snapshot-${label}/frontend`;
const manifestPath =
    "evidencias/frontend-lume-manifesto-estavel-marco02-20261008.json";
const receiptPath = "evidencias/frontend-lume-entrega-lucas-20261008.json";
const markdownPath = "evidencias/frontend-lume-entrega-lucas-20261008.md";
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
const readJson = (file) =>
    JSON.parse(readFileSync(file, "utf8").replace(/^\uFEFF/, ""));
const excluded = new Set([manifestPath, receiptPath, markdownPath]);
function inventory(directory) {
    const files = [];
    for (const item of readdirSync(directory, { withFileTypes: true })) {
        if (["node_modules", ".tools", ".git"].includes(item.name)) continue;
        if (item.name.startsWith(".env") && item.name !== ".env.example")
            continue;
        const file = join(directory, item.name);
        if (item.isDirectory()) files.push(...inventory(file));
        else if (item.isFile()) {
            const path = relative(root, file).replaceAll("\\", "/");
            if (excluded.has(path)) continue;
            const bytes = readFileSync(file);
            files.push({
                path: "frontend/" + path,
                bytes: bytes.length,
                sha256: hash(bytes),
            });
        }
    }
    return files;
}
const isSource = (file) =>
    !/^frontend\/(evidencias|dist|test-results[^/]*)\//.test(file.path);
const source = inventory(root)
    .filter(isSource)
    .sort((a, b) => a.path.localeCompare(b.path));
const sourceHash = hash(
    source.map((f) => `${f.path}\t${f.sha256}\t${f.bytes}\n`).join(""),
);
for (const file of source) {
    const tested = resolve(execution, file.path.replace("frontend/", ""));
    if (hash(readFileSync(tested)) !== file.sha256)
        throw Error("Fonte/check divergente: " + file.path);
}
const checks = readJson(`${execution}/evidencias/${label}-checks-final.json`);
if (checks.length !== 5 || checks.some((c) => c.exitCode !== 0))
    throw Error("Checks finais incompletos");
const unit = readJson(`${execution}/evidencias/${label}-unit-resultados.json`);
const browser = readJson(
    `${execution}/evidencias/${label}-browser-resultados.json`,
);
if (
    !unit.success ||
    unit.numFailedTests ||
    unit.numPendingTests ||
    browser.stats.unexpected ||
    browser.stats.skipped
)
    throw Error("Teste final falhou/ignorado");
const baseline = readJson("evidencias/marco01-preservado/preservacao.json");
for (const f of baseline.files)
    if (
        hash(readFileSync(join("evidencias/marco01-preservado", f.path))) !==
        f.sha256
    )
        throw Error("MARCO01 divergente: " + f.path);
const contract = readJson("evidencias/fontes-contratos.json");
const contractInputs = Object.entries(contract.fontes).map(
    ([sourcePath, sha256]) => {
        const bytes = readFileSync(resolve("..", sourcePath));
        if (hash(bytes) !== sha256)
            throw Error("Input atual divergiu: " + sourcePath);
        const copied = join(
            "evidencias",
            `snapshot-${label}`,
            "contractInputs",
            sourcePath,
        );
        mkdirSync(join(copied, ".."), { recursive: true });
        writeFileSync(copied, bytes);
        return {
            path: "frontend/" + copied.replaceAll("\\", "/"),
            sourcePath,
            origin: "bytes atuais efetivamente lidos pela geração; cópia verificável",
            sha256,
            roles: contract.leituras[sourcePath],
        };
    },
);
for (const [file, expected] of Object.entries(contract.gerados))
    if (hash(readFileSync(resolve("..", file))) !== expected)
        throw Error("Contrato derivado divergente: " + file);
const observed = readJson(
    `${execution}/evidencias/marco02-exercicio-observado.json`,
);
const processProof = readJson(
    "evidencias/frontend-lume-exercicio-marco02.json",
);
if (
    observed.errors.length ||
    observed.violations.length ||
    observed.backendCalled ||
    observed.externalRequests.length ||
    !processProof.observedRunning
)
    throw Error("Exercício/servidor fora da fronteira");
const traceFiles = readdirSync(`${execution}/evidencias`).filter(
    (name) =>
        name.endsWith(".json") &&
        (name.startsWith(`${label}-ui-`) ||
            name.startsWith("fe12-cedro-ui-") ||
            name === `${label}-browser-rede.json`),
);
for (const name of traceFiles) {
    const trace = readJson(`${execution}/evidencias/${name}`);
    if (
        trace.backendCalled ||
        trace.externalRequests?.length ||
        trace.external?.length
    )
        throw Error("Rede API/externa registrada: " + name);
}
const coverage = readJson("docs/provas-jornadas-marco02.json");
for (const criterion of coverage.rows) {
    for (const witness of criterion.witnesses) {
        const relativeTest = witness.path.replace("frontend/", "");
        const isBrowser = relativeTest.startsWith("tests/browser/");
        const executed = isBrowser
            ? browser.suites.some((s) =>
                  s.file
                      ?.replaceAll("\\", "/")
                      .endsWith(relativeTest.replace("tests/browser/", "")),
              )
            : unit.testResults.some(
                  (s) =>
                      s.name.replaceAll("\\", "/").endsWith(relativeTest) &&
                      s.status === "passed",
              );
        if (!executed)
            throw Error("Prova essencial sem execução verde: " + witness.path);
    }
}
const emittedUtc = new Date().toISOString();
const files = inventory(root).sort((a, b) => a.path.localeCompare(b.path));
const previousStage = {
    stage: baseline.stage,
    sourceHash: baseline.sourceHash,
    manifestSha256: baseline.manifestSha256,
    preservedFiles: baseline.copied,
    directory: "frontend/evidencias/marco01-preservado",
    verified: true,
};
const manifest = {
    demand: "FIM_FRONTEND_LUCAS_20261008",
    stage: "D31-LUME-MARCO02",
    emittedUtc,
    algorithm: "SHA256",
    sourceHash,
    previousStage,
    sourceHashDefinition:
        "SHA256 da lista ordenada path TAB sha256 TAB bytes LF; fonte exclui evidencias/dist/test-results/dependências; snapshot final idêntico por arquivo.",
    exclusions: [
        "node_modules",
        ".tools",
        ".git",
        ".env exceto .env.example",
        ...excluded,
    ],
    source,
    artifacts: files.filter((f) => !isSource(f)),
    contractInputs,
    generatedOutputs: contract.gerados,
    contractInterpretation:
        "167 hashes certificam bytes lidos, não interpretação de regexp/guardas. Overrides de apresentação condicional rastreados e provados por perfil. Input XmlService histórico separado, geração atual5e9e qualificada.",
};
writeFileSync(manifestPath, JSON.stringify(manifest, null, 2) + "\n");
const receipt = {
    demanda: manifest.demand,
    autor: "WMS - Lume",
    marco: manifest.stage,
    emitidoUtc: emittedUtc,
    versao: readJson("package.json").version,
    fonteSha256: sourceHash,
    estado: "FONTE_LOCAL_ESTAVEL_CONGELADA_PARA_REVISAO_INTEGRADA_SEM_ACEITE_ANTECIPADO",
    marcoAnterior: previousStage,
    fonte: {
        manifesto: "frontend/" + manifestPath,
        sha256: hash(readFileSync(manifestPath)),
        arquivos: source.length,
        artefatos: manifest.artifacts.length,
        entradasGerador: contractInputs.length,
        rotas: contract.rotas,
        records: contract.records,
        enums: contract.enums,
        snapshot: "frontend/" + execution,
        byteMatches: source.length,
        divergencias: 0,
    },
    execucoes: checks.map((c) => ({
        ...c,
        log: "frontend/" + execution + "/" + c.log,
    })),
    testes: {
        unitarios: unit.numPassedTests,
        browser: browser.stats.expected,
        falhasUnitarias: unit.numFailedTests,
        falhasBrowser: browser.stats.unexpected,
        ignorados: unit.numPendingTests + browser.stats.skipped,
        alcance:
            "Inclui161 smokes de contrato. Jornadas são22 critérios essenciais com19 arquivos compartilhados; não161E2E nem prova de negócio backend.",
    },
    criterios: coverage.rows.map((c) => ({
        ...c,
        status: "PROVAS_LOCAIS_VERDES_NESTA_FONTE_PENDENTE_REVISAO_INTEGRADA",
    })),
    browser: {
        processoChecks: readJson(
            `${execution}/evidencias/${label}-browser-processo.json`,
        ),
        encerramentoChecks:
            "Playwright encerrou seu preview próprio5192, sem reutilizar/encerrar existente.",
        traceFiles: traceFiles.map(
            (name) => "frontend/" + execution + "/evidencias/" + name,
        ),
        backendCalled: false,
        externalRequests: [],
        runtimeLucas: processProof,
        observacao:
            "frontend/" +
            execution +
            "/evidencias/marco02-exercicio-observado.json",
        css: "6 estados DEV próprios1440/768/390, fonte final; CSS externo/bodyArial/margin0/headerazul/sem overflow/CSP/console. Fatia Prumo30 capturas e reconferência Vigia permanecem referenciadas.",
    },
    correcoes: [
        "001NON_NULL/required",
        "002pai+atualidade de catálogo/revisão/reseleção/parcial legítima",
        "003constraints do elemento ListXML",
        "004primitivo/raiz obrigatória null e2xx incerto",
        "005ler/separar comuns por perfil e resolverPendentesGestor explícito",
        "006demonstrativo corresponde à versão selecionada;007aprovação/entrega preservam identidade/hash/documentos próprios e versão antiga histórica; PREPARADA sem decisão genérica",
        "ProblemDetail null/HTML/status/requestId; montagem antes de recursos/cleanup",
        "401 atrasado abort sem expirar sessão nova;500/502 escrita incerta preservaUUID/payload",
        "XML A/B/edição/unmount/rejeição e callback atual",
        "Coletor resposta antiga/contexto e Mapcontingência numérico por DTO",
        "FE-PRU-SKIP01salto preserva rota/estado/foco",
        "ORG01–05shell/páginas/hooks/validação/componentes/módulos/fixtures/estilos",
        "GEN01inputs reais167+gerador/overrides+3outputs e interpretação condicional qualificada",
        "Jornadas22: refs próprias, nomes semânticos de tabelas, fixtures por estágio, chegada≠efetivação, novosramosFE10 sem cálculo comercial",
    ],
    fontesColegas: [
        "frontend/evidencias/frontend-prumo-css-lucas-20261008.json",
        "frontend/evidencias/frontend-prumo-dev-config-lucas-20261008.json",
        "frontend/evidencias/frontend-prumo-dev-guarda-lucas-20261008.json",
        "frontend/evidencias/frontend-cedro-fe12-lucas-20261008.json",
    ].map((path) => ({
        path,
        sha256: hash(readFileSync(path.replace("frontend/", ""))),
    })),
    revisao: {
        estado: "Vigia reconfirma fonte final via Farol; fatias verdes não concedem aceite global",
        fatiasHistoricas:
            "002R10 onze;004R09 nove;005R16 revisão11 dezessete;CSSPrumo/indep;FE12Cedro+indep e auditR14 seis. R18 leitura paralela imutável preservada.",
        adendaR16: "frontend/evidencias/adenda-r16-comando-lint.json",
        escritaSuspensa: true,
        canal: "Somente WMS - Farol via maestri ask; nenhum Hermes/conexão nova",
        criterioOrganizacao:
            "Responsabilidades/acoplamento/testabilidade concretos em docs/organizacao.md; importtypeWorkflow apagado, sem ciclo runtime; não teto de linhas.",
    },
    preservacao: {
        marco01:
            "259 cópias reconferidas; snapshots/logs/capturas históricos não reescritos",
        reds: "R14/R17/R18/R19/R21/R22 e três focais originais preservados; resultado verde final não altera identidade dos anteriores",
        preexistente: "frontend/evidencias/README-preexistente.md",
        pdf: "frontend/evidencias/referencia-pdf/inventario.json",
        regra: "Somentefrontend Lume; mudanças DEV01 Cedro/Prumo próprias referenciadas. Não alega backend atual todosbytes iguais ao histórico; não execução backend/SQL.",
    },
    documentos: [
        "README.md",
        "docs/organizacao.md",
        "docs/contratos-e-transporte.md",
        "docs/cobertura-e-limites.md",
        "docs/provas-jornadas-marco02.md",
        "docs/mapeamento-telas-contratos.md",
        "docs/versoes.md",
        "docs/desenvolvimento-dev.md",
    ].map((p) => "frontend/" + p),
    limites: [
        "G01BLOQUEADA: nenhum frontend→API/backend/SQL. DEV01preparado/real failclosed; proxy ausente no fictício",
        "ResourceServerRS256/Bearer sem login/token/senha: provider/ciclo de tokens/atribuições reais pendentes",
        "SQLServer/HTTPDEV/PROD/sa/sondas/DDL/migrations/sharedruntime/restart/kill existentes não executados",
        "Coletor/impressora/fiscal/NOTAZZ/ESL/TMS/equipamento real/piloto/restauração não homologados",
        "Respostas/valores/hash/datas fictícios não provam regra/stock/cobrança/capacidade/concorrência backend;161 smokes não161 jornadas",
        "Bundle principal maior500kB; desempenho em coletor físico ainda não medido",
        "Sem publicação/commit/push/ETL/rotina/callback Hermes",
    ],
    proximoPasso:
        "Farol encaminha fonte congelada/checks/22 critérios a Vigia e atualiza canônicos/mapa isoladamente. Somente achado material autoriza novo delta/re-freeze; integração/piloto permanecem externos.",
};
writeFileSync(receiptPath, JSON.stringify(receipt, null, 2) + "\n");
writeFileSync(
    markdownPath,
    [
        "# MARCO02 Lume — fonte estável para revisão",
        "",
        `Versão${receipt.versao}, emitido${emittedUtc}. Fonte congelada com ${source.length} arquivos idênticos ao snapshot executado. **Sem aceite independente antecipado ou integração real.**`,
        "",
        `Fonte SHA256\`${sourceHash}\`. [Manifesto integral](frontend-lume-manifesto-estavel-marco02-20261008.json), SHA256\`${receipt.fonte.sha256}\`. [Recibo JSON](frontend-lume-entrega-lucas-20261008.json). MARCO01${baseline.copied} cópias preservadas/reconferidas.`,
        "",
        `Tipagem/lint/build exit0; ${unit.numPassedTests} testes locais e ${browser.stats.expected} Chromium, zero falhas/ignorados. Os161 smokes contratuais são distintos das jornadas.`,
        "",
        "| Comando real no snapshot | Resultado | Log |",
        "| --- | --- | --- |",
        ...receipt.execucoes.map(
            (c) =>
                `| ${c.command} | exit${c.exitCode} | [log](${c.log.replace("frontend/evidencias/", "")}) |`,
        ),
        "",
        "[22 critérios operacionais, identidades/versões e provas compartilhadas](../docs/provas-jornadas-marco02.md): FE04(3),FE05(2),FE06(2),FE08(2),FE09(2),FE10(4),FE11(3),FE12(4). Todos os arquivos de prova tiveram execução verde na fonte final. FE10 percorreu documento→retirada→baixas; cancelamento→retorno interno; retirada→devolução→nova entrada. Valores são respostas preparadas, sem fórmula/decisão de negócio no FE.",
        "",
        `Lucas: **${processProof.url}**, fictício, PID${processProof.pid}, processo próprio mantido aberto. [Identidade/observação do processo](frontend-lume-exercicio-marco02.json); [CSS/DOM/6 estados desktop/coletor](${execution.replace("evidencias/", "")}/evidencias/marco02-exercicio-observado.json). ZeroAPI/externos/CSP/console/overflow. Processo existente5178 não reiniciado/encerrado/config original preservada.`,
        "",
        "Organização por responsabilidades e fronteiras, contratos exatos/omissão/null/replay/contexto, mapaBE/FE e limites estão em [organização](../docs/organizacao.md), [transporte](../docs/contratos-e-transporte.md), [cobertura](../docs/cobertura-e-limites.md) e [mapa](../docs/mapeamento-telas-contratos.md). CSSPrumo, FE12Cedro03 e DEV01integrados por bytes/referências. [Adenda lint exatoR16](adenda-r16-comando-lint.md), sem reescrever closure histórico.",
        "",
        "167 inputs têm caminho real de bytes usados, origem/sourcePath e hashes separados, incluindo gerador/overrides; não certificam interpretação regexp/permissões. R08histórico4d3b e geração atual5e9e não são confundidos. Logs vermelhos/snapshots anteriores preservados. Manifesto não se inclui nem inclui os dois recibos para evitar hash recursivo.",
        "",
        "Limites:",
        "",
        ...receipt.limites.map((text) => "- " + text),
        "",
        "Escrita de aplicação/config/testes/docs suspensa para revisão integrada. Farol controla canônicos/mapa e recebe este pacote; Vigia reconfirma a fonte final. Nenhuma mensagem Hermes ou nova conexão.",
        "",
    ].join("\n"),
);
console.log(
    JSON.stringify(
        {
            stage: receipt.marco,
            sourceHash,
            manifestHash: receipt.fonte.sha256,
            sourceFiles: source.length,
            artifacts: manifest.artifacts.length,
            contractInputs: contractInputs.length,
            unitTests: unit.numPassedTests,
            browserTests: browser.stats.expected,
            criteria: coverage.rows.length,
            runtime: processProof.url,
            frozen: true,
        },
        null,
        2,
    ),
);
