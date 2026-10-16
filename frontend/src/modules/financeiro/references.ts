import { isObject, type Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
export function billingReferences(
    ctx: Values,
    controller: string,
    action: string,
    state: Workflow,
) {
    const s = state.selected,
        closure = s["FechamentoCobrancaDto.Fechamento"],
        candidate = s["FechamentoCobrancaDto.Versao"],
        version =
            candidate?.fechamentoId === closure?.id ? candidate : undefined,
        calc = s["CalculoCobrancaDto.Resultado"];
    if (controller === "ConfiguracaoCobrancaController") {
        const table = s["ConfiguracaoCobrancaDto.Tabela"],
            service = s["ConfiguracaoCobrancaDto.Servico"];
        if (action === "alterar" && table) {
            Object.assign(ctx, table);
            if (Array.isArray(table.itens))
                ctx.itens = table.itens
                    .filter(isObject)
                    .map(({ servicoId, categoria, preco, percentual }) => ({
                        servicoId,
                        categoria,
                        preco,
                        percentual,
                    }));
        }
        ctx.servicoId = service?.id;
        ctx.tabelaId = table?.id;
        const resource = action.includes("Vinculo")
            ? s["ConfiguracaoCobrancaDto.Vinculo"]
            : action.includes("Contrato")
              ? s["ConfiguracaoCobrancaDto.Contrato"]
              : table;
        if (resource) {
            ctx.id = resource.id;
            ctx.versao = resource.versao;
        }
    }
    if (controller === "FatoServicoController")
        ctx.servicoId = s["ConfiguracaoCobrancaDto.Servico"]?.id;
    if (controller === "FatoServicoController" && action === "registrar") {
        const suggestion = s["FatoServicoDto.Sugestao"];
        ctx.servicoId =
            suggestion?.servicoId ?? s["ConfiguracaoCobrancaDto.Servico"]?.id;
        if (suggestion) {
            ctx.origem = "SUGESTAO";
            ctx.unidadeId = suggestion.unidadeId;
            ctx.pedidoSaidaId = suggestion.pedidoSaidaId;
            ctx.produtoId = suggestion.produtoId;
            // A chave calculada da sugestão não é a referência de uma execução manual.
            // Entrada/saída exigem referenciaExecucao nula no contrato do serviço.
            ctx.referenciaExecucao = null;
            ctx.executadoEm = suggestion.executadoEm;
            ctx.quantidade = suggestion.quantidade;
            ctx.categoria = suggestion.categoria;
        }
    }
    if (controller === "FechamentoCobrancaController") {
        ctx.id = closure?.id;
        ctx.versao = closure?.versao;
        ctx.numero = version?.numero ?? closure?.versaoAtual;
        ctx.calculoId = calc?.id;
        ctx.arquivoHash = version?.conteudoHash;
        ctx.layoutVersao = 1;
        ctx.numeroResultado = version?.numero;
        const external = version?.nfse;
        if (action === "tratar" && Array.isArray(external) && external.length)
            ctx.referencias = external.filter(isObject).map((v) => ({
                referenciaId: v.id,
                situacao: "",
            }));
    }
    if (controller === "AjusteFechamentoController") {
        ctx.destinoFechamentoId = closure?.id;
        ctx.versaoDestino = closure?.versao;
        ctx.origemVersaoId = state.adjustmentOrigin?.id;
        ctx.calculoCorrigidoId = calc?.id;
    }
}
