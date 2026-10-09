import { readFileSync, writeFileSync, readdirSync, mkdirSync } from "node:fs";
import ts from "typescript";

// Somente AST das definições locais e JSON derivados; nenhum backend executado.
const json = (path) => JSON.parse(readFileSync(path, "utf8"));
const endpoints = json("src/contracts/endpoints.json");
const sources = json("evidencias/fontes-contratos.json");
const screens = [];
const bindings = new Map();
function variable(file, name) {
    let found;
    function visit(node) {
        if (ts.isVariableDeclaration(node) && node.name.getText(file) === name)
            found = node.initializer;
        ts.forEachChild(node, visit);
    }
    visit(file);
    if (!found) throw new Error("Declaração ausente: " + name);
    return found;
}
function literal(node) {
    if (ts.isStringLiteral(node)) return node.text;
    if (ts.isArrayLiteralExpression(node)) return node.elements.map(literal);
    throw new Error("Definição exige revisão do gerador: " + node.getText());
}
const parse = (file) =>
    ts.createSourceFile(
        file,
        readFileSync(file, "utf8"),
        ts.ScriptTarget.Latest,
        true,
    );
const helperFile = parse("src/domain/journeyTypes.ts");
const cadastro = variable(helperFile, "cadastro");
const cadastroActions = literal(cadastro.body.arguments[3]);
for (const group of readdirSync("src/modules").sort()) {
    const source = "src/modules/" + group + "/definition.ts";
    const file = parse(source);
    for (const object of variable(file, "journeys").elements) {
        const props = Object.fromEntries(
            object.properties.map((p) => [p.name.getText(file), p.initializer]),
        );
        const screen = {
            id: literal(props.id),
            title: literal(props.title),
            fe: literal(props.fe),
            be: literal(props.be),
            source,
        };
        screens.push(screen);
        for (const step of props.steps.elements) {
            const name = step.expression.getText(file);
            if (!["s", "cadastro"].includes(name))
                throw new Error("Passo desconhecido: " + name);
            const title = literal(step.arguments[0]);
            const controller = literal(step.arguments[name === "s" ? 2 : 1]);
            const actions =
                name === "s" ? literal(step.arguments[3]) : cadastroActions;
            for (const action of actions) {
                const id = controller + "." + action;
                bindings.set(id, [
                    ...(bindings.get(id) ?? []),
                    { ...screen, step: title },
                ]);
            }
        }
    }
}
for (const [id, fe, be] of [
    ["EstoqueController.consultar", "FE07", "BE08"],
    ["EstoqueController.posicionar", "FE07", "BE08"],
    ["EnderecoController.listar", "FE07", "BE05 / BE08"],
    ["ExpedicaoController.ler", "FE09", "BE10"],
    ["ExpedicaoController.separar", "FE09", "BE10"],
    ["ContagemController.contar", "FE12", "BE14"],
])
    bindings.set(id, [
        ...(bindings.get(id) ?? []),
        {
            id: "coletor",
            title: "Coletor",
            step: "Leitura e confirmação",
            fe,
            be,
            source: "src/components/Collector.tsx",
        },
    ]);
const rows = endpoints.map((endpoint) => ({
    ...endpoint,
    screens: bindings.get(endpoint.id) ?? [],
    supportOnly: endpoint.id === "StatusController.consultar",
}));
const missing = rows.filter((row) => !row.supportOnly && !row.screens.length);
if (missing.length)
    throw new Error("Ações sem tela: " + missing.map((x) => x.id).join(", "));
for (const id of bindings.keys())
    if (!endpoints.some((e) => e.id === id))
        throw new Error("Tela sem contrato: " + id);
mkdirSync("docs", { recursive: true });
writeFileSync(
    "docs/mapeamento-telas-contratos.json",
    JSON.stringify(
        {
            generatedBy: "tools/mapeamento.mjs",
            contractSources: "evidencias/fontes-contratos.json",
            screens,
            rows,
            sourceHashes: sources.fontes,
        },
        null,
        2,
    ),
);
const table = [
    "# Mapeamento atual de telas e contratos",
    "",
    "Gerado da AST das definições frontend e dos contratos estáticos atuais. A associação prova existência de formulário/consulta; não comprova execução de todos os ramos ou integração real. Status é suporte do adaptador e não tem ação do operador. Início e Acesso não chamam API. Perfis abaixo são apresentação base; alcance, condições e autorização final continuam nos serviços. Resoluções históricas são Gestor; exceção FIFO é Supervisor/Gestor; valores de indicadores exigem Supervisor/Gestor.",
    "",
    "| Tela/etapa · FE→BE | Ação | Método e rota real | Request → response | Apresentação base | Fonte backend |",
    "| --- | --- | --- | --- | --- | --- |",
];
for (const row of rows)
    table.push(
        "| " +
            [
                row.screens
                    .map((s) => `${s.title} / ${s.step} · ${s.fe}→${s.be}`)
                    .join("<br>") || "Suporte técnico",
                row.id,
                `${row.method} ${row.path}`,
                `${row.request ?? "Sem corpo"} → ${row.response}`,
                row.permission,
                `${row.source}#${row.handler}<br>${row.permissionSource.join("<br>")}`,
            ].join(" | ") +
            " |",
    );
writeFileSync("docs/mapeamento-telas-contratos.md", table.join("\n") + "\n");
console.log(
    `${screens.length} telas de domínio; ${rows.length} contratos; ${missing.length} ações de negócio sem tela. Não é contagem de testes/aceite.`,
);
