import { isObject, type Values } from "../../contracts/runtime";
import { recordRoot } from "../../domain/recordContext";

export const receivingSections = ["Dados gerais", "Notas e itens", "Conferência", "Histórico"] as const;
export type ReceivingSection = (typeof receivingSections)[number];
export function receivingSection(action: string): ReceivingSection {
    const command = action.split(".")[1];
    if (["nota", "xml"].includes(command)) return "Notas e itens";
    if (["iniciar", "chegada", "efetivar"].includes(command)) return "Conferência";
    if (["chegadas", "estornar", "entradas"].includes(command)) return "Histórico";
    return "Dados gerais";
}
export function receivingNotes(detail?: Values): Values[] {
    return Array.isArray(detail?.notas) ? detail.notas.filter(isObject) : [];
}
export function receivingState(detail?: Values) {
    if (!detail || !isObject(detail.pedido)) return "Não consultado";
    const situation = String(recordRoot(detail).situacao);
    if (situation === "CANCELADO") return "Cancelado";
    if (situation === "EFETIVADO") return detail.divergente ? "Conferido com divergência" : "Conferido";
    // Divergência antes de qualquer chegada é previsão ainda não conferida.
    if (!receivingNotes(detail).some((note) => note.primeiraChegada))
        return situation === "RASCUNHO" ? "Pendente" : "Em conferência";
    return detail.divergente ? "Divergente" : "Conferido";
}
export function firstArrival(detail?: Values) {
    return receivingNotes(detail)
        .map((note) => String(note.primeiraChegada ?? ""))
        .filter(Boolean)
        .sort((a, b) => Date.parse(a) - Date.parse(b))[0];
}
export function receivingDate(value: unknown) {
    if (!value) return "—";
    const date = new Date(String(value));
    return Number.isNaN(date.getTime()) ? "—" : date.toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" });
}
export function situationLabel(value: unknown) {
    return ({ RASCUNHO: "Rascunho", EM_CONFERENCIA: "Em conferência", QUARENTENA: "Quarentena", EFETIVADO: "Efetivado", CANCELADO: "Cancelado" } as Record<string, string>)[String(value)] ?? String(value ?? "—");
}
