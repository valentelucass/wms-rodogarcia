import { useContext, type ReactNode } from "react";
import { FormReferences } from "../../components/FormReferences";
import { isObject, type Values } from "../../contracts/runtime";
export function CoverageFields({
    value,
    onChange,
    disabled,
    renderRow,
}: {
    value: unknown;
    onChange: (v: unknown) => void;
    disabled: boolean;
    renderRow: (
        row: Values,
        index: number,
        change: (v: Values) => void,
    ) => ReactNode;
}) {
    const refs = useContext(FormReferences);
    const rows = Array.isArray(value) ? value.filter(isObject) : [];
    const reserves = refs.records?.["PedidoSaidaDto.Reserva"] ?? [];
    const items = refs.records?.["PedidoSaidaDto.Item"] ?? [];
    return (
        <fieldset disabled={disabled}>
            <legend>
                Cobertura de mercadoria por reserva e nota de origem
            </legend>
            <p>
                Adicione as reservas deste pedido ao documento. Quantidades e
                origens vêm da consulta; o servidor confere a cobertura
                integral. Registro fiscal não confirma retirada física.
            </p>
            {reserves.map((r) => (
                <button
                    key={String(r.id)}
                    type="button"
                    disabled={
                        disabled ||
                        rows.some(
                            (row) =>
                                row.reservaId === r.id &&
                                row.notaOrigemId === r.notaOrigemId,
                        )
                    }
                    onClick={() =>
                        onChange([
                            ...rows,
                            {
                                reservaId: r.id,
                                notaOrigemId: r.notaOrigemId,
                                sku:
                                    items.find((item) => item.id === r.itemId)
                                        ?.sku ?? "",
                                quantidade: r.quantidade,
                            },
                        ])
                    }
                >
                    Adicionar cobertura da reserva {String(r.id)} · nota{" "}
                    {String(r.notaOrigemId)}
                </button>
            ))}
            {reserves.length === 0 && (
                <p>Consulte e selecione o pedido para obter suas reservas.</p>
            )}
            {rows.map((row, i) => (
                <div key={i} className="list-item">
                    {renderRow(row, i, (next) =>
                        onChange(rows.map((old, j) => (i === j ? next : old))),
                    )}
                    <button
                        type="button"
                        onClick={() => onChange(rows.filter((_, j) => i !== j))}
                    >
                        Remover cobertura {i + 1}
                    </button>
                </div>
            ))}
        </fieldset>
    );
}
