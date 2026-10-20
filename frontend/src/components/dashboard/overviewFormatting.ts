import { expandedDecimal } from "../../contracts/codec";
import { formatQuantity } from "./OperationDashboard";

export function overviewMoney(value: string | null | undefined): string {
    if (value == null) return "—";
    const { negative, whole, fraction } = expandedDecimal(value);
    const cents =
        BigInt(whole) * 100n +
        BigInt(fraction.padEnd(2, "0").slice(0, 2)) +
        (Number(fraction[2] ?? "0") >= 5 ? 1n : 0n);
    return `R$ ${negative && cents > 0n ? "-" : ""}${formatQuantity(String(cents / 100n))},${String(cents % 100n).padStart(2, "0")}`;
}
