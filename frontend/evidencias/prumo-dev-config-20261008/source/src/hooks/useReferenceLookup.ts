import type { Request } from "../api/client";
import { useEffect, useRef, useState } from "react";
import { endpoint, type Endpoint, type Values } from "../contracts/runtime";
import { route, type Transport, type Receipt } from "../api/client";
import { useRequestScope } from "./useRequestScope";
export function useReferenceLookup({
    context,
    transport,
    onReceipt,
}: {
    context: Values;
    transport: Transport;
    onReceipt: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
}) {
    const [result, setResult] = useState<{ receipt: Receipt; type: string }>();
    const [error, setError] = useState("");
    const [pending, setPending] = useState(false);
    const scope = useRequestScope();
    const { cancel } = scope;
    const current = useRef<{ endpoint: Endpoint; key: string } | undefined>(
        undefined,
    );
    useEffect(() => {
        if (!current.current) return;
        const { params, query } = lookupReferences(
            current.current.endpoint,
            context,
        );
        try {
            if (
                route(current.current.endpoint, params, query) ===
                current.current.key
            )
                return;
        } catch {
            /* Incomplete new context invalidates the old query. */
        }
        cancel();
        current.current = undefined;
        setPending(false);
        setResult(undefined);
        setError("");
    }, [context, cancel]);
    const load = async (id: string) => {
        if (pending) return;
        setPending(true);
        setError("");
        const e = endpoint(id);
        const ticket = scope.begin();
        try {
            const { params, query } = lookupReferences(e, context);
            current.current = { endpoint: e, key: route(e, params, query) };
            const request = {
                endpoint: e,
                params,
                query,
                signal: ticket.signal,
            };
            const r = await transport.send(request);
            if (!ticket.isCurrent()) return;
            setResult({ receipt: r, type: e.response });
            onReceipt(r, e.response, id, request);
        } catch (err) {
            if (ticket.isCurrent())
                setError(
                    err instanceof Error ? err.message : "Falha de consulta.",
                );
        } finally {
            if (ticket.isCurrent()) setPending(false);
        }
    };
    return { result, error, pending, load };
}
function lookupReferences(e: Endpoint, context: Values) {
    const params =
        e.id === "UnidadeLogisticaController.etiqueta"
            ? { codigo: context.codigoLido ?? context.codigo }
            : e.id === "UnidadeLogisticaController.consultar" ? {id:context.unidadeId} : {};
    const defaults={...context,pagina:0,tamanho:20};
    const query=Object.fromEntries(e.query.map(f=>[f.name,defaults[f.name as keyof typeof defaults]]).filter(([,value])=>value!==undefined));
    return { params, query };
}
