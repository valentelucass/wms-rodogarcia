import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
import { ApiError } from "../../api/client";
// Two authored exercise receipts. There is no subtraction or stock rule here.
const readings: Record<string, Values> = {
    "8": {
        contado: "8.000000",
        diferenca: "-2.000000",
        reservado: "0.000000",
        impedimento: false,
        situacao: "PENDENTE",
    },
    "8.000000": {
        contado: "8.000000",
        diferenca: "-2.000000",
        reservado: "0.000000",
        impedimento: false,
        situacao: "PENDENTE",
    },
    "7": {
        contado: "7.000000",
        diferenca: "-3.000000",
        reservado: "8.000000",
        impedimento: true,
        situacao: "PENDENTE_RESERVA",
    },
    "7.000000": {
        contado: "7.000000",
        diferenca: "-3.000000",
        reservado: "8.000000",
        impedimento: true,
        situacao: "PENDENTE_RESERVA",
    },
};
export class ExampleCount {
    private count: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        if (r.endpoint.id === "ContagemController.contar" && isObject(data)) {
            const reading = readings[String(body.contado)];
            if (!reading)
                throw new ApiError(
                    "Exercício FE12: use a leitura preparada 8 (sem reserva) ou 7 (com impedimento de reserva). Outras leituras dependem do backend.",
                    422,
                    "EXEMPLO_NAO_PREPARADO",
                );
            this.count = {
                ...data,
                id: "2201",
                versao: "0",
                revisao: 1,
                unidadeId: "1",
                codigoUnidade: body.codigoUnidade,
                esperado: "10.000000",
                observadoEm: body.observadoEm,
                ...reading,
                efeitoJson: null,
                origens: [
                    {
                        ...(fixture("ContagemDto.Origem") as Values),
                        entradaId: "201",
                        itemNotaId: "401",
                        quantidade: "10.000000",
                    },
                ],
            };
            return this.count;
        }
        if (r.endpoint.id === "ContagemController.aplicar" && this.count) {
            if (this.count.impedimento)
                throw new ApiError(
                    "Exercício FE12: ajuste recusado por reserva; consulte o impedimento. Nenhum efeito confirmado.",
                    409,
                    "CONTAGEM_RESERVADA",
                );
            if (body.revisao !== this.count.revisao)
                throw new ApiError(
                    "Exercício FE12: revisão de contagem divergente; consulte novamente.",
                    409,
                    "REVISAO_DIVERGENTE",
                );
            this.count = {
                ...this.count,
                versao: "1",
                situacao: "APLICADA",
                efeitoJson: '{"exercicio":"ajuste ficticio identificado 2201"}',
            };
            return this.count;
        }
        if (r.endpoint.id === "ContagemController.consultar" && this.count)
            return this.count;
        return data;
    }
}
