import { isLosslessNumber } from "lossless-json";
import { isObject, records, listType, type Values } from "../contracts/runtime";
import { label, display } from "../domain/labels";
import { resultName } from "../domain/resultNames";
import { Pagination } from "./layout/Pagination";
import { Fragment, useContext, useId, useState } from "react";
import { Icon } from "../design-system/Icon";
import { FormReferences } from "./FormReferences";
import { ReferenceValue } from "./context/ReferencePresentation";
import {
    indexRecords,
    presentationLabel,
    technicalField,
} from "../domain/referencePresentation";
import { ConfirmedEntries } from "../modules/recebimento/ConfirmedEntries";
import { ClosureDetail } from "../modules/cadastros/ClosureDetail";
import { InitialLoadDetail } from "../modules/regularizacao/InitialLoadDetail";
import { AuditDetail } from "../modules/relatorios/AuditDetail";
import { ServiceFactDetail } from "../modules/financeiro/ServiceFactDetail";
import { BillingAdjustments } from "../modules/financeiro/BillingAdjustments";
export function Result(props: Parameters<typeof ResultContent>[0]) {
    const parent = useContext(FormReferences);
    const indexed = indexRecords(
        props.data,
        props.type ?? "unknown",
        Object.fromEntries(
            Object.entries(parent.records ?? {}).map(([k, v]) => [k, [...v]]),
        ),
    );
    return (
        <FormReferences.Provider value={{ ...parent, records: indexed }}>
            <ResultContent {...props} />
        </FormReferences.Provider>
    );
}
function ResultContent({
    data,
    onSelect,
    depth = 0,
    type = "unknown",
    onPage,
    pending = false,
}: {
    data: unknown;
    onSelect?: (v: Values, type: string) => void;
    depth?: number;
    type?: string;
    onPage?: (page: number) => void;
    pending?: boolean;
}) {
    if (data === null || data === undefined)
        return <p>Não informado pelo servidor.</p>;
    if (isLosslessNumber(data)) return <span>{data.value}</span>;
    if (
        type === "List<FechamentoCobrancaDto.Ajuste>" &&
        Array.isArray(data) &&
        data.every(isObject)
    )
        return (
            <BillingAdjustments
                rows={data}
                onSelect={onSelect}
                pending={pending}
            />
        );
    if (type === "FechamentoCobrancaDto.Ajuste" && isObject(data))
        return (
            <BillingAdjustments
                rows={[data]}
                onSelect={onSelect}
                pending={pending}
            />
        );
    if (type === "FatoServicoDto.Fato" && isObject(data))
        return (
            <div className="service-fact-result">
                <ServiceFactDetail data={data} />
                {onSelect && data.id !== undefined && (
                    <div className="result-selection">
                        <button
                            type="button"
                            className="action-select"
                            disabled={pending}
                            onClick={() => onSelect(data, type)}
                        >
                            Selecionar este registro
                        </button>
                    </div>
                )}
            </div>
        );
    if (type === "AuditoriaResponse" && isObject(data))
        return (
            <div className="audit-result">
                <AuditDetail data={data} />
                {onSelect && data.id !== undefined && (
                    <div className="result-selection">
                        <button
                            type="button"
                            className="action-select"
                            disabled={pending}
                            onClick={() => onSelect(data, type)}
                        >
                            Selecionar este registro
                        </button>
                    </div>
                )}
            </div>
        );
    if (type === "EncerramentoDto.Resultado" && isObject(data))
        return <ClosureDetail data={data} onSelect={onSelect} />;
    if (type === "CargaInicialDto.Resultado" && isObject(data))
        return (
            <div className="initial-load-result">
                <InitialLoadDetail data={data} />
                {onSelect && data.id !== undefined && (
                    <div className="result-selection">
                        <button
                            type="button"
                            className="action-select"
                            disabled={pending}
                            onClick={() => onSelect(data, type)}
                        >
                            Selecionar esta carga
                        </button>
                    </div>
                )}
            </div>
        );
    if (Array.isArray(data)) {
        if (data.length === 0)
            return <p className="empty">Nenhum registro encontrado.</p>;
        if (!isObject(data[0]))
            return (
                <ul>
                    {data.map((x, i) => (
                        <li key={i}>{display(x)}</li>
                    ))}
                </ul>
            );
        const keys = [
            ...new Set(
                data.flatMap((x) => (isObject(x) ? Object.keys(x) : [])),
            ),
        ];
        const columns = keys.filter(
            (k) =>
                k !== "id" &&
                !technicalField(k) &&
                data.some(
                    (x) =>
                        isObject(x) &&
                        x[k] !== null &&
                        !isObject(x[k]) &&
                        !Array.isArray(x[k]),
                ),
        );
        const child = listType(type) ?? "unknown";
        if (child === "RecebimentoDto.Entrada")
            return (
                <ConfirmedEntries
                    rows={data.filter(isObject)}
                    onSelect={onSelect}
                />
            );
        return (
            <ResultTable
                data={data}
                columns={columns}
                type={child}
                depth={depth}
                pending={pending}
                onSelect={onSelect}
            />
        );
    }
    if (isObject(data)) {
        if (Object.keys(data).length === 0)
            return <p className="empty">Nenhum dado informado.</p>;
        if (type.startsWith("PaginaResponse<") && Array.isArray(data.itens)) {
            return (
                <div className="paged-results" aria-busy={pending}>
                    <Result
                        data={data.itens}
                        type={"List<" + type.slice(15, -1) + ">"}
                        onSelect={onSelect}
                        depth={depth + 1}
                        pending={pending}
                    />
                    {(data.itens.length > 0 ||
                        Number(data.totalItens) > 0 ||
                        Number(data.totalPaginas) > 1) && (
                        <Pagination
                            page={Number(data.pagina)}
                            pages={Number(data.totalPaginas)}
                            total={String(data.totalItens)}
                            count={data.itens.length}
                            size={Number(data.tamanho)}
                            disabled={pending}
                            onPage={onPage}
                        />
                    )}
                </div>
            );
        }
        const scalar = Object.entries(data).filter(
            ([k, x]) =>
                k !== "id" &&
                !technicalField(k) &&
                !Array.isArray(x) &&
                !isObject(x),
        );
        const nested = Object.entries(data).filter(
            ([, x]) => Array.isArray(x) || isObject(x),
        );
        const emptyNested = nested.filter(
            ([, x]) => Object.keys(x as object).length === 0,
        );
        return (
            <div className="result-record">
                {(scalar.length > 0 || emptyNested.length > 0) && (
                    <dl
                        className={
                            "record result-fields" +
                            (nested.length > emptyNested.length
                                ? " result-fields--summary"
                                : "")
                        }
                    >
                        {scalar.map(([k, x]) => (
                            <div key={k} data-field={k}>
                                <dt>{presentationLabel(k)}</dt>
                                <dd>
                                    <ReferenceValue
                                        field={k}
                                        value={x}
                                        row={data}
                                        type={type}
                                    />
                                </dd>
                            </div>
                        ))}
                        {emptyNested.map(([k]) => (
                            <div key={k}>
                                <dt>{label(k)}</dt>
                                <dd className="hint">
                                    Nenhum registro informado.
                                </dd>
                            </div>
                        ))}
                    </dl>
                )}
                {nested
                    .filter((entry) => !emptyNested.includes(entry))
                    .map(([k, x]) => (
                        <section className="result-section" key={k}>
                            <h4>{label(k)}</h4>
                            <Result
                                data={x}
                                type={
                                    type.startsWith("PaginaResponse<") &&
                                    k === "itens"
                                        ? "List<" + type.slice(15, -1) + ">"
                                        : (records[type]?.find(
                                              (f) => f.name === k,
                                          )?.type ?? "unknown")
                                }
                                onSelect={onSelect}
                                depth={depth + 1}
                                pending={pending}
                            />
                        </section>
                    ))}
                {onSelect && records[type] && data.id !== undefined && (
                    <div className="result-selection">
                        <button
                            type="button"
                            className="action-select"
                            disabled={pending}
                            onClick={() => onSelect(data, type)}
                        >
                            Selecionar este registro
                        </button>
                    </div>
                )}
            </div>
        );
    }
    return <p>{display(data)}</p>;
}

function ResultTable({
    data,
    columns,
    type,
    depth,
    pending,
    onSelect,
}: {
    data: unknown[];
    columns: string[];
    type: string;
    depth: number;
    pending: boolean;
    onSelect?: (row: Values, type: string) => void;
}) {
    const id = useId();
    const [expanded, setExpanded] = useState<{
        data: unknown[];
        index: number;
    } | null>(null);
    const notes = type === "PedidoEntradaDto.Nota";
    return (
        <div
            className={
                "table-scroll readable-results result-table-scroll" +
                (notes ? " note-results" : "")
            }
            aria-busy={pending}
        >
            <table>
                <caption>
                    {resultName(type)} · {data.length}{" "}
                    {notes
                        ? data.length === 1
                            ? "nota"
                            : "notas"
                        : data.length === 1
                          ? "registro"
                          : "registros"}{" "}
                    nesta resposta
                </caption>
                <thead>
                    <tr>
                        {columns.map((key) => (
                            <th key={key} scope="col">
                                {presentationLabel(key)}
                            </th>
                        ))}
                        <th scope="col">Ações</th>
                    </tr>
                </thead>
                <tbody>
                    {data.map((row, index) => {
                        const open =
                            expanded?.data === data && expanded.index === index;
                        const triggerId = `${id}-trigger-${index}`,
                            panelId = `${id}-panel-${index}`;
                        const noteIdentity = `${isObject(row) && row.numero != null ? String(row.numero) : index + 1}, série ${isObject(row) && row.serie != null ? String(row.serie) : "não informada"}`;
                        const panelTitle = notes
                            ? `Detalhes da nota ${noteIdentity}`
                            : `Detalhes do registro ${index + 1}`;
                        const close = () => {
                            setExpanded(null);
                            document.getElementById(triggerId)?.focus();
                        };
                        return (
                            <Fragment key={index}>
                                <tr
                                    className="result-data-row"
                                    data-expanded={open}
                                    data-record-id={
                                        isObject(row) && row.id != null
                                            ? String(row.id)
                                            : undefined
                                    }
                                >
                                    {columns.map((key) => (
                                        <td key={key}>
                                            <ReferenceValue
                                                compact
                                                field={key}
                                                value={
                                                    isObject(row)
                                                        ? row[key]
                                                        : null
                                                }
                                                row={isObject(row) ? row : {}}
                                                type={type}
                                            />
                                        </td>
                                    ))}
                                    <td
                                        className={
                                            "result-row-actions" +
                                            (notes
                                                ? " note-result-actions"
                                                : "")
                                        }
                                    >
                                        <button
                                            id={triggerId}
                                            type="button"
                                            className="action-details result-detail-toggle"
                                            aria-expanded={open}
                                            aria-controls={
                                                open ? panelId : undefined
                                            }
                                            aria-label={
                                                notes
                                                    ? `Ver detalhes da nota ${noteIdentity}`
                                                    : undefined
                                            }
                                            onClick={() =>
                                                setExpanded(
                                                    open
                                                        ? null
                                                        : { data, index },
                                                )
                                            }
                                        >
                                            <Icon
                                                name={
                                                    open
                                                        ? "chevron-down"
                                                        : "chevron-right"
                                                }
                                            />
                                            {notes
                                                ? "Ver detalhes"
                                                : `Ver registro ${index + 1}`}
                                        </button>
                                        {onSelect &&
                                            isObject(row) &&
                                            records[type] && (
                                                <button
                                                    type="button"
                                                    className="action-select"
                                                    disabled={pending}
                                                    aria-label={
                                                        notes
                                                            ? `Selecionar nota ${noteIdentity}`
                                                            : undefined
                                                    }
                                                    onClick={() =>
                                                        onSelect(row, type)
                                                    }
                                                >
                                                    {notes
                                                        ? "Selecionar nota"
                                                        : `Selecionar registro ${index + 1}`}
                                                </button>
                                            )}
                                    </td>
                                </tr>
                                {open && (
                                    <tr className="result-detail-row">
                                        <td colSpan={columns.length + 1}>
                                            <section
                                                id={panelId}
                                                className="result-row-detail"
                                                aria-label={panelTitle}
                                            >
                                                <header className="result-row-detail-header">
                                                    <h4>{panelTitle}</h4>
                                                    <button
                                                        type="button"
                                                        className="action-dismiss"
                                                        aria-label={`Fechar ${panelTitle.charAt(0).toLowerCase() + panelTitle.slice(1)}`}
                                                        onClick={close}
                                                    >
                                                        <Icon name="close" />
                                                        Fechar detalhes
                                                    </button>
                                                </header>
                                                <Result
                                                    data={row}
                                                    type={type}
                                                    onSelect={onSelect}
                                                    depth={depth + 1}
                                                    pending={pending}
                                                />
                                            </section>
                                        </td>
                                    </tr>
                                )}
                            </Fragment>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
}
