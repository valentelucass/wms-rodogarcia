import { createServer } from "vite";
import { writeFileSync, existsSync } from "node:fs";
import { resolve } from "node:path";

// Mesmo contrato do npm run dev:ficticio, em porta própria para o exercício congelado.
// A configuração Prumo continua responsável pela CSP/proxy/guarda G01.
if ((process.env.VITE_DATA_MODE ?? "ficticio") !== "ficticio")
    throw Error(
        "Exercício próprio exige ficticio; G01 real permanece bloqueada",
    );
const evidence = "evidencias/frontend-lume-exercicio-marco02.json";
if (existsSync(evidence)) throw Error("Processo/recibo anterior preservado");
process.env.WMS_FE_PORT ??= "5188";
const server = await createServer({
    configFile: resolve("vite.dev.config.ts"),
    mode: "ficticio",
    server: { host: "127.0.0.1", port: 5188, strictPort: true },
});
await server.listen();
writeFileSync(
    evidence,
    JSON.stringify(
        {
            pid: process.pid,
            owner: "WMS - Lume",
            startedAt: new Date().toISOString(),
            cwd: process.cwd(),
            url: "http://127.0.0.1:5188",
            mode: "ficticio",
            command: "node tools/start-exercise.mjs",
            devScript: "vite --config vite.dev.config.ts --mode ficticio",
            configuration: "vite.dev.config.ts",
            proxyAbsent: server.config.server.proxy === undefined,
            observedRunning: server.httpServer?.listening === true,
            existing5178Untouched: true,
            proof: "listen próprio concluído; observação HTTP/DOM em marco02-exercicio-observado.json",
            keptRunning: true,
        },
        null,
        2,
    ) + "\n",
);
server.printUrls();
for (const signal of ["SIGINT", "SIGTERM"])
    process.on(signal, async () => {
        await server.close();
        process.exit(0);
    });
