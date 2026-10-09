import { useId, useContext } from "react";
import { enums, type Field } from "../../contracts/runtime";
import { label } from "../../domain/labels";
import { FormReferences } from "../FormReferences";
import { XmlField } from "./XmlField";
export function ScalarField({
    f,
    value,
    path,
    onChange,
    disabled,
}: {
    f: Field;
    value: unknown;
    path: string;
    onChange: (v: unknown) => void;
    disabled: boolean;
}) {
    const id = useId(),
        refs = useContext(FormReferences);
    const title = label(f.name) + (f.required ? " *" : "");
    const choices =
        enums[f.type] ??
        f.Pattern?.match(/regexp = "([A-Z_|]+)"/)?.[1].split("|");
    const name = prefixLabel(path, title);
    if (refs.options[f.name]?.length && ["Long", "long"].includes(f.type))
        return (
            <div className="field">
                <label htmlFor={id}>{name}</label>
                <select
                    id={id}
                    disabled={disabled}
                    value={String(value ?? "")}
                    required={f.required}
                    onChange={(e) => onChange(e.target.value)}
                >
                    <option value="">Selecione o registro consultado</option>
                    {!refs.options[f.name].some(
                        (o) => o.value === String(value),
                    ) &&
                        value !== "" &&
                        value !== undefined && (
                            <option value={String(value)}>
                                {String(value)} (referência atual)
                            </option>
                        )}
                    {refs.options[f.name].map((o) => (
                        <option key={o.value} value={o.value}>
                            {o.title}
                        </option>
                    ))}
                </select>
            </div>
        );
    if (choices)
        return (
            <div className="field">
                <label htmlFor={id}>{name}</label>
                <select
                    id={id}
                    disabled={disabled}
                    required={f.required}
                    value={String(value ?? "")}
                    onChange={(e) => onChange(e.target.value)}
                >
                    <option value="">Selecione</option>
                    {choices.map((x) => (
                        <option key={x} value={x}>
                            {x.replaceAll("_", " ")}
                        </option>
                    ))}
                </select>
            </div>
        );
    if (f.type === "boolean" || f.type === "Boolean")
        return (
            <div className="field">
                <label htmlFor={id}>{name}</label>
                <select
                    id={id}
                    disabled={disabled}
                    value={
                        value === null || value === undefined
                            ? ""
                            : String(value)
                    }
                    onChange={(e) =>
                        onChange(
                            e.target.value === ""
                                ? null
                                : e.target.value === "true",
                        )
                    }
                    required={f.required}
                >
                    <option value="">Não informado</option>
                    <option value="false">Não</option>
                    <option value="true">Sim</option>
                </select>
            </div>
        );
    const multiline = /xml|motivo|observacao|descricao|fonte|criterio/i.test(
        f.name,
    );
    const max = f.Size?.match(/max = (\d+)/)?.[1];
    if (/xml/i.test(f.name))
        return (
            <XmlField
                id={id}
                name={name}
                value={value}
                onChange={onChange}
                disabled={disabled}
                required={f.required}
                maxLength={max ? Number(max) : undefined}
            />
        );
    return (
        <div className="field">
            <label htmlFor={id}>{name}</label>
            {multiline ? (
                <textarea
                    id={id}
                    disabled={disabled}
                    rows={/xml/i.test(f.name) ? 5 : 2}
                    required={f.required}
                    maxLength={max ? Number(max) : undefined}
                    value={String(value ?? "")}
                    onChange={(e) => onChange(e.target.value)}
                />
            ) : (
                <input
                    id={id}
                    disabled={disabled || f.name === "operacaoId"}
                    type={f.type === "LocalDate" ? "date" : "text"}
                    inputMode={
                        [
                            "Long",
                            "long",
                            "Integer",
                            "int",
                            "BigDecimal",
                        ].includes(f.type)
                            ? "decimal"
                            : undefined
                    }
                    required={f.required}
                    maxLength={max ? Number(max) : undefined}
                    value={String(value ?? "")}
                    onChange={(e) => onChange(e.target.value)}
                    placeholder={
                        f.type === "Instant"
                            ? "2026-10-08T12:00:00.123456Z"
                            : undefined
                    }
                />
            )}
            {f.type === "BigDecimal" && (
                <span className="hint">
                    Decimal exato; use ponto. Sem cálculo no navegador.
                </span>
            )}
            {f.name === "operacaoId" && (
                <span className="hint">
                    Conservado na repetição do mesmo conteúdo.
                </span>
            )}
        </div>
    );
}
function prefixLabel(path: string, title: string): string {
    const parts = path.split(".");
    if (parts.length < 2) return title;
    return (
        parts
            .slice(0, -1)
            .map((x) =>
                /^\d+$/.test(x) ? "Item " + (Number(x) + 1) : label(x),
            )
            .join(" / ") +
        " / " +
        title
    );
}
