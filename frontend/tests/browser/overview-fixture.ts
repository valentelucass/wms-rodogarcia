import type { Route } from "@playwright/test";
import { LosslessNumber, stringify } from "lossless-json";
import { overviewExample } from "../../src/api/mock/overview";
import { dashboardExample } from "../../src/api/mock/dashboard";
import type { Request } from "../../src/api/client";
import type { Perfil } from "../../src/contracts/runtime";

const numbers = new Set([
    "id",
    "versao",
    "clienteId",
    "armazemId",
    "capacidade",
    "posicoesOcupadas",
    "posicoesLivres",
    "ocupacao",
    "unidadesArmazenadas",
    "valorArmazenado",
    "emQuarentena",
    "reservasAtivas",
    "entradasAbertas",
    "saidasAbertas",
    "faturamentoMes",
    "totalItens",
    "capacidadePesoKg",
    "unidadesVisiveis",
    "quantidade",
    "pedidoEntradaId",
    "alturaMetros",
    "larguraMetros",
    "profundidadeMetros",
    "posicoesCliente",
    "unidadesDisponiveis",
    "pedidosAbertos",
    "unidadesComAviso",
    "capacidadeAtiva",
    "livresArmazem",
    "pedidos",
    "produtoId",
    "fisicoTotal",
    "disponivel",
    "reservado",
    "indisponivel",
    "pendenteUnitizacao",
]);
export function overviewWire(v: unknown, key = ""): unknown {
    return Array.isArray(v)
        ? v.map((v) => overviewWire(v))
        : v && typeof v === "object"
          ? Object.fromEntries(
                Object.entries(v).map(([k, v]) => [k, overviewWire(v, k)]),
            )
          : typeof v === "string" && numbers.has(key)
            ? new LosslessNumber(v)
            : v;
}
export async function fulfillOverviewRead(
    route: Route,
    perfil: Perfil = "GESTOR",
    clientId = "1",
    warehouseId = "1",
) {
    const url = new URL(route.request().url());
    const client = url.pathname === "/api/v1/clientes",
        warehouse = url.pathname === "/api/v1/armazens";
    let data: unknown;
    if (client || warehouse)
        data = {
            itens: [
                {
                    id: client ? clientId : warehouseId,
                    versao: 0,
                    situacao: "ATIVO",
                    criadoEm: "2026-10-09T12:00:00Z",
                    alteradoEm: "2026-10-09T12:00:00Z",
                    codigo: "DEMO",
                    nome: client ? "Cliente de teste" : "Armazém de teste",
                    documentoFiscal: "00000000000000",
                    cidade: "Cidade fictícia",
                    uf: "SP",
                },
            ],
            pagina: 0,
            tamanho: 100,
            totalItens: "1",
            totalPaginas: 1,
        };
    else if (url.pathname === "/api/v1/visao-operacao")
        data = overviewExample(
            {
                endpoint: {
                    id: "VisaoOperacaoController.consultar",
                } as Request["endpoint"],
                params: {},
                query: Object.fromEntries(url.searchParams),
                signal: new AbortController().signal,
            },
            perfil,
            true,
        );
    else if (url.pathname === "/api/v1/dashboard")
        data = dashboardExample(
            {
                endpoint: {
                    id: "DashboardController.consultar",
                } as Request["endpoint"],
                params: {},
                query: Object.fromEntries(url.searchParams),
                signal: new AbortController().signal,
            },
            perfil,
            false,
        );
    else return false;
    await route.fulfill({
        contentType: "application/json",
        body: stringify(overviewWire(data))!,
    });
    return true;
}
