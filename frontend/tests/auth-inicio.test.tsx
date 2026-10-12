import {
    act,
    fireEvent,
    render,
    screen,
    waitFor,
    within,
} from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { AuthApp } from "../src/auth/AuthApp";
import type { User } from "../src/auth/client";
import { fixture } from "../src/api/mock/fixtures";
import { stringifyExact, toWire } from "../src/contracts/codec";

const user: User = {
    id: "auth-inicio-local",
    nome: "Pessoa fictícia",
    email: "inicio@fixture.invalid",
    perfil: "GESTOR",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: "0",
};
const json = (value: unknown, status = 200) =>
    new Response(stringifyExact(value), { status });

// Apenas fetch fictício em memória: nenhuma conexão HTTP, backend ou banco.
function server(restored: User | null = null) {
    vi.spyOn(globalThis, "setInterval");
    const state = {
        session: restored,
        loginUser: user,
        loginStatus: 200,
        renewals: 0,
        changes: 0,
    };
    const calls: string[] = [];
    const tokens = (current: User) =>
        json({
            accessToken: "synthetic-memory-only",
            expiresIn: 300,
            usuario: { ...current, versao: toWire("long", current.versao) },
        });
    vi.stubGlobal(
        "fetch",
        vi.fn<typeof fetch>(async (input) => {
            const path = new URL(String(input), location.origin).pathname;
            calls.push(path);
            if (path === "/api/auth/csrf")
                return json({
                    token: "synthetic-csrf",
                    header: "X-XSRF-TOKEN",
                });
            if (path === "/api/auth/renovar") {
                state.renewals++;
                return state.session
                    ? tokens(state.session)
                    : json({ detail: "Entre no WMS." }, 401);
            }
            if (path === "/api/auth/entrar") {
                if (state.loginStatus !== 200)
                    return json(
                        { detail: "Acesso inválido." },
                        state.loginStatus,
                    );
                state.session = state.loginUser;
                return tokens(state.session);
            }
            if (path === "/api/auth/sair" || path === "/api/auth/senha") {
                state.session = null;
                if (path.endsWith("/senha")) {
                    state.changes++;
                    state.loginUser = {
                        ...state.loginUser,
                        trocarSenha: false,
                    };
                }
                return new Response(null, { status: 204 });
            }
            if (["/api/v1/clientes", "/api/v1/armazens"].includes(path)) {
                const type = path.endsWith("clientes")
                    ? "ClienteDto.Resposta"
                    : "ArmazemDto.Resposta";
                return json(
                    toWire(`PaginaResponse<${type}>`, {
                        itens: [fixture(type)],
                        pagina: 0,
                        tamanho: 100,
                        totalItens: 1,
                        totalPaginas: 1,
                    }),
                );
            }
            // O destino Início deve independer também da disponibilidade dos indicadores.
            return json({ detail: "Consulta fictícia indisponível." }, 503);
        }),
    );
    return { state, calls };
}

async function login() {
    await screen.findByRole("heading", { name: "Entrar no WMS" });
    fireEvent.change(screen.getByLabelText("E-mail"), {
        target: { value: user.email },
    });
    fireEvent.change(screen.getByLabelText("Senha"), {
        target: { value: "Credencial-ficticia-123!" },
    });
    fireEvent.click(screen.getByRole("button", { name: "Entrar" }));
}
async function home() {
    await waitFor(() =>
        expect(screen.getByRole("button", { name: "Início" })).toHaveAttribute(
            "aria-current",
            "page",
        ),
    );
    expect(location.hash).toBe("#inicio");
}
async function cadence() {
    const callback = vi
        .mocked(setInterval)
        .mock.calls.findLast(([, delay]) => delay === 240000)?.[0];
    expect(typeof callback).toBe("function");
    await act(async () => {
        if (typeof callback === "function") callback();
    });
}
afterEach(() => {
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    history.replaceState(null, "", "/");
});

describe("FE03-AUTH-INICIO01 — entrada por credenciais distinta de restauração", () => {
    it.each(["cadastros", "entrada", "usuarios"])(
        "novo login a partir de #%s entra no Início",
        async (route) => {
            server();
            history.replaceState(
                { marker: "preservado" },
                "",
                `/wms?origem=local#${route}`,
            );
            render(<AuthApp />);
            await login();
            await home();
            expect(history.state).toEqual({ marker: "preservado" });
            expect(location.pathname + location.search).toBe(
                "/wms?origem=local",
            );
        },
    );

    it("logout em outra rota seguido de novo login entra no Início", async () => {
        const proof = server(user);
        history.replaceState(null, "", "#cadastros");
        render(<AuthApp />);
        await screen.findByRole("heading", { name: "Cadastros" });
        fireEvent.click(screen.getByRole("button", { name: "Sair" }));
        await login();
        await home();
        expect(
            proof.calls.filter((path) => path === "/api/auth/sair"),
        ).toHaveLength(1);
        expect(
            proof.calls.filter((path) => path === "/api/auth/entrar"),
        ).toHaveLength(1);
    });

    it("falha de login preserva hash e tela de acesso", async () => {
        const proof = server();
        proof.state.loginStatus = 401;
        history.replaceState(null, "", "#entrada");
        render(<AuthApp />);
        await login();
        await screen.findByRole("alert");
        expect(location.hash).toBe("#entrada");
        expect(
            screen.getByRole("heading", { name: "Entrar no WMS" }),
        ).toBeInTheDocument();
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    });

    it("sessão restaurada, reload e renovação preservam rota operacional sem consultar dashboard", async () => {
        const proof = server(user);
        history.replaceState(null, "", "#cadastros");
        let view = render(<AuthApp />);
        await screen.findByRole("heading", { name: "Cadastros" });
        view.unmount();
        view = render(<AuthApp />);
        await screen.findByRole("heading", { name: "Cadastros" });
        await cadence();
        await waitFor(() => expect(proof.state.renewals).toBe(3));
        expect(location.hash).toBe("#cadastros");
        expect(
            screen.getByRole("heading", { name: "Cadastros" }),
        ).toBeInTheDocument();
        expect(
            proof.calls.every(
                (path) =>
                    path.startsWith("/api/auth/") ||
                    ["/api/v1/clientes", "/api/v1/armazens"].includes(path),
            ),
        ).toBe(true);
        expect(proof.calls).not.toContain("/api/auth/entrar");
        view.unmount();
    });

    it("navegação explícita após login e renovação preservam modal e rascunho de senha", async () => {
        const proof = server();
        history.replaceState(null, "", "#entrada");
        render(<AuthApp />);
        await login();
        await home();
        fireEvent.click(screen.getByRole("button", { name: "Cadastros" }));
        await screen.findByRole("heading", { name: "Cadastros" });
        fireEvent.click(screen.getByRole("button", { name: "Minha senha" }));
        const dialog = within(await screen.findByRole("dialog"));
        fireEvent.change(dialog.getByLabelText("Nova senha"), {
            target: { value: "Rascunho-ficticio-123!" },
        });
        await cadence();
        await waitFor(() => expect(proof.state.renewals).toBe(2));
        expect(location.hash).toBe("#cadastros");
        expect(dialog.getByLabelText("Nova senha")).toHaveValue(
            "Rascunho-ficticio-123!",
        );
        expect(proof.state.changes).toBe(0);
    });

    it("login com senha temporária, reload no gate, troca e nova entrada chegam ao Início", async () => {
        const proof = server();
        proof.state.loginUser = { ...user, trocarSenha: true };
        history.replaceState(null, "", "#cadastros");
        const first = render(<AuthApp />);
        await login();
        await screen.findByLabelText("Senha atual");
        expect(location.hash).toBe("#inicio");
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        first.unmount();
        render(<AuthApp />);
        await screen.findByLabelText("Senha atual");
        expect(location.hash).toBe("#inicio");
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
        fireEvent.change(screen.getByLabelText("Senha atual"), {
            target: { value: "Temporaria-ficticia-123!" },
        });
        fireEvent.change(screen.getByLabelText("Nova senha"), {
            target: { value: "Nova-ficticia-123!" },
        });
        fireEvent.change(screen.getByLabelText("Repita a nova senha"), {
            target: { value: "Nova-ficticia-123!" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: "Salvar nova senha" }),
        );
        await screen.findByText(
            "Senha alterada. Entre novamente com sua nova senha.",
        );
        expect(proof.state.changes).toBe(1);
        await login();
        await home();
    });

    it("reload de sessão que já exige senha preserva hash e bloqueia operações", async () => {
        server({ ...user, trocarSenha: true });
        history.replaceState(null, "", "#entrada");
        const first = render(<AuthApp />);
        await screen.findByLabelText("Senha atual");
        first.unmount();
        render(<AuthApp />);
        await screen.findByLabelText("Senha atual");
        expect(location.hash).toBe("#entrada");
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    });
});
