import type { Perfil } from "../../contracts/runtime";
import { records } from "../../contracts/runtime";
import type { OperationState } from "../../hooks/useOperation";
import { Fields } from "../Fields";
import { ScalarField } from "../fields/ScalarField";
import { DispatchOrderFields } from "../../modules/saida/DispatchOrderFields";
import { AddressImportFields } from "../../modules/cadastros/AddressImportFields";
import { PositionSetFields } from "../../modules/cadastros/PositionSetFields";
import { AddressEditFields } from "../../modules/cadastros/AddressEditFields";
import { EndValidityFields } from "../../modules/cadastros/EndValidityFields";
import { DamageRegistrationFields } from "../../modules/estoque/DamageRegistrationFields";
import { ContractConfigurationFields } from "../../modules/financeiro/ContractConfigurationFields";
import { ServiceFactFields } from "../../modules/financeiro/ServiceFactFields";
import { CalculationFields } from "../../modules/financeiro/CalculationFields";
import { ReopenCycleFields } from "../../modules/financeiro/ReopenCycleFields";
import { InitialLoadFields } from "../../modules/regularizacao/InitialLoadFields";
import { ContingencyRegistrationFields } from "../../modules/regularizacao/ContingencyRegistrationFields";
import { automaticCodes } from "../../domain/automaticCodes";
import { operationContextFields } from "../../domain/operationContextFields";
import { information } from "../../content/information";
import { Result } from "../Result";
import {
    presentationLabel,
    technicalField,
} from "../../domain/referencePresentation";
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
    const dispatchOrderCreate = s.e.id === "PedidoSaidaController.criar";
    const addressEdit = s.e.id === "EnderecoController.alterar";
    const initialLoadQuery = s.e.id === "CargaInicialController.consultar";
    const labelQuery = s.e.id === "UnidadeLogisticaController.etiqueta";
    const serviceSuggestionsQuery =
        s.e.id === "FatoServicoController.sugestoes";
    const positionSetCreate = s.e.id === "CapacidadeController.criar";
    const tableEdit = s.e.id === "ConfiguracaoCobrancaController.alterar";
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
    const visibleFixedField = (name: string) =>
        fixedFields.includes(name) && name !== "id" && !technicalField(name);
    const hasVisibleFixedFields = groups.some((group) =>
        group.fields.some((f) => visibleFixedField(f.name)),
    );
    const hasEditableFields = groups.some((group) =>
        group.fields.some((f) => !fixedFields.includes(f.name)),
    );
    const paginationFields = s.e.query.some((f) =>
        ["pagina", "tamanho"].includes(f.name),
    ) && (
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
    );
    return (
        <form
            className={
                dispatchOrderCreate
                    ? "dispatch-order-form"
                    : initialLoadQuery
                      ? "initial-load-query"
                      : labelQuery
                        ? "label-query"
                        : serviceSuggestionsQuery
                          ? "service-suggestions-query"
                          : undefined
            }
            onSubmit={(e) => {
                e.preventDefault();
                s.submit();
            }}
            noValidate
        >
            {serviceSuggestionsQuery && (
                <p className="hint service-suggestions-intro">
                    {information.serviceSuggestions.description}
                </p>
            )}
            {initialLoadQuery && (
                <p className="hint initial-load-query-hint">
                    {information.initialLoadQuery.description}
                </p>
            )}
            {tableEdit && (
                <p className="hint">
                    {information.billingTableEdit.description}
                </p>
            )}
            {registryEdit && (
                <p className="hint">{information.registryEdit.description}</p>
            )}
            {/^(EnderecoController\.(criar|alterar)|CapacidadeController\.configurar)$/.test(
                s.e.id,
            ) &&
                (addressEdit ? (
                    <details className="address-edit-help">
                        <summary>{information.addressCapacity.title}</summary>
                        <p className="hint">
                            {information.addressCapacity.description}
                        </p>
                    </details>
                ) : (
                    <p className="hint">
                        {information.addressCapacity.description}
                    </p>
                ))}
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
            {automaticCodes[s.e.request ?? ""] && !positionSetCreate && (
                <p className="hint">
                    O código será gerado automaticamente ao confirmar.
                </p>
            )}
            {addressImport && <AddressImportFields state={s} />}
            {!addressImport && hasFixedFields && (
                <fieldset
                    className="operation-fixed-fields"
                    disabled
                    style={
                        registryEdit ||
                        tableEdit ||
                        !hasVisibleFixedFields ||
                        s.e.id.startsWith("EncerramentoController.")
                            ? { display: "none" }
                            : undefined
                    }
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
                            group.fields.some((f) => visibleFixedField(f.name)),
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
                        {dispatchOrderCreate ? (
                            <DispatchOrderFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : initialLoadQuery ? (
                            groups.flatMap((group) =>
                                group.fields
                                    .filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    )
                                    .map((field) => (
                                        <ScalarField
                                            key={group.schema + field.name}
                                            f={field}
                                            value={group.values[field.name]}
                                            path=""
                                            schema={group.schema}
                                            row={group.values}
                                            labelOverride={
                                                field.name === "id"
                                                    ? "Carga inicial"
                                                    : undefined
                                            }
                                            disabled={
                                                s.uncertain || s.completed
                                            }
                                            onChange={(value) =>
                                                group.onChange({
                                                    ...group.values,
                                                    [field.name]: value,
                                                })
                                            }
                                        />
                                    )),
                            )
                        ) : s.e.id === "ContingenciaController.registrar" ? (
                            <ContingencyRegistrationFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (f) => !fixedFields.includes(f.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : s.e.id === "EncerramentoController.vigencia" ? (
                            <EndValidityFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : addressEdit ? (
                            <AddressEditFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : positionSetCreate ? (
                            <PositionSetFields state={s} />
                        ) : s.e.id ===
                          "FechamentoCobrancaController.reabrir" ? (
                            <ReopenCycleFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : s.e.id === "CalculoCobrancaController.calcular" ? (
                            <CalculationFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : s.e.id === "FatoServicoController.registrar" ? (
                            <ServiceFactFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : s.e.id ===
                          "ConfiguracaoCobrancaController.configurar" ? (
                            <ContractConfigurationFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : /^CargaInicialController\.(criar|revisar)$/.test(
                              s.e.id,
                          ) ? (
                            <InitialLoadFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : s.e.id === "AvariaController.registrar" ? (
                            <DamageRegistrationFields
                                groups={groups.map((group) => ({
                                    ...group,
                                    fields: group.fields.filter(
                                        (field) =>
                                            !fixedFields.includes(field.name),
                                    ),
                                }))}
                                perfil={perfil}
                                disabled={
                                    s.pending || s.uncertain || s.completed
                                }
                            />
                        ) : (
                            groups.map((group, index) => (
                                <Fields
                                    key={index}
                                    {...group}
                                    fields={group.fields.filter(
                                        (f) => !fixedFields.includes(f.name),
                                    )}
                                    perfil={perfil}
                                    lockedFields={fixedFields}
                                />
                            ))
                        )}
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
            {serviceSuggestionsQuery ? (
                <details className="service-suggestions-pagination">
                    <summary>Opções de paginação</summary>
                    {paginationFields}
                </details>
            ) : (
                paginationFields
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
                        className="action-replay"
                        type="button"
                        disabled={s.pending}
                        onClick={() => void s.send(true)}
                    >
                        Repetir exatamente a mesma operação
                    </button>
                )}
                {s.completed && (
                    <button
                        type="button"
                        className="action-add"
                        onClick={s.startNew}
                    >
                        Iniciar nova operação
                    </button>
                )}
            </div>
        </form>
    );
}
