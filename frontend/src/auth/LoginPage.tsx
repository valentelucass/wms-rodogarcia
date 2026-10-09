import { useRef, useState, type FormEvent } from "react";
import type { AuthClient, User } from "./client";
import { ThemeSelector } from "../design-system/ThemeSelector";

export function LoginPage({
    auth,
    onLogin,
    message = "",
}: {
    auth: AuthClient;
    onLogin: (user: User) => void;
    message?: string;
}) {
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const sending = useRef(false);
    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (sending.current) return;
        sending.current = true;
        setBusy(true);
        setError("");
        const form = event.currentTarget;
        const data = new FormData(form);
        try {
            onLogin(
                await auth.login(
                    String(data.get("email")).trim(),
                    String(data.get("senha")),
                ),
            );
        } catch (e) {
            setError(
                e instanceof Error ? e.message : "Não foi possível entrar.",
            );
        } finally {
            const password = form.elements.namedItem("senha");
            if (password instanceof HTMLInputElement) password.value = "";
            sending.current = false;
            setBusy(false);
        }
    }
    return (
        <main className="auth-shell">
            <div className="auth-theme">
                <ThemeSelector />
            </div>
            <section className="auth-card" aria-labelledby="login-title">
                <p className="auth-brand">WMS · RODOGARCIA</p>
                <h1 id="login-title">Entrar no WMS</h1>
                <p>Use seu e-mail e sua senha para acessar o sistema.</p>
                {message && <p role="status">{message}</p>}
                {error && (
                    <p role="alert" className="error">
                        {error}
                    </p>
                )}
                <form onSubmit={(e) => void submit(e)}>
                    <label>
                        E-mail
                        <input
                            name="email"
                            type="email"
                            autoComplete="username"
                            maxLength={254}
                            required
                            autoFocus
                            disabled={busy}
                        />
                    </label>
                    <label>
                        Senha
                        <input
                            name="senha"
                            type="password"
                            autoComplete="current-password"
                            maxLength={128}
                            required
                            disabled={busy}
                        />
                    </label>
                    <button className="primary" type="submit" disabled={busy}>
                        {busy ? "Entrando…" : "Entrar"}
                    </button>
                </form>
                <p className="muted">
                    Esqueceu sua senha? Peça a um administrador para definir uma
                    senha temporária.
                </p>
            </section>
        </main>
    );
}

export function PasswordPage({
    auth,
    required,
    onChanged,
    onCancel,
}: {
    auth: AuthClient;
    required: boolean;
    onChanged: () => void;
    onCancel: () => void;
}) {
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const sending = useRef(false);
    async function submit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (sending.current) return;
        const form = event.currentTarget;
        const data = new FormData(form);
        const next = String(data.get("novaSenha"));
        const current = String(data.get("senhaAtual"));
        if (next.length < 12 || next.length > 128 || !next.trim()) {
            setError("A nova senha precisa ter de 12 a 128 caracteres.");
            return;
        }
        if (next === current) {
            setError("A nova senha precisa ser diferente da senha atual.");
            return;
        }
        if (next !== data.get("confirmacao")) {
            setError("As novas senhas precisam ser iguais.");
            return;
        }
        sending.current = true;
        setBusy(true);
        setError("");
        try {
            await auth.changePassword(current, next);
            form.reset();
            onChanged();
        } catch (e) {
            setError(
                e instanceof Error
                    ? e.message
                    : "Não foi possível alterar a senha.",
            );
        } finally {
            sending.current = false;
            setBusy(false);
        }
    }
    return (
        <section className="auth-card" aria-labelledby="password-title">
            <h1 id="password-title">
                {required ? "Crie sua nova senha" : "Alterar minha senha"}
            </h1>
            <p>
                {required
                    ? "Sua senha é temporária. Escolha uma nova para continuar no WMS."
                    : "Ao confirmar, seus acessos atuais serão encerrados. Entre novamente com a nova senha."}
            </p>
            <p>
                Use de 12 a 128 caracteres e uma senha diferente da atual. Você
                pode usar uma frase longa e um gerenciador de senhas.
            </p>
            {error && (
                <p role="alert" className="error">
                    {error}
                </p>
            )}
            <form onSubmit={(e) => void submit(e)}>
                <fieldset disabled={busy}>
                    <label>
                        Senha atual
                        <input
                            name="senhaAtual"
                            type="password"
                            autoComplete="current-password"
                            maxLength={128}
                            required
                            autoFocus
                        />
                    </label>
                    <label>
                        Nova senha
                        <input
                            name="novaSenha"
                            type="password"
                            autoComplete="new-password"
                            minLength={12}
                            maxLength={128}
                            required
                        />
                    </label>
                    <label>
                        Repita a nova senha
                        <input
                            name="confirmacao"
                            type="password"
                            autoComplete="new-password"
                            minLength={12}
                            maxLength={128}
                            required
                        />
                    </label>
                    <button type="submit">
                        {busy ? "Salvando…" : "Salvar nova senha"}
                    </button>
                    <button type="button" onClick={onCancel}>
                        {required ? "Sair" : "Cancelar"}
                    </button>
                </fieldset>
            </form>
        </section>
    );
}
