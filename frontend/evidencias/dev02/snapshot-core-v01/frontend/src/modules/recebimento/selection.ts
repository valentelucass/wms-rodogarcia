export const receivingSelection = {
    root: "PedidoEntradaDto.Resumo",
    details: ["PedidoEntradaDto.Detalhe"],
    views: ["PedidoEntradaDto.Detalhe"],
    children: [
        "PedidoEntradaDto.Nota",
        "PedidoEntradaDto.ItemConferencia",
        "RecebimentoDto.Chegada",
        "RecebimentoDto.ItemChegada",
        "RecebimentoDto.Entrada",
    ],
    controllers: ["PedidoEntradaController", "UnidadeLogisticaController"],
};
