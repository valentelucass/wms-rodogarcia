import { it, expect } from "vitest";
import {
    absorb,
    emptyWorkflow,
    operationContext,
} from "../src/domain/workflow";
const code = "00000000-0000-4000-8000-000000000001";
function selected(labelCode = code) {
    let workflow = absorb(emptyWorkflow(), "PedidoSaidaDto.Detalhe", {
        id: "601",
        versao: "5",
        itens: [],
        reservas: [{ id: "701", unidadeId: "501", codigoUnidade: code }],
    });
    workflow = absorb(workflow, "UnidadeLogisticaDto.Etiqueta", {
        codigoLeitura: labelCode,
        versaoConteudo: "11",
    });
    return workflow;
}
it("etiqueta da reserva atual transfere UUID ao coletor sem usar revisão11 como versão da unidade", () => {
    const context = operationContext(
        "EstoqueController.posicionar",
        {},
        selected(),
    );
    expect(context.codigo).toBe(code);
    expect(context.versaoUnidade).toBeUndefined();
});
it("etiqueta de outro UUID não preenche código da reserva selecionada", () => {
    const context = operationContext(
        "EstoqueController.posicionar",
        {},
        selected("00000000-0000-4000-8000-000000000002"),
    );
    expect(context.codigo).toBeUndefined();
});
it("troca do pedido remove vínculo e etiqueta anterior não restaura contexto no coletor", () => {
    const workflow = absorb(selected(), "PedidoSaidaDto.Detalhe", {
        id: "602",
        versao: "0",
        itens: [],
        reservas: [],
    });
    expect(
        operationContext("EstoqueController.posicionar", {}, workflow).codigo,
    ).toBeUndefined();
});
