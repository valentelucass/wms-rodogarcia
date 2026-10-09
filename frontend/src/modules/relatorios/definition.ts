import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "relatorios",
        title: "Consultas e relatórios",
        fe: "FE08 / FE12",
        be: "BE08 / BE12–14",
        steps: [
            s(
                "Estoque e movimentos",
                "Consultas paginadas no mesmo alcance do usuário. Sem total global extrapolado de uma página.",
                "EstoqueController",
                ["listar", "saldo", "historico"],
            ),
            s(
                "Valor e validade",
                "Valores são retornados pelo backend; null é não informado.",
                "IndicadorEstoqueController",
                ["listar"],
            ),
            s(
                "Entrada e saída",
                "Consulte pedidos do contexto. Filtros adicionais inexistentes no contrato não são enviados.",
                "PedidoEntradaController",
                ["listar"],
            ),
            s(
                "Pedidos de saída",
                "Reservado, separado e retirado permanecem distintos.",
                "PedidoSaidaController",
                ["listar"],
            ),
            s(
                "Serviços e valores",
                "Memória e histórico financeiro do servidor.",
                "CalculoCobrancaController",
                ["listar", "consultar"],
            ),
            s(
                "Auditoria",
                "Somente Gestor. Registros preservados; não há edição/exclusão.",
                "AuditoriaController",
                ["listar"],
            ),
        ],
        lookups: [
            ["Consultar clientes para auditoria", "ClienteController.listar"],
        ],
        references: [
            ["Pedido de entrada", "PedidoEntradaDto.Resumo"],
            ["Entrada conferida", "RecebimentoDto.Entrada"],
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
            ["Pedido de saída", "PedidoSaidaDto.Detalhe"],
            ["Reserva", "PedidoSaidaDto.Reserva"],
            ["Cálculo", "CalculoCobrancaDto.Resultado"],
            ["Fechamento", "FechamentoCobrancaDto.Fechamento"],
            ["Versão financeira", "FechamentoCobrancaDto.Versao"],
        ],
        referenceFields: {},
    },
];
export const nextActions: Record<string, NextAction> = {};
