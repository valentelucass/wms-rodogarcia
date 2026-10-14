import type { Values } from "../../contracts/runtime";
import { referenceCatalog, type Workflow } from "../../domain/workflow";
import { ReferenceValue } from "../../components/context/ReferencePresentation";
import { formatProductQuantity } from "../../domain/quantityPresentation";
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
        <section aria-label="Reservas atuais do pedido">
            <h2>
                Reservas do pedido {String(order.referencia ?? "selecionado")}
            </h2>
            <p>
                Selecione a reserva antes de ler a etiqueta ou registrar o
                destino. As quantidades e condições são as consultadas.
            </p>
            <ul>
                {reserves.map((r) => (
                    <li key={String(r.id)}>
                        Nota de origem{" "}
                        <ReferenceValue
                            field="notaOrigemId"
                            value={r.notaOrigemId}
                            row={r}
                        />{" "}
                        · quantidade{" "}
                        {formatProductQuantity(r.quantidade, r.unidadeMedida)} ·{" "}
                        {String(r.situacao)} · unidade {String(r.codigoUnidade)}{" "}
                        <button
                            type="button"
                            disabled={disabled}
                            aria-pressed={
                                workflow.selected["PedidoSaidaDto.Reserva"]
                                    ?.id === r.id
                            }
                            onClick={() =>
                                onSelect(r, "PedidoSaidaDto.Reserva")
                            }
                        >
                            Selecionar reserva da unidade{" "}
                            {String(r.codigoUnidade)}
                        </button>
                    </li>
                ))}
            </ul>
        </section>
    );
}
