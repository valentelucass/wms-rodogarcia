import { canLeavePage } from "../domain/pageLeave";
import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthClient, AuthError, type User } from "./client";
import { LoginPage, PasswordPage } from "./LoginPage";
import { UsersPage } from "./UsersPage";
import { realTransport, type Receipt, type Request } from "../api/client";
import { JourneyPage } from "../components/JourneyPage";
import { Collector } from "../components/Collector";
import { PageHeader } from "../components/layout/PageHeader";
import { HomeOverview } from "../components/layout/HomeOverview";
import { WarehouseOverview } from "../components/dashboard/WarehouseOverview";
import { ReferenceCatalogProvider } from "../components/context/ReferenceCatalog";
import { ContextPicker } from "../components/context/ContextPicker";
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
    const [passwordOpen, setPasswordOpen] = useState(false);
    const page = nav.page === "senha" ? "inicio" : nav.page;
    const [context, setContext] = useState<Values>({}),
        [workflow, setWorkflow] = useState(emptyWorkflow);
    const [revision, setRevision] = useState(0);
    const [contextRevision, setContextRevision] = useState(0);
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
    ) => {
        setWorkflow((old) => absorb(old, type, r.data, false, request));
        if (request.endpoint.method !== "GET") setRevision((v) => v + 1);
    };
    const applyContext = (v: Values) => {
        if (!canLeavePage()) return;
        setContext(v);
        setWorkflow(emptyWorkflow());
        setRevision((n) => n + 1);
        setContextRevision((n) => n + 1);
    };
    const journey = journeys.find((j) => j.id === page);
    const accessScope = `${revision}/${user.perfil}/${user.clientes.join(",")}/${user.armazens.join(",")}`;
    return (
        <ReferenceCatalogProvider transport={transport} version={accessScope}>
            <AppShell
                page={page}
                navigate={nav.navigate}
                contentKey={`${contextRevision}-${nav.revision}`}
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
                onPassword={() => setPasswordOpen(true)}
                context={
                    <ContextPicker context={context} onApply={applyContext} />
                }
            >
                {page === "usuarios" && user.administrador ? (
                    <UsersPage auth={auth} current={user} />
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
                ) : page === "coletor" ? (
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
                            title="Início"
                            icon="inicio"
                            description="Visão geral da operação"
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
                        <WarehouseOverview
                            key={accessScope}
                            transport={transport}
                            context={context}
                            onScope={applyContext}
                            onAddress={(p) => {
                                applyContext({
                                    ...context,
                                    armazemId: p.armazemId,
                                    enderecoId: p.id,
                                    enderecoCodigo: p.codigo,
                                });
                                nav.navigate("coletor");
                            }}
                        />
                    </>
                )}
                {(passwordOpen || nav.page === "senha") && (
                    <PasswordPage
                        auth={auth}
                        required={false}
                        onChanged={onChanged}
                        onCancel={() => {
                            setPasswordOpen(false);
                            if (nav.page === "senha") nav.navigate("inicio");
                        }}
                    />
                )}
            </AppShell>
        </ReferenceCatalogProvider>
    );
}
