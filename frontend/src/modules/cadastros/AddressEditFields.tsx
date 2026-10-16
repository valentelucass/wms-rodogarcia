import type { ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import type { Perfil } from "../../contracts/runtime";
import { isFieldDisabled } from "../../domain/fieldPresentation";
import { information } from "../../content/information";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const labels: Record<string, string> = {
    descricao: "Descrição",
    codigo: "Código",
    tipo: "Tipo de endereço",
    rua: "Rua",
    nivel: "Nível",
    posicao: "Posição",
    capacidadePesoKg: "Peso máximo (kg)",
    empilhamentoMaximo: "Empilhamento máximo",
    alturaMetros: "Altura (m)",
    larguraMetros: "Largura (m)",
    profundidadeMetros: "Profundidade (m)",
    sequenciaColeta: "Sequência de coleta",
    tipoUnidadePermitido: "Tipos de unidade permitidos",
    motivo: "Motivo / justificativa",
};

export function AddressEditFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const has = (names: string[]) =>
        groups.some((group) =>
            group.fields.some((field) => names.includes(field.name)),
        );
    const render = (names: string[]) =>
        names.flatMap((name) =>
            groups.flatMap((group, index) => {
                const field = group.fields.find((field) => field.name === name);
                if (!field) return [];
                return [
                    <div
                        className={`address-edit-field address-edit-field--${name}`}
                        key={index + name}
                    >
                        <ScalarField
                            f={field}
                            value={group.values[name]}
                            path={
                                group.prefix ? `${group.prefix}.${name}` : name
                            }
                            schema={group.schema}
                            row={{ ...group.root, ...group.values }}
                            labelOverride={labels[name]}
                            showDecimalHint={false}
                            disabled={
                                disabled ||
                                Boolean(group.disabled) ||
                                Boolean(group.lockedFields?.includes(name)) ||
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
                        />
                    </div>,
                ];
            }),
        );
    return (
        <div className="address-edit-fields">
            {has([
                "descricao",
                "codigo",
                "tipo",
                "rua",
                "nivel",
                "posicao",
            ]) && (
                <fieldset className="address-edit-section">
                    <legend>Identificação e localização</legend>
                    <div className="address-edit-grid">
                        {render(["descricao", "codigo", "tipo"])}
                    </div>
                    <div className="address-edit-dimensions">
                        {render(["rua", "nivel", "posicao"])}
                    </div>
                </fieldset>
            )}
            {has([
                "capacidadePesoKg",
                "empilhamentoMaximo",
                "alturaMetros",
                "larguraMetros",
                "profundidadeMetros",
            ]) && (
                <fieldset className="address-edit-section">
                    <legend>Capacidade e dimensões</legend>
                    <p className="hint">
                        {information.addressCapacityValues.description}
                    </p>
                    <div className="address-edit-grid">
                        {render(["capacidadePesoKg", "empilhamentoMaximo"])}
                    </div>
                    <div className="address-edit-dimensions">
                        {render([
                            "alturaMetros",
                            "larguraMetros",
                            "profundidadeMetros",
                        ])}
                    </div>
                </fieldset>
            )}
            {has(["sequenciaColeta", "tipoUnidadePermitido"]) && (
                <fieldset className="address-edit-section">
                    <legend>Coleta e unidades permitidas</legend>
                    <div className="address-edit-collection">
                        {render(["sequenciaColeta", "tipoUnidadePermitido"])}
                    </div>
                </fieldset>
            )}
            {has(["motivo"]) && (
                <fieldset className="address-edit-section address-edit-reason">
                    <legend>Justificativa da alteração</legend>
                    {render(["motivo"])}
                </fieldset>
            )}
            {groups.map((group, index) => (
                <Fields
                    key={index}
                    {...group}
                    fields={group.fields.filter(
                        (field) => !(field.name in labels),
                    )}
                    perfil={perfil}
                    disabled={disabled || group.disabled}
                />
            ))}
        </div>
    );
}
