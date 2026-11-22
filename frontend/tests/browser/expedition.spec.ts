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
    await page.goto("/#fiscal");
    await page.getByRole("button", { name: "DEMO-01", exact: true }).click();
    await expect(
        page.getByText("Retirada física ainda não confirmada."),
    ).toBeVisible();
    await page.getByText("Documentos já registrados", { exact: true }).click();
    await page
        .getByRole("button", { name: "Registrar nota existente" })
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
    await page
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    await expect(
        page.getByText("Retirada física ainda não confirmada."),
    ).toBeVisible();
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
            name: "Confirmar retirada física",
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
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    await expect(page.getByText(/Retirada registrada em/)).toBeVisible();
    const physical = command({
        wire: (await recorded(page, "ExpedicaoController.consultar", "receipt"))
            .raw!,
    });
    expect(physical.retiradaEm).toBe("2026-10-08T12:30:00.123456Z");
    expect(
        exact(
            (physical.fatos as Record<string, unknown>[])[0].quantidadeDepois,
        ),
    ).toBe("490.000000");
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
        .getByRole("button", { name: "Ver detalhes", exact: true })
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
    await page.getByRole("button", { name: "DEMO-01", exact: true }).click();
    await page.getByText("Retorno e devolução", { exact: true }).click();
    await page
        .getByRole("button", { name: "Registrar chegada da devolução" })
        .click();
    await page
        .getByLabel("Chegada Real *", { exact: true })
        .fill("2026-10-08T10:00:00.123");
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
    const returnedReceipt = command({
        wire: (await recorded(page, "ExpedicaoController.devolver", "receipt"))
            .raw!,
    });
    expect(exact(returnedReceipt.pedidoEntradaId)).toBe("102");
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
    await expect(
        page.getByRole("dialog").getByText("DEV-102", { exact: true }).first(),
    ).toBeVisible();
    const detailId = "PedidoEntradaController.consultar";
    const request = await recorded(
        page,
        detailId,
        "request",
        (event) => event.params?.id === "102",
    );
    const receipt = await recorded(page, detailId, "receipt", (event) => {
        const detail = command({ wire: event.raw });
        return (
            exact((detail.pedido as Record<string, unknown>).id) ===
            request.params?.id
        );
    });
    const returned = command({ wire: receipt.raw });
    const returnedPedido = returned.pedido as Record<string, unknown>;
    expect(returnedPedido.situacao).toBe("EFETIVADO");
    expect(returnedPedido.efetivadoEm).toBe("2026-10-08T13:01:00.123456Z");
    await page.getByRole("button", { name: "Histórico", exact: true }).click();
    await page
        .getByRole("button", { name: "Consultar entradas conferidas" })
        .click();
    await consult(page);
    expect(
        (await recorded(page, "PedidoEntradaController.entradas")).params?.id,
    ).toBe("102");
    const entries = command({
        wire: (
            await recorded(page, "PedidoEntradaController.entradas", "receipt")
        ).raw!,
    });
    const entry = (entries.itens as Record<string, unknown>[])[0];
    expect(entry.chegadaReal).toBe("2026-10-08T13:00:00.123Z");
    expect(entry.efetivadaEm).toBe("2026-10-08T13:01:00.123456Z");
    expect(exact(entry.id)).toBe("202");
    expect(entry.dataFifo).toBe("2026-10-07T12:00:00.123456Z");
});
test("FE10 documento simbolico cancelamento e retorno interno distintos", async ({
    page,
}) => {
    await separated(page);
    await document(page, true);
    await page
        .getByRole("button", { name: "Registrar nota existente" })
        .click();
    await page
        .locator("summary")
        .filter({ hasText: "Outras operações" })
        .click();
    await page.getByRole("button", { name: /cancelamento/i }).click();
    await expect(page.getByLabel("Documento *", { exact: true })).toHaveValue(
        "1601",
    );
    await page
        .getByLabel("Motivo / justificativa *", { exact: true })
        .fill("Cancelamento fiscal fictício antes de retorno");
    await confirm(page);
    const c = await recorded(page, "ExpedicaoController.cancelar");
    expect(c.params?.documentoId).toBe("1601");
    expect(exact(command(c).versao)).toBe("4");
    await page
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    await expect(
        page.getByText("Retirada física ainda não confirmada."),
    ).toBeVisible();
    await expect(page.getByText("Cancelado", { exact: true })).toBeVisible();
    await page.getByText("Retorno e devolução", { exact: true }).click();
    await page
        .getByRole("button", { name: "Reendereçar mercadoria separada" })
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
        .getByRole("button", { name: "Voltar à lista", exact: true })
        .click();
    await expect
        .poll(
            async () =>
                (
                    await recorded(
                        page,
                        "ExpedicaoController.consultar",
                        "receipt",
                    )
                ).raw,
        )
        .toContain("RETORNO_INTERNO");
});
