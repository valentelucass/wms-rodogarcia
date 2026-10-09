import { describe, expect, it, vi } from "vitest";
import { AuthClient, AuthError } from "../src/auth/client";

const user = {
    id: "fixture-user",
    nome: "Pessoa",
    email: "pessoa@fixture.invalid",
    perfil: "OPERACAO",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: ["9007199254740993"],
    armazens: ["1"],
    versao: 0,
};
const json = (data: unknown, status = 200) =>
    new Response(JSON.stringify(data), { status });
const csrf = () => json({ token: "csrf-fixture", header: "X-XSRF-TOKEN" });
const tokens = (token = "A-fixture") =>
    json({ accessToken: token, expiresIn: 300, usuario: user });
const page = () =>
    json({ content: [user], number: 0, totalPages: 1, totalElements: 1 });
function deferred() {
    let resolve!: (r: Response) => void;
    const promise = new Promise<Response>((r) => {
        resolve = r;
    });
    return { promise, resolve };
}

describe("CORR-LOGIN-CAD01 001 identidade/generacao", () => {
    it("001 logout nao ressuscita login/refresh nem dispara expiracao atrasada", async () => {
        const old = deferred();
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(async (input) =>
                String(input).endsWith("/csrf")
                    ? csrf()
                    : String(input).endsWith("/sair")
                      ? new Response(null, { status: 204 })
                      : old.promise,
            );
        const auth = new AuthClient(fetcher),
            expired = vi.fn();
        auth.expired(expired);
        const pending = auth.refresh().catch((e) => e);
        await vi.waitFor(() =>
            expect(
                fetcher.mock.calls.some(([p]) =>
                    String(p).endsWith("/renovar"),
                ),
            ).toBe(true),
        );
        const logout = auth.logout();
        old.resolve(tokens());
        await pending;
        await logout;
        expect(auth.token()).toBeNull();
        expect(expired).not.toHaveBeenCalled();
    });
    it("001 logins concorrentes aceitam apenas a ultima intencao", async () => {
        const old = deferred();
        let entrances = 0;
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(async (input) =>
                String(input).endsWith("/csrf")
                    ? csrf()
                    : ++entrances === 1
                      ? old.promise
                      : tokens("B-fixture"),
            );
        const auth = new AuthClient(fetcher),
            pending = auth.login(user.email, "Sintetica-A-123").catch((e) => e);
        await vi.waitFor(() => expect(entrances).toBe(1));
        const latest = auth.login(user.email, "Sintetica-B-123");
        old.resolve(tokens("A-fixture"));
        expect(await pending).toBeInstanceOf(AuthError);
        await latest;
        expect(auth.token()).toBe("B-fixture");
    });
    it("001 abort externo e401 atrasado preservam sessao", async () => {
        const old = deferred(),
            controller = new AbortController();
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(tokens())
            .mockImplementationOnce(() => old.promise);
        const auth = new AuthClient(fetcher),
            expired = vi.fn();
        auth.expired(expired);
        await auth.login(user.email, "Sintetica-123");
        const pending = auth.users(0, controller.signal).catch((e) => e);
        await vi.waitFor(() => expect(fetcher).toHaveBeenCalledTimes(3));
        controller.abort();
        old.resolve(json({}, 401));
        expect(await pending).toMatchObject({
            code: "AUTH_CONTEXT_ENDED",
            uncertain: false,
        });
        expect(auth.token()).toBe("A-fixture");
        expect(expired).not.toHaveBeenCalled();
    });
    it("001 resposta textual atrasada tambem revalida sessao", async () => {
        const old = deferred();
        let finishText!: (s: string) => void;
        const response = new Response();
        vi.spyOn(response, "text").mockImplementation(
            () =>
                new Promise((resolve) => {
                    finishText = resolve;
                }),
        );
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(tokens())
            .mockImplementationOnce(() => old.promise)
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(tokens("B-fixture"));
        const auth = new AuthClient(fetcher);
        await auth.login(user.email, "Sintetica-123");
        const pending = auth.users(0).catch((e) => e);
        old.resolve(response);
        await vi.waitFor(() => expect(finishText).toBeTypeOf("function"));
        await auth.login(user.email, "Sintetica-456");
        finishText(await page().text());
        expect(await pending).toMatchObject({ code: "AUTH_CONTEXT_ENDED" });
        expect(auth.token()).toBe("B-fixture");
    });
    it("001 401 antigo nao apaga login novo", async () => {
        const old = deferred();
        let n = 0;
        const fetcher = vi
            .fn<typeof fetch>()
            .mockImplementation(async (input) =>
                String(input).endsWith("/csrf")
                    ? csrf()
                    : String(input).endsWith("/entrar")
                      ? tokens(++n === 1 ? "A-fixture" : "B-fixture")
                      : old.promise,
            );
        const auth = new AuthClient(fetcher),
            expired = vi.fn();
        auth.expired(expired);
        await auth.login(user.email, "Sintetica-123");
        const pending = auth.users(0).catch((e) => e);
        await vi.waitFor(() =>
            expect(
                fetcher.mock.calls.some(([p]) =>
                    String(p).includes("/usuarios"),
                ),
            ).toBe(true),
        );
        await auth.login(user.email, "Sintetica-456");
        old.resolve(json(null, 401));
        await pending;
        expect(auth.token()).toBe("B-fixture");
        expect(expired).not.toHaveBeenCalled();
    });
    it("001 401 vigente expira uma vez e 403 preserva token", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(tokens())
            .mockResolvedValueOnce(json(null, 403))
            .mockResolvedValueOnce(new Response("html", { status: 401 }));
        const auth = new AuthClient(fetcher),
            expired = vi.fn();
        auth.expired(expired);
        await auth.login(user.email, "Sintetica-123");
        await expect(auth.users(0)).rejects.toMatchObject({ status: 403 });
        expect(auth.token()).toBe("A-fixture");
        await expect(auth.users(0)).rejects.toMatchObject({ status: 401 });
        expect(auth.token()).toBeNull();
        expect(expired).toHaveBeenCalledOnce();
    });
});
describe("CORR-LOGIN-CAD01 002 Long", () => {
    it("002 reset emite Longmax numerico; Number inseguro recusado antes de fetch", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(new Response(null, { status: 204 }));
        const auth = new AuthClient(fetcher);
        await auth.reset(
            user.id,
            "Temporaria-sintetica-123",
            "9223372036854775807",
        );
        expect(String(fetcher.mock.calls[1][1]?.body)).toContain(
            '"versao":9223372036854775807',
        );
        const count = fetcher.mock.calls.length;
        await expect(
            auth.edit(user.id, { versao: 9007199254740992 }),
        ).rejects.toThrow("sem precisão");
        expect(fetcher).toHaveBeenCalledTimes(count);
    });
    it.each([
        "null",
        '"9007199254740993"',
        "9007199254740993.5",
        "9223372036854775808",
    ])("002 Long resposta incompatível %s", async (version) => {
        const raw = JSON.stringify({
            content: [user],
            number: 0,
            totalPages: 1,
            totalElements: 1,
        }).replace('"versao":0', '"versao":' + version);
        await expect(
            new AuthClient(
                vi.fn<typeof fetch>().mockResolvedValue(new Response(raw)),
            ).users(0),
        ).rejects.toMatchObject({ code: "AUTH_CONTRACT", uncertain: false });
    });
    it("002 revisao/totalElements acima 2^53 ficam exatos e PUT numerico", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(
                new Response(
                    JSON.stringify({
                        content: [user],
                        number: 0,
                        totalPages: 1,
                        totalElements: 1,
                    })
                        .replace('"versao":0', '"versao":9007199254740993')
                        .replace(
                            '"totalElements":1',
                            '"totalElements":9223372036854775807',
                        ),
                ),
            )
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(json(user));
        const auth = new AuthClient(fetcher),
            result = await auth.users(0);
        await auth.edit(result.content[0].id, {
            ...user,
            versao: result.content[0].versao,
        });
        expect(String(result.totalElements)).toBe("9223372036854775807");
        expect(String(fetcher.mock.calls[2][1]?.body)).toContain(
            '"versao":9007199254740993',
        );
        expect(String(fetcher.mock.calls[2][1]?.body)).toContain(
            '"clientes":["9007199254740993"]',
        );
    });
});
describe("CORR-LOGIN-CAD01 003 contrato/erro", () => {
    it.each([
        null,
        {},
        { token: "fixture" },
        { header: "Authorization", token: "fixture" },
        { header: "X-XSRF-TOKEN", token: 42 },
    ])("003 CSRF obrigatório/shape %j", async (data) => {
        const fetcher = vi.fn<typeof fetch>().mockResolvedValue(json(data));
        await expect(
            new AuthClient(fetcher).create({ nome: "Pessoa" }),
        ).rejects.toMatchObject({ code: "AUTH_CONTRACT", uncertain: false });
        expect(fetcher).toHaveBeenCalledOnce();
    });
    it.each(["null", "<html>erro</html>", "{"])(
        "003 erro HTTP corpo %s conserva status/requestId",
        async (raw) => {
            const auth = new AuthClient(
                vi
                    .fn<typeof fetch>()
                    .mockResolvedValue(
                        new Response(raw, {
                            status: 403,
                            headers: { "X-Request-Id": "request-fixture" },
                        }),
                    ),
            );
            await expect(auth.users(0)).rejects.toMatchObject({
                status: 403,
                uncertain: false,
                requestId: "request-fixture",
            });
        },
    );
    it.each([500, 502])(
        "003 escrita %i incerta, sem replay",
        async (status) => {
            const fetcher = vi
                .fn<typeof fetch>()
                .mockResolvedValueOnce(csrf())
                .mockResolvedValueOnce(json(null, status));
            await expect(
                new AuthClient(fetcher).create({ nome: "Pessoa" }),
            ).rejects.toMatchObject({ status, uncertain: true });
            expect(fetcher).toHaveBeenCalledTimes(2);
        },
    );
    it.each([
        "administrador",
        "principal",
        "ativo",
        "trocarSenha",
        "clientes",
        "armazens",
        "nome",
        "email",
    ])("003 usuario campo obrigatório %s", async (key) => {
        const value = { ...user } as Record<string, unknown>;
        delete value[key];
        const auth = new AuthClient(
            vi
                .fn<typeof fetch>()
                .mockResolvedValueOnce(csrf())
                .mockResolvedValueOnce(
                    json({
                        accessToken: "fixture",
                        expiresIn: 300,
                        usuario: value,
                    }),
                ),
        );
        await expect(
            auth.login(user.email, "Sintetica-123"),
        ).rejects.toMatchObject({ code: "AUTH_CONTRACT", uncertain: true });
        expect(auth.token()).toBeNull();
    });
    it("003 void sem204 e User204 recusados", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(json({}))
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(new Response(null, { status: 204 }));
        const auth = new AuthClient(fetcher);
        await expect(
            auth.reset(user.id, "Sintetica-123", "0"),
        ).rejects.toMatchObject({ uncertain: true });
        await expect(auth.create({ nome: "Pessoa" })).rejects.toMatchObject({
            uncertain: true,
        });
    });
    it.each([
        "null",
        "[]",
        '{"content":[],"number":0,"totalPages":1}',
        '{"content":[null],"number":0,"totalPages":1,"totalElements":1}',
    ])("003 pagina invalida %s", async (raw) => {
        await expect(
            new AuthClient(
                vi.fn<typeof fetch>().mockResolvedValue(new Response(raw)),
            ).users(0),
        ).rejects.toBeInstanceOf(AuthError);
    });
    it.each([null, { id: user.id, perfil: "GESTOR" }])(
        "003 tokens usuario incompleto %j",
        async (usuario) => {
            const auth = new AuthClient(
                vi
                    .fn<typeof fetch>()
                    .mockResolvedValueOnce(csrf())
                    .mockResolvedValueOnce(
                        json({
                            accessToken: "sintetico",
                            expiresIn: 300,
                            usuario,
                        }),
                    ),
            );
            await expect(
                auth.login(user.email, "Sintetica-123"),
            ).rejects.toBeInstanceOf(AuthError);
            expect(auth.token()).toBeNull();
        },
    );
    it("003 criar201null e incerto e nao repete", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(json(null, 201));
        await expect(
            new AuthClient(fetcher).create({ nome: "Pessoa sintetica" }),
        ).rejects.toMatchObject({ status: 201, uncertain: true });
        expect(fetcher).toHaveBeenCalledTimes(2);
    });
    it("003 csrf nulo recusa antes de escrever", async () => {
        const fetcher = vi.fn<typeof fetch>().mockResolvedValue(json(null));
        await expect(
            new AuthClient(fetcher).create({ nome: "Pessoa" }),
        ).rejects.toBeInstanceOf(AuthError);
        expect(fetcher).toHaveBeenCalledOnce();
    });
    it("003 pagina valida tolera campos futuros", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValue(
                new Response(
                    JSON.stringify({
                        content: [{ ...user, adicional: "ok" }],
                        number: 0,
                        totalPages: 1,
                        totalElements: 1,
                        futuro: true,
                    }),
                ),
            );
        expect((await new AuthClient(fetcher).users(0)).content[0].id).toBe(
            user.id,
        );
    });
    it("003 pagina preparada coerente", async () => {
        expect(
            (
                await new AuthClient(
                    vi.fn<typeof fetch>().mockResolvedValue(page()),
                ).users(0)
            ).content,
        ).toHaveLength(1);
    });
});
