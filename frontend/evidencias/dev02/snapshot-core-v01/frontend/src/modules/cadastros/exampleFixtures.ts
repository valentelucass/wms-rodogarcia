import type { Values } from "../../contracts/runtime";
export const exampleFixtures: Record<string, Values> = {
    "ProdutoDto.Resposta": { id: "11" },
    "EmbalagemDto.Resposta": { id: "21", produtoId: "11" },
    "EnderecoDto.Resposta": { id: "81", codigo: "A101" },
    "ClienteDto.Resposta": { codigo: "DEMO" },
    "ArmazemDto.Resposta": { codigo: "DEMO-ARMAZEM" },
};
