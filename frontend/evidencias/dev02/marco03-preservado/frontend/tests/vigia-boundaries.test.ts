import { it, expect } from "vitest";
import {
    absorb,
    emptyWorkflow,
    operationContext,
} from "../src/domain/workflow";
import {
    encode,
    parseResponse,
    stringifyExact,
    toWire,
} from "../src/contracts/codec";
import { fixture } from "../src/api/mock/fixtures";
import type { Values } from "../src/contracts/runtime";

it("VIG-WORKFLOW: mudar pedido não reutiliza reserva de outro pedido", () => {
    const oldOrder = {
        ...(fixture("PedidoSaidaDto.Detalhe") as Values),
        id: "601",
        reservas: [
            { ...(fixture("PedidoSaidaDto.Reserva") as Values), id: "701" },
        ],
    };
    let state = absorb(
        emptyWorkflow(),
        "PedidoSaidaDto.Detalhe",
        oldOrder,
        true,
    );
    const newOrder = {
        ...(fixture("PedidoSaidaDto.Detalhe") as Values),
        id: "602",
        reservas: [],
    };
    state = absorb(state, "PedidoSaidaDto.Detalhe", newOrder, true);
    const context = operationContext(
        "ExpedicaoController.ler",
        { clienteId: "1", armazemId: "1" },
        state,
    );
    expect(context.id).toBe("602");
    expect(context.reservaId).toBeUndefined();
});

it("VIG-VALIDACAO: XML de retirada em branco deve ser rejeitado antes de enviar", () => {
    const body = {
        operacaoId: "00000000-0000-4000-8000-000000000005",
        versao: "3",
        xmls: ["   "],
        remanescentes: [],
        motivo: "Retirada fictícia apenas para prova de validação",
        resolverPendentes: false,
    };
    expect(() => encode("ExpedicaoDto.Retirar", body)).toThrow();
});

it("VIG-DECODER: versão long obrigatória null é resposta incompatível", () => {
    const data = {
        ...(fixture("ClienteDto.Resposta") as Values),
        versao: null,
    };
    const raw = stringifyExact(toWire("ClienteDto.Resposta", data));
    expect(() => parseResponse("ClienteDto.Resposta", raw)).toThrow();
});
