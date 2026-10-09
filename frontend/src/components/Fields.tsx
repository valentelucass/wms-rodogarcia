import { useContext } from "react";
import {
    records,
    listType,
    isObject,
    type Field,
    type Values,
    type Perfil,
} from "../contracts/runtime";
import { initialValue } from "../contracts/codec";
import { label } from "../domain/labels";
import { ContingencyData } from "../modules/regularizacao/ContingencyFields";
import { ScalarField } from "./fields/ScalarField";
import { isFieldDisabled } from "../domain/fieldPresentation";
import { FormReferences } from "./FormReferences";
import { CoverageFields } from "../modules/saida/CoverageFields";
import { RegroupOrigins } from "../modules/recebimento/RegroupOrigins";
import { ResolutionReferences } from "../modules/financeiro/ResolutionReferences";
interface Props {
    fields: Field[];
    values: Values;
    onChange: (v: Values) => void;
    prefix?: string;
    disabled?: boolean;
    perfil: Perfil;
    root?: Values;
    schema?: string;
    lockedFields?: string[];
}
export function Fields({
    fields,
    values,
    onChange,
    prefix = "",
    disabled = false,
    perfil,
    root = values,
    schema = "",
    lockedFields = [],
}: Props) {
    return (
        <>
            {fields.map((f) => (
                <FieldControl
                    key={f.name}
                    f={f}
                    value={values[f.name]}
                    path={prefix ? prefix + "." + f.name : f.name}
                    onChange={(v) => onChange({ ...values, [f.name]: v })}
                    disabled={
                        disabled ||
                        lockedFields.includes(f.name) ||
                        isFieldDisabled(schema, f.name, perfil)
                    }
                    perfil={perfil}
                    root={root}
                />
            ))}
        </>
    );
}
function FieldControl({
    f,
    value,
    path,
    onChange,
    disabled,
    perfil,
    root,
}: {
    f: Field;
    value: unknown;
    path: string;
    onChange: (v: unknown) => void;
    disabled: boolean;
    perfil: Perfil;
    root: Values;
}) {
    const refs = useContext(FormReferences);
    const lt = listType(f.type);
    const title = label(f.name) + (f.required ? " *" : "");
    if (f.type === "List<UnidadeLogisticaDto.RevisaoUnidade>")
        return (
            <RegroupOrigins
                value={value}
                onChange={onChange}
                disabled={disabled}
                renderRow={(v, i, change) => (
                    <Fields
                        fields={records["UnidadeLogisticaDto.RevisaoUnidade"]}
                        values={v}
                        onChange={change}
                        prefix={path + "." + i}
                        disabled={disabled}
                        perfil={perfil}
                        root={root}
                        schema="UnidadeLogisticaDto.RevisaoUnidade"
                    />
                )}
            />
        );
    if (f.type === "List<ExpedicaoDto.Cobertura>")
        return (
            <CoverageFields
                value={value}
                onChange={onChange}
                disabled={disabled}
                renderRow={(row, index, change) => (
                    <Fields
                        fields={records["ExpedicaoDto.Cobertura"]}
                        values={row}
                        onChange={change}
                        prefix={path + "." + index}
                        disabled={disabled}
                        perfil={perfil}
                        root={root}
                        schema="ExpedicaoDto.Cobertura"
                    />
                )}
            />
        );
    if (lt) {
        const list = Array.isArray(value) ? value : [];
        return (
            <fieldset disabled={disabled}>
                <legend>{title}</legend>
                {list.length === 0 && (
                    <p className="hint">Nenhum item informado.</p>
                )}
                {list.map((v, i) => (
                    <div className="list-item" key={i}>
                        <h4>
                            {label(f.name)} {i + 1}
                        </h4>
                        <FieldControl
                            f={{
                                ...(f.element ?? { required: false }),
                                name: String(i + 1),
                                type: lt,
                            }}
                            value={v}
                            path={path + "." + i}
                            onChange={(next) =>
                                onChange(
                                    list.map((old, j) =>
                                        i === j ? next : old,
                                    ),
                                )
                            }
                            disabled={disabled}
                            perfil={perfil}
                            root={root}
                        />
                        <button
                            type="button"
                            onClick={() =>
                                onChange(list.filter((_, j) => i !== j))
                            }
                        >
                            Remover {label(f.name)} {i + 1}
                        </button>
                    </div>
                ))}
                <button
                    type="button"
                    onClick={() =>
                        onChange([...list, initialValue(lt, refs.defaults)])
                    }
                >
                    Adicionar {label(f.name)}
                </button>
            </fieldset>
        );
    }
    if (records[f.type]) {
        return (
            <fieldset disabled={disabled}>
                <legend>{title}</legend>
                {f.type === "FechamentoCobrancaDto.Resolucao" &&
                    isObject(value) && (
                        <ResolutionReferences onChange={onChange} />
                    )}
                {value === null || value === undefined ? (
                    <button
                        type="button"
                        onClick={() => onChange(initialValue(f.type))}
                    >
                        Informar {label(f.name)}
                    </button>
                ) : (
                    <>
                        <Fields
                            fields={records[f.type]}
                            values={isObject(value) ? value : {}}
                            onChange={onChange}
                            prefix={path}
                            disabled={disabled}
                            perfil={perfil}
                            root={root}
                            schema={f.type}
                        />
                        {!f.required && (
                            <button
                                type="button"
                                onClick={() => onChange(null)}
                            >
                                Remover {label(f.name)}
                            </button>
                        )}
                    </>
                )}
            </fieldset>
        );
    }
    if (f.type.startsWith("Map<"))
        return (
            <ContingencyData
                Fields={Fields}
                value={value}
                onChange={onChange}
                disabled={disabled}
                perfil={perfil}
                kind={String(root.tipo ?? "CHEGADA")}
            />
        );
    return (
        <ScalarField
            f={f}
            value={value}
            path={path}
            onChange={onChange}
            disabled={disabled}
        />
    );
}
