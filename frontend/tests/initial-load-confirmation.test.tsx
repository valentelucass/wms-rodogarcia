import { expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import { InitialLoadConfirmation } from "../src/modules/regularizacao/InitialLoadConfirmation";
import type { Values } from "../src/contracts/runtime";
import { rowContext } from "../src/domain/recordContext";
import { emptyWorkflow } from "../src/domain/workflow";
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

const context = {
    id: load.id,
    versao: load.versao,
    clienteId: load.clienteId,
    armazemId: load.armazemId,
    revisao: load.revisao.numero,
    conteudoHash: load.revisao.conteudoHash,
    leitura: load.etiquetaFornecida,
    etiquetasUnidades: [
        ...load.etiquetas,
        "00000000-0000-4000-8000-000000000004",
    ],
    motivo: "Conferência fictícia da leitura e de todas as etiquetas",
};

it.each(["preparar", "confirmar"])(
    "abre %s pela carga consultada usando o número da revisão, sem substituir por seu objeto",
    (action) => {
        const values = rowContext(
            `CargaInicialController.${action}`,
            {},
            emptyWorkflow(),
            load,
            "CargaInicialDto.Resultado",
        );
        expect(values).toMatchObject({
            id: load.id,
            versao: load.versao,
            revisao: load.revisao.numero,
            conteudoHash: load.revisao.conteudoHash,
            leitura: load.etiquetaFornecida,
            etiquetasUnidades: load.etiquetas,
        });
        expect(load.revisao.dados.unidades).toHaveLength(1);
    },
);

function setup() {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            return new Promise<Receipt>((_resolve, fail) => {
                reject = fail;
            });
        },
    };
    render(
        <FormReferences.Provider
            value={{
                ...refs,
                records: {
                    ...refs.records,
                    "CargaInicialDto.Resultado": [load],
                },
            }}
        >
            <Operation
                id="CargaInicialController.confirmar"
                context={structuredClone(context)}
                fixedFields={["id", "versao"]}
                transport={transport}
                perfil="SUPERVISOR"
                onSelect={vi.fn()}
                inDialog
                hideHeading
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        fail: () =>
            reject(
                new ApiError(
                    "A carga mudou. Consulte a revisão atual.",
                    409,
                    "VERSAO_DESATUALIZADA",
                ),
            ),
    };
}

it("confere a carga e as etiquetas completas e volta ao formulário sem enviar ou alterar valores", () => {
    const { requests } = setup();
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    expect(requests).toHaveLength(0);
    expect(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    ).toHaveFocus();
    expect(screen.getByText(load.referencia)).toBeInTheDocument();
    expect(screen.getByText(load.etiquetaFornecida)).toBeInTheDocument();
    for (const value of context.etiquetasUnidades)
        expect(screen.getByText(value)).toBeInTheDocument();
    expect(screen.getByText("2 etiquetas")).toBeInTheDocument();
    const technical = screen
        .getByText("Dados técnicos da revisão")
        .closest("details")!;
    expect(technical).not.toHaveAttribute("open");
    expect(technical).toHaveTextContent(load.id);
    expect(technical).toHaveTextContent(load.versao);
    expect(technical).toHaveTextContent(load.revisao.conteudoHash);
    expect(
        screen.queryByText("Dados para conferência"),
    ).not.toBeInTheDocument();
    fireEvent.click(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    );
    expect(screen.getByLabelText("Leitura *", { exact: true })).toHaveValue(
        context.leitura,
    );
    expect(
        screen.getByLabelText("Motivo / justificativa *", { exact: true }),
    ).toHaveValue(context.motivo);
    expect(requests).toHaveLength(0);
});

it("envia uma única confirmação explícita com IDs, revisão, hash e etiquetas exatos e preserva dados após conflito", async () => {
    const state = setup();
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    const confirm = screen.getByRole("button", { name: "Confirmar agora" });
    fireEvent.click(confirm);
    fireEvent.click(confirm);
    await waitFor(() => expect(state.requests).toHaveLength(1));
    expect(state.requests[0].params.id).toBe(load.id);
    expect(state.requests[0].body).toMatchObject({
        versao: context.versao,
        conteudoHash: context.conteudoHash,
        leitura: context.leitura,
        etiquetasUnidades: context.etiquetasUnidades,
        motivo: context.motivo,
    });
    expect(String((state.requests[0].body as Values).revisao)).toBe(
        String(context.revisao),
    );
    expect(
        screen.getByRole("button", { name: "Aguardando confirmação…" }),
    ).toBeDisabled();
    state.fail();
    expect(await screen.findByRole("alert")).toHaveTextContent("A carga mudou");
    expect(screen.getByLabelText("Leitura *", { exact: true })).toHaveValue(
        context.leitura,
    );
    expect(
        screen.getByLabelText("Motivo / justificativa *", { exact: true }),
    ).toHaveValue(context.motivo);
    expect(state.requests).toHaveLength(1);
});

it("distingue etiquetas ausentes de uma lista vazia sem apresentar confirmação ou leitura inventada", () => {
    const { rerender } = render(
        <InitialLoadConfirmation context={{}} values={{}} />,
    );
    expect(
        screen.getByText("Etiquetas das unidades não informadas."),
    ).toBeInTheDocument();
    expect(
        screen.queryByRole("button", { name: "Confirmar agora" }),
    ).not.toBeInTheDocument();
    rerender(
        <InitialLoadConfirmation
            context={{}}
            values={{ etiquetasUnidades: [], revisao: 0 }}
        />,
    );
    expect(
        screen.getByText("Nenhuma etiqueta de unidade informada."),
    ).toBeInTheDocument();
    expect(screen.getByText("0")).toBeInTheDocument();
});
