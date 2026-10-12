import { test, expect, type Page } from "@playwright/test";
import { nfePreview } from "../nfe-example";
import { origin } from "./environment";
import { readFileSync } from "node:fs";
import { LosslessNumber, stringify } from "lossless-json";
import type { Endpoint, Values } from "../../src/contracts/runtime";

const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { endpoints: Endpoint[]; fixtures: Record<string, string> };
prepared.endpoints = JSON.parse(
    readFileSync("src/contracts/endpoints.json", "utf8"),
) as Endpoint[];
const source = (id: string) => JSON.parse(prepared.fixtures[id]) as Values;
const schema = JSON.parse(
    readFileSync("src/contracts/schemas.json", "utf8"),
) as {
    records: Record<string, { name: string; type: string }[]>;
};
// Serialização dos dados fictícios pelo tipo declarado, sem confundir versão/protocolo
// documentais (String) com versões e números Long de registros operacionais.
function contractWire(type: string, data: unknown): unknown {
    if (data == null) return data;
    if (type.startsWith("List<"))
        return (data as unknown[]).map((v) =>
            contractWire(type.slice(5, -1), v),
        );
    if (schema.records[type])
        return Object.fromEntries(
            schema.records[type].map((f) => [
                f.name,
                contractWire(f.type, (data as Values)[f.name]),
            ]),
        );
    return ["Long", "long", "Integer", "int", "BigDecimal"].includes(type)
        ? new LosslessNumber(String(data))
        : data;
}
function wire(value: unknown, key = ""): unknown {
    if (Array.isArray(value)) return value.map((item) => wire(item));
    if (value && typeof value === "object")
        return Object.fromEntries(
            Object.entries(value).map(([name, item]) => [
                name,
                wire(item, name),
            ]),
        );
    return typeof value === "string" &&
        /^(id|versao|serie|numero|numeroItem|pagina|tamanho|totalItens|totalPaginas|prevista|recebidaBoa|recebidaAvariada|diferenca|produtoVersao|quantidadeComercial|quantidadeTributavel|valorUnitario|valorProduto|valorTotal|quantidade|pesoLiquido|pesoBruto|quantidadeEstoque|fatorConversao)$|Id$/.test(
            key,
        ) &&
        /^\d+(\.\d+)?$/.test(value)
        ? new LosslessNumber(value)
        : value;
}
async function api(page: Page, perfil = "SUPERVISOR", longText = false) {
    const template = source("PedidoEntradaController.consultar");
    const makeOrder = (
        id: string,
        situation: string,
        reference: string,
    ): Values => ({
        ...(template.pedido as Values),
        id,
        referencia: reference,
        clienteId: "1",
        armazemId: "1",
        versao: "4",
        situacao: situation,
        efetivadoEm: null,
        motivoConclusao: null,
        origemCriacao: "MANUAL",
    });
    const makeNote = (id: string, item: string): Values => {
        const example = (template.notas as Values[])[0];
        return {
            ...example,
            id,
            numero: id,
            serie: "1",
            primeiraChegada: "2026-10-08T12:00:00Z",
            itens: [
                {
                    ...(example.itens as Values[])[0],
                    id: item,
                    numeroItem: "1",
                    sku: `SKU-${item}`,
                    prevista: 100,
                    recebidaBoa: 98,
                    recebidaAvariada: 0,
                    diferenca: -2,
                },
            ],
        };
    };
    const orders = [
        makeOrder("9007199254740993", "QUARENTENA", "PED-ENT-01"),
        makeOrder("102", "EFETIVADO", "PED-ENT-02"),
    ];
    orders[1].efetivadoEm = "2026-10-09T12:15:00Z";
    const details: Record<string, Values> = {
        "9007199254740993": {
            pedido: orders[0],
            divergente: true,
            notas: [makeNote("301", "401"), makeNote("302", "402")],
        },
        "102": {
            pedido: orders[1],
            divergente: false,
            notas: [makeNote("303", "403")],
        },
    };
    const confirmedItem = (
        (details["102"].notas as Values[])[0].itens as Values[]
    )[0];
    confirmedItem.recebidaBoa = 100;
    confirmedItem.diferenca = 0;
    if (longText) {
        orders[0].referencia =
            "PED-ENT-01 / Referência extensa para conferir o alinhamento das colunas";
        orders[1].situacao = "RASCUNHO";
        (details["9007199254740993"].notas as Values[])[0].numero =
            "9007199254740994";
        const confirmed = makeOrder("105", "EFETIVADO", "PED-ENT-03");
        orders.push(confirmed);
        details["105"] = {
            pedido: confirmed,
            divergente: true,
            notas: [makeNote("306", "406")],
        };
    }
    const calls: { id: string; params: string[]; body?: Values }[] = [],
        unknown: string[] = [];
    let failure = false,
        writeFailure = false,
        detailFailure = false;
    const errors: string[] = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
        const request = route.request(),
            url = new URL(request.url());
        if (url.origin !== origin) return route.abort("blockedbyclient");
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "ped01-csrf-ficticio", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar")
            return route.fulfill({
                json: {
                    accessToken: "ped01-acesso-ficticio",
                    expiresIn: 300,
                    usuario: {
                        id: "ped01-local",
                        nome: "Pessoa de ensaio",
                        email: "teste@example.invalid",
                        perfil,
                        administrador: false,
                        principal: false,
                        ativo: true,
                        trocarSenha: false,
                        clientes: [],
                        armazens: [],
                        versao: 0,
                    },
                },
            });
        const contract = prepared.endpoints.find(
            (endpoint) =>
                endpoint.method === request.method() &&
                new RegExp(
                    "^" + endpoint.path.replace(/\{[^}]+\}/g, "([^/]+)") + "$",
                ).test(url.pathname),
        );
        if (!contract) {
            unknown.push(request.method() + " " + url.pathname);
            return route.fulfill({
                status: 404,
                json: { detail: "API sem resposta de teste" },
            });
        }
        const params = new RegExp(
            "^" + contract.path.replace(/\{[^}]+\}/g, "([^/]+)") + "$",
        )
            .exec(url.pathname)!
            .slice(1);
        const body =
            request.method() === "GET"
                ? undefined
                : (request.postDataJSON() as Values);
        calls.push({ id: contract.id, params, body });
        expect(request.headers().authorization).toBe(
            "Bearer ped01-acesso-ficticio",
        );
        if (request.method() !== "GET" && writeFailure)
            return route.fulfill({
                status: 409,
                json: {
                    detail: "Revisão desatualizada. Consulte novamente.",
                    codigo: "VERSAO_DESATUALIZADA",
                },
            });
        let data: unknown;
        if (
            ["ClienteController.listar", "ArmazemController.listar"].includes(
                contract.id,
            )
        ) {
            data = source(contract.id);
            const record = (data as Values).itens as Values[];
            record[0].id = "1";
            record[0].codigo = contract.id.startsWith("Cliente")
                ? "CLI-PED01"
                : "ARM-PED01";
            record[0].nome = contract.id.startsWith("Cliente")
                ? "Cliente de ensaio PED01"
                : "Armazém de ensaio PED01";
            if (longText)
                record[0].nome +=
                    " com nome extenso para conferir reticências e texto completo";
        } else if (contract.id === "ProdutoController.listar") {
            data = source(contract.id);
            const p = ((data as Values).itens as Values[])[0];
            p.id = "1";
            p.sku = "69230424";
            p.descricao = "Bandeja";
            p.unidadeMedida = "PEC";
        } else if (contract.id === "PedidoEntradaXmlController.previa") {
            data = nfePreview;
        } else if (contract.id === "PedidoEntradaXmlController.documento") {
            data = {
                documento: nfePreview.documento,
                xmlOriginal: readFileSync(
                    "../backend/src/test/resources/nfe/exemplo-reconstruido.xml",
                    "utf8",
                ),
                xmlHash: nfePreview.xmlHash,
            };
        } else if (contract.id === "PedidoEntradaXmlController.confirmar") {
            const created = {
                ...makeOrder("104", "RASCUNHO", String(body!.referencia)),
                versao: "1",
                origemCriacao: "XML",
            };
            const nota = makeNote("305", "405");
            nota.xmlVinculado = true;
            nota.primeiraChegada = null;
            const item = (nota.itens as Values[])[0];
            item.prevista = "32000";
            item.recebidaBoa = "0";
            item.recebidaAvariada = "0";
            item.diferenca = -32000;
            orders.push(created);
            details["104"] = {
                pedido: created,
                divergente: true,
                notas: [nota],
            };
            data = {
                tipoRecurso: "PEDIDO_ENTRADA_XML",
                operacaoId: body!.operacaoId,
                xmlHash: nfePreview.xmlHash,
                pedido: created,
            };
        } else if (contract.id === "PedidoEntradaController.listar") {
            if (failure)
                return route.fulfill({
                    status: 503,
                    json: {
                        detail: "Consulta de pedidos indisponível no teste",
                    },
                });
            const pagina = Number(url.searchParams.get("pagina") ?? 0);
            data = {
                itens: pagina === 0 ? orders : [orders[1]],
                pagina,
                tamanho: 20,
                totalItens: 21,
                totalPaginas: 2,
            };
        } else if (contract.id === "PedidoEntradaController.consultar") {
            if (detailFailure)
                return route.fulfill({
                    status: 503,
                    json: { detail: "Detalhe indisponível no teste" },
                });
            data = details[params[0]];
        } else if (contract.id === "PedidoEntradaController.criar") {
            const created = {
                ...makeOrder("103", "RASCUNHO", String(body!.referencia)),
                versao: "0",
            };
            orders.push(created);
            details["103"] = { pedido: created, divergente: false, notas: [] };
            data = created;
        } else if (
            contract.id.startsWith("PedidoEntradaController.") &&
            request.method() !== "GET"
        ) {
            const order = orders.find((item) => item.id === params[0])!;
            expect(String(body!.versao)).toBe(String(order.versao));
            if (contract.id.endsWith(".xml")) {
                const imported = makeNote("304", "404");
                imported.primeiraChegada = null;
                imported.xmlVinculado = true;
                const item = (imported.itens as Values[])[0];
                item.recebidaBoa = 0;
                item.diferenca = -100;
                details[params[0]].notas = [imported];
                details[params[0]].divergente = true;
            }
            if (contract.id.endsWith(".iniciar"))
                order.situacao = "EM_CONFERENCIA";
            if (contract.id.endsWith(".efetivar")) {
                order.situacao = "EFETIVADO";
                order.efetivadoEm = "2026-10-09T13:00:00Z";
                order.motivoConclusao = body!.motivo;
            }
            order.versao = String(Number(order.versao) + 1);
            data = order;
        } else data = source(contract.id);
        return route.fulfill({
            contentType: "application/json",
            body: contract.id.startsWith("PedidoEntradaXmlController.")
                ? stringify(contractWire(contract.response, data))!
                : stringify(wire(data))!,
        });
    });
    return {
        calls,
        unknown,
        errors,
        fail: (value: boolean) => {
            failure = value;
        },
        failWrite: (value: boolean) => {
            writeFailure = value;
        },
        failDetail: (value: boolean) => {
            detailFailure = value;
        },
    };
}
async function open(page: Page, theme: string, width = 1440) {
    await page.setViewportSize({ width, height: width < 600 ? 800 : 1000 });
    await page.addInitScript(
        (value) => localStorage.setItem("wms.theme", value),
        theme,
    );
    await page.goto("/#entrada");
    const context = page.getByRole("form", { name: "Contexto operacional" });
    await context
        .getByRole("combobox", { name: "Cliente", exact: true })
        .fill("Cliente de ensaio");
    await page.getByRole("option", { name: /Cliente de ensaio/ }).click();
    await context
        .getByRole("combobox", { name: "Armazém", exact: true })
        .fill("Armazém de ensaio");
    await page.getByRole("option", { name: /Armazém de ensaio/ }).click();
    await context.getByRole("button", { name: "Aplicar contexto" }).click();
    await expect(
        page.getByRole("cell", { name: "Divergente", exact: true }),
    ).toBeVisible();
}
async function ready(page: Page) {
    await expect(
        page.getByText(
            /Carregando os dados atuais do registro|Atualizando notas, itens e conferência/,
        ),
    ).toHaveCount(0);
}
for (const [width, theme] of [
    [1440, "light"],
    [1440, "dark"],
    [1152, "light"],
    [1152, "dark"],
    [390, "light"],
    [390, "dark"],
] as const) {
    test(`PED01 tabela alinhada, textos completos e ações em ${width}px ${theme}`, async ({
        page,
    }, info) => {
        const mock = await api(page, "SUPERVISOR", true);
        await open(page, theme, width);
        const table = page.getByRole("region", {
            name: "Pedidos de entrada, tabela com rolagem horizontal",
        });
        const row = table.locator("tbody tr").first();
        const identity = await row
            .locator(".receiving-order-identity > *")
            .evaluateAll((elements) =>
                elements.map((element) => {
                    const rect = element.getBoundingClientRect();
                    return {
                        center: rect.top + rect.height / 2,
                        left: rect.left,
                        right: rect.right,
                    };
                }),
            );
        expect(
            Math.abs(identity[0].center - identity[1].center),
        ).toBeLessThanOrEqual(1);
        expect(identity[1].left - identity[0].right).toBeGreaterThanOrEqual(7);
        const client = row.locator("td").nth(0).locator(".record-text");
        await expect(client).toHaveAttribute("tabindex", "0");
        await expect(client).toHaveCSS("text-overflow", "ellipsis");
        await client.hover();
        await expect(page.getByRole("tooltip")).toHaveText(
            await client.innerText(),
        );
        await page.screenshot({ path: info.outputPath("nomes.png") });
        await page.keyboard.press("Escape");
        const conference = table
            .locator("tbody tr")
            .nth(2)
            .locator("td")
            .nth(6)
            .locator(".record-text");
        await expect(conference).toHaveAttribute("tabindex", "0");
        await conference.focus();
        await expect(page.getByRole("tooltip")).toHaveText(
            "Conferido com divergência",
        );
        await page.keyboard.press("Escape");
        await client.focus();
        await expect(page.getByRole("tooltip")).toHaveText(
            await client.innerText(),
        );
        await page.keyboard.press("Escape");
        const notes = row.locator("td").nth(3).locator(".record-text");
        await notes.focus();
        await expect(page.getByRole("tooltip")).toContainText(
            "9007199254740994",
        );
        await page.keyboard.press("Escape");
        const warehouse = row.locator("td").nth(1).locator(".record-text");
        await warehouse.hover();
        await expect(page.getByRole("tooltip")).toHaveText(
            await warehouse.innerText(),
        );
        await page.keyboard.press("Escape");
        const dimensions = await table.evaluate((element) => ({
            scrollWidth: element.scrollWidth,
            clientWidth: element.clientWidth,
            rows: Array.from(element.querySelectorAll("tbody tr")).map(
                (row) => row.getBoundingClientRect().height,
            ),
            wraps: Array.from(element.querySelectorAll("th, td")).map(
                (cell) => getComputedStyle(cell).whiteSpace,
            ),
        }));
        expect(dimensions.scrollWidth).toBeGreaterThan(dimensions.clientWidth);
        expect(dimensions.wraps.every((value) => value === "nowrap")).toBe(
            true,
        );
        await info.attach("dimensoes-tabela", {
            body: JSON.stringify(dimensions),
            contentType: "application/json",
        });
        dimensions.rows.forEach((height) =>
            expect(height).toBeLessThanOrEqual(64),
        );
        await table.focus();
        await table.evaluate((element) => {
            element.scrollLeft = 0;
        });
        await page.keyboard.press("ArrowRight");
        await expect
            .poll(() => table.evaluate((element) => element.scrollLeft))
            .toBeGreaterThan(0);
        const more = row.getByRole("button", { name: /Mais ações/ });
        await more.scrollIntoViewIfNeeded();
        await expect(more.locator(".receiving-more-icon")).toHaveText("⋮");
        await expect(more).toHaveCSS("background-color", "rgba(0, 0, 0, 0)");
        await expect(more).toHaveCSS("border-top-width", "0px");
        const view = row.getByRole("button", {
            name: "Ver pedido",
            exact: true,
        });
        await view.hover();
        await expect(page.getByRole("tooltip")).toHaveText("Ver pedido");
        await page.keyboard.press("Escape");
        await more.hover();
        await expect(page.getByRole("tooltip")).toHaveText("Mais ações");
        await page.keyboard.press("Escape");
        const positions = await row
            .locator(".receiving-row-actions > button")
            .evaluateAll((buttons) =>
                buttons.map((button) => {
                    const rect = button.getBoundingClientRect();
                    return {
                        top: rect.top,
                        left: rect.left,
                        right: rect.right,
                        height: rect.height,
                        overflow: button.scrollWidth > button.clientWidth,
                    };
                }),
            );
        expect(positions).toHaveLength(3);
        expect(new Set(positions.map((button) => button.top)).size).toBe(1);
        const actionsRight = await row
            .locator(".receiving-row-actions")
            .evaluate((element) => element.getBoundingClientRect().right);
        expect(Math.abs(actionsRight - positions[2].right)).toBeLessThanOrEqual(
            1,
        );
        await expect(table.locator("thead th").last()).toHaveCSS(
            "text-align",
            "right",
        );
        expect(positions.every((button) => !button.overflow)).toBe(true);
        expect(positions[1].left - positions[0].right).toBeGreaterThanOrEqual(
            7,
        );
        expect(positions[2].left - positions[1].right).toBeGreaterThanOrEqual(
            7,
        );
        if (width < 600)
            positions.forEach((button) =>
                expect(button.height).toBeGreaterThanOrEqual(44),
            );
        const height = await row.evaluate(
            (element) => element.getBoundingClientRect().height,
        );
        await more.focus();
        await page.keyboard.press("Enter");
        const menu = page.getByRole("group", {
            name: /Ações do pedido 9007199254740993/,
        });
        await expect(menu).toBeVisible();
        expect(
            await row.evaluate(
                (element) => element.getBoundingClientRect().height,
            ),
        ).toBe(height);
        const bounds = await menu.boundingBox();
        expect(bounds!.x).toBeGreaterThanOrEqual(8);
        expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(width - 8);
        await page.screenshot({ path: info.outputPath("acoes.png") });
        await page.keyboard.press("Escape");
        await expect(menu).not.toBeVisible();
        await expect(more).toBeFocused();
        await more.click();
        await menu
            .getByRole("button", {
                name: "Importar XML existente",
                exact: true,
            })
            .click();
        await expect(page.getByRole("dialog")).toBeVisible();
        await ready(page);
        await expect(
            page.getByLabel("Identificador *", { exact: true }),
        ).toHaveValue("9007199254740993");
        expect(
            await page.evaluate(
                () => document.documentElement.scrollWidth <= innerWidth,
            ),
        ).toBe(true);
        expect(mock.calls.every((call) => !call.body)).toBe(true);
        expect(mock.unknown).toEqual([]);
        expect(mock.errors).toEqual([]);
    });
}
test.describe("PED01 toque na tabela", () => {
    test.use({ hasTouch: true });
    test("revela o texto completo e fecha o menu ao tocar fora ou rolar", async ({
        page,
    }) => {
        const mock = await api(page, "SUPERVISOR", true);
        await open(page, "light", 390);
        const table = page.getByRole("region", {
            name: "Pedidos de entrada, tabela com rolagem horizontal",
        });
        const row = table.locator("tbody tr").first();
        const client = row.locator("td").first().locator(".record-text");
        await client.tap();
        await expect(page.getByRole("tooltip")).toHaveText(
            await client.innerText(),
        );
        await page.keyboard.press("Escape");
        const more = row.getByRole("button", { name: /Mais ações/ });
        await more.scrollIntoViewIfNeeded();
        await more.tap();
        const menu = page.getByRole("group", {
            name: "Ações do pedido 9007199254740993",
        });
        await expect(menu).toBeVisible();
        await row.getByRole("cell", { name: "Quarentena", exact: true }).tap();
        await expect(menu).not.toBeVisible();
        await more.tap();
        await expect(menu).toBeVisible();
        await table.evaluate((element) => {
            element.scrollLeft -= 30;
        });
        await expect(menu).not.toBeVisible();
        expect(mock.calls.every((call) => !call.body)).toBe(true);
        expect(mock.errors).toEqual([]);
    });
});
for (const [width, theme] of [
    [1440, "light"],
    [1440, "dark"],
    [360, "light"],
    [360, "dark"],
] as const) {
    test(`PED01 lista, notas múltiplas, detalhe, teclado e filtros em ${width}px ${theme}`, async ({
        page,
    }, info) => {
        const mock = await api(page);
        await open(page, theme, width);
        await expect(
            page.getByRole("heading", {
                name: "Pedidos de entrada",
                exact: true,
            }),
        ).toBeVisible();
        await expect(
            page.getByRole("navigation", {
                name: "Etapas de Entrada e conferência",
            }),
        ).toHaveCount(0);
        await expect(
            page.getByRole("cell", { name: "1/301, 1/302" }),
        ).toBeVisible();
        await page.screenshot({
            path: info.outputPath("lista.png"),
            fullPage: true,
        });
        await page
            .getByLabel("Conferência nesta página")
            .selectOption("Divergente");
        await expect(
            page.getByRole("button", { name: /Ver detalhes de PED-ENT-02/ }),
        ).toHaveCount(0);
        await page.getByLabel("Pedido ou nota nesta página").fill("302");
        const link = page.getByRole("button", {
            name: /Ver detalhes de PED-ENT-01/,
        });
        await link.focus();
        await page.keyboard.press("Enter");
        const dialog = page.getByRole("dialog");
        await ready(page);
        await expect(
            dialog.getByRole("navigation", { name: "Seções do pedido" }),
        ).toBeVisible();
        await dialog
            .getByRole("button", { name: "Notas e itens", exact: true })
            .click();
        await expect(
            dialog.getByRole("region", { name: "Nota 1/301" }),
        ).toBeVisible();
        await expect(
            dialog.getByRole("region", { name: "Nota 1/302" }),
        ).toBeVisible();
        await page.screenshot({ path: info.outputPath("pedido-notas.png") });
        await dialog
            .getByRole("button", { name: "Selecionar item 402 da nota 302" })
            .click();
        await dialog
            .getByRole("button", { name: "Conferência", exact: true })
            .click();
        await expect(
            dialog.getByRole("button", { name: "Efetivar carga integral" }),
        ).toBeVisible();
        await dialog
            .getByRole("button", { name: "Registrar chegada física" })
            .click();
        await expect(
            dialog.getByLabel("Itens / Item 1 / Item da nota (ID) *", {
                exact: true,
            }),
        ).toHaveValue("402");
        await page.screenshot({
            path: info.outputPath("pedido-conferencia.png"),
        });
        await expect
            .poll(() =>
                page.evaluate(
                    () => document.documentElement.scrollWidth <= innerWidth,
                ),
            )
            .toBe(true);
        await page.keyboard.press("Escape");
        await expect(dialog).toHaveCount(0);
        await expect(link).toBeFocused();
        expect(mock.calls.every((call) => !call.body)).toBe(true);
        expect(mock.unknown).toEqual([]);
        expect(mock.errors).toEqual([]);
    });
}
test("PED01 criação continua com XML no pedido novo e atualiza a fila sem exigir ID", async ({
    page,
}) => {
    const mock = await api(page);
    await open(page, "light");
    await page
        .getByRole("button", { name: "Novo pedido", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Criar manualmente", exact: true })
        .click();
    const dialog = page.getByRole("dialog");
    await dialog
        .getByLabel("Referência *", { exact: true })
        .fill("PED-NOVO-XML");
    await dialog.getByRole("button", { name: "Conferir e confirmar" }).click();
    await dialog.getByRole("button", { name: "Confirmar agora" }).click();
    await expect(
        dialog.getByRole("navigation", { name: "Seções do pedido" }),
    ).toBeVisible();
    await ready(page);
    await dialog
        .getByRole("button", { name: "Notas e itens", exact: true })
        .click();
    await dialog
        .getByRole("button", { name: "Importar XML existente" })
        .click();
    await expect(
        dialog.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("103");
    await expect(
        dialog.getByLabel("Identificador *", { exact: true }),
    ).toBeDisabled();
    await dialog
        .getByLabel("XML existente da NF-e *", { exact: true })
        .fill("<nfe>arquivo fictício interceptado</nfe>");
    await dialog.getByRole("button", { name: "Conferir e confirmar" }).click();
    await dialog.getByRole("button", { name: "Confirmar agora" }).click();
    await expect(
        dialog.getByRole("region", { name: "Nota 1/304" }),
    ).toBeVisible();
    await ready(page);
    expect(
        mock.calls.find((call) => call.id === "PedidoEntradaController.xml")
            ?.params,
    ).toEqual(["103"]);
    expect(
        mock.calls.filter((call) => call.body).map((call) => call.id),
    ).toEqual(["PedidoEntradaController.criar", "PedidoEntradaController.xml"]);
    await dialog.getByRole("button", { name: "Voltar à lista" }).click();
    await expect(
        page.getByRole("button", { name: /Ver detalhes de PED-NOVO-XML/ }),
    ).toBeVisible();
    expect(mock.unknown).toEqual([]);
    expect(mock.errors).toEqual([]);
});
test("PED01 erros, ausência, permissões e paginação conservam contexto e filtros", async ({
    page,
}) => {
    const mock = await api(page, "OPERACAO");
    await open(page, "dark", 390);
    await page
        .getByRole("button", { name: /Ver detalhes de PED-ENT-01/ })
        .click();
    await ready(page);
    const dialog = page.getByRole("dialog");
    await dialog
        .getByRole("button", { name: "Conferência", exact: true })
        .click();
    await expect(
        dialog.getByRole("button", { name: "Efetivar carga integral" }),
    ).toHaveCount(0);
    await expect(
        dialog.getByRole("button", { name: "Estornar chegada" }),
    ).toHaveCount(0);
    await page.keyboard.press("Escape");
    await page.getByLabel("Pedido ou nota nesta página").fill("ausente");
    await expect(
        page.getByText(/Nenhum pedido corresponde aos filtros desta página/),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "Limpar busca nesta página" })
        .click();
    await page.getByRole("button", { name: "Próxima", exact: true }).click();
    await expect(page.getByText("Página 2 de 2")).toBeVisible();
    await page.getByLabel("Pedido ou nota nesta página").fill("PED-ENT-02");
    await page
        .getByRole("button", { name: "Atualizar lista", exact: true })
        .click();
    await expect(page.getByLabel("Pedido ou nota nesta página")).toHaveValue(
        "PED-ENT-02",
    );
    mock.fail(true);
    await page
        .getByRole("button", { name: "Atualizar lista", exact: true })
        .click();
    await expect(
        page.getByText("Consulta de pedidos indisponível no teste"),
    ).toBeVisible();
    mock.fail(false);
    await page
        .getByRole("button", { name: "Tentar novamente", exact: true })
        .click();
    await expect(page.getByText("Página 2 de 2")).toBeVisible();
    await expect(page.getByLabel("Pedido ou nota nesta página")).toHaveValue(
        "PED-ENT-02",
    );
    expect(mock.unknown).toEqual([]);
    expect(mock.errors).toEqual([]);
});

for (const [width, theme] of [
    [1440, "light"],
    [1440, "dark"],
    [360, "light"],
    [360, "dark"],
] as const) {
    test(`XML01 prévia e confirmação em ${width}px ${theme}`, async ({
        page,
    }, info) => {
        const mock = await api(page);
        await open(page, theme, width);
        await page
            .getByLabel("Pedido ou nota nesta página")
            .fill("fora-do-filtro");
        await page
            .getByRole("button", { name: "Novo pedido", exact: true })
            .click();
        await page
            .getByRole("button", { name: "Importar XML da NF-e", exact: true })
            .click();
        const dialog = page.getByRole("dialog");
        await dialog
            .getByLabel("Arquivo XML da NF-e")
            .setInputFiles(
                "../backend/src/test/resources/nfe/exemplo-reconstruido.xml",
            );
        await dialog
            .getByRole("button", { name: "Ler XML", exact: true })
            .click();
        await expect(
            dialog.getByText("TIGRE FERRAMENTAS PARA CONSTRUCAO C"),
        ).toBeVisible();
        await expect(dialog.getByText("04547874000203")).toBeVisible();
        await expect(dialog.getByText(/0.7083000000/)).toBeVisible();
        expect(
            mock.calls.some(
                (c) => c.id === "PedidoEntradaXmlController.confirmar",
            ),
        ).toBe(false);
        await expect(
            dialog.getByRole("button", { name: "Confirmar pedido de entrada" }),
        ).toBeEnabled();
        await page.screenshot({
            path: info.outputPath("xml-previa.png"),
            fullPage: true,
        });
        expect(
            await page.evaluate(
                () => document.documentElement.scrollWidth <= window.innerWidth,
            ),
        ).toBe(true);
        await dialog
            .getByRole("button", { name: "Confirmar pedido de entrada" })
            .focus();
        await page.keyboard.press("Enter");
        await expect(
            page.getByRole("button", { name: "Ver registro confirmado" }),
        ).toBeVisible();
        await expect(
            page.getByLabel("Pedido ou nota nesta página"),
        ).toHaveValue("fora-do-filtro");
        await page
            .getByRole("button", { name: "Ver registro confirmado" })
            .click();
        await ready(page);
        await dialog
            .getByRole("button", { name: "Notas e itens", exact: true })
            .click();
        await dialog
            .getByRole("button", {
                name: "Ver documento original",
                exact: true,
            })
            .click();
        await expect(
            dialog.getByText("DALGA LOGISTICA E TRANSPORTES LTDA"),
        ).toBeVisible();
        await page.screenshot({
            path: info.outputPath("xml-salvo.png"),
            fullPage: true,
        });
        expect(
            mock.calls.filter(
                (c) => c.id === "PedidoEntradaXmlController.confirmar",
            ),
        ).toHaveLength(1);
        expect(mock.unknown).toEqual([]);
        expect(mock.errors).toEqual([]);
    });
}
