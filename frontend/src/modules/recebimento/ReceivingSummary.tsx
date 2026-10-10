import { isObject, type Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
import { display } from "../../domain/labels";
export function ReceivingSummary({
    workflow,
    onSelectItem,
    disabled = false,
}: {
    workflow: Workflow;
    onSelectItem?: (note: Values, item: Values) => void;
    disabled?: boolean;
}) {
    const detail = workflow.selected["PedidoEntradaDto.Detalhe"],
        current = workflow.selected["PedidoEntradaDto.Resumo"];
    if (!detail || !isObject(detail.pedido) || detail.pedido.id !== current?.id)
        return null;
    const notes = Array.isArray(detail.notas)
        ? detail.notas.filter(isObject)
        : [];
    return (
        <section aria-label="Conferência física e notas do pedido">
            <h2>
                Conferência do pedido {String(current.id)} · consulta da revisão{" "}
                {String(detail.pedido.versao)}
            </h2>
            {detail.pedido.versao !== current.versao && (
                <p role="status">
                    A revisão atual é {String(current.versao)}. Consulte
                    novamente para reconferir quantidades.
                </p>
            )}
            <p>
                XML importado é documento de mercadoria. As quantidades físicas
                abaixo vêm da conferência retornada; importação não registra
                chegada.
            </p>
            <div className="table-scroll">
                <table>
                    <caption>
                        Previsto, recebido e diferença por item da nota
                    </caption>
                    <thead>
                        <tr>
                            <th scope="col">Nota / item / SKU</th>
                            <th scope="col">Previsto</th>
                            <th scope="col">Bom físico</th>
                            <th scope="col">Avariado físico</th>
                            <th scope="col">Diferença recebida</th>
                            {onSelectItem && <th scope="col">Ação</th>}
                        </tr>
                    </thead>
                    <tbody>
                        {notes.flatMap((note) =>
                            (Array.isArray(note.itens) ? note.itens : [])
                                .filter(isObject)
                                .map((item) => (
                                    <tr
                                        key={
                                            String(note.id) +
                                            ":" +
                                            String(item.id)
                                        }
                                    >
                                        <th scope="row">
                                            Nota {String(note.serie)}/
                                            {String(note.numero)} · item{" "}
                                            {String(item.id)} ·{" "}
                                            {String(item.sku)} ·{" "}
                                            {note.xmlVinculado
                                                ? "XML vinculado"
                                                : "nota manual"}
                                        </th>
                                        <td>{display(item.prevista)}</td>
                                        <td>{display(item.recebidaBoa)}</td>
                                        <td>
                                            {display(item.recebidaAvariada)}
                                        </td>
                                        <td>{display(item.diferenca)}</td>
                                        {onSelectItem && (
                                            <td>
                                                <button
                                                    type="button"
                                                    disabled={disabled}
                                                    onClick={() =>
                                                        onSelectItem(note, item)
                                                    }
                                                >
                                                    Selecionar item{" "}
                                                    {String(item.id)} da nota{" "}
                                                    {String(note.numero)}
                                                </button>
                                            </td>
                                        )}
                                    </tr>
                                )),
                        )}
                    </tbody>
                </table>
            </div>
        </section>
    );
}
