import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import { InformationHint } from "../../components/information/InformationPopup";
import type { Field, Perfil } from "../../contracts/runtime";
import { information } from "../../content/information";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { ContingencyData } from "./ContingencyFields";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const identity = [
    "identidadeFato",
    "tipo",
    "efeitoRegistradoNoWms",
    "clienteId",
    "armazemId",
];
const source = ["operador", "ocorridaEm", "fonte"];
const labels: Record<string, string> = {
    identidadeFato: "Identidade do fato",
    ocorridaEm: "Instante real do fato",
    efeitoRegistradoNoWms: "Efeito já registrado no WMS",
    motivo: "Justificativa do registro",
};

export function ContingencyRegistrationFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const body = groups.find((g) => g.fields.some((f) => f.name === "dados"));
    const assigned = new Set([
        ...identity,
        ...source,
        "dados",
        "dependencias",
        "motivo",
    ]);
    const render = (names: string[]) =>
        names.flatMap((name) =>
            groups.flatMap((group, i) => {
                const field = group.fields.find((f) => f.name === name);
                return field
                    ? [
                          <ScalarField
                              key={i + name}
                              f={field}
                              value={group.values[name]}
                              path=""
                              schema={group.schema}
                              row={group.values}
                              labelOverride={labels[name]}
                              disabled={
                                  disabled ||
                                  isFieldDisabled(
                                      group.schema ?? "",
                                      name,
                                      perfil,
                                  )
                              }
                              onChange={(value) =>
                                  group.onChange({
                                      ...group.values,
                                      [name]: value,
                                  })
                              }
                          />,
                      ]
                    : [];
            }),
        );
    return (
        <div className="contingency-registration-fields">
            <section className="contingency-form-section">
                <header>
                    <h3>Identificação da ocorrência</h3>
                    <InformationHint topic="contingencyRegistration" />
                </header>
                <div className="contingency-field-grid">
                    <div className="contingency-field-wide">
                        {render(["identidadeFato"])}
                    </div>
                    {render(
                        identity.filter((name) => name !== "identidadeFato"),
                    )}
                </div>
            </section>
            <section className="contingency-form-section">
                <h3>Registro na planilha</h3>
                <div className="contingency-field-grid">{render(source)}</div>
            </section>
            {body?.fields.find((f) => f.name === "dependencias") && (
                <Dependencies
                    field={body.fields.find((f) => f.name === "dependencias")!}
                    value={body.values.dependencias}
                    disabled={disabled}
                    onChange={(dependencias) =>
                        body.onChange({ ...body.values, dependencias })
                    }
                />
            )}
            {body && (
                <ContingencyData
                    value={body.values.dados}
                    kind={String(body.values.tipo ?? "")}
                    onChange={(dados) =>
                        body.onChange({ ...body.values, dados })
                    }
                    disabled={disabled}
                    perfil={perfil}
                    Fields={Fields}
                />
            )}
            <section
                className="contingency-form-section contingency-form-section--reason"
                aria-label="Justificativa do registro"
            >
                {render(["motivo"])}
                <p className="hint">
                    {information.contingencyRegistrationReason.description}
                </p>
            </section>
            {groups.some((g) =>
                g.fields.some((f) => !assigned.has(f.name)),
            ) && (
                <section className="contingency-form-section">
                    <h3>Outras referências</h3>
                    <div className="contingency-field-grid">
                        {groups.map((group, i) => (
                            <Fields
                                key={i}
                                {...group}
                                fields={group.fields.filter(
                                    (f) => !assigned.has(f.name),
                                )}
                                perfil={perfil}
                                disabled={disabled}
                            />
                        ))}
                    </div>
                </section>
            )}
        </div>
    );
}

function Dependencies({
    field,
    value,
    disabled,
    onChange,
}: {
    field: Field;
    value: unknown;
    disabled: boolean;
    onChange: (values: unknown[]) => void;
}) {
    const values = Array.isArray(value) ? value : [];
    return (
        <section
            className="contingency-form-section"
            aria-label="Dependências do fato"
        >
            <header>
                <h3>Dependências</h3>
                <InformationHint topic="contingencyDependencies" />
            </header>
            {values.length === 0 && (
                <p className="hint">Nenhuma dependência informada.</p>
            )}
            {values.map((value, i) => (
                <div className="contingency-dependency" key={i}>
                    <ScalarField
                        f={{
                            ...field.element,
                            name: "dependencia",
                            type: "String",
                            required: field.element?.required ?? false,
                        }}
                        value={value}
                        path=""
                        disabled={disabled}
                        labelOverride={`Identidade da dependência ${i + 1}`}
                        onChange={(next) =>
                            onChange(
                                values.map((old, index) =>
                                    index === i ? next : old,
                                ),
                            )
                        }
                    />
                    <button
                        type="button"
                        className="action-remove"
                        disabled={disabled}
                        onClick={() =>
                            onChange(values.filter((_, index) => index !== i))
                        }
                    >
                        Remover dependência {i + 1}
                    </button>
                </div>
            ))}
            <div className="contingency-dependency-actions">
                <button
                    type="button"
                    className="action-add"
                    disabled={disabled}
                    onClick={() => onChange([...values, ""])}
                >
                    Adicionar dependência
                </button>
            </div>
        </section>
    );
}
