import {
    readFileSync,
    writeFileSync,
    readdirSync,
    mkdirSync,
    existsSync,
} from "node:fs";
import path from "node:path";
import { createHash } from "node:crypto";
const label = process.argv[2];
if (!/^[a-z0-9-]+$/.test(label ?? "")) throw Error("Informe rótulo próprio");
const root = process.cwd(),
    target = path.join(root, "evidencias", "snapshot-" + label, "frontend");
if (existsSync(target)) throw Error("Snapshot existente é imutável");
const cedro = JSON.parse(
    readFileSync("evidencias/frontend-cedro-fe12-lucas-20261008.json", "utf8"),
);
if (!cedro.sourceStable || cedro.writerActive)
    throw Error("Fatia Cedro não estabilizada");
const transferred = new Map(
    cedro.files.map((f) => [
        f.path.replaceAll("\\", "/").replace(/^frontend\//, ""),
        f,
    ]),
);
const rows = [];
function walk(dir = "") {
    for (const e of readdirSync(path.join(root, dir), {
        withFileTypes: true,
    })) {
        const relative = path.posix.join(dir, e.name);
        if (e.isDirectory()) {
            if (
                !["node_modules", ".tools", "evidencias", "dist"].includes(
                    e.name,
                ) &&
                !e.name.startsWith("test-results")
            )
                walk(relative);
            continue;
        }
        const frozen = transferred.get(relative);
        const source = frozen
            ? path.join(
                  root,
                  frozen.snapshot
                      .replaceAll("\\", "/")
                      .replace(/^frontend\//, ""),
              )
            : path.join(root, relative);
        let bytes = readFileSync(source);
        if (
            frozen &&
            createHash("sha256").update(bytes).digest("hex") !== frozen.sha256
        )
            throw Error("Fatia Cedro divergente: " + relative);
        // Porta/rótulo são ambiente do processo; os checks usam bytes idênticos.
        const out = path.join(target, relative);
        mkdirSync(path.dirname(out), { recursive: true });
        writeFileSync(out, bytes);
        rows.push({
            path: relative,
            origin: frozen
                ? "fatia Cedro snapshot03 estável"
                : "fonte integrada Lume/Prumo",
            sha256: createHash("sha256").update(bytes).digest("hex"),
        });
    }
}
walk();
mkdirSync(path.join(target, "evidencias"), { recursive: true });
writeFileSync(
    path.join(target, "..", "manifesto.json"),
    JSON.stringify(
        {
            label,
            at: new Date().toISOString(),
            policy: "Checks próprios isolados da posse Cedro/Prumo; somente frontend fictício 5192",
            files: rows,
        },
        null,
        2,
    ) + "\n",
);
console.log(target);
