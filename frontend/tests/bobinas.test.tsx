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
