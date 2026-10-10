import { useState } from "react";
import type { Values } from "../../contracts/runtime";
import { useReferenceCatalog } from "./ReferenceCatalog";
import { ReferenceSelect } from "./ReferenceSelect";

export function ContextPicker({
    context,
    onApply,
}: {
    context: Values;
    onApply: (context: Values) => void;
}) {
    const catalog = useReferenceCatalog();
    const [client, setClient] = useState(String(context.clienteId ?? "")),
        [warehouse, setWarehouse] = useState(String(context.armazemId ?? ""));
    const [message, setMessage] = useState("");
    const scope = `${context.clienteId ?? ""}/${context.armazemId ?? ""}`;
    const [selectedScope, setSelectedScope] = useState(scope);
    const loading =
        !catalog ||
        (!client && !warehouse
            ? catalog.loading
            : (!!client && catalog.pending.clientes) ||
              (!!warehouse && catalog.pending.armazens));
    if (selectedScope !== scope) {
        setSelectedScope(scope);
        setClient(String(context.clienteId ?? ""));
        setWarehouse(String(context.armazemId ?? ""));
    }
    return (
        <form
            className="auth-context"
            aria-label="Contexto operacional"
            onSubmit={(event) => {
                event.preventDefault();
                if (!catalog || loading || catalog.error) {
                    setMessage(
                        "Carregue as opções antes de aplicar o contexto.",
                    );
                    return;
                }
                if (
                    (client &&
                        !catalog.clientes.some(
                            (option) => option.id === client,
                        )) ||
                    (warehouse &&
                        !catalog.armazens.some(
                            (option) => option.id === warehouse,
                        ))
                ) {
                    setMessage(
                        "Selecione cliente e armazém entre as opções disponíveis.",
                    );
                    return;
                }
                onApply({
                    ...(client ? { clienteId: client } : {}),
                    ...(warehouse ? { armazemId: warehouse } : {}),
                });
                setMessage("Contexto selecionado.");
            }}
        >
            <ReferenceSelect
                compact
                all
                kind="clientes"
                title="Cliente"
                name="clienteId"
                value={client}
                onChange={setClient}
            />
            <ReferenceSelect
                compact
                all
                kind="armazens"
                title="Armazém"
                name="armazemId"
                value={warehouse}
                onChange={setWarehouse}
            />
            <button
                className="context-apply"
                disabled={loading || !!catalog?.error}
            >
                Aplicar contexto
            </button>
            {catalog?.error && (
                <button
                    type="button"
                    className="context-retry"
                    onClick={catalog.refresh}
                >
                    Atualizar opções
                </button>
            )}
            <span
                role="status"
                className={
                    message === "Contexto selecionado."
                        ? "sr-only"
                        : "context-error"
                }
            >
                {catalog?.error || message}
            </span>
        </form>
    );
}
