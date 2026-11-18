import { useEffect, useState } from "react";
import { ApiError, type Transport } from "../../api/client";
import type { Perfil, Values } from "../../contracts/runtime";
import type { TarefaColetorDto_Tarefa } from "../../contracts/types";
import type { NextAction } from "../../domain/journeys";
import { inventoryRead } from "../regularizacao/inventoryApi";

export function DispatchTaskStatus({
    order,
    transport,
    perfil,
    disabled,
    onNavigate,
}: {
    order: Values;
    transport: Transport;
    perfil: Perfil;
    disabled: boolean;
    onNavigate: (next: NextAction) => void;
}) {
    const [task, setTask] = useState<TarefaColetorDto_Tarefa>();
    const [missing, setMissing] = useState(false),
        [error, setError] = useState("");
    const [revision, setRevision] = useState(0),
        [loading, setLoading] = useState(true);
    const pedidoId = String(order.id ?? "");
    useEffect(() => {
        const abort = new AbortController();
        let current = true;
        setTask(undefined);
        setMissing(false);
        setError("");
        setLoading(true);
        void inventoryRead(
            transport,
            perfil,
            "TarefaColetorController.disponibilidade",
            {},
            {},
            abort.signal,
        )
            .then(async (r) => {
                if (r.data.disponivel === false) return null;
                if (r.data.disponivel !== true)
                    throw new Error(
                        "Não foi possível verificar a disponibilidade das tarefas.",
                    );
                return inventoryRead(
                    transport,
                    perfil,
                    "TarefaColetorController.porPedido",
                    { pedidoId },
                    {},
                    abort.signal,
                );
            })
            .then((r) => {
                if (current) {
                    if (!r) {
                        setMissing(true);
                        return;
                    }
                    if (
                        String(r.data.origemId) !== pedidoId ||
                        r.data.tipo !== "SEPARACAO"
                    )
                        throw new Error(
                            "A consulta retornou a tarefa de outro pedido.",
                        );
                    setTask(r.data);
                }
            })
            .catch((e) => {
                if (current) {
                    if (e instanceof ApiError && e.status === 404)
                        setMissing(true);
                    else
                        setError(
                            e instanceof Error
                                ? e.message
                                : "Não foi possível consultar a separação.",
                        );
                }
            })
            .finally(() => {
                if (current) setLoading(false);
            });
        return () => {
            current = false;
            abort.abort();
        };
    }, [transport, perfil, pedidoId, order.versao, revision]);
    const labels: Record<string, string> = {
        AGUARDANDO_RESERVA: "Aguardando reserva",
        PENDENTE: "Aguardando separação",
        EM_ANDAMENTO: "Em separação",
        CONCLUIDA: "Separado",
        CANCELADA: "Cancelado",
    };
    return (
        <section
            className="dispatch-reservations"
            aria-label="Separação no Coletor"
        >
            <h3>Separação no Coletor</h3>
            {loading && <p role="status">Consultando tarefa do pedido…</p>}
            {task && (
                <>
                    <p role="status">
                        {labels[task.situacao ?? ""] ?? task.situacao}
                    </p>
                    <p>
                        A leitura das etiquetas e a separação são realizadas no
                        Coletor. O início exige reserva confirmada.
                    </p>
                    <button
                        type="button"
                        disabled={disabled}
                        onClick={() =>
                            onNavigate({
                                page: "coletor",
                                action: `tarefa:${task.id}`,
                                title: "Abrir tarefa no Coletor",
                            })
                        }
                    >
                        Acompanhar no Coletor
                    </button>
                </>
            )}
            {missing && (
                <>
                    <p>
                        Este pedido não possui tarefa vinculada. A separação
                        continua disponível na operação manual do Coletor.
                    </p>
                    <button
                        type="button"
                        disabled={disabled}
                        onClick={() =>
                            onNavigate({
                                page: "coletor",
                                action: "ExpedicaoController.ler",
                                title: "Abrir separação manual no Coletor",
                            })
                        }
                    >
                        Abrir separação manual no Coletor
                    </button>
                </>
            )}
            {error && <p role="alert">{error}</p>}
            <button
                type="button"
                disabled={disabled || loading}
                onClick={() => setRevision((v) => v + 1)}
            >
                Atualizar separação
            </button>
        </section>
    );
}
