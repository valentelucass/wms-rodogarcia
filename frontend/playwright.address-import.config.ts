import { defineConfig } from "@playwright/test";

export default defineConfig({
    testDir: "tests/browser",
    testMatch: "address-import.spec.ts",
    outputDir: ".tools/address-import-results",
    workers: 1,
    retries: 0,
    timeout: 60000,
    reporter: "list",
    use: { channel: "chrome", headless: true },
    projects: [
        { name: "build", use: { baseURL: "http://127.0.0.1:5206" } },
        { name: "dev-css", use: { baseURL: "http://127.0.0.1:5207" } },
    ],
    webServer: [
        {
            command:
                "npx vite preview --host 127.0.0.1 --port 5206 --strictPort --outDir .tools/address-import-dist",
            url: "http://127.0.0.1:5206",
            reuseExistingServer: false,
        },
        {
            command:
                "npx vite --config vite.config.ts --mode real --host 127.0.0.1 --port 5207 --strictPort",
            url: "http://127.0.0.1:5207",
            reuseExistingServer: false,
        },
    ],
});
