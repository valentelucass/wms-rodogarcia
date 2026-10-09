import { fulfillOverviewRead } from "./overview-fixture";
import { test, expect, type Page, type Route } from "@playwright/test";
import { origin } from "./environment";
import { chooseTheme } from "./theme-controls";

const user = {
    id: "1",
    nome: "Pessoa de teste",
    email: "pessoa@fixture.invalid",
    perfil: "GESTOR",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: 0,
};

async function intercept(page: Page, password?: (route: Route) => void) {
    const unknown: string[] = [];
    await page.route("**/*", async (route) => {
        const url = new URL(route.request().url());
        if (url.origin !== origin) return route.abort("blockedbyclient");
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (await fulfillOverviewRead(route)) return;
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "csrf-fixture", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar")
            return route.fulfill({
                json: {
                    accessToken: "fixture-local",
                    expiresIn: 300,
                    usuario: user,
                },
            });
        if (
            url.pathname === "/api/auth/usuarios" &&
            route.request().method() === "GET"
        )
            return route.fulfill({
                json: {
                    content: [user],
                    number: 0,
                    totalPages: 1,
                    totalElements: 1,
                },
            });
        if (url.pathname === "/api/auth/senha" && password) {
            password(route);
            return;
        }
        unknown.push(route.request().method() + " " + url.pathname);
        return route.abort("blockedbyclient");
    });
    return unknown;
}

for (const [width, height] of [
    [320, 640],
    [390, 844],
    [768, 720],
    [1440, 900],
    [1920, 1080],
]) {
    test(`diálogos de conta em ${width}px preservam a página e o teclado`, async ({
        page,
    }, info) => {
        await page.setViewportSize({ width, height });
        const unknown = await intercept(page);
        const errors: string[] = [];
        page.on("pageerror", (error) => errors.push(error.message));
        await page.goto("/#coletor");
        const reading = page.getByLabel("1. Leia o UUID da unidade", {
            exact: true,
        });
        await reading.fill("edicao-preservada");
        const opener = page.getByRole("button", {
            name: "Minha senha",
            exact: true,
        });
        const before = page.url();
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            await opener.click();
            const modal = page.getByRole("dialog", {
                name: "Alterar minha senha",
            });
            await expect(modal).toBeVisible();
            await expect(
                modal.getByLabel("Senha atual", { exact: true }),
            ).toBeFocused();
            expect(page.url()).toBe(before);
            expect(await page.locator(".collector").count()).toBe(1);
            for (let i = 0; i < 18; i++) {
                await page.keyboard.press(i < 9 ? "Tab" : "Shift+Tab");
                expect(
                    await modal.evaluate((el) =>
                        el.contains(document.activeElement),
                    ),
                ).toBe(true);
            }
            const bounds = (await modal.boundingBox())!;
            expect(bounds.x).toBeGreaterThanOrEqual(0);
            expect(bounds.y).toBeGreaterThanOrEqual(0);
            expect(bounds.x + bounds.width).toBeLessThanOrEqual(width);
            expect(bounds.y + bounds.height).toBeLessThanOrEqual(height);
            await page.screenshot({
                path: info.outputPath(`senha-${theme}.png`),
            });
            await page.keyboard.press("Escape");
            await expect(modal).toHaveCount(0);
            await expect(opener).toBeFocused();
            await expect(reading).toHaveValue("edicao-preservada");
        }
        await page.goto("/#usuarios");
        const create = page.getByRole("button", {
            name: "Criar usuário",
            exact: true,
        });
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            await create.click();
            const modal = page.getByRole("dialog", { name: "Novo usuário" });
            await expect(
                modal.getByLabel("Nome", { exact: true }),
            ).toBeFocused();
            expect(
                await modal
                    .locator(".dialog-header")
                    .evaluate((el) => getComputedStyle(el).borderTopWidth),
            ).toBe("0px");
            expect(
                await modal.evaluate(
                    (el) => el.scrollWidth <= el.clientWidth + 1,
                ),
            ).toBe(true);
            const bounds = (await modal.boundingBox())!;
            expect(bounds.x + bounds.width).toBeLessThanOrEqual(width);
            expect(bounds.y + bounds.height).toBeLessThanOrEqual(height);
            await page.screenshot({
                path: info.outputPath(`usuario-${theme}.png`),
            });
            await modal
                .getByRole("button", { name: "Cancelar", exact: true })
                .click();
            await expect(create).toBeFocused();
        }
        expect(unknown).toEqual([]);
        expect(errors).toEqual([]);
    });
}

test("troca de senha mantém o modal durante envio e encerra a sessão ao concluir", async ({
    page,
}) => {
    let pending: Route | undefined;
    let writes = 0;
    const unknown = await intercept(page, (route) => {
        writes++;
        pending = route;
    });
    await page.goto("/#coletor");
    await page
        .getByRole("button", { name: "Minha senha", exact: true })
        .click();
    const modal = page.getByRole("dialog", { name: "Alterar minha senha" });
    await modal
        .getByLabel("Senha atual", { exact: true })
        .fill("Fixture-atual-123");
    await modal
        .getByLabel("Nova senha", { exact: true })
        .fill("Fixture-nova-456");
    await modal
        .getByLabel("Repita a nova senha", { exact: true })
        .fill("Fixture-outra-789");
    await modal
        .getByRole("button", { name: "Salvar nova senha", exact: true })
        .click();
    await expect(modal.getByRole("alert")).toContainText("precisam ser iguais");
    expect(writes).toBe(0);
    await modal
        .getByLabel("Repita a nova senha", { exact: true })
        .fill("Fixture-nova-456");
    await modal
        .getByRole("button", { name: "Salvar nova senha", exact: true })
        .click();
    await expect(
        modal.getByRole("button", { name: "Salvando…" }),
    ).toBeDisabled();
    await expect(
        modal.getByRole("button", { name: "Fechar diálogo" }),
    ).toBeDisabled();
    await expect(
        modal.getByRole("button", { name: "Cancelar" }),
    ).toBeDisabled();
    await page.keyboard.press("Escape");
    await expect(modal).toBeVisible();
    await expect.poll(() => !!pending).toBe(true);
    await pending!.fulfill({
        status: 400,
        json: { detail: "Senha atual inválida (fixture)." },
    });
    await expect(modal.getByRole("alert")).toContainText(
        "Senha atual inválida",
    );
    await expect(
        modal.getByRole("button", { name: "Fechar diálogo" }),
    ).toBeEnabled();
    pending = undefined;
    await modal
        .getByRole("button", { name: "Salvar nova senha", exact: true })
        .click();
    await expect.poll(() => !!pending).toBe(true);
    await pending!.fulfill({ status: 204 });
    await expect(modal).toHaveCount(0);
    await expect(
        page.getByRole("button", { name: "Entrar", exact: true }),
    ).toBeVisible();
    await expect(page.getByRole("status")).toContainText("Senha alterada");
    expect(writes).toBe(2);
    expect(unknown).toEqual([]);
});
