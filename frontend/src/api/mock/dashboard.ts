import type { DashboardDto_Resumo } from "../../contracts/types";
import type { Request } from "../client";
import type { Perfil } from "../../contracts/runtime";

/** Amostra declaradamente fictícia, sem ligação com API ou banco. */
export function dashboardExample(
    request: Request,
    perfil: Perfil,
    empty: boolean,
): DashboardDto_Resumo {
    const gestor = perfil === "GESTOR";
    const pagina = Number(request.query.pagina ?? 0);
    const samples = [
        [
            "DEMO-BOBINA",
            "KG",
            "12500.250000",
            "8300.125000",
            "2100.125000",
            "1700.000000",
            "400.000000",
        ],
        ["DEMO-CAIXA", "UN", "500", "280", "100", "100", "20"],
        [
            "DEMO-FILME",
            "KG",
            "780.500000",
            "600.250000",
            "80.250000",
            "100",
            "0",
        ],
        ["DEMO-PALETE", "UN", "120", "90", "20", "10", "0"],
        ["DEMO-PAPEL", "KG", "2200", "1200", "500", "300", "200"],
        ["DEMO-TUBO", "UN", "900", "650", "100", "100", "50"],
        ["DEMO-VOLUME", "UN", "0", "0", "0", "0", "0"],
    ];
    return {
        clienteId:
            request.query.clienteId == null
                ? null
                : String(request.query.clienteId),
        armazemId:
            request.query.armazemId == null
                ? null
                : String(request.query.armazemId),
        consultadoEm: new Date().toISOString(),
        fuso: String(request.query.fuso),
        posicoesCliente: empty ? "0" : "64",
        unidadesDisponiveis: empty ? "0" : "46",
        pedidosAbertos: empty ? "0" : "18",
        unidadesComAviso: empty ? "0" : "3",
        antecedenciaValidade: 15,
        visaoArmazem: gestor,
        areas: (
            [
                ["ARMAZENAGEM", "48", "100", "32"],
                ["TRIAGEM", "6", "20", "10"],
                ["QUARENTENA", "4", "10", "4"],
                ["SEPARACAO", "6", "20", "10"],
            ] as const
        ).map(([tipo, posicoes, capacidade, livres]) => ({
            tipo,
            posicoesCliente: empty ? "0" : posicoes,
            capacidadeAtiva: gestor ? capacidade : null,
            livresArmazem: gestor ? (empty ? capacidade : livres) : null,
        })),
        fila: (
            [
                ["RASCUNHO", "3"],
                ["RESERVADO", "8"],
                ["EM_SEPARACAO", "5"],
                ["SEPARADO", "2"],
            ] as const
        ).map(([situacao, pedidos]) => ({
            situacao,
            pedidos: empty ? "0" : pedidos,
        })),
        produtos: {
            pagina,
            tamanho: 6,
            totalItens: empty ? "0" : "7",
            totalPaginas: empty ? 0 : 2,
            itens: empty
                ? []
                : samples.slice(pagina * 6, pagina * 6 + 6).map((p, i) => ({
                      produtoId: String(pagina * 6 + i + 1),
                      clienteId: String(request.query.clienteId ?? "1"),
                      sku: p[0],
                      unidadeMedida: p[1],
                      fisicoTotal: p[2],
                      disponivel: p[3],
                      reservado: p[4],
                      indisponivel: p[5],
                      pendenteUnitizacao: p[6],
                  })),
        },
    };
}
