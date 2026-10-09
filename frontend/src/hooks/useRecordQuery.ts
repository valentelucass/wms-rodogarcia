import { useEffect, useRef, useState } from "react";
import {
    endpoint,
    canPresent,
    type Perfil,
    type Values,
} from "../contracts/runtime";
import { initialField } from "../contracts/codec";
import { prepareCommand } from "../api/prepareCommand";
import type { Transport, Request, Receipt } from "../api/client";
import { useRequestScope } from "./useRequestScope";

export function useRecordQuery(
    id: string,
    transport: Transport,
    context: Values,
    perfil: Perfil,
    onReceipt?: (
        r: Receipt,
        type: string,
        id: string,
        request: Request,
    ) => void,
) {
    const e = endpoint(id);
    const callback = useRef(onReceipt);
    const defaults = useRef<Values>(
        Object.fromEntries(
            [...e.params, ...e.query].map((f) => [
                f.name,
                initialField(f, { ...context, fuso: "America/Sao_Paulo" }),
            ]),
        ),
    );
    const [filters, setFilters] = useState(defaults.current);
    const [receipt, setReceipt] = useState<Receipt>();
    const [pending, setPending] = useState(false);
    const [error, setError] = useState("");
    const [needsFilters, setNeedsFilters] = useState(false);
    const scope = useRequestScope();
    const applied = useRef<Request | undefined>(undefined);
    const allowed = canPresent(perfil, e.permission);
    async function load(values: Values, automatic = false) {
        if (!allowed) return;
        let request: Request;
        try {
            request = prepareCommand({
                endpoint: e,
                perfil,
                params: Object.fromEntries(
                    e.params.map((f) => [f.name, values[f.name]]),
                ),
                query: Object.fromEntries(
                    e.query.map((f) => [f.name, values[f.name]]),
                ),
                body: {},
            });
        } catch (err) {
            setNeedsFilters(true);
            if (!automatic)
                setError(
                    err instanceof Error ? err.message : "Confira os filtros.",
                );
            return;
        }
        const ticket = scope.begin();
        setPending(true);
        setError("");
        setNeedsFilters(false);
        try {
            const result = await transport.send({
                ...request,
                signal: ticket.signal,
            });
            if (!ticket.isCurrent()) return;
            // A write can remove the last record on the last page.
            if (
                result.data &&
                typeof result.data === "object" &&
                "itens" in result.data &&
                "totalPaginas" in result.data &&
                "pagina" in result.data
            ) {
                const pages = Number(result.data.totalPaginas),
                    current = Number(result.data.pagina);
                if (current > 0 && current >= pages) {
                    const next = { ...values, pagina: Math.max(0, pages - 1) };
                    setFilters(next);
                    void load(next);
                    return;
                }
            }
            applied.current = request;
            setReceipt(result);
            callback.current?.(result, e.response, e.id, request);
        } catch (err) {
            if (!ticket.isCurrent()) return;
            setReceipt(undefined);
            setError(
                err instanceof Error
                    ? err.message
                    : "Não foi possível carregar os registros.",
            );
        } finally {
            if (ticket.isCurrent()) setPending(false);
        }
    }
    const refresh = () =>
        void load(
            applied.current
                ? { ...applied.current.params, ...applied.current.query }
                : filters,
        );
    // The page is keyed by source/context/profile. Workflow updates do not restart its query.
    useEffect(() => {
        void load(defaults.current, true);
        return scope.cancel;
    }, []);
    const targetFields = [
        ...e.params.map((f) => f.name),
        ...(id === "AuditoriaController.listar" ? ["tipo", "registroId"] : []),
    ];
    const targetKey = JSON.stringify(
        Object.fromEntries(targetFields.map((name) => [name, context[name]])),
    );
    const previousTarget = useRef(targetKey);
    useEffect(() => {
        if (previousTarget.current === targetKey) return;
        previousTarget.current = targetKey;
        const target = JSON.parse(targetKey) as Values;
        if (
            targetFields.length === 0 ||
            targetFields.some(
                (name) => target[name] === undefined || target[name] === "",
            )
        )
            return;
        const next = { ...defaults.current, ...target, pagina: 0 };
        setFilters(next);
        if (
            targetFields.every(
                (name) =>
                    applied.current &&
                    (applied.current.params[name] ??
                        applied.current.query[name]) === target[name],
            )
        )
            return;
        void load(next, true);
    }, [targetKey]);
    return {
        e,
        filters,
        setFilters,
        receipt,
        pending,
        error,
        needsFilters,
        allowed,
        hasFilters: Object.entries({
            ...applied.current?.params,
            ...applied.current?.query,
        }).some(
            ([name, value]) =>
                ![
                    "pagina",
                    "tamanho",
                    "clienteId",
                    "armazemId",
                    "fuso",
                ].includes(name) &&
                value !== "" &&
                value != null,
        ),
        search: () => void load({ ...filters, pagina: "0" }),
        reset: () => {
            setFilters(defaults.current);
            void load(defaults.current);
        },
        refresh,
        page: (pagina: number) => {
            if (pending || !applied.current) return;
            const next = {
                ...applied.current.params,
                ...applied.current.query,
                pagina,
            };
            setFilters(next);
            void load(next);
        },
    };
}
