import { isObject } from "../../contracts/runtime";
import type { NextAction } from "../../domain/journeyTypes";
import type { Receipt } from "../../api/client";
export function dispatchContinuation(
    id: string,
    receipt: Receipt,
    fallback: NextAction | undefined,
): NextAction | undefined {
    const data = receipt.data;
    if (
        id === "PedidoSaidaController.reservar" &&
        isObject(data) &&
        isObject(data.pedido) &&
        data.pedido.id
    )
        return {
            page: "coletor",
            action: `pedido:${data.pedido.id}`,
            title: "Abrir tarefa no Coletor",
        };
    if (
        id === "ExpedicaoController.separar" &&
        isObject(data) &&
        isObject(data.expedicao) &&
        isObject(data.expedicao.pedido) &&
        data.expedicao.pedido.situacao !== "SEPARADO"
    )
        return {
            page: "coletor",
            action: "ExpedicaoController.ler",
            title: "Selecionar e ler outra reserva deste pedido",
        };
    return fallback;
}
