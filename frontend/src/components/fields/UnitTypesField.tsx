import { useId } from "react";
import {
    selectedUnitTypes,
    unitTypes,
    unitTypeLabels,
    unitTypeProfile,
} from "../../domain/unitTypes";

export function UnitTypesField({
    name,
    value,
    disabled,
    required,
    onChange,
}: {
    name: string;
    value: unknown;
    disabled: boolean;
    required: boolean;
    onChange: (value: unknown) => void;
}) {
    const id = useId();
    const selected = selectedUnitTypes(value);
    return (
        <div className="field">
            <fieldset
                className="unit-types-field"
                disabled={disabled}
                aria-describedby={id + "-hint"}
                aria-required={required}
            >
                <legend>{name}</legend>
                <label className="unit-type-choice unit-type-all">
                    <input
                        type="checkbox"
                        checked={selected.length === unitTypes.length}
                        onChange={(e) =>
                            onChange(e.target.checked ? "TODAS" : null)
                        }
                    />
                    Todas as opções
                </label>
                <div className="unit-type-options">
                    {unitTypes.map((type) => (
                        <label className="unit-type-choice" key={type}>
                            <input
                                type="checkbox"
                                checked={selected.includes(type)}
                                onChange={(e) =>
                                    onChange(
                                        unitTypeProfile(
                                            e.target.checked
                                                ? [...selected, type]
                                                : selected.filter(
                                                      (item) => item !== type,
                                                  ),
                                        ),
                                    )
                                }
                            />
                            {unitTypeLabels[type]}
                        </label>
                    ))}
                </div>
                <span className="hint" id={id + "-hint"}>
                    Selecione um ou mais tipos de unidade.
                </span>
            </fieldset>
        </div>
    );
}
