import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthClient, AuthError, type User } from "./client";
import { LoginPage, PasswordPage } from "./LoginPage";
import { UsersPage } from "./UsersPage";
import { realTransport, type Receipt, type Request } from "../api/client";
import { JourneyPage } from "../components/JourneyPage";
import { Collector } from "../components/Collector";
import { Operation } from "../components/Operation";
import { PageHeader } from "../components/layout/PageHeader";
import { HomeOverview } from "../components/layout/HomeOverview";
import { journeys } from "../domain/journeys";
import { absorb, emptyWorkflow, operationContext } from "../domain/workflow";
import type { Values } from "../contracts/runtime";
import { useNavigation } from "../hooks/useNavigation";
import { AppShell } from "../components/shell/AppShell";
import { AuthLayout } from "./AuthLayout";
import { Icon } from "../design-system/Icon";

export function AuthApp() {
    const [auth] = useState(() => new AuthClient());
    const [user, setUser] = useState<User | null>(null),
        [loading, setLoading] = useState(true);
    const [message, setMessage] = useState("");
    useEffect(() => {
        let active = true;
        auth.expired(() => {
            if (active) {
                setUser(null);
                setMessage("Sua sessão terminou. Entre novamente.");
            }
        });
        void auth
            .refresh()
            .then((u) => {
                if (active) setUser(u);
            })
            .catch((error: unknown) => {
                if (active)
                    setMessage(
                        error instanceof AuthError && error.status === 401
                            ? ""
                            : "Não foi possível conectar ao WMS. Confira se o sistema foi iniciado.",
                    );
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        const timer = setInterval(() => {
            if (auth.token())
                void auth
                    .refresh()
                    .then((u) => {
                        if (active) setUser(u);
                    })
                    .catch(() => {});
        }, 240000);
        return () => {
            active = false;
            clearInterval(timer);
        };
    }, [auth]);
    const logout = useCallback(async () => {
        setUser(null);
        setMessage("");
        try {
            await auth.logout();
        } catch {
            setMessage(
                "A saída não foi confirmada pelo servidor. Feche o navegador se estiver em um computador compartilhado.",
            );
        }
    }, [auth]);
    const changed = useCallback(() => {
        setUser(null);
        setMessage("Senha alterada. Entre novamente com sua nova senha.");
    }, []);
    if (loading)
        return (
            <AuthLayout>
                <section
                    className="auth-card auth-loading"
                    aria-label="Verificação de acesso"
                >
                    <span className="auth-card-icon">
                        <Icon name="acesso" />
                    </span>
                    <p role="status">Verificando seu acesso…</p>
                    <p className="muted">
                        Aguarde enquanto recuperamos sua sessão.
                    </p>
                </section>
            </AuthLayout>
        );
    if (!user)
        return (
            <LoginPage
                auth={auth}
                onLogin={(u) => {
                    setUser(u);
                    setMessage("");
                }}
                message={message}
            />
        );
    if (user.trocarSenha)
        return (
            <AuthLayout>
                <PasswordPage
                    auth={auth}
                    required
                    onChanged={changed}
                    onCancel={() => void logout()}
                />
            </AuthLayout>
        );
    return (
        <Workspace
            key={user.id}
            auth={auth}
            user={user}
            onLogout={logout}
            onChanged={changed}
        />
    );
}

function Workspace({
    auth,
    user,
    onLogout,
    onChanged,
}: {
    auth: AuthClient;
    user: User;
    onLogout: () => void;
    onChanged: () => void;
}) {
    const nav = useNavigation();
    const [context, setContext] = useState<Values>({}),
        [workflow, setWorkflow] = useState(emptyWorkflow);
    const [contextMessage, setContextMessage] = useState(""),
        [revision, setRevision] = useState(0);
    const transport = useMemo(() => {
        const real = realTransport({
            baseUrl: window.location.origin,
            allowReal: true,
            token: auth.token,
            onExpired: onLogout,
        });
        return {
            async send(request: Request) {
                await auth.fresh();
                return real.send(request);
            },
        };
    }, [auth, onLogout]);
    const onRecord = (v: Values, type: string) =>
        setWorkflow((old) => absorb(old, type, v, true));
    const onReceipt = (
        r: Receipt,
        type: string,
        _id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => setWorkflow((old) => absorb(old, type, r.data, false, request));
    const journey = journeys.find((j) => j.id === nav.page);
    return (
        <AppShell
            page={nav.page}
            navigate={nav.navigate}
            contentKey={`${revision}-${nav.revision}`}
            administrator={user.administrador}
            userName={user.nome}
            userRole={
                {
                    GESTOR: "Gestor",
                    SUPERVISOR: "Supervisor",
                    OPERACAO: "Operação",
                }[user.perfil]
            }
            onLogout={onLogout}
            context={
                <form
                    className="auth-context"
                    aria-label="Contexto operacional"
                    onSubmit={(e) => {
                        e.preventDefault();
                        const data = new FormData(e.currentTarget);
                        const clienteId = String(data.get("clienteId") ?? ""),
                            armazemId = String(data.get("armazemId") ?? "");
                        if (
                            [clienteId, armazemId].some(
                                (v) =>
                                    !/^[1-9]\d{0,18}$/.test(v) ||
                                    BigInt(v) > 9223372036854775807n,
                            )
                        ) {
                            setContextMessage(
                                "Informe os IDs do cliente e do armazém.",
                            );
                            return;
                        }
                        if (
                            user.perfil !== "GESTOR" &&
                            (!user.clientes.includes(clienteId) ||
                                !user.armazens.includes(armazemId))
                        ) {
                            setContextMessage(
                                "Seu usuário não tem acesso a esse cliente ou armazém.",
                            );
                            return;
                        }
                        setContext({ clienteId, armazemId });
                        setWorkflow(emptyWorkflow());
                        setRevision((v) => v + 1);
                        setContextMessage("Contexto selecionado.");
                    }}
                >
                    <label>
                        <span className="sr-only">Cliente (ID)</span>
                        <input
                            name="clienteId"
                            aria-label="Cliente (ID)"
                            inputMode="numeric"
                            required
                            placeholder="Cliente (ID)"
                        />
                    </label>
                    <label>
                        <span className="sr-only">Armazém (ID)</span>
                        <input
                            name="armazemId"
                            aria-label="Armazém (ID)"
                            inputMode="numeric"
                            required
                            placeholder="Armazém (ID)"
                        />
                    </label>
                    <button className="context-apply">Aplicar contexto</button>
                    <span
                        role="status"
                        className={
                            contextMessage === "Contexto selecionado."
                                ? "sr-only"
                                : "context-error"
                        }
                    >
                        {contextMessage}
                    </span>
                </form>
            }
        >
            {nav.page === "usuarios" && user.administrador ? (
                <UsersPage auth={auth} current={user} />
            ) : nav.page === "senha" ? (
                <PasswordPage
                    auth={auth}
                    required={false}
                    onChanged={onChanged}
                    onCancel={() => nav.navigate("inicio")}
                />
            ) : journey ? (
                <JourneyPage
                    journey={journey}
                    transport={transport}
                    context={context}
                    perfil={user.perfil}
                    workflow={workflow}
                    onRecord={onRecord}
                    onReceipt={onReceipt}
                    startAction={nav.startAction}
                    onNavigate={(n) => nav.navigate(n.page, n.action)}
                />
            ) : nav.page === "coletor" ? (
                <Collector
                    transport={transport}
                    context={operationContext(
                        "EstoqueController.posicionar",
                        context,
                        workflow,
                    )}
                    perfil={user.perfil}
                    onReceipt={onReceipt}
                    taskContexts={{
                        ler: operationContext(
                            "ExpedicaoController.ler",
                            context,
                            workflow,
                        ),
                        separar: operationContext(
                            "ExpedicaoController.separar",
                            context,
                            workflow,
                        ),
                        contar: operationContext(
                            "ContagemController.contar",
                            context,
                            workflow,
                        ),
                    }}
                />
            ) : (
                <>
                    <PageHeader
                        title={`Bem-vindo, ${user.nome}`}
                        icon="inicio"
                        description="Escolha um módulo para começar. Para operar o estoque, selecione o cliente e o armazém."
                        actions={
                            user.administrador && (
                                <button
                                    onClick={() => nav.navigate("usuarios")}
                                >
                                    Administrar usuários
                                </button>
                            )
                        }
                    />
                    <HomeOverview navigate={nav.navigate} />
                    <section
                        className="workspace-panel"
                        aria-label="Indicadores do contexto"
                    >
                        <Operation
                            id="IndicadorEstoqueController.listar"
                            transport={transport}
                            context={context}
                            perfil={user.perfil}
                            onSelect={() => nav.navigate("estoque")}
                        />
                    </section>
                </>
            )}
        </AppShell>
    );
}
