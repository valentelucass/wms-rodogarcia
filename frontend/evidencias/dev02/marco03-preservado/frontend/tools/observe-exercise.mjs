import { chromium } from "@playwright/test";
import { writeFileSync, existsSync } from "node:fs";
import assert from "node:assert/strict";

// Observa exclusivamente o frontend fictício próprio disponibilizado a Lucas.
const origin = "http://127.0.0.1:5188";
const output = "evidencias/marco02-exercicio-observado.json";
if (existsSync(output)) throw Error("Observação anterior preservada");
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext();
const requests = [],
    errors = [],
    violations = [],
    states = [];
await context.addInitScript(() => {
    window.__wmsCsp = [];
    document.addEventListener("securitypolicyviolation", (event) =>
        window.__wmsCsp.push({
            directive: event.violatedDirective,
            blocked: event.blockedURI,
        }),
    );
});
await context.route("**/*", (route) => {
    const url = route.request().url();
    requests.push(url);
    return new URL(url).origin === origin &&
        !new URL(url).pathname.startsWith("/api/")
        ? route.continue()
        : route.abort("blockedbyclient");
});
const page = await context.newPage();
page.on("pageerror", (error) => errors.push(error.message));
page.on("console", (message) => {
    if (message.type() === "error") errors.push(message.text());
});
try {
    for (const width of [1440, 768, 390]) {
        await page.setViewportSize({ width, height: 900 });
        for (const route of ["entrada", "coletor"]) {
            // Query própria força composição nova; âncora isolada não gera HTTP.
            const response = await page.goto(
                `${origin}/?observacao=${width}-${route}#${route}`,
            );
            await page.getByRole("banner").waitFor();
            await page
                .getByRole("heading", {
                    name:
                        route === "entrada" ? "Entrada e conferência" : "Coletor",
                    exact: true,
                    level: 1,
                })
                .waitFor();
            const state = await page.evaluate(() => ({
                title: document.querySelector("main h1, main h2")?.textContent,
                font: getComputedStyle(document.body).fontFamily,
                margin: getComputedStyle(document.body).margin,
                header: getComputedStyle(document.querySelector("header"))
                    .backgroundColor,
                width: innerWidth,
                documentWidth: document.documentElement.scrollWidth,
                css: [
                    ...document.querySelectorAll('link[rel="stylesheet"]'),
                ].map((link) => link.href),
                csp: window.__wmsCsp,
                text: document.body.innerText,
            }));
            assert.ok(response, "Observação exige navegação HTTP própria");
            assert.equal(response.status(), 200);
            assert.match(state.text, /EXERCÍCIO FICTÍCIO/);
            assert.match(state.font, /Arial/);
            assert.equal(state.margin, "0px");
            assert.equal(state.header, "rgb(18, 76, 171)");
            assert.ok(state.documentWidth <= width);
            assert.equal(state.csp.length, 0);
            violations.push(...state.csp);
            const screenshot = `evidencias/marco02-exercicio-${route}-${width}.png`;
            await page.screenshot({ path: screenshot, fullPage: true });
            states.push({
                route,
                width,
                httpStatus: response.status(),
                screenshot,
                ...state,
            });
        }
    }
    assert.deepEqual(errors, []);
    assert.deepEqual(
        requests.filter(
            (url) =>
                new URL(url).origin !== origin ||
                new URL(url).pathname.startsWith("/api/"),
        ),
        [],
    );
    writeFileSync(
        output,
        JSON.stringify(
            {
                at: new Date().toISOString(),
                origin,
                purpose:
                    "Observação do processo próprio fictício em5188, fonte final integrada; não integraçãoAPI/SQL",
                states,
                requests,
                errors,
                violations,
                backendCalled: false,
                externalRequests: [],
                serverKeptRunning: true,
            },
            null,
            2,
        ) + "\n",
    );
    console.log(
        "DEV próprio5188:6 estados CSS/DOM/responsividade, zeroAPI/external/CSP/console",
    );
} finally {
    await context.close();
    await browser.close();
}
