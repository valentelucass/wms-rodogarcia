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
    return (
        <form
            onSubmit={(e) => {
                e.preventDefault();
                s.submit();
            }}
            noValidate
        >
            {(s.e.params.length > 0 ||
                s.e.query.some(
                    (f) => !["pagina", "tamanho"].includes(f.name),
                ) ||
                s.e.request ||
                s.e.multipart) && (
                <fieldset disabled={s.pending || s.uncertain || s.completed}>
                    <legend>Contexto e referências da operação</legend>
                    {s.e.params.length > 0 && (
                        <Fields
                            fields={s.e.params}
                            values={s.params}
                            onChange={s.setParams}
                            perfil={perfil}
                            lockedFields={fixedFields}
                        />
                    )}
                    {s.e.query.length > 0 && (
                        <Fields
                            fields={s.e.query.filter(
                                (f) => !["pagina", "tamanho"].includes(f.name),
                            )}
                            values={s.query}
                            onChange={s.setQuery}
                            perfil={perfil}
                            schema={s.e.id + ".query"}
                        />
                    )}
                    {s.e.request && (
                        <Fields
                            fields={records[s.e.request].filter(
                                (f) =>
                                    !collector ||
                                    !["medidas", "conjuntoId"].includes(f.name),
                            )}
                            values={s.body}
                            onChange={s.updateBody}
                            perfil={perfil}
                            schema={s.e.request}
                            lockedFields={fixedFields}
                        />
                    )}
                    {collector && s.e.request && (
                        <details>
                            <summary>
                                Medidas físicas e conjunto de duas posições
                            </summary>
                            <Fields
                                fields={records[s.e.request].filter((f) =>
                                    ["medidas", "conjuntoId"].includes(f.name),
                                )}
                                values={s.body}
                                onChange={s.updateBody}
                                perfil={perfil}
                                schema={s.e.request}
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
