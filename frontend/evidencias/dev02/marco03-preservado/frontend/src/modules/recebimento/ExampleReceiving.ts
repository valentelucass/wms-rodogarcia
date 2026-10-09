import { fixture } from "../../api/mock/fixtures";
import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
// Cenários nominados e valores preparados. Nenhuma soma/aceitação de carga.
export class ExampleReceiving {
    private partial = false;
    private multiple = false;
    private damaged = false;
    private xml = false;
    private arrivals = 0;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "PedidoEntradaController.criar") {
            this.partial = String(body.referencia).includes("FICT-100-");
            this.multiple = String(body.referencia).includes("FICT-1000");
            this.damaged = String(body.referencia).includes("FICT-AVARIA");
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
            data.notas = data.notas.filter(isObject).map((note) => ({
                ...note,
                xmlVinculado: this.xml,
                itens: (Array.isArray(note.itens) ? note.itens : [])
                    .filter(isObject)
                    .map((item) => ({
                        ...item,
                        prevista: this.damaged
                            ? "100.000000"
                            : this.multiple
                              ? "1000.000000"
                              : this.partial
                                ? "100.000000"
                                : item.prevista,
                        recebidaBoa: this.damaged
                            ? this.arrivals === 0
                                ? "0.000000"
                                : "95.000000"
                            : this.partial
                              ? this.arrivals === 0
                                  ? "0.000000"
                                  : this.arrivals === 1
                                    ? "50.000000"
                                    : "98.000000"
                              : this.arrivals === 0
                                ? "0.000000"
                                : this.multiple
                                  ? "1000.000000"
                                  : "10.000000",
                        recebidaAvariada:
                            this.damaged && this.arrivals > 0
                                ? "5.000000"
                                : "0.000000",
                        diferenca: this.damaged
                            ? this.arrivals === 0
                                ? "100.000000"
                                : "0.000000"
                            : this.partial
                              ? this.arrivals === 0
                                  ? "100.000000"
                                  : this.arrivals === 1
                                    ? "50.000000"
                                    : "2.000000"
                              : this.arrivals === 0
                                ? this.multiple
                                    ? "1000.000000"
                                    : "10.000000"
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
        if (id === "PedidoEntradaController.entradas") {
            const rows = Array.isArray(data)
                ? data
                : isObject(data) && Array.isArray(data.itens)
                  ? data.itens
                  : [];
            const entries = rows.filter(isObject).map((entry) => ({
                ...entry,
                quantidadeTriagem: this.damaged
                    ? "95.000000"
                    : this.multiple
                      ? "1000.000000"
                      : this.partial
                        ? "98.000000"
                        : "10.000000",
                quantidadeQuarentena: this.damaged ? "5.000000" : "0.000000",
                disponivelParaSaida: false,
                unitizadaEm: null,
            }));
            return isObject(data) ? { ...data, itens: entries } : entries;
        }
        return data;
    }
}
