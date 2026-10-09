import { useState } from "react";
import { it, expect, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/modules/saida/definition";
import { useExerciseSession } from "../src/hooks/useExerciseSession";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { toWire, stringifyExact } from "../src/contracts/codec";
import type { Values } from "../src/contracts/runtime";

const obj = (type: string, patch: Values) => ({
    ...(fixture(type) as Values),
    ...patch,
});
const reserve = obj("PedidoSaidaDto.Reserva", { id: "701" });
const first = obj("PedidoSaidaDto.Detalhe", {
    id: "601",
    versao: "3",
    reservas: [reserve],
});
const second = obj("PedidoSaidaDto.Detalhe", {
    id: "602",
    versao: "1",
    reservas: [],
});
function Harness({ transport }: { transport: Transport }) {
    const s = useExerciseSession();
    const [start, setStart] = useState("PedidoSaidaController.listar");
    return (
        <JourneyPage
            key={start}
            journey={journeys[0]}
            transport={transport}
            context={s.context}
            perfil={s.perfil}
            workflow={s.workflow}
            onRecord={s.onRecord}
            onReceipt={s.onReceipt}
            startAction={start}
            onNavigate={(next) => setStart(next.action)}
        />
    );
}
function transportWith(write?: (r: Request) => Promise<never>): Transport {
    return {
        send: vi.fn(async (r: Request) => {
            if (r.endpoint.method !== "GET" && write) return write(r);
            const data =
                r.endpoint.id === "PedidoSaidaController.listar"
                    ? {
                          itens: [first, second],
                          pagina: 0,
                          tamanho: 20,
                          totalItens: "2",
                          totalPaginas: 1,
                      }
                    : obj("PedidoSaidaDto.Sugestao", { versao: "3" });
            return {
                data,
                raw: stringifyExact(toWire(r.endpoint.response, data)),
                requestId: "selecoes-ficticias",
                ficticio: true,
                replay: false,
            };
        }),
    };
}
async function selectOrder(number: number) {
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    const table = await screen.findByRole("table", {
        name: "Pedidos de saída · 2 registros nesta resposta",
    });
    const row = table.querySelector("tbody")!.children[
        number - 1
    ] as HTMLElement;
    fireEvent.click(
        within(row)
            .getAllByRole("button", { name: "Selecionar registro " + number })
            .at(-1)!,
    );
}
it("UI troca pedido601/reserva701 por pedido602 sem conservar ID, revisão ou opção de reserva", async () => {
    render(<Harness transport={transportWith()} />);
    await selectOrder(1);
    const refs = screen.getByRole("region", {
        name: "Referências confirmadas da jornada",
    });
    expect(refs).toHaveTextContent("Pedido de saída: 601");
    expect(refs).toHaveTextContent("Reserva: 701");
    fireEvent.click(
        screen.getByRole("button", { name: "3. Leitura e separação" }),
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir leitura da reserva" }),
    );
    expect(
        screen.getByLabelText("Identificador *", { exact: true }),
    ).toHaveValue("601");
    expect(
        screen.getByLabelText("Reserva (ID) *", { exact: true }),
    ).toHaveValue("701");
    expect(
        screen.getByLabelText("Revisão atual *", { exact: true }),
    ).toHaveValue("3");
    fireEvent.click(screen.getByRole("button", { name: "1. Pedido integral" }));
    await selectOrder(2);
    expect(refs).toHaveTextContent("Pedido de saída: 602");
    expect(refs).not.toHaveTextContent("Reserva: 701");
    fireEvent.click(
        screen.getByRole("button", { name: "3. Leitura e separação" }),
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir leitura da reserva" }),
    );
    expect(
        screen.getByLabelText("Identificador *", { exact: true }),
    ).toHaveValue("602");
    expect(
        screen.getByLabelText("Revisão atual *", { exact: true }),
    ).toHaveValue("1");
    expect(
        screen.getByLabelText("Reserva (ID) *", { exact: true }),
    ).toHaveValue("");
    expect(screen.queryByRole("option", { name: /^701/ })).toBeNull();
});
it("UI consulta FIFO de outro pedido exige selecionar esse pai antes do próximo passo", async () => {
    render(<Harness transport={transportWith()} />);
    await selectOrder(1);
    fireEvent.click(
        screen.getByRole("button", { name: "2. Sugestão FIFO e reserva" }),
    );
    fireEvent.change(
        screen.getByLabelText("Identificador *", { exact: true }),
        { target: { value: "602" } },
    );
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    await screen.findByText(/O pedido consultado difere da seleção da jornada/);
    expect(
        screen.queryByRole("button", {
            name: "Confirmar reserva da sugestão do servidor",
        }),
    ).toBeNull();
    expect(
        screen.getByRole("region", {
            name: "Referências confirmadas da jornada",
        }),
    ).toHaveTextContent("Pedido de saída: 601");
});
it("UI com escrita incerta conserva formulário e impede trocar referências para liberar outro payload", async () => {
    const sent: Request[] = [];
    const transport = transportWith(async (r) => {
        sent.push(r);
        throw new ApiError(
            "Resultado desconhecido",
            502,
            "ERRO_HTTP",
            "incerto",
            true,
        );
    });
    render(<Harness transport={transport} />);
    await selectOrder(1);
    fireEvent.click(
        screen.getByRole("button", { name: "2. Sugestão FIFO e reserva" }),
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Confirmar reserva integral" }),
    );
    fireEvent.change(
        screen.getByLabelText("Motivo / justificativa *", { exact: true }),
        { target: { value: "Reserva fictícia para limite de contexto" } },
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await screen.findByText("Resultado desconhecido", { exact: true });
    expect(
        screen.getByRole("button", { name: "1. Pedido integral" }),
    ).toBeDisabled();
    expect(
        screen.getByRole("button", {
            name: "Usar referências consultadas no formulário (descarta edição atual)",
        }),
    ).toBeDisabled();
    expect(
        screen.getByLabelText("Identificador *", { exact: true }),
    ).toHaveValue("601");
    fireEvent.click(
        screen.getByRole("button", {
            name: "Repetir exatamente a mesma operação",
        }),
    );
    await vi.waitFor(() => expect(sent).toHaveLength(2));
    expect(sent[1].params).toEqual(sent[0].params);
    expect(sent[1].body).toEqual(sent[0].body);
});
