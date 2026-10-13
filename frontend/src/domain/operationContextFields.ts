import { endpoint, records, type Values } from "../contracts/runtime";

/** Referências internas obtidas do registro consultado, nunca digitadas. */
export function operationContextFields(id: string): string[] {
    if (id.startsWith("FiscalCadastroController.")) return ["id", "versao"];
    if (id === "UnidadeLogisticaController.unitizar")
        return ["pedidoId", "versaoPedido"];
    return [];
}

export function missingOperationContext(id: string, context: Values): boolean {
    const e = endpoint(id);
    const fields = [...e.params, ...(e.request ? records[e.request] : [])];
    return fields.some(
        (f) =>
            f.required &&
            operationContextFields(id).includes(f.name) &&
            (context[f.name] == null || context[f.name] === ""),
    );
}
