import { fixture } from "../../api/mock/fixtures";
import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
// Respostas preparadas para exercício; não interpreta Excel nem compromissos.
export class ExampleRegistry {
    private registrations = new Map<string, Values[]>();
    private fiscal = new Map<string, Values>();
    private preview: Values | undefined;
    private ending: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        const [controller, command] = id.split(".");
        const entity = controller.replace("Controller", "");
        if (
            ["Cliente", "Armazem", "Produto", "Embalagem", "Endereco"].includes(
                entity,
            )
        ) {
            let rows = this.registrations.get(entity);
            if (!rows) {
                const template = fixture(`${entity}Dto.Resposta`) as Values;
                rows = ["Cliente", "Armazem"].includes(entity)
                    ? [1, 2].map((i) => ({
                          ...template,
                          id: String(i),
                          codigo: `DEMO-${i}`,
                          nome:
                              entity === "Cliente"
                                  ? `Cliente fictício ${i}`
                                  : i === 1
                                    ? "Centro fictício"
                                    : "Anexo fictício",
                          situacao: "ATIVO",
                          versao: "0",
                      }))
                    : [{ ...template }];
                this.registrations.set(entity, rows);
            }
            if (command === "listar") {
                const filtered = rows.filter(
                    (row) =>
                        (!r.query.situacao ||
                            row.situacao === r.query.situacao) &&
                        (!r.query.clienteId ||
                            !row.clienteId ||
                            row.clienteId === r.query.clienteId) &&
                        (!r.query.armazemId ||
                            !row.armazemId ||
                            row.armazemId === r.query.armazemId),
                );
                const pagina = Number(r.query.pagina ?? 0),
                    tamanho = Number(r.query.tamanho ?? 20);
                return {
                    itens: filtered.slice(
                        pagina * tamanho,
                        (pagina + 1) * tamanho,
                    ),
                    pagina,
                    tamanho,
                    totalItens: String(filtered.length),
                    totalPaginas: Math.ceil(filtered.length / tamanho),
                };
            }
            if (command === "criar") {
                const nextId = String(
                    rows.reduce(
                        (max, row) =>
                            BigInt(String(row.id)) > max
                                ? BigInt(String(row.id))
                                : max,
                        0n,
                    ) + 1n,
                );
                const created = {
                    ...(fixture(`${entity}Dto.Resposta`) as Values),
                    ...body,
                    id: nextId,
                    versao: "0",
                    situacao: "ATIVO",
                };
                rows.push(created);
                return created;
            }
            const row = rows.find((row) => row.id === r.params.id);
            if (!row) throw new Error("Exercício: cadastro não encontrado.");
            if (command === "consultar") return row;
            if (command === "alterar")
                for (const field of ["nome", "descricao"])
                    if (body[field] !== undefined) row[field] = body[field];
            if (command === "encerrar") row.situacao = "ENCERRAMENTO_PENDENTE";
            if (command === "reativar") row.situacao = "ATIVO";
            row.versao = String(BigInt(String(row.versao)) + 1n);
            return row;
        }
        if (controller === "FiscalCadastroController" && isObject(data)) {
            const family = command.toLowerCase().includes("armazem")
                ? "Armazem"
                : "Cliente";
            const key = family + ":" + r.params.id;
            if (
                [
                    "cliente",
                    "armazem",
                    "complementarCliente",
                    "complementarArmazem",
                ].includes(command)
            ) {
                const previous = this.fiscal.get(key) ?? {
                    ...data,
                    id: r.params.id,
                    versao: "0",
                };
                const current = command.startsWith("complementar")
                    ? {
                          ...previous,
                          dados: body.dados,
                          versao: String(BigInt(String(previous.versao)) + 1n),
                      }
                    : previous;
                this.fiscal.set(key, current);
                return current;
            }
        }
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
