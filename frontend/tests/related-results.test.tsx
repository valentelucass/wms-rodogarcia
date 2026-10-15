import { describe, it, expect, vi } from "vitest";
import {
    render,
    screen,
    within,
    fireEvent,
    waitFor,
} from "@testing-library/react";
import { RecordDialog } from "../src/components/records/RecordDialog";
import { recordPages } from "../src/domain/recordPages";
import { journeys } from "../src/domain/journeys";
import { emptyWorkflow } from "../src/domain/workflow";
import { fixture } from "../src/api/mock/fixtures";
import type { Values } from "../src/contracts/runtime";
import type { Request, Transport } from "../src/api/client";

function setup({ populated = false, failProgress = false } = {}) {
    const requests: Request[] = [];
    const selected = vi.fn();
    const order = {
        ...(fixture("PedidoEntradaDto.Resumo") as Values),
        id: "9007199254740993",
        referencia: "PED-ALFA",
    };
    const entry = {
        ...(fixture("RecebimentoDto.Entrada") as Values),
        id: "9007199254740995",
        pedidoEntradaId: order.id,
    };
    let failure = failProgress;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            let data: unknown;
            if (request.endpoint.id === "PedidoEntradaController.consultar")
                data = {
                    pedido: order,
                    notas: [],
                    divergente: false,
                };
            else if (request.endpoint.id === "PedidoEntradaController.entradas")
                data = {
                    itens: populated ? [entry] : [],
                    pagina: Number(request.query.pagina ?? 0),
                    totalPaginas: populated ? 2 : 0,
                    tamanho: 20,
                    totalItens: populated ? "21" : "0",
                };
            else if (
                request.endpoint.id === "UnidadeLogisticaController.progresso"
            ) {
                if (failure)
                    throw new Error("Progresso indisponível nesta consulta");
                data = {
                    pedidoId: order.id,
                    entradasConferidas: "0",
                    entradasUnitizadas: "0",
                    entradasPendentes: "0",
                    concluida: false,
                };
            } else
                throw new Error(`Consulta inesperada: ${request.endpoint.id}`);
            return {
                data,
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "RELATED-RESULTS-LOCAL",
            };
        },
    };
    render(
        <RecordDialog
            opening={{ row: order, type: "PedidoEntradaDto.Resumo" }}
            page={recordPages.unidades[0]}
            actions={[]}
            transport={transport}
            context={{ clienteId: "1", armazemId: "1" }}
            workflow={emptyWorkflow()}
            perfil="GESTOR"
            onClose={vi.fn()}
            onUnknownClose={vi.fn()}
            onReceipt={vi.fn()}
            onRecord={selected}
            onNavigate={vi.fn()}
            journey={journeys.find((j) => j.id === "unidades")!}
        />,
    );
    return {
        requests,
        selected,
        entry,
        recover: () => {
            failure = false;
        },
    };
}

describe("Consultas relacionadas agrupadas", () => {
    it("mantém o vazio sem paginação inútil e os valores zero/falso no progresso", async () => {
        setup();
        const entries = await screen.findByRole("region", {
            name: "Consulta: Entradas conferidas",
        });
        await within(entries).findByText("Nenhum registro encontrado.");
        expect(
            within(entries).queryByRole("navigation"),
        ).not.toBeInTheDocument();
        expect(
            within(entries).getByRole("button", {
                name: "Atualizar entradas conferidas",
            }),
        ).toBeEnabled();
        const progress = screen.getByRole("region", {
            name: "Consulta: Progresso",
        });
        expect(
            await within(progress).findAllByText("0", { selector: "dd" }),
        ).toHaveLength(3);
        expect(
            within(progress).getByText("Não", { selector: "dd" }),
        ).toBeVisible();
    });

    it("pagina no mesmo pedido com ID exato e conserva a seleção da entrada", async () => {
        const { requests, selected, entry } = setup({ populated: true });
        const entries = await screen.findByRole("region", {
            name: "Consulta: Entradas conferidas",
        });
        const next = await within(entries).findByRole("button", {
            name: "Próxima",
        });
        await waitFor(() => expect(next).toBeEnabled());
        fireEvent.click(next);
        await within(entries).findByText("Página 2 de 2");
        const request = requests
            .filter((r) => r.endpoint.id === "PedidoEntradaController.entradas")
            .at(-1)!;
        expect(request.params.id).toBe("9007199254740993");
        expect(request.query.pagina).toBe(1);
        fireEvent.click(
            within(entries).getByRole("button", { name: /^Selecionar / }),
        );
        expect(selected).toHaveBeenCalledWith(entry, "RecebimentoDto.Entrada");
        expect(requests.every((r) => r.endpoint.method === "GET")).toBe(true);
    });

    it("recupera erro de um bloco sem atualizar o outro", async () => {
        const state = setup({ failProgress: true });
        const progress = await screen.findByRole("region", {
            name: "Consulta: Progresso",
        });
        expect(await within(progress).findByRole("alert")).toHaveTextContent(
            "Progresso indisponível",
        );
        const entriesBefore = state.requests.filter(
            (r) => r.endpoint.id === "PedidoEntradaController.entradas",
        ).length;
        state.recover();
        fireEvent.click(
            within(progress).getByRole("button", {
                name: "Atualizar progresso",
            }),
        );
        await within(progress).findByText("Não", { selector: "dd" });
        expect(within(progress).queryByRole("alert")).not.toBeInTheDocument();
        expect(
            state.requests.filter(
                (r) => r.endpoint.id === "PedidoEntradaController.entradas",
            ),
        ).toHaveLength(entriesBefore);
    });
});
