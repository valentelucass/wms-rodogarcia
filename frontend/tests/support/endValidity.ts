import type { FechamentoCobrancaDto_Fechamento } from "../../src/contracts/types";

export const endValiditySample = {
    id: "9007199254740993",
    tipo: "CONTRATO",
    versao: "9007199254740995",
    corte: "2026-10-16",
    resolucao: { tipo: "", compromissoId: "" },
    motivo: "Encerramento fictício da vigência.\nCompromisso histórico conferido pelo Gestor.",
};
export const endValidityCommitments = [
    {
        id: "9007199254740997",
        periodoInicio: "2026-10-14",
        periodoFim: "2026-10-15",
        situacao: "FECHADO",
    },
    {
        id: "9007199254740999",
        periodoInicio: "2026-10-15",
        periodoFim: "2026-10-16",
        situacao: "FECHADO",
    },
] satisfies Pick<
    FechamentoCobrancaDto_Fechamento,
    "id" | "periodoInicio" | "periodoFim" | "situacao"
>[];
export const endValidityReferences = {
    defaults: {},
    options: {
        compromissoId: endValidityCommitments.map((row, index) => ({
            value: row.id,
            title: `Fechamento fictício ${index + 1}`,
        })),
    },
    records: { "FechamentoCobrancaDto.Fechamento": endValidityCommitments },
};
