import { createServer } from "vite";
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";
import { resolve } from "node:path";

// Processo novo, somente o snapshot fictício corrigido. Nunca altera existentes.
if (process.env.VITE_DATA_MODE !== "ficticio")
    throw Error("Somente exercício fictício; G01 permanece bloqueada");
const source = "src/modules/financeiro/ExampleBilling.ts";
const sourceSha256 = createHash("sha256").update(readFileSync(source)).digest("hex");
if (sourceSha256 !== "cd21738ca19feb16993855e3fd41e680f08adcd370df48bfac2a45a5af33757e")
    throw Error("Snapshot de rejeição não corresponde à fonte estabilizada");
const output = "evidencias/runtime-5189.json";
if (existsSync(output)) throw Error("Processo/recibo anterior preservado");
const server = await createServer({
    configFile: resolve("vite.dev.config.ts"),
    server: { host: "127.0.0.1", port: 5189, strictPort: true },
});
if (server.config.server.proxy !== undefined) {
    await server.close();
    throw Error("Fictício exige proxy ausente antes de escutar");
}
await server.listen();
writeFileSync(output, JSON.stringify({
    owner: "WMS - Lume", pid: process.pid, startedAt: new Date().toISOString(),
    cwd: process.cwd(), mode: "ficticio", url: "http://127.0.0.1:5189",
    command: "node frontend/evidencias/fe-vig-008/servir-snapshot-corrigido.mjs",
    devScript: "vite --config vite.dev.config.ts", configuration: "vite.dev.config.ts",
    source, sourceSha256, proxyAbsent: true,
    observedRunning: server.httpServer?.listening === true,
    existing5178Untouched: true, existing5188Untouched: true, keptRunning: true,
}, null, 2) + "\n");
server.printUrls();
for (const signal of ["SIGINT", "SIGTERM"])
    process.on(signal, async () => { await server.close(); process.exit(0); });
