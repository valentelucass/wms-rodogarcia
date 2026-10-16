import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { addressEditSample } from "./support/addressEdit";
import type { Values } from "../src/contracts/runtime";

function setup(send: Transport["send"]) {
    render(
        <Operation
            id="EnderecoController.alterar"
            context={addressEditSample}
            perfil="GESTOR"
            transport={{ send }}
            onSelect={vi.fn()}
            fixedFields={["id", "versao"]}
        />,
    );
}
const fill = (name: string, value: string) =>
    fireEvent.change(screen.getByLabelText(name), { target: { value } });
const review = () =>
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
const confirm = () =>
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
const receipt = (request: Request) => ({
    data: { ...addressEditSample, ...(request.body as Values) },
    raw: "",
    ficticio: true,
    replay: false,
    requestId: "ADDRESS-EDIT-LOCAL",
});

it("organiza a edição e envia somente após confirmação com ID, revisão, decimais e perfil de unidades exatos", async () => {
    const send = vi.fn(async (request: Request) => receipt(request));
    setup(send);
    for (const name of [
        "Identificação e localização",
        "Capacidade e dimensões",
        "Coleta e unidades permitidas",
        "Justificativa da alteração",
    ])
        expect(screen.getByRole("group", { name })).toBeVisible();
    expect(screen.getByLabelText("Descrição *")).toBeRequired();
    expect(screen.getByLabelText("Motivo / justificativa *")).toBeRequired();
    fill("Código", "QA-POS-BETA");
    fill("Peso máximo (kg)", "6000.125");
    fill("Altura (m)", "5.125");
    fill("Sequência de coleta", "2");
    fireEvent.click(screen.getByRole("checkbox", { name: "Bobina" }));
    fill(
        "Motivo / justificativa *",
        "Correção fictícia da posição.\nConferência preservada.",
    );
    review();
    expect(send).not.toHaveBeenCalled();
    confirm();
    await waitFor(() => expect(send).toHaveBeenCalledTimes(1));
    expect(send.mock.calls[0][0]).toMatchObject({
        params: { id: addressEditSample.id },
        body: {
            versao: addressEditSample.versao,
            codigo: "QA-POS-BETA",
            capacidadePesoKg: "6000.125",
            alturaMetros: "5.125",
            larguraMetros: "5.000",
            profundidadeMetros: "5.000",
            empilhamentoMaximo: 1,
            sequenciaColeta: "2",
            tipoUnidadePermitido: "PALLET_VOLUME",
            motivo: "Correção fictícia da posição.\nConferência preservada.",
        },
    });
});

it("preserva os campos editados após conflito", async () => {
    const send = vi.fn(async () => {
        throw new ApiError(
            "Revisão da posição mudou",
            409,
            "CONFLITO",
            "ADDRESS-CONFLICT",
        );
    });
    setup(send);
    fill("Largura (m)", "4.750");
    fill("Motivo / justificativa *", "Revisão fictícia da largura");
    review();
    confirm();
    await screen.findByText(/Revisão da posição mudou/);
    expect(screen.getByLabelText("Largura (m)")).toHaveValue("4.750");
    expect(screen.getByLabelText("Motivo / justificativa *")).toHaveValue(
        "Revisão fictícia da largura",
    );
    expect(
        screen.getByRole("checkbox", { name: "Todas as opções" }),
    ).toBeChecked();
});

it("bloqueia campos e tipos de unidade durante envio", async () => {
    setup(vi.fn(() => new Promise<never>(() => {})));
    fill("Motivo / justificativa *", "Conferência fictícia");
    review();
    confirm();
    await screen.findByRole("button", { name: "Aguardando confirmação…" });
    expect(screen.getByLabelText("Peso máximo (kg)")).toBeDisabled();
    expect(screen.getByLabelText("Motivo / justificativa *")).toBeDisabled();
    for (const checkbox of screen.getAllByRole("checkbox"))
        expect(checkbox).toBeDisabled();
});

it("não expõe edição de endereço ao perfil Operação", () => {
    const send = vi.fn();
    render(
        <Operation
            id="EnderecoController.alterar"
            context={addressEditSample}
            perfil="OPERACAO"
            transport={{ send }}
            onSelect={vi.fn()}
        />,
    );
    expect(screen.queryByLabelText("Peso máximo (kg)")).not.toBeInTheDocument();
    expect(send).not.toHaveBeenCalled();
});
