import { createServer } from "node:http";
import { readFileSync, existsSync, writeFileSync } from "node:fs";
import { resolve, join, extname } from "node:path";
import { chromium } from "@playwright/test";

// Servidor ESTATICO de artefatos isolados. Nunca Vite real, proxy, API ou SQL.
const directory = resolve("evidencias/dev02");
const reportPath = join(directory, "browser-isolado.json");
if (existsSync(reportPath)) throw Error("Prova anterior preservada");
const report = {
    owner: "WMS - Lume",
    pid: process.pid,
    startedAt: new Date().toISOString(),
    scope: "APRESENTACAO_ISOLADA_NAO_INTEGRACAO",
    source: "frontend/evidencias/dev02/fonte-estavel.json",
    existing5178_5188_5189Untouched: true,
    proxy: false,
    api: 0,
    external: [],
    cases: [],
    errors: [],
};
let browser;
try {
    browser = await chromium.launch({ headless: true });
    for (const mode of ["real", "ficticio"]) {
        const artifact = resolve(directory, "build-" + mode + "-isolado");
        const server = createServer((request, response) => {
            if (request.url?.startsWith("/api")) {
                report.api++;
                response.writeHead(503);
                response.end("LOCAL_STATIC_NO_API");
                return;
            }
            const path = new URL(request.url, "http://127.0.0.1").pathname;
            const file = resolve(
                artifact,
                "." + (path === "/" ? "/index.html" : path),
            );
            if (
                !file.startsWith(artifact + "/") &&
                !file.startsWith(artifact + "\\")
            ) {
                response.writeHead(403);
                response.end();
                return;
            }
            try {
                const type =
                    {
                        ".html": "text/html",
                        ".js": "text/javascript",
                        ".css": "text/css",
                        ".svg": "image/svg+xml",
                    }[extname(file)] ?? "application/octet-stream";
                response.writeHead(200, { "Content-Type": type });
                response.end(readFileSync(file));
            } catch {
                response.writeHead(404);
                response.end();
            }
        });
        await new Promise((accept) => server.listen(0, "127.0.0.1", accept));
        const url = "http://127.0.0.1:" + server.address().port;
        try {
            for (const width of [1440, 390]) {
                const context = await browser.newContext({
                    viewport: { width, height: 900 },
                });
                await context.route("**/*", (route) => {
                    const address = new URL(route.request().url());
                    if (
                        address.origin !== url ||
                        address.pathname.startsWith("/api")
                    ) {
                        report.external.push(route.request().url());
                        return route.abort();
                    }
                    return route.continue();
                });
                const page = await context.newPage();
                page.on("pageerror", (error) =>
                    report.errors.push(error.message),
                );
                page.on("console", (message) => {
                    if (message.type() === "error")
                        report.errors.push(message.text());
                });
                await page.goto(
                    url + (mode === "real" ? "/#entrada" : "/#inicio"),
                );
                await page
                    .getByRole("heading", {
                        name:
                            mode === "real"
                                ? "Sessão real indisponível"
                                : "Início da operação",
                    })
                    .waitFor();
                const hash = await page.evaluate(() => location.hash);
                await page.keyboard.press("Tab");
                await page.keyboard.press("Enter");
                if (
                    (await page.evaluate(() => location.hash)) !== hash ||
                    (await page.evaluate(() => document.activeElement?.id)) !==
                        "conteudo"
                )
                    throw Error("SKIP_CONTEXT_OR_FOCUS_LOST");
                const profileCount = await page
                    .getByLabel("Perfil de apresentação fictício")
                    .count();
                if (profileCount !== (mode === "real" ? 0 : 1))
                    throw Error("PROFILE_MODE_MISMATCH");
                const state = await page.evaluate(() => ({
                    header: document.querySelector("header")?.textContent,
                    font: getComputedStyle(document.body).fontFamily,
                    css: [...document.styleSheets].map((sheet) => sheet.href),
                    overflow: document.documentElement.scrollWidth > innerWidth,
                    storage: localStorage.length + sessionStorage.length,
                }));
                if (
                    !state.css.some((href) => href?.includes(".css")) ||
                    state.overflow ||
                    state.storage
                )
                    throw Error("CSS_OVERFLOW_OR_STORAGE");
                await page.screenshot({
                    path: join(
                        directory,
                        "browser-" + mode + "-" + width + ".png",
                    ),
                    fullPage: true,
                });
                report.cases.push({
                    mode,
                    width,
                    url,
                    hash,
                    keyboardSkip: "PASS",
                    fictitiousProfile: profileCount === 1,
                    ...state,
                });
                await context.close();
            }
        } finally {
            await new Promise((accept) => server.close(accept));
        }
    }
    if (report.api || report.external.length || report.errors.length)
        throw Error("ISOLATION_OR_CONSOLE_FAILURE");
    report.result = "PASS";
} catch (error) {
    report.result = "FAIL";
    report.errors.push(String(error));
    process.exitCode = 1;
} finally {
    await browser?.close();
    report.finishedAt = new Date().toISOString();
    report.ownStaticServersClosed = true;
    writeFileSync(reportPath, JSON.stringify(report, null, 2) + "\n");
}
