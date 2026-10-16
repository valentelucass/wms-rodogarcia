import { records, isObject, type Perfil } from "../../contracts/runtime";
import { contingencyTypes } from "../../contracts/contingency";
import type { Fields as FieldsView } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import { information } from "../../content/information";
export function ContingencyData({
    value,
    onChange,
    disabled,
    perfil,
    kind,
    Fields,
}: {
    value: unknown;
    onChange: (v: unknown) => void;
    disabled: boolean;
    perfil: Perfil;
    kind: string;
    Fields: typeof FieldsView;
}) {
    const config = contingencyTypes[kind];
    const data = isObject(value) ? value : {};
    if (!config) return <p>Selecione o tipo de fato.</p>;
    return (
        <fieldset disabled={disabled} className="contingency-data-fields">
            <legend>
                {kind === "CONTAGEM"
                    ? "Dados da contagem"
                    : `Dados do fato de ${kind.replaceAll("_", " ").toLocaleLowerCase("pt-BR")}`}
            </legend>
            <p className="hint">
                {kind === "CONTAGEM"
                    ? information.contingencyCount.description
                    : information.contingencyCommand.description}
            </p>
            {config.targets.length > 0 && (
                <div className="contingency-field-grid">
                    <Fields
                        fields={config.targets}
                        values={data}
                        onChange={onChange}
                        perfil={perfil}
                        disabled={disabled}
                    />
                </div>
            )}
            <div className="contingency-field-grid">
                {kind === "CONTAGEM" ? (
                    records[config.dto]
                        .filter((f) => f.name !== "operacaoId")
                        .map((f) => (
                            <ScalarField
                                key={f.name}
                                f={f}
                                value={
                                    isObject(data.dados)
                                        ? data.dados[f.name]
                                        : undefined
                                }
                                path=""
                                schema={config.dto}
                                row={isObject(data.dados) ? data.dados : {}}
                                disabled={disabled}
                                labelOverride={
                                    {
                                        codigoUnidade: "UUID da unidade",
                                        versaoUnidade: "Versão da unidade",
                                        contado: "Quantidade contada",
                                        observadoEm: "Instante da observação",
                                        motivo: "Motivo da contagem",
                                    }[f.name]
                                }
                                onChange={(value) =>
                                    onChange({
                                        ...data,
                                        dados: {
                                            ...(isObject(data.dados)
                                                ? data.dados
                                                : {}),
                                            [f.name]: value,
                                        },
                                    })
                                }
                            />
                        ))
                ) : (
                    <Fields
                        fields={records[config.dto].filter(
                            (f) => f.name !== "operacaoId",
                        )}
                        values={isObject(data.dados) ? data.dados : {}}
                        onChange={(v) => onChange({ ...data, dados: v })}
                        perfil={perfil}
                        disabled={disabled}
                        schema={config.dto}
                    />
                )}
            </div>
        </fieldset>
    );
}
