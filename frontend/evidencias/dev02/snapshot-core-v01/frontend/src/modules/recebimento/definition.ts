import { s, type Journey, type NextAction } from "../../domain/journeyTypes";
export const journeys: Journey[] = [
    {
        id: "entrada",
        title: "Entrada e conferência",
        fe: "FE05",
        be: "BE06",
        steps: [
            s(
                "1. Pedido de entrada",
                "Crie o pedido no cliente e armazém corretos. Selecione uma linha para continuar com o seu ID e revisão.",
                "PedidoEntradaController",
                ["listar", "consultar", "criar"],
            ),
            s(
                "2. Notas e itens",
                "Informe a nota manual ou importe o XML existente. Importação fiscal não confirma chegada física.",
                "PedidoEntradaController",
                ["nota", "xml"],
            ),
            s(
                "3. Conferência e chegadas",
                "Inicie a conferência, registre cada chegada real e compare previsto, recebido bom, avaria e diferença.",
                "PedidoEntradaController",
                ["iniciar", "chegada", "chegadas", "consultar"],
            ),
            s(
                "4. Divergência e efetivação",
                "Supervisor/Gestor registra a tratativa e efetiva a carga integral. Quarentena permanece indisponível; o servidor confirma o resultado.",
                "PedidoEntradaController",
                ["efetivar", "estornar", "cancelar", "entradas"],
            ),
        ],
        lookups: [
            ["Consultar produtos para a nota", "ProdutoController.listar"],
        ],
        references: [
            ["Pedido de entrada", "PedidoEntradaDto.Resumo"],
            ["Entrada conferida", "RecebimentoDto.Entrada"],
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
        ],
        referenceFields: {
            produtoId: "ProdutoDto.Resposta",
            embalagemId: "EmbalagemDto.Resposta",
            itemNotaId: "PedidoEntradaDto.ItemConferencia",
            entradaId: "RecebimentoDto.Entrada",
            unidadeId: "UnidadeLogisticaDto.Resumo",
        },
    },
    {
        id: "unidades",
        title: "Unidades e etiquetas",
        fe: "FE06",
        be: "BE07",
        steps: [
            s(
                "1. Distribuir quantidades",
                "Use as entradas conferidas. Quantidade de produto, DUN e identidade da unidade são campos distintos. O backend confere a soma.",
                "UnidadeLogisticaController",
                ["progresso", "unitizar", "listar"],
            ),
            s(
                "2. Corrigir organização física",
                "Divisão e reagrupamento exigem Supervisor/Gestor e motivo. Preserve a origem e consulte novamente a etiqueta.",
                "UnidadeLogisticaController",
                ["consultar", "dividir", "reagrupar"],
            ),
            s(
                "3. Ler e reimprimir etiqueta",
                "A etiqueta usa o UUID existente e conteúdo atual, sem endereço fixo. Impressão local é prévia; formato/equipamento do piloto permanecem pendentes.",
                "UnidadeLogisticaController",
                ["lerCodigo", "etiqueta"],
            ),
        ],
        lookups: [
            ["Consultar embalagens deste SKU", "EmbalagemController.listar"],
        ],
        references: [
            ["Pedido de entrada", "PedidoEntradaDto.Resumo"],
            ["Entrada conferida", "RecebimentoDto.Entrada"],
            ["Unidade logística", "UnidadeLogisticaDto.Resumo"],
        ],
        referenceFields: {
            produtoId: "ProdutoDto.Resposta",
            embalagemId: "EmbalagemDto.Resposta",
            itemNotaId: "PedidoEntradaDto.ItemConferencia",
            entradaId: "RecebimentoDto.Entrada",
        },
    },
];
export const nextActions: Record<string, NextAction> = {
    "PedidoEntradaController.criar": {
        page: "entrada",
        action: "PedidoEntradaController.nota",
        title: "Continuar com a nota deste pedido",
    },
    "PedidoEntradaController.nota": {
        page: "entrada",
        action: "PedidoEntradaController.consultar",
        title: "Conferir notas e itens recebidos do servidor",
    },
    "PedidoEntradaController.xml": {
        page: "entrada",
        action: "PedidoEntradaController.consultar",
        title: "Conferir itens importados (sem confirmar chegada)",
    },
    "PedidoEntradaController.iniciar": {
        page: "entrada",
        action: "PedidoEntradaController.chegada",
        title: "Registrar chegada dos itens consultados",
    },
    "PedidoEntradaController.chegada": {
        page: "entrada",
        action: "PedidoEntradaController.consultar",
        title: "Comparar previsto e físico registrado",
    },
    "PedidoEntradaController.efetivar": {
        page: "entrada",
        action: "PedidoEntradaController.entradas",
        title: "Consultar entradas efetivadas para unitização",
    },
    "PedidoEntradaController.entradas": {
        page: "unidades",
        action: "UnidadeLogisticaController.unitizar",
        title: "Unitizar a entrada conferida selecionada",
    },
    "UnidadeLogisticaController.unitizar": {
        page: "unidades",
        action: "UnidadeLogisticaController.etiqueta",
        title: "Consultar etiqueta da unidade criada",
    },
    "UnidadeLogisticaController.dividir": {page:"unidades",action:"UnidadeLogisticaController.listar",title:"Consultar identidades após divisão"},
    "UnidadeLogisticaController.reagrupar": {page:"unidades",action:"UnidadeLogisticaController.etiqueta",title:"Reimprimir conteúdo após reagrupamento"},
    "UnidadeLogisticaController.etiqueta": {
        page: "coletor",
        action: "",
        title: "Endereçar esta unidade no coletor",
    },
};
