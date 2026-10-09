import { fileURLToPath } from "node:url";
import { defineConfig, type Plugin } from "vite";
import react from "@vitejs/plugin-react";
import {
    dataMode,
    frontendPort,
    type DevEnvironment,
} from "./dev/configuration";
import {
    readyFiles,
    validateReadyChain,
    type ReadyDependencies,
} from "./dev/ready-chain";
import { inspectBackend } from "./dev/process-identity";

const isolatedExercise: Plugin = {
    name: "wms-exercicio-sem-api",
    configureServer(server) {
        server.middlewares.use("/api", (_request, response) => {
            response.statusCode = 503;
            response.setHeader("Content-Type", "text/plain; charset=utf-8");
            response.end(
                "Exercicio ficticio explicito: API real indisponivel neste modo.",
            );
        });
    },
};

export function createDevConfiguration(
    mode: string,
    environment: DevEnvironment,
    dependencies: ReadyDependencies,
) {
    const selected = dataMode(mode, environment);
    const port = frontendPort(environment.WMS_FE_PORT);
    const target =
        selected === "real"
            ? validateReadyChain(environment, port, dependencies)
            : undefined;
    const binding: Plugin = {
        name: "wms-dev-binding-confirmado",
        configResolved(config) {
            if (
                config.server.host !== "127.0.0.1" ||
                config.server.port !== port ||
                config.server.strictPort !== true
            )
                throw new Error(
                    "WMS_DEV_BINDING_ALTERADO: use a porta coordenada em WMS_FE_PORT, sem override CLI.",
                );
        },
    };
    return {
        plugins: [
            react(),
            binding,
            ...(selected === "ficticio" ? [isolatedExercise] : []),
        ],
        define: {
            "import.meta.env.VITE_DATA_MODE": JSON.stringify(selected),
        },
        server: {
            host: "127.0.0.1",
            port,
            strictPort: true,
            proxy: target
                ? { "/api": { target, changeOrigin: false } }
                : undefined,
        },
    };
}

export default defineConfig(({ mode }) =>
    createDevConfiguration(mode, process.env, {
        ...readyFiles,
        projectRoot: fileURLToPath(new URL("..", import.meta.url)),
        now: Date.now(),
        inspectBackend,
    }),
);
