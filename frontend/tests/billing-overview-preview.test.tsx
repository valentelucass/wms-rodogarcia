import { act, fireEvent, render, screen, within } from "@testing-library/react";
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
        data: {
            ...overviewExample(request, "GESTOR", true),
            // O legado fiscal difere de propósito da previsão operacional.
            faturamentoMes: "9999.00",
            financeiroPermitido: permitted,
            previsaoFaturamento: value,
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
            return response(request, value, permitted);
        },
    };
    render(
        <WarehouseOverview
            transport={transport}
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
    expect(requests).toHaveLength(1);
    expect(requests[0].query).toMatchObject({
        clienteId: period.clienteId,
        armazemId: period.armazemId,
    });
    expect(requests[0].endpoint.id).toBe("VisaoOperacaoController.consultar");
    expect(requests[0].endpoint.method).toBe("GET");
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
    expect(requests).toHaveLength(1);
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Fechar diálogo" }),
    );
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
});

it.each([
    ["123.456", "R$ 123,46"],
    ["0.00", "R$ 0,00"],
])(
    "mantém o total indisponível com pendência e subtotal conhecido %s",
    async (known, formatted) => {
        setup({
            ...preview,
            completo: false,
            conhecido: known,
            total: null,
            periodos: [{ ...period, total: null, minimoCalculado: null }],
            pendencias: [
                {
                    clienteId: period.clienteId,
                    armazemId: period.armazemId,
                    codigo: "PRECO_AUSENTE",
                    data: "2026-10-20",
                    unidadeId: null,
                    fatoId: "9007199254741009",
                    detalhe:
                        "Preço do serviço não informado na tabela do cliente.",
                },
            ],
        });
        await within(card()).findByText(`Valor conhecido: ${formatted}`);
        expect(card().querySelector("strong")).toHaveTextContent("—");
        expect(card()).toHaveTextContent("Prévia incompleta · 1 pendência");
        fireEvent.click(
            within(card()).getByRole("button", {
                name: "Ver períodos e pendências",
            }),
        );
        const dialog = screen.getByRole("dialog");
        expect(dialog).toHaveTextContent(
            "Preço do serviço não informado na tabela do cliente.",
        );
        expect(dialog).toHaveTextContent("9007199254741009");
        expect(dialog).toHaveTextContent("PRECO_AUSENTE");
    },
);

it.each([
    ["objeto ausente no backend antigo", undefined, true],
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

it("respeita moeda por período e não converte agregação de moedas divergentes em zero", async () => {
    setup({
        ...preview,
        completo: false,
        moeda: null,
        conhecido: null,
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
    await within(card()).findByText("Prévia incompleta · 1 pendência");
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
});

it("exibe zero somente quando a prévia completa confirma total zero", async () => {
    setup({ ...preview, total: "0.00", conhecido: "0.00", periodos: [] });
    await within(card()).findByText("R$ 0,00");
    expect(card()).toHaveTextContent("Prévia automática · 2026-10");
});

it("espera a consulta e remove a previsão anterior após falha de atualização", async () => {
    let finish!: (receipt: Receipt) => void;
    let fail = false;
    let request!: Request;
    const transport: Transport = {
        send: async (value) => {
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
});

it.each(["GESTOR", "OPERACAO"] as const)(
    "o exercício fictício faz roundtrip do novo contrato para %s",
    async (perfil) => {
        const transport = new FictitiousTransport(perfil);
        render(
            <WarehouseOverview
                transport={transport}
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
        expect(transport.requests).toHaveLength(1);
        expect(transport.requests[0].endpoint.method).toBe("GET");
    },
);
