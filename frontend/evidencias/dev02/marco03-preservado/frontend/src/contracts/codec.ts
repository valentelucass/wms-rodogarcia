import {
    LosslessNumber,
    isLosslessNumber,
    parse,
    stringify,
} from "lossless-json";
import {
    enums,
    records,
    isObject,
    listType,
    type Field,
    type Values,
} from "./runtime";
import { contingencyTypes } from "./contingency";

export class InputError extends Error {
    constructor(
        message: string,
        public field = "",
    ) {
        super(message);
    }
}
const textNumber = (v: unknown): string => {
    if (isLosslessNumber(v)) return v.value;
    throw new InputError("Número JSON esperado; string numérica não aceita.");
};
export function expandedDecimal(s: string): {
    negative: boolean;
    whole: string;
    fraction: string;
} {
    const m = /^(-?)(\d+)(?:\.(\d+))?(?:[eE]([+-]?\d+))?$/.exec(s);
    if (!m)
        throw new InputError(
            "Informe um número decimal, com ponto como separador.",
        );
    const exp = m[4] ? BigInt(m[4]) : 0n;
    if (exp > 1000n || exp < -1000n)
        throw new InputError("Expoente fora do limite de entrada.");
    const digits = m[2] + (m[3] ?? "");
    const dot = m[2].length + Number(exp);
    const whole = (
        dot <= 0 ? "0" : digits.slice(0, dot).padEnd(dot, "0")
    ).replace(/^0+(?=\d)/, "");
    const fraction = dot <= 0 ? "0".repeat(-dot) + digits : digits.slice(dot);
    return { negative: m[1] === "-", whole, fraction };
}
const attr = (s: string | undefined, k: string) =>
    s
        ? new RegExp(k + '\\s*=\\s*(?:"([^" ]+)"|(\\d+))')
              .exec(s)
              ?.slice(1)
              .find(Boolean)
        : undefined;
const bound = (s: string | undefined) =>
    s
        ?.match(/"([^"]+)"|(-?\d+)/)
        ?.slice(1)
        .find(Boolean);
function compareDecimals(a: string, b: string): number {
    const left = expandedDecimal(a),
        right = expandedDecimal(b);
    const scale = Math.max(left.fraction.length, right.fraction.length);
    const signed = (v: typeof left) =>
        BigInt(v.whole + v.fraction.padEnd(scale, "0")) *
        (v.negative ? -1n : 1n);
    const l = signed(left),
        r = signed(right);
    return l < r ? -1 : l > r ? 1 : 0;
}
export function validateField(f: Field, v: unknown, path = f.name): void {
    const missing =
        v === undefined || v === null || (v === "" && f.type !== "String");
    if (missing) {
        if (f.required) throw new InputError("Preencha " + path + ".", path);
        return;
    }
    const lt = listType(f.type);
    if (lt) {
        if (!Array.isArray(v))
            throw new InputError("Lista esperada: " + path, path);
        const min = Number(attr(f.Size, "min") ?? 0),
            max = Number(attr(f.Size, "max") ?? 2147483647);
        if (v.length < min || v.length > max)
            throw new InputError(
                `${path}: informe de ${min} a ${max} itens.`,
                path,
            );
        v.forEach((item, i) =>
            validateField(
                f.element ?? { name: String(i), type: lt, required: false },
                item,
                `${path}.${i}`,
            ),
        );
        return;
    }
    if (records[f.type]) {
        validateValue(f.type, v, path);
        return;
    }
    if (f.type.startsWith("Map<")) {
        if (!isObject(v) || Object.keys(v).length === 0)
            throw new InputError("Informe os dados do fato.", path);
        return;
    }
    if (["boolean", "Boolean"].includes(f.type)) {
        if (typeof v !== "boolean")
            throw new InputError("Escolha sim ou não.", path);
        return;
    }
    const s = String(v);
    if (f.notBlank && !s.trim())
        throw new InputError("Preencha " + path + " com texto.", path);
    if (["Long", "long", "Integer", "int"].includes(f.type)) {
        if (!/^-?(0|[1-9]\d*)$/.test(s))
            throw new InputError("Inteiro sem fração esperado: " + path, path);
        const n = BigInt(s),
            long = ["Long", "long"].includes(f.type);
        if (
            n > (long ? 9223372036854775807n : 2147483647n) ||
            n < (long ? -9223372036854775808n : -2147483648n)
        )
            throw new InputError("Inteiro fora do domínio: " + path, path);
        if (
            (f.positive && n <= 0n) ||
            (f.Min && n < BigInt(bound(f.Min)!)) ||
            (f.Max && n > BigInt(bound(f.Max)!))
        )
            throw new InputError("Inteiro fora dos limites: " + path, path);
    }
    if (f.type === "BigDecimal") {
        const d = expandedDecimal(s);
        for (const [annotation, direction] of [
            [f.DecimalMin, -1],
            [f.DecimalMax, 1],
        ] as const) {
            const limit = bound(annotation);
            if (limit === undefined) continue;
            const comparison = compareDecimals(s, limit);
            if (
                comparison === direction ||
                (comparison === 0 &&
                    /inclusive\s*=\s*false/.test(annotation ?? ""))
            )
                throw new InputError(
                    "Quantidade/valor fora do limite: " + path,
                    path,
                );
        }
        const integer = attr(f.Digits, "integer"),
            fraction = attr(f.Digits, "fraction");
        if (
            (integer && d.whole.length > Number(integer)) ||
            (fraction && d.fraction.length > Number(fraction))
        )
            throw new InputError("Precisão excedida: " + path, path);
    }
    if (
        f.type === "UUID" &&
        !/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(
            s,
        )
    )
        throw new InputError("Leia/informe o UUID completo: " + path, path);
    if (
        f.type === "Instant" &&
        !/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d{1,9})?(?:Z|[+-]\d\d:\d\d)$/.test(
            s,
        )
    )
        throw new InputError(
            "Use data/hora ISO com fuso, por exemplo 2026-10-08T12:00:00.123456Z.",
            path,
        );
    if (
        f.type === "LocalDate" &&
        (!/^\d{4}-\d\d-\d\d$/.test(s) || Number.isNaN(Date.parse(s)))
    )
        throw new InputError("Data inválida: " + path, path);
    if (enums[f.type] && !enums[f.type].includes(s))
        throw new InputError("Escolha uma opção válida: " + path, path);
    const min = Number(attr(f.Size, "min") ?? 0),
        max = Number(attr(f.Size, "max") ?? 2147483647);
    if (s.length < min || s.length > max)
        throw new InputError("Tamanho inválido: " + path, path);
    const pattern = attr(f.Pattern, "regexp");
    if (pattern && !new RegExp("^(?:" + pattern + ")$").test(s))
        throw new InputError("Formato inválido: " + path, path);
}
export function validateValue(type: string, v: unknown, path = ""): void {
    const fields = records[type];
    if (!fields) {
        validateField({ name: path, type, required: true }, v, path);
        return;
    }
    if (!isObject(v)) throw new InputError("Preencha " + path + ".", path);
    for (const k of Object.keys(v))
        if (!fields.some((f) => f.name === k))
            throw new InputError("Campo fora do contrato: " + k, k);
    fields.forEach((f) =>
        validateField(f, v[f.name], path ? path + "." + f.name : f.name),
    );
}
export function toWire(type: string, v: unknown): unknown {
    if (v === undefined || v === null || (v === "" && type !== "String"))
        return null;
    if (type.startsWith("PaginaResponse<") && isObject(v))
        return {
            ...v,
            itens: (v.itens as unknown[]).map((x) =>
                toWire(type.slice(15, -1), x),
            ),
            totalItens: new LosslessNumber(String(v.totalItens)),
        };
    const list = listType(type);
    if (list) return (v as unknown[]).map((x) => toWire(list, x));
    if (type.startsWith("Map<")) return mapNumbers(v);
    const fields = records[type];
    if (fields && isObject(v)) {
        const result = Object.fromEntries(
            fields
                .filter(
                    (f) =>
                        v[f.name] !== undefined &&
                        !(f.omitNull && v[f.name] === null),
                )
                .map((f) => [f.name, toWire(f.type, v[f.name])]),
        );
        if (type === "ContingenciaDto.Registrar") {
            const cfg = contingencyTypes[String(v.tipo)],
                root = v.dados;
            if (!cfg || !isObject(root) || !isObject(root.dados))
                throw new InputError(
                    "Dados tipados de contingência incompletos.",
                );
            result.dados = {
                ...root,
                ...Object.fromEntries(
                    cfg.targets.map((f) => [
                        f.name,
                        toWire(f.type, root[f.name]),
                    ]),
                ),
                dados: toWire(cfg.dto, root.dados),
            };
        }
        return result;
    }
    if (["Long", "long", "BigDecimal"].includes(type))
        return new LosslessNumber(String(v));
    if (["int", "Integer"].includes(type)) return Number(v);
    return v;
}
function mapNumbers(v: unknown): unknown {
    if (Array.isArray(v)) return v.map(mapNumbers);
    if (isObject(v) && !isLosslessNumber(v))
        return Object.fromEntries(
            Object.entries(v).map(([k, x]) => [k, mapNumbers(x)]),
        );
    return v;
}
export function encode(type: string, v: unknown): string {
    validateValue(type, v);
    return stringify(toWire(type, v))!;
}
export function decode(type: string, v: unknown, field?: Field): unknown {
    if (v === null) {
        if (field?.required || ["long", "int", "boolean"].includes(type))
            throw new Error("Resposta incompatível: campo obrigatório nulo.");
        return null;
    }
    const list = listType(type);
    if (list) {
        if (!Array.isArray(v))
            throw new Error("Resposta incompatível: lista esperada.");
        return v.map((x) => decode(list, x, field?.element));
    }
    if (type.startsWith("PaginaResponse<")) {
        if (!isObject(v) || !Array.isArray(v.itens))
            throw new Error("Resposta paginada incompleta.");
        const child = type.slice(15, -1);
        return {
            itens: v.itens.map((x) => decode(child, x)),
            pagina: decode("int", v.pagina),
            tamanho: decode("int", v.tamanho),
            totalItens: decode("long", v.totalItens),
            totalPaginas: decode("int", v.totalPaginas),
        };
    }
    if (type === "unknown" || type === "Object" || type.startsWith("Map<"))
        return v;
    if (["Long", "long", "BigDecimal"].includes(type)) {
        const s = textNumber(v);
        if (
            type !== "BigDecimal" &&
            (!/^-?\d+$/.test(s) ||
                BigInt(s) > 9223372036854775807n ||
                BigInt(s) < -9223372036854775808n)
        )
            throw new Error("Long incompatível.");
        return s;
    }
    if (["int", "Integer"].includes(type)) {
        const s = textNumber(v);
        if (
            !/^-?\d+$/.test(s) ||
            BigInt(s) > 2147483647n ||
            BigInt(s) < -2147483648n
        )
            throw new Error("Inteiro incompatível.");
        return Number(s);
    }
    if (["boolean", "Boolean"].includes(type)) {
        if (typeof v !== "boolean")
            throw new Error("Resposta booleana incompatível.");
        return v;
    }
    if (records[type]) {
        if (!isObject(v)) throw new Error("Resposta incompatível: " + type);
        return Object.fromEntries(
            records[type]
                .filter((f) => !(f.omitNull && !(f.name in v)))
                .map((f) => {
                    if (!(f.name in v))
                        throw new Error("Resposta incompleta: " + f.name);
                    return [f.name, decode(f.type, v[f.name], f)];
                }),
        );
    }
    if (typeof v !== "string")
        throw new Error("Resposta textual incompatível: " + type);
    if (enums[type] && !enums[type].includes(v))
        throw new Error("Estado não reconhecido: " + type);
    return v;
}
export function parseResponse(type: string, text: string): unknown {
    // Na raiz, um DTO, coleção ou página exige sua estrutura. A nulabilidade
    // de membros continua derivada de cada Field, inclusive NON_NULL.
    const structured =
        !!records[type] ||
        !!listType(type) ||
        type.startsWith("PaginaResponse<");
    return decode(
        type,
        parse(text),
        structured ? { name: "resposta", type, required: true } : undefined,
    );
}
export const parseExact = (s: string): unknown => parse(s);
export const stringifyExact = (v: unknown): string => stringify(v) ?? "";
export function initialValue(
    type: string,
    ctx: Values = {},
    sample = false,
): unknown {
    const list = listType(type);
    if (list) return [];
    if (records[type])
        return Object.fromEntries(
            records[type].map((f) => [f.name, initialField(f, ctx, sample)]),
        );
    if (type.startsWith("Map<")) return {};
    if (type === "boolean" || type === "Boolean") return false;
    if (enums[type]) return sample ? enums[type][0] : "";
    return "";
}
export function initialField(f: Field, ctx: Values, sample = false): unknown {
    if (f.name in ctx) return ctx[f.name];
    if (f.name === "operacaoId") return crypto.randomUUID();
    if (f.default !== undefined)
        return ["boolean", "Boolean"].includes(f.type)
            ? f.default === "true"
            : f.default;
    if (records[f.type] && !f.required) return null;
    // Um controle opcional com formato obrigatório começa sem informação.
    // String obrigatória vazia continua sendo texto, validado por seus constraints.
    if (f.type === "String" && f.Pattern && !f.required) return null;
    if (sample && f.required && !records[f.type] && !listType(f.type)) {
        if (f.type === "UUID") return "00000000-0000-4000-8000-000000000001";
        if (f.type === "Instant") return "2026-10-08T12:00:00.123456Z";
        if (f.type === "LocalDate") return "2026-10-08";
        if (["Long", "long", "Integer", "int"].includes(f.type))
            return /versao|revisao/.test(f.name) ? "0" : "1";
        if (f.type === "BigDecimal") return "10.000000";
        if (f.type === "String")
            return /motivo|observacao|criterio/.test(f.name)
                ? "Exercício fictício local"
                : /referencia|codigo|sku/.test(f.name)
                  ? "DEMO-01"
                  : "Demonstração";
    }
    return initialValue(f.type, ctx, sample);
}
