import { chooseTheme } from "./theme-controls";
import { test, expect } from "@playwright/test";
import { origin } from "./environment";

// UI da aplicação real com respostas interceptadas. Nunca usa sessão/SQL reais.
const user = {
    id: "ds01-fixture",
    nome: "Pessoa de teste com nome comprido para validar a navegação",
    email: "pessoa@test.invalid",
    perfil: "GESTOR",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: 0,
};
for (const width of [360, 1440]) {
    test(`FE02-DS01 login e workspace nativo em ${width}px com API interceptada`, async ({
        page,
    }, info) => {
        let authenticated = false;
        const unknown: string[] = [];
        await page.route("**/*", async (route) => {
            const url = new URL(route.request().url());
            if (url.origin !== origin) return route.abort("blockedbyclient");
            if (!url.pathname.startsWith("/api/")) return route.continue();
            if (url.pathname === "/api/auth/csrf")
                return route.fulfill({
                    json: { token: "csrf-fixture", header: "X-XSRF-TOKEN" },
                });
            if (url.pathname === "/api/auth/renovar")
                return authenticated
                    ? route.fulfill({
                          json: {
                              accessToken: "fixture-local",
                              expiresIn: 300,
                              usuario: user,
                          },
                      })
                    : route.fulfill({
                          status: 401,
                          json: { detail: "Sem sessão fictícia" },
                      });
            if (url.pathname === "/api/auth/entrar") {
                authenticated = true;
                return route.fulfill({
                    json: {
                        accessToken: "fixture-local",
                        expiresIn: 300,
                        usuario: user,
                    },
                });
            }
            if (url.pathname === "/api/auth/usuarios")
                return route.fulfill({
                    json: {
                        content: [],
                        number: 0,
                        totalPages: 0,
                        totalElements: 0,
                    },
                });
            if (url.pathname === "/api/auth/sair") {
                authenticated = false;
                return route.fulfill({ status: 204 });
            }
            unknown.push(url.pathname);
            return route.abort("blockedbyclient");
        });
        await page.setViewportSize({ width, height: 900 });
        await page.goto("/");
        await expect(
            page.getByRole("heading", { name: "Entrar no WMS" }),
        ).toBeVisible();
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            await page.screenshot({
                path: info.outputPath(`login-${width}-${theme}.png`),
                animations: "disabled",
            });
        }
        await page.getByLabel("E-mail", { exact: true }).fill(user.email);
        await page
            .getByLabel("Senha", { exact: true })
            .fill("Somente-Fixture-123!");
        await page.getByRole("button", { name: "Entrar", exact: true }).click();
        await expect(
            page.getByRole("heading", {
                name: `Bem-vindo, ${user.nome}`,
                exact: true,
            }),
        ).toBeVisible();
        await page.getByLabel("Cliente (ID)", { exact: true }).fill("123");
        await page.getByLabel("Armazém (ID)", { exact: true }).fill("456");
        const before = page.url();
        await page
            .getByRole("link", { name: "Ir para o conteúdo", exact: true })
            .focus();
        await page.keyboard.press("Enter");
        await expect(page.locator("#conteudo")).toBeFocused();
        expect(page.url()).toBe(before);
        await expect(
            page.getByLabel("Cliente (ID)", { exact: true }),
        ).toHaveValue("123");
        await page
            .getByRole("button", { name: "Minha senha", exact: true })
            .click();
        await expect(
            page.getByRole("heading", {
                name: "Alterar minha senha",
                exact: true,
            }),
        ).toBeVisible();
        if (width < 1024)
            await page
                .getByRole("button", { name: "Abrir menu", exact: true })
                .click();
        await page
            .getByRole("navigation", { name: "Módulos" })
            .getByRole("button", { name: "Usuários e acessos", exact: true })
            .click();
        await expect(
            page.getByRole("heading", {
                name: "Usuários e acessos",
                exact: true,
            }),
        ).toBeVisible();
        for (const theme of ["dark", "light"] as const) {
            await chooseTheme(page, theme);
            expect(
                await page.evaluate(
                    () =>
                        document.documentElement.scrollWidth <=
                        window.innerWidth,
                ),
            ).toBe(true);
            await page.screenshot({
                path: info.outputPath(`workspace-${width}-${theme}.png`),
                animations: "disabled",
            });
        }
        await page.getByRole("button", { name: "Sair", exact: true }).click();
        await expect(
            page.getByRole("heading", { name: "Entrar no WMS", exact: true }),
        ).toBeVisible();
        expect(unknown).toEqual([]);
    });
}
