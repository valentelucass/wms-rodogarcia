import http from "node:http";
import { lstat, readFile, realpath, readdir } from "node:fs/promises";
import path from "node:path";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";

const PUBLIC_ORIGIN = "https://wms.rodogarcia.com.br";
const CSP =
    "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'self'; form-action 'self'; frame-ancestors 'none'";
const HOP = new Set([
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailer",
    "transfer-encoding",
    "upgrade",
]);
const MIME = {
    ".html": "text/html; charset=utf-8",
    ".js": "text/javascript; charset=utf-8",
    ".css": "text/css; charset=utf-8",
    ".json": "application/json",
    ".svg": "image/svg+xml",
    ".png": "image/png",
    ".woff2": "font/woff2",
    ".ico": "image/x-icon",
};
const fail = (code) => {
    throw Object.assign(new Error(code), { code });
};
const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
const inside = (root, file) =>
    file !== root &&
    !path.relative(root, file).startsWith("..") &&
    !path.isAbsolute(path.relative(root, file));
const samePath = (a, b) =>
    process.platform === "win32"
        ? a.toLowerCase() === b.toLowerCase()
        : a === b;

export function parseArguments(args) {
    const result = {};
    const allowed = new Set([
        "dist",
        "manifest",
        "port",
        "backend",
        "public-origin",
    ]);
    for (let i = 0; i < args.length; i += 2) {
        const name = args[i]?.slice(2);
        if (
            !args[i]?.startsWith("--") ||
            !allowed.has(name) ||
            result[name] !== undefined ||
            !args[i + 1] ||
            args[i + 1].startsWith("--")
        )
            fail("WMS_PROD_ARGUMENTS_INVALID");
        result[name] = args[i + 1];
    }
    if ([...allowed].some((key) => !result[key]))
        fail("WMS_PROD_ARGUMENTS_MISSING");
    if (!/^[1-9]\d{0,4}$/.test(result.port) || Number(result.port) > 65535)
        fail("WMS_PROD_PORT_INVALID");
    return {
        dist: result.dist,
        manifest: result.manifest,
        port: Number(result.port),
        backend: result.backend,
        publicOrigin: result["public-origin"],
    };
}

export function validateTopology(options) {
    if (options.publicOrigin !== PUBLIC_ORIGIN)
        fail("WMS_PROD_PUBLIC_ORIGIN_INVALID");
    if (
        typeof options.backend !== "string" ||
        !/^http:\/\/127\.0\.0\.1:[1-9]\d{0,4}$/.test(options.backend)
    )
        fail("WMS_PROD_BACKEND_INVALID");
    const target = new URL(options.backend);
    if (
        !target.port ||
        Number(target.port) > 65535 ||
        target.origin !== options.backend ||
        Number(target.port) === options.port
    )
        fail("WMS_PROD_BACKEND_INVALID");
    if (
        !Number.isInteger(options.port) ||
        options.port < 0 ||
        options.port > 65535
    )
        fail("WMS_PROD_PORT_INVALID");
    // Porta0 só para harness via função; a CLI exige uma porta explícita livre.
    return target;
}

async function filesIn(root, directory = root) {
    const result = [];
    for (const entry of await readdir(directory, { withFileTypes: true })) {
        const absolute = path.join(directory, entry.name);
        if (entry.isSymbolicLink()) fail("WMS_PROD_ARTIFACT_LINK_REJECTED");
        if (entry.isDirectory())
            result.push(...(await filesIn(root, absolute)));
        else if (entry.isFile())
            result.push(
                path.relative(root, absolute).split(path.sep).join("/"),
            );
        else fail("WMS_PROD_ARTIFACT_TYPE_INVALID");
    }
    return result.sort();
}

export async function verifyBuild(dist, manifestPath) {
    if (
        (await lstat(dist)).isSymbolicLink() ||
        (await lstat(manifestPath)).isSymbolicLink()
    )
        fail("WMS_PROD_ARTIFACT_LINK_REJECTED");
    const root = await realpath(dist);
    const manifestReal = await realpath(manifestPath);
    if (
        !samePath(root, path.resolve(dist)) ||
        !samePath(manifestReal, path.resolve(manifestPath))
    )
        fail("WMS_PROD_ARTIFACT_LINK_REJECTED");
    if (!inside(root, manifestReal)) fail("WMS_PROD_MANIFEST_OUTSIDE_DIST");
    const raw = await readFile(manifestReal);
    let manifest;
    try {
        manifest = JSON.parse(raw.toString("utf8"));
    } catch {
        fail("WMS_PROD_MANIFEST_INVALID");
    }
    if (
        !manifest ||
        typeof manifest !== "object" ||
        manifest.natureza !== "WMS_PROD_FRONTEND_BUILD" ||
        manifest.buildmode !== "real" ||
        !/^[a-zA-Z0-9-]{1,100}$/.test(manifest.runId ?? "") ||
        !/^[a-f0-9]{64}$/i.test(manifest.sourceHash ?? "") ||
        !Array.isArray(manifest.artifacts) ||
        !manifest.artifacts.length
    )
        fail("WMS_PROD_MANIFEST_INVALID");
    const files = new Map();
    for (const item of manifest.artifacts) {
        if (
            !item ||
            typeof item.path !== "string" ||
            !item.path ||
            item.path.includes("\\") ||
            item.path.startsWith("/") ||
            item.path.split("/").some((p) => p === ".." || p === "." || !p) ||
            !/^[a-f0-9]{64}$/i.test(item.sha256 ?? "") ||
            !Number.isSafeInteger(item.bytes) ||
            item.bytes < 0 ||
            files.has(item.path)
        )
            fail("WMS_PROD_MANIFEST_INVALID");
        const absolute = await realpath(path.join(root, item.path));
        if (!inside(root, absolute) || !(await lstat(absolute)).isFile())
            fail("WMS_PROD_ARTIFACT_OUTSIDE_DIST");
        const bytes = await readFile(absolute);
        if (
            bytes.length !== item.bytes ||
            hash(bytes) !== item.sha256.toLowerCase()
        )
            fail("WMS_PROD_ARTIFACT_HASH_MISMATCH");
        // A versão servida é o conjunto exato de bytes validado antes de listen.
        // Trocar arquivos depois não troca silenciosamente esta execução.
        files.set(item.path, { ...item, absolute, body: bytes });
    }
    const listed = await filesIn(root);
    const manifestRelative = path
        .relative(root, manifestReal)
        .split(path.sep)
        .join("/");
    if (
        listed
            .filter((p) => p !== manifestRelative)
            .some((p) => !files.has(p)) ||
        listed.length !== files.size + 1 ||
        !files.has("index.html")
    )
        fail("WMS_PROD_ARTIFACT_UNLISTED");
    return { root, files, manifest, manifestHash: hash(raw) };
}

function cleanHeaders(headers, request = false) {
    const connection = String(headers.connection ?? "")
        .toLowerCase()
        .split(",")
        .map((v) => v.trim());
    const result = {};
    for (const [name, value] of Object.entries(headers)) {
        if (
            HOP.has(name) ||
            connection.includes(name) ||
            (request &&
                (name === "forwarded" ||
                    name.startsWith("x-forwarded-") ||
                    name.startsWith("cf-")))
        )
            continue;
        if (value !== undefined) result[name] = value;
    }
    return result;
}

function refuse(response, status, code) {
    if (response.headersSent) {
        response.destroy();
        return;
    }
    response.writeHead(status, {
        "Content-Type": "application/problem+json",
        "Cache-Control": "no-store",
    });
    response.end(JSON.stringify({ status, code }));
}

function relay(request, response, target) {
    // Sem fetch/JSON.parse/retry/redirect: os bytes e o status são do upstream.
    const upstream = http.request(
        {
            hostname: target.hostname,
            port: target.port,
            path: request.url,
            method: request.method,
            headers: cleanHeaders(request.headers, true),
            agent: false,
        },
        (received) => {
            response.writeHead(
                received.statusCode,
                cleanHeaders(received.headers),
            );
            received.on("error", () => response.destroy());
            received.pipe(response);
        },
    );
    upstream.setTimeout(30000, () => upstream.destroy());
    upstream.on("error", () => {
        const requestId = request.headers["x-request-id"];
        if (
            !response.headersSent &&
            typeof requestId === "string" &&
            requestId.length <= 200
        )
            response.setHeader("X-Request-Id", requestId);
        refuse(response, 502, "WMS_PROD_RELAY_UNCONFIRMED");
    });
    request.on("aborted", () => upstream.destroy());
    request.on("error", () => upstream.destroy());
    response.on("close", () => {
        if (!response.writableEnded) upstream.destroy();
    });
    request.pipe(upstream);
}

export async function createProductionServer(options) {
    const target = validateTopology(options);
    const build = await verifyBuild(options.dist, options.manifest);
    const server = http.createServer(async (request, response) => {
        const localAuthority = `127.0.0.1:${server.address().port}`;
        if (
            ![new URL(PUBLIC_ORIGIN).host, localAuthority].includes(
                request.headers.host,
            )
        )
            return refuse(response, 400, "WMS_PROD_HOST_INVALID");
        if (!request.url?.startsWith("/") || request.url.startsWith("//"))
            return refuse(response, 400, "WMS_PROD_PATH_INVALID");
        const encoded = request.url.split("?")[0];
        let pathname;
        try {
            pathname = decodeURIComponent(encoded);
        } catch {
            return refuse(response, 400, "WMS_PROD_PATH_INVALID");
        }
        if (
            /[\\\0#]/.test(pathname) ||
            /%2f|%5c/i.test(encoded) ||
            pathname.split("/").some((p) => p === ".." || p === ".")
        )
            return refuse(response, 400, "WMS_PROD_PATH_INVALID");
        if (pathname === "/api" || pathname.startsWith("/api/"))
            return relay(request, response, target);
        if (request.method !== "GET" && request.method !== "HEAD")
            return refuse(response, 405, "WMS_PROD_STATIC_METHOD_INVALID");
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("X-Content-Type-Options", "nosniff");
        if (pathname === "/__wms/health") {
            response.writeHead(200, {
                "Content-Type": "application/json",
                "Cache-Control": "no-store",
            });
            return response.end(
                request.method === "HEAD"
                    ? undefined
                    : JSON.stringify({
                          natureza: "WMS_PROD_FRONTEND_READINESS",
                          estado: "STATIC_READY",
                          pid: process.pid,
                          port: server.address().port,
                          runId: build.manifest.runId,
                          sourceHash: build.manifest.sourceHash,
                          manifestSha256: build.manifestHash,
                          buildmode: "real",
                          backendReadiness: "NOT_PROBED",
                          publicOrigin: PUBLIC_ORIGIN,
                      }),
            );
        }
        const file = pathname === "/" ? "index.html" : pathname.slice(1);
        let item = build.files.get(file);
        if (
            !item &&
            !path.extname(file) &&
            request.headers.accept?.includes("text/html") &&
            !file.split("/").some((p) => p.startsWith("."))
        )
            item = build.files.get("index.html");
        if (!item) return refuse(response, 404, "WMS_PROD_STATIC_NOT_FOUND");
        response.writeHead(200, {
            "Content-Type":
                MIME[path.extname(item.path)] ?? "application/octet-stream",
            "Content-Length": item.bytes,
            "Cache-Control": item.path.startsWith("assets/")
                ? "public, max-age=31536000, immutable"
                : "no-cache",
        });
        return response.end(request.method === "HEAD" ? undefined : item.body);
    });
    server.on("upgrade", (_request, socket) => socket.destroy());
    return {
        server,
        build,
        listen: () =>
            new Promise((resolve, reject) => {
                server.once("error", reject);
                server.listen(
                    { host: "127.0.0.1", port: options.port, exclusive: true },
                    () => {
                        server.removeListener("error", reject);
                        resolve(server.address());
                    },
                );
            }),
    };
}

async function main() {
    const options = parseArguments(process.argv.slice(2));
    const running = await createProductionServer(options);
    const address = await running.listen();
    console.log(
        JSON.stringify({
            natureza: "WMS_PROD_FRONTEND_STARTED",
            pid: process.pid,
            port: address.port,
            host: address.address,
            runId: running.build.manifest.runId,
            sourceHash: running.build.manifest.sourceHash,
            manifestSha256: running.build.manifestHash,
            buildmode: "real",
            technicalUrl: `http://127.0.0.1:${address.port}`,
            publicOrigin: PUBLIC_ORIGIN,
        }),
    );
    for (const signal of ["SIGINT", "SIGTERM"])
        process.once(signal, () => running.server.close());
}

if (
    process.argv[1] &&
    path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
)
    main().catch((error) => {
        console.error(error.code ?? "WMS_PROD_SERVER_FAILED");
        process.exitCode = 1;
    });
