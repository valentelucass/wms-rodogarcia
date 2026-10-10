import { defineConfig } from "@playwright/test";
export default defineConfig({
    testDir: "tests/browser",
    testMatch: "record-colors.spec.ts",
    outputDir: "test-results-record-colors-a01",
    workers: 1,
    retries: 0,
    reporter: [
        ["list"],
        ["json", { outputFile: "evidencias/record-colors-a01-browser.json" }],
    ],
    use: {
        baseURL: "http://127.0.0.1:5223",
        channel: "chrome",
        headless: true,
        screenshot: "only-on-failure",
    },
    webServer: {
        command:
            "npx vite preview --host 127.0.0.1 --port 5223 --strictPort --outDir .tools/record-colors-a01-real",
        url: "http://127.0.0.1:5223",
        reuseExistingServer: false,
        timeout: 30000,
    },
});
