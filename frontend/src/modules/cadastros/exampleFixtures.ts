import type { Values } from "../../contracts/runtime";
export const exampleFixtures: Record<string, Values> = {
    "ProdutoDto.Resposta": { id: "11" },
    "EmbalagemDto.Resposta": {
        id: "21",
        produtoId: "11",
        codigoDun: "DUN-001",
    },
    "EnderecoDto.Resposta": {
        id: "81",
        codigo: "A101",
        capacidadePesoKg: "1000.000",
        alturaMetros: "2.000",
        larguraMetros: "1.000",
        profundidadeMetros: "1.000",
    },
    "ClienteDto.Resposta": { codigo: "DEMO" },
    "ArmazemDto.Resposta": { codigo: "DEMO-ARMAZEM" },
};
