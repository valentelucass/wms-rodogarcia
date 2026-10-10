import { defineConfig } from "@playwright/test";
import { resolve } from "node:path";

const output = resolve("../orchestracao/.runtime/qual-conf01/lume");
export default defineConfig({
    testDir: "tests/browser",
    testMatch: "qual-conf01.spec.ts",
    outputDir: resolve(output, "browser-final-artifacts"),
    workers: 1,
    retries: 0,
    timeout: 30000,
    reporter: [
        ["list"],
        ["json", { outputFile: resolve(output, "browser.json") }],
    ],
    use: {
        baseURL: "https://1z8126n0-25581.brs.devtunnels.ms",
        browserName: "chromium",
        channel: "chrome",
        headless: true,
        serviceWorkers: "block",
        screenshot: "only-on-failure",
    },
    // Sem webServer: todos os recursos são cumpridos pela interceptação do build.
});
