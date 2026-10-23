import type { Transport } from "../../api/client";
import { prepareCommand } from "../../api/prepareCommand";
import {
    endpoint,
    isObject,
    type Perfil,
    type Values,
} from "../../contracts/runtime";

export async function invoiceRead(
    transport: Transport,
    id:
        | "FechamentoCobrancaController.versao"
        | "CalculoCobrancaController.consultar",
    params: Values,
    perfil: Perfil,
    signal: AbortSignal,
) {
    const request = prepareCommand({
        endpoint: endpoint(id),
        params,
        query: {},
        body: {},
        perfil,
    });
    const receipt = await transport.send({ ...request, signal });
    if (!isObject(receipt.data))
        throw new Error("A consulta não retornou os dados da fatura.");
    return { request: { ...request, signal }, receipt, data: receipt.data };
}

export function checkInvoiceVersion(data: Values, invoice: Values) {
    if (
        String(data.fechamentoId) !== String(invoice.id) ||
        String(data.numero) !== String(invoice.versaoAtual)
    )
        throw new Error(
            "A versão recebida não corresponde à fatura consultada.",
        );
}
