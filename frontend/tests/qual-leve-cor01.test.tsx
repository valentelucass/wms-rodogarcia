import { useState } from "react";
import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it } from "vitest";
import { InformationHint } from "../src/components/information/InformationPopup";
import { ReceivingSummary } from "../src/modules/recebimento/ReceivingSummary";
import { ReceivingDetail } from "../src/modules/recebimento/ReceivingDetail";
import { Fields } from "../src/components/Fields";
import { FormReferences } from "../src/components/FormReferences";
import { parseResponse, encode } from "../src/contracts/codec";
import { records, type Values } from "../src/contracts/runtime";
import { emptyWorkflow } from "../src/domain/workflow";
import type { Transport } from "../src/api/client";

// Dados de resposta escritos à mão: os oráculos não vêm do catálogo,
// de fixture(), toWire(), schemas ou da unidade comercial do XML.
const itemKg = {
    id: "91",
    numeroItem: 1,
    produtoId: "80",
    sku: "000600366",
    unidadeMedida: "KG",
    prevista: "12.000000",
    recebidaBoa: "11.000000",
    recebidaAvariada: "1.000000",
    diferenca: "0.000000",
    valorMercadoria: null,
    bobinasPrevistas: 1,
};
const itemUn = {
    ...itemKg,
    id: "92",
    numeroItem: 2,
    produtoId: "81",
    sku: "SKU-UN",
    unidadeMedida: "UN",
    prevista: "3",
    recebidaBoa: "2",
    recebidaAvariada: "0",
    diferenca: "-1",
    bobinasPrevistas: null,
};
const pedido = {
    id: "82",
    referencia: "ENT-000082",
    versao: "4",
    situacao: "EM_CONFERENCIA",
};
const nota = {
    id: "71",
    serie: 1,
    numero: "32332",
    xmlVinculado: false,
    itens: [itemKg, itemUn],
};
const detalhe = { pedido, notas: [nota], divergente: true };
function workflow() {
    const state = emptyWorkflow();
    state.selected["PedidoEntradaDto.Resumo"] = pedido;
    state.selected["PedidoEntradaDto.Detalhe"] = detalhe;
    return state;
}
const offline: Transport = {
    send: async () => {
        throw new Error("Transporte real não autorizado neste teste.");
    },
};

it("COR01 popup informa literalmente uma unidade por posição sem usar autocatalogo", () => {
    render(<InformationHint topic="occupied" />);
    act(() => screen.getByRole("button").focus());
    const tip = screen.getByRole("tooltip");
    expect(tip).toHaveTextContent(
        "Uma posição recebe uma única unidade logística.",
    );
    expect(tip).toHaveTextContent(
        "Uma unidade pode ocupar várias posições conforme a regra de capacidade.",
    );
    expect(tip).not.toHaveTextContent(
        "Uma posição pode conter mais de uma unidade",
    );
});

it.each(["resumo", "detalhe"] as const)(
    "COR01 %s identifica KG convertido de TO e UN por linha sem mudar quantidades",
    (consumer) => {
        // A resposta do backend já converteu 0,012 TO para 12 KG.
        // O frontend deve apresentar estes lexemas e a unidade DO PRODUTO.
        if (consumer === "resumo")
            render(<ReceivingSummary workflow={workflow()} />);
        else
            render(
                <ReceivingDetail
                    record={detalhe}
                    section="Notas e itens"
                    onSection={() => {}}
                    onSelect={() => {}}
                    locked={false}
                    workflow={workflow()}
                    transport={offline}
                />,
            );
        const table = screen.getByRole("table");
        expect(
            within(table).getByRole("columnheader", {
                name: "Unidade do produto",
            }),
        ).toBeVisible();
        const kgRow = within(table)
            .getAllByRole("row")
            .find((row) => row.textContent?.includes("000600366"))!;
        const unRow = within(table)
            .getAllByRole("row")
            .find((row) => row.textContent?.includes("SKU-UN"))!;
        expect(
            within(kgRow).getByRole("cell", { name: "KG" }),
        ).toBeVisible();
        expect(
            within(kgRow).getByRole("cell", { name: "12.000000" }),
        ).toBeVisible();
        expect(
            within(kgRow).getByRole("cell", { name: "11.000000" }),
        ).toBeVisible();
        expect(
            within(unRow).getByRole("cell", { name: "UN" }),
        ).toBeVisible();
        expect(
            within(unRow).getByRole("cell", { name: "-1" }),
        ).toBeVisible();
        expect(table).not.toHaveTextContent("0.012");
        expect(table).not.toHaveTextContent(" TO");
    },
);

function PhysicalForm({ consulted = true }: { consulted?: boolean }) {
    const [value, setValue] = useState<Values>({
        itemNotaId: "91",
        lote: null,
        validade: null,
        quantidadeBoa: "12.000000",
        quantidadeAvariada: "0",
        bobinas: null,
    });
    return (
        <FormReferences.Provider
            value={{
                defaults: {},
                options: {
                    itemNotaId: [
                        { value: "91", title: "Bobina" },
                        { value: "92", title: "Peça" },
                    ],
                },
                records: {
                    "PedidoEntradaDto.ItemConferencia": consulted
                        ? [itemKg, itemUn]
                        : [],
                },
            }}
        >
            <Fields
                fields={[
                    {
                        name: "item",
                        type: "RecebimentoDto.Item",
                        required: true,
                    },
                ]}
                values={{ item: value }}
                onChange={(next) => setValue(next.item as Values)}
                perfil="GESTOR"
            />
            <output>{encode("RecebimentoDto.Item", value)}</output>
        </FormReferences.Provider>
    );
}
it("COR01 formulario orienta quantidade fisica em KG e acompanha troca para UN sem alterar peso", () => {
    render(<PhysicalForm />);
    expect(screen.getByText("Unidade do produto: KG.")).toBeVisible();
    expect(
        screen.getByText(
            /Informe a quantidade física na unidade do produto cadastrado/,
        ),
    ).toHaveTextContent(
        "Não use a unidade comercial do XML nem a contagem de bobinas.",
    );
    fireEvent.change(screen.getByRole("combobox", { name: /Item da nota/ }), {
        target: { value: "92" },
    });
    expect(screen.getByText("Unidade do produto: UN.")).toBeVisible();
    expect(
        screen.queryByText("Unidade do produto: KG."),
    ).not.toBeInTheDocument();
    const raw = screen.getByRole("status").textContent!;
    expect(raw).toContain('"quantidadeBoa":12.000000');
    expect(raw).not.toContain("unidadeMedida");
});
it("COR01 formulario sem item consultado pede consulta e nunca presume KG pelo XML", () => {
    render(<PhysicalForm consulted={false} />);
    expect(
        screen.getByText(
            "Consulte o item da nota para identificar a unidade do produto.",
        ),
    ).toBeVisible();
    expect(
        screen.queryByText("Unidade do produto: KG."),
    ).not.toBeInTheDocument();
});

const rawKg =
    '{"id":91,"numeroItem":1,"produtoId":80,"sku":"000600366","unidadeMedida":"KG","prevista":12.000000,"recebidaBoa":11.000000,"recebidaAvariada":1.000000,"diferenca":0.000000,"valorMercadoria":null,"bobinasPrevistas":1}';
it("COR01 decoder preserva unidade obrigatoria do produto apos sku e lexemas convertidos", () => {
    const decoded = parseResponse(
        "PedidoEntradaDto.ItemConferencia",
        rawKg,
    ) as Values;
    expect(decoded).toMatchObject({
        unidadeMedida: "KG",
        prevista: "12.000000",
        recebidaAvariada: "1.000000",
    });
    const fields = records["PedidoEntradaDto.ItemConferencia"];
    const skuIndex = fields.findIndex((f) => f.name === "sku");
    expect(fields[skuIndex + 1]).toMatchObject({
        name: "unidadeMedida",
        type: "String",
        required: true,
    });
});
it.each([
    ["ausente", rawKg.replace('"unidadeMedida":"KG",', "")],
    ["nula", rawKg.replace('"unidadeMedida":"KG"', '"unidadeMedida":null')],
])("COR01 decoder recusa unidade do produto %s sem fallback", (_name, raw) => {
    expect(() =>
        parseResponse("PedidoEntradaDto.ItemConferencia", raw),
    ).toThrow();
});
