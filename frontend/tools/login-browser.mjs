// Jornada real HTTP/JPA em H2 efêmero iniciada exclusivamente por LoginBrowserTest.
import { chromium } from "@playwright/test";
import { preview } from "vite";
import { mkdir, writeFile } from "node:fs/promises";
import { resolve } from "node:path";

const port = process.argv[2];
if (!/^[1-9]\d{0,4}$/.test(port ?? "") || Number(port) > 65535)
    throw new Error("PORTA_FIXTURE_INVALIDA");
const output = resolve("../orchestracao/.runtime/login-d32");
await mkdir(output, { recursive: true });
let server, browser, page;
let stage = "abrir_login";
const checks = [];
try {
    server = await preview({
        configFile: false,
        root: process.cwd(),
        preview: {
            host: "127.0.0.1",
            port: 59999,
            strictPort: true,
            proxy: {
                "/api": {
                    target: `http://127.0.0.1:${port}`,
                    changeOrigin: false,
                },
            },
        },
    });
    browser = await chromium.launch({ headless: true, channel: "chrome" });
    page = await browser.newPage({
        viewport: { width: 1366, height: 900 },
    });
    page.setDefaultTimeout(30000);
    page.on("pageerror", (error) =>
        console.error("PAGE_ERROR", error.name, error.message.split("\n")[0]),
    );
    const root = "desenvolvedor@rodogarcia.com.br";
    const email = "administrador-browser@test.invalid";
    const temporary = "Fixture-temporaria-123!";
    const definitive = "Fixture-browser-definitiva-456!";
    async function login(mail, password) {
        stage = "entrar";
        await page.getByRole("heading", { name: "Entrar no WMS" }).waitFor();
        await page.getByLabel("E-mail", { exact: true }).fill(mail);
        await page.getByLabel("Senha", { exact: true }).fill(password);
        await page.getByRole("button", { name: "Entrar", exact: true }).click();
    }
    async function change(current, next) {
        stage = "troca_obrigatoria";
        await page
            .getByRole("heading", { name: "Crie sua nova senha" })
            .waitFor();
        if (await page.getByRole("navigation").count())
            throw new Error("OPERACAO_ANTES_TROCA");
        await page.getByLabel("Senha atual", { exact: true }).fill(current);
        await page.getByLabel("Nova senha", { exact: true }).fill(next);
        await page
            .getByLabel("Repita a nova senha", { exact: true })
            .fill(next);
        await page.getByRole("button", { name: "Salvar nova senha" }).click();
    }
    await page.goto("http://127.0.0.1:59999");
    await login(root, temporary);
    await change(temporary, definitive);
    await login(root, definitive);
    await page
        .getByRole("button", { name: "Usuários e acessos", exact: true })
        .click();
    await page.getByText("Conta protegida", { exact: true }).waitFor();
    checks.push("principal_troca_inicial_e_conta_protegida");
    await page
        .getByRole("button", { name: "Criar usuário", exact: true })
        .click();
    await page
        .getByLabel("Nome", { exact: true })
        .fill("Administrador de teste");
    await page.getByLabel("E-mail", { exact: true }).fill(email);
    await page.getByLabel("Administrador de usuários", { exact: true }).check();
    await page.getByLabel("Senha temporária", { exact: false }).fill(temporary);
    await page
        .getByRole("button", { name: "Salvar usuário", exact: true })
        .click();
    await page
        .getByText(
            "Usuário criado. A senha temporária deverá ser trocada no primeiro acesso.",
            { exact: true },
        )
        .waitFor();
    await page.getByRole("button", { name: "Sair", exact: true }).click();
    await login(email, temporary);
    await change(temporary, definitive);
    await login(email, definitive);
    await page
        .getByRole("button", { name: "Usuários e acessos", exact: true })
        .click();
    await page
        .getByRole("heading", { name: "Usuários e acessos", exact: true })
        .waitFor();
    await page.screenshot({
        path: resolve(output, "usuarios-desktop.png"),
        fullPage: true,
    });
    checks.push("administrador_delegado_acessa_painel_com_perfil_operacao");
    await page.getByRole("button", { name: "Sair", exact: true }).click();
    await login(root, definitive);
    await page
        .getByRole("button", { name: "Usuários e acessos", exact: true })
        .click();
    const row = page.getByRole("row").filter({ hasText: email });
    await row
        .getByRole("button", { name: "Redefinir senha", exact: true })
        .click();
    await page.getByLabel("Senha temporária", { exact: false }).fill(temporary);
    await page
        .getByRole("button", { name: "Confirmar nova senha temporária" })
        .click();
    await page.getByText(/Senha temporária redefinida/).waitFor();
    await page.getByRole("button", { name: "Sair", exact: true }).click();
    await login(email, temporary);
    await page.getByRole("heading", { name: "Crie sua nova senha" }).waitFor();
    await page.reload();
    await page.getByRole("heading", { name: "Crie sua nova senha" }).waitFor();
    checks.push("reset_reexige_troca_e_refresh_mantem_restricao");
    await page.setViewportSize({ width: 360, height: 800 });
    await page.screenshot({
        path: resolve(output, "troca-mobile.png"),
        fullPage: true,
    });
    const state = await page.evaluate(() => ({
        local: localStorage.length,
        session: sessionStorage.length,
        width: document.documentElement.scrollWidth,
        viewport: innerWidth,
    }));
    if (state.local || state.session || state.width > state.viewport)
        throw new Error("ARMAZENAMENTO_OU_LAYOUT_INVALIDO");
    await page.getByLabel("Senha atual", { exact: true }).focus();
    await page.keyboard.press("Tab");
    if (
        !(await page
            .getByLabel("Nova senha", { exact: true })
            .evaluate((el) => el === document.activeElement))
    )
        throw new Error("TECLADO_INVALIDO");
    checks.push("mobile_360_sem_overflow_teclado_e_sem_tokens_storage");
    await page.getByRole("button", { name: "Sair", exact: true }).click();
    await page.getByRole("heading", { name: "Entrar no WMS" }).waitFor();
    await writeFile(
        resolve(output, "browser.json"),
        JSON.stringify(
            {
                natureza: "D32_BROWSER_HTTP_JPA_H2",
                sqlServer: false,
                checks,
                passed: true,
            },
            null,
            2,
        ),
    );
    console.log(
        `PASS ${checks.length} jornadas: navegador real + API + H2 efemero; zero SQL Server.`,
    );
} catch (error) {
    // Nao imprimir valores de campos, cookies ou trace de requests.
    console.error(
        "BROWSER_FIXTURE_FAILED",
        stage,
        error instanceof Error
            ? error.message
                  .split("\n")[0]
                  .replace(/Fixture[^\s'"<]*/g, "[fixture]")
            : "Unknown",
    );
    if (page)
        await page.screenshot({
            path: resolve(output, "falha-browser.png"),
            fullPage: true,
        });
    await writeFile(
        resolve(output, "browser.json"),
        JSON.stringify(
            {
                natureza: "D32_BROWSER_HTTP_JPA_H2",
                sqlServer: false,
                checks,
                passed: false,
            },
            null,
            2,
        ),
    );
    process.exitCode = 1;
} finally {
    await browser?.close();
    if (server) await new Promise((done) => server.httpServer.close(done));
}
