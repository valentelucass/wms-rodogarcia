import { useEffect, useRef, useState, type FormEvent } from "react";
import type { AuthClient, User, UsersPage as Page } from "./client";
import { AuthError } from "./client";
import { PageHeader } from "../components/layout/PageHeader";
import { Dialog } from "../components/layout/Dialog";
import { Pagination } from "../components/layout/Pagination";

export function UsersPage({
    auth,
    current,
}: {
    auth: AuthClient;
    current: User;
}) {
    const [page, setPage] = useState(0),
        [revision, setRevision] = useState(0);
    const [result, setResult] = useState<Page | null>(null),
        [error, setError] = useState("");
    const [loading, setLoading] = useState(true),
        [queryError, setQueryError] = useState("");
    const [needsCheck, setNeedsCheck] = useState(false);
    const [message, setMessage] = useState(""),
        [busy, setBusy] = useState(false);
    const [editing, setEditing] = useState<User | "new" | null>(null),
        [reset, setReset] = useState<User | null>(null);
    const sending = useRef(false);
    const mutation = useRef<AbortController | null>(null);
    useEffect(() => () => mutation.current?.abort(), [auth]);
    useEffect(() => {
        let active = true;
        const controller = new AbortController();
        setLoading(true);
        setQueryError("");
        setResult(null);
        void auth
            .users(page, controller.signal)
            .then((p) => {
                if (active) {
                    setResult(p);
                    setNeedsCheck(false);
                    setEditing(null);
                    setReset(null);
                    setError("");
                }
            })
            .catch((e: unknown) => {
                if (active)
                    setQueryError(
                        e instanceof AuthError && e.status === 401
                            ? "Sua sessão terminou. Entre novamente."
                            : e instanceof AuthError && e.status === 403
                              ? "Você não tem permissão para consultar usuários."
                              : e instanceof Error
                                ? e.message
                                : "Falha ao consultar usuários.",
                    );
            })
            .finally(() => {
                if (active) setLoading(false);
            });
        return () => {
            active = false;
            controller.abort();
        };
    }, [auth, page, revision]);
    async function save(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (sending.current || needsCheck) return;
        const controller = new AbortController();
        mutation.current = controller;
        sending.current = true;
        setBusy(true);
        setError("");
        setMessage("");
        const form = event.currentTarget,
            data = new FormData(form);
        const scopes = (name: string) => {
            const value = String(data.get(name) ?? "").trim();
            const ids = value ? value.split(/[,\s]+/) : [];
            if (
                ids.length > 500 ||
                ids.some(
                    (id) =>
                        !/^[1-9]\d{0,18}$/.test(id) ||
                        BigInt(id) > 9223372036854775807n,
                )
            )
                throw new Error(
                    "Informe IDs positivos de clientes e armazéns, separados por vírgulas.",
                );
            return [...new Set(ids)];
        };
        try {
            if (reset) {
                await auth.reset(
                    reset.id,
                    String(data.get("senhaTemporaria")),
                    reset.versao,
                    controller.signal,
                );
                setMessage(
                    "Senha temporária redefinida. Os acessos anteriores foram encerrados; a próxima entrada exigirá uma nova senha.",
                );
            } else {
                const body = {
                    nome: data.get("nome"),
                    perfil: data.get("perfil"),
                    administrador: data.get("administrador") === "on",
                    clientes: scopes("clientes"),
                    armazens: scopes("armazens"),
                };
                if (editing === "new")
                    await auth.create(
                        {
                            ...body,
                            email: String(data.get("email")).trim(),
                            senhaTemporaria: data.get("senhaTemporaria"),
                        },
                        controller.signal,
                    );
                else if (editing)
                    await auth.edit(
                        editing.id,
                        {
                            ...body,
                            ativo: data.get("ativo") === "on",
                            versao: editing.versao,
                        },
                        controller.signal,
                    );
                setMessage(
                    editing === "new"
                        ? "Usuário criado. A senha temporária deverá ser trocada no primeiro acesso."
                        : "Usuário atualizado. Os acessos anteriores foram encerrados.",
                );
            }
            if (controller.signal.aborted) return;
            form.reset();
            setEditing(null);
            setReset(null);
            setRevision((v) => v + 1);
        } catch (e) {
            if (controller.signal.aborted) return;
            if (e instanceof AuthError && e.uncertain) setNeedsCheck(true);
            setError(
                e instanceof Error ? e.message : "Não foi possível salvar.",
            );
        } finally {
            if (mutation.current === controller) mutation.current = null;
            sending.current = false;
            setBusy(false);
        }
    }
    const selected = editing && editing !== "new" ? editing : null;
    const closeEditor = () => {
        if (busy || needsCheck) return;
        setEditing(null);
        setReset(null);
        setError("");
    };
    const editorTitle = reset
        ? `Redefinir senha de ${reset.nome}`
        : selected
          ? `Editar ${selected.nome}`
          : "Novo usuário";
    return (
        <section>
            <PageHeader
                title="Usuários e acessos"
                icon="usuarios"
                description="Cadastre pessoas, defina suas permissões e redefina senhas temporárias."
                actions={
                    <button
                        type="button"
                        className="primary"
                        disabled={
                            loading ||
                            busy ||
                            needsCheck ||
                            !!editing ||
                            !!reset
                        }
                        onClick={() => {
                            setEditing("new");
                            setError("");
                        }}
                    >
                        Criar usuário
                    </button>
                }
            />
            {message && <p role="status">{message}</p>}
            {error && !editing && !reset && (
                <p role="alert" className="error">
                    {error}
                </p>
            )}
            {queryError && !editing && !reset && (
                <p role="alert" className="error">
                    {queryError}
                </p>
            )}
            {(queryError || needsCheck) && !editing && !reset && (
                <button
                    type="button"
                    disabled={loading || busy}
                    onClick={() => setRevision((v) => v + 1)}
                >
                    Consultar novamente
                </button>
            )}
            {(editing || reset) && (
                <Dialog
                    title={editorTitle}
                    wide
                    onClose={closeEditor}
                    locked={busy || needsCheck}
                >
                    {error && (
                        <p role="alert" className="error">
                            {error}
                        </p>
                    )}
                    {queryError && needsCheck && (
                        <p role="alert">{queryError}</p>
                    )}
                    {needsCheck && (
                        <button
                            type="button"
                            disabled={loading || busy}
                            onClick={() => setRevision((v) => v + 1)}
                        >
                            Consultar novamente
                        </button>
                    )}
                    <form
                        className="user-form"
                        onSubmit={(e) => void save(e)}
                        key={reset?.id ?? selected?.id ?? "new"}
                    >
                        <fieldset disabled={busy || needsCheck}>
                            {!reset && (
                                <>
                                    <label>
                                        Nome
                                        <input
                                            name="nome"
                                            required
                                            maxLength={200}
                                            defaultValue={selected?.nome}
                                            autoFocus
                                        />
                                    </label>
                                    {editing === "new" && (
                                        <label>
                                            E-mail
                                            <input
                                                name="email"
                                                type="email"
                                                required
                                                maxLength={254}
                                                autoComplete="off"
                                            />
                                        </label>
                                    )}
                                    <label>
                                        Perfil operacional
                                        <select
                                            name="perfil"
                                            defaultValue={
                                                selected?.perfil ?? "OPERACAO"
                                            }
                                        >
                                            <option value="OPERACAO">
                                                Operação
                                            </option>
                                            <option value="SUPERVISOR">
                                                Supervisor
                                            </option>
                                            <option value="GESTOR">
                                                Gestor
                                            </option>
                                        </select>
                                    </label>
                                    <p>
                                        Gestor tem acesso operacional a todos os
                                        clientes e armazéns. Os demais perfis
                                        usam os vínculos abaixo.
                                    </p>
                                    <label>
                                        Clientes permitidos (IDs separados por
                                        vírgulas)
                                        <input
                                            name="clientes"
                                            defaultValue={selected?.clientes.join(
                                                ", ",
                                            )}
                                            placeholder="Ex.: 1, 2"
                                        />
                                    </label>
                                    <label>
                                        Armazéns permitidos (IDs separados por
                                        vírgulas)
                                        <input
                                            name="armazens"
                                            defaultValue={selected?.armazens.join(
                                                ", ",
                                            )}
                                            placeholder="Ex.: 1"
                                        />
                                    </label>
                                    <p>
                                        Para Operação e Supervisor, deixar os
                                        vínculos vazios não libera nenhum
                                        cliente ou armazém.
                                    </p>
                                    <label className="check-label">
                                        <input
                                            name="administrador"
                                            type="checkbox"
                                            defaultChecked={
                                                selected?.administrador
                                            }
                                        />{" "}
                                        Administrador de usuários
                                    </label>
                                    <p>
                                        Permite criar e editar contas, conceder
                                        administração e redefinir senhas de
                                        outros usuários.
                                    </p>
                                    {selected && (
                                        <label className="check-label">
                                            <input
                                                name="ativo"
                                                type="checkbox"
                                                defaultChecked={selected.ativo}
                                            />{" "}
                                            Acesso ativo
                                        </label>
                                    )}
                                </>
                            )}
                            {(reset || editing === "new") && (
                                <label>
                                    Senha temporária
                                    <input
                                        name="senhaTemporaria"
                                        type="password"
                                        autoComplete="new-password"
                                        minLength={12}
                                        maxLength={128}
                                        required
                                    />
                                    <small>
                                        De 12 a 128 caracteres. Entregue
                                        diretamente ao usuário; ele deverá
                                        trocá-la.
                                    </small>
                                </label>
                            )}
                            <div className="actions">
                                <button type="button" onClick={closeEditor}>
                                    Cancelar
                                </button>
                                <button type="submit" className="primary">
                                    {busy
                                        ? "Salvando…"
                                        : reset
                                          ? "Confirmar nova senha temporária"
                                          : "Salvar usuário"}
                                </button>
                            </div>
                        </fieldset>
                    </form>
                </Dialog>
            )}
            {loading ? (
                <p role="status">Carregando usuários…</p>
            ) : result ? (
                <section
                    className="workspace-panel users-list"
                    aria-label="Lista de usuários"
                >
                    {result.content.length === 0 && (
                        <p role="status">Nenhum usuário encontrado.</p>
                    )}
                    <div className="table-scroll">
                        <table>
                            <caption>Contas cadastradas</caption>
                            <thead>
                                <tr>
                                    <th>Usuário</th>
                                    <th>Perfil</th>
                                    <th>Permissões</th>
                                    <th>Situação</th>
                                    <th>Ações</th>
                                </tr>
                            </thead>
                            <tbody>
                                {result.content.map((u) => (
                                    <tr key={u.id}>
                                        <td>
                                            {u.nome}
                                            <br />
                                            <small>{u.email}</small>
                                        </td>
                                        <td>{u.perfil}</td>
                                        <td>
                                            {u.principal
                                                ? "Administrador principal"
                                                : u.administrador
                                                  ? "Administrador de usuários"
                                                  : "Usuário"}
                                        </td>
                                        <td>
                                            {!u.ativo
                                                ? "Desativado"
                                                : u.trocarSenha
                                                  ? "Troca de senha pendente"
                                                  : "Ativo"}
                                        </td>
                                        <td>
                                            <div className="row-actions">
                                                {u.principal ? (
                                                    "Conta protegida"
                                                ) : (
                                                    <>
                                                        <button
                                                            disabled={
                                                                busy ||
                                                                !!editing ||
                                                                !!reset
                                                            }
                                                            onClick={() => {
                                                                setEditing(u);
                                                                setError("");
                                                            }}
                                                        >
                                                            Editar
                                                        </button>
                                                        {u.id !==
                                                            current.id && (
                                                            <button
                                                                disabled={
                                                                    busy ||
                                                                    !!editing ||
                                                                    !!reset
                                                                }
                                                                onClick={() => {
                                                                    setReset(u);
                                                                    setError(
                                                                        "",
                                                                    );
                                                                }}
                                                            >
                                                                Redefinir senha
                                                            </button>
                                                        )}
                                                    </>
                                                )}
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <Pagination
                        page={page}
                        pages={result.totalPages}
                        total={result.totalElements}
                        count={result.content.length}
                        disabled={
                            loading ||
                            busy ||
                            !!editing ||
                            !!reset ||
                            needsCheck
                        }
                        onPage={(next) => {
                            setResult(null);
                            setPage(next);
                        }}
                    />
                </section>
            ) : null}
        </section>
    );
}
