import { expect, it, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../src/api/client";
import type { Values } from "../src/contracts/runtime";
import {
    contingencyCountCommand as count,
    contingencyArrivalCommand as arrival,
    contingencyReferences as refs,
} from "./support/contingency";

function setup(
    context: Values = count,
    pending = false,
    perfil: "SUPERVISOR" | "OPERACAO" = "SUPERVISOR",
) {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (pending)
                return new Promise<Receipt>((_resolve, fail) => {
                    reject = fail;
                });
            return {
                data: {
                    id: "9007199254741007",
                    tipo: context.tipo,
                    situacao: "PENDENTE",
                },
                ficticio: true,
                replay: false,
                raw: "",
                requestId: "CONTINGENCY-LAYOUT-LOCAL",
            };
        },
    };
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="ContingenciaController.registrar"
                context={structuredClone(context)}
                transport={transport}
                perfil={perfil}
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
                    "O conteúdo da ocorrência mudou.",
                    409,
                    "CONTEUDO_DIVERGENTE",
                ),
            ),
    };
}
function change(name: string, value: string) {
    fireEvent.change(screen.getByLabelText(name, { exact: true }), {
        target: { value },
    });
}
function confirm() {
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
}

it("edita contagem e dependências com rótulos curtos, preservando os motivos distintos, IDs, decimais e instantes", async () => {
    const state = setup();
    expect(
        screen.queryByText(/Dados do fato de contingência/),
    ).not.toBeInTheDocument();
    fireEvent.click(
        screen.getByRole("button", { name: "Adicionar dependência" }),
    );
    change("Identidade da dependência 1 *", "PLAN-ORIGEM-ALFA");
    fireEvent.click(
        screen.getByRole("button", { name: "Adicionar dependência" }),
    );
    change("Identidade da dependência 2 *", "PLAN-ORIGEM-BETA");
    fireEvent.click(
        screen.getByRole("button", { name: "Remover dependência 1" }),
    );
    expect(screen.getByLabelText("Identidade da dependência 1 *")).toHaveValue(
        "PLAN-ORIGEM-BETA",
    );
    change("Quantidade contada *", "0.000001");
    change("Motivo da contagem *", "Motivo específico da contagem fictícia");
    change(
        "Justificativa do registro *",
        "Justificativa específica do registro fictício",
    );
    confirm();
    expect(state.requests).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(state.requests).toHaveLength(1));
    expect(state.requests[0].body).toMatchObject({
        ...count,
        dependencias: ["PLAN-ORIGEM-BETA"],
        motivo: "Justificativa específica do registro fictício",
        dados: {
            dados: {
                ...count.dados.dados,
                contado: "0.000001",
                motivo: "Motivo específico da contagem fictícia",
            },
        },
    });
});

it("bloqueia os controles durante envio e conserva o conteúdo após conflito", async () => {
    const state = setup(count, true);
    confirm();
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() =>
        expect(
            screen.getByRole("button", { name: "Adicionar dependência" }),
        ).toBeDisabled(),
    );
    expect(screen.getByLabelText("Quantidade contada *")).toBeDisabled();
    await act(async () => state.fail());
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "O conteúdo da ocorrência mudou",
    );
    expect(screen.getByLabelText("Motivo da contagem *")).toHaveValue(
        count.dados.dados.motivo,
    );
    expect(screen.getByLabelText("Justificativa do registro *")).toHaveValue(
        count.motivo,
    );
    expect(state.requests).toHaveLength(1);
});

it("troca o tipo sem transportar os campos da contagem para a chegada, conservando o registro e as dependências", () => {
    setup({ ...count, dependencias: ["PLAN-ORIGEM-ALFA"] });
    change("Tipo *", "CHEGADA");
    expect(
        screen.queryByLabelText("Quantidade contada *"),
    ).not.toBeInTheDocument();
    expect(screen.getByLabelText("Chegada física real *")).toBeInTheDocument();
    expect(screen.getByLabelText("Justificativa do registro *")).toHaveValue(
        count.motivo,
    );
    expect(screen.getByLabelText("Identidade da dependência 1 *")).toHaveValue(
        "PLAN-ORIGEM-ALFA",
    );
    expect(screen.getByLabelText("Fonte *")).toHaveValue(count.fonte);
});

it("envia a chegada pelo contrato existente sem perder pedido, item, versão, quantidades e instante", async () => {
    const state = setup(arrival);
    change("Observacao *", "Observação fictícia atualizada");
    confirm();
    expect(state.requests).toHaveLength(0);
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(state.requests).toHaveLength(1));
    expect(state.requests[0].body).toMatchObject({
        ...arrival,
        dados: {
            ...arrival.dados,
            dados: {
                ...arrival.dados.dados,
                observacao: "Observação fictícia atualizada",
            },
        },
    });
});

it("não oferece registro de ocorrência ao perfil Operação", () => {
    const state = setup(count, false, "OPERACAO");
    expect(screen.getByRole("status")).toHaveTextContent("SUPERVISOR");
    expect(
        screen.queryByRole("button", { name: "Conferir e confirmar" }),
    ).not.toBeInTheDocument();
    expect(state.requests).toHaveLength(0);
});
