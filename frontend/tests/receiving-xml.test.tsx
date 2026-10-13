import { describe, it, expect, vi } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { ReceivingCreateDialog } from "../src/modules/recebimento/ReceivingCreateDialog";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { nfePreview } from "./nfe-example";
import { canLeavePage } from "../src/domain/pageLeave";
import { NfeDocument } from "../src/modules/recebimento/NfeDocument";

it("exportacao mostra oito bobinas do mesmo SKU e o aviso sem corrigir peso", () => {
    const doc = structuredClone(nfePreview.documento!);
    doc.formato = "EXPORTACAO_OPERACIONAL";
    doc.avisos = [
        "Peso líquido informado 14.172 KG difere da soma 14172 KG. Valores preservados.",
    ];
    doc.itens = [
        "1.8480",
        "1.8260",
        "1.8370",
        "1.7820",
        "1.7830",
        "2.0260",
        "1.5500",
        "1.5200",
    ].map((peso, i) => ({
        ...doc.itens![0],
        numeroItem: i + 1,
        quantidadeComercial: peso,
        quantidadeTributavel: peso,
        unidadeComercial: "TO",
        unidadeTributavel: "TO",
        bobinasPrevistas: 1,
    }));
    doc.volumes = [
        {
            quantidade: "8",
            especie: "Tonelada",
            pesoLiquido: "14.172",
            pesoBruto: "14.172",
        },
    ];
    render(<NfeDocument documento={doc} />);
    expect(screen.getAllByText("1 bobina · 1 posição prevista")).toHaveLength(
        8,
    );
    expect(screen.getAllByText("14.172")).toHaveLength(2);
    expect(screen.getByLabelText("Conferência do arquivo")).toHaveTextContent(
        "Valores preservados",
    );
    expect(screen.getByText(/Exportação operacional/)).toBeVisible();
});
const receipt = (data: unknown) => ({
    data,
    raw: "",
    requestId: "XML01-ficticio",
    ficticio: true,
    replay: false,
});
function harness(
    options: {
        uncertain?: boolean;
        pending?: boolean;
        previa?: typeof nfePreview;
        gestor?: boolean;
    } = {},
) {
    const requests: Request[] = [],
        confirmed = vi.fn(),
        manual = vi.fn(),
        close = vi.fn(),
        existing = vi.fn();
    const transport: Transport = {
        send: vi.fn(async (request: Request): Promise<Receipt> => {
            requests.push(request);
            switch (request.endpoint.id) {
                case "PedidoEntradaXmlController.previa":
                    if (options.pending) return new Promise<Receipt>(() => {});
                    return receipt(
                        structuredClone(options.previa ?? nfePreview),
                    );
                case "ProdutoController.listar":
                    return receipt({
                        itens: [
                            {
                                id: "1",
                                sku: "69230424",
                                descricao: "Bandeja",
                                unidadeMedida: "PEC",
                                situacao: "ATIVO",
                            },
                        ],
                        totalPaginas: "1",
                    });
                case "ProdutoController.criar":
                    return receipt({
                        ...(request.body as object),
                        id: "80",
                        versao: "0",
                        situacao: "ATIVO",
                    });
                case "PedidoEntradaXmlController.confirmar":
                    if (options.uncertain)
                        throw new ApiError(
                            "Transporte interrompido",
                            0,
                            "RESPOSTA_DESCONHECIDA",
                            "teste",
                            true,
                        );
                    return receipt({
                        operacaoId: (request.body as { operacaoId: string })
                            .operacaoId,
                        pedido: fixture("PedidoEntradaDto.Resumo"),
                    });
                case "PedidoEntradaXmlController.resultado":
                    return receipt({
                        operacaoId: request.params.operacaoId,
                        pedido: fixture("PedidoEntradaDto.Resumo"),
                    });
                default:
                    throw new Error(request.endpoint.id);
            }
        }),
    };
    render(
        <ReceivingCreateDialog
            transport={transport}
            perfil={options.gestor ? "GESTOR" : "OPERACAO"}
            onClose={close}
            onManual={manual}
            onConfirmed={confirmed}
            onExisting={existing}
        />,
    );
    return { requests, confirmed, manual, close, existing };
}
async function file(name = "exemplo.xml", text = "<NFe>fictício</NFe>") {
    fireEvent.click(
        screen.getByRole("button", {
            name: "Importar XML da NF-e",
        }),
    );
    const selected = new File([text], name, { type: "application/xml" });
    Object.defineProperty(selected, "arrayBuffer", {
        value: async () => new TextEncoder().encode(text).buffer,
    });
    fireEvent.change(screen.getByLabelText("Arquivo XML da NF-e"), {
        target: { files: [selected] },
    });
    await waitFor(() =>
        expect(screen.queryByText("Lendo arquivo…")).toBeNull(),
    );
}
async function preview() {
    await file();
    fireEvent.click(screen.getByRole("button", { name: "Ler XML" }));
    await screen.findByText("TIGRE FERRAMENTAS PARA CONSTRUCAO C");
}
describe("FE05-XML01", () => {
    it("cadastra SKU pela nota e exige nova previa sem perder o XML", async () => {
        const p = structuredClone(nfePreview);
        p.documento!.itens![0].unidadeComercial = "TO";
        p.itens![0].produtoId = null;
        p.podeConfirmar = false;
        const h = harness({ previa: p, gestor: true });
        await preview();
        fireEvent.click(
            screen.getByRole("button", { name: "Cadastrar produto do item 1" }),
        );
        expect(screen.getByDisplayValue("69230424")).toBeInTheDocument();
        expect(screen.getByDisplayValue("KG")).toBeInTheDocument();
        fireEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findAllByRole("button", { name: "Atualizar prévia" });
        expect(
            screen.getByRole("button", { name: "Confirmar pedido de entrada" }),
        ).toBeDisabled();
        fireEvent.click(
            screen.getAllByRole("button", { name: "Atualizar prévia" })[0],
        );
        await waitFor(() =>
            expect(
                h.requests.filter(
                    (r) =>
                        r.endpoint.id === "PedidoEntradaXmlController.previa",
                ),
            ).toHaveLength(2),
        );
        const last = h.requests
            .filter(
                (r) => r.endpoint.id === "PedidoEntradaXmlController.previa",
            )
            .at(-1)!;
        expect(last.body).toMatchObject({
            xml: "<NFe>fictício</NFe>",
            associacoes: [{ numeroItem: 1, produtoId: "80" }],
        });
        const created = h.requests.find(
            (r) => r.endpoint.id === "ProdutoController.criar",
        )!;
        expect(created.body).toMatchObject({
            clienteId: "1",
            unidadeMedida: "KG",
            tipoQuantidade: "MEDIDA",
            precisaoQuantidade: 3,
        });
    });
    it("operacao associa produtos existentes sem apresentar cadastro restrito", async () => {
        const p = structuredClone(nfePreview);
        p.itens![0].produtoId = null;
        harness({ previa: p });
        await preview();
        expect(
            screen.queryByRole("button", {
                name: "Cadastrar produto do item 1",
            }),
        ).toBeNull();
    });
    it("oferece criação manual e XML sem executar comandos na escolha", () => {
        const h = harness();
        fireEvent.click(
            screen.getByRole("button", { name: "Criar manualmente" }),
        );
        expect(h.manual).toHaveBeenCalledOnce();
        expect(h.requests).toHaveLength(0);
    });
    it("lê e mostra o original antes de confirmar, preservando casas decimais e volumes", async () => {
        const h = harness();
        await preview();
        expect(screen.getByText("24")).toBeVisible();
        expect(screen.getByText("04547874000203")).toBeVisible();
        expect(screen.getByText(/0.7083000000/)).toBeVisible();
        expect(
            h.requests.some(
                (r) => r.endpoint.id === "PedidoEntradaXmlController.confirmar",
            ),
        ).toBe(false);
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar pedido de entrada" }),
        );
        await waitFor(() => expect(h.confirmed).toHaveBeenCalledOnce());
        expect(
            h.requests.find(
                (r) => r.endpoint.id === "PedidoEntradaXmlController.confirmar",
            )?.body,
        ).toMatchObject({
            clienteId: "1",
            armazemId: "1",
            referencia: null,
            revisaoPrevia: nfePreview.revisaoPrevia,
            associacoes: [{ numeroItem: 1, produtoId: "1" }],
        });
    });
    it("bloqueia confirmação quando escolha muda e mantém o documento visível", async () => {
        harness();
        await preview();
        const selector = await screen.findByLabelText(
            "Produto do item 1 · 69230424",
        );
        fireEvent.change(selector, { target: { value: "" } });
        expect(
            screen.getByRole("button", { name: "Confirmar pedido de entrada" }),
        ).toBeDisabled();
        expect(
            screen.getByText("TIGRE FERRAMENTAS PARA CONSTRUCAO C"),
        ).toBeVisible();
    });
    it("recusa arquivo errado e permite remover sem gravar", async () => {
        const h = harness();
        await file("nota.txt");
        expect(screen.getByRole("alert")).toHaveTextContent(
            "Selecione um arquivo .xml",
        );
        expect(h.requests).toHaveLength(0);
    });
    it("resultado desconhecido bloqueia saída, não repete POST automaticamente e recupera pelo identificador", async () => {
        const h = harness({ uncertain: true });
        await preview();
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar pedido de entrada" }),
        );
        await screen.findByRole("button", { name: "Consultar confirmação" });
        expect(canLeavePage()).toBe(false);
        const posts = h.requests.filter(
            (r) => r.endpoint.id === "PedidoEntradaXmlController.confirmar",
        );
        expect(posts).toHaveLength(1);
        fireEvent.click(
            screen.getByRole("button", { name: "Consultar confirmação" }),
        );
        await waitFor(() => expect(h.confirmed).toHaveBeenCalledOnce());
        expect(h.requests.at(-1)?.params.operacaoId).toEqual(
            (posts[0].body as { operacaoId: string }).operacaoId,
        );
        expect(
            h.requests.filter(
                (r) => r.endpoint.id === "PedidoEntradaXmlController.confirmar",
            ),
        ).toHaveLength(1);
    });
    it("pendências e duplicidade impedem confirmar e permitem consultar o pedido autorizado", async () => {
        const h = harness({
            previa: {
                ...nfePreview,
                podeConfirmar: false,
                pendencias: ["Esta nota já está vinculada."],
                pedidoExistenteId: "901",
            },
        });
        await preview();
        expect(
            screen.getByRole("button", { name: "Confirmar pedido de entrada" }),
        ).toBeDisabled();
        fireEvent.click(
            screen.getByRole("button", { name: "Ver pedido existente" }),
        );
        expect(h.existing).toHaveBeenCalledWith("901");
    });
});
