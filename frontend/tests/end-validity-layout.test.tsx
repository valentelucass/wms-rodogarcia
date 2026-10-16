import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import {
    ApiError,
    type Request,
    type Receipt,
    type Transport,
} from "../src/api/client";
import {
    endValiditySample as sample,
    endValidityReferences as refs,
    endValidityCommitments as commitments,
} from "./support/endValidity";

function setup(
    send: Transport["send"],
    profile: "GESTOR" | "SUPERVISOR" = "GESTOR",
) {
    return render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="EncerramentoController.vigencia"
                context={sample}
                perfil={profile}
                transport={{ send }}
                onSelect={() => {}}
                fixedFields={["id", "tipo", "versao"]}
            />
        </FormReferences.Provider>,
    );
}
const pick = () =>
    fireEvent.click(
        screen.getAllByRole("button", {
            name: /^Usar fechamento .* como compromisso$/,
        })[1],
    );
const review = () =>
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
const confirm = () =>
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));

it("separa a data, resolução e motivo e envia somente após confirmar com referência, versão e compromisso exatos", async () => {
    const send = vi.fn(async (request: Request): Promise<Receipt> => ({
        data: {
            tipo: sample.tipo,
            id: String(request.params.id),
            versao: "9007199254740996",
            inicio: "2026-10-01",
            fim: sample.corte,
        },
        raw: "",
        ficticio: true,
        replay: false,
        requestId: "END-VALIDITY-LOCAL",
    }));
    setup(send);
    expect(
        screen.getByRole("region", { name: "Data do encerramento" }),
    ).toBeVisible();
    expect(screen.getByLabelText("Data de corte *")).toBeRequired();
    expect(screen.getByLabelText("Tipo de compromisso *")).toBeRequired();
    expect(screen.getByLabelText("Compromisso *")).toBeRequired();
    expect(
        screen.getByRole("button", {
            name: /Usar fechamento 14\/10\/2026.*15\/10\/2026 como compromisso/,
        }),
    ).toBeVisible();
    pick();
    expect(screen.getByLabelText("Tipo de compromisso *")).toHaveValue(
        "FECHAMENTO",
    );
    expect(screen.getByLabelText("Compromisso *")).toHaveValue(
        commitments[1].id,
    );
    expect(screen.getByText("Compromisso selecionado")).toBeVisible();
    expect(send).not.toHaveBeenCalled();
    review();
    expect(send).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", { name: "Voltar à conferência" }),
    );
    expect(screen.getByLabelText("Motivo do encerramento *")).toHaveValue(
        sample.motivo,
    );
    expect(screen.getByLabelText("Compromisso *")).toHaveValue(
        commitments[1].id,
    );
    review();
    confirm();
    await waitFor(() => expect(send).toHaveBeenCalledTimes(1));
    expect(send.mock.calls[0][0]).toMatchObject({
        params: { tipo: sample.tipo, id: sample.id },
        body: {
            versao: sample.versao,
            corte: sample.corte,
            resolucao: { tipo: "FECHAMENTO", compromissoId: commitments[1].id },
            motivo: sample.motivo,
        },
    });
});

it("bloqueia controles e atalhos durante envio e preserva a escolha e justificativa após conflito", async () => {
    let reject!: (error: Error) => void;
    const send = vi.fn<Transport["send"]>(
        async () =>
            new Promise((_resolve, fail) => {
                reject = fail;
            }),
    );
    setup(send);
    pick();
    review();
    confirm();
    await waitFor(() => expect(send).toHaveBeenCalledTimes(1));
    expect(screen.getByLabelText("Data de corte *")).toBeDisabled();
    for (const button of screen.getAllByRole("button", {
        name: /^Usar fechamento/,
    }))
        expect(button).toBeDisabled();
    await act(async () =>
        reject(
            new ApiError(
                "Revisão da vigência mudou",
                409,
                "CONFLITO",
                "END-VALIDITY-CONFLICT",
            ),
        ),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        "Revisão da vigência mudou",
    );
    expect(screen.getByLabelText("Data de corte *")).toHaveValue(sample.corte);
    expect(screen.getByLabelText("Compromisso *")).toHaveValue(
        commitments[1].id,
    );
    expect(screen.getByLabelText("Motivo do encerramento *")).toHaveValue(
        sample.motivo,
    );
});

it("não apresenta o comando ao Supervisor nem preenche compromisso ou data ausentes automaticamente", () => {
    const send = vi.fn();
    const view = setup(send, "SUPERVISOR");
    expect(screen.getByRole("status")).toHaveTextContent("GESTOR");
    expect(
        screen.queryByRole("button", { name: "Conferir e confirmar" }),
    ).toBeNull();
    view.unmount();
    render(
        <FormReferences.Provider
            value={{ defaults: {}, options: {}, records: {} }}
        >
            <Operation
                id="EncerramentoController.vigencia"
                context={{
                    ...sample,
                    corte: "",
                    resolucao: { tipo: "", compromissoId: "" },
                }}
                perfil="GESTOR"
                transport={{ send }}
                onSelect={() => {}}
                fixedFields={["id", "tipo", "versao"]}
            />
        </FormReferences.Provider>,
    );
    expect(
        screen.getByText(/Nenhum compromisso consultado disponível/),
    ).toBeVisible();
    expect(screen.getByLabelText("Data de corte *")).toHaveValue("");
    expect(screen.getByLabelText("Tipo de compromisso *")).toHaveValue("");
    review();
    expect(screen.getByRole("alert")).toHaveTextContent("corte");
    expect(send).not.toHaveBeenCalled();
});
