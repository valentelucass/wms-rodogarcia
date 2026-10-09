import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "saida",
        title: "Saída, FIFO e reserva",
        fe: "FE09",
        be: "BE09 / BE10",
        steps: [
            s(
                "1. Pedido integral",
                "Crie manualmente ou importe XML existente. Não há liberação parcial do pedido.",
                "PedidoSaidaController",
                ["listar", "consultar", "criar", "importarXml"],
            ),
            s(
                "2. Sugestão FIFO e reserva",
                "Consulte a sugestão do backend e transcreva/seleciona as unidades. A reserva só existe após confirmação. Exceção FIFO exige justificativa e Supervisor/Gestor.",
                "PedidoSaidaController",
                ["sugerir", "justificar", "reservar", "revalidar"],
            ),
            s(
                "3. Leitura e separação",
                "Confirme UUID e revisão do conteúdo da etiqueta. Destino de separação e remanescente são explícitos. Avaria pode bloquear a expedição.",
                "ExpedicaoController",
                ["consultar", "ler", "separar"],
            ),
            s(
                "4. Reversão e cancelamento",
                "A reserva não vence automaticamente. Consulte consequências antes de cancelar ou reverter.",
                "PedidoSaidaController",
                ["reverter", "cancelar"],
            ),
        ],
        lookups: [
            ["Consultar posições para separação", "EnderecoController.listar"],
            [
                "Consultar etiqueta da unidade",
                "UnidadeLogisticaController.etiqueta",
            ],
        ],
        references: [
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
            ["Pedido de saída", "PedidoSaidaDto.Detalhe"],
            ["Reserva", "PedidoSaidaDto.Reserva"],
        ],
        referenceFields: {
            produtoId: "ProdutoDto.Resposta",
            enderecoId: "EnderecoDto.Resposta",
            reservaId: "PedidoSaidaDto.Reserva",
            unidadeId: "UnidadeLogisticaDto.Resumo",
        },
    },
    {
        id: "fiscal",
        title: "Fiscal e retirada",
        fe: "FE10",
        be: "BE10 / BE11",
        steps: [
            s(
                "1. Documento de mercadoria",
                "NOTAZZ registra nota/protocolo/cobertura; XML é alternativa de registro. Nenhuma ação aqui emite nota no sistema externo.",
                "ExpedicaoController",
                ["consultar", "documento", "cancelar"],
            ),
            s(
                "2. Retirada física integral",
                "Supervisor/Gestor confirma retirada real, com XMLs comprovantes e destinos de todos os remanescentes. Documento registrado e retirada são fatos diferentes.",
                "ExpedicaoController",
                ["retirar", "fatos"],
            ),
            s(
                "3. Retorno e devolução",
                "Retorno interno exige destinos e documento cancelado. Devolução após saída cria nova entrada ligada à baixa original, preservando FIFO.",
                "ExpedicaoController",
                ["retornar", "devolver"],
            ),
        ],
        lookups: [
            [
                "Consultar posições de retorno ou remanescente",
                "EnderecoController.listar",
            ],
        ],
        references: [
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
            ["Pedido de saída", "PedidoSaidaDto.Detalhe"],
            ["Reserva", "PedidoSaidaDto.Reserva"],
            ["Documento fiscal", "ExpedicaoDto.DocumentoRegistrado"],
            ["Baixa de origem", "ExpedicaoDto.Baixa"],
        ],
        referenceFields: {
            produtoId: "ProdutoDto.Resposta",
            enderecoId: "EnderecoDto.Resposta",
            reservaId: "PedidoSaidaDto.Reserva",
            unidadeId: "UnidadeLogisticaDto.Resumo",
            baixaId: "ExpedicaoDto.Baixa",
        },
    },
];
export const nextActions: Record<string, NextAction> = {
    "PedidoSaidaController.justificar": {
        page: "saida",
        action: "PedidoSaidaController.reservar",
        title: "Conferir reserva com esta justificativa FIFO",
    },
    "PedidoSaidaController.criar": {
        page: "saida",
        action: "PedidoSaidaController.sugerir",
        title: "Consultar FIFO deste pedido integral",
    },
    "PedidoSaidaController.sugerir": {
        page: "saida",
        action: "PedidoSaidaController.reservar",
        title: "Confirmar reserva da sugestão do servidor",
    },
    "PedidoSaidaController.reservar": {
        page: "saida",
        action: "ExpedicaoController.ler",
        title: "Ler a etiqueta da reserva confirmada",
    },
    "ExpedicaoController.ler": {
        page: "saida",
        action: "ExpedicaoController.separar",
        title: "Confirmar destino da reserva lida",
    },
    "ExpedicaoController.separar": {
        page: "fiscal",
        action: "ExpedicaoController.documento",
        title: "Registrar documento existente deste pedido",
    },
    "ExpedicaoController.documento": {
        page: "fiscal",
        action: "ExpedicaoController.retirar",
        title: "Confirmar retirada física com comprovantes",
    },
    "ExpedicaoController.retirar": {
        page: "fiscal",
        action: "ExpedicaoController.consultar",
        title: "Conferir retirada física, baixas e documentos",
    },
    "ExpedicaoController.cancelar": {
        page: "fiscal",
        action: "ExpedicaoController.retornar",
        title: "Conferir destinos para retorno interno (ação física distinta)",
    },
    "ExpedicaoController.retornar": {
        page: "fiscal",
        action: "ExpedicaoController.consultar",
        title: "Conferir fatos do retorno interno",
    },
    "ExpedicaoController.devolver": {
        page: "entrada",
        action: "PedidoEntradaController.consultar",
        title: "Consultar a nova entrada ligada à devolução",
    },
};
