export const calculationSample = {
    clienteId: "9007199254740993",
    armazemId: "9007199254740995",
    periodoInicio: "2026-10-15",
    periodoFim: "2026-10-16",
    motivo: "QA MANUAL - recalcular o período encerrado para conferir o ajuste identificado.\nJustificativa fictícia preservada integralmente.",
    resolucao: { tipo: "FECHAMENTO", compromissoId: "9007199254740997" },
};
export const calculationRecords = {
    "ClienteDto.Resposta": [
        {
            id: calculationSample.clienteId,
            nome: "QA MANUAL - Financeiro fictício",
            codigo: "CLI-000009",
        },
    ],
    "ArmazemDto.Resposta": [
        {
            id: calculationSample.armazemId,
            nome: "QA MANUAL - Armazém revisado",
            codigo: "ARM-000006",
        },
    ],
    "FechamentoCobrancaDto.Fechamento": [
        {
            id: calculationSample.resolucao.compromissoId,
            clienteId: calculationSample.clienteId,
            armazemId: calculationSample.armazemId,
            periodoInicio: "2026-10-14",
            periodoFim: "2026-10-15",
            situacao: "FECHADO",
            versao: "3",
        },
        {
            id: "9007199254740999",
            clienteId: calculationSample.clienteId,
            armazemId: calculationSample.armazemId,
            periodoInicio: "2026-10-15",
            periodoFim: "2026-10-16",
            situacao: "FECHADO",
            versao: "4",
        },
    ],
};
export const calculationReferences = {
    defaults: {},
    options: {
        clienteId: calculationRecords["ClienteDto.Resposta"].map((row) => ({
            value: row.id,
            title: row.nome,
        })),
        armazemId: calculationRecords["ArmazemDto.Resposta"].map((row) => ({
            value: row.id,
            title: row.nome,
        })),
        compromissoId: calculationRecords[
            "FechamentoCobrancaDto.Fechamento"
        ].map((row) => ({
            value: row.id,
            title: `Fechamento ${row.periodoInicio} / ${row.periodoFim}`,
        })),
    },
    records: calculationRecords,
};
