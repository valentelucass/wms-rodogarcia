import { fireEvent, render, screen, within } from "@testing-library/react";
import { expect, it } from "vitest";
import { Operation } from "../src/components/Operation";
import {
    absorb,
    emptyWorkflow,
    operationContext,
} from "../src/domain/workflow";
import { rowContext } from "../src/domain/recordContext";
import { ApiError, type Request, type Transport } from "../src/api/client";
import { type Values } from "../src/contracts/runtime";

const pedido: Values = {
    id: "91",
    versao: "3",
    clienteId: "1",
    armazemId: "1",
    situacao: "EM_CONFERENCIA",
};
function context(divergente: boolean, detalhePedido: Values = pedido) {
    const detalhe: Values = { pedido: detalhePedido, divergente, notas: [] };
    let workflow = absorb(
        emptyWorkflow(),
        "PedidoEntradaDto.Detalhe",
        detalhe,
        true,
    );
    workflow = absorb(workflow, "PedidoEntradaDto.Resumo", pedido, true);
    return rowContext(
        "PedidoEntradaController.efetivar",
        {},
        workflow,
        pedido,
        "PedidoEntradaDto.Resumo",
        false,
    );
}
function form(ctx: Values, reject = false) {
    const requests: Request[] = [];
    const transport: Transport = {
        send: async (request) => {
            requests.push(request);
            if (reject)
                throw new ApiError(
                    "A conferência mudou: há divergência pendente.",
                    409,
                    "DIVERGENCIA_PENDENTE",
                );
            return {
                data: { ...pedido, situacao: "EFETIVADO" },
                raw: "",
                requestId: "FINALIZATION-FICTITIOUS",
                ficticio: true,
                replay: false,
            };
        },
    };
    render(
        <Operation
            id="PedidoEntradaController.efetivar"
            context={ctx}
            perfil="SUPERVISOR"
            transport={transport}
            onSelect={() => {}}
            fixedFields={["id", "versao"]}
        />,
    );
    fireEvent.change(screen.getByLabelText("Motivo / justificativa *"), {
        target: { value: "Conferência física concluída" },
    });
    return requests;
}
const verify = () =>
    fireEvent.click(
        screen.getByRole("button", {
            name: "Conferir e confirmar",
        }),
    );

it("pedido conferido sem divergência confirma sem pedir escolha e envia false", async () => {
    const requests = form(context(false));
    expect(screen.queryByLabelText(/Aceitar.*iverg/)).toBeNull();
    expect(screen.getByText(/As quantidades físicas conferem/)).toBeVisible();
    verify();
    fireEvent.click(
        within(screen.getByRole("dialog")).getByRole("button", {
            name: "Confirmar agora",
        }),
    );
    await screen.findByText(/Resposta FICTÍCIA/);
    expect(requests).toHaveLength(1);
    expect(requests[0].body).toEqual({
        versao: "3",
        aceitarDivergencias: false,
        motivo: "Conferência física concluída",
    });
});

it("divergência exige escolha explícita e nunca preenche Sim automaticamente", async () => {
    const requests = form(context(true));
    const choice = screen.getByLabelText(/Aceitar.*iverg/);
    expect(choice).toHaveValue("");
    verify();
    expect(await screen.findByRole("alert")).toHaveTextContent(
        /aceitarDivergencias/,
    );
    expect(requests).toHaveLength(0);
    fireEvent.change(choice, { target: { value: "true" } });
    verify();
    fireEvent.click(
        within(screen.getByRole("dialog")).getByRole("button", {
            name: "Confirmar agora",
        }),
    );
    await screen.findByText(/Resposta FICTÍCIA/);
    expect((requests[0].body as Values).aceitarDivergencias).toBe(true);
});

it.each([
    { ...pedido, id: "92" },
    { ...pedido, versao: "2" },
])(
    "conferência de outro pedido ou revisão antiga não dispensa aceite",
    (stale) => {
        const requests = form(context(false, stale));
        expect(screen.getByLabelText(/Aceitar.*iverg/)).toHaveValue("");
        verify();
        expect(requests).toHaveLength(0);
    },
);

it("resposta de divergência surgida depois da consulta permanece bloqueada sem aceite automático", async () => {
    const requests = form(context(false), true);
    verify();
    fireEvent.click(
        within(screen.getByRole("dialog")).getByRole("button", {
            name: "Confirmar agora",
        }),
    );
    expect(await screen.findByRole("alert")).toHaveTextContent(
        /divergência pendente/,
    );
    expect(requests).toHaveLength(1);
    expect((requests[0].body as Values).aceitarDivergencias).toBe(false);
});

it("contexto sem consulta de conferência não cria aceite implícito", () => {
    const ctx = operationContext(
        "PedidoEntradaController.efetivar",
        {},
        absorb(emptyWorkflow(), "PedidoEntradaDto.Resumo", pedido, true),
    );
    form(ctx);
    expect(screen.getByLabelText(/Aceitar.*iverg/)).toHaveValue("");
});
