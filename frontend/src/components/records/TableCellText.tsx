import { useLayoutEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";
import { information, informationLabels } from "../../content/information";
import { Dialog } from "../layout/Dialog";

/** A célula conserva o texto; só a apresentação é abreviada. */
export function TableCellText({
    text,
    label,
}: {
    text: string;
    label: string;
}) {
    const anchor = useRef<HTMLSpanElement>(null);
    const [truncated, setTruncated] = useState(false);
    const [open, setOpen] = useState(false);
    useLayoutEffect(() => {
        const element = anchor.current;
        if (!element) return;
        const measure = () =>
            setTruncated(element.scrollWidth > element.clientWidth + 1);
        measure();
        const observer =
            typeof ResizeObserver === "undefined"
                ? undefined
                : new ResizeObserver(measure);
        observer?.observe(element);
        window.addEventListener("resize", measure);
        return () => {
            observer?.disconnect();
            window.removeEventListener("resize", measure);
        };
    }, [text]);
    const show = () => {
        if (truncated) setOpen(true);
    };
    return (
        <>
            <span
                ref={anchor}
                className="table-cell-text"
                role={truncated ? "button" : undefined}
                tabIndex={truncated ? 0 : undefined}
                aria-label={
                    truncated ? informationLabels.fullText(label) : undefined
                }
                aria-haspopup={truncated ? "dialog" : undefined}
                aria-expanded={truncated ? open : undefined}
                onClick={show}
                onKeyDown={(event) => {
                    if (
                        truncated &&
                        (event.key === "Enter" || event.key === " ")
                    ) {
                        event.preventDefault();
                        event.stopPropagation();
                        show();
                    }
                }}
            >
                {text}
            </span>
            {open &&
                createPortal(
                    <div onKeyDown={(event) => event.stopPropagation()}>
                        <Dialog
                            title={`${information.tableCellText.title}: ${label}`}
                            className="table-text-dialog"
                            onClose={() => setOpen(false)}
                        >
                            <p className="hint">
                                {information.tableCellText.description}
                            </p>
                            <div className="table-text-content" tabIndex={0}>
                                {text}
                            </div>
                        </Dialog>
                    </div>,
                    document.body,
                )}
        </>
    );
}
