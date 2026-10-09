import {
    test,
    expect,
    confirm,
    consult,
    recorded,
    command,
    exact,
} from "./support";
import type { Page } from "@playwright/test";
async function separated(page: Page) {
    await page.goto("/#saida");
    await consult(page);
    await page
        .getByRole("button", { name: "Selecionar registro 1", exact: true })
        .first()
        .click();
    await page
        .getByRole("button", { name: "Fiscal e retirada", exact: true })
        .click();
    await consult(page);
    await expect(
        page.getByRole("region", {
            name: "Fiscal e físico do pedido selecionado",
        }),
    ).toContainText("Retirada física: não registrada");
    await page
        .getByRole("button", {
            name: "Registrar documento existente",
            exact: true,
        })
        .click();
}
async function document(page: Page, symbolic = false) {
    await page.getByLabel("Origem *", { exact: true }).selectOption("NOTAZZ");
    await page
        .getByLabel("Natureza *", { exact: true })
        .selectOption(symbolic ? "RETORNO_SIMBOLICO" : "RETORNO_MERCADORIA");
    await page
        .getByLabel("Protocolo existente NOTAZZ *", { exact: true })
        .fill("DOC-FICTICIO-601");
    await page
        .getByRole("button", { name: "Informar Nota", exact: true })
        .click();
    await page
        .getByLabel("Nota / Emitente Cnpj *", { exact: true })
        .fill("12345678000190");
    await page.getByLabel("Nota / Serie *", { exact: true }).fill("1");
    await page.getByLabel("Nota / Numero *", { exact: true }).fill("456");
    await page
        .getByLabel("Nota / Emissao *", { exact: true })
        .fill("2026-10-08");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Documento fictício existente conferido");
    await confirm(page);
    const c = command(await recorded(page, "ExpedicaoController.documento"));
    const coverage = (c.coberturas as Record<string, unknown>[])[0];
    expect(exact(coverage.reservaId)).toBe("701");
    expect(exact(coverage.notaOrigemId)).toBe("301");
    expect(coverage.sku).toBeTruthy();
    expect(exact(coverage.quantidade)).toBe("10.000000");
    await expect(
        page.getByRole("region", {
            name: "Fiscal e físico do pedido selecionado",
        }),
    ).toContainText("Retirada física: não registrada");
}
async function destination(page: Page, field: string) {
    await page
        .getByRole("button", {
            name: "Consultar posições de retorno ou remanescente",
            exact: true,
        })
        .click();
    await page
        .getByRole("region", { name: "Consultar referências para a tarefa" })
        .getByRole("button", { name: "Selecionar registro 1", exact: true })
        .click();
    await page
        .getByRole("button", { name: "Adicionar " + field, exact: true })
        .click();
}
test("FE10 retirada integral e devolucao ligada a baixa", async ({ page }) => {
    await separated(page);
    await document(page);
    await page
        .getByRole("button", {
            name: "Confirmar retirada física com comprovantes",
            exact: true,
        })
        .click();
    await destination(page, "Destinos dos remanescentes");
    await page
        .getByRole("button", {
            name: "Adicionar XMLs comprovantes da retirada",
            exact: true,
        })
        .click();
    await page
        .getByLabel("XMLs comprovantes da retirada / 1 *", {
            exact: true,
        })
        .fill("<nfe>comprovante ficticio</nfe>");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Retirada física fictícia integral conferida");
    await confirm(page);
    const r = await recorded(page, "ExpedicaoController.retirar"),
        c = command(r);
    expect(r.params?.id).toBe("601");
    expect(exact(c.versao)).toBe("4");
    const rem = (c.remanescentes as Record<string, unknown>[])[0];
    expect(exact(rem.reservaId)).toBe("701");
    expect(
        exact((rem.destinos as Record<string, unknown>[])[0].enderecoId),
    ).toBe("81");
    await page
        .getByRole("button", {
            name: "Conferir retirada física, baixas e documentos",
            exact: true,
        })
        .click();
    await consult(page);
    await expect(
        page.getByRole("region", {
            name: "Fiscal e físico do pedido selecionado",
        }),
    ).toContainText("2026-10-08T12:30:00.123456Z");
    await expect(
        page.getByRole("cell", { name: "490.000000", exact: true }).first(),
    ).toBeVisible();
    // A nova revisão é recebida da etiqueta após a retirada parcial do pallet.
    await page
        .getByRole("button", { name: "Unidades e etiquetas", exact: true })
        .click();
    await page
        .getByRole("button", {
            name: "3. Ler e reimprimir etiqueta",
            exact: true,
        })
        .click();
    await page
        .getByRole("button", {
            name: "Consultar / reimprimir etiqueta",
            exact: true,
        })
        .click();
    await expect(page.getByLabel("Código *", { exact: true })).toHaveValue(
        "00000000-0000-4000-8000-000000000001",
    );
    await consult(page);
    const label = command({
        wire: (
            await recorded(
                page,
                "UnidadeLogisticaController.etiqueta",
                "receipt",
            )
        ).raw!,
    });
    expect(label.codigoLeitura).toBe("00000000-0000-4000-8000-000000000001");
    expect(exact(label.versaoConteudo)).toBe("11");
    expect(exact(label.quantidadeProduto)).toBe("490.000000");
    await page
        .getByRole("button", {
            name: "Endereçar esta unidade no coletor",
            exact: true,
        })
        .click();
    await expect(page.getByLabel("1. Leia o UUID da unidade")).toHaveValue(
        "00000000-0000-4000-8000-000000000001",
    );
    await page.getByLabel("1. Leia o UUID da unidade").press("Enter");
    await page.getByLabel("2. Leia o código da posição").fill("A101");
    await page.getByLabel("2. Leia o código da posição").press("Enter");
    await expect(
        page.getByLabel("Revisão atual da unidade *", { exact: true }),
    ).toHaveValue("8");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Remanescente fictício consultado após etiqueta atual");
    await confirm(page);
    const reposition = await recorded(page, "EstoqueController.posicionar");
    expect(exact(command(reposition).versaoUnidade)).toBe("8");
    expect(reposition.params?.codigo).toBe(label.codigoLeitura);
    await page.waitForFunction(() =>
        (
            window as unknown as {
                __wmsOperationalTrace: { id: string; kind: string }[];
            }
        ).__wmsOperationalTrace.some(
            (event) =>
                event.id === "EstoqueController.posicionar" &&
                event.kind === "receipt",
        ),
    );
    const positioned = command({
        wire: (await recorded(page, "EstoqueController.posicionar", "receipt"))
            .raw!,
    });
    const positionedUnit = (positioned.estoque as Record<string, unknown>)
        .unidade as Record<string, unknown>;
    expect(exact(positionedUnit.versao)).toBe("9");
    await page
        .getByRole("button", { name: "Fiscal e retirada", exact: true })
        .click();
    await consult(page);
    await page
        .getByRole("button", { name: "3. Retorno e devolução", exact: true })
        .click();
    await page
        .getByRole("button", {
            name: "Registrar devolução / nova entrada",
            exact: true,
        })
        .click();
    await page
        .getByLabel("Referência *", { exact: true })
        .fill("DEV-FICTICIA-601");
    await page
        .getByLabel("Chegada Real *", { exact: true })
        .fill("2026-10-08T13:00:00.123456Z");
    await page
        .getByLabel("Nota / Emitente Cnpj *", { exact: true })
        .fill("12345678000190");
    await page.getByLabel("Nota / Serie *", { exact: true }).fill("1");
    await page.getByLabel("Nota / Numero *", { exact: true }).fill("456");
    await page
        .getByLabel("Nota / Emissao *", { exact: true })
        .fill("2026-10-08");
    await page
        .getByLabel("Itens / Item 1 / Quantidade *", { exact: true })
        .fill("2.000000");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Devolução fictícia ligada à baixa original");
    await confirm(page);
    const d = command(await recorded(page, "ExpedicaoController.devolver"));
    expect(exact((d.itens as Record<string, unknown>[])[0].baixaId)).toBe(
        "1701",
    );
    expect(exact(d.versao)).toBe("5");
    await expect(
        page.getByRole("table", { name: /Devoluções ligadas às baixas/ }),
    ).toContainText("202");
    await page
        .getByRole("button", {
            name: "Consultar a nova entrada ligada à devolução",
            exact: true,
        })
        .click();
    await expect(
        page.getByLabel("Identificador *", { exact: true }),
    ).toHaveValue("102");
    await consult(page);
    expect(
        (await recorded(page, "PedidoEntradaController.consultar")).params?.id,
    ).toBe("102");
    await expect(
        page.getByText("DEV-FICTICIA-601", { exact: true }).first(),
    ).toBeVisible();
    await expect(
        page.getByText("EFETIVADO", { exact: true }).first(),
    ).toBeVisible();
    const returned = command({
        wire: (
            await recorded(page, "PedidoEntradaController.consultar", "receipt")
        ).raw!,
    });
    const returnedPedido = returned.pedido as Record<string, unknown>;
    expect(returnedPedido.efetivadoEm).toBe("2026-10-08T13:01:00.123456Z");
    await page
        .getByRole("button", {
            name: "4. Divergência e efetivação",
            exact: true,
        })
        .click();
    await page
        .getByRole("button", {
            name: "Consultar entradas conferidas",
            exact: true,
        })
        .click();
    await consult(page);
    expect(
        (await recorded(page, "PedidoEntradaController.entradas")).params?.id,
    ).toBe("102");
    await expect(
        page.getByRole("cell", { name: "202", exact: true }).first(),
    ).toBeVisible();
    const entries = command({
        wire: (
            await recorded(page, "PedidoEntradaController.entradas", "receipt")
        ).raw!,
    });
    const entry = (entries.itens as Record<string, unknown>[])[0];
    expect(entry.chegadaReal).toBe("2026-10-08T13:00:00.123456Z");
    expect(entry.efetivadaEm).toBe("2026-10-08T13:01:00.123456Z");
    await expect(
        page
            .getByRole("cell", {
                name: "2026-10-07T12:00:00.123456Z",
                exact: true,
            })
            .first(),
    ).toBeVisible();
});
test("FE10 documento simbolico cancelamento e retorno interno distintos", async ({
    page,
}) => {
    await separated(page);
    await document(page, true);
    await page
        .getByRole("button", {
            name: "1. Documento de mercadoria",
            exact: true,
        })
        .click();
    await page
        .getByRole("button", { name: "Registrar cancelamento", exact: true })
        .click();
    await expect(
        page.getByLabel("Documento Id *", { exact: true }),
    ).toHaveValue("1601");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Cancelamento fiscal fictício antes de retorno");
    await confirm(page);
    const c = await recorded(page, "ExpedicaoController.cancelar");
    expect(c.params?.documentoId).toBe("1601");
    expect(exact(command(c).versao)).toBe("4");
    await expect(
        page.getByRole("region", {
            name: "Fiscal e físico do pedido selecionado",
        }),
    ).toContainText("Retirada física: não registrada");
    await expect(
        page.getByRole("region", {
            name: "Fiscal e físico do pedido selecionado",
        }),
    ).toContainText("CANCELADO");
    await page
        .getByRole("button", {
            name: "Conferir destinos para retorno interno (ação física distinta)",
            exact: true,
        })
        .click();
    await destination(page, "Unidades");
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Retorno físico interno fictício conferido");
    await confirm(page);
    const r = await recorded(page, "ExpedicaoController.retornar");
    expect(r.params?.id).toBe("601");
    expect(exact(command(r).versao)).toBe("5");
    expect(
        exact((command(r).unidades as Record<string, unknown>[])[0].reservaId),
    ).toBe("701");
    await page
        .getByRole("button", {
            name: "Conferir fatos do retorno interno",
            exact: true,
        })
        .click();
    await consult(page);
    await expect(
        page.getByRole("cell", { name: "RETORNO_INTERNO", exact: true }),
    ).toBeVisible();
});
