import { preview } from "vite";
import { writeFileSync } from "node:fs";

const startedAt = new Date().toISOString();
const portText = process.env.WMS_FE_TEST_PORT ?? "5188";
const label = process.env.WMS_FE_PROOF_LABEL ?? "marco02-local";
if (!/^[1-9]\d{0,4}$/.test(portText) || Number(portText) > 65535)
    throw Error("Porta própria frontend inválida");
if (!/^[a-z0-9-]+$/.test(label)) throw Error("Rótulo de prova inválido");
const port = Number(portText);
const server = await preview({
    preview: { host: "127.0.0.1", port, strictPort: true },
});
const evidence = {
    pid: process.pid,
    startedAt,
    url: `http://127.0.0.1:${port}`,
    cwd: process.cwd(),
    command: "node tools/preview.mjs",
    mode: "ficticio",
    ownsProcess: true,
};
writeFileSync(
    `evidencias/${label}-browser-processo.json`,
    JSON.stringify(evidence, null, 2),
);
server.printUrls();
console.log(`Frontend próprio PID ${process.pid}; somente exercício fictício.`);
for (const signal of ["SIGINT", "SIGTERM"])
    process.on(signal, () => {
        server.httpServer.close(() => process.exit(0));
    });
