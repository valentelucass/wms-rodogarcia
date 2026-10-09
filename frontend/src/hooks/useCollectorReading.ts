import { useEffect, useRef, useState } from "react";
import {
    endpoint,
    isObject,
    type Values,
    type Perfil,
} from "../contracts/runtime";
import type { Transport } from "../api/client";
import { useRequestScope } from "./useRequestScope";
export function useCollectorReading(
    transport: Transport,
    context: Values,
    perfil: Perfil,
) {
    const [code, setCode] = useState(String(context.codigo ?? ""));
    const [position, setPosition] = useState(
        String(context.enderecoCodigo ?? ""),
    );
    const [unit, setUnit] = useState<Values>();
    const [addresses, setAddresses] = useState<Values[]>([]);
    const [target, setTarget] = useState<Values>();
    const [error, setError] = useState("");
    const [busy, setBusy] = useState(false);
    const [page, setPage] = useState(0);
    const [mode, setMode] = useState("posicionar");
    const scope = useRequestScope();
    const posRef = useRef<HTMLInputElement>(null),
        captured = useRef("");
    const contextKey = JSON.stringify([
        context.clienteId,
        context.armazemId,
        context.enderecoId,
        context.enderecoCodigo,
        perfil,
    ]);
    const { cancel } = scope;
    useEffect(() => {
        cancel();
        setBusy(false);
        setUnit(undefined);
        setTarget(undefined);
        setAddresses([]);
        setError("");
        setPosition(String(context.enderecoCodigo ?? ""));
    }, [contextKey, cancel, context.enderecoCodigo]);
    const invalidate = () => {
        scope.cancel();
        setBusy(false);
        setUnit(undefined);
        setTarget(undefined);
        setAddresses([]);
        setError("");
    };
    const load = async () => {
        const ticket = scope.begin();
        const reading = code;
        setBusy(true);
        setError("");
        setUnit(undefined);
        setTarget(undefined);
        try {
            if (
                !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
                    reading,
                )
            )
                throw new Error("Leia o UUID completo da etiqueta.");
            const [u, a] = await Promise.all([
                transport.send({
                    endpoint: endpoint("EstoqueController.consultar"),
                    params: { codigo: reading },
                    query: {},
                    signal: ticket.signal,
                }),
                transport.send({
                    endpoint: endpoint("EnderecoController.listar"),
                    params: {},
                    query: {
                        armazemId: context.armazemId,
                        pagina: page,
                        tamanho: 20,
                    },
                    signal: ticket.signal,
                }),
            ]);
            if (!ticket.isCurrent()) return;
            if (
                !isObject(u.data) ||
                !isObject(u.data.unidade) ||
                u.data.unidade.codigo !== reading ||
                !isObject(a.data) ||
                !Array.isArray(a.data.itens)
            )
                throw new Error(
                    "Resposta de leitura incompatível com o UUID solicitado.",
                );
            captured.current = reading;
            setUnit(u.data);
            const listed = a.data.itens.filter(isObject);
            if (
                context.enderecoId &&
                !listed.some((v) => String(v.id) === String(context.enderecoId))
            ) {
                const destination = await transport.send({
                    endpoint: endpoint("EnderecoController.consultar"),
                    params: { id: context.enderecoId },
                    query: {},
                    signal: ticket.signal,
                });
                if (!ticket.isCurrent()) return;
                if (
                    !isObject(destination.data) ||
                    String(destination.data.armazemId) !==
                        String(context.armazemId)
                )
                    throw new Error(
                        "O destino selecionado não pertence ao armazém atual.",
                    );
                listed.push(destination.data);
            }
            setAddresses(listed);
        } catch (err) {
            if (ticket.isCurrent())
                setError(
                    err instanceof Error ? err.message : "Falha de leitura.",
                );
        } finally {
            if (ticket.isCurrent()) setBusy(false);
        }
    };
    const match = () => {
        const a = addresses.find((a) => a.codigo === position);
        if (!a) {
            setTarget(undefined);
            setError(
                "Código não encontrado na página consultada. Consulte outra página de posições; nenhum destino foi presumido.",
            );
            return;
        }
        setTarget(a);
        setError("");
    };
    return {
        code,
        setCode,
        position,
        setPosition,
        unit,
        addresses,
        target,
        setTarget,
        error,
        busy,
        page,
        setPage,
        mode,
        setMode,
        posRef,
        captured,
        invalidate,
        load,
        match,
    };
}
