import { createHash } from "node:crypto";
import { dirname, join } from "node:path";
import { describe, expect, it } from "vitest";
import { validateReadyChain } from "../dev/ready-chain";
import { backendProcessMatches } from "../dev/process-identity";
import { readyFixture } from "./support/dev02ReadyFixture";

// Recibos/bytes/PID ficticios somente em memoria; nenhum SQL, HTTP ou processo observado.
function currentFixture() {
    const base = readyFixture();
    const hash = (bytes: Buffer) =>
        createHash("sha256").update(bytes).digest("hex");
    const runId = "00000000-0000-4000-8000-000000000001";
    const directory = join(
        base.root,
        "orchestracao/.runtime/be02-dev-fonte01/prumo/runs",
        runId,
    );
    const guard = {
        ...base.guard,
        aberturaTentativas: 1,
        aberturaConcluida: true,
        checks: Array.from({ length: 26 }, (_, n) => ({
            criterio: `PURE_ONLY_${n}`,
            passou: true,
        })),
    };
    const artifact = (path: string, content = `FICTITIOUS_BYTES ${path}`) => {
        const bytes = Buffer.from(content);
        base.files.set(path, bytes);
        return { path, sha256: hash(bytes) };
    };
    const configSource =
        "backend/src/main/resources/application-frontend-dev.properties";
    const javaHome = dirname(dirname(base.backend.java.path));
    const records = [
        "backend/mvnw",
        "backend/mvnw.cmd",
        "backend/pom.xml",
        configSource,
        "infra/dev/preparar-backend-dev.ps1",
        "infra/dev/artefato-backend.ps1",
        "infra/sql01/launcher-backend.ps1",
        "infra/sql01/Sql01Bootstrap.java",
        "infra/dev/acesso-dev.ps1",
        "infra/dev/acesso-dev.json",
        "JDK21/bin/java.exe",
        "JDK21/bin/javac.exe",
        "JDK21/release",
        "JDK21/lib/modules",
    ].map((path) => ({
        path,
        sha256:
            path === "JDK21/bin/java.exe"
                ? base.backend.java.sha256
                : artifact(
                      path.startsWith("JDK21/")
                          ? join(javaHome, path.substring(6))
                          : join(base.root, path),
                  ).sha256,
    }));
    for (const path of [
        "backend/.mvn/maven.config",
        "backend/.mvn/jvm.config",
        "backend/.mvn/extensions.xml",
    ])
        records.push({ path, sha256: "ABSENT" });
    records.sort((a, b) =>
        a.path.toLowerCase().localeCompare(b.path.toLowerCase()),
    );
    const sourceHash = hash(
        Buffer.from(
            records
                .filter(
                    (r) =>
                        r.path === "backend/pom.xml" ||
                        r.path.startsWith("backend/src/main/"),
                )
                .map((r) => `${r.path}=${r.sha256}`)
                .join("\n"),
        ),
    );
    const fingerprint = {
        schema: 1,
        sourceHash,
        records,
        sha256: hash(
            Buffer.from(records.map((r) => `${r.path}=${r.sha256}`).join("\n")),
        ),
    };
    const jar = artifact(
        join(directory, "target/wms-backend-0.0.1-SNAPSHOT.jar"),
    );
    const config = artifact(
        join(directory, "application-frontend-dev.properties"),
        base.files.get(join(base.root, configSource))!.toString(),
    );
    const candidate: Record<string, unknown> = {
        id: "BE02-DEV-FONTE01",
        runId,
        sourceHash,
        sourceHashBefore: sourceHash,
        sourceHashAfter: sourceHash,
        reused: false,
        fingerprint,
        fingerprintBefore: { sha256: fingerprint.sha256, sourceHash },
        fingerprintAfter: { sha256: fingerprint.sha256, sourceHash },
        jar,
        config,
        java: base.backend.java,
        build: {
            exit: 0,
            scope: "PURE_NO_DATABASE",
            testsExecuted: 0,
            H2: 0,
            observed: {
                testSourcesNotCompiled: true,
                testsSkipped: true,
                buildSuccess: true,
            },
        },
        package: {
            manifestSha256: "1".repeat(64),
            dependenciesSha256: "2".repeat(64),
            classes: [{ path: "FICTITIOUS_ONLY", sha256: "3".repeat(64) }],
            dependencies: [{ path: "FICTITIOUS_ONLY", sha256: "4".repeat(64) }],
        },
    };
    const jdbc: Record<string, unknown> = {
        id: "QUAL-CONF01-SQL01",
        runId,
        pid: base.backend.backend.pid,
        declaredSourceHash: sourceHash,
        declaredJarSha256: jar.sha256,
        approved: true,
        provider: "SQLServer",
        databaseProductName: "Microsoft SQL Server",
        database: "WMS_DEV",
        login: "WMSDEV",
        user: "WMSDEV",
        sessionId: 1,
        scope: "SUT_CONNECTION_METADATA_ONLY_NOT_FUNCTIONAL_TEST",
        driverName: "FICTITIOUS_DRIVER",
        driverVersion: "FICTITIOUS_VERSION",
        metadataStatementsExecuted: 1,
        connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse: true,
        privateTrustStoreAndCertificateHostConfigured: true,
        startedUtc: "2026-10-09T00:58:10Z",
        finishedUtc: "2026-10-09T00:58:11Z",
    };
    const candidateRef = {
        path: join(directory, "preparo-candidato.json"),
        sha256: "",
    };
    const jdbcRef = { path: join(directory, "jdbc-provider.json"), sha256: "" };
    const backend: Record<string, unknown> = {
        ...base.backend,
        id: "BE02-DEV-FONTE01",
        owner: "WMS - Prumo",
        runId,
        projectPath: base.root,
        state: "DEV_CURRENT_JDBC_READY",
        code: "DEV_CURRENT_READY",
        sourceHash,
        fingerprint,
        candidate: candidateRef,
        jdbc: jdbcRef,
        artifact: jar,
        config,
        bootstrapDisabled: true,
        observationEnabled: false,
        guard: {
            ...base.backend.guard,
            path: join(directory, "guarda.json"),
            receiptPath: join(directory, "guarda.json"),
        },
        backend: { ...base.backend.backend, address: "127.0.0.1" },
    };
    delete backend.status;
    const chain = {
        ...base.chain,
        id: "BE02-DEV-FONTE01",
        runId,
        fingerprint,
        candidate: candidateRef,
        jdbc: jdbcRef,
        guard: { ...base.chain.guard, path: join(directory, "guarda.json") },
        backend: {
            ...base.chain.backend,
            receiptPath: join(directory, "backend-readiness.json"),
        },
    };
    const path = join(directory, "integracao-pronta.json");
    const environment = { ...base.environment, WMS_DEV_READY_PATH: path };
    function refresh() {
        const put = (path: string, value: unknown) => {
            const bytes = Buffer.from(JSON.stringify(value));
            base.files.set(path, bytes);
            return hash(bytes);
        };
        candidateRef.sha256 = put(candidateRef.path, candidate);
        jdbcRef.sha256 = put(jdbcRef.path, jdbc);
        chain.guard.sha256 = put(chain.guard.path, guard);
        (backend.guard as Record<string, unknown>).sha256 = chain.guard.sha256;
        chain.backend.receiptSha256 = put(chain.backend.receiptPath, backend);
        base.files.set(
            join(directory, "runtime-prumo.json"),
            base.files.get(chain.backend.receiptPath)!,
        );
        environment.WMS_DEV_READY_SHA256 = put(path, chain);
    }
    refresh();
    return {
        ...base,
        runId,
        directory,
        path,
        guard,
        candidate,
        jdbc,
        backend,
        chain,
        environment,
        refresh,
    };
}

describe("BE02 validator puro: zero banco/browser/processo real", () => {
    it("aceita namespace dinamico e JDBC coerente sem inventar status HTTP", () => {
        const f = currentFixture();
        expect(f.backend).not.toHaveProperty("status");
        expect(validateReadyChain(f.environment, 25581, f.dependencies)).toBe(
            "http://127.0.0.1:25580",
        );
        expect(f.dependencies.inspectBackend).toHaveBeenCalledOnce();
    });
    it.each([
        ["approved", false],
        ["database", "WMS_PROD"],
        ["login", "sa"],
        ["provider", "H2"],
        ["pid", 999],
        ["declaredSourceHash", "0".repeat(64)],
        ["declaredJarSha256", "0".repeat(64)],
        ["metadataStatementsExecuted", 2],
        [
            "connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse",
            false,
        ],
        ["privateTrustStoreAndCertificateHostConfigured", false],
        ["finishedUtc", "2026-10-09T00:59:01Z"],
        ["startedUtc", "2026-10-09T00:57:01Z"],
    ])("recusa proof JDBC %s invalida antes inspetor", (key, value) => {
        const f = currentFixture();
        f.jdbc[key as string] = value;
        f.refresh();
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("WMS_DEV_READY_");
        expect(f.dependencies.inspectBackend).not.toHaveBeenCalled();
    });
    it.each(["id", "owner", "state", "code", "sourceHash"])(
        "recusa runtime %s divergente",
        (key) => {
            const f = currentFixture();
            f.backend[key] = "OTHER";
            f.refresh();
            expect(() =>
                validateReadyChain(f.environment, 25581, f.dependencies),
            ).toThrow("WMS_DEV_READY_");
        },
    );
    it("status200 falso nao substitui JDBC recusado", () => {
        const f = currentFixture();
        f.backend.status = {
            http: 200,
            aplicacao: "wms-rodogarcia",
            status: "DISPONIVEL",
        };
        f.jdbc.approved = false;
        f.refresh();
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("WMS_DEV_READY_");
    });
    it.each(["sourceHashBefore", "sourceHashAfter"])(
        "recusa source %s alterada",
        (key) => {
            const f = currentFixture();
            f.candidate[key] = "0".repeat(64);
            f.refresh();
            expect(() =>
                validateReadyChain(f.environment, 25581, f.dependencies),
            ).toThrow("WMS_DEV_READY_");
        },
    );
    it.each(["jdbc", "candidate", "runtime", "source"])(
        "recusa bytes %s trocados sem reexecutar banco",
        (part) => {
            const f = currentFixture();
            const path =
                part === "jdbc"
                    ? f.chain.jdbc.path
                    : part === "candidate"
                      ? f.chain.candidate.path
                      : part === "runtime"
                        ? join(f.directory, "runtime-prumo.json")
                        : join(f.root, "backend/pom.xml");
            f.files.set(path, Buffer.from("FICTITIOUS_TAMPER"));
            expect(() =>
                validateReadyChain(f.environment, 25581, f.dependencies),
            ).toThrow("HASH_MISMATCH");
        },
    );
    it("recusa guard com menos de26 e duas aberturas", () => {
        const f = currentFixture();
        f.guard.aberturaTentativas = 2;
        f.refresh();
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("CONTEXT_MISMATCH");
        f.guard.aberturaTentativas = 1;
        f.guard.checks.pop();
        f.refresh();
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("CONTEXT_MISMATCH");
    });
    it("recusa arquivo opcional declarado ausente que reapareceu", () => {
        const f = currentFixture();
        f.files.set(
            join(f.root, "backend/.mvn/maven.config"),
            Buffer.from("CHANGED"),
        );
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("FINGERPRINT_ABSENCE_INVALID");
    });
    it("recusa JDK alterado e fingerprint inline divergente", () => {
        const f = currentFixture();
        f.files.set(
            join(
                dirname(dirname((f.backend.java as { path: string }).path)),
                "lib/modules",
            ),
            Buffer.from("CHANGED"),
        );
        expect(() =>
            validateReadyChain(f.environment, 25581, f.dependencies),
        ).toThrow("HASH_MISMATCH");
        const g = currentFixture();
        g.backend.fingerprint = {
            ...(g.candidate.fingerprint as object),
            records: [],
        };
        g.refresh();
        expect(() =>
            validateReadyChain(g.environment, 25581, g.dependencies),
        ).toThrow("CONTEXT_MISMATCH");
    });
});

describe("identidade de processo pura, sem CIM nem encerramento", () => {
    const identity = {
        pid: 902,
        port: 25580,
        startUtc: "2026-10-09T00:58:01Z",
        executable: "/fake/java.exe",
    };
    const observed = {
        pid: 902,
        startUtc: identity.startUtc,
        path: identity.executable,
        listeners: [{ pid: 902, address: "127.0.0.1", port: 25580 }],
    };
    it("aceita identidade exata independente de -jar", () =>
        expect(backendProcessMatches(identity, observed)).toBe(true));
    it("recusa listener adicional do mesmo PID", () =>
        expect(
            backendProcessMatches(identity, {
                ...observed,
                listeners: [
                    ...observed.listeners,
                    { pid: 902, address: "127.0.0.1", port: 25586 },
                ],
            }),
        ).toBe(false));
    it("recusa dono/porta/endereco/inicio distintos", () => {
        for (const wrong of [
            { ...observed, pid: 903 },
            { ...observed, startUtc: "2026-10-09T00:58:02Z" },
            {
                ...observed,
                listeners: [{ pid: 903, address: "127.0.0.1", port: 25580 }],
            },
            {
                ...observed,
                listeners: [{ pid: 902, address: "0.0.0.0", port: 25580 }],
            },
            {
                ...observed,
                listeners: [{ pid: 902, address: "127.0.0.1", port: 25586 }],
            },
        ])
            expect(backendProcessMatches(identity, wrong)).toBe(false);
    });
});
