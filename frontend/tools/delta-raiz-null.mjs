import { readFileSync, writeFileSync, mkdirSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import path from "node:path";
const output = "evidencias/delta-raiz-null-r09-20261008";
if (existsSync(output)) throw Error("Delta existente imutável");
const files = ["src/contracts/codec.ts", "tests/root-null-response.test.tsx"];
const logs = ["evidencias/raiz-null-red.log", "evidencias/raiz-null-green-v01.log", "evidencias/raiz-null-green-v02.log", "evidencias/raiz-null-green-v03.log", "evidencias/raiz-null-lint.log", "evidencias/raiz-null-formato.log"];
const copy = (relative) => {
    const bytes = readFileSync(relative), target = path.join(output, relative);
    mkdirSync(path.dirname(target), { recursive: true });
    writeFileSync(target, bytes);
    return { path: relative, bytes: bytes.length, sha256: createHash("sha256").update(bytes).digest("hex") };
};
mkdirSync(output, { recursive: true });
const receipt = {
    id: "D31-FE-VIG-004-RAIZ-NULL-R09", at: new Date().toISOString(),
    flavour: "DELTA-FOCAL-RAIZ-NULL-SEM-ACEITE-GLOBAL",
    base: { path: "evidencias/snapshot-marco02-r08/frontend", manifest: "47214bf3cae246e18c1cd0b9f15f1aaf26c80be4ae25804daa3ce9751353cb91" },
    files: files.map(copy), logs: logs.map(copy),
    changes: "parseResponse exige estrutura não nula na raiz DTO/lista/página; nulabilidade dos membros continua por Field. Cliente preserva status/correlação e incerteza de escrita, sem alteração.",
    checks: [
        { command: "npm test -- tests/root-null-response.test.tsx", log: "evidencias/raiz-null-red.log", result: "RED 6/7 antes da correção, incluindo reprodução Receipt data:null" },
        { command: "npm test -- tests/root-null-response.test.tsx", log: "evidencias/raiz-null-green-v03.log", exit: 0, result: "GREEN 7/7; raiz DTO/lista/página, NON_NULL/boxed legítimos, GET/escrita HTTP200 null e replay UI UUID+payload exatos" },
        { command: "npx eslint src/contracts/codec.ts tests/root-null-response.test.tsx", log: "evidencias/raiz-null-lint.log", exit: 0 },
    ],
    retainedIntermediateRed: "green-v02 contém 1 RED independente em expectativa de SKU no teste de workflow antigo; não apresentado como regressão verde/global. Confronto com referência real do item pendente na elaboração de jornadas.",
    network: "fetcher fictício injetado; zero chamadas HTTP/backend/SQL", sourceFrozen: "Somente os dois arquivos copiados deste delta; aplicação global continua draft sob integração.",
};
writeFileSync(path.join(output, "manifesto.json"), JSON.stringify(receipt, null, 2) + "\n");
console.log(JSON.stringify({ output, manifestSha256: createHash("sha256").update(readFileSync(path.join(output,"manifesto.json"))).digest("hex"), files: receipt.files }, null, 2));
