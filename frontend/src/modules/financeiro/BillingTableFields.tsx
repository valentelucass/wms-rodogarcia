import { useContext, useId, type ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { FormReferences } from "../../components/FormReferences";
import { ScalarField } from "../../components/fields/ScalarField";
import { initialValue } from "../../contracts/codec";
import { isObject, records, type Perfil } from "../../contracts/runtime";
import { information } from "../../content/information";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { HistoricalResolutionField } from "./HistoricalResolutionField";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const assigned = new Set([
    "armazemId",
    "clienteId",
    "descricao",
    "tipo",
    "vigenciaInicio",
    "vigenciaFim",
    "itens",
    "motivo",
    "resolucao",
]);
const labels: Record<string, string> = {
    vigenciaInicio: "Início da vigência",
    vigenciaFim: "Fim da vigência",
    preco: "Preço",
    percentual: "Percentual (%)",
};

export function BillingTableFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const descriptionId = useId();
    const refs = useContext(FormReferences);
    const render = (names: string[]) =>
        names.flatMap((name) =>
            groups.flatMap((group, index) => {
                const field = group.fields.find((f) => f.name === name);
                if (!field) return [];
                const change = (value: unknown) =>
                    group.onChange({ ...group.values, [name]: value });
                const blocked =
                    disabled ||
                    isFieldDisabled(group.schema ?? "", name, perfil);
                if (name === "resolucao")
                    return [
                        <details
                            className="billing-table-history"
                            key={index + name}
                        >
                            <summary>
                                Resolução financeira histórica{" "}
                                <span>Ação do Gestor</span>
                                {isObject(group.values[name]) && (
                                    <span>Informada</span>
                                )}
                            </summary>
                            <p className="hint">
                                {information.contractHistorical.description}
                            </p>
                            <HistoricalResolutionField
                                field={field}
                                value={group.values[name]}
                                root={group.values}
                                disabled={blocked}
                                onChange={change}
                            />
                        </details>,
                    ];
                if (name === "itens") {
                    const list: unknown[] = Array.isArray(group.values[name])
                        ? group.values[name]
                        : [];
                    return [
                        <section
                            className="billing-table-services"
                            aria-label="Serviços da tabela"
                            key={index + name}
                        >
                            <div className="billing-table-section-heading">
                                <h3>
                                    Serviços da tabela
                                    {field.required ? " *" : ""}{" "}
                                    <span className="billing-table-count">
                                        {list.length}
                                    </span>
                                </h3>
                                <button
                                    type="button"
                                    disabled={blocked}
                                    onClick={() =>
                                        change([
                                            ...list,
                                            initialValue(
                                                "ConfiguracaoCobrancaDto.Item",
                                                refs.defaults,
                                            ),
                                        ])
                                    }
                                >
                                    Adicionar serviço
                                </button>
                            </div>
                            <p className="hint">
                                {information.billingTableItems.description}
                            </p>
                            {list.length === 0 && (
                                <p className="billing-table-empty">
                                    Adicione o primeiro serviço para informar os
                                    valores da tabela.
                                </p>
                            )}
                            {list.map((item, i) => {
                                const row = isObject(item) ? item : {};
                                return (
                                    <div
                                        className="billing-table-item"
                                        role="group"
                                        aria-label={`Serviço ${i + 1}`}
                                        key={i}
                                    >
                                        <div className="billing-table-item-heading">
                                            <h4>Serviço {i + 1}</h4>
                                            <button
                                                type="button"
                                                className="billing-table-remove"
                                                aria-label={`Remover serviço ${i + 1}`}
                                                disabled={blocked}
                                                onClick={() =>
                                                    change(
                                                        list.filter(
                                                            (_, j) => i !== j,
                                                        ),
                                                    )
                                                }
                                            >
                                                Remover
                                            </button>
                                        </div>
                                        <div className="billing-table-item-grid">
                                            {records[
                                                "ConfiguracaoCobrancaDto.Item"
                                            ].map((nested) => (
                                                <ScalarField
                                                    key={nested.name}
                                                    f={nested}
                                                    value={row[nested.name]}
                                                    path=""
                                                    schema="ConfiguracaoCobrancaDto.Item"
                                                    row={{
                                                        ...group.values,
                                                        ...row,
                                                    }}
                                                    disabled={blocked}
                                                    showDecimalHint={false}
                                                    labelOverride={
                                                        labels[nested.name]
                                                    }
                                                    onChange={(value) =>
                                                        change(
                                                            list.map(
                                                                (old, j) =>
                                                                    j === i
                                                                        ? {
                                                                              ...row,
                                                                              [nested.name]:
                                                                                  value,
                                                                          }
                                                                        : old,
                                                            ),
                                                        )
                                                    }
                                                />
                                            ))}
                                        </div>
                                    </div>
                                );
                            })}
                        </section>,
                    ];
                }
                return [
                    <div
                        className={`billing-table-field billing-table-field--${name}`}
                        key={index + name}
                    >
                        {name === "descricao" ? (
                            <div className="field">
                                <label htmlFor={descriptionId}>
                                    Descrição{field.required ? " *" : ""}
                                </label>
                                <input
                                    id={descriptionId}
                                    value={String(group.values[name] ?? "")}
                                    required={field.required}
                                    maxLength={Number(
                                        field.Size?.match(/max = (\d+)/)?.[1] ??
                                            160,
                                    )}
                                    disabled={blocked}
                                    onChange={(e) => change(e.target.value)}
                                />
                            </div>
                        ) : (
                            <ScalarField
                                f={field}
                                value={group.values[name]}
                                path=""
                                schema={group.schema}
                                row={group.values}
                                disabled={blocked}
                                labelOverride={labels[name]}
                                choiceLabels={{
                                    PADRAO: "Padrão",
                                    ESPECIFICA: "Específica por cliente",
                                }}
                                onChange={change}
                            />
                        )}
                    </div>,
                ];
            }),
        );
    return (
        <div className="billing-table-fields">
            <section
                className="billing-table-section"
                aria-label="Dados da tabela"
            >
                <h3>Dados da tabela</h3>
                <div className="billing-table-context">
                    {render(["armazemId", "clienteId", "descricao", "tipo"])}
                </div>
                <div className="billing-table-dates">
                    {render(["vigenciaInicio", "vigenciaFim"])}
                </div>
                <p className="hint">
                    {information.billingTableScope.description}
                </p>
            </section>
            {render(["itens"])}
            <section
                className="billing-table-section"
                aria-label="Justificativa da tabela"
            >
                {render(["motivo"])}
            </section>
            {render(["resolucao"])}
            {groups.map((group, index) => (
                <Fields
                    {...group}
                    key={index}
                    fields={group.fields.filter((f) => !assigned.has(f.name))}
                    perfil={perfil}
                    disabled={disabled}
                />
            ))}
        </div>
    );
}
