import { createHash } from "node:crypto";
import {
    readFile,
    writeFile,
    mkdir,
    readdir,
    realpath,
    lstat,
    copyFile,
    symlink,
} from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { spawn } from "node:child_process";
import { build } from "vite";

const hash = (bytes) => createHash("sha256").update(bytes).digest("hex");
const project = path.resolve(
    path.dirname(fileURLToPath(import.meta.url)),
    "../..",
);
const frontend = path.join(project, "frontend");
const fail = (code) => {
    throw Object.assign(new Error(code), { code });
};
const rootFiles = [
    "package.json",
    "package-lock.json",
    "tsconfig.json",
    "index.html",
    "eslint.config.js",
    "vite.prod.config.ts",
    "tools/prod-server.mjs",
    "tools/build-prod.mjs",
];

export function buildArguments(args) {
    const result = {};
    for (let i = 0; i < args.length; i += 2) {
        const key = args[i]?.slice(2);
        if (
            !args[i]?.startsWith("--") ||
            !["release", "run-id"].includes(key) ||
            result[key] ||
            !args[i + 1] ||
            args[i + 1].startsWith("--")
        )
            fail("WMS_PROD_BUILD_ARGUMENTS_INVALID");
        result[key] = args[i + 1];
    }
    if (
        !path.isAbsolute(result.release ?? "") ||
        !/^[a-zA-Z0-9-]{1,100}$/.test(result["run-id"] ?? "")
    )
        fail("WMS_PROD_BUILD_ARGUMENTS_INVALID");
    const base = path.join(project, "orchestracao/.runtime/launcher-producao");
    const relative = path.relative(base, result.release);
    if (!relative || relative.startsWith("..") || path.isAbsolute(relative))
        fail("WMS_PROD_RELEASE_OUTSIDE_SCOPE");
    return { release: path.resolve(result.release), runId: result["run-id"] };
}

async function list(directory, prefix = "") {
    const result = [];
    for (const entry of await readdir(directory, { withFileTypes: true })) {
        const relative = prefix ? `${prefix}/${entry.name}` : entry.name;
        if (entry.isSymbolicLink()) fail("WMS_PROD_SOURCE_LINK_REJECTED");
        if (entry.isDirectory())
            result.push(
                ...(await list(path.join(directory, entry.name), relative)),
            );
        else if (entry.isFile()) result.push(relative);
    }
    return result.sort();
}

async function inputs() {
    const names = [...rootFiles];
    for (const dir of ["src", "public", "tests", "dev"])
        names.push(
            ...(await list(path.join(frontend, dir))).map((p) => `${dir}/${p}`),
        );
    names.push(
        ...(await readdir(frontend)).filter((name) => /\.ts$/.test(name)),
    );
    const result = [];
    for (const name of [...new Set(names)].sort()) {
        if ((await lstat(path.join(frontend, name))).isSymbolicLink())
            fail("WMS_PROD_SOURCE_LINK_REJECTED");
        const bytes = await readFile(path.join(frontend, name));
        result.push({ path: name, bytes: bytes.length, sha256: hash(bytes) });
    }
    return result;
}

const sourceHash = (files) =>
    hash(Buffer.from(files.map((f) => `${f.path}\0${f.sha256}\n`).join("")));

export function assertSourceUnchanged(expected, current, phase) {
    if (sourceHash(expected) !== sourceHash(current))
        fail(`WMS_PROD_SOURCE_CHANGED_DURING_${phase}`);
}

export async function typecheckSnapshot(source) {
    const code = await new Promise((resolve, reject) => {
        const child = spawn(
            process.execPath,
            [
                path.join(frontend, "node_modules/typescript/bin/tsc"),
                "--noEmit",
                "-p",
                path.join(source, "tsconfig.json"),
            ],
            { cwd: source, stdio: "inherit", windowsHide: true },
        );
        child.once("error", reject);
        child.once("exit", resolve);
    });
    if (code !== 0) fail("WMS_PROD_TYPECHECK_FAILED");
}

export async function buildRelease(options) {
    const releaseParent = await realpath(path.dirname(options.release));
    const base = await realpath(
        path.join(project, "orchestracao/.runtime/launcher-producao"),
    );
    const relative = path.relative(base, releaseParent);
    if (relative.startsWith("..") || path.isAbsolute(relative))
        fail("WMS_PROD_RELEASE_OUTSIDE_SCOPE");
    await mkdir(options.release, { recursive: true });
    if ((await lstat(options.release)).isSymbolicLink())
        fail("WMS_PROD_RELEASE_LINK_REJECTED");
    const source = path.join(options.release, "source");
    const output = path.join(options.release, "dist");
    // mkdir sem recursive impede reaproveitar fonte/dist de candidato anterior.
    await mkdir(source);
    await mkdir(output);
    const initial = await inputs();
    for (const item of initial) {
        const target = path.join(source, item.path);
        await mkdir(path.dirname(target), { recursive: true });
        await copyFile(path.join(frontend, item.path), target);
        if (hash(await readFile(target)) !== item.sha256)
            fail("WMS_PROD_SOURCE_CHANGED_DURING_CAPTURE");
    }
    await symlink(
        path.join(frontend, "node_modules"),
        path.join(source, "node_modules"),
        process.platform === "win32" ? "junction" : "dir",
    );
    assertSourceUnchanged(initial, await inputs(), "CAPTURE");
    await typecheckSnapshot(source);
    await build({
        root: source,
        configFile: path.join(source, "vite.prod.config.ts"),
        // Evita config compilada/cache temporária dentro do node_modules compartilhado.
        configLoader: "native",
        cacheDir: path.join(options.release, "vite-cache"),
        mode: "production",
        build: { outDir: output, emptyOutDir: false },
    });
    assertSourceUnchanged(initial, await inputs(), "BUILD");
    const artifacts = [];
    for (const name of await list(output)) {
        const bytes = await readFile(path.join(output, name));
        artifacts.push({
            path: name,
            bytes: bytes.length,
            sha256: hash(bytes),
        });
    }
    const manifest = {
        natureza: "WMS_PROD_FRONTEND_BUILD",
        runId: options.runId,
        sourceHash: sourceHash(initial),
        buildmode: "real",
        builtUtc: new Date().toISOString(),
        artifacts,
        inputs: initial.map((item) => ({
            ...item,
            sourcePath: `frontend/${item.path}`,
            usedPath: path.join(source, item.path),
        })),
        typecheck: "PASS",
        liveSourceUnchangedThroughBuild: true,
    };
    const manifestPath = path.join(output, "build-manifest.json");
    await writeFile(manifestPath, JSON.stringify(manifest, null, 2) + "\n", {
        flag: "wx",
    });
    return {
        ...manifest,
        dist: output,
        manifestPath,
        manifestSha256: hash(await readFile(manifestPath)),
    };
}

if (
    process.argv[1] &&
    path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)
) {
    const options = buildArguments(process.argv.slice(2));
    buildRelease(options)
        .then((result) =>
            console.log(
                JSON.stringify({
                    natureza: "WMS_PROD_FRONTEND_BUILT",
                    runId: result.runId,
                    sourceHash: result.sourceHash,
                    buildmode: result.buildmode,
                    dist: result.dist,
                    manifestPath: result.manifestPath,
                    manifestSha256: result.manifestSha256,
                    artifacts: result.artifacts.length,
                }),
            ),
        )
        .catch((error) => {
            console.error(error.code ?? "WMS_PROD_FRONTEND_BUILD_FAILED");
            process.exitCode = 1;
        });
}
