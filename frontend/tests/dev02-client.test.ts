import { describe, expect, it, vi } from "vitest";
import { realTransport } from "../src/api/client";
import { endpoint } from "../src/contracts/runtime";

describe("DEV02 adaptador preparado: fetcher FICTICIO, nunca HTTP", () => {
    it("same-origin conserva /api/v1, filtro Long exato e Bearer em memoria; recusa nao gera mock", async () => {
        const fetcher = vi.fn().mockResolvedValue(
            new Response("null", {
                status: 403,
                headers: { "X-Request-Id": "FICTITIOUS_DENIAL" },
            }),
        );
        const transport = realTransport({
            baseUrl: "http://127.0.0.1:25581",
            allowReal: true,
            token: () => "FIXTURE_ONLY_NOT_REAL",
            onExpired: vi.fn(),
            fetcher,
        });
        await expect(
            transport.send({
                endpoint: endpoint("IndicadorEstoqueController.listar"),
                params: {},
                query: {
                    clienteId: "9007199254740993",
                    armazemId: "7",
                    fuso: "UTC",
                },
                signal: new AbortController().signal,
            }),
        ).rejects.toMatchObject({
            status: 403,
            code: "ERRO_HTTP",
            requestId: "FICTITIOUS_DENIAL",
            uncertain: false,
        });
        expect(String(fetcher.mock.calls[0][0])).toBe(
            "http://127.0.0.1:25581/api/v1/indicadores-estoque?clienteId=9007199254740993&armazemId=7&fuso=UTC",
        );
        expect(fetcher.mock.calls[0][1]).toMatchObject({
            headers: { Authorization: "Bearer FIXTURE_ONLY_NOT_REAL" },
            credentials: "omit",
            redirect: "error",
            cache: "no-store",
        });
        expect(fetcher).toHaveBeenCalledOnce();
        expect(localStorage.length).toBe(0);
        expect(sessionStorage.length).toBe(0);
    });
    it("mutacao 502 no caminho real preparado mantem resultado incerto sem trocar por resposta ficticia", async () => {
        const fetcher = vi
            .fn()
            .mockResolvedValue(
                new Response("<html>FICTITIOUS_GATEWAY_FAILURE</html>", {
                    status: 502,
                }),
            );
        await expect(
            realTransport({
                baseUrl: "http://127.0.0.1:25581",
                allowReal: true,
                token: () => "FIXTURE_ONLY_NOT_REAL",
                onExpired: vi.fn(),
                fetcher,
            }).send({
                endpoint: endpoint("PedidoSaidaController.revalidar"),
                params: { id: "9007199254740993" },
                query: {},
                signal: new AbortController().signal,
            }),
        ).rejects.toMatchObject({ status: 502, uncertain: true });
        expect(String(fetcher.mock.calls[0][0])).toBe(
            "http://127.0.0.1:25581/api/v1/pedidos-saida/9007199254740993/revalidacao",
        );
        expect(fetcher).toHaveBeenCalledOnce();
    });
});
