import { fixture } from "../../api/mock/fixtures";
import type { Request } from "../../api/client";
import { type Values, isObject } from "../../contracts/runtime";

// Respostas preparadas do ramo devolução: o backend efetiva a nova entrada.
// Estes valores nominais não são cálculo de estoque nem validação fiscal.
export class ExampleReturnedEntry {
    private reference = "";
    private arrived = "";
    respond(r: Request, body: Values, data: unknown): unknown {
        if (r.endpoint.id === "ExpedicaoController.devolver") {
            this.reference = String(body.referencia);
            this.arrived = String(body.chegadaReal);
        }
        if (!this.reference || r.params.id !== "102") return data;
        const entry = {
            ...(fixture("RecebimentoDto.Entrada") as Values),
            id: "202",
            itemNotaId: "402",
            notaId: "302",
            itemChegadaId: "1402",
            chegadaReal: this.arrived,
            dataFifo: "2026-10-07T12:00:00.123456Z",
            efetivadaEm: "2026-10-08T13:01:00.123456Z",
            quantidadeTriagem: "2.000000",
            quantidadeQuarentena: "0.000000",
            unitizadaEm: null,
            disponivelParaSaida: false,
        };
        if (
            r.endpoint.id === "PedidoEntradaController.entradas" &&
            isObject(data)
        )
            return {
                ...data,
                itens: [entry],
                totalItens: "1",
                totalPaginas: 1,
            };
        if (r.endpoint.id !== "PedidoEntradaController.consultar") return data;
        return {
            pedido: {
                ...(fixture("PedidoEntradaDto.Resumo") as Values),
                id: "102",
                versao: "0",
                referencia: this.reference,
                situacao: "EFETIVADO",
                criadoEm: "2026-10-08T13:01:00.123456Z",
                alteradoEm: "2026-10-08T13:01:00.123456Z",
                efetivadoEm: "2026-10-08T13:01:00.123456Z",
                motivoConclusao: "Devolução fictícia ligada à baixa 1701",
            },
            divergente: false,
            notas: [
                {
                    ...(fixture("PedidoEntradaDto.Nota") as Values),
                    id: "302",
                    xmlVinculado: false,
                    primeiraChegada: this.arrived,
                    itens: [
                        {
                            ...(fixture(
                                "PedidoEntradaDto.ItemConferencia",
                            ) as Values),
                            id: "402",
                            prevista: "2.000000",
                            recebidaBoa: "2.000000",
                            recebidaAvariada: "0.000000",
                            diferenca: "0.000000",
                        },
                    ],
                },
            ],
        };
    }
}
