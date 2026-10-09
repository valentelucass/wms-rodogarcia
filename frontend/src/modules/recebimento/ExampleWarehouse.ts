import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
import { transitions } from "../../api/mock/scriptedValues";
import { ExampleReceiving } from "./ExampleReceiving";
import { ExampleReturnedEntry } from "./ExampleReturnedEntry";
// In-memory exercise receipts, not WMS availability/capacity rules.
export class ExampleWarehouse {
    private entry: Values | undefined;
    private unit: Values | undefined;
    private receiving = new ExampleReceiving();
    private returned = new ExampleReturnedEntry();
    private units: Values[] = [];
    physicalReceipt: { antes: string; depois: string } | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        if (r.endpoint.id === "ExpedicaoController.consultar" && !this.unit) {
            // Receita fiscal separada: pallet500 com reserva10, preparada para FE10.
            this.unit = {
                ...(fixture("UnidadeLogisticaDto.Resumo") as Values),
                quantidade: "500.000000",
                versao: "7",
            };
            this.units = [this.unit];
        }
        if (r.endpoint.id === "ExpedicaoController.retirar") {
            // Duas receitas pré-escritas, sem subtrair ou conciliar quantidade.
            this.physicalReceipt =
                this.unit?.quantidade === "10.000000"
                    ? { antes: "10.000000", depois: "0.000000" }
                    : { antes: "500.000000", depois: "490.000000" };
            this.unit = {
                ...(this.unit ??
                    (fixture("UnidadeLogisticaDto.Resumo") as Values)),
                quantidade: this.physicalReceipt.depois,
                versao: "8",
            };
            this.units = [this.unit];
        }
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
                pedidoId: r.params.pedidoId,
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
            isObject(data)
        ) {
            const distributed = this.units.some(
                (unit) => unit.pedidoId === r.params.pedidoId,
            );
            data = {
                ...data,
                pedidoId: r.params.pedidoId,
                entradasConferidas: "1",
                entradasUnitizadas: distributed ? "1" : "0",
                entradasPendentes: distributed ? "0" : "1",
                concluida: distributed,
            };
        }
        if (
            [
                "UnidadeLogisticaController.consultar",
                "UnidadeLogisticaController.lerCodigo",
            ].includes(r.endpoint.id) &&
            isObject(data)
        )
            data.unidade =
                this.units.find((u) =>
                    r.params.codigo
                        ? u.codigo === r.params.codigo
                        : u.id === r.params.unidadeId,
                ) ??
                this.unit ??
                data.unidade;
        if (
            r.endpoint.id === "UnidadeLogisticaController.dividir" &&
            isObject(data)
        ) {
            const source =
                this.units.find((u) => u.id === r.params.unidadeId) ??
                (fixture("UnidadeLogisticaDto.Resumo") as Values);
            this.units = [
                ...this.units.filter((u) => u.id !== source.id),
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
                ...this.units.filter(
                    (u) => u.id !== r.params.unidadeId && u.id !== "503",
                ),
                {
                    ...(this.units.find((u) => u.id === r.params.unidadeId) ??
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
                    ? {
                          quantidadeProduto: u.quantidade,
                          versaoConteudo:
                              u.versao === "9"
                                  ? "12"
                                  : u.versao === "8"
                                    ? "11"
                                    : "7",
                      }
                    : {}),
            };
        }
        if (
            r.endpoint.id === "EstoqueController.listar" &&
            isObject(data) &&
            this.units.length
        ) {
            const pagina = Number(r.query.pagina ?? 0),
                tamanho = Number(r.query.tamanho ?? 20);
            data = {
                ...data,
                itens: this.units
                    .slice(pagina * tamanho, (pagina + 1) * tamanho)
                    .map((unidade) => ({
                        ...(fixture("EstoqueDto.Unidade") as Values),
                        unidade,
                    })),
                pagina,
                tamanho,
                totalItens: String(this.units.length),
                totalPaginas: Math.ceil(this.units.length / tamanho),
            };
        }
        if (r.endpoint.id === "EstoqueController.consultar" && isObject(data))
            data.unidade = {
                ...(this.units.find((u) => u.codigo === r.params.codigo) ??
                    this.unit ??
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
        return this.returned.respond(
            r,
            body,
            this.receiving.respond(r, body, data),
        );
    }
}
