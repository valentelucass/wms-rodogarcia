import { useState } from "react";
import { expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { Fields } from "../src/components/Fields";
import { records, type Values } from "../src/contracts/runtime";
import { receivingReferences } from "../src/modules/recebimento/references";
import { emptyWorkflow } from "../src/domain/workflow";
import { initialValue, encode } from "../src/contracts/codec";

function Form() {
    const [value, change] = useState<Values>({
        itemNotaId: "1",
        quantidadeBoa: "1488",
        quantidadeAvariada: "0",
        bobinas: null,
    });
    return (
        <>
            <Fields
                fields={records["RecebimentoDto.Item"]}
                values={value}
                onChange={change}
                perfil="GESTOR"
            />
            <output>{JSON.stringify(value)}</output>
        </>
    );
}
it("contagem fisica pode ser informada e removida sem alterar o peso", () => {
    render(<Form />);
    fireEvent.click(
        screen.getByRole("button", {
            name: "Informar Contagem física de bobinas",
        }),
    );
    const good = screen.getByLabelText(/Bobinas boas/);
    fireEvent.change(good, { target: { value: "3" } });
    expect(screen.getByRole("status")).toHaveTextContent(
        '"quantidadeBoa":"1488"',
    );
    expect(screen.getByRole("status")).toHaveTextContent('"boas":"3"');
    fireEvent.click(
        screen.getByRole("button", {
            name: "Remover Contagem física de bobinas",
        }),
    );
    expect(screen.getByRole("status")).toHaveTextContent('"bobinas":null');
});
it("lote opcional apagado continua ausente no comando de chegada", () => {
    const initial = initialValue("RecebimentoDto.Item") as Values;
    expect(initial.lote).toBeNull();
    render(<Form />);
    const lote = screen.getByLabelText("Lote");
    fireEvent.change(lote, { target: { value: "LOTE-TESTE" } });
    fireEvent.change(lote, { target: { value: "" } });
    const value = JSON.parse(screen.getByRole("status").textContent!);
    expect(JSON.parse(encode("RecebimentoDto.Item", value)).lote).toBeNull();
});
it("distribuicao sugere o numero contado mas nunca inventa pesos individuais", () => {
    const state = emptyWorkflow();
    state.selected["PedidoEntradaDto.Resumo"] = { id: "82", versao: "4" };
    state.selected["RecebimentoDto.Entrada"] = {
        id: "101",
        produtoId: "80",
        bobinas: { boas: 2, avariadas: 1 },
    };
    state.owners["RecebimentoDto.Entrada:101"] = {
        root: "PedidoEntradaDto.Resumo",
        id: "82",
    };
    const context: Values = {};
    receivingReferences(
        context,
        "UnidadeLogisticaController",
        "unitizar",
        state,
    );
    expect(context.unidades).toEqual([
        { embalagemId: "", tipo: "BOBINA", condicao: "BOA", quantidade: "" },
        { embalagemId: "", tipo: "BOBINA", condicao: "BOA", quantidade: "" },
        {
            embalagemId: "",
            tipo: "BOBINA",
            condicao: "AVARIADA",
            quantidade: "",
        },
    ]);
});

it("item com regra confirmada sugere uma bobina e seu peso sem inventar lote", () => {
    const state = emptyWorkflow();
    state.selected["PedidoEntradaDto.Resumo"] = { id: "82", versao: "4" };
    state.selected["PedidoEntradaDto.ItemConferencia"] = {
        id: "91",
        prevista: "1848.000000",
        recebidaBoa: "0",
        recebidaAvariada: "0",
        bobinasPrevistas: 1,
    };
    state.owners["PedidoEntradaDto.ItemConferencia:91"] = {
        root: "PedidoEntradaDto.Resumo",
        id: "82",
    };
    const context: Values = {};
    receivingReferences(context, "PedidoEntradaController", "chegada", state);
    expect(context.itens).toEqual([
        {
            itemNotaId: "91",
            lote: null,
            validade: null,
            quantidadeBoa: "1848.000000",
            quantidadeAvariada: "0",
            bobinas: { boas: 1, avariadas: 0 },
        },
    ]);
    state.selected["PedidoEntradaDto.ItemConferencia"].recebidaBoa = "1848";
    receivingReferences(context, "PedidoEntradaController", "chegada", state);
    expect((context.itens as Values[])[0]).toMatchObject({
        quantidadeBoa: "",
        bobinas: null,
    });
});

it("bobina unica recebe exatamente o peso fisico conferido", () => {
    const state = emptyWorkflow();
    state.selected["PedidoEntradaDto.Resumo"] = { id: "82", versao: "4" };
    state.selected["RecebimentoDto.Entrada"] = {
        id: "101",
        produtoId: "80",
        quantidadeTriagem: "0",
        quantidadeQuarentena: "1826.123456",
        bobinas: { boas: 0, avariadas: 1 },
    };
    state.owners["RecebimentoDto.Entrada:101"] = {
        root: "PedidoEntradaDto.Resumo",
        id: "82",
    };
    const context: Values = {};
    receivingReferences(
        context,
        "UnidadeLogisticaController",
        "unitizar",
        state,
    );
    expect(context.unidades).toEqual([
        {
            embalagemId: "",
            tipo: "BOBINA",
            condicao: "AVARIADA",
            quantidade: "1826.123456",
        },
    ]);
});
