import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import { initialValue } from "../../contracts/codec";
import { isObject, records, type Perfil } from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";
import { ResolutionReferences } from "../financeiro/ResolutionReferences";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const labels: Record<string, string> = {
    corte: "Data de corte",
    motivo: "Motivo do encerramento",
    tipo: "Tipo da vigência",
    id: "Registro",
};
const commitmentChoices = {
    PEDIDO_ENTRADA: "Pedido de entrada",
    PEDIDO_SAIDA: "Pedido de saída",
    FECHAMENTO: "Fechamento de cobrança",
};

export function EndValidityFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const scalar = (names: string[]) =>
        groups.flatMap((group, index) =>
            group.fields
                .filter((field) => names.includes(field.name))
                .map((field) => (
                    <ScalarField
                        key={index + field.name}
                        f={field}
                        value={group.values[field.name]}
                        path=""
                        schema={group.schema}
                        row={group.values}
                        labelOverride={labels[field.name]}
                        disabled={
                            disabled ||
                            isFieldDisabled(
                                group.schema ?? "",
                                field.name,
                                perfil,
                            )
                        }
                        onChange={(value) =>
                            group.onChange({
                                ...group.values,
                                [field.name]: value,
                            })
                        }
                    />
                )),
        );
    const context = scalar(["tipo", "id", "versao"]);
    return (
        <div className="end-validity-fields">
            {context.length > 0 && (
                <section
                    className="end-validity-section"
                    aria-label="Vigência consultada"
                >
                    <h3>Vigência consultada</h3>
                    <div className="end-validity-grid">{context}</div>
                </section>
            )}
            <section
                className="end-validity-section"
                aria-label="Data do encerramento"
            >
                <h3>Data do encerramento</h3>
                <p className="hint">{information.endValidity.description}</p>
                <div className="end-validity-cut">{scalar(["corte"])}</div>
            </section>
            {groups.flatMap((group, index) =>
                group.fields
                    .filter((field) => field.name === "resolucao")
                    .map((field) => {
                        const value = group.values.resolucao;
                        const blocked =
                            disabled ||
                            isFieldDisabled(
                                group.schema ?? "",
                                "resolucao",
                                perfil,
                            );
                        const change = (resolucao: unknown) =>
                            group.onChange({ ...group.values, resolucao });
                        return (
                            <fieldset
                                key={index}
                                className="end-validity-resolution"
                                disabled={blocked}
                            >
                                <legend>
                                    Resolução financeira histórica
                                    {field.required ? " *" : ""}
                                </legend>
                                <span className="end-validity-manager">
                                    Ação do Gestor
                                </span>
                                {isObject(value) ? (
                                    <>
                                        <ResolutionReferences
                                            value={value}
                                            onChange={change}
                                            disabled={blocked}
                                        />
                                        <div className="end-validity-manual">
                                            <h4>
                                                Identificação do compromisso
                                            </h4>
                                            <div className="end-validity-grid">
                                                {records[field.type].map(
                                                    (nested) => (
                                                        <ScalarField
                                                            key={nested.name}
                                                            f={nested}
                                                            value={
                                                                value[
                                                                    nested.name
                                                                ]
                                                            }
                                                            path=""
                                                            schema={field.type}
                                                            row={{
                                                                ...group.values,
                                                                ...value,
                                                            }}
                                                            disabled={blocked}
                                                            labelOverride={
                                                                nested.name ===
                                                                "tipo"
                                                                    ? "Tipo de compromisso"
                                                                    : nested.name ===
                                                                        "compromissoId"
                                                                      ? "Compromisso"
                                                                      : undefined
                                                            }
                                                            choiceLabels={
                                                                commitmentChoices
                                                            }
                                                            onChange={(next) =>
                                                                change({
                                                                    ...value,
                                                                    [nested.name]:
                                                                        next,
                                                                })
                                                            }
                                                        />
                                                    ),
                                                )}
                                            </div>
                                        </div>
                                        {!field.required && (
                                            <button
                                                type="button"
                                                className="action-remove"
                                                onClick={() => change(null)}
                                            >
                                                Remover resolução financeira
                                            </button>
                                        )}
                                    </>
                                ) : (
                                    <button
                                        type="button"
                                        className="action-details"
                                        onClick={() =>
                                            change(initialValue(field.type))
                                        }
                                    >
                                        Informar resolução financeira
                                    </button>
                                )}
                            </fieldset>
                        );
                    }),
            )}
            <section
                className="end-validity-section"
                aria-label="Justificativa do encerramento"
            >
                <h3>Justificativa</h3>
                {scalar(["motivo"])}
            </section>
            {groups.map((group, index) => (
                <Fields
                    key={index}
                    {...group}
                    fields={group.fields.filter(
                        (field) =>
                            ![
                                "tipo",
                                "id",
                                "versao",
                                "corte",
                                "resolucao",
                                "motivo",
                            ].includes(field.name),
                    )}
                    perfil={perfil}
                    disabled={disabled}
                />
            ))}
        </div>
    );
}
