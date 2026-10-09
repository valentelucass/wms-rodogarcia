import { Icon } from "../../design-system/Icon";

export function Pagination({
    page,
    pages,
    total,
    count,
    size,
    disabled = false,
    onPage,
}: {
    page: number;
    pages: number;
    total: string | number;
    count: number;
    size?: number;
    disabled?: boolean;
    onPage?: (page: number) => void;
}) {
    return (
        <nav className="pagination" aria-label="Paginação dos resultados">
            <p className="pagination-summary">
                <span>{total}</span> registros · {count} nesta página
                {size !== undefined && (
                    <>
                        {" "}
                        · <span>{size}</span> por página
                    </>
                )}
            </p>
            <div className="pagination-controls">
                <button
                    type="button"
                    disabled={disabled || !onPage || page <= 0}
                    onClick={() => onPage?.(page - 1)}
                >
                    <Icon name="chevron-left" />
                    <span>Anterior</span>
                </button>
                <span aria-live="polite">
                    Página {page + 1} de {Math.max(1, pages)}
                </span>
                <button
                    type="button"
                    disabled={disabled || !onPage || page + 1 >= pages}
                    onClick={() => onPage?.(page + 1)}
                >
                    <span>Próxima</span>
                    <Icon name="chevron-right" />
                </button>
            </div>
        </nav>
    );
}
