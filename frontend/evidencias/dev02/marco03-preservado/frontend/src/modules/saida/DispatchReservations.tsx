import type { Values } from "../../contracts/runtime";
import { referenceCatalog, type Workflow } from "../../domain/workflow";
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
                Reservas do pedido {String(order.id)} · revisão{" "}
                {String(order.versao)}
            </h2>
            <p>
                Selecione a reserva antes de ler a etiqueta ou registrar o
                destino. As quantidades e condições são as consultadas.
            </p>
            <ul>
                {reserves.map((r) => (
                    <li key={String(r.id)}>
                        Reserva {String(r.id)} · nota de origem{" "}
                        {String(r.notaOrigemId)} · quantidade{" "}
                        {String(r.quantidade)} · {String(r.situacao)} · UUID{" "}
                        {String(r.codigoUnidade)}{" "}
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
                            Selecionar reserva {String(r.id)} · unidade{" "}
                            {String(r.unidadeId)}
                        </button>
                    </li>
                ))}
            </ul>
        </section>
    );
}
