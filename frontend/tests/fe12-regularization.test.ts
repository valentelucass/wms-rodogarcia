import { describe, expect, it } from "vitest";
import { endpoint, type Values } from "../src/contracts/runtime";
import type { Request } from "../src/api/client";
import { fixture } from "../src/api/mock/fixtures";
import { ExampleCount } from "../src/modules/regularizacao/ExampleCount";
import { ExampleInitialLoad } from "../src/modules/regularizacao/ExampleInitialLoad";
import { ExampleContingency } from "../src/modules/regularizacao/ExampleContingency";
import {
    operationContext,
    absorb,
    emptyWorkflow,
} from "../src/domain/workflow";
import { encode, parseExact } from "../src/contracts/codec";

function request(id: string, body: Values = {}, resource = "2401"): Request {
    return {
        endpoint: endpoint(id),
        body,
        params: { id: resource },
        query: {},
        signal: new AbortController().signal,
    };
}
function respond(
    example: { respond(r: Request, b: Values, d: unknown): unknown },
    id: string,
    body: Values = {},
    resource?: string,
): Values {
    const r = request(id, body, resource);
    return example.respond(r, body, fixture(r.endpoint.response)) as Values;
}
const manual: Values = {
    operacaoId: "00000000-0000-4000-8000-000000000001",
    identidadeFato: "FE12-CHEGADA",
    clienteId: "1",
    armazemId: "1",
    tipo: "CHEGADA",
    ocorridaEm: "2026-10-08T12:00:00.123456Z",
    operador: "Operador fictício",
    fonte: "Planilha fictícia FE12",
    efeitoRegistradoNoWms: false,
    dependencias: [],
    dados: {
        pedidoId: "9007199254740993",
        dados: {
            versao: "0",
            chegouEm: "2026-10-08T12:00:00.123456Z",
            observacao: "Chegada fictícia",
            itens: [
                {
                    itemNotaId: "401",
                    quantidadeBoa: "8.000001",
                    quantidadeAvariada: "0",
                },
            ],
        },
    },
    motivo: "Exercício isolado FE12",
};
describe("FE12 — roteiros preparados, sem cálculo ou efeito backend", () => {
    it("contagem conserva esperado/observado/origem e só Aplicar recebe efeito", () => {
        const e = new ExampleCount();
        const pending = respond(e, "ContagemController.contar", {
            contado: "8.000000",
            codigoUnidade: "00000000-0000-4000-8000-000000000003",
            observadoEm: manual.ocorridaEm,
        });
        expect(pending).toMatchObject({
            esperado: "10.000000",
            contado: "8.000000",
            diferenca: "-2.000000",
            reservado: "0.000000",
            impedimento: false,
            situacao: "PENDENTE",
            efeitoJson: null,
        });
        expect(respond(e, "ContagemController.consultar", {}, "2201")).toEqual(
            pending,
        );
        const applied = respond(
            e,
            "ContagemController.aplicar",
            {
                revisao: 1,
                versaoUnidade: "0",
                motivo: "Fictício",
                causa: "Perda fictícia",
                destino: "Ajuste fictício",
                comprovacao: "Registro fictício",
                origens: [{ entradaId: "201", delta: "-2.000000" }],
            },
            "2201",
        );
        expect(applied.situacao).toBe("APLICADA");
        expect(applied.efeitoJson).not.toBeNull();
        expect(pending.efeitoJson).toBeNull();
    });
    it("vetor reservado é preparado e recusa Aplicar sem mudar a consulta", () => {
        const e = new ExampleCount();
        const pending = respond(e, "ContagemController.contar", {
            contado: "7.000000",
            codigoUnidade: "00000000-0000-4000-8000-000000000003",
        });
        expect(pending).toMatchObject({
            contado: "7.000000",
            diferenca: "-3.000000",
            reservado: "8.000000",
            impedimento: true,
            situacao: "PENDENTE_RESERVA",
            efeitoJson: null,
        });
        expect(() =>
            respond(e, "ContagemController.aplicar", { revisao: 1 }, "2201"),
        ).toThrow(/reserva/i);
        expect(respond(e, "ContagemController.consultar", {}, "2201")).toEqual(
            pending,
        );
    });
    it("carga ausente não vira preparada; revisão conhecida mantém entrada/etiquetas", () => {
        const e = new ExampleInitialLoad();
        const pending = respond(e, "CargaInicialController.criar", {
            clienteId: "1",
            armazemId: "1",
            produtoId: "1",
            referencia: "FE12",
            etiquetaFornecida: "FE12-L1",
            quantidade: "10.000000",
            dados: {},
        });
        expect(pending.entradaId).toBeNull();
        expect(pending.pendencias).not.toEqual([]);
        expect(() =>
            respond(e, "CargaInicialController.preparar", {}, "2301"),
        ).toThrow(/pend/i);
        expect(
            respond(e, "CargaInicialController.consultar", {}, "2301"),
        ).toEqual(pending);
        const revised = respond(
            e,
            "CargaInicialController.revisar",
            {
                versao: "0",
                dados: {
                    entradaExistenteId: "201",
                    fonte: "Entrada fictícia conhecida FE12",
                },
            },
            "2301",
        );
        expect(revised).toMatchObject({
            situacao: "PENDENTE",
            entradaId: null,
            revisao: { numero: 2 },
            pendencias: [],
        });
        const prepared = respond(
            e,
            "CargaInicialController.preparar",
            { versao: "1", revisao: 2, conteudoHash: "c".repeat(64) },
            "2301",
        );
        expect(prepared).toMatchObject({
            situacao: "PREPARADA",
            entradaId: "201",
            etiquetas: ["00000000-0000-4000-8000-000000000003"],
        });
        const confirmed = respond(
            e,
            "CargaInicialController.confirmar",
            {
                versao: "2",
                revisao: 2,
                conteudoHash: "c".repeat(64),
                leitura: "FE12-L1",
                etiquetasUnidades: prepared.etiquetas,
            },
            "2301",
        );
        expect(confirmed.situacao).toBe("REGULARIZADA");
        expect(confirmed.entradaId).toBe(prepared.entradaId);
        expect(
            respond(e, "CargaInicialController.consultar", {}, "2301"),
        ).toEqual(confirmed);
    });
    it("repetição de registro mantém confirmação original; consulta informa conciliação atual", () => {
        const e = new ExampleContingency();
        const original = respond(e, "ContingenciaController.registrar", manual);
        const current = respond(e, "ContingenciaController.conciliar", {
            versao: "0",
            modo: "EXECUTAR",
            motivo: manual.motivo,
        });
        expect(current.situacao).toBe("CONCILIADA");
        expect(
            respond(e, "ContingenciaController.registrar", {
                ...manual,
                operacaoId: "00000000-0000-4000-8000-000000000002",
            }),
        ).toEqual(original);
        expect(respond(e, "ContingenciaController.consultar")).toEqual(current);
        expect(() =>
            respond(e, "ContingenciaController.registrar", {
                ...manual,
                operador: "Outro operador",
            }),
        ).toThrow(/divergente/i);
    });
    it("dependência devolve pendência sem efeito; consulta conserva conteúdo e precisão", () => {
        const e = new ExampleContingency();
        const original = respond(e, "ContingenciaController.registrar", {
            ...manual,
            dependencias: ["FE12-AUSENTE"],
        });
        const result = respond(e, "ContingenciaController.conciliar", {
            versao: "0",
            modo: "EXECUTAR",
        });
        expect(result).toMatchObject({
            situacao: "PENDENTE",
            resultado: null,
            conciliadaEm: null,
        });
        expect(result.pendencia).toMatch(/dependência/);
        expect(result.conteudo).toEqual(original.conteudo);
        const wire = encode("ContingenciaDto.Registrar", manual);
        expect(wire).toContain('"pedidoId":9007199254740993');
        expect(wire).toContain('"quantidadeBoa":8.000001');
        expect(parseExact(wire)).toBeDefined();
    });
    it("VINCULAR recebe prova, versão e hash e não executa o exemplo sem prova", () => {
        const e = new ExampleContingency();
        const original = respond(e, "ContingenciaController.registrar", {
            ...manual,
            efeitoRegistradoNoWms: true,
        });
        expect(() =>
            respond(e, "ContingenciaController.conciliar", {
                versao: "0",
                modo: "VINCULAR",
            }),
        ).toThrow(/prova/i);
        expect(respond(e, "ContingenciaController.consultar")).toEqual(
            original,
        );
        const result = respond(e, "ContingenciaController.conciliar", {
            versao: "0",
            modo: "VINCULAR",
            prova: {
                operacaoOriginal: manual.operacaoId,
                conteudoHash: "d".repeat(64),
            },
        });
        expect(result).toMatchObject({
            situacao: "CONCILIADA",
            resultado: { modo: "VINCULAR" },
        });
    });
    it("referências carregam revisão/hash/etiquetas e nunca inventam delta ou prova", () => {
        let w = absorb(emptyWorkflow(), "CargaInicialDto.Resultado", {
            ...(fixture("CargaInicialDto.Resultado") as Values),
            id: "2301",
            versao: "2",
            etiquetaFornecida: "FE12-L1",
            revisao: {
                numero: 2,
                conteudoHash: "c".repeat(64),
                dados: { entradaExistenteId: "201" },
            },
            etiquetas: ["U1"],
        });
        const c = operationContext("CargaInicialController.confirmar", {}, w);
        expect(c).toMatchObject({
            id: "2301",
            versao: "2",
            revisao: 2,
            conteudoHash: "c".repeat(64),
            leitura: "FE12-L1",
            etiquetasUnidades: ["U1"],
        });
        w = absorb(w, "ContagemDto.Resultado", {
            ...(fixture("ContagemDto.Resultado") as Values),
            id: "2201",
            revisao: 4,
            diferenca: "-2",
        });
        expect(
            operationContext("ContagemController.aplicar", {}, w),
        ).toMatchObject({ id: "2201", revisao: 4 });
        expect(
            operationContext("ContagemController.aplicar", {}, w).origens,
        ).toBeUndefined();
        expect(
            operationContext("ContingenciaController.conciliar", {}, w).prova,
        ).toBeUndefined();
    });
});
