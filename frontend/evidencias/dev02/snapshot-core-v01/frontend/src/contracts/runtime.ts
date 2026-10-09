import rawSchemas from "./schemas.json";
import rawEndpoints from "./endpoints.json";
export type Perfil = "GESTOR" | "SUPERVISOR" | "OPERACAO";
export interface Field {
    name: string;
    type: string;
    required: boolean;
    omitNull?: boolean;
    notBlank?: boolean;
    source?: string;
    default?: string;
    Size?: string;
    Digits?: string;
    Min?: string;
    Max?: string;
    DecimalMin?: string;
    DecimalMax?: string;
    Pattern?: string;
    positive?: boolean;
    element?: Field;
}
export interface Endpoint {
    id: string;
    method: string;
    path: string;
    request: string | null;
    response: string;
    query: Field[];
    params: Field[];
    multipart: boolean;
    source: string;
    handler: string;
    permission: Perfil;
    permissionSource: string[];
}
export const records: Record<string, Field[]> = rawSchemas.records;
export const enums: Record<string, string[]> = rawSchemas.enums;
export const endpoints: Endpoint[] = rawEndpoints as Endpoint[];
export const endpoint = (id: string): Endpoint => {
    const e = endpoints.find((e) => e.id === id);
    if (!e) throw new Error("Ação sem contrato.");
    return e;
};
export const canPresent = (perfil: Perfil, required: Perfil) =>
    required === "OPERACAO" || perfil === "GESTOR" || perfil === required;
export type Values = Record<string, unknown>;
export const isObject = (v: unknown): v is Values =>
    v !== null && typeof v === "object" && !Array.isArray(v);
export const listType = (type: string) =>
    type.startsWith("List<") ? type.slice(5, -1) : null;
