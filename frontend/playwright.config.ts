import { defineConfig } from "@playwright/test";
import { origin, proofLabel } from "./tests/browser/environment";
export default defineConfig({
    testDir: "tests/browser",
    outputDir: `test-results-${proofLabel}`,
    fullyParallel: false,
    workers: 1,
    retries: 0,
    reporter: [
        ["list"],
        [
            "json",
            { outputFile: `evidencias/${proofLabel}-browser-resultados.json` },
        ],
    ],
    use: {
        baseURL: origin,
        browserName: "chromium",
        headless: true,
        screenshot: "only-on-failure",
    },
    webServer: {
        command: "npm run preview",
        url: origin,
        reuseExistingServer: false,
        timeout: 30000,
    },
});
