import { useCallback, useEffect, useState } from "react";
import type { Request, Receipt, Transport } from "../../api/client";
import { ApiError } from "../../api/client";
import type { Perfil, Values } from "../../contracts/runtime";
import type {
    PaginaResponse,
    TarefaColetorDto_Tarefa,
    TarefaColetorDto_Totais,
} from "../../contracts/types";
import { Icon } from "../../design-system/Icon";
import {
    inventoryRead,
    inventoryQuantity,
} from "../regularizacao/inventoryApi";
import { CollectorTaskDialog } from "./CollectorTaskDialog";
import { information } from "../../content/information";

export type CollectorTaskProps = {
    transport: Transport;
    context: Values;
    perfil: Perfil;
    onReceipt?: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
};
export const taskKinds = [
    {
        type: "ENDERECAMENTO",
        key: "enderecamento",
        title: "Endereçar / movimentar",
        icon: "estoque",
    },
    {
        type: "SEPARACAO",
        key: "separacao",
        title: "Separação para saída",
        icon: "saida",
    },
    {
        type: "INVENTARIO",
        key: "inventario",
        title: "Inventário",
        icon: "contagem",
    },
] as const;
export const taskStatus: Record<string, string> = {
    PENDENTE: "Aguardando início",
    EM_ANDAMENTO: "Em andamento",
    AGUARDANDO_RESERVA: "Aguardando reserva",
    CONCLUIDA: "Concluída",
    CANCELADA: "Cancelada",
};

export function CollectorTasks(
    props: CollectorTaskProps & {
        startAction?: string;
        onManualSeparation?: () => void;
    },
) {
    const { transport, perfil, context } = props;
    const [available, setAvailable] = useState<boolean | null>(null),
        [message, setMessage] = useState("");
    const [totals, setTotals] = useState<TarefaColetorDto_Totais>();
    const [kind, setKind] = useState<string>(),
        [page, setPage] = useState(0);
    const [list, setList] = useState<PaginaResponse<TarefaColetorDto_Tarefa>>();
    const [loading, setLoading] = useState(false),
        [error, setError] = useState("");
    const [selected, setSelected] = useState<string>(),
        [revision, setRevision] = useState(0);
    const [availabilityRevision, setAvailabilityRevision] = useState(0);
    const refresh = useCallback(() => setRevision((v) => v + 1), []);
    const clienteId = context.clienteId,
        armazemId = context.armazemId;
    const [targetError, setTargetError] = useState("");
    const [missingTarget, setMissingTarget] = useState(false);
    useEffect(() => {
        if (!available || !props.startAction) return;
        const abort = new AbortController();
        let current = true;
        setTargetError("");
        setMissingTarget(false);
        setSelected(undefined);
        if (props.startAction === "SEPARACAO") setKind("SEPARACAO");
        const taskId = /^tarefa:(\d+)$/.exec(props.startAction)?.[1];
        if (taskId) {
            setKind("SEPARACAO");
            setSelected(taskId);
        }
        const pedidoId = /^pedido:(\d+)$/.exec(props.startAction)?.[1];
        if (pedidoId) {
            setKind("SEPARACAO");
            void inventoryRead(
                transport,
                perfil,
                "TarefaColetorController.porPedido",
                { pedidoId },
                {},
                abort.signal,
            )
                .then((r) => {
                    if (current) {
                        if (
                            String(r.data.origemId) !== pedidoId ||
                            r.data.tipo !== "SEPARACAO"
                        )
                            throw new Error(
                                "A consulta retornou a tarefa de outro pedido.",
                            );
                        setSelected(String(r.data.id));
                    }
                })
                .catch((e) => {
                    if (current) {
                        if (e instanceof ApiError && e.status === 404)
                            setMissingTarget(true);
                        else
                            setTargetError(
                                e instanceof Error
                                    ? e.message
                                    : "Não foi possível abrir a tarefa do pedido.",
                            );
                    }
                });
        }
        return () => {
            current = false;
            abort.abort();
        };
    }, [available, props.startAction, transport, perfil]);
    useEffect(() => {
        const abort = new AbortController();
        let current = true;
        setAvailable(null);
        void inventoryRead(
            transport,
            perfil,
            "TarefaColetorController.disponibilidade",
            {},
            {},
            abort.signal,
        )
            .then((r) => {
                if (current) {
                    setAvailable(r.data.disponivel);
                    setMessage(r.data.mensagem ?? "");
                }
            })
            .catch((e) => {
                if (current) {
                    setAvailable(false);
                    setMessage(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar as tarefas.",
                    );
                }
            });
        return () => {
            current = false;
            abort.abort();
        };
    }, [transport, perfil, availabilityRevision]);
    useEffect(() => {
        if (!available) return;
        const abort = new AbortController();
        let current = true;
        setLoading(true);
        setError("");
        setList(undefined);
        setTotals(undefined);
        void (async () => {
            try {
                const query = { clienteId, armazemId };
                const r = await inventoryRead(
                    transport,
                    perfil,
                    "TarefaColetorController.totais",
                    {},
                    query,
                    abort.signal,
                );
                if (!current) return;
                if (
                    ![
                        r.data.enderecamento,
                        r.data.separacao,
                        r.data.inventario,
                    ].every((n) => /^\d+$/.test(String(n)))
                )
                    throw new Error(
                        "A consulta não retornou os contadores das tarefas.",
                    );
                setTotals(r.data);
                if (kind) {
                    const result = await inventoryRead(
                        transport,
                        perfil,
                        "TarefaColetorController.listar",
                        {},
                        { ...query, tipo: kind, pagina: page, tamanho: 20 },
                        abort.signal,
                    );
                    if (!current) return;
                    if (!Array.isArray(result.data.itens))
                        throw new Error(
                            "A consulta não retornou a lista de tarefas.",
                        );
                    setList(result.data);
                }
            } catch (e) {
                if (current) {
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar as tarefas.",
                    );
                    setTotals(undefined);
                }
            } finally {
                if (current) setLoading(false);
            }
        })();
        return () => {
            current = false;
            abort.abort();
        };
    }, [
        transport,
        perfil,
        available,
        clienteId,
        armazemId,
        kind,
        page,
        revision,
    ]);
    useEffect(() => {
        if (!available) return;
        const interval = setInterval(() => {
            if (document.visibilityState !== "hidden") refresh();
        }, 15000);
        window.addEventListener("focus", refresh);
        return () => {
            clearInterval(interval);
            window.removeEventListener("focus", refresh);
        };
    }, [available, refresh]);
    if (available === false)
        return (
            <section className="collector-queue">
                <p role="status">{message}</p>
                <button onClick={() => setAvailabilityRevision((v) => v + 1)}>
                    Consultar tarefas novamente
                </button>
            </section>
        );
    return (
        <section className="collector-queue" aria-label="Tarefas do coletor">
            <header className="collector-queue-heading">
                <h2>Tarefas disponíveis</h2>
                <button onClick={refresh} disabled={loading || !available}>
                    <Icon name="refresh" />
                    <span>Atualizar</span>
                </button>
            </header>
            <div className="collector-queue-cards">
                {taskKinds.map((item) => {
                    const n = totals?.[item.key];
                    return (
                        <button
                            key={item.type}
                            className={`collector-queue-card collector-queue-card--${item.key}`}
                            disabled={!available}
                            aria-pressed={kind === item.type}
                            onClick={() => {
                                setKind(item.type);
                                setPage(0);
                            }}
                        >
                            <span className="collector-queue-icon">
                                <Icon name={item.icon} />
                            </span>
                            <span className="collector-queue-name">
                                {item.title}
                            </span>
                            <strong className="collector-queue-count">
                                {n == null ? "—" : inventoryQuantity(n)}
                            </strong>
                            <span className="collector-queue-caption">
                                {n === "0"
                                    ? "nenhuma tarefa pendente"
                                    : n === "1"
                                      ? "tarefa pendente"
                                      : "tarefas pendentes"}
                            </span>
                            <span className="collector-queue-arrow">
                                <Icon name="chevron-right" />
                            </span>
                        </button>
                    );
                })}
            </div>
            <details className="collector-queue-help">
                <summary>Como funcionam as tarefas?</summary>
                <p>{information.collectorTasks.description}</p>
            </details>
            {error && <p role="alert">{error}</p>}
            {targetError && <p role="alert">{targetError}</p>}
            {missingTarget && (
                <div role="status">
                    <p>
                        Este pedido não possui tarefa vinculada. Utilize a
                        separação manual do Coletor.
                    </p>
                    <button onClick={props.onManualSeparation}>
                        Abrir separação manual
                    </button>
                </div>
            )}
            {kind && (
                <section
                    className="collector-queue-list workspace-panel"
                    aria-label="Lista de tarefas"
                >
                    <header>
                        <h3>{taskKinds.find((k) => k.type === kind)?.title}</h3>
                        <button
                            onClick={() => {
                                setKind(undefined);
                                setPage(0);
                            }}
                        >
                            Fechar lista
                        </button>
                    </header>
                    {loading && <p role="status">Consultando tarefas…</p>}
                    {!loading && list?.itens?.length === 0 && (
                        <p>Nenhuma tarefa pendente neste contexto.</p>
                    )}
                    {list?.itens?.map((task) => (
                        <article key={task.id}>
                            <div>
                                <strong>{task.referencia}</strong>
                                <p>
                                    {task.cliente} · {task.armazem}
                                </p>
                                {task.sku && (
                                    <p>
                                        {task.sku} · {task.descricao}
                                    </p>
                                )}
                                <p>
                                    {taskStatus[task.situacao ?? ""] ??
                                        task.situacao}
                                    {task.responsavel
                                        ? task.minha
                                            ? " · Com você"
                                            : " · Com outro operador"
                                        : ""}
                                </p>
                                {task.impedimento && <p>{task.impedimento}</p>}
                            </div>
                            <button
                                onClick={() => setSelected(String(task.id))}
                            >
                                Ver tarefa
                                <Icon name="chevron-right" />
                            </button>
                        </article>
                    ))}
                    {list && list.totalPaginas > 1 && (
                        <nav aria-label="Páginas de tarefas">
                            <button
                                disabled={loading || page === 0}
                                onClick={() => setPage((p) => p - 1)}
                            >
                                Anterior
                            </button>
                            <span>
                                Página {page + 1} de {list.totalPaginas}
                            </span>
                            <button
                                disabled={
                                    loading || page + 1 >= list.totalPaginas
                                }
                                onClick={() => setPage((p) => p + 1)}
                            >
                                Próxima
                            </button>
                        </nav>
                    )}
                </section>
            )}
            {selected && (
                <CollectorTaskDialog
                    key={selected}
                    {...props}
                    id={selected}
                    onClose={() => {
                        setSelected(undefined);
                        refresh();
                    }}
                    onChanged={refresh}
                />
            )}
        </section>
    );
}
