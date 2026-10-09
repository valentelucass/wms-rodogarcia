import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { ApiError } from "../../api/client";
import { stringifyExact } from "../../contracts/codec";
export class ExampleContingency {
    private fact: Values | undefined;
    private registeredContent = "";
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "ContingenciaController.registrar" && isObject(data)) {
            const { operacaoId: _operation, ...content } = body;
            void _operation;
            const fingerprint = stringifyExact(content);
            if (this.fact?.identidadeFato === body.identidadeFato) {
                if (this.registeredContent !== fingerprint)
                    throw new ApiError(
                        "Exercício: identidade do fato já registrada com conteúdo divergente.",
                        409,
                        "CONTINGENCIA_DIVERGENTE",
                    );
                return this.fact;
            }
            this.registeredContent = fingerprint;
            this.fact = {
                ...data,
                id: "2401",
                versao: "0",
                identidadeFato: body.identidadeFato,
                tipo: body.tipo,
                ocorridaEm: body.ocorridaEm,
                conteudoHash: "d".repeat(64),
                conteudo: {
                    dados: structuredClone(body.dados),
                    dependencias: body.dependencias,
                    efeitoRegistradoNoWms: body.efeitoRegistradoNoWms,
                },
                situacao: "PENDENTE",
                pendencia: null,
                conciliadaEm: null,
                resultado: null,
            };
            return this.fact;
        }
        if (id === "ContingenciaController.consultar" && this.fact)
            return this.fact;
        if (id === "ContingenciaController.conciliar" && this.fact) {
            const unresolved =
                isObject(this.fact.conteudo) &&
                Array.isArray(this.fact.conteudo.dependencias) &&
                this.fact.conteudo.dependencias.length > 0;
            this.fact = unresolved
                ? {
                      ...this.fact,
                      pendencia:
                          "Exercício: dependência não conciliada; nenhum efeito confirmado",
                  }
                : {
                      ...this.fact,
                      versao: "1",
                      situacao: "CONCILIADA",
                      pendencia: null,
                      conciliadaEm: "2026-10-08T14:00:00.123456Z",
                      resultado: {
                          exercicio: "chegada registrada pelo roteiro fictício",
                      },
                  };
            return this.fact;
        }
        return data;
    }
}
