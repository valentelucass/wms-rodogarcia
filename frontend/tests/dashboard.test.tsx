import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import {
    OperationDashboard,
    barShare,
    formatQuantity,
} from "../src/components/dashboard/OperationDashboard";
import { FictitiousTransport } from "../src/api/fictitious";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { dashboardExample } from "../src/api/mock/dashboard";
import type { DashboardDto_Resumo } from "../src/contracts/types";
import { toWire, stringifyExact, parseResponse } from "../src/contracts/codec";

const context = { clienteId: "9007199254740993", armazemId: "2" };
const receipt = (r: Request, data: DashboardDto_Resumo): Receipt => {
    const raw = stringifyExact(toWire(r.endpoint.response, data));
    return {
        data: parseResponse(r.endpoint.response, raw),
        raw,
        requestId: "dashboard-test",
        ficticio: true,
        replay: false,
    };
};

it("dashboard consulta agregados, preserva contexto Long, separa unidades e pagina no servidor", async () => {
    const transport = new FictitiousTransport("OPERACAO");
    const navigate = vi.fn();
    render(
        <OperationDashboard
            transport={transport}
            context={context}
            perfil="OPERACAO"
            navigate={navigate}
        />,
    );
    await screen.findByText("DEMO-BOBINA");
    expect(transport.requests[0].query).toMatchObject({
        ...context,
        pagina: 0,
        tamanho: 6,
    });
    expect(transport.requests[0].endpoint.id).toBe(
        "DashboardController.consultar",
    );
    expect(screen.getByText("12.500,25")).toBeInTheDocument();
    expect(screen.queryByText(/livres de/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Próxima" }));
    await screen.findByText("DEMO-VOLUME");
    expect(transport.requests.at(-1)?.query.pagina).toBe(1);
    expect(screen.queryByText("DEMO-BOBINA")).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Abrir saídas" }));
    expect(navigate).toHaveBeenCalledWith("saida");
});

it("ausência de contexto não consulta; aviso sem configuração e estoque vazio não viram dados inventados", async () => {
    const send = vi.fn(async (r: Request) =>
        receipt(r, {
            ...dashboardExample(r, "GESTOR", true),
            unidadesComAviso: null,
            antecedenciaValidade: null,
        }),
    );
    const props = {
        transport: { send },
        perfil: "GESTOR" as const,
        navigate: vi.fn(),
    };
    const view = render(<OperationDashboard {...props} context={{}} />);
    expect(send).not.toHaveBeenCalled();
    expect(
        screen.getByRole("button", { name: "Atualizar indicadores" }),
    ).toBeDisabled();
    view.rerender(<OperationDashboard {...props} context={context} />);
    await screen.findByText("Antecedência ainda não configurada");
    expect(screen.getByText("—")).toBeInTheDocument();
    expect(
        screen.getByText("Nenhum pedido de saída aberto."),
    ).toBeInTheDocument();
    expect(
        screen.getByText("Nenhum produto nesta página do contexto."),
    ).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
});

it("resposta atrasada de outro contexto é descartada mesmo quando o transporte ignora abort", async () => {
    const pending: Array<{ request: Request; resolve: (r: Receipt) => void }> =
        [];
    const transport: Transport = {
        send: vi.fn(
            (r) =>
                new Promise<Receipt>((resolve) =>
                    pending.push({ request: r, resolve }),
                ),
        ),
    };
    const props = { transport, perfil: "GESTOR" as const, navigate: vi.fn() };
    const view = render(
        <OperationDashboard
            {...props}
            context={{ clienteId: "1", armazemId: "1" }}
        />,
    );
    await waitFor(() => expect(pending).toHaveLength(1));
    view.rerender(<OperationDashboard {...props} context={context} />);
    await waitFor(() => expect(pending).toHaveLength(2));
    expect(pending[0].request.signal.aborted).toBe(true);
    const latest = dashboardExample(pending[1].request, "GESTOR", true);
    pending[1].resolve(receipt(pending[1].request, latest));
    await screen.findByText("Nenhum produto nesta página do contexto.");
    pending[0].resolve(
        receipt(
            pending[0].request,
            dashboardExample(pending[0].request, "GESTOR", false),
        ),
    );
    await waitFor(() =>
        expect(screen.queryByText("DEMO-BOBINA")).not.toBeInTheDocument(),
    );
    expect(screen.getByText(/Cliente 9007199254740993/)).toBeInTheDocument();
});

it("recusa de acesso remove os indicadores e atualização é uma consulta explícita", async () => {
    let denied = false;
    const send = vi.fn(async (r: Request) => {
        if (denied) throw new ApiError("Recusado", 403, "ACESSO_NEGADO");
        return receipt(r, dashboardExample(r, "GESTOR", false));
    });
    render(
        <OperationDashboard
            transport={{ send }}
            context={context}
            perfil="GESTOR"
            navigate={vi.fn()}
        />,
    );
    await screen.findByText("DEMO-BOBINA");
    denied = true;
    fireEvent.click(
        screen.getByRole("button", { name: "Atualizar indicadores" }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Seu acesso não permite consultar este contexto.",
    );
    expect(screen.queryByText("DEMO-BOBINA")).not.toBeInTheDocument();
    expect(send).toHaveBeenCalledTimes(2);
});

it("erro de transporte aparece separado de zero e os contadores mantêm precisão", async () => {
    render(
        <OperationDashboard
            transport={{
                send: async () => {
                    throw new ApiError("Falha", 503, "INDISPONIVEL");
                },
            }}
            context={context}
            perfil="GESTOR"
            navigate={vi.fn()}
        />,
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Não foi possível consultar",
    );
    expect(
        within(screen.getByRole("alert")).queryByText("0"),
    ).not.toBeInTheDocument();
    expect(formatQuantity("9007199254740993")).toBe("9.007.199.254.740.993");
    expect(formatQuantity("9999999999999.999999")).toBe(
        "9.999.999.999.999,999999",
    );
    expect(barShare("9007199254740993", "90071992547409930")).toBe(100);
    expect(barShare("0", "0")).toBe(0);
});
