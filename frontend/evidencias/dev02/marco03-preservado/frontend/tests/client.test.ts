import { describe, it, expect, vi } from "vitest";
import { realTransport, ApiError, call, type Request } from "../src/api/client";
import { FictitiousTransport } from "../src/api/fictitious";
import { endpoint } from "../src/contracts/runtime";
const request = (id = "ClienteController.listar"): Request => ({
    endpoint: endpoint(id),
    params: { id: "1" },
    query: {},
    signal: new AbortController().signal,
});
describe("Cliente isolado com fetcher fictício; zero HTTP real", () => {
    it.each([500, 502])(
        "%i conserva status/correlação e só escrita fica incerta",
        async (status) => {
            const t = realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: vi.fn(),
                fetcher: vi.fn().mockImplementation(() =>
                    Promise.resolve(
                        new Response("null", {
                            status,
                            headers: { "X-Request-Id": "falha-" + status },
                        }),
                    ),
                ),
            });
            await expect(
                t.send(request("PedidoSaidaController.revalidar")),
            ).rejects.toMatchObject({
                status,
                requestId: "falha-" + status,
                uncertain: true,
            });
            await expect(t.send(request())).rejects.toMatchObject({
                status,
                requestId: "falha-" + status,
                uncertain: false,
            });
        },
    );
    it("fachada tipada usa o mesmo contrato na resposta fictícia", async () => {
        const result = await call(
            new FictitiousTransport(),
            "ClienteController.listar",
            undefined,
            {},
            {},
            new AbortController().signal,
        );
        if (!Array.isArray(result.itens))
            throw new Error("Catálogo esperado no exercício.");
        const id: string | null = result.itens[0].id;
        expect(id).toBe("1");
    });
    it.each(["null", "<html>erro</html>", "{", "[]"])(
        "preserva recusa 409/correlação com corpo %s",
        async (body) => {
            const fetcher = vi.fn().mockResolvedValue(
                new Response(body, {
                    status: 409,
                    headers: { "X-Request-Id": "recusa-409" },
                }),
            );
            const transport = realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: vi.fn(),
                fetcher,
            });
            await expect(transport.send(request())).rejects.toMatchObject({
                status: 409,
                requestId: "recusa-409",
                uncertain: false,
                code: "ERRO_HTTP",
            });
        },
    );
    it("valida antes de listener/timer/fetch, sem recurso pendente", async () => {
        const r = request("ClienteController.criar");
        r.body = {};
        const add = vi.spyOn(r.signal, "addEventListener");
        const timeout = vi.spyOn(globalThis, "setTimeout");
        const fetcher = vi.fn();
        await expect(
            realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: vi.fn(),
                fetcher,
            }).send(r),
        ).rejects.toThrow("Preencha");
        expect(add).not.toHaveBeenCalled();
        expect(timeout).not.toHaveBeenCalled();
        expect(fetcher).not.toHaveBeenCalled();
        add.mockRestore();
        timeout.mockRestore();
    });
    it("401 aciona expiração e remove listener", async () => {
        const r = request();
        const remove = vi.spyOn(r.signal, "removeEventListener");
        const expired = vi.fn();
        await expect(
            realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: expired,
                fetcher: vi
                    .fn()
                    .mockResolvedValue(new Response("null", { status: 401 })),
            }).send(r),
        ).rejects.toMatchObject({ status: 401, uncertain: false });
        expect(expired).toHaveBeenCalledOnce();
        expect(remove).toHaveBeenCalledWith("abort", expect.any(Function));
    });
    it.each(["resposta", "corpo"])(
        "401 atrasado na %s de contexto encerrado não expira sessão vigente",
        async (stage) => {
            const context = new AbortController();
            const expired = vi.fn();
            const remove = vi.spyOn(context.signal, "removeEventListener");
            let release!: (value: Response) => void;
            let releaseBody!: (value: string) => void;
            const response = new Response("null", { status: 401 });
            if (stage === "corpo") {
                vi.spyOn(response, "text").mockImplementation(
                    () =>
                        new Promise<string>((resolve) => {
                            releaseBody = resolve;
                        }),
                );
            }
            // Deliberadamente ignora o signal: representa um conector atrasado.
            const fetcher = vi.fn(
                () =>
                    new Promise<Response>((resolve) => {
                        release = resolve;
                    }),
            );
            const transport = realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "token-do-contexto-antigo-ficticio",
                onExpired: expired,
                fetcher,
            });
            const pending = transport.send({
                ...request(),
                signal: context.signal,
            });
            const rejected = expect(pending).rejects.toMatchObject({
                name: "AbortError",
            });
            if (stage === "corpo") {
                release(response);
                await Promise.resolve();
            }
            context.abort("Contexto antigo encerrado; nova sessão já ativa.");
            if (stage === "corpo") releaseBody("null");
            else release(response);
            await rejected;
            expect(expired).not.toHaveBeenCalled();
            expect(remove).toHaveBeenCalledWith("abort", expect.any(Function));
        },
    );
    it("2xx incompatível conserva status/correlação sem sucesso de escrita", async () => {
        await expect(
            realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: vi.fn(),
                fetcher: vi.fn().mockResolvedValue(
                    new Response("{}", {
                        status: 200,
                        headers: { "X-Request-Id": "contrato-incompativel" },
                    }),
                ),
            }).send(request("PedidoSaidaController.revalidar")),
        ).rejects.toMatchObject({
            status: 200,
            requestId: "contrato-incompativel",
            code: "RESPOSTA_INCOMPATIVEL",
            uncertain: true,
        });
    });
    it("default fail closed não chama API", async () => {
        const fetcher = vi.fn();
        await expect(
            realTransport({
                baseUrl: "https://api.invalid",
                allowReal: false,
                token: () => null,
                onExpired: vi.fn(),
                fetcher,
            }).send(request()),
        ).rejects.toMatchObject({ code: "API_DESABILITADA" });
        expect(fetcher).not.toHaveBeenCalled();
    });
    it("timeout de escrita gera resultado desconhecido e limpa timer", async () => {
        const fetcher: typeof fetch = vi.fn(
            (_url: RequestInfo | URL, init?: RequestInit) =>
                new Promise<Response>((_resolve, reject) =>
                    init?.signal?.addEventListener("abort", () =>
                        reject(new DOMException("timeout", "AbortError")),
                    ),
                ),
        );
        const t = realTransport({
            baseUrl: "https://api.invalid",
            allowReal: true,
            token: () => "ficticio",
            onExpired: vi.fn(),
            fetcher,
            timeoutMs: 5,
        });
        const r = request("PedidoSaidaController.revalidar");
        await expect(t.send(r)).rejects.toMatchObject({
            code: "RESPOSTA_DESCONHECIDA",
            uncertain: true,
        });
    });
    it("resposta perdida conserva mesmo payload/chave e replay original", async () => {
        const transport = new FictitiousTransport();
        transport.scenario = "perdida";
        const r = request("EstoqueController.bloquear");
        r.params = { codigo: "00000000-0000-4000-8000-000000000001" };
        r.body = {
            operacaoId: crypto.randomUUID(),
            versaoUnidade: "0",
            motivo: "Exercício fictício",
        };
        await expect(transport.send(r)).rejects.toBeInstanceOf(ApiError);
        const replay = await transport.send(r);
        expect(replay.replay).toBe(true);
        await expect(
            transport.send({
                ...r,
                body: {
                    ...(r.body as object),
                    motivo: "Outro conteúdo fictício",
                },
            }),
        ).rejects.toMatchObject({ status: 409, code: "OPERACAO_DIVERGENTE" });
    });
    it("perfil é revalidado no respondedor fictício antes do replay", async () => {
        const t = new FictitiousTransport("OPERACAO");
        const r = request("ClienteController.criar");
        r.body = {
            codigo: "DEMO",
            nome: "Nome fictício",
            documentoFiscal: "00000000000000",
        };
        await expect(t.send(r)).rejects.toMatchObject({ status: 403 });
    });
});
