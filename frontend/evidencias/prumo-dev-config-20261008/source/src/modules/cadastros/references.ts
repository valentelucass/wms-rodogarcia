import type { Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
export function registryReferences(
    ctx: Values,
    controller: string,
    action: string,
    state: Workflow,
) {
    if (controller === "ImportacaoEnderecoController") {
        if (action === "previa") {ctx.id=ctx.armazemId;return;}
        const preview = state.selected["ImportacaoEnderecoDto.Resultado"];
        if (preview) ctx.arquivoHash = preview.arquivoHash;
    }
    if (controller === "EncerramentoController") {
        const ending = state.selected["EncerramentoDto.Resultado"];
        if (ending) {
            ctx.tipo = ending.tipo;
            ctx.id = ending.id;
            ctx.versao = ending.versao;
        }
    }
}
