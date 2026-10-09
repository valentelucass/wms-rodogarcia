import { createHash } from "node:crypto";
import { join, resolve } from "node:path";
import { vi } from "vitest";
import type { ReadyDependencies } from "../../dev/ready-chain";

/** Tudo em memoria: nao grava recibo aprovado nem observa PID/backend/SQL reais. */
export function readyFixture() {
    const root = resolve("/wms-dev02-test-only");
    const runId = "00000000000040008000000000000001";
    const directory = join(
        root,
        "orchestracao/.runtime/frontend-integracao-dev-runs",
        runId,
    );
    const now = Date.parse("2026-10-09T01:00:00Z");
    const files = new Map<string, Buffer>();
    const hash = (value: Buffer) =>
        createHash("sha256").update(value).digest("hex");
    const artifact = (path: string) => {
        const bytes = Buffer.from("ONLY-FICTITIOUS-BYTES " + path);
        files.set(path, bytes);
        return { path, sha256: hash(bytes) };
    };
    const helper = artifact(join(root, "infra/dev02/guarda-wmsdev.ps1"));
    const guard = {
        guardaRealAprovada: true,
        estado: "GUARDA_ATUAL_APROVADA",
        identidadeConfirmada: true,
        inicioUtc: "2026-10-09T00:57:00Z",
        observadoEm: "2026-10-09T00:58:00Z",
        pidGuarda: 901,
        auxiliar: {
            arquivo: "infra/dev02/guarda-wmsdev.ps1",
            sha256: helper.sha256,
        },
        alvo: {
            banco: "WMS_DEV",
            login: "WMSDEV",
            usuario: "WMSDEV",
            estado: "ONLINE",
        },
        tls: {
            encrypt: "Mandatory",
            trustServerCertificate: false,
            handshakeConcluido: true,
            confirmacaoAtual: true,
            certificateSha256: "a".repeat(64),
            truststoreSha256: "b".repeat(64),
            notAfter: "2027-01-01T00:00:00Z",
        },
        direitos: { source: "FIXTURE_ONLY" },
        catalogo: { source: "FIXTURE_ONLY" },
        historico: [{ source: "FIXTURE_ONLY" }],
        checks: [{ criterio: "FIXTURE_ONLY", passou: true }],
    };
    const backend = {
        natureza: "D31_DEV02_BACKEND_READINESS",
        runId,
        estado: "READY",
        observadoUtc: "2026-10-09T00:59:00Z",
        profile: "sqlserver-dev",
        database: "WMS_DEV",
        login: "WMSDEV",
        guard: {
            sha256: "",
            processId: 901,
            helperSha256: helper.sha256,
            finishedUtc: guard.observadoEm,
        },
        artifact: artifact(join(root, "backend/app.jar")),
        config: artifact(join(root, "backend/config.properties")),
        java: artifact(join(root, "jdk/bin/java.exe")),
        backend: {
            pid: 902,
            port: 25580,
            startUtc: "2026-10-09T00:58:01Z",
            url: "http://127.0.0.1:25580",
            listenerOwned: true,
        },
        status: {
            http: 200,
            aplicacao: "wms-rodogarcia",
            status: "DISPONIVEL",
        },
    };
    const chain = {
        natureza: "D31_DEV02_INTEGRACAO_PRONTA",
        runId,
        projectPath: root,
        observadoUtc: "2026-10-09T00:59:01Z",
        publicIdentity: {
            issuer: "https://identity.invalid",
            jwkSetUri: "https://identity.invalid/jwks",
            audience: "FIXTURE_ONLY",
        },
        guard: {
            path: join(directory, "guarda.json"),
            sha256: "",
            helperSha256: helper.sha256,
            processId: 901,
        },
        backend: {
            receiptPath: join(directory, "backend-readiness.json"),
            receiptSha256: "",
            pid: 902,
            url: backend.backend.url,
        },
        frontend: {
            port: 25581,
            modo: "real",
            proxyPrefix: "/api",
            mockFallback: false,
        },
        authenticatedBusinessRead: false,
    };
    const path = join(directory, "integracao-pronta.json");
    const environment = {
        WMS_FE_PORT: "25581",
        WMS_DEV_READY_PATH: path,
        WMS_DEV_READY_SHA256: "",
        WMS_DEV_BACKEND_URL: backend.backend.url,
    };
    const refresh = () => {
        const bytes = (record: unknown) => Buffer.from(JSON.stringify(record));
        files.set(chain.guard.path, bytes(guard));
        chain.guard.sha256 = hash(files.get(chain.guard.path)!);
        backend.guard.sha256 = chain.guard.sha256;
        files.set(chain.backend.receiptPath, bytes(backend));
        chain.backend.receiptSha256 = hash(
            files.get(chain.backend.receiptPath)!,
        );
        files.set(path, bytes(chain));
        environment.WMS_DEV_READY_SHA256 = hash(files.get(path)!);
    };
    const change = (name: string, value: unknown) => {
        const keys = name.split(".");
        let record = { guard, backend, chain } as Record<string, unknown>;
        for (const key of keys.slice(0, -1))
            record = record[key] as Record<string, unknown>;
        record[keys.at(-1)!] = value;
        refresh();
    };
    const inspectBackend = vi.fn(() => true);
    const dependencies: ReadyDependencies = {
        projectRoot: root,
        now,
        inspectBackend,
        canonical: (value) => value,
        read: vi.fn((value) => {
            const bytes = files.get(value);
            if (!bytes) throw new Error("FICTITIOUS_FILE_MISSING");
            return bytes;
        }),
    };
    refresh();
    return {
        root,
        environment,
        dependencies,
        files,
        chain,
        guard,
        backend,
        change,
    };
}
