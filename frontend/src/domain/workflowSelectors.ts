import type { Values } from "../contracts/runtime";
import type { Workflow } from "./workflow";
import { receivingSelection } from "../modules/recebimento/selection";
import { dispatchSelection } from "../modules/saida/selection";

// Leituras puras do catálogo. Os módulos não precisam carregar o redutor que
// coordena suas próprias referências para consultar vínculo/atualidade.
export const selectionKey = (type: string, value: Values) =>
    type + ":" + String(value.id ?? "singleton");
export function ownedSelection(
    state: Workflow,
    type: string,
    root: string,
): Values | undefined {
    const value = state.selected[type];
    const owner = value && state.owners[selectionKey(type, value)];
    return owner?.root === root && owner.id === state.selected[root]?.id
        ? value
        : undefined;
}
export function referenceCatalog(state: Workflow, type: string): Values[] {
    const family = [receivingSelection, dispatchSelection].find((f) =>
        f.children.includes(type),
    );
    return (state.catalogs[type] ?? []).filter(
        (value) =>
            !family ||
            (state.selected[family.root]?.id !== undefined &&
                state.owners[selectionKey(type, value)]?.root === family.root &&
                state.owners[selectionKey(type, value)]?.id ===
                    state.selected[family.root].id),
    );
}
