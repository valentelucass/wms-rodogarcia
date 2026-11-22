import { useRef, useState, type FormEvent } from "react";
import { AuthError, type AuthClient, type User } from "./client";
import { Icon } from "../design-system/Icon";
import { AuthLayout } from "./AuthLayout";
import { Dialog } from "../components/layout/Dialog";

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
    const [showPassword, setShowPassword] = useState(false);
    const [helpTopic, setHelpTopic] = useState<"recovery" | "first">(
        "recovery",
    );
    const helpDialog = useRef<HTMLDialogElement>(null);
    function showHelp(topic: "recovery" | "first") {
        setHelpTopic(topic);
        helpDialog.current?.showModal();
    }
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
                e instanceof AuthError && e.status === 0
                    ? "Não foi possível conectar ao WMS. Verifique sua conexão e tente novamente."
                    : e instanceof AuthError && e.status >= 500
                      ? "Não foi possível entrar no momento. Aguarde e tente novamente."
                      : e instanceof AuthError && e.code === "AUTH_CONTRACT"
                        ? "Não foi possível confirmar seu acesso. Tente novamente ou fale com o suporte."
                        : e instanceof Error
                          ? e.message
                          : "Não foi possível entrar. Tente novamente ou fale com o suporte.",
            );
        } finally {
            const password = form.elements.namedItem("senha");
            if (password instanceof HTMLInputElement) password.value = "";
            setShowPassword(false);
            sending.current = false;
            setBusy(false);
        }
    }
    return (
        <AuthLayout shippingLabel>
            <section
                className="auth-card auth-login-card"
                aria-labelledby="login-title"
            >
                <div className="auth-card-heading">
                    <h1 id="login-title">Entrar no WMS</h1>
                </div>
                <div className="auth-message-slot">
                    <p
                        id="login-feedback"
                        role={error ? "alert" : message ? "status" : undefined}
                        className={`auth-feedback${error ? " error" : message ? "" : " auth-feedback--hint"}`}
                    >
                        {error || message || "Entre com sua conta corporativa."}
                    </p>
                </div>
                <form
                    aria-describedby="login-feedback"
                    onSubmit={(e) => void submit(e)}
                >
                    <div className="auth-field">
                        <label htmlFor="login-email">E-mail</label>
                        <div className="auth-input-wrap">
                            <input
                                id="login-email"
                                name="email"
                                type="email"
                                autoComplete="username"
                                autoCapitalize="none"
                                spellCheck={false}
                                placeholder="nome@empresa.com.br"
                                maxLength={254}
                                required
                                autoFocus
                                disabled={busy}
                            />
                        </div>
                    </div>
                    <div className="auth-field">
                        <label htmlFor="login-password">Senha</label>
                        <div className="auth-input-wrap auth-input-wrap--password">
                            <input
                                id="login-password"
                                name="senha"
                                type={showPassword ? "text" : "password"}
                                autoComplete="current-password"
                                placeholder="Digite sua senha"
                                maxLength={128}
                                required
                                disabled={busy}
                            />
                            <button
                                className="auth-password-toggle"
                                type="button"
                                aria-label={
                                    showPassword
                                        ? "Ocultar senha"
                                        : "Mostrar senha"
                                }
                                aria-controls="login-password"
                                title={
                                    showPassword
                                        ? "Ocultar senha"
                                        : "Mostrar senha"
                                }
                                disabled={busy}
                                onClick={() =>
                                    setShowPassword((value) => !value)
                                }
                            >
                                <Icon name={showPassword ? "eye-off" : "eye"} />
                            </button>
                        </div>
                    </div>
                    <button
                        className="primary auth-submit"
                        type="submit"
                        disabled={busy}
                    >
                        <span>{busy ? "Entrando…" : "Entrar"}</span>
                        <Icon name="arrow" />
                    </button>
                </form>
                <div className="auth-help">
                    <button
                        type="button"
                        aria-haspopup="dialog"
                        onClick={() => showHelp("recovery")}
                    >
                        Esqueceu sua senha?
                    </button>
                    <button
                        type="button"
                        aria-haspopup="dialog"
                        onClick={() => showHelp("first")}
                    >
                        É seu primeiro acesso?
                    </button>
                </div>
                <dialog
                    ref={helpDialog}
                    className="auth-help-dialog"
                    aria-labelledby="login-help-title"
                >
                    <div className="auth-help-dialog-heading">
                        <span className="auth-card-icon">
                            <Icon
                                name={
                                    helpTopic === "recovery"
                                        ? "senha"
                                        : "usuarios"
                                }
                            />
                        </span>
                        <h2 id="login-help-title">
                            {helpTopic === "recovery"
                                ? "Esqueceu sua senha?"
                                : "É seu primeiro acesso?"}
                        </h2>
                    </div>
                    <p>
                        {helpTopic === "recovery" ? (
                            <>
                                Peça a um administrador para definir uma senha
                                temporária. Ao entrar, você deverá criar uma
                                nova senha.
                            </>
                        ) : (
                            <>
                                Use o e-mail cadastrado e a senha temporária
                                recebida do administrador. Crie sua nova senha
                                para acessar as operações.
                            </>
                        )}
                    </p>
                    <button
                        type="button"
                        className="primary"
                        onClick={() => helpDialog.current?.close()}
                    >
                        Entendi
                    </button>
                </dialog>
            </section>
            <div className="shipping-stub" aria-hidden="true">
                <strong>RODOGARCIA</strong>
                <span className="shipping-stub-label">Via de acesso</span>
                <span className="shipping-barcode">
                    <svg
                        className="reader-frame"
                        viewBox="0 0 200 80"
                        preserveAspectRatio="none"
                        focusable="false"
                    >
                        <path d="M1 10V1h10m178 0h10v9m0 60v9h-10M11 79H1v-9" />
                    </svg>
                    <span className="reader-ink">
                        <i />
                        <i />
                        <i />
                        <i />
                    </span>
                    <span className="reader-head" />
                </span>
                <span className="shipping-serial">RG / 000 026</span>
            </div>
        </AuthLayout>
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
    const content = (
        <>
            <p className="password-notice">
                {required
                    ? "Sua senha é temporária. Escolha uma nova para continuar no WMS."
                    : "Ao confirmar, seus acessos atuais serão encerrados. Entre novamente com a nova senha."}
            </p>
            <p className="password-guidance">
                Use de 12 a 128 caracteres e uma senha diferente da atual. Você
                pode usar uma frase longa e um gerenciador de senhas.
            </p>
            {error && (
                <p role="alert" className="error">
                    {error}
                </p>
            )}
            <form className="password-form" onSubmit={(e) => void submit(e)}>
                <fieldset disabled={busy}>
                    <label className="password-current">
                        Senha atual
                        <input
                            name="senhaAtual"
                            type="password"
                            autoComplete="current-password"
                            data-dialog-autofocus
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
                    <div className="actions">
                        <button type="button" onClick={onCancel}>
                            {required ? "Sair" : "Cancelar"}
                        </button>
                        <button type="submit" className="primary">
                            {busy ? "Salvando…" : "Salvar nova senha"}
                        </button>
                    </div>
                </fieldset>
            </form>
        </>
    );
    return required ? (
        <section className="auth-card" aria-labelledby="password-title">
            <h1 id="password-title">Crie sua nova senha</h1>
            {content}
        </section>
    ) : (
        <Dialog
            title="Alterar minha senha"
            account
            icon="senha"
            locked={busy}
            onClose={onCancel}
        >
            {content}
        </Dialog>
    );
}
