import type {
    TiposUnidadePermitidos,
    TipoUnidadeLogistica,
} from "../contracts/types";

export const unitTypes: TipoUnidadeLogistica[] = ["PALLET", "BOBINA", "VOLUME"];
const profiles: Record<
    TiposUnidadePermitidos,
    readonly TipoUnidadeLogistica[]
> = {
    PALLET: ["PALLET"],
    BOBINA: ["BOBINA"],
    VOLUME: ["VOLUME"],
    PALLET_BOBINA: ["PALLET", "BOBINA"],
    PALLET_VOLUME: ["PALLET", "VOLUME"],
    BOBINA_VOLUME: ["BOBINA", "VOLUME"],
    TODAS: unitTypes,
};
export const unitTypeLabels: Record<TipoUnidadeLogistica, string> = {
    PALLET: "Pallet",
    BOBINA: "Bobina",
    VOLUME: "Volume",
};
export function selectedUnitTypes(
    value: unknown,
): readonly TipoUnidadeLogistica[] {
    return profiles[value as TiposUnidadePermitidos] ?? [];
}
export function unitTypeProfile(
    selected: readonly TipoUnidadeLogistica[],
): TiposUnidadePermitidos | null {
    if (!selected.length) return null;
    return (
        (Object.keys(profiles) as TiposUnidadePermitidos[]).find(
            (key) =>
                profiles[key].length === selected.length &&
                profiles[key].every((type) => selected.includes(type)),
        ) ?? null
    );
}
export function permittedUnitTypesLabel(value: unknown): string {
    if (value === "TODAS") return "Todas as opções";
    return (
        selectedUnitTypes(value)
            .map((type) => unitTypeLabels[type])
            .join(", ") || "Não informado"
    );
}
