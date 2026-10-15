import { isLosslessNumber } from "lossless-json";
import { isObject, records, listType, type Values } from "../contracts/runtime";
import { label, display } from "../domain/labels";
import { resultName } from "../domain/resultNames";
import { Pagination } from "./layout/Pagination";
import { useContext } from "react";
import { FormReferences } from "./FormReferences";
import { ReferenceValue } from "./context/ReferencePresentation";
import {
    indexRecords,
    presentationLabel,
    technicalField,
} from "../domain/referencePresentation";
import { ConfirmedEntries } from "../modules/recebimento/ConfirmedEntries";
import { ClosureDetail } from "../modules/cadastros/ClosureDetail";
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
    if (type === "EncerramentoDto.Resultado" && isObject(data))
        return <ClosureDetail data={data} onSelect={onSelect} />;
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
            <div className="table-scroll readable-results">
                <table>
                    <caption>
                        {resultName(child)} · {data.length} registros nesta
                        resposta
                    </caption>
                    <thead>
                        <tr>
                            {columns.map((k) => (
                                <th key={k} scope="col">
                                    {presentationLabel(k)}
                                </th>
                            ))}
                            <th scope="col">Detalhe</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.map((x, i) => (
                            <tr
                                key={i}
                                data-record-id={
                                    isObject(x) && x.id != null
                                        ? String(x.id)
                                        : undefined
                                }
                            >
                                {columns.map((k) => (
                                    <td key={k}>
                                        <ReferenceValue
                                            field={k}
                                            value={isObject(x) ? x[k] : null}
                                            row={isObject(x) ? x : {}}
                                            type={child}
                                        />
                                    </td>
                                ))}
                                <td>
                                    <details>
                                        <summary>Ver registro {i + 1}</summary>
                                        <Result
                                            data={x}
                                            type={child}
                                            onSelect={onSelect}
                                            depth={depth + 1}
                                        />
                                    </details>
                                    {onSelect &&
                                        isObject(x) &&
                                        records[child] && (
                                            <button
                                                type="button"
                                                className="action-select"
                                                onClick={() =>
                                                    onSelect(x, child)
                                                }
                                            >
                                                Selecionar registro {i + 1}
                                            </button>
                                        )}
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>
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
                            <div key={k}>
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
                            />
                        </section>
                    ))}
                {onSelect && records[type] && data.id !== undefined && (
                    <div className="result-selection">
                        <button
                            type="button"
                            className="action-select"
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
