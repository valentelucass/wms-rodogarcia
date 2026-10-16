import { useContext, type ComponentProps } from "react";
import { Fields } from "../../components/Fields";
import { ScalarField } from "../../components/fields/ScalarField";
import { FormReferences } from "../../components/FormReferences";
import { InformationHint } from "../../components/information/InformationPopup";
import { initialValue } from "../../contracts/codec";
import {
    isObject,
    records,
    type Perfil,
    type Values,
} from "../../contracts/runtime";
import { information, type InformationKey } from "../../content/information";

type Group = Omit<ComponentProps<typeof Fields>, "perfil">;
const dataType = "CargaInicialDto.Dados";
const unitType = "UnidadeLogisticaDto.NovaUnidade";
const identity = [
    "clienteId",
    "armazemId",
    "produtoId",
    "referencia",
    "etiquetaFornecida",
    "quantidade",
];
const dataSections: {
    title: string;
    names: string[];
    help?: InformationKey;
}[] = [
    {
        title: "Origem da mercadoria",
        names: ["entradaExistenteId", "referenciaPedido", "nota"],
        help: "initialLoadOrigin",
    },
    {
        title: "Datas e lote",
        names: ["chegadaReal", "dataFifo", "lote", "validade"],
        help: "initialLoadDates",
    },
    {
        title: "Quantidades levantadas",
        names: ["quantidadeBoa", "quantidadeAvariada"],
    },
];

export function InitialLoadFields({
    groups,
    perfil,
    disabled,
}: {
    groups: Group[];
    perfil: Perfil;
    disabled: boolean;
}) {
    const body = groups.find((g) => g.fields.some((f) => f.name === "dados"));
    const data = body && isObject(body.values.dados) ? body.values.dados : {};
    const setData = (next: Values) =>
        body?.onChange({ ...body.values, dados: next });
    const root = { ...body?.values, ...data };
    const renderRoot = (names: string[]) =>
        names.flatMap((name) =>
            groups.flatMap((group, index) => {
                const field = group.fields.find((f) => f.name === name);
                return field
                    ? [
                          <ScalarField
                              key={index + name}
                              f={field}
                              value={group.values[name]}
                              path={name}
                              schema={group.schema}
                              row={group.values}
                              disabled={disabled}
                              labelOverride={
                                  name === "quantidade"
                                      ? "Quantidade da carga"
                                      : undefined
                              }
                              showDecimalHint={false}
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
    const renderData = (names: string[]) =>
        names.flatMap((name) => {
            const field = records[dataType].find((f) => f.name === name);
            if (!field) return [];
            return [
                field.type === "BigDecimal" ? (
                    <ScalarField
                        key={name}
                        f={field}
                        value={data[name]}
                        path={name}
                        schema={dataType}
                        row={root}
                        disabled={disabled}
                        showDecimalHint={false}
                        labelOverride={
                            {
                                quantidadeBoa: "Quantidade boa",
                                quantidadeAvariada: "Quantidade avariada",
                            }[name]
                        }
                        onChange={(value) =>
                            setData({ ...data, [name]: value })
                        }
                    />
                ) : (
                    <Fields
                        key={name}
                        fields={[field]}
                        values={data}
                        onChange={setData}
                        perfil={perfil}
                        disabled={disabled}
                        root={root}
                        schema={dataType}
                    />
                ),
            ];
        });
    const assigned = new Set([...identity, "dados", "motivo"]);
    const knownData = new Set([
        ...dataSections.flatMap((s) => s.names),
        "unidades",
        "fonte",
    ]);
    return (
        <div className="initial-load-fields">
            {groups.some((g) =>
                g.fields.some((f) => identity.includes(f.name)),
            ) && (
                <section className="initial-load-form-section">
                    <h3>Identificação da carga</h3>
                    <div className="initial-load-field-grid">
                        {renderRoot(identity)}
                    </div>
                </section>
            )}
            {body && (
                <>
                    {dataSections.map((section) => (
                        <section
                            className="initial-load-form-section"
                            key={section.title}
                        >
                            <header>
                                <h3>{section.title}</h3>
                                {section.help && (
                                    <InformationHint topic={section.help} />
                                )}
                            </header>
                            {section.title === "Quantidades levantadas" && (
                                <p className="hint">
                                    {
                                        information.initialLoadQuantities
                                            .description
                                    }
                                </p>
                            )}
                            <div className="initial-load-field-grid">
                                {renderData(section.names)}
                            </div>
                        </section>
                    ))}
                    <InitialLoadUnits
                        value={data.unidades}
                        onChange={(unidades) => setData({ ...data, unidades })}
                        root={root}
                        perfil={perfil}
                        disabled={disabled}
                    />
                </>
            )}
            <section className="initial-load-form-section initial-load-form-section--reason">
                <h3>Fonte e justificativa</h3>
                <div className="initial-load-field-grid">
                    {body && renderData(["fonte"])}
                    {renderRoot(["motivo"])}
                </div>
            </section>
            {(groups.some((g) => g.fields.some((f) => !assigned.has(f.name))) ||
                records[dataType].some((f) => !knownData.has(f.name))) && (
                <section className="initial-load-form-section">
                    <h3>Outras referências</h3>
                    <div className="initial-load-field-grid">
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
                        {body &&
                            renderData(
                                records[dataType]
                                    .filter((f) => !knownData.has(f.name))
                                    .map((f) => f.name),
                            )}
                    </div>
                </section>
            )}
        </div>
    );
}

function InitialLoadUnits({
    value,
    onChange,
    root,
    disabled,
}: {
    value: unknown;
    onChange: (next: unknown[]) => void;
    root: Values;
    perfil: Perfil;
    disabled: boolean;
}) {
    const refs = useContext(FormReferences);
    const rows = Array.isArray(value) ? value : [];
    return (
        <section className="initial-load-form-section">
            <header>
                <h3>Unidades informadas</h3>
                <InformationHint topic="initialLoadUnits" />
            </header>
            {rows.length === 0 && (
                <p className="hint">
                    {information.initialLoadUnitsEmpty.description}
                </p>
            )}
            <div className="initial-load-unit-list">
                {rows.map((row, index) => (
                    <section
                        key={index}
                        className="initial-load-unit"
                        aria-label={`Unidade ${index + 1}`}
                    >
                        <header>
                            <h4>Unidade {index + 1}</h4>
                            <button
                                type="button"
                                className="action-remove"
                                disabled={disabled}
                                onClick={() =>
                                    onChange(rows.filter((_, i) => i !== index))
                                }
                            >
                                Remover unidade {index + 1}
                            </button>
                        </header>
                        <div className="initial-load-field-grid">
                            {records[unitType].map((field) => (
                                <ScalarField
                                    key={field.name}
                                    f={field}
                                    path={field.name}
                                    schema={unitType}
                                    row={{
                                        ...root,
                                        ...(isObject(row) ? row : {}),
                                    }}
                                    labelOverride={
                                        field.name === "condicao"
                                            ? "Condição"
                                            : undefined
                                    }
                                    value={
                                        isObject(row)
                                            ? row[field.name]
                                            : undefined
                                    }
                                    disabled={disabled}
                                    showDecimalHint={false}
                                    choiceLabels={
                                        field.name === "tipo"
                                            ? {
                                                  PALLET: "Pallet",
                                                  BOBINA: "Bobina",
                                                  VOLUME: "Volume",
                                              }
                                            : field.name === "condicao"
                                              ? {
                                                    BOA: "Boa",
                                                    AVARIADA: "Avariada",
                                                    QUARENTENA: "Quarentena",
                                                    TRIAGEM: "Triagem",
                                                }
                                              : undefined
                                    }
                                    onChange={(next) =>
                                        onChange(
                                            rows.map((old, i) =>
                                                i === index
                                                    ? {
                                                          ...(isObject(old)
                                                              ? old
                                                              : {}),
                                                          [field.name]: next,
                                                      }
                                                    : old,
                                            ),
                                        )
                                    }
                                />
                            ))}
                        </div>
                    </section>
                ))}
            </div>
            <div className="initial-load-unit-actions">
                <button
                    type="button"
                    className="action-add"
                    disabled={disabled}
                    onClick={() =>
                        onChange([
                            ...rows,
                            initialValue(unitType, refs.defaults),
                        ])
                    }
                >
                    Adicionar unidade
                </button>
            </div>
        </section>
    );
}
