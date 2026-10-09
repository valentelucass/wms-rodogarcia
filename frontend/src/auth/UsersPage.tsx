import { useEffect, useRef, useState, type FormEvent } from "react";
import type { AuthClient, User, UsersPage as Page } from "./client";

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
    const [message, setMessage] = useState(""),
        [busy, setBusy] = useState(false);
    const [editing, setEditing] = useState<User | "new" | null>(null),
        [reset, setReset] = useState<User | null>(null);
    const sending = useRef(false);
    useEffect(() => {
        let active = true;
        void auth
            .users(page)
            .then((p) => {
                if (active) setResult(p);
            })
            .catch((e: unknown) => {
                if (active)
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Falha ao consultar usuários.",
                    );
            });
        return () => {
            active = false;
        };
    }, [auth, page, revision]);
    async function save(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();
        if (sending.current) return;
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
                    await auth.create({
                        ...body,
                        email: String(data.get("email")).trim(),
                        senhaTemporaria: data.get("senhaTemporaria"),
                    });
                else if (editing)
                    await auth.edit(editing.id, {
                        ...body,
                        ativo: data.get("ativo") === "on",
                        versao: editing.versao,
                    });
                setMessage(
                    editing === "new"
                        ? "Usuário criado. A senha temporária deverá ser trocada no primeiro acesso."
                        : "Usuário atualizado. Os acessos anteriores foram encerrados.",
                );
            }
            form.reset();
            setEditing(null);
            setReset(null);
            setRevision((v) => v + 1);
        } catch (e) {
            setError(
                e instanceof Error ? e.message : "Não foi possível salvar.",
            );
        } finally {
            sending.current = false;
            setBusy(false);
        }
    }
    const selected = editing && editing !== "new" ? editing : null;
    return (
        <section>
            <h1>Usuários e acessos</h1>
            <p>
                Cadastre pessoas, defina suas permissões e redefina senhas
                temporárias.
            </p>
            {message && <p role="status">{message}</p>}
            {error && (
                <p role="alert" className="error">
                    {error}
                </p>
            )}
            {!editing && !reset && (
                <button
                    onClick={() => {
                        setEditing("new");
                        setError("");
                    }}
                >
                    Criar usuário
                </button>
            )}
            {(editing || reset) && (
                <form
                    className="user-form"
                    onSubmit={(e) => void save(e)}
                    key={reset?.id ?? selected?.id ?? "new"}
                >
                    <h2>
                        {reset
                            ? `Redefinir senha de ${reset.nome}`
                            : selected
                              ? `Editar ${selected.nome}`
                              : "Novo usuário"}
                    </h2>
                    <fieldset disabled={busy}>
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
                                        <option value="GESTOR">Gestor</option>
                                    </select>
                                </label>
                                <p>
                                    Gestor tem acesso operacional a todos os
                                    clientes e armazéns. Os demais perfis usam
                                    os vínculos abaixo.
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
                                    vínculos vazios não libera nenhum cliente ou
                                    armazém.
                                </p>
                                <label className="check-label">
                                    <input
                                        name="administrador"
                                        type="checkbox"
                                        defaultChecked={selected?.administrador}
                                    />{" "}
                                    Administrador de usuários
                                </label>
                                <p>
                                    Permite criar e editar contas, conceder
                                    administração e redefinir senhas de outros
                                    usuários.
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
                                    De 12 a 128 caracteres. Entregue diretamente
                                    ao usuário; ele deverá trocá-la.
                                </small>
                            </label>
                        )}
                        <button type="submit">
                            {busy
                                ? "Salvando…"
                                : reset
                                  ? "Confirmar nova senha temporária"
                                  : "Salvar usuário"}
                        </button>
                        <button
                            type="button"
                            onClick={() => {
                                setEditing(null);
                                setReset(null);
                                setError("");
                            }}
                        >
                            Cancelar
                        </button>
                    </fieldset>
                </form>
            )}
            {!result ? (
                <p role="status">Carregando usuários…</p>
            ) : (
                <>
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
                                                    {u.id !== current.id && (
                                                        <button
                                                            disabled={
                                                                busy ||
                                                                !!editing ||
                                                                !!reset
                                                            }
                                                            onClick={() => {
                                                                setReset(u);
                                                                setError("");
                                                            }}
                                                        >
                                                            Redefinir senha
                                                        </button>
                                                    )}
                                                </>
                                            )}
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                    <div className="pagination">
                        <button
                            disabled={page === 0 || busy}
                            onClick={() => {
                                setResult(null);
                                setPage((p) => p - 1);
                            }}
                        >
                            Anterior
                        </button>
                        <span>
                            Página {page + 1} de{" "}
                            {Math.max(1, result.totalPages)}
                        </span>
                        <button
                            disabled={page + 1 >= result.totalPages || busy}
                            onClick={() => {
                                setResult(null);
                                setPage((p) => p + 1);
                            }}
                        >
                            Próxima
                        </button>
                    </div>
                </>
            )}
        </section>
    );
}
