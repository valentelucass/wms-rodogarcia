import { isObject } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
export function ExpeditionStatus({ workflow }: { workflow: Workflow }) {
    const detail = workflow.selected["ExpedicaoDto.Detalhe"];
    const order = workflow.selected["PedidoSaidaDto.Detalhe"];
    if (
        !order ||
        !detail ||
        !isObject(detail.pedido) ||
        detail.pedido.id !== order.id
    )
        return (
            <p>
                Consulte a expedição do pedido selecionado para conferir
                documentos e movimentos físicos.
            </p>
        );
    const docs = Array.isArray(detail.documentos)
        ? detail.documentos.filter(isObject)
        : [];
    return (
        <section aria-label="Fiscal e físico do pedido selecionado">
            <h2>
                Pedido {String(order.id)} · revisão {String(order.versao)}
            </h2>
            <p>
                Retirada física:{" "}
                {detail.retiradaEm
                    ? String(detail.retiradaEm)
                    : "não registrada"}
                . Documentos registrados ou cancelados são fatos distintos do
                movimento físico.
            </p>
            <ul>
                {docs.map((doc) => (
                    <li key={String(doc.id)}>
                        Documento {String(doc.id)} · {String(doc.natureza)} ·{" "}
                        {String(doc.situacao)} · Protocolo{" "}
                        {String(doc.protocolo)}
                    </li>
                ))}
            </ul>
            {workflow.followUps["PedidoEntradaController.consultar"] && (
                <p>
                    Entrada de devolução confirmada:{" "}
                    {String(
                        workflow.followUps["PedidoEntradaController.consultar"]
                            .id,
                    )}
                    . Consulte os dados desta entrada antes de continuar o
                    recebimento.
                </p>
            )}
        </section>
    );
}
