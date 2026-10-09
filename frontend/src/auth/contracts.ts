import type { Perfil } from "../contracts/runtime";
import { isObject } from "../contracts/runtime";
import {
    decode,
    parseExact,
    stringifyExact,
    toWire,
    validateField,
} from "../contracts/codec";

/** Contrato nativo AcessoDtos, separado dos DTOs operacionais gerados.
 * Long usa lexema decimal em memória e número JSON no transporte.
 * Origem: backend/dto/AcessoDtos.java e LoginController.csrf. */
export interface User {
    id: string;
    nome: string;
    email: string;
    perfil: Perfil;
    administrador: boolean;
    principal: boolean;
    ativo: boolean;
    trocarSenha: boolean;
    clientes: string[];
    armazens: string[];
    versao: string;
}
export interface Tokens {
    accessToken: string;
    expiresIn: string;
    usuario: User;
}
export interface UsersPage {
    content: User[];
    number: number;
    totalPages: number;
    totalElements: string;
}
const required = (name: string, type: string) => ({
    name,
    type,
    required: true,
});
function object(v: unknown): Record<string, unknown> {
    if (!isObject(v)) throw new Error("Objeto obrigatório ausente.");
    return v;
}
function text(v: unknown, nonempty = false): string {
    if (typeof v !== "string" || (nonempty && !v.trim()))
        throw new Error("Texto obrigatório incompatível.");
    return v;
}
function long(v: unknown): string {
    return decode("long", v, required("long", "long")) as string;
}
function strings(v: unknown): string[] {
    if (!Array.isArray(v)) throw new Error("Lista obrigatória ausente.");
    return v.map((item) => text(item));
}
export function decodeUser(v: unknown): User {
    const value = object(v),
        perfil = text(value.perfil);
    if (!["GESTOR", "SUPERVISOR", "OPERACAO"].includes(perfil))
        throw new Error("Perfil incompatível.");
    return {
        id: text(value.id, true),
        nome: text(value.nome),
        email: text(value.email),
        perfil: perfil as Perfil,
        administrador: decode("boolean", value.administrador) as boolean,
        principal: decode("boolean", value.principal) as boolean,
        ativo: decode("boolean", value.ativo) as boolean,
        trocarSenha: decode("boolean", value.trocarSenha) as boolean,
        clientes: strings(value.clientes),
        armazens: strings(value.armazens),
        versao: long(value.versao),
    };
}
export function decodeTokens(v: unknown): Tokens {
    const value = object(v),
        expiresIn = long(value.expiresIn);
    // LoginService publica access de300s; o relógio não converte um Long arbitrário.
    if (expiresIn !== "300") throw new Error("Prazo de sessão incompatível.");
    return {
        accessToken: text(value.accessToken, true),
        expiresIn,
        usuario: decodeUser(value.usuario),
    };
}
export function decodeUsersPage(v: unknown): UsersPage {
    const value = object(v);
    if (!Array.isArray(value.content))
        throw new Error("Página obrigatória ausente.");
    return {
        content: value.content.map(decodeUser),
        number: decode("int", value.number) as number,
        totalPages: decode("int", value.totalPages) as number,
        totalElements: long(value.totalElements),
    };
}
export function decodeCsrf(v: unknown): { token: string; header: string } {
    const value = object(v),
        header = text(value.header, true);
    // CookieCsrfTokenRepository atual. Não permitir sobrescrever Bearer/Host/etc.
    if (header !== "X-XSRF-TOKEN")
        throw new Error("Cabeçalho CSRF incompatível.");
    return { header, token: text(value.token, true) };
}
export function decodeAuth<T>(raw: string, decoder: (v: unknown) => T): T {
    return decoder(parseExact(raw));
}
export function encodeRevisionBody(body: unknown): string {
    const value = object(body),
        revision = value.versao;
    if (typeof revision === "number" && !Number.isSafeInteger(revision))
        throw new Error("Revisão numérica sem precisão. Consulte novamente.");
    validateField(required("versao", "long"), revision);
    return stringifyExact({ ...value, versao: toWire("long", revision) });
}
