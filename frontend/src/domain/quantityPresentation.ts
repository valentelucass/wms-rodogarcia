import { expandedDecimal } from "../contracts/codec";
import { display } from "./labels";

/** Formata os lexemas exatos; não usa Number nem arredonda dados da operação. */
export function formatProductQuantity(value: unknown, unit: unknown): string {
    if (value === null || value === undefined || value === "") return "—";
    try {
        const decimal = expandedDecimal(String(value));
        const fraction = decimal.fraction
            .replace(/0+$/, "")
            .padEnd(String(unit).trim().toUpperCase() === "KG" ? 2 : 0, "0");
        return (
            (decimal.negative ? "-" : "") +
            decimal.whole.replace(/\B(?=(\d{3})+(?!\d))/g, ".") +
            (fraction ? "," + fraction : "")
        );
    } catch {
        return display(value);
    }
}
