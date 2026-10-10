import { test, expect, type Page, type TestInfo } from "@playwright/test";
import { readFileSync } from "node:fs";
import { resolve, sep } from "node:path";
import { stringify } from "lossless-json";
import { overviewExample } from "../../src/api/mock/overview";
import { overviewWire, fulfillOverviewRead } from "./overview-fixture";
import type { Request } from "../../src/api/client";

const origin = "https://1z8126n0-25581.brs.devtunnels.ms";
const build = resolve(
    process.env.WMS_INIT01_DIST_DIR ??
        "../orchestracao/.runtime/qual-conf01/perf01/inicio-independencia/lume/confirmed-build-real",
);
const contracts = JSON.parse(
    readFileSync("src/contracts/endpoints.json", "utf8"),
) as Request["endpoint"][];
const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as {
    fixtures: Record<string, string>;
    journeys: { id: string; title: string }[];
};
const contract = (path: string) =>
    contracts.find(
        (e) =>
            e.method === "GET" &&
            new RegExp("^" + e.path.replace(/\{[^}]+\}/g, "[^/]+") + "$").test(
                path,
            ),
    );
type State = "pending" | "error" | "success";
const gate = () => {
    let release!: () => void;
    const promise = new Promise<void>((resolve) => {
        release = resolve;
    });
    return { promise, release };
};

async function offline(
    page: Page,
    options: { catalog?: State; dashboard?: State } = {},
) {
    let catalogState = options.catalog ?? "success",
        dashboardState = options.dashboard ?? "success";
    let catalogGate = gate(),
        dashboardGate = gate(),
        authGate = gate(),
        authPending = false,
        changedRights = false;
    const calls: {
        method: string;
        path: string;
        endpoint?: string;
        catalog: boolean;
        sequence: number;
        context: Record<string, string>;
    }[] = [];
    const responses: { path: string; status: number }[] = [],
        errors: string[] = [],
        unknown: string[] = [];
    const failures: { path: string }[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    page.on("response", (r) =>
        responses.push({ path: new URL(r.url()).pathname, status: r.status() }),
    );
    page.on("requestfailed", (r) =>
        failures.push({ path: new URL(r.url()).pathname }),
    );
    await page.route("**/*", async (route) => {
        const request = route.request(),
            url = new URL(request.url());
        const e = contract(url.pathname);
        const catalog =
            ["/api/v1/clientes", "/api/v1/armazens"].includes(url.pathname) &&
            url.searchParams.get("tamanho") === "100";
        calls.push({
            method: request.method(),
            path: url.pathname,
            endpoint: e?.id,
            catalog,
            sequence: calls.length + 1,
            context: Object.fromEntries(
                [...url.searchParams].filter(([key]) =>
                    ["clienteId", "armazemId", "pagina", "tamanho"].includes(
                        key,
                    ),
                ),
            ),
        });
        if (url.origin !== origin) {
            unknown.push(url.pathname);
            return route.abort();
        }
        if (!url.pathname.startsWith("/api/")) {
            const target = resolve(
                build,
                url.pathname === "/"
                    ? "index.html"
                    : "." + decodeURIComponent(url.pathname),
            );
            if (!target.startsWith(build + sep)) {
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
            try {
                return route.fulfill({
                    body: readFileSync(target),
                    contentType:
                        mime[target.split(".").at(-1)!] ??
                        "application/octet-stream",
                    headers:
                        url.pathname === "/"
                            ? {
                                  "Content-Security-Policy":
                                      "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data: blob:; connect-src 'self'; base-uri 'self'; frame-ancestors 'none'",
                              }
                            : {},
                });
            } catch {
                unknown.push(url.pathname);
                return route.abort();
            }
        }
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "synthetic-only", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar") {
            if (authPending) await authGate.promise;
            return route.fulfill({
                json: {
                    accessToken: "synthetic-memory-only",
                    expiresIn: 300,
                    usuario: {
                        id: "init01-synthetic",
                        nome: "Pessoa fictícia",
                        email: "init@fixture.invalid",
                        perfil: changedRights ? "OPERACAO" : "GESTOR",
                        administrador: !changedRights,
                        principal: false,
                        ativo: true,
                        trocarSenha: false,
                        clientes: changedRights ? ["2"] : [],
                        armazens: changedRights ? ["2"] : [],
                        versao: changedRights ? 1 : 0,
                    },
                },
            });
        }
        if (request.method() !== "GET") {
            unknown.push(request.method() + " " + url.pathname);
            return route.abort();
        }
        if (catalog) {
            if (catalogState === "pending") await catalogGate.promise;
            if (catalogState === "error")
                return route.fulfill({
                    status: 500,
                    json: { codigo: "SYNTHETIC_CATALOG_ERROR" },
                });
            if (
                await fulfillOverviewRead(
                    route,
                    changedRights ? "OPERACAO" : "GESTOR",
                    changedRights ? "2" : "1",
                    changedRights ? "2" : "1",
                )
            )
                return;
        }
        if (
            ["/api/v1/visao-operacao", "/api/v1/dashboard"].includes(
                url.pathname,
            )
        ) {
            if (dashboardState === "pending") await dashboardGate.promise;
            if (dashboardState === "error")
                return route.fulfill({
                    status: 500,
                    json: { codigo: "SYNTHETIC_DASHBOARD_ERROR" },
                });
            if (url.pathname === "/api/v1/visao-operacao")
                return route.fulfill({
                    contentType: "application/json",
                    body: stringify(
                        overviewWire(
                            overviewExample(
                                {
                                    endpoint: e!,
                                    params: {},
                                    query: Object.fromEntries(url.searchParams),
                                    signal: new AbortController().signal,
                                },
                                "GESTOR",
                                false,
                            ),
                        ),
                    )!,
                });
            if (await fulfillOverviewRead(route)) return;
        }
        if (e && prepared.fixtures[e.id])
            return route.fulfill({
                contentType: "application/json",
                body: prepared.fixtures[e.id],
            });
        unknown.push(url.pathname);
        return route.abort();
    });
    return {
        calls,
        errors,
        unknown,
        responses,
        failures,
        setCatalog: (state: State) => {
            if (state === "pending") catalogGate = gate();
            catalogState = state;
            if (state !== "pending") catalogGate.release();
        },
        setDashboard: (state: State) => {
            if (state === "pending") dashboardGate = gate();
            dashboardState = state;
            if (state !== "pending") dashboardGate.release();
        },
        holdAuth: () => {
            authPending = true;
            authGate = gate();
        },
        releaseAuth: () => {
            authPending = false;
            authGate.release();
        },
        changeRights: () => {
            changedRights = true;
        },
        homeCalls: () =>
            calls.filter((c) =>
                ["/api/v1/visao-operacao", "/api/v1/dashboard"].includes(
                    c.path,
                ),
            ),
        proof: async (info: TestInfo, facts: unknown) => {
            await info.attach("independencia", {
                body: JSON.stringify({
                    at: new Date().toISOString(),
                    scope: "REAL build integralmente interceptado, nenhum portal/API/SQL/login real. Sem headers/cookies/tokens/payloads coletados.",
                    calls,
                    responses,
                    failures,
                    facts,
                }),
                contentType: "application/json",
            });
            expect(errors).toEqual([]);
            expect(unknown).toEqual([]);
            expect(
                calls.filter(
                    (c) =>
                        c.method !== "GET" &&
                        !["/api/auth/csrf", "/api/auth/renovar"].includes(
                            c.path,
                        ),
                ),
            ).toEqual([]);
        },
    };
}

async function chooseContext(page: Page) {
    const form = page.getByRole("form", { name: "Contexto operacional" });
    for (const [title, value] of [
        ["Cliente", "Cliente de teste"],
        ["Armazém", "Armazém de teste"],
    ]) {
        const input = form.getByRole("combobox", { name: title, exact: true });
        await expect(input).toBeEnabled();
        await input.fill(value);
        await input.press("Enter");
    }
    await form
        .getByRole("button", { name: "Aplicar contexto", exact: true })
        .click();
}

test("INIT01 primeira prova: entrada direta e reload não visitam Início com catálogo pendente", async ({
    page,
}, info) => {
    const proof = await offline(page, {
        catalog: "pending",
        dashboard: "pending",
    });
    await page.goto("/#entrada");
    await expect(page.locator(".record-workspace")).toBeVisible();
    await expect(
        page.getByRole("heading", {
            name: "Entrada e conferência",
            exact: true,
        }),
    ).toBeVisible();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeDisabled();
    await expect(
        page.getByRole("button", { name: "Sair", exact: true }),
    ).toBeVisible();
    await expect(
        page.locator(".home-header,.warehouse-overview,.operation-dashboard"),
    ).toHaveCount(0);
    expect(proof.homeCalls()).toHaveLength(0);
    // O contrato exige clienteId/armazemId: não confundir contexto obrigatório com bootstrap pelo Início.
    await expect(
        page.getByText(
            "Informe as referências obrigatórias nos filtros para carregar esta visão.",
        ),
    ).toBeVisible();
    expect(
        proof.calls.some(
            (c) => c.endpoint === "PedidoEntradaController.listar",
        ),
    ).toBe(false);
    proof.setCatalog("success");
    await chooseContext(page);
    await expect
        .poll(
            () =>
                proof.calls.filter(
                    (c) => c.endpoint === "PedidoEntradaController.listar",
                ).length,
        )
        .toBe(1);
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    proof.setCatalog("pending");
    await page.reload();
    await expect(page.locator(".record-workspace")).toBeVisible();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeDisabled();
    expect(new URL(page.url()).hash).toBe("#entrada");
    expect(proof.homeCalls()).toHaveLength(0);
    expect(
        proof.calls.filter(
            (c) => c.endpoint === "PedidoEntradaController.listar",
        ),
    ).toHaveLength(1);
    proof.setCatalog("success");
    await chooseContext(page);
    await expect
        .poll(
            () =>
                proof.calls.filter(
                    (c) => c.endpoint === "PedidoEntradaController.listar",
                ).length,
        )
        .toBe(2);
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    expect(
        proof.calls
            .filter((c) => c.endpoint === "PedidoEntradaController.listar")
            .every(
                (c) =>
                    c.context.clienteId === "1" && c.context.armazemId === "1",
            ),
    ).toBe(true);
    expect(proof.homeCalls()).toHaveLength(0);
    await proof.proof(info, {
        route: "#entrada",
        reloads: 1,
        homeReads: 0,
        operationListReads: 2,
        contextChosenTwice: true,
        catalogPendingBlocksRequiredSelection: true,
        reloadDoesNotPersistContext: true,
    });
});

test("INIT01 cadastros consulta sem contexto mesmo com catálogo recusado; retry e reload não precisam de Início", async ({
    page,
}, info) => {
    const proof = await offline(page, {
        catalog: "error",
        dashboard: "pending",
    });
    await page.goto("/#cadastros");
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await expect(
        page.getByRole("form", { name: "Contexto operacional" }),
    ).toContainText("Não foi possível carregar clientes e armazéns");
    expect(
        proof.calls.filter(
            (c) => c.endpoint === "ClienteController.listar" && !c.catalog,
        ),
    ).toHaveLength(1);
    proof.setCatalog("pending");
    await page
        .getByRole("button", { name: "Atualizar opções", exact: true })
        .click();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeDisabled();
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    proof.setCatalog("success");
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeEnabled();
    await page.reload();
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    expect(new URL(page.url()).hash).toBe("#cadastros");
    expect(proof.homeCalls()).toHaveLength(0);
    expect(
        proof.calls.filter(
            (c) => c.endpoint === "ClienteController.listar" && !c.catalog,
        ),
    ).toHaveLength(2);
    await proof.proof(info, {
        directAndReload: "#cadastros",
        homeReads: 0,
        catalogErrorDoesNotBlockList: true,
        retryWhileOperational: true,
    });
});

test("INIT01 navegação rápida abandona dashboard pendente e funciona antes do catálogo acabar", async ({
    page,
}, info) => {
    const proof = await offline(page, {
        catalog: "pending",
        dashboard: "pending",
    });
    await page.goto("/#inicio");
    await expect(page.locator(".home-header")).toBeVisible();
    await expect.poll(() => proof.homeCalls().length).toBe(2);
    await page
        .getByRole("navigation", { name: "Módulos" })
        .getByRole("button", { name: "Cadastros", exact: true })
        .click();
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await page
        .getByRole("navigation", { name: "Módulos" })
        .getByRole("button", { name: "Entrada e conferência", exact: true })
        .click();
    await expect(
        page.getByText(
            "Informe as referências obrigatórias nos filtros para carregar esta visão.",
        ),
    ).toBeVisible();
    expect(proof.calls.filter((c) => c.catalog)).toHaveLength(2);
    proof.setCatalog("success");
    await chooseContext(page);
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    proof.setDashboard("error");
    await expect
        .poll(
            () =>
                proof.failures.filter((c) =>
                    ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(
                        c.path,
                    ),
                ).length,
        )
        .toBe(2);
    await expect(
        page.locator(".home-header,.warehouse-overview,.operation-dashboard"),
    ).toHaveCount(0);
    await expect(
        page.getByRole("heading", {
            name: "Entrada e conferência",
            exact: true,
        }),
    ).toBeVisible();
    expect(proof.calls.filter((c) => c.catalog)).toHaveLength(2);
    expect(proof.homeCalls()).toHaveLength(2);
    await proof.proof(info, {
        homePending: true,
        navigationBeforeCatalogAndHome: true,
        abandonedHomeReads: 2,
        late500DoesNotChangeOperationalPage: true,
        sameCatalogAndTransportAcrossNavigation: true,
    });
});

for (const state of ["error", "success"] as const)
    test(`INIT01 dashboard ${state === "error" ? "500" : "sucesso/Atualizar"} não habilita a operação`, async ({
        page,
    }, info) => {
        const proof = await offline(page, { dashboard: state });
        await page.goto("/#inicio");
        await expect
            .poll(
                () =>
                    proof.responses.filter((c) =>
                        [
                            "/api/v1/dashboard",
                            "/api/v1/visao-operacao",
                        ].includes(c.path),
                    ).length,
            )
            .toBe(2);
        if (state === "success") {
            await expect(
                page.getByRole("button", {
                    name: "Atualizar visão",
                    exact: true,
                }),
            ).toBeEnabled();
            await page
                .getByRole("button", { name: "Atualizar visão", exact: true })
                .click();
            await expect.poll(() => proof.homeCalls().length).toBe(3);
        }
        await page
            .getByRole("navigation", { name: "Módulos" })
            .getByRole("button", { name: "Entrada e conferência", exact: true })
            .click();
        await chooseContext(page);
        await expect(
            page.locator(".record-workspace table tbody tr").first(),
        ).toBeVisible();
        expect(
            proof.calls.filter(
                (c) => c.endpoint === "PedidoEntradaController.listar",
            ),
        ).toHaveLength(1);
        await expect(
            page.locator(
                ".home-header,.warehouse-overview,.operation-dashboard",
            ),
        ).toHaveCount(0);
        await proof.proof(info, {
            homeState: state,
            explicitRefresh: state === "success",
            requiredContextStillChosen: true,
            operationalReadSucceeded: true,
        });
    });

test("INIT01 doze hashes de jornada entram diretamente sem montar Início", async ({
    page,
}, info) => {
    const proof = await offline(page);
    const routes: string[] = [];
    for (const journey of prepared.journeys) {
        await page.goto("/#" + journey.id);
        await expect(
            page.getByRole("heading", { name: journey.title, exact: true }),
        ).toBeVisible();
        await expect(page.locator(".record-workspace")).toBeVisible();
        await expect(
            page.locator(
                ".home-header,.warehouse-overview,.operation-dashboard",
            ),
        ).toHaveCount(0);
        routes.push(journey.id);
    }
    expect(proof.homeCalls()).toHaveLength(0);
    await proof.proof(info, {
        directRoutes: routes,
        homeReads: 0,
        note: "Montagem da primeira visão; filtros obrigatórios continuam necessários conforme cada contrato, não afirma execução de todas as operações.",
    });
});

test("INIT01 fresh concorrente e navegação cancelada preservam sessão e catálogo memoizado", async ({
    page,
}, info) => {
    const initial = new Date("2026-10-10T20:00:00Z");
    await page.clock.setFixedTime(initial);
    const proof = await offline(page);
    await page.goto("/#cadastros");
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await chooseContext(page);
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    proof.holdAuth();
    await page.clock.setFixedTime(new Date(initial.getTime() + 271000));
    const nav = page.getByRole("navigation", { name: "Módulos" });
    await nav
        .getByRole("button", { name: "Entrada e conferência", exact: true })
        .click();
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/auth/renovar")
                    .length,
        )
        .toBe(2);
    await nav.getByRole("button", { name: "Cadastros", exact: true }).click();
    await expect(
        page.getByRole("heading", { name: "Cadastros", exact: true }),
    ).toBeVisible();
    expect(
        proof.calls.filter((c) => c.path === "/api/auth/renovar"),
    ).toHaveLength(2);
    expect(
        proof.calls.filter(
            (c) => c.endpoint === "PedidoEntradaController.listar",
        ),
    ).toHaveLength(0);
    proof.releaseAuth();
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await expect(
        page.getByRole("button", { name: "Sair", exact: true }),
    ).toBeVisible();
    expect(proof.calls.filter((c) => c.catalog)).toHaveLength(2);
    expect(
        proof.calls.filter((c) => c.path === "/api/auth/renovar"),
    ).toHaveLength(2);
    expect(
        proof.calls.filter(
            (c) => c.endpoint === "PedidoEntradaController.listar",
        ),
    ).toHaveLength(0);
    expect(proof.homeCalls()).toHaveLength(0);
    await proof.proof(info, {
        fresh: "duas consultas concorrentes compartilham uma renovação após inicial",
        cancelledBeforeWire: "PedidoEntradaController.listar",
        currentRoute: "#cadastros",
        catalogEnumerations: 2,
        homeReads: 0,
        noSessionLoss: true,
    });
});

test("INIT01 fresh aplica direitos atuais e consulta com novo alcance sem bootstrap por Início", async ({
    page,
}, info) => {
    const initial = new Date("2026-10-10T20:00:00Z");
    await page.clock.setFixedTime(initial);
    const proof = await offline(page);
    await page.goto("/#cadastros");
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toBeEnabled();
    const nav = page.getByRole("navigation", { name: "Módulos" });
    await expect(
        nav.getByRole("button", { name: "Usuários e acessos", exact: true }),
    ).toBeVisible();
    proof.changeRights();
    await page.clock.setFixedTime(new Date(initial.getTime() + 271000));
    await nav.getByRole("button", { name: "Cadastros", exact: true }).click();
    await expect
        .poll(() => proof.calls.filter((c) => c.catalog).length)
        .toBe(4);
    await expect(
        nav.getByRole("button", { name: "Usuários e acessos", exact: true }),
    ).toHaveCount(0);
    await chooseContext(page);
    await nav
        .getByRole("button", { name: "Entrada e conferência", exact: true })
        .click();
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    const reads = proof.calls.filter(
        (c) => c.endpoint === "PedidoEntradaController.listar",
    );
    expect(reads).toHaveLength(1);
    expect(reads[0].context).toMatchObject({ clienteId: "2", armazemId: "2" });
    expect(proof.homeCalls()).toHaveLength(0);
    await proof.proof(info, {
        currentRole: "OPERACAO",
        priorAdminOptionHidden: true,
        catalogsInvalidatedByRights: true,
        currentOperationalContext: "synthetic2/2",
        homeReads: 0,
    });
});

test("INIT01 rascunho em contexto revogado recusa duas confirmações e respeita descarte antes de aplicar alcance", async ({
    page,
}, info) => {
    const initial = new Date("2026-10-10T20:00:00Z");
    await page.clock.setFixedTime(initial);
    let discard = false;
    const leaveQuestions: string[] = [];
    page.on("dialog", async (dialog) => {
        leaveQuestions.push(dialog.message());
        if (discard) await dialog.accept();
        else await dialog.dismiss();
    });
    const proof = await offline(page);
    await page.goto("/#cadastros");
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await chooseContext(page);
    await page
        .locator(".record-workspace table tbody tr")
        .first()
        .getByRole("button", { name: "Editar", exact: true })
        .click();
    const dialog = page.getByRole("dialog");
    await dialog
        .getByLabel("Nome *", { exact: true })
        .fill("Rascunho preservado INIT01");
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Correção apenas sintética");
    proof.holdAuth();
    proof.changeRights();
    await page.clock.setFixedTime(new Date(initial.getTime() + 271000));
    await dialog
        .getByRole("button", { name: "Conferir e confirmar", exact: true })
        .click();
    await dialog
        .getByRole("button", { name: "Confirmar agora", exact: true })
        .click();
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/auth/renovar")
                    .length,
        )
        .toBe(2);
    proof.releaseAuth();
    await expect(dialog.getByText(/O comando não foi enviado/)).toBeVisible();
    await expect(dialog.getByLabel("Nome *", { exact: true })).toHaveValue(
        "Rascunho preservado INIT01",
    );
    await dialog
        .getByRole("button", { name: "Conferir e confirmar", exact: true })
        .click();
    await dialog
        .getByRole("button", { name: "Confirmar agora", exact: true })
        .click();
    await expect(dialog.getByText(/O comando não foi enviado/)).toBeVisible();
    expect(
        proof.calls.filter(
            (c) => c.path.startsWith("/api/v1/") && c.method !== "GET",
        ),
    ).toHaveLength(0);
    expect(
        proof.calls.filter((c) => c.path === "/api/auth/renovar"),
    ).toHaveLength(2);
    await dialog
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    await expect(dialog.getByLabel("Nome *", { exact: true })).toHaveValue(
        "Rascunho preservado INIT01",
    );
    discard = true;
    await dialog
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    await expect(dialog).toHaveCount(0);
    await page
        .getByRole("button", { name: "Aplicar acesso atualizado", exact: true })
        .click();
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toHaveValue("Todos os clientes");
    const nav = page.getByRole("navigation", { name: "Módulos" });
    await expect(
        nav.getByRole("button", { name: "Usuários e acessos", exact: true }),
    ).toHaveCount(0);
    await nav
        .getByRole("button", { name: "Entrada e conferência", exact: true })
        .click();
    await expect(
        page.getByText(
            "Informe as referências obrigatórias nos filtros para carregar esta visão.",
        ),
    ).toBeVisible();
    await expect(
        page.getByRole("combobox", { name: "Cliente *", exact: true }),
    ).toHaveValue("");
    await expect(
        page.getByRole("combobox", { name: "Armazém *", exact: true }),
    ).toHaveValue("");
    await chooseContext(page);
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    expect(
        proof.calls.find((c) => c.endpoint === "PedidoEntradaController.listar")
            ?.context,
    ).toMatchObject({ clienteId: "2", armazemId: "2" });
    expect(proof.homeCalls()).toHaveLength(0);
    expect(leaveQuestions).toEqual([
        "Descartar as alterações não salvas deste registro?",
        "Descartar as alterações não salvas deste registro?",
    ]);
    await proof.proof(info, {
        staleWrites: 0,
        confirmations: 2,
        renewedAuthCallsIncludingInitial: 2,
        draftPreservedWhileGuardRefused: true,
        explicitDiscardAccepted: true,
        previousContextAndWorkflowCleared: true,
        newContext: "synthetic2/2",
        homeReads: 0,
    });
});

test("INIT01 duas leituras antigas aguardando o mesmo fresh são recusadas antes do wire com guarda aceita", async ({
    page,
}, info) => {
    const initial = new Date("2026-10-10T20:00:00Z");
    await page.clock.setFixedTime(initial);
    const proof = await offline(page);
    await page.goto("/#cadastros");
    await expect(
        page.locator(".record-workspace table tbody tr").first(),
    ).toBeVisible();
    await chooseContext(page);
    proof.holdAuth();
    proof.changeRights();
    await page.clock.setFixedTime(new Date(initial.getTime() + 271000));
    await page
        .getByRole("navigation", { name: "Módulos" })
        .getByRole("button", { name: "Início", exact: true })
        .click();
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/auth/renovar")
                    .length,
        )
        .toBe(2);
    await expect(page.locator(".home-header")).toBeVisible();
    expect(proof.homeCalls()).toHaveLength(0);
    proof.releaseAuth();
    await expect.poll(() => proof.homeCalls().length).toBe(2);
    expect(
        proof
            .homeCalls()
            .filter(
                (c) =>
                    c.context.clienteId === "1" || c.context.armazemId === "1",
            ),
    ).toHaveLength(0);
    await expect(
        page.getByRole("combobox", { name: "Cliente", exact: true }),
    ).toHaveValue("Todos os clientes");
    await expect(
        page.getByRole("combobox", { name: "Armazém", exact: true }),
    ).toHaveValue("Todos os armazéns");
    await proof.proof(info, {
        coalescedFresh: true,
        staleReadsBeforeWire: 0,
        newReadsAfterApplyingCurrentAccess: 2,
        previousContextCleared: true,
        cleanupIsNotTheOnlyGuard: true,
    });
});

test("INIT01 visual do pacote confirmado após CSS paralelo em desktop e mobile", async ({
    page,
}, info) => {
    const proof = await offline(page);
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.goto("/#inicio");
    await expect(
        page.getByRole("button", { name: "Atualizar visão", exact: true }),
    ).toBeEnabled();
    await expect(page.locator(".warehouse-overview")).toBeVisible();
    const desktop = await page.locator(".home-header").boundingBox();
    await page.screenshot({
        path: info.outputPath("home-desktop-css-confirmed.png"),
        fullPage: true,
    });
    await page.setViewportSize({ width: 390, height: 844 });
    const mobile = await page.locator(".home-header").boundingBox();
    expect(mobile).not.toBeNull();
    expect(mobile!.x + mobile!.width).toBeLessThanOrEqual(391);
    await page.screenshot({
        path: info.outputPath("home-mobile-css-confirmed.png"),
        fullPage: true,
    });
    expect(proof.homeCalls()).toHaveLength(2);
    await proof.proof(info, {
        visualBuild: build,
        desktop,
        mobile,
        viewportOnlyChange: true,
        extraHomeReadsOnResize: 0,
        syntheticDataOnly: true,
    });
});

test("INIT01 informação dos consumidores atuais preserva teclado, leitura e navegação operacional", async ({
    page,
}, info) => {
    const proof = await offline(page);
    await page.setViewportSize({ width: 1280, height: 800 });
    await page.goto("/#inicio");
    await expect(
        page.getByRole("button", { name: "Atualizar visão", exact: true }),
    ).toBeEnabled();
    const shortcut = page.locator(".home-shortcuts .shortcut-card").first();
    await shortcut.focus();
    const tip = page.getByRole("tooltip");
    await expect(tip).toBeVisible();
    await expect(tip).toContainText("Receber");
    const bounds = await tip.boundingBox();
    expect(bounds).not.toBeNull();
    expect(bounds!.x).toBeGreaterThanOrEqual(0);
    expect(bounds!.x + bounds!.width).toBeLessThanOrEqual(1280);
    await page.screenshot({
        path: info.outputPath("information-current-keyboard.png"),
        fullPage: true,
    });
    await shortcut.press("Escape");
    await expect(tip).toHaveCount(0);
    await expect(shortcut).toBeFocused();
    expect(proof.homeCalls()).toHaveLength(2);
    await shortcut.press("Enter");
    await expect(
        page.getByRole("heading", {
            name: "Entrada e conferência",
            exact: true,
        }),
    ).toBeVisible();
    await expect(page.locator(".home-header")).toHaveCount(0);
    expect(proof.homeCalls()).toHaveLength(2);
    await proof.proof(info, {
        visualBuild: build,
        informationBounds: bounds,
        keyboardPreserved: true,
        operationalNavigation: true,
        extraHomeReadsFromInformation: 0,
    });
});
