export const serviceFactRecoveryData = {
    id: "9007199254740993",
    versao: "9007199254740995",
    clienteId: "83",
    armazemId: "46",
    servicoId: "27",
    chaveFato: "ENTRADA:UNIDADE:1359",
    origem: "SUGESTAO",
    unidadeId: "1359",
    pedidoEntradaId: "114",
    pedidoSaidaId: null,
    produtoId: "26",
    referenciaExecucao: null,
    executadoEm: "2026-10-16T02:46:36.454123Z",
    quantidade: "1.000000",
    categoria: "PALLET",
    valorBase: null,
    criterioRateio:
        "Origem única da NF 15102026, unidade B10.\nCritério fictício preservado integralmente.",
    situacao: "CONFIRMADO",
    cotas: [{ notaId: "91", cota: "10.000000" }],
};
export const serviceFactRecoveryRecords = {
    "ClienteDto.Resposta": [
        {
            id: "83",
            nome: "QA MANUAL - Cliente revisado",
            codigo: "CLI-000008",
        },
    ],
    "ArmazemDto.Resposta": [
        {
            id: "46",
            nome: "QA MANUAL - Armazém revisado",
            codigo: "ARM-000006",
        },
    ],
    "ConfiguracaoCobrancaDto.Servico": [
        {
            id: "27",
            codigo: "SRV-000004",
            descricao: "QA MANUAL - Entrada fictícia",
            tipo: "ENTRADA",
            unidade: "UNIDADE_LOGISTICA",
            situacao: "ATIVO",
        },
    ],
    "UnidadeLogisticaDto.Resumo": [
        {
            id: "1359",
            codigo: "bf1b96bc-62e4-4f29-80fb-9cbf2bfaba80",
            tipo: "PALLET",
            situacao: "ATIVA",
        },
    ],
    "ProdutoDto.Resposta": [
        { id: "26", sku: "QA1510-UN", descricao: "Produto contado revisado" },
    ],
    "PedidoEntradaDto.Resumo": [{ id: "114", referencia: "ENT-000011" }],
    "PedidoEntradaDto.Nota": [{ id: "91", numero: "15102026", serie: 0 }],
};
