import { useState } from "react";
import { instantToLocal, localToInstant } from "../../domain/dateTime";
import { information } from "../../content/information";

export function DateTimeField({
    id,
    name,
    value,
    onChange,
    required,
    disabled,
}: {
    id: string;
    name: string;
    value: unknown;
    onChange: (value: unknown) => void;
    required: boolean;
    disabled: boolean;
}) {
    const [error, setError] = useState("");
    return (
        <div className="field">
            <label htmlFor={id}>{name}</label>
            <input
                id={id}
                type="datetime-local"
                lang="pt-BR"
                step="any"
                value={instantToLocal(value)}
                required={required}
                disabled={disabled}
                aria-describedby={
                    id + "-hint" + (error ? " " + id + "-error" : "")
                }
                aria-invalid={error ? true : undefined}
                onChange={(event) => {
                    const next = localToInstant(event.target.value);
                    if (next === null) {
                        setError(
                            "Escolha uma data e um horário válidos em Brasília.",
                        );
                        return;
                    }
                    setError("");
                    onChange(next);
                }}
            />
            <span className="hint" id={id + "-hint"}>
                {information.operationDateTime.description}
            </span>
            {error && (
                <span id={id + "-error"} role="alert">
                    {error}
                </span>
            )}
        </div>
    );
}
