import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import { type Field, type Perfil, type Values } from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const sections = [
    {
        title: "Cliente e vigência",
        names: [
            "clienteId",
            "armazemId",
            "vigenciaInicio",
            "vigenciaFim",
            "fuso",
            "moeda",
        ],
        kind: "context",
    },
    {
        title: "Ciclo de cobrança",
        names: ["modalidadeCiclo", "diaCorte", "duracaoDias"],
        kind: "cycle",
    },
    {
        title: "Mínimo de cobrança",
        names: [
            "minimoModo",
            "minimoValor",
            "minimoProporcao",
            "servicosMinimo",
        ],
        kind: "minimum",
        help: information.contractMinimum.description,
    },
    {
        title: "GRIS",
        names: [
            "grisModo",
            "grisPercentual",
            "grisBase",
            "grisPeriodicidade",
            "grisProporcao",
        ],
        kind: "gris",
        help: information.contractGris.description,
    },
    {
        title: "Justificativa e resolução histórica",
        names: ["motivo", "resolucao"],
        kind: "reason",
    },
];
const labels: Record<string, string> = {
    vigenciaInicio: "Início da vigência",
    vigenciaFim: "Fim da vigência",
    fuso: "Fuso do contrato",
    modalidadeCiclo: "Modalidade do ciclo",
    diaCorte: "Dia de corte",
    duracaoDias: "Duração (dias)",
    minimoModo: "Aplicação do mínimo",
    minimoValor: "Valor mínimo",
    minimoProporcao: "Proporção",
    grisModo: "Aplicação do GRIS",
    grisPercentual: "Percentual (%)",
    grisBase: "Base de cálculo",
    grisPeriodicidade: "Periodicidade",
    grisProporcao: "Proporção",
};
const choices: Record<string, string> = {
    NAO_INFORMADO: "Não informado",
    NAO_APLICAVEL: "Não aplicável",
    APLICAVEL: "Aplicável",
    MES_DIA_FIXO: "Mês com dia de corte",
    DIAS_CORRIDOS: "Dias corridos",
    INTEGRAL: "Integral",
    PROPORCIONAL_DIAS: "Proporcional aos dias",
    VALOR_ESTOQUE_PICO: "Pico de valor do estoque",
    VALOR_ESTOQUE_MEDIO: "Média dos picos diários do estoque",
    DIARIA: "Diária",
    POR_CICLO: "Por ciclo",
};

export function ContractConfigurationFields({
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
                const blocked =
                    disabled ||
                    isFieldDisabled(group.schema ?? "", name, perfil);
                const change = (value: unknown) =>
                    group.onChange({ ...group.values, [name]: value });
                if (name === "servicosMinimo")
                    return [
                        <MinimumServices
                            key={name}
                            field={field}
                            value={group.values[name]}
                            onChange={change}
                            root={group.values}
                            schema={group.schema}
                            disabled={blocked}
                        />,
                    ];
                if (name === "resolucao")
                    return [
                        <div className="contract-resolution" key={name}>
                            <p className="hint">
                                {information.contractHistorical.description}
                            </p>
                            <Fields
                                {...group}
                                fields={[field]}
                                perfil={perfil}
                                disabled={blocked}
                            />
                        </div>,
                    ];
                return [
                    <div
                        key={index + name}
                        className={
                            "contract-field" +
                            ([
                                "modalidadeCiclo",
                                "minimoModo",
                                "grisModo",
                                "motivo",
                            ].includes(name)
                                ? " contract-field--wide"
                                : "")
                        }
                    >
                        <ScalarField
                            f={field}
                            value={group.values[name]}
                            path={name}
                            onChange={change}
                            disabled={blocked}
                            schema={group.schema}
                            row={group.values}
                            labelOverride={labels[name]}
                            choiceLabels={choices}
                            showDecimalHint={false}
                        />
                    </div>,
                ];
            }),
        );
    return (
        <div className="contract-configuration-fields">
            <p className="hint contract-configuration-help">
                {information.contractConfiguration.description}
            </p>
            {sections.map(
                (section) =>
                    groups.some((group) =>
                        group.fields.some((field) =>
                            section.names.includes(field.name),
                        ),
                    ) && (
                        <fieldset
                            className={`contract-section contract-section--${section.kind}`}
                            key={section.kind}
                        >
                            <legend>{section.title}</legend>
                            {section.help && (
                                <p className="hint contract-section-help">
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
                <fieldset className="contract-section">
                    <legend>Dados adicionais</legend>
                    {groups.map((group, index) => (
                        <Fields
                            {...group}
                            key={index}
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

function MinimumServices({
    field,
    value,
    onChange,
    root,
    schema,
    disabled,
}: {
    field: Field;
    value: unknown;
    onChange: (value: unknown) => void;
    root: Values;
    schema?: string;
    disabled: boolean;
}) {
    const list: unknown[] = Array.isArray(value) ? value : [];
    return (
        <fieldset className="contract-minimum-services" disabled={disabled}>
            <legend>
                Serviços abrangidos pelo mínimo{field.required ? " *" : ""}
            </legend>
            {list.length === 0 && (
                <p className="hint">
                    {information.contractMinimumEmpty.description}
                </p>
            )}
            {list.map((service, index) => (
                <div
                    className="contract-service-row"
                    role="group"
                    aria-label={`Serviço ${index + 1}`}
                    key={index}
                >
                    <ScalarField
                        f={{
                            ...field.element,
                            name: "servicoId",
                            type: "Long",
                            required: field.element?.required ?? true,
                        }}
                        value={service}
                        path="servicoId"
                        onChange={(next) =>
                            onChange(
                                list.map((old, i) =>
                                    i === index ? next : old,
                                ),
                            )
                        }
                        disabled={disabled}
                        schema={schema}
                        row={root}
                        labelOverride={`Serviço ${index + 1}`}
                    />
                    <button
                        type="button"
                        className="action-remove"
                        disabled={disabled}
                        onClick={() =>
                            onChange(list.filter((_, i) => i !== index))
                        }
                    >
                        Remover serviço {index + 1}
                    </button>
                </div>
            ))}
            <div className="contract-service-actions">
                <button
                    type="button"
                    className="action-add"
                    disabled={disabled}
                    onClick={() => onChange([...list, ""])}
                >
                    Adicionar serviço
                </button>
            </div>
        </fieldset>
    );
}
