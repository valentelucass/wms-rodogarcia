import { useId } from "react";
import { isObject, records } from "../../contracts/runtime";
import type { OperationState } from "../../hooks/useOperation";
import { ScalarField } from "../../components/fields/ScalarField";
import { information } from "../../content/information";

const schema = "CapacidadeDto.CriarConjunto";
const fields = records[schema];
const dimensions = [
    ["alturaMetros", "Altura máxima (m)"],
    ["larguraMetros", "Largura máxima (m)"],
    ["profundidadeMetros", "Profundidade máxima (m)"],
];

export function PositionSetFields({ state: s }: { state: OperationState }) {
    const selectionHelp = useId();
    const limitsHelp = useId();
    const disabled = s.pending || s.uncertain || s.completed;
    const hasWarehouse = s.body.armazemId != null && s.body.armazemId !== "";
    const limits = isObject(s.body.limites) ? s.body.limites : {};
    const scalar = (name: string, onChange?: (value: unknown) => void) => (
        <ScalarField
            f={fields.find((field) => field.name === name)!}
            path={name}
            value={s.body[name]}
            row={s.body}
            schema={schema}
            onChange={
                onChange ??
                ((value) => s.updateBody({ ...s.body, [name]: value }))
            }
            disabled={disabled}
        />
    );
    const limit = (name: string, title: string) => (
        <ScalarField
            key={name}
            f={records["CapacidadeDto.Limites"].find(
                (field) => field.name === name,
            )!}
            path={name}
            labelOverride={title}
            value={limits[name]}
            schema="CapacidadeDto.Limites"
            onChange={(value) =>
                s.updateBody({
                    ...s.body,
                    limites: { ...limits, [name]: value },
                })
            }
            disabled={disabled}
            showDecimalHint={false}
        />
    );
    return (
        <div className="position-set-fields">
            <div className="position-set-intro">
                <p>{information.positionSetCreate.description}</p>
                <span className="position-set-code hint">
                    {information.positionSetCreate.footer}
                </span>
            </div>
            <fieldset className="position-set-section">
                <legend>1. Armazém e posições</legend>
                <div className="position-set-warehouse">
                    {scalar("armazemId", (value) =>
                        s.updateBody(
                            value === s.body.armazemId
                                ? s.body
                                : {
                                      ...s.body,
                                      armazemId: value,
                                      enderecoAId: "",
                                      enderecoBId: "",
                                  },
                        ),
                    )}
                </div>
                <p className="hint" id={selectionHelp}>
                    {information.positionSetSelection.description}
                </p>
                <fieldset
                    className="position-set-pair"
                    disabled={disabled || !hasWarehouse}
                    aria-describedby={selectionHelp}
                >
                    <legend className="sr-only">Posições do conjunto</legend>
                    {scalar("enderecoAId")}
                    {scalar("enderecoBId")}
                </fieldset>
            </fieldset>
            <fieldset
                className="position-set-section"
                aria-describedby={limitsHelp}
            >
                <legend>2. Limites do conjunto</legend>
                <p className="hint" id={limitsHelp}>
                    {information.positionSetLimits.description}
                </p>
                <div className="position-set-load">
                    {limit("pesoKg", "Peso máximo (kg)")}
                    {limit("empilhamentoMaximo", "Empilhamento máximo")}
                </div>
                <div className="position-set-dimensions">
                    {dimensions.map(([name, title]) => limit(name, title))}
                </div>
            </fieldset>
            <fieldset className="position-set-section position-set-reason">
                <legend>3. Justificativa</legend>
                {scalar("motivo")}
            </fieldset>
        </div>
    );
}
