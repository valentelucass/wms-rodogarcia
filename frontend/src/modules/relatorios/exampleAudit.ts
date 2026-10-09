import type { Request } from "../../api/client";
import { isObject } from "../../contracts/runtime";

/** Respostas preparadas apenas para exercício. Não classifica JSON histórico real. */
const prepared = {
    VINCULO_COBRANCA: {
        tipo: "VINCULO_COBRANCA",
        tipoFisico: "CONTRATO_COBRANCA",
        acao: "VINCULO_TABELA",
    },
    VINCULO_COBRANCA_LEGADO: {
        tipo: "LEGADO_NAO_ATRIBUIDO",
        tipoFisico: "TABELA_COBRANCA",
        acao: "ENCERRAMENTO_VIGENCIA",
    },
    CONTRATO_COBRANCA: {
        tipo: "CONTRATO_COBRANCA",
        tipoFisico: "CONTRATO_COBRANCA",
        acao: "CONFIGURACAO",
    },
    TABELA_COBRANCA: {
        tipo: "TABELA_COBRANCA",
        tipoFisico: "TABELA_COBRANCA",
        acao: "ENCERRAMENTO",
    },
};
export function exampleAudit(request: Request, data: unknown): unknown {
    if (
        request.endpoint.id !== "AuditoriaController.listar" ||
        !isObject(data) ||
        !Array.isArray(data.itens)
    )
        return data;
    const row = prepared[request.query.tipo as keyof typeof prepared];
    if (!row) return data;
    return {
        ...data,
        itens: data.itens
            .filter(isObject)
            .map((item) => ({
                ...item,
                ...row,
                registroId: request.query.registroId,
                motivo: "Exercício fictício; histórico preparado, sem atribuição de dado real.",
            })),
    };
}
