import { ScalarField } from "../../components/fields/ScalarField";
import { initialValue } from "../../contracts/codec";
import {
    isObject,
    records,
    type Field,
    type Values,
} from "../../contracts/runtime";
import { ResolutionReferences } from "./ResolutionReferences";
import "./historicalResolution.css";

const commitments = {
    PEDIDO_ENTRADA: "Pedido de entrada",
    PEDIDO_SAIDA: "Pedido de saída",
    FECHAMENTO: "Fechamento de cobrança",
};

export function HistoricalResolutionField({
    field,
    value,
    root,
    disabled,
    onChange,
}: {
    field: Field;
    value: unknown;
    root: Values;
    disabled: boolean;
    onChange: (value: unknown) => void;
}) {
    return (
        <fieldset className="historical-resolution" disabled={disabled}>
            <legend>
                Resolução financeira histórica{field.required ? " *" : ""}
            </legend>
            <span className="historical-resolution-manager">
                Ação do Gestor
            </span>
            {isObject(value) ? (
                <>
                    <ResolutionReferences
                        value={value}
                        disabled={disabled}
                        onChange={onChange}
                    />
                    <div className="historical-resolution-fields">
                        <h4>Identificação do compromisso</h4>
                        <div className="historical-resolution-grid">
                            {records[field.type].map((nested) => (
                                <ScalarField
                                    key={nested.name}
                                    f={nested}
                                    value={value[nested.name]}
                                    path=""
                                    schema={field.type}
                                    row={{ ...root, ...value }}
                                    disabled={disabled}
                                    labelOverride={
                                        nested.name === "tipo"
                                            ? "Tipo de compromisso"
                                            : nested.name === "compromissoId"
                                              ? "Compromisso"
                                              : undefined
                                    }
                                    choiceLabels={commitments}
                                    onChange={(next) =>
                                        onChange({
                                            ...value,
                                            [nested.name]: next,
                                        })
                                    }
                                />
                            ))}
                        </div>
                    </div>
                    {!field.required && (
                        <div className="historical-resolution-actions">
                            <button
                                type="button"
                                className="action-remove"
                                onClick={() => onChange(null)}
                            >
                                Remover resolução financeira
                            </button>
                        </div>
                    )}
                </>
            ) : (
                <div className="historical-resolution-actions">
                    <button
                        type="button"
                        className="action-details"
                        onClick={() => onChange(initialValue(field.type))}
                    >
                        Informar resolução financeira
                    </button>
                </div>
            )}
        </fieldset>
    );
}
