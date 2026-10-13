import { expect, it, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    renderHook,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { ApiError, type Request, type Transport } from "../src/api/client";
import type { Values } from "../src/contracts/runtime";
import { useOperation } from "../src/hooks/useOperation";

function transport() {
    const calls: Request[] = [];
    return {
        calls,
        send: vi.fn(async (request: Request) => {
            calls.push(request);
            return {
                data: { id: "1" },
                raw: "",
                ficticio: true,
                replay: false,
                requestId: "teste",
            };
        }),
    } satisfies Transport & { calls: Request[] };
}
function confirm() {
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    const preview = screen.getByRole("region", { name: "Confirmação da ação" });
    expect(preview).not.toHaveTextContent(
        /Identificador|Revisão atual|Versao Pedido|Pedido \(ID\)/,
    );
    fireEvent.click(
        within(preview).getByRole("button", { name: "Confirmar agora" }),
    );
}
it("atualiza o contexto recebido durante uma requisição recusada antes do novo envio", async () => {
    let reject: (reason: Error) => void = () => {};
    const t: Transport = {
        send: () =>
            new Promise((_, fail) => {
                reject = fail;
            }),
    };
    const { result, rerender } = renderHook(
        ({ context }) =>
            useOperation({
                id: "FiscalCadastroController.complementarCliente",
                transport: t,
                perfil: "GESTOR",
                context,
            }),
        {
            initialProps: {
                context: {
                    id: "42",
                    versao: "7",
                    motivo: "Conferência fiscal",
                    dados: {},
                },
            },
        },
    );
    let sent: Promise<void>;
    act(() => {
        sent = result.current.send();
    });
    expect(result.current.pending).toBe(true);
    rerender({
        context: {
            id: "42",
            versao: "8",
            motivo: "Conferência fiscal",
            dados: {},
        },
    });
    expect(result.current.body.versao).toBe("7");
    await act(async () => {
        reject(new ApiError("Revisão desatualizada", 409, "CONFLITO"));
        await sent;
    });
    expect(result.current.pending).toBe(false);
    expect(result.current.body.versao).toBe("8");
});
it.each(["complementarCliente", "complementarArmazem", "referenciar"])(
    "fiscal %s envia ID/revisão do registro sem campos editáveis",
    async (action) => {
        const t = transport();
        const context = {
            id: "42",
            versao: "7",
            armazemId: "3",
            operacao: "VENDA",
            fonte: "Conferência fictícia",
            motivo: "Atualização cadastral fictícia",
        };
        render(
            <Operation
                id={`FiscalCadastroController.${action}`}
                context={context}
                transport={t}
                perfil="GESTOR"
                onSelect={vi.fn()}
            />,
        );
        expect(screen.queryByLabelText("Identificador *")).toBeNull();
        expect(screen.queryByLabelText("Revisão atual *")).toBeNull();
        expect(screen.queryByText(/Dados do fato de contingência/)).toBeNull();
        expect(
            screen.queryByLabelText("Identificador desta operação *"),
        ).toBeNull();
        confirm();
        await waitFor(() => expect(t.calls).toHaveLength(1));
        expect(t.calls[0].params.id).toBe("42");
        expect(t.calls[0].body).toMatchObject({
            versao: "7",
            operacaoId: expect.any(String),
        });
    },
);
it("referências ausentes não assumem revisão zero nem permitem envio", () => {
    const t = transport();
    render(
        <Operation
            id="FiscalCadastroController.complementarCliente"
            context={{ id: "42" }}
            transport={t}
            perfil="GESTOR"
            onSelect={vi.fn()}
        />,
    );
    expect(screen.getByRole("status")).toHaveTextContent(
        "Selecione e consulte",
    );
    expect(
        screen.queryByRole("button", { name: "Conferir e confirmar" }),
    ).toBeNull();
    expect(t.calls).toHaveLength(0);
});
it.each([false, true])(
    "unitização mantém a repetição incerta com contexto externo alterado ou ausente (%s)",
    async (missing) => {
        const t = transport();
        t.send.mockRejectedValueOnce(
            new ApiError("Resposta incerta", 503, "INCERTA", "teste", true),
        );
        const props = {
            id: "UnidadeLogisticaController.unitizar",
            transport: t,
            perfil: "GESTOR" as const,
            onSelect: vi.fn(),
        };
        const context: Values = {
            pedidoId: "9",
            entradaId: "11",
            versaoPedido: "11",
            motivo: "Distribuição fictícia",
            unidades: [
                {
                    embalagemId: "3",
                    tipo: "PALLET",
                    condicao: "BOA",
                    quantidade: "10",
                },
            ],
        };
        const view = render(<Operation {...props} context={context} />);
        view.rerender(
            <Operation
                {...props}
                context={{ ...context, pedidoId: "19", versaoPedido: "12" }}
            />,
        );
        expect(screen.queryByLabelText("Pedido (ID) *")).toBeNull();
        expect(screen.queryByLabelText("Versao Pedido *")).toBeNull();
        expect(
            screen.queryByLabelText("Identificador desta operação *"),
        ).toBeNull();
        confirm();
        await screen.findByText(/Resultado desconhecido/);
        view.rerender(
            <Operation
                {...props}
                context={
                    missing
                        ? {}
                        : { ...context, pedidoId: "99", versaoPedido: "99" }
                }
            />,
        );
        fireEvent.click(
            screen.getByRole("button", {
                name: "Repetir exatamente a mesma operação",
            }),
        );
        await waitFor(() => expect(t.send).toHaveBeenCalledTimes(2));
        const sent = t.send.mock.calls.map(([request]) => request);
        expect(sent[0].params).toMatchObject({
            pedidoId: "19",
            entradaId: "11",
        });
        expect(sent[0].body).toMatchObject({
            versaoPedido: "12",
            operacaoId: expect.any(String),
        });
        expect(sent[1].params).toEqual(sent[0].params);
        expect(sent[1].body).toEqual(sent[0].body);
    },
);
