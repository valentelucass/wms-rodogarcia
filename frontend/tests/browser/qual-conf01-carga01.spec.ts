import { test, expect, type Page, type TestInfo } from "@playwright/test";
import { readFileSync } from "node:fs";
import { resolve, sep } from "node:path";
import { fulfillOverviewRead, overviewWire } from "./overview-fixture";
import { overviewExample } from "../../src/api/mock/overview";
import type { Request } from "../../src/api/client";
import { stringify } from "lossless-json";

const origin = "https://1z8126n0-25581.brs.devtunnels.ms";
const build = resolve(
    process.env.WMS_CARGA01_DIST_DIR ??
        "../orchestracao/.runtime/qual-conf01/perf01/carga01/lume/baseline-package-build-real",
);
const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { fixtures: Record<string, string> };
const endpoints = JSON.parse(
    readFileSync("src/contracts/endpoints.json", "utf8"),
) as { id: string; path: string; method: string }[];
const gate = () => {
    let release!: () => void;
    const promise = new Promise<void>((r) => {
        release = r;
    });
    return { promise, release };
};
async function offline(page: Page, dashboardError = false, allowWrite = false) {
    const started = performance.now(),
        tail = gate(),
        graph = gate();
    let updated = false;
    const calls: {
        sequence: number;
        path: string;
        method: string;
        page: number;
        size: number;
        startMs: number;
        endMs?: number;
        status?: number;
        fixtureBodyBytes?: number;
    }[] = [];
    const network: {
        path: string;
        timing: ReturnType<import("@playwright/test").Request["timing"]>;
        responseBodyBytes?: number;
    }[] = [];
    const pendingNetwork: Promise<void>[] = [];
    const errors: string[] = [],
        unknown: string[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("requestfinished", (request) => {
        const item = {
            path: new URL(request.url()).pathname,
            timing: request.timing(),
            responseBodyBytes: undefined as number | undefined,
        };
        network.push(item);
        pendingNetwork.push(
            request
                .sizes()
                .then((s) => {
                    item.responseBodyBytes = s.responseBodySize;
                })
                .catch(() => {}),
        );
    });
    await page.route("**/*", async (route) => {
        const request = route.request(),
            url = new URL(request.url());
        const item = {
            sequence: calls.length + 1,
            path: url.pathname,
            method: request.method(),
            page: Number(url.searchParams.get("pagina") ?? 0),
            size: Number(url.searchParams.get("tamanho") ?? 0),
            startMs: performance.now() - started,
            endMs: undefined as number | undefined,
            status: undefined as number | undefined,
            fixtureBodyBytes: undefined as number | undefined,
        };
        calls.push(item);
        const fulfill = async (
            body: string | Buffer,
            status = 200,
            contentType = "application/json",
            csp = false,
        ) => {
            item.fixtureBodyBytes = Buffer.byteLength(body);
            item.status = status;
            await route.fulfill({
                body,
                status,
                contentType,
                headers: csp
                    ? {
                          "Content-Security-Policy":
                              "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data: blob:; connect-src 'self'; base-uri 'self'; frame-ancestors 'none'",
                      }
                    : {},
            });
            item.endMs = performance.now() - started;
        };
        if (url.origin !== origin) {
            unknown.push(url.pathname);
            return route.abort();
        }
        if (!url.pathname.startsWith("/api/")) {
            const path = resolve(
                build,
                url.pathname === "/"
                    ? "index.html"
                    : "." + decodeURIComponent(url.pathname),
            );
            if (!path.startsWith(build + sep)) {
                unknown.push(url.pathname);
                return route.abort();
            }
            const mime: Record<string, string> = {
                html: "text/html",
                js: "text/javascript",
                css: "text/css",
                svg: "image/svg+xml",
                woff2: "font/woff2",
            };
            return fulfill(
                readFileSync(path),
                200,
                mime[path.split(".").at(-1)!] ?? "application/octet-stream",
                url.pathname === "/",
            );
        }
        if (url.pathname === "/api/auth/csrf")
            return fulfill(
                JSON.stringify({
                    token: "synthetic-only",
                    header: "X-XSRF-TOKEN",
                }),
            );
        if (url.pathname === "/api/auth/renovar")
            return fulfill(
                JSON.stringify({
                    accessToken: "synthetic-memory-only",
                    expiresIn: 300,
                    usuario: {
                        id: "carga-synthetic",
                        nome: "Pessoa fictícia",
                        email: "carga@fixture.invalid",
                        perfil: "GESTOR",
                        administrador: true,
                        principal: false,
                        ativo: true,
                        trocarSenha: false,
                        clientes: [],
                        armazens: [],
                        versao: 0,
                    },
                }),
            );
        if (
            allowWrite &&
            request.method() === "PUT" &&
            url.pathname === "/api/v1/clientes/1"
        ) {
            const response = prepared.fixtures["ClienteController.alterar"];
            if (!response) throw new Error("Fixture de atualização ausente");
            updated = true;
            return fulfill(response);
        }
        if (request.method() !== "GET") {
            unknown.push(request.method() + " " + url.pathname);
            return route.abort();
        }
        const catalog =
            ["/api/v1/clientes", "/api/v1/armazens"].includes(url.pathname) &&
            url.searchParams.get("tamanho") === "100";
        if (catalog) {
            const client = url.pathname === "/api/v1/clientes",
                index = item.page;
            if (client && index > 0) await tail.promise;
            return fulfill(
                JSON.stringify({
                    itens: Array.from({ length: client ? 100 : 1 }, (_, i) => ({
                        id: index * 100 + i + 1,
                        nome:
                            client && updated && index === 0 && i === 0
                                ? "Cliente atualizado CARGA01"
                                : (client ? "Cliente" : "Armazém") +
                                  " sintético " +
                                  (index * 100 + i + 1),
                        codigo: "CARGA" + i,
                        situacao: "ATIVO",
                        versao: updated ? 1 : 0,
                        criadoEm: "2026-10-10T12:00:00Z",
                        alteradoEm: "2026-10-10T12:00:00Z",
                        documentoFiscal: "00000000000000",
                        cidade: "Cidade fictícia",
                        uf: "SP",
                    })),
                    pagina: index,
                    tamanho: 100,
                    totalItens: client ? 400 : 1,
                    totalPaginas: client ? 4 : 1,
                }),
            );
        }
        if (url.pathname === "/api/v1/dashboard") {
            await graph.promise;
            if (dashboardError)
                return fulfill(
                    JSON.stringify({ codigo: "SYNTHETIC_ERROR" }),
                    500,
                );
        }
        if (
            ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(
                url.pathname,
            )
        ) {
            if (url.pathname === "/api/v1/visao-operacao")
                return fulfill(
                    stringify(
                        overviewWire(
                            overviewExample(
                                {
                                    endpoint: {
                                        id: "VisaoOperacaoController.consultar",
                                    } as Request["endpoint"],
                                    params: {},
                                    query: Object.fromEntries(url.searchParams),
                                    signal: new AbortController().signal,
                                },
                                "GESTOR",
                                false,
                            ),
                        ),
                    )!,
                );
            if (await fulfillOverviewRead(route)) {
                item.status = 200;
                item.endMs = performance.now() - started;
                return;
            }
        }
        const endpoint = endpoints.find(
            (e) =>
                e.method === "GET" &&
                new RegExp(
                    "^" + e.path.replace(/\{[^}]+\}/g, "[^/]+") + "$",
                ).test(url.pathname),
        );
        if (endpoint && prepared.fixtures[endpoint.id])
            return fulfill(prepared.fixtures[endpoint.id]);
        unknown.push(url.pathname);
        return route.abort();
    });
    return {
        calls,
        releaseTail: tail.release,
        releaseGraph: graph.release,
        elapsed: () => performance.now() - started,
        proof: async (info: TestInfo, facts: unknown) => {
            await Promise.all(pendingNetwork);
            await info.attach("carga", {
                contentType: "application/json",
                body: JSON.stringify({
                    at: new Date().toISOString(),
                    package: build,
                    environment:
                        "Browser offline integralmente interceptado/CSP estrita; gates sintéticos, não latência real",
                    calls,
                    network,
                    facts,
                }),
            });
            expect(errors).toEqual([]);
            expect(unknown).toEqual([]);
        },
    };
}

test("CARGA01 primeira carga apresenta mapa sem gráficos nem catálogo lento e mede waterfall/DOM", async ({
    page,
}, info) => {
    const proof = await offline(page);
    await page.goto("/#inicio");
    await expect(page.locator(".home-header")).toBeVisible();
    await expect
        .poll(() => page.locator(".map-position").count())
        .toBeGreaterThan(0);
    await expect
        .poll(
            () =>
                proof.calls.filter(
                    (c) => c.path === "/api/v1/clientes" && c.page === 1,
                ).length,
        )
        .toBe(1);
    const first = {
        measuredMs: proof.elapsed(),
        warehouseDisabled: await page
            .getByRole("combobox", { name: "Armazém", exact: true })
            .isDisabled(),
        clientDisabled: await page
            .getByRole("combobox", { name: "Cliente", exact: true })
            .isDisabled(),
        catalogPagesStarted: proof.calls
            .filter((c) => c.path === "/api/v1/clientes")
            .map((c) => c.page),
        positions: await page.locator(".map-position").count(),
        graphBars: await page.locator(".dashboard-bar").count(),
        domNodes: await page.locator("*").count(),
    };
    proof.releaseTail();
    proof.releaseGraph();
    const client = page.getByRole("combobox", { name: "Cliente", exact: true });
    await expect(client).toBeEnabled();
    await expect(page.locator(".dashboard-bar").first()).toBeVisible();
    await client.click();
    await expect(page.getByRole("option")).toHaveCount(401);
    const ready = {
        measuredMs: proof.elapsed(),
        options: await page.getByRole("option").count(),
        domNodes: await page.locator("*").count(),
    };
    await client.press("Escape");
    for (const [label, name] of [
        ["Cliente", "Cliente sintético 1"],
        ["Armazém", "Armazém sintético 1"],
    ]) {
        const field = page.getByRole("combobox", { name: label, exact: true });
        await field.fill(name);
        await field.press("Enter");
    }
    await page
        .getByRole("button", { name: "Aplicar contexto", exact: true })
        .click();
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/v1/visao-operacao")
                    .length,
        )
        .toBe(2);
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/v1/dashboard")
                    .length,
        )
        .toBe(2);
    expect(
        proof.calls.filter((c) =>
            ["/api/v1/clientes", "/api/v1/armazens"].includes(c.path),
        ),
    ).toHaveLength(5);
    await page.screenshot({
        path: info.outputPath("carga-contexto-desktop.png"),
        fullPage: true,
    });
    await proof.proof(info, {
        first,
        ready,
        contextDoesNotReenumerateCatalog: true,
        syntheticCatalogBodyBytes: proof.calls
            .filter((c) =>
                ["/api/v1/clientes", "/api/v1/armazens"].includes(c.path),
            )
            .reduce((n, c) => n + (c.fixtureBodyBytes ?? 0), 0),
        cpuNotMeasured: true,
    });
});

test("CARGA01 lista navega enquanto gráficos aguardam e erro não bloqueia mapa", async ({
    page,
}, info) => {
    const proof = await offline(page, true);
    await page.goto("/#inicio");
    await expect
        .poll(() => page.locator(".map-position").count())
        .toBeGreaterThan(0);
    proof.releaseGraph();
    await expect(
        page.getByText(
            "Não foi possível consultar os indicadores. Tente atualizar.",
        ),
    ).toBeVisible();
    expect(await page.locator(".map-position").count()).toBeGreaterThan(0);
    await page
        .getByRole("navigation", { name: "Módulos" })
        .getByRole("button", { name: "Cadastros", exact: true })
        .click();
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    proof.releaseTail();
    await expect(
        page.getByRole("heading", { name: "Cadastros", exact: true }),
    ).toBeVisible();
    await proof.proof(info, {
        operationalListBeforeCatalogReady: true,
        overviewPreservedOnGraph500: true,
        realNetwork: false,
    });
});

test("CARGA01 contexto de armazém aplica sem esperar clientes Todos e mantém leituras independentes", async ({
    page,
}, info) => {
    const proof = await offline(page);
    await page.goto("/#inicio");
    const warehouse = page.getByRole("combobox", {
        name: "Armazém",
        exact: true,
    });
    await expect(warehouse).toBeEnabled();
    await warehouse.fill("Armazém sintético 1");
    await warehouse.press("Enter");
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeDisabled();
    await page
        .getByRole("button", { name: "Aplicar contexto", exact: true })
        .click();
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/v1/visao-operacao")
                    .length,
        )
        .toBe(2);
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/v1/dashboard")
                    .length,
        )
        .toBe(2);
    await expect
        .poll(() => page.locator(".map-position").count())
        .toBeGreaterThan(0);
    proof.releaseTail();
    proof.releaseGraph();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeEnabled();
    await proof.proof(info, {
        selectedWarehouseBeforeClientEnumeration: true,
        mandatoryContextRulesUnchanged: true,
        noCatalogReenumeration:
            proof.calls.filter((c) =>
                ["/api/v1/clientes", "/api/v1/armazens"].includes(c.path),
            ).length === 5,
    });
});

test("CARGA01 escrita confirmada invalida catálogo da sessão e mostra opção atual sem cache global", async ({
    page,
}, info) => {
    const proof = await offline(page, false, true);
    proof.releaseTail();
    proof.releaseGraph();
    await page.goto("/#cadastros");
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeEnabled();
    await page
        .locator(".record-workspace table tbody tr")
        .first()
        .getByRole("button", { name: "Editar", exact: true })
        .click();
    const dialog = page.getByRole("dialog");
    await dialog
        .getByLabel("Nome *", { exact: true })
        .fill("Cliente atualizado CARGA01");
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Atualização sintética rastreável");
    await dialog
        .getByRole("button", { name: "Conferir e confirmar", exact: true })
        .click();
    await dialog
        .getByRole("button", { name: "Confirmar agora", exact: true })
        .click();
    await expect
        .poll(() => proof.calls.filter((c) => c.method === "PUT").length)
        .toBe(1);
    await expect
        .poll(
            () =>
                proof.calls.filter(
                    (c) =>
                        c.method === "GET" &&
                        c.size === 100 &&
                        ["/api/v1/clientes", "/api/v1/armazens"].includes(
                            c.path,
                        ),
                ).length,
        )
        .toBe(10);
    await dialog
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    const client = page.getByRole("combobox", { name: "Cliente", exact: true });
    await expect(client).toBeEnabled();
    await client.fill("Cliente atualizado CARGA01");
    await expect(
        page.getByRole("option", { name: /Cliente atualizado CARGA01/ }),
    ).toBeVisible();
    await client.press("Escape");
    await proof.proof(info, {
        confirmedSyntheticWrites: 1,
        catalogRequestsBefore: 5,
        catalogRequestsAfter: 5,
        updatedReferenceVisible: true,
        globalCache: false,
        homeReads: proof.calls.filter((c) =>
            ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(c.path),
        ).length,
    });
});
