import { it, expect, vi } from "vitest";
import { act, fireEvent, render, screen } from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import type { Receipt, Request, Transport } from "../src/api/client";

const pageReceipt = (pagina: number, empty = false): Receipt => ({
    data: {
        itens: empty
            ? []
            : [{ id: "1", sku: "PROVA", descricao: "Produto da consulta" }],
        pagina,
        tamanho: 20,
        totalItens: empty ? "0" : "9223372036854775807",
        totalPaginas: empty ? 0 : 3,
    },
    requestId: "prova-local",
    raw: "",
    ficticio: true,
    replay: false,
});
it("paginação remota preserva filtros recebidos, Long exato e bloqueia envio duplicado", async () => {
    const requests: Request[] = [];
    let finish!: (r: Receipt) => void;
    const transport: Transport = {
        send: vi.fn(async (request) => {
            requests.push(request);
            if (requests.length === 1) return pageReceipt(0);
            return new Promise<Receipt>((resolve) => {
                finish = resolve;
            });
        }),
    };
    render(
        <Operation
            id="ProdutoController.listar"
            transport={transport}
            context={{ clienteId: "9223372036854775807" }}
            perfil="GESTOR"
            onSelect={vi.fn()}
        />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    await screen.findByText("Página 1 de 3");
    expect(
        screen.getByRole("navigation", { name: "Paginação dos resultados" }),
    ).toHaveTextContent("9223372036854775807 registros");
    fireEvent.change(screen.getByLabelText("Cliente (ID) *"), {
        target: { value: "2" },
    });
    const next = screen.getByRole("button", { name: "Próxima" });
    fireEvent.click(next);
    fireEvent.click(next);
    expect(requests).toHaveLength(2);
    expect(requests[1].query).toEqual({
        clienteId: "9223372036854775807",
        pagina: 1,
        tamanho: "20",
    });
    expect(next).toBeDisabled();
    expect(screen.getByRole("button", { name: "Consultando…" })).toBeDisabled();
    await act(async () => finish(pageReceipt(1)));
    expect(screen.getByText("Página 2 de 3")).toBeInTheDocument();
    expect(screen.getByLabelText("Cliente (ID) *")).toHaveValue(
        "9223372036854775807",
    );
    expect(screen.getByRole("button", { name: "Anterior" })).toBeEnabled();
});
it("resultado vazio conserva paginação coerente e não oferece destinos inexistentes", async () => {
    render(
        <Operation
            id="ProdutoController.listar"
            transport={{ send: async () => pageReceipt(0, true) }}
            context={{ clienteId: "1" }}
            perfil="GESTOR"
            onSelect={vi.fn()}
        />,
    );
    fireEvent.click(screen.getByRole("button", { name: "Consultar" }));
    await screen.findByText("Nenhum registro encontrado.");
    expect(screen.getByText("Página 1 de 1")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Anterior" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
});
