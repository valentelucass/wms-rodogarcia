import type { Perfil } from "../../contracts/runtime";
import { records } from "../../contracts/runtime";
import type { OperationState } from "../../hooks/useOperation";
import { Fields } from "../Fields";
import { AddressImportFields } from "../../modules/cadastros/AddressImportFields";
import { automaticCodes } from "../../domain/automaticCodes";
import { operationContextFields } from "../../domain/operationContextFields";
import { information } from "../../content/information";
import { Result } from "../Result";
import { presentationLabel } from "../../domain/referencePresentation";
export function OperationForm({
    state: s,
    perfil,
    collector = false,
    fixedFields = [],
}: {
    state: OperationState;
    perfil: Perfil;
    collector?: boolean;
    fixedFields?: string[];
}) {
    const addressImport = s.e.id === "ImportacaoEnderecoController.previa";
    const registryEdit =
        /^(Cliente|Armazem|Produto|Embalagem|Endereco)Controller\.alterar$/.test(
            s.e.id,
        );
    const internalFields = operationContextFields(s.e.id);
    const queryFields = s.e.query.filter(
        (f) => !["pagina", "tamanho"].includes(f.name),
    );
    const bodyFields = (s.e.request ? records[s.e.request] : []).filter(
        (f) =>
            !(s.withoutReceivingDivergence && f.name === "aceitarDivergencias"),
    );
    const collectorFields = collector
        ? bodyFields.filter((f) => ["medidas", "conjuntoId"].includes(f.name))
        : [];
    if (registryEdit)
        bodyFields.sort(
            (a, b) => Number(a.name === "motivo") - Number(b.name === "motivo"),
        );
    const groups = [
        {
            fields: s.e.params,
            values: s.params,
            onChange: s.setParams,
            schema: s.e.id + ".params",
        },
        {
            fields: queryFields,
            values: s.query,
            onChange: s.setQuery,
            schema: s.e.id + ".query",
        },
        {
            fields: bodyFields.filter((f) => !collectorFields.includes(f)),
            values: s.body,
            onChange: s.updateBody,
            schema: s.e.request ?? "",
        },
    ].map((group) => ({
        ...group,
        fields: group.fields.filter(
            (f) =>
                !internalFields.includes(f.name) &&
                f.name !== "operacaoId" &&
                f.name !== automaticCodes[group.schema],
        ),
    }));
    const hasFixedFields = groups.some((group) =>
        group.fields.some((f) => fixedFields.includes(f.name)),
    );
    const hasEditableFields = groups.some((group) =>
        group.fields.some((f) => !fixedFields.includes(f.name)),
    );
    return (
        <form
            onSubmit={(e) => {
                e.preventDefault();
                s.submit();
            }}
            noValidate
        >
            {registryEdit && (
                <p className="hint">{information.registryEdit.description}</p>
            )}
            {/^(EnderecoController\.(criar|alterar)|CapacidadeController\.configurar)$/.test(
                s.e.id,
            ) && (
                <p className="hint">
                    {information.addressCapacity.description}
                </p>
            )}
            {s.e.id === "PedidoEntradaController.efetivar" && (
                <p className="hint">
                    {s.withoutReceivingDivergence
                        ? information.receivingFinalizationMatched.description
                        : information.receivingFinalizationAcceptance
                              .description}
                </p>
            )}
            {s.e.id === "UnidadeLogisticaController.unitizar" && (
                <p className="hint">
                    {information.receivingBobinas.description}
                </p>
            )}
            {automaticCodes[s.e.request ?? ""] && (
                <p className="hint">
                    O código será gerado automaticamente ao confirmar.
                </p>
            )}
            {addressImport && <AddressImportFields state={s} />}
            {!addressImport && hasFixedFields && (
                <fieldset
                    className="operation-fixed-fields"
                    disabled
                    style={registryEdit ? { display: "none" } : undefined}
                >
                    <legend className="sr-only">Registro selecionado</legend>
                    {groups.flatMap((group) =>
                        group.fields
                            .filter((f) => fixedFields.includes(f.name))
                            .map((f) => (
                                <input
                                    key={group.schema + f.name}
                                    type="hidden"
                                    aria-label={
                                        presentationLabel(f.name) +
                                        (f.required ? " *" : "")
                                    }
                                    value={String(group.values[f.name] ?? "")}
                                    readOnly
                                />
                            )),
                    )}
                    {groups
                        .filter((group) =>
                            group.fields.some(
                                (f) =>
                                    fixedFields.includes(f.name) &&
                                    !["id", "versao"].includes(f.name),
                            ),
                        )
                        .map((group, index) => (
                            <Result
                                key={index}
                                type={s.e.id}
                                data={Object.fromEntries(
                                    group.fields
                                        .filter((f) =>
                                            fixedFields.includes(f.name),
                                        )
                                        .map((f) => [
                                            f.name,
                                            group.values[f.name],
                                        ]),
                                )}
                            />
                        ))}
                </fieldset>
            )}
            {!addressImport &&
                (hasEditableFields ||
                    collectorFields.length > 0 ||
                    s.e.multipart) && (
                    <fieldset
                        className="operation-fields"
                        disabled={s.pending || s.uncertain || s.completed}
                    >
                        <legend className="sr-only">Dados da operação</legend>
                        {groups.map((group, index) => (
                            <Fields
                                key={index}
                                {...group}
                                fields={group.fields.filter(
                                    (f) => !fixedFields.includes(f.name),
                                )}
                                perfil={perfil}
                                lockedFields={fixedFields}
                            />
                        ))}
                        {collectorFields.length > 0 && (
                            <details className="collector-measures">
                                <summary>
                                    Medidas físicas e conjunto de duas posições
                                </summary>
                                <p className="hint">
                                    {information.collectorMeasures.description}
                                </p>
                                <Fields
                                    fields={collectorFields}
                                    values={s.body}
                                    onChange={s.updateBody}
                                    perfil={perfil}
                                    schema={s.e.request ?? undefined}
                                />
                            </details>
                        )}
                        {s.e.multipart && (
                            <label>
                                Planilha de endereços .xlsx *
                                <input
                                    type="file"
                                    accept=".xlsx"
                                    onChange={(e) =>
                                        s.setFile(e.target.files?.[0])
                                    }
                                />
                            </label>
                        )}
                    </fieldset>
                )}
            {s.e.query.some((f) => ["pagina", "tamanho"].includes(f.name)) && (
                <fieldset
                    className="pagination-fields"
                    disabled={s.pending || s.uncertain || s.completed}
                >
                    <legend>Paginação da consulta</legend>
                    <Fields
                        fields={s.e.query.filter((f) =>
                            ["pagina", "tamanho"].includes(f.name),
                        )}
                        values={s.query}
                        onChange={s.setQuery}
                        perfil={perfil}
                        schema={s.e.id + ".query"}
                    />
                </fieldset>
            )}
            <div className="actions">
                <button
                    className="primary"
                    type="submit"
                    disabled={s.pending || s.completed || s.uncertain}
                >
                    {s.pending
                        ? s.e.method === "GET"
                            ? "Consultando…"
                            : "Aguardando confirmação…"
                        : s.e.method === "GET"
                          ? "Consultar"
                          : addressImport
                            ? "Conferir prévia"
                            : "Conferir e confirmar"}
                </button>
                {s.pending && (
                    <button type="button" onClick={s.interrupt}>
                        Interromper espera
                    </button>
                )}
                {(s.uncertain || s.completed) && s.canReplay && (
                    <button
                        type="button"
                        disabled={s.pending}
                        onClick={() => void s.send(true)}
                    >
                        Repetir exatamente a mesma operação
                    </button>
                )}
                {s.completed && (
                    <button type="button" onClick={s.startNew}>
                        Iniciar nova operação
                    </button>
                )}
            </div>
        </form>
    );
}
