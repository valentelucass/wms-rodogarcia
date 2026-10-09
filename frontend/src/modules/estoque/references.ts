import type { Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
import { ownedSelection } from "../../domain/workflowSelectors";

export function stockReferences(
    ctx: Values,
    controller: string,
    state: Workflow,
) {
    if (
        controller === "EstoqueController" &&
        !state.selected["UnidadeLogisticaDto.Resumo"]
    ) {
        const label = state.selected["UnidadeLogisticaDto.Etiqueta"];
        const reserve = ownedSelection(
            state,
            "PedidoSaidaDto.Reserva",
            "PedidoSaidaDto.Detalhe",
        );
        if (label && reserve && label.codigoLeitura === reserve.codigoUnidade) {
            ctx.codigo = label.codigoLeitura;
            ctx.codigoLido = label.codigoLeitura;
            ctx.codigoUnidade = label.codigoLeitura;
            // Revisão da etiqueta não é versão da unidade: a leitura seguinte
            // consulta a unidade atual antes de oferecer confirmação.
        }
    }
    if (controller !== "AvariaController") return;
    const unit = state.selected["UnidadeLogisticaDto.Resumo"];
    const damage = state.selected["AvariaDto.Ocorrencia"];
    ctx.id = damage?.unidadeId === unit?.id ? damage?.id : undefined;
    ctx.versao = damage?.unidadeId === unit?.id ? damage?.versao : undefined;
    const position = state.selected["EnderecoDto.Resposta"];
    if (position)
        ctx.destinos = [
            { enderecoId: position.id, codigoLido: position.codigo },
        ];
}
