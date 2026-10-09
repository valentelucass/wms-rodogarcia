import { test as base, expect, type Page } from "@playwright/test";
import { writeFileSync } from "node:fs";
import { parse, isLosslessNumber } from "lossless-json";

interface Trace {
    kind: string;
    id: string;
    wire?: string;
    raw?: string;
    params?: Record<string, string>;
    query?: Record<string, unknown>;
}
const url = process.env.FE12_BASE_URL ?? "http://127.0.0.1:5194";
const test = base.extend<{ isolated: void }>({
    isolated: [
        async ({ page }, use, info) => {
            const network: string[] = [];
            await page.addInitScript(() => {
                const traces: unknown[] = [];
                Object.defineProperty(window, "__fe12Trace", { value: traces });
                for (const kind of ["request", "receipt"])
                    window.addEventListener("wms:fictitious-" + kind, (e) =>
                        traces.push({ kind, ...(e as CustomEvent).detail }),
                    );
            });
            await page.route("**/*", (route) => {
                const target = route.request().url();
                network.push(target);
                return new URL(target).origin === url &&
                    !new URL(target).pathname.startsWith("/api/")
                    ? route.continue()
                    : route.abort("blockedbyclient");
            });
            await use();
            const trace = await page.evaluate(
                () =>
                    (window as unknown as { __fe12Trace: Trace[] })
                        .__fe12Trace ?? [],
            );
            const name =
                "evidencias/fe12-cedro-ui-" +
                info.title.replace(/[^a-zA-Z0-9]/g, "_");
            writeFileSync(
                name + ".json",
                JSON.stringify(
                    {
                        test: info.title,
                        status: info.status,
                        trace,
                        network,
                        backendCalled: network.some((x) =>
                            new URL(x).pathname.startsWith("/api/"),
                        ),
                        external: network.filter(
                            (x) => new URL(x).origin !== url,
                        ),
                    },
                    null,
                    2,
                ),
            );
            await page.screenshot({ path: name + ".png", fullPage: true });
            expect(
                network.filter(
                    (x) =>
                        new URL(x).origin !== url ||
                        new URL(x).pathname.startsWith("/api/"),
                ),
            ).toEqual([]);
        },
        { auto: true },
    ],
});
async function trace(page: Page, id: string, kind = "request") {
    return page.evaluate(
        ({ id, kind }) =>
            (window as unknown as { __fe12Trace: Trace[] }).__fe12Trace
                .filter((x) => x.id === id && x.kind === kind)
                .at(-1)!,
        { id, kind },
    );
}
async function body(page: Page, id: string) {
    return parse((await trace(page, id)).wire!) as Record<string, unknown>;
}
const exact = (v: unknown) => (isLosslessNumber(v) ? v.value : v);
async function fill(page: Page, name: string, value: string) {
    await page.getByLabel(name, { exact: true }).fill(value);
}
async function confirm(page: Page, accepted = true) {
    await page
        .getByRole("button", { name: "Conferir e confirmar", exact: true })
        .click();
    await expect(
        page.getByRole("region", { name: "Confirmação da ação" }),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "Confirmar agora", exact: true })
        .click();
    if (accepted)
        await expect(
            page.getByText(/Resposta FICTÍCIA de exercício recebida/).last(),
        ).toBeVisible();
}
async function consult(page: Page) {
    await page
        .getByRole("button", { name: "Consultar", exact: true })
        .last()
        .click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/).last(),
    ).toBeVisible();
}
async function continueWith(page: Page, name: string) {
    await page.getByRole("button", { name, exact: true }).click();
}
const unit = "00000000-0000-4000-8000-000000000003";
const instant = "2026-10-08T12:00:00.123456Z";
async function count(page: Page, value = "8.000000") {
    await page.goto(url + "/#contagem");
    await fill(page, "Código *", unit);
    await consult(page);
    expect(
        (await trace(page, "EstoqueController.consultar")).params?.codigo,
    ).toBe(unit);
    await page
        .getByRole("button", { name: "Contagem física", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Registrar contagem física", exact: true })
        .click();
    await fill(page, "UUID da unidade *", unit);
    await fill(page, "Revisão atual da unidade *", "0");
    await fill(page, "Contado *", value);
    await fill(page, "Observado Em *", instant);
    await fill(page, "Motivo / justificativa *", "Contagem fictícia FE12");
    await confirm(page);
}
test("FE12 contagem consulta ajuste motivado consulta", async ({ page }) => {
    await count(page);
    const result = parse(
        (await trace(page, "ContagemController.contar", "receipt")).raw!,
    ) as Record<string, unknown>;
    expect(exact(result.esperado)).toBe("10.000000");
    expect(exact(result.contado)).toBe("8.000000");
    expect(exact(result.diferenca)).toBe("-2.000000");
    expect(exact(result.reservado)).toBe("0.000000");
    expect(result.efeitoJson).toBeNull();
    expect(await trace(page, "ContagemController.aplicar")).toBeUndefined();
    await continueWith(page, "Conferir contagem sem efeito no estoque");
    await consult(page);
    expect((await trace(page, "ContagemController.consultar")).params?.id).toBe(
        "2201",
    );
    await continueWith(page, "Conferir ajuste motivado desta contagem");
    await fill(page, "Revisão atual da unidade *", "0");
    for (const name of [
        "Motivo / justificativa *",
        "Causa *",
        "Destino *",
        "Comprovacao *",
    ])
        await fill(page, name, "Comprovação fictícia FE12");
    await page
        .getByRole("button", { name: "Adicionar Origens", exact: true })
        .click();
    await fill(page, "Origens / Item 1 / Entrada conferida (ID) *", "201");
    await fill(page, "Origens / Item 1 / Delta *", "-2.000000");
    await confirm(page);
    const applied = await body(page, "ContagemController.aplicar");
    expect(exact(applied.revisao)).toBe("1");
    expect(exact((applied.origens as Record<string, unknown>[])[0].delta)).toBe(
        "-2.000000",
    );
    expect((await trace(page, "ContagemController.aplicar")).params?.id).toBe(
        "2201",
    );
    await continueWith(page, "Consultar o efeito confirmado do ajuste");
    await consult(page);
    expect(
        (await trace(page, "ContagemController.consultar", "receipt")).raw,
    ).toContain('"situacao":"APLICADA"');
});
test("FE12 reserva recusa ajuste e conserva pendencia", async ({ page }) => {
    await count(page, "7.000000");
    await continueWith(page, "Conferir contagem sem efeito no estoque");
    await consult(page);
    await expect(
        page.getByText("PENDENTE_RESERVA", { exact: true }).last(),
    ).toBeVisible();
    await continueWith(page, "Conferir ajuste motivado desta contagem");
    await fill(page, "Revisão atual da unidade *", "0");
    for (const name of [
        "Motivo / justificativa *",
        "Causa *",
        "Destino *",
        "Comprovacao *",
    ])
        await fill(page, name, "Comprovação fictícia FE12");
    await page
        .getByRole("button", { name: "Adicionar Origens", exact: true })
        .click();
    await fill(page, "Origens / Item 1 / Entrada conferida (ID) *", "201");
    await fill(page, "Origens / Item 1 / Delta *", "-3.000000");
    await confirm(page, false);
    await expect(page.getByText(/ajuste recusado por reserva/)).toBeVisible();
    expect(
        await trace(page, "ContagemController.aplicar", "receipt"),
    ).toBeUndefined();
});
test("FE12 carga pendente revisada preparada confirmada consulta", async ({
    page,
}) => {
    await page.goto(url + "/#contagem");
    await page
        .getByRole("button", { name: "Carga inicial excepcional", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await fill(page, "Produto / SKU (ID) *", "1");
    await fill(page, "Referência *", "FE12-CARGA");
    await fill(page, "Etiqueta Fornecida", "FE12-L1");
    await fill(page, "Quantidade *", "10.000000");
    await fill(
        page,
        "Motivo / justificativa *",
        "Levantamento fictício com ausências",
    );
    await confirm(page);
    expect(
        (await trace(page, "CargaInicialController.criar", "receipt")).raw,
    ).toContain('"entradaId":null');
    await continueWith(page, "Consultar pendências desta carga excepcional");
    await consult(page);
    await expect(
        page.getByText(
            "Origem, nota e FIFO não informados no levantamento fictício",
            { exact: true },
        ),
    ).toBeVisible();
    await continueWith(page, "Revisar os dados levantados desta carga");
    await fill(
        page,
        "Dados do fato de contingência / Entrada Existente Id",
        "201",
    );
    await fill(
        page,
        "Dados do fato de contingência / Fonte",
        "Entrada fictícia conhecida FE12",
    );
    await fill(
        page,
        "Motivo / justificativa *",
        "Revisão fictícia documentada",
    );
    await confirm(page);
    await continueWith(
        page,
        "Conferir preparação pela revisão e hash recebidos",
    );
    await expect(page.getByLabel("Revisao *", { exact: true })).toHaveValue(
        "2",
    );
    await expect(
        page.getByLabel("SHA-256 do conteúdo *", { exact: true }),
    ).toHaveValue("c".repeat(64));
    await fill(
        page,
        "Motivo / justificativa *",
        "Preparação fictícia conferida",
    );
    await confirm(page);
    expect(
        (await trace(page, "CargaInicialController.preparar", "receipt")).raw,
    ).toContain('"situacao":"PREPARADA"');
    await continueWith(
        page,
        "Confirmar leitura e etiquetas da carga preparada",
    );
    await expect(page.getByLabel("Leitura *", { exact: true })).toHaveValue(
        "FE12-L1",
    );
    await fill(
        page,
        "Motivo / justificativa *",
        "Conferência fictícia de todas as etiquetas",
    );
    await confirm(page);
    const command = await body(page, "CargaInicialController.confirmar");
    expect(exact(command.versao)).toBe("2");
    expect(exact(command.revisao)).toBe("2");
    expect(command.conteudoHash).toBe("c".repeat(64));
    expect(command.etiquetasUnidades).toEqual([unit]);
    await continueWith(page, "Consultar a entrada regularizada");
    await consult(page);
    expect(
        (await trace(page, "CargaInicialController.consultar", "receipt")).raw,
    ).toContain('"situacao":"REGULARIZADA"');
});
test("FE12 CHEGADA consulta modo prova concilia consulta", async ({ page }) => {
    await page.goto(url + "/#contingencia");
    await page
        .getByRole("button", { name: "Registrar fato", exact: true })
        .click();
    await fill(
        page,
        "Identidade do registro de contingência *",
        "FE12-CHEGADA",
    );
    await page.getByLabel("Tipo *", { exact: true }).selectOption("CHEGADA");
    await fill(page, "Instante real do fato (ISO com fuso) *", instant);
    await fill(page, "Operador *", "Operador fictício FE12");
    await fill(page, "Fonte *", "Planilha fictícia FE12");
    await page
        .getByLabel("Efeito já registrado no WMS *", { exact: true })
        .selectOption("true");
    await fill(page, "Pedido (ID) *", "9007199254740993");
    await fill(
        page,
        "Dados do fato de contingência / Dados do fato de contingência / Revisão atual *",
        "0",
    );
    await fill(
        page,
        "Dados do fato de contingência / Dados do fato de contingência / Chegada física real (ISO com fuso) *",
        instant,
    );
    await fill(
        page,
        "Dados do fato de contingência / Dados do fato de contingência / Observacao *",
        "Chegada fictícia FE12",
    );
    await page
        .getByRole("button", { name: "Adicionar Itens", exact: true })
        .click();
    await fill(
        page,
        "Dados do fato de contingência / Dados do fato de contingência / Itens / Item 1 / Item da nota (ID) *",
        "401",
    );
    await fill(
        page,
        "Dados do fato de contingência / Dados do fato de contingência / Itens / Item 1 / Quantidade boa recebida *",
        "8.000001",
    );
    await fill(
        page,
        "Dados do fato de contingência / Dados do fato de contingência / Itens / Item 1 / Quantidade avariada *",
        "0",
    );
    await fill(page, "Motivo / justificativa *", "Registro fictício FE12");
    await confirm(page);
    const registered = await body(page, "ContingenciaController.registrar");
    expect(exact((registered.dados as Record<string, unknown>).pedidoId)).toBe(
        "9007199254740993",
    );
    await continueWith(page, "Consultar o fato manual registrado");
    await consult(page);
    await continueWith(page, "Conferir conciliação manual deste fato");
    await page.getByLabel("Modo *", { exact: true }).selectOption("VINCULAR");
    await page
        .getByRole("button", {
            name: "Informar Prova da operação original",
            exact: true,
        })
        .click();
    await fill(
        page,
        "Prova da operação original / Operacao Original *",
        String(registered.operacaoId),
    );
    await fill(
        page,
        "Prova da operação original / SHA-256 do conteúdo *",
        "d".repeat(64),
    );
    await fill(page, "Motivo / justificativa *", "Vínculo fictício com prova");
    await confirm(page);
    const reconciled = await body(page, "ContingenciaController.conciliar");
    expect(exact(reconciled.versao)).toBe("0");
    expect(reconciled.modo).toBe("VINCULAR");
    expect((reconciled.prova as Record<string, unknown>).conteudoHash).toBe(
        "d".repeat(64),
    );
    await continueWith(page, "Consultar resultado e pendências da conciliação");
    await consult(page);
    expect(
        (await trace(page, "ContingenciaController.consultar", "receipt")).raw,
    ).toContain('"situacao":"CONCILIADA"');
});
test("FE12 Operacao sem ajuste carga ou conciliacao", async ({ page }) => {
    await page.goto(url + "/#contagem");
    await page
        .getByRole("combobox", {
            name: "Perfil de apresentação fictício",
            exact: true,
        })
        .selectOption("OPERACAO");
    expect(
        await page
            .getByRole("button", {
                name: "Aplicar ajuste conferido",
                exact: true,
            })
            .count(),
    ).toBe(0);
    await page
        .getByRole("button", { name: "Carga inicial excepcional", exact: true })
        .click();
    await expect(
        page.getByText(
            "Nenhuma ação desta etapa é apresentada ao perfil atual. Selecione outra etapa.",
        ),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "Contingência", exact: true })
        .click();
    expect(
        await page
            .getByRole("button", {
                name: "Conciliar fato manualmente",
                exact: true,
            })
            .count(),
    ).toBe(0);
    expect(await trace(page, "ContagemController.aplicar")).toBeUndefined();
    expect(await trace(page, "CargaInicialController.criar")).toBeUndefined();
    expect(
        await trace(page, "ContingenciaController.conciliar"),
    ).toBeUndefined();
});
