import { useEffect, useState } from "react";
import { call, type Transport } from "../../api/client";
import { isObject, type Perfil, type Values } from "../../contracts/runtime";
import { recordIdentity, recordRoot } from "../../domain/recordContext";
import { recordActionLabel, visibleRecordActions } from "../../domain/recordPages";
import { useReferenceCatalog } from "../../components/context/ReferenceCatalog";
import { receivingDate, receivingNotes, receivingState, firstArrival, situationLabel } from "./orderPresentation";

const normalize = (value: unknown) => String(value ?? "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("pt-BR");
type Detail = { data?: Values; error?: string };
export function ReceivingList({ rows, transport, perfil, pending, actions, onOpen }: {
    rows: Values[]; transport: Transport; perfil: Perfil; pending: boolean; actions: string[];
    onOpen: (row: Values, action?: string) => void;
}) {
    const catalog = useReferenceCatalog();
    const [details, setDetails] = useState<{ rows?: Values[]; values: Record<string, Detail> }>({ values: {} });
    const [search, setSearch] = useState("");
    const [situation, setSituation] = useState("");
    const [conference, setConference] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [revision, setRevision] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        let active = true, next = 0;
        const values: Record<string, Detail> = {};
        setDetails({ rows, values: {} });
        async function worker() {
            while (active && next < rows.length) {
                const row = rows[next++], id = recordIdentity(row);
                try {
                    const data = await call(transport, "PedidoEntradaController.consultar", undefined, { id }, {}, controller.signal);
                    if (!isObject(data) || !isObject(data.pedido) || recordIdentity(data) !== id ||
                        String(data.pedido.clienteId) !== String(row.clienteId) || String(data.pedido.armazemId) !== String(row.armazemId))
                        throw new Error("Detalhe incompatível com o pedido desta lista.");
                    values[id] = { data };
                } catch (error) {
                    values[id] = { error: error instanceof Error ? error.message : "Não foi possível consultar a conferência." };
                }
                if (active) setDetails({ rows, values: { ...values } });
            }
        }
        // Somente a página recebida; no máximo quatro leituras concorrentes.
        for (let i = 0; i < Math.min(4, rows.length); i++) void worker();
        return () => { active = false; controller.abort(); };
    }, [rows, transport, revision]);
    const current = details.rows === rows ? details.values : {};
    const name = (kind: "clientes" | "armazens", id: unknown) => catalog?.[kind].find((item) => String(item.id) === String(id))?.nome ?? `ID ${String(id)}`;
    const filtered = rows.filter((row) => {
        const detail = current[recordIdentity(row)]?.data, root = detail ? recordRoot(detail) : row;
        const notes = receivingNotes(detail);
        const created = root.criadoEm ? new Date(String(root.criadoEm)).toLocaleDateString("sv-SE", { timeZone: "America/Sao_Paulo" }) : "";
        const text = [root.id, root.referencia, name("clientes", root.clienteId), name("armazens", root.armazemId), ...notes.flatMap((note) => [note.numero, `${note.serie}/${note.numero}`, note.chaveAcesso])].map(normalize).join(" ");
        return (!search || text.includes(normalize(search))) && (!situation || root.situacao === situation) &&
            (!conference || (detail && receivingState(detail) === conference)) &&
            (!from || (created && created >= from)) && (!to || (created && created <= to));
    });
    const incomplete = rows.filter((row) => !current[recordIdentity(row)]?.data).length;
    return <section aria-label="Lista de pedidos de entrada">
        <div className="record-toolbar receiving-filters">
            <label>Pedido ou nota nesta página<input type="search" value={search} onChange={(event) => setSearch(event.target.value)} /></label>
            <label>Situação nesta página<select value={situation} onChange={(event) => setSituation(event.target.value)}><option value="">Todas</option>{["RASCUNHO", "EM_CONFERENCIA", "QUARENTENA", "EFETIVADO", "CANCELADO"].map((value) => <option key={value} value={value}>{situationLabel(value)}</option>)}</select></label>
            <label>Conferência nesta página<select value={conference} onChange={(event) => setConference(event.target.value)}><option value="">Todos os resultados</option>{["Pendente", "Em conferência", "Conferido", "Divergente", "Conferido com divergência", "Cancelado"].map((value) => <option key={value}>{value}</option>)}</select></label>
            <label>Criado a partir de (nesta página)<input type="date" value={from} onChange={(event) => setFrom(event.target.value)} /></label>
            <label>Criado até (nesta página)<input type="date" value={to} min={from || undefined} onChange={(event) => setTo(event.target.value)} /></label>
            <button type="button" onClick={() => { setSearch(""); setSituation(""); setConference(""); setFrom(""); setTo(""); }}>Limpar busca nesta página</button>
        </div>
        {incomplete > 0 && <p role="status">Notas e conferência de {incomplete} pedido(s) ainda sem consulta concluída. A busca por nota e resultado usa os detalhes consultados; ausência de resposta não significa conferido.</p>}
        {Object.values(current).some((item) => item.error) && <div role="alert"><p>Não foi possível consultar todos os detalhes. Os pedidos continuam acessíveis; confira o resultado no detalhe.</p><button type="button" disabled={pending} onClick={() => setRevision((value) => value + 1)}>Tentar consultar notas e conferência novamente</button></div>}
        {filtered.length === 0 && <p className="empty">{rows.length ? "Nenhum pedido corresponde aos filtros desta página. Limpe a busca ou consulte outra página." : "Ainda não há pedidos de entrada neste contexto."}</p>}
        {filtered.length > 0 && <div className="table-scroll record-table"><table>
            <caption>Pedidos de entrada · {filtered.length} de {rows.length} registros nesta página</caption>
            <thead><tr>{["Pedido", "Cliente", "Armazém", "Notas", "Primeira chegada física", "Efetivação", "Conferência", "Situação", "Ações"].map((title) => <th scope="col" key={title}>{title}</th>)}</tr></thead>
            <tbody>{filtered.map((row) => {
                const id = recordIdentity(row), state = current[id], detail = state?.data, root = detail ? recordRoot(detail) : row;
                const available = visibleRecordActions(actions, perfil, root);
                const primary = available.find((action) => action === "PedidoEntradaController.iniciar" || action === "PedidoEntradaController.chegada");
                return <tr key={id}>
                    <th scope="row"><button className="record-link" type="button" disabled={pending} onClick={() => onOpen(root)} aria-label={`Ver detalhes de ${String(root.referencia ?? id)} · ${id}`}>{String(root.referencia ?? id)}</button><small className="receiving-order-id">Pedido {id}</small></th>
                    <td>{name("clientes", root.clienteId)}</td><td>{name("armazens", root.armazemId)}</td>
                    <td>{detail ? receivingNotes(detail).map((note) => `${note.serie}/${note.numero}`).join(", ") || "Sem notas" : state?.error ? "Não consultado" : "Consultando…"}</td>
                    <td>{detail ? receivingDate(firstArrival(detail)) : "—"}</td><td>{receivingDate(root.efetivadoEm)}</td>
                    <td><span className="record-status">{detail ? receivingState(detail) : state?.error ? "Não consultado" : "Consultando…"}</span></td><td><span className="record-status">{situationLabel(root.situacao)}</span></td>
                    <td><div className="record-row-actions"><button type="button" disabled={pending} onClick={() => onOpen(root)}>Ver pedido</button>
                        {primary && <button type="button" disabled={pending} onClick={() => onOpen(root, primary)}>{primary.endsWith(".iniciar") ? "Iniciar conferência" : "Continuar conferência"}</button>}
                        <details className="receiving-row-menu"><summary aria-label={`Mais ações do pedido ${id}`}>Mais ações</summary><div>{available.map((action) => <button type="button" key={action} disabled={pending} onClick={() => onOpen(root, action)}>{recordActionLabel(action)}</button>)}</div></details>
                    </div></td>
                </tr>;
            })}</tbody>
        </table></div>}
    </section>;
}
