import { Profiler } from "react";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { ReferenceCatalogProvider } from "../src/components/context/ReferenceCatalog";
import { ReferenceSelect } from "../src/components/context/ReferenceSelect";
import { ContextPicker } from "../src/components/context/ContextPicker";

const page = (request: Request, pages = 4, empty = false): Receipt => {
    const index = Number(request.query.pagina);
    const client = request.endpoint.id === "ClienteController.listar";
    const data = {
        itens: empty
            ? []
            : Array.from({ length: client ? 100 : 1 }, (_, i) => ({
                  id: String(index * 100 + i + 1),
                  nome:
                      (client ? "Cliente" : "Armazém") +
                      " sintético " +
                      (index * 100 + i + 1),
                  codigo: "CARGA" + i,
                  situacao: "ATIVO",
              })),
        pagina: index,
        tamanho: 100,
        totalItens: String(empty ? 0 : client ? pages * 100 : 1),
        totalPaginas: empty ? 0 : client ? pages : 1,
    };
    return {
        data,
        raw: JSON.stringify(data),
        requestId: "carga-local",
        ficticio: true,
        replay: false,
    };
};

function selects() {
    return (
        <>
            <ReferenceSelect
                kind="clientes"
                title="Cliente"
                value=""
                onChange={vi.fn()}
            />
            <ReferenceSelect
                kind="armazens"
                title="Armazém"
                value=""
                onChange={vi.fn()}
            />
        </>
    );
}

it("CARGA01 catálogo independente fica disponível enquanto páginas de clientes aguardam", async () => {
    const requests: Request[] = [],
        renders: { phase: string; durationMs: number }[] = [];
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
        release = resolve;
    });
    const started = performance.now();
    const events: {
        endpoint: string;
        page: number;
        startedMs: number;
        finishedMs?: number;
        bytes?: number;
    }[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            const event = {
                endpoint: request.endpoint.id,
                page: Number(request.query.pagina),
                startedMs: performance.now() - started,
            };
            events.push(event);
            if (
                request.endpoint.id === "ClienteController.listar" &&
                Number(request.query.pagina) > 0
            )
                await gate;
            const result = page(request);
            Object.assign(event, {
                finishedMs: performance.now() - started,
                bytes: new TextEncoder().encode(result.raw).byteLength,
            });
            return result;
        },
    };
    const view = render(
        <Profiler
            id="catalog"
            onRender={(_id, phase, durationMs) =>
                renders.push({ phase, durationMs })
            }
        >
            <ReferenceCatalogProvider transport={transport}>
                {selects()}
            </ReferenceCatalogProvider>
        </Profiler>,
    );
    await waitFor(() =>
        expect(requests.some((r) => Number(r.query.pagina) === 1)).toBe(true),
    );
    const warehouse = screen.getByRole("combobox", { name: "Armazém" });
    console.log(
        "CARGA01 proof",
        JSON.stringify({
            at: new Date().toISOString(),
            environment: "jsdom/transport interceptado",
            events,
            renders,
            warehouseDisabled: warehouse.hasAttribute("disabled"),
            clientPending: screen
                .getByRole("combobox", { name: "Cliente" })
                .hasAttribute("disabled"),
            domNodes: view.container.querySelectorAll("*").length,
        }),
    );
    expect.soft(warehouse).toBeEnabled();
    expect
        .soft(
            requests
                .filter((r) => r.endpoint.id === "ClienteController.listar")
                .map((r) => Number(r.query.pagina)),
        )
        .toEqual([0, 1, 2, 3]);
    await act(async () => release());
    await waitFor(() =>
        expect(screen.getByRole("combobox", { name: "Cliente" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("combobox", { name: "Cliente" }));
    expect(screen.getAllByRole("option")).toHaveLength(400);
    console.log(
        "CARGA01 complete",
        JSON.stringify({
            events,
            renders,
            requestCount: requests.length,
            responseBytes: events.reduce((n, e) => n + (e.bytes ?? 0), 0),
            options: 400,
            domNodes: view.container.querySelectorAll("*").length,
        }),
    );
});

it("CARGA01 vazio mantém seletores disponíveis e erro não fabrica referências", async () => {
    let denied = false;
    const transport: Transport = {
        send: async (r) => {
            if (denied) throw new ApiError("Recusa sintética", 403, "DENIED");
            return page(r, 1, true);
        },
    };
    const view = render(
        <ReferenceCatalogProvider transport={transport} version="one">
            {selects()}
        </ReferenceCatalogProvider>,
    );
    const client = screen.getByRole("combobox", { name: "Cliente" });
    await waitFor(() => expect(client).toBeEnabled());
    fireEvent.click(client);
    expect(screen.queryAllByRole("option")).toHaveLength(0);
    expect(
        screen.getByText("Nenhum cadastro disponível para seu acesso."),
    ).toBeVisible();
    denied = true;
    view.rerender(
        <ReferenceCatalogProvider transport={transport} version="two">
            {selects()}
        </ReferenceCatalogProvider>,
    );
    await waitFor(() =>
        expect(screen.getByRole("combobox", { name: "Cliente" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("combobox", { name: "Cliente" }));
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Não foi possível carregar",
    );
    expect(screen.queryAllByRole("option")).toHaveLength(0);
});

it("CARGA01 contexto Todos os clientes usa armazém concluído sem depender da enumeração de clientes", async () => {
    const onApply = vi.fn();
    let release!: () => void;
    const gate = new Promise<void>((resolve) => {
        release = resolve;
    });
    const transport: Transport = {
        send: async (request) => {
            if (request.endpoint.id === "ClienteController.listar") await gate;
            return page(request, 1);
        },
    };
    render(
        <ReferenceCatalogProvider transport={transport}>
            <ContextPicker context={{}} onApply={onApply} />
        </ReferenceCatalogProvider>,
    );
    const warehouse = screen.getByRole("combobox", { name: "Armazém" });
    await waitFor(() => expect(warehouse).toBeEnabled());
    fireEvent.change(warehouse, { target: { value: "Armazém sintético 1" } });
    fireEvent.keyDown(warehouse, { key: "Enter" });
    expect(screen.getByRole("combobox", { name: "Cliente" })).toBeDisabled();
    const apply = screen.getByRole("button", { name: "Aplicar contexto" });
    expect.soft(apply).toBeEnabled();
    fireEvent.click(apply);
    expect.soft(onApply).toHaveBeenCalledExactlyOnceWith({ armazemId: "1" });
    await act(async () => release());
});

it("CARGA01 paralelismo limitado completa oito páginas sem duplicar requests nem perder opções", async () => {
    let active = 0,
        maxActive = 0;
    const pages: number[] = [];
    const transport: Transport = {
        send: async (request) => {
            if (request.endpoint.id === "ClienteController.listar") {
                pages.push(Number(request.query.pagina));
                active++;
                maxActive = Math.max(maxActive, active);
                await new Promise((resolve) => setTimeout(resolve, 5));
                active--;
            }
            return page(request, 8);
        },
    };
    render(
        <ReferenceCatalogProvider transport={transport}>
            {selects()}
        </ReferenceCatalogProvider>,
    );
    await waitFor(() =>
        expect(screen.getByRole("combobox", { name: "Cliente" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("combobox", { name: "Cliente" }));
    expect(screen.getAllByRole("option")).toHaveLength(800);
    expect([...pages].sort((a, b) => a - b)).toEqual([0, 1, 2, 3, 4, 5, 6, 7]);
    expect(maxActive).toBe(3);
});

it("CARGA01 mudança de paginação recusa catálogo incompleto e aborta páginas ainda pendentes", async () => {
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            const response = page(request, 4);
            if (
                request.endpoint.id === "ClienteController.listar" &&
                Number(request.query.pagina) === 1
            )
                (response.data as { totalPaginas: number }).totalPaginas = 5;
            return response;
        },
    };
    render(
        <ReferenceCatalogProvider transport={transport}>
            {selects()}
        </ReferenceCatalogProvider>,
    );
    const client = screen.getByRole("combobox", { name: "Cliente" });
    await waitFor(() => expect(client).toBeEnabled());
    fireEvent.click(client);
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Não foi possível carregar",
    );
    expect(screen.queryAllByRole("option")).toHaveLength(0);
    expect(requests.every((r) => r.signal.aborted)).toBe(true);
});
