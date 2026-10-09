import { records, listType, isObject, type Values } from "../contracts/runtime";
import type { Workflow } from "./workflow";

/** Substitui somente coleções presentes no DTO autoritativo desse pai.
 * Consultas parciais de um filho e respostas Resumo não apagam irmãos.
 */
export function replaceAuthoritativeCollections(
    state: Workflow,
    root: string,
    parentId: unknown,
    children: readonly string[],
    type: string,
    value: Values,
): void {
    const collections = new Set<string>();
    const collectType = (t: string) => {
        if (children.includes(t)) collections.add(t);
        for (const field of records[t] ?? []) {
            const child = listType(field.type);
            if (child) collectType(child);
        }
    };
    const visit = (t: string, v: unknown) => {
        if (!isObject(v)) return;
        for (const field of records[t] ?? []) {
            if (!(field.name in v)) continue;
            const child = listType(field.type);
            if (child) collectType(child);
            else if (records[field.type]) visit(field.type, v[field.name]);
        }
    };
    visit(type, value);
    for (const child of collections) {
        state.catalogs[child] = (state.catalogs[child] ?? []).filter((row) => {
            const key = child + ":" + String(row.id ?? "singleton");
            const owner = state.owners[key];
            if (owner?.root !== root || owner.id !== parentId) return true;
            delete state.owners[key];
            if (state.selected[child]?.id === row.id)
                delete state.selected[child];
            return false;
        });
    }
}
