import { describe, expect, it } from "vitest";
import { readFileSync } from "node:fs";
import { dataMode, frontendPort, backendTarget } from "../dev/configuration";
import { validateReadyChain } from "../dev/ready-chain";
import { createDevConfiguration } from "../vite.dev.config";
import { readyFixture } from "./support/dev02ReadyFixture";

describe("DEV02 core: fixtures/inspetor locais; zero HTTP/SQL/processo backend", () => {
    it("npm dev e REAL; ficticio exige comando distinto, sem fallback de env", () => {
        const scripts = JSON.parse(
            readFileSync("package.json", "utf8"),
        ).scripts;
        expect(scripts.dev).toBe(
            "vite --config vite.dev.config.ts --mode real",
        );
        expect(scripts["dev:ficticio"]).toBe(
            "vite --config vite.dev.config.ts --mode ficticio",
        );
        expect(dataMode("development", {})).toBe("real");
        expect(() => dataMode("real", { VITE_DATA_MODE: "ficticio" })).toThrow(
            "CONFLITANTE",
        );
        expect(() => dataMode("other", {})).toThrow("MODO_INVALIDO");
    });
    it("ausencia de chain nao inicia real nem observa processo", () => {
        const f = readyFixture();
        expect(() =>
            createDevConfiguration(
                "real",
                { WMS_FE_PORT: "25581" },
                f.dependencies,
            ),
        ).toThrow("READY_MISSING");
        expect(f.dependencies.read).not.toHaveBeenCalled();
        expect(f.dependencies.inspectBackend).not.toHaveBeenCalled();
    });
    it("ficticio explicito nao le guarda nem cria proxy, mesmo com target informado", () => {
        const f = readyFixture();
        const config = createDevConfiguration(
            "ficticio",
            f.environment,
            f.dependencies,
        );
        expect(config.server.proxy).toBeUndefined();
        expect(config.define["import.meta.env.VITE_DATA_MODE"]).toBe(
            '"ficticio"',
        );
        expect(f.dependencies.read).not.toHaveBeenCalled();
        expect(f.dependencies.inspectBackend).not.toHaveBeenCalled();
    });
    it("chain ficticia coerente prepara proxy /api exato sem rewrite; nao prova ambiente real", () => {
        const f = readyFixture();
        const config = createDevConfiguration(
            "real",
            f.environment,
            f.dependencies,
        );
        expect(config.server.proxy).toEqual({
            "/api": { target: "http://127.0.0.1:25580", changeOrigin: false },
        });
        expect(config.server).toMatchObject({
            host: "127.0.0.1",
            port: 25581,
            strictPort: true,
        });
        expect(f.dependencies.inspectBackend).toHaveBeenCalledWith({
            pid: 902,
            port: 25580,
            startUtc: "2026-10-09T00:58:01Z",
            executable: f.backend.java.path,
        });
    });
    it.each([
        ["guard.guardaRealAprovada", false],
        ["guard.alvo", null],
        ["guard.alvo.banco", "WMS_PROD"],
        ["guard.alvo.login", "sa"],
        ["guard.tls.handshakeConcluido", false],
        ["guard.tls.trustServerCertificate", true],
        ["guard.direitos", null],
        ["guard.catalogo", null],
        ["guard.historico", null],
        ["guard.checks", []],
        ["guard.pidGuarda", 999],
        ["guard.inicioUtc", "2026-10-08T00:00:00Z"],
        ["backend.database", "WMS_PROD"],
        ["backend.profile", "sqlserver-prod"],
        ["backend.backend.pid", 903],
        ["backend.backend.listenerOwned", false],
        ["backend.guard.processId", 999],
        ["backend.guard.helperSha256", "c".repeat(64)],
        ["backend.guard.finishedUtc", "2026-10-09T00:57:00Z"],
        ["backend.status.http", 401],
        ["chain.frontend.mockFallback", true],
        ["chain.frontend.proxyPrefix", "/"],
        ["chain.frontend.port", 5178],
        ["chain.publicIdentity.audience", ""],
        ["chain.publicIdentity.issuer", "http://127.0.0.1:9999"],
        [
            "chain.publicIdentity.jwkSetUri",
            "https://identity.invalid/jwks?secret=forbidden",
        ],
    ])("recusa %s=%s antes do inspetor", (path, value) => {
        const f = readyFixture();
        f.change(path as string, value);
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("WMS_DEV_");
        expect(f.dependencies.inspectBackend).not.toHaveBeenCalled();
    });
    it("guarda ATUAL bloqueada Prumo e recusada mesmo encadeada com hash correto", () => {
        const f = readyFixture();
        const observed = JSON.parse(
            readFileSync(
                "evidencias/frontend-prumo-dev02-guarda-lucas-20261008.json",
                "utf8",
            ).replace(/^\uFEFF/, ""),
        );
        expect(observed.estado).toBe("GUARDA_ATUAL_BLOQUEADA");
        expect(observed.aberturaTentativas).toBe(1);
        Object.assign(f.guard, observed);
        f.change("guard.guardaRealAprovada", observed.guardaRealAprovada);
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("CONTEXT_MISMATCH");
        expect(f.dependencies.inspectBackend).not.toHaveBeenCalled();
    });
    it.each([
        "chain",
        "guard",
        "backend",
        "artifact",
        "config",
        "java",
        "helper",
    ])("bytes alterados de %s recusados", (part) => {
        const f = readyFixture();
        const path =
            part === "chain"
                ? f.environment.WMS_DEV_READY_PATH
                : part === "guard"
                  ? f.chain.guard.path
                  : part === "backend"
                    ? f.chain.backend.receiptPath
                    : part === "helper"
                      ? [...f.files.keys()].find((p) =>
                            p.endsWith("guarda-wmsdev.ps1"),
                        )!
                      : f.backend[part as "artifact" | "config" | "java"].path;
        f.files.set(path, Buffer.from("TAMPERED FICTITIOUS BYTES"));
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("HASH_MISMATCH");
        expect(f.dependencies.inspectBackend).not.toHaveBeenCalled();
    });
    it("mesmo PID com inicio/listener divergente nao habilita proxy", () => {
        const f = readyFixture();
        f.dependencies.inspectBackend = () => false;
        expect(() =>
            createDevConfiguration("real", f.environment, f.dependencies),
        ).toThrow("PROCESS_IDENTITY_MISMATCH");
    });
    it("recusa link/substituicao de caminho da chain", () => {
        const f = readyFixture();
        f.dependencies.canonical = (path) => path + "-different";
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("LINK_REFUSED");
    });
    it.each([undefined, "0", "65536", "25581suffix"])(
        "porta %s recusada",
        (port) => expect(() => frontendPort(port)).toThrow("PORTA_INVALIDA"),
    );
    it.each([
        undefined,
        "http://localhost:25580",
        "http://127.0.0.1:25580/api",
        "http://user:pass@127.0.0.1:25580",
        "http://127.0.0.1:25580?x=1",
    ])("target %s recusado", (target) =>
        expect(() => backendTarget(target)).toThrow("DESTINO_INVALIDO"),
    );
});
