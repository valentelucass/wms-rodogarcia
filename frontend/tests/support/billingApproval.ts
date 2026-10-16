import { reopenCycleRecord, reopenCycleRecords } from "./reopenCycle";

export const billingApprovalAdjustment = {
    id: "9007199254741011",
    origemVersaoId: "9007199254741005",
    destinoFechamentoId: reopenCycleRecord.id,
    calculoBaseId: "9007199254741007",
    calculoCorrigidoId: "9007199254741009",
    hashCorrecao:
        "d804f54533f7046c872ad821a28e2c53090d7a1e2c33456789abcdef0123456789ab",
    valorBase: "9007199254740993.123456",
    valorCorrigido: "9007199254740993.123456",
    diferenca: "0.000000",
    situacao: "APLICADO",
    aplicadoVersaoId: "9007199254741013",
    aplicadoEm: "2026-10-16T05:06:07.123456Z",
    motivo: "QA MANUAL - conservar o ajuste de valor zero após a correção do cálculo.\nJustificativa fictícia integral.",
    evidencia:
        "Memória fictícia conferida e documento de origem preservado.\nSegunda linha da evidência.",
    usuario: "00000000-0000-4000-8000-000000000001",
    registradoEm: "2026-10-16T04:05:06.654321Z",
    tipo: "CORRECAO_CALCULO",
    tratativaOrigemId: null,
};
export const billingApprovalData = {
    fechamento: {
        ...reopenCycleRecord,
        versao: "9007199254741015",
        situacao: "APROVADO",
        versaoAtual: 3,
        contratoId: "9007199254741017",
        criadoEm: "2026-10-15T02:03:04.123456Z",
        alteradoEm: "2026-10-16T06:07:08.654321Z",
    },
    versao: {
        id: billingApprovalAdjustment.aplicadoVersaoId,
        fechamentoId: reopenCycleRecord.id,
        numero: 3,
        calculoId: "9007199254741003",
        situacao: "APROVADA",
        estadoExterno: "NAO_ENTREGUE",
        conteudoHash:
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcd",
        saldo: "9007199254740993.123456",
        natureza: "DEBITO",
        criadaEm: "2026-10-16T04:05:06.123456Z",
        decididaEm: "2026-10-16T06:07:08.654321Z",
        decisor: "00000000-0000-4000-8000-000000000001",
        motivoDecisao:
            "QA MANUAL - aprovar o ciclo integral com a versão e os ajustes consultados.\nMotivo fictício preservado.",
        entregas: [],
        confirmacoes: [],
        nfse: [],
        resolucaoFinanceira: null,
        ajustes: [billingApprovalAdjustment],
    },
};
export const billingApprovalReferences = {
    defaults: {},
    options: {},
    records: {
        ...reopenCycleRecords,
        "FechamentoCobrancaDto.Versao": [
            {
                id: billingApprovalAdjustment.origemVersaoId,
                fechamentoId: reopenCycleRecord.id,
                numero: 1,
            },
        ],
        "CalculoCobrancaDto.Resultado": [
            ...reopenCycleRecords["CalculoCobrancaDto.Resultado"],
            {
                id: billingApprovalAdjustment.calculoBaseId,
                periodoInicio: "2026-10-14",
                periodoFim: "2026-10-15",
            },
            {
                id: billingApprovalAdjustment.calculoCorrigidoId,
                periodoInicio: "2026-10-14",
                periodoFim: "2026-10-15",
            },
        ],
    },
};
