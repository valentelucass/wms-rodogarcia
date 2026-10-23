import { overviewMoney } from "../../components/dashboard/overviewFormatting";

export function invoiceMoney(value: unknown, currency: unknown = "BRL") {
    if (value == null || value === "") return "—";
    const amount = overviewMoney(String(value));
    return currency === "BRL"
        ? amount
        : `${currency ?? "Moeda não informada"} ${amount.slice(3)}`;
}

export function invoiceDate(value: unknown) {
    const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(String(value ?? ""));
    return match ? `${match[3]}/${match[2]}/${match[1]}` : "—";
}

// O fim contratual é exclusivo; a apresentação mostra o último dia incluído.
export function invoicePeriod(start: unknown, end: unknown) {
    const value = String(end ?? "");
    if (!/^\d{4}-\d{2}-\d{2}$/.test(value) || !start)
        return "Período não informado";
    const last = new Date(value + "T00:00:00Z");
    if (Number.isNaN(last.getTime())) return "Período não informado";
    if (value <= String(start)) return "Período inválido";
    last.setUTCDate(last.getUTCDate() - 1);
    return `${invoiceDate(start)} a ${invoiceDate(last.toISOString())}`;
}
