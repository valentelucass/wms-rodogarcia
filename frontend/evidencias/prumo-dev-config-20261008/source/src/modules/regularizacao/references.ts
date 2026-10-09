import { isObject, type Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
export function regularizationReferences(
    ctx: Values,
    controller: string,
    state: Workflow,
) {
    const s = state.selected;
    if (controller === "ContagemController") {
        const count = s["ContagemDto.Resultado"];
        if (count) {
            ctx.id = count.id;
            ctx.revisao = count.revisao;
        }
    }
    if (controller === "CargaInicialController") {
        const load = s["CargaInicialDto.Resultado"];
        if (load && isObject(load.revisao)) {
            ctx.revisao = load.revisao.numero;
            ctx.conteudoHash = load.revisao.conteudoHash;
            ctx.dados = load.revisao.dados;
            ctx.etiquetasUnidades = load.etiquetas;
            ctx.leitura = load.etiquetaFornecida;
        }
    }
    if (controller === "ContingenciaController") {
        const fact = s["ContingenciaDto.Resultado"];
        if (fact) ctx.conteudoHash = fact.conteudoHash;
    }
}
