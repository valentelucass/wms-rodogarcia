import { test, expect } from "@playwright/test";
import { origin } from "./environment";
import { fulfillOverviewRead } from "./overview-fixture";
// O contrato legado de gráficos conserva testes unitários. O Início agora usa DASH02.
test("Início nativo preserva Long exato, Bearer e valores restritos ao perfil", async ({
    page,
}) => {
    const clienteId = "9007199254740993";
    const calls: URL[] = [],
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
        if (url.pathname === "/api/v1/visao-operacao") {
            calls.push(url);
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
    await expect(overview.getByText("0%", { exact: true })).toBeVisible();
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
    await expect(overview.locator(".overview-metric--4 strong")).toHaveText(
        "—",
    );
    await expect(overview.locator(".overview-metric--9 strong")).toHaveText(
        "—",
    );
    denied = true;
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(overview.getByRole("alert")).toContainText(
        "Seu acesso não permite",
    );
    expect(unknown).toEqual([]);
});
