import { expect, it } from "vitest";
import { instantToLocal, localToInstant } from "../src/domain/dateTime";
import { formatProductQuantity } from "../src/domain/quantityPresentation";

it.each([
    ["2026-10-13T08:45", "2026-10-13T11:45:00Z"],
    ["2026-10-13T23:30:12.123", "2026-10-14T02:30:12.123Z"],
    ["2024-02-29T00:00", "2024-02-29T03:00:00Z"],
    ["2018-01-15T10:00", "2018-01-15T12:00:00Z"],
])(
    "converte escolha %s com fuso explícito incluindo virada do dia e histórico",
    (local, expected) => {
        expect(localToInstant(local)).toBe(expected);
    },
);
it.each([
    "2026-02-30T08:00",
    "2026-10-13T24:00",
    "2026-10-13",
    "2018-11-04T00:30",
])("recusa data ou horário inexistente %s", (value) => {
    expect(localToInstant(value)).toBeNull();
});
it("apresenta em Brasília sem depender do fuso original e sem inventar hora para data avulsa", () => {
    expect(instantToLocal("2026-10-14T02:30:12+00:00")).toBe(
        "2026-10-13T23:30:12",
    );
    expect(instantToLocal("2026-10-13T08:45:00-03:00")).toBe(
        "2026-10-13T08:45:00",
    );
    expect(instantToLocal("2026-10-13")).toBe("");
    expect(instantToLocal(null)).toBe("");
    expect(localToInstant("")).toBe("");
});
it.each([
    ["1820.000000", "KG", "1.820,00"],
    ["-1820.000000", "KG", "-1.820,00"],
    ["0", "kg", "0,00"],
    ["12.5", "Kg", "12,50"],
    ["0.000001", "KG", "0,000001"],
    ["1.820e3", "KG", "1.820,00"],
    ["9007199254740993.123456", "KG", "9.007.199.254.740.993,123456"],
    ["12.000000", "UN", "12"],
    ["1.8200", "TO", "1,82"],
    [null, "KG", "—"],
])("formata quantidade exata %s em %s", (value, unit, expected) => {
    expect(formatProductQuantity(value, unit)).toBe(expected);
});
