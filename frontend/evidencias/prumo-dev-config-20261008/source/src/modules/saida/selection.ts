export const dispatchSelection = {
    root: "PedidoSaidaDto.Detalhe",
    details: ["PedidoSaidaDto.Detalhe", "ExpedicaoDto.Detalhe"],
    children: [
        "PedidoSaidaDto.Item",
        "PedidoSaidaDto.Reserva",
        "PedidoSaidaDto.Sugestao",
        "ExpedicaoDto.Separacao",
        "ExpedicaoDto.DocumentoRegistrado",
        "ExpedicaoDto.Baixa",
        "ExpedicaoDto.Devolucao",
        "ExpedicaoDto.Fato",
    ],
    controllers: ["PedidoSaidaController", "ExpedicaoController"],
    views: ["ExpedicaoDto.Detalhe"],
};
