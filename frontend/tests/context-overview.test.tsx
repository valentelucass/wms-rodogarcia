import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { FictitiousTransport } from "../src/api/fictitious";
import { ReferenceCatalogProvider } from "../src/components/context/ReferenceCatalog";
import { ReferenceSelect } from "../src/components/context/ReferenceSelect";
import {
    WarehouseOverview,
    AddressMap,
    overviewMoney,
} from "../src/components/dashboard/WarehouseOverview";
import { overviewPositions } from "../src/api/mock/overview";
import { ScalarField } from "../src/components/fields/ScalarField";
import { useCollectorReading } from "../src/hooks/useCollectorReading";
import { renderHook } from "@testing-library/react";
import { act } from "@testing-library/react";
import type { Transport, Receipt, Request } from "../src/api/client";
import { overviewExample } from "../src/api/mock/overview";

it("prévia do endereço abre por foco, fecha com Escape e preserva o clique de detalhes", () => {
    const position = {
        ...overviewPositions[0],
        codigo: "ENDERECO-COM-CODIGO-COMPLETO",
        bloqueada: true,
        reservada: true,
        capacidadePesoKg: null,
    };
    const onSelect = vi.fn();
    const { unmount } = render(
        <AddressMap positions={[position]} onSelect={onSelect} />,
    );
    const cell = screen.getByRole("button", {
        name: /ENDERECO-COM-CODIGO-COMPLETO/,
    });
    fireEvent.focus(cell);
    const preview = screen.getByRole("tooltip");
    expect(preview).toHaveTextContent(position.codigo);
    expect(preview).toHaveTextContent(position.armazem);
    expect(preview).toHaveTextContent("Bloqueada");
    expect(preview).toHaveTextContent("Reservada");
    expect(preview).toHaveTextContent("Não configurada");
    expect(cell).toHaveAttribute("aria-describedby", preview.id);
    expect(onSelect).not.toHaveBeenCalled();
    fireEvent.keyDown(window, { key: "Escape" });
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    fireEvent.focus(cell);
    fireEvent.click(cell);
    expect(onSelect).toHaveBeenCalledExactlyOnceWith(position);
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
    fireEvent.focus(cell);
    unmount();
    expect(screen.queryByRole("tooltip")).not.toBeInTheDocument();
});

it("moeda mantém centavos, arredonda sem float e distingue ausência de zero", () => {
    expect(overviewMoney("12500.005")).toBe("R$ 12.500,01");
    expect(overviewMoney("9007199254740993.99")).toBe(
        "R$ 9.007.199.254.740.993,99",
    );
    expect(overviewMoney("0")).toBe("R$ 0,00");
    expect(overviewMoney(null)).toBe("—");
    expect(overviewMoney("0E-12")).toBe("R$ 0,00");
});

it("seleção busca por nome sem transformar texto digitado em ID e aceita teclado", async () => {
    const transport = new FictitiousTransport();
    const change = vi.fn();
    render(
        <ReferenceCatalogProvider transport={transport}>
            <ReferenceSelect
                kind="armazens"
                title="Armazém"
                value="1"
                onChange={change}
            />
        </ReferenceCatalogProvider>,
    );
    const input = screen.getByRole("combobox");
    await waitFor(() => expect(input).toBeEnabled());
    expect(input).toHaveValue("Centro fictício");
    fireEvent.click(input);
    fireEvent.change(input, { target: { value: "anexo" } });
    expect(change).not.toHaveBeenCalled();
    expect(screen.getByRole("option")).toHaveTextContent("Anexo fictício");
    fireEvent.keyDown(input, { key: "Enter" });
    expect(change).toHaveBeenCalledWith("2");
    fireEvent.click(input);
    fireEvent.change(input, { target: { value: "não cadastrado" } });
    expect(screen.queryByRole("option")).not.toBeInTheDocument();
    fireEvent.keyDown(input, { key: "Enter" });
    expect(change).toHaveBeenCalledTimes(1);
});
it("contexto do formulário apresenta seletor de cliente com ID interno", async () => {
    render(
        <ReferenceCatalogProvider transport={new FictitiousTransport()}>
            <ScalarField
                f={{ name: "clienteId", type: "Long", required: true }}
                value="1"
                onChange={vi.fn()}
                path="clienteId"
                disabled={false}
            />
        </ReferenceCatalogProvider>,
    );
    await waitFor(() =>
        expect(screen.getByRole("combobox")).toHaveValue("Cliente fictício 1"),
    );
});
it("mapa e indicadores distinguem 80 posições, duas ocupadas e uma unidade; filtro não muda totais", async () => {
    const transport = new FictitiousTransport();
    render(
        <ReferenceCatalogProvider transport={transport}>
            <WarehouseOverview
                transport={transport}
                context={{ clienteId: "1", armazemId: "1" }}
                onScope={vi.fn()}
                onAddress={vi.fn()}
            />
        </ReferenceCatalogProvider>,
    );
    await screen.findByText("3%");
    const region = screen.getByRole("region", {
        name: "Visão geral da operação",
    });
    expect(region.querySelectorAll(".overview-metric")).toHaveLength(10);
    expect(
        region.querySelector(".overview-metric--3 strong"),
    ).toHaveTextContent("1");
    expect(region.querySelectorAll(".map-position")).toHaveLength(80);
    fireEvent.click(screen.getByRole("button", { name: "Filtros" }));
    fireEvent.change(screen.getByLabelText("Mostrar posições"), {
        target: { value: "OCUPADO" },
    });
    await waitFor(() =>
        expect(region.querySelectorAll(".map-position")).toHaveLength(2),
    );
    expect(screen.getByText("3%")).toBeInTheDocument();
    expect(transport.requests.at(-1)?.query.estado).toBe("OCUPADO");
});
it("área especial e endereço inativo nunca recebem cor de livre", () => {
    const p = overviewPositions[2];
    const select = vi.fn();
    render(
        <AddressMap
            positions={[
                {
                    ...p,
                    id: "10",
                    tipo: "QUARENTENA",
                    quarentena: true,
                    estado: "QUARENTENA",
                    disponivel: false,
                },
                {
                    ...p,
                    id: "11",
                    situacao: "INATIVO",
                    estado: "INATIVO",
                    disponivel: false,
                },
            ]}
            onSelect={select}
        />,
    );
    const buttons = screen.getAllByRole("button");
    expect(buttons.every((b) => b.classList.contains("map-state--other"))).toBe(
        true,
    );
    expect(buttons[0]).toHaveAccessibleName(`${p.codigo} · Quarentena`);
    fireEvent.click(buttons[0]);
    expect(select).toHaveBeenCalledWith(expect.objectContaining({ id: "10" }));
});

it("resposta atrasada de outro armazém não substitui a visão atual", async () => {
    let finish!: (r: Receipt) => void;
    const receipt = (r: Request, empty: boolean): Receipt => ({
        data: overviewExample(r, "GESTOR", empty),
        raw: "",
        requestId: "isolado",
        ficticio: true,
        replay: false,
    });
    let old: Request;
    const transport: Transport = {
        send: vi.fn(async (r) => {
            if (String(r.query.armazemId) === "1") {
                old = r;
                return new Promise<Receipt>((resolve) => {
                    finish = resolve;
                });
            }
            return receipt(r, true);
        }),
    };
    const props = { transport, onScope: vi.fn(), onAddress: vi.fn() };
    const view = render(
        <WarehouseOverview {...props} context={{ armazemId: "1" }} />,
    );
    view.rerender(
        <WarehouseOverview {...props} context={{ armazemId: "2" }} />,
    );
    await screen.findByText("0%", { exact: true });
    await act(async () => finish(receipt(old, false)));
    expect(screen.getByText("0%", { exact: true })).toBeInTheDocument();
    expect(screen.queryByText("3%", { exact: true })).not.toBeInTheDocument();
});

it("catálogo percorre todas as páginas e preserva o ID Long ao escolher um nome", async () => {
    const requests: Request[] = [];
    const change = vi.fn();
    const large = "9007199254740993";
    const transport: Transport = {
        send: async (r) => {
            requests.push(r);
            const cliente = r.endpoint.id === "ClienteController.listar";
            const pagina = Number(r.query.pagina);
            const count = cliente ? (pagina === 0 ? 100 : 1) : 1;
            return {
                data: {
                    itens: Array.from({ length: count }, (_, i) => ({
                        id: cliente && pagina === 1 ? large : String(i + 1),
                        nome:
                            cliente && pagina === 1
                                ? "Último cliente"
                                : "Cadastro " + i,
                        codigo: "DEMO",
                        situacao: "ATIVO",
                    })),
                    pagina,
                    tamanho: 100,
                    totalItens: cliente ? "101" : "1",
                    totalPaginas: cliente ? 2 : 1,
                },
                raw: "",
                requestId: "isolado",
                ficticio: true,
                replay: false,
            };
        },
    };
    render(
        <ReferenceCatalogProvider transport={transport}>
            <ReferenceSelect
                kind="clientes"
                title="Cliente"
                value=""
                onChange={change}
            />
        </ReferenceCatalogProvider>,
    );
    const input = screen.getByRole("combobox");
    await waitFor(() => expect(input).toBeEnabled());
    fireEvent.click(input);
    fireEvent.change(input, { target: { value: "ultimo" } });
    fireEvent.keyDown(input, { key: "Enter" });
    expect(change).toHaveBeenCalledWith(large);
    expect(
        requests.filter((r) => r.endpoint.id === "ClienteController.listar"),
    ).toHaveLength(2);
});

it("destino vindo do mapa é consultado por ID mesmo fora da página antes da confirmação", async () => {
    const code = "00000000-0000-4000-8000-000000000001";
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (r) => {
            requests.push(r);
            const data =
                r.endpoint.id === "EstoqueController.consultar"
                    ? { unidade: { codigo: code, id: "1", versao: "0" } }
                    : r.endpoint.id === "EnderecoController.listar"
                      ? { itens: [] }
                      : { id: "901", codigo: "B-03-99", armazemId: "2" };
            return {
                data,
                raw: "",
                requestId: "isolado",
                ficticio: true,
                replay: false,
            };
        },
    };
    const context = {
        clienteId: "1",
        armazemId: "2",
        codigo: code,
        enderecoId: "901",
        enderecoCodigo: "B-03-99",
    };
    const hook = renderHook(() =>
        useCollectorReading(transport, context, "OPERACAO"),
    );
    await act(async () => hook.result.current.load());
    act(() => hook.result.current.match());
    expect(hook.result.current.position).toBe("B-03-99");
    expect(hook.result.current.target?.id).toBe("901");
    expect(
        requests.find((r) => r.endpoint.id === "EnderecoController.consultar")
            ?.params.id,
    ).toBe("901");
    expect(requests.every((r) => r.endpoint.method === "GET")).toBe(true);
});
