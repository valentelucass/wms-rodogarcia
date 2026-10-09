import { records, isObject, type Perfil } from "../../contracts/runtime";
import { contingencyTypes } from "../../contracts/contingency";
import type { Fields as FieldsView } from "../../components/Fields";
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
        <fieldset disabled={disabled}>
            <legend>Dados do fato de {kind}</legend>
            <p>
                Alvo e comando da planilha. Reserva, separação e retorno admitem
                vínculo de efeito existente conforme o servidor; não são fila
                offline.
            </p>
            <Fields
                fields={config.targets}
                values={data}
                onChange={onChange}
                perfil={perfil}
            />
            <Fields
                fields={records[config.dto].filter(
                    (f) => f.name !== "operacaoId",
                )}
                values={isObject(data.dados) ? data.dados : {}}
                onChange={(v) => onChange({ ...data, dados: v })}
                prefix="dados.dados"
                perfil={perfil}
                schema={config.dto}
            />
        </fieldset>
    );
}
