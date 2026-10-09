import { receivingReferences } from "../modules/recebimento/references";
import { registryReferences } from "../modules/cadastros/references";
import { regularizationReferences } from "../modules/regularizacao/references";
import { dispatchReferences } from "../modules/saida/references";
import { billingReferences } from "../modules/financeiro/references";
import { stockReferences } from "../modules/estoque/references";
import { records, listType, isObject, type Values } from "../contracts/runtime";
import type { Request } from "../api/client";
import { dispatchSelection } from "../modules/saida/selection";
import { receivingSelection } from "../modules/recebimento/selection";
import { replaceAuthoritativeCollections } from "./selectionCollections";
import { applyAuditTarget } from "../modules/relatorios/auditTargets";
import { selectionKey as rowKey } from "./workflowSelectors";
export { ownedSelection, referenceCatalog } from "./workflowSelectors";
interface Owner {
    root: string;
    id: unknown;
    noteId?: unknown;
}
const families = [receivingSelection, dispatchSelection];
export interface Workflow {
    selected: Record<string, Values>;
    catalogs: Record<string, Values[]>;
    owners: Record<string, Owner>;
    selectionRevision: number;
    followUps: Record<string, Values>;
    adjustmentOrigin?: Values;
    fifoException?: { pedidoId: unknown; versao: unknown; id: unknown };
}
export const emptyWorkflow = (): Workflow => ({
    selected: {},
    catalogs: {},
    owners: {},
    selectionRevision: 0,
    followUps: {},
});
export function absorb(
    state: Workflow,
    type: string,
    value: unknown,
    explicit = false,
    request?: Pick<Request, "endpoint" | "params" | "query">,
): Workflow {
    const next: Workflow = {
        selected: { ...state.selected },
        catalogs: { ...state.catalogs },
        owners: { ...state.owners },
        selectionRevision: state.selectionRevision + (explicit ? 1 : 0),
        followUps: { ...state.followUps },
        adjustmentOrigin: state.adjustmentOrigin,
        fifoException: state.fifoException,
    };
    if (explicit && type === "Auditoria.alvo" && isObject(value)) {
        applyAuditTarget(next, value);
        return next;
    }
    if (explicit && type === "AjusteFechamento.origem") {
        if (
            isObject(value) &&
            value.fechamentoId ===
                state.selected["FechamentoCobrancaDto.Fechamento"]?.id
        )
            next.adjustmentOrigin = structuredClone(value);
        return next;
    }
    const previous = state.selected;
    const invalidate = (root: string) => {
        const family = families.find((f) => f.root === root)!;
        for (const child of family.children) delete next.selected[child];
        for (const view of family.views) delete next.selected[view];
    };
    const controller = request?.endpoint.id.split(".")[0];
    const requestFamily = families.find((f) =>
        f.controllers.includes(controller ?? ""),
    );
    const requestedId =
        request?.params.id ??
        request?.params.pedidoId ??
        request?.query.pedidoId;
    const requestOwner =
        requestFamily && requestedId !== undefined
            ? { root: requestFamily.root, id: requestedId }
            : undefined;
    const visit = (
        t: string,
        v: unknown,
        choose: boolean,
        inherited?: Owner,
    ) => {
        const lt = listType(t);
        if (lt && Array.isArray(v)) {
            v.forEach((x) =>
                visit(
                    lt,
                    x,
                    choose &&
                        (v.length === 1 ||
                            (isObject(x) &&
                                x.id !== undefined &&
                                previous[lt]?.id === x.id)),
                    inherited,
                ),
            );
            return;
        }
        if (
            t.startsWith("PaginaResponse<") &&
            isObject(v) &&
            Array.isArray(v.itens)
        ) {
            visit("List<" + t.slice(15, -1) + ">", v.itens, choose, inherited);
            return;
        }
        if (!records[t] || !isObject(v)) return;
        const family = families.find(
            (f) =>
                f.root === t || f.children.includes(t) || f.details.includes(t),
        );
        // Uma seleção de linha usa a instância do catálogo atual. Um callback
        // de uma tela antiga não pode restaurar um filho removido/substituído.
        if (
            explicit &&
            !request &&
            t === type &&
            family?.children.includes(t) &&
            !(state.catalogs[t] ?? []).includes(v)
        )
            return;
        let owner = inherited;
        if (family?.details.includes(t)) {
            const parent =
                t === family.root
                    ? v
                    : isObject(v.pedido)
                      ? v.pedido
                      : undefined;
            if (parent?.id !== undefined) {
                owner = { root: family.root, id: parent.id };
                if (choose) {
                    invalidate(family.root);
                    replaceAuthoritativeCollections(
                        next,
                        family.root,
                        parent.id,
                        family.children,
                        t,
                        v,
                    );
                }
            }
        }
        if (family?.root === t && choose) {
            if (t === receivingSelection.root)
                delete next.followUps["PedidoEntradaController.consultar"];
            if (
                next.selected[t]?.id !== v.id ||
                (family.root === dispatchSelection.root &&
                    next.selected[t]?.versao !== v.versao)
            )
                invalidate(t);
            owner = { root: t, id: v.id };
        }
        const scopedChild = family?.children.includes(t);
        if (scopedChild) {
            owner ??= next.owners[rowKey(t, v)];
            if (
                choose &&
                (!owner || next.selected[owner.root]?.id !== owner.id)
            )
                choose = false;
            // Sugestão não tem ID próprio. Uma resposta de outro pedido não
            // pode sobrescrever a proveniência da sugestão vigente.
            if (t === "PedidoSaidaDto.Sugestao" && !choose) return;
            if (owner) next.owners[rowKey(t, v)] = owner;
        }
        if (
            v.id !== undefined ||
            [
                "PedidoSaidaDto.Sugestao",
                "UnidadeLogisticaDto.Etiqueta",
                "FatoServicoDto.Sugestao",
            ].includes(t)
        ) {
            const list = next.catalogs[t] ?? [];
            next.catalogs[t] = [
                ...list.filter((x) =>
                    t === "FatoServicoDto.Sugestao"
                        ? x.chaveFato !== v.chaveFato
                        : x.id !== v.id,
                ),
                v,
            ];
            if (choose) {
                next.selected[t] = v;
                if (t === "PedidoEntradaDto.Nota" && previous[t]?.id !== v.id)
                    delete next.selected["PedidoEntradaDto.ItemConferencia"];
                if (
                    t === "PedidoEntradaDto.ItemConferencia" &&
                    owner?.noteId !== undefined
                ) {
                    const noteId = owner.noteId;
                    const note = next.catalogs["PedidoEntradaDto.Nota"]?.find(
                        (x) => x.id === noteId,
                    );
                    if (note) next.selected["PedidoEntradaDto.Nota"] = note;
                }
            }
        }
        if (t === "PedidoEntradaDto.Nota" && owner)
            owner = { ...owner, noteId: v.id };
        for (const f of records[t])
            if (
                ![
                    "antes",
                    "dadosAntes",
                    "origens",
                    "calculo",
                    "resolucao",
                    "ajustes",
                    "versoesAnteriores",
                ].includes(f.name)
            )
                visit(f.type, v[f.name], choose, owner);
        if (
            ["ExpedicaoDto.Detalhe", "PedidoEntradaDto.Detalhe"].includes(t) &&
            choose
        )
            next.selected[t] = v;
    };
    visit(type, value, explicit || !Array.isArray(value), requestOwner);
    if (
        request?.endpoint.id === "PedidoSaidaController.justificar" &&
        isObject(value) &&
        isObject(value.pedido) &&
        value.pedido.id === request.params.id
    )
        next.fifoException = {
            pedidoId: value.pedido.id,
            versao: value.pedido.versao,
            id: value.operacaoId,
        };
    if (
        request?.endpoint.id === "ExpedicaoController.devolver" &&
        isObject(value) &&
        value.pedidoEntradaId != null
    ) {
        invalidate(receivingSelection.root);
        delete next.selected[receivingSelection.root];
        next.followUps["PedidoEntradaController.consultar"] = {
            id: value.pedidoEntradaId,
        };
    }
    if (type === "UnidadeLogisticaDto.Resultado" && isObject(value)) {
        const entry = next.selected["PedidoEntradaDto.Resumo"];
        if (entry && entry.id === value.pedidoId)
            next.selected["PedidoEntradaDto.Resumo"] = {
                ...entry,
                versao: value.versaoPedido,
            };
    }
    return next;
}
const aliases: Record<string, string> = {
    ClienteController: "ClienteDto.Resposta",
    ArmazemController: "ArmazemDto.Resposta",
    ProdutoController: "ProdutoDto.Resposta",
    EmbalagemController: "EmbalagemDto.Resposta",
    EnderecoController: "EnderecoDto.Resposta",
    PedidoEntradaController: "PedidoEntradaDto.Resumo",
    PedidoSaidaController: "PedidoSaidaDto.Detalhe",
    ExpedicaoController: "PedidoSaidaDto.Detalhe",
    CargaInicialController: "CargaInicialDto.Resultado",
    ContagemController: "ContagemDto.Resultado",
    ContingenciaController: "ContingenciaDto.Resultado",
    FechamentoCobrancaController: "FechamentoCobrancaDto.Fechamento",
    CalculoCobrancaController: "CalculoCobrancaDto.Resultado",
    FatoServicoController: "FatoServicoDto.Fato",
    ImportacaoEnderecoController: "ImportacaoEnderecoDto.Resultado",
    AvariaController: "AvariaDto.Ocorrencia",
};
export function operationContext(
    id: string,
    base: Values,
    state: Workflow,
): Values {
    const [controller, action] = id.split(".");
    const selected = state.selected;
    const unit = selected["UnidadeLogisticaDto.Resumo"];
    const ctx: Values = { ...base };
    const resource = selected[aliases[controller]];
    if (resource) {
        ctx.id = resource.id;
        if (resource.versao !== undefined) ctx.versao = resource.versao;
    }
    const product = selected["ProdutoDto.Resposta"];
    if (product) ctx.produtoId = product.id;
    const pack = selected["EmbalagemDto.Resposta"];
    if (pack) ctx.embalagemId = pack.id;
    const position = selected["EnderecoDto.Resposta"];
    if (position) ctx.enderecoId = position.id;
    if (unit) {
        ctx.unidadeId = unit.id;
        ctx.pedidoUnidadeId = unit.pedidoId;
        ctx.codigo = unit.codigo;
        ctx.codigoLido = unit.codigo;
        ctx.codigoUnidade = unit.codigo;
        ctx.versaoUnidade = unit.versao;
    }
    if (
        ["PedidoEntradaController", "UnidadeLogisticaController"].includes(
            controller,
        )
    )
        receivingReferences(ctx, controller, action, state);
    if (["PedidoSaidaController", "ExpedicaoController"].includes(controller))
        dispatchReferences(ctx, controller, action, state);
    registryReferences(ctx, controller, action, state);
    stockReferences(ctx, controller, state);
    regularizationReferences(ctx, controller, state);
    if (
        controller === "EstoqueController" &&
        action === "posicionar" &&
        position
    )
        ctx.destinos = [
            { enderecoId: position.id, codigoLido: position.codigo },
        ];
    if (
        [
            "FechamentoCobrancaController",
            "AjusteFechamentoController",
            "ConfiguracaoCobrancaController",
            "FatoServicoController",
        ].includes(controller)
    )
        billingReferences(ctx, controller, action, state);
    return Object.fromEntries(
        Object.entries({ ...ctx, ...state.followUps[id] }).filter(
            ([, v]) => v !== undefined,
        ),
    );
}
