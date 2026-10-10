import { defineConfig } from "@playwright/test";
import base from "./playwright.config";
import { origin } from "./tests/browser/environment";

const mode = process.env.WMS_FE_DASH_MODE === "ficticio" ? "ficticio" : "real";
export default defineConfig({
    ...base,
    testMatch:
        mode === "real"
            ? ["dashboard-real.spec.ts", "overview-real.spec.ts"]
            : "dashboard.spec.ts",
    use: { ...base.use, channel: "chrome" },
    webServer: {
        command: `npx vite preview --host 127.0.0.1 --port ${new URL(origin).port} --strictPort --outDir .tools/graficos-inicio-${mode}`,
        url: origin,
        reuseExistingServer: false,
        timeout: 30000,
    },
});
