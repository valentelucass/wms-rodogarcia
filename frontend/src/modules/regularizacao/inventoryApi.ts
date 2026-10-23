import { useEffect, useRef, useState } from "react";
import {
    ApiError,
    type Receipt,
    type Request,
    type Transport,
} from "../../api/client";
import { prepareCommand } from "../../api/prepareCommand";
import {
    canPresent,
    endpoint,
    type Perfil,
    type Values,
} from "../../contracts/runtime";
import type { ApiContracts } from "../../contracts/types";
import { registerPageLeave } from "../../domain/pageLeave";
import { useRequestScope } from "../../hooks/useRequestScope";

export type InventoryEndpoint = Extract<
    keyof ApiContracts,
    `InventarioController.${string}`
>;
export function inventoryRequest(
    id: InventoryEndpoint,
    perfil: Perfil,
    params: Values = {},
    query: Values = {},
    body: Values = {},
) {
    const e = endpoint(id);
    if (!canPresent(perfil, e.permission))
        throw new Error("Seu perfil não permite esta ação no inventário.");
    return prepareCommand({ endpoint: e, perfil, params, query, body });
}

export async function inventoryRead<T extends InventoryEndpoint>(
    transport: Transport,
    perfil: Perfil,
    id: T,
    params: Values,
    query: Values,
    signal: AbortSignal,
) {
    const request = { ...inventoryRequest(id, perfil, params, query), signal };
    const receipt = await transport.send(request);
    return {
        data: receipt.data as ApiContracts[T]["response"],
        receipt,
        request,
    };
}

export function useInventoryWrite(
    transport: Transport,
    perfil: Perfil,
    onReceipt: (
        receipt: Receipt,
        type: string,
        id: string,
        request: Request,
    ) => void,
) {
    const scope = useRequestScope();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState("");
    const [uncertain, setUncertain] = useState(false);
    const running = useRef(false);
    const callbacks = useRef(onReceipt);
    callbacks.current = onReceipt;
    const original = useRef<
        { request: Request; success: (data: unknown) => void } | undefined
    >(undefined);
    useEffect(() => {
        if (!pending && !uncertain) return;
        const stop = registerPageLeave(() => false);
        const beforeUnload = (event: BeforeUnloadEvent) => {
            event.preventDefault();
            event.returnValue = "";
        };
        window.addEventListener("beforeunload", beforeUnload);
        return () => {
            stop();
            window.removeEventListener("beforeunload", beforeUnload);
        };
    }, [pending, uncertain]);
    async function execute(request: Request, success: (data: unknown) => void) {
        if (running.current) return;
        running.current = true;
        original.current = { request, success };
        const ticket = scope.begin();
        setPending(true);
        setError("");
        setUncertain(false);
        try {
            const r = await transport.send({
                ...request,
                signal: ticket.signal,
            });
            if (!ticket.isCurrent()) return;
            original.current = undefined;
            callbacks.current(
                r,
                request.endpoint.response,
                request.endpoint.id,
                request,
            );
            success(r.data);
        } catch (e) {
            if (!ticket.isCurrent()) return;
            setError(
                e instanceof Error
                    ? e.message
                    : "Não foi possível confirmar a operação.",
            );
            setUncertain(!(e instanceof ApiError) || e.uncertain);
        } finally {
            running.current = false;
            if (ticket.isCurrent()) setPending(false);
        }
    }
    return {
        pending,
        error,
        uncertain,
        run: (
            id: InventoryEndpoint,
            params: Values,
            body: Values,
            success: (data: unknown) => void,
        ) => {
            if (running.current || uncertain) return;
            try {
                const request = inventoryRequest(
                    id,
                    perfil,
                    params,
                    {},
                    { ...body, operacaoId: crypto.randomUUID() },
                );
                void execute(request, success);
            } catch (e) {
                setError(e instanceof Error ? e.message : "Confira os campos.");
            }
        },
        retry: () => {
            const last = original.current;
            if (last) void execute(last.request, last.success);
        },
    };
}

export function inventoryQuantity(value: unknown) {
    if (value == null || value === "") return "—";
    const match = /^(-?)(\d+)(?:\.(\d+))?$/.exec(String(value));
    if (!match) return String(value);
    const fraction = (match[3] ?? "").replace(/0+$/, "");
    return (
        match[1] +
        match[2].replace(/\B(?=(\d{3})+(?!\d))/g, ".") +
        (fraction ? "," + fraction : "")
    );
}

export function inventoryDate(value: unknown) {
    if (!value) return "—";
    const date = new Date(String(value));
    return Number.isNaN(date.getTime())
        ? "—"
        : date.toLocaleString("pt-BR", {
              timeZone: "America/Sao_Paulo",
              dateStyle: "short",
              timeStyle: "short",
          });
}
