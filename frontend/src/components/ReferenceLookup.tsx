import type { Request } from "../api/client";
import { type Values } from "../contracts/runtime";
import type { Receipt, Transport } from "../api/client";
import { useReferenceLookup } from "../hooks/useReferenceLookup";
import { Result } from "./Result";
import type { Journey } from "../domain/journeys";
export function ReferenceLookup({
    journey,
    context,
    transport,
    onRecord,
    onReceipt,
    selectionLocked = false,
}: {
    journey: Journey;
    context: Values;
    transport: Transport;
    onRecord: (v: Values, type: string) => void;
    selectionLocked?: boolean;
    onReceipt: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
}) {
    const { result, error, pending, load } = useReferenceLookup({
        context,
        transport,
        onReceipt,
    });
    const lookups = journey.lookups ?? [];
    if (!lookups.length) return null;
    return (
        <section aria-label="Consultar referências para a tarefa">
            <h2>Consultar referências para a tarefa</h2>
            {lookups.map(([title, id]) => (
                <button
                    key={id}
                    disabled={pending}
                    onClick={() => void load(id)}
                >
                    {title}
                </button>
            ))}
            {pending && <p role="status">Consultando referências…</p>}
            {error && <p role="alert">{error}</p>}
            {result && (
                <details open>
                    <summary>Referências consultadas</summary>
                    <Result
                        data={result.receipt.data}
                        type={result.type}
                        onSelect={selectionLocked ? undefined : onRecord}
                    />
                </details>
            )}
        </section>
    );
}
