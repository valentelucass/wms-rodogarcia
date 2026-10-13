import { isObject, type Values } from "../../contracts/runtime";
import type { Workflow } from "../../domain/workflow";
import { ownedSelection } from "../../domain/workflowSelectors";
export function receivingReferences(
    ctx: Values,
    controller: string,
    action: string,
    state: Workflow,
) {
    const selected = state.selected,
        entry = selected["PedidoEntradaDto.Resumo"],
        incoming = ownedSelection(
            state,
            "RecebimentoDto.Entrada",
            "PedidoEntradaDto.Resumo",
        ),
        unit = selected["UnidadeLogisticaDto.Resumo"];
    if (entry) {
        ctx.pedidoEntradaId = entry.id;
        ctx.pedidoId = entry.id;
        ctx.versaoPedido = entry.versao;
    }
    ctx.entradaId = undefined;
    if (incoming) {
        ctx.entradaId = incoming.id;
        ctx.produtoId = incoming.produtoId;
        if (
            controller === "UnidadeLogisticaController" &&
            action === "unitizar" &&
            isObject(incoming.bobinas)
        ) {
            const boas = Number(incoming.bobinas.boas),
                avariadas = Number(incoming.bobinas.avariadas);
            if (
                Number.isInteger(boas) &&
                Number.isInteger(avariadas) &&
                boas >= 0 &&
                avariadas >= 0 &&
                boas + avariadas <= 100
            ) {
                ctx.unidades = Array.from(
                    { length: boas + avariadas },
                    (_, i) => ({
                        embalagemId: "",
                        tipo: "BOBINA",
                        condicao: i < boas ? "BOA" : "AVARIADA",
                        quantidade: "",
                    }),
                );
            }
        }
    }
    if (controller === "UnidadeLogisticaController") {
        if (["consultar", "dividir", "reagrupar"].includes(action))
            ctx.pedidoId = unit?.pedidoId;
        if (action === "dividir") ctx.versao = unit?.versao;
        if (action === "reagrupar") ctx.versaoDestino = unit?.versao;
        if (action === "etiqueta")
            ctx.codigo =
                unit?.codigo ??
                ownedSelection(
                    state,
                    "PedidoSaidaDto.Reserva",
                    "PedidoSaidaDto.Detalhe",
                )?.codigoUnidade;
    }
    if (controller === "PedidoEntradaController" && action === "chegada") {
        const item = ownedSelection(
            state,
            "PedidoEntradaDto.ItemConferencia",
            "PedidoEntradaDto.Resumo",
        );
        if (item)
            ctx.itens = [
                {
                    itemNotaId: item.id,
                    lote: null,
                    validade: null,
                    quantidadeBoa: "",
                    quantidadeAvariada: "0",
                },
            ];
    }
    if (controller === "PedidoEntradaController" && action === "estornar")
        ctx.chegadaId = ownedSelection(
            state,
            "RecebimentoDto.Chegada",
            "PedidoEntradaDto.Resumo",
        )?.id;
}
