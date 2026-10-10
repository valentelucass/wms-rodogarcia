import type { Perfil } from "../../contracts/runtime";
import { records } from "../../contracts/runtime";
import type { OperationState } from "../../hooks/useOperation";
import { Fields } from "../Fields";
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
    const queryFields = s.e.query.filter(
        (f) => !["pagina", "tamanho"].includes(f.name),
    );
    const bodyFields = s.e.request ? records[s.e.request] : [];
    const collectorFields = collector
        ? bodyFields.filter((f) => ["medidas", "conjuntoId"].includes(f.name))
        : [];
    const groups = [
        {
            fields: s.e.params,
            values: s.params,
            onChange: s.setParams,
            schema: "",
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
    ];
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
            {hasFixedFields && (
                <fieldset className="operation-fixed-fields" disabled>
                    <legend className="sr-only">Registro selecionado</legend>
                    {groups.map((group, index) => (
                        <Fields
                            key={index}
                            {...group}
                            fields={group.fields.filter((f) =>
                                fixedFields.includes(f.name),
                            )}
                            perfil={perfil}
                            lockedFields={fixedFields}
                        />
                    ))}
                </fieldset>
            )}
            {(hasEditableFields ||
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
                        <details>
                            <summary>
                                Medidas físicas e conjunto de duas posições
                            </summary>
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
                                onChange={(e) => s.setFile(e.target.files?.[0])}
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
