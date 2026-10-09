import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "estoque",
        title: "Estoque e rastreabilidade",
        fe: "FE08",
        be: "BE08 / BE14",
        steps: [
            s(
                "Unidades e saldo por produto",
                "Consulte físico, disponível, reservado, triagem, quarentena e avaria. Um erro de consulta não significa saldo zero.",
                "EstoqueController",
                ["listar", "saldo", "consultar", "historico"],
            ),
            s(
                "Bloqueio e liberação",
                "Bloqueio preventivo não altera saldo nem cancela reserva. Liberação é confirmada no backend por Supervisor/Gestor.",
                "EstoqueController",
                ["bloquear", "liberar", "avariar"],
            ),
            s(
                "Avarias e reparos",
                "Registre o fato, quantidade afetada e destino. Reconhecimento de responsabilidade é do Gestor; preserve datas e reservas.",
                "AvariaController",
                ["listar", "registrar", "reconhecer", "reparar"],
            ),
            s(
                "Indicadores e validade",
                "Valor é consultado somente por Supervisor/Gestor. FIFO, chegada real e início da cobrança são datas diferentes.",
                "IndicadorEstoqueController",
                ["listar", "consultar", "configurar"],
            ),
        ],
        lookups: [
            ["Consultar produtos para saldo", "ProdutoController.listar"],
            [
                "Consultar origem da unidade selecionada",
                "UnidadeLogisticaController.consultar",
            ],
            [
                "Consultar posições para avaria e reparo",
                "EnderecoController.listar",
            ],
        ],
        references: [["Unidade logística", "UnidadeLogisticaDto.Resumo"]],
        referenceFields: {
            enderecoId: "EnderecoDto.Resposta",
            unidadeId: "UnidadeLogisticaDto.Resumo",
        },
    },
];
export const nextActions: Record<string, NextAction> = {
    "AvariaController.registrar": {
        page: "estoque",
        action: "AvariaController.listar",
        title: "Consultar ocorrência e condição registradas",
    },
    "AvariaController.reconhecer": {
        page: "estoque",
        action: "AvariaController.listar",
        title: "Consultar responsabilidade reconhecida",
    },
    "AvariaController.reparar": {
        page: "estoque",
        action: "EstoqueController.consultar",
        title: "Consultar condição e bloqueio após reparo",
    },
    "EstoqueController.liberar": {
        page: "estoque",
        action: "EstoqueController.consultar",
        title: "Conferir a unidade após liberação",
    },
};
