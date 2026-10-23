import { useEffect, useMemo, useState, type ComponentProps } from "react";
import type { JourneyPage } from "../../components/JourneyPage";
import { PageHeader } from "../../components/layout/PageHeader";
import { Pagination } from "../../components/layout/Pagination";
import { Fields } from "../../components/Fields";
import { WorkflowReferences } from "../../components/context/WorkflowReferences";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import {
    RecordDialog,
    type RecordOpening,
} from "../../components/records/RecordDialog";
import { useRecordQuery } from "../../hooks/useRecordQuery";
import { recordPages } from "../../domain/recordPages";
import { operationContext } from "../../domain/workflow";
import { canLeavePage } from "../../domain/pageLeave";
import { isObject, type Values } from "../../contracts/runtime";
import { StatusBadge } from "../../design-system/StatusBadge";
import { Icon } from "../../design-system/Icon";
import { invoiceMoney, invoicePeriod } from "./invoicePresentation";
import { checkInvoiceVersion, invoiceRead } from "./invoiceRead";
import "./invoice.css";

type Props = ComponentProps<typeof JourneyPage>;
const noRows: Values[] = [];
type InvoiceAmount = { data?: Values; error?: string };

export function InvoiceWorkspace(props: Props) {
    const {
        journey,
        transport,
        perfil,
        context,
        workflow,
        onRecord,
        onReceipt,
    } = props;
    const query = useRecordQuery(
        "FechamentoCobrancaController.listar",
        transport,
        operationContext(
            "FechamentoCobrancaController.listar",
            context,
            workflow,
        ),
        perfil,
        onReceipt,
    );
    const page = recordPages.fechamento[0];
    const [opening, setOpening] = useState<RecordOpening | undefined>(() =>
        props.startAction && props.startAction !== page.source
            ? { action: props.startAction }
            : undefined,
    );
    const [calculationReady, setCalculationReady] = useState(
        Boolean(props.startAction === "FechamentoCobrancaController.preparar"),
    );
    const [amounts, setAmounts] = useState<{
        rows: Values[];
        values: Record<string, InvoiceAmount>;
    }>({ rows: noRows, values: {} });
    const [amountRevision, setAmountRevision] = useState(0);
    const [message, setMessage] = useState("");
    const data = query.receipt?.data;
    const rows = useMemo(
        () =>
            isObject(data) && Array.isArray(data.itens)
                ? data.itens.filter(isObject)
                : Array.isArray(data)
                  ? data.filter(isObject)
                  : noRows,
        [data],
    );
    useEffect(() => {
        if (!query.allowed || !rows.length) return;
        const abort = new AbortController();
        let current = true;
        let next = 0;
        const worker = async () => {
            while (current && next < rows.length) {
                const row = rows[next++];
                let result: InvoiceAmount;
                try {
                    const response = await invoiceRead(
                        transport,
                        "FechamentoCobrancaController.versao",
                        { id: row.id, numero: row.versaoAtual },
                        perfil,
                        abort.signal,
                    );
                    checkInvoiceVersion(response.data, row);
                    result = { data: response.data };
                } catch (error) {
                    result = {
                        error:
                            error instanceof Error
                                ? error.message
                                : "Valor indisponível.",
                    };
                }
                if (current)
                    setAmounts((previous) => ({
                        rows,
                        values: {
                            ...(previous.rows === rows ? previous.values : {}),
                            [String(row.id)]: result,
                        },
                    }));
            }
        };
        void Promise.all(
            Array.from({ length: Math.min(3, rows.length) }, worker),
        );
        return () => {
            current = false;
            abort.abort();
        };
    }, [rows, transport, perfil, query.allowed, amountRevision]);
    const actions = journey.steps
        .flatMap((step) => step.actions)
        .filter(
            (id) =>
                id !== page.source &&
                id !== page.detail &&
                id !== "FechamentoCobrancaController.preparar",
        );
    const completed: Props["onReceipt"] = (receipt, type, id, request) => {
        onReceipt(receipt, type, id, request);
        if (request.endpoint.method === "GET") return;
        if (id.startsWith("FatoServicoController.")) setCalculationReady(false);
        if (id === "CalculoCobrancaController.calcular")
            setCalculationReady(true);
        query.refresh();
        if (isObject(receipt.data) && isObject(receipt.data.fechamento)) {
            onRecord(
                receipt.data.fechamento,
                "FechamentoCobrancaDto.Fechamento",
            );
            if (isObject(receipt.data.versao))
                onRecord(receipt.data.versao, "FechamentoCobrancaDto.Versao");
            if (id === "FechamentoCobrancaController.preparar")
                setOpening({
                    row: receipt.data.fechamento,
                    type: "FechamentoCobrancaDto.Fechamento",
                });
        }
        setMessage(
            receipt.replay
                ? "Confirmação original recebida. Consulte os dados atuais da fatura."
                : receipt.ficticio
                  ? "Operação confirmada no exercício fictício."
                  : "Operação confirmada pelo servidor.",
        );
    };
    const navigate: Props["onNavigate"] = (next) => {
        if (["fechamento", "cobranca"].includes(next.page)) {
            if (canLeavePage()) {
                const row =
                    workflow.selected["FechamentoCobrancaDto.Fechamento"];
                setOpening({
                    action: next.action,
                    ...(row &&
                    next.action !== "FechamentoCobrancaController.preparar" &&
                    !next.action.startsWith("CalculoCobrancaController.")
                        ? { row, type: "FechamentoCobrancaDto.Fechamento" }
                        : {}),
                });
            }
        } else props.onNavigate(next);
    };
    return (
        <WorkflowReferences
            workflow={workflow}
            journey={journey}
            defaults={context}
        >
            <PageHeader
                title="Fechamento de Fatura"
                icon="fechamento"
                description="Consulte os fechamentos e confira os serviços cobrados."
            />
            <section
                className="workspace-panel invoice-workspace"
                aria-label="Últimos fechamentos"
            >
                <header className="invoice-heading">
                    <div>
                        <h2>Últimos fechamentos</h2>
                        <p>
                            Selecione uma fatura para consultar os valores, os
                            serviços e o histórico.
                        </p>
                    </div>
                    {query.allowed && (
                        <button
                            type="button"
                            className="primary"
                            onClick={() => {
                                setCalculationReady(false);
                                setOpening({
                                    action: "CalculoCobrancaController.calcular",
                                });
                            }}
                        >
                            <Icon name="plus" />
                            {perfil === "GESTOR"
                                ? "Novo fechamento"
                                : "Conferir serviços e valores"}
                        </button>
                    )}
                </header>
                {!query.allowed ? (
                    <p role="status">
                        Seu perfil não permite consultar faturas.
                    </p>
                ) : (
                    <>
                        <form
                            className="record-filters invoice-filters"
                            onSubmit={(event) => {
                                event.preventDefault();
                                query.search();
                            }}
                        >
                            <fieldset disabled={query.pending}>
                                <legend>Cliente e armazém</legend>
                                <Fields
                                    fields={[
                                        ...query.e.params,
                                        ...query.e.query,
                                    ].filter(
                                        (field) =>
                                            !["pagina", "tamanho"].includes(
                                                field.name,
                                            ),
                                    )}
                                    values={query.filters}
                                    onChange={query.setFilters}
                                    perfil={perfil}
                                />
                                <div className="button-row">
                                    <button type="submit" className="primary">
                                        Aplicar filtros
                                    </button>
                                    <button type="button" onClick={query.reset}>
                                        Restaurar filtros
                                    </button>
                                </div>
                            </fieldset>
                        </form>
                        <div className="invoice-list-tools">
                            <span role="status">
                                {query.pending
                                    ? "Consultando fechamentos…"
                                    : `${rows.length} nesta página`}
                            </span>
                            <button
                                type="button"
                                onClick={query.refresh}
                                disabled={query.pending}
                            >
                                <Icon name="refresh" />
                                Atualizar lista
                            </button>
                        </div>
                        {query.needsFilters && (
                            <p role="status">
                                Selecione o cliente e o armazém para consultar
                                as faturas.
                            </p>
                        )}
                        {query.error && <p role="alert">{query.error}</p>}
                        {!query.pending && query.receipt && !rows.length && (
                            <p>
                                Nenhum fechamento encontrado para os filtros
                                informados.
                            </p>
                        )}
                        {rows.length > 0 && (
                            <div className="invoice-table-scroll">
                                <table
                                    className="invoice-table"
                                    aria-label="Fechamentos · faturas"
                                >
                                    <thead>
                                        <tr>
                                            <th>Cliente</th>
                                            <th>Período de fechamento</th>
                                            <th>Valor da fatura</th>
                                            <th>
                                                <span className="sr-only">
                                                    Acessar fatura
                                                </span>
                                            </th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {rows.map((row) => {
                                            const amount =
                                                amounts.rows === rows
                                                    ? amounts.values[
                                                          String(row.id)
                                                      ]
                                                    : undefined;
                                            return (
                                                <tr
                                                    key={String(row.id)}
                                                    data-record-id={String(
                                                        row.id,
                                                    )}
                                                >
                                                    <td>
                                                        <ReferenceValue
                                                            field="clienteId"
                                                            value={
                                                                row.clienteId
                                                            }
                                                            row={row}
                                                            type="FechamentoCobrancaDto.Fechamento"
                                                        />
                                                        <div className="invoice-row-status">
                                                            <StatusBadge
                                                                value={
                                                                    row.situacao
                                                                }
                                                            />
                                                        </div>
                                                    </td>
                                                    <td>
                                                        {invoicePeriod(
                                                            row.periodoInicio,
                                                            row.periodoFim,
                                                        )}
                                                    </td>
                                                    <td>
                                                        {amount?.error ? (
                                                            <span
                                                                title={
                                                                    amount.error
                                                                }
                                                            >
                                                                Valor
                                                                indisponível{" "}
                                                                <button
                                                                    className="invoice-retry"
                                                                    type="button"
                                                                    onClick={() => {
                                                                        setAmounts(
                                                                            {
                                                                                rows: noRows,
                                                                                values: {},
                                                                            },
                                                                        );
                                                                        setAmountRevision(
                                                                            (
                                                                                value,
                                                                            ) =>
                                                                                value +
                                                                                1,
                                                                        );
                                                                    }}
                                                                >
                                                                    Consultar
                                                                    novamente
                                                                </button>
                                                            </span>
                                                        ) : amount?.data ? (
                                                            <>
                                                                {invoiceMoney(
                                                                    amount.data
                                                                        .saldo,
                                                                )}
                                                                {amount.data
                                                                    .saldo ==
                                                                    null && (
                                                                    <span>
                                                                        {" "}
                                                                        Pendente
                                                                        de
                                                                        cálculo
                                                                    </span>
                                                                )}
                                                                {amount.data
                                                                    .natureza ===
                                                                    "CREDITO" && (
                                                                    <small>
                                                                        {" "}
                                                                        ·
                                                                        Crédito
                                                                    </small>
                                                                )}
                                                            </>
                                                        ) : (
                                                            "Consultando…"
                                                        )}
                                                    </td>
                                                    <td>
                                                        <button
                                                            type="button"
                                                            className="primary"
                                                            disabled={
                                                                query.pending
                                                            }
                                                            onClick={() => {
                                                                onRecord(
                                                                    row,
                                                                    "FechamentoCobrancaDto.Fechamento",
                                                                );
                                                                setOpening({
                                                                    row,
                                                                    type: "FechamentoCobrancaDto.Fechamento",
                                                                });
                                                            }}
                                                        >
                                                            Acessar fatura
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                    </tbody>
                                </table>
                            </div>
                        )}
                        {isObject(data) && Array.isArray(data.itens) && (
                            <Pagination
                                page={Number(data.pagina)}
                                pages={Number(data.totalPaginas)}
                                total={String(data.totalItens ?? rows.length)}
                                count={rows.length}
                                size={Number(data.tamanho)}
                                disabled={query.pending}
                                onPage={query.page}
                            />
                        )}
                    </>
                )}
                {message && <p role="status">{message}</p>}
            </section>
            {opening && (
                <RecordDialog
                    key={`${opening.row?.id ?? "new"}/${opening.action ?? "detail"}`}
                    opening={opening}
                    page={page}
                    actions={
                        opening.row
                            ? actions
                            : [
                                  "CalculoCobrancaController.calcular",
                                  ...(calculationReady
                                      ? [
                                            "FechamentoCobrancaController.preparar",
                                        ]
                                      : []),
                              ]
                    }
                    transport={transport}
                    context={context}
                    workflow={
                        opening.row
                            ? workflow
                            : {
                                  ...workflow,
                                  adjustmentOrigin: undefined,
                                  selected: Object.fromEntries(
                                      Object.entries(workflow.selected).filter(
                                          ([type]) =>
                                              !type.startsWith(
                                                  "FechamentoCobrancaDto.",
                                              ) &&
                                              (opening.action !==
                                                  "CalculoCobrancaController.calcular" ||
                                                  type !==
                                                      "CalculoCobrancaDto.Resultado"),
                                      ),
                                  ),
                              }
                    }
                    perfil={perfil}
                    onClose={() => setOpening(undefined)}
                    onUnknownClose={() => setOpening(undefined)}
                    onReceipt={completed}
                    onRecord={onRecord}
                    onNavigate={navigate}
                    journey={journey}
                />
            )}
        </WorkflowReferences>
    );
}
