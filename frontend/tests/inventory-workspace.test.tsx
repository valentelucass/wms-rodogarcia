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
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { FictitiousTransport } from "../src/api/fictitious";
import { JourneyPage } from "../src/components/JourneyPage";
import { emptyWorkflow } from "../src/domain/workflow";
import { journeys } from "../src/domain/journeys";
import type { Perfil, Values } from "../src/contracts/runtime";
import { inventoryRequest } from "../src/modules/regularizacao/inventoryApi";

const barcode = "f489503e-680b-40a5-9fc3-9bb44d743489";
const base = {
    id: "9007199254740993",
    versao: "0",
    clienteId: "83",
    armazemId: "46",
    tipo: "CEGAS",
    produtoId: null,
    enderecoId: null,
    situacao: "EM_ANDAMENTO",
    iniciadoEm: "2026-10-23T12:00:00Z",
    encerradoEm: null,
    responsavel: "Conferente",
    unidadesLidas: "0",
    divergencias: null,
};
const receipt = (data: unknown): Receipt => ({
    data,
    raw: "",
    ficticio: true,
    replay: false,
    requestId: "INVENTORY-TEST",
});
const page = (itens: unknown[]) => ({
    itens,
    pagina: 0,
    tamanho: 20,
    totalItens: String(itens.length),
    totalPaginas: itens.length ? 1 : 0,
});
function prepared(
    options: { available?: boolean; uncertainStart?: boolean } = {},
) {
    const requests: Request[] = [];
    const fallback = new FictitiousTransport();
    let current: Values = { ...base };
    let failed = false;
    let readings: Values[] = [];
    const report = () => ({
        inventario: current,
        referenciaEstoque: base.iniciadoEm,
        mercadoriasConferidas: 1,
        mercadoriasNaoLidas: 0,
        divergenciasQuantidade: 1,
        divergenciasEndereco: 1,
        movimentadasDuranteContagem: 0,
        porSku: [
            {
                produtoId: "17",
                sku: "SKU-17",
                descricao: "Produto contado",
                unidadeMedida: "UN",
                esperado: "10.000000",
                contado: "8.000000",
                diferenca: "-2.000000",
                mercadoriasDivergentes: 1,
            },
        ],
        mercadorias: [
            {
                codigoUnidade: barcode,
                produtoId: "17",
                sku: "SKU-17",
                descricao: "Produto contado",
                unidadeMedida: "UN",
                esperado: "10.000000",
                contado: "8.000000",
                diferenca: "-2.000000",
                posicoesEsperadas: "A-01",
                posicaoEncontrada: "B-01",
                ocorrencias: ["QUANTIDADE", "ENDERECO"],
            },
        ],
    });
    const transport: Transport = {
        send: vi.fn(async (r) => {
            requests.push(r);
            const body = (r.body ?? {}) as Values;
            switch (r.endpoint.id) {
                case "InventarioController.disponibilidade":
                    return receipt({
                        disponivel: options.available !== false,
                        mensagem:
                            options.available === false
                                ? "O registro de inventários aguarda ativação neste ambiente."
                                : null,
                    });
                case "InventarioController.visao":
                    return receipt(
                        page([
                            {
                                produtoId: "17",
                                sku: "SKU-17",
                                descricao: "Produto em estoque",
                                unidadeMedida: "UN",
                                quantidade: "1234567890123.000001",
                                mercadorias: "2",
                                posicoes: "A-01",
                            },
                        ]),
                    );
                case "InventarioController.listar":
                    return receipt(page([current]));
                case "InventarioController.iniciar":
                    current = {
                        ...base,
                        tipo: body.tipo,
                        produtoId: body.produtoId,
                        enderecoId: body.enderecoId,
                    };
                    if (options.uncertainStart && !failed) {
                        failed = true;
                        throw new ApiError(
                            "A confirmação não chegou.",
                            503,
                            "RESPOSTA_PERDIDA",
                            "",
                            true,
                        );
                    }
                    return receipt(current);
                case "InventarioController.consultar":
                    return receipt({ inventario: current, leituras: readings });
                case "InventarioController.ler":
                    if (readings.length && !body.substituir)
                        throw new ApiError(
                            "Etiqueta já contada. Use Recontar.",
                            409,
                            "ETIQUETA_JA_LIDA",
                        );
                    readings = [
                        {
                            codigoUnidade: barcode,
                            sku: "SKU-17",
                            descricao: "Produto contado",
                            unidadeMedida: "UN",
                            contado: body.contado,
                            enderecoId: body.enderecoId,
                            posicao: "B-01",
                            lidoEm: "2026-10-23T12:10:00Z",
                        },
                    ];
                    current = { ...current, versao: "1", unidadesLidas: "1" };
                    return receipt({
                        inventario: current,
                        leitura: readings[0],
                    });
                case "InventarioController.finalizar":
                    current = {
                        ...current,
                        versao: "2",
                        situacao: "CONCLUIDO",
                        encerradoEm: "2026-10-23T12:15:00Z",
                        divergencias: 1,
                    };
                    return receipt(report());
                case "InventarioController.relatorio":
                    return receipt(report());
                default:
                    return fallback.send(r);
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
    const [workflow] = useState(() => {
        const w = emptyWorkflow();
        w.catalogs["ClienteDto.Resposta"] = [
            { id: "83", nome: "Cliente de teste" },
        ];
        w.catalogs["ArmazemDto.Resposta"] = [
            { id: "46", nome: "Armazém de teste" },
        ];
        w.catalogs["ProdutoDto.Resposta"] = [
            {
                id: "17",
                sku: "SKU-17",
                descricao: "Produto contado",
                clienteId: "83",
                situacao: "ATIVO",
            },
        ];
        w.catalogs["EnderecoDto.Resposta"] = [
            {
                id: "71",
                codigo: "B-01",
                descricao: "Posição B",
                armazemId: "46",
                situacao: "ATIVO",
            },
        ];
        return w;
    });
    return (
        <JourneyPage
            key={`${context.clienteId}/${context.armazemId}/${perfil}`}
            journey={journeys.find((j) => j.id === "contagem")!}
            transport={transport}
            perfil={perfil}
            context={context}
            workflow={workflow}
            onReceipt={() => {}}
            onRecord={() => {}}
            onNavigate={() => {}}
            startAction=""
        />
    );
}
async function openNew() {
    const button = await screen.findByRole("button", {
        name: "Realizar inventário",
    });
    await waitFor(() => expect(button).toBeEnabled());
    fireEvent.click(button);
    return screen.findByRole("dialog", { name: "Realizar inventário" });
}
async function start() {
    const dialog = await openNew();
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Iniciar inventário" }),
    );
    const active = await screen.findByRole("dialog", {
        name: "Contagem do inventário",
    });
    await waitFor(() =>
        expect(
            within(active).getByRole("button", { name: "Registrar leitura" }),
        ).toBeEnabled(),
    );
    return active;
}
it("apresenta uma única tela com as três opções e quantidades exatas do servidor", async () => {
    const p = prepared();
    render(<Harness transport={p.transport} />);
    expect(
        await screen.findByText("1.234.567.890.123,000001"),
    ).toBeInTheDocument();
    expect(screen.queryByText("Etapas da jornada")).not.toBeInTheDocument();
    const dialog = await openNew();
    for (const mode of ["Às cegas", "Por SKU", "Por posição"])
        expect(
            within(dialog).getByRole("radio", { name: new RegExp(mode) }),
        ).toBeInTheDocument();
    expect(
        screen.queryByRole("table", { name: "Mercadorias em estoque" }),
    ).not.toBeInTheDocument();
    expect(p.requests.every((r) => r.endpoint.method === "GET")).toBe(true);
});
it.each([
    ["Por SKU", "Produto / SKU", "SKU"],
    ["Por posição", "Endereço", "posição"],
])("exige seleção em %s antes de iniciar", async (mode, field, name) => {
    const p = prepared();
    render(<Harness transport={p.transport} />);
    const dialog = await openNew();
    fireEvent.click(
        within(dialog).getByRole("radio", { name: new RegExp(mode) }),
    );
    expect(within(dialog).getByText(new RegExp(field))).toBeInTheDocument();
    fireEvent.submit(
        within(dialog)
            .getByRole("button", { name: "Iniciar inventário" })
            .closest("form")!,
    );
    await waitFor(() =>
        expect(
            p.requests.filter(
                (r) => r.endpoint.id === "InventarioController.iniciar",
            ),
        ).toHaveLength(0),
    );
    expect(within(dialog).getByRole("alert")).toHaveTextContent(
        new RegExp(name),
    );
});
it("mantém a contagem cega e apresenta diferenças de SKU e endereço somente ao encerrar", async () => {
    const p = prepared();
    render(<Harness transport={p.transport} />);
    const dialog = await start();
    expect(within(dialog).queryByText("Esperado")).not.toBeInTheDocument();
    expect(
        within(dialog).getByLabelText("Etiqueta da mercadoria"),
    ).toHaveFocus();
    expect(within(dialog).queryByText("Diferença")).not.toBeInTheDocument();
    expect(
        within(dialog).getByRole("button", {
            name: "Encerrar e gerar relatório",
        }),
    ).toBeDisabled();
    fireEvent.change(within(dialog).getByLabelText("Etiqueta da mercadoria"), {
        target: { value: barcode },
    });
    fireEvent.change(within(dialog).getByLabelText("Quantidade contada"), {
        target: { value: "8,000000" },
    });
    fireEvent.click(
        within(dialog).getByRole("checkbox", {
            name: "Mercadoria sem endereço",
        }),
    );
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Registrar leitura" }),
    );
    await within(dialog).findByText(/Leitura registrada/);
    fireEvent.click(
        within(dialog).getByRole("checkbox", { name: /Concluí a conferência/ }),
    );
    await waitFor(() =>
        expect(
            within(dialog).getByRole("button", {
                name: "Encerrar e gerar relatório",
            }),
        ).toBeEnabled(),
    );
    fireEvent.click(
        within(dialog).getByRole("button", {
            name: "Encerrar e gerar relatório",
        }),
    );
    const report = await screen.findByRole("dialog", {
        name: "Relatório do inventário",
    });
    expect(
        await within(report).findByRole("columnheader", { name: "Esperado" }),
    ).toBeInTheDocument();
    expect(
        within(report).getByRole("table", { name: "Resultado do inventário" }),
    ).toHaveTextContent("-2");
    fireEvent.click(
        within(report).getByRole("button", { name: "Mercadorias e posições" }),
    );
    expect(within(report).getByText(/Posição diferente/)).toBeInTheDocument();
    const writes = p.requests.filter((r) => r.endpoint.method !== "GET");
    expect(writes.map((r) => r.endpoint.id)).toEqual([
        "InventarioController.iniciar",
        "InventarioController.ler",
        "InventarioController.finalizar",
    ]);
    expect(writes[1].params.id).toBe(base.id);
    expect((writes[1].body as Values).contado).toBe("8.000000");
    expect(
        p.requests.some((r) =>
            [
                "ContagemController.aplicar",
                "EnderecoController.alterar",
            ].includes(r.endpoint.id),
        ),
    ).toBe(false);
});
it("reenvia a mesma chave após confirmação perdida e bloqueia novos comandos enquanto incerta", async () => {
    const p = prepared({ uncertainStart: true });
    render(<Harness transport={p.transport} />);
    const dialog = await openNew();
    fireEvent.click(
        within(dialog).getByRole("button", { name: "Iniciar inventário" }),
    );
    const retry = await within(dialog).findByRole("button", {
        name: "Conferir a mesma solicitação",
    });
    expect(
        within(dialog).getByRole("button", { name: "Iniciar inventário" }),
    ).toBeDisabled();
    fireEvent.click(retry);
    await screen.findByRole("dialog", { name: "Contagem do inventário" });
    const starts = p.requests.filter(
        (r) => r.endpoint.id === "InventarioController.iniciar",
    );
    expect(starts).toHaveLength(2);
    expect(starts[0].body).toEqual(starts[1].body);
});
it("não oferece cancelamento ao operador e preserva os controles de ajuste do supervisor", async () => {
    const p = prepared();
    render(<Harness transport={p.transport} perfil="OPERACAO" />);
    const dialog = await start();
    expect(
        within(dialog).queryByText("Cancelar este inventário"),
    ).not.toBeInTheDocument();
    expect(() =>
        inventoryRequest("InventarioController.cancelar", "OPERACAO", {
            id: base.id,
        }),
    ).toThrow(/perfil/);
});
it("informa a pendência de ativação sem simular inventários gravados", async () => {
    const p = prepared({ available: false });
    render(<Harness transport={p.transport} />);
    expect(await screen.findByText(/aguarda ativação/)).toBeInTheDocument();
    expect(
        screen.getByRole("button", { name: "Realizar inventário" }),
    ).toBeDisabled();
    expect(await screen.findByText("Produto em estoque")).toBeInTheDocument();
    expect(p.requests.every((r) => r.endpoint.method === "GET")).toBe(true);
});

it("trocar a visão de estoque pelo histórico não interpreta mercadorias como inventários", async () => {
    const p = prepared();
    const previous = p.transport.send;
    p.transport.send = (r) =>
        r.endpoint.id === "InventarioController.visao"
            ? Promise.resolve(
                  receipt(
                      page(
                          [17, 18, 19].map((id) => ({
                              produtoId: String(id),
                              sku: `SKU-${id}`,
                              descricao: `Mercadoria ${id}`,
                              unidadeMedida: "UN",
                              quantidade: "10",
                              mercadorias: "1",
                              posicoes: null,
                          })),
                      ),
                  ),
              )
            : previous(r);
    const errors = vi.spyOn(console, "error").mockImplementation(() => {});
    try {
        render(<Harness transport={p.transport} />);
        await screen.findByText("Mercadoria 17");
        fireEvent.click(
            screen.getByRole("button", { name: "Inventários realizados" }),
        );
        await screen.findByRole("table", { name: "Inventários realizados" });
        expect(await screen.findByText(`#${base.id}`)).toBeInTheDocument();
        expect(screen.queryByText("#undefined")).not.toBeInTheDocument();
        expect(
            errors.mock.calls.some((c) => String(c[0]).includes("same key")),
        ).toBe(false);
    } finally {
        errors.mockRestore();
    }
});
it("descarta o estoque tardio quando o contexto muda", async () => {
    const waits: ((r: Receipt) => void)[] = [];
    const transport: Transport = {
        send: (r) =>
            r.endpoint.id === "InventarioController.visao"
                ? new Promise((resolve) => waits.push(resolve))
                : Promise.resolve(
                      receipt({ disponivel: true, mensagem: null }),
                  ),
    };
    const ui = render(<Harness transport={transport} />);
    await waitFor(() => expect(waits.length).toBeGreaterThan(0));
    ui.rerender(
        <Harness
            transport={transport}
            context={{ clienteId: "999", armazemId: "46" }}
        />,
    );
    await act(async () =>
        waits[0](
            receipt(
                page([
                    {
                        produtoId: "17",
                        sku: "antigo",
                        descricao: "Estoque do contexto anterior",
                        unidadeMedida: "UN",
                        quantidade: "900",
                        mercadorias: "1",
                        posicoes: null,
                    },
                ]),
            ),
        ),
    );
    expect(
        screen.queryByText("Estoque do contexto anterior"),
    ).not.toBeInTheDocument();
});
