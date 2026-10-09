import { readFileSync } from "node:fs";
import { defineConfig, type Plugin, type ProxyOptions } from "vite";
import react from "@vitejs/plugin-react";

function backendTarget(): string {
    const port = process.env.WMS_PORT ?? "8080";
    if (!/^[1-9]\d{0,4}$/.test(port) || Number(port) > 65535) {
        throw new Error("WMS_DEV_PORTA_INVALIDA: WMS_PORT deve ser 1 a 65535.");
    }
    const target =
        process.env.WMS_DEV_BACKEND_URL ?? `http://127.0.0.1:${port}`;
    const destination = /^http:\/\/127\.0\.0\.1:([1-9]\d{0,4})\/?$/.exec(
        target,
    );
    if (!destination || Number(destination[1]) > 65535) {
        throw new Error(
            "WMS_DEV_DESTINO_INVALIDO: use http://127.0.0.1:porta, sem caminho, credencial, query ou fragmento.",
        );
    }
    const url = new URL(target);
    return url.origin;
}

// Contrato preparado, ainda sem associacao ao servidor: conserva /api/v1 sem rewrite.
export const proxyApiDev: Record<string, ProxyOptions> = {
    "/api": { target: backendTarget(), changeOrigin: false },
};

const blockRealApi: Plugin = {
    name: "wms-dev-api-bloqueada",
    configureServer(server) {
        server.middlewares.use("/api", (_request, response) => {
            response.statusCode = 503;
            response.setHeader("Content-Type", "text/plain; charset=utf-8");
            response.end(
                "API real desabilitada: D31-DEV01 aguarda guarda atual e liberacao Farol.",
            );
        });
    },
};

export default defineConfig(() => {
    const mode = process.env.VITE_DATA_MODE ?? "ficticio";
    if (mode !== "ficticio" && mode !== "real") {
        throw new Error(
            "WMS_DEV_MODO_INVALIDO: use ficticio; real permanece condicionado a guarda.",
        );
    }
    if (mode === "real") {
        let guard: { guardaRealAprovada?: boolean };
        try {
            const source = readFileSync(
                new URL(
                    "./evidencias/frontend-prumo-dev-guarda-lucas-20261008.json",
                    import.meta.url,
                ),
                "utf8",
            ).replace(/^\uFEFF/, "");
            guard = JSON.parse(source) as { guardaRealAprovada?: boolean };
        } catch {
            throw new Error(
                "WMS_DEV_GUARDA_INDISPONIVEL: modo real recusado antes de iniciar o servidor.",
            );
        }
        if (guard?.guardaRealAprovada !== true) {
            throw new Error(
                "WMS_DEV_G01_BLOQUEADO: incidente D29 sem resolucao segura comprovada; modo real recusado.",
            );
        }
        // Um booleano nao comprova resolucao/identidade/TLS/permissoes/history nem autoriza real.
        throw new Error(
            "WMS_DEV_REAL_NAO_LIBERADO: nova prova e guarda integral precisam de revisao e liberacao Farol; booleano isolado nao habilita proxy.",
        );
    }
    return {
        plugins: [react(), blockRealApi],
        define: {
            "import.meta.env.VITE_DATA_MODE": JSON.stringify("ficticio"),
        },
        server: {
            host: "127.0.0.1",
            port: 5178,
            strictPort: true,
            proxy: undefined,
        },
    };
});
