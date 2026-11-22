import type { Values } from "../../contracts/runtime";
import { demoCode } from "../../api/mock/constants";
export const exampleFixtures: Record<string, Values> = {
    "FiscalNfeDto.Linha": {
        id: "601",
        referencia: "DEMO-01",
        situacaoOperacional: "SEPARADO",
        situacaoFiscal: "PENDENTE",
        numerosNfe: [],
    },
    "FiscalNfeDto.Andamento": {
        pedidoId: "601",
        situacao: "PENDENTE",
        solicitacoes: [],
    },
    "PedidoSaidaDto.Detalhe": { id: "601", situacao: "RASCUNHO", reservas: [] },
    "PedidoSaidaDto.Reserva": {
        id: "701",
        codigoUnidade: demoCode,
        unidadeId: "501",
        notaOrigemId: "301",
    },
    "PedidoSaidaDto.Selecao": { unidadeId: "501" },
};
