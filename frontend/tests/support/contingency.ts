import type { Values } from "../../src/contracts/runtime";
import { initialLoadReferences } from "./initialLoad";

export const contingencyCountCommand = {
    identidadeFato: "PLAN-CONTAGEM-ALFA-01",
    clienteId: "9007199254741001",
    armazemId: "9007199254741003",
    tipo: "CONTAGEM",
    ocorridaEm: "2026-10-15T16:20:30.123456Z",
    operador: "Operador Alfa fictício",
    fonte: "Planilha inteiramente fictícia para conferir o formulário.",
    efeitoRegistradoNoWms: false,
    dependencias: [] as string[],
    dados: {
        dados: {
            codigoUnidade: "00000000-0000-4000-8000-000000000003",
            versaoUnidade: "9007199254740995",
            contado: "3.000001",
            observadoEm: "2026-10-15T15:20:30.987654Z",
            motivo: "Contagem fictícia observada no armazém",
        },
    },
    motivo: "Registro fictício posterior da planilha no WMS",
} satisfies Values;

export const contingencyArrivalCommand = {
    ...contingencyCountCommand,
    identidadeFato: "PLAN-CHEGADA-ALFA-01",
    tipo: "CHEGADA",
    dados: {
        pedidoId: "9007199254740993",
        dados: {
            versao: "9007199254740995",
            chegouEm: "2026-10-15T15:20:30.987654Z",
            observacao: "Chegada inteiramente fictícia",
            itens: [
                {
                    itemNotaId: "9007199254740997",
                    quantidadeBoa: "3.000001",
                    quantidadeAvariada: "0",
                },
            ],
        },
    },
} satisfies Values;

export const contingencyReferences = {
    ...initialLoadReferences,
    options: {
        ...initialLoadReferences.options,
        pedidoId: [{ value: "9007199254740993", title: "ENT-ALFA-01" }],
        itemNotaId: [
            { value: "9007199254740997", title: "SKU-ALFA · item da nota" },
        ],
    },
    records: {
        ...initialLoadReferences.records,
        "PedidoEntradaDto.Resumo": [
            { id: "9007199254740993", referencia: "ENT-ALFA-01" },
        ],
        "PedidoEntradaDto.ItemConferencia": [
            { id: "9007199254740997", sku: "SKU-ALFA", unidadeMedida: "UN" },
        ],
    },
};
