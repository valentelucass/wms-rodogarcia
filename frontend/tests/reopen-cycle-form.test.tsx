import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import { ApiError, type Request, type Receipt } from "../src/api/client";
import {
    reopenCycleSample as sample,
    reopenCycleReferences as refs,
} from "./support/reopenCycle";

const click = (name: string | RegExp) =>
    fireEvent.click(screen.getByRole("button", { name }));
function setup(send: (request: Request) => Promise<Receipt>) {
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="FechamentoCobrancaController.reabrir"
                context={sample}
                fixedFields={["id", "versao"]}
                perfil="GESTOR"
                transport={{ send }}
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );
}

it("conserva revisão e número atuais ao trocar/remover/reincluir compromisso, voltar e confirmar a reabertura", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(
        () => new Promise(() => {}),
    );
    setup(send);
    expect(screen.getByLabelText("Versão atual do ciclo *")).toHaveValue("2");
    expect(screen.getByLabelText("Cálculo de referência *")).toHaveValue(
        sample.calculoId,
    );
    click("Remover resolução financeira");
    expect(screen.queryByLabelText("Compromisso *")).toBeNull();
    click("Informar resolução financeira");
    click(/^Usar fechamento 15\/10\/2026.*16\/10\/2026 como compromisso$/);
    click("Conferir e confirmar");
    expect(send).not.toHaveBeenCalled();
    click("Voltar à conferência");
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        sample.motivo,
    );
    expect(screen.getByLabelText("Compromisso *")).toHaveValue(
        "9007199254740999",
    );
    click("Conferir e confirmar");
    click("Confirmar agora");
    await waitFor(() => expect(send).toHaveBeenCalledOnce());
    expect(send.mock.calls[0][0].params.id).toBe(sample.id);
    expect(send.mock.calls[0][0].body).toMatchObject({
        versao: sample.versao,
        numero: 2,
        calculoId: sample.calculoId,
        motivo: sample.motivo,
        resolucao: { tipo: "FECHAMENTO", compromissoId: "9007199254740999" },
    });
    expect(
        screen.queryByRole("combobox", { name: "Cálculo de referência *" }),
    ).toBeNull();
    expect(
        screen.getByRole("button", { name: "Remover resolução financeira" }),
    ).toBeDisabled();
});

it("bloqueia os campos após resposta desconhecida e repete o mesmo ID, revisão, versão e operação", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(async () => {
        throw new ApiError(
            "Resposta fictícia perdida após reabertura",
            0,
            "RESPOSTA_DESCONHECIDA",
            "REABRIR-QA",
            true,
        );
    });
    setup(send);
    click("Conferir e confirmar");
    click("Confirmar agora");
    await screen.findByText("Resposta fictícia perdida após reabertura");
    expect(screen.getByLabelText("Versão atual do ciclo *")).toBeDisabled();
    expect(
        screen.queryByRole("combobox", { name: "Cálculo de referência *" }),
    ).toBeNull();
    expect(screen.getByLabelText("Motivo / justificativa *")).toBeDisabled();
    expect(
        screen.getByRole("button", { name: "Remover resolução financeira" }),
    ).toBeDisabled();
    click("Repetir exatamente a mesma operação");
    await waitFor(() => expect(send).toHaveBeenCalledTimes(2));
    expect(send.mock.calls[1][0].params).toEqual(send.mock.calls[0][0].params);
    expect(send.mock.calls[1][0].body).toEqual(send.mock.calls[0][0].body);
    expect(send.mock.calls[0][0].body).toMatchObject({
        versao: sample.versao,
        numero: 2,
        calculoId: sample.calculoId,
        motivo: sample.motivo,
    });
});
