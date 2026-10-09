import { describe, it, expect } from "vitest";
import {
    encode,
    parseResponse,
    validateValue,
    toWire,
    stringifyExact,
    validateField,
} from "../src/contracts/codec";
import { records, endpoints } from "../src/contracts/runtime";
import { fixture } from "../src/api/mock/fixtures";
import { journeys } from "../src/domain/journeys";
import type { FechamentoCobrancaDto_Ajuste } from "../src/contracts/types";
describe("Fronteira contratual exata", () => {
    it("validação de entrada deriva NotBlank e limite decimal sem arredondar", () => {
        expect(() =>
            encode("RevisaoCadastroRequest", { versao: "0", motivo: "     " }),
        ).toThrow("com texto");
        const percentual = records[
            "ConfiguracaoCobrancaDto.ConfigurarContrato"
        ].find((f) => f.DecimalMax);
        expect(percentual).toBeDefined();
        expect(() => validateField(percentual!, "100.000000")).not.toThrow();
        expect(() => validateField(percentual!, "100.000001")).toThrow(
            "fora do limite",
        );
        expect(() => validateField(percentual!, "-0.000001")).toThrow(
            "fora do limite",
        );
    });
    it("preserva Long acima de 2^53 e seu limite, emitindo número JSON", () => {
        const raw = encode("RevisaoCadastroRequest", {
            versao: "9223372036854775807",
            motivo: "Teste de revisão",
        });
        expect(raw).toContain('"versao":9223372036854775807');
        const type = "ClienteDto.Resposta";
        const example = fixture(type) as Record<string, unknown>;
        example.id = "9007199254740993";
        expect(
            parseResponse(type, stringifyExact(toWire(type, example))),
        ).toMatchObject({ id: "9007199254740993" });
        expect(() =>
            encode("RevisaoCadastroRequest", {
                versao: "9223372036854775808",
                motivo: "Teste de revisão",
            }),
        ).toThrow("fora do domínio");
    });
    it("mantém decimal, escala, exponencial e null diferentes de zero", () => {
        const note = {
            numeroItem: 1,
            produtoId: "1",
            quantidadePrevista: "9999999999999.999999",
            valorMercadoria: "99999999999999999.99",
        };
        const raw = encode("PedidoEntradaDto.ItemNota", note);
        expect(raw).toContain("9999999999999.999999");
        expect(raw).toContain("99999999999999999.99");
        expect(
            encode("PedidoEntradaDto.ItemNota", {
                ...note,
                quantidadePrevista: "1.230000e2",
                valorMercadoria: null,
            }),
        ).toContain('"valorMercadoria":null');
        expect(() =>
            encode("PedidoEntradaDto.ItemNota", {
                ...note,
                quantidadePrevista: "1.0000001",
            }),
        ).toThrow("Precisão");
    });
    it("recusa strings numéricas na resposta da API", () => {
        const example = fixture("ClienteDto.Resposta") as Record<
            string,
            unknown
        >;
        const raw = stringifyExact(
            toWire("ClienteDto.Resposta", example),
        ).replace('"id":1', '"id":"1"');
        expect(() => parseResponse("ClienteDto.Resposta", raw)).toThrow(
            "string numérica",
        );
    });
    it("FE-VIG-001 aceita só omissão NON_NULL derivada, sem inventar default", () => {
        const example = fixture("FechamentoCobrancaDto.Ajuste") as Record<
            string,
            unknown
        >;
        delete example.tipo;
        delete example.tratativaOrigemId;
        const raw = stringifyExact(
            toWire("FechamentoCobrancaDto.Ajuste", example),
        );
        const result = parseResponse(
            "FechamentoCobrancaDto.Ajuste",
            raw,
        ) as FechamentoCobrancaDto_Ajuste;
        const typedOptional: FechamentoCobrancaDto_Ajuste["tratativaOrigemId"] =
            undefined;
        expect(typedOptional).toBeUndefined();
        expect(result).not.toHaveProperty("tratativaOrigemId");
        expect(result).not.toHaveProperty("tipo");
        expect(() =>
            parseResponse(
                "FechamentoCobrancaDto.Ajuste",
                raw.replace(/"id":1,?/, ""),
            ),
        ).toThrow("Resposta incompleta");
    });
    it("conserva null e recusa ausência em campo comum", () => {
        const example = fixture("ClienteDto.Resposta") as Record<
            string,
            unknown
        >;
        example.nome = null;
        expect(
            parseResponse(
                "ClienteDto.Resposta",
                stringifyExact(toWire("ClienteDto.Resposta", example)),
            ),
        ).toMatchObject({ nome: null });
        delete example.nome;
        expect(() =>
            parseResponse(
                "ClienteDto.Resposta",
                stringifyExact(toWire("ClienteDto.Resposta", example)),
            ),
        ).toThrow("nome");
    });
    it("recusa campo desconhecido e UUID incompleto antes de enviar", () => {
        expect(() =>
            validateValue("RevisaoCadastroRequest", {
                versao: "0",
                motivo: "Teste local",
                extra: true,
            }),
        ).toThrow("fora do contrato");
        expect(() =>
            validateValue("EstoqueDto.Bloqueio", {
                operacaoId: "123",
                versaoUnidade: "0",
                motivo: "Teste local",
            }),
        ).toThrow("UUID");
    });
    it("161 rotas operacionais estão nas jornadas; 11 rotas auth usam telas/cliente nativos", () => {
        const used = new Set(
            journeys.flatMap((j) => j.steps.flatMap((s) => s.actions)),
        );
        const nativeAuth = [
            "csrf",
            "jwks",
            "entrar",
            "renovar",
            "sair",
            "senha",
            "eu",
            "usuarios",
            "criar",
            "editar",
            "redefinir",
        ].map((handler) => `LoginController.${handler}`);
        expect(
            endpoints
                .filter((e) => e.id.startsWith("LoginController."))
                .map((e) => e.id)
                .sort(),
        ).toEqual(nativeAuth.sort());
        expect(
            endpoints.filter((e) => !nativeAuth.includes(e.id)),
        ).toHaveLength(161);
        expect(
            endpoints.filter(
                (e) =>
                    !nativeAuth.includes(e.id) &&
                    e.id !== "StatusController.consultar" &&
                    e.id !== "EstoqueController.posicionar" &&
                    !used.has(e.id),
            ),
        ).toEqual([]);
        expect(
            endpoints.filter((e) => e.request && !records[e.request]),
        ).toEqual([]);
    });
    it.each(endpoints.map((e) => [e.id, e.response] as const))(
        "resposta fictícia tipada e decodificável: %s",
        (_, type) => {
            expect(() =>
                parseResponse(
                    type,
                    stringifyExact(toWire(type, fixture(type))),
                ),
            ).not.toThrow();
        },
    );
});
