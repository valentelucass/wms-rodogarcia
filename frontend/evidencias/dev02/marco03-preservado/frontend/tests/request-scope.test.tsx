import { it, expect, vi } from "vitest";
import {
    act,
    fireEvent,
    render,
    renderHook,
    screen,
} from "@testing-library/react";
import { Operation } from "../src/components/Operation";
import { useReferenceLookup } from "../src/hooks/useReferenceLookup";
import {
    realTransport,
    type Request,
    type Receipt,
    type Transport,
} from "../src/api/client";
import { endpoint } from "../src/contracts/runtime";
import { stringifyExact, toWire } from "../src/contracts/codec";
import { fixture } from "../src/api/mock/fixtures";
import { demoCode } from "../src/api/mock/constants";
import { FictitiousTransport } from "../src/api/fictitious";
it.each([500, 502])(
    "HTTP fictício %i na escrita congela comando e retry conserva bytes/UUID",
    async (status) => {
        const fetched: RequestInit[] = [];
        const transport = realTransport({
            baseUrl: "https://api.invalid",
            allowReal: true,
            token: () => "ficticio",
            onExpired: vi.fn(),
            fetcher: vi.fn((_url, init) => {
                fetched.push(init!);
                return Promise.resolve(
                    fetched.length === 1
                        ? new Response("null", {
                              status,
                              headers: { "X-Request-Id": "incerto-" + status },
                          })
                        : new Response(
                              stringifyExact(
                                  toWire(
                                      endpoint("EstoqueController.bloquear")
                                          .response,
                                      fixture(
                                          endpoint("EstoqueController.bloquear")
                                              .response,
                                      ),
                                  ),
                              ),
                              { status: 200 },
                          ),
                );
            }),
        });
        render(
            <Operation
                id="EstoqueController.bloquear"
                transport={transport}
                context={{
                    codigo: demoCode,
                    versaoUnidade: "9007199254740993",
                }}
                perfil="OPERACAO"
                onSelect={vi.fn()}
            />,
        );
        fireEvent.change(screen.getByLabelText("Motivo / justificativa *"), {
            target: { value: "Bloqueio fictício com resultado desconhecido" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        fireEvent.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findByText(/Resultado desconhecido/);
        expect(
            screen.getByLabelText("Motivo / justificativa *"),
        ).toBeDisabled();
        expect(screen.getByRole("alert")).toHaveTextContent(
            "incerto-" + status,
        );
        expect(
            screen.queryByText(/Resposta do servidor recebida/),
        ).not.toBeInTheDocument();
        fireEvent.click(
            screen.getByRole("button", {
                name: "Repetir exatamente a mesma operação",
            }),
        );
        await screen.findByText(/Resposta do servidor recebida/);
        expect(fetched).toHaveLength(2);
        expect(fetched[1].body).toBe(fetched[0].body);
        expect(fetched[1].body).toContain('"versaoUnidade":9007199254740993');
        expect(String(fetched[1].body)).toMatch(/"operacaoId":"[0-9a-f-]{36}"/);
    },
);

it("interromper escrita conserva incerteza, comando e chave; replay não substitui seleção atual", async () => {
    const requests: Request[] = [];
    const mock = new FictitiousTransport();
    const transport: Transport = {
        send: (request) => {
            requests.push(request);
            if (requests.length === 1)
                return new Promise<Receipt>((_resolve, reject) =>
                    request.signal.addEventListener("abort", () =>
                        reject(new DOMException("interrompida", "AbortError")),
                    ),
                );
            return mock
                .send(request)
                .then((receipt) => ({ ...receipt, replay: true }));
        },
    };
    const onReceipt = vi.fn();
    render(
        <Operation
            id="EstoqueController.bloquear"
            transport={transport}
            context={{ codigo: demoCode, versaoUnidade: "7" }}
            perfil="OPERACAO"
            onSelect={vi.fn()}
            onReceipt={onReceipt}
        />,
    );
    fireEvent.change(screen.getByLabelText("Motivo / justificativa *"), {
        target: { value: "Bloqueio fictício por conferência" },
    });
    fireEvent.click(
        screen.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Confirmar agora" }));
    fireEvent.click(screen.getByRole("button", { name: "Interromper espera" }));
    await screen.findByText(/Resultado desconhecido/);
    expect(screen.getByLabelText("Motivo / justificativa *")).toBeDisabled();
    expect(screen.queryByText(/Resposta FICTÍCIA/)).not.toBeInTheDocument();
    expect(onReceipt).not.toHaveBeenCalled();
    fireEvent.click(
        screen.getByRole("button", {
            name: "Repetir exatamente a mesma operação",
        }),
    );
    await screen.findByText(/Repetição: confirmação original/);
    expect(requests).toHaveLength(2);
    expect(requests[1].body).toEqual(requests[0].body);
    expect(requests[1].params).toEqual(requests[0].params);
    expect(requests[0].signal.aborted).toBe(true);
    expect(requests[1].signal.aborted).toBe(false);
    expect(onReceipt).not.toHaveBeenCalled();
});

it("consulta auxiliar atrasada após novo contexto não publica catálogo nem seleção", async () => {
    let release!: (receipt: Receipt) => void;
    let signal!: AbortSignal;
    const transport: Transport = {
        send: (request) => {
            signal = request.signal;
            return new Promise<Receipt>((resolve) => {
                release = resolve;
            });
        },
    };
    const onReceipt = vi.fn();
    const { result, rerender } = renderHook(
        ({ clienteId }) =>
            useReferenceLookup({
                context: { clienteId, armazemId: "1" },
                transport,
                onReceipt,
            }),
        { initialProps: { clienteId: "1" } },
    );
    let pending!: Promise<void>;
    act(() => {
        pending = result.current.load("ProdutoController.listar");
    });
    expect(result.current.pending).toBe(true);
    rerender({ clienteId: "2" });
    expect(signal.aborted).toBe(true);
    await act(async () => {
        release({
            data: { itens: [{ id: "11", clienteId: "1" }] },
            raw: "",
            requestId: "antiga",
            ficticio: true,
            replay: false,
        });
        await pending;
    });
    expect(result.current.result).toBeUndefined();
    expect(result.current.pending).toBe(false);
    expect(onReceipt).not.toHaveBeenCalled();
});
