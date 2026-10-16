import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import type { Perfil } from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const sections = [
    {
        title: "Cliente e armazém",
        kind: "context",
        names: ["clienteId", "armazemId"],
    },
    {
        title: "Serviço e execução",
        kind: "execution",
        names: ["servicoId", "origem", "executadoEm", "referenciaExecucao"],
    },
    {
        title: "Vínculos da execução",
        kind: "references",
        names: ["unidadeId", "produtoId", "pedidoEntradaId", "pedidoSaidaId"],
    },
    {
        title: "Quantidade e valor",
        kind: "quantity",
        names: ["quantidade", "categoria", "valorBase"],
        help: information.serviceFactValues.description,
    },
    { title: "Rateio", kind: "allocation", names: ["cotas", "criterioRateio"] },
    {
        title: "Justificativa e resolução histórica",
        kind: "reason",
        names: ["motivo", "resolucao"],
    },
];
const labels: Record<string, string> = {
    origem: "Origem do registro",
    executadoEm: "Data e hora da execução",
    referenciaExecucao: "Referência da execução",
    valorBase: "Valor base",
    criterioRateio: "Critério de rateio",
};
const origins = { MANUAL: "Manual", SUGESTAO: "Sugestão" };
const wide = new Set([
    "servicoId",
    "referenciaExecucao",
    "cotas",
    "criterioRateio",
    "motivo",
    "resolucao",
]);

export function ServiceFactFields({
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
                    Boolean(group.disabled) ||
                    Boolean(group.lockedFields?.includes(name)) ||
                    isFieldDisabled(group.schema ?? "", name, perfil);
                return [
                    <div
                        className={
                            "service-fact-field" +
                            (wide.has(name) ? " service-fact-field--wide" : "")
                        }
                        key={index + name}
                    >
                        {["cotas", "resolucao"].includes(name) ? (
                            <Fields
                                {...group}
                                fields={[field]}
                                perfil={perfil}
                                disabled={blocked}
                            />
                        ) : (
                            <ScalarField
                                f={field}
                                value={group.values[name]}
                                path={group.prefix ?? ""}
                                schema={group.schema}
                                row={{ ...group.root, ...group.values }}
                                labelOverride={labels[name]}
                                choiceLabels={
                                    name === "origem" ? origins : undefined
                                }
                                showDecimalHint={false}
                                disabled={blocked}
                                onChange={(value) =>
                                    group.onChange({
                                        ...group.values,
                                        [name]: value,
                                    })
                                }
                            />
                        )}
                    </div>,
                ];
            }),
        );
    return (
        <div className="service-fact-fields">
            <p className="hint service-fact-intro">
                {information.serviceFact.description}
            </p>
            {sections.map(
                (section) =>
                    groups.some((group) =>
                        group.fields.some((field) =>
                            section.names.includes(field.name),
                        ),
                    ) && (
                        <fieldset
                            className={`service-fact-section service-fact-section--${section.kind}`}
                            key={section.kind}
                        >
                            <legend>{section.title}</legend>
                            {section.help && (
                                <p className="hint service-fact-section-help">
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
                <fieldset className="service-fact-section">
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
