import { useLayoutEffect, useRef, type ComponentPropsWithoutRef } from "react";

export function ResponsiveTable({
    className = "",
    children,
    ...props
}: ComponentPropsWithoutRef<"table">) {
    const table = useRef<HTMLTableElement>(null);
    useLayoutEffect(() => {
        const element = table.current;
        if (!element) return;
        const heading = element.tHead?.rows.item(
            (element.tHead?.rows.length ?? 1) - 1,
        );
        const labels = heading
            ? Array.from(
                  heading.cells,
                  (cell) => cell.textContent?.trim() ?? "",
              )
            : [];
        for (const body of [
            ...Array.from(element.tBodies),
            ...(element.tFoot ? [element.tFoot] : []),
        ]) {
            for (const row of Array.from(body.rows)) {
                let column = 0;
                for (const cell of Array.from(row.cells)) {
                    if (cell.colSpan === 1 && labels[column])
                        cell.dataset.label = labels[column];
                    else delete cell.dataset.label;
                    column += cell.colSpan;
                }
            }
        }
    }, [children]);
    return (
        <table
            {...props}
            ref={table}
            className={`mobile-records ${className}`.trim()}
        >
            {children}
        </table>
    );
}
