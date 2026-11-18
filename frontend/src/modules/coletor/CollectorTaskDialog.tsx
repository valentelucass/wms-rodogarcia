import { useEffect, useState } from "react";
import type { TarefaColetorDto_Detalhe } from "../../contracts/types";
import { Dialog } from "../../components/layout/Dialog";
import { FormReferences } from "../../components/FormReferences";
import {
    inventoryRead,
    useInventoryWrite,
} from "../regularizacao/inventoryApi";
import { InventoryDialog } from "../regularizacao/InventoryDialog";
import {
    taskKinds,
    taskStatus,
    type CollectorTaskProps,
} from "./CollectorTasks";
import { CollectorTaskExecution } from "./CollectorTaskExecution";

export function CollectorTaskDialog(
    props: CollectorTaskProps & {
        id: string;
        onClose: () => void;
        onChanged: () => void;
    },
) {
    const [detail, setDetail] = useState<TarefaColetorDto_Detalhe>(),
        [loading, setLoading] = useState(false),
        [error, setError] = useState("");
    const [revision, setRevision] = useState(0),
        [counting, setCounting] = useState(false);
    const receipt: NonNullable<CollectorTaskProps["onReceipt"]> = (...args) =>
        props.onReceipt?.(...args);
    const writer = useInventoryWrite(props.transport, props.perfil, receipt);
    useEffect(() => {
        const abort = new AbortController();
        let current = true;
        setLoading(true);
        setError("");
        void inventoryRead(
            props.transport,
            props.perfil,
            "TarefaColetorController.consultar",
            { id: props.id },
            {},
            abort.signal,
        )
            .then((r) => {
                if (!current) return;
                if (String(r.data.tarefa?.id) !== props.id)
                    throw new Error("A consulta retornou outra tarefa.");
                setDetail(r.data);
            })
            .catch((e) => {
                if (current)
                    setError(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível consultar a tarefa.",
                    );
            })
            .finally(() => {
                if (current) setLoading(false);
            });
        return () => {
            current = false;
            abort.abort();
        };
    }, [props.transport, props.perfil, props.id, revision]);
    const task = detail?.tarefa,
        locked = writer.pending || writer.uncertain;
    const closed =
            task?.situacao === "CONCLUIDA" || task?.situacao === "CANCELADA",
        waiting = task?.situacao === "AGUARDANDO_RESERVA";
    const changed = (value: unknown) => {
        setDetail(value as TarefaColetorDto_Detalhe);
        props.onChanged();
    };
    if (counting && task && detail?.inventario)
        return (
            <FormReferences.Provider
                value={{
                    defaults: {
                        clienteId: task.clienteId,
                        armazemId: task.armazemId,
                    },
                    options: {},
                }}
            >
                <InventoryDialog
                    transport={props.transport}
                    perfil={props.perfil}
                    context={{
                        clienteId: task.clienteId,
                        armazemId: task.armazemId,
                    }}
                    initial={detail.inventario}
                    taskId={props.id}
                    onReceipt={receipt}
                    onClose={() => {
                        setCounting(false);
                        setRevision((v) => v + 1);
                        props.onChanged();
                    }}
                    onChanged={props.onChanged}
                />
            </FormReferences.Provider>
        );
    return (
        <Dialog
            title={
                taskKinds.find((k) => k.type === task?.tipo)?.title ??
                "Tarefa do coletor"
            }
            icon="coletor"
            locked={locked}
            onClose={props.onClose}
            className="collector-task-dialog"
        >
            {loading && <p role="status">Consultando tarefa…</p>}
            {error && <p role="alert">{error}</p>}
            {task && !loading && !error && (
                <>
                    <div className="collector-task-summary">
                        <strong>{task.referencia}</strong>
                        <p>
                            {task.cliente} · {task.armazem}
                        </p>
                        {task.sku && (
                            <p>
                                {task.sku} · {task.descricao}
                            </p>
                        )}
                        <p role="status">
                            {taskStatus[task.situacao ?? ""] ?? task.situacao}
                        </p>
                        {task.responsavel && (
                            <p>
                                {task.minha
                                    ? "Esta tarefa está com você."
                                    : "Esta tarefa está com outro operador."}
                            </p>
                        )}
                    </div>
                    {waiting && <p>{task.impedimento}</p>}
                    {!closed && !waiting && !task.responsavel && (
                        <button
                            className="primary"
                            disabled={loading || locked}
                            onClick={() =>
                                writer.run(
                                    "TarefaColetorController.iniciar",
                                    { id: props.id },
                                    {
                                        versao: task.versao,
                                        motivo: "Início da tarefa no coletor",
                                    },
                                    changed,
                                )
                            }
                        >
                            Iniciar tarefa
                        </button>
                    )}
                    {!closed && !waiting && task.minha && detail && (
                        <>
                            {task.tipo === "INVENTARIO" ? (
                                <button
                                    className="primary"
                                    disabled={loading || locked}
                                    onClick={() => setCounting(true)}
                                >
                                    Contar mercadorias
                                </button>
                            ) : (
                                <CollectorTaskExecution
                                    key={`${task.id}:${detail.pedido?.versao ?? task.versao}`}
                                    {...props}
                                    detail={detail}
                                    writer={writer}
                                    onConfirmed={changed}
                                />
                            )}
                        </>
                    )}
                    {!closed && task.minha && (
                        <button
                            disabled={loading || locked}
                            onClick={() =>
                                writer.run(
                                    "TarefaColetorController.liberar",
                                    { id: props.id },
                                    {
                                        versao: task.versao,
                                        motivo: "Tarefa devolvida à fila pelo operador",
                                    },
                                    changed,
                                )
                            }
                        >
                            Devolver tarefa à fila
                        </button>
                    )}
                    {!closed &&
                        !task.minha &&
                        task.responsavel &&
                        props.perfil !== "OPERACAO" && (
                            <button
                                disabled={loading || locked}
                                onClick={() =>
                                    writer.run(
                                        "TarefaColetorController.liberar",
                                        { id: props.id },
                                        {
                                            versao: task.versao,
                                            motivo: "Redistribuição da tarefa pelo supervisor",
                                        },
                                        changed,
                                    )
                                }
                            >
                                Devolver tarefa à fila
                            </button>
                        )}
                    {closed && (
                        <p role="status">
                            A tarefa foi encerrada. Os registros da operação
                            foram preservados.
                        </p>
                    )}
                </>
            )}
            {writer.error && <p role="alert">{writer.error}</p>}
            {writer.uncertain && (
                <div role="alert">
                    <p>
                        A resposta não chegou. Confira a mesma operação antes de
                        continuar.
                    </p>
                    <button onClick={writer.retry} disabled={writer.pending}>
                        Conferir operação pendente
                    </button>
                </div>
            )}
            <footer className="collector-task-dialog-actions">
                <button
                    disabled={loading || locked}
                    onClick={() => setRevision((v) => v + 1)}
                >
                    Atualizar tarefa
                </button>
                <button disabled={locked} onClick={props.onClose}>
                    Voltar às tarefas
                </button>
            </footer>
        </Dialog>
    );
}
