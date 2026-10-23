import { isObject, records, listType, type Values } from "../contracts/runtime";
import { display, label } from "./labels";
import { formatProductQuantity } from "./quantityPresentation";
import { auditTargets } from "../modules/relatorios/auditTargets";
import { permittedUnitTypesLabel } from "./unitTypes";

export type ReferenceRecords = Record<string, Values[]>;
export const referenceTypes: Record<string, string[]> = {
    clienteId: ["ClienteDto.Resposta"],
    armazemId: ["ArmazemDto.Resposta"],
    produtoId: ["ProdutoDto.Resposta"],
    embalagemId: ["EmbalagemDto.Resposta"],
    enderecoId: ["EnderecoDto.Resposta"],
    enderecoAId: ["EnderecoDto.Resposta"],
    enderecoBId: ["EnderecoDto.Resposta"],
    conjuntoId: ["CapacidadeDto.Conjunto"],
    conjuntoOrigemId: ["CapacidadeDto.Conjunto"],
    pedidoEntradaId: ["PedidoEntradaDto.Resumo"],
    pedidoExistenteId: ["PedidoEntradaDto.Resumo"],
    pedidoSaidaId: ["PedidoSaidaDto.Detalhe"],
    pedidoResolucaoId: ["PedidoSaidaDto.Detalhe"],
    notaId: ["PedidoEntradaDto.Nota"],
    notaOrigemId: ["PedidoEntradaDto.Nota"],
    itemNotaId: ["PedidoEntradaDto.ItemConferencia"],
    itemId: ["PedidoSaidaDto.Item"],
    entradaId: ["RecebimentoDto.Entrada"],
    entradaExistenteId: ["RecebimentoDto.Entrada"],
    entradaOrigemId: ["RecebimentoDto.Entrada"],
    entradaNovaId: ["RecebimentoDto.Entrada"],
    chegadaId: ["RecebimentoDto.Chegada"],
    itemChegadaId: ["RecebimentoDto.ItemFisico"],
    unidadeId: ["UnidadeLogisticaDto.Resumo", "EstoqueDto.Unidade"],
    reservaId: ["PedidoSaidaDto.Reserva"],
    baixaId: ["ExpedicaoDto.Baixa"],
    documentoId: ["ExpedicaoDto.DocumentoRegistrado"],
    servicoId: ["ConfiguracaoCobrancaDto.Servico"],
    tabelaId: ["ConfiguracaoCobrancaDto.Tabela"],
    itemTabelaId: ["ConfiguracaoCobrancaDto.ItemResposta"],
    contratoId: ["ConfiguracaoCobrancaDto.Contrato"],
    fatoId: ["FatoServicoDto.Fato"],
    fatoPermanenciaId: ["FatoServicoDto.Fato"],
    avariaId: ["AvariaDto.Ocorrencia"],
    calculoId: ["CalculoCobrancaDto.Resultado"],
    calculoBaseId: ["CalculoCobrancaDto.Resultado"],
    calculoCorrigidoId: ["CalculoCobrancaDto.Resultado"],
    fechamentoId: ["FechamentoCobrancaDto.Fechamento"],
    destinoFechamentoId: ["FechamentoCobrancaDto.Fechamento"],
    destinoAjustesId: ["FechamentoCobrancaDto.Fechamento"],
    origemVersaoId: ["FechamentoCobrancaDto.Versao"],
    aplicadoVersaoId: ["FechamentoCobrancaDto.Versao"],
    versaoResultadoId: ["FechamentoCobrancaDto.Versao"],
    versaoBaseAnteriorId: ["FechamentoCobrancaDto.Versao"],
    entregaId: ["FechamentoCobrancaDto.Entrega"],
    tratativaOrigemId: ["FechamentoCobrancaDto.Tratativa"],
    cargaInicialId: ["CargaInicialDto.Resultado"],
    justificativaId: ["PedidoSaidaDto.Justificativa"],
};
export const technicalField = (key: string) =>
    /^(operacaoId|idOperacao|versao|versaoPedido|versaoDestino|versaoUnidade|revisaoConteudo)$/.test(
        key,
    );
export const referenceField = (key: string) => key === "id" || /Id$/.test(key);
export const presentationLabel = (key: string) =>
    label(key)
        .replace(/\s*\(ID\)/g, "")
        .replace(/ Id$/, "");

export function formatDate(value: unknown): string {
    if (typeof value !== "string") return display(value);
    if (/^\d{4}-\d{2}-\d{2}$/.test(value))
        return value.split("-").reverse().join("/");
    if (!/^\d{4}-\d{2}-\d{2}T.*(?:Z|[+-]\d{2}:\d{2})$/.test(value))
        return value;
    const time = new Date(value);
    return Number.isNaN(time.getTime())
        ? value
        : new Intl.DateTimeFormat("pt-BR", {
              timeZone: "America/Sao_Paulo",
              dateStyle: "short",
              timeStyle: "short",
          }).format(time);
}

/** Index only typed, already-authorized responses. IDs remain exact strings. */
export function indexRecords(
    data: unknown,
    type: string,
    into: ReferenceRecords = {},
): ReferenceRecords {
    const child = listType(type);
    if (child && Array.isArray(data)) {
        data.forEach((v) => indexRecords(v, child, into));
        return into;
    }
    if (!isObject(data)) return into;
    if (type.startsWith("PaginaResponse<"))
        return indexRecords(data.itens, `List<${type.slice(15, -1)}>`, into);
    if (data.id != null) {
        const list = (into[type] ??= []);
        const index = list.findIndex((v) => String(v.id) === String(data.id));
        if (index < 0) list.push(data);
        else list[index] = data;
    }
    for (const field of records[type] ?? [])
        if (isObject(data[field.name]) || Array.isArray(data[field.name]))
            indexRecords(data[field.name], field.type, into);
    return into;
}

export function typesForReference(
    key: string,
    type: string,
    row: Values = {},
): string[] {
    if (["registroId", "compromissoId"].includes(key)) {
        if (key === "compromissoId" && row.tipo === "FECHAMENTO")
            return ["FechamentoCobrancaDto.Fechamento"];
        const target = auditTargets.find((target) => target.tipo === row.tipo);
        return target ? [target.dto] : [];
    }
    if (key === "pedidoId")
        return type.startsWith("EstoqueDto.Confirmacao") ||
            /PedidoSaida|Expedicao/.test(type)
            ? ["PedidoSaidaDto.Detalhe"]
            : ["PedidoEntradaDto.Resumo"];
    if (key === "id") {
        if (type.includes("Dto.") && !type.endsWith(".params")) return [type];
        const subject = type.split(/[.]/)[0].replace("Controller", "Dto");
        const subjects: Record<string, string> = {
            ClienteDto: "ClienteDto.Resposta",
            ArmazemDto: "ArmazemDto.Resposta",
            ProdutoDto: "ProdutoDto.Resposta",
            EmbalagemDto: "EmbalagemDto.Resposta",
            EnderecoDto: "EnderecoDto.Resposta",
            PedidoEntradaDto: "PedidoEntradaDto.Resumo",
            PedidoSaidaDto: "PedidoSaidaDto.Detalhe",
            ExpedicaoDto: "PedidoSaidaDto.Detalhe",
            CargaInicialDto: "CargaInicialDto.Resultado",
            CalculoCobrancaDto: "CalculoCobrancaDto.Resultado",
            FechamentoCobrancaDto: "FechamentoCobrancaDto.Fechamento",
            FatoServicoDto: "FatoServicoDto.Fato",
        };
        return [subjects[subject] ?? type];
    }
    return referenceTypes[key] ?? [];
}

export function businessTitle(
    row: Values,
    type = "",
    refs: ReferenceRecords = {},
    depth = 0,
): string | undefined {
    if (depth > 3) return;
    if (isObject(row.pedido))
        return businessTitle(row.pedido, type, refs, depth + 1);
    if (isObject(row.unidade))
        return businessTitle(row.unidade, type, refs, depth + 1);
    const join = (...v: unknown[]) =>
        v
            .filter((x) => x != null && x !== "")
            .map(String)
            .join(" · ") || undefined;
    if (row.referencia) return String(row.referencia);
    if (row.nome) return join(row.nome, row.codigo);
    if (type === "PedidoEntradaDto.Nota")
        return row.numero != null
            ? `NF ${row.numero} · série ${row.serie ?? "—"}`
            : undefined;
    if (type === "PedidoEntradaDto.ItemConferencia")
        return join(
            row.numeroItem != null ? `Item ${row.numeroItem}` : undefined,
            row.sku,
        );
    if (type === "RecebimentoDto.Entrada") {
        return join(
            referenceText("notaId", row.notaId, row, type, refs, depth + 1),
            referenceText(
                "itemNotaId",
                row.itemNotaId,
                row,
                type,
                refs,
                depth + 1,
            ),
            formatDate(row.chegadaReal),
        );
    }
    if (type === "RecebimentoDto.Chegada")
        return join("Chegada", formatDate(row.chegouEm), row.observacao);
    if (type === "RecebimentoDto.ItemFisico")
        return join(
            referenceText(
                "itemNotaId",
                row.itemNotaId,
                row,
                type,
                refs,
                depth + 1,
            ),
            row.lote,
        );
    if (row.identidadeFato) return String(row.identidadeFato);
    if (type === "ConfiguracaoCobrancaDto.Servico" && row.descricao)
        return join(row.descricao, row.codigo);
    if (row.codigoDun) return join(row.codigoDun, row.descricao);
    if (row.sku && /Produto|Item/.test(type))
        return join(row.sku, row.descricao);
    if (row.codigo || row.codigoUnidade || row.codigoLeitura)
        return String(row.codigo ?? row.codigoUnidade ?? row.codigoLeitura);
    if (type === "FechamentoCobrancaDto.Versao" && row.numero != null)
        return `Versão ${row.numero}`;
    if (row.periodoInicio || row.vigenciaInicio)
        return join(
            formatDate(row.periodoInicio ?? row.vigenciaInicio),
            formatDate(row.periodoFim ?? row.vigenciaFim),
        );
    if (row.descricao) return String(row.descricao);
    return join(
        row.numeroItem != null ? `Item ${row.numeroItem}` : undefined,
        row.sku,
        row.executadoEm ? formatDate(row.executadoEm) : undefined,
        row.ocorridaEm ? formatDate(row.ocorridaEm) : undefined,
    );
}

export function referenceText(
    key: string,
    value: unknown,
    row: Values,
    type: string,
    refs: ReferenceRecords,
    depth = 0,
): string | undefined {
    if (value == null || value === "") return "Não informado";
    if (depth > 3) return;
    if (key === "produtoId" && row.sku) return String(row.sku);
    if (key === "unidadeId" && row.codigoUnidade)
        return String(row.codigoUnidade);
    if ((key === "notaId" || key === "notaOrigemId") && row.numeroNota)
        return `NF ${row.numeroNota}`;
    if (key === "id") {
        const own = businessTitle(row, type, refs, depth + 1);
        if (own) return own;
    }
    for (const target of typesForReference(key, type, row)) {
        const match = refs[target]?.find((v) => String(v.id) === String(value));
        if (match) return businessTitle(match, target, refs, depth + 1);
    }
    if (key === "produtoId") {
        const item = refs["PedidoEntradaDto.ItemConferencia"]?.find(
            (v) => String(v.produtoId) === String(value),
        );
        if (item?.sku) return String(item.sku);
    }
}

export function scalarText(
    key: string,
    value: unknown,
    row: Values,
    type: string,
): string {
    const field = records[type]?.find((f) => f.name === key);
    if (field?.type === "TiposUnidadePermitidos")
        return permittedUnitTypesLabel(value);
    if (
        field?.type === "Instant" ||
        field?.type === "LocalDate" ||
        (typeof value === "string" && /^\d{4}-\d{2}-\d{2}(T|$)/.test(value))
    )
        return formatDate(value);
    if (
        field?.type === "BigDecimal" &&
        /quantidade|peso|prevista|recebida|diferenca|esperado|contado/i.test(
            key,
        )
    )
        return formatProductQuantity(
            value,
            typeof row.unidadeMedida === "string"
                ? row.unidadeMedida
                : undefined,
        );
    return display(value);
}
