import { fixture } from "../../api/mock/fixtures";
import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
// Respostas preparadas para exercício; não interpreta Excel nem compromissos.
export class ExampleRegistry {
    private client: Values | undefined;
    private preview: Values | undefined;
    private ending: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (id === "ClienteController.alterar" && isObject(data)) {
            this.client = { ...data, nome: body.nome, versao: "1" };
            return this.client;
        }
        if (id === "ClienteController.consultar" && this.client)
            return this.client;
        if (id === "ImportacaoEnderecoController.previa" && isObject(data)) {
            const errors = r.file?.name.includes("erros")
                ? [
                      {
                          ...(fixture("ImportacaoEnderecoDto.Erro") as Values),
                          linha: 2,
                          coluna: "codigo",
                          codigo: "CODIGO_INVALIDO",
                          mensagem: "Exercício: código inválido na linha 2",
                      },
                  ]
                : [];
            this.preview = {
                ...data,
                id: "2101",
                armazemId: r.params.id,
                versao: "0",
                arquivoHash: "a".repeat(64),
                situacao: errors.length ? "COM_ERROS" : "VALIDA",
                erros: errors,
                enderecos: [],
                confirmadaEm: null,
            };
            return this.preview;
        }
        if (id === "ImportacaoEnderecoController.consultar" && this.preview)
            return this.preview;
        if (id === "ImportacaoEnderecoController.confirmar" && this.preview) {
            this.preview = {
                ...this.preview,
                versao: "1",
                situacao: "CONFIRMADA",
                confirmadaEm: "2026-10-08T12:00:00.123456Z",
                enderecos: [fixture("EnderecoDto.Resposta")],
            };
            return this.preview;
        }
        if (id.startsWith("EncerramentoController.") && isObject(data)) {
            if (id.endsWith("consultar"))
                return (
                    this.ending ?? {
                        ...data,
                        tipo: r.params.tipo,
                        id: r.params.id,
                        impedimentos: [],
                    }
                );
            this.ending = {
                ...data,
                tipo: r.params.tipo,
                id: r.params.id,
                versao: id.endsWith("inativar") ? "2" : "1",
                situacao: id.endsWith("inativar")
                    ? "INATIVO"
                    : "ENCERRAMENTO_SOLICITADO",
                impedimentos: [],
            };
            return this.ending;
        }
        return data;
    }
}
