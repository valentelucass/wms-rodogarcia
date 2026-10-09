import { describe, it, expect } from "vitest";
import {
    absorb,
    emptyWorkflow,
    referenceCatalog,
    ownedSelection,
    operationContext,
} from "../src/domain/workflow";
import { endpoint } from "../src/contracts/runtime";
const reserve = {
    id: "701",
    itemId: "611",
    unidadeId: "501",
    notaOrigemId: "301",
    codigoUnidade: "00000000-0000-4000-8000-000000000001",
};
const order = (versao: string, reservas: unknown[]) => ({
    id: "601",
    versao,
    itens: [],
    reservas,
});
describe("FE-VIG-002: catálogo autoritativo e consulta parcial", () => {
    it("mesmo pedido nova revisão remove reserva do catálogo e impede reseleção antiga", () => {
        let s = absorb(
            emptyWorkflow(),
            "PedidoSaidaDto.Detalhe",
            order("1", [reserve]),
        );
        expect(referenceCatalog(s, "PedidoSaidaDto.Reserva")).toEqual([
            reserve,
        ]);
        s = absorb(s, "PedidoSaidaDto.Detalhe", order("2", []));
        expect(referenceCatalog(s, "PedidoSaidaDto.Reserva")).toEqual([]);
        s = absorb(s, "PedidoSaidaDto.Reserva", reserve, true);
        expect(
            ownedSelection(
                s,
                "PedidoSaidaDto.Reserva",
                "PedidoSaidaDto.Detalhe",
            ),
        ).toBeUndefined();
        expect(
            operationContext("ExpedicaoController.ler", {}, s),
        ).toMatchObject({ id: "601", versao: "2" });
        expect(
            operationContext("ExpedicaoController.ler", {}, s).reservaId,
        ).toBeUndefined();
    });
    it("mesma revisão também respeita retirada autoritativa; objeto antigo não substitui filho atualizado", () => {
        const updated = { ...reserve, quantidade: "6.000000" };
        let s = absorb(
            emptyWorkflow(),
            "PedidoSaidaDto.Detalhe",
            order("1", [reserve]),
        );
        s = absorb(s, "PedidoSaidaDto.Detalhe", order("1", [updated]));
        s = absorb(s, "PedidoSaidaDto.Reserva", reserve, true);
        expect(referenceCatalog(s, "PedidoSaidaDto.Reserva")).toEqual([
            updated,
        ]);
        expect(s.selected["PedidoSaidaDto.Reserva"]).toBe(updated);
        s = absorb(s, "PedidoSaidaDto.Detalhe", order("1", []));
        expect(referenceCatalog(s, "PedidoSaidaDto.Reserva")).toEqual([]);
    });
    it("consulta parcial de filho legítimo conserva irmãos e vínculos do pai", () => {
        const second = { ...reserve, id: "702", unidadeId: "502" };
        let s = absorb(
            emptyWorkflow(),
            "PedidoSaidaDto.Detalhe",
            order("2", [reserve, second]),
        );
        s = absorb(s, "PedidoSaidaDto.Reserva", second, false, {
            endpoint: endpoint("ExpedicaoController.consultar"),
            params: { id: "601" },
            query: {},
        });
        expect(referenceCatalog(s, "PedidoSaidaDto.Reserva")).toEqual([
            reserve,
            second,
        ]);
        s = absorb(s, "PedidoSaidaDto.Reserva", second, true);
        expect(
            operationContext("ExpedicaoController.ler", {}, s),
        ).toMatchObject({ id: "601", versao: "2", reservaId: "702" });
    });
    it("Resumo parcial de recebimento mantém nota/item; Detalhe notas[] os remove", () => {
        const item = { id: "401" },
            note = { id: "301", itens: [item] };
        let s = absorb(emptyWorkflow(), "PedidoEntradaDto.Detalhe", {
            pedido: { id: "101", versao: "1" },
            notas: [note],
        });
        s = absorb(s, "PedidoEntradaDto.Resumo", { id: "101", versao: "2" });
        expect(referenceCatalog(s, "PedidoEntradaDto.ItemConferencia")).toEqual(
            [item],
        );
        s = absorb(s, "PedidoEntradaDto.Detalhe", {
            pedido: { id: "101", versao: "3" },
            notas: [],
        });
        expect(referenceCatalog(s, "PedidoEntradaDto.Nota")).toEqual([]);
        expect(referenceCatalog(s, "PedidoEntradaDto.ItemConferencia")).toEqual(
            [],
        );
        s = absorb(s, "PedidoEntradaDto.ItemConferencia", item, true);
        expect(
            operationContext("PedidoEntradaController.chegada", {}, s).itens,
        ).toBeUndefined();
    });
});
