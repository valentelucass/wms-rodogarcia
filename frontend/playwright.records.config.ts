import { defineConfig } from "@playwright/test";

export default defineConfig({
    testDir: "tests/browser",
    testMatch: "record-pages.spec.ts",
    outputDir: "test-results-record-pages",
    workers: 1,
    retries: 0,
    reporter: [
        ["list"],
        ["json", { outputFile: "evidencias/record-pages-browser.json" }],
    ],
    use: {
        baseURL: "http://127.0.0.1:5199",
        channel: "chrome",
        headless: true,
        screenshot: "only-on-failure",
    },
    webServer: {
        command:
            "npx vite preview --host 127.0.0.1 --port 5199 --strictPort --outDir .tools/records-real",
        url: "http://127.0.0.1:5199",
        reuseExistingServer: false,
        timeout: 30000,
    },
});
