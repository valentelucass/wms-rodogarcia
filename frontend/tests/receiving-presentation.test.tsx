import { useState } from "react";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it } from "vitest";
import { ScalarField } from "../src/components/fields/ScalarField";
import { ReceivingSummary } from "../src/modules/recebimento/ReceivingSummary";
import { ReceivingDetail } from "../src/modules/recebimento/ReceivingDetail";
import { emptyWorkflow } from "../src/domain/workflow";
import type { Transport } from "../src/api/client";

function Arrival({ initial = "", disabled = false }) {
    const [value, setValue] = useState<unknown>(initial);
    return (
        <>
            <ScalarField
                f={{ name: "chegouEm", type: "Instant", required: true }}
                value={value}
                path="chegouEm"
                onChange={setValue}
                disabled={disabled}
            />
            <output aria-label="Instante enviado">{String(value)}</output>
        </>
    );
}

it("calendário registra data e horário de Brasília como instante com fuso", () => {
    render(<Arrival />);
    const input = screen.getByLabelText(/Chegada física real/);
    expect(input).toHaveAttribute("type", "datetime-local");
    expect(input).toBeRequired();
    expect(input).toHaveAccessibleDescription(/Horário de Brasília/);
    fireEvent.change(input, { target: { value: "2026-10-13T08:45" } });
    expect(screen.getByLabelText("Instante enviado")).toHaveTextContent(
        "2026-10-13T11:45:00Z",
    );
    fireEvent.change(input, { target: { value: "" } });
    expect(screen.getByLabelText("Instante enviado")).toBeEmptyDOMElement();
});

it("calendário mostra instante existente em Brasília sem alterar precisão ou campo bloqueado", () => {
    render(<Arrival initial="2026-10-13T11:45:00.123456Z" disabled />);
    const input = screen.getByLabelText(/Chegada física real/);
    expect(input).toBeDisabled();
    expect(input).toHaveValue("2026-10-13T08:45:00.123");
    expect(screen.getByLabelText("Instante enviado")).toHaveTextContent(
        "2026-10-13T11:45:00.123456Z",
    );
});

const pedido = { id: "91", referencia: "ENT-000091", versao: "2" };
const kg = {
    id: "1",
    sku: "000600366",
    unidadeMedida: "KG",
    prevista: "1820.000000",
    recebidaBoa: "0.000000",
    recebidaAvariada: "0.125000",
    diferenca: "-1819.875000",
};
const un = {
    ...kg,
    id: "2",
    sku: "SKU-UN",
    unidadeMedida: "UN",
    prevista: "9007199254740993",
    recebidaBoa: "0",
    recebidaAvariada: "0",
    diferenca: "-9007199254740993",
};
const detalhe = {
    pedido,
    notas: [{ id: "1", serie: "5", numero: "32328", itens: [kg, un] }],
};
const offline: Transport = {
    send: async () => {
        throw new Error("Sem API neste teste de apresentação.");
    },
};

it.each(["resumo", "detalhe"] as const)(
    "%s formata KG e sinais em português sem perder frações, IDs ou quantidades grandes",
    (consumer) => {
        const workflow = emptyWorkflow();
        workflow.selected["PedidoEntradaDto.Resumo"] = pedido;
        workflow.selected["PedidoEntradaDto.Detalhe"] = detalhe;
        const select = () => {};
        if (consumer === "resumo")
            render(
                <ReceivingSummary workflow={workflow} onSelectItem={select} />,
            );
        else
            render(
                <ReceivingDetail
                    record={detalhe}
                    section="Notas e itens"
                    onSection={select}
                    onSelect={select}
                    locked={false}
                    workflow={workflow}
                    transport={offline}
                />,
            );
        const rows = screen.getAllByRole("row");
        const kgRow = rows.find((row) =>
            row.textContent?.includes("000600366"),
        )!;
        expect(
            within(kgRow).getByRole("cell", { name: "1.820,00" }),
        ).toBeVisible();
        expect(within(kgRow).getByRole("cell", { name: "0,00" })).toBeVisible();
        expect(
            within(kgRow).getByRole("cell", { name: "0,125" }),
        ).toBeVisible();
        expect(
            within(kgRow).getByRole("cell", { name: "-1.819,875" }),
        ).toBeVisible();
        const unRow = rows.find((row) => row.textContent?.includes("SKU-UN"))!;
        expect(
            within(unRow).getByRole("cell", { name: "9.007.199.254.740.993" }),
        ).toBeVisible();
        expect(kg.prevista).toBe("1820.000000");
        expect(un.prevista).toBe("9007199254740993");
    },
);
