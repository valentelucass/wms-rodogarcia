import { useContext, type ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import { FormReferences } from "../../components/FormReferences";
import { initialValue } from "../../contracts/codec";
import {
    isObject,
    records,
    type Perfil,
    type Values,
} from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";
import { Icon } from "../../design-system/Icon";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const itemSchema = "PedidoSaidaDto.ItemCriar";
const assigned = new Set(["clienteId", "armazemId", "itens", "motivo"]);

export function DispatchOrderFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const refs = useContext(FormReferences);
    const blocked = (group: Group, name: string) =>
        disabled ||
        Boolean(group.disabled) ||
        Boolean(group.lockedFields?.includes(name)) ||
        isFieldDisabled(group.schema ?? "", name, perfil);
    const scalar = (name: string, labelOverride?: string) =>
        groups.flatMap((group, index) => {
            const field = group.fields.find((field) => field.name === name);
            return field
                ? [
                      <ScalarField
                          key={index + name}
                          f={field}
                          path={name}
                          value={group.values[name]}
                          schema={group.schema}
                          row={{ ...group.root, ...group.values }}
                          labelOverride={labelOverride}
                          disabled={blocked(group, name)}
                          onChange={(value) =>
                              group.onChange({ ...group.values, [name]: value })
                          }
                      />,
                  ]
                : [];
        });
    const itemGroup = groups.find((group) =>
        group.fields.some((field) => field.name === "itens"),
    );
    const itemField = itemGroup?.fields.find((field) => field.name === "itens");
    const rows =
        itemGroup && Array.isArray(itemGroup.values.itens)
            ? itemGroup.values.itens
            : [];
    const itemsDisabled = !itemGroup || blocked(itemGroup, "itens");
    const changeItems = (next: unknown[]) =>
        itemGroup?.onChange({ ...itemGroup.values, itens: next });
    const updateItem = (index: number, name: string, value: unknown) =>
        changeItems(
            rows.map((row, i) =>
                i === index
                    ? { ...(isObject(row) ? row : {}), [name]: value }
                    : row,
            ),
        );
    return (
        <div className="dispatch-order-fields">
            {groups.some((group) =>
                group.fields.some((field) =>
                    ["clienteId", "armazemId"].includes(field.name),
                ),
            ) && (
                <section
                    className="dispatch-order-context"
                    aria-label="Cliente e armazém"
                >
                    <h3>Cliente e armazém</h3>
                    <div className="dispatch-order-grid">
                        {scalar("clienteId")}
                        {scalar("armazemId")}
                    </div>
                </section>
            )}
            {itemGroup && itemField && (
                <section
                    className="dispatch-order-items"
                    aria-label="Itens do pedido"
                >
                    <header className="dispatch-order-items-heading">
                        <div>
                            <h3>
                                Itens do pedido{itemField.required ? " *" : ""}
                            </h3>
                            <p className="hint">
                                {information.dispatchOrder.description}
                            </p>
                        </div>
                        <button
                            className="action-add"
                            type="button"
                            disabled={itemsDisabled}
                            onClick={() =>
                                changeItems([
                                    ...rows,
                                    initialValue(itemSchema, refs.defaults),
                                ])
                            }
                        >
                            Adicionar item
                        </button>
                    </header>
                    {rows.length === 0 && (
                        <p className="empty">
                            Nenhum item adicionado. Adicione um produto para
                            continuar.
                        </p>
                    )}
                    <ol className="dispatch-order-item-list">
                        {rows.map((value, index) => {
                            const row: Values = isObject(value) ? value : {};
                            return (
                                <li
                                    className="dispatch-order-item"
                                    key={index}
                                    aria-label={`Item ${index + 1}`}
                                >
                                    <header className="dispatch-order-item-heading">
                                        <h4>Item {index + 1}</h4>
                                        <button
                                            type="button"
                                            className="action-remove"
                                            disabled={itemsDisabled}
                                            aria-label={`Remover item ${index + 1}`}
                                            onClick={() =>
                                                changeItems(
                                                    rows.filter(
                                                        (_, i) => i !== index,
                                                    ),
                                                )
                                            }
                                        >
                                            <Icon name="close" />
                                            Remover
                                        </button>
                                    </header>
                                    <div className="dispatch-order-item-grid">
                                        {records[itemSchema].map((field) => (
                                            <ScalarField
                                                key={field.name}
                                                f={field}
                                                path={field.name}
                                                schema={itemSchema}
                                                value={row[field.name]}
                                                row={{
                                                    ...itemGroup.root,
                                                    ...itemGroup.values,
                                                    ...row,
                                                }}
                                                showDecimalHint={false}
                                                disabled={
                                                    itemsDisabled ||
                                                    isFieldDisabled(
                                                        itemSchema,
                                                        field.name,
                                                        perfil,
                                                    )
                                                }
                                                onChange={(next) =>
                                                    updateItem(
                                                        index,
                                                        field.name,
                                                        next,
                                                    )
                                                }
                                            />
                                        ))}
                                    </div>
                                </li>
                            );
                        })}
                    </ol>
                    <p className="hint">
                        {information.dispatchOrderQuantity.description}
                    </p>
                </section>
            )}
            <section
                className="dispatch-order-reason"
                aria-label="Justificativa do pedido"
            >
                {scalar("motivo", "Justificativa do pedido")}
            </section>
            {groups.some((group) =>
                group.fields.some((field) => !assigned.has(field.name)),
            ) && (
                <section
                    className="dispatch-order-additional"
                    aria-label="Dados adicionais"
                >
                    {groups.map((group, index) => (
                        <Fields
                            key={index}
                            {...group}
                            fields={group.fields.filter(
                                (field) => !assigned.has(field.name),
                            )}
                            perfil={perfil}
                            disabled={disabled}
                        />
                    ))}
                </section>
            )}
        </div>
    );
}
