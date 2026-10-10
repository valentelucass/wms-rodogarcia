import { readFileSync } from "node:fs";
import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { afterEach, expect, it, vi } from "vitest";
import { AuthApp } from "../src/auth/AuthApp";
import { AuthClient, type User } from "../src/auth/client";
import { decodeAuth, decodeTokens } from "../src/auth/contracts";
import { endpoints } from "../src/contracts/runtime";
import { canLeavePage } from "../src/domain/pageLeave";
import { FictitiousTransport } from "../src/api/fictitious";

const prepared = JSON.parse(
    readFileSync("evidencias/record-pages-browser-fixtures.json", "utf8"),
) as { fixtures: Record<string, string> };
const initialUser: User = {
    id: "init01-local",
    nome: "Pessoa fictícia",
    email: "init@fixture.invalid",
    perfil: "GESTOR",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: "0",
};
const json = (data: unknown, status = 200) =>
    new Response(JSON.stringify(data), { status });
function gate<T>() {
    let resolve!: (value: T) => void;
    const promise = new Promise<T>((done) => {
        resolve = done;
    });
    return { promise, resolve };
}
afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    history.replaceState(null, "", "/");
});

async function mounted() {
    let now = 1_800_000_000_000;
    vi.spyOn(Date, "now").mockImplementation(() => now);
    let renewals = 0;
    const renewal = gate<Response>();
    const calls: {
        path: string;
        method: string;
        catalog: boolean;
        signal?: AbortSignal;
        context: Record<string, string>;
    }[] = [];
    const unknown: string[] = [];
    const isolatedHomeResponse = new FictitiousTransport("GESTOR");
    // AcessoDtos usa número JSON para versao, mas listas de texto para os alcances de cliente/armazém.
    const tokens = (usuario = initialUser) => {
        const wire = JSON.stringify({
            accessToken: "synthetic-memory-only",
            expiresIn: 300,
            usuario: { ...usuario, versao: Number(usuario.versao) },
        });
        decodeAuth(wire, decodeTokens); // Valida também a fixture com o codec HTTP real.
        return new Response(wire);
    };
    vi.stubGlobal(
        "fetch",
        vi.fn<typeof fetch>(async (input, init) => {
            const url = new URL(String(input), location.origin);
            const catalog =
                ["/api/v1/clientes", "/api/v1/armazens"].includes(
                    url.pathname,
                ) && url.searchParams.get("tamanho") === "100";
            calls.push({
                path: url.pathname,
                method: init?.method ?? "GET",
                catalog,
                signal: init?.signal ?? undefined,
                context: Object.fromEntries(
                    [...url.searchParams].filter(([key]) =>
                        ["clienteId", "armazemId"].includes(key),
                    ),
                ),
            });
            if (url.pathname === "/api/auth/csrf")
                return json({
                    token: "synthetic-csrf",
                    header: "X-XSRF-TOKEN",
                });
            if (url.pathname === "/api/auth/renovar")
                return ++renewals === 1 ? tokens() : renewal.promise;
            if (init?.signal?.aborted)
                throw new DOMException("Synthetic abort", "AbortError");
            const contract = endpoints.find(
                (e) =>
                    e.method === (init?.method ?? "GET") &&
                    new RegExp(
                        "^" + e.path.replace(/\{[^}]+\}/g, "[^/]+") + "$",
                    ).test(url.pathname),
            );
            if (!contract) {
                unknown.push(url.pathname);
                throw new Error("Unexpected synthetic request");
            }
            if (catalog)
                return json({
                    itens: [
                        {
                            id: renewals === 1 ? 1 : 2,
                            versao: 0,
                            situacao: "ATIVO",
                            criadoEm: "2026-10-09T12:00:00Z",
                            alteradoEm: "2026-10-09T12:00:00Z",
                            codigo: "DEMO",
                            nome:
                                renewals === 1
                                    ? "Nome do alcance anterior"
                                    : "Nome do alcance atual",
                            documentoFiscal: "00000000000000",
                            cidade: "Cidade fictícia",
                            uf: "SP",
                        },
                    ],
                    pagina: 0,
                    tamanho: 100,
                    totalItens: 1,
                    totalPaginas: 1,
                });
            if (
                ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(
                    url.pathname,
                )
            )
                return new Response(
                    (
                        await isolatedHomeResponse.send({
                            endpoint: contract,
                            params: {},
                            query: Object.fromEntries(url.searchParams),
                            signal:
                                init?.signal ?? new AbortController().signal,
                        })
                    ).raw,
                );
            const raw =
                prepared.fixtures[
                    contract.id === "ClienteController.alterar"
                        ? "ClienteController.consultar"
                        : contract.id
                ];
            if (!raw) throw new Error("Fixture local ausente: " + contract.id);
            return new Response(raw);
        }),
    );
    history.replaceState(null, "", "/#cadastros");
    render(<AuthApp />);
    await screen.findByRole("heading", { name: "Cadastros" });
    await waitFor(() =>
        expect(
            document.querySelector(".record-workspace table tbody tr"),
        ).not.toBeNull(),
    );
    await waitFor(() =>
        expect(screen.getByRole("combobox", { name: "Cliente" })).toBeEnabled(),
    );
    return {
        calls,
        unknown,
        nearExpiry: () => {
            now += 271000;
        },
        release: (user = initialUser) => renewal.resolve(tokens(user)),
        refuse: (status = 503) =>
            renewal.resolve(json({ codigo: "SYNTHETIC_UNAVAILABLE" }, status)),
        renewalCount: () => renewals,
    };
}

it("INIT01 fresh concorrente na Workspace cancela rota anterior e não reenumera catálogo estável", async () => {
    const proof = await mounted();
    proof.nearExpiry();
    // A seleção de outra visão de Cadastros inicia consulta; a navegação seguinte a cancela enquanto fresh aguarda.
    fireEvent.click(screen.getByRole("button", { name: "Cadastros" }));
    await waitFor(() => expect(proof.renewalCount()).toBe(2));
    fireEvent.click(
        screen.getByRole("button", { name: "Entrada e conferência" }),
    );
    fireEvent.click(screen.getByRole("button", { name: "Cadastros" }));
    await act(async () => proof.release());
    await waitFor(() =>
        expect(
            document.querySelector(".record-workspace table tbody tr"),
        ).not.toBeNull(),
    );
    expect(proof.renewalCount()).toBe(2);
    expect(proof.calls.filter((c) => c.catalog)).toHaveLength(2);
    expect(
        proof.calls.some((c) =>
            ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(c.path),
        ),
    ).toBe(false);
    expect(
        proof.calls.filter((c) => c.path === "/api/v1/pedidos-entrada"),
    ).toHaveLength(0);
    expect(screen.getByRole("button", { name: "Sair" })).toBeVisible();
    expect(proof.unknown).toEqual([]);
});

it.each([401, 403, 503])(
    "INIT01 fresh recusado%i encerra a sessão e não apresenta fallback fictício nem envia operação",
    async (status) => {
        const proof = await mounted();
        proof.nearExpiry();
        const before = proof.calls.length;
        fireEvent.click(screen.getByRole("button", { name: "Cadastros" }));
        await waitFor(() => expect(proof.renewalCount()).toBe(2));
        await act(async () => proof.refuse(status));
        await screen.findByRole("button", { name: "Entrar" });
        expect(document.querySelector(".record-workspace")).toBeNull();
        expect(
            proof.calls
                .slice(before)
                .filter((c) => c.path.startsWith("/api/v1/")),
        ).toHaveLength(0);
        expect(proof.unknown).toEqual([]);
    },
);

async function choosePriorContext() {
    const form = within(
        screen.getByRole("form", { name: "Contexto operacional" }),
    );
    for (const title of ["Cliente", "Armazém"]) {
        const input = form.getByRole("combobox", { name: title });
        fireEvent.change(input, {
            target: { value: "Nome do alcance anterior" },
        });
        fireEvent.keyDown(input, { key: "Enter" });
    }
    fireEvent.click(form.getByRole("button", { name: "Aplicar contexto" }));
    await waitFor(() =>
        expect(
            document.querySelector(".record-workspace table tbody tr"),
        ).not.toBeNull(),
    );
}

it("INIT01 direitos revogados com contexto escolhido e rascunho não enviam escrita nem descartam edição sem guarda", async () => {
    const proof = await mounted();
    await choosePriorContext();
    const row = (
        await screen.findByRole("button", {
            name: /Ver detalhes de Exemplo fictício/,
        })
    ).closest("tr")!;
    fireEvent.click(within(row).getByRole("button", { name: "Editar" }));
    const dialog = within(await screen.findByRole("dialog"));
    await dialog.findByLabelText("Nome *");
    fireEvent.change(dialog.getByLabelText("Nome *"), {
        target: { value: "Rascunho preservado INIT01" },
    });
    fireEvent.change(dialog.getByLabelText("Motivo / justificativa *"), {
        target: { value: "Correção apenas sintética" },
    });
    const leave = vi.spyOn(window, "confirm").mockReturnValue(false);
    expect(canLeavePage()).toBe(false);
    proof.nearExpiry();
    fireEvent.click(
        dialog.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(dialog.getByRole("button", { name: "Confirmar agora" }));
    await waitFor(() => expect(proof.renewalCount()).toBe(2));
    await act(async () =>
        proof.release({
            ...initialUser,
            perfil: "OPERACAO",
            administrador: false,
            clientes: ["2"],
            armazens: ["2"],
            versao: "1",
        }),
    );
    expect
        .soft(
            proof.calls.filter(
                (c) => c.path.startsWith("/api/v1/") && c.method !== "GET",
            ),
        )
        .toHaveLength(0);
    expect
        .soft(screen.queryByLabelText("Nome *"))
        .toHaveValue("Rascunho preservado INIT01");
    expect.soft(canLeavePage()).toBe(false);
    expect(proof.unknown).toEqual([]);
    await screen.findByText(/Operações do contexto anterior estão bloqueadas/);
    // O access já foi renovado: uma segunda confirmação não pode esquecer a pendência.
    fireEvent.click(
        dialog.getByRole("button", { name: "Conferir e confirmar" }),
    );
    fireEvent.click(dialog.getByRole("button", { name: "Confirmar agora" }));
    await dialog.findByText(/O comando não foi enviado/);
    expect(proof.renewalCount()).toBe(2);
    expect(
        proof.calls.filter(
            (c) => c.path.startsWith("/api/v1/") && c.method !== "GET",
        ),
    ).toHaveLength(0);
    expect(dialog.getByLabelText("Nome *")).toHaveValue(
        "Rascunho preservado INIT01",
    );
    fireEvent.click(dialog.getByRole("button", { name: "Voltar à lista" }));
    expect(screen.queryByRole("dialog")).not.toBeNull();
    expect(leave).toHaveBeenCalledWith(
        "Descartar as alterações não salvas deste registro?",
    );
    leave.mockReturnValue(true);
    fireEvent.click(dialog.getByRole("button", { name: "Voltar à lista" }));
    expect(screen.queryByRole("dialog")).toBeNull();
    fireEvent.click(
        screen.getByRole("button", { name: "Aplicar acesso atualizado" }),
    );
    await waitFor(() =>
        expect(proof.calls.filter((c) => c.catalog)).toHaveLength(4),
    );
    expect(
        screen.queryByRole("button", { name: "Usuários e acessos" }),
    ).toBeNull();
    expect(screen.getByRole("combobox", { name: "Cliente" })).toHaveValue(
        "Todos os clientes",
    );
    expect(screen.getByRole("combobox", { name: "Armazém" })).toHaveValue(
        "Todos os armazéns",
    );
    fireEvent.click(
        screen.getByRole("button", { name: "Entrada e conferência" }),
    );
    await screen.findByText(
        "Informe as referências obrigatórias nos filtros para carregar esta visão.",
    );
    expect(screen.getByRole("combobox", { name: "Cliente *" })).toHaveValue("");
    expect(screen.getByRole("combobox", { name: "Armazém *" })).toHaveValue("");
    expect(
        proof.calls.filter((c) => c.path === "/api/v1/pedidos-entrada"),
    ).toHaveLength(0);
    const form = within(
        screen.getByRole("form", { name: "Contexto operacional" }),
    );
    for (const title of ["Cliente", "Armazém"]) {
        const input = form.getByRole("combobox", { name: title });
        fireEvent.change(input, { target: { value: "Nome do alcance atual" } });
        fireEvent.keyDown(input, { key: "Enter" });
    }
    fireEvent.click(form.getByRole("button", { name: "Aplicar contexto" }));
    await waitFor(() =>
        expect(
            proof.calls.filter((c) => c.path === "/api/v1/pedidos-entrada"),
        ).toHaveLength(1),
    );
    expect(
        proof.calls.find((c) => c.path === "/api/v1/pedidos-entrada")?.context,
    ).toEqual({ clienteId: "2", armazemId: "2" });
    expect(
        proof.calls.filter(
            (c) => c.path.startsWith("/api/v1/") && c.method !== "GET",
        ),
    ).toHaveLength(0);
});

it("INIT01 direitos retornados por fresh atualizam perfil e invalidam nomes do catálogo anterior sem visitar Início", async () => {
    const proof = await mounted();
    expect(
        screen.getByRole("button", { name: "Usuários e acessos" }),
    ).toBeVisible();
    proof.nearExpiry();
    fireEvent.click(screen.getByRole("button", { name: "Cadastros" }));
    await waitFor(() => expect(proof.renewalCount()).toBe(2));
    await act(async () =>
        proof.release({
            ...initialUser,
            perfil: "OPERACAO",
            administrador: false,
            clientes: ["2"],
            armazens: ["2"],
            versao: "1",
        }),
    );
    expect(screen.getByRole("button", { name: "Sair" })).toBeVisible();
    await waitFor(() =>
        expect(proof.calls.filter((c) => c.catalog)).toHaveLength(4),
    );
    expect(
        screen.queryByRole("button", { name: "Usuários e acessos" }),
    ).toBeNull();
    const form = screen.getByRole("form", { name: "Contexto operacional" });
    fireEvent.click(within(form).getByRole("combobox", { name: "Cliente" }));
    await screen.findByRole("option", { name: /Nome do alcance atual/ });
    expect(
        screen.queryByRole("option", { name: /Nome do alcance anterior/ }),
    ).toBeNull();
    expect(
        proof.calls.some((c) =>
            ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(c.path),
        ),
    ).toBe(false);
    expect(proof.unknown).toEqual([]);
});

it("INIT01 duas solicitações antigas aguardando o mesmo fresh não enviam contexto revogado após guarda aceita", async () => {
    const proof = await mounted();
    await choosePriorContext();
    const fresh = vi.spyOn(AuthClient.prototype, "fresh");
    proof.nearExpiry();
    fireEvent.click(screen.getByRole("button", { name: "Início" }));
    await waitFor(() => expect(proof.renewalCount()).toBe(2));
    await waitFor(() => expect(fresh).toHaveBeenCalledTimes(2));
    await act(async () =>
        proof.release({
            ...initialUser,
            perfil: "OPERACAO",
            administrador: false,
            clientes: ["2"],
            armazens: ["2"],
            versao: "1",
        }),
    );
    const home = () =>
        proof.calls.filter((c) =>
            ["/api/v1/dashboard", "/api/v1/visao-operacao"].includes(c.path),
        );
    await waitFor(() => expect(home().length).toBeGreaterThanOrEqual(2));
    expect(
        home().filter(
            (c) => c.context.clienteId === "1" || c.context.armazemId === "1",
        ),
    ).toHaveLength(0);
    expect(proof.renewalCount()).toBe(2);
    expect(screen.getByRole("combobox", { name: "Cliente" })).toHaveValue(
        "Todos os clientes",
    );
    expect(screen.getByRole("combobox", { name: "Armazém" })).toHaveValue(
        "Todos os armazéns",
    );
    expect(proof.unknown).toEqual([]);
});
