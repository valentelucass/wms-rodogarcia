import { useContext, useEffect, useState } from "react";
import { endpoint, isObject, type Values } from "../../contracts/runtime";
import { FormReferences } from "../FormReferences";
import {
    useReferenceTransport,
    usePresentationRecords,
    ReferenceValue,
} from "../context/ReferencePresentation";
import {
    businessTitle,
    referenceText,
    typesForReference,
} from "../../domain/referencePresentation";
import type { Transport } from "../../api/client";
const lists: Record<string, string> = {
    "ProdutoDto.Resposta": "ProdutoController.listar",
    "EmbalagemDto.Resposta": "EmbalagemController.listar",
    "EnderecoDto.Resposta": "EnderecoController.listar",
    "CapacidadeDto.Conjunto": "CapacidadeController.listar",
    "PedidoEntradaDto.Resumo": "PedidoEntradaController.listar",
    "PedidoSaidaDto.Detalhe": "PedidoSaidaController.listar",
    "RecebimentoDto.Entrada": "PedidoEntradaController.entradas",
    "UnidadeLogisticaDto.Resumo": "UnidadeLogisticaController.listar",
    "ConfiguracaoCobrancaDto.Servico":
        "ConfiguracaoCobrancaController.servicos",
    "ConfiguracaoCobrancaDto.Tabela": "ConfiguracaoCobrancaController.tabelas",
    "CalculoCobrancaDto.Resultado": "CalculoCobrancaController.listar",
    "FechamentoCobrancaDto.Fechamento": "FechamentoCobrancaController.listar",
    "FatoServicoDto.Fato": "FatoServicoController.listar",
    "CargaInicialDto.Resultado": "CargaInicialController.listar",
};
export function EntityReferenceField({
    id,
    name,
    field,
    value,
    type,
    disabled,
    required,
    onChange,
    row = {},
}: {
    id: string;
    name: string;
    field: string;
    value: unknown;
    type: string;
    disabled: boolean;
    required: boolean;
    onChange: (value: unknown) => void;
    row?: Values;
}) {
    const refs = useContext(FormReferences),
        known = usePresentationRecords(),
        transport = useReferenceTransport();
    const target = typesForReference(field, type, row)[0],
        action = lists[target];
    const [pagination, setPagination] = useState({ context: "", page: 0 }),
        [search, setSearch] = useState("");
    const [state, setState] = useState<{
        key: string;
        transport?: Transport;
        rows: Values[];
        pages: number;
        error: string;
    }>({ key: "", rows: [], pages: 0, error: "" });
    const e = action ? endpoint(action) : undefined;
    const context = { ...refs.defaults, ...row };
    const parameters = Object.fromEntries(
        (e?.params ?? []).map((f) => [
            f.name,
            context[f.name] ??
                (target === "RecebimentoDto.Entrada" && f.name === "id"
                    ? context.pedidoId
                    : undefined),
        ]),
    );
    const contextKey = JSON.stringify([
        action,
        parameters,
        (e?.query ?? [])
            .filter((f) => !["pagina", "tamanho"].includes(f.name))
            .map((f) => context[f.name]),
    ]);
    const page = pagination.context === contextKey ? pagination.page : 0;
    const query = Object.fromEntries(
        (e?.query ?? []).map((f) => [
            f.name,
            f.name === "pagina"
                ? page
                : f.name === "tamanho"
                  ? 100
                  : context[f.name],
        ]),
    );
    const available = Boolean(
        e &&
        [...e.params, ...e.query].every((f) => {
            const value = parameters[f.name] ?? query[f.name];
            return !f.required || (value != null && value !== "");
        }),
    );
    const key = JSON.stringify([action, parameters, query]);
    const options = refs.options[field] ?? [];
    const shouldRead = !disabled && !options.length && available && !!transport;
    useEffect(() => {
        if (!shouldRead || !transport || !action) return;
        const abort = new AbortController();
        const [, params, query] = JSON.parse(key) as [string, Values, Values];
        void transport
            .send({
                endpoint: endpoint(action),
                params,
                query,
                signal: abort.signal,
            })
            .then((receipt) => {
                if (abort.signal.aborted) return;
                const data = receipt.data;
                const rows = Array.isArray(data)
                    ? data.filter(isObject)
                    : isObject(data) && Array.isArray(data.itens)
                      ? data.itens.filter(isObject)
                      : [];
                setState({
                    key,
                    transport,
                    rows,
                    pages: isObject(data) ? Number(data.totalPaginas ?? 1) : 1,
                    error: "",
                });
            })
            .catch(() => {
                if (!abort.signal.aborted)
                    setState({
                        key,
                        transport,
                        rows: [],
                        pages: 0,
                        error: "Não foi possível consultar as opções. Reabra a seleção para tentar novamente.",
                    });
            });
        return () => abort.abort();
    }, [shouldRead, transport, action, key]);
    const current = state.key === key && state.transport === transport;
    // A cache used to name references is not a catalog of selectable records for another context.
    const candidates = options.length
        ? options
        : (action ? (current ? state.rows : []) : (known[target] ?? [])).map(
              (row) => ({
                  value: String(row.id),
                  title:
                      businessTitle(row, target, known) ??
                      "Referência indisponível",
              }),
          );
    const filtered = candidates.filter((o) =>
        o.title
            .toLocaleLowerCase("pt-BR")
            .includes(search.toLocaleLowerCase("pt-BR")),
    );
    const selected = candidates.find((o) => o.value === String(value));
    return (
        <div className="field entity-reference-field">
            <label htmlFor={id}>{name}</label>
            {disabled ? (
                <div id={id} className="reference-readonly">
                    <ReferenceValue
                        field={field}
                        value={value}
                        row={row}
                        type={type}
                    />
                </div>
            ) : (
                <>
                    {candidates.length > 8 && (
                        <input
                            aria-label={`Buscar ${name.replace(" *", "")}`}
                            placeholder="Buscar pelo nome ou código nesta página"
                            value={search}
                            onChange={(e) => setSearch(e.target.value)}
                        />
                    )}
                    <select
                        id={id}
                        required={required}
                        value={String(value ?? "")}
                        onChange={(e) => onChange(e.target.value)}
                    >
                        <option value="">
                            {shouldRead && !current
                                ? "Carregando opções…"
                                : "Selecione pelo nome ou código"}
                        </option>
                        {value != null &&
                            value !== "" &&
                            !filtered.some(
                                (o) => o.value === String(value),
                            ) && (
                                <option value={String(value)}>
                                    {selected?.title ??
                                        referenceText(
                                            field,
                                            value,
                                            {},
                                            type,
                                            known,
                                        ) ??
                                        "Referência atual indisponível"}
                                </option>
                            )}
                        {filtered.map((o) => (
                            <option key={o.value} value={o.value}>
                                {o.title}
                            </option>
                        ))}
                    </select>
                    {current && state.error && (
                        <span role="alert">{state.error}</span>
                    )}
                    {!candidates.length && !shouldRead && (
                        <span className="hint">
                            Selecione o contexto e consulte os registros
                            relacionados para carregar as opções.
                        </span>
                    )}
                    {current && !state.error && !candidates.length && (
                        <span className="hint">
                            Nenhum registro encontrado neste contexto.
                        </span>
                    )}
                    {current && state.pages > 1 && (
                        <div className="button-row">
                            <button
                                type="button"
                                disabled={!page}
                                onClick={() =>
                                    setPagination({
                                        context: contextKey,
                                        page: page - 1,
                                    })
                                }
                            >
                                Opções anteriores
                            </button>
                            <span>
                                Página {page + 1} de {state.pages}
                            </span>
                            <button
                                type="button"
                                disabled={page + 1 >= state.pages}
                                onClick={() =>
                                    setPagination({
                                        context: contextKey,
                                        page: page + 1,
                                    })
                                }
                            >
                                Mais opções
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
