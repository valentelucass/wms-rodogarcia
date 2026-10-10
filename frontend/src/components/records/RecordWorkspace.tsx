import { useEffect, useState, type ComponentProps } from "react";
import {
    endpoint,
    isObject,
    listType,
    type Values,
} from "../../contracts/runtime";
import {
    recordPages,
    recordActionLabel,
    visibleRecordActions,
    type RecordPage,
} from "../../domain/recordPages";
import { operationContext } from "../../domain/workflow";
import { recordIdentity, recordRoot } from "../../domain/recordContext";
import { canLeavePage } from "../../domain/pageLeave";
import { useRecordQuery } from "../../hooks/useRecordQuery";
import { Fields } from "../Fields";
import { Pagination } from "../layout/Pagination";
import { OperationResults } from "../operation/OperationResults";
import { RecordTable } from "./RecordTable";
import { RecordDialog, type RecordOpening } from "./RecordDialog";
import type { JourneyPage } from "../JourneyPage";
import { ReceivingCreateDialog } from "../../modules/recebimento/ReceivingCreateDialog";
import { ReceivingList } from "../../modules/recebimento/ReceivingList";
import { Icon } from "../../design-system/Icon";
import { statusLabel } from "../../design-system/StatusBadge";

type Props = ComponentProps<typeof JourneyPage> & { step: number };
const emptyReceivingRows: Values[] = [];
export function RecordWorkspace(props: Props) {
    const definition =
        props.journey.id === "entrada"
            ? {
                  ...recordPages.entrada[0],
                  title: "Pedidos de entrada",
                  actions: [
                      ...new Set([
                          ...props.journey.steps.flatMap(
                              (step) => step.actions,
                          ),
                          "AuditoriaController.listar",
                      ]),
                  ],
              }
            : recordPages[props.journey.id][props.step];
    const [view, setView] = useState(() =>
        Math.max(
            0,
            definition.views?.findIndex(
                (v) =>
                    v.detail === props.startAction ||
                    v.actions?.includes(props.startAction),
            ) ?? 0,
        ),
    );
    return (
        <>
            {definition.views && (
                <label className="record-view-picker">
                    Tipo de cadastro
                    <select
                        value={view}
                        onChange={(event) => {
                            if (canLeavePage())
                                setView(Number(event.target.value));
                        }}
                    >
                        {definition.views.map((v, index) => (
                            <option value={index} key={v.title}>
                                {v.title}
                            </option>
                        ))}
                    </select>
                </label>
            )}
            <PageRecords
                key={view}
                {...props}
                definition={definition.views?.[view] ?? definition}
                context={{
                    ...props.context,
                    ...definition.context,
                    ...definition.views?.[view]?.context,
                }}
            />
        </>
    );
}
function PageRecords(props: Props & { definition: RecordPage }) {
    const {
        journey,
        step,
        perfil,
        transport,
        context,
        workflow,
        onRecord,
        onReceipt,
        onNavigate,
    } = props;
    const definition = props.definition;
    const current = journey.steps[step];
    const query = useRecordQuery(
        definition.source,
        transport,
        operationContext(definition.source, context, workflow),
        perfil,
        onReceipt,
    );
    const [opening, setOpening] = useState<RecordOpening | undefined>(() =>
        props.startAction && props.startAction !== definition.source
            ? {
                  action: props.startAction,
                  ...(journey.id === "entrada" &&
                  props.startAction !== "PedidoEntradaController.criar" &&
                  workflow.selected["PedidoEntradaDto.Resumo"]
                      ? {
                            row: workflow.selected["PedidoEntradaDto.Resumo"],
                            type: "PedidoEntradaDto.Resumo",
                        }
                      : {}),
              }
            : undefined,
    );
    const [creatingEntry, setCreatingEntry] = useState(false);
    const [search, setSearch] = useState("");
    const [situation, setSituation] = useState("");
    const [localPage, setLocalPage] = useState(0);
    const [message, setMessage] = useState("");
    const [confirmed, setConfirmed] = useState<RecordOpening>();
    const headers = visibleRecordActions(definition.headers, perfil);
    const actions = [
        ...(definition.actions ?? current.actions).filter(
            (id) =>
                id !== definition.source &&
                id !== definition.detail &&
                !definition.headers.includes(id),
        ),
        ...(definition.contextHeaders ?? []),
    ];
    const data = query.receipt?.data;
    const paged = isObject(data) && Array.isArray(data.itens);
    const rows = (
        paged ? data.itens : Array.isArray(data) ? data : undefined
    ) as Values[] | undefined;
    const type = paged
        ? query.e.response.slice(15, -1)
        : (listType(query.e.response) ?? query.e.response);
    const filtered = rows?.filter((row) => {
        const values = { ...row, ...recordRoot(row) };
        return (
            (!situation || String(values.situacao ?? "") === situation) &&
            (!search ||
                Object.values(values)
                    .filter(
                        (value) => !isObject(value) && !Array.isArray(value),
                    )
                    .some((value) =>
                        String(value ?? "")
                            .toLocaleLowerCase("pt-BR")
                            .includes(search.toLocaleLowerCase("pt-BR")),
                    ))
        );
    });
    const situations = [
        ...new Set(
            rows
                ?.map((row) => String(recordRoot(row).situacao ?? ""))
                .filter(Boolean),
        ),
    ];
    const maximumLocalPage = Math.max(
        0,
        Math.ceil((filtered?.length ?? 0) / 10) - 1,
    );
    useEffect(() => {
        if (localPage > maximumLocalPage) setLocalPage(maximumLocalPage);
    }, [localPage, maximumLocalPage]);
    const open = (row: Values, action?: string) => {
        onRecord(row, type);
        setOpening({ row, type, action });
    };
    const completed: Props["onReceipt"] = (
        receipt,
        responseType,
        id,
        request,
    ) => {
        if (!receipt.replay) onReceipt(receipt, responseType, id, request);
        if (request.endpoint.method === "GET") return;
        query.refresh();
        setMessage(
            receipt.replay
                ? "Confirmação original recebida. Consulte os dados atuais do registro; lista em atualização."
                : receipt.ficticio
                  ? "Operação confirmada no exercício fictício. Lista em atualização."
                  : "Operação confirmada pelo servidor. Lista em atualização.",
        );
        if (isObject(receipt.data))
            setConfirmed({ row: receipt.data, type: responseType });
    };
    return (
        <section
            className="workspace-panel record-workspace"
            aria-label={definition.title ?? current.title}
        >
            <header className="record-page-header">
                <div>
                    <h2>{definition.title ?? current.title}</h2>
                    <p>
                        {journey.id === "entrada"
                            ? "Consulte o pedido para administrar suas notas, registrar chegadas e conferir a carga antes da efetivação."
                            : current.help}
                    </p>
                </div>
                <div className="record-header-actions">
                    {headers.map((id, i) => (
                        <button
                            className={i === 0 ? "primary" : ""}
                            key={id}
                            type="button"
                            onClick={() => {
                                if (id === "PedidoEntradaController.criar")
                                    setCreatingEntry(true);
                                else setOpening({ action: id });
                            }}
                        >
                            {id === "PedidoEntradaController.criar"
                                ? "Novo pedido"
                                : recordActionLabel(id)}
                        </button>
                    ))}
                </div>
            </header>
            {!query.allowed ? (
                <p role="status">
                    Seu perfil não permite consultar esta página.
                </p>
            ) : (
                <>
                    {[...query.e.params, ...query.e.query].some(
                        (f) => !["pagina", "tamanho"].includes(f.name),
                    ) && (
                        <form
                            className="record-filters"
                            onSubmit={(event) => {
                                event.preventDefault();
                                query.search();
                            }}
                        >
                            <fieldset disabled={query.pending}>
                                <legend>Filtros da consulta</legend>
                                <Fields
                                    fields={[
                                        ...query.e.params,
                                        ...query.e.query,
                                    ].filter(
                                        (f) =>
                                            !["pagina", "tamanho"].includes(
                                                f.name,
                                            ),
                                    )}
                                    values={query.filters}
                                    onChange={query.setFilters}
                                    perfil={perfil}
                                    schema={query.e.id + ".query"}
                                />
                                <div className="actions record-filter-actions">
                                    <button
                                        className="primary"
                                        type="submit"
                                        disabled={query.pending}
                                    >
                                        Aplicar filtros
                                    </button>
                                    <button
                                        type="button"
                                        disabled={query.pending}
                                        onClick={query.reset}
                                    >
                                        Restaurar filtros
                                    </button>
                                    {journey.id === "entrada" && (
                                        <button
                                            className="record-refresh"
                                            type="button"
                                            disabled={query.pending}
                                            onClick={query.refresh}
                                        >
                                            <Icon name="refresh" />
                                            Atualizar lista
                                        </button>
                                    )}
                                </div>
                            </fieldset>
                        </form>
                    )}
                    {message && (
                        <div
                            className={`record-feedback${message.includes("desconhecido") ? " record-feedback--unknown" : ""}`}
                            role="status"
                        >
                            <p>{message}</p>
                            {confirmed?.row && (
                                <button
                                    type="button"
                                    onClick={() => setOpening(confirmed)}
                                >
                                    Ver registro confirmado
                                </button>
                            )}
                        </div>
                    )}
                    {journey.id !== "entrada" && (
                        <div className="record-toolbar">
                            <label>
                                Buscar nos registros desta página
                                <input
                                    type="search"
                                    value={search}
                                    onChange={(event) => {
                                        setSearch(event.target.value);
                                        setLocalPage(0);
                                    }}
                                />
                            </label>
                            {situations.length > 0 && (
                                <label>
                                    Situação nesta página
                                    <select
                                        value={situation}
                                        onChange={(event) => {
                                            setSituation(event.target.value);
                                            setLocalPage(0);
                                        }}
                                    >
                                        <option value="">Todas</option>
                                        {[
                                            ...new Set([
                                                ...situations,
                                                ...(situation
                                                    ? [situation]
                                                    : []),
                                            ]),
                                        ].map((state) => (
                                            <option key={state} value={state}>
                                                {statusLabel(state)}
                                            </option>
                                        ))}
                                    </select>
                                </label>
                            )}
                            <button
                                type="button"
                                disabled={query.pending}
                                onClick={query.refresh}
                            >
                                <Icon name="refresh" />
                                Atualizar lista
                            </button>
                        </div>
                    )}
                    {query.pending && (
                        <p role="status">
                            {data
                                ? "Atualizando os registros apresentados…"
                                : "Carregando registros…"}
                        </p>
                    )}
                    {query.error && (
                        <div role="alert">
                            <p>{query.error}</p>
                            <button type="button" onClick={query.refresh}>
                                Tentar novamente
                            </button>
                        </div>
                    )}
                    {query.needsFilters && !query.error && (
                        <p role="status">
                            Informe as referências obrigatórias nos filtros para
                            carregar esta visão.
                        </p>
                    )}
                    {journey.id !== "entrada" &&
                        !query.pending &&
                        rows?.length === 0 && (
                            <p className="empty">
                                {search || situation || query.hasFilters
                                    ? "Nenhum resultado para os filtros consultados nesta visão."
                                    : "Ainda não há registros nesta visão para o contexto atual."}
                            </p>
                        )}
                    {journey.id !== "entrada" &&
                        rows &&
                        rows.length > 0 &&
                        filtered?.length === 0 && (
                            <p className="empty">
                                Nenhum resultado para a busca ou situação nesta
                                página. Limpe esses filtros ou consulte outra
                                página.
                            </p>
                        )}
                    {journey.id === "entrada" && (
                        <ReceivingList
                            rows={rows ?? emptyReceivingRows}
                            loaded={rows !== undefined}
                            transport={transport}
                            perfil={perfil}
                            pending={query.pending}
                            actions={actions}
                            onOpen={open}
                        />
                    )}
                    {journey.id !== "entrada" &&
                        filtered &&
                        filtered.length > 0 && (
                            <>
                                <p className="muted">
                                    {filtered.length} de {rows!.length}{" "}
                                    registros desta resposta correspondem aos
                                    filtros de apresentação.
                                </p>
                                <RecordTable
                                    rows={
                                        paged
                                            ? filtered
                                            : filtered.slice(
                                                  localPage * 10,
                                                  (localPage + 1) * 10,
                                              )
                                    }
                                    type={type}
                                    title={definition.title ?? current.title}
                                    pending={query.pending}
                                    onOpen={open}
                                    canEdit={(row) =>
                                        visibleRecordActions(
                                            actions,
                                            perfil,
                                            recordRoot(row),
                                        ).find((id) => id.endsWith(".alterar"))
                                    }
                                    canOperate={(row) =>
                                        visibleRecordActions(
                                            actions,
                                            perfil,
                                            recordRoot(row),
                                        ).find(
                                            (id) =>
                                                endpoint(id).method !== "GET" &&
                                                !id.endsWith(".alterar"),
                                        )
                                    }
                                />
                            </>
                        )}
                    {paged && (
                        <Pagination
                            page={Number(data.pagina)}
                            pages={Number(data.totalPaginas)}
                            total={String(data.totalItens)}
                            count={rows?.length ?? 0}
                            size={Number(data.tamanho)}
                            disabled={query.pending}
                            onPage={query.page}
                        />
                    )}
                    {rows && !paged && (
                        <Pagination
                            page={localPage}
                            pages={Math.ceil((filtered?.length ?? 0) / 10)}
                            total={String(filtered?.length ?? 0)}
                            count={
                                filtered?.slice(
                                    localPage * 10,
                                    (localPage + 1) * 10,
                                ).length ?? 0
                            }
                            size={10}
                            disabled={query.pending}
                            onPage={setLocalPage}
                        />
                    )}
                    {query.receipt && !rows && (
                        <>
                            <OperationResults
                                id={query.e.id}
                                receipt={query.receipt}
                                type={type}
                                onSelect={onRecord}
                                pending={query.pending}
                                onContinue={onNavigate}
                            />
                            {isObject(data) && (
                                <button
                                    type="button"
                                    onClick={() => open(data)}
                                >
                                    Operações deste registro
                                </button>
                            )}
                            {isObject(data) &&
                                !opening &&
                                visibleRecordActions(
                                    actions,
                                    perfil,
                                    recordRoot(data),
                                ).map((id) => (
                                    <button
                                        type="button"
                                        key={id}
                                        onClick={() => open(data, id)}
                                    >
                                        {recordActionLabel(id)}
                                    </button>
                                ))}
                        </>
                    )}
                </>
            )}
            {creatingEntry && (
                <ReceivingCreateDialog
                    transport={transport}
                    onClose={() => setCreatingEntry(false)}
                    onManual={() => {
                        setCreatingEntry(false);
                        setOpening({ action: "PedidoEntradaController.criar" });
                    }}
                    onExisting={(id) => {
                        setCreatingEntry(false);
                        setOpening({
                            row: { id },
                            type: "PedidoEntradaDto.Resumo",
                        });
                    }}
                    onConfirmed={(receipt, request, data) => {
                        onReceipt(
                            receipt,
                            "NfeImportacaoDto.Confirmacao",
                            request.endpoint.id,
                            request,
                        );
                        if (data.pedido) {
                            onRecord(
                                { ...data.pedido },
                                "PedidoEntradaDto.Resumo",
                            );
                            setConfirmed({
                                row: { ...data.pedido },
                                type: "PedidoEntradaDto.Resumo",
                            });
                        }
                        setMessage(
                            receipt.ficticio
                                ? "Pedido confirmado no exercício fictício. Consulte o registro confirmado, inclusive fora dos filtros."
                                : "Pedido confirmado pelo servidor. Consulte o registro confirmado, inclusive fora dos filtros.",
                        );
                        query.refresh();
                        setCreatingEntry(false);
                    }}
                />
            )}
            {opening && (
                <RecordDialog
                    key={
                        opening.action +
                        ":" +
                        (opening.row ? recordIdentity(opening.row) : "new")
                    }
                    opening={opening}
                    page={definition}
                    actions={actions}
                    transport={transport}
                    context={context}
                    workflow={workflow}
                    perfil={perfil}
                    onClose={() => setOpening(undefined)}
                    onUnknownClose={() => {
                        setOpening(undefined);
                        setMessage(
                            "Resultado da alteração desconhecido. Consulte os dados atuais antes de iniciar outra operação.",
                        );
                        setConfirmed(undefined);
                        query.refresh();
                    }}
                    onReceipt={completed}
                    onRecord={onRecord}
                    onNavigate={onNavigate}
                    journey={journey}
                />
            )}
        </section>
    );
}
