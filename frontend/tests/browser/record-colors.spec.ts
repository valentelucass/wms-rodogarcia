import { test, expect, type Page, type Locator } from "@playwright/test";
import { readFileSync } from "node:fs";
import { stringify, LosslessNumber } from "lossless-json";
import type { Values } from "../../src/contracts/runtime";

const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { fixtures: Record<string, string> };
const origin = "http://127.0.0.1:5223";
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
        /^(id|versao|totalItens)$|Id$/.test(key) &&
        /^\d+$/.test(value)
        ? new LosslessNumber(value)
        : value;
}
async function mockApi(page: Page, perfil = "GESTOR") {
    const template = JSON.parse(
        prepared.fixtures["ClienteController.consultar"],
    ) as Values;
    const clients = Array.from({ length: 20 }, (_, index) => ({
        ...template,
        id: String(101 + index),
        versao: "3",
        codigo: `CLI-${String(index + 1).padStart(3, "0")}`,
        nome:
            [
                "Cliente ativo de demonstração",
                "Cliente com encerramento pendente",
                "Cliente inativo de demonstração",
            ][index] ??
            `Cliente exemplo ${index + 1} com nome para conferir a leitura`,
        documentoFiscal: String(12345678000100 + index),
        situacao:
            ["ATIVO", "ENCERRAMENTO_PENDENTE", "INATIVO"][index] ?? "ATIVO",
    }));
    const calls: string[] = [],
        writes: string[] = [],
        errors: string[] = [],
        unknown: string[] = [];
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
        const request = route.request(),
            url = new URL(request.url());
        if (url.origin !== origin) return route.abort("blockedbyclient");
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "colors-csrf-ficticio", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar")
            return route.fulfill({
                json: {
                    accessToken: "colors-acesso-ficticio",
                    expiresIn: 300,
                    usuario: {
                        id: "colors-local",
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
        calls.push(request.method() + " " + url.pathname);
        if (request.method() !== "GET") {
            writes.push(url.pathname);
            return route.fulfill({
                status: 400,
                json: { detail: "Escrita não prevista no recorte visual" },
            });
        }
        let data: unknown;
        if (url.pathname === "/api/v1/clientes")
            data = {
                itens: clients,
                pagina: 0,
                tamanho: 20,
                totalItens: "20",
                totalPaginas: 1,
            };
        else if (/^\/api\/v1\/clientes\/\d+$/.test(url.pathname))
            data = clients.find(
                (client) => client.id === url.pathname.split("/").at(-1),
            );
        else if (url.pathname === "/api/v1/armazens")
            data = JSON.parse(prepared.fixtures["ArmazemController.listar"]);
        else if (url.pathname === "/api/v1/pedidos-saida") {
            data = JSON.parse(
                prepared.fixtures["PedidoSaidaController.listar"],
            );
            (data as { itens: Values[] }).itens[0].clienteId = 101;
        } else if (url.pathname === "/api/v1/estoque") {
            const stock = JSON.parse(
                prepared.fixtures["EstoqueController.listar"],
            );
            stock.itens[0].unidade.condicao = "AVARIADA";
            stock.itens[0].unidade.clienteId = 101;
            stock.itens[0].unidade.disponivelParaSaida = false;
            stock.itens[0].bloqueada = true;
            stock.itens[0].avariaPosterior = true;
            data = stock;
        } else {
            unknown.push(request.method() + " " + url.pathname);
            return route.fulfill({
                status: 404,
                json: { detail: "Consulta fora do recorte visual" },
            });
        }
        return route.fulfill({
            contentType: "application/json",
            body: stringify(wire(data))!,
        });
    });
    return { calls, writes, errors, unknown };
}

async function badgeContrast(badges: Locator) {
    const palette = await badges.evaluateAll((elements) =>
        elements.slice(0, 3).map((element) => {
            const css = getComputedStyle(element);
            const luminance = (color: string) => {
                const components = color
                    .match(/[\d.]+/g)!
                    .slice(0, 3)
                    .map((value) => {
                        const channel =
                            Number(value) /
                            (color.startsWith("color(srgb") ? 1 : 255);
                        return channel <= 0.04045
                            ? channel / 12.92
                            : ((channel + 0.055) / 1.055) ** 2.4;
                    });
                return (
                    components[0] * 0.2126 +
                    components[1] * 0.7152 +
                    components[2] * 0.0722
                );
            };
            const foreground = luminance(css.color),
                background = luminance(css.backgroundColor);
            return {
                background: css.backgroundColor,
                contrast:
                    (Math.max(foreground, background) + 0.05) /
                    (Math.min(foreground, background) + 0.05),
            };
        }),
    );
    palette.forEach((color) =>
        expect(color.contrast).toBeGreaterThanOrEqual(4.5),
    );
    return palette;
}

for (const [width, theme] of [
    [1440, "light"],
    [1440, "dark"],
    [360, "light"],
    [360, "dark"],
] as const) {
    test(`REG01-A01 tabela, cores, contraste e teclado em ${width}px ${theme}`, async ({
        page,
    }, info) => {
        const mock = await mockApi(page);
        await page.setViewportSize({ width, height: width < 600 ? 800 : 1000 });
        await page.addInitScript(
            (value) => localStorage.setItem("wms.theme", value),
            theme,
        );
        await page.goto("/#cadastros");
        const table = page.locator(".record-table");
        await expect(table.getByRole("row")).toHaveCount(21);
        await expect(table.locator("caption")).toContainText("Clientes");
        const badges = table.locator(".record-status");
        await expect(badges.nth(0)).toHaveAttribute("data-tone", "success");
        await expect(badges.nth(1)).toHaveAttribute("data-tone", "warning");
        await expect(badges.nth(2)).toHaveAttribute("data-tone", "neutral");
        const palette = await badgeContrast(badges);
        expect(new Set(palette.map((color) => color.background)).size).toBe(3);
        const row = table.getByRole("row").nth(1),
            edit = row.getByRole("button", { name: "Editar", exact: true });
        await expect(edit).toHaveClass("primary");
        const view = row.getByRole("button", {
            name: "Ver detalhes",
            exact: true,
        });
        await badgeContrast(row.locator(".record-row-actions button"));
        if (width >= 600) {
            expect(
                await table.evaluate(
                    (element) => element.scrollWidth - element.clientWidth,
                ),
            ).toBeLessThanOrEqual(1);
        }
        expect(
            await edit.evaluate(
                (element) => getComputedStyle(element).backgroundColor,
            ),
        ).not.toBe(
            await view.evaluate(
                (element) => getComputedStyle(element).backgroundColor,
            ),
        );
        if (width < 600)
            expect((await edit.boundingBox())!.height).toBeGreaterThanOrEqual(
                44,
            );
        await page.screenshot({
            path: info.outputPath("clientes.png"),
            fullPage: width >= 600,
        });
        if (width < 600) {
            await table.evaluate((element) =>
                element.scrollIntoView({ block: "start" }),
            );
            await page.screenshot({
                path: info.outputPath("clientes-mobile-tabela.png"),
            });
            await table.evaluate((element) => {
                element.scrollLeft = element.scrollWidth;
            });
            await page.screenshot({
                path: info.outputPath("clientes-mobile-acoes.png"),
            });
            await table.evaluate((element) => {
                element.scrollLeft = 0;
            });
        }
        await view.focus();
        await page.keyboard.press("Enter");
        const dialog = page.getByRole("dialog");
        await expect(dialog).toBeVisible();
        await expect(
            page.getByText("Carregando os dados atuais do registro…"),
        ).toHaveCount(0);
        await page.keyboard.press("Escape");
        await expect(dialog).toHaveCount(0);
        await expect(view).toBeFocused();
        await page
            .getByRole("combobox", {
                name: "Situação nesta página",
                exact: true,
            })
            .selectOption("INATIVO");
        await expect(table.getByRole("row")).toHaveCount(2);
        await expect(
            table.getByRole("button", { name: "Editar", exact: true }),
        ).toHaveCount(0);
        expect(
            await page.evaluate(
                () => document.documentElement.scrollWidth <= innerWidth,
            ),
        ).toBe(true);
        expect(mock.writes).toEqual([]);
        expect(mock.errors).toEqual([]);
        expect(mock.unknown).toEqual([]);
    });
}

test("REG01-A01 Operação conserva as permissões e cores ao filtrar", async ({
    page,
}) => {
    const mock = await mockApi(page, "OPERACAO");
    await page.goto("/#cadastros");
    await expect(page.locator(".record-table .record-status")).toHaveCount(20);
    await expect(
        page
            .locator(".record-table")
            .getByRole("button", { name: "Editar", exact: true }),
    ).toHaveCount(0);
    await page
        .getByRole("combobox", { name: "Situação nesta página", exact: true })
        .selectOption("ENCERRAMENTO_PENDENTE");
    await expect(page.locator(".record-status")).toHaveText([
        "Encerramento pendente",
    ]);
    await expect(page.locator(".record-status")).toHaveAttribute(
        "data-tone",
        "warning",
    );
    expect(mock.writes).toEqual([]);
    expect(mock.errors).toEqual([]);
    expect(mock.unknown).toEqual([]);
});

for (const theme of ["light", "dark"]) {
    test(`REG01-A01 avaria e pedido em andamento mantêm cores distintas em ${theme}`, async ({
        page,
    }, info) => {
        const mock = await mockApi(page);
        await page.setViewportSize({ width: 1440, height: 1000 });
        await page.addInitScript(
            (value) => localStorage.setItem("wms.theme", value),
            theme,
        );
        await page.goto("/#cadastros");
        const context = page.getByRole("form", {
            name: "Contexto operacional",
        });
        await context
            .getByRole("combobox", { name: "Cliente", exact: true })
            .fill("Cliente ativo");
        await page.getByRole("option", { name: /Cliente ativo/ }).click();
        await context
            .getByRole("combobox", { name: "Armazém", exact: true })
            .fill("DEMO-ARMAZEM");
        await page.getByRole("option", { name: /DEMO-ARMAZEM/ }).click();
        await context.getByRole("button", { name: "Aplicar contexto" }).click();
        await page.evaluate(() => {
            location.hash = "estoque";
        });
        const damaged = page.locator(".record-table .record-status");
        await expect(damaged).toHaveText("Avariada");
        await expect(damaged).toHaveAttribute("data-tone", "danger");
        const danger = await badgeContrast(damaged);
        await page.screenshot({ path: info.outputPath("avaria.png") });
        await page.evaluate(() => {
            location.hash = "saida";
        });
        const draft = page.locator(".record-table .record-status");
        await expect(draft).toHaveText("Rascunho");
        await expect(draft).toHaveAttribute("data-tone", "info");
        const infoColors = await badgeContrast(draft);
        expect(danger[0].background).not.toBe(infoColors[0].background);
        await page.screenshot({ path: info.outputPath("saida.png") });
        await page
            .getByRole("button", { name: /Reversão e cancelamento/ })
            .click();
        const cancel = page.locator(".record-action-danger");
        await expect(cancel).toHaveCount(1);
        const cancelPalette = await badgeContrast(cancel);
        const cancelBackground = cancelPalette[0].background;
        expect(cancelBackground).toBe(danger[0].background);
        await cancel.hover();
        expect((await badgeContrast(cancel))[0].background).toBe(
            cancelBackground,
        );
        expect(mock.writes).toEqual([]);
        expect(mock.errors).toEqual([]);
        expect(mock.unknown).toEqual([]);
    });
}
