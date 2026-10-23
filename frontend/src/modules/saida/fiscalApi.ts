import { useEffect, useRef, useState } from "react";
import { ApiError, type Request, type Transport, type Receipt } from "../../api/client";
import { prepareCommand } from "../../api/prepareCommand";
import { canPresent, endpoint, type Perfil, type Values } from "../../contracts/runtime";
import { registerPageLeave } from "../../domain/pageLeave";
import { useRequestScope } from "../../hooks/useRequestScope";

export function fiscalRequest(id: string, perfil: Perfil, params: Values = {}, query: Values = {}, body: Values = {}) {
    const e = endpoint(id);
    if (!canPresent(perfil, e.permission)) throw new Error("Seu perfil não permite esta ação fiscal.");
    return prepareCommand({ endpoint: e, perfil, params, query, body });
}
export async function fiscalRead<T>(transport: Transport, perfil: Perfil, id: string, params: Values, query: Values, signal: AbortSignal) {
    const request = { ...fiscalRequest(id, perfil, params, query), signal };
    const receipt = await transport.send(request);
    return { data: receipt.data as T, receipt, request };
}
export function useFiscalWrite(transport: Transport, perfil: Perfil, onReceipt: (r: Receipt, type: string, id: string, request: Request) => void) {
    const scope = useRequestScope();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState("");
    const [uncertain, setUncertain] = useState(false);
    const running = useRef(false);
    const callback = useRef(onReceipt);
    callback.current = onReceipt;
    const original = useRef<{ request: Request; success: (data: unknown) => void } | undefined>(undefined);
    useEffect(() => {
        if (!pending && !uncertain) return;
        const clear = registerPageLeave(() => false);
        const stop = (event: BeforeUnloadEvent) => { event.preventDefault(); event.returnValue = ""; };
        window.addEventListener("beforeunload", stop);
        return () => { clear(); window.removeEventListener("beforeunload", stop); };
    }, [pending, uncertain]);
    async function execute(request: Request, success: (data: unknown) => void) {
        if (running.current) return;
        running.current = true; original.current = { request, success };
        const ticket = scope.begin(); setPending(true); setError(""); setUncertain(false);
        try {
            const r = await transport.send({ ...request, signal: ticket.signal });
            if (!ticket.isCurrent()) return;
            original.current = undefined;
            callback.current(r, request.endpoint.response, request.endpoint.id, request);
            success(r.data);
        } catch (e) {
            if (!ticket.isCurrent()) return;
            setError(e instanceof Error ? e.message : "Não foi possível confirmar a operação fiscal.");
            setUncertain(!(e instanceof ApiError) || e.uncertain);
        } finally { running.current = false; if (ticket.isCurrent()) setPending(false); }
    }
    return {
        pending, error, uncertain,
        run(id: string, params: Values, body: Values, success: (data: unknown) => void) {
            if (running.current || uncertain) return;
            try { void execute(fiscalRequest(id, perfil, params, {}, body), success); }
            catch(e) { setError(e instanceof Error ? e.message : "Confira os dados fiscais."); }
        },
        retry() { const last = original.current; if (last) void execute(last.request, last.success); },
    };
}
