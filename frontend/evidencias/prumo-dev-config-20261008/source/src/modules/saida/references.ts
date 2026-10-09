import type { Values } from "../../contracts/runtime";
import { ownedSelection, type Workflow } from "../../domain/workflow";
export function dispatchReferences(
    ctx: Values,
    controller: string,
    action: string,
    state: Workflow,
) {
    const s = state.selected,
        outgoing = s["PedidoSaidaDto.Detalhe"],
        reserve = ownedSelection(
            state,
            "PedidoSaidaDto.Reserva",
            "PedidoSaidaDto.Detalhe",
        ),
        unit = s["UnidadeLogisticaDto.Resumo"],
        position = s["EnderecoDto.Resposta"];
    const candidate = ownedSelection(
        state,
        "PedidoSaidaDto.Sugestao",
        "PedidoSaidaDto.Detalhe",
    );
    const suggestion =
        candidate?.versao === outgoing?.versao ? candidate : undefined;
    if (
        controller === "PedidoSaidaController" &&
        action === "reservar" &&
        suggestion
    )
        ctx.versao = suggestion.versao;
    if (controller === "PedidoSaidaController" && action === "justificar")
        ctx.selecoes = suggestion?.selecoes;
    if (controller === "PedidoSaidaController" && action === "reservar")
        ctx.justificativaId = state.fifoException && state.fifoException.pedidoId === outgoing?.id && state.fifoException.versao === outgoing?.versao ? state.fifoException.id : undefined;
    if (controller !== "ExpedicaoController") return;
    ctx.id = outgoing?.id;
    ctx.versao = outgoing?.versao;
    ctx.reservaId = reserve?.id;
    ctx.codigoLido = reserve?.codigoUnidade;
    ctx.revisaoConteudo = undefined;
    const label = s["UnidadeLogisticaDto.Etiqueta"];
    if (label && label.codigoLeitura === ctx.codigoLido)
        ctx.revisaoConteudo = label.versaoConteudo;
    ctx.documentoId = ownedSelection(
        state,
        "ExpedicaoDto.DocumentoRegistrado",
        "PedidoSaidaDto.Detalhe",
    )?.id;
    ctx.destinacao = undefined;
    ctx.coberturas = undefined;
    if (action === "separar" && reserve)
        ctx.destinacao = {
            reservaId: reserve.id,
            conjuntoId: null,
            destinos: position
                ? [{ enderecoId: position.id, codigoLido: position.codigo }]
                : [],
        };
    if (action === "documento" && reserve)
        ctx.coberturas = [
            {
                reservaId: reserve.id,
                notaOrigemId: reserve.notaOrigemId,
                sku:
                    unit?.id === reserve.unidadeId &&
                    unit.codigo === reserve.codigoUnidade
                        ? unit.sku
                        : label?.codigoLeitura === reserve.codigoUnidade
                          ? label.sku
                          : (state.catalogs["PedidoSaidaDto.Item"]?.find(
                                (item) => item.id === reserve.itemId,
                            )?.sku ?? ""),
                quantidade: reserve.quantidade,
            },
        ];
    if (["retirar", "retornar"].includes(action) && reserve && position) {
        // Oferece o destino consultado ao operador quando ele adiciona a linha.
        // Não decide quais unidades têm remanescente ou precisam retornar.
        ctx.destinos = [
            { enderecoId: position.id, codigoLido: position.codigo },
        ];
    }
    if (action === "devolver") {
        const detail = s["ExpedicaoDto.Detalhe"];
        const rows =
            detail && Array.isArray(detail.baixas) ? detail.baixas : [];
        if (
            rows.length === 1 &&
            rows[0] &&
            typeof rows[0] === "object" &&
            "id" in rows[0]
        )
            ctx.itens = [
                {
                    baixaId: rows[0].id,
                    quantidade: "",
                    quantidadeAvariada: "0.000000",
                },
            ];
    }
}
