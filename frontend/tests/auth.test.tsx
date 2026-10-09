import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { AuthClient, type User } from "../src/auth/client";
import { LoginPage, PasswordPage } from "../src/auth/LoginPage";
import { UsersPage } from "../src/auth/UsersPage";

const user: User = {
    id: "usuario-fixture",
    nome: "Pessoa",
    email: "pessoa@test.invalid",
    perfil: "OPERACAO",
    administrador: true,
    principal: false,
    ativo: true,
    trocarSenha: false,
    clientes: [],
    armazens: [],
    versao: 0,
};
const csrf = () =>
    new Response(
        JSON.stringify({ token: "csrf-fixture", header: "X-XSRF-TOKEN" }),
        { status: 200 },
    );
const tokens = () =>
    new Response(
        JSON.stringify({
            accessToken: "jwt-fixture",
            expiresIn: 300,
            usuario: user,
        }),
        { status: 200 },
    );
afterEach(() => {
    vi.restoreAllMocks();
    localStorage.clear();
    sessionStorage.clear();
});

describe("login real", () => {
    it.each([
        ["Fixture-atual-123!", "A nova senha precisa ser diferente da senha atual."],
        ["curta", "A nova senha precisa ter de 12 a 128 caracteres."],
    ])("explica senha invalida antes de enviar: %s", async (next, message) => {
        const auth = new AuthClient();
        const change = vi.spyOn(auth, "changePassword");
        render(<PasswordPage auth={auth} required onChanged={vi.fn()} onCancel={vi.fn()} />);
        fireEvent.change(screen.getByLabelText("Senha atual"), { target: { value: "Fixture-atual-123!" } });
        fireEvent.change(screen.getByLabelText("Nova senha"), { target: { value: next } });
        fireEvent.change(screen.getByLabelText("Repita a nova senha"), { target: { value: next } });
        fireEvent.submit(screen.getByRole("button", { name: "Salvar nova senha" }).closest("form")!);
        expect(await screen.findByRole("alert")).toHaveTextContent(message);
        expect(change).not.toHaveBeenCalled();
    });
    it("envia credenciais com CSRF, guarda JWT so em memoria e nao repete escrita", async () => {
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(tokens());
        const auth = new AuthClient(fetcher);
        await auth.login(user.email, "senha-fixture-123");
        expect(auth.token()).toBe("jwt-fixture");
        expect(fetcher.mock.calls[1][1]).toMatchObject({
            credentials: "same-origin",
            cache: "no-store",
            headers: { "X-XSRF-TOKEN": "csrf-fixture" },
        });
        expect(localStorage.length).toBe(0);
        expect(sessionStorage.length).toBe(0);
        fetcher
            .mockResolvedValueOnce(csrf())
            .mockResolvedValueOnce(new Response("{}", { status: 500 }));
        await expect(auth.create({ nome: "Teste" })).rejects.toMatchObject({
            status: 500,
        });
        expect(
            fetcher.mock.calls.filter(
                ([path]) => path === "/api/auth/usuarios",
            ),
        ).toHaveLength(1);
    });
    it("nao ressuscita JWT quando refresh atrasado termina depois de sair", async () => {
        let finish: ((r: Response) => void) | undefined;
        const fetcher = vi
            .fn<typeof fetch>()
            .mockResolvedValueOnce(csrf())
            .mockImplementationOnce(
                () =>
                    new Promise((resolve) => {
                        finish = resolve;
                    }),
            );
        const auth = new AuthClient(fetcher);
        const pending = auth.refresh();
        await waitFor(() => expect(finish).toBeDefined());
        auth.clear();
        finish!(tokens());
        await expect(pending).rejects.toMatchObject({ status: 401 });
        expect(auth.token()).toBeNull();
    });
    it("primeiro acesso pede confirmacao da nova senha e nao mostra operacao", async () => {
        const auth = new AuthClient();
        const change = vi.spyOn(auth, "changePassword").mockResolvedValue();
        const onChanged = vi.fn();
        render(
            <PasswordPage
                auth={auth}
                required
                onChanged={onChanged}
                onCancel={vi.fn()}
            />,
        );
        fireEvent.change(screen.getByLabelText("Senha atual"), {
            target: { value: "temporaria-fixture" },
        });
        fireEvent.change(screen.getByLabelText("Nova senha"), {
            target: { value: "nova-senha-fixture" },
        });
        fireEvent.change(screen.getByLabelText("Repita a nova senha"), {
            target: { value: "outra-senha-fixture" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: "Salvar nova senha" }),
        );
        expect(screen.getByRole("alert")).toHaveTextContent(
            "precisam ser iguais",
        );
        expect(change).not.toHaveBeenCalled();
        fireEvent.change(screen.getByLabelText("Repita a nova senha"), {
            target: { value: "nova-senha-fixture" },
        });
        fireEvent.click(
            screen.getByRole("button", { name: "Salvar nova senha" }),
        );
        await waitFor(() => expect(onChanged).toHaveBeenCalledOnce());
        expect(screen.queryByRole("navigation")).not.toBeInTheDocument();
    });
    it("login limpa senha apos recusa e permite tentar novamente", async () => {
        const auth = new AuthClient();
        vi.spyOn(auth, "login").mockRejectedValue(
            new Error("Acesso inválido."),
        );
        render(<LoginPage auth={auth} onLogin={vi.fn()} />);
        fireEvent.change(screen.getByLabelText("E-mail"), {
            target: { value: user.email },
        });
        fireEvent.change(screen.getByLabelText("Senha"), {
            target: { value: "senha-fixture" },
        });
        fireEvent.click(screen.getByRole("button", { name: "Entrar" }));
        await screen.findByRole("alert");
        expect(screen.getByLabelText("Senha")).toHaveValue("");
        expect(screen.getByRole("button", { name: "Entrar" })).toBeEnabled();
    });
    it("painel permite delegar administracao e protege a conta principal", async () => {
        const auth = new AuthClient();
        vi.spyOn(auth, "users").mockResolvedValue({
            content: [{ ...user, principal: true }],
            number: 0,
            totalPages: 1,
            totalElements: 1,
        });
        const create = vi
            .spyOn(auth, "create")
            .mockResolvedValue({ ...user, trocarSenha: true });
        render(<UsersPage auth={auth} current={user} />);
        await screen.findByText("Conta protegida");
        expect(
            screen.queryByRole("button", { name: "Redefinir senha" }),
        ).not.toBeInTheDocument();
        fireEvent.click(screen.getByRole("button", { name: "Criar usuário" }));
        fireEvent.change(screen.getByLabelText("Nome"), {
            target: { value: "Outro administrador" },
        });
        fireEvent.change(screen.getByLabelText("E-mail"), {
            target: { value: "outro@test.invalid" },
        });
        fireEvent.change(screen.getByLabelText(/Senha temporária/), {
            target: { value: "temporaria-fixture-123" },
        });
        fireEvent.click(screen.getByLabelText("Administrador de usuários"));
        fireEvent.click(screen.getByRole("button", { name: "Salvar usuário" }));
        await waitFor(() =>
            expect(create).toHaveBeenCalledWith(
                expect.objectContaining({
                    administrador: true,
                    perfil: "OPERACAO",
                }),
            ),
        );
        expect(await screen.findByRole("status")).toHaveTextContent(
            "primeiro acesso",
        );
    });
});
