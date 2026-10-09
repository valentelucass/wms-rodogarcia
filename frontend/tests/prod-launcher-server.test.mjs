import { test } from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { mkdir, mkdtemp, writeFile, readFile, symlink } from "node:fs/promises";
import path from "node:path";
import { createHash } from "node:crypto";
import { spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
import {
    createProductionServer,
    parseArguments,
    validateTopology,
    verifyBuild,
} from "../tools/prod-server.mjs";
import {
    buildArguments,
    assertSourceUnchanged,
    typecheckSnapshot,
} from "../tools/build-prod.mjs";

const root = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    "../..",
);
const proofRoot = path.join(
    root,
    "orchestracao/.runtime/launcher-producao/lume/harness",
);
const sha = (value) => createHash("sha256").update(value).digest("hex");
const origin = "https://wms.rodogarcia.com.br";
const listen = (server) =>
    new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", () => resolve(server.address().port));
    });
const close = (server) =>
    new Promise((resolve) => {
        server.closeAllConnections();
        server.close(resolve);
    });

async function fixture() {
    await mkdir(proofRoot, { recursive: true });
    const dist = await mkdtemp(path.join(proofRoot, "dist-"));
    await mkdir(path.join(dist, "assets"));
    const contents = {
        "index.html": Buffer.from(
            "<!doctype html><main>fixture estática sintética</main>",
        ),
        "assets/app-a1.js": Buffer.from("console.log('fixture');"),
    };
    for (const [name, bytes] of Object.entries(contents))
        await writeFile(path.join(dist, name), bytes);
    const manifest = {
        natureza: "WMS_PROD_FRONTEND_BUILD",
        runId: "synthetic-harness",
        sourceHash: "a".repeat(64),
        buildmode: "real",
        artifacts: Object.entries(contents).map(([name, bytes]) => ({
            path: name,
            bytes: bytes.length,
            sha256: sha(bytes),
        })),
    };
    const manifestPath = path.join(dist, "build-manifest.json");
    await writeFile(manifestPath, JSON.stringify(manifest));
    return { dist, manifestPath, manifest, contents };
}

function request(port, pathname, options = {}) {
    return new Promise((resolve, reject) => {
        const req = http.request(
            {
                host: "127.0.0.1",
                port,
                path: pathname,
                method: options.method ?? "GET",
                headers: options.headers,
            },
            (response) => {
                const chunks = [];
                response.on("data", (chunk) => chunks.push(chunk));
                response.on("end", () =>
                    resolve({
                        status: response.statusCode,
                        headers: response.headers,
                        body: Buffer.concat(chunks),
                    }),
                );
            },
        );
        req.once("error", reject);
        req.end(options.body);
    });
}

async function running(handler = (_req, res) => res.end("synthetic upstream")) {
    const fixtureBuild = await fixture();
    const upstream = http.createServer(handler);
    const upstreamPort = await listen(upstream);
    const frontend = await createProductionServer({
        dist: fixtureBuild.dist,
        manifest: fixtureBuild.manifestPath,
        port: 0,
        backend: `http://127.0.0.1:${upstreamPort}`,
        publicOrigin: origin,
    });
    const address = await frontend.listen();
    return {
        ...fixtureBuild,
        upstream,
        frontend,
        port: address.port,
        cleanup: async () => {
            await close(frontend.server);
            await close(upstream);
        },
    };
}

test("CLI/topologia recusam argumentos ausentes, duplicados, destino externo, DEV/mock e origem divergente", () => {
    const valid = [
        "--dist",
        "dist",
        "--manifest",
        "dist/manifest",
        "--port",
        "25591",
        "--backend",
        "http://127.0.0.1:25590",
        "--public-origin",
        origin,
    ];
    assert.equal(parseArguments(valid).port, 25591);
    for (const args of [
        [],
        [...valid, "--port", "25592"],
        [...valid, "--mock", "true"],
        valid.map((v) => (v === "25591" ? "0" : v)),
    ])
        assert.throws(() => parseArguments(args));
    const options = parseArguments(valid);
    for (const backend of [
        "http://localhost:25590",
        "https://example.test",
        "http://127.0.0.1:25590/api",
        "http://127.0.0.1:25591",
        "http://u:p@127.0.0.1:25590",
    ])
        assert.throws(() => validateTopology({ ...options, backend }));
    assert.throws(() =>
        validateTopology({
            ...options,
            publicOrigin: "http://127.0.0.1:25591",
        }),
    );
    assert.throws(() =>
        buildArguments([
            "--release",
            path.join(root, "frontend/dist"),
            "--run-id",
            "abc",
        ]),
    );
    assert.throws(() =>
        buildArguments(["--release", "relative", "--run-id", "abc"]),
    );
});

test("HTML/assets verificados, HEAD e CSP fechada; sem Vite server", async () => {
    const r = await running();
    try {
        const index = await request(r.port, "/");
        assert.equal(index.status, 200);
        assert.deepEqual(index.body, r.contents["index.html"]);
        assert.match(
            index.headers["content-security-policy"],
            /connect-src 'self';/,
        );
        assert.doesNotMatch(
            index.headers["content-security-policy"],
            /unsafe-inline|ws:|\*/,
        );
        const asset = await request(r.port, "/assets/app-a1.js?v=1");
        assert.deepEqual(asset.body, r.contents["assets/app-a1.js"]);
        assert.match(asset.headers["cache-control"], /immutable/);
        const head = await request(r.port, "/assets/app-a1.js", {
            method: "HEAD",
        });
        assert.equal(head.body.length, 0);
        assert.equal(Number(head.headers["content-length"]), asset.body.length);
    } finally {
        await r.cleanup();
    }
});

test("readiness é estático e não sonda backend; bytes em execução não mudam por troca de arquivo", async () => {
    let calls = 0;
    const r = await running((_req, res) => {
        calls++;
        res.end();
    });
    try {
        const health = JSON.parse(
            (await request(r.port, "/__wms/health")).body,
        );
        assert.equal(health.estado, "STATIC_READY");
        assert.equal(health.backendReadiness, "NOT_PROBED");
        assert.equal(health.buildmode, "real");
        assert.equal(health.runId, "synthetic-harness");
        assert.equal(
            health.manifestSha256,
            sha(await readFile(r.manifestPath)),
        );
        assert.equal(calls, 0);
        await writeFile(
            path.join(r.dist, "index.html"),
            "alteração posterior sintética",
        );
        assert.deepEqual(
            (await request(r.port, "/")).body,
            r.contents["index.html"],
        );
    } finally {
        await r.cleanup();
    }
});

test("relay conserva Long/decimal/binary/status/headers/cookies e URL sem reescrever", async () => {
    const body = Buffer.from('{"versao":9007199254740993,"valor":2.500000}');
    const responseBytes = Buffer.from([0, 255, 128, 10, 13]);
    let seen;
    const r = await running(async (req, res) => {
        const chunks = [];
        for await (const chunk of req) chunks.push(chunk);
        seen = {
            url: req.url,
            method: req.method,
            headers: req.headers,
            body: Buffer.concat(chunks),
        };
        res.writeHead(409, {
            "Content-Type": "application/octet-stream",
            "X-Request-Id": "synthetic-correlation",
            "Set-Cookie": [
                "WMS_REFRESH=synthetic; Path=/api/auth; HttpOnly; Secure; SameSite=Strict",
                "XSRF-TOKEN=synthetic; Path=/api/auth; HttpOnly; Secure; SameSite=Strict",
            ],
        });
        res.end(responseBytes);
    });
    try {
        const result = await request(
            r.port,
            "/api/auth/usuarios/901?pagina=0&texto=a%20b",
            {
                method: "PUT",
                body,
                headers: {
                    "content-type": "application/json",
                    "content-length": String(body.length),
                    origin,
                    authorization: "Bearer synthetic",
                    cookie: "WMS_REFRESH=synthetic",
                    "x-xsrf-token": "synthetic",
                    "x-request-id": "synthetic-request",
                },
            },
        );
        assert.equal(result.status, 409);
        assert.deepEqual(result.body, responseBytes);
        assert.equal(seen.url, "/api/auth/usuarios/901?pagina=0&texto=a%20b");
        assert.equal(seen.method, "PUT");
        assert.deepEqual(seen.body, body);
        for (const name of [
            "origin",
            "authorization",
            "cookie",
            "x-xsrf-token",
            "x-request-id",
        ])
            assert.ok(seen.headers[name]);
        assert.equal(seen.headers.origin, origin);
        assert.equal(result.headers["x-request-id"], "synthetic-correlation");
        assert.equal(result.headers["set-cookie"].length, 2);
        assert.match(
            result.headers["set-cookie"][0],
            /HttpOnly; Secure; SameSite=Strict/,
        );
    } finally {
        await r.cleanup();
    }
});

test("preserva Origin recusável e Host exato; remove Forwarded/X-Forwarded/CF sem inventar HTTPS", async () => {
    let seen;
    const r = await running((req, res) => {
        seen = req.headers;
        res.writeHead(403);
        res.end("synthetic origin refusal");
    });
    try {
        const result = await request(r.port, "/api/auth/renovar", {
            method: "POST",
            headers: {
                origin: "http://127.0.0.1:1234",
                host: "wms.rodogarcia.com.br",
                forwarded: "proto=https;host=attacker.test",
                "x-forwarded-proto": "https",
                "x-forwarded-host": "attacker.test",
                "x-forwarded-for": "203.0.113.1",
                "cf-connecting-ip": "203.0.113.1",
            },
        });
        assert.equal(result.status, 403);
        assert.equal(seen.origin, "http://127.0.0.1:1234");
        assert.equal(seen.host, "wms.rodogarcia.com.br");
        for (const name of [
            "forwarded",
            "x-forwarded-proto",
            "x-forwarded-host",
            "x-forwarded-for",
            "cf-connecting-ip",
        ])
            assert.equal(seen[name], undefined);
        assert.equal(result.headers["access-control-allow-origin"], undefined);
    } finally {
        await r.cleanup();
    }
});

test("5xx e redirects do upstream preservados sem retry/replay", async () => {
    let calls = 0;
    const r = await running((_req, res) => {
        calls++;
        res.writeHead(calls === 1 ? 502 : 307, {
            Location: "https://example.test/future",
        });
        res.end("synthetic upstream body");
    });
    try {
        assert.equal(
            (
                await request(r.port, "/api/v1/comando", {
                    method: "POST",
                    body: "synthetic",
                })
            ).status,
            502,
        );
        assert.equal(calls, 1);
        const redirect = await request(r.port, "/api/v1/comando", {
            method: "POST",
            body: "synthetic",
        });
        assert.equal(redirect.status, 307);
        assert.equal(calls, 2);
        assert.equal(redirect.headers.location, "https://example.test/future");
    } finally {
        await r.cleanup();
    }
});

test("falha de transporte gera502 sem anunciar sucesso", async () => {
    const r = await running((req) => req.socket.destroy());
    try {
        const result = await request(r.port, "/api/v1/comando", {
            method: "POST",
            body: "synthetic",
            headers: { "x-request-id": "synthetic-lost-response" },
        });
        assert.equal(result.status, 502);
        assert.equal(
            JSON.parse(result.body).code,
            "WMS_PROD_RELAY_UNCONFIRMED",
        );
        assert.equal(result.headers["x-request-id"], "synthetic-lost-response");
    } finally {
        await r.cleanup();
    }
});

test("cancelamento fecha upstream próprio sem replay nem queda do servidor", async () => {
    let calls = 0;
    let started;
    const seen = new Promise((resolve) => {
        started = resolve;
    });
    const r = await running((req) => {
        calls++;
        req.on("error", () => {});
        req.on("data", () => started());
    });
    try {
        const req = http.request({
            host: "127.0.0.1",
            port: r.port,
            path: "/api/v1/comando",
            method: "POST",
        });
        req.on("error", () => {});
        req.write("synthetic pending command");
        await seen;
        req.destroy();
        await new Promise((resolve) => setTimeout(resolve, 20));
        assert.equal(calls, 1);
        assert.equal((await request(r.port, "/__wms/health")).status, 200);
    } finally {
        await r.cleanup();
    }
});

test("colisão de porta falha sem encerrar processo/listener dono", async () => {
    const r = await running();
    try {
        const conflict = await createProductionServer({
            dist: r.dist,
            manifest: r.manifestPath,
            port: r.port,
            backend: `http://127.0.0.1:${r.upstream.address().port}`,
            publicOrigin: origin,
        });
        await assert.rejects(conflict.listen(), { code: "EADDRINUSE" });
        assert.equal((await request(r.port, "/")).status, 200);
    } finally {
        await r.cleanup();
    }
});

test("traversal, Host estranho, método estático e assets ausentes não viram SPA ou API", async () => {
    let calls = 0;
    const r = await running((_req, res) => {
        calls++;
        res.end();
    });
    try {
        for (const pathname of [
            "/%2e%2e/secret",
            "/assets%2fapp-a1.js",
            "/%5csecret",
        ])
            assert.equal((await request(r.port, pathname)).status, 400);
        for (const pathname of [
            "/.env",
            "/src/auth/client.ts",
            "/assets/missing.js",
        ])
            assert.equal((await request(r.port, pathname)).status, 404);
        assert.equal(
            (await request(r.port, "/", { headers: { host: "attacker.test" } }))
                .status,
            400,
        );
        assert.equal(
            (await request(r.port, "/", { method: "POST" })).status,
            405,
        );
        assert.equal(
            (
                await request(r.port, "/cadastros", {
                    headers: { accept: "text/html" },
                })
            ).status,
            200,
        );
        assert.equal(calls, 0);
    } finally {
        await r.cleanup();
    }
});

test("manifesto/hash/arquivo não listado são recusados antes de listen", async () => {
    const f = await fixture();
    await writeFile(path.join(f.dist, "assets/app-a1.js"), "tampered");
    await assert.rejects(verifyBuild(f.dist, f.manifestPath), {
        code: "WMS_PROD_ARTIFACT_HASH_MISMATCH",
    });
    const extra = await fixture();
    await writeFile(path.join(extra.dist, "unknown.txt"), "unlisted");
    await assert.rejects(verifyBuild(extra.dist, extra.manifestPath), {
        code: "WMS_PROD_ARTIFACT_UNLISTED",
    });
    const mocked = await fixture();
    mocked.manifest.buildmode = "ficticio";
    await writeFile(mocked.manifestPath, JSON.stringify(mocked.manifest));
    await assert.rejects(verifyBuild(mocked.dist, mocked.manifestPath), {
        code: "WMS_PROD_MANIFEST_INVALID",
    });
});

test("manifesto raiz null/path fora/duplicado e link/junction externo são recusados", async () => {
    for (const mutation of [
        () => null,
        (m) => ({ ...m, artifacts: [...m.artifacts, m.artifacts[0]] }),
        (m) => ({
            ...m,
            artifacts: [{ ...m.artifacts[0], path: "../outside.html" }],
        }),
    ]) {
        const f = await fixture();
        await writeFile(f.manifestPath, JSON.stringify(mutation(f.manifest)));
        await assert.rejects(verifyBuild(f.dist, f.manifestPath));
    }
    const f = await fixture();
    const outside = await mkdtemp(path.join(proofRoot, "outside-"));
    await writeFile(path.join(outside, "external.txt"), "synthetic outside");
    await symlink(
        outside,
        path.join(f.dist, "linked"),
        process.platform === "win32" ? "junction" : "dir",
    );
    await assert.rejects(verifyBuild(f.dist, f.manifestPath), {
        code: "WMS_PROD_ARTIFACT_LINK_REJECTED",
    });
});

test("CLI real config ausente falha sem build/start/fallback", () => {
    const result = spawnSync(
        process.execPath,
        [path.join(root, "frontend/tools/prod-server.mjs")],
        { encoding: "utf8", windowsHide: true },
    );
    assert.equal(result.status, 1);
    assert.match(result.stderr, /WMS_PROD_ARGUMENTS_MISSING/);
    assert.doesNotMatch(result.stdout, /STARTED/);
});

test("mudança/adição/remoção de fonte recusa candidato capturado; fonte igual preserva aprovação", () => {
    const before = [{ path: "src/app.tsx", sha256: "a".repeat(64) }];
    assert.doesNotThrow(() =>
        assertSourceUnchanged(before, [...before], "BUILD"),
    );
    for (const after of [
        [],
        [{ path: "src/app.tsx", sha256: "b".repeat(64) }],
        [...before, { path: "src/new.ts", sha256: "b".repeat(64) }],
    ])
        assert.throws(() => assertSourceUnchanged(before, after, "BUILD"), {
            code: "WMS_PROD_SOURCE_CHANGED_DURING_BUILD",
        });
    assert.throws(() => assertSourceUnchanged(before, [], "CAPTURE"), {
        code: "WMS_PROD_SOURCE_CHANGED_DURING_CAPTURE",
    });
});

test("erro de tipagem sintético recusa build; candidato existente não é substituído", async () => {
    await mkdir(proofRoot, { recursive: true });
    const source = await mkdtemp(path.join(proofRoot, "type-error-"));
    await writeFile(
        path.join(source, "tsconfig.json"),
        JSON.stringify({
            compilerOptions: {
                noEmit: true,
                strict: true,
                skipLibCheck: true,
                types: [],
            },
            files: ["synthetic.ts"],
        }),
    );
    await writeFile(
        path.join(source, "synthetic.ts"),
        'const value: number = "synthetic type error";',
    );
    await assert.rejects(typecheckSnapshot(source), {
        code: "WMS_PROD_TYPECHECK_FAILED",
    });
    const release = await mkdtemp(path.join(proofRoot, "existing-"));
    await mkdir(path.join(release, "source"));
    await mkdir(path.join(release, "dist"));
    const sentinel = path.join(release, "dist/existing.html");
    await writeFile(sentinel, "existing synthetic artifact");
    const result = spawnSync(
        process.execPath,
        [
            path.join(root, "frontend/tools/build-prod.mjs"),
            "--release",
            release,
            "--run-id",
            "synthetic-failure",
        ],
        { encoding: "utf8", windowsHide: true },
    );
    assert.equal(result.status, 1);
    assert.doesNotMatch(result.stdout, /WMS_PROD_FRONTEND_BUILT/);
    assert.equal(
        await readFile(sentinel, "utf8"),
        "existing synthetic artifact",
    );
});
