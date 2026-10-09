import { exampleValues } from "./exampleCatalog";
import { records, enums, listType, type Values } from "../../contracts/runtime";
import { demoCode } from "./constants";
const baseValues: Values = {
    id: "1",
    clienteId: "1",
    armazemId: "1",
    produtoId: "1",
    pedidoId: "1",
    pedidoEntradaId: "1",
    pedidoSaidaId: "1",
    unidadeId: "1",
    entradaId: "1",
    notaId: "1",
    reservaId: "1",
    versao: "0",
    versaoPedido: "0",
    versaoUnidade: "0",
    revisaoConteudo: "0",
    codigo: demoCode,
    codigoLido: demoCode,
    codigoUnidade: demoCode,
    sku: "DEMO-CAIXA",
    nome: "Exemplo fictício",
    descricao: "Mercadoria fictícia",
    referencia: "DEMO-01",
    quantidadeProduto: "10.000000",
    fisicoTotal: "10.000000",
    fisicoUnitizado: "10.000000",
    disponivel: "10.000000",
    reservado: "0.000000",
    bloqueado: "0.000000",
    naoEnderecado: "0.000000",
    emTriagem: "0.000000",
    emQuarentena: "0.000000",
    avariado: "0.000000",
    emArmazenagem: "10.000000",
    pendenteUnitizacao: "0.000000",
    unidadeMedida: "UN",
    posicoesEquivalentes: 1,
    posicoesNecessarias: 1,
    empilhamento: 1,
    rua: "A",
    nivel: 1,
    posicao: 1,
    sequenciaColeta: 1,
    documentoFiscal: "00000000000000",
    uf: "SP",
    cidade: "Cidade fictícia",
    lote: null,
    validade: null,
    retiradaEm: null,
    encerradaEm: null,
    canceladoEm: null,
    estornadaEm: null,
    conciliadaEm: null,
    bloqueada: false,
    avariaPosterior: false,
    avariaInicialReparada: false,
    divergente: false,
    valorConsultado: false,
    valorExato: null,
    pendencias: [],
    avisos: [],
    xmlVinculado: false,
    disponivelParaSaida: true,
    conteudoHash: "a".repeat(64),
};
export function fixture(type: string, depth = 0): unknown {
    if (depth > 9) return null;
    if (type.startsWith("PaginaResponse<"))
        return {
            itens: [fixture(type.slice(15, -1), depth + 1)],
            pagina: 0,
            tamanho: 20,
            totalItens: "1",
            totalPaginas: 1,
        };
    const list = listType(type);
    if (list) return [fixture(list, depth + 1)];
    if (records[type]) {
        const result: Values = {};
        for (const f of records[type]) {
            const v =
                f.name in baseValues && !records[f.type]
                    ? baseValues[f.name]
                    : fixture(f.type, depth + 1);
            result[f.name] =
                f.type === "String" && typeof v === "number" ? String(v) : v;
        }
        Object.assign(result, exampleValues(type));
        return result;
    }
    if (enums[type]) return enums[type][0];
    if (["Long", "long"].includes(type)) return "1";
    if (["Integer", "int"].includes(type)) return 1;
    if (type === "BigDecimal") return "10.000000";
    if (["boolean", "Boolean"].includes(type)) return false;
    if (type === "UUID") return demoCode;
    if (type === "Instant") return "2026-10-07T12:00:00.123456Z";
    if (type === "LocalDate") return "2026-10-07";
    if (type === "Object" || type === "unknown" || type.startsWith("Map<"))
        return {
            ficticio: true,
            descricao: "Resposta de exercício; sem efeito real",
        };
    return "Exemplo fictício";
}
