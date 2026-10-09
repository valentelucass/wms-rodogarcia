import { test, expect, type Page } from "@playwright/test";
import { writeFileSync } from "node:fs";
import { isLosslessNumber, parse as parseExact } from "lossless-json";
import { origin as frontendOrigin, proofLabel } from "./environment";
const isObject = (v: unknown): v is Record<string, unknown> =>
    typeof v === "object" && v !== null && !Array.isArray(v);
const requests: string[] = [];
interface Trace {
    kind: string;
    id: string;
    params?: Record<string, string>;
    wire?: string;
    raw?: string;
}
declare global {
    interface Window {
        __wmsFictitiousTrace?: Trace[];
    }
}
const exercises: { test: string; trace: Trace[] }[] = [];
const code = "00000000-0000-4000-8000-000000000001";
const origin = frontendOrigin + "/";
test.beforeEach(async ({ page }) => {
    await page.addInitScript(() => {
        const trace: unknown[] = [];
        Object.defineProperty(window, "__wmsFictitiousTrace", { value: trace });
        for (const kind of ["request", "receipt"])
            window.addEventListener("wms:fictitious-" + kind, (event) =>
                trace.push({ kind, ...(event as CustomEvent).detail }),
            );
    });
    await page.route("**/*", (route) => {
        const url = route.request().url();
        requests.push(url);
        if (
            url.startsWith(origin) &&
            !new URL(url).pathname.startsWith("/api/")
        )
            return route.continue();
        return route.abort("blockedbyclient");
    });
});
test.afterEach(async ({ page }, info) => {
    exercises.push({
        test: info.title,
        trace: await page.evaluate(() => window.__wmsFictitiousTrace ?? []),
    });
    writeFileSync(
        `evidencias/${proofLabel}-browser-jornadas-payloads.json`,
        JSON.stringify(exercises, null, 2),
    );
});
test.afterAll(() =>
    writeFileSync(
        `evidencias/${proofLabel}-browser-rede.json`,
        JSON.stringify(
            {
                requests,
                allowedOrigin: frontendOrigin,
                externalRequests: requests.filter(
                    (x) => new URL(x).origin !== frontendOrigin,
                ),
                backendCalled: requests.some((x) =>
                    new URL(x).pathname.startsWith("/api/"),
                ),
            },
            null,
            2,
        ),
    ),
);

async function confirm(page: Page) {
    await page
        .getByRole("button", { name: "Conferir e confirmar", exact: true })
        .click();
    await expect(
        page.getByRole("region", { name: "Confirmação da ação" }),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "Confirmar agora", exact: true })
        .click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
}
async function continueTo(page: Page, title: string) {
    await page.getByRole("button", { name: title, exact: true }).click();
}
async function recorded(page: Page, id: string, kind = "request") {
    const result = await page.evaluate(
        ({ id, kind }) =>
            (window.__wmsFictitiousTrace ?? [])
                .filter((x) => x.id === id && x.kind === kind)
                .at(-1),
        { id, kind },
    );
    expect(result, `${kind} ${id}`).toBeDefined();
    return result!;
}
function command(t: Trace) {
    const body = parseExact(t.wire ?? "null");
    expect(isObject(body)).toBe(true);
    return body as Record<string, unknown>;
}
function exact(v: unknown) {
    return isLosslessNumber(v) ? v.value : v;
}
function value(t: Trace) {
    return parseExact(t.raw ?? "null") as Record<string, unknown>;
}

test("Recebimento integral: nota → chegada → efetivação → unidade → etiqueta → posição sem copiar IDs", async ({
    page,
}) => {
    await page.goto("/#entrada");
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await page
        .getByLabel("Referência *", { exact: true })
        .fill("DEMO-RECEBIMENTO");
    await confirm(page);
    const created = value(
        await recorded(page, "PedidoEntradaController.criar", "receipt"),
    );
    expect(exact(created.id)).toBe("101");
    expect(exact(created.versao)).toBe("0");
    await continueTo(page, "Continuar com a nota deste pedido");
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("101");
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("0");
    await page
        .getByRole("button", { name: "Consultar produtos para a nota" })
        .click();
    await page
        .getByRole("region", { name: "Consultar referências para a tarefa" })
        .getByRole("button", { name: "Selecionar registro 1" })
        .click();
    await page.getByLabel("Serie *", { exact: true }).fill("1");
    await page.getByLabel("Numero *", { exact: true }).fill("123");
    await page.getByLabel("Emissao *", { exact: true }).fill("2026-10-07");
    await page
        .getByRole("button", { name: "Adicionar Itens", exact: true })
        .click();
    await page
        .getByLabel("Itens / Item 1 / Numero Item *", { exact: true })
        .fill("1");
    await expect(
        page.getByLabel("Itens / Item 1 / Produto / SKU (ID) *", {
            exact: true,
        }),
    ).toHaveValue("11");
    await page
        .getByLabel("Itens / Item 1 / Quantidade prevista *", { exact: true })
        .fill("10.000000");
    await confirm(page);
    const note = await recorded(page, "PedidoEntradaController.nota");
    expect(note.params?.id).toBe("101");
    expect(exact(command(note).versao)).toBe("0");
    expect(note.wire).toContain('"produtoId":11');
    await continueTo(page, "Conferir notas e itens recebidos do servidor");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    await page
        .getByRole("button", { name: "3. Conferência e chegadas", exact: true })
        .click();
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("1");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Iniciar conferência fictícia");
    await confirm(page);
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("EM_CONFERENCIA");
    await continueTo(page, "Registrar chegada dos itens consultados");
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("2");
    await expect(
        page.getByLabel("Itens / Item 1 / Item da nota (ID) *", {
            exact: true,
        }),
    ).toHaveValue("401");
    await page
        .getByLabel("Chegada física real (ISO com fuso) *", { exact: true })
        .fill("2026-10-08T12:00:00.123456Z");
    await page
        .getByLabel("Observacao *", { exact: true })
        .fill("Recebimento fictício conferido");
    await page
        .getByLabel("Itens / Item 1 / Quantidade boa recebida *", {
            exact: true,
        })
        .fill("10.000000");
    await confirm(page);
    const arrival = await recorded(page, "PedidoEntradaController.chegada");
    expect(arrival.params?.id).toBe("101");
    expect(exact(command(arrival).versao)).toBe("2");
    expect(arrival.wire).toContain('"itemNotaId":401');
    await continueTo(page, "Comparar previsto e físico registrado");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    await page
        .getByRole("button", {
            name: "4. Divergência e efetivação",
            exact: true,
        })
        .click();
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("3");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Efetivação fictícia integral");
    await confirm(page);
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("EFETIVADO");
    await continueTo(page, "Consultar entradas efetivadas para unitização");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("Entrada conferida: 201");
    await continueTo(page, "Unitizar a entrada conferida selecionada");
    await expect(page.getByLabel("Pedido (ID) *", { exact: true })).toHaveValue(
        "101",
    );
    await expect(
        page.getByLabel("Entrada conferida (ID) *", { exact: true }),
    ).toHaveValue("201");
    await expect(
        page.getByLabel("Versao Pedido *", { exact: true }),
    ).toHaveValue("4");
    await page
        .getByRole("button", { name: "Consultar embalagens deste SKU" })
        .click();
    await page
        .getByRole("region", { name: "Consultar referências para a tarefa" })
        .getByRole("button", { name: "Selecionar registro 1" })
        .click();
    await page
        .getByRole("button", { name: "Adicionar Unidades", exact: true })
        .click();
    await expect(
        page.getByLabel("Unidades / Item 1 / Embalagem / DUN (ID) *", {
            exact: true,
        }),
    ).toHaveValue("21");
    await page
        .getByLabel("Unidades / Item 1 / Tipo *", { exact: true })
        .selectOption("PALLET");
    await page
        .getByLabel("Unidades / Item 1 / Condicao *", { exact: true })
        .selectOption("BOA");
    await page
        .getByLabel("Unidades / Item 1 / Quantidade *", { exact: true })
        .fill("10.000000");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Unitização fictícia integral");
    await confirm(page);
    const units = await recorded(page, "UnidadeLogisticaController.unitizar");
    expect(units.params).toEqual({ pedidoId: "101", entradaId: "201" });
    expect(exact(command(units).versaoPedido)).toBe("4");
    expect(units.wire).toContain('"embalagemId":21');
    await continueTo(page, "Consultar etiqueta da unidade criada");
    await expect(page.getByLabel("Código *", { exact: true })).toHaveValue(
        code,
    );
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByRole("img", { name: "QR do UUID " + code }),
    ).toBeVisible();
    await continueTo(page, "Endereçar esta unidade no coletor");
    await expect(page.getByLabel("1. Leia o UUID da unidade")).toHaveValue(
        code,
    );
    await page
        .getByRole("button", { name: "Consultar unidade e posições" })
        .click();
    await expect(page.getByLabel("2. Leia o código da posição")).toBeFocused();
    await page.getByLabel("2. Leia o código da posição").fill("A101");
    await page.getByLabel("2. Leia o código da posição").press("Enter");
    await expect(
        page.getByLabel("Revisão atual da unidade *", { exact: true }),
    ).toHaveValue("7");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Endereçamento fictício conferido");
    await confirm(page);
    const move = await recorded(page, "EstoqueController.posicionar");
    expect(move.params?.codigo).toBe(code);
    expect(exact(command(move).versaoUnidade)).toBe("7");
    expect(move.wire).toContain('"enderecoId":81');
    await page.screenshot({
        path: "evidencias/marco02-r05-browser-recebimento-completo.png",
        fullPage: true,
    });
});

test("Saída integral: pedido → FIFO → reserva → etiqueta → leitura → separação conserva identidades e revisões", async ({
    page,
}) => {
    await page.goto("/#saida");
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    // Consulted product choice, without copying an ID from a different entity.
    await page.getByRole("button", { name: "Cadastros", exact: true }).click();
    await page
        .getByRole("button", { name: "Produtos / SKU", exact: true })
        .click();
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await page.getByRole("button", { name: "Selecionar registro 1" }).click();
    await page
        .getByRole("button", { name: "Saída, FIFO e reserva", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await page.getByLabel("Referência *", { exact: true }).fill("DEMO-SAIDA");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Pedido fictício integral");
    await page
        .getByRole("button", { name: "Adicionar Itens", exact: true })
        .click();
    await expect(
        page.getByLabel("Itens / Item 1 / Produto / SKU (ID) *", {
            exact: true,
        }),
    ).toHaveValue("11");
    await page
        .getByLabel("Itens / Item 1 / Quantidade *", { exact: true })
        .fill("10.000000");
    await confirm(page);
    await continueTo(page, "Consultar FIFO deste pedido integral");
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("601");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    await continueTo(page, "Confirmar reserva da sugestão do servidor");
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("0");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Reserva fictícia da sugestão");
    await confirm(page);
    const reserve = await recorded(page, "PedidoSaidaController.reservar");
    expect(reserve.params?.id).toBe("601");
    expect(exact(command(reserve).versao)).toBe("0");
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("RESERVADO");
    await continueTo(page, "Ler a etiqueta da reserva confirmada");
    await page
        .getByRole("button", {
            name: "Consultar etiqueta da unidade",
            exact: true,
        })
        .click();
    await expect(
        page.getByRole("region", {
            name: "Consultar referências para a tarefa",
        }),
    ).toContainText(code);
    await page
        .getByRole("button", {
            name: "Usar referências consultadas no formulário (descarta edição atual)",
            exact: true,
        })
        .click();
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("601");
    await expect(
        page.getByLabel("Reserva (ID) *", { exact: true }),
    ).toHaveValue("701");
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("1");
    await expect(
        page.getByLabel("Revisão do conteúdo da etiqueta *", { exact: true }),
    ).toHaveValue("7");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Leitura fictícia da etiqueta");
    await confirm(page);
    const read = await recorded(page, "ExpedicaoController.ler");
    expect(read.params?.id).toBe("601");
    const rb = command(read);
    expect(exact(rb.reservaId)).toBe("701");
    expect(exact(rb.versao)).toBe("1");
    expect(exact(rb.revisaoConteudo)).toBe("7");
    expect(rb.codigoLido).toBe(code);
    await continueTo(page, "Confirmar destino da reserva lida");
    await page
        .getByRole("button", { name: "Consultar posições para separação" })
        .click();
    await page
        .getByRole("region", { name: "Consultar referências para a tarefa" })
        .getByRole("button", { name: "Selecionar registro 1" })
        .click();
    await page
        .getByRole("button", {
            name: "Usar referências consultadas no formulário (descarta edição atual)",
            exact: true,
        })
        .click();
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("2");
    await expect(
        page.getByLabel("Destinacao / Reserva (ID) *", { exact: true }),
    ).toHaveValue("701");
    await expect(
        page.getByLabel(
            "Destinacao / Posições de destino / Item 1 / Endereço (ID) *",
            { exact: true },
        ),
    ).toHaveValue("81");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Separação fictícia integral");
    await confirm(page);
    const separated = await recorded(page, "ExpedicaoController.separar");
    expect(separated.params?.id).toBe("601");
    expect(exact(command(separated).versao)).toBe("2");
    expect(separated.wire).toContain('"reservaId":701');
    expect(separated.wire).toContain('"enderecoId":81');
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("SEPARADO");
    await page.screenshot({
        path: "evidencias/marco02-r05-browser-saida-completa.png",
        fullPage: true,
    });
});

test("Financeiro: cálculo → fechamento → memória → aprovação → entrega → conflito externo → tratativa", async ({
    page,
}) => {
    await page.goto("/#cobranca");
    await page
        .getByRole("button", { name: "3. Memória do servidor", exact: true })
        .click();
    await page
        .getByRole("button", {
            name: "Solicitar cálculo ao servidor",
            exact: true,
        })
        .click();
    await page
        .getByLabel("Início do período *", { exact: true })
        .fill("2026-10-01");
    await page
        .getByLabel("Fim exclusivo do período *", { exact: true })
        .fill("2026-11-01");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Cálculo fictício de conferência");
    await confirm(page);
    const calculation = value(
        await recorded(page, "CalculoCobrancaController.calcular", "receipt"),
    );
    expect(exact(calculation.id)).toBe("801");
    expect(exact(calculation.total)).toBe("10.000000");
    await expect(page.locator(".results")).toContainText("10.000000");
    await continueTo(page, "Preparar fechamento com este cálculo");
    await expect(page.getByLabel("Calculo Id *", { exact: true })).toHaveValue(
        "801",
    );
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Preparar ciclo fictício");
    await confirm(page);
    const prepare = await recorded(
        page,
        "FechamentoCobrancaController.preparar",
    );
    expect(exact(command(prepare).calculoId)).toBe("801");
    await continueTo(page, "Conferir memória e hash da versão preparada");
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("901");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    const version = value(
        await recorded(page, "FechamentoCobrancaController.versao", "receipt"),
    );
    expect(exact(version.id)).toBe("1001");
    expect(exact(version.fechamentoId)).toBe("901");
    expect(exact(version.calculoId)).toBe("801");
    await continueTo(page, "Consultar demonstrativo da versão selecionada");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    const memory = await recorded(
        page,
        "FechamentoCobrancaController.demonstrativo",
    );
    expect(memory.params).toEqual({ id: "901", numero: 1 });
    await expect(page.locator(".results")).toContainText("Calculo");
    await expect(page.locator(".results")).toContainText("10.000000");
    const downloadPromise = page.waitForEvent("download");
    await page
        .getByRole("button", {
            name: "Baixar os bytes do demonstrativo fictício",
        })
        .click();
    const download = await downloadPromise;
    await download.saveAs(
        "evidencias/marco02-r05-browser-demonstrativo-ficticio.json",
    );
    await continueTo(page, "Conferir e decidir o ciclo inteiro");
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("901");
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("0");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Aprovação fictícia integral");
    await confirm(page);
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("APROVADO");
    await continueTo(page, "Registrar entrega manual desta versão");
    await expect(
        page.getByLabel("Arquivo Hash *", { exact: true }),
    ).toHaveValue(String(version.conteudoHash));
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("1");
    await page
        .getByLabel("Destino Referencia *", { exact: true })
        .fill("ESL EXERCÍCIO MANUAL");
    await page
        .getByLabel("Entregue Em *", { exact: true })
        .fill("2026-10-08T12:00:00.123456Z");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Entrega fictícia efetuada externamente");
    await confirm(page);
    const delivery = await recorded(
        page,
        "FechamentoCobrancaController.entregar",
    );
    expect(delivery.params?.id).toBe("901");
    expect(exact(command(delivery).versao)).toBe("1");
    expect(command(delivery).arquivoHash).toBe(String(version.conteudoHash));
    await page
        .getByRole("button", {
            name: "Registrar referência NFS-e",
            exact: true,
        })
        .click();
    for (const reference of ["NFS-DEMO-A", "NFS-DEMO-B"]) {
        await page
            .getByLabel("Emissor Documento *", { exact: true })
            .fill("00000000000000");
        await page
            .getByLabel("Referencia Externa *", { exact: true })
            .fill(reference);
        await page
            .getByLabel("Emitida Em *", { exact: true })
            .fill("2026-10-08T12:30:00.123456Z");
        await page
            .getByLabel("Fonte *", { exact: true })
            .fill("Conferência externa fictícia");
        await page
            .getByLabel("Conferida Por *", { exact: true })
            .fill("Operador fictício");
        await page
            .getByLabel("Motivo / justificativa *", { exact: true })
            .fill("Referência externa fictícia conferida");
        await confirm(page);
        if (reference === "NFS-DEMO-A")
            await page
                .getByRole("button", {
                    name: "Iniciar nova operação",
                    exact: true,
                })
                .click();
    }
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("CONFLITO_EXTERNO");
    await continueTo(page, "Consultar referências e conflitos externos");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    await continueTo(page, "Registrar conferência das referências externas");
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("901");
    await expect(
        page.getByLabel("Revisão atual *", { exact: true }),
    ).toHaveValue("4");
    await expect(
        page.getByLabel("Referencias / Item 1 / Referencia Id *", {
            exact: true,
        }),
    ).toHaveValue("1101");
    await expect(
        page.getByLabel("Referencias / Item 2 / Referencia Id *", {
            exact: true,
        }),
    ).toHaveValue("1102");
    await page
        .getByLabel("Resultado *", { exact: true })
        .selectOption("EMITIDO");
    await page
        .getByLabel("Referencias / Item 1 / Situação informada *", {
            exact: true,
        })
        .selectOption("MANTIDO");
    await page
        .getByLabel("Referencias / Item 2 / Situação informada *", {
            exact: true,
        })
        .selectOption("CANCELAMENTO_COMPROVADO");
    await page
        .getByLabel("Fonte *", { exact: true })
        .fill("Prova fictícia das duas referências");
    await page
        .getByLabel("Conferida Por *", { exact: true })
        .fill("Gestor fictício");
    await page
        .getByLabel("Conferida Em *", { exact: true })
        .fill("2026-10-08T13:00:00.123456Z");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Tratativa fictícia com comprovantes");
    await confirm(page);
    const treatment = await recorded(
        page,
        "FechamentoCobrancaController.tratar",
    );
    expect(treatment.params?.id).toBe("901");
    expect(exact(command(treatment).versao)).toBe("4");
    expect(treatment.wire).toContain('"referenciaId":1101');
    expect(treatment.wire).toContain('"referenciaId":1102');
    await continueTo(page, "Conferir o fechamento após a tratativa");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(
        page.getByText(/Resposta FICTÍCIA de exercício recebida/),
    ).toBeVisible();
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("APROVADO");
    await expect(
        page.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toContainText("Revisão 5");
    await page.screenshot({
        path: "evidencias/marco02-r05-browser-financeiro-completo.png",
        fullPage: true,
    });
});
test("Desktop: cliente, entrada manual, consulta e perfil", async ({
    page,
}) => {
    await page.goto("/");
    await page.getByRole("button", { name: "Cadastros", exact: true }).click();
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await page.getByLabel("Código *", { exact: true }).fill("DEMO-TESTE");
    await page
        .getByLabel("Nome *", { exact: true })
        .fill("Cliente fictício do browser");
    await page
        .getByLabel("CPF / CNPJ *", { exact: true })
        .fill("00000000000000");
    await page.getByRole("button", { name: "Conferir e confirmar" }).click();
    await page.getByRole("button", { name: "Confirmar agora" }).dblclick();
    await expect(page.getByText(/Resposta FICTÍCIA/)).toBeVisible();
    await page
        .getByRole("button", { name: "Entrada e conferência", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Novo registro", exact: true })
        .click();
    await page.getByLabel("Referência *", { exact: true }).fill("DEMO-ENTRADA");
    await page.getByRole("button", { name: "Conferir e confirmar" }).click();
    await page.getByRole("button", { name: "Confirmar agora" }).click();
    await expect(page.getByText("DEMO-ENTRADA", { exact: true })).toBeVisible();
    await page.screenshot({
        path: "evidencias/marco02-r05-browser-desktop.png",
        fullPage: true,
    });
    await page
        .getByLabel("Perfil de apresentação fictício")
        .selectOption("OPERACAO");
    await page.getByRole("button", { name: "Cadastros", exact: true }).click();
    await expect(
        page.getByRole("button", { name: "Novo registro", exact: true }),
    ).toHaveCount(0);
});
test("Coletor 390px: teclado, UUID, leitura posição e movimento", async ({
    page,
}) => {
    await page.setViewportSize({ width: 390, height: 844 });
    await page.goto("/#coletor");
    await page
        .getByLabel("1. Leia o UUID da unidade")
        .fill("00000000-0000-4000-8000-000000000001");
    await page.getByLabel("1. Leia o UUID da unidade").press("Enter");
    await expect(page.getByText(/Posições na página: A101/)).toBeVisible();
    await expect(page.getByLabel("2. Leia o código da posição")).toBeFocused();
    await page.getByLabel("2. Leia o código da posição").fill("A101");
    await page.getByLabel("2. Leia o código da posição").press("Enter");
    await expect(page.getByText(/Destino lido: A101/)).toBeVisible();
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Movimento fictício pelo coletor");
    await page.getByRole("button", { name: "Conferir e confirmar" }).click();
    await page.getByRole("button", { name: "Confirmar agora" }).click();
    await expect(page.getByText(/Resposta FICTÍCIA/)).toBeVisible();
    await page.screenshot({
        path: "evidencias/marco02-r05-browser-coletor.png",
        fullPage: true,
    });
    expect(
        await page.evaluate(
            () => document.documentElement.scrollWidth <= innerWidth,
        ),
    ).toBe(true);
});
test("Conflito, vazio e indisponibilidade são distintos", async ({ page }) => {
    await page.goto("/#entrada");
    await page
        .getByLabel("Resposta do exercício fictício")
        .selectOption("conflito");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(page.getByRole("alert")).toContainText("CONFLITO_CONCORRENTE");
    await page
        .getByLabel("Resposta do exercício fictício")
        .selectOption("vazio");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(page.getByText("Nenhum registro encontrado.")).toBeVisible();
    await page
        .getByLabel("Resposta do exercício fictício")
        .selectOption("falha");
    await page.getByRole("button", { name: "Consultar", exact: true }).click();
    await expect(page.getByRole("alert")).toContainText("INDISPONIVEL");
    await expect(page.getByText("Nenhum registro encontrado.")).toHaveCount(0);
});
test("Navega todos módulos em 768px e 1440px sem erro de página", async ({
    page,
}) => {
    const errors: string[] = [];
    page.on("pageerror", (e) => errors.push(e.message));
    for (const width of [768, 1440]) {
        await page.setViewportSize({ width, height: 900 });
        await page.goto("/");
        for (const title of [
            "Cadastros",
            "Entrada e conferência",
            "Unidades e etiquetas",
            "Estoque e rastreabilidade",
            "Saída, FIFO e reserva",
            "Fiscal e retirada",
            "Serviços e tabelas",
            "Serviços e cálculo",
            "Fechamentos e ESL",
            "Contagem e carga inicial",
            "Contingência",
            "Consultas e relatórios",
            "Coletor",
            "Acesso e limites",
        ]) {
            await page
                .getByRole("button", { name: title, exact: true })
                .click();
            await expect(page.getByRole("heading", { level: 1 })).toHaveText(
                title,
            );
            expect(
                await page.evaluate(
                    () => document.documentElement.scrollWidth <= innerWidth,
                ),
            ).toBe(true);
        }
    }
    expect(errors).toEqual([]);
});
test("Teclado: página composta conserva foco ao navegar, voltar, avançar e trocar contexto", async ({
    page,
}) => {
    await page.goto("/");
    const entry = page.getByRole("button", {
        name: "Entrada e conferência",
        exact: true,
    });
    await entry.focus();
    await page.keyboard.press("Enter");
    await expect(page.locator("#conteudo")).toBeFocused();
    await expect(page.locator("#conteudo h1")).toHaveText(
        "Entrada e conferência",
    );
    await page.keyboard.press("Tab");
    expect(
        await page.evaluate(() =>
            document
                .getElementById("conteudo")
                ?.contains(document.activeElement),
        ),
    ).toBe(true);
    const outgoing = page.getByRole("button", {
        name: "Saída, FIFO e reserva",
        exact: true,
    });
    await outgoing.focus();
    await page.keyboard.press("Enter");
    await expect(page.locator("#conteudo")).toBeFocused();
    await page.goBack();
    await expect(page.locator("#conteudo h1")).toHaveText(
        "Entrada e conferência",
    );
    await expect(page.locator("#conteudo")).toBeFocused();
    await page.goForward();
    await expect(page.locator("#conteudo h1")).toHaveText(
        "Saída, FIFO e reserva",
    );
    await expect(page.locator("#conteudo")).toBeFocused();
    await page.getByLabel("Cliente fictício (ID)").fill("2");
    await page
        .getByRole("button", { name: "Aplicar contexto", exact: true })
        .focus();
    await page.keyboard.press("Enter");
    await expect(page.locator("#conteudo")).toBeFocused();
    await page.screenshot({
        path: "evidencias/marco02-r05-browser-foco-contexto.png",
        fullPage: true,
    });
});
