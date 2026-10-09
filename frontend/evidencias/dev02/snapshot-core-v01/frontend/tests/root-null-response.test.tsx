import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { parseResponse, stringifyExact, toWire } from "../src/contracts/codec";
import { endpoint, type Values } from "../src/contracts/runtime";
import { realTransport } from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { Operation } from "../src/components/Operation";

describe("FE-VIG-004: raiz estruturada não admite null; fetcher fictício sem HTTP", () => {
    it.each([
        "ClienteDto.Resposta",
        "List<ClienteDto.Resposta>",
        "PaginaResponse<ClienteDto.Resposta>",
    ])("%s recusa null na raiz", (type) =>
        expect(() => parseResponse(type, "null")).toThrow(),
    );
    it("preserva boxed nullable e omissão legítima NON_NULL", () => {
        expect(parseResponse("Long", "null")).toBeNull();
        const dto = { ...(fixture("FechamentoCobrancaDto.Ajuste") as Values) };
        delete dto.tipo;
        delete dto.tratativaOrigemId;
        expect(
            parseResponse(
                "FechamentoCobrancaDto.Ajuste",
                stringifyExact(toWire("FechamentoCobrancaDto.Ajuste", dto)),
            ),
        ).toEqual(dto);
    });
    it.each(["ClienteController.consultar", "ClienteController.criar"])(
        "HTTP200 null preserva status/correlação e incerteza apenas para escrita: %s",
        async (id) => {
            const transport = realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: vi.fn(),
                fetcher: vi
                    .fn()
                    .mockResolvedValue(
                        new Response("null", {
                            status: 200,
                            headers: { "X-Request-Id": "raiz-null" },
                        }),
                    ),
            });
            await expect(
                transport.send({
                    endpoint: endpoint(id),
                    params: { id: "1" },
                    query: {},
                    body: id.endsWith("criar")
                        ? {
                              codigo: "DEMO",
                              nome: "Cliente fictício",
                              documentoFiscal: "00000000000000",
                          }
                        : undefined,
                    signal: new AbortController().signal,
                }),
            ).rejects.toMatchObject({
                status: 200,
                code: "RESPOSTA_INCOMPATIVEL",
                requestId: "raiz-null",
                uncertain: id.endsWith("criar"),
            });
        },
    );
    it("UI conserva payload após 2xx incompatível e repete exatamente o UUID/conteúdo", async () => {
        const bodies: string[] = [];
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(async (_url, init) => {
                bodies.push(String(init?.body));
                return new Response(
                    bodies.length === 1
                        ? "null"
                        : stringifyExact(
                              toWire(
                                  "EstoqueDto.Confirmacao",
                                  fixture("EstoqueDto.Confirmacao"),
                              ),
                          ),
                    {
                        status: 200,
                        headers: { "X-Request-Id": "raiz-null-ui" },
                    },
                );
            });
        const transport = realTransport({
            baseUrl: "https://api.invalid",
            allowReal: true,
            token: () => "ficticio",
            onExpired: vi.fn(),
            fetcher,
        });
        const onReceipt = vi.fn();
        render(
            <Operation
                id="EstoqueController.bloquear"
                transport={transport}
                context={{
                    codigo: "00000000-0000-4000-8000-000000000001",
                    versaoUnidade: "7",
                }}
                perfil="OPERACAO"
                onSelect={vi.fn()}
                onReceipt={onReceipt}
            />,
        );
        const user = userEvent.setup();
        await user.type(
            screen.getByLabelText("Motivo / justificativa *"),
            "Exercício de resultado incerto",
        );
        await user.click(
            screen.getByRole("button", { name: "Conferir e confirmar" }),
        );
        await user.click(
            screen.getByRole("button", { name: "Confirmar agora" }),
        );
        await screen.findByText(/Resultado desconhecido/);
        expect(onReceipt).not.toHaveBeenCalled();
        expect(
            screen.getByLabelText("Motivo / justificativa *"),
        ).toBeDisabled();
        expect(
            screen.queryByRole("button", { name: "Iniciar nova operação" }),
        ).not.toBeInTheDocument();
        await user.click(
            screen.getByRole("button", {
                name: "Repetir exatamente a mesma operação",
            }),
        );
        await screen.findByText(/Resposta do servidor recebida/);
        expect(bodies).toHaveLength(2);
        expect(bodies[1]).toBe(bodies[0]);
        expect(bodies[0]).toMatch(/"operacaoId":"[0-9a-f-]{36}"/);
        expect(onReceipt).toHaveBeenCalledOnce();
    });
});
