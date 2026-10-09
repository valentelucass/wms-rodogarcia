import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
export class ExampleInitialLoad {
    private load: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "CargaInicialController.criar" && isObject(data)) {
            this.load = {
                ...data,
                id: "2301",
                versao: "0",
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
            this.load = {
                ...this.load,
                versao: "1",
                revisao: {
                    numero: 2,
                    conteudoHash: "c".repeat(64),
                    dados: structuredClone(body.dados),
                },
                pendencias: [],
            };
            return this.load;
        }
        if (id === "CargaInicialController.preparar" && this.load) {
            this.load = {
                ...this.load,
                versao: "2",
                situacao: "PREPARADA",
                etiquetas: ["00000000-0000-4000-8000-000000000003"],
            };
            return this.load;
        }
        if (id === "CargaInicialController.confirmar" && this.load) {
            this.load = {
                ...this.load,
                versao: "3",
                situacao: "REGULARIZADA",
                entradaId: "203",
            };
            return this.load;
        }
        if (id === "CargaInicialController.consultar" && this.load)
            return this.load;
        return data;
    }
}
