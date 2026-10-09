import { fixture } from "../../api/mock/fixtures";
import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
// Cenários nominados e valores preparados. Nenhuma soma/aceitação de carga.
export class ExampleReceiving {
    private partial = false;
    private multiple = false;
    private xml = false;
    private arrivals = 0;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "PedidoEntradaController.criar") {
            this.partial = String(body.referencia).includes("FICT-100-");
            this.multiple = String(body.referencia).includes("FICT-1000");
            this.xml = false;
            this.arrivals = 0;
        }
        if (id === "PedidoEntradaController.xml") this.xml = true;
        if (id === "PedidoEntradaController.chegada") this.arrivals++;
        if (
            id === "PedidoEntradaController.consultar" &&
            isObject(data) &&
            Array.isArray(data.notas)
        ) {
            data.divergente = this.partial && this.arrivals > 0;
            data.notas = data.notas
                .filter(isObject)
                .map((note) => ({
                    ...note,
                    xmlVinculado: this.xml,
                    itens: (Array.isArray(note.itens) ? note.itens : [])
                        .filter(isObject)
                        .map((item) => ({
                            ...item,
                            quantidadePrevista: this.multiple ? "1000.000000" : this.partial
                                ? "100.000000"
                                : item.quantidadePrevista,
                            quantidadeRecebidaBoa: this.partial
                                ? this.arrivals === 0
                                    ? "0.000000"
                                    : this.arrivals === 1
                                      ? "50.000000"
                                      : "98.000000"
                                : this.arrivals === 0
                                  ? "0.000000"
                                  : this.multiple ? "1000.000000" : "10.000000",
                            quantidadeRecebidaAvariada: "0.000000",
                            diferenca: this.partial
                                ? this.arrivals === 0
                                    ? "100.000000"
                                    : this.arrivals === 1
                                      ? "50.000000"
                                      : "2.000000"
                                : this.arrivals === 0
                                  ? "10.000000"
                                  : "0.000000",
                        })),
                }));
        }
        if (id === "PedidoEntradaController.chegadas")
            return Array.from({ length: this.arrivals }, (_, i) => ({
                ...(fixture("RecebimentoDto.Chegada") as Values),
                id: String(1401 + i),
                estornadaEm: null,
                estornadaPor: null,
                motivoEstorno: null,
            }));
        if (id === "PedidoEntradaController.entradas" && Array.isArray(data))
            return data
                .filter(isObject)
                .map((entry) => ({
                    ...entry,
                    quantidadeTriagem: this.multiple ? "1000.000000" : this.partial ? "98.000000" : "10.000000",
                    quantidadeQuarentena: "0.000000",
                    disponivelParaSaida: false,
                    unitizadaEm: null,
                }));
        return data;
    }
}
