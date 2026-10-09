import { isObject, type Values } from "../../contracts/runtime";
export const transitions: Record<string, string> = {
    "PedidoEntradaController.iniciar": "EM_CONFERENCIA",
    "PedidoEntradaController.efetivar": "EFETIVADO",
    "PedidoEntradaController.cancelar": "CANCELADO",
    "PedidoSaidaController.reservar": "RESERVADO",
    "PedidoSaidaController.cancelar": "CANCELADO",
    "ExpedicaoController.separar": "SEPARADO",
    "ExpedicaoController.retirar": "RETIRADO",
    "ExpedicaoController.retornar": "CANCELADO",
    "CargaInicialController.preparar": "PREPARADA",
    "CargaInicialController.confirmar": "REGULARIZADA",
    "CargaInicialController.cancelar": "CANCELADA",
    "ContagemController.aplicar": "APLICADA",
    "ContingenciaController.conciliar": "CONCILIADA",
};
export function reflect(v: unknown, body: Values, op: string): unknown {
    if (Array.isArray(v)) return v.map((x) => reflect(x, body, op));
    if (!isObject(v)) return v;
    return Object.fromEntries(
        Object.entries(v).map(([k, x]) => [
            k,
            k === "operacaoId"
                ? (body.operacaoId ?? x)
                : k === "situacao" &&
                    transitions[op] &&
                    ["RASCUNHO", "PENDENTE"].includes(String(x))
                  ? transitions[op]
                  : [
                          "referencia",
                          "clienteId",
                          "armazemId",
                          "nome",
                          "descricao",
                          "produtoId",
                          "sku",
                      ].includes(k) && body[k] !== undefined
                    ? body[k]
                    : reflect(x, body, op),
        ]),
    );
}
