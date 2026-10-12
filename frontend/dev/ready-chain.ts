import { createHash } from "node:crypto";
import { readFileSync, realpathSync } from "node:fs";
import { dirname, isAbsolute, join, relative, resolve } from "node:path";
import {
    backendTarget,
    requirePublicIdentity,
    type DevEnvironment,
} from "./configuration";

type ObjectValue = Record<string, unknown>;
export interface BackendIdentity {
    pid: number;
    startUtc: string;
    port: number;
    executable: string;
}
export interface ReadyDependencies {
    projectRoot: string;
    now: number;
    read: (path: string) => Buffer;
    canonical: (path: string) => string;
    inspectBackend: (identity: BackendIdentity) => boolean;
}
export const readyFiles = {
    read: (path: string) => readFileSync(path),
    canonical: (path: string) => realpathSync(path),
};

function refuse(reason: string): never {
    throw new Error("WMS_DEV_READY_" + reason);
}
function object(value: unknown): ObjectValue {
    if (!value || typeof value !== "object" || Array.isArray(value))
        refuse("CONTRACT_INVALID");
    return value as ObjectValue;
}
function text(value: unknown): string {
    if (typeof value !== "string" || !value) refuse("CONTRACT_INVALID");
    return value;
}
function integer(value: unknown): number {
    if (typeof value !== "number" || !Number.isSafeInteger(value) || value < 1)
        refuse("CONTRACT_INVALID");
    return value;
}
function equal(actual: unknown, expected: unknown): void {
    if (actual !== expected) refuse("CONTEXT_MISMATCH");
}
const digest = (bytes: Buffer) =>
    createHash("sha256").update(bytes).digest("hex");
const validSha = (value: unknown) => {
    const sha = text(value).toLowerCase();
    if (!/^[a-f0-9]{64}$/.test(sha)) refuse("HASH_INVALID");
    return sha;
};
function current(value: unknown, now: number): number {
    const instant = Date.parse(text(value));
    if (
        !Number.isFinite(instant) ||
        instant > now + 5000 ||
        now - instant > 600000
    )
        refuse("EXPIRED_OR_INVALID_TIME");
    return instant;
}
function checkedBytes(
    path: string,
    sha: unknown,
    dependencies: ReadyDependencies,
): Buffer {
    const expected = text(sha).toLowerCase();
    if (!/^[a-f0-9]{64}$/.test(expected)) refuse("HASH_INVALID");
    let bytes: Buffer;
    try {
        bytes = dependencies.read(path);
    } catch {
        return refuse("FILE_UNAVAILABLE");
    }
    if (digest(bytes) !== expected) refuse("HASH_MISMATCH");
    return bytes;
}
function hashed(
    path: string,
    sha: unknown,
    dependencies: ReadyDependencies,
): ObjectValue {
    const bytes = checkedBytes(path, sha, dependencies);
    try {
        return object(
            JSON.parse(bytes.toString("utf8").replace(/^\uFEFF/, "")),
        );
    } catch {
        return refuse("RECEIPT_INVALID");
    }
}
function sameFile(
    actual: unknown,
    expected: string,
    dependencies: ReadyDependencies,
): string {
    const path = text(actual);
    if (
        !isAbsolute(path) ||
        resolve(path).toLowerCase() !== resolve(expected).toLowerCase()
    )
        refuse("PATH_INVALID");
    let canonical: string;
    try {
        canonical = dependencies.canonical(path);
    } catch {
        return refuse("FILE_UNAVAILABLE");
    }
    if (resolve(canonical).toLowerCase() !== resolve(path).toLowerCase())
        refuse("LINK_REFUSED");
    return path;
}

function currentBackendProof(
    chain: ObjectValue,
    backend: ObjectValue,
    root: string,
    directory: string,
    runId: string,
    dependencies: ReadyDependencies,
): { started: number; finished: number } {
    equal(backend.id, "BE02-DEV-FONTE01");
    equal(backend.owner, "WMS - Prumo");
    equal(backend.state, "DEV_CURRENT_JDBC_READY");
    equal(backend.code, "DEV_CURRENT_READY");
    equal(resolve(text(backend.projectPath)).toLowerCase(), root.toLowerCase());
    equal(backend.bootstrapDisabled, true);
    equal(backend.observationEnabled, false);
    const sourceHash = validSha(backend.sourceHash);
    const backendGuard = object(backend.guard);
    equal(backendGuard.path, join(directory, "guarda.json"));
    equal(backendGuard.receiptPath, join(directory, "guarda.json"));
    sameFile(
        object(backend.artifact).path,
        join(directory, "target/wms-backend-0.0.1-SNAPSHOT.jar"),
        dependencies,
    );
    sameFile(
        object(backend.config).path,
        join(directory, "application-frontend-dev.properties"),
        dependencies,
    );
    const candidateRef = object(backend.candidate);
    const candidatePath = sameFile(
        candidateRef.path,
        join(directory, "preparo-candidato.json"),
        dependencies,
    );
    const candidate = hashed(candidatePath, candidateRef.sha256, dependencies);
    equal(candidate.id, "BE02-DEV-FONTE01");
    equal(candidate.runId, runId);
    equal(validSha(candidate.sourceHash), sourceHash);
    equal(validSha(candidate.sourceHashBefore), sourceHash);
    equal(validSha(candidate.sourceHashAfter), sourceHash);
    equal(candidate.reused, false);
    const build = object(candidate.build);
    equal(build.exit, 0);
    equal(build.scope, "PURE_NO_DATABASE");
    equal(build.testsExecuted, 0);
    equal(build.H2, 0);
    for (const name of [
        "testSourcesNotCompiled",
        "testsSkipped",
        "buildSuccess",
    ])
        equal(object(build.observed)[name], true);
    for (const name of ["candidate", "jdbc"]) {
        const first = object(chain[name]);
        const second = object(backend[name]);
        equal(first.path, second.path);
        equal(validSha(first.sha256), validSha(second.sha256));
    }
    const fingerprint = object(candidate.fingerprint);
    equal(fingerprint.schema, 1);
    equal(validSha(fingerprint.sourceHash), sourceHash);
    const fingerprintHash = validSha(fingerprint.sha256);
    for (const name of ["fingerprintBefore", "fingerprintAfter"]) {
        const observed = object(candidate[name]);
        equal(validSha(observed.sha256), fingerprintHash);
        equal(validSha(observed.sourceHash), sourceHash);
    }
    for (const value of [chain.fingerprint, backend.fingerprint]) {
        const linked = object(value);
        equal(linked.schema, 1);
        equal(validSha(linked.sha256), fingerprintHash);
        equal(validSha(linked.sourceHash), sourceHash);
        if (
            !Array.isArray(linked.records) ||
            !Array.isArray(fingerprint.records)
        )
            refuse("FINGERPRINT_INCOMPLETE");
        equal(linked.records.length, fingerprint.records.length);
        linked.records.forEach((entry, index) => {
            const record = object(entry);
            const original = object((fingerprint.records as unknown[])[index]);
            equal(record.path, original.path);
            equal(record.sha256, original.sha256);
        });
    }
    if (!Array.isArray(fingerprint.records) || !fingerprint.records.length)
        refuse("FINGERPRINT_INCOMPLETE");
    const paths = new Set<string>();
    const optional = new Set([
        "backend/.mvn/maven.config",
        "backend/.mvn/jvm.config",
        "backend/.mvn/extensions.xml",
    ]);
    const helpers = new Set([
        "infra/dev/preparar-backend-dev.ps1",
        "infra/dev/artefato-backend.ps1",
        "infra/sql01/launcher-backend.ps1",
        "infra/sql01/Sql01Bootstrap.java",
        "infra/dev/acesso-dev.ps1",
        "infra/dev/acesso-dev.json",
    ]);
    const jdk = new Set([
        "JDK21/bin/java.exe",
        "JDK21/bin/javac.exe",
        "JDK21/release",
        "JDK21/lib/modules",
    ]);
    const javaHome = dirname(dirname(text(object(backend.java).path)));
    const lines: string[] = [];
    const sourceLines: string[] = [];
    for (const value of fingerprint.records) {
        const record = object(value);
        const path = text(record.path);
        if (
            paths.has(path.toLowerCase()) ||
            path.includes("\\") ||
            path.split("/").some((part) => part === ".." || part === ".") ||
            !(
                path === "backend/pom.xml" ||
                path === "backend/mvnw" ||
                path === "backend/mvnw.cmd" ||
                path.startsWith("backend/.mvn/") ||
                path.startsWith("backend/src/main/") ||
                helpers.has(path) ||
                jdk.has(path)
            )
        )
            refuse("FINGERPRINT_PATH_INVALID");
        paths.add(path.toLowerCase());
        if (record.sha256 === "ABSENT") {
            if (!optional.has(path)) refuse("FINGERPRINT_ABSENCE_INVALID");
            let exists = true;
            try {
                dependencies.read(join(root, path));
            } catch {
                exists = false;
            }
            if (exists) refuse("FINGERPRINT_ABSENCE_INVALID");
            lines.push(`${path}=ABSENT`);
            continue;
        }
        const physicalPath = jdk.has(path)
            ? join(javaHome, path.substring("JDK21/".length))
            : join(root, path);
        const ownPath = sameFile(physicalPath, physicalPath, dependencies);
        const sha = validSha(record.sha256);
        checkedBytes(ownPath, sha, dependencies);
        const line = `${path}=${sha}`;
        lines.push(line);
        if (path === "backend/pom.xml" || path.startsWith("backend/src/main/"))
            sourceLines.push(line);
    }
    if (
        [
            "backend/pom.xml",
            "backend/mvnw",
            "backend/mvnw.cmd",
            ...optional,
            ...helpers,
            ...jdk,
        ].some((path) => !paths.has(path.toLowerCase()))
    )
        refuse("FINGERPRINT_INCOMPLETE");
    equal(digest(Buffer.from(lines.join("\n"), "utf8")), fingerprintHash);
    equal(digest(Buffer.from(sourceLines.join("\n"), "utf8")), sourceHash);
    for (const [left, right] of [
        [candidate.jar, backend.artifact],
        [candidate.config, backend.config],
        [candidate.java, backend.java],
    ]) {
        const expected = object(left);
        const actual = object(right);
        equal(actual.path, expected.path);
        equal(validSha(actual.sha256), validSha(expected.sha256));
    }
    const currentConfig = object(backend.config);
    checkedBytes(
        join(
            root,
            "backend/src/main/resources/application-frontend-dev.properties",
        ),
        currentConfig.sha256,
        dependencies,
    );
    const packageProof = object(candidate.package);
    validSha(packageProof.manifestSha256);
    validSha(packageProof.dependenciesSha256);
    if (
        !Array.isArray(packageProof.classes) ||
        !packageProof.classes.length ||
        !Array.isArray(packageProof.dependencies) ||
        !packageProof.dependencies.length
    )
        refuse("PACKAGE_PROOF_INCOMPLETE");
    const jdbcRef = object(backend.jdbc);
    const jdbcPath = sameFile(
        jdbcRef.path,
        join(directory, "jdbc-provider.json"),
        dependencies,
    );
    const jdbc = hashed(jdbcPath, jdbcRef.sha256, dependencies);
    equal(jdbc.id, "QUAL-CONF01-SQL01");
    equal(jdbc.runId, runId);
    equal(jdbc.pid, object(backend.backend).pid);
    equal(validSha(jdbc.declaredSourceHash), sourceHash);
    equal(
        validSha(jdbc.declaredJarSha256),
        validSha(object(backend.artifact).sha256),
    );
    equal(jdbc.approved, true);
    equal(jdbc.provider, "SQLServer");
    equal(jdbc.databaseProductName, "Microsoft SQL Server");
    equal(jdbc.database, "WMS_DEV");
    equal(jdbc.login, "WMSDEV");
    equal(jdbc.user, "WMSDEV");
    equal(jdbc.scope, "SUT_CONNECTION_METADATA_ONLY_NOT_FUNCTIONAL_TEST");
    integer(jdbc.sessionId);
    text(jdbc.driverName);
    text(jdbc.driverVersion);
    equal(jdbc.metadataStatementsExecuted, 1);
    equal(
        jdbc.connectionOpenedWithEncryptTrueAndTrustServerCertificateFalse,
        true,
    );
    equal(jdbc.privateTrustStoreAndCertificateHostConfigured, true);
    const started = current(jdbc.startedUtc, dependencies.now);
    const finished = current(jdbc.finishedUtc, dependencies.now);
    if (finished < started) refuse("TIME_ORDER_INVALID");
    const runtimePath = sameFile(
        join(directory, "runtime-prumo.json"),
        join(directory, "runtime-prumo.json"),
        dependencies,
    );
    checkedBytes(
        runtimePath,
        object(chain.backend).receiptSha256,
        dependencies,
    );
    return { started, finished };
}

/** Apenas leitura local. Nao abre SQL, HTTP ou processo backend. */
export function validateReadyChain(
    environment: DevEnvironment,
    port: number,
    dependencies: ReadyDependencies,
): string {
    if (!environment.WMS_DEV_READY_PATH || !environment.WMS_DEV_READY_SHA256)
        refuse(
            "MISSING: launcher real requer AUTH e guarda atuais; nenhum mock sera iniciado.",
        );
    const target = backendTarget(environment.WMS_DEV_BACKEND_URL);
    const path = text(environment.WMS_DEV_READY_PATH);
    const root = resolve(dependencies.projectRoot);
    const runDirectory = dirname(path);
    const legacyRunId = relative(
        join(root, "orchestracao/.runtime/frontend-integracao-dev-runs"),
        runDirectory,
    );
    const currentRunId = relative(
        join(root, "orchestracao/.runtime/be02-dev-fonte01/prumo/runs"),
        runDirectory,
    );
    const currentRun = /^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(
        currentRunId,
    );
    const runId = currentRun ? currentRunId : legacyRunId;
    if (
        !/^(?:[a-f0-9]{32}|[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12})$/i.test(
            runId,
        )
    )
        refuse("RUN_INVALID");
    sameFile(path, join(runDirectory, "integracao-pronta.json"), dependencies);
    const chain = hashed(path, environment.WMS_DEV_READY_SHA256, dependencies);
    equal(chain.natureza, "D31_DEV02_INTEGRACAO_PRONTA");
    if (currentRun) equal(chain.id, "BE02-DEV-FONTE01");
    if (currentRun) equal(port, 25581);
    equal(
        text(chain.runId).replaceAll("-", "").toLowerCase(),
        runId.replaceAll("-", "").toLowerCase(),
    );
    equal(resolve(text(chain.projectPath)).toLowerCase(), root.toLowerCase());
    const identity = object(chain.publicIdentity);
    requirePublicIdentity({
        issuer: identity.issuer,
        jwkSetUri: identity.jwkSetUri,
        audience: identity.audience,
    });
    const observed = current(chain.observadoUtc, dependencies.now);
    const frontend = object(chain.frontend);
    equal(frontend.port, port);
    equal(frontend.modo, "real");
    equal(frontend.proxyPrefix, "/api");
    equal(frontend.mockFallback, false);

    const guardLink = object(chain.guard);
    const guardPath = sameFile(
        guardLink.path,
        join(runDirectory, "guarda.json"),
        dependencies,
    );
    const guard = hashed(guardPath, guardLink.sha256, dependencies);
    equal(guard.guardaRealAprovada, true);
    equal(guard.estado, "GUARDA_ATUAL_APROVADA");
    equal(guard.pidGuarda, integer(guardLink.processId));
    const guardStart = current(guard.inicioUtc, dependencies.now);
    const guardEnd = current(guard.observadoEm, dependencies.now);
    if (guardStart > guardEnd || guardEnd > observed)
        refuse("TIME_ORDER_INVALID");
    const helper = object(guard.auxiliar);
    equal(helper.arquivo, "infra/dev02/guarda-wmsdev.ps1");
    equal(
        text(helper.sha256).toLowerCase(),
        text(guardLink.helperSha256).toLowerCase(),
    );
    checkedBytes(join(root, text(helper.arquivo)), helper.sha256, dependencies);
    const targetProof = object(guard.alvo);
    equal(targetProof.banco, "WMS_DEV");
    equal(targetProof.login, "WMSDEV");
    equal(targetProof.usuario, "WMSDEV");
    equal(targetProof.estado, "ONLINE");
    equal(guard.identidadeConfirmada, true);
    const tls = object(guard.tls);
    equal(tls.encrypt, "Mandatory");
    equal(tls.trustServerCertificate, false);
    equal(tls.handshakeConcluido, true);
    equal(tls.confirmacaoAtual, true);
    for (const name of ["certificateSha256", "truststoreSha256"])
        if (!/^[a-f0-9]{64}$/i.test(text(tls[name])))
            refuse("TLS_PROOF_INVALID");
    const validUntil = Date.parse(text(tls.notAfter));
    if (!Number.isFinite(validUntil) || validUntil <= dependencies.now)
        refuse("TLS_PROOF_INVALID");
    object(guard.direitos);
    object(guard.catalogo);
    if (
        !Array.isArray(guard.historico) ||
        !guard.historico.length ||
        !Array.isArray(guard.checks) ||
        !guard.checks.length ||
        guard.checks.some((check) => object(check).passou !== true)
    )
        refuse("GUARD_INCOMPLETE");
    if (currentRun) {
        equal(guard.aberturaTentativas, 1);
        equal(guard.aberturaConcluida, true);
        equal(guard.checks.length, 26);
    }

    const backendLink = object(chain.backend);
    equal(backendLink.url, target);
    const backendPath = sameFile(
        backendLink.receiptPath,
        join(runDirectory, "backend-readiness.json"),
        dependencies,
    );
    const backend = hashed(
        backendPath,
        backendLink.receiptSha256,
        dependencies,
    );
    equal(backend.natureza, "D31_DEV02_BACKEND_READINESS");
    equal(
        text(backend.runId).replaceAll("-", "").toLowerCase(),
        runId.replaceAll("-", "").toLowerCase(),
    );
    equal(backend.estado, "READY");
    equal(backend.profile, "sqlserver-dev");
    equal(backend.database, "WMS_DEV");
    equal(backend.login, "WMSDEV");
    const backendTime = current(backend.observadoUtc, dependencies.now);
    if (backendTime < guardEnd || backendTime > observed)
        refuse("TIME_ORDER_INVALID");
    const backendGuard = object(backend.guard);
    equal(
        text(backendGuard.sha256).toLowerCase(),
        text(guardLink.sha256).toLowerCase(),
    );
    equal(backendGuard.processId, guard.pidGuarda);
    equal(
        text(backendGuard.helperSha256).toLowerCase(),
        text(guardLink.helperSha256).toLowerCase(),
    );
    equal(backendGuard.finishedUtc, guard.observadoEm);
    for (const name of ["artifact", "config", "java"]) {
        const file = object(backend[name]);
        checkedBytes(text(file.path), file.sha256, dependencies);
    }
    const jdbcTime = currentRun
        ? currentBackendProof(
              chain,
              backend,
              root,
              runDirectory,
              runId,
              dependencies,
          )
        : undefined;
    if (jdbcTime) {
        if (jdbcTime.started < guardEnd || jdbcTime.finished > backendTime)
            refuse("TIME_ORDER_INVALID");
    } else {
        const status = object(backend.status);
        equal(status.http, 200);
        equal(status.aplicacao, "wms-rodogarcia");
        equal(status.status, "DISPONIVEL");
    }
    const process = object(backend.backend);
    const pid = integer(process.pid);
    equal(pid, backendLink.pid);
    equal(process.url, target);
    equal(process.port, Number(new URL(target).port));
    equal(process.listenerOwned, true);
    const startUtc = text(process.startUtc);
    if (
        Date.parse(startUtc) < guardStart ||
        Date.parse(startUtc) > backendTime ||
        !Number.isFinite(Date.parse(startUtc))
    )
        refuse("PROCESS_TIME_INVALID");
    if (currentRun) {
        equal(process.address, "127.0.0.1");
        if (Date.parse(startUtc) > jdbcTime!.started)
            refuse("TIME_ORDER_INVALID");
    }
    if (
        !dependencies.inspectBackend({
            pid,
            startUtc,
            port: integer(process.port),
            executable: text(object(backend.java).path),
        })
    )
        refuse("PROCESS_IDENTITY_MISMATCH");
    return target;
}
