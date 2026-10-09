import { describe, it, expect, expectTypeOf, vi } from "vitest";
import {
    absorb,
    emptyWorkflow,
    operationContext,
    referenceCatalog,
} from "../src/domain/workflow";
import {
    encode,
    parseResponse,
    stringifyExact,
    toWire,
    validateField,
} from "../src/contracts/codec";
import { fixture } from "../src/api/mock/fixtures";
import { endpoint, records, type Values } from "../src/contracts/runtime";
import { realTransport } from "../src/api/client";
import type { ClienteDto_Resposta } from "../src/contracts/types";

const obj = (type: string, patch: Values = {}): Values => ({
    ...(fixture(type) as Values),
    ...patch,
});
const origin = (id: string, order: string) => ({
    endpoint: endpoint(id),
    params: { id: order },
    query: {},
});
const reserve = (id: string) =>
    obj("PedidoSaidaDto.Reserva", {
        id,
        unidadeId: "501",
        notaOrigemId: "301",
        codigoUnidade: "00000000-0000-4000-8000-000000000501",
    });
const order = (id: string, revision: string, reserves: Values[]) =>
    obj("PedidoSaidaDto.Detalhe", { id, versao: revision, reservas: reserves });

describe("MARCO02: referências conservam pedido, nota e revisão", () => {
    it("consulta alheia não substitui FIFO vigente e catálogo sem pai não oferece referência", () => {
        const unowned = absorb(
            emptyWorkflow(),
            "List<RecebimentoDto.Entrada>",
            [obj("RecebimentoDto.Entrada", { id: "201" })],
        );
        expect(referenceCatalog(unowned, "RecebimentoDto.Entrada")).toEqual([]);
        let state = absorb(
            emptyWorkflow(),
            "PedidoSaidaDto.Detalhe",
            order("602", "1", []),
        );
        const current = obj("PedidoSaidaDto.Sugestao", { versao: "1" });
        state = absorb(
            state,
            "PedidoSaidaDto.Sugestao",
            current,
            false,
            origin("PedidoSaidaController.sugerir", "602"),
        );
        state = absorb(
            state,
            "PedidoSaidaDto.Sugestao",
            obj("PedidoSaidaDto.Sugestao", { versao: "1" }),
            false,
            origin("PedidoSaidaController.sugerir", "601"),
        );
        expect(state.selected["PedidoSaidaDto.Sugestao"]).toBe(current);
        expect(
            operationContext("PedidoSaidaController.justificar", {}, state)
                .selecoes,
        ).toEqual(current.selecoes);
    });
    it("troca de pedido invalida FIFO, documento, reserva, nota de origem e revisão de etiqueta", () => {
        let state = absorb(
            emptyWorkflow(),
            "ExpedicaoDto.Detalhe",
            obj("ExpedicaoDto.Detalhe", {
                pedido: order("601", "3", [reserve("701")]),
                documentos: [
                    obj("ExpedicaoDto.DocumentoRegistrado", { id: "801" }),
                ],
            }),
        );
        state = absorb(
            state,
            "PedidoSaidaDto.Sugestao",
            obj("PedidoSaidaDto.Sugestao", { versao: "3" }),
            false,
            origin("PedidoSaidaController.sugerir", "601"),
        );
        state = absorb(
            state,
            "UnidadeLogisticaDto.Etiqueta",
            obj("UnidadeLogisticaDto.Etiqueta", {
                codigoLeitura: reserve("701").codigoUnidade,
                versaoConteudo: "9",
            }),
        );
        expect(
            operationContext("ExpedicaoController.documento", {}, state),
        ).toMatchObject({
            id: "601",
            versao: "3",
            reservaId: "701",
            documentoId: "801",
            revisaoConteudo: "9",
            coberturas: [{ reservaId: "701", notaOrigemId: "301" }],
        });
        state = absorb(
            state,
            "PedidoSaidaDto.Detalhe",
            order("602", "1", []),
            true,
        );
        for (const action of ["ler", "separar", "documento", "cancelar"]) {
            const ctx = operationContext(
                "ExpedicaoController." + action,
                {},
                state,
            );
            expect(ctx).toMatchObject({ id: "602", versao: "1" });
            for (const key of [
                "reservaId",
                "documentoId",
                "codigoLido",
                "revisaoConteudo",
                "destinacao",
                "coberturas",
            ])
                expect(ctx[key]).toBeUndefined();
        }
        expect(
            operationContext("PedidoSaidaController.justificar", {}, state)
                .selecoes,
        ).toBeUndefined();
        state = absorb(state, "PedidoSaidaDto.Reserva", reserve("701"), true);
        expect(
            operationContext("ExpedicaoController.ler", {}, state).reservaId,
        ).toBeUndefined();
        // Resposta de consulta antiga não recebe o vínculo do pedido atualmente escolhido.
        state = absorb(
            state,
            "PedidoSaidaDto.Sugestao",
            obj("PedidoSaidaDto.Sugestao", { versao: "3" }),
            false,
            origin("PedidoSaidaController.sugerir", "601"),
        );
        expect(
            operationContext("PedidoSaidaController.justificar", {}, state)
                .selecoes,
        ).toBeUndefined();
    });
    it("seleção explícita legítima e atualização preservam só reservas retornadas do mesmo pedido", () => {
        const r1 = reserve("701"),
            r2 = reserve("702");
        let state = absorb(
            emptyWorkflow(),
            "PedidoSaidaDto.Detalhe",
            order("601", "1", [r1, r2]),
        );
        expect(state.selected["PedidoSaidaDto.Reserva"]).toBeUndefined();
        state = absorb(state, "PedidoSaidaDto.Reserva", r2, true);
        expect(
            operationContext("ExpedicaoController.ler", {}, state),
        ).toMatchObject({ id: "601", reservaId: "702", versao: "1" });
        state = absorb(
            state,
            "PedidoSaidaDto.Sugestao",
            obj("PedidoSaidaDto.Sugestao", { versao: "0" }),
            false,
            origin("PedidoSaidaController.sugerir", "601"),
        );
        expect(
            operationContext("PedidoSaidaController.justificar", {}, state)
                .selecoes,
        ).toBeUndefined();
        state = absorb(
            state,
            "PedidoSaidaDto.Sugestao",
            obj("PedidoSaidaDto.Sugestao", { versao: "1" }),
            false,
            origin("PedidoSaidaController.sugerir", "601"),
        );
        expect(
            operationContext("PedidoSaidaController.justificar", {}, state)
                .selecoes,
        ).toEqual(state.selected["PedidoSaidaDto.Sugestao"].selecoes);
        state = absorb(
            state,
            "PedidoSaidaDto.Detalhe",
            order("601", "2", [r1, r2]),
        );
        expect(
            operationContext("ExpedicaoController.ler", {}, state),
        ).toMatchObject({ reservaId: "702", versao: "2" });
        expect(state.selected["PedidoSaidaDto.Sugestao"]).toBeUndefined();
        state = absorb(state, "PedidoSaidaDto.Detalhe", order("601", "3", []));
        expect(
            operationContext("ExpedicaoController.ler", {}, state).reservaId,
        ).toBeUndefined();
    });
    it("SKU de unidade distinta não preenche documento da reserva", () => {
        let state = absorb(
            emptyWorkflow(),
            "PedidoSaidaDto.Detalhe",
            order("601", "1", [reserve("701")]),
        );
        state = absorb(
            state,
            "UnidadeLogisticaDto.Resumo",
            obj("UnidadeLogisticaDto.Resumo", { id: "999", sku: "SKU-ERRADO" }),
        );
        expect(
            operationContext("ExpedicaoController.documento", {}, state)
                .coberturas,
        ).toMatchObject([{ reservaId: "701", notaOrigemId: "301", sku: "DEMO-CAIXA" }]);
        state = absorb(
            state,
            "UnidadeLogisticaDto.Resumo",
            obj("UnidadeLogisticaDto.Resumo", {
                id: "501",
                codigo: reserve("701").codigoUnidade,
                sku: "SKU-CERTO",
            }),
        );
        expect(
            operationContext("ExpedicaoController.documento", {}, state)
                .coberturas,
        ).toMatchObject([{ sku: "SKU-CERTO" }]);
    });
    it("troca de entrada não conserva itens de nota, entrada efetivada ou chegada anterior", () => {
        const item = obj("PedidoEntradaDto.ItemConferencia", { id: "401" });
        const note = obj("PedidoEntradaDto.Nota", { id: "301", itens: [item] });
        let state = absorb(emptyWorkflow(), "PedidoEntradaDto.Detalhe", {
            pedido: obj("PedidoEntradaDto.Resumo", { id: "101", versao: "4" }),
            divergente: false,
            notas: [note],
        });
        // Iniciar conferência retorna só Resumo: revisão nova do mesmo pai
        // conserva a identidade da nota/item previamente consultados.
        state = absorb(
            state,
            "PedidoEntradaDto.Resumo",
            obj("PedidoEntradaDto.Resumo", { id: "101", versao: "5" }),
        );
        expect(
            operationContext("PedidoEntradaController.chegada", {}, state),
        ).toMatchObject({
            id: "101",
            versao: "5",
            itens: [{ itemNotaId: "401" }],
        });
        state = absorb(
            state,
            "RecebimentoDto.Entrada",
            obj("RecebimentoDto.Entrada", { id: "201" }),
            true,
            origin("PedidoEntradaController.entradas", "101"),
        );
        expect(
            operationContext("PedidoEntradaController.chegada", {}, state)
                .itens,
        ).toMatchObject([{ itemNotaId: "401" }]);
        expect(
            operationContext("UnidadeLogisticaController.unitizar", {}, state)
                .entradaId,
        ).toBe("201");
        state = absorb(
            state,
            "PedidoEntradaDto.Resumo",
            obj("PedidoEntradaDto.Resumo", { id: "102", versao: "1" }),
            true,
        );
        expect(
            operationContext("PedidoEntradaController.chegada", {}, state)
                .itens,
        ).toBeUndefined();
        expect(
            operationContext("UnidadeLogisticaController.unitizar", {}, state)
                .entradaId,
        ).toBeUndefined();
        state = absorb(state, "PedidoEntradaDto.ItemConferencia", item, true);
        expect(
            state.selected["PedidoEntradaDto.ItemConferencia"],
        ).toBeUndefined();
    });
});

describe("MARCO02: metadata de elementos e nulabilidade real", () => {
    it("XML recebe NotBlank/Size por elemento, mantendo limites separados da coleção", () => {
        const field = records["ExpedicaoDto.Retirar"].find(
            (f) => f.name === "xmls",
        )!;
        expect(field).toMatchObject({
            element: {
                type: "String",
                required: true,
                notBlank: true,
                Size: "max = 1048576",
            },
        });
        const body = obj("ExpedicaoDto.Retirar", {
            xmls: ["<nfe>fictício</nfe>"],
        });
        expect(() => encode("ExpedicaoDto.Retirar", body)).not.toThrow();
        for (const xml of [null, "", " \t\n", "x".repeat(1048577)])
            expect(() =>
                encode("ExpedicaoDto.Retirar", { ...body, xmls: [xml] }),
            ).toThrow();
        expect(() =>
            encode("ExpedicaoDto.Retirar", {
                ...body,
                xmls: ["x".repeat(1048576)],
            }),
        ).not.toThrow();
        const unannotated = Object.values(records)
            .flat()
            .find(
                (f) =>
                    f.type === "List<String>" &&
                    !f.element?.notBlank &&
                    !f.element?.Size,
            )!;
        expect(unannotated).toBeDefined();
        expect(() => validateField(unannotated, [" \t\n"])).not.toThrow();
    });
    it("long/int/boolean primitivos e campos required recusam null; boxed nullable preserva null", () => {
        for (const type of ["long", "int", "boolean"])
            expect(() => parseResponse(type, "null")).toThrow();
        for (const type of ["Long", "Integer", "Boolean", "String"])
            expect(parseResponse(type, "null")).toBeNull();
        const data = obj("ClienteDto.Resposta");
        expect(() =>
            parseResponse(
                "ClienteDto.Resposta",
                stringifyExact(
                    toWire("ClienteDto.Resposta", { ...data, id: null }),
                ),
            ),
        ).not.toThrow();
        expect(() =>
            parseResponse(
                "ClienteDto.Resposta",
                stringifyExact(
                    toWire("ClienteDto.Resposta", { ...data, versao: null }),
                ),
            ),
        ).toThrow();
        const typed = parseResponse(
            "ClienteDto.Resposta",
            stringifyExact(toWire("ClienteDto.Resposta", data)),
        ) as ClienteDto_Resposta;
        expectTypeOf<ClienteDto_Resposta["versao"]>().toEqualTypeOf<string>();
        const version: string = typed.versao;
        expect(version).toBe(data.versao);
    });
    it.each(["ClienteController.criar", "ClienteController.consultar"])(
        "2xx com null obrigatório conserva status/correlação e incerteza apenas para escrita: %s",
        async (id) => {
            const data = obj("ClienteDto.Resposta", { versao: null });
            const transport = realTransport({
                baseUrl: "https://api.invalid",
                allowReal: true,
                token: () => "ficticio",
                onExpired: vi.fn(),
                fetcher: vi.fn().mockResolvedValue(
                    new Response(
                        stringifyExact(toWire("ClienteDto.Resposta", data)),
                        {
                            status: 200,
                            headers: { "X-Request-Id": "null-obrigatorio" },
                        },
                    ),
                ),
            });
            const e = endpoint(id);
            await expect(
                transport.send({
                    endpoint: e,
                    params: { id: "1" },
                    query: {},
                    body: e.request
                        ? obj(e.request, {
                              codigo: "DEMO",
                              nome: "Cliente fictício",
                              documentoFiscal: "12345678901",
                          })
                        : undefined,
                    signal: new AbortController().signal,
                }),
            ).rejects.toMatchObject({
                status: 200,
                requestId: "null-obrigatorio",
                code: "RESPOSTA_INCOMPATIVEL",
                uncertain: e.method !== "GET",
            });
        },
    );
});
