import type { Values } from "../../src/contracts/runtime";

export const initialLoadSample = {
    id: "9007199254740993",
    versao: "9007199254740995",
    clienteId: "9007199254741001",
    armazemId: "9007199254741003",
    produtoId: "9007199254740997",
    referencia: "CAR-ALFA-01",
    etiquetaFornecida: "ETQ-ORIGEM-ALFA",
    quantidadeEstagio: "3.000001",
    situacao: "PENDENTE",
    revisao: {
        numero: 2,
        conteudoHash: "a".repeat(64),
        dados: {
            entradaExistenteId: null,
            referenciaPedido: "PED-ORIGEM-ALFA",
            nota: null,
            chegadaReal: "2026-10-15T14:20:30.123456Z",
            dataFifo: "2026-10-14T10:11:12.123456Z",
            lote: "LOTE-ALFA",
            validade: "2027-06-30",
            quantidadeBoa: "3.000001",
            quantidadeAvariada: "0",
            unidades: [
                {
                    embalagemId: "9007199254740999",
                    tipo: "PALLET",
                    condicao: "BOA",
                    quantidade: "3.000001",
                },
            ],
            fonte: "QA fictícia — documento e conferência da carga Alfa.\nDados levantados preservados para revisão.",
        },
    },
    entradaId: null,
    etiquetas: ["00000000-0000-4000-8000-000000000003"],
    pendencias: ["Nota de origem ainda não informada."],
} satisfies Values;

export const initialLoadReferences = {
    defaults: {},
    options: {
        clienteId: [
            { value: "9007199254741001", title: "Cliente Alfa fictício" },
        ],
        armazemId: [
            { value: "9007199254741003", title: "Armazém central fictício" },
        ],
        produtoId: [
            { value: "9007199254740997", title: "Produto Alfa · SKU-ALFA" },
        ],
        embalagemId: [
            { value: "9007199254740999", title: "DUN-ALFA · Pallet de 10" },
            { value: "9007199254741005", title: "DUN-BETA · Bobina" },
        ],
    },
    records: {
        "ClienteDto.Resposta": [
            { id: "9007199254741001", nome: "Cliente Alfa fictício" },
        ],
        "ArmazemDto.Resposta": [
            { id: "9007199254741003", nome: "Armazém central fictício" },
        ],
        "ProdutoDto.Resposta": [
            {
                id: "9007199254740997",
                sku: "SKU-ALFA",
                descricao: "Produto Alfa",
            },
        ],
        "EmbalagemDto.Resposta": [
            {
                id: "9007199254740999",
                codigoDun: "DUN-ALFA",
                descricao: "Pallet de 10",
            },
            {
                id: "9007199254741005",
                codigoDun: "DUN-BETA",
                descricao: "Bobina",
            },
        ],
    },
};
