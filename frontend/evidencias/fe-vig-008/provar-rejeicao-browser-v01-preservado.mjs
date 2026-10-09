import { chromium, expect } from "@playwright/test";
import { parse, isLosslessNumber } from "lossless-json";
import { readFileSync, writeFileSync, existsSync } from "node:fs";
import { createHash } from "node:crypto";

const origin = "http://127.0.0.1:5189";
const output = "evidencias/fe-vig-008-browser-atual.json";
if (existsSync(output)) throw Error("Prova anterior preservada");
const browser = await chromium.launch({ headless: true });
const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } });
const requests = [], errors = [], captures = [];
let status = "failed", failure, trace = [], css;
await context.addInitScript(() => {
    window.__fe008 = [];
    window.__fe008Csp = [];
    for (const kind of ["request", "receipt"])
        window.addEventListener("wms:fictitious-" + kind, event =>
            window.__fe008.push({ kind, ...event.detail }));
    document.addEventListener("securitypolicyviolation", event =>
        window.__fe008Csp.push({ directive: event.violatedDirective, blocked: event.blockedURI }));
});
await context.route("**/*", route => {
    const url = route.request().url();
    requests.push(url);
    return new URL(url).origin === origin && !new URL(url).pathname.startsWith("/api/")
        ? route.continue() : route.abort("blockedbyclient");
});
const page = await context.newPage();
page.on("pageerror", error => errors.push(error.message));
page.on("console", message => { if (message.type() === "error") errors.push(message.text()); });
const button = name => page.getByRole("button", { name, exact: true });
const records = (id, kind) => page.evaluate(({ id, kind }) =>
    window.__fe008.filter(record => record.id === id && record.kind === kind), { id, kind });
const last = async (id, kind = "receipt") => (await records(id, kind)).at(-1);
const exact = value => isLosslessNumber(value) ? value.value : value;
const data = record => parse(record.raw);
const consult = async id => {
    const count = (await records(id, "receipt")).length;
    await button("Consultar").last().click();
    await expect.poll(async () => (await records(id, "receipt")).length).toBe(count + 1);
    await expect(page.getByText(/Resposta FICTÍCIA de exercício recebida/).last()).toBeVisible();
};
const choose = async (table, id) => {
    const row = page.getByRole("table", { name: table, exact: true })
        .getByRole("row").filter({ has: page.getByRole("cell", { name: id, exact: true }) });
    await expect(row).toHaveCount(1);
    await row.getByRole("button").click();
};
try {
    const response = await page.goto(origin + "/#cobranca");
    expect(response.status()).toBe(200);
    await expect(page.getByText(/EXERCÍCIO FICTÍCIO/)).toBeVisible();
    await consult("CalculoCobrancaController.listar");
    await choose("Cálculos", "801");
    await button("Fechamentos e ESL").click();
    await consult("FechamentoCobrancaController.listar");
    await choose("Fechamentos", "901");
    await button("Consultar versão e hash").click();
    await consult("FechamentoCobrancaController.versao");
    const before = data(await last("FechamentoCobrancaController.versao"));
    expect(exact(before.id)).toBe("1001");
    expect(before.situacao).toBe("PREPARADA");
    await button("2. Decisão integral do Gestor").click();
    await button("Rejeitar ciclo integral").click();
    await expect(page.getByLabel("Identificador *", { exact: true })).toHaveValue("901");
    await expect(page.getByLabel("Numero *", { exact: true })).toHaveValue("1");
    await page.getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Rejeição integral fictícia FE-VIG-008 no navegador");
    await button("Conferir e confirmar").click();
    await expect(page.getByRole("region", { name: "Confirmação da ação" })).toBeVisible();
    await button("Confirmar agora").click();
    await expect.poll(async () => (await records("FechamentoCobrancaController.rejeitar", "receipt")).length).toBe(1);
    const request = await last("FechamentoCobrancaController.rejeitar", "request");
    const command = parse(request.wire);
    expect(request.params.id).toBe("901");
    expect(exact(command.numero)).toBe("1");
    expect(exact(command.versao)).toBe("0");
    expect(command.motivo).toBe("Rejeição integral fictícia FE-VIG-008 no navegador");
    const rejected = data(await last("FechamentoCobrancaController.rejeitar"));
    expect(rejected.fechamento.situacao).toBe("REJEITADO");
    expect(rejected.versao.situacao).toBe("REJEITADA");
    expect(exact(rejected.versao.id)).toBe(exact(before.id));
    expect(exact(rejected.versao.calculoId)).toBe(exact(before.calculoId));
    expect(rejected.versao.conteudoHash).toBe(before.conteudoHash);
    await expect(page.locator(".results")).toContainText("REJEITADO");
    await expect(page.locator(".results")).toContainText("REJEITADA");
    captures.push("evidencias/fe-vig-008-browser-rejeicao.png");
    await page.screenshot({ path: captures.at(-1), fullPage: true });
    await button("1. Ciclo e versões").click();
    await button("Consultar detalhe").click();
    await consult("FechamentoCobrancaController.consultar");
    const queried = data(await last("FechamentoCobrancaController.consultar"));
    expect(exact(queried.id)).toBe(exact(rejected.fechamento.id));
    expect(exact(queried.versao)).toBe(exact(rejected.fechamento.versao));
    expect(queried.situacao).toBe("REJEITADO");
    await expect(page.locator(".results")).toContainText("REJEITADO");
    css = await page.evaluate(() => ({
        font: getComputedStyle(document.body).fontFamily,
        header: getComputedStyle(document.querySelector("header")).backgroundColor,
        csp: window.__fe008Csp,
    }));
    expect(css.font).toContain("Arial");
    expect(css.header).toBe("rgb(18, 76, 171)");
    expect(css.csp).toEqual([]);
    expect(errors).toEqual([]);
    expect(requests.filter(url => new URL(url).origin !== origin || new URL(url).pathname.startsWith("/api/"))).toEqual([]);
    captures.push("evidencias/fe-vig-008-browser-consulta.png");
    await page.screenshot({ path: captures.at(-1), fullPage: true });
    status = "passed";
} catch (error) {
    failure = error.stack ?? String(error);
    process.exitCode = 1;
} finally {
    trace = await page.evaluate(() => window.__fe008 ?? []);
    const source = "src/modules/financeiro/ExampleBilling.ts";
    writeFileSync(output, JSON.stringify({
        finding: "FE-VIG-008", at: new Date().toISOString(), status, failure,
        origin, cwd: process.cwd(), source,
        sourceSha256: createHash("sha256").update(readFileSync(source)).digest("hex"),
        boundary: "Chromium real, somente frontend próprio fictício; Gestor/contexto → cálculo801 → fechamento901 → versão1001 → rejeição integral → consulta atual",
        trace, requests, errors, css, captures,
        backendCalled: requests.some(url => new URL(url).pathname.startsWith("/api/")),
        externalRequests: requests.filter(url => new URL(url).origin !== origin),
        keptServerRunning: true,
    }, null, 2) + "\n");
    await context.close();
    await browser.close();
    console.log("FE-VIG-008 browser atual: " + status);
}
