import { useState } from "react";
import { describe, it, expect, vi } from "vitest";
import {
    render,
    screen,
    within,
    fireEvent,
    waitFor,
    cleanup,
} from "@testing-library/react";
import { writeFileSync } from "node:fs";
import { JourneyPage } from "../src/components/JourneyPage";
import { journeys } from "../src/domain/journeys";
import { actionLabel } from "../src/domain/actionLabels";
import { recordPages } from "../src/domain/recordPages";
import { rowContext } from "../src/domain/recordContext";
import { emptyWorkflow, absorb } from "../src/domain/workflow";
import {
    endpoint,
    endpoints,
    records,
    type Values,
    type Perfil,
} from "../src/contracts/runtime";
import { stringifyExact, toWire } from "../src/contracts/codec";
import {
    ApiError,
    type Request,
    type Receipt,
    type Transport,
} from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { canLeavePage } from "../src/domain/pageLeave";

const result = (data: unknown): Receipt => ({
    data,
    ficticio: true,
    raw: "",
    requestId: "REG01-local",
    replay: false,
});
function Harness({
    transport,
    journey = "cadastros",
    context = { clienteId: "1", armazemId: "1" },
    perfil = "GESTOR",
}: {
    transport: Transport;
    journey?: string;
    context?: Values;
    perfil?: Perfil;
}) {
    const [workflow, setWorkflow] = useState(emptyWorkflow);
    return (
        <JourneyPage
            journey={journeys.find((j) => j.id === journey)!}
            transport={transport}
            context={context}
            perfil={perfil}
            workflow={workflow}
            startAction=""
            onRecord={(row, type) =>
                setWorkflow((old) => absorb(old, type, row, true))
            }
            onReceipt={(r, type, _id, request) =>
                setWorkflow((old) => absorb(old, type, r.data, false, request))
            }
            onNavigate={vi.fn()}
        />
    );
}
const click = (
    name: string | RegExp,
    element: ReturnType<typeof within> = screen,
) => fireEvent.click(element.getByRole("button", { name }));
const change = (
    name: string,
    value: string,
    element: ReturnType<typeof within> = screen,
) =>
    fireEvent.change(element.getByLabelText(name, { exact: true }), {
        target: { value },
    });
function clientTransport() {
    const clients: Values[] = [
        {
            ...(fixture("ClienteDto.Resposta") as Values),
            id: "9007199254740993",
            codigo: "CLI-A",
            nome: "Cliente Alfa",
            documentoFiscal: "11111111111",
            versao: "3",
            situacao: "ATIVO",
        },
        {
            ...(fixture("ClienteDto.Resposta") as Values),
            id: "42",
            codigo: "CLI-B",
            nome: "Cliente Beta",
            documentoFiscal: "22222222222222",
            versao: "8",
            situacao: "ENCERRAMENTO_PENDENTE",
        },
    ];
    const requests: Request[] = [];
    let fail = false;
    const transport: Transport = {
        send: vi.fn(async (request) => {
            requests.push(request);
            const id = request.endpoint.id;
            if (fail)
                throw new ApiError(
                    "Registro alterado por outra operação. Consulte novamente.",
                    409,
                    "VERSAO_DESATUALIZADA",
                );
            if (id.endsWith(".listar"))
                return result({
                    itens: clients.map((row) => ({ ...row })),
                    pagina: 0,
                    totalPaginas: 1,
                    tamanho: 20,
                    totalItens: String(clients.length),
                });
            if (id === "ClienteController.criar") {
                const created = {
                    ...(fixture("ClienteDto.Resposta") as Values),
                    ...(request.body as Values),
                    id: "77",
                    versao: "0",
                    situacao: "ATIVO",
                };
                clients.push(created);
                return result({ ...created });
            }
            const row = clients.find((row) => row.id === request.params.id);
            if (!row)
                throw new ApiError(
                    "Cliente não encontrado.",
                    404,
                    "NAO_ENCONTRADO",
                );
            if (request.endpoint.method !== "GET") {
                const body = request.body as Values;
                if (body.versao !== row.versao)
                    throw new ApiError(
                        "Revisão desatualizada.",
                        409,
                        "VERSAO_DESATUALIZADA",
                    );
                if (id.endsWith(".alterar")) row.nome = body.nome;
                if (id.endsWith(".encerrar"))
                    row.situacao = "ENCERRAMENTO_PENDENTE";
                if (id.endsWith(".reativar")) row.situacao = "ATIVO";
                row.versao = String(BigInt(String(row.versao)) + 1n);
            }
            return result({ ...row });
        }),
    };
    return {
        transport,
        requests,
        clients,
        setFail: (value: boolean) => {
            fail = value;
        },
    };
}
async function openEdit(name = "Cliente Alfa") {
    const row = (
        await screen.findByRole("button", {
            name: new RegExp(`Ver detalhes de ${name}`),
        })
    ).closest("tr")!;
    click("Editar", within(row));
    const dialog = await screen.findByRole("dialog");
    await within(dialog).findByLabelText("Nome *");
    return within(dialog);
}
async function confirm(dialog: ReturnType<typeof within>) {
    click("Conferir e confirmar", dialog);
    click("Confirmar agora", dialog);
    await dialog.findByText(/Resposta FICTÍCIA de exercício recebida/);
}
describe("REG01 Clientes por registro", () => {
    it("acessa todas as páginas e recupera a última página removida mantendo a busca e sem selecionar automaticamente seu único registro", async () => {
        const requests: Request[] = [];
        let total = 41;
        const transport: Transport = {
            send: vi.fn(async (r) => {
                requests.push(r);
                const pagina = Number(r.query.pagina ?? 0),
                    tamanho = Number(r.query.tamanho ?? 20);
                const rows = Array.from({ length: total }, (_, i) => ({
                    ...(fixture("ClienteDto.Resposta") as Values),
                    id: String(i + 1),
                    codigo: `CLI-${i + 1}`,
                    nome: `Cliente ${i + 1}`,
                }));
                return result({
                    itens: rows.slice(pagina * tamanho, (pagina + 1) * tamanho),
                    pagina,
                    tamanho,
                    totalItens: String(total),
                    totalPaginas: Math.ceil(total / tamanho),
                });
            }),
        };
        render(<Harness transport={transport} />);
        await screen.findByText("Página 1 de 3");
        click("Próxima");
        await screen.findByText("Página 2 de 3");
        click("Próxima");
        await screen.findByRole("button", {
            name: /Ver detalhes de Cliente 41/,
        });
        expect(screen.queryByRole("dialog")).toBeNull();
        change("Buscar nos registros desta página", "Cliente");
        total = 40;
        click("Atualizar lista");
        await screen.findByText("Página 2 de 2");
        expect(requests.slice(-2).map((r) => Number(r.query.pagina))).toEqual([
            2, 1,
        ]);
        expect(
            screen.getByLabelText("Buscar nos registros desta página"),
        ).toHaveValue("Cliente");
        expect(screen.getByRole("button", { name: "Próxima" })).toBeDisabled();
    });
    it("distingue visão vazia de ausência de resultados para filtro local", async () => {
        const t = clientTransport();
        t.clients.splice(0);
        render(<Harness transport={t.transport} />);
        await screen.findByText(/Ainda não há registros nesta visão/);
        change("Buscar nos registros desta página", "Cliente ausente");
        expect(
            screen.getByText(/Nenhum resultado para os filtros consultados/),
        ).toBeInTheDocument();
    });
    it("abre lista automaticamente, preserva documento e ID Long e carrega edição atual pelo ID", async () => {
        const t = clientTransport();
        render(<Harness transport={t.transport} />);
        await screen.findByRole("button", {
            name: /Ver detalhes de Cliente Alfa/,
        });
        expect(t.requests[0].endpoint.id).toBe("ClienteController.listar");
        expect(
            screen.queryByRole("group", { name: "Ações desta etapa" }),
        ).toBeNull();
        expect(
            screen.getByRole("columnheader", { name: "CPF/CNPJ" }),
        ).toBeInTheDocument();
        const dialog = await openEdit();
        expect(t.requests.at(-1)?.params.id).toBe("9007199254740993");
        expect(dialog.getByLabelText("Nome *")).toHaveValue("Cliente Alfa");
        expect(dialog.getByLabelText("Revisão atual *")).toHaveValue("3");
        expect(dialog.getByLabelText("Identificador *")).toBeDisabled();
        change("Nome *", "Cliente Alfa corrigido", dialog);
        change(
            "Motivo / justificativa *",
            "Correção conferida do nome",
            dialog,
        );
        await confirm(dialog);
        const request = t.requests.find(
            (request) => request.endpoint.id === "ClienteController.alterar",
        )!;
        expect(request.params.id).toBe("9007199254740993");
        expect(request.body).toMatchObject({
            nome: "Cliente Alfa corrigido",
            versao: "3",
            motivo: "Correção conferida do nome",
        });
        click("Voltar à lista", dialog);
        await screen.findByRole("button", {
            name: /Ver detalhes de Cliente Alfa corrigido/,
        });
        expect(
            t.requests.filter((request) => request.endpoint.method !== "GET"),
        ).toHaveLength(1);
    });
    it("cancela edição sem escrita e protege mudança de contexto e etapa quando há edição", async () => {
        const t = clientTransport();
        const confirmLeave = vi.spyOn(window, "confirm").mockReturnValue(false);
        render(<Harness transport={t.transport} />);
        const dialog = await openEdit();
        change("Nome *", "Texto pendente", dialog);
        expect(canLeavePage()).toBe(false);
        click("Voltar à lista", dialog);
        expect(screen.getByRole("dialog")).toBeInTheDocument();
        expect(
            t.requests.filter((request) => request.endpoint.method !== "GET"),
        ).toHaveLength(0);
        confirmLeave.mockReturnValue(true);
        click("Voltar à lista", dialog);
        expect(screen.queryByRole("dialog")).toBeNull();
        expect(t.clients[0].nome).toBe("Cliente Alfa");
        confirmLeave.mockRestore();
    });
    it("cria e atualiza lista sem limpar a busca ativa; oferece acesso ao registro confirmado", async () => {
        const t = clientTransport();
        render(<Harness transport={t.transport} />);
        await screen.findByRole("button", {
            name: /Ver detalhes de Cliente Alfa/,
        });
        change("Buscar nos registros desta página", "Alfa");
        click("Novo cliente");
        const dialog = within(screen.getByRole("dialog"));
        change("Código *", "CLI-C", dialog);
        change("Nome *", "Cliente Gama", dialog);
        change("CPF / CNPJ *", "33333333333", dialog);
        await confirm(dialog);
        click("Voltar à lista", dialog);
        await screen.findByRole("button", { name: "Ver registro confirmado" });
        expect(
            screen.getByLabelText("Buscar nos registros desta página"),
        ).toHaveValue("Alfa");
        expect(
            screen.queryByRole("button", {
                name: /Ver detalhes de Cliente Gama/,
            }),
        ).toBeNull();
        click("Ver registro confirmado");
        await screen.findByText("Cliente Gama", { selector: "dd" });
        expect(t.requests.at(-1)?.params.id).toBe("77");
    });
    it("preserva encerramento pendente e reativação contextual com revisão/motivo", async () => {
        const t = clientTransport();
        render(<Harness transport={t.transport} />);
        const dialog = await openEdit();
        expect(
            dialog.queryByRole("button", { name: "Reativar cliente" }),
        ).toBeNull();
        click("Solicitar desativação de cliente", dialog);
        change("Motivo / justificativa *", "Solicitação motivada", dialog);
        await confirm(dialog);
        expect(t.clients[0].situacao).toBe("ENCERRAMENTO_PENDENTE");
        click("Recarregar dados atuais", dialog);
        await dialog.findByRole("button", { name: "Reativar cliente" });
        click("Reativar cliente", dialog);
        change("Motivo / justificativa *", "Reativação motivada", dialog);
        await confirm(dialog);
        expect(t.clients[0].situacao).toBe("ATIVO");
        expect(
            (
                t.requests.find(
                    (request) =>
                        request.endpoint.id === "ClienteController.reativar",
                )!.body as Values
            ).versao,
        ).toBe("4");
    });
    it("trata conflito de salvamento sem sucesso e conserva os campos", async () => {
        const t = clientTransport();
        render(<Harness transport={t.transport} />);
        const dialog = await openEdit();
        change("Nome *", "Tentativa preservada", dialog);
        change("Motivo / justificativa *", "Correção solicitada", dialog);
        t.setFail(true);
        click("Conferir e confirmar", dialog);
        click("Confirmar agora", dialog);
        await dialog.findByText(/Registro alterado por outra operação/);
        expect(dialog.getByLabelText("Nome *")).toHaveValue(
            "Tentativa preservada",
        );
        expect(
            screen.queryByText(/Operação confirmada no exercício/),
        ).toBeNull();
        expect(t.clients[0].nome).toBe("Cliente Alfa");
    });
    it("consulta sem oferecer criação, edição ou mudança de situação a Operação", async () => {
        const t = clientTransport();
        render(<Harness transport={t.transport} perfil="OPERACAO" />);
        await screen.findByRole("button", {
            name: /Ver detalhes de Cliente Alfa/,
        });
        expect(
            screen.queryByRole("button", { name: "Novo cliente" }),
        ).toBeNull();
        expect(screen.queryByRole("button", { name: "Editar" })).toBeNull();
        click(/Ver detalhes de Cliente Alfa/);
        await screen.findByText("Cliente Alfa", { selector: "dd" });
        expect(
            screen.queryByRole("button", {
                name: "Solicitar desativação de cliente",
            }),
        ).toBeNull();
    });
    it("recusa detalhe de outro registro antes de apresentar o formulário", async () => {
        const t = clientTransport();
        const base = t.transport.send;
        t.transport.send = async (request) =>
            request.endpoint.id === "ClienteController.consultar"
                ? result(t.clients[1])
                : base(request);
        render(<Harness transport={t.transport} />);
        const row = (
            await screen.findByRole("button", {
                name: /Ver detalhes de Cliente Alfa/,
            })
        ).closest("tr")!;
        click("Editar", within(row));
        await screen.findByText(/não corresponde ao registro selecionado/);
        expect(screen.queryByLabelText("Nome *")).toBeNull();
    });
    it("resposta desconhecida permite consultar sem reenviar e encerrar sem declarar sucesso", async () => {
        const t = clientTransport();
        const send = t.transport.send;
        t.transport.send = async (request) => {
            const receipt = await send(request);
            if (request.endpoint.method !== "GET")
                throw new ApiError(
                    "Resposta perdida após processamento",
                    0,
                    "RESPOSTA_DESCONHECIDA",
                    "local",
                    true,
                );
            return receipt;
        };
        render(<Harness transport={t.transport} />);
        const dialog = await openEdit();
        change("Nome *", "Cliente após resposta perdida", dialog);
        change("Motivo / justificativa *", "Correção conferida", dialog);
        click("Conferir e confirmar", dialog);
        click("Confirmar agora", dialog);
        await dialog.findByText(/Resposta perdida após processamento/);
        expect(
            dialog.getByRole("button", { name: "Fechar diálogo" }),
        ).toBeDisabled();
        click("Consultar estado atual sem repetir", dialog);
        await dialog.findByText("Cliente após resposta perdida", {
            selector: "dd",
        });
        expect(
            t.requests.filter((r) => r.endpoint.method !== "GET"),
        ).toHaveLength(1);
        expect(
            screen.queryByText(/Operação confirmada no exercício/),
        ).toBeNull();
        vi.spyOn(window, "confirm").mockReturnValue(true);
        click("Voltar à lista mantendo resultado desconhecido", dialog);
        await screen.findByText(/Resultado da alteração desconhecido/);
        expect(screen.queryByRole("dialog")).toBeNull();
        vi.restoreAllMocks();
    });
});
describe("REG01 expansão das páginas reais", () => {
    it.each(journeys.map((journey) => [journey.id, journey] as const))(
        "%s carrega a fonte de cada etapa e mantém todos os comandos mapeados",
        async (_id, journey) => {
            const requests: Request[] = [];
            const transport: Transport = {
                send: async (request) => {
                    requests.push(request);
                    return result(fixture(request.endpoint.response));
                },
            };
            const context = {
                clienteId: "1",
                armazemId: "1",
                produtoId: "1",
                id: "1",
                pedidoId: "1",
                unidadeId: "1",
                entradaId: "1",
                codigo: "00000000-0000-4000-8000-000000000001",
                tipo: journey.id === "cadastros" ? "CLIENTE" : "",
                registroId: "1",
                fuso: "America/Sao_Paulo",
                servicoId: "1",
            };
            render(
                <Harness
                    transport={transport}
                    journey={journey.id}
                    context={context}
                />,
            );
            for (let i = 0; i < journey.steps.length; i++) {
                if (i > 0 && journey.id !== "entrada")
                    click(
                        new RegExp(
                            journey.steps[i].title.replace(
                                /[.*+?^${}()|[\]\\]/g,
                                "\\$&",
                            ),
                        ),
                    );
                await waitFor(() =>
                    expect(
                        requests.some(
                            (request) =>
                                request.endpoint.id ===
                                recordPages[journey.id][i].source,
                        ),
                    ).toBe(true),
                );
                expect(requests.at(-1)?.endpoint.method).toBe("GET");
                expect(
                    screen.queryByRole("group", { name: "Ações desta etapa" }),
                ).toBeNull();
                expect(recordPages[journey.id]).toHaveLength(
                    journey.steps.length,
                );
                for (const id of journey.steps[i].actions)
                    expect(endpoint(id)).toBeDefined();
            }
            cleanup();
        },
    );
    it("stock e expedição vinculam ações à unidade/pedido do objeto composto, preservando IDs distintos", () => {
        const base = { clienteId: "9", armazemId: "8" };
        const unit = {
            id: "44",
            pedidoId: "12",
            codigo: "uuid-44",
            versao: "6",
        };
        expect(
            rowContext(
                "EstoqueController.bloquear",
                base,
                emptyWorkflow(),
                { unidade: unit, bloqueada: false },
                "EstoqueDto.Unidade",
            ),
        ).toMatchObject({ codigo: "uuid-44", unidadeId: "44", versao: "6" });
        expect(
            rowContext(
                "UnidadeLogisticaController.dividir",
                base,
                emptyWorkflow(),
                { unidade: unit },
                "UnidadeLogisticaDto.Detalhe",
            ),
        ).toMatchObject({ pedidoId: "12", unidadeId: "44", versao: "6" });
        expect(
            rowContext(
                "ExpedicaoController.retirar",
                base,
                emptyWorkflow(),
                { pedido: { id: "56", versao: "7" } },
                "ExpedicaoDto.Detalhe",
            ),
        ).toMatchObject({ id: "56", versao: "7" });
    });
    it("ação de avaria usa a ocorrência selecionada e marco financeiro usa avaria, sem substituir pelo ID da unidade ou do marco", () => {
        const stock = fixture("EstoqueDto.Unidade") as Values;
        const unit = {
            ...(fixture("UnidadeLogisticaDto.Resumo") as Values),
            id: "44",
            pedidoId: "12",
            codigo: "uuid-44",
            versao: "6",
        };
        const damage = {
            ...(fixture("AvariaDto.Ocorrencia") as Values),
            id: "88",
            unidadeId: "44",
            versao: "11",
        };
        let workflow = absorb(
            emptyWorkflow(),
            "EstoqueDto.Unidade",
            { ...stock, unidade: unit },
            true,
        );
        workflow = absorb(workflow, "AvariaDto.Ocorrencia", damage, true);
        expect(
            rowContext(
                "AvariaController.reconhecer",
                {},
                workflow,
                { ...stock, unidade: unit },
                "EstoqueDto.Unidade",
                false,
            ),
        ).toMatchObject({ id: "88", versao: "11", codigo: "uuid-44" });
        expect(
            rowContext(
                "FatoServicoController.marcos",
                {},
                workflow,
                { id: "99", avariaId: "88" },
                "FatoServicoDto.MarcoResposta",
            ),
        ).toMatchObject({ id: "88" });
    });
    it("registra inventário de todas as páginas, contratos e destinos sem excluir capacidades", () => {
        const inventory = journeys.map((journey) => ({
            id: journey.id,
            area: journey.title,
            fe: journey.fe,
            be: journey.be,
            pages: journey.steps.map((step, index) => {
                const definition = recordPages[journey.id][index];
                return {
                    id: `${journey.id}-${index + 1}`,
                    page: step.title,
                    initial: definition.source,
                    detail: definition.detail ?? "dados da consulta",
                    views: definition.views?.map((v) => ({
                        title: v.title,
                        source: v.source,
                        detail: v.detail,
                        actions: v.actions ?? step.actions,
                        context: v.context,
                    })),
                    related: definition.related,
                    dependencies: journey.references,
                    actions: step.actions.map((id) => {
                        const e = endpoint(id);
                        return {
                            id,
                            previousLabel: actionLabel(id),
                            destination: definition.views?.some(
                                (v) => v.detail === id,
                            )
                                ? "detalhe do tipo de cadastro escolhido"
                                : id === definition.source
                                  ? "visão principal"
                                  : id === definition.detail
                                    ? "detalhe do registro"
                                    : definition.headers.includes(id)
                                      ? "cabeçalho"
                                      : "contexto do registro / etapa operacional",
                            method: e.method,
                            path: e.path,
                            input: e.request,
                            fields: e.request ? records[e.request] : [],
                            output: e.response,
                            permission: e.permission,
                            requiredReferences: [...e.params, ...e.query]
                                .filter((field) => field.required)
                                .map((field) => field.name),
                            source: e.source,
                            rules: e.permissionSource,
                        };
                    }),
                    validationAndHistory: step.help,
                };
            }),
        }));
        expect(inventory).toHaveLength(12);
        writeFileSync(
            "evidencias/record-pages-browser-fixtures.json",
            JSON.stringify(
                {
                    journeys: journeys.map((j) => ({
                        id: j.id,
                        title: j.title,
                        steps: j.steps.map((s) => ({ title: s.title })),
                    })),
                    pages: recordPages,
                    endpoints,
                    fixtures: Object.fromEntries(
                        endpoints.map((e) => [
                            e.id,
                            stringifyExact(
                                toWire(e.response, fixture(e.response)),
                            ),
                        ]),
                    ),
                },
                null,
                2,
            ),
        );
        writeFileSync(
            "evidencias/record-pages-inventario.json",
            JSON.stringify(
                {
                    scope: "FE02-REG01",
                    at: new Date().toISOString(),
                    inventory,
                },
                null,
                2,
            ),
        );
    });
});
