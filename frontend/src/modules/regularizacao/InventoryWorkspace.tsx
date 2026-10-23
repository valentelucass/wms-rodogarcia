import { useEffect, useRef, useState, type ComponentProps } from "react";
import type { JourneyPage } from "../../components/JourneyPage";
import { PageHeader } from "../../components/layout/PageHeader";
import { Dialog } from "../../components/layout/Dialog";
import { Pagination } from "../../components/layout/Pagination";
import { Fields } from "../../components/Fields";
import { WorkflowReferences } from "../../components/context/WorkflowReferences";
import { RecordWorkspace } from "../../components/records/RecordWorkspace";
import { endpoint } from "../../contracts/runtime";
import type {
    InventarioDto_Resumo,
    InventarioDto_Visao,
    PaginaResponse,
} from "../../contracts/types";
import { StatusBadge } from "../../design-system/StatusBadge";
import { Icon } from "../../design-system/Icon";
import {
    inventoryDate,
    inventoryQuantity,
    inventoryRead,
} from "./inventoryApi";
import { InventoryDialog } from "./InventoryDialog";
import "./inventory.css";

type Props = ComponentProps<typeof JourneyPage>;
type Filters = Record<string, unknown>;
export function InventoryWorkspace(props: Props) {
    const { context, transport, perfil, workflow, journey } = props;
    const [filters, setFilters] = useState<Filters>(() => ({
        clienteId: context.clienteId ?? "",
        armazemId: context.armazemId ?? "",
        produtoId: "",
        enderecoId: "",
        pesquisa: "",
    }));
    const [applied, setApplied] = useState(filters);
    const [view, setView] = useState("SKU");
    const [page, setPage] = useState(0);
    const [revision, setRevision] = useState(0);
    const [available, setAvailable] = useState(false);
    const [availabilityMessage, setAvailabilityMessage] = useState("");
    const [availabilityRevision, setAvailabilityRevision] = useState(0);
    const [opening, setOpening] = useState<InventarioDto_Resumo | "new">();
    const [presetProduct, setPresetProduct] = useState<string>();
    const [legacy, setLegacy] = useState<number | undefined>(() =>
        props.startAction.startsWith("CargaInicialController.")
            ? 2
            : props.startAction.startsWith("ContagemController.")
              ? 1
              : undefined,
    );
    const [state, setState] = useState<{
        key?: string;
        transport?: Props["transport"];
        pending: boolean;
        error: string;
        data?:
            | PaginaResponse<InventarioDto_Visao>
            | PaginaResponse<InventarioDto_Resumo>;
    }>({ pending: false, error: "" });
    const callbacks = useRef(props);
    callbacks.current = props;
    const ready = Boolean(applied.clienteId && applied.armazemId);
    const readKey = JSON.stringify([applied, view, page, revision, available]);
    useEffect(() => {
        const abort = new AbortController();
        let current = true;
        setAvailable(false);
        setAvailabilityMessage("");
        void inventoryRead(
            transport,
            perfil,
            "InventarioController.disponibilidade",
            {},
            {},
            abort.signal,
        )
            .then((r) => {
                if (!current) return;
                setAvailable(r.data.disponivel);
                setAvailabilityMessage(r.data.mensagem ?? "");
            })
            .catch((e) => {
                if (current)
                    setAvailabilityMessage(
                        e instanceof Error
                            ? e.message
                            : "Não foi possível conferir a disponibilidade do inventário.",
                    );
            });
        return () => {
            current = false;
            abort.abort();
        };
    }, [transport, perfil, availabilityRevision]);
    useEffect(() => {
        if (!ready || opening || (view === "HISTORICO" && !available)) {
            setState({ key: readKey, transport, pending: false, error: "" });
            return;
        }
        const abort = new AbortController();
        let current = true;
        setState({ key: readKey, transport, pending: true, error: "" });
        const id =
            view === "HISTORICO"
                ? "InventarioController.listar"
                : "InventarioController.visao";
        const query =
            view === "HISTORICO"
                ? {
                      clienteId: applied.clienteId,
                      armazemId: applied.armazemId,
                      pagina: page,
                      tamanho: 20,
                  }
                : { ...applied, agrupar: view, pagina: page, tamanho: 20 };
        void inventoryRead(transport, perfil, id, {}, query, abort.signal)
            .then((r) => {
                if (!current) return;
                setState({
                    key: readKey,
                    transport,
                    pending: false,
                    error: "",
                    data: r.data,
                });
                callbacks.current.onReceipt(
                    r.receipt,
                    r.request.endpoint.response,
                    r.request.endpoint.id,
                    r.request,
                );
            })
            .catch((e) => {
                if (current)
                    setState({
                        key: readKey,
                        transport,
                        pending: false,
                        error:
                            e instanceof Error
                                ? e.message
                                : "Não foi possível consultar o inventário.",
                    });
            });
        return () => {
            current = false;
            abort.abort();
        };
    }, [
        transport,
        perfil,
        applied,
        view,
        page,
        revision,
        available,
        opening,
        ready,
        readKey,
    ]);
    const result =
        state.key === readKey && state.transport === transport
            ? state.data
            : undefined;
    const stock =
        view === "HISTORICO"
            ? []
            : ((result?.itens ?? []) as InventarioDto_Visao[]);
    const history =
        view === "HISTORICO"
            ? ((result?.itens ?? []) as InventarioDto_Resumo[])
            : [];
    function refresh() {
        setRevision((v) => v + 1);
    }
    return (
        <WorkflowReferences
            workflow={workflow}
            journey={journey}
            defaults={applied}
        >
            <PageHeader
                title="Inventário"
                icon="contagem"
                description="Consulte as mercadorias e realize a conferência por SKU, posição ou às cegas."
            />
            <section
                className="workspace-panel inventory-workspace"
                aria-label="Visão geral do inventário"
            >
                <header className="inventory-heading">
                    <div>
                        <h2>Visão geral do inventário</h2>
                        <p>
                            Confira o estoque do cliente no armazém e acompanhe
                            os inventários realizados.
                        </p>
                    </div>
                    <button
                        className="primary"
                        type="button"
                        disabled={!available || !ready}
                        onClick={() => {
                            setPresetProduct(undefined);
                            setOpening("new");
                        }}
                    >
                        <Icon name="plus" />
                        Realizar inventário
                    </button>
                </header>
                {availabilityMessage && (
                    <div className="inventory-activation" role="status">
                        <span>{availabilityMessage}</span>
                        <button
                            type="button"
                            onClick={() =>
                                setAvailabilityRevision((v) => v + 1)
                            }
                        >
                            Conferir disponibilidade
                        </button>
                    </div>
                )}
                {!opening && (
                    <>
                        <form
                            className="record-filters inventory-filters"
                            onSubmit={(e) => {
                                e.preventDefault();
                                setApplied({ ...filters });
                                setPage(0);
                                refresh();
                            }}
                        >
                            <fieldset disabled={state.pending}>
                                <legend>Filtros</legend>
                                <Fields
                                    fields={endpoint(
                                        "InventarioController.visao",
                                    ).query.filter((f) =>
                                        [
                                            "clienteId",
                                            "armazemId",
                                            "produtoId",
                                            "enderecoId",
                                        ].includes(f.name),
                                    )}
                                    values={filters}
                                    onChange={setFilters}
                                    perfil={perfil}
                                />
                                {view !== "HISTORICO" && (
                                    <label className="inventory-search">
                                        Pesquisar
                                        <input
                                            type="search"
                                            value={String(
                                                filters.pesquisa ?? "",
                                            )}
                                            onChange={(e) =>
                                                setFilters((v) => ({
                                                    ...v,
                                                    pesquisa: e.target.value,
                                                }))
                                            }
                                            placeholder="SKU ou descrição do produto"
                                            maxLength={100}
                                        />
                                    </label>
                                )}
                                <div className="button-row">
                                    <button type="submit" className="primary">
                                        Aplicar filtros
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            const initial = {
                                                clienteId:
                                                    context.clienteId ?? "",
                                                armazemId:
                                                    context.armazemId ?? "",
                                                produtoId: "",
                                                enderecoId: "",
                                                pesquisa: "",
                                            };
                                            setFilters(initial);
                                            setApplied(initial);
                                            setPage(0);
                                            refresh();
                                        }}
                                    >
                                        Restaurar filtros
                                    </button>
                                </div>
                            </fieldset>
                        </form>
                        <div className="inventory-toolbar">
                            <div
                                className="button-row"
                                aria-label="Visões do inventário"
                            >
                                {[
                                    ["SKU", "Por SKU"],
                                    ["POSICAO", "Por posição"],
                                    ["HISTORICO", "Inventários realizados"],
                                ].map(([id, title]) => (
                                    <button
                                        key={id}
                                        type="button"
                                        aria-pressed={view === id}
                                        onClick={() => {
                                            setView(id);
                                            setPage(0);
                                        }}
                                    >
                                        {title}
                                    </button>
                                ))}
                            </div>
                            <button
                                type="button"
                                disabled={state.pending}
                                onClick={refresh}
                            >
                                <Icon name="refresh" />
                                Atualizar lista
                            </button>
                        </div>
                        {!ready && (
                            <p role="status">
                                Selecione o cliente e o armazém para consultar
                                as mercadorias.
                            </p>
                        )}
                        {state.pending && (
                            <p role="status">Consultando registros…</p>
                        )}
                        {state.error && <p role="alert">{state.error}</p>}
                        {view === "HISTORICO" && !available && (
                            <p>
                                O histórico estará disponível após a ativação do
                                registro de inventários neste ambiente.
                            </p>
                        )}
                        {!state.pending &&
                            result &&
                            result.itens?.length === 0 && (
                                <p>
                                    Nenhum registro encontrado para os filtros
                                    informados.
                                </p>
                            )}
                        {result && (result.itens?.length ?? 0) > 0 && (
                            <div className="inventory-table-scroll">
                                <table
                                    className="inventory-table"
                                    aria-label={
                                        view === "HISTORICO"
                                            ? "Inventários realizados"
                                            : "Mercadorias em estoque"
                                    }
                                >
                                    <thead>
                                        {view === "HISTORICO" ? (
                                            <tr>
                                                <th>Inventário</th>
                                                <th>Tipo</th>
                                                <th>Início</th>
                                                <th>Etiquetas lidas</th>
                                                <th>Situação</th>
                                                <th>Ações</th>
                                            </tr>
                                        ) : (
                                            <tr>
                                                <th>Produto / SKU</th>
                                                {view === "POSICAO" && (
                                                    <th>Posição</th>
                                                )}
                                                <th>Unidade</th>
                                                <th>Em estoque</th>
                                                <th>Mercadorias</th>
                                                <th>Ações</th>
                                            </tr>
                                        )}
                                    </thead>
                                    <tbody>
                                        {history.map((row) => (
                                            <tr key={String(row.id)}>
                                                <td>
                                                    <strong>
                                                        #{String(row.id)}
                                                    </strong>
                                                    <small>
                                                        {row.responsavel}
                                                    </small>
                                                </td>
                                                <td>
                                                    {row.tipo === "CEGAS"
                                                        ? "Às cegas"
                                                        : row.tipo === "SKU"
                                                          ? "Por SKU"
                                                          : "Por posição"}
                                                </td>
                                                <td>
                                                    {inventoryDate(
                                                        row.iniciadoEm,
                                                    )}
                                                </td>
                                                <td>
                                                    {String(row.unidadesLidas)}
                                                </td>
                                                <td>
                                                    <StatusBadge
                                                        value={row.situacao}
                                                    />
                                                    {row.divergencias != null &&
                                                        row.divergencias >
                                                            0 && (
                                                            <small className="inventory-difference">
                                                                {
                                                                    row.divergencias
                                                                }{" "}
                                                                mercadorias com
                                                                ocorrências
                                                            </small>
                                                        )}
                                                </td>
                                                <td>
                                                    <button
                                                        type="button"
                                                        className="primary"
                                                        onClick={() =>
                                                            setOpening(row)
                                                        }
                                                    >
                                                        {row.situacao ===
                                                        "CONCLUIDO"
                                                            ? "Acessar relatório"
                                                            : row.situacao ===
                                                                "CANCELADO"
                                                              ? "Consultar inventário"
                                                              : "Continuar contagem"}
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                        {stock.map((row, index) => (
                                            <tr
                                                key={`${row.produtoId}/${row.posicoes}/${index}`}
                                            >
                                                <td>
                                                    <strong>
                                                        {row.descricao}
                                                    </strong>
                                                    <small>SKU {row.sku}</small>
                                                </td>
                                                {view === "POSICAO" && (
                                                    <td>
                                                        {row.posicoes ||
                                                            "Sem endereço"}
                                                    </td>
                                                )}
                                                <td>{row.unidadeMedida}</td>
                                                <td>
                                                    <span className="inventory-stock-quantity">
                                                        {inventoryQuantity(
                                                            row.quantidade,
                                                        )}
                                                    </span>
                                                </td>
                                                <td>
                                                    {String(row.mercadorias)}
                                                </td>
                                                <td>
                                                    <button
                                                        type="button"
                                                        disabled={!available}
                                                        onClick={() => {
                                                            setPresetProduct(
                                                                row.produtoId ??
                                                                    undefined,
                                                            );
                                                            setOpening("new");
                                                        }}
                                                    >
                                                        Inventariar
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                        {result && (
                            <Pagination
                                page={result.pagina}
                                pages={result.totalPaginas}
                                total={String(result.totalItens)}
                                count={result.itens?.length ?? 0}
                                size={result.tamanho}
                                disabled={state.pending}
                                onPage={setPage}
                            />
                        )}
                        {perfil !== "OPERACAO" && (
                            <details className="inventory-admin">
                                <summary>Ajustes e carga inicial</summary>
                                <p>
                                    Consulte as contagens anteriores ou
                                    regularize a carga inicial quando
                                    necessário.
                                </p>
                                <div className="button-row">
                                    <button
                                        type="button"
                                        onClick={() => setLegacy(1)}
                                    >
                                        Contagens e ajustes anteriores
                                    </button>
                                    <button
                                        type="button"
                                        onClick={() => setLegacy(2)}
                                    >
                                        Carga inicial excepcional
                                    </button>
                                </div>
                            </details>
                        )}
                    </>
                )}
                {opening && (
                    <p className="hint">
                        A conferência está aberta. As quantidades do estoque
                        ficam ocultas durante as leituras.
                    </p>
                )}
            </section>
            {opening && (
                <InventoryDialog
                    key={opening === "new" ? "new" : String(opening.id)}
                    initial={opening === "new" ? undefined : opening}
                    presetProduct={
                        opening === "new" ? presetProduct : undefined
                    }
                    transport={transport}
                    perfil={perfil}
                    context={applied}
                    onReceipt={props.onReceipt}
                    onChanged={refresh}
                    onClose={() => {
                        setOpening(undefined);
                        refresh();
                    }}
                />
            )}
            {legacy !== undefined && (
                <Dialog
                    title={
                        legacy === 1
                            ? "Contagens e ajustes anteriores"
                            : "Carga inicial excepcional"
                    }
                    wide
                    onClose={() => setLegacy(undefined)}
                >
                    <RecordWorkspace
                        {...props}
                        context={applied}
                        step={legacy}
                        startAction={props.startAction}
                    />
                </Dialog>
            )}
        </WorkflowReferences>
    );
}
