import { fireEvent, render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import { ApiError, type Receipt, type Request } from "../src/api/client";
import {
    dispatchOrderSample as sample,
    dispatchOrderReferences as refs,
} from "./support/dispatchOrder";

const setup = (error: Error, itens = sample.itens) => {
    const send = vi
        .fn<(request: Request) => Promise<Receipt>>()
        .mockRejectedValue(error);
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="PedidoSaidaController.criar"
                context={{ ...sample, itens }}
                perfil="OPERACAO"
                transport={{ send }}
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );
    return send;
};
const submit = async () => {
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    return screen.findByRole("alert");
};

it("mantém o erro focado e os dados do formulário, com diagnóstico integral recolhido e acessível", async () => {
    const message =
        "Pedido excede o saldo disponível de um dos itens. <img src=x>";
    const code = "SALDO_INSUFICIENTE",
        requestId = "5f452461-e9e8-49b3-a76d-9a5a7a3cb2f5";
    const send = setup(new ApiError(message, 409, code, requestId));
    const alert = await submit();
    expect(alert).toHaveFocus();
    expect(within(alert).getByText(message)).toBeVisible();
    expect(alert.querySelector("img")).toBeNull();
    const summary = within(alert).getByText("Detalhes técnicos"),
        details = summary.closest("details")!;
    expect(details).not.toHaveAttribute("open");
    expect(within(alert).getByText(code)).not.toBeVisible();
    await userEvent.click(summary);
    expect(details).toHaveAttribute("open");
    expect(within(alert).getByText(code)).toBeVisible();
    expect(within(alert).getByText(requestId)).toBeVisible();
    expect(send).toHaveBeenCalledOnce();
    expect(screen.getByLabelText("Quantidade *")).toHaveValue(
        sample.itens[0].quantidade,
    );
    expect(screen.getByLabelText("Justificativa do pedido *")).toHaveValue(
        sample.motivo,
    );
    expect(
        screen.getByRole("button", { name: "Adicionar item" }),
    ).toBeEnabled();
});

it("mantém a orientação da validação no alerta sem criar diagnóstico de API ou enviar dados", () => {
    const send = setup(new Error("Não deve enviar"), []);
    fireEvent.submit(
        screen
            .getByRole("button", { name: "Conferir e confirmar" })
            .closest("form")!,
    );
    const alert = screen.getByRole("alert");
    expect(alert).toHaveFocus();
    expect(alert).toHaveTextContent("Campo:");
    expect(within(alert).queryByText("Detalhes técnicos")).toBeNull();
    expect(send).not.toHaveBeenCalled();
});

it("resultado desconhecido mantém a orientação e os bloqueios fora dos detalhes técnicos", async () => {
    const send = setup(
        new ApiError(
            "Resposta não confirmada",
            0,
            "RESPOSTA_DESCONHECIDA",
            "correlacao-ficticia-desconhecida",
            true,
        ),
    );
    const alert = await submit();
    expect(alert).toHaveClass("feedback--uncertain");
    expect(
        within(alert).getByText(/Resultado desconhecido. Campos conservados/),
    ).toBeVisible();
    expect(
        within(alert).getByText("Detalhes técnicos").closest("details"),
    ).not.toHaveAttribute("open");
    expect(screen.getByLabelText("Quantidade *")).toBeDisabled();
    expect(
        screen.getByRole("button", {
            name: "Repetir exatamente a mesma operação",
        }),
    ).toBeEnabled();
    expect(send).toHaveBeenCalledOnce();
});
