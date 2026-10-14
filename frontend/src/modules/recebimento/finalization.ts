import { isObject, type Values } from "../../contracts/runtime";

/** Ajuda de apresentação baseada na consulta; o servidor revalida ao efetivar. */
export function confirmedWithoutDivergence(
    context: Values,
    pedidoId: unknown = context.id,
    versao: unknown = context.versao,
): boolean {
    const conference = context.receivingConference;
    return (
        isObject(conference) &&
        pedidoId != null &&
        versao != null &&
        conference.id === pedidoId &&
        conference.versao === versao &&
        conference.divergente === false
    );
}
