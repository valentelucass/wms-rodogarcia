import { useState } from "react";
import { useReferenceCatalog } from "../components/context/ReferenceCatalog";

export function ScopeChoices({
    kind,
    selected = [],
}: {
    kind: "clientes" | "armazens";
    selected?: string[];
}) {
    const catalog = useReferenceCatalog(),
        [values, setValues] = useState(selected),
        [search, setSearch] = useState("");
    const title =
        kind === "clientes" ? "Clientes permitidos" : "Armazéns permitidos";
    const options = catalog?.[kind] ?? [];
    const unavailable = values.filter(
        (id) => !options.some((o) => o.id === id),
    );
    return (
        <fieldset className="scope-choices">
            <legend>{title}</legend>
            <input type="hidden" name={kind} value={values.join(",")} />
            <input
                aria-label={`Buscar ${title.toLowerCase()}`}
                placeholder="Buscar nome ou código"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
            />
            {catalog?.loading && <p role="status">Carregando opções…</p>}
            {catalog?.error && <p role="alert">{catalog.error}</p>}
            <div className="scope-options">
                {options
                    .filter((o) =>
                        `${o.nome} ${o.codigo}`
                            .toLocaleLowerCase("pt-BR")
                            .includes(search.toLocaleLowerCase("pt-BR")),
                    )
                    .map((o) => (
                        <label key={o.id}>
                            <input
                                type="checkbox"
                                checked={values.includes(o.id)}
                                onChange={(e) =>
                                    setValues((old) =>
                                        e.target.checked
                                            ? [...old, o.id]
                                            : old.filter((id) => id !== o.id),
                                    )
                                }
                            />
                            {o.nome} · {o.codigo}
                        </label>
                    ))}
            </div>
            {unavailable.length > 0 && (
                <p className="hint">
                    {unavailable.length} vínculo(s) existente(s) preservado(s),
                    com nome indisponível nesta consulta.
                </p>
            )}
            {!catalog?.loading && !options.length && (
                <p className="hint">Nenhuma opção disponível nesta consulta.</p>
            )}
        </fieldset>
    );
}
