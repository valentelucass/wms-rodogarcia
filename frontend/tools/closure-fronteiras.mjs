import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
const label = process.argv[2];
const gen01=process.argv[3]==="gen01";
if (!/^marco02-r\d+$/.test(label ?? "")) throw Error("Rótulo requerido");
const target = path.join("evidencias", "snapshot-" + label);
const output = path.join(target, "closure-fronteiras.json");
if (existsSync(output)) throw Error("Closure existente imutável");
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
const snapshot = JSON.parse(readFileSync(path.join(target, "manifesto.json"), "utf8"));
const inputs = JSON.parse(readFileSync("evidencias/fontes-contratos.json", "utf8"));
const contractInputs = Object.entries(inputs.fontes).map(([sourcePath, sha256]) => {
    const bytes = readFileSync(path.join("..", sourcePath));
    if (hash(bytes) !== sha256) throw Error("Entrada atual divergente: " + sourcePath);
    const copied = path.join(target, "contractInputs", sourcePath);
    mkdirSync(path.dirname(copied), { recursive: true });
    writeFileSync(copied, bytes);
    return { path: copied.replaceAll("\\", "/"), sourcePath, origin: "fonte atual em leitura", sha256, roles: inputs.leituras[sourcePath] };
});
const manifestTarget = path.join(target, "frontend", "evidencias", "fontes-contratos.json");
writeFileSync(manifestTarget, readFileSync("evidencias/fontes-contratos.json"));
const proofLogs = ["workflow-currentness-red.log", "workflow-currentness-green-v01.log", "workflow-currentness-green-v02.log", "workflow-currentness-lint.log", "gen01-fonte-atual-r09.log", "gen01-fonte-atual-r09.json"];
for (const file of proofLogs) writeFileSync(path.join(target, "frontend", "evidencias", file), readFileSync(path.join("evidencias", file)));
const receipt = {
    flavour: "LUME-FRONTEIRAS-" + label.toUpperCase() + "-FOCAL-SEM-ACEITE-GLOBAL",
    at: new Date().toISOString(), snapshot: path.join(target, "frontend").replaceAll("\\", "/"),
    snapshotManifestSha256: hash(readFileSync(path.join(target, "manifesto.json"))),
    closure: snapshot.files, contractInputs, generatedOutputs: inputs.gerados,
    inputManifest: { path: manifestTarget.replaceAll("\\", "/"), sha256: hash(readFileSync(manifestTarget)) },
    commands: [
        { command: gen01?"npm test -- tests/dispatch-profile-contract.test.tsx tests/string-constraints.test.ts tests/root-null-response.test.tsx --reporter=json --outputFile=evidencias/perfis-resultados.json":"npm test -- tests/workflow-currentness.test.ts tests/marco02-boundaries.test.ts tests/vigia-boundaries.test.ts tests/workflow.test.ts tests/root-null-response.test.tsx", log: gen01?"frontend/evidencias/perfis-fronteiras.log":"frontend/evidencias/focais-fronteiras.log", exit: 0 },
        { command: "npm run typecheck", log: gen01?"frontend/evidencias/typecheck.log":"frontend/evidencias/typecheck-fronteiras.log", exit: 0 },
        { command: "npx eslint [arquivos alterados workflow/references/testes focais]", log: gen01?"frontend/evidencias/perfis-lint.log":"frontend/evidencias/workflow-currentness-lint.log", exit: 0 },
        { command: "uv run --offline --no-project --python 3.12 tools/contratos.py", log: gen01?"frontend/evidencias/gen01-perfil-condicional-r13.log":"frontend/evidencias/gen01-fonte-atual-r09.log", exit: 0, result: gen01?"167 entradas atuais verificadas; schemas/types iguais, endpoints mudam apenas apresentação condicional de Expedição; hashes não certificam interpretação":"167 entradas atuais verificadas; três outputs idênticos em bytes ao R08; delta somente input XmlService relatado" },
    ],
    changes: ["Coleções presentes no detalhe autoritativo substituem apenas filhos do pai correspondente; resumo/consulta parcial preservam irmãos", "Reseleção de objeto fora do catálogo atual recusada; remoção e substituição de filho mesmo pai/revisão provadas", "Raiz DTO/lista/página null recusada; 2xx escrito incompatível conserva replay", "SKU fallback pertence aos itens do próprio pedido; expectativa focal atualizada para SKU legítimo do item", "Detalhe recebimento conservado para conferência visível; lookup unidade usa pedidoUnidadeId"],
    transferredSlices: gen01?"CSS/entrypoint/config DEV01 Prumo estáveis + FE12 Cedro snapshot03 estável integrados; closure focal não é freeze geral da aplicação":"Regularização/CSS/entrypoint deste snapshot usam handoff imutável original; novas fatias Cedro/Prumo são revisadas e integradas separadamente. Closure focal não é fonte final integrada.",
    limits: "Sem suíte geral, HTTP real, backend/SQL. R08, inputs históricos/adenda e MARCO01 preservados.",
};
if(gen01) {
    writeFileSync(path.join(target,"frontend/evidencias/gen01-perfil-condicional-r13.log"),readFileSync("evidencias/gen01-perfil-condicional-r13.log"));
    receipt.changes=["Expedição ler/separar mínimo de apresentação OPERACAO; demais mutações SUPERVISOR. Guarda Gestor do backend é condicional, não global","Prova UI Operação/Supervisor executa os dois comandos comuns com IDs, revisão, UUID e destino atuais; resolverPendentes bloqueado e false. Gestor explicita true","Defaults de apresentação não herdam valores proibidos do contexto; backend segue autoridade","NotNull String sem NotBlank/min permite vazio e transporta string, não null; raiz DTO/lista/página segue recusando null"];
    receipt.historicalCorrection="R10 literal é histórico e tinha mínimo GESTOR incorreto para caminho comum. Leitura/hash 167 não certificou interpretação de guardas condicionais. R16 documenta e prova a correção; originals/adenda preservados.";
}
writeFileSync(output, JSON.stringify(receipt, null, 2) + "\n");
console.log(JSON.stringify({ output, sha256: hash(readFileSync(output)), sourceFiles: snapshot.files.length, contractInputs: contractInputs.length }, null, 2));
