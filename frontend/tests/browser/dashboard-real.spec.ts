import { test, expect } from "@playwright/test";
import { origin } from "./environment";
import { fulfillOverviewRead } from "./overview-fixture";
// Aplicação real com respostas interceptadas; nenhuma API ou banco real.
test("Início nativo preserva Long exato, Bearer e valores restritos ao perfil", async ({
    page,
}) => {
    const clienteId = "9007199254740993";
    const calls: URL[] = [],
        chartCalls: URL[] = [],
        unknown: string[] = [];
    let denied = false;
    await page.route("**/*", async (route) => {
        const url = new URL(route.request().url());
        if (url.origin !== origin) return route.abort();
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "ficticio", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar")
            return route.fulfill({
                json: {
                    accessToken: "ficticio",
                    expiresIn: 300,
                    usuario: {
                        id: "1",
                        nome: "Pessoa fictícia",
                        email: "teste@test.invalid",
                        perfil: "OPERACAO",
                        administrador: false,
                        principal: false,
                        ativo: true,
                        trocarSenha: false,
                        clientes: [clienteId],
                        armazens: ["2"],
                        versao: 0,
                    },
                },
            });
        expect(route.request().headers().authorization).toBe("Bearer ficticio");
        if (
            ["/api/v1/visao-operacao", "/api/v1/dashboard"].includes(
                url.pathname,
            )
        ) {
            (url.pathname === "/api/v1/dashboard" ? chartCalls : calls).push(
                url,
            );
            if (denied)
                return route.fulfill({
                    status: 403,
                    json: {
                        detail: "Recusa fictícia",
                        codigo: "ACESSO_NEGADO",
                    },
                });
        }
        if (await fulfillOverviewRead(route, "OPERACAO", clienteId, "2"))
            return;
        unknown.push(url.pathname);
        return route.abort();
    });
    await page.goto("/");
    const overview = page.getByRole("region", {
        name: "Visão geral da operação",
    });
    const charts = page.getByRole("region", { name: "Gráficos da operação" });
    await expect(overview.getByText("0%", { exact: true })).toBeVisible();
    await expect(charts.locator(".dashboard-chart")).toHaveCount(3);
    await expect(charts).toContainText(
        "Todos os clientes permitidos · Todos os armazéns permitidos",
    );
    expect(chartCalls.at(-1)?.searchParams.has("clienteId")).toBe(false);
    expect(chartCalls.at(-1)?.searchParams.has("armazemId")).toBe(false);
    for (const title of ["Cliente", "Armazém"]) {
        await page.getByRole("combobox", { name: title, exact: true }).click();
        await page
            .getByRole("option", {
                name:
                    title === "Cliente"
                        ? /Cliente de teste/
                        : /Armazém de teste/,
            })
            .click();
    }
    await page.getByRole("button", { name: "Aplicar contexto" }).click();
    await expect(overview.getByText("0%", { exact: true })).toBeVisible();
    await expect
        .poll(() => calls.at(-1)?.searchParams.get("clienteId"))
        .toBe(clienteId);
    expect(calls.at(-1)?.searchParams.get("armazemId")).toBe("2");
    await expect(charts.locator(".dashboard-chart")).toHaveCount(3);
    await expect(charts.getByText("DEMO-BOBINA")).toBeVisible();
    expect(chartCalls.at(-1)?.searchParams.get("clienteId")).toBe(clienteId);
    expect(chartCalls.at(-1)?.searchParams.get("armazemId")).toBe("2");
    await expect(charts).toContainText("Cliente de teste · Armazém de teste");
    await expect(charts.locator(".dashboard-metric")).toHaveCount(0);
    await expect(overview.locator(".overview-metric")).toHaveCount(10);
    await charts.getByRole("button", { name: "Próxima", exact: true }).focus();
    await page.keyboard.press("Enter");
    await expect(charts.getByText("DEMO-VOLUME")).toBeVisible();
    await expect(
        charts.getByRole("heading", { name: "Disponibilidade por produto" }),
    ).toBeFocused();
    await charts.getByRole("button", { name: "Anterior", exact: true }).click();
    await expect(charts.getByText("DEMO-BOBINA")).toBeVisible();
    for (const theme of ["light", "dark"]) {
        if (theme === "dark")
            await page.getByRole("button", { name: /tema escuro/i }).click();
        for (const width of [320, 1440]) {
            await page.setViewportSize({ width, height: 900 });
            await charts.scrollIntoViewIfNeeded();
            await expect
                .poll(() =>
                    page.evaluate(
                        () =>
                            document.documentElement.scrollWidth <= innerWidth,
                    ),
                )
                .toBe(true);
            await charts.screenshot({
                path: `evidencias/graficos-inicio-${theme}-${width}.png`,
            });
        }
    }
    await expect(overview.locator(".overview-metric--4 strong")).toHaveText(
        "—",
    );
    await expect(overview.locator(".overview-metric--9 strong")).toHaveText(
        "—",
    );
    for (const title of ["Cliente", "Armazém"]) {
        await page.getByRole("combobox", { name: title, exact: true }).click();
        await page
            .getByRole("option", {
                name:
                    title === "Cliente"
                        ? "Todos os clientes"
                        : "Todos os armazéns",
                exact: true,
            })
            .click();
        await page.getByRole("button", { name: "Aplicar contexto" }).click();
        await expect(charts.locator(".dashboard-chart")).toHaveCount(3);
        await expect
            .poll(() =>
                chartCalls
                    .at(-1)
                    ?.searchParams.has(
                        title === "Cliente" ? "clienteId" : "armazemId",
                    ),
            )
            .toBe(false);
    }
    await expect(charts).toContainText(
        "Todos os clientes permitidos · Todos os armazéns permitidos",
    );
    denied = true;
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(overview.getByRole("alert")).toContainText(
        "Seu acesso não permite",
    );
    await charts.getByRole("button", { name: "Atualizar gráficos" }).click();
    await expect(charts.getByRole("alert")).toContainText(
        "Seu acesso não permite",
    );
    await expect(charts.locator(".dashboard-chart")).toHaveCount(0);
    expect(unknown).toEqual([]);
});
