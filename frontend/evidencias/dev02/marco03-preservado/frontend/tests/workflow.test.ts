import { describe, it, expect } from "vitest";
import {
    absorb,
    emptyWorkflow,
    operationContext,
} from "../src/domain/workflow";
import { encode, parseExact } from "../src/contracts/codec";
import { isObject, endpoint } from "../src/contracts/runtime";
import { isLosslessNumber } from "lossless-json";
describe("Transferências explícitas entre etapas", () => {
    it("pedido/entrada/unidade/saída/reserva/fechamento têm IDs e revisões próprios", () => {
        let state = emptyWorkflow();
        for (const [type, data] of [
            [
                "PedidoEntradaDto.Resumo",
                { id: "101", versao: "3", clienteId: "1", armazemId: "1" },
            ],
            [
                "RecebimentoDto.Entrada",
                { id: "201", itemNotaId: "401", produtoId: "11" },
            ],
            [
                "UnidadeLogisticaDto.Resumo",
                {
                    id: "501",
                    versao: "7",
                    pedidoId: "101",
                    codigo: "00000000-0000-4000-8000-000000000001",
                },
            ],
            [
                "PedidoSaidaDto.Detalhe",
                { id: "601", versao: "5", clienteId: "1", armazemId: "1" },
            ],
            [
                "PedidoSaidaDto.Reserva",
                {
                    id: "701",
                    unidadeId: "501",
                    codigoUnidade: "00000000-0000-4000-8000-000000000001",
                },
            ],
            [
                "FechamentoCobrancaDto.Fechamento",
                { id: "901", versao: "9", versaoAtual: 2 },
            ],
            [
                "FechamentoCobrancaDto.Versao",
                {
                    id: "1001",
                    fechamentoId: "901",
                    numero: 2,
                    conteudoHash: "a".repeat(64),
                },
            ],
        ] as const)
            state = absorb(
                state,
                type,
                data,
                true,
                type === "RecebimentoDto.Entrada"
                    ? {
                          endpoint: endpoint(
                              "PedidoEntradaController.entradas",
                          ),
                          params: { id: "101" },
                          query: {},
                      }
                    : type === "PedidoSaidaDto.Reserva"
                      ? {
                            endpoint: endpoint("ExpedicaoController.consultar"),
                            params: { id: "601" },
                            query: {},
                        }
                      : undefined,
            );
        expect(
            operationContext("UnidadeLogisticaController.unitizar", {}, state),
        ).toMatchObject({
            pedidoId: "101",
            entradaId: "201",
            versaoPedido: "3",
        });
        expect(
            operationContext("ExpedicaoController.ler", {}, state),
        ).toMatchObject({ id: "601", versao: "5", reservaId: "701" });
        expect(
            operationContext(
                "FechamentoCobrancaController.entregar",
                {},
                state,
            ),
        ).toMatchObject({
            id: "901",
            versao: "9",
            numero: 2,
            arquivoHash: "a".repeat(64),
        });
        const address = absorb(
            state,
            "EnderecoDto.Resposta",
            { id: "81", codigo: "A101" },
            true,
        );
        expect(
            operationContext("PedidoEntradaController.consultar", {}, address)
                .id,
        ).toBe("101");
    });
    it("lista com vários registros não seleciona por conveniência", () => {
        const state = absorb(emptyWorkflow(), "List<RecebimentoDto.Entrada>", [
            { id: "201" },
            { id: "202" },
        ]);
        expect(state.selected["RecebimentoDto.Entrada"]).toBeUndefined();
        expect(state.catalogs["RecebimentoDto.Entrada"]).toHaveLength(2);
    });
    it("Map de contingência serializa schema do fato sem heurística por nome", () => {
        const raw = encode("ContingenciaDto.Registrar", {
            operacaoId: crypto.randomUUID(),
            identidadeFato: "PLANILHA-DEMO-01",
            clienteId: "1",
            armazemId: "1",
            tipo: "CHEGADA",
            ocorridaEm: "2026-10-08T12:00:00.123456Z",
            operador: "Operador fictício",
            fonte: "Planilha de exercício",
            efeitoRegistradoNoWms: false,
            dependencias: [],
            motivo: "Conferência fictícia",
            dados: {
                pedidoId: "9007199254740993",
                dados: {
                    versao: "0",
                    chegouEm: "2026-10-08T12:00:00.123456Z",
                    observacao: "Exercício fictício",
                    itens: [
                        {
                            itemNotaId: "9223372036854775807",
                            lote: null,
                            validade: null,
                            quantidadeBoa: "9999999999999.999999",
                            quantidadeAvariada: "0.000000",
                        },
                    ],
                },
            },
        });
        expect(raw).toContain('"pedidoId":9007199254740993');
        expect(raw).toContain('"itemNotaId":9223372036854775807');
        expect(raw).toContain('"quantidadeBoa":9999999999999.999999');
        const data = parseExact(raw);
        expect(isObject(data) && data.tipo).toBe("CHEGADA");
        expect(
            isObject(data) &&
                isObject(data.dados) &&
                isLosslessNumber(data.dados.pedidoId),
        ).toBe(true);
    });
});
