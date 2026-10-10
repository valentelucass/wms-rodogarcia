import { useState } from "react";
import { describe, it, expect, vi } from "vitest";
import {
    render,
    screen,
    within,
    fireEvent,
    waitFor,
} from "@testing-library/react";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/domain/journeys";
import { absorb, emptyWorkflow } from "../src/domain/workflow";
import { type Perfil, type Values } from "../src/contracts/runtime";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { receivingState } from "../src/modules/recebimento/orderPresentation";
import { fixture } from "../src/api/mock/fixtures";

const summary = (id: string, situation = "RASCUNHO"): Values => ({
    ...(fixture("PedidoEntradaDto.Resumo") as Values),
    id,
    clienteId: "1",
    armazemId: "1",
    referencia: `PED-${id}`,
    situacao: situation,
    versao: "2",
    efetivadoEm: null,
});
const note = (
    id: string,
    itemId: string,
    arrival: string | null = null,
): Values => ({
    ...(fixture("PedidoEntradaDto.Nota") as Values),
    id,
    numero: id,
    serie: "1",
    primeiraChegada: arrival,
    itens: [
        {
            ...(fixture("PedidoEntradaDto.ItemConferencia") as Values),
            id: itemId,
            numeroItem: "1",
            sku: `SKU-${itemId}`,
            prevista: "100.000000",
            recebidaBoa: arrival ? "98.000000" : "0.000000",
            recebidaAvariada: "0.000000",
            diferenca: arrival ? "-2.000000" : "-100.000000",
        },
    ],
});
const result = (data: unknown) => ({
    data,
    raw: "",
    requestId: "PED01-teste",
    ficticio: true,
    replay: false,
});
function Harness({
    transport,
    perfil = "GESTOR",
    context = { clienteId: "1", armazemId: "1" },
    action = "",
}: {
    transport: Transport;
    perfil?: Perfil;
    context?: Values;
    action?: string;
}) {
    const [workflow, setWorkflow] = useState(emptyWorkflow);
    return (
        <JourneyPage
            journey={journeys.find((journey) => journey.id === "entrada")!}
            transport={transport}
            perfil={perfil}
            context={context}
            workflow={workflow}
            onRecord={(row, type) =>
                setWorkflow((old) => absorb(old, type, row, true))
            }
            onReceipt={(receipt, type, _id, request) =>
                setWorkflow((old) =>
                    absorb(old, type, receipt.data, false, request),
                )
            }
            onNavigate={vi.fn()}
            startAction={action}
        />
    );
}
const click = (
    name: string,
    scope: typeof screen | ReturnType<typeof within> = screen,
) => fireEvent.click(scope.getByRole("button", { name, exact: true }));
function transportFor(rows: Values[], details: Record<string, Values>) {
    const requests: Request[] = [];
    return {
        requests,
        send: vi.fn(async (request: Request) => {
            requests.push(request);
            if (request.endpoint.id === "PedidoEntradaController.listar")
                return result({
                    itens: rows.map((row) => ({ ...row })),
                    pagina: String(request.query.pagina ?? 0),
                    tamanho: "20",
                    totalItens: String(rows.length),
                    totalPaginas: "1",
                });
            if (request.endpoint.id === "PedidoEntradaController.consultar")
                return result(
                    structuredClone(details[String(request.params.id)]),
                );
            throw new Error("Comando sem fixture: " + request.endpoint.id);
        }),
    };
}
describe("FE05-PED01 pedido de entrada", () => {
    it("mantém busca e conferência após erro da lista e não apresenta vazio como resposta de erro", async () => {
        const root = summary("101", "QUARENTENA");
        const transport = transportFor([root], {
            "101": {
                pedido: root,
                divergente: true,
                notas: [note("301", "401", "2026-10-08T12:00:00Z")],
            },
        });
        const send = transport.send.getMockImplementation()!;
        let fail = false;
        transport.send.mockImplementation(async (request) => {
            if (
                request.endpoint.id === "PedidoEntradaController.listar" &&
                fail
            )
                throw new Error("Lista indisponível PED01");
            return send(request);
        });
        render(<Harness transport={transport} />);
        await screen.findByRole("cell", { name: "Divergente" });
        fireEvent.change(screen.getByLabelText("Pedido ou nota nesta página"), {
            target: { value: "301" },
        });
        fireEvent.change(screen.getByLabelText("Conferência nesta página"), {
            target: { value: "Divergente" },
        });
        fail = true;
        click("Atualizar lista");
        await screen.findByText("Lista indisponível PED01");
        expect(
            screen.getByLabelText("Pedido ou nota nesta página"),
        ).toHaveValue("301");
        expect(screen.getByLabelText("Conferência nesta página")).toHaveValue(
            "Divergente",
        );
        expect(
            screen.queryByText(
                "Ainda não há pedidos de entrada neste contexto.",
            ),
        ).toBeNull();
        expect(
            screen.queryByText(/Nenhum pedido corresponde aos filtros/),
        ).toBeNull();
        fail = false;
        click("Tentar novamente");
        await screen.findByRole("cell", { name: "Divergente" });
        expect(
            screen.getByLabelText("Pedido ou nota nesta página"),
        ).toHaveValue("301");
        expect(screen.getByLabelText("Conferência nesta página")).toHaveValue(
            "Divergente",
        );
    });
    it("consulta auditoria no histórico mantendo o tipo e o ID do pedido protegidos", async () => {
        const root = summary("9007199254740993", "QUARENTENA");
        const transport = transportFor([root], {
            "9007199254740993": { pedido: root, divergente: true, notas: [] },
        });
        const send = transport.send.getMockImplementation()!;
        transport.send.mockImplementation(async (request) => {
            if (request.endpoint.id === "AuditoriaController.listar") {
                transport.requests.push(request);
                return result({
                    itens: [],
                    pagina: "0",
                    tamanho: "20",
                    totalItens: "0",
                    totalPaginas: "0",
                });
            }
            return send(request);
        });
        render(<Harness transport={transport} />);
        click(
            "Ver pedido",
            within(
                await screen.findByRole("region", {
                    name: "Lista de pedidos de entrada",
                }),
            ),
        );
        const dialog = within(await screen.findByRole("dialog"));
        await dialog.findByRole("navigation", { name: "Seções do pedido" });
        click("Histórico", dialog);
        click("Consultar lista", dialog);
        expect(dialog.getByLabelText("Tipo *")).toHaveValue("PEDIDO_ENTRADA");
        expect(dialog.getByLabelText("Tipo *")).toBeDisabled();
        expect(dialog.getByLabelText("Registro Id *")).toHaveValue(
            "9007199254740993",
        );
        expect(dialog.getByLabelText("Registro Id *")).toBeDisabled();
        click("Consultar", dialog);
        await waitFor(() =>
            expect(
                transport.requests.some(
                    (request) =>
                        request.endpoint.id === "AuditoriaController.listar",
                ),
            ).toBe(true),
        );
        expect(transport.requests.at(-1)?.query).toMatchObject({
            tipo: "PEDIDO_ENTRADA",
            registroId: "9007199254740993",
        });
    });
    it("abre a fila no contexto aplicado, reúne notas múltiplas e distingue conferência da efetivação", async () => {
        const first = summary("9007199254740993", "QUARENTENA"),
            second = summary("102", "QUARENTENA");
        const t = transportFor([first, second], {
            "9007199254740993": {
                pedido: first,
                divergente: true,
                notas: [
                    note("301", "401", "2026-10-08T12:00:00Z"),
                    note("302", "402", "2026-10-09T13:00:00Z"),
                ],
            },
            "102": {
                pedido: second,
                divergente: false,
                notas: [note("303", "403", "2026-10-09T12:00:00Z")],
            },
        });
        render(<Harness transport={t} />);
        await screen.findByRole("cell", { name: "Divergente" });
        expect(
            screen.getByRole("cell", { name: "Conferido" }),
        ).toBeInTheDocument();
        expect(
            screen.getByRole("cell", { name: "1/301, 1/302" }),
        ).toBeInTheDocument();
        expect(
            screen.queryByRole("navigation", {
                name: "Etapas de Entrada e conferência",
            }),
        ).toBeNull();
        expect(t.requests[0].query).toMatchObject({
            clienteId: "1",
            armazemId: "1",
            pagina: "0",
        });
        expect(
            screen.getByRole("button", { name: "Novo pedido de entrada" }),
        ).toBeInTheDocument();
        fireEvent.change(screen.getByLabelText("Conferência nesta página"), {
            target: { value: "Divergente" },
        });
        expect(
            screen.queryByRole("button", { name: /Ver detalhes de PED-102/ }),
        ).toBeNull();
        fireEvent.change(screen.getByLabelText("Pedido ou nota nesta página"), {
            target: { value: "302" },
        });
        click("Ver pedido");
        const dialog = within(await screen.findByRole("dialog"));
        await dialog.findByRole("navigation", { name: "Seções do pedido" });
        click("Notas e itens", dialog);
        expect(
            dialog.getByRole("region", { name: "Nota 1/301" }),
        ).toBeInTheDocument();
        expect(
            dialog.getByRole("region", { name: "Nota 1/302" }),
        ).toBeInTheDocument();
        click("Selecionar item 402 da nota 302", dialog);
        click("Conferência", dialog);
        click("Registrar chegada física", dialog);
        expect(
            dialog.getByLabelText("Itens / Item 1 / Item da nota (ID) *"),
        ).toHaveValue("402");
        expect(
            t.requests.every((request) => request.endpoint.method === "GET"),
        ).toBe(true);
    });
    it("não chama o backend sem contexto obrigatório e oferece seleção no lugar de exigir paginação técnica", async () => {
        const t = transportFor([], {});
        render(<Harness transport={t} context={{}} />);
        await screen.findByText(/Informe as referências obrigatórias/);
        expect(t.requests).toHaveLength(0);
        expect(screen.queryByLabelText(/Página \(início 0\)/)).toBeNull();
    });
    it("falha do detalhe não aparece como conferido ou sem notas", async () => {
        const root = summary("101");
        const t = transportFor([root], {});
        render(<Harness transport={t} />);
        await screen.findByText(/Não foi possível consultar todos os detalhes/);
        expect(screen.queryByRole("cell", { name: "Conferido" })).toBeNull();
        expect(screen.queryByRole("cell", { name: "Sem notas" })).toBeNull();
        expect(
            screen.getByRole("button", { name: "Ver pedido" }),
        ).toBeEnabled();
    });
    it("cria e continua com a nota no mesmo pedido, lê seu ID e revisão e atualiza a lista", async () => {
        const rows: Values[] = [];
        const details: Record<string, Values> = {};
        const t = transportFor(rows, details);
        const send = t.send.getMockImplementation()!;
        t.send.mockImplementation(async (request) => {
            if (request.endpoint.id === "PedidoEntradaController.criar") {
                t.requests.push(request);
                const created = {
                    ...summary("901"),
                    referencia: (request.body as Values).referencia,
                    versao: "0",
                };
                rows.push(created);
                details["901"] = {
                    pedido: created,
                    divergente: false,
                    notas: [],
                };
                return result(created);
            }
            return send(request);
        });
        render(<Harness transport={t} />);
        await screen.findByText(/Ainda não há pedidos/);
        click("Novo pedido de entrada");
        const dialog = within(await screen.findByRole("dialog"));
        fireEvent.change(dialog.getByLabelText("Referência *"), {
            target: { value: "PED-NOVO" },
        });
        click("Conferir e confirmar", dialog);
        click("Confirmar agora", dialog);
        await dialog.findByText(/Resposta FICTÍCIA/);
        await dialog.findByRole("navigation", { name: "Seções do pedido" });
        await waitFor(() =>
            expect(dialog.queryByText(/Atualizando notas/)).toBeNull(),
        );
        click("Continuar com a nota deste pedido", dialog);
        expect(
            dialog.getByRole("heading", { name: "Registrar nota manual" }),
        ).toBeInTheDocument();
        expect(dialog.getByLabelText("Identificador *")).toHaveValue("901");
        expect(dialog.getByLabelText("Identificador *")).toBeDisabled();
        expect(
            t.requests.find(
                (request) =>
                    request.endpoint.id === "PedidoEntradaController.consultar",
            )?.params.id,
        ).toBe("901");
        expect(
            t.requests.filter((request) => request.endpoint.method !== "GET"),
        ).toHaveLength(1);
        expect(
            screen.getByRole("button", { name: /Ver detalhes de PED-NOVO/ }),
        ).toBeInTheDocument();
    });
    it("Operação não recebe efetivação e um link antigo continua abrindo a operação autorizada no pedido", async () => {
        const root = summary("101", "QUARENTENA"),
            t = transportFor([root], {
                "101": {
                    pedido: root,
                    divergente: true,
                    notas: [note("301", "401", "2026-10-08T12:00:00Z")],
                },
            });
        render(<Harness transport={t} perfil="OPERACAO" />);
        await screen.findByRole("cell", { name: "Divergente" });
        click("Continuar conferência");
        const dialog = within(await screen.findByRole("dialog"));
        await dialog.findByRole("heading", {
            name: "Registrar chegada física",
        });
        expect(
            dialog.queryByRole("button", { name: "Efetivar carga integral" }),
        ).toBeNull();
        expect(
            dialog.queryByRole("button", { name: "Estornar chegada" }),
        ).toBeNull();
    });
    it("protege a troca de seção com edição não salva e preserva os campos após conflito", async () => {
        vi.spyOn(window, "confirm").mockReturnValue(false);
        const root = summary("101", "QUARENTENA"),
            t = transportFor([root], {
                "101": {
                    pedido: root,
                    divergente: true,
                    notas: [note("301", "401", "2026-10-08T12:00:00Z")],
                },
            });
        const send = t.send.getMockImplementation()!;
        t.send.mockImplementation(async (request) => {
            if (request.endpoint.method !== "GET") {
                t.requests.push(request);
                throw new ApiError(
                    "Revisão desatualizada.",
                    409,
                    "VERSAO_DESATUALIZADA",
                );
            }
            return send(request);
        });
        render(<Harness transport={t} />);
        await screen.findByRole("cell", { name: "Divergente" });
        click("Continuar conferência");
        const dialog = within(await screen.findByRole("dialog"));
        await dialog.findByRole("heading", {
            name: "Registrar chegada física",
        });
        fireEvent.change(dialog.getByLabelText("Observacao *"), {
            target: { value: "Contagem preservada" },
        });
        click("Notas e itens", dialog);
        expect(dialog.getByLabelText("Observacao *")).toHaveValue(
            "Contagem preservada",
        );
        expect(window.confirm).toHaveBeenCalled();
        click("Selecionar item 401 da nota 301", dialog);
        await waitFor(() =>
            expect(
                dialog.getByLabelText("Itens / Item 1 / Item da nota (ID) *"),
            ).toHaveValue("401"),
        );
        fireEvent.change(
            dialog.getByLabelText("Chegada física real (ISO com fuso) *"),
            { target: { value: "2026-10-08T12:00:00Z" } },
        );
        fireEvent.change(
            dialog.getByLabelText("Itens / Item 1 / Quantidade boa recebida *"),
            { target: { value: "98" } },
        );
        click("Conferir e confirmar", dialog);
        click("Confirmar agora", dialog);
        await dialog.findByText("Revisão desatualizada.");
        expect(dialog.getByLabelText("Observacao *")).toHaveValue(
            "Contagem preservada",
        );
        const writes = t.requests.filter(
            (request) => request.endpoint.method !== "GET",
        );
        expect(writes).toHaveLength(1);
        expect(writes[0].params.id).toBe("101");
        expect((writes[0].body as Values).versao).toBe("2");
    });
    it("mapeia os dados reais sem confundir divergência, quarentena e efetivação", () => {
        const notes = [note("301", "401", "2026-10-08T12:00:00Z")];
        expect(
            receivingState({
                pedido: summary("1"),
                divergente: true,
                notas: [note("301", "401")],
            }),
        ).toBe("Pendente");
        expect(
            receivingState({
                pedido: summary("1", "QUARENTENA"),
                divergente: false,
                notas: notes,
            }),
        ).toBe("Conferido");
        expect(
            receivingState({
                pedido: summary("1", "QUARENTENA"),
                divergente: true,
                notas: notes,
            }),
        ).toBe("Divergente");
        expect(
            receivingState({
                pedido: summary("1", "EFETIVADO"),
                divergente: true,
                notas: notes,
            }),
        ).toBe("Conferido com divergência");
    });
    it("descarta detalhe atrasado após mudança do cliente, mesmo se o transporte ignorar cancelamento", async () => {
        const first = summary("101", "QUARENTENA"),
            second = { ...summary("202", "QUARENTENA"), clienteId: "2" };
        let resolveFirst:
            ((value: ReturnType<typeof result>) => void) | undefined;
        const transport: Transport = {
            send: vi.fn(async (request) => {
                if (request.endpoint.id === "PedidoEntradaController.listar")
                    return result({
                        itens: [
                            request.query.clienteId === "1" ? first : second,
                        ],
                        pagina: "0",
                        tamanho: "20",
                        totalItens: "1",
                        totalPaginas: "1",
                    });
                if (request.params.id === "101")
                    return new Promise<ReturnType<typeof result>>((resolve) => {
                        resolveFirst = resolve;
                    });
                return result({
                    pedido: second,
                    divergente: false,
                    notas: [note("802", "902", "2026-10-08T12:00:00Z")],
                });
            }),
        };
        const view = render(<Harness transport={transport} />);
        await waitFor(() => expect(resolveFirst).toBeDefined());
        view.rerender(
            <Harness
                transport={transport}
                context={{ clienteId: "2", armazemId: "1" }}
            />,
        );
        await screen.findByRole("cell", { name: "1/802" });
        resolveFirst!(
            result({
                pedido: first,
                divergente: true,
                notas: [note("801", "901", "2026-10-08T12:00:00Z")],
            }),
        );
        await waitFor(() =>
            expect(
                screen.queryByRole("button", {
                    name: /Ver detalhes de PED-101/,
                }),
            ).toBeNull(),
        );
        expect(screen.queryByRole("cell", { name: "1/801" })).toBeNull();
        expect(
            screen.getByRole("cell", { name: "Conferido" }),
        ).toBeInTheDocument();
    });
    it("registra duas chegadas distintas sem sair do pedido, com UUID novo e revisão atual", async () => {
        const root = summary("101", "QUARENTENA"),
            details = {
                "101": {
                    pedido: root,
                    divergente: true,
                    notas: [note("301", "401", "2026-10-08T12:00:00Z")],
                },
            };
        const t = transportFor([root], details),
            send = t.send.getMockImplementation()!;
        t.send.mockImplementation(async (request) => {
            if (request.endpoint.id === "PedidoEntradaController.chegada") {
                t.requests.push(request);
                root.versao = String(Number(root.versao) + 1);
                return result({ ...root });
            }
            return send(request);
        });
        render(<Harness transport={t} />);
        await screen.findByRole("cell", { name: "Divergente" });
        click("Continuar conferência");
        const dialog = within(await screen.findByRole("dialog"));
        await dialog.findByRole("heading", {
            name: "Registrar chegada física",
        });
        for (let arrival = 0; arrival < 2; arrival++) {
            if (arrival) click("Registrar chegada física", dialog);
            click("Selecionar item 401 da nota 301", dialog);
            await waitFor(() =>
                expect(
                    dialog.getByLabelText(
                        "Itens / Item 1 / Item da nota (ID) *",
                    ),
                ).toHaveValue("401"),
            );
            fireEvent.change(
                dialog.getByLabelText("Chegada física real (ISO com fuso) *"),
                { target: { value: "2026-10-08T12:00:00Z" } },
            );
            fireEvent.change(dialog.getByLabelText("Observacao *"), {
                target: { value: `Chegada física ${arrival + 1}` },
            });
            fireEvent.change(
                dialog.getByLabelText(
                    "Itens / Item 1 / Quantidade boa recebida *",
                ),
                { target: { value: "10" } },
            );
            click("Conferir e confirmar", dialog);
            click("Confirmar agora", dialog);
            await dialog.findByText(/Resposta FICTÍCIA/);
            await waitFor(() =>
                expect(
                    dialog.queryByText(
                        /Atualizando notas, itens e conferência/,
                    ),
                ).toBeNull(),
            );
        }
        const writes = t.requests.filter(
            (request) => request.endpoint.method !== "GET",
        );
        expect(writes).toHaveLength(2);
        expect(
            writes.map((request) => (request.body as Values).versao),
        ).toEqual(["2", "3"]);
        expect((writes[0].body as Values).operacaoId).not.toBe(
            (writes[1].body as Values).operacaoId,
        );
        expect(writes.every((request) => request.params.id === "101")).toBe(
            true,
        );
    });
});
