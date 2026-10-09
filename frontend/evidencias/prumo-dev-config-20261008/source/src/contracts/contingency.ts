import type { Field } from "./runtime";
export const contingencyTypes: Record<
    string,
    { dto: string; targets: Field[] }
> = {
    CHEGADA: {
        dto: "RecebimentoDto.RegistrarChegada",
        targets: [{ name: "pedidoId", type: "Long", required: true }],
    },
    REMANEJAMENTO: {
        dto: "EstoqueDto.Posicionar",
        targets: [{ name: "codigoUnidade", type: "UUID", required: true }],
    },
    AVARIA: {
        dto: "AvariaDto.Registrar",
        targets: [{ name: "codigoUnidade", type: "UUID", required: true }],
    },
    FATO_SERVICO: { dto: "FatoServicoDto.Registrar", targets: [] },
    CONTAGEM: { dto: "ContagemDto.Contar", targets: [] },
    RETIRADA: {
        dto: "ExpedicaoDto.Retirar",
        targets: [{ name: "pedidoId", type: "Long", required: true }],
    },
    RESERVA: {
        dto: "PedidoSaidaDto.Reservar",
        targets: [{ name: "pedidoId", type: "Long", required: true }],
    },
    SEPARACAO: {
        dto: "ExpedicaoDto.Separar",
        targets: [{ name: "pedidoId", type: "Long", required: true }],
    },
    RETORNO: {
        dto: "ExpedicaoDto.Retornar",
        targets: [{ name: "pedidoId", type: "Long", required: true }],
    },
    ENTRADA: {
        dto: "PedidoEntradaDto.Efetivar",
        targets: [
            { name: "pedidoId", type: "Long", required: true },
            { name: "identidadeExecucao", type: "String", required: true },
        ],
    },
    AJUSTE: {
        dto: "ContagemDto.Aplicar",
        targets: [{ name: "contagemId", type: "Long", required: true }],
    },
};
