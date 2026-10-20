import { useState } from "react";
import { it, expect, vi } from "vitest";
import {
    render,
    screen,
    within,
    fireEvent,
    waitFor,
} from "@testing-library/react";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/domain/journeys";
import { absorb, emptyWorkflow } from "../src/domain/workflow";
import type { Perfil, Values } from "../src/contracts/runtime";
import type { Request, Transport } from "../src/api/client";

const type = "ConfiguracaoCobrancaDto.Servico";
const alpha = {
    id: "9007199254740993",
    versao: "9007199254740997",
    codigo: "SRV-ALFA",
    descricao: "Armazenagem Alfa",
    tipo: "ARMAZENAGEM",
    unidade: "POSICAO_DIA",
    situacao: "ATIVO",
} satisfies Values;
const beta = {
    ...alpha,
    id: "9007199254740995",
    codigo: "SRV-BETA",
    descricao: "Serviço Beta",
    tipo: "SAIDA",
    unidade: "UNIDADE_LOGISTICA",
} satisfies Values;
function setup(perfil: Perfil = "GESTOR", initial?: Values) {
    const requests: Request[] = [];
    const selections = vi.fn();
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            const rows =
                request.endpoint.id ===
                "ConfiguracaoCobrancaController.servicos"
                    ? [alpha, beta]
                    : [];
            return {
                data: {
                    itens: rows,
                    pagina: 0,
                    tamanho: 20,
                    totalItens: String(rows.length),
                    totalPaginas: rows.length ? 1 : 0,
                },
                ficticio: true,
                raw: "",
                replay: false,
                requestId: "SERVICE-SELECTION-LOCAL",
            };
        },
    };
    function Harness() {
        const [workflow, setWorkflow] = useState(() =>
            initial
                ? absorb(emptyWorkflow(), type, initial, true)
                : emptyWorkflow(),
        );
        return (
            <JourneyPage
                journey={journeys.find((j) => j.id === "precos")!}
                transport={transport}
                context={{ clienteId: "1", armazemId: "1" }}
                perfil={perfil}
                workflow={workflow}
                startAction=""
                onRecord={(row, selectedType) => {
                    selections(row, selectedType);
                    setWorkflow((old) => absorb(old, selectedType, row, true));
                }}
                onReceipt={(r, t, _id, request) =>
                    setWorkflow((old) => absorb(old, t, r.data, false, request))
                }
                onNavigate={vi.fn()}
            />
        );
    }
    render(<Harness />);
    return { requests, selections };
}
const opener = (name: string) =>
    screen.getByRole("button", { name: `Ver detalhes de ${name}` });

it("só seleciona ao confirmar, fecha os detalhes, informa o resultado e usa o serviço na nova tabela", async () => {
    const { requests, selections } = setup();
    const open = await screen.findByRole("button", {
        name: "Ver detalhes de Armazenagem Alfa",
    });
    open.focus();
    fireEvent.click(open);
    expect(selections).not.toHaveBeenCalled();
    fireEvent.click(screen.getByRole("button", { name: "Fechar detalhes" }));
    await waitFor(() =>
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(selections).not.toHaveBeenCalled();
    fireEvent.click(open);
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar este registro" }),
    );
    await waitFor(() =>
        expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
    );
    expect(selections).toHaveBeenCalledExactlyOnceWith(alpha, type);
    expect(screen.getByRole("status")).toHaveTextContent(
        "Serviço selecionado: Armazenagem Alfa.",
    );
    expect(screen.getByText("Selecionado").closest("tr")).toHaveAttribute(
        "data-record-id",
        alpha.id,
    );
    expect(
        screen.getByRole("button", {
            name: /Ver histórico de Serviço: Armazenagem Alfa/,
        }),
    ).toBeVisible();
    fireEvent.click(
        screen.getByRole("button", { name: "Tabelas e vigências" }),
    );
    fireEvent.click(await screen.findByRole("button", { name: "Nova tabela" }));
    const dialog = within(screen.getByRole("dialog"));
    fireEvent.click(dialog.getByRole("button", { name: "Adicionar serviço" }));
    expect(dialog.getByLabelText("Serviço *")).toHaveValue(alpha.id);
    expect(requests.every((r) => r.endpoint.method === "GET")).toBe(true);
});

it.each(["GESTOR", "SUPERVISOR"] as const)(
    "%s conserva a seleção anterior ao apenas consultar outro serviço e confirma a substituição",
    async (perfil) => {
        const { requests, selections } = setup(perfil, alpha);
        await screen.findByText("Selecionado");
        fireEvent.click(opener("Serviço Beta"));
        expect(selections).not.toHaveBeenCalled();
        fireEvent.click(
            screen.getByRole("button", { name: "Fechar detalhes" }),
        );
        expect(screen.getByText("Selecionado").closest("tr")).toHaveAttribute(
            "data-record-id",
            alpha.id,
        );
        fireEvent.click(opener("Serviço Beta"));
        fireEvent.click(
            screen.getByRole("button", { name: "Selecionar este registro" }),
        );
        await waitFor(() =>
            expect(screen.queryByRole("dialog")).not.toBeInTheDocument(),
        );
        expect(selections).toHaveBeenCalledExactlyOnceWith(beta, type);
        expect(screen.getAllByText("Selecionado")).toHaveLength(1);
        expect(screen.getByText("Selecionado").closest("tr")).toHaveAttribute(
            "data-record-id",
            beta.id,
        );
        expect(screen.getByRole("status")).toHaveTextContent(
            "Serviço selecionado: Serviço Beta.",
        );
        if (perfil === "SUPERVISOR")
            expect(
                screen.queryByRole("button", {
                    name: /Ver histórico de Serviço:/,
                }),
            ).not.toBeInTheDocument();
        expect(requests.every((r) => r.endpoint.method === "GET")).toBe(true);
    },
);
