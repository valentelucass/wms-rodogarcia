import { useCallback, useEffect, useMemo, useState } from "react";
import { AuthClient, AuthError, type User } from "./client";
import { LoginPage, PasswordPage } from "./LoginPage";
import { UsersPage } from "./UsersPage";
import { realTransport, type Receipt, type Request } from "../api/client";
import { JourneyPage } from "../components/JourneyPage";
import { Collector } from "../components/Collector";
import { Operation } from "../components/Operation";
import { journeys } from "../domain/journeys";
import { absorb, emptyWorkflow, operationContext } from "../domain/workflow";
import type { Values } from "../contracts/runtime";
import { useNavigation } from "../hooks/useNavigation";

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
            <main className="auth-shell">
                <p role="status">Verificando seu acesso…</p>
            </main>
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
            <main className="auth-shell">
                <PasswordPage
                    auth={auth}
                    required
                    onChanged={changed}
                    onCancel={() => void logout()}
                />
            </main>
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
        <>
            <a className="skip" href="#conteudo">
                Ir para o conteúdo
            </a>
            <header>
                <strong>WMS Rodogarcia</strong>
                <span>{user.nome}</span>
                <button onClick={() => nav.navigate("senha")}>
                    Minha senha
                </button>
                <button onClick={onLogout}>Sair</button>
            </header>
            <form
                className="auth-context"
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
                    Cliente (ID)
                    <input name="clienteId" inputMode="numeric" required />
                </label>
                <label>
                    Armazém (ID)
                    <input name="armazemId" inputMode="numeric" required />
                </label>
                <button>Aplicar contexto</button>
                <span role="status">{contextMessage}</span>
            </form>
            <div className="layout">
                <nav className="main-nav" aria-label="Módulos">
                    {[
                        { id: "inicio", title: "Início" },
                        ...journeys,
                        { id: "coletor", title: "Coletor" },
                        ...(user.administrador
                            ? [{ id: "usuarios", title: "Usuários e acessos" }]
                            : []),
                    ].map((item) => (
                        <button
                            key={item.id}
                            aria-current={
                                nav.page === item.id ? "page" : undefined
                            }
                            onClick={() => nav.navigate(item.id)}
                        >
                            {item.title}
                        </button>
                    ))}
                </nav>
                <main
                    id="conteudo"
                    tabIndex={-1}
                    key={`${revision}-${nav.revision}`}
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
                            <h1>Bem-vindo, {user.nome}</h1>
                            <p>
                                Escolha um módulo para começar. Para operar o
                                estoque, selecione o cliente e o armazém.
                            </p>
                            {user.administrador && (
                                <button
                                    onClick={() => nav.navigate("usuarios")}
                                >
                                    Administrar usuários
                                </button>
                            )}
                            <Operation
                                id="IndicadorEstoqueController.listar"
                                transport={transport}
                                context={context}
                                perfil={user.perfil}
                                onSelect={() => nav.navigate("estoque")}
                            />
                        </>
                    )}
                </main>
            </div>
        </>
    );
}
