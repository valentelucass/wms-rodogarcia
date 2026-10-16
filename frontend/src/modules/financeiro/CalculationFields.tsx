import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import type { Perfil } from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";
import { HistoricalResolutionField } from "./HistoricalResolutionField";
import "./calculation.css";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const assignments = [
    "clienteId",
    "armazemId",
    "periodoInicio",
    "periodoFim",
    "motivo",
    "resolucao",
];

export function CalculationFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const blocked = (group: Group, name: string) =>
        disabled ||
        Boolean(group.disabled) ||
        Boolean(group.lockedFields?.includes(name)) ||
        isFieldDisabled(group.schema ?? "", name, perfil);
    const scalar = (names: string[]) =>
        groups.flatMap((group, index) =>
            group.fields
                .filter((field) => names.includes(field.name))
                .map((field) => (
                    <ScalarField
                        key={index + field.name}
                        f={field}
                        value={group.values[field.name]}
                        path={group.prefix ?? ""}
                        schema={group.schema}
                        row={{ ...group.root, ...group.values }}
                        disabled={blocked(group, field.name)}
                        onChange={(value) =>
                            group.onChange({
                                ...group.values,
                                [field.name]: value,
                            })
                        }
                    />
                )),
        );
    const context = scalar(["clienteId", "armazemId"]);
    return (
        <div className="calculation-fields">
            {context.length > 0 && (
                <section
                    className="calculation-section"
                    aria-label="Contexto do cálculo"
                >
                    <h3>Cliente e armazém</h3>
                    <div className="calculation-grid">{context}</div>
                </section>
            )}
            <section
                className="calculation-section"
                aria-label="Período do cálculo"
            >
                <h3>Período do cálculo</h3>
                <p className="hint">
                    {information.serviceCalculation.description}
                </p>
                <div className="calculation-grid">
                    {scalar(["periodoInicio", "periodoFim"])}
                </div>
            </section>
            <section
                className="calculation-section"
                aria-label="Justificativa do cálculo"
            >
                <h3>Justificativa</h3>
                {scalar(["motivo"])}
            </section>
            {groups.flatMap((group, index) =>
                group.fields
                    .filter((field) => field.name === "resolucao")
                    .map((field) => (
                        <HistoricalResolutionField
                            key={index}
                            field={field}
                            value={group.values.resolucao}
                            root={{ ...group.root, ...group.values }}
                            disabled={blocked(group, "resolucao")}
                            onChange={(resolucao) =>
                                group.onChange({ ...group.values, resolucao })
                            }
                        />
                    )),
            )}
            {groups.map((group, index) => (
                <Fields
                    key={index}
                    {...group}
                    fields={group.fields.filter(
                        (field) => !assignments.includes(field.name),
                    )}
                    perfil={perfil}
                    disabled={disabled || group.disabled}
                />
            ))}
        </div>
    );
}
