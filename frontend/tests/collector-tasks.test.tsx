import { it, expect, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    screen,
    within,
    waitFor,
} from "@testing-library/react";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import { CollectorTasks } from "../src/modules/coletor/CollectorTasks";
import { Collector } from "../src/components/Collector";
import { absorb, emptyWorkflow } from "../src/domain/workflow";
import { ReferencePresentationProvider } from "../src/components/context/ReferencePresentation";
import { FormReferences } from "../src/components/FormReferences";
import { fixture } from "../src/api/mock/fixtures";
import type { EstoqueDto_Unidade } from "../src/contracts/types";
import type { TarefaColetorDto_Tarefa } from "../src/contracts/types";

const task: TarefaColetorDto_Tarefa = {
    id: "5",
    versao: "2",
    tipo: "INVENTARIO",
    origemId: "91",
    clienteId: "1",
    cliente: "Cliente de teste",
    armazemId: "2",
    armazem: "Armazém de teste",
    referencia: "Inventário 91",
    sku: null,
    descricao: null,
    codigoUnidade: null,
    situacao: "PENDENTE",
    impedimento: null,
    criadaEm: "2026-10-28T12:00:00Z",
    responsavel: null,
    iniciadaEm: null,
    minha: false,
};
const result = (data: unknown): Receipt => ({
    data,
    raw: "",
    requestId: "teste",
    ficticio: true,
    replay: false,
});
function transport(
    overrides: Record<string, (r: Request) => Promise<Receipt>> = {},
): Transport {
    return {
        send: vi.fn(async (r: Request) => {
            if (overrides[r.endpoint.id]) return overrides[r.endpoint.id](r);
            switch (r.endpoint.id) {
                case "TarefaColetorController.disponibilidade":
                    return result({ disponivel: true, mensagem: null });
                case "TarefaColetorController.totais":
                    return result({
                        enderecamento: "0",
                        separacao: "0",
                        inventario: "1",
                    });
                case "TarefaColetorController.listar":
                    return result({
                        itens: [task],
                        pagina: 0,
                        tamanho: 20,
                        totalItens: "1",
                        totalPaginas: 1,
                    });
                case "TarefaColetorController.consultar":
                    return result({
                        tarefa: task,
                        unidade: null,
                        pedido: null,
                        separacoes: [],
                        inventario: null,
                    });
                default:
                    throw new Error("Rota inesperada: " + r.endpoint.id);
            }
        }),
    };
}
function draw(t: Transport) {
    return render(
        <CollectorTasks
            transport={t}
            perfil="OPERACAO"
            context={{ clienteId: "1", armazemId: "2" }}
        />,
    );
}

it("inventário do Coletor consulta posições do armazém da tarefa sem herdar outro contexto", async () => {
    const summary = {
        id: "91",
        versao: "0",
        clienteId: "1",
        armazemId: "2",
        tipo: "SKU",
        situacao: "EM_ANDAMENTO",
        unidadesLidas: "0",
    };
    const t = transport({
        "TarefaColetorController.consultar": async () =>
            result({
                tarefa: {
                    ...task,
                    situacao: "EM_ANDAMENTO",
                    minha: true,
                    responsavel: "operador",
                },
                inventario: summary,
                separacoes: [],
            }),
        "InventarioController.consultar": async () =>
            result({ inventario: summary, leituras: [] }),
        "EnderecoController.listar": async () =>
            result({
                itens: [
                    {
                        ...(fixture("EnderecoDto.Resposta") as object),
                        id: "81",
                        armazemId: "2",
                        codigo: "POSICAO-DA-TAREFA",
                    },
                ],
                pagina: 0,
                tamanho: 100,
                totalItens: "1",
                totalPaginas: 1,
            }),
    });
    render(
        <ReferencePresentationProvider transport={t}>
            <FormReferences.Provider
                value={{
                    defaults: { clienteId: "8", armazemId: "99" },
                    options: {
                        enderecoId: [
                            { value: "999", title: "Posição de outro armazém" },
                        ],
                    },
                }}
            >
                <CollectorTasks
                    transport={t}
                    perfil="OPERACAO"
                    context={{ clienteId: "1", armazemId: "2" }}
                />
            </FormReferences.Provider>
        </ReferencePresentationProvider>,
    );
    fireEvent.click(
        await screen.findByRole("button", { name: /Inventário.*1/ }),
    );
    fireEvent.click(await screen.findByRole("button", { name: /Ver tarefa/ }));
    fireEvent.click(
        await screen.findByRole("button", { name: "Contar mercadorias" }),
    );
    const option = await screen.findByRole("option", {
        name: /POSICAO-DA-TAREFA/,
    });
    expect(option).toHaveValue("81");
    expect(
        screen.queryByRole("option", { name: "Posição de outro armazém" }),
    ).toBeNull();
    const request = vi
        .mocked(t.send)
        .mock.calls.find(
            ([r]) => r.endpoint.id === "EnderecoController.listar",
        )?.[0];
    expect(request?.query.armazemId).toBe("2");
});

it("três cards distinguem zero de tarefas pendentes e preservam o contexto", async () => {
    const t = transport();
    draw(t);
    const end = await screen.findByRole("button", {
        name: /Endereçar.*0.*nenhuma tarefa/,
    });
    expect(end).toBeEnabled();
    expect(
        screen.getByRole("button", { name: /Inventário.*1.*tarefa pendente/ }),
    ).toBeEnabled();
    const r = vi
        .mocked(t.send)
        .mock.calls.find(
            ([v]) => v.endpoint.id === "TarefaColetorController.totais",
        )![0];
    expect(r.query).toEqual({ clienteId: "1", armazemId: "2" });
});
it("erro de consulta não se apresenta como zero", async () => {
    draw(
        transport({
            "TarefaColetorController.totais": async () => {
                throw new ApiError(
                    "Consulta indisponível",
                    503,
                    "INDISPONIVEL",
                );
            },
        }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Consulta indisponível",
    );
    expect(screen.getByRole("button", { name: /Endereçar/ })).toHaveTextContent(
        "—",
    );
    expect(
        screen.queryByText("nenhuma tarefa pendente"),
    ).not.toBeInTheDocument();
});
it("card sem tarefas abre a lista vazia", async () => {
    draw(
        transport({
            "TarefaColetorController.listar": async () =>
                result({
                    itens: [],
                    pagina: 0,
                    tamanho: 20,
                    totalItens: "0",
                    totalPaginas: 0,
                }),
        }),
    );
    fireEvent.click(
        await screen.findByRole("button", { name: /Endereçar.*0/ }),
    );
    expect(
        await screen.findByText("Nenhuma tarefa pendente neste contexto."),
    ).toBeInTheDocument();
});
it("pedido rascunho não oferece iniciar separação", async () => {
    const waiting = {
        ...task,
        tipo: "SEPARACAO",
        situacao: "AGUARDANDO_RESERVA",
        impedimento: "Aguarde a reserva.",
    };
    draw(
        transport({
            "TarefaColetorController.consultar": async () =>
                result({ tarefa: waiting, separacoes: [] }),
        }),
    );
    fireEvent.click(
        await screen.findByRole("button", { name: /Separação para saída.*0/ }),
    );
    fireEvent.click(await screen.findByRole("button", { name: /Ver tarefa/ }));
    const dialog = await screen.findByRole("dialog");
    expect(
        await within(dialog).findByText("Aguarde a reserva."),
    ).toBeInTheDocument();
    expect(
        within(dialog).queryByRole("button", { name: "Iniciar tarefa" }),
    ).not.toBeInTheDocument();
});
it("operador devolve sua tarefa à fila quando a reserva deixa de estar confirmada", async () => {
    const waiting = {
        ...task,
        tipo: "SEPARACAO",
        situacao: "AGUARDANDO_RESERVA",
        impedimento: "Aguarde a reserva.",
        responsavel: "operador",
        minha: true,
    };
    const t = transport({
        "TarefaColetorController.consultar": async () =>
            result({ tarefa: waiting, separacoes: [] }),
        "TarefaColetorController.liberar": async () =>
            result({
                tarefa: { ...waiting, responsavel: null, minha: false },
                separacoes: [],
            }),
    });
    draw(t);
    fireEvent.click(
        await screen.findByRole("button", { name: /Separação para saída.*0/ }),
    );
    fireEvent.click(await screen.findByRole("button", { name: /Ver tarefa/ }));
    const dialog = await screen.findByRole("dialog");
    const release = await within(dialog).findByRole("button", {
        name: "Devolver tarefa à fila",
    });
    expect(release).toBeEnabled();
    expect(
        within(dialog).queryByRole("button", { name: "Iniciar tarefa" }),
    ).not.toBeInTheDocument();
    fireEvent.click(release);
    await waitFor(() =>
        expect(
            within(dialog).queryByRole("button", {
                name: "Devolver tarefa à fila",
            }),
        ).not.toBeInTheDocument(),
    );
    const request = vi
        .mocked(t.send)
        .mock.calls.find(
            ([r]) => r.endpoint.id === "TarefaColetorController.liberar",
        )![0];
    expect(request.params).toEqual({ id: task.id });
    expect(request.body).toMatchObject({ versao: waiting.versao });
});
it("tarefa com outro operador não oferece iniciar nem devolver para Operação", async () => {
    draw(
        transport({
            "TarefaColetorController.consultar": async () =>
                result({
                    tarefa: {
                        ...task,
                        situacao: "EM_ANDAMENTO",
                        responsavel: "outro",
                        minha: false,
                    },
                    separacoes: [],
                }),
        }),
    );
    fireEvent.click(
        await screen.findByRole("button", { name: /Inventário.*1/ }),
    );
    fireEvent.click(await screen.findByRole("button", { name: /Ver tarefa/ }));
    const dialog = await screen.findByRole("dialog");
    await within(dialog).findByText("Esta tarefa está com outro operador.");
    expect(
        within(dialog).queryByRole("button", { name: "Iniciar tarefa" }),
    ).not.toBeInTheDocument();
    expect(
        within(dialog).queryByRole("button", {
            name: "Devolver tarefa à fila",
        }),
    ).not.toBeInTheDocument();
});
it("resposta incerta mantém a chave e bloqueia saída até conferir a mesma solicitação", async () => {
    const calls: Request[] = [];
    const t = transport({
        "TarefaColetorController.iniciar": async (r) => {
            calls.push(r);
            if (calls.length === 1)
                throw new ApiError("Resposta perdida", 0, "INCERTA", "", true);
            return result({
                tarefa: {
                    ...task,
                    situacao: "EM_ANDAMENTO",
                    responsavel: "operador",
                    minha: true,
                },
                separacoes: [],
            });
        },
    });
    draw(t);
    fireEvent.click(
        await screen.findByRole("button", { name: /Inventário.*1/ }),
    );
    fireEvent.click(await screen.findByRole("button", { name: /Ver tarefa/ }));
    fireEvent.click(
        await screen.findByRole("button", { name: "Iniciar tarefa" }),
    );
    const retry = await screen.findByRole("button", {
        name: "Conferir operação pendente",
    });
    expect(
        screen.getByRole("button", { name: "Voltar às tarefas" }),
    ).toBeDisabled();
    fireEvent.click(retry);
    await screen.findByText("Esta tarefa está com você.");
    expect(calls).toHaveLength(2);
    expect(calls[1].body).toEqual(calls[0].body);
});
it("resposta atrasada de outro contexto é descartada", async () => {
    let resolve!: (r: Receipt) => void;
    const t = transport({
        "TarefaColetorController.totais": (r) =>
            String(r.query.clienteId) === "1"
                ? new Promise((r) => {
                      resolve = r;
                  })
                : Promise.resolve(
                      result({
                          enderecamento: "0",
                          separacao: "0",
                          inventario: "0",
                      }),
                  ),
    });
    const { rerender } = draw(t);
    await act(async () => {});
    rerender(
        <CollectorTasks
            transport={t}
            perfil="OPERACAO"
            context={{ clienteId: "9", armazemId: "2" }}
        />,
    );
    await screen.findByRole("button", { name: /Inventário.*0.*nenhuma/ });
    await act(async () =>
        resolve(
            result({
                enderecamento: "999",
                separacao: "999",
                inventario: "999",
            }),
        ),
    );
    expect(screen.queryByText("999")).not.toBeInTheDocument();
});

it("tarefas seguem o contexto aplicado e não substituem a leitura manual", async () => {
    const unit = fixture("EstoqueDto.Unidade") as EstoqueDto_Unidade;
    if (unit.unidade) {
        unit.unidade.clienteId = "1";
        unit.unidade.armazemId = "2";
    }
    const addressTask = {
        ...task,
        tipo: "ENDERECAMENTO",
        codigoUnidade: unit.unidade?.codigo ?? null,
    };
    const detail = {
        tarefa: addressTask,
        unidade: unit,
        pedido: null,
        separacoes: [],
        inventario: null,
    };
    let workflow = emptyWorkflow();
    workflow.selected["UnidadeLogisticaDto.Resumo"] = {
        id: "88",
        codigo: "MANUAL-88",
    };
    const t = transport({
        "TarefaColetorController.listar": async () =>
            result({
                itens: [addressTask],
                pagina: 0,
                tamanho: 20,
                totalItens: "1",
                totalPaginas: 1,
            }),
        "TarefaColetorController.consultar": async () => result(detail),
        "TarefaColetorController.iniciar": async () =>
            result({
                ...detail,
                tarefa: {
                    ...addressTask,
                    situacao: "EM_ANDAMENTO",
                    responsavel: "operador",
                    minha: true,
                },
            }),
    });
    render(
        <Collector
            transport={t}
            perfil="OPERACAO"
            context={{ clienteId: "7", armazemId: "8", codigo: "MANUAL-88" }}
            taskScope={{ clienteId: "1", armazemId: "2" }}
            onReceipt={(r, type, _id, request) => {
                workflow = absorb(workflow, type, r.data, false, request);
            }}
        />,
    );
    fireEvent.click(
        await screen.findByRole("button", { name: /Endereçar.*0/ }),
    );
    const totals = vi
        .mocked(t.send)
        .mock.calls.find(
            ([r]) => r.endpoint.id === "TarefaColetorController.totais",
        )![0];
    expect(totals.query).toEqual({ clienteId: "1", armazemId: "2" });
    fireEvent.click(await screen.findByRole("button", { name: "Ver tarefa" }));
    fireEvent.click(
        await screen.findByRole("button", { name: "Iniciar tarefa" }),
    );
    await screen.findByText("Esta tarefa está com você.");
    expect(screen.getByLabelText("1. Leia o UUID da unidade")).toHaveValue(
        "MANUAL-88",
    );
    expect(workflow.selected["UnidadeLogisticaDto.Resumo"].id).toBe("88");
});
