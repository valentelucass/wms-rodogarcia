import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
export class ExampleCount {
    private count: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        if (r.endpoint.id === "ContagemController.contar" && isObject(data)) {
            this.count = {
                ...data,
                id: "2201",
                versao: "0",
                revisao: 1,
                unidadeId: "501",
                codigoUnidade: body.codigoUnidade,
                esperado: "10.000000",
                contado: "8.000000",
                diferenca: "-2.000000",
                reservado: "0.000000",
                impedimento: false,
                observadoEm: body.observadoEm,
                situacao: "PENDENTE",
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
