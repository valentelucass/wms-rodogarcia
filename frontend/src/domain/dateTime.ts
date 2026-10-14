export const operationTimeZone = "America/Sao_Paulo";
const localFormatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: operationTimeZone,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hourCycle: "h23",
});

function localSeconds(date: Date): string {
    const parts = Object.fromEntries(
        localFormatter
            .formatToParts(date)
            .map((part) => [part.type, part.value]),
    );
    return `${parts.year}-${parts.month}-${parts.day}T${parts.hour}:${parts.minute}:${parts.second}`;
}

/** Apenas apresentação: o instante original permanece intacto até uma edição. */
export function instantToLocal(value: unknown): string {
    if (!value) return "";
    const text = String(value);
    if (
        !/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{1,9})?(?:Z|[+-]\d\d:\d\d)$/.test(
            text,
        )
    )
        return "";
    const date = new Date(text);
    if (Number.isNaN(date.getTime())) return "";
    const fraction = text.match(/\.(\d+)(?:Z|[+-]\d\d:\d\d)$/)?.[1];
    return localSeconds(date) + (fraction ? "." + fraction.slice(0, 3) : "");
}

/** Converte a escolha em Brasília para UTC, sem depender do fuso do computador. */
export function localToInstant(value: string): string | null {
    if (value === "") return "";
    const match = /^(\d{4}-\d\d-\d\dT\d\d:\d\d)(?::(\d\d)(\.\d{1,3})?)?$/.exec(
        value,
    );
    if (!match) return null;
    const seconds = match[1] + ":" + (match[2] ?? "00");
    const date = new Date(seconds + "Z");
    if (
        Number.isNaN(date.getTime()) ||
        date.toISOString().slice(0, 19) !== seconds
    )
        return null;
    const localMilliseconds = date.getTime();
    let instant = localMilliseconds;
    for (let i = 0; i < 3; i++) {
        const offset =
            Date.parse(localSeconds(new Date(instant)) + "Z") - instant;
        instant = localMilliseconds - offset;
    }
    // Horários inexistentes na mudança histórica de fuso não são inventados.
    if (localSeconds(new Date(instant)) !== seconds) return null;
    return (
        new Date(instant).toISOString().slice(0, 19) + (match[3] ?? "") + "Z"
    );
}
