import { defineConfig } from "@playwright/test";
import { resolve } from "node:path";
const stage = process.env.WMS_PERF01_BROWSER_STAGE ?? "browser-final";
const output = resolve("../orchestracao/.runtime/qual-conf01/perf01/lume");
export default defineConfig({
    testDir: "tests/browser",
    testMatch: "qual-conf01-perf01.spec.ts",
    outputDir: resolve(output, stage + "-artifacts"),
    workers: 1, retries: 0, timeout: 30000,
    reporter: [["list"], ["json", { outputFile: resolve(output, stage + ".json") }]],
    use: { baseURL: "https://1z8126n0-25581.brs.devtunnels.ms", browserName: "chromium", channel: "chrome", headless: true, serviceWorkers: "block", screenshot: "only-on-failure" },
    // Nenhum webServer. Todas as requisições são cumpridas ou recusadas localmente.
});
