import { isLosslessNumber } from "lossless-json";
import { isObject, records, listType, type Values } from "../contracts/runtime";
import { label, display } from "../domain/labels";
export function Result({
    data,
    onSelect,
    depth = 0,
    type = "unknown",
}: {
    data: unknown;
    onSelect?: (v: Values, type: string) => void;
    depth?: number;
    type?: string;
}) {
    if (data === null || data === undefined)
        return <p>Não informado pelo servidor.</p>;
    if (isLosslessNumber(data)) return <span>{data.value}</span>;
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
        const columns = keys.filter((k) =>
            data.some(
                (x) =>
                    isObject(x) &&
                    x[k] !== null &&
                    !isObject(x[k]) &&
                    !Array.isArray(x[k]),
            ),
        );
        const child = listType(type) ?? "unknown";
        return (
            <div className="table-scroll">
                <table>
                    <caption>{data.length} registros nesta resposta</caption>
                    <thead>
                        <tr>
                            {columns.map((k) => (
                                <th key={k} scope="col">
                                    {label(k)}
                                </th>
                            ))}
                            <th scope="col">Detalhe</th>
                        </tr>
                    </thead>
                    <tbody>
                        {data.map((x, i) => (
                            <tr key={i}>
                                {columns.map((k) => (
                                    <td key={k}>
                                        {display(isObject(x) ? x[k] : null)}
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
        const scalar = Object.entries(data).filter(
            ([, x]) => !Array.isArray(x) && !isObject(x),
        );
        const nested = Object.entries(data).filter(
            ([, x]) => Array.isArray(x) || isObject(x),
        );
        return (
            <>
                <dl className="record">
                    {scalar.map(([k, x]) => (
                        <div key={k}>
                            <dt>{label(k)}</dt>
                            <dd>{display(x)}</dd>
                        </div>
                    ))}
                </dl>
                {onSelect && records[type] && data.id !== undefined && (
                    <button type="button" onClick={() => onSelect(data, type)}>
                        Selecionar este registro
                    </button>
                )}
                {nested.map(([k, x]) => (
                    <section className="result-section" key={k}>
                        <h4>{label(k)}</h4>
                        <Result
                            data={x}
                            type={
                                type.startsWith("PaginaResponse<") &&
                                k === "itens"
                                    ? "List<" + type.slice(15, -1) + ">"
                                    : (records[type]?.find((f) => f.name === k)
                                          ?.type ?? "unknown")
                            }
                            onSelect={onSelect}
                            depth={depth + 1}
                        />
                    </section>
                ))}
            </>
        );
    }
    return <p>{display(data)}</p>;
}
