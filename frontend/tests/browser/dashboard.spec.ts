import { test, expect } from "./support";
import { chooseTheme } from "./theme-controls";
test.beforeEach(async ({ page }) => {
    await page.goto("/");
    await expect(page.getByText("3%", { exact: true })).toBeVisible();
});
test("visão geral fictícia nos dois temas e mudança de armazém", async ({
    page,
}) => {
    const overview = page.getByRole("region", {
        name: "Visão geral da operação",
    });
    await expect(overview.locator(".overview-metric")).toHaveCount(10);
    const charts = page.getByRole("region", { name: "Gráficos da operação" });
    await expect(charts.locator(".dashboard-chart")).toHaveCount(3);
    await expect(charts.getByText("DEMO-BOBINA")).toBeVisible();
    await expect(charts.locator(".dashboard-metric")).toHaveCount(0);
    await expect(overview.locator(".map-position")).toHaveCount(80);
    await chooseTheme(page, "light");
    await chooseTheme(page, "dark");
    const picker = overview.getByRole("combobox", {
        name: "Visualizar armazém",
    });
    await picker.click();
    await picker.fill("anexo");
    await picker.press("Enter");
    await expect(overview.locator(".map-position")).toHaveCount(24);
    await expect(overview.getByText("0%", { exact: true })).toBeVisible();
    await expect(charts.locator(".dashboard-chart")).toHaveCount(3);
    await expect(charts).toContainText("Anexo fictício");
    await page
        .getByLabel("Perfil de apresentação fictício")
        .selectOption("OPERACAO");
    await expect(overview.locator(".overview-metric--4 strong")).toHaveText(
        "—",
    );
});
test("mapa fictício permite navegação local no celular sem exceder a página", async ({
    page,
}) => {
    const overview = page.getByRole("region", {
        name: "Visão geral da operação",
    });
    for (const width of [320, 375, 768, 1024]) {
        await page.setViewportSize({ width, height: 900 });
        await expect
            .poll(() =>
                page.evaluate(
                    () => document.documentElement.scrollWidth <= innerWidth,
                ),
            )
            .toBe(true);
        await overview.scrollIntoViewIfNeeded();
        await expect(overview.locator(".map-position")).toHaveCount(80);
    }
});
test("atualização lenta, vazio e erro conservam os dez cards sem zeros inventados", async ({
    page,
}) => {
    const overview = page.getByRole("region", {
        name: "Visão geral da operação",
    });
    await page
        .getByLabel("Resposta do exercício fictício")
        .selectOption("lento");
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(overview.getByRole("status")).toContainText("Consultando");
    await expect(overview.getByText("3%", { exact: true })).toBeVisible();
    await expect(
        overview.getByRole("button", { name: "Atualizar visão" }),
    ).toBeEnabled();
    await page
        .getByLabel("Resposta do exercício fictício")
        .selectOption("vazio");
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(
        overview.getByText("Nenhum endereço cadastrado neste contexto."),
    ).toBeVisible();
    await expect(overview.getByText("0%", { exact: true })).toBeVisible();
    await page
        .getByLabel("Resposta do exercício fictício")
        .selectOption("falha");
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(overview.getByRole("alert")).toContainText("Não foi possível");
    await expect(overview.locator(".overview-metric")).toHaveCount(10);
    await expect(overview.locator(".overview-metric--0 strong")).toHaveText(
        "—",
    );
});
