import { isObject, type Values } from "../contracts/runtime";
import { absorb, operationContext, type Workflow } from "./workflow";

export function recordRoot(row: Values) {
    for (const key of [
        "pedido",
        "unidade",
        "fechamento",
        "expedicao",
        "estoque",
    ])
        if (isObject(row[key])) return recordRoot(row[key]);
    return row;
}
export function recordIdentity(row: Values) {
    const root = recordRoot(row);
    return String(
        root.id ??
            root.produtoId ??
            root.codigo ??
            root.chaveFato ??
            root.sku ??
            JSON.stringify(root),
    );
}
export function recordTitle(row: Values) {
    const root = recordRoot(row);
    return String(
        root.nome ??
            root.descricao ??
            root.referencia ??
            root.codigo ??
            root.id ??
            "Registro consultado",
    );
}
export function rowContext(
    action: string,
    base: Values,
    workflow: Workflow,
    row: Values,
    type: string,
    bind = true,
): Values {
    const root = recordRoot(row);
    const next = bind ? absorb(workflow, type, row, true) : workflow;
    const derived = operationContext(action, base, next);
    const controller = action.split(".")[0];
    // Only fields of the command's actual subject may override derived references.
    // A unit's ID is not the ID of its damage occurrence or its financial fact.
    const subject = controller.replace("Controller", "");
    const sameSubject =
        type.startsWith(subject + "Dto.") ||
        (controller === "PedidoEntradaController" &&
            type === "PedidoEntradaDto.Detalhe") ||
        (["ExpedicaoController", "PedidoSaidaController"].includes(
            controller,
        ) &&
            ["ExpedicaoDto.Detalhe", "PedidoSaidaDto.Detalhe"].includes(
                type,
            )) ||
        (controller === "ConfiguracaoCobrancaController" &&
            type.startsWith("ConfiguracaoCobrancaDto."));
    const values: Values = { ...derived, ...(sameSubject ? root : {}) };
    if (sameSubject) {
        // Client/warehouse always belong to the selected subject, including nullable values.
        for (const key of ["clienteId", "armazemId"])
            if (key in root) values[key] = root[key];
    }
    if (controller === "EncerramentoController") {
        const kinds: Record<string, string> = {
            "ClienteDto.Resposta": "CLIENTE",
            "ArmazemDto.Resposta": "ARMAZEM",
            "ProdutoDto.Resposta": "PRODUTO",
            "EmbalagemDto.Resposta": "EMBALAGEM",
            "EnderecoDto.Resposta": "ENDERECO",
        };
        if (kinds[type]) values.tipo = kinds[type];
        if (kinds[type]) {
            values.id = root.id;
            values.versao = root.versao;
        }
    }
    if (
        controller === "FiscalCadastroController" &&
        [
            "ClienteDto.Resposta",
            "ArmazemDto.Resposta",
            "ProdutoDto.Resposta",
            "FiscalCadastroDto.Complemento",
        ].includes(type)
    ) {
        values.id = root.id;
        values.versao = root.versao;
        if (root.dados) values.dados = root.dados;
    }
    if (controller === "UnidadeLogisticaController") {
        if (
            type.startsWith("UnidadeLogisticaDto.") ||
            type === "EstoqueDto.Unidade"
        ) {
            values.unidadeId = root.id;
            values.pedidoId = root.pedidoId;
            values.codigo = root.codigo ?? root.codigoLeitura;
            values.versao = root.versao;
            if (action.endsWith("reagrupar"))
                values.versaoDestino = root.versao;
        }
    }
    if (
        controller === "EstoqueController" ||
        controller === "AvariaController"
    ) {
        values.codigo = root.codigo ?? root.codigoUnidade ?? derived.codigo;
        values.unidadeId = root.unidadeId ?? root.id;
        if (
            controller === "EstoqueController" &&
            (type === "EstoqueDto.Unidade" ||
                type.startsWith("UnidadeLogisticaDto."))
        )
            values.versao = root.versao;
    }
    if (controller === "IndicadorEstoqueController") {
        values.produtoId = root.produtoId ?? root.id;
    }
    if (type === "FechamentoCobrancaDto.Versao") {
        values.id = root.fechamentoId;
        values.numero = root.numero;
    }
    if (
        controller === "FatoServicoController" &&
        ["marcos", "marco"].includes(action.split(".")[1])
    ) {
        values.avariaId =
            type === "AvariaDto.Ocorrencia"
                ? root.id
                : (root.avariaId ??
                  next.selected["AvariaDto.Ocorrencia"]?.id ??
                  derived.avariaId);
        values.id = values.avariaId;
    }
    return values;
}
