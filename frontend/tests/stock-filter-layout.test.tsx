import { it, expect, vi } from "vitest";
import {
    render,
    screen,
    fireEvent,
    waitFor,
    act,
} from "@testing-library/react";
import { RecordWorkspace } from "../src/components/records/RecordWorkspace";
import { FormReferences } from "../src/components/FormReferences";
import { journeys } from "../src/domain/journeys";
import { emptyWorkflow } from "../src/domain/workflow";
import type { Request, Receipt, Transport } from "../src/api/client";

const client = "9007199254740993",
    warehouse = "9007199254740995";
const product = "9007199254740997",
    address = "9007199254740999";
const code = "00000000-0000-4000-8000-000000000002";
const empty: Receipt = {
    data: {
        itens: [],
        pagina: 0,
        tamanho: 20,
        totalPaginas: 0,
        totalItens: "0",
    },
    ficticio: true,
    raw: "",
    replay: false,
    requestId: "STOCK-FILTER-LOCAL",
};
function setup(hold = false) {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (hold && requests.length > 1)
                return new Promise<Receipt>((_resolve, fail) => {
                    reject = fail;
                });
            return empty;
        },
    };
    render(
        <FormReferences.Provider
            value={{
                defaults: {},
                records: {},
                options: {
                    clienteId: [{ value: client, title: "Cliente Alfa" }],
                    armazemId: [{ value: warehouse, title: "Armazém central" }],
                    produtoId: [{ value: product, title: "Produto Alfa" }],
                    enderecoId: [{ value: address, title: "Posição A01" }],
                },
            }}
        >
            <RecordWorkspace
                step={1}
                journey={journeys.find((j) => j.id === "unidades")!}
                transport={transport}
                context={{ clienteId: client, armazemId: warehouse }}
                workflow={emptyWorkflow()}
                perfil="GESTOR"
                startAction=""
                onRecord={vi.fn()}
                onReceipt={vi.fn()}
                onNavigate={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        fail: () => reject(new Error("Consulta fictícia indisponível")),
    };
}
const set = (name: string, value: string) =>
    fireEvent.change(screen.getByLabelText(name, { exact: true }), {
        target: { value },
    });

it("aplica os sete filtros reorganizados com IDs exatos e restaura o contexto inicial", async () => {
    const { requests } = setup();
    await waitFor(() => expect(requests).toHaveLength(1));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Aplicar filtros" }),
        ).toBeEnabled(),
    );
    set("Produto / SKU", product);
    set("UUID da unidade", code);
    set("Endereço", address);
    set("Disponível para saída", "true");
    set("Situação informada", "BLOQUEADA");
    fireEvent.click(screen.getByRole("button", { name: "Aplicar filtros" }));
    await waitFor(() => expect(requests).toHaveLength(2));
    expect(requests[1].query).toMatchObject({
        clienteId: client,
        armazemId: warehouse,
        produtoId: product,
        codigoUnidade: code,
        enderecoId: address,
        disponivel: true,
        situacao: "BLOQUEADA",
    });
    expect(Number(requests[1].query.pagina)).toBe(0);
    fireEvent.click(screen.getByRole("button", { name: "Restaurar filtros" }));
    await waitFor(() => expect(requests).toHaveLength(3));
    expect(requests[2].query.clienteId).toBe(client);
    expect(requests[2].query.armazemId).toBe(warehouse);
    expect(screen.getByLabelText("UUID da unidade")).toHaveValue("");
    expect(screen.getByLabelText("Situação informada")).toHaveValue("");
    expect(
        requests.every(
            (r) =>
                r.endpoint.id === "EstoqueController.listar" &&
                r.endpoint.method === "GET",
        ),
    ).toBe(true);
});

it("bloqueia os grupos durante a consulta e conserva os filtros após erro", async () => {
    const state = setup(true);
    await waitFor(() => expect(state.requests).toHaveLength(1));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Aplicar filtros" }),
        ).toBeEnabled(),
    );
    set("UUID da unidade", code);
    fireEvent.click(screen.getByRole("button", { name: "Aplicar filtros" }));
    await waitFor(() =>
        expect(screen.getByLabelText("Produto / SKU")).toBeDisabled(),
    );
    expect(screen.getByLabelText("Endereço")).toBeDisabled();
    expect(
        screen.getByRole("button", { name: "Restaurar filtros" }),
    ).toBeDisabled();
    await act(async () => state.fail());
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Consulta fictícia indisponível",
    );
    expect(screen.getByLabelText("UUID da unidade")).toHaveValue(code);
    expect(
        screen.getByRole("button", { name: "Aplicar filtros" }),
    ).toBeEnabled();
});
