import type { Values } from "../../contracts/runtime";
import { demoCode } from "../../api/mock/constants";
export const exampleFixtures: Record<string, Values> = {
    "PedidoEntradaDto.Resumo": {
        id: "101",
        efetivadoEm: null,
        canceladoEm: null,
        motivoConclusao: null,
    },
    "PedidoEntradaDto.Nota": { id: "301" },
    "PedidoEntradaDto.ItemConferencia": { id: "401", produtoId: "11" },
    "RecebimentoDto.Entrada": {
        id: "201",
        itemNotaId: "401",
        notaId: "301",
        produtoId: "11",
    },
    "UnidadeLogisticaDto.Resumo": {
        id: "501",
        pedidoId: "101",
        notaId: "301",
        produtoId: "11",
        embalagemId: "21",
        quantidade: "10.000000",
    },
    "UnidadeLogisticaDto.Etiqueta": {
        dataEntrada: "2026-10-07T12:00:00.123456Z",
        codigoLeitura: demoCode,
        versaoConteudo: "7",
        notaId: "301",
    },
};
