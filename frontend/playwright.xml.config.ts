import base from "./playwright.config";
import { defineConfig } from "@playwright/test";
// Chrome instalado: não depende de baixar um navegador nem reutiliza sessão do operador.
export default defineConfig({
    ...base,
    use: { ...base.use, channel: "chrome" },
});
