import { test, expect } from "@playwright/test";
import { origin } from "./environment";
import { overviewExample } from "../../src/api/mock/overview";
import type { Values } from "../../src/contracts/runtime";
import type { Request } from "../../src/api/client";
import { fulfillOverviewRead } from "./overview-fixture";
const numeric = new Set([
    "id",
    "versao",
    "clienteId",
    "armazemId",
    "capacidade",
    "posicoesOcupadas",
    "posicoesLivres",
    "ocupacao",
    "unidadesArmazenadas",
    "valorArmazenado",
    "emQuarentena",
    "reservasAtivas",
    "entradasAbertas",
    "saidasAbertas",
    "faturamentoMes",
    "totalItens",
    "capacidadePesoKg",
    "unidadesVisiveis",
    "quantidade",
    "pedidoEntradaId",
]);
const wire = (v: unknown, key = ""): unknown =>
    Array.isArray(v)
        ? v.map((v) => wire(v))
        : v && typeof v === "object"
          ? Object.fromEntries(
                Object.entries(v).map(([k, v]) => [k, wire(v, k)]),
            )
          : numeric.has(key) && typeof v === "string"
            ? Number(v)
            : v;
const endpoint = (id: string) => ({ id }) as Request["endpoint"];

// Aplicação real, HTTP interceptado e dados exclusivamente fictícios.
test("três correções: nomes, mapa completo, detalhes e destino sem escrita automática", async ({
    page,
}) => {
    const calls: URL[] = [],
        writes: string[] = [],
        unknown: string[] = [];
    let failure = false,
        empty = false;
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
                        perfil: "GESTOR",
                        administrador: true,
                        principal: true,
                        ativo: true,
                        trocarSenha: false,
                        clientes: [],
                        armazens: [],
                        versao: 0,
                    },
                },
            });
        if (route.request().method() !== "GET") writes.push(url.pathname);
        expect(route.request().headers().authorization).toBe("Bearer ficticio");
        const catalog =
            url.pathname === "/api/v1/clientes"
                ? "Cliente"
                : url.pathname === "/api/v1/armazens"
                  ? "Armazem"
                  : null;
        if (catalog) {
            const example = {
                versao: 0,
                documentoFiscal: "00000000000000",
                criadoEm: "2026-10-09T12:00:00Z",
                alteradoEm: "2026-10-09T12:00:00Z",
                cidade: "Cidade fictícia",
                uf: "SP",
            } as Values;
            return route.fulfill({
                contentType: "application/json",
                body: JSON.stringify(
                    wire({
                        itens: [1, 2].map((id) => ({
                            ...example,
                            id: String(id),
                            nome:
                                catalog === "Cliente"
                                    ? `Cliente ${id === 1 ? "Álfa" : "Beta"}`
                                    : id === 1
                                      ? "Centro fictício"
                                      : "Anexo fictício",
                            codigo: `DEMO-${id}`,
                            situacao: "ATIVO",
                        })),
                        pagina: 0,
                        tamanho: 100,
                        totalItens: "2",
                        totalPaginas: 1,
                    }),
                ),
            });
        }
        if (url.pathname.startsWith("/api/v1/visao-operacao")) {
            calls.push(url);
            if (failure)
                return route.fulfill({
                    status: 503,
                    json: { detail: "Falha de prova isolada", codigo: "FALHA" },
                });
            const detail = url.pathname.includes("/posicoes/");
            const e = endpoint(
                detail
                    ? "VisaoOperacaoController.detalhe"
                    : "VisaoOperacaoController.consultar",
            );
            const query = Object.fromEntries(url.searchParams);
            const data = overviewExample(
                {
                    endpoint: e,
                    params: { id: url.pathname.split("/").at(-1) },
                    query,
                    signal: new AbortController().signal,
                },
                "GESTOR",
                empty,
            );
            return route.fulfill({
                contentType: "application/json",
                body: JSON.stringify(wire(data)),
            });
        }
        if (
            url.pathname === "/api/v1/dashboard" &&
            (await fulfillOverviewRead(route))
        )
            return;
        unknown.push(url.pathname);
        return route.abort();
    });
    await page.goto("/");
    await expect(
        page.getByRole("heading", { name: "Início", exact: true }),
    ).toBeVisible();
    const overview = page.getByRole("region", {
        name: "Visão geral da operação",
    });
    await expect(overview.locator(".overview-metric")).toHaveCount(10);
    await expect(overview.locator(".map-position")).toHaveCount(100);
    await expect(
        page.getByText("104 registros", { exact: false }),
    ).toBeVisible();
    await overview
        .locator(".warehouse-map")
        .getByRole("button", { name: "Próxima", exact: true })
        .click();
    await expect(overview.locator(".map-position")).toHaveCount(4);
    const client = page.getByRole("combobox", { name: "Cliente", exact: true });
    await client.click();
    await client.fill("alfa");
    await client.press("Enter");
    await expect(client).toHaveValue("Cliente Álfa");
    const warehouse = page.getByRole("combobox", {
        name: "Armazém",
        exact: true,
    });
    await warehouse.click();
    await warehouse.fill("centro");
    await warehouse.press("Enter");
    await page.getByRole("button", { name: "Aplicar contexto" }).click();
    await expect(overview.getByText("3%", { exact: true })).toBeVisible();
    await expect(overview.locator(".map-position")).toHaveCount(80);
    expect(calls.at(-1)?.searchParams.get("clienteId")).toBe("1");
    expect(calls.at(-1)?.searchParams.get("armazemId")).toBe("1");
    await overview
        .getByRole("button", { name: "Filtros", exact: true })
        .click();
    await page.getByLabel("Mostrar posições").selectOption("OCUPADO");
    await expect(overview.locator(".map-position")).toHaveCount(2);
    await expect(overview.getByText("3%", { exact: true })).toBeVisible();
    const position = overview.locator(".map-position").first();
    await position.click();
    const detail = page.getByRole("dialog");
    await expect(
        detail.getByText("Mercadoria fictícia", { exact: true }),
    ).toBeVisible();
    await expect(
        detail.getByRole("button", { name: "Endereçar", exact: true }),
    ).toBeDisabled();
    await page.keyboard.press("Escape");
    await expect(detail).not.toBeVisible();
    await expect(position).toBeFocused();
    await page.getByLabel("Mostrar posições").selectOption("DISPONIVEL");
    await expect(overview.locator(".map-position")).toHaveCount(78);
    await overview.locator(".map-position").first().click();
    await expect(
        detail.getByRole("button", { name: "Endereçar", exact: true }),
    ).toBeEnabled();
    const destination = await detail
        .getByRole("heading", { level: 2 })
        .textContent();
    await detail
        .getByRole("button", { name: "Endereçar", exact: true })
        .click();
    await expect(
        page.getByRole("heading", { name: "Coletor", exact: true }),
    ).toBeVisible();
    await expect(page.getByText(/Destino selecionado:/)).toContainText(
        destination!,
    );
    expect(writes).toEqual([]);
    await page
        .getByRole("button", { name: "Início", exact: true })
        .first()
        .click();
    await expect(overview.getByText("3%", { exact: true })).toBeVisible();
    for (const width of [320, 390, 768, 1440, 1920]) {
        await page.setViewportSize({ width, height: 1080 });
        await expect(overview.locator(".overview-metric")).toHaveCount(10);
        await expect
            .poll(() =>
                page.evaluate(
                    () =>
                        document.documentElement.scrollWidth <=
                        window.innerWidth,
                ),
            )
            .toBe(true);
        await page.screenshot({
            path: `evidencias/visao-operacao-${width}-browser-light.png`,
            fullPage: true,
        });
    }
    await page.getByRole("button", { name: /tema escuro/i }).click();
    await page.screenshot({
        path: "evidencias/visao-operacao-1920-browser-dark.png",
        fullPage: true,
    });
    failure = true;
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(overview.getByRole("alert")).toContainText("Não foi possível");
    await expect(overview.locator(".overview-metric--0 strong")).toHaveText(
        "—",
    );
    failure = false;
    empty = true;
    await overview.getByRole("button", { name: "Atualizar visão" }).click();
    await expect(overview.getByText("0%", { exact: true })).toBeVisible();
    await expect(
        overview.getByText("Nenhum endereço cadastrado neste contexto."),
    ).toBeVisible();
    expect(unknown).toEqual([]);
});
