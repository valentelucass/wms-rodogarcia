import {
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { expect, it, vi } from "vitest";
import { Operation } from "../src/components/Operation";
import { FormReferences } from "../src/components/FormReferences";
import { ApiError, type Receipt, type Request } from "../src/api/client";
import {
    dispatchOrderSample as sample,
    dispatchOrderReferences as refs,
} from "./support/dispatchOrder";

const click = (name: string) =>
    fireEvent.click(screen.getByRole("button", { name }));
const row = (index: number) =>
    within(screen.getByRole("listitem", { name: `Item ${index}` }));
const setup = (
    send: (request: Request) => Promise<Receipt>,
    itens = sample.itens,
    fixedFields: string[] = [],
) =>
    render(
        <FormReferences.Provider value={refs}>
            <Operation
                id="PedidoSaidaController.criar"
                context={{ ...sample, itens }}
                perfil="OPERACAO"
                transport={{ send }}
                fixedFields={fixedFields}
                onSelect={vi.fn()}
            />
        </FormReferences.Provider>,
    );

it("adiciona/remove itens sem perder dados no retorno e envia IDs/decimais exatos somente após confirmar", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(
        () => new Promise(() => {}),
    );
    setup(send);
    click("Adicionar item");
    fireEvent.change(row(2).getByLabelText("Produto / SKU *"), {
        target: { value: "9007199254740999" },
    });
    fireEvent.change(row(2).getByLabelText("Quantidade *"), {
        target: { value: "2.345678" },
    });
    click("Conferir e confirmar");
    expect(send).not.toHaveBeenCalled();
    click("Voltar à conferência");
    expect(row(1).getByLabelText("Quantidade *")).toHaveValue("1.125000");
    expect(row(2).getByLabelText("Produto / SKU *")).toHaveValue(
        "9007199254740999",
    );
    click("Remover item 1");
    expect(row(1).getByLabelText("Quantidade *")).toHaveValue("2.345678");
    expect(screen.queryByRole("listitem", { name: "Item 2" })).toBeNull();
    expect(screen.getByLabelText("Justificativa do pedido *")).toHaveValue(
        sample.motivo,
    );
    click("Conferir e confirmar");
    click("Confirmar agora");
    await waitFor(() => expect(send).toHaveBeenCalledOnce());
    expect(send.mock.calls[0][0].body).toMatchObject({
        clienteId: sample.clienteId,
        armazemId: sample.armazemId,
        referencia: null,
        itens: [{ produtoId: "9007199254740999", quantidade: "2.345678" }],
        motivo: sample.motivo,
    });
    expect(
        screen.getByRole("button", { name: "Adicionar item" }),
    ).toBeDisabled();
    expect(
        screen.getByRole("button", { name: "Remover item 1" }),
    ).toBeDisabled();
    expect(row(1).getByLabelText("Quantidade *")).toBeDisabled();
});

it("exige ao menos um item e preserva o formulário após recusa do servidor", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(async () => {
        throw new ApiError("Conflito fictício do pedido", 409, "CONFLITO");
    });
    setup(send, []);
    click("Conferir e confirmar");
    expect(send).not.toHaveBeenCalled();
    expect(
        screen.queryByRole("button", { name: "Confirmar agora" }),
    ).toBeNull();
    click("Adicionar item");
    fireEvent.change(row(1).getByLabelText("Quantidade *"), {
        target: { value: "3.250000" },
    });
    click("Conferir e confirmar");
    click("Confirmar agora");
    await screen.findByText("Conflito fictício do pedido");
    expect(send).toHaveBeenCalledOnce();
    expect(row(1).getByLabelText("Quantidade *")).toHaveValue("3.250000");
    expect(row(1).getByLabelText("Produto / SKU *")).toHaveValue(
        sample.itens[0].produtoId,
    );
    expect(screen.getByLabelText("Justificativa do pedido *")).toHaveValue(
        sample.motivo,
    );
    expect(
        screen.getByRole("button", { name: "Adicionar item" }),
    ).toBeEnabled();
});

it("contexto fixo permanece no comando e não ganha seletores editáveis", async () => {
    const send = vi.fn<(request: Request) => Promise<Receipt>>(
        () => new Promise(() => {}),
    );
    setup(send, sample.itens, ["clienteId", "armazemId"]);
    expect(screen.queryByRole("combobox", { name: /^Cliente/ })).toBeNull();
    expect(screen.queryByRole("combobox", { name: /^Armazém/ })).toBeNull();
    click("Conferir e confirmar");
    click("Confirmar agora");
    await waitFor(() => expect(send).toHaveBeenCalledOnce());
    expect(send.mock.calls[0][0].body).toMatchObject(sample);
});
