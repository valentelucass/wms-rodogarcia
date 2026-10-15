import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";

type FilterGroup = { title: string; names: string[]; kind?: string };
const stockGroups: FilterGroup[] = [
    { title: "Cliente e armazém", names: ["clienteId", "armazemId"] },
    {
        title: "Identificação da unidade",
        names: ["produtoId", "codigoUnidade"],
    },
    {
        title: "Localização e situação",
        names: ["enderecoId", "disponivel", "situacao"],
        kind: "location",
    },
];
const indicatorGroups: FilterGroup[] = [
    {
        title: "Contexto da consulta",
        names: ["clienteId", "armazemId", "produtoId"],
        kind: "indicator-context",
    },
    { title: "Opções do relatório", names: ["valor", "fuso"], kind: "options" },
];

export function StockQueryFilters(props: ComponentProps<typeof Fields>) {
    const indicators =
        props.schema === "IndicadorEstoqueController.listar.query";
    const groups = indicators ? indicatorGroups : stockGroups;
    const known = new Set(groups.flatMap((group) => group.names));
    const other = props.fields.filter((field) => !known.has(field.name));
    return (
        <div
            className={
                "stock-filter-grid" +
                (indicators ? " stock-filter-grid--indicators" : "")
            }
        >
            {groups.map((group) => {
                const fields = group.names.flatMap((name) =>
                    props.fields.filter((field) => field.name === name),
                );
                return (
                    fields.length > 0 && (
                        <fieldset
                            className={
                                "stock-filter-group" +
                                (group.kind
                                    ? ` stock-filter-group--${group.kind}`
                                    : "")
                            }
                            key={group.title}
                        >
                            <legend>{group.title}</legend>
                            <Fields {...props} fields={fields} />
                        </fieldset>
                    )
                );
            })}
            {other.length > 0 && (
                <fieldset className="stock-filter-group stock-filter-group--other">
                    <legend>Outros filtros</legend>
                    <Fields {...props} fields={other} />
                </fieldset>
            )}
        </div>
    );
}
