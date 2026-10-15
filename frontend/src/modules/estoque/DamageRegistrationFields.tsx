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
import { information } from "../../content/information";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const sections = [
    {
        title: "Unidade afetada",
        names: ["codigo", "versaoUnidade"],
        help: information.damageUnit.description,
        kind: "unit",
    },
    {
        title: "Dados do fato",
        names: ["quantidade", "ocorridaEm"],
        kind: "fact",
    },
    {
        title: "Destino da mercadoria",
        names: ["destinos", "conjuntoId"],
        help: information.damageDestinations.description,
        kind: "destination",
    },
    {
        title: "Justificativa e pendências",
        names: ["motivo", "resolverPendentes"],
        kind: "reason",
    },
];

export function DamageRegistrationFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const assigned = new Set(sections.flatMap((section) => section.names));
    const render = (names: string[]) =>
        names.flatMap((name) =>
            groups.flatMap((group, index) => {
                const field = group.fields.find((field) => field.name === name);
                if (!field) return [];
                const change = (value: unknown) =>
                    group.onChange({ ...group.values, [name]: value });
                if (name === "destinos")
                    return [
                        <DamageDestinations
                            key={name}
                            value={group.values[name]}
                            onChange={change}
                            root={group.values}
                            perfil={perfil}
                            disabled={disabled}
                        />,
                    ];
                if (name === "quantidade")
                    return [
                        <div className="damage-quantity" key={name}>
                            <ScalarField
                                f={field}
                                value={group.values[name]}
                                path={name}
                                onChange={change}
                                disabled={disabled}
                                schema={group.schema}
                                row={group.values}
                                showDecimalHint={false}
                            />
                            <p className="hint">
                                {information.damageQuantity.description}
                            </p>
                        </div>,
                    ];
                return [
                    <Fields
                        key={index + name}
                        {...group}
                        fields={[field]}
                        perfil={perfil}
                        disabled={disabled}
                    />,
                ];
            }),
        );
    return (
        <div className="damage-registration-fields">
            {sections.map(
                (section) =>
                    groups.some((group) =>
                        group.fields.some((field) =>
                            section.names.includes(field.name),
                        ),
                    ) && (
                        <fieldset
                            className={`damage-section damage-section--${section.kind}`}
                            key={section.kind}
                        >
                            <legend>{section.title}</legend>
                            {section.help && (
                                <p className="hint damage-section-help">
                                    {section.help}
                                </p>
                            )}
                            {render(section.names)}
                        </fieldset>
                    ),
            )}
            {groups.some((group) =>
                group.fields.some((field) => !assigned.has(field.name)),
            ) && (
                <fieldset className="damage-section">
                    <legend>Dados adicionais</legend>
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
                </fieldset>
            )}
        </div>
    );
}

function DamageDestinations({
    value,
    onChange,
    root,
    perfil,
    disabled,
}: {
    value: unknown;
    onChange: (value: unknown) => void;
    root: Values;
    perfil: Perfil;
    disabled: boolean;
}) {
    const refs = useContext(FormReferences);
    const rows = Array.isArray(value) ? value : [];
    return (
        <fieldset className="damage-destinations" disabled={disabled}>
            <legend>Posições de destino *</legend>
            {rows.length === 0 && (
                <p className="hint">
                    {information.damageDestinationsEmpty.description}
                </p>
            )}
            {rows.map((row, index) => (
                <div
                    className="damage-destination"
                    role="group"
                    aria-label={`Posição ${index + 1}`}
                    key={index}
                >
                    <header>
                        <strong>Posição {index + 1}</strong>
                        <button
                            type="button"
                            className="action-remove"
                            onClick={() =>
                                onChange(
                                    rows.filter(
                                        (_, position) => position !== index,
                                    ),
                                )
                            }
                        >
                            Remover posição {index + 1}
                        </button>
                    </header>
                    <div className="damage-destination-fields">
                        <Fields
                            fields={records["EstoqueDto.Destino"]}
                            values={isObject(row) ? row : {}}
                            onChange={(next) =>
                                onChange(
                                    rows.map((old, position) =>
                                        position === index ? next : old,
                                    ),
                                )
                            }
                            root={root}
                            perfil={perfil}
                            disabled={disabled}
                            schema="EstoqueDto.Destino"
                        />
                    </div>
                </div>
            ))}
            <div className="damage-destination-actions">
                <button
                    type="button"
                    className="action-add"
                    onClick={() =>
                        onChange([
                            ...rows,
                            initialValue("EstoqueDto.Destino", refs.defaults),
                        ])
                    }
                >
                    Adicionar posição
                </button>
            </div>
        </fieldset>
    );
}
