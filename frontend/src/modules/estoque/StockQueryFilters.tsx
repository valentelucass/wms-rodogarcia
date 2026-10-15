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
        names: ["enderecoId", "disponivel", "situacao", "impedimento"],
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
const initialLoadGroups: FilterGroup[] = [
    stockGroups[0],
    {
        title: "Dados da carga",
        names: ["produtoId", "referencia", "situacao"],
        kind: "initial-load",
    },
];
const contingencyGroups: FilterGroup[] = [
    stockGroups[0],
    {
        title: "Registro e situação",
        names: ["identidadeFato", "tipo", "situacao", "pendencia"],
    },
];
const groupsBySchema: Record<string, FilterGroup[]> = {
    "IndicadorEstoqueController.listar.query": indicatorGroups,
    "CargaInicialController.listar.query": initialLoadGroups,
    "ContingenciaController.listar.query": contingencyGroups,
};

export function StockQueryFilters(props: ComponentProps<typeof Fields>) {
    const indicators =
        props.schema === "IndicadorEstoqueController.listar.query";
    const initialLoad = props.schema === "CargaInicialController.listar.query";
    const contingency = props.schema === "ContingenciaController.listar.query";
    const groups = groupsBySchema[props.schema ?? ""] ?? stockGroups;
    const known = new Set(groups.flatMap((group) => group.names));
    const other = props.fields.filter((field) => !known.has(field.name));
    return (
        <div
            className={
                "stock-filter-grid" +
                (indicators ? " stock-filter-grid--indicators" : "") +
                (initialLoad ? " stock-filter-grid--initial-load" : "") +
                (contingency ? " stock-filter-grid--contingency" : "")
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
