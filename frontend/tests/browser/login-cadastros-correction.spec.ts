import { test, expect, type Page } from "@playwright/test";
import { writeFileSync } from "node:fs";

const user = {
    id: "user-fixture",
    nome: "Pessoa sintetica",
    email: "pessoa@fixture.invalid",
    perfil: "OPERACAO",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: ["1"],
    armazens: ["1"],
    versao: 0,
};
const prefix = process.env.CORR_PROOF_DIR!;
async function mockSession(page: Page) {
    const trace: unknown[] = [];
    let failure = 403,
        createInvalid = false,
        token = "A-fixture",
        userRevision = "9007199254740993";
    let delayed: (() => void) | undefined;
    let oldUsers = false;
    await page.route("**/*", async (route) => {
        const req = route.request(),
            url = new URL(req.url());
        if (url.origin !== "http://127.0.0.1:5195") {
            trace.push({ externalBlocked: url.origin });
            await route.abort();
            return;
        }
        if (!url.pathname.startsWith("/api/")) {
            await route.continue();
            return;
        }
        let status = 200,
            data: unknown;
        if (url.pathname === "/api/auth/csrf")
            data = { token: "csrf-fixture", header: "X-XSRF-TOKEN" };
        else if (
            ["/api/auth/renovar", "/api/auth/entrar"].includes(url.pathname)
        ) {
            if (url.pathname.endsWith("/entrar")) token = "B-fixture";
            data = { accessToken: token, expiresIn: 300, usuario: user };
        } else if (url.pathname === "/api/auth/sair") {
            status = 204;
            data = undefined;
        } else if (
            url.pathname === "/api/auth/usuarios" &&
            req.method() === "GET"
        ) {
            if (oldUsers) {
                oldUsers = false;
                await new Promise<void>((resolve) => {
                    delayed = resolve;
                });
                status = 401;
                data = { detail: "Recusa A sintetica" };
            } else if (failure) {
                status = failure;
                failure = 0;
                data = { detail: "Recusa sintetica" };
            } else
                data = {
                    content: [user],
                    number: 0,
                    totalPages: 1,
                    totalElements: 9007199254740991,
                };
        } else if (
            url.pathname === "/api/auth/usuarios" &&
            req.method() === "POST"
        ) {
            status = 201;
            data = createInvalid ? null : user;
        } else if (
            url.pathname.startsWith("/api/auth/usuarios/") &&
            req.method() === "PUT"
        ) {
            userRevision = "9007199254740994";
            data = user;
        } else {
            status = 503;
            data = { detail: "Resposta preparada indisponível" };
        }
        const event: Record<string, unknown> = {
            path: url.pathname,
            query: url.search,
            method: req.method(),
            status,
            ficticio: true,
        };
        if (req.method() === "PUT") event.body = req.postData();
        trace.push(event);
        let body = data === undefined ? "" : JSON.stringify(data);
        if (url.pathname.startsWith("/api/auth/usuarios") && status === 200)
            body = body
                .replace('"versao":0', `"versao":${userRevision}`)
                .replace(
                    '"totalElements":9007199254740991',
                    '"totalElements":9223372036854775807',
                );
        await route
            .fulfill({
                status,
                body,
                headers: {
                    "Content-Type": "application/json",
                    "Cache-Control": "no-store",
                },
            })
            .catch(() => {});
    });
    const consoleErrors: string[] = [];
    page.on("pageerror", (e) => consoleErrors.push(e.message));
    return {
        trace,
        consoleErrors,
        fail: (status: number) => {
            failure = status;
        },
        invalid: () => {
            createInvalid = true;
        },
        delay: () => {
            oldUsers = true;
        },
        release: () => delayed?.(),
        delayedReady: () => !!delayed,
    };
}
function evidence(name: string, value: unknown) {
    writeFileSync(
        prefix + "/" + name + ".json",
        JSON.stringify(value, null, 2),
    );
}
async function ready(page: Page) {
    await page.goto("/");
    await expect(
        page.getByRole("button", { name: "Cadastros", exact: true }),
    ).toBeVisible();
}
test("006 Tab Enter e clique conservam jornada/contexto/foco no shell atual", async ({
    page,
}) => {
    const mock = await mockSession(page);
    await ready(page);
    await page.getByRole("button", { name: "Cadastros", exact: true }).click();
    await page.getByLabel("Cliente (ID)").fill("1");
    await page.getByLabel("Armazém (ID)").fill("1");
    await page.getByRole("button", { name: "Aplicar contexto" }).click();
    const before = page.url();
    for (const action of ["Enter", "click"]) {
        let tabs = 0;
        while (
            !(await page
                .locator("a.skip")
                .evaluate((el) => el === document.activeElement)) &&
            tabs++ < 60
        )
            await page.keyboard.press("Tab");
        expect(tabs).toBeLessThanOrEqual(60);
        if (action === "Enter") await page.keyboard.press("Enter");
        else await page.locator("a.skip").click();
        expect(page.url()).toBe(before);
        await expect(
            page.getByRole("heading", { name: "Cadastros", exact: true }),
        ).toBeVisible();
        await expect(page.getByText("Contexto selecionado.")).toBeVisible();
        await expect(page.locator("#conteudo")).toBeFocused();
    }
    await page.screenshot({
        path: prefix + "/006-desktop.png",
        fullPage: true,
    });
    expect(mock.consoleErrors).toEqual([]);
    evidence("006", {
        trace: mock.trace,
        route: page.url(),
        focused: await page
            .locator("#conteudo")
            .evaluate((el) => el === document.activeElement),
        integration: false,
    });
});
test("007 403 recupera,002 edicao Long exato,003 incerto sem sucesso/replay", async ({
    page,
}) => {
    const mock = await mockSession(page);
    await ready(page);
    await page
        .getByRole("button", { name: "Usuários e acessos", exact: true })
        .click();
    await expect(page.getByRole("alert")).toContainText("não tem permissão");
    await expect(page.getByText("Carregando usuários…")).toHaveCount(0);
    await page.getByRole("button", { name: "Consultar novamente" }).click();
    await expect(
        page.getByText("Pessoa sintetica", { exact: true }).last(),
    ).toBeVisible();
    await expect(page.getByText(/9223372036854775807 usuários/)).toBeVisible();
    await page.getByRole("button", { name: "Editar", exact: true }).click();
    await page.getByRole("button", { name: "Salvar usuário" }).click();
    await expect(
        page.getByText(
            "Usuário atualizado. Os acessos anteriores foram encerrados.",
        ),
    ).toBeVisible();
    expect(
        mock.trace.some(
            (e) =>
                typeof e === "object" &&
                e !== null &&
                "body" in e &&
                String(e.body).includes('"versao":9007199254740993'),
        ),
    ).toBe(true);
    mock.invalid();
    await page.getByRole("button", { name: "Criar usuário" }).click();
    await page.getByLabel("Nome", { exact: true }).fill("Pessoa nova");
    await page
        .getByLabel("E-mail", { exact: true })
        .fill("nova@fixture.invalid");
    await page.getByLabel(/^Senha temporária/).fill("Sintetica-temporaria-123");
    await page.getByRole("button", { name: "Salvar usuário" }).click();
    await expect(page.getByRole("alert")).toContainText("incerto");
    await expect(page.getByText(/Usuário criado\./)).toHaveCount(0);
    await expect(
        page.getByRole("button", { name: "Salvar usuário" }),
    ).toBeDisabled();
    await page.getByRole("button", { name: "Consultar novamente" }).click();
    await expect(
        page.getByRole("button", { name: "Salvar usuário" }),
    ).toHaveCount(0);
    expect(
        mock.trace.filter(
            (e) =>
                typeof e === "object" &&
                e !== null &&
                "path" in e &&
                e.path === "/api/auth/usuarios" &&
                "method" in e &&
                e.method === "POST",
        ),
    ).toHaveLength(1);
    expect(mock.consoleErrors).toEqual([]);
    await page.screenshot({
        path: prefix + "/007-consulta.png",
        fullPage: true,
    });
    evidence("002-003-007", {
        trace: mock.trace,
        consoleErrors: mock.consoleErrors,
        integration: false,
    });
});
test("001 401 antigo apos logout e loginB nao expiraB,401 vigente encerra", async ({
    page,
}) => {
    const mock = await mockSession(page);
    mock.fail(0);
    mock.delay();
    await ready(page);
    await page
        .getByRole("button", { name: "Usuários e acessos", exact: true })
        .click();
    await expect.poll(mock.delayedReady).toBe(true);
    await page.getByRole("button", { name: "Sair", exact: true }).click();
    await page.getByLabel("E-mail", { exact: true }).fill(user.email);
    await page.getByLabel("Senha", { exact: true }).fill("Sintetica-login-123");
    await page.getByRole("button", { name: "Entrar", exact: true }).click();
    await expect(
        page.getByRole("button", { name: "Cadastros", exact: true }),
    ).toBeVisible();
    mock.release();
    await page.waitForTimeout(100);
    await expect(
        page.getByRole("heading", { name: "Entrar no WMS" }),
    ).toHaveCount(0);
    mock.fail(401);
    await page.getByRole("button", { name: "Início", exact: true }).click();
    await page
        .getByRole("button", { name: "Usuários e acessos", exact: true })
        .click();
    await expect(
        page.getByRole("heading", { name: "Entrar no WMS" }),
    ).toBeVisible();
    await expect(
        page.getByText("Sua sessão terminou. Entre novamente."),
    ).toBeVisible();
    expect(mock.consoleErrors).toEqual([]);
    evidence("001", {
        trace: mock.trace,
        consoleErrors: mock.consoleErrors,
        integration: false,
    });
});
