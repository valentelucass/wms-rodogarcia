import { useState } from "react";
import type { DashboardDto_Produto } from "../../contracts/types";
import { barShare, formatQuantity } from "./chartFormatting";

const segments = [
    ["disponivel", "Disponível"],
    ["reservado", "Reservado"],
    ["indisponivel", "Demais indisponíveis"],
    ["pendenteUnitizacao", "Sem unitização"],
] as const;
const zero = (value: string) =>
    /^[+-]?0+(?:\.0+)?(?:[eE][+-]?\d+)?$/.test(value);
const withoutBalance = (product: DashboardDto_Produto) =>
    zero(product.fisicoTotal) &&
    segments.every(([field]) => zero(product[field]));
const identity = (product: DashboardDto_Produto) =>
    `${product.clienteId}/${product.produtoId}`;

export function ProductAvailability({
    products,
    clientName,
}: {
    products: DashboardDto_Produto[];
    clientName: (id: string) => string | undefined;
}) {
    const [selectedId, setSelectedId] = useState<string>();
    const selected =
        products.find((product) => identity(product) === selectedId) ??
        products.find((product) => !withoutBalance(product)) ??
        products[0];
    return (
        <div className="dashboard-availability">
            <ul
                className="dashboard-availability-list"
                aria-label="Saldos por produto"
            >
                {products.map((product) => {
                    const client = clientName(product.clienteId);
                    let offset = 0;
                    return (
                        <li key={identity(product)}>
                            <button
                                type="button"
                                className="dashboard-availability-row"
                                aria-pressed={selected === product}
                                aria-label={`${product.sku}${client ? ", " + client : ""}, total físico ${formatQuantity(product.fisicoTotal)} ${product.unidadeMedida}`}
                                onClick={() => setSelectedId(identity(product))}
                                onFocus={() => setSelectedId(identity(product))}
                            >
                                <span className="dashboard-availability-identity">
                                    <strong>{product.sku}</strong>
                                    {client && (
                                        <small title={client}>{client}</small>
                                    )}
                                </span>
                                {withoutBalance(product) ? (
                                    <span className="dashboard-availability-empty">
                                        Sem saldo
                                    </span>
                                ) : (
                                    <svg
                                        className="dashboard-availability-bar"
                                        viewBox="0 0 1000 8"
                                        preserveAspectRatio="none"
                                        aria-hidden="true"
                                    >
                                        <rect
                                            className="dashboard-track"
                                            width="1000"
                                            height="8"
                                            rx="4"
                                        />
                                        {segments.map(([field]) => {
                                            const width = barShare(
                                                    product[field],
                                                    product.fisicoTotal,
                                                ),
                                                x = offset;
                                            offset += width;
                                            return (
                                                <rect
                                                    key={field}
                                                    className={`dashboard-series dashboard-series--${field}`}
                                                    x={x}
                                                    width={width}
                                                    height="8"
                                                />
                                            );
                                        })}
                                    </svg>
                                )}
                                <span className="dashboard-availability-total">
                                    <strong>
                                        {formatQuantity(product.fisicoTotal)}
                                    </strong>{" "}
                                    <small>{product.unidadeMedida}</small>
                                </span>
                            </button>
                        </li>
                    );
                })}
            </ul>
            {selected && (
                <div className="dashboard-availability-detail">
                    <div className="dashboard-availability-detail-heading">
                        <strong>Composição de {selected.sku}</strong>
                        {clientName(selected.clienteId) && (
                            <span>{clientName(selected.clienteId)}</span>
                        )}
                    </div>
                    <dl>
                        {segments.map(([field, label]) => (
                            <div key={field}>
                                <dt>
                                    <i
                                        className={`dashboard-swatch dashboard-series--${field}`}
                                        aria-hidden="true"
                                    />
                                    {label}
                                </dt>
                                <dd>
                                    {formatQuantity(selected[field])}{" "}
                                    <small>{selected.unidadeMedida}</small>
                                </dd>
                            </div>
                        ))}
                    </dl>
                </div>
            )}
        </div>
    );
}
