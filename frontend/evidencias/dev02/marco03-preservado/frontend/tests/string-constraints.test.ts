import { it, expect } from "vitest";
import {
    encode,
    validateField,
    parseExact,
    initialField,
} from "../src/contracts/codec";
import { records } from "../src/contracts/runtime";
import { scalarInputValue } from "../src/components/fields/ScalarField";
it("NotNull String permite vazio quando não há NotBlank/min; emite string, não null", () => {
    const category = records["ConfiguracaoCobrancaDto.Item"].find(
        (f) => f.name === "categoria",
    )!;
    expect(category.required).toBe(true);
    expect(category.notBlank).toBeUndefined();
    expect(() => validateField(category, "")).not.toThrow();
    expect(
        parseExact(
            encode("ConfiguracaoCobrancaDto.Item", {
                servicoId: "2501",
                categoria: "",
                preco: "2.500000",
                percentual: null,
            }),
        ),
    ).toMatchObject({ categoria: "" });
    expect(() => validateField(category, null)).toThrow();
    const name = records["ClienteDto.Criar"].find((f) => f.name === "nome")!;
    expect(() => validateField(name, "")).toThrow();
    expect(() => validateField(name, "  ")).toThrow();
});
it("Controle de chave fiscal opcional começa sem informação; limpar não emite string vazia incompatível", () => {
    const key = records["ExpedicaoDto.Nota"].find(
        (f) => f.name === "chaveAcesso",
    )!;
    expect(initialField(key, {})).toBeNull();
    expect(scalarInputValue(key, "")).toBeNull();
    expect(() => validateField(key, "")).toThrow();
    expect(() => validateField(key, null)).not.toThrow();
    expect(scalarInputValue(key, "1".repeat(44))).toBe("1".repeat(44));
});
