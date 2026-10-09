import { useRef, useState, type FormEvent } from "react";
import { AuthError, type AuthClient, type User } from "./client";
import { Icon } from "../design-system/Icon";
import { AuthLayout } from "./AuthLayout";

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
        <AuthLayout wide>
            <section className="auth-intro" aria-labelledby="intro-title">
                <p className="auth-eyebrow">
                    <span aria-hidden="true" /> GESTÃO DE ARMAZÉM
                </p>
                <h2 id="intro-title">
                    Sua operação,
                    <br />
                    <span>em um só lugar.</span>
                </h2>
                <p className="auth-intro-description">
                    Do recebimento à expedição, acompanhe cada etapa com
                    organização, rastreabilidade e controle.
                </p>
                <ol
                    className="auth-flow"
                    aria-label="Etapas da operação no WMS"
                >
                    <li>
                        <span className="auth-flow-icon">
                            <Icon name="entrada" />
                        </span>
                        <div>
                            <strong>Recebimento</strong>
                            <span>
                                Conferência e identificação da mercadoria.
                            </span>
                        </div>
                        <span className="auth-flow-number" aria-hidden="true">
                            01
                        </span>
                    </li>
                    <li>
                        <span className="auth-flow-icon">
                            <Icon name="estoque" />
                        </span>
                        <div>
                            <strong>Armazenagem</strong>
                            <span>
                                Localização, estoque e reservas organizados.
                            </span>
                        </div>
                        <span className="auth-flow-number" aria-hidden="true">
                            02
                        </span>
                    </li>
                    <li>
                        <span className="auth-flow-icon">
                            <Icon name="saida" />
                        </span>
                        <div>
                            <strong>Expedição</strong>
                            <span>
                                Separação e saída com histórico preservado.
                            </span>
                        </div>
                        <span className="auth-flow-number" aria-hidden="true">
                            03
                        </span>
                    </li>
                </ol>
                <p className="auth-profile-note">
                    <Icon name="usuarios" />
                    As funções disponíveis seguem seu perfil de acesso.
                </p>
            </section>
            <section
                className="auth-card auth-login-card"
                aria-labelledby="login-title"
            >
                <div className="auth-card-heading">
                    <span className="auth-card-icon">
                        <Icon name="senha" />
                    </span>
                    <p className="auth-eyebrow">ACESSO AO SISTEMA</p>
                    <h1 id="login-title">Entrar no WMS</h1>
                </div>
                <div className="auth-message-slot">
                    <p
                        id="login-feedback"
                        role={error ? "alert" : message ? "status" : undefined}
                        className={`auth-feedback${error ? " error" : message ? "" : " auth-feedback--hint"}`}
                    >
                        {error ||
                            message ||
                            "Bem-vindo de volta. Use seu e-mail e sua senha para continuar."}
                    </p>
                </div>
                <form
                    aria-describedby="login-feedback"
                    onSubmit={(e) => void submit(e)}
                >
                    <div className="auth-field">
                        <label htmlFor="login-email">E-mail</label>
                        <div className="auth-input-wrap">
                            <Icon name="mail" />
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
                            <Icon name="senha" />
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
