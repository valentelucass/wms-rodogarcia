import { useRef, useState } from "react";
import {
    endpoint,
    canPresent,
    type Values,
    type Perfil,
} from "../contracts/runtime";
import { initialField, initialValue } from "../contracts/codec";
import {
    ApiError,
    type Transport,
    type Receipt,
    type Request,
} from "../api/client";
import { prepareCommand } from "../api/prepareCommand";
import { changeContingencyKind } from "../modules/regularizacao/command";
import { useRequestScope } from "./useRequestScope";
export interface OperationOptions {
    id: string;
    transport: Transport;
    context: Values;
    perfil: Perfil;
    onReceipt?: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
}
export function useOperation({
    id,
    transport,
    context,
    perfil,
    onReceipt,
}: OperationOptions) {
    const e = endpoint(id),
        scope = useRequestScope();
    const [params, setParams] = useState<Values>(() =>
        Object.fromEntries(
            e.params.map((f) => [f.name, initialField(f, context)]),
        ),
    );
    const [query, setQuery] = useState<Values>(() =>
        Object.fromEntries(
            e.query.map((f) => [
                f.name,
                initialField(f, { ...context, fuso: "America/Sao_Paulo" }),
            ]),
        ),
    );
    const [body, setBody] = useState<Values>(() =>
        e.request ? (initialValue(e.request, context) as Values) : {},
    );
    const [file, setFile] = useState<File>();
    const [receipt, setReceipt] = useState<Receipt>();
    const [error, setError] = useState<Error>();
    const [pending, setPending] = useState(false),
        [completed, setCompleted] = useState(false),
        [confirm, setConfirm] = useState(false),
        [uncertain, setUncertain] = useState(false);
    const running = useRef(false),
        saved = useRef<Request | null>(null),
        status = useRef<HTMLDivElement>(null);
    const allowed = canPresent(perfil, e.permission);
    const updateBody = (v: Values) =>
        setBody(
            id === "ContingenciaController.registrar"
                ? changeContingencyKind(body, v)
                : v,
        );
    const prepare = () =>
        prepareCommand({ endpoint: e, params, query, body, file, perfil });
    const send = async (repeat = false) => {
        if (running.current || !allowed) return;
        running.current = true;
        setConfirm(false);
        setPending(true);
        setError(undefined);
        const ticket = scope.begin();
        try {
            const r = repeat ? saved.current : prepare();
            if (!r) throw new Error("Nenhuma operação para repetir.");
            const request = { ...r, signal: ticket.signal };
            saved.current = request;
            const result = await transport.send(request);
            if (!ticket.isCurrent()) return;
            setReceipt(result);
            setUncertain(false);
            setCompleted(e.method !== "GET");
            if (!result.replay) onReceipt?.(result, e.response, id, request);
        } catch (err) {
            if (!ticket.isCurrent()) return;
            const failure =
                ticket.signal.aborted && e.method !== "GET"
                    ? new ApiError(
                          "Espera interrompida. Consulte o estado atual; a operação pode ter sido registrada.",
                          0,
                          "RESPOSTA_DESCONHECIDA",
                          "",
                          true,
                      )
                    : err instanceof Error
                      ? err
                      : new Error("Falha na solicitação.");
            setError(failure);
            setReceipt(undefined);
            setUncertain(failure instanceof ApiError && failure.uncertain);
        } finally {
            running.current = false;
            if (ticket.isCurrent()) {
                setPending(false);
            }
        }
    };
    const submit = () => {
        if (running.current || completed || uncertain) return;
        try {
            prepare();
            setError(undefined);
            if (e.method === "GET") void send();
            else setConfirm(true);
        } catch (err) {
            setError(
                err instanceof Error ? err : new Error("Confira os campos."),
            );
            status.current?.focus();
        }
    };
    const startNew = () => {
        setCompleted(false);
        setReceipt(undefined);
        saved.current = null;
        setBody(e.request ? (initialValue(e.request, context) as Values) : {});
    };
    return {
        e,
        params,
        setParams,
        query,
        setQuery,
        body,
        updateBody,
        file,
        setFile,
        receipt,
        error,
        pending,
        completed,
        confirm,
        uncertain,
        allowed,
        status,
        submit,
        send,
        startNew,
        interrupt: scope.interrupt,
        cancelConfirmation: () => setConfirm(false),
        canReplay:
            !!saved.current &&
            typeof saved.current.body === "object" &&
            saved.current.body !== null &&
            "operacaoId" in saved.current.body,
    };
}
export type OperationState = ReturnType<typeof useOperation>;
