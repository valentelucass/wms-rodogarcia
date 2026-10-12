import { useEffect, useState } from "react";
import { StatusBadge, statusLabel } from "../../design-system/StatusBadge";
import { call, type Transport } from "../../api/client";
import { isObject, type Perfil, type Values } from "../../contracts/runtime";
import { recordIdentity, recordRoot } from "../../domain/recordContext";
import { visibleRecordActions } from "../../domain/recordPages";
import { useReferenceCatalog } from "../../components/context/ReferenceCatalog";
import { RecordText } from "../../components/records/RecordText";
import { ReceivingRowActions } from "./ReceivingRowActions";
import {
    receivingDate,
    receivingNotes,
    receivingState,
    firstArrival,
    situationLabel,
} from "./orderPresentation";

const normalize = (value: unknown) =>
    String(value ?? "")
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("pt-BR");
type Detail = { data?: Values; error?: string };
export function ReceivingList({
    rows,
    transport,
    perfil,
    pending,
    loaded,
    actions,
    onOpen,
}: {
    rows: Values[];
    transport: Transport;
    perfil: Perfil;
    pending: boolean;
    loaded: boolean;
    actions: string[];
    onOpen: (row: Values, action?: string) => void;
}) {
    const catalog = useReferenceCatalog();
    const [details, setDetails] = useState<{
        rows?: Values[];
        values: Record<string, Detail>;
    }>({ values: {} });
    const [search, setSearch] = useState("");
    const [situation, setSituation] = useState("");
    const [conference, setConference] = useState("");
    const [from, setFrom] = useState("");
    const [to, setTo] = useState("");
    const [revision, setRevision] = useState(0);
    useEffect(() => {
        const controller = new AbortController();
        let active = true,
            next = 0;
        const values: Record<string, Detail> = {};
        setDetails({ rows, values: {} });
        async function worker() {
            while (active && next < rows.length) {
                const row = rows[next++],
                    id = recordIdentity(row);
                try {
                    const data = await call(
                        transport,
                        "PedidoEntradaController.consultar",
                        undefined,
                        { id },
                        {},
                        controller.signal,
                    );
                    if (
                        !isObject(data) ||
                        !isObject(data.pedido) ||
                        recordIdentity(data) !== id ||
                        String(data.pedido.clienteId) !==
                            String(row.clienteId) ||
                        String(data.pedido.armazemId) !== String(row.armazemId)
                    )
                        throw new Error(
                            "Detalhe incompatível com o pedido desta lista.",
                        );
                    values[id] = { data };
                } catch (error) {
                    values[id] = {
                        error:
                            error instanceof Error
                                ? error.message
                                : "Não foi possível consultar a conferência.",
                    };
                }
                if (active) setDetails({ rows, values: { ...values } });
            }
        }
        // Somente a página recebida; no máximo quatro leituras concorrentes.
        for (let i = 0; i < Math.min(4, rows.length); i++) void worker();
        return () => {
            active = false;
            controller.abort();
        };
    }, [rows, transport, revision]);
    const current = details.rows === rows ? details.values : {};
    const name = (kind: "clientes" | "armazens", id: unknown) =>
        catalog?.[kind].find((item) => String(item.id) === String(id))?.nome ??
        `ID ${String(id)}`;
    const filtered = rows.filter((row) => {
        const detail = current[recordIdentity(row)]?.data,
            root = detail ? recordRoot(detail) : row;
        const notes = receivingNotes(detail);
        const created = root.criadoEm
            ? new Date(String(root.criadoEm)).toLocaleDateString("sv-SE", {
                  timeZone: "America/Sao_Paulo",
              })
            : "";
        const text = [
            root.id,
            root.referencia,
            name("clientes", root.clienteId),
            name("armazens", root.armazemId),
            ...notes.flatMap((note) => [
                note.numero,
                `${note.serie}/${note.numero}`,
                note.chaveAcesso,
            ]),
        ]
            .map(normalize)
            .join(" ");
        return (
            (!search || text.includes(normalize(search))) &&
            (!situation || root.situacao === situation) &&
            (!conference ||
                (detail && receivingState(detail) === conference)) &&
            (!from || (created && created >= from)) &&
            (!to || (created && created <= to))
        );
    });
    const incomplete = rows.filter(
        (row) => !current[recordIdentity(row)]?.data,
    ).length;
    return (
        <section
            className="receiving-list"
            aria-label="Lista de pedidos de entrada"
        >
            <div className="record-toolbar receiving-filters">
                <label>
                    Pedido ou nota nesta página
                    <input
                        type="search"
                        value={search}
                        onChange={(event) => setSearch(event.target.value)}
                    />
                </label>
                <label>
                    Situação nesta página
                    <select
                        value={situation}
                        onChange={(event) => setSituation(event.target.value)}
                    >
                        <option value="">Todas</option>
                        {[
                            "RASCUNHO",
                            "EM_CONFERENCIA",
                            "QUARENTENA",
                            "EFETIVADO",
                            "CANCELADO",
                        ].map((value) => (
                            <option key={value} value={value}>
                                {situationLabel(value)}
                            </option>
                        ))}
                    </select>
                </label>
                <label>
                    Conferência nesta página
                    <select
                        value={conference}
                        onChange={(event) => setConference(event.target.value)}
                    >
                        <option value="">Todos os resultados</option>
                        {[
                            "Pendente",
                            "Em conferência",
                            "Conferido",
                            "Divergente",
                            "Conferido com divergência",
                            "Cancelado",
                        ].map((value) => (
                            <option key={value}>{value}</option>
                        ))}
                    </select>
                </label>
                <label>
                    Criado a partir de (nesta página)
                    <input
                        type="date"
                        value={from}
                        onChange={(event) => setFrom(event.target.value)}
                    />
                </label>
                <label>
                    Criado até (nesta página)
                    <input
                        type="date"
                        value={to}
                        min={from || undefined}
                        onChange={(event) => setTo(event.target.value)}
                    />
                </label>
                <button
                    type="button"
                    onClick={() => {
                        setSearch("");
                        setSituation("");
                        setConference("");
                        setFrom("");
                        setTo("");
                    }}
                >
                    Limpar busca nesta página
                </button>
            </div>
            {incomplete > 0 && (
                <p role="status">
                    Notas e conferência de {incomplete} pedido(s) ainda sem
                    consulta concluída. A busca por nota e resultado usa os
                    detalhes consultados; ausência de resposta não significa
                    conferido.
                </p>
            )}
            {Object.values(current).some((item) => item.error) && (
                <div role="alert">
                    <p>
                        Não foi possível consultar todos os detalhes. Os pedidos
                        continuam acessíveis; confira o resultado no detalhe.
                    </p>
                    <button
                        type="button"
                        disabled={pending}
                        onClick={() => setRevision((value) => value + 1)}
                    >
                        Tentar consultar notas e conferência novamente
                    </button>
                </div>
            )}
            {loaded && !pending && filtered.length === 0 && (
                <p className="empty">
                    {rows.length
                        ? "Nenhum pedido corresponde aos filtros desta página. Limpe a busca ou consulte outra página."
                        : "Ainda não há pedidos de entrada neste contexto."}
                </p>
            )}
            {filtered.length > 0 && (
                <div
                    className="table-scroll record-table receiving-table"
                    role="region"
                    aria-label="Pedidos de entrada, tabela com rolagem horizontal"
                    tabIndex={0}
                >
                    <table>
                        <caption>
                            Pedidos de entrada · {filtered.length} de{" "}
                            {rows.length} registros nesta página
                        </caption>
                        <colgroup>
                            <col className="receiving-column-order" />
                            <col />
                            <col />
                            <col className="receiving-column-origin" />
                            <col className="receiving-column-notes" />
                            <col className="receiving-column-date" />
                            <col className="receiving-column-date" />
                            <col className="receiving-column-conference" />
                            <col className="receiving-column-situation" />
                            <col className="receiving-column-actions" />
                        </colgroup>
                        <thead>
                            <tr>
                                {[
                                    "Pedido",
                                    "Cliente",
                                    "Armazém",
                                    "Origem",
                                    "Notas",
                                    "Primeira chegada física",
                                    "Efetivação",
                                    "Conferência",
                                    "Situação",
                                    "Ações",
                                ].map((title) => (
                                    <th scope="col" key={title}>
                                        {title}
                                    </th>
                                ))}
                            </tr>
                        </thead>
                        <tbody>
                            {filtered.map((row) => {
                                const id = recordIdentity(row),
                                    state = current[id],
                                    detail = state?.data,
                                    root = detail ? recordRoot(detail) : row;
                                const available = visibleRecordActions(
                                    actions,
                                    perfil,
                                    root,
                                );
                                const primary = available.find(
                                    (action) =>
                                        action ===
                                            "PedidoEntradaController.iniciar" ||
                                        action ===
                                            "PedidoEntradaController.chegada",
                                );
                                const conference = detail
                                    ? receivingState(detail)
                                    : state?.error
                                      ? "Não consultado"
                                      : "Consultando…";
                                return (
                                    <tr key={id}>
                                        <th scope="row">
                                            <div className="receiving-order-identity">
                                                <button
                                                    className="record-link"
                                                    title={String(
                                                        root.referencia ?? id,
                                                    )}
                                                    type="button"
                                                    disabled={pending}
                                                    onClick={() => onOpen(root)}
                                                    aria-label={`Ver detalhes de ${String(root.referencia ?? id)} · ${id}`}
                                                >
                                                    {String(
                                                        root.referencia ?? id,
                                                    )}
                                                </button>
                                                <small className="receiving-order-id">
                                                    <RecordText
                                                        text={`Pedido ${id}`}
                                                    />
                                                </small>
                                            </div>
                                        </th>
                                        <td>
                                            <RecordText
                                                text={name(
                                                    "clientes",
                                                    root.clienteId,
                                                )}
                                            />
                                        </td>
                                        <td>
                                            <RecordText
                                                text={name(
                                                    "armazens",
                                                    root.armazemId,
                                                )}
                                            />
                                        </td>
                                        <td>
                                            {root.origemCriacao === "XML"
                                                ? "XML"
                                                : "Manual"}
                                        </td>
                                        <td>
                                            <RecordText
                                                text={
                                                    detail
                                                        ? receivingNotes(detail)
                                                              .map(
                                                                  (note) =>
                                                                      `${note.serie}/${note.numero}`,
                                                              )
                                                              .join(", ") ||
                                                          "Sem notas"
                                                        : state?.error
                                                          ? "Não consultado"
                                                          : "Consultando…"
                                                }
                                            />
                                        </td>
                                        <td>
                                            <RecordText
                                                text={
                                                    detail
                                                        ? receivingDate(
                                                              firstArrival(
                                                                  detail,
                                                              ),
                                                          )
                                                        : "—"
                                                }
                                            />
                                        </td>
                                        <td>
                                            <RecordText
                                                text={receivingDate(
                                                    root.efetivadoEm,
                                                )}
                                            />
                                        </td>
                                        <td>
                                            <StatusBadge
                                                value={conference}
                                                label={
                                                    <RecordText
                                                        text={statusLabel(
                                                            conference,
                                                        )}
                                                    />
                                                }
                                            />
                                        </td>
                                        <td>
                                            <StatusBadge
                                                value={root.situacao}
                                                label={
                                                    <RecordText
                                                        text={situationLabel(
                                                            root.situacao,
                                                        )}
                                                    />
                                                }
                                            />
                                        </td>
                                        <td>
                                            <ReceivingRowActions
                                                id={id}
                                                available={available}
                                                primary={primary}
                                                pending={pending}
                                                onOpen={(action) =>
                                                    onOpen(root, action)
                                                }
                                            />
                                        </td>
                                    </tr>
                                );
                            })}
                        </tbody>
                    </table>
                </div>
            )}
        </section>
    );
}
