import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export const productionCsp =
    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'";

export function productionConfiguration(mode: string) {
    if (mode !== "production") throw new Error("WMS_PROD_BUILD_MODE_INVALID");
    return defineConfig({
        // Nenhum .env nem variável VITE_* herdada é publicada no navegador.
        envDir: false,
        envPrefix: [],
        base: "/",
        define: { "import.meta.env.VITE_DATA_MODE": JSON.stringify("real") },
        plugins: [
            react(),
            {
                name: "wms-production-csp",
                transformIndexHtml: {
                    order: "pre",
                    handler(html) {
                        // Endurece somente o artefato; index.html vivo é preservado.
                        return html.replace(
                            /(<meta\s+http-equiv="Content-Security-Policy"\s+content=")[^"]*(")/,
                            `$1${productionCsp}$2`,
                        );
                    },
                },
            },
        ],
        build: { sourcemap: false, emptyOutDir: false },
    });
}

export default defineConfig(({ mode }) => productionConfiguration(mode));
