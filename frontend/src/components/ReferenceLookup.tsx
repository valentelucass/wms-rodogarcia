import type { Request } from "../api/client";
import { type Values } from "../contracts/runtime";
import type { Receipt, Transport } from "../api/client";
import { useReferenceLookup } from "../hooks/useReferenceLookup";
import { Result } from "./Result";
import type { Journey } from "../domain/journeys";
import { useState } from "react";
import { Dialog } from "./layout/Dialog";
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
    const [showResult, setShowResult] = useState(false);
    const { result, error, pending, load } = useReferenceLookup({
        context,
        transport,
        onReceipt,
    });
    const lookups = journey.lookups ?? [];
    if (!lookups.length) return null;
    return (
        <section
            className="workspace-panel reference-lookup"
            aria-label="Consultar referências para a tarefa"
        >
            <h2>Consultar referências para a tarefa</h2>
            <div className="button-row">
                {lookups.map(([title, id]) => (
                    <button
                        key={id}
                        disabled={pending}
                        onClick={() => {
                            setShowResult(true);
                            void load(id);
                        }}
                    >
                        {title}
                    </button>
                ))}
                <button
                    type="button"
                    disabled={!result || pending}
                    onClick={() => setShowResult(true)}
                >
                    Ver referências consultadas
                </button>
            </div>
            {pending && <p role="status">Consultando referências…</p>}
            {error && <p role="alert">{error}</p>}
            {result && showResult && !pending && (
                <Dialog
                    title="Referências consultadas"
                    wide
                    onClose={() => setShowResult(false)}
                >
                    <Result
                        data={result.receipt.data}
                        type={result.type}
                        onSelect={
                            selectionLocked
                                ? undefined
                                : (v, type) => {
                                      onRecord(v, type);
                                      setShowResult(false);
                                  }
                        }
                    />
                </Dialog>
            )}
        </section>
    );
}
