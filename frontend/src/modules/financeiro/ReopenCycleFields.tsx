import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import type { Perfil } from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";
import { HistoricalResolutionField } from "./HistoricalResolutionField";
import "./reopenCycle.css";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const assigned = ["numero", "calculoId", "motivo", "resolucao"];
const labels: Record<string, string> = {
    numero: "Versão atual do ciclo",
    calculoId: "Cálculo de referência",
};

export function ReopenCycleFields({
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
                        labelOverride={labels[field.name]}
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
    return (
        <div className="reopen-cycle-fields">
            <section
                className="reopen-cycle-section"
                aria-label="Versão e cálculo da reabertura"
            >
                <h3>Versão e cálculo</h3>
                <p className="hint">{information.reopenCycle.description}</p>
                <div className="reopen-cycle-grid">
                    {scalar(["numero", "calculoId"])}
                </div>
            </section>
            <section
                className="reopen-cycle-section"
                aria-label="Justificativa da reabertura"
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
                        (field) => !assigned.includes(field.name),
                    )}
                    perfil={perfil}
                    disabled={disabled || group.disabled}
                />
            ))}
        </div>
    );
}
