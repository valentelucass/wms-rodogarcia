import { useId, useRef, useState } from "react";
import { Icon } from "../../design-system/Icon";
import { useReferenceCatalog } from "./ReferenceCatalog";

const normalize = (value: string) =>
    value
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .toLocaleLowerCase("pt-BR");
export function ReferenceSelect({
    kind,
    value,
    onChange,
    name,
    title,
    required = false,
    disabled = false,
    all = false,
    compact = false,
}: {
    kind: "clientes" | "armazens";
    value: string;
    onChange: (id: string) => void;
    name?: string;
    title: string;
    required?: boolean;
    disabled?: boolean;
    all?: boolean;
    compact?: boolean;
}) {
    const catalog = useReferenceCatalog();
    const id = useId(),
        listId = useId();
    const input = useRef<HTMLInputElement>(null);
    const [open, setOpen] = useState(false),
        [search, setSearch] = useState(""),
        [active, setActive] = useState(0);
    const options = catalog?.[kind] ?? [];
    const selected = options.find((option) => option.id === value);
    const allTitle =
        kind === "clientes" ? "Todos os clientes" : "Todos os armazéns";
    const choices = [
        ...(all
            ? [{ id: "", nome: allTitle, codigo: "", situacao: "ATIVO" }]
            : []),
        ...options,
    ];
    const filtered = choices.filter((option) =>
        normalize(option.nome + " " + option.codigo).includes(
            normalize(search),
        ),
    );
    const blocked = disabled || !!catalog?.loading;
    const selectedTitle = selected
        ? selected.nome
        : all && !value
          ? allTitle
          : value
            ? "Cadastro não disponível"
            : "";
    const choose = (index: number) => {
        const option = filtered[index];
        if (!option) return;
        onChange(option.id);
        setOpen(false);
        setSearch("");
        input.current?.focus();
    };
    return (
        <div
            className={`reference-select${compact ? " reference-select--compact" : ""}`}
            onBlur={(event) => {
                if (!event.currentTarget.contains(event.relatedTarget)) {
                    setOpen(false);
                    setSearch("");
                }
            }}
        >
            <label className={compact ? "sr-only" : undefined} htmlFor={id}>
                {title}
                {required && !compact ? " *" : ""}
            </label>
            {name && <input type="hidden" name={name} value={value} />}
            <div className="reference-select-control">
                <input
                    ref={input}
                    id={id}
                    role="combobox"
                    aria-expanded={open}
                    aria-controls={listId}
                    aria-autocomplete="list"
                    aria-haspopup="listbox"
                    aria-activedescendant={
                        open && filtered.length
                            ? `${listId}-${Math.min(active, filtered.length - 1)}`
                            : undefined
                    }
                    aria-required={required}
                    autoComplete="off"
                    disabled={blocked}
                    placeholder={catalog?.loading ? "Carregando…" : title}
                    value={open ? search : selectedTitle}
                    onClick={() => {
                        setOpen(true);
                        setSearch("");
                        setActive(0);
                    }}
                    onChange={(event) => {
                        setSearch(event.target.value);
                        setOpen(true);
                        setActive(0);
                    }}
                    onKeyDown={(event) => {
                        if (
                            event.key === "ArrowDown" ||
                            event.key === "ArrowUp"
                        ) {
                            event.preventDefault();
                            if (!open) {
                                setOpen(true);
                                setSearch("");
                                setActive(0);
                            } else
                                setActive((index) =>
                                    Math.max(
                                        0,
                                        Math.min(
                                            filtered.length - 1,
                                            index +
                                                (event.key === "ArrowDown"
                                                    ? 1
                                                    : -1),
                                        ),
                                    ),
                                );
                        } else if (event.key === "Enter" && open) {
                            event.preventDefault();
                            choose(active);
                        } else if (event.key === "Escape" && open) {
                            event.preventDefault();
                            event.stopPropagation();
                            setOpen(false);
                            setSearch("");
                        }
                    }}
                />
                <span className="reference-select-chevron" aria-hidden="true">
                    <Icon name="chevron-down" />
                </span>
            </div>
            {open && (
                <div className="reference-select-popup">
                    {catalog?.error ? (
                        <>
                            <p role="alert">{catalog.error}</p>
                            <button type="button" onClick={catalog.refresh}>
                                Atualizar opções
                            </button>
                        </>
                    ) : (
                        <>
                            <ul
                                id={listId}
                                role="listbox"
                                aria-label={`Opções de ${title}`}
                            >
                                {filtered.map((option, index) => (
                                    <li
                                        key={option.id}
                                        id={`${listId}-${index}`}
                                        role="option"
                                        aria-selected={option.id === value}
                                        className={
                                            index === active ? "is-active" : ""
                                        }
                                        ref={(element) => {
                                            if (
                                                index === active &&
                                                element?.parentElement
                                            ) {
                                                const list =
                                                    element.parentElement;
                                                if (
                                                    element.offsetTop <
                                                    list.scrollTop
                                                )
                                                    list.scrollTop =
                                                        element.offsetTop;
                                                else if (
                                                    element.offsetTop +
                                                        element.offsetHeight >
                                                    list.scrollTop +
                                                        list.clientHeight
                                                )
                                                    list.scrollTop =
                                                        element.offsetTop +
                                                        element.offsetHeight -
                                                        list.clientHeight;
                                            }
                                        }}
                                        onMouseDown={(event) =>
                                            event.preventDefault()
                                        }
                                        onClick={() => choose(index)}
                                    >
                                        <strong>{option.nome}</strong>
                                        {option.codigo && (
                                            <small>
                                                {option.codigo}
                                                {option.situacao !== "ATIVO"
                                                    ? ` · ${option.situacao.replaceAll("_", " ").toLowerCase()}`
                                                    : ""}
                                            </small>
                                        )}
                                    </li>
                                ))}
                            </ul>
                            {filtered.length === 0 && (
                                <p role="status">
                                    {options.length
                                        ? "Nenhum cadastro encontrado."
                                        : "Nenhum cadastro disponível para seu acesso."}
                                </p>
                            )}
                            <small className="reference-select-hint">
                                {filtered.length}{" "}
                                {filtered.length === 1 ? "opção" : "opções"} ·
                                selecione um cadastro
                            </small>
                        </>
                    )}
                </div>
            )}
        </div>
    );
}
