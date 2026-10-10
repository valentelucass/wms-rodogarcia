import {
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";
import { call, type Transport } from "../../api/client";

export type ReferenceOption = {
    id: string;
    nome: string;
    codigo: string;
    situacao: string;
};
interface Catalog {
    clientes: ReferenceOption[];
    armazens: ReferenceOption[];
    loading: boolean;
    pending: Record<"clientes" | "armazens", boolean>;
    error: string;
    refresh: () => void;
}
const ReferenceCatalog = createContext<Catalog | null>(null);
export const useReferenceCatalog = () => useContext(ReferenceCatalog);

/** Catálogos autorizados pelo backend; nenhum nome digitado vira ID. */
export function ReferenceCatalogProvider({
    transport,
    children,
    version = 0,
}: {
    transport: Transport;
    children: ReactNode;
    version?: string | number;
}) {
    const [revision, setRevision] = useState(0);
    const [state, setState] = useState<{
        transport?: Transport;
        revision: number;
        version?: string | number;
        clientes: ReferenceOption[];
        armazens: ReferenceOption[];
        error: string;
        ready: Record<"clientes" | "armazens", boolean>;
    }>({
        revision: -1,
        clientes: [],
        armazens: [],
        error: "",
        ready: { clientes: false, armazens: false },
    });
    const stale =
        state.transport !== transport ||
        state.revision !== revision ||
        state.version !== version;
    const pending = {
        clientes: stale || !state.ready.clientes,
        armazens: stale || !state.ready.armazens,
    };
    const loading = pending.clientes || pending.armazens;
    useEffect(() => {
        const controller = new AbortController();
        let active = true;
        async function load(
            kind: "ClienteController.listar" | "ArmazemController.listar",
        ) {
            async function read(pagina: number) {
                controller.signal.throwIfAborted();
                const result = await call(
                    transport,
                    kind,
                    undefined,
                    {},
                    { pagina, tamanho: 100 },
                    controller.signal,
                );
                controller.signal.throwIfAborted();
                if (
                    !result.itens ||
                    result.itens.some(
                        (v) => !v.id || !v.nome || !v.codigo || !v.situacao,
                    )
                )
                    throw new Error("Cadastro incompleto.");
                if (
                    !Number.isSafeInteger(result.totalPaginas) ||
                    result.totalPaginas < 0
                )
                    throw new Error("Paginação de catálogo inválida.");
                if (
                    result.itens.length === 0 &&
                    pagina + 1 < result.totalPaginas
                )
                    throw new Error(
                        "Catálogo mudou durante a consulta. Atualize as opções.",
                    );
                return result;
            }
            const first = await read(0);
            const pages: ReferenceOption[][] = [
                first.itens as ReferenceOption[],
            ];
            let next = 1;
            async function worker() {
                while (next < first.totalPaginas) {
                    const pagina = next++;
                    const result = await read(pagina);
                    if (
                        result.totalPaginas !== first.totalPaginas ||
                        result.totalItens !== first.totalItens
                    )
                        throw new Error(
                            "Catálogo mudou durante a consulta. Atualize as opções.",
                        );
                    pages[pagina] = result.itens as ReferenceOption[];
                }
            }
            // Páginas são independentes após conhecer o total; limite por catálogo.
            await Promise.all(
                Array.from(
                    {
                        length: Math.min(
                            3,
                            Math.max(0, first.totalPaginas - 1),
                        ),
                    },
                    worker,
                ),
            );
            const options = pages.flat();
            return [
                ...new Map(
                    options.map((option) => [option.id, option]),
                ).values(),
            ].sort(
                (a, b) =>
                    a.nome.localeCompare(b.nome, "pt-BR", {
                        sensitivity: "base",
                    }) || a.codigo.localeCompare(b.codigo),
            );
        }
        const publish = (
            kind: "clientes" | "armazens",
            options: ReferenceOption[],
        ) => {
            if (!active || controller.signal.aborted) return;
            setState((old) => {
                const current =
                    old.transport === transport &&
                    old.revision === revision &&
                    old.version === version;
                return {
                    transport,
                    revision,
                    version,
                    clientes:
                        kind === "clientes"
                            ? options
                            : current
                              ? old.clientes
                              : [],
                    armazens:
                        kind === "armazens"
                            ? options
                            : current
                              ? old.armazens
                              : [],
                    ready: {
                        clientes:
                            kind === "clientes" ||
                            (current && old.ready.clientes),
                        armazens:
                            kind === "armazens" ||
                            (current && old.ready.armazens),
                    },
                    error: "",
                };
            });
        };
        const fail = () => {
            if (active && !controller.signal.aborted) {
                controller.abort();
                setState({
                    transport,
                    revision,
                    version,
                    clientes: [],
                    armazens: [],
                    ready: { clientes: true, armazens: true },
                    error: "Não foi possível carregar clientes e armazéns. Atualize as opções.",
                });
            }
        };
        void load("ClienteController.listar")
            .then((options) => publish("clientes", options))
            .catch(fail);
        void load("ArmazemController.listar")
            .then((options) => publish("armazens", options))
            .catch(fail);
        return () => {
            active = false;
            controller.abort();
        };
    }, [transport, revision, version]);
    return (
        <ReferenceCatalog.Provider
            value={{
                clientes: stale ? [] : state.clientes,
                armazens: stale ? [] : state.armazens,
                loading,
                pending,
                error: loading ? "" : state.error,
                refresh: () => setRevision((v) => v + 1),
            }}
        >
            {children}
        </ReferenceCatalog.Provider>
    );
}
