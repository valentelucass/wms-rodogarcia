import { fulfillOverviewRead } from "./overview-fixture";
import { test, expect, type Route } from "@playwright/test";
import { origin } from "./environment";
import { chooseTheme } from "./theme-controls";

const current = {
    id: "1",
    nome: "Administrador principal",
    email: "principal@fixture.invalid",
    perfil: "GESTOR",
    administrador: true,
    principal: true,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: 0,
};
const row = {
    ...current,
    id: "2",
    nome: "Pessoa de teste",
    email: "pessoa@fixture.invalid",
    perfil: "OPERACAO",
    administrador: false,
    principal: false,
    clientes: ["1"],
    armazens: ["1"],
};

for (const width of [390, 1024, 1440]) {
    test(`FE02-DS03 header compacto e usuários em ${width}px com API interceptada`, async ({
        page,
    }, info) => {
        await page.setViewportSize({ width, height: 900 });
        let pending: Route | undefined;
        let writes = 0;
        const calls: string[] = [],
            unknown: string[] = [],
            errors: string[] = [];
        page.on("pageerror", (err) => errors.push(err.message));
        await page.route("**/*", async (route) => {
            const url = new URL(route.request().url());
            if (url.origin !== origin) return route.abort("blockedbyclient");
            if (!url.pathname.startsWith("/api/")) return route.continue();
            if (await fulfillOverviewRead(route)) return;
            calls.push(
                route.request().method() + " " + url.pathname + url.search,
            );
            if (url.pathname === "/api/auth/csrf")
                return route.fulfill({
                    json: { token: "csrf-fixture", header: "X-XSRF-TOKEN" },
                });
            if (url.pathname === "/api/auth/renovar")
                return route.fulfill({
                    json: {
                        accessToken: "fixture-local",
                        expiresIn: 300,
                        usuario: current,
                    },
                });
            // A consulta do Início pertence à frente DASH01; esta prova
            // mantém sua falha isolada e não alcança o backend real.
            if (
                url.pathname === "/api/v1/dashboard" &&
                route.request().method() === "GET"
            )
                return route.fulfill({
                    status: 503,
                    json: {
                        detail: "Consulta fictícia indisponível nesta prova de usuários.",
                    },
                });
            if (url.pathname === "/api/auth/usuarios") {
                if (route.request().method() === "GET") {
                    const index = Number(url.searchParams.get("pagina"));
                    return route.fulfill({
                        json: {
                            content:
                                index === 0
                                    ? [row, current]
                                    : [
                                          {
                                              ...row,
                                              id: "3",
                                              nome: "Pessoa da próxima página",
                                          },
                                      ],
                            number: index,
                            totalPages: 2,
                            totalElements: 3,
                        },
                    });
                }
                writes++;
                pending = route;
                return;
            }
            unknown.push(url.pathname);
            return route.abort("blockedbyclient");
        });
        await page.goto("/");
        await expect(
            page.getByRole("heading", {
                name: "Início",
            }),
        ).toBeVisible();
        const header = page.locator("header.topbar");
        await expect(header.getByLabel("Cliente")).toBeVisible();
        await expect(header.getByLabel("Armazém")).toBeVisible();
        await expect(header.getByLabel("Cliente")).toHaveAttribute(
            "placeholder",
            "Cliente",
        );
        await expect(header.getByLabel("Armazém")).toHaveAttribute(
            "placeholder",
            "Armazém",
        );
        await header.getByLabel("Cliente").focus();
        expect(
            await header
                .getByLabel("Cliente")
                .evaluate((el) => getComputedStyle(el, "::placeholder").color),
        ).toBe("rgba(0, 0, 0, 0)");
        await expect(
            header.getByRole("button", { name: "Aplicar contexto" }),
        ).toBeVisible();
        await expect(
            header.getByText("Administrador principal", { exact: true }),
        ).toHaveCount(0);
        await expect(header.getByText("Gestor", { exact: true })).toHaveCount(
            0,
        );
        if (width >= 1400)
            expect((await header.boundingBox())!.height).toBe(64);
        await header
            .getByRole("combobox", { name: "Cliente", exact: true })
            .click();
        await page.getByRole("option", { name: /Cliente de teste/ }).click();
        await header
            .getByRole("combobox", { name: "Armazém", exact: true })
            .click();
        await page.getByRole("option", { name: /Armazém de teste/ }).click();
        await header.getByRole("button", { name: "Aplicar contexto" }).click();
        if (width >= 1400)
            expect((await header.boundingBox())!.height).toBe(64);
        await page.screenshot({
            path: info.outputPath("inicio-contexto.png"),
            fullPage: true,
        });
        await page
            .getByRole("button", { name: "Administrar usuários", exact: true })
            .click();
        await expect(
            page.getByRole("cell", { name: /^Pessoa de teste pessoa@/ }),
        ).toBeVisible();
        const pagination = page.getByRole("navigation", {
            name: "Paginação dos resultados",
        });
        await expect(pagination).toContainText("Página 1 de 2");
        await pagination.getByRole("button", { name: "Próxima" }).click();
        await expect(
            page.getByRole("cell", {
                name: /^Pessoa da próxima página pessoa@/,
            }),
        ).toBeVisible();
        await expect(
            pagination.getByRole("button", { name: "Próxima" }),
        ).toBeDisabled();
        await pagination.getByRole("button", { name: "Anterior" }).click();
        await expect(
            page.getByRole("cell", { name: /^Pessoa de teste pessoa@/ }),
        ).toBeVisible();
        const create = page.getByRole("button", {
            name: "Criar usuário",
            exact: true,
        });
        await create.click();
        let dialog = page.getByRole("dialog", {
            name: "Novo usuário",
            exact: true,
        });
        await expect(dialog).toBeVisible();
        await page.keyboard.press("Escape");
        await expect(dialog).toHaveCount(0);
        await expect(create).toBeFocused();
        const edit = page.getByRole("button", { name: "Editar", exact: true });
        await edit.click();
        dialog = page.getByRole("dialog", {
            name: "Editar Pessoa de teste",
            exact: true,
        });
        await expect(dialog.getByLabel("Nome", { exact: true })).toHaveValue(
            "Pessoa de teste",
        );
        for (const theme of ["light", "dark"] as const) {
            await page.keyboard.press("Escape");
            await chooseTheme(page, theme);
            await edit.click();
            await page.screenshot({
                path: info.outputPath(`edicao-${theme}.png`),
            });
            expect(
                await page.evaluate(
                    () =>
                        document.documentElement.scrollWidth <=
                        document.documentElement.clientWidth + 1,
                ),
            ).toBe(true);
            for (let i = 0; i < 12; i++) {
                await page.keyboard.press("Tab");
                expect(
                    await dialog.evaluate((el) =>
                        el.contains(document.activeElement),
                    ),
                ).toBe(true);
            }
        }
        await dialog.getByRole("button", { name: "Cancelar" }).click();
        await create.click();
        dialog = page.getByRole("dialog", {
            name: "Novo usuário",
            exact: true,
        });
        await dialog.getByLabel("Nome", { exact: true }).fill("Pessoa nova");
        await dialog
            .getByLabel("E-mail", { exact: true })
            .fill("nova@fixture.invalid");
        await dialog.getByLabel(/^Senha temporária/).fill("Ficticia-local-123");
        await dialog
            .getByRole("button", { name: "Salvar usuário", exact: true })
            .click();
        await expect(
            dialog.getByRole("button", { name: "Salvando…" }),
        ).toBeDisabled();
        await expect(
            dialog.getByRole("button", { name: "Fechar diálogo" }),
        ).toBeDisabled();
        await page.keyboard.press("Escape");
        await expect(dialog).toBeVisible();
        await expect.poll(() => !!pending).toBe(true);
        await pending!.fulfill({ status: 201, json: null });
        await expect(dialog.getByRole("alert")).toContainText("incerto");
        await page.keyboard.press("Escape");
        await expect(dialog).toBeVisible();
        await expect(
            dialog.getByRole("button", { name: "Salvar usuário" }),
        ).toBeDisabled();
        await dialog
            .getByRole("button", { name: "Consultar novamente" })
            .click();
        await expect(dialog).toHaveCount(0);
        expect(writes).toBe(1);
        expect(calls).toContain("GET /api/auth/usuarios?pagina=1");
        expect(unknown).toEqual([]);
        expect(errors).toEqual([]);
    });
}
