import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { ApiError } from "../../api/client";
import { stringifyExact } from "../../contracts/codec";
export class ExampleContingency {
    private fact: Values | undefined;
    private registeredContent = "";
    private original: Values | undefined;
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
                return structuredClone(this.original);
            }
            this.registeredContent = fingerprint;
            this.fact = {
                ...data,
                id: "2401",
                versao: "0",
                identidadeFato: body.identidadeFato,
                clienteId: body.clienteId,
                armazemId: body.armazemId,
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
            this.original = structuredClone(this.fact);
            return this.fact;
        }
        if (id === "ContingenciaController.consultar" && this.fact)
            return this.fact;
        if (id === "ContingenciaController.conciliar" && this.fact) {
            if (body.versao !== this.fact.versao)
                throw new ApiError(
                    "Exercício FE12: versão divergente; consulte o fato atual.",
                    409,
                    "REVISAO_DIVERGENTE",
                );
            if (
                body.modo === "VINCULAR" &&
                (!isObject(body.prova) ||
                    !body.prova.operacaoOriginal ||
                    body.prova.conteudoHash !== this.fact.conteudoHash)
            )
                throw new ApiError(
                    "Exercício FE12: informe a prova da operação original e o hash consultado. Nenhum efeito confirmado.",
                    409,
                    "PROVA_AUSENTE",
                );
            if (body.modo !== "EXECUTAR" && body.modo !== "VINCULAR")
                throw new ApiError(
                    "Exercício FE12: selecione o modo previsto no contrato.",
                    422,
                    "MODO_INVALIDO",
                );
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
                          modo: body.modo,
                          prova: structuredClone(body.prova ?? null),
                      },
                  };
            return this.fact;
        }
        return data;
    }
}
