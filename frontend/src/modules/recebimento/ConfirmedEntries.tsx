import type { Values } from "../../contracts/runtime";
import {
    ReferenceValue,
    usePresentationRecords,
} from "../../components/context/ReferencePresentation";
import { formatProductQuantity } from "../../domain/quantityPresentation";
import { formatDate, referenceText } from "../../domain/referencePresentation";
import { information } from "../../content/information";

export function ConfirmedEntries({
    rows,
    onSelect,
}: {
    rows: Values[];
    onSelect?: (row: Values, type: string) => void;
}) {
    const refs = usePresentationRecords();
    return (
        <section className="confirmed-entries" aria-label="Entradas conferidas">
            <p className="hint">{information.confirmedEntries.description}</p>
            {rows.map((row, index) => {
                const item = refs["PedidoEntradaDto.ItemConferencia"]?.find(
                    (v) => String(v.id) === String(row.itemNotaId),
                );
                const unit =
                    item?.unidadeMedida ??
                    refs["ProdutoDto.Resposta"]?.find(
                        (v) => String(v.id) === String(row.produtoId),
                    )?.unidadeMedida;
                const title =
                    referenceText(
                        "itemNotaId",
                        row.itemNotaId,
                        row,
                        "RecebimentoDto.Entrada",
                        refs,
                    ) ?? `Entrada ${index + 1}`;
                return (
                    <article
                        className="confirmed-entry"
                        key={String(row.id ?? index)}
                    >
                        <header className="confirmed-entry-header">
                            <div>
                                <h4>
                                    <ReferenceValue
                                        field="notaId"
                                        value={row.notaId}
                                        row={row}
                                        type="RecebimentoDto.Entrada"
                                    />
                                </h4>
                                <p>
                                    <ReferenceValue
                                        field="itemNotaId"
                                        value={row.itemNotaId}
                                        row={row}
                                        type="RecebimentoDto.Entrada"
                                    />
                                </p>
                            </div>
                            <span className="confirmed-entry-state">
                                {row.unitizadaEm
                                    ? "Distribuída em unidades"
                                    : "A distribuir"}
                            </span>
                        </header>
                        <dl className="confirmed-entry-summary">
                            <div>
                                <dt>Produto / SKU</dt>
                                <dd>
                                    <ReferenceValue
                                        field="produtoId"
                                        value={row.produtoId}
                                        row={row}
                                        type="RecebimentoDto.Entrada"
                                    />
                                </dd>
                            </div>
                            <div>
                                <dt>Em triagem{unit ? ` (${unit})` : ""}</dt>
                                <dd className="quantity-cell">
                                    {formatProductQuantity(
                                        row.quantidadeTriagem,
                                        unit,
                                    )}
                                </dd>
                            </div>
                            <div>
                                <dt>Em quarentena{unit ? ` (${unit})` : ""}</dt>
                                <dd className="quantity-cell">
                                    {formatProductQuantity(
                                        row.quantidadeQuarentena,
                                        unit,
                                    )}
                                </dd>
                            </div>
                            <div>
                                <dt>Lote</dt>
                                <dd>
                                    {row.lote ? String(row.lote) : "Sem lote"}
                                </dd>
                            </div>
                        </dl>
                        <details className="confirmed-entry-dates">
                            <summary>Datas e rastreabilidade</summary>
                            <dl className="record">
                                {[
                                    ["Chegada real", row.chegadaReal],
                                    ["Data FIFO", row.dataFifo],
                                    ["Efetivação", row.efetivadaEm],
                                    ["Validade", row.validade],
                                ].map(([name, value]) => (
                                    <div key={String(name)}>
                                        <dt>{String(name)}</dt>
                                        <dd>{formatDate(value)}</dd>
                                    </div>
                                ))}
                            </dl>
                            <p className="hint">Horários de Brasília.</p>
                        </details>
                        {onSelect && (
                            <footer>
                                <button
                                    type="button"
                                    onClick={() =>
                                        onSelect(row, "RecebimentoDto.Entrada")
                                    }
                                >
                                    Selecionar {title}
                                </button>
                            </footer>
                        )}
                    </article>
                );
            })}
        </section>
    );
}
