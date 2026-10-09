import { chooseTheme } from "./theme-controls";
import { test, expect, type Route } from "@playwright/test";
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
for (const [width, height] of [
    [320, 640],
    [360, 740],
    [768, 720],
    [1024, 720],
    [1440, 720],
]) {
    test(`FE02-DS02 login e workspace nativo em ${width}px com API interceptada`, async ({
        page,
    }, info) => {
        let authenticated = false;
        let attempts = 0;
        let pendingLogin: Route | undefined;
        const unknown: string[] = [];
        const consoleErrors: string[] = [];
        page.on("pageerror", (error) => consoleErrors.push(error.message));
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
                attempts++;
                if (attempts === 1)
                    return route.fulfill({
                        status: 401,
                        json: { detail: "E-mail ou senha inválidos." },
                    });
                if (attempts === 2)
                    return route.fulfill({
                        status: 503,
                        json: { detail: "Falha fictícia do serviço" },
                    });
                if (attempts === 3) return route.abort("failed");
                if (attempts === 4)
                    return route.fulfill({
                        json: {
                            accessToken: "fixture-invalid",
                            expiresIn: 300,
                            usuario: null,
                        },
                    });
                pendingLogin = route;
                return;
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
        await page.setViewportSize({ width, height });
        await page.goto("/");
        await expect(
            page.getByRole("heading", { name: "Entrar no WMS" }),
        ).toBeVisible();
        const footer = page.getByRole("contentinfo");
        await expect(
            footer.getByRole("link", { name: /Lucas Andrade/ }),
        ).toHaveAttribute(
            "href",
            "https://www.linkedin.com/in/dev-lucasandrade/",
        );
        await expect(
            footer.getByRole("link", { name: "lucasmac.dev@gmail.com" }),
        ).toHaveAttribute("href", "mailto:lucasmac.dev@gmail.com");
        await expect(page.getByLabel("E-mail", { exact: true })).toBeFocused();
        const password = page.getByLabel("Senha", { exact: true });
        const form = page.locator(".auth-login-card form");
        const initialForm = await form.boundingBox();
        const expectStableLogin = async () => {
            expect(
                await page.evaluate(
                    () =>
                        document.documentElement.scrollHeight <=
                        window.innerHeight,
                ),
            ).toBe(true);
            expect(await page.evaluate(() => window.scrollY)).toBe(0);
            expect((await form.boundingBox())?.y).toBeCloseTo(
                initialForm!.y,
                0,
            );
        };
        await expectStableLogin();
        await password.fill("Somente-Fixture-123!");
        await page
            .getByRole("button", { name: "Mostrar senha", exact: true })
            .focus();
        await page.keyboard.press("Enter");
        await expect(password).toHaveAttribute("type", "text");
        await expect(password).toHaveValue("Somente-Fixture-123!");
        await page
            .getByRole("button", { name: "Ocultar senha", exact: true })
            .click();
        await expect(password).toHaveAttribute("type", "password");
        await page
            .getByRole("button", { name: "Esqueceu sua senha?", exact: true })
            .click();
        await expect(
            page.getByRole("dialog", {
                name: "Esqueceu sua senha?",
                exact: true,
            }),
        ).toBeVisible();
        await expect(
            page.getByText(/Peça a um administrador para definir/),
        ).toBeVisible();
        await expectStableLogin();
        await page.keyboard.press("Escape");
        await expect(
            page.getByRole("button", {
                name: "Esqueceu sua senha?",
                exact: true,
            }),
        ).toBeFocused();
        await page
            .getByRole("button", {
                name: "É seu primeiro acesso?",
                exact: true,
            })
            .click();
        await expect(
            page.getByText(/Use o e-mail cadastrado e a senha temporária/),
        ).toBeVisible();
        await page
            .getByRole("button", { name: "Entendi", exact: true })
            .click();
        await expectStableLogin();
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            for (const name of [
                "Esqueceu sua senha?",
                "É seu primeiro acesso?",
            ]) {
                const help = page.getByRole("button", { name, exact: true });
                await help.hover();
                await expect(help).toHaveCSS(
                    "background-color",
                    "rgba(0, 0, 0, 0)",
                );
                await expect(help).toHaveCSS(
                    "text-decoration-line",
                    "underline",
                );
                await help.focus();
                await page.keyboard.press("Tab");
                await page.keyboard.press("Shift+Tab");
                await expect(help).toBeFocused();
                await expect(help).toHaveCSS("outline-style", "none");
                await expect(help).toHaveCSS(
                    "text-decoration-thickness",
                    "2px",
                );
            }
            await expect(password).toHaveValue("Somente-Fixture-123!");
            expect(
                await page.evaluate(
                    () =>
                        document.documentElement.scrollWidth <=
                        window.innerWidth,
                ),
            ).toBe(true);
            await page.screenshot({
                path: info.outputPath(`login-${width}-${theme}.png`),
                animations: "disabled",
                fullPage: true,
            });
        }
        await page.getByLabel("E-mail", { exact: true }).fill(user.email);
        await page
            .getByLabel("Senha", { exact: true })
            .fill("Somente-Fixture-123!");
        await page.getByRole("button", { name: "Entrar", exact: true }).click();
        await expect(page.getByRole("alert")).toContainText(
            "E-mail ou senha inválidos.",
        );
        await expect(password).toHaveValue("");
        await expect(page.getByLabel("E-mail", { exact: true })).toHaveValue(
            user.email,
        );
        await expect(password).toHaveAttribute("type", "password");
        await expectStableLogin();
        for (const text of [
            "Não foi possível entrar no momento.",
            "Não foi possível conectar ao WMS.",
            "Não foi possível confirmar seu acesso.",
        ]) {
            await password.fill("Somente-Fixture-123!");
            await page
                .getByRole("button", { name: "Entrar", exact: true })
                .click();
            await expect(page.getByRole("alert")).toContainText(text);
            await expect(password).toHaveValue("");
            await expectStableLogin();
        }
        await password.fill("Somente-Fixture-123!");
        await page.getByRole("button", { name: "Entrar", exact: true }).click();
        await expect(
            page.getByRole("button", { name: "Entrando…", exact: true }),
        ).toBeDisabled();
        await expect(password).toBeDisabled();
        await expect.poll(() => Boolean(pendingLogin)).toBe(true);
        authenticated = true;
        await pendingLogin!.fulfill({
            json: {
                accessToken: "fixture-local",
                expiresIn: 300,
                usuario: user,
            },
        });
        await expect(
            page.getByRole("heading", {
                name: `Bem-vindo, ${user.nome}`,
                exact: true,
            }),
        ).toBeVisible();
        await expect(footer).toBeVisible();
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
        expect(attempts).toBe(5);
        expect(consoleErrors).toEqual([]);
    });
}

for (const width of [360, 1440]) {
    test(`FE02-DS02 verificação de sessão e senha temporária em ${width}px`, async ({
        page,
    }, info) => {
        let pendingRefresh: Route | undefined;
        const unknown: string[] = [];
        await page.route("**/*", async (route) => {
            const url = new URL(route.request().url());
            if (url.origin !== origin) return route.abort("blockedbyclient");
            if (!url.pathname.startsWith("/api/")) return route.continue();
            if (url.pathname === "/api/auth/csrf")
                return route.fulfill({
                    json: { token: "csrf-fixture", header: "X-XSRF-TOKEN" },
                });
            if (url.pathname === "/api/auth/renovar") {
                pendingRefresh = route;
                return;
            }
            if (url.pathname === "/api/auth/sair")
                return route.fulfill({ status: 204 });
            unknown.push(url.pathname);
            return route.abort("blockedbyclient");
        });
        await page.setViewportSize({ width, height: 900 });
        await page.goto("/");
        await expect(page.getByRole("status")).toHaveText(
            "Verificando seu acesso…",
        );
        await expect(page.getByRole("contentinfo")).toBeVisible();
        await chooseTheme(page, "dark");
        await expect.poll(() => Boolean(pendingRefresh)).toBe(true);
        await pendingRefresh!.fulfill({
            json: {
                accessToken: "fixture-local",
                expiresIn: 300,
                usuario: { ...user, trocarSenha: true },
            },
        });
        await expect(
            page.getByRole("heading", { name: "Crie sua nova senha" }),
        ).toBeVisible();
        await expect(
            page.getByLabel("Senha atual", { exact: true }),
        ).toBeFocused();
        await expect(
            page.getByLabel("Nova senha", { exact: true }),
        ).toHaveAttribute("minlength", "12");
        await expect(
            page.getByRole("button", {
                name: "Salvar nova senha",
                exact: true,
            }),
        ).toBeVisible();
        await expect(
            page.getByRole("navigation", { name: "Módulos" }),
        ).toHaveCount(0);
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
                path: info.outputPath(`senha-temporaria-${width}-${theme}.png`),
                fullPage: true,
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
