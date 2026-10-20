import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import {
    ApiError,
    type Request,
    type Receipt,
    type Transport,
} from "../src/api/client";
import { overviewExample } from "../src/api/mock/overview";
import { WarehouseOverview } from "../src/components/dashboard/WarehouseOverview";
import { FictitiousTransport } from "../src/api/fictitious";

const period = {
    clienteId: "9007199254741001",
    armazemId: "9007199254741002",
    contratoId: "9007199254741003",
    moeda: "BRL",
    fuso: "America/Sao_Paulo",
    cicloInicio: "2026-09-25",
    cicloFimExclusivo: "2026-10-25",
    apuradoInicio: "2026-09-25",
    apuradoFimExclusivo: "2026-10-20",
    incluiDiaAtual: true,
    subtotalConhecido: "2000.67",
    minimoCalculado: "300.00",
    grisCalculado: "45.00",
    total: "2345.67",
    pendencias: [],
};
const preview = {
    competencia: "2026-10",
    referenciaEm: "2026-10-20T15:25:30Z",
    incluiDiaAtual: true,
    moeda: "BRL",
    completo: true,
    conhecido: "2345.67",
    total: "2345.67",
    periodos: [period],
    pendencias: [],
};

function response(request: Request, value: unknown, permitted = true): Receipt {
    return {
        data:
            request.endpoint.id === "VisaoOperacaoController.preverFaturamento"
                ? value
                : {
                      ...overviewExample(request, "GESTOR", true),
                      // O legado fiscal difere de propósito da previsão operacional.
                      faturamentoMes: "9999.00",
                      financeiroPermitido: permitted,
                  },
        ficticio: true,
        replay: false,
        requestId: "BILLING-PREVIEW-LOCAL",
        raw: "",
    };
}

function setup(value: unknown, permitted = true) {
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (
                request.endpoint.id ===
                    "VisaoOperacaoController.preverFaturamento" &&
                value === undefined
            )
                throw new ApiError(
                    "Rota fictícia ausente",
                    404,
                    "NAO_ENCONTRADO",
                );
            return response(request, value, permitted);
        },
    };
    render(
        <WarehouseOverview
            transport={transport}
            perfil={permitted ? "GESTOR" : "OPERACAO"}
            context={{
                clienteId: period.clienteId,
                armazemId: period.armazemId,
            }}
            onScope={vi.fn()}
            onAddress={vi.fn()}
        />,
    );
    return requests;
}

const card = () =>
    screen.getByRole("article", { name: "Faturamento previsto do mês" });

it("mostra a previsão devolvida pelo backend sem depender do valor fiscal legado", async () => {
    const requests = setup(preview);
    await within(card()).findByText("R$ 2.345,67");
    expect(card()).toHaveTextContent("Prévia automática · 2026-10");
    expect(card()).not.toHaveTextContent("9.999");
    expect(requests).toHaveLength(2);
    const request = requests.find(
        (value) =>
            value.endpoint.id === "VisaoOperacaoController.preverFaturamento",
    )!;
    expect(request.query).toMatchObject({
        clienteId: period.clienteId,
        armazemId: period.armazemId,
    });
    expect(request.endpoint.method).toBe("GET");
});

it("detalha ciclo, período apurado e valores recebidos sem recalcular mínimo ou GRIS", async () => {
    const requests = setup(preview);
    await within(card()).findByText("R$ 2.345,67");
    fireEvent.click(
        within(card()).getByRole("button", {
            name: "Ver períodos e pendências",
        }),
    );
    const dialog = screen.getByRole("dialog", {
        name: "Previsão de faturamento",
    });
    expect(dialog).toHaveTextContent("25/09/2026 a 25/10/2026");
    expect(dialog).toHaveTextContent("25/09/2026 a 20/10/2026");
    expect(dialog).toHaveTextContent("Dia final não incluído");
    expect(dialog).toHaveTextContent(
        "Inclui as operações de hoje até a referência",
    );
    expect(dialog).toHaveTextContent("R$ 2.000,67");
    expect(dialog).toHaveTextContent("R$ 300,00");
    expect(dialog).toHaveTextContent("R$ 45,00");
    expect(requests).toHaveLength(2);
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Fechar diálogo" }),
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

it.each([
    ["1143.20", "R$ 1.143,20", 236],
    ["123.456", "R$ 123,46", 1],
    ["0.00", "R$ 0,00", 1],
])(
    "destaca o conhecido %s como parcial e mantém o total incompleto indisponível",
    async (known, formatted, pendingCount) => {
        setup({
            ...preview,
            completo: false,
            conhecido: known,
            total: null,
            periodos: [{ ...period, total: null, minimoCalculado: null }],
            pendencias: Array.from({ length: pendingCount }, () => ({
                clienteId: period.clienteId,
                armazemId: period.armazemId,
                codigo: "PRECO_AUSENTE",
                data: "2026-10-20",
                unidadeId: null,
                fatoId: "9007199254741009",
                detalhe: "Preço do serviço não informado na tabela do cliente.",
            })),
        });
        await within(card()).findByText("Parcial — cálculo incompleto");
        expect(card().querySelector("strong")).toHaveTextContent(formatted);
        expect(card()).toHaveTextContent(
            `${pendingCount} ${pendingCount === 1 ? "pendência" : "pendências"}`,
        );
        fireEvent.click(
            within(card()).getByRole("button", {
                name: "Ver períodos e pendências",
            }),
        );
        const dialog = screen.getByRole("dialog");
        const mainValue = dialog.querySelector(
            ".billing-preview-details > dl > div",
        )!;
        expect(mainValue).toHaveTextContent("Valor conhecido (parcial)");
        expect(mainValue).toHaveTextContent(formatted);
        expect(
            within(dialog).getByText("Total previsto", { exact: true })
                .parentElement,
        ).toHaveTextContent("Indisponível até concluir o cálculo");
        expect(dialog).toHaveTextContent(
            "Preço do serviço não informado na tabela do cliente.",
        );
        expect(dialog).toHaveTextContent("9007199254741009");
        expect(dialog).toHaveTextContent("PRECO_AUSENTE");
    },
);

it.each([
    ["endpoint ausente no backend antigo", undefined, true],
    ["consulta sem previsão", null, true],
    ["perfil sem acesso financeiro", preview, false],
])(
    "não usa o legado fiscal ou zero quando há %s",
    async (_name, value, permitted) => {
        setup(value, permitted);
        await screen.findByText("0%", { exact: true });
        expect(card().querySelector("strong")).toHaveTextContent("—");
        expect(card()).not.toHaveTextContent("9.999");
        expect(
            within(card()).queryByRole("button", {
                name: "Ver períodos e pendências",
            }),
        ).not.toBeInTheDocument();
    },
);

it.each([null, "123.45"])(
    "mantém agregado sem moeda única indisponível mesmo com conhecido %s",
    async (known) => {
        setup({
            ...preview,
            completo: false,
            moeda: null,
            conhecido: known,
            total: null,
            periodos: [
                period,
                { ...period, moeda: "USD", contratoId: "9007199254741004" },
            ],
            pendencias: [
                {
                    clienteId: period.clienteId,
                    armazemId: period.armazemId,
                    codigo: "MOEDAS_DIVERGENTES",
                    data: null,
                    unidadeId: null,
                    fatoId: null,
                    detalhe: "Os períodos consultados têm moedas diferentes.",
                },
            ],
        });
        await within(card()).findByText("Prévia incompleta");
        expect(card().querySelector("strong")).toHaveTextContent("—");
        expect(card()).not.toHaveTextContent("R$ 0,00");
        fireEvent.click(
            within(card()).getByRole("button", {
                name: "Ver períodos e pendências",
            }),
        );
        const dialog = screen.getByRole("dialog");
        expect(dialog).toHaveTextContent("USD 2.345,67");
        expect(dialog).toHaveTextContent(
            "Os períodos consultados têm moedas diferentes.",
        );
        expect(
            dialog.querySelector(".billing-preview-details > dl > div"),
        ).toHaveTextContent("Valor conhecido (parcial)—");
    },
);

it("não inventa parcial quando a moeda é conhecida mas o subtotal não foi informado", async () => {
    setup({ ...preview, completo: false, conhecido: null, total: null });
    await within(card()).findByText("Prévia incompleta");
    expect(card().querySelector("strong")).toHaveTextContent("—");
    expect(card()).not.toHaveTextContent("R$ 0,00");
    fireEvent.click(
        within(card()).getByRole("button", {
            name: "Ver períodos e pendências",
        }),
    );
    const aggregate = screen
        .getByRole("dialog")
        .querySelector(".billing-preview-details > dl > div");
    expect(aggregate).toHaveTextContent("Valor conhecido (parcial)—");
});

it("exibe zero somente quando a prévia completa confirma total zero", async () => {
    setup({ ...preview, total: "0.00", conhecido: "0.00", periodos: [] });
    await within(card()).findByText("R$ 0,00");
    expect(card()).toHaveTextContent("Prévia automática · 2026-10");
});

it("explica a ausência de moeda única mesmo em prévia completa sem períodos", async () => {
    setup({
        ...preview,
        moeda: null,
        total: "0.00",
        conhecido: "0.00",
        periodos: [],
    });
    await within(card()).findByText("Sem moeda única informada");
    expect(card().querySelector("strong")).toHaveTextContent("—");
    expect(card()).not.toHaveTextContent("0,00");
    fireEvent.click(
        within(card()).getByRole("button", {
            name: "Ver períodos e pendências",
        }),
    );
    const aggregate = screen
        .getByRole("dialog")
        .querySelector(".billing-preview-details > dl > div");
    expect(aggregate).toHaveTextContent("Total previsto—");
    expect(screen.getByRole("dialog")).toHaveTextContent(
        "Sem moeda única informada",
    );
});

it("espera a consulta e remove a previsão anterior após falha de atualização", async () => {
    let finish!: (receipt: Receipt) => void;
    let fail = false;
    let request!: Request;
    const transport: Transport = {
        send: async (value) => {
            if (
                value.endpoint.id !==
                "VisaoOperacaoController.preverFaturamento"
            )
                return response(value, preview);
            request = value;
            if (fail) throw new ApiError("Falha fictícia", 503, "INDISPONIVEL");
            return new Promise<Receipt>((resolve) => {
                finish = resolve;
            });
        },
    };
    render(
        <WarehouseOverview
            transport={transport}
            perfil="GESTOR"
            context={{}}
            onScope={vi.fn()}
            onAddress={vi.fn()}
        />,
    );
    expect(card().querySelector("strong")).toHaveTextContent("—");
    expect(card()).not.toHaveTextContent("R$ 0,00");
    await act(async () => finish(response(request, preview)));
    expect(card().querySelector("strong")).toHaveTextContent("R$ 2.345,67");
    fail = true;
    fireEvent.click(screen.getByRole("button", { name: "Atualizar visão" }));
    await screen.findByRole("alert");
    expect(card().querySelector("strong")).toHaveTextContent("—");
    expect(card()).not.toHaveTextContent("2.345");
    expect(card()).not.toHaveTextContent("R$ 0,00");
    expect(card()).toHaveTextContent(
        "Use Atualizar visão para tentar novamente.",
    );
    expect(
        within(card()).queryByRole("button", { name: "Atualizar previsão" }),
    ).not.toBeInTheDocument();
    fail = false;
    fireEvent.click(screen.getByRole("button", { name: "Atualizar visão" }));
    expect(card()).toHaveAttribute("aria-busy", "true");
    expect(card().querySelector("strong")).toHaveTextContent("—");
    await act(async () => finish(response(request, preview)));
    expect(card().querySelector("strong")).toHaveTextContent("R$ 2.345,67");
    expect(within(card()).queryByRole("alert")).not.toBeInTheDocument();
});

it.each(["GESTOR", "OPERACAO"] as const)(
    "o exercício fictício faz roundtrip do novo contrato para %s",
    async (perfil) => {
        const transport = new FictitiousTransport(perfil);
        render(
            <WarehouseOverview
                transport={transport}
                perfil={perfil}
                context={{ clienteId: "1", armazemId: "1" }}
                onScope={vi.fn()}
                onAddress={vi.fn()}
            />,
        );
        await screen.findByText("3%", { exact: true });
        if (perfil === "GESTOR") {
            expect(card().querySelector("strong")).toHaveTextContent(
                "R$ 2.950,00",
            );
            expect(card()).not.toHaveTextContent("2.345");
        } else {
            expect(card().querySelector("strong")).toHaveTextContent("—");
            expect(card()).toHaveTextContent(
                "Disponível para Supervisor e Gestor",
            );
            expect(card()).not.toHaveTextContent("2.950");
        }
        expect(transport.requests).toHaveLength(perfil === "GESTOR" ? 2 : 1);
        expect(
            transport.requests.filter(
                (value) =>
                    value.endpoint.id ===
                    "VisaoOperacaoController.preverFaturamento",
            ),
        ).toHaveLength(perfil === "GESTOR" ? 1 : 0);
        expect(transport.requests[0].endpoint.method).toBe("GET");
    },
);

it("prévia pendente não segura o mapa e filtros/paginação não repetem apuração financeira", async () => {
    const requests: Request[] = [];
    let finish!: (value: Receipt) => void;
    let financial!: Request;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (
                request.endpoint.id ===
                "VisaoOperacaoController.preverFaturamento"
            ) {
                financial = request;
                return new Promise<Receipt>((resolve) => {
                    finish = resolve;
                });
            }
            const data = overviewExample(request, "GESTOR", false);
            if (!("mapa" in data)) throw Error("Consulta fictícia inesperada");
            return {
                ...response(request, preview),
                data: {
                    ...data,
                    mapa: { ...data.mapa, totalPaginas: 2, totalItens: "160" },
                },
            };
        },
    };
    render(
        <WarehouseOverview
            transport={transport}
            perfil="GESTOR"
            context={{ armazemId: "1" }}
            onScope={vi.fn()}
            onAddress={vi.fn()}
        />,
    );
    await screen.findByText("3%", { exact: true });
    expect(
        screen.getByRole("region", { name: "Visão geral da operação" }),
    ).toHaveAttribute("aria-busy", "false");
    expect(card()).toHaveAttribute("aria-busy", "true");
    expect(card().querySelector("strong")).toHaveTextContent("—");
    fireEvent.click(screen.getByRole("button", { name: "Próxima" }));
    await screen.findByText("Página 2 de 2");
    await waitFor(() =>
        expect(screen.getByRole("button", { name: "Anterior" })).toBeEnabled(),
    );
    fireEvent.click(screen.getByRole("button", { name: "Filtros" }));
    fireEvent.change(screen.getByLabelText("Mostrar posições"), {
        target: { value: "OCUPADO" },
    });
    await waitFor(() =>
        expect(
            requests.filter(
                (value) =>
                    value.endpoint.id === "VisaoOperacaoController.consultar",
            ),
        ).toHaveLength(3),
    );
    expect(
        requests.filter(
            (value) =>
                value.endpoint.id ===
                "VisaoOperacaoController.preverFaturamento",
        ),
    ).toHaveLength(1);
    await act(async () => finish(response(financial, preview)));
    expect(card().querySelector("strong")).toHaveTextContent("R$ 2.345,67");
    fireEvent.click(screen.getByRole("button", { name: "Atualizar visão" }));
    await waitFor(() =>
        expect(
            requests.filter(
                (value) =>
                    value.endpoint.id === "VisaoOperacaoController.consultar",
            ),
        ).toHaveLength(4),
    );
    expect(
        requests.filter(
            (value) =>
                value.endpoint.id ===
                "VisaoOperacaoController.preverFaturamento",
        ),
    ).toHaveLength(2);
    await waitFor(() =>
        expect(
            screen.getByRole("region", { name: "Visão geral da operação" }),
        ).toHaveAttribute("aria-busy", "false"),
    );
    expect(card()).toHaveAttribute("aria-busy", "true");
    expect(card().querySelector("strong")).toHaveTextContent("—");
    expect(card()).not.toHaveTextContent("R$ 0,00");
    await act(async () => finish(response(financial, preview)));
    expect(card().querySelector("strong")).toHaveTextContent("R$ 2.345,67");
});

it("trocar contexto aborta a prévia anterior e recusa sua resposta atrasada", async () => {
    let finish!: (value: Receipt) => void;
    let old!: Request;
    const current = {
        ...preview,
        conhecido: "2.00",
        total: "2.00",
        periodos: [
            { ...period, clienteId: "2", armazemId: "2", total: "2.00" },
        ],
    };
    const transport: Transport = {
        send: async (request) => {
            if (
                request.endpoint.id !==
                "VisaoOperacaoController.preverFaturamento"
            )
                return response(request, preview);
            if (request.query.clienteId === "1") {
                old = request;
                return new Promise<Receipt>((resolve) => {
                    finish = resolve;
                });
            }
            return response(request, current);
        },
    };
    const props = {
        transport,
        perfil: "GESTOR" as const,
        onScope: vi.fn(),
        onAddress: vi.fn(),
    };
    const view = render(
        <WarehouseOverview
            {...props}
            context={{ clienteId: "1", armazemId: "1" }}
        />,
    );
    expect(card()).toHaveAttribute("aria-busy", "true");
    view.rerender(
        <WarehouseOverview
            {...props}
            context={{ clienteId: "2", armazemId: "2" }}
        />,
    );
    await within(card()).findByText("R$ 2,00");
    expect(old.signal.aborted).toBe(true);
    await act(async () => finish(response(old, preview)));
    expect(card().querySelector("strong")).toHaveTextContent("R$ 2,00");
    expect(card()).not.toHaveTextContent("2.345");
});
