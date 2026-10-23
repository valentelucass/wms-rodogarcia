import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { JourneyPage } from "../src/components/JourneyPage";
import { emptyWorkflow } from "../src/domain/workflow";
import { journeys } from "../src/domain/journeys";
import type { Perfil } from "../src/contracts/runtime";
import { businessTitle } from "../src/domain/referencePresentation";

const row = {
    id: "9007199254740993",
    versao: "4",
    referencia: "SAI-TESTE",
    criadoEm: "2026-10-23T12:00:00Z",
    clienteId: "83",
    cliente: "Proprietário de teste",
    armazemId: "46",
    situacaoOperacional: "SEPARADO",
    situacaoFiscal: "PENDENTE",
    numerosNfe: [],
    podeEmitir: true,
};
const receipt = (data: unknown): Receipt => ({
    data,
    raw: "",
    ficticio: true,
    replay: false,
    requestId: "FISCAL-TESTE",
});
const page = (itens: unknown[]) => ({
    itens,
    pagina: 0,
    tamanho: 25,
    totalItens: String(itens.length),
    totalPaginas: 1,
});
function prepared(
    options: { uncertain?: boolean; blocked?: boolean; pending?: boolean } = {},
) {
    const requests: Request[] = [];
    let failed = false;
    const andamento = {
        pedidoId: row.id,
        situacao: "PENDENTE",
        solicitacoes: [],
    };
    const doc = {
        grupo: "CONSOLIDADO",
        natureza: "RETORNO_MERCADORIA",
        emitenteCnpj: "11111111000191",
        destinatarioDocumento: "22222222000191",
        destinatarioNome: "Proprietário de teste",
        naturezaOperacao: "Retorno de depósito",
        finalidade: "4",
        ambiente: 2,
        documentoOrigemId: null,
        itens: [
            {
                reservaId: "7",
                entradaOrigemId: "8",
                itemNotaOrigemId: "9",
                notaOrigemId: "10",
                produtoId: "11",
                sku: "SKU-1",
                descricao: "Produto de teste",
                unidade: "PC",
                quantidade: "2.000001",
                valorUnitario: "5.00",
                valorTotal: "10.00",
                ncm: "39269090",
                cfop: "5906",
                chaveOrigem: "1".repeat(44),
                revisaoConteudo: "3",
            },
        ],
        coberturas: [],
        chavesReferenciadas: ["1".repeat(44)],
        valorTotal: "10.00",
        observacao: "Saída WMS: SAI-TESTE",
    };
    const transport: Transport = {
        send: vi.fn(async (r) => {
            requests.push(r);
            switch (r.endpoint.id) {
                case "FiscalNfeController.listar":
                    return receipt(page([row]));
                case "ExpedicaoController.consultar":
                    return receipt({
                        pedido: {
                            ...row,
                            itens: [
                                {
                                    id: "1",
                                    sku: "SKU-1",
                                    quantidade: "2.000001",
                                },
                            ],
                            situacao: "SEPARADO",
                        },
                        documentos: [],
                        baixas: [],
                        devolucoes: [],
                        retiradaEm: null,
                    });
                case "FiscalNfeController.andamento":
                    return receipt(andamento);
                case "FiscalNfeController.previa":
                    return receipt({
                        pedidoId: row.id,
                        versao: "4",
                        hash: "a".repeat(64),
                        podeEmitir: !options.blocked,
                        integracaoHabilitada: true,
                        pendencias: options.blocked
                            ? ["Produto sem valor fiscal da entrada."]
                            : [],
                        documentos: [doc],
                        expedicao: null,
                    });
                case "FiscalNfeController.confirmar":
                    if (options.uncertain && !failed) {
                        failed = true;
                        throw new ApiError(
                            "Resposta perdida",
                            0,
                            "SEM_RESPOSTA",
                            "",
                            true,
                        );
                    }
                    if (options.pending) return new Promise<Receipt>(() => {});
                    return receipt(andamento);
                default:
                    throw new Error(`Consulta inesperada: ${r.endpoint.id}`);
            }
        }),
    };
    return { transport, requests };
}
function Harness({
    transport,
    perfil = "GESTOR",
    cliente = "83",
}: {
    transport: Transport;
    perfil?: Perfil;
    cliente?: string;
}) {
    return (
        <JourneyPage
            journey={journeys.find((j) => j.id === "fiscal")!}
            transport={transport}
            perfil={perfil}
            context={{ clienteId: cliente, armazemId: "46" }}
            workflow={emptyWorkflow()}
            onReceipt={() => {}}
            onRecord={() => {}}
            onNavigate={() => {}}
            startAction=""
        />
    );
}
async function preview() {
    fireEvent.click(await screen.findByRole("button", { name: "EMITIR NF-e" }));
    fireEvent.click(
        await screen.findByRole("button", { name: "Conferir e emitir NF-e" }),
    );
    return screen.findByRole("dialog", { name: "Conferir emissão da NF-e" });
}
it("lista pedidos com filtros globais enviados ao servidor e preserva o número exato", async () => {
    const p = prepared();
    render(<Harness transport={p.transport} />);
    expect(await screen.findByText("SAI-TESTE")).toBeInTheDocument();
    expect(screen.getByRole("cell", { name: "PENDENTE" })).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText("Pedido"), {
        target: { value: "SAI-123" },
    });
    fireEvent.change(screen.getByLabelText("Situação da NF-e"), {
        target: { value: "FINALIZADO" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Aplicar filtros" }));
    await waitFor(() =>
        expect(
            p.requests
                .filter((r) => r.endpoint.id === "FiscalNfeController.listar")
                .at(-1)?.query,
        ).toMatchObject({
            clienteId: "83",
            armazemId: "46",
            situacao: "FINALIZADO",
            busca: "SAI-123",
            pagina: 0,
        }),
    );
    expect(p.requests.some((r) => r.endpoint.method !== "GET")).toBe(false);
});
it("revisa emitente, proprietário, origens e valores sem emitir antes da confirmação", async () => {
    const p = prepared();
    render(<Harness transport={p.transport} />);
    const d = await preview();
    expect(within(d).getByText("11111111000191")).toBeInTheDocument();
    expect(within(d).getByText(/22222222000191/)).toBeInTheDocument();
    expect(within(d).getByText("Saída WMS: SAI-TESTE")).toBeInTheDocument();
    expect(within(d).getByText("39269090 / 5906")).toBeInTheDocument();
    expect(
        within(d).getByRole("button", { name: "Confirmar emissão" }),
    ).toBeDisabled();
    expect(
        p.requests.filter(
            (r) => r.endpoint.id === "FiscalNfeController.confirmar",
        ),
    ).toHaveLength(0);
    fireEvent.click(within(d).getByRole("checkbox"));
    fireEvent.click(
        within(d).getByRole("button", { name: "Confirmar emissão" }),
    );
    await waitFor(() =>
        expect(
            p.requests.filter(
                (r) => r.endpoint.id === "FiscalNfeController.confirmar",
            ),
        ).toHaveLength(1),
    );
    const r = p.requests.find(
        (r) => r.endpoint.id === "FiscalNfeController.confirmar",
    )!;
    expect(r.params.id).toBe(row.id);
    expect(r.body).toMatchObject({
        versao: "4",
        hash: "a".repeat(64),
        natureza: "RETORNO_MERCADORIA",
    });
});
it("bloqueia emissão quando o cadastro ou a origem está incompleto", async () => {
    const p = prepared({ blocked: true });
    render(<Harness transport={p.transport} />);
    const d = await preview();
    expect(
        within(d).getByText("Produto sem valor fiscal da entrada."),
    ).toBeInTheDocument();
    expect(within(d).getByRole("checkbox")).toBeDisabled();
    expect(
        within(d).getByRole("button", { name: "Confirmar emissão" }),
    ).toBeDisabled();
});
it("recupera a mesma confirmação após resposta perdida, mantendo identidade e conteúdo", async () => {
    const p = prepared({ uncertain: true });
    render(<Harness transport={p.transport} />);
    const d = await preview();
    fireEvent.click(within(d).getByRole("checkbox"));
    fireEvent.click(
        within(d).getByRole("button", { name: "Confirmar emissão" }),
    );
    const retry = await within(d).findByRole("button", {
        name: "Recuperar mesma confirmação",
    });
    expect(
        within(d).getByRole("button", { name: "Voltar à conferência" }),
    ).toBeDisabled();
    fireEvent.click(retry);
    await waitFor(() =>
        expect(
            p.requests.filter(
                (r) => r.endpoint.id === "FiscalNfeController.confirmar",
            ),
        ).toHaveLength(2),
    );
    const [a, b] = p.requests.filter(
        (r) => r.endpoint.id === "FiscalNfeController.confirmar",
    );
    expect(a.body).toEqual(b.body);
    expect(a.params).toEqual(b.params);
});
it("perfil operacional consulta sem apresentar emissão ou cancelamento", async () => {
    const p = prepared();
    render(<Harness transport={p.transport} perfil="OPERACAO" />);
    fireEvent.click(await screen.findByRole("button", { name: "SAI-TESTE" }));
    expect(
        await screen.findByRole("table", { name: "Itens do pedido" }),
    ).toBeInTheDocument();
    expect(
        screen.queryByRole("button", { name: "EMITIR NF-e" }),
    ).not.toBeInTheDocument();
    expect(
        screen.queryByRole("button", { name: "Conferir e emitir NF-e" }),
    ).not.toBeInTheDocument();
    expect(p.requests.every((r) => r.endpoint.method === "GET")).toBe(true);
});
it("trocar cliente descarta detalhes e respostas do contexto anterior", async () => {
    const p = prepared();
    const view = render(<Harness transport={p.transport} />);
    fireEvent.click(await screen.findByRole("button", { name: "SAI-TESTE" }));
    await screen.findByRole("table", { name: "Itens do pedido" });
    view.rerender(<Harness transport={p.transport} cliente="84" />);
    expect(
        screen.queryByRole("table", { name: "Itens do pedido" }),
    ).not.toBeInTheDocument();
    await waitFor(() =>
        expect(
            p.requests
                .filter((r) => r.endpoint.id === "FiscalNfeController.listar")
                .at(-1)?.query.clienteId,
        ).toBe("84"),
    );
});
it("apresenta notas existentes pelo número e série, sem identificador técnico", () => {
    expect(
        businessTitle(
            {
                id: "99999999",
                nota: { numero: "123", serie: "1" },
                situacao: "AUTORIZADO",
            },
            "ExpedicaoDto.DocumentoRegistrado",
        ),
    ).toBe("NF-e 123 · série 1 · AUTORIZADO");
});
