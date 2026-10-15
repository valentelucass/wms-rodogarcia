import { isObject, type Values } from "../../contracts/runtime";
import type { Request } from "../../api/client";
import { fixture } from "../../api/mock/fixtures";
export class ExampleServiceConfiguration {
    private service: Values | undefined;
    private table: Values | undefined;
    private link: Values | undefined;
    private fact: Values | undefined;
    respond(r: Request, body: Values, data: unknown): unknown {
        const id = r.endpoint.id;
        if (
            id === "ConfiguracaoCobrancaController.criarServico" &&
            isObject(data)
        ) {
            this.service = {
                ...data,
                id: "2501",
                codigo: body.codigo,
                descricao: body.descricao,
                tipo: body.tipo,
                unidade: body.unidade,
            };
            return this.service;
        }
        if (
            id === "ConfiguracaoCobrancaController.servicos" &&
            isObject(data) &&
            this.service
        )
            return { ...data, itens: [this.service] };
        if (
            [
                "ConfiguracaoCobrancaController.criarTabela",
                "ConfiguracaoCobrancaController.alterar",
            ].includes(id) &&
            isObject(data)
        ) {
            this.table = {
                ...data,
                id: id.endsWith(".alterar") ? r.params?.id : "2601",
                versao: id.endsWith(".alterar")
                    ? String(BigInt(String(body.versao)) + 1n)
                    : "0",
                codigo: body.codigo ?? this.table?.codigo ?? data.codigo,
                descricao: body.descricao,
                clienteId: body.clienteId,
                armazemId: body.armazemId,
                tipo: body.tipo,
                vigenciaInicio: body.vigenciaInicio,
                vigenciaFim: body.vigenciaFim,
                itens: (Array.isArray(body.itens) ? body.itens : [])
                    .filter(isObject)
                    .map((item, index) => ({
                        ...(fixture(
                            "ConfiguracaoCobrancaDto.ItemResposta",
                        ) as Values),
                        ...item,
                        id: String(2602 + index),
                    })),
            };
            return this.table;
        }
        if (id === "ConfiguracaoCobrancaController.tabela" && this.table)
            return this.table;
        if (id === "ConfiguracaoCobrancaController.tabelas" && this.table)
            return [this.table];
        if (
            id === "ConfiguracaoCobrancaController.vincular" &&
            isObject(data)
        ) {
            this.link = {
                ...data,
                id: "2701",
                tabelaId: body.tabelaId,
                vigenciaInicio: body.vigenciaInicio,
                vigenciaFim: body.vigenciaFim,
            };
            return this.link;
        }
        if (id === "ConfiguracaoCobrancaController.vinculos" && this.link)
            return [this.link];
        if (id === "FatoServicoController.sugestoes")
            return {
                itens: [
                    {
                        ...(fixture("FatoServicoDto.Sugestao") as Values),
                        servicoId: this.service?.id ?? "2501",
                        unidadeId: "501",
                        pedidoSaidaId: "601",
                        produtoId: "11",
                        chaveFato: "FATO-FICTICIO-SERVICO-501",
                        quantidade: "2.123456",
                        categoria: "",
                        pendencia: null,
                    },
                ],
                pagina: 0,
                tamanho: 20,
                totalItens: "1",
                totalPaginas: 1,
            };
        if (id === "FatoServicoController.registrar" && isObject(data)) {
            this.fact = {
                ...data,
                id: "2801",
                servicoId: body.servicoId,
                unidadeId: body.unidadeId,
                pedidoSaidaId: body.pedidoSaidaId,
                referenciaExecucao: body.referenciaExecucao,
                origem: body.origem,
                executadoEm: body.executadoEm,
                quantidade: body.quantidade,
                categoria: body.categoria,
                valorBase: body.valorBase,
                situacao: "VALIDO",
            };
            return this.fact;
        }
        if (id === "FatoServicoController.consultar" && this.fact)
            return this.fact;
        return data;
    }
}
