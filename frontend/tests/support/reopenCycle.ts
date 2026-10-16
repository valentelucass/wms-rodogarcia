import { calculationRecords } from "./calculation";

export const reopenCycleRecord = {
    ...calculationRecords["FechamentoCobrancaDto.Fechamento"][1],
    versao: "9007199254741001",
    versaoAtual: 2,
    situacao: "EM_REVISAO",
};
export const reopenCycleSample = {
    id: reopenCycleRecord.id,
    versao: reopenCycleRecord.versao,
    numero: 2,
    calculoId: "9007199254741003",
    motivo: "QA MANUAL - atualizar o ciclo com o cálculo consultado e conservar o ajuste identificado.\nJustificativa fictícia preservada integralmente.",
    resolucao: {
        tipo: "FECHAMENTO",
        compromissoId:
            calculationRecords["FechamentoCobrancaDto.Fechamento"][0].id,
    },
};
export const reopenCycleRecords = {
    ...calculationRecords,
    "CalculoCobrancaDto.Resultado": [
        {
            id: reopenCycleSample.calculoId,
            clienteId: reopenCycleRecord.clienteId,
            armazemId: reopenCycleRecord.armazemId,
            periodoInicio: "2026-10-15",
            periodoFim: "2026-10-16",
            situacao: "CALCULADO",
            total: "0.000000",
        },
    ],
};
export const reopenCycleReferences = {
    defaults: {},
    options: {
        calculoId: [
            {
                value: reopenCycleSample.calculoId,
                title: "15/10/2026 · 16/10/2026",
            },
        ],
        compromissoId: calculationRecords[
            "FechamentoCobrancaDto.Fechamento"
        ].map((row) => ({
            value: row.id,
            title: `Fechamento ${row.periodoInicio} / ${row.periodoFim}`,
        })),
    },
    records: reopenCycleRecords,
};
