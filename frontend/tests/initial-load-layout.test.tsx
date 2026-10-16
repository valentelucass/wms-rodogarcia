import { it, expect, vi } from "vitest";
import {
    fireEvent,
    render,
    screen,
    within,
    waitFor,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import { RecordDialog } from "../src/components/records/RecordDialog";
import { InitialLoadDetail } from "../src/modules/regularizacao/InitialLoadDetail";
import { emptyWorkflow } from "../src/domain/workflow";
import { journeys } from "../src/domain/journeys";
import {
    ApiError,
    type Request,
    type Receipt,
    type Transport,
} from "../src/api/client";
import {
    initialLoadSample as load,
    initialLoadReferences as refs,
} from "./support/initialLoad";

function setup(hold = false) {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (hold)
                return new Promise<Receipt>((_resolve, fail) => {
                    reject = fail;
                });
            return {
                data: load,
                ficticio: true,
                replay: false,
                raw: "",
                requestId: "LOAD-LAYOUT-LOCAL",
            };
        },
    };
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="CargaInicialController.revisar"
                transport={transport}
                perfil="SUPERVISOR"
                context={{
                    id: load.id,
                    versao: load.versao,
                    dados: structuredClone(load.revisao.dados),
                    motivo: "Revisão fictícia da carga Alfa",
                }}
                fixedFields={["id", "versao"]}
                onSelect={vi.fn()}
                onReceipt={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        fail: () =>
            reject(
                new ApiError(
                    "A revisão da carga mudou.",
                    409,
                    "VERSAO_DESATUALIZADA",
                ),
            ),
    };
}
const change = (name: string, value: string) =>
    fireEvent.change(screen.getByLabelText(name, { exact: true }), {
        target: { value },
    });

it("cria com os IDs exatos e conserva ausências sem inventar origem, datas ou unidades", async () => {
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            return {
                data: load,
                ficticio: true,
                replay: false,
                raw: "",
                requestId: "LOAD-CREATE-LOCAL",
            };
        },
    };
    const missing = {
        ...load.revisao.dados,
        nota: null,
        chegadaReal: null,
        dataFifo: null,
        fonte: null,
        unidades: [],
        quantidadeBoa: "0",
        quantidadeAvariada: "0",
    };
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="CargaInicialController.criar"
                transport={transport}
                perfil="SUPERVISOR"
                onSelect={vi.fn()}
                context={{
                    clienteId: load.clienteId,
                    armazemId: load.armazemId,
                    produtoId: load.produtoId,
                    referencia: load.referencia,
                    quantidade: "3.000001",
                    dados: missing,
                    motivo: "Levantamento fictício com ausências declaradas",
                }}
            />
        </FormReferences.Provider>,
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    expect(requests).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(requests).toHaveLength(1));
    expect(requests[0].body).toMatchObject({
        clienteId: load.clienteId,
        armazemId: load.armazemId,
        produtoId: load.produtoId,
        quantidade: "3.000001",
        dados: missing,
    });
});

it("edita unidades com rótulos curtos, preserva dados não alterados e confirma o payload exato", async () => {
    const { requests } = setup();
    expect(
        screen.queryByText(/Dados do fato de contingência/),
    ).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Adicionar unidade" }));
    const second = within(screen.getByRole("region", { name: "Unidade 2" }));
    for (const [label, value] of [
        ["Embalagem / DUN *", "9007199254741005"],
        ["Tipo *", "BOBINA"],
        ["Condição *", "AVARIADA"],
        ["Quantidade *", "0.000001"],
    ]) {
        fireEvent.change(second.getByLabelText(label, { exact: true }), {
            target: { value },
        });
    }
    fireEvent.click(screen.getByRole("button", { name: "Remover unidade 1" }));
    expect(
        within(
            screen.getByRole("region", { name: "Unidade 1" }),
        ).getByLabelText("Quantidade *"),
    ).toHaveValue("0.000001");
    change("Quantidade boa", "0");
    change("Quantidade avariada", "0.000001");
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    expect(requests).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(requests).toHaveLength(1));
    expect(requests[0].params.id).toBe(load.id);
    expect(requests[0].body).toMatchObject({
        versao: load.versao,
        dados: {
            ...load.revisao.dados,
            quantidadeBoa: "0",
            quantidadeAvariada: "0.000001",
            unidades: [
                {
                    embalagemId: "9007199254741005",
                    tipo: "BOBINA",
                    condicao: "AVARIADA",
                    quantidade: "0.000001",
                },
            ],
        },
    });
});

it("bloqueia a composição durante envio e mantém a revisão após conflito", async () => {
    const state = setup(true);
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Adicionar unidade" }),
        ).toBeDisabled(),
    );
    expect(
        screen.getByRole("button", { name: "Remover unidade 1" }),
    ).toBeDisabled();
    state.fail();
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "A revisão da carga mudou",
    );
    expect(screen.getByLabelText("Fonte")).toHaveValue(
        load.revisao.dados.fonte,
    );
    expect(
        within(
            screen.getByRole("region", { name: "Unidade 1" }),
        ).getByLabelText("Quantidade *"),
    ).toHaveValue("3.000001");
    expect(state.requests).toHaveLength(1);
});

it("consulta unidades sem ações de registro fictícias e seleciona apenas a carga original", () => {
    const onRecord = vi.fn(),
        onClose = vi.fn(),
        send = vi.fn();
    const workflow = emptyWorkflow();
    workflow.catalogs = refs.records;
    render(
        <RecordDialog
            opening={{ row: load, type: "CargaInicialDto.Resultado" }}
            page={{ source: "CargaInicialController.listar", headers: [] }}
            actions={[]}
            transport={{ send }}
            context={{}}
            workflow={workflow}
            perfil="SUPERVISOR"
            journey={journeys.find((j) => j.id === "contagem")!}
            onClose={onClose}
            onUnknownClose={vi.fn()}
            onRecord={onRecord}
            onReceipt={vi.fn()}
            onNavigate={vi.fn()}
        />,
    );
    expect(
        screen.getByRole("dialog", { name: "Carga inicial" }),
    ).toBeInTheDocument();
    expect(
        screen.getByRole("article", { name: "Unidade 1" }),
    ).toHaveTextContent("DUN-ALFA");
    expect(
        screen.queryByRole("button", {
            name: /Selecionar registro 1|Ver registro 1/,
        }),
    ).not.toBeInTheDocument();
    expect(screen.getByText(load.etiquetas[0])).toBeVisible();
    fireEvent.click(
        screen.getByRole("button", { name: "Selecionar esta carga" }),
    );
    expect(onRecord).toHaveBeenCalledExactlyOnceWith(
        load,
        "CargaInicialDto.Resultado",
    );
    expect(onClose).toHaveBeenCalledOnce();
    expect(send).not.toHaveBeenCalled();
});

it("distingue zero, dados ausentes e enum desconhecido sem inventar informação", () => {
    render(
        <FormReferences.Provider value={refs}>
            <InitialLoadDetail
                data={{
                    ...load,
                    quantidadeEstagio: "0",
                    pendencias: null,
                    etiquetas: [],
                    revisao: {
                        ...load.revisao,
                        dados: {
                            ...load.revisao.dados,
                            fonte: null,
                            unidades: [
                                {
                                    ...load.revisao.dados.unidades[0],
                                    tipo: "TIPO_EXTERNO",
                                    condicao: "CONDICAO_EXTERNA",
                                },
                            ],
                        },
                    },
                }}
            />
        </FormReferences.Provider>,
    );
    expect(
        screen.getByText("As pendências não foram informadas nesta consulta."),
    ).toBeVisible();
    expect(screen.getByText("TIPO_EXTERNO")).toBeVisible();
    expect(screen.getByText("CONDICAO_EXTERNA")).toBeVisible();
    expect(screen.getAllByText("0").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Não informado").length).toBeGreaterThan(0);
});
