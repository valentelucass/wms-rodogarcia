import { test, expect, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";
import { resolve, sep } from "node:path";
import { stringify } from "lossless-json";
import { overviewExample } from "../../src/api/mock/overview";
import { overviewWire, fulfillOverviewRead } from "./overview-fixture";
import { chooseTheme } from "./theme-controls";
import type { Perfil } from "../../src/contracts/runtime";
import type { Request } from "../../src/api/client";

const origin = "https://1z8126n0-25581.brs.devtunnels.ms";
const build = resolve(
    "../orchestracao/.runtime/qual-conf01/lume/final-build-real",
);
const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { fixtures: Record<string, string>; journeys: { id: string }[] };
const contracts = JSON.parse(
    readFileSync("src/contracts/endpoints.json", "utf8"),
) as Request["endpoint"][];
const endpoint = (id: string) => contracts.find((item) => item.id === id)!;

async function offline(page: Page, perfil: Perfil = "GESTOR", loggedIn = true) {
    const calls: { path: string; method: string; bearer?: string }[] = [];
    const unknown: string[] = [],
        errors: string[] = [];
    let renewals = 0,
        csrfInvalid = false,
        denied = false;
    let holdChart: (() => Promise<void>) | undefined;
    let loginGate: (() => Promise<void>) | undefined;
    page.on("pageerror", (error) => errors.push(error.message));
    await page.route("**/*", async (route) => {
        const request = route.request(),
            url = new URL(request.url());
        calls.push({
            path: url.pathname,
            method: request.method(),
            bearer: request.headers().authorization,
        });
        if (url.origin !== origin) {
            unknown.push(url.href);
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
            const ext = target.split(".").at(-1)!;
            const mime = (
                {
                    html: "text/html",
                    js: "text/javascript",
                    css: "text/css",
                    svg: "image/svg+xml",
                    woff2: "font/woff2",
                } as Record<string, string>
            )[ext];
            try {
                return route.fulfill({
                    body: readFileSync(target),
                    contentType: mime ?? "application/octet-stream",
                    headers:
                        ext === "html"
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
                json: csrfInvalid
                    ? { token: "ficticio", header: "Authorization" }
                    : { token: "csrf-ficticio", header: "X-XSRF-TOKEN" },
            });
        if (
            url.pathname === "/api/auth/renovar" ||
            url.pathname === "/api/auth/entrar"
        ) {
            if (url.pathname.endsWith("renovar") && !loggedIn)
                return route.fulfill({ status: 401, json: {} });
            if (url.pathname.endsWith("entrar") && loginGate) await loginGate();
            renewals++;
            return route.fulfill({
                json: {
                    accessToken: `qual-ficticio-${renewals}`,
                    expiresIn: 300,
                    usuario: {
                        id: "qual-ficticio",
                        nome: "Pessoa fictícia",
                        email: "qual@fixture.invalid",
                        perfil,
                        administrador: perfil === "GESTOR",
                        principal: false,
                        ativo: true,
                        trocarSenha: false,
                        clientes: [],
                        armazens: [],
                        versao: 0,
                    },
                },
            });
        }
        if (url.pathname === "/api/auth/sair")
            return route.fulfill({ status: 204 });
        if (url.pathname === "/api/v1/dashboard" && holdChart) {
            await holdChart();
            return route.fulfill({
                status: 401,
                json: { codigo: "TOKEN_EXPIRADO" },
            });
        }
        if (
            denied &&
            ["/api/v1/visao-operacao", "/api/v1/dashboard"].includes(
                url.pathname,
            )
        )
            return route.fulfill({
                status: 403,
                json: {
                    detail: "Recusa fictícia de contexto",
                    codigo: "ACESSO_NEGADO",
                },
            });
        if (
            url.pathname === "/api/v1/visao-operacao" ||
            url.pathname.startsWith("/api/v1/visao-operacao/posicoes/")
        ) {
            const detail = url.pathname.includes("/posicoes/");
            const data = overviewExample(
                {
                    endpoint: endpoint(
                        detail
                            ? "VisaoOperacaoController.detalhe"
                            : "VisaoOperacaoController.consultar",
                    ),
                    params: detail
                        ? { id: url.pathname.split("/").at(-1)! }
                        : {},
                    query: Object.fromEntries(url.searchParams),
                    signal: new AbortController().signal,
                },
                perfil,
                false,
            );
            return route.fulfill({
                contentType: "application/json",
                body: stringify(overviewWire(data))!,
            });
        }
        if (await fulfillOverviewRead(route, perfil)) return;
        if (request.method() === "GET") {
            const contract = contracts.find(
                (e) =>
                    e.method === "GET" &&
                    new RegExp(
                        "^" + e.path.replace(/\{[^}]+\}/g, "[^/]+") + "$",
                    ).test(url.pathname),
            );
            if (contract && prepared.fixtures[contract.id])
                return route.fulfill({
                    contentType: "application/json",
                    body: prepared.fixtures[contract.id],
                });
        }
        unknown.push(request.method() + " " + url.pathname);
        return route.abort();
    });
    return {
        calls,
        unknown,
        errors,
        deny: () => {
            denied = true;
        },
        invalidCsrf: (value: boolean) => {
            csrfInvalid = value;
        },
        holdChart: (value: () => Promise<void>) => {
            holdChart = value;
        },
        holdLogin: (value: () => Promise<void>) => {
            loginGate = value;
        },
    };
}

for (const width of [1440, 768, 360]) {
    test(`build atual offline: ${width}px, temas, mapa e teclado/modal`, async ({
        page,
    }, info) => {
        await page.setViewportSize({ width, height: 900 });
        const proof = await offline(page);
        await page.goto("/");
        await expect(page.locator(".map-position").first()).toBeVisible();
        const measures = [];
        for (const theme of ["light", "dark"] as const) {
            await chooseTheme(page, theme);
            const sizes = await page.evaluate(() => ({
                width: innerWidth,
                scrollWidth: document.documentElement.scrollWidth,
                shortcuts: [
                    ...document.querySelectorAll(
                        ".home-shortcuts .shortcut-card",
                    ),
                ].map((e) => ({
                    height: e.getBoundingClientRect().height,
                    header: !!e.closest(".home-header"),
                    hiddenDescription:
                        e.querySelector("strong+span")?.className,
                })),
                map: [...document.querySelectorAll(".map-position")]
                    .slice(0, 3)
                    .map((e) => ({
                        height: e.getBoundingClientRect().height,
                        width: e.getBoundingClientRect().width,
                    })),
            }));
            expect(sizes.scrollWidth).toBeLessThanOrEqual(width);
            expect(sizes.shortcuts).toHaveLength(4);
            sizes.shortcuts.forEach((s) => {
                expect(s.height).toBeGreaterThanOrEqual(44);
                expect(s.header).toBe(true);
                expect(s.hiddenDescription).toBe("sr-only");
            });
            if (width === 1440)
                sizes.shortcuts.forEach((s) => expect(s.height).toBe(44));
            sizes.map.forEach((s) => {
                expect(s.width).toBe(64);
                // Etapa05/A06: até700px ou ponteiro de toque; este viewport usa mouse.
                expect(s.height).toBe(width <= 700 ? 44 : 28);
            });
            measures.push({ theme, ...sizes });
            await page.screenshot({
                path: info.outputPath(`home-${theme}.png`),
            });
        }
        const position = page.locator(".map-position").first();
        await position.focus();
        await page.keyboard.press("Enter");
        const detail = page.getByRole("dialog");
        await expect(detail).toBeVisible();
        await expect(detail.getByText("Carregando endereço…")).toHaveCount(0);
        await page.keyboard.press("Escape");
        await expect(detail).toHaveCount(0);
        await expect(position).toBeFocused();
        const collector = page.locator(".shortcut-card--coletor");
        await collector.focus();
        await page.keyboard.press("Enter");
        const reading = page.getByLabel("1. Leia o UUID da unidade", {
            exact: true,
        });
        await reading.fill("edicao-ficticia-preservada");
        const password = page.getByRole("button", {
            name: "Minha senha",
            exact: true,
        });
        await password.click();
        const modal = page.getByRole("dialog", { name: "Alterar minha senha" });
        await expect(
            modal.getByLabel("Senha atual", { exact: true }),
        ).toBeFocused();
        for (let i = 0; i < 10; i++) {
            await page.keyboard.press(i < 5 ? "Tab" : "Shift+Tab");
            expect(
                await modal.evaluate((e) => e.contains(document.activeElement)),
            ).toBe(true);
        }
        await page.screenshot({ path: info.outputPath("modal.png") });
        await page.keyboard.press("Escape");
        await expect(reading).toHaveValue("edicao-ficticia-preservada");
        await expect(password).toBeFocused();
        if (width === 1440) {
            await reading.fill("");
            for (const journey of prepared.journeys) {
                await page.evaluate((id) => {
                    location.hash = id;
                }, journey.id);
                await expect(page.locator(".record-workspace")).toBeVisible();
                await expect(
                    page.getByText(
                        /Carregando registros|Atualizando os registros apresentados/,
                    ),
                ).toHaveCount(0);
            }
        }
        expect(
            proof.calls.filter(
                (c) => c.method !== "GET" && !c.path.startsWith("/api/auth/"),
            ),
        ).toEqual([]);
        expect(proof.unknown).toEqual([]);
        expect(proof.errors).toEqual([]);
        await info.attach("medidas-e-chamadas", {
            body: JSON.stringify({ measures, calls: proof.calls }),
            contentType: "application/json",
        });
    });
}

test("perfil Operação: financeiro/admin ausentes, 403 preserva área autenticada", async ({
    page,
}, info) => {
    const proof = await offline(page, "OPERACAO");
    await page.goto("/");
    await expect(page.locator(".overview-metric")).toHaveCount(10);
    await expect(
        page.getByRole("button", { name: "Administrar usuários" }),
    ).toHaveCount(0);
    expect(await page.locator(".overview-metrics").innerText()).not.toContain(
        "R$",
    );
    proof.deny();
    await page
        .getByRole("button", { name: "Atualizar visão", exact: true })
        .click();
    await expect(
        page.getByRole("alert").filter({ hasText: /acesso|contexto/i }),
    ).toBeVisible();
    await expect(
        page.getByRole("button", { name: "Sair", exact: true }),
    ).toBeVisible();
    expect(proof.calls.filter((c) => c.path === "/api/auth/sair")).toEqual([]);
    expect(proof.unknown).toEqual([]);
    expect(proof.errors).toEqual([]);
    await info.attach("chamadas-interceptadas", {
        body: JSON.stringify(proof.calls),
        contentType: "application/json",
    });
});

test("401 atrasado após renovação não faz logout na aplicação construída", async ({
    page,
}, info) => {
    await page.clock.install();
    const proof = await offline(page);
    let release!: () => void;
    const delayed = new Promise<void>((done) => {
        release = done;
    });
    proof.holdChart(() => delayed);
    await page.goto("/");
    await expect(page.locator(".map-position").first()).toBeVisible();
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/v1/dashboard")
                    .length,
        )
        .toBe(1);
    await page.clock.fastForward(240001);
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/auth/renovar")
                    .length,
        )
        .toBe(2);
    // Confirma que a segunda resposta foi consumida antes de liberar o401 antigo.
    await page
        .getByRole("button", { name: "Atualizar visão", exact: true })
        .click();
    await expect
        .poll(
            () =>
                proof.calls
                    .filter((c) => c.path === "/api/v1/visao-operacao")
                    .at(-1)?.bearer,
        )
        .toBe("Bearer qual-ficticio-2");
    release();
    await expect(
        page
            .getByRole("region", { name: "Gráficos da operação" })
            .getByRole("alert"),
    ).toBeVisible();
    await expect(
        page.getByRole("button", { name: "Sair", exact: true }),
    ).toBeVisible();
    expect(proof.calls.filter((c) => c.path === "/api/auth/sair")).toEqual([]);
    expect(proof.unknown).toEqual([]);
    expect(proof.errors).toEqual([]);
    await info.attach("chamadas-interceptadas", {
        body: JSON.stringify(proof.calls),
        contentType: "application/json",
    });
});

test("login fictício: CSRF incompatível bloqueia POST; envio duplo fica único", async ({
    page,
}, info) => {
    const proof = await offline(page, "GESTOR", false);
    await page.goto("/");
    await expect(
        page.getByRole("heading", { name: "Entrar no WMS" }),
    ).toBeVisible();
    const fill = async () => {
        await page
            .getByLabel("E-mail", { exact: true })
            .fill("qual@fixture.invalid");
        await page
            .getByLabel("Senha", { exact: true })
            .fill("Somente-ficticia-123");
    };
    proof.invalidCsrf(true);
    await fill();
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await expect(page.getByRole("alert")).toBeVisible();
    expect(proof.calls.filter((c) => c.path === "/api/auth/entrar")).toEqual(
        [],
    );
    proof.invalidCsrf(false);
    let release!: () => void;
    const hold = new Promise<void>((done) => {
        release = done;
    });
    proof.holdLogin(() => hold);
    await fill();
    await page.getByLabel("Senha", { exact: true }).focus();
    await page.keyboard.press("Enter");
    await page.keyboard.press("Enter");
    await expect
        .poll(
            () =>
                proof.calls.filter((c) => c.path === "/api/auth/entrar").length,
        )
        .toBe(1);
    release();
    await expect(page.locator(".home-header")).toBeVisible();
    expect(
        proof.calls.filter((c) => c.path === "/api/auth/entrar"),
    ).toHaveLength(1);
    expect(proof.unknown).toEqual([]);
    expect(proof.errors).toEqual([]);
    await info.attach("chamadas-interceptadas", {
        body: JSON.stringify(proof.calls),
        contentType: "application/json",
    });
});
