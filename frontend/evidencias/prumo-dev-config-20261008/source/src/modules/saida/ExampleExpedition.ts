import { fixture } from "../../api/mock/fixtures";
import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";

// Recibos previamente definidos para exercício. Não calcula baixa, FIFO ou saldo.
export class ExampleExpedition {
    private detail: Values | undefined;
    respond(r: Request, body: Values, order: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (!id.startsWith("ExpedicaoController.")) return data;
        this.detail ??= {
            ...(fixture("ExpedicaoDto.Detalhe") as Values),
            documentos: [],
            baixas: [],
            devolucoes: [],
            fatos: [],
            retiradaEm: null,
        };
        this.detail.pedido = order;
        if (id === "ExpedicaoController.consultar")
            return structuredClone(this.detail);
        if (id === "ExpedicaoController.fatos")
            return structuredClone(this.detail.fatos);
        if (!isObject(data) || !isObject(data.expedicao)) return data;
        data.pedidoEntradaId = null;
        this.detail.pedido = data.expedicao.pedido;
        if (id === "ExpedicaoController.documento") {
            const documents = this.detail.documentos as Values[];
            this.detail.documentos = [
                ...documents,
                {
                    ...(fixture("ExpedicaoDto.DocumentoRegistrado") as Values),
                    id: String(1601 + documents.length),
                    origem: body.origem,
                    natureza: body.natureza,
                    nota: body.nota,
                    protocolo: body.protocolo,
                    coberturas: structuredClone(body.coberturas),
                    situacao: "AUTORIZADO",
                    canceladoEm: null,
                },
            ];
        }
        if (id === "ExpedicaoController.cancelar")
            this.detail.documentos = (this.detail.documentos as Values[]).map(
                (doc) =>
                    doc.id === r.params.documentoId
                        ? {
                              ...doc,
                              situacao: "CANCELADO",
                              canceladoEm: "2026-10-08T12:25:00.123456Z",
                          }
                        : doc,
            );
        if (id === "ExpedicaoController.retirar") {
            this.detail.retiradaEm = "2026-10-08T12:30:00.123456Z";
            this.detail.baixas = [
                {
                    ...(fixture("ExpedicaoDto.Baixa") as Values),
                    id: "1701",
                    reservaId: "701",
                    entradaOrigemId: "201",
                    quantidade: "10.000000",
                },
            ];
            this.detail.fatos = [
                {
                    ...(fixture("ExpedicaoDto.Fato") as Values),
                    id: "1801",
                    unidadeId: "501",
                    tipo: "RETIRADA",
                    quantidadeAntes: "500.000000",
                    quantidadeDepois: "490.000000",
                },
            ];
        }
        if (id === "ExpedicaoController.retornar") {
            this.detail.fatos = [
                {
                    ...(fixture("ExpedicaoDto.Fato") as Values),
                    id: "1802",
                    unidadeId: "501",
                    tipo: "RETORNO_INTERNO",
                    quantidadeAntes: "500.000000",
                    quantidadeDepois: "500.000000",
                },
            ];
            this.detail.separacoes = (this.detail.separacoes as Values[]).map(
                (row) => ({ ...row, situacao: "RETORNADA" }),
            );
            if (
                isObject(this.detail.pedido) &&
                Array.isArray(this.detail.pedido.reservas)
            )
                this.detail.pedido.reservas = this.detail.pedido.reservas.map(
                    (row) => ({
                        ...row,
                        situacao: "CANCELADA",
                        encerradaEm: "2026-10-08T12:35:00.123456Z",
                    }),
                );
        }
        if (id === "ExpedicaoController.devolver") {
            data.pedidoEntradaId = "102";
            this.detail.devolucoes = [
                {
                    ...(fixture("ExpedicaoDto.Devolucao") as Values),
                    id: "1901",
                    baixaId: "1701",
                    pedidoEntradaId: "102",
                    entradaNovaId: "202",
                    quantidade: "2.000000",
                    dataFifo: "2026-10-07T12:00:00.123456Z",
                },
            ];
        }
        if (id === "ExpedicaoController.separar")
            this.detail.separacoes = [
                {
                    ...(fixture("ExpedicaoDto.Separacao") as Values),
                    id: "1501",
                    reservaId: "701",
                    situacao: "SEPARADA",
                    revisaoConteudoLida: "7",
                },
            ];
        data.expedicao = structuredClone(this.detail);
        return data;
    }
}
