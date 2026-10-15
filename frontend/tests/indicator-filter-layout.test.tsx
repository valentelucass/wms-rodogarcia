import { it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { RecordWorkspace } from "../src/components/records/RecordWorkspace";
import { FormReferences } from "../src/components/FormReferences";
import { journeys } from "../src/domain/journeys";
import { emptyWorkflow } from "../src/domain/workflow";
import type { Request, Transport } from "../src/api/client";
import type { Perfil } from "../src/contracts/runtime";

const client = "9007199254740993",
    warehouse = "9007199254740995",
    product = "9007199254740997";
function setup(perfil: Perfil) {
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            return {
                data: {
                    itens: [],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "0",
                    totalPaginas: 0,
                },
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "INDICATOR-FILTER-LOCAL",
            };
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
                    produtoId: [{ value: product, title: "SKU-ALFA" }],
                },
            }}
        >
            <RecordWorkspace
                step={3}
                journey={journeys.find((j) => j.id === "estoque")!}
                transport={transport}
                context={{
                    clienteId: client,
                    armazemId: warehouse,
                    fuso: "America/Sao_Paulo",
                }}
                workflow={emptyWorkflow()}
                perfil={perfil}
                startAction=""
                onRecord={vi.fn()}
                onReceipt={vi.fn()}
                onNavigate={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return requests;
}
const change = (name: string, value: string) =>
    fireEvent.change(screen.getByLabelText(name, { exact: true }), {
        target: { value },
    });
async function ready(requests: Request[], count: number) {
    await waitFor(() => expect(requests).toHaveLength(count));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Aplicar filtros" }),
        ).toBeEnabled(),
    );
}

it.each(["SUPERVISOR", "GESTOR"] as const)(
    "%s aplica produto/valor/fuso exatos e restaura os padrões",
    async (perfil) => {
        const requests = setup(perfil);
        await ready(requests, 1);
        change("Produto / SKU", product);
        change("Incluir valor (Supervisor / Gestor)", "true");
        change("Fuso do relatório *", "America/Manaus");
        fireEvent.click(
            screen.getByRole("button", { name: "Aplicar filtros" }),
        );
        await ready(requests, 2);
        expect(requests[1].query).toMatchObject({
            clienteId: client,
            armazemId: warehouse,
            produtoId: product,
            valor: true,
            fuso: "America/Manaus",
        });
        expect(Number(requests[1].query.pagina)).toBe(0);
        fireEvent.click(
            screen.getByRole("button", { name: "Restaurar filtros" }),
        );
        await ready(requests, 3);
        expect(screen.getByLabelText("Produto / SKU")).toHaveValue("");
        expect(
            screen.getByLabelText("Incluir valor (Supervisor / Gestor)"),
        ).toHaveValue("false");
        expect(screen.getByLabelText("Fuso do relatório *")).toHaveValue(
            "America/Sao_Paulo",
        );
        expect(requests[2].query).toMatchObject({
            clienteId: client,
            armazemId: warehouse,
            valor: false,
            fuso: "America/Sao_Paulo",
        });
        expect(
            requests.every(
                (r) =>
                    r.endpoint.id === "IndicadorEstoqueController.listar" &&
                    r.endpoint.method === "GET",
            ),
        ).toBe(true);
    },
);

it("Operação mantém a inclusão de valor desabilitada e consulta sem valor", async () => {
    const requests = setup("OPERACAO");
    await ready(requests, 1);
    expect(
        screen.getByLabelText("Incluir valor (Supervisor / Gestor)"),
    ).toBeDisabled();
    expect(
        screen.getByLabelText("Incluir valor (Supervisor / Gestor)"),
    ).toHaveValue("false");
    fireEvent.click(screen.getByRole("button", { name: "Aplicar filtros" }));
    await ready(requests, 2);
    expect(requests[1].query.valor).toBe(false);
});
