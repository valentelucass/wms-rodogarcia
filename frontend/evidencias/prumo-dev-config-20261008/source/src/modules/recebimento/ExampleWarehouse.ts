import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
import { transitions } from "../../api/mock/scriptedValues";
import { ExampleReceiving } from "./ExampleReceiving";
// In-memory exercise receipts, not WMS availability/capacity rules.
export class ExampleWarehouse {
    private entry: Values | undefined;
    private unit: Values | undefined;
    private receiving = new ExampleReceiving();
    private units: Values[] = [];
    respond(r: Request, body: Values, data: unknown): unknown {
        if (r.endpoint.id.startsWith("PedidoEntradaController.")) {
            if (r.endpoint.method !== "GET" && isObject(data)) {
                this.entry = {
                    ...data,
                    referencia: this.entry?.referencia ?? data.referencia,
                    situacao:
                        transitions[r.endpoint.id] ??
                        this.entry?.situacao ??
                        data.situacao,
                    versao: String(
                        BigInt(String(this.entry?.versao ?? "-1")) + 1n,
                    ),
                    efetivadoEm:
                        r.endpoint.id === "PedidoEntradaController.efetivar"
                            ? "2026-10-08T12:15:00.123456Z"
                            : (this.entry?.efetivadoEm ?? null),
                    canceladoEm:
                        r.endpoint.id === "PedidoEntradaController.cancelar"
                            ? "2026-10-08T12:16:00.123456Z"
                            : (this.entry?.canceladoEm ?? null),
                    motivoConclusao: [
                        "PedidoEntradaController.efetivar",
                        "PedidoEntradaController.cancelar",
                    ].includes(r.endpoint.id)
                        ? body.motivo
                        : (this.entry?.motivoConclusao ?? null),
                };
                data = this.entry;
            }
            if (
                r.endpoint.id === "PedidoEntradaController.consultar" &&
                isObject(data) &&
                this.entry
            )
                data.pedido = this.entry;
            if (
                r.endpoint.id === "PedidoEntradaController.consultar" &&
                r.params.id === "102" &&
                isObject(data)
            )
                data.pedido = {
                    ...(fixture("PedidoEntradaDto.Resumo") as Values),
                    id: "102",
                    versao: "0",
                    referencia: "DEV-SAIDA-601",
                    situacao: "EM_CONFERENCIA",
                };
            if (
                r.endpoint.id === "PedidoEntradaController.listar" &&
                isObject(data) &&
                this.entry
            )
                data.itens = [this.entry];
        }
        if (
            r.endpoint.id === "UnidadeLogisticaController.unitizar" &&
            isObject(data)
        ) {
            data.pedidoId = r.params.pedidoId;
            data.versaoPedido = String(BigInt(String(body.versaoPedido)) + 1n);
            if (this.entry) this.entry.versao = data.versaoPedido;
            this.unit = {
                ...(fixture("UnidadeLogisticaDto.Resumo") as Values),
                versao: "7",
            };
            if (Array.isArray(body.unidades) && body.unidades.length === 2) {
                this.units = [
                    { ...this.unit, quantidade: "500.000000" },
                    {
                        ...this.unit,
                        id: "502",
                        codigo: "00000000-0000-4000-8000-000000000002",
                        quantidade: "500.000000",
                    },
                ];
                data.unidades = this.units.map((unidade) => ({
                    ...(fixture("UnidadeLogisticaDto.Detalhe") as Values),
                    unidade,
                }));
            } else {
                this.units = [this.unit];
                if (Array.isArray(data.unidades) && isObject(data.unidades[0]))
                    data.unidades[0].unidade = this.unit;
            }
        }
        if (
            r.endpoint.id === "UnidadeLogisticaController.listar" &&
            isObject(data) &&
            this.units.length
        )
            data = {
                ...data,
                itens: this.units,
                totalItens: String(this.units.length),
            };
        if (
            r.endpoint.id === "UnidadeLogisticaController.progresso" &&
            isObject(data) &&
            this.units.length
        )
            data = {
                ...data,
                entradasConferidas: "1",
                entradasUnitizadas: "1",
                entradasPendentes: "0",
                concluida: true,
            };
        if (
            r.endpoint.id === "UnidadeLogisticaController.consultar" &&
            isObject(data)
        )
            data.unidade =
                this.units.find((u) => u.id === r.params.id) ??
                this.unit ??
                data.unidade;
        if (
            r.endpoint.id === "UnidadeLogisticaController.dividir" &&
            isObject(data)
        ) {
            const source =
                this.units.find((u) => u.id === r.params.id) ??
                (fixture("UnidadeLogisticaDto.Resumo") as Values);
            this.units = [
                { ...source, versao: "8", quantidade: "300.000000" },
                {
                    ...source,
                    id: "503",
                    versao: "0",
                    codigo: "00000000-0000-4000-8000-000000000004",
                    quantidade: "200.000000",
                },
            ];
            data.unidades = this.units.map((unidade) => ({
                ...(fixture("UnidadeLogisticaDto.Detalhe") as Values),
                unidade,
            }));
        }
        if (
            r.endpoint.id === "UnidadeLogisticaController.reagrupar" &&
            isObject(data)
        ) {
            this.units = [
                {
                    ...(this.units.find((u) => u.id === r.params.id) ??
                        (fixture("UnidadeLogisticaDto.Resumo") as Values)),
                    versao: "9",
                    quantidade: "500.000000",
                },
            ];
            data.unidades = this.units.map((unidade) => ({
                ...(fixture("UnidadeLogisticaDto.Detalhe") as Values),
                unidade,
            }));
        }
        if (
            r.endpoint.id === "UnidadeLogisticaController.etiqueta" &&
            isObject(data)
        ) {
            const u = this.units.find((u) => u.codigo === r.params.codigo);
            data = {
                ...data,
                codigoLeitura: r.params.codigo,
                ...(u
                    ? { quantidade: u.quantidade, versaoConteudo: u.versao }
                    : {}),
            };
        }
        if (r.endpoint.id === "EstoqueController.consultar" && isObject(data))
            data.unidade = {
                ...(this.unit ??
                    (fixture("UnidadeLogisticaDto.Resumo") as Values)),
                codigo: r.params.codigo,
            };
        if (
            r.endpoint.id === "EstoqueController.posicionar" &&
            isObject(data) &&
            isObject(data.estoque)
        ) {
            this.unit = {
                ...(this.unit ??
                    (fixture("UnidadeLogisticaDto.Resumo") as Values)),
                versao: String(BigInt(String(body.versaoUnidade)) + 1n),
            };
            data.estoque.unidade = this.unit;
        }
        return this.receiving.respond(r, body, data);
    }
}
