export function formatQuantity(value: string): string {
    const [whole, fraction] = value.split(".");
    const decimals = (fraction ?? "").replace(/0+$/, "");
    return (
        whole.replace(/\B(?=(\d{3})+(?!\d))/g, ".") +
        (decimals ? "," + decimals : "")
    );
}

/** Só geometria: rótulos e valores conservam a precisão recebida. */
export function barShare(value: string, maximum: string): number {
    const scale = Math.max(
        value.split(".")[1]?.length ?? 0,
        maximum.split(".")[1]?.length ?? 0,
    );
    const integer = (v: string) => {
        const [whole, fraction = ""] = v.split(".");
        return BigInt(whole + fraction.padEnd(scale, "0"));
    };
    const total = integer(maximum),
        part = integer(value);
    if (total <= 0n || part <= 0n) return 0;
    return (
        Number(
            (part * 100000n) / total > 100000n
                ? 100000n
                : (part * 100000n) / total,
        ) / 100
    );
}
