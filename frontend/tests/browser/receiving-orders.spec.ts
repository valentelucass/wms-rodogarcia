import { test, expect, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";
import { LosslessNumber, stringify } from "lossless-json";
import type { Endpoint, Values } from "../../src/contracts/runtime";

const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { endpoints: Endpoint[]; fixtures: Record<string, string> };
const source = (id: string) => JSON.parse(prepared.fixtures[id]) as Values;
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
        /^(id|versao|serie|numero|numeroItem|pagina|tamanho|totalItens|totalPaginas|prevista|recebidaBoa|recebidaAvariada|diferenca)$|Id$/.test(
            key,
        ) &&
        /^\d+(\.\d+)?$/.test(value)
        ? new LosslessNumber(value)
        : value;
}
const origin = "http://127.0.0.1:5222";
async function api(page: Page, perfil = "SUPERVISOR") {
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
            body: stringify(wire(data))!,
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
    await page.getByRole("button", { name: "Novo pedido de entrada" }).click();
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
