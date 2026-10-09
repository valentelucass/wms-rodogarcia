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
    const runId = relative(
        join(root, "orchestracao/.runtime/frontend-integracao-dev-runs"),
        runDirectory,
    );
    if (
        !/^(?:[a-f0-9]{32}|[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12})$/i.test(
            runId,
        )
    )
        refuse("RUN_INVALID");
    sameFile(path, join(runDirectory, "integracao-pronta.json"), dependencies);
    const chain = hashed(path, environment.WMS_DEV_READY_SHA256, dependencies);
    equal(chain.natureza, "D31_DEV02_INTEGRACAO_PRONTA");
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
    const status = object(backend.status);
    equal(status.http, 200);
    equal(status.aplicacao, "wms-rodogarcia");
    equal(status.status, "DISPONIVEL");
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
