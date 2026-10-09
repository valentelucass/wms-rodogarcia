import { spawnSync } from "node:child_process";
import { writeFileSync, existsSync, mkdirSync } from "node:fs";
import { resolve } from "node:path";

// Somente frontend no snapshot próprio, sem escrita de fonte durante checks.
const label = process.argv[2];
if (!/^[a-z0-9-]+$/.test(label ?? "")) throw Error("Informe rótulo novo");
const port = process.env.WMS_FE_TEST_PORT ?? "5192";
const ledger = `evidencias/${label}-checks-final.json`;
if (existsSync(ledger)) throw Error("Rodada anterior preservada: " + ledger);
mkdirSync("evidencias", { recursive: true });
const checks = [
    ["npm run typecheck", "typecheck"],
    ["npm run lint", "lint"],
    [
        `npm test -- --reporter=default --reporter=json --outputFile=evidencias/${label}-unit-resultados.json`,
        "testes",
    ],
    ["npm run build", "build"],
    ["npm run test:browser", "browser"],
];
const results = [];
for (const [command, name] of checks) {
    const log = `evidencias/${label}-${name}-final.log`;
    if (existsSync(log)) throw new Error("Preserve a rodada anterior: " + log);
    const startedAt = new Date().toISOString();
    const result = spawnSync(command, {
        shell: true,
        encoding: "utf8",
        maxBuffer: 32 * 1024 * 1024,
        windowsHide: true,
        env: {
            ...process.env,
            WMS_FE_TEST_PORT: port,
            WMS_FE_PROOF_LABEL: label,
            FE12_BASE_URL: `http://127.0.0.1:${port}`,
            PLAYWRIGHT_BROWSERS_PATH:
                process.env.PLAYWRIGHT_BROWSERS_PATH ??
                resolve(".tools/ms-playwright"),
            VITE_DATA_MODE: "ficticio",
            NO_COLOR: "1",
        },
    });
    writeFileSync(log, result.stdout + result.stderr, "utf8");
    results.push({
        command,
        cwd: process.cwd(),
        startedAt,
        finishedAt: new Date().toISOString(),
        exitCode: result.status ?? -1,
        log,
        environment: {
            WMS_FE_TEST_PORT: port,
            WMS_FE_PROOF_LABEL: label,
            FE12_BASE_URL: `http://127.0.0.1:${port}`,
            VITE_DATA_MODE: "ficticio",
        },
    });
    writeFileSync(ledger, JSON.stringify(results, null, 2) + "\n");
    console.log(`${name}: exit${result.status}; ${log}`);
    if (result.status !== 0) process.exit(result.status ?? 1);
}
