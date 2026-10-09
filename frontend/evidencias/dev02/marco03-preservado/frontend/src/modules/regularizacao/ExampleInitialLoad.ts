import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { ApiError } from "../../api/client";
export class ExampleInitialLoad {
    private load: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "CargaInicialController.criar" && isObject(data)) {
            this.load = {
                ...data,
                id: "2301",
                versao: "0",
                clienteId: body.clienteId,
                armazemId: body.armazemId,
                produtoId: body.produtoId,
                referencia: body.referencia,
                etiquetaFornecida: body.etiquetaFornecida,
                quantidadeEstagio: body.quantidade,
                situacao: "PENDENTE",
                entradaId: null,
                etiquetas: [],
                pendencias: [
                    "Origem, nota e FIFO não informados no levantamento fictício",
                ],
                revisao: {
                    numero: 1,
                    conteudoHash: "b".repeat(64),
                    dados: structuredClone(body.dados),
                },
            };
            return this.load;
        }
        if (id === "CargaInicialController.revisar" && this.load) {
            // An explicitly known fictitious entry selects the prepared receipt.
            // Other evidence stays pending; the responder does not regularize data.
            const known =
                isObject(body.dados) && body.dados.entradaExistenteId === "201";
            this.load = {
                ...this.load,
                versao: "1",
                revisao: {
                    numero: 2,
                    conteudoHash: "c".repeat(64),
                    dados: structuredClone(body.dados),
                },
                pendencias: known
                    ? []
                    : [
                          "Exercício FE12: origem ainda não conhecida. O roteiro preparado usa a entrada fictícia 201; não invente dados reais.",
                      ],
            };
            return this.load;
        }
        if (id === "CargaInicialController.preparar" && this.load) {
            if (
                Array.isArray(this.load.pendencias) &&
                this.load.pendencias.length
            )
                throw new ApiError(
                    "Exercício FE12: pendências de origem impedem a preparação; nenhum efeito confirmado.",
                    409,
                    "CARGA_PENDENTE",
                );
            this.checkRevision(body);
            this.load = {
                ...this.load,
                versao: "2",
                situacao: "PREPARADA",
                entradaId: "201",
                etiquetas: ["00000000-0000-4000-8000-000000000003"],
            };
            return this.load;
        }
        if (id === "CargaInicialController.confirmar" && this.load) {
            if (this.load.situacao !== "PREPARADA")
                throw new ApiError(
                    "Exercício FE12: carga ainda não preparada.",
                    409,
                    "CARGA_PENDENTE",
                );
            this.checkRevision(body);
            if (
                body.leitura !== this.load.etiquetaFornecida ||
                !Array.isArray(body.etiquetasUnidades) ||
                body.etiquetasUnidades.length !== 1 ||
                body.etiquetasUnidades[0] !==
                    "00000000-0000-4000-8000-000000000003"
            )
                throw new ApiError(
                    "Exercício FE12: confira leitura e todas as etiquetas preparadas; nenhum efeito confirmado.",
                    409,
                    "LEITURA_DIVERGENTE",
                );
            this.load = {
                ...this.load,
                versao: "3",
                situacao: "REGULARIZADA",
            };
            return this.load;
        }
        if (id === "CargaInicialController.consultar" && this.load)
            return this.load;
        return data;
    }
    private checkRevision(body: Values) {
        const revision = this.load?.revisao;
        if (
            !isObject(revision) ||
            body.versao !== this.load?.versao ||
            body.revisao !== revision.numero ||
            body.conteudoHash !== revision.conteudoHash
        )
            throw new ApiError(
                "Exercício FE12: versão, revisão ou hash divergentes; consulte novamente.",
                409,
                "REVISAO_DIVERGENTE",
            );
    }
}
