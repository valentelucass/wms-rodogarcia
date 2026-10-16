import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import { ApiError, type Request, type Receipt } from "../src/api/client";
import {
    calculationSample as sample,
    calculationReferences as refs,
    calculationRecords,
} from "./support/calculation";

const click = (name: string | RegExp) =>
    fireEvent.click(screen.getByRole("button", { name }));
function setup(
    perfil: "GESTOR" | "SUPERVISOR",
    send: (request: Request) => Promise<Receipt>,
) {
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="CalculoCobrancaController.calcular"
                context={sample}
                perfil={perfil}
                transport={{ send }}
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );
}

it("troca/remove/reinforma a resolução, conserva período e motivo no retorno e envia o compromisso exato somente após confirmar", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(
        () => new Promise(() => {}),
    );
    setup("GESTOR", send);
    click(/^Usar fechamento 15\/10\/2026.*16\/10\/2026 como compromisso$/);
    expect(screen.getByLabelText("Compromisso *")).toHaveValue(
        calculationRecords["FechamentoCobrancaDto.Fechamento"][1].id,
    );
    click("Remover resolução financeira");
    expect(screen.queryByLabelText("Compromisso *")).toBeNull();
    click("Informar resolução financeira");
    click(/^Usar fechamento 15\/10\/2026.*16\/10\/2026 como compromisso$/);
    click("Conferir e confirmar");
    expect(send).not.toHaveBeenCalled();
    click("Voltar à conferência");
    expect(screen.getByLabelText("Início do período *")).toHaveValue(
        sample.periodoInicio,
    );
    expect(screen.getByLabelText("Fim exclusivo do período *")).toHaveValue(
        sample.periodoFim,
    );
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        sample.motivo,
    );
    click("Conferir e confirmar");
    click("Confirmar agora");
    await waitFor(() => expect(send).toHaveBeenCalledOnce());
    expect(send.mock.calls[0][0].body).toMatchObject({
        ...sample,
        resolucao: { tipo: "FECHAMENTO", compromissoId: "9007199254740999" },
    });
    expect(screen.getByLabelText("Motivo / justificativa *")).toBeDisabled();
    expect(
        screen.getByRole("button", { name: "Remover resolução financeira" }),
    ).toBeDisabled();
});

it("Supervisor calcula sem herdar resolução do Gestor e preserva dados após recusa", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(async () => {
        throw new ApiError(
            "Conflito fictício do período consultado",
            409,
            "CONFLITO_FICTICIO",
            "CALC-QA",
        );
    });
    setup("SUPERVISOR", send);
    expect(
        screen.getByRole("button", { name: "Informar resolução financeira" }),
    ).toBeDisabled();
    expect(screen.queryByLabelText("Compromisso *")).toBeNull();
    click("Conferir e confirmar");
    click("Confirmar agora");
    await screen.findByText("Conflito fictício do período consultado");
    expect(send).toHaveBeenCalledOnce();
    expect(send.mock.calls[0][0].body).toHaveProperty("resolucao", null);
    expect(send.mock.calls[0][0].body).toMatchObject({
        clienteId: sample.clienteId,
        armazemId: sample.armazemId,
        periodoInicio: sample.periodoInicio,
        periodoFim: sample.periodoFim,
        motivo: sample.motivo,
    });
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        sample.motivo,
    );
    expect(
        screen.getByLabelText("Motivo / justificativa *"),
    ).not.toBeDisabled();
});
