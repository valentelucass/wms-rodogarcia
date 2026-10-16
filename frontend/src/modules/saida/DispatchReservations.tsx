import type { Values } from "../../contracts/runtime";
import { referenceCatalog, type Workflow } from "../../domain/workflow";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { formatProductQuantity } from "../../domain/quantityPresentation";
import { information } from "../../content/information";
import { StatusBadge } from "../../design-system/StatusBadge";
import { Icon } from "../../design-system/Icon";
export function DispatchReservations({
    workflow,
    onSelect,
    disabled,
}: {
    workflow: Workflow;
    onSelect: (v: Values, type: string) => void;
    disabled: boolean;
}) {
    const order = workflow.selected["PedidoSaidaDto.Detalhe"],
        reserves = referenceCatalog(workflow, "PedidoSaidaDto.Reserva");
    if (!order || reserves.length === 0) return null;
    return (
        <section
            className="dispatch-reservations"
            aria-label="Reservas atuais do pedido"
        >
            <header className="dispatch-reservations-heading">
                <h2>{information.dispatchReservations.title}</h2>
                <span className="dispatch-reservations-order">
                    {String(order.referencia ?? "Pedido selecionado")}
                </span>
            </header>
            <p className="hint">
                {information.dispatchReservations.description}
            </p>
            <ul className="dispatch-reservations-list">
                {reserves.map((r, index) => {
                    const selected =
                        workflow.selected["PedidoSaidaDto.Reserva"]?.id ===
                        r.id;
                    const unit = r.codigoUnidade
                        ? String(r.codigoUnidade)
                        : null;
                    return (
                        <li
                            className="dispatch-reservation"
                            key={String(r.id)}
                            data-selected={selected}
                        >
                            <div className="dispatch-reservation-body">
                                <div className="dispatch-reservation-heading">
                                    <h3>Reserva {index + 1}</h3>
                                    <StatusBadge value={r.situacao} />
                                    {selected && (
                                        <span className="dispatch-reservation-selected">
                                            <Icon name="bookmark" />
                                            Reserva selecionada
                                        </span>
                                    )}
                                </div>
                                <dl className="dispatch-reservation-fields">
                                    <div className="dispatch-reservation-quantity">
                                        <dt>Quantidade reservada</dt>
                                        <dd>
                                            <strong>
                                                {formatProductQuantity(
                                                    r.quantidade,
                                                    r.unidadeMedida,
                                                )}
                                            </strong>
                                            {r.unidadeMedida != null && (
                                                <span>
                                                    {String(r.unidadeMedida)}
                                                </span>
                                            )}
                                        </dd>
                                    </div>
                                    <div>
                                        <dt>Nota de origem</dt>
                                        <dd>
                                            <ReferenceValue
                                                field="notaOrigemId"
                                                value={r.notaOrigemId}
                                                row={r}
                                            />
                                        </dd>
                                    </div>
                                    <div className="dispatch-reservation-unit">
                                        <dt>Identificador da unidade</dt>
                                        <dd>
                                            {unit ? (
                                                <code>{unit}</code>
                                            ) : (
                                                "Não informado"
                                            )}
                                        </dd>
                                    </div>
                                </dl>
                            </div>
                            <div className="dispatch-reservation-actions">
                                <button
                                    type="button"
                                    className="action-select"
                                    disabled={disabled}
                                    aria-label={
                                        unit
                                            ? `Selecionar reserva da unidade ${unit}`
                                            : `Selecionar reserva ${index + 1}`
                                    }
                                    aria-pressed={selected}
                                    onClick={() =>
                                        onSelect(r, "PedidoSaidaDto.Reserva")
                                    }
                                >
                                    <Icon name="bookmark" />
                                    Selecionar reserva
                                </button>
                            </div>
                        </li>
                    );
                })}
            </ul>
        </section>
    );
}
