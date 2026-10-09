import type { Values } from "../../contracts/runtime";
export const exampleFixtures: Record<string, Values> = {
    "CalculoCobrancaDto.Resultado": { id: "801" },
    "FechamentoCobrancaDto.Fechamento": { id: "901", situacao: "PREPARADO" },
    "FechamentoCobrancaDto.Versao": {
        id: "1001",
        fechamentoId: "901",
        calculoId: "801",
        entregas: [],
        nfse: [],
        confirmacoes: [],
        resolucaoFinanceira: null,
        ajustes: [],
        situacao: "PREPARADA",
        estadoExterno: "NAO_ENTREGUE",
    },
    "FechamentoCobrancaDto.Demonstrativo": { fechamentoId: "901", numero: 1 },
};
