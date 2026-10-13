import { test, expect, type Page } from "@playwright/test";
import { readFileSync } from "node:fs";
import type { Endpoint } from "../../src/contracts/runtime";

// Todas as APIs são interceptadas; nenhuma chamada chega ao backend ou ao SQL.
const { endpoints, fixtures } = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { endpoints: Endpoint[]; fixtures: Record<string, string> };

async function prepare(page: Page, origin: string) {
    const sent: { url: string; body: string }[] = [];
    await page.route("**/*", async (route) => {
        const request = route.request();
        const url = new URL(request.url());
        if (url.origin !== origin) return route.abort();
        if (!url.pathname.startsWith("/api/")) return route.continue();
        if (url.pathname === "/api/auth/csrf")
            return route.fulfill({
                json: { token: "test-csrf", header: "X-XSRF-TOKEN" },
            });
        if (url.pathname === "/api/auth/renovar")
            return route.fulfill({
                json: {
                    accessToken: "isolated-excel",
                    expiresIn: 300,
                    usuario: {
                        id: "excel-test",
                        nome: "Teste Excel",
                        email: "excel@example.invalid",
                        perfil: "GESTOR",
                        administrador: false,
                        principal: false,
                        ativo: true,
                        trocarSenha: false,
                        clientes: [],
                        armazens: [],
                        versao: 0,
                    },
                },
            });
        const contract = endpoints.find(
            (e) =>
                e.method === request.method() &&
                new RegExp(
                    "^" + e.path.replace(/\{[^}]+\}/g, "[^/]+") + "$",
                ).test(url.pathname),
        );
        if (!contract)
            return route.fulfill({
                status: 404,
                json: { detail: "API sem fixture" },
            });
        if (contract.id === "ClienteController.listar")
            return route.fulfill({
                json: {
                    itens: [],
                    pagina: 0,
                    tamanho: 100,
                    totalItens: 0,
                    totalPaginas: 0,
                },
            });
        if (contract.id === "ArmazemController.listar")
            return route.fulfill({
                json: {
                    itens: [
                        {
                            ...JSON.parse(fixtures["ArmazemController.listar"])
                                .itens[0],
                            id: 42,
                            codigo: "ARM-TESTE",
                            nome: "Armazém de teste",
                            situacao: "ATIVO",
                        },
                    ],
                    pagina: 0,
                    tamanho: 100,
                    totalItens: 1,
                    totalPaginas: 1,
                },
            });
        if (contract.id === "ImportacaoEnderecoController.previa") {
            sent.push({ url: url.pathname, body: request.postData() ?? "" });
            return route.fulfill({
                status: 422,
                json: {
                    detail: "Planilha vazia: preencha ao menos uma posição.",
                    codigo: "VALIDACAO",
                },
            });
        }
        return route.fulfill({
            contentType: "application/json",
            body: fixtures[contract.id],
        });
    });
    await page.goto("/#cadastros");
    await page
        .getByRole("button", { name: /Importar endereços Excel/ })
        .click();
    return sent;
}

test("fiscal, novo pedido e unitização ocultam referências técnicas", async ({
    page,
    baseURL,
}, info) => {
    await prepare(page, baseURL!);
    const fiscal = JSON.parse(fixtures["FiscalCadastroController.cliente"]);
    fiscal.versao = 27;
    fiscal.codigo = "CLI-000001";
    fiscal.dados = Object.fromEntries(
        Object.keys(fiscal.dados).map((key) => [key, null]),
    );
    const detalhe = JSON.parse(fixtures["PedidoEntradaController.consultar"]);
    detalhe.pedido = {
        ...detalhe.pedido,
        situacao: "EFETIVADO",
        versao: 31,
        armazemId: 42,
    };
    let sentFiscal: Record<string, unknown> | undefined;
    await page.route("**/api/**", async (route) => {
        const r = route.request(),
            path = new URL(r.url()).pathname;
        const e = endpoints.find(
            (e) =>
                e.method === r.method() &&
                new RegExp(
                    "^" + e.path.replace(/\{[^}]+\}/g, "[^/]+") + "$",
                ).test(path),
        );
        if (e?.id === "ClienteController.listar")
            return route.fulfill({
                contentType: "application/json",
                body: fixtures[e.id],
            });
        if (e?.id === "FiscalCadastroController.cliente")
            return route.fulfill({ json: fiscal });
        if (e?.id === "FiscalCadastroController.complementarCliente") {
            sentFiscal = r.postDataJSON();
            return route.fulfill({ json: { ...fiscal, versao: 28 } });
        }
        if (e?.id === "PedidoEntradaController.consultar")
            return route.fulfill({ json: detalhe });
        if (e?.id === "PedidoEntradaController.listar")
            return route.fulfill({
                json: {
                    ...JSON.parse(fixtures[e.id]),
                    itens: [detalhe.pedido],
                },
            });
        return route.fallback();
    });
    await page
        .getByRole("button", { name: "Dados fiscais cadastrais", exact: true })
        .click();
    await page
        .getByRole("button", {
            name: "Complementar fiscal do cliente",
            exact: true,
        })
        .click();
    let dialog = page.getByRole("dialog");
    await expect(
        dialog.getByLabel("Motivo / justificativa *", { exact: true }),
    ).toBeVisible();
    await expect(dialog.getByLabel(/Identificador|Revisão atual/)).toHaveCount(
        0,
    );
    await expect(dialog.getByText(/Dados do fato de contingência/)).toHaveCount(
        0,
    );
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Conferência cadastral fictícia");
    await page.screenshot({ path: info.outputPath("fiscal-automatico.png") });
    await dialog.getByRole("button", { name: "Conferir e confirmar" }).click();
    await expect(
        dialog.getByRole("region", { name: "Confirmação da ação" }),
    ).not.toContainText("Revisão atual");
    await dialog.getByRole("button", { name: "Confirmar agora" }).click();
    await expect.poll(() => sentFiscal?.versao).toBe(27);
    expect(sentFiscal?.operacaoId).toMatch(/^[a-f0-9-]{36}$/i);
    await page.keyboard.press("Escape");
    await page.goto("/#entrada");
    await page
        .getByRole("button", { name: "Novo pedido", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Criar manualmente", exact: true })
        .click();
    dialog = page.getByRole("dialog");
    await expect(
        dialog.getByLabel("Referência *", { exact: true }),
    ).toHaveCount(0);
    await expect(
        dialog.getByText("O código será gerado automaticamente ao confirmar."),
    ).toBeVisible();
    await page.keyboard.press("Escape");
    await page.goto("/#unidades");
    const filters = page.locator(".record-filters");
    await filters.getByLabel("Cliente", { exact: false }).fill("Exemplo");
    await filters.getByRole("option", { name: /Exemplo fictício/ }).click();
    await filters.getByLabel("Armazém", { exact: false }).fill("teste");
    await filters.getByRole("option", { name: /Armazém de teste/ }).click();
    await filters.getByRole("button", { name: "Aplicar filtros" }).click();
    await page
        .getByRole("button", { name: "Distribuir em unidades", exact: true })
        .last()
        .click();
    dialog = page.getByRole("dialog");
    await expect(
        dialog.getByLabel("Motivo / justificativa *", { exact: true }),
    ).toBeVisible();
    await expect(
        dialog.getByLabel(
            /Pedido \(ID\)|Versao Pedido|Identificador desta operação/,
        ),
    ).toHaveCount(0);
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .scrollIntoViewIfNeeded();
    await page.screenshot({ path: info.outputPath("unidades-automatico.png") });
});

test("código do serviço vem da resposta e UUID permanece oculto", async ({
    page,
    baseURL,
}, info) => {
    await prepare(page, baseURL!);
    const contract = endpoints.find(
        (e) => e.id === "ConfiguracaoCobrancaController.criarServico",
    )!;
    const bodies: Record<string, unknown>[] = [];
    await page.route("**" + contract.path, async (route) => {
        if (route.request().method() !== "POST") return route.fallback();
        bodies.push(route.request().postDataJSON());
        return route.fulfill({
            json: {
                ...JSON.parse(fixtures[contract.id]),
                codigo: "SRV-000001",
            },
        });
    });
    await page.evaluate(() => {
        location.hash = "precos";
    });
    await page
        .getByRole("button", { name: "Novo serviço", exact: true })
        .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.getByLabel("Código *", { exact: true })).toHaveCount(0);
    await expect(
        dialog.getByText("Identificador desta operação", { exact: true }),
    ).toHaveCount(0);
    await dialog
        .getByLabel("Descricao *", { exact: true })
        .fill("Serviço fictício automático");
    await dialog
        .getByLabel("Tipo *", { exact: true })
        .selectOption("ADICIONAL");
    await dialog
        .getByLabel("Unidade *", { exact: true })
        .fill("QUANTIDADE_PRODUTO");
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Conferência local do formulário automático");
    await page.screenshot({ path: info.outputPath("servico-automatico.png") });
    await dialog.getByRole("button", { name: "Conferir e confirmar" }).click();
    await expect(dialog).not.toContainText("Identificador desta operação");
    await dialog.getByRole("button", { name: "Confirmar agora" }).click();
    await expect(dialog.getByText("SRV-000001", { exact: true })).toBeVisible();
    expect(bodies).toHaveLength(1);
    expect(bodies[0]).toMatchObject({
        codigo: null,
        operacaoId: expect.stringMatching(/^[a-f\d-]{36}$/),
    });
    await expect(dialog).not.toContainText(String(bodies[0].operacaoId));
});

test("download, CSS externo e envio multipart preservado após voltar", async ({
    page,
    baseURL,
}) => {
    const sent = await prepare(page, baseURL!);
    const link = page.getByRole("link", { name: "Baixar planilha modelo" });
    await expect(link).toHaveCSS("display", "flex");
    await expect(link).toHaveCSS("text-decoration-line", "none");
    const downloading = page.waitForEvent("download");
    await link.click();
    const download = await downloading;
    expect(download.suggestedFilename()).toBe("modelo-enderecos.xlsx");
    expect(readFileSync((await download.path())!)).toEqual(
        readFileSync("public/modelos/enderecos.xlsx"),
    );
    await page
        .getByRole("button", { name: "Conferir prévia Excel", exact: true })
        .click();
    const dialog = page.getByRole("dialog");
    await expect(dialog.locator(".address-import-fields")).toHaveCSS(
        "display",
        "grid",
    );
    await dialog
        .getByRole("combobox", { name: "Armazém *", exact: true })
        .fill("teste");
    await dialog.getByRole("option", { name: /Armazém de teste/ }).click();
    await dialog
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Preparação das posições para conferência");
    await dialog
        .getByRole("button", { name: "Conferir prévia", exact: true })
        .click();
    await expect(dialog.getByRole("alert")).toContainText(
        "Selecione uma planilha .xlsx",
    );
    expect(sent).toHaveLength(0);
    await dialog
        .getByLabel("Planilha de endereços .xlsx *", { exact: true })
        .setInputFiles("public/modelos/enderecos.xlsx");
    await dialog
        .getByRole("button", { name: "Conferir prévia", exact: true })
        .click();
    const confirmation = dialog.getByRole("region", {
        name: "Confirmação da ação",
    });
    await expect(confirmation).not.toContainText(
        "Identificador desta operação",
    );
    await dialog.getByRole("button", { name: "Voltar à conferência" }).click();
    await expect(
        dialog.getByText(/Arquivo selecionado: enderecos.xlsx/),
    ).toBeVisible();
    await dialog
        .getByRole("button", { name: "Conferir prévia", exact: true })
        .click();
    await expect(confirmation).not.toContainText(
        "Identificador desta operação",
    );
    await dialog.getByRole("button", { name: "Confirmar agora" }).click();
    await expect.poll(() => sent.length).toBe(1);
    expect(sent[0].url).toContain("/42/");
    expect(sent[0].body).toMatch(/[a-f\d]{8}-(?:[a-f\d]{4}-){3}[a-f\d]{12}/i);
    expect(sent[0].body).toContain('filename="enderecos.xlsx"');
    expect(sent[0].body).toContain("Preparação das posições para conferência");
    await expect(dialog.getByRole("alert")).toContainText(
        "preencha ao menos uma posição",
    );
});

for (const [width, height, theme] of [
    [1440, 1000, "light"],
    [390, 844, "dark"],
] as const) {
    test(`popup ${width}px ${theme}: layout, teclado e fechamento`, async ({
        page,
        baseURL,
    }, info) => {
        await page.setViewportSize({ width, height });
        await prepare(page, baseURL!);
        await page.evaluate(
            (value) => (document.documentElement.dataset.ogTheme = value),
            theme,
        );
        const trigger = page.getByRole("button", {
            name: "Conferir prévia Excel",
            exact: true,
        });
        await trigger.click();
        const dialog = page.getByRole("dialog");
        const link = dialog.getByRole("link", {
            name: "Baixar planilha modelo",
        });
        await expect(link).toHaveCSS("text-decoration-line", "none");
        await expect(dialog.locator(".address-import-fields")).toHaveCSS(
            "display",
            "grid",
        );
        const warehouse = await dialog
            .getByRole("combobox", { name: "Armazém *", exact: true })
            .boundingBox();
        const reason = await dialog
            .getByLabel("Motivo / justificativa *", { exact: true })
            .boundingBox();
        expect(reason!.y).toBeGreaterThan(warehouse!.y + warehouse!.height);
        expect(
            await dialog.evaluate((el) => el.scrollWidth <= el.clientWidth),
        ).toBe(true);
        await link.focus();
        await page.keyboard.press("Tab");
        await expect(
            dialog.getByRole("combobox", { name: "Armazém *", exact: true }),
        ).toBeFocused();
        await page.screenshot({
            path: info.outputPath(`popup-${width}-${theme}.png`),
            fullPage: true,
        });
        await page.keyboard.press("Escape");
        await expect(dialog).toHaveCount(0);
        await expect(trigger).toBeFocused();
    });
}

test("formulários semelhantes compartilham a organização", async ({
    page,
    baseURL,
}, info) => {
    await prepare(page, baseURL!);
    for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: 900 });
        for (const [step, title] of [
            ["Clientes", "Novo cliente"],
            ["Armazéns", "Novo armazém"],
            ["Endereços", "Novo endereço"],
        ]) {
            await page
                .getByRole("navigation", { name: "Etapas de Cadastros" })
                .getByRole("button", { name: step, exact: true })
                .click();
            await page
                .getByRole("button", { name: title, exact: true })
                .click();
            const dialog = page.getByRole("dialog");
            await expect(
                dialog.getByRole("heading", { name: title, exact: true }),
            ).toHaveCount(1);
            const fields = dialog.locator(".operation-fields");
            await expect(fields).toHaveCSS("display", "grid");
            const shortFields = fields.locator(
                ":scope > .field:not(:has(textarea))",
            );
            const first = (await shortFields.nth(0).boundingBox())!;
            const second = (await shortFields.nth(1).boundingBox())!;
            if (width === 1440) {
                expect(second.x).toBeGreaterThan(first.x + first.width - 1);
            } else {
                expect(second.y).toBeGreaterThanOrEqual(first.y + first.height);
            }
            const area = dialog.locator("textarea").first();
            if (await area.count()) {
                const parentWidth = (await fields.boundingBox())!.width;
                expect((await area.boundingBox())!.width).toBeGreaterThan(
                    parentWidth - 3,
                );
            }
            expect(
                await dialog.evaluate((el) => el.scrollWidth <= el.clientWidth),
            ).toBe(true);
            await dialog
                .getByRole("button", { name: "Conferir e confirmar" })
                .scrollIntoViewIfNeeded();
            await expect(
                dialog.getByRole("button", { name: "Conferir e confirmar" }),
            ).toBeVisible();
            await page.screenshot({
                path: info.outputPath(`${step}-${width}.png`),
            });
            await page.keyboard.press("Escape");
            await expect(dialog).toHaveCount(0);
        }
    }
});
