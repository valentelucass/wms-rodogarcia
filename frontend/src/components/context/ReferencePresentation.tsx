import {
    createContext,
    useContext,
    useEffect,
    useMemo,
    useSyncExternalStore,
    type ReactNode,
} from "react";
import { endpoint, isObject, type Values } from "../../contracts/runtime";
import type { Transport } from "../../api/client";
import {
    indexRecords,
    presentationLabel,
    referenceField,
    referenceText,
    scalarText,
    typesForReference,
    type ReferenceRecords,
} from "../../domain/referencePresentation";
import { FormReferences } from "../FormReferences";
import { useReferenceCatalog } from "./ReferenceCatalog";
import { TableCellText } from "../records/TableCellText";

const lookups: Record<string, string> = {
    "ClienteDto.Resposta": "ClienteController.consultar",
    "ArmazemDto.Resposta": "ArmazemController.consultar",
    "ProdutoDto.Resposta": "ProdutoController.consultar",
    "EmbalagemDto.Resposta": "EmbalagemController.consultar",
    "EnderecoDto.Resposta": "EnderecoController.consultar",
    "PedidoEntradaDto.Resumo": "PedidoEntradaController.consultar",
    "PedidoSaidaDto.Detalhe": "PedidoSaidaController.consultar",
    "CalculoCobrancaDto.Resultado": "CalculoCobrancaController.consultar",
    "FatoServicoDto.Fato": "FatoServicoController.consultar",
    "FechamentoCobrancaDto.Fechamento":
        "FechamentoCobrancaController.consultar",
    "CargaInicialDto.Resultado": "CargaInicialController.consultar",
    "ConfiguracaoCobrancaDto.Tabela": "ConfiguracaoCobrancaController.tabela",
    "ContagemDto.Resultado": "ContagemController.consultar",
    "ContingenciaDto.Resultado": "ContingenciaController.consultar",
};

/** Per mounted access scope: exact GETs and names already read by selectors. No global cache. */
export function createReferenceReader(transport: Transport) {
    let data: ReferenceRecords = {},
        active = 0,
        closed = false,
        subscribers = 0;
    const requested = new Set<string>(),
        listeners = new Set<() => void>();
    const queue: (() => Promise<void>)[] = [],
        abort = new AbortController();
    const publish = () => listeners.forEach((fn) => fn());
    const drain = () => {
        while (!closed && active < 3 && queue.length) {
            active++;
            void queue.shift()!().finally(() => {
                active--;
                drain();
            });
        }
    };
    return {
        snapshot: () => data,
        subscribe: (fn: () => void) => {
            listeners.add(fn);
            return () => {
                listeners.delete(fn);
            };
        },
        remember(type: string, row: Values) {
            if (
                closed ||
                row.id == null ||
                !/^\d+$/.test(String(row.id)) ||
                data[type]?.some((known) => known === row)
            )
                return;
            data = indexRecords(
                row,
                type,
                Object.fromEntries(
                    Object.entries(data).map(([key, rows]) => [key, [...rows]]),
                ),
            );
            publish();
        },
        read(type: string, id: unknown) {
            const action = lookups[type],
                key = `${type}:${String(id)}`;
            if (
                !action ||
                id == null ||
                !/^\d+$/.test(String(id)) ||
                requested.has(key) ||
                closed
            )
                return;
            requested.add(key);
            queue.push(async () => {
                try {
                    const e = endpoint(action);
                    const result = await transport.send({
                        endpoint: e,
                        params: { id },
                        query: {},
                        signal: abort.signal,
                    });
                    if (
                        closed ||
                        abort.signal.aborted ||
                        !isObject(result.data)
                    )
                        return;
                    const root = isObject(result.data.pedido)
                        ? result.data.pedido
                        : result.data;
                    // A mismatched response can never name another entity.
                    if (String(root.id) !== String(id)) return;
                    data = indexRecords(
                        result.data,
                        e.response,
                        Object.fromEntries(
                            Object.entries(data).map(([k, v]) => [k, [...v]]),
                        ),
                    );
                    publish();
                } catch {
                    /* Restricted/missing references stay explicit, without retry or ID fallback. */
                }
            });
            drain();
        },
        close() {
            closed = true;
            queue.length = 0;
            abort.abort();
            listeners.clear();
        },
        retain() {
            subscribers++;
        },
        release() {
            subscribers--;
            queueMicrotask(() => {
                if (!subscribers) {
                    closed = true;
                    queue.length = 0;
                    abort.abort();
                    listeners.clear();
                }
            });
        },
    };
}
const ReferenceReader = createContext<ReturnType<
    typeof createReferenceReader
> | null>(null);
const ReferenceTransport = createContext<Transport | null>(null);
export const useReferenceTransport = () => useContext(ReferenceTransport);
export const useRememberReference = () => useContext(ReferenceReader)?.remember;
export function ReferencePresentationProvider({
    transport,
    children,
    scope,
}: {
    transport: Transport;
    children: ReactNode;
    scope?: string | number;
}) {
    const reader = useMemo(() => {
        void scope;
        return createReferenceReader(transport);
    }, [transport, scope]);
    useEffect(() => {
        reader.retain();
        return () => reader.release();
    }, [reader]);
    return (
        <ReferenceTransport.Provider value={transport}>
            <ReferenceReader.Provider value={reader}>
                {children}
            </ReferenceReader.Provider>
        </ReferenceTransport.Provider>
    );
}
const empty: ReferenceRecords = {};
const noSubscribe = () => () => {};
const emptySnapshot = () => empty;
export function usePresentationRecords(): ReferenceRecords {
    const refs = useContext(FormReferences),
        catalog = useReferenceCatalog(),
        reader = useContext(ReferenceReader);
    const loaded = useSyncExternalStore(
        reader?.subscribe ?? noSubscribe,
        reader?.snapshot ?? emptySnapshot,
    );
    return useMemo(() => {
        const merged: ReferenceRecords = { ...loaded };
        for (const [type, rows] of Object.entries(refs.records ?? {}))
            merged[type] = [
                ...new Map(
                    [...(loaded[type] ?? []), ...rows].map((row) => [
                        String(row.id),
                        row,
                    ]),
                ).values(),
            ];
        if (catalog) {
            merged["ClienteDto.Resposta"] = catalog.clientes;
            merged["ArmazemDto.Resposta"] = catalog.armazens;
        }
        return merged;
    }, [loaded, refs.records, catalog]);
}
export function ReferenceValue({
    field,
    value,
    row = {},
    type = "unknown",
    compact = false,
}: {
    field: string;
    value: unknown;
    row?: Values;
    type?: string;
    compact?: boolean;
}) {
    const refs = usePresentationRecords(),
        reader = useContext(ReferenceReader);
    const form = useContext(FormReferences);
    const reference = referenceField(field);
    const text = reference
        ? referenceText(field, value, row, type, refs)
        : scalarText(field, value, row, type);
    const target = typesForReference(field, type, row)[0];
    const parent =
        row.pedidoEntradaId ??
        row.pedidoId ??
        form.defaults.pedidoEntradaId ??
        form.defaults.pedidoId;
    useEffect(() => {
        if (reference && !text && target) reader?.read(target, value);
        if (
            !text &&
            parent &&
            ["notaId", "notaOrigemId", "itemNotaId"].includes(field)
        )
            reader?.read("PedidoEntradaDto.Resumo", parent);
    }, [reader, reference, text, target, value, parent, field]);
    const shown = text ?? "Referência indisponível";
    return compact ? (
        <TableCellText
            key={shown}
            text={shown}
            label={presentationLabel(field)}
        />
    ) : (
        <>{shown}</>
    );
}
