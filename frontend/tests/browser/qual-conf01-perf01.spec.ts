import { expect, test, type Page, type TestInfo } from "@playwright/test";
import { readFileSync } from "node:fs";
import { resolve, sep } from "node:path";
import { stringify } from "lossless-json";
import { overviewExample } from "../../src/api/mock/overview";
import { fulfillOverviewRead, overviewWire } from "./overview-fixture";
import type { Request } from "../../src/api/client";
import type { Perfil } from "../../src/contracts/runtime";

const origin = "https://1z8126n0-25581.brs.devtunnels.ms";
const build = resolve("../orchestracao/.runtime/qual-conf01/perf01/lume", process.env.WMS_PERF01_BROWSER_BUILD ?? "final-build-real");
const contracts = JSON.parse(readFileSync("src/contracts/endpoints.json", "utf8")) as Request["endpoint"][];
const endpoint = (id: string) => contracts.find(e => e.id === id)!;

async function offline(page: Page, initialRole: Perfil = "GESTOR") {
    const calls: { path: string; method: string; startedWallMs: number; sequence: number }[] = [];
    const completed: unknown[] = [], tasks: Promise<unknown>[] = [], unknown: string[] = [], errors: string[] = [];
    let role = initialRole, scopeVersion = 0, denied = false;
    page.on("pageerror", e => errors.push(e.message));
    page.on("request", r => calls.push({ path: new URL(r.url()).pathname, method: r.method(), startedWallMs: Date.now(), sequence: calls.length + 1 }));
    page.on("response", response => {
        const request = response.request();
        tasks.push((async () => {
            await response.finished();
            const sizes = await request.sizes().catch(() => null);
            completed.push({ path: new URL(response.url()).pathname, method: request.method(), status: response.status(), timing: request.timing(), sizes, scope: "browser interceptado; nenhum roundtrip ao runtime" });
        })());
    });
    await page.addInitScript(() => {
        const observed: { startTime: number; duration: number }[] = [];
        Object.defineProperty(window, "perf01LongTasks", { value: observed });
        if (PerformanceObserver.supportedEntryTypes.includes("longtask")) new PerformanceObserver(list => {
            for (const e of list.getEntries()) observed.push({ startTime: e.startTime, duration: e.duration });
        }).observe({ type: "longtask", buffered: true });
    });
    await page.route("**/*", async route => {
        const request = route.request(), url = new URL(request.url());
        if (url.origin !== origin) { unknown.push(url.pathname); return route.abort(); }
        if (!url.pathname.startsWith("/api/")) {
            const target = resolve(build, url.pathname === "/" ? "index.html" : "." + decodeURIComponent(url.pathname));
            if (!target.startsWith(build + sep)) { unknown.push(url.pathname); return route.abort(); }
            const ext = target.split(".").at(-1)!;
            const mime: Record<string, string> = { html: "text/html", js: "text/javascript", css: "text/css", svg: "image/svg+xml", woff2: "font/woff2" };
            try { return route.fulfill({ body: readFileSync(target), contentType: mime[ext] ?? "application/octet-stream", headers: ext === "html" ? { "Content-Security-Policy": "default-src 'self'; script-src 'self'; style-src 'self'; font-src 'self'; img-src 'self' data: blob:; connect-src 'self'; base-uri 'self'; frame-ancestors 'none'" } : {} }); }
            catch { unknown.push(url.pathname); return route.abort(); }
        }
        if (url.pathname === "/api/auth/csrf") return route.fulfill({ json: { token: "synthetic-only", header: "X-XSRF-TOKEN" } });
        if (url.pathname === "/api/auth/renovar") return route.fulfill({ json: {
            accessToken: "synthetic-memory-only", expiresIn: 300,
            usuario: { id: "synthetic-only", nome: "Pessoa fictícia", email: "perf@fixture.invalid", perfil: role, administrador: role === "GESTOR", principal: false, ativo: true, trocarSenha: false, clientes: scopeVersion ? ["2"] : [], armazens: scopeVersion ? ["2"] : [], versao: scopeVersion },
        } });
        if (request.method() !== "GET") { unknown.push(request.method() + " " + url.pathname); return route.abort(); }
        if (denied && ["/api/v1/visao-operacao", "/api/v1/dashboard"].includes(url.pathname)) return route.fulfill({ status: 403, json: { codigo: "ACESSO_NEGADO", detail: "Recusa fictícia" } });
        if (url.pathname === "/api/v1/visao-operacao") return route.fulfill({ contentType: "application/json", body: stringify(overviewWire(overviewExample({ endpoint: endpoint("VisaoOperacaoController.consultar"), params: {}, query: Object.fromEntries(url.searchParams), signal: new AbortController().signal }, role, false)))! });
        if (await fulfillOverviewRead(route, role, scopeVersion ? "2" : "1", scopeVersion ? "2" : "1")) return;
        unknown.push(url.pathname); return route.abort();
    });
    return { calls, errors, unknown,
        deny: () => { denied = true; },
        restrict: () => { scopeVersion++; role = "OPERACAO"; denied = true; },
        proof: async (info: TestInfo, measures: unknown) => {
            await Promise.allSettled(tasks);
            const longTasks = await page.evaluate(() => (window as Window & { perf01LongTasks?: unknown[] }).perf01LongTasks ?? []);
            expect(unknown).toEqual([]); expect(errors).toEqual([]);
            await info.attach("intercepted-request-render-proof", { body: JSON.stringify({ at: new Date().toISOString(), scope: "Somente build offline interceptado; latência/bytes/CPU do runtime não provados. Longtasks são duração ocupada da main thread, não tempo CPU.", calls, completed, longTasks, measures }), contentType: "application/json" });
        },
    };
}

test("PERF01 contexto: 4 leituras iniciais, 2 no novo contexto, atualização explícita mantém catálogo", async ({ page }, info) => {
    const proof = await offline(page);
    const started = Date.now();
    await page.goto("/");
    await expect(page.locator(".map-position").first()).toBeVisible();
    await expect(page.getByRole("region", { name: "Gráficos da operação" })).toHaveAttribute("aria-busy", "false");
    const initialUiWallMs = Date.now() - started;
    const catalogs = () => proof.calls.filter(c => ["/api/v1/clientes", "/api/v1/armazens"].includes(c.path)).length;
    expect(catalogs()).toBe(2);
    expect(proof.calls.filter(c => c.path.startsWith("/api/v1/"))).toHaveLength(4);
    for (const title of ["Cliente", "Armazém"]) {
        const input = page.getByRole("combobox", { name: title, exact: true });
        await input.click(); await input.fill(title === "Cliente" ? "Cliente de teste" : "Armazém de teste"); await input.press("Enter");
    }
    const contextStart = Date.now();
    await page.getByRole("button", { name: "Aplicar contexto" }).click();
    await expect(page.locator(".map-position")).toHaveCount(80);
    await expect(page.getByRole("region", { name: "Gráficos da operação" })).toHaveAttribute("aria-busy", "false");
    const contextUiWallMs = Date.now() - contextStart;
    expect(catalogs()).toBe(2);
    expect(proof.calls.filter(c => c.path.startsWith("/api/v1/"))).toHaveLength(6);
    await page.getByRole("button", { name: "Atualizar visão", exact: true }).click();
    await expect(page.getByRole("region", { name: "Visão geral da operação" })).toHaveAttribute("aria-busy", "false");
    expect(catalogs()).toBe(4);
    expect(proof.calls.filter(c => c.path === "/api/v1/visao-operacao")).toHaveLength(3);
    expect(proof.calls.filter(c => c.path === "/api/v1/dashboard")).toHaveLength(2);
    await proof.proof(info, { initialUiWallMs, contextUiWallMs, dataReads: 9, catalogReads: 4 });
});

test("PERF01 recusa 403 limpa valores/mapa e mantém sessão, sem replay", async ({ page }, info) => {
    const proof = await offline(page);
    await page.goto("/");
    await expect(page.locator(".map-position").first()).toBeVisible();
    await expect(page.locator(".overview-metrics")).toContainText("R$");
    proof.deny();
    await page.getByRole("button", { name: "Atualizar visão", exact: true }).click();
    await expect(page.getByRole("alert").first()).toContainText("Seu acesso não permite");
    await expect(page.locator(".map-position")).toHaveCount(0);
    await expect(page.locator(".overview-metrics")).not.toContainText("R$");
    await expect(page.getByRole("button", { name: "Sair", exact: true })).toBeVisible();
    await expect(page.getByRole("button", { name: "Entrar", exact: true })).toHaveCount(0);
    expect(proof.calls.filter(c => c.path === "/api/v1/visao-operacao")).toHaveLength(2);
    expect(proof.calls.filter(c => c.path === "/api/auth/renovar")).toHaveLength(1);
    await proof.proof(info, { refusal: 403, overviewReads: 2, renewals: 1 });
});

test("PERF01 renovação com redução de direitos revalida catálogo e remove financeiro/admin antigos", async ({ page }, info) => {
    await page.clock.install();
    const proof = await offline(page);
    await page.goto("/");
    await expect(page.locator(".map-position").first()).toBeVisible();
    await expect(page.locator(".overview-metrics")).toContainText("R$");
    await expect(page.getByRole("button", { name: "Administrar usuários" })).toBeVisible();
    proof.restrict();
    await page.clock.fastForward(240000);
    await expect(page.getByRole("button", { name: "Administrar usuários" })).toHaveCount(0);
    await expect(page.locator(".overview-metrics")).not.toContainText("R$");
    await expect(page.locator(".map-position")).toHaveCount(0);
    await expect(page.getByRole("alert").first()).toContainText("Seu acesso não permite");
    await expect(page.getByRole("combobox", { name: "Cliente", exact: true })).toBeEnabled();
    expect(proof.calls.filter(c => c.path === "/api/auth/renovar")).toHaveLength(2);
    expect(proof.calls.filter(c => ["/api/v1/clientes", "/api/v1/armazens"].includes(c.path))).toHaveLength(4);
    await proof.proof(info, { renewals: 2, catalogReads: 4, oldFinanceRemoved: true });
});
