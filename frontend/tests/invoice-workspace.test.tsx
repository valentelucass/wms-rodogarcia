import { useState } from "react";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { JourneyPage } from "../src/components/JourneyPage";
import { Navigation } from "../src/components/shell/Navigation";
import { journeys } from "../src/domain/journeys";
import { absorb, emptyWorkflow } from "../src/domain/workflow";
import { type Perfil, type Values } from "../src/contracts/runtime";
import type { Receipt, Request, Transport } from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";

const invoice = {
    id: "9007199254740993",
    versao: "4",
    clienteId: "83",
    armazemId: "46",
    contratoId: "9",
    periodoInicio: "2026-09-01",
    periodoFim: "2026-10-01",
    situacao: "APROVADO",
    versaoAtual: 2,
};
const version = {
    ...(fixture("FechamentoCobrancaDto.Versao") as Values),
    id: "9007199254741001",
    fechamentoId: invoice.id,
    numero: 2,
    calculoId: "9007199254741009",
    saldo: "18450.00",
    natureza: "DEBITO",
    situacao: "APROVADA",
    ajustes: [],
    nfse: [],
    entregas: [],
    confirmacoes: [],
};
const calculation = {
    ...(fixture("CalculoCobrancaDto.Resultado") as Values),
    id: version.calculoId,
    clienteId: invoice.clienteId,
    armazemId: invoice.armazemId,
    periodoInicio: invoice.periodoInicio,
    periodoFim: invoice.periodoFim,
    moeda: "BRL",
    total: "18450.00",
    subtotalConhecido: "18450.00",
    minimoCalculado: "0",
    grisCalculado: "0",
    pendencias: [],
    memoria: {
        servicos: [
            {
                fatoId: "501",
                servicoId: "17",
                executadoEm: "2026-09-12T12:00:00Z",
                quantidade: "12.000001",
                valor: "800.00",
                parcelas: [],
            },
        ],
        diarias: [],
        ajustes: {},
    },
};
const receipt = (data: unknown): Receipt => ({
    data,
    raw: "",
    ficticio: true,
    replay: false,
    requestId: "INVOICE-LOCAL",
});
function createTransport(
    patch: { version?: Values; calculation?: Values } = {},
) {
    const requests: Request[] = [];
    const transport: Transport = {
        send: vi.fn(async (request) => {
            requests.push(request);
            if (request.endpoint.method !== "GET")
                throw new Error("Este roteiro não autoriza gravação.");
            switch (request.endpoint.id) {
                case "FechamentoCobrancaController.listar":
                    return receipt({
                        itens: [invoice],
                        pagina: 0,
                        tamanho: 20,
                        totalItens: "1",
                        totalPaginas: 1,
                    });
                case "FechamentoCobrancaController.consultar":
                    return receipt(invoice);
                case "FechamentoCobrancaController.versao":
                    return receipt(patch.version ?? version);
                case "CalculoCobrancaController.consultar":
                    return receipt(patch.calculation ?? calculation);
                default:
                    return receipt([]);
            }
        }),
    };
    return { transport, requests };
}
function Harness({
    transport,
    perfil = "GESTOR",
    context = { clienteId: "83", armazemId: "46" },
}: {
    transport: Transport;
    perfil?: Perfil;
    context?: Values;
}) {
    const [workflow, setWorkflow] = useState(() => {
        const initial = emptyWorkflow();
        initial.catalogs["ClienteDto.Resposta"] = [
            {
                id: "83",
                nome: "Cliente de conferência",
                razaoSocial: "Cliente de conferência",
            },
        ];
        initial.catalogs["ConfiguracaoCobrancaDto.Servico"] = [
            {
                id: "17",
                codigo: "EXTRA",
                descricao: "Serviço adicional confirmado",
                situacao: "ATIVO",
            },
        ];
        return initial;
    });
    return (
        <JourneyPage
            key={`${context.clienteId}/${context.armazemId}/${perfil}`}
            journey={journeys.find((journey) => journey.id === "fechamento")!}
            transport={transport}
            perfil={perfil}
            context={context}
            workflow={workflow}
            startAction=""
            onRecord={(data, type) =>
                setWorkflow((value) => absorb(value, type, data, true))
            }
            onReceipt={(r, type, _id, request) =>
                setWorkflow((value) =>
                    absorb(value, type, r.data, false, request),
                )
            }
            onNavigate={vi.fn()}
        />
    );
}

it("reúne o menu e mostra o valor da versão certa, sem perder IDs longos ou o último dia do ciclo", async () => {
    const { transport, requests } = createTransport();
    render(
        <>
            <Navigation page="fechamento" navigate={vi.fn()} />
            <Harness transport={transport} />
        </>,
    );
    expect(
        screen.queryByRole("button", { name: "Serviços e cálculo" }),
    ).not.toBeInTheDocument();
    expect(
        screen.getByRole("button", { name: "Fechamento de Fatura" }),
    ).toHaveAttribute("aria-current", "page");
    expect(screen.queryByText("Etapas da jornada")).not.toBeInTheDocument();
    expect(await screen.findByText("R$ 18.450,00")).toBeInTheDocument();
    expect(screen.getByText("01/09/2026 a 30/09/2026")).toBeInTheDocument();
    expect(
        requests.find((request) => request.endpoint.id.endsWith(".versao"))
            ?.params,
    ).toEqual({ id: invoice.id, numero: 2 });
    expect(requests.every((request) => request.endpoint.method === "GET")).toBe(
        true,
    );
});

it("acessar fatura consulta serviços e composição reais do cálculo, mantendo envio ESL indisponível", async () => {
    const { transport, requests } = createTransport();
    render(<Harness transport={transport} />);
    fireEvent.click(
        await screen.findByRole("button", { name: "Acessar fatura" }),
    );
    const dialog = await screen.findByRole("dialog", {
        name: "Detalhes da fatura",
    });
    expect(
        await within(dialog).findByText("Serviço adicional confirmado · EXTRA"),
    ).toBeInTheDocument();
    expect(within(dialog).getByText("R$ 800,00")).toBeInTheDocument();
    expect(
        within(dialog).getByRole("button", { name: "Enviar minuta ao ESL" }),
    ).toBeDisabled();
    expect(
        within(dialog).getByText(/ambiente próprio da Dalga/),
    ).toBeInTheDocument();
    expect(
        requests.find(
            (request) =>
                request.endpoint.id === "CalculoCobrancaController.consultar",
        )?.params.id,
    ).toBe(version.calculoId);
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Fechar diálogo" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Novo fechamento" }));
    expect(
        within(
            await screen.findByRole("dialog", { name: "Novo fechamento" }),
        ).queryByRole("button", {
            name: "Guardar versão consultada como origem do ajuste",
        }),
    ).not.toBeInTheDocument();
    expect(requests.every((request) => request.endpoint.method === "GET")).toBe(
        true,
    );
});

it("recusa versão de outra fatura sem mostrar zero ou oferecer confirmação", async () => {
    const { transport, requests } = createTransport({
        version: { ...version, fechamentoId: "99" },
    });
    render(<Harness transport={transport} />);
    expect(await screen.findByText(/Valor indisponível/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Acessar fatura" }));
    const dialog = await screen.findByRole("dialog");
    expect(
        await within(dialog).findByText(/versão recebida não corresponde/),
    ).toBeInTheDocument();
    expect(
        within(dialog).queryByRole("button", { name: "Confirmar fechamento" }),
    ).not.toBeInTheDocument();
    expect(
        requests.some(
            (request) =>
                request.endpoint.id === "CalculoCobrancaController.consultar",
        ),
    ).toBe(false);
});

it("cálculo de outro cliente não aparece na fatura e valores pendentes não viram zero", async () => {
    const { transport } = createTransport({
        version: { ...version, saldo: null },
        calculation: { ...calculation, clienteId: "999" },
    });
    render(<Harness transport={transport} />);
    expect(await screen.findByText(/Pendente de cálculo/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Acessar fatura" }));
    const dialog = await screen.findByRole("dialog");
    expect(
        await within(dialog).findByText(/cálculo recebido pertence a outra/),
    ).toBeInTheDocument();
    expect(
        within(dialog).queryByText(/Serviço adicional confirmado/),
    ).not.toBeInTheDocument();
    expect(within(dialog).queryByText("R$ 0,00")).not.toBeInTheDocument();
});

it("novo fechamento conserva os lançamentos de serviços e só prepara após calcular; Operação não consulta financeiro", async () => {
    const { transport, requests } = createTransport();
    const rendered = render(<Harness transport={transport} />);
    fireEvent.click(screen.getByRole("button", { name: "Novo fechamento" }));
    const dialog = await screen.findByRole("dialog", {
        name: "Novo fechamento",
    });
    expect(
        within(dialog).getByText("Conferir ou incluir serviços realizados"),
    ).toBeInTheDocument();
    expect(
        within(dialog).getByText("Conferir avarias na cobrança"),
    ).toBeInTheDocument();
    expect(
        within(dialog).queryByRole("button", { name: "Preparar fatura" }),
    ).not.toBeInTheDocument();
    expect(requests.every((request) => request.endpoint.method === "GET")).toBe(
        true,
    );
    rendered.unmount();
    const denied = createTransport();
    render(<Harness transport={denied.transport} perfil="OPERACAO" />);
    await act(async () => {});
    expect(
        screen.getByText("Seu perfil não permite consultar faturas."),
    ).toBeInTheDocument();
    expect(denied.requests).toHaveLength(0);
});

it("troca de contexto descarta respostas financeiras atrasadas", async () => {
    const pending: ((value: Receipt) => void)[] = [];
    const transport: Transport = {
        send: async (request) => {
            if (request.endpoint.id === "FechamentoCobrancaController.listar")
                return receipt({
                    itens: [invoice],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "1",
                    totalPaginas: 1,
                });
            return new Promise((resolve) => pending.push(resolve));
        },
    };
    const rendered = render(<Harness transport={transport} />);
    await waitFor(() => expect(pending).toHaveLength(1));
    rendered.rerender(
        <Harness
            transport={transport}
            context={{ clienteId: "999", armazemId: "46" }}
            perfil="OPERACAO"
        />,
    );
    await act(async () => pending[0](receipt(version)));
    expect(screen.queryByText("R$ 18.450,00")).not.toBeInTheDocument();
    expect(
        screen.queryByRole("button", { name: "Acessar fatura" }),
    ).not.toBeInTheDocument();
});

it("calcula, prepara e confirma a fatura com as identidades retornadas, sem enviar ao ESL", async () => {
    const requests: Request[] = [];
    let currentInvoice: Values | undefined;
    let currentVersion: Values = { ...version, situacao: "PENDENTE_REVISAO" };
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            switch (request.endpoint.id) {
                case "FechamentoCobrancaController.listar":
                    return receipt({
                        itens: currentInvoice ? [currentInvoice] : [],
                        pagina: 0,
                        tamanho: 20,
                        totalItens: currentInvoice ? "1" : "0",
                        totalPaginas: 1,
                    });
                case "CalculoCobrancaController.calcular":
                    return receipt({ ...calculation, situacao: "COMPLETO" });
                case "FechamentoCobrancaController.preparar":
                    currentInvoice = {
                        ...invoice,
                        situacao: "EM_REVISAO",
                        versao: "5",
                    };
                    return receipt({
                        fechamento: currentInvoice,
                        versao: currentVersion,
                    });
                case "FechamentoCobrancaController.consultar":
                    return receipt(currentInvoice);
                case "FechamentoCobrancaController.versao":
                    return receipt(currentVersion);
                case "CalculoCobrancaController.consultar":
                    return receipt(calculation);
                case "FechamentoCobrancaController.aprovar":
                    currentInvoice = {
                        ...currentInvoice,
                        situacao: "APROVADO",
                        versao: "6",
                    };
                    currentVersion = {
                        ...currentVersion,
                        situacao: "APROVADA",
                    };
                    return receipt({
                        fechamento: currentInvoice,
                        versao: currentVersion,
                    });
                default:
                    throw new Error(
                        "Rota não prevista neste fluxo de conferência.",
                    );
            }
        },
    };
    render(<Harness transport={transport} />);
    fireEvent.click(screen.getByRole("button", { name: "Novo fechamento" }));
    const newDialog = await screen.findByRole("dialog", {
        name: "Novo fechamento",
    });
    for (const [label, value] of [
        ["Início do período *", "2026-09-01"],
        ["Fim exclusivo do período *", "2026-10-01"],
        [
            "Motivo / justificativa *",
            "Conferência dos serviços realizados no período",
        ],
    ])
        fireEvent.change(within(newDialog).getByLabelText(label), {
            target: { value },
        });
    fireEvent.click(
        within(newDialog).getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(
        await within(newDialog).findByRole("button", {
            name: "Confirmar agora",
        }),
    );
    fireEvent.click(
        await within(newDialog).findByRole("button", {
            name: "Preparar fechamento com este cálculo",
        }),
    );
    let dialog = await screen.findByRole("dialog", { name: "Preparar fatura" });
    fireEvent.change(
        within(dialog).getByLabelText("Motivo / justificativa *"),
        { target: { value: "Preparação da fatura com o cálculo conferido" } },
    );
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(
        await within(dialog).findByRole("button", { name: "Confirmar agora" }),
    );
    dialog = await screen.findByRole("dialog", { name: "Detalhes da fatura" });
    expect(
        await within(dialog).findByText("Serviços cobrados"),
    ).toBeInTheDocument();
    fireEvent.click(
        await within(dialog).findByRole("button", {
            name: "Confirmar fechamento",
        }),
    );
    fireEvent.change(
        within(dialog).getByLabelText("Motivo / justificativa *"),
        { target: { value: "Aprovação integral após conferir toda a fatura" } },
    );
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(
        await within(dialog).findByRole("button", { name: "Confirmar agora" }),
    );
    await waitFor(() =>
        expect(
            requests.some(
                (request) =>
                    request.endpoint.id ===
                    "FechamentoCobrancaController.aprovar",
            ),
        ).toBe(true),
    );
    const writes = requests.filter(
        (request) => request.endpoint.method !== "GET",
    );
    expect(writes.map((request) => request.endpoint.id)).toEqual([
        "CalculoCobrancaController.calcular",
        "FechamentoCobrancaController.preparar",
        "FechamentoCobrancaController.aprovar",
    ]);
    expect(writes[1].body).toMatchObject({ calculoId: version.calculoId });
    expect(writes[2].params.id).toBe(invoice.id);
    expect(writes[2].body).toMatchObject({ versao: "5", numero: 2 });
});
