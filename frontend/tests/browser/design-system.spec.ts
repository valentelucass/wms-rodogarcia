import { chooseTheme } from "./theme-controls";
import { test, expect } from "./support";

test("FE02-DS01-A03 inicia pelo sistema e preserva a escolha manual entre acessos e abas", async ({
    page,
    context,
}) => {
    await page.emulateMedia({ colorScheme: "dark" });
    await page.goto("/");
    const root = page.locator("html");
    await expect(root).toHaveAttribute("data-og-theme", "dark");
    await expect(root).toHaveAttribute("data-wms-theme-preference", "system");
    await expect(
        page
            .getByRole("group", { name: "Tema", exact: true })
            .getByRole("button"),
    ).toHaveCount(1);
    await expect(
        page.getByRole("button", { name: "Usar tema do sistema", exact: true }),
    ).toHaveCount(0);
    await page.emulateMedia({ colorScheme: "light" });
    await expect(root).toHaveAttribute("data-og-theme", "light");
    await expect(
        page.getByRole("button", { name: "Ativar tema escuro", exact: true }),
    ).toBeVisible();
    await page.emulateMedia({ colorScheme: "dark" });
    await expect(root).toHaveAttribute("data-og-theme", "dark");
    const circular = page.getByRole("button", {
        name: "Ativar tema claro",
        exact: true,
    });
    await expect(circular).toHaveCSS("border-radius", "50%");
    await circular.focus();
    await page.keyboard.press("Enter");
    await expect(root).toHaveAttribute("data-og-theme", "light");
    await expect(root).toHaveAttribute("data-wms-theme-preference", "light");
    expect(await page.evaluate(() => localStorage.getItem("wms.theme"))).toBe(
        "light",
    );
    await page.emulateMedia({ colorScheme: "light" });
    await page.emulateMedia({ colorScheme: "dark" });
    await expect(root).toHaveAttribute("data-og-theme", "light");
    await page.reload();
    await expect(root).toHaveAttribute("data-og-theme", "light");
    await expect(root).toHaveAttribute("data-wms-theme-preference", "light");
    const second = await context.newPage();
    await second.emulateMedia({ colorScheme: "light" });
    await second.goto("/");
    await expect(second.locator("html")).toHaveAttribute(
        "data-wms-theme-preference",
        "light",
    );
    await chooseTheme(second, "dark");
    await expect(root).toHaveAttribute("data-og-theme", "dark");
    await page.reload();
    await expect(root).toHaveAttribute("data-og-theme", "dark");
    await expect(root).toHaveAttribute("data-wms-theme-preference", "dark");
    await page.emulateMedia({ colorScheme: "light" });
    await expect(root).toHaveAttribute("data-og-theme", "dark");
    await page.emulateMedia({ colorScheme: "dark" });
    await expect(root).toHaveAttribute("data-og-theme", "dark");
    await second.close();
});

for (const [preference, system] of [
    ["light", "dark"],
    ["dark", "light"],
] as const) {
    test(`FE02-DS01-A03 escolha ${preference} salva prevalece sobre sistema ${system}`, async ({
        page,
    }) => {
        await page.addInitScript(
            (value) => localStorage.setItem("wms.theme", value),
            preference,
        );
        await page.emulateMedia({ colorScheme: system });
        await page.goto("/");
        await expect(page.locator("html")).toHaveAttribute(
            "data-og-theme",
            preference,
        );
        await expect(page.locator("html")).toHaveAttribute(
            "data-wms-theme-preference",
            preference,
        );
        await page.emulateMedia({ colorScheme: preference });
        await page.emulateMedia({ colorScheme: system });
        await expect(page.locator("html")).toHaveAttribute(
            "data-og-theme",
            preference,
        );
        await page.reload();
        await expect(page.locator("html")).toHaveAttribute(
            "data-og-theme",
            preference,
        );
    });
}

test("FE02-DS01-A03 preferência inválida usa o sistema sem apagar outros dados", async ({
    page,
}) => {
    await page.addInitScript(() => {
        localStorage.setItem("wms.theme", "invalid");
        localStorage.setItem("wms.example", "preservar");
    });
    await page.emulateMedia({ colorScheme: "dark" });
    await page.goto("/");
    await expect(page.locator("html")).toHaveAttribute("data-og-theme", "dark");
    await expect(page.locator("html")).toHaveAttribute(
        "data-wms-theme-preference",
        "system",
    );
    await chooseTheme(page, "light");
    expect(await page.evaluate(() => localStorage.getItem("wms.theme"))).toBe(
        "light",
    );
    expect(await page.evaluate(() => localStorage.getItem("wms.example"))).toBe(
        "preservar",
    );
});

test("FE02-DS01 menu desktop preserva nomes, seleção e formulário ao trocar tema", async ({
    page,
}) => {
    await page.setViewportSize({ width: 1440, height: 900 });
    await page.goto("/#entrada");
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await page.getByLabel("Referência *", { exact: true }).fill("DS01-EDICAO");
    await chooseTheme(page, "dark");
    await expect(page.getByLabel("Referência *", { exact: true })).toHaveValue(
        "DS01-EDICAO",
    );
    await page
        .getByRole("button", { name: "Minimizar menu", exact: true })
        .click();
    await expect(page.locator(".shell-sidebar")).toHaveCSS("width", "64px");
    expect((await page.locator(".shell-sidebar").boundingBox())?.x).toBe(0);
    await expect(
        page.locator(".main-nav .nav-label").first(),
    ).not.toBeVisible();
    await expect(page.locator(".shell-workspace")).toHaveCSS(
        "margin-left",
        "64px",
    );
    const item = page
        .getByRole("navigation", { name: "Módulos" })
        .getByRole("button", { name: "Entrada e conferência", exact: true });
    await expect(item).toHaveAttribute("aria-current", "page");
    await expect(item).toBeVisible();
    await page.screenshot({
        path: "evidencias/design-system-etapa01-a02-sidebar-minimizado.png",
        animations: "disabled",
    });
    await page
        .getByRole("button", { name: "Expandir menu", exact: true })
        .click();
    await expect(page.locator(".shell-sidebar")).toHaveCSS("width", "256px");
    await expect(page.locator(".main-nav .nav-label").first()).toBeVisible();
    await expect(page.getByLabel("Referência *", { exact: true })).toHaveValue(
        "DS01-EDICAO",
    );
});

test("FE02-DS01 drawer móvel fecha com Escape, contém foco e navega para conteúdo", async ({
    page,
}) => {
    await page.setViewportSize({ width: 360, height: 800 });
    await page.goto("/");
    const trigger = page.getByRole("button", {
        name: "Abrir menu",
        exact: true,
    });
    const menu = page.getByRole("dialog", { name: "Menu de navegação" });
    await trigger.click();
    await expect(menu).toBeVisible();
    for (let i = 0; i < 18; i++) {
        await page.keyboard.press("Tab");
        expect(
            await menu.evaluate((el) => el.contains(document.activeElement)),
        ).toBe(true);
    }
    await page.keyboard.press("Escape");
    await expect(menu).not.toBeVisible();
    await expect(trigger).toBeFocused();
    await trigger.click();
    await menu
        .getByRole("button", { name: "Entrada e conferência", exact: true })
        .click();
    await expect(menu).not.toBeVisible();
    await expect(page.locator("#conteudo")).toBeFocused();
    await expect(
        page.getByRole("heading", {
            name: "Entrada e conferência",
            exact: true,
        }),
    ).toBeVisible();
    await trigger.click();
    await expect(
        menu.getByRole("button", {
            name: "Entrada e conferência",
            exact: true,
        }),
    ).toHaveAttribute("aria-current", "page");
});

test("FE02-DS01-A03 tema automático e botão funcionam sem armazenamento", async ({
    page,
}) => {
    await page.addInitScript(() => {
        Storage.prototype.getItem = () => {
            throw new Error("Armazenamento indisponível");
        };
        Storage.prototype.setItem = () => {
            throw new Error("Armazenamento indisponível");
        };
    });
    await page.emulateMedia({ colorScheme: "dark" });
    await page.goto("/");
    await expect(page.locator("html")).toHaveAttribute("data-og-theme", "dark");
    await chooseTheme(page, "light");
    await expect(page.locator("html")).toHaveAttribute(
        "data-og-theme",
        "light",
    );
});

for (const width of [320, 360, 390, 768, 1024, 1280, 1440]) {
    test(`FE02-DS01 claro e escuro sem overflow em ${width}px`, async ({
        page,
    }, info) => {
        await page.setViewportSize({ width, height: 900 });
        const errors: string[] = [];
        page.on("pageerror", (error) => errors.push(error.message));
        page.on("console", (message) => {
            if (message.type() === "error") errors.push(message.text());
        });
        await page.goto("/#cadastros");
        await expect(
            page.getByRole("heading", { name: "Cadastros", exact: true }),
        ).toBeVisible();
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            await page.evaluate(() => document.fonts.ready);
            expect(
                await page.evaluate(
                    () =>
                        document.documentElement.scrollWidth <=
                        window.innerWidth,
                ),
            ).toBe(true);
            expect(
                await page.evaluate(() => document.fonts.check('14px "Inter"')),
            ).toBe(true);
            await page.screenshot({
                path: info.outputPath(`${width}-${theme}.png`),
                animations: "disabled",
            });
            if (width < 1024) {
                await page
                    .getByRole("button", { name: "Abrir menu", exact: true })
                    .click();
                await page.screenshot({
                    path: info.outputPath(`${width}-${theme}-menu.png`),
                    animations: "disabled",
                });
                await page.keyboard.press("Escape");
            }
        }
        expect(errors).toEqual([]);
    });
}
