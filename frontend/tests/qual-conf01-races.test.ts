import { afterEach, describe, expect, it, vi } from "vitest";
import { realTransport, type Request } from "../src/api/client";
import { endpoint } from "../src/contracts/runtime";
import { AuthClient } from "../src/auth/client";

function deferred<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((done) => {
        resolve = done;
    });
    return { promise, resolve };
}
const user = {
    id: "qual-fixture",
    nome: "Pessoa fictícia",
    email: "qual@fixture.invalid",
    perfil: "OPERACAO",
    administrador: false,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: 0,
};
const json = (data: unknown, status = 200) =>
    new Response(JSON.stringify(data), { status });
const csrf = () => json({ token: "csrf-ficticio", header: "X-XSRF-TOKEN" });
const tokens = (accessToken: string) =>
    json({ accessToken, expiresIn: 300, usuario: user });
const request = (): Request => ({
    endpoint: endpoint("ClienteController.listar"),
    params: {},
    query: {},
    signal: new AbortController().signal,
});
afterEach(() => vi.restoreAllMocks());

describe("QUAL-CONF01 corridas reais do adaptador, fetch totalmente fictício", () => {
    it("401 de Bearer anterior não encerra a sessão renovada durante a consulta", async () => {
        let access = "access-A-ficticio";
        const reply = deferred<Response>(),
            expired = vi.fn();
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(() => reply.promise);
        const transport = realTransport({
            baseUrl: "https://qual.invalid",
            allowReal: true,
            token: () => access,
            onExpired: expired,
            fetcher,
        });
        const pending = transport
            .send(request())
            .catch((error: unknown) => error);
        expect(fetcher.mock.calls[0][1]?.headers).toMatchObject({
            Authorization: "Bearer access-A-ficticio",
        });
        access = "access-B-ficticio";
        reply.resolve(json({ codigo: "TOKEN_EXPIRADO" }, 401));
        expect(await pending).toMatchObject({
            status: 401,
            code: "TOKEN_EXPIRADO",
        });
        expect(expired).not.toHaveBeenCalled();
        expect(access).toBe("access-B-ficticio");
        expect(fetcher).toHaveBeenCalledOnce();
    });
    it("401 cujo corpo termina após a rotação também preserva o Bearer atual", async () => {
        let access = "access-A-ficticio";
        const body = deferred<string>(),
            expired = vi.fn();
        const response = new Response(null, { status: 401 });
        vi.spyOn(response, "text").mockImplementation(() => body.promise);
        const transport = realTransport({
            baseUrl: "https://qual.invalid",
            allowReal: true,
            token: () => access,
            onExpired: expired,
            fetcher: vi.fn<typeof fetch>().mockResolvedValue(response),
        });
        const pending = transport
            .send(request())
            .catch((error: unknown) => error);
        await vi.waitFor(() => expect(response.text).toHaveBeenCalledOnce());
        access = "access-B-ficticio";
        body.resolve("null");
        expect(await pending).toMatchObject({ status: 401 });
        expect(expired).not.toHaveBeenCalled();
    });
    it.each([401, 403])(
        "recusa %i do Bearer vigente mantém o comportamento correto",
        async (status) => {
            const expired = vi.fn(),
                fetcher = vi
                    .fn<typeof fetch>()
                    .mockResolvedValue(json(null, status));
            const transport = realTransport({
                baseUrl: "https://qual.invalid",
                allowReal: true,
                token: () => "vigente-ficticio",
                onExpired: expired,
                fetcher,
            });
            await expect(transport.send(request())).rejects.toMatchObject({
                status,
                uncertain: false,
            });
            expect(expired).toHaveBeenCalledTimes(status === 401 ? 1 : 0);
            expect(fetcher).toHaveBeenCalledOnce();
        },
    );
    it("duas consultas próximas da expiração compartilham uma renovação e usam novo Bearer", async () => {
        let now = 1_800_000_000_000;
        vi.spyOn(Date, "now").mockImplementation(() => now);
        const rotation = deferred<Response>();
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(async (path) => {
                if (String(path).endsWith("/csrf")) return csrf();
                if (String(path).endsWith("/entrar"))
                    return tokens("A-ficticio");
                if (String(path).endsWith("/renovar")) return rotation.promise;
                return json({
                    content: [user],
                    number: 0,
                    totalPages: 1,
                    totalElements: 1,
                });
            });
        const auth = new AuthClient(fetcher);
        await auth.login(user.email, "Senha-apenas-ficticia-123");
        now += 271_000;
        const queries = [auth.users(0), auth.users(0)];
        await vi.waitFor(() =>
            expect(
                fetcher.mock.calls.filter(([p]) =>
                    String(p).endsWith("/renovar"),
                ),
            ).toHaveLength(1),
        );
        rotation.resolve(tokens("B-ficticio"));
        await Promise.all(queries);
        const calls = fetcher.mock.calls.filter(([p]) =>
            String(p).includes("/usuarios"),
        );
        expect(calls).toHaveLength(2);
        calls.forEach(([, init]) =>
            expect(init?.headers).toMatchObject({
                Authorization: "Bearer B-ficticio",
            }),
        );
        expect(
            fetcher.mock.calls.filter(([p]) => String(p).endsWith("/renovar")),
        ).toHaveLength(1);
    });
    it("cancelamento durante CSRF impede envio do cadastro", async () => {
        const read = deferred<Response>(),
            stop = new AbortController();
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(() => read.promise);
        const pending = new AuthClient(fetcher)
            .create({ nome: "Pessoa fictícia" }, stop.signal)
            .catch((error: unknown) => error);
        await vi.waitFor(() => expect(fetcher).toHaveBeenCalledOnce());
        stop.abort();
        read.resolve(csrf());
        expect(await pending).toMatchObject({
            code: "AUTH_CONTEXT_ENDED",
            uncertain: false,
        });
        expect(fetcher).toHaveBeenCalledOnce();
        expect(fetcher.mock.calls[0][1]?.method).toBe("GET");
    });
    it("503 ao consultar CSRF não envia escrita nem declara escrita incerta", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValue(json(null, 503));
        await expect(
            new AuthClient(fetcher).create({ nome: "Pessoa fictícia" }),
        ).rejects.toMatchObject({ status: 503, uncertain: false });
        expect(fetcher).toHaveBeenCalledOnce();
        expect(String(fetcher.mock.calls[0][0])).toBe("/api/auth/csrf");
    });
});
