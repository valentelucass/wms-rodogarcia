import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { CountConfirmation } from "../src/modules/estoque/CountConfirmation";
import { FormReferences } from "../src/components/FormReferences";
import { ApiError, type Request, type Receipt } from "../src/api/client";
import {
    countConfirmationSample as sample,
    countConfirmationReferences as refs,
} from "./support/countConfirmation";

function setup() {
    const requests: Request[] = [];
    let reject!: (error: Error) => void;
    const send = vi.fn(async (request: Request) => {
        requests.push(request);
        return new Promise<Receipt>((_resolve, fail) => {
            reject = fail;
        });
    });
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="ContagemController.contar"
                context={sample}
                perfil="OPERACAO"
                transport={{ send }}
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return {
        requests,
        send,
        fail: () =>
            reject(
                new ApiError(
                    "Versão fictícia da unidade mudou",
                    409,
                    "CONFLITO",
                    "COUNT-LOCAL",
                ),
            ),
    };
}
const review = () =>
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );

it("separa referências, leitura e motivo, expõe dados técnicos e volta sem enviar ou perder valores", () => {
    const { send } = setup();
    review();
    expect(
        screen.getByRole("dialog", { name: "Confirmar contagem física" }),
    ).toBeVisible();
    expect(
        screen.getByText("Cliente QA fictício revisado · CLI-QA"),
    ).toBeVisible();
    expect(
        screen.getByText("Armazém QA fictício revisado · ARM-QA"),
    ).toBeVisible();
    expect(screen.getByText(sample.contado, { selector: "dd" })).toBeVisible();
    expect(
        screen
            .getByRole("region", { name: "Motivo da contagem" })
            .querySelector("p")?.textContent,
    ).toBe(sample.motivo);
    fireEvent.click(screen.getByText("Dados técnicos da contagem"));
    expect(
        screen.getByText(sample.versaoUnidade, { selector: "dd" }),
    ).toBeVisible();
    expect(
        screen.getByText(sample.observadoEm, { selector: "dd" }),
    ).toBeVisible();
    expect(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    ).toHaveFocus();
    fireEvent.click(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    );
    expect(screen.getByLabelText("Contado *")).toHaveValue(sample.contado);
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        sample.motivo,
    );
    expect(send).not.toHaveBeenCalled();
});

it("mantém confirmação explícita única e o comando exato, preservando o formulário após conflito", async () => {
    const { requests, send, fail } = setup();
    review();
    expect(send).not.toHaveBeenCalled();
    const confirm = screen.getByRole("button", { name: "Confirmar agora" });
    fireEvent.click(confirm);
    fireEvent.click(confirm);
    await waitFor(() => expect(send).toHaveBeenCalledTimes(1));
    expect(requests[0].body).toMatchObject({
        codigoUnidade: sample.codigoUnidade,
        versaoUnidade: sample.versaoUnidade,
        contado: sample.contado,
        observadoEm: sample.observadoEm,
        motivo: sample.motivo,
    });
    expect(
        screen.getByRole("button", { name: "Aguardando confirmação…" }),
    ).toBeDisabled();
    fail();
    await screen.findByText(/Versão fictícia da unidade mudou/);
    expect(screen.getByLabelText("Contado *")).toHaveValue(sample.contado);
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        sample.motivo,
    );
});

it("preserva contagem zero e identifica ausências sem inventar dados", () => {
    render(<CountConfirmation context={{}} values={{ contado: "0" }} />);
    expect(screen.getByText("0", { selector: "dd" })).toBeVisible();
    expect(screen.getAllByText("Não informado").length).toBeGreaterThan(1);
    expect(
        screen.getByText("Registrar a contagem não aplica ajuste de estoque."),
    ).toBeVisible();
});
