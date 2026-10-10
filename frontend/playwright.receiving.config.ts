import { defineConfig } from "@playwright/test";
export default defineConfig({
    testDir: "tests/browser",
    testMatch: "receiving-orders.spec.ts",
    outputDir: "test-results-receiving-ped01",
    workers: 1,
    retries: 0,
    reporter: [
        ["list"],
        ["json", { outputFile: "evidencias/receiving-ped01-browser.json" }],
    ],
    use: {
        baseURL: "http://127.0.0.1:5222",
        channel: "chrome",
        headless: true,
        screenshot: "only-on-failure",
    },
    webServer: {
        command:
            "npx vite preview --host 127.0.0.1 --port 5222 --strictPort --outDir .tools/receiving-ped01-real",
        url: "http://127.0.0.1:5222",
        reuseExistingServer: false,
        timeout: 30000,
    },
});
