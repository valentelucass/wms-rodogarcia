import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
import { transitions } from "../../api/mock/scriptedValues";
import { ExampleExpedition } from "./ExampleExpedition";
// Scripted exercise revisions/statuses, not FIFO or availability calculation.
export class ExampleDispatch {
    private outgoing: Values | undefined;
    private expedition = new ExampleExpedition();
    respond(
        r: Request,
        _body: Values,
        data: unknown,
        physical?: { antes: string; depois: string },
    ): unknown {
        if (r.endpoint.id === "ExpedicaoController.consultar" && !this.outgoing)
            this.outgoing = {
                ...(fixture("PedidoSaidaDto.Detalhe") as Values),
                versao: "3",
                situacao: "SEPARADO",
                reservas: [fixture("PedidoSaidaDto.Reserva")],
            };
        if (
            r.endpoint.id === "PedidoSaidaController.reservar" &&
            isObject(data) &&
            isObject(data.pedido)
        )
            data.pedido.reservas = [fixture("PedidoSaidaDto.Reserva")];
        if (
            r.endpoint.id.startsWith("PedidoSaidaController.") &&
            isObject(data) &&
            isObject(data.pedido)
        ) {
            this.outgoing = {
                ...data.pedido,
                versao: String(
                    BigInt(String(this.outgoing?.versao ?? "-1")) + 1n,
                ),
            };
            data.pedido = this.outgoing;
        }
        if (
            r.endpoint.id === "PedidoSaidaController.consultar" &&
            this.outgoing
        )
            data = this.outgoing;
        if (
            r.endpoint.id === "PedidoSaidaController.sugerir" &&
            isObject(data) &&
            this.outgoing
        )
            data.versao = this.outgoing.versao;
        if (
            r.endpoint.id.startsWith("ExpedicaoController.") &&
            isObject(data) &&
            isObject(data.expedicao) &&
            this.outgoing
        ) {
            data.expedicao.pedido = {
                ...this.outgoing,
                versao: String(BigInt(String(this.outgoing.versao)) + 1n),
                situacao: transitions[r.endpoint.id] ?? this.outgoing.situacao,
            };
            this.outgoing = data.expedicao.pedido as Values;
        }
        return this.expedition.respond(
            r,
            _body,
            this.outgoing ?? (fixture("PedidoSaidaDto.Detalhe") as Values),
            data,
            physical,
        );
    }
}
