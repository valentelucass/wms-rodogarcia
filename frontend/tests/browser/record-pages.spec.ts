import { test, expect, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";
import { stringify, LosslessNumber } from "lossless-json";
import type { Endpoint, Values } from "../../src/contracts/runtime";
import type { RecordPage } from "../../src/domain/recordPages";
const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as {
    journeys: { id: string; title: string; steps: { title: string }[] }[];
    pages: Record<string, RecordPage[]>;
    endpoints: Endpoint[];
    fixtures: Record<string, string>;
};
const { journeys, endpoints, fixtures, pages: recordPages } = prepared;
const endpoint = (id: string) => endpoints.find((e) => e.id === id)!;
const clientTemplate = JSON.parse(
    fixtures["ClienteController.consultar"],
) as Values;
function clientWire(value: unknown, key = ""): unknown {
    return Array.isArray(value)
        ? value.map((v) => clientWire(v))
        : value && typeof value === "object"
          ? Object.fromEntries(
                Object.entries(value).map(([k, v]) => [k, clientWire(v, k)]),
            )
          : typeof value === "string" &&
              ["id", "versao", "totalItens"].includes(key)
            ? new LosslessNumber(value)
            : value;
}

const origin = "http://127.0.0.1:5199";
async function isolatedApi(page: Page, perfil = "GESTOR") {
    const clients: Values[] = [
        {
            ...clientTemplate,
            id: "9007199254740993",
            codigo: "CLI-A",
            nome: "Cliente Alfa com nome longo para conferir a tabela",
            documentoFiscal: "11111111111",
            situacao: "ATIVO",
            versao: "3",
        },
        {
            ...clientTemplate,
            id: "42",
            codigo: "CLI-B",
            nome: "Cliente Beta",
            documentoFiscal: "22222222222222",
            situacao: "ENCERRAMENTO_PENDENTE",
            versao: "8",
        },
    ];
    const calls: {
        id: string;
        method: string;
        params: string[];
        body?: Values;
    }[] = [];
    const unknown: string[] = [];
    let failList = false,
        failWrite = false;
    const user = {
        id: "reg01-fixture",
        nome: "Pessoa de teste",
        email: "teste@example.invalid",
        perfil,
        administrador: false,
        principal: false,
        ativo: true,
        trocarSenha: false,
        clientes: [],
        armazens: [],
        versao: 0,
    };
    await page.route("**/*", async (route) => {
        const request = route.request(),
            url = new URL(request.url());
        if (url.origin !== origin) return route.abort("blockedbyclient");
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "csrf-test", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar")
            return route.fulfill({
                json: {
                    accessToken: "isolated-reg01",
                    expiresIn: 300,
                    usuario: user,
                },
            });
        const contract = endpoints.find(
            (e) =>
                e.method === request.method() &&
                new RegExp(
                    "^" + e.path.replace(/\{[^}]+\}/g, "([^/]+)") + "$",
                ).test(url.pathname),
        );
        if (!contract) {
            unknown.push(request.method() + " " + url.pathname);
            return route.fulfill({
                status: 404,
                json: { detail: "Rota sem fixture isolado" },
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
        calls.push({ id: contract.id, method: request.method(), params, body });
        if (request.method() !== "GET" && failWrite)
            return route.fulfill({
                status: 409,
                json: {
                    detail: "Revisão mudou. Consulte os dados atuais.",
                    codigo: "VERSAO_DESATUALIZADA",
                },
            });
        let data: unknown = undefined;
        if (contract.id === "ClienteController.listar") {
            if (failList)
                return route.fulfill({
                    status: 503,
                    json: { detail: "Falha de carregamento para teste" },
                });
            data = {
                itens: clients,
                pagina: 0,
                tamanho: 20,
                totalItens: String(clients.length),
                totalPaginas: 1,
            };
        }
        if (contract.id === "ClienteController.criar") {
            const row = {
                ...clientTemplate,
                ...body,
                id: "77",
                versao: "0",
                situacao: "ATIVO",
            };
            clients.push(row);
            data = row;
        } else if (
            contract.id.startsWith("ClienteController.") &&
            contract.id !== "ClienteController.listar"
        ) {
            const row = clients.find((row) => row.id === params[0]);
            if (!row)
                return route.fulfill({
                    status: 404,
                    json: { detail: "Cliente não encontrado" },
                });
            if (request.method() !== "GET") {
                expect(String(body?.versao)).toBe(String(row.versao));
                if (contract.id.endsWith("alterar")) row.nome = body?.nome;
                if (contract.id.endsWith("encerrar"))
                    row.situacao = "ENCERRAMENTO_PENDENTE";
                if (contract.id.endsWith("reativar")) row.situacao = "ATIVO";
                row.versao = String(BigInt(String(row.versao)) + 1n);
            }
            data = row;
        }
        return route.fulfill({
            contentType: "application/json",
            body:
                data === undefined
                    ? fixtures[contract.id]
                    : stringify(clientWire(data)),
        });
    });
    return {
        calls,
        clients,
        unknown,
        failList: (v: boolean) => {
            failList = v;
        },
        failWrite: (v: boolean) => {
            failWrite = v;
        },
    };
}
async function loaded(page: Page) {
    await expect(
        page.getByText(
            /Carregando registros|Atualizando os registros apresentados/,
        ),
    ).toHaveCount(0);
}

for (const [width, height, theme] of [
    [1440, 900, "light"],
    [360, 740, "dark"],
] as const) {
    test(`REG01 12 áreas e todas as etapas em ${width}px ${theme}`, async ({
        page,
    }, info) => {
        const api = await isolatedApi(page);
        const errors: string[] = [];
        page.on("pageerror", (e) => errors.push(e.message));
        await page.setViewportSize({ width, height });
        await page.addInitScript(
            (theme) => localStorage.setItem("wms.theme", theme),
            theme,
        );
        await page.goto("/#cadastros");
        const context = page.getByRole("form", {
            name: "Contexto operacional",
        });
        await context
            .getByRole("combobox", { name: "Cliente", exact: true })
            .fill("Cliente Beta");
        await page.getByRole("option", { name: /Cliente Beta/ }).click();
        await context
            .getByRole("combobox", { name: "Armazém", exact: true })
            .fill("DEMO-ARMAZEM");
        await page.getByRole("option", { name: /DEMO-ARMAZEM/ }).click();
        await context.getByRole("button", { name: "Aplicar contexto" }).click();
        for (const journey of journeys) {
            await page.evaluate((id) => {
                location.hash = id;
            }, journey.id);
            await expect(page.locator(".record-workspace")).toBeVisible();
            const views =
                journey.id === "entrada" ? [0] : journey.steps.map((_, i) => i);
            for (const i of views) {
                if (i)
                    await page
                        .getByRole("navigation", {
                            name: "Etapas de " + journey.title,
                        })
                        .getByRole("button", {
                            name: journey.steps[i].title,
                            exact: true,
                        })
                        .click();
                await loaded(page);
                await expect(
                    page.getByRole("group", { name: "Ações desta etapa" }),
                ).toHaveCount(0);
                const definition = recordPages[journey.id][i];
                const required = [
                    ...endpoint(definition.source).params,
                    ...endpoint(definition.source).query,
                ].filter(
                    (f) =>
                        f.required &&
                        !["clienteId", "armazemId", "fuso"].includes(f.name),
                );
                if (required.length)
                    await expect(
                        page.getByText(
                            "Informe as referências obrigatórias nos filtros para carregar esta visão.",
                        ),
                    ).toBeVisible();
                else
                    await expect(
                        page.locator(".record-workspace table").first(),
                    ).toBeVisible();
                expect(
                    await page.evaluate(
                        () =>
                            document.documentElement.scrollWidth <= innerWidth,
                    ),
                ).toBe(true);
            }
            await page.screenshot({
                path: info.outputPath(`${journey.id}-${theme}.png`),
                fullPage: true,
            });
        }
        expect(errors).toEqual([]);
        expect(api.unknown).toEqual([]);
    });
    test(`REG01 Clientes criar editar situação teclado e filtros em ${width}px`, async ({
        page,
    }, info) => {
        const api = await isolatedApi(page);
        await page.setViewportSize({ width, height });
        await page.addInitScript(
            (theme) => localStorage.setItem("wms.theme", theme),
            theme,
        );
        await page.goto("/#cadastros");
        const row = page.getByRole("row").filter({ hasText: "CLI-A" });
        await expect(row).toBeVisible();
        const edit = row.getByRole("button", { name: "Editar", exact: true });
        await edit.click();
        const dialog = page.getByRole("dialog");
        await expect(dialog.getByLabel("Nome *", { exact: true })).toHaveValue(
            String(api.clients[0].nome),
        );
        await expect(
            dialog.getByLabel("Identificador *", { exact: true }),
        ).toHaveValue("9007199254740993");
        await expect(
            dialog.getByLabel("Identificador *", { exact: true }),
        ).toBeDisabled();
        await expect(page.getByRole("dialog")).toHaveCount(1);
        await dialog
            .getByLabel("Nome *", { exact: true })
            .fill("Cliente Alfa atualizado");
        page.once("dialog", (d) => d.dismiss());
        await page.keyboard.press("Escape");
        await expect(dialog).toBeVisible();
        page.once("dialog", (d) => d.accept());
        await page.keyboard.press("Escape");
        await expect(dialog).toHaveCount(0);
        await expect(edit).toBeFocused();
        expect(api.calls.filter((r) => r.method !== "GET")).toHaveLength(0);
        await edit.click();
        await dialog
            .getByLabel("Nome *", { exact: true })
            .fill("Cliente Alfa atualizado");
        await dialog
            .getByLabel("Motivo / justificativa *", { exact: true })
            .fill("Correção revisada da apresentação");
        await dialog
            .getByRole("button", { name: "Conferir e confirmar" })
            .click();
        await expect(page.getByRole("dialog")).toHaveCount(1);
        await dialog.getByRole("button", { name: "Confirmar agora" }).click();
        await expect(
            page.getByText(
                "Operação confirmada pelo servidor. Lista em atualização.",
            ),
        ).toBeVisible();
        await dialog
            .getByRole("button", { name: "Solicitar desativação de cliente" })
            .click();
        await dialog
            .getByLabel("Motivo / justificativa *", { exact: true })
            .fill("Solicitação conferida");
        await dialog
            .getByRole("button", { name: "Conferir e confirmar" })
            .click();
        await dialog.getByRole("button", { name: "Confirmar agora" }).click();
        await expect(
            dialog.getByRole("button", { name: "Reativar cliente" }),
        ).toBeVisible();
        await dialog.getByRole("button", { name: "Reativar cliente" }).click();
        await dialog
            .getByLabel("Motivo / justificativa *", { exact: true })
            .fill("Reativação conferida");
        await dialog
            .getByRole("button", { name: "Conferir e confirmar" })
            .click();
        await dialog.getByRole("button", { name: "Confirmar agora" }).click();
        await expect(
            dialog.getByRole("button", { name: "Voltar à lista" }),
        ).toBeEnabled();
        await dialog.getByRole("button", { name: "Voltar à lista" }).click();
        await expect(row.locator(".record-status")).toHaveText("Ativo");
        await page.getByLabel("Buscar nos registros desta página").fill("Alfa");
        await page.getByRole("button", { name: "Novo cliente" }).click();
        await dialog.getByLabel("Código *", { exact: true }).fill("CLI-C");
        await dialog.getByLabel("Nome *", { exact: true }).fill("Cliente Gama");
        await dialog
            .getByLabel("CPF / CNPJ *", { exact: true })
            .fill("33333333333");
        await dialog
            .getByRole("button", { name: "Conferir e confirmar" })
            .click();
        await dialog.getByRole("button", { name: "Confirmar agora" }).click();
        await expect(
            dialog.getByRole("button", { name: "Voltar à lista" }),
        ).toBeEnabled();
        await dialog.getByRole("button", { name: "Voltar à lista" }).click();
        await expect(
            page.getByLabel("Buscar nos registros desta página"),
        ).toHaveValue("Alfa");
        await page
            .getByRole("button", { name: "Ver registro confirmado" })
            .click();
        await expect(dialog).toContainText("Cliente Gama");
        await page.screenshot({
            path: info.outputPath(`cliente-modal-${theme}.png`),
            fullPage: true,
        });
        const writes = api.calls.filter((r) => r.method !== "GET");
        expect(writes.map((r) => r.id)).toEqual([
            "ClienteController.alterar",
            "ClienteController.encerrar",
            "ClienteController.reativar",
            "ClienteController.criar",
        ]);
        expect(
            writes.slice(0, 3).every((r) => r.params[0] === "9007199254740993"),
        ).toBe(true);
        expect(api.unknown).toEqual([]);
    });
}
test("REG01 somente consulta, falha de lista, vazio de busca e conflito preservado", async ({
    page,
}) => {
    const api = await isolatedApi(page);
    await page.goto("/#cadastros");
    await expect(
        page.getByRole("row").filter({ hasText: "CLI-A" }),
    ).toBeVisible();
    await page
        .getByLabel("Buscar nos registros desta página")
        .fill("não existe");
    await expect(page.getByText(/Nenhum resultado para a busca/)).toBeVisible();
    await page.getByLabel("Buscar nos registros desta página").fill("");
    api.failList(true);
    await page.getByRole("button", { name: "Atualizar lista" }).click();
    await expect(
        page.getByText(/Falha de carregamento para teste/),
    ).toBeVisible();
    api.failList(false);
    await page.getByRole("button", { name: "Tentar novamente" }).click();
    await page
        .getByRole("row")
        .filter({ hasText: "CLI-A" })
        .getByRole("button", { name: "Editar", exact: true })
        .click();
    const dialog = page.getByRole("dialog");
    await dialog.getByLabel("Nome *", { exact: true }).fill("Texto preservado");
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Correção solicitada");
    api.failWrite(true);
    await dialog.getByRole("button", { name: "Conferir e confirmar" }).click();
    await dialog.getByRole("button", { name: "Confirmar agora" }).click();
    await expect(dialog.getByText(/Revisão mudou/)).toBeVisible();
    await expect(dialog.getByLabel("Nome *", { exact: true })).toHaveValue(
        "Texto preservado",
    );
    await expect(
        page.getByText(
            "Operação confirmada pelo servidor. Lista em atualização.",
        ),
    ).toHaveCount(0);
});
test("REG01 Operação consulta detalhes sem receber edição nem desativação", async ({
    page,
}) => {
    await isolatedApi(page, "OPERACAO");
    await page.goto("/#cadastros");
    await expect(
        page.getByRole("row").filter({ hasText: "CLI-A" }),
    ).toBeVisible();
    await expect(
        page.getByRole("button", { name: "Novo cliente" }),
    ).toHaveCount(0);
    await expect(
        page.getByRole("button", { name: "Editar", exact: true }),
    ).toHaveCount(0);
    await page
        .getByRole("row")
        .filter({ hasText: "CLI-A" })
        .getByRole("button", { name: "Ver detalhes", exact: true })
        .click();
    await expect(page.getByRole("dialog")).toContainText("Cliente Alfa");
    await expect(
        page.getByRole("button", { name: "Solicitar desativação de cliente" }),
    ).toHaveCount(0);
});
