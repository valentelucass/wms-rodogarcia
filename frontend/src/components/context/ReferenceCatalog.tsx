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
    }>({ revision: -1, clientes: [], armazens: [], error: "" });
    const loading =
        state.transport !== transport ||
        state.revision !== revision ||
        state.version !== version;
    useEffect(() => {
        const controller = new AbortController();
        let active = true;
        async function load(
            kind: "ClienteController.listar" | "ArmazemController.listar",
        ) {
            const options: ReferenceOption[] = [];
            for (let pagina = 0; ; pagina++) {
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
                options.push(...(result.itens as ReferenceOption[]));
                if (pagina + 1 >= result.totalPaginas) break;
                if (result.itens.length === 0)
                    throw new Error(
                        "Catálogo mudou durante a consulta. Atualize as opções.",
                    );
            }
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
        void Promise.all([
            load("ClienteController.listar"),
            load("ArmazemController.listar"),
        ])
            .then(([clientes, armazens]) => {
                if (active)
                    setState({
                        transport,
                        revision,
                        version,
                        clientes,
                        armazens,
                        error: "",
                    });
            })
            .catch(() => {
                if (active && !controller.signal.aborted) {
                    controller.abort();
                    setState({
                        transport,
                        revision,
                        version,
                        clientes: [],
                        armazens: [],
                        error: "Não foi possível carregar clientes e armazéns. Atualize as opções.",
                    });
                }
            });
        return () => {
            active = false;
            controller.abort();
        };
    }, [transport, revision, version]);
    return (
        <ReferenceCatalog.Provider
            value={{
                clientes: loading ? [] : state.clientes,
                armazens: loading ? [] : state.armazens,
                loading,
                error: loading ? "" : state.error,
                refresh: () => setRevision((v) => v + 1),
            }}
        >
            {children}
        </ReferenceCatalog.Provider>
    );
}
