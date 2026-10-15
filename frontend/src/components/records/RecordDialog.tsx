import { useEffect, useRef, useState, useCallback } from "react";
import type { Request, Receipt, Transport } from "../../api/client";
import {
    endpoint,
    isObject,
    type Values,
    type Perfil,
} from "../../contracts/runtime";
import { prepareCommand } from "../../api/prepareCommand";
import {
    recordIdentity,
    recordTitle,
    rowContext,
    recordRoot,
} from "../../domain/recordContext";
import {
    recordActionLabel,
    visibleRecordActions,
    type RecordPage,
} from "../../domain/recordPages";
import { absorb, operationContext, type Workflow } from "../../domain/workflow";
import { registerPageLeave } from "../../domain/pageLeave";
import { Dialog } from "../layout/Dialog";
import { StatusBadge } from "../../design-system/StatusBadge";
import { Operation } from "../Operation";
import { Result } from "../Result";
import type { NextAction, Journey } from "../../domain/journeys";
import { ReferenceLookup } from "../ReferenceLookup";
import { DispatchReservations } from "../../modules/saida/DispatchReservations";
import { AdjustmentOrigin } from "../../modules/financeiro/AdjustmentOrigin";
import { ServiceDetail } from "../../modules/financeiro/ServiceDetail";
import { AuditSelection } from "../../modules/relatorios/AuditSelection";
import { useRecordQuery } from "../../hooks/useRecordQuery";
import { ReceivingDetail } from "../../modules/recebimento/ReceivingDetail";
import { ClosureDetail } from "../../modules/cadastros/ClosureDetail";
import { Icon } from "../../design-system/Icon";
import { WorkflowReferences } from "../context/WorkflowReferences";
import {
    receivingSection,
    type ReceivingSection,
} from "../../modules/recebimento/orderPresentation";

export interface RecordOpening {
    row?: Values;
    type?: string;
    action?: string;
}
function RelatedRecords({
    id,
    transport,
    context,
    perfil,
    locked,
    onReceipt,
    onRecord,
}: {
    id: string;
    transport: Transport;
    context: Values;
    perfil: Perfil;
    locked: boolean;
    onReceipt: NonNullable<Parameters<typeof useRecordQuery>[4]>;
    onRecord: (row: Values, type: string) => void;
}) {
    const query = useRecordQuery(id, transport, context, perfil, onReceipt);
    const name = recordActionLabel(id).replace(/^Consultar\s+/i, "");
    const title = name.charAt(0).toLocaleUpperCase("pt-BR") + name.slice(1);
    return (
        <section
            className="record-related"
            aria-label={`Consulta: ${title}`}
            aria-busy={query.pending}
        >
            <header className="record-related-header">
                <h3>{title}</h3>
                <button
                    type="button"
                    className="action-refresh"
                    aria-label={`Atualizar ${name}`}
                    disabled={query.pending || locked}
                    onClick={query.refresh}
                >
                    <Icon name="refresh" />
                    Atualizar
                </button>
            </header>
            <div className="record-related-content">
                {query.pending && (
                    <p role="status">Carregando registros relacionados…</p>
                )}
                {query.error && <p role="alert">{query.error}</p>}
                {query.receipt && (
                    <Result
                        data={query.receipt.data}
                        type={query.e.response}
                        onSelect={locked ? undefined : onRecord}
                        onPage={locked ? undefined : query.page}
                        pending={query.pending}
                    />
                )}
            </div>
        </section>
    );
}
export function RecordDialog({
    opening,
    page,
    actions,
    transport,
    context,
    workflow,
    perfil,
    onClose,
    onUnknownClose,
    onReceipt,
    onRecord,
    onNavigate,
    journey,
}: {
    opening: RecordOpening;
    page: RecordPage;
    actions: string[];
    transport: Transport;
    context: Values;
    workflow: Workflow;
    perfil: Perfil;
    onClose: () => void;
    onUnknownClose: () => void;
    onReceipt: (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => void;
    onRecord: (row: Values, type: string) => void;
    onNavigate: (next: NextAction) => void;
    journey: Journey;
}) {
    const snapshot = useRef({
        context,
        workflow,
        opening,
        detail: page.detail,
        transport,
        perfil,
        onReceipt,
        onRecord,
    });
    const target = useRef(opening);
    const [detailRevision, setDetailRevision] = useState(0);
    const [refreshing, setRefreshing] = useState(false);
    const [refreshError, setRefreshError] = useState("");
    const [section, setSection] = useState<ReceivingSection>(
        receivingSection(opening.action ?? ""),
    );
    const workingWorkflow = useRef(
        opening.row
            ? absorb(workflow, opening.type ?? "unknown", opening.row, true)
            : workflow,
    );
    const [, selectionChanged] = useState(0);
    const [record, setRecord] = useState(opening.row);
    const [recordType, setRecordType] = useState(opening.type ?? "unknown");
    const [action, setAction] = useState(opening.action ?? "");
    const [completedAction, setCompletedAction] = useState("");
    const [actionRevision, setActionRevision] = useState(0);
    const [loading, setLoading] = useState(Boolean(opening.row && page.detail));
    const [error, setError] = useState("");
    const [reload, setReload] = useState(0);
    const [locked, setLocked] = useState(false);
    const [uncertain, setUncertain] = useState(false);
    const [verification, setVerification] = useState<Receipt>();
    const [verifying, setVerifying] = useState(false);
    const [verificationError, setVerificationError] = useState("");
    const edit = useRef({ dirty: false, locked: false });
    const editingChange = useCallback(
        (dirty: boolean, locked: boolean, uncertain: boolean) => {
            edit.current = { dirty, locked };
            setLocked(locked);
            setUncertain(uncertain);
        },
        [],
    );
    const canClose = useCallback(
        () =>
            !edit.current.locked &&
            (!edit.current.dirty ||
                window.confirm(
                    "Descartar as alterações não salvas deste registro?",
                )),
        [],
    );
    useEffect(() => {
        const dispose = registerPageLeave(canClose);
        const unload = (event: BeforeUnloadEvent) => {
            if (edit.current.dirty || edit.current.locked) {
                event.preventDefault();
                event.returnValue = "";
            }
        };
        window.addEventListener("beforeunload", unload);
        return () => {
            dispose();
            window.removeEventListener("beforeunload", unload);
        };
    }, [canClose]);
    useEffect(() => {
        const { detail, transport, perfil, onReceipt, onRecord } =
            snapshot.current;
        const opening = target.current;
        if (!opening.row || !detail) return;
        const abort = new AbortController();
        let active = true;
        async function read() {
            if (detailRevision) setRefreshing(true);
            else setLoading(true);
            setError("");
            setRefreshError("");
            try {
                const e = endpoint(detail!);
                const values = rowContext(
                    e.id,
                    snapshot.current.context,
                    workingWorkflow.current,
                    opening.row!,
                    opening.type!,
                );
                const request = prepareCommand({
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
                const receipt = await transport.send({
                    ...request,
                    signal: abort.signal,
                });
                if (!active) return;
                if (
                    !isObject(receipt.data) ||
                    recordIdentity(receipt.data) !==
                        recordIdentity(opening.row!)
                )
                    throw new Error(
                        "O detalhe recebido não corresponde ao registro selecionado. Consulte novamente.",
                    );
                setRecord(receipt.data);
                setRecordType(e.response);
                workingWorkflow.current = absorb(
                    workingWorkflow.current,
                    e.response,
                    receipt.data,
                    true,
                    request,
                );
                onReceipt(receipt, e.response, e.id, request);
                onRecord(receipt.data, e.response);
            } catch (err) {
                if (active)
                    (detailRevision ? setRefreshError : setError)(
                        err instanceof Error
                            ? err.message
                            : "Não foi possível carregar o registro.",
                    );
            } finally {
                if (active) {
                    setLoading(false);
                    setRefreshing(false);
                }
            }
        }
        void read();
        return () => {
            active = false;
            abort.abort();
        };
    }, [reload, detailRevision]);
    const chooseAction = (id: string) => {
        if (!canClose()) return false;
        edit.current = { dirty: false, locked: false };
        setAction(id);
        if (journey.id === "entrada") setActionRevision((value) => value + 1);
        if (journey.id === "entrada" && !id.endsWith(".consultar"))
            setSection(receivingSection(id));
        setCompletedAction("");
        return true;
    };
    const derived = record
        ? rowContext(
              action,
              snapshot.current.context,
              workingWorkflow.current,
              record,
              recordType,
              false,
          )
        : operationContext(
              action,
              snapshot.current.context,
              workingWorkflow.current,
          );
    if (
        journey.id === "entrada" &&
        record &&
        action === "AuditoriaController.listar"
    ) {
        derived.tipo = "PEDIDO_ENTRADA";
        derived.registroId = recordIdentity(record);
    }
    const available = actions.filter(
        (id) =>
            visibleRecordActions(
                [id],
                perfil,
                id.startsWith("AvariaController.")
                    ? workingWorkflow.current.selected["AvariaDto.Ocorrencia"]
                    : record
                      ? recordRoot(record)
                      : undefined,
            ).length > 0,
    );
    const contextActions = available.filter(
        (id) =>
            (id !== action || completedAction === action) &&
            (journey.id !== "entrada" || receivingSection(id) === section),
    );
    const title = record ? recordTitle(record) : "";
    const serviceDetail =
        recordType === "ConfiguracaoCobrancaDto.Servico" && !action;
    const subjectTitle =
        title === "Registro consultado" &&
        target.current.row &&
        record &&
        recordIdentity(target.current.row) === recordIdentity(record)
            ? recordTitle(target.current.row)
            : title;
    const commandReceipt = (
        r: Receipt,
        type: string,
        id: string,
        request: Pick<Request, "endpoint" | "params" | "query">,
    ) => {
        if (
            journey.id === "entrada" &&
            id === "PedidoEntradaController.consultar" &&
            isObject(r.data)
        ) {
            if (recordIdentity(r.data) !== String(request.params.id))
                throw new Error(
                    "O detalhe recebido não corresponde ao pedido consultado.",
                );
            setRecord(r.data);
            setRecordType(type);
        }
        onReceipt(r, type, id, request);
        if (r.replay) {
            edit.current.dirty = false;
            setCompletedAction(id);
            if (journey.id === "entrada" && target.current.row)
                setDetailRevision((value) => value + 1);
            return;
        }
        workingWorkflow.current = absorb(
            workingWorkflow.current,
            type,
            r.data,
            false,
            request,
        );
        if (request.endpoint.method !== "GET") {
            setCompletedAction(id);
            edit.current.dirty = false;
            const canonicalType = endpoint(page.detail ?? page.source).response;
            const candidate =
                isObject(r.data) &&
                canonicalType === "EstoqueDto.Unidade" &&
                isObject(r.data.estoque)
                    ? r.data.estoque
                    : isObject(r.data) &&
                        canonicalType === "ExpedicaoDto.Detalhe" &&
                        isObject(r.data.expedicao)
                      ? r.data.expedicao
                      : r.data;
            if (
                record &&
                isObject(candidate) &&
                (candidate !== r.data ||
                    type.split(".")[0] === recordType.split(".")[0]) &&
                recordIdentity(candidate) === recordIdentity(record)
            ) {
                setRecord(candidate);
                setRecordType(candidate !== r.data ? canonicalType : type);
            }
            if (
                journey.id === "entrada" &&
                isObject(r.data) &&
                id.startsWith("PedidoEntradaController.")
            ) {
                const root = recordRoot(r.data);
                if (
                    root.id !== undefined &&
                    (!record || recordIdentity(root) === recordIdentity(record))
                ) {
                    target.current = {
                        row: root,
                        type: "PedidoEntradaDto.Resumo",
                    };
                    if (!record) {
                        setRecord(root);
                        setRecordType("PedidoEntradaDto.Resumo");
                    }
                    setDetailRevision((value) => value + 1);
                }
            }
        }
        selectionChanged((v) => v + 1);
    };
    const selectRecord = (row: Values, type: string) => {
        if (locked) return;
        workingWorkflow.current = absorb(
            workingWorkflow.current,
            type,
            row,
            true,
        );
        onRecord(row, type);
        selectionChanged((v) => v + 1);
    };
    const verify = async () => {
        if (verifying) return;
        setVerifying(true);
        setVerificationError("");
        try {
            const e = endpoint(
                record && page.detail ? page.detail : page.source,
            );
            const values = record
                ? rowContext(
                      e.id,
                      snapshot.current.context,
                      workingWorkflow.current,
                      record,
                      recordType,
                      false,
                  )
                : operationContext(
                      e.id,
                      snapshot.current.context,
                      workingWorkflow.current,
                  );
            const request = prepareCommand({
                endpoint: e,
                perfil,
                params: Object.fromEntries(
                    e.params.map((f) => [f.name, values[f.name]]),
                ),
                query: Object.fromEntries(
                    e.query.map((f) => [
                        f.name,
                        values[f.name] ??
                            (f.name === "fuso"
                                ? "America/Sao_Paulo"
                                : f.name === "pagina"
                                  ? 0
                                  : f.name === "tamanho"
                                    ? 20
                                    : undefined),
                    ]),
                ),
                body: {},
            });
            const receipt = await transport.send({
                ...request,
                signal: new AbortController().signal,
            });
            setVerification(receipt);
        } catch (err) {
            setVerificationError(
                err instanceof Error
                    ? err.message
                    : "Falha ao consultar o estado atual.",
            );
        } finally {
            setVerifying(false);
        }
    };
    return (
        <WorkflowReferences
            workflow={workingWorkflow.current}
            journey={journey}
            defaults={derived}
        >
            <Dialog
                className={
                    "record-dialog" +
                    (serviceDetail ? " record-dialog--service" : "") +
                    (recordType === "EncerramentoDto.Resultado"
                        ? " record-dialog--closure"
                        : "") +
                    (/^EncerramentoController\.(solicitar|inativar)$/.test(
                        action,
                    )
                        ? " record-dialog--compact-action"
                        : "")
                }
                title={
                    action
                        ? recordActionLabel(action)
                        : serviceDetail
                          ? "Detalhes do serviço"
                          : recordType === "EncerramentoDto.Resultado"
                            ? "Encerramento do cadastro"
                            : subjectTitle
                }
                wide
                icon={
                    serviceDetail
                        ? "servicos"
                        : journey.id === "entrada"
                          ? "entrada"
                          : "cadastros"
                }
                locked={locked}
                onClose={() => {
                    if (canClose()) onClose();
                }}
            >
                {loading ? (
                    <p role="status">Carregando os dados atuais do registro…</p>
                ) : error ? (
                    <div role="alert">
                        <p>{error}</p>
                        <button
                            type="button"
                            onClick={() => setReload((x) => x + 1)}
                        >
                            Tentar novamente
                        </button>
                    </div>
                ) : (
                    <>
                        {record && action && (
                            <section
                                className="record-action-subject"
                                aria-label="Registro da operação"
                            >
                                <div>
                                    <span className="hint">
                                        Registro selecionado
                                    </span>
                                    <strong>{subjectTitle}</strong>
                                </div>
                                {recordRoot(record).situacao != null && (
                                    <StatusBadge
                                        value={recordRoot(record).situacao}
                                    />
                                )}
                            </section>
                        )}
                        {refreshing && (
                            <p role="status">
                                Atualizando notas, itens e conferência deste
                                pedido…
                            </p>
                        )}
                        {refreshError && (
                            <div role="alert">
                                <p>{refreshError}</p>
                                <button
                                    type="button"
                                    onClick={() =>
                                        setDetailRevision((value) => value + 1)
                                    }
                                >
                                    Consultar pedido atualizado novamente
                                </button>
                            </div>
                        )}
                        {action && (
                            <>
                                <ReferenceLookup
                                    inline
                                    journey={journey}
                                    context={derived}
                                    transport={transport}
                                    onRecord={selectRecord}
                                    selectionLocked={locked}
                                    onReceipt={commandReceipt}
                                />
                                {["saida", "fiscal"].includes(journey.id) && (
                                    <DispatchReservations
                                        workflow={workingWorkflow.current}
                                        onSelect={selectRecord}
                                        disabled={locked}
                                    />
                                )}
                                {journey.id === "fechamento" && (
                                    <AdjustmentOrigin
                                        workflow={workingWorkflow.current}
                                        onPin={selectRecord}
                                        disabled={locked}
                                    />
                                )}
                                {["precos", "relatorios"].includes(
                                    journey.id,
                                ) &&
                                    action === "AuditoriaController.listar" &&
                                    perfil === "GESTOR" && (
                                        <AuditSelection
                                            workflow={workingWorkflow.current}
                                            onSelect={selectRecord}
                                            disabled={locked}
                                        />
                                    )}
                            </>
                        )}
                        {record && journey.id === "entrada" && (
                            <ReceivingDetail
                                transport={transport}
                                record={record}
                                section={section}
                                onSection={(next) => {
                                    if (canClose()) {
                                        setSection(next);
                                        setAction("");
                                        edit.current.dirty = false;
                                    }
                                }}
                                onSelect={selectRecord}
                                locked={locked || refreshing}
                                workflow={workingWorkflow.current}
                            />
                        )}
                        {record && !action && journey.id !== "entrada" && (
                            <section
                                className="record-detail"
                                aria-label="Dados do registro"
                            >
                                {serviceDetail ? (
                                    <ServiceDetail data={record} />
                                ) : recordType ===
                                  "EncerramentoDto.Resultado" ? (
                                    <ClosureDetail
                                        data={record}
                                        subjectTitle={subjectTitle}
                                        onSelect={selectRecord}
                                    />
                                ) : (
                                    <Result
                                        data={record}
                                        type={recordType}
                                        onSelect={selectRecord}
                                    />
                                )}
                            </section>
                        )}
                        {record && Boolean(page.related?.length) && (
                            <div className="record-related-list">
                                {page.related?.map((id) => (
                                    <RelatedRecords
                                        key={id}
                                        id={id}
                                        transport={transport}
                                        perfil={perfil}
                                        context={rowContext(
                                            id,
                                            snapshot.current.context,
                                            workingWorkflow.current,
                                            record,
                                            recordType,
                                            false,
                                        )}
                                        locked={locked}
                                        onReceipt={commandReceipt}
                                        onRecord={selectRecord}
                                    />
                                ))}
                            </div>
                        )}
                        {action &&
                            (!opening.row ||
                                available.includes(action) ||
                                completedAction === action ||
                                endpoint(action).method === "GET") && (
                                <Operation
                                    key={
                                        action +
                                        ":" +
                                        reload +
                                        ":" +
                                        actionRevision
                                    }
                                    id={action}
                                    transport={transport}
                                    context={derived}
                                    perfil={perfil}
                                    inDialog
                                    hideHeading
                                    fixedFields={
                                        opening.row ||
                                        (journey.id === "entrada" && record)
                                            ? [
                                                  "id",
                                                  ...(action ===
                                                  "ConfiguracaoCobrancaController.alterar"
                                                      ? []
                                                      : [
                                                            "clienteId",
                                                            "armazemId",
                                                        ]),
                                                  "pedidoId",
                                                  "unidadeId",
                                                  ...(action ===
                                                  "EnderecoController.alterar"
                                                      ? []
                                                      : ["codigo"]),
                                                  "versao",
                                                  "versaoPedido",
                                                  "versaoDestino",
                                                  ...(journey.id ===
                                                      "entrada" ||
                                                  action.startsWith(
                                                      "EncerramentoController.",
                                                  )
                                                      ? ["tipo", "registroId"]
                                                      : []),
                                              ].filter(
                                                  (key) =>
                                                      derived[key] !==
                                                          undefined &&
                                                      derived[key] !== "",
                                              )
                                            : []
                                    }
                                    onEditingChange={editingChange}
                                    onReceipt={commandReceipt}
                                    onReplayReceipt={commandReceipt}
                                    onSelect={selectRecord}
                                    onContinue={(next) => {
                                        if (canClose()) {
                                            if (
                                                journey.id === "entrada" &&
                                                next.page === "entrada"
                                            )
                                                chooseAction(next.action);
                                            else onNavigate(next);
                                        }
                                    }}
                                />
                            )}
                        {uncertain && (
                            <section aria-label="Recuperação da operação">
                                <p>
                                    A resposta da operação é desconhecida.
                                    Consulte o estado atual; a consulta não
                                    confirma o resultado nem repete a alteração.
                                    A repetição mantém o mesmo conteúdo e
                                    identificador de operação.
                                </p>
                                <button
                                    type="button"
                                    className="action-refresh"
                                    disabled={verifying}
                                    onClick={() => void verify()}
                                >
                                    Consultar estado atual sem repetir
                                </button>
                                {verificationError && (
                                    <p role="alert">{verificationError}</p>
                                )}
                                {verification && (
                                    <Result
                                        data={verification.data}
                                        type={
                                            endpoint(
                                                record && page.detail
                                                    ? page.detail
                                                    : page.source,
                                            ).response
                                        }
                                    />
                                )}
                                {verification && (
                                    <button
                                        type="button"
                                        className="action-dismiss"
                                        onClick={() => {
                                            if (
                                                window.confirm(
                                                    "Encerrar o acompanhamento sem declarar sucesso? O resultado da alteração permanece desconhecido; consulte o registro antes de iniciar outra ação.",
                                                )
                                            )
                                                onUnknownClose();
                                        }}
                                    >
                                        Voltar à lista mantendo resultado
                                        desconhecido
                                    </button>
                                )}
                            </section>
                        )}
                        {action &&
                            opening.row &&
                            !available.includes(action) &&
                            completedAction !== action &&
                            endpoint(action).method !== "GET" && (
                                <p role="status">
                                    Esta ação não está disponível para a
                                    situação ou permissão atual do registro.
                                    Recarregue os dados para conferir.
                                </p>
                            )}
                        {record && contextActions.length > 0 && (
                            <details
                                className="record-other-operations"
                                open={
                                    !action &&
                                    recordType !== "EncerramentoDto.Resultado"
                                }
                            >
                                <summary>
                                    {action
                                        ? "Outras operações"
                                        : "Operações deste registro"}
                                    <span className="record-operation-count">
                                        {contextActions.length}
                                    </span>
                                </summary>
                                <div
                                    className="record-context-actions"
                                    role="group"
                                    aria-label="Operações deste registro"
                                >
                                    {contextActions.map((id) => (
                                        <button
                                            key={id}
                                            type="button"
                                            disabled={
                                                locked ||
                                                (journey.id === "entrada" &&
                                                    (refreshing ||
                                                        Boolean(refreshError)))
                                            }
                                            onClick={(event) => {
                                                const dialog =
                                                    event.currentTarget.closest(
                                                        "dialog",
                                                    );
                                                if (!chooseAction(id)) return;
                                                event.currentTarget
                                                    .closest("details")
                                                    ?.removeAttribute("open");
                                                requestAnimationFrame(() =>
                                                    dialog
                                                        ?.querySelector<HTMLElement>(
                                                            '.operation-fields :is(input:not([type="hidden"]), select, textarea):not(:disabled)',
                                                        )
                                                        ?.focus(),
                                                );
                                            }}
                                        >
                                            {recordActionLabel(id)}
                                        </button>
                                    ))}
                                </div>
                            </details>
                        )}
                        <div className="actions record-dialog-footer">
                            <button
                                type="button"
                                className="action-dismiss"
                                disabled={locked}
                                onClick={() => {
                                    if (canClose()) onClose();
                                }}
                            >
                                {action ? "Voltar à lista" : "Fechar detalhes"}
                            </button>
                            {serviceDetail && record?.id !== undefined && (
                                <button
                                    type="button"
                                    className="action-select"
                                    disabled={locked}
                                    onClick={() => {
                                        if (!canClose()) return;
                                        selectRecord(record, recordType);
                                        onClose();
                                    }}
                                >
                                    Selecionar este registro
                                </button>
                            )}
                            {record && action && (
                                <button
                                    type="button"
                                    className="action-details"
                                    disabled={locked}
                                    onClick={() => {
                                        if (canClose()) {
                                            setAction("");
                                            edit.current.dirty = false;
                                        }
                                    }}
                                >
                                    Ver detalhes do registro
                                </button>
                            )}
                            {record && page.detail && !locked && (
                                <button
                                    type="button"
                                    className="action-refresh"
                                    onClick={() => {
                                        if (canClose()) {
                                            setAction("");
                                            edit.current.dirty = false;
                                            setReload((x) => x + 1);
                                        }
                                    }}
                                >
                                    Recarregar dados atuais
                                </button>
                            )}
                        </div>
                    </>
                )}
            </Dialog>
        </WorkflowReferences>
    );
}
